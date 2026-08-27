package com.coui.appcompat.lockview;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BlendMode;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.customview.widget.ExploreByTouchHelper;
import androidx.dynamicanimation.animation.FloatValueHolder;
import com.coui.appcompat.accessibilityutil.COUIAccessibilityUtil;
import com.coui.appcompat.animation.COUIEaseInterpolator;
import com.coui.appcompat.animation.COUIInEaseInterpolator;
import com.coui.appcompat.animation.COUIOutEaseInterpolator;
import com.coui.appcompat.animation.dynamicanimation.COUIDynamicAnimation;
import com.coui.appcompat.animation.dynamicanimation.COUISpringAnimation;
import com.coui.appcompat.animation.dynamicanimation.COUISpringForce;
import com.coui.appcompat.darkmode.COUIDarkModeUtil;
import com.coui.appcompat.lockview.LightEffectHelper;
import com.coui.appcompat.log.COUILog;
import com.coui.appcompat.pressfeedback.COUIPressFeedbackHelper;
import com.coui.appcompat.uiutil.UIUtil;
import com.coui.appcompat.vibrateutil.VibrateUtils;
import com.coui.appcompat.R;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/* JADX INFO: loaded from: classes11.dex */
public class COUINumericKeyboard extends View {
    public static final long ALPHA_DELAY = 166;
    public static final long ALPHA_DURATION = 167;
    public static final long ALPHA_OFFSET = 16;
    private static final float BLUR_END_SCALE = 2.0f;
    private static final float BLUR_START_SCALE = 1.0f;
    public static final int CELL_COLUMN_COUNT = 3;
    public static final int CELL_ROW_COUNT = 4;
    public static final float DEFAULT_ALPHA_VALUE = 255.0f;
    private static final int ELEVEN = 11;

    @Deprecated
    public static final int EMPTY_NINE_AND_ELEVEN = 1;
    private static final int FADE_ANIMATOR_TIME = 160;
    private static final int FADE_BLUR_ANIMATOR_TIME = 400;
    private static final float FADE_END_SCALE = 2.5f;
    private static final float FADE_START_SCALE = 2.15f;
    public static final int FONT_VARIATION_DEFAULT = 550;
    public static final int FONT_VARIATION_DEFAULT_PLUS = 200;
    public static final String FONT_VARIATION_SETTINGS = "font_variation_settings";
    private static final float GRADIENT_COLOR_STOP_END = 1.0f;
    private static final float GRADIENT_COLOR_STOP_START = 0.0f;
    private static final float GRADIENT_INNER_STOP_1 = 0.7f;
    private static final float GRADIENT_OUTER_STOP_1 = 0.3f;
    private static final float GRADIENT_OUTER_STOP_2 = 0.6f;
    private static final float GRADIENT_OUTER_STOP_3 = 0.8f;
    private static final float INNER_SHADOW_DX = 0.0f;
    private static final float INNER_SHADOW_DY_1 = -8.0f;
    private static final float INNER_SHADOW_DY_2 = 2.0f;
    private static final float INNER_SHADOW_RADIUS_1 = 32.0f;
    private static final float INNER_SHADOW_RADIUS_2 = 8.0f;
    private static final float INNER_SHADOW_STROKE_WIDTH_1 = 20.0f;
    private static final float INNER_SHADOW_STROKE_WIDTH_2 = 12.0f;
    private static final int KEYCODE_0_COLUMN = 1;
    private static final int KEYCODE_0_ROW = 3;
    private static final int LIGHT_EFFECT = 1;
    private static final int NINE = 9;

    @Deprecated
    public static final int RETAIN_ELEVEN = 3;

    @Deprecated
    public static final int RETAIN_NINE = 2;
    private static final int RIPPLE_EFFECT = 0;
    private static final int SHOW_ANIMATOR_TIME = 100;
    private static final float SHOW_END_SCALE = 2.15f;
    private static final float SHOW_START_SCALE = 1.0f;
    private static final float SIDE_STYLE_SPRING_RESPONSE = 0.2f;
    public static final int SIDE_TYPE_DELETE = 1;
    public static final int SIDE_TYPE_FINISH = 2;
    public static final int SIDE_TYPE_NONE = 0;
    private static final String TAG = "COUINumericKeyboard";
    private static final int TEN = 10;
    public static final long TRANSLATE_Y_DURATION = 500;
    public static final long TRANSLATE_Y_OFFSET = 16;

    @Deprecated
    public int NUMERIC;

    @Deprecated
    public int WORD;
    private final AccessibilityManager mAccessibilityManagerService;
    private int mAdditionalPressableArea;
    private Interpolator mAlphaInterpolator;
    private int mBorderLineAlpha;
    private int mBorderLineColor;
    private int mBorderLineHighLightAlpha;
    private int mBorderLineHighLightColor;
    private final Paint mBorderLinePaint;
    private final float mButtonBorderWidth;
    private final Path mButtonPath;
    private int mCellHeight;
    private int mCellWidth;
    private float mCircleMaxAlpha;
    private int mCircleRadius;
    private final Paint mClipPaint;
    private Context mContext;
    private Typeface mCustomTypeface;
    private int mDefaultHeight;
    private int mDefaultWidth;
    public SideStyle mDeleteStyle;
    private boolean mDownState;
    private KeyboardDrawDelegate mDrawDelegate;
    private float mDrawableAlpha;
    private int mDrawableTranslateX;
    private int mDrawableTranslateY;
    private boolean mEnableHapticFeedback;
    private PatternExploreByTouchHelper mExploreByTouchHelper;
    public final SideStyle mFinishStyle;
    public int mFontVariationDefaultPlus;
    private RadialGradient mGradient;
    private RadialGradient mGradient2;
    private boolean mHasCustomTypeface;
    private int mHorizontalSpacing;
    private final int mInnerGradientColor1;
    private final int mInnerGradientColor2;
    private Bitmap mInnerShadowBitmap;
    private Paint mInnerShadowBitmapPaint;
    private InnerShadowHelper mInnerShadowHelper;
    private Matrix mInnerShadowMatrix;
    private boolean mIsLinearMotorVersion;
    private Drawable mKeyboardDelete;
    private int mKeyboardLineColor;
    private int mKeyboardNumberTextAlpha;
    private int mKeyboardNumberTextColor;
    private float mKeyboardNumberTextSize;
    private int[] mKeyboardNumbers;
    private SideStyle mLeftStyle;
    private int mLightShaderRadius;
    private Paint mLinePaint;
    private final int mLowerInnerShadowColor;
    private int mMaxTranslateY;
    private float mNormalAlpha;
    private GradientDrawable mNumberBackground;
    private int mNumberBackgroundAlpha;
    private int mNumberBackgroundColor;
    private int mNumberBackgroundRadius;
    private final RectF mNumberBounds;
    private float mNumberOffsetY;
    private Paint.FontMetrics mNumberTextFontMetrics;
    private TextPaint mNumberTextPaint;
    private OnClickItemListener mOnClickItemListener;
    private final int mOuterGradientColor1;
    private final int mOuterGradientColor2;
    private final int mOuterGradientColor3;
    private Paint mPaint;
    private int mPreVariation;
    private int mPressEffectStyle;
    private int mPressedColor;
    private SideStyle mRightStyle;
    private Path mShadowLayerPath;
    private int mSideBackgroundColor;
    private int mStyle;
    private float mTextAlpha;
    private int mTextTranslateX;
    private int mTextTranslateY;
    private Cell mTouchCell;
    private final RectF mTranslateBounds;
    private Interpolator mTranslateYInterpolator;
    private String mTtfPath;
    private final int mUpperInnerShadowColor;
    private int mVerticalSpacing;
    private int mViewSize;
    private Paint.FontMetricsInt mWordTextFontMetrics;
    private TextPaint mWordTextPaint;
    private Cell[][] sCells;
    private static final Interpolator DEFAULT_OUT_EASE_INTERPOLATOR = new COUIOutEaseInterpolator();
    private static final Interpolator PATH_INTERPOLATOR = new PathInterpolator(0.0f, 0.0f, 0.6f, 1.0f);

    public class Cell {
        float blurAlpha;
        Drawable blurCircle;
        ValueAnimator blurFadeAnimator;
        float blurScale;
        String cellLettersStr;
        float cellNumberAlpha;
        String cellNumberStr;
        int cellNumberTranslateX;
        int cellNumberTranslateY;
        int column;
        ValueAnimator fadeAnimator;
        float mButtonScale;
        COUIPressFeedbackHelper mButtonScaleHelper;
        float mInnerLightAlpha;
        boolean mInvalidatePaths;
        LightEffectHelper mLightEffectHelper;
        Path mLightEffectPath;
        Path mNumberPaths;
        float normalAlpha;
        Drawable normalCircle;
        float normalScale;
        int pointerId;
        int pressedColor;
        int row;
        ValueAnimator showAnimator;

        /* JADX INFO: Access modifiers changed from: private */
        public void refreshLightEffectDrawPath() {
            if (this.mInvalidatePaths) {
                refreshNumberPaths();
                this.mLightEffectPath.reset();
                this.mLightEffectPath.addOval(-COUINumericKeyboard.this.mLightShaderRadius, -COUINumericKeyboard.this.mLightShaderRadius, COUINumericKeyboard.this.mLightShaderRadius, COUINumericKeyboard.this.mLightShaderRadius, Path.Direction.CCW);
                this.mLightEffectPath.op(this.mNumberPaths, Path.Op.INTERSECT);
                this.mInvalidatePaths = false;
            }
        }

        private void refreshNumberPaths() {
            this.mNumberPaths.reset();
            float centerXForColumn = COUINumericKeyboard.this.getCenterXForColumn(this.column);
            float centerYForRow = COUINumericKeyboard.this.getCenterYForRow(this.row);
            for (int i2 = 0; i2 < 4; i2++) {
                for (int i3 = 0; i3 < 3; i3++) {
                    if ((i2 != 3 || i3 != 0) && ((i2 != 3 || i3 != 2) && (i2 != this.row || i3 != this.column))) {
                        this.mNumberPaths.addOval((COUINumericKeyboard.this.getCenterXForColumn(i3) - (COUINumericKeyboard.this.mNumberBackgroundRadius * COUINumericKeyboard.this.sCells[i2][i3].mButtonScale)) - centerXForColumn, (COUINumericKeyboard.this.getCenterYForRow(i2) - (COUINumericKeyboard.this.mNumberBackgroundRadius * COUINumericKeyboard.this.sCells[i2][i3].mButtonScale)) - centerYForRow, (COUINumericKeyboard.this.getCenterXForColumn(i3) + (COUINumericKeyboard.this.mNumberBackgroundRadius * COUINumericKeyboard.this.sCells[i2][i3].mButtonScale)) - centerXForColumn, (COUINumericKeyboard.this.getCenterYForRow(i2) + (COUINumericKeyboard.this.mNumberBackgroundRadius * COUINumericKeyboard.this.sCells[i2][i3].mButtonScale)) - centerYForRow, Path.Direction.CCW);
                    }
                }
            }
        }

