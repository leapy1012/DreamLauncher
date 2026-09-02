package com.android.launcher3.iconresize;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.android.launcher3.R;

/**
 * generic morph plates (activity-icon dominant color + span layer mask).
 */
public final class MorphPlateColorHelper {

    /** Oppo {@code getHsbFgColor}: {@code (rgb & 0xFFFFFF) | 0xB2000000}. */
    private static final int MORPH_PLATE_ALPHA = 0xB2;

    private MorphPlateColorHelper() {}

    /** Foreground for morph: high-res activity icon when available. */
    @Nullable
    public static Drawable loadMorphForeground(Context context, @Nullable ComponentName cn,
            Drawable fallback) {
        Drawable activity = loadActivityIcon(context, cn);
        if (activity == null) {
            return fallback;
        }
        if (activity.getConstantState() != null) {
            return activity.getConstantState().newDrawable().mutate();
        }
        return activity;
    }

    /** Oppo {@code getHsbFgColor} plate tint from activity icon dominant color. */
    public static int getMorphPlateTintColor(Context context, @Nullable ComponentName cn,
            Drawable fallbackIcon) {
        Drawable src = loadActivityIcon(context, cn);
        if (src == null) {
            src = fallbackIcon;
        }
        int dominant = getDominantColor(compressBeforeGetColor(src));
        if (dominant == Color.TRANSPARENT) {
            return Color.parseColor("#B2383838");
        }
        return (dominant & 0xFFFFFF) | (MORPH_PLATE_ALPHA << 24);
    }

    /**
     * Pre-composited morph plate bitmap (layer mask + tint), matching Oppo coverup bg.
     */
    @Nullable
    public static Bitmap buildCoverupPlateBitmap(Context context, int spanX, int spanY,
            int width, int height, @Nullable ComponentName cn, Drawable fallbackIcon) {
        if (width <= 0 || height <= 0) {
            return null;
        }
        @DrawableRes int layerRes = getMorphLayerResId(spanX, spanY);
        Drawable layer = ContextCompat.getDrawable(context, layerRes);
        if (layer == null) {
            return null;
        }
        int tint = getMorphPlateTintColor(context, cn, fallbackIcon);
        Bitmap layerBitmap = MorphIconBitmapHelper.drawableToBitmap(layer, width, height);
        if (layerBitmap == null) {
            return null;
        }
        layerBitmap = MorphIconBitmapHelper.enhanceLayerLuminance(layerBitmap);
        Bitmap out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Path mask = MorphShapeHelper.getMorphMaskPath(spanX, spanY, width, height);
        canvas.save();
        canvas.clipPath(mask);
        Paint layerPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(layerBitmap, 0, 0, layerPaint);
        layerBitmap.recycle();
        // Oppo createCoverupBgDrawable: canvas.drawColor(getHsbFgColor(...)) — SRC_OVER default.
        canvas.drawColor(tint);
        canvas.restore();
        return out;
    }

    @DrawableRes
    public static int getMorphLayerResId(int spanX, int spanY) {
        spanX = IconResizeHelper.normalizeSpan(spanX);
        spanY = IconResizeHelper.normalizeSpan(spanY);
        if (spanX == 1 && spanY == 2) {
            return R.drawable.ux_icon_mt_layer_1x2;
        }
        if (spanX == 2 && spanY == 1) {
            return R.drawable.ux_icon_mt_layer_2x1;
        }
        if (spanX == 2 && spanY == 2) {
            return R.drawable.ux_icon_mt_layer_2x2;
        }
        return R.drawable.ux_icon_mt_layer_2x2;
    }

    /** Fallback opaque plate color when layer assets are unavailable. */
    public static int getFallbackPlateColor(Context context, @Nullable ComponentName cn,
            Drawable fallbackIcon) {
        return compositeOnWhite(getMorphPlateTintColor(context, cn, fallbackIcon));
    }

    /** Visible plate color when drawn over the workspace (approximate tint over white). */
    static int compositeOnWhite(int argb) {
        int a = Color.alpha(argb);
        if (a == 0) {
            return Color.WHITE;
        }
        float alpha = a / 255f;
        int r = Math.round(Color.red(argb) * alpha + 255 * (1f - alpha));
        int g = Math.round(Color.green(argb) * alpha + 255 * (1f - alpha));
        int b = Math.round(Color.blue(argb) * alpha + 255 * (1f - alpha));
        return Color.rgb(r, g, b);
    }

    static int compositeOnBlack(int argb) {
        int a = Color.alpha(argb);
        if (a == 0) {
            return Color.BLACK;
        }
        float alpha = a / 255f;
        int r = Math.round(Color.red(argb) * alpha);
        int g = Math.round(Color.green(argb) * alpha);
        int b = Math.round(Color.blue(argb) * alpha);
        return Color.rgb(r, g, b);
    }

