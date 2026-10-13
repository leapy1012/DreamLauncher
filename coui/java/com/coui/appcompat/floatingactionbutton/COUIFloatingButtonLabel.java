package com.coui.appcompat.floatingactionbutton;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.GravityCompat;
import com.coui.appcompat.animation.COUIAnimationListenerAdapter;
import com.coui.appcompat.animation.COUIMoveEaseInterpolator;
import com.coui.appcompat.contextutil.COUIContextUtil;
import com.coui.appcompat.grid.COUIResponsiveUtils;
import com.coui.appcompat.statelistutil.COUIStateListUtil;
import com.coui.appcompat.uiutil.ShadowUtils;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.ShapeAppearanceModel;
import java.lang.ref.WeakReference;

/* JADX INFO: loaded from: classes4.dex */
public class COUIFloatingButtonLabel extends LinearLayout {

    /* JADX INFO: renamed from: o, reason: collision with root package name */
    public static final /* synthetic */ int f8324o = 0;

    /* JADX INFO: renamed from: a, reason: collision with root package name */
    public final COUIFloatingButtonSeamlessImpl f8325a;

    /* JADX INFO: renamed from: b, reason: collision with root package name */
    public boolean f8326b;
    public int c;
    public int d;

    /* JADX INFO: renamed from: e, reason: collision with root package name */
    public float f8327e;

    /* JADX INFO: renamed from: f, reason: collision with root package name */
    public ValueAnimator f8328f;

    /* JADX INFO: renamed from: g, reason: collision with root package name */
    public TextView f8329g;

    /* JADX INFO: renamed from: h, reason: collision with root package name */
    public ShapeableImageView f8330h;

    /* JADX INFO: renamed from: i, reason: collision with root package name */
    public CardView f8331i;

    /* JADX INFO: renamed from: j, reason: collision with root package name */
    public boolean f8332j;

    /* JADX INFO: renamed from: k, reason: collision with root package name */
    @Nullable
    public COUIFloatingButtonItem f8333k;

    /* JADX INFO: renamed from: l, reason: collision with root package name */
    @Nullable
    public COUIFloatingButton.OnActionSelectedListener f8334l;

    /* JADX INFO: renamed from: m, reason: collision with root package name */
    public float f8335m;

    /* JADX INFO: renamed from: n, reason: collision with root package name */
    public int f8336n;

    /* JADX INFO: renamed from: com.coui.appcompat.floatingactionbutton.COUIFloatingButtonLabel$3, reason: invalid class name */
    public class AnonymousClass3 extends ViewOutlineProvider {
        @Override // android.view.ViewOutlineProvider
        public final void getOutline(View view, Outline outline) {
            outline.setOval(0, 0, view.getWidth(), view.getHeight());
        }
    }

    public COUIFloatingButtonLabel(Context context) {
        super(context);
        this.f8325a = new COUIFloatingButtonSeamlessImpl();
        this.f8326b = true;
        this.c = 0;
        this.f8336n = -1;
        a(context, null);
    }

    private void setFabBackgroundColor(ColorStateList colorStateList) {
        this.f8330h.setBackgroundTintList(colorStateList);
    }

    private void setFabIcon(@Nullable Drawable drawable) {
        this.f8330h.setImageDrawable(drawable);
    }

    private void setLabel(@Nullable CharSequence charSequence) {
        if (TextUtils.isEmpty(charSequence)) {
            setLabelEnabled(false);
        } else {
            this.f8329g.setText(charSequence);
            setLabelEnabled(getOrientation() == 0);
        }
    }

    private void setLabelBackgroundColor(ColorStateList colorStateList) {
        if (colorStateList == ColorStateList.valueOf(Integer.MIN_VALUE)) {
            this.f8331i.setCardBackgroundColor(0);
            this.f8335m = this.f8331i.getElevation();
            this.f8331i.setElevation(0.0f);
        } else {
            this.f8331i.setCardBackgroundColor(colorStateList);
            float f3 = this.f8335m;
            if (f3 != 0.0f) {
                this.f8331i.setElevation(f3);
                this.f8335m = 0.0f;
            }
        }
    }

