package com.coui.appcompat.scroll;

import android.content.Context;
import android.util.Log;
import android.view.ViewConfiguration;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.OverScroller;


public class COUILocateOverScroller extends OverScroller implements COUIIOverScroller {
    private static final int FLING_MODE = 1;
    private static final int INVALID_POSITION = -1;
    private static final float ONE = 1.0f;
    private static final Interpolator SCROLL = new Interpolator() {
        @Override
        public float getInterpolation(float f2) {
            float f10 = f2 - 1.0f;
            return (f10 * f10 * f10 * f10 * f10) + 1.0f;
        }
    };
    private static final int SCROLL_DEFAULT_DURATION = 250;
    private static final int SCROLL_MODE = 0;
    private static final String TAG = "COUILocateOverScroller";
    private static final float THOUSAND = 1000.0f;
    private COUlFrameRateScrollSceneHelper mFrameRateHelper;
    private Interpolator mInterpolator;
    private int mMode;
    private COUISplineOverScroller mScrollerX;
    private COUISplineOverScroller mScrollerY;

    public static class COUISplineOverScroller {
        private static final int BALLISTIC = 2;
        private static final int CUBIC = 1;
        private static final float END_TENSION = 1.0f;
        private static final float GRAVITY = 2000.0f;
        private static final float INFLEXION = 0.35f;
        private static final float LENGTH = 39.37f;
        private static final double MIN_ALPHA = 1.0E-5d;
        private static final int NB_SAMPLES = 100;
        private static final float ONE = 1.0f;
        private static final float P1 = 0.175f;
        private static final float P2 = 0.35000002f;
        private static final float SIX = 6.0f;
        private static final int SPLINE = 0;
        private static final float START_TENSION = 0.5f;
        private static final float THREE = 3.0f;
        private static final float TUNING = 0.84f;
        private static final float TWO = 2.0f;
        private static final float ZERO = 0.0f;
        private float mCurrVelocity;
        private int mCurrentPosition;
        private float mDeceleration;
        private int mDuration;
        private int mFinal;
        private int mOver;
        private float mPhysicalCoeff;
        private int mSplineDistance;
        private int mSplineDuration;
        private int mStart;
        private long mStartTime;
        private int mVelocity;
        private static final float DECELERATION_RATE = (float) (Math.log(0.78d) / Math.log(0.9d));
        private static final float[] SPLINE_POSITION = new float[101];
        private static final float[] SPLINE_TIME = new float[101];
        private float mDurationRatio = 1.0f;
        private float mVelocityRatio = 1.0f;
        private float mFlingFriction = ViewConfiguration.getScrollFriction() * 2.5f;
        private int mState = 0;
        private boolean mFinished = true;

        static {
            float f2;
            float f10;
            float f11;
            float f12;
            float f13;
            float f14;
            float f15;
            float f16;
            float f17;
            float f18;
            float f19 = 0.0f;
            float f20 = 0.0f;
            for (int i2 = 0; i2 < 100; i2++) {
                float f21 = i2 / 100.0f;
                float f22 = 1.0f;
                while (true) {
                    f2 = 2.0f;
                    f10 = ((f22 - f19) / 2.0f) + f19;
                    f11 = THREE;
                    f12 = 1.0f - f10;
                    f13 = f10 * THREE * f12;
                    f14 = f10 * f10 * f10;
                    float f23 = (((f12 * P1) + (f10 * P2)) * f13) + f14;
                    if (Math.abs(f23 - f21) < MIN_ALPHA) {
                        break;
                    } else if (f23 > f21) {
                        f22 = f10;
                    } else {
                        f19 = f10;
                    }
                }
                SPLINE_POSITION[i2] = (f13 * ((f12 * 0.5f) + f10)) + f14;
                float f24 = 1.0f;
                while (true) {
                    f15 = ((f24 - f20) / f2) + f20;
                    f16 = 1.0f - f15;
                    f17 = f15 * f11 * f16;
                    f18 = f15 * f15 * f15;
                    float f25 = (((f16 * 0.5f) + f15) * f17) + f18;
                    if (Math.abs(f25 - f21) < MIN_ALPHA) {
                        break;
                    }
                    if (f25 > f21) {
                        f24 = f15;
                    } else {
                        f20 = f15;
                    }
                    f2 = 2.0f;
                    f11 = THREE;
                }
                SPLINE_TIME[i2] = (f17 * ((f16 * P1) + (f15 * P2))) + f18;
            }
            SPLINE_POSITION[100] = 1.0f;
            SPLINE_TIME[100] = 1.0f;
        }

