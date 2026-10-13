package com.coui.appcompat.material.bottomnavigation;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.core.view.ViewCompat;
import com.coui.appcompat.material.navigation.NavigationBarItemView;
import com.coui.appcompat.material.navigation.NavigationBarMenuView;
import com.google.android.material.R;

/* JADX INFO: loaded from: classes4.dex */
@RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
public class BottomNavigationMenuView extends NavigationBarMenuView {
    public final int F;
    public final int G;
    public final int H;
    public final int I;
    public boolean J;
    public final int[] K;

    public BottomNavigationMenuView(@NonNull Context context) {
        super(context);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-2, -2);
        layoutParams.gravity = 17;
        setLayoutParams(layoutParams);
        Resources resources = getResources();
        this.F = resources.getDimensionPixelSize(R.dimen.design_bottom_navigation_item_max_width);
        this.G = resources.getDimensionPixelSize(R.dimen.design_bottom_navigation_item_min_width);
        this.H = resources.getDimensionPixelSize(R.dimen.design_bottom_navigation_active_item_max_width);
        this.I = resources.getDimensionPixelSize(R.dimen.design_bottom_navigation_active_item_min_width);
        this.K = new int[5];
    }

    @Override // com.coui.appcompat.material.navigation.NavigationBarMenuView
    @NonNull
    public NavigationBarItemView d(@NonNull Context context) {
        return new BottomNavigationItemView(context);
    }

    @Override // android.view.ViewGroup, android.view.View
    public final void onLayout(boolean z6, int i3, int i10, int i11, int i12) {
        int childCount = getChildCount();
        int i13 = i11 - i3;
        int i14 = i12 - i10;
        int measuredWidth = 0;
        for (int i15 = 0; i15 < childCount; i15++) {
            View childAt = getChildAt(i15);
            if (childAt.getVisibility() != 8) {
                if (ViewCompat.getLayoutDirection(this) == 1) {
                    int i16 = i13 - measuredWidth;
                    childAt.layout(i16 - childAt.getMeasuredWidth(), 0, i16, i14);
                } else {
                    childAt.layout(measuredWidth, 0, childAt.getMeasuredWidth() + measuredWidth, i14);
                }
                measuredWidth += childAt.getMeasuredWidth();
            }
        }
    }

    @Override // android.view.View
    public void onMeasure(int i3, int i10) {
        MenuBuilder menu = getMenu();
        int size = View.MeasureSpec.getSize(i3);
        int size2 = menu.getVisibleItems().size();
        int childCount = getChildCount();
        int size3 = View.MeasureSpec.getSize(i10);
        int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(size3, 1073741824);
        boolean zF = f(getLabelVisibilityMode(), size2);
        int i11 = this.H;
        int[] iArr = this.K;
        if (zF && this.J) {
            View childAt = getChildAt(getSelectedItemPosition());
            int visibility = childAt.getVisibility();
            int iMax = this.I;
            if (visibility != 8) {
                childAt.measure(View.MeasureSpec.makeMeasureSpec(i11, Integer.MIN_VALUE), iMakeMeasureSpec);
                iMax = Math.max(iMax, childAt.getMeasuredWidth());
            }
            int i12 = size2 - (childAt.getVisibility() != 8 ? 1 : 0);
            int iMin = Math.min(size - (this.G * i12), Math.min(iMax, i11));
            int i13 = size - iMin;
            int iMin2 = Math.min(i13 / (i12 == 0 ? 1 : i12), this.F);
            int i14 = i13 - (i12 * iMin2);
            int i15 = 0;
            while (i15 < childCount) {
                if (getChildAt(i15).getVisibility() != 8) {
                    iArr[i15] = i15 == getSelectedItemPosition() ? iMin : iMin2;
                    if (i14 > 0) {
                        iArr[i15] = iArr[i15] + 1;
                        i14--;
                    }
                } else {
                    iArr[i15] = 0;
                }
                i15++;
            }
        } else {
            int iMin3 = Math.min(size / (size2 != 0 ? size2 : 1), i11);
            int i16 = size - (size2 * iMin3);
            for (int i17 = 0; i17 < childCount; i17++) {
                if (getChildAt(i17).getVisibility() != 8) {
                    iArr[i17] = iMin3;
                    if (i16 > 0) {
                        iArr[i17] = iMin3 + 1;
                        i16--;
                    }
                } else {
                    iArr[i17] = 0;
                }
            }
        }
        int measuredWidth = 0;
        for (int i18 = 0; i18 < childCount; i18++) {
            View childAt2 = getChildAt(i18);
            if (childAt2.getVisibility() != 8) {
                childAt2.measure(View.MeasureSpec.makeMeasureSpec(iArr[i18], 1073741824), iMakeMeasureSpec);
                childAt2.getLayoutParams().width = childAt2.getMeasuredWidth();
                measuredWidth += childAt2.getMeasuredWidth();
            }
        }
        setMeasuredDimension(View.resolveSizeAndState(measuredWidth, View.MeasureSpec.makeMeasureSpec(measuredWidth, 1073741824), 0), View.resolveSizeAndState(size3, i10, 0));
    }

    public void setItemHorizontalTranslationEnabled(boolean z6) {
        this.J = z6;
    }
}
