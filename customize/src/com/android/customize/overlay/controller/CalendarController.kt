package com.android.customize.overlay.controller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.android.customize.overlay.model.CalendarInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Calendar

class CalendarController(private val context: Context) {

    val calendarFlow: Flow<CalendarInfo> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                trySend(getCalendarInfo(Calendar.getInstance()))
            }
        }

        trySend(getCalendarInfo(Calendar.getInstance()))

        context.registerReceiver(receiver, IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        })

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }

    private fun getCalendarInfo(calendar: Calendar): CalendarInfo {
        val tempCalendar = calendar.clone() as Calendar
        tempCalendar.set(Calendar.DAY_OF_MONTH, 1)

        val startingDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val gridItems = mutableListOf<Int?>()

        repeat(startingDayOfWeek) {
            gridItems.add(null)
        }

        (1..daysInMonth).forEach { day ->
            gridItems.add(day)
        }

        val totalCells = startingDayOfWeek + daysInMonth
        val neededCells = 42
        val paddingDaysAfter = neededCells - totalCells
        if (paddingDaysAfter >= 0) {
            repeat(paddingDaysAfter) {
                gridItems.add(null)
            }
        }

        return CalendarInfo(
            currentMonth = calendar.get(Calendar.MONTH),
            currentYear = calendar.get(Calendar.YEAR),
            todayDay = calendar.get(Calendar.DAY_OF_MONTH),
            todayMonth = calendar.get(Calendar.MONTH),
            todayYear = calendar.get(Calendar.YEAR),
            weekdays = CalendarInfo.WEEKDAYS,
            days = gridItems
        )
    }
}