        public COUISplineOverScroller(Context context) {
            this.mPhysicalCoeff = context.getResources().getDisplayMetrics().density * 160.0f * 386.0878f * TUNING;
        }

        private void adjustDuration(int i2, int i6, int i10) {
            float fAbs = Math.abs((i10 - i2) / (i6 - i2));
            int i11 = (int) (fAbs * 100.0f);
            if (i11 >= 100 || i11 < 0) {
                return;
            }
            float f2 = i11 / 100.0f;
            int i12 = i11 + 1;
            float[] fArr = SPLINE_TIME;
            float f10 = fArr[i11];
            this.mDuration = (int) (this.mDuration * (f10 + (((fAbs - f2) / ((i12 / 100.0f) - f2)) * (fArr[i12] - f10))));
        }

        private void fitOnBounceCurve(int i2, int i6, int i10) {
            float f2 = this.mDeceleration;
            float f10 = (-i10) / f2;
            float f11 = i10;
            float fSqrt = (float) Math.sqrt((((double) ((((f11 * f11) / 2.0f) / Math.abs(f2)) + Math.abs(i6 - i2))) * 2.0d) / ((double) Math.abs(this.mDeceleration)));
            this.mStartTime -= (long) ((int) ((fSqrt - f10) * COUILocateOverScroller.THOUSAND));
            this.mCurrentPosition = i6;
            this.mStart = i6;
            this.mVelocity = (int) ((-this.mDeceleration) * fSqrt);
        }

        private static float getDeceleration(int i2) {
            if (i2 > 0) {
                return -2000.0f;
            }
            return GRAVITY;
        }

        private double getSplineDeceleration(int i2) {
            return Math.log((Math.abs(i2) * INFLEXION) / (this.mFlingFriction * this.mPhysicalCoeff));
        }

        private double getSplineFlingDistance(int i2) {
            double splineDeceleration = getSplineDeceleration(i2);
            float f2 = DECELERATION_RATE;
            return ((double) (this.mFlingFriction * this.mPhysicalCoeff)) * Math.exp((((double) f2) / (((double) f2) - 1.0d)) * splineDeceleration);
        }

        private int getSplineFlingDuration(int i2) {
            return (int) (Math.exp(getSplineDeceleration(i2) / ((double) (DECELERATION_RATE - 1.0f))) * 1000.0d);
        }

        private void onEdgeReached() {
            int i2 = this.mVelocity;
            float f2 = i2 * i2;
            float fAbs = f2 / (Math.abs(this.mDeceleration) * 2.0f);
            float fSignum = Math.signum(this.mVelocity);
            int i6 = this.mOver;
            if (fAbs > i6) {
                this.mDeceleration = ((-fSignum) * f2) / (i6 * 2.0f);
                fAbs = i6;
            }
            this.mOver = (int) fAbs;
            this.mState = 2;
            int i10 = this.mStart;
            int i11 = this.mVelocity;
            if (i11 <= 0) {
                fAbs = -fAbs;
            }
            this.mFinal = i10 + ((int) fAbs);
            this.mDuration = -((int) ((i11 * COUILocateOverScroller.THOUSAND) / this.mDeceleration));
        }

        private void startAfterEdge(int i2, int i6, int i10, int i11) {
            if (i2 > i6 && i2 < i10) {
                Log.e(COUILocateOverScroller.TAG, "startAfterEdge called from a valid position");
                this.mFinished = true;
                return;
            }
            boolean z6 = i2 > i10;
            int i12 = z6 ? i10 : i6;
            if ((i2 - i12) * i11 >= 0) {
                startBounceAfterEdge(i2, i12, i11);
            } else if (getSplineFlingDistance(i11) > Math.abs(i2 - i12)) {
                fling(i2, i11, z6 ? i6 : i2, z6 ? i2 : i10, this.mOver);
            } else {
                startSpringback(i2, i12, i11);
            }
        }

        private void startBounceAfterEdge(int i2, int i6, int i10) {
            this.mDeceleration = getDeceleration(i10 == 0 ? i2 - i6 : i10);
            fitOnBounceCurve(i2, i6, i10);
            onEdgeReached();
        }

        private void startSpringback(int i2, int i6, int i10) {
            this.mFinished = false;
            this.mState = 1;
            this.mCurrentPosition = i2;
            this.mStart = i2;
            this.mFinal = i6;
            int i11 = i2 - i6;
            this.mDeceleration = getDeceleration(i11);
            this.mVelocity = -i11;
            this.mOver = Math.abs(i11);
            this.mDuration = (int) (Math.sqrt((i11 * (-2.0f)) / this.mDeceleration) * 1000.0d);
        }

