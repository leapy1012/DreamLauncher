package com.android.launcher3.anim;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Property;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.android.launcher3.big.HxyAnimBubbleTextView;

import java.util.function.Consumer;

public abstract class BaseParams {
    private static final Property<BaseParams, Float> ANIMATION_PROGRESS = new Property<BaseParams, Float>(Float.TYPE, "SWITCH_ANIMATION_PROGRESS") {
        public Float get(BaseParams anim) {
            return Float.valueOf(anim.mAnimationProgress);
        }

        public void set(BaseParams anim, Float progress) {
            anim.mAnimationProgress = progress.floatValue();
            anim.setAnimationProgress(progress.floatValue());
        }
    };
    protected static final int DURATION = 500;
    private static final String TAG = "BaseParams";
    public float mAnimationProgress = 0.0f;
    public Bitmap mBg;
    protected Consumer mCallBack;
    public Context mContext;
    protected boolean mIsEnd = true;
    private ValueAnimator mProgressAnimator = null;
    public Bitmap mSrc;

    public abstract void onClick();

    public abstract void startAnimation();

    public abstract void stopAnimation();

    public BaseParams(Consumer<View> call) {
        this.mCallBack = call;
    }

    public void release() {
        cancelProgressAnimation();
        this.mAnimationProgress = 0.0f;
    }

    public void init(String path, String zipPath, String themedName) {
    }

    public boolean resouceOK() {
        return (this.mBg == null || this.mSrc == null) ? false : true;
    }

    public int getDuration() {
        return 500;
    }

    public Drawable getWallPaperDrawable(Context context, String iconName) {
        return null;
    }

    public static Bitmap drawableToBitmap(Drawable draw) {
        if (draw == null) {
            return null;
        }
        int width = draw.getIntrinsicWidth();
        int height = draw.getIntrinsicHeight();
        Bitmap sbitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(sbitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        draw.setBounds(0, 0, width, height);
        draw.draw(canvas);
        return sbitmap;
    }

    public void startProgressAnimation() {
        if (this.mIsEnd) {
            cancelProgressAnimation();
            this.mAnimationProgress = 0.0f;
            if (this.mProgressAnimator == null) {
                this.mProgressAnimator = ObjectAnimator.ofFloat(this, ANIMATION_PROGRESS, new float[]{0.0f, 1.0f});
            }
            this.mProgressAnimator.setInterpolator(new LinearInterpolator());
            this.mProgressAnimator.setDuration((long) getDuration());
            this.mProgressAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    BaseParams.this.onProgressAnimationBegin();
                }

                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    BaseParams.this.onProgressAnimationEnd();
                }
            });
            this.mProgressAnimator.start();
        }
    }

    public void onProgressAnimationBegin() {
        this.mIsEnd = false;
    }

    public void onProgressAnimationEnd() {
        this.mIsEnd = true;
    }

    public void cancelProgressAnimation() {
        ValueAnimator valueAnimator = this.mProgressAnimator;
        if (valueAnimator != null) {
            valueAnimator.cancel();
            this.mProgressAnimator.removeAllListeners();
            this.mProgressAnimator = null;
        }
    }

    public void onDraw(Canvas canvas, Rect rect) {
    }

    public void onAttachedToWindow(HxyAnimBubbleTextView icon) {
    }

    public void onDetachedFromWindow(HxyAnimBubbleTextView icon) {
    }

    public void setAnimationProgress(float progress) {
        this.mCallBack.accept((Object) null);
    }
}
