package com.coui.appcompat.scrollview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import com.coui.appcompat.animation.COUIPhysicalAnimationUtil;
import com.coui.appcompat.hapticfeedback.COUIHapticFeedbackConstants;
import com.coui.appcompat.scroll.COUIIOverScroller;
import com.coui.appcompat.scroll.SpringOverScroller;
import com.coui.appcompat.version.COUIVersionUtil;
import com.coui.appcompat.view.ViewNative;
import com.coui.appcompat.R;
import java.util.ArrayList;


public class COUIHorizontalScrollView extends HorizontalScrollView {
    static final int ANIMATED_SCROLL_GAP = 250;
    private static final float DEFAULT_INTERACTING_NESTED_SCROLL_ANGLE = 20.0f;
    private static final int DEFAULT_INTERACTING_NESTED_SCROLL_VELOCITY_THRESHOLD = 2500;
    private static final double DEGREE_TO_ARC_CONSTANT = 0.017453292519943295d;
    private static final int FLING_SCROLL_THRESHOLD = 1500;
    private static final float HORIZONTAL_SPRING_BACK_TENSION_MULTIPLE = 3.2f;
    private static final int INVALID_POINTER = -1;
    private static final int OVER_SCROLL_TOUCH_DURATION_THRESHOLD = 100;
    private static final int OVER_SCROLL_TOUCH_OFFSET_THRESHOLD = 10;
    private static final int SLOW_SCROLL_THRESHOLD = 250;
    private static final String TAG = "COUIHorScrollView";
    private float mAbortVelocityX;
    private int mActivePointerId;
    private boolean mAvoidAccidentalTouch;
    private View mChildToScrollTo;
    private int mDispatchEventVelocityThreshold;
    private boolean mEnableDispatchEventWhileOverScrolling;
    private boolean mEnableDispatchEventWhileScrolling;
    private boolean mEnableOptimizedScroll;
    private boolean mEnableVibrator;
    private float mEventFilterAngle;
    private float mFastFlingVelocity;
    private boolean mFillViewport;
    private boolean mFlingStrictSpan;
    private float mFlingVelocityX;
    private float mHorizontalScrollFactor;
    private int mInitialTouchX;
    private int mInitialTouchY;
    private boolean mIsBeingDragged;
    private Boolean mIsColorDevice;
    private boolean mIsLayoutDirty;
    private boolean mIsTouchDownWhileOverScrolling;
    private boolean mIsTouchDownWhileSlowScrolling;
    private boolean mItemClickableWhileOverScrolling;
    private boolean mItemClickableWhileSlowScrolling;
    private int mLastMotionX;
    private long mLastScroll;
    private int mMaximumVelocity;
    private int mMinimumVelocity;
    private COUIIOverScroller mOverScroller;
    private int mOverflingDistance;
    private int mOverscrollDistance;
    private int mScreenWidth;
    private boolean mScrollStrictSpan;
    private boolean mSmoothScrollingEnabled;
    private SpringOverScroller mSpringOverScroller;
    private final Rect mTempRect;
    private int mTouchSlop;
    private long mTouchTime;
    private VelocityTracker mVelocityTracker;

    public static class COUISavedState extends View.BaseSavedState {
        public static final Parcelable.Creator<COUISavedState> CREATOR = new Parcelable.Creator<COUISavedState>() {

            @Override
            public COUISavedState createFromParcel(Parcel parcel) {
                return new COUISavedState(parcel, COUISavedState.class.getClassLoader());
            }


            @Override
            public COUISavedState[] newArray(int i2) {
                return new COUISavedState[i2];
            }
        };
        public int scrollOffsetFromStart;

        public COUISavedState(Parcelable parcelable) {
            super(parcelable);
        }

        public String toString() {
            return "HorizontalScrollView.SavedState{" + Integer.toHexString(System.identityHashCode(this)) + " scrollPosition=" + this.scrollOffsetFromStart + "}";
        }

        @Override
        public void writeToParcel(Parcel parcel, int i2) {
            super.writeToParcel(parcel, i2);
            parcel.writeInt(this.scrollOffsetFromStart);
        }

        public COUISavedState(Parcel parcel) {
            super(parcel);
            this.scrollOffsetFromStart = parcel.readInt();
        }

        public COUISavedState(Parcel parcel, ClassLoader classLoader) {
            super(parcel, classLoader);
            this.scrollOffsetFromStart = parcel.readInt();
        }
    }

    public COUIHorizontalScrollView(Context context) {
        this(context, null);
    }

    private boolean canScroll() {
        View childAt = getChildAt(0);
        if (childAt != null) {
            return getWidth() < (childAt.getWidth() + getPaddingLeft()) + getPaddingRight();
        }
        return false;
    }

    private boolean dispatchClickEvent(View view, MotionEvent motionEvent) {
        boolean zDispatchTouchEvent = true;
        int[] iArr = {0, 1};
        for (int i2 = 0; i2 < 2; i2++) {
            motionEvent.setAction(iArr[i2]);
            zDispatchTouchEvent &= view.dispatchTouchEvent(motionEvent);
        }
        return zDispatchTouchEvent;
    }

    private void doScrollX(int i2) {
        if (i2 != 0) {
            if (this.mSmoothScrollingEnabled) {
                smoothCOUIScrollBy(i2, 0);
            } else {
                scrollBy(i2, 0);
            }
        }
    }

