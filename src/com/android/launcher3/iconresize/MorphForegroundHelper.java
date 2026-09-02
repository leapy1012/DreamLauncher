package com.android.launcher3.iconresize;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

/**
 * Oppo {@code UxIconLoaderUtil.createMorphFgResize}: rasterize foreground at span-specific size,
 * centered in the morph plate canvas.
 */
public final class MorphForegroundHelper {

    private static final float SPAN_1X2_FG_FRAC = 0.74f;
    private static final float SPAN_2X1_FG_FRAC = 0.74f;
    private static final float SPAN_2X2_FG_FRAC = 0.49f;

    private MorphForegroundHelper() {}

    /**
     * Returns a {@code plateW x plateH} bitmap drawable with the icon pre-scaled and centered.
     */
    @Nullable
    public static Drawable createMorphForeground(Drawable icon, Resources res, int plateW,
            int plateH, int spanX, int spanY) {
        if (icon == null || plateW <= 0 || plateH <= 0) {
            return icon;
        }
        Drawable clipped = MorphIconBitmapHelper.clipTransparentEdges(icon);
        if (clipped == null) {
            clipped = icon;
        }

        int targetSide = resolveTargetSide(plateW, plateH, spanX, spanY);
        if (targetSide <= 0) {
            return icon;
        }

        Bitmap iconBmp = rasterizeToMaxSide(clipped, targetSide);
        if (iconBmp == null) {
            return icon;
        }

        Bitmap plateBmp = Bitmap.createBitmap(plateW, plateH, Bitmap.Config.ARGB_8888);
        Canvas plateCanvas = new Canvas(plateBmp);
        Rect src = new Rect(0, 0, iconBmp.getWidth(), iconBmp.getHeight());
        RectF dst = new RectF(
                (plateW - iconBmp.getWidth()) / 2f,
                (plateH - iconBmp.getHeight()) / 2f,
                (plateW + iconBmp.getWidth()) / 2f,
                (plateH + iconBmp.getHeight()) / 2f);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        plateCanvas.drawBitmap(iconBmp, src, dst, paint);
        iconBmp.recycle();

        BitmapDrawable result = new BitmapDrawable(res, plateBmp);
        result.setBounds(0, 0, plateW, plateH);
        return result;
    }

    /** True when foreground is a full-plate raster from {@link #createMorphForeground}. */
    public static boolean isFullPlateForeground(Drawable drawable, int plateW, int plateH) {
        return plateW > 0 && plateH > 0
                && drawable.getIntrinsicWidth() == plateW
                && drawable.getIntrinsicHeight() == plateH;
    }

    /**
     * Oppo {@code createMorphFgResize}: draw icon into a square {@code targetSide} canvas, then
     * scale down uniformly so max(width, height) == targetSide.
     */
    @Nullable
    private static Bitmap rasterizeToMaxSide(Drawable drawable, int targetSide) {
        if (targetSide <= 0) {
            return null;
        }
        Bitmap square = Bitmap.createBitmap(targetSide, targetSide, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(square);
        Rect bounds = drawable.getBounds();
        drawable.setBounds(0, 0, targetSide, targetSide);
        drawable.draw(canvas);
        drawable.setBounds(bounds);

        int width = square.getWidth();
        int height = square.getHeight();
        int max = Math.max(width, height);
        if (max <= 0 || max == targetSide) {
            return square;
        }
        float scale = targetSide / (float) max;
        int scaledW = floatToEvenInt(width * scale);
        int scaledH = floatToEvenInt(height * scale);
        if (scaledW <= 0 || scaledH <= 0) {
            return square;
        }
        Bitmap scaled = Bitmap.createScaledBitmap(square, scaledW, scaledH, true);
        if (scaled != square) {
            square.recycle();
        }
        return scaled;
    }

    /** Oppo {@code floatToEvenInt}. */
    private static int floatToEvenInt(float value) {
        int rounded = Math.round(value);
        return rounded % 2 == 0 ? rounded : rounded + 1;
    }

    private static int resolveTargetSide(int plateW, int plateH, int spanX, int spanY) {
        spanX = IconResizeHelper.normalizeSpan(spanX);
        spanY = IconResizeHelper.normalizeSpan(spanY);
        if (spanX == 1 && spanY == 2) {
            return Math.round(plateW * SPAN_1X2_FG_FRAC);
        }
        if (spanX == 2 && spanY == 1) {
            return Math.round(plateH * SPAN_2X1_FG_FRAC);
        }
        if (spanX == 2 && spanY == 2) {
            return Math.round(plateH * SPAN_2X2_FG_FRAC);
        }
        return Math.min(plateW, plateH);
    }
}
