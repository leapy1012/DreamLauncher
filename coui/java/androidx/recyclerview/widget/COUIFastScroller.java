package androidx.recyclerview.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.PathInterpolator;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.coui.appcompat.animation.COUIEaseInterpolator;
import com.coui.appcompat.R;
import com.coui.appcompat.contextutil.COUIContextUtil;
import com.coui.appcompat.vibrateutil.VibrateUtils;




/**
 * Leapy added 2026-08-01: COUI fast scroller recovered from the decoded
 * ColorOS 16 TrafficMonitor COUI implementation. Resource symbols and the
 * vibrator type are mapped to this source tree; the animation/state behavior
 * remains the decoded implementation.
 */
public class COUIFastScroller extends RecyclerView.ItemDecoration implements RecyclerView.OnItemTouchListener {
    private static final int ANIMATION_STATE_FADING_IN = 1;
    private static final int ANIMATION_STATE_FADING_OUT = 3;
    private static final int ANIMATION_STATE_IN = 2;
    private static final int ANIMATION_STATE_OUT = 0;
    private static final int DIRECTION = 1;
    private static final int DRAG_NONE = 0;
    private static final int DRAG_X = 1;
    private static final int DRAG_Y = 2;
    private static final String HEIGHT_ANIM_HOLDER = "HEIGHT_ANIM_HOLDER";
    private static final int HIDE_DELAY_AFTER_DRAGGING_MS = 2000;
    private static final int HIDE_DELAY_AFTER_VISIBLE_MS = 2000;
    private static final int HIDE_DURATION_MS = 160;
    private static final String MEDIUM_FONT = "sans-serif-medium";
    private static final int MIN_VELOCITY_WEAK = 250;
    private static final int MIN_VELOCITY_WEAKEST = 70;
    public static final int MIN_VIBRATOR_TIME = 100;
    private static final int SCROLLBAR_FULL_OPAQUE = 255;
    private static final int SHOW_DURATION_MS = 160;
    private static final int STATE_DRAGGING = 2;
    private static final int STATE_HIDDEN = 0;
    private static final int STATE_VISIBLE = 1;
    private static final String THUMB_TRANSLATE_X_HOLDER = "THUMB_TRANSLATE_X_HOLDER";
    private static final float TOUCH_SCALE_FACTOR = 2.5f;
    private static final String WIDTH_ANIM_HOLDER = "WIDTH_ANIM_HOLDER";
    private float mCurrentThumbShadowX;
    private float mCurrentThumbShadowY;
    private final int mDefaultVerticalMarginEnd;
    private final int mDefaultVerticalThumbHeight;
    private final int mDefaultVerticalThumbWidth;
    private final String mDots;
    private boolean mHasMotorVibrator;
    private final float mHeightEndScale;
    private PropertyValuesHolder mHeightScaleHolder;
    private ValueAnimator mMessageAlphaAnimator;
    private f mMessageAnimatorUpdateListener;
    private final Drawable mMessageBackgroundDrawable;
    private int mMessageBackgroundHeight;
    private final int mMessageBackgroundInternalPadding;
    private int mMessageBackgroundShadowPaddingEnd;
    private int mMessageBackgroundShadowPaddingTop;
    private final int mMessageBackgroundTopOffset;
    private final int mMessageMarginEnd;
    private final int mMessageMaximumWidth;
    private final int mMessageMinimumWidth;
    private TextPaint mMessagePaint;
    private final int mMessageTextPadding;
    private float mMessageTextShadowPaddingY;
    private float mMessageWidth;
    private boolean mNeedShowMessage;
    private g mPressAnimatorListener;
    private RecyclerView mRecyclerView;
    private final int mScaleEndThumbTranslateX;
    private float mScaleEndThumbTranslateY;
    private final int mScaleEndVerticalThumbHeight;
    private final int mScaleEndVerticalThumbWidth;
    private final int mScrollbarMinimumRange;
    private float mTextWidth;
    private float mTextX;
    private float mTextY;
    private int mThumbBackgroundShadowPaddingEnd;
    private int mThumbBackgroundShadowPaddingTop;
    private int mThumbBottomMargin;
    private final int mThumbDrawableBackgroundScaleCenterX;
    private final int mThumbDrawableBackgroundScaleCenterY;
    private ValueAnimator mThumbScaleAnimator;
    private h mThumbScaleAnimatorUpdateListener;
    private int mThumbTopMargin;
    private PropertyValuesHolder mThumbTranslateXHolder;
    private VelocityTracker mVelocityTracker;
    private float mVerticalDragY;
    private int mVerticalThumbCenterY;
    private final Drawable mVerticalThumbDrawable;
    private final Drawable mVerticalThumbDrawableBackground;
    private int mVibrateLevel;
    private final float mWidthEndScale;
    private PropertyValuesHolder mWidthScaleHolder;
    private float mCurrentWidthScale = 1.0f;
    private float mCurrentHeightScale = 1.0f;
    private float mCurrentThumbTranslateX = 0.0f;
    private float mCurrentThumbTranslateScaleX = 0.0f;
    private float mCurrentThumbTranslateY = 0.0f;
    private final PathInterpolator mCommonInterpolator = new COUIEaseInterpolator();
    private AnimatorSet mPressAnimators = new AnimatorSet();
    private int mPressAnimatorState = 0;
    private float mMessageAlphaAnimatedValue = 0.0f;
    private String mMessage = "";
    private String mRealShowMessage = "";
    private boolean mIsThumbAlwaysShow = false;
    private int mRecyclerViewWidth = 0;
    private int mRecyclerViewHeight = 0;
    private int mTrackerMaxVelocity = 8000;
    private int mLowVelocityThreshold = 3000;
    private int mMidVelocityThreshold = 6000;
    private int mTrackerPeriod = 1000;
    private long lastVibratorTime = -1;
    private Object mLinearMotorVibrator = null;
    private boolean mEnableAdaptiveVibrator = true;
    private boolean mEnabled = true;
    private boolean mNeedVerticalScrollbar = false;
    private int mState = 0;
    private int mDragState = 0;
    private float mVibrateIntensity = 1.0f;
    private final int[] mVerticalRange = new int[2];
    final ValueAnimator mShowHideAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
    int mAnimationState = 0;
    private final Runnable mHideRunnable = new a();
    private final RecyclerView.OnScrollListener mOnScrollListener = new b();

