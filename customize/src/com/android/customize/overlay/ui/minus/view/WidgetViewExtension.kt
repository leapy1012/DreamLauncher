package com.android.customize.overlay.ui.minus.view

import android.view.View
import android.view.ViewGroup
import androidx.core.view.setMargins
import com.android.customize.common.extension.applyBackgroundStyle
import com.android.customize.common.extension.color
import com.android.customize.common.extension.px
import com.android.launcher3.R

fun View.applyWidgetStyle() {
    layoutParams = ViewGroup.MarginLayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply {
        setMargins(px(R.dimen.widget_margin) / 2)
    }
    applyBackgroundStyle(
        color(R.color.widget_bg, 0.3f),
        px(R.dimen.widget_bg_radius)
    )
}
