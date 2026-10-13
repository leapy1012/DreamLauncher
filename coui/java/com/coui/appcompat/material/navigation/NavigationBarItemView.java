package com.coui.appcompat.material.navigation;

import android.R;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.FloatRange;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.RestrictTo;
import androidx.annotation.StyleRes;
import androidx.appcompat.view.menu.MenuItemImpl;
import androidx.appcompat.view.menu.MenuView;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.PointerIconCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.widget.TextViewCompat;
import com.google.android.material.animation.AnimationUtils;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.motion.MotionUtils;
import com.google.android.material.resources.MaterialResources;

/* JADX INFO: loaded from: classes4.dex */
@RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
public abstract class NavigationBarItemView extends FrameLayout implements MenuView.ItemView {
    public static final int[] D = {R.attr.state_checked};
    public static final ActiveIndicatorTransform E;
    public static final ActiveIndicatorUnlabeledTransform F;
    public boolean A;
    public int B;

    @Nullable
    public BadgeDrawable C;

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    public boolean f8527a;

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    public int f8528b;
    public int c;
    public float d;

    /* JADX INFO: renamed from: e, reason: collision with root package name */
    public float f8529e;

    /* JADX INFO: renamed from: f, reason: collision with root package name */
    public float f8530f;

    /* JADX INFO: renamed from: g, reason: collision with root package name */
    public int f8531g;

    /* JADX INFO: renamed from: h, reason: collision with root package name */
    public boolean f8532h;

    /* JADX INFO: renamed from: i, reason: collision with root package name */
    @Nullable
    public final FrameLayout f8533i;

    /* JADX INFO: renamed from: j, reason: collision with root package name */
    @Nullable
    public final View f8534j;

    /* JADX INFO: renamed from: k, reason: collision with root package name */
    public final ImageView f8535k;

    /* JADX INFO: renamed from: l, reason: collision with root package name */
    public final ViewGroup f8536l;

    /* JADX INFO: renamed from: m, reason: collision with root package name */
    public final TextView f8537m;

    /* JADX INFO: renamed from: n, reason: collision with root package name */
    public final TextView f8538n;

    /* JADX INFO: renamed from: o, reason: collision with root package name */
    public int f8539o;

    /* JADX INFO: renamed from: p, reason: collision with root package name */
    @Nullable
    public MenuItemImpl f8540p;

    /* JADX INFO: renamed from: q, reason: collision with root package name */
    @Nullable
    public ColorStateList f8541q;

    /* JADX INFO: renamed from: r, reason: collision with root package name */
    @Nullable
    public Drawable f8542r;

    /* JADX INFO: renamed from: s, reason: collision with root package name */
    @Nullable
    public Drawable f8543s;
    public ValueAnimator t;

    /* JADX INFO: renamed from: u, reason: collision with root package name */
    public ActiveIndicatorTransform f8544u;

    /* JADX INFO: renamed from: v, reason: collision with root package name */
    public float f8545v;
    public boolean w;

    /* JADX INFO: renamed from: x, reason: collision with root package name */
    public int f8546x;
    public int y;

    public static class ActiveIndicatorTransform {
        private ActiveIndicatorTransform() {
        }

        public float a(@FloatRange(from = 0.0d, to = 1.0d) float f3, @FloatRange(from = 0.0d, to = 1.0d) float f10) {
            return 1.0f;
        }

        public /* synthetic */ ActiveIndicatorTransform(int i3) {
            this();
        }
    }

    public static class ActiveIndicatorUnlabeledTransform extends ActiveIndicatorTransform {
        private ActiveIndicatorUnlabeledTransform() {
            super(0);
        }

        @Override // com.coui.appcompat.material.navigation.NavigationBarItemView.ActiveIndicatorTransform
        public final float a(float f3, float f10) {
            return AnimationUtils.lerp(0.4f, 1.0f, f3);
        }

        public /* synthetic */ ActiveIndicatorUnlabeledTransform(int i3) {
            this();
        }
    }

    static {
        int i3 = 0;
        E = new ActiveIndicatorTransform(i3);
        F = new ActiveIndicatorUnlabeledTransform(i3);
    }

