package com.android.launcher3

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import com.android.customize.overlay.OverlayBase
import com.android.launcher3.DropTarget.DragObject
import com.android.launcher3.pageindicators.PageIndicator
import com.android.launcher3.util.CustomizeOverlayEdgeEffect
import com.android.launcher3.util.EdgeEffectCompat
import com.android.systemui.plugins.shared.LauncherOverlayManager.LauncherOverlay
import kotlin.math.abs

/**
 * Oppo [OplusWorkspace.overScroll] — pure amount/width pipeline:
 * - progress = |unbounded overscroll| / width (no EdgeEffect, no finger-Δx, no seed)
 * - onScrollChange every eligible frame (not gated on session begin — Oppo 5112-5113)
 * - Session begin only: interactionBegan + scroller idle (Oppo isFinished)
 * - Gate overlay while page springing only (Oppo isSpringing ≈ isPageSpringing)
 * - z6: leave overscroll zone with prior progress → force 0
 * - Home frost/scale from overlayScrollChanged (no DragLayer slide+fade)
 */
class CustomizeWorkspace<T> @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : Workspace<T>(context, attrs, defStyleAttr) where T : View, T : PageIndicator {

    /** Oppo mStartedSendingScrollEvents */
    private var startedSendingScrollEvents = false
    /** Oppo mScrollInteractionBegan */
    private var scrollInteractionBegan = false
    private var lastOverlayProgress = 0f
    private var overlayRtl = false
    private var velocityTracker: VelocityTracker? = null

    override fun onOverlayScrollChanged(scroll: Float) {
        super.onOverlayScrollChanged(scroll)
        if (scroll < 0f) {
            mOverlayProgress = Utilities.boundToRange(scroll, -1f, 1f)
            val count = mOverlayCallbacks.size
            for (i in 0 until count) {
                mOverlayCallbacks[i].onOverlayScrollChanged(mOverlayProgress)
            }
        }
        if (!startedSendingScrollEvents) {
            lastOverlayProgress = abs(mOverlayProgress)
        }
    }

    override fun setLauncherOverlay(overlay: LauncherOverlay?) {
        val newEffect: EdgeEffectCompat
        val newRightEffect: EdgeEffectCompat
        if (overlay == null) {
            newEffect = EdgeEffectCompat(context)
            newRightEffect = EdgeEffectCompat(context)
            mOverlayEdgeEffect = null
        } else {
            mOverlayEdgeEffect = CustomizeOverlayEdgeEffect(context, overlay, false)
            newEffect = mOverlayEdgeEffect
            newRightEffect = CustomizeOverlayEdgeEffect(context, overlay, true)
            overlay.setOverlayCallbacks(this)
        }

        if (mIsRtl) {
            mEdgeGlowRight = newEffect
            mEdgeGlowLeft = newRightEffect
        } else {
            mEdgeGlowLeft = newEffect
            mEdgeGlowRight = newRightEffect
        }
        onOverlayScrollChanged(0f)
    }

    override fun shouldPullEdgeGlow(): Boolean = false

