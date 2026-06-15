package com.android.launcher3

import android.os.Bundle
import android.os.UserHandle
import android.view.View
import android.window.OnBackAnimationCallback
import androidx.core.view.children
import androidx.core.view.isVisible
import com.android.customize.common.di.LauncherContainer
import com.android.customize.overlay.OverlayManagerImpl
import com.android.customize.overlay.extension.getFirstMatchForAppClose
import com.android.customize.overlay.preference.OverlayPreference
import com.android.launcher3.statemanager.StateManager
import com.android.launcher3.uioverrides.QuickstepLauncher
import com.android.systemui.plugins.shared.LauncherOverlayManager
import android.provider.Settings

class CustomizeLauncher : QuickstepLauncher() {
    val container = LauncherContainer(this)
    val overlayPreference by lazy {
        OverlayPreference.get(this)
    }
    val overlayManager by lazy {
        OverlayManagerImpl(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (overlayPreference.overlayEnabled) {
            deferOverlayCallbacksUntilNextResumeOrStop()
        }
        super.onCreate(savedInstanceState)

        stateManager.addStateListener(object : StateManager.StateListener<LauncherState> {
            override fun onStateTransitionStart(toState: LauncherState) {
                if (toState == LauncherState.OVERVIEW) {
                    getOpenScreenView()?.close(true)
                }
            }
        })
    }

    override fun getDefaultOverlay(): LauncherOverlayManager {
        return overlayManager
    }

    override fun getOnBackAnimationCallback(): OnBackAnimationCallback {
        val screenView = getOpenScreenView()
        if (screenView != null && screenView.canHandleBack()) {
            return screenView
        }
        return super.getOnBackAnimationCallback()
    }

    override fun getFirstMatchForAppClose(
        preferredItemId: Int,
        packageName: String?,
        user: UserHandle?,
        supportsAllAppsState: Boolean
    ): View? {
        val (view, matched) = getFirstMatchForAppClose(preferredItemId, packageName, user)
        if (matched) {
            return view
        }

        return super.getFirstMatchForAppClose(
            preferredItemId,
            packageName,
            user,
            supportsAllAppsState
        )
    }

    override fun onStateSetStart(state: LauncherState) {
        super.onStateSetStart(state)
    }

    override fun onStateSetEnd(state: LauncherState) {
        super.onStateSetEnd(state)
        var minusEnabled : Boolean = false
        var plusEnabled : Boolean = false
        if (state == LauncherState.SPRING_LOADED) {
            minusEnabled = Settings.Global.getInt(contentResolver, "persist.sys.desktop_minus", 1) == 1
            plusEnabled = Settings.Global.getInt(contentResolver, "persist.sys.desktop_plus", 1) == 1
            if (minusEnabled) {
                overlayPreference.setMinusEnabled(false)
            }
            if (plusEnabled) {
                overlayPreference.setPlusEnabled(false)
            }
        } else if (state == LauncherState.NORMAL) {
            minusEnabled = Settings.Global.getInt(contentResolver, "persist.sys.desktop_minus", 1) == 1
            plusEnabled = Settings.Global.getInt(contentResolver, "persist.sys.desktop_plus", 1) == 1
            if (minusEnabled) {
                overlayPreference.setMinusEnabled(true)
            }
            if (plusEnabled) {
                overlayPreference.setPlusEnabled(true)
            }
        }
    }

    fun getOpenScreenView(): AbstractFloatingView? {
        return rootView.children.firstOrNull {
            it is AbstractFloatingView && it.isVisible
        } as? AbstractFloatingView
    }
}