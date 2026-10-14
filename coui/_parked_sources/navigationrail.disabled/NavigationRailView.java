package com.coui.appcompat.material.navigationrail;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.RestrictTo;
import androidx.appcompat.widget.TintTypedArray;
import androidx.core.view.WindowInsetsCompat;
import com.coui.appcompat.material.navigation.NavigationBarView;
import com.google.android.material.R;
import com.google.android.material.internal.ThemeEnforcement;
import com.google.android.material.internal.ViewUtils;

/* JADX INFO: loaded from: classes4.dex */
public class NavigationRailView extends NavigationBarView {

    /* JADX INFO: renamed from: h, reason: collision with root package name */
    public final int f8603h;

    /* JADX INFO: renamed from: i, reason: collision with root package name */
    @Nullable
    public final View f8604i;

    /* JADX INFO: renamed from: com.coui.appcompat.material.navigationrail.NavigationRailView$1, reason: invalid class name */
    class AnonymousClass1 implements ViewUtils.OnApplyWindowInsetsListener {
        @Override // com.google.android.material.internal.ViewUtils.OnApplyWindowInsetsListener
        @NonNull
        public final WindowInsetsCompat onApplyWindowInsets(View view, @NonNull WindowInsetsCompat windowInsetsCompat, @NonNull ViewUtils.RelativePadding relativePadding) {
            throw null;
        }
    }

    /* JADX INFO: renamed from: com.coui.appcompat.material.navigationrail.NavigationRailView$2, reason: invalid class name */
    class AnonymousClass2 implements ViewUtils.OnApplyWindowInsetsListener {
        @Override // com.google.android.material.internal.ViewUtils.OnApplyWindowInsetsListener
        @NonNull
        public final WindowInsetsCompat onApplyWindowInsets(View view, @NonNull WindowInsetsCompat windowInsetsCompat, @NonNull ViewUtils.RelativePadding relativePadding) {
            throw null;
        }
    }

    public NavigationRailView(@NonNull Context context) {
        this(context, null);
    }

    private NavigationRailMenuView getNavigationRailMenuView() {
        return (NavigationRailMenuView) getMenuView();
    }

    @Override // com.coui.appcompat.material.navigation.NavigationBarView
    @NonNull
    @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
    /* JADX INFO: renamed from: d, reason: merged with bridge method [inline-methods] */
    public NavigationRailMenuView b(@NonNull Context context) {
        return new NavigationRailMenuView(context);
    }

    @Nullable
    public View getHeaderView() {
        return this.f8604i;
    }

    public int getItemMinimumHeight() {
        return ((NavigationRailMenuView) getMenuView()).getItemMinimumHeight();
    }

    @Override // com.coui.appcompat.material.navigation.NavigationBarView
    public int getMaxItemCount() {
        return 7;
    }

    public int getMenuGravity() {
        return getNavigationRailMenuView().getMenuGravity();
    }

    @Override // android.widget.FrameLayout, android.view.ViewGroup, android.view.View
    public void onLayout(boolean z6, int i3, int i10, int i11, int i12) {
        super.onLayout(z6, i3, i10, i11, i12);
        NavigationRailMenuView navigationRailMenuView = getNavigationRailMenuView();
        View view = this.f8604i;
        int i13 = 0;
        boolean z10 = (view == null || view.getVisibility() == 8) ? false : true;
        int i14 = this.f8603h;
        if (z10) {
            int bottom = this.f8604i.getBottom() + i14;
            int top = navigationRailMenuView.getTop();
            if (top < bottom) {
                i13 = bottom - top;
            }
        } else if ((navigationRailMenuView.G.gravity & 112) == 48) {
            i13 = i14;
        }
        if (i13 > 0) {
            navigationRailMenuView.layout(navigationRailMenuView.getLeft(), navigationRailMenuView.getTop() + i13, navigationRailMenuView.getRight(), navigationRailMenuView.getBottom() + i13);
        }
    }