        public boolean continueWhenFinished() {
            int i2 = this.mState;
            if (i2 != 0) {
                if (i2 == 1) {
                    return false;
                }
                if (i2 == 2) {
                    this.mStartTime += (long) this.mDuration;
                    startSpringback(this.mFinal, this.mStart, 0);
                }
            } else {
                if (this.mDuration >= this.mSplineDuration) {
                    return false;
                }
                int i6 = this.mFinal;
                this.mCurrentPosition = i6;
                this.mStart = i6;
                int i10 = (int) this.mCurrVelocity;
                this.mVelocity = i10;
                this.mDeceleration = getDeceleration(i10);
                this.mStartTime += (long) this.mDuration;
                onEdgeReached();
            }
            update();
            return true;
        }

        public void finish() {
            this.mCurrentPosition = this.mFinal;
            this.mFinished = true;
        }

        public void fling(int i2, int i6, int i10, int i11, int i12) {
            double splineFlingDistance;
            this.mOver = i12;
            this.mFinished = false;
            float f2 = i6;
            this.mCurrVelocity = f2;
            this.mVelocity = i6;
            this.mDuration = 0;
            this.mSplineDuration = 0;
            this.mStartTime = AnimationUtils.currentAnimationTimeMillis();
            this.mCurrentPosition = i2;
            this.mStart = i2;
            if (i2 > i11 || i2 < i10) {
                startAfterEdge(i2, i10, i11, i6);
                return;
            }
            float f10 = this.mVelocityRatio;
            if (f10 != 1.0f) {
                i6 = (int) (f2 * f10);
                float f11 = i6;
                this.mCurrVelocity = f11;
                this.mVelocity = Math.round(f11 * f10);
            }
            this.mState = 0;
            if (i6 != 0) {
                int iRound = Math.round(getSplineFlingDuration(i6) * this.mDurationRatio);
                this.mDuration = iRound;
                this.mSplineDuration = iRound;
                splineFlingDistance = getSplineFlingDistance(i6);
            } else {
                splineFlingDistance = 0.0d;
            }
            int iSignum = (int) (splineFlingDistance * ((double) Math.signum(i6)));
            this.mSplineDistance = iSignum;
            int i13 = i2 + iSignum;
            this.mFinal = i13;
            if (i13 < i10) {
                adjustDuration(this.mStart, i13, i10);
                this.mFinal = i10;
            }
            int i14 = this.mFinal;
            if (i14 > i11) {
                adjustDuration(this.mStart, i14, i11);
                this.mFinal = i11;
            }
        }

        public void notifyEdgeReached(int i2, int i6, int i10) {
            if (this.mState == 0) {
                this.mOver = i10;
                this.mStartTime = AnimationUtils.currentAnimationTimeMillis();
                startAfterEdge(i2, i6, i6, (int) this.mCurrVelocity);
            }
        }

        public void setFinalPosition(int i2) {
            this.mFinal = i2;
            this.mSplineDistance = i2 - this.mStart;
            this.mFinished = false;
        }

        public void setFriction(float f2) {
            this.mFlingFriction = f2;
        }

        public boolean springback(int i2, int i6, int i10) {
            this.mFinished = true;
            this.mCurrentPosition = i2;
            this.mStart = i2;
            this.mFinal = i2;
            this.mVelocity = 0;
            this.mStartTime = AnimationUtils.currentAnimationTimeMillis();
            this.mDuration = 0;
            if (i2 < i6) {
                startSpringback(i2, i6, 0);
            } else if (i2 > i10) {
                startSpringback(i2, i10, 0);
            }
            return !this.mFinished;
        }

        public void startScroll(int i2, int i6, int i10) {
            this.mFinished = false;
            this.mCurrentPosition = i2;
            this.mStart = i2;
            this.mFinal = i2 + i6;
            this.mStartTime = AnimationUtils.currentAnimationTimeMillis();
            this.mDuration = i10;
            this.mDeceleration = 0.0f;
            this.mVelocity = 0;
        }

