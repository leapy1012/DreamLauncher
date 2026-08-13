package com.android.launcher.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import androidx.recyclerview.widget.COUIRecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

/** Direction-aware horizontal COUI widget list ported from the decoded OPPO launcher. */
public class OplusWidgetsHzRecyclerView extends COUIRecyclerView {
    private int mInitialTouchX;
    private int mInitialTouchY;
    private int mScrollPointerId;
    private int mTouchSlop;

    public OplusWidgetsHzRecyclerView(Context context) {
        this(context, null);
    }

    public OplusWidgetsHzRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public OplusWidgetsHzRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
    }

    @Override
    public void setScrollingTouchSlop(int slopConstant) {
        ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = slopConstant == TOUCH_SLOP_PAGING
                ? configuration.getScaledPagingTouchSlop()
                : configuration.getScaledTouchSlop();
        super.setScrollingTouchSlop(slopConstant);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int actionIndex = event.getActionIndex();
        if (action == MotionEvent.ACTION_DOWN) {
            mScrollPointerId = event.getPointerId(0);
            mInitialTouchX = Math.round(event.getX());
            mInitialTouchY = Math.round(event.getY());
            if (getScrollState() == SCROLL_STATE_SETTLING) {
                return false;
            }
            return super.onInterceptTouchEvent(event);
        }
        if (action == MotionEvent.ACTION_POINTER_DOWN) {
            mScrollPointerId = event.getPointerId(actionIndex);
            mInitialTouchX = Math.round(event.getX(actionIndex));
            mInitialTouchY = Math.round(event.getY(actionIndex));
            return super.onInterceptTouchEvent(event);
        }
        if (action != MotionEvent.ACTION_MOVE) {
            return super.onInterceptTouchEvent(event);
        }
        int pointerIndex = event.findPointerIndex(mScrollPointerId);
        if (pointerIndex < 0) {
            return false;
        }
        if (getScrollState() == SCROLL_STATE_DRAGGING) {
            return super.onInterceptTouchEvent(event);
        }
        int dx = Math.round(event.getX(pointerIndex)) - mInitialTouchX;
        int dy = Math.round(event.getY(pointerIndex)) - mInitialTouchY;
        boolean horizontalDrag = getLayoutManager() != null
                && getLayoutManager().canScrollHorizontally()
                && Math.abs(dx) > mTouchSlop && Math.abs(dx) > Math.abs(dy);
        boolean verticalDrag = getLayoutManager() != null
                && getLayoutManager().canScrollVertically()
                && Math.abs(dy) > mTouchSlop && Math.abs(dy) > Math.abs(dx);
        return (horizontalDrag || verticalDrag) && super.onInterceptTouchEvent(event);
    }
}