    public NavigationBarItemView(@NonNull Context context) {
        super(context);
        this.f8527a = false;
        this.f8539o = -1;
        this.f8544u = E;
        this.f8545v = 0.0f;
        this.w = false;
        this.f8546x = 0;
        this.y = 0;
        this.A = false;
        this.B = 0;
        LayoutInflater.from(context).inflate(getItemLayoutResId(), (ViewGroup) this, true);
        this.f8533i = (FrameLayout) findViewById(com.google.android.material.R.id.navigation_bar_item_icon_container);
        this.f8534j = findViewById(com.google.android.material.R.id.navigation_bar_item_active_indicator_view);
        ImageView imageView = (ImageView) findViewById(com.google.android.material.R.id.navigation_bar_item_icon_view);
        this.f8535k = imageView;
        ViewGroup viewGroup = (ViewGroup) findViewById(com.google.android.material.R.id.navigation_bar_item_labels_group);
        this.f8536l = viewGroup;
        TextView textView = (TextView) findViewById(com.google.android.material.R.id.navigation_bar_item_small_label_view);
        this.f8537m = textView;
        TextView textView2 = (TextView) findViewById(com.google.android.material.R.id.navigation_bar_item_large_label_view);
        this.f8538n = textView2;
        setBackgroundResource(getItemBackgroundResId());
        this.f8528b = getResources().getDimensionPixelSize(getItemDefaultMarginResId());
        this.c = viewGroup.getPaddingBottom();
        ViewCompat.setImportantForAccessibility(textView, 2);
        ViewCompat.setImportantForAccessibility(textView2, 2);
        setFocusable(true);
        a(textView.getTextSize(), textView2.getTextSize());
        if (imageView != null) {
            imageView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() { // from class: com.coui.appcompat.material.navigation.NavigationBarItemView.1
                @Override // android.view.View.OnLayoutChangeListener
                public final void onLayoutChange(View view, int i3, int i10, int i11, int i12, int i13, int i14, int i15, int i16) {
                    BadgeDrawable badgeDrawable;
                    NavigationBarItemView navigationBarItemView = NavigationBarItemView.this;
                    if (navigationBarItemView.f8535k.getVisibility() != 0 || (badgeDrawable = navigationBarItemView.C) == null) {
                        return;
                    }
                    boolean z6 = BadgeUtils.USE_COMPAT_PARENT;
                    ImageView imageView2 = navigationBarItemView.f8535k;
                    BadgeUtils.setBadgeDrawableBounds(badgeDrawable, imageView2, z6 ? (FrameLayout) imageView2.getParent() : null);
                }
            });
        }
    }

    public static void c(@NonNull View view, float f3, float f10, int i3) {
        view.setScaleX(f3);
        view.setScaleY(f10);
        view.setVisibility(i3);
    }

    public static void d(@NonNull View view, int i3, int i10) {
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) view.getLayoutParams();
        layoutParams.topMargin = i3;
        layoutParams.bottomMargin = i3;
        layoutParams.gravity = i10;
        view.setLayoutParams(layoutParams);
    }

    public static void f(@NonNull View view, int i3) {
        view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), i3);
    }

    private View getIconOrContainer() {
        FrameLayout frameLayout = this.f8533i;
        return frameLayout != null ? frameLayout : this.f8535k;
    }

    private int getItemVisiblePosition() {
        ViewGroup viewGroup = (ViewGroup) getParent();
        int iIndexOfChild = viewGroup.indexOfChild(this);
        int i3 = 0;
        for (int i10 = 0; i10 < iIndexOfChild; i10++) {
            View childAt = viewGroup.getChildAt(i10);
            if ((childAt instanceof NavigationBarItemView) && childAt.getVisibility() == 0) {
                i3++;
            }
        }
        return i3;
    }

    private int getSuggestedIconHeight() {
        BadgeDrawable badgeDrawable = this.C;
        int minimumHeight = badgeDrawable != null ? badgeDrawable.getMinimumHeight() / 2 : 0;
        return this.f8535k.getMeasuredWidth() + Math.max(minimumHeight, ((FrameLayout.LayoutParams) getIconOrContainer().getLayoutParams()).topMargin) + minimumHeight;
    }

    private int getSuggestedIconWidth() {
        BadgeDrawable badgeDrawable = this.C;
        int minimumWidth = badgeDrawable == null ? 0 : badgeDrawable.getMinimumWidth() - this.C.getHorizontalOffset();
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) getIconOrContainer().getLayoutParams();
        return Math.max(minimumWidth, layoutParams.rightMargin) + this.f8535k.getMeasuredWidth() + Math.max(minimumWidth, layoutParams.leftMargin);
    }

    public final void a(float f3, float f10) {
        this.d = f3 - f10;
        this.f8529e = (f10 * 1.0f) / f3;
        this.f8530f = (f3 * 1.0f) / f10;
    }

    public final void b(@FloatRange(from = 0.0d, to = 1.0d) float f3, float f10) {
        View view = this.f8534j;
        if (view != null) {
            ActiveIndicatorTransform activeIndicatorTransform = this.f8544u;
            activeIndicatorTransform.getClass();
            view.setScaleX(AnimationUtils.lerp(0.4f, 1.0f, f3));
            view.setScaleY(activeIndicatorTransform.a(f3, f10));
            view.setAlpha(AnimationUtils.lerp(0.0f, 1.0f, f10 == 0.0f ? 0.8f : 0.0f, f10 == 0.0f ? 1.0f : 0.2f, f3));
        }
        this.f8545v = f3;
    }

    public final void e(int i3) {
        View view = this.f8534j;
        if (view == null) {
            return;
        }
        int iMin = Math.min(this.f8546x, i3 - (this.B * 2));
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) view.getLayoutParams();
        layoutParams.height = (this.A && this.f8531g == 2) ? iMin : this.y;
        layoutParams.width = iMin;
        view.setLayoutParams(layoutParams);
    }

    @Nullable
    public Drawable getActiveIndicatorDrawable() {
        View view = this.f8534j;
        if (view == null) {
            return null;
        }
        return view.getBackground();
    }

    @Nullable
    public BadgeDrawable getBadge() {
        return this.C;
    }

    @DrawableRes
    public int getItemBackgroundResId() {
        return com.google.android.material.R.drawable.mtrl_navigation_bar_item_background;
    }

    @Override // androidx.appcompat.view.menu.MenuView.ItemView
    @Nullable
    public MenuItemImpl getItemData() {
        return this.f8540p;
    }

    @DimenRes
    public int getItemDefaultMarginResId() {
        return com.google.android.material.R.dimen.mtrl_navigation_bar_item_default_margin;
    }

    @LayoutRes
    public abstract int getItemLayoutResId();

    public int getItemPosition() {
        return this.f8539o;
    }

    @Override // android.view.View
    public int getSuggestedMinimumHeight() {
        ViewGroup viewGroup = this.f8536l;
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) viewGroup.getLayoutParams();
        return viewGroup.getMeasuredHeight() + getSuggestedIconHeight() + layoutParams.topMargin + layoutParams.bottomMargin;
    }

    @Override // android.view.View
    public int getSuggestedMinimumWidth() {
        ViewGroup viewGroup = this.f8536l;
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) viewGroup.getLayoutParams();
        return Math.max(getSuggestedIconWidth(), viewGroup.getMeasuredWidth() + layoutParams.leftMargin + layoutParams.rightMargin);
    }

    public void initialize(@NonNull MenuItemImpl menuItemImpl, int i3) {
        this.f8540p = menuItemImpl;
        setCheckable(menuItemImpl.isCheckable());
        setChecked(menuItemImpl.isChecked());
        setEnabled(menuItemImpl.isEnabled());
        setIcon(menuItemImpl.getIcon());
        setTitle(menuItemImpl.getTitle());
        setId(menuItemImpl.getItemId());
        if (!TextUtils.isEmpty(menuItemImpl.getContentDescription())) {
            setContentDescription(menuItemImpl.getContentDescription());
        }
        TooltipCompat.setTooltipText(this, !TextUtils.isEmpty(menuItemImpl.getTooltipText()) ? menuItemImpl.getTooltipText() : menuItemImpl.getTitle());
        setVisibility(menuItemImpl.isVisible() ? 0 : 8);
        this.f8527a = true;
    }

    @Override // android.view.ViewGroup, android.view.View
    @NonNull
    public final int[] onCreateDrawableState(int i3) {
        int[] iArrOnCreateDrawableState = super.onCreateDrawableState(i3 + 1);
        MenuItemImpl menuItemImpl = this.f8540p;
        if (menuItemImpl != null && menuItemImpl.isCheckable() && this.f8540p.isChecked()) {
            View.mergeDrawableStates(iArrOnCreateDrawableState, D);
        }
        return iArrOnCreateDrawableState;
    }

    @Override // android.view.View
    public void onInitializeAccessibilityNodeInfo(@NonNull AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        BadgeDrawable badgeDrawable = this.C;
        if (badgeDrawable != null && badgeDrawable.isVisible()) {
            CharSequence title = this.f8540p.getTitle();
            if (!TextUtils.isEmpty(this.f8540p.getContentDescription())) {
                title = this.f8540p.getContentDescription();
            }
            accessibilityNodeInfo.setContentDescription(((Object) title) + ", " + ((Object) this.C.getContentDescription()));
        }
        AccessibilityNodeInfoCompat accessibilityNodeInfoCompatWrap = AccessibilityNodeInfoCompat.wrap(accessibilityNodeInfo);
        accessibilityNodeInfoCompatWrap.setCollectionItemInfo(AccessibilityNodeInfoCompat.CollectionItemInfoCompat.obtain(0, 1, getItemVisiblePosition(), 1, false, isSelected()));
        if (isSelected()) {
            accessibilityNodeInfoCompatWrap.setClickable(false);
            accessibilityNodeInfoCompatWrap.removeAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK);
        }
        accessibilityNodeInfoCompatWrap.setRoleDescription(getResources().getString(com.google.android.material.R.string.item_view_role_description));
    }

    @Override // android.view.View
    public void onSizeChanged(final int i3, int i10, int i11, int i12) {
        super.onSizeChanged(i3, i10, i11, i12);
        post(new Runnable() { // from class: com.coui.appcompat.material.navigation.NavigationBarItemView.2
            @Override // java.lang.Runnable
            public final void run() {
                int[] iArr = NavigationBarItemView.D;
                NavigationBarItemView.this.e(i3);
            }
        });
    }

    @Override // androidx.appcompat.view.menu.MenuView.ItemView
    public final boolean prefersCondensedTitle() {
        return false;
    }

    public void setActiveIndicatorDrawable(@Nullable Drawable drawable) {
        View view = this.f8534j;
        if (view == null) {
            return;
        }
        view.setBackgroundDrawable(drawable);
    }

    public void setActiveIndicatorEnabled(boolean z6) {
        this.w = z6;
        View view = this.f8534j;
        if (view != null) {
            view.setVisibility(z6 ? 0 : 8);
            requestLayout();
        }
    }

    public void setActiveIndicatorHeight(int i3) {
        this.y = i3;
        e(getWidth());
    }

    public void setActiveIndicatorMarginHorizontal(@Px int i3) {
        this.B = i3;
        e(getWidth());
    }

    public void setActiveIndicatorResizeable(boolean z6) {
        this.A = z6;
    }

    public void setActiveIndicatorWidth(int i3) {
        this.f8546x = i3;
        e(getWidth());
    }

    public void setBadge(@NonNull BadgeDrawable badgeDrawable) {
        BadgeDrawable badgeDrawable2 = this.C;
        if (badgeDrawable2 == badgeDrawable) {
            return;
        }
        boolean z6 = badgeDrawable2 != null;
        ImageView imageView = this.f8535k;
        if (z6 && imageView != null) {
            Log.w("NavigationBar", "Multiple badges shouldn't be attached to one item.");
            if (this.C != null) {
                setClipChildren(true);
                setClipToPadding(true);
                BadgeUtils.detachBadgeDrawable(this.C, imageView);
                this.C = null;
            }
        }
        this.C = badgeDrawable;
        if (imageView == null || badgeDrawable == null) {
            return;
        }
        setClipChildren(false);
        setClipToPadding(false);
        BadgeUtils.attachBadgeDrawable(this.C, imageView, BadgeUtils.USE_COMPAT_PARENT ? (FrameLayout) imageView.getParent() : null);
    }

    @Override // androidx.appcompat.view.menu.MenuView.ItemView
    public void setCheckable(boolean z6) {
        refreshDrawableState();
    }

    public void setChecked(boolean z6) {
        TextView textView = this.f8538n;
        textView.setPivotX(textView.getWidth() / 2);
        textView.setPivotY(textView.getBaseline());
        TextView textView2 = this.f8537m;
        textView2.setPivotX(textView2.getWidth() / 2);
        textView2.setPivotY(textView2.getBaseline());
        final float f3 = z6 ? 1.0f : 0.0f;
        if (this.w && this.f8527a && ViewCompat.isAttachedToWindow(this)) {
            ValueAnimator valueAnimator = this.t;
            if (valueAnimator != null) {
                valueAnimator.cancel();
                this.t = null;
            }
            ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(this.f8545v, f3);
            this.t = valueAnimatorOfFloat;
            valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.coui.appcompat.material.navigation.NavigationBarItemView.3
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public final void onAnimationUpdate(ValueAnimator valueAnimator2) {
                    float fFloatValue = ((Float) valueAnimator2.getAnimatedValue()).floatValue();
                    int[] iArr = NavigationBarItemView.D;
                    NavigationBarItemView.this.b(fFloatValue, f3);
                }
            });
            this.t.setInterpolator(MotionUtils.resolveThemeInterpolator(getContext(), com.google.android.material.R.attr.motionEasingStandard, AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR));
            this.t.setDuration(MotionUtils.resolveThemeDuration(getContext(), com.google.android.material.R.attr.motionDurationLong1, getResources().getInteger(com.google.android.material.R.integer.material_motion_duration_long_1)));
            this.t.start();
        } else {
            b(f3, f3);
        }
        int i3 = this.f8531g;
        ViewGroup viewGroup = this.f8536l;
        if (i3 != -1) {
            if (i3 == 0) {
                if (z6) {
                    d(getIconOrContainer(), this.f8528b, 49);
                    f(viewGroup, this.c);
                    textView.setVisibility(0);
                } else {
                    d(getIconOrContainer(), this.f8528b, 17);
                    f(viewGroup, 0);
                    textView.setVisibility(4);
                }
                textView2.setVisibility(4);
            } else if (i3 == 1) {
                f(viewGroup, this.c);
                if (z6) {
                    d(getIconOrContainer(), (int) (this.f8528b + this.d), 49);
                    c(textView, 1.0f, 1.0f, 0);
                    float f10 = this.f8529e;
                    c(textView2, f10, f10, 4);
                } else {
                    d(getIconOrContainer(), this.f8528b, 49);
                    float f11 = this.f8530f;
                    c(textView, f11, f11, 4);
                    c(textView2, 1.0f, 1.0f, 0);
                }
            } else if (i3 == 2) {
                d(getIconOrContainer(), this.f8528b, 17);
                textView.setVisibility(8);
                textView2.setVisibility(8);
            }
        } else if (this.f8532h) {
            if (z6) {
                d(getIconOrContainer(), this.f8528b, 49);
                f(viewGroup, this.c);
                textView.setVisibility(0);
            } else {
                d(getIconOrContainer(), this.f8528b, 17);
                f(viewGroup, 0);
                textView.setVisibility(4);
            }
            textView2.setVisibility(4);
        } else {
            f(viewGroup, this.c);
            if (z6) {
                d(getIconOrContainer(), (int) (this.f8528b + this.d), 49);
                c(textView, 1.0f, 1.0f, 0);
                float f12 = this.f8529e;
                c(textView2, f12, f12, 4);
            } else {
                d(getIconOrContainer(), this.f8528b, 49);
                float f13 = this.f8530f;
                c(textView, f13, f13, 4);
                c(textView2, 1.0f, 1.0f, 0);
            }
        }
        refreshDrawableState();
        setSelected(z6);
    }

    @Override // android.view.View, androidx.appcompat.view.menu.MenuView.ItemView
    public void setEnabled(boolean z6) {
        super.setEnabled(z6);
        this.f8537m.setEnabled(z6);
        this.f8538n.setEnabled(z6);
        this.f8535k.setEnabled(z6);
        if (z6) {
            ViewCompat.setPointerIcon(this, PointerIconCompat.getSystemIcon(getContext(), 1002));
        } else {
            ViewCompat.setPointerIcon(this, null);
        }
    }

    @Override // androidx.appcompat.view.menu.MenuView.ItemView
    public void setIcon(@Nullable Drawable drawable) {
        if (drawable == this.f8542r) {
            return;
        }
        this.f8542r = drawable;
        if (drawable != null) {
            Drawable.ConstantState constantState = drawable.getConstantState();
            if (constantState != null) {
                drawable = constantState.newDrawable();
            }
            drawable = DrawableCompat.wrap(drawable).mutate();
            this.f8543s = drawable;
            ColorStateList colorStateList = this.f8541q;
            if (colorStateList != null) {
                DrawableCompat.setTintList(drawable, colorStateList);
            }
        }
        this.f8535k.setImageDrawable(drawable);
    }

    public void setIconSize(int i3) {
        ImageView imageView = this.f8535k;
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) imageView.getLayoutParams();
        layoutParams.width = i3;
        layoutParams.height = i3;
        imageView.setLayoutParams(layoutParams);
    }

    public void setIconTintList(@Nullable ColorStateList colorStateList) {
        Drawable drawable;
        this.f8541q = colorStateList;
        if (this.f8540p == null || (drawable = this.f8543s) == null) {
            return;
        }
        DrawableCompat.setTintList(drawable, colorStateList);
        this.f8543s.invalidateSelf();
    }

    public void setItemBackground(int i3) {
        setItemBackground(i3 == 0 ? null : ContextCompat.getDrawable(getContext(), i3));
    }

    public void setItemPaddingBottom(int i3) {
        if (this.c != i3) {
            this.c = i3;
            MenuItemImpl menuItemImpl = this.f8540p;
            if (menuItemImpl != null) {
                setChecked(menuItemImpl.isChecked());
            }
        }
    }

    public void setItemPaddingTop(int i3) {
        if (this.f8528b != i3) {
            this.f8528b = i3;
            MenuItemImpl menuItemImpl = this.f8540p;
            if (menuItemImpl != null) {
                setChecked(menuItemImpl.isChecked());
            }
        }
    }

    public void setItemPosition(int i3) {
        this.f8539o = i3;
    }

    public void setLabelVisibilityMode(int i3) {
        if (this.f8531g != i3) {
            this.f8531g = i3;
            if (this.A && i3 == 2) {
                this.f8544u = F;
            } else {
                this.f8544u = E;
            }
            e(getWidth());
            MenuItemImpl menuItemImpl = this.f8540p;
            if (menuItemImpl != null) {
                setChecked(menuItemImpl.isChecked());
            }
        }
    }

    public void setShifting(boolean z6) {
        if (this.f8532h != z6) {
            this.f8532h = z6;
            MenuItemImpl menuItemImpl = this.f8540p;
            if (menuItemImpl != null) {
                setChecked(menuItemImpl.isChecked());
            }
        }
    }

    @Override // androidx.appcompat.view.menu.MenuView.ItemView
    public final void setShortcut(boolean z6, char c) {
    }

    public void setTextAppearanceActive(@StyleRes int i3) {
        TextView textView = this.f8538n;
        TextViewCompat.setTextAppearance(textView, i3);
        int unscaledTextSize = MaterialResources.getUnscaledTextSize(textView.getContext(), i3, 0);
        if (unscaledTextSize != 0) {
            textView.setTextSize(0, unscaledTextSize);
        }
        a(this.f8537m.getTextSize(), textView.getTextSize());
    }

    public void setTextAppearanceInactive(@StyleRes int i3) {
        TextView textView = this.f8537m;
        TextViewCompat.setTextAppearance(textView, i3);
        int unscaledTextSize = MaterialResources.getUnscaledTextSize(textView.getContext(), i3, 0);
        if (unscaledTextSize != 0) {
            textView.setTextSize(0, unscaledTextSize);
        }
        a(textView.getTextSize(), this.f8538n.getTextSize());
    }

    public void setTextColor(@Nullable ColorStateList colorStateList) {
        if (colorStateList != null) {
            this.f8537m.setTextColor(colorStateList);
            this.f8538n.setTextColor(colorStateList);
        }
    }

    @Override // androidx.appcompat.view.menu.MenuView.ItemView
    public void setTitle(@Nullable CharSequence charSequence) {
        this.f8537m.setText(charSequence);
        this.f8538n.setText(charSequence);
        MenuItemImpl menuItemImpl = this.f8540p;
        if (menuItemImpl == null || TextUtils.isEmpty(menuItemImpl.getContentDescription())) {
            setContentDescription(charSequence);
        }
        MenuItemImpl menuItemImpl2 = this.f8540p;
        if (menuItemImpl2 != null && !TextUtils.isEmpty(menuItemImpl2.getTooltipText())) {
            charSequence = this.f8540p.getTooltipText();
        }
        TooltipCompat.setTooltipText(this, charSequence);
    }

    @Override // androidx.appcompat.view.menu.MenuView.ItemView
    public final boolean showsIcon() {
        return true;
    }

    public void setItemBackground(@Nullable Drawable drawable) {
        if (drawable != null && drawable.getConstantState() != null) {
            drawable = drawable.getConstantState().newDrawable().mutate();
        }
        ViewCompat.setBackground(this, drawable);
    }
}