        public boolean update() {
            float f2;
            float f10;
            double d2;
            long jCurrentAnimationTimeMillis = AnimationUtils.currentAnimationTimeMillis() - this.mStartTime;
            if (jCurrentAnimationTimeMillis == 0) {
                return this.mDuration > 0;
            }
            int i2 = this.mDuration;
            if (jCurrentAnimationTimeMillis > i2) {
                return false;
            }
            int i6 = this.mState;
            if (i6 == 0) {
                int i10 = this.mSplineDuration;
                float f11 = jCurrentAnimationTimeMillis / i10;
                int i11 = (int) (f11 * 100.0f);
                if (i11 >= 100 || i11 < 0) {
                    f2 = 1.0f;
                    f10 = 0.0f;
                } else {
                    float f12 = i11 / 100.0f;
                    int i12 = i11 + 1;
                    float[] fArr = SPLINE_POSITION;
                    float f13 = fArr[i11];
                    f10 = (fArr[i12] - f13) / ((i12 / 100.0f) - f12);
                    f2 = f13 + ((f11 - f12) * f10);
                }
                int i13 = this.mSplineDistance;
                this.mCurrVelocity = ((f10 * i13) / i10) * COUILocateOverScroller.THOUSAND;
                d2 = f2 * i13;
            } else if (i6 == 1) {
                float f14 = jCurrentAnimationTimeMillis / i2;
                float f15 = f14 * f14;
                float fSignum = Math.signum(this.mVelocity);
                int i14 = this.mOver;
                double d7 = i14 * fSignum * ((THREE * f15) - ((2.0f * f14) * f15));
                this.mCurrVelocity = fSignum * i14 * SIX * ((-f14) + f15);
                d2 = d7;
            } else if (i6 != 2) {
                d2 = 0.0d;
            } else {
                float f16 = jCurrentAnimationTimeMillis / COUILocateOverScroller.THOUSAND;
                int i15 = this.mVelocity;
                float f17 = this.mDeceleration;
                this.mCurrVelocity = i15 + (f17 * f16);
                d2 = (i15 * f16) + (((f17 * f16) * f16) / 2.0f);
            }
            this.mCurrentPosition = this.mStart + ((int) Math.round(d2));
            return true;
        }

        public void updateScroll(float f2) {
            this.mCurrentPosition = this.mStart + Math.round(f2 * (this.mFinal - this.mStart));
        }
    }

    public COUILocateOverScroller(Context context) {
        this(context, null);
    }

    @Override
    public void abortAnimation() {
        this.mScrollerX.finish();
        this.mScrollerY.finish();
        this.mFrameRateHelper.setFrameRate(false);
    }

    @Override
    public boolean computeScrollOffset() {
        if (isCOUIFinished()) {
            return false;
        }
        int i2 = this.mMode;
        if (i2 == 0) {
            long jCurrentAnimationTimeMillis = AnimationUtils.currentAnimationTimeMillis() - this.mScrollerX.mStartTime;
            int i6 = this.mScrollerX.mDuration;
            if (jCurrentAnimationTimeMillis < i6) {
                float interpolation = this.mInterpolator.getInterpolation(jCurrentAnimationTimeMillis / i6);
                this.mScrollerX.updateScroll(interpolation);
                this.mScrollerY.updateScroll(interpolation);
            } else {
                abortAnimation();
            }
        } else if (i2 == 1) {
            if (!this.mScrollerX.mFinished && !this.mScrollerX.update() && !this.mScrollerX.continueWhenFinished()) {
                this.mScrollerX.finish();
            }
            if (!this.mScrollerY.mFinished && !this.mScrollerY.update() && !this.mScrollerY.continueWhenFinished()) {
                this.mScrollerY.finish();
            }
        }
        return true;
    }

    public void enableFrameRate(boolean z6) {
        this.mFrameRateHelper.enableFrameRate(z6);
    }

    @Override
    public void fling(int i2, int i6, int i10, int i11, int i12, int i13, int i14, int i15) {
        fling(i2, i6, i10, i11, i12, i13, i14, i15, 0, 0);
    }

    @Override
    public int getCOUICurrX() {
        return this.mScrollerX.mCurrentPosition;
    }

    @Override
    public int getCOUICurrY() {
        return this.mScrollerY.mCurrentPosition;
    }

    @Override
    public int getCOUIFinalX() {
        return this.mScrollerX.mFinal;
    }

    @Override
    public int getCOUIFinalY() {
        return this.mScrollerY.mFinal;
    }

    @Override
    public float getCurrVelocity() {
        return (float) Math.hypot(this.mScrollerX.mCurrVelocity, this.mScrollerY.mCurrVelocity);
    }

    @Override
    public float getCurrVelocityX() {
        return this.mScrollerX.mCurrVelocity;
    }

    @Override
    public float getCurrVelocityY() {
        return this.mScrollerY.mCurrVelocity;
    }

