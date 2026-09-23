/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.launcher3.layoutparam;

import static com.android.launcher3.testing.shared.ResourceUtils.pxFromDp;

import android.graphics.Paint;
import android.util.DisplayMetrics;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.R;

/**
 * Oppo-style {@code com.android.launcher.layoutparam.IconParam}: icon size, drawable padding,
 * and label text metrics used by workspace measure and ToggleBar layout preview.
 *
 * <p>Dream keeps a thin facade on {@link DeviceProfile}; geometry lives here.
 */
public final class IconParam {

    /** Oppo IconParam: 4-col preview / default grid icon. */
    public static final float PREVIEW_ICON_DP_4COL = 56f;
    /** Oppo IconParam: 5-col denser grid icon. */
    public static final float PREVIEW_ICON_DP_5COL = 50f;
    /** Oppo IconParam.ICON_TEXT_SIZE_OFFSET_5_COLS (applied in DeviceProfile for live 5-col). */
    public static final int ICON_TEXT_SIZE_OFFSET_5_COLS = 3;

    private final DeviceProfile mDp;

    public IconParam(DeviceProfile dp) {
        mDp = dp;
    }

    public int getIconSizePx() {
        return mDp.iconSizePx;
    }

    public int getIconDrawablePaddingPx() {
        return mDp.iconDrawablePaddingPx;
    }

    public int getIconDrawablePaddingOriginalPx() {
        return mDp.iconDrawablePaddingOriginalPx;
    }

    public int getIconTextSizePx() {
        return mDp.iconTextSizePx;
    }

    /**
     * Oppo {@code calculateTextHeightIgnoreFontPadding}: Paint descent−ascent for one line.
     */
    public int getIconTextHeightPx() {
        return calculateTextHeightIgnoreFontPadding(mDp.iconTextSizePx);
    }

    public static int calculateTextHeightIgnoreFontPadding(float textSizePx) {
        Paint paint = new Paint();
        paint.setTextSize(textSizePx);
        Paint.FontMetrics fm = paint.getFontMetrics();
        return (int) Math.ceil(fm.descent - fm.ascent);
    }

    /**
     * Oppo ToggleBar preview icon size for an alternate column count
     * ({@code CellLayoutParam.getIconSizeTmp} simplified for Dream grids).
     */
    public int getPreviewIconSizePx(int cols) {
        DisplayMetrics metrics = mDp.getDisplayMetricsForLayoutParam();
        return pxFromDp(cols <= 4 ? PREVIEW_ICON_DP_4COL : PREVIEW_ICON_DP_5COL, metrics);
    }

    /**
     * Uncompressed drawable padding for layout preview (never the dense-grid compressed 0).
     */
    public int getPreviewDrawablePaddingPx() {
        DisplayMetrics metrics = mDp.getDisplayMetricsForLayoutParam();
        int minPad = pxFromDp(4f, metrics);
        return Math.max(mDp.iconDrawablePaddingOriginalPx, minPad);
    }

    public int getWorkspaceIconPaddingTopMin(int cols) {
        return cols == 5
                ? mDp.getResourcesForLayoutParam().getDimensionPixelSize(
                        R.dimen.cellPaddingTopMin5Cols)
                : mDp.getResourcesForLayoutParam().getDimensionPixelSize(
                        R.dimen.cellPaddingTopMin);
    }
}
