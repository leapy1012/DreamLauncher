package com.coui.appcompat.material.navigation;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.FrameLayout;
import androidx.annotation.AttrRes;
import androidx.annotation.DimenRes;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.RestrictTo;
import androidx.annotation.StyleRes;
import androidx.appcompat.view.SupportMenuInflater;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuView;
import androidx.appcompat.widget.TintTypedArray;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.customview.view.AbsSavedState;
import com.google.android.material.R;
import com.google.android.material.internal.ThemeEnforcement;
import com.google.android.material.resources.MaterialResources;
import com.google.android.material.ripple.RippleUtils;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.MaterialShapeUtils;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.theme.overlay.MaterialThemeOverlay;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/* JADX INFO: loaded from: classes4.dex */
public abstract class NavigationBarView extends FrameLayout {

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    @NonNull
    public final NavigationBarMenu f8579a;

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    @NonNull
    public final NavigationBarMenuView f8580b;

    @NonNull
    public final NavigationBarPresenter c;

    @Nullable
    public ColorStateList d;

    /* JADX INFO: renamed from: e, reason: collision with root package name */
    public SupportMenuInflater f8581e;

    /* JADX INFO: renamed from: f, reason: collision with root package name */
    public OnItemSelectedListener f8582f;

    /* JADX INFO: renamed from: g, reason: collision with root package name */
    public OnItemReselectedListener f8583g;

    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
    public @interface LabelVisibility {
    }

    public interface OnItemReselectedListener {
        void a();
    }

    public interface OnItemSelectedListener {
        void onNavigationItemSelected(@NonNull MenuItem menuItem);
    }

    public static class SavedState extends AbsSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.ClassLoaderCreator<SavedState>() { // from class: com.coui.appcompat.material.navigation.NavigationBarView.SavedState.1
            @Override // android.os.Parcelable.ClassLoaderCreator
            @NonNull
            public final SavedState createFromParcel(@NonNull Parcel parcel, ClassLoader classLoader) {
                return new SavedState(parcel, classLoader);
            }

            @Override // android.os.Parcelable.Creator
            @NonNull
            public final SavedState[] newArray(int i3) {
                return new SavedState[i3];
            }

            @Override // android.os.Parcelable.Creator
            @Nullable
            public final SavedState createFromParcel(@NonNull Parcel parcel) {
                return new SavedState(parcel, null);
            }
        };

        /* JADX INFO: renamed from: a, reason: collision with root package name */
        @Nullable
        public Bundle f8585a;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(@NonNull Parcel parcel, ClassLoader classLoader) {
            super(parcel, classLoader);
            this.f8585a = parcel.readBundle(classLoader == null ? getClass().getClassLoader() : classLoader);
        }

