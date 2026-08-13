package com.android.launcher3.big.popup;

import static com.android.launcher3.LauncherSettings.Favorites.CONTAINER_DESKTOP;

import android.graphics.Point;
import android.view.View;
import android.widget.Toast;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.folder.large.HxyLargeFolderIcon;
import com.android.launcher3.model.data.FolderInfo;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.android.launcher3.popup.SystemShortcut;
import com.android.launcher3.touch.ItemClickHandler;
import com.coui.appcompat.dialog.COUIAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

/** Source-matched ColorOS folder actions shown under the three preview-style controls. */
public final class ColorOsFolderShortcuts {
    private static final long OPEN_AFTER_POPUP_MS = 330L;

    private ColorOsFolderShortcuts() {
    }

    public static List<SystemShortcut<Launcher>> create(
            Launcher launcher, FolderInfo info, HxyLargeFolderIcon icon) {
        ArrayList<SystemShortcut<Launcher>> shortcuts = new ArrayList<>(4);
        shortcuts.add(new SwitchFolderShortcut(launcher, info, icon));
        shortcuts.add(new Open(launcher, info, icon));
        shortcuts.add(new Rename(launcher, info, icon));
        shortcuts.add(new Ungroup(launcher, info, icon));
        return shortcuts;
    }

    private abstract static class FolderAction extends SystemShortcut<Launcher> {
        protected final HxyLargeFolderIcon mFolderIcon;

        FolderAction(int iconRes, int labelRes, Launcher launcher, FolderInfo info,
                HxyLargeFolderIcon icon) {
            super(iconRes, labelRes, launcher, info, icon);
            mFolderIcon = icon;
        }

        protected void closeAndRun(Runnable action) {
            AbstractFloatingView.closeAllOpenViews(mTarget);
            mFolderIcon.postDelayed(action, OPEN_AFTER_POPUP_MS);
        }
    }

    private static final class Open extends FolderAction {
        Open(Launcher launcher, FolderInfo info, HxyLargeFolderIcon icon) {
            super(R.drawable.big_folder_expand_shortcut_icon,
                    R.string.coloros_folder_open, launcher, info, icon);
        }

        @Override
        public void onClick(View view) {
            closeAndRun(() -> ItemClickHandler.onClick(mFolderIcon));
        }
    }

    private static final class Rename extends FolderAction {
        Rename(Launcher launcher, FolderInfo info, HxyLargeFolderIcon icon) {
            super(R.drawable.folder_rename_shortcut_icon,
                    R.string.coloros_folder_rename, launcher, info, icon);
        }

        @Override
        public void onClick(View view) {
            closeAndRun(() -> {
                ItemClickHandler.onClick(mFolderIcon);
                Folder folder = mFolderIcon.getFolder();
                if (folder != null) {
                    folder.postDelayed(() -> folder.mFolderName.requestFocus(), 250L);
                }
            });
        }
    }

    private static final class Ungroup extends FolderAction {
        Ungroup(Launcher launcher, FolderInfo info, HxyLargeFolderIcon icon) {
            super(R.drawable.ic_folder_disband_new,
                    R.string.coloros_folder_ungroup, launcher, info, icon);
        }

        @Override
        public void onClick(View view) {
            AbstractFloatingView.closeAllOpenViews(mTarget);
            FolderInfo info = (FolderInfo) mItemInfo;
            new COUIAlertDialogBuilder(mTarget)
                    .setTitle(mTarget.getString(
                            R.string.coloros_folder_ungroup_title, info.title))
                    .setMessage(R.string.coloros_folder_ungroup_message)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.coloros_folder_ungroup_confirm,
                            (dialog, which) -> ungroup(info))
                    .create()
                    .show();
        }

        private void ungroup(FolderInfo info) {
            CellLayout layout = mTarget.getWorkspace().getScreenWithId(info.screenId);
            if (layout == null) {
                showNoSpace();
                return;
            }

            ArrayList<WorkspaceItemInfo> children = new ArrayList<>(info.contents);
            ArrayList<Point> targets = collectTargets(layout, info, children.size());
            if (targets.size() < children.size()) {
                showNoSpace();
                return;
            }

            mTarget.removeItem(mFolderIcon, info, false);
            for (int index = 0; index < children.size(); index++) {
                WorkspaceItemInfo child = children.get(index);
                Point target = targets.get(index);
                child.spanX = 1;
                child.spanY = 1;
                mTarget.getModelWriter().moveItemInDatabase(child, CONTAINER_DESKTOP,
                        info.screenId, target.x, target.y);
            }
            mTarget.getModelWriter().deleteItemFromDatabase(info, "ColorOS folder ungroup");
            mTarget.bindItems(new ArrayList<ItemInfo>(children), true);
        }

        private ArrayList<Point> collectTargets(
                CellLayout layout, FolderInfo folder, int required) {
            ArrayList<Point> result = new ArrayList<>(required);
            int folderRight = folder.cellX + folder.spanX;
            int folderBottom = folder.cellY + folder.spanY;
            for (int y = 0; y < layout.getCountY() && result.size() < required; y++) {
                for (int x = 0; x < layout.getCountX() && result.size() < required; x++) {
                    boolean occupiedByFolder = x >= folder.cellX && x < folderRight
                            && y >= folder.cellY && y < folderBottom;
                    if (occupiedByFolder || !layout.isOccupied(x, y)) {
                        result.add(new Point(x, y));
                    }
                }
            }
            return result;
        }

        private void showNoSpace() {
            Toast.makeText(mTarget, R.string.coloros_folder_ungroup_no_space,
                    Toast.LENGTH_SHORT).show();
        }
    }
}
