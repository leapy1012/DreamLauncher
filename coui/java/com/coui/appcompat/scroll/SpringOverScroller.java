package com.coui.appcompat.scroll;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;
import android.view.Choreographer;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.OverScroller;
import com.coui.appcompat.animation.COUISpringInterpolator;
import com.coui.appcompat.log.COUILog;
import com.coui.appcompat.uiutil.UIUtil;
import java.lang.reflect.Method;


public class SpringOverScroller extends OverScroller implements COUIIOverScroller {
    public static final float COUI_FLING_FRICTION_FAST = 0.76f;
    public static final int COUI_FLING_MODE_FAST = 0;
    public static final int COUI_FLING_MODE_NORMAL = 1;
    private static boolean DEBUG = false;
    private static final float ERROR_THRESHOLD = 0.025f;
    private static final int FLING_MODE = 1;
    private static final int FLING_SPEED_INCREASE_COUNT_THRESHOLD = 4;
    private static final int FLING_SPEED_INCREASE_EDGE_REACHED_VELOCITY = 1000;
    private static final int FLING_SPEED_INCREASE_EDGE_REACHED_VELOCITY_THRESHOLD = 20000;
    private static final int FLING_SPEED_INCREASE_MAX_VELOCITY = 70000;
    private static final float FLING_SPEED_INCREASE_RATE = 1.4f;
    private static final int FLING_SPEED_INCREASE_TIME_INTERVAL_THRESHOLD = 500;
    private static final int FLING_SPEED_INCREASE_VELOCITY_THRESHOLD = 8000;
    private static final float MIN_FRAME_INTERVAL = 0.008f;
    private static final double MIN_UPDATE_ONE_STEP = 0.5d;
    private static final float NANO_ONE_SECOND = 1.0E9f;
    private static final float NANO_TO_MILLIS = 1000000.0f;
    private static final float ONE_SECOND = 1000.0f;
    private static final int REST_MODE = 2;
    private static final int SCROLL_DEFAULT_DURATION = 250;
    private static final int SCROLL_MODE = 0;
    private static final float SOLVER_TIMESTEP_SEC = 0.016f;
    public static final String TAG = "SpringOverScroller";
    private static final int VSYNC_DURATION = 5000;
    private static float mRefreshTime = 0.0f;
    private static float mSpringBackFriction = 12.19f;
    private final Choreographer.FrameCallback mCallback;
    private boolean mCancelCallback;
    private Context mContext;
    private int mContinuousFlingCount;
    private long mCurrentComputeTimeFromCallback;
    private boolean mEnableFlingSpeedIncrease;
    private COUlFrameRateScrollSceneHelper mFrameRateHelper;
    private Interpolator mInterpolator;
    private boolean mIsUpdateTimeFromCallback;
    private long mLastComputeTimeFromCallback;
    private float mLastFlingSpeedIncreaseRate;
    private long mLastFlingTime;
    private int mMode;
    protected ReboundOverScroller mScrollerX;
    protected ReboundOverScroller mScrollerY;

    public static class COUIViscousFluidInterpolator implements Interpolator {
        private static final float VISCOUS_FLUID_NORMALIZE;
        private static final float VISCOUS_FLUID_OFFSET;
        private static final float VISCOUS_FLUID_SCALE = 8.0f;

        static {
            float fViscousFluid = 1.0f / viscousFluid(1.0f);
            VISCOUS_FLUID_NORMALIZE = fViscousFluid;
            VISCOUS_FLUID_OFFSET = 1.0f - (fViscousFluid * viscousFluid(1.0f));
        }

        private static float viscousFluid(float f2) {
            float f10 = f2 * VISCOUS_FLUID_SCALE;
            return f10 < 1.0f ? f10 - (1.0f - ((float) Math.exp(-f10))) : 0.36787945f + ((1.0f - ((float) Math.exp(1.0f - f10))) * 0.63212055f);
        }

        @Override
        public float getInterpolation(float f2) {
            float fViscousFluid = VISCOUS_FLUID_NORMALIZE * viscousFluid(f2);
            return fViscousFluid > 0.0f ? fViscousFluid + VISCOUS_FLUID_OFFSET : fViscousFluid;
        }
    }

