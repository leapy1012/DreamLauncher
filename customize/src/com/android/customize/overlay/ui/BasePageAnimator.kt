package com.android.customize.overlay.ui

import android.view.View

interface BasePageAnimator {
    fun open(animate: Boolean, anchor: View? = null)
    fun close(animate: Boolean)
}