        @Override // androidx.customview.view.AbsSavedState, android.os.Parcelable
        public final void writeToParcel(@NonNull Parcel parcel, int i3) {
            super.writeToParcel(parcel, i3);
            parcel.writeBundle(this.f8585a);
        }
    }

    public NavigationBarView(@NonNull Context context, @Nullable AttributeSet attributeSet, @AttrRes int i3, @StyleRes int i10) {
        super(MaterialThemeOverlay.wrap(context, attributeSet, i3, i10), attributeSet, i3);
        NavigationBarPresenter navigationBarPresenter = new NavigationBarPresenter();
        this.c = navigationBarPresenter;
        Context context2 = getContext();
        TintTypedArray tintTypedArrayObtainTintedStyledAttributes = ThemeEnforcement.obtainTintedStyledAttributes(context2, attributeSet, R.styleable.NavigationBarView, i3, i10, R.styleable.NavigationBarView_itemTextAppearanceInactive, R.styleable.NavigationBarView_itemTextAppearanceActive);
        NavigationBarMenu navigationBarMenu = new NavigationBarMenu(context2, getClass(), getMaxItemCount());
        this.f8579a = navigationBarMenu;
        NavigationBarMenuView navigationBarMenuViewB = b(context2);
        this.f8580b = navigationBarMenuViewB;
        navigationBarPresenter.f8575a = navigationBarMenuViewB;
        navigationBarPresenter.c = 1;
        navigationBarMenuViewB.setPresenter(navigationBarPresenter);
        navigationBarMenu.addMenuPresenter(navigationBarPresenter);
        getContext();
        navigationBarPresenter.f8575a.C = navigationBarMenu;
        if (tintTypedArrayObtainTintedStyledAttributes.hasValue(R.styleable.NavigationBarView_itemIconTint)) {
            navigationBarMenuViewB.setIconTintList(tintTypedArrayObtainTintedStyledAttributes.getColorStateList(R.styleable.NavigationBarView_itemIconTint));
        } else {
            navigationBarMenuViewB.setIconTintList(navigationBarMenuViewB.b(android.R.attr.textColorSecondary));
        }
        setItemIconSize(tintTypedArrayObtainTintedStyledAttributes.getDimensionPixelSize(R.styleable.NavigationBarView_itemIconSize, getResources().getDimensionPixelSize(R.dimen.mtrl_navigation_bar_item_default_icon_size)));
        if (tintTypedArrayObtainTintedStyledAttributes.hasValue(R.styleable.NavigationBarView_itemTextAppearanceInactive)) {
            setItemTextAppearanceInactive(tintTypedArrayObtainTintedStyledAttributes.getResourceId(R.styleable.NavigationBarView_itemTextAppearanceInactive, 0));
        }
        if (tintTypedArrayObtainTintedStyledAttributes.hasValue(R.styleable.NavigationBarView_itemTextAppearanceActive)) {
            setItemTextAppearanceActive(tintTypedArrayObtainTintedStyledAttributes.getResourceId(R.styleable.NavigationBarView_itemTextAppearanceActive, 0));
        }
        if (tintTypedArrayObtainTintedStyledAttributes.hasValue(R.styleable.NavigationBarView_itemTextColor)) {
            setItemTextColor(tintTypedArrayObtainTintedStyledAttributes.getColorStateList(R.styleable.NavigationBarView_itemTextColor));
        }
        if (getBackground() == null || (getBackground() instanceof ColorDrawable)) {
            MaterialShapeDrawable materialShapeDrawable = new MaterialShapeDrawable();
            Drawable background = getBackground();
            if (background instanceof ColorDrawable) {
                materialShapeDrawable.setFillColor(ColorStateList.valueOf(((ColorDrawable) background).getColor()));
            }
            materialShapeDrawable.initializeElevationOverlay(context2);
            ViewCompat.setBackground(this, materialShapeDrawable);
        }
        if (tintTypedArrayObtainTintedStyledAttributes.hasValue(R.styleable.NavigationBarView_itemPaddingTop)) {
            setItemPaddingTop(tintTypedArrayObtainTintedStyledAttributes.getDimensionPixelSize(R.styleable.NavigationBarView_itemPaddingTop, 0));
        }
        if (tintTypedArrayObtainTintedStyledAttributes.hasValue(R.styleable.NavigationBarView_itemPaddingBottom)) {
            setItemPaddingBottom(tintTypedArrayObtainTintedStyledAttributes.getDimensionPixelSize(R.styleable.NavigationBarView_itemPaddingBottom, 0));
        }
        if (tintTypedArrayObtainTintedStyledAttributes.hasValue(R.styleable.NavigationBarView_elevation)) {
            setElevation(tintTypedArrayObtainTintedStyledAttributes.getDimensionPixelSize(R.styleable.NavigationBarView_elevation, 0));
        }
        DrawableCompat.setTintList(getBackground().mutate(), MaterialResources.getColorStateList(context2, tintTypedArrayObtainTintedStyledAttributes, R.styleable.NavigationBarView_backgroundTint));
        setLabelVisibilityMode(tintTypedArrayObtainTintedStyledAttributes.getInteger(R.styleable.NavigationBarView_labelVisibilityMode, -1));
        int resourceId = tintTypedArrayObtainTintedStyledAttributes.getResourceId(R.styleable.NavigationBarView_itemBackground, 0);
        if (resourceId != 0) {
            navigationBarMenuViewB.setItemBackgroundRes(resourceId);
        } else {
            setItemRippleColor(MaterialResources.getColorStateList(context2, tintTypedArrayObtainTintedStyledAttributes, R.styleable.NavigationBarView_itemRippleColor));
        }
        int resourceId2 = tintTypedArrayObtainTintedStyledAttributes.getResourceId(R.styleable.NavigationBarView_itemActiveIndicatorStyle, 0);
        if (resourceId2 != 0) {
            setItemActiveIndicatorEnabled(true);
            TypedArray typedArrayObtainStyledAttributes = context2.obtainStyledAttributes(resourceId2, R.styleable.NavigationBarActiveIndicator);
            setItemActiveIndicatorWidth(typedArrayObtainStyledAttributes.getDimensionPixelSize(R.styleable.NavigationBarActiveIndicator_android_width, 0));
            setItemActiveIndicatorHeight(typedArrayObtainStyledAttributes.getDimensionPixelSize(R.styleable.NavigationBarActiveIndicator_android_height, 0));
            setItemActiveIndicatorMarginHorizontal(typedArrayObtainStyledAttributes.getDimensionPixelOffset(R.styleable.NavigationBarActiveIndicator_marginHorizontal, 0));
            setItemActiveIndicatorColor(MaterialResources.getColorStateList(context2, typedArrayObtainStyledAttributes, R.styleable.NavigationBarActiveIndicator_android_color));
            setItemActiveIndicatorShapeAppearance(ShapeAppearanceModel.builder(context2, typedArrayObtainStyledAttributes.getResourceId(R.styleable.NavigationBarActiveIndicator_shapeAppearance, 0), 0).build());
            typedArrayObtainStyledAttributes.recycle();
        }
        if (tintTypedArrayObtainTintedStyledAttributes.hasValue(R.styleable.NavigationBarView_menu)) {
            c(tintTypedArrayObtainTintedStyledAttributes.getResourceId(R.styleable.NavigationBarView_menu, 0));
        }
        tintTypedArrayObtainTintedStyledAttributes.recycle();
        addView(navigationBarMenuViewB);
        navigationBarMenu.setCallback(new MenuBuilder.Callback() { // from class: com.coui.appcompat.material.navigation.NavigationBarView.1
            @Override // androidx.appcompat.view.menu.MenuBuilder.Callback
            public final boolean onMenuItemSelected(MenuBuilder menuBuilder, @NonNull MenuItem menuItem) {
                NavigationBarView navigationBarView = NavigationBarView.this;
                if (navigationBarView.f8583g != null && menuItem.getItemId() == navigationBarView.getSelectedItemId()) {
                    navigationBarView.f8583g.a();
                    return true;
                }
                OnItemSelectedListener onItemSelectedListener = navigationBarView.f8582f;
                if (onItemSelectedListener == null) {
                    return false;
                }
                onItemSelectedListener.onNavigationItemSelected(menuItem);
                return false;
            }

            @Override // androidx.appcompat.view.menu.MenuBuilder.Callback
            public final void onMenuModeChange(MenuBuilder menuBuilder) {
            }
        });
    }

    private MenuInflater getMenuInflater() {
        if (this.f8581e == null) {
            this.f8581e = new SupportMenuInflater(getContext());
        }
        return this.f8581e;
    }

    @NonNull
    @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
    public abstract NavigationBarMenuView b(@NonNull Context context);

    public void c(int i3) {
        NavigationBarPresenter navigationBarPresenter = this.c;
        navigationBarPresenter.f8576b = true;
        getMenuInflater().inflate(i3, this.f8579a);
        navigationBarPresenter.f8576b = false;
        navigationBarPresenter.updateMenuView(true);
    }

    @Nullable
    public ColorStateList getItemActiveIndicatorColor() {
        return this.f8580b.getItemActiveIndicatorColor();
    }

    @Px
    public int getItemActiveIndicatorHeight() {
        return this.f8580b.getItemActiveIndicatorHeight();
    }

    @Px
    public int getItemActiveIndicatorMarginHorizontal() {
        return this.f8580b.getItemActiveIndicatorMarginHorizontal();
    }

    @Nullable
    public ShapeAppearanceModel getItemActiveIndicatorShapeAppearance() {
        return this.f8580b.getItemActiveIndicatorShapeAppearance();
    }

    @Px
    public int getItemActiveIndicatorWidth() {
        return this.f8580b.getItemActiveIndicatorWidth();
    }

    @Nullable
    public Drawable getItemBackground() {
        return this.f8580b.getItemBackground();
    }

    @DrawableRes
    @Deprecated
    public int getItemBackgroundResource() {
        return this.f8580b.getItemBackgroundRes();
    }

    @Dimension
    public int getItemIconSize() {
        return this.f8580b.getItemIconSize();
    }

    @Nullable
    public ColorStateList getItemIconTintList() {
        return this.f8580b.getIconTintList();
    }

    @Px
    public int getItemPaddingBottom() {
        return this.f8580b.getItemPaddingBottom();
    }

    @Px
    public int getItemPaddingTop() {
        return this.f8580b.getItemPaddingTop();
    }

    @Nullable
    public ColorStateList getItemRippleColor() {
        return this.d;
    }

    @StyleRes
    public int getItemTextAppearanceActive() {
        return this.f8580b.getItemTextAppearanceActive();
    }

    @StyleRes
    public int getItemTextAppearanceInactive() {
        return this.f8580b.getItemTextAppearanceInactive();
    }

    @Nullable
    public ColorStateList getItemTextColor() {
        return this.f8580b.getItemTextColor();
    }

    public int getLabelVisibilityMode() {
        return this.f8580b.getLabelVisibilityMode();
    }

    public abstract int getMaxItemCount();

    @NonNull
    public Menu getMenu() {
        return this.f8579a;
    }

    @NonNull
    @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
    public MenuView getMenuView() {
        return this.f8580b;
    }

    @NonNull
    @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
    public NavigationBarPresenter getPresenter() {
        return this.c;
    }

    @IdRes
    public int getSelectedItemId() {
        return this.f8580b.getSelectedItemId();
    }

    @Override // android.view.ViewGroup, android.view.View
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        MaterialShapeUtils.setParentAbsoluteElevation(this);
    }

    @Override // android.view.View
    public final void onRestoreInstanceState(@Nullable Parcelable parcelable) {
        if (!(parcelable instanceof SavedState)) {
            super.onRestoreInstanceState(parcelable);
            return;
        }
        SavedState savedState = (SavedState) parcelable;
        super.onRestoreInstanceState(savedState.getSuperState());
        this.f8579a.restorePresenterStates(savedState.f8585a);
    }

    @Override // android.view.View
    @NonNull
    public final Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        Bundle bundle = new Bundle();
        savedState.f8585a = bundle;
        this.f8579a.savePresenterStates(bundle);
        return savedState;
    }

    @Override // android.view.View
    public void setElevation(float f3) {
        super.setElevation(f3);
        MaterialShapeUtils.setElevation(this, f3);
    }

    public void setItemActiveIndicatorColor(@Nullable ColorStateList colorStateList) {
        this.f8580b.setItemActiveIndicatorColor(colorStateList);
    }

    public void setItemActiveIndicatorEnabled(boolean z6) {
        this.f8580b.setItemActiveIndicatorEnabled(z6);
    }

    public void setItemActiveIndicatorHeight(@Px int i3) {
        this.f8580b.setItemActiveIndicatorHeight(i3);
    }

    public void setItemActiveIndicatorMarginHorizontal(@Px int i3) {
        this.f8580b.setItemActiveIndicatorMarginHorizontal(i3);
    }

    public void setItemActiveIndicatorShapeAppearance(@Nullable ShapeAppearanceModel shapeAppearanceModel) {
        this.f8580b.setItemActiveIndicatorShapeAppearance(shapeAppearanceModel);
    }

    public void setItemActiveIndicatorWidth(@Px int i3) {
        this.f8580b.setItemActiveIndicatorWidth(i3);
    }

    public void setItemBackground(@Nullable Drawable drawable) {
        this.f8580b.setItemBackground(drawable);
        this.d = null;
    }

    public void setItemBackgroundResource(@DrawableRes int i3) {
        this.f8580b.setItemBackgroundRes(i3);
        this.d = null;
    }

    public void setItemIconSize(@Dimension int i3) {
        this.f8580b.setItemIconSize(i3);
    }

    public void setItemIconSizeRes(@DimenRes int i3) {
        setItemIconSize(getResources().getDimensionPixelSize(i3));
    }

    public void setItemIconTintList(@Nullable ColorStateList colorStateList) {
        this.f8580b.setIconTintList(colorStateList);
    }

    public void setItemPaddingBottom(@Px int i3) {
        this.f8580b.setItemPaddingBottom(i3);
    }

    public void setItemPaddingTop(@Px int i3) {
        this.f8580b.setItemPaddingTop(i3);
    }

    public void setItemRippleColor(@Nullable ColorStateList colorStateList) {
        ColorStateList colorStateList2 = this.d;
        NavigationBarMenuView navigationBarMenuView = this.f8580b;
        if (colorStateList2 == colorStateList) {
            if (colorStateList != null || navigationBarMenuView.getItemBackground() == null) {
                return;
            }
            navigationBarMenuView.setItemBackground(null);
            return;
        }
        this.d = colorStateList;
        if (colorStateList == null) {
            navigationBarMenuView.setItemBackground(null);
        } else {
            navigationBarMenuView.setItemBackground(new RippleDrawable(RippleUtils.convertToRippleDrawableColor(colorStateList), null, null));
        }
    }

    public void setItemTextAppearanceActive(@StyleRes int i3) {
        this.f8580b.setItemTextAppearanceActive(i3);
    }

    public void setItemTextAppearanceInactive(@StyleRes int i3) {
        this.f8580b.setItemTextAppearanceInactive(i3);
    }

    public void setItemTextColor(@Nullable ColorStateList colorStateList) {
        this.f8580b.setItemTextColor(colorStateList);
    }

    public void setLabelVisibilityMode(int i3) {
        NavigationBarMenuView navigationBarMenuView = this.f8580b;
        if (navigationBarMenuView.getLabelVisibilityMode() != i3) {
            navigationBarMenuView.setLabelVisibilityMode(i3);
            this.c.updateMenuView(false);
        }
    }

    public void setOnItemReselectedListener(@Nullable OnItemReselectedListener onItemReselectedListener) {
        this.f8583g = onItemReselectedListener;
    }

    public void setOnItemSelectedListener(@Nullable OnItemSelectedListener onItemSelectedListener) {
        this.f8582f = onItemSelectedListener;
    }

    public void setSelectedItemId(@IdRes int i3) {
        NavigationBarMenu navigationBarMenu = this.f8579a;
        MenuItem menuItemFindItem = navigationBarMenu.findItem(i3);
        if (menuItemFindItem == null || navigationBarMenu.performItemAction(menuItemFindItem, this.c, 0)) {
            return;
        }
        menuItemFindItem.setChecked(true);
    }
}
