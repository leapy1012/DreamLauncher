package com.android.launcher3.iconresize;

import android.graphics.Matrix;
import android.graphics.Path;

import androidx.core.graphics.PathParser;

/**
 * Oppo {@code MorphIconUtils} morph silhouette paths (pill / squircle), scaled to plate bounds.
 */
public final class MorphShapeHelper {

    /** Oppo {@code SPAN_1X2_MASK_PATH} — vertical pill. */
    private static final String SPAN_1X2_MASK_PATH =
            "M32,0L76,0A32,32 0,0 1,108 32L108,256A32,32 0,0 1,76 288L32,288A32,32 0,0 1,0 256L0,32A32,32 0,0 1,32 0z";
    /** Oppo {@code SPAN_2X1_MASK_PATH} — horizontal pill. */
    private static final String SPAN_2X1_MASK_PATH =
            "M32,0L256,0A32,32 0,0 1,288 32L288,76A32,32 0,0 1,256 108L32,108A32,32 0,0 1,0 76L0,32A32,32 0,0 1,32 0z";
    /** Oppo {@code SPAN_2X2_MASK_PATH} — large squircle. */
    private static final String SPAN_2X2_MASK_PATH =
            "M40,0L248,0A40,40 0,0 1,288 40L288,248A40,40 0,0 1,248 288L40,288A40,40 0,0 1,0 248L0,40A40,40 0,0 1,40 0z";

    private static final float BASE_1X2_W = 108f;
    private static final float BASE_1X2_H = 288f;
    private static final float BASE_2X1_W = 288f;
    private static final float BASE_2X1_H = 108f;
    private static final float BASE_2X2 = 288f;
    private static final float CORNER_1X2 = 32f;
    private static final float CORNER_2X1 = 32f;
    private static final float CORNER_2X2 = 40f;

    private MorphShapeHelper() {}

    public static Path getMorphMaskPath(int spanX, int spanY, int width, int height) {
        spanX = IconResizeHelper.normalizeSpan(spanX);
        spanY = IconResizeHelper.normalizeSpan(spanY);
        Path base = PathParser.createPathFromPathData(getMaskPathData(spanX, spanY));
        float baseW = getBaseWidth(spanX, spanY);
        float baseH = getBaseHeight(spanX, spanY);
        Matrix matrix = new Matrix();
        matrix.setScale(width / baseW, height / baseH);
        Path out = new Path();
        base.transform(matrix, out);
        return out;
    }

    /** Approximate corner radius when the vector mask is unavailable (fallback only). */
    public static float getFallbackCornerRadius(int spanX, int spanY, int width, int height) {
        spanX = IconResizeHelper.normalizeSpan(spanX);
        spanY = IconResizeHelper.normalizeSpan(spanY);
        if (spanX == 1 && spanY == 2) {
            return CORNER_1X2 / BASE_1X2_W * width;
        }
        if (spanX == 2 && spanY == 1) {
            return CORNER_2X1 / BASE_2X1_H * height;
        }
        if (spanX == 2 && spanY == 2) {
            return CORNER_2X2 / BASE_2X2 * Math.min(width, height);
        }
        return CORNER_2X2 / BASE_2X2 * Math.min(width, height);
    }

    /** Pick morph mask for in-between transition geometry from plate aspect ratio. */
    public static Path getMorphMaskPathForSize(int width, int height) {
        if (width <= 0 || height <= 0) {
            return new Path();
        }
        float ratio = (float) width / height;
        if (ratio < 0.72f) {
            return getMorphMaskPath(1, 2, width, height);
        }
        if (ratio > 1.38f) {
            return getMorphMaskPath(2, 1, width, height);
        }
        return getMorphMaskPath(2, 2, width, height);
    }

    private static String getMaskPathData(int spanX, int spanY) {
        if (spanX == 1 && spanY == 2) {
            return SPAN_1X2_MASK_PATH;
        }
        if (spanX == 2 && spanY == 1) {
            return SPAN_2X1_MASK_PATH;
        }
        return SPAN_2X2_MASK_PATH;
    }

    private static float getBaseWidth(int spanX, int spanY) {
        if (spanX == 1 && spanY == 2) {
            return BASE_1X2_W;
        }
        if (spanX == 2 && spanY == 1) {
            return BASE_2X1_W;
        }
        return BASE_2X2;
    }

    private static float getBaseHeight(int spanX, int spanY) {
        if (spanX == 1 && spanY == 2) {
            return BASE_1X2_H;
        }
        if (spanX == 2 && spanY == 1) {
            return BASE_2X1_H;
        }
        return BASE_2X2;
    }
}
