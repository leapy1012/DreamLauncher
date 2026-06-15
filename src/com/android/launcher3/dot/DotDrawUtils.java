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

    public static final int MAX_COUNT = 99;
    
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

    public static void draw(Canvas canvas, DotNumParams dotNumParams, boolean  isLargeFolder) {
        if (dotNumParams == null || dotNumParams.mUnreadNum <= 0) {
            Log.e(TAG, "Invalid null argument(s) passed in call to draw.");
            return;
        }
        initPaint(dotNumParams.mScale);
        String text = getUnreadNumText(dotNumParams.mUnreadNum);
        Rect adjustRect = adjustRect(getRect(text));
        Point point = getPoint(adjustRect);
        canvas.save();
        Rect rect = dotNumParams.mDrawParams.iconBounds;
        float width = ((float) rect.left) + (((float) rect.width()) * dotNumParams.mRect[0]);
        float height = ((float) rect.top) + (((float) rect.height()) * dotNumParams.mRect[1]);
        Rect clipBounds = canvas.getClipBounds();
        canvas.translate(((width + ((float) (dotNumParams.mDrawParams.leftAlign ? Math.max(0, clipBounds.left - Math.round(width - ((float) adjustRect.centerX()))) :
                        Math.min(0, clipBounds.right - Math.round(((float) adjustRect.centerX()) + width))))) - ((float) adjustRect.centerX())) + (isLargeFolder ? -6.0f : 0.0f),
                ((height + Math.max(0.0f, ((float) clipBounds.top) - (height - ((float) adjustRect.centerY())))) - ((float) adjustRect.centerY())) + (isLargeFolder ? 45.0f : 0.0f));
        float f = dotNumParams.mDrawParams.scale;
        canvas.scale(f, f);
        Paint paint = mPaintX;
        paint.setShadowLayer(0.0f, 0.0f, 0.0f, Color.DKGRAY);
        paint.setColor(dotNumParams.mDrawParams.dotColor);
        canvas.drawRoundRect(new RectF(adjustRect), (float) adjustRect.centerY(), (float) adjustRect.centerY(), paint);
        canvas.drawText(text, (float) point.x, (float) point.y, mPaint);
        canvas.restore();
    }

    public static void initPaint(int i) {
        Paint paint = mPaint;
        paint.setTextSize((((float) i) * 0.16f) - 1.0f);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);
    }
}
