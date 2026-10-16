/*
 * Copyright (C) 2026 GreenDream
 *
 * Faithful port of ColorOS third-party icon conversion:
 *   com.oplus.theme.OplusConvertIcon + OplusThirdPartUtil + OplusIconParam
 *
 * Plate / mask / fg come from /data/theme/com.android.launcher|com.oppo.launcher
 * (zip or expanded dir). No zzzz / AAA_ / solid-color fallbacks.
 */
package com.android.launcher3.icons;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * OPPO {@code OplusConvertIcon} third-party path for DreamLauncher.
 *
 * <p>IconBgType (from launcher pack contents):
 * <ul>
 *   <li>{@code iphone_style_bg.png} → COVER</li>
 *   <li>else {@code new_iphone_style_mask.png} → MASK (bg = {@code new_iphone_style_bg.png})</li>
 *   <li>else → SCALE (no plate; leave icon alone)</li>
 * </ul>
 *
 * <p>{@code themeInfo.xml} {@code Scale} / offsets size the glyph on the plate
 * (wood theme uses {@code Scale=0.70}).
 */
public final class OplusStyleIconConverter {
    private static final String TAG = "OplusStyleIconConverter";

    private static final String IPHONE_STYLE_BG = "iphone_style_bg.png";
    private static final String IPHONE_STYLE_FG = "iphone_style_fg.png";
    private static final String NEW_IPHONE_STYLE_BG = "new_iphone_style_bg.png";
    private static final String NEW_IPHONE_STYLE_MASK = "new_iphone_style_mask.png";
    private static final String THEME_INFO = "themeInfo.xml";

    /** {@code OplusConvertIcon.COVER_ICON_RADIO} when themeInfo Scale is missing. */
    private static final float COVER_ICON_RADIO = 0.62f;
    private static final float DEFAULT_DETECT_MASK_BORDER_OFFSET = 0.065f;
    private static final int NORMAL_ICON_SIZE = 192;
    private static final int PIXEL_ALPHA_THRESHOLD = 220;
    private static final int NO_TRANSPARENT_SCALE_PIXELS = 11;

    public enum IconBgType {
        MASK,
        COVER,
        SCALE
    }

    private static final Object LOCK = new Object();
    private static boolean sResolved;
    private static IconBgType sIconBgType = IconBgType.SCALE;
    private static boolean sNeedDrawForeground;
    private static float sThemeScale = 1f;
    private static float sXOffsetPct;
    private static float sYOffsetPct;
    private static float sDetectMaskBorderOffset = DEFAULT_DETECT_MASK_BORDER_OFFSET;
    private static Bitmap sPlateBg;
    private static Bitmap sPlateFg;
    private static Bitmap sMask;
    private static String sLauncherPackPath;
    private static long sPackStamp;

    private OplusStyleIconConverter() {}

    /** True when theme provides COVER/MASK plate assets (third-party icons should be wrapped). */
    public static boolean hasThirdPartyStyle() {
        ensureLoaded();
        return sIconBgType == IconBgType.COVER || sIconBgType == IconBgType.MASK;
    }

    public static void clearCache() {
        synchronized (LOCK) {
            recycleQuiet(sPlateBg);
            recycleQuiet(sPlateFg);
            recycleQuiet(sMask);
            sPlateBg = null;
            sPlateFg = null;
            sMask = null;
            sResolved = false;
            sIconBgType = IconBgType.SCALE;
            sNeedDrawForeground = false;
            sLauncherPackPath = null;
            sPackStamp = 0;
        }
    }

    public static String stateToken() {
        File pack = DreamThirdPartUtil.getLauncherName();
        if (pack == null) return "oplus-style-none";
        return "oplus-style-" + pack.lastModified() + "-" + pack.length();
    }

