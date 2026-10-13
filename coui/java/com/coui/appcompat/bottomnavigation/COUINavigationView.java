package com.coui.appcompat.bottomnavigation;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.util.Property;
import android.view.ContextThemeWrapper;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.TintTypedArray;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.coui.appcompat.animation.COUIInEaseInterpolator;
import com.coui.appcompat.animation.COUILinearInterpolator;
import com.coui.appcompat.animation.COUIOutEaseInterpolator;
import com.coui.appcompat.contextutil.COUIContextUtil;
import com.coui.appcompat.material.bottomnavigation.BottomNavigationView;
import com.coui.appcompat.material.navigation.NavigationBarItemView;
import com.coui.appcompat.material.navigation.NavigationBarMenuView;
import com.coui.appcompat.material.navigation.NavigationBarView;
import com.coui.appcompat.poplist.PopupMenuConfigRule;
import com.coui.appcompat.textutil.COUIChangeTextUtil;
import com.google.android.material.internal.ViewUtils;

/* JADX INFO: loaded from: classes4.dex */
public class COUINavigationView extends BottomNavigationView implements PopupMenuConfigRule {
    public boolean A;
    public Rect B;
    public Rect C;
    public Rect D;

    /* JADX INFO: renamed from: h, reason: collision with root package name */
    public final ObjectAnimator f7544h;

    /* JADX INFO: renamed from: i, reason: collision with root package name */
    public final ObjectAnimator f7545i;

    /* JADX INFO: renamed from: j, reason: collision with root package name */
    public final ValueAnimator f7546j;

    /* JADX INFO: renamed from: k, reason: collision with root package name */
    public final ValueAnimator f7547k;

    /* JADX INFO: renamed from: l, reason: collision with root package name */
    public final ValueAnimator f7548l;

    /* JADX INFO: renamed from: m, reason: collision with root package name */
    public final int f7549m;

    /* JADX INFO: renamed from: n, reason: collision with root package name */
    public OnNavigationEnterExitListener f7550n;

    /* JADX INFO: renamed from: o, reason: collision with root package name */
    public OnNavigationShowHideListener f7551o;

    /* JADX INFO: renamed from: p, reason: collision with root package name */
    public OnEnlargeSelectListener f7552p;

    /* JADX INFO: renamed from: q, reason: collision with root package name */
    public OnConfigChangedListener f7553q;

    /* JADX INFO: renamed from: r, reason: collision with root package name */
    public final COUINavigationMenuView f7554r;

    /* JADX INFO: renamed from: s, reason: collision with root package name */
    public FrameLayout f7555s;
    public int t;

    /* JADX INFO: renamed from: u, reason: collision with root package name */
    public final View f7556u;

    /* JADX INFO: renamed from: v, reason: collision with root package name */
    public int f7557v;
    public final int w;

    /* JADX INFO: renamed from: x, reason: collision with root package name */
    public boolean f7558x;
    public boolean y;

    /* JADX INFO: renamed from: com.coui.appcompat.bottomnavigation.COUINavigationView$1, reason: invalid class name */
    class AnonymousClass1 implements ViewUtils.OnApplyWindowInsetsListener {
        @Override // com.google.android.material.internal.ViewUtils.OnApplyWindowInsetsListener
        @NonNull
        public final WindowInsetsCompat onApplyWindowInsets(View view, @NonNull WindowInsetsCompat windowInsetsCompat, @NonNull ViewUtils.RelativePadding relativePadding) {
            boolean z6 = ViewCompat.getLayoutDirection(view) == 1;
            int i3 = windowInsetsCompat.getInsets(WindowInsetsCompat.Type.systemBars()).left;
            int i10 = windowInsetsCompat.getInsets(WindowInsetsCompat.Type.systemBars()).right;
            relativePadding.start += z6 ? i10 : i3;
            int i11 = relativePadding.end;
            if (!z6) {
                i3 = i10;
            }
            relativePadding.end = i11 + i3;
            relativePadding.applyToView(view);
            return windowInsetsCompat;
        }
    }

