package com.coui.appcompat.navigationrail;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.TintTypedArray;
import androidx.core.view.GravityCompat;
import com.coui.appcompat.contextutil.COUIContextUtil;
import com.coui.appcompat.material.navigation.NavigationBarMenuView;
import com.coui.appcompat.material.navigationrail.NavigationRailMenuView;
import com.coui.appcompat.material.navigationrail.NavigationRailView;
import com.google.android.material.R;

/* JADX INFO: loaded from: classes4.dex */
public class COUINavigationRailView extends NavigationRailView {

    /* JADX INFO: renamed from: j, reason: collision with root package name */
    public final View f8607j;

    /* JADX INFO: renamed from: k, reason: collision with root package name */
    public final COUINavigationRailMenuView f8608k;

    public COUINavigationRailView(@NonNull Context context) {
        this(context, null);
    }

    @Override // com.coui.appcompat.material.navigationrail.NavigationRailView, com.coui.appcompat.material.navigation.NavigationBarView
    @NonNull
    public final NavigationBarMenuView b(@NonNull Context context) {
        return new COUINavigationRailMenuView(context);
    }

    @Override // com.coui.appcompat.material.navigation.NavigationBarView
    public final void c(int i3) {
        if (getMenu().size() > 0) {
            getMenu().clear();
        }
        super.c(i3);
    }

    public COUINavigationRailMenuView getCOUINavigationMenuView() {
        return this.f8608k;
    }

    public View getDividerView() {
        return this.f8607j;
    }

    @Override // com.coui.appcompat.material.navigationrail.NavigationRailView, android.widget.FrameLayout, android.view.ViewGroup, android.view.View
    public final void onLayout(boolean z6, int i3, int i10, int i11, int i12) {
        super.onLayout(z6, i3, i10, i11, i12);
        int measuredHeight = getMeasuredHeight();
        COUINavigationRailMenuView cOUINavigationRailMenuView = this.f8608k;
        int measuredHeight2 = cOUINavigationRailMenuView.getMeasuredHeight();
        int i13 = (measuredHeight / 2) - (measuredHeight2 / 2);
        cOUINavigationRailMenuView.layout(0, i13, cOUINavigationRailMenuView.getMeasuredWidth(), measuredHeight2 + i13);
    }

    public COUINavigationRailView(@NonNull Context context, @Nullable AttributeSet attributeSet) {
        this(context, attributeSet, R.attr.navigationRailStyle);
    }

    /* JADX WARN: Illegal instructions before constructor call */
    public COUINavigationRailView(@NonNull Context context, @Nullable AttributeSet attributeSet, int i3) {
        super(context, attributeSet, i3, com.coui.appcompat.R.style.Widget_COUI_COUINavigationRailView);
        int i10 = com.coui.appcompat.R.style.Widget_COUI_COUINavigationRailView;
        TintTypedArray tintTypedArrayObtainStyledAttributes = TintTypedArray.obtainStyledAttributes(context, attributeSet, com.coui.appcompat.R.styleable.COUINavigationRailView, i3, i10);
        this.f8608k = (COUINavigationRailMenuView) getMenuView();
        if (tintTypedArrayObtainStyledAttributes.getInt(com.coui.appcompat.R.styleable.COUINavigationRailView_navigationRailType, 0) == 0) {
            setBackgroundResource(com.coui.appcompat.R.drawable.coui_bottom_tool_side_navigation_item_bg);
        }
        setElevation(0.0f);
        tintTypedArrayObtainStyledAttributes.recycle();
        View view = new View(context);
        this.f8607j = view;
        view.setForceDarkAllowed(false);
        this.f8607j.setBackgroundColor(COUIContextUtil.a(context, com.coui.appcompat.R.attr.couiColorDivider));
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_side_navigation_shadow_height), -1);
        layoutParams.gravity = GravityCompat.END;
        this.f8607j.setLayoutParams(layoutParams);
        addView(this.f8607j);
    }
}
