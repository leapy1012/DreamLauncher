package com.coui.appcompat.lockview;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.PathInterpolator;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.customview.widget.ExploreByTouchHelper;
import com.coui.appcompat.animation.COUIEaseInterpolator;
import com.coui.appcompat.animation.COUILinearInterpolator;
import com.coui.appcompat.contextutil.COUIContextUtil;
import com.coui.appcompat.darkmode.COUIDarkModeUtil;
import com.coui.appcompat.vibrateutil.VibrateUtils;
import com.coui.appcompat.R;
import java.util.LinkedList;
import java.util.List;

/* JADX INFO: loaded from: classes11.dex */
public class COUISimpleLock extends View {
    public static final int DEFAULTTYPE = 0;
    private static final int FOURCIRCLE = 4;
    private static final int FOURINTERVAL = 3;
    private static final int SIXCIRCLE = 6;
    public static final int SIXCIRCLETYPE = 1;
    private static final int SIXINTERVAL = 5;
    private static final String TAG = "COUISimpleLock";
    private final int ADD_ANIMATION;
    private final int CLEAR_ALL_ANIMATION;
    private final float[] DELAY_FOUR;
    private final float[] DELAY_SIX;
    private final int DELETE_ANIMATION;
    private final int DRAW_ALL_ANIMATION;
    private final int FAILED_ANIMATION;
    private final int FALL_HEIGHT;
    private final int FALL_SPEED;
    private final int MORPHING_FILLED_TO_OUTLINED_TIME;
    private final int MORPHING_OUTLINED_TO_FILLED_TIME;
    private final int SHAKE_AND_FALL_TIME;
    private final float[] SHAKE_X_POINT;
    private int animationMode;
    private boolean fail_lastDraw;
    private boolean fto_lastDraw;
    private boolean isFingerprintMode;
    private ValueAnimator mAddAnimator;
    private int mCodeImageStart;
    public int mCodeNumber;
    private int mContentHeight;
    private int mContentWidth;
    private Context mContext;
    private String mDecription;
    private ValueAnimator mDeleteAnimator;
    private PathInterpolator mDeleteAnimatorInterpolator;
    private boolean mDrawFailedAnimation;
    private Drawable mDrawable;
    private int mDrawableHeight;
    private int mDrawableWidth;
    private Animator mFailedAnimator;
    private Drawable mFilledRectangleDrawable;
    private boolean mIsLinearMotorVersion;
    private boolean mIsVibrator;
    private LinkedList<String> mNumberStrList;
    private int mOpacity;
    private Drawable mOutlinedRectangleDrawable;
    private int mRectangleNum;
    private int mRectanglePadding;
    private int mRectangleType;
    private int mRectanglesNumber;
    private int mRectanglesWidth;
    private float mScaleX;
    private float mScaleY;
    private int mStyle;
    private SimpleLockTouchHelper mTouchHelper;
    private float mTransitionX;
    private float mTransitionY;
    private boolean otf_lastDraw;

    public final class SimpleLockTouchHelper extends ExploreByTouchHelper {
        private Rect mTempRect;

        public SimpleLockTouchHelper(View view) {
            super(view);
            this.mTempRect = new Rect();
        }

        public CharSequence getItemDescription(int i2) {
            if (COUISimpleLock.this.mDecription == null || COUISimpleLock.this.mNumberStrList == null) {
                return SimpleLockTouchHelper.class.getSimpleName();
            }
            COUISimpleLock cOUISimpleLock = COUISimpleLock.this;
            cOUISimpleLock.mDecription = cOUISimpleLock.mDecription.replace('y', String.valueOf(COUISimpleLock.this.mRectangleNum).charAt(0));
            return COUISimpleLock.this.mDecription.replace('x', String.valueOf(COUISimpleLock.this.mNumberStrList.size()).charAt(0));
        }

        @Override // androidx.customview.widget.ExploreByTouchHelper
        public int getVirtualViewAt(float f2, float f3) {
            return (f2 < 0.0f || f2 > ((float) COUISimpleLock.this.mContentWidth) || f3 < 0.0f || f3 > ((float) COUISimpleLock.this.mDrawableHeight)) ? -2 : 0;
        }

        @Override // androidx.customview.widget.ExploreByTouchHelper
        public void getVisibleVirtualViews(List<Integer> list) {
            list.add(0);
        }

        public boolean onItemClicked(int i2) {
            sendEventForVirtualView(i2, 1);
            return false;
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
            accessibilityNodeInfoCompat.addAction(16);
            setRectBounds(i2, this.mTempRect);
            accessibilityNodeInfoCompat.setBoundsInParent(this.mTempRect);
        }

