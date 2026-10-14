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

package com.dream.quickstep.views;

import android.content.Context;
import android.graphics.Rect;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.launcher3.R;
import com.android.launcher3.util.Executors;
import com.android.launcher3.util.DisplayController;
import com.android.launcher3.util.NavigationMode;
import com.android.quickstep.views.SystemMemory;
import com.dream.quickstep.dock.DockView;

import java.text.DecimalFormat;

/**
 * Oppo-style Clear All pill panel — sibling of {@link RecentsView}, not inside
 * {@link OverviewActionsView}.
 */
public class DreamClearAllPanelView extends LinearLayout {

    /** Oppo {@code OplusClearAllPanelView.DOCK_SPACE_PERCENT}. */
    private static final float DOCK_SPACE_PERCENT = 0.55f;

    private Button mClearButton;
    private TextView mMemoryInfo;
    private FrameLayout.LayoutParams mPanelParams;
    private int mBottomMargin;
    private boolean mMemoryRefreshPending;

    public DreamClearAllPanelView(Context context) {
        this(context, null);
    }

    public DreamClearAllPanelView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DreamClearAllPanelView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mClearButton = findViewById(R.id.btn_clear);
        mMemoryInfo = findViewById(R.id.memeryinfo_textview);
        setAlpha(0f);
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

    /** Refresh once on entry, with proc/Binder/settings work off the animation thread. */
    public void updateMemoryInfoDisplay() {
        if (mMemoryInfo == null || mMemoryRefreshPending) {
            return;
        }
        mMemoryRefreshPending = true;
        Context context = getContext().getApplicationContext();
        Executors.THREAD_POOL_EXECUTOR.execute(() -> {
            String memoryText = null;
            try {
                memoryText = loadMemoryInfoText(context);
            } catch (RuntimeException e) {
                Log.w("DreamClearAllPanelView", "Unable to refresh memory information", e);
            }
            final String text = memoryText;
            Executors.MAIN_EXECUTOR.execute(() -> {
                mMemoryRefreshPending = false;
                if (text != null && mMemoryInfo != null) {
                    mMemoryInfo.setText(text);
                }
            });
        });
    }

    private static String loadMemoryInfoText(Context context) {
        float availMemory = SystemMemory.getAvailMemoryFloat(context);
        String totalMemory = SystemMemory.getRealTotalRam(context);
        DecimalFormat format = new DecimalFormat("#.0");
        String expansion = android.os.SystemProperties.get(
                "persist.sys.hxycustom.memory_expansion_size", "-1").trim();
        if (!"-1".equals(expansion) && Settings.System.getInt(
                context.getContentResolver(), "hxy_memory_expansion_enable", 0) == 1) {
            long expansionGb = TextUtils.isEmpty(expansion) ? 0 : Long.parseLong(expansion);
            availMemory += expansionGb;
            totalMemory = totalMemory.replace("GB", " + ") + expansionGb + "GB";
        }
        return format.format(availMemory) + "GB "
                + context.getString(R.string.hxy_launcher_available) + " | " + totalMemory;
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
            @Nullable DockView dockView) {
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