    class a implements Runnable {
        a() {
        }

        @Override
        public void run() {
            if (COUIFastScroller.this.mIsThumbAlwaysShow) {
                return;
            }
            COUIFastScroller.this.hide(160);
        }
    }

    class b extends RecyclerView.OnScrollListener {
        b() {
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int i5, int i6) {
            if (COUIFastScroller.this.mEnabled) {
                COUIFastScroller.this.performFeedback();
                COUIFastScroller.this.updateScrollPosition(recyclerView.computeHorizontalScrollOffset(), recyclerView.computeVerticalScrollOffset());
            }
        }
    }

    class c implements View.OnAttachStateChangeListener {
        c() {
        }

        @Override
        public void onViewAttachedToWindow(View view) {
            VibrateUtils.registerHapticObserver(COUIFastScroller.this.mRecyclerView.getContext());
        }

        @Override
        public void onViewDetachedFromWindow(View view) {
            VibrateUtils.unRegisterHapticObserver();
            COUIFastScroller.this.mRecyclerView.removeOnAttachStateChangeListener(this);
            COUIFastScroller.this.cancelHide();
        }
    }

    private class d extends AnimatorListenerAdapter {


        private boolean mCanceled = false;

        d() {
        }

        @Override
        public void onAnimationCancel(Animator animator) {
            this.mCanceled = true;
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            if (this.mCanceled) {
                this.mCanceled = false;
                return;
            }
            if (((Float) COUIFastScroller.this.mShowHideAnimator.getAnimatedValue()).floatValue() == 0.0f) {
                COUIFastScroller cOUIFastScroller = COUIFastScroller.this;
                cOUIFastScroller.mAnimationState = 0;
                cOUIFastScroller.setState(0);
            } else {
                COUIFastScroller cOUIFastScroller2 = COUIFastScroller.this;
                cOUIFastScroller2.mAnimationState = 2;
                cOUIFastScroller2.requestRedraw();
            }
        }
    }

