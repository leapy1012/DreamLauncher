package com.android.launcher3.util

import android.content.Context
import com.android.customize.overlay.OverlayBase
import com.android.systemui.plugins.shared.LauncherOverlayManager

class CustomizeOverlayEdgeEffect(
    context: Context,
    overlay: LauncherOverlayManager.LauncherOverlay,
    val swipeRtl: Boolean
) : OverlayEdgeEffect(context, overlay) {
    override fun onPullDistance(deltaDistance: Float, displacement: Float): Float {
        if (mOverlay is OverlayBase) {
            mOverlay.swipeRtl = swipeRtl
        }
        return super.onPullDistance(deltaDistance, displacement)
    }
}