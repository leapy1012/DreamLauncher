package androidx.recyclerview.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;
import androidx.core.view.MotionEventCompat;
import androidx.core.view.ViewCompat;
import com.coui.appcompat.R;
import androidx.recyclerview.widget.RecyclerView;
import com.coui.appcompat.animation.COUIPhysicalAnimationUtil;
import com.coui.appcompat.contextutil.COUIContextUtil;
import com.coui.appcompat.hapticfeedback.COUIHapticFeedbackConstants;
import com.coui.appcompat.log.COUILog;
import com.coui.appcompat.scroll.COUIFlingLocateHelper;
import com.coui.appcompat.scroll.COUIIOverScroller;
import com.coui.appcompat.scroll.COUILocateOverScroller;
import com.coui.appcompat.scroll.SpringOverScroller;
import com.coui.appcompat.scrollbar.COUIScrollBar;
import com.coui.appcompat.uiutil.UIUtil;
import com.coui.appcompat.vibrateutil.VibrateUtils;
import com.coui.appcompat.view.ViewNative;
import java.util.ArrayList;

public class COUIRecyclerView extends RecyclerView implements COUIScrollBar.COUIScrollable {
    public static final int CENTER_ALIGN = 2;
    private static final boolean COUI_DEBUG;
    private static final int CUSTOM_TOUCH_SLOP = 2;
    private static final int DEBUG_PAINT_TEXT_OFFSET_Y = 50;
    private static final int DEBUG_PAINT_TEXT_SIZE = 30;
    private static final float DEFAULT_INTERACTING_NESTED_SCROLL_ANGLE = 20.0f;
    private static final int DEFAULT_INTERACTING_NESTED_SCROLL_VELOCITY_THRESHOLD = 2500;
    private static final double DEGREE_TO_ARC_CONSTANT = 0.017453292519943295d;
    private static final int FLING_SCROLL_THRESHOLD = 1000;
    private static final int FLING_SCROLL_THRESHOLD_WHILE_OVER_SCROLLING = 6000;
    private static final float HORIZONTAL_SPRING_BACK_TENSION_MULTIPLE = 3.2f;
    private static final int INVALID_POINTER = -1;
    private static final int OVER_SCROLL_TOUCH_DURATION_THRESHOLD;
    private static final int OVER_SCROLL_TOUCH_OFFSET_THRESHOLD = 10;
    private static final int SLOW_SCROLL_THRESHOLD = 2500;
    public static final int START_ALIGN = 1;
    static final String TAG = "COUIRecyclerView";
    private static final float VERTICAL_SPRING_BACK_TENSION_MULTIPLE = 2.15f;
    final int FLING;
    final int OVER_FLING;
    final int OVER_SCROLLING;
    private final int SCROLLBARS_NONE;
    private final int SCROLLBARS_VERTICAL;
    final int SCROLLING;
    private float mAbortVelocityX;
    private float mAbortVelocityY;
    private boolean mAvoidAccidentalTouch;
    private COUILocateOverScroller mCOUILocateOverScroller;
    // Leapy modified 2026-07-22: Use OPPO's decoded divider manager contract.
    private COUIRecyclerDividerManager mCOUIRecyclerDividerManager;
    private COUIScrollBar mCOUIScrollBar;
    private float mClickVelocityX;
    private float mClickVelocityY;
    private float mDebugAbortVelocityX;
    private float mDebugAbortVelocityY;
    private Paint mDebugPaint;
    private int mDispatchEventVelocityThreshold;
    private boolean mEnableDispatchEventWhileOverScrolling;
    private boolean mEnableDispatchEventWhileScrolling;
    private boolean mEnableFlingSpeedIncrease;
    private boolean mEnableOptimizedScroll;
    private boolean mEnablePointerDown;
    private boolean mEnableVibrator;
    private float mEventFilterAngle;
    private float mFastFlingVelocity;
    private boolean mFixScrollTypeForOverScrolling;
    private float mFlingRatio;
    private float mFlingVelocityX;
    private float mFlingVelocityY;
    private boolean mIgnoreMotionEventTillDown;
    private int mInitialTouchX;
    private int mInitialTouchY;
    private RecyclerView.OnItemTouchListener mInterceptingOnItemTouchListener;
    private boolean mIsOverScrollingReverseFling;
    private boolean mIsTouchDownWhileOverScrolling;
    private boolean mIsTouchDownWhileSlowScrolling;
    private boolean mIsUseNativeOverScroll;
    private boolean mItemClickableWhileOverScrolling;
    private boolean mItemClickableWhileSlowScrolling;
    private int mLastTouchX;
    private int mLastTouchY;
    private COUIFlingLocateHelper mLocateHelper;
    private final int mMaxFlingVelocity;
    private final int mMinFlingVelocity;
    private final int[] mNestedOffsets;
    private RecyclerView.OnFlingListener mOnFlingListener;
    private ArrayList<RecyclerView.OnItemTouchListener> mOnItemTouchListeners;
    boolean mOverScrollEnable;
    private COUIIOverScroller mOverScroller;
    private int mOverflingDistance;
    private int mOverscrollDistance;
    private int mScreenHeight;
    private int mScreenWidth;
    private final int[] mScrollOffset;
    private int mScrollPointerId;
    private int mScrollState;
    private int mScrollType;
    private Drawable mScrollbarThumbVertical;
    private int mScrollbars;
    private int mScrollbarsSize;
    private int mSlowScrollThreshold;
    private boolean mSmoothScrollFlag;
    private SpringOverScroller mSpringOverScroller;
    private int mStyle;
    private int mTouchSlop;
    private long mTouchTime;
    private VelocityTracker mVelocityTracker;
    private float mVerticalSpringOverTension;
    private ViewFlinger mViewFlinger;

    // Leapy modified 2026-07-22: Restore the exact OPPO nested decoration API.
    public static class COUIDividerItemDecoration extends RecyclerView.ItemDecoration {
        private Drawable mDivider;
        private int mDividerColor;
        private int mDividerStrokeWidth;
        private int mOriginAlpha;
        private Paint mPaint;
        private int mPressDividerAlpha;
        private int mPressDividerPos;
        private int mPrevTop;

        public COUIDividerItemDecoration(Context context) {
            init(context);
        }

        private void init(Context context) {
            this.mDividerColor = COUIContextUtil.getAttrColor(context, R.attr.couiColorDivider);
            this.mDividerStrokeWidth = context.getResources().getDimensionPixelOffset(R.dimen.coui_list_divider_height);
            Paint paint = new Paint(1);
            this.mPaint = paint;
            paint.setColor(this.mDividerColor);
            int alpha = this.mPaint.getAlpha();
            this.mOriginAlpha = alpha;
            this.mPressDividerAlpha = alpha;
        }

        public void drawDividerOuterBackground(Canvas canvas, RecyclerView recyclerView, View view) {
        }

        public void drawExpandableDivider(Canvas canvas, RecyclerView.ViewHolder viewHolder) {
            View view = viewHolder.itemView;
            boolean z6 = view.getLayoutDirection() == 1;
            int measuredHeight = view.getMeasuredHeight() - Math.max(1, this.mDividerStrokeWidth);
            int measuredHeight2 = view.getMeasuredHeight();
            int x6 = (int) (view.getX() + (z6 ? getDividerInsetEnd(viewHolder) : getDividerInsetStart(viewHolder)));
            int x10 = (int) ((view.getX() + view.getWidth()) - (z6 ? getDividerInsetStart(viewHolder) : getDividerInsetEnd(viewHolder)));
            Drawable drawable = this.mDivider;
            if (drawable == null) {
                canvas.drawRect(x6, measuredHeight, x10, measuredHeight2, this.mPaint);
            } else {
                drawable.setBounds(x6, measuredHeight, x10, measuredHeight2);
                this.mDivider.draw(canvas);
            }
        }

        public Drawable getDivider() {
            return this.mDivider;
        }

        public int getDividerColor() {
            return this.mDividerColor;
        }

        public int getDividerInsetEnd(RecyclerView.ViewHolder viewHolder) {
            return 0;
        }

        public int getDividerInsetStart(RecyclerView.ViewHolder viewHolder) {
            return 0;
        }

        public int getDividerStrokeWidth() {
            return this.mDividerStrokeWidth;
        }

        public Paint getPaint() {
            return this.mPaint;
        }

        @Override
        public void onDrawOver(Canvas canvas, RecyclerView recyclerView, RecyclerView.State b0Var) {
            int childCount = recyclerView.getChildCount();
            this.mPrevTop = -1;
            int i2 = 0;
            while (i2 < childCount) {
                View childAt = recyclerView.getChildAt(i2);
                if (shouldDrawDivider(recyclerView, i2)) {
                    drawDividerOuterBackground(canvas, recyclerView, childAt);
                    boolean z6 = childAt.getLayoutDirection() == 1;
                    int y6 = (int) (childAt.getY() + childAt.getHeight());
                    if (this.mPrevTop != y6) {
                        this.mPrevTop = y6;
                        int iMax = Math.max(1, this.mDividerStrokeWidth) + y6;
                        int x6 = (int) (childAt.getX() + (z6 ? getDividerInsetEnd(recyclerView, i2) : getDividerInsetStart(recyclerView, i2)));
                        int x10 = (int) ((childAt.getX() + childAt.getWidth()) - (z6 ? getDividerInsetStart(recyclerView, i2) : getDividerInsetEnd(recyclerView, i2)));
                        int i6 = this.mPressDividerPos;
                        int i10 = (i6 == i2 || i6 + (-1) == i2) ? this.mPressDividerAlpha : this.mOriginAlpha;
                        Drawable drawable = this.mDivider;
                        if (drawable == null) {
                            this.mPaint.setAlpha(i10);
                            canvas.drawRect(x6, y6, x10, iMax, this.mPaint);
                        } else {
                            drawable.setAlpha(i10);
                            this.mDivider.setBounds(x6, y6, x10, iMax);
                            this.mDivider.draw(canvas);
                        }
                    }
                }
                i2++;
            }
        }

        public void setDivider(RecyclerView recyclerView, Drawable drawable) {
            this.mDivider = drawable;
            if (recyclerView != null) {
                recyclerView.invalidateItemDecorations();
            }
        }

        public void setDividerColor(RecyclerView recyclerView, int i2) {
            this.mDividerColor = i2;
            this.mPaint.setColor(i2);
            if (recyclerView != null) {
                recyclerView.invalidateItemDecorations();
            }
        }

        public void setDividerStrokeWidth(RecyclerView recyclerView, int i2) {
            this.mDividerStrokeWidth = i2;
            this.mPaint.setStrokeWidth(i2);
            if (recyclerView != null) {
                recyclerView.invalidateItemDecorations();
            }
        }

        public void setPressDividerAlpha(int i2) {
            this.mPressDividerAlpha = i2;
        }

        public void setPressDividerPos(int i2) {
            this.mPressDividerPos = i2;
        }

        public boolean shouldDrawDivider(RecyclerView recyclerView, int i2) {
            RecyclerView.Adapter adapter = recyclerView.getAdapter();
            return adapter == null || adapter.getItemCount() - 1 != i2;
        }

