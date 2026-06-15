package com.android.customize.overlay.extension

import android.os.UserHandle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.size
import com.android.customize.overlay.ui.minus.MinuscreenView
import com.android.customize.overlay.ui.minus.view.UsageStatsWidgetView
import com.android.customize.overlay.ui.plus.PluscreenView
import com.android.launcher3.CustomizeLauncher
import com.android.launcher3.LauncherSettings
import com.android.launcher3.model.data.ItemInfo
import java.util.function.Predicate

fun CustomizeLauncher.getFirstMatchForAppClose(
    preferredItemId: Int,
    packageName: String?,
    user: UserHandle?
): Pair<View?, Boolean> {
    val preferredItem = Predicate { info: ItemInfo? ->
        info != null && info.id == preferredItemId
    }
    val packageAndUserAndApp = Predicate { info: ItemInfo? ->
        info != null && info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION
                && info.user == user && info.targetComponent?.packageName == packageName
    }

    val view = getOpenScreenView()

    return when (view) {
        is MinuscreenView -> {
            val containers = mutableListOf<ViewGroup>().apply {
                view.widgetListPage.rvWidgets.children
                    .firstOrNull { it is UsageStatsWidgetView }?.also {
                        val v = it as UsageStatsWidgetView
                        add(v.rvApps)
                    }
                add(view.widgetListPage.rvWidgets)
            }
            val matchedView = getFirstMatch(containers,
                preferredItem, packageAndUserAndApp)
            Pair(matchedView, true)
        }

        is PluscreenView -> {
            val containers = mutableListOf<ViewGroup>().apply {
                if (view.searchAppPage.isVisible) {
                    add(view.searchAppPage.rvApps)
                } else if (view.categoryMorePage.isVisible) {
                    view.categoryMorePage.folderPagedView.getCurrentCellLayout()?.also {
                        add(it.shortcutsAndWidgets)
                    }
                } else {
                    addAll(view.categoryListPage
                        .rvCategory.children.map { it as ViewGroup })
                }
            }
            val matchedView = getFirstMatch(containers,
                preferredItem, packageAndUserAndApp)
            Pair(matchedView, true)
        }

        else -> {
            Pair(null, false)
        }
    }
}

private fun getFirstMatch(
    containers: List<ViewGroup>,
    vararg operators: Predicate<ItemInfo?>
): View? {
    for (operator in operators) {
        for (container in containers) {
            val match = mapOverViewGroup(container, operator)
            if (match != null) {
                return match
            }
        }
    }
    return null
}

private fun mapOverViewGroup(container: ViewGroup, op: Predicate<ItemInfo?>): View? {
    val itemCount = container.size
    for (itemIdx in 0 until itemCount) {
        val item = container.getChildAt(itemIdx)
        if (op.test(item.tag as ItemInfo?)) {
            return item
        }
    }
    return null
}