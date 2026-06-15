package com.android.launcher3.screenedit;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.viewpager.widget.ViewPager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GridGalleryPager extends ViewPager {

    public GridGalleryPager(@NotNull Context context) {
        this(context, (AttributeSet) null, 2);
    }

    public GridGalleryPager(Context context, AttributeSet attributeSet, int i) {
        this(context, (i & 2) != 0 ? null : attributeSet);
    }

    public void onMeasure(int i, int i2) {
        if (getLayoutParams().height != -2) {
            super.onMeasure(i, i2);
            return;
        }
        int childCount = getChildCount();
        int i3 = i2;
        int i4 = 0;
        for (int i5 = 0; i5 < childCount; i5++) {
            View childAt = getChildAt(i5);
            childAt.measure(i, ViewGroup.getChildMeasureSpec(i2, 0, childAt.getLayoutParams().height));
            int measuredHeight = childAt.getMeasuredHeight();
            if (measuredHeight > i4) {
                i4 = measuredHeight;
            }
            i3 = View.MeasureSpec.makeMeasureSpec(i4, MeasureSpec.EXACTLY);
        }
        super.onMeasure(i, i3);
    }

    public GridGalleryPager(@NotNull Context context, @Nullable AttributeSet attributeSet) {
        super(context, attributeSet);
    }
}
