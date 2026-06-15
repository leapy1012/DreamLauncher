package com.android.customize.common.controller

import android.content.Context
import android.os.LocaleList
import com.android.launcher3.Launcher
import com.android.launcher3.allapps.AllAppsStore
import com.android.launcher3.allapps.AppInfoComparator
import com.android.launcher3.compat.AlphabeticIndexCompat
import com.android.launcher3.model.data.AppInfo
import com.android.launcher3.util.LabelComparator
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale

class AppController(val launcher: Launcher) {

    private val appStore by lazy {
        launcher.appsView.appsStore
    }

    private val alphabeticIndex by lazy {
        AlphabeticIndexCompat(LocaleList.forLanguageTags("zh"))
    }

    val appInfosFlow: Flow<List<AppInfo>> = callbackFlow {
        val onUpdateListener = AllAppsStore.OnUpdateListener {
            trySend(getFilteredApps())
        }
        appStore.addUpdateListener(onUpdateListener)
        trySend(getFilteredApps())
        awaitClose {
            appStore.removeUpdateListener(onUpdateListener)
        }
    }

    private val appNameComparator = AppInfoComparator(launcher)
    private fun getFilteredApps(): List<AppInfo> {
        val result = if (launcher.isSectionSortingRequired) {
            appStore.apps
                .onEach { it.sectionName = alphabeticIndex.computeSectionName(it.title ?: "") }
                .sortedWith(appNameComparator)
                .groupBy { it.sectionName }
                .toSortedMap(LabelComparator())
                .values
                .flatten()
        } else {
            appStore.apps.sortedWith(appNameComparator)
        }
        return result
    }
}

private val Context.isSectionSortingRequired: Boolean
    get() {
        val sortingLocales = arrayOf(Locale.CHINESE, Locale.ENGLISH)
        val currentLocale = resources.configuration.locales.get(0)
        return sortingLocales.any { it.language == currentLocale.language }
    }