    public static class ReboundOverScroller {
        private static final float DEFAULT_CUTRATIO = 1.0f;
        private static final double DEFAULT_DAMPINGRATIO = 1.15d;
        private static final double DEFAULT_STIFFNESS = 40.0d;
        private static final float DEFAULT_VELOCITY_UNIT = 15000.0f;
        private static final double DELTA_POSITION_HIGH = 0.35d;
        private static final double DELTA_POSITION_LOW = 0.2d;
        private static final double DELTA_POSITION_MID = 0.3d;
        private static final double DELTA_POSITION_VELOCITY_HIGH = 8000.0d;
        private static final double DELTA_POSITION_VELOCITY_LOW = 5000.0d;
        private static final float DISTANCE_SCALE_MAX = 1.0f;
        private static final float DISTANCE_SCALE_MID = 0.8f;
        private static final float DISTANCE_SCALE_MIN = 0.4f;
        private static final float DURATION_SCALE_MAX = 1.3f;
        private static final float DURATION_SCALE_MID = 0.8f;
        private static final float DURATION_SCALE_MIN = 0.5f;
        private static final float FLING_CHANGE_INCREASE_STEP = 1.2f;
        private static final float FLING_CHANGE_REDUCE_STEP = 0.6f;
        private static final float FLING_DXDT_RATIO = 0.167f;
        private static final double FLING_FRICTION_DIVISOR = 10000.0d;
        private static final float FLING_VELOCITY_HIGH = 10000.0f;
        private static final float FLING_VELOCITY_HIGHEST = 20000.0f;
        private static final float FLING_VELOCITY_LOW = 6000.0f;
        private static final float FLING_VELOCITY_LOWEST = 2000.0f;
        private static final float FLOAT_1 = 1.0f;
        private static final float FLOAT_2 = 2.0f;
        private static final double INCREASE_FRICTION_COEF = 0.00125d;
        private static final double MAX_VELOCITY_ADJUST_FRICTION = 10000.0d;
        private static final double MID_VELOCITY_ADJUST_FRICTION = 4000.0d;
        private static final double MIN_FLING_FRICTION_REDUCE = 2.0d;
        private static final double MIN_VELOCITY_ADJUST_FRICTION = 1000.0d;
        public static final long NANOS_PER_MS = 1000000;
        private static final int NUM_60 = 60;
        private static final float ONE_MILLION = 1.0E-7f;
        private static final double REDUCE_FRICTION_COEF = 0.00125d;
        private static final float REST_SPEED_THRESHOLD = 5.0f;
        private static final int SPRING_BACK_ADJUST_TENSION_VALUE = 100;
        private static final int SPRING_BACK_ADJUST_THRESHOLD = 180;
        private static final float SPRING_BACK_FRICTION = 12.19f;
        private static final float SPRING_BACK_STOP_THRESHOLD = 0.25f;
        private static final float SPRING_BACK_TENSION = 16.0f;
        private static final long TIME_ADJUST_FRICTION = 480;
        private static final double VELOCITY_REDUCE_FRICTION = 2000.0d;
        private static float sCouiFlingFrictionNormal = 0.2f;
        private static double sMidFlingBaseFriction = 2.5d;
        private static double sSlowFlingBaseFriction = 2.5d;
        private static float sTimeIncrease = 1.0f;
        private int mCOUICount;
        private boolean mCancelCallback;
        private Choreographer mChoreographer;
        private boolean mComputeTimeFromCallbackUpdated;
        private ReboundConfig mConfig;
        private long mCurrentComputeTime;
        private long mCurrentComputeTimeFromCallback;
        private double mDisplacementFromRestThreshold;
        private int mDuration;
        private double mEndValue;
        private boolean mFinished;
        private ReboundConfig mFlingConfig;
        private float mFlingFriction;
        private Method mGetFrameIntervalNanos;
        private boolean mIsScrollView;
        private boolean mIsSpringBack;
        private long mLastComputeTime;
        private long mLastComputeTimeFromCallback;
        private long mLastFlingUpdateTime;
        private double mRestSpeedThreshold;
        private int mScrollFinal;
        private int mScrollStart;
        private long mScrollStartTime;
        private int mSimulateDuration;
        private int mSimulateSplineDistance;
        private int mSimulateSplineDuration;
        private int mSplineDistance;
        private int mSplineDuration;
        private double mSplineMinDelta;
        private ReboundConfig mSpringBackConfig;
        private float mSpringBackTensionMultiple;
        private COUISpringInterpolator mSpringInterpolator;
        private long mStartTime;
        private double mStartValue;
        private boolean mTensionAdjusted;
        private boolean mWithSpring;
        private PhysicsState mCurrentState = new PhysicsState();
        private PhysicsState mPreviousState = new PhysicsState();
        private PhysicsState mTempState = new PhysicsState();
        private Rk4Data mRk4Result = new Rk4Data(0.0d, 0.0d, 0.0d, 0.0d);

        public static class PhysicsState {
            double mPosition;
            double mVelocity;
        }

        public static class ReboundConfig {
            double mFriction;
            double mTension;

            public ReboundConfig(double d2, double d7) {
                this.mFriction = frictionFromOrigamiValue((float) d2);
                this.mTension = tensionFromOrigamiValue((float) d7);
            }

            private float frictionFromOrigamiValue(float f2) {
                if (f2 == 0.0f) {
                    return 0.0f;
                }
                return 25.0f + ((f2 - 8.0f) * 3.0f);
            }

            private double tensionFromOrigamiValue(float f2) {
                if (f2 == 0.0f) {
                    return 0.0d;
                }
                return ((f2 - 30.0f) * 3.62f) + 194.0f;
            }

            public void setFriction(double d2) {
                this.mFriction = frictionFromOrigamiValue((float) d2);
            }

            public void setTension(double d2) {
                this.mTension = tensionFromOrigamiValue((float) d2);
            }
        }

        public static class Rk4Data {
            double mCurrentPosition;
            double mCurrentVelocity;
            double mTempPosition;
            double mTempVelocity;

            public Rk4Data(double d2, double d7, double d10, double d11) {
                this.mCurrentPosition = d2;
                this.mCurrentVelocity = d7;
                this.mTempPosition = d10;
                this.mTempVelocity = d11;
            }
        }

        public ReboundOverScroller() {
            float f2 = sCouiFlingFrictionNormal;
            this.mFlingFriction = f2;
            this.mRestSpeedThreshold = 20.0d;
            this.mDisplacementFromRestThreshold = 0.05d;
            this.mCOUICount = 1;
            this.mIsScrollView = false;
            this.mSpringBackTensionMultiple = 0.83f;
            this.mFlingConfig = new ReboundConfig(f2, 0.0d);
            this.mSpringBackConfig = new ReboundConfig(12.1899995803833d, 16.0d);
            setConfig(this.mFlingConfig);
            this.mFinished = true;
            int animLevel = UIUtil.getAnimLevel();
            if (animLevel == 2) {
                sMidFlingBaseFriction = 3.799999952316284d;
                sSlowFlingBaseFriction = 3.4000000953674316d;
            } else if (animLevel >= 3) {
                sMidFlingBaseFriction = 4.5d;
                sSlowFlingBaseFriction = 4.0d;
                sCouiFlingFrictionNormal = 0.24f;
            }
        }

        private void adjustFrictionByStartVelocity() {
            if (this.mIsSpringBack || this.mCOUICount != 1) {
                return;
            }
            if (Math.abs(this.mCurrentState.mVelocity) > MID_VELOCITY_ADJUST_FRICTION && Math.abs(this.mCurrentState.mVelocity) < 10000.0d) {
                this.mConfig.mFriction = sMidFlingBaseFriction;
            } else if (Math.abs(this.mCurrentState.mVelocity) <= MID_VELOCITY_ADJUST_FRICTION) {
                this.mConfig.mFriction = sSlowFlingBaseFriction;
            }
        }

        private void calculateCurStateWithInterpolator(float f2) {
            float interpolation = this.mSpringInterpolator.getInterpolation(f2);
            PhysicsState physicsState = this.mCurrentState;
            physicsState.mPosition = ((double) (this.mSimulateSplineDistance * interpolation)) + this.mStartValue;
            physicsState.mVelocity = ((this.mSpringInterpolator.getSpeed(f2) * this.mSimulateSplineDistance) / this.mSimulateSplineDuration) * SpringOverScroller.ONE_SECOND;
            if (SpringOverScroller.DEBUG) {
                Log.d(SpringOverScroller.TAG, " calculateCurStateWithInterpolator fraction:" + f2 + ", ratio: " + interpolation + ", position : " + this.mCurrentState.mPosition + ", mVelocity: " + this.mCurrentState.mVelocity);
            }
            this.mCOUICount++;
        }

