package com.coui.appcompat.navigationrail;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.DimenRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuItemImpl;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.view.ViewCompat;
import com.coui.appcompat.material.navigation.NavigationBarItemView;
import com.coui.appcompat.reddot.COUIHintRedDot;
import com.coui.appcompat.state.COUIMaskEffectDrawable;
import com.coui.appcompat.state.COUIStateEffectDrawable;
import com.coui.appcompat.uiutil.UIUtil;
import com.google.android.material.R;

/* JADX INFO: loaded from: classes4.dex */
@SuppressLint({"RestrictedApi"})
public class COUINavigationRailItemView extends NavigationBarItemView {
    public final TextView G;
    public final TextView H;
    public final RectF I;
    public int J;
    public final COUIStateEffectDrawable K;
    public final COUIHintRedDot L;
    public final FrameLayout M;
    public final FrameLayout N;
    public final FrameLayout O;
    public Rect P;
    public final int Q;
    public final int R;
    public final int S;
    public final int T;
    public final int U;
    public final int[] V;
    public final int W;

    /* JADX INFO: renamed from: a0, reason: collision with root package name */
    public final int f8605a0;

    /* JADX INFO: renamed from: b0, reason: collision with root package name */
    public ViewGroup.MarginLayoutParams f8606b0;

    public COUINavigationRailItemView(@NonNull Context context) {
        super(context);
        RectF rectF = new RectF();
        this.I = rectF;
        this.Q = -2;
        this.R = 1;
        this.S = getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_side_navigation_red_dot_with_number_vertical_offset);
        this.T = getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_side_navigation_red_dot_with_number_horizontal_offset);
        this.U = getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_side_navigation_red_dot_offset);
        this.V = new int[2];
        this.G = (TextView) findViewById(R.id.navigation_bar_item_small_label_view);
        this.H = (TextView) findViewById(R.id.navigation_bar_item_large_label_view);
        this.L = (COUIHintRedDot) findViewById(com.coui.appcompat.R.id.red_dot);
        this.M = (FrameLayout) findViewById(R.id.navigation_bar_item_icon_container);
        this.N = (FrameLayout) findViewById(com.coui.appcompat.R.id.navigation_bar_item_labels_group_self);
        this.O = (FrameLayout) findViewById(com.coui.appcompat.R.id.fl_root_rail);
        this.W = getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_navigation_margin_vertical);
        this.f8605a0 = getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_navigation_rail_text_margin_top);
        setClipChildren(false);
        setClipToPadding(false);
        setTextSize(context.getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_side_navigation_rail_item_text_size));
        setDefaultFocusHighlightEnabled(false);
        float dimension = getContext().getResources().getDimension(com.coui.appcompat.R.dimen.coui_side_navigation_item_background_radius);
        COUIMaskEffectDrawable cOUIMaskEffectDrawable = new COUIMaskEffectDrawable(getContext(), 1);
        cOUIMaskEffectDrawable.setMaskRect(rectF, dimension, dimension);
        cOUIMaskEffectDrawable.setIsRoundStyle(false);
        cOUIMaskEffectDrawable.setMinProgressForTouchEnterAnimation(0.0f);
        this.K = new COUIStateEffectDrawable(new Drawable[]{getBackground() == null ? new ColorDrawable(0) : getBackground(), cOUIMaskEffectDrawable});
        setForceDarkAllowed(false);
        super.setBackground(this.K);
    }

    public COUIHintRedDot getCOUIHintRedDot() {
        return this.L;
    }

    @Override // com.coui.appcompat.material.navigation.NavigationBarItemView
    @DimenRes
    public int getItemDefaultMarginResId() {
        return com.coui.appcompat.R.dimen.coui_navigation_rail_item_icon_margin;
    }

    @Override // com.coui.appcompat.material.navigation.NavigationBarItemView
    @LayoutRes
    public int getItemLayoutResId() {
        return com.coui.appcompat.R.layout.coui_navigation_rail_item_layout;
    }

    public int getRootViewHeight() {
        int measuredHeight = this.M.getMeasuredHeight();
        int i3 = this.W;
        return this.N.getMeasuredHeight() + measuredHeight + i3 + i3 + this.f8605a0;
    }

    @Override // com.coui.appcompat.material.navigation.NavigationBarItemView, androidx.appcompat.view.menu.MenuView.ItemView
    public final void initialize(@NonNull MenuItemImpl menuItemImpl, int i3) {
        super.initialize(menuItemImpl, i3);
        TooltipCompat.setTooltipText(this, "");
    }

    @Override // android.view.ViewGroup, android.view.View
    public final void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        COUIHintRedDot cOUIHintRedDot = this.L;
        cOUIHintRedDot.setPointMode(0);
        cOUIHintRedDot.setPointText("");
        cOUIHintRedDot.setVisibility(4);
    }

    @Override // android.widget.FrameLayout, android.view.ViewGroup, android.view.View
    public final void onLayout(boolean z6, int i3, int i10, int i11, int i12) {
        super.onLayout(z6, i3, i10, i11, i12);
        FrameLayout frameLayout = this.O;
        int measuredWidth = frameLayout.getMeasuredWidth() / 2;
        FrameLayout frameLayout2 = this.M;
        int measuredWidth2 = measuredWidth - (frameLayout2.getMeasuredWidth() / 2);
        int measuredWidth3 = frameLayout2.getMeasuredWidth() + measuredWidth2;
        int paddingTop = getPaddingTop() + this.W;
        frameLayout2.layout(measuredWidth2, paddingTop, measuredWidth3, frameLayout2.getMeasuredHeight() + paddingTop);
        int measuredWidth4 = frameLayout.getMeasuredWidth() / 2;
        FrameLayout frameLayout3 = this.N;
        int measuredWidth5 = measuredWidth4 - (frameLayout3.getMeasuredWidth() / 2);
        int measuredWidth6 = frameLayout3.getMeasuredWidth() + measuredWidth5;
        int bottom = frameLayout2.getBottom() + this.f8605a0;
        frameLayout3.layout(measuredWidth5, bottom, measuredWidth6, frameLayout3.getMeasuredHeight() + bottom);
        COUIHintRedDot cOUIHintRedDot = this.L;
        if (cOUIHintRedDot.getVisibility() == 8) {
            return;
        }
        if (this.P == null) {
            this.P = new Rect();
        }
        int pointMode = cOUIHintRedDot.getPointMode();
        int[] iArr = this.V;
        if (pointMode == 1) {
            int i13 = this.U;
            iArr[1] = i13;
            iArr[0] = i13;
        } else {
            iArr[1] = this.S;
            iArr[0] = this.T;
            if (cOUIHintRedDot.getPointNumber() >= 100 && cOUIHintRedDot.getPointNumber() < 1000) {
                iArr[0] = UIUtil.dip2px(getContext(), this.R) + iArr[0];
            } else if (cOUIHintRedDot.getPointNumber() > 0 && cOUIHintRedDot.getPointNumber() < 10) {
                iArr[0] = UIUtil.dip2px(getContext(), this.Q) + iArr[0];
            }
        }
        if (ViewCompat.getLayoutDirection(this) == 1) {
            this.P.set(frameLayout2.getLeft(), frameLayout2.getTop(), cOUIHintRedDot.getMeasuredWidth() + frameLayout2.getLeft(), cOUIHintRedDot.getMeasuredHeight() + frameLayout2.getTop());
            this.P.offset(-iArr[0], -iArr[1]);
        } else {
            this.P.set(frameLayout2.getRight() - cOUIHintRedDot.getMeasuredWidth(), frameLayout2.getTop(), frameLayout2.getRight(), cOUIHintRedDot.getMeasuredHeight() + frameLayout2.getTop());
            this.P.offset(iArr[0], -iArr[1]);
        }
        Rect rect = this.P;
        cOUIHintRedDot.layout(rect.left, rect.top, rect.right, rect.bottom);
    }

    @Override // android.widget.FrameLayout, android.view.View
    public final void onMeasure(int i3, int i10) {
        super.onMeasure(i3, i10);
        int rootViewHeight = getRootViewHeight();
        FrameLayout frameLayout = this.O;
        if (frameLayout.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) frameLayout.getLayoutParams();
            this.f8606b0 = marginLayoutParams;
            marginLayoutParams.height = rootViewHeight;
            frameLayout.setLayoutParams(marginLayoutParams);
            ViewGroup.MarginLayoutParams marginLayoutParams2 = this.f8606b0;
            rootViewHeight += marginLayoutParams2.topMargin + marginLayoutParams2.bottomMargin;
        }
        setMeasuredDimension(View.MeasureSpec.getSize(i3), rootViewHeight);
    }

    @Override // com.coui.appcompat.material.navigation.NavigationBarItemView, android.view.View
    public final void onSizeChanged(int i3, int i10, int i11, int i12) {
        super.onSizeChanged(i3, i10, i11, i12);
        RectF rectF = this.I;
        ViewGroup.MarginLayoutParams marginLayoutParams = this.f8606b0;
        rectF.set(marginLayoutParams.leftMargin, marginLayoutParams.topMargin, i3 - marginLayoutParams.rightMargin, i10 - marginLayoutParams.bottomMargin);
    }

    @Override // android.view.View
    public final boolean performLongClick() {
        return false;
    }

    @Override // android.view.View
    public void setBackground(Drawable drawable) {
        COUIStateEffectDrawable cOUIStateEffectDrawable = this.K;
        if (cOUIStateEffectDrawable == null) {
            super.setBackground(drawable);
        } else if (drawable == null) {
            cOUIStateEffectDrawable.setViewBackground(new ColorDrawable(0));
        } else {
            cOUIStateEffectDrawable.setViewBackground(drawable);
        }
    }

    public void setTextSize(int i3) {
        this.J = i3;
        this.G.setTextSize(0, i3);
        this.H.setTextSize(0, this.J);
    }
}
