package com.android.customize.overlay.ui.minus.view

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setPadding
import com.android.customize.common.extension.parseIntent
import com.android.customize.common.extension.px
import com.android.customize.overlay.model.PhotoInfo
import com.android.customize.overlay.model.WidgetInfo
import com.android.launcher3.Launcher
import com.android.launcher3.R

class PhotoWidgetView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    val imageView by lazy {
        ImageView(context).apply {
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }

    init {
        applyWidgetStyle()
        setPadding(px(R.dimen.photo_padding))

        addView(
            imageView, LayoutParams(
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

    fun bind(photoInfo: PhotoInfo) {
        tag = photoInfo.apply { bind(context) }
        imageView.setImageURI(photoInfo.uri)
    }
}