package com.android.launcher3.customer.seekbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Html;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import com.android.launcher3.R;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.math.BigDecimal;
import java.text.NumberFormat;

public class SignSeekBar extends View {
    static final int NONE = -1;
    private int barRoundingRadius;
    float dx;
    private boolean isAutoAdjustSectionMark;
    private boolean isFloatType;
    private boolean isSeekBySection;
    private boolean isShowProgressInFloat;
    private boolean isShowSectionMark;
    private boolean isShowSectionText;
    private boolean isShowSign;
    private boolean isShowSignBorder;
    private boolean isShowThumbShadow;
    private boolean isShowThumbText;
    private boolean isSidesLabels;
    private boolean isSignArrowAutofloat;
    public boolean isThumbOnDragging;
    private boolean isTouchToSeek;
    public boolean isTouchToSeekAnimEnd;
    private long mAnimDuration;
    private SignConfigBuilder mConfigBuilder;
    public float mDelta;
    private NumberFormat mFormat;
    public float mLeft;
    private float mMax;
    public float mMin;
    private Paint mPaint;
    private float mPreSecValue;
    public float mProgress;
    public OnProgressChangedListener mProgressListener;
    private Rect mRectText;
    private boolean mReverse;
    private float mRight;
    private int mSecondTrackColor;
    private int mSecondTrackSize;
    private int mSectionCount;
    private float mSectionOffset;
    private int mSectionTextColor;
    private int mSectionTextInterval;
    private int mSectionTextPosition;
    private int mSectionTextSize;
    private float mSectionValue;
    private String[] mSidesLabels;
    private int mSignArrowHeight;
    private int mSignArrowWidth;
    private int mSignBorderColor;
    private int mSignBorderSize;
    private int mSignColor;
    private int mSignHeight;
    private int mSignRound;
    private int mSignTextColor;
    private int mSignTextSize;
    private int mSignWidth;
    private int mTextSpace;
    private float mThumbBgAlpha;
    public float mThumbCenterX;
    private int mThumbColor;
    private int mThumbRadius;
    private int mThumbRadiusOnDragging;
    private float mThumbRatio;
    private int mThumbTextColor;
    private int mThumbTextSize;
    private int mTrackColor;
    public float mTrackLength;
    private int mTrackSize;
    private int mUnusableColor;
    private OnValueFormatListener mValueFormatListener;
    private Point point1;
    private Point point2;
    private Point point3;
    private RectF roundRectangleBounds;
    private Paint signPaint;
    private Paint signborderPaint;
    private Path trianglePath;
    private Path triangleboderPath;
    private boolean triggerSeekBySection;
    private String unit;
    private Rect valueSignBounds;
    private StaticLayout valueTextLayout;
    private TextPaint valueTextPaint;

    public interface OnProgressChangedListener {
        void getProgressOnActionUp(SignSeekBar signSeekBar, int i, float f);

        void getProgressOnFinally(SignSeekBar signSeekBar, int i, float f, boolean z);

        void onProgressChanged(SignSeekBar signSeekBar, int i, float f, boolean z);
    }

    public interface OnValueFormatListener {
        String format(float f);
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface TextPosition {
        public static final int BELOW_SECTION_MARK = 2;
        public static final int BOTTOM_SIDES = 1;
        public static final int SIDES = 0;
    }

    public SignSeekBar(Context context) {
        this(context, (AttributeSet) null);
    }

