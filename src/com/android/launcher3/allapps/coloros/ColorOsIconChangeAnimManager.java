package com.android.launcher3.allapps.coloros;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

import androidx.dynamicanimation.animation.FloatPropertyCompat;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.icons.FastBitmapDrawable;
import com.coui.appcompat.animation.COUIMoveEaseInterpolator;
import com.coui.appcompat.animation.dynamicanimation.COUISpringAnimation;
import com.coui.appcompat.animation.dynamicanimation.COUISpringForce;

import java.util.ArrayList;

/**
 * Port of Oppo {@code OPlusIconChangeAnimManager} for drawer sort: in-place icon
 * crossfade (alpha + {@link FastBitmapDrawable#mScale}) and label fade.
 */
public final class ColorOsIconChangeAnimManager {

    private static final float BOUNCE_ALPHA = 0f;
    private static final float BOUNCE_SCALE = 0.35f;
    private static final long FADE_DURATION = 130L;
    private static final float MAX_ALPHA = 255f;
    private static final float MAX_SCALE = 1f;
    private static final float MIN_ALPHA = 0f;
    private static final float MIN_SCALE_NEW = 0.5f;
    private static final float MIN_SCALE_OLD = 0f;
    private static final float RESPONSE_ALPHA_HIDE = 0.2f;
    private static final float RESPONSE_ALPHA_SHOW = 0.3f;
    private static final float RESPONSE_SCALE = 0.4f;
    private static final float MIN_VISIBLE_ALPHA = 1f / 256f;
    private static final float MIN_VISIBLE_SCALE = 0.002f;

    private static final FloatPropertyCompat<FastBitmapDrawable> ALPHA_SPRING =
            new FloatPropertyCompat<FastBitmapDrawable>("alpha_spring") {
                @Override
                public float getValue(FastBitmapDrawable d) {
                    return d.getAlpha();
                }

                @Override
                public void setValue(FastBitmapDrawable d, float value) {
                    d.setAlpha((int) value);
                    d.invalidateSelf();
                }
            };

    private static final FloatPropertyCompat<FastBitmapDrawable> SCALE_SPRING =
            new FloatPropertyCompat<FastBitmapDrawable>("scale_spring") {
                @Override
                public float getValue(FastBitmapDrawable d) {
                    return d.mScale;
                }

                @Override
                public void setValue(FastBitmapDrawable d, float value) {
                    d.mScale = value;
                    d.invalidateSelf();
                }
            };

    private final BubbleTextView mBubbleTextView;
    private ArrayList<COUISpringAnimation> mAnimSet;
    private AnimatorSet mTextFadeAnimatorSet;

    public ColorOsIconChangeAnimManager(BubbleTextView bubbleTextView) {
        mBubbleTextView = bubbleTextView;
    }

    public void startIconChangeAnimIfNeeded(Drawable drawable) {
        if (mBubbleTextView == null || !mBubbleTextView.getIsNeedIconChangeAnim()) {
            return;
        }
        mBubbleTextView.setIsNeedIconChangeAnim(false);
        if (!mBubbleTextView.isAllAppsDisplay()) {
            return;
        }
        if (!(drawable instanceof FastBitmapDrawable)) {
            return;
        }
        FastBitmapDrawable newIcon = (FastBitmapDrawable) drawable;
        cancelSpringAnims();
        Drawable oldDrawable = mBubbleTextView.getIcon();
        if (!(oldDrawable instanceof FastBitmapDrawable) || oldDrawable == newIcon) {
            return;
        }
        handleFastIconChange(newIcon, (FastBitmapDrawable) oldDrawable);
    }

