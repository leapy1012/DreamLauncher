package com.android.customize.overlay.ui.minus.page

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.customize.overlay.di.OverlayContainer
import com.android.launcher3.model.data.FolderInfo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class WidgetListStore(val overlayContainer: OverlayContainer) : ViewModel() {

    val widgetsFlow = combine(
        overlayContainer.usageStatsController
            .appInfosFlow.map { apps ->
                val folderInfo = FolderInfo()
                apps.forEach {
                    folderInfo.add(
                        it.makeWorkspaceItem(
                            overlayContainer.launcher
                        ), false
                    )
                }
                folderInfo
            },
        overlayContainer.batteryController.batteryFlow,
        overlayContainer.calendarController.calendarFlow,
        overlayContainer.ramController.ramFlow,
        overlayContainer.photoController.randomPhotoFlow
    ) { folderInfo, batteryInfo, calendarInfo, ramInfo, photoInfo ->
        listOfNotNull(
            folderInfo, batteryInfo,
            calendarInfo, ramInfo, photoInfo
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        emptyList()
    )

    fun refresh() {
        overlayContainer.usageStatsController.refresh()
        overlayContainer.ramController.refresh()
    }
}