package com.android.customize.overlay

import com.android.customize.common.logger.MyLogger
import com.android.customize.overlay.preference.OverlayPreference
import com.android.customize.overlay.quickglance.QuickGlanceRemoteOverlay
import com.android.customize.overlay.ui.plus.PluscreenView
import com.android.launcher3.CustomizeLauncher
import com.android.systemui.plugins.shared.LauncherOverlayManager.LauncherOverlayCallbacks

/**
 * Combines minus (via [OverlayProxy]) and optional plus overlays.
 */
class OverlayCombine() : OverlayBase() {

    var overlayMinus: OverlayBase? = null
    var overlayPlus: OverlayDragSource? = null

    val quickGlanceOverlay: QuickGlanceRemoteOverlay?
        get() = overlayMinus as? QuickGlanceRemoteOverlay

    override fun addView(launcher: CustomizeLauncher) {
        val preference = OverlayPreference.get(launcher)
        overlayMinus = OverlayProxy.createMinus(launcher)
        OverlayProxy.wireLocalMinusProgress(overlayMinus, callbacks)

        overlayPlus = if (preference.plusEnabled) {
            val view = PluscreenView(launcher)
            view.observeProgress {
                callbacks?.onOverlayScrollChanged(-it)
            }
            OverlayDragSource(view).also {
                it.addView(launcher)
            }
        } else {
            null
        }

        setOverlayCallbacks(callbacks)
    }

    override fun removeView() {
        overlayPlus?.removeView()
        overlayPlus = null
        overlayMinus?.removeView()
        overlayMinus = null
    }

    override fun onScrollInteractionBegin() {
        myLogger.d("onScrollInteractionBegin: $swipeRtl")
        if (swipeRtl) {
            overlayPlus?.onScrollInteractionBegin()
        } else {
            overlayMinus?.onScrollInteractionBegin()
        }
    }

    override fun onScrollInteractionEnd() {
        myLogger.d("onScrollInteractionEnd: $swipeRtl")
        if (swipeRtl) {
            overlayPlus?.onScrollInteractionEnd()
        } else {
            overlayMinus?.onScrollInteractionEnd()
        }
    }

    override fun onScrollInteractionEndWithVelocity(velocityPx: Float) {
        myLogger.d("onScrollInteractionEndWithVelocity: $swipeRtl v=$velocityPx")
        if (swipeRtl) {
            overlayPlus?.onScrollInteractionEndWithVelocity(velocityPx)
        } else {
            overlayMinus?.onScrollInteractionEndWithVelocity(velocityPx)
        }
    }

    override fun onScrollChange(progress: Float, rtl: Boolean) {
        swipeRtl = rtl
        if (rtl) {
            overlayPlus?.onScrollChange(progress, rtl)
        } else {
            overlayMinus?.onScrollChange(progress, rtl)
        }
    }

    override fun setOverlayCallbacks(callbacks: LauncherOverlayCallbacks?) {
        super.setOverlayCallbacks(callbacks)
        overlayPlus?.setOverlayCallbacks(callbacks)
        overlayMinus?.setOverlayCallbacks(callbacks)
        OverlayProxy.wireLocalMinusProgress(overlayMinus, callbacks)
    }

    companion object {
        private val myLogger = MyLogger("OverlayCombine")
    }
}
