package com.android.launcher3.editselection;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.android.launcher3.CellLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.R;
import com.android.launcher3.Workspace;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.model.data.FolderInfo;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.model.data.WorkspaceItemInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Batch actions for edit-mode selection: Create folder / Uninstall-or-Remove.
 */
public final class EditSelectionActions {

    private EditSelectionActions() {}

    public static void createFolder(Launcher launcher, Iterable<View> selected) {
        List<View> appViews = new ArrayList<>();
        for (View v : selected) {
            if (v.getTag() instanceof WorkspaceItemInfo) {
                appViews.add(v);
            }
        }
        if (appViews.size() < 2) {
            Toast.makeText(launcher, R.string.edit_selection_need_two_apps, Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        View anchor = appViews.get(0);
        ItemInfo anchorInfo = (ItemInfo) anchor.getTag();
        Workspace workspace = launcher.getWorkspace();
        CellLayout layout = workspace.getScreenWithId(anchorInfo.screenId);
        if (layout == null) {
            layout = (CellLayout) workspace.getChildAt(workspace.getCurrentPage());
        }
        if (layout == null) {
            Toast.makeText(launcher, R.string.edit_selection_no_space_for_folder, Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        int screenId = anchorInfo.screenId;
        int cellX = anchorInfo.cellX;
        int cellY = anchorInfo.cellY;

        List<WorkspaceItemInfo> infos = new ArrayList<>();
        for (View v : appViews) {
            WorkspaceItemInfo info = (WorkspaceItemInfo) v.getTag();
            infos.add(info);
            launcher.removeItem(v, info, false /* deleteFromDb */);
        }

        if (!layout.isRegionVacant(cellX, cellY, 1, 1)) {
            int[] cell = new int[2];
            if (!layout.findCellForSpan(cell, 1, 1)) {
                Toast.makeText(launcher, R.string.edit_selection_no_space_for_folder,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            cellX = cell[0];
            cellY = cell[1];
        }

        FolderIcon folderIcon = launcher.addFolder(
                layout,
                LauncherSettings.Favorites.CONTAINER_DESKTOP,
                screenId,
                cellX,
                cellY);
        FolderInfo folderInfo = (FolderInfo) folderIcon.getTag();
        for (WorkspaceItemInfo info : infos) {
            folderInfo.add(info, false);
            launcher.getModelWriter().addOrMoveItemInDatabase(
                    info, folderInfo.id, 0, info.cellX, info.cellY);
        }
        folderIcon.invalidate();
    }

    public static void uninstallOrRemove(Launcher launcher, Iterable<View> selected) {
        List<View> snapshot = new ArrayList<>();
        for (View v : selected) {
            snapshot.add(v);
        }
        boolean startedUninstall = false;
        boolean removedAny = false;

        for (View v : snapshot) {
            Object tag = v.getTag();
            if (!(tag instanceof ItemInfo info)) {
                continue;
            }
            if (!EditSelectionEligibility.isUninstallOrRemoveEligible(launcher, info)) {
                continue;
            }

            ComponentName uninstallCn = getUninstallTarget(launcher, info);
            if (uninstallCn != null) {
                if (startUninstallActivity(launcher, uninstallCn, info)) {
                    startedUninstall = true;
                }
            } else if (launcher.removeItem(v, info, true)) {
                // Shortcut / deep shortcut — remove from workspace.
                removedAny = true;
            }
        }

        if (!startedUninstall && !removedAny) {
            Toast.makeText(launcher, R.string.uninstall_system_app_text, Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    private static ComponentName getUninstallTarget(Context context, ItemInfo item) {
        if (item.itemType != LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
            return null;
        }
        Intent intent = item.getIntent();
        if (intent == null || item.user == null) {
            return null;
        }
        LauncherActivityInfo lai = context.getSystemService(LauncherApps.class)
                .resolveActivity(intent, item.user);
        if (lai != null
                && (lai.getApplicationInfo().flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
            return lai.getComponentName();
        }
        return null;
    }

    private static boolean startUninstallActivity(Launcher launcher, ComponentName cn,
            ItemInfo info) {
        try {
            Intent intent = Intent.parseUri(launcher.getString(R.string.delete_package_intent), 0)
                    .setData(Uri.fromParts("package", cn.getPackageName(), cn.getClassName()))
                    .putExtra(Intent.EXTRA_USER, info.user);
            launcher.startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
