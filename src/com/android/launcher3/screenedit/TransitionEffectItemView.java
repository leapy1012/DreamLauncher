package com.android.launcher3.screenedit;

import android.animation.ValueAnimator;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.PathInterpolator;
import android.widget.LinearLayout;

import com.android.launcher3.R;
import com.coui.appcompat.roundRect.COUIRoundRectUtil;

/**
 * Source-compatible counterpart of OPPO's ToggleBarEffectItemContainer and
 * PressFeedbackPreviewWrapper.
 */
public class TransitionEffectItemView extends LinearLayout {

    private static final PathInterpolator SELECTED_STROKE_INTERPOLATOR =
            new PathInterpolator(0.33f, 0f, 0.67f, 1f);

    private final Paint mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mBounds = new RectF();
    private final float mRadius;
    private final float mStrokeWidth;
    private int mStrokeAlpha;
    private int mFillRgb;
    private int mNormalFillAlpha;
    private int mPressedFillAlpha;
    private int mCurrentFillAlpha;
    private ValueAnimator mStrokeAnimator;
    private ValueAnimator mPressAnimator;

    public TransitionEffectItemView(Context context) {
        this(context, null);
    }

    public TransitionEffectItemView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TransitionEffectItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(false);
        mRadius = getResources().getDimension(R.dimen.transition_effect_item_radius);
        mStrokeWidth = getResources().getDimension(
                R.dimen.transition_effect_selected_stroke_width);
        boolean bright = isBrightWallpaper(context);
        int fillColor = Color.parseColor(bright ? "#66A6A6A6" : "#66E0E0E0");
        mFillRgb = fillColor & 0x00ffffff;
        mNormalFillAlpha = Color.alpha(fillColor);
        mPressedFillAlpha = Math.min(255, mNormalFillAlpha * 2);
        mCurrentFillAlpha = mNormalFillAlpha;
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setStrokeWidth(mStrokeWidth);
        mStrokePaint.setColor(bright ? Color.BLACK : Color.WHITE);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        mFillPaint.setColor(mFillRgb | (mCurrentFillAlpha << 24));
        canvas.drawPath(COUIRoundRectUtil.getInstance().getPath(mBounds, mRadius), mFillPaint);
        if (mStrokeAlpha > 0) {
            mStrokePaint.setAlpha(mStrokeAlpha);
            float inset = mStrokeWidth / 2f;
            RectF strokeBounds = new RectF(mBounds);
            strokeBounds.inset(inset, inset);
            canvas.drawPath(COUIRoundRectUtil.getInstance().getPath(
                    strokeBounds, mRadius - mStrokeWidth), mStrokePaint);
        }
        super.dispatchDraw(canvas);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mBounds.set(0, 0, w, h);
    }

    @Override
    public void setSelected(boolean selected) {
        if (selected == isSelected()) {
            return;
        }
        super.setSelected(selected);
        if (mStrokeAnimator != null) {
            mStrokeAnimator.cancel();
        }
        mStrokeAnimator = ValueAnimator.ofInt(mStrokeAlpha, selected ? 255 : 0);
        mStrokeAnimator.setDuration(selected ? 280 : 150);
        mStrokeAnimator.setInterpolator(SELECTED_STROKE_INTERPOLATOR);
        mStrokeAnimator.addUpdateListener(animation -> {
            mStrokeAlpha = (Integer) animation.getAnimatedValue();
            invalidate();
        });
        mStrokeAnimator.start();
    }

    public void setSelectedImmediately(boolean selected) {
        if (mStrokeAnimator != null) {
            mStrokeAnimator.cancel();
        }
        super.setSelected(selected);
        mStrokeAlpha = selected ? 255 : 0;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            animatePress(0.9f, 200, true);
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP
                || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            animatePress(1f, 355, false);
        }
        return super.onTouchEvent(event);
    }

    private void animatePress(float targetScale, long duration, boolean pressing) {
        if (mPressAnimator != null) {
            mPressAnimator.cancel();
        }
        float startScale = getScaleX();
        int startAlpha = mCurrentFillAlpha;
        int targetAlpha = pressing ? mPressedFillAlpha : mNormalFillAlpha;
        mPressAnimator = ValueAnimator.ofFloat(startScale, targetScale);
        mPressAnimator.setDuration(duration);
        mPressAnimator.setInterpolator(new PathInterpolator(0.4f, 0f, 0.2f, 1f));
        mPressAnimator.addUpdateListener(animation -> {
            float scale = (Float) animation.getAnimatedValue();
            setScaleX(scale);
            setScaleY(scale);
            float fraction = animation.getAnimatedFraction();
            mCurrentFillAlpha = Math.round(
                    startAlpha + (targetAlpha - startAlpha) * fraction);
            invalidate();
        });
        mPressAnimator.start();
    }

    private static boolean isBrightWallpaper(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return false;
        }
        WallpaperColors colors = context.getSystemService(WallpaperManager.class)
                .getWallpaperColors(WallpaperManager.FLAG_SYSTEM);
        return colors != null
                && (colors.getColorHints() & WallpaperColors.HINT_SUPPORTS_DARK_TEXT) != 0;
    }
}
