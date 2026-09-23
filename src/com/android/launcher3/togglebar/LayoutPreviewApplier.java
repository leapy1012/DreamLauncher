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

package com.android.launcher3.togglebar;

import android.graphics.Point;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.ShortcutAndWidgetContainer;
import com.android.launcher3.Workspace;
import com.android.launcher3.celllayout.CellLayoutLayoutParams;
import com.android.launcher3.layoutparam.CellLayoutParam;
import com.android.launcher3.util.GridOccupancy;
import com.android.launcher3.widget.LauncherAppWidgetHostView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Oppo {@code LayoutSettingsHelper.PreviewGridChangedTask}: snapshot workspace icons, then
 * live-preview an alternate grid (cell size, padding, icon size, icon↔label gap).
 *
 * <p>Owned by {@link ColorOsLayoutOverlay}; keeps sheet UI separate from preview apply.
 */
public final class LayoutPreviewApplier {

    private final Launcher mLauncher;
    private final List<SavedIcon> mSavedIcons = new ArrayList<>();
    private final List<SavedPage> mSavedPages = new ArrayList<>();
    private int mOriginalCols;
    private int mOriginalRows;

    public LayoutPreviewApplier(Launcher launcher) {
        mLauncher = launcher;
    }

    public void snapshot(int originalCols, int originalRows) {
        mOriginalCols = originalCols;
        mOriginalRows = originalRows;
        mSavedIcons.clear();
        mSavedPages.clear();
        Workspace workspace = mLauncher.getWorkspace();
        if (workspace == null) {
            return;
        }
        for (int page = 0; page < workspace.getPageCount(); page++) {
            View pageView = workspace.getPageAt(page);
            if (!(pageView instanceof CellLayout cell)) {
                continue;
            }
            ViewGroup.LayoutParams lp = cell.getLayoutParams();
            mSavedPages.add(new SavedPage(cell, cell.getPaddingLeft(), cell.getPaddingTop(),
                    cell.getPaddingRight(), cell.getPaddingBottom(),
                    lp != null ? lp.height : ViewGroup.LayoutParams.MATCH_PARENT,
                    cell.getTranslationY()));
            ShortcutAndWidgetContainer container = cell.getShortcutsAndWidgets();
            if (container == null) {
                continue;
            }
            for (int i = 0; i < container.getChildCount(); i++) {
                View child = container.getChildAt(i);
                if (!(child.getLayoutParams() instanceof CellLayoutLayoutParams childLp)) {
                    continue;
                }
                boolean widget = child instanceof LauncherAppWidgetHostView;
                int iconSize = 0;
                int drawablePadding = 0;
                if (child instanceof BubbleTextView btv) {
                    iconSize = btv.getIconSize();
                    drawablePadding = btv.getCompoundDrawablePadding();
                }
                mSavedIcons.add(new SavedIcon(child, page, childLp.getCellX(), childLp.getCellY(),
                        childLp.cellHSpan, childLp.cellVSpan, widget, iconSize, drawablePadding));
            }
        }
        mSavedIcons.sort(Comparator.comparingInt((SavedIcon s) -> s.page)
                .thenComparingInt(s -> s.cellY)
                .thenComparingInt(s -> s.cellX));
    }