    private class e implements ValueAnimator.AnimatorUpdateListener {
        e() {
        }

        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            int iFloatValue = (int) (((Float) valueAnimator.getAnimatedValue()).floatValue() * 255.0f);
            COUIFastScroller.this.mVerticalThumbDrawableBackground.setAlpha(iFloatValue);
            COUIFastScroller.this.mVerticalThumbDrawable.setAlpha(iFloatValue);
            COUIFastScroller.this.requestRedraw();
        }
    }

    private class f implements ValueAnimator.AnimatorUpdateListener {
        private f() {
        }

        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            COUIFastScroller.this.mMessageAlphaAnimatedValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
            int i5 = (int) (COUIFastScroller.this.mMessageAlphaAnimatedValue * 255.0f);
            COUIFastScroller.this.mMessageBackgroundDrawable.setAlpha(i5);
            COUIFastScroller.this.mMessagePaint.setAlpha(i5);
            COUIFastScroller.this.requestRedraw();
        }

 f(COUIFastScroller cOUIFastScroller, a aVar) {
            this();
        }
    }

    private class g extends AnimatorListenerAdapter {


        private boolean mCanceled = false;

        g() {
        }

        @Override
        public void onAnimationCancel(Animator animator) {
            this.mCanceled = true;
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            if (this.mCanceled) {
                this.mCanceled = false;
            } else if (COUIFastScroller.this.mCurrentWidthScale != 1.0f) {
                COUIFastScroller.this.mPressAnimatorState = 2;
            } else {
                COUIFastScroller.this.mPressAnimatorState = 0;
            }
        }
    }

    private class h implements ValueAnimator.AnimatorUpdateListener {
        private h() {
        }

        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            COUIFastScroller.this.mCurrentHeightScale = ((Float) valueAnimator.getAnimatedValue(COUIFastScroller.HEIGHT_ANIM_HOLDER)).floatValue();
            COUIFastScroller.this.mCurrentWidthScale = ((Float) valueAnimator.getAnimatedValue(COUIFastScroller.WIDTH_ANIM_HOLDER)).floatValue();
            COUIFastScroller.this.mCurrentThumbTranslateScaleX = ((Float) valueAnimator.getAnimatedValue(COUIFastScroller.THUMB_TRANSLATE_X_HOLDER)).floatValue();
            COUIFastScroller cOUIFastScroller = COUIFastScroller.this;
            cOUIFastScroller.mCurrentThumbTranslateX = cOUIFastScroller.mCurrentThumbTranslateScaleX * COUIFastScroller.this.mScaleEndThumbTranslateX;
            COUIFastScroller cOUIFastScroller2 = COUIFastScroller.this;
            cOUIFastScroller2.mCurrentThumbTranslateY = cOUIFastScroller2.mCurrentThumbTranslateScaleX * COUIFastScroller.this.mScaleEndThumbTranslateY;
            COUIFastScroller.this.requestRedraw();
        }

 h(COUIFastScroller cOUIFastScroller, a aVar) {
            this();
        }
    }

    public COUIFastScroller(RecyclerView recyclerView, Context context) {
        this.mScaleEndThumbTranslateY = 0.0f;
        this.mHasMotorVibrator = true;
        this.mMessageBackgroundHeight = 0;
        this.mMessageBackgroundShadowPaddingEnd = 0;
        this.mThumbBackgroundShadowPaddingEnd = 0;
        this.mMessageBackgroundShadowPaddingTop = 0;
        this.mThumbBackgroundShadowPaddingTop = 0;
        this.mCurrentThumbShadowY = 0.0f;
        this.mCurrentThumbShadowX = 0.0f;
        this.mMessageTextShadowPaddingY = 0.0f;
        int dimensionPixelOffset = context.getResources().getDimensionPixelOffset(R.dimen.coui_fast_scroller_default_width);
        this.mDefaultVerticalThumbWidth = dimensionPixelOffset;
        int dimensionPixelOffset2 = context.getResources().getDimensionPixelOffset(R.dimen.coui_fast_scroller_default_height);
        this.mDefaultVerticalThumbHeight = dimensionPixelOffset2;
        this.mDefaultVerticalMarginEnd = context.getResources().getDimensionPixelOffset(R.dimen.coui_fast_scroller_default_vertical_margin_end);
        int dimensionPixelOffset3 = context.getResources().getDimensionPixelOffset(R.dimen.coui_fast_scroller_scale_end_width);
        this.mScaleEndVerticalThumbWidth = dimensionPixelOffset3;
        int dimensionPixelOffset4 = context.getResources().getDimensionPixelOffset(R.dimen.coui_fast_scroller_scale_end_height);
        this.mScaleEndVerticalThumbHeight = dimensionPixelOffset4;
        this.mThumbDrawableBackgroundScaleCenterX = context.getResources().getDimensionPixelOffset(R.dimen.coui_fast_scroller_bar_background_scale_x_offset);
        this.mThumbBackgroundShadowPaddingEnd = context.getResources().getDimensionPixelOffset(R.dimen.coui_fast_scroller_scale_shadow_padding_end);
        this.mThumbBackgroundShadowPaddingTop = context.getResources().getDimensionPixelOffset(R.dimen.coui_fast_scroller_scale_shadow_padding_top);
        this.mThumbDrawableBackgroundScaleCenterY = dimensionPixelOffset2 / 2;
        // Leapy modified 2026-08-01: The decoded bytecode performs
        // int-to-float on both operands before div-float. Keeping Java integer
        // division here collapses the OPPO press scale to 1.0 and removes the
        // fast-scroller expansion animation.
        this.mWidthEndScale = (float) dimensionPixelOffset3 / dimensionPixelOffset;
        this.mHeightEndScale = (float) dimensionPixelOffset4 / dimensionPixelOffset2;
        // Leapy end
        Drawable drawable = context.getDrawable(R.drawable.coui_fast_scroller_slide_bar_background);
        this.mVerticalThumbDrawableBackground = drawable;
        drawable.setBounds(0, 0, dimensionPixelOffset, dimensionPixelOffset2);
        drawable.setAlpha(255);
        Drawable drawable2 = context.getDrawable(R.drawable.coui_fast_scroller_union);
        this.mVerticalThumbDrawable = drawable2;
        this.mScaleEndThumbTranslateX = context.getResources().getDimensionPixelOffset(R.dimen.coui_fast_scroller_bar_thumb_translate_x);
        this.mScaleEndThumbTranslateY = context.getResources().getDimensionPixelOffset(R.dimen.coui_fast_scroller_bar_thumb_translate_y);
        this.mCurrentThumbShadowY = context.getResources().getDimensionPixelOffset(R.dimen.coui_fast_scroller_bar_thumb_shadow_padding_y);
        this.mCurrentThumbShadowX = context.getResources().getDimensionPixelOffset(R.dimen.coui_fast_scroller_bar_thumb_shadow_padding_x);
        int dimensionPixelOffset5 = context.getResources().getDimensionPixelOffset(R.dimen.coui_fast_scroller_union_width);
        int dimensionPixelOffset6 = context.getResources().getDimensionPixelOffset(R.dimen.coui_fast_scroller_union_height);
        int i5 = (dimensionPixelOffset - dimensionPixelOffset5) / 2;
        int i6 = (dimensionPixelOffset2 - dimensionPixelOffset6) / 2;
        drawable2.setBounds(i5, i6, dimensionPixelOffset5 + i5, dimensionPixelOffset6 + i6);
        drawable2.setAlpha(255);
        Drawable drawable3 = context.getDrawable(R.drawable.coui_fast_scroller_message_background);
        this.mMessageBackgroundDrawable = drawable3;
        drawable3.setAlpha(0);
        this.mMessageTextPadding = context.getResources().getDimensionPixelOffset(R.dimen.coui_fast_scroller_message_text_padding);
        this.mMessageBackgroundInternalPadding = context.getResources().getDimensionPixelOffset(R.dimen.coui_fast_scroller_message_background_internal_padding);
        this.mMessageBackgroundTopOffset = context.getResources().getDimensionPixelOffset(R.dimen.coui_fast_scroller_message_background_top_offset);
        this.mMessageBackgroundHeight = context.getResources().getDimensionPixelOffset(R.dimen.coui_fast_scroller_message_minimum_height);
        this.mMessageBackgroundShadowPaddingEnd = context.getResources().getDimensionPixelOffset(R.dimen.coui_fast_scroller_message_shadow_padding_end);
        this.mMessageBackgroundShadowPaddingTop = context.getResources().getDimensionPixelOffset(R.dimen.coui_fast_scroller_message_shadow_padding_top);
        this.mMessageTextShadowPaddingY = context.getResources().getDimensionPixelOffset(R.dimen.coui_fast_scroller_message_text_shadow_padding_top);
        this.mMessageMaximumWidth = context.getResources().getDimensionPixelOffset(R.dimen.coui_fast_scroller_max_message_width);
        this.mMessageMinimumWidth = context.getResources().getDimensionPixelOffset(R.dimen.coui_fast_scroller_message_minimum_width);
        this.mMessageMarginEnd = context.getResources().getDimensionPixelOffset(R.dimen.coui_fast_scroller_message_margin_end);
        this.mDots = context.getString(R.string.fast_scroller_dots);
        this.mScrollbarMinimumRange = context.getResources().getDimensionPixelOffset(R.dimen.coui_fast_scroller_minimum_range);
        this.mThumbTopMargin = context.getResources().getDimensionPixelOffset(R.dimen.coui_fast_scroller_thumb_top_margin);
        this.mThumbBottomMargin = context.getResources().getDimensionPixelOffset(R.dimen.coui_fast_scroller_thumb_bottom_margin);
        this.mHasMotorVibrator = VibrateUtils.isLinearMotorVersion(context);
        initMessagePaint(context);
        initAnimators();
        attachToRecyclerView(recyclerView);
    }


    public void cancelHide() {
        this.mRecyclerView.removeCallbacks(this.mHideRunnable);
    }

    private void computeVelocityWithTouchEvent(MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        if (action == 0) {
            initOrResetVelocityTracker();
            this.mVelocityTracker.addMovement(motionEvent);
            return;
        }
        if (action != 1) {
            if (action == 2) {
                initVelocityTrackerIfNotExists();
                this.mVelocityTracker.addMovement(motionEvent);
                return;
            } else if (action != 3) {
                return;
            }
        }
        recycleVelocityTracker();
    }

    private void destroyCallbacks() {
        this.mRecyclerView.removeItemDecoration(this);
        this.mRecyclerView.removeOnItemTouchListener(this);
        this.mRecyclerView.removeOnScrollListener(this.mOnScrollListener);
        cancelHide();
    }

    private void drawVerticalScrollbar(Canvas canvas) {
        int i5;
        int i6;
        float f5;
        float f6;
        float f7;
        this.mVerticalThumbDrawableBackground.mutate();
        this.mVerticalThumbDrawable.mutate();
        int i7 = this.mRecyclerViewWidth;
        int i8 = this.mVerticalThumbCenterY;
        int i9 = (i8 - (this.mDefaultVerticalThumbHeight / 2)) + this.mThumbBackgroundShadowPaddingTop;
        float f8 = ((i8 - (this.mScaleEndVerticalThumbHeight / 2.0f)) - this.mMessageBackgroundTopOffset) + this.mMessageBackgroundShadowPaddingTop;
        float f9 = -this.mCurrentThumbTranslateY;
        float f10 = -this.mCurrentThumbShadowY;
        float f11 = -this.mMessageTextShadowPaddingY;
        if (isLayoutRTL()) {
            int i10 = this.mDefaultVerticalMarginEnd;
            i5 = i10 - this.mThumbBackgroundShadowPaddingEnd;
            f5 = ((i10 + this.mScaleEndVerticalThumbWidth) - this.mMessageMarginEnd) - this.mMessageBackgroundShadowPaddingEnd;
            f6 = this.mCurrentThumbTranslateX;
            f7 = -this.mCurrentThumbShadowX;
            i6 = this.mThumbDrawableBackgroundScaleCenterX - i5;
        } else {
            int i11 = i7 - this.mDefaultVerticalThumbWidth;
            int i12 = this.mDefaultVerticalMarginEnd;
            i5 = (i11 - i12) + this.mThumbBackgroundShadowPaddingEnd;
            float f12 = this.mMessageBackgroundShadowPaddingEnd + ((((i7 - this.mMessageWidth) - this.mScaleEndVerticalThumbWidth) - i12) - this.mMessageMarginEnd);
            float f13 = -this.mCurrentThumbTranslateX;
            float f14 = this.mCurrentThumbShadowX;
            i6 = (i7 - i5) - this.mThumbDrawableBackgroundScaleCenterX;
            f5 = f12;
            f6 = f13;
            f7 = f14;
        }
        int iSave = canvas.save();
        canvas.translate(i5, i9);
        int iSave2 = canvas.save();
        float f15 = i6;
        canvas.scale(this.mCurrentWidthScale, this.mCurrentHeightScale, f15, this.mThumbDrawableBackgroundScaleCenterY);
        this.mVerticalThumbDrawableBackground.draw(canvas);
        canvas.restoreToCount(iSave2);
        canvas.translate(f7, f10);
        canvas.translate(f6, f9);
        canvas.scale(this.mCurrentWidthScale, this.mCurrentHeightScale, f15, this.mThumbDrawableBackgroundScaleCenterY);
        this.mVerticalThumbDrawable.draw(canvas);
        canvas.restoreToCount(iSave);
        if (!this.mNeedShowMessage || this.mMessageAlphaAnimatedValue == 0.0f) {
            return;
        }
        int iSave3 = canvas.save();
        canvas.translate(f5, f8);
        this.mMessageBackgroundDrawable.draw(canvas);
        canvas.translate(0.0f, f11);
        canvas.drawText(this.mRealShowMessage, this.mTextX, this.mTextY, this.mMessagePaint);
        canvas.restoreToCount(iSave3);
    }

    private void executePressAnimator(boolean z5) {
        this.mWidthScaleHolder.setFloatValues(this.mCurrentWidthScale, z5 ? this.mWidthEndScale : 1.0f);
        this.mHeightScaleHolder.setFloatValues(this.mCurrentHeightScale, z5 ? this.mHeightEndScale : 1.0f);
        this.mThumbTranslateXHolder.setFloatValues(this.mCurrentThumbTranslateScaleX, z5 ? 1.0f : 0.0f);
        if (this.mNeedShowMessage) {
            this.mMessageAlphaAnimator.setFloatValues(this.mMessageAlphaAnimatedValue, z5 ? 1.0f : 0.0f);
        }
        this.mPressAnimators.start();
    }

    private boolean filterVibrator() {
        if (this.lastVibratorTime == -1) {
            this.lastVibratorTime = System.currentTimeMillis();
            return false;
        }
        if (System.currentTimeMillis() - this.lastVibratorTime < 100) {
            return true;
        }
        this.lastVibratorTime = System.currentTimeMillis();
        return false;
    }

    private int[] getVerticalRange() {
        int[] iArr = this.mVerticalRange;
        iArr[0] = this.mThumbTopMargin;
        iArr[1] = this.mRecyclerViewHeight - this.mThumbBottomMargin;
        return iArr;
    }


    public void hide(int i5) {
        int i6 = this.mAnimationState;
        if (i6 == 1) {
            this.mShowHideAnimator.cancel();
        } else if (i6 != 2) {
            return;
        }
        this.mAnimationState = 3;
        ValueAnimator valueAnimator = this.mShowHideAnimator;
        valueAnimator.setFloatValues(((Float) valueAnimator.getAnimatedValue()).floatValue(), 0.0f);
        this.mShowHideAnimator.setDuration(i5);
        this.mShowHideAnimator.start();
    }

    private void initAnimators() {
        this.mShowHideAnimator.addListener(new d());
        this.mShowHideAnimator.addUpdateListener(new e());
        this.mShowHideAnimator.setInterpolator(this.mCommonInterpolator);
        a aVar = null;
        this.mThumbScaleAnimatorUpdateListener = new h(this, aVar);
        this.mPressAnimatorListener = new g();
        this.mMessageAnimatorUpdateListener = new f(this, aVar);
        this.mWidthScaleHolder = PropertyValuesHolder.ofFloat(WIDTH_ANIM_HOLDER, 0.0f, 0.0f);
        this.mHeightScaleHolder = PropertyValuesHolder.ofFloat(HEIGHT_ANIM_HOLDER, 0.0f, 0.0f);
        PropertyValuesHolder propertyValuesHolderOfFloat = PropertyValuesHolder.ofFloat(THUMB_TRANSLATE_X_HOLDER, 0.0f, 0.0f);
        this.mThumbTranslateXHolder = propertyValuesHolderOfFloat;
        ValueAnimator valueAnimatorOfPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(this.mWidthScaleHolder, this.mHeightScaleHolder, propertyValuesHolderOfFloat);
        this.mThumbScaleAnimator = valueAnimatorOfPropertyValuesHolder;
        valueAnimatorOfPropertyValuesHolder.setDuration(200L);
        this.mThumbScaleAnimator.setInterpolator(this.mCommonInterpolator);
        this.mThumbScaleAnimator.addUpdateListener(this.mThumbScaleAnimatorUpdateListener);
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(new float[0]);
        this.mMessageAlphaAnimator = valueAnimatorOfFloat;
        valueAnimatorOfFloat.addUpdateListener(this.mMessageAnimatorUpdateListener);
        this.mMessageAlphaAnimator.setDuration(160L);
        this.mMessageAlphaAnimator.setInterpolator(this.mCommonInterpolator);
        resetPressAnimator(false);
    }

    private void initMessagePaint(Context context) {
        TextPaint textPaint = new TextPaint();
        this.mMessagePaint = textPaint;
        textPaint.setAntiAlias(true);
        this.mMessagePaint.setTextSize(context.getResources().getDimensionPixelSize(R.dimen.coui_fast_scroller_message_text_size));
        this.mMessagePaint.setTypeface(Typeface.create("sans-serif-medium", 0));
        this.mMessagePaint.setColor(COUIContextUtil.getAttrColor(context, R.attr.couiColorLabelPrimary));
        this.mMessagePaint.setAlpha(0);
        Paint.FontMetrics fontMetrics = this.mMessagePaint.getFontMetrics();
        float f5 = fontMetrics.bottom;
        this.mTextY = ((this.mScaleEndVerticalThumbHeight + (f5 - fontMetrics.top)) / 2.0f) - f5;
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

    private boolean isLayoutRTL() {
        return ViewCompat.getLayoutDirection(this.mRecyclerView) == 1;
    }

    private void letGo() {
        int i5 = this.mPressAnimatorState;
        if (i5 == 1) {
            this.mPressAnimators.cancel();
        } else if (i5 != 2) {
            return;
        }
        this.mPressAnimatorState = 3;
        executePressAnimator(false);
    }

    private boolean performAdaptiveFeedback() {
        VelocityTracker velocityTracker;
        if (this.mLinearMotorVibrator == null) {
            Object linearMotorVibrator = VibrateUtils.getLinearMotorVibrator(this.mRecyclerView.getContext());
            this.mLinearMotorVibrator = linearMotorVibrator;
            this.mHasMotorVibrator = linearMotorVibrator != null;
        }
        if (this.mLinearMotorVibrator == null || (velocityTracker = this.mVelocityTracker) == null) {
            return false;
        }
        velocityTracker.computeCurrentVelocity(this.mTrackerPeriod, this.mTrackerMaxVelocity);
        int iAbs = (int) Math.abs(this.mVelocityTracker.getYVelocity());
        int i5 = iAbs > this.mMidVelocityThreshold ? 0 : 1;
        if ((iAbs > MIN_VELOCITY_WEAKEST && iAbs < MIN_VELOCITY_WEAK && filterVibrator()) || iAbs < MIN_VELOCITY_WEAKEST) {
            return true;
        }
        VibrateUtils.setLinearMotorVibratorStrength(this.mLinearMotorVibrator, i5, iAbs, this.mTrackerMaxVelocity, 1200, VibrateUtils.STRENGTH_MAX_GRANULAR, this.mVibrateLevel, this.mVibrateIntensity);
        return true;
    }


    public void performFeedback() {
        if (this.mHasMotorVibrator && this.mEnableAdaptiveVibrator) {
            performAdaptiveFeedback();
        }
    }

    private void press() {
        int i5 = this.mPressAnimatorState;
        if (i5 != 0) {
            if (i5 != 3) {
                return;
            } else {
                this.mPressAnimators.cancel();
            }
        }
        this.mPressAnimatorState = 1;
        executePressAnimator(true);
    }

    private void recycleVelocityTracker() {
        VelocityTracker velocityTracker = this.mVelocityTracker;
        if (velocityTracker != null) {
            velocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    private void resetHideDelay(int i5) {
        cancelHide();
        if (this.mIsThumbAlwaysShow) {
            return;
        }
        this.mRecyclerView.postDelayed(this.mHideRunnable, i5);
    }

    private void resetPressAnimator(boolean z5) {
        AnimatorSet animatorSet = new AnimatorSet();
        this.mPressAnimators = animatorSet;
        animatorSet.play(this.mThumbScaleAnimator);
        this.mPressAnimators.addListener(this.mPressAnimatorListener);
        if (z5) {
            this.mPressAnimators.playTogether(this.mMessageAlphaAnimator);
        }
    }

    private int scrollTo(float f5, float f6, int[] iArr, int i5) {
        int i6 = iArr[1] - iArr[0];
        if (i6 == 0) {
            return 0;
        }
        return (int) (((f6 - f5) / i6) * (i5 - this.mRecyclerViewHeight));
    }

    private void setupCallbacks() {
        this.mRecyclerView.addItemDecoration(this);
        this.mRecyclerView.addOnItemTouchListener(this);
        this.mRecyclerView.addOnScrollListener(this.mOnScrollListener);
        this.mRecyclerView.addOnAttachStateChangeListener(new c());
    }

    private void show() {
        int i5 = this.mAnimationState;
        if (i5 != 0) {
            if (i5 != 3) {
                return;
            } else {
                this.mShowHideAnimator.cancel();
            }
        }
        this.mAnimationState = 1;
        ValueAnimator valueAnimator = this.mShowHideAnimator;
        valueAnimator.setFloatValues(((Float) valueAnimator.getAnimatedValue()).floatValue(), 1.0f);
        this.mShowHideAnimator.setDuration(160L);
        this.mShowHideAnimator.start();
    }

    private void verticalScrollTo(float f5) {
        int iScrollTo;
        int[] verticalRange = getVerticalRange();
        if (((f5 <= verticalRange[0] || f5 >= verticalRange[1]) && !this.mRecyclerView.canScrollVertically(1)) || Math.abs(this.mVerticalThumbCenterY - f5) < 2.0f || (iScrollTo = scrollTo(this.mVerticalDragY, f5, verticalRange, this.mRecyclerView.computeVerticalScrollRange())) == 0) {
            return;
        }
        this.mRecyclerView.scrollBy(0, iScrollTo);
        this.mVerticalDragY = f5;
    }

    public void attachToRecyclerView(RecyclerView recyclerView) {
        RecyclerView recyclerView2 = this.mRecyclerView;
        if (recyclerView2 == recyclerView) {
            return;
        }
        if (recyclerView2 != null) {
            destroyCallbacks();
        }
        this.mRecyclerView = recyclerView;
        if (recyclerView != null) {
            setupCallbacks();
        }
    }

    public boolean getEnable() {
        return this.mEnabled;
    }

    public String getMessage() {
        return this.mMessage;
    }

    public boolean getNeedShowMessage() {
        return this.mNeedShowMessage;
    }

    public String getRealShowMessage() {
        return this.mRealShowMessage;
    }

    public boolean getThumbAlwaysShow() {
        return this.mIsThumbAlwaysShow;
    }

    public int getThumbBottomMargin() {
        return this.mThumbBottomMargin;
    }

    public int getThumbTopMargin() {
        return this.mThumbTopMargin;
    }

    Drawable getVerticalThumbDrawable() {
        return this.mVerticalThumbDrawableBackground;
    }

    public boolean isDragging() {
        return this.mState == 2;
    }

    /**
     * Returns whether the active gesture is dragging the vertical thumb.
     *
     * <p>Leapy added 2026-08-01: expose the decoded {@code DRAG_Y} state to
     * clients without reflecting the private {@code mDragState} field. D8 may
     * inline and remove the private static-final constant, so reflective access
     * is not a stable library contract.</p>
     */
    public boolean isDraggingVertically() {
        return isDragging() && this.mDragState == DRAG_Y;
    }

    boolean isPointInsideVerticalThumb(float f5, float f6) {
        int i5 = this.mDefaultVerticalThumbWidth;
        int i6 = this.mDefaultVerticalMarginEnd;
        int i7 = this.mThumbBackgroundShadowPaddingEnd;
        float f7 = (i5 + i6) - (i7 * TOUCH_SCALE_FACTOR);
        float f8 = ((this.mRecyclerViewWidth - i5) - i6) + (i7 * TOUCH_SCALE_FACTOR);
        int i8 = this.mVerticalThumbCenterY;
        int i9 = this.mDefaultVerticalThumbHeight;
        int i10 = this.mThumbBackgroundShadowPaddingTop;
        float f9 = (i8 - (i9 / 2.0f)) + (i10 * TOUCH_SCALE_FACTOR);
        float f10 = (i8 + (i9 / 2.0f)) - (i10 * TOUCH_SCALE_FACTOR);
        if (!isLayoutRTL() ? f5 >= f8 : f5 <= f7) {
            if (f6 >= f9 && f6 <= f10) {
                return true;
            }
        }
        return false;
    }

    public boolean isVisible() {
        return this.mState == 1;
    }

    @Override
    public void onDrawOver(Canvas canvas, RecyclerView recyclerView, RecyclerView.State b5) {
        if (this.mRecyclerViewWidth != this.mRecyclerView.getWidth() || this.mRecyclerViewHeight != this.mRecyclerView.getHeight()) {
            this.mRecyclerViewWidth = this.mRecyclerView.getWidth();
            this.mRecyclerViewHeight = this.mRecyclerView.getHeight();
            setState(0);
        } else {
            if (this.mAnimationState == 0 || !this.mNeedVerticalScrollbar) {
                return;
            }
            drawVerticalScrollbar(canvas);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
        int i5 = this.mState;
        if (i5 == 1) {
            boolean zIsPointInsideVerticalThumb = isPointInsideVerticalThumb(motionEvent.getX(), motionEvent.getY());
            if (motionEvent.getAction() != 0 || !zIsPointInsideVerticalThumb) {
                return false;
            }
            this.mDragState = 2;
            this.mVerticalDragY = (int) motionEvent.getY();
            setState(2);
        } else if (i5 != 2) {
            return false;
        }
        return true;
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean z5) {
    }

    @Override
    public void onTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
        if (this.mState == 0) {
            return;
        }
        if (this.mEnableAdaptiveVibrator) {
            computeVelocityWithTouchEvent(motionEvent);
        }
        int action = motionEvent.getAction();
        if (action == 0) {
            if (isPointInsideVerticalThumb(motionEvent.getX(), motionEvent.getY())) {
                this.mDragState = 2;
                this.mVerticalDragY = (int) motionEvent.getY();
                setState(2);
                return;
            }
            return;
        }
        if (action != 1) {
            if (action == 2) {
                if (this.mState == 2) {
                    show();
                    if (this.mDragState == 2) {
                        verticalScrollTo(motionEvent.getY());
                        return;
                    }
                    return;
                }
                return;
            }
            if (action != 3) {
                return;
            }
        }
        if (this.mState == 2) {
            this.mVerticalDragY = 0.0f;
            setState(1);
            this.mDragState = 0;
        }
    }

    void requestRedraw() {
        this.mRecyclerView.invalidate();
    }

    public void setEnable(boolean z5) {
        this.mEnabled = z5;
        if (z5 || this.mState == 0) {
            return;
        }
        hide(160);
    }

    public void setEnableAdaptiveVibrator(boolean z5) {
        this.mEnableAdaptiveVibrator = z5;
    }

    public void setMessage(String str) {
        if (str == null || str.equals(this.mMessage) || str.trim().equals("")) {
            return;
        }
        this.mMessage = str;
        this.mRealShowMessage = str;
        float fMeasureText = this.mMessagePaint.measureText(str);
        this.mTextWidth = fMeasureText;
        float f5 = fMeasureText + this.mMessageTextPadding + this.mMessageBackgroundInternalPadding;
        this.mMessageWidth = f5;
        if (f5 > this.mMessageMaximumWidth) {
            for (int i5 = 1; i5 < str.length(); i5++) {
                String str2 = str.substring(0, str.length() - i5) + this.mDots;
                this.mRealShowMessage = str2;
                float fMeasureText2 = this.mMessagePaint.measureText(str2);
                this.mTextWidth = fMeasureText2;
                float f6 = fMeasureText2 + this.mMessageTextPadding + this.mMessageBackgroundInternalPadding;
                this.mMessageWidth = f6;
                if (f6 <= this.mMessageMaximumWidth) {
                    break;
                }
            }
        } else {
            int i6 = this.mMessageMinimumWidth;
            if (f5 < i6) {
                this.mMessageWidth = i6;
            }
        }
        this.mMessageBackgroundDrawable.setBounds(0, 0, (int) this.mMessageWidth, this.mMessageBackgroundHeight);
        this.mTextX = (this.mMessageWidth - this.mTextWidth) / 2.0f;
        requestRedraw();
    }

    public void setNeedShowMessage(boolean z5) {
        if (this.mNeedShowMessage != z5) {
            resetPressAnimator(z5);
            this.mNeedShowMessage = z5;
            requestRedraw();
        }
    }

    void setState(int i5) {
        if (i5 == 2 && this.mState != 2) {
            press();
            cancelHide();
        }
        if (i5 == 0) {
            requestRedraw();
        } else {
            show();
        }
        if (this.mState == 2 && i5 != 2) {
            resetHideDelay(2000);
            letGo();
        } else if (i5 == 1) {
            resetHideDelay(2000);
        }
        this.mState = i5;
    }

    public void setThumbAlwaysShow(boolean z5) {
        if (z5 != this.mIsThumbAlwaysShow) {
            this.mIsThumbAlwaysShow = z5;
            if (z5) {
                cancelHide();
            } else if (this.mState == 1) {
                resetHideDelay(2000);
            }
        }
    }

    public void setThumbBottomMargin(int i5) {
        this.mThumbBottomMargin = i5;
    }

    public void setThumbTopMargin(int i5) {
        this.mThumbTopMargin = i5;
    }

    public void setVibrateIntensity(float f5) {
        this.mVibrateIntensity = f5;
    }

    public void setVibrateLevel(int i5) {
        this.mVibrateLevel = i5;
    }

    void updateScrollPosition(int i5, int i6) {
        int[] verticalRange = getVerticalRange();
        int iComputeVerticalScrollRange = this.mRecyclerView.computeVerticalScrollRange();
        int i7 = verticalRange[1];
        int i8 = verticalRange[0];
        int i9 = i7 - i8;
        boolean z5 = iComputeVerticalScrollRange - i9 > 0 && this.mRecyclerViewHeight >= this.mScrollbarMinimumRange;
        this.mNeedVerticalScrollbar = z5;
        if (!z5) {
            if (this.mState != 0) {
                setState(0);
                return;
            }
            return;
        }
        // Leapy modified 2026-08-01: Match decoded int-to-float/div-float.
        // Integer division pins the thumb to the start until the final scroll
        // position, which is why the recovered scroller appeared unchanged.
        float f5 = (float) i6
                / (iComputeVerticalScrollRange - this.mRecyclerViewHeight);
        // Leapy end
        if (f5 > 1.0f) {
            this.mVerticalThumbCenterY = i9 + i8;
        } else {
            this.mVerticalThumbCenterY = (int) ((f5 * i9) + i8);
        }
        int i10 = this.mState;
        if (i10 == 0 || i10 == 1) {
            setState(1);
        }
    }
}
// Leapy end
