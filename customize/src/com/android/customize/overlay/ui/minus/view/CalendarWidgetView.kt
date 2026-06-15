package com.android.customize.overlay.ui.minus.view

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setPadding
import com.android.customize.common.extension.parseIntent
import com.android.customize.common.extension.px
import com.android.customize.overlay.model.CalendarInfo
import com.android.customize.overlay.model.WidgetInfo
import com.android.customize.overlay.ui.widget.CalendarView
import com.android.launcher3.Launcher
import com.android.launcher3.R

class CalendarWidgetView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    val calendarView by lazy {
        CalendarView(context)
    }

    init {
        applyWidgetStyle()
        setPadding(px(R.dimen.calendar_padding))

        addView(
            calendarView, LayoutParams(
                LayoutParams.MATCH_CONSTRAINT,
                LayoutParams.MATCH_CONSTRAINT
            ).apply {
                dimensionRatio = "w,1:1"
                startToStart = LayoutParams.PARENT_ID
                topToTop = LayoutParams.PARENT_ID
                bottomToBottom = LayoutParams.PARENT_ID
                endToEnd = LayoutParams.PARENT_ID
            })

        setOnClickListener {
            val info = tag as WidgetInfo
            val launcher = Launcher.getLauncher(context)
            launcher.startActivitySafely(this, info.intent, null)
        }
    }

    fun bind(calendarInfo: CalendarInfo) {
        tag = calendarInfo.apply { bind(context) }
        calendarView.setCalendarInfo(calendarInfo)
    }
}