        public int getDividerInsetEnd(RecyclerView recyclerView, int i2) {
            return 0;
        }

        public int getDividerInsetStart(RecyclerView recyclerView, int i2) {
            return 0;
        }
    }

    public interface ICOUIDividerDecorationInterface {
        default boolean drawDivider() {
            return false;
        }

        default View getDividerEndAlignView() {
            return null;
        }

        default int getDividerEndInset() {
            return 0;
        }

        default View getDividerStartAlignView() {
            return null;
        }

        default int getDividerStartInset() {
            return 0;
        }
    }

    public class ViewFlinger implements Runnable {
        public int mLastFlingX;
        public int mLastFlingY;
        public Interpolator mInterpolator = RecyclerView.sQuinticInterpolator;
        public boolean mEatRunOnAnimationRequest = false;
        public boolean mReSchedulePostAnimationCallback = false;

        public ViewFlinger() {
        }

        public final int computeScrollDuration(int i2, int i6, int i10, int i11) {
            int iRound;
            int iAbs = Math.abs(i2);
            int iAbs2 = Math.abs(i6);
            boolean z6 = iAbs > iAbs2;
            int iSqrt = (int) Math.sqrt((i10 * i10) + (i11 * i11));
            int iSqrt2 = (int) Math.sqrt((i2 * i2) + (i6 * i6));
            COUIRecyclerView cOUIRecyclerView = COUIRecyclerView.this;
            int width = z6 ? cOUIRecyclerView.getWidth() : cOUIRecyclerView.getHeight();
            int i12 = width / 2;
            float f2 = width;
            float f10 = i12;
            float fB = f10 + (distanceInfluenceForSnapDuration(Math.min(1.0f, (iSqrt2 * 1.0f) / f2)) * f10);
            if (iSqrt > 0) {
                iRound = Math.round(Math.abs(fB / iSqrt) * 1000.0f) * 4;
            } else {
                if (!z6) {
                    iAbs = iAbs2;
                }
                iRound = (int) (((iAbs / f2) + 1.0f) * 300.0f);
            }
            return Math.min(iRound, VibrateUtils.STRENGTH_MAX_STEP);
        }

        public final float distanceInfluenceForSnapDuration(float f2) {
            return (float) Math.sin((f2 - 0.5f) * 0.47123894f);
        }

        public void fling(int i2, int i6) {
            COUIRecyclerView.this.mFlingVelocityX = i2;
            COUIRecyclerView.this.mFlingVelocityY = i6;
            COUIRecyclerView.this.setScrollState(2);
            this.mLastFlingY = 0;
            this.mLastFlingX = 0;
            Interpolator interpolator = this.mInterpolator;
            Interpolator interpolator2 = RecyclerView.sQuinticInterpolator;
            if (interpolator != interpolator2) {
                this.mInterpolator = interpolator2;
                if (COUIRecyclerView.this.mOverScroller != null) {
                    COUIRecyclerView.this.mOverScroller.setInterpolator(interpolator2);
                }
            }
            if (COUIRecyclerView.this.mOverScroller != null) {
                COUIRecyclerView.this.mOverScroller.fling(0, 0, i2, i6, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
                COUIRecyclerView.this.mOverScroller.setFinalX(COUIRecyclerView.this.mLocateHelper.getTargetViewDistance(COUIRecyclerView.this.mOverScroller.getCOUIFinalX()));
            }
            requestPostOnAnimation();
        }

        public final void postOnAnimation() {
            COUIRecyclerView.this.removeCallbacks(this);
            ViewCompat.postOnAnimation(COUIRecyclerView.this, this);
        }

        public void requestPostOnAnimation() {
            if (this.mEatRunOnAnimationRequest) {
                this.mReSchedulePostAnimationCallback = true;
            } else {
                postOnAnimation();
            }
        }

        public void smoothScrollBy(int i2, int i6, int i10, Interpolator interpolator) {
            if (i10 == Integer.MIN_VALUE) {
                i10 = computeScrollDuration(i2, i6, 0, 0);
            }
            int i11 = i10;
            if (interpolator == null) {
                interpolator = RecyclerView.sQuinticInterpolator;
            }
            if (this.mInterpolator != interpolator) {
                this.mInterpolator = interpolator;
                if (COUIRecyclerView.this.mOverScroller != null) {
                    COUIRecyclerView.this.mOverScroller.setInterpolator(interpolator);
                }
            }
            this.mLastFlingY = 0;
            this.mLastFlingX = 0;
            COUIRecyclerView.this.setScrollState(2);
            if (COUIRecyclerView.this.mOverScroller != null) {
                COUIRecyclerView.this.mOverScroller.startScroll(0, 0, i2, i6, i11);
            }
            requestPostOnAnimation();
        }

        public void stop() {
            COUIRecyclerView.this.removeCallbacks(this);
            COUIRecyclerView cOUIRecyclerView = COUIRecyclerView.this;
            cOUIRecyclerView.ensureOverScrollers(cOUIRecyclerView.getContext());
            COUIRecyclerView cOUIRecyclerView2 = COUIRecyclerView.this;
            float f2 = 0.0f;
            cOUIRecyclerView2.mAbortVelocityX = (cOUIRecyclerView2.mOverScroller == null || COUIRecyclerView.this.mOverScroller.getCurrVelocityX() == 0.0f) ? 0.0f : COUIRecyclerView.this.mFlingVelocityX;
            COUIRecyclerView cOUIRecyclerView3 = COUIRecyclerView.this;
            if (cOUIRecyclerView3.mOverScroller != null && COUIRecyclerView.this.mOverScroller.getCurrVelocityY() != 0.0f) {
                f2 = COUIRecyclerView.this.mFlingVelocityY;
            }
            cOUIRecyclerView3.mAbortVelocityY = f2;
            if (COUIRecyclerView.this.mOverScroller != null) {
                COUIRecyclerView.this.mOverScroller.abortAnimation();
            }
            if (COUIRecyclerView.this.mSpringOverScroller != null) {
                COUIRecyclerView.this.mSpringOverScroller.abortAnimation();
            }
        }

        @Override
        public void run() {
            int i2;
            int i6;
            int i10;
            COUIRecyclerView cOUIRecyclerView = COUIRecyclerView.this;
            if (cOUIRecyclerView.mLayout == null) {
                stop();
                return;
            }
            this.mReSchedulePostAnimationCallback = false;
            this.mEatRunOnAnimationRequest = true;
            cOUIRecyclerView.consumePendingUpdateOperations();
            COUIIOverScroller cOUIIOverScroller = COUIRecyclerView.this.mOverScroller;
            if (cOUIIOverScroller != null && cOUIIOverScroller.computeScrollOffset()) {
                int cOUICurrX = cOUIIOverScroller.getCOUICurrX();
                int cOUICurrY = cOUIIOverScroller.getCOUICurrY();
                int i11 = cOUICurrX - this.mLastFlingX;
                int i12 = cOUICurrY - this.mLastFlingY;
                this.mLastFlingX = cOUICurrX;
                this.mLastFlingY = cOUICurrY;
                COUIRecyclerView cOUIRecyclerView2 = COUIRecyclerView.this;
                int[] iArr = cOUIRecyclerView2.mReusableIntPair;
                iArr[0] = 0;
                iArr[1] = 0;
                if (cOUIRecyclerView2.dispatchNestedPreScroll(i11, i12, iArr, null, 1)) {
                    int[] iArr2 = COUIRecyclerView.this.mReusableIntPair;
                    i11 -= iArr2[0];
                    i12 -= iArr2[1];
                }
                COUIRecyclerView cOUIRecyclerView3 = COUIRecyclerView.this;
                if (cOUIRecyclerView3.mAdapter != null) {
                    int[] iArr3 = cOUIRecyclerView3.mReusableIntPair;
                    iArr3[0] = 0;
                    iArr3[1] = 0;
                    cOUIRecyclerView3.scrollStep(i11, i12, iArr3);
                    COUIRecyclerView cOUIRecyclerView4 = COUIRecyclerView.this;
                    int[] iArr4 = cOUIRecyclerView4.mReusableIntPair;
                    i6 = iArr4[0];
                    i2 = iArr4[1];
                    i11 -= i6;
                    i12 -= i2;
                    RecyclerView.SmoothScroller a0Var = cOUIRecyclerView4.mLayout.mSmoothScroller;
                    if (a0Var != null && !a0Var.isPendingInitialRun() && a0Var.isRunning()) {
                        int iB = COUIRecyclerView.this.mState.getItemCount();
                        if (iB == 0) {
                            a0Var.stop();
                        } else if (a0Var.getTargetPosition() >= iB) {
                            a0Var.setTargetPosition(iB - 1);
                            a0Var.onAnimation(i6, i2);
                        } else {
                            a0Var.onAnimation(i6, i2);
                        }
                    }
                } else {
                    i2 = 0;
                    i6 = 0;
                }
                if (!COUIRecyclerView.this.mItemDecorations.isEmpty()) {
                    COUIRecyclerView.this.invalidate();
                }
                COUIRecyclerView cOUIRecyclerView5 = COUIRecyclerView.this;
                int[] iArr5 = cOUIRecyclerView5.mReusableIntPair;
                iArr5[0] = 0;
                iArr5[1] = 0;
                cOUIRecyclerView5.dispatchNestedScroll(i6, i2, i11, i12, null, 1, iArr5);
                COUIRecyclerView cOUIRecyclerView6 = COUIRecyclerView.this;
                int[] iArr6 = cOUIRecyclerView6.mReusableIntPair;
                int i13 = i11 - iArr6[0];
                int i14 = i12 - iArr6[1];
                if (i6 != 0 || i2 != 0) {
                    cOUIRecyclerView6.dispatchOnScrolled(i6, i2);
                }
                if (!COUIRecyclerView.this.mSmoothScrollFlag || (i13 == 0 && i14 == 0)) {
                    i10 = i14;
                } else {
                    cOUIIOverScroller.abortAnimation();
                    COUIRecyclerView.this.mSmoothScrollFlag = false;
                    i10 = 0;
                    i13 = 0;
                }
                if (i10 != 0) {
                    COUIRecyclerView cOUIRecyclerView7 = COUIRecyclerView.this;
                    if (cOUIRecyclerView7.mOverScrollEnable) {
                        cOUIRecyclerView7.mScrollType = 3;
                        COUIRecyclerView.this.performEdgeHapticFeedback();
                        COUIRecyclerView cOUIRecyclerView8 = COUIRecyclerView.this;
                        cOUIRecyclerView8.overScrollBy(0, i10, 0, cOUIRecyclerView8.getScrollY(), 0, 0, 0, COUIRecyclerView.this.mOverflingDistance, false);
                        if (COUIRecyclerView.this.mIsUseNativeOverScroll) {
                            if (COUIRecyclerView.this.mSpringOverScroller != null) {
                                COUIRecyclerView.this.mSpringOverScroller.setCurrVelocityY(cOUIIOverScroller.getCurrVelocityY());
                                COUIRecyclerView.this.mSpringOverScroller.notifyVerticalEdgeReached(i10, 0, COUIRecyclerView.this.mOverflingDistance);
                            }
                        } else if (COUIRecyclerView.this.mOverScroller != null) {
                            COUIRecyclerView.this.mOverScroller.notifyVerticalEdgeReached(i10, 0, COUIRecyclerView.this.mOverflingDistance);
                        }
                    }
                }
                if (i13 != 0) {
                    COUIRecyclerView cOUIRecyclerView9 = COUIRecyclerView.this;
                    if (cOUIRecyclerView9.mOverScrollEnable) {
                        cOUIRecyclerView9.mScrollType = 3;
                        COUIRecyclerView.this.performEdgeHapticFeedback();
                        COUIRecyclerView cOUIRecyclerView10 = COUIRecyclerView.this;
                        cOUIRecyclerView10.overScrollBy(i13, 0, cOUIRecyclerView10.getScrollX(), 0, 0, 0, COUIRecyclerView.this.mOverflingDistance, 0, false);
                        if (COUIRecyclerView.this.mIsUseNativeOverScroll) {
                            if (COUIRecyclerView.this.mSpringOverScroller != null) {
                                COUIRecyclerView.this.mSpringOverScroller.setCurrVelocityX(cOUIIOverScroller.getCurrVelocityX());
                                COUIRecyclerView.this.mSpringOverScroller.notifyHorizontalEdgeReached(i13, 0, COUIRecyclerView.this.mOverflingDistance);
                            }
                        } else if (COUIRecyclerView.this.mOverScroller != null) {
                            COUIRecyclerView.this.mOverScroller.notifyHorizontalEdgeReached(i13, 0, COUIRecyclerView.this.mOverflingDistance);
                        }
                    }
                }
                if (!COUIRecyclerView.this.awakenScrollBars()) {
                    COUIRecyclerView.this.invalidate();
                }
                boolean z6 = cOUIIOverScroller.isCOUIFinished() || (((cOUIIOverScroller.getCOUICurrX() == cOUIIOverScroller.getCOUIFinalX()) || i13 != 0) && ((cOUIIOverScroller.getCOUICurrY() == cOUIIOverScroller.getCOUIFinalY()) || i10 != 0));
                RecyclerView.SmoothScroller a0Var2 = COUIRecyclerView.this.mLayout.mSmoothScroller;
                if ((a0Var2 != null && a0Var2.isPendingInitialRun()) || !z6) {
                    requestPostOnAnimation();
                    COUIRecyclerView cOUIRecyclerView11 = COUIRecyclerView.this;
                    GapWorker iVar = cOUIRecyclerView11.mGapWorker;
                    if (iVar != null) {
                        iVar.postFromTraversal(cOUIRecyclerView11, i6, i2);
                    }
                } else if (RecyclerView.ALLOW_THREAD_GAP_WORK) {
                    COUIRecyclerView.this.mPrefetchRegistry.clearPrefetchPositions();
                }
            }
            RecyclerView.SmoothScroller a0Var3 = COUIRecyclerView.this.mLayout.mSmoothScroller;
            if (a0Var3 != null && a0Var3.isPendingInitialRun()) {
                a0Var3.onAnimation(0, 0);
            }
            this.mEatRunOnAnimationRequest = false;
            if (this.mReSchedulePostAnimationCallback) {
                postOnAnimation();
            } else {
                if (COUIRecyclerView.this.mScrollType == 3 && COUIRecyclerView.this.mOverScrollEnable) {
                    return;
                }
                COUIRecyclerView.this.setScrollState(0);
                COUIRecyclerView.this.stopNestedScroll(1);
            }
        }
    }

