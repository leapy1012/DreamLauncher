/*
 * Copyright (C) 2026 GreenDream
 */
package com.android.launcher3.allapps;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.Nullable;

import com.android.launcher.DreamPagedViewImpl;
import com.android.launcher3.Utilities;

/**
 * ColorOS drawer pager: page 0 is the All list, page 1 is Categories. Pages follow the finger
 * and settle with the {@link DreamPagedViewImpl} spring; {@link DreamCategoryTabView} tracks the
 * scroll.
 */
public class DreamCategoryPagedView extends DreamPagedViewImpl<DreamCategoryTabView> {

    public static final int PAGE_ALL = 0;
    public static final int PAGE_CATEGORIES = 1;

    /** Drags steeper than this never page. */
    public static final float MAX_SWIPE_ANGLE = (float) Math.PI / 3;
    /** Above this angle the touch slop grows, so diagonal drags favour the lists. */
    public static final float START_DAMPING_TOUCH_SLOP_ANGLE = (float) Math.PI / 6;
    public static final float TOUCH_SLOP_DAMPING_FACTOR = 4f;
    /** Page switch duration for a tab tap. */
    public static final int TAB_SNAP_DURATION = 600;

    /** Host hooks for the drawer chrome. */
    public interface Callbacks {
        /** False while select mode, search, a category drag or the letter cluster owns touch. */
        boolean canSwipePages();

        /** @param progress 0 on All, 1 on Categories. */
        void onPagesScrolled(float progress);

        void onPageSettled(int page);
    }

    @Nullable private Callbacks mCallbacks;

    public DreamCategoryPagedView(Context context) {
        this(context, null);
    }

    public DreamCategoryPagedView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DreamCategoryPagedView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setEnableOverscroll(false);
    }

    public void setCallbacks(@Nullable Callbacks callbacks) {
        mCallbacks = callbacks;
    }

    public void setTabView(@Nullable DreamCategoryTabView tabView) {
        mPageIndicator = tabView;
        if (tabView != null) {
            tabView.setMarkersCount(getChildCount());
            tabView.setActiveMarker(getNextPage());
        }
    }

    /** True while a drag or a settle is moving the pages. */
    public boolean isMoving() {
        return isHandlingTouch() || isPageInTransition();
    }

    private boolean canSwipePages() {
        return mCallbacks == null || mCallbacks.canSwipePages();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!isHandlingTouch() && !canSwipePages()) {
            return false;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!isHandlingTouch() && !canSwipePages()) {
            return false;
        }
        return super.onTouchEvent(ev);
    }

    @Override
    protected boolean canScroll(float absVScroll, float absHScroll) {
        return absHScroll > absVScroll && super.canScroll(absVScroll, absHScroll);
    }

    @Override
    protected void determineScrollingStart(MotionEvent ev) {
        float absX = Math.abs(ev.getX() - getDownMotionX());
        float absY = Math.abs(ev.getY() - getDownMotionY());
        if (Float.compare(absX, 0f) == 0) {
            return;
        }
        if (absX > mTouchSlop || absY > mTouchSlop) {
            cancelCurrentPageLongPress();
        }
        float angle = (float) Math.atan(absY / absX);
        if (angle > MAX_SWIPE_ANGLE) {
            return;
        }
        if (angle > START_DAMPING_TOUCH_SLOP_ANGLE) {
            float extent = (float) Math.sqrt((angle - START_DAMPING_TOUCH_SLOP_ANGLE)
                    / START_DAMPING_TOUCH_SLOP_ANGLE);
            super.determineScrollingStart(ev, 1 + TOUCH_SLOP_DAMPING_FACTOR * extent);
        } else {
            super.determineScrollingStart(ev);
        }
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        int maxScroll = computeMaxScroll();
        if (mPageIndicator != null) {
            mPageIndicator.setScroll(l, maxScroll);
        }
        if (mCallbacks != null && maxScroll > 0) {
            mCallbacks.onPagesScrolled(Utilities.boundToRange((float) l / maxScroll, 0f, 1f));
        }
    }

    @Override
    protected void onPageEndTransition() {
        super.onPageEndTransition();
        if (mCallbacks != null) {
            // draw() can end the transition before computeScroll commits mNextPage.
            mCallbacks.onPageSettled(getNextPage());
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}
