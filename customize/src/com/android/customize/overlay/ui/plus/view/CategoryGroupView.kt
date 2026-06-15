package com.android.customize.overlay.ui.plus.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.setMargins
import com.android.customize.common.extension.applyBackgroundStyle
import com.android.customize.common.extension.color
import com.android.customize.common.extension.px
import com.android.customize.common.extension.pxf
import com.android.customize.overlay.util.BitmapUtil
import com.android.launcher3.BubbleTextView
import com.android.launcher3.CheckLongPressHelper
import com.android.launcher3.R
import com.android.launcher3.dragndrop.DraggableView
import com.android.launcher3.icons.BitmapInfo
import com.android.launcher3.model.data.FolderInfo
import com.android.launcher3.model.data.WorkspaceItemInfo
import kotlin.math.min

@SuppressLint("VisibleForTests")
@OptIn(ExperimentalStdlibApi::class)
class CategoryGroupView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs), DraggableView {

    val icons by lazy {
        Array(4) {
            LayoutInflater.from(context).inflate(
                R.layout.category_app_icon,
                this, false
            ) as BubbleTextView
        }.apply {
            forEach {
                it.id = generateViewId()
                it.setTextVisibility(false)
            }
        }
    }

    val name by lazy {
        TextView(context).apply {
            setTextColor(Color.WHITE)
            setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                pxf(R.dimen.category_name_size)
            )
            setCompoundDrawablePadding(0)
        }
    }

    val folderBackground by lazy {
        View(context).apply {
            id = generateViewId()
            applyBackgroundStyle(
                color(R.color.category_folder_bg, 0.3f),
                px(R.dimen.category_bg_radius)
            )
        }
    }

    private val checkLongPressHelper = CheckLongPressHelper(this)

    init {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(px(R.dimen.category_margin) / 2)
        }

        addView(
            folderBackground, LayoutParams(
                LayoutParams.MATCH_CONSTRAINT,
                LayoutParams.MATCH_CONSTRAINT
            ).apply {
                dimensionRatio = "w,1:1"
                topToTop = LayoutParams.PARENT_ID
                startToStart = LayoutParams.PARENT_ID
                endToEnd = LayoutParams.PARENT_ID
            })

        val iconSize = icons.first().iconSize
        val iconMargin = px(R.dimen.category_icon_margin)
        icons.forEachIndexed { index, iconView ->
            val i = index % 2
            val j = index / 2

            val layoutParams = LayoutParams(
                iconSize, iconSize
            ).apply {
                dimensionRatio = "w,1:1"
                matchConstraintPercentWidth = 0.5f

                if (j == 0) {
                    topToTop = folderBackground.id
                } else {
                    bottomToBottom = folderBackground.id
                }

                if (i == 0) {
                    startToStart = folderBackground.id
                } else {
                    endToEnd = folderBackground.id
                }
                setMargins(iconMargin)
            }
            addView(iconView, layoutParams)
        }

        addView(
            name,
            LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                topToBottom = folderBackground.id
                startToStart = LayoutParams.PARENT_ID
                endToEnd = LayoutParams.PARENT_ID
                bottomToBottom = LayoutParams.PARENT_ID
                topMargin = px(R.dimen.category_name_margin)
            })
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun bind(folderInfo: FolderInfo) {
        name.text = folderInfo.title
        val itemInfos = folderInfo.contents

        icons.forEachIndexed { index, icon ->
            val appInfo = itemInfos.getOrNull(index)
            if (appInfo != null) {
                icon.applyFromWorkspaceItem(appInfo)
                icon.setIconVisible(true)
                icon.isVisible = true
            } else {
                icon.isVisible = false
            }
        }

        if (itemInfos.size > 4) {
            val itemInfo = WorkspaceItemInfo()
            val bitmaps = itemInfos
                .slice(3..<min(7, itemInfos.size))
                .mapNotNull { it.bitmap.icon }
                .toTypedArray()
            val previewBitmap = BitmapUtil.createFolderPreviewBitmap(bitmaps)
            itemInfo.bitmap = BitmapInfo(previewBitmap, Color.TRANSPARENT)
            icons.last().applyFromWorkspaceItem(itemInfo)
        }

        tag = folderInfo
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        checkLongPressHelper.onTouchEvent(event)
        return true
    }

    override fun cancelLongPress() {
        super.cancelLongPress()
        checkLongPressHelper.cancelLongPress()
    }

    override fun getViewType(): Int {
        return DraggableView.DRAGGABLE_ICON
    }
}