package com.android.launcher3.dot;

import android.graphics.Path;
import com.android.launcher3.icons.DotRenderer;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import com.android.launcher3.R;
import android.graphics.Rect;

public class NumberDotRenderer extends DotRenderer {
    public boolean mShowNumber;
    private Bitmap mBackgroundCheckIcon;

    public static class DrawParams extends DotRenderer.DrawParams {
        public float textSize;
        public int unreadNum;
        public final int badgeColor;
        public final int badgeTextColor;

        public DrawParams(float f, Context context) {
            this.textSize = f;
            this.badgeColor = context.getColor(R.color.launcher_badge_background_color);
            this.badgeTextColor = context.getColor(R.color.launcher_unread_msg_num_text_color);
        }
    }

    public NumberDotRenderer(int iconSizePx, Path iconShapePath, int pathSize, boolean showNumber) {
        super(iconSizePx, iconShapePath, pathSize);
        this.mShowNumber = showNumber;
    }

    public NumberDotRenderer(int iconSizePx, Path iconShapePath, int pathSize, boolean showNumber, Context context) {
        super(iconSizePx, iconShapePath, pathSize);
        this.mShowNumber = showNumber;
        Drawable checkIcon = context.getResources().getDrawable(R.drawable.ic_select_box_blue_20dp);
        if (mBackgroundCheckIcon == null) {
            mBackgroundCheckIcon = buildScaledBitmap(checkIcon, checkIcon.getIntrinsicWidth(), checkIcon.getIntrinsicHeight());
        }
    }

    private Bitmap buildScaledBitmap(Drawable drawable, int maxWidth, int maxHeight) {
        if (drawable == null) {
            return null;
        }
        int originalWidth = drawable.getIntrinsicWidth();
        int originalHeight = drawable.getIntrinsicHeight();
        if (originalWidth <= maxWidth && originalHeight <= maxHeight && (drawable instanceof BitmapDrawable)) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        if (originalHeight <= 0 || originalWidth <= 0) {
            return null;
        }
        float ratio = Math.min(1.0f, Math.min(((float) maxWidth) / ((float) originalWidth), ((float) maxHeight) / ((float) originalHeight)));
        int scaledWidth = (int) (((float) originalWidth) * ratio);
        int scaledHeight = (int) (((float) originalHeight) * ratio);
        Bitmap result = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        drawable.setBounds(0, 0, scaledWidth, scaledHeight);
        drawable.draw(canvas);
        return result;
    }

    public void drawCheck(Canvas canvas, DotRenderer.DrawParams params) {
        float offsetX;
        if (params != null) {
            canvas.save();
            Rect iconBounds = params.iconBounds;
            float[] dotPosition = params.leftAlign ? getLeftDotPosition() : getRightDotPosition();
            float dotCenterX = ((float) iconBounds.left) + (((float) iconBounds.width()) * dotPosition[0]);
            float dotCenterY = ((float) iconBounds.top) + (((float) iconBounds.height()) * dotPosition[1]);
            Rect canvasBounds = canvas.getClipBounds();
            if (params.leftAlign) {
                offsetX = Math.max(0.0f, ((float) canvasBounds.left) - (getBitmapOffset() + dotCenterX));
            } else {
                offsetX = Math.min(0.0f, ((float) canvasBounds.right) - (dotCenterX - getBitmapOffset()));
            }
            canvas.translate(dotCenterX + offsetX, dotCenterY + Math.max(0.0f, ((float) canvasBounds.top) - (getBitmapOffset() + dotCenterY)));
            canvas.scale(params.scale, params.scale);
            try {
                Bitmap bitmap = mBackgroundCheckIcon;
                float f = getBitmapOffset();
                canvas.drawBitmap(bitmap, f, f, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            canvas.restore();
        }
    }
}
