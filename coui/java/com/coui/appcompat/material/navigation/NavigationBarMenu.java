package com.coui.appcompat.material.navigation;

import android.content.Context;
import android.view.MenuItem;
import android.view.SubMenu;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuItemImpl;

/* JADX INFO: loaded from: classes4.dex */
@RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
public final class NavigationBarMenu extends MenuBuilder {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    @NonNull
    public final Class<?> f8552a;

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    public final int f8553b;

    public NavigationBarMenu(@NonNull Context context, @NonNull Class<?> cls, int i3) {
        super(context);
        this.f8552a = cls;
        this.f8553b = i3;
    }

    @Override // androidx.appcompat.view.menu.MenuBuilder
    @NonNull
    public final MenuItem addInternal(int i3, int i10, int i11, @NonNull CharSequence charSequence) {
        int size = size() + 1;
        int i12 = this.f8553b;
        if (size > i12) {
            String simpleName = this.f8552a.getSimpleName();
            throw new IllegalArgumentException(
                    "Maximum number of items supported by " + simpleName + " is " + i12
                            + ". Limit can be checked with " + simpleName + "#getMaxItemCount()");
        }
        stopDispatchingItemsChanged();
        MenuItem menuItemAddInternal = super.addInternal(i3, i10, i11, charSequence);
        if (menuItemAddInternal instanceof MenuItemImpl) {
            ((MenuItemImpl) menuItemAddInternal).setExclusiveCheckable(true);
        }
        startDispatchingItemsChanged();
        return menuItemAddInternal;
    }

    @Override // androidx.appcompat.view.menu.MenuBuilder, android.view.Menu
    @NonNull
    public final SubMenu addSubMenu(int i3, int i10, int i11, @NonNull CharSequence charSequence) {
        throw new UnsupportedOperationException(this.f8552a.getSimpleName().concat(" does not support submenus"));
    }
}
