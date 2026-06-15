package com.android.launcher3.touch

import android.view.View
import com.android.customize.overlay.ui.plus.PluscreenView
import com.android.customize.overlay.ui.plus.view.CategoryGroupView
import com.android.launcher3.BubbleTextView
import com.android.launcher3.model.data.FolderInfo
import com.android.launcher3.model.data.WorkspaceItemInfo

object CustomizeItemClickHandler {
    @JvmField
    val INSTANCE = View.OnClickListener { v ->
        if (v is BubbleTextView && v.tag is WorkspaceItemInfo) {
            val folderIcon = v.parent as? CategoryGroupView
            if (folderIcon != null && v == folderIcon.icons.last()) {
                val folderInfo = folderIcon.tag as? FolderInfo ?: return@OnClickListener
                val view = PluscreenView.get(folderIcon)
                view.categoryMorePage.bind(folderInfo) {
                    view.categoryMoreAnimator.open(true, folderIcon)
                }
                return@OnClickListener
            }
        } else if (v is CategoryGroupView) {
            val folderInfo = v.tag as? FolderInfo ?: return@OnClickListener
            val view = PluscreenView.get(v)
            view.categoryMorePage.bind(folderInfo) {
                view.categoryMoreAnimator.open(true, v)
            }
        }
        ItemClickHandler.INSTANCE.onClick(v)
    }
}