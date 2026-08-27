package com.coui.appcompat.tablayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Paint;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.view.ViewCompat;
import com.coui.appcompat.animation.COUIEaseInterpolator;
import com.coui.appcompat.grid.COUIResponsiveUtils;
import com.coui.appcompat.uiutil.UIUtil;
import com.coui.appcompat.R;


public class COUISlidingTabStrip extends LinearLayout {
    private final Paint mBottomDividerPaint;
    private COUITabLayout mCOUITabLayout;
    private int mContentMinWidth;
    private int mHorizontalLargeMargin;
    private int mHorizontalMediumMargin;
    private int mHorizontalSmallMargin;
    private int mIndicatorAnimTime;
    private ValueAnimator mIndicatorAnimator;
    private int mIndicatorBackgroundHeight;
    private int mIndicatorBackgroundPaddingLeft;
    private int mIndicatorBackgroundPaddingRight;
    private final Paint mIndicatorBackgroundPaint;
    private int mIndicatorLeft;
    private int mIndicatorRight;
    private float mIndicatorWidthRatio;
    float mLastOffset;
    private int mLastPosition;
    float mLastSelectionOffset;
    private int mLayoutDirection;
    protected int mSelectedIndicatorHeight;
    private final Paint mSelectedIndicatorPaint;
    int mSelectedPosition;
    float mSelectionOffset;
    private int mTabMediumSpacing;
    private int mTabSmallSpacing;

    public COUISlidingTabStrip(Context context, COUITabLayout cOUITabLayout) {
        super(context);
        this.mSelectedPosition = -1;
        this.mLayoutDirection = -1;
        this.mIndicatorLeft = -1;
        this.mIndicatorRight = -1;
        this.mLastPosition = 0;
        this.mIndicatorAnimTime = -1;
        this.mCOUITabLayout = cOUITabLayout;
        setWillNotDraw(false);
        this.mSelectedIndicatorPaint = new Paint();
        this.mBottomDividerPaint = new Paint();
        this.mIndicatorBackgroundPaint = new Paint();
        setGravity(17);
        this.mHorizontalLargeMargin = getResources().getDimensionPixelSize(R.dimen.coui_tab_layout_large_horizontal_margin);
        this.mHorizontalMediumMargin = getResources().getDimensionPixelSize(R.dimen.coui_tab_layout_medium_horizontal_margin);
        this.mHorizontalSmallMargin = getResources().getDimensionPixelSize(R.dimen.coui_tab_layout_small_horizontal_margin);
        this.mTabSmallSpacing = getResources().getDimensionPixelSize(R.dimen.coui_tab_layout_small_tab_spacing);
        this.mTabMediumSpacing = getResources().getDimensionPixelSize(R.dimen.coui_tab_layout_medium_tab_spacing);
        this.mContentMinWidth = getResources().getDimensionPixelSize(R.dimen.coui_tab_layout_content_min_width);
    }

    private boolean isLayoutRTL() {
        return ViewCompat.getLayoutDirection(this) == 1;
    }

