package com.android.customize.overlay.ui.plus.page

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.customize.overlay.di.OverlayContainer
import com.android.launcher3.model.data.FolderInfo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.android.customize.common.logger.MyLogger

class CategoryListStore(overlayContainer: OverlayContainer) : ViewModel() {
    val launcherContainer = overlayContainer.launcherContainer
    val folderInfosFlow = launcherContainer.appController
        .appInfosFlow.map { appInfos ->
            val folderInfos = mutableListOf<FolderInfo>()
            overlayContainer.categoryController.getCategories(
                overlayContainer.launcher, appInfos
            ).filter { category ->
                category.componentNames.any {
                    overlayContainer.categoryController
                        .getAppInfo(it) != null
                }
            }.forEach { category ->
                val folderInfo = FolderInfo()
                folderInfo.title = category.folderName
                category.componentNames.forEach {
                    val appInfo = overlayContainer.categoryController
                        .getAppInfo(it)
                    if (appInfo != null) {
                        folderInfo.add(
                            appInfo.makeWorkspaceItem(
                                overlayContainer.launcher
                            ), false
                        )
                    }
                }
                folderInfos.add(folderInfo)
            }
            android.util.Log.d("OK.DEBUG:", "folderInfos: $folderInfos")
            folderInfos.toList()
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            emptyList()
        )
}