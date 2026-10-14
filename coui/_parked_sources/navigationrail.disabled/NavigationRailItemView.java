package com.coui.appcompat.material.navigationrail;

import android.view.View;
import androidx.annotation.DimenRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.RestrictTo;
import com.coui.appcompat.material.navigation.NavigationBarItemView;
import com.google.android.material.R;

/* JADX INFO: loaded from: classes4.dex */
@RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
final class NavigationRailItemView extends NavigationBarItemView {
    @Override // com.coui.appcompat.material.navigation.NavigationBarItemView
    @DimenRes
    public final int getItemDefaultMarginResId() {
        return R.dimen.mtrl_navigation_rail_icon_margin;
    }

    @Override // com.coui.appcompat.material.navigation.NavigationBarItemView
    @LayoutRes
    public final int getItemLayoutResId() {
        return R.layout.mtrl_navigation_rail_item;
    }

    @Override // android.widget.FrameLayout, android.view.View
    public final void onMeasure(int i3, int i10) {
        super.onMeasure(i3, i10);
        if (View.MeasureSpec.getMode(i10) == 0) {
            setMeasuredDimension(getMeasuredWidthAndState(), View.resolveSizeAndState(Math.max(getMeasuredHeight(), View.MeasureSpec.getSize(i10)), i10, 0));
        }
    }
}
