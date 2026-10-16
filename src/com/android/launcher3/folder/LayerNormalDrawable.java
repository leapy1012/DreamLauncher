package com.android.launcher3.folder;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;

import androidx.annotation.NonNull;

/**
 * Folder plate that stacks the theme's folder image over the default plate. The theme layer
 * is shown; {@link #setThemeAlpha} cross-fades to the default one.
 */
public class LayerNormalDrawable extends LayerDrawable {
    private static final int MAX_ALPHA = 255;

    private final Drawable mThemeDrawable;
    private final Drawable mDefaultThemeDrawable;
    private int mThemeAlpha = MAX_ALPHA;

    public LayerNormalDrawable(@NonNull Drawable defaultThemeDrawable,
            @NonNull Drawable themeDrawable) {
        super(new Drawable[]{themeDrawable, defaultThemeDrawable});
        mThemeDrawable = themeDrawable;
        mDefaultThemeDrawable = defaultThemeDrawable;
        setThemeAlpha(MAX_ALPHA);
    }

    public Drawable getThemeDrawable() {
        return mThemeDrawable;
    }

    public Drawable getDefaultThemeDrawable() {
        return mDefaultThemeDrawable;
    }

    public int getThemeAlpha() {
        return mThemeAlpha;
    }

    /** 255 shows only the theme image, 0 only the default plate. */
    public void setThemeAlpha(int alpha) {
        mThemeAlpha = alpha;
        mThemeDrawable.setAlpha(alpha);
        mDefaultThemeDrawable.setAlpha(MAX_ALPHA - alpha);
        invalidateSelf();
    }

    public void showDefaultDrawable() {
        setThemeAlpha(0);
    }

    @Override
    public void setAlpha(int alpha) {
        if (mThemeAlpha == MAX_ALPHA) {
            mThemeDrawable.setAlpha(alpha);
        } else if (mThemeAlpha == 0) {
            mDefaultThemeDrawable.setAlpha(alpha);
        } else {
            super.setAlpha(alpha);
        }
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);
        mThemeDrawable.setBounds(left, top, right, bottom);
        mDefaultThemeDrawable.setBounds(left, top, right, bottom);
    }

    public void setRadius(float radius) {
        if (mThemeDrawable instanceof GradientDrawable) {
            ((GradientDrawable) mThemeDrawable).setCornerRadius(radius);
        }
        if (mDefaultThemeDrawable instanceof GradientDrawable) {
            ((GradientDrawable) mDefaultThemeDrawable).setCornerRadius(radius);
        }
    }
}
