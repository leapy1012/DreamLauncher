package com.coui.appcompat.material.navigation;

import android.R;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;
import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.RestrictTo;
import androidx.annotation.StyleRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuItemImpl;
import androidx.appcompat.view.menu.MenuView;
import androidx.core.util.Pools;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.transition.AutoTransition;
import com.google.android.material.animation.AnimationUtils;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.internal.TextScale;
import com.google.android.material.motion.MotionUtils;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import java.util.HashSet;

/* JADX INFO: loaded from: classes4.dex */
@RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
public abstract class NavigationBarMenuView extends ViewGroup implements MenuView {
    public static final int[] D = {R.attr.state_checked};
    public static final int[] E = {-16842910};
    public ColorStateList A;
    public NavigationBarPresenter B;
    public MenuBuilder C;

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    @Nullable
    public final AutoTransition f8554a;

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    @NonNull
    public final View.OnClickListener f8555b;
    public final Pools.SynchronizedPool c;

    @NonNull
    public final SparseArray<View.OnTouchListener> d;

    /* JADX INFO: renamed from: e, reason: collision with root package name */
    public int f8556e;

    /* JADX INFO: renamed from: f, reason: collision with root package name */
    @Nullable
    public NavigationBarItemView[] f8557f;

    /* JADX INFO: renamed from: g, reason: collision with root package name */
    public int f8558g;

    /* JADX INFO: renamed from: h, reason: collision with root package name */
    public int f8559h;

    /* JADX INFO: renamed from: i, reason: collision with root package name */
    @Nullable
    public ColorStateList f8560i;

    /* JADX INFO: renamed from: j, reason: collision with root package name */
    @Dimension
    public int f8561j;

    /* JADX INFO: renamed from: k, reason: collision with root package name */
    public ColorStateList f8562k;

    /* JADX INFO: renamed from: l, reason: collision with root package name */
    @Nullable
    public final ColorStateList f8563l;

    /* JADX INFO: renamed from: m, reason: collision with root package name */
    @StyleRes
    public int f8564m;

    /* JADX INFO: renamed from: n, reason: collision with root package name */
    @StyleRes
    public int f8565n;

    /* JADX INFO: renamed from: o, reason: collision with root package name */
    public Drawable f8566o;

    /* JADX INFO: renamed from: p, reason: collision with root package name */
    public int f8567p;

    /* JADX INFO: renamed from: q, reason: collision with root package name */
    @NonNull
    public final SparseArray<BadgeDrawable> f8568q;

    /* JADX INFO: renamed from: r, reason: collision with root package name */
    public int f8569r;

    /* JADX INFO: renamed from: s, reason: collision with root package name */
    public int f8570s;
    public boolean t;

    /* JADX INFO: renamed from: u, reason: collision with root package name */
    public int f8571u;

    /* JADX INFO: renamed from: v, reason: collision with root package name */
    public int f8572v;
    public int w;

    /* JADX INFO: renamed from: x, reason: collision with root package name */
    public ShapeAppearanceModel f8573x;
    public boolean y;

