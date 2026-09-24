package com.android.launcher3.util

import android.content.Context
import com.android.systemui.plugins.shared.LauncherOverlayManager

/**
 * Overlay holder for Quick Glance / plus. Progress is driven exclusively by
 * [com.android.launcher3.CustomizeWorkspace] (Oppo overScroll amount/width).
 *
 * EdgeEffect pull/release are no-ops. [isFinished] / [getDistance] stay inert so
 * PagedView does not treat leftover overlay progress as an active edge glow
 * (that would set mIsBeingDragged on DOWN and swallow icon clicks).
 */
class CustomizeOverlayEdgeEffect(
    context: Context,
    overlay: LauncherOverlayManager.LauncherOverlay,
    val swipeRtl: Boolean
) : OverlayEdgeEffect(context, overlay) {

    val launcherOverlay: LauncherOverlayManager.LauncherOverlay
        get() = mOverlay

    override fun finish() {
        mDistance = 0f
        mIsScrolling = false
    }

    override fun getDistance(): Float = 0f

    override fun isFinished(): Boolean = true

    override fun onPullDistance(deltaDistance: Float, displacement: Float): Float {
        // Oppo: ignore EdgeEffect pull entirely.
        return 0f
    }

    override fun onRelease() {
        // Session end is owned by CustomizeWorkspace.endOverlayScrollIfNeeded.
        mIsScrolling = false
    }
}