    static {
        COUI_DEBUG = COUILog.LOG_DEBUG || COUILog.isLoggable(TAG, 3);
        OVER_SCROLL_TOUCH_DURATION_THRESHOLD = ViewConfiguration.getLongPressTimeout();
    }

    public COUIRecyclerView(Context context) {
        this(context, null);
    }

    public void performEdgeHapticFeedback() {
        if (this.mEnableVibrator) {
            performHapticFeedback(COUIHapticFeedbackConstants.EDGE_LIST_VIBRATE);
        }
    }

    private float getVelocityAlongScrollableDirection() {
        COUIIOverScroller cOUIIOverScroller;
        COUIIOverScroller cOUIIOverScroller2;
        RecyclerView.LayoutManager layoutManager = getLayoutManager();
        if (!(layoutManager instanceof LinearLayoutManager)) {
            return 0.0f;
        }
        if (layoutManager.canScrollHorizontally() && (cOUIIOverScroller2 = this.mOverScroller) != null) {
            return cOUIIOverScroller2.getCurrVelocityX();
        }
        if (!layoutManager.canScrollVertically() || (cOUIIOverScroller = this.mOverScroller) == null) {
            return 0.0f;
        }
        return cOUIIOverScroller.getCurrVelocityY();
    }

    public final void clearVelocityTrackerAndStopNestedScroll() {
        VelocityTracker velocityTracker = this.mVelocityTracker;
        if (velocityTracker != null) {
            velocityTracker.clear();
        }
        stopNestedScroll(0);
    }

    public final void stopScrollersAndSmoothScroller() {
        ensureViewFlinger();
        this.mViewFlinger.stop();
        RecyclerView.LayoutManager pVar = this.mLayout;
        if (pVar != null) {
            pVar.stopSmoothScroller();
        }
    }

    public final void cancelTouch() {
        clearVelocityTrackerAndStopNestedScroll();
        setScrollState(0);
    }

    public final void dispatchIdleScrollStateIfNeeded() {
        if (this.mScrollState != 0) {
            this.mScrollState = 0;
            dispatchOnScrollStateChanged(0);
        }
    }

    public final void initCOUIScrollBar(Context context) {
        this.mCOUIScrollBar = new COUIScrollBar.Builder(this).build();
    }

    public final boolean dispatchSyntheticClickToChild(View view, MotionEvent motionEvent) {
        boolean zDispatchTouchEvent = true;
        int[] iArr = {0, 1};
        for (int i2 = 0; i2 < 2; i2++) {
            motionEvent.setAction(iArr[i2]);
            zDispatchTouchEvent &= view.dispatchTouchEvent(motionEvent);
        }
        return zDispatchTouchEvent;
    }

    public final View dispatchClickToTouchedChild(MotionEvent motionEvent) {
        if (!isTapGesture(motionEvent)) {
            return null;
        }
        Rect rect = new Rect();
        View view = null;
        for (int childCount = getChildCount() - 1; childCount >= 0; childCount--) {
            View childAt = getChildAt(childCount);
            if (childAt.getVisibility() == 0 || childAt.getAnimation() != null) {
                childAt.getHitRect(rect);
                boolean zContains = rect.contains(((int) motionEvent.getX()) + getScrollX(), ((int) motionEvent.getY()) + getScrollY());
                MotionEvent motionEventObtain = MotionEvent.obtain(motionEvent);
                motionEventObtain.offsetLocation(getScrollX() - childAt.getLeft(), getScrollY() - childAt.getTop());
                if (zContains && dispatchSyntheticClickToChild(childAt, motionEventObtain)) {
                    view = childAt;
                }
                motionEventObtain.recycle();
                if (COUI_DEBUG) {
                    childAt.setBackground(childAt == view ? new ColorDrawable(Color.parseColor("#80FF0000")) : null);
                }
            }
        }
        return view;
    }

    public final boolean shouldAcceptDispatchTouchEvent(float f2, float f10) {
        return !(this.mEnableDispatchEventWhileScrolling || (this.mEnableDispatchEventWhileOverScrolling && isOverScrolling())) || f2 == 0.0f || ((double) Math.abs(f10 / f2)) > Math.tan(((double) this.mEventFilterAngle) * DEGREE_TO_ARC_CONSTANT);
    }

    public final void readCOUIRecyclerViewAttributes(Context context, AttributeSet attributeSet, int i2) {
        if (attributeSet == null || attributeSet.getStyleAttribute() == 0) {
            this.mStyle = i2;
        } else {
            this.mStyle = attributeSet.getStyleAttribute();
        }
        if (context != null) {
            TypedArray typedArrayObtainStyledAttributes = getContext().obtainStyledAttributes(attributeSet, R.styleable.COUIRecyclerView, i2, 0);
            this.mScrollbars = typedArrayObtainStyledAttributes.getInteger(R.styleable.COUIRecyclerView_couiScrollbars, 0);
            this.mScrollbarsSize = typedArrayObtainStyledAttributes.getDimensionPixelSize(R.styleable.COUIRecyclerView_couiScrollbarSize, 0);
            this.mScrollbarThumbVertical = typedArrayObtainStyledAttributes.getDrawable(R.styleable.COUIRecyclerView_couiScrollbarThumbVertical);
            this.mEnableVibrator = typedArrayObtainStyledAttributes.getBoolean(R.styleable.COUIRecyclerView_couiRecyclerViewEnableVibrator, true);
            typedArrayObtainStyledAttributes.recycle();
        }
    }

    public final void ensureOnItemTouchListeners() {
        if (this.mOnItemTouchListeners == null) {
            this.mOnItemTouchListeners = new ArrayList<>();
        }
    }

    public final void ensureOverScrollers(Context context) {
        if (this.mOverScroller == null) {
            this.mVerticalSpringOverTension = VERTICAL_SPRING_BACK_TENSION_MULTIPLE;
            this.mSpringOverScroller = new SpringOverScroller(context);
            this.mCOUILocateOverScroller = new COUILocateOverScroller(context);
            enableFrameRate(true);
            setIsUseNativeOverScroll(false);
            setEnableFlingSpeedIncrease(this.mEnableFlingSpeedIncrease);
        }
    }

    public final void initOverScrollDistances(Context context) {
        int i2 = context.getResources().getDisplayMetrics().heightPixels;
        this.mOverscrollDistance = i2;
        this.mOverflingDistance = i2;
    }

    public final void ensureViewFlinger() {
        if (this.mViewFlinger == null) {
            this.mViewFlinger = new ViewFlinger();
        }
    }

    public final boolean isTapGesture(MotionEvent motionEvent) {
        int x6 = (int) (motionEvent.getX() - this.mInitialTouchX);
        int y6 = (int) (motionEvent.getY() - this.mInitialTouchY);
        int iSqrt = (int) Math.sqrt((x6 * x6) + (y6 * y6));
        long jCurrentTimeMillis = System.currentTimeMillis() - this.mTouchTime;
        if (COUI_DEBUG) {
            Log.d(TAG, "onTouchEvent: ACTION_UP. touchDuration = " + jCurrentTimeMillis + ", offset = " + iSqrt);
        }
        return jCurrentTimeMillis < ((long) OVER_SCROLL_TOUCH_DURATION_THRESHOLD) && iSqrt < 10;
    }

    private boolean isDrawDivider(View view, int i2) {
        return this.mCOUIRecyclerDividerManager.isDrawDivider(view, i2);
    }