    /**
     * Oppo {@code PreviewGridChangedTask.changeLayout}.
     */
    public void applyPreviewGrid(int cols, int rows) {
        Workspace workspace = mLauncher.getWorkspace();
        if (workspace == null) {
            return;
        }
        boolean restoreOriginal = cols == mOriginalCols && rows == mOriginalRows;
        DeviceProfile dp = mLauncher.getDeviceProfile();
        CellLayoutParam cellParam = dp.cellLayout();
        Point cellSize = cellParam.getPreviewCellSize(cols, rows);
        int iconSize = cellParam.getPreviewIconSizePx(cols);
        int iconDrawablePadding = cellParam.getPreviewDrawablePaddingPx();
        cellParam.beginLayoutPreview(
                restoreOriginal ? Math.max(iconSize, dp.iconSizePx) : iconSize,
                iconDrawablePadding);
        for (int page = 0; page < workspace.getPageCount(); page++) {
            View pageView = workspace.getPageAt(page);
            if (!(pageView instanceof CellLayout cell)) {
                continue;
            }
            if (restoreOriginal) {
                restorePageChrome(cell);
                cell.setGridSize(cols, rows);
                cell.setCellDimensions(Math.max(1, cellSize.x), Math.max(1, cellSize.y));
                restorePageIcons(page, iconDrawablePadding);
            } else {
                applyCellLayoutPadding(dp, cell, workspace, cellSize, cols, rows);
                cell.setGridSize(cols, rows);
                cell.setCellDimensions(Math.max(1, cellSize.x), Math.max(1, cellSize.y));
                placePageIcons(page, cols, rows, iconSize, iconDrawablePadding);
            }
            cell.requestLayout();
        }
        workspace.requestLayout();
        workspace.invalidate();
    }

    public void clearPreviewOverrides() {
        mLauncher.getDeviceProfile().cellLayout().endLayoutPreview();
    }

    private void applyCellLayoutPadding(DeviceProfile dp, CellLayout cell,
            Workspace workspace, Point cellSize, int cols, int rows) {
        SavedPage saved = findSavedPage(cell);
        int padTop = saved != null ? saved.padT : cell.getPaddingTop();
        int padBottom = saved != null ? saved.padB : cell.getPaddingBottom();
        int padHor = Math.max(0, ((dp.widthPx - (cellSize.x * cols)) / 2)
                - workspace.getPaddingLeft());
        int height = padTop + padBottom + (cellSize.y * rows);
        float translationY = -((dp.availableHeightPx - height
                - workspace.getPaddingTop() - workspace.getPaddingBottom()) / 2f);
        ViewGroup.LayoutParams lp = cell.getLayoutParams();
        if (lp != null) {
            lp.height = height;
            cell.setLayoutParams(lp);
        }
        cell.setTranslationY(translationY);
        cell.setPadding(padHor, padTop, padHor, padBottom);
    }

    @Nullable
    private SavedPage findSavedPage(CellLayout cell) {
        for (SavedPage saved : mSavedPages) {
            if (saved.cell == cell) {
                return saved;
            }
        }
        return null;
    }

    private void restorePageChrome(CellLayout cell) {
        SavedPage saved = findSavedPage(cell);
        if (saved == null) {
            cell.setTranslationY(0f);
            return;
        }
        cell.setPadding(saved.padL, saved.padT, saved.padR, saved.padB);
        ViewGroup.LayoutParams lp = cell.getLayoutParams();
        if (lp != null) {
            lp.height = saved.height;
            cell.setLayoutParams(lp);
        }
        cell.setTranslationY(saved.translationY);
    }

    private void restorePageIcons(int page, int iconDrawablePaddingPx) {
        for (SavedIcon saved : mSavedIcons) {
            if (saved.page != page) {
                continue;
            }
            if (saved.view.getLayoutParams() instanceof CellLayoutLayoutParams lp) {
                lp.setCellX(saved.cellX);
                lp.setCellY(saved.cellY);
                lp.cellHSpan = saved.spanX;
                lp.cellVSpan = saved.spanY;
                lp.useTmpCoords = false;
            }
            saved.view.setScaleX(1f);
            saved.view.setScaleY(1f);
            if (saved.widget) {
                saved.view.setVisibility(View.INVISIBLE);
            } else {
                saved.view.setVisibility(View.VISIBLE);
                applyPreviewIconSize(saved.view, saved.iconSizePx, iconDrawablePaddingPx);
            }
        }
    }

