package com.coui.appcompat.material.navigation;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuItemImpl;
import androidx.appcompat.view.menu.MenuPresenter;
import androidx.appcompat.view.menu.MenuView;
import androidx.appcompat.view.menu.SubMenuBuilder;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.internal.ParcelableSparseArray;

/* JADX INFO: loaded from: classes4.dex */
@RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
public class NavigationBarPresenter implements MenuPresenter {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    public NavigationBarMenuView f8575a;

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    public boolean f8576b = false;
    public int c;

    public static class SavedState implements Parcelable {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() { // from class: com.coui.appcompat.material.navigation.NavigationBarPresenter.SavedState.1
            @Override // android.os.Parcelable.Creator
            @NonNull
            public final SavedState createFromParcel(@NonNull Parcel parcel) {
                SavedState savedState = new SavedState();
                savedState.f8577a = parcel.readInt();
                savedState.f8578b = (ParcelableSparseArray) parcel.readParcelable(SavedState.class.getClassLoader());
                return savedState;
            }

            @Override // android.os.Parcelable.Creator
            @NonNull
            public final SavedState[] newArray(int i3) {
                return new SavedState[i3];
            }
        };

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        public int f8577a;

        /* JADX INFO: renamed from: b, reason: collision with root package name */
        @Nullable
        public ParcelableSparseArray f8578b;

        @Override // android.os.Parcelable
        public final int describeContents() {
            return 0;
        }

        @Override // android.os.Parcelable
        public final void writeToParcel(@NonNull Parcel parcel, int i3) {
            parcel.writeInt(this.f8577a);
            parcel.writeParcelable(this.f8578b, 0);
        }
    }

    @Override // androidx.appcompat.view.menu.MenuPresenter
    public final boolean collapseItemActionView(@Nullable MenuBuilder menuBuilder, @Nullable MenuItemImpl menuItemImpl) {
        return false;
    }

    @Override // androidx.appcompat.view.menu.MenuPresenter
    public final boolean expandItemActionView(@Nullable MenuBuilder menuBuilder, @Nullable MenuItemImpl menuItemImpl) {
        return false;
    }

    @Override // androidx.appcompat.view.menu.MenuPresenter
    public final boolean flagActionItems() {
        return false;
    }

    @Override // androidx.appcompat.view.menu.MenuPresenter
    public final int getId() {
        return this.c;
    }

    @Override // androidx.appcompat.view.menu.MenuPresenter
    @Nullable
    public final MenuView getMenuView(@Nullable ViewGroup viewGroup) {
        return this.f8575a;
    }

    @Override // androidx.appcompat.view.menu.MenuPresenter
    public final void initForMenu(@NonNull Context context, @NonNull MenuBuilder menuBuilder) {
        this.f8575a.C = menuBuilder;
    }

    @Override // androidx.appcompat.view.menu.MenuPresenter
    public final void onCloseMenu(@Nullable MenuBuilder menuBuilder, boolean z6) {
    }

    @Override // androidx.appcompat.view.menu.MenuPresenter
    public final void onRestoreInstanceState(@NonNull Parcelable parcelable) {
        SparseArray<BadgeDrawable> sparseArray;
        if (parcelable instanceof SavedState) {
            NavigationBarMenuView navigationBarMenuView = this.f8575a;
            SavedState savedState = (SavedState) parcelable;
            int i3 = savedState.f8577a;
            int size = navigationBarMenuView.C.size();
            for (int i10 = 0; i10 < size; i10++) {
                MenuItem item = navigationBarMenuView.C.getItem(i10);
                if (i3 == item.getItemId()) {
                    navigationBarMenuView.f8558g = i3;
                    navigationBarMenuView.f8559h = i10;
                    item.setChecked(true);
                    break;
                }
            }
            SparseArray<BadgeDrawable> sparseArrayCreateBadgeDrawablesFromSavedStates = BadgeUtils.createBadgeDrawablesFromSavedStates(this.f8575a.getContext(), savedState.f8578b);
            NavigationBarMenuView navigationBarMenuView2 = this.f8575a;
            navigationBarMenuView2.getClass();
            int i11 = 0;
            while (true) {
                int size2 = sparseArrayCreateBadgeDrawablesFromSavedStates.size();
                sparseArray = navigationBarMenuView2.f8568q;
                if (i11 >= size2) {
                    break;
                }
                int iKeyAt = sparseArrayCreateBadgeDrawablesFromSavedStates.keyAt(i11);
                if (sparseArray.indexOfKey(iKeyAt) < 0) {
                    sparseArray.append(iKeyAt, sparseArrayCreateBadgeDrawablesFromSavedStates.get(iKeyAt));
                }
                i11++;
            }
            NavigationBarItemView[] navigationBarItemViewArr = navigationBarMenuView2.f8557f;
            if (navigationBarItemViewArr != null) {
                for (NavigationBarItemView navigationBarItemView : navigationBarItemViewArr) {
                    navigationBarItemView.setBadge(sparseArray.get(navigationBarItemView.getId()));
                }
            }
        }
    }

    @Override // androidx.appcompat.view.menu.MenuPresenter
    @NonNull
    public final Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState();
        savedState.f8577a = this.f8575a.getSelectedItemId();
        savedState.f8578b = BadgeUtils.createParcelableBadgeStates(this.f8575a.getBadgeDrawables());
        return savedState;
    }

    @Override // androidx.appcompat.view.menu.MenuPresenter
    public final boolean onSubMenuSelected(@Nullable SubMenuBuilder subMenuBuilder) {
        return false;
    }

    @Override // androidx.appcompat.view.menu.MenuPresenter
    public final void setCallback(@Nullable MenuPresenter.Callback callback) {
    }

    @Override // androidx.appcompat.view.menu.MenuPresenter
    public final void updateMenuView(boolean z6) {
        AutoTransition autoTransition;
        if (this.f8576b) {
            return;
        }
        if (z6) {
            this.f8575a.a();
            return;
        }
        NavigationBarMenuView navigationBarMenuView = this.f8575a;
        MenuBuilder menuBuilder = navigationBarMenuView.C;
        if (menuBuilder == null || navigationBarMenuView.f8557f == null) {
            return;
        }
        int size = menuBuilder.size();
        if (size != navigationBarMenuView.f8557f.length) {
            navigationBarMenuView.a();
            return;
        }
        int i3 = navigationBarMenuView.f8558g;
        for (int i10 = 0; i10 < size; i10++) {
            MenuItem item = navigationBarMenuView.C.getItem(i10);
            if (item.isChecked()) {
                navigationBarMenuView.f8558g = item.getItemId();
                navigationBarMenuView.f8559h = i10;
            }
        }
        if (i3 != navigationBarMenuView.f8558g && (autoTransition = navigationBarMenuView.f8554a) != null) {
            TransitionManager.beginDelayedTransition(navigationBarMenuView, autoTransition);
        }
        boolean zF = navigationBarMenuView.f(navigationBarMenuView.f8556e, navigationBarMenuView.C.getVisibleItems().size());
        for (int i11 = 0; i11 < size; i11++) {
            navigationBarMenuView.B.f8576b = true;
            navigationBarMenuView.f8557f[i11].setLabelVisibilityMode(navigationBarMenuView.f8556e);
            navigationBarMenuView.f8557f[i11].setShifting(zF);
            navigationBarMenuView.f8557f[i11].initialize((MenuItemImpl) navigationBarMenuView.C.getItem(i11), 0);
            navigationBarMenuView.B.f8576b = false;
        }
    }
}