        public void setRectBounds(int i2, Rect rect) {
            if (i2 < 0 || i2 >= 1) {
                return;
            }
            rect.set(0, 0, COUISimpleLock.this.mContentWidth, COUISimpleLock.this.mDrawableHeight);
        }
    }

    public COUISimpleLock(Context context) {
        this(context, null);
    }

    private ValueAnimator createMorphingAnimationFilledToOutLined() {
        ValueAnimator valueAnimator = this.mDeleteAnimator;
        if (valueAnimator != null) {
            return valueAnimator;
        }
        ValueAnimator valueAnimatorOfInt = ValueAnimator.ofInt(255, 0);
        this.mDeleteAnimator = valueAnimatorOfInt;
        valueAnimatorOfInt.setInterpolator(this.mDeleteAnimatorInterpolator);
        this.mDeleteAnimator.setDuration(230L);
        this.mDeleteAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.coui.appcompat.lockview.COUISimpleLock.3
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator2) {
                COUISimpleLock.this.setOpacity(((Integer) valueAnimator2.getAnimatedValue()).intValue());
                COUISimpleLock.this.invalidate();
            }
        });
        this.mDeleteAnimator.addListener(new Animator.AnimatorListener() { // from class: com.coui.appcompat.lockview.COUISimpleLock.4
            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationCancel(Animator animator) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                COUISimpleLock.this.fto_lastDraw = true;
                COUISimpleLock.this.invalidate();
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationRepeat(Animator animator) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator animator) {
                COUISimpleLock.this.fto_lastDraw = false;
            }
        });
        return this.mDeleteAnimator;
    }

    private ValueAnimator createMorphingAnimationOutLinedToFilled() {
        ValueAnimator valueAnimator = this.mAddAnimator;
        if (valueAnimator != null) {
            return valueAnimator;
        }
        ValueAnimator valueAnimatorOfInt = ValueAnimator.ofInt(0, 255);
        this.mAddAnimator = valueAnimatorOfInt;
        valueAnimatorOfInt.setDuration(230L);
        this.mAddAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.coui.appcompat.lockview.COUISimpleLock.1
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator2) {
                COUISimpleLock.this.setOpacity(((Integer) valueAnimator2.getAnimatedValue()).intValue());
                COUISimpleLock.this.invalidate();
            }
        });
        this.mAddAnimator.addListener(new Animator.AnimatorListener() { // from class: com.coui.appcompat.lockview.COUISimpleLock.2
            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationCancel(Animator animator) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                COUISimpleLock.this.otf_lastDraw = true;
                COUISimpleLock.this.invalidate();
                if (COUISimpleLock.this.mDrawFailedAnimation) {
                    if (COUISimpleLock.this.mFailedAnimator != null && COUISimpleLock.this.mFailedAnimator.isRunning()) {
                        COUISimpleLock.this.otf_lastDraw = false;
                        return;
                    }
                    COUISimpleLock.this.animationMode = 5;
                    COUISimpleLock cOUISimpleLock = COUISimpleLock.this;
                    cOUISimpleLock.mFailedAnimator = cOUISimpleLock.createFailedAnimator();
                    COUISimpleLock.this.mFailedAnimator.start();
                    COUISimpleLock.this.mIsVibrator = true;
                }
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationRepeat(Animator animator) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator animator) {
                COUISimpleLock.this.otf_lastDraw = false;
            }
        });
        return this.mAddAnimator;
    }

    private void drawAllCodeAnimation(Canvas canvas, int i2) {
        int i3 = this.mCodeImageStart;
        int i4 = this.mDrawableHeight;
        if (this.otf_lastDraw) {
            drawPreviousState(canvas, this.mCodeNumber);
            this.animationMode = 0;
            return;
        }
        int iJudgeType = judgeType();
        int i5 = i3;
        for (int i6 = 0; i6 < iJudgeType; i6++) {
            int i7 = i5 + this.mDrawableWidth;
            drawOutLinedRectangle(canvas, i5, 0, i7, i4);
            if (i6 <= i2) {
                drawFilledRectangle(canvas, i5, 0, i7, i4);
            }
            if (i6 > i2) {
                drawFilledRectangle(canvas, 0, i5, i7, i4, this.mOpacity);
            }
            i5 = i7 + this.mRectanglePadding;
        }
    }

    private void drawClearAllAnimation(Canvas canvas, int i2) {
        int i3 = this.mCodeImageStart;
        int i4 = this.mDrawableHeight;
        if (this.fto_lastDraw) {
            drawPreviousState(canvas, this.mCodeNumber);
            this.animationMode = 0;
            return;
        }
        int iJudgeType = judgeType();
        int i5 = i3;
        for (int i6 = 0; i6 < iJudgeType; i6++) {
            int i7 = i5 + this.mDrawableWidth;
            drawOutLinedRectangle(canvas, i5, 0, i7, i4);
            if (i6 <= i2) {
                drawFilledRectangleWithAlphaChange(canvas, 0, i5, i7, i4, this.mOpacity);
            }
            i5 = i7 + this.mRectanglePadding;
        }
    }

    private void drawFailedAnimation(Canvas canvas, int i2) {
        int i3 = this.mCodeImageStart;
        int i4 = this.mDrawableHeight;
        if (this.fail_lastDraw) {
            this.animationMode = 0;
            this.mDrawFailedAnimation = false;
            this.mCodeNumber = -1;
            drawPreviousState(canvas, -1);
            return;
        }
        int iJudgeType = judgeType();
        int i5 = i3;
        for (int i6 = 0; i6 < iJudgeType; i6++) {
            int i7 = i5 + this.mDrawableWidth;
            drawOutLinedRectangleShake(canvas, 0, i5, i7, i4, 0.0f, 0.0f);
            if (i6 <= i2) {
                drawFilledRectangleShakeAndFall(canvas, 0, i5, i7, i4, 0.0f, 0.0f, i6);
            }
            i5 = i5 + this.mDrawableWidth + this.mRectanglePadding;
        }
    }

    private void drawFilledRectangle(Canvas canvas, int i2, int i3, int i4, int i5) {
        Drawable drawableNewDrawable = this.mFilledRectangleDrawable.getConstantState().newDrawable();
        this.mDrawable = drawableNewDrawable;
        float f2 = this.mTransitionX;
        drawableNewDrawable.setBounds((int) (i2 + f2), i3, (int) (i4 + f2), i5);
        this.mDrawable.draw(canvas);
    }

    private void drawFilledRectangleShakeAndFall(Canvas canvas, int i2, int i3, int i4, int i5, float f2, float f3, int i6) {
        this.mDrawable = this.mFilledRectangleDrawable.getConstantState().newDrawable();
        float f4 = this.mTransitionX;
        this.mDrawable.setBounds((int) (i3 + f4), (int) (i2 + getDelayFallHeight(i6, this.mTransitionY)), (int) (i4 + f4), (int) (i5 + getDelayFallHeight(i6, this.mTransitionY)));
        int delayFallHeight = (int) ((1.0f - (getDelayFallHeight(i6, this.mTransitionY) / 150.0f)) * 140.0f);
        Drawable drawable = this.mDrawable;
        if (delayFallHeight <= 0) {
            delayFallHeight = 0;
        }
        drawable.setAlpha(delayFallHeight);
        this.mDrawable.draw(canvas);
    }

    private void drawFilledRectangleWithAlphaChange(Canvas canvas, int i2, int i3, int i4, int i5, int i6) {
        Drawable drawableNewDrawable = this.mFilledRectangleDrawable.getConstantState().newDrawable();
        this.mDrawable = drawableNewDrawable;
        float f2 = this.mTransitionX;
        drawableNewDrawable.setBounds((int) (i3 + f2), i2, (int) (i4 + f2), i5);
        this.mDrawable.setAlpha(i6);
        this.mDrawable.draw(canvas);
    }

    private void drawFilledToOutLined(Canvas canvas, int i2) {
        int i3 = this.mCodeImageStart;
        int i4 = this.mDrawableHeight;
        if (this.fto_lastDraw) {
            this.animationMode = 0;
            drawPreviousState(canvas, this.mCodeNumber);
            return;
        }
        int iJudgeType = judgeType();
        int i5 = i3;
        for (int i6 = 0; i6 < iJudgeType; i6++) {
            int i7 = i5 + this.mDrawableWidth;
            drawOutLinedRectangle(canvas, i5, 0, i7, i4);
            if (i6 < i2) {
                drawFilledRectangle(canvas, i5, 0, i7, i4);
            }
            if (i6 == i2) {
                drawFilledRectangleWithAlphaChange(canvas, 0, i5, i7, i4, this.mOpacity);
            }
            i5 = i7 + this.mRectanglePadding;
        }
    }

    private void drawOutLinedRectangle(Canvas canvas, int i2, int i3, int i4, int i5) {
        Drawable drawableNewDrawable = this.mOutlinedRectangleDrawable.getConstantState().newDrawable();
        this.mDrawable = drawableNewDrawable;
        float f2 = this.mTransitionX;
        drawableNewDrawable.setBounds((int) (i2 + f2), i3, (int) (i4 + f2), i5);
        this.mDrawable.draw(canvas);
    }

    private void drawOutLinedRectangleShake(Canvas canvas, int i2, int i3, int i4, int i5, float f2, float f3) {
        Drawable drawableNewDrawable = this.mOutlinedRectangleDrawable.getConstantState().newDrawable();
        this.mDrawable = drawableNewDrawable;
        float f4 = this.mTransitionX;
        drawableNewDrawable.setBounds((int) (i3 + f4), i2, (int) (i4 + f4), i5);
        this.mDrawable.draw(canvas);
    }

    private void drawOutLinedToFilled(Canvas canvas, int i2) {
        int i3 = this.mCodeImageStart;
        int i4 = this.mDrawableHeight;
        if (this.otf_lastDraw) {
            this.animationMode = 0;
            drawPreviousState(canvas, this.mCodeNumber);
            return;
        }
        int iJudgeType = judgeType();
        int i5 = i3;
        for (int i6 = 0; i6 < iJudgeType; i6++) {
            int i7 = i5 + this.mDrawableWidth;
            drawOutLinedRectangle(canvas, i5, 0, i7, i4);
            if (i6 < i2) {
                drawFilledRectangle(canvas, i5, 0, i7, i4);
            }
            if (i6 == i2) {
                drawFilledRectangle(canvas, 0, i5, i7, i4, this.mOpacity);
            }
            if (this.mDrawFailedAnimation) {
                drawFilledRectangleShakeAndFall(canvas, 0, i5, i7, i4, 0.0f, 0.0f, i6);
            }
            i5 = i5 + this.mDrawableWidth + this.mRectanglePadding;
        }
    }

    private void drawPreviousState(Canvas canvas, int i2) {
        int i3 = this.mCodeImageStart;
        int i4 = this.mDrawableHeight;
        int iJudgeType = judgeType();
        int i5 = i3;
        for (int i6 = 0; i6 < iJudgeType; i6++) {
            int i7 = i5 + this.mDrawableWidth;
            if (i6 <= i2) {
                drawFilledRectangle(canvas, i5, 0, i7, i4);
            }
            if (i6 > i2) {
                drawOutLinedRectangle(canvas, i5, 0, i7, i4);
            }
            i5 = i7 + this.mRectanglePadding;
        }
    }

    private float getDelayFallHeight(int i2, float f2) {
        int i3 = this.mRectangleNum;
        if (i3 == 4) {
            float f3 = f2 - this.DELAY_FOUR[i2];
            if (f3 >= 0.0f) {
                return f3;
            }
            return 0.0f;
        }
        if (i3 != 6) {
            return f2;
        }
        float f4 = f2 - this.DELAY_SIX[i2];
        if (f4 >= 0.0f) {
            return f4;
        }
        return 0.0f;
    }

    private int judgeType() {
        int i2 = this.mRectangleNum;
        if (i2 == 4) {
            return 4;
        }
        return i2 == 6 ? 6 : -1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void performFeedback() {
        if (this.mIsLinearMotorVersion) {
            performHapticFeedback(304);
        } else {
            performHapticFeedback(300);
        }
    }

    public Animator createFailedAnimator() {
        Animator animator = this.mFailedAnimator;
        if (animator != null) {
            return animator;
        }
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 30.0f, -28.0f, 14.0f, -8.0f, 4.0f, -3.0f, 0.0f);
        valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.coui.appcompat.lockview.COUISimpleLock.5
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                COUISimpleLock.this.setInternalTranslationX(((Float) valueAnimator.getAnimatedValue()).floatValue());
                COUISimpleLock.this.invalidate();
            }
        });
        final ValueAnimator valueAnimatorOfFloat2 = ValueAnimator.ofFloat(0.0f, 250.0f);
        valueAnimatorOfFloat2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.coui.appcompat.lockview.COUISimpleLock.6
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                COUISimpleLock.this.setInternalTranslationY(((Float) valueAnimator.getAnimatedValue()).floatValue());
            }
        });
        valueAnimatorOfFloat.setInterpolator(new COUILinearInterpolator());
        valueAnimatorOfFloat2.setInterpolator(new COUILinearInterpolator());
        valueAnimatorOfFloat.setDuration(800L);
        valueAnimatorOfFloat2.setDuration(800L);
        valueAnimatorOfFloat.addListener(new Animator.AnimatorListener() { // from class: com.coui.appcompat.lockview.COUISimpleLock.7
            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationCancel(Animator animator2) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator2) {
                COUISimpleLock.this.setInternalTranslationX(0.0f);
                COUISimpleLock.this.fail_lastDraw = true;
                COUISimpleLock.this.mDrawFailedAnimation = false;
                COUISimpleLock.this.invalidate();
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationRepeat(Animator animator2) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator animator2) {
                COUISimpleLock.this.animationMode = 5;
                COUISimpleLock.this.setInternalTranslationX(0.0f);
                COUISimpleLock.this.fail_lastDraw = false;
                COUISimpleLock.this.mDrawFailedAnimation = true;
                valueAnimatorOfFloat2.start();
                if (COUISimpleLock.this.isFingerprintMode) {
                    COUISimpleLock.this.isFingerprintMode = false;
                } else if (COUISimpleLock.this.mIsVibrator) {
                    COUISimpleLock.this.performFeedback();
                    COUISimpleLock.this.mIsVibrator = false;
                }
            }
        });
        this.mFailedAnimator = valueAnimatorOfFloat;
        return valueAnimatorOfFloat;
    }

    @Override // android.view.View
    public boolean dispatchHoverEvent(MotionEvent motionEvent) {
        SimpleLockTouchHelper simpleLockTouchHelper = this.mTouchHelper;
        if (simpleLockTouchHelper == null || !simpleLockTouchHelper.dispatchHoverEvent(motionEvent)) {
            return super.dispatchHoverEvent(motionEvent);
        }
        return true;
    }

    public Animator getAddAnimator() {
        return createMorphingAnimationOutLinedToFilled();
    }

    public Animator getDeleteAnimator() {
        return createMorphingAnimationFilledToOutLined();
    }

    public Animator getFailedAnimator() {
        this.mIsVibrator = true;
        return createFailedAnimator();
    }

    public LinkedList<String> getNumberStrList() {
        return this.mNumberStrList;
    }

    @Override // android.view.View
    public void onDraw(Canvas canvas) {
        int i2 = this.animationMode;
        if (i2 == 1) {
            drawFilledToOutLined(canvas, this.mCodeNumber + 1);
            return;
        }
        if (i2 == 2) {
            drawOutLinedToFilled(canvas, this.mCodeNumber);
            return;
        }
        if (i2 == 3) {
            drawClearAllAnimation(canvas, this.mRectanglesNumber);
            return;
        }
        if (i2 == 4) {
            drawAllCodeAnimation(canvas, this.mRectanglesNumber);
        } else if (i2 != 5) {
            drawPreviousState(canvas, this.mCodeNumber);
        } else {
            drawFailedAnimation(canvas, this.mCodeNumber);
        }
    }

    @Override // android.view.View
    public void onMeasure(int i2, int i3) {
        int size = View.MeasureSpec.getSize(i2);
        this.mContentWidth = size;
        this.mCodeImageStart = (size - this.mRectanglesWidth) / 2;
        setMeasuredDimension(size, this.mDrawableHeight + 150);
    }

    public void refresh() {
        String resourceTypeName = getResources().getResourceTypeName(this.mStyle);
        TypedArray typedArrayObtainStyledAttributes = null;
        if ("attr".equals(resourceTypeName)) {
            typedArrayObtainStyledAttributes = this.mContext.obtainStyledAttributes(null, R.styleable.COUISimpleLock, this.mStyle, 0);
        } else if ("style".equals(resourceTypeName)) {
            typedArrayObtainStyledAttributes = this.mContext.obtainStyledAttributes(null, R.styleable.COUISimpleLock, 0, this.mStyle);
        }
        if (typedArrayObtainStyledAttributes != null) {
            this.mOutlinedRectangleDrawable = typedArrayObtainStyledAttributes.getDrawable(R.styleable.COUISimpleLock_couiOutLinedRectangleIconDrawable);
            this.mFilledRectangleDrawable = typedArrayObtainStyledAttributes.getDrawable(R.styleable.COUISimpleLock_couiFilledRectangleIconDrawable);
            typedArrayObtainStyledAttributes.recycle();
        }
    }

    public void reset() {
        ValueAnimator valueAnimator = this.mDeleteAnimator;
        if (valueAnimator != null && valueAnimator.isRunning()) {
            this.mDeleteAnimator.cancel();
        }
        ValueAnimator valueAnimator2 = this.mAddAnimator;
        if (valueAnimator2 != null && valueAnimator2.isRunning()) {
            this.mAddAnimator.cancel();
        }
        Animator animator = this.mFailedAnimator;
        if (animator != null && animator.isRunning()) {
            this.mFailedAnimator.cancel();
        }
        this.mCodeNumber = -1;
        this.animationMode = 0;
        this.mNumberStrList.clear();
        this.mDrawFailedAnimation = false;
        invalidate();
    }

    public void setAllCode(boolean z2) {
        int i2 = this.mRectangleNum;
        if (i2 == 4) {
            if (this.mDrawFailedAnimation || this.mCodeNumber >= 3) {
                return;
            }
            Animator animator = this.mFailedAnimator;
            if (animator != null && animator.isRunning()) {
                return;
            }
        } else if (i2 == 6) {
            if (this.mDrawFailedAnimation || this.mCodeNumber >= 5) {
                return;
            }
            Animator animator2 = this.mFailedAnimator;
            if (animator2 != null && animator2.isRunning()) {
                return;
            }
        }
        if (z2) {
            ValueAnimator valueAnimator = this.mDeleteAnimator;
            if (valueAnimator != null && valueAnimator.isRunning()) {
                this.mDeleteAnimator.end();
            }
            ValueAnimator valueAnimator2 = this.mAddAnimator;
            if (valueAnimator2 != null && valueAnimator2.isRunning()) {
                this.mAddAnimator.end();
            }
            this.animationMode = 4;
            this.mRectanglesNumber = this.mCodeNumber;
            int i3 = this.mRectangleNum;
            if (i3 == 4) {
                this.mCodeNumber = 3;
            } else if (i3 == 6) {
                this.mCodeNumber = 5;
            }
            ValueAnimator valueAnimatorCreateMorphingAnimationOutLinedToFilled = createMorphingAnimationOutLinedToFilled();
            this.mAddAnimator = valueAnimatorCreateMorphingAnimationOutLinedToFilled;
            valueAnimatorCreateMorphingAnimationOutLinedToFilled.start();
        }
    }

    public void setClearAll(boolean z2) {
        int i2 = this.mRectangleNum;
        if (i2 == 4) {
            int i3 = this.mCodeNumber;
            if (i3 == -1 || this.mDrawFailedAnimation || i3 > 3 || !z2) {
                return;
            }
            Animator animator = this.mFailedAnimator;
            if (animator != null && animator.isRunning()) {
                return;
            }
        } else if (i2 == 6) {
            int i4 = this.mCodeNumber;
            if (i4 == -1 || this.mDrawFailedAnimation || i4 > 5 || !z2) {
                return;
            }
            Animator animator2 = this.mFailedAnimator;
            if (animator2 != null && animator2.isRunning()) {
                return;
            }
        }
        ValueAnimator valueAnimator = this.mDeleteAnimator;
        if (valueAnimator != null && valueAnimator.isRunning()) {
            this.mDeleteAnimator.end();
        }
        ValueAnimator valueAnimator2 = this.mAddAnimator;
        if (valueAnimator2 != null && valueAnimator2.isRunning()) {
            this.mAddAnimator.end();
        }
        LinkedList<String> linkedList = this.mNumberStrList;
        if (linkedList != null) {
            linkedList.clear();
        }
        this.animationMode = 3;
        this.mRectanglesNumber = this.mCodeNumber;
        this.mCodeNumber = -1;
        ValueAnimator valueAnimatorCreateMorphingAnimationFilledToOutLined = createMorphingAnimationFilledToOutLined();
        this.mDeleteAnimator = valueAnimatorCreateMorphingAnimationFilledToOutLined;
        valueAnimatorCreateMorphingAnimationFilledToOutLined.start();
    }

    public void setDeleteLast(boolean z2) {
        int i2;
        int i3 = this.mRectangleNum;
        if ((i3 == 4 || i3 == 6) && ((i2 = this.mCodeNumber) == -1 || !z2 || i2 >= i3)) {
            return;
        }
        LinkedList<String> linkedList = this.mNumberStrList;
        if (linkedList != null && !linkedList.isEmpty()) {
            this.mNumberStrList.removeFirst();
            String str = this.mDecription;
            if (str != null && this.mNumberStrList != null) {
                this.mDecription = str.replace('y', String.valueOf(this.mRectangleNum).charAt(0));
                announceForAccessibility(this.mDecription.replace('x', String.valueOf(this.mNumberStrList.size()).charAt(0)));
            }
        }
        this.mCodeNumber--;
        if (this.mDrawFailedAnimation) {
            return;
        }
        Animator animator = this.mFailedAnimator;
        if (animator == null || !animator.isRunning()) {
            if (this.mCodeNumber < -1) {
                this.mCodeNumber = -1;
                return;
            }
            ValueAnimator valueAnimator = this.mDeleteAnimator;
            if (valueAnimator != null && valueAnimator.isRunning()) {
                this.mDeleteAnimator.end();
            }
            ValueAnimator valueAnimator2 = this.mAddAnimator;
            if (valueAnimator2 != null && valueAnimator2.isRunning()) {
                this.mAddAnimator.end();
            }
            this.animationMode = 1;
            ValueAnimator valueAnimatorCreateMorphingAnimationFilledToOutLined = createMorphingAnimationFilledToOutLined();
            this.mDeleteAnimator = valueAnimatorCreateMorphingAnimationFilledToOutLined;
            valueAnimatorCreateMorphingAnimationFilledToOutLined.start();
        }
    }

    public void setFailed(boolean z2) {
        Animator animator = this.mFailedAnimator;
        if (animator != null && animator.isRunning()) {
            this.mFailedAnimator.end();
        }
        this.mDrawFailedAnimation = z2;
    }

    public void setFilledRectangleDrawable(Drawable drawable) {
        this.mFilledRectangleDrawable = drawable;
    }

    public void setFingerprintRecognition(boolean z2) {
        this.isFingerprintMode = z2;
    }

    public void setInternalTranslationX(float f2) {
        this.mTransitionX = f2;
    }

    public void setInternalTranslationY(float f2) {
        this.mTransitionY = f2;
    }

    public void setOneCode(int i2) {
        int i3 = this.mRectangleNum;
        if (i3 == 4) {
            if (this.mCodeNumber > 3) {
                return;
            }
        } else if (i3 == 6 && this.mCodeNumber > 5) {
            return;
        }
        if (i3 == 4) {
            if (this.mCodeNumber == 3) {
                this.mCodeNumber = -1;
            }
        } else if (i3 == 6 && this.mCodeNumber == 5) {
            this.mCodeNumber = -1;
        }
        ValueAnimator valueAnimator = this.mDeleteAnimator;
        if (valueAnimator != null && valueAnimator.isRunning()) {
            this.mDeleteAnimator.end();
        }
        ValueAnimator valueAnimator2 = this.mAddAnimator;
        if (valueAnimator2 != null && valueAnimator2.isRunning()) {
            this.mAddAnimator.end();
        }
        this.animationMode = 2;
        this.mCodeNumber++;
        ValueAnimator valueAnimatorCreateMorphingAnimationOutLinedToFilled = createMorphingAnimationOutLinedToFilled();
        this.mAddAnimator = valueAnimatorCreateMorphingAnimationOutLinedToFilled;
        valueAnimatorCreateMorphingAnimationOutLinedToFilled.start();
        if (this.mNumberStrList != null) {
            String strValueOf = String.valueOf(i2);
            if (this.mCodeNumber != this.mRectangleNum - 1) {
                this.mNumberStrList.addFirst(strValueOf);
            } else {
                this.mNumberStrList.clear();
            }
        }
    }

    public void setOpacity(int i2) {
        this.mOpacity = i2;
    }

    public void setOutlinedRectangleDrawable(Drawable drawable) {
        this.mOutlinedRectangleDrawable = drawable;
    }

    public void setRectanglePadding(int i2) {
        this.mRectanglePadding = i2;
    }

    public void setRectangleType(int i2) {
        this.mRectangleType = i2;
    }

    @Override // android.view.View
    public void setScaleX(float f2) {
        this.mScaleX = f2;
    }

    @Override // android.view.View
    public void setScaleY(float f2) {
        this.mScaleY = f2;
    }

    public void setSimpleLockType(int i2) {
        if (i2 == 0) {
            this.mRectangleNum = 4;
            this.mRectanglesWidth = (this.mDrawableWidth * 4) + (this.mRectanglePadding * 3);
        } else if (i2 == 1) {
            this.mRectangleNum = 6;
            this.mRectanglesWidth = (this.mDrawableWidth * 6) + (this.mRectanglePadding * 5);
        }
        this.mCodeImageStart = (this.mContentWidth - this.mRectanglesWidth) / 2;
        invalidate();
    }

    public COUISimpleLock(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R.attr.couiSimpleLockStyle);
    }

    public COUISimpleLock(Context context, AttributeSet attributeSet, int i2) {
        this(context, attributeSet, i2, COUIContextUtil.isCOUIDarkTheme(context) ? R.style.Widget_COUI_COUISimpleLock_Dark : R.style.Widget_COUI_COUISimpleLock);
    }

    public COUISimpleLock(Context context, AttributeSet attributeSet, int i2, int i3) {
        super(context, attributeSet, i2, i3);
        this.mCodeNumber = -1;
        this.DELETE_ANIMATION = 1;
        this.ADD_ANIMATION = 2;
        this.CLEAR_ALL_ANIMATION = 3;
        this.DRAW_ALL_ANIMATION = 4;
        this.FAILED_ANIMATION = 5;
        this.MORPHING_OUTLINED_TO_FILLED_TIME = 230;
        this.MORPHING_FILLED_TO_OUTLINED_TIME = 230;
        this.SHAKE_AND_FALL_TIME = 800;
        this.FALL_SPEED = 250;
        this.FALL_HEIGHT = 150;
        this.SHAKE_X_POINT = new float[]{0.0f, 30.0f, -28.0f, 14.0f, -8.0f, 4.0f, -3.0f, 0.0f};
        this.DELAY_FOUR = new float[]{0.0f, 38.5f, 91.0f, 63.0f};
        this.DELAY_SIX = new float[]{0.0f, 38.5f, 91.0f, 63.0f, 38.5f, 70.0f};
        this.mRectanglesWidth = 0;
        this.mDrawable = null;
        this.fto_lastDraw = false;
        this.otf_lastDraw = false;
        this.fail_lastDraw = false;
        this.animationMode = 0;
        this.mDrawFailedAnimation = false;
        this.mAddAnimator = null;
        this.mDeleteAnimator = null;
        this.mFailedAnimator = null;
        this.mScaleX = 0.0f;
        this.mScaleY = 0.0f;
        this.mOpacity = 0;
        this.mTransitionX = 0.0f;
        this.mTransitionY = 0.0f;
        this.isFingerprintMode = false;
        this.mRectangleType = -1;
        this.mRectangleNum = -1;
        this.mNumberStrList = null;
        this.mTouchHelper = null;
        this.mDecription = null;
        this.mIsVibrator = true;
        this.mDeleteAnimatorInterpolator = new COUIEaseInterpolator();
        if (attributeSet != null && attributeSet.getStyleAttribute() != 0) {
            this.mStyle = attributeSet.getStyleAttribute();
        } else {
            this.mStyle = i2;
        }
        this.mContext = context;
        COUIDarkModeUtil.setForceDarkAllow(this, false);
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.COUISimpleLock, i2, i3);
        this.mRectanglePadding = typedArrayObtainStyledAttributes.getDimensionPixelSize(R.styleable.COUISimpleLock_couiRectanglePadding, 0);
        this.mOutlinedRectangleDrawable = typedArrayObtainStyledAttributes.getDrawable(R.styleable.COUISimpleLock_couiOutLinedRectangleIconDrawable);
        this.mFilledRectangleDrawable = typedArrayObtainStyledAttributes.getDrawable(R.styleable.COUISimpleLock_couiFilledRectangleIconDrawable);
        this.mRectangleType = typedArrayObtainStyledAttributes.getInteger(R.styleable.COUISimpleLock_couiCircleNum, 0);
        typedArrayObtainStyledAttributes.recycle();
        Drawable drawable = this.mFilledRectangleDrawable;
        if (drawable != null) {
            this.mDrawable = drawable;
            this.mDrawableWidth = drawable.getIntrinsicWidth();
            this.mDrawableHeight = this.mDrawable.getIntrinsicHeight();
            int i4 = this.mRectangleType;
            if (i4 == 0) {
                this.mRectangleNum = 4;
                this.mRectanglesWidth = (this.mDrawableWidth * 4) + (this.mRectanglePadding * 3);
            } else if (i4 == 1) {
                this.mRectangleNum = 6;
                this.mRectanglesWidth = (this.mDrawableWidth * 6) + (this.mRectanglePadding * 5);
            }
        }
        SimpleLockTouchHelper simpleLockTouchHelper = new SimpleLockTouchHelper(this);
        this.mTouchHelper = simpleLockTouchHelper;
        ViewCompat.setAccessibilityDelegate(this, simpleLockTouchHelper);
        LinkedList<String> linkedList = new LinkedList<>();
        this.mNumberStrList = linkedList;
        linkedList.clear();
        this.mDecription = context.getResources().getString(R.string.coui_simple_lock_access_description);
        setImportantForAccessibility(1);
        this.mIsLinearMotorVersion = VibrateUtils.isLinearMotorVersion(context);
    }

    private void drawFilledRectangle(Canvas canvas, int i2, int i3, int i4, int i5, int i6) {
        Drawable drawableNewDrawable = this.mFilledRectangleDrawable.getConstantState().newDrawable();
        this.mDrawable = drawableNewDrawable;
        float f2 = this.mTransitionX;
        drawableNewDrawable.setBounds((int) (i3 + f2), i2, (int) (i4 + f2), i5);
        this.mDrawable.setAlpha(i6 > 0 ? 255 : 0);
        this.mDrawable.draw(canvas);
    }
}
