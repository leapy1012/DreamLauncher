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

package com.android.launcher3;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.android.quickstep.util.HxyRecentsLayoutManager;
import com.android.quickstep.views.HxyClearAllPanelView;
import com.android.quickstep.views.OverviewDockView;
import com.android.quickstep.views.RecentsView;

/**
 * Oppo-style overview shell: {@link RecentsView} with dock + Close pill as siblings.
 */
public class RecentContainerView extends InsettableFrameLayout {

    @Nullable
    private RecentsView<?, ?> mRecentsView;
    @Nullable
    private OverviewDockView mDockView;
    @Nullable
    private HxyClearAllPanelView mClearAllPanel;

    public RecentContainerView(Context context) {
        this(context, null);
    }

    public RecentContainerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mRecentsView = findViewById(R.id.overview_panel);
        mDockView = findViewById(R.id.overview_dock);
        mClearAllPanel = findViewById(R.id.hxy_clear_all_panel);
        if (mRecentsView != null) {
            mRecentsView.setBottomUi(mDockView, mClearAllPanel);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (!getResources().getBoolean(R.bool.config_clearall_center)) {
            return;
        }
        if (mRecentsView == null || mClearAllPanel == null) {
            return;
        }
        HxyRecentsLayoutManager.layoutBottomUi(mRecentsView, mClearAllPanel, mDockView);
    }

    @Nullable
    public RecentsView<?, ?> getRecentsView() {
        return mRecentsView;
    }

    @Nullable
    public OverviewDockView getDockView() {
        return mDockView;
    }

    @Nullable
    public HxyClearAllPanelView getClearAllPanel() {
        return mClearAllPanel;
    }

    /** Hide dock + Close on tablet / landscape, matching Oppo gating. */
    public boolean isDockSupported() {
        if (mRecentsView == null) {
            return false;
        }
        DeviceProfile dp = mRecentsView.getDeviceProfile();
        return !dp.isTablet && !dp.isLandscape;
    }

    public void updateBottomUiInsets() {
        if (!getResources().getBoolean(R.bool.config_clearall_center)) {
            return;
        }
        if (mRecentsView == null || mClearAllPanel == null) {
            return;
        }
        DeviceProfile dp = mRecentsView.getDeviceProfile();
        android.graphics.Rect taskRect = new android.graphics.Rect();
        mRecentsView.computeTaskSize(taskRect);
        boolean dockHidden = !isDockSupported();
        mClearAllPanel.onInsetsChanged(
                dp.heightPx, mRecentsView.getOverviewInsets(), taskRect, dockHidden, mDockView);
        requestLayout();
    }
}
