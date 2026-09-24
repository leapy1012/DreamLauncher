package com.android.customize.overlay

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.android.launcher3.CustomizeLauncher
import com.android.launcher3.views.BaseDragLayer
import kotlin.math.pow

/**
 * Home effect while Quick Glance scrolls.
 *
 * Oppo [OverlayAnimManager] only creates the DragLayer scale anim when
 * workspace overlay blur is available; otherwise scale stays at 1.0. We do not
 * have that blur path, so scaling DragLayer left a visible "scaled icons vs
 * full-bleed wallpaper" frame. Keep frost only; no scale.
 *
 * Home slide translation stays disabled
 * ([CustomizeLauncher.shouldTranslateDragLayerForOverlay] = false).
 */
class OverlayHomeEffect(private val launcher: CustomizeLauncher) {

    private var lastProgress = -1f
    private var dragFrost: ColorDrawable? = null

    fun onOverlayScrollChanged(progress: Float) {
        val p = progress.coerceIn(0f, 1f)
        if (absEq(lastProgress, p)) return
        lastProgress = p

        val dragLayer = launcher.dragLayer as? BaseDragLayer<*> ?: return
        ensureFrost(dragLayer)

        // Match Oppo non-blur path: leave DragLayer at identity scale.
        dragLayer.scaleX = 1f
        dragLayer.scaleY = 1f

        // Translucent sheet over still-visible icons (gesture-reactive).
        val frost = p.toDouble().pow(0.9).toFloat()
        dragFrost?.alpha = (frost * SCRIM_MAX_ALPHA * 255f).toInt().coerceIn(0, 255)

        if (p <= 0f) {
            dragFrost?.alpha = 0
        }
    }

    fun reset() {
        lastProgress = -1f
        val dragLayer = launcher.dragLayer as? BaseDragLayer<*>
        if (dragLayer != null) {
            dragLayer.scaleX = 1f
            dragLayer.scaleY = 1f
            if (dragLayer.foreground === dragFrost) {
                dragLayer.foreground = null
            }
        }
        dragFrost = null
    }

    private fun ensureFrost(dragLayer: BaseDragLayer<*>) {
        if (dragFrost != null && dragLayer.foreground === dragFrost) return
        val fg = ColorDrawable(SCRIM_COLOR).apply { alpha = 0 }
        dragLayer.foreground = fg
        dragFrost = fg
    }

    private fun absEq(a: Float, b: Float): Boolean = kotlin.math.abs(a - b) < 0.001f

    companion object {
        /** Matches Oppo mid-swipe light-blue sheet over home icons. */
        private const val SCRIM_MAX_ALPHA = 0.42f
        private val SCRIM_COLOR = Color.rgb(0x83, 0x97, 0xCC)
    }
}
