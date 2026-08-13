package com.android.launcher3.anim;

import android.view.animation.BaseInterpolator;

/**
 * ColorOS spring timing curve ported from the decoded OPPO launcher.
 *
 * <p>The constructor arguments are stiffness, damping ratio, initial velocity and the normalized
 * time range used to sample the response.</p>
 */
public final class OSpringInterpolator extends BaseInterpolator {
    private static final float VELOCITY_UNIT = 3000f;

    private final double mNaturalFrequency;
    private final double mDampingRatio;
    private final double mInitialVelocity;
    private final float mCutRatio;
    private final double mDampedFrequency;
    private final double mB;
    private float mFinalValue = -1f;

    public OSpringInterpolator(double stiffness, double dampingRatio, double flingVelocity,
            float cutRatio) {
        mNaturalFrequency = Math.sqrt(stiffness);
        mDampingRatio = dampingRatio;
        mInitialVelocity = Math.abs(flingVelocity) / VELOCITY_UNIT;
        mCutRatio = cutRatio;
        if (dampingRatio < 1d) {
            mDampedFrequency = Math.sqrt(1d - dampingRatio * dampingRatio)
                    * mNaturalFrequency;
            mB = (dampingRatio * mNaturalFrequency - mInitialVelocity) / mDampedFrequency;
        } else {
            mDampedFrequency = 0d;
            mB = mNaturalFrequency - mInitialVelocity;
        }
    }

    private float getOriginInterpolation(float input) {
        double time = input * mCutRatio;
        double decay = Math.exp(-mDampingRatio * mNaturalFrequency * time);
        final double remaining;
        if (mDampingRatio < 1d) {
            remaining = (Math.cos(mDampedFrequency * time)
                    + mB * Math.sin(mDampedFrequency * time)) * decay;
        } else if (Double.compare(mDampingRatio, 1d) == 0) {
            remaining = (1d + mB * time) * decay;
        } else {
            double overFrequency = Math.sqrt(mDampingRatio * mDampingRatio - 1d)
                    * mNaturalFrequency;
            remaining = (Math.cosh(overFrequency * time)
                    + ((mDampingRatio * mNaturalFrequency - mInitialVelocity) / overFrequency)
                    * Math.sinh(overFrequency * time)) * decay;
        }
        return (float) (1d - remaining);
    }

    @Override
    public float getInterpolation(float input) {
        if (mFinalValue == -1f) {
            mFinalValue = getOriginInterpolation(1f);
        }
        return getOriginInterpolation(input) / mFinalValue;
    }
}
