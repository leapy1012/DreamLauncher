/*
 * Copyright (C) 2026 GreenDream
 */
package com.android.quickstep.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.android.launcher3.R;

/**
 * ColorOS-style split (grouped) task card: larger app icons pinned under the card header.
 */
public class DreamGroupedTaskView extends GroupedTaskView {

    public DreamGroupedTaskView(Context context) {
        this(context, null);
    }

    public DreamGroupedTaskView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DreamGroupedTaskView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onPrepareSplitIconPlacement() {
        int iconSize = getResources().getDimensionPixelSize(R.dimen.color_task_header_view_icon_size);
        int topMargin = getResources().getDimensionPixelSize(
                R.dimen.split_recent_first_snapview_topmargin1);
        mIconView.setDrawableSize(iconSize, iconSize);
        mIconView2.setDrawableSize(iconSize, iconSize);
        FrameLayout.LayoutParams iconParams =
                (FrameLayout.LayoutParams) mIconView.getLayoutParams();
        FrameLayout.LayoutParams iconParams2 =
                (FrameLayout.LayoutParams) mIconView2.getLayoutParams();
        iconParams.topMargin = topMargin;
        iconParams2.topMargin = topMargin;
        mIconView.setLayoutParams(iconParams);
        mIconView2.setLayoutParams(iconParams2);
    }
}