        public boolean equals(Cell cell) {
            if (cell == null) {
                return false;
            }
            if (this == cell) {
                return true;
            }
            return this.row == cell.row && this.column == cell.column;
        }

        public int getColumn() {
            return this.column;
        }

        public int getRow() {
            return this.row;
        }

        public int hashCode() {
            return (this.row * 31) + this.column;
        }

        public void setCellNumberAlpha(float f2) {
            this.cellNumberAlpha = f2;
            COUINumericKeyboard.this.invalidate();
        }

        public void setCellNumberTranslateX(int i2) {
            this.cellNumberTranslateX = i2;
            COUINumericKeyboard.this.invalidate();
        }

        public void setCellNumberTranslateY(int i2) {
            this.cellNumberTranslateY = i2;
            COUINumericKeyboard.this.invalidate();
        }

        public void setCircleColor(int i2) {
            if (i2 != 0) {
                this.pressedColor = i2;
                Drawable drawable = this.normalCircle;
                if (drawable != null) {
                    drawable.mutate().setTint(i2);
                }
                Drawable drawable2 = this.blurCircle;
                if (drawable2 != null) {
                    drawable2.mutate().setTint(i2);
                }
            }
        }

        public String toString() {
            return "row " + this.row + "column " + this.column;
        }

        private Cell(int i2, int i3) {
            this.cellNumberStr = "";
            this.cellLettersStr = "";
            this.cellNumberAlpha = 1.0f;
            this.mButtonScale = 1.0f;
            this.normalAlpha = -1.0f;
            this.blurAlpha = -1.0f;
            this.mInnerLightAlpha = 0.0f;
            this.pointerId = -1;
            this.mLightEffectPath = new Path();
            this.mNumberPaths = new Path();
            this.mInvalidatePaths = true;
            COUINumericKeyboard.this.checkRange(i2, i3);
            this.row = i2;
            this.column = i3;
            this.normalCircle = COUINumericKeyboard.this.getContext().getDrawable(R.drawable.coui_number_keyboard_normal_circle);
            this.blurCircle = COUINumericKeyboard.this.getContext().getDrawable(R.drawable.coui_number_keyboard_blur_circle);
            this.normalCircle.setTint(COUINumericKeyboard.this.mPressedColor);
            this.blurCircle.setTint(COUINumericKeyboard.this.mPressedColor);
            this.pressedColor = COUINumericKeyboard.this.mPressedColor;
        }

        public boolean equals(Object obj) {
            try {
                return equals((Cell) obj);
            } catch (ClassCastException unused) {
                Log.e(COUINumericKeyboard.TAG, "ClassCastException when equals");
                return false;
            }
        }
    }

    public interface KeyboardDrawDelegate {
        Paint[] getCustomKeyboardPaint(int i2, int i3, RectF rectF);
    }

    public interface OnClickItemListener {
        void onClickLeft();

        void onClickNumber(int i2);

        void onClickRight();
    }

    @Deprecated
    public interface OnItemTouchListener {
        void OnItemTouch();
    }

    @Deprecated
    public interface OnTouchTextListener {
        void onTouchText(int i2);
    }

    @Deprecated
    public interface OnTouchUpListener {
        void OnTouchUp();
    }

    public final class PatternExploreByTouchHelper extends ExploreByTouchHelper {
        private Rect mTempRect;

        public PatternExploreByTouchHelper(View view) {
            super(view);
            this.mTempRect = new Rect();
        }

        private Rect getBoundsForVirtualView(int i2) {
            int centerXForColumn;
            int centerYForRow;
            Rect rect = this.mTempRect;
            if (i2 != -1) {
                Cell cellOf = COUINumericKeyboard.this.of(i2 / 3, i2 % 3);
                centerXForColumn = (int) COUINumericKeyboard.this.getCenterXForColumn(cellOf.column);
                centerYForRow = (int) COUINumericKeyboard.this.getCenterYForRow(cellOf.row);
            } else {
                centerXForColumn = 0;
                centerYForRow = 0;
            }
            rect.left = centerXForColumn - COUINumericKeyboard.this.mCircleRadius;
            rect.right = centerXForColumn + COUINumericKeyboard.this.mCircleRadius;
            rect.top = centerYForRow - COUINumericKeyboard.this.mCircleRadius;
            rect.bottom = centerYForRow + COUINumericKeyboard.this.mCircleRadius;
            return rect;
        }

        private int getVirtualViewIdForHit(float f2, float f3) {
            Cell cellCheckForNewHit = COUINumericKeyboard.this.checkForNewHit(f2, f3);
            if (cellCheckForNewHit == null) {
                return -1;
            }
            int row = (cellCheckForNewHit.getRow() * 3) + cellCheckForNewHit.getColumn();
            if (row == 9) {
                COUINumericKeyboard cOUINumericKeyboard = COUINumericKeyboard.this;
                if (cOUINumericKeyboard.isEmptyStyle(cOUINumericKeyboard.mLeftStyle)) {
                    row = -1;
                }
            }
            if (row == 11) {
                COUINumericKeyboard cOUINumericKeyboard2 = COUINumericKeyboard.this;
                if (cOUINumericKeyboard2.isEmptyStyle(cOUINumericKeyboard2.mRightStyle)) {
                    return -1;
                }
            }
            return row;
        }

        public int getItemCounts() {
            return 12;
        }

        public CharSequence getItemDescription(int i2) {
            if (i2 == 9) {
                COUINumericKeyboard cOUINumericKeyboard = COUINumericKeyboard.this;
                if (!cOUINumericKeyboard.isEmptyStyle(cOUINumericKeyboard.mLeftStyle)) {
                    return COUINumericKeyboard.this.mLeftStyle.mDescription;
                }
            }
            if (i2 == 11) {
                COUINumericKeyboard cOUINumericKeyboard2 = COUINumericKeyboard.this;
                if (!cOUINumericKeyboard2.isEmptyStyle(cOUINumericKeyboard2.mRightStyle)) {
                    return COUINumericKeyboard.this.mRightStyle.mDescription;
                }
            }
            if (i2 == -1) {
                return PatternExploreByTouchHelper.class.getSimpleName();
            }
            return COUINumericKeyboard.this.mKeyboardNumbers[i2] + "";
        }

        @Override // androidx.customview.widget.ExploreByTouchHelper
        public int getVirtualViewAt(float f2, float f3) {
            return getVirtualViewIdForHit(f2, f3);
        }

        @Override // androidx.customview.widget.ExploreByTouchHelper
        public void getVisibleVirtualViews(List<Integer> virtualViewIds) {
            for (int i = 0; i < getItemCounts(); i++) {
                if (i == 9 && COUINumericKeyboard.this.isEmptyStyle(COUINumericKeyboard.this.mLeftStyle)) {
                    virtualViewIds.add(-1);
                } else if (i == 11 && COUINumericKeyboard.this.isEmptyStyle(COUINumericKeyboard.this.mRightStyle)) {
                    virtualViewIds.add(-1);
                } else {
                    virtualViewIds.add(i);
                }
            }
        }

        public boolean onItemClicked(int i2) {
            invalidateVirtualView(i2);
            if (COUINumericKeyboard.this.isEnabled()) {
                COUINumericKeyboard.this.callback(i2);
                COUINumericKeyboard.this.announceForAccessibility(getItemDescription(i2));
            }
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
        }

        @Override // androidx.customview.widget.ExploreByTouchHelper
        public void onPopulateEventForVirtualView(int i2, AccessibilityEvent accessibilityEvent) {
            accessibilityEvent.getText().add(getItemDescription(i2));
        }