        private int[] calculateFinalPosition(double d2, boolean z6) {
            PhysicsState physicsState = this.mCurrentState;
            int i2 = (int) physicsState.mPosition;
            int i6 = (int) physicsState.mVelocity;
            int i10 = this.mCOUICount;
            boolean z10 = this.mFinished;
            PhysicsState physicsState2 = this.mTempState;
            physicsState2.mPosition = 0.0d;
            physicsState2.mVelocity = 0.0d;
            float frameIntervalNanos = getFrameIntervalNanos() / SpringOverScroller.NANO_TO_MILLIS;
            float f2 = SpringOverScroller.ONE_SECOND;
            float unused = SpringOverScroller.mRefreshTime = frameIntervalNanos / SpringOverScroller.ONE_SECOND;
            if (SpringOverScroller.mRefreshTime == 0.0f) {
                calculateRefreshTime();
            }
            if (SpringOverScroller.DEBUG) {
                Log.d(SpringOverScroller.TAG, this + " calculateFinalPosition finalValue " + d2 + " savedPosition " + i2 + " savedVelocity " + i6 + ", mRefreshTime: " + SpringOverScroller.mRefreshTime);
            }
            boolean z11 = true;
            this.mCOUICount = 1;
            boolean z12 = false;
            while (!this.mFinished) {
                double d7 = this.mCurrentState.mPosition;
                if (z6) {
                    calculateCurStateWithInterpolator(((this.mCOUICount * SpringOverScroller.mRefreshTime) * f2) / this.mSimulateSplineDuration);
                } else {
                    calculateOnceWithRebound();
                }
                double d10 = this.mCurrentState.mPosition;
                double dAbs = Math.abs(d10 - d7);
                if (lostVelocity()) {
                    if (SpringOverScroller.DEBUG) {
                        Log.d(SpringOverScroller.TAG, this + " calculateFinalPosition lostVelocity");
                    }
                    this.mFinished = z11;
                }
                if (dAbs < this.mSplineMinDelta) {
                    if (SpringOverScroller.DEBUG) {
                        Log.d(SpringOverScroller.TAG, this + " calculateFinalPosition deltaPosition < " + this.mSplineMinDelta);
                    }
                    z11 = true;
                    this.mFinished = true;
                } else {
                    z11 = true;
                }
                if (d2 != -1.0d && !z12 && (d7 - d2) * (d10 - d2) <= 0.0d) {
                    this.mCurrentState.mPosition = d2;
                    if (z6) {
                        this.mDuration = (int) (this.mCOUICount * SpringOverScroller.mRefreshTime * SpringOverScroller.ONE_SECOND);
                    } else {
                        this.mSimulateDuration = (int) (this.mCOUICount * SpringOverScroller.mRefreshTime * SpringOverScroller.ONE_SECOND);
                    }
                    if (SpringOverScroller.DEBUG) {
                        Log.d(SpringOverScroller.TAG, this + " calculateFinalPosition reaching edge" + d2);
                    }
                    z12 = z11;
                }
                f2 = SpringOverScroller.ONE_SECOND;
            }
            int i11 = (int) this.mCurrentState.mPosition;
            int i12 = (int) (this.mCOUICount * SpringOverScroller.mRefreshTime * SpringOverScroller.ONE_SECOND);
            PhysicsState physicsState3 = this.mCurrentState;
            physicsState3.mPosition = i2;
            physicsState3.mVelocity = i6;
            this.mCOUICount = i10;
            PhysicsState physicsState4 = this.mTempState;
            physicsState4.mPosition = 0.0d;
            physicsState4.mVelocity = 0.0d;
            this.mFinished = z10;
            return new int[]{i11, i12};
        }

        private void calculateOnceWithRebound() {
            adjustFrictionByStartVelocity();
            Rk4Data rk4Data = this.mRk4Result;
            PhysicsState physicsState = this.mCurrentState;
            rk4Data.mCurrentPosition = physicsState.mPosition;
            rk4Data.mCurrentVelocity = physicsState.mVelocity;
            rk4Data.mTempPosition = this.mTempState.mPosition;
            Rk4Data rk4DataCalculateWithRk4 = calculateWithRk4(rk4Data, this.mConfig, this.mEndValue, SpringOverScroller.mRefreshTime);
            this.mCOUICount++;
            PhysicsState physicsState2 = this.mCurrentState;
            physicsState2.mPosition = rk4DataCalculateWithRk4.mCurrentPosition;
            physicsState2.mVelocity = rk4DataCalculateWithRk4.mCurrentVelocity;
            PhysicsState physicsState3 = this.mTempState;
            physicsState3.mPosition = rk4DataCalculateWithRk4.mTempPosition;
            physicsState3.mVelocity = rk4DataCalculateWithRk4.mTempVelocity;
        }

        private Rk4Data calculateWithRk4(Rk4Data rk4Data, ReboundConfig reboundConfig, double d2, float f2) {
            double d7 = rk4Data.mCurrentPosition;
            double d10 = rk4Data.mCurrentVelocity;
            double d11 = rk4Data.mTempPosition;
            double d12 = reboundConfig.mTension;
            double d13 = reboundConfig.mFriction;
            double d14 = (d2 - d11) * d12;
            double d15 = f2;
            double d16 = ((d10 * d15) / MIN_FLING_FRICTION_REDUCE) + d7;
            double d17 = d10 + ((d14 * d15) / MIN_FLING_FRICTION_REDUCE);
            double d18 = ((d2 - d16) * d12) - (d13 * d17);
            double d19 = d7 + ((d17 * d15) / MIN_FLING_FRICTION_REDUCE);
            double d20 = d10 + ((d18 * d15) / MIN_FLING_FRICTION_REDUCE);
            double d21 = ((d2 - d19) * d12) - (d13 * d20);
            double d22 = d7 + (d20 * d15);
            double d23 = d10 + (d21 * d15);
            double d24 = (d10 + ((d17 + d20) * MIN_FLING_FRICTION_REDUCE) + d23) * 0.16699999570846558d;
            double d25 = (d14 + ((d18 + d21) * MIN_FLING_FRICTION_REDUCE) + (((d2 - d22) * d12) - (d13 * d23))) * 0.16699999570846558d;
            double d26 = d7 + (d24 * d15);
            double d27 = d10 + (d25 * d15);
            rk4Data.mCurrentPosition = d26;
            rk4Data.mCurrentVelocity = d27;
            rk4Data.mTempPosition = d22;
            rk4Data.mTempVelocity = d23;
            if (SpringOverScroller.DEBUG) {
                Log.d(SpringOverScroller.TAG, " calculateOnceWithRebound, position : " + d26 + ", mVelocity: " + d27 + ",tempPosition:" + d22 + ",tempVelocity:" + d23 + ",tension:" + d12 + ",friction:" + d13 + ",refreshTime:" + f2);
            }
            return rk4Data;
        }

