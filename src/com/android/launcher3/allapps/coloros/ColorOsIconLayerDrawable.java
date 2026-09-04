package com.android.launcher3.allapps.coloros;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

import androidx.annotation.NonNull;

/**
 * Oppo {@code OplusLayerDrawable}: LayerDrawable that does not redistribute bounds to
 * children on bounds change, so each FastBitmapDrawable keeps the icon size set by
 * BubbleTextView during the sort crossfade.
 */
final class ColorOsIconLayerDrawable extends LayerDrawable {

    ColorOsIconLayerDrawable(@NonNull Drawable[] layers) {
        super(layers);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        // Intentionally empty — match Oppo OplusLayerDrawable.
    }
}
