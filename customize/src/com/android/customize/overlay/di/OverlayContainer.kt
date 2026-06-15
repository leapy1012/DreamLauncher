package com.android.customize.overlay.di

import com.android.customize.overlay.controller.BatteryController
import com.android.customize.overlay.controller.CalendarController
import com.android.customize.overlay.controller.CategoryController
import com.android.customize.overlay.controller.PhotoController
import com.android.customize.overlay.controller.RamController
import com.android.customize.overlay.controller.UsageStatsController
import com.android.launcher3.CustomizeLauncher

class OverlayContainer(val launcher: CustomizeLauncher) {
    val launcherContainer get() = launcher.container
    val processContainer get() = launcherContainer.processContainer

    val categoryController by lazy {
        CategoryController()
    }

    val batteryController by lazy {
        BatteryController(launcher)
    }

    val usageStatsController by lazy {
        UsageStatsController(
            launcher,
            launcherContainer.appController
        )
    }

    val calendarController by lazy {
        CalendarController(launcher)
    }

    val ramController by lazy {
        RamController(launcher)
    }

    val photoController by lazy {
        PhotoController(launcher)
    }
}