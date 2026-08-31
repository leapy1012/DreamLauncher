/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.quickstep.util;

import android.graphics.Rect;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.R;
import com.android.quickstep.views.HxyClearAllPanelView;
import com.android.quickstep.views.OverviewDockView;
import com.android.quickstep.views.RecentsView;

/**
 * Positions Oppo-style Clear All pill and dock under the task carousel.
 */
public final class HxyRecentsLayoutManager {

    private HxyRecentsLayoutManager() { }

    public static void layoutBottomUi(RecentsView<?, ?> rv, HxyClearAllPanelView clearPanel,
            OverviewDockView dockView) {
        if (rv == null || clearPanel == null) {
            return;
        }
        if (!rv.getResources().getBoolean(R.bool.config_clearall_center)) {
            return;
        }
        // Dock + Clear All are siblings of RecentsView, so gate on overview visibility —
        // task count alone would leak them onto the home screen.
        boolean showClear = rv.shouldShowBottomUi();
        clearPanel.setVisibility(showClear ? View.VISIBLE : View.GONE);
        if (dockView != null) {
            boolean showDock = showClear && isDockSupported(rv);
            dockView.setVisibility(showDock ? View.VISIBLE : View.GONE);
        }
        if (!showClear) {
            return;
        }
        // Ensure no leftover translation from older spacing experiments (breaks snapshots).
        if (rv.getTranslationY() != 0f) {
            rv.setTranslationY(0f);
        }

        DeviceProfile dp = rv.getDeviceProfile();
        Rect insets = rv.getOverviewInsets();
        int heightPx = dp.heightPx;
        int clearButtonHeight = clearPanel.getClearButtonHeight();
        int panelHeight = clearPanel.getMeasuredHeight();
        if (panelHeight == 0) {
            panelHeight = clearButtonHeight
                    + rv.getResources().getDimensionPixelSize(R.dimen.hxy_clear_btn_padding);
        }

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) clearPanel.getLayoutParams();
        if (lp == null) {
            lp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM);
            clearPanel.setLayoutParams(lp);
        }
        lp.width = FrameLayout.LayoutParams.MATCH_PARENT;
        lp.gravity = Gravity.BOTTOM;

        final int newBottomMargin;
        // Always Oppo nav-anchored Clear — do not lift the panel when dock/cards move.
        int bottom = rv.getBottom() - insets.bottom - clearPanel.getBottomMargin();
        int paddingTop = bottom - (clearButtonHeight / 2) + panelHeight;
        newBottomMargin = heightPx - paddingTop;
        // Avoid layout thrash: setLayoutParams during onLayout requests another pass.
        if (lp.bottomMargin != newBottomMargin) {
            lp.bottomMargin = newBottomMargin;
            clearPanel.setLayoutParams(lp);
        }
    }

    private static boolean isDockSupported(RecentsView<?, ?> rv) {
        DeviceProfile dp = rv.getDeviceProfile();
        return !dp.isTablet && !dp.isLandscape;
    }
}
