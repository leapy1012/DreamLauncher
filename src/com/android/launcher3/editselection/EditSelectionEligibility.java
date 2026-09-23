package com.android.launcher3.editselection;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.view.View;

import androidx.annotation.Nullable;

import com.android.launcher3.BuildConfig;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.LauncherStyle;
import com.android.launcher3.R;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.model.data.FolderInfo;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.model.data.ItemInfoWithIcon;
import com.android.launcher3.model.data.WorkspaceItemInfo;

/**
 * Rules for which workspace icons show edit-mode selection checkmarks / actions.
 * <p>
 * Always excludes hotseat.<br>
 * Cleanup / Switch wallpaper are launcher-owned utilities: they show a remove (−) badge
 * (not a checkmark), can leave Home, and are never uninstalled.<br>
 * Workspace icons with a DB id can be selected (Create folder, etc.).<br>
 * Checkmarks still show on system apps (Oppo); Uninstall is gated separately.
 */
public final class EditSelectionEligibility {

    private static final ComponentName CLEANUP = new ComponentName(
            BuildConfig.APPLICATION_ID,
            "com.android.launcher3.big.memoryclean.MemoryCleanActivity");
    private static final ComponentName SWITCH_WALLPAPER = new ComponentName(
            BuildConfig.APPLICATION_ID,
            "com.android.launcher3.settings.WallpaperChangeActivity");

    private EditSelectionEligibility() {}

    public static boolean canShowCheckmark(Context context, @Nullable View view) {
        if (view == null) {
            return false;
        }
        // Large-folder preview cells never draw selection checks.
        if (view instanceof com.android.launcher3.folder.large.listview.LargeFolderIconItem) {
            return false;
        }
        Object tag = view.getTag();
        if (!(tag instanceof ItemInfo info)) {
            return false;
        }
        if (isHotseat(info)) {
            return false;
        }
        // Utilities use the remove (−) badge instead of multi-select checks.
        if (isLauncherUtility(info)) {
            return false;
        }
        // Folders draw their own chrome (check / count badge); not via BubbleTextView.
        if (view instanceof FolderIcon || info instanceof FolderInfo) {
            return false;
        }
        // Apps / shortcuts on the workspace or inside an open folder.
        if (info instanceof WorkspaceItemInfo
                || info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION
                || info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT
                || info.itemType == LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT) {
            return canRemoveFromWorkspace(info) || isInsideFolder(info);
        }
        return false;
    }

    /**
     * Cleanup / Switch wallpaper in home-edit: widget-style remove (−), not a checkmark.
     */
    public static boolean canShowRemoveBadge(Context context, @Nullable View view) {
        if (view == null || !(view.getTag() instanceof ItemInfo info)) {
            return false;
        }
        if (isHotseat(info) || !canRemoveFromWorkspace(info)) {
            return false;
        }
        return isLauncherUtility(info);
    }

    public static boolean canToggle(Context context, @Nullable View view) {
        if (view instanceof FolderIcon
                || view instanceof com.android.launcher3.folder.large.listview.LargeFolderIconItem) {
            return false;
        }
        return canShowCheckmark(context, view);
    }

    /** Apps inside a folder have container = folder id (not DESKTOP/HOTSEAT). */
    private static boolean isInsideFolder(ItemInfo info) {
        return info.container > 0;
    }

    /**
     * Workspace-edit trash label. Oppo drawer mode is always {@code remove_action}.
     */
    public static int removeButtonLabel(Context context,
            @Nullable Iterable<? extends ItemInfo> selected) {
        if (LauncherStyle.isAppDrawer(context)) {
            return R.string.edit_selection_remove;
        }
        boolean uninstallable = false;
        boolean removable = false;
        if (selected != null) {
            for (ItemInfo info : selected) {
                if (isUninstallable(context, info)) {
                    uninstallable = true;
                } else if (canRemoveFromHome(info)) {
                    removable = true;
                }
            }
        }
        if (uninstallable && !removable) {
            return R.string.edit_selection_uninstall;
        }
        if (removable) {
            return R.string.edit_selection_remove;
        }
        return R.string.edit_selection_uninstall;
    }

    /**
     * Oppo {@code PagePreviewButtonContainer}: drawer mode always removes from Home;
     * standard mode uninstalls or removes shortcuts.
     */
    public static boolean isRemoveButtonEnabled(Context context,
            @Nullable Iterable<? extends ItemInfo> selected) {
        if (selected == null) {
            return false;
        }
        if (LauncherStyle.isAppDrawer(context)) {
            for (ItemInfo info : selected) {
                if (canRemoveFromHome(info)) {
                    return true;
                }
            }
            return false;
        }
        return isUninstallButtonEnabled(context, selected);
    }

