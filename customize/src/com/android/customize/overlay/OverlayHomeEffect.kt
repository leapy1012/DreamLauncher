package com.android.customize.overlay

import com.android.launcher3.CustomizeLauncher
import com.android.launcher3.views.BaseDragLayer

/**
 * Home effect while Quick Glance scrolls.
 *
 * Oppo [OverlayAnimManager] does NOT paint a DragLayer ColorDrawable frost.
 * Mid-swipe light-blue wash is the Assist window's fixed DecorView plate
 * (BackgroundController alpha) compositing above the launcher. DragLayer scale
 * runs only when overlay blur is available; we keep scale at 1.0.
 *
 * This class is intentionally a no-op for color frost — a DragLayer foreground
 * sits under TYPE_APPLICATION_PANEL and cannot produce the Oppo wash.
 *
 * Home slide translation stays disabled
 * ([CustomizeLauncher.shouldTranslateDragLayerForOverlay] = false).
 */
class OverlayHomeEffect(private val launcher: CustomizeLauncher) {

    private var lastProgress = -1f

    fun onOverlayScrollChanged(progress: Float) {
        val p = progress.coerceIn(0f, 1f)
        if (absEq(lastProgress, p)) return
        lastProgress = p

        val dragLayer = launcher.dragLayer as? BaseDragLayer<*> ?: return
        // Match Oppo non-blur path: leave DragLayer at identity scale.
        dragLayer.scaleX = 1f
        dragLayer.scaleY = 1f
    }

    fun reset() {
        lastProgress = -1f
        val dragLayer = launcher.dragLayer as? BaseDragLayer<*> ?: return
        dragLayer.scaleX = 1f
        dragLayer.scaleY = 1f
    }

    private fun absEq(a: Float, b: Float): Boolean = kotlin.math.abs(a - b) < 0.001f
}
