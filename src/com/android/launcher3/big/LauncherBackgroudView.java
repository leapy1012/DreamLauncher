package com.android.launcher3.big;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Property;
import android.view.View;
import android.view.animation.Interpolator;
import com.android.launcher3.Insettable;
import com.android.launcher3.Launcher;
import com.android.launcher3.customer.tools.ImageUtils;
import java.util.function.Consumer;
import com.android.launcher3.R;
/* loaded from: classes8.dex */
public class LauncherBackgroudView extends View implements Insettable {
    private ObjectAnimator mAnim;
    private Consumer<String> mCallback;
    private Drawable mWllpaperBlu = null;
    private Drawable mWallpaperSrcBlu = null;
    private Drawable mIconBlu = null;
    boolean isRunning = false;
    boolean showWallpaper = false;
    boolean isWallpaperChanged = false;

    public void setLauncherBg(Drawable mLauncherBg, Drawable src) {
        this.mWllpaperBlu = mLauncherBg;
    }
    public boolean isRunning() {
        return this.isRunning;
    }

    @Override
    public void setAlpha(float alpha) {
        if (alpha == 0.0f) {
            if (View.GONE != getVisibility()) {
                setVisibility(View.GONE);
            }
        } else if (getVisibility() != View.VISIBLE) {
            setVisibility(View.VISIBLE);
        }
        super.setAlpha(alpha);
    }

    public LauncherBackgroudView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public LauncherBackgroudView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public LauncherBackgroudView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LauncherBackgroudView(Context context) {
        super(context);
    }

    public Drawable getWallpaperBlu() {
        return mWllpaperBlu;
    }
    @Override
    public void setInsets(Rect insets) {
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        int w = getMeasuredWidth();
        int h = getMeasuredHeight();
        if (this.mWllpaperBlu != null && !showWallpaper) {
            canvas.save();
            mWllpaperBlu.setBounds(0, 0, w, h);
            mWllpaperBlu.draw(canvas);
            canvas.restore();
        }
        OnDrawIcon(canvas);
        OnDrawSrcWallpaper(canvas);
    }

    public void destoryIconBlue() {
        mIconBlu = null;
    }

    public void setIconBlu() {
        Bitmap blu = ImageUtils.takeScreenShotOfView(Launcher.getLauncher(getContext()).getDragLayer());
        int w = (int) (getMeasuredWidth() * 0.15f);
        int h = (int) (getMeasuredHeight() * 0.15f);
        Bitmap blu2 = ImageUtils.scaleBitmap(blu, w, h);
        this.mIconBlu = ImageUtils.bitmapToDrawable(ImageUtils.blurBitmap(getContext(), blu2));
        invalidate();
    }

    public boolean isShowWallpaper() {
        return this.showWallpaper;
    }

    public void setshowWallpaper(boolean showWallpaper) {
        this.showWallpaper=showWallpaper;
    }

    public void setWallpaperChanged(boolean isWallpaperChanged) {
        this.isWallpaperChanged = isWallpaperChanged;
    }

    public void OnDrawIcon(Canvas canvas) {
        if (this.mIconBlu != null) {
            int w = getMeasuredWidth();
            int h = getMeasuredHeight();
            canvas.save();
            this.mIconBlu.setBounds(0, 0, w, h);
            this.mIconBlu.draw(canvas);
            canvas.restore();
        }
    }

    public void OnDrawSrcWallpaper(Canvas canvas) {
        if (this.mWallpaperSrcBlu != null && showWallpaper) {
            int w = getMeasuredWidth();
            int h = getMeasuredHeight();
            canvas.save();
            mWallpaperSrcBlu.setBounds(0, 0, w, h);
            mWallpaperSrcBlu.draw(canvas);
            canvas.restore();
        }
    }

    public void onDestroy() {
        if (mWllpaperBlu != null) {
            mWllpaperBlu.setCallback(null);
        }
    }

    /**public void startAnim(Launcher launcher, long duration, Interpolator in) {
        AnimatorSet animatorSet = new AnimatorSet();
        float[] wallpaperScales = {1.0f};
        LauncherBackgroudView wallpaper = (LauncherBackgroudView) launcher.findViewById(R.id.wallpaper_bg);
        ObjectAnimator wallpaperscaleX = ObjectAnimator.ofFloat(wallpaper, View.SCALE_X, wallpaperScales);
        ObjectAnimator wallpaperscaleY = ObjectAnimator.ofFloat(wallpaper, View.SCALE_Y, wallpaperScales);
        wallpaperscaleX.setInterpolator(in);
        wallpaperscaleX.setDuration(duration);
        wallpaperscaleY.setInterpolator(in);
        wallpaperscaleY.setDuration(duration);
        if (!launcher.isInMultiWindowMode()) {
            animatorSet.start();
        }
    }*/

    public void startAnim(Property property, float v1, float v2, boolean isOpening, final Consumer<String> call, Consumer<String> callback) {
        ObjectAnimator objectAnimator = this.mAnim;
        if (objectAnimator != null) {
            objectAnimator.cancel();
        }
        ObjectAnimator ofFloat = isOpening ? ObjectAnimator.ofFloat(this, property, getAlpha(), v2) : ObjectAnimator.ofFloat(this, property, getAlpha(), v1);
        this.mAnim = ofFloat;
        ofFloat.setDuration(500L);
        this.mAnim.start();
        this.mCallback = callback;
        if (callback != null) {
            this.isRunning = true;
        }
        this.mAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (call != null) {
                    call.accept(null);
                    isRunning = false;
                }
                if (mCallback != null) {
                    mCallback.accept(null);
                    mCallback = null;
                }
                if (isWallpaperChanged) {
                    isWallpaperChanged = false;
                    setAlpha(0.0f);
                }
               setLayerType(View.LAYER_TYPE_NONE, null);
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }
        });
    }
}