    @Nullable
    private static Drawable loadActivityIcon(Context context, @Nullable ComponentName cn) {
        if (context == null || cn == null) {
            return null;
        }
        try {
            return context.getPackageManager().getActivityIcon(cn);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    /** Oppo {@code compressBeforeGetColor(Drawable)} — rasterize then downscale to 60px. */
    private static Bitmap compressBeforeGetColor(Drawable drawable) {
        if (drawable == null) {
            return Bitmap.createBitmap(60, 60, Bitmap.Config.ARGB_8888);
        }
        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();
        if (w <= 0 || h <= 0) {
            w = 1;
            h = 1;
        }
        Rect bounds = drawable.getBounds();
        Bitmap full = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(full);
        drawable.setBounds(0, 0, w, h);
        drawable.draw(canvas);
        drawable.setBounds(bounds);
        return compressBeforeGetColor(full);
    }

    /** Oppo {@code compressBeforeGetColor(Bitmap)}. */
    private static Bitmap compressBeforeGetColor(Bitmap bitmap) {
        if (bitmap == null) {
            return Bitmap.createBitmap(60, 60, Bitmap.Config.ARGB_8888);
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width <= 0 || height <= 0) {
            return Bitmap.createBitmap(60, 60, Bitmap.Config.ARGB_8888);
        }
        if (width <= 60 && height <= 60) {
            return bitmap;
        }
        float aspect = (float) width / height;
        int outW;
        int outH;
        if (width > height) {
            outW = 60;
            outH = Math.max(1, Math.round(60 / aspect));
        } else {
            outH = 60;
            outW = Math.max(1, Math.round(60 * aspect));
        }
        return Bitmap.createScaledBitmap(bitmap, outW, outH, true);
    }

    /** Port of Oppo {@code FastColorAnalyzer.getDominantColor}. */
    static int getDominantColor(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) {
            return Color.BLACK;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        java.util.HashMap<Integer, ColorBucket> buckets = new java.util.HashMap<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = pixels[y * width + x];
                if (((pixel >> 24) & 0xFF) == 0) {
                    continue;
                }
                if (isEdgePixel(pixels, x, y, width, height)) {
                    continue;
                }
                int rgb = pixel | 0xFF000000;
                int key = quantizeColor(rgb);
                ColorBucket bucket = buckets.get(key);
                if (bucket == null) {
                    bucket = new ColorBucket();
                    buckets.put(key, bucket);
                }
                bucket.add(rgb);
            }
        }
        ColorBucket best = null;
        float bestScore = 0f;
        for (ColorBucket bucket : buckets.values()) {
            float score = bucket.score();
            if (score > bestScore) {
                bestScore = score;
                best = bucket;
            }
        }
        if (best == null || best.count <= 0) {
            return Color.BLACK;
        }
        return best.average();
    }

    private static int quantizeColor(int rgb) {
        return Color.rgb((Color.red(rgb) / 64) * 64, (Color.green(rgb) / 64) * 64,
                (Color.blue(rgb) / 64) * 64);
    }

    private static boolean isEdgePixel(int[] pixels, int x, int y, int width, int height) {
        int center = pixels[y * width + x];
        int similar = 0;
        int total = 0;
        for (int dy = -3; dy <= 3; dy++) {
            for (int dx = -3; dx <= 3; dx++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                int nx = x + dx;
                int ny = y + dy;
                if (nx < 0 || nx >= width || ny < 0 || ny >= height) {
                    continue;
                }
                int neighbor = pixels[ny * width + nx];
                if (((neighbor >> 24) & 0xFF) == 0) {
                    continue;
                }
                total++;
                if (isColorSimilar(center, neighbor)) {
                    similar++;
                }
            }
        }
        return total != 0 && ((float) similar / total) < 0.85f;
    }

    private static boolean isColorSimilar(int a, int b) {
        int dr = ((a >> 16) & 0xFF) - ((b >> 16) & 0xFF);
        int dg = ((a >> 8) & 0xFF) - ((b >> 8) & 0xFF);
        int db = (a & 0xFF) - (b & 0xFF);
        return (dr * dr + dg * dg + db * db) < 1936;
    }

    private static final class ColorBucket {
        long rSum;
        long gSum;
        long bSum;
        int count;
        float satSum;

        void add(int rgb) {
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            rSum += r;
            gSum += g;
            bSum += b;
            int max = Math.max(r, Math.max(g, b));
            int min = Math.min(r, Math.min(g, b));
            if (max > 0) {
                satSum += (max - min) / (float) max;
            }
            count++;
        }

        float score() {
            if (count == 0) {
                return 0f;
            }
            float sat = satSum / count;
            return Math.min(sat * 0.15f + 1f, 1f) * (float) Math.log(count + 1);
        }

        int average() {
            if (count == 0) {
                return Color.BLACK;
            }
            return Color.rgb((int) (rSum / count), (int) (gSum / count), (int) (bSum / count));
        }
    }
}
