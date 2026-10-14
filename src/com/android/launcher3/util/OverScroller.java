/*
 * Copyright (C) 2026 GreenDream
 */
package com.android.launcher3.util;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

import com.android.launcher3.R;

/**
 * Page scroller for {@link com.android.launcher3.PagedView}. Behaves exactly like
 * {@link android.widget.OverScroller} and adds a spring mode ({@link #startScrollSpring}) for
 * page snapping.
 *
 * <p>A subclass of the framework scroller cannot do this because its {@code isFinished},
 * {@code getCurrX}, {@code getFinalX} and {@code getDuration} are final. While springing, every
 * accessor here reports the spring, so callers relying on the scroller contract (page settling,
 * swipe-up end duration, etc.) keep working.
 */
public class OverScroller {

    /** Spring settles once within this many pixels of the target... */
    private static final float SPRING_VALUE_THRESHOLD = 0.75f;
    /** ...and slower than this many pixels per second. */
    private static final float SPRING_VELOCITY_THRESHOLD = SPRING_VALUE_THRESHOLD * 62.5f;

    private final android.widget.OverScroller mScroller;
    private final float mNaturalFrequency;
    private final float mDampingRatio;

    private boolean mSpringMode;
    private boolean mSpringFinished = true;
    private int mSpringStart;
    private int mSpringFinal;
    private float mSpringPosition;
    private float mSpringVelocity;
    private long mSpringStartTime;
    private int mSpringDuration;
    /** Displacement from {@link #mSpringFinal} and velocity at {@link #mSpringStartTime}. */
    private float mSpringStartDisplacement;
    private float mSpringStartVelocity;

    public OverScroller(Context context) {
        this(context, null);
    }

    public OverScroller(Context context, Interpolator interpolator) {
        mScroller = new android.widget.OverScroller(context, interpolator);
        Resources res = context.getResources();
        mNaturalFrequency = (float) Math.sqrt(getFloat(res, R.dimen.horizontal_spring_stiffness));
        mDampingRatio = getFloat(res, R.dimen.horizontal_spring_damping_ratio);
    }

    private static float getFloat(Resources res, int id) {
        TypedValue value = new TypedValue();
        res.getValue(id, value, true);
        return value.getFloat();
    }

    /**
     * Springs the x axis from {@code start} to {@code start + delta}.
     *
     * @param duration estimated settle time, only reported through {@link #getDuration()}.
     * @param velocity initial velocity in pixels per second, in scroll direction.
     */
    public void startScrollSpring(int start, int delta, int duration, float velocity) {
        mScroller.forceFinished(true);
        mSpringMode = true;
        mSpringStart = start;
        mSpringFinal = start + delta;
        mSpringDuration = duration;
        restartSpring(start, velocity);
    }

    /** Whether a spring started by {@link #startScrollSpring} is still running. */
    public boolean isSpringing() {
        return mSpringMode && !mSpringFinished;
    }

    private void restartSpring(float position, float velocity) {
        mSpringPosition = position;
        mSpringVelocity = velocity;
        mSpringStartDisplacement = position - mSpringFinal;
        mSpringStartVelocity = velocity;
        mSpringStartTime = AnimationUtils.currentAnimationTimeMillis();
        mSpringFinished = false;
    }

    private void finishSpring(boolean jumpToFinal) {
        if (jumpToFinal) {
            mSpringPosition = mSpringFinal;
        }
        mSpringVelocity = 0;
        mSpringFinished = true;
    }

    /** Closed-form damped spring (unit mass) evaluated {@code t} seconds after the start. */
    private void updateSpring(float t) {
        double x0 = mSpringStartDisplacement;
        double v0 = mSpringStartVelocity;
        double w0 = mNaturalFrequency;
        double zeta = mDampingRatio;
        double x;
        double v;
        if (zeta < 1) {
            double wd = w0 * Math.sqrt(1 - zeta * zeta);
            double a = x0;
            double b = (v0 + zeta * w0 * x0) / wd;
            double decay = Math.exp(-zeta * w0 * t);
            double cos = Math.cos(wd * t);
            double sin = Math.sin(wd * t);
            x = decay * (a * cos + b * sin);
            v = decay * ((-zeta * w0 * a + wd * b) * cos + (-zeta * w0 * b - wd * a) * sin);
        } else if (zeta == 1) {
            double a = x0;
            double b = v0 + w0 * x0;
            double decay = Math.exp(-w0 * t);
            x = (a + b * t) * decay;
            v = (b - w0 * (a + b * t)) * decay;
        } else {
            double root = w0 * Math.sqrt(zeta * zeta - 1);
            double rMinus = -zeta * w0 - root;
            double rPlus = -zeta * w0 + root;
            double c2 = (rMinus * x0 - v0) / (rMinus - rPlus);
            double c1 = x0 - c2;
            x = c1 * Math.exp(rMinus * t) + c2 * Math.exp(rPlus * t);
            v = c1 * rMinus * Math.exp(rMinus * t) + c2 * rPlus * Math.exp(rPlus * t);
        }
        if (Math.abs(x) < SPRING_VALUE_THRESHOLD && Math.abs(v) < SPRING_VELOCITY_THRESHOLD) {
            finishSpring(true);
        } else {
            mSpringPosition = (float) (mSpringFinal + x);
            mSpringVelocity = (float) v;
        }
    }

