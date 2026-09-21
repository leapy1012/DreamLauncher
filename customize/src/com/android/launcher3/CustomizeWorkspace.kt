package com.android.launcher3

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import com.android.customize.overlay.OverlayBase
import com.android.launcher3.DropTarget.DragObject
import com.android.launcher3.pageindicators.PageIndicator
import com.android.launcher3.util.CustomizeOverlayEdgeEffect
import com.android.launcher3.util.EdgeEffectCompat
import com.android.systemui.plugins.shared.LauncherOverlayManager.LauncherOverlay
import kotlin.math.abs

class CustomizeWorkspace<T> @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : Workspace<T>(context, attrs, defStyleAttr) where T : View, T : PageIndicator {

    private var overlayScrollActive = false
    private var lastOverlayProgress = 0f
    /** Overlay open velocity (px/s); positive = opening. */
    private var overlayVelocityPx = 0f
    private var overlayRtl = false
    private var velocityTracker: VelocityTracker? = null
    private val minFlingVelocity =
        ViewConfiguration.get(context).scaledMinimumFlingVelocity.toFloat()

    override fun onOverlayScrollChanged(scroll: Float) {
        super.onOverlayScrollChanged(scroll)
        if (scroll < 0f) {
            mOverlayProgress = Utilities.boundToRange(scroll, -1f, 1f)
            val count = mOverlayCallbacks.size
            for (i in 0 until count) {
                mOverlayCallbacks[i].onOverlayScrollChanged(mOverlayProgress)
            }
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

    /**
     * ColorOS rubber-band overscroll is the only overlay driver.
     * Returning false prevents OverlayEdgeEffect from also firing on the same gesture
     * (double begin/scroll/end was the main swipe stutter).
     */
    override fun shouldPullEdgeGlow(): Boolean = false

    /**
     * ColorOS Workspace uses rubber-band [scrollTo] instead of EdgeEffect for overscroll.
     * Drive the installed [LauncherOverlay] from that path so Quick Glance opens on swipe.
     */
    override fun scrollTo(x: Int, y: Int) {
        super.scrollTo(x, y)
        driveOverlayFromOverscroll()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                velocityTracker?.recycle()
                velocityTracker = VelocityTracker.obtain().also { it.addMovement(ev) }
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
            // End overlay before Workspace processes UP so post-UP rubber-band scrollTo
            // cannot start a new overlay interaction in the same gesture.
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                endOverlayScrollIfNeeded()
                velocityTracker?.recycle()
                velocityTracker = null
            }
        }
        return super.onTouchEvent(ev)
    }

    private fun driveOverlayFromOverscroll() {
        if (mOverlayEdgeEffect == null) return
        // Only drive while the finger is down. After UP, Workspace rubber-band settle
        // still calls scrollTo; starting a new overlay interaction there cancels DQG's
        // open settle animator and leaves a full-screen touchable ghost window.
        if (!isHandlingTouch()) return
        val size = mOrientationHandler.getMeasuredSize(this).toFloat().coerceAtLeast(1f)
        val unbounded = getUnboundedScrollForOverlay()
        val overLeft = (mMinScroll - unbounded).toFloat()
        val overRight = (unbounded - mMaxScroll).toFloat()

        when {
            overLeft > 0.5f && !mIsRtl ->
                dispatchOverlayProgress(overLeft / size, rtl = false)
            overRight > 0.5f && !mIsRtl ->
                dispatchOverlayProgress(overRight / size, rtl = true)
            overRight > 0.5f && mIsRtl ->
                dispatchOverlayProgress(overRight / size, rtl = false)
            overLeft > 0.5f && mIsRtl ->
                dispatchOverlayProgress(overLeft / size, rtl = true)
        }
    }

    private fun dispatchOverlayProgress(rawProgress: Float, rtl: Boolean) {
        val progress = rawProgress.coerceIn(0f, 1f)
        val edge = if (rtl) {
            mEdgeGlowRight as? CustomizeOverlayEdgeEffect
        } else {
            mEdgeGlowLeft as? CustomizeOverlayEdgeEffect
        } ?: return
        val overlay = edge.launcherOverlay
        if (!overlayScrollActive) {
            Log.i(TAG, "overlay scroll begin progress=$progress rtl=$rtl")
            overlay.onScrollInteractionBegin()
            overlayScrollActive = true
            overlayVelocityPx = 0f
        }
        overlayRtl = rtl
        if (abs(progress - lastOverlayProgress) > 0.01f || progress >= 0.99f) {
            lastOverlayProgress = progress
            overlay.onScrollChange(progress, rtl)
        }
    }

    private fun computeOverlayFlingVelocity(): Float {
        val tracker = velocityTracker ?: return 0f
        tracker.computeCurrentVelocity(1000)
        // Minus-side open: finger moves right (positive X). Plus-side: left (negative X).
        val vx = tracker.xVelocity
        val opening = if (overlayRtl) -vx else vx
        return if (abs(opening) >= minFlingVelocity) opening else 0f
    }

    private fun endOverlayScrollIfNeeded() {
        if (!overlayScrollActive) return
        val edge = (mEdgeGlowLeft as? CustomizeOverlayEdgeEffect)
            ?: (mEdgeGlowRight as? CustomizeOverlayEdgeEffect)
            ?: return
        val overlay = edge.launcherOverlay
        val velocity = computeOverlayFlingVelocity()
        overlayVelocityPx = velocity
        Log.i(TAG, "overlay scroll end last=$lastOverlayProgress v=$velocity")
        if (overlay is OverlayBase) {
            overlay.onScrollInteractionEndWithVelocity(velocity)
        } else {
            overlay.onScrollInteractionEnd()
        }
        overlayScrollActive = false
        lastOverlayProgress = 0f
        overlayVelocityPx = 0f
    }

    private fun getUnboundedScrollForOverlay(): Int = mUnboundedScroll

    companion object {
        private const val TAG = "CustomizeWorkspace"
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
