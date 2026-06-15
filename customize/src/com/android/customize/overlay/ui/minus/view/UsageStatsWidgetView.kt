package com.android.customize.overlay.ui.minus.view

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setMargins
import androidx.core.view.setPadding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.customize.common.extension.color
import com.android.customize.common.extension.px
import com.android.customize.common.extension.pxf
import com.android.launcher3.BubbleTextView
import com.android.launcher3.Launcher
import com.android.launcher3.R
import com.android.launcher3.model.data.FolderInfo
import com.android.launcher3.model.data.ItemInfoWithIcon
import com.android.launcher3.model.data.areContentSame
import com.android.launcher3.touch.CustomizeItemClickHandler

class UsageStatsWidgetView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val launcher by lazy {
        Launcher.getLauncher(context)
    }
    private val usageStatsAdapter = UsageStatsAdapter()

    val sectionName by lazy {
        TextView(context).apply {
            setText(R.string.usagestats_section_name)
            setTextColor(color(R.color.widget_section_name))
            setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                pxf(R.dimen.widget_section_name)
            )
        }
    }

    val rvApps by lazy {
        RecyclerView(context).apply {
            layoutManager = GridLayoutManager(context, 10).apply {
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return if (launcher.deviceProfile.isTablet) 1 else 2
                    }
                }
            }
            adapter = usageStatsAdapter
        }
    }

    init {
        orientation = VERTICAL
        setPadding(px(R.dimen.usagestats_padding))

        addView(
            sectionName, LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )
        )

        rvApps.applyWidgetStyle()
        rvApps.setPadding(px(R.dimen.usagestats_rv_padding))
        addView(
            rvApps, LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = px(R.dimen.widget_section_name_margin)
            })
    }

    fun bind(folderInfo: FolderInfo) {
        val itemInfos = folderInfo.contents
            .map { it as ItemInfoWithIcon }
        usageStatsAdapter.submitList(itemInfos)
    }

    class UsageStatsAdapter : ListAdapter<ItemInfoWithIcon, RecyclerView.ViewHolder>(
        object : DiffUtil.ItemCallback<ItemInfoWithIcon>() {
            override fun areItemsTheSame(
                oldItem: ItemInfoWithIcon,
                newItem: ItemInfoWithIcon
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: ItemInfoWithIcon,
                newItem: ItemInfoWithIcon
            ): Boolean {
                return oldItem.areContentSame(newItem)
            }

            override fun getChangePayload(
                oldItem: ItemInfoWithIcon,
                newItem: ItemInfoWithIcon
            ): Any? {
                return true
            }
        }
    ) {
        private val onIconClickListener = CustomizeItemClickHandler.INSTANCE

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): RecyclerView.ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(
                R.layout.usagestats_app_icon, parent, false
            ) as BubbleTextView
            val lp = view.layoutParams as MarginLayoutParams
            lp.setMargins(view.px(R.dimen.usagestats_app_margin) / 2)
            view.apply {
                maxLines = 1
                gravity = Gravity.CENTER
                ellipsize = TextUtils.TruncateAt.END
                setTextColor(color(R.color.usagestats_app_name))
                setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    pxf(R.dimen.usagestats_app_name)
                )
                compoundDrawablePadding = 0
            }
            view.setOnClickListener(onIconClickListener)
            return object : RecyclerView.ViewHolder(view) {}
        }

        override fun onBindViewHolder(
            holder: RecyclerView.ViewHolder,
            position: Int
        ) {
            val view = holder.itemView as BubbleTextView
            view.applyFromItemInfoWithIcon(getItem(position))
        }
    }
}