        private float getDistanceScale(float f2) {
            float f10;
            float f11 = 1.0f;
            if (f2 <= FLING_VELOCITY_LOWEST) {
                return 1.0f;
            }
            if (f2 <= FLING_VELOCITY_LOW) {
                f10 = ((f2 - FLING_VELOCITY_LOWEST) / 4000.0f) * 0.19999999f;
            } else {
                f11 = 0.8f;
                if (f2 <= 10000.0f) {
                    return 0.8f;
                }
                if (f2 > FLING_VELOCITY_HIGHEST) {
                    return DISTANCE_SCALE_MIN;
                }
                f10 = ((f2 - 10000.0f) / 10000.0f) * DISTANCE_SCALE_MIN;
            }
            return f11 - f10;
        }

        private float getDurationScale(float f2) {
            float f10;
            float f11;
            float f12 = DURATION_SCALE_MAX;
            if (f2 <= FLING_VELOCITY_LOWEST) {
                return DURATION_SCALE_MAX;
            }
            if (f2 <= FLING_VELOCITY_LOW) {
                f10 = (f2 - FLING_VELOCITY_LOWEST) / 4000.0f;
                f11 = 0.49999994f;
            } else {
                f12 = 0.8f;
                if (f2 <= 10000.0f) {
                    return 0.8f;
                }
                if (f2 > FLING_VELOCITY_HIGHEST) {
                    return 0.5f;
                }
                f10 = (f2 - 10000.0f) / 10000.0f;
                f11 = 0.3f;
            }
            return f12 - (f10 * f11);
        }

        private long getFrameIntervalNanos() {
            try {
                if (this.mChoreographer == null) {
                    this.mChoreographer = Choreographer.getInstance();
                }
                if (this.mGetFrameIntervalNanos == null) {
                    Method declaredMethod = Class.forName("android.view.Choreographer").getDeclaredMethod("getFrameIntervalNanos", new Class[0]);
                    this.mGetFrameIntervalNanos = declaredMethod;
                    declaredMethod.setAccessible(true);
                }
                return ((Long) this.mGetFrameIntervalNanos.invoke(this.mChoreographer, new Object[0])).longValue();
            } catch (Exception e2) {
                if (!SpringOverScroller.DEBUG) {
                    return 0L;
                }
                Log.e(SpringOverScroller.TAG, "getFrameIntervalNanos error" + e2);
                return 0L;
            }
        }

        private float getSplineMinVelocity() {
            return REST_SPEED_THRESHOLD;
        }

        private boolean lostVelocity() {
            if (Math.abs(this.mCurrentState.mVelocity) >= getSplineMinVelocity()) {
                return false;
            }
            if (!SpringOverScroller.DEBUG) {
                return true;
            }
            Log.d(SpringOverScroller.TAG, this + " lostVelocity");
            return true;
        }


        public void updateComputeTimeFromCallback(long j2) {
            this.mLastComputeTimeFromCallback = this.mCurrentComputeTimeFromCallback;
            this.mCurrentComputeTimeFromCallback = j2;
            this.mComputeTimeFromCallbackUpdated = true;
        }

        public void adjustSimulateSplineDistance(float f2) {
            int i2 = (int) f2;
            this.mSimulateSplineDistance = i2;
            this.mEndValue = this.mStartValue + ((double) i2);
            if (SpringOverScroller.DEBUG) {
                Log.d(SpringOverScroller.TAG, "adjustSimulateSplineDistance: StartValue = " + this.mStartValue + " EndValue = " + this.mEndValue + " SimulateSplineDistance = " + this.mSimulateSplineDistance);
            }
        }

        public void calculateRefreshTime() {
            this.mCurrentComputeTime = SystemClock.elapsedRealtime();
            if (this.mComputeTimeFromCallbackUpdated) {
                this.mComputeTimeFromCallbackUpdated = false;
                if (SpringOverScroller.DEBUG) {
                    Log.d(SpringOverScroller.TAG, "update if: " + ((this.mCurrentComputeTimeFromCallback - this.mLastComputeTimeFromCallback) / SpringOverScroller.NANO_ONE_SECOND));
                }
                float unused = SpringOverScroller.mRefreshTime = Math.max(SpringOverScroller.MIN_FRAME_INTERVAL, (this.mCurrentComputeTimeFromCallback - this.mLastComputeTimeFromCallback) / SpringOverScroller.NANO_ONE_SECOND);
            } else {
                if (SpringOverScroller.DEBUG) {
                    Log.d(SpringOverScroller.TAG, "update else: " + ((this.mCurrentComputeTime - this.mLastComputeTime) / SpringOverScroller.ONE_SECOND));
                }
                float unused2 = SpringOverScroller.mRefreshTime = Math.max(SpringOverScroller.MIN_FRAME_INTERVAL, (this.mCurrentComputeTime - this.mLastComputeTime) / SpringOverScroller.ONE_SECOND);
            }
            if (SpringOverScroller.mRefreshTime > SpringOverScroller.ERROR_THRESHOLD) {
                if (SpringOverScroller.DEBUG) {
                    Log.d(SpringOverScroller.TAG, "update: error mRefreshTime = " + SpringOverScroller.mRefreshTime);
                }
                float unused3 = SpringOverScroller.mRefreshTime = SpringOverScroller.MIN_FRAME_INTERVAL;
            }
            if (SpringOverScroller.DEBUG) {
                Log.d(SpringOverScroller.TAG, "update: mRefreshTime = " + SpringOverScroller.mRefreshTime + " mLastComputeTime = " + this.mLastComputeTime);
            }
            this.mLastComputeTime = this.mCurrentComputeTime;
        }

