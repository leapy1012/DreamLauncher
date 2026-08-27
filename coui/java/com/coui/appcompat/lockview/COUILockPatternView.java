package com.coui.appcompat.lockview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.customview.widget.ExploreByTouchHelper;
import com.coui.appcompat.animation.COUIEaseInterpolator;
import com.coui.appcompat.animation.COUIInEaseInterpolator;
import com.coui.appcompat.contextutil.COUIContextUtil;
import com.coui.appcompat.darkmode.COUIDarkModeUtil;
import com.coui.appcompat.vibrateutil.VibrateUtils;
import com.coui.appcompat.R;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes11.dex */
public class COUILockPatternView extends View {
    public static final long ALPHA_DELAY = 166;
    public static final long ALPHA_DURATION = 167;
    public static final long ALPHA_OFFSET = 16;
    private static final int ASPECT_LOCK_HEIGHT = 2;
    private static final int ASPECT_LOCK_WIDTH = 1;
    private static final int ASPECT_SQUARE = 0;
    public static final boolean DEBUG_A11Y = false;
    private static final float DRAG_THRESHHOLD = 0.0f;
    private static final int FEEDBACK_MIN_SIZE = 1;
    private static final float MAX_ALPHA = 255.0f;
    private static final int MILLIS_PER_CIRCLE_ANIMATING = 700;
    private static final boolean PROFILE_DRAWING = false;
    private static final String TAG = "COUILockPatternView";
    public static final long TRANSLATE_Y_DURATION = 500;
    public static final long TRANSLATE_Y_OFFSET = 16;
    public static final int VIRTUAL_BASE_VIEW_ID = 1;
    private AccessibilityManager mAccessibilityManagerService;
    private Interpolator mAlphaInterpolator;
    private long mAnimatingPeriodStart;
    private final CellState[][] mCellStates;
    private Context mContext;
    private final Path mCurrentPath;
    private int mDefaultHeight;
    private int mDefaultWidth;
    private boolean mDrawingProfilingStarted;
    private boolean mEnableHapticFeedback;
    private int mErrorColor;
    private PatternExploreByTouchHelper mExploreByTouchHelper;
    private final Interpolator mFastOutSlowInInterpolator;
    private float mHitFactor;
    private float mInProgressX;
    private float mInProgressY;
    private boolean mInStealthMode;
    private Drawable mInnerDrawable;
    private boolean mInputEnabled;
    private final Rect mInvalidate;
    private boolean mIsLinearMotorVersion;
    private boolean mIsSetPassword;
    private int mMaxTranslateY;
    private OnPatternListener mOnPatternListener;
    private float mOuterCircleMaxAlpha;
    private Drawable mOuterDrawable;
    private final Paint mPaint;
    private float mPathAlpha;
    private final Paint mPathPaint;
    private final int mPathWidth;
    private final ArrayList<Cell> mPattern;
    private DisplayMode mPatternDisplayMode;
    private final boolean[][] mPatternDrawLookup;
    private boolean mPatternInProgress;
    private int mRegularColor;
    private float mSquareHeight;
    private float mSquareWidth;
    private int mStyle;
    private int mSuccessColor;
    private final Rect mTmpInvalidateRect;
    private Interpolator mTranslateYInterpolator;
    private AnimatorListenerAdapter mWongAnimatorListener;
    private ValueAnimator mWrongAnimator;

    public static final class Cell {
        private static final Cell[][] sCells = createCells();
        private final int column;
        private final int row;

        private Cell(int i2, int i3) {
            checkRange(i2, i3);
            this.row = i2;
            this.column = i3;
        }

        private static void checkRange(int i2, int i3) {
            if (i2 < 0 || i2 > 2) {
                throw new IllegalArgumentException("row must be in range 0-2");
            }
            if (i3 < 0 || i3 > 2) {
                throw new IllegalArgumentException("column must be in range 0-2");
            }
        }

        private static Cell[][] createCells() {
            Cell[][] cellArr = (Cell[][]) Array.newInstance((Class<?>) Cell.class, 3, 3);
            for (int i2 = 0; i2 < 3; i2++) {
                for (int i3 = 0; i3 < 3; i3++) {
                    cellArr[i2][i3] = new Cell(i2, i3);
                }
            }
            return cellArr;
        }

        public static Cell of(int i2, int i3) {
            checkRange(i2, i3);
            return sCells[i2][i3];
        }

        public int getColumn() {
            return this.column;
        }

        public int getRow() {
            return this.row;
        }

        public String toString() {
            return "(row=" + this.row + ",clmn=" + this.column + ")";
        }
    }

    public static class CellState {
        float alpha;
        OnCellDrawListener cellDrawListener;
        int col;
        float innerCircleAlpha;
        float innerCircleScale;
        public ValueAnimator lineAnimator;
        public float lineEndX = Float.MIN_VALUE;
        public float lineEndY = Float.MIN_VALUE;
        boolean needDrawCircle;
        float outerCircleAlpha;
        float outerCircleScale;
        float radius;
        int row;
        float translationX;
        float translationY;

        public void setCellDrawListener(OnCellDrawListener onCellDrawListener) {
            this.cellDrawListener = onCellDrawListener;
        }

        public void setCellNumberAlpha(float f2) {
            this.alpha = f2;
            this.cellDrawListener.drawCell();
        }

        public void setCellNumberTranslateX(int i2) {
            this.translationX = i2;
            this.cellDrawListener.drawCell();
        }

        public void setCellNumberTranslateY(int i2) {
            this.translationY = i2;
            this.cellDrawListener.drawCell();
        }
    }

    public enum DisplayMode {
        Correct,
        Animate,
        Wrong,
        FingerprintMatch,
        FingerprintNoMatch
    }

    public interface OnCellDrawListener {
        void drawCell();
    }

    public interface OnPatternListener {
        void onPatternCellAdded(List<Cell> list);

        void onPatternCleared();

        void onPatternDetected(List<Cell> list);

        void onPatternStart();
    }

    public final class PatternExploreByTouchHelper extends ExploreByTouchHelper {
        private final SparseArray<VirtualViewContainer> mItems;
        private Rect mTempRect;

        public class VirtualViewContainer {
            CharSequence description;

            public VirtualViewContainer(CharSequence charSequence) {
                this.description = charSequence;
            }
        }

        public PatternExploreByTouchHelper(View view) {
            super(view);
            this.mTempRect = new Rect();
            this.mItems = new SparseArray<>();
            for (int i2 = 1; i2 < 10; i2++) {
                this.mItems.put(i2, new VirtualViewContainer(getTextForVirtualView(i2)));
            }
        }

