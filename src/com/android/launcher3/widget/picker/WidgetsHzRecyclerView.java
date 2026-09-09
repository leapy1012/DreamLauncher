package com.android.launcher3.widget.picker;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.COUIRecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * ColorOS widget row. Matches {@code OplusWidgetsHzRecyclerView}: intercept only after a
 * clearly horizontal drag so a vertical pull from empty space can scroll the parent list.
 */
public class WidgetsHzRecyclerView extends COUIRecyclerView {

    private int mInitialTouchX;
    private int mInitialTouchY;
    private int mScrollPointerId = -1;
    private int mTouchSlop;

    public WidgetsHzRecyclerView(@NonNull Context context) {
        this(context, null);
    }

    public WidgetsHzRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WidgetsHzRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        setFlingRatio(0.35f);
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
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_MOVE
                && getScrollState() != SCROLL_STATE_DRAGGING) {
            int pointerIndex = e.findPointerIndex(mScrollPointerId);
            if (pointerIndex >= 0) {
                int dx = Math.round(e.getX(pointerIndex)) - mInitialTouchX;
                int dy = Math.round(e.getY(pointerIndex)) - mInitialTouchY;
                if (Math.abs(dy) > mTouchSlop && Math.abs(dy) > Math.abs(dx)) {
                    return false;
                }
            }
        }
        return super.onTouchEvent(e);
    }
}
