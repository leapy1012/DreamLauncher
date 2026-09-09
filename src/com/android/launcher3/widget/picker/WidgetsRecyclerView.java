/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.launcher3.widget.picker;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener;

import com.android.launcher3.FastScrollRecyclerView;
import com.android.launcher3.R;
import com.android.launcher3.util.ScrollableLayoutManager;

/**
 * The widgets recycler view.
 */
public class WidgetsRecyclerView extends FastScrollRecyclerView implements OnItemTouchListener {

    private WidgetsListAdapter mAdapter;

    private final int mScrollbarTop;

    private final Point mFastScrollerOffset = new Point();
    private boolean mTouchDownOnScroller;
    private HeaderViewDimensionsProvider mHeaderViewDimensionsProvider;
    private int mInitialTouchX;
    private int mInitialTouchY;
    private int mScrollPointerId = -1;
    private int mTouchSlop;

    public WidgetsRecyclerView(Context context) {
        this(context, null);
    }

    public WidgetsRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WidgetsRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        // API 21 and below only support 3 parameter ctor.
        super(context, attrs, defStyleAttr);
        mScrollbarTop = getResources().getDimensionPixelSize(R.dimen.dynamic_grid_edge_margin);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        addOnItemTouchListener(this);
    }

    @Override
    public void setScrollingTouchSlop(int slopConstant) {
        ViewConfiguration configuration = ViewConfiguration.get(getContext());
        if (slopConstant == RecyclerView.TOUCH_SLOP_DEFAULT) {
            mTouchSlop = configuration.getScaledTouchSlop();
        } else if (slopConstant == RecyclerView.TOUCH_SLOP_PAGING) {
            mTouchSlop = configuration.getScaledPagingTouchSlop();
        }
        super.setScrollingTouchSlop(slopConstant);
    }

    /**
     * Oppo {@code OplusWidgetsRecyclerView}: take the gesture only when movement matches
     * this list's axis so a vertical pull inside a widget row is not eaten by the row.
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        LayoutManager layoutManager = getLayoutManager();
        if (layoutManager == null) {
            return super.onInterceptTouchEvent(e);
        }
        boolean canScrollHorizontally = layoutManager.canScrollHorizontally();
        boolean canScrollVertically = layoutManager.canScrollVertically();
        int actionMasked = e.getActionMasked();
        int actionIndex = e.getActionIndex();
        if (actionMasked == MotionEvent.ACTION_DOWN) {
            mScrollPointerId = e.getPointerId(0);
            mInitialTouchX = Math.round(e.getX());
            mInitialTouchY = Math.round(e.getY());
            if (getScrollState() == SCROLL_STATE_SETTLING) {
                return false;
            }
            return super.onInterceptTouchEvent(e);
        }
        if (actionMasked != MotionEvent.ACTION_MOVE) {
            if (actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                mScrollPointerId = e.getPointerId(actionIndex);
                mInitialTouchX = Math.round(e.getX(actionIndex));
                mInitialTouchY = Math.round(e.getY(actionIndex));
            }
            return super.onInterceptTouchEvent(e);
        }
        int pointerIndex = e.findPointerIndex(mScrollPointerId);
        if (pointerIndex < 0) {
            return false;
        }
        int x = Math.round(e.getX(pointerIndex));
        int y = Math.round(e.getY(pointerIndex));
        if (getScrollState() == SCROLL_STATE_DRAGGING) {
            return super.onInterceptTouchEvent(e);
        }
        int dx = x - mInitialTouchX;
        int dy = y - mInitialTouchY;
        boolean intercept = canScrollHorizontally
                && Math.abs(dx) > mTouchSlop && Math.abs(dx) > Math.abs(dy);
        if (canScrollVertically && Math.abs(dy) > mTouchSlop && Math.abs(dy) > Math.abs(dx)) {
            intercept = true;
        }
        return intercept && super.onInterceptTouchEvent(e);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setLayoutManager(new ScrollableLayoutManager(getContext()));
    }

    @Override
    public void setAdapter(Adapter adapter) {
        super.setAdapter(adapter);
        mAdapter = (WidgetsListAdapter) adapter;
    }

    /**
     * Maps the touch (from 0..1) to the adapter position that should be visible.
     */
    @Override
    public String scrollToPositionAtProgress(float touchFraction) {
        // Skip early if widgets are not bound.
        if (isModelNotReady()) {
            return "";
        }

        // Stop the scroller if it is scrolling
        stopScroll();

        int rowCount = mAdapter.getItemCount();
        float pos = rowCount * touchFraction;
        int availableScrollHeight = getAvailableScrollHeight();
        LinearLayoutManager layoutManager = ((LinearLayoutManager) getLayoutManager());
        layoutManager.scrollToPositionWithOffset(0, (int) -(availableScrollHeight * touchFraction));

        int posInt = (int) ((touchFraction == 1) ? pos - 1 : pos);
        return mAdapter.getSectionName(posInt);
    }

    /**
     * Updates the bounds for the scrollbar.
     */
    @Override
    public void onUpdateScrollbar(int dy) {
        // Skip early if widgets are not bound.
        if (isModelNotReady()) {
            mScrollbar.setThumbOffsetY(-1);
            return;
        }

        // Skip early if, there no child laid out in the container.
        int scrollY = computeVerticalScrollOffset();
        if (scrollY < 0) {
            mScrollbar.setThumbOffsetY(-1);
            return;
        }

        synchronizeScrollBarThumbOffsetToViewScroll(scrollY, getAvailableScrollHeight());
    }

    private boolean isModelNotReady() {
        return mAdapter.getItemCount() == 0;
    }

    @Override
    public int getScrollBarTop() {
        return mHeaderViewDimensionsProvider == null
                ? mScrollbarTop
                : mHeaderViewDimensionsProvider.getHeaderViewHeight() + mScrollbarTop;
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            mTouchDownOnScroller = isHitOnScrollBar(e);
        }
        if (mTouchDownOnScroller) {
            final boolean result = mScrollbar.handleTouchEvent(e, mFastScrollerOffset);
            return result;
        }
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        if (mTouchDownOnScroller) {
            mScrollbar.handleTouchEvent(e, mFastScrollerOffset);
        }
    }

    /**
     * Detects whether a {@code MotionEvent} is on the scroll bar
     * @param e The {@code MotionEvent} on the screen
     * @return {@code true} if the motion is on the scroll bar
     */
    boolean isHitOnScrollBar(MotionEvent e) {
        return mScrollbar.isHitInParent(e.getX(), e.getY(), mFastScrollerOffset);
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }

    public void setHeaderViewDimensionsProvider(
            HeaderViewDimensionsProvider headerViewDimensionsProvider) {
        mHeaderViewDimensionsProvider = headerViewDimensionsProvider;
    }

    /**
     * Provides dimensions of the header view that is shown at the top of a
     * {@link WidgetsRecyclerView}.
     */
    public interface HeaderViewDimensionsProvider {
        /**
         * Returns the height, in pixels, of the header view that is shown at the top of a
         * {@link WidgetsRecyclerView}.
         */
        int getHeaderViewHeight();
    }
}
