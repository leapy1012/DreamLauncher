package com.android.customize.overlay

import com.android.launcher3.CustomizeLauncher
import com.android.systemui.plugins.shared.LauncherOverlayManager.LauncherOverlay
import com.android.systemui.plugins.shared.LauncherOverlayManager.LauncherOverlayCallbacks

abstract class OverlayBase() : LauncherOverlay {
    var swipeRtl: Boolean = false
    var callbacks: LauncherOverlayCallbacks? = null

    override fun setOverlayCallbacks(callbacks: LauncherOverlayCallbacks?) {
        this.callbacks = callbacks
    }

    abstract fun addView(launcher: CustomizeLauncher)
    abstract fun removeView()
}