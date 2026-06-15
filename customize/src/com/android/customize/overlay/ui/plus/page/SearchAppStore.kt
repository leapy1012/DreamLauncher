package com.android.customize.overlay.ui.plus.page

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.customize.overlay.di.OverlayContainer
import com.android.launcher3.util.LabelComparator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class SearchAppStore(overlayContainer: OverlayContainer) : ViewModel() {
    private val launcherContainer = overlayContainer.launcherContainer
    private val keywordFlow = MutableStateFlow<String?>(null)
    val dataFlow = combine(
        keywordFlow,
        launcherContainer.appController.appInfosFlow
    ) { keyword, appInfos ->
        val filteredAppInfos = if (keyword.isNullOrBlank()) {
            appInfos
        } else {
            appInfos.filter { appInfo ->
                appInfo.sectionName.contains(keyword, ignoreCase = true) ||
                        (appInfo.title?.contains(keyword, ignoreCase = true) ?: false)
            }
        }
        val groupedBySection = filteredAppInfos.groupBy { it.sectionName }
        val entries = groupedBySection.entries.sortedWith { a, b ->
            comparator.compare(a.key, b.key)
        }
        entries.flatMap { (sectionName, apps) ->
            listOf(sectionName) + apps
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        emptyList()
    )

    fun search(keyword: String?) {
        keywordFlow.value = keyword
    }

    companion object {
        private val comparator = LabelComparator()
    }
}