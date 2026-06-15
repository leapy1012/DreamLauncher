package com.android.customize.overlay.ui.plus.page

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.customize.overlay.ui.plus.view.CategoryGroupView
import com.android.launcher3.model.data.FolderInfo
import com.android.launcher3.model.data.areContentSame
import com.android.launcher3.touch.CustomizeItemClickHandler
import com.android.launcher3.touch.CustomizeItemLongClickListener

class CategoryListAdapter : ListAdapter<FolderInfo, RecyclerView.ViewHolder>(
    object : DiffUtil.ItemCallback<FolderInfo>() {
        override fun areItemsTheSame(
            oldItem: FolderInfo,
            newItem: FolderInfo
        ): Boolean {
            return oldItem.title == newItem.title
        }

        override fun areContentsTheSame(
            oldItem: FolderInfo,
            newItem: FolderInfo
        ): Boolean {
            return oldItem.areContentSame(newItem)
        }

        override fun getChangePayload(oldItem: FolderInfo, newItem: FolderInfo): Any? {
            return true
        }
    }
) {

    var onIconLongClickListener = CustomizeItemLongClickListener.INSTANCE

    private val onIconClickListener = CustomizeItemClickHandler.INSTANCE

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        val view = CategoryGroupView(parent.context)
        view.icons.forEach { icon ->
            icon.setOnClickListener(onIconClickListener)
            icon.setOnLongClickListener {
                if (icon != view.icons.last()) {
                    onIconLongClickListener.onLongClick(icon)
                    true
                } else {
                    true
                }
            }
        }
        view.setOnClickListener(onIconClickListener)
        return object : RecyclerView.ViewHolder(view) {}
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val view = holder.itemView as CategoryGroupView
        view.bind(getItem(position))
    }
}