    public final boolean passesAccidentalTouchFilter(float f2, float f10) {
        return !this.mAvoidAccidentalTouch || Math.abs(f2) > this.mFastFlingVelocity || Math.abs(f10) > this.mFastFlingVelocity;
    }

    public final boolean isOverScrolling() {
        int i2;
        return this.mOverScrollEnable && ((i2 = this.mScrollType) == 2 || i2 == 3) && hasOverScrollOffset();
    }

    public final boolean hasOverScrollOffset() {
        RecyclerView.LayoutManager pVar = this.mLayout;
        if (pVar == null) {
            return false;
        }
        return (pVar.canScrollVertically() && this.mLayout.canScrollHorizontally()) ? (getScrollY() == 0 || getScrollX() == 0) ? false : true : this.mLayout.canScrollVertically() ? getScrollY() != 0 : this.mLayout.canScrollHorizontally() && getScrollX() != 0;
    }

    public final void snapLocateTarget() {
        this.mLocateHelper.trySnapToTargetExistingView();
    }

    @Override
    public void addOnItemTouchListener(RecyclerView.OnItemTouchListener tVar) {
        ensureOnItemTouchListeners();
        this.mOnItemTouchListeners.add(tVar);
    }

    @Override
    public boolean awakenScrollBars() {
        COUIScrollBar cOUIScrollBar = this.mCOUIScrollBar;
        return cOUIScrollBar != null ? cOUIScrollBar.awakenScrollBars() : super.awakenScrollBars();
    }

    public final boolean isHorizontalLinearLayout() {
        return getLayoutManager() != null && (getLayoutManager() instanceof LinearLayoutManager) && ((LinearLayoutManager) getLayoutManager()).getOrientation() == 0;
    }

    public final void resetOverScrollState() {
        clearVelocityTrackerAndStopNestedScroll();
        setScrollState(0);
        ViewNative.setScrollX(this, 0);
        ViewNative.setScrollY(this, 0);
        this.mScrollType = 0;
    }
    public final boolean interceptTouchEventInternal(MotionEvent motionEvent) {
        boolean z6;
        COUIScrollBar cOUIScrollBar = this.mCOUIScrollBar;
        if (cOUIScrollBar != null && cOUIScrollBar.onInterceptTouchEvent(motionEvent)) {
            return true;
        }
        if (this.mLayoutSuppressed) {
            return false;
        }
        this.mInterceptingOnItemTouchListener = null;
        if (findInterceptingOnItemTouchListener(motionEvent)) {
            resetOverScrollState();
            return true;
        }
        RecyclerView.LayoutManager pVar = this.mLayout;
        if (pVar == null) {
            return false;
        }
        boolean zCanScrollHorizontally = pVar.canScrollHorizontally();
        boolean zCanScrollVertically = this.mLayout.canScrollVertically();
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
        this.mVelocityTracker.addMovement(motionEvent);
        int actionMasked = motionEvent.getActionMasked();
        int adjustmentPointerIndex = UIUtil.getAdjustmentPointerIndex(motionEvent, motionEvent.getActionIndex());
        if (actionMasked == 0) {
            if (this.mIgnoreMotionEventTillDown) {
                this.mIgnoreMotionEventTillDown = false;
            }
            COUIIOverScroller cOUIIOverScroller = this.mOverScroller;
            float currVelocityX = cOUIIOverScroller != null ? cOUIIOverScroller.getCurrVelocityX() : 0.0f;
            COUIIOverScroller cOUIIOverScroller2 = this.mOverScroller;
            float currVelocityY = cOUIIOverScroller2 != null ? cOUIIOverScroller2.getCurrVelocityY() : 0.0f;
            boolean zW = passesAccidentalTouchFilter(this.mFlingVelocityX, this.mAbortVelocityX);
            boolean zW2 = passesAccidentalTouchFilter(this.mFlingVelocityY, this.mAbortVelocityY);
            this.mIsTouchDownWhileSlowScrolling = (Math.abs(currVelocityX) > 0.0f && Math.abs(currVelocityX) < ((float) this.mSlowScrollThreshold) && zW) || (Math.abs(currVelocityY) > 0.0f && Math.abs(currVelocityY) < ((float) this.mSlowScrollThreshold) && zW2);
            this.mIsTouchDownWhileOverScrolling = isOverScrolling();
            this.mTouchTime = System.currentTimeMillis();
            if (COUI_DEBUG) {
                this.mClickVelocityX = currVelocityX;
                this.mClickVelocityY = currVelocityY;
                this.mDebugAbortVelocityX = this.mAbortVelocityX;
                this.mDebugAbortVelocityY = this.mAbortVelocityY;
                Log.d(TAG, "onInterceptTouchEvent: ACTION_DOWN, isOverScrolling=:" + this.mIsTouchDownWhileOverScrolling + ", scrollVelocityX=:" + Math.abs(currVelocityX) + ", isFastFlingX=:" + zW + ", mFlingVelocityX=:" + this.mFlingVelocityX + ", mAbortVelocityX=:" + this.mAbortVelocityX + ", scrollVelocityY=:" + Math.abs(currVelocityY) + ", isFastFlingY=:" + zW2 + ", mFlingVelocityY=:" + this.mFlingVelocityY + ", mAbortVelocityY=:" + this.mAbortVelocityY);
            }
            this.mScrollPointerId = motionEvent.getPointerId(0);
            int x6 = (int) (motionEvent.getX() + 0.5f);
            this.mLastTouchX = x6;
            this.mInitialTouchX = x6;
            int y6 = (int) (motionEvent.getY() + 0.5f);
            this.mLastTouchY = y6;
            this.mInitialTouchY = y6;
            if (this.mScrollState == 2) {
                getParent().requestDisallowInterceptTouchEvent(true);
                setScrollState(1);
                stopNestedScroll(1);
            }
            int[] iArr = this.mNestedOffsets;
            iArr[1] = 0;
            iArr[0] = 0;
            int i2 = zCanScrollHorizontally ? 1 : 0;
            if (zCanScrollVertically) {
                i2 = (zCanScrollHorizontally ? 1 : 0) | 2;
            }
            startNestedScroll(i2, 0);
            this.mSmoothScrollFlag = false;
        } else if (actionMasked == 1) {
            this.mVelocityTracker.clear();
            stopNestedScroll(0);
        } else if (actionMasked == 2) {
            int iFindPointerIndex = motionEvent.findPointerIndex(this.mScrollPointerId);
            if (iFindPointerIndex < 0) {
                Log.e(TAG, "Error processing scroll; pointer index for id " + this.mScrollPointerId + " not found. Did any MotionEvents get skipped?");
                return false;
            }
            int x10 = (int) (motionEvent.getX(iFindPointerIndex) + 0.5f);
            int y10 = (int) (motionEvent.getY(iFindPointerIndex) + 0.5f);
            if (this.mScrollState != 1) {
                int i6 = x10 - this.mInitialTouchX;
                int i10 = y10 - this.mInitialTouchY;
                if (zCanScrollHorizontally && Math.abs(i6) > this.mTouchSlop && shouldAcceptDispatchTouchEvent(i10, i6)) {
                    this.mLastTouchX = x10;
                    z6 = true;
                } else {
                    z6 = false;
                }
                if (zCanScrollVertically && Math.abs(i10) > this.mTouchSlop && shouldAcceptDispatchTouchEvent(i6, i10)) {
                    this.mLastTouchY = y10;
                    z6 = true;
                }
                // Leapy modified 2026-07-22: Match OPPO COUIRecyclerView smali.
                // Enter dragging state only when this RecyclerView accepted movement on
                // one of its own scroll axes. The decompiled unconditional call stole
                // horizontal MOVE events from child controls such as the brightness SeekBar.
                if (z6) {
                    setScrollState(1);
                }
                // Leapy end
            }
        } else if (actionMasked == 3) {
            cancelTouch();
        } else if (actionMasked == 5) {
            this.mScrollPointerId = motionEvent.getPointerId(adjustmentPointerIndex);
            int x11 = (int) (motionEvent.getX(adjustmentPointerIndex) + 0.5f);
            this.mLastTouchX = x11;
            this.mInitialTouchX = x11;
            int y11 = (int) (motionEvent.getY(adjustmentPointerIndex) + 0.5f);
            this.mLastTouchY = y11;
            this.mInitialTouchY = y11;
            if (!this.mEnablePointerDown) {
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            }
        } else if (actionMasked == 6) {
            onPointerUp(motionEvent);
        }
        return this.mScrollState == 1;
    }

    public void cancelHorizontalItemAlign() {
        this.mLocateHelper.cancelHorizontalItemAlign();
    }

    @Override
    public void computeScroll() {
        SpringOverScroller springOverScroller;
        if (this.mIsOverScrollingReverseFling) {
            int scrollX = getScrollX();
            int scrollY = getScrollY();
            if (scrollX == 0 && scrollY == 0) {
                overScrollBy(-scrollX, -scrollY, scrollX, scrollY, 0, 0, 0, 0, false);
                onScrollChanged(getScrollX(), getScrollY(), scrollX, scrollY);
                this.mIsOverScrollingReverseFling = false;
                SpringOverScroller springOverScroller2 = this.mSpringOverScroller;
                int currVelocityX = springOverScroller2 != null ? (int) springOverScroller2.getCurrVelocityX() : 0;
                SpringOverScroller springOverScroller3 = this.mSpringOverScroller;
                int currVelocityY = springOverScroller3 != null ? (int) springOverScroller3.getCurrVelocityY() : 0;
                SpringOverScroller springOverScroller4 = this.mSpringOverScroller;
                if (springOverScroller4 != null) {
                    springOverScroller4.abortAnimation();
                }
                setScrollState(0);
                fling(currVelocityX, currVelocityY);
                return;
            }
        }
        if (this.mOverScrollEnable) {
            int i2 = this.mScrollType;
            if ((i2 == 2 || i2 == 3) && (springOverScroller = this.mSpringOverScroller) != null && springOverScroller.computeScrollOffset()) {
                int scrollX2 = getScrollX();
                int scrollY2 = getScrollY();
                int cOUICurrX = springOverScroller.getCOUICurrX();
                int cOUICurrY = springOverScroller.getCOUICurrY();
                if (scrollX2 != cOUICurrX || scrollY2 != cOUICurrY) {
                    int i6 = this.mOverflingDistance;
                    overScrollBy(cOUICurrX - scrollX2, cOUICurrY - scrollY2, scrollX2, scrollY2, 0, 0, i6, i6, false);
                    onScrollChanged(getScrollX(), getScrollY(), scrollX2, scrollY2);
                }
                if (springOverScroller.isCOUIFinished()) {
                    setScrollState(0);
                } else {
                    setScrollState(2);
                }
                if (awakenScrollBars()) {
                    return;
                }
                postInvalidateOnAnimation();
            }
        }
    }

    public boolean disallowInterceptWhenIsOverScrolling() {
        return true;
    }

