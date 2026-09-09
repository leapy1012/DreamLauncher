package com.android.launcher3.editselection;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.LauncherStyle;
import com.android.launcher3.R;
import com.android.launcher3.Workspace;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.model.data.FolderInfo;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.android.launcher3.util.IntSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Batch actions for edit-mode selection: Create folder / Uninstall-or-Remove.
 */
public final class EditSelectionActions {

    /** Oppo {@code BatchDragViewManager.generateFolderIcon} drop-in delay. */
    private static final long GENERATE_FOLDER_ANIM_MS = 600L;

    private EditSelectionActions() {}

    /**
     * Oppo {@code BatchDragViewManager.generateFolderIcon}: place the folder on the current
     * workspace page, fly icons in, collapse leftover source folders, then return to the
     * edit options bar.
     *
     * @return true if a folder was created
     */
    public static boolean createFolder(Launcher launcher, Collection<ItemInfo> selectedItems,
            Iterable<View> selectedViews) {
        List<WorkspaceItemInfo> infos = new ArrayList<>();
        for (ItemInfo item : selectedItems) {
            if (item instanceof WorkspaceItemInfo app) {
                infos.add(app);
            }
        }
        if (infos.size() < 2) {
            Toast.makeText(launcher, R.string.edit_selection_need_two_apps, Toast.LENGTH_SHORT)
                    .show();
            return false;
        }

        Workspace workspace = launcher.getWorkspace();
        if (workspace == null || isWorkspaceScrolling(launcher, workspace)) {
            return false;
        }
        if (isSelectedSameFolderAllViews(launcher, infos)) {
            Toast.makeText(launcher, R.string.generate_failed_folder, Toast.LENGTH_SHORT).show();
            return false;
        }

        CellLayout current = (CellLayout) workspace.getChildAt(workspace.getNextPage());
        if (current == null) {
            Toast.makeText(launcher, R.string.colorrect_tips, Toast.LENGTH_SHORT).show();
            return false;
        }
        CellLayout pair = workspace.getPanelCount() > 1 ? workspace.getScreenPair(current) : null;

        int[] cell = new int[2];
        CellLayout target = current;
        WorkspaceItemInfo occupyInfo = promoteDesktopItemOnPage(infos,
                workspace.getIdForScreen(current));
        if (occupyInfo == null && pair != null) {
            occupyInfo = promoteDesktopItemOnPage(infos, workspace.getIdForScreen(pair));
            if (occupyInfo != null) {
                target = pair;
            }
        }
        View occupyView = occupyInfo != null ? findView(selectedViews, occupyInfo) : null;
        if (occupyInfo != null) {
            cell[0] = occupyInfo.cellX;
            cell[1] = occupyInfo.cellY;
        } else if (!target.findCellForSpan(cell, 1, 1)) {
            if (pair != null && pair != target && pair.findCellForSpan(cell, 1, 1)) {
                target = pair;
            } else if (current != target && current.findCellForSpan(cell, 1, 1)) {
                target = current;
            } else {
                Toast.makeText(launcher, R.string.colorrect_tips, Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        if (cell[0] < 0 || cell[1] < 0) {
            Toast.makeText(launcher, R.string.colorrect_tips, Toast.LENGTH_SHORT).show();
            return false;
        }

        int screenId = workspace.getIdForScreen(target);
        if (Workspace.EXTRA_EMPTY_SCREEN_IDS.contains(screenId)) {
            IntSet committed = workspace.commitExtraEmptyScreens();
            if (committed.isEmpty()) {
                Toast.makeText(launcher, R.string.colorrect_tips, Toast.LENGTH_SHORT).show();
                return false;
            }
            screenId = workspace.getIdForScreen(target);
        }

        List<FlyIn> flyIns = snapshotFlyIns(launcher, selectedViews, occupyInfo);

        AbstractFloatingView.closeOpenViews(launcher, true, AbstractFloatingView.TYPE_FOLDER);

        Map<FolderInfo, List<WorkspaceItemInfo>> fromFolders = new LinkedHashMap<>();
        for (WorkspaceItemInfo info : infos) {
            if (info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP
                    || info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
                View view = findWorkspaceView(workspace, info);
                if (view == null) {
                    view = findView(selectedViews, info);
                }
                if (view != null) {
                    workspace.removeWorkspaceItem(view);
                }
            } else {
                FolderInfo source = findFolderInfo(launcher, info.container);
                if (source != null) {
                    fromFolders.computeIfAbsent(source, k -> new ArrayList<>()).add(info);
                }
            }
        }
        for (Map.Entry<FolderInfo, List<WorkspaceItemInfo>> entry : fromFolders.entrySet()) {
            entry.getKey().removeAll(entry.getValue(), false);
        }

        FolderIcon folderIcon = launcher.addFolder(
                target,
                LauncherSettings.Favorites.CONTAINER_DESKTOP,
                screenId,
                cell[0],
                cell[1]);
        folderIcon.setEnabled(false);
        if (occupyView != null) {
            folderIcon.prepareCreateAnimation(occupyView);
        }
        FolderInfo folderInfo = (FolderInfo) folderIcon.getTag();
        for (WorkspaceItemInfo info : infos) {
            info.cellX = -1;
            info.cellY = -1;
            folderInfo.add(info, false);
        }
        folderIcon.invalidate();

        animateFlyIn(launcher, flyIns, folderIcon);
        folderIcon.postDelayed(() -> {
            folderIcon.setEnabled(true);
            workspace.reorderIfNeed();
        }, GENERATE_FOLDER_ANIM_MS);
        return true;
    }

    /**
     * Oppo {@code isSelectedSameFolderAllViews}: every selected app is already every item
     * in the same folder.
     */
    private static boolean isSelectedSameFolderAllViews(Launcher launcher,
            List<WorkspaceItemInfo> infos) {
        if (infos.isEmpty()) {
            return false;
        }
        int container = infos.get(0).container;
        if (container == LauncherSettings.Favorites.CONTAINER_DESKTOP
                || container == LauncherSettings.Favorites.CONTAINER_HOTSEAT
                || container == LauncherSettings.Favorites.CONTAINER_HOTSEAT_PREDICTION) {
            return false;
        }
        for (WorkspaceItemInfo info : infos) {
            if (info.container != container) {
                return false;
            }
        }
        FolderInfo folderInfo = findFolderInfo(launcher, container);
        return folderInfo != null && folderInfo.contents.size() == infos.size();
    }

    /**
     * Oppo {@code isCellLayoutHasSelectedView}: move a Home-screen icon on this page to the
     * front of the selection so the folder replaces that cell.
     */
    @Nullable
    private static WorkspaceItemInfo promoteDesktopItemOnPage(List<WorkspaceItemInfo> infos,
            int screenId) {
        for (int i = 0; i < infos.size(); i++) {
            WorkspaceItemInfo info = infos.get(i);
            if (info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP
                    && info.screenId == screenId) {
                if (i != 0) {
                    infos.remove(i);
                    infos.add(0, info);
                }
                return info;
            }
        }
        return null;
    }

    private static boolean isWorkspaceScrolling(Launcher launcher, Workspace workspace) {
        if (workspace.isPageInTransitionForEffect()) {
            return true;
        }
        int slop = ViewConfiguration.get(launcher).getScaledTouchSlop();
        return Math.abs(workspace.getScrollX()
                - workspace.getScrollForPage(workspace.getCurrentPage())) > slop;
    }

    @Nullable
    private static FolderInfo findFolderInfo(Launcher launcher, int folderId) {
        View folderIcon = launcher.getWorkspace().getHomescreenIconByItemId(folderId);
        if (folderIcon instanceof FolderIcon
                && folderIcon.getTag() instanceof FolderInfo info) {
            return info;
        }
        Folder open = Folder.getOpen(launcher);
        if (open != null && open.mInfo != null && open.mInfo.id == folderId) {
            return open.mInfo;
        }
        return null;
    }

    @Nullable
    private static View findView(Iterable<View> views, WorkspaceItemInfo info) {
        for (View view : views) {
            Object tag = view.getTag();
            if (tag == info || (tag instanceof ItemInfo item && sameItem(item, info))) {
                return view;
            }
        }
        return null;
    }

    @Nullable
    private static View findWorkspaceView(Workspace workspace, WorkspaceItemInfo info) {
        CellLayout layout = workspace.getScreenWithId(info.screenId);
        if (layout == null) {
            return null;
        }
        for (int i = 0; i < layout.getShortcutsAndWidgets().getChildCount(); i++) {
            View child = layout.getShortcutsAndWidgets().getChildAt(i);
            Object tag = child.getTag();
            if (tag == info || (tag instanceof ItemInfo item && sameItem(item, info))) {
                return child;
            }
        }
        return null;
    }

    private static boolean sameItem(ItemInfo a, ItemInfo b) {
        return a == b || (a.id != ItemInfo.NO_ID && a.id == b.id);
    }

    private static List<FlyIn> snapshotFlyIns(Launcher launcher, Iterable<View> selectedViews,
            @Nullable WorkspaceItemInfo occupyInfo) {
        List<FlyIn> out = new ArrayList<>();
        DragLayer dragLayer = launcher.getDragLayer();
        for (View view : selectedViews) {
            if (!(view.getTag() instanceof WorkspaceItemInfo info)) {
                continue;
            }
            if (occupyInfo != null && sameItem(info, occupyInfo)) {
                continue;
            }
            Bitmap bitmap = snapshotIcon(view);
            if (bitmap == null) {
                continue;
            }
            Rect from = new Rect();
            dragLayer.getDescendantRectRelativeToSelf(view, from);
            out.add(new FlyIn(bitmap, from));
        }
        return out;
    }

    @Nullable
    private static Bitmap snapshotIcon(View view) {
        if (view instanceof BubbleTextView btv && btv.getIcon() != null) {
            int size = btv.getIconSize();
            if (size <= 0) {
                size = view.getWidth();
            }
            if (size <= 0) {
                return null;
            }
            Drawable src = btv.getIcon();
            Drawable icon = src.getConstantState() != null
                    ? src.getConstantState().newDrawable().mutate()
                    : src.mutate();
            final int iconSize = size;
            // Use Picture recording: hardware icon bitmaps cannot be drawn onto a
            // software Canvas (ARGB_8888), which caused create-folder fly-in crashes.
            return snapshotWithPicture(iconSize, iconSize, canvas -> {
                icon.setBounds(0, 0, iconSize, iconSize);
                icon.draw(canvas);
            });
        }
        int width = view.getWidth();
        int height = view.getHeight();
        if (width <= 0 || height <= 0) {
            return null;
        }
        return snapshotWithPicture(width, height, view::draw);
    }

    /** Records drawing into a Picture so hardware bitmaps are safe to rasterize. */
    private static Bitmap snapshotWithPicture(int width, int height, Consumer<Canvas> drawer) {
        Picture picture = new Picture();
        Canvas canvas = picture.beginRecording(width, height);
        drawer.accept(canvas);
        picture.endRecording();
        return Bitmap.createBitmap(picture);
    }

    private static void animateFlyIn(Launcher launcher, List<FlyIn> flyIns, FolderIcon folderIcon) {
        if (flyIns.isEmpty()) {
            return;
        }
        DragLayer dragLayer = launcher.getDragLayer();
        Rect to = new Rect();
        dragLayer.getDescendantRectRelativeToSelf(folderIcon, to);
        for (FlyIn flyIn : flyIns) {
            ImageView ghost = new ImageView(launcher);
            ghost.setImageBitmap(flyIn.bitmap);
            ghost.setClickable(false);
            ghost.setFocusable(false);
            int width = flyIn.bitmap.getWidth();
            int height = flyIn.bitmap.getHeight();
            DragLayer.LayoutParams lp = new DragLayer.LayoutParams(width, height);
            lp.customPosition = true;
            lp.x = flyIn.from.centerX() - width / 2;
            lp.y = flyIn.from.centerY() - height / 2;
            dragLayer.addView(ghost, lp);
            float destX = to.centerX() - width / 2f;
            float destY = to.centerY() - height / 2f;
            ghost.post(() -> {
                if (ghost.getParent() == null) {
                    return;
                }
                ghost.animate()
                        .x(destX)
                        .y(destY)
                        .scaleX(0.2f)
                        .scaleY(0.2f)
                        .alpha(0f)
                        .setDuration(GENERATE_FOLDER_ANIM_MS)
                        .setInterpolator(Interpolators.DEACCEL_2)
                        .withEndAction(() -> dragLayer.removeView(ghost))
                        .start();
            });
        }
    }

    private static final class FlyIn {
        final Bitmap bitmap;
        final Rect from;

        FlyIn(Bitmap bitmap, Rect from) {
            this.bitmap = bitmap;
            this.from = from;
        }
    }

    public static void uninstallOrRemove(Launcher launcher, Iterable<View> selected) {
        List<View> snapshot = new ArrayList<>();
        for (View v : selected) {
            snapshot.add(v);
        }
        // Oppo PagePreviewButtonContainer.dealAppIcons(): drawer → removeApps().
        if (LauncherStyle.isAppDrawer(launcher)) {
            boolean removedAny = false;
            for (View v : snapshot) {
                Object tag = v.getTag();
                if (!(tag instanceof ItemInfo info)) {
                    continue;
                }
                if (!EditSelectionEligibility.canRemoveFromHome(info)) {
                    continue;
                }
                if (launcher.removeItem(v, info, true)) {
                    removedAny = true;
                }
            }
            if (!removedAny) {
                Toast.makeText(launcher, R.string.uninstall_system_app_text, Toast.LENGTH_SHORT)
                        .show();
            } else {
                stripEmptyWorkspaceScreens(launcher);
            }
            return;
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
        } else if (removedAny) {
            stripEmptyWorkspaceScreens(launcher);
        }
    }

    /**
     * Oppo {@code Workspace.stripEmptyScreens}: drop empty pages and snap to a neighbor.
     * At least one page is always kept.
     */
    public static void stripEmptyWorkspaceScreens(Launcher launcher) {
        Workspace workspace = launcher.getWorkspace();
        if (workspace == null) {
            return;
        }
        workspace.stripEmptyScreens();
        launcher.getEditSelectionManager().onWorkspacePageChanged();
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
