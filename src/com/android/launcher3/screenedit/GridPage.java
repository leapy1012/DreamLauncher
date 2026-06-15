package com.android.launcher3.screenedit;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroupOverlay;
import android.widget.GridView;
import android.widget.ListAdapter;

public class GridPage extends GridView {
    public GridPage(Context context) {
        this(context, null);
    }

    public LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return super.generateLayoutParams(attributeSet);
    }

    public ListAdapter getAdapter() {
        return super.getAdapter();
    }

    public ViewGroupOverlay getOverlay() {
        return super.getOverlay();
    }

    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (getLayoutParams().height != -2) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } else {
            super.onMeasure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(536870911, MeasureSpec.AT_MOST));
        }
    }

    public void setAdapter(ListAdapter adapter) {
        super.setAdapter(adapter);
    }

    public GridPage(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public GridPage(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }
}
