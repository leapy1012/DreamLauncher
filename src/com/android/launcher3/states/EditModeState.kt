/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */
package com.android.launcher3.states

import android.content.Context
import com.android.launcher3.Launcher
import com.android.launcher3.LauncherState
import com.android.launcher3.anim.Interpolators.ACCEL_2
import com.android.launcher3.logging.StatsLogManager
import com.android.launcher3.views.ActivityContext

/** Definition for Edit Mode state used for home gardening multi-select */
class EditModeState(id: Int) : LauncherState(id, StatsLogManager.LAUNCHER_STATE_HOME, STATE_FLAGS) {

    companion object {
        private val STATE_FLAGS =
            (FLAG_MULTI_PAGE or
                FLAG_WORKSPACE_INACCESSIBLE or
                FLAG_DISABLE_RESTORE or
                FLAG_WORKSPACE_ICONS_CAN_BE_DRAGGED)
    }

    override fun <T> getTransitionDuration(context: T, isToState: Boolean): Int where
    T : Context?,
    T : ActivityContext? {
        return 320
    }

    override fun <T> getDepthUnchecked(context: T): Float where T : Context?, T : ActivityContext? {
        // OPPO exposes blur=1 and depth=0 as separate state channels. AOSP Launcher has one
        // depth channel, so depth=1 drives the same surface blur while BaseDepthController keeps
        // the ColorOS wallpaper zoom contribution at zero.
        return 1f
    }

    override fun getWorkspaceScaleAndTranslation(launcher: Launcher): ScaleAndTranslation {
        // ToggleBarState changes the workspace pivot and keeps both translations at zero.
        return ScaleAndTranslation(
            ColorOsWorkspaceEditTransition.getWorkspaceScale(launcher), 0f, 0f)
    }

    override fun getHotseatScaleAndTranslation(launcher: Launcher): ScaleAndTranslation {
        return getWorkspaceScaleAndTranslation(launcher)
    }

    override fun getWorkspaceBackgroundAlpha(launcher: Launcher): Float {
        return 0f
    }

    override fun getWorkspacePageAlphaProvider(launcher: Launcher): PageAlphaProvider {
        val visiblePages = launcher.workspace.visiblePageIndices
        return object : PageAlphaProvider(ACCEL_2) {
            override fun getPageAlpha(pageIndex: Int): Float =
                if (visiblePages.contains(pageIndex)) 1f else 0f
        }
    }

    override fun onLeavingState(launcher: Launcher?, toState: LauncherState?) {
        // cleanup any changes to workspace
    }
}