    public NavigationBarMenuView(@NonNull Context context) {
        super(context);
        this.c = new Pools.SynchronizedPool(5);
        this.d = new SparseArray<>(5);
        this.f8558g = 0;
        this.f8559h = 0;
        this.f8568q = new SparseArray<>(5);
        this.f8569r = -1;
        this.f8570s = -1;
        this.y = false;
        this.f8563l = b(R.attr.textColorSecondary);
        if (isInEditMode()) {
            this.f8554a = null;
        } else {
            AutoTransition autoTransition = new AutoTransition();
            this.f8554a = autoTransition;
            autoTransition.setOrdering(0);
            autoTransition.setDuration(MotionUtils.resolveThemeDuration(getContext(), com.google.android.material.R.attr.motionDurationLong1, getResources().getInteger(com.google.android.material.R.integer.material_motion_duration_long_1)));
            autoTransition.setInterpolator(MotionUtils.resolveThemeInterpolator(getContext(), com.google.android.material.R.attr.motionEasingStandard, AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR));
            autoTransition.addTransition(new TextScale());
        }
        this.f8555b = new View.OnClickListener() { // from class: com.coui.appcompat.material.navigation.NavigationBarMenuView.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                MenuItemImpl itemData = ((NavigationBarItemView) view).getItemData();
                NavigationBarMenuView navigationBarMenuView = NavigationBarMenuView.this;
                if (navigationBarMenuView.C.performItemAction(itemData, navigationBarMenuView.B, 0)) {
                    return;
                }
                itemData.setChecked(true);
            }
        };
        ViewCompat.setImportantForAccessibility(this, 1);
    }

    /* JADX WARN: Multi-variable type inference failed */
    private NavigationBarItemView getNewItem() {
        NavigationBarItemView navigationBarItemView = (NavigationBarItemView) this.c.acquire();
        return navigationBarItemView == null ? d(getContext()) : navigationBarItemView;
    }

    private void setBadgeIfNeeded(@NonNull NavigationBarItemView navigationBarItemView) {
        BadgeDrawable badgeDrawable;
        int id = navigationBarItemView.getId();
        if (id == -1 || (badgeDrawable = this.f8568q.get(id)) == null) {
            return;
        }
        navigationBarItemView.setBadge(badgeDrawable);
    }

    @SuppressLint({"ClickableViewAccessibility"})
    public final void a() {
        removeAllViews();
        NavigationBarItemView[] navigationBarItemViewArr = this.f8557f;
        if (navigationBarItemViewArr != null) {
            for (NavigationBarItemView navigationBarItemView : navigationBarItemViewArr) {
                if (navigationBarItemView != null) {
                    this.c.release(navigationBarItemView);
                    if (navigationBarItemView.C != null) {
                        ImageView imageView = navigationBarItemView.f8535k;
                        if (imageView != null) {
                            navigationBarItemView.setClipChildren(true);
                            navigationBarItemView.setClipToPadding(true);
                            BadgeUtils.detachBadgeDrawable(navigationBarItemView.C, imageView);
                        }
                        navigationBarItemView.C = null;
                    }
                    navigationBarItemView.f8540p = null;
                    navigationBarItemView.f8545v = 0.0f;
                    navigationBarItemView.f8527a = false;
                }
            }
        }
        if (this.C.size() == 0) {
            this.f8558g = 0;
            this.f8559h = 0;
            this.f8557f = null;
            return;
        }
        HashSet hashSet = new HashSet();
        for (int i3 = 0; i3 < this.C.size(); i3++) {
            hashSet.add(Integer.valueOf(this.C.getItem(i3).getItemId()));
        }
        int i10 = 0;
        while (true) {
            SparseArray<BadgeDrawable> sparseArray = this.f8568q;
            if (i10 >= sparseArray.size()) {
                break;
            }
            int iKeyAt = sparseArray.keyAt(i10);
            if (!hashSet.contains(Integer.valueOf(iKeyAt))) {
                sparseArray.delete(iKeyAt);
            }
            i10++;
        }
        this.f8557f = new NavigationBarItemView[this.C.size()];
        boolean zF = f(this.f8556e, this.C.getVisibleItems().size());
        for (int i11 = 0; i11 < this.C.size(); i11++) {
            this.B.f8576b = true;
            this.C.getItem(i11).setCheckable(true);
            this.B.f8576b = false;
            NavigationBarItemView newItem = getNewItem();
            this.f8557f[i11] = newItem;
            newItem.setIconTintList(this.f8560i);
            newItem.setIconSize(this.f8561j);
            newItem.setTextColor(this.f8563l);
            newItem.setTextAppearanceInactive(this.f8564m);
            newItem.setTextAppearanceActive(this.f8565n);
            newItem.setTextColor(this.f8562k);
            int i12 = this.f8569r;
            if (i12 != -1) {
                newItem.setItemPaddingTop(i12);
            }
            int i13 = this.f8570s;
            if (i13 != -1) {
                newItem.setItemPaddingBottom(i13);
            }
            newItem.setActiveIndicatorWidth(this.f8571u);
            newItem.setActiveIndicatorHeight(this.f8572v);
            newItem.setActiveIndicatorMarginHorizontal(this.w);
            newItem.setActiveIndicatorDrawable(c());
            newItem.setActiveIndicatorResizeable(this.y);
            newItem.setActiveIndicatorEnabled(this.t);
            Drawable drawable = this.f8566o;
            if (drawable != null) {
                newItem.setItemBackground(drawable);
            } else {
                newItem.setItemBackground(this.f8567p);
            }
            newItem.setShifting(zF);
            newItem.setLabelVisibilityMode(this.f8556e);
            MenuItemImpl menuItemImpl = (MenuItemImpl) this.C.getItem(i11);
            newItem.initialize(menuItemImpl, 0);
            newItem.setItemPosition(i11);
            int itemId = menuItemImpl.getItemId();
            newItem.setOnTouchListener(this.d.get(itemId));
            newItem.setOnClickListener(this.f8555b);
            int i14 = this.f8558g;
            if (i14 != 0 && itemId == i14) {
                this.f8559h = i11;
            }
            setBadgeIfNeeded(newItem);
            addView(newItem);
        }
        int iMin = Math.min(this.C.size() - 1, this.f8559h);
        this.f8559h = iMin;
        this.C.getItem(iMin).setChecked(true);
    }

    @Nullable
    public final ColorStateList b(int i3) {
        TypedValue typedValue = new TypedValue();
        if (!getContext().getTheme().resolveAttribute(R.attr.textColorSecondary, typedValue, true)) {
            return null;
        }
        ColorStateList colorStateList = AppCompatResources.getColorStateList(getContext(), typedValue.resourceId);
        if (!getContext().getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true)) {
            return null;
        }
        int i10 = typedValue.data;
        int defaultColor = colorStateList.getDefaultColor();
        int[] iArr = E;
        return new ColorStateList(new int[][]{iArr, D, ViewGroup.EMPTY_STATE_SET}, new int[]{colorStateList.getColorForState(iArr, defaultColor), i10, defaultColor});
    }

    @Nullable
    public final MaterialShapeDrawable c() {
        if (this.f8573x == null || this.A == null) {
            return null;
        }
        MaterialShapeDrawable materialShapeDrawable = new MaterialShapeDrawable(this.f8573x);
        materialShapeDrawable.setFillColor(this.A);
        return materialShapeDrawable;
    }

    @NonNull
    public abstract NavigationBarItemView d(@NonNull Context context);

    @Nullable
    public final NavigationBarItemView e(int i3) {
        if (i3 == -1) {
            throw new IllegalArgumentException(i3 + " is not a valid view id");
        }
        NavigationBarItemView[] navigationBarItemViewArr = this.f8557f;
        if (navigationBarItemViewArr == null) {
            return null;
        }
        for (NavigationBarItemView navigationBarItemView : navigationBarItemViewArr) {
            if (navigationBarItemView.getId() == i3) {
                return navigationBarItemView;
            }
        }
        return null;
    }

    public final boolean f(int i3, int i10) {
        if (i3 == -1) {
            if (i10 <= 3) {
                return false;
            }
        } else if (i3 != 0) {
            return false;
        }
        return true;
    }

    public SparseArray<BadgeDrawable> getBadgeDrawables() {
        return this.f8568q;
    }

    @Nullable
    public ColorStateList getIconTintList() {
        return this.f8560i;
    }

    @Nullable
    public ColorStateList getItemActiveIndicatorColor() {
        return this.A;
    }

    public boolean getItemActiveIndicatorEnabled() {
        return this.t;
    }

    @Px
    public int getItemActiveIndicatorHeight() {
        return this.f8572v;
    }

    @Px
    public int getItemActiveIndicatorMarginHorizontal() {
        return this.w;
    }

    @Nullable
    public ShapeAppearanceModel getItemActiveIndicatorShapeAppearance() {
        return this.f8573x;
    }

    @Px
    public int getItemActiveIndicatorWidth() {
        return this.f8571u;
    }

    @Nullable
    public Drawable getItemBackground() {
        NavigationBarItemView[] navigationBarItemViewArr = this.f8557f;
        return (navigationBarItemViewArr == null || navigationBarItemViewArr.length <= 0) ? this.f8566o : navigationBarItemViewArr[0].getBackground();
    }

    @Deprecated
    public int getItemBackgroundRes() {
        return this.f8567p;
    }

    @Dimension
    public int getItemIconSize() {
        return this.f8561j;
    }

    @Px
    public int getItemPaddingBottom() {
        return this.f8570s;
    }

    @Px
    public int getItemPaddingTop() {
        return this.f8569r;
    }

    @StyleRes
    public int getItemTextAppearanceActive() {
        return this.f8565n;
    }

    @StyleRes
    public int getItemTextAppearanceInactive() {
        return this.f8564m;
    }

    @Nullable
    public ColorStateList getItemTextColor() {
        return this.f8562k;
    }

    public int getLabelVisibilityMode() {
        return this.f8556e;
    }

    @Nullable
    public MenuBuilder getMenu() {
        return this.C;
    }

    public int getSelectedItemId() {
        return this.f8558g;
    }

    public int getSelectedItemPosition() {
        return this.f8559h;
    }

    @Override // androidx.appcompat.view.menu.MenuView
    public int getWindowAnimations() {
        return 0;
    }

    @Override // androidx.appcompat.view.menu.MenuView
    public final void initialize(@NonNull MenuBuilder menuBuilder) {
        this.C = menuBuilder;
    }

    @Override // android.view.View
    public void onInitializeAccessibilityNodeInfo(@NonNull AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        AccessibilityNodeInfoCompat.wrap(accessibilityNodeInfo).setCollectionInfo(AccessibilityNodeInfoCompat.CollectionInfoCompat.obtain(1, this.C.getVisibleItems().size(), false, 1));
    }

    public void setIconTintList(@Nullable ColorStateList colorStateList) {
        this.f8560i = colorStateList;
        NavigationBarItemView[] navigationBarItemViewArr = this.f8557f;
        if (navigationBarItemViewArr != null) {
            for (NavigationBarItemView navigationBarItemView : navigationBarItemViewArr) {
                navigationBarItemView.setIconTintList(colorStateList);
            }
        }
    }

    public void setItemActiveIndicatorColor(@Nullable ColorStateList colorStateList) {
        this.A = colorStateList;
        NavigationBarItemView[] navigationBarItemViewArr = this.f8557f;
        if (navigationBarItemViewArr != null) {
            for (NavigationBarItemView navigationBarItemView : navigationBarItemViewArr) {
                navigationBarItemView.setActiveIndicatorDrawable(c());
            }
        }
    }

    public void setItemActiveIndicatorEnabled(boolean z6) {
        this.t = z6;
        NavigationBarItemView[] navigationBarItemViewArr = this.f8557f;
        if (navigationBarItemViewArr != null) {
            for (NavigationBarItemView navigationBarItemView : navigationBarItemViewArr) {
                navigationBarItemView.setActiveIndicatorEnabled(z6);
            }
        }
    }

    public void setItemActiveIndicatorHeight(@Px int i3) {
        this.f8572v = i3;
        NavigationBarItemView[] navigationBarItemViewArr = this.f8557f;
        if (navigationBarItemViewArr != null) {
            for (NavigationBarItemView navigationBarItemView : navigationBarItemViewArr) {
                navigationBarItemView.setActiveIndicatorHeight(i3);
            }
        }
    }

    public void setItemActiveIndicatorMarginHorizontal(@Px int i3) {
        this.w = i3;
        NavigationBarItemView[] navigationBarItemViewArr = this.f8557f;
        if (navigationBarItemViewArr != null) {
            for (NavigationBarItemView navigationBarItemView : navigationBarItemViewArr) {
                navigationBarItemView.setActiveIndicatorMarginHorizontal(i3);
            }
        }
    }

    public void setItemActiveIndicatorResizeable(boolean z6) {
        this.y = z6;
        NavigationBarItemView[] navigationBarItemViewArr = this.f8557f;
        if (navigationBarItemViewArr != null) {
            for (NavigationBarItemView navigationBarItemView : navigationBarItemViewArr) {
                navigationBarItemView.setActiveIndicatorResizeable(z6);
            }
        }
    }

    public void setItemActiveIndicatorShapeAppearance(@Nullable ShapeAppearanceModel shapeAppearanceModel) {
        this.f8573x = shapeAppearanceModel;
        NavigationBarItemView[] navigationBarItemViewArr = this.f8557f;
        if (navigationBarItemViewArr != null) {
            for (NavigationBarItemView navigationBarItemView : navigationBarItemViewArr) {
                navigationBarItemView.setActiveIndicatorDrawable(c());
            }
        }
    }

    public void setItemActiveIndicatorWidth(@Px int i3) {
        this.f8571u = i3;
        NavigationBarItemView[] navigationBarItemViewArr = this.f8557f;
        if (navigationBarItemViewArr != null) {
            for (NavigationBarItemView navigationBarItemView : navigationBarItemViewArr) {
                navigationBarItemView.setActiveIndicatorWidth(i3);
            }
        }
    }

    public void setItemBackground(@Nullable Drawable drawable) {
        this.f8566o = drawable;
        NavigationBarItemView[] navigationBarItemViewArr = this.f8557f;
        if (navigationBarItemViewArr != null) {
            for (NavigationBarItemView navigationBarItemView : navigationBarItemViewArr) {
                navigationBarItemView.setItemBackground(drawable);
            }
        }
    }

    public void setItemBackgroundRes(int i3) {
        this.f8567p = i3;
        NavigationBarItemView[] navigationBarItemViewArr = this.f8557f;
        if (navigationBarItemViewArr != null) {
            for (NavigationBarItemView navigationBarItemView : navigationBarItemViewArr) {
                navigationBarItemView.setItemBackground(i3);
            }
        }
    }

    public void setItemIconSize(@Dimension int i3) {
        this.f8561j = i3;
        NavigationBarItemView[] navigationBarItemViewArr = this.f8557f;
        if (navigationBarItemViewArr != null) {
            for (NavigationBarItemView navigationBarItemView : navigationBarItemViewArr) {
                navigationBarItemView.setIconSize(i3);
            }
        }
    }

    public void setItemPaddingBottom(@Px int i3) {
        this.f8570s = i3;
        NavigationBarItemView[] navigationBarItemViewArr = this.f8557f;
        if (navigationBarItemViewArr != null) {
            for (NavigationBarItemView navigationBarItemView : navigationBarItemViewArr) {
                navigationBarItemView.setItemPaddingBottom(i3);
            }
        }
    }

    public void setItemPaddingTop(@Px int i3) {
        this.f8569r = i3;
        NavigationBarItemView[] navigationBarItemViewArr = this.f8557f;
        if (navigationBarItemViewArr != null) {
            for (NavigationBarItemView navigationBarItemView : navigationBarItemViewArr) {
                navigationBarItemView.setItemPaddingTop(i3);
            }
        }
    }

    public void setItemTextAppearanceActive(@StyleRes int i3) {
        this.f8565n = i3;
        NavigationBarItemView[] navigationBarItemViewArr = this.f8557f;
        if (navigationBarItemViewArr != null) {
            for (NavigationBarItemView navigationBarItemView : navigationBarItemViewArr) {
                navigationBarItemView.setTextAppearanceActive(i3);
                ColorStateList colorStateList = this.f8562k;
                if (colorStateList != null) {
                    navigationBarItemView.setTextColor(colorStateList);
                }
            }
        }
    }

    public void setItemTextAppearanceInactive(@StyleRes int i3) {
        this.f8564m = i3;
        NavigationBarItemView[] navigationBarItemViewArr = this.f8557f;
        if (navigationBarItemViewArr != null) {
            for (NavigationBarItemView navigationBarItemView : navigationBarItemViewArr) {
                navigationBarItemView.setTextAppearanceInactive(i3);
                ColorStateList colorStateList = this.f8562k;
                if (colorStateList != null) {
                    navigationBarItemView.setTextColor(colorStateList);
                }
            }
        }
    }

    public void setItemTextColor(@Nullable ColorStateList colorStateList) {
        this.f8562k = colorStateList;
        NavigationBarItemView[] navigationBarItemViewArr = this.f8557f;
        if (navigationBarItemViewArr != null) {
            for (NavigationBarItemView navigationBarItemView : navigationBarItemViewArr) {
                navigationBarItemView.setTextColor(colorStateList);
            }
        }
    }

    public void setLabelVisibilityMode(int i3) {
        this.f8556e = i3;
    }

    public void setPresenter(@NonNull NavigationBarPresenter navigationBarPresenter) {
        this.B = navigationBarPresenter;
    }
}
