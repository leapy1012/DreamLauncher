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
import com.android.launcher3.settings.HomeScreenGestures
import com.android.launcher3.statemanager.StateManager
import com.android.launcher3.uioverrides.QuickstepLauncher
import com.android.systemui.plugins.shared.LauncherOverlayManager

class CustomizeLauncher : QuickstepLauncher() {
    val container = LauncherContainer(this)
    val overlayPreference by lazy {
        OverlayPreference.get(this)
    }
    val overlayManager by lazy {
        OverlayManagerImpl(this)
    }

    /**
     * Oppo does NOT slide/fade DragLayer for Assist (setOverlayTranslation is
     * storage-only). Keep icons on-screen so [OverlayHomeEffect] frost can cover
     * them. DragLayer scale is also left at 1.0 (Oppo only scales when overlay
     * blur is available).
     */
    override fun shouldTranslateDragLayerForOverlay(): Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Align swipe-right SharedPreferences with OverlayPreference before overlay binds.
        HomeScreenGestures.reconcileSwipeRightWithOverlay(this)
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
        if (workspace.isOverlayShown) {
            return object : OnBackAnimationCallback {
                override fun onBackInvoked() {
                    overlayManager.hideOverlay(true)
                }
            }
        }
        return super.getOnBackAnimationCallback()
    }

    override fun onBackPressed() {
        if (workspace.isOverlayShown) {
            overlayManager.hideOverlay(true)
            return
        }
        super.onBackPressed()
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
        // Left/right side screens removed — do not restore overlays from Settings.Global.
    }

    fun getOpenScreenView(): AbstractFloatingView? {
        return rootView.children.firstOrNull {
            it is AbstractFloatingView && it.isVisible
        } as? AbstractFloatingView
    }
}