        private Rect getBoundsForVirtualView(int i2) {
            int i3 = i2 - 1;
            Rect rect = this.mTempRect;
            int i4 = i3 / 3;
            float centerXForColumn = COUILockPatternView.this.getCenterXForColumn(i3 % 3);
            float centerYForRow = COUILockPatternView.this.getCenterYForRow(i4);
            float f2 = COUILockPatternView.this.mSquareHeight * COUILockPatternView.this.mHitFactor * 0.5f;
            float f3 = COUILockPatternView.this.mSquareWidth * COUILockPatternView.this.mHitFactor * 0.5f;
            rect.left = (int) (centerXForColumn - f3);
            rect.right = (int) (centerXForColumn + f3);
            rect.top = (int) (centerYForRow - f2);
            rect.bottom = (int) (centerYForRow + f2);
            return rect;
        }

        private CharSequence getTextForVirtualView(int i2) {
            return COUILockPatternView.this.getResources().getString(R.string.lockscreen_access_pattern_cell_added_verbose, String.valueOf(i2));
        }

        private int getVirtualViewIdForHit(float f2, float f3) {
            int columnHit;
            int rowHit = COUILockPatternView.this.getRowHit(f3);
            if (rowHit < 0 || (columnHit = COUILockPatternView.this.getColumnHit(f2)) < 0) {
                return Integer.MIN_VALUE;
            }
            boolean z2 = COUILockPatternView.this.mPatternDrawLookup[rowHit][columnHit];
            int i2 = (rowHit * 3) + columnHit + 1;
            if (z2) {
                return i2;
            }
            return Integer.MIN_VALUE;
        }

        private boolean isClickable(int i2) {
            if (i2 == Integer.MIN_VALUE || i2 == Integer.MAX_VALUE) {
                return false;
            }
            int i3 = i2 - 1;
            return !COUILockPatternView.this.mPatternDrawLookup[i3 / 3][i3 % 3];
        }

        @Override // androidx.customview.widget.ExploreByTouchHelper
        public int getVirtualViewAt(float f2, float f3) {
            return getVirtualViewIdForHit(f2, f3);
        }

        @Override // androidx.customview.widget.ExploreByTouchHelper
        public void getVisibleVirtualViews(List<Integer> list) {
            if (COUILockPatternView.this.mPatternInProgress) {
                for (int i2 = 1; i2 < 10; i2++) {
                    list.add(Integer.valueOf(i2));
                }
            }
        }

        public boolean onItemClicked(int i2) {
            invalidateVirtualView(i2);
            sendEventForVirtualView(i2, 1);
            return true;
        }

        @Override // androidx.customview.widget.ExploreByTouchHelper
        public boolean onPerformActionForVirtualView(int i2, int i3, Bundle bundle) {
            if (i3 != 16) {
                return false;
            }
            return onItemClicked(i2);
        }

        @Override // androidx.core.view.AccessibilityDelegateCompat
        public void onPopulateAccessibilityEvent(View view, AccessibilityEvent accessibilityEvent) {
            super.onPopulateAccessibilityEvent(view, accessibilityEvent);
            if (COUILockPatternView.this.mPatternInProgress) {
                return;
            }
            accessibilityEvent.setContentDescription(COUILockPatternView.this.getContext().getText(R.string.lockscreen_access_pattern_area));
        }

        @Override // androidx.customview.widget.ExploreByTouchHelper
        public void onPopulateEventForVirtualView(int i2, AccessibilityEvent accessibilityEvent) {
            VirtualViewContainer virtualViewContainer = this.mItems.get(i2);
            if (virtualViewContainer != null) {
                accessibilityEvent.getText().add(virtualViewContainer.description);
            }
        }

