package com.coui.appcompat.panel;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.OverScroller;
import androidx.core.view.ViewCompat;
import java.util.Arrays;


public class COUIViewDragHelper {
    private static final int BASE_SETTLE_DURATION = 256;
    public static final int DIRECTION_ALL = 3;
    public static final int DIRECTION_HORIZONTAL = 1;
    public static final int DIRECTION_VERTICAL = 2;
    public static final int EDGE_ALL = 15;
    public static final int EDGE_BOTTOM = 8;
    public static final int EDGE_LEFT = 1;
    public static final int EDGE_RIGHT = 2;
    private static final int EDGE_SIZE = 20;
    public static final int EDGE_TOP = 4;
    public static final int INVALID_POINTER = -1;
    private static final int MAX_SETTLE_DURATION = 600;
    public static final int STATE_DRAGGING = 1;
    public static final int STATE_IDLE = 0;
    public static final int STATE_SETTLING = 2;
    private static final String TAG = "COUIViewDragHelper";
    private static final Interpolator sInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float f2) {
            float f10 = f2 - 1.0f;
            return (f10 * f10 * f10 * f10 * f10) + 1.0f;
        }
    };
    private final Callback mCallback;
    private View mCapturedView;
    private final int mDefaultEdgeSize;
    private int mDragState;
    private int[] mEdgeDragsInProgress;
    private int[] mEdgeDragsLocked;
    private int mEdgeSize;
    private int[] mInitialEdgesTouched;
    private float[] mInitialMotionX;
    private float[] mInitialMotionY;
    private float[] mLastMotionX;
    private float[] mLastMotionY;
    private float mMaxVelocity;
    private float mMinVelocity;
    private final ViewGroup mParentView;
    private int mPointersDown;
    private boolean mReleaseInProgress;
    private OverScroller mScroller;
    private int mTouchSlop;
    private int mTrackingEdges;
    private VelocityTracker mVelocityTracker;
    private int mActivePointerId = -1;
    private final Runnable mSetIdleRunnable = new Runnable() {
        @Override
        public void run() {
            COUIViewDragHelper.this.setDragState(0);
        }
    };

    public static abstract class Callback {
        public int clampViewPositionHorizontal(View view, int i2, int i6) {
            return 0;
        }

        public int clampViewPositionVertical(View view, int i2, int i6) {
            return 0;
        }

        public int getOrderedChildIndex(int i2) {
            return i2;
        }

        public int getViewHorizontalDragRange(View view) {
            return 0;
        }

        public int getViewVerticalDragRange(View view) {
            return 0;
        }

        public void onEdgeDragStarted(int i2, int i6) {
        }

        public boolean onEdgeLock(int i2) {
            return false;
        }

        public void onEdgeTouched(int i2, int i6) {
        }

        public void onViewCaptured(View view, int i2) {
        }

        public void onViewDragStateChanged(int i2) {
        }

        public void onViewPositionChanged(View view, int i2, int i6, int i10, int i11) {
        }

        public void onViewReleased(View view, float f2, float f10) {
        }

        public abstract boolean tryCaptureView(View view, int i2);
    }

    private COUIViewDragHelper(Context context, ViewGroup viewGroup, Callback callback) {
        if (viewGroup == null) {
            throw new IllegalArgumentException("Parent view may not be null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("Callback may not be null");
        }
        this.mParentView = viewGroup;
        this.mCallback = callback;
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        int i2 = (int) ((context.getResources().getDisplayMetrics().density * 20.0f) + 0.5f);
        this.mDefaultEdgeSize = i2;
        this.mEdgeSize = i2;
        this.mTouchSlop = viewConfiguration.getScaledTouchSlop();
        this.mMaxVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
        this.mMinVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
    }

    private boolean checkNewEdgeDrag(float f2, float f10, int i2, int i6) {
        float fAbs = Math.abs(f2);
        float fAbs2 = Math.abs(f10);
        if ((this.mInitialEdgesTouched[i2] & i6) != i6 || (this.mTrackingEdges & i6) == 0 || (this.mEdgeDragsLocked[i2] & i6) == i6 || (this.mEdgeDragsInProgress[i2] & i6) == i6) {
            return false;
        }
        int i10 = this.mTouchSlop;
        if (fAbs <= i10 && fAbs2 <= i10) {
            return false;
        }
        if (fAbs >= fAbs2 * 0.5f || !this.mCallback.onEdgeLock(i6)) {
            return (this.mEdgeDragsInProgress[i2] & i6) == 0 && fAbs > ((float) this.mTouchSlop);
        }
        int[] iArr = this.mEdgeDragsLocked;
        iArr[i2] = iArr[i2] | i6;
        return false;
    }

    private boolean checkTouchSlop(View view, float f2, float f10) {
        if (view == null) {
            return false;
        }
        boolean z6 = this.mCallback.getViewHorizontalDragRange(view) > 0;
        boolean z10 = this.mCallback.getViewVerticalDragRange(view) > 0;
        if (!z6 || !z10) {
            return z6 ? Math.abs(f2) > ((float) this.mTouchSlop) : z10 && Math.abs(f10) > ((float) this.mTouchSlop);
        }
        float f11 = (f2 * f2) + (f10 * f10);
        int i2 = this.mTouchSlop;
        return f11 > ((float) (i2 * i2));
    }

    private int clampMag(int i2, int i6, int i10) {
        int iAbs = Math.abs(i2);
        if (iAbs < i6) {
            return 0;
        }
        return iAbs > i10 ? i2 > 0 ? i10 : -i10 : i2;
    }

    private void clearMotionHistory() {
        float[] fArr = this.mInitialMotionX;
        if (fArr == null) {
            return;
        }
        Arrays.fill(fArr, 0.0f);
        Arrays.fill(this.mInitialMotionY, 0.0f);
        Arrays.fill(this.mLastMotionX, 0.0f);
        Arrays.fill(this.mLastMotionY, 0.0f);
        Arrays.fill(this.mInitialEdgesTouched, 0);
        Arrays.fill(this.mEdgeDragsInProgress, 0);
        Arrays.fill(this.mEdgeDragsLocked, 0);
        this.mPointersDown = 0;
    }

    private int computeAxisDuration(int i2, int i6, int i10) {
        int iAbs;
        if (i2 == 0) {
            return 0;
        }
        int width = this.mParentView.getWidth();
        float f2 = width / 2;
        float fDistanceInfluenceForSnapDuration = f2 + (distanceInfluenceForSnapDuration(Math.min(1.0f, Math.abs(i2) / width)) * f2);
        int iAbs2 = Math.abs(i6);
        if (iAbs2 > 0) {
            iAbs = Math.round(Math.abs(fDistanceInfluenceForSnapDuration / iAbs2) * 1000.0f) * 4;
        } else {
            iAbs = (int) (((Math.abs(i2) / (i10 == 0 ? 1 : Math.abs(i10))) + 1.0f) * 256.0f);
        }
        return Math.min(iAbs, 600);
    }

    private int computeSettleDuration(View view, int i2, int i6, int i10, int i11) {
        float f2;
        float f10;
        float f11;
        float f12;
        float f13;
        float f14;
        int iClampMag = clampMag(i10, (int) this.mMinVelocity, (int) this.mMaxVelocity);
        int iClampMag2 = clampMag(i11, (int) this.mMinVelocity, (int) this.mMaxVelocity);
        int iAbs = Math.abs(i2);
        int iAbs2 = Math.abs(i6);
        int iAbs3 = Math.abs(iClampMag);
        int iAbs4 = Math.abs(iClampMag2);
        int i12 = iAbs3 + iAbs4;
        int i13 = iAbs + iAbs2;
        if (i12 > 0) {
            if (iClampMag != 0) {
                f13 = iAbs3;
                f14 = i12;
            } else {
                f13 = iAbs;
                f14 = i13;
            }
            f2 = f13 / f14;
            if (iClampMag2 != 0) {
                f12 = iAbs4;
                f11 = i12;
            } else {
                f12 = iAbs2;
                f11 = i13;
            }
        } else {
            if (i13 <= 0) {
                f2 = 0.5f;
                f10 = 0.5f;
                return (int) ((computeAxisDuration(i2, iClampMag, this.mCallback.getViewHorizontalDragRange(view)) * f2) + (computeAxisDuration(i6, iClampMag2, this.mCallback.getViewVerticalDragRange(view)) * f10));
            }
            f11 = i13;
            f2 = iAbs / f11;
            f12 = iAbs2;
        }
        f10 = f12 / f11;
        return (int) ((computeAxisDuration(i2, iClampMag, this.mCallback.getViewHorizontalDragRange(view)) * f2) + (computeAxisDuration(i6, iClampMag2, this.mCallback.getViewVerticalDragRange(view)) * f10));
    }

    public static COUIViewDragHelper create(ViewGroup viewGroup, Callback callback) {
        return new COUIViewDragHelper(viewGroup.getContext(), viewGroup, callback);
    }

    private void dispatchViewReleased(float f2, float f10) {
        this.mReleaseInProgress = true;
        this.mCallback.onViewReleased(this.mCapturedView, f2, f10);
        this.mReleaseInProgress = false;
        if (this.mDragState == 1) {
            setDragState(0);
        }
    }

    private float distanceInfluenceForSnapDuration(float f2) {
        return (float) Math.sin((f2 - 0.5f) * 0.47123894f);
    }

    private void dragTo(int i2, int i6, int i10, int i11) {
        int left = this.mCapturedView.getLeft();
        int top = this.mCapturedView.getTop();
        if (i10 != 0) {
            i2 = this.mCallback.clampViewPositionHorizontal(this.mCapturedView, i2, i10);
            ViewCompat.offsetLeftAndRight(this.mCapturedView, i2 - left);
        }
        int i12 = i2;
        if (i11 != 0) {
            i6 = this.mCallback.clampViewPositionVertical(this.mCapturedView, i6, i11);
            ViewCompat.offsetTopAndBottom(this.mCapturedView, i6 - top);
        }
        int i13 = i6;
        if (i10 == 0 && i11 == 0) {
            return;
        }
        this.mCallback.onViewPositionChanged(this.mCapturedView, i12, i13, i12 - left, i13 - top);
    }

    private void ensureMotionHistorySizeForId(int i2) {
        float[] fArr = this.mInitialMotionX;
        if (fArr == null || fArr.length <= i2) {
            int i6 = i2 + 1;
            float[] fArr2 = new float[i6];
            float[] fArr3 = new float[i6];
            float[] fArr4 = new float[i6];
            float[] fArr5 = new float[i6];
            int[] iArr = new int[i6];
            int[] iArr2 = new int[i6];
            int[] iArr3 = new int[i6];
            if (fArr != null) {
                System.arraycopy(fArr, 0, fArr2, 0, fArr.length);
                float[] fArr6 = this.mInitialMotionY;
                System.arraycopy(fArr6, 0, fArr3, 0, fArr6.length);
                float[] fArr7 = this.mLastMotionX;
                System.arraycopy(fArr7, 0, fArr4, 0, fArr7.length);
                float[] fArr8 = this.mLastMotionY;
                System.arraycopy(fArr8, 0, fArr5, 0, fArr8.length);
                int[] iArr4 = this.mInitialEdgesTouched;
                System.arraycopy(iArr4, 0, iArr, 0, iArr4.length);
                int[] iArr5 = this.mEdgeDragsInProgress;
                System.arraycopy(iArr5, 0, iArr2, 0, iArr5.length);
                int[] iArr6 = this.mEdgeDragsLocked;
                System.arraycopy(iArr6, 0, iArr3, 0, iArr6.length);
            }
            this.mInitialMotionX = fArr2;
            this.mInitialMotionY = fArr3;
            this.mLastMotionX = fArr4;
            this.mLastMotionY = fArr5;
            this.mInitialEdgesTouched = iArr;
            this.mEdgeDragsInProgress = iArr2;
            this.mEdgeDragsLocked = iArr3;
        }
    }

    private boolean forceSettleCapturedViewAt(int i2, int i6, int i10, int i11) {
        int left = i2 - this.mCapturedView.getLeft();
        int top = i6 - this.mCapturedView.getTop();
        if (left == 0 && top == 0) {
            setDragState(0);
            return false;
        }
        computeSettleDuration(this.mCapturedView, left, top, i10, i11);
        setDragState(2);
        return true;
    }

    private int getEdgesTouched(int i2, int i6) {
        int i10 = i2 < this.mParentView.getLeft() + this.mEdgeSize ? 1 : 0;
        if (i6 < this.mParentView.getTop() + this.mEdgeSize) {
            i10 |= 4;
        }
        if (i2 > this.mParentView.getRight() - this.mEdgeSize) {
            i10 |= 2;
        }
        return i6 > this.mParentView.getBottom() - this.mEdgeSize ? i10 | 8 : i10;
    }

    private boolean isValidPointerForActionMove(int i2) {
        if (isPointerDown(i2)) {
            return true;
        }
        Log.e(TAG, "Ignoring pointerId=" + i2 + " because ACTION_DOWN was not received for this pointer before ACTION_MOVE. It likely happened because  COUIViewDragHelper did not receive all the events in the event stream.");
        return false;
    }

    private void releaseViewForPointerUp() {
        this.mVelocityTracker.computeCurrentVelocity(1000, this.mMaxVelocity);
        dispatchViewReleased(clampMag(this.mVelocityTracker.getXVelocity(this.mActivePointerId), this.mMinVelocity, this.mMaxVelocity), clampMag(this.mVelocityTracker.getYVelocity(this.mActivePointerId), this.mMinVelocity, this.mMaxVelocity));
    }













    private void reportNewEdgeDrags(float f2, float f10, int i2) {
        int edgeDrags = checkNewEdgeDrag(f2, f10, i2, 1) ? 1 : 0;
        if (checkNewEdgeDrag(f10, f2, i2, 4)) {
            edgeDrags |= 4;
        }
        if (checkNewEdgeDrag(f2, f10, i2, 2)) {
            edgeDrags |= 2;
        }
        if (checkNewEdgeDrag(f10, f2, i2, 8)) {
            edgeDrags |= 8;
        }
        if (edgeDrags != 0) {
            int[] iArr = this.mEdgeDragsInProgress;
            iArr[i2] = iArr[i2] | edgeDrags;
            this.mCallback.onEdgeDragStarted(edgeDrags, i2);
        }
    }

    private void saveInitialMotion(float f2, float f10, int i2) {
        ensureMotionHistorySizeForId(i2);
        float[] fArr = this.mInitialMotionX;
        this.mLastMotionX[i2] = f2;
        fArr[i2] = f2;
        float[] fArr2 = this.mInitialMotionY;
        this.mLastMotionY[i2] = f10;
        fArr2[i2] = f10;
        this.mInitialEdgesTouched[i2] = getEdgesTouched((int) f2, (int) f10);
        this.mPointersDown |= 1 << i2;
    }

    private void saveLastMotion(MotionEvent motionEvent) {
        int pointerCount = motionEvent.getPointerCount();
        for (int i2 = 0; i2 < pointerCount; i2++) {
            int pointerId = motionEvent.getPointerId(i2);
            if (isValidPointerForActionMove(pointerId)) {
                float x6 = motionEvent.getX(i2);
                float y6 = motionEvent.getY(i2);
                this.mLastMotionX[pointerId] = x6;
                this.mLastMotionY[pointerId] = y6;
            }
        }
    }

    public void abort() {
        cancel();
        if (this.mDragState == 2) {
            int currX = this.mScroller.getCurrX();
            int currY = this.mScroller.getCurrY();
            this.mScroller.abortAnimation();
            int currX2 = this.mScroller.getCurrX();
            int currY2 = this.mScroller.getCurrY();
            this.mCallback.onViewPositionChanged(this.mCapturedView, currX2, currY2, currX2 - currX, currY2 - currY);
        }
        setDragState(0);
    }

    public boolean canScroll(View view, boolean z6, int i2, int i6, int i10, int i11) {
        int i12;
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            int scrollX = view.getScrollX();
            int scrollY = view.getScrollY();
            for (int childCount = viewGroup.getChildCount() - 1; childCount >= 0; childCount--) {
                View childAt = viewGroup.getChildAt(childCount);
                int i13 = i10 + scrollX;
                if (i13 >= childAt.getLeft() && i13 < childAt.getRight() && (i12 = i11 + scrollY) >= childAt.getTop() && i12 < childAt.getBottom() && canScroll(childAt, true, i2, i6, i13 - childAt.getLeft(), i12 - childAt.getTop())) {
                    return true;
                }
            }
        }
        return z6 && (view.canScrollHorizontally(-i2) || view.canScrollVertically(-i6));
    }

    public void cancel() {
        this.mActivePointerId = -1;
        clearMotionHistory();
        VelocityTracker velocityTracker = this.mVelocityTracker;
        if (velocityTracker != null) {
            velocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    public void captureChildView(View view, int i2) {
        if (view.getParent() == this.mParentView) {
            this.mCapturedView = view;
            this.mActivePointerId = i2;
            this.mCallback.onViewCaptured(view, i2);
            setDragState(1);
            return;
        }
        throw new IllegalArgumentException("captureChildView: parameter must be a descendant of the COUIViewDragHelper's tracked parent view (" + this.mParentView + ")");
    }

    public boolean continueSettling(boolean z6) {
        if (this.mDragState == 2) {
            boolean zComputeScrollOffset = this.mScroller.computeScrollOffset();
            int currX = this.mScroller.getCurrX();
            int currY = this.mScroller.getCurrY();
            int left = currX - this.mCapturedView.getLeft();
            int top = currY - this.mCapturedView.getTop();
            if (left != 0) {
                ViewCompat.offsetLeftAndRight(this.mCapturedView, left);
            }
            if (top != 0) {
                ViewCompat.offsetTopAndBottom(this.mCapturedView, top);
            }
            if (left != 0 || top != 0) {
                this.mCallback.onViewPositionChanged(this.mCapturedView, currX, currY, left, top);
            }
            if (zComputeScrollOffset && currX == this.mScroller.getFinalX() && currY == this.mScroller.getFinalY()) {
                this.mScroller.abortAnimation();
            } else if (!zComputeScrollOffset) {
            }
            if (z6) {
                this.mParentView.post(this.mSetIdleRunnable);
            } else {
                setDragState(0);
            }
        }
        return this.mDragState == 2;
    }

    public View findTopChildUnder(int i2, int i6) {
        for (int childCount = this.mParentView.getChildCount() - 1; childCount >= 0; childCount--) {
            View childAt = this.mParentView.getChildAt(this.mCallback.getOrderedChildIndex(childCount));
            if (i2 >= childAt.getLeft() && i2 < childAt.getRight() && i6 >= childAt.getTop() && i6 < childAt.getBottom()) {
                return childAt;
            }
        }
        return null;
    }

    public void flingCapturedView(int i2, int i6, int i10, int i11) {
        if (!this.mReleaseInProgress) {
            throw new IllegalStateException("Cannot flingCapturedView outside of a call to Callback#onViewReleased");
        }
        this.mScroller.fling(this.mCapturedView.getLeft(), this.mCapturedView.getTop(), (int) this.mVelocityTracker.getXVelocity(this.mActivePointerId), (int) this.mVelocityTracker.getYVelocity(this.mActivePointerId), i2, i10, i6, i11);
        setDragState(2);
    }

    public int getActivePointerId() {
        return this.mActivePointerId;
    }

    public View getCapturedView() {
        return this.mCapturedView;
    }

    public int getDefaultEdgeSize() {
        return this.mDefaultEdgeSize;
    }

    public int getEdgeSize() {
        return this.mEdgeSize;
    }

    public float getMinVelocity() {
        return this.mMinVelocity;
    }

    public int getTouchSlop() {
        return this.mTouchSlop;
    }

    public int getViewDragState() {
        return this.mDragState;
    }

    public boolean isCapturedViewUnder(int i2, int i6) {
        return isViewUnder(this.mCapturedView, i2, i6);
    }

    public boolean isEdgeTouched(int i2) {
        int length = this.mInitialEdgesTouched.length;
        for (int i6 = 0; i6 < length; i6++) {
            if (isEdgeTouched(i2, i6)) {
                return true;
            }
        }
        return false;
    }

    public boolean isPointerDown(int i2) {
        return (this.mPointersDown & (1 << i2)) != 0;
    }

    public boolean isViewUnder(View view, int i2, int i6) {
        return view != null && i2 >= view.getLeft() && i2 < view.getRight() && i6 >= view.getTop() && i6 < view.getBottom();
    }

    public void processTouchEvent(MotionEvent motionEvent) {
        int actionMasked = motionEvent.getActionMasked();
        int actionIndex = motionEvent.getActionIndex();
        if (actionMasked == 0) {
            cancel();
        }
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
        this.mVelocityTracker.addMovement(motionEvent);
        int i2 = 0;
        if (actionMasked == 0) {
            float x6 = motionEvent.getX();
            float y6 = motionEvent.getY();
            int pointerId = motionEvent.getPointerId(0);
            View viewFindTopChildUnder = findTopChildUnder((int) x6, (int) y6);
            saveInitialMotion(x6, y6, pointerId);
            tryCaptureViewForDrag(viewFindTopChildUnder, pointerId);
            int i6 = this.mInitialEdgesTouched[pointerId];
            int i10 = this.mTrackingEdges;
            if ((i6 & i10) != 0) {
                this.mCallback.onEdgeTouched(i6 & i10, pointerId);
                return;
            }
            return;
        }
        if (actionMasked == 1) {
            if (this.mDragState == 1) {
                releaseViewForPointerUp();
            }
            cancel();
            return;
        }
        if (actionMasked == 2) {
            if (this.mDragState == 1) {
                if (isValidPointerForActionMove(this.mActivePointerId)) {
                    int iFindPointerIndex = motionEvent.findPointerIndex(this.mActivePointerId);
                    float x10 = motionEvent.getX(iFindPointerIndex);
                    float y10 = motionEvent.getY(iFindPointerIndex);
                    float[] fArr = this.mLastMotionX;
                    int i11 = this.mActivePointerId;
                    int i12 = (int) (x10 - fArr[i11]);
                    int i13 = (int) (y10 - this.mLastMotionY[i11]);
                    dragTo(this.mCapturedView.getLeft() + i12, this.mCapturedView.getTop() + i13, i12, i13);
                    saveLastMotion(motionEvent);
                    return;
                }
                return;
            }
            int pointerCount = motionEvent.getPointerCount();
            while (i2 < pointerCount) {
                int pointerId2 = motionEvent.getPointerId(i2);
                if (isValidPointerForActionMove(pointerId2)) {
                    float x11 = motionEvent.getX(i2);
                    float y11 = motionEvent.getY(i2);
                    float f2 = x11 - this.mInitialMotionX[pointerId2];
                    float f10 = y11 - this.mInitialMotionY[pointerId2];
                    reportNewEdgeDrags(f2, f10, pointerId2);
                    if (this.mDragState != 1) {
                        View viewFindTopChildUnder2 = findTopChildUnder((int) x11, (int) y11);
                        if (checkTouchSlop(viewFindTopChildUnder2, f2, f10) && tryCaptureViewForDrag(viewFindTopChildUnder2, pointerId2)) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
                i2++;
            }
            saveLastMotion(motionEvent);
            return;
        }
        if (actionMasked == 3) {
            if (this.mDragState == 1) {
                dispatchViewReleased(0.0f, 0.0f);
            }
            cancel();
            return;
        }
        if (actionMasked == 5) {
            int pointerId3 = motionEvent.getPointerId(actionIndex);
            float x12 = motionEvent.getX(actionIndex);
            float y12 = motionEvent.getY(actionIndex);
            saveInitialMotion(x12, y12, pointerId3);
            if (this.mDragState != 0) {
                if (isCapturedViewUnder((int) x12, (int) y12)) {
                    tryCaptureViewForDrag(this.mCapturedView, pointerId3);
                    return;
                }
                return;
            } else {
                tryCaptureViewForDrag(findTopChildUnder((int) x12, (int) y12), pointerId3);
                int i14 = this.mInitialEdgesTouched[pointerId3];
                int i15 = this.mTrackingEdges;
                if ((i14 & i15) != 0) {
                    this.mCallback.onEdgeTouched(i14 & i15, pointerId3);
                    return;
                }
                return;
            }
        }
        if (actionMasked != 6) {
            return;
        }
        int pointerId4 = motionEvent.getPointerId(actionIndex);
        if (this.mDragState == 1 && pointerId4 == this.mActivePointerId) {
            int pointerCount2 = motionEvent.getPointerCount();
            while (true) {
                if (i2 >= pointerCount2) {
                    break;
                }
                int pointerId5 = motionEvent.getPointerId(i2);
                if (pointerId5 != this.mActivePointerId) {
                    View viewFindTopChildUnder3 = findTopChildUnder((int) motionEvent.getX(i2), (int) motionEvent.getY(i2));
                    View view = this.mCapturedView;
                    if (viewFindTopChildUnder3 == view && tryCaptureViewForDrag(view, pointerId5)) {
                        if (this.mActivePointerId == -1) {
                            break;
                        }
                    }
                }
                i2++;
            }
            releaseViewForPointerUp();
        }
        clearMotionHistory(pointerId4);
    }

    public void setCapturedView(View view) {
        this.mCapturedView = view;
    }

    public void setDragState(int i2) {
        this.mParentView.removeCallbacks(this.mSetIdleRunnable);
        if (this.mDragState != i2) {
            this.mDragState = i2;
            this.mCallback.onViewDragStateChanged(i2);
            if (this.mDragState == 0) {
                this.mCapturedView = null;
            }
        }
    }

    public void setEdgeSize(int i2) {
        this.mEdgeSize = i2;
    }

    public void setEdgeTrackingEnabled(int i2) {
        this.mTrackingEdges = i2;
    }

    public void setMinVelocity(float f2) {
        this.mMinVelocity = f2;
    }

    public boolean settleCapturedViewAt(int i2, int i6) {
        if (this.mReleaseInProgress) {
            return forceSettleCapturedViewAt(i2, i6, (int) this.mVelocityTracker.getXVelocity(this.mActivePointerId), (int) this.mVelocityTracker.getYVelocity(this.mActivePointerId));
        }
        throw new IllegalStateException("Cannot settleCapturedViewAt outside of a call to Callback#onViewReleased");
    }

    public boolean shouldInterceptTouchEvent(MotionEvent motionEvent) {
        int actionMasked = motionEvent.getActionMasked();
        int actionIndex = motionEvent.getActionIndex();
        if (actionMasked == MotionEvent.ACTION_DOWN) {
            cancel();
        }
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
        this.mVelocityTracker.addMovement(motionEvent);
        if (actionMasked == MotionEvent.ACTION_DOWN) {
            float x = motionEvent.getX();
            float y = motionEvent.getY();
            int pointerId = motionEvent.getPointerId(0);
            saveInitialMotion(x, y, pointerId);
            View childUnder = findTopChildUnder((int) x, (int) y);
            if (childUnder == this.mCapturedView && this.mDragState == STATE_SETTLING) {
                tryCaptureViewForDrag(childUnder, pointerId);
            }
            int edgesTouched = this.mInitialEdgesTouched[pointerId];
            int trackingEdges = this.mTrackingEdges;
            if ((edgesTouched & trackingEdges) != 0) {
                this.mCallback.onEdgeTouched(edgesTouched & trackingEdges, pointerId);
            }
        } else if (actionMasked == MotionEvent.ACTION_UP || actionMasked == MotionEvent.ACTION_CANCEL) {
            cancel();
        } else if (actionMasked == MotionEvent.ACTION_MOVE) {
            if (this.mInitialMotionX != null && this.mInitialMotionY != null) {
                int pointerCount = motionEvent.getPointerCount();
                for (int pointerIndex = 0; pointerIndex < pointerCount; pointerIndex++) {
                    int pointerId = motionEvent.getPointerId(pointerIndex);
                    if (!isValidPointerForActionMove(pointerId)) {
                        continue;
                    }
                    float x = motionEvent.getX(pointerIndex);
                    float y = motionEvent.getY(pointerIndex);
                    float dx = x - this.mInitialMotionX[pointerId];
                    float dy = y - this.mInitialMotionY[pointerId];
                    View childUnder = findTopChildUnder((int) x, (int) y);
                    boolean pastSlop = childUnder != null && checkTouchSlop(childUnder, dx, dy);
                    if (pastSlop) {
                        int oldLeft = childUnder.getLeft();
                        int targetLeft = oldLeft + ((int) dx);
                        int clampedX = this.mCallback.clampViewPositionHorizontal(childUnder, targetLeft, (int) dx);
                        int oldTop = childUnder.getTop();
                        int targetTop = oldTop + ((int) dy);
                        int clampedY = this.mCallback.clampViewPositionVertical(childUnder, targetTop, (int) dy);
                        int horizontalDragRange = this.mCallback.getViewHorizontalDragRange(childUnder);
                        int verticalDragRange = this.mCallback.getViewVerticalDragRange(childUnder);
                        if ((horizontalDragRange == 0 || (horizontalDragRange > 0 && clampedX == oldLeft)) && (verticalDragRange == 0 || (verticalDragRange > 0 && clampedY == oldTop))) {
                            break;
                        }
                    }
                    reportNewEdgeDrags(dx, dy, pointerId);
                    if (this.mDragState == STATE_DRAGGING) {
                        break;
                    }
                    if (pastSlop && tryCaptureViewForDrag(childUnder, pointerId)) {
                        break;
                    }
                }
                saveLastMotion(motionEvent);
            }
        } else if (actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
            int pointerId = motionEvent.getPointerId(actionIndex);
            float x = motionEvent.getX(actionIndex);
            float y = motionEvent.getY(actionIndex);
            saveInitialMotion(x, y, pointerId);
            if (this.mDragState == STATE_IDLE) {
                int edgesTouched = this.mInitialEdgesTouched[pointerId];
                int trackingEdges = this.mTrackingEdges;
                if ((edgesTouched & trackingEdges) != 0) {
                    this.mCallback.onEdgeTouched(edgesTouched & trackingEdges, pointerId);
                }
            } else if (this.mDragState == STATE_SETTLING) {
                View childUnder = findTopChildUnder((int) x, (int) y);
                if (childUnder == this.mCapturedView) {
                    tryCaptureViewForDrag(childUnder, pointerId);
                }
            }
        } else if (actionMasked == MotionEvent.ACTION_POINTER_UP) {
            clearMotionHistory(motionEvent.getPointerId(actionIndex));
        }
        return this.mDragState == STATE_DRAGGING;
    }

    public boolean smoothSlideViewTo(View view, int i2, int i6) {
        this.mCapturedView = view;
        this.mActivePointerId = -1;
        boolean zForceSettleCapturedViewAt = forceSettleCapturedViewAt(i2, i6, 0, 0);
        if (!zForceSettleCapturedViewAt && this.mDragState == 0 && this.mCapturedView != null) {
            this.mCapturedView = null;
        }
        return zForceSettleCapturedViewAt;
    }

    public boolean tryCaptureViewForDrag(View view, int i2) {
        if (view == this.mCapturedView && this.mActivePointerId == i2) {
            return true;
        }
        if (view == null || !this.mCallback.tryCaptureView(view, i2)) {
            return false;
        }
        this.mActivePointerId = i2;
        captureChildView(view, i2);
        return true;
    }

    private float clampMag(float f2, float f10, float f11) {
        float fAbs = Math.abs(f2);
        if (fAbs < f10) {
            return 0.0f;
        }
        return fAbs > f11 ? f2 > 0.0f ? f11 : -f11 : f2;
    }

    public static COUIViewDragHelper create(ViewGroup viewGroup, float f2, Callback callback) {
        COUIViewDragHelper cOUIViewDragHelperCreate = create(viewGroup, callback);
        cOUIViewDragHelperCreate.mTouchSlop = (int) (cOUIViewDragHelperCreate.mTouchSlop * (1.0f / f2));
        return cOUIViewDragHelperCreate;
    }

    public boolean isEdgeTouched(int i2, int i6) {
        return isPointerDown(i6) && (this.mInitialEdgesTouched[i6] & i2) != 0;
    }

    public boolean checkTouchSlop(int i2) {
        int length = this.mInitialMotionX.length;
        for (int i6 = 0; i6 < length; i6++) {
            if (checkTouchSlop(i2, i6)) {
                return true;
            }
        }
        return false;
    }

    public boolean checkTouchSlop(int i2, int i6) {
        if (!isPointerDown(i6)) {
            return false;
        }
        boolean z6 = (i2 & 1) == 1;
        boolean z10 = (i2 & 2) == 2;
        float f2 = this.mLastMotionX[i6] - this.mInitialMotionX[i6];
        float f10 = this.mLastMotionY[i6] - this.mInitialMotionY[i6];
        if (!z6 || !z10) {
            return z6 ? Math.abs(f2) > ((float) this.mTouchSlop) : z10 && Math.abs(f10) > ((float) this.mTouchSlop);
        }
        float f11 = (f2 * f2) + (f10 * f10);
        int i10 = this.mTouchSlop;
        return f11 > ((float) (i10 * i10));
    }

    private void clearMotionHistory(int i2) {
        if (this.mInitialMotionX == null || !isPointerDown(i2)) {
            return;
        }
        this.mInitialMotionX[i2] = 0.0f;
        this.mInitialMotionY[i2] = 0.0f;
        this.mLastMotionX[i2] = 0.0f;
        this.mLastMotionY[i2] = 0.0f;
        this.mInitialEdgesTouched[i2] = 0;
        this.mEdgeDragsInProgress[i2] = 0;
        this.mEdgeDragsLocked[i2] = 0;
        this.mPointersDown = (~(1 << i2)) & this.mPointersDown;
    }
}
