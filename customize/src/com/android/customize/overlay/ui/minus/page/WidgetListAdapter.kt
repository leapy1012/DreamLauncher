package com.android.customize.overlay.ui.minus.page

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.customize.overlay.model.BatteryInfo
import com.android.customize.overlay.model.CalendarInfo
import com.android.customize.overlay.model.PhotoInfo
import com.android.customize.overlay.model.RamInfo
import com.android.customize.overlay.ui.minus.view.BatteryWidgetView
import com.android.customize.overlay.ui.minus.view.CalendarWidgetView
import com.android.customize.overlay.ui.minus.view.PhotoWidgetView
import com.android.customize.overlay.ui.minus.view.RamWidgetView
import com.android.customize.overlay.ui.minus.view.UsageStatsWidgetView
import com.android.launcher3.model.data.areContentSame
import com.android.launcher3.model.data.FolderInfo

class WidgetListAdapter : ListAdapter<Any, RecyclerView.ViewHolder>(
    object : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(
            oldItem: Any, newItem: Any
        ): Boolean {
            return oldItem.javaClass == newItem.javaClass
        }

        override fun areContentsTheSame(
            oldItem: Any, newItem: Any
        ): Boolean {
            if (oldItem is BatteryInfo && newItem is BatteryInfo) {
                return oldItem == newItem
            } else if (oldItem is CalendarInfo && newItem is CalendarInfo) {
                return oldItem == newItem
            } else if (oldItem is PhotoInfo && newItem is PhotoInfo) {
                return oldItem == newItem
            } else if (oldItem is RamInfo && newItem is RamInfo) {
                return oldItem == newItem
            } else if (oldItem is FolderInfo && newItem is FolderInfo) {
                return oldItem.areContentSame(newItem)
            }
            return false
        }

        override fun getChangePayload(oldItem: Any, newItem: Any): Any? {
            return true
        }
    }
) {

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        val view = when (viewType) {
            VT_USAGESTATS -> UsageStatsWidgetView(parent.context)
            VT_BATTERY -> BatteryWidgetView(parent.context)
            VT_CALENDAR -> CalendarWidgetView(parent.context)
            VT_RAM -> RamWidgetView(parent.context)
            VT_PHOTO -> PhotoWidgetView(parent.context)
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
        return object : RecyclerView.ViewHolder(view) {}
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val view = holder.itemView
        val item = getItem(position)
        if (item is BatteryInfo && view is BatteryWidgetView) {
            view.bind(item)
        } else if (item is FolderInfo && view is UsageStatsWidgetView) {
            view.bind(item)
        } else if (item is CalendarInfo && view is CalendarWidgetView) {
            view.bind(item)
        } else if (item is RamInfo && view is RamWidgetView) {
            view.bind(item)
        } else if (item is PhotoInfo && view is PhotoWidgetView) {
            view.bind(item)
        }
    }

    companion object {
        const val VT_USAGESTATS = 0
        const val VT_BATTERY = 1
        const val VT_CALENDAR = 2
        const val VT_RAM = 3
        const val VT_PHOTO = 4
    }
}