        public void fling(int i2, int i6, int i10, int i11, int i12) {
            if (SpringOverScroller.DEBUG) {
                Log.d(SpringOverScroller.TAG, this + " fling start " + i2 + " min " + i6 + " max " + i10 + " velocity " + i11 + " over " + i12);
            }
            this.mSplineMinDelta = getSplineMinDelta(Math.abs(i11));
            this.mFinished = false;
            initFling(i2, i11);
            this.mSplineDuration = 0;
            this.mDuration = 0;
            this.mSimulateSplineDuration = 0;
            this.mSimulateDuration = 0;
            this.mSpringInterpolator = new COUISpringInterpolator(DEFAULT_STIFFNESS, DEFAULT_DAMPINGRATIO, i11, 1.0f, DEFAULT_VELOCITY_UNIT, true);
            initFling(i2, i11);
            float durationScale = getDurationScale(Math.abs(i11));
            float distanceScale = getDistanceScale(Math.abs(i11));
            double d2 = i11 >= 0 ? i10 : i6;
            int[] iArrCalculateFinalPosition = calculateFinalPosition(d2, false);
            int i13 = iArrCalculateFinalPosition[0] - i2;
            int i14 = iArrCalculateFinalPosition[1];
            int i15 = this.mSimulateDuration;
            if (i15 == 0) {
                i15 = i14;
            }
            this.mSimulateDuration = i15;
            this.mSimulateSplineDistance = (int) (i13 * distanceScale);
            this.mSimulateSplineDuration = (int) (i14 * durationScale);
            int[] iArrCalculateFinalPosition2 = calculateFinalPosition(d2, true);
            this.mSplineDistance = iArrCalculateFinalPosition2[0] - i2;
            int i16 = iArrCalculateFinalPosition2[1];
            this.mSplineDuration = i16;
            int i17 = this.mDuration;
            if (i17 == 0) {
                i17 = i16;
            }
            this.mDuration = i17;
            this.mWithSpring = i16 == i17;
            if (SpringOverScroller.DEBUG) {
                Log.d(SpringOverScroller.TAG, this + " fling mStartTime " + this.mStartTime + " mStart " + this.mStartValue + " edge " + d2 + " distanceScale " + distanceScale + " durationScale " + durationScale + " mWithSpring " + this.mWithSpring + " [ Distance_old " + i13 + " Distance_new " + this.mSplineDistance + " ] [ SplineDuration_old " + i14 + " Duration_old " + this.mSimulateDuration + " SplineDuration_new " + this.mSplineDuration + " mSimulateSplineDistance " + this.mSimulateSplineDistance + " Duration_new " + this.mDuration + " ]");
            }
            if (this.mWithSpring) {
                this.mEndValue = Math.max(Math.min(iArrCalculateFinalPosition2[0], i10), i6);
                return;
            }
            this.mSplineDistance = i13;
            this.mSplineDuration = i14;
            this.mDuration = this.mSimulateDuration;
            this.mEndValue = Math.max(Math.min(iArrCalculateFinalPosition[0], i10), i6);
        }

        public double getCurrentValue() {
            return this.mCurrentState.mPosition;
        }

        public double getDisplacementDistanceForState(PhysicsState physicsState) {
            return Math.abs(this.mEndValue - physicsState.mPosition);
        }

        public double getDisplacementFromRestThreshold() {
            return this.mDisplacementFromRestThreshold;
        }

        public double getEndValue() {
            return this.mEndValue;
        }

        public double getRestSpeedThreshold() {
            return this.mRestSpeedThreshold;
        }

        public int getSimulateSplineDistance() {
            return this.mSimulateSplineDistance;
        }

        public double getSplineMinDelta(float f2) {
            double d2 = f2;
            return d2 <= DELTA_POSITION_VELOCITY_LOW ? DELTA_POSITION_LOW : d2 <= DELTA_POSITION_VELOCITY_HIGH ? DELTA_POSITION_MID : DELTA_POSITION_HIGH;
        }

        public double getVelocity() {
            return this.mCurrentState.mVelocity;
        }

        public void initFling(int i2, int i6) {
            long jCurrentAnimationTimeMillis = AnimationUtils.currentAnimationTimeMillis();
            this.mStartTime = jCurrentAnimationTimeMillis;
            this.mLastFlingUpdateTime = jCurrentAnimationTimeMillis;
            this.mCOUICount = 1;
            sTimeIncrease = 1.0f;
            this.mFlingConfig.setFriction(this.mFlingFriction);
            this.mFlingConfig.setTension(0.0d);
            setConfig(this.mFlingConfig);
            setCurrentValue(i2, true);
            setVelocity(i6);
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            this.mLastComputeTime = jElapsedRealtime;
            this.mCurrentComputeTime = jElapsedRealtime;
        }

        public boolean isAtRest() {
            return Math.abs(this.mCurrentState.mVelocity) <= getRestSpeedThreshold() && (getDisplacementDistanceForState(this.mCurrentState) <= getDisplacementFromRestThreshold() || this.mConfig.mTension == 0.0d);
        }

        public void notifyEdgeReached(int i2, int i6, int i10) {
            PhysicsState physicsState = this.mCurrentState;
            physicsState.mPosition = i2;
            PhysicsState physicsState2 = this.mPreviousState;
            physicsState2.mPosition = 0.0d;
            physicsState2.mVelocity = 0.0d;
            PhysicsState physicsState3 = this.mTempState;
            physicsState3.mPosition = i6;
            physicsState3.mVelocity = physicsState.mVelocity;
        }

        public void setAtRest() {
            PhysicsState physicsState = this.mCurrentState;
            double d2 = physicsState.mPosition;
            this.mEndValue = d2;
            this.mTempState.mPosition = d2;
            physicsState.mVelocity = 0.0d;
            this.mIsSpringBack = false;
            this.mCancelCallback = true;
        }

        public void setConfig(ReboundConfig reboundConfig) {
            if (reboundConfig == null) {
                throw new IllegalArgumentException("springConfig is required");
            }
            this.mConfig = reboundConfig;
        }

        public void setCurrentValue(double d2, boolean z6) {
            this.mStartValue = d2;
            if (!this.mIsScrollView) {
                this.mPreviousState.mPosition = 0.0d;
                this.mTempState.mPosition = 0.0d;
            }
            this.mCurrentState.mPosition = d2;
            if (z6) {
                setAtRest();
            }
        }

        public void setEndValue(double d2) {
            if (this.mEndValue == d2) {
                return;
            }
            this.mStartValue = getCurrentValue();
            this.mEndValue = d2;
            this.mFinished = false;
        }

        public void setVelocity(double d2) {
            if (Math.abs(d2 - this.mCurrentState.mVelocity) < 1.0000000116860974E-7d) {
                return;
            }
            this.mCurrentState.mVelocity = d2;
        }

        public boolean springBack(int i2, int i6, int i10, boolean z6) {
            double d2 = i2;
            setCurrentValue(d2, false);
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            this.mLastComputeTime = jElapsedRealtime;
            this.mCurrentComputeTime = jElapsedRealtime;
            if (i2 <= i10 && i2 >= i6 && !z6) {
                setConfig(new ReboundConfig(this.mFlingFriction, 0.0d));
                return false;
            }
            if (i2 > i10) {
                setEndValue(i10);
            } else if (i2 < i6) {
                setEndValue(i6);
            } else if (z6) {
                setEndValue(d2);
            }
            this.mIsSpringBack = true;
            this.mSpringBackConfig.setFriction(SpringOverScroller.mSpringBackFriction);
            this.mSpringBackConfig.setTension(this.mSpringBackTensionMultiple * 16.0f);
            setConfig(this.mSpringBackConfig);
            return true;
        }