    private void placePageIcons(int page, int cols, int rows, int iconSizePx,
            int iconDrawablePaddingPx) {
        GridOccupancy occupancy = new GridOccupancy(cols, rows);
        int[] vacant = new int[2];
        for (SavedIcon saved : mSavedIcons) {
            if (saved.page != page || !saved.widget) {
                continue;
            }
            markOrHide(saved, occupancy, vacant, cols, rows);
        }
        for (SavedIcon saved : mSavedIcons) {
            if (saved.page != page || saved.widget) {
                continue;
            }
            int spanX = Math.max(1, Math.min(saved.spanX, cols));
            int spanY = Math.max(1, Math.min(saved.spanY, rows));
            if (!occupancy.findVacantCell(vacant, spanX, spanY)) {
                saved.view.setVisibility(View.INVISIBLE);
                continue;
            }
            int destX = vacant[0];
            int destY = vacant[1];
            occupancy.markCells(destX, destY, spanX, spanY, true);
            if (saved.view.getLayoutParams() instanceof CellLayoutLayoutParams lp) {
                lp.setCellX(destX);
                lp.setCellY(destY);
                lp.cellHSpan = spanX;
                lp.cellVSpan = spanY;
                lp.useTmpCoords = false;
            }
            saved.view.setScaleX(1f);
            saved.view.setScaleY(1f);
            saved.view.setVisibility(View.VISIBLE);
            applyPreviewIconSize(saved.view, iconSizePx, iconDrawablePaddingPx);
        }
    }

    private static void markOrHide(SavedIcon saved, GridOccupancy occupancy, int[] vacant,
            int cols, int rows) {
        int spanX = Math.max(1, Math.min(saved.spanX, cols));
        int spanY = Math.max(1, Math.min(saved.spanY, rows));
        int destX;
        int destY;
        if (occupancy.isRegionVacant(saved.cellX, saved.cellY, spanX, spanY)) {
            destX = saved.cellX;
            destY = saved.cellY;
        } else if (occupancy.findVacantCell(vacant, spanX, spanY)) {
            destX = vacant[0];
            destY = vacant[1];
        } else {
            return;
        }
        occupancy.markCells(destX, destY, spanX, spanY, true);
        if (saved.view.getLayoutParams() instanceof CellLayoutLayoutParams lp) {
            lp.setCellX(destX);
            lp.setCellY(destY);
            lp.cellHSpan = spanX;
            lp.cellVSpan = spanY;
            lp.useTmpCoords = false;
        }
    }

    private static void applyPreviewIconSize(View view, int iconSizePx, int drawablePaddingPx) {
        if (!(view instanceof BubbleTextView icon)) {
            return;
        }
        int size = iconSizePx > 0 ? iconSizePx : icon.getIconSize();
        int minPad = Math.round(4f * view.getResources().getDisplayMetrics().density);
        int padding = Math.max(minPad, Math.max(0, drawablePaddingPx));
        icon.setIconSizeForLayoutPreview(size);
        icon.setCompoundDrawablePadding(padding);
        if (icon.getIcon() != null) {
            icon.setIcon(icon.getIcon());
            icon.setCompoundDrawablePadding(padding);
        }
    }

    private static final class SavedIcon {
        final View view;
        final int page;
        final int cellX;
        final int cellY;
        final int spanX;
        final int spanY;
        final boolean widget;
        final int iconSizePx;
        final int drawablePaddingPx;

        SavedIcon(View view, int page, int cellX, int cellY, int spanX, int spanY, boolean widget,
                int iconSizePx, int drawablePaddingPx) {
            this.view = view;
            this.page = page;
            this.cellX = cellX;
            this.cellY = cellY;
            this.spanX = spanX;
            this.spanY = spanY;
            this.widget = widget;
            this.iconSizePx = iconSizePx;
            this.drawablePaddingPx = drawablePaddingPx;
        }
    }

    private static final class SavedPage {
        final CellLayout cell;
        final int padL;
        final int padT;
        final int padR;
        final int padB;
        final int height;
        final float translationY;

        SavedPage(CellLayout cell, int padL, int padT, int padR, int padB,
                int height, float translationY) {
            this.cell = cell;
            this.padL = padL;
            this.padT = padT;
            this.padR = padR;
            this.padB = padB;
            this.height = height;
            this.translationY = translationY;
        }
    }
}
