/*
 * Copyright (C) 2026 GreenDream
 */
package com.android.launcher;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.android.launcher3.PagedView;
import com.android.launcher3.pageindicators.PageIndicator;

/**
 * Dream paging behavior on top of the AOSP {@link PagedView}: a page released with a fling
 * settles with a spring that carries the finger velocity, instead of the AOSP timed scroll.
 * Programmatic snaps ({@link #snapToPage(int)} etc.) keep the AOSP timed scroll.
 */
public abstract class DreamPagedViewImpl<T extends View & PageIndicator> extends PagedView<T> {

    private static final float MAX_SPRING_SCROLL_TO_PAGE_VELOCITY = 8000f;

    public DreamPagedViewImpl(Context context) {
        this(context, null);
    }

    public DreamPagedViewImpl(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DreamPagedViewImpl(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected boolean snapToPageWithVelocity(int whichPage, int velocity) {
        if (Math.abs(velocity) < mMinFlingVelocity) {
            return super.snapToPageWithVelocity(whichPage, velocity);
        }
        whichPage = validateNewPage(whichPage);
        int halfScreenSize = mOrientationHandler.getMeasuredSize(this) / 2;
        int delta = getScrollForPage(whichPage) - mOrientationHandler.getPrimaryScroll(this);

        float distanceRatio = Math.min(1f, 1.0f * Math.abs(delta) / (2 * halfScreenSize));
        float distance = halfScreenSize
                + halfScreenSize * distanceInfluenceForSnapDuration(distanceRatio);
        int snapVelocity = Math.max(mMinSnapVelocity, Math.abs(velocity));
        int duration = 4 * Math.round(1000 * Math.abs(distance / snapVelocity));

        return dreamSnapToPageSpring(whichPage, delta, duration, velocity);
    }

    /**
     * Springs to {@code whichPage}.
     *
     * @param duration estimated settle time, reported by {@code mScroller.getDuration()}.
     * @param primaryVelocity finger velocity along the paging axis, in pixels per second.
     */
    protected boolean dreamSnapToPageSpring(int whichPage, int delta, int duration,
            int primaryVelocity) {
        if (mFirstLayout) {
            setCurrentPage(whichPage);
            return false;
        }
        whichPage = validateNewPage(whichPage);
        mNextPage = whichPage;

        awakenScrollBars(duration);
        if (duration == 0) {
            duration = Math.abs(delta);
        }
        if (duration != 0) {
            pageBeginTransition();
        }
        if (!mScroller.isFinished()) {
            abortScrollerAnimation(false);
        }

        float velocity = primaryVelocity;
        if (Math.abs(velocity) >= MAX_SPRING_SCROLL_TO_PAGE_VELOCITY) {
            velocity = Math.copySign(MAX_SPRING_SCROLL_TO_PAGE_VELOCITY, velocity);
        }
        // The page moves with the finger, so scrolling runs against the finger velocity.
        mScroller.startScrollSpring(
                mOrientationHandler.getPrimaryScroll(this), delta, duration, -velocity);
        updatePageIndicator();

        invalidate();
        return Math.abs(delta) > 0;
    }
}
