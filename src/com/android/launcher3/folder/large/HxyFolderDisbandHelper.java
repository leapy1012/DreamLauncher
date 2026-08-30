package com.android.launcher3.folder.large;

import android.graphics.Point;
import android.util.Log;
import android.view.View;

import com.android.launcher3.CellLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.Workspace;
import com.android.launcher3.celllayout.CellPosMapper;
import com.android.launcher3.model.data.FolderInfo;
import com.android.launcher3.model.data.WorkspaceItemInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Ungroups a folder: moves every child onto the workspace and deletes the folder.
 */
public final class HxyFolderDisbandHelper {
    private static final String TAG = "HxyFolderDisband";

    private HxyFolderDisbandHelper() {
    }

    public static void ungroup(Launcher launcher, HxyLargeFolderIcon folderIcon) {
        if (launcher == null || folderIcon == null) {
            return;
        }
        Object tag = folderIcon.getTag();
        if (!(tag instanceof FolderInfo)) {
            return;
        }
        FolderInfo folderInfo = (FolderInfo) tag;
        if (folderInfo.contents.isEmpty()) {
            return;
        }

        List<WorkspaceItemInfo> items = new ArrayList<>(folderInfo.contents);
        CellPosMapper.CellPos pos = launcher.getCellPosMapper().mapModelToPresenter(folderInfo);
        int container = folderInfo.container;
        int screenId = pos.screenId;
        Workspace workspace = launcher.getWorkspace();
        CellLayout layout = launcher.getCellLayout(container, screenId);
        if (layout == null) {
            Log.w(TAG, "ungroup: missing CellLayout for folder");
            return;
        }

        folderIcon.removeListeners();
        workspace.removeWorkspaceItem(folderIcon);

        int[] cellXY = new int[2];
        for (WorkspaceItemInfo item : items) {
            folderInfo.remove(item, false /* animate */);

            CellLayout targetLayout = layout;
            int targetScreenId = screenId;
            if (!targetLayout.findCellForSpan(cellXY, 1, 1)) {
                Point found = findSpaceOnWorkspace(launcher, cellXY);
                if (found == null) {
                    Log.w(TAG, "ungroup: no space for " + item);
                    // Put item back into a transient state on current page origin.
                    cellXY[0] = 0;
                    cellXY[1] = 0;
                } else {
                    targetScreenId = found.x;
                    targetLayout = launcher.getCellLayout(
                            LauncherSettings.Favorites.CONTAINER_DESKTOP, targetScreenId);
                    // cellXY already filled by findSpaceOnWorkspace
                }
            }

            if (targetLayout == null) {
                continue;
            }
            launcher.getModelWriter().addOrMoveItemInDatabase(
                    item, container == LauncherSettings.Favorites.CONTAINER_HOTSEAT
                            ? LauncherSettings.Favorites.CONTAINER_DESKTOP
                            : container,
                    targetScreenId, cellXY[0], cellXY[1]);
            View shortcut = launcher.createShortcut(targetLayout, item);
            workspace.addInScreen(shortcut,
                    container == LauncherSettings.Favorites.CONTAINER_HOTSEAT
                            ? LauncherSettings.Favorites.CONTAINER_DESKTOP
                            : container,
                    targetScreenId, cellXY[0], cellXY[1], 1, 1);
        }

        launcher.getModelWriter().deleteItemFromDatabase(folderInfo, "ungroup folder");
    }

    /**
     * @return Point(screenId, unused) with cellXY filled, or null if none.
     */
    private static Point findSpaceOnWorkspace(Launcher launcher, int[] cellXY) {
        Workspace workspace = launcher.getWorkspace();
        workspace.addExtraEmptyScreens();
        int count = workspace.getPageCount();
        for (int i = 0; i < count; i++) {
            int screenId = workspace.getScreenIdForPageIndex(i);
            CellLayout layout = launcher.getCellLayout(
                    LauncherSettings.Favorites.CONTAINER_DESKTOP, screenId);
            if (layout != null && layout.findCellForSpan(cellXY, 1, 1)) {
                return new Point(screenId, 0);
            }
        }
        return null;
    }
}
