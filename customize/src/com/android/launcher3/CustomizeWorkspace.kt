package com.android.launcher3

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.android.launcher3.DropTarget.DragObject
import com.android.launcher3.Workspace.REORDER_TIMEOUT
import com.android.launcher3.dragndrop.DragOptions
import com.android.launcher3.dragndrop.DragView
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.pageindicators.PageIndicator
import com.android.launcher3.util.CustomizeOverlayEdgeEffect
import com.android.launcher3.util.EdgeEffectCompat
import com.android.systemui.plugins.shared.LauncherOverlayManager.LauncherOverlay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CustomizeWorkspace<T> @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : Workspace<T>(context, attrs, defStyleAttr) where T : View, T : PageIndicator {

    override fun onOverlayScrollChanged(scroll: Float) {
        mOverlayProgress = Utilities.boundToRange(scroll, -1f, 1f)
        val count = mOverlayCallbacks.size
        for (i in 0 until count) {
            mOverlayCallbacks[i].onOverlayScrollChanged(mOverlayProgress)
        }
    }

    override fun setLauncherOverlay(overlay: LauncherOverlay?) {
        val newEffect: EdgeEffectCompat
        var newRightEffect: EdgeEffectCompat? = null
        if (overlay == null) {
            newEffect = EdgeEffectCompat(context)
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

    override fun onDragStart(dragObject: DragObject?, options: DragOptions?) {
        super.onDragStart(dragObject, options)

        if (mLauncher.isInState(LauncherState.EDIT_MODE)) {
            mLauncher.stateManager.goToState(LauncherState.SPRING_LOADED)
        }
    }
}

private const val DRAG_MODE_NONE = 0
private const val DRAG_MODE_REORDER = 3

fun Workspace<*>.manageAutofillOnDragOver(
    d: DragObject, minSpanX: Int, minSpanY: Int, reorderX: Int, reorderY: Int
) {
    android.util.Log.d("OK.DEBUG", "manageAutofillOnDragOver")
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
        // Otherwise, if we aren't adding to or creating a folder and there's no pending
        // reorder, then we schedule a reorder
        val listener = ReorderAlarmListener(
            mDragViewVisualCenter,
            minSpanX, minSpanY, item.spanX, item.spanY, d, child
        )
        mReorderAlarm.setOnAlarmListener(listener)
        mReorderAlarm.setAlarm(REORDER_TIMEOUT.toLong())
    }
}