    private View findFocusableViewInBounds(boolean leftFocus, int left, int right) {
        ArrayList<View> focusables = getFocusables(View.FOCUS_FORWARD);
        int focusableCount = focusables.size();
        View focusCandidate = null;
        boolean candidateIsFullyContained = false;
        for (int index = 0; index < focusableCount; index++) {
            View view = focusables.get(index);
            int viewLeft = view.getLeft();
            int viewRight = view.getRight();
            if (left < viewRight && viewLeft < right) {
                boolean viewIsFullyContained = left < viewLeft && viewRight < right;
                if (focusCandidate == null) {
                    focusCandidate = view;
                    candidateIsFullyContained = viewIsFullyContained;
                } else {
                    boolean viewIsCloser = (leftFocus && viewLeft < focusCandidate.getLeft())
                            || (!leftFocus && viewRight > focusCandidate.getRight());
                    if (candidateIsFullyContained) {
                        if (viewIsFullyContained && viewIsCloser) {
                            focusCandidate = view;
                        }
                    } else if (viewIsFullyContained) {
                        focusCandidate = view;
                        candidateIsFullyContained = true;
                    } else if (viewIsCloser) {
                        focusCandidate = view;
                    }
                }
            }
        }
        return focusCandidate;
    }

    private View findFocusableViewInMyBounds(boolean z6, int i2, View view) {
        int horizontalFadingEdgeLength = getHorizontalFadingEdgeLength() / 2;
        int i6 = i2 + horizontalFadingEdgeLength;
        int width = (i2 + getWidth()) - horizontalFadingEdgeLength;
        return (view == null || view.getLeft() >= width || view.getRight() <= i6) ? findFocusableViewInBounds(z6, i6, width) : view;
    }

    private View findViewToDispatchClickEvent(MotionEvent motionEvent) {
        View view = null;
        if (!isClickEvent(motionEvent)) {
            return null;
        }
        Rect rect = new Rect();
        for (int childCount = getChildCount() - 1; childCount >= 0; childCount--) {
            View childAt = getChildAt(childCount);
            if (childAt.getVisibility() == 0 || childAt.getAnimation() != null) {
                childAt.getHitRect(rect);
                boolean zContains = rect.contains(((int) motionEvent.getX()) + getScrollX(), ((int) motionEvent.getY()) + getScrollY());
                MotionEvent motionEventObtain = MotionEvent.obtain(motionEvent);
                motionEventObtain.offsetLocation(getScrollX() - childAt.getLeft(), getScrollY() - childAt.getTop());
                if (zContains && dispatchClickEvent(childAt, motionEventObtain)) {
                    view = childAt;
                }
                motionEventObtain.recycle();
            }
        }
        return view;
    }

    private int getScrollRange() {
        if (getChildCount() > 0) {
            return Math.max(0, getChildAt(0).getWidth() - ((getWidth() - getPaddingLeft()) - getPaddingRight()));
        }
        return 0;
    }

    private float getVelocityAlongScrollableDirection() {
        if (this.mOverScroller == null || (getNestedScrollAxes() & 2) != 0) {
            return 0.0f;
        }
        return this.mOverScroller.getCurrVelocityX();
    }

    private boolean hookIfNeedInterceptMoveEvent(float f2, float f10) {
        return !(this.mEnableDispatchEventWhileScrolling || (this.mEnableDispatchEventWhileOverScrolling && isOverScrolling())) || f10 == 0.0f || ((double) Math.abs(f2 / f10)) > Math.tan(((double) this.mEventFilterAngle) * DEGREE_TO_ARC_CONSTANT);
    }

    private boolean inChild(int i2, int i6) {
        if (getChildCount() <= 0) {
            return false;
        }
        int scrollX = getScrollX();
        View childAt = getChildAt(0);
        return i6 >= childAt.getTop() && i6 < childAt.getBottom() && i2 >= childAt.getLeft() - scrollX && i2 < childAt.getRight() - scrollX;
    }

    private void initCOUIHorizontalScrollView(Context context) {
        if (this.mOverScroller == null) {
            SpringOverScroller springOverScroller = new SpringOverScroller(context);
            this.mSpringOverScroller = springOverScroller;
            springOverScroller.setSpringBackTensionMultiple(HORIZONTAL_SPRING_BACK_TENSION_MULTIPLE);
            this.mSpringOverScroller.setIsScrollView(true);
            this.mOverScroller = this.mSpringOverScroller;
            setEnableFlingSpeedIncrease(true);
        }
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        this.mTouchSlop = viewConfiguration.getScaledTouchSlop();
        this.mMinimumVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
        this.mMaximumVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
        int i2 = displayMetrics.widthPixels;
        this.mOverscrollDistance = i2;
        this.mOverflingDistance = i2;
        this.mScreenWidth = i2;
        this.mHorizontalScrollFactor = viewConfiguration.getScaledHorizontalScrollFactor();
        setOverScrollMode(0);
    }

