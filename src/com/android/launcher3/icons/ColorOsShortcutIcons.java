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
package com.android.launcher3.icons;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.launcher3.R;

/**
 * Oppo-style shortcut theming (stand-in for {@code convertToThemeStyle} / UX shortcut morph):
 * circular muted plate + mono glyph. Applied in {@link ShortcutCachingLogic} so popup and
 * workspace shortcuts share the same bitmap.
 */
public final class ColorOsShortcutIcons {

    /**
     * Optical inset after undoing adaptive safe-zone. Oppo UX glyphs sit near ~10–12% of
     * diameter; 0.22 was too aggressive and looked “tiny in the plate”.
     */
    private static final float GLYPH_INSET = 0.10f;

    private ColorOsShortcutIcons() {}

    /**
     * Themes a raw {@link ShortcutInfo} drawable into a circular ColorOS shortcut bitmap.
     */
    @NonNull
    public static Bitmap theme(@NonNull Context context, @Nullable Drawable raw, int sizePx) {
        @ColorInt int plate = context.getColor(R.color.coloros_popup_icon_plate);
        // Oppo popup label / shortcut glyph: #E6000000 on light plates (not washed gray).
        @ColorInt int glyph = Color.luminance(plate) < 0.5f
                ? Color.WHITE
                : context.getColor(R.color.coloros_popup_label);

        Bitmap out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);

        Paint platePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        platePaint.setColor(plate);
        float r = sizePx / 2f;
        canvas.drawCircle(r, r, r, platePaint);

        if (raw == null) {
            return out;
        }

        GlyphExtract extracted = extractGlyph(raw);
        if (extracted.drawable == null) {
            return out;
        }

        Drawable d = extracted.drawable.mutate();
        d.setTintList(null);
        d.setColorFilter(null);

        // Only AdaptiveIconDrawable foreground needs ExtraInsetFraction undo; monochrome does not.
        int expand = extracted.expandAdaptiveFg
                ? Math.round(sizePx * AdaptiveIconDrawable.getExtraInsetFraction())
                : 0;
        int inset = Math.round(sizePx * GLYPH_INSET);
        d.setBounds(-expand + inset, -expand + inset,
                sizePx + expand - inset, sizePx + expand - inset);

        Bitmap tmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas tmpCanvas = new Canvas(tmp);
        d.draw(tmpCanvas);

        Bitmap mask = luminanceToAlphaMask(tmp);
        tmp.recycle();

        Path clip = new Path();
        clip.addCircle(r, r, r, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clip);
        Paint mono = new Paint(Paint.ANTI_ALIAS_FLAG);
        // Nearest-neighbor keeps solid ink opaque; bilinear was washing black → gray.
        mono.setFilterBitmap(false);
        mono.setColorFilter(new PorterDuffColorFilter(glyph, PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(mask, 0, 0, mono);
        canvas.restore();
        mask.recycle();

        return out;
    }

    private static final class GlyphExtract {
        @Nullable final Drawable drawable;
        final boolean expandAdaptiveFg;

        GlyphExtract(@Nullable Drawable drawable, boolean expandAdaptiveFg) {
            this.drawable = drawable;
            this.expandAdaptiveFg = expandAdaptiveFg;
        }
    }

    @NonNull
    private static GlyphExtract extractGlyph(@NonNull Drawable src) {
        if (src instanceof AdaptiveIconDrawable) {
            AdaptiveIconDrawable aid = (AdaptiveIconDrawable) src;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                Drawable mono = aid.getMonochrome();
                if (mono != null) {
                    return new GlyphExtract(mono.mutate(), /* expandAdaptiveFg= */ false);
                }
            }
            Drawable fg = aid.getForeground();
            return new GlyphExtract(fg != null ? fg.mutate() : null, /* expandAdaptiveFg= */ true);
        }
        return new GlyphExtract(src.mutate(), /* expandAdaptiveFg= */ false);
    }

    /**
     * Luminance→alpha mask. Solid ink is snapped opaque so SRC_IN paints Oppo-black
     * ({@code coloros_popup_label}), not mid-gray from soft alpha.
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
            int alpha = Math.min(255, (ink * a) / 255);
            if (alpha < 40) {
                pixels[i] = 0;
            } else if (alpha >= 120) {
                // Core of the glyph — full opacity → solid black after SRC_IN.
                pixels[i] = 0xFFFFFFFF;
            } else {
                // Narrow AA rim only.
                pixels[i] = (alpha << 24) | 0x00FFFFFF;
            }
        }

        Bitmap mask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mask.setPixels(pixels, 0, w, 0, 0, w, h);
        return mask;
    }
}
