/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.uioverrides.states;

import static com.android.launcher3.anim.Interpolators.DEACCEL_2;
import static com.android.launcher3.logging.StatsLogManager.LAUNCHER_STATE_ALLAPPS;

import android.content.Context;

import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.R;
import com.android.launcher3.util.Themes;
import com.android.launcher3.views.ActivityContext;

/**
 * Definition for AllApps state
 */
public class AllAppsState extends LauncherState {

    private static final int COLOROS_OPEN_DURATION_MS = 380;
    private static final int COLOROS_CLOSE_DURATION_MS = 517;
    private static final float COLOROS_WORKSPACE_SCALE = 0.92f;

    private static final int STATE_FLAGS =
            FLAG_WORKSPACE_INACCESSIBLE | FLAG_CLOSE_POPUPS | FLAG_HOTSEAT_INACCESSIBLE;

    public AllAppsState(int id) {
        super(id, LAUNCHER_STATE_ALLAPPS, STATE_FLAGS);
    }

    @Override
    public <DEVICE_PROFILE_CONTEXT extends Context & ActivityContext>
    int getTransitionDuration(DEVICE_PROFILE_CONTEXT context, boolean isToState) {
        if (context.getResources().getBoolean(R.bool.config_hxy_grid)) {
            return isToState ? COLOROS_OPEN_DURATION_MS : COLOROS_CLOSE_DURATION_MS;
        }
        return isToState
                ? context.getDeviceProfile().allAppsOpenDuration
                : context.getDeviceProfile().allAppsCloseDuration;
    }

    @Override
    public String getDescription(Launcher launcher) {
        return launcher.getAppsView().getDescription();
    }

    @Override
    public float getVerticalProgress(Launcher launcher) {
        return 0f;
    }

    @Override
    public ScaleAndTranslation getWorkspaceScaleAndTranslation(Launcher launcher) {
        if (launcher.getResources().getBoolean(R.bool.config_hxy_grid)) {
            return new ScaleAndTranslation(COLOROS_WORKSPACE_SCALE, NO_OFFSET, NO_OFFSET);
        }
        return new ScaleAndTranslation(launcher.getDeviceProfile().workspaceContentScale, NO_OFFSET,
                NO_OFFSET);
    }

    @Override
    public ScaleAndTranslation getHotseatScaleAndTranslation(Launcher launcher) {
        if (launcher.getResources().getBoolean(R.bool.config_hxy_grid)) {
            return getWorkspaceScaleAndTranslation(launcher);
        }
        if (launcher.getDeviceProfile().isTablet) {
            return getWorkspaceScaleAndTranslation(launcher);
        } else {
            ScaleAndTranslation overviewScaleAndTranslation = LauncherState.OVERVIEW
                    .getWorkspaceScaleAndTranslation(launcher);
            return new ScaleAndTranslation(
                    launcher.getDeviceProfile().workspaceContentScale,
                    overviewScaleAndTranslation.translationX,
                    overviewScaleAndTranslation.translationY);
        }
    }

    @Override
    protected <DEVICE_PROFILE_CONTEXT extends Context & ActivityContext>
            float getDepthUnchecked(DEVICE_PROFILE_CONTEXT context) {
        if (context.getResources().getBoolean(R.bool.config_hxy_grid)) {
            // ColorOS uses a single-depth drawer state. Wallpaper scaling remains disabled by
            // BaseDepthController on MTK, while this value keeps the scrim/blur timeline aligned.
            return 1f;
        }
        if (context.getDeviceProfile().isTablet) {
            return context.getDeviceProfile().bottomSheetDepth;
        } else {
            // The scrim fades in at approximately 50% of the swipe gesture.
            // This means that the depth should be greater than 1, in order to fully zoom out.
            return 2f;
        }
    }

    @Override
    public PageAlphaProvider getWorkspacePageAlphaProvider(Launcher launcher) {
        PageAlphaProvider superPageAlphaProvider = super.getWorkspacePageAlphaProvider(launcher);
        return new PageAlphaProvider(DEACCEL_2) {
            @Override
            public float getPageAlpha(int pageIndex) {
                return launcher.getDeviceProfile().isTablet
                        ? superPageAlphaProvider.getPageAlpha(pageIndex)
                        : 0;
            }
        };
    }

    @Override
    public int getVisibleElements(Launcher launcher) {
        // Don't add HOTSEAT_ICONS for non-tablets in ALL_APPS state.
        return launcher.getDeviceProfile().isTablet ? ALL_APPS_CONTENT | HOTSEAT_ICONS
                : ALL_APPS_CONTENT;
    }

    @Override
    public LauncherState getHistoryForState(LauncherState previousState) {
        return previousState == OVERVIEW ? OVERVIEW : NORMAL;
    }

    @Override
    public int getWorkspaceScrimColor(Launcher launcher) {
        return launcher.getDeviceProfile().isTablet
                ? launcher.getResources().getColor(R.color.widgets_picker_scrim)
                : Themes.getAttrColor(launcher, R.attr.allAppsScrimColor);
    }
}
