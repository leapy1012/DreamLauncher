package com.android.launcher3.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

import com.android.launcher3.R;
import com.android.launcher3.util.Themes;
import com.coui.appcompat.animation.dynamicanimation.COUIDynamicAnimation;
import com.coui.appcompat.animation.dynamicanimation.COUISpringAnimation;
import com.coui.appcompat.animation.dynamicanimation.COUISpringForce;
import com.coui.appcompat.roundRect.COUIRoundRectUtil;

/** OPPO-style workspace-edit completion button, including COUI press feedback. */
public class ColorOsDoneButton extends AppCompatButton {
    private static final int NEW_BLUR_BACKGROUND_COLOR = 0x3DFFFFFF;
    private static final int DARK_WALLPAPER_FALLBACK_COLOR = 0x66E0E0E0;
    private static final int BRIGHT_WALLPAPER_FALLBACK_COLOR = 0x66A6A6A6;
    private static final float PRESSED_SCALE = 0.9f;
    private static final long COLOR_DURATION_MS = 200L;

    private final Paint mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mBounds = new RectF();
    private final float mRadius;
    private final COUISpringAnimation mScaleXSpring;
    private final COUISpringAnimation mScaleYSpring;
    private int mBaseColor;
    private int mCurrentAlpha;
    private ValueAnimator mColorAnimator;

    public ColorOsDoneButton(Context context) {
        this(context, null);
    }

    public ColorOsDoneButton(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorOsDoneButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mRadius = 15f * getResources().getDisplayMetrics().density;
        boolean useDarkForeground = Themes.getAttrBoolean(context, R.attr.isWorkspaceDarkText);
        mBaseColor = com.android.systemui.shared.system.BlurUtils.supportsBlursOnWindows()
                ? NEW_BLUR_BACKGROUND_COLOR
                : (useDarkForeground ? BRIGHT_WALLPAPER_FALLBACK_COLOR
                        : DARK_WALLPAPER_FALLBACK_COLOR);
        mCurrentAlpha = Color.alpha(mBaseColor);
        setTextColor(useDarkForeground ? Color.BLACK : Color.WHITE);
        setBackground(null);
        setAllCaps(false);
        setClickable(true);

        mScaleXSpring = new COUISpringAnimation(this, COUIDynamicAnimation.SCALE_X)
                .setSpring(new COUISpringForce(1f).setBounce(0f).setResponse(0.3f));
        mScaleYSpring = new COUISpringAnimation(this, COUIDynamicAnimation.SCALE_Y)
                .setSpring(new COUISpringForce(1f).setBounce(0f).setResponse(0.3f));
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        mBounds.set(0f, 0f, width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mFillPaint.setColor(Color.argb(mCurrentAlpha, Color.red(mBaseColor),
                Color.green(mBaseColor), Color.blue(mBaseColor)));
        canvas.drawPath(COUIRoundRectUtil.getInstance().getPath(mBounds, mRadius), mFillPaint);
        super.onDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isEnabled()) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    animatePressedState(true);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    animatePressedState(false);
                    break;
                default:
                    break;
            }
        }
        return super.onTouchEvent(event);
    }

    private void animatePressedState(boolean pressed) {
        float targetScale = pressed ? PRESSED_SCALE : 1f;
        mScaleXSpring.animateToFinalPosition(targetScale);
        mScaleYSpring.animateToFinalPosition(targetScale);
        int normalAlpha = Color.alpha(mBaseColor);
        int targetAlpha = pressed ? Math.min(255, normalAlpha * 2) : normalAlpha;
        if (mColorAnimator != null) {
            mColorAnimator.cancel();
        }
        mColorAnimator = ValueAnimator.ofInt(mCurrentAlpha, targetAlpha);
        mColorAnimator.setDuration(COLOR_DURATION_MS);
        mColorAnimator.addUpdateListener(animation -> {
            mCurrentAlpha = (int) animation.getAnimatedValue();
            invalidate();
        });
        mColorAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mColorAnimator != null) {
            mColorAnimator.cancel();
        }
        mScaleXSpring.cancel();
        mScaleYSpring.cancel();
        super.onDetachedFromWindow();
    }
}