    private void initOrResetVelocityTracker() {
        VelocityTracker velocityTracker = this.mVelocityTracker;
        if (velocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        } else {
            velocityTracker.clear();
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private boolean isClickEvent(MotionEvent motionEvent) {
        int x6 = (int) (motionEvent.getX() - this.mInitialTouchX);
        return System.currentTimeMillis() - this.mTouchTime < 100 && ((int) Math.sqrt((double) (x6 * x6))) < 10;
    }

    private Boolean isColorDevice() {
        if (this.mIsColorDevice == null) {
            this.mIsColorDevice = Boolean.valueOf(COUIVersionUtil.isColorOS());
        }
        return this.mIsColorDevice;
    }

    private boolean isFastFling(float f2, float f10) {
        return !this.mAvoidAccidentalTouch || Math.abs(f2) > this.mFastFlingVelocity || Math.abs(f10) > this.mFastFlingVelocity;
    }

    private boolean isOffScreen(View view) {
        return !isWithinDeltaOfScreen(view, 0);
    }

    private boolean isOverScrolling() {
        return getScrollX() < 0 || getScrollX() > getScrollRange();
    }

    private static boolean isViewDescendantOf(View view, View view2) {
        if (view == view2) {
            return true;
        }
        Object parent = view.getParent();
        return (parent instanceof ViewGroup) && isViewDescendantOf((View) parent, view2);
    }

    private boolean isWithinDeltaOfScreen(View view, int i2) {
        view.getDrawingRect(this.mTempRect);
        offsetDescendantRectToMyCoords(view, this.mTempRect);
        return this.mTempRect.right + i2 >= getScrollX() && this.mTempRect.left - i2 <= getScrollX() + getWidth();
    }

    private void onSecondaryPointerUp(MotionEvent motionEvent) {
        int action = (motionEvent.getAction() & 65280) >> 8;
        if (motionEvent.getPointerId(action) == this.mActivePointerId) {
            int i2 = action == 0 ? 1 : 0;
            int x6 = (int) motionEvent.getX(i2);
            this.mLastMotionX = x6;
            this.mInitialTouchX = x6;
            this.mInitialTouchY = (int) motionEvent.getY(i2);
            this.mActivePointerId = motionEvent.getPointerId(i2);
            VelocityTracker velocityTracker = this.mVelocityTracker;
            if (velocityTracker != null) {
                velocityTracker.clear();
            }
        }
    }

    private void performFeedback() {
        if (this.mEnableVibrator) {
            performHapticFeedback(COUIHapticFeedbackConstants.EDGE_LIST_VIBRATE);
        }
    }

    private void recycleVelocityTracker() {
        VelocityTracker velocityTracker = this.mVelocityTracker;
        if (velocityTracker != null) {
            velocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    private boolean scrollAndFocus(int i2, int i6, int i10) {
        int width = getWidth();
        int scrollX = getScrollX();
        int i11 = width + scrollX;
        boolean z6 = false;
        boolean z10 = i2 == 17;
        View viewFindFocusableViewInBounds = findFocusableViewInBounds(z10, i6, i10);
        if (viewFindFocusableViewInBounds == null) {
            viewFindFocusableViewInBounds = this;
        }
        if (i6 < scrollX || i10 > i11) {
            doScrollX(z10 ? i6 - scrollX : i10 - i11);
            z6 = true;
        }
        if (viewFindFocusableViewInBounds != findFocus()) {
            viewFindFocusableViewInBounds.requestFocus(i2);
        }
        return z6;
    }

    private void scrollToChild(View view) {
        view.getDrawingRect(this.mTempRect);
        offsetDescendantRectToMyCoords(view, this.mTempRect);
        int iComputeScrollDeltaToGetChildRectOnScreen = computeScrollDeltaToGetChildRectOnScreen(this.mTempRect);
        if (iComputeScrollDeltaToGetChildRectOnScreen != 0) {
            scrollBy(iComputeScrollDeltaToGetChildRectOnScreen, 0);
        }
    }

    private boolean scrollToChildRect(Rect rect, boolean z6) {
        int iComputeScrollDeltaToGetChildRectOnScreen = computeScrollDeltaToGetChildRectOnScreen(rect);
        boolean z10 = iComputeScrollDeltaToGetChildRectOnScreen != 0;
        if (z10) {
            if (z6) {
                scrollBy(iComputeScrollDeltaToGetChildRectOnScreen, 0);
            } else {
                smoothCOUIScrollBy(iComputeScrollDeltaToGetChildRectOnScreen, 0);
            }
        }
        return z10;
    }

    @Override
    public boolean arrowScroll(int i2) {
        int right;
        View viewFindFocus = findFocus();
        if (viewFindFocus == this) {
            viewFindFocus = null;
        }
        View viewFindNextFocus = FocusFinder.getInstance().findNextFocus(this, viewFindFocus, i2);
        int maxScrollAmount = getMaxScrollAmount();
        if (viewFindNextFocus == null || !isWithinDeltaOfScreen(viewFindNextFocus, maxScrollAmount)) {
            if (i2 == 17 && getScrollX() < maxScrollAmount) {
                maxScrollAmount = getScrollX();
            } else if (i2 == 66 && getChildCount() > 0 && (right = getChildAt(0).getRight() - (getScrollX() + getWidth())) < maxScrollAmount) {
                maxScrollAmount = right;
            }
            if (maxScrollAmount == 0) {
                return false;
            }
            if (i2 != 66) {
                maxScrollAmount = -maxScrollAmount;
            }
            doScrollX(maxScrollAmount);
        } else {
            viewFindNextFocus.getDrawingRect(this.mTempRect);
            offsetDescendantRectToMyCoords(viewFindNextFocus, this.mTempRect);
            doScrollX(computeScrollDeltaToGetChildRectOnScreen(this.mTempRect));
            viewFindNextFocus.requestFocus(i2);
        }
        if (viewFindFocus == null || !viewFindFocus.isFocused() || !isOffScreen(viewFindFocus)) {
            return true;
        }
        int descendantFocusability = getDescendantFocusability();
        setDescendantFocusability(131072);
        requestFocus();
        setDescendantFocusability(descendantFocusability);
        return true;
    }

    @Override
    public void computeScroll() {
        COUIIOverScroller cOUIIOverScroller = this.mOverScroller;
        if (cOUIIOverScroller == null || !cOUIIOverScroller.computeScrollOffset()) {
            if (this.mFlingStrictSpan) {
                this.mFlingStrictSpan = false;
                return;
            }
            return;
        }
        int scrollX = getScrollX();
        int scrollY = getScrollY();
        int cOUICurrX = this.mOverScroller.getCOUICurrX();
        int cOUICurrY = this.mOverScroller.getCOUICurrY();
        if (scrollX != cOUICurrX || scrollY != cOUICurrY) {
            overScrollBy(cOUICurrX - scrollX, cOUICurrY - scrollY, scrollX, scrollY, getScrollRange(), 0, this.mOverflingDistance, 0, false);
            onScrollChanged(getScrollX(), getScrollY(), scrollX, scrollY);
        }
        if (awakenScrollBars()) {
            return;
        }
        postInvalidateOnAnimation();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        return super.dispatchKeyEvent(keyEvent) || executeKeyEvent(keyEvent);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        COUIIOverScroller cOUIIOverScroller;
        if (this.mEnableDispatchEventWhileScrolling || (this.mEnableDispatchEventWhileOverScrolling && isOverScrolling())) {
            float velocityAlongScrollableDirection = getVelocityAlongScrollableDirection();
            if (motionEvent.getActionMasked() == 0 && this.mDispatchEventVelocityThreshold >= Math.abs(velocityAlongScrollableDirection)) {
                COUIIOverScroller cOUIIOverScroller2 = this.mOverScroller;
                float f2 = 0.0f;
                if (cOUIIOverScroller2 != null && cOUIIOverScroller2.getCurrVelocityX() != 0.0f) {
                    f2 = this.mFlingVelocityX;
                }
                this.mAbortVelocityX = f2;
                COUIIOverScroller cOUIIOverScroller3 = this.mOverScroller;
                if (cOUIIOverScroller3 != null) {
                    cOUIIOverScroller3.abortAnimation();
                }
                stopNestedScroll();
            }
            if ((motionEvent.getActionMasked() == 1 || motionEvent.getActionMasked() == 3) && (cOUIIOverScroller = this.mOverScroller) != null && cOUIIOverScroller.springBack(getScrollX(), getScrollY(), 0, getScrollRange(), 0, 0)) {
                postInvalidateOnAnimation();
            }
        }
        return super.dispatchTouchEvent(motionEvent);
    }

    @Override
    public boolean executeKeyEvent(KeyEvent keyEvent) {
        this.mTempRect.setEmpty();
        if (!canScroll()) {
            if (!isFocused()) {
                return false;
            }
            View viewFindFocus = findFocus();
            if (viewFindFocus == this) {
                viewFindFocus = null;
            }
            View viewFindNextFocus = FocusFinder.getInstance().findNextFocus(this, viewFindFocus, 66);
            return (viewFindNextFocus == null || viewFindNextFocus == this || !viewFindNextFocus.requestFocus(66)) ? false : true;
        }
        if (keyEvent.getAction() != 0) {
            return false;
        }
        int keyCode = keyEvent.getKeyCode();
        if (keyCode == 21) {
            return !keyEvent.isAltPressed() ? arrowScroll(17) : fullScroll(17);
        }
        if (keyCode == 22) {
            return !keyEvent.isAltPressed() ? arrowScroll(66) : fullScroll(66);
        }
        if (keyCode != 62) {
            return false;
        }
        pageScroll(keyEvent.isShiftPressed() ? 17 : 66);
        return false;
    }

    @Override
    public void fling(int i2) {
        this.mFlingVelocityX = i2;
        if (getChildCount() > 0) {
            int width = (getWidth() - getPaddingRight()) - getPaddingLeft();
            int iMax = Math.max(0, (getChildAt(0).getRight() - getPaddingLeft()) - width);
            COUIIOverScroller cOUIIOverScroller = this.mOverScroller;
            if (cOUIIOverScroller != null) {
                cOUIIOverScroller.fling(getScrollX(), getScrollY(), i2, 0, 0, iMax, 0, 0, width / 2, 0);
            }
            if (!this.mFlingStrictSpan) {
                this.mFlingStrictSpan = true;
            }
            boolean z6 = i2 > 0;
            View viewFindFocus = findFocus();
            COUIIOverScroller cOUIIOverScroller2 = this.mOverScroller;
            View viewFindFocusableViewInMyBounds = findFocusableViewInMyBounds(z6, cOUIIOverScroller2 != null ? cOUIIOverScroller2.getCOUIFinalX() : 0, viewFindFocus);
            if (viewFindFocusableViewInMyBounds == null) {
                viewFindFocusableViewInMyBounds = this;
            }
            if (viewFindFocusableViewInMyBounds != viewFindFocus) {
                viewFindFocusableViewInMyBounds.requestFocus(z6 ? 66 : 17);
            }
            postInvalidateOnAnimation();
        }
    }

    @Override
    public boolean fullScroll(int i2) {
        boolean z6 = i2 == 66;
        int width = getWidth();
        Rect rect = this.mTempRect;
        rect.left = 0;
        rect.right = width;
        if (z6 && getChildCount() > 0) {
            this.mTempRect.right = getChildAt(0).getRight();
            Rect rect2 = this.mTempRect;
            rect2.left = rect2.right - width;
        }
        Rect rect3 = this.mTempRect;
        return scrollAndFocus(i2, rect3.left, rect3.right);
    }

    public int getScrollableRange() {
        return (getWidth() - getPaddingLeft()) - getPaddingRight();
    }

    public void invalidateParentIfNeeded() {
        if (isHardwareAccelerated() && (getParent() instanceof View)) {
            ((View) getParent()).invalidate();
        }
    }

    public boolean isEnableFlingSpeedIncrease() {
        SpringOverScroller springOverScroller = this.mSpringOverScroller;
        if (springOverScroller != null) {
            return springOverScroller.isEnableFlingSpeedIncrease();
        }
        return false;
    }

    @Override
    public boolean isFillViewport() {
        return this.mFillViewport;
    }

    @Override
    public boolean isSmoothScrollingEnabled() {
        return this.mSmoothScrollingEnabled;
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mScrollStrictSpan) {
            this.mScrollStrictSpan = false;
        }
        if (this.mFlingStrictSpan) {
            this.mFlingStrictSpan = false;
        }
        SpringOverScroller springOverScroller = this.mSpringOverScroller;
        if (springOverScroller != null) {
            springOverScroller.cancelCallback();
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() == 8 && !this.mIsBeingDragged) {
            int iRound = Math.round((motionEvent.isFromSource(2) ? (motionEvent.getMetaState() & 1) != 0 ? -motionEvent.getAxisValue(9) : motionEvent.getAxisValue(10) : motionEvent.isFromSource(4194304) ? motionEvent.getAxisValue(26) : 0.0f) * this.mHorizontalScrollFactor);
            if (iRound != 0) {
                int scrollRange = getScrollRange();
                int scrollX = getScrollX();
                int i2 = iRound + scrollX;
                if (i2 < 0) {
                    scrollRange = 0;
                } else if (i2 <= scrollRange) {
                    scrollRange = i2;
                }
                if (scrollRange != scrollX) {
                    super.scrollTo(scrollRange, getScrollY());
                    return true;
                }
            }
        }
        return super.onGenericMotionEvent(motionEvent);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_MOVE && this.mIsBeingDragged) {
            return true;
        }
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                COUIIOverScroller overScroller = this.mOverScroller;
                float currentVelocity = overScroller != null ? overScroller.getCurrVelocityX() : 0.0f;
                boolean isFastFling = Math.abs(this.mFlingVelocityX) > FLING_SCROLL_THRESHOLD;
                this.mIsTouchDownWhileSlowScrolling = Math.abs(currentVelocity) > 0.0f
                        && Math.abs(currentVelocity) < SLOW_SCROLL_THRESHOLD
                        && isFastFling;
                this.mIsTouchDownWhileOverScrolling = isOverScrolling();
                this.mTouchTime = System.currentTimeMillis();
                int x = (int) event.getX();
                int y = (int) event.getY();
                if (!inChild(x, (int) event.getY())) {
                    this.mIsBeingDragged = false;
                    recycleVelocityTracker();
                    break;
                }
                this.mLastMotionX = x;
                this.mInitialTouchX = x;
                this.mInitialTouchY = y;
                this.mActivePointerId = event.getPointerId(0);
                initOrResetVelocityTracker();
                this.mVelocityTracker.addMovement(event);
                this.mIsBeingDragged = this.mOverScroller != null && !this.mOverScroller.isCOUIFinished();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                this.mIsBeingDragged = false;
                this.mActivePointerId = INVALID_POINTER;
                COUIIOverScroller scroller = this.mOverScroller;
                if (scroller != null && scroller.springBack(getScrollX(), getScrollY(), 0, getScrollRange(), 0, 0)) {
                    postInvalidateOnAnimation();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                int activePointerId = this.mActivePointerId;
                if (activePointerId == INVALID_POINTER) {
                    break;
                }
                int pointerIndex = event.findPointerIndex(activePointerId);
                if (pointerIndex == INVALID_POINTER) {
                    Log.e(TAG, "Invalid pointerId=" + activePointerId + " in onInterceptTouchEvent");
                    break;
                }
                int moveX = (int) event.getX(pointerIndex);
                int deltaX = Math.abs(moveX - this.mInitialTouchX);
                int deltaY = Math.abs(((int) event.getY(pointerIndex)) - this.mInitialTouchY);
                if (deltaX > this.mTouchSlop
                        && (getNestedScrollAxes() & View.SCROLL_AXIS_HORIZONTAL) == 0
                        && hookIfNeedInterceptMoveEvent((float) deltaX, (float) deltaY)) {
                    this.mIsBeingDragged = true;
                    this.mLastMotionX = moveX;
                    initVelocityTrackerIfNotExists();
                    this.mVelocityTracker.addMovement(event);
                    ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                int pointerDownIndex = event.getActionIndex();
                int pointerDownX = (int) event.getX(pointerDownIndex);
                this.mLastMotionX = pointerDownX;
                this.mInitialTouchX = pointerDownX;
                this.mInitialTouchY = (int) event.getY(pointerDownIndex);
                this.mActivePointerId = event.getPointerId(pointerDownIndex);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(event);
                int newPointerIndex = event.findPointerIndex(this.mActivePointerId);
                if (newPointerIndex == INVALID_POINTER) {
                    Log.e(TAG, "Invalid pointerId=" + this.mActivePointerId + " in onInterceptTouchEvent ACTION_POINTER_UP");
                    break;
                }
                int newPointerX = (int) event.getX(newPointerIndex);
                this.mLastMotionX = newPointerX;
                this.mInitialTouchX = newPointerX;
                this.mInitialTouchY = (int) event.getY(newPointerIndex);
                break;
            default:
                break;
        }
        return this.mIsBeingDragged;
    }

    @Override
    public void onMeasure(int i2, int i6) {
        super.onMeasure(i2, i6);
        if (this.mFillViewport && View.MeasureSpec.getMode(i2) != 0 && getChildCount() > 0) {
            View childAt = getChildAt(0);
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) childAt.getLayoutParams();
            int paddingLeft = getPaddingLeft() + getPaddingRight() + layoutParams.leftMargin + layoutParams.rightMargin;
            int paddingTop = getPaddingTop() + getPaddingBottom() + layoutParams.topMargin + layoutParams.bottomMargin;
            int measuredWidth = getMeasuredWidth() - paddingLeft;
            if (childAt.getMeasuredWidth() < measuredWidth) {
                childAt.measure(View.MeasureSpec.makeMeasureSpec(measuredWidth, 1073741824), ViewGroup.getChildMeasureSpec(i6, paddingTop, layoutParams.height));
            }
        }
    }

    @Override
    public void onOverScrolled(int i2, int i6, boolean z6, boolean z10) {
        if (getScrollY() == i6 && getScrollX() == i2) {
            return;
        }
        if ((i2 < 0 || i2 > getScrollRange()) && this.mFlingStrictSpan) {
            int scrollRange = i2 >= getScrollRange() ? getScrollRange() : 0;
            i2 = COUIPhysicalAnimationUtil.calcOverFlingDecelerateDist(scrollRange, i2 - scrollRange, this.mScreenWidth);
        }
        if (getOverScrollMode() == 2 || (getOverScrollMode() == 1 && getChildAt(0).getWidth() <= getScrollableRange())) {
            i2 = Math.min(Math.max(i2, 0), getScrollRange());
        }
        if (getScrollX() >= 0 && i2 < 0 && this.mFlingStrictSpan) {
            performFeedback();
            SpringOverScroller springOverScroller = this.mSpringOverScroller;
            if (springOverScroller != null) {
                springOverScroller.notifyHorizontalEdgeReached(i2, 0, this.mOverflingDistance);
            }
        }
        if (getScrollX() <= getScrollRange() && i2 > getScrollRange() && this.mFlingStrictSpan) {
            performFeedback();
            SpringOverScroller springOverScroller2 = this.mSpringOverScroller;
            if (springOverScroller2 != null) {
                springOverScroller2.notifyHorizontalEdgeReached(i2, getScrollRange(), this.mOverflingDistance);
            }
        }
        if (isColorDevice().booleanValue()) {
            ViewNative.setScrollX(this, i2);
            ViewNative.setScrollY(this, i6);
        } else {
            super.scrollTo(i2, i6);
        }
        invalidateParentIfNeeded();
        awakenScrollBars();
    }

    @Override
    public boolean onRequestFocusInDescendants(int i2, Rect rect) {
        if (i2 == 2) {
            i2 = 66;
        } else if (i2 == 1) {
            i2 = 17;
        }
        View viewFindNextFocus = rect == null ? FocusFinder.getInstance().findNextFocus(this, null, i2) : FocusFinder.getInstance().findNextFocusFromRect(this, rect, i2);
        if (viewFindNextFocus == null || isOffScreen(viewFindNextFocus)) {
            return false;
        }
        return viewFindNextFocus.requestFocus(i2, rect);
    }

    @Override
    public void onSizeChanged(int i2, int i6, int i10, int i11) {
        super.onSizeChanged(i2, i6, i10, i11);
        int i12 = getContext().getResources().getDisplayMetrics().widthPixels;
        this.mOverscrollDistance = i12;
        this.mOverflingDistance = i12;
        this.mScreenWidth = i12;
        View viewFindFocus = findFocus();
        if (viewFindFocus == null || this == viewFindFocus || !isWithinDeltaOfScreen(viewFindFocus, getRight() - getLeft())) {
            return;
        }
        viewFindFocus.getDrawingRect(this.mTempRect);
        offsetDescendantRectToMyCoords(viewFindFocus, this.mTempRect);
        doScrollX(computeScrollDeltaToGetChildRectOnScreen(this.mTempRect));
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        ViewParent parent;
        initVelocityTrackerIfNotExists();
        this.mVelocityTracker.addMovement(motionEvent);
        int action = motionEvent.getAction() & 255;
        if (action != 0) {
            if (action == 1) {
                boolean zIsOverScrolling = isOverScrolling();
                boolean z6 = this.mItemClickableWhileSlowScrolling && this.mIsTouchDownWhileSlowScrolling;
                boolean z10 = this.mItemClickableWhileOverScrolling && this.mIsTouchDownWhileOverScrolling && zIsOverScrolling;
                if (z6 || z10) {
                    findViewToDispatchClickEvent(motionEvent);
                }
                if (this.mIsBeingDragged) {
                    initVelocityTrackerIfNotExists();
                    VelocityTracker velocityTracker = this.mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, this.mMaximumVelocity);
                    int xVelocity = (int) velocityTracker.getXVelocity(this.mActivePointerId);
                    if (Math.abs(xVelocity) <= this.mMinimumVelocity) {
                        COUIIOverScroller cOUIIOverScroller = this.mOverScroller;
                        if (cOUIIOverScroller != null && cOUIIOverScroller.springBack(getScrollX(), getScrollY(), 0, getScrollRange(), 0, 0)) {
                            postInvalidateOnAnimation();
                        }
                    } else if (getScrollX() < 0) {
                        if (xVelocity > -1500) {
                            COUIIOverScroller cOUIIOverScroller2 = this.mOverScroller;
                            if (cOUIIOverScroller2 != null) {
                                cOUIIOverScroller2.setCurrVelocityX(-xVelocity);
                            }
                            COUIIOverScroller cOUIIOverScroller3 = this.mOverScroller;
                            if (cOUIIOverScroller3 != null && cOUIIOverScroller3.springBack(getScrollX(), getScrollY(), 0, getScrollRange(), 0, 0)) {
                                postInvalidateOnAnimation();
                            }
                        } else {
                            fling(-xVelocity);
                        }
                    } else if (getScrollX() > getScrollRange()) {
                        if (xVelocity < FLING_SCROLL_THRESHOLD) {
                            COUIIOverScroller cOUIIOverScroller4 = this.mOverScroller;
                            if (cOUIIOverScroller4 != null) {
                                cOUIIOverScroller4.setCurrVelocityX(-xVelocity);
                            }
                            COUIIOverScroller cOUIIOverScroller5 = this.mOverScroller;
                            if (cOUIIOverScroller5 != null && cOUIIOverScroller5.springBack(getScrollX(), getScrollY(), 0, getScrollRange(), 0, 0)) {
                                postInvalidateOnAnimation();
                            }
                        } else {
                            fling(-xVelocity);
                        }
                    } else if (getScrollX() > 0 && getScrollX() < getScrollRange()) {
                        fling(-xVelocity);
                    }
                    if (getScrollX() < 0 || getScrollX() > getScrollRange()) {
                        performFeedback();
                    }
                    this.mActivePointerId = -1;
                    this.mIsBeingDragged = false;
                    recycleVelocityTracker();
                } else {
                    COUIIOverScroller cOUIIOverScroller6 = this.mOverScroller;
                    if (cOUIIOverScroller6 != null && cOUIIOverScroller6.springBack(getScrollX(), getScrollY(), 0, getScrollRange(), 0, 0)) {
                        postInvalidateOnAnimation();
                    }
                }
            } else if (action == 2) {
                COUIIOverScroller cOUIIOverScroller7 = this.mOverScroller;
                if ((cOUIIOverScroller7 instanceof SpringOverScroller) && this.mEnableOptimizedScroll) {
                    ((SpringOverScroller) cOUIIOverScroller7).triggerCallback();
                }
                int iFindPointerIndex = motionEvent.findPointerIndex(this.mActivePointerId);
                if (iFindPointerIndex == -1) {
                    Log.e(TAG, "Invalid pointerId=" + this.mActivePointerId + " in onTouchEvent");
                } else {
                    int x6 = (int) motionEvent.getX(iFindPointerIndex);
                    int iCalcRealOverScrollDist = this.mLastMotionX - x6;
                    if (!this.mIsBeingDragged && Math.abs(iCalcRealOverScrollDist) > this.mTouchSlop) {
                        ViewParent parent2 = getParent();
                        if (parent2 != null) {
                            parent2.requestDisallowInterceptTouchEvent(true);
                        }
                        this.mIsBeingDragged = true;
                        iCalcRealOverScrollDist = iCalcRealOverScrollDist > 0 ? iCalcRealOverScrollDist - this.mTouchSlop : iCalcRealOverScrollDist + this.mTouchSlop;
                    }
                    if (this.mIsBeingDragged) {
                        this.mLastMotionX = x6;
                        int scrollRange = getScrollRange();
                        if (getScrollX() < 0) {
                            iCalcRealOverScrollDist = COUIPhysicalAnimationUtil.calcRealOverScrollDist(iCalcRealOverScrollDist, getScrollX(), this.mOverscrollDistance);
                        } else if (getScrollX() > getScrollRange()) {
                            iCalcRealOverScrollDist = COUIPhysicalAnimationUtil.calcRealOverScrollDist(iCalcRealOverScrollDist, getScrollX() - getScrollRange(), this.mOverscrollDistance);
                        }
                        if (overScrollBy(iCalcRealOverScrollDist, 0, getScrollX(), 0, scrollRange, 0, this.mOverscrollDistance, 0, true) && !hasNestedScrollingParent()) {
                            this.mVelocityTracker.clear();
                        }
                    }
                }
            } else if (action != 3) {
                if (action == 6) {
                    onSecondaryPointerUp(motionEvent);
                }
            } else if (this.mIsBeingDragged && getChildCount() > 0) {
                COUIIOverScroller cOUIIOverScroller8 = this.mOverScroller;
                if (cOUIIOverScroller8 != null && cOUIIOverScroller8.springBack(getScrollX(), getScrollY(), 0, getScrollRange(), 0, 0)) {
                    postInvalidateOnAnimation();
                }
                this.mActivePointerId = -1;
                this.mIsBeingDragged = false;
                recycleVelocityTracker();
            }
        } else {
            if (getChildCount() == 0) {
                return false;
            }
            COUIIOverScroller cOUIIOverScroller9 = this.mOverScroller;
            if (cOUIIOverScroller9 != null && !cOUIIOverScroller9.isCOUIFinished() && (parent = getParent()) != null) {
                parent.requestDisallowInterceptTouchEvent(true);
            }
            COUIIOverScroller cOUIIOverScroller10 = this.mOverScroller;
            if (cOUIIOverScroller10 != null && !cOUIIOverScroller10.isCOUIFinished()) {
                this.mAbortVelocityX = this.mOverScroller.getCurrVelocityX() != 0.0f ? this.mFlingVelocityX : 0.0f;
                this.mOverScroller.abortAnimation();
                if (this.mFlingStrictSpan) {
                    this.mFlingStrictSpan = false;
                }
            }
            int x10 = (int) motionEvent.getX();
            this.mLastMotionX = x10;
            this.mInitialTouchX = x10;
            this.mInitialTouchY = (int) motionEvent.getY();
            this.mActivePointerId = motionEvent.getPointerId(0);
        }
        return true;
    }

    @Override
    public void onVisibilityChanged(View view, int i2) {
        SpringOverScroller springOverScroller;
        super.onVisibilityChanged(view, i2);
        if (i2 == 0 || (springOverScroller = this.mSpringOverScroller) == null) {
            return;
        }
        springOverScroller.abortAnimation();
        this.mSpringOverScroller.cancelCallback();
    }

    @Override
    public boolean overScrollBy(int i2, int i6, int i10, int i11, int i12, int i13, int i14, int i15, boolean z6) {
        onOverScrolled(i10 + i2, i11 + i6, false, false);
        return false;
    }

    @Override
    public boolean pageScroll(int i2) {
        boolean z6 = i2 == 66;
        int width = getWidth();
        if (z6) {
            this.mTempRect.left = getScrollX() + width;
            if (getChildCount() > 0) {
                View childAt = getChildAt(0);
                if (this.mTempRect.left + width > childAt.getRight()) {
                    this.mTempRect.left = childAt.getRight() - width;
                }
            }
        } else {
            this.mTempRect.left = getScrollX() - width;
            Rect rect = this.mTempRect;
            if (rect.left < 0) {
                rect.left = 0;
            }
        }
        Rect rect2 = this.mTempRect;
        int i6 = rect2.left;
        int i10 = width + i6;
        rect2.right = i10;
        return scrollAndFocus(i2, i6, i10);
    }

    @Override
    public void requestChildFocus(View view, View view2) {
        if (view2 != null && view2.getRevealOnFocusHint()) {
            if (this.mIsLayoutDirty) {
                this.mChildToScrollTo = view2;
            } else {
                scrollToChild(view2);
            }
        }
        super.requestChildFocus(view, view2);
    }

    @Override
    public boolean requestChildRectangleOnScreen(View view, Rect rect, boolean z6) {
        rect.offset(view.getLeft() - view.getScrollX(), view.getTop() - view.getScrollY());
        return scrollToChildRect(rect, z6);
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean z6) {
        if (z6) {
            recycleVelocityTracker();
        }
        super.requestDisallowInterceptTouchEvent(z6);
    }

    @Override
    public void requestLayout() {
        this.mIsLayoutDirty = true;
        super.requestLayout();
    }

    public void setAvoidAccidentalTouch(boolean z6) {
        this.mAvoidAccidentalTouch = z6;
    }

    public void setDispatchEventWhileOverScrolling(boolean z6) {
        this.mEnableDispatchEventWhileOverScrolling = z6;
    }

    public void setDispatchEventWhileScrolling(boolean z6) {
        this.mEnableDispatchEventWhileScrolling = z6;
    }

    public void setDispatchEventWhileScrollingThreshold(int i2) {
        this.mDispatchEventVelocityThreshold = i2;
    }

    public void setEnableFlingSpeedIncrease(boolean z6) {
        SpringOverScroller springOverScroller = this.mSpringOverScroller;
        if (springOverScroller != null) {
            springOverScroller.setEnableFlingSpeedIncrease(z6);
        }
    }

    public void setEnableVibrator(boolean z6) {
        this.mEnableVibrator = z6;
    }

    public void setEventFilterTangent(float f2) {
        this.mEventFilterAngle = f2;
    }

    public void setFastFlingThreshold(float f2) {
        this.mFastFlingVelocity = Math.max(f2, 0.0f);
    }

    @Override
    public void setFillViewport(boolean z6) {
        if (z6 != this.mFillViewport) {
            this.mFillViewport = z6;
            requestLayout();
        }
    }

    public void setIsUseOptimizedScroll(boolean z6) {
        this.mEnableOptimizedScroll = z6;
    }

    public void setItemClickableWhileOverScrolling(boolean z6) {
        this.mItemClickableWhileOverScrolling = z6;
    }

    public void setItemClickableWhileSlowScrolling(boolean z6) {
        this.mItemClickableWhileSlowScrolling = z6;
    }

    @Override
    public void setSmoothScrollingEnabled(boolean z6) {
        this.mSmoothScrollingEnabled = z6;
    }

    public void setSpringOverScrollerDebug(boolean z6) {
        SpringOverScroller springOverScroller = this.mSpringOverScroller;
        if (springOverScroller != null) {
            springOverScroller.setDebug(z6);
        }
    }

    public final void smoothCOUIScrollBy(int i2, int i6) {
        if (getChildCount() == 0) {
            return;
        }
        if (AnimationUtils.currentAnimationTimeMillis() - this.mLastScroll > 250) {
            int iMax = Math.max(0, getChildAt(0).getWidth() - ((getWidth() - getPaddingRight()) - getPaddingLeft()));
            int scrollX = getScrollX();
            int iMax2 = Math.max(0, Math.min(i2 + scrollX, iMax)) - scrollX;
            COUIIOverScroller cOUIIOverScroller = this.mOverScroller;
            if (cOUIIOverScroller != null) {
                cOUIIOverScroller.startScroll(scrollX, getScrollY(), iMax2, 0);
            }
            postInvalidateOnAnimation();
        } else {
            COUIIOverScroller cOUIIOverScroller2 = this.mOverScroller;
            if (cOUIIOverScroller2 != null && !cOUIIOverScroller2.isCOUIFinished()) {
                this.mAbortVelocityX = this.mOverScroller.getCurrVelocityX() != 0.0f ? this.mFlingVelocityX : 0.0f;
                this.mOverScroller.abortAnimation();
                if (this.mFlingStrictSpan) {
                    this.mFlingStrictSpan = false;
                }
            }
            scrollBy(i2, i6);
        }
        this.mLastScroll = AnimationUtils.currentAnimationTimeMillis();
    }

    public final void smoothCOUIScrollTo(int i2, int i6) {
        smoothCOUIScrollBy(i2 - getScrollX(), i6 - getScrollY());
    }

    public COUIHorizontalScrollView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public COUIHorizontalScrollView(Context context, AttributeSet attributeSet, int i2) {
        super(context, attributeSet, i2);
        this.mScreenWidth = 0;
        this.mTempRect = new Rect();
        this.mOverScroller = null;
        this.mSpringOverScroller = null;
        this.mIsLayoutDirty = true;
        this.mChildToScrollTo = null;
        this.mIsBeingDragged = false;
        this.mSmoothScrollingEnabled = true;
        this.mActivePointerId = -1;
        this.mItemClickableWhileSlowScrolling = true;
        this.mItemClickableWhileOverScrolling = true;
        this.mEnableDispatchEventWhileScrolling = false;
        this.mEnableDispatchEventWhileOverScrolling = false;
        this.mDispatchEventVelocityThreshold = DEFAULT_INTERACTING_NESTED_SCROLL_VELOCITY_THRESHOLD;
        this.mEventFilterAngle = DEFAULT_INTERACTING_NESTED_SCROLL_ANGLE;
        this.mFastFlingVelocity = 1500.0f;
        this.mAvoidAccidentalTouch = true;
        this.mScrollStrictSpan = false;
        this.mFlingStrictSpan = false;
        this.mEnableOptimizedScroll = true;
        this.mEnableVibrator = true;
        this.mIsColorDevice = null;
        initCOUIHorizontalScrollView(context);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.COUIHorizontalScrollView, i2, 0);
        this.mEnableVibrator = typedArrayObtainStyledAttributes.getBoolean(R.styleable.COUIHorizontalScrollView_couiScrollViewEnableVibrator, true);
        typedArrayObtainStyledAttributes.recycle();
    }

