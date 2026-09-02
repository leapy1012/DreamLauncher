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
import com.android.launcher3.R;
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
        return 150;
    }

    @Override
    public ScaleAndTranslation getWorkspaceScaleAndTranslation(Launcher launcher) {
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

    /**
     * Oppo ToggleBar uses depth=0 + blur=1. Without Oplus wallpaper blur on MTK,
     * dimming is handled by {@link #getWorkspaceBackgroundAlpha} instead.
     */
    @Override
    protected float getDepthUnchecked(Context context) {
        return 0f;
    }

    @Override
    public ScaleAndTranslation getHotseatScaleAndTranslation(Launcher launcher) {
        return new ScaleAndTranslation(1, 0, 0);
    }

    /**
     * Oppo ToggleBar darkening (decoded):
     * <ul>
     *   <li>Blur available → wallpaper blur progress 1.0 with blend alpha 0.3</li>
     *   <li>Blur unavailable → {@code OplusStaticBlurView} + {@code popup_no_blur_background}
     *       ({@code #73000000}) at alpha 1.0</li>
     * </ul>
     * DragLayer draws {@link com.android.launcher3.graphics.Scrim} under children, so
     * icons stay bright while the wallpaper dims — matching Oppo z-order.
     */
    @Override
    public float getWorkspaceBackgroundAlpha(Launcher launcher) {
        return launcher.getResources().getFloat(R.dimen.oplus_home_edit_wallpaper_scrim_alpha);
    }
}