    @Override
    public void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (COUI_DEBUG) {
            this.mDebugPaint.setTextSize(30.0f);
            this.mDebugPaint.setColor(-65536);
            canvas.drawText("isOverScrolling: " + isOverScrolling(), getWidth() / 2.0f, (getHeight() / 2.0f) - 50.0f, this.mDebugPaint);
            canvas.drawText("X: FlingVX: " + this.mFlingVelocityX + ", ClickVX: " + this.mClickVelocityX, getWidth() / 2.0f, getHeight() / 2.0f, this.mDebugPaint);
            canvas.drawText("Y: FlingVY: " + this.mFlingVelocityY + ", ClickVY: " + this.mClickVelocityY, getWidth() / 2.0f, (getHeight() / 2.0f) + 50.0f, this.mDebugPaint);
            canvas.drawText("AbortVX:" + this.mDebugAbortVelocityX + ", AbortVY:" + this.mDebugAbortVelocityY, getWidth() / 2.0f, (getHeight() / 2.0f) + 100.0f, this.mDebugPaint);
        }
        COUIScrollBar cOUIScrollBar = this.mCOUIScrollBar;
        if (cOUIScrollBar != null) {
            cOUIScrollBar.dispatchDrawOver(canvas);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        this.mCOUIRecyclerDividerManager.dispatchTouchEvent(motionEvent);
        if (motionEvent.getActionMasked() == 1 && isOverScrolling() && this.mScrollType == 3) {
            this.mScrollType = 2;
        }
        if (this.mEnableDispatchEventWhileScrolling || (this.mEnableDispatchEventWhileOverScrolling && isOverScrolling())) {
            float velocityAlongScrollableDirection = getVelocityAlongScrollableDirection();
            if (motionEvent.getActionMasked() == 0 && this.mDispatchEventVelocityThreshold >= Math.abs(velocityAlongScrollableDirection)) {
                COUIIOverScroller cOUIIOverScroller = this.mOverScroller;
                float f2 = 0.0f;
                this.mAbortVelocityX = (cOUIIOverScroller == null || cOUIIOverScroller.getCurrVelocityX() == 0.0f) ? 0.0f : this.mFlingVelocityX;
                COUIIOverScroller cOUIIOverScroller2 = this.mOverScroller;
                if (cOUIIOverScroller2 != null && cOUIIOverScroller2.getCurrVelocityY() != 0.0f) {
                    f2 = this.mFlingVelocityY;
                }
                this.mAbortVelocityY = f2;
                COUIIOverScroller cOUIIOverScroller3 = this.mOverScroller;
                if (cOUIIOverScroller3 != null) {
                    cOUIIOverScroller3.abortAnimation();
                }
                stopScroll();
            }
        }
        if (isOverScrolling() && (motionEvent.getActionMasked() == 1 || motionEvent.getActionMasked() == 3)) {
            springBackFromOverScroll();
            if (!isTapGesture(motionEvent)) {
                performEdgeHapticFeedback();
            }
            postInvalidateOnAnimation();
        }
        if (motionEvent.getActionMasked() != 5 || this.mEnablePointerDown) {
            return super.dispatchTouchEvent(motionEvent);
        }
        getParent().requestDisallowInterceptTouchEvent(true);
        return true;
    }

    public final void flingBackFromReverseOverScroll(float f2, float f10) {
        this.mIsOverScrollingReverseFling = true;
        SpringOverScroller springOverScroller = this.mSpringOverScroller;
        if (springOverScroller != null) {
            springOverScroller.fling(getScrollX(), getScrollY(), (int) f2, (int) f10);
        }
        dispatchIdleScrollStateIfNeeded();
    }

    public void enableFrameRate(boolean z6) {
        this.mSpringOverScroller.enableFrameRate(z6);
        this.mCOUILocateOverScroller.enableFrameRate(z6);
    }

    public final void springBackFromOverScroll() {
        SpringOverScroller springOverScroller = this.mSpringOverScroller;
        if (springOverScroller == null || !springOverScroller.springBack(getScrollX(), getScrollY(), 0, 0, 0, 0)) {
            return;
        }
        dispatchIdleScrollStateIfNeeded();
    }
    @Override
    public boolean fling(int i2, int i6) {
        RecyclerView.LayoutManager pVar = this.mLayout;
        if (pVar == null) {
            Log.e(TAG, "Cannot fling without a LayoutManager set. Call setLayoutManager with a non-null argument.");
            return false;
        }
        if (this.mLayoutSuppressed) {
            return false;
        }
        int iCanScrollHorizontally = pVar.canScrollHorizontally() ? 1 : 0;
        boolean zCanScrollVertically = this.mLayout.canScrollVertically();
        if (iCanScrollHorizontally == 0 || Math.abs(i2) < this.mMinFlingVelocity) {
            i2 = 0;
        }
        if (!zCanScrollVertically || Math.abs(i6) < this.mMinFlingVelocity) {
            i6 = 0;
        }
        if (i2 == 0 && i6 == 0) {
            return false;
        }
        float f2 = i2;
        float f10 = i6;
        if (!dispatchNestedPreFling(f2, f10)) {
            this.mScrollType = 1;
            boolean z6 = iCanScrollHorizontally != 0 || zCanScrollVertically;
            dispatchNestedFling(f2, f10, z6);
            RecyclerView.OnFlingListener sVar = this.mOnFlingListener;
            if (sVar != null && sVar.onFling(i2, i6)) {
                return true;
            }
            if (z6) {
                if (zCanScrollVertically) {
                    iCanScrollHorizontally |= 2;
                }
                startNestedScroll(iCanScrollHorizontally, 1);
                int i10 = this.mMaxFlingVelocity;
                int iMax = Math.max(-i10, Math.min(i2, i10));
                int i11 = this.mMaxFlingVelocity;
                this.mViewFlinger.fling(iMax, Math.max(-i11, Math.min(i6, i11)));
                return true;
            }
        }
        return false;
    }

    @Override
    public COUIScrollBar getCOUIScrollDelegate() {
        return this.mCOUIScrollBar;
    }

    @Override
    public View getCOUIScrollableView() {
        return this;
    }

    public int getHorizontalItemAlign() {
        return this.mLocateHelper.getHorizontalItemAlign();
    }

    public boolean getIsUseNativeOverScroll() {
        return this.mIsUseNativeOverScroll;
    }

    public COUIFlingLocateHelper getLocateHelper() {
        return this.mLocateHelper;
    }

    @Override
    public int getMaxFlingVelocity() {
        return this.mMaxFlingVelocity;
    }

    @Override
    public int getMinFlingVelocity() {
        return this.mMinFlingVelocity;
    }

    public COUILocateOverScroller getNativeOverScroller() {
        return this.mCOUILocateOverScroller;
    }

    @Override
    public RecyclerView.OnFlingListener getOnFlingListener() {
        return this.mOnFlingListener;
    }

    @Override
    public int getScrollState() {
        return this.mScrollState;
    }

