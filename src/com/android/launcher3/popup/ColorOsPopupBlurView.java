package com.android.launcher3.popup;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.WallpaperManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.PixelCopy;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.android.launcher3.Launcher;
import com.android.launcher3.folder.large.HxyLargeFolderIcon;
import com.android.launcher3.views.BaseDragLayer;

/**
 * ColorOS popup backdrop. OPPO captures wallpaper and launcher content into a full drag-layer
 * surface, blurs it, adds its neutral blend color, and redraws the pressed item above that surface.
 */
public final class ColorOsPopupBlurView extends FrameLayout {
    private static final long BLUR_DURATION_MS = 330L;
    private static final float BLUR_RADIUS_PX = 80f;
    private static final int BLEND_COLOR = 0x4D1C2634;

    private final Launcher mLauncher;
    private final ImageView mBackground;
    private final Rect mPressedBounds = new Rect();
    private Bitmap mCapturedBitmap;
    private Bitmap mPressedBitmap;
    private ImageView mPressedCopy;
    private boolean mUseDirectPressedCopy;

    private ColorOsPopupBlurView(Launcher launcher, View pressedView) {
        super(launcher);
        mLauncher = launcher;
        setClickable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);

        mBackground = new ImageView(launcher);
        mBackground.setScaleType(ImageView.ScaleType.FIT_XY);
        addView(mBackground, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        View blend = new View(launcher);
        blend.setBackgroundColor(BLEND_COLOR);
        addView(blend, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        captureLauncherSurface();
        addPressedItem(pressedView);
    }

    public static ColorOsPopupBlurView show(Launcher launcher, View pressedView) {
        ColorOsPopupBlurView blurView = new ColorOsPopupBlurView(launcher, pressedView);
        BaseDragLayer<?> dragLayer = launcher.getDragLayer();
        BaseDragLayer.LayoutParams lp = new BaseDragLayer.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.ignoreInsets = true;
        dragLayer.addView(blurView, lp);
        blurView.setAlpha(0f);
        blurView.animate().alpha(1f).setDuration(BLUR_DURATION_MS).start();
        return blurView;
    }

    private void captureLauncherSurface() {
        BaseDragLayer<?> dragLayer = mLauncher.getDragLayer();
        int width = Math.max(1, dragLayer.getWidth());
        int height = Math.max(1, dragLayer.getHeight());
        try {
            mCapturedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas wallpaperCanvas = new Canvas(mCapturedBitmap);
            drawWallpaper(wallpaperCanvas, width, height);
            mBackground.setImageBitmap(mCapturedBitmap);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mBackground.setRenderEffect(RenderEffect.createBlurEffect(
                        BLUR_RADIUS_PX, BLUR_RADIUS_PX, Shader.TileMode.MIRROR));
            }

            Bitmap windowBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            PixelCopy.request(mLauncher.getWindow(), windowBitmap, result -> {
                if (result == PixelCopy.SUCCESS && mCapturedBitmap != null
                        && !mCapturedBitmap.isRecycled()) {
                    new Canvas(mCapturedBitmap).drawBitmap(windowBitmap, 0f, 0f, null);
                    populatePressedItemFromWindow(windowBitmap);
                    mBackground.invalidate();
                }
                windowBitmap.recycle();
            }, new Handler(Looper.getMainLooper()));
        } catch (OutOfMemoryError | IllegalArgumentException error) {
            mBackground.setBackgroundColor(0x66000000);
        }
    }

    private void drawWallpaper(Canvas canvas, int width, int height) {
        Drawable wallpaper = WallpaperManager.getInstance(getContext()).getDrawable();
        if (wallpaper == null) {
            canvas.drawColor(Color.BLACK);
            return;
        }
        int sourceWidth = Math.max(1, wallpaper.getIntrinsicWidth());
        int sourceHeight = Math.max(1, wallpaper.getIntrinsicHeight());
        float scale = Math.max((float) width / sourceWidth, (float) height / sourceHeight);
        int drawWidth = Math.round(sourceWidth * scale);
        int drawHeight = Math.round(sourceHeight * scale);
        int left = (width - drawWidth) / 2;
        int top = (height - drawHeight) / 2;
        wallpaper.setBounds(left, top, left + drawWidth, top + drawHeight);
        wallpaper.draw(canvas);
    }

