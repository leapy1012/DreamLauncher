package com.android.launcher3.dot;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.graphics.Color;
import com.android.launcher3.icons.DotRenderer;

public class DotDrawUtils {

    public static final int MAX_COUNT = 999;
    
    public static final String TAG = "Unread DotRendererExt";

    public static final String hintText = "  ";

    public static final Paint mPaintX = new Paint(Paint.CURSOR_AT_OR_BEFORE);

    public static final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public static class DotNumParams {

        public float[] mRect;

        public int mScale;

        public DotRenderer.DrawParams mDrawParams;

        public int mUnreadNum;

        public DotNumParams(DotRenderer dotRenderer, int i, NumberDotRenderer.DrawParams drawParams) {
            this.mScale = i;
            this.mRect = drawParams.leftAlign ? dotRenderer.getLeftDotPosition() : dotRenderer.getRightDotPosition();
            this.mDrawParams = drawParams;
            this.mUnreadNum = drawParams.unreadNum;
        }
    }

    public static String getUnreadNumText(int count) {
        return (count <= 0 || count > 99) ? count > 99 ? "99+" : "" : String.valueOf(count);
    }

    public static Rect adjustRect(Rect rect) {
        Rect rect2 = new Rect();
        rect2.left = 0;
        rect2.top = 0;
        rect2.right = rect.right + 20;
        rect2.bottom = rect.bottom + 20;
        return rect2;
    }

    public static Rect getRect(String str) {
        Paint paint = mPaint;
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        Rect rect = new Rect();
        rect.left = 0;
        rect.top = 0;
        rect.right = Math.round(paint.measureText(str));
        rect.bottom = Math.round(fontMetrics.descent - fontMetrics.ascent);
        if (rect.width() <= rect.height()) {
            rect.right = rect.bottom;
        } else {
            rect.right += Math.round(paint.measureText(hintText));
        }
        return rect;
    }

    public static Point getPoint(Rect rect) {
        Point point = new Point();
        Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();
        int height = (rect.height() - Math.round(fontMetrics.descent - fontMetrics.ascent)) >> 1;
        point.x = rect.centerX();
        point.y = rect.top + height + Math.abs(Math.round(fontMetrics.ascent));
        return point;
    }

    private static void drawLegacy(Canvas canvas, DotNumParams dotNumParams, boolean isLargeFolder) {
        if (dotNumParams == null || dotNumParams.mUnreadNum <= 0) {
            Log.e(TAG, "Invalid null argument(s) passed in call to draw.");
            return;
        }
        NumberDotRenderer.DrawParams drawParams =
                (NumberDotRenderer.DrawParams) dotNumParams.mDrawParams;
        initPaintLegacy(drawParams.textSize);
        String text = getUnreadNumText(dotNumParams.mUnreadNum);
        // Decoded OplusDotRenderer uses a 20dp circular minimum, 13dp bold text and 2dp
        // ellipsize padding. Derive the pixel dimensions from the supplied 13dp text resource.
        int diameter = Math.round(drawParams.textSize * (20f / 13f));
        int textPadding = Math.round(drawParams.textSize * (2f / 13f));
        int textWidth = Math.round(mPaint.measureText(text));
        Rect adjustRect = new Rect(0, 0, Math.max(diameter, textWidth + textPadding * 2),
                diameter);
        Point point = getPoint(adjustRect);
        canvas.save();
        Rect rect = drawParams.iconBounds;
        float width = ((float) rect.left) + (((float) rect.width()) * dotNumParams.mRect[0]);
        float height = ((float) rect.top) + (((float) rect.height()) * dotNumParams.mRect[1]);
        Rect clipBounds = canvas.getClipBounds();
        canvas.translate(((width + ((float) (drawParams.leftAlign ? Math.max(0, clipBounds.left - Math.round(width - ((float) adjustRect.centerX()))) :
                        Math.min(0, clipBounds.right - Math.round(((float) adjustRect.centerX()) + width))))) - ((float) adjustRect.centerX())) + (isLargeFolder ? -6.0f : 0.0f),
                ((height + Math.max(0.0f, ((float) clipBounds.top) - (height - ((float) adjustRect.centerY())))) - ((float) adjustRect.centerY())) + (isLargeFolder ? 45.0f : 0.0f));
        float f = drawParams.scale;
        canvas.scale(f, f);
        Paint paint = mPaintX;
        paint.setShadowLayer(0.0f, 0.0f, 0.0f, Color.DKGRAY);
        paint.setColor(drawParams.dotColor);
        canvas.drawRoundRect(new RectF(adjustRect), (float) adjustRect.centerY(), (float) adjustRect.centerY(), paint);
        canvas.drawText(text, (float) point.x, (float) point.y, mPaint);
        canvas.restore();
    }

