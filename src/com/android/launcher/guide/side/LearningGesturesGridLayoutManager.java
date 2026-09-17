package com.android.launcher.guide.side;

import android.content.Context;

import androidx.recyclerview.widget.GridLayoutManager;

/** Two-column grid whose scrolling can be disabled like the ColorOS learning page. */
final class LearningGesturesGridLayoutManager extends GridLayoutManager {

    private boolean mIsScrollEnabled = true;

    LearningGesturesGridLayoutManager(Context context, int spanCount) {
        super(context, spanCount);
    }

    void setScrollEnabled(boolean enabled) {
        mIsScrollEnabled = enabled;
    }

    @Override
    public boolean canScrollVertically() {
        return mIsScrollEnabled && super.canScrollVertically();
    }
}
