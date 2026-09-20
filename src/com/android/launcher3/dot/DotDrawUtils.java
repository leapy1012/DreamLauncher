package com.android.launcher3.dot;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.Log;

import com.android.launcher3.R;
import com.android.launcher3.icons.DotRenderer;

/**
 * Draws ColorOS-style numeric badges on workspace icons and folders.
 * Anchors to the icon's top-right (or top-left) corner with a fixed diameter —
 * matching Oppo {@code OplusDotRenderer#drawNumber} / folder plate badges —
 * instead of AOSP path-percentage dots that sit inset on the glyph.
 */
public class DotDrawUtils {

    public static final int MAX_COUNT = 99;

    public static final String TAG = "Unread DotRendererExt";

    private static final Paint sBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint sTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public static class DotNumParams {

        public float[] mRect;

        public int mScale;

        public DotRenderer.DrawParams mDrawParams;

        public int mUnreadNum;

        public DotNumParams(DotRenderer dotRenderer, int i, NumberDotRenderer.DrawParams drawParams) {
            this.mScale = i;
            this.mRect = drawParams.leftAlign
                    ? dotRenderer.getLeftDotPosition()
                    : dotRenderer.getRightDotPosition();
            this.mDrawParams = drawParams;
            this.mUnreadNum = drawParams.unreadNum;
        }
    }

    public static String getUnreadNumText(int count) {
        if (count <= 0) {
            return "";
        }
        return count > MAX_COUNT ? "99+" : String.valueOf(count);
    }

    /**
     * @param isLargeFolder unused; kept for call-site compatibility.
     */
    public static void draw(Canvas canvas, DotNumParams dotNumParams, boolean isLargeFolder) {
        draw(canvas, null, dotNumParams);
    }

    public static void draw(Canvas canvas, Context context, DotNumParams dotNumParams) {
        if (dotNumParams == null || dotNumParams.mUnreadNum <= 0 || dotNumParams.mDrawParams == null) {
            Log.e(TAG, "Invalid null argument(s) passed in call to draw.");
            return;
        }
        if (context == null) {
            return;
        }

        Resources res = context.getResources();
        int bgDiameter = res.getDimensionPixelSize(R.dimen.badge_num_background_diameter);
        int fontSize = res.getDimensionPixelSize(R.dimen.badge_num_font_size);
        int corner = res.getDimensionPixelSize(R.dimen.badge_num_background_corner);
        int offsetDesign = res.getDimensionPixelSize(R.dimen.badge_num_background_offset_design);

        String text = getUnreadNumText(dotNumParams.mUnreadNum);
        sTextPaint.setTextSize(fontSize);
        sTextPaint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        sTextPaint.setTextAlign(Paint.Align.CENTER);
        sTextPaint.setColor(Color.WHITE);
        sTextPaint.setAntiAlias(true);

        float textWidth = sTextPaint.measureText(text);
        int padding = Math.max(0, (bgDiameter - Math.round(sTextPaint.measureText("0"))) / 2);
        int bgWidth = bgDiameter;
        if (textWidth > bgDiameter - padding * 2f) {
            bgWidth = Math.round(textWidth + padding * 2f);
        }
        int bgHeight = bgDiameter;

        Rect iconBounds = dotNumParams.mDrawParams.iconBounds;
        boolean leftAlign = dotNumParams.mDrawParams.leftAlign;
        Rect clip = canvas.getClipBounds();

        int[] centerOffset = calculateBadgeCenterOffset(
                iconBounds, clip, leftAlign, dotNumParams.mUnreadNum,
                bgWidth, bgDiameter, offsetDesign);

        float centerX = (leftAlign ? iconBounds.left : iconBounds.right) + centerOffset[0];
        float centerY = iconBounds.top + centerOffset[1];

        canvas.save();
        canvas.translate(centerX, centerY);
        float scale = dotNumParams.mDrawParams.scale;
        if (scale != 1f) {
            canvas.scale(scale, scale);
        }

        RectF bg = new RectF(-bgWidth / 2f, -bgHeight / 2f, bgWidth / 2f, bgHeight / 2f);
        sBgPaint.setColor(dotNumParams.mDrawParams.dotColor);
        sBgPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(bg, corner, corner, sBgPaint);

        Paint.FontMetrics fm = sTextPaint.getFontMetrics();
        float textY = -(fm.ascent + fm.descent) / 2f;
        canvas.drawText(text, 0, textY, sTextPaint);
        canvas.restore();
    }

    /**
     * Oppo {@code IconUtils.calculateBadgeCenterOffset}: place badge center near the
     * icon corner with {@code offsetDesign}, allowing overhang when clip space allows.
     */
    private static int[] calculateBadgeCenterOffset(Rect iconBounds, Rect clipBounds,
            boolean leftAlign, int count, int bgWidth, int bgDiameter, int offsetDesign) {
        int half = bgDiameter / 2;
        int cornerX = leftAlign ? iconBounds.left : iconBounds.right;
        int cornerY = iconBounds.top;

        int spaceX = leftAlign
                ? cornerX - clipBounds.left
                : clipBounds.right - cornerX;
        int spaceY = cornerY - clipBounds.top;

        int centerX;
        int centerY;
        if (spaceX < half || spaceY < half) {
            // Not enough room outside — push inward just enough to keep the pill visible.
            int delta = Math.max(Math.max(half - spaceX, half - spaceY), offsetDesign);
            int wideAdjust = (bgWidth / 2) - half;
            centerX = leftAlign
                    ? cornerX + delta + wideAdjust
                    : cornerX - delta - wideAdjust;
            centerY = cornerY + delta;
        } else {
            if (count > 9) {
                int wideAdjust = (bgWidth - bgDiameter) / 2;
                cornerX = leftAlign ? cornerX + wideAdjust : cornerX - wideAdjust;
            }
            // Sit like folder plate badge: slightly inset from the corner so ~half hangs off.
            centerX = leftAlign ? cornerX + offsetDesign : cornerX - offsetDesign;
            centerY = cornerY + offsetDesign;
        }
        return new int[]{centerX - (leftAlign ? iconBounds.left : iconBounds.right),
                centerY - iconBounds.top};
    }
}
