package com.android.launcher3.editselection;

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

import androidx.appcompat.widget.AppCompatTextView;

import com.coui.appcompat.roundRect.COUIRoundRectUtil;

/** Launcher-local port of OPPO's PressFeedbackButton rendering and touch response. */
public class OppoPressFeedbackButton extends AppCompatTextView {

    private static final PathInterpolator PRESS_INTERPOLATOR =
            new PathInterpolator(0.4f, 0f, 0.2f, 1f);

    private final Paint mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mBounds = new RectF();
    private final float mRadius;
    private int mNormalAlpha;
    private int mPressedAlpha;
    private int mCurrentAlpha;
    private int mFillRgb;
    private ValueAnimator mScaleAnimator;

    public OppoPressFeedbackButton(Context context) {
        this(context, null);
    }

    public OppoPressFeedbackButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OppoPressFeedbackButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mRadius = 15f * getResources().getDisplayMetrics().density;
        setBackground(null);
        updateWallpaperColors();
    }

    public void updateWallpaperColors() {
        boolean bright = isBrightWallpaper(getContext());
        int fill = Color.parseColor(bright ? "#66A6A6A6" : "#66E0E0E0");
        mFillRgb = fill & 0x00ffffff;
        mNormalAlpha = Color.alpha(fill);
        mPressedAlpha = Math.min(255, mNormalAlpha * 2);
        mCurrentAlpha = mNormalAlpha;
        setTextColor(bright ? Color.BLACK : Color.WHITE);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mBounds.set(0, 0, w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mFillPaint.setColor(mFillRgb | (mCurrentAlpha << 24));
        canvas.drawPath(COUIRoundRectUtil.getInstance().getPath(mBounds, mRadius), mFillPaint);
        super.onDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                animateScale(0.9f, 200, true);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                animateScale(1f, 355, false);
                break;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    private void animateScale(float target, long duration, boolean pressing) {
        if (mScaleAnimator != null) {
            mScaleAnimator.cancel();
        }
        float start = getScaleX();
        int alphaStart = mCurrentAlpha;
        int alphaEnd = pressing ? mPressedAlpha : mNormalAlpha;
        mScaleAnimator = ValueAnimator.ofFloat(start, target);
        mScaleAnimator.setDuration(duration);
        mScaleAnimator.setInterpolator(PRESS_INTERPOLATOR);
        mScaleAnimator.addUpdateListener(animation -> {
            float value = (Float) animation.getAnimatedValue();
            setScaleX(value);
            setScaleY(value);
            float fraction = animation.getAnimatedFraction();
            mCurrentAlpha = Math.round(alphaStart + (alphaEnd - alphaStart) * fraction);
            invalidate();
        });
        mScaleAnimator.start();
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
