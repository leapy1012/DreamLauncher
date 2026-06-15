package com.android.customize.common.extension

import android.graphics.Outline
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewOutlineProvider
import androidx.core.content.ContextCompat
import com.android.customize.common.lifecycle.ViewLifecycleOwner
import com.android.customize.common.lifecycle.findViewLifecycleOwner

fun View.px(id: Int): Int {
    return resources.getDimensionPixelSize(id)
}

fun View.pxf(id: Int): Float {
    return resources.getDimension(id)
}

fun View.color(id: Int, alpha: Float = 1f): Int {
    return ContextCompat.getColor(context, id).withAlpha(alpha)
}

fun View.drawable(id: Int): Drawable? {
    return ContextCompat.getDrawable(context, id)
}

fun View.applyBackgroundStyle(color: Int, radius: Int) {
    clipToOutline = true
    outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(
                0, 0,
                view.width, view.height,
                radius.toFloat()
            )
        }
    }
    setBackgroundColor(color)
}

fun View.viewScopeObserve(observe: ViewLifecycleOwner.() -> Unit) {
    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            findViewLifecycleOwner()?.also { observe.invoke(it) }
        }

        override fun onViewDetachedFromWindow(v: View) {
            removeOnAttachStateChangeListener(this)
        }
    })
}