    @Override // android.widget.FrameLayout, android.view.View
    public final void onMeasure(int i3, int i10) {
        int suggestedMinimumWidth = getSuggestedMinimumWidth();
        if (View.MeasureSpec.getMode(i3) != 1073741824 && suggestedMinimumWidth > 0) {
            i3 = View.MeasureSpec.makeMeasureSpec(Math.min(View.MeasureSpec.getSize(i3), getPaddingRight() + getPaddingLeft() + suggestedMinimumWidth), 1073741824);
        }
        super.onMeasure(i3, i10);
        View view = this.f8604i;
        if (view == null || view.getVisibility() == 8) {
            return;
        }
        measureChild(getNavigationRailMenuView(), i3, View.MeasureSpec.makeMeasureSpec((getMeasuredHeight() - this.f8604i.getMeasuredHeight()) - this.f8603h, Integer.MIN_VALUE));
    }

    public void setItemMinimumHeight(@Px int i3) {
        ((NavigationRailMenuView) getMenuView()).setItemMinimumHeight(i3);
    }

    public void setMenuGravity(int i3) {
        getNavigationRailMenuView().setMenuGravity(i3);
    }

    public NavigationRailView(@NonNull Context context, @Nullable AttributeSet attributeSet) {
        this(context, attributeSet, R.attr.navigationRailStyle);
    }

    public NavigationRailView(@NonNull Context context, @Nullable AttributeSet attributeSet, int i3) {
        this(context, attributeSet, i3, R.style.Widget_MaterialComponents_NavigationRailView);
    }

    public NavigationRailView(@NonNull Context context, @Nullable AttributeSet attributeSet, int i3, int i10) {
        super(context, attributeSet, i3, i10);
        int dimensionPixelSize = getResources().getDimensionPixelSize(R.dimen.mtrl_navigation_rail_margin);
        this.f8603h = dimensionPixelSize;
        TintTypedArray tintTypedArrayObtainTintedStyledAttributes = ThemeEnforcement.obtainTintedStyledAttributes(getContext(), attributeSet, R.styleable.NavigationRailView, i3, i10, new int[0]);
        int resourceId = tintTypedArrayObtainTintedStyledAttributes.getResourceId(R.styleable.NavigationRailView_headerLayout, 0);
        if (resourceId != 0) {
            View viewInflate = LayoutInflater.from(getContext()).inflate(resourceId, (ViewGroup) this, false);
            View view = this.f8604i;
            if (view != null) {
                removeView(view);
                this.f8604i = null;
            }
            this.f8604i = viewInflate;
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-2, -2);
            layoutParams.gravity = 49;
            layoutParams.topMargin = dimensionPixelSize;
            addView(viewInflate, 0, layoutParams);
        }
        setMenuGravity(tintTypedArrayObtainTintedStyledAttributes.getInt(R.styleable.NavigationRailView_menuGravity, 49));
        if (tintTypedArrayObtainTintedStyledAttributes.hasValue(R.styleable.NavigationRailView_itemMinHeight)) {
            setItemMinimumHeight(tintTypedArrayObtainTintedStyledAttributes.getDimensionPixelSize(R.styleable.NavigationRailView_itemMinHeight, -1));
        }
        if (tintTypedArrayObtainTintedStyledAttributes.hasValue(R.styleable.NavigationRailView_paddingTopSystemWindowInsets)) {
            tintTypedArrayObtainTintedStyledAttributes.getBoolean(R.styleable.NavigationRailView_paddingTopSystemWindowInsets, false);
        }
        if (tintTypedArrayObtainTintedStyledAttributes.hasValue(R.styleable.NavigationRailView_paddingBottomSystemWindowInsets)) {
            tintTypedArrayObtainTintedStyledAttributes.getBoolean(R.styleable.NavigationRailView_paddingBottomSystemWindowInsets, false);
        }
        tintTypedArrayObtainTintedStyledAttributes.recycle();
    }
}
