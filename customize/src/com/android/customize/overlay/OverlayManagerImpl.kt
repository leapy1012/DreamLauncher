package com.android.customize.overlay

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.android.customize.common.extension.flowWithLifecycle
import com.android.customize.overlay.di.OverlayContainer
import com.android.customize.overlay.preference.OverlayPreference
import com.android.customize.overlay.quickglance.QuickGlanceRemoteOverlay
import com.android.launcher3.CustomizeLauncher
import com.android.systemui.plugins.shared.LauncherOverlayManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

class OverlayManagerImpl(val launcher: CustomizeLauncher) : OverlayManagerLifecycle() {

    var overlay: OverlayBase? = null
    val overlayContainer = OverlayContainer(launcher)
    private val homeEffect = OverlayHomeEffect(launcher)

    init {
        val overlayPreference = OverlayPreference.get(launcher)
        lifecycleScope.launch {
            overlayPreference.overlayEnabledFlow
                .flowWithLifecycle(lifecycle, Lifecycle.State.CREATED)
                .collectLatest {
                    overlay?.removeView()
                    if (it) {
                        overlay = OverlayCombine()
                        launcher.setLauncherOverlay(overlay)
                        overlay?.addView(launcher)
                    } else {
                        // Do not use OverlayFallback — that still installs empty overscroll edges.
                        overlay = null
                        launcher.setLauncherOverlay(null)
                        homeEffect.reset()
                    }
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

        val overlayCallback = LauncherOverlayManager.LauncherOverlayCallbacks { progress ->
            val p = abs(progress).coerceIn(0f, 1f)
            // Home frost only — do not mutate wallpaper background here (reassigning
            // rootView.background every frame shakes the whole launcher on exit).
            homeEffect.onOverlayScrollChanged(p)
        }
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                launcher.workspace.addOverlayCallback(overlayCallback)
            }

            override fun onDestroy(owner: LifecycleOwner) {
                lifecycle.removeObserver(this)
                launcher.workspace.removeOverlayCallback(overlayCallback)
                homeEffect.reset()
            }
        })
    }

    private fun quickGlance(): QuickGlanceRemoteOverlay? =
        (overlay as? OverlayCombine)?.quickGlanceOverlay

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        quickGlance()?.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        quickGlance()?.onDetachedFromWindow()
        super.onDetachedFromWindow()
    }

    override fun onActivityStarted(activity: android.app.Activity) {
        super.onActivityStarted(activity)
        quickGlance()?.onStart()
    }

    override fun onActivityResumed(activity: android.app.Activity) {
        super.onActivityResumed(activity)
        quickGlance()?.onResume()
    }

    override fun onActivityPaused(activity: android.app.Activity) {
        quickGlance()?.onPause()
        super.onActivityPaused(activity)
    }

    override fun onActivityStopped(activity: android.app.Activity) {
        quickGlance()?.onStop()
        super.onActivityStopped(activity)
    }

    override fun openOverlay() {
        quickGlance()?.openOverlay()
    }

    override fun hideOverlay(duration: Int) {
        quickGlance()?.closeOverlay()
    }
}
