package com.oplus.graphics;

import android.graphics.Path;
import android.graphics.RectF;

/**
 * Software fallback for ColorOS {@code OplusPath.addSmoothRoundRect}.
 * Weight {@code 1.1f} matches Oppo workspace resize-frame corners on non-OEM devices.
 */
class OplusPathFallback implements IOplusPath {

    /** Cubic-bezier circle approximation constant. */
    private static final float BEZIER_CIRCLE_K = 0.5522847498f;

    private final Path mPath;

    OplusPathFallback(Path path) {
        mPath = path;
    }

    @Override
    public void addSmoothRoundRect(float left, float top, float right, float bottom, float rx,
            float ry, float weight, Path.Direction dir) {
        addSmoothRoundRect(new RectF(left, top, right, bottom), rx, ry, weight, dir);
    }

    @Override
    public void addSmoothRoundRect(float left, float top, float right, float bottom, float rx,
            float ry, Path.Direction dir) {
        addSmoothRoundRect(new RectF(left, top, right, bottom), rx, ry, 1f, dir);
    }

    @Override
    public void addSmoothRoundRect(RectF rect, float rx, float ry, Path.Direction dir) {
        addSmoothRoundRect(rect, rx, ry, 1f, dir);
    }

    @Override
    public void addSmoothRoundRect(RectF rect, float rx, float ry, float weight,
            Path.Direction dir) {
        if (rect == null) {
            return;
        }
        float r = Math.min(rx, rect.width() / 2f);
        r = Math.min(r, Math.min(ry, rect.height() / 2f));
        if (r <= 0f) {
            mPath.addRect(rect, dir);
            return;
        }
        if (weight <= 1.001f) {
            mPath.addRoundRect(rect, r, r, dir);
            return;
        }
        float smooth = weight - 1f;
        float k = BEZIER_CIRCLE_K * (1f + smooth * 2f);
        addWeightedRoundRect(rect, r, k, dir);
    }

    @Override
    public void addSmoothRoundRect(RectF rect, float[] radii, Path.Direction dir, float weight) {
        if (rect == null || radii == null || radii.length < 8) {
            return;
        }
        float r = 0f;
        for (int i = 0; i < 8; i++) {
            r = Math.max(r, radii[i]);
        }
        addSmoothRoundRect(rect, r, r, weight, dir);
    }

    @Override
    public void addSmoothRoundRect(RectF rect, float[] radii, Path.Direction dir) {
        addSmoothRoundRect(rect, radii, dir, 1f);
    }

    private void addWeightedRoundRect(RectF rect, float r, float k, Path.Direction dir) {
        float l = rect.left;
        float t = rect.top;
        float rgt = rect.right;
        float b = rect.bottom;
        float rk = r * k;

        if (dir == Path.Direction.CCW) {
            mPath.moveTo(l + r, t);
            mPath.lineTo(rgt - r, t);
            mPath.cubicTo(rgt - r + rk, t, rgt, t + r - rk, rgt, t + r);
            mPath.lineTo(rgt, b - r);
            mPath.cubicTo(rgt, b - r + rk, rgt - r + rk, b, rgt - r, b);
            mPath.lineTo(l + r, b);
            mPath.cubicTo(l + r - rk, b, l, b - r + rk, l, b - r);
            mPath.lineTo(l, t + r);
            mPath.cubicTo(l, t + r - rk, l + r - rk, t, l + r, t);
            mPath.close();
        } else {
            mPath.moveTo(l + r, t);
            mPath.lineTo(l + r, b - r);
            mPath.cubicTo(l + r - rk, b - r + rk, l, b - r + rk, l, b - r);
            mPath.lineTo(rgt - r, b);
            mPath.cubicTo(rgt - r + rk, b, rgt, b - r + rk, rgt, b - r);
            mPath.lineTo(rgt, t + r);
            mPath.cubicTo(rgt, t + r - rk, rgt - r + rk, t, rgt - r, t);
            mPath.lineTo(l + r, t);
            mPath.cubicTo(l + r - rk, t, l, t + r - rk, l, t + r);
            mPath.close();
        }
    }
}
