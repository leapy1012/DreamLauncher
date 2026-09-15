/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.popup;

import android.content.Context;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.R;

/**
 * ColorOS popup row icons:
 * <ul>
 *   <li>Deep shortcuts — 24dp circular plate + mono glyph</li>
 *   <li>System shortcuts (App info / Widgets / …) — flat mono glyph, no plate (Oppo)</li>
 * </ul>
 * Deep shortcuts load the raw {@link ShortcutInfo} drawable (usually adaptive). We rasterize it
 * and convert luminance→alpha so white-on-dark Material glyphs become a clean silhouette on a
 * muted ColorOS plate — without keeping the purple adaptive plate.
 */
public final class ColorOsPopupIcons {

    /** Inset of line glyph inside the deep-shortcut plate (Oppo line-art look). */
    private static final float DEEP_GLYPH_INSET = 0.22f;
    /** System vectors are already padded; keep almost full size. */
    private static final float SYSTEM_GLYPH_INSET = 0.06f;

    private ColorOsPopupIcons() {}

    public static final class Theme {
        @ColorInt public final int plate;
        @ColorInt public final int glyph;

        public Theme(@ColorInt int plate, @ColorInt int glyph) {
            this.plate = plate;
            this.glyph = glyph;
        }

        @NonNull
        public static Theme fromSurface(@ColorInt int popupSurface) {
            boolean dark = Color.luminance(popupSurface) < 0.5f;
            return new Theme(dark ? 0xFF4A4A4A : 0xFFEEF0F2,
                    dark ? Color.WHITE : Color.BLACK);
        }
    }

    @NonNull
    public static Drawable forDeepShortcut(@NonNull Context context, @NonNull ShortcutInfo detail,
            @NonNull Theme theme) {
        int sizePx = context.getResources().getDimensionPixelSize(R.dimen.coloros_popup_icon_size);
        Drawable raw = loadShortcutDrawable(context, detail);
        // Prefer monochrome / FG only — keep line art; do not dilate into a filled blob.
        Drawable glyph = extractGlyph(raw);
        boolean adaptiveFg = glyph != null && raw instanceof AdaptiveIconDrawable;
        return renderOnPlate(context, sizePx, theme, glyph != null ? glyph : raw,
                DEEP_GLYPH_INSET, /* expandAdaptiveFg= */ adaptiveFg);
    }

