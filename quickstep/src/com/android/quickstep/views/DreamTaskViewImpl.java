/*
 * Copyright (C) 2026 GreenDream
 */
package com.android.quickstep.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.touch.PagedOrientationHandler;
import com.android.quickstep.TaskUtils;
import com.android.quickstep.util.RecentsOrientedState;
import com.android.systemui.navigationbar.buttons.KeyButtonRipple;

/**
 * ColorOS-style Recents task card on top of the AOSP {@link TaskView}: header with the app name
 * and a ⋮ menu button that opens {@link DreamTaskMenuViewImpl}.
 */
public class DreamTaskViewImpl extends TaskView {

    @Nullable
    private ImageView mHxyMenu;
    @Nullable
    private TextView mHxyTaskName;

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
        mHxyTaskName = findViewById(R.id.hxy_task_name);
        mHxyMenu = findViewById(R.id.hxy_task_menu);
    }

    @Override
    protected void setIcon(IconView iconView, @Nullable Drawable icon) {
        super.setIcon(iconView, icon);
        if (icon == null) {
            return;
        }
        if (mHxyMenu != null) {
            mHxyMenu.setAlpha(1f);
            mHxyMenu.setEnabled(true);
            mHxyMenu.setClickable(true);
            mHxyMenu.setFocusable(true);
            mHxyMenu.setVisibility(VISIBLE);
            ensureMenuButtonRipple();
            mHxyMenu.setOnClickListener(v -> {
                if (confirmSecondSplitSelectApp()) {
                    return;
                }
                showTaskMenu(iconView);
            });
            mHxyMenu.setOnLongClickListener(v -> {
                requestDisallowInterceptTouchEvent(true);
                return showTaskMenu(iconView);
            });
        }
        if (mHxyTaskName != null) {
            mHxyTaskName.setText(TaskUtils.getTitle(getContext(), getTask()));
        }
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
        int iconSize = getResources().getDimensionPixelSize(R.dimen.hxy_task_thumbnail_icon_size);
        mIconView.setDrawableSize(iconSize, iconSize);
        FrameLayout.LayoutParams snapshotParams =
                (FrameLayout.LayoutParams) mSnapshotView.getLayoutParams();
        snapshotParams.topMargin = thumbnailTopMargin;
        mSnapshotView.setLayoutParams(snapshotParams);
    }

    /**
     * Oppo {@code OplusTaskHeaderView.setShortCutAlpha}: only the menu glyph fades; icon/title stay.
     * Ignore taps when largely transparent (off-center cards).
     */
    public void setMenuShortcutAlpha(float alpha) {
        if (mHxyMenu == null) {
            return;
        }
        float clamped = Utilities.boundToRange(alpha, 0f, 1f);
        mHxyMenu.setAlpha(clamped);
        boolean interactive = clamped > 0.4f;
        mHxyMenu.setEnabled(interactive);
        mHxyMenu.setClickable(interactive);
        mHxyMenu.setFocusable(interactive);
    }

    /** ColorOS {@code OplusKeyButtonRipple} oval press feedback on the ⋮ control. */
    private void ensureMenuButtonRipple() {
        if (mHxyMenu == null) {
            return;
        }
        if (mHxyMenu.getBackground() instanceof KeyButtonRipple) {
            return;
        }
        KeyButtonRipple ripple = new KeyButtonRipple(
                getContext(), mHxyMenu, R.dimen.hxy_task_menu_icon_size);
        ripple.setType(KeyButtonRipple.Type.OVAL);
        mHxyMenu.setBackground(ripple);
    }
}