    @Override
    public boolean isCOUIFinished() {
        return this.mScrollerX.mFinished && this.mScrollerY.mFinished;
    }

    @Override
    public boolean isScrollingInDirection(float f2, float f10) {
        return !isFinished() && Math.signum(f2) == Math.signum((float) (this.mScrollerX.mFinal - this.mScrollerX.mStart)) && Math.signum(f10) == Math.signum((float) (this.mScrollerY.mFinal - this.mScrollerY.mStart));
    }

    @Override
    public void notifyHorizontalEdgeReached(int i2, int i6, int i10) {
        this.mScrollerX.notifyEdgeReached(i2, i6, i10);
        springBack(i2, 0, 0, 0, 0, 0);
    }

    @Override
    public void notifyVerticalEdgeReached(int i2, int i6, int i10) {
        this.mScrollerY.notifyEdgeReached(i2, i6, i10);
        springBack(0, i2, 0, 0, 0, 0);
    }

    @Override
    public void setCOUIFriction(float f2) {
    }

    @Override
    public void setCurrVelocityX(float f2) {
        this.mScrollerX.mCurrVelocity = f2;
    }

    @Override
    public void setCurrVelocityY(float f2) {
        this.mScrollerY.mCurrVelocity = f2;
    }

    @Override
    public void setDurationRatio(float f2) {
        this.mScrollerX.mDurationRatio = f2;
        this.mScrollerY.mDurationRatio = f2;
    }

    @Override
    public void setFinalX(int i2) {
        if (i2 == -1) {
            return;
        }
        this.mScrollerX.setFinalPosition(i2);
    }

    @Override
    public void setFinalY(int i2) {
        if (i2 == -1) {
            return;
        }
        this.mScrollerY.setFinalPosition(i2);
    }

    @Override
    public void setFlingFriction(float f2) {
        this.mScrollerX.setFriction(f2);
        this.mScrollerY.setFriction(f2);
    }

    @Override
    public void setInterpolator(Interpolator interpolator) {
        if (interpolator == null) {
            this.mInterpolator = SCROLL;
        } else {
            this.mInterpolator = interpolator;
        }
    }

    @Override
    public void setIsScrollView(boolean z6) {
    }

    @Override
    public void setVelocityXRatio(float f2) {
        this.mScrollerX.mVelocityRatio = f2;
    }

    @Override
    public void setVelocityYRatio(float f2) {
        this.mScrollerY.mVelocityRatio = f2;
    }

    @Override
    public boolean springBack(int i2, int i6, int i10, int i11, int i12, int i13) {
        boolean zSpringback = this.mScrollerX.springback(i2, i10, i11);
        boolean zSpringback2 = this.mScrollerY.springback(i6, i12, i13);
        if (zSpringback || zSpringback2) {
            this.mMode = 1;
        }
        return zSpringback || zSpringback2;
    }

    @Override
    public void startScroll(int i2, int i6, int i10, int i11) {
        startScroll(i2, i6, i10, i11, SCROLL_DEFAULT_DURATION);
    }

    public COUILocateOverScroller(Context context, Interpolator interpolator) {
        super(context, interpolator);
        this.mScrollerX = new COUISplineOverScroller(context);
        this.mScrollerY = new COUISplineOverScroller(context);
        if (interpolator == null) {
            this.mInterpolator = SCROLL;
        } else {
            this.mInterpolator = interpolator;
        }
        this.mFrameRateHelper = new COUlFrameRateScrollSceneHelper(false);
    }

    @Override
    public void fling(int i2, int i6, int i10, int i11, int i12, int i13, int i14, int i15, int i16, int i17) {
        if (i6 > i15 || i6 < i14) {
            springBack(i2, i6, i12, i13, i14, i15);
        } else {
            fling(i2, i6, i10, i11);
        }
    }

    @Override
    public void startScroll(int i2, int i6, int i10, int i11, int i12) {
        this.mMode = 0;
        this.mScrollerX.startScroll(i2, i10, i12);
        this.mScrollerY.startScroll(i6, i11, i12);
        this.mFrameRateHelper.setFrameRate(true);
    }

    @Override
    public void fling(int i2, int i6, int i10, int i11) {
        this.mMode = 1;
        this.mScrollerX.fling(i2, i10, Integer.MIN_VALUE, Integer.MAX_VALUE, 0);
        this.mScrollerY.fling(i6, i11, Integer.MIN_VALUE, Integer.MAX_VALUE, 0);
        this.mFrameRateHelper.setFrameRate(true);
    }
}
