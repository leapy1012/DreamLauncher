package com.android.launcher3.togglebar;

import android.view.View;

import androidx.annotation.Nullable;

import com.coui.appcompat.animation.dynamicanimation.COUIDynamicAnimation;
import com.coui.appcompat.animation.dynamicanimation.COUISpringAnimation;
import com.coui.appcompat.animation.dynamicanimation.COUISpringForce;

import java.util.ArrayList;
import java.util.List;

/**
 * Oppo {@code LayoutSpringAnimationHelper}: fade workspace page(s) out to scale 0.9,
 * run a layout mutation, then spring fade back in.
 */
public final class LayoutSpringAnimationHelper {

    private static final float SCALE_END = 0.9f;
    private static final float SCALE_OUT_RESPONSE = 0.2f;
    private static final float SCALE_IN_RESPONSE = 0.5f;
    private static final float FADE_IN_SCALE_BOUNCE = 0.35f;
    private static final float FADE_OUT_ALPHA_RESPONSE = 0.2f;
    private static final float FADE_IN_ALPHA_RESPONSE = 0.25f;
    private static final float SCALE_MIN_VISIBLE = 0.002f;
    private static final float ALPHA_MIN_VISIBLE = COUIDynamicAnimation.MIN_VISIBLE_CHANGE_ALPHA;

    private final List<COUISpringAnimation> mRunning = new ArrayList<>();
    private boolean mCanceled;

    public void cancel() {
        mCanceled = true;
        for (COUISpringAnimation spring : mRunning) {
            spring.cancel();
        }
        mRunning.clear();
    }

    public boolean isRunning() {
        return !mRunning.isEmpty();
    }

    /**
     * Fade {@code pages} out (α→0, scale→0.9), invoke {@code onMid}, then fade in
     * (α→1, scale→1 with bounce).
     */
    public void transitionPages(List<View> pages, @Nullable Runnable onMid,
            @Nullable Runnable onEnd) {
        cancel();
        mCanceled = false;
        if (pages == null || pages.isEmpty()) {
            if (onMid != null) {
                onMid.run();
            }
            if (onEnd != null) {
                onEnd.run();
            }
            return;
        }
        for (View page : pages) {
            if (page == null) {
                continue;
            }
            page.setPivotX(page.getWidth() / 2f);
            page.setPivotY(Math.max(1, page.getHeight()) / 2f);
        }
        final int[] pending = {0};
        for (View page : pages) {
            if (page == null) {
                continue;
            }
            pending[0] += 3; // alpha + sx + sy
            startSpring(page, COUIDynamicAnimation.ALPHA, page.getAlpha(), 0f,
                    FADE_OUT_ALPHA_RESPONSE, 0f, ALPHA_MIN_VISIBLE, () -> {
                        pending[0]--;
                        if (pending[0] == 0) {
                            onFadeOutComplete(pages, onMid, onEnd);
                        }
                    });
            startSpring(page, COUIDynamicAnimation.SCALE_X, page.getScaleX(), SCALE_END,
                    SCALE_OUT_RESPONSE, 0f, SCALE_MIN_VISIBLE, () -> {
                        pending[0]--;
                        if (pending[0] == 0) {
                            onFadeOutComplete(pages, onMid, onEnd);
                        }
                    });
            startSpring(page, COUIDynamicAnimation.SCALE_Y, page.getScaleY(), SCALE_END,
                    SCALE_OUT_RESPONSE, 0f, SCALE_MIN_VISIBLE, () -> {
                        pending[0]--;
                        if (pending[0] == 0) {
                            onFadeOutComplete(pages, onMid, onEnd);
                        }
                    });
        }
        if (pending[0] == 0) {
            onFadeOutComplete(pages, onMid, onEnd);
        }
    }

    private void onFadeOutComplete(List<View> pages, @Nullable Runnable onMid,
            @Nullable Runnable onEnd) {
        if (mCanceled) {
            return;
        }
        mRunning.clear();
        if (onMid != null) {
            onMid.run();
        }
        if (mCanceled) {
            return;
        }
        final int[] pending = {0};
        for (View page : pages) {
            if (page == null) {
                continue;
            }
            page.setAlpha(0f);
            page.setScaleX(SCALE_END);
            page.setScaleY(SCALE_END);
            pending[0] += 3;
            startSpring(page, COUIDynamicAnimation.ALPHA, 0f, 1f,
                    FADE_IN_ALPHA_RESPONSE, 0f, ALPHA_MIN_VISIBLE, () -> {
                        pending[0]--;
                        if (pending[0] == 0) {
                            finish(onEnd);
                        }
                    });
            startSpring(page, COUIDynamicAnimation.SCALE_X, SCALE_END, 1f,
                    SCALE_IN_RESPONSE, FADE_IN_SCALE_BOUNCE, SCALE_MIN_VISIBLE, () -> {
                        pending[0]--;
                        if (pending[0] == 0) {
                            finish(onEnd);
                        }
                    });
            startSpring(page, COUIDynamicAnimation.SCALE_Y, SCALE_END, 1f,
                    SCALE_IN_RESPONSE, FADE_IN_SCALE_BOUNCE, SCALE_MIN_VISIBLE, () -> {
                        pending[0]--;
                        if (pending[0] == 0) {
                            finish(onEnd);
                        }
                    });
        }
        if (pending[0] == 0) {
            finish(onEnd);
        }
    }

    private void finish(@Nullable Runnable onEnd) {
        if (mCanceled) {
            return;
        }
        mRunning.clear();
        if (onEnd != null) {
            onEnd.run();
        }
    }

    private void startSpring(View view, COUIDynamicAnimation.ViewProperty property,
            float start, float end, float response, float bounce, float minVisible,
            Runnable onEnd) {
        COUISpringForce force = new COUISpringForce(end)
                .setBounce(bounce)
                .setResponse(response);
        COUISpringAnimation spring = new COUISpringAnimation(view, property, end);
        spring.setSpring(force);
        spring.setStartValue(start);
        spring.setMinimumVisibleChange(minVisible);
        spring.addEndListener((animation, canceled, value, velocity) -> {
            mRunning.remove(spring);
            if (!canceled && !mCanceled && onEnd != null) {
                onEnd.run();
            }
        });
        mRunning.add(spring);
        spring.start();
    }
}
