/*
 * Copyright (C) 2026 GreenDream
 */
package com.android.quickstep.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.R;
import com.android.launcher3.touch.PagedOrientationHandler;
import com.android.quickstep.TaskUtils;
import com.android.quickstep.util.RecentsOrientedState;
import com.dream.quickstep.views.DreamTaskHeaderView;

/**
 * ColorOS-style Recents task card on top of the AOSP {@link TaskView}: a
 * {@link DreamTaskHeaderView} with the app name and a ⋮ menu button that opens
 * {@link DreamTaskMenuViewImpl}.
 */
public class DreamTaskViewImpl extends TaskView {

    @Nullable
    private DreamTaskHeaderView mHeader;

    public DreamTaskViewImpl(Context context) {
        this(context, null);
    }

    public DreamTaskViewImpl(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DreamTaskViewImpl(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public DreamTaskViewImpl(
            Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mHeader = findViewById(R.id.dream_task_header);
    }

    @Nullable
    public DreamTaskHeaderView getHeader() {
        return mHeader;
    }

    @Override
    protected void setIcon(IconView iconView, @Nullable Drawable icon) {
        super.setIcon(iconView, icon);
        if (icon == null || mHeader == null) {
            return;
        }
        mHeader.setShortCutAlpha(1f);
        ImageView menuBtn = mHeader.getMenuBtn();
        menuBtn.setVisibility(VISIBLE);
        menuBtn.setOnClickListener(v -> {
            if (confirmSecondSplitSelectApp()) {
                return;
            }
            showTaskMenu(iconView);
        });
        menuBtn.setOnLongClickListener(v -> {
            requestDisallowInterceptTouchEvent(true);
            return showTaskMenu(iconView);
        });
        mHeader.setTitle(TaskUtils.getTitle(getContext(), getTask()));
    }

    @Override
    protected boolean showTaskMenuWithContainer(IconView iconView) {
        if (mActivity.getDeviceProfile().isTablet) {
            return super.showTaskMenuWithContainer(iconView);
        }
        TaskIdAttributeContainer menuContainer =
                getTaskIdAttributeContainers()[iconView == mIconView ? 0 : 1];
        return DreamTaskMenuViewImpl.showForTask(menuContainer);
    }

    @Override
    protected void setIconOrientation(RecentsOrientedState orientationState) {
        PagedOrientationHandler orientationHandler = orientationState.getOrientationHandler();
        boolean isRtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        DeviceProfile deviceProfile = mActivity.getDeviceProfile();
        int thumbnailTopMargin = deviceProfile.overviewTaskThumbnailTopMarginPx;
        boolean isGridTask = isGridTask();
        if (deviceProfile.isMultiWindowMode) {
            LayoutParams iconParams = (LayoutParams) mIconView.getLayoutParams();
            int taskIconHeight = deviceProfile.overviewTaskIconSizePx;
            int taskMargin = deviceProfile.overviewTaskMarginPx;
            int taskIconMargin = thumbnailTopMargin - taskIconHeight - taskMargin;
            orientationHandler.setTaskIconParams(iconParams, taskIconMargin, taskIconHeight,
                    thumbnailTopMargin, isRtl);
            iconParams.width = iconParams.height = taskIconHeight;
            mIconView.setLayoutParams(iconParams);
            mIconView.setRotation(orientationHandler.getDegreesRotated());
            int iconDrawableSize = isGridTask ? deviceProfile.overviewTaskIconDrawableSizeGridPx
                    : deviceProfile.overviewTaskIconDrawableSizePx;
            mIconView.setDrawableSize(iconDrawableSize, iconDrawableSize);
            return;
        }
        int iconSize = getResources().getDimensionPixelSize(
                R.dimen.color_task_header_view_icon_size);
        mIconView.setDrawableSize(iconSize, iconSize);
        FrameLayout.LayoutParams snapshotParams =
                (FrameLayout.LayoutParams) mSnapshotView.getLayoutParams();
        snapshotParams.topMargin = thumbnailTopMargin;
        mSnapshotView.setLayoutParams(snapshotParams);
    }
}
