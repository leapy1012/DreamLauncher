package com.android.launcher3.iconresize;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;

import com.android.launcher3.R;

/**
 * Oppo {@code ResizeFrameStrokeManager} stroke widths and alpha for the workspace icon frame.
 */
public final class ResizeFrameStrokeState {

    private static final long ENTER_DURATION_MS = 320;

    private final float mMinFrameStrokePx;
    private final float mMaxFrameStrokePx;
    private final float mMinHandleStrokePx;
    private final float mMaxHandleStrokePx;

    private float mFrameStrokePx;
    private float mHandleStrokePx;
    private float mStrokeAlpha;
    private boolean mActive;
    private ValueAnimator mEnterAnimator;

    public ResizeFrameStrokeState(Context context) {
        mMinFrameStrokePx = context.getResources().getDimension(
                R.dimen.icon_resize_frame_stroke_width_min);
        mMaxFrameStrokePx = context.getResources().getDimension(
                R.dimen.icon_resize_frame_stroke_width_max);
        mMinHandleStrokePx = context.getResources().getDimension(
                R.dimen.icon_resize_handle_stroke_width_min);
        mMaxHandleStrokePx = context.getResources().getDimension(
                R.dimen.icon_resize_handle_stroke_width_max);
        mFrameStrokePx = mMinFrameStrokePx;
        mHandleStrokePx = mMinHandleStrokePx;
        mStrokeAlpha = 0f;
    }

    public boolean isActive() {
        return mActive;
    }

    public float getFrameStrokePx() {
        return mFrameStrokePx;
    }

    public float getHandleStrokePx() {
        return mHandleStrokePx;
    }

    public float getStrokeAlpha() {
        return mStrokeAlpha;
    }

    public void activate(View host, @Nullable Runnable overlayInvalidator) {
        if (mActive) {
            return;
        }
        mActive = true;
        mOverlayInvalidator = overlayInvalidator;
        startEnterAnimation(host);
    }

    public void deactivate(View host) {
        mActive = false;
        mOverlayInvalidator = null;
        cancelAnimation();
        mFrameStrokePx = mMinFrameStrokePx;
        mHandleStrokePx = mMinHandleStrokePx;
        mStrokeAlpha = 0f;
        if (host != null) {
            host.invalidate();
        }
    }

    @Nullable
    private Runnable mOverlayInvalidator;

    private void startEnterAnimation(View host) {
        cancelAnimation();
        mEnterAnimator = ValueAnimator.ofFloat(0f, 1f);
        mEnterAnimator.setDuration(ENTER_DURATION_MS);
        mEnterAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
        mEnterAnimator.addUpdateListener(animation -> {
            float t = (float) animation.getAnimatedValue();
            mFrameStrokePx = lerp(mMinFrameStrokePx, mMaxFrameStrokePx, t);
            mHandleStrokePx = lerp(mMinHandleStrokePx, mMaxHandleStrokePx, t);
            mStrokeAlpha = t;
            host.invalidate();
            if (mOverlayInvalidator != null) {
                mOverlayInvalidator.run();
            }
        });
        mEnterAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mStrokeAlpha = 1f;
                mFrameStrokePx = mMaxFrameStrokePx;
                mHandleStrokePx = mMaxHandleStrokePx;
                host.invalidate();
                if (mOverlayInvalidator != null) {
                    mOverlayInvalidator.run();
                }
            }
        });
        mEnterAnimator.start();
    }

    private void cancelAnimation() {
        if (mEnterAnimator != null) {
            mEnterAnimator.cancel();
            mEnterAnimator = null;
        }
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }
}