    /**
     * Compose third-party icon like {@code OplusConvertIcon.convertIconBitmap(..., true)}
     * → {@code BitmapDrawable} (same type ColorOS PackageManager returns).
     *
     * <p>Dream's icon factory then wraps this legacy bitmap the same way as mapped
     * theme PNGs under {@code /data/theme/icons}.
     */
    @Nullable
    public static Drawable wrapThirdPartyIcon(@Nullable Drawable original) {
        if (original == null) return null;
        ensureLoaded();
        if (sIconBgType == IconBgType.SCALE || sPlateBg == null) {
            return original;
        }
        synchronized (LOCK) {
            Bitmap src = rasterizeSource(original);
            if (src == null) return original;
            try {
                int iconSize = NORMAL_ICON_SIZE;
                int themeParamScale = Math.max(1, Math.round(iconSize * sThemeScale));
                int xOff = Math.round(iconSize * sXOffsetPct);
                int yOff = Math.round(iconSize * sYOffsetPct);

                Bitmap composed = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(composed);
                canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG
                        | Paint.FILTER_BITMAP_FLAG));

                if (sIconBgType == IconBgType.COVER) {
                    drawCover(canvas, src, iconSize, themeParamScale, xOff, yOff);
                } else {
                    drawMask(canvas, src, iconSize, themeParamScale, xOff, yOff);
                }
                if (sNeedDrawForeground && sPlateFg != null) {
                    drawForeground(canvas, iconSize);
                }
                return new BitmapDrawable(null, composed);
            } finally {
                src.recycle();
            }
        }
    }

    private static void drawCover(Canvas canvas, Bitmap src, int iconSize, int themeParamScale,
            int xOff, int yOff) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        if (sPlateBg != null) {
            canvas.drawBitmap(sPlateBg, null, new Rect(0, 0, iconSize, iconSize), paint);
        }
        Bitmap scaled = cutAndScaleBitmap(src, themeParamScale);
        if (scaled != null) {
            float left = ((iconSize - scaled.getWidth()) / 2f) + xOff;
            float top = ((iconSize - scaled.getHeight()) / 2f) + yOff;
            canvas.drawBitmap(scaled, left, top, paint);
            if (scaled != src) scaled.recycle();
        }
    }

    private static void drawMask(Canvas canvas, Bitmap src, int iconSize, int themeParamScale,
            int xOff, int yOff) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        // Background plate (wood / theme texture) — OplusConvertIcon.maskBitmap
        if (sPlateBg != null) {
            canvas.drawBitmap(sPlateBg, null, new Rect(0, 0, iconSize, iconSize), paint);
        }
        Bitmap masked = scaleAndMaskBitmap(src, iconSize, themeParamScale);
        if (masked == null) return;

        boolean hasTransparent = hasTransparentPixels(src);
        int addPixels = 0;
        int srcWidth = masked.getWidth();
        int targetWidth = srcWidth;
        if (!hasTransparent && srcWidth > NO_TRANSPARENT_SCALE_PIXELS * 2) {
            addPixels = NO_TRANSPARENT_SCALE_PIXELS;
            targetWidth = srcWidth - (addPixels * 2);
        }
        Bitmap thirdParty = masked;
        if (targetWidth > 0 && targetWidth != srcWidth) {
            float widthScale = (targetWidth * 1f) / srcWidth;
            Matrix matrix = new Matrix();
            matrix.postScale(widthScale, widthScale, 0f, 0f);
            thirdParty = Bitmap.createBitmap(targetWidth, targetWidth, Bitmap.Config.ARGB_8888);
            Canvas tmp = new Canvas(thirdParty);
            tmp.drawBitmap(masked, matrix, new Paint(Paint.FILTER_BITMAP_FLAG));
        }

        int w = masked.getWidth();
        int h = masked.getHeight();
        int xEndPosDiffer = ((w - iconSize) / 2) + xOff;
        if (xEndPosDiffer > -1) {
            canvas.drawBitmap(thirdParty,
                    ((iconSize - w) / 2f) + addPixels,
                    ((iconSize - h) / 2f) + addPixels,
                    paint);
        } else {
            canvas.drawBitmap(thirdParty,
                    ((iconSize - w) / 2f) + xOff + addPixels,
                    ((iconSize - h) / 2f) + yOff + addPixels,
                    paint);
        }
        if (thirdParty != masked) thirdParty.recycle();
        masked.recycle();
    }

    private static void drawForeground(Canvas canvas, int iconSize) {
        if (sPlateFg == null) return;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        boolean hasTransparent = hasTransparentPixels(sPlateFg);
        int addPixels = hasTransparent ? 0 : NO_TRANSPARENT_SCALE_PIXELS;
        canvas.drawBitmap(sPlateFg, null,
                new Rect(addPixels, addPixels, iconSize - addPixels, iconSize - addPixels),
                paint);
    }

    /**
     * Java stand-in for {@code OplusMaskBitmapUtilities.cutAndScaleBitmap}:
     * crop opaque content, scale so max side = themeParamScale.
     */
    @NonNull
    private static Bitmap cutAndScaleBitmap(@NonNull Bitmap src, int themeParamScale) {
        Rect bounds = contentBounds(src, 16);
        Bitmap content = src;
        boolean cropped = false;
        if (bounds.width() > 0 && bounds.height() > 0
                && (bounds.width() < src.getWidth() || bounds.height() < src.getHeight())) {
            content = Bitmap.createBitmap(src, bounds.left, bounds.top,
                    bounds.width(), bounds.height());
            cropped = true;
        }
        int maxSide = Math.max(content.getWidth(), content.getHeight());
        if (maxSide <= 0) {
            if (cropped) content.recycle();
            return src.copy(Bitmap.Config.ARGB_8888, false);
        }
        float scale = themeParamScale / (float) maxSide;
        int dw = Math.max(1, Math.round(content.getWidth() * scale));
        int dh = Math.max(1, Math.round(content.getHeight() * scale));
        Bitmap out = Bitmap.createScaledBitmap(content, dw, dh, true);
        if (cropped) content.recycle();
        return out;
    }

    /**
     * Java stand-in for {@code OplusMaskBitmapUtilities.scaleAndMaskBitmap}:
     * place cut+scaled glyph on iconSize canvas, apply theme mask (DST_IN).
     */
    @Nullable
    private static Bitmap scaleAndMaskBitmap(@NonNull Bitmap src, int iconSize,
            int themeParamScale) {
        Bitmap scaled = cutAndScaleBitmap(src, themeParamScale);
        Bitmap out = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        float left = (iconSize - scaled.getWidth()) / 2f;
        float top = (iconSize - scaled.getHeight()) / 2f;
        canvas.drawBitmap(scaled, left, top, paint);
        if (scaled != src) scaled.recycle();

        if (sMask != null) {
            // Full theme mask DST_IN — matches OplusMaskBitmapUtilities.scaleAndMaskBitmap usage.
            // DetectMaskBorderOffset is consumed by native cut detection, not mask draw inset.
            Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
            canvas.drawBitmap(sMask, null, new Rect(0, 0, iconSize, iconSize), maskPaint);
            maskPaint.setXfermode(null);
        }
        return out;
    }

    @NonNull
    private static Rect contentBounds(@NonNull Bitmap src, int alphaMin) {
        int w = src.getWidth();
        int h = src.getHeight();
        int minX = w;
        int minY = h;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (Color.alpha(src.getPixel(x, y)) < alphaMin) continue;
                if (x < minX) minX = x;
                if (y < minY) minY = y;
                if (x > maxX) maxX = x;
                if (y > maxY) maxY = y;
            }
        }
        if (maxX < minX) return new Rect(0, 0, w, h);
        return new Rect(minX, minY, maxX + 1, maxY + 1);
    }

    /** OPPO {@code hasTransparentPixels} — edge sampling. */
    private static boolean hasTransparentPixels(@Nullable Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) return false;
        try {
            int transparentCount = 0;
            int xStep = (int) Math.ceil((bitmap.getWidth() * 1f) / 4f);
            int yStep = (int) Math.ceil((bitmap.getHeight() * 1f) / 4f);
            int pixels;
            for (int i = xStep; i < bitmap.getWidth(); i += xStep) {
                pixels = 0;
                for (int j = 1; j < 4; j++) {
                    if (Color.alpha(bitmap.getPixel(i, j)) < PIXEL_ALPHA_THRESHOLD) pixels++;
                    if (j == 3 && pixels > 1) transparentCount++;
                }
            }
            for (int i = xStep; i < bitmap.getWidth(); i += xStep) {
                pixels = 0;
                for (int j = bitmap.getHeight() - 2; j > bitmap.getHeight() - 4; j--) {
                    if (j < 0) break;
                    if (Color.alpha(bitmap.getPixel(i, j)) < PIXEL_ALPHA_THRESHOLD) pixels++;
                    if (pixels > 1) transparentCount++;
                }
            }
            for (int i = yStep; i < bitmap.getHeight(); i += yStep) {
                pixels = 0;
                for (int j = 0; j < 3 && j < bitmap.getWidth(); j++) {
                    if (Color.alpha(bitmap.getPixel(j, i)) < PIXEL_ALPHA_THRESHOLD) pixels++;
                    if (j == 2 && pixels > 1) transparentCount++;
                }
            }
            for (int i = yStep; i < bitmap.getHeight(); i += yStep) {
                pixels = 0;
                for (int j = bitmap.getWidth() - 2; j > bitmap.getWidth() - 4; j--) {
                    if (j < 0) break;
                    if (Color.alpha(bitmap.getPixel(j, i)) < PIXEL_ALPHA_THRESHOLD) pixels++;
                    if (j == bitmap.getWidth() - 4 && pixels > 1) transparentCount++;
                }
            }
            return transparentCount >= NO_TRANSPARENT_SCALE_PIXELS * 2;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Rasterize source for convert. Adaptive icons → foreground only when it has ink
     * (matches ColorOS Google "G" on plate, not white adaptive disk).
     */
    @Nullable
    private static Bitmap rasterizeSource(@NonNull Drawable original) {
        Drawable src = original;
        if (original instanceof AdaptiveIconDrawable) {
            Drawable fg = ((AdaptiveIconDrawable) original).getForeground();
            if (fg != null) {
                Bitmap trial = drawToBitmap(fg, NORMAL_ICON_SIZE);
                if (trial != null && contentBounds(trial, 160).width() > 4) {
                    return trial;
                }
                if (trial != null) trial.recycle();
            }
        }
        return drawToBitmap(src, NORMAL_ICON_SIZE);
    }

    @Nullable
    private static Bitmap drawToBitmap(@NonNull Drawable d, int size) {
        Drawable draw = d.getConstantState() != null
                ? d.getConstantState().newDrawable().mutate()
                : d.mutate();
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        draw.setBounds(0, 0, size, size);
        draw.draw(canvas);
        return bmp;
    }

    private static void ensureLoaded() {
        File pack = DreamThirdPartUtil.getLauncherName();
        long stamp = pack == null ? 0 : pack.lastModified() + pack.length();
        synchronized (LOCK) {
            if (sResolved && stamp == sPackStamp
                    && (pack == null ? sLauncherPackPath == null
                    : pack.getAbsolutePath().equals(sLauncherPackPath))) {
                return;
            }
            recycleQuiet(sPlateBg);
            recycleQuiet(sPlateFg);
            recycleQuiet(sMask);
            sPlateBg = null;
            sPlateFg = null;
            sMask = null;
            sNeedDrawForeground = false;
            sThemeScale = 1f;
            sXOffsetPct = 0f;
            sYOffsetPct = 0f;
            sDetectMaskBorderOffset = DEFAULT_DETECT_MASK_BORDER_OFFSET;
            sIconBgType = IconBgType.SCALE;
            sLauncherPackPath = pack == null ? null : pack.getAbsolutePath();
            sPackStamp = stamp;
            sResolved = true;
            if (pack == null) {
                Log.i(TAG, "no launcher theme pack under " + DreamThirdPartUtil.THEME_ROOT);
                return;
            }
            loadFromPack(pack);
            Log.i(TAG, "loaded style from " + pack.getAbsolutePath()
                    + " type=" + sIconBgType
                    + " scale=" + sThemeScale
                    + " maskBorder=" + sDetectMaskBorderOffset
                    + " fg=" + sNeedDrawForeground
                    + " bg=" + (sPlateBg != null));
        }
    }

    private static void loadFromPack(@NonNull File pack) {
        // getIconBgType — OplusConvertIcon
        boolean hasCoverBg = DreamThirdPartUtil.assetExists(pack, IPHONE_STYLE_BG);
        boolean hasMask = DreamThirdPartUtil.assetExists(pack, NEW_IPHONE_STYLE_MASK);
        boolean hasFg = DreamThirdPartUtil.assetExists(pack, IPHONE_STYLE_FG);
        if (hasCoverBg) {
            sIconBgType = IconBgType.COVER;
            sNeedDrawForeground = hasFg;
        } else if (hasMask) {
            sIconBgType = IconBgType.MASK;
            sNeedDrawForeground = hasFg;
        } else {
            sIconBgType = IconBgType.SCALE;
            return;
        }

        parseThemeInfo(pack);

        if (sThemeScale <= 0f) {
            if (sIconBgType == IconBgType.COVER) {
                sThemeScale = COVER_ICON_RADIO;
            } else {
                sThemeScale = 1f;
            }
        }

        if (sIconBgType == IconBgType.COVER) {
            sPlateBg = DreamThirdPartUtil.decodeAsset(pack, IPHONE_STYLE_BG);
        } else {
            sPlateBg = DreamThirdPartUtil.decodeAsset(pack, NEW_IPHONE_STYLE_BG);
            sMask = DreamThirdPartUtil.decodeAsset(pack, NEW_IPHONE_STYLE_MASK);
            if (sMask != null && (sMask.getWidth() != NORMAL_ICON_SIZE
                    || sMask.getHeight() != NORMAL_ICON_SIZE)) {
                Bitmap scaled = Bitmap.createScaledBitmap(
                        sMask, NORMAL_ICON_SIZE, NORMAL_ICON_SIZE, true);
                if (scaled != sMask) {
                    sMask.recycle();
                    sMask = scaled;
                }
            }
        }
        if (sNeedDrawForeground) {
            sPlateFg = DreamThirdPartUtil.decodeAsset(pack, IPHONE_STYLE_FG);
        }
        if (sPlateBg == null) {
            // COVER/MASK without a plate bitmap cannot wrap third-party icons.
            sIconBgType = IconBgType.SCALE;
            return;
        }
        if (sPlateBg.getWidth() != NORMAL_ICON_SIZE
                || sPlateBg.getHeight() != NORMAL_ICON_SIZE) {
            Bitmap scaled = Bitmap.createScaledBitmap(
                    sPlateBg, NORMAL_ICON_SIZE, NORMAL_ICON_SIZE, true);
            if (scaled != sPlateBg) {
                sPlateBg.recycle();
                sPlateBg = scaled;
            }
        }
    }

    private static void parseThemeInfo(@NonNull File pack) {
        InputStream in = null;
        ZipFile zip = null;
        try {
            if (pack.isDirectory()) {
                File info = new File(pack, THEME_INFO);
                if (!info.isFile()) return;
                in = new BufferedInputStream(new FileInputStream(info));
            } else {
                zip = new ZipFile(pack);
                ZipEntry entry = zip.getEntry(THEME_INFO);
                if (entry == null) return;
                in = zip.getInputStream(entry);
            }
            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setInput(in, "UTF-8");
            String current = null;
            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
                if (type == XmlPullParser.START_TAG) {
                    current = parser.getName();
                } else if (type == XmlPullParser.TEXT && current != null) {
                    String v = parser.getText();
                    if (v == null) continue;
                    v = v.trim();
                    if (v.isEmpty()) continue;
                    try {
                        if ("Scale".equals(current)) {
                            sThemeScale = Float.parseFloat(v);
                        } else if ("XOffsetPCT".equals(current)) {
                            sXOffsetPct = Float.parseFloat(v);
                        } else if ("YOffsetPCT".equals(current)) {
                            sYOffsetPct = Float.parseFloat(v);
                        } else if ("DetectMaskBorderOffset".equals(current)) {
                            sDetectMaskBorderOffset = Float.parseFloat(v);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                } else if (type == XmlPullParser.END_TAG) {
                    current = null;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "parseThemeInfo: " + e.getMessage());
        } finally {
            try {
                if (in != null) in.close();
            } catch (Exception ignored) {
            }
            try {
                if (zip != null) zip.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static void recycleQuiet(@Nullable Bitmap b) {
        if (b != null && !b.isRecycled()) b.recycle();
    }
}
