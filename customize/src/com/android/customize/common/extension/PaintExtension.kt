package com.android.customize.common.extension

import android.graphics.Paint

val Paint.baselineY
    get() = fontMetrics.run { (descent - ascent) / 2f - descent }