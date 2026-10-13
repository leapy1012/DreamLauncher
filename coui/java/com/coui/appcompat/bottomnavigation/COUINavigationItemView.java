package com.coui.appcompat.bottomnavigation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.DimenRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuItemImpl;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.coui.appcompat.material.bottomnavigation.BottomNavigationItemView;
import com.coui.appcompat.reddot.COUIHintRedDot;
import com.coui.appcompat.state.COUIMaskEffectDrawable;
import com.coui.appcompat.state.COUIStateEffectDrawable;
import com.coui.appcompat.uiutil.UIUtil;
import com.google.android.material.R;

/* JADX INFO: loaded from: classes4.dex */
@SuppressLint({"RestrictedApi"})
public class COUINavigationItemView extends BottomNavigationItemView {
    public final RectF G;
    public final int H;
    public final int I;
    public final int J;
    public final int K;
    public final int L;
    public final int M;
    public final int N;
    public final int O;
    public int P;
    public final int Q;
    public final int[] mLocation;
    public int S;
    public final TextView T;
    public final TextView U;
    public final ImageView V;
    public final FrameLayout W;

    /* JADX INFO: renamed from: a0, reason: collision with root package name */
    public final FrameLayout f7535a0;

    /* JADX INFO: renamed from: b0, reason: collision with root package name */
    public final COUIHintRedDot f7536b0;

    /* JADX INFO: renamed from: c0, reason: collision with root package name */
    public Rect f7537c0;

    /* JADX INFO: renamed from: d0, reason: collision with root package name */
    public int f7538d0;

    /* JADX INFO: renamed from: e0, reason: collision with root package name */
    public boolean f7539e0;

    /* JADX INFO: renamed from: f0, reason: collision with root package name */
    public boolean f7540f0;
    public boolean g0;
    public boolean h0;
    public final COUIMaskEffectDrawable i0;

    /* JADX INFO: renamed from: j0, reason: collision with root package name */
    public final COUIStateEffectDrawable f7541j0;

    /* JADX INFO: renamed from: k0, reason: collision with root package name */
    public int f7542k0;

    /* JADX INFO: renamed from: l0, reason: collision with root package name */
    public AccessibilityManager f7543l0;

