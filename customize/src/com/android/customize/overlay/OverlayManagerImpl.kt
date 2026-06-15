package com.android.customize.overlay

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.android.customize.common.extension.flowWithLifecycle
import com.android.customize.overlay.di.OverlayContainer
import com.android.customize.overlay.preference.OverlayPreference
import com.android.launcher3.CustomizeLauncher
import com.android.systemui.plugins.shared.LauncherOverlayManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

class OverlayManagerImpl(val launcher: CustomizeLauncher) : OverlayManagerLifecycle() {

    var overlay: OverlayBase? = null
    val overlayContainer = OverlayContainer(launcher)

    init {
        val overlayPreference = OverlayPreference.get(launcher)
        lifecycleScope.launch {
            overlayPreference.overlayEnabledFlow
                .flowWithLifecycle(lifecycle, Lifecycle.State.CREATED)
                .collectLatest {
                    overlay?.removeView()
                    overlay = if (it) {
                        OverlayCombine()
                    } else {
                        OverlayFallback()
                    }
                    launcher.setLauncherOverlay(overlay)
                    overlay?.addView(launcher)
                }
        }

        val processContainer = overlayContainer.processContainer
        lifecycleScope.launch {
            processContainer.wallpaperController.backgroundFlow
                .flowWithLifecycle(lifecycle, Lifecycle.State.CREATED)
                .collectLatest {
                    launcher.rootView.background = it
                }
        }

        val overlayCallback = LauncherOverlayManager.LauncherOverlayCallbacks {
            processContainer.wallpaperController.updateAlpha(abs(it))
        }
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                launcher.workspace.addOverlayCallback(overlayCallback)
            }

            override fun onDestroy(owner: LifecycleOwner) {
                lifecycle.removeObserver(this)
                launcher.workspace.removeOverlayCallback(overlayCallback)
            }
        })
    }
}