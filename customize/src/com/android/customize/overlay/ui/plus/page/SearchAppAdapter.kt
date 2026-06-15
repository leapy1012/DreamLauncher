package com.android.customize.overlay.ui.plus.page

import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.setMargins
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.customize.common.extension.color
import com.android.customize.common.extension.px
import com.android.customize.overlay.ui.plus.view.CategoryBubbleTextView
import com.android.launcher3.BubbleTextView
import com.android.launcher3.Launcher
import com.android.launcher3.R
import com.android.launcher3.model.data.ItemInfoWithIcon
import com.android.launcher3.model.data.areContentSame
import com.android.launcher3.touch.CustomizeItemClickHandler
import com.android.launcher3.touch.CustomizeItemLongClickListener

class SearchAppAdapter : ListAdapter<Any, RecyclerView.ViewHolder>(
    object : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            if (oldItem is ItemInfoWithIcon && newItem is ItemInfoWithIcon) {
                return oldItem.id == newItem.id
            } else if (oldItem is String && newItem is String) {
                return oldItem == newItem
            }
            return false
        }

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            if (oldItem is ItemInfoWithIcon && newItem is ItemInfoWithIcon) {
                return oldItem.areContentSame(newItem)
            }
            return false
        }

        override fun getChangePayload(oldItem: Any, newItem: Any): Any? {
            return true
        }
    }
) {

    var onIconLongClickListener = CustomizeItemLongClickListener.INSTANCE

    private val onIconClickListener = CustomizeItemClickHandler.INSTANCE

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        if (item is ItemInfoWithIcon) {
            return VT_APP
        } else if (item is String) {
            return VT_HEADER
        }
        return super.getItemViewType(position)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        val view = when (viewType) {
            VT_HEADER -> {
                TextView(parent.context).apply {
                    layoutParams = ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(px(R.dimen.search_section_header_margin))
                    }
                    setTextColor(color(R.color.search_section_header))
                }
            }

            VT_APP -> {
                CategoryBubbleTextView(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        iconSize, ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    setTextColor(color(R.color.search_app_name))
                    ellipsize = TextUtils.TruncateAt.END
                    gravity = Gravity.CENTER
                    maxLines = 1

                    setOnClickListener(onIconClickListener)
                    setOnLongClickListener(onIconLongClickListener)
                }
            }

            else -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
        return object : RecyclerView.ViewHolder(view) {}
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        when (val item = getItem(position)) {
            is String -> {
                val view = holder.itemView as TextView
                view.text = item
            }

            is ItemInfoWithIcon -> {
                val view = holder.itemView as BubbleTextView
                view.applyFromItemInfoWithIcon(item)
            }
        }
    }

    fun createSpanSizeLookup(launcher: Launcher): GridLayoutManager.SpanSizeLookup {
        return object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (val viewType = getItemViewType(position)) {
                    VT_HEADER -> 8
                    VT_APP -> if (launcher.deviceProfile.isTablet) 1 else 2
                    else -> throw IllegalArgumentException("Invalid view type: $viewType")
                }
            }
        }
    }

    companion object {
        private const val VT_HEADER = 1
        private const val VT_APP = 2
    }
}