    private void measureChildWithRedDot(COUITabView cOUITabView, int i2, int i6) {
        if (cOUITabView.getTextView() != null) {
            cOUITabView.getTextView().getLayoutParams().width = -2;
        }
        if (cOUITabView.getTextView() == null || cOUITabView.getHintRedDot() == null || cOUITabView.getHintRedDot().getVisibility() == 8) {
            cOUITabView.measure(i2, i6);
            return;
        }
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) cOUITabView.getHintRedDot().getLayoutParams();
        layoutParams.gravity = 48;
        if (cOUITabView.getHintRedDot().getPointMode() == 0) {
            layoutParams.leftMargin = 0;
            layoutParams.rightMargin = 0;
            cOUITabView.measure(i2, i6);
            return;
        }
        if (isLayoutRTL()) {
            layoutParams.rightMargin = this.mCOUITabLayout.mDotHorizontalOffset;
        } else {
            layoutParams.leftMargin = this.mCOUITabLayout.mDotHorizontalOffset;
        }
        if (cOUITabView.getHintRedDot().getPointMode() == 2) {
            layoutParams.topMargin = this.mCOUITabLayout.mDotVerticalOffsetFromNumberRed;
        } else {
            layoutParams.topMargin = this.mCOUITabLayout.mDotVerticalOffsetFromOnlyRed;
        }
        cOUITabView.measure(View.MeasureSpec.makeMeasureSpec(0, 0), i6);
        if (cOUITabView.getMeasuredWidth() > this.mCOUITabLayout.mRequestedTabMaxWidth) {
            cOUITabView.getTextView().getLayoutParams().width = ((this.mCOUITabLayout.mRequestedTabMaxWidth - cOUITabView.getHintRedDot().getMeasuredWidth()) - layoutParams.getMarginStart()) + layoutParams.getMarginEnd();
            cOUITabView.measure(i2, i6);
        }
    }

    private void measureShortChild(int i2, int i6, int i10, int i11) {
        int childCount = getChildCount();
        int i12 = ((i2 - i6) - (i10 * childCount)) / 2;
        int i13 = i10 / 2;
        setLayoutPadding(i12, i12);
        for (int i14 = 0; i14 < childCount; i14++) {
            View childAt = getChildAt(i14);
            setMargin(childAt, i13, i13, childAt.getMeasuredWidth());
        }
    }

    private void measureSmallChild(int i2, int i6, int i10) {
        int iMax;
        int i11;
        int childCount = getChildCount();
        int i12 = this.mContentMinWidth;
        if (i2 >= i12) {
            iMax = Math.max((i12 - i6) / (childCount + 1), i10);
            i11 = ((i2 - this.mContentMinWidth) + iMax) / 2;
        } else {
            iMax = Math.max((i2 - i6) / (childCount + 1), i10);
            i11 = iMax / 2;
        }
        int i13 = iMax / 2;
        setLayoutPadding(i11, i11);
        for (int i14 = 0; i14 < childCount; i14++) {
            View childAt = getChildAt(i14);
            setMargin(childAt, i13, i13, childAt.getMeasuredWidth());
        }
    }

    private int parseMinDivider(int i2) {
        if (i2 != -1) {
            return i2;
        }
        int measuredWidth = ((COUITabLayout) getParent()).getMeasuredWidth();
        int measuredHeight = ((COUITabLayout) getParent()).getMeasuredHeight();
        return (COUIResponsiveUtils.isMediumScreen(getContext(), measuredWidth, measuredHeight) || COUIResponsiveUtils.isLargeScreen(getContext(), measuredWidth, measuredHeight)) ? this.mTabMediumSpacing : this.mTabSmallSpacing;
    }

    private int parseMinMargin(int i2) {
        if (i2 != -1) {
            return i2;
        }
        int measuredWidth = ((COUITabLayout) getParent()).getMeasuredWidth();
        return COUIResponsiveUtils.isLargeScreen(getContext(), measuredWidth, UIUtil.getScreenHeightMetrics(getContext())) ? this.mHorizontalLargeMargin : COUIResponsiveUtils.isMediumScreen(getContext(), measuredWidth, UIUtil.getScreenHeightMetrics(getContext())) ? this.mHorizontalMediumMargin : this.mHorizontalSmallMargin;
    }

    private void setLayoutPadding(int i2, int i6) {
        if (getParent() == null || !(getParent() instanceof COUITabLayout)) {
            return;
        }
        ((COUITabLayout) getParent()).setPaddingLeftAndRight(i2, i6);
    }

    private void setMargin(View view, int i2, int i6, int i10) {
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) view.getLayoutParams();
        layoutParams.width = i10 + i6 + i2;
        view.setPaddingRelative(i2, view.getPaddingTop(), i6, view.getPaddingBottom());
        view.measure(View.MeasureSpec.makeMeasureSpec(layoutParams.width, 1073741824), View.MeasureSpec.makeMeasureSpec(view.getMeasuredHeight(), 1073741824));
    }

    public void animateIndicatorToPosition(final int i2, int i6) {
        boolean z6;
        COUISlidingTabStrip cOUISlidingTabStrip;
        final int i10;
        int i11;
        ValueAnimator valueAnimator = this.mIndicatorAnimator;
        if (valueAnimator == null || !valueAnimator.isRunning()) {
            z6 = false;
        } else if (i2 != this.mLastPosition) {
            this.mIndicatorAnimator.end();
            z6 = false;
        } else {
            this.mIndicatorAnimator.cancel();
            z6 = true;
        }
        boolean z10 = ViewCompat.getLayoutDirection(this) == 1;
        View childAt = getChildAt(i2);
        if (childAt == null) {
            updateIndicatorPosition();
            return;
        }
        final COUITabView cOUITabView = (COUITabView) childAt;
        final COUITabView cOUITabView2 = (COUITabView) getChildAt(this.mCOUITabLayout.getSelectedTabPosition());
        if (cOUITabView.getTextView() == null || cOUITabView.mCustomView != null) {
            cOUISlidingTabStrip = this;
            final int indicatorLeft = cOUISlidingTabStrip.getIndicatorLeft(cOUITabView.getLeft() + cOUITabView.mCustomView.getLeft());
            final int indicatorRight = cOUISlidingTabStrip.getIndicatorRight(cOUITabView.getLeft() + cOUITabView.mCustomView.getRight());
            if (Math.abs(i2 - cOUISlidingTabStrip.mSelectedPosition) <= 1) {
                i10 = cOUISlidingTabStrip.mIndicatorLeft;
                i11 = cOUISlidingTabStrip.mIndicatorRight;
            } else {
                int iDpToPx = cOUISlidingTabStrip.dpToPx(24);
                i10 = (i2 >= cOUISlidingTabStrip.mSelectedPosition ? !z10 : z10) ? indicatorLeft - iDpToPx : iDpToPx + indicatorRight;
                i11 = i10;
            }
            if (i10 != indicatorLeft || i11 != indicatorRight) {
                ValueAnimator valueAnimator2 = new ValueAnimator();
                cOUISlidingTabStrip.mIndicatorAnimator = valueAnimator2;
                valueAnimator2.setInterpolator(COUIAnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR);
                valueAnimator2.setDuration(i6);
                valueAnimator2.setFloatValues(0.0f, 1.0f);
                final int i12 = i11;
                valueAnimator2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator3) {
                        float animatedFraction = valueAnimator3.getAnimatedFraction();
                        COUISlidingTabStrip.this.setIndicatorPosition(COUIAnimationUtils.lerp(i10, indicatorLeft, animatedFraction), COUIAnimationUtils.lerp(i12, indicatorRight, animatedFraction));
                    }
                });
                valueAnimator2.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        COUISlidingTabStrip cOUISlidingTabStrip2 = COUISlidingTabStrip.this;
                        cOUISlidingTabStrip2.mSelectedPosition = i2;
                        cOUISlidingTabStrip2.mSelectionOffset = 0.0f;
                        if (cOUITabView.getTextView() != null) {
                            cOUITabView.getTextView().setTextColor(COUISlidingTabStrip.this.mCOUITabLayout.mSelectedTextColor);
                        }
                        if (cOUITabView2.getTextView() != null) {
                            cOUITabView2.getTextView().setTextColor(COUISlidingTabStrip.this.mCOUITabLayout.mNormalTextColor);
                        }
                    }
                });
                valueAnimator2.start();
            }
        } else {
            final TextView textView = cOUITabView.getTextView();
            final int i13 = this.mIndicatorLeft;
            final int i14 = this.mIndicatorRight;
            int indicatorPadding = this.mCOUITabLayout.getIndicatorPadding();
            final int indicatorLeft2 = getIndicatorLeft((cOUITabView.getLeft() + textView.getLeft()) - indicatorPadding);
            final int indicatorRight2 = getIndicatorRight(cOUITabView.getLeft() + textView.getRight() + indicatorPadding);
            final int i15 = (indicatorRight2 - indicatorLeft2) - (i14 - i13);
            final int i16 = indicatorLeft2 - i13;
            int indicatorAnimTime = this.mCOUITabLayout.getIndicatorAnimTime(i2, this.mSelectedPosition);
            int i17 = this.mIndicatorAnimTime;
            if (i17 != -1) {
                indicatorAnimTime = i17;
            }
            ValueAnimator valueAnimator3 = new ValueAnimator();
            this.mIndicatorAnimator = valueAnimator3;
            valueAnimator3.setDuration(indicatorAnimTime);
            valueAnimator3.setInterpolator(new COUIEaseInterpolator());
            valueAnimator3.setIntValues(0, 1);
            final ArgbEvaluator argbEvaluator = new ArgbEvaluator();
            final int currentTextColor = z6 ? textView.getCurrentTextColor() : this.mCOUITabLayout.mNormalTextColor;
            final int currentTextColor2 = z6 ? cOUITabView2.getTextView().getCurrentTextColor() : this.mCOUITabLayout.mSelectedTextColor;
            valueAnimator3.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator4) {
                    int i18;
                    int i19;
                    float animatedFraction = valueAnimator4.getAnimatedFraction();
                    textView.setTextColor(((Integer) argbEvaluator.evaluate(animatedFraction, Integer.valueOf(currentTextColor), Integer.valueOf(COUISlidingTabStrip.this.mCOUITabLayout.mSelectedTextColor))).intValue());
                    COUITabView cOUITabView3 = cOUITabView2;
                    if (cOUITabView3 != null && cOUITabView3.getTextView() != null) {
                        cOUITabView2.getTextView().setTextColor(((Integer) argbEvaluator.evaluate(animatedFraction, Integer.valueOf(currentTextColor2), Integer.valueOf(COUISlidingTabStrip.this.mCOUITabLayout.mNormalTextColor))).intValue());
                    }
                    COUISlidingTabStrip cOUISlidingTabStrip2 = COUISlidingTabStrip.this;
                    if (cOUISlidingTabStrip2.mLastOffset == 0.0f) {
                        cOUISlidingTabStrip2.mLastOffset = animatedFraction;
                    }
                    if (animatedFraction - cOUISlidingTabStrip2.mLastOffset > 0.0f) {
                        int i20 = i14;
                        i18 = (int) ((i20 - i13) + (i15 * animatedFraction));
                        i19 = (int) (i13 + (i16 * animatedFraction));
                    } else {
                        int i21 = indicatorRight2;
                        float f2 = 1.0f - animatedFraction;
                        i18 = (int) ((i21 - indicatorLeft2) - (i15 * f2));
                        i19 = (int) (indicatorLeft2 - (i16 * f2));
                    }
                    cOUISlidingTabStrip2.setIndicatorPosition(i19, i18 + i19);
                }
            });
            cOUISlidingTabStrip = this;
            valueAnimator3.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    COUISlidingTabStrip cOUISlidingTabStrip2 = COUISlidingTabStrip.this;
                    cOUISlidingTabStrip2.mSelectedPosition = i2;
                    cOUISlidingTabStrip2.mSelectionOffset = 0.0f;
                    cOUISlidingTabStrip2.updateIndicatorPosition();
                    COUISlidingTabStrip.this.mCOUITabLayout.resetTextColorAfterAnim();
                }
            });
            valueAnimator3.start();
        }
        cOUISlidingTabStrip.mLastPosition = cOUISlidingTabStrip.mCOUITabLayout.getSelectedTabPosition();
    }

    public boolean childrenNeedLayout() {
        int childCount = getChildCount();
        for (int i2 = 0; i2 < childCount; i2++) {
            if (getChildAt(i2).getWidth() <= 0) {
                return true;
            }
        }
        return false;
    }

    public int dpToPx(int i2) {
        return Math.round(getResources().getDisplayMetrics().density * i2);
    }

    public Paint getBottomDividerPaint() {
        return this.mBottomDividerPaint;
    }

    public int getIndicatorAnimTime() {
        return this.mIndicatorAnimTime;
    }

    public int getIndicatorBackgroundHeight() {
        return this.mIndicatorBackgroundHeight;
    }

    public int getIndicatorBackgroundPaddingLeft() {
        return this.mIndicatorBackgroundPaddingLeft;
    }

    public int getIndicatorBackgroundPaddingRight() {
        return this.mIndicatorBackgroundPaddingRight;
    }

    public Paint getIndicatorBackgroundPaint() {
        return this.mIndicatorBackgroundPaint;
    }

    public int getIndicatorLeft() {
        return this.mIndicatorLeft;
    }

    public float getIndicatorPosition() {
        return this.mSelectedPosition + this.mSelectionOffset;
    }

    public int getIndicatorRight() {
        return this.mIndicatorRight;
    }

    public float getIndicatorWidthRatio() {
        return this.mIndicatorWidthRatio;
    }

    public Paint getSelectedIndicatorPaint() {
        return this.mSelectedIndicatorPaint;
    }

    @Override
    public void onLayout(boolean z6, int i2, int i6, int i10, int i11) {
        super.onLayout(z6, i2, i6, i10, i11);
        if (this.mCOUITabLayout.isUpdateindicatorposition()) {
            updateIndicatorPosition();
        }
        if (this.mCOUITabLayout.mTabAlreadyMeasure) {
            return;
        }
        ValueAnimator valueAnimator = this.mIndicatorAnimator;
        if (valueAnimator != null && valueAnimator.isRunning()) {
            this.mIndicatorAnimator.cancel();
            animateIndicatorToPosition(this.mSelectedPosition, Math.round((1.0f - this.mIndicatorAnimator.getAnimatedFraction()) * this.mIndicatorAnimator.getDuration()));
        }
        COUITabLayout cOUITabLayout = this.mCOUITabLayout;
        cOUITabLayout.mTabAlreadyMeasure = true;
        cOUITabLayout.setScrollPosition(this.mSelectedPosition, 0.0f, true, true);
    }

    @Override
    public void onMeasure(int i2, int i6) {
        if (View.MeasureSpec.getMode(i2) == 0) {
            return;
        }
        int size = View.MeasureSpec.getSize(i2);
        int childCount = getChildCount();
        if (childCount == 0) {
            super.onMeasure(i2, i6);
            return;
        }
        int minMargin = parseMinMargin(this.mCOUITabLayout.getTabMinMargin());
        int minDivider = parseMinDivider(this.mCOUITabLayout.getTabMinDivider());
        if (this.mCOUITabLayout.getTabMode() == 1) {
            this.mIndicatorWidthRatio = this.mCOUITabLayout.getDefaultIndicatoRatio();
            int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(this.mCOUITabLayout.mRequestedTabMaxWidth, Integer.MIN_VALUE);
            int measuredWidth = 0;
            for (int i10 = 0; i10 < childCount; i10++) {
                COUITabView cOUITabView = (COUITabView) getChildAt(i10);
                setMargin(cOUITabView, 0, 0);
                measureChildWithRedDot(cOUITabView, iMakeMeasureSpec, i6);
                measuredWidth += cOUITabView.getMeasuredWidth();
            }
            int i11 = (minMargin * 2) + measuredWidth + ((childCount - 1) * minDivider);
            if (i11 <= this.mContentMinWidth) {
                measureSmallChild(size, measuredWidth, minDivider);
            } else if (i11 <= size) {
                measureShortChild(size, measuredWidth, minDivider, minMargin);
            } else {
                int i12 = minDivider / 2;
                int i13 = minMargin - i12;
                setLayoutPadding(i13, i13);
                for (int i14 = 0; i14 < childCount; i14++) {
                    View childAt = getChildAt(i14);
                    setMargin(childAt, i12, i12, childAt.getMeasuredWidth());
                }
            }
        } else {
            int iMakeMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(this.mCOUITabLayout.mRequestedTabMaxWidth, Integer.MIN_VALUE);
            int i15 = minDivider / 2;
            int i16 = minMargin - i15;
            setLayoutPadding(i16, i16);
            for (int i17 = 0; i17 < childCount; i17++) {
                View childAt2 = getChildAt(i17);
                setMargin(childAt2, 0, 0);
                measureChildWithRedDot((COUITabView) childAt2, iMakeMeasureSpec2, i6);
                setMargin(childAt2, i15, i15, childAt2.getMeasuredWidth());
            }
        }
        int measuredWidth2 = 0;
        for (int i18 = 0; i18 < childCount; i18++) {
            measuredWidth2 += getChildAt(i18).getMeasuredWidth();
        }
        super.onMeasure(View.MeasureSpec.makeMeasureSpec(measuredWidth2, 1073741824), i6);
    }

    @Override
    public void onRtlPropertiesChanged(int i2) {
        super.onRtlPropertiesChanged(i2);
    }

    public void setBottomDividerColor(int i2) {
        this.mBottomDividerPaint.setColor(i2);
        ViewCompat.postInvalidateOnAnimation(this.mCOUITabLayout);
    }

    public void setIndicatorAnimTime(int i2) {
        this.mIndicatorAnimTime = i2;
    }

    public void setIndicatorBackgroundHeight(int i2) {
        this.mIndicatorBackgroundHeight = i2;
    }

    public void setIndicatorBackgroundPaddingLeft(int i2) {
        this.mIndicatorBackgroundPaddingLeft = i2;
    }

    public void setIndicatorBackgroundPaddingRight(int i2) {
        this.mIndicatorBackgroundPaddingRight = i2;
    }

    public void setIndicatorLeft(int i2) {
        this.mIndicatorLeft = i2;
    }

    public void setIndicatorPosition(int i2, int i6) {
        int i10 = (i2 + i6) / 2;
        int iMax = Math.max(i6 - i2, dpToPx(32)) / 2;
        int i11 = i10 - iMax;
        int i12 = i10 + iMax;
        if (i11 == this.mIndicatorLeft && i12 == this.mIndicatorRight) {
            return;
        }
        this.mIndicatorLeft = i11;
        this.mIndicatorRight = i12;
        ViewCompat.postInvalidateOnAnimation(this.mCOUITabLayout);
    }

    public void setIndicatorPositionFromTabPosition(int i2, float f2) {
        ValueAnimator valueAnimator = this.mIndicatorAnimator;
        if (valueAnimator != null && valueAnimator.isRunning()) {
            this.mIndicatorAnimator.cancel();
        }
        this.mSelectedPosition = i2;
        this.mSelectionOffset = f2;
        updateIndicatorPosition();
    }

    public void setIndicatorRight(int i2) {
        this.mIndicatorRight = i2;
    }

    public void setIndicatorWidthRatio(float f2) {
        this.mIndicatorWidthRatio = f2;
    }

    public void setSelectedIndicatorColor(int i2) {
        this.mSelectedIndicatorPaint.setColor(i2);
        ViewCompat.postInvalidateOnAnimation(this.mCOUITabLayout);
    }

    public void setSelectedIndicatorHeight(int i2) {
        if (this.mSelectedIndicatorHeight != i2) {
            this.mSelectedIndicatorHeight = i2;
            ViewCompat.postInvalidateOnAnimation(this.mCOUITabLayout);
        }
    }

    public void updateIndicatorPosition() {
        View selectedChild = getChildAt(this.mSelectedPosition);
        COUITabView selectedTabView = (COUITabView) getChildAt(this.mSelectedPosition);
        boolean hasTextViewContent = selectedTabView != null
                && selectedTabView.getTextView() != null
                && selectedTabView.mCustomView == null;
        boolean hasCustomViewContent = selectedTabView != null && selectedTabView.mCustomView != null;
        int left = -1;
        int right = -1;
        if (hasTextViewContent || hasCustomViewContent) {
            View contentView = hasTextViewContent ? selectedTabView.getTextView() : selectedTabView.mCustomView;
            if (contentView.getWidth() > 0) {
                left = (selectedTabView.getLeft() + contentView.getLeft()) - this.mCOUITabLayout.getIndicatorPadding();
                right = selectedTabView.getLeft() + contentView.getRight() + this.mCOUITabLayout.getIndicatorPadding();
                if (this.mSelectionOffset > 0.0f && this.mSelectedPosition < getChildCount() - 1) {
                    COUITabView nextTabView = (COUITabView) getChildAt(this.mSelectedPosition + 1);
                    View nextContentView = nextTabView.mCustomView != null ? nextTabView.mCustomView : nextTabView.getTextView();
                    int nextLeft;
                    int nextRight;
                    if (nextContentView != null) {
                        nextLeft = (nextTabView.getLeft() + nextContentView.getLeft()) - this.mCOUITabLayout.getIndicatorPadding();
                        nextRight = nextTabView.getLeft() + nextContentView.getRight() + this.mCOUITabLayout.getIndicatorPadding();
                    } else {
                        nextLeft = nextTabView.getLeft();
                        nextRight = nextTabView.getRight();
                    }
                    int nextWidth = nextRight - nextLeft;
                    int width = right - left;
                    int widthDiff = nextWidth - width;
                    int leftDiff = nextLeft - left;
                    if (this.mLastSelectionOffset == 0.0f) {
                        this.mLastSelectionOffset = this.mSelectionOffset;
                    }
                    float offset = this.mSelectionOffset;
                    if (offset - this.mLastSelectionOffset > 0.0f) {
                        width = (int) (width + (widthDiff * offset));
                        left = (int) (left + (leftDiff * offset));
                    } else {
                        width = (int) (nextWidth - (widthDiff * (1.0f - offset)));
                        left = (int) (nextLeft - (leftDiff * (1.0f - offset)));
                    }
                    right = left + width;
                    this.mLastSelectionOffset = offset;
                }
                left = getIndicatorLeft(left);
                right = getIndicatorRight(right);
            }
        } else if (selectedChild != null && selectedChild.getWidth() > 0) {
            left = selectedChild.getLeft();
            right = selectedChild.getRight();
            if (this.mSelectionOffset > 0.0f && this.mSelectedPosition < getChildCount() - 1) {
                View nextChild = getChildAt(this.mSelectedPosition + 1);
                float offset = this.mSelectionOffset;
                left = (int) ((offset * nextChild.getLeft()) + ((1.0f - offset) * left));
                right = (int) ((offset * nextChild.getRight()) + ((1.0f - offset) * right));
            }
        }
        setIndicatorPosition(left, right);
    }

    private int getIndicatorLeft(int i2) {
        int width = ((this.mCOUITabLayout.getWidth() - this.mCOUITabLayout.getPaddingLeft()) - this.mCOUITabLayout.getPaddingRight()) - getWidth();
        return (!isLayoutRTL() || width <= 0) ? i2 : i2 + width;
    }

    private int getIndicatorRight(int i2) {
        int width = ((this.mCOUITabLayout.getWidth() - this.mCOUITabLayout.getPaddingLeft()) - this.mCOUITabLayout.getPaddingRight()) - getWidth();
        return (!isLayoutRTL() || width <= 0) ? i2 : i2 + width;
    }

    private void setMargin(View view, int i2, int i6) {
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) view.getLayoutParams();
        ViewCompat.setPaddingRelative(view, 0, view.getPaddingTop(), 0, view.getPaddingBottom());
        layoutParams.setMarginStart(i2);
        layoutParams.setMarginEnd(i6);
    }
}






