package com.android.launcher3.big.booster;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.View;
import android.view.Window;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.Launcher;
import com.android.launcher3.dragndrop.DragLayer;

/**
 * CM Booster wave: expanding radial mesh warp of a full home composite
 * (wallpaper + window), matching {@code LauncherViewFrameLayout$b}.
 *
 * <p>CM warps a live GL hardware-layer texture. AOSP has no
 * {@code HardwareDrawCallback}, so we composite wallpaper under a window
 * PixelCopy (launcher windows are translucent — icons alone look black)
 * and warp that bitmap with {@link Canvas#drawBitmapMesh}.
 */
public class BoosterOverlayView extends AbstractFloatingView {

    private static final String TAG = "BoosterWave";

    /** CM: {@code 0x8fc} ms. */
    private static final long DURATION_MS = 2300L;
    /** CM mesh cell size. */
    private static final float CELL_DP = 13f;
    /** CM ring thickness / wavelength. */
    private static final float RING_THICKNESS_DP = 120f;
    /** CM max travel = max(W,H) + this. */
    private static final float MAX_EXTRA_DP = 300f;
    /** CM smali UV amp — matches on-device look. */
    private static final float UV_AMP = 0.01f;
    /** CM epicenter tweak before start. */
    private static final float ORIGIN_X_NUDGE_DP = 1f;
    private static final float ORIGIN_Y_NUDGE_DP = 9f;

    private Bitmap mSnapshot;
    private float[] mOrigVerts;
    private float[] mVerts;
    private int mMeshW;
    private int mMeshH;
    private float mOriginX;
    private float mOriginY;
    private float mRingThickness;
    private float mMaxRadius;
    private final Paint mPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator mAnim;

    public BoosterOverlayView(Context context) {
        this(context, null);
    }

