package com.android.customize.common.controller

import com.android.launcher3.DeviceProfile
import com.android.launcher3.Launcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

class DeviceProfileController(launcher: Launcher) {
    val deviceProfileFlow = callbackFlow {
        val listener = DeviceProfile.OnDeviceProfileChangeListener {
            trySend(it)
        }
        launcher.addOnDeviceProfileChangeListener(listener)
        trySend(launcher.deviceProfile)
        awaitClose {
            launcher.removeOnDeviceProfileChangeListener(listener)
        }
    }
}