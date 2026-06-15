package com.android.customize.common.di

import com.android.customize.common.controller.AppController
import com.android.customize.common.controller.DeviceProfileController
import com.android.launcher3.Launcher

class LauncherContainer(val launcher: Launcher) {
    val processContainer get() = ProcessContainer.get(launcher)

    val deviceProfileController by lazy {
        DeviceProfileController(launcher)
    }

    val appController by lazy {
        AppController(launcher)
    }
}