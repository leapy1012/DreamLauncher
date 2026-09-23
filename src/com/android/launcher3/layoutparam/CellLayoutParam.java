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

import static com.android.launcher3.DeviceProfile.calculateCellWidth;
import static com.android.launcher3.testing.shared.ResourceUtils.pxFromDp;

import android.content.res.Resources;
import android.graphics.Point;
import android.util.DisplayMetrics;

import androidx.annotation.Nullable;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.R;

/**
 * Oppo-style {@code com.android.launcher.layoutparam.CellLayoutParam}: cell size, content height,
 * icon top factor, and ToggleBar layout-preview cell geometry.
 */
public final class CellLayoutParam {

    public static final float ICON_TOP_FACTOR_HALF = 0.5f;
    public static final float ICON_TOP_FACTOR_THREE_OVER_FIVE = 0.6f;

    private final DeviceProfile mDp;
    private final IconParam mIcon;

    public CellLayoutParam(DeviceProfile dp) {
        mDp = dp;
        mIcon = new IconParam(dp);
    }

    public IconParam icon() {
        return mIcon;
    }

    /**
     * Oppo IconUtils.getIconFactor: 0.5 for 4-col, 0.6 for 5-col.
     */
    public float getIconTopFactor() {
        return mDp.inv.numColumns == 5
                ? ICON_TOP_FACTOR_THREE_OVER_FIVE
                : ICON_TOP_FACTOR_HALF;
    }

    /**
     * Live workspace content height (icon + drawable pad + one text line).
     * When layout preview is active, uses preview icon/pad and two text lines so
     * {@code BubbleTextView} does not squeeze compound-drawable padding.
     */
    public int getContentHeight() {
        int icon = mDp.iconSizePx;
        int pad = mDp.iconDrawablePaddingPx;
        int textLines = 1;
        Point preview = mDp.layoutPreviewIconAndPaddingPx;
        if (preview != null) {
            icon = preview.x;
            pad = preview.y;
            textLines = 2;
        }
        return icon + pad + (mIcon.getIconTextHeightPx() * textLines);
    }

    /** Folder plate + label content height on the workspace. */
    public int getFolderWorkspaceContentHeight() {
        return mDp.folderIconSizePx + mDp.iconDrawablePaddingPx + mIcon.getIconTextHeightPx();
    }

    public int getWorkspaceCellHeight(int cellWidth) {
        return getWorkspaceCellHeightForGrid(
                cellWidth, mDp.inv.numColumns, mDp.inv.numRows);
    }

    private int getWorkspaceCellHeightForGrid(int cellWidth, int cols, int rows) {
        int iconSize = mDp.iconSizePx > 0
                ? mDp.iconSizePx
                : mIcon.getPreviewIconSizePx(cols);
        int contentHeight = iconSize + mDp.iconDrawablePaddingPx + mIcon.getIconTextHeightPx();
        int minVerticalPadding = 2 * mIcon.getWorkspaceIconPaddingTopMin(cols);
        return Math.max(contentHeight + minVerticalPadding,
                cellWidth + getDiffCellHeightWithCellWidth(cols, rows));
    }

    /**
     * Oppo {@code CellLayoutParam.getCellSize(cols, rows)} for ToggleBar layout preview.
     */
    public Point getPreviewCellSize(int cols, int rows) {
        Point result = new Point();
        int padHor = getPreviewPaddingHor(cols);
        int workspaceWidth = (mDp.availableWidthPx - (2 * mDp.getOppoWorkspacePaddingLeftPx()))
                / mDp.getPanelCount();
        int contentWidth = workspaceWidth - (2 * padHor);
        result.x = calculateCellWidth(contentWidth, mDp.cellLayoutBorderSpacePx.x, cols);
        int iconSize = mIcon.getPreviewIconSizePx(cols);
        int textHeight = mIcon.getIconTextHeightPx() * 2;
        int drawablePad = mIcon.getPreviewDrawablePaddingPx();
        int contentHeight = iconSize + drawablePad + textHeight;
        int minVerticalPadding = 2 * mIcon.getWorkspaceIconPaddingTopMin(cols);
        result.y = Math.max(contentHeight + minVerticalPadding,
                result.x + getDiffCellHeightWithCellWidth(cols, rows));
        return result;
    }

    public int getPreviewPaddingHor(int cols) {
        Resources res = mDp.getResourcesForLayoutParam();
        DisplayMetrics metrics = mDp.getDisplayMetricsForLayoutParam();
        int padDp = cols >= 5
                ? res.getInteger(R.integer.oplusLayoutCellLayoutPaddingHor5colDp)
                : res.getInteger(R.integer.oplusLayoutCellLayoutPaddingHor4colDp);
        return pxFromDp(padDp, metrics);
    }

    public int getPreviewIconSizePx(int cols) {
        return mIcon.getPreviewIconSizePx(cols);
    }

    public int getPreviewDrawablePaddingPx() {
        return mIcon.getPreviewDrawablePaddingPx();
    }

    public int getDiffCellHeightWithCellWidth(int cols, int rows) {
        Resources res = mDp.getResourcesForLayoutParam();
        if (cols == 3 && rows == 5) {
            return res.getDimensionPixelSize(R.dimen.diffCellHeightWithCellWidth3x5);
        }
        if (cols == 3 && rows == 6) {
            return res.getDimensionPixelSize(R.dimen.diffCellHeightWithCellWidth3x6);
        }
        if (cols == 4 && rows == 5) {
            return res.getDimensionPixelSize(R.dimen.diffCellHeightWithCellWidth4x5);
        }
        if (cols == 5 && rows == 5) {
            return res.getDimensionPixelSize(R.dimen.diffCellHeightWithCellWidth5x5);
        }
        if (cols == 5 && rows == 6) {
            return res.getDimensionPixelSize(R.dimen.diffCellHeightWithCellWidth5x6);
        }
        if (cols == 5 && rows == 7) {
            return res.getDimensionPixelSize(R.dimen.diffCellHeightWithCellWidth);
        }
        if ((cols == 4 && rows == 7)
                || (cols == 5 && (rows == 8 || rows == 9))) {
            return 0;
        }
        return res.getDimensionPixelSize(R.dimen.diffCellHeightWithCellWidth);
    }

    /** Begin layout-preview overrides used by {@link #getContentHeight()}. */
    public void beginLayoutPreview(int iconSizePx, int drawablePaddingPx) {
        mDp.layoutPreviewIconAndPaddingPx = new Point(iconSizePx, drawablePaddingPx);
    }

    /** Clear layout-preview overrides. */
    public void endLayoutPreview() {
        mDp.layoutPreviewIconAndPaddingPx = null;
    }

    @Nullable
    public Point getLayoutPreviewIconAndPadding() {
        return mDp.layoutPreviewIconAndPaddingPx;
    }
}
