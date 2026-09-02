package com.android.launcher3.iconresize;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * Oppo {@code createMorphFgResize} + {@code computeForegroundLayerScaleCenterInside}:
 * scale foreground per span, always centered in the morph plate.
 */
public final class MorphIconLayoutHelper {

    /** Oppo {@code createMorphFgResize} mode 0 (1×2): {@code width * 0.74}. */
    private static final float SPAN_1X2_FG_FRAC = 0.74f;
    /** Oppo mode 1 (2×1): {@code height * 0.74}. */
    private static final float SPAN_2X1_FG_FRAC = 0.74f;
    /** Oppo mode 2 (2×2): {@code height * 0.49}. */
    private static final float SPAN_2X2_FG_FRAC = 0.49f;

    private MorphIconLayoutHelper() {}

    public static void computeInnerBounds(Rect plate, int spanX, int spanY, int iconSize,
            Rect out) {
        computeInnerBounds(plate, spanX, spanY, iconSize, iconSize, out);
    }

    public static void computeInnerBounds(Rect plate, int spanX, int spanY, Drawable inner,
            int iconSize, Rect out) {
        int srcW = iconSize;
        int srcH = iconSize;
        if (inner != null && inner.getIntrinsicWidth() > 0 && inner.getIntrinsicHeight() > 0) {
            srcW = inner.getIntrinsicWidth();
            srcH = inner.getIntrinsicHeight();
        }
        computeInnerBounds(plate, spanX, spanY, srcW, srcH, out);
    }

    /**
     * Bounds for foreground inside morph plate — uniform scale to fit span target square, centered.
     */
    public static void computeInnerBounds(Rect plate, int spanX, int spanY, int srcW, int srcH,
            Rect out) {
        spanX = IconResizeHelper.normalizeSpan(spanX);
        spanY = IconResizeHelper.normalizeSpan(spanY);
        if (plate.isEmpty() || srcW <= 0 || srcH <= 0) {
            out.setEmpty();
            return;
        }
        float targetSide = resolveTargetSide(plate, spanX, spanY, Math.max(srcW, srcH));
        float scale = Math.min(targetSide / srcW, targetSide / srcH);
        int dw = Math.round(srcW * scale);
        int dh = Math.round(srcH * scale);
        int left = plate.left + (plate.width() - dw) / 2;
        int top = plate.top + (plate.height() - dh) / 2;
        out.set(left, top, left + dw, top + dh);
    }

    private static float resolveTargetSide(Rect plate, int spanX, int spanY, int iconSize) {
        if (spanX == 1 && spanY == 2) {
            return plate.width() * SPAN_1X2_FG_FRAC;
        }
        if (spanX == 2 && spanY == 1) {
            return plate.height() * SPAN_2X1_FG_FRAC;
        }
        if (spanX == 2 && spanY == 2) {
            return plate.height() * SPAN_2X2_FG_FRAC;
        }
        return iconSize;
    }

    /** Linearly interpolate inner icon bounds during span morph transitions. */
    public static void lerpInnerBounds(Rect from, Rect to, float t, Rect out) {
        out.set(
                Math.round(from.left + (to.left - from.left) * t),
                Math.round(from.top + (to.top - from.top) * t),
                Math.round(from.right + (to.right - from.right) * t),
                Math.round(from.bottom + (to.bottom - from.bottom) * t));
    }
}