        public void startScroll(int i2, int i6, int i10, long j2) {
            this.mScrollStart = i2;
            int i11 = i2 + i6;
            this.mScrollFinal = i11;
            this.mEndValue = i11;
            this.mDuration = i10;
            this.mScrollStartTime = j2;
            setConfig(this.mFlingConfig);
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            this.mLastComputeTime = jElapsedRealtime;
            this.mCurrentComputeTime = jElapsedRealtime;
        }

        public boolean update() {
            calculateRefreshTime();
            if (this.mIsSpringBack) {
                if (isAtRest()) {
                    return false;
                }
                PhysicsState physicsState = this.mCurrentState;
                double d2 = physicsState.mPosition;
                double displacementDistanceForState = getDisplacementDistanceForState(physicsState);
                if (!this.mTensionAdjusted && displacementDistanceForState < 180.0d) {
                    this.mTensionAdjusted = true;
                } else if (displacementDistanceForState < 0.25d) {
                    this.mCurrentState.mPosition = this.mEndValue;
                    this.mTensionAdjusted = false;
                    this.mIsSpringBack = false;
                    this.mCancelCallback = true;
                    return false;
                }
                do {
                    Rk4Data rk4Data = this.mRk4Result;
                    PhysicsState physicsState2 = this.mCurrentState;
                    rk4Data.mCurrentPosition = physicsState2.mPosition;
                    rk4Data.mCurrentVelocity = physicsState2.mVelocity;
                    rk4Data.mTempPosition = this.mTempState.mPosition;
                    Rk4Data rk4DataCalculateWithRk4 = calculateWithRk4(rk4Data, this.mConfig, this.mEndValue, SpringOverScroller.mRefreshTime);
                    PhysicsState physicsState3 = this.mTempState;
                    physicsState3.mVelocity = rk4DataCalculateWithRk4.mTempVelocity;
                    physicsState3.mPosition = rk4DataCalculateWithRk4.mTempPosition;
                    PhysicsState physicsState4 = this.mCurrentState;
                    physicsState4.mVelocity = rk4DataCalculateWithRk4.mCurrentVelocity;
                    double d7 = rk4DataCalculateWithRk4.mCurrentPosition;
                    physicsState4.mPosition = d7;
                    if (Math.abs(d2 - d7) > SpringOverScroller.MIN_UPDATE_ONE_STEP || !this.mIsSpringBack) {
                        break;
                    }
                } while (!isAtRest());
                this.mCOUICount++;
            } else {
                if (isAtRest()) {
                    this.mCancelCallback = true;
                    return false;
                }
                long jCurrentAnimationTimeMillis = AnimationUtils.currentAnimationTimeMillis();
                float frameIntervalNanos = getFrameIntervalNanos();
                if (frameIntervalNanos != 0.0f) {
                    float unused = SpringOverScroller.mRefreshTime = Math.min((frameIntervalNanos / SpringOverScroller.ONE_SECOND) / SpringOverScroller.NANO_TO_MILLIS, (jCurrentAnimationTimeMillis - this.mLastFlingUpdateTime) / SpringOverScroller.ONE_SECOND);
                }
                this.mLastFlingUpdateTime = jCurrentAnimationTimeMillis;
                double d10 = this.mCurrentState.mPosition;
                if (!this.mWithSpring || this.mIsSpringBack) {
                    calculateOnceWithRebound();
                } else {
                    if (this.mSimulateSplineDuration <= 0) {
                        if (SpringOverScroller.DEBUG) {
                            Log.d(SpringOverScroller.TAG, this + " update end : SPLINE OSpring error duration");
                        }
                        return false;
                    }
                    calculateCurStateWithInterpolator(Math.max(jCurrentAnimationTimeMillis - this.mStartTime, 0.0f) / this.mSimulateSplineDuration);
                }
                double d11 = this.mCurrentState.mPosition;
                double dAbs = Math.abs(d11 - d10);
                if (!this.mWithSpring && dAbs < this.mSplineMinDelta && SpringOverScroller.mRefreshTime != 0.0f) {
                    if (SpringOverScroller.DEBUG) {
                        Log.d(SpringOverScroller.TAG, this + " update end : deltaPosition < " + DELTA_POSITION_LOW);
                    }
                    return false;
                }
                if (this.mWithSpring && lostVelocity()) {
                    if (SpringOverScroller.DEBUG) {
                        Log.d(SpringOverScroller.TAG, this + " update end : lostVelocity when BALLISTIC (or only SPLINE)");
                    }
                    return false;
                }
                double d12 = this.mEndValue;
                if ((d10 - d12) * (d11 - d12) <= 0.0d) {
                    this.mCurrentState.mPosition = d12;
                    if (SpringOverScroller.DEBUG) {
                        Log.d(SpringOverScroller.TAG, this + " update end : reaching final " + this.mEndValue);
                    }
                    return false;
                }
                if (Double.isNaN(this.mCurrentState.mVelocity) || Double.isNaN(this.mCurrentState.mPosition)) {
                    if (SpringOverScroller.DEBUG) {
                        Log.d(SpringOverScroller.TAG, this + " update end : mVelocity or mPosition NaN ");
                    }
                    return false;
                }
            }
            if (SpringOverScroller.DEBUG) {
                Log.d(SpringOverScroller.TAG, this + " <<< FLING_MODE: update mSplineDuration:" + this.mSplineDuration + " ,elapsedInternalTime:" + (this.mLastFlingUpdateTime - this.mStartTime) + " ,mFinal:" + this.mEndValue + " ,position:" + this.mCurrentState.mPosition + " ,velocity:" + this.mCurrentState.mVelocity + " ,tension: " + this.mConfig.mTension + " ,friction: " + this.mConfig.mFriction + " ,mOplusCount:" + this.mCOUICount + " >>> ");
            }
            return true;
        }

        public void updateScroll(float f2) {
            PhysicsState physicsState = this.mCurrentState;
            int i2 = this.mScrollStart;
            physicsState.mPosition = i2 + Math.round(f2 * (this.mScrollFinal - i2));
        }
    }

    static {
        DEBUG = COUILog.LOG_DEBUG || COUILog.isLoggable(TAG, 3);
    }