    public ViewFlinger getViewFlinger() {
        return this.mViewFlinger;
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

    public final boolean dispatchToInterceptingOnItemTouchListener(MotionEvent motionEvent) {
        RecyclerView.OnItemTouchListener tVar = this.mInterceptingOnItemTouchListener;
        if (tVar == null) {
            if (motionEvent.getAction() == 0) {
                return false;
            }
            return findInterceptingOnItemTouchListener(motionEvent);
        }
        tVar.onTouchEvent(this, motionEvent);
        int action = motionEvent.getAction();
        if (action == 3 || action == 1) {
            this.mInterceptingOnItemTouchListener = null;
        }
        return true;
    }

    public final boolean findInterceptingOnItemTouchListener(MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        int size = this.mOnItemTouchListeners.size();
        for (int i2 = 0; i2 < size; i2++) {
            RecyclerView.OnItemTouchListener tVar = this.mOnItemTouchListeners.get(i2);
            if (tVar.onInterceptTouchEvent(this, motionEvent) && action != MotionEvent.ACTION_CANCEL) {
                this.mInterceptingOnItemTouchListener = tVar;
                return true;
            }
        }
        return false;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        resetOverScrollState();
        COUIScrollBar cOUIScrollBar = this.mCOUIScrollBar;
        if (cOUIScrollBar != null) {
            cOUIScrollBar.onAttachedToWindow();
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        SpringOverScroller springOverScroller = this.mSpringOverScroller;
        if (springOverScroller != null) {
            springOverScroller.cancelCallback();
        }
        COUIScrollBar cOUIScrollBar = this.mCOUIScrollBar;
        if (cOUIScrollBar != null) {
            cOUIScrollBar.release();
            this.mCOUIScrollBar = null;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        boolean zC0 = interceptTouchEventInternal(motionEvent);
        if (zC0) {
            this.mCOUIRecyclerDividerManager.onInterceptTouchEvent(motionEvent);
        }
        return zC0;
    }

    @Override
    public void onOverScrolled(int i2, int i6, boolean z6, boolean z10) {
        if (getScrollY() == i6 && getScrollX() == i2) {
            return;
        }
        if (COUI_DEBUG) {
            Log.d(TAG, "onOverScrolled: scrollX: " + i2 + " scrollY: " + i6);
        }
        if (this.mScrollType == 3) {
            i2 = (int) (COUIPhysicalAnimationUtil.calcOverFlingDecelerateDist(0, i2, this.mScreenWidth) * this.mFlingRatio);
            i6 = (int) (COUIPhysicalAnimationUtil.calcOverFlingDecelerateDist(0, i6, this.mScreenHeight) * this.mFlingRatio);
        }
        onScrollChanged(i2, i6, getScrollX(), getScrollY());
        ViewNative.setScrollX(this, i2);
        ViewNative.setScrollY(this, i6);
        invalidateParentIfNeeded();
        awakenScrollBars();
    }

    @Override
    public void onSizeChanged(int i2, int i6, int i10, int i11) {
        super.onSizeChanged(i2, i6, i10, i11);
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        this.mScreenWidth = displayMetrics.widthPixels;
        this.mScreenHeight = displayMetrics.heightPixels;
        // Leapy modified 2026-07-22: OPPO always re-runs the locate helper here;
        // the gradual-stop branch belongs to a different COUI version.
        if (this.mLocateHelper != null) {
            post(new Runnable() {
                @Override
                public final void run() {
                    COUIRecyclerView.this.snapLocateTarget();
                }
            });
        }
    }
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        COUIScrollBar scrollBar = this.mCOUIScrollBar;
        if (scrollBar != null && scrollBar.onTouchEvent(motionEvent)) {
            return true;
        }
        if (this.mLayoutSuppressed || this.mIgnoreMotionEventTillDown) {
            return false;
        }
        if (dispatchToInterceptingOnItemTouchListener(motionEvent)) {
            resetOverScrollState();
            return true;
        }
        RecyclerView.LayoutManager layoutManager = this.mLayout;
        if (layoutManager == null) {
            return false;
        }
        boolean canScrollHorizontally = layoutManager.canScrollHorizontally();
        boolean canScrollVertically = this.mLayout.canScrollVertically();
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
        boolean eventAddedToVelocityTracker = false;
        MotionEvent vtev = MotionEvent.obtain(motionEvent);
        int action = motionEvent.getActionMasked();
        int actionIndex = UIUtil.getAdjustmentPointerIndex(motionEvent, motionEvent.getActionIndex());
        if (action == MotionEvent.ACTION_DOWN) {
            int[] offsets = this.mNestedOffsets;
            offsets[1] = 0;
            offsets[0] = 0;
        }
        vtev.offsetLocation(this.mNestedOffsets[0], this.mNestedOffsets[1]);
        if (this.mOverScrollEnable) {
            this.mVelocityTracker.addMovement(vtev);
        }
        if (action == MotionEvent.ACTION_DOWN) {
            this.mScrollPointerId = motionEvent.getPointerId(0);
            int x = (int) (motionEvent.getX() + 0.5f);
            this.mLastTouchX = x;
            this.mInitialTouchX = x;
            int y = (int) (motionEvent.getY() + 0.5f);
            this.mLastTouchY = y;
            this.mInitialTouchY = y;
            if (this.mOverScrollEnable) {
                COUIIOverScroller overScroller = this.mOverScroller;
                boolean overScrollerRunning = overScroller != null && !overScroller.isCOUIFinished();
                SpringOverScroller springOverScroller = this.mSpringOverScroller;
                boolean springRunning = springOverScroller != null && !springOverScroller.isCOUIFinished();
                if (overScrollerRunning || springRunning) {
                    COUIIOverScroller current = this.mOverScroller;
                    this.mAbortVelocityX = (current == null || current.getCurrVelocityX() == 0.0f)
                            ? 0.0f : this.mFlingVelocityX;
                    COUIIOverScroller current2 = this.mOverScroller;
                    this.mAbortVelocityY = (current2 == null || current2.getCurrVelocityY() == 0.0f)
                            ? 0.0f : this.mFlingVelocityY;
                    COUIIOverScroller current3 = this.mOverScroller;
                    if (current3 != null) {
                        current3.abortAnimation();
                    }
                    SpringOverScroller spring = this.mSpringOverScroller;
                    if (spring != null) {
                        spring.abortAnimation();
                    }
                }
            }
            int scrollAxes = canScrollHorizontally ? 1 : 0;
            if (canScrollVertically) {
                scrollAxes |= 2;
            }
            startNestedScroll(scrollAxes, 0);
        } else if (action == MotionEvent.ACTION_UP) {
            if (!this.mOverScrollEnable) {
                this.mVelocityTracker.addMovement(vtev);
                eventAddedToVelocityTracker = true;
            }
            this.mVelocityTracker.computeCurrentVelocity(1000, this.mMaxFlingVelocity);
            float xVelocity = canScrollHorizontally
                    ? -this.mVelocityTracker.getXVelocity(this.mScrollPointerId) : 0.0f;
            float yVelocity = canScrollVertically
                    ? -this.mVelocityTracker.getYVelocity(this.mScrollPointerId) : 0.0f;
            boolean isOverScrolling = isOverScrolling();
            boolean clickableWhileSlowScrolling = this.mItemClickableWhileSlowScrolling
                    && this.mIsTouchDownWhileSlowScrolling;
            boolean clickableWhileOverScrolling = this.mItemClickableWhileOverScrolling
                    && this.mIsTouchDownWhileOverScrolling && isOverScrolling;
            if (clickableWhileSlowScrolling || clickableWhileOverScrolling) {
                dispatchClickToTouchedChild(motionEvent);
            }
            if (isOverScrolling) {
                boolean reverseFling = false;
                if (this.mOverScroller != null && Math.abs(xVelocity) > 6000.0f) {
                    this.mOverScroller.setCurrVelocityX(xVelocity);
                    if (getScrollX() * xVelocity < 0.0f) {
                        reverseFling = true;
                    }
                }
                if (this.mOverScroller != null && Math.abs(yVelocity) > 6000.0f) {
                    this.mOverScroller.setCurrVelocityY(yVelocity);
                    if (getScrollY() * yVelocity < 0.0f) {
                        reverseFling = true;
                    }
                }
                if (reverseFling) {
                    flingBackFromReverseOverScroll(xVelocity, yVelocity);
                } else {
                    springBackFromOverScroll();
                }
                postInvalidateOnAnimation();
            } else {
                if ((xVelocity == 0.0f && yVelocity == 0.0f)
                        || !fling((int) xVelocity, (int) yVelocity)) {
                    // Leapy modified 2026-07-22: Exact OPPO ACTION_UP smali path.
                    setScrollState(0);
                }
            }
            clearVelocityTrackerAndStopNestedScroll();
        } else if (action == MotionEvent.ACTION_MOVE) {
            COUIIOverScroller overScroller = this.mOverScroller;
            if (overScroller instanceof SpringOverScroller && this.mEnableOptimizedScroll) {
                ((SpringOverScroller) overScroller).triggerCallback();
            }
            int pointerIndex = motionEvent.findPointerIndex(this.mScrollPointerId);
            if (pointerIndex < 0) {
                Log.e(TAG, "Error processing scroll; pointer index for id " + this.mScrollPointerId
                        + " not found. Did any MotionEvents get skipped?");
                vtev.recycle();
                return false;
            }
            int x = (int) (motionEvent.getX(pointerIndex) + 0.5f);
            int y = (int) (motionEvent.getY(pointerIndex) + 0.5f);
            int dx = this.mLastTouchX - x;
            int dy = this.mLastTouchY - y;
            int[] consumed = this.mReusableIntPair;
            consumed[0] = 0;
            consumed[1] = 0;
            if (dispatchNestedPreScroll(dx, dy, consumed, this.mScrollOffset, 0)) {
                dx -= this.mReusableIntPair[0];
                dy -= this.mReusableIntPair[1];
                vtev.offsetLocation(this.mScrollOffset[0], this.mScrollOffset[1]);
                int[] offsets = this.mNestedOffsets;
                offsets[0] = offsets[0] + this.mScrollOffset[0];
                offsets[1] = offsets[1] + this.mScrollOffset[1];
                getParent().requestDisallowInterceptTouchEvent(true);
            }
            if (this.mScrollState != 1) {
                boolean startScroll = false;
                if (canScrollHorizontally && Math.abs(dx) > this.mTouchSlop) {
                    dx = dx > 0 ? dx - this.mTouchSlop : dx + this.mTouchSlop;
                    startScroll = true;
                }
                if (canScrollVertically && Math.abs(dy) > this.mTouchSlop) {
                    dy = dy > 0 ? dy - this.mTouchSlop : dy + this.mTouchSlop;
                    startScroll = true;
                }
                if (startScroll) {
                    setScrollState(1);
                }
            }
            if (this.mScrollState == 1) {
                this.mLastTouchX = x - this.mScrollOffset[0];
                this.mLastTouchY = y - this.mScrollOffset[1];
                if (this.mOverScrollEnable) {
                    this.mScrollType = 0;
                }
                boolean scrolled = scrollByInternal(canScrollHorizontally ? dx : 0,
                        canScrollVertically ? dy : 0, vtev);
                if (scrolled || (hasOverScrollOffset() && disallowInterceptWhenIsOverScrolling())) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                GapWorker gapWorker = this.mGapWorker;
                if (gapWorker != null && (dx != 0 || dy != 0)) {
                    gapWorker.postFromTraversal(this, dx, dy);
                }
            }
        } else if (action == MotionEvent.ACTION_CANCEL) {
            SpringOverScroller springOverScroller = this.mSpringOverScroller;
            if (springOverScroller != null) {
                springOverScroller.cancelCallback();
            }
            cancelTouch();
        } else if (action == MotionEvent.ACTION_POINTER_DOWN) {
            this.mScrollPointerId = motionEvent.getPointerId(actionIndex);
            int x = (int) (motionEvent.getX(actionIndex) + 0.5f);
            this.mLastTouchX = x;
            this.mInitialTouchX = x;
            int y = (int) (motionEvent.getY(actionIndex) + 0.5f);
            this.mLastTouchY = y;
            this.mInitialTouchY = y;
        } else if (action == MotionEvent.ACTION_POINTER_UP) {
            onPointerUp(motionEvent);
        }
        if (!this.mOverScrollEnable && !eventAddedToVelocityTracker) {
            this.mVelocityTracker.addMovement(vtev);
        }
        vtev.recycle();
        return true;
    }

    @Override
    public void onVisibilityChanged(View view, int i2) {
        super.onVisibilityChanged(view, i2);
        if (i2 != 0) {
            resetOverScrollState();
            SpringOverScroller springOverScroller = this.mSpringOverScroller;
            if (springOverScroller != null) {
                springOverScroller.abortAnimation();
            }
        }
        COUIScrollBar cOUIScrollBar = this.mCOUIScrollBar;
        if (cOUIScrollBar != null) {
            cOUIScrollBar.onVisibilityChanged(view, i2);
        }
    }

    @Override
    public void onWindowVisibilityChanged(int i2) {
        super.onWindowVisibilityChanged(i2);
        COUIScrollBar cOUIScrollBar = this.mCOUIScrollBar;
        if (cOUIScrollBar != null) {
            cOUIScrollBar.onWindowVisibilityChanged(i2);
        }
    }

    @Override
    public boolean overScrollBy(int i2, int i6, int i10, int i11, int i12, int i13, int i14, int i15, boolean z6) {
        int i16 = i2 + i10;
        int i17 = i6 + i11;
        if ((i10 < 0 && i16 > 0) || (i10 > 0 && i16 < 0)) {
            i16 = 0;
        }
        if ((i11 < 0 && i17 > 0) || (i11 > 0 && i17 < 0)) {
            i17 = 0;
        }
        onOverScrolled(i16, i17, false, false);
        return false;
    }

    public void refresh() {
        TypedArray typedArrayObtainStyledAttributes = null;
        String resourceTypeName = this.mStyle == 0 ? null : getResources().getResourceTypeName(this.mStyle);
        if (!TextUtils.isEmpty(resourceTypeName) && "style".equals(resourceTypeName)) {
            typedArrayObtainStyledAttributes = getContext().obtainStyledAttributes(null, R.styleable.COUIRecyclerView, 0, this.mStyle);
        }
        if (typedArrayObtainStyledAttributes != null) {
            this.mScrollbarThumbVertical = typedArrayObtainStyledAttributes.getDrawable(R.styleable.COUIRecyclerView_couiScrollbarThumbVertical);
            typedArrayObtainStyledAttributes.recycle();
        }
        if (this.mScrollbars == 512) {
            Drawable drawable = this.mScrollbarThumbVertical;
            if (drawable != null) {
                this.mCOUIScrollBar.setThumbDrawable(drawable);
            } else {
                this.mCOUIScrollBar.refreshScrollBarColor();
            }
        }
        invalidate();
    }

    @Override
    public void removeOnItemTouchListener(RecyclerView.OnItemTouchListener tVar) {
        this.mOnItemTouchListeners.remove(tVar);
        if (this.mInterceptingOnItemTouchListener == tVar) {
            this.mInterceptingOnItemTouchListener = null;
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean z6) {
        int size = this.mOnItemTouchListeners.size();
        for (int i2 = 0; i2 < size; i2++) {
            this.mOnItemTouchListeners.get(i2).onRequestDisallowInterceptTouchEvent(z6);
        }
        super.requestDisallowInterceptTouchEvent(z6);
    }

    @Override
    public void scrollBy(int i2, int i6) {
        RecyclerView.LayoutManager pVar = this.mLayout;
        if (pVar == null) {
            Log.e(TAG, "Cannot scroll without a LayoutManager set. Call setLayoutManager with a non-null argument.");
            return;
        }
        if (this.mLayoutSuppressed) {
            return;
        }
        boolean zCanScrollHorizontally = pVar.canScrollHorizontally();
        boolean zCanScrollVertically = this.mLayout.canScrollVertically();
        if (zCanScrollHorizontally || zCanScrollVertically) {
            if (!zCanScrollHorizontally) {
                i2 = 0;
            }
            if (!zCanScrollVertically) {
                i6 = 0;
            }
            scrollByInternal(i2, i6, null);
        }
    }

    public boolean scrollByInternal(int i2, int i6, MotionEvent motionEvent) {
        int i10;
        int i11;
        int i12;
        int i13;
        int i14;
        int i15;
        int i16;
        int i17;
        consumePendingUpdateOperations();
        if (this.mAdapter == null || (i2 == 0 && i6 == 0)) {
            i10 = 0;
            i11 = 0;
            i12 = 0;
            i13 = 0;
        } else {
            if (!this.mOverScrollEnable || ((getScrollY() >= 0 || i6 <= 0) && ((getScrollY() <= 0 || i6 >= 0) && ((getScrollX() >= 0 || i2 <= 0) && (getScrollX() <= 0 || i2 >= 0))))) {
                int[] iArr = this.mReusableIntPair;
                iArr[0] = 0;
                iArr[1] = 0;
                scrollStep(i2, i6, iArr);
                int[] iArr2 = this.mReusableIntPair;
                i14 = iArr2[0];
                i15 = iArr2[1];
                i16 = i2 - i14;
                i17 = i6 - i15;
            } else {
                i15 = 0;
                i14 = 0;
                i16 = 0;
                i17 = 0;
            }
            if (COUI_DEBUG) {
                Log.d(TAG, "scrollByInternal: y: " + i6 + " consumedY: " + i15 + " unconsumedY: " + i17);
            }
            i10 = i15;
            i11 = i14;
            i12 = i16;
            i13 = i17;
        }
        if (!this.mItemDecorations.isEmpty()) {
            invalidate();
        }
        int[] iArr3 = this.mReusableIntPair;
        iArr3[0] = 0;
        iArr3[1] = 0;
        dispatchNestedScroll(i11, i10, i12, i13, this.mScrollOffset, 0, iArr3);
        int[] iArr4 = this.mReusableIntPair;
        int i18 = i12 - iArr4[0];
        int i19 = i13 - iArr4[1];
        int i20 = this.mLastTouchX;
        int[] iArr5 = this.mScrollOffset;
        int i21 = iArr5[0];
        this.mLastTouchX = i20 - i21;
        int i22 = this.mLastTouchY;
        int i23 = iArr5[1];
        this.mLastTouchY = i22 - i23;
        if (motionEvent != null) {
            motionEvent.offsetLocation(i21, i23);
        }
        int[] iArr6 = this.mNestedOffsets;
        int i24 = iArr6[0];
        int[] iArr7 = this.mScrollOffset;
        iArr6[0] = i24 + iArr7[0];
        iArr6[1] = iArr6[1] + iArr7[1];
        if (getOverScrollMode() != 2 && motionEvent != null && this.mOverScrollEnable && (MotionEventCompat.isFromSource(motionEvent, 4098) || MotionEventCompat.isFromSource(motionEvent, 8194))) {
            if (i19 != 0 || i18 != 0) {
                this.mScrollType = 2;
            }
            if (Math.abs(i19) == 0 && Math.abs(i10) < 2 && Math.abs(i6) < 2 && Math.abs(getScrollY()) > 2) {
                this.mScrollType = 2;
            }
            if (i19 == 0 && i10 == 0 && Math.abs(i6) > 2) {
                this.mScrollType = 2;
            }
            if (Math.abs(i18) == 0 && Math.abs(i11) < 2 && Math.abs(i2) < 2 && Math.abs(getScrollX()) > 2) {
                this.mScrollType = 2;
            }
            if (i18 == 0 && i11 == 0 && Math.abs(i2) > 2) {
                this.mScrollType = 2;
            }
            if (this.mFixScrollTypeForOverScrolling && (getScrollX() != 0 || getScrollY() != 0)) {
                this.mScrollType = 2;
            }
            int scrollX = getScrollX();
            int scrollY = getScrollY();
            int iCalcRealOverScrollDist = (int) (COUIPhysicalAnimationUtil.calcRealOverScrollDist(i19, scrollY, this.mOverscrollDistance) * this.mFlingRatio);
            int iCalcRealOverScrollDist2 = (int) (COUIPhysicalAnimationUtil.calcRealOverScrollDist(i18, scrollX, this.mOverscrollDistance) * this.mFlingRatio);
            if ((scrollY < 0 && i6 > 0) || (scrollY > 0 && i6 < 0)) {
                iCalcRealOverScrollDist = (int) (COUIPhysicalAnimationUtil.calcRealOverScrollDist(i6, scrollX, this.mOverscrollDistance) * this.mFlingRatio);
            }
            int i25 = iCalcRealOverScrollDist;
            if ((scrollX < 0 && i2 > 0) || (scrollX > 0 && i2 < 0)) {
                iCalcRealOverScrollDist2 = (int) (COUIPhysicalAnimationUtil.calcRealOverScrollDist(i2, scrollX, this.mOverscrollDistance) * this.mFlingRatio);
            }
            if (i25 != 0 || iCalcRealOverScrollDist2 != 0) {
                int i26 = this.mOverscrollDistance;
                overScrollBy(iCalcRealOverScrollDist2, i25, scrollX, scrollY, 0, 0, i26, i26, true);
            }
        }
        if (i11 != 0 || i10 != 0) {
            dispatchOnScrolled(i11, i10);
        }
        if (!awakenScrollBars()) {
            invalidate();
        }
        return (i11 == 0 && i10 == 0) ? false : true;
    }

    @Override
    public void scrollToPosition(int i2) {
        resetOverScrollState();
        super.scrollToPosition(i2);
    }

    @Override
    public void setAdapter(RecyclerView.Adapter hVar) {
        super.setAdapter(hVar);
    }

    public void setAvoidAccidentalTouch(boolean z6) {
        this.mAvoidAccidentalTouch = z6;
    }

    public void setCustomTouchSlop(int i2) {
        Log.w(TAG, "setTouchSlop: set touchSlop from " + this.mTouchSlop + " to " + i2);
        this.mTouchSlop = i2;
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

    public void setEnablePointerDownAction(boolean z6) {
        this.mEnablePointerDown = z6;
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

    public void setFlingRatio(float f2) {
        this.mFlingRatio = f2;
    }

    public void setHorizontalFlingDurationRatio(float f2) {
        this.mCOUILocateOverScroller.setDurationRatio(f2);
    }

    public void setHorizontalFlingFriction(float f2) {
        COUILocateOverScroller cOUILocateOverScroller = this.mCOUILocateOverScroller;
        if (cOUILocateOverScroller != null) {
            cOUILocateOverScroller.setFlingFriction(f2);
        }
    }

    public void setHorizontalFlingVelocityRatio(float f2) {
        this.mCOUILocateOverScroller.setVelocityXRatio(f2);
        this.mCOUILocateOverScroller.setVelocityYRatio(f2);
    }

    public void setHorizontalItemAlign(int i2) {
        if (isHorizontalLinearLayout()) {
            setIsUseNativeOverScroll(true);
            this.mLocateHelper.setHorizontalItemAlign(i2);
        }
    }

    public void setIsUseNativeOverScroll(boolean z6) {
        this.mIsUseNativeOverScroll = z6;
        if (z6) {
            this.mOverScroller = this.mCOUILocateOverScroller;
        } else {
            this.mOverScroller = this.mSpringOverScroller;
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
    public void setLayoutManager(RecyclerView.LayoutManager pVar) {
        super.setLayoutManager(pVar);
        if (pVar == null || this.mSpringOverScroller == null) {
            return;
        }
        if (pVar.canScrollHorizontally()) {
            this.mSpringOverScroller.setSpringBackTensionMultiple(HORIZONTAL_SPRING_BACK_TENSION_MULTIPLE);
        } else {
            this.mSpringOverScroller.setSpringBackTensionMultiple(this.mVerticalSpringOverTension);
        }
    }

    public void setNativeOverScroller(COUILocateOverScroller cOUILocateOverScroller) {
        this.mCOUILocateOverScroller = cOUILocateOverScroller;
        if (this.mIsUseNativeOverScroll) {
            this.mOverScroller = cOUILocateOverScroller;
        }
    }

    @Override
    public void setNewCOUIScrollDelegate(COUIScrollBar cOUIScrollBar) {
        if (cOUIScrollBar == null) {
            throw new IllegalArgumentException("setNewCOUIScrollDelegate must NOT be NULL.");
        }
        this.mCOUIScrollBar = cOUIScrollBar;
        cOUIScrollBar.onAttachedToWindow();
    }

    @Override
    public void setOnFlingListener(RecyclerView.OnFlingListener sVar) {
        this.mOnFlingListener = sVar;
    }

    public void setOverScrollEnable(boolean z6) {
        this.mOverScrollEnable = z6;
    }

    public void setOverScrollingFixed(boolean z6) {
        this.mFixScrollTypeForOverScrolling = z6;
    }

    public void setPressHideDivider(boolean z6) {
        this.mCOUIRecyclerDividerManager.setEnablePressHideDivider(z6);
    }

    @Override
    public void setScrollState(int i2) {
        if (i2 == this.mScrollState) {
            return;
        }
        this.mScrollState = i2;
        if (i2 != 2) {
            stopScrollersAndSmoothScroller();
        }
        super.setScrollState(i2);
    }

    @Override
    public void setScrollingTouchSlop(int i2) {
        ViewConfiguration viewConfiguration = ViewConfiguration.get(getContext());
        if (i2 != 0) {
            if (i2 == 1) {
                this.mTouchSlop = viewConfiguration.getScaledPagingTouchSlop();
                return;
            }
            Log.w(TAG, "setScrollingTouchSlop(): bad argument constant " + i2 + "; using default value");
        }
        this.mTouchSlop = viewConfiguration.getScaledTouchSlop();
    }

    public void setSlowScrollThreshold(int i2) {
        Log.d(TAG, "Slow scroll threshold set to " + i2);
        this.mSlowScrollThreshold = i2;
    }

    public void setSpringBackFriction(float f2) {
        SpringOverScroller springOverScroller = this.mSpringOverScroller;
        if (springOverScroller != null) {
            springOverScroller.setSpringBackFriction(f2);
        }
    }

    public void setSpringBackTension(float f2) {
        this.mVerticalSpringOverTension = f2;
        SpringOverScroller springOverScroller = this.mSpringOverScroller;
        if (springOverScroller != null) {
            springOverScroller.setSpringBackTensionMultiple(f2);
        }
    }

    public void setSpringOverScrollerDebug(boolean z6) {
        SpringOverScroller springOverScroller = this.mSpringOverScroller;
        if (springOverScroller != null) {
            springOverScroller.setDebug(z6);
        }
    }

    @Override
    public void smoothScrollBy(int i2, int i6) {
        smoothScrollBy(i2, i6, null);
    }

    @Override
    public void smoothScrollToPosition(int i2) {
        resetOverScrollState();
        super.smoothScrollToPosition(i2);
    }

    @Override
    public void stopScroll() {
        super.stopScroll();
        setScrollState(0);
        stopScrollersAndSmoothScroller();
    }

    @Override
    public int superComputeVerticalScrollExtent() {
        return super.computeVerticalScrollExtent();
    }

    @Override
    public int superComputeVerticalScrollOffset() {
        return super.computeVerticalScrollOffset();
    }

    @Override
    public int superComputeVerticalScrollRange() {
        return super.computeVerticalScrollRange();
    }

    @Override
    public void superOnTouchEvent(MotionEvent motionEvent) {
        super.onTouchEvent(motionEvent);
    }

    public final void onPointerUp(MotionEvent motionEvent) {
        int adjustmentPointerIndex = UIUtil.getAdjustmentPointerIndex(motionEvent, motionEvent.getActionIndex());
        if (motionEvent.getPointerId(adjustmentPointerIndex) == this.mScrollPointerId) {
            int i2 = adjustmentPointerIndex == 0 ? 1 : 0;
            this.mScrollPointerId = motionEvent.getPointerId(i2);
            int x6 = (int) (motionEvent.getX(i2) + 0.5f);
            this.mLastTouchX = x6;
            this.mInitialTouchX = x6;
            int y6 = (int) (motionEvent.getY(i2) + 0.5f);
            this.mLastTouchY = y6;
            this.mInitialTouchY = y6;
        }
    }

    public COUIRecyclerView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    @Override
    public void smoothScrollBy(int i2, int i6, Interpolator interpolator) {
        smoothScrollBy(i2, i6, interpolator, Integer.MIN_VALUE);
    }

    public COUIRecyclerView(Context context, AttributeSet attributeSet, int i2) {
        super(context, attributeSet, i2);
        this.SCROLLBARS_NONE = 0;
        this.SCROLLBARS_VERTICAL = 512;
        this.mFixScrollTypeForOverScrolling = true;
        this.mOverScrollEnable = true;
        this.SCROLLING = 0;
        this.FLING = 1;
        this.OVER_SCROLLING = 2;
        this.OVER_FLING = 3;
        this.mIsOverScrollingReverseFling = false;
        this.mScreenHeight = 0;
        this.mScreenWidth = 0;
        this.mItemClickableWhileSlowScrolling = true;
        this.mItemClickableWhileOverScrolling = true;
        this.mFastFlingVelocity = 1000.0f;
        this.mAvoidAccidentalTouch = true;
        this.mDebugPaint = new Paint();
        this.mEnableFlingSpeedIncrease = true;
        this.mEnableOptimizedScroll = true;
        this.mSmoothScrollFlag = false;
        this.mEnableDispatchEventWhileScrolling = false;
        this.mEnableDispatchEventWhileOverScrolling = false;
        this.mDispatchEventVelocityThreshold = 2500;
        this.mEventFilterAngle = DEFAULT_INTERACTING_NESTED_SCROLL_ANGLE;
        this.mScrollbars = 0;
        this.mSlowScrollThreshold = 2500;
        this.mScrollState = 0;
        this.mScrollPointerId = -1;
        this.mScrollOffset = new int[2];
        this.mNestedOffsets = new int[2];
        this.mVerticalSpringOverTension = VERTICAL_SPRING_BACK_TENSION_MULTIPLE;
        this.mEnablePointerDown = true;
        this.mFlingRatio = 1.0f;
        this.mEnableVibrator = true;
        readCOUIRecyclerViewAttributes(context, attributeSet, i2);
        ensureViewFlinger();
        ensureOnItemTouchListeners();
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        this.mTouchSlop = viewConfiguration.getScaledTouchSlop();
        this.mMinFlingVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
        this.mMaxFlingVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
        setSlowScrollThreshold(2500);
        initOverScrollDistances(context);
        if (COUI_DEBUG) {
            Log.d(TAG, "COUIRecyclerView: overscroll_mode: " + getOverScrollMode() + " mOverScrollEnable: " + this.mOverScrollEnable);
        }
        ensureOverScrollers(context);
        COUIFlingLocateHelper cOUIFlingLocateHelper = new COUIFlingLocateHelper();
        this.mLocateHelper = cOUIFlingLocateHelper;
        cOUIFlingLocateHelper.attachToRecyclerView(this);
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        this.mScreenWidth = displayMetrics.widthPixels;
        this.mScreenHeight = displayMetrics.heightPixels;
        this.mCOUIRecyclerDividerManager = new COUIRecyclerDividerManager(this, this.mTouchSlop);
        if (this.mScrollbars == 512) {
            initCOUIScrollBar(context);
            int i6 = this.mScrollbarsSize;
            if (i6 != 0) {
                this.mCOUIScrollBar.setThumbSize(i6);
            }
            Drawable drawable = this.mScrollbarThumbVertical;
            if (drawable != null) {
                this.mCOUIScrollBar.setThumbDrawable(drawable);
            }
        }
    }

    @Override
    public void smoothScrollBy(int i2, int i6, Interpolator interpolator, int i10) {
        smoothScrollBy(i2, i6, interpolator, i10, false);
    }

    public static class COUIRecyclerViewItemDecoration extends COUIDividerItemDecoration {
        public final int[] mItemLocation;
        public final int[] mAlignedViewLocation;

        public COUIRecyclerViewItemDecoration(Context context) {
            super(context);
            this.mItemLocation = new int[2];
            this.mAlignedViewLocation = new int[2];
        }

        @Override
        public int getDividerInsetEnd(RecyclerView recyclerView, int i2) {
            View childAt = recyclerView.getChildAt(i2);
            return childAt != null ? getDividerInsetEnd(recyclerView.getChildViewHolder(childAt)) : super.getDividerInsetEnd(recyclerView, i2);
        }

        @Override
        public int getDividerInsetStart(RecyclerView recyclerView, int i2) {
            View childAt = recyclerView.getChildAt(i2);
            return childAt != null ? getDividerInsetStart(recyclerView.getChildViewHolder(childAt)) : super.getDividerInsetStart(recyclerView, i2);
        }

        @Override
        public boolean shouldDrawDivider(RecyclerView recyclerView, int i2) {
            View childAt = recyclerView.getChildAt(i2);
            if (childAt == null) {
                return true;
            }
            if ((recyclerView instanceof COUIRecyclerView) && !((COUIRecyclerView) recyclerView).isDrawDivider(childAt, i2)) {
                return false;
            }
            Object childViewHolder = recyclerView.getChildViewHolder(childAt);
            if (childViewHolder instanceof ICOUIDividerDecorationInterface) {
                return ((ICOUIDividerDecorationInterface) childViewHolder).drawDivider();
            }
            return true;
        }
        @Override
        public int getDividerInsetEnd(RecyclerView.ViewHolder viewHolder) {
            int width;
            int width2;
            if (viewHolder instanceof ICOUIDividerDecorationInterface) {
                View view = viewHolder.itemView;
                boolean z6 = view.getLayoutDirection() == 1;
                ICOUIDividerDecorationInterface dividerProvider = (ICOUIDividerDecorationInterface) viewHolder;
                View dividerEndAlignView = dividerProvider.getDividerEndAlignView();
                if (dividerEndAlignView != null) {
                    view.getLocationInWindow(this.mItemLocation);
                    dividerEndAlignView.getLocationInWindow(this.mAlignedViewLocation);
                    if (z6) {
                        width = this.mAlignedViewLocation[0] + dividerEndAlignView.getPaddingEnd();
                        width2 = this.mItemLocation[0];
                    } else {
                        width = this.mItemLocation[0] + view.getWidth();
                        width2 = (this.mAlignedViewLocation[0] + dividerEndAlignView.getWidth()) - dividerEndAlignView.getPaddingEnd();
                    }
                    return width - width2;
                }
                return dividerProvider.getDividerEndInset();
            }
            return super.getDividerInsetEnd(viewHolder);
        }
        @Override
        public int getDividerInsetStart(RecyclerView.ViewHolder viewHolder) {
            int paddingStart;
            int width;
            if (viewHolder instanceof ICOUIDividerDecorationInterface) {
                View view = viewHolder.itemView;
                boolean z6 = view.getLayoutDirection() == 1;
                ICOUIDividerDecorationInterface dividerProvider = (ICOUIDividerDecorationInterface) viewHolder;
                View dividerStartAlignView = dividerProvider.getDividerStartAlignView();
                if (dividerStartAlignView != null) {
                    view.getLocationInWindow(this.mItemLocation);
                    dividerStartAlignView.getLocationInWindow(this.mAlignedViewLocation);
                    if (z6) {
                        paddingStart = this.mItemLocation[0] + view.getWidth();
                        width = (this.mAlignedViewLocation[0] + dividerStartAlignView.getWidth()) - dividerStartAlignView.getPaddingStart();
                    } else {
                        paddingStart = this.mAlignedViewLocation[0] + dividerStartAlignView.getPaddingStart();
                        width = this.mItemLocation[0];
                    }
                    return paddingStart - width;
                }
                return dividerProvider.getDividerStartInset();
            }
            return super.getDividerInsetStart(viewHolder);
        }
    }

    @Override
    public void smoothScrollBy(int i2, int i6, Interpolator interpolator, int i10, boolean z6) {
        if (isOverScrolling()) {
            resetOverScrollState();
        }
        this.mSmoothScrollFlag = true;
        RecyclerView.LayoutManager pVar = this.mLayout;
        if (pVar == null) {
            Log.e(TAG, "Cannot smooth scroll without a LayoutManager set. Call setLayoutManager with a non-null argument.");
            return;
        }
        if (this.mLayoutSuppressed) {
            return;
        }
        if (!pVar.canScrollHorizontally()) {
            i2 = 0;
        }
        if (!this.mLayout.canScrollVertically()) {
            i6 = 0;
        }
        if (i2 == 0 && i6 == 0) {
            return;
        }
        this.mScrollType = 0;
        if (i10 != Integer.MIN_VALUE && i10 <= 0) {
            scrollBy(i2, i6);
            return;
        }
        if (z6) {
            int i11 = i2 != 0 ? 1 : 0;
            if (i6 != 0) {
                i11 |= 2;
            }
            startNestedScroll(i11, 1);
        }
        this.mViewFlinger.smoothScrollBy(i2, i6, i10, interpolator);
    }
}

