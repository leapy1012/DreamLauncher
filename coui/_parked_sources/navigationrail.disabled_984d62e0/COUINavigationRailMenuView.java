package com.coui.appcompat.navigationrail;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuItemImpl;
import com.coui.appcompat.material.navigation.NavigationBarItemView;
import com.coui.appcompat.material.navigationrail.NavigationRailMenuView;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes4.dex */
@SuppressLint({"RestrictedApi"})
public class COUINavigationRailMenuView extends NavigationRailMenuView {
    public final float H;

    /* JADX INFO: renamed from: com.coui.appcompat.navigationrail.COUINavigationRailMenuView$1, reason: invalid class name */
    public class AnonymousClass1 implements View.OnLongClickListener {
        @Override // android.view.View.OnLongClickListener
        public final boolean onLongClick(View view) {
            return true;
        }
    }

    public COUINavigationRailMenuView(@NonNull Context context) {
        super(context);
        this.H = getResources().getDimension(com.coui.appcompat.R.dimen.coui_navigation_rail_root_view_divider_height);
    }

    private int getAllItemHeight() {
        int childCount = getChildCount();
        int rootViewHeight = 0;
        for (int i3 = 0; i3 < childCount; i3++) {
            View childAt = getChildAt(i3);
            if (childAt instanceof COUINavigationRailItemView) {
                rootViewHeight = ((COUINavigationRailItemView) childAt).getRootViewHeight() + rootViewHeight;
            }
        }
        return rootViewHeight;
    }

    private int getDividerHeight() {
        ViewGroup viewGroup = (ViewGroup) getParent();
        if (viewGroup instanceof COUINavigationRailView) {
            return (int) Math.max(((viewGroup.getMeasuredHeight() * 0.4f) - getAllItemHeight()) / (getRealChildCount() + 1), this.H);
        }
        return 0;
    }

    private int getRealChildCount() {
        int childCount = getChildCount();
        int i3 = 0;
        for (int i10 = 0; i10 < childCount; i10++) {
            if (getChildAt(i10).getVisibility() != 8) {
                i3++;
            }
        }
        return i3;
    }

    @Override // android.view.ViewGroup
    public final void addView(View view) {
        super.addView(view);
        view.setOnLongClickListener(new AnonymousClass1());
    }

    @Override // com.coui.appcompat.material.navigationrail.NavigationRailMenuView, com.coui.appcompat.material.navigation.NavigationBarMenuView
    @NonNull
    public final NavigationBarItemView d(@NonNull Context context) {
        return new COUINavigationRailItemView(context);
    }

    public ArrayList<MenuItemImpl> getVisibleItems() {
        return getMenu().getVisibleItems();
    }

    @Override // com.coui.appcompat.material.navigationrail.NavigationRailMenuView, android.view.ViewGroup, android.view.View
    public final void onLayout(boolean z6, int i3, int i10, int i11, int i12) {
        int dividerHeight = getDividerHeight();
        int childCount = getChildCount();
        int i13 = i11 - i3;
        int i14 = 0;
        for (int i15 = 0; i15 < childCount; i15++) {
            View childAt = getChildAt(i15);
            if (childAt.getVisibility() != 8) {
                int i16 = i14 + dividerHeight;
                int measuredHeight = childAt.getMeasuredHeight() + i16;
                childAt.layout(0, i16, i13, measuredHeight);
                i14 = measuredHeight;
            }
        }
    }

    @Override // com.coui.appcompat.material.navigationrail.NavigationRailMenuView, android.view.View
    public final void onMeasure(int i3, int i10) {
        super.onMeasure(i3, i10);
        setMeasuredDimension(View.resolveSizeAndState(getMeasuredWidthAndState(), i3, 0), View.resolveSizeAndState(((getRealChildCount() + 1) * getDividerHeight()) + getMeasuredHeightAndState(), i10, 0));
    }
}