    @Nullable
    private static Drawable extractGlyph(@Nullable Drawable src) {
        if (src == null) {
            return null;
        }
        if (src instanceof AdaptiveIconDrawable) {
            AdaptiveIconDrawable aid = (AdaptiveIconDrawable) src;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                Drawable mono = aid.getMonochrome();
                if (mono != null) {
                    return mono.mutate();
                }
            }
            Drawable fg = aid.getForeground();
            return fg != null ? fg.mutate() : null;
        }
        return src.mutate();
    }

    /**
     * Oppo {@code SystemShortcut.updateIcon}: flat vector glyph, no grey circular plate.
     */
    @NonNull
    public static Drawable forSystemShortcut(@NonNull Context context, int iconResId,
            @NonNull Theme theme) {
        int sizePx = context.getResources().getDimensionPixelSize(
                R.dimen.coloros_popup_system_icon_size);
        Drawable src = ContextCompat.getDrawable(context, iconResId);
        return renderFlatGlyph(context, sizePx, theme, src, SYSTEM_GLYPH_INSET);
    }

    @Nullable
    private static Drawable loadShortcutDrawable(Context context, ShortcutInfo detail) {
        try {
            int dpi = LauncherAppState.getIDP(context).fillResIconDpi;
            return context.getSystemService(LauncherApps.class)
                    .getShortcutIconDrawable(detail, dpi);
        } catch (Exception ignored) {
            return null;
        }
    }

    @NonNull
    private static Drawable renderFlatGlyph(Context context, int sizePx, Theme theme,
            @Nullable Drawable src, float insetFraction) {
        Bitmap out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        if (src != null) {
            Drawable d = src.mutate();
            d.setTintList(null);
            d.setColorFilter(null);
            int inset = Math.round(sizePx * insetFraction);
            d.setBounds(inset, inset, sizePx - inset, sizePx - inset);

            Bitmap tmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
            Canvas tmpCanvas = new Canvas(tmp);
            d.draw(tmpCanvas);

            Bitmap mask = luminanceToAlphaMask(tmp);
            tmp.recycle();

            Paint mono = new Paint(Paint.ANTI_ALIAS_FLAG);
            mono.setFilterBitmap(false);
            mono.setColorFilter(new PorterDuffColorFilter(theme.glyph, PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(mask, 0, 0, mono);
            mask.recycle();
        }
        BitmapDrawable bd = new BitmapDrawable(context.getResources(), out);
        bd.setBounds(0, 0, sizePx, sizePx);
        return bd;
    }

    @NonNull
    private static Drawable renderOnPlate(Context context, int sizePx, Theme theme,
            @Nullable Drawable src, float insetFraction, boolean expandAdaptiveFg) {
        Bitmap out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);

        Paint platePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        platePaint.setColor(theme.plate);
        float r = sizePx / 2f;
        canvas.drawCircle(r, r, r, platePaint);

        if (src != null) {
            Drawable d = src.mutate();
            d.setTintList(null);
            d.setColorFilter(null);

            // Undo adaptive safe-zone so stroked glyphs fill the plate, then apply Oppo inset.
            int expand = expandAdaptiveFg
                    ? Math.round(sizePx * AdaptiveIconDrawable.getExtraInsetFraction())
                    : 0;
            int inset = Math.round(sizePx * insetFraction);
            d.setBounds(-expand + inset, -expand + inset,
                    sizePx + expand - inset, sizePx + expand - inset);

            Bitmap tmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
            Canvas tmpCanvas = new Canvas(tmp);
            d.draw(tmpCanvas);

            Bitmap mask = luminanceToAlphaMask(tmp);
            tmp.recycle();
            // Keep thin line art — do not dilate adaptive FGs into filled blobs.

            Path clip = new Path();
            clip.addCircle(r, r, r, Path.Direction.CW);
            canvas.save();
            canvas.clipPath(clip);
            Paint mono = new Paint(Paint.ANTI_ALIAS_FLAG);
            mono.setFilterBitmap(true);
            mono.setColorFilter(new PorterDuffColorFilter(theme.glyph, PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(mask, 0, 0, mono);
            canvas.restore();
            mask.recycle();
        }

        BitmapDrawable bd = new BitmapDrawable(context.getResources(), out);
        bd.setBounds(0, 0, sizePx, sizePx);
        return bd;
    }

    /**
     * Builds an ARGB mask whose alpha follows glyph ink: light-on-dark icons use luminance as
     * alpha; dark-on-light icons invert. Soft AA is preserved for a line-icon look.
     */
    @NonNull
    private static Bitmap luminanceToAlphaMask(@NonNull Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        int[] pixels = new int[w * h];
        src.getPixels(pixels, 0, w, 0, 0, w, h);

        long lumSum = 0;
        int opaque = 0;
        for (int c : pixels) {
            int a = (c >>> 24) & 0xff;
            if (a < 16) {
                continue;
            }
            int r = (c >> 16) & 0xff;
            int g = (c >> 8) & 0xff;
            int b = c & 0xff;
            lumSum += Math.max(r, Math.max(g, b));
            opaque++;
        }
        boolean lightInkOnDark = opaque > 0 && (lumSum / opaque) > 140;

        for (int i = 0; i < pixels.length; i++) {
            int c = pixels[i];
            int a = (c >>> 24) & 0xff;
            if (a < 8) {
                pixels[i] = 0;
                continue;
            }
            int r = (c >> 16) & 0xff;
            int g = (c >> 8) & 0xff;
            int b = c & 0xff;
            int lum = Math.max(r, Math.max(g, b));
            int ink = lightInkOnDark ? lum : (255 - lum);
            // Preserve AA so stroked/line glyphs stay thin (Oppo), not filled.
            int alpha = Math.min(255, (ink * a) / 255);
            if (alpha < 24) {
                pixels[i] = 0;
            } else {
                pixels[i] = (alpha << 24) | 0x00FFFFFF;
            }
        }

        Bitmap mask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mask.setPixels(pixels, 0, w, 0, 0, w, h);
        return mask;
    }
}
