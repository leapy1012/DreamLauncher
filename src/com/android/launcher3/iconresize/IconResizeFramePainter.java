package com.android.launcher3.iconresize;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.R;

/**
 * Oppo {@code IResizeFramePainter}: frame outline on the icon view, handle arc on the overlay.
 */
public final class IconResizeFramePainter {

    private static final float SMOOTH_WEIGHT = ResizeFramePathHelper.SMOOTH_WEIGHT;
    private static final Rect sTmpRect = new Rect();
    private static final Path sTmpPath = new Path();

    private IconResizeFramePainter() {}

    /** Draws the resize outline in {@link BubbleTextView} coordinates (Oppo drawResizeFrame). */
    public static void drawFrame(Canvas canvas, BubbleTextView icon,
            ResizeFrameStrokeState state) {
        Object tag = icon.getTag();
        if (!(tag instanceof com.android.launcher3.model.data.ItemInfo info)) {
            return;
        }
        int spanX = IconResizeHelper.normalizeSpan(info.spanX);
        int spanY = IconResizeHelper.normalizeSpan(info.spanY);
        IconResizeHelper.getResizeFrameBounds(icon, spanX, spanY, sTmpRect);
        drawFrame(canvas, icon, state, sTmpRect);
    }

    public static void drawFrame(Canvas canvas, BubbleTextView icon,
            ResizeFrameStrokeState state, Rect bounds) {
        if (!state.isActive() || state.getStrokeAlpha() <= 0f) {
            return;
        }
        Object tag = icon.getTag();
        if (!(tag instanceof com.android.launcher3.model.data.ItemInfo info)) {
            return;
        }
        if (bounds.isEmpty()) {
            return;
        }
        sTmpRect.set(bounds);
        int spanX = IconResizeHelper.normalizeSpan(info.spanX);
        int spanY = IconResizeHelper.normalizeSpan(info.spanY);

        float stroke = state.getFrameStrokePx();
        sTmpPath.reset();
        ResizeFramePathHelper.buildFramePath(sTmpPath, sTmpRect, spanX, spanY,
                icon.getIconSize(), stroke, icon.getContext());

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(stroke);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        int color = getFrameColor(icon.getContext());
        paint.setColor(color);
        paint.setAlpha(Math.round(Color.alpha(color) * state.getStrokeAlpha()));
        canvas.drawPath(sTmpPath, paint);
    }

    /**
     * Draws the bottom-right handle arc in overlay coordinates (Oppo ItemResizeFrame.dispatchDraw).
     */
    public static void drawHandle(Canvas canvas, int overlayWidth, int overlayHeight,
            Context context, int iconSizePx, int spanX, int spanY,
            ResizeFrameStrokeState state) {
        if (!state.isActive() || state.getStrokeAlpha() <= 0f
                || overlayWidth <= 0 || overlayHeight <= 0) {
            return;
        }
        spanX = IconResizeHelper.normalizeSpan(spanX);
        spanY = IconResizeHelper.normalizeSpan(spanY);

        float frameStroke = state.getFrameStrokePx();
        float handleStroke = state.getHandleStrokePx();
        ResizeFramePathHelper.buildHandlePath(sTmpPath, overlayWidth, overlayHeight,
                spanX, spanY, iconSizePx, frameStroke, handleStroke, context);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(handleStroke);
        paint.setStrokeCap(Paint.Cap.ROUND);
        int color = getHandleColor(context);
        paint.setColor(color);
        paint.setAlpha(Math.round(255f * state.getStrokeAlpha()));
        canvas.drawPath(sTmpPath, paint);
    }

    private static int getFrameColor(Context context) {
        // Oppo item_resize_frame_dark_color on typical dark wallpapers.
        return context.getColor(R.color.item_resize_frame_dark_color);
    }

    private static int getHandleColor(Context context) {
        return context.getColor(R.color.item_resize_handle_dark_color);
    }

    /** Corner radius for handle arc — Oppo getResizeFrameRadius / getHandleRadius. */
    public static float getHandleArcRadius(Context context, int iconSizePx, int spanX, int spanY,
            int boundsW, int boundsH) {
        return ResizeFramePathHelper.getHandleCornerRadius(
                context, iconSizePx, spanX, spanY, boundsW, boundsH);
    }
}