    public COUIHorizontalScrollView(Context context, AttributeSet attributeSet, int i2, int i6) {
        super(context, attributeSet, i2, i6);
        this.mScreenWidth = 0;
        this.mTempRect = new Rect();
        this.mOverScroller = null;
        this.mSpringOverScroller = null;
        this.mIsLayoutDirty = true;
        this.mChildToScrollTo = null;
        this.mIsBeingDragged = false;
        this.mSmoothScrollingEnabled = true;
        this.mActivePointerId = -1;
        this.mItemClickableWhileSlowScrolling = true;
        this.mItemClickableWhileOverScrolling = true;
        this.mEnableDispatchEventWhileScrolling = false;
        this.mEnableDispatchEventWhileOverScrolling = false;
        this.mDispatchEventVelocityThreshold = DEFAULT_INTERACTING_NESTED_SCROLL_VELOCITY_THRESHOLD;
        this.mEventFilterAngle = DEFAULT_INTERACTING_NESTED_SCROLL_ANGLE;
        this.mFastFlingVelocity = 1500.0f;
        this.mAvoidAccidentalTouch = true;
        this.mScrollStrictSpan = false;
        this.mFlingStrictSpan = false;
        this.mEnableOptimizedScroll = true;
        this.mEnableVibrator = true;
        this.mIsColorDevice = null;
        initCOUIHorizontalScrollView(context);
    }
}


