package com.android.customize.overlay

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.android.launcher3.CustomizeLauncher
import com.android.launcher3.views.BaseDragLayer
import kotlin.math.pow

/**
 * ColorOS-style home frost while Quick Glance scrolls.
 *
 * Do **not** translate/scale Workspace or Hotseat on the progress hot path — that fights
 * the remote overlay panel (and AIDL round-trip echo) and shakes every icon.
 * Dim only via DragLayer foreground.
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

        val frost = p.toDouble().pow(1.1).toFloat()
        dragFrost?.alpha = (frost * SCRIM_MAX_ALPHA * 255f).toInt().coerceIn(0, 255)

        if (p <= 0f) {
            dragFrost?.alpha = 0
        }
    }

    fun reset() {
        lastProgress = -1f
        val dragLayer = launcher.dragLayer as? BaseDragLayer<*>
        if (dragLayer != null && dragLayer.foreground === dragFrost) {
            dragLayer.foreground = null
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
        private const val SCRIM_MAX_ALPHA = 0.55f
        private val SCRIM_COLOR = Color.rgb(12, 16, 30)
    }
}
