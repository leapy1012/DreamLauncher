package com.android.customize.overlay.ui.minus.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setMargins
import androidx.core.view.setPadding
import com.android.customize.common.extension.color
import com.android.customize.common.extension.parseIntent
import com.android.customize.common.extension.px
import com.android.customize.common.extension.pxf
import com.android.customize.overlay.model.RamInfo
import com.android.customize.overlay.model.WidgetInfo
import com.android.customize.overlay.ui.widget.CircularSlider
import com.android.launcher3.Launcher
import com.android.launcher3.R

class RamWidgetView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    val circleSlider by lazy {
        CircularSlider(context).apply {
            id = generateViewId()
            setProgressColor(R.color.ram_progress_active)
        }
    }

    val smartphoneIcon by lazy {
        ImageView(context).apply {
            id = generateViewId()
            setImageResource(R.drawable.icc_memory)
            setColorFilter(color(R.color.ram_phone_icon))
        }
    }

    val batteryPercentage by lazy {
        TextView(context).apply {
            id = generateViewId()
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )
            setTextColor(color(R.color.ram_percentage))
            setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                pxf(R.dimen.ram_percentage_size)
            )
        }
    }

    init {
        applyWidgetStyle()
        setPadding(px(R.dimen.ram_padding))

        addView(
            circleSlider, LayoutParams(
                LayoutParams.MATCH_CONSTRAINT,
                LayoutParams.MATCH_CONSTRAINT
            ).apply {
                dimensionRatio = "w,1:1"
                startToStart = LayoutParams.PARENT_ID
                topToTop = LayoutParams.PARENT_ID
                bottomToBottom = LayoutParams.PARENT_ID
                endToEnd = LayoutParams.PARENT_ID
                setMargins(px(R.dimen.ram_progress_margin))
            })

        val iconSize = px(R.dimen.ram_phone_icon_size)
        addView(
            smartphoneIcon, LayoutParams(
                iconSize, iconSize
            ).apply {
                startToStart = LayoutParams.PARENT_ID
                topToTop = circleSlider.id
                bottomToTop = batteryPercentage.id
                endToEnd = LayoutParams.PARENT_ID
                topMargin = px(R.dimen.ram_phone_icon_margin)
            })

        addView(
            batteryPercentage, LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                startToStart = LayoutParams.PARENT_ID
                topToBottom = smartphoneIcon.id
                bottomToBottom = circleSlider.id
                endToEnd = LayoutParams.PARENT_ID
                bottomMargin = px(R.dimen.ram_percentage_margin)
            })

        setOnClickListener {
            val info = tag as WidgetInfo
            val launcher = Launcher.getLauncher(context)
            launcher.startActivitySafely(this, info.intent, null)
        }
    }

    @SuppressLint("SetTextI18n")
    fun bind(ramInfo: RamInfo) {
        tag = ramInfo.apply { bind(context) }
        circleSlider.setProgress(ramInfo.progress)
        batteryPercentage.text = "${ramInfo.freeGB}/${ramInfo.totalGB}"
    }
}
