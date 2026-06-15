package com.android.launcher3.folder.large.switchparams;

import android.util.Log;
import android.view.View;
import com.android.launcher3.CellLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.ShortcutAndWidgetContainer;
import com.android.launcher3.Workspace;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.views.ActivityContext;
import com.android.launcher3.folder.large.HxyLargeFolderIcon;
import com.android.launcher3.folder.large.HxyLargeFolderProxy;

public class HxyLargeFolderSwitcher implements ISwitchFolderAnimation {
    private static final String TAG = "HxyLargeFolderSwitcher";
    private BaseSwitchParams mAniParams = null;

    public void release() {
        releaseAnimationParams();
    }

    public void releaseAnimationParams() {
        BaseSwitchParams baseSwitchParams = this.mAniParams;
        if (baseSwitchParams != null) {
            baseSwitchParams.release();
            this.mAniParams = null;
        }
    }

    public void switchLargeFolder(ActivityContext context, HxyLargeFolderIcon child, int spanX, int spanY) {
        int screenId;
        boolean isLargeFolder;
        CellLayout newCellLayout;
        HxyLargeFolderIcon hxyLargeFolderIcon = child;
        releaseAnimationParams();
        CellLayout cellLayout = getCellLayout(child);
        if (cellLayout != null) {
            ShortcutAndWidgetContainer container = cellLayout.getShortcutsAndWidgets();
            Launcher launcher = Launcher.cast(context);
            if (!isHotseatLayout(launcher, cellLayout) && (screenId = launcher.getWorkspace().getIdForScreen(cellLayout)) >= 0) {
                ItemInfo info = (ItemInfo) child.getTag();
                int[] cellXY = {info.cellX, info.cellY};
                int[] screenIds = {screenId};
                isLargeFolder = HxyLargeFolderProxy.isLargeFolder(spanX, spanY);
                if (isLargeFolder) {
                    newCellLayout = updateLargeFolderLayout(launcher.getWorkspace(), cellLayout, child, screenIds, cellXY, spanX, spanY);
                } else {
                    newCellLayout = null;
                }
                CellLayout targetLayout = cellLayout;
                if (newCellLayout != null) {
                    this.mAniParams = new NextPageParams(launcher, hxyLargeFolderIcon, container, true);
                    targetLayout = newCellLayout;
                } else if (isChangeCellXY(info, cellXY)) {
                    this.mAniParams = new CurrentPageParams(launcher, hxyLargeFolderIcon, true);
                } else {
                    this.mAniParams = new CurrentLocationParams(launcher, hxyLargeFolderIcon, isLargeFolder);
                }
                Log.d(TAG, "HxyLargeFolderSwitcher switchLargeFolder ; cellX = " + cellXY[0] + "; cellY = " + cellXY[1] + "; screenId = " + screenIds[0] + "; getSwitchMode = " + this.mAniParams.getSwitchMode() + "; isLargeFolder = " + isLargeFolder + "; info.cellX = " + info.cellX + "; info.cellY = " + info.cellY);
                int i = screenIds[0];
                this.mAniParams.setSwitchFolderBean(new SwitchFolderBean(launcher, targetLayout, child, i, cellXY[0], cellXY[1], spanX, spanY));
                this.mAniParams.onSwitchFolderBegin();
                this.mAniParams.startAnimation();
            }
        }
    }

    public void stopAnimation() {
        BaseSwitchParams baseSwitchParams = this.mAniParams;
        if (baseSwitchParams != null) {
            baseSwitchParams.stopAnimation();
        }
    }

    public void onSwitchFolderBegin() {
        BaseSwitchParams baseSwitchParams = this.mAniParams;
        if (baseSwitchParams != null) {
            baseSwitchParams.onSwitchFolderBegin();
        }
    }

    public void onSwitchFolderEnd() {
        BaseSwitchParams baseSwitchParams = this.mAniParams;
        if (baseSwitchParams != null) {
            baseSwitchParams.onSwitchFolderEnd();
        }
    }

    private static boolean isChangeCellXY(ItemInfo info, int[] cellXY) {
        return info.cellX != cellXY[0] || info.cellY != cellXY[1];
    }

    private CellLayout updateLargeFolderLayout(Workspace workspace, CellLayout cellLayout, HxyLargeFolderIcon child, int[] screenIds, int[] cellXY, int spanX, int spanY) {
        if (isSupportLarge(cellLayout.getShortcutsAndWidgets(), child)) {
            return null;
        }
        int[] tempCellXY = {-1, -1};
        CellLayout newCellLayout = workspace.findNextWorkspaceScreen(cellLayout, tempCellXY, spanX, spanY);
        if (newCellLayout != null) {
            cellXY[0] = tempCellXY[0];
            cellXY[1] = tempCellXY[1];
        } else {
            newCellLayout = workspace.insertNewWorkspaceScreen();
            cellXY[0] = 0;
            cellXY[1] = 0;
        }
        screenIds[0] = getScreenId(workspace, newCellLayout);
        if (newCellLayout != cellLayout) {
            return newCellLayout;
        }
        return null;
    }

    private static int getScreenId(Workspace workspace, CellLayout layout) {
        return workspace.getIdForScreen(layout);
    }

    public static boolean isHotseatLayout(ActivityContext context, CellLayout cellLayout) {
        return isHotseatLayout(Launcher.cast(context), cellLayout);
    }

    public static boolean isHotseatLayout(Launcher launcher, CellLayout cellLayout) {
        return launcher.isHotseatLayout(cellLayout);
    }

    public static boolean isSupportHorizontal(ShortcutAndWidgetContainer container, View child) {
        ItemInfo currentItem = (ItemInfo) child.getTag();
        if (currentItem.cellX + 1 >= container.getCountX()) {
            return false;
        }
        if (container.getChildAt(currentItem.cellX + 1, currentItem.cellY) == null) {
            return true;
        }
        return false;
    }

    public static boolean isSupportVertical(ShortcutAndWidgetContainer container, View child) {
        ItemInfo currentItem = (ItemInfo) child.getTag();
        if (currentItem.cellY + 1 >= container.getCountY()) {
            return false;
        }
        if (container.getChildAt(currentItem.cellX, currentItem.cellY + 1) == null) {
            return true;
        }
        return false;
    }

    public static boolean isSupportLarge(ShortcutAndWidgetContainer container, View child) {
        ItemInfo currentItem = (ItemInfo) child.getTag();
        if (container.getChildAt(currentItem.cellX + 1, currentItem.cellY + 1) != null || !isSupportHorizontal(container, child) || !isSupportVertical(container, child)) {
            return false;
        }
        return true;
    }

    public static CellLayout getCellLayout(View view) {
        if (!(view.getParent() instanceof ShortcutAndWidgetContainer)) {
            return null;
        }
        ShortcutAndWidgetContainer container = (ShortcutAndWidgetContainer) view.getParent();
        if (container.getParent() instanceof CellLayout) {
            return (CellLayout) container.getParent();
        }
        return null;
    }

    public ShortcutAndWidgetContainer getContainer(View view) {
        if (view.getParent() instanceof ShortcutAndWidgetContainer) {
            return (ShortcutAndWidgetContainer) view.getParent();
        }
        return null;
    }
}