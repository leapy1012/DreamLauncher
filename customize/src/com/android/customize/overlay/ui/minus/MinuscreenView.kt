package com.android.customize.overlay.ui.minus

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.android.customize.common.lifecycle.findViewLifecycleOwner
import com.android.customize.overlay.lifecycle.OverlayViewLifecycleInitializer
import com.android.customize.overlay.ui.minus.page.WidgetListPage
import com.android.launcher3.touch.CustomizeSingleAxisSwipeDetector
import com.android.launcher3.touch.SingleAxisSwipeDetector
import com.android.launcher3.views.AbsSlideTouchView

class MinuscreenView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AbsSlideTouchView(context, attrs) {

    val widgetListPage by lazy {
        WidgetListPage(context)
    }

    init {
        OverlayViewLifecycleInitializer.init(this)

        rootView.addView(
            widgetListPage, LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
        )
    }

    override fun isOfType(type: Int): Boolean {
        return (type and TYPE_ON_BOARD_POPUP) != 0
    }

    override fun onBackInvoked() {
        val viewLifecycleOwner = findViewLifecycleOwner()
        viewLifecycleOwner?.onBackPressedDispatcher?.onBackPressed()
    }

    override val direction: SingleAxisSwipeDetector.Direction
        get() = CustomizeSingleAxisSwipeDetector.HORIZONTAL_RTL

    companion object {
        fun get(view: View): MinuscreenView {
            return view as? MinuscreenView ?: get(view.parent as View)
        }
    }
}