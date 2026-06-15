package com.android.customize.overlay.model

import com.android.launcher3.R
import java.util.Calendar

data class CalendarInfo(
    val currentMonth: Int,
    val currentYear: Int,
    val todayDay: Int,
    val todayMonth: Int,
    val todayYear: Int,
    val weekdays: List<String>,
    val days: List<Int?>
) : WidgetInfo() {
    override val cnOrAction: Int
        get() = R.string.config_calendarCnOrAction

    fun isToday(day: Int?): Boolean {
        return day == todayDay
                && currentMonth == todayMonth
                && currentYear == todayYear
    }

    fun isPastDay(day: Int?): Boolean {
        return if (day != null) {
            currentYear < todayYear ||
                    (currentYear == todayYear && currentMonth < todayMonth) ||
                    (currentYear == todayYear && currentMonth == todayMonth && day < todayDay)
        } else false
    }

    companion object {
        val WEEKDAYS = listOf("日", "一", "二", "三", "四", "五", "六")
        val DEFAULT = CalendarInfo(
            currentMonth = Calendar.getInstance().get(Calendar.MONTH),
            currentYear = Calendar.getInstance().get(Calendar.YEAR),
            todayDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
            todayMonth = Calendar.getInstance().get(Calendar.MONTH),
            todayYear = Calendar.getInstance().get(Calendar.YEAR),
            weekdays = WEEKDAYS,
            days = emptyList()
        )
    }
}