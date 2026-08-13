/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package com.android.launcher3.views;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Outline;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;

import com.android.launcher3.R;
import com.android.launcher3.util.Themes;

/**
 * OPPO widget-sheet container which clips only its top corners.
 *
 * <p>This is ported from the decoded launcher implementation. Keeping this as a
 * {@link SpringRelativeLayout} preserves Launcher's overscroll edge effects while matching
 * the ColorOS sheet outline.</p>
 */
public class TopRoundedCornerView extends SpringRelativeLayout {

    private final Paint mNavBarScrimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float mCornerRadius;
    private int mNavBarScrimHeight;

    public TopRoundedCornerView(Context context) {
        this(context, null);
    }

    public TopRoundedCornerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TopRoundedCornerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mCornerRadius = getResources().getDimension(R.dimen.bg_round_rect_radius);
        mNavBarScrimPaint.setColor(Themes.getAttrColor(context, R.attr.allAppsNavBarScrimColor));
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                // Extending the bottom by the radius keeps its corners square while preserving
                // OPPO's rounded top. This avoids Canvas.clipPath corruption on the MTK GPU.
                outline.setRoundRect(0, 0, view.getWidth(),
                        Math.round(view.getHeight() + mCornerRadius), mCornerRadius);
            }
        });
        setClipToOutline(true);
    }

    public void setNavBarScrimHeight(int navBarScrimHeight) {
        mNavBarScrimHeight = navBarScrimHeight;
        invalidate();
    }
}
