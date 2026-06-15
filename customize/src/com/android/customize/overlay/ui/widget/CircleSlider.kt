package com.android.customize.overlay.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.android.launcher3.R

class CircularSlider @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var progress: Float = 0f
    private var strokeWidthPx: Float = 0f
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectF = RectF()

    init {
        strokeWidthPx = dpToPx(6)
        backgroundPaint.color = ContextCompat.getColor(context, R.color.battery_progress_inactive)
        backgroundPaint.style = Paint.Style.STROKE
        backgroundPaint.strokeWidth = strokeWidthPx
        backgroundPaint.strokeCap = Paint.Cap.ROUND

        progressPaint.color = ContextCompat.getColor(context, R.color.battery_progress_active)
        progressPaint.style = Paint.Style.STROKE
        progressPaint.strokeWidth = strokeWidthPx
        progressPaint.strokeCap = Paint.Cap.ROUND
    }

    fun setProgress(progress: Float) {
        this.progress = progress.coerceIn(0f, 1f)
        invalidate()
    }

    fun setProgressColor(color: Int) {
        progressPaint.color = ContextCompat.getColor(context, color)
    }

    fun setStrokeWidth(dp: Int) {
        strokeWidthPx = dpToPx(dp)
        backgroundPaint.strokeWidth = strokeWidthPx
        progressPaint.strokeWidth = strokeWidthPx
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = MeasureSpec.getSize(widthMeasureSpec)
            .coerceAtMost(MeasureSpec.getSize(heightMeasureSpec))
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (minOf(width, height) - strokeWidthPx) / 2f

        rectF.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)

        val totalSweepAngle = 360f * progress
        val ccwProgressSweep = -totalSweepAngle
        val ccwBackgroundSweep = -(360f - totalSweepAngle)

        canvas.drawArc(
            rectF,
            0f + ccwProgressSweep,
            ccwBackgroundSweep,
            false,
            backgroundPaint
        )

        canvas.drawArc(
            rectF,
            0f,
            ccwProgressSweep,
            false,
            progressPaint
        )
    }

    private fun dpToPx(dp: Int): Float {
        return dp * resources.displayMetrics.density
    }
}
