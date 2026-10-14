/*
 * Copyright (C) 2026 GreenDream
 *
 * ColorOS "fancy" live clock from the applied /data/theme icon pack:
 * {@code fancy_icons/ic_launcher_clock/images/drawable-xxhdpi/clock_day_{bg,hour,minute}.png}
 * plus {@code manifest.xml} (rotation driven by DateTime.Minute).
 */
package com.android.launcher3.icons;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Theme-provided live clock layers. Cached per {@code /data/theme/icons} file state,
 * so repeated {@code ItemInfoWithIcon.newIcon} calls only stat the pack.
 */
public final class DataThemeFancyClock {
    private static final String TAG = "DataThemeFancyClock";
    private static final String CLOCK_DIR = "fancy_icons/ic_launcher_clock/";

    private static String sStateKey;
    private static DataThemeFancyClock sClock;
    private static boolean sHasDataThemeIcons;

    public final Bitmap background;
    public final Bitmap hour;
    public final Bitmap minute;
    @Nullable public final Bitmap second;

    private DataThemeFancyClock(Bitmap background, Bitmap hour, Bitmap minute,
            @Nullable Bitmap second) {
        this.background = background;
        this.hour = hour;
        this.minute = minute;
        this.second = second;
    }

    /** Live clock layers of the applied theme, or {@code null} when the pack has none. */
    @Nullable
    public static synchronized DataThemeFancyClock get() {
        refreshLocked();
        return sClock;
    }

    /** Whether a /data/theme icon pack is applied (static themed icons take precedence). */
    public static synchronized boolean hasDataThemeIcons() {
        refreshLocked();
        return sHasDataThemeIcons;
    }

    private static void refreshLocked() {
        File icons = new File(DataThemeIconLoader.ICONS_DIR);
        String key = icons.exists() ? icons.lastModified() + ":" + icons.length() : "none";
        if (key.equals(sStateKey)) {
            return;
        }
        sStateKey = key;
        sHasDataThemeIcons = icons.exists() && DataThemeIconLoader.hasDataThemeIcons();
        sClock = sHasDataThemeIcons ? load(icons) : null;
        Log.i(TAG, "state=" + key + " dataTheme=" + sHasDataThemeIcons
                + " fancyClock=" + (sClock != null));
    }

    @Nullable
    private static DataThemeFancyClock load(File icons) {
        Layers layers = new Layers();
        try {
            if (icons.isFile()) {
                try (ZipFile zip = new ZipFile(icons)) {
                    Enumeration<? extends ZipEntry> en = zip.entries();
                    while (en.hasMoreElements()) {
                        ZipEntry e = en.nextElement();
                        if (e.isDirectory() || !e.getName().startsWith(CLOCK_DIR)) continue;
                        try (InputStream in = zip.getInputStream(e)) {
                            layers.offer(e.getName(), in);
                        }
                    }
                }
            } else if (icons.isDirectory()) {
                File images = new File(icons, CLOCK_DIR + "images");
                File[] dirs = images.listFiles(File::isDirectory);
                if (dirs != null) {
                    for (File dir : dirs) {
                        File[] files = dir.listFiles(File::isFile);
                        if (files == null) continue;
                        for (File f : files) {
                            try (InputStream in = new FileInputStream(f)) {
                                layers.offer(dir.getName() + "/" + f.getName(), in);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "load failed: " + e.getMessage());
            return null;
        }
        if (layers.background == null || layers.hour == null || layers.minute == null) {
            return null;
        }
        return new DataThemeFancyClock(layers.background, layers.hour, layers.minute,
                layers.second);
    }

    /** Drawable of the clock face at {@code time}; intrinsic size is the background size. */
    public Drawable newFaceDrawable(Calendar time) {
        return new FaceDrawable(this, time);
    }

    /** Collects day-variant layers, preferring xxhdpi over other densities. */
    private static final class Layers {
        Bitmap background;
        Bitmap hour;
        Bitmap minute;
        Bitmap second;
        int bgRank = Integer.MAX_VALUE;
        int hourRank = Integer.MAX_VALUE;
        int minuteRank = Integer.MAX_VALUE;
        int secondRank = Integer.MAX_VALUE;

        void offer(String path, InputStream in) {
            String lower = path.toLowerCase(Locale.US);
            if (!lower.endsWith(".png") && !lower.endsWith(".webp")) return;
            String name = lower.substring(lower.lastIndexOf('/') + 1);
            if (name.contains("night")) return;
            int rank = (lower.contains("xxhdpi") ? 0 : 1) + (name.contains("day") ? 0 : 2);
            if (name.endsWith("_bg.png") || name.endsWith("_bg.webp")) {
                if (rank < bgRank && (background = decode(in, background)) != null) bgRank = rank;
            } else if (name.contains("hour")) {
                if (rank < hourRank && (hour = decode(in, hour)) != null) hourRank = rank;
            } else if (name.contains("minute")) {
                if (rank < minuteRank && (minute = decode(in, minute)) != null) minuteRank = rank;
            } else if (name.contains("second")) {
                if (rank < secondRank && (second = decode(in, second)) != null) secondRank = rank;
            }
        }

        private static Bitmap decode(InputStream in, Bitmap previous) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap b = BitmapFactory.decodeStream(in, null, opts);
            return b != null ? b : previous;
        }
    }

    /**
     * Background plus hands rotated about the face center, same as the pack manifest
     * ({@code #hour12*30+#minute/2}, {@code #minute*6}).
     */
    private static final class FaceDrawable extends Drawable {
        private final DataThemeFancyClock mClock;
        private final float mHourAngle;
        private final float mMinuteAngle;
        private final float mSecondAngle;
        private final Paint mPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);

        FaceDrawable(DataThemeFancyClock clock, Calendar time) {
            mClock = clock;
            int hour12 = time.get(Calendar.HOUR);
            int minute = time.get(Calendar.MINUTE);
            int second = time.get(Calendar.SECOND);
            mHourAngle = hour12 * 30f + minute / 2f;
            mMinuteAngle = minute * 6f;
            mSecondAngle = second * 6f;
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            Rect b = getBounds();
            canvas.drawBitmap(mClock.background, null, b, mPaint);
            drawHand(canvas, b, mClock.minute, mMinuteAngle);
            drawHand(canvas, b, mClock.hour, mHourAngle);
            if (mClock.second != null) {
                drawHand(canvas, b, mClock.second, mSecondAngle);
            }
        }

        private void drawHand(Canvas canvas, Rect b, Bitmap hand, float angle) {
            float scale = b.width() / (float) mClock.background.getWidth();
            float w = hand.getWidth() * scale;
            float h = hand.getHeight() * scale;
            float cx = b.exactCenterX();
            float cy = b.exactCenterY();
            int count = canvas.save();
            canvas.rotate(angle, cx, cy);
            canvas.drawBitmap(hand, null,
                    new Rect(Math.round(cx - w / 2f), Math.round(cy - h / 2f),
                            Math.round(cx + w / 2f), Math.round(cy + h / 2f)),
                    mPaint);
            canvas.restoreToCount(count);
        }

        @Override
        public int getIntrinsicWidth() {
            return mClock.background.getWidth();
        }

        @Override
        public int getIntrinsicHeight() {
            return mClock.background.getHeight();
        }

        @Override
        public void setAlpha(int alpha) {
            mPaint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            mPaint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
