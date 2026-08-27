package com.coui.appcompat.statement;

import com.coui.appcompat.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import com.coui.appcompat.scrollview.COUIScrollView;


public class COUIMaxHeightScrollView extends COUIScrollView {
    private int mMinHeight;
    private int maxHeight;

    public COUIMaxHeightScrollView(Context context) {
        this(context, null);
    }

    public int getMaxHeight() {
        return this.maxHeight;
    }

    public int getMinHeight() {
        return this.mMinHeight;
    }

    @Override
    public void onMeasure(int i2, int i6) {
        int size = View.MeasureSpec.getSize(i6);
        int i10 = this.maxHeight;
        if (i10 > 0) {
            i6 = View.MeasureSpec.makeMeasureSpec(Math.min(i10, size), Integer.MIN_VALUE);
        }
        super.onMeasure(i2, i6);
        int measuredHeight = getMeasuredHeight();
        int i11 = this.mMinHeight;
        if (measuredHeight < i11) {
            super.onMeasure(i2, View.MeasureSpec.makeMeasureSpec(i11, 1073741824));
        }
    }

    public void setMaxHeight(int i2) {
        this.maxHeight = i2;
        requestLayout();
    }

    public void setMinHeight(int i2) {
        this.mMinHeight = i2;
        requestLayout();
    }

    public COUIMaxHeightScrollView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public COUIMaxHeightScrollView(Context context, AttributeSet attributeSet, int i2) {
        super(context, attributeSet, i2);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.COUIMaxHeightScrollView);
        this.maxHeight = typedArrayObtainStyledAttributes.getDimensionPixelOffset(R.styleable.COUIMaxHeightScrollView_scrollViewMaxHeight, 0);
        this.mMinHeight = typedArrayObtainStyledAttributes.getDimensionPixelOffset(R.styleable.COUIMaxHeightScrollView_scrollViewMinHeight, 0);
        typedArrayObtainStyledAttributes.recycle();
    }
}
