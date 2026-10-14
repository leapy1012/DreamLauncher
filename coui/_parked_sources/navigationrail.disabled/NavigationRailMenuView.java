package com.coui.appcompat.material.navigationrail;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Px;
import androidx.annotation.RestrictTo;
import com.coui.appcompat.material.navigation.NavigationBarItemView;
import com.coui.appcompat.material.navigation.NavigationBarMenuView;

/* JADX INFO: loaded from: classes4.dex */
@RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
public class NavigationRailMenuView extends NavigationBarMenuView {

    @Px
    public int F;
    public final FrameLayout.LayoutParams G;

    public NavigationRailMenuView(@NonNull Context context) {
        super(context);
        this.F = -1;
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-1, -2);
        this.G = layoutParams;
        layoutParams.gravity = 49;
        setLayoutParams(layoutParams);
        setItemActiveIndicatorResizeable(true);
    }

    @Override // com.coui.appcompat.material.navigation.NavigationBarMenuView
    @NonNull
    public NavigationBarItemView d(@NonNull Context context) {
        return new NavigationRailItemView(context);
    }

    public final int g(int i3, int i10, int i11) {
        int iMax = i10 / Math.max(1, i11);
        int size = this.F;
        if (size == -1) {
            size = View.MeasureSpec.getSize(i3);
        }
        return View.MeasureSpec.makeMeasureSpec(Math.min(size, iMax), 0);
    }

    @Px
    public int getItemMinimumHeight() {
        return this.F;
    }

    public int getMenuGravity() {
        return this.G.gravity;
    }

    public final int h(int i3, int i10, int i11, View view) {
        int measuredHeight;
        g(i3, i10, i11);
        int iG = view == null ? g(i3, i10, i11) : View.MeasureSpec.makeMeasureSpec(view.getMeasuredHeight(), 0);
        int childCount = getChildCount();
        int i12 = 0;
        for (int i13 = 0; i13 < childCount; i13++) {
            View childAt = getChildAt(i13);
            if (childAt != view) {
                if (childAt.getVisibility() != 8) {
                    childAt.measure(i3, iG);
                    measuredHeight = childAt.getMeasuredHeight();
                } else {
                    measuredHeight = 0;
                }
                i12 += measuredHeight;
            }
        }
        return i12;
    }

    @Override // android.view.ViewGroup, android.view.View
    public void onLayout(boolean z6, int i3, int i10, int i11, int i12) {
        int childCount = getChildCount();
        int i13 = i11 - i3;
        int i14 = 0;
        for (int i15 = 0; i15 < childCount; i15++) {
            View childAt = getChildAt(i15);
            if (childAt.getVisibility() != 8) {
                int measuredHeight = childAt.getMeasuredHeight() + i14;
                childAt.layout(0, i14, i13, measuredHeight);
                i14 = measuredHeight;
            }
        }
    }

    @Override // android.view.View
    public void onMeasure(int i3, int i10) {
        int iH;
        int measuredHeight;
        int size = View.MeasureSpec.getSize(i10);
        int size2 = getMenu().getVisibleItems().size();
        if (size2 <= 1 || !f(getLabelVisibilityMode(), size2)) {
            iH = h(i3, size, size2, null);
        } else {
            View childAt = getChildAt(getSelectedItemPosition());
            if (childAt != null) {
                int iG = g(i3, size, size2);
                if (childAt.getVisibility() != 8) {
                    childAt.measure(i3, iG);
                    measuredHeight = childAt.getMeasuredHeight();
                } else {
                    measuredHeight = 0;
                }
                size -= measuredHeight;
                size2--;
            } else {
                measuredHeight = 0;
            }
            iH = h(i3, size, size2, childAt) + measuredHeight;
        }
        setMeasuredDimension(View.resolveSizeAndState(View.MeasureSpec.getSize(i3), i3, 0), View.resolveSizeAndState(iH, i10, 0));
    }

    public void setItemMinimumHeight(@Px int i3) {
        if (this.F != i3) {
            this.F = i3;
            requestLayout();
        }
    }

    public void setMenuGravity(int i3) {
        FrameLayout.LayoutParams layoutParams = this.G;
        if (layoutParams.gravity != i3) {
            layoutParams.gravity = i3;
            setLayoutParams(layoutParams);
        }
    }
}
