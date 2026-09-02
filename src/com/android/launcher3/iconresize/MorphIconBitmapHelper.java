package com.android.launcher3.iconresize;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

/**
 * Bitmap utilities ported from Oppo {@code UxIconLoaderUtil} for morph icon rasterization.
 */
public final class MorphIconBitmapHelper {

    /** Slightly darkens mid-tones on span layer masks for a stronger center highlight. */
    private static final float LAYER_LUMINANCE_GAMMA = 1.22f;

    private MorphIconBitmapHelper() {}

    @Nullable
    public static Bitmap drawableToBitmap(Drawable drawable, int width, int height) {
        if (drawable == null || width <= 0 || height <= 0) {
            return null;
        }
        Bitmap full = drawableToBitmap(drawable);
        if (full == null) {
            return null;
        }
        Bitmap scaled = Bitmap.createScaledBitmap(full, width, height, true);
        if (scaled != full) {
            full.recycle();
        }
        return scaled;
    }

    @Nullable
    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();
        if (w <= 0 || h <= 0) {
            w = 1;
            h = 1;
        }
        Rect bounds = drawable.getBounds();
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, w, h);
        drawable.draw(canvas);
        drawable.setBounds(bounds);
        return bitmap;
    }

    /** Oppo {@code clipTransparentEdges(drawable, false)} — crop adaptive-icon padding. */
    @Nullable
    public static Drawable clipTransparentEdges(Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        Bitmap src = drawableToBitmap(drawable);
        if (src == null) {
            return drawable;
        }
        int width = src.getWidth();
        int height = src.getHeight();
        int left = findTransparentEdge(src, width, height, true, true);
        int top = findTransparentEdge(src, width, height, false, true);
        int right = findTransparentEdge(src, width, height, true, false);
        int bottom = findTransparentEdge(src, width, height, false, false);
        if (left <= 0 && top <= 0 && right <= 0 && bottom <= 0) {
            return drawable;
        }
        int cropW = width - left - right;
        int cropH = height - top - bottom;
        if (cropW % 2 != 0) {
            cropW--;
        }
        if (cropH % 2 != 0) {
            cropH--;
        }
        if (cropW <= 0 || cropH <= 0) {
            src.recycle();
            return drawable;
        }
        Bitmap cropped = Bitmap.createBitmap(cropW, cropH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(cropped);
        Rect srcRect = new Rect(left, top, left + cropW, top + cropH);
        canvas.drawBitmap(src, srcRect, new Rect(0, 0, cropW, cropH), new Paint(Paint.ANTI_ALIAS_FLAG));
        src.recycle();
        return new BitmapDrawable(cropped);
    }

    /** Boost span-layer contrast before tint compositing. */
    @Nullable
    public static Bitmap enhanceLayerLuminance(@Nullable Bitmap layer) {
        if (layer == null || layer.isRecycled()) {
            return layer;
        }
        int width = layer.getWidth();
        int height = layer.getHeight();
        if (width <= 0 || height <= 0) {
            return layer;
        }
        Bitmap out = layer.copy(Bitmap.Config.ARGB_8888, true);
        if (out != layer) {
            layer.recycle();
        }
        int[] pixels = new int[width * height];
        out.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int a = (pixel >> 24) & 0xFF;
            if (a == 0) {
                continue;
            }
            int r = applyGamma((pixel >> 16) & 0xFF);
            int g = applyGamma((pixel >> 8) & 0xFF);
            int b = applyGamma(pixel & 0xFF);
            pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        out.setPixels(pixels, 0, width, 0, 0, width, height);
        return out;
    }

    private static int applyGamma(int channel) {
        float normalized = channel / 255f;
        return Math.round((float) Math.pow(normalized, LAYER_LUMINANCE_GAMMA) * 255f);
    }

    /** Oppo {@code findTransparentEdge}. */
    private static int findTransparentEdge(Bitmap bitmap, int width, int height, boolean horizontal,
            boolean fromStart) {
        if (width <= 0 || height <= 0) {
            return 0;
        }
        int primary = horizontal ? width : height;
        int secondary = horizontal ? height : width;
        for (int i = 0; i < primary; i++) {
            int primaryIndex = fromStart ? i : (primary - 1 - i);
            for (int j = 0; j < secondary; j++) {
                int x = horizontal ? primaryIndex : j;
                int y = horizontal ? j : primaryIndex;
                if ((bitmap.getPixel(x, y) >>> 24) != 0) {
                    return i;
                }
            }
        }
        return 0;
    }
}
