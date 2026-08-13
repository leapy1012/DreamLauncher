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
package com.android.launcher3.states;

import static com.android.launcher3.logging.StatsLogManager.LAUNCHER_STATE_HOME;

import android.content.Context;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.Workspace;

/**
 * Definition for spring loaded state used during drag and drop.
 */
public class SpringLoadedState extends LauncherState {

    private static final int STATE_FLAGS = FLAG_MULTI_PAGE
            | FLAG_WORKSPACE_INACCESSIBLE | FLAG_DISABLE_RESTORE
            | FLAG_WORKSPACE_ICONS_CAN_BE_DRAGGED | FLAG_WORKSPACE_HAS_BACKGROUNDS;

    public SpringLoadedState(int id) {
        super(id, LAUNCHER_STATE_HOME, STATE_FLAGS);
    }

    @Override
    public int getTransitionDuration(Context context, boolean isToState) {
        if (context.getResources().getBoolean(com.android.launcher3.R.bool.config_hxy_grid)) {
            return 320;
        }
        return 150;
    }

    @Override
    public ScaleAndTranslation getWorkspaceScaleAndTranslation(Launcher launcher) {
        if (launcher.getResources().getBoolean(com.android.launcher3.R.bool.config_hxy_grid)) {
            return new ScaleAndTranslation(0.85f, 0f, 0f);
        }
        DeviceProfile grid = launcher.getDeviceProfile();
        Workspace<?> ws = launcher.getWorkspace();
        if (ws.getChildCount() == 0) {
            return super.getWorkspaceScaleAndTranslation(launcher);
        }

        float shrunkTop = grid.getCellLayoutSpringLoadShrunkTop();
        float scale = grid.getWorkspaceSpringLoadScale(launcher);

        float halfHeight = ws.getHeight() *  0.65f;// 长按桌面时，缩放因子调整
        float myCenter = ws.getTop() + halfHeight;
        float cellTopFromCenter = halfHeight - ws.getChildAt(0).getTop();
        float actualCellTop = myCenter - cellTopFromCenter * scale;
        return new ScaleAndTranslation(scale, 0, shrunkTop - actualCellTop);
    }

    @Override
    protected float getDepthUnchecked(Context context) {
        if (context.getResources().getBoolean(com.android.launcher3.R.bool.config_hxy_grid)) {
            // OPPO's ToggleBar state requests full wallpaper blur while keeping wallpaper zoom
            // disabled. BaseDepthController already keeps HXY wallpaper zoom at zero, so use the
            // full depth value here only to drive the surface blur pipeline.
            return 1f;
        }
        return 0.5f;
    }

    @Override
    public ScaleAndTranslation getHotseatScaleAndTranslation(Launcher launcher) {
        return new ScaleAndTranslation(1, 0, 0);
    }

    @Override
    public float getWorkspaceBackgroundAlpha(Launcher launcher) {
        if (launcher.getResources().getBoolean(com.android.launcher3.R.bool.config_hxy_grid)) {
            return 0f;
        }
        return 0.2f;
    }
}
