package com.android.launcher3.editselection;

import static com.android.launcher3.folder.ClippedFolderIconLayoutRule.MAX_NUM_ITEMS_IN_PREVIEW;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
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
import android.view.ViewTreeObserver;
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
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.model.data.FolderInfo;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.android.launcher3.util.IntSet;

import com.coui.appcompat.animation.COUISpringInterpolator;

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

    /** Oppo {@code BatchDragViewManager.generateFolderIcon} / {@code FolderDropManager} drop. */
    private static final long GENERATE_FOLDER_ANIM_MS = 600L;
    /** Oppo {@code AnimationConstant.FOLDER_DROP} = COUISpringInterpolator(0.8, 0). */
    private static final COUISpringInterpolator FOLDER_DROP =
            new COUISpringInterpolator(0.8d, 0.0d);

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

        List<FlyIn> flyIns = snapshotFlyIns(launcher, infos, selectedViews);

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
        // Hide plate previews while ghosts fly in (Oppo hidePreviewItem during drop).
        int previewCount = Math.min(MAX_NUM_ITEMS_IN_PREVIEW, infos.size());
        for (int i = 0; i < previewCount; i++) {
            folderIcon.getPreviewItemManager().hidePreviewItem(i, true);
        }
        folderIcon.invalidate();

        animateFlyIn(launcher, flyIns, folderIcon, infos.size());
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
    private static View findView(Iterable<View> views, ItemInfo info) {
        for (View view : views) {
            Object tag = view.getTag();
            if (tag == info || (tag instanceof ItemInfo item && sameItem(item, info))) {
                return view;
            }
        }
        return null;
    }

    /**
     * Resolve a live icon for {@code info}. Folder contents may be unbound after the folder
     * closes — then return null and let {@link Launcher#removeItem} remove via FolderInfo.
     */
    @Nullable
    private static View resolveSelectedView(Launcher launcher, ItemInfo info,
            Iterable<View> selectedViews) {
        View view = findView(selectedViews, info);
        if (view != null) {
            return view;
        }
        Workspace workspace = launcher.getWorkspace();
        if (workspace != null && info instanceof WorkspaceItemInfo wi
                && (wi.container == LauncherSettings.Favorites.CONTAINER_DESKTOP
                || wi.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT)) {
            return findWorkspaceView(workspace, wi);
        }
        Folder open = Folder.getOpen(launcher);
        if (open != null) {
            for (View icon : open.getIconsInReadingOrder()) {
                if (icon == null) {
                    continue;
                }
                Object tag = icon.getTag();
                if (tag == info || (tag instanceof ItemInfo item && sameItem(item, info))) {
                    return icon;
                }
            }
        }
        return null;
    }

    private static boolean isInFolder(ItemInfo info) {
        return info.container != LauncherSettings.Favorites.CONTAINER_DESKTOP
                && info.container != LauncherSettings.Favorites.CONTAINER_HOTSEAT
                && info.container != LauncherSettings.Favorites.CONTAINER_HOTSEAT_PREDICTION;
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

    /**
     * Snapshot selected icons in folder-contents order (includes occupy cell).
     * Oppo {@code createAnimateViewsToFolder} keeps every selected drag view.
     */
    private static List<FlyIn> snapshotFlyIns(Launcher launcher, List<WorkspaceItemInfo> infos,
            Iterable<View> selectedViews) {
        List<FlyIn> out = new ArrayList<>();
        DragLayer dragLayer = launcher.getDragLayer();
        Workspace workspace = launcher.getWorkspace();
        for (int i = 0; i < infos.size(); i++) {
            WorkspaceItemInfo info = infos.get(i);
            View view = findView(selectedViews, info);
            if (view == null && workspace != null) {
                view = findWorkspaceView(workspace, info);
            }
            if (view == null) {
                continue;
            }
            Bitmap bitmap = snapshotIcon(view);
            if (bitmap == null) {
                continue;
            }
            Rect from = new Rect();
            dragLayer.getDescendantRectRelativeToSelf(view, from);
            out.add(new FlyIn(bitmap, from, i));
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

    /**
     * Oppo {@code FolderDropManager.onDrop} for generate-folder:
     * each icon flies to its preview-slot center/scale inside the new folder plate,
     * 600ms with {@code AnimationConstant.FOLDER_DROP} spring interpolator.
     */
    private static void animateFlyIn(Launcher launcher, List<FlyIn> flyIns, FolderIcon folderIcon,
            int totalItems) {
        if (flyIns.isEmpty()) {
            revealPreviewItems(folderIcon, totalItems);
            return;
        }
        ViewTreeObserver observer = folderIcon.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (observer.isAlive()) {
                    observer.removeOnPreDrawListener(this);
                }
                startPreviewSlotFlyIns(launcher, flyIns, folderIcon, totalItems);
                return true;
            }
        });
        folderIcon.invalidate();
    }

    private static void startPreviewSlotFlyIns(Launcher launcher, List<FlyIn> flyIns,
            FolderIcon folderIcon, int totalItems) {
        DragLayer dragLayer = launcher.getDragLayer();
        Rect folderBounds = new Rect();
        float scaleRel = dragLayer.getDescendantRectRelativeToSelf(folderIcon, folderBounds);
        int curNumItems = Math.min(MAX_NUM_ITEMS_IN_PREVIEW, totalItems);
        final int[] pending = {flyIns.size()};

        Runnable onOneEnded = () -> {
            pending[0]--;
            if (pending[0] <= 0) {
                revealPreviewItems(folderIcon, totalItems);
            }
        };

        int[] center = new int[2];
        for (FlyIn flyIn : flyIns) {
            ImageView ghost = new ImageView(launcher);
            ghost.setImageBitmap(flyIn.bitmap);
            ghost.setClickable(false);
            ghost.setFocusable(false);
            int width = flyIn.bitmap.getWidth();
            int height = flyIn.bitmap.getHeight();
            DragLayer.LayoutParams lp = new DragLayer.LayoutParams(width, height);
            lp.customPosition = true;
            float startX = flyIn.from.centerX() - width / 2f;
            float startY = flyIn.from.centerY() - height / 2f;
            lp.x = Math.round(startX);
            lp.y = Math.round(startY);
            dragLayer.addView(ghost, lp);
            ghost.setPivotX(width / 2f);
            ghost.setPivotY(height / 2f);
            ghost.setScaleX(1f);
            ghost.setScaleY(1f);
            ghost.setAlpha(1f);

            float previewScale = folderIcon.getPreviewItemCenter(
                    flyIn.index, curNumItems, center);
            int cx = Math.round(center[0] * scaleRel);
            int cy = Math.round(center[1] * scaleRel);
            float destX = folderBounds.left + cx - width / 2f;
            float destY = folderBounds.top + cy - height / 2f;
            float finalScale = previewScale * scaleRel;
            // Oppo: visible preview slots keep alpha 1; overflow fades out.
            float finalAlpha = flyIn.index < MAX_NUM_ITEMS_IN_PREVIEW ? 1f : 0f;

            ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
            anim.setDuration(GENERATE_FOLDER_ANIM_MS);
            anim.setInterpolator(FOLDER_DROP);
            anim.addUpdateListener(a -> {
                float t = (Float) a.getAnimatedValue();
                float u = 1f - t;
                ghost.setX(startX * u + destX * t);
                ghost.setY(startY * u + destY * t);
                float s = u + finalScale * t;
                ghost.setScaleX(s);
                ghost.setScaleY(s);
                ghost.setAlpha(u + finalAlpha * t);
            });
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (ghost.getParent() != null) {
                        dragLayer.removeView(ghost);
                    }
                    onOneEnded.run();
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    if (ghost.getParent() != null) {
                        dragLayer.removeView(ghost);
                    }
                    onOneEnded.run();
                }
            });
            anim.start();
        }
    }

    private static void revealPreviewItems(FolderIcon folderIcon, int totalItems) {
        int previewCount = Math.min(MAX_NUM_ITEMS_IN_PREVIEW, totalItems);
        for (int i = 0; i < previewCount; i++) {
            folderIcon.getPreviewItemManager().hidePreviewItem(i, false);
        }
        folderIcon.invalidate();
    }

    private static final class FlyIn {
        final Bitmap bitmap;
        final Rect from;
        final int index;

        FlyIn(Bitmap bitmap, Rect from, int index) {
            this.bitmap = bitmap;
            this.from = from;
            this.index = index;
        }
    }

    /**
     * Oppo {@code PagePreviewButtonContainer.dealAppIcons}:
     * drawer → remove from Home (including folder contents); regular → uninstall or remove.
     * <p>
     * Must use {@code selectedItems} (not only live views): folder icons unbind when the
     * folder closes while selection ItemInfos remain.
     */
    public static void uninstallOrRemove(Launcher launcher, Collection<ItemInfo> selectedItems,
            Iterable<View> selectedViews) {
        List<ItemInfo> items = new ArrayList<>(selectedItems);
        // Oppo PagePreviewButtonContainer.dealAppIcons(): drawer → removeApps().
        if (LauncherStyle.isAppDrawer(launcher)) {
            // Unbind open-folder content views; remove via FolderInfo + workspace FolderIcon.
            AbstractFloatingView.closeOpenViews(launcher, true, AbstractFloatingView.TYPE_FOLDER);
            boolean removedAny = false;
            for (ItemInfo info : items) {
                if (!EditSelectionEligibility.canRemoveFromHome(info)) {
                    continue;
                }
                View v = resolveSelectedView(launcher, info, selectedViews);
                // Folder contents: view may be null after close; removeItem still works.
                if (v == null && !isInFolder(info)) {
                    continue;
                }
                if (launcher.removeItem(v, info, true)) {
                    removedAny = true;
                }
            }
            // Do not misuse uninstall_system_app_text — drawer Remove is never package uninstall.
            if (removedAny) {
                stripEmptyWorkspaceScreens(launcher);
            }
            return;
        }

        boolean startedUninstall = false;
        boolean removedAny = false;
        boolean hadSystemOnlyFailure = false;

        for (ItemInfo info : items) {
            if (!EditSelectionEligibility.isUninstallOrRemoveEligible(launcher, info)) {
                continue;
            }

            ComponentName uninstallCn = getUninstallTarget(launcher, info);
            if (uninstallCn != null) {
                if (startUninstallActivity(launcher, uninstallCn, info)) {
                    startedUninstall = true;
                }
            } else if (EditSelectionEligibility.canRemoveFromHome(info)) {
                View v = resolveSelectedView(launcher, info, selectedViews);
                if (v == null && !isInFolder(info)) {
                    continue;
                }
                if (launcher.removeItem(v, info, true)) {
                    removedAny = true;
                }
            } else if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
                // Genuine system app — cannot uninstall and not a removable shortcut.
                hadSystemOnlyFailure = true;
            }
        }

        if (!startedUninstall && !removedAny && hadSystemOnlyFailure) {
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