    public SignSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mSectionTextPosition = -1;
        boolean z = true;
        this.isTouchToSeekAnimEnd = true;
        this.barRoundingRadius = 0;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SignSeekBar, defStyleAttr, 0);
        this.mMin = a.getFloat(R.styleable.SignSeekBar_ssb_min, 0.0f);
        this.mMax = a.getFloat(R.styleable.SignSeekBar_ssb_max, 100.0f);
        this.mProgress = a.getFloat(R.styleable.SignSeekBar_ssb_progress, this.mMin);
        this.isFloatType = a.getBoolean(R.styleable.SignSeekBar_ssb_is_float_type, false);
        this.mTrackSize = a.getDimensionPixelSize(R.styleable.SignSeekBar_ssb_track_size, SignUtils.dp2px(2));
        this.mTextSpace = a.getDimensionPixelSize(R.styleable.SignSeekBar_ssb_text_space, SignUtils.dp2px(2));
        this.mSecondTrackSize = a.getDimensionPixelSize(R.styleable.SignSeekBar_ssb_second_track_size, this.mTrackSize + SignUtils.dp2px(2));
        this.mThumbRadius = a.getDimensionPixelSize(R.styleable.SignSeekBar_ssb_thumb_radius, this.mSecondTrackSize + SignUtils.dp2px(2));
        this.mThumbRadiusOnDragging = a.getDimensionPixelSize(R.styleable.SignSeekBar_ssb_thumb_radius, this.mSecondTrackSize * 2);
        this.mSignBorderSize = a.getDimensionPixelSize(R.styleable.SignSeekBar_ssb_sign_border_size, SignUtils.dp2px(1));
        this.mSectionCount = a.getInteger(R.styleable.SignSeekBar_ssb_section_count, 10);
        this.mTrackColor = a.getColor(R.styleable.SignSeekBar_ssb_track_color, ContextCompat.getColor(context, R.color.material_color_surface_variant));
        this.mSecondTrackColor = a.getColor(R.styleable.SignSeekBar_ssb_second_track_color, ContextCompat.getColor(context, R.color.material_color_primary));
        this.mThumbColor = a.getColor(R.styleable.SignSeekBar_ssb_thumb_color, this.mSecondTrackColor);
        this.isShowSectionText = a.getBoolean(R.styleable.SignSeekBar_ssb_show_section_text, false);
        this.mSectionTextSize = a.getDimensionPixelSize(R.styleable.SignSeekBar_ssb_section_text_size, SignUtils.sp2px(14));
        this.mSectionTextColor = a.getColor(R.styleable.SignSeekBar_ssb_section_text_color, this.mTrackColor);
        this.isSeekBySection = a.getBoolean(R.styleable.SignSeekBar_ssb_seek_by_section, false);
        int pos = a.getInteger(R.styleable.SignSeekBar_ssb_section_text_position, -1);
        if (pos == 0) {
            this.mSectionTextPosition = 0;
        } else if (pos == 1) {
            this.mSectionTextPosition = 1;
        } else if (pos == 2) {
            this.mSectionTextPosition = 2;
        } else {
            this.mSectionTextPosition = -1;
        }
        this.mSectionTextInterval = a.getInteger(R.styleable.SignSeekBar_ssb_section_text_interval, 1);
        this.isShowThumbText = a.getBoolean(R.styleable.SignSeekBar_ssb_show_thumb_text, false);
        this.mThumbTextSize = a.getDimensionPixelSize(R.styleable.SignSeekBar_ssb_thumb_text_size, SignUtils.sp2px(14));
        this.mThumbTextColor = a.getColor(R.styleable.SignSeekBar_ssb_thumb_text_color, this.mSecondTrackColor);
        this.mSignColor = a.getColor(R.styleable.SignSeekBar_ssb_sign_color, this.mSecondTrackColor);
        this.mSignBorderColor = a.getColor(R.styleable.SignSeekBar_ssb_sign_border_color, this.mSecondTrackColor);
        this.mUnusableColor = a.getColor(R.styleable.SignSeekBar_ssb_unusable_color, -7829368);
        this.mSignTextSize = a.getDimensionPixelSize(R.styleable.SignSeekBar_ssb_sign_text_size, SignUtils.sp2px(14));
        this.mSignHeight = a.getDimensionPixelSize(R.styleable.SignSeekBar_ssb_sign_height, SignUtils.dp2px(32));
        this.mSignWidth = a.getDimensionPixelSize(R.styleable.SignSeekBar_ssb_sign_width, SignUtils.dp2px(72));
        this.mSignArrowHeight = a.getDimensionPixelSize(R.styleable.SignSeekBar_ssb_sign_arrow_height, SignUtils.dp2px(3));
        this.mSignArrowWidth = a.getDimensionPixelSize(R.styleable.SignSeekBar_ssb_sign_arrow_width, SignUtils.dp2px(5));
        this.mSignRound = a.getDimensionPixelSize(R.styleable.SignSeekBar_ssb_sign_round, SignUtils.dp2px(3));
        this.mSignTextColor = a.getColor(R.styleable.SignSeekBar_ssb_sign_text_color, -1);
        this.isShowSectionMark = a.getBoolean(R.styleable.SignSeekBar_ssb_show_section_mark, false);
        this.isAutoAdjustSectionMark = a.getBoolean(R.styleable.SignSeekBar_ssb_auto_adjust_section_mark, false);
        this.isShowProgressInFloat = a.getBoolean(R.styleable.SignSeekBar_ssb_show_progress_in_float, false);
        int duration = a.getInteger(R.styleable.SignSeekBar_ssb_anim_duration, -1);
        this.mAnimDuration = duration < 0 ? 200 : (long) duration;
        this.isTouchToSeek = a.getBoolean(R.styleable.SignSeekBar_ssb_touch_to_seek, false);
        this.isShowSignBorder = a.getBoolean(R.styleable.SignSeekBar_ssb_sign_show_border, false);
        int labelsResId = a.getResourceId(R.styleable.SignSeekBar_ssb_sides_labels, 0);
        this.mThumbBgAlpha = a.getFloat(R.styleable.SignSeekBar_ssb_thumb_bg_alpha, 0.2f);
        this.mThumbRatio = a.getFloat(R.styleable.SignSeekBar_ssb_thumb_ratio, 0.7f);
        this.isShowThumbShadow = a.getBoolean(R.styleable.SignSeekBar_ssb_show_thumb_shadow, false);
        this.isShowSign = a.getBoolean(R.styleable.SignSeekBar_ssb_show_sign, false);
        this.isSignArrowAutofloat = a.getBoolean(R.styleable.SignSeekBar_ssb_sign_arrow_autofloat, true);
        a.recycle();
        Paint paint = new Paint();
        this.mPaint = paint;
        paint.setAntiAlias(true);
        this.mPaint.setStrokeCap(Paint.Cap.ROUND);
        this.mPaint.setTextAlign(Paint.Align.CENTER);
        this.mRectText = new Rect();
        if (labelsResId > 0) {
            this.mSidesLabels = getResources().getStringArray(labelsResId);
        }
        String[] strArr = this.mSidesLabels;
        this.isSidesLabels = (strArr == null || strArr.length <= 0) ? false : z;
        this.roundRectangleBounds = new RectF();
        this.valueSignBounds = new Rect();
        this.point1 = new Point();
        this.point2 = new Point();
        this.point3 = new Point();
        Path path = new Path();
        this.trianglePath = path;
        path.setFillType(Path.FillType.EVEN_ODD);
        this.triangleboderPath = new Path();
        init();
        initConfigByPriority();
    }

    private void init() {
        Paint paint = new Paint(1);
        this.signPaint = paint;
        paint.setStyle(Paint.Style.FILL);
        this.signPaint.setAntiAlias(true);
        this.signPaint.setColor(this.mSignColor);
        Paint paint2 = new Paint(1);
        this.signborderPaint = paint2;
        paint2.setStyle(Paint.Style.STROKE);
        this.signborderPaint.setStrokeWidth((float) this.mSignBorderSize);
        this.signborderPaint.setColor(this.mSignBorderColor);
        this.signborderPaint.setAntiAlias(true);
        TextPaint textPaint = new TextPaint(1);
        this.valueTextPaint = textPaint;
        textPaint.setStyle(Paint.Style.FILL);
        this.valueTextPaint.setTextSize((float) this.mSignTextSize);
        this.valueTextPaint.setColor(this.mSignTextColor);
    }

    private void initConfigByPriority() {
        if (this.mMin == this.mMax) {
            this.mMin = 0.0f;
            this.mMax = 100.0f;
        }
        float f = this.mMin;
        if (f > this.mMax) {
            float tmp = this.mMax;
            this.mMax = f;
            this.mMin = tmp;
        }
        float f2 = this.mProgress;
        float f3 = this.mMin;
        if (f2 < f3) {
            this.mProgress = f3;
        }
        float f4 = this.mProgress;
        float f5 = this.mMax;
        if (f4 > f5) {
            this.mProgress = f5;
        }
        if (this.mSectionCount <= 0) {
            this.mSectionCount = 10;
        }
        float f6 = f5 - f3;
        this.mDelta = f6;
        float f7 = f6 / ((float) this.mSectionCount);
        this.mSectionValue = f7;
        if (f7 < 1.0f) {
            this.isFloatType = true;
        }
        if (this.isFloatType) {
            this.isShowProgressInFloat = true;
        }
        int i = this.mSectionTextPosition;
        if (i != -1) {
            this.isShowSectionText = true;
        }
        if (this.isShowSectionText) {
            if (i == -1) {
                this.mSectionTextPosition = 0;
            }
            if (this.mSectionTextPosition == 2) {
                this.isShowSectionMark = true;
            }
        }
        if (this.mSectionTextInterval < 1) {
            this.mSectionTextInterval = 1;
        }
        if (this.isAutoAdjustSectionMark && !this.isShowSectionMark) {
            this.isAutoAdjustSectionMark = false;
        }
        if (this.isSeekBySection) {
            this.mPreSecValue = f3;
            if (this.mProgress != f3) {
                this.mPreSecValue = f7;
            }
            this.isShowSectionMark = true;
            this.isAutoAdjustSectionMark = true;
            this.isTouchToSeek = false;
        }
        setProgress(this.mProgress);
        this.mThumbTextSize = (this.isFloatType || this.isSeekBySection || (this.isShowSectionText && this.mSectionTextPosition == 2)) ? this.mSectionTextSize : this.mThumbTextSize;
    }
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        String str;
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int height = this.mThumbRadiusOnDragging * 2;
        String str2 = "j";
        if (this.isShowThumbText) {
            this.mPaint.setTextSize((float) this.mThumbTextSize);
            this.mPaint.getTextBounds(str2, 0, 1, this.mRectText);
            height += this.mRectText.height() + this.mTextSpace;
        }
        if (this.isShowSectionText && this.mSectionTextPosition >= 1) {
            if (this.isSidesLabels) {
                str2 = this.mSidesLabels[0];
            }
            String measuretext = str2;
            this.mPaint.setTextSize((float) this.mSectionTextSize);
            this.mPaint.getTextBounds(measuretext, 0, measuretext.length(), this.mRectText);
            height = Math.max(height, (this.mThumbRadiusOnDragging * 2) + this.mRectText.height() + this.mTextSpace);
        }
        int height2 = height + this.mSignHeight;
        if (this.isShowSignBorder) {
            height2 += this.mSignBorderSize;
        }
        setMeasuredDimension(resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec), height2);
        this.mLeft = (float) (getPaddingLeft() );
        this.mRight = (float) ((getMeasuredWidth() - getPaddingRight()));
        if (this.isShowSectionText) {
            this.mPaint.setTextSize((float) this.mSectionTextSize);
            int i = this.mSectionTextPosition;
            if (i == 0) {
                String text = getMinText();
                this.mPaint.getTextBounds(text, 0, text.length(), this.mRectText);
                this.mLeft += (float) (this.mRectText.width() + this.mTextSpace);
                String text2 = getMaxText();
                this.mPaint.getTextBounds(text2, 0, text2.length(), this.mRectText);
                this.mRight -= (float) (this.mRectText.width() + this.mTextSpace);
            } else if (i >= 1) {
                String text3 = this.isSidesLabels ? this.mSidesLabels[0] : getMinText();
                this.mPaint.getTextBounds(text3, 0, text3.length(), this.mRectText);
//                this.mLeft = ((float) getPaddingLeft()) + Math.max((float) this.mThumbRadiusOnDragging, ((float) this.mRectText.width()) / 2.0f) + ((float) this.mTextSpace);
                if (this.isSidesLabels) {
                    String[] strArr = this.mSidesLabels;
                    str = strArr[strArr.length - 1];
                } else {
                    str = getMaxText();
                }
                String text4 = str;
                this.mPaint.getTextBounds(text4, 0, text4.length(), this.mRectText);
//                this.mRight = (((float) (getMeasuredWidth() - getPaddingRight())) - Math.max((float) this.mThumbRadiusOnDragging, ((float) this.mRectText.width()) / 2.0f)) - ((float) this.mTextSpace);
            }
        } else if (this.isShowThumbText && this.mSectionTextPosition == -1) {
            this.mPaint.setTextSize((float) this.mThumbTextSize);
            String text5 = getMinText();
            this.mPaint.getTextBounds(text5, 0, text5.length(), this.mRectText);
            this.mLeft = ((float) getPaddingLeft()) + Math.max((float) this.mThumbRadiusOnDragging, ((float) this.mRectText.width()) / 2.0f) + ((float) this.mTextSpace);
            String text6 = getMaxText();
            this.mPaint.getTextBounds(text6, 0, text6.length(), this.mRectText);
            this.mRight = (((float) (getMeasuredWidth() - getPaddingRight())) - Math.max((float) this.mThumbRadiusOnDragging, ((float) this.mRectText.width()) / 2.0f)) - ((float) this.mTextSpace);
        }
        if (this.isShowSign && !this.isSignArrowAutofloat) {
            this.mLeft = Math.max(this.mLeft, (float) (getPaddingLeft() + (this.mSignWidth / 2) + this.mSignBorderSize));
            this.mRight = Math.min(this.mRight, (float) (((getMeasuredWidth() - getPaddingRight()) - (this.mSignWidth / 2)) - this.mSignBorderSize));
        }
        float f = this.mRight - this.mLeft;
        this.mTrackLength = f;
        this.mSectionOffset = (f) / ((float) this.mSectionCount);
    }

    @Override
    public void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        // 初始化基础参数（重命名反编译变量）
        float leftPadding = getPaddingLeft();  // 原r0
        float rightBound = (float) (getMeasuredWidth() - getPaddingRight());  // 原r1
        float topOffset = (float) (getPaddingTop() + mThumbRadiusOnDragging);  // 原r2

        // 处理标记高度偏移（替代goto L_0x0022逻辑）
        if (isShowSign) {
            topOffset += (float) mSignHeight;
        }

        // 处理标记边框偏移（替代goto L_0x002a逻辑）
        if (isShowSignBorder) {
            topOffset += (float) mSignBorderSize;
        }

        // 调整左右边界（替代goto L_0x003e逻辑）
        if (isShowSign && !isSignArrowAutofloat) {
            final float signHalfWidth = (float) (mSignWidth / 2 + mSignBorderSize);
            leftPadding += signHalfWidth;
            rightBound -= signHalfWidth;
        }

        // 绘制分段文本（替代原goto L_0x0134前的复杂逻辑）
        if (isShowSectionText) {
            mPaint.setTextSize((float) mSectionTextSize);
            mPaint.setColor(isEnabled() ? mSectionTextColor : mUnusableColor);

            switch (mSectionTextPosition) {
                case 0: // 两侧文本
                    drawSidesSectionText(canvas, leftPadding, rightBound, topOffset);
                    break;
                case 1: // 底部两侧文本
                case 2: // 分段标记下方文本
                    drawBottomOrMarkSectionText(canvas, leftPadding, rightBound, topOffset);
                    break;
            }
        }

        // 绘制滑块文本（简化条件判断）
        if (isShowThumbText && mSectionTextPosition == -1) {
            mPaint.setTextSize((float) mThumbTextSize);
            String minText = getMinText();
            String maxText = getMaxText();
            // 计算左右边界（保持原有逻辑）
            mPaint.getTextBounds(minText, 0, minText.length(), mRectText);
            float adjustedLeft = leftPadding + Math.max((float) mThumbRadiusOnDragging, (float) mRectText.width() / 2.0f) + (float) mTextSpace;
            mPaint.getTextBounds(maxText, 0, maxText.length(), mRectText);
            float adjustedRight = rightBound - Math.max((float) mThumbRadiusOnDragging, (float) mRectText.width() / 2.0f) - (float) mTextSpace;
            // 绘制逻辑（保持原有）
        }

        // 计算滑块中心位置（替代原goto L_0x017d逻辑）
        if (!isThumbOnDragging) {
            float progressRatio = (mProgress - mMin) / mDelta;
            mThumbCenterX = mLeft + progressRatio * mTrackLength;
        }

        // 绘制轨道
        drawTrack(canvas, leftPadding, mThumbCenterX, mSecondTrackColor, mSecondTrackSize, topOffset);
        drawTrack(canvas, mThumbCenterX, rightBound, mTrackColor, mTrackSize, topOffset);

        // 绘制滑块（简化阴影和主体绘制逻辑）
        if (isShowThumbShadow) {
            float shadowRadius = (isThumbOnDragging ? mThumbRadiusOnDragging : mThumbRadius) * mThumbRatio;
            mPaint.setColor(getColorWithAlpha(mThumbColor, mThumbBgAlpha));
            canvas.drawCircle(mThumbCenterX, topOffset, shadowRadius, mPaint);
        }
        mPaint.setColor(mThumbColor);
        float thumbRadius = isThumbOnDragging ? mThumbRadiusOnDragging : mThumbRadius;
        canvas.drawCircle(mThumbCenterX, topOffset, thumbRadius, mPaint);

        // 绘制标记（保持核心逻辑）
        if (isShowSign) {
            drawValueSign(canvas, mSignHeight, (int) mThumbCenterX);
        }
    }

    private void drawTrack(Canvas canvas, float startX, float endX, int color, int size, float topOffset) {
        mPaint.setColor(color);
        mPaint.setStrokeWidth((float) size);
        canvas.drawLine(startX, topOffset, endX, topOffset, mPaint);
    }

    private void drawSidesSectionText(Canvas canvas, float xLeft, float rightBound, float yTop) {
        if (!isShowSectionText || mSectionTextPosition != TextPosition.SIDES) return;

        mPaint.setTextSize(mSectionTextSize);
        mPaint.getTextBounds("0", 0, 1, mRectText); // 获取文本高度参考
        float textHeight = mRectText.height();
        float textY = yTop + textHeight + mTextSpace; // 侧边文本垂直位置

        for (int i = 0; i <= mSectionCount; i++) {
            float x = xLeft + (i * mSectionOffset);
            float sectionValue = mMin + (mSectionValue * i);
            String text = isSidesLabels ? mSidesLabels[i] : (isFloatType ? float2String(sectionValue) : ((int) sectionValue) + "");

            // 根据进度与分段值的距离调整颜色
            mPaint.setColor(Math.abs(mProgress - sectionValue) <= 0.045f ? mSectionTextColor : mUnusableColor);

            // 绘制侧边文本（左/右对齐需根据具体需求调整，示例为居中）
            canvas.drawText(text, x, textY, mPaint);
        }
    }

    private void drawBottomOrMarkSectionText(Canvas canvas, float xLeft, float rightBound, float yTop) {
        if (!isShowSectionText || (mSectionTextPosition != TextPosition.BOTTOM_SIDES && mSectionTextPosition != TextPosition.BELOW_SECTION_MARK)) return;

        mPaint.setTextSize(mSectionTextSize);
        mPaint.getTextBounds("0", 0, 1, mRectText);
        float textHeight = mRectText.height();
        float textY = yTop + mThumbRadiusOnDragging + mTextSpace + textHeight; // 标记下方或底部的垂直位置

        for (int i = 0; i <= mSectionCount; i++) {
            // 间隔过滤（仅绘制指定间隔的分段文本）
            if (mSectionTextInterval > 1 && i % mSectionTextInterval != 0) continue;

            float x = xLeft + (i * mSectionOffset);
            float sectionValue = mMin + (mSectionValue * i);
            String text = isSidesLabels ? mSidesLabels[i / mSectionTextInterval] : (isFloatType ? float2String(sectionValue) : ((int) sectionValue) + "");

            mPaint.setColor(Math.abs(mProgress - sectionValue) <= 0.045f ? mSectionTextColor : mUnusableColor);
            canvas.drawText(text, x, textY, mPaint);
        }
    }

    private void drawMark(Canvas canvas, float xLeft, float yTop, boolean isShowTextBelowSectionMark, boolean conditionInterval) {
        Canvas canvas2 = canvas;
        float f = yTop;
        float r = ((float) (this.mThumbRadiusOnDragging - SignUtils.dp2px(2))) / 2.0f;
        float junction = ((this.mTrackLength / this.mDelta) * Math.abs(this.mProgress - this.mMin)) + this.mLeft;
        this.mPaint.setTextSize((float) this.mSectionTextSize);
        this.mPaint.getTextBounds("0123456789", 0, "0123456789".length(), this.mRectText);
        float y_ = ((float) this.mRectText.height()) + f + ((float) this.mThumbRadiusOnDragging) + ((float) this.mTextSpace);
        for (int i = 0; i <= this.mSectionCount; i++) {
            float x_ = xLeft + (((float) i) * this.mSectionOffset);
            this.mPaint.setColor(x_ <= junction ? this.mSecondTrackColor : this.mTrackColor);
            canvas.drawCircle(x_, f, r, this.mPaint);
            if (isShowTextBelowSectionMark) {
                float m = this.mMin + (this.mSectionValue * ((float) i));
                this.mPaint.setColor(((double) Math.abs(this.mProgress - m)) <= 0.045d ? this.mSectionTextColor : this.mUnusableColor);
                int i2 = this.mSectionTextInterval;
                if (i2 > 1) {
                    if (conditionInterval && i % i2 == 0) {
                        if (this.isSidesLabels) {
                            canvas.drawText(this.mSidesLabels[i], x_, y_, this.mPaint);
                        } else {
                            canvas.drawText(this.isFloatType ? float2String(m) : ((int) m) + "", x_, y_, this.mPaint);
                        }
                    }
                } else if (conditionInterval && i % i2 == 0) {
                    if (this.isSidesLabels) {
                        int i3 = i / i2;
                        String[] strArr = this.mSidesLabels;
                        if (i3 <= strArr.length) {
                            canvas.drawText(strArr[i / i2], x_, y_, this.mPaint);
                        }
                    }
                    canvas.drawText(this.isFloatType ? float2String(m) : ((int) m) + "", x_, y_, this.mPaint);
                }
            }
        }
    }

    private void drawThumbText(android.graphics.Canvas canvas, float topOffset) {
        // 初始化画笔属性
        mPaint.setColor(mThumbTextColor);
        mPaint.setTextSize(mThumbTextSize);

        // 测量文本高度（用于计算绘制Y坐标）
        mPaint.getTextBounds("0123456789", 0, 10, mRectText);
        float textY = topOffset
                + mRectText.height()
                + mThumbRadiusOnDragging
                + mTextSpace; // 文本垂直位置 = 顶部偏移 + 文本高度 + 滑块半径 + 文本间距

        // 获取当前进度值（整数或浮点数）
        String formattedProgress;
        if (isFloatType) {
            formattedProgress = formatFloatProgress(getProgressFloat());
        } else {
            formattedProgress = formatIntProgress(getProgress());
        }

        // 应用自定义值格式化监听器（若存在）
        if (mValueFormatListener != null) {
            formattedProgress = mValueFormatListener.format(isFloatType ? getProgressFloat() : getProgress());
        }

        // 绘制滑块文本（居中对齐）
        drawSignText(canvas, formattedProgress, mThumbCenterX, textY, mPaint);
    }

    // 提取：浮点数进度格式化方法（保持原有逻辑）
    private String formatFloatProgress(float progress) {
        String progressStr = String.valueOf(progress);
        if (mFormat != null) {
            progressStr = mFormat.format(progress);
        }
        return appendUnitIfNeeded(progressStr);
    }

    // 提取：整数进度格式化方法（保持原有逻辑）
    private String formatIntProgress(int progress) {
        String progressStr = String.valueOf(progress);
        if (mFormat != null) {
            progressStr = mFormat.format(progress);
        }
        return appendUnitIfNeeded(progressStr);
    }

    // 提取：附加单位逻辑（保持原有逻辑）
    private String appendUnitIfNeeded(String progressStr) {
        if (unit == null || unit.isEmpty()) return progressStr;

        if (mReverse) {
            return unit + progressStr;
        } else {
            return progressStr + unit;
        }
    }

    public void drawSignText(Canvas canvas, String text, float x, float y, Paint paint) {
        canvas.drawText(text, x, y, paint);
    }

    private void drawValueSign(Canvas canvas, int valueSignSpaceHeight, int valueSignCenter) {
        this.valueSignBounds.set(valueSignCenter - (this.mSignWidth / 2), getPaddingTop(), (this.mSignWidth / 2) + valueSignCenter, (this.mSignHeight - this.mSignArrowHeight) + getPaddingTop());
        int bordersize = this.isShowSignBorder ? this.mSignBorderSize : 0;
        if (this.valueSignBounds.left < getPaddingLeft()) {
            int difference = (-this.valueSignBounds.left) + getPaddingLeft() + bordersize;
            this.roundRectangleBounds.set((float) (this.valueSignBounds.left + difference), (float) this.valueSignBounds.top, (float) (this.valueSignBounds.right + difference), (float) this.valueSignBounds.bottom);
        } else if (this.valueSignBounds.right > getMeasuredWidth() - getPaddingRight()) {
            int difference2 = (this.valueSignBounds.right - getMeasuredWidth()) + getPaddingRight() + bordersize;
            this.roundRectangleBounds.set((float) (this.valueSignBounds.left - difference2), (float) this.valueSignBounds.top, (float) (this.valueSignBounds.right - difference2), (float) this.valueSignBounds.bottom);
        } else {
            this.roundRectangleBounds.set((float) this.valueSignBounds.left, (float) this.valueSignBounds.top, (float) this.valueSignBounds.right, (float) this.valueSignBounds.bottom);
        }
        RectF rectF = this.roundRectangleBounds;
        int i = this.mSignRound;
        canvas.drawRoundRect(rectF, (float) i, (float) i, this.signPaint);
        if (this.isShowSignBorder) {
            this.roundRectangleBounds.top += (float) (this.mSignBorderSize / 2);
            RectF rectF2 = this.roundRectangleBounds;
            int i2 = this.mSignRound;
            canvas.drawRoundRect(rectF2, (float) i2, (float) i2, this.signborderPaint);
        }
        int i3 = this.isThumbOnDragging ? this.mThumbRadiusOnDragging : this.mThumbRadius;
        this.barRoundingRadius = i3;
        int difference3 = 0;
        if (valueSignCenter - (this.mSignArrowWidth / 2) < i3 + getPaddingLeft() + this.mTextSpace + bordersize) {
            difference3 = (this.barRoundingRadius - valueSignCenter) + getPaddingLeft() + bordersize + this.mTextSpace;
        } else if ((this.mSignArrowWidth / 2) + valueSignCenter > (((getMeasuredWidth() - this.barRoundingRadius) - getPaddingRight()) - this.mTextSpace) - bordersize) {
            difference3 = ((((getMeasuredWidth() - this.barRoundingRadius) - valueSignCenter) - getPaddingRight()) - bordersize) - this.mTextSpace;
        }
        this.point1.set((valueSignCenter - (this.mSignArrowWidth / 2)) + difference3, (valueSignSpaceHeight - this.mSignArrowHeight) + getPaddingTop());
        this.point2.set((this.mSignArrowWidth / 2) + valueSignCenter + difference3, (valueSignSpaceHeight - this.mSignArrowHeight) + getPaddingTop());
        this.point3.set(valueSignCenter + difference3, getPaddingTop() + valueSignSpaceHeight);
        drawTriangle(canvas, this.point1, this.point2, this.point3, this.signPaint);
        if (this.isShowSignBorder) {
            drawTriangleBoder(canvas, this.point1, this.point2, this.point3, this.signborderPaint);
        }
        createValueTextLayout();
        if (this.valueTextLayout != null) {
            canvas.translate(this.roundRectangleBounds.left, (this.roundRectangleBounds.top + (this.roundRectangleBounds.height() / 2.0f)) - ((float) (this.valueTextLayout.getHeight() / 2)));
            this.valueTextLayout.draw(canvas);
        }
    }

    private void drawTriangle(Canvas canvas, Point point12, Point point22, Point point32, Paint paint) {
        this.trianglePath.reset();
        this.trianglePath.moveTo((float) point12.x, (float) point12.y);
        this.trianglePath.lineTo((float) point22.x, (float) point22.y);
        this.trianglePath.lineTo((float) point32.x, (float) point32.y);
        this.trianglePath.lineTo((float) point12.x, (float) point12.y);
        this.trianglePath.close();
        canvas.drawPath(this.trianglePath, paint);
    }

    private void drawTriangleBoder(Canvas canvas, Point point12, Point point22, Point point32, Paint paint) {
        this.triangleboderPath.reset();
        this.triangleboderPath.moveTo((float) point12.x, (float) point12.y);
        this.triangleboderPath.lineTo((float) point22.x, (float) point22.y);
        paint.setColor(this.signPaint.getColor());
        int i = this.mSignBorderSize;
        float value = (float) (i / 6);
        paint.setStrokeWidth(((float) i) + 1.0f);
        canvas.drawPath(this.triangleboderPath, paint);
        this.triangleboderPath.reset();
        paint.setStrokeWidth((float) this.mSignBorderSize);
        this.triangleboderPath.moveTo(((float) point12.x) - value, ((float) point12.y) - value);
        this.triangleboderPath.lineTo((float) point32.x, (float) point32.y);
        this.triangleboderPath.lineTo(((float) point22.x) + value, ((float) point22.y) - value);
        paint.setColor(this.mSignBorderColor);
        canvas.drawPath(this.triangleboderPath, paint);
    }

    public void setUnit(String unit2) {
        this.unit = unit2;
        createValueTextLayout();
        invalidate();
        requestLayout();
    }

    public void setProgressWithUnit(float progress, String unitHtml) {
        setProgress(progress);
        this.unit = unitHtml;
        createValueTextLayout();
        invalidate();
        requestLayout();
    }

    private void createValueTextLayout() {
        String value;
        String str;
        if (this.isShowProgressInFloat) {
            float progress = getProgressFloat();
            value = String.valueOf(progress);
            NumberFormat numberFormat = this.mFormat;
            if (numberFormat != null) {
                value = numberFormat.format((double) progress);
            }
        } else {
            int progress2 = getProgress();
            value = String.valueOf(progress2);
            NumberFormat numberFormat2 = this.mFormat;
            if (numberFormat2 != null) {
                value = numberFormat2.format((long) progress2);
            }
        }
        OnValueFormatListener onValueFormatListener = this.mValueFormatListener;
        if (onValueFormatListener != null) {
            value = onValueFormatListener.format(Float.parseFloat(value));
        } else if (!(value == null || (str = this.unit) == null || str.isEmpty())) {
            if (!this.mReverse) {
                value = value + String.format(" <small>%s</small> ", new Object[]{this.unit});
            } else {
                value = String.format(" %s ", new Object[]{this.unit}) + value;
            }
        }
        this.valueTextLayout = new StaticLayout(Html.fromHtml(value), this.valueTextPaint, this.mSignWidth, Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);
    }

    private void drawProgressText(Canvas canvas) {
        String str;
        String value = this.isShowProgressInFloat ? String.valueOf(getProgressFloat()) : String.valueOf(getProgress());
        if (!(value == null || (str = this.unit) == null || str.isEmpty())) {
            value = value + String.format("%s", new Object[]{this.unit});
        }
        int i = this.isThumbOnDragging ? this.mThumbRadiusOnDragging : this.mThumbRadius;
        Paint mPartTextPaint = this.mPaint;
        mPartTextPaint.setColor(-16777216);
        mPartTextPaint.setTextSize(25.0f);
        drawCircleText(canvas, mPartTextPaint, this.mThumbCenterX, (float) (getPaddingTop() + this.mThumbRadiusOnDragging), (float) i, value);
    }

    private void drawCircleText(Canvas canvas, Paint paint, float centerX, float centerY, float radius, String text) {
        paint.setTextAlign(Paint.Align.LEFT);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        Paint.FontMetricsInt fontMetrics = paint.getFontMetricsInt();
        canvas.drawText(text, ((centerX - radius) + radius) - ((float) (bounds.width() / 2)), ((centerY - radius) + ((((radius * 2.0f) - ((float) fontMetrics.bottom)) + ((float) fontMetrics.top)) / 2.0f)) - ((float) fontMetrics.top), paint);
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        post(new Runnable() {
            public void run() {
                SignSeekBar.this.requestLayout();
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                boolean isThumbTouched = isThumbTouched(event);
                this.isThumbOnDragging = isThumbTouched;
                if (isThumbTouched) {
                    if (this.isSeekBySection && !this.triggerSeekBySection) {
                        this.triggerSeekBySection = true;
                    }
                    invalidate();
                } else if (this.isTouchToSeek && isTrackTouched(event)) {
                    this.isThumbOnDragging = true;
                    float x = event.getX();
                    this.mThumbCenterX = x;
                    float f = this.mLeft;
                    if (x < f) {
                        this.mThumbCenterX = f;
                    }
                    float f2 = this.mThumbCenterX;
                    float f3 = this.mRight;
                    if (f2 > f3) {
                        this.mThumbCenterX = f3;
                    }
                    this.mProgress = (((this.mThumbCenterX - f) * this.mDelta) / this.mTrackLength) + this.mMin;
                    invalidate();
                }
                this.dx = this.mThumbCenterX - event.getX();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                long j = 300;
                if (this.isAutoAdjustSectionMark) {
                    if (this.isTouchToSeek) {
                        if (this.isThumbOnDragging) {
                            j = 0;
                        }
                        postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                SignSeekBar.this.isTouchToSeekAnimEnd = false;
                                SignSeekBar.this.autoAdjustSection();
                            }
                        }, j);
                    } else {
                        autoAdjustSection();
                    }
                } else if (this.isThumbOnDragging || this.isTouchToSeek) {
                    ViewPropertyAnimator duration = animate().setDuration(this.mAnimDuration);
                    if (this.isThumbOnDragging || !this.isTouchToSeek) {
                        j = 0;
                    }
                    duration.setStartDelay(j).setListener(new AnimatorListenerAdapter() {
                        public void onAnimationEnd(Animator animation) {
                            SignSeekBar.this.isThumbOnDragging = false;
                            SignSeekBar.this.invalidate();
                            if (SignSeekBar.this.mProgressListener != null) {
                                OnProgressChangedListener r0 = SignSeekBar.this.mProgressListener;
                                SignSeekBar signSeekBar = SignSeekBar.this;
                                r0.onProgressChanged(signSeekBar, signSeekBar.getProgress(), SignSeekBar.this.getProgressFloat(), true);
                            }
                        }

                        public void onAnimationCancel(Animator animation) {
                            SignSeekBar.this.isThumbOnDragging = false;
                            SignSeekBar.this.invalidate();
                        }
                    }).start();
                }
                OnProgressChangedListener onProgressChangedListener = this.mProgressListener;
                if (onProgressChangedListener != null) {
                    onProgressChangedListener.getProgressOnActionUp(this, getProgress(), getProgressFloat());
                    break;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (this.isThumbOnDragging) {
                    float x2 = event.getX() + this.dx;
                    this.mThumbCenterX = x2;
                    float f4 = this.mLeft;
                    if (x2 < f4) {
                        this.mThumbCenterX = f4;
                    }
                    float f5 = this.mThumbCenterX;
                    float f6 = this.mRight;
                    if (f5 > f6) {
                        this.mThumbCenterX = f6;
                    }
                    this.mProgress = (((this.mThumbCenterX - f4) * this.mDelta) / this.mTrackLength) + this.mMin;
                    invalidate();
                    OnProgressChangedListener onProgressChangedListener2 = this.mProgressListener;
                    if (onProgressChangedListener2 != null) {
                        onProgressChangedListener2.onProgressChanged(this, getProgress(), getProgressFloat(), true);
                        break;
                    }
                }
                break;
        }
        if (this.isThumbOnDragging || this.isTouchToSeek || super.onTouchEvent(event)) {
            return true;
        }
        return false;
    }

    public int getColorWithAlpha(int color, float ratio) {
        return Color.argb(Math.round(((float) Color.alpha(color)) * ratio), Color.red(color), Color.green(color), Color.blue(color));
    }

    private boolean isThumbTouched(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        float mCircleR = (float) (this.isThumbOnDragging ? this.mThumbRadiusOnDragging : this.mThumbRadius);
        float x = ((this.mTrackLength / this.mDelta) * (this.mProgress - this.mMin)) + this.mLeft;
        float y = ((float) getMeasuredHeight()) / 2.0f;
        float x2 = ((event.getX() - x) * (event.getX() - x)) + ((event.getY() - y) * (event.getY() - y));
        float f = this.mLeft;
        if (x2 <= (f + mCircleR) * (f + mCircleR)) {
            return true;
        }
        return false;
    }

    private boolean isTrackTouched(MotionEvent event) {
        return isEnabled() && event.getX() >= ((float) getPaddingLeft()) && event.getX() <= ((float) (getMeasuredWidth() - getPaddingRight())) && event.getY() >= ((float) getPaddingTop()) && event.getY() <= ((float) (getMeasuredHeight() - getPaddingBottom()));
    }

    public void autoAdjustSection() {
        float x = 0.0f;
        int i = 0;
        while (i <= this.mSectionCount) {
            float f = this.mSectionOffset;
            x = (((float) i) * f) + this.mLeft;
            float f2 = this.mThumbCenterX;
            if (x <= f2 && f2 - x <= f) {
                break;
            }
            i++;
        }
        boolean onSection = BigDecimal.valueOf((double) this.mThumbCenterX).setScale(1, 4).floatValue() == x;
        AnimatorSet animatorSet = new AnimatorSet();
        ValueAnimator valueAnim = null;
        if (!onSection) {
            float f3 = this.mThumbCenterX;
            float f4 = this.mSectionOffset;
            if (f3 - x <= f4 / 2.0f) {
                valueAnim = ValueAnimator.ofFloat(new float[]{f3, x});
            } else {
                valueAnim = ValueAnimator.ofFloat(new float[]{f3, (((float) (i + 1)) * f4) + this.mLeft});
            }
            valueAnim.setInterpolator(new LinearInterpolator());
            valueAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    SignSeekBar.this.mThumbCenterX = ((Float) animation.getAnimatedValue()).floatValue();
                    SignSeekBar signSeekBar = SignSeekBar.this;
                    signSeekBar.mProgress = (((signSeekBar.mThumbCenterX - SignSeekBar.this.mLeft) * SignSeekBar.this.mDelta) / SignSeekBar.this.mTrackLength) + SignSeekBar.this.mMin;
                    SignSeekBar.this.invalidate();
                    if (SignSeekBar.this.mProgressListener != null) {
                        OnProgressChangedListener r0 = SignSeekBar.this.mProgressListener;
                        SignSeekBar signSeekBar2 = SignSeekBar.this;
                        r0.onProgressChanged(signSeekBar2, signSeekBar2.getProgress(), SignSeekBar.this.getProgressFloat(), true);
                    }
                }
            });
        }
        if (!onSection) {
            animatorSet.setDuration(this.mAnimDuration).playTogether(new Animator[]{valueAnim});
        }
        animatorSet.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                SignSeekBar signSeekBar = SignSeekBar.this;
                signSeekBar.mProgress = (((signSeekBar.mThumbCenterX - SignSeekBar.this.mLeft) * SignSeekBar.this.mDelta) / SignSeekBar.this.mTrackLength) + SignSeekBar.this.mMin;
                SignSeekBar.this.isThumbOnDragging = false;
                SignSeekBar.this.isTouchToSeekAnimEnd = true;
                SignSeekBar.this.invalidate();
                if (SignSeekBar.this.mProgressListener != null) {
                    OnProgressChangedListener r0 = SignSeekBar.this.mProgressListener;
                    SignSeekBar signSeekBar2 = SignSeekBar.this;
                    r0.getProgressOnFinally(signSeekBar2, signSeekBar2.getProgress(), SignSeekBar.this.getProgressFloat(), true);
                }
            }

            public void onAnimationCancel(Animator animation) {
                SignSeekBar signSeekBar = SignSeekBar.this;
                signSeekBar.mProgress = (((signSeekBar.mThumbCenterX - SignSeekBar.this.mLeft) * SignSeekBar.this.mDelta) / SignSeekBar.this.mTrackLength) + SignSeekBar.this.mMin;
                SignSeekBar.this.isThumbOnDragging = false;
                SignSeekBar.this.isTouchToSeekAnimEnd = true;
                SignSeekBar.this.invalidate();
            }
        });
        animatorSet.start();
    }

    private String getMinText() {
        return this.isFloatType ? float2String(this.mMin) : String.valueOf((int) this.mMin);
    }

    private String getMaxText() {
        return this.isFloatType ? float2String(this.mMax) : String.valueOf((int) this.mMax);
    }

    public float getMin() {
        return this.mMin;
    }

    public float getMax() {
        return this.mMax;
    }

    public void setProgress(float progress) {
        this.mProgress = progress;
        OnProgressChangedListener onProgressChangedListener = this.mProgressListener;
        if (onProgressChangedListener != null) {
            onProgressChangedListener.onProgressChanged(this, getProgress(), getProgressFloat(), false);
            this.mProgressListener.getProgressOnFinally(this, getProgress(), getProgressFloat(), false);
        }
        postInvalidate();
    }

    public int getProgress() {
        if (!this.isSeekBySection || !this.triggerSeekBySection) {
            return Math.round(this.mProgress);
        }
        float f = this.mSectionValue;
        float half = f / 2.0f;
        float f2 = this.mProgress;
        float f3 = this.mPreSecValue;
        if (f2 >= f3) {
            if (f2 < f3 + half) {
                return Math.round(f3);
            }
            float f4 = f3 + f;
            this.mPreSecValue = f4;
            return Math.round(f4);
        } else if (f2 >= f3 - half) {
            return Math.round(f3);
        } else {
            float f5 = f3 - f;
            this.mPreSecValue = f5;
            return Math.round(f5);
        }
    }

    public float getProgressFloat() {
        return formatFloat(this.mProgress);
    }

    public void setOnProgressChangedListener(OnProgressChangedListener onProgressChangedListener) {
        this.mProgressListener = onProgressChangedListener;
    }

    public void setValueFormatListener(OnValueFormatListener valueFormatListener) {
        this.mValueFormatListener = valueFormatListener;
    }

    public void config(SignConfigBuilder builder) {
        this.mMin = builder.min;
        this.mMax = builder.max;
        this.mProgress = builder.progress;
        this.isFloatType = builder.floatType;
        this.mTrackSize = builder.trackSize;
        this.mSecondTrackSize = builder.secondTrackSize;
        this.mThumbRadius = builder.thumbRadius;
        this.mThumbRadiusOnDragging = builder.thumbRadiusOnDragging;
        this.mTrackColor = builder.trackColor;
        this.mSecondTrackColor = builder.secondTrackColor;
        this.mThumbColor = builder.thumbColor;
        this.mSectionCount = builder.sectionCount;
        this.isShowSectionMark = builder.showSectionMark;
        this.isAutoAdjustSectionMark = builder.autoAdjustSectionMark;
        this.isShowSectionText = builder.showSectionText;
        this.mSectionTextSize = builder.sectionTextSize;
        this.mSectionTextColor = builder.sectionTextColor;
        this.mSectionTextPosition = builder.sectionTextPosition;
        this.mSectionTextInterval = builder.sectionTextInterval;
        this.isShowThumbText = builder.showThumbText;
        this.mThumbTextSize = builder.thumbTextSize;
        this.mThumbTextColor = builder.thumbTextColor;
        this.isShowProgressInFloat = builder.showProgressInFloat;
        this.mAnimDuration = builder.animDuration;
        this.isTouchToSeek = builder.touchToSeek;
        this.isSeekBySection = builder.seekBySection;
        String[] strArr = this.mConfigBuilder.bottomSidesLabels;
        this.mSidesLabels = strArr;
        this.isSidesLabels = strArr != null && strArr.length > 0;
        this.mThumbBgAlpha = this.mConfigBuilder.thumbBgAlpha;
        this.mThumbRatio = this.mConfigBuilder.thumbRatio;
        this.isShowThumbShadow = this.mConfigBuilder.showThumbShadow;
        this.unit = this.mConfigBuilder.unit;
        this.mReverse = this.mConfigBuilder.reverse;
        this.mFormat = this.mConfigBuilder.format;
        this.mSignColor = builder.signColor;
        this.mSignTextSize = builder.signTextSize;
        this.mSignTextColor = builder.signTextColor;
        this.isShowSign = builder.showSign;
        this.mSignArrowWidth = builder.signArrowWidth;
        this.mSignArrowHeight = builder.signArrowHeight;
        this.mSignRound = builder.signRound;
        this.mSignHeight = builder.signHeight;
        this.mSignWidth = builder.signWidth;
        this.isShowSignBorder = builder.showSignBorder;
        this.mSignBorderSize = builder.signBorderSize;
        this.mSignBorderColor = builder.signBorderColor;
        this.isSignArrowAutofloat = builder.signArrowAutofloat;
        init();
        initConfigByPriority();
        createValueTextLayout();
        OnProgressChangedListener onProgressChangedListener = this.mProgressListener;
        if (onProgressChangedListener != null) {
            onProgressChangedListener.onProgressChanged(this, getProgress(), getProgressFloat(), false);
            this.mProgressListener.getProgressOnFinally(this, getProgress(), getProgressFloat(), false);
        }
        this.mConfigBuilder = null;
        requestLayout();
    }

    public SignConfigBuilder getConfigBuilder() {
        if (this.mConfigBuilder == null) {
            this.mConfigBuilder = new SignConfigBuilder(this);
        }
        this.mConfigBuilder.min = this.mMin;
        this.mConfigBuilder.max = this.mMax;
        this.mConfigBuilder.progress = this.mProgress;
        this.mConfigBuilder.floatType = this.isFloatType;
        this.mConfigBuilder.trackSize = this.mTrackSize;
        this.mConfigBuilder.secondTrackSize = this.mSecondTrackSize;
        this.mConfigBuilder.thumbRadius = this.mThumbRadius;
        this.mConfigBuilder.thumbRadiusOnDragging = this.mThumbRadiusOnDragging;
        this.mConfigBuilder.trackColor = this.mTrackColor;
        this.mConfigBuilder.secondTrackColor = this.mSecondTrackColor;
        this.mConfigBuilder.thumbColor = this.mThumbColor;
        this.mConfigBuilder.sectionCount = this.mSectionCount;
        this.mConfigBuilder.showSectionMark = this.isShowSectionMark;
        this.mConfigBuilder.autoAdjustSectionMark = this.isAutoAdjustSectionMark;
        this.mConfigBuilder.showSectionText = this.isShowSectionText;
        this.mConfigBuilder.sectionTextSize = this.mSectionTextSize;
        this.mConfigBuilder.sectionTextColor = this.mSectionTextColor;
        this.mConfigBuilder.sectionTextPosition = this.mSectionTextPosition;
        this.mConfigBuilder.sectionTextInterval = this.mSectionTextInterval;
        this.mConfigBuilder.showThumbText = this.isShowThumbText;
        this.mConfigBuilder.thumbTextSize = this.mThumbTextSize;
        this.mConfigBuilder.thumbTextColor = this.mThumbTextColor;
        this.mConfigBuilder.showProgressInFloat = this.isShowProgressInFloat;
        this.mConfigBuilder.animDuration = this.mAnimDuration;
        this.mConfigBuilder.touchToSeek = this.isTouchToSeek;
        this.mConfigBuilder.seekBySection = this.isSeekBySection;
        this.mConfigBuilder.bottomSidesLabels = this.mSidesLabels;
        this.mConfigBuilder.thumbBgAlpha = this.mThumbBgAlpha;
        this.mConfigBuilder.thumbRatio = this.mThumbRatio;
        this.mConfigBuilder.showThumbShadow = this.isShowThumbShadow;
        this.mConfigBuilder.unit = this.unit;
        this.mConfigBuilder.reverse = this.mReverse;
        this.mConfigBuilder.format = this.mFormat;
        this.mConfigBuilder.signColor = this.mSignColor;
        this.mConfigBuilder.signTextSize = this.mSignTextSize;
        this.mConfigBuilder.signTextColor = this.mSignTextColor;
        this.mConfigBuilder.showSign = this.isShowSign;
        this.mConfigBuilder.signArrowHeight = this.mSignArrowHeight;
        this.mConfigBuilder.signArrowWidth = this.mSignArrowWidth;
        this.mConfigBuilder.signRound = this.mSignRound;
        this.mConfigBuilder.signHeight = this.mSignHeight;
        this.mConfigBuilder.signWidth = this.mSignWidth;
        this.mConfigBuilder.showSignBorder = this.isShowSignBorder;
        this.mConfigBuilder.signBorderSize = this.mSignBorderSize;
        this.mConfigBuilder.signBorderColor = this.mSignBorderColor;
        this.mConfigBuilder.signArrowAutofloat = this.isSignArrowAutofloat;
        return this.mConfigBuilder;
    }

    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("save_instance", super.onSaveInstanceState());
        bundle.putFloat("progress", this.mProgress);
        return bundle;
    }

    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            this.mProgress = bundle.getFloat("progress");
            super.onRestoreInstanceState(bundle.getParcelable("save_instance"));
            setProgress(this.mProgress);
            return;
        }
        super.onRestoreInstanceState(state);
    }

    private String float2String(float value) {
        return String.valueOf(formatFloat(value));
    }

    private float formatFloat(float value) {
        return value;
    }
}
