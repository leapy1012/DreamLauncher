package com.android.launcher3.iconresize;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;
import android.view.ViewOutlineProvider;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import java.lang.reflect.Method;

/**
 * Oppo {@code ItemResizeFrame.BlurView} + {@code IResizeFramePainter.countBlurRadius}.
 */
public class IconResizeBlurHelper {

    static final float MAX_BLUR_RADIUS_PX = 20f;
    private static final float BLUR_SCALE = 40f;
    private static final float SPRING_STIFFNESS = 400f;
    private static final float SPRING_DAMPING = 1f;

    private float mBlurFactor;
    private boolean mAnimBlock;
    @Nullable
    private SpringAnimation mFadeSpring;
    @Nullable
    private Runnable mInvalidator;

    public static IconResizeBlurView createBlurView(Context context) {
        IconResizeBlurView view = new IconResizeBlurView(context);
        view.setWillNotDraw(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
        return view;
    }

    /** Oppo countBlurRadius: blur strength from icon vs frame size mismatch. */
    public void updateBlur(Rect iconBounds, Rect frameBounds, boolean animBlock,
            Runnable invalidator) {
        mInvalidator = invalidator;
        if (animBlock) {
            mAnimBlock = true;
            setBlurFactor(1f);
            return;
        }
        mAnimBlock = false;
        float diff = Math.max(
                Math.abs(iconBounds.width() - frameBounds.width()),
                Math.abs(iconBounds.height() - frameBounds.height()));
        float factor = Math.min(1f, diff / BLUR_SCALE);
        if (mFadeSpring != null && mFadeSpring.isRunning() && factor <= mBlurFactor) {
            return;
        }
        cancelFade();
        setBlurFactor(factor);
    }

    public void fadeOut(Runnable invalidator) {
        mInvalidator = invalidator;
        if (mBlurFactor <= 0f) {
            return;
        }
        cancelFade();
        mFadeSpring = new SpringAnimation(this, BLUR_FACTOR, 0f);
        mFadeSpring.setSpring(new SpringForce(0f)
                .setStiffness(SPRING_STIFFNESS)
                .setDampingRatio(SPRING_DAMPING));
        mFadeSpring.addEndListener((animation, canceled, value, velocity) -> {
            mFadeSpring = null;
            setBlurFactor(0f);
        });
        mFadeSpring.start();
    }

    public void clear() {
        cancelFade();
        mAnimBlock = false;
        setBlurFactor(0f);
        mInvalidator = null;
    }

    public void applyTo(IconResizeBlurView blurView) {
        if (Build.VERSION.SDK_INT < 33) {
            return;
        }
        float radius = mBlurFactor * MAX_BLUR_RADIUS_PX;
        if (radius <= 0.5f) {
            setBackdropBlur(blurView, null);
        } else {
            setBackdropBlur(blurView, RenderEffect.createBlurEffect(
                    radius, radius, Shader.TileMode.DECAL));
        }
    }

    @RequiresApi(33)
    private static void setBackdropBlur(View view, @Nullable RenderEffect effect) {
        try {
            Method method = View.class.getMethod("setBackdropRenderEffect", RenderEffect.class);
            method.invoke(view, effect);
        } catch (ReflectiveOperationException ignored) {
            // Backdrop blur unavailable on this build.
        }
    }

    private void cancelFade() {
        if (mFadeSpring != null) {
            mFadeSpring.cancel();
            mFadeSpring = null;
        }
    }

    private void setBlurFactor(float factor) {
        mBlurFactor = Math.max(0f, Math.min(1f, factor));
        if (mInvalidator != null) {
            mInvalidator.run();
        }
    }

    private static final FloatPropertyCompat<IconResizeBlurHelper> BLUR_FACTOR =
            new FloatPropertyCompat<IconResizeBlurHelper>("blurFactor") {
                @Override
                public float getValue(IconResizeBlurHelper object) {
                    return object.mBlurFactor;
                }

                @Override
                public void setValue(IconResizeBlurHelper object, float value) {
                    object.setBlurFactor(value);
                }
            };

    /** Clipped backdrop blur mask (Oppo ItemResizeFrame.BlurView). */
    public static final class IconResizeBlurView extends View {
        private final Path mClipPath = new Path();
        private float mCornerRadius;

        public IconResizeBlurView(Context context) {
            super(context);
        }

        public void setCornerRadius(float radius) {
            if (mCornerRadius != radius) {
                mCornerRadius = radius;
                invalidateOutline();
            }
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            updateOutline();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            updateOutline();
        }

        private void updateOutline() {
            if (getWidth() <= 0 || getHeight() <= 0) {
                return;
            }
            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    mClipPath.reset();
                    RectF rect = new RectF(0, 0, view.getWidth(), view.getHeight());
                    mClipPath.addRoundRect(rect, mCornerRadius, mCornerRadius, Path.Direction.CCW);
                    outline.setPath(mClipPath);
                }
            });
            setClipToOutline(true);
        }
    }
}
