package com.coui.appcompat.lockview;

import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.view.View;
import androidx.dynamicanimation.animation.FloatValueHolder;
import com.coui.appcompat.animation.dynamicanimation.COUIDynamicAnimation;
import com.coui.appcompat.animation.dynamicanimation.COUISpringAnimation;
import com.coui.appcompat.animation.dynamicanimation.COUISpringForce;

/* JADX INFO: loaded from: classes11.dex */
class LightEffectHelper {
    private static final float APPEAR_SPRING_RESPONSE = 0.3f;
    private static final float DEFAULT_ALPHA_VALUE = 255.0f;
    private static final float DEFAULT_DELAY_PERCENT = 0.6f;
    private static final float DEFAULT_SPRING_RESPONSE = 0.3f;
    private static final float DISAPPEAR_SPRING_RESPONSE = 0.6f;
    private LightEffectHelperCallback mCallback;
    private float mInnerLightAlpha;
    private COUISpringAnimation mInnerLightAnimator;
    private float mInnerLightRadius;
    private RadialGradient mInnerLightShader;
    private float mOuterLightAlpha;
    private COUISpringAnimation mOuterLightAnimator;
    private float mOuterLightRadiusEnd;
    private float mOuterLightRadiusStart;
    private RadialGradient mOuterLightShader;
    private final Matrix mRadialMatrix;
    private final View mTargetView;

    public interface LightEffectHelperCallback {
        default void onInnerLightUpdate(float f2) {
        }
    }

    public LightEffectHelper(View view) {
        this(view, 0.0f, 0.0f, null, null);
    }

    private void ensureLightEffectAnimator() {
        if (this.mInnerLightAnimator == null) {
            COUISpringForce cOUISpringForce = new COUISpringForce();
            cOUISpringForce.setBounce(0.0f);
            cOUISpringForce.setResponse(0.3f);
            COUISpringAnimation cOUISpringAnimation = new COUISpringAnimation(new FloatValueHolder(this.mInnerLightAlpha));
            this.mInnerLightAnimator = cOUISpringAnimation;
            cOUISpringAnimation.setSpring(cOUISpringForce);
            this.mInnerLightAnimator.addUpdateListener(new COUIDynamicAnimation.OnAnimationUpdateListener() {
                @Override // com.coui.appcompat.animation.dynamicanimation.COUIDynamicAnimation.OnAnimationUpdateListener
                public void onAnimationUpdate(COUIDynamicAnimation cOUIDynamicAnimation, float f2, float f3) {
                    LightEffectHelper.this.lambda$ensureLightEffectAnimator$0(cOUIDynamicAnimation, f2, f3);
                }
            });
        }
        if (this.mOuterLightAnimator == null) {
            COUISpringForce cOUISpringForce2 = new COUISpringForce();
            cOUISpringForce2.setBounce(0.0f);
            cOUISpringForce2.setResponse(0.3f);
            COUISpringAnimation cOUISpringAnimation2 = new COUISpringAnimation(new FloatValueHolder(this.mOuterLightAlpha));
            this.mOuterLightAnimator = cOUISpringAnimation2;
            cOUISpringAnimation2.setSpring(cOUISpringForce2);
            this.mOuterLightAnimator.addUpdateListener(new COUIDynamicAnimation.OnAnimationUpdateListener() {
                @Override // com.coui.appcompat.animation.dynamicanimation.COUIDynamicAnimation.OnAnimationUpdateListener
                public void onAnimationUpdate(COUIDynamicAnimation cOUIDynamicAnimation, float f2, float f3) {
                    LightEffectHelper.this.lambda$ensureLightEffectAnimator$1(cOUIDynamicAnimation, f2, f3);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$ensureLightEffectAnimator$0(COUIDynamicAnimation cOUIDynamicAnimation, float f2, float f3) {
        this.mInnerLightAlpha = f2;
        LightEffectHelperCallback lightEffectHelperCallback = this.mCallback;
        if (lightEffectHelperCallback != null) {
            lightEffectHelperCallback.onInnerLightUpdate(f2);
        }
        View view = this.mTargetView;
        if (view != null) {
            view.invalidate();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$ensureLightEffectAnimator$1(COUIDynamicAnimation cOUIDynamicAnimation, float f2, float f3) {
        this.mOuterLightAlpha = f2;
        View view = this.mTargetView;
        if (view != null) {
            view.invalidate();
        }
    }

    public void drawLightEffect(Canvas canvas, float f2, Path path, Paint paint, float f3, float f4) {
        if (paint == null || this.mInnerLightShader == null || this.mOuterLightShader == null) {
            return;
        }
        float f5 = this.mOuterLightRadiusEnd;
        float f6 = this.mOuterLightRadiusStart;
        float f7 = (((this.mOuterLightAlpha / 255.0f) * (f5 - f6)) + f6) / f5;
        this.mRadialMatrix.reset();
        this.mRadialMatrix.setScale(f7, f7, f3, f4);
        this.mOuterLightShader.setLocalMatrix(this.mRadialMatrix);
        paint.setShader(this.mOuterLightShader);
        paint.setAlpha((int) this.mOuterLightAlpha);
        paint.setBlendMode(BlendMode.LIGHTEN);
        canvas.drawPath(path, paint);
        this.mRadialMatrix.reset();
        this.mRadialMatrix.setScale(f2, f2, f3, f4);
        this.mInnerLightShader.setLocalMatrix(this.mRadialMatrix);
        paint.setShader(this.mInnerLightShader);
        paint.setAlpha((int) this.mInnerLightAlpha);
        canvas.drawCircle(f3, f4, this.mInnerLightRadius * f2, paint);
    }

    public void executeLightEffectAnimator(boolean z2) {
        ensureLightEffectAnimator();
        this.mInnerLightAnimator.animateToFinalPosition(z2 ? 255.0f : 0.0f);
        this.mOuterLightAnimator.getSpring().setResponse(z2 ? 0.3f : 0.6f);
        this.mOuterLightAnimator.animateToFinalPosition(z2 ? 255.0f : 0.0f);
    }

    public void setCallback(LightEffectHelperCallback lightEffectHelperCallback) {
        this.mCallback = lightEffectHelperCallback;
    }

    public void updateLightShaderConfig(float f2, float f3, RadialGradient radialGradient, RadialGradient radialGradient2) {
        this.mInnerLightRadius = f2;
        this.mOuterLightRadiusStart = 0.6f * f3;
        this.mOuterLightRadiusEnd = f3;
        this.mInnerLightShader = radialGradient;
        this.mOuterLightShader = radialGradient2;
    }

    public LightEffectHelper(View view, float f2, float f3, RadialGradient radialGradient, RadialGradient radialGradient2) {
        this.mTargetView = view;
        this.mRadialMatrix = new Matrix();
        this.mInnerLightRadius = f2;
        this.mOuterLightRadiusStart = 0.6f * f3;
        this.mOuterLightRadiusEnd = f3;
        this.mInnerLightShader = radialGradient;
        this.mOuterLightShader = radialGradient2;
    }
}
