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
import com.android.customize.common.extension.px
import com.android.customize.common.extension.pxf
import com.android.customize.overlay.model.BatteryInfo
import com.android.customize.overlay.model.WidgetInfo
import com.android.customize.overlay.ui.widget.CircularSlider
import com.android.launcher3.Launcher
import com.android.launcher3.R

class BatteryWidgetView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    val circleSlider by lazy {
        CircularSlider(context).apply {
            id = generateViewId()
        }
    }

    val icon by lazy {
        ImageView(context).apply {
            id = generateViewId()
            setImageResource(R.drawable.icc_battery)
            setColorFilter(color(R.color.battery_phone_icon))
        }
    }

    val batteryPercentage by lazy {
        TextView(context).apply {
            id = generateViewId()
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )
            setTextColor(color(R.color.battery_percentage))
            setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                pxf(R.dimen.battery_percentage_size)
            )
        }
    }

    init {
        applyWidgetStyle()
        setPadding(px(R.dimen.battery_padding))

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
                setMargins(px(R.dimen.battery_progress_margin))
            })

        val iconSize = px(R.dimen.battery_phone_icon_size)
        addView(
            icon, LayoutParams(
                iconSize, iconSize
            ).apply {
                startToStart = LayoutParams.PARENT_ID
                topToTop = circleSlider.id
                bottomToTop = batteryPercentage.id
                endToEnd = LayoutParams.PARENT_ID
                topMargin = px(R.dimen.battery_phone_icon_margin)
            })

        addView(
            batteryPercentage, LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                startToStart = LayoutParams.PARENT_ID
                topToBottom = icon.id
                bottomToBottom = circleSlider.id
                endToEnd = LayoutParams.PARENT_ID
                bottomMargin = px(R.dimen.battery_percentage_margin)
            })

        setOnClickListener {
            val info = tag as WidgetInfo
            val launcher = Launcher.getLauncher(context)
            launcher.startActivitySafely(this, info.intent, null)
        }
    }

    @SuppressLint("SetTextI18n")
    fun bind(batteryInfo: BatteryInfo) {
        tag = batteryInfo.apply { bind(context) }
        circleSlider.setProgress(batteryInfo.progress)
        batteryPercentage.text = "${(batteryInfo.progress * 100).toInt()}%"
    }
}
