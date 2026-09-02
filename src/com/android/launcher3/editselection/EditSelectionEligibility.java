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
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.model.data.FolderInfo;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.model.data.ItemInfoWithIcon;
import com.android.launcher3.model.data.WorkspaceItemInfo;

/**
 * Rules for which workspace icons show edit-mode selection checkmarks / actions.
 * <p>
 * Always excludes hotseat and launcher utilities (Cleanup / Switch wallpaper).<br>
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
        if (view instanceof com.android.launcher3.folder.large.listview.HxyLargeFolderIconItem) {
            return false;
        }
        Object tag = view.getTag();
        if (!(tag instanceof ItemInfo info)) {
            return false;
        }
        if (isHotseat(info)) {
            return false;
        }
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

    public static boolean canToggle(Context context, @Nullable View view) {
        if (view instanceof FolderIcon
                || view instanceof com.android.launcher3.folder.large.listview.HxyLargeFolderIconItem) {
            return false;
        }
        return canShowCheckmark(context, view);
    }

    /** Apps inside a folder have container = folder id (not DESKTOP/HOTSEAT). */
    private static boolean isInsideFolder(ItemInfo info) {
        return info.container > 0;
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

    /**
     * Whether Uninstall/Remove should apply to this item
     * (Oppo {@code PackageUtils.isCanUninstall || isCanDeleteIcon}).
     */
    public static boolean isUninstallOrRemoveEligible(Context context, @Nullable ItemInfo info) {
        if (info == null || isHotseat(info) || isLauncherUtility(info)) {
            return false;
        }
        return isUninstallable(context, info) || isRemovableShortcut(info);
    }

    private static boolean isHotseat(ItemInfo info) {
        return info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT
                || info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT_PREDICTION;
    }

    /** Cleanup / Switch wallpaper — launcher-owned utilities, not user apps. */
    private static boolean isLauncherUtility(ItemInfo info) {
        ComponentName cn = info.getTargetComponent();
        if (cn == null) {
            return false;
        }
        return CLEANUP.equals(cn) || SWITCH_WALLPAPER.equals(cn);
    }

    private static boolean canRemoveFromWorkspace(ItemInfo info) {
        return info.id != ItemInfo.NO_ID;
    }

    /** Oppo {@code PackageUtils.isCanDeleteIcon}: shortcut / deep shortcut. */
    private static boolean isRemovableShortcut(ItemInfo item) {
        return item.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT
                || item.itemType == LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT;
    }

    /** Mirrors {@link com.android.launcher3.SecondaryDropTarget} uninstall eligibility. */
    private static boolean isUninstallable(Context context, ItemInfo item) {
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