    /**
     * Oppo {@code GenericUtils.isEnableUninstallButton}: enable Uninstall when any
     * selected item is uninstallable or a removable shortcut.
     */
    public static boolean isUninstallButtonEnabled(Context context,
            @Nullable Iterable<? extends ItemInfo> selected) {
        if (selected == null) {
            return false;
        }
        for (ItemInfo info : selected) {
            if (isUninstallOrRemoveEligible(context, info)) {
                return true;
            }
        }
        return false;
    }

    /** Workspace / folder items that can leave Home without uninstalling the package. */
    public static boolean canRemoveFromHome(@Nullable ItemInfo info) {
        if (info == null || isHotseat(info)) {
            return false;
        }
        return canRemoveFromWorkspace(info);
    }

    /**
     * Whether Uninstall/Remove should apply to this item
     * (Oppo {@code PackageUtils.isCanUninstall || isCanDeleteIcon}).
     * Launcher utilities are remove-from-Home only (never package uninstall).
     */
    public static boolean isUninstallOrRemoveEligible(Context context, @Nullable ItemInfo info) {
        if (info == null || isHotseat(info)) {
            return false;
        }
        if (isLauncherUtility(info)) {
            return canRemoveFromWorkspace(info);
        }
        return isUninstallable(context, info) || isRemovableShortcut(info);
    }

    private static boolean isHotseat(ItemInfo info) {
        return info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT
                || info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT_PREDICTION;
    }

    /** Cleanup / Switch wallpaper — launcher-owned utilities (remove icon, never uninstall). */
    public static boolean isLauncherUtility(ItemInfo info) {
        ComponentName cn = info.getTargetComponent();
        if (cn == null) {
            return false;
        }
        return isCleanupUtility(cn) || isSwitchWallpaperUtility(cn);
    }

    public static boolean isCleanupUtility(@Nullable ComponentName cn) {
        return CLEANUP.equals(cn);
    }

    public static boolean isSwitchWallpaperUtility(@Nullable ComponentName cn) {
        return SWITCH_WALLPAPER.equals(cn);
    }

    private static boolean canRemoveFromWorkspace(ItemInfo info) {
        return info.id != ItemInfo.NO_ID;
    }

    /** Oppo {@code PackageUtils.isCanDeleteIcon}: shortcut / deep shortcut. */
    private static boolean isRemovableShortcut(ItemInfo item) {
        return item.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT
                || item.itemType == LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT;
    }

    /**
     * Whether the long-press popup may show Remove Widget (any home style).
     */
    public static boolean canShowPopupRemoveWidget(@Nullable ItemInfo info) {
        if (info == null || info.id == ItemInfo.NO_ID) {
            return false;
        }
        return info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET
                || info.itemType == LauncherSettings.Favorites.ITEM_TYPE_CUSTOM_APPWIDGET;
    }

    /**
     * Whether the long-press popup may show Remove (Oppo drawer mode only):
     * icon must already live on Home / hotseat / in a folder (has a DB id).
     */
    public static boolean canShowPopupRemove(Context context, @Nullable ItemInfo info) {
        if (info == null || !LauncherStyle.isAppDrawer(context)) {
            return false;
        }
        if (info.isPredictedItem()) {
            return false;
        }
        // All Apps entries are not on the workspace — nothing to remove.
        if (info instanceof com.android.launcher3.model.data.AppInfo
                && info.id == ItemInfo.NO_ID) {
            return false;
        }
        return info.id != ItemInfo.NO_ID
                && (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION
                || info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT
                || info.itemType == LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT);
    }

    /**
     * Whether the long-press popup may show Uninstall (both launcher styles).
     */
    public static boolean canShowPopupUninstall(Context context, @Nullable ItemInfo info) {
        if (info == null || info.isPredictedItem()) {
            return false;
        }
        return isUninstallable(context, info);
    }

    /** Mirrors {@link com.android.launcher3.SecondaryDropTarget} uninstall eligibility. */
    public static boolean isUninstallable(Context context, ItemInfo item) {
        if (isLauncherUtility(item)) {
            return false;
        }
        if (item.itemType != LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
            return false;
        }
        if (item instanceof ItemInfoWithIcon iconInfo) {
            if ((iconInfo.runtimeStatusFlags & ItemInfoWithIcon.FLAG_SYSTEM_MASK) != 0
                    && (iconInfo.runtimeStatusFlags & ItemInfoWithIcon.FLAG_SYSTEM_NO) == 0) {
                return false;
            }
        }
        Intent intent = item.getIntent();
        if (intent == null || item.user == null) {
            return false;
        }
        LauncherActivityInfo lai = context.getSystemService(LauncherApps.class)
                .resolveActivity(intent, item.user);
        return lai != null
                && (lai.getApplicationInfo().flags & ApplicationInfo.FLAG_SYSTEM) == 0;
    }
}