        @Override // androidx.customview.widget.ExploreByTouchHelper
        public void onPopulateNodeForVirtualView(int i2, @NonNull AccessibilityNodeInfoCompat accessibilityNodeInfoCompat) {
            accessibilityNodeInfoCompat.setContentDescription(getItemDescription(i2));
            accessibilityNodeInfoCompat.setClassName(COUIAccessibilityUtil.BUTTON_CLASS_NAME);
            accessibilityNodeInfoCompat.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK);
            accessibilityNodeInfoCompat.setClickable(true);
            accessibilityNodeInfoCompat.setBoundsInParent(getBoundsForVirtualView(i2));
        }
    }

    public static class SideStyle {
        private float mAlpha;
        private String mDescription;
        private Drawable mDrawable;
        private boolean mIsDisappearing;
        private COUISpringAnimation mSideStyleAlphaAnimator;
        private String mText;
        private int mTextColor;
        private float mTextSize;
        private int mType;

        public static class Builder {
            private String mDescription;
            private Drawable mDrawable;
            private String mText;
            private int mTextColor;
            private float mTextSize;
            private int mType = 0;

            public SideStyle build() {
                return new SideStyle(this);
            }

            public Builder description(String str) {
                this.mDescription = str;
                return this;
            }

            public Builder drawable(Drawable drawable) {
                this.mDrawable = drawable;
                return this;
            }

            public Builder text(String str) {
                this.mText = str;
                return this;
            }

            public Builder textColor(int i2) {
                this.mTextColor = i2;
                return this;
            }

            public Builder textSize(float f2) {
                this.mTextSize = f2;
                return this;
            }

            public Builder type(int i2) {
                this.mType = i2;
                return this;
            }
        }

        private SideStyle(Builder builder) {
            this.mAlpha = 0.0f;
            this.mSideStyleAlphaAnimator = null;
            this.mIsDisappearing = false;
            this.mDrawable = builder.mDrawable;
            this.mText = builder.mText;
            this.mTextColor = builder.mTextColor;
            this.mTextSize = builder.mTextSize;
            this.mDescription = builder.mDescription;
            this.mType = builder.mType;
        }
    }

    public COUINumericKeyboard(Context context) {
        this(context, null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void callback(int i2) {
        OnClickItemListener onClickItemListener = this.mOnClickItemListener;
        if (onClickItemListener != null) {
            if (i2 >= 0 && i2 <= 8) {
                onClickItemListener.onClickNumber(i2 + 1);
            }
            if (i2 == 10) {
                this.mOnClickItemListener.onClickNumber(0);
            }
            if (i2 == 9) {
                this.mOnClickItemListener.onClickLeft();
            }
            if (i2 == 11) {
                this.mOnClickItemListener.onClickRight();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Cell checkForNewHit(float f2, float f3) {
        int columnHit;
        int rowHit = getRowHit(f3);
        if (rowHit >= 0 && (columnHit = getColumnHit(f2)) >= 0) {
            return of(rowHit, columnHit);
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkRange(int i2, int i3) {
        if (i2 < 0 || i2 > 3) {
            throw new IllegalArgumentException("row must be in range 0-3");
        }
        if (i3 < 0 || i3 > 2) {
            throw new IllegalArgumentException("column must be in range 0-2");
        }
    }

    private void drawBackground(Canvas canvas, float f2, float f3, int i2, int i3, int i4, float f4) {
        int i5 = this.mNumberBackgroundRadius;
        this.mNumberBackground.setBounds(((int) (f2 - (i5 * f4))) + i3, ((int) (f3 - (i5 * f4))) + i4, ((int) (f2 + (i5 * f4))) + i3, ((int) (f3 + (i5 * f4))) + i4);
        this.mNumberBackground.setAlpha(i2);
        this.mNumberBackground.draw(canvas);
    }

    private void drawCell(Canvas canvas, int i2, int i3) {
        Paint[] paintArr;
        Cell cell = this.sCells[i3][i2];
        float centerXForColumn = getCenterXForColumn(i2);
        float centerYForRow = getCenterYForRow(i3);
        int i4 = (i3 * 3) + i2;
        if (i4 == 9) {
            drawSide(this.mLeftStyle, canvas, centerXForColumn, centerYForRow, cell);
            return;
        }
        if (i4 == 11) {
            drawSide(this.mRightStyle, canvas, centerXForColumn, centerYForRow, cell);
            return;
        }
        if (i4 != -1) {
            this.mNumberTextPaint.setTextSize(this.mKeyboardNumberTextSize * cell.mButtonScale);
            float fMeasureText = this.mNumberTextPaint.measureText(cell.cellNumberStr);
            Paint.FontMetrics fontMetrics = this.mNumberTextFontMetrics;
            float f2 = (centerYForRow - ((fontMetrics.descent + fontMetrics.ascent) / 2.0f)) - this.mNumberOffsetY;
            this.mNumberTextPaint.setAlpha((int) (cell.cellNumberAlpha * this.mKeyboardNumberTextAlpha));
            int i5 = this.mPressEffectStyle;
            if (i5 == 0) {
                this.mNumberBackground.setColor(this.mNumberBackgroundColor);
                drawBackground(canvas, centerXForColumn, centerYForRow, (int) (cell.cellNumberAlpha * this.mNumberBackgroundAlpha), cell.cellNumberTranslateX, cell.cellNumberTranslateY, 1.0f);
            } else if (i5 == 1) {
                KeyboardDrawDelegate keyboardDrawDelegate = this.mDrawDelegate;
                if (keyboardDrawDelegate != null) {
                    RectF rectF = this.mNumberBounds;
                    int i6 = this.mNumberBackgroundRadius;
                    float f3 = cell.mButtonScale;
                    rectF.left = (int) (centerXForColumn - (i6 * f3));
                    rectF.top = (int) (centerYForRow - (i6 * f3));
                    rectF.right = (int) ((i6 * f3) + centerXForColumn);
                    rectF.bottom = (int) ((i6 * f3) + centerYForRow);
                    Paint[] customKeyboardPaint = keyboardDrawDelegate.getCustomKeyboardPaint(i3, i2, rectF);
                    if (customKeyboardPaint != null && customKeyboardPaint.length > 0) {
                        canvas.save();
                        int length = customKeyboardPaint.length;
                        int i7 = 0;
                        while (i7 < length) {
                            Paint paint = customKeyboardPaint[i7];
                            if (paint == null) {
                                paintArr = customKeyboardPaint;
                            } else {
                                paint.setAlpha((int) (paint.getAlpha() * cell.cellNumberAlpha));
                                RectF rectF2 = this.mTranslateBounds;
                                RectF rectF3 = this.mNumberBounds;
                                float f4 = rectF3.left;
                                int i8 = cell.cellNumberTranslateX;
                                rectF2.left = f4 + i8;
                                float f5 = rectF3.top;
                                int i9 = cell.cellNumberTranslateY;
                                paintArr = customKeyboardPaint;
                                rectF2.top = f5 + i9;
                                rectF2.right = rectF3.right + i8;
                                rectF2.bottom = rectF3.bottom + i9;
                                canvas.drawOval(rectF2, paint);
                            }
                            i7++;
                            customKeyboardPaint = paintArr;
                        }
                        canvas.restore();
                    }
                }
                drawInnerShadowLayer(canvas, centerXForColumn, centerYForRow, cell, cell.cellNumberTranslateX, cell.cellNumberTranslateY, cell.cellNumberAlpha);
                drawInnerBorder(canvas, centerXForColumn, centerYForRow, cell, cell.cellNumberTranslateX, cell.cellNumberTranslateY, cell.cellNumberAlpha);
            }
            this.mNumberTextPaint.setAlpha((int) (cell.cellNumberAlpha * this.mKeyboardNumberTextAlpha));
            canvas.drawText(cell.cellNumberStr, (centerXForColumn - (fMeasureText / 2.0f)) + cell.cellNumberTranslateX, f2 + cell.cellNumberTranslateY, this.mNumberTextPaint);
        }
    }

    private void drawInnerBorder(Canvas canvas, float f2, float f3, Cell cell, int i2, int i3, float f4) {
        int iSave = canvas.save();
        this.mButtonPath.reset();
        this.mButtonPath.addCircle(f2 + i2, f3 + i3, this.mNumberBackgroundRadius * cell.mButtonScale, Path.Direction.CW);
        canvas.clipPath(this.mButtonPath);
        this.mBorderLinePaint.setStyle(Paint.Style.STROKE);
        this.mBorderLinePaint.setStrokeWidth(this.mButtonBorderWidth * 2.0f);
        if (cell.mInnerLightAlpha > 0.0f) {
            this.mBorderLinePaint.setColor(this.mBorderLineHighLightAlpha);
            this.mBorderLinePaint.setAlpha((int) cell.mInnerLightAlpha);
            this.mBorderLinePaint.setBlendMode(BlendMode.LUMINOSITY);
            canvas.drawPath(this.mButtonPath, this.mBorderLinePaint);
        }
        this.mBorderLinePaint.setColor(this.mBorderLineColor);
        this.mBorderLinePaint.setAlpha((int) (this.mBorderLineAlpha * f4));
        canvas.drawPath(this.mButtonPath, this.mBorderLinePaint);
        canvas.restoreToCount(iSave);
    }

    private void drawInnerShadowLayer(Canvas canvas, float f2, float f3, Cell cell, int i2, int i3, float f4) {
        int iSave = canvas.save();
        if (this.mInnerShadowBitmapPaint == null) {
            this.mInnerShadowBitmapPaint = new Paint();
        }
        this.mInnerShadowBitmapPaint.setAlpha((int) (f4 * 255.0f));
        Matrix matrix = this.mInnerShadowMatrix;
        if (matrix == null) {
            this.mInnerShadowMatrix = new Matrix();
        } else {
            matrix.reset();
        }
        Matrix matrix2 = this.mInnerShadowMatrix;
        float f5 = cell.mButtonScale;
        matrix2.postScale(f5, f5);
        Matrix matrix3 = this.mInnerShadowMatrix;
        float f6 = f2 + i2;
        int i4 = this.mNumberBackgroundRadius;
        float f7 = cell.mButtonScale;
        matrix3.postTranslate(f6 - (i4 * f7), (f3 + i3) - (i4 * f7));
        canvas.drawBitmap(this.mInnerShadowBitmap, this.mInnerShadowMatrix, this.mInnerShadowBitmapPaint);
        canvas.clipPath(this.mShadowLayerPath);
        canvas.restoreToCount(iSave);
    }

    private void drawLightEffect(Canvas canvas, int i2, int i3) {
        Cell cell = this.sCells[i3][i2];
        if (cell == null || getTouchIndex(cell) == -1 || cell.mInnerLightAlpha <= 0.0f) {
            return;
        }
        cell.refreshLightEffectDrawPath();
        float centerXForColumn = getCenterXForColumn(cell.column);
        float centerYForRow = getCenterYForRow(cell.row);
        canvas.save();
        canvas.translate(centerXForColumn, centerYForRow);
        cell.mLightEffectHelper.drawLightEffect(canvas, cell.mButtonScale, cell.mLightEffectPath, this.mClipPaint, 0.0f, 0.0f);
        canvas.restore();
    }

    private void drawPressCircle(Canvas canvas, int i2, int i3) {
        Cell cell = this.sCells[i3][i2];
        if (cell != null) {
            float centerXForColumn = getCenterXForColumn(cell.column);
            float centerYForRow = getCenterYForRow(cell.row);
            if (getTouchIndex(cell) != -1) {
                if (cell.normalAlpha >= 0.0f || cell.blurAlpha >= 0.0f) {
                    int i4 = this.mCircleRadius;
                    int i5 = (int) (centerXForColumn - i4);
                    int i6 = (int) (centerYForRow - i4);
                    int i7 = (int) (i4 + centerXForColumn);
                    int i8 = (int) (i4 + centerYForRow);
                    canvas.save();
                    int i9 = this.mPressedColor;
                    if (i9 != cell.pressedColor) {
                        cell.setCircleColor(i9);
                    }
                    float f2 = cell.normalScale;
                    canvas.scale(f2, f2, centerXForColumn, centerYForRow);
                    cell.normalCircle.setAlpha((int) Math.max(0.0f, cell.normalAlpha * 255.0f));
                    cell.normalCircle.setBounds(i5, i6, i7, i8);
                    cell.normalCircle.draw(canvas);
                    canvas.restore();
                    canvas.save();
                    float f3 = cell.blurScale;
                    canvas.scale(f3, f3, centerXForColumn, centerYForRow);
                    cell.blurCircle.setBounds(i5, i6, i7, i8);
                    cell.blurCircle.setAlpha((int) Math.max(0.0f, cell.blurAlpha * 255.0f));
                    cell.blurCircle.draw(canvas);
                    canvas.restore();
                    if (cell.normalAlpha == 0.0f) {
                        cell.normalAlpha = -1.0f;
                    }
                    if (cell.blurAlpha == 0.0f) {
                        cell.blurAlpha = -1.0f;
                    }
                }
            }
        }
    }

    private void drawSide(SideStyle sideStyle, Canvas canvas, float f2, float f3, Cell cell) {
        if (isEmptyStyle(sideStyle)) {
            return;
        }
        this.mNumberBackground.setColor(this.mSideBackgroundColor);
        if (sideStyle.mDrawable != null) {
            int intrinsicWidth = (int) (f2 - ((sideStyle.mDrawable.getIntrinsicWidth() * cell.mButtonScale) / 2.0f));
            int intrinsicWidth2 = (int) (intrinsicWidth + (sideStyle.mDrawable.getIntrinsicWidth() * cell.mButtonScale));
            int intrinsicHeight = (int) (f3 - ((sideStyle.mDrawable.getIntrinsicHeight() * cell.mButtonScale) / 2.0f));
            int intrinsicHeight2 = (int) (intrinsicHeight + (sideStyle.mDrawable.getIntrinsicHeight() * cell.mButtonScale));
            drawBackground(canvas, f2, f3, (int) (this.mDrawableAlpha * sideStyle.mAlpha), this.mDrawableTranslateX, this.mDrawableTranslateY, cell.mButtonScale);
            Drawable drawable = sideStyle.mDrawable;
            int i2 = this.mDrawableTranslateX;
            int i3 = this.mDrawableTranslateY;
            drawable.setBounds(intrinsicWidth + i2, intrinsicHeight + i3, intrinsicWidth2 + i2, intrinsicHeight2 + i3);
            sideStyle.mDrawable.setAlpha((int) (this.mDrawableAlpha * sideStyle.mAlpha));
            sideStyle.mDrawable.draw(canvas);
        } else if (!TextUtils.isEmpty(sideStyle.mText)) {
            this.mWordTextPaint.setTextSize(sideStyle.mTextSize * cell.mButtonScale);
            this.mWordTextPaint.setColor(sideStyle.mTextColor);
            this.mWordTextPaint.setAlpha((int) (this.mTextAlpha * sideStyle.mAlpha));
            float fMeasureText = this.mWordTextPaint.measureText(sideStyle.mText);
            this.mWordTextFontMetrics = this.mWordTextPaint.getFontMetricsInt();
            drawBackground(canvas, f2, f3, (int) (this.mTextAlpha * sideStyle.mAlpha), this.mTextTranslateX, this.mTextTranslateY, cell.mButtonScale);
            canvas.drawText(sideStyle.mText, (f2 - (fMeasureText / 2.0f)) + this.mTextTranslateX, (f3 - ((this.mWordTextFontMetrics.descent + this.mWordTextFontMetrics.ascent) / 2.0f)) + this.mTextTranslateY, this.mWordTextPaint);
        }
        if (this.mPressEffectStyle == 1) {
            drawInnerBorder(canvas, f2, f3, cell, 0, 0, 0.0f);
        }
    }

    private void ensureButtonScaleAnimator(final Cell cell) {
        if (cell != null && cell.mButtonScaleHelper == null) {
            COUIPressFeedbackHelper cOUIPressFeedbackHelper = new COUIPressFeedbackHelper(getContext());
            cell.mButtonScaleHelper = cOUIPressFeedbackHelper;
            cOUIPressFeedbackHelper.setCallback(new COUIPressFeedbackHelper.COUIPressFeedbackHelperCallback() { // from class: com.coui.appcompat.lockview.COUINumericKeyboard.5
                @Override // com.coui.appcompat.pressfeedback.COUIPressFeedbackHelper.COUIPressFeedbackHelperCallback
                public int getTargetHeight() {
                    return COUINumericKeyboard.this.mNumberBackgroundRadius * 2;
                }

                @Override // com.coui.appcompat.pressfeedback.COUIPressFeedbackHelper.COUIPressFeedbackHelperCallback
                public int getTargetWidth() {
                    return COUINumericKeyboard.this.mNumberBackgroundRadius * 2;
                }

                @Override // com.coui.appcompat.pressfeedback.COUIPressFeedbackHelper.COUIPressFeedbackHelperCallback
                public void onScaleUpdate(float f2) {
                    Cell cell2 = cell;
                    cell2.mButtonScale = f2;
                    COUINumericKeyboard.this.invalidatePaths(cell2);
                    COUINumericKeyboard.this.invalidate();
                }
            });
        }
    }

    private void ensureLightEffectAnimator(final Cell cell) {
        if (cell != null && cell.mLightEffectHelper == null) {
            LightEffectHelper lightEffectHelper = new LightEffectHelper(this, this.mNumberBackgroundRadius, this.mLightShaderRadius, this.mGradient2, this.mGradient);
            cell.mLightEffectHelper = lightEffectHelper;
            lightEffectHelper.setCallback(new LightEffectHelper.LightEffectHelperCallback() { // from class: com.coui.appcompat.lockview.COUINumericKeyboard.4
                @Override // com.coui.appcompat.lockview.LightEffectHelper.LightEffectHelperCallback
                public void onInnerLightUpdate(float f2) {
                    cell.mInnerLightAlpha = f2;
                }
            });
        }
    }

    private void ensureRightStyleAnimator(final SideStyle sideStyle) {
        if (sideStyle.mSideStyleAlphaAnimator == null) {
            COUISpringForce cOUISpringForce = new COUISpringForce();
            cOUISpringForce.setBounce(0.0f);
            cOUISpringForce.setResponse(0.2f);
            sideStyle.mSideStyleAlphaAnimator = new COUISpringAnimation(new FloatValueHolder(sideStyle.mAlpha));
            sideStyle.mSideStyleAlphaAnimator.setSpring(cOUISpringForce);
            sideStyle.mSideStyleAlphaAnimator.addUpdateListener(new COUIDynamicAnimation.OnAnimationUpdateListener() {
                @Override // com.coui.appcompat.animation.dynamicanimation.COUIDynamicAnimation.OnAnimationUpdateListener
                public void onAnimationUpdate(COUIDynamicAnimation cOUIDynamicAnimation, float f2, float f3) {
                    COUINumericKeyboard.this.lambda$ensureRightStyleAnimator$0(sideStyle, cOUIDynamicAnimation, f2, f3);
                }
            });
        }
    }

    private void executeLightEffectAnimator(Cell cell, boolean z2) {
        if (cell != null) {
            ensureLightEffectAnimator(cell);
            ensureButtonScaleAnimator(cell);
            cell.mLightEffectHelper.executeLightEffectAnimator(z2);
            cell.mButtonScaleHelper.executeFeedbackAnimator(z2);
            if (z2) {
                return;
            }
            cell.pointerId = -1;
        }
    }

    private Cell findCellByPointerId(int i2) {
        for (int i3 = 0; i3 < 4; i3++) {
            for (int i4 = 0; i4 < 3; i4++) {
                Cell cell = this.sCells[i3][i4];
                if (cell.pointerId == i2) {
                    return cell;
                }
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public float getCenterXForColumn(int i2) {
        return getPaddingLeft() + (this.mCellWidth / 2.0f) + (this.mCellWidth * i2) + (this.mHorizontalSpacing * i2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public float getCenterYForRow(int i2) {
        return getPaddingTop() + (this.mCellHeight / 2.0f) + (this.mCellHeight * i2) + (this.mVerticalSpacing * i2);
    }

    private int getColumnHit(float f2) {
        for (int i2 = 0; i2 < 3; i2++) {
            int centerXForColumn = (int) getCenterXForColumn(i2);
            int iMax = Math.max(0, Math.min(this.mHorizontalSpacing / 2, this.mAdditionalPressableArea));
            int iMax2 = Math.max((centerXForColumn - (this.mCellWidth / 2)) - iMax, 0);
            int i3 = centerXForColumn + (this.mCellWidth / 2) + iMax;
            if (iMax2 <= f2 && f2 <= i3) {
                return i2;
            }
        }
        return -1;
    }

    private int[] getDeleteCellIndex() {
        SideStyle sideStyle = this.mLeftStyle;
        if (sideStyle != null && sideStyle.mType == 1) {
            return new int[]{0, 3};
        }
        SideStyle sideStyle2 = this.mRightStyle;
        if (sideStyle2 == null || sideStyle2.mType != 1) {
            return null;
        }
        return new int[]{2, 3};
    }

    private int[] getFinishCellIndex() {
        SideStyle sideStyle = this.mLeftStyle;
        if (sideStyle != null && sideStyle.mType == 2) {
            return new int[]{0, 3};
        }
        SideStyle sideStyle2 = this.mRightStyle;
        if (sideStyle2 == null || sideStyle2.mType != 2) {
            return null;
        }
        return new int[]{2, 3};
    }

    private float[] getKeyboardNumberPosition(int i2) {
        int i3;
        int i4 = 3;
        if (i2 >= 8 && i2 <= 16) {
            int i5 = i2 - 8;
            i3 = i5 % 3;
            i4 = i5 / 3;
        } else if (i2 >= 145 && i2 <= 153) {
            int i6 = i2 - 145;
            i3 = i6 % 3;
            i4 = i6 / 3;
        } else if (i2 == 67) {
            int[] deleteCellIndex = getDeleteCellIndex();
            if (deleteCellIndex == null || deleteCellIndex.length != 2) {
                return new float[]{-1.0f, -1.0f};
            }
            i3 = deleteCellIndex[0];
            i4 = deleteCellIndex[1];
        } else if (i2 == 7 || i2 == 144) {
            i3 = 1;
        } else {
            if (i2 != 66 && i2 != FADE_ANIMATOR_TIME) {
                return new float[]{-1.0f, -1.0f};
            }
            int[] finishCellIndex = getFinishCellIndex();
            if (finishCellIndex == null || finishCellIndex.length != 2) {
                return new float[]{-1.0f, -1.0f};
            }
            i3 = finishCellIndex[0];
            i4 = finishCellIndex[1];
        }
        Cell cell = this.sCells[i4][i3];
        float centerXForColumn = getCenterXForColumn(i3);
        float centerYForRow = getCenterYForRow(i4);
        Paint.FontMetrics fontMetrics = this.mNumberTextFontMetrics;
        return new float[]{centerXForColumn + cell.cellNumberTranslateX, (centerYForRow - ((fontMetrics.descent + fontMetrics.ascent) / 2.0f)) + cell.cellNumberTranslateY};
    }

    private int getRowHit(float f2) {
        for (int i2 = 0; i2 < 4; i2++) {
            int centerYForRow = (int) getCenterYForRow(i2);
            int iMax = Math.max(0, Math.min(this.mVerticalSpacing / 2, this.mAdditionalPressableArea));
            int iMax2 = Math.max((centerYForRow - (this.mCellHeight / 2)) - iMax, 0);
            int i3 = centerYForRow + (this.mCellHeight / 2) + iMax;
            if (iMax2 <= f2 && f2 <= i3) {
                return i2;
            }
        }
        return -1;
    }

    private Typeface getTypeface(int[] iArr) {
        try {
            final java.io.File fontFile = new java.io.File(this.mTtfPath);
            if (!fontFile.isFile()) {
                return Typeface.create("sans-serif", Typeface.NORMAL);
            }
            if (iArr[0] == 0) {
                return new Typeface.Builder(this.mTtfPath).build();
            }
            return new Typeface.Builder(this.mTtfPath)
                    .setFontVariationSettings("'wght' " + (iArr[1] + this.mFontVariationDefaultPlus))
                    .build();
        } catch (RuntimeException e) {
            return Typeface.create("sans-serif", Typeface.NORMAL);
        }
    }

    private synchronized void handleActionCancel(int i2) {
        try {
            Cell cellFindCellByPointerId = findCellByPointerId(i2);
            int i3 = this.mPressEffectStyle;
            if (i3 == 0) {
                initFadeAnimator(cellFindCellByPointerId);
            } else if (i3 == 1) {
                executeLightEffectAnimator(cellFindCellByPointerId, false);
            }
            int touchIndex = getTouchIndex(cellFindCellByPointerId);
            if (this.mAccessibilityManagerService.isTouchExplorationEnabled() && cellFindCellByPointerId != null) {
                this.mExploreByTouchHelper.invalidateRoot();
                if (this.mEnableHapticFeedback && touchIndex != -1) {
                    setTouchFeedback();
                }
            }
            invalidate();
        } catch (Throwable th) {
            throw th;
        }
    }

    private void handleActionDown(MotionEvent motionEvent, int i2) {
        handleActionDown(motionEvent.getX(i2), motionEvent.getY(i2), motionEvent.getPointerId(i2));
    }

    private void handleActionMove(MotionEvent motionEvent, int i2) {
        int iFindPointerIndex = motionEvent.findPointerIndex(i2);
        if (iFindPointerIndex >= 0) {
            handleActionMove(motionEvent.getX(iFindPointerIndex), motionEvent.getY(iFindPointerIndex), i2);
        }
    }

    private void handleActionUp(MotionEvent motionEvent, int i2) {
        handleActionUp(motionEvent.getX(i2), motionEvent.getY(i2), motionEvent.getPointerId(i2));
    }

    private void handleKeyEvent(int i2, boolean z2) {
        if (isValidKeyCode(i2)) {
            float[] keyboardNumberPosition = getKeyboardNumberPosition(i2);
            if (z2) {
                handleActionDown(keyboardNumberPosition[0], keyboardNumberPosition[1], -1);
            } else {
                handleActionUp(keyboardNumberPosition[0], keyboardNumberPosition[1], -1);
            }
        }
    }

    private void initCellAnim(Cell cell, List<Animator> list, int i2) {
        cell.setCellNumberAlpha(0.0f);
        cell.setCellNumberTranslateY(this.mMaxTranslateY);
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(cell, "cellNumberAlpha", 0.0f, 1.0f);
        objectAnimatorOfFloat.setStartDelay(166 + (((long) ((i2 == 10 && isEmptyStyle(this.mLeftStyle)) ? i2 - 1 : i2)) * 16));
        objectAnimatorOfFloat.setDuration(167L);
        objectAnimatorOfFloat.setInterpolator(this.mAlphaInterpolator);
        list.add(objectAnimatorOfFloat);
        ObjectAnimator objectAnimatorOfInt = ObjectAnimator.ofInt(cell, "cellNumberTranslateY", this.mMaxTranslateY, 0);
        if (i2 == 10 && isEmptyStyle(this.mLeftStyle)) {
            i2--;
        }
        objectAnimatorOfInt.setStartDelay(16 * ((long) i2));
        objectAnimatorOfInt.setDuration(500L);
        objectAnimatorOfInt.setInterpolator(this.mTranslateYInterpolator);
        list.add(objectAnimatorOfInt);
    }

    private void initFadeAnimator(final Cell cell) {
        if (cell == null) {
            return;
        }
        cell.pointerId = -1;
        if (cell.fadeAnimator == null) {
            ValueAnimator valueAnimatorOfPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat("scaleHolder", 2.15f, FADE_END_SCALE), PropertyValuesHolder.ofFloat("alphaHolder", this.mCircleMaxAlpha, 0.0f));
            valueAnimatorOfPropertyValuesHolder.setDuration(160L);
            valueAnimatorOfPropertyValuesHolder.setInterpolator(PATH_INTERPOLATOR);
            valueAnimatorOfPropertyValuesHolder.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.coui.appcompat.lockview.COUINumericKeyboard.2
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    Cell cell2 = cell;
                    if (cell2 == null) {
                        return;
                    }
                    cell2.normalAlpha = ((Float) valueAnimator.getAnimatedValue("alphaHolder")).floatValue();
                    cell.normalScale = ((Float) valueAnimator.getAnimatedValue("scaleHolder")).floatValue();
                    COUINumericKeyboard.this.invalidate();
                }
            });
            cell.fadeAnimator = valueAnimatorOfPropertyValuesHolder;
        }
        if (cell.blurFadeAnimator == null) {
            ValueAnimator valueAnimatorOfPropertyValuesHolder2 = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofKeyframe("blurAlpha", Keyframe.ofFloat(0.0f, 0.0f), Keyframe.ofFloat(0.5f, this.mCircleMaxAlpha), Keyframe.ofFloat(1.0f, 0.0f)), PropertyValuesHolder.ofFloat("blurScale", 1.0f, 2.0f));
            valueAnimatorOfPropertyValuesHolder2.setDuration(400L);
            valueAnimatorOfPropertyValuesHolder2.setInterpolator(PATH_INTERPOLATOR);
            valueAnimatorOfPropertyValuesHolder2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.coui.appcompat.lockview.COUINumericKeyboard.3
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    Cell cell2 = cell;
                    if (cell2 == null) {
                        return;
                    }
                    cell2.blurAlpha = ((Float) valueAnimator.getAnimatedValue("blurAlpha")).floatValue();
                    cell.blurScale = ((Float) valueAnimator.getAnimatedValue("blurScale")).floatValue();
                    COUINumericKeyboard.this.invalidate();
                }
            });
            cell.blurFadeAnimator = valueAnimatorOfPropertyValuesHolder2;
        }
        ValueAnimator valueAnimator = cell.showAnimator;
        if (valueAnimator != null && valueAnimator.isRunning()) {
            cell.showAnimator.end();
        }
        cell.fadeAnimator.start();
        cell.blurFadeAnimator.start();
    }

    private void initInnerShadowBitmap() {
        Path path = this.mShadowLayerPath;
        if (path == null) {
            this.mShadowLayerPath = new Path();
        } else {
            path.reset();
        }
        Path path2 = this.mShadowLayerPath;
        int i2 = this.mNumberBackgroundRadius;
        path2.addCircle(i2, i2, i2, Path.Direction.CCW);
        InnerShadowHelper innerShadowHelper = this.mInnerShadowHelper;
        if (innerShadowHelper == null) {
            this.mInnerShadowHelper = new InnerShadowHelper(this.mCellWidth, this.mCellHeight);
        } else {
            innerShadowHelper.reset();
        }
        this.mInnerShadowHelper.addInnerShadowLayer(32.0f, 0.0f, INNER_SHADOW_DY_1, this.mUpperInnerShadowColor, 0, 20.0f, this.mShadowLayerPath);
        this.mInnerShadowHelper.addInnerShadowLayer(8.0f, 0.0f, 2.0f, this.mLowerInnerShadowColor, 0, 12.0f, this.mShadowLayerPath);
        this.mInnerShadowBitmap = this.mInnerShadowHelper.createInnerShadowBitmap();
    }

    private void initPaint() {
        Paint paint = new Paint(5);
        this.mPaint = paint;
        paint.setColor(this.mPressedColor);
        this.mPaint.setMaskFilter(new BlurMaskFilter(20.0f, BlurMaskFilter.Blur.NORMAL));
        this.mPaint.setAlpha(0);
        this.mNumberTextPaint.setTextSize(this.mKeyboardNumberTextSize);
        this.mNumberTextPaint.setColor(this.mKeyboardNumberTextColor);
        this.mNumberTextPaint.setAntiAlias(true);
        this.mKeyboardNumberTextAlpha = this.mNumberTextPaint.getAlpha();
        if (this.mHasCustomTypeface) {
            Typeface typeface = this.mCustomTypeface;
            if (typeface != null) {
                this.mNumberTextPaint.setTypeface(typeface);
                invalidate();
            }
        } else {
            updateNumberTextTypeface();
        }
        this.mNumberTextFontMetrics = this.mNumberTextPaint.getFontMetrics();
        this.mLinePaint.setColor(this.mKeyboardLineColor);
        this.mLinePaint.setAntiAlias(true);
        this.mLinePaint.setStyle(Paint.Style.STROKE);
        this.mWordTextPaint.setFakeBoldText(true);
        this.mWordTextPaint.setAntiAlias(true);
    }

    private void initRadialGradient() {
        float f2 = this.mLightShaderRadius;
        Shader.TileMode tileMode = Shader.TileMode.CLAMP;
        this.mGradient = new RadialGradient(0.0f, 0.0f, f2, new int[]{0, this.mOuterGradientColor1, this.mOuterGradientColor2, this.mOuterGradientColor3, 0}, new float[]{0.0f, 0.3f, 0.6f, 0.8f, 1.0f}, tileMode);
        this.mGradient2 = new RadialGradient(0.0f, 0.0f, this.mNumberBackgroundRadius, new int[]{0, this.mInnerGradientColor1, this.mInnerGradientColor2}, new float[]{0.0f, GRADIENT_INNER_STOP_1, 1.0f}, tileMode);
    }

    private void initShowAnimator(final Cell cell) {
        if (cell == null) {
            return;
        }
        if (cell.showAnimator == null) {
            ValueAnimator valueAnimatorOfPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat("scaleHolder", 1.0f, 2.15f), PropertyValuesHolder.ofFloat("alphaHolder", 0.0f, this.mCircleMaxAlpha));
            valueAnimatorOfPropertyValuesHolder.setDuration(100L);
            valueAnimatorOfPropertyValuesHolder.setInterpolator(DEFAULT_OUT_EASE_INTERPOLATOR);
            valueAnimatorOfPropertyValuesHolder.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.coui.appcompat.lockview.COUINumericKeyboard.1
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    Cell cell2 = cell;
                    if (cell2 == null) {
                        return;
                    }
                    cell2.normalAlpha = ((Float) valueAnimator.getAnimatedValue("alphaHolder")).floatValue();
                    cell.normalScale = ((Float) valueAnimator.getAnimatedValue("scaleHolder")).floatValue();
                    COUINumericKeyboard.this.invalidate();
                }
            });
            cell.showAnimator = valueAnimatorOfPropertyValuesHolder;
        }
        cell.showAnimator.removeAllListeners();
        if (cell.showAnimator.isRunning()) {
            cell.showAnimator.end();
        }
        ValueAnimator valueAnimator = cell.fadeAnimator;
        if (valueAnimator != null && valueAnimator.isRunning()) {
            cell.fadeAnimator.end();
        }
        cell.showAnimator.start();
    }

    private void initSideAnim(SideStyle sideStyle, List<Animator> list, int i2) {
        if (isEmptyStyle(sideStyle)) {
            return;
        }
        if (sideStyle.mDrawable != null) {
            setDrawableAlpha(0.0f);
            setDrawableTranslateY(this.mMaxTranslateY);
            ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this, "drawableAlpha", 0.0f, 1.0f);
            long j2 = ((long) i2) * 16;
            objectAnimatorOfFloat.setStartDelay(166 + j2);
            objectAnimatorOfFloat.setDuration(167L);
            objectAnimatorOfFloat.setInterpolator(this.mAlphaInterpolator);
            list.add(objectAnimatorOfFloat);
            ObjectAnimator objectAnimatorOfInt = ObjectAnimator.ofInt(this, "drawableTranslateY", this.mMaxTranslateY, 0);
            objectAnimatorOfInt.setStartDelay(j2);
            objectAnimatorOfInt.setDuration(500L);
            objectAnimatorOfInt.setInterpolator(this.mTranslateYInterpolator);
            list.add(objectAnimatorOfInt);
            return;
        }
        if (TextUtils.isEmpty(sideStyle.mText)) {
            return;
        }
        setTextAlpha(0.0f);
        setTextTranslateY(this.mMaxTranslateY);
        ObjectAnimator objectAnimatorOfFloat2 = ObjectAnimator.ofFloat(this, "textAlpha", 0.0f, 1.0f);
        long j3 = ((long) i2) * 16;
        objectAnimatorOfFloat2.setStartDelay(166 + j3);
        objectAnimatorOfFloat2.setDuration(167L);
        objectAnimatorOfFloat2.setInterpolator(this.mAlphaInterpolator);
        list.add(objectAnimatorOfFloat2);
        ObjectAnimator objectAnimatorOfInt2 = ObjectAnimator.ofInt(this, "textTranslateY", this.mMaxTranslateY, 0);
        objectAnimatorOfInt2.setStartDelay(j3);
        objectAnimatorOfInt2.setDuration(500L);
        objectAnimatorOfInt2.setInterpolator(this.mTranslateYInterpolator);
        list.add(objectAnimatorOfInt2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void invalidatePaths(Cell cell) {
        Cell cell2;
        int i2;
        if (cell == null) {
            return;
        }
        for (int i3 = 0; i3 < 4; i3++) {
            for (int i4 = 0; i4 < 3; i4++) {
                int i5 = cell.row;
                if ((i3 != i5 || i4 != cell.column) && (i2 = (cell2 = this.sCells[i3][i4]).row) >= i5 - 1) {
                    int i6 = cell2.column;
                    int i7 = cell.column;
                    if (i6 >= i7 - 1 && i2 <= i5 + 1 && i6 <= i7 + 1) {
                        cell2.mInvalidatePaths = true;
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isEmptyStyle(SideStyle sideStyle) {
        return sideStyle == null || (sideStyle.mDrawable == null && TextUtils.isEmpty(sideStyle.mText)) || sideStyle.mAlpha == 0.0f;
    }

    private boolean isMultiPointerEvent(MotionEvent motionEvent) {
        return motionEvent.getPointerId(motionEvent.getActionIndex()) > 0;
    }

    private boolean isValidKeyCode(int i2) {
        return (i2 >= 7 && i2 <= 16) || (i2 >= 144 && i2 <= 153) || i2 == 67 || i2 == 66 || i2 == FADE_ANIMATOR_TIME;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$ensureRightStyleAnimator$0(SideStyle sideStyle, COUIDynamicAnimation cOUIDynamicAnimation, float f2, float f3) {
        sideStyle.mAlpha = f2;
        invalidate();
    }

    private boolean needFadeWhenDisabled(int i2) {
        return this.mNormalAlpha > 0.0f && (1 == i2 || 3 == i2 || i2 == 0);
    }

    private void setTouchFeedback() {
        if (this.mIsLinearMotorVersion) {
            performHapticFeedback(302);
        } else {
            performHapticFeedback(301);
        }
    }

    private void setTouchSoundFeedBack() {
        playSoundEffect(0);
    }

    private void showSideStyle(SideStyle sideStyle, boolean z2) {
        if (sideStyle == null) {
            return;
        }
        ensureRightStyleAnimator(sideStyle);
        sideStyle.mSideStyleAlphaAnimator.animateToFinalPosition(z2 ? 255.0f : 0.0f);
        sideStyle.mIsDisappearing = !z2;
    }

    private void updateNumberTextTypeface() {
        int[] statusAndVariation = getStatusAndVariation();
        if (statusAndVariation == null) {
            return;
        }
        // Prefer SysSans when present (OPPO). Without it, wght on Roboto looks too bold.
        final java.io.File fontFile = new java.io.File(this.mTtfPath);
        if (fontFile.isFile()) {
            final Typeface typeface = getTypeface(statusAndVariation);
            if (typeface != null) {
                this.mNumberTextPaint.setTypeface(typeface);
            }
            this.mNumberTextPaint.setFontVariationSettings(
                    "'wght' " + (statusAndVariation[1] + this.mFontVariationDefaultPlus));
        } else {
            this.mNumberTextPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        }
        this.mNumberTextFontMetrics = this.mNumberTextPaint.getFontMetrics();
        invalidate();
    }

    @Override // android.view.View
    public boolean dispatchHoverEvent(MotionEvent motionEvent) {
        return this.mExploreByTouchHelper.dispatchHoverEvent(motionEvent) | super.dispatchHoverEvent(motionEvent);
    }

    public AnimatorSet getEnterAnim() {
        AnimatorSet animatorSet = new AnimatorSet();
        ArrayList arrayList = new ArrayList();
        for (int i2 = 0; i2 < 4; i2++) {
            for (int i3 = 0; i3 < 3; i3++) {
                Cell cellOf = of(i2, i3);
                int i4 = (i2 * 3) + i3;
                if (i4 == 9) {
                    initSideAnim(this.mLeftStyle, arrayList, i4);
                } else if (i4 == 11) {
                    SideStyle sideStyle = this.mRightStyle;
                    if (isEmptyStyle(this.mLeftStyle)) {
                        i4--;
                    }
                    initSideAnim(sideStyle, arrayList, i4);
                } else {
                    initCellAnim(cellOf, arrayList, i4);
                }
            }
        }
        animatorSet.playTogether(arrayList);
        return animatorSet;
    }

    public int[] getStatusAndVariation() {
        int i2 = Settings.System.getInt(this.mContext.getContentResolver(), "font_variation_settings", 550);
        int[] iArr = {(61440 & i2) >> 12, i2 & 4095};
        int i3 = this.mPreVariation;
        int i4 = iArr[1];
        if (i3 == i4) {
            return null;
        }
        this.mPreVariation = i4;
        return iArr;
    }

    @Deprecated
    public int getTouchIndex() {
        return 0;
    }

    public boolean isTactileFeedbackEnabled() {
        return this.mEnableHapticFeedback;
    }

    public synchronized Cell of(int i2, int i3) {
        checkRange(i2, i3);
        return this.sCells[i2][i3];
    }

    @Override // android.view.View
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (this.mHasCustomTypeface) {
            return;
        }
        updateNumberTextTypeface();
    }

    @Override // android.view.View
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mPaint != null) {
            this.mPaint = null;
        }
        if (this.mTouchCell != null) {
            this.mTouchCell = null;
        }
        this.mDownState = false;
    }

    @Override // android.view.View
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int i2 = this.mPressEffectStyle;
        if (i2 == 0) {
            for (int i3 = 0; i3 < 4; i3++) {
                for (int i4 = 0; i4 < 3; i4++) {
                    drawPressCircle(canvas, i4, i3);
                    drawCell(canvas, i4, i3);
                }
            }
            return;
        }
        if (i2 != 1) {
            return;
        }
        for (int i5 = 0; i5 < 4; i5++) {
            for (int i6 = 0; i6 < 3; i6++) {
                drawCell(canvas, i6, i5);
            }
        }
        for (int i7 = 0; i7 < 4; i7++) {
            for (int i8 = 0; i8 < 3; i8++) {
                drawLightEffect(canvas, i8, i7);
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

    @Override // android.view.View, android.view.KeyEvent.Callback
    public boolean onKeyDown(int i2, KeyEvent keyEvent) {
        if (keyEvent.getRepeatCount() == 0) {
            handleKeyEvent(i2, true);
        }
        return super.onKeyDown(i2, keyEvent);
    }

    @Override // android.view.View, android.view.KeyEvent.Callback
    public boolean onKeyUp(int i2, KeyEvent keyEvent) {
        if (keyEvent.getScanCode() != 0) {
            handleKeyEvent(i2, false);
        }
        return super.onKeyUp(i2, keyEvent);
    }

    @Override // android.view.View
    public void onMeasure(int i2, int i3) {
        int mode = View.MeasureSpec.getMode(i2);
        int size = View.MeasureSpec.getSize(i2);
        int mode2 = View.MeasureSpec.getMode(i3);
        int size2 = View.MeasureSpec.getSize(i3);
        if (mode == View.MeasureSpec.AT_MOST || mode == View.MeasureSpec.UNSPECIFIED) {
            size = this.mDefaultWidth;
        }
        if (mode2 == View.MeasureSpec.AT_MOST || mode2 == View.MeasureSpec.UNSPECIFIED) {
            size2 = this.mDefaultHeight;
        }
        setMeasuredDimension(size, size2);
    }

    @Override // android.view.View
    public void onSizeChanged(int i2, int i3, int i4, int i5) {
        int i6 = this.mViewSize;
        this.mCellWidth = i6;
        this.mCellHeight = i6;
        this.mNumberBackgroundRadius = i6 / 2;
        this.mHorizontalSpacing = (((getWidth() - getPaddingLeft()) - getPaddingRight()) - (this.mCellWidth * 3)) / 2;
        int height = (getHeight() - getPaddingTop()) - getPaddingBottom();
        int i7 = this.mCellHeight;
        this.mVerticalSpacing = (height - (i7 * 4)) / 3;
        this.mCircleRadius = i7 / 2;
        if (this.mPressEffectStyle == 1) {
            initRadialGradient();
            initInnerShadowBitmap();
        }
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent event) {
        final int actionIndex = event.getActionIndex();
        final int actionMasked = event.getActionMasked();
        if (!isEnabled()) {
            if (needFadeWhenDisabled(actionMasked)) {
                final int pointerCount = event.getPointerCount();
                for (int i = 0; i < pointerCount; i++) {
                    handleActionCancel(event.getPointerId(i));
                }
            }
            return false;
        }
        final Cell hit = checkForNewHit(event.getX(actionIndex), event.getY(actionIndex));
        final int touchIndex = getTouchIndex(hit);
        if (touchIndex == 9
                && this.mLeftStyle != null
                && this.mLeftStyle.mSideStyleAlphaAnimator != null
                && this.mLeftStyle.mSideStyleAlphaAnimator.isRunning()
                && this.mLeftStyle.mIsDisappearing) {
            return false;
        }
        if (touchIndex == 11
                && this.mRightStyle != null
                && this.mRightStyle.mSideStyleAlphaAnimator != null
                && this.mRightStyle.mSideStyleAlphaAnimator.isRunning()
                && this.mRightStyle.mIsDisappearing) {
            return false;
        }
        if (touchIndex != -1 && hit != null && hit.cellNumberAlpha < 1.0f) {
            return false;
        }
        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                this.mDownState = true;
                handleActionDown(event, actionIndex);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                this.mDownState = false;
                handleActionUp(event, actionIndex);
                break;
            case MotionEvent.ACTION_MOVE:
                final int moveCount = event.getPointerCount();
                for (int i = 0; i < moveCount; i++) {
                    handleActionMove(event, event.getPointerId(i));
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                final int cancelCount = event.getPointerCount();
                for (int i = 0; i < cancelCount; i++) {
                    handleActionCancel(event.getPointerId(i));
                }
                this.mDownState = false;
                break;
            default:
                break;
        }
        return true;
    }

    public void refresh() {
        String resourceTypeName = getResources().getResourceTypeName(this.mStyle);
        TypedArray typedArrayObtainStyledAttributes = null;
        if ("attr".equals(resourceTypeName)) {
            typedArrayObtainStyledAttributes = this.mContext.obtainStyledAttributes(null, R.styleable.COUINumericKeyboard, this.mStyle, 0);
        } else if ("style".equals(resourceTypeName)) {
            typedArrayObtainStyledAttributes = this.mContext.obtainStyledAttributes(null, R.styleable.COUINumericKeyboard, 0, this.mStyle);
        }
        if (typedArrayObtainStyledAttributes != null) {
            this.mPressedColor = typedArrayObtainStyledAttributes.getColor(R.styleable.COUINumericKeyboard_couiNumPressColor, 0);
            this.mKeyboardNumberTextColor = typedArrayObtainStyledAttributes.getColor(R.styleable.COUINumericKeyboard_couiNumberColor, 0);
            this.mKeyboardLineColor = typedArrayObtainStyledAttributes.getColor(R.styleable.COUINumericKeyboard_couiLineColor, 0);
            this.mCircleMaxAlpha = typedArrayObtainStyledAttributes.getFloat(R.styleable.COUINumericKeyboard_couiCircleMaxAlpha, 0.0f);
            this.mNumberBackgroundColor = typedArrayObtainStyledAttributes.getColor(R.styleable.COUINumericKeyboard_couiNumberBackgroundColor, 0);
            this.mSideBackgroundColor = typedArrayObtainStyledAttributes.getColor(R.styleable.COUINumericKeyboard_couiSideBackgroundColor, 0);
            typedArrayObtainStyledAttributes.recycle();
        }
        this.mKeyboardDelete.setTint(this.mKeyboardNumberTextColor);
        initPaint();
    }

    public void setCellViewSize(int i2) {
        this.mViewSize = i2;
    }

    public void setCircleMaxAlpha(int i2) {
        setCircleMaxAlpha(i2 / 255.0f);
    }

    public void setCustomTypeFace(Typeface typeface) {
        if (this.mHasCustomTypeface) {
            this.mCustomTypeface = typeface;
            this.mNumberTextPaint.setTypeface(typeface);
            invalidate();
        }
    }

    public void setDeleteStyle(Drawable drawable) {
        this.mDeleteStyle = new SideStyle.Builder().drawable(drawable).description(getResources().getString(R.string.coui_number_keyboard_delete)).type(1).build();
    }

    public void setDrawableAlpha(float f2) {
        this.mDrawableAlpha = f2;
        invalidate();
    }

    public void setDrawableTranslateX(int i2) {
        this.mDrawableTranslateX = i2;
        invalidate();
    }

    public void setDrawableTranslateY(int i2) {
        this.mDrawableTranslateY = i2;
        invalidate();
    }

    @Override // android.view.View
    public void setEnabled(boolean z2) {
        Paint paint;
        if (!z2 && this.mDownState && (paint = this.mPaint) != null) {
            paint.setAlpha(0);
            this.mDownState = false;
            invalidate();
        }
        super.setEnabled(z2);
    }

    @Deprecated
    public void setHasFinishButton(boolean z2) {
    }

    @Deprecated
    public void setItemTouchListener(OnItemTouchListener onItemTouchListener) {
    }

    public void setKeyboardDrawDelegate(KeyboardDrawDelegate keyboardDrawDelegate) {
        this.mDrawDelegate = keyboardDrawDelegate;
    }

    public void setKeyboardLineColor(int i2) {
        this.mKeyboardLineColor = i2;
        initPaint();
    }

    public void setKeyboardNumberTextColor(int i2) {
        this.mKeyboardNumberTextColor = i2;
        this.mKeyboardDelete.setTint(i2);
    }

    public void setLeftStyle(SideStyle sideStyle) {
        this.mLeftStyle = sideStyle;
        this.mExploreByTouchHelper.invalidateVirtualView(9);
        if (sideStyle != null) {
            sideStyle.mAlpha = 255.0f;
        }
        invalidate();
    }

    public void setNumberBackgroundColor(int i2) {
        this.mNumberBackgroundColor = i2;
    }

    public void setNumberOffsetY(float f2) {
        if (this.mNumberOffsetY != f2) {
            this.mNumberOffsetY = f2;
            invalidate();
        }
    }

    public void setOnClickItemListener(OnClickItemListener onClickItemListener) {
        this.mOnClickItemListener = onClickItemListener;
    }

    public void setPressedColor(int i2) {
        this.mPressedColor = i2;
        initPaint();
    }

    public void setRightStyle(SideStyle sideStyle) {
        this.mRightStyle = sideStyle;
        this.mExploreByTouchHelper.invalidateVirtualView(11);
        if (sideStyle != null) {
            sideStyle.mAlpha = 255.0f;
        }
        invalidate();
    }

    public void setSideBackgroundColor(int i2) {
        this.mSideBackgroundColor = i2;
    }

    public void setTactileFeedbackEnabled(boolean z2) {
        this.mEnableHapticFeedback = z2;
    }

    public void setTextAlpha(float f2) {
        this.mTextAlpha = f2;
        invalidate();
    }

    public void setTextTranslateX(int i2) {
        this.mTextTranslateX = i2;
        invalidate();
    }

    public void setTextTranslateY(int i2) {
        this.mTextTranslateY = i2;
        invalidate();
    }

    @Deprecated
    public void setTouchTextListener(OnTouchTextListener onTouchTextListener) {
    }

    @Deprecated
    public void setTouchUpListener(OnTouchUpListener onTouchUpListener) {
    }

    @Deprecated
    public void setType(int i2) {
    }

    public void setWordTextNormalColor(int i2) {
        this.mFinishStyle.mTextColor = i2;
    }

    public COUINumericKeyboard(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R.attr.couiNumericKeyboardStyle);
    }

    private int getTouchIndex(Cell cell) {
        if (cell == null) {
            return -1;
        }
        int row = (cell.getRow() * 3) + cell.getColumn();
        if (row == 9 && isEmptyStyle(this.mLeftStyle)) {
            row = -1;
        }
        if (row == 11 && isEmptyStyle(this.mRightStyle)) {
            return -1;
        }
        return row;
    }

    private void handleActionDown(float f2, float f3, int i2) {
        if (this.mAccessibilityManagerService.isTouchExplorationEnabled()) {
            return;
        }
        Cell cellCheckForNewHit = checkForNewHit(f2, f3);
        if (cellCheckForNewHit != null) {
            int touchIndex = getTouchIndex(cellCheckForNewHit);
            this.mExploreByTouchHelper.invalidateRoot();
            if (this.mEnableHapticFeedback && touchIndex != -1) {
                setTouchFeedback();
            }
            if (i2 != -1) {
                cellCheckForNewHit.pointerId = i2;
            }
            int i3 = this.mPressEffectStyle;
            if (i3 == 0) {
                initShowAnimator(cellCheckForNewHit);
            } else if (i3 == 1) {
                executeLightEffectAnimator(cellCheckForNewHit, true);
            }
        }
        invalidate();
    }

    private void handleActionUp(float f2, float f3, int i2) {
        int i3;
        Cell cellCheckForNewHit = checkForNewHit(f2, f3);
        int touchIndex = getTouchIndex(cellCheckForNewHit);
        if (this.mAccessibilityManagerService.isTouchExplorationEnabled()) {
            if (cellCheckForNewHit == null || (i3 = cellCheckForNewHit.pointerId) == -1 || i3 != i2) {
                return;
            }
            this.mExploreByTouchHelper.invalidateRoot();
            if (!this.mEnableHapticFeedback || touchIndex == -1) {
                return;
            }
            setTouchFeedback();
            return;
        }
        if (cellCheckForNewHit != null && cellCheckForNewHit.pointerId == i2) {
            callback(touchIndex);
        }
        if (i2 != -1 && (cellCheckForNewHit == null || cellCheckForNewHit.pointerId != i2)) {
            cellCheckForNewHit = findCellByPointerId(i2);
        }
        int i4 = this.mPressEffectStyle;
        if (i4 == 0) {
            initFadeAnimator(cellCheckForNewHit);
        } else if (i4 == 1) {
            executeLightEffectAnimator(cellCheckForNewHit, false);
        }
        if (touchIndex != -1 && isEnabled() && !hasOnClickListeners()) {
            setTouchSoundFeedBack();
        }
        invalidate();
    }

    public void setCircleMaxAlpha(float f2) {
        if (f2 < 0.0f || f2 > 1.0f) {
            COUILog.e(TAG, "The alpha value must be greater than or equal to 0 and less than or equal to 1");
            return;
        }
        this.mCircleMaxAlpha = f2;
        for (int i2 = 0; i2 < 4; i2++) {
            for (int i3 = 0; i3 < 3; i3++) {
                Cell cell = this.sCells[i2][i3];
                if (cell != null) {
                    ValueAnimator valueAnimator = cell.showAnimator;
                    if (valueAnimator != null && !valueAnimator.isRunning()) {
                        this.sCells[i2][i3].showAnimator = null;
                    }
                    ValueAnimator valueAnimator2 = this.sCells[i2][i3].fadeAnimator;
                    if (valueAnimator2 != null && !valueAnimator2.isRunning()) {
                        this.sCells[i2][i3].fadeAnimator = null;
                    }
                    ValueAnimator valueAnimator3 = this.sCells[i2][i3].blurFadeAnimator;
                    if (valueAnimator3 != null && !valueAnimator3.isRunning()) {
                        this.sCells[i2][i3].blurFadeAnimator = null;
                    }
                }
            }
        }
    }

    public COUINumericKeyboard(Context context, AttributeSet attributeSet, int i2) {
        this(context, attributeSet, i2, R.style.Widget_COUI_COUINumericKeyboard);
    }

    private void handleActionMove(float f2, float f3, int i2) {
        Cell cellCheckForNewHit = checkForNewHit(f2, f3);
        if (i2 != -1) {
            if (cellCheckForNewHit == null || cellCheckForNewHit.pointerId != i2) {
                handleActionCancel(i2);
            }
        }
    }

    public COUINumericKeyboard(Context context, AttributeSet attributeSet, int i2, int i3) {
        super(context, attributeSet, i2, i3);
        this.mClipPaint = new Paint(1);
        this.mNumberBounds = new RectF();
        this.mTranslateBounds = new RectF();
        this.mButtonPath = new Path();
        this.NUMERIC = 1;
        this.WORD = 2;
        this.mPaint = null;
        this.mTouchCell = null;
        this.mDrawDelegate = null;
        this.mEnableHapticFeedback = true;
        int i4 = 0;
        this.mDownState = false;
        this.sCells = (Cell[][]) Array.newInstance((Class<?>) Cell.class, 4, 3);
        this.mKeyboardDelete = null;
        this.mNumberBackgroundAlpha = 255;
        this.mKeyboardNumbers = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, -1, 0, -1};
        this.mNumberTextPaint = new TextPaint();
        this.mNumberTextFontMetrics = null;
        this.mWordTextFontMetrics = null;
        this.mLinePaint = new Paint();
        this.mKeyboardNumberTextSize = -1.0f;
        this.mKeyboardNumberTextColor = -1;
        this.mKeyboardNumberTextAlpha = 255;
        this.mKeyboardLineColor = -1;
        this.mBorderLineColor = 0;
        this.mBorderLineAlpha = 0;
        this.mBorderLineHighLightAlpha = 0;
        this.mBorderLineHighLightColor = 0;
        this.mWordTextPaint = new TextPaint();
        this.mNormalAlpha = 0.12f;
        this.mPreVariation = -1;
        this.mDrawableAlpha = 1.0f;
        this.mTextAlpha = 1.0f;
        this.mAlphaInterpolator = new COUIEaseInterpolator();
        this.mTranslateYInterpolator = new COUIInEaseInterpolator();
        this.mLightShaderRadius = 0;
        this.mPressEffectStyle = 0;
        if (attributeSet != null && attributeSet.getStyleAttribute() != 0) {
            this.mStyle = attributeSet.getStyleAttribute();
        } else {
            this.mStyle = i2;
        }
        COUIDarkModeUtil.setForceDarkAllow(this, false);
        this.mContext = context;
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.COUINumericKeyboard, i2, i3);
        this.mPressedColor = typedArrayObtainStyledAttributes.getColor(R.styleable.COUINumericKeyboard_couiNumPressColor, 0);
        Resources resources = context.getResources();
        this.mLightShaderRadius = resources.getDimensionPixelOffset(R.dimen.coui_numeric_light_shader_radius);
        this.mDefaultWidth = resources.getDimensionPixelSize(R.dimen.coui_numeric_keyboard_view_width);
        this.mDefaultHeight = resources.getDimensionPixelSize(R.dimen.coui_numeric_keyboard_view_height);
        this.mViewSize = resources.getDimensionPixelSize(R.dimen.coui_numeric_keyboard_view_size);
        this.mAdditionalPressableArea = resources.getDimensionPixelOffset(R.dimen.coui_additional_pressable_area);
        this.mKeyboardNumberTextSize = typedArrayObtainStyledAttributes.getDimensionPixelSize(R.styleable.COUINumericKeyboard_couiNumberTextSize, resources.getDimensionPixelSize(R.dimen.number_keyboard_number_size));
        this.mMaxTranslateY = resources.getDimensionPixelSize(R.dimen.coui_numeric_keyboard_max_translate_y);
        this.mFontVariationDefaultPlus = resources.getInteger(R.integer.font_variation_default_plus);
        this.mNumberOffsetY = typedArrayObtainStyledAttributes.getDimension(R.styleable.COUINumericKeyboard_couiNumberOffsetY, resources.getDimensionPixelOffset(R.dimen.coui_numeric_keyboard_number_offset_y));
        this.mKeyboardNumberTextColor = typedArrayObtainStyledAttributes.getColor(R.styleable.COUINumericKeyboard_couiNumberColor, 0);
        this.mKeyboardLineColor = typedArrayObtainStyledAttributes.getColor(R.styleable.COUINumericKeyboard_couiLineColor, 0);
        int color = typedArrayObtainStyledAttributes.getColor(R.styleable.COUINumericKeyboard_couiWordTextNormalColor, 0);
        this.mCircleMaxAlpha = typedArrayObtainStyledAttributes.getFloat(R.styleable.COUINumericKeyboard_couiCircleMaxAlpha, 0.0f);
        this.mNumberBackgroundColor = typedArrayObtainStyledAttributes.getColor(R.styleable.COUINumericKeyboard_couiNumberBackgroundColor, 0);
        this.mSideBackgroundColor = typedArrayObtainStyledAttributes.getColor(R.styleable.COUINumericKeyboard_couiSideBackgroundColor, 0);
        this.mKeyboardDelete = typedArrayObtainStyledAttributes.getDrawable(R.styleable.COUINumericKeyboard_couiKeyboardDelete);
        this.mHasCustomTypeface = typedArrayObtainStyledAttributes.getBoolean(R.styleable.COUINumericKeyboard_couiSetCustomTypeface, false);
        this.mPressEffectStyle = typedArrayObtainStyledAttributes.getInt(R.styleable.COUINumericKeyboard_couiPressEffect, 0);
        if (!UIUtil.confirmLevelAnim(UIUtil.ANIM_LEVEL_SUPPORT_BLUR_MIN)) {
            this.mPressEffectStyle = 0;
        }
        typedArrayObtainStyledAttributes.recycle();
        this.mButtonBorderWidth = resources.getDimension(R.dimen.coui_keyboard_button_border_width);
        int color2 = resources.getColor(R.color.coui_numeric_keyboard_border_color);
        this.mBorderLineColor = color2;
        this.mBorderLineAlpha = Color.alpha(color2);
        int color3 = resources.getColor(R.color.coui_numeric_keyboard_border_highlight_color);
        this.mBorderLineHighLightColor = color3;
        this.mBorderLineHighLightAlpha = Color.alpha(color3);
        this.mUpperInnerShadowColor = resources.getColor(R.color.coui_numeric_keyboard_upper_inner_shadow_color);
        this.mLowerInnerShadowColor = resources.getColor(R.color.coui_numeric_keyboard_lower_inner_shadow_color);
        this.mOuterGradientColor1 = resources.getColor(R.color.coui_numeric_keyboard_outer_gradient_color_1);
        this.mOuterGradientColor2 = resources.getColor(R.color.coui_numeric_keyboard_outer_gradient_color_2);
        this.mOuterGradientColor3 = resources.getColor(R.color.coui_numeric_keyboard_outer_gradient_color_3);
        this.mInnerGradientColor1 = resources.getColor(R.color.coui_numeric_keyboard_inner_gradient_color_1);
        this.mInnerGradientColor2 = resources.getColor(R.color.coui_numeric_keyboard_inner_gradient_color_2);
        if (this.mKeyboardDelete == null) {
            this.mKeyboardDelete = context.getDrawable(R.drawable.ic_coui_number_keyboard_launhcer_delete);
        }
        PatternExploreByTouchHelper patternExploreByTouchHelper = new PatternExploreByTouchHelper(this);
        this.mExploreByTouchHelper = patternExploreByTouchHelper;
        ViewCompat.setAccessibilityDelegate(this, patternExploreByTouchHelper);
        setImportantForAccessibility(1);
        this.mExploreByTouchHelper.invalidateRoot();
        String[] stringArray = context.getResources().getStringArray(R.array.coui_number_keyboard_letters);
        this.mIsLinearMotorVersion = VibrateUtils.isLinearMotorVersion(context);
        GradientDrawable gradientDrawable = new GradientDrawable();
        this.mNumberBackground = gradientDrawable;
        gradientDrawable.setShape(1);
        this.mNumberBackground.setCornerRadius(this.mNumberBackgroundRadius);
        int i5 = 0;
        while (i5 < 4) {
            int i6 = i4;
            while (i6 < 3) {
                this.sCells[i5][i6] = new Cell(i5, i6);
                Cell cell = this.sCells[i5][i6];
                int i7 = (i5 * 3) + i6;
                cell.cellLettersStr = stringArray[i7];
                int i8 = this.mKeyboardNumbers[i7];
                if (i8 > -1) {
                    cell.cellNumberStr = String.format(Locale.getDefault(), "%d", Integer.valueOf(i8));
                }
                i6++;
            }
            i5++;
            i4 = 0;
        }
        this.mTtfPath = getResources().getString(R.string.ttf_path);
        String string = getResources().getString(R.string.coui_numeric_keyboard_sure);
        this.mFinishStyle = new SideStyle.Builder().text(string).textColor(color).textSize(resources.getDimensionPixelSize(R.dimen.coui_number_keyboard_finish_text_size)).description(string).type(2).build();
        this.mKeyboardDelete.setTint(this.mKeyboardNumberTextColor);
        this.mDeleteStyle = new SideStyle.Builder().drawable(this.mKeyboardDelete).description(getResources().getString(R.string.coui_number_keyboard_delete)).type(1).build();
        this.mAccessibilityManagerService = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        initPaint();
        this.mInnerShadowMatrix = new Matrix();
        this.mBorderLinePaint = new Paint();
    }

    public void setRightStyle(SideStyle sideStyle, boolean z2) {
        if (!z2) {
            setRightStyle(sideStyle);
            return;
        }
        if (sideStyle != null) {
            this.mRightStyle = sideStyle;
            showSideStyle(sideStyle, true);
        } else {
            showSideStyle(this.mRightStyle, false);
        }
        this.mExploreByTouchHelper.invalidateVirtualView(11);
        invalidate();
    }
}
