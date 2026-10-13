package com.coui.appcompat.material.bottomnavigation;

import android.content.Context;
import androidx.annotation.DimenRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import com.coui.appcompat.material.navigation.NavigationBarItemView;
import com.google.android.material.R;

/* JADX INFO: loaded from: classes4.dex */
@RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
public class BottomNavigationItemView extends NavigationBarItemView {
    public BottomNavigationItemView(@NonNull Context context) {
        super(context);
    }

    @Override // com.coui.appcompat.material.navigation.NavigationBarItemView
    @DimenRes
    public int getItemDefaultMarginResId() {
        return R.dimen.design_bottom_navigation_margin;
    }

    @Override // com.coui.appcompat.material.navigation.NavigationBarItemView
    @LayoutRes
    public int getItemLayoutResId() {
        return R.layout.design_bottom_navigation_item;
    }
}
