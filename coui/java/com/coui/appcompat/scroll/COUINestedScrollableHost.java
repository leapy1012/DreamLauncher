package com.coui.appcompat.scroll;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.coui.appcompat.viewpager.COUIViewPager2;

/**
 * Coordinates vertical child scroll vs horizontal {@link COUIViewPager2} paging.
 * <p>
 * COUIRecyclerView aggressively calls {@code requestDisallowInterceptTouchEvent(true)};
 * without this host, left/right page swipes never reach the pager.
 */
public class COUINestedScrollableHost extends FrameLayout {

    private final int mTouchSlop;
    private float mInitialX;
    private float mInitialY;
    private boolean mIsBeingDragged;

    public COUINestedScrollableHost(@NonNull Context context) {
        this(context, null);
    }

    public COUINestedScrollableHost(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Nullable
    private COUIViewPager2 findParentViewPager() {
        ViewParent parent = getParent();
        while (parent != null && !(parent instanceof COUIViewPager2) && parent instanceof View) {
            parent = parent.getParent();
        }
        return parent instanceof COUIViewPager2 ? (COUIViewPager2) parent : null;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        handleTouch(ev);
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        handleTouch(event);
        return super.onTouchEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // Run before children so we can undo COUIRecyclerView's disallow calls.
        handleTouch(ev);
        return super.dispatchTouchEvent(ev);
    }

    private void handleTouch(MotionEvent e) {
        COUIViewPager2 viewPager = findParentViewPager();
        if (viewPager == null || !viewPager.isUserInputEnabled()) {
            return;
        }
        if (viewPager.getOrientation() != COUIViewPager2.ORIENTATION_HORIZONTAL) {
            return;
        }

        final int action = e.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            mInitialX = e.getX();
            mInitialY = e.getY();
            mIsBeingDragged = false;
            // Do not lock parents yet; wait until direction is known.
            requestDisallowUp(false);
        } else if (action == MotionEvent.ACTION_MOVE) {
            float dx = e.getX() - mInitialX;
            float dy = e.getY() - mInitialY;
            float absDx = Math.abs(dx);
            float absDy = Math.abs(dy);
            if (!mIsBeingDragged && (absDx > mTouchSlop || absDy > mTouchSlop)) {
                mIsBeingDragged = true;
                if (absDx > absDy) {
                    // Horizontal → let COUIViewPager2 intercept; child must not lock parents.
                    requestDisallowUp(false);
                } else {
                    // Vertical → keep gesture for the grid.
                    requestDisallowUp(true);
                }
            } else if (mIsBeingDragged && absDx > absDy) {
                requestDisallowUp(false);
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            mIsBeingDragged = false;
            requestDisallowUp(false);
        }
    }

    private void requestDisallowUp(boolean disallow) {
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallow);
        }
    }
}