    public interface OnConfigChangedListener {
        void a();
    }

    public interface OnEnlargeSelectListener {
        void a();
    }

    public interface OnNavigationEnterExitListener {
        void a();

        void b();
    }

    public interface OnNavigationShowHideListener {
        void a();

        void b();

        void c();

        void d();

        void e();

        void f();
    }

    public COUINavigationView(Context context) {
        this(context, null);
    }

    @Override // com.coui.appcompat.material.bottomnavigation.BottomNavigationView, com.coui.appcompat.material.navigation.NavigationBarView
    @NonNull
    public final NavigationBarMenuView b(@NonNull Context context) {
        int i3 = com.coui.appcompat.R.attr.COUINavigationViewItemStyle;
        int i10 = com.coui.appcompat.R.style.COUINavigationView_NoAnimation;
        TypedArray typedArrayObtainStyledAttributes = context.getTheme().obtainStyledAttributes(new int[]{i3});
        int resourceId = typedArrayObtainStyledAttributes.getResourceId(0, i10);
        typedArrayObtainStyledAttributes.recycle();
        return new COUINavigationMenuView(new ContextThemeWrapper(context, resourceId));
    }

    @Override // com.coui.appcompat.material.navigation.NavigationBarView
    public final void c(int i3) {
        if (getMenu().size() > 0) {
            getMenu().clear();
        }
        super.c(i3);
        if (this.f7549m == 0) {
            this.f7554r.setShowPressShadow(true);
        }
    }

    public final void d(Context context) {
        int dimensionPixelOffset = context.getResources().getDimensionPixelOffset(com.coui.appcompat.R.dimen.coui_navigation_item_text_size);
        int i3 = this.f7557v;
        if (i3 != 0) {
            dimensionPixelOffset = getContext().getResources().getDimensionPixelOffset(i3);
        } else if (this.t == 1) {
            dimensionPixelOffset = context.getResources().getDimensionPixelOffset(com.coui.appcompat.R.dimen.coui_navigation_item_small_text_size);
        }
        this.f7554r.setTextSize(dimensionPixelOffset);
    }