        @Override // androidx.customview.widget.ExploreByTouchHelper
        public void onPopulateNodeForVirtualView(int i2, @NonNull AccessibilityNodeInfoCompat accessibilityNodeInfoCompat) {
            accessibilityNodeInfoCompat.setText(getTextForVirtualView(i2));
            accessibilityNodeInfoCompat.setContentDescription(getTextForVirtualView(i2));
            if (COUILockPatternView.this.mPatternInProgress) {
                accessibilityNodeInfoCompat.setFocusable(true);
                if (isClickable(i2)) {
                    accessibilityNodeInfoCompat.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK);
                    accessibilityNodeInfoCompat.setClickable(isClickable(i2));
                }
            }
            accessibilityNodeInfoCompat.setBoundsInParent(getBoundsForVirtualView(i2));
        }
    }

    public COUILockPatternView(Context context) {
        this(context, null);
    }

    private void addCellToPattern(Cell cell) {
        this.mPatternDrawLookup[cell.getRow()][cell.getColumn()] = true;
        this.mPattern.add(cell);
        if (!this.mInStealthMode) {
            startCellActivatedAnimation(cell);
        }
        notifyCellAdded();
    }

    private float calculateLastSegmentAlpha(float f2, float f3, float f4, float f5) {
        float f6 = f2 - f4;
        float f7 = f3 - f5;
        return Math.min(1.0f, Math.max(0.0f, ((((float) Math.sqrt((f6 * f6) + (f7 * f7))) / this.mSquareWidth) - 0.3f) * 4.0f));
    }

    private void cancelLineAnimations() {
        for (int i2 = 0; i2 < 3; i2++) {
            for (int i3 = 0; i3 < 3; i3++) {
                CellState cellState = this.mCellStates[i2][i3];
                ValueAnimator valueAnimator = cellState.lineAnimator;
                if (valueAnimator != null) {
                    valueAnimator.cancel();
                    cellState.lineEndX = Float.MIN_VALUE;
                    cellState.lineEndY = Float.MIN_VALUE;
                }
            }
        }
    }

    private Cell checkForNewHit(float f2, float f3) {
        int columnHit;
        int rowHit = getRowHit(f3);
        if (rowHit >= 0 && (columnHit = getColumnHit(f2)) >= 0 && !this.mPatternDrawLookup[rowHit][columnHit]) {
            return Cell.of(rowHit, columnHit);
        }
        return null;
    }

    private void clearPatternDrawLookup() {
        for (int i2 = 0; i2 < 3; i2++) {
            for (int i3 = 0; i3 < 3; i3++) {
                this.mPatternDrawLookup[i2][i3] = false;
            }
        }
    }

    private Cell detectAndAddHit(float f2, float f3) {
        Cell cellCheckForNewHit = checkForNewHit(f2, f3);
        Cell cellOf = null;
        if (cellCheckForNewHit == null) {
            return null;
        }
        ArrayList<Cell> arrayList = this.mPattern;
        if (!arrayList.isEmpty()) {
            Cell cell = arrayList.get(arrayList.size() - 1);
            int i2 = cellCheckForNewHit.row - cell.row;
            int i3 = cellCheckForNewHit.column - cell.column;
            int i4 = cell.row;
            int i5 = cell.column;
            if (Math.abs(i2) == 2 && Math.abs(i3) != 1) {
                i4 = cell.row + (i2 > 0 ? 1 : -1);
            }
            if (Math.abs(i3) == 2 && Math.abs(i2) != 1) {
                i5 = cell.column + (i3 <= 0 ? -1 : 1);
            }
            cellOf = Cell.of(i4, i5);
        }
        if (cellOf != null && !this.mPatternDrawLookup[cellOf.row][cellOf.column]) {
            addCellToPattern(cellOf);
        }
        addCellToPattern(cellCheckForNewHit);
        if (this.mEnableHapticFeedback) {
            performHitFeedback();
        }
        return cellCheckForNewHit;
    }

    private void drawCircle(Canvas canvas, float f2, float f3, float f4, boolean z2, float f5) {
        this.mPaint.setColor(this.mRegularColor);
        this.mPaint.setAlpha((int) (f5 * 255.0f));
        canvas.drawCircle(f2, f3, f4, this.mPaint);
    }

    private void drawCircleDrawable(Canvas canvas, float f2, float f3, float f4, float f5, float f6, float f7) {
        canvas.save();
        int intrinsicWidth = this.mInnerDrawable.getIntrinsicWidth();
        float f8 = intrinsicWidth / 2;
        int i2 = (int) (f2 - f8);
        int i3 = (int) (f3 - f8);
        canvas.scale(f4, f4, f2, f3);
        this.mInnerDrawable.setTint(getCurrentColor(true));
        this.mInnerDrawable.setBounds(i2, i3, i2 + intrinsicWidth, intrinsicWidth + i3);
        this.mInnerDrawable.setAlpha((int) (f5 * 255.0f));
        this.mInnerDrawable.draw(canvas);
        canvas.restore();
        canvas.save();
        int intrinsicWidth2 = this.mOuterDrawable.getIntrinsicWidth();
        float f9 = intrinsicWidth2 / 2;
        int i4 = (int) (f2 - f9);
        int i5 = (int) (f3 - f9);
        canvas.scale(f6, f6, f2, f3);
        this.mOuterDrawable.setTint(getCurrentColor(true));
        this.mOuterDrawable.setBounds(i4, i5, i4 + intrinsicWidth2, intrinsicWidth2 + i5);
        this.mOuterDrawable.setAlpha((int) (f7 * 255.0f));
        this.mOuterDrawable.draw(canvas);
        canvas.restore();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public float getCenterXForColumn(int i2) {
        float paddingLeft = getPaddingLeft();
        float f2 = this.mSquareWidth;
        return paddingLeft + (i2 * f2) + (f2 / 2.0f);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public float getCenterYForRow(int i2) {
        float paddingTop = getPaddingTop();
        float f2 = this.mSquareHeight;
        return paddingTop + (i2 * f2) + (f2 / 2.0f);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getColumnHit(float f2) {
        float f3 = this.mSquareWidth;
        float f4 = this.mHitFactor * f3;
        float paddingLeft = getPaddingLeft() + ((f3 - f4) / 2.0f);
        for (int i2 = 0; i2 < 3; i2++) {
            float f5 = (i2 * f3) + paddingLeft;
            if (f2 >= f5 && f2 <= f5 + f4) {
                return i2;
            }
        }
        return -1;
    }

    private int getCurrentColor(boolean z2) {
        DisplayMode displayMode = this.mPatternDisplayMode;
        if (displayMode == DisplayMode.Wrong || displayMode == DisplayMode.FingerprintNoMatch) {
            return this.mErrorColor;
        }
        if (displayMode == DisplayMode.Correct || displayMode == DisplayMode.Animate || displayMode == DisplayMode.FingerprintMatch) {
            return this.mSuccessColor;
        }
        if (!z2 || this.mInStealthMode || this.mPatternInProgress) {
            return this.mRegularColor;
        }
        throw new IllegalStateException("unknown display mode " + this.mPatternDisplayMode);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getRowHit(float f2) {
        float f3 = this.mSquareHeight;
        float f4 = this.mHitFactor * f3;
        float paddingTop = getPaddingTop() + ((f3 - f4) / 2.0f);
        for (int i2 = 0; i2 < 3; i2++) {
            float f5 = (i2 * f3) + paddingTop;
            if (f2 >= f5 && f2 <= f5 + f4) {
                return i2;
            }
        }
        return -1;
    }

    private void handleActionDown(MotionEvent motionEvent) {
        this.mPathAlpha = 1.0f;
        resetPattern();
        float x2 = motionEvent.getX();
        float y2 = motionEvent.getY();
        Cell cellDetectAndAddHit = detectAndAddHit(x2, y2);
        if (cellDetectAndAddHit != null) {
            setPatternInProgress(true);
            this.mPatternDisplayMode = DisplayMode.Correct;
            notifyPatternStarted();
        } else if (this.mPatternInProgress) {
            setPatternInProgress(false);
            notifyPatternCleared();
        }
        if (cellDetectAndAddHit != null) {
            float centerXForColumn = getCenterXForColumn(cellDetectAndAddHit.column);
            float centerYForRow = getCenterYForRow(cellDetectAndAddHit.row);
            float f2 = this.mSquareWidth / 2.0f;
            float f3 = this.mSquareHeight / 2.0f;
            invalidate((int) (centerXForColumn - f2), (int) (centerYForRow - f3), (int) (centerXForColumn + f2), (int) (centerYForRow + f3));
        }
        this.mInProgressX = x2;
        this.mInProgressY = y2;
    }

    private void handleActionMove(MotionEvent motionEvent) {
        float f2 = this.mPathWidth;
        int historySize = motionEvent.getHistorySize();
        this.mTmpInvalidateRect.setEmpty();
        int i2 = 0;
        boolean z2 = false;
        while (i2 < historySize + 1) {
            float historicalX = i2 < historySize ? motionEvent.getHistoricalX(i2) : motionEvent.getX();
            float historicalY = i2 < historySize ? motionEvent.getHistoricalY(i2) : motionEvent.getY();
            Cell cellDetectAndAddHit = detectAndAddHit(historicalX, historicalY);
            int size = this.mPattern.size();
            if (cellDetectAndAddHit != null && size == 1) {
                setPatternInProgress(true);
                notifyPatternStarted();
            }
            float fAbs = Math.abs(historicalX - this.mInProgressX);
            float fAbs2 = Math.abs(historicalY - this.mInProgressY);
            if (fAbs > 0.0f || fAbs2 > 0.0f) {
                z2 = true;
            }
            if (this.mPatternInProgress && size > 0) {
                Cell cell = this.mPattern.get(size - 1);
                float centerXForColumn = getCenterXForColumn(cell.column);
                float centerYForRow = getCenterYForRow(cell.row);
                float fMin = Math.min(centerXForColumn, historicalX) - f2;
                float fMax = Math.max(centerXForColumn, historicalX) + f2;
                float fMin2 = Math.min(centerYForRow, historicalY) - f2;
                float fMax2 = Math.max(centerYForRow, historicalY) + f2;
                if (cellDetectAndAddHit != null) {
                    float f3 = this.mSquareWidth * 0.5f;
                    float f4 = this.mSquareHeight * 0.5f;
                    float centerXForColumn2 = getCenterXForColumn(cellDetectAndAddHit.column);
                    float centerYForRow2 = getCenterYForRow(cellDetectAndAddHit.row);
                    fMin = Math.min(centerXForColumn2 - f3, fMin);
                    fMax = Math.max(centerXForColumn2 + f3, fMax);
                    fMin2 = Math.min(centerYForRow2 - f4, fMin2);
                    fMax2 = Math.max(centerYForRow2 + f4, fMax2);
                }
                this.mTmpInvalidateRect.union(Math.round(fMin), Math.round(fMin2), Math.round(fMax), Math.round(fMax2));
            }
            i2++;
        }
        this.mInProgressX = motionEvent.getX();
        this.mInProgressY = motionEvent.getY();
        if (z2) {
            this.mInvalidate.union(this.mTmpInvalidateRect);
            invalidate(this.mInvalidate);
            this.mInvalidate.set(this.mTmpInvalidateRect);
        }
    }

    private void handleActionUp() {
        if (this.mPattern.isEmpty()) {
            return;
        }
        setPatternInProgress(false);
        cancelLineAnimations();
        notifyPatternDetected();
        invalidate();
    }

    private void initCellAnim(CellState cellState, List<Animator> list, int i2) {
        cellState.setCellNumberAlpha(0.0f);
        cellState.setCellNumberTranslateY(this.mMaxTranslateY);
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(cellState, "cellNumberAlpha", 0.0f, Color.alpha(this.mRegularColor) / 255.0f);
        long j2 = ((long) i2) * 16;
        objectAnimatorOfFloat.setStartDelay(166 + j2);
        objectAnimatorOfFloat.setDuration(167L);
        objectAnimatorOfFloat.setInterpolator(this.mAlphaInterpolator);
        list.add(objectAnimatorOfFloat);
        ObjectAnimator objectAnimatorOfInt = ObjectAnimator.ofInt(cellState, "cellNumberTranslateY", this.mMaxTranslateY, 0);
        objectAnimatorOfInt.setStartDelay(j2);
        objectAnimatorOfInt.setDuration(500L);
        objectAnimatorOfInt.setInterpolator(this.mTranslateYInterpolator);
        list.add(objectAnimatorOfInt);
    }

    private void notifyCellAdded() {
        OnPatternListener onPatternListener = this.mOnPatternListener;
        if (onPatternListener != null) {
            onPatternListener.onPatternCellAdded(this.mPattern);
        }
        this.mExploreByTouchHelper.invalidateRoot();
    }

    private void notifyPatternCleared() {
        sendAccessEvent(R.string.lockscreen_access_pattern_cleared);
        OnPatternListener onPatternListener = this.mOnPatternListener;
        if (onPatternListener != null) {
            onPatternListener.onPatternCleared();
        }
    }

    private void notifyPatternDetected() {
        sendAccessEvent(R.string.lockscreen_access_pattern_detected);
        OnPatternListener onPatternListener = this.mOnPatternListener;
        if (onPatternListener != null) {
            onPatternListener.onPatternDetected(this.mPattern);
        }
    }

    private void notifyPatternStarted() {
        sendAccessEvent(R.string.lockscreen_access_pattern_start);
        OnPatternListener onPatternListener = this.mOnPatternListener;
        if (onPatternListener != null) {
            onPatternListener.onPatternStart();
        }
    }

    private void performHitFeedback() {
        if (this.mIsLinearMotorVersion) {
            performHapticFeedback(302);
        } else {
            performHapticFeedback(1);
        }
    }

    private void performWrongModeFeedback() {
        if (this.mEnableHapticFeedback) {
            if (this.mIsLinearMotorVersion) {
                performHapticFeedback(304, 3);
            } else {
                performHapticFeedback(300, 3);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void resetPattern() {
        this.mPattern.clear();
        clearPatternDrawLookup();
        this.mPatternDisplayMode = DisplayMode.Correct;
        invalidate();
    }

    private int resolveMeasured(int i2, int i3) {
        int size = View.MeasureSpec.getSize(i2);
        int mode = View.MeasureSpec.getMode(i2);
        return mode != Integer.MIN_VALUE ? mode != 0 ? size : i3 : Math.max(size, i3);
    }

    private void sendAccessEvent(int i2) {
        announceForAccessibility(this.mContext.getString(i2));
    }

    private void setPatternInProgress(boolean z2) {
        this.mPatternInProgress = z2;
        this.mExploreByTouchHelper.invalidateRoot();
    }

    private void startCellActivatedAnimation(Cell cell) {
        CellState cellState = this.mCellStates[cell.row][cell.column];
        startOuterAnimation(cellState);
        startInnerAnimation(cellState);
        startLineEndAnimation(cellState, this.mInProgressX, this.mInProgressY, getCenterXForColumn(cell.column), getCenterYForRow(cell.row));
    }

    private void startFingerprintNoMatchAnimator() {
        ValueAnimator valueAnimatorOfPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofKeyframe("pathAlpha", Keyframe.ofFloat(0.0f, 1.0f), Keyframe.ofFloat(0.2f, 0.35f), Keyframe.ofFloat(0.4f, 1.0f), Keyframe.ofFloat(0.6f, 0.15f), Keyframe.ofFloat(0.8f, 0.5f), Keyframe.ofFloat(1.0f, 0.0f)));
        valueAnimatorOfPropertyValuesHolder.setDuration(1000L);
        valueAnimatorOfPropertyValuesHolder.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.coui.appcompat.lockview.COUILockPatternView.3
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                for (int i2 = 0; i2 < 3; i2++) {
                    for (int i3 = 0; i3 < 3; i3++) {
                        CellState cellState = COUILockPatternView.this.mCellStates[i2][i3];
                        float fFloatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                        cellState.innerCircleAlpha = fFloatValue;
                        cellState.needDrawCircle = fFloatValue <= 0.1f;
                    }
                }
                COUILockPatternView.this.invalidate();
            }
        });
        valueAnimatorOfPropertyValuesHolder.start();
    }

    private void startInnerAnimation(final CellState cellState) {
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        valueAnimatorOfFloat.setInterpolator(new COUIEaseInterpolator());
        valueAnimatorOfFloat.setDuration(230L);
        valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.coui.appcompat.lockview.COUILockPatternView.9
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                cellState.innerCircleAlpha = ((Float) valueAnimator.getAnimatedValue()).floatValue();
            }
        });
        valueAnimatorOfFloat.start();
    }

    private void startLineEndAnimation(final CellState cellState, final float f2, final float f3, final float f4, final float f5) {
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.coui.appcompat.lockview.COUILockPatternView.5
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float fFloatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                CellState cellState2 = cellState;
                float f6 = 1.0f - fFloatValue;
                cellState2.lineEndX = (f2 * f6) + (f4 * fFloatValue);
                cellState2.lineEndY = (f6 * f3) + (fFloatValue * f5);
                COUILockPatternView.this.invalidate();
            }
        });
        valueAnimatorOfFloat.addListener(new AnimatorListenerAdapter() { // from class: com.coui.appcompat.lockview.COUILockPatternView.6
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                cellState.lineAnimator = null;
            }
        });
        valueAnimatorOfFloat.setInterpolator(this.mFastOutSlowInInterpolator);
        valueAnimatorOfFloat.setDuration(100L);
        valueAnimatorOfFloat.start();
        cellState.lineAnimator = valueAnimatorOfFloat;
    }

    private void startOuterAnimation(final CellState cellState) {
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(460L);
        animatorSet.setInterpolator(new COUIInEaseInterpolator());
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(1.0f, 7.0f);
        valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.coui.appcompat.lockview.COUILockPatternView.7
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                cellState.outerCircleScale = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                COUILockPatternView.this.invalidate();
            }
        });
        ValueAnimator valueAnimatorOfPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofKeyframe("alpha", Keyframe.ofFloat(0.0f, 0.0f), Keyframe.ofFloat(0.5f, this.mOuterCircleMaxAlpha), Keyframe.ofFloat(1.0f, 0.0f)));
        valueAnimatorOfPropertyValuesHolder.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.coui.appcompat.lockview.COUILockPatternView.8
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                cellState.outerCircleAlpha = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                COUILockPatternView.this.invalidate();
            }
        });
        animatorSet.play(valueAnimatorOfFloat).with(valueAnimatorOfPropertyValuesHolder);
        animatorSet.start();
    }

    private void startWrongAnimator() {
        ValueAnimator valueAnimatorOfPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofKeyframe("pathAlpha", Keyframe.ofFloat(0.0f, 1.0f), Keyframe.ofFloat(0.2f, 0.35f), Keyframe.ofFloat(0.4f, 1.0f), Keyframe.ofFloat(0.6f, 0.15f), Keyframe.ofFloat(0.8f, 0.5f), Keyframe.ofFloat(1.0f, 0.0f)));
        this.mWrongAnimator = valueAnimatorOfPropertyValuesHolder;
        valueAnimatorOfPropertyValuesHolder.setDuration(1000L);
        this.mWrongAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.coui.appcompat.lockview.COUILockPatternView.2
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                COUILockPatternView.this.mPathAlpha = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                for (Cell cell : COUILockPatternView.this.mPattern) {
                    CellState cellState = COUILockPatternView.this.mCellStates[cell.row][cell.column];
                    cellState.innerCircleAlpha = COUILockPatternView.this.mPathAlpha;
                    cellState.needDrawCircle = COUILockPatternView.this.mPathAlpha <= 0.1f;
                }
                COUILockPatternView.this.invalidate();
            }
        });
        this.mWrongAnimator.start();
    }

    @Deprecated
    public void clearPattern(boolean z2) {
    }

    public void disableInput() {
        this.mInputEnabled = false;
    }

    @Override // android.view.View
    public boolean dispatchHoverEvent(MotionEvent motionEvent) {
        return this.mExploreByTouchHelper.dispatchHoverEvent(motionEvent) | super.dispatchHoverEvent(motionEvent);
    }

    public void enableInput() {
        this.mInputEnabled = true;
    }

    public CellState[][] getCellStates() {
        return this.mCellStates;
    }

    public AnimatorSet getEnterAnim() {
        AnimatorSet animatorSet = new AnimatorSet();
        ArrayList arrayList = new ArrayList();
        for (int i2 = 0; i2 < 3; i2++) {
            for (int i3 = 0; i3 < 3; i3++) {
                initCellAnim(this.mCellStates[i2][i3], arrayList, (i2 * 3) + i3);
            }
        }
        animatorSet.playTogether(arrayList);
        return animatorSet;
    }

    @Deprecated
    public Animator getFailAnimator() {
        return ValueAnimator.ofFloat(0.0f, 1.0f);
    }

    @Deprecated
    public Animator getSuccessAnimator() {
        return ValueAnimator.ofInt(255, 0);
    }

    public boolean isInStealthMode() {
        return this.mInStealthMode;
    }

    public boolean isSetLockPassword() {
        return this.mIsSetPassword;
    }

    public boolean isTactileFeedbackEnabled() {
        return this.mEnableHapticFeedback;
    }

    @Override // android.view.View
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ValueAnimator valueAnimator = this.mWrongAnimator;
        if (valueAnimator != null) {
            valueAnimator.removeAllUpdateListeners();
            this.mWrongAnimator.removeAllListeners();
            this.mWrongAnimator = null;
        }
    }

    @Override // android.view.View
    public void onDraw(Canvas canvas) {
        ArrayList<Cell> arrayList = this.mPattern;
        int size = arrayList.size();
        boolean[][] zArr = this.mPatternDrawLookup;
        if (this.mPatternDisplayMode == DisplayMode.Animate) {
            int i2 = (int) (SystemClock.elapsedRealtime() - this.mAnimatingPeriodStart) % ((size + 1) * MILLIS_PER_CIRCLE_ANIMATING);
            int i3 = i2 / MILLIS_PER_CIRCLE_ANIMATING;
            clearPatternDrawLookup();
            for (int i4 = 0; i4 < i3; i4++) {
                Cell cell = arrayList.get(i4);
                zArr[cell.getRow()][cell.getColumn()] = true;
            }
            if (i3 > 0 && i3 < size) {
                float f2 = (i2 % MILLIS_PER_CIRCLE_ANIMATING) / 700.0f;
                Cell cell2 = arrayList.get(i3 - 1);
                float centerXForColumn = getCenterXForColumn(cell2.column);
                float centerYForRow = getCenterYForRow(cell2.row);
                Cell cell3 = arrayList.get(i3);
                float f3 = (getCenterXForColumn(cell3.column) - centerXForColumn) * f2;
                float f4 = f2 * (getCenterYForRow(cell3.row) - centerYForRow);
                this.mInProgressX = centerXForColumn + f3;
                this.mInProgressY = centerYForRow + f4;
            }
            invalidate();
        }
        Path path = this.mCurrentPath;
        path.rewind();
        if (!this.mInStealthMode) {
            this.mPathPaint.setColor(getCurrentColor(true));
            this.mPathPaint.setAlpha((int) (this.mPathAlpha * 255.0f));
            float centerXForColumn2 = 0.0f;
            float centerYForRow2 = 0.0f;
            boolean z2 = false;
            for (int i5 = 0; i5 < size; i5++) {
                Cell cell4 = arrayList.get(i5);
                if (!zArr[cell4.row][cell4.column]) {
                    break;
                }
                centerXForColumn2 = getCenterXForColumn(cell4.column);
                centerYForRow2 = getCenterYForRow(cell4.row);
                if (i5 == 0) {
                    path.rewind();
                    path.moveTo(centerXForColumn2, centerYForRow2);
                } else {
                    CellState cellState = this.mCellStates[cell4.row][cell4.column];
                    float f5 = cellState.lineEndX;
                    float f6 = cellState.lineEndY;
                    if (f5 != Float.MIN_VALUE && f6 != Float.MIN_VALUE) {
                        path.lineTo(f5, f6);
                    } else {
                        path.lineTo(centerXForColumn2, centerYForRow2);
                    }
                }
                z2 = true;
            }
            if ((this.mPatternInProgress || this.mPatternDisplayMode == DisplayMode.Animate) && z2) {
                path.moveTo(centerXForColumn2, centerYForRow2);
                path.lineTo(this.mInProgressX, this.mInProgressY);
            }
            canvas.drawPath(path, this.mPathPaint);
        }
        for (int i6 = 0; i6 < 3; i6++) {
            float centerYForRow3 = getCenterYForRow(i6);
            for (int i7 = 0; i7 < 3; i7++) {
                CellState cellState2 = this.mCellStates[i6][i7];
                float centerXForColumn3 = getCenterXForColumn(i7);
                float f7 = cellState2.translationY;
                float f8 = cellState2.translationX;
                boolean z3 = zArr[i6][i7];
                if (z3 || this.mPatternDisplayMode == DisplayMode.FingerprintNoMatch) {
                    drawCircleDrawable(canvas, ((int) centerXForColumn3) + f8, ((int) centerYForRow3) + f7, cellState2.innerCircleScale, cellState2.innerCircleAlpha, cellState2.outerCircleScale, cellState2.outerCircleAlpha);
                }
                if (cellState2.needDrawCircle) {
                    drawCircle(canvas, ((int) centerXForColumn3) + f8, ((int) centerYForRow3) + f7, cellState2.radius, z3, cellState2.alpha);
                }
            }
        }
    }

    @Override // android.view.View
    public boolean onHoverEvent(MotionEvent motionEvent) {
        if (this.mAccessibilityManagerService.isTouchExplorationEnabled()) {
            int action = motionEvent.getAction();
            if (action == 7) {
                motionEvent.setAction(2);
            } else if (action == 9) {
                motionEvent.setAction(0);
            } else if (action == 10) {
                motionEvent.setAction(1);
            }
            onTouchEvent(motionEvent);
            motionEvent.setAction(action);
        }
        return super.onHoverEvent(motionEvent);
    }

    @Override // android.view.View
    public void onMeasure(int i2, int i3) {
        int mode = View.MeasureSpec.getMode(i2);
        int size = View.MeasureSpec.getSize(i2);
        int mode2 = View.MeasureSpec.getMode(i3);
        int size2 = View.MeasureSpec.getSize(i3);
        if (mode == Integer.MIN_VALUE) {
            size = this.mDefaultWidth;
        }
        if (mode2 == Integer.MIN_VALUE) {
            size2 = this.mDefaultHeight;
        }
        setMeasuredDimension(size, size2);
    }

    @Override // android.view.View
    public void onRestoreInstanceState(Parcelable parcelable) {
        SavedState savedState = (SavedState) parcelable;
        super.onRestoreInstanceState(savedState.getSuperState());
        setPattern(DisplayMode.Correct, COUILockPatternUtils.stringToPattern(savedState.getSerializedPattern()));
        this.mPatternDisplayMode = DisplayMode.values()[savedState.getDisplayMode()];
        this.mInputEnabled = savedState.isInputEnabled();
        this.mInStealthMode = savedState.isInStealthMode();
        this.mEnableHapticFeedback = savedState.isTactileFeedbackEnabled();
    }

    @Override // android.view.View
    public Parcelable onSaveInstanceState() {
        return new SavedState(super.onSaveInstanceState(), COUILockPatternUtils.patternToString(this.mPattern), this.mPatternDisplayMode.ordinal(), this.mInputEnabled, this.mInStealthMode, this.mEnableHapticFeedback);
    }

    @Override // android.view.View
    public void onSizeChanged(int i2, int i3, int i4, int i5) {
        this.mSquareWidth = ((i2 - getPaddingLeft()) - getPaddingRight()) / 3.0f;
        this.mSquareHeight = ((i3 - getPaddingTop()) - getPaddingBottom()) / 3.0f;
        this.mExploreByTouchHelper.invalidateRoot();
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (!this.mInputEnabled || !isEnabled()) {
            return false;
        }
        int action = motionEvent.getAction();
        if (action == 0) {
            ValueAnimator valueAnimator = this.mWrongAnimator;
            if (valueAnimator != null && valueAnimator.isRunning()) {
                this.mWrongAnimator.end();
            }
            handleActionDown(motionEvent);
            return true;
        }
        if (action == 1) {
            handleActionUp();
            return true;
        }
        if (action == 2) {
            handleActionMove(motionEvent);
            return true;
        }
        if (action != 3) {
            return false;
        }
        if (this.mPatternInProgress) {
            setPatternInProgress(false);
            resetPattern();
            notifyPatternCleared();
        }
        return true;
    }

    public void refresh() {
        String resourceTypeName = getResources().getResourceTypeName(this.mStyle);
        TypedArray typedArrayObtainStyledAttributes = null;
        if ("attr".equals(resourceTypeName)) {
            typedArrayObtainStyledAttributes = this.mContext.obtainStyledAttributes(null, R.styleable.COUILockPatternView, this.mStyle, 0);
        } else if ("style".equals(resourceTypeName)) {
            typedArrayObtainStyledAttributes = this.mContext.obtainStyledAttributes(null, R.styleable.COUILockPatternView, 0, this.mStyle);
        }
        if (typedArrayObtainStyledAttributes != null) {
            this.mRegularColor = typedArrayObtainStyledAttributes.getColor(R.styleable.COUILockPatternView_couiRegularColor, 0);
            this.mErrorColor = typedArrayObtainStyledAttributes.getColor(R.styleable.COUILockPatternView_couiErrorColor, 0);
            this.mSuccessColor = typedArrayObtainStyledAttributes.getColor(R.styleable.COUILockPatternView_couiSuccessColor, 0);
            this.mPathPaint.setColor(typedArrayObtainStyledAttributes.getColor(R.styleable.COUILockPatternView_couiPathColor, this.mRegularColor));
            this.mOuterCircleMaxAlpha = typedArrayObtainStyledAttributes.getFloat(R.styleable.COUILockPatternView_couiOuterCircleMaxAlpha, 0.0f);
            typedArrayObtainStyledAttributes.recycle();
        }
    }

    public void setDisplayMode(DisplayMode displayMode) {
        setDisplayMode(displayMode, true);
    }

    public void setErrorColor(int i2) {
        this.mErrorColor = i2;
    }

    public void setInStealthMode(boolean z2) {
        this.mInStealthMode = z2;
    }

    public void setLockPassword(boolean z2) {
        this.mIsSetPassword = z2;
    }

    public void setOnPatternListener(OnPatternListener onPatternListener) {
        this.mOnPatternListener = onPatternListener;
    }

    public void setOuterCircleMaxAlpha(int i2) {
        this.mOuterCircleMaxAlpha = i2;
    }

    public void setPathColor(int i2) {
        this.mPathPaint.setColor(i2);
    }

    public void setPattern(DisplayMode displayMode, List<Cell> list) {
        this.mPattern.clear();
        this.mPattern.addAll(list);
        clearPatternDrawLookup();
        for (Cell cell : list) {
            this.mPatternDrawLookup[cell.getRow()][cell.getColumn()] = true;
        }
        setDisplayMode(displayMode);
    }

    public void setRegularColor(int i2) {
        this.mRegularColor = i2;
    }

    public void setSuccessColor(int i2) {
        this.mSuccessColor = i2;
    }

    @Deprecated
    public void setSuccessFinger() {
    }

    public void setTactileFeedbackEnabled(boolean z2) {
        this.mEnableHapticFeedback = z2;
    }

    public static class SavedState extends View.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() { // from class: com.coui.appcompat.lockview.COUILockPatternView.SavedState.1
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            /* JADX WARN: Can't rename method to resolve collision */
            @Override // android.os.Parcelable.Creator
            public SavedState[] newArray(int i2) {
                return new SavedState[i2];
            }
        };
        private final int mDisplayMode;
        private final boolean mInStealthMode;
        private final boolean mInputEnabled;
        private final String mSerializedPattern;
        private final boolean mTactileFeedbackEnabled;

        public int getDisplayMode() {
            return this.mDisplayMode;
        }

        public String getSerializedPattern() {
            return this.mSerializedPattern;
        }

        public boolean isInStealthMode() {
            return this.mInStealthMode;
        }

        public boolean isInputEnabled() {
            return this.mInputEnabled;
        }

        public boolean isTactileFeedbackEnabled() {
            return this.mTactileFeedbackEnabled;
        }

        @Override // android.view.View.BaseSavedState, android.view.AbsSavedState, android.os.Parcelable
        public void writeToParcel(Parcel parcel, int i2) {
            super.writeToParcel(parcel, i2);
            parcel.writeString(this.mSerializedPattern);
            parcel.writeInt(this.mDisplayMode);
            parcel.writeValue(Boolean.valueOf(this.mInputEnabled));
            parcel.writeValue(Boolean.valueOf(this.mInStealthMode));
            parcel.writeValue(Boolean.valueOf(this.mTactileFeedbackEnabled));
        }

        private SavedState(Parcelable parcelable, String str, int i2, boolean z2, boolean z3, boolean z4) {
            super(parcelable);
            this.mSerializedPattern = str;
            this.mDisplayMode = i2;
            this.mInputEnabled = z2;
            this.mInStealthMode = z3;
            this.mTactileFeedbackEnabled = z4;
        }

        private SavedState(Parcel parcel) {
            super(parcel);
            this.mSerializedPattern = parcel.readString();
            this.mDisplayMode = parcel.readInt();
            this.mInputEnabled = ((Boolean) parcel.readValue(null)).booleanValue();
            this.mInStealthMode = ((Boolean) parcel.readValue(null)).booleanValue();
            this.mTactileFeedbackEnabled = ((Boolean) parcel.readValue(null)).booleanValue();
        }
    }

    public COUILockPatternView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mPathAlpha = 1.0f;
        this.mDrawingProfilingStarted = false;
        Paint paint = new Paint();
        this.mPaint = paint;
        Paint paint2 = new Paint();
        this.mPathPaint = paint2;
        this.mPattern = new ArrayList<>(9);
        this.mPatternDrawLookup = (boolean[][]) Array.newInstance((Class<?>) Boolean.TYPE, 3, 3);
        this.mInProgressX = -1.0f;
        this.mInProgressY = -1.0f;
        this.mPatternDisplayMode = DisplayMode.Correct;
        this.mInputEnabled = true;
        this.mInStealthMode = false;
        this.mEnableHapticFeedback = true;
        this.mPatternInProgress = false;
        this.mHitFactor = 0.6f;
        this.mCurrentPath = new Path();
        this.mInvalidate = new Rect();
        this.mTmpInvalidateRect = new Rect();
        this.mIsSetPassword = false;
        this.mAlphaInterpolator = new COUIEaseInterpolator();
        this.mTranslateYInterpolator = new COUIInEaseInterpolator();
        this.mWongAnimatorListener = new AnimatorListenerAdapter() { // from class: com.coui.appcompat.lockview.COUILockPatternView.4
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                COUILockPatternView.this.resetPattern();
                if (COUILockPatternView.this.mWrongAnimator != null) {
                    COUILockPatternView.this.mWrongAnimator.removeAllListeners();
                }
            }
        };
        if (attributeSet == null || attributeSet.getStyleAttribute() == 0) {
            this.mStyle = R.attr.couiLockPatternViewStyle;
        } else {
            this.mStyle = attributeSet.getStyleAttribute();
        }
        this.mContext = context;
        COUIDarkModeUtil.setForceDarkAllow(this, false);
        this.mContext = context;
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.COUILockPatternView, R.attr.couiLockPatternViewStyle, COUIContextUtil.isCOUIDarkTheme(context) ? R.style.Widget_COUI_COUILockPatternView_Dark : R.style.Widget_COUI_COUILockPatternView);
        setClickable(true);
        paint2.setAntiAlias(true);
        paint2.setDither(true);
        this.mRegularColor = typedArrayObtainStyledAttributes.getColor(R.styleable.COUILockPatternView_couiRegularColor, 0);
        this.mErrorColor = typedArrayObtainStyledAttributes.getColor(R.styleable.COUILockPatternView_couiErrorColor, 0);
        this.mSuccessColor = typedArrayObtainStyledAttributes.getColor(R.styleable.COUILockPatternView_couiSuccessColor, 0);
        paint2.setColor(typedArrayObtainStyledAttributes.getColor(R.styleable.COUILockPatternView_couiPathColor, this.mRegularColor));
        paint2.setStyle(Paint.Style.STROKE);
        paint2.setStrokeJoin(Paint.Join.ROUND);
        paint2.setStrokeCap(Paint.Cap.ROUND);
        int dimensionPixelSize = getResources().getDimensionPixelSize(R.dimen.lock_pattern_dot_line_width);
        this.mPathWidth = dimensionPixelSize;
        paint2.setStrokeWidth(dimensionPixelSize);
        int dimensionPixelSize2 = getResources().getDimensionPixelSize(R.dimen.lock_pattern_dot_size);
        paint.setAntiAlias(true);
        paint.setDither(true);
        this.mMaxTranslateY = getResources().getDimensionPixelSize(R.dimen.color_lock_pattern_view_max_translate_y);
        this.mCellStates = (CellState[][]) Array.newInstance((Class<?>) CellState.class, 3, 3);
        for (int i2 = 0; i2 < 3; i2++) {
            for (int i3 = 0; i3 < 3; i3++) {
                this.mCellStates[i2][i3] = new CellState();
                CellState cellState = this.mCellStates[i2][i3];
                cellState.radius = dimensionPixelSize2 / 2;
                cellState.row = i2;
                cellState.col = i3;
                cellState.alpha = Color.alpha(this.mRegularColor) / 255.0f;
                CellState cellState2 = this.mCellStates[i2][i3];
                cellState2.innerCircleAlpha = 0.0f;
                cellState2.innerCircleScale = 1.0f;
                cellState2.outerCircleAlpha = 0.0f;
                cellState2.outerCircleScale = 1.0f;
                cellState2.needDrawCircle = true;
                cellState2.setCellDrawListener(new OnCellDrawListener() { // from class: com.coui.appcompat.lockview.COUILockPatternView.1
                    @Override // com.coui.appcompat.lockview.COUILockPatternView.OnCellDrawListener
                    public void drawCell() {
                        COUILockPatternView.this.invalidate();
                    }
                });
            }
        }
        this.mInnerDrawable = getResources().getDrawable(R.drawable.coui_lock_pattern_inner_circle);
        this.mOuterDrawable = getResources().getDrawable(R.drawable.coui_lock_pattern_outer_circle);
        this.mDefaultWidth = getResources().getDimensionPixelSize(R.dimen.coui_lock_pattern_view_width);
        this.mDefaultHeight = getResources().getDimensionPixelSize(R.dimen.coui_lock_pattern_view_height);
        this.mOuterCircleMaxAlpha = typedArrayObtainStyledAttributes.getFloat(R.styleable.COUILockPatternView_couiOuterCircleMaxAlpha, 0.0f);
        this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, 17563661);
        PatternExploreByTouchHelper patternExploreByTouchHelper = new PatternExploreByTouchHelper(this);
        this.mExploreByTouchHelper = patternExploreByTouchHelper;
        ViewCompat.setAccessibilityDelegate(this, patternExploreByTouchHelper);
        this.mAccessibilityManagerService = (AccessibilityManager) this.mContext.getSystemService(Context.ACCESSIBILITY_SERVICE);
        typedArrayObtainStyledAttributes.recycle();
        this.mIsLinearMotorVersion = VibrateUtils.isLinearMotorVersion(context);
    }

    public void clearPattern() {
        ValueAnimator valueAnimator = this.mWrongAnimator;
        if (valueAnimator == null || !valueAnimator.isRunning()) {
            resetPattern();
        } else {
            this.mWrongAnimator.addListener(this.mWongAnimatorListener);
        }
    }

    public void setDisplayMode(DisplayMode displayMode, boolean z2) {
        this.mPatternDisplayMode = displayMode;
        if (displayMode == DisplayMode.Animate) {
            if (this.mPattern.size() == 0) {
                throw new IllegalStateException("you must have a pattern to animate if you want to set the display mode to animate");
            }
            this.mAnimatingPeriodStart = SystemClock.elapsedRealtime();
            Cell cell = this.mPattern.get(0);
            this.mInProgressX = getCenterXForColumn(cell.getColumn());
            this.mInProgressY = getCenterYForRow(cell.getRow());
            clearPatternDrawLookup();
        }
        if (displayMode == DisplayMode.Wrong) {
            if (z2 && this.mPattern.size() > 1) {
                performWrongModeFeedback();
            }
            startWrongAnimator();
        }
        if (displayMode == DisplayMode.FingerprintNoMatch) {
            startFingerprintNoMatchAnimator();
        }
        invalidate();
    }
}
