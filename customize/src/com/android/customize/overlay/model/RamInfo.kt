package com.android.customize.overlay.model

import android.annotation.SuppressLint
import com.android.launcher3.R
import kotlin.math.roundToInt

data class RamInfo(
    val free: Long,
    val total: Long,
    val freeDesc: String,
    val totalDesc: String
) : WidgetInfo() {
    override val cnOrAction: Int
        get() = R.string.config_ramCnOrAction

    val progress
        get() = if (total <= 0) 0f
        else free.toFloat() / total

    val freeGB: String
        get() = freeDesc

    val totalGB: String
        get() = totalDesc

    companion object {
        val DEFAULT = RamInfo(0, 0, "", "")

        @SuppressLint("DefaultLocale")
        private fun Long.asGB(): String {
            val gb = toFloat() / 1024 / 1024 / 1024
            return "${gb.roundToInt()}GB"
        }
    }
}