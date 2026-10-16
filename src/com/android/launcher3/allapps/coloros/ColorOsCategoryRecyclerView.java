package com.android.launcher3.allapps.coloros;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.COUIRecyclerView;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Categories list: Oppo soft edge fades and the same COUI spring overscroll as the All grid.
 * Bottom edge is layout-pinned above search.
 */
public class ColorOsCategoryRecyclerView extends COUIRecyclerView {

    private final ColorOsEdgeFadeHelper mFade = new ColorOsEdgeFadeHelper();
    private int mLastTouchAction = MotionEvent.ACTION_CANCEL;

    public ColorOsCategoryRecyclerView(Context context) {
        this(context, null);
    }

    public ColorOsCategoryRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorOsCategoryRecyclerView(Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setEnablePointerDownAction(false);
        setClipChildren(true);
        setClipToPadding(true);
        if (ColorOsEdgeFadeHelper.shouldUse(context)) {
            mFade.setEnabled(true);
            mFade.configureFromResources(context.getResources());
            mFade.ensureHostLayer(this);
            addOnScrollListener(new OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                    mFade.applyEdgeAlphas(ColorOsCategoryRecyclerView.this);
                }
            });
        }
    }

    public ColorOsEdgeFadeHelper getEdgeFade() {
        return mFade;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean intercept = super.onInterceptTouchEvent(ev);
        // COUIRecyclerView claims a touch that lands on a still-moving list. At either end the
        // drawer's swipe-to-close must still be able to take it.
        if (intercept && ev.getActionMasked() == MotionEvent.ACTION_DOWN
                && (!canScrollVertically(-1) || !canScrollVertically(1))) {
            getParent().requestDisallowInterceptTouchEvent(false);
        }
        return intercept;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        int actionMasked = ev.getActionMasked();
        // When the drawer's close gesture steals the touch during a top bounce, treat the cancel
        // as a release so the bounce springs back instead of freezing.
        if (actionMasked == MotionEvent.ACTION_CANCEL && getScrollY() < 0
                && mLastTouchAction != MotionEvent.ACTION_DOWN) {
            ev.setAction(MotionEvent.ACTION_UP);
        }
        mLastTouchAction = actionMasked;
        boolean handled = super.onTouchEvent(ev);
        ev.setAction(action);
        return handled;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (mFade.isEnabled()) {
            mFade.applyEdgeAlphas(this);
        }
    }

    @Override
    public void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mFade.isEnabled()) {
            mFade.drawSoftScrims(this, canvas);
        }
    }
}
