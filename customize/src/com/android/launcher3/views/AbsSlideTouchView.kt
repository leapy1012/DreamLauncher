package com.android.launcher3.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

abstract class AbsSlideTouchView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AbsSlideControllerView(context, attrs) {

    val rootView by lazy {
        FrameLayout(context)
    }

    init {
        addView(rootView, LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT,
        ))
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return onControllerInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        return onControllerTouchEvent(ev)
    }

    override fun isOfType(type: Int): Boolean {
        return false
    }
}