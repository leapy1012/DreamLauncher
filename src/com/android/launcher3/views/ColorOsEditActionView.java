package com.android.launcher3.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.android.launcher3.R;
import com.android.launcher3.util.Themes;
import com.coui.appcompat.animation.dynamicanimation.COUIDynamicAnimation;
import com.coui.appcompat.animation.dynamicanimation.COUISpringAnimation;
import com.coui.appcompat.animation.dynamicanimation.COUISpringForce;

/** One item in the ColorOS workspace-edit horizontal toolbar. */
public class ColorOsEditActionView extends AppCompatTextView {
    private static final int DARK_WALLPAPER_PLATE = 0x4DDBDBDB;
    private static final int BRIGHT_WALLPAPER_PLATE = 0x33000000;
    private static final float PRESSED_SCALE = 0.9f;
    private static final long CLICK_DEBOUNCE_MS = 250L;
    private static long sLastActionClickTime;

    private final Paint mPlatePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int mIconSize;
    private final COUISpringAnimation mScaleXSpring;
    private final COUISpringAnimation mScaleYSpring;
    private int mPlateColor;
    private int mPlateAlpha;
    private ValueAnimator mAlphaAnimator;

    public ColorOsEditActionView(Context context) {
        this(context, null);
    }

    public ColorOsEditActionView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorOsEditActionView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        float density = getResources().getDisplayMetrics().density;
        mIconSize = Math.round(44f * density);
        boolean brightWallpaper = Themes.getAttrBoolean(context, R.attr.isWorkspaceDarkText);
        mPlateColor = brightWallpaper ? BRIGHT_WALLPAPER_PLATE : DARK_WALLPAPER_PLATE;
        mPlateAlpha = Color.alpha(mPlateColor);
        int enabledTextColor = brightWallpaper ? Color.BLACK : Color.WHITE;
        int disabledTextColor = brightWallpaper ? 0x26000000 : 0x26FFFFFF;
        setTextColor(new ColorStateList(
                new int[][] {new int[] {-android.R.attr.state_enabled}, new int[] {}},
                new int[] {disabledTextColor, enabledTextColor}));
        setTextSize(10f);
        setTypeface(getTypeface(), android.graphics.Typeface.BOLD);
        setGravity(android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL);
        setMaxLines(2);
        setEllipsize(android.text.TextUtils.TruncateAt.END);
        setCompoundDrawablePadding(Math.round(4f * density));
        setPadding(Math.round(4f * density), 0, Math.round(4f * density), 0);
        setBackground(null);
        setClickable(true);
        setAllCaps(false);
        Drawable[] drawables = getCompoundDrawables();
        setCompoundDrawables(drawables[0], drawables[1], drawables[2], drawables[3]);

        mScaleXSpring = new COUISpringAnimation(this, COUIDynamicAnimation.SCALE_X)
                .setSpring(new COUISpringForce(1f).setBounce(0f).setResponse(0.3f));
        mScaleYSpring = new COUISpringAnimation(this, COUIDynamicAnimation.SCALE_Y)
                .setSpring(new COUISpringForce(1f).setBounce(0f).setResponse(0.3f));
    }

    @Override
    public void setCompoundDrawables(@Nullable Drawable left, @Nullable Drawable top,
            @Nullable Drawable right, @Nullable Drawable bottom) {
        if (top != null) {
            top.setBounds(0, 0, mIconSize, mIconSize);
        }
        super.setCompoundDrawables(left, top, right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mPlatePaint.setColor(Color.argb(mPlateAlpha, Color.red(mPlateColor),
                Color.green(mPlateColor), Color.blue(mPlateColor)));
        float radius = mIconSize / 2f;
        canvas.drawCircle(getWidth() / 2f, radius, radius, mPlatePaint);
        super.onDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                animatePress(true);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                animatePress(false);
                break;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    private void animatePress(boolean pressed) {
        float scale = pressed ? PRESSED_SCALE : 1f;
        mScaleXSpring.animateToFinalPosition(scale);
        mScaleYSpring.animateToFinalPosition(scale);
        int normalAlpha = Color.alpha(mPlateColor);
        int targetAlpha = pressed ? Math.min(255, normalAlpha * 2) : normalAlpha;
        if (mAlphaAnimator != null) {
            mAlphaAnimator.cancel();
        }
        mAlphaAnimator = ValueAnimator.ofInt(mPlateAlpha, targetAlpha);
        mAlphaAnimator.setDuration(200L);
        mAlphaAnimator.addUpdateListener(animator -> {
            mPlateAlpha = (int) animator.getAnimatedValue();
            invalidate();
        });
        mAlphaAnimator.start();
    }

    @Override
    public boolean performClick() {
        long now = android.os.SystemClock.uptimeMillis();
        if (now - sLastActionClickTime < CLICK_DEBOUNCE_MS) {
            return false;
        }
        sLastActionClickTime = now;
        return super.performClick();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mAlphaAnimator != null) {
            mAlphaAnimator.cancel();
        }
        mScaleXSpring.cancel();
        mScaleYSpring.cancel();
        super.onDetachedFromWindow();
    }
}
