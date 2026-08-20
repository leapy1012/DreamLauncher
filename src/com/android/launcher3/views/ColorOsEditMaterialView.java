package com.android.launcher3.views;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * Local fallback for OPPO's ToggleBar background-surface material blur.
 *
 * <p>OplusDepthController applies radius 240, mirror tiling, and blend RGBA
 * (0.10980392, 0.14901961, 0.20392157, 0.3) below launcher content. MTK exposes neither
 * OplusBlurManager nor Android cross-window blur, so this view renders the same parameters from
 * the current wallpaper while leaving Workspace children sharp above it.</p>
 */
public final class ColorOsEditMaterialView extends FrameLayout {
    // OplusDepthController supplies 240 in its compositor scale; decoded
    // OplusBlurProperties.toUXRadius() maps that value to UX/Gaussian radius 30.
    private static final float BLUR_RADIUS_PX = 30f;
    private static final int MATERIAL_BLEND_COLOR = 0x4D1C2634;

    private final ImageView mWallpaperView;
    private final View mBlendView;
    private Bitmap mWallpaperBitmap;
    private ValueAnimator mMaterialAnimator;
    private float mMaterialProgress = 1f;

    public ColorOsEditMaterialView(Context context) {
        super(context);
        setClickable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);

        mWallpaperView = new ImageView(context);
        mWallpaperView.setScaleType(ImageView.ScaleType.FIT_XY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mWallpaperView.setRenderEffect(RenderEffect.createBlurEffect(
                    BLUR_RADIUS_PX, BLUR_RADIUS_PX, Shader.TileMode.MIRROR));
        }
        addView(mWallpaperView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mBlendView = new View(context);
        mBlendView.setBackgroundColor(MATERIAL_BLEND_COLOR);
        addView(mBlendView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setMaterialProgress(1f);
    }

    /**
     * Mirrors OPPO's independent ToggleBar blur channel. The wallpaper bitmap itself always stays
     * opaque; only blur and blend progress change. This guarantees that leaving edit mode never
     * exposes the transparent Launcher window before the real wallpaper surface is committed.
     */
    public void animateToNormal(long duration, TimeInterpolator interpolator) {
        if (mMaterialAnimator != null) {
            mMaterialAnimator.cancel();
        }
        mMaterialAnimator = ValueAnimator.ofFloat(mMaterialProgress, 0f);
        mMaterialAnimator.setDuration(duration);
        mMaterialAnimator.setInterpolator(interpolator);
        mMaterialAnimator.addUpdateListener(animation ->
                setMaterialProgress((float) animation.getAnimatedValue()));
        mMaterialAnimator.start();
    }

    private void setMaterialProgress(float progress) {
        mMaterialProgress = Math.max(0f, Math.min(1f, progress));
        mBlendView.setAlpha(mMaterialProgress);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            float radius = BLUR_RADIUS_PX * mMaterialProgress;
            mWallpaperView.setRenderEffect(radius > 0.01f
                    ? RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.MIRROR)
                    : null);
        }
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        if (width > 0 && height > 0 && (width != oldWidth || height != oldHeight)) {
            renderWallpaper(width, height);
        }
    }

    private void renderWallpaper(int width, int height) {
        recycleWallpaper();
        try {
            mWallpaperBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mWallpaperBitmap);
            Drawable wallpaper = WallpaperManager.getInstance(getContext()).getDrawable();
            if (wallpaper == null) {
                canvas.drawColor(Color.BLACK);
            } else {
                int sourceWidth = Math.max(1, wallpaper.getIntrinsicWidth());
                int sourceHeight = Math.max(1, wallpaper.getIntrinsicHeight());
                float scale = Math.max((float) width / sourceWidth,
                        (float) height / sourceHeight);
                int drawWidth = Math.round(sourceWidth * scale);
                int drawHeight = Math.round(sourceHeight * scale);
                int left = (width - drawWidth) / 2;
                int top = (height - drawHeight) / 2;
                wallpaper.setBounds(left, top, left + drawWidth, top + drawHeight);
                wallpaper.draw(canvas);
            }
            mWallpaperView.setImageBitmap(mWallpaperBitmap);
        } catch (OutOfMemoryError | RuntimeException error) {
            recycleWallpaper();
            mWallpaperView.setBackgroundColor(Color.BLACK);
        }
    }

    private void recycleWallpaper() {
        mWallpaperView.setImageDrawable(null);
        if (mWallpaperBitmap != null && !mWallpaperBitmap.isRecycled()) {
            mWallpaperBitmap.recycle();
        }
        mWallpaperBitmap = null;
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mMaterialAnimator != null) {
            mMaterialAnimator.cancel();
            mMaterialAnimator = null;
        }
        recycleWallpaper();
        super.onDetachedFromWindow();
    }
}
