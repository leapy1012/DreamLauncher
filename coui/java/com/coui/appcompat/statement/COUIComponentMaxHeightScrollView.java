package com.coui.appcompat.statement;

import com.coui.appcompat.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;


public final class COUIComponentMaxHeightScrollView extends COUIMaxHeightScrollView {
    private boolean isProtocolFixed;

    public COUIComponentMaxHeightScrollView(Context context, AttributeSet attributeSet, int i2, DefaultConstructorMarker defaultConstructorMarker) {
        this(context, (i2 & 2) != 0 ? null : attributeSet);
    }

    public final boolean isProtocolFixed() {
        return this.isProtocolFixed;
    }

    @Override
    public void onMeasure(int i2, int i6) {
        if (getMaxHeight() > 0) {
            i6 = View.MeasureSpec.makeMeasureSpec(Math.min(getMaxHeight(), View.MeasureSpec.getSize(i6)), Integer.MIN_VALUE);
        }
        if (this.isProtocolFixed && getChildCount() > 0) {
            measureChild(getChildAt(0), i2, i6);
            setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), getChildAt(0).getMeasuredHeight() > View.MeasureSpec.getSize(i6) - getPaddingTop() ? getContext().getResources().getDimensionPixelOffset(R.dimen.coui_component_bottom_sheet_margin) : 0);
        }
        super.onMeasure(i2, i6);
    }

    public final void setProtocolFixed(boolean z6) {
        this.isProtocolFixed = z6;
    }


    public COUIComponentMaxHeightScrollView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        Intrinsics.checkNotNullParameter(context, "context");
    }
}