    private void setLabelEnabled(boolean z6) {
        this.f8332j = z6;
        this.f8331i.setVisibility(z6 ? 0 : 8);
    }

    private void setLabelTextColor(ColorStateList colorStateList) {
        this.f8329g.setTextColor(colorStateList);
    }

    private void setShadowColor(int i3) {
        this.f8336n = i3;
        ShadowUtils.c(getResources().getDimensionPixelOffset(com.coui.appcompat.R.dimen.support_shadow_size_level_three), getResources().getColor(com.coui.appcompat.R.color.coui_floating_button_elevation_color), this.f8336n, this.f8330h);
        ShadowUtils.c(getResources().getDimensionPixelOffset(com.coui.appcompat.R.dimen.support_shadow_size_level_three), getResources().getColor(com.coui.appcompat.R.color.coui_floating_button_elevation_color), this.f8336n, this.f8331i);
    }

    public final void a(Context context, @Nullable AttributeSet attributeSet) {
        View viewInflate = View.inflate(context, com.coui.appcompat.R.layout.coui_floating_button_item_label, this);
        this.f8330h = (ShapeableImageView) viewInflate.findViewById(com.coui.appcompat.R.id.coui_floating_button_child_fab);
        this.f8329g = (TextView) viewInflate.findViewById(com.coui.appcompat.R.id.coui_floating_button_label);
        this.f8331i = (CardView) viewInflate.findViewById(com.coui.appcompat.R.id.coui_floating_button_label_container);
        ShadowUtils.c(getResources().getDimensionPixelOffset(com.coui.appcompat.R.dimen.support_shadow_size_level_three), getResources().getColor(com.coui.appcompat.R.color.coui_floating_button_elevation_color), this.f8336n, this.f8330h);
        this.f8330h.setOutlineProvider(new AnonymousClass3());
        this.f8330h.setShapeAppearanceModel(ShapeAppearanceModel.builder().setAllCornerSizes(ShapeAppearanceModel.PILL).build());
        ShadowUtils.c(getResources().getDimensionPixelOffset(com.coui.appcompat.R.dimen.support_shadow_size_level_three), getResources().getColor(com.coui.appcompat.R.color.coui_floating_button_elevation_color), this.f8336n, this.f8331i);
        this.f8331i.setOutlineProvider(new ViewOutlineProvider() { // from class: com.coui.appcompat.floatingactionbutton.COUIFloatingButtonLabel.4
            @Override // android.view.ViewOutlineProvider
            public final void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), COUIContextUtil.c(com.coui.appcompat.R.attr.couiRoundCornerXS, COUIFloatingButtonLabel.this.getContext()));
            }
        });
        setOrientation(0);
        setClipChildren(false);
        setClipToPadding(false);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, com.coui.appcompat.R.styleable.COUIFloatingButtonLabel, 0, 0);
        try {
            try {
                this.f8326b = typedArrayObtainStyledAttributes.getBoolean(com.coui.appcompat.R.styleable.COUIFloatingButtonLabel_fabLabelNeedVibrate, true);
                int resourceId = typedArrayObtainStyledAttributes.getResourceId(com.coui.appcompat.R.styleable.COUIFloatingButtonLabel_srcCompat, Integer.MIN_VALUE);
                if (resourceId == Integer.MIN_VALUE) {
                    resourceId = typedArrayObtainStyledAttributes.getResourceId(com.coui.appcompat.R.styleable.COUIFloatingButtonLabel_android_src, Integer.MIN_VALUE);
                }
                COUIFloatingButtonItem.Builder builder = new COUIFloatingButtonItem.Builder(getId(), resourceId);
                builder.c = typedArrayObtainStyledAttributes.getString(com.coui.appcompat.R.styleable.COUIFloatingButtonLabel_fabLabel);
                builder.f8320e = ColorStateList.valueOf(typedArrayObtainStyledAttributes.getColor(com.coui.appcompat.R.styleable.COUIFloatingButtonLabel_fabBackgroundColor, COUIContextUtil.b(getContext(), com.coui.appcompat.R.attr.couiColorPrimary, 0)));
                builder.f8321f = ColorStateList.valueOf(typedArrayObtainStyledAttributes.getColor(com.coui.appcompat.R.styleable.COUIFloatingButtonLabel_fabLabelColor, Integer.MIN_VALUE));
                builder.f8322g = ColorStateList.valueOf(typedArrayObtainStyledAttributes.getColor(com.coui.appcompat.R.styleable.COUIFloatingButtonLabel_fabLabelBackgroundColor, Integer.MIN_VALUE));
                setFloatingButtonItem(new COUIFloatingButtonItem(builder));
            } catch (Exception e10) {
                Log.e("COUIFloatingButtonLabel", "Failure setting FabWithLabelView icon" + e10.getMessage());
            }
            typedArrayObtainStyledAttributes.recycle();
            setClipChildren(false);
            this.f8330h.setImportantForAccessibility(2);
            this.f8329g.setImportantForAccessibility(2);
            this.f8331i.setImportantForAccessibility(2);
            setFocusable(true);
            setImportantForAccessibility(1);
        } catch (Throwable th) {
            typedArrayObtainStyledAttributes.recycle();
            throw th;
        }
    }

    public ImageView getChildFloatingButton() {
        return this.f8330h;
    }

    public COUIFloatingButtonItem getFloatingButtonItem() {
        COUIFloatingButtonItem cOUIFloatingButtonItem = this.f8333k;
        if (cOUIFloatingButtonItem != null) {
            return cOUIFloatingButtonItem;
        }
        throw new IllegalStateException("SpeedDialActionItem not set yet!");
    }

    public COUIFloatingButtonItem.Builder getFloatingButtonItemBuilder() {
        return new COUIFloatingButtonItem.Builder(getFloatingButtonItem());
    }

    public CardView getFloatingButtonLabelBackground() {
        return this.f8331i;
    }

    public TextView getFloatingButtonLabelText() {
        return this.f8329g;
    }

    public Bundle getSeamlessViewBundle() {
        return this.f8325a.f8344a;
    }

    @Override // android.view.View
    public final void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (this.c <= 0) {
            Context contextCreateConfigurationContext = getContext().createConfigurationContext(configuration);
            if (COUIResponsiveUtils.isSmallScreenDp(configuration.screenWidthDp)) {
                this.d = contextCreateConfigurationContext.getResources().getDimensionPixelOffset(com.coui.appcompat.R.dimen.coui_floating_button_normal_size);
            } else {
                this.d = contextCreateConfigurationContext.getResources().getDimensionPixelOffset(com.coui.appcompat.R.dimen.coui_floating_button_large_size);
            }
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) this.f8330h.getLayoutParams();
            int i3 = this.d;
            layoutParams.width = i3;
            layoutParams.height = i3;
            this.f8330h.setLayoutParams(layoutParams);
        }
    }

    @Override // android.view.View
    public final void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK);
        accessibilityNodeInfo.setClassName(Button.class.getName());
        String string = this.f8329g.getText().toString();
        CharSequence contentDescription = this.f8330h.getContentDescription();
        if (TextUtils.isEmpty(string)) {
            string = !TextUtils.isEmpty(contentDescription) ? contentDescription.toString() : null;
        }
        accessibilityNodeInfo.setContentDescription(string);
    }

    @Override // android.view.View
    public final boolean performAccessibilityAction(int i3, @Nullable Bundle bundle) {
        if (i3 != 16) {
            return super.performAccessibilityAction(i3, bundle);
        }
        COUIFloatingButtonItem floatingButtonItem = getFloatingButtonItem();
        COUIFloatingButton.OnActionSelectedListener onActionSelectedListener = this.f8334l;
        if (onActionSelectedListener == null || floatingButtonItem == null) {
            return true;
        }
        onActionSelectedListener.a(floatingButtonItem);
        return true;
    }

    @Override // android.view.View
    public void setEnabled(boolean z6) {
        super.setEnabled(z6);
        this.f8329g.setEnabled(z6);
        this.f8330h.setEnabled(z6);
        this.f8331i.setEnabled(z6);
    }

    public void setFloatingButtonItem(COUIFloatingButtonItem cOUIFloatingButtonItem) {
        this.f8333k = cOUIFloatingButtonItem;
        setId(cOUIFloatingButtonItem.f8312a);
        Context context = getContext();
        String string = cOUIFloatingButtonItem.f8313b;
        if (string == null) {
            int i3 = cOUIFloatingButtonItem.c;
            string = i3 != Integer.MIN_VALUE ? context.getString(i3) : null;
        }
        setLabel(string);
        Context context2 = getContext();
        int i10 = cOUIFloatingButtonItem.d;
        setFabIcon(i10 != Integer.MIN_VALUE ? AppCompatResources.getDrawable(context2, i10) : null);
        int color = getContext().getResources().getColor(com.coui.appcompat.R.color.couiGreenTintControlNormal);
        int iB = COUIContextUtil.b(getContext(), com.coui.appcompat.R.attr.couiColorPrimary, color);
        ColorStateList colorStateListValueOf = ColorStateList.valueOf(Integer.MIN_VALUE);
        ColorStateList colorStateListA = cOUIFloatingButtonItem.f8314e;
        if (colorStateListA == colorStateListValueOf) {
            colorStateListA = COUIStateListUtil.a(iB, color);
        } else {
            setShadowColor(colorStateListA.getDefaultColor());
        }
        setFabBackgroundColor(colorStateListA);
        ColorStateList colorStateListValueOf2 = ColorStateList.valueOf(Integer.MIN_VALUE);
        ColorStateList colorStateList = cOUIFloatingButtonItem.f8315f;
        if (colorStateList == colorStateListValueOf2) {
            colorStateList = ResourcesCompat.getColorStateList(getResources(), com.coui.appcompat.R.color.coui_floating_button_label_text_color, getContext().getTheme());
        }
        setLabelTextColor(colorStateList);
        ColorStateList colorStateListValueOf3 = ColorStateList.valueOf(Integer.MIN_VALUE);
        ColorStateList colorStateListA2 = cOUIFloatingButtonItem.f8316g;
        if (colorStateListA2 == colorStateListValueOf3) {
            colorStateListA2 = COUIStateListUtil.a(iB, color);
        }
        setLabelBackgroundColor(colorStateListA2);
        if (cOUIFloatingButtonItem.f8317h) {
            this.f8330h.setOnTouchListener(new View.OnTouchListener() { // from class: com.coui.appcompat.floatingactionbutton.COUIFloatingButtonLabel.5
                @Override // android.view.View.OnTouchListener
                public final boolean onTouch(View view, MotionEvent motionEvent) {
                    int action = motionEvent.getAction();
                    final COUIFloatingButtonLabel cOUIFloatingButtonLabel = COUIFloatingButtonLabel.this;
                    if (action != 0) {
                        if (action != 1 && action != 3) {
                            return false;
                        }
                        int i11 = COUIFloatingButtonLabel.f8324o;
                        cOUIFloatingButtonLabel.clearAnimation();
                        ValueAnimator valueAnimator = cOUIFloatingButtonLabel.f8328f;
                        if (valueAnimator != null && valueAnimator.isRunning()) {
                            cOUIFloatingButtonLabel.f8328f.cancel();
                        }
                        ShapeableImageView shapeableImageView = cOUIFloatingButtonLabel.f8330h;
                        float f3 = cOUIFloatingButtonLabel.f8327e;
                        COUIMoveEaseInterpolator cOUIMoveEaseInterpolator = COUIFABPressFeedbackUtil.f8271a;
                        if (shapeableImageView == null) {
                            throw new IllegalArgumentException("The given view is empty. Please provide a valid view.");
                        }
                        COUIFloatingButtonTouchAnimation cOUIFloatingButtonTouchAnimation = new COUIFloatingButtonTouchAnimation(f3, 1.0f, shapeableImageView.getWidth() / 2.0f, shapeableImageView.getHeight() / 2.0f);
                        cOUIFloatingButtonTouchAnimation.setDuration(340L);
                        cOUIFloatingButtonTouchAnimation.setFillAfter(true);
                        cOUIFloatingButtonTouchAnimation.setInterpolator(COUIFABPressFeedbackUtil.f8271a);
                        cOUIFloatingButtonTouchAnimation.f8346a = new WeakReference<>(shapeableImageView);
                        shapeableImageView.startAnimation(cOUIFloatingButtonTouchAnimation);
                        return false;
                    }
                    cOUIFloatingButtonLabel.f8325a.a(cOUIFloatingButtonLabel.f8330h);
                    if (cOUIFloatingButtonLabel.f8326b) {
                        cOUIFloatingButtonLabel.performHapticFeedback(302);
                    }
                    cOUIFloatingButtonLabel.clearAnimation();
                    ValueAnimator valueAnimator2 = cOUIFloatingButtonLabel.f8328f;
                    if (valueAnimator2 != null && valueAnimator2.isRunning()) {
                        cOUIFloatingButtonLabel.f8328f.cancel();
                    }
                    ShapeableImageView shapeableImageView2 = cOUIFloatingButtonLabel.f8330h;
                    COUIMoveEaseInterpolator cOUIMoveEaseInterpolator2 = COUIFABPressFeedbackUtil.f8271a;
                    if (shapeableImageView2 == null) {
                        throw new IllegalArgumentException("The given view is empty. Please provide a valid view.");
                    }
                    COUIFloatingButtonTouchAnimation cOUIFloatingButtonTouchAnimation2 = new COUIFloatingButtonTouchAnimation(1.0f, 0.9f, shapeableImageView2.getWidth() / 2.0f, shapeableImageView2.getHeight() / 2.0f);
                    cOUIFloatingButtonTouchAnimation2.setDuration(200L);
                    cOUIFloatingButtonTouchAnimation2.setFillAfter(true);
                    COUIMoveEaseInterpolator cOUIMoveEaseInterpolator3 = COUIFABPressFeedbackUtil.f8271a;
                    cOUIFloatingButtonTouchAnimation2.setInterpolator(cOUIMoveEaseInterpolator3);
                    cOUIFloatingButtonTouchAnimation2.f8346a = new WeakReference<>(shapeableImageView2);
                    ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(1.0f, 0.9f);
                    valueAnimatorOfFloat.setDuration(200L);
                    valueAnimatorOfFloat.setInterpolator(cOUIMoveEaseInterpolator3);
                    cOUIFloatingButtonLabel.f8328f = valueAnimatorOfFloat;
                    valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.coui.appcompat.floatingactionbutton.COUIFloatingButtonLabel.6
                        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                        public final void onAnimationUpdate(ValueAnimator valueAnimator3) {
                            float fFloatValue = ((Float) valueAnimator3.getAnimatedValue()).floatValue();
                            COUIFloatingButtonLabel cOUIFloatingButtonLabel2 = COUIFloatingButtonLabel.this;
                            cOUIFloatingButtonLabel2.f8327e = fFloatValue;
                            if (cOUIFloatingButtonLabel2.f8327e >= 0.98f) {
                                cOUIFloatingButtonLabel2.f8327e = 0.98f;
                            }
                        }
                    });
                    cOUIFloatingButtonTouchAnimation2.setAnimationListener(new COUIAnimationListenerAdapter() { // from class: com.coui.appcompat.floatingactionbutton.COUIFloatingButtonLabel.7
                        @Override // com.coui.appcompat.animation.COUIAnimationListenerAdapter, android.view.animation.Animation.AnimationListener
                        public final void onAnimationStart(Animation animation) {
                            COUIFloatingButtonLabel.this.f8328f.start();
                        }
                    });
                    cOUIFloatingButtonLabel.f8330h.startAnimation(cOUIFloatingButtonTouchAnimation2);
                    return false;
                }
            });
        }
        getChildFloatingButton().setOnClickListener(new View.OnClickListener() { // from class: com.coui.appcompat.floatingactionbutton.COUIFloatingButtonLabel.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                int i11 = COUIFloatingButtonLabel.f8324o;
                COUIFloatingButtonLabel cOUIFloatingButtonLabel = COUIFloatingButtonLabel.this;
                COUIFloatingButtonItem floatingButtonItem = cOUIFloatingButtonLabel.getFloatingButtonItem();
                COUIFloatingButton.OnActionSelectedListener onActionSelectedListener = cOUIFloatingButtonLabel.f8334l;
                if (onActionSelectedListener == null || floatingButtonItem == null) {
                    return;
                }
                onActionSelectedListener.a(floatingButtonItem);
            }
        });
    }

    public void setMainButtonSize(int i3) {
        this.c = i3;
        if (i3 > 0) {
            this.d = i3;
        } else {
            this.d = getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_floating_button_size);
        }
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) this.f8330h.getLayoutParams();
        int i10 = this.d;
        layoutParams.width = i10;
        layoutParams.height = i10;
        this.f8330h.setLayoutParams(layoutParams);
    }

    public void setOnActionSelectedListener(@Nullable COUIFloatingButton.OnActionSelectedListener onActionSelectedListener) {
        this.f8334l = onActionSelectedListener;
        if (onActionSelectedListener != null) {
            getFloatingButtonLabelBackground().setOnClickListener(new View.OnClickListener() { // from class: com.coui.appcompat.floatingactionbutton.COUIFloatingButtonLabel.2
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    COUIFloatingButtonLabel cOUIFloatingButtonLabel = COUIFloatingButtonLabel.this;
                    COUIFloatingButtonItem floatingButtonItem = cOUIFloatingButtonLabel.getFloatingButtonItem();
                    COUIFloatingButton.OnActionSelectedListener onActionSelectedListener2 = cOUIFloatingButtonLabel.f8334l;
                    if (onActionSelectedListener2 == null || floatingButtonItem == null) {
                        return;
                    }
                    onActionSelectedListener2.a(floatingButtonItem);
                }
            });
        } else {
            getChildFloatingButton().setOnClickListener(null);
            getFloatingButtonLabelBackground().setOnClickListener(null);
        }
    }

    @Override // android.widget.LinearLayout
    public void setOrientation(int i3) {
        LinearLayout.LayoutParams layoutParams;
        super.setOrientation(i3);
        int dimensionPixelSize = getContext().getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_floating_button_fab_normal_size);
        getContext().getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_floating_button_fab_side_margin);
        getContext().getResources().getDimensionPixelSize(com.coui.appcompat.R.dimen.coui_floating_button_item_normal_bottom_margin);
        LinearLayout.LayoutParams layoutParams2 = (LinearLayout.LayoutParams) this.f8330h.getLayoutParams();
        if (getOrientation() == 0) {
            layoutParams = new LinearLayout.LayoutParams(-2, -2);
            layoutParams.gravity = GravityCompat.END;
        } else {
            LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(dimensionPixelSize, -2);
            layoutParams3.gravity = 16;
            layoutParams2.setMargins(0, 0, 0, 0);
            layoutParams = layoutParams3;
        }
        setLayoutParams(layoutParams);
        this.f8330h.setLayoutParams(layoutParams2);
        if (i3 == 1) {
            setLabelEnabled(false);
        } else {
            setLabel(this.f8329g.getText().toString());
        }
    }

    @Override // android.view.View
    @SuppressLint({"RestrictedApi"})
    public void setVisibility(int i3) {
        super.setVisibility(i3);
        getChildFloatingButton().setVisibility(i3);
        if (this.f8332j) {
            getFloatingButtonLabelBackground().setVisibility(i3);
        }
    }

    public COUIFloatingButtonLabel(Context context, @Nullable AttributeSet attributeSet) {
        super(context, attributeSet);
        this.f8325a = new COUIFloatingButtonSeamlessImpl();
        this.f8326b = true;
        this.c = 0;
        this.f8336n = -1;
        a(context, attributeSet);
    }

    public COUIFloatingButtonLabel(Context context, @Nullable AttributeSet attributeSet, int i3) {
        super(context, attributeSet, i3);
        this.f8325a = new COUIFloatingButtonSeamlessImpl();
        this.f8326b = true;
        this.c = 0;
        this.f8336n = -1;
        a(context, attributeSet);
    }
}
