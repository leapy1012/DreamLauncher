package com.android.launcher3.iconresize;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Oppo-style morph plate: {@code ux_icon_mt_layer_*} mask + {@code getHsbFgColor} tint, centered fg.
 */
public class MorphWorkspaceIconDrawable extends Drawable {

    public enum ScaleMode {
        CENTER,
        FILL,
        MORPH
    }

    private static final Rect sTmpInner = new Rect();

    private final Drawable mInner;
    private final int mIconSize;
    private final int mPlateW;
    private final int mPlateH;
    private final int mSpanX;
    private final int mSpanY;
    private final float mCornerRadius;
    private final ScaleMode mScaleMode;
    private final boolean mRasterForeground;
    private final Path mMorphMask = new Path();
    @Nullable
    private final Bitmap mPlateBitmap;
    private final Paint mPlatePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint mFallbackBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public MorphWorkspaceIconDrawable(Context context, Drawable inner, int iconSize, int spanX,
            int spanY, int plateW, int plateH, @Nullable ComponentName cn, float cornerRadius) {
        this(context, inner, iconSize, spanX, spanY, plateW, plateH, cn, cornerRadius,
                ScaleMode.MORPH);
    }

    public MorphWorkspaceIconDrawable(Context context, Drawable inner, int iconSize, int spanX,
            int spanY, int plateW, int plateH, @Nullable ComponentName cn, float cornerRadius,
            ScaleMode scaleMode) {
        mInner = inner;
        mIconSize = iconSize;
        mPlateW = plateW;
        mPlateH = plateH;
        mSpanX = spanX;
        mSpanY = spanY;
        mCornerRadius = cornerRadius;
        mScaleMode = scaleMode;
        mRasterForeground = MorphForegroundHelper.isFullPlateForeground(inner, plateW, plateH);
        mPlateBitmap = MorphPlateColorHelper.buildCoverupPlateBitmap(context, spanX, spanY, plateW,
                plateH, cn, inner);
        mFallbackBgPaint.setColor(
                MorphPlateColorHelper.getFallbackPlateColor(context, cn, inner));
        mFallbackBgPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect b = getBounds();
        if (b.isEmpty()) {
            return;
        }
        updateMorphMask(b);
        canvas.save();
        canvas.clipPath(mMorphMask);
        drawPlate(canvas, b);
        switch (mScaleMode) {
            case FILL -> drawInnerFill(canvas, b);
            case CENTER -> drawInnerCentered(canvas, b);
            default -> drawInnerMorph(canvas, b);
        }
        canvas.restore();
    }

    private void updateMorphMask(Rect b) {
        Path local = MorphShapeHelper.getMorphMaskPath(mSpanX, mSpanY, b.width(), b.height());
        Matrix matrix = new Matrix();
        matrix.setTranslate(b.left, b.top);
        local.transform(matrix, mMorphMask);
    }

    private void drawPlate(Canvas canvas, Rect b) {
        if (mPlateBitmap != null && !mPlateBitmap.isRecycled()) {
            canvas.drawBitmap(mPlateBitmap, null, b, mPlatePaint);
        } else {
            Path local = MorphShapeHelper.getMorphMaskPath(mSpanX, mSpanY, b.width(), b.height());
            canvas.save();
            canvas.translate(b.left, b.top);
            canvas.drawPath(local, mFallbackBgPaint);
            canvas.restore();
        }
    }

    private void drawInnerMorph(Canvas canvas, Rect b) {
        if (mRasterForeground) {
            mInner.setBounds(b);
            mInner.draw(canvas);
            return;
        }
        MorphIconLayoutHelper.computeInnerBounds(b, mSpanX, mSpanY, mInner, mIconSize, sTmpInner);
        if (!sTmpInner.isEmpty()) {
            mInner.setBounds(sTmpInner);
            mInner.draw(canvas);
        }
    }

    private void drawInnerCentered(Canvas canvas, Rect b) {
        int iw = mIconSize;
        int ih = mIconSize;
        int left = b.left + (b.width() - iw) / 2;
        int top = b.top + (b.height() - ih) / 2;
        mInner.setBounds(left, top, left + iw, top + ih);
        mInner.draw(canvas);
    }

    private void drawInnerFill(Canvas canvas, Rect b) {
        int srcW = mIconSize;
        int srcH = mIconSize;
        if (mInner.getIntrinsicWidth() > 0 && mInner.getIntrinsicHeight() > 0) {
            srcW = mInner.getIntrinsicWidth();
            srcH = mInner.getIntrinsicHeight();
        }
        float scale = Math.max((float) b.width() / srcW, (float) b.height() / srcH);
        int dw = Math.round(srcW * scale);
        int dh = Math.round(srcH * scale);
        int left = b.left + (b.width() - dw) / 2;
        int top = b.top + (b.height() - dh) / 2;
        mInner.setBounds(left, top, left + dw, top + dh);
        mInner.draw(canvas);
    }

    @Override
    public void setAlpha(int alpha) {
        mInner.setAlpha(alpha);
        mPlatePaint.setAlpha(alpha);
        mFallbackBgPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(android.graphics.ColorFilter colorFilter) {
        mInner.setColorFilter(colorFilter);
        mPlatePaint.setColorFilter(colorFilter);
        mFallbackBgPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return mInner.getOpacity();
    }

    @Override
    public int getIntrinsicWidth() {
        return getBounds().width();
    }

    @Override
    public int getIntrinsicHeight() {
        return getBounds().height();
    }

    public Drawable getInner() {
        return mInner;
    }

    public static float getMorphCornerRadius(int spanX, int spanY, int width, int height) {
        return MorphShapeHelper.getFallbackCornerRadius(spanX, spanY, width, height);
    }
}
