package com.android.launcher3.iconresize;

import android.content.Context;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;

import com.android.launcher3.R;
import com.oplus.graphics.OplusPath;

/**
 * Oppo {@code IResizeFramePainter.drawResizeFrame} / {@code drawResizeHandle} path geometry.
 */
public final class ResizeFramePathHelper {

    /** Oppo {@code addSmoothRoundRect(..., 1.1f)} for workspace icons. */
    public static final float SMOOTH_WEIGHT = 1.1f;

    private static final RectF sTmpRectF = new RectF();

    private ResizeFramePathHelper() {}

    /**
     * Frame corner radius: theme squircle for 1×1, morph radius for extended spans.
     */
    public static float getFrameCornerRadius(Context context, int iconSizePx, int spanX, int spanY,
            int width, int height) {
        spanX = IconResizeHelper.normalizeSpan(spanX);
        spanY = IconResizeHelper.normalizeSpan(spanY);
        if (spanX == 1 && spanY == 1) {
            return getThemeIconCornerRadius(context, iconSizePx);
        }
        return MorphShapeHelper.getFallbackCornerRadius(spanX, spanY, width, height);
    }

    public static float getHandleCornerRadius(Context context, int iconSizePx, int spanX,
            int spanY, int width, int height) {
        spanX = IconResizeHelper.normalizeSpan(spanX);
        spanY = IconResizeHelper.normalizeSpan(spanY);
        if (spanX == 1 && spanY == 1) {
            return getThemeIconCornerRadius(context, iconSizePx);
        }
        if (spanX == 2 && spanY == 2) {
            return context.getResources().getDimension(R.dimen.icon_resize_handle_radius_2x2)
                    * scaleForIconSize(context, iconSizePx);
        }
        return context.getResources().getDimension(R.dimen.icon_resize_handle_radius_1xx)
                * scaleForIconSize(context, iconSizePx);
    }

    /** Builds the frame outline path in the given bounds (Oppo smooth-round or morph pill). */
    public static void buildFramePath(Path out, Rect bounds, int spanX, int spanY,
            int iconSizePx, float strokeWidthPx, Context context) {
        spanX = IconResizeHelper.normalizeSpan(spanX);
        spanY = IconResizeHelper.normalizeSpan(spanY);
        sTmpRectF.set(bounds);
        float inset = -strokeWidthPx / 2f;
        sTmpRectF.inset(inset, inset);

        if (spanX > 1 || spanY > 1) {
            out.set(MorphShapeHelper.getMorphMaskPath(spanX, spanY,
                    Math.round(sTmpRectF.width()), Math.round(sTmpRectF.height())));
            out.offset(sTmpRectF.left, sTmpRectF.top);
            return;
        }

        float radius = getThemeIconCornerRadius(context, iconSizePx);
        out.reset();
        new OplusPath(out).addSmoothRoundRect(
                sTmpRectF, radius, radius, SMOOTH_WEIGHT, Path.Direction.CCW);
    }

    /** Oppo {@code inHandleHotRect}: bottom-right touch target for the resize handle. */
    public static void getHandleHotRect(Rect out, int frameWidth, int frameHeight,
            int spanX, int spanY, Context context) {
        spanX = IconResizeHelper.normalizeSpan(spanX);
        spanY = IconResizeHelper.normalizeSpan(spanY);
        int hot = context.getResources().getDimensionPixelSize(
                spanX == 1 && spanY == 1
                        ? R.dimen.icon_resize_handle_hot_region_1x1
                        : R.dimen.icon_resize_handle_hot_region);
        out.set(frameWidth - hot, frameHeight - hot, frameWidth, frameHeight);
    }

    public static boolean isInHandleHotRect(int localX, int localY, int frameWidth,
            int frameHeight, int spanX, int spanY, Context context) {
        Rect hot = new Rect();
        getHandleHotRect(hot, frameWidth, frameHeight, spanX, spanY, context);
        return hot.contains(localX, localY);
    }

    /** Oppo {@code LauncherIconConfig.getIconRadius(iconSize)} approximation. */
    private static float getThemeIconCornerRadius(Context context, int iconSizePx) {
        float ratio = context.getResources().getFraction(
                R.fraction.icon_resize_frame_corner_ratio, 1, 1);
        return iconSizePx * ratio;
    }

    /** Oppo scales handle radius by iconSize / badgeBaseline (~icon grid size). */
    private static float scaleForIconSize(Context context, int iconSizePx) {
        int baseline = context.getResources().getDimensionPixelSize(
                R.dimen.icon_resize_radius_baseline_icon);
        if (baseline <= 0) {
            return 1f;
        }
        return iconSizePx / (float) baseline;
    }

    /**
     * Oppo {@code drawResizeHandle}: quarter-arc aligned to the smooth frame corner.
     */
    public static void buildHandlePath(Path out, int width, int height, int spanX, int spanY,
            int iconSizePx, float frameStrokeWidthPx, float handleStrokeWidthPx,
            Context context) {
        spanX = IconResizeHelper.normalizeSpan(spanX);
        spanY = IconResizeHelper.normalizeSpan(spanY);

        float handleR = getHandleCornerRadius(context, iconSizePx, spanX, spanY, width, height);
        float sweep = getHandleSweep(spanX, spanY);
        float offset = getHandleOffset(spanX, spanY);

        float halfHandle = handleStrokeWidthPx / 2f;
        float right = width - halfHandle;
        float bottom = height - halfHandle;
        RectF oval = new RectF(
                right - handleR * 2f,
                bottom - handleR * 2f,
                right,
                bottom);
        float frameHalf = frameStrokeWidthPx / 2f;
        oval.offset(frameHalf, frameHalf);

        out.reset();
        out.addArc(oval, 90f - offset, -sweep);
    }

    /** Oppo {@code getAngle}: 50° on 1×2 / 2×1, 90° on 2×2. */
    private static float getHandleSweep(int spanX, int spanY) {
        if (spanX == 2 && spanY == 2) {
            return 90f;
        }
        return 50f;
    }

    /** Oppo {@code getOffsetAngle}: 20° on middle spans, 0° on 2×2. */
    private static float getHandleOffset(int spanX, int spanY) {
        if (spanX == 2 && spanY == 2) {
            return 0f;
        }
        return 20f;
    }

    public static float getFrameStrokeWidthPx(Context context) {
        return context.getResources().getDimension(R.dimen.icon_resize_frame_stroke_width_max);
    }

    public static float getHandleStrokeWidthPx(Context context) {
        return context.getResources().getDimension(R.dimen.icon_resize_handle_stroke_width_max);
    }
}