    @Override // com.coui.appcompat.poplist.PopupMenuConfigRule
    public int getBarrierDirection() {
        if (this.C == null) {
            this.C = new Rect();
        }
        getRootView().getGlobalVisibleRect(this.C);
        return this.C.height() <= getContext().getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_popup_list_window_min_window_height_to_apply_vertical_barrier) ? -1 : 3;
    }

    public COUINavigationMenuView getCOUINavigationMenuView() {
        return this.f7554r;
    }

    @Override // com.coui.appcompat.poplist.PopupMenuConfigRule
    @NonNull
    public Rect getDisplayFrame() {
        if (this.B == null) {
            this.B = new Rect();
        }
        getGlobalVisibleRect(this.B);
        return this.B;
    }

    public View getDividerView() {
        return this.f7556u;
    }

    public FrameLayout getEnlargeBgView() {
        return this.f7555s;
    }

    @Override // com.coui.appcompat.material.bottomnavigation.BottomNavigationView, com.coui.appcompat.material.navigation.NavigationBarView
    public int getMaxItemCount() {
        return 10;
    }

    @Override // com.coui.appcompat.poplist.PopupMenuConfigRule
    @NonNull
    public Rect getOutsets() {
        if (this.D == null) {
            this.D = new Rect(0, getContext().getResources().getDimensionPixelOffset(com.coui.appcompat.R.dimen.coui_popup_list_window_gap_to_navigation_view), 0, 0);
        }
        return this.D;
    }

    @Override // com.coui.appcompat.poplist.PopupMenuConfigRule
    public boolean getPopupMenuRuleEnabled() {
        return this.A;
    }

    @Override // com.coui.appcompat.poplist.PopupMenuConfigRule
    public int getType() {
        return 2;
    }

    @Override // com.coui.appcompat.material.navigation.NavigationBarView, android.view.ViewGroup, android.view.View
    public final void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.f7558x) {
            this.f7555s = new FrameLayout(getContext());
            this.f7555s.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
            addView(this.f7555s, 0);
            ViewCompat.setBackground(this.f7555s, new ColorDrawable(ContextCompat.getColor(getContext(), com.coui.appcompat.R.color.coui_navigation_enlarge_default_bg)));
        }
        d(getContext());
        this.f7554r.setItemLayoutType(this.t);
    }

    @Override // android.view.View
    public final void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        OnConfigChangedListener onConfigChangedListener = this.f7553q;
        if (onConfigChangedListener != null) {
            onConfigChangedListener.a();
        }
        d(getContext().createConfigurationContext(configuration));
    }

    @Override // com.coui.appcompat.material.bottomnavigation.BottomNavigationView, android.widget.FrameLayout, android.view.View
    public final void onMeasure(int i3, int i10) {
        int mode = View.MeasureSpec.getMode(i10);
        int size = View.MeasureSpec.getSize(i10);
        int dimensionPixelSize = getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_tool_navigation_item_height);
        if (mode == Integer.MIN_VALUE) {
            i10 = View.MeasureSpec.makeMeasureSpec(dimensionPixelSize, 1073741824);
        } else if (mode == 1073741824) {
            i10 = View.MeasureSpec.makeMeasureSpec(size, 1073741824);
        }
        super.onMeasure(i3, i10);
    }

    @Override // android.view.View
    public final boolean onTouchEvent(MotionEvent motionEvent) {
        super.onTouchEvent(motionEvent);
        return true;
    }

    public void setAnimationType(int i3) {
        if (i3 == 1) {
            this.f7544h.start();
        } else if (i3 == 2) {
            this.f7545i.start();
        }
    }

    public void setEnlargeIndex(int i3) {
        COUINavigationMenuView cOUINavigationMenuView = this.f7554r;
        cOUINavigationMenuView.O = i3;
        if (!this.f7558x || i3 < 0) {
            return;
        }
        int i10 = 0;
        while (i10 < cOUINavigationMenuView.getMenu().getVisibleItems().size()) {
            NavigationBarItemView navigationBarItemViewE = cOUINavigationMenuView.e(cOUINavigationMenuView.getMenu().getVisibleItems().get(i10).getItemId());
            if (navigationBarItemViewE instanceof COUINavigationItemView) {
                COUINavigationItemView cOUINavigationItemView = (COUINavigationItemView) navigationBarItemViewE;
                boolean z6 = i10 == cOUINavigationMenuView.O;
                cOUINavigationItemView.f7539e0 = true;
                cOUINavigationItemView.f7540f0 = z6;
            }
            i10++;
        }
    }

    public void setItemLayoutType(int i3) {
        this.t = i3;
        d(getContext());
        this.f7554r.setItemLayoutType(this.t);
    }

    @Deprecated
    public void setNeedTextAnim(boolean z6) {
    }

    public void setOnAnimatorListener(OnNavigationEnterExitListener onNavigationEnterExitListener) {
        this.f7550n = onNavigationEnterExitListener;
    }

    public void setOnAnimatorShowHideListener(OnNavigationShowHideListener onNavigationShowHideListener) {
        this.f7551o = onNavigationShowHideListener;
    }

    public void setOnConfigChangedListener(OnConfigChangedListener onConfigChangedListener) {
        this.f7553q = onConfigChangedListener;
    }

    public void setOnEnlargeSelectListener(OnEnlargeSelectListener onEnlargeSelectListener) {
        this.f7552p = onEnlargeSelectListener;
        setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() { // from class: com.coui.appcompat.bottomnavigation.COUINavigationView.2
            @Override // com.coui.appcompat.material.navigation.NavigationBarView.OnItemSelectedListener
            public final void onNavigationItemSelected(@NonNull MenuItem menuItem) {
                COUINavigationView cOUINavigationView = COUINavigationView.this;
                int i3 = 0;
                cOUINavigationView.y = cOUINavigationView.f7554r.getEnlargeId() == menuItem.getItemId();
                cOUINavigationView.f7552p.a();
                int i10 = cOUINavigationView.w;
                if (i10 == 0) {
                    return;
                }
                boolean z6 = cOUINavigationView.y;
                COUINavigationMenuView cOUINavigationMenuView = cOUINavigationView.f7554r;
                if (!z6) {
                    while (i3 < cOUINavigationMenuView.getMenu().size()) {
                        NavigationBarItemView navigationBarItemViewE = cOUINavigationMenuView.e(cOUINavigationMenuView.getMenu().getItem(i3).getItemId());
                        if (navigationBarItemViewE instanceof COUINavigationItemView) {
                            ((COUINavigationItemView) navigationBarItemViewE).V.clearColorFilter();
                        }
                        i3++;
                    }
                    cOUINavigationMenuView.setItemTextColor(cOUINavigationView.getItemTextColor());
                    cOUINavigationView.f7555s.setBackgroundColor(cOUINavigationView.getResources().getColor(com.coui.appcompat.R.color.coui_navigation_enlarge_default_bg));
                    return;
                }
                while (i3 < cOUINavigationMenuView.getMenu().size()) {
                    NavigationBarItemView navigationBarItemViewE2 = cOUINavigationMenuView.e(cOUINavigationMenuView.getMenu().getItem(i3).getItemId());
                    if (navigationBarItemViewE2 instanceof COUINavigationItemView) {
                        navigationBarItemViewE2.setTextColor(ColorStateList.valueOf(ContextCompat.getColor(cOUINavigationMenuView.getContext(), com.coui.appcompat.R.color.coui_navigation_enlarge_item_color)));
                        COUINavigationItemView cOUINavigationItemView = (COUINavigationItemView) navigationBarItemViewE2;
                        if (!cOUINavigationItemView.f7540f0) {
                            cOUINavigationItemView.V.setColorFilter(-1);
                        }
                    }
                    i3++;
                }
                cOUINavigationView.f7555s.setBackgroundResource(i10);
            }
        });
    }

    public void setPopupMenuRuleEnabled(boolean z6) {
        this.A = z6;
    }

    public COUINavigationView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, com.coui.appcompat.R.attr.couiNavigationViewStyle);
    }

    /* JADX WARN: Illegal instructions before constructor call */
    public COUINavigationView(Context context, AttributeSet attributeSet, int i3) {
        super(context, attributeSet, i3, com.coui.appcompat.R.style.Widget_COUI_COUINavigationView);
        COUINavigationItemView cOUINavigationItemView;
        int i10 = com.coui.appcompat.R.style.Widget_COUI_COUINavigationView;
        this.f7557v = 0;
        this.f7558x = false;
        this.y = false;
        this.A = true;
        this.B = null;
        this.C = null;
        this.D = null;
        TintTypedArray tintTypedArrayObtainStyledAttributes = TintTypedArray.obtainStyledAttributes(context, attributeSet, com.coui.appcompat.R.styleable.COUINavigationMenuView, i3, i10);
        COUINavigationMenuView cOUINavigationMenuView = (COUINavigationMenuView) getMenuView();
        this.f7554r = cOUINavigationMenuView;
        if (tintTypedArrayObtainStyledAttributes.hasValue(com.coui.appcompat.R.styleable.COUINavigationMenuView_couiNaviTextColor)) {
            setItemTextColor(tintTypedArrayObtainStyledAttributes.getColorStateList(com.coui.appcompat.R.styleable.COUINavigationMenuView_couiNaviTextColor));
        } else {
            setItemTextColor(getResources().getColorStateList(com.coui.appcompat.R.color.coui_bottom_tool_navigation_item_selector));
        }
        cOUINavigationMenuView.setIconTintList(tintTypedArrayObtainStyledAttributes.getColorStateList(com.coui.appcompat.R.styleable.COUINavigationMenuView_couiNaviIconTint));
        this.f7549m = tintTypedArrayObtainStyledAttributes.getInt(com.coui.appcompat.R.styleable.COUINavigationMenuView_navigationType, 0);
        int dimensionPixelSize = tintTypedArrayObtainStyledAttributes.getDimensionPixelSize(com.coui.appcompat.R.styleable.COUINavigationMenuView_couiNaviTextSize, getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_navigation_item_text_size));
        this.f7557v = tintTypedArrayObtainStyledAttributes.getResourceId(com.coui.appcompat.R.styleable.COUINavigationMenuView_couiNaviTextSize, 0);
        cOUINavigationMenuView.setTextSize((int) COUIChangeTextUtil.d(dimensionPixelSize, getResources().getConfiguration().fontScale, 2));
        int integer = tintTypedArrayObtainStyledAttributes.getInteger(com.coui.appcompat.R.styleable.COUINavigationMenuView_couiNaviTipsType, -1);
        int integer2 = tintTypedArrayObtainStyledAttributes.getInteger(com.coui.appcompat.R.styleable.COUINavigationMenuView_couiNaviTipsNumber, 0);
        if (tintTypedArrayObtainStyledAttributes.hasValue(com.coui.appcompat.R.styleable.COUINavigationMenuView_couiNaviMenu)) {
            c(tintTypedArrayObtainStyledAttributes.getResourceId(com.coui.appcompat.R.styleable.COUINavigationMenuView_couiNaviMenu, 0));
            if (cOUINavigationMenuView.getVisibleItems().size() > 0) {
                String strValueOf = String.valueOf(integer2);
                if (cOUINavigationMenuView.getVisibleItems().size() > 0 && (cOUINavigationItemView = (COUINavigationItemView) cOUINavigationMenuView.e(getCOUINavigationMenuView().getMenu().getVisibleItems().get(0).getItemId())) != null) {
                    if (integer == 1) {
                        cOUINavigationItemView.getCOUIHintRedDot().setVisibility(0);
                        cOUINavigationItemView.getCOUIHintRedDot().setPointMode(1);
                    } else if (integer != 2) {
                        cOUINavigationItemView.getCOUIHintRedDot().setVisibility(4);
                    } else {
                        cOUINavigationItemView.getCOUIHintRedDot().setVisibility(0);
                        try {
                            Integer.parseInt(strValueOf);
                            cOUINavigationItemView.getCOUIHintRedDot().setPointMode(2);
                            cOUINavigationItemView.getCOUIHintRedDot().setPointNumber(Integer.parseInt(strValueOf));
                        } catch (NumberFormatException unused) {
                            cOUINavigationItemView.getCOUIHintRedDot().setPointMode(3);
                            cOUINavigationItemView.getCOUIHintRedDot().setPointText(strValueOf);
                        }
                    }
                }
            }
        }
        int resourceId = tintTypedArrayObtainStyledAttributes.getResourceId(com.coui.appcompat.R.styleable.COUINavigationMenuView_couiToolNavigationViewBg, 0);
        int resourceId2 = tintTypedArrayObtainStyledAttributes.getResourceId(com.coui.appcompat.R.styleable.COUINavigationMenuView_couiTabNavigationViewBg, 0);
        this.w = tintTypedArrayObtainStyledAttributes.getResourceId(com.coui.appcompat.R.styleable.COUINavigationMenuView_couiEnlargeNavigationViewBg, 0);
        int i11 = this.f7549m;
        if (i11 == 2) {
            this.f7558x = true;
            setBackgroundColor(0);
            COUINavigationMenuView cOUINavigationMenuView2 = this.f7554r;
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) cOUINavigationMenuView2.getLayoutParams();
            layoutParams.gravity = 81;
            cOUINavigationMenuView2.setLayoutParams(layoutParams);
        } else if (i11 == 0) {
            setBackgroundResource(resourceId);
        } else {
            setBackgroundResource(resourceId2);
        }
        this.f7554r.setItemNavigationType(this.f7549m);
        if (tintTypedArrayObtainStyledAttributes.hasValue(com.coui.appcompat.R.styleable.COUINavigationMenuView_couiItemLayoutType)) {
            setItemLayoutType(tintTypedArrayObtainStyledAttributes.getInteger(com.coui.appcompat.R.styleable.COUINavigationMenuView_couiItemLayoutType, 0));
        }
        setLabelVisibilityMode(1);
        // Material NavigationBarView may enable the M3 pill when a theme supplies
        // itemActiveIndicatorStyle; OPPO ColorOS tabs are labeled icons without it.
        setItemActiveIndicatorEnabled(false);
        setItemActiveIndicatorWidth(0);
        setItemActiveIndicatorHeight(0);
        setClipChildren(false);
        setClipToPadding(false);
        View view = new View(context);
        this.f7556u = view;
        view.setForceDarkAllowed(false);
        this.f7556u.setBackgroundColor(COUIContextUtil.a(context, com.coui.appcompat.R.attr.couiColorDivider));
        this.f7556u.setLayoutParams(new FrameLayout.LayoutParams(-1, getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_navigation_shadow_height)));
        if (this.f7558x) {
            addView(this.f7556u, 0);
        } else {
            addView(this.f7556u);
            this.f7554r.setTop(0);
        }
        setElevation(0.0f);
        tintTypedArrayObtainStyledAttributes.recycle();
        AnimatorSet animatorSet = new AnimatorSet();
        Property property = View.ALPHA;
        COUINavigationMenuView cOUINavigationMenuView3 = this.f7554r;
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(cOUINavigationMenuView3, (Property<COUINavigationMenuView, Float>) property, 0.0f, 1.0f);
        this.f7544h = objectAnimatorOfFloat;
        objectAnimatorOfFloat.setInterpolator(new COUIInEaseInterpolator());
        this.f7544h.setDuration(100L);
        this.f7544h.addListener(new Animator.AnimatorListener() { // from class: com.coui.appcompat.bottomnavigation.COUINavigationView.3
            @Override // android.animation.Animator.AnimatorListener
            public final void onAnimationCancel(Animator animator) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public final void onAnimationEnd(Animator animator) {
                OnNavigationEnterExitListener onNavigationEnterExitListener = COUINavigationView.this.f7550n;
                if (onNavigationEnterExitListener != null) {
                    onNavigationEnterExitListener.b();
                }
            }

            @Override // android.animation.Animator.AnimatorListener
            public final void onAnimationRepeat(Animator animator) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public final void onAnimationStart(Animator animator) {
                COUINavigationView.this.getClass();
            }
        });
        ObjectAnimator objectAnimatorOfFloat2 = ObjectAnimator.ofFloat(cOUINavigationMenuView3, (Property<COUINavigationMenuView, Float>) property, 1.0f, 0.0f);
        this.f7545i = objectAnimatorOfFloat2;
        objectAnimatorOfFloat2.setInterpolator(new COUILinearInterpolator());
        this.f7545i.setDuration(100L);
        this.f7545i.addListener(new Animator.AnimatorListener() { // from class: com.coui.appcompat.bottomnavigation.COUINavigationView.4
            @Override // android.animation.Animator.AnimatorListener
            public final void onAnimationCancel(Animator animator) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public final void onAnimationEnd(Animator animator) {
                COUINavigationView cOUINavigationView = COUINavigationView.this;
                cOUINavigationView.getClass();
                OnNavigationEnterExitListener onNavigationEnterExitListener = cOUINavigationView.f7550n;
                if (onNavigationEnterExitListener != null) {
                    onNavigationEnterExitListener.a();
                }
            }

            @Override // android.animation.Animator.AnimatorListener
            public final void onAnimationRepeat(Animator animator) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public final void onAnimationStart(Animator animator) {
            }
        });
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(1.0f, 0.0f);
        this.f7546j = valueAnimatorOfFloat;
        valueAnimatorOfFloat.setInterpolator(new COUIInEaseInterpolator());
        this.f7546j.setDuration(350L);
        this.f7546j.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.coui.appcompat.bottomnavigation.COUINavigationView.5
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                COUINavigationView cOUINavigationView = COUINavigationView.this;
                cOUINavigationView.f7554r.setTranslationY(((Float) valueAnimator.getAnimatedValue()).floatValue() * cOUINavigationView.getMeasuredHeight());
            }
        });
        animatorSet.playTogether(this.f7544h, this.f7546j);
        ValueAnimator valueAnimatorOfFloat2 = ValueAnimator.ofFloat(0.0f, 1.0f);
        this.f7548l = valueAnimatorOfFloat2;
        valueAnimatorOfFloat2.setInterpolator(new COUIOutEaseInterpolator());
        this.f7548l.setDuration(200L);
        this.f7548l.addListener(new Animator.AnimatorListener() { // from class: com.coui.appcompat.bottomnavigation.COUINavigationView.6
            @Override // android.animation.Animator.AnimatorListener
            public final void onAnimationCancel(Animator animator) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public final void onAnimationEnd(Animator animator) {
                OnNavigationShowHideListener onNavigationShowHideListener = COUINavigationView.this.f7551o;
                if (onNavigationShowHideListener != null) {
                    onNavigationShowHideListener.c();
                }
            }

            @Override // android.animation.Animator.AnimatorListener
            public final void onAnimationRepeat(Animator animator) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public final void onAnimationStart(Animator animator) {
                OnNavigationShowHideListener onNavigationShowHideListener = COUINavigationView.this.f7551o;
                if (onNavigationShowHideListener != null) {
                    onNavigationShowHideListener.f();
                }
            }
        });
        this.f7548l.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.coui.appcompat.bottomnavigation.COUINavigationView.7
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                COUINavigationView cOUINavigationView = COUINavigationView.this;
                ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) cOUINavigationView.getLayoutParams();
                marginLayoutParams.bottomMargin = (int) (((Float) valueAnimator.getAnimatedValue()).floatValue() * cOUINavigationView.getMeasuredHeight() * (-1.0f));
                cOUINavigationView.setLayoutParams(marginLayoutParams);
                OnNavigationShowHideListener onNavigationShowHideListener = cOUINavigationView.f7551o;
                if (onNavigationShowHideListener != null) {
                    onNavigationShowHideListener.d();
                }
            }
        });
        ValueAnimator valueAnimatorOfFloat3 = ValueAnimator.ofFloat(1.0f, 0.0f);
        this.f7547k = valueAnimatorOfFloat3;
        valueAnimatorOfFloat3.setInterpolator(new COUIInEaseInterpolator());
        this.f7547k.setDuration(250L);
        this.f7547k.addListener(new Animator.AnimatorListener() { // from class: com.coui.appcompat.bottomnavigation.COUINavigationView.8
            @Override // android.animation.Animator.AnimatorListener
            public final void onAnimationCancel(Animator animator) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public final void onAnimationEnd(Animator animator) {
                OnNavigationShowHideListener onNavigationShowHideListener = COUINavigationView.this.f7551o;
                if (onNavigationShowHideListener != null) {
                    onNavigationShowHideListener.e();
                }
            }

            @Override // android.animation.Animator.AnimatorListener
            public final void onAnimationRepeat(Animator animator) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public final void onAnimationStart(Animator animator) {
                OnNavigationShowHideListener onNavigationShowHideListener = COUINavigationView.this.f7551o;
                if (onNavigationShowHideListener != null) {
                    onNavigationShowHideListener.a();
                }
            }
        });
        this.f7547k.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.coui.appcompat.bottomnavigation.COUINavigationView.9
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                COUINavigationView cOUINavigationView = COUINavigationView.this;
                ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) cOUINavigationView.getLayoutParams();
                marginLayoutParams.bottomMargin = (int) (((Float) valueAnimator.getAnimatedValue()).floatValue() * cOUINavigationView.getMeasuredHeight() * (-1.0f));
                cOUINavigationView.setLayoutParams(marginLayoutParams);
                OnNavigationShowHideListener onNavigationShowHideListener = cOUINavigationView.f7551o;
                if (onNavigationShowHideListener != null) {
                    onNavigationShowHideListener.b();
                }
            }
        });
        ViewUtils.doOnApplyWindowInsets(this, new AnonymousClass1());
        setForceDarkAllowed(false);
    }
}