    public final void setFriction(float friction) {
        mScroller.setFriction(friction);
    }

    public final boolean isFinished() {
        return mSpringMode ? mSpringFinished : mScroller.isFinished();
    }

    public final void forceFinished(boolean finished) {
        if (!mSpringMode) {
            mScroller.forceFinished(finished);
        } else if (finished) {
            finishSpring(false);
        } else if (mSpringFinished) {
            restartSpring(mSpringPosition, 0);
        }
    }

    public final int getCurrX() {
        return mSpringMode ? Math.round(mSpringPosition) : mScroller.getCurrX();
    }

    public final int getCurrY() {
        return mScroller.getCurrY();
    }

    public float getCurrVelocity() {
        return mSpringMode ? Math.abs(mSpringVelocity) : mScroller.getCurrVelocity();
    }

    public final int getStartX() {
        return mSpringMode ? mSpringStart : mScroller.getStartX();
    }

    public final int getStartY() {
        return mScroller.getStartY();
    }

    public final int getFinalX() {
        return mSpringMode ? mSpringFinal : mScroller.getFinalX();
    }

    public final int getFinalY() {
        return mScroller.getFinalY();
    }

    public final int getDuration() {
        return mSpringMode ? mSpringDuration : mScroller.getDuration();
    }

    public void extendDuration(int extend) {
        if (mSpringMode) {
            mSpringDuration += extend;
        } else {
            mScroller.extendDuration(extend);
        }
    }

    public void setFinalX(int newX) {
        if (!mSpringMode) {
            mScroller.setFinalX(newX);
        } else if (newX != mSpringFinal) {
            mSpringFinal = newX;
            restartSpring(mSpringPosition, mSpringVelocity);
        }
    }

    public void setFinalY(int newY) {
        mScroller.setFinalY(newY);
    }

    /**
     * Same contract as {@link android.widget.OverScroller#computeScrollOffset()}: returns true
     * while animating, including the call that lands on the final position.
     */
    public boolean computeScrollOffset() {
        if (!mSpringMode) {
            return mScroller.computeScrollOffset();
        }
        if (mSpringFinished) {
            return false;
        }
        updateSpring((AnimationUtils.currentAnimationTimeMillis() - mSpringStartTime) / 1000f);
        return true;
    }

    public void startScroll(int startX, int startY, int dx, int dy) {
        mSpringMode = false;
        mScroller.startScroll(startX, startY, dx, dy);
    }

    public void startScroll(int startX, int startY, int dx, int dy, int duration) {
        mSpringMode = false;
        mScroller.startScroll(startX, startY, dx, dy, duration);
    }

    public boolean springBack(int startX, int startY, int minX, int maxX, int minY, int maxY) {
        mSpringMode = false;
        return mScroller.springBack(startX, startY, minX, maxX, minY, maxY);
    }

    public void fling(int startX, int startY, int velocityX, int velocityY,
            int minX, int maxX, int minY, int maxY) {
        mSpringMode = false;
        mScroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY);
    }

    public void fling(int startX, int startY, int velocityX, int velocityY,
            int minX, int maxX, int minY, int maxY, int overX, int overY) {
        mSpringMode = false;
        mScroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY,
                overX, overY);
    }

    public void notifyHorizontalEdgeReached(int startX, int finalX, int overX) {
        if (!mSpringMode) {
            mScroller.notifyHorizontalEdgeReached(startX, finalX, overX);
        }
    }

    public void notifyVerticalEdgeReached(int startY, int finalY, int overY) {
        if (!mSpringMode) {
            mScroller.notifyVerticalEdgeReached(startY, finalY, overY);
        }
    }

    public boolean isOverScrolled() {
        return !mSpringMode && mScroller.isOverScrolled();
    }

    public void abortAnimation() {
        if (mSpringMode) {
            finishSpring(true);
        } else {
            mScroller.abortAnimation();
        }
    }

    public int timePassed() {
        return mSpringMode
                ? (int) (AnimationUtils.currentAnimationTimeMillis() - mSpringStartTime)
                : mScroller.timePassed();
    }

    public boolean isScrollingInDirection(float xvel, float yvel) {
        if (!mSpringMode) {
            return mScroller.isScrollingInDirection(xvel, yvel);
        }
        return !mSpringFinished && Math.signum(xvel) == Math.signum(mSpringFinal - mSpringStart);
    }
}
