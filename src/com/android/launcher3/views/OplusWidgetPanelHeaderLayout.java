/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package com.android.launcher3.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.recyclerview.widget.RecyclerView;

/**
 * Compatibility bridge for the OPPO widget-panel hierarchy.
 *
 * <p>The local picker expects a {@link StickyHeaderLayout}, while ColorOS lays the toolbar out
 * directly above the recycler and does not insert AOSP's synthetic header row. This subclass
 * keeps the existing picker contract but disables sticky-header tracking and its spacer.</p>
 */
public class OplusWidgetPanelHeaderLayout extends StickyHeaderLayout {

    public OplusWidgetPanelHeaderLayout(Context context) {
        this(context, null);
    }

    public OplusWidgetPanelHeaderLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OplusWidgetPanelHeaderLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setCurrentRecyclerView(RecyclerView currentRecyclerView) {
        // ColorOS places the recycler below this complete toolbar block.
    }

    @Override
    public int getHeaderHeight() {
        // The AOSP adapter still creates its compatibility spacer; keep it at zero height.
        return 0;
    }

    @Override
    public void reset(boolean animate) {
        // No translated sticky children exist in the ColorOS hierarchy.
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }
}
