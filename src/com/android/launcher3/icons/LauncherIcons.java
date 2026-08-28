/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static android.graphics.drawable.AdaptiveIconDrawable.getExtraInsetFraction;

import static com.android.launcher3.config.FeatureFlags.ENABLE_FORCED_MONO_ICON;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.graphics.IconShape;
import com.android.launcher3.graphics.LauncherPreviewRenderer;
import com.android.launcher3.util.Themes;

/**
 * Wrapper class to provide access to {@link BaseIconFactory} and also to provide pool of this class
 * that are threadsafe.
 */
public class LauncherIcons extends BaseIconFactory implements AutoCloseable {

    private static final Object sPoolSync = new Object();
    private static LauncherIcons sPool;
    private static int sPoolId = 0;

    /**
     * Extra shrink for adaptive foreground glyphs only (Calendar grid, etc.).
     * Background plate always fills the mask edge-to-edge — no white under-paint.
     */
    private static final float ADAPTIVE_FG_OPTICAL_SCALE = 0.88f;

    /**
     * Return a new Message instance from the global pool. Allows us to
     * avoid allocating new objects in many cases.
     */
    public static LauncherIcons obtain(Context context) {
        if (context instanceof LauncherPreviewRenderer.PreviewContext) {
            return ((LauncherPreviewRenderer.PreviewContext) context).newLauncherIcons(context);
        }

        int poolId;
        synchronized (sPoolSync) {
            if (sPool != null) {
                LauncherIcons m = sPool;
                sPool = m.next;
                m.next = null;
                return m;
            }
            poolId = sPoolId;
        }

        InvariantDeviceProfile idp = InvariantDeviceProfile.INSTANCE.get(context);
        return new LauncherIcons(context, idp.fillResIconDpi, idp.iconBitmapSize, poolId);
    }

    public static void clearPool() {
        synchronized (sPoolSync) {
            sPool = null;
            sPoolId++;
        }
    }

    private final int mPoolId;

    private LauncherIcons next;

    private MonochromeIconFactory mMonochromeIconFactory;

    protected LauncherIcons(Context context, int fillResIconDpi, int iconBitmapSize, int poolId) {
        super(context, fillResIconDpi, iconBitmapSize, IconShape.getShape().enableShapeDetection());
        mMonoIconEnabled = Themes.isThemedIconEnabled(context);
        mPoolId = poolId;
    }

    /**
     * ColorOS leaves draw scale at 1.0 and remasks via UX ({@code convertToThemeStyle}).
     * We remask every icon with a uniform optical inset so full-bleed art matches padded
     * icons, while the shape mask still fills {@code iconSizePx} (outScale 1.0).
     */
    @Override
    @Nullable
    protected Drawable normalizeAndWrapToAdaptiveIcon(@Nullable Drawable icon,
            boolean shrinkNonAdaptiveIcons, @Nullable android.graphics.RectF outIconBounds,
            @NonNull float[] outScale, int wrapperBackgroundColor) {
        if (icon == null) {
            return null;
        }
        if (!shrinkNonAdaptiveIcons) {
            outScale[0] = getNormalizer().getScale(icon, outIconBounds, null, null);
            return icon;
        }
        if (icon instanceof AdaptiveIconDrawable
                && ((AdaptiveIconDrawable) icon).getForeground() instanceof FixedScaleDrawable) {
            outScale[0] = 1f;
            return icon;
        }
        Drawable remasked = remaskIcon(icon);
        outScale[0] = 1f;
        if (outIconBounds != null) {
            outIconBounds.set(0, 0, 0, 0);
        }
        return remasked;
    }

    /**
     * Oppo-style remask: background plate fills the squircle; only the foreground
     * glyph is optically inset. No synthetic white plate — legacy icons draw full-bleed.
     */
    @NonNull
    private Drawable remaskIcon(@NonNull Drawable icon) {
        int size = mIconBitmapSize;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        int layerInset = Math.round(size * getExtraInsetFraction()
                / (1f + 2f * getExtraInsetFraction()));
        int fgExtraInset = Math.round(size * (1f - ADAPTIVE_FG_OPTICAL_SCALE) / 2f);

        if (icon instanceof AdaptiveIconDrawable) {
            AdaptiveIconDrawable aid = (AdaptiveIconDrawable) icon;
            Drawable bg = aid.getBackground();
            Drawable fg = aid.getForeground();
            if (bg != null) {
                bg = bg.mutate();
                bg.setBounds(-layerInset, -layerInset, size + layerInset, size + layerInset);
                bg.draw(canvas);
            }
            if (fg != null) {
                fg = fg.mutate();
                fg.setBounds(
                        -layerInset + fgExtraInset,
                        -layerInset + fgExtraInset,
                        size + layerInset - fgExtraInset,
                        size + layerInset - fgExtraInset);
                fg.draw(canvas);
            }
        } else {
            Rect oldBounds = new Rect(icon.getBounds());
            icon.setBounds(0, 0, size, size);
            icon.draw(canvas);
            icon.setBounds(oldBounds);
        }

        return applyShapeMaskAndShadow(bitmap);
    }

    @NonNull
    private Drawable applyShapeMaskAndShadow(@NonNull Bitmap bitmap) {
        int size = bitmap.getWidth();
        Canvas canvas = new Canvas(bitmap);

        AdaptiveIconDrawable shapeProbe =
                new AdaptiveIconDrawable(new ColorDrawable(Color.WHITE), null);
        shapeProbe.setBounds(0, 0, size, size);
        Path maskPath = new Path(shapeProbe.getIconMask());

        Bitmap maskBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas maskCanvas = new Canvas(maskBitmap);
        Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        maskPaint.setColor(Color.WHITE);
        maskCanvas.drawPath(maskPath, maskPaint);

        Paint xfer = new Paint(Paint.ANTI_ALIAS_FLAG);
        xfer.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        canvas.drawBitmap(maskBitmap, 0, 0, xfer);

        Bitmap withShadow = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas shadowCanvas = new Canvas(withShadow);
        getShadowGenerator().drawShadow(bitmap, shadowCanvas);
        shadowCanvas.drawBitmap(bitmap, 0, 0, null);

        return new BitmapDrawable(mContext.getResources(), withShadow);
    }

    /**
     * Recycles a LauncherIcons that may be in-use.
     */
    public void recycle() {
        synchronized (sPoolSync) {
            if (sPoolId != mPoolId) {
                return;
            }
            clear();

            next = sPool;
            sPool = this;
        }
    }

    @Override
    protected Drawable getMonochromeDrawable(Drawable base) {
        Drawable mono = super.getMonochromeDrawable(base);
        if (mono != null || !ENABLE_FORCED_MONO_ICON.get()) {
            return mono;
        }
        if (mMonochromeIconFactory == null) {
            mMonochromeIconFactory = new MonochromeIconFactory(mIconBitmapSize);
        }
        return mMonochromeIconFactory.wrap(base);
    }

    @Override
    public void close() {
        recycle();
    }
}
