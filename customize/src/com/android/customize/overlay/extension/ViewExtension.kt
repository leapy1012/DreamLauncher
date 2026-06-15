package com.android.customize.overlay.extension

import android.view.View
import com.android.customize.common.lifecycle.ViewLifecycleOwner
import com.android.customize.common.lifecycle.findViewLifecycleOwner

fun View.overlayScopeObserve(observe: ViewLifecycleOwner.() -> Unit) {
    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            findViewLifecycleOwner()?.also { observe.invoke(it) }
        }

        override fun onViewDetachedFromWindow(v: View) {
            removeOnAttachStateChangeListener(this)
        }
    })
}