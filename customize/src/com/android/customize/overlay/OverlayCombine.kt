package com.android.customize.overlay

import com.android.customize.common.logger.MyLogger
import com.android.customize.overlay.preference.OverlayPreference
import com.android.customize.overlay.ui.minus.MinuscreenView
import com.android.customize.overlay.ui.plus.PluscreenView
import com.android.launcher3.CustomizeLauncher
import com.android.systemui.plugins.shared.LauncherOverlayManager.LauncherOverlayCallbacks

class OverlayCombine() : OverlayBase() {

    var overlayMinus: OverlayDragSource? = null
    var overlayPlus: OverlayDragSource? = null

    override fun addView(launcher: CustomizeLauncher) {
        val preference = OverlayPreference.get(launcher)
        overlayMinus = if (preference.minusEnabled) {
            val view = MinuscreenView(launcher)
            view.observeProgress {
                callbacks?.onOverlayScrollChanged(it)
            }
            OverlayDragSource(view).also {
                it.addView(launcher)
            }
        } else {
            null
        }

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

    override fun onScrollChange(progress: Float, rtl: Boolean) {
        if (swipeRtl) {
            overlayPlus?.onScrollChange(progress, rtl)
        } else {
            overlayMinus?.onScrollChange(progress, rtl)
        }
    }

    override fun setOverlayCallbacks(callbacks: LauncherOverlayCallbacks?) {
        super.setOverlayCallbacks(callbacks)
        overlayPlus?.setOverlayCallbacks(callbacks)
        overlayMinus?.setOverlayCallbacks(callbacks)
    }

    companion object {
        private val myLogger = MyLogger("OverlayCombine")
    }
}