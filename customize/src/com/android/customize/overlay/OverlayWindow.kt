package com.android.customize.overlay

import android.graphics.PixelFormat
import android.view.WindowManager
import com.android.customize.common.logger.MyLogger
import com.android.launcher3.CustomizeLauncher
import com.android.launcher3.views.AbsSlideTouchView

class OverlayWindow(val decorView: AbsSlideTouchView) : OverlayBase() {

    private val context by lazy { decorView.context }
    private val wmLp by lazy { createWmLp() }
    private val windowManager: WindowManager by lazy {
        context.getSystemService(WindowManager::class.java)
    }

    override fun addView(launcher: CustomizeLauncher) {
        decorView.observeProgress {
            callbacks?.onOverlayScrollChanged(it)
            wmLp.x = if (it < 0.01) -decorView.width else 0
            windowManager.updateViewLayout(decorView, wmLp)
        }

        try {
            myLogger.d("addView")
            val lp = launcher.window.attributes
            wmLp.token = lp.token
            windowManager.addView(decorView, wmLp)
            decorView.close(false)
        } catch (e: Exception) {
            myLogger.e("addView", e)
            windowManager.removeViewImmediate(decorView)
            windowManager.addView(decorView, wmLp)
        }
    }

    override fun removeView() {
        try {
            myLogger.d("removeView")
            windowManager.removeViewImmediate(decorView)
        } catch (e: Exception) {
            myLogger.e("removeView", e)
        }
    }

    override fun onScrollInteractionBegin() {
        myLogger.d("onScrollInteractionBegin")
    }

    override fun onScrollInteractionEnd() {
        myLogger.d("onScrollInteractionEnd")
        decorView.animateOpen()
    }

    override fun onScrollChange(progress: Float, rtl: Boolean) {
        decorView.setProgress(progress, false)
    }

    companion object {
        private val myLogger = MyLogger("OverlayWindow")
        private fun createWmLp(): WindowManager.LayoutParams {
            return WindowManager.LayoutParams().apply {
                format = PixelFormat.RGBA_8888
                type = WindowManager.LayoutParams.TYPE_APPLICATION
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
            }
        }
    }
}