package com.android.customize.overlay
import android.view.View
import android.view.ViewGroup
import com.android.launcher3.R
import com.android.customize.common.logger.MyLogger
import com.android.launcher3.CustomizeLauncher
import com.android.launcher3.views.AbsSlideTouchView

class OverlayDragSource(
    val decorView: AbsSlideTouchView
) : OverlayBase() {

    override fun addView(launcher: CustomizeLauncher) {
        myLogger.d("addView")

        launcher.rootView.addView(
            decorView,
            ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        )
        decorView.post { decorView.close(false) }
    }

    override fun removeView() {
        myLogger.d("removeView")
        val parent = decorView.parent as? ViewGroup
        parent?.removeView(decorView)
    }

    override fun onScrollInteractionBegin() {
        myLogger.d("onScrollInteractionBegin")
        val view = decorView.findViewById<View?>(R.id.search_bar_container)
        view?.visibility = View.VISIBLE
    }

    override fun onScrollInteractionEnd() {
        myLogger.d("onScrollInteractionEnd")
        decorView.animateOpen()
    }

    override fun onScrollChange(progress: Float, rtl: Boolean) {
        decorView.setProgress(progress, false)
    }

    companion object {
        private val myLogger = MyLogger("OverlayDragSource")
    }
}
