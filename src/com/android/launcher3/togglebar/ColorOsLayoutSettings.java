package com.android.launcher3.togglebar;

import android.view.View;

import androidx.annotation.Nullable;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.ShortcutAndWidgetContainer;
import com.android.launcher3.Workspace;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.model.data.ItemInfo;

/**
 * Oppo {@code WordlessDesktopHelper} hide-names flag.
 */
public final class ColorOsLayoutSettings {

    /** Non-null while the Layout sheet is previewing a hide-names value. */
    @Nullable
    private static Boolean sPreviewHideNames;

    private ColorOsLayoutSettings() {}

    public static boolean isHideIconNames(android.content.Context context) {
        if (sPreviewHideNames != null) {
            return sPreviewHideNames;
        }
        return LauncherPrefs.getPrefs(context).getBoolean(
                LauncherPrefs.ENABLE_NO_APP_TITLE, false);
    }

    public static void setPreviewHideNames(@Nullable Boolean hide) {
        sPreviewHideNames = hide;
    }

    public static void setHideIconNames(android.content.Context context, boolean hide) {
        LauncherPrefs.getPrefs(context).edit()
                .putBoolean(LauncherPrefs.ENABLE_NO_APP_TITLE, hide)
                .apply();
    }

    /**
     * Hide or restore workspace labels only. Hotseat stays nameless unless docked-app is on.
     */
    public static void applyHideNames(@Nullable Launcher launcher, boolean hide) {
        if (launcher == null) {
            return;
        }
        Workspace workspace = launcher.getWorkspace();
        if (workspace != null) {
            for (int i = 0; i < workspace.getPageCount(); i++) {
                View page = workspace.getPageAt(i);
                if (page instanceof CellLayout cell) {
                    applyHideToContainer(cell.getShortcutsAndWidgets(), hide);
                }
            }
        }
    }

    public static void applyToWorkspace(@Nullable Launcher launcher) {
        applyHideNames(launcher, isHideIconNames(launcher));
    }

    private static void applyHideToContainer(@Nullable ShortcutAndWidgetContainer container,
            boolean hide) {
        if (container == null) {
            return;
        }
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (isHotseatChild(child)) {
                continue;
            }
            if (child instanceof BubbleTextView icon) {
                icon.setTextAlpha(hide ? 0f : 1f);
                icon.invalidate();
            } else if (child instanceof FolderIcon folderIcon) {
                folderIcon.setTextVisibility(!hide);
                folderIcon.setTextVisible(!hide);
                folderIcon.invalidate();
            }
        }
    }

    private static boolean isHotseatChild(View child) {
        Object tag = child.getTag();
        if (!(tag instanceof ItemInfo info)) {
            return false;
        }
        return info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT
                || info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT_PREDICTION;
    }
}
