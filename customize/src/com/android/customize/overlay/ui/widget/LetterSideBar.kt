package com.android.customize.overlay.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.android.customize.common.extension.withAlpha
import com.android.launcher3.R

class LetterSideBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    var onLetterChange: ((String) -> Unit)? = null

    private val letters by lazy {
        listOf(
            "A", "B", "C", "D", "E", "F", "G",
            "H", "I", "J", "K", "L", "M", "N",
            "O", "P", "Q", "R", "S", "T",
            "U", "V", "W", "X", "Y", "Z", "#"
        )
    }

    var activeLetters: List<String> = letters
        set(value) {
            field = value
            invalidate()
        }

    private var wavePosition: Int? = null
        set(value) {
            value?.also { onLetterChange?.invoke(letters[it]) }
            field = value
            invalidate()
        }

    private val paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }
    }

    private val imagePaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }
    }

    private val letterSize
        get() = (height - paddingTop - paddingBottom) / (letters.size + 1)

    private val waveBackground = ContextCompat
        .getDrawable(context, R.drawable.icc_letter_bg)!!

    private val waveBackgroundBmp by lazy {
        val drawable = ContextCompat.getDrawable(context, R.drawable.icc_letter_bg)!!
        drawable.setTint(imagePaint.color)
        val aspectRatio = drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight
        val bitmap = createBitmap((letterSize * 2 * aspectRatio).toInt(), letterSize * 2)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val letterSize = (heightSize - paddingTop - paddingBottom) / (letters.size + 1)
        val aspectRatio = waveBackground.intrinsicWidth.toFloat() / waveBackground.intrinsicHeight
        val waveLetterW = letterSize * 2 * aspectRatio
        setMeasuredDimension(
            (waveLetterW + letterSize + paddingLeft + paddingRight).toInt(), heightSize
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val startY = letterSize.toFloat() + paddingTop

        paint.textSize = letterSize * 0.6f
        paint.textAlign = Paint.Align.RIGHT
        for (i in letters.indices) {
            val letter = letters[i]
            paint.color = if (activeLetters.contains(letter)) Color.WHITE
            else Color.WHITE.withAlpha(0.5f)

            val currentY = startY + letterSize * i
            val fm = paint.fontMetrics
            val drawLetterH = (fm.descent - fm.ascent) / 2f - fm.descent
            val baseline = currentY + drawLetterH
            canvas.drawText(letter, width.toFloat() - paddingRight, baseline, paint)
        }

        wavePosition?.also {
            paint.textSize = letterSize.toFloat()
            paint.textAlign = Paint.Align.CENTER

            val currentY = startY + letterSize * it
            val fm = paint.fontMetrics
            val drawLetterH = (fm.descent - fm.ascent) / 2 - fm.descent
            val baseline = currentY + drawLetterH
            canvas.drawText(
                letters[it],
                waveBackgroundBmp.height / 2f + paddingLeft,
                baseline,
                paint
            )
            canvas.drawBitmap(
                waveBackgroundBmp,
                paddingLeft.toFloat(), currentY - waveBackgroundBmp.height / 2f,
                imagePaint
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                wavePosition = null
                invalidate()
                return true
            }

            else -> {
                if (event.action == MotionEvent.ACTION_DOWN &&
                    event.x < width - letterSize
                ) {
                    wavePosition = null
                    invalidate()
                    return false
                }

                val currentY = event.y - letterSize.toFloat() - paddingTop
                val newPosition = (currentY / letterSize).toInt()
                if (newPosition in letters.indices) {
                    val letter = letters[newPosition]
                    if (!activeLetters.contains(letter)) {
                        wavePosition = null
                        invalidate()
                        return true
                    }
                    if (newPosition != wavePosition) {
                        wavePosition = newPosition
                        invalidate()
                        return true
                    }
                }
            }
        }
        return true
    }
}