    public BoosterOverlayView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BoosterOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(false);
        setClickable(true);
        setFocusable(true);
        setBackgroundColor(Color.TRANSPARENT);
    }

    /**
     * @param originX epicenter X in DragLayer coordinates (CleanUp icon center)
     * @param originY epicenter Y in DragLayer coordinates
     */
    public static void show(Launcher launcher, float originX, float originY) {
        closeOpenViews(launcher, false, TYPE_BOOSTER_OVERLAY);
        DragLayer dragLayer = launcher.getDragLayer();
        final int w = dragLayer.getWidth();
        final int h = dragLayer.getHeight();
        if (w <= 0 || h <= 0) {
            Log.w(TAG, "DragLayer has no size, skip wave");
            return;
        }

        float density = launcher.getResources().getDisplayMetrics().density;
        final float ox = originX - ORIGIN_X_NUDGE_DP * density;
        final float oy = originY + ORIGIN_Y_NUDGE_DP * density;

        captureHomeComposite(launcher, w, h, snapshot -> {
            if (snapshot == null) {
                Log.w(TAG, "Home capture failed, skip wave");
                return;
            }
            attachAndStart(launcher, snapshot, ox, oy);
        });
    }

    public static void show(Launcher launcher) {
        DragLayer dragLayer = launcher.getDragLayer();
        show(launcher, dragLayer.getWidth() / 2f, dragLayer.getHeight() / 2f);
    }

    private interface SnapshotReady {
        void onReady(@Nullable Bitmap snapshot);
    }

    /**
     * Wallpaper (behind translucent window) + window pixels composited into one
     * opaque bitmap — same visual source CM gets from its GL home root texture.
     */
    private static void captureHomeComposite(Launcher launcher, int w, int h,
            SnapshotReady ready) {
        final Bitmap composite = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        drawWallpaper(launcher, composite);

        final Bitmap windowLayer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Window window = launcher.getWindow();
        Handler handler = new Handler(Looper.getMainLooper());

        try {
            PixelCopy.request(window, windowLayer, copyResult -> {
                Canvas c = new Canvas(composite);
                if (copyResult == PixelCopy.SUCCESS) {
                    c.drawBitmap(windowLayer, 0, 0, null);
                } else {
                    Log.w(TAG, "PixelCopy failed (" + copyResult + "), drawing root view");
                    View root = launcher.getRootView();
                    if (root != null) {
                        root.draw(c);
                    } else {
                        launcher.getDragLayer().draw(c);
                    }
                }
                windowLayer.recycle();
                if (isMostlyBlack(composite)) {
                    Log.w(TAG, "Composite too dark — redraw wallpaper + root");
                    composite.eraseColor(Color.BLACK);
                    drawWallpaper(launcher, composite);
                    View root = launcher.getRootView();
                    if (root != null) {
                        root.draw(new Canvas(composite));
                    }
                }
                ready.onReady(composite);
            }, handler);
        } catch (Throwable t) {
            Log.w(TAG, "PixelCopy unavailable", t);
            windowLayer.recycle();
            Canvas c = new Canvas(composite);
            View root = launcher.getRootView();
            if (root != null) {
                root.draw(c);
            } else {
                launcher.getDragLayer().draw(c);
            }
            ready.onReady(composite);
        }
    }

    private static void drawWallpaper(Launcher launcher, Bitmap into) {
        try {
            WallpaperManager wm = WallpaperManager.getInstance(launcher);
            Drawable d = wm.getDrawable();
            if (d == null) {
                d = wm.getBuiltInDrawable();
            }
            if (d == null) {
                new Canvas(into).drawColor(0xFF101018);
                return;
            }

            final int dstW = into.getWidth();
            final int dstH = into.getHeight();
            int srcW = d.getIntrinsicWidth();
            int srcH = d.getIntrinsicHeight();
            // Fall back to stretch only when intrinsic size is unknown.
            if (srcW <= 0 || srcH <= 0) {
                d.setBounds(0, 0, dstW, dstH);
                d.draw(new Canvas(into));
                return;
            }

            // Match system wallpaper: scale-to-cover (center-crop), then pan with
            // the same X offset Workspace uses for parallax on the current page.
            float alignX = 0.5f;
            float alignY = 0.5f;
            try {
                if (launcher.getWorkspace() != null) {
                    alignX = launcher.getWorkspace().getWallpaperOffsetForCenterPage();
                }
            } catch (Throwable ignored) {
                // Keep centered.
            }
            alignX = Math.max(0f, Math.min(1f, alignX));
            alignY = Math.max(0f, Math.min(1f, alignY));

            float scale = Math.max(dstW / (float) srcW, dstH / (float) srcH);
            int scaledW = Math.round(srcW * scale);
            int scaledH = Math.round(srcH * scale);
            float dx = (dstW - scaledW) * alignX;
            float dy = (dstH - scaledH) * alignY;

            Canvas c = new Canvas(into);
            c.save();
            c.translate(dx, dy);
            d.setBounds(0, 0, scaledW, scaledH);
            d.draw(c);
            c.restore();
        } catch (Throwable t) {
            Log.w(TAG, "Wallpaper draw failed", t);
            new Canvas(into).drawColor(0xFF101018);
        }
    }

    /** Detect failed captures that look like a black flash to the user. */
    private static boolean isMostlyBlack(Bitmap bmp) {
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int samples = 0;
        int dark = 0;
        // Sparse sample grid — cheap brightness check.
        for (int y = h / 10; y < h; y += h / 5) {
            for (int x = w / 10; x < w; x += w / 5) {
                int c = bmp.getPixel(x, y);
                int lum = ((c >> 16) & 0xff) + ((c >> 8) & 0xff) + (c & 0xff);
                samples++;
                if (lum < 40) {
                    dark++;
                }
            }
        }
        return samples > 0 && dark * 1f / samples > 0.85f;
    }

    private static void attachAndStart(Launcher launcher, Bitmap snapshot,
            float originX, float originY) {
        if (launcher.isDestroyed()) {
            snapshot.recycle();
            return;
        }
        closeOpenViews(launcher, false, TYPE_BOOSTER_OVERLAY);

        float density = launcher.getResources().getDisplayMetrics().density;
        BoosterOverlayView overlay = new BoosterOverlayView(launcher);
        overlay.mSnapshot = snapshot;
        overlay.mOriginX = originX;
        overlay.mOriginY = originY;
        overlay.initMesh(snapshot.getWidth(), snapshot.getHeight(), density);
        overlay.mIsOpen = true;

        DragLayer.LayoutParams lp = new DragLayer.LayoutParams(
                DragLayer.LayoutParams.MATCH_PARENT,
                DragLayer.LayoutParams.MATCH_PARENT);
        lp.gravity = Gravity.FILL;
        lp.ignoreInsets = true;
        launcher.getDragLayer().addView(overlay, lp);
        overlay.applyWave(0f);
        overlay.invalidate();
        overlay.post(overlay::startWave);
    }

    private void initMesh(int width, int height, float density) {
        float cell = CELL_DP * density;
        mMeshW = Math.max(1, (int) Math.ceil(width / cell));
        mMeshH = Math.max(1, (int) Math.ceil(height / cell));
        mRingThickness = RING_THICKNESS_DP * density;
        mMaxRadius = Math.max(width, height) + MAX_EXTRA_DP * density;

        int vertCount = (mMeshW + 1) * (mMeshH + 1);
        mOrigVerts = new float[vertCount * 2];
        mVerts = new float[vertCount * 2];

        int i = 0;
        for (int y = 0; y <= mMeshH; y++) {
            float fy = (float) y * height / mMeshH;
            for (int x = 0; x <= mMeshW; x++) {
                float fx = (float) x * width / mMeshW;
                mOrigVerts[i] = fx;
                mOrigVerts[i + 1] = fy;
                mVerts[i] = fx;
                mVerts[i + 1] = fy;
                i += 2;
            }
        }
    }

    private void startWave() {
        if (!mIsOpen || mSnapshot == null) {
            return;
        }
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        mAnim = anim;
        anim.setDuration(DURATION_MS);
        anim.setInterpolator(new LinearInterpolator());
        anim.addUpdateListener(a -> {
            float radius = ((Float) a.getAnimatedValue()) * mMaxRadius;
            applyWave(radius);
            invalidate();
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mIsOpen) {
                    close(false);
                }
            }
        });
        anim.start();
    }

    /** Port of CM {@code LauncherViewFrameLayout$b.a(FFF)}. */
    private void applyWave(float radius) {
        if (mSnapshot == null || mOrigVerts == null) {
            return;
        }
        final float ring = mRingThickness;
        final float ox = mOriginX;
        final float oy = mOriginY;
        final int w = mSnapshot.getWidth();
        final int h = mSnapshot.getHeight();
        final int vertCount = (mMeshW + 1) * (mMeshH + 1);

        for (int i = 0; i < vertCount; i++) {
            int vi = i * 2;
            float px = mOrigVerts[vi];
            float py = mOrigVerts[vi + 1];
            float dx = px - ox;
            float dy = py - oy;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            float front = dist - radius;

            if (front > -ring && front < 0f) {
                float s = (float) Math.sin((front / ring + 1f) * Math.PI);
                float factor = Math.min(dist / ring, 1f);
                float uv = UV_AMP * s * factor;
                mVerts[vi] = px - uv * w;
                mVerts[vi + 1] = py + uv * h;
            } else {
                mVerts[vi] = px;
                mVerts[vi + 1] = py;
            }
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (mSnapshot != null && !mSnapshot.isRecycled() && mVerts != null) {
            canvas.drawBitmapMesh(mSnapshot, mMeshW, mMeshH, mVerts, 0, null, 0, mPaint);
            return;
        }
        super.dispatchDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    @Override
    protected void handleClose(boolean animate) {
        if (mAnim != null) {
            mAnim.cancel();
            mAnim = null;
        }
        mIsOpen = false;
        if (mSnapshot != null) {
            if (!mSnapshot.isRecycled()) {
                mSnapshot.recycle();
            }
            mSnapshot = null;
        }
        if (getParent() instanceof DragLayer) {
            ((DragLayer) getParent()).removeView(this);
        }
    }

    @Override
    protected boolean isOfType(int type) {
        return (type & TYPE_BOOSTER_OVERLAY) != 0;
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        return true;
    }
}
