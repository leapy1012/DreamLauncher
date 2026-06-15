package com.android.customize.common.extension

import androidx.annotation.FloatRange
import androidx.core.graphics.ColorUtils

fun Int.withAlpha(@FloatRange(from = 0.0, to = 1.0) alpha: Float): Int {
    return ColorUtils.setAlphaComponent(
        this, (alpha * 255).toInt()
    )
}