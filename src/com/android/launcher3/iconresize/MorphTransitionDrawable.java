package com.android.launcher3.iconresize;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.dynamicanimation.animation.FloatPropertyCompat;

/**
 * Interpolates morph plate geometry between two workspace icon spans (Oppo-style resize morph).
 */
public class MorphTransitionDrawable extends Drawable {

    private static final Rect sTmpPlate = new Rect();
    private static final Rect sTmpFromInner = new Rect();
    private static final Rect sTmpToInner = new Rect();
    private static final Rect sTmpInner = new Rect();

    public static final FloatPropertyCompat<MorphTransitionDrawable> PROGRESS =
            new FloatPropertyCompat<MorphTransitionDrawable>("morphProgress") {
                @Override
                public float getValue(MorphTransitionDrawable object) {
                    return object.mProgress;
                }

                @Override
                public void setValue(MorphTransitionDrawable object, float value) {
                    object.mProgress = value;
                    object.invalidateSelf();
                }
            };

    private final Drawable mInner;
    private final int mIconSize;
    private final int mFromSpanX;
    private final int mFromSpanY;
    private final int mToSpanX;
    private final int mToSpanY;
    private final int mFromW;
    private final int mFromH;
    private final int mToW;
    private final int mToH;
    private final Paint mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mMorphMask = new Path();

    private float mProgress;

    public MorphTransitionDrawable(Context context, Drawable inner, @Nullable ComponentName cn,
            int iconSize, int fromSpanX, int fromSpanY, int fromW, int fromH,
            int toSpanX, int toSpanY, int toW, int toH) {
        mInner = inner;
        mIconSize = iconSize;
        mFromSpanX = fromSpanX;
        mFromSpanY = fromSpanY;
        mToSpanX = toSpanX;
        mToSpanY = toSpanY;
        mFromW = fromW;
        mFromH = fromH;
        mToW = toW;
        mToH = toH;
        mBgPaint.setColor(MorphPlateColorHelper.getFallbackPlateColor(context, cn, inner));
        mBgPaint.setStyle(Paint.Style.FILL);
    }

    public float getProgress() {
        return mProgress;
    }

    public void setProgress(float progress) {
        mProgress = progress;
        invalidateSelf();
    }

    public Drawable getInner() {
        return mInner;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect b = getBounds();
        if (b.isEmpty()) {
            return;
        }
        float t = Math.max(0f, Math.min(1f, mProgress));
        int pw = Math.round(mFromW + (mToW - mFromW) * t);
        int ph = Math.round(mFromH + (mToH - mFromH) * t);

        int left = b.left + (b.width() - pw) / 2;
        int top = b.top + (b.height() - ph) / 2;
        int right = left + pw;
        int bottom = top + ph;

        Path local = MorphShapeHelper.getMorphMaskPathForSize(pw, ph);
        Matrix matrix = new Matrix();
        matrix.setTranslate(left, top);
        local.transform(matrix, mMorphMask);

        canvas.save();
        canvas.clipPath(mMorphMask);
        canvas.drawRect(left, top, right, bottom, mBgPaint);
        drawInnerMorph(canvas, left, top, right, bottom, t);
        canvas.restore();
    }

    private void drawInnerMorph(Canvas canvas, int left, int top, int right, int bottom, float t) {
        sTmpPlate.set(left, top, right, bottom);
        MorphIconLayoutHelper.computeInnerBounds(sTmpPlate, mFromSpanX, mFromSpanY, mInner,
                mIconSize, sTmpFromInner);
        MorphIconLayoutHelper.computeInnerBounds(sTmpPlate, mToSpanX, mToSpanY, mInner,
                mIconSize, sTmpToInner);
        MorphIconLayoutHelper.lerpInnerBounds(sTmpFromInner, sTmpToInner, t, sTmpInner);
        if (!sTmpInner.isEmpty()) {
            mInner.setBounds(sTmpInner);
            mInner.draw(canvas);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        mInner.setAlpha(alpha);
        mBgPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(android.graphics.ColorFilter colorFilter) {
        mInner.setColorFilter(colorFilter);
        mBgPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return mInner.getOpacity();
    }

    static Drawable unwrapBaseIcon(Drawable icon) {
        if (icon instanceof MorphTransitionDrawable td) {
            return td.getInner();
        }
        if (icon instanceof MorphWorkspaceIconDrawable morph) {
            return morph.getInner();
        }
        return icon;
    }

    static Drawable mutateInner(Drawable icon) {
        Drawable base = unwrapBaseIcon(icon);
        if (base.getConstantState() != null) {
            return base.getConstantState().newDrawable().mutate();
        }
        return base;
    }
}
