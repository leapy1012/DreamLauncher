/*
 * Copyright (C) 2026 The Dream Project
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

package com.android.launcher3;

import static com.android.launcher3.icons.IconNormalizer.ICON_VISIBLE_AREA_FACTOR;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.android.launcher3.celllayout.CellLayoutLayoutParams;
import com.android.launcher3.folder.large.LargeFolderProxy;
import com.android.launcher3.util.CellAndSpan;
import com.android.launcher3.util.GridOccupancy;

import java.util.ArrayList;

/**
 * Workspace page {@link CellLayout}: Oppo workspace cell metrics, large-folder hit areas and
 * the auto-fill (row-major compact) reorder.
 */
public class DreamCellLayout extends CellLayout {

    public DreamCellLayout(Context context) {
        super(context);
    }

    public DreamCellLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DreamCellLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected int calculateCellHeight(int childHeightSize, int cellWidth) {
        DeviceProfile dp = mActivity.getDeviceProfile();
        if (dp.useOppoWorkspaceMetrics()) {
            return dp.getOppoWorkspaceCellHeight(cellWidth);
        }
        return super.calculateCellHeight(childHeightSize, cellWidth);
    }

    @Override
    protected void onCellDimensionsMeasured() {
        LargeFolderProxy.initFolderIconSize(getContext(), mActivity, getCellWidth(),
                getCellHeight(), getPaddingLeft(), getPaddingRight(), getPaddingTop(),
                getPaddingTop());
    }

    private boolean isLargeFolder(int[] targetCell) {
        return LargeFolderProxy.isLargeFolder(getChildAt(targetCell[0], targetCell[1]));
    }

    /**
     * Oppo: {@code (reorderRadius + iconSize*0.92/2) / 2} so the outer ring of an occupied
     * cell can still trigger push-reorder instead of always creating a folder.
     */
    @Override
    public float getFolderCreationRadius(int[] targetCell) {
        if (!isLargeFolder(targetCell)) {
            return super.getFolderCreationRadius(targetCell);
        }
        return (getReorderRadius(targetCell, 1, 1)
                + LargeFolderProxy.getMaxDistanceForFolderCreation()) / 2f;
    }

    @Override
    protected void getReorderCellBounds(int[] targetCell, int spanX, int spanY, Rect outBounds) {
        if (isLargeFolder(targetCell)) {
            cellToRect(targetCell[0], targetCell[1], 2, 2, outBounds);
        } else {
            super.getReorderCellBounds(targetCell, spanX, spanY, outBounds);
        }
    }

    @Override
    protected int[] findNearestArea(int relativeXPos, int relativeYPos, int minSpanX,
            int minSpanY, int spanX, int spanY, boolean ignoreOccupied, int[] result,
            int[] resultSpan) {
        int[] bestXY = super.findNearestArea(relativeXPos, relativeYPos, minSpanX, minSpanY,
                spanX, spanY, ignoreOccupied, result, resultSpan);
        if (!Launcher.isAutoFull() || spanX != 1 || spanY != 1) {
            return bestXY;
        }
        int lastOccupiedCellIndex = getLastOccupiedCellIndex();
        if (relativeXPos == -1 && relativeYPos == -1) {
            if (isDragOriginViewFromOuter()) {
                lastOccupiedCellIndex++;
            }
            int[] cellXY = getCellXY(lastOccupiedCellIndex);
            bestXY[0] = cellXY[0];
            bestXY[1] = cellXY[1];
        } else if (bestXY[0] != -1 && bestXY[1] != -1) {
            if (isDragOriginViewFromOuter()) {
                lastOccupiedCellIndex++;
            }
            if (getIndex(bestXY[0], bestXY[1]) > lastOccupiedCellIndex) {
                int[] cellXY = getCellXY(lastOccupiedCellIndex);
                bestXY[0] = cellXY[0];
                bestXY[1] = cellXY[1];
            }
        }
        return bestXY;
    }

    @Override
    protected boolean attemptPushInDirection(ArrayList<View> intersectingViews, Rect occupied,
            int[] direction, View ignoreView, ItemConfiguration solution) {
        if (!Launcher.isAutoFull() || occupied.width() != 1 || occupied.height() != 1
                || ((ignoreView != null || isDragging())
                        && isOccupiedByWidget(solution, occupied))) {
            return super.attemptPushInDirection(intersectingViews, occupied, direction,
                    ignoreView, solution);
        }
        if (getFirstAvailableCellIndex(mOccupied.cells) == -1) {
            return false;
        }
        setStrictOccupied(occupied, ignoreView, solution);
        return true;
    }

