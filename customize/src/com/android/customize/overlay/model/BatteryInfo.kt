package com.android.customize.overlay.model

import com.android.launcher3.R

data class BatteryInfo(
    val progress: Float,
    val isCharging: Boolean
) : WidgetInfo() {
    override val cnOrAction: Int
        get() = R.string.config_batteryCnOrAction

    companion object {
        val DEFAULT = BatteryInfo(0f, false)
    }
}