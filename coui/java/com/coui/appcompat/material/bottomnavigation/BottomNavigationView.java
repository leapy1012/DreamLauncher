package com.coui.appcompat.material.bottomnavigation;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.widget.TintTypedArray;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.coui.appcompat.material.navigation.NavigationBarMenuView;
import com.coui.appcompat.material.navigation.NavigationBarView;
import com.google.android.material.R;
import com.google.android.material.internal.ThemeEnforcement;
import com.google.android.material.internal.ViewUtils;

/* JADX INFO: loaded from: classes4.dex */
public class BottomNavigationView extends NavigationBarView {

    /* JADX INFO: renamed from: com.coui.appcompat.material.bottomnavigation.BottomNavigationView$1, reason: invalid class name */
    class AnonymousClass1 implements ViewUtils.OnApplyWindowInsetsListener {
        @Override // com.google.android.material.internal.ViewUtils.OnApplyWindowInsetsListener
        @NonNull
        public final WindowInsetsCompat onApplyWindowInsets(View view, @NonNull WindowInsetsCompat windowInsetsCompat, @NonNull ViewUtils.RelativePadding relativePadding) {
            relativePadding.bottom = windowInsetsCompat.getSystemWindowInsetBottom() + relativePadding.bottom;
            boolean z6 = ViewCompat.getLayoutDirection(view) == 1;
            int systemWindowInsetLeft = windowInsetsCompat.getSystemWindowInsetLeft();
            int systemWindowInsetRight = windowInsetsCompat.getSystemWindowInsetRight();
            relativePadding.start += z6 ? systemWindowInsetRight : systemWindowInsetLeft;
            int i3 = relativePadding.end;
            if (!z6) {
                systemWindowInsetLeft = systemWindowInsetRight;
            }
            relativePadding.end = i3 + systemWindowInsetLeft;
            relativePadding.applyToView(view);
            return windowInsetsCompat;
        }
    }

    /* JADX INFO: renamed from: com.coui.appcompat.material.bottomnavigation.BottomNavigationView$2, reason: invalid class name */
    class AnonymousClass2 implements ViewUtils.OnApplyWindowInsetsListener {
        @Override // com.google.android.material.internal.ViewUtils.OnApplyWindowInsetsListener
        @NonNull
        public final WindowInsetsCompat onApplyWindowInsets(View view, @NonNull WindowInsetsCompat windowInsetsCompat, @NonNull ViewUtils.RelativePadding relativePadding) {
            relativePadding.bottom = windowInsetsCompat.getSystemWindowInsetBottom() + relativePadding.bottom;
            boolean z6 = ViewCompat.getLayoutDirection(view) == 1;
            int systemWindowInsetLeft = windowInsetsCompat.getSystemWindowInsetLeft();
            int systemWindowInsetRight = windowInsetsCompat.getSystemWindowInsetRight();
            relativePadding.start += z6 ? systemWindowInsetRight : systemWindowInsetLeft;
            int i3 = relativePadding.end;
            if (!z6) {
                systemWindowInsetLeft = systemWindowInsetRight;
            }
            relativePadding.end = i3 + systemWindowInsetLeft;
            relativePadding.applyToView(view);
            return windowInsetsCompat;
        }
    }

    @Deprecated
    public interface OnNavigationItemReselectedListener extends NavigationBarView.OnItemReselectedListener {
    }

    @Deprecated
    public interface OnNavigationItemSelectedListener extends NavigationBarView.OnItemSelectedListener {
    }

    public BottomNavigationView(@NonNull Context context) {
        this(context, null);
    }

    @Override // com.coui.appcompat.material.navigation.NavigationBarView
    @NonNull
    @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
    public NavigationBarMenuView b(@NonNull Context context) {
        return new BottomNavigationMenuView(context);
    }

    @Override // com.coui.appcompat.material.navigation.NavigationBarView
    public int getMaxItemCount() {
        return 5;
    }

    @Override // android.widget.FrameLayout, android.view.View
    public void onMeasure(int i3, int i10) {
        int suggestedMinimumHeight = getSuggestedMinimumHeight();
        if (View.MeasureSpec.getMode(i10) != 1073741824 && suggestedMinimumHeight > 0) {
            i10 = View.MeasureSpec.makeMeasureSpec(Math.min(View.MeasureSpec.getSize(i10), getPaddingBottom() + getPaddingTop() + suggestedMinimumHeight), 1073741824);
        }
        super.onMeasure(i3, i10);
    }

    public void setItemHorizontalTranslationEnabled(boolean z6) {
        BottomNavigationMenuView bottomNavigationMenuView = (BottomNavigationMenuView) getMenuView();
        if (bottomNavigationMenuView.J != z6) {
            bottomNavigationMenuView.setItemHorizontalTranslationEnabled(z6);
            getPresenter().updateMenuView(false);
        }
    }

    @Deprecated
    public void setOnNavigationItemReselectedListener(@Nullable OnNavigationItemReselectedListener onNavigationItemReselectedListener) {
        setOnItemReselectedListener(onNavigationItemReselectedListener);
    }

    @Deprecated
    public void setOnNavigationItemSelectedListener(@Nullable OnNavigationItemSelectedListener onNavigationItemSelectedListener) {
        setOnItemSelectedListener(onNavigationItemSelectedListener);
    }

    public BottomNavigationView(@NonNull Context context, @Nullable AttributeSet attributeSet) {
        this(context, attributeSet, R.attr.bottomNavigationStyle);
    }

    public BottomNavigationView(@NonNull Context context, @Nullable AttributeSet attributeSet, int i3) {
        this(context, attributeSet, i3, R.style.Widget_Design_BottomNavigationView);
    }

    public BottomNavigationView(@NonNull Context context, @Nullable AttributeSet attributeSet, int i3, int i10) {
        super(context, attributeSet, i3, i10);
        TintTypedArray tintTypedArrayObtainTintedStyledAttributes = ThemeEnforcement.obtainTintedStyledAttributes(getContext(), attributeSet, R.styleable.BottomNavigationView, i3, i10, new int[0]);
        setItemHorizontalTranslationEnabled(tintTypedArrayObtainTintedStyledAttributes.getBoolean(R.styleable.BottomNavigationView_itemHorizontalTranslationEnabled, true));
        if (tintTypedArrayObtainTintedStyledAttributes.hasValue(R.styleable.BottomNavigationView_android_minHeight)) {
            setMinimumHeight(tintTypedArrayObtainTintedStyledAttributes.getDimensionPixelSize(R.styleable.BottomNavigationView_android_minHeight, 0));
        }
        tintTypedArrayObtainTintedStyledAttributes.recycle();
    }
}
