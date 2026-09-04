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

package com.android.quickstep.views;

import android.content.Context;
import android.graphics.Rect;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.launcher3.R;
import com.android.launcher3.util.DisplayController;
import com.android.launcher3.util.NavigationMode;

import java.text.DecimalFormat;

/**
 * Oppo-style Clear All pill panel — sibling of {@link RecentsView}, not inside
 * {@link OverviewActionsView}.
 */
public class HxyClearAllPanelView extends LinearLayout {

    /** Oppo {@code OplusClearAllPanelView.DOCK_SPACE_PERCENT}. */
    private static final float DOCK_SPACE_PERCENT = 0.55f;

    private Button mClearButton;
    private TextView mMemoryInfo;
    private FrameLayout.LayoutParams mPanelParams;
    private int mBottomMargin;

    public HxyClearAllPanelView(Context context) {
        this(context, null);
    }

    public HxyClearAllPanelView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HxyClearAllPanelView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mClearButton = findViewById(R.id.btn_clear);
        mMemoryInfo = findViewById(R.id.memeryinfo_textview);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        if (lp == null) {
            lp = new FrameLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
            setLayoutParams(lp);
        } else {
            lp.gravity = Gravity.BOTTOM;
            lp.width = LayoutParams.MATCH_PARENT;
        }
        mPanelParams = lp;
        updateMemoryInfoDisplay();
    }

    public Button getClearButton() {
        return mClearButton;
    }

    public int getBottomMargin() {
        return mBottomMargin;
    }

    public int getClearButtonHeight() {
        if (mClearButton == null) {
            return 0;
        }
        int h = mClearButton.getMeasuredHeight();
        if (h > 0) {
            return h;
        }
        return getResources().getDimensionPixelSize(R.dimen.overview_close_button_height);
    }

    /** Refresh available / total RAM line shown above Clear All. */
    public void updateMemoryInfoDisplay() {
        if (mMemoryInfo == null) {
            return;
        }
        Float availMemory = SystemMemory.getAvailMemoryFloat(getContext());
        String totalMemory = SystemMemory.getRealTotalRam(getContext());
        DecimalFormat df1 = new DecimalFormat("#.0");
        if (android.os.SystemProperties.get(
                "persist.sys.hxycustom.memory_expansion_size", "-1").trim().equals("-1")) {
            mMemoryInfo.setText(df1.format(availMemory) + "GB "
                    + getResources().getString(R.string.hxy_launcher_available)
                    + " | " + totalMemory);
            return;
        }
        int expansionEnabled = Settings.System.getInt(
                getContext().getContentResolver(), "hxy_memory_expansion_enable", 0);
        if (expansionEnabled == 1) {
            String expand = android.os.SystemProperties.get(
                    "persist.sys.hxycustom.memory_expansion_size", "1");
            float availSwapAndMemory = TextUtils.isEmpty(expand)
                    ? availMemory : availMemory + Long.parseLong(expand);
            mMemoryInfo.setText(df1.format(availSwapAndMemory) + "GB "
                    + getResources().getString(R.string.hxy_launcher_available) + " | "
                    + totalMemory.replaceAll("GB", " + ")
                    + android.os.SystemProperties.get(
                            "persist.sys.hxycustom.memory_expansion_size", "1")
                    + "GB");
        } else {
            mMemoryInfo.setText(df1.format(availMemory) + "GB "
                    + getResources().getString(R.string.hxy_launcher_available)
                    + " | " + totalMemory);
        }
    }

    /**
     * Positions Clear All using Oppo's live bottom-space math:
     * {@code bottomSpace = heightPx - insets.bottom - taskRect.bottom}.
     * <p>
     * That adapts to nav mode automatically: 3-button has a large {@code insets.bottom},
     * gesture has a thin one — but task cards claim space accordingly, so Clear stays in the
     * gap above the nav chrome. A fixed "stable" band (overview_actions=0) ignored that and
     * dropped Clear into the gesture handle.
     */
    public void onInsetsChanged(int heightPx, Rect insets, Rect taskRect, boolean dockHidden,
            @Nullable OverviewDockView dockView) {
        int bottomSpace = heightPx - insets.bottom - taskRect.bottom;
        if (bottomSpace < 0) {
            bottomSpace = 0;
        }
        if (dockHidden) {
            mBottomMargin = bottomSpace / 2;
            return;
        }

        int dockZone = (int) (bottomSpace * DOCK_SPACE_PERCENT);
        int halfDockZone = dockZone / 2;
        // Oppo: center Clear in the lower half of the gap under the dock zone.
        mBottomMargin = ((bottomSpace - dockZone) + halfDockZone) / 2;
        if (DisplayController.getNavigationMode(getContext()) == NavigationMode.THREE_BUTTONS) {
            mBottomMargin -= getResources().getDimensionPixelSize(R.dimen.hxy_three_bottom_margin);
        } else {
            // Gesture: slight extra air above the handle (Oppo gesture_bottom_margin).
            mBottomMargin += getResources().getDimensionPixelSize(R.dimen.hxy_gesture_bottom_margin);
        }
        if (mBottomMargin < 0) {
            mBottomMargin = 0;
        }

        int iconSize = getResources().getDimensionPixelSize(R.dimen.overview_dock_icon_size);
        int targetCardToDock = getResources().getDimensionPixelSize(R.dimen.hxy_card_to_dock_gap);
        int dockCenterY = taskRect.bottom + targetCardToDock + iconSize / 2;

        if (dockView != null) {
            dockView.calculateLocation(insets, dockCenterY, taskRect.width());
        }
    }
}