    /** Commits a pending auto-fill reorder of this page. */
    public void completeReorder(View dragView) {
        if (isItemPlacementDirty()) {
            commitTempPlacement(dragView);
            completeAndClearReorderPreviewAnimations();
            setItemPlacementDirty(false);
            setUseTempCoords(false);
            mPreviousSolution = null;
        }
    }

    /** Finishes the reorder-hint shake animations, leaving drag-over previews running. */
    public void clearHintAnimations() {
        for (int i = mShakeAnimators.size() - 1; i >= 0; i--) {
            ReorderPreviewAnimation animation = mShakeAnimators.valueAt(i);
            if (animation != null && animation.mode == ReorderPreviewAnimation.MODE_HINT) {
                animation.finishAnimation();
                mShakeAnimators.removeAt(i);
            }
        }
    }

    private int getLastOccupiedCellIndex() {
        int cellIndex = -1;
        for (int i = mShortcutsAndWidgets.getChildCount() - 1; i >= 0; i--) {
            CellLayoutLayoutParams lp =
                    (CellLayoutLayoutParams) mShortcutsAndWidgets.getChildAt(i).getLayoutParams();
            int index = getIndex(lp.getCellX(), lp.getCellY());
            if (lp.cellHSpan == 1 && lp.cellVSpan == 1 && cellIndex < index) {
                cellIndex = index;
            }
        }
        return cellIndex;
    }

    private boolean isDragOriginViewFromOuter() {
        if (!(mActivity instanceof Launcher launcher)) {
            return true;
        }
        CellLayout.CellInfo dragInfo = launcher.getWorkspace().getDragInfo();
        if (dragInfo == null || dragInfo.cell == null) {
            return true;
        }
        for (View v = dragInfo.cell; v != null;
                v = v.getParent() instanceof View ? (View) v.getParent() : null) {
            if (v instanceof CellLayout) {
                return v != this;
            }
        }
        return true;
    }

    private boolean isDragging() {
        return mActivity instanceof Launcher launcher
                && launcher.getDragController().isDragging();
    }

    /** Packs the 1x1 items of {@code solution} row-major around widgets and the drop cell. */
    private void setStrictOccupied(Rect occupied, View dragView, ItemConfiguration solution) {
        GridOccupancy grid = new GridOccupancy(mCountX, mCountY);
        ArrayList<CellAndSpan> icons = new ArrayList<>();
        for (CellAndSpan c : solution.map.values()) {
            if (c.spanX == 1 && c.spanY == 1) {
                icons.add(c);
            } else {
                grid.markCells(c, true);
            }
        }
        if (isDragging()) {
            grid.markCells(occupied, true);
            CellAndSpan dragged = solution.map.get(dragView);
            if (dragged != null) {
                icons.remove(dragged);
            }
        }
        icons.sort((a, b) -> getIndex(a.cellX, a.cellY) - getIndex(b.cellX, b.cellY));
        int count = mCountX * mCountY;
        for (CellAndSpan c : icons) {
            for (int i = 0; i < count; i++) {
                int[] cellXY = getCellXY(i);
                if (!grid.cells[cellXY[0]][cellXY[1]]) {
                    c.cellX = cellXY[0];
                    c.cellY = cellXY[1];
                    grid.markCells(c, true);
                    break;
                }
            }
        }
    }

    private boolean isOccupiedByWidget(ItemConfiguration solution, Rect rect) {
        for (CellAndSpan c : solution.map.values()) {
            if ((c.spanX > 1 || c.spanY > 1)
                    && rect.left >= c.cellX && rect.left < c.cellX + c.spanX
                    && rect.top >= c.cellY && rect.top < c.cellY + c.spanY) {
                return true;
            }
        }
        return false;
    }

    private int getIndex(int cellX, int cellY) {
        return mCountX * cellY + cellX;
    }

    private int[] getCellXY(int index) {
        if (index == -1) {
            return new int[] {-1, -1};
        }
        return new int[] {index % mCountX, index / mCountX};
    }

    /** First free cell in row-major order, or -1 if the page is full. */
    private int getFirstAvailableCellIndex(boolean[][] cells) {
        for (int y = 0; y < mCountY; y++) {
            for (int x = 0; x < mCountX; x++) {
                if (!cells[x][y]) {
                    return getIndex(x, y);
                }
            }
        }
        return -1;
    }
}
