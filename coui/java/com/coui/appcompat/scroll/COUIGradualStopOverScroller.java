package com.coui.appcompat.scroll;

import android.content.Context;
import android.util.Log;
import com.coui.appcompat.log.COUILog;
import com.coui.appcompat.scroll.SpringOverScroller;


public class COUIGradualStopOverScroller extends SpringOverScroller {
    public static final boolean DEBUG;
    public static final String TAG = "GradualStopOverScroller";
    private COUIGradualStopHelper mGradualStopHelper;
    private final int mMaxFlingDistance;

    public static class GradualStopReboundOverScroller extends SpringOverScroller.ReboundOverScroller {
        private static final double DELTA_POSITION_MID = 1.0d;
        private COUIGradualStopHelper mGradualStopHelper;
        private double mRestSpeedThreshold = 5.0d;
        private double mDisplacementFromRestThreshold = 0.05d;

        public GradualStopReboundOverScroller(COUIGradualStopHelper cOUIGradualStopHelper) {
            this.mGradualStopHelper = cOUIGradualStopHelper;
        }

        private void adjustSplineDistance(int i2) {
            int simulateSplineDistance = getSimulateSplineDistance();
            if (simulateSplineDistance == 0) {
                return;
            }
            float centerToEdgeOffsetInVelocityDirection = this.mGradualStopHelper.getCenterToEdgeOffsetInVelocityDirection(i2);
            boolean z6 = COUIGradualStopOverScroller.DEBUG;
            if (z6) {
                Log.d(COUIGradualStopOverScroller.TAG, this + "[ simulateSplineDistance = " + simulateSplineDistance + " edgeDistance = " + centerToEdgeOffsetInVelocityDirection + " ]");
            }
            if (centerToEdgeOffsetInVelocityDirection != 0.0f && Math.abs(simulateSplineDistance) <= Math.abs(centerToEdgeOffsetInVelocityDirection)) {
                adjustSimulateSplineDistance(0.0f);
                springBack();
                return;
            }
            float displacementToAlignCenter = this.mGradualStopHelper.getDisplacementToAlignCenter(i2, simulateSplineDistance);
            if (z6) {
                Log.d(COUIGradualStopOverScroller.TAG, this + "[ adaptDistance = " + displacementToAlignCenter + " ]");
            }
            if (displacementToAlignCenter != 0.0f) {
                adjustSimulateSplineDistance(displacementToAlignCenter);
            }
        }

        @Override
        public void fling(int i2, int i6, int i10, int i11, int i12) {
            super.fling(i2, i6, i10, i11, i12);
            adjustSplineDistance(i11);
        }

        @Override
        public double getDisplacementFromRestThreshold() {
            return this.mDisplacementFromRestThreshold;
        }

        @Override
        public double getRestSpeedThreshold() {
            return this.mRestSpeedThreshold;
        }

        @Override
        public double getSplineMinDelta(float f2) {
            return DELTA_POSITION_MID;
        }

        public boolean springBack() {
            int centerToItemCenterOffsetUnderCenter = (int) this.mGradualStopHelper.getCenterToItemCenterOffsetUnderCenter();
            if (COUIGradualStopOverScroller.DEBUG) {
                Log.d(COUIGradualStopOverScroller.TAG, this + " childCenterDiff " + centerToItemCenterOffsetUnderCenter + " ]");
            }
            return springBack(0, centerToItemCenterOffsetUnderCenter, centerToItemCenterOffsetUnderCenter, true);
        }
    }

    static {
        DEBUG = COUILog.LOG_DEBUG || COUILog.isLoggable(TAG, 3);
    }

    public COUIGradualStopOverScroller(Context context, COUIGradualStopHelper cOUIGradualStopHelper) {
        super(context, null);
        this.mMaxFlingDistance = 10000;
        this.mGradualStopHelper = cOUIGradualStopHelper;
        this.mScrollerX = new GradualStopReboundOverScroller(cOUIGradualStopHelper);
        this.mScrollerY = new GradualStopReboundOverScroller(cOUIGradualStopHelper);
    }

    public double getMaxFlingDistance() {
        return 10000.0d;
    }

    public boolean springBackToCenter(int i2) {
        int i6;
        int i10;
        int i11;
        int i12;
        int centerToItemCenterOffsetUnderCenter = (int) this.mGradualStopHelper.getCenterToItemCenterOffsetUnderCenter();
        if (centerToItemCenterOffsetUnderCenter == 0) {
            return false;
        }
        if (DEBUG) {
            Log.d(TAG, this + " childCenterDiff " + centerToItemCenterOffsetUnderCenter + " ]");
        }
        if (i2 == 0) {
            i11 = centerToItemCenterOffsetUnderCenter;
            i12 = i11;
            i6 = 0;
            i10 = 0;
        } else {
            i6 = centerToItemCenterOffsetUnderCenter;
            i10 = i6;
            i11 = 0;
            i12 = 0;
        }
        springBack(0, 0, i11, i12, i6, i10);
        return true;
    }
}