    private void handleFastIconChange(FastBitmapDrawable newIcon, FastBitmapDrawable oldIcon) {
        oldIcon.resetScale();
        newIcon.resetScale();

        int size = mBubbleTextView.getIconSize();
        oldIcon.setBounds(0, 0, size, size);
        newIcon.setBounds(0, 0, size, size);

        ColorOsIconLayerDrawable layer =
                new ColorOsIconLayerDrawable(new Drawable[]{oldIcon, newIcon});
        layer.setBounds(0, 0, size, size);

        newIcon.setAlpha(0);
        oldIcon.setAlpha(255);
        mBubbleTextView.applyIconVisual(layer);

        mAnimSet = new ArrayList<>(4);

        COUISpringAnimation newAlpha =
                new COUISpringAnimation(newIcon, ALPHA_SPRING, MAX_ALPHA);
        newAlpha.setStartValue(MIN_ALPHA);
        configureSpring(newAlpha.getSpring(), BOUNCE_ALPHA, RESPONSE_ALPHA_SHOW);
        newAlpha.setMinimumVisibleChange(MIN_VISIBLE_ALPHA);
        newAlpha.addEndListener((anim, canceled, value, velocity) -> {
            newIcon.setAlpha(255);
            handleFastIconChangeAnimEnd(layer);
        });
        mAnimSet.add(newAlpha);

        COUISpringAnimation newScale =
                new COUISpringAnimation(newIcon, SCALE_SPRING, MAX_SCALE);
        newScale.setStartValue(MIN_SCALE_NEW);
        configureSpring(newScale.getSpring(), BOUNCE_SCALE, RESPONSE_SCALE);
        newScale.setMinimumVisibleChange(MIN_VISIBLE_SCALE);
        newScale.addEndListener((anim, canceled, value, velocity) -> newIcon.mScale = MAX_SCALE);
        mAnimSet.add(newScale);

        COUISpringAnimation oldAlpha =
                new COUISpringAnimation(oldIcon, ALPHA_SPRING, MIN_ALPHA);
        oldAlpha.setStartValue(MAX_ALPHA);
        configureSpring(oldAlpha.getSpring(), BOUNCE_ALPHA, RESPONSE_ALPHA_HIDE);
        oldAlpha.setMinimumVisibleChange(MIN_VISIBLE_ALPHA);
        oldAlpha.addEndListener((anim, canceled, value, velocity) -> oldIcon.setAlpha(0));
        mAnimSet.add(oldAlpha);

        COUISpringAnimation oldScale =
                new COUISpringAnimation(oldIcon, SCALE_SPRING, MIN_SCALE_OLD);
        oldScale.setStartValue(MAX_SCALE);
        configureSpring(oldScale.getSpring(), BOUNCE_SCALE, RESPONSE_SCALE);
        oldScale.setMinimumVisibleChange(MIN_VISIBLE_SCALE);
        oldScale.addEndListener((anim, canceled, value, velocity) -> oldIcon.mScale = MIN_SCALE_OLD);
        mAnimSet.add(oldScale);

        for (COUISpringAnimation spring : mAnimSet) {
            spring.start();
        }
        mBubbleTextView.setIsNeedTextChangeAnim(true);
    }

    private static void configureSpring(COUISpringForce force, float bounce, float response) {
        force.setBounce(bounce);
        force.setResponse(response);
    }

    private void handleFastIconChangeAnimEnd(LayerDrawable layer) {
        FastBitmapDrawable icon = mBubbleTextView.getIcon();
        if (icon == null) {
            Drawable top = layer.getDrawable(1);
            if (top instanceof FastBitmapDrawable) {
                icon = (FastBitmapDrawable) top;
            }
        }
        if (icon == null) {
            return;
        }
        icon.setAlpha(255);
        icon.mScale = MAX_SCALE;
        // Prefer applying the logical icon (already assigned in setIcon) over
        // re-running applyFromApplicationInfo, which would interrupt label fade.
        mBubbleTextView.applyIconVisual(icon);
    }

    public void changeTextWithFade(CharSequence label) {
        if (mBubbleTextView == null || !mBubbleTextView.getIsNeedTextChangeAnim()) {
            return;
        }
        if (mTextFadeAnimatorSet != null && mTextFadeAnimatorSet.isRunning()) {
            mTextFadeAnimatorSet.cancel();
        }
        ValueAnimator fadeOut = ValueAnimator.ofFloat(1f, 0f);
        fadeOut.setDuration(FADE_DURATION);
        fadeOut.addUpdateListener(a ->
                mBubbleTextView.setTextAlpha((Float) a.getAnimatedValue()));
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mBubbleTextView.setText(label);
            }
        });
        ValueAnimator fadeIn = ValueAnimator.ofFloat(0f, 1f);
        fadeIn.setDuration(FADE_DURATION);
        fadeIn.addUpdateListener(a ->
                mBubbleTextView.setTextAlpha((Float) a.getAnimatedValue()));

        AnimatorSet set = new AnimatorSet();
        mTextFadeAnimatorSet = set;
        set.playSequentially(fadeOut, fadeIn);
        set.setInterpolator(new COUIMoveEaseInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                mBubbleTextView.setText(label);
                mBubbleTextView.setTextAlpha(1f);
                mBubbleTextView.setIsNeedTextChangeAnim(false);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mBubbleTextView.setIsNeedTextChangeAnim(false);
            }

            @Override
            public void onAnimationStart(Animator animation) {
                mBubbleTextView.setIsNeedTextChangeAnim(false);
            }
        });
        set.start();
    }

    public boolean isIconChangeAnimRunning() {
        if (mAnimSet == null || mAnimSet.isEmpty()) {
            return false;
        }
        for (COUISpringAnimation spring : mAnimSet) {
            if (spring.isRunning()) {
                return true;
            }
        }
        return false;
    }

    public void onDestroy() {
        if (mBubbleTextView == null) {
            return;
        }
        mBubbleTextView.setIsNeedIconChangeAnim(false);
        mBubbleTextView.setIsNeedTextChangeAnim(false);
        if (mTextFadeAnimatorSet != null && mTextFadeAnimatorSet.isRunning()) {
            mTextFadeAnimatorSet.cancel();
            mTextFadeAnimatorSet = null;
        }
        cancelSpringAnims();
    }

    private void cancelSpringAnims() {
        if (mAnimSet == null) {
            return;
        }
        for (COUISpringAnimation spring : mAnimSet) {
            if (spring != null && spring.isRunning()) {
                spring.cancel();
            }
        }
        mAnimSet.clear();
        mAnimSet = null;
    }
}
