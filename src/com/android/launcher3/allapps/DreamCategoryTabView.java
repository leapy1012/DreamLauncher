/*
 * Copyright (C) 2026 GreenDream
 */
package com.android.launcher3.allapps;

import android.content.Context;
import android.util.AttributeSet;

import com.android.launcher3.Utilities;
import com.android.launcher3.pageindicators.PageIndicator;
import com.coui.appcompat.segmentbutton.COUISegmentButtonLayout;

/**
 * All / Categories segment pill and page indicator of {@link DreamCategoryPagedView}: the
 * selected pill follows the page scroll, and the selected segment follows the settling page.
 */
public class DreamCategoryTabView extends COUISegmentButtonLayout implements PageIndicator {

    public DreamCategoryTabView(Context context) {
        super(context);
    }

    public DreamCategoryTabView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DreamCategoryTabView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setScroll(int currentScroll, int totalScroll) {
        int lastPosition = getSegmentCount() - 1;
        if (totalScroll <= 0 || lastPosition <= 0) {
            return;
        }
        float progress = Utilities.boundToRange((float) currentScroll / totalScroll, 0f, 1f)
                * lastPosition;
        int position = Math.min((int) progress, lastPosition);
        onPageScrolled(position, progress - position, 0);
    }

    @Override
    public void setActiveMarker(int activePage) {
        selectSegmentAt(activePage);
    }

    @Override
    public void setMarkersCount(int numMarkers) {
    }
}