    public SpringOverScroller(Context context, Interpolator interpolator) {
        super(context, interpolator);
        this.mMode = 2;
        this.mEnableFlingSpeedIncrease = true;
        this.mLastFlingSpeedIncreaseRate = 1.0f;
        this.mIsUpdateTimeFromCallback = false;
        this.mCallback = new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long j2) {
                ReboundOverScroller reboundOverScroller = SpringOverScroller.this.mScrollerX;
                if (reboundOverScroller != null) {
                    reboundOverScroller.updateComputeTimeFromCallback(j2);
                }
                ReboundOverScroller reboundOverScroller2 = SpringOverScroller.this.mScrollerY;
                if (reboundOverScroller2 != null) {
                    reboundOverScroller2.updateComputeTimeFromCallback(j2);
                }
                SpringOverScroller springOverScroller = SpringOverScroller.this;
                springOverScroller.mLastComputeTimeFromCallback = springOverScroller.mCurrentComputeTimeFromCallback;
                SpringOverScroller.this.mCurrentComputeTimeFromCallback = j2;
                SpringOverScroller.this.mIsUpdateTimeFromCallback = true;
                if (SpringOverScroller.this.mCancelCallback) {
                    return;
                }
                Choreographer.getInstance().postFrameCallback(this);
            }
        };
        this.mScrollerX = new ReboundOverScroller();
        this.mScrollerY = new ReboundOverScroller();
        if (interpolator == null) {
            this.mInterpolator = new COUIViscousFluidInterpolator();
        } else {
            this.mInterpolator = interpolator;
        }
        setRefreshRateUnConvert(SOLVER_TIMESTEP_SEC);
        this.mContext = context;
        this.mFrameRateHelper = new COUlFrameRateScrollSceneHelper(false);
    }

    private int increaseVelocityIfNeed(int i2) {
        if (!this.mEnableFlingSpeedIncrease) {
            return i2;
        }
        long jCurrentTimeMillis = System.currentTimeMillis();
        int i6 = this.mContinuousFlingCount;
        if (i6 <= 0) {
            if (i6 != 0) {
                return i2;
            }
            this.mContinuousFlingCount = i6 + 1;
            this.mLastFlingTime = jCurrentTimeMillis;
            return i2;
        }
        if (jCurrentTimeMillis - this.mLastFlingTime > 500 || i2 < FLING_SPEED_INCREASE_VELOCITY_THRESHOLD) {
            resetFlingSpeedValue();
            return i2;
        }
        this.mLastFlingTime = jCurrentTimeMillis;
        int i10 = i6 + 1;
        this.mContinuousFlingCount = i10;
        if (i10 <= 4) {
            return i2;
        }
        float f2 = this.mLastFlingSpeedIncreaseRate * FLING_SPEED_INCREASE_RATE;
        this.mLastFlingSpeedIncreaseRate = f2;
        return Math.max(-70000, Math.min((int) (i2 * f2), FLING_SPEED_INCREASE_MAX_VELOCITY));
    }

    private void limitEdgeReachedVelocityIfNeed(ReboundOverScroller reboundOverScroller) {
        if (!this.mEnableFlingSpeedIncrease || this.mContinuousFlingCount <= 4) {
            return;
        }
        ReboundOverScroller.PhysicsState physicsState = reboundOverScroller.mCurrentState;
        double d2 = physicsState.mVelocity;
        if (d2 > 20000.0d) {
            physicsState.mVelocity = 1000.0d;
        } else if (d2 < -20000.0d) {
            physicsState.mVelocity = -1000.0d;
        }
    }

    private void resetFlingSpeedValue() {
        this.mLastFlingTime = 0L;
        this.mContinuousFlingCount = 0;
        this.mLastFlingSpeedIncreaseRate = 1.0f;
    }

    private void setRefreshRateUnConvert(float f2) {
        mRefreshTime = f2;
    }

    private static synchronized void setStaticSpringBackFriction(float f2) {
        mSpringBackFriction = f2;
    }

    @Override
    public void abortAnimation() {
        if (DEBUG) {
            Log.d(TAG, "abortAnimation", new Throwable());
        }
        this.mMode = 2;
        this.mScrollerX.setAtRest();
        this.mScrollerY.setAtRest();
        this.mCancelCallback = true;
        this.mFrameRateHelper.setFrameRate(false);
    }

    public void cancelCallback() {
        this.mCancelCallback = true;
    }

    @Override
    public boolean computeScrollOffset() {
        if (isCOUIFinished()) {
            this.mCancelCallback = this.mScrollerX.mCancelCallback && this.mScrollerY.mCancelCallback;
            return false;
        }
        int i2 = this.mMode;
        if (i2 == 0) {
            long jCurrentAnimationTimeMillis = AnimationUtils.currentAnimationTimeMillis() - this.mScrollerX.mScrollStartTime;
            int i6 = this.mScrollerX.mDuration;
            if (jCurrentAnimationTimeMillis < i6) {
                float interpolation = this.mInterpolator.getInterpolation(jCurrentAnimationTimeMillis / i6);
                this.mScrollerX.updateScroll(interpolation);
                this.mScrollerY.updateScroll(interpolation);
            } else {
                this.mScrollerX.updateScroll(1.0f);
                this.mScrollerY.updateScroll(1.0f);
                abortAnimation();
            }
        } else if (i2 == 1 && !this.mScrollerX.update() && !this.mScrollerY.update()) {
            abortAnimation();
        }
        return true;
    }

    public void enableFrameRate(boolean z6) {
        this.mFrameRateHelper.enableFrameRate(z6);
    }

    @Override
    public void fling(int i2, int i6, int i10, int i11, int i12, int i13, int i14, int i15, int i16, int i17) {
        fling(i2, i6, i10, i11, i12, i13, i14, i15);
    }

    @Override
    public final int getCOUICurrX() {
        return (int) Math.round(this.mScrollerX.getCurrentValue());
    }

    @Override
    public final int getCOUICurrY() {
        return (int) Math.round(this.mScrollerY.getCurrentValue());
    }

    @Override
    public final int getCOUIFinalX() {
        return (int) this.mScrollerX.getEndValue();
    }

    @Override
    public final int getCOUIFinalY() {
        return (int) this.mScrollerY.getEndValue();
    }

    @Override
    public float getCurrVelocity() {
        double velocity = this.mScrollerX.getVelocity();
        double velocity2 = this.mScrollerY.getVelocity();
        return (int) Math.sqrt((velocity * velocity) + (velocity2 * velocity2));
    }

    @Override
    public float getCurrVelocityX() {
        return (float) this.mScrollerX.getVelocity();
    }

    @Override
    public float getCurrVelocityY() {
        return (float) this.mScrollerY.getVelocity();
    }

    @Override
    public final boolean isCOUIFinished() {
        boolean zIsAtRest = this.mScrollerX.isAtRest();
        boolean zIsAtRest2 = this.mScrollerY.isAtRest();
        if (DEBUG) {
            Log.d(TAG, "scrollX is rest: " + this.mScrollerX.isAtRest() + "  scrollY is rest: " + this.mScrollerY.isAtRest() + "  mMode = " + this.mMode);
        }
        return zIsAtRest && zIsAtRest2 && this.mMode != 0;
    }

    public boolean isEnableFlingSpeedIncrease() {
        return this.mEnableFlingSpeedIncrease;
    }

    @Override
    public boolean isScrollingInDirection(float f2, float f10) {
        return !isFinished() && Math.signum(f2) == Math.signum((float) ((int) (this.mScrollerX.mEndValue - this.mScrollerX.mStartValue))) && Math.signum(f10) == Math.signum((float) ((int) (this.mScrollerY.mEndValue - this.mScrollerY.mStartValue)));
    }

    @Override
    public void notifyHorizontalEdgeReached(int i2, int i6, int i10) {
        this.mScrollerX.notifyEdgeReached(i2, i6, i10);
        springBack(i2, 0, 0, i6, 0, 0);
    }

    @Override
    public void notifyVerticalEdgeReached(int i2, int i6, int i10) {
        this.mScrollerY.notifyEdgeReached(i2, i6, i10);
        springBack(0, i2, 0, 0, 0, i6);
    }

    public void postChoreographerCallback() {
        if (DEBUG) {
            Log.d(TAG, "postChoreographerCallback: post Callback");
        }
        Choreographer.getInstance().postFrameCallback(this.mCallback);
    }

    public void removeChoreographerCallback() {
        if (DEBUG) {
            Log.d(TAG, "removeChoreographerCallback: remove Callback");
        }
        Choreographer.getInstance().removeFrameCallback(this.mCallback);
    }

    @Override
    public void setCOUIFriction(float f2) {
    }

    @Override
    public void setCurrVelocityX(float f2) {
        this.mScrollerX.mCurrentState.mVelocity = f2;
    }

    @Override
    public void setCurrVelocityY(float f2) {
        this.mScrollerY.mCurrentState.mVelocity = f2;
    }

    public void setDebug(boolean z6) {
        DEBUG = z6;
    }

    @Override
    public void setDurationRatio(float f2) {
    }

    public void setEnableFlingSpeedIncrease(boolean z6) {
        if (this.mEnableFlingSpeedIncrease == z6) {
            return;
        }
        this.mEnableFlingSpeedIncrease = z6;
        resetFlingSpeedValue();
    }

    @Override
    public void setFinalX(int i2) {
    }

    @Override
    public void setFinalY(int i2) {
    }

    @Override
    public void setFlingFriction(float f2) {
        this.mScrollerX.mFlingFriction = f2;
        this.mScrollerY.mFlingFriction = f2;
    }

    @Override
    public void setInterpolator(Interpolator interpolator) {
        if (interpolator == null) {
            this.mInterpolator = new COUIViscousFluidInterpolator();
        } else {
            this.mInterpolator = interpolator;
        }
    }

    @Override
    public void setIsScrollView(boolean z6) {
        this.mScrollerX.mIsScrollView = z6;
        this.mScrollerY.mIsScrollView = z6;
    }

    public void setRefreshRate(float f2) {
        mRefreshTime = Math.round(10000.0f / f2) / 10000.0f;
    }

    public void setSpringBackFriction(float f2) {
        setStaticSpringBackFriction(f2);
    }

    public void setSpringBackTensionMultiple(float f2) {
        this.mScrollerX.mSpringBackTensionMultiple = f2;
        this.mScrollerY.mSpringBackTensionMultiple = f2;
    }

    @Override
    public void setVelocityXRatio(float f2) {
    }

    @Override
    public void setVelocityYRatio(float f2) {
    }

    @Override
    public boolean springBack(int i2, int i6, int i10, int i11, int i12, int i13) {
        if (DEBUG) {
            Log.d(TAG, "springBack startX = " + i2 + " startY = " + i6 + " minX = " + i10 + " minY = " + i12 + " maxY = " + i13, new Throwable());
        }
        boolean zSpringBack = this.mScrollerX.springBack(i2, i10, i11, false);
        boolean zSpringBack2 = this.mScrollerY.springBack(i6, i12, i13, false);
        if (zSpringBack || zSpringBack2) {
            this.mMode = 1;
        }
        return zSpringBack || zSpringBack2;
    }

    @Override
    public void startScroll(int i2, int i6, int i10, int i11) {
        startScroll(i2, i6, i10, i11, SCROLL_DEFAULT_DURATION);
    }

    public void triggerCallback() {
        removeChoreographerCallback();
        postChoreographerCallback();
        this.mCancelCallback = false;
        this.mScrollerX.mCancelCallback = false;
        this.mScrollerY.mCancelCallback = false;
    }

    @Override
    public void fling(int i2, int i6, int i10, int i11, int i12, int i13, int i14, int i15) {
        fling(i2, i6, i10, i11);
    }

    @Override
    public void startScroll(int i2, int i6, int i10, int i11, int i12) {
        if (DEBUG) {
            Log.d(TAG, "startScroll startX = " + i2 + " startY = " + i6 + " dx = " + i10 + " dy = " + i11 + " duration = " + i12, new Throwable());
        }
        this.mMode = 0;
        long jCurrentAnimationTimeMillis = AnimationUtils.currentAnimationTimeMillis();
        this.mScrollerX.startScroll(i2, i10, i12, jCurrentAnimationTimeMillis);
        this.mScrollerY.startScroll(i6, i11, i12, jCurrentAnimationTimeMillis);
        this.mFrameRateHelper.setFrameRate(true);
    }

    @Override
    public void fling(int i2, int i6, int i10, int i11) {
        if (DEBUG) {
            Log.d(TAG, "fling startX = " + i2 + " startY = " + i6 + " velocityX = " + i10 + " velocityY = " + i11, new Throwable());
        }
        this.mMode = 1;
        this.mScrollerX.fling(i2, Integer.MIN_VALUE, Integer.MAX_VALUE, increaseVelocityIfNeed(i10), 0);
        this.mScrollerY.fling(i6, Integer.MIN_VALUE, Integer.MAX_VALUE, increaseVelocityIfNeed(i11), 0);
        this.mFrameRateHelper.setFrameRate(true);
    }

    public SpringOverScroller(Context context) {
        this(context, null);
    }
}
