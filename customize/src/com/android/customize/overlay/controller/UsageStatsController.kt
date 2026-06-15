package com.android.customize.overlay.controller

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
import com.android.customize.common.controller.AppController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

class UsageStatsController(
    context: Context,
    appController: AppController
) {
    private val usm = context.getSystemService(UsageStatsManager::class.java)

    private val _packageNamesFlow = MutableStateFlow<List<UsageStats>>(emptyList())
    val appInfosFlow = combine(
        _packageNamesFlow,
        appController.appInfosFlow
    ) { stats, apps ->
        val prioritizedApps = stats.sortedByDescending {
            it.mLastTimeUsed
        }.mapNotNull { stat ->
            val intent = context.packageManager.getLaunchIntentForPackage(stat.packageName)
            if (intent != null) {
                val info = context.packageManager.resolveActivity(intent, MATCH_DEFAULT_ONLY)
                apps.firstOrNull { info.componentInfo.componentName.flattenToString() == it.componentName.flattenToString() }
            } else {
                apps.firstOrNull { stat.packageName == it.componentName.packageName }
            }
        }.toSet()

        val existingPackageNames = prioritizedApps.map { it.componentName.packageName }
        val unprioritizedApps =
            apps.filterNot { it.componentName.packageName in existingPackageNames }

        return@combine (prioritizedApps + unprioritizedApps).take(TARGET_STATS_APP_SIZE)
    }

    fun refresh() {
        _packageNamesFlow.value = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            0, System.currentTimeMillis()
        )
    }

    companion object {
        private const val TARGET_STATS_APP_SIZE = 10
    }
}