    override fun scrollTo(x: Int, y: Int) {
        super.scrollTo(x, y)
        driveOverlayFromOverscroll()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                velocityTracker?.recycle()
                velocityTracker = VelocityTracker.obtain().also { it.addMovement(ev) }
                // Oppo onScrollInteractionBegin sets mScrollInteractionBegan.
                scrollInteractionBegan = true
            }
            MotionEvent.ACTION_MOVE -> velocityTracker?.addMovement(ev)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                velocityTracker?.addMovement(ev)
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                endOverlayScrollIfNeeded()
                scrollInteractionBegan = false
                velocityTracker?.recycle()
                velocityTracker = null
            }
        }
        return super.onTouchEvent(ev)
    }

    /**
     * Oppo gates with OverScroller.isSpringing() only — page snap spring.
     * Do not treat residual COUI settle as springing; that deadens reverse swipes.
     */
    private fun isScrollerSpringing(): Boolean = mScroller.isPageSpringing()

    /**
     * Oppo [OplusWorkspace.overScroll].
     */
    private fun driveOverlayFromOverscroll() {
        if (mOverlayEdgeEffect == null) return

        val size = mOrientationHandler.getMeasuredSize(this).toFloat().coerceAtLeast(1f)
        val unbounded = mUnboundedScroll
        val overLeft = (mMinScroll - unbounded).toFloat()
        val overRight = (unbounded - mMaxScroll).toFloat()
        val pastMinus = if (!mIsRtl) overLeft > 0.5f else overRight > 0.5f
        val pastPlus = if (!mIsRtl) overRight > 0.5f else overLeft > 0.5f
        val minusAmount = if (!mIsRtl) overLeft else overRight

        // Oppo shouldScrollOverlay
        val shouldScrollOverlay = pastMinus && !isScrollerSpringing() &&
            mLauncher.stateManager.isInStableState(LauncherState.NORMAL)

        // Oppo z6: left overscroll zone exited while we still had glance progress.
        val z6 = lastOverlayProgress > SHOWING_EPSILON && !pastMinus &&
            lastOverlayProgress > 0f && !overlayRtl

        if (shouldScrollOverlay) {
            maybeBeginScrollSession()
            // Oppo: always emit amount/width while eligible — not gated on begin.
            dispatchOverlayProgress((minusAmount / size).coerceIn(0f, 1f), rtl = false)
        } else if (pastPlus && !isScrollerSpringing() &&
            mLauncher.stateManager.isInStableState(LauncherState.NORMAL)
        ) {
            val plusAmount = if (!mIsRtl) overRight else overLeft
            maybeBeginScrollSession()
            dispatchOverlayProgress((plusAmount / size).coerceIn(0f, 1f), rtl = true)
        }

        if (z6) {
            lastOverlayProgress = 0f
            dispatchOverlayProgress(0f, rtl = false)
        }
    }

    /**
     * Oppo: only start AIDL session when interaction began / being dragged,
     * scroller finished, not icon-dragging, translationY stable.
     */
    private fun maybeBeginScrollSession() {
        if (startedSendingScrollEvents) return
        if (!(scrollInteractionBegan || isHandlingTouch())) return
        if (mLauncher.dragController.isDragging) {
            scrollInteractionBegan = false
            return
        }
        // Oppo: mScroller.isFinished()
        if (!mScroller.isCOUIFinished()) return
        if (abs(translationY) > 0.5f) return

        Log.i(TAG, "overlay scroll begin")
        val edge = (mEdgeGlowLeft as? CustomizeOverlayEdgeEffect)
            ?: (mEdgeGlowRight as? CustomizeOverlayEdgeEffect)
            ?: return
        edge.launcherOverlay.onScrollInteractionBegin()
        startedSendingScrollEvents = true
    }

    private fun dispatchOverlayProgress(rawProgress: Float, rtl: Boolean) {
        val progress = rawProgress.coerceIn(0f, 1f)
        val edge = if (rtl) {
            mEdgeGlowRight as? CustomizeOverlayEdgeEffect
        } else {
            mEdgeGlowLeft as? CustomizeOverlayEdgeEffect
        } ?: return
        overlayRtl = rtl
        lastOverlayProgress = progress
        // Oppo 5112-5113: every eligible frame → onScrollChange (session optional).
        edge.launcherOverlay.onScrollChange(progress, rtl)
        // Drive home frost/scale immediately (Oppo OverlayAnimManager). Remote
        // overlayScrollChanged will echo the same progress during settle.
        if (!rtl) {
            onOverlayScrollChanged(progress)
        }
    }

    private fun computeOverlayFlingVelocity(): Float {
        val tracker = velocityTracker ?: return 0f
        tracker.computeCurrentVelocity(1000)
        val vx = tracker.xVelocity
        // Always forward signed open-direction velocity. Zeroing below
        // minFlingVelocity made quick flicks settle late/wrong on the remote.
        return if (!overlayRtl) vx else -vx
    }

    private fun endOverlayScrollIfNeeded() {
        if (!startedSendingScrollEvents) {
            // Oppo may have streamed onScrollChange without begin; still end if we
            // marked a session via progress-only path on the remote (auto-start).
            if (lastOverlayProgress <= SHOWING_EPSILON) return
            val edge = (mEdgeGlowLeft as? CustomizeOverlayEdgeEffect)
                ?: (mEdgeGlowRight as? CustomizeOverlayEdgeEffect)
                ?: return
            // Ensure begin+end pair so QG can settle.
            edge.launcherOverlay.onScrollInteractionBegin()
            startedSendingScrollEvents = true
        }
        val edge = (mEdgeGlowLeft as? CustomizeOverlayEdgeEffect)
            ?: (mEdgeGlowRight as? CustomizeOverlayEdgeEffect)
            ?: return
        val overlay = edge.launcherOverlay
        val velocity = computeOverlayFlingVelocity()
        Log.i(TAG, "overlay scroll end last=$lastOverlayProgress v=$velocity")
        if (overlay is OverlayBase) {
            overlay.onScrollInteractionEndWithVelocity(velocity)
        } else {
            overlay.onScrollInteractionEnd()
        }
        startedSendingScrollEvents = false
        scrollInteractionBegan = false
    }

    companion object {
        private const val TAG = "CustomizeWorkspace"
        private const val SHOWING_EPSILON = 0.02f
    }
}

private const val DRAG_MODE_NONE = 0
private const val DRAG_MODE_REORDER = 3

fun Workspace<*>.manageAutofillOnDragOver(
    d: DragObject, minSpanX: Int, minSpanY: Int, reorderX: Int, reorderY: Int
) {
    Log.d("OK.DEBUG", "manageAutofillOnDragOver")
    val item = d.dragInfo
    val child = if (mDragInfo == null) null else mDragInfo.cell

    if ((mDragMode == DRAG_MODE_NONE || mDragMode == DRAG_MODE_REORDER)
        && (mLastReorderX != reorderX || mLastReorderY != reorderY)
        && !mReorderAlarm.alarmPending()
    ) {
        mLastReorderX = reorderX
        mLastReorderY = reorderY
        mDragTargetLayout.performReorder(
            mDragViewVisualCenter[0].toInt(),
            mDragViewVisualCenter[1].toInt(), minSpanX, minSpanY, item.spanX, item.spanY,
            child, mTargetCell, IntArray(2), CellLayout.MODE_SHOW_REORDER_HINT
        )
        val listener = ReorderAlarmListener(
            mDragViewVisualCenter,
            minSpanX, minSpanY, item.spanX, item.spanY, d, child
        )
        mReorderAlarm.setOnAlarmListener(listener)
        mReorderAlarm.setAlarm(Workspace.REORDER_TIMEOUT.toLong())
    }
}
