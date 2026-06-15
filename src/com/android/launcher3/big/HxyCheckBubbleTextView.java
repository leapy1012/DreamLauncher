package com.android.launcher3.big;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Property;
import com.android.launcher3.Utilities;
import com.android.launcher3.graphics.IconShape;
import com.android.launcher3.icons.DotRenderer;

public class HxyCheckBubbleTextView extends HxyAnimBubbleTextView {
    private static final Property<HxyCheckBubbleTextView, Float> DOT_SCALE_PROPERTY = new Property<HxyCheckBubbleTextView, Float>(Float.TYPE, "dotScale") {
        public Float get(HxyCheckBubbleTextView icon) {
            return Float.valueOf(icon.mDotParams.scale);
        }

        public void set(HxyCheckBubbleTextView icon, Float value) {
            icon.mDotParams.scale = value.floatValue();
            icon.invalidate();
        }
    };
    public boolean isSelect = false;
    public DotRenderer.DrawParams mDotParams = new DotRenderer.DrawParams();
    public Animator mDotScaleAnim;
    protected boolean mForceCheckHideDot = true;

    public HxyCheckBubbleTextView(Context context) {
        super(context);
    }

    public void setmForceCheckHideDot(boolean mForceCheckHideDot2) {
        this.mForceCheckHideDot = mForceCheckHideDot2;
    }

    public HxyCheckBubbleTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HxyCheckBubbleTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!this.mForceCheckHideDot) {
            drawCheck(canvas);
        }
    }

    private void drawCheck(Canvas canvas) {
        if (this.mDotParams.scale > 0.0f) {
            getIconBounds(this.mDotParams.iconBounds);
            Utilities.scaleRectAboutCenter(this.mDotParams.iconBounds, IconShape.getNormalizationScale());
            int scrollX = getScrollX();
            int scrollY = getScrollY();
            canvas.translate((float) scrollX, (float) scrollY);
            try {
                if (this.mDotRenderer != null) {
                    this.mDotParams.leftAlign = true;
                    this.mDotRenderer.drawCheck(canvas, this.mDotParams);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            canvas.translate((float) (-scrollX), (float) (-scrollY));
        }
    }

    public void animateDotScale(float... dotScales) {
        cancelDotScaleAnim();
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(this, DOT_SCALE_PROPERTY, dotScales);
        this.mDotScaleAnim = ofFloat;
        ofFloat.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                HxyCheckBubbleTextView.this.mDotScaleAnim = null;
            }
        });
        this.mDotScaleAnim.start();
    }

    private void cancelDotScaleAnim() {
        Animator animator = this.mDotScaleAnim;
        if (animator != null) {
            animator.cancel();
        }
    }

    public void updateDotScale(boolean animate, boolean isDotted) {
        float newDotScale = isDotted ? 1.0f : 0.0f;
        if (!animate || !isDotted || !isShown()) {
            cancelDotScaleAnim();
            animateDotScale(newDotScale);
            return;
        }
        animateDotScale(newDotScale);
    }
}