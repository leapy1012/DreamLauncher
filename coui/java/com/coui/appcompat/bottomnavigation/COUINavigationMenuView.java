package com.coui.appcompat.bottomnavigation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuItemImpl;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.coui.appcompat.material.bottomnavigation.BottomNavigationMenuView;
import com.coui.appcompat.material.navigation.NavigationBarItemView;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes4.dex */
@SuppressLint({"RestrictedApi"})
public class COUINavigationMenuView extends BottomNavigationMenuView {
    public int L;
    public int M;
    public COUINavigationItemView N;
    public int O;
    public int P;
    public final int[] Q;
    public int R;
    public int S;

    public COUINavigationMenuView(@NonNull Context context) {
        super(context);
        this.O = -1;
        this.L = getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_tool_navigation_edge_item_padding);
        setClipChildren(false);
        setClipToPadding(false);
        this.Q = new int[10];
    }

    @Override // com.coui.appcompat.material.bottomnavigation.BottomNavigationMenuView, com.coui.appcompat.material.navigation.NavigationBarMenuView
    @NonNull
    public final NavigationBarItemView d(@NonNull Context context) {
        COUINavigationItemView cOUINavigationItemView = new COUINavigationItemView(context);
        this.N = cOUINavigationItemView;
        cOUINavigationItemView.setLongClickable(isLongClickable());
        return this.N;
    }

    public final void g() {
        if (this.R == 1) {
            this.L = getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_tool_navigation_edge_item_default_padding);
            this.M = getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_tool_navigation_item_default_height);
        } else {
            this.L = getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_tool_navigation_edge_item_padding);
            this.M = getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_tool_navigation_item_height);
        }
    }

    public COUINavigationItemView getCOUINavigationItemView() {
        return this.N;
    }

    public int getEnlargeId() {
        int i3 = this.O;
        return i3 == -1 ? i3 : getMenu().getVisibleItems().get(this.O).getItemId();
    }

    public ArrayList<MenuItemImpl> getVisibleItems() {
        return getMenu().getVisibleItems();
    }

    @Override // android.view.View
    public final void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        g();
    }

    @Override // com.coui.appcompat.material.navigation.NavigationBarMenuView, android.view.View
    public final void onInitializeAccessibilityNodeInfo(@NonNull AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        if (this.S == 0) {
            AccessibilityNodeInfoCompat.wrap(accessibilityNodeInfo).setCollectionInfo(null);
        }
    }

    @Override // com.coui.appcompat.material.bottomnavigation.BottomNavigationMenuView, android.view.View
    public final void onMeasure(int i3, int i10) {
        int[] iArr;
        int size = View.MeasureSpec.getSize(i3) - (this.L * 2);
        int size2 = getMenu().getVisibleItems().size();
        int dimensionPixelSize = getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_tool_navigation_item_height);
        this.M = dimensionPixelSize;
        int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(dimensionPixelSize, 1073741824);
        int i11 = size / (size2 == 0 ? 1 : size2);
        int i12 = size - (i11 * size2);
        int i13 = 0;
        while (true) {
            iArr = this.Q;
            if (i13 >= size2) {
                break;
            }
            iArr[i13] = i11;
            if (i12 > 0) {
                iArr[i13] = i11 + 1;
                i12--;
            }
            i13++;
        }
        int measuredWidth = 0;
        int i14 = 0;
        for (int i15 = 0; i15 < getChildCount(); i15++) {
            View childAt = getChildAt(i15);
            if (childAt.getVisibility() != 8) {
                childAt.measure(View.MeasureSpec.makeMeasureSpec(iArr[i14], 1073741824), iMakeMeasureSpec);
                childAt.getLayoutParams().width = childAt.getMeasuredWidth();
                measuredWidth += childAt.getMeasuredWidth();
                i14++;
            }
        }
        setMeasuredDimension(View.resolveSizeAndState(measuredWidth, View.MeasureSpec.makeMeasureSpec(measuredWidth, 1073741824), 0), View.resolveSizeAndState(this.M, iMakeMeasureSpec, 0));
    }

    public void setItemLayoutType(int i3) {
        this.R = i3;
        g();
        for (int i10 = 0; i10 < getMenu().size(); i10++) {
            NavigationBarItemView navigationBarItemViewE = e(getMenu().getItem(i10).getItemId());
            if (navigationBarItemViewE instanceof COUINavigationItemView) {
                COUINavigationItemView cOUINavigationItemView = (COUINavigationItemView) navigationBarItemViewE;
                cOUINavigationItemView.S = this.R;
                cOUINavigationItemView.requestLayout();
            }
        }
    }

    public void setItemNavigationType(int i3) {
        if (this.S == i3) {
            return;
        }
        this.S = i3;
        if (getMenu() != null) {
            for (int i10 = 0; i10 < getMenu().size(); i10++) {
                NavigationBarItemView navigationBarItemViewE = e(getMenu().getItem(i10).getItemId());
                if (navigationBarItemViewE instanceof COUINavigationItemView) {
                    ((COUINavigationItemView) navigationBarItemViewE).setNavigationType(i3);
                }
            }
        }
    }

    public void setShowPressShadow(boolean z6) {
        for (int i3 = 0; i3 < getMenu().size(); i3++) {
            NavigationBarItemView navigationBarItemViewE = e(getMenu().getItem(i3).getItemId());
            if (navigationBarItemViewE instanceof COUINavigationItemView) {
                ((COUINavigationItemView) navigationBarItemViewE).setShowPressShadow(z6);
            }
        }
    }

    public void setTextSize(int i3) {
        this.P = i3;
        if (getMenu() != null) {
            for (int i10 = 0; i10 < getMenu().size(); i10++) {
                NavigationBarItemView navigationBarItemViewE = e(getMenu().getItem(i10).getItemId());
                if (navigationBarItemViewE instanceof COUINavigationItemView) {
                    ((COUINavigationItemView) navigationBarItemViewE).setTextSize(this.P);
                }
            }
        }
    }
}