    public COUINavigationItemView(@NonNull Context context) {
        super(context);
        RectF rectF = new RectF();
        this.G = rectF;
        this.H = -2;
        this.I = 1;
        this.J = getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_navigation_enlarge_icon_size);
        this.K = getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_navigation_normal_icon_size);
        this.L = getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_navigation_enlarge_item_height);
        this.M = getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_navigation_normal_item_height);
        this.N = getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_navigation_enlarge_icon_margin_top);
        this.O = getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_navigation_red_dot_with_number_vertical_offset);
        this.P = 0;
        this.Q = getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_navigation_red_dot_offset);
        this.mLocation = new int[2];
        this.f7539e0 = false;
        this.f7540f0 = false;
        this.g0 = false;
        this.h0 = false;
        this.f7543l0 = null;
        this.T = (TextView) findViewById(R.id.navigation_bar_item_small_label_view);
        this.U = (TextView) findViewById(R.id.navigation_bar_item_large_label_view);
        this.V = (ImageView) findViewById(R.id.navigation_bar_item_icon_view);
        this.W = (FrameLayout) findViewById(R.id.navigation_bar_item_icon_container);
        this.f7535a0 = (FrameLayout) findViewById(com.coui.appcompat.R.id.fl_root);
        this.f7536b0 = (COUIHintRedDot) findViewById(com.coui.appcompat.R.id.red_dot);
        setTextSize(context.getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_navigation_item_text_size));
        setClipChildren(false);
        setClipToPadding(false);
        setHapticFeedbackEnabled(false);
        this.P = context.getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_navigation_red_dot_with_number_horizontal_offset);
        setDefaultFocusHighlightEnabled(false);
        float dimension = getContext().getResources().getDimension(com.coui.appcompat.R.dimen.coui_navigation_item_background_radius);
        COUIMaskEffectDrawable cOUIMaskEffectDrawable = new COUIMaskEffectDrawable(getContext(), 1);
        this.i0 = cOUIMaskEffectDrawable;
        cOUIMaskEffectDrawable.l(rectF, dimension, dimension);
        COUIMaskEffectDrawable cOUIMaskEffectDrawable2 = this.i0;
        cOUIMaskEffectDrawable2.setIsRoundStyle(false);
        cOUIMaskEffectDrawable2.setMinProgressForTouchEnterAnimation(0.0f);
        COUIStateEffectDrawable cOUIStateEffectDrawable = new COUIStateEffectDrawable(new Drawable[]{getBackground() == null ? new ColorDrawable(0) : getBackground(), this.i0});
        this.f7541j0 = cOUIStateEffectDrawable;
        super.setBackground(cOUIStateEffectDrawable);
        this.f7543l0 = (AccessibilityManager) context.getSystemService("accessibility");
    }

    @Override // android.view.ViewGroup, android.view.View
    public final boolean dispatchTouchEvent(MotionEvent motionEvent) {
        if (this.g0) {
            if (motionEvent.getActionMasked() == 0) {
                this.f7541j0.e(true);
            }
            if (motionEvent.getActionMasked() == 1 || motionEvent.getActionMasked() == 3) {
                this.f7541j0.e(false);
            }
        }
        return super.dispatchTouchEvent(motionEvent);
    }

    public final void g(boolean z6) {
        if (this.f7540f0) {
            setIconSize(z6 ? this.J : this.K);
            this.T.setVisibility(z6 ? 8 : 0);
            if (this.h0) {
                return;
            }
            FrameLayout frameLayout = this.W;
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) frameLayout.getLayoutParams();
            layoutParams.setMargins(0, z6 ? 0 : this.N, 0, 0);
            frameLayout.setLayoutParams(layoutParams);
        }
    }

    public COUIHintRedDot getCOUIHintRedDot() {
        return this.f7536b0;
    }

    @Override // com.coui.appcompat.material.bottomnavigation.BottomNavigationItemView, com.coui.appcompat.material.navigation.NavigationBarItemView
    @DimenRes
    public int getItemDefaultMarginResId() {
        return com.coui.appcompat.R.dimen.coui_navigation_enlarge_default_margin;
    }

    @Override // com.coui.appcompat.material.bottomnavigation.BottomNavigationItemView, com.coui.appcompat.material.navigation.NavigationBarItemView
    @LayoutRes
    public int getItemLayoutResId() {
        return com.coui.appcompat.R.layout.coui_navigation_item_layout;
    }

    @Override // com.coui.appcompat.material.navigation.NavigationBarItemView, androidx.appcompat.view.menu.MenuView.ItemView
    public final void initialize(@NonNull MenuItemImpl menuItemImpl, int i3) {
        super.initialize(menuItemImpl, i3);
        TooltipCompat.setTooltipText(this, "");
    }

    @Override // android.view.ViewGroup, android.view.View
    public final void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.f7539e0) {
            FrameLayout frameLayout = this.f7535a0;
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) frameLayout.getLayoutParams();
            layoutParams.gravity = 80;
            if (this.f7540f0) {
                layoutParams.height = this.L;
                setIconSize(this.K);
                setIconTintList(null);
            } else {
                layoutParams.height = this.M;
            }
            frameLayout.setLayoutParams(layoutParams);
            g(isSelected());
        }
    }

    @Override // android.view.ViewGroup, android.view.View
    public final void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        COUIHintRedDot cOUIHintRedDot = this.f7536b0;
        cOUIHintRedDot.setPointMode(0);
        cOUIHintRedDot.setPointText("");
        cOUIHintRedDot.setVisibility(4);
    }

    @Override // android.view.View
    public final boolean onHoverEvent(MotionEvent motionEvent) {
        boolean zOnHoverEvent = super.onHoverEvent(motionEvent);
        if (this.f7543l0.isTouchExplorationEnabled()) {
            return true;
        }
        return zOnHoverEvent;
    }

    @Override // com.coui.appcompat.material.navigation.NavigationBarItemView, android.view.View
    public final void onInitializeAccessibilityNodeInfo(@NonNull AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        if (this.f7542k0 == 0) {
            AccessibilityNodeInfoCompat accessibilityNodeInfoCompatWrap = AccessibilityNodeInfoCompat.wrap(accessibilityNodeInfo);
            accessibilityNodeInfoCompatWrap.setCollectionItemInfo(null);
            accessibilityNodeInfoCompatWrap.setRoleDescription(null);
            accessibilityNodeInfo.setClassName("android.widget.Button");
            accessibilityNodeInfo.setSelected(false);
            accessibilityNodeInfoCompatWrap.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK);
        }
    }

    @Override // android.widget.FrameLayout, android.view.ViewGroup, android.view.View
    public final void onLayout(boolean z6, int i3, int i10, int i11, int i12) {
        int measuredWidth;
        int measuredWidth2;
        super.onLayout(z6, i3, i10, i11, i12);
        boolean z10 = getContext().getResources().getBoolean(com.coui.appcompat.R.bool.is_big_layout);
        int i13 = this.S;
        FrameLayout frameLayout = this.f7535a0;
        if (i13 == 1 || !z10) {
            View childAt = frameLayout.getChildAt(0);
            View childAt2 = frameLayout.getChildAt(1);
            int dimensionPixelSize = (this.f7540f0 && isSelected()) ? 0 : getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_navigation_icon_margin_top);
            childAt.layout((frameLayout.getMeasuredWidth() / 2) - (childAt.getMeasuredWidth() / 2), dimensionPixelSize, (childAt.getMeasuredWidth() / 2) + (frameLayout.getMeasuredWidth() / 2), childAt.getMeasuredHeight() + dimensionPixelSize);
            childAt2.layout((frameLayout.getMeasuredWidth() / 2) - (childAt2.getMeasuredWidth() / 2), getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_navigation_text_margin_top) + ((frameLayout.getMeasuredHeight() - childAt2.getMeasuredHeight()) - getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_navigation_icon_margin_top)), (childAt2.getMeasuredWidth() / 2) + (frameLayout.getMeasuredWidth() / 2), getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_navigation_text_margin_top) + (frameLayout.getMeasuredHeight() - getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_navigation_icon_margin_top)));
            if (this.f7540f0) {
                childAt2.setVisibility(0);
            }
            this.h0 = false;
        } else if (getLayoutDirection() == 1) {
            View childAt3 = frameLayout.getChildAt(0);
            View childAt4 = frameLayout.getChildAt(1);
            int dimensionPixelSize2 = getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_navigation_enlarge_icon_horizontal_margin);
            int measuredWidth3 = ((frameLayout.getMeasuredWidth() - childAt3.getMeasuredWidth()) - childAt4.getMeasuredWidth()) - dimensionPixelSize2;
            int i14 = measuredWidth3 > 0 ? measuredWidth3 / 2 : 0;
            if (this.f7540f0) {
                childAt3.layout((frameLayout.getMeasuredWidth() / 2) - (childAt3.getMeasuredWidth() / 2), (frameLayout.getMeasuredHeight() / 2) - (childAt3.getMeasuredHeight() / 2), (childAt3.getMeasuredWidth() / 2) + (frameLayout.getMeasuredWidth() / 2), (childAt3.getMeasuredHeight() / 2) + (frameLayout.getMeasuredHeight() / 2));
            } else {
                int measuredWidth4 = frameLayout.getMeasuredWidth() - i14;
                childAt3.layout(measuredWidth4 - childAt3.getMeasuredWidth(), (frameLayout.getMeasuredHeight() / 2) - (childAt3.getMeasuredHeight() / 2), measuredWidth4, (childAt3.getMeasuredHeight() / 2) + (frameLayout.getMeasuredHeight() / 2));
            }
            int left = childAt3.getLeft() - dimensionPixelSize2;
            int measuredHeight = (frameLayout.getMeasuredHeight() / 2) - (childAt4.getMeasuredHeight() / 2);
            int measuredHeight2 = (childAt4.getMeasuredHeight() / 2) + (frameLayout.getMeasuredHeight() / 2);
            childAt4.measure(View.MeasureSpec.makeMeasureSpec(left - i14, 1073741824), View.MeasureSpec.makeMeasureSpec(childAt4.getMeasuredHeight(), 1073741824));
            childAt4.layout(i14, measuredHeight, left, measuredHeight2);
            if (this.f7540f0) {
                childAt4.setVisibility(8);
            }
            this.h0 = true;
        } else {
            View childAt5 = frameLayout.getChildAt(0);
            View childAt6 = frameLayout.getChildAt(1);
            int dimensionPixelSize3 = getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_navigation_enlarge_icon_horizontal_margin);
            int measuredWidth5 = ((frameLayout.getMeasuredWidth() - childAt5.getMeasuredWidth()) - childAt6.getMeasuredWidth()) - dimensionPixelSize3;
            int i15 = measuredWidth5 > 0 ? measuredWidth5 / 2 : 0;
            if (this.f7540f0) {
                measuredWidth2 = (frameLayout.getMeasuredWidth() / 2) - (childAt5.getMeasuredWidth() / 2);
                measuredWidth = (childAt5.getMeasuredWidth() / 2) + (frameLayout.getMeasuredWidth() / 2);
            } else {
                measuredWidth = childAt5.getMeasuredWidth() + i15;
                measuredWidth2 = i15;
            }
            childAt5.layout(measuredWidth2, (frameLayout.getMeasuredHeight() / 2) - (childAt5.getMeasuredHeight() / 2), measuredWidth, (childAt5.getMeasuredHeight() / 2) + (frameLayout.getMeasuredHeight() / 2));
            int right = childAt5.getRight() + dimensionPixelSize3;
            int measuredWidth6 = frameLayout.getMeasuredWidth() - i15;
            int measuredHeight3 = (frameLayout.getMeasuredHeight() / 2) - (childAt6.getMeasuredHeight() / 2);
            int measuredHeight4 = (childAt6.getMeasuredHeight() / 2) + (frameLayout.getMeasuredHeight() / 2);
            childAt6.measure(View.MeasureSpec.makeMeasureSpec(measuredWidth6 - right, 1073741824), View.MeasureSpec.makeMeasureSpec(childAt6.getMeasuredHeight(), 1073741824));
            childAt6.layout(right, measuredHeight3, measuredWidth6, measuredHeight4);
            if (this.f7540f0) {
                childAt6.setVisibility(8);
            }
            this.h0 = true;
        }
        COUIHintRedDot cOUIHintRedDot = this.f7536b0;
        if (cOUIHintRedDot.getVisibility() == 8) {
            return;
        }
        if (this.f7537c0 == null) {
            this.f7537c0 = new Rect();
        }
        int pointMode = cOUIHintRedDot.getPointMode();
        int[] iArr = this.mLocation;
        if (pointMode == 1) {
            int i16 = this.Q;
            iArr[1] = i16;
            iArr[0] = i16;
        } else {
            iArr[1] = this.O;
            iArr[0] = this.P;
            if (cOUIHintRedDot.getPointNumber() >= 100 && cOUIHintRedDot.getPointNumber() < 1000) {
                iArr[0] = UIUtil.d(getContext(), this.I) + iArr[0];
            } else if (cOUIHintRedDot.getPointNumber() > 0 && cOUIHintRedDot.getPointNumber() < 10) {
                iArr[0] = UIUtil.d(getContext(), this.H) + iArr[0];
            }
        }
        int layoutDirection = ViewCompat.getLayoutDirection(this);
        FrameLayout frameLayout2 = this.W;
        if (layoutDirection == 1) {
            this.f7537c0.set(frameLayout2.getLeft(), frameLayout2.getTop(), cOUIHintRedDot.getMeasuredWidth() + frameLayout2.getLeft(), cOUIHintRedDot.getMeasuredHeight() + frameLayout2.getTop());
            this.f7537c0.offset(-iArr[0], -iArr[1]);
        } else {
            this.f7537c0.set(frameLayout2.getRight() - cOUIHintRedDot.getMeasuredWidth(), frameLayout2.getTop(), frameLayout2.getRight(), cOUIHintRedDot.getMeasuredHeight() + frameLayout2.getTop());
            this.f7537c0.offset(iArr[0], -iArr[1]);
        }
        Rect rect = this.f7537c0;
        cOUIHintRedDot.layout(rect.left, rect.top, rect.right, rect.bottom);
    }

    @Override // com.coui.appcompat.material.navigation.NavigationBarItemView, android.view.View
    public final void onSizeChanged(int i3, int i10, int i11, int i12) {
        super.onSizeChanged(i3, i10, i11, i12);
        this.G.set(0.0f, 0.0f, i3, i10);
    }

    @Override // android.view.View
    public void setBackground(Drawable drawable) {
        COUIStateEffectDrawable cOUIStateEffectDrawable = this.f7541j0;
        if (cOUIStateEffectDrawable == null) {
            super.setBackground(drawable);
        } else if (drawable == null) {
            cOUIStateEffectDrawable.f(new ColorDrawable(0));
        } else {
            cOUIStateEffectDrawable.f(drawable);
        }
    }

    @Override // com.coui.appcompat.material.navigation.NavigationBarItemView, androidx.appcompat.view.menu.MenuView.ItemView
    public void setChecked(boolean z6) {
        g(z6);
        setSelected(z6);
    }

    public void setNavigationType(int i3) {
        if (this.f7542k0 != i3) {
            this.f7542k0 = i3;
        }
    }

    public void setShowPressShadow(boolean z6) {
        this.g0 = z6;
    }

    public void setTextSize(int i3) {
        this.f7538d0 = i3;
        this.T.setTextSize(0, i3);
        this.U.setTextSize(0, this.f7538d0);
        requestLayout();
    }
}
