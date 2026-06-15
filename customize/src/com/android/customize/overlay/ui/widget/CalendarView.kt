package com.android.customize.overlay.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import com.android.customize.common.extension.baselineY
import com.android.customize.common.extension.withAlpha
import com.android.customize.overlay.model.CalendarInfo

class CalendarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var calendarInfo: CalendarInfo? = null

    fun setCalendarInfo(info: CalendarInfo) {
        calendarInfo = info
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val info = calendarInfo ?: return

        paint.typeface = Typeface.DEFAULT
        paint.textSize = height * 0.75f
        paint.color = Color.WHITE.withAlpha(0.1f)
        canvas.drawText(
            "${info.currentMonth + 1}"
                .padStart(2, '0'), width / 2f,
            height / 2f + paint.baselineY, paint
        )

        val weekdays = info.weekdays
        val days = info.days.chunked(7)

        val w = width / 7
        val h = height / (days.size + 1)

        var currentY = paddingTop.toFloat()

        paint.textSize = h * 0.6f
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.color = Color.LTGRAY

        currentY += h / 2 + paint.baselineY

        weekdays.forEachIndexed { index, weekday ->
            val x = paddingStart + index * w + w / 2f
            canvas.drawText(weekday, x, currentY, paint)
        }

        paint.typeface = Typeface.DEFAULT

        paint.textSize = h * 0.6f
        val baselineY = paint.baselineY

        days.forEach { daysInRow ->
            currentY += h
            daysInRow.forEachIndexed { index, day ->
                val x = paddingStart + index * w + w / 2f

                val isToday = info.isToday(day)
                val isPastDay = info.isPastDay(day)

                day ?: return@forEachIndexed
                when {
                    isToday -> {
                        paint.textSize = h * 0.6f
                        paint.color = Color.WHITE
                        bgPaint.color = Color.WHITE.withAlpha(0.4f)

                        canvas.drawText("$day", x, currentY + paint.baselineY, paint)
                        canvas.drawCircle(x, currentY, w / 2f, bgPaint)
                    }

                    isPastDay -> {
                        paint.color = Color.WHITE.withAlpha(0.2f)

                        canvas.drawText("$day", x, currentY + baselineY, paint)
                    }

                    else -> {
                        paint.textSize = h * 0.6f
                        paint.color = Color.WHITE.withAlpha(0.4f)

                        canvas.drawText("$day", x, currentY + baselineY, paint)
                    }
                }
            }
        }
    }
}