    private void addPressedItem(View pressedView) {
        if (pressedView == null || pressedView.getWidth() <= 0 || pressedView.getHeight() <= 0) {
            return;
        }
        mLauncher.getDragLayer().getDescendantRectRelativeToSelf(pressedView, mPressedBounds);
        Rect localGroupBounds = null;
        if (pressedView instanceof HxyLargeFolderIcon) {
            // Decoded IGroupView behavior: retain only the original group body, never its label.
            localGroupBounds = new Rect();
            ((HxyLargeFolderIcon) pressedView).getColorOsGroupBounds(localGroupBounds);
            int parentLeft = mPressedBounds.left;
            int parentTop = mPressedBounds.top;
            mPressedBounds.set(parentLeft + localGroupBounds.left,
                    parentTop + localGroupBounds.top,
                    parentLeft + localGroupBounds.right,
                    parentTop + localGroupBounds.bottom);
        }
        if (mPressedBounds.isEmpty()) {
            return;
        }
        mPressedCopy = new ImageView(getContext());
        mPressedCopy.setScaleType(ImageView.ScaleType.FIT_XY);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                mPressedBounds.width(), mPressedBounds.height());
        lp.leftMargin = mPressedBounds.left;
        lp.topMargin = mPressedBounds.top;
        addView(mPressedCopy, lp);
        if (localGroupBounds != null) {
            try {
                mPressedBitmap = Bitmap.createBitmap(
                        Math.max(1, localGroupBounds.width()),
                        Math.max(1, localGroupBounds.height()),
                        Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(mPressedBitmap);
                canvas.translate(-localGroupBounds.left, -localGroupBounds.top);
                pressedView.draw(canvas);
                mPressedCopy.setImageBitmap(mPressedBitmap);
                mUseDirectPressedCopy = true;
            } catch (OutOfMemoryError | IllegalArgumentException ignored) {
                mUseDirectPressedCopy = false;
            }
        }
    }

    private void populatePressedItemFromWindow(Bitmap windowBitmap) {
        if (mUseDirectPressedCopy || mPressedCopy == null || mPressedBounds.isEmpty()) {
            return;
        }
        int left = Math.max(0, mPressedBounds.left);
        int top = Math.max(0, mPressedBounds.top);
        int right = Math.min(windowBitmap.getWidth(), mPressedBounds.right);
        int bottom = Math.min(windowBitmap.getHeight(), mPressedBounds.bottom);
        if (right <= left || bottom <= top) {
            return;
        }
        try {
            mPressedBitmap = Bitmap.createBitmap(
                    windowBitmap, left, top, right - left, bottom - top);
            mPressedCopy.setImageBitmap(mPressedBitmap);
        } catch (OutOfMemoryError | IllegalArgumentException ignored) {
            // The blur layer remains valid even when the selected-item crop cannot be retained.
        }
    }

    public Animator createCloseAnimator() {
        ObjectAnimator animator = ObjectAnimator.ofFloat(this, View.ALPHA, getAlpha(), 0f);
        animator.setDuration(BLUR_DURATION_MS);
        return animator;
    }

    public void removeAndRecycle() {
        animate().cancel();
        if (getParent() instanceof ViewGroup) {
            ((ViewGroup) getParent()).removeView(this);
        }
        mBackground.setImageDrawable(null);
        if (mPressedCopy != null) {
            mPressedCopy.setImageDrawable(null);
        }
        if (mCapturedBitmap != null && !mCapturedBitmap.isRecycled()) {
            mCapturedBitmap.recycle();
        }
        if (mPressedBitmap != null && !mPressedBitmap.isRecycled()) {
            mPressedBitmap.recycle();
        }
        mCapturedBitmap = null;
        mPressedBitmap = null;
        mPressedCopy = null;
    }
}