    private static void initPaintLegacy(float textSize) {
        Paint paint = mPaint;
        paint.setTextSize(textSize);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);
    }

    public static void draw(Canvas canvas, DotNumParams dotNumParams, boolean isLargeFolder) {
        if (dotNumParams == null || dotNumParams.mUnreadNum <= 0) {
            Log.e(TAG, "Invalid null argument(s) passed in call to draw.");
            return;
        }
        NumberDotRenderer.DrawParams drawParams =
                (NumberDotRenderer.DrawParams) dotNumParams.mDrawParams;
        initPaint(drawParams.textSize, drawParams.badgeTextColor);

        int count = dotNumParams.mUnreadNum;
        String measuredText = String.valueOf(Math.min(count, MAX_COUNT));
        int diameter = Math.round(drawParams.textSize * (20f / 13f));
        int designOffset = Math.round(drawParams.textSize * (3f / 13f));
        int ellipsize = Math.round(drawParams.textSize * (2f / 13f));
        int textPadding = Math.max(0,
                (diameter - Math.round(mPaint.measureText("0"))) / 2);
        int badgeWidth = Math.max(diameter,
                Math.round(mPaint.measureText(measuredText)) + textPadding * 2);

        Rect iconBounds = drawParams.iconBounds;
        Rect canvasBounds = canvas.getClipBounds();
        int[] centerOffset = calculateBadgeCenterOffset(iconBounds, canvasBounds,
                drawParams.leftAlign, count, badgeWidth, diameter, designOffset);
        float anchorX = drawParams.leftAlign ? iconBounds.left : iconBounds.right;
        float centerX = anchorX + centerOffset[0] + (isLargeFolder ? -6f : 0f);
        float centerY = iconBounds.top + centerOffset[1] + (isLargeFolder ? 45f : 0f);

        canvas.save();
        canvas.translate(centerX, centerY);
        canvas.scale(drawParams.scale, drawParams.scale);

        Paint backgroundPaint = mPaintX;
        backgroundPaint.clearShadowLayer();
        backgroundPaint.setAntiAlias(true);
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setColor(drawParams.badgeColor);
        float halfWidth = badgeWidth * 0.5f;
        float halfHeight = diameter * 0.5f;
        canvas.drawRoundRect(new RectF(-halfWidth, -halfHeight, halfWidth, halfHeight),
                halfHeight, halfHeight, backgroundPaint);

        if (count > MAX_COUNT) {
            drawOverflowEllipsis(canvas, ellipsize, drawParams.badgeTextColor);
        } else {
            float baseline = -(mPaint.ascent() + mPaint.descent()) * 0.5f;
            canvas.drawText(measuredText, 0f, baseline, mPaint);
        }
        canvas.restore();
    }

    private static int[] calculateBadgeCenterOffset(Rect iconBounds, Rect canvasBounds,
            boolean leftAlign, int count, int badgeWidth, int diameter, int designOffset) {
        int anchorX = leftAlign ? iconBounds.left : iconBounds.right;
        int anchorY = iconBounds.top;
        int horizontalSpace = leftAlign
                ? anchorX - canvasBounds.left : canvasBounds.right - anchorX;
        int verticalSpace = anchorY - canvasBounds.top;
        int radius = diameter / 2;
        int centerX;
        int centerY;
        if (horizontalSpace < radius || verticalSpace < radius) {
            int delta = Math.max(Math.max(radius - horizontalSpace,
                    radius - verticalSpace), designOffset);
            int widthDelta = badgeWidth / 2 - radius;
            centerX = leftAlign
                    ? anchorX + delta + widthDelta : anchorX - delta - widthDelta;
            centerY = anchorY + delta;
        } else {
            if (count > 9) {
                int widthDelta = (badgeWidth - diameter) / 2;
                anchorX = leftAlign ? anchorX + widthDelta : anchorX - widthDelta;
            }
            centerX = leftAlign ? anchorX + designOffset : anchorX - designOffset;
            centerY = anchorY + designOffset;
        }
        return new int[] {centerX - (leftAlign ? iconBounds.left : iconBounds.right),
                centerY - iconBounds.top};
    }

    private static void drawOverflowEllipsis(Canvas canvas, int ellipsize, int color) {
        Paint ellipsisPaint = mPaint;
        ellipsisPaint.setColor(color);
        float radius = ellipsize * 0.5f;
        canvas.save();
        canvas.translate(-(ellipsize * 5f) * 0.5f, -ellipsize * 0.5f);
        canvas.drawCircle(radius, radius, radius, ellipsisPaint);
        canvas.drawCircle(5f * radius, radius, radius, ellipsisPaint);
        canvas.drawCircle(9f * radius, radius, radius, ellipsisPaint);
        canvas.restore();
    }

    private static void initPaint(float textSize, int textColor) {
        Paint paint = mPaint;
        paint.setTextSize(textSize);
        paint.setTypeface(Typeface.create("sans-serif-regular", Typeface.NORMAL));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setAntiAlias(true);
        paint.setColor(textColor);
    }
}
