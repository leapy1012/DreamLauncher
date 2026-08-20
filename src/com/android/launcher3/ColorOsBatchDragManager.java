package com.android.launcher3;

import static com.android.launcher3.LauncherSettings.Favorites.CONTAINER_DESKTOP;
import static com.android.launcher3.LauncherSettings.Favorites.CONTAINER_HOTSEAT;

import android.graphics.Rect;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.launcher3.DropTarget.DragObject;
import android.util.Log;
import androidx.core.graphics.drawable.DrawableCompat;
import com.android.launcher3.folder.FolderGridOrganizer;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.android.launcher3.graphics.DragPreviewProvider;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.dragndrop.DragController;
import com.android.launcher3.dragndrop.DragView;
import com.android.launcher3.dragndrop.DragOptions;
import com.android.launcher3.views.BaseDragLayer;
import com.android.launcher3.views.ColorOsPagePreviewStrip;
import com.coui.appcompat.animation.dynamicanimation.COUIDynamicAnimation;
import com.coui.appcompat.animation.dynamicanimation.COUISpringAnimation;
import com.coui.appcompat.animation.dynamicanimation.COUISpringForce;
import com.coui.appcompat.contextutil.COUIContextUtil;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.net.URISyntaxException;

/**
 * ColorOS workspace multi-selection and batch-drop coordinator.
 *
 * <p>The decoded launcher keeps selection independently from View state, promotes the
 * long-clicked item to the head, removes all selected source cells while dragging, then completes
 * each item against the final drop. This class preserves those lifecycle rules while using this
 * launcher's existing Workspace drag transaction for the head item.</p>
 */
public final class ColorOsBatchDragManager implements DragController.DragListener {

    private static final WeakHashMap<Launcher, ColorOsBatchDragManager> INSTANCES =
            new WeakHashMap<>();
    private static final String TAG = "ColorOsBatchDrag";
    private static final long GATHER_DURATION_MS = 250L;
    private static final float GATHER_TRANSLATION_BOUNCE = 0.2f;
    private static final float GATHER_TRANSLATION_RESPONSE = 0.35f;
    private static final float GATHER_SCALE_RESPONSE = 0.45f;
    private static final float TAIL_BOUNCE = 0.05f;
    private static final float TAIL_RESPONSE = 0.05f;
    private static final float TAIL_DELTA_X_PX = 2f;
    // Decoded AbsLauncherMode.updateMaxFolderPages(): phones use nine pages.
    private static final int COLOR_OS_MAX_FOLDER_PAGES = 9;
    // PagePreviewButtonContainer waits for its folder-gather animation before clearing selection.
    private static final long FOLDER_CREATION_FINISH_DELAY_MS = 600L;

    private final Launcher mLauncher;
    private final LinkedHashSet<OplusBubbleTextView> mSelected = new LinkedHashSet<>();
    private final ArrayList<OriginalPosition> mActiveCompanions = new ArrayList<>();
    private final ArrayList<TailVisual> mTailVisuals = new ArrayList<>();
    private OplusBubbleTextView mActiveHead;
    private DragView<?> mActiveHeadDragView;
    private TextView mBatchCountView;
    private boolean mTailReady;
    private boolean mFolderCreationInProgress;
    private SelectionListener mSelectionListener;

    public interface SelectionListener {
        void onSelectionChanged(int count, boolean canCreateFolder, boolean canRemove,
                int removeLabelRes);
    }

    private ColorOsBatchDragManager(Launcher launcher) {
        mLauncher = launcher;
        launcher.getDragController().addDragListener(this);
    }

    public static synchronized ColorOsBatchDragManager get(Launcher launcher) {
        ColorOsBatchDragManager manager = INSTANCES.get(launcher);
        if (manager == null) {
            manager = new ColorOsBatchDragManager(launcher);
            INSTANCES.put(launcher, manager);
        }
        return manager;
    }

    /** Mirrors OPPO's UiConfig rows * columns selection limit. */
    public boolean canSelectAnother() {
        InvariantDeviceProfile idp = LauncherAppState.getIDP(mLauncher);
        return mSelected.size() < idp.numRows * idp.numColumns;
    }

    public void setSelectionListener(SelectionListener listener) {
        mSelectionListener = listener;
        notifySelectionChanged();
    }

    public int getSelectionCount() {
        pruneSelection();
        return mSelected.size();
    }

    public void onSelectionChanged(OplusBubbleTextView view, boolean selected) {
        if (selected) {
            if (mSelected.contains(view) || canSelectAnother()) {
                mSelected.add(view);
            }
        } else {
            mSelected.remove(view);
        }
        Log.d(TAG, "selection " + selected + " count=" + mSelected.size());
        notifySelectionChanged();
    }

    public void clearSelection() {
        ArrayList<OplusBubbleTextView> snapshot = new ArrayList<>(mSelected);
        mSelected.clear();
        for (OplusBubbleTextView view : snapshot) {
            view.setColorOsWorkspaceSelected(false, false);
        }
        notifySelectionChanged();
    }

    private void notifySelectionChanged() {
        if (mSelectionListener != null) {
            int uninstallableCount = getUninstallableCount();
            int removableShortcutCount = getRemovableShortcutCount();
            boolean canRemove = uninstallableCount > 0 || removableShortcutCount > 0;
            int removeLabelRes;
            if (uninstallableCount > 0 && removableShortcutCount > 0) {
                removeLabelRes = R.string.both_uninstall_and_remove_action;
            } else if (removableShortcutCount > 0) {
                removeLabelRes = R.string.remove_action;
            } else {
                removeLabelRes = R.string.uninstall_action;
            }
            mSelectionListener.onSelectionChanged(mSelected.size(),
                    mSelected.size() > 1 && !mFolderCreationInProgress,
                    canRemove && !mFolderCreationInProgress, removeLabelRes);
        }
    }

    public boolean generateFolderFromSelection() {
        pruneSelection();
        if (mFolderCreationInProgress || mSelected.size() < 2
                || mSelected.size() > getMaxFolderItems()) {
            return false;
        }
        ArrayList<OplusBubbleTextView> views = new ArrayList<>(mSelected);
        ArrayList<WorkspaceItemInfo> items = new ArrayList<>();
        for (OplusBubbleTextView view : views) {
            if (!(view.getTag() instanceof WorkspaceItemInfo) || view.getParent() == null) {
                return false;
            }
            WorkspaceItemInfo item = (WorkspaceItemInfo) view.getTag();
            if (item.container != CONTAINER_DESKTOP && item.container != CONTAINER_HOTSEAT) {
                return false;
            }
            items.add(item);
        }

        Workspace<?> workspace = mLauncher.getWorkspace();
        int currentScreenId = workspace.getScreenIdForPageIndex(workspace.getCurrentPage());
        WorkspaceItemInfo targetItem = null;
        for (WorkspaceItemInfo item : items) {
            if (item.container == CONTAINER_DESKTOP && item.screenId == currentScreenId) {
                targetItem = item;
                break;
            }
        }
        CellLayout layout = workspace.getScreenWithId(currentScreenId);
        if (layout == null) {
            return false;
        }

        int targetCellX;
        int targetCellY;
        if (targetItem != null) {
            targetCellX = targetItem.cellX;
            targetCellY = targetItem.cellY;
        } else {
            int[] vacantCell = new int[2];
            if (!layout.findCellForSpan(vacantCell, 1, 1)) {
                return false;
            }
            targetCellX = vacantCell[0];
            targetCellY = vacantCell[1];
        }

        mFolderCreationInProgress = true;
        notifySelectionChanged();

        for (OplusBubbleTextView view : views) {
            workspace.removeWorkspaceItem(view);
        }
        FolderIcon folder = mLauncher.addFolder(layout, CONTAINER_DESKTOP, currentScreenId,
                targetCellX, targetCellY);
        for (int i = 0; i < items.size(); i++) {
            WorkspaceItemInfo item = items.get(i);
            item.cellX = -1;
            item.cellY = -1;
            folder.mInfo.add(item, folder.mInfo.contents.size(), i == items.size() - 1);
        }
        folder.requestLayout();
        folder.invalidate();
        // OPPO keeps batch state until the 600 ms gather transaction has finished. Keeping the
        // state here also prevents a second folder/remove command from racing the model writes.
        folder.setAlpha(0f);
        folder.setScaleX(0.72f);
        folder.setScaleY(0.72f);
        folder.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(FOLDER_CREATION_FINISH_DELAY_MS).start();
        folder.postDelayed(() -> {
            mFolderCreationInProgress = false;
            clearSelection();
            folder.requestLayout();
            folder.invalidate();
        }, FOLDER_CREATION_FINISH_DELAY_MS);
        return true;
    }

    public boolean hasUninstallableSelection() {
        for (OplusBubbleTextView view : mSelected) {
            if (view.getTag() instanceof WorkspaceItemInfo
                    && getUninstallTarget((WorkspaceItemInfo) view.getTag()) != null) {
                return true;
            }
        }
        return false;
    }

    public int getUninstallableCount() {
        int count = 0;
        for (OplusBubbleTextView view : mSelected) {
            if (view.getTag() instanceof WorkspaceItemInfo
                    && getUninstallTarget((WorkspaceItemInfo) view.getTag()) != null) {
                count++;
            }
        }
        return count;
    }

    public int getRemovableShortcutCount() {
        int count = 0;
        for (OplusBubbleTextView view : mSelected) {
            if (view.getTag() instanceof WorkspaceItemInfo
                    && isRemovableShortcut((WorkspaceItemInfo) view.getTag())) {
                count++;
            }
        }
        return count;
    }

    public void removeOrUninstallSelectedItems() {
        ArrayList<OplusBubbleTextView> removableViews = new ArrayList<>();
        for (OplusBubbleTextView view : mSelected) {
            if (view.getTag() instanceof WorkspaceItemInfo
                    && isRemovableShortcut((WorkspaceItemInfo) view.getTag())) {
                removableViews.add(view);
            }
        }
        uninstallSelectedApps();
        clearSelection();
        for (OplusBubbleTextView view : removableViews) {
            if (view.getTag() instanceof WorkspaceItemInfo) {
                mLauncher.removeItem(view, (WorkspaceItemInfo) view.getTag(), true,
                        "ColorOS batch shortcut remove");
            }
        }
    }

    private boolean isRemovableShortcut(WorkspaceItemInfo item) {
        return item.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT
                || item.itemType == LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT;
    }

    public void uninstallSelectedApps() {
        ArrayList<WorkspaceItemInfo> items = new ArrayList<>();
        for (OplusBubbleTextView view : mSelected) {
            if (view.getTag() instanceof WorkspaceItemInfo) {
                WorkspaceItemInfo item = (WorkspaceItemInfo) view.getTag();
                if (getUninstallTarget(item) != null) {
                    items.add(item);
                }
            }
        }
        for (int i = items.size() - 1; i >= 0; i--) {
            WorkspaceItemInfo item = items.get(i);
            ComponentName component = getUninstallTarget(item);
            if (component == null) continue;
            try {
                Intent intent = Intent.parseUri(
                        mLauncher.getString(R.string.delete_package_intent), 0)
                        .setData(Uri.fromParts("package", component.getPackageName(),
                                component.getClassName()))
                        .putExtra(Intent.EXTRA_USER, item.user);
                mLauncher.startActivity(intent);
            } catch (URISyntaxException exception) {
                Log.e(TAG, "Unable to create uninstall intent", exception);
            }
        }
    }

    private ComponentName getUninstallTarget(WorkspaceItemInfo item) {
        if (item.itemType != LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
            return null;
        }
        UserManager userManager = mLauncher.getSystemService(UserManager.class);
        Bundle restrictions = userManager.getUserRestrictions(item.user);
        if (restrictions.getBoolean(UserManager.DISALLOW_APPS_CONTROL, false)
                || restrictions.getBoolean(UserManager.DISALLOW_UNINSTALL_APPS, false)) {
            return null;
        }
        LauncherApps launcherApps = mLauncher.getSystemService(LauncherApps.class);
        LauncherActivityInfo activityInfo = launcherApps.resolveActivity(item.getIntent(), item.user);
        if (activityInfo == null
                || (activityInfo.getApplicationInfo().flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
            return null;
        }
        return activityInfo.getComponentName();
    }

    /** Whether FolderIcon must validate an entire OPPO batch instead of only the head item. */
    public boolean isBatchDragActive() {
        return mActiveHead != null && !mActiveCompanions.isEmpty();
    }

    /**
     * Mirrors FlexibleFolderIcon.willAcceptBatchItems/isNotOverMaxItems: reject the whole batch
     * before the head item mutates FolderInfo. There is no partial folder insertion.
     */
    public boolean canDropIntoFolder(FolderIcon folderIcon) {
        if (!isBatchDragActive()) {
            return true;
        }
        int incomingCount = 1 + mActiveCompanions.size();
        return folderIcon != null && folderIcon.getFolder() != null
                && !folderIcon.getFolder().isOpen()
                && folderIcon.mInfo.contents.size() + incomingCount <= getMaxFolderItems();
    }

    /**
     * Promotes the long-clicked icon to the head and detaches companion views from their cells.
     * The database is deliberately untouched until the head drop has succeeded.
     */
    public DragPreviewProvider prepareBatchDrag(OplusBubbleTextView head) {
        pruneSelection();
        // The compatibility overlay may outlive an interrupted Launcher state transition. OPPO's
        // PAGE_PREVIEW is a stable edit state, so normalize before AOSP Workspace decides whether
        // this is a spring-loaded drag; otherwise cancel exits the entire edit surface.
        if (mLauncher.getDragLayer().findViewById(R.id.edit_mode_container) != null
                && !mLauncher.isInState(LauncherState.EDIT_MODE)) {
            mLauncher.getStateManager().goToState(LauncherState.EDIT_MODE, false);
        }
        if (!mSelected.contains(head)) {
            if (!canSelectAnother()) {
                return new DragPreviewProvider(head);
            }
            head.setColorOsWorkspaceSelected(true, false);
            mSelected.add(head);
        }
        if (mSelected.size() < 2) {
            return new DragPreviewProvider(head);
        }

        ArrayList<OplusBubbleTextView> ordered = new ArrayList<>();
        ordered.add(head);
        for (OplusBubbleTextView view : mSelected) {
            if (view != head) {
                ordered.add(view);
            }
        }
        mSelected.clear();
        mSelected.addAll(ordered);
        mActiveHead = head;
        mActiveCompanions.clear();

        for (int i = 1; i < ordered.size(); i++) {
            OplusBubbleTextView view = ordered.get(i);
            if (!(view.getTag() instanceof ItemInfo)) {
                continue;
            }
            ItemInfo info = (ItemInfo) view.getTag();
            if (info.container != CONTAINER_DESKTOP && info.container != CONTAINER_HOTSEAT) {
                continue;
            }
            DragSnapshot snapshot = createDragSnapshot(view);
            OriginalPosition original = new OriginalPosition(view, info.container, info.screenId,
                    info.cellX, info.cellY, info.spanX, info.spanY, snapshot);
            mActiveCompanions.add(original);
            // OPPO keeps every selected source view in its CellLayout and only hides it while
            // the corresponding live BatchDragView is moving.  Keeping the source attached is
            // also important for an ACTION_CANCEL: CellLayout never loses the selected item and
            // rollback is a visibility change rather than a fragile remove/add round trip.
            view.setVisibility(View.INVISIBLE);
        }
        Log.d(TAG, "start head=" + ((ItemInfo) head.getTag()).id
                + " companions=" + mActiveCompanions.size());
        return new ColorOsBatchDragPreviewProvider(head, ordered);
    }

    /** Completes or rolls back the companions after Workspace has completed the head item. */
    public void onHeadDropCompleted(View target, DragObject dragObject, boolean success) {
        if (mActiveHead == null) {
            // A one-item selected drag uses Workspace's normal DragView rather than a batch
            // preview, but OPPO still clears selection and returns PAGE_PREVIEW to TOGGLE_BAR
            // after a successful delivery.
            if (success && !mSelected.isEmpty()) {
                clearSelection();
            }
            return;
        }
        boolean placed = false;
        boolean previewDrop = target instanceof ColorOsPagePreviewStrip
                && ((ColorOsPagePreviewStrip) target).didDelegateLastDrop();
        if (success && (target == mLauncher.getWorkspace() || previewDrop)
                && mActiveHead.getTag() instanceof ItemInfo) {
            ItemInfo headInfo = (ItemInfo) mActiveHead.getTag();
            FolderIcon folderIcon = mLauncher.findFolderIcon(headInfo.container);
            placed = folderIcon != null
                    ? placeCompanionsInFolder(folderIcon)
                    : placeCompanions(headInfo);
        }
        if (!placed) {
            restoreCompanions(false);
            finishTailVisuals(false);
        } else {
            clearSelection();
            finishTailVisuals(true);
        }
        Log.d(TAG, "complete success=" + success + " workspaceTarget="
                + (target == mLauncher.getWorkspace()) + " placed=" + placed);
        mActiveCompanions.clear();
        mActiveHead = null;
    }

    @Override
    public void onDragStart(DragObject dragObject, DragOptions options) {
        if (mActiveHead != null && !mActiveCompanions.isEmpty()
                && dragObject != null && dragObject.dragView != null) {
            startBatchDragVisuals(dragObject.dragView);
        }
    }

    /** Updates OPPO's head-to-tail spring chain after DragView has consumed the motion event. */
    public void onDragMove() {
        if (mActiveHeadDragView == null || mTailVisuals.isEmpty()) {
            return;
        }
        View target = mActiveHeadDragView;
        for (int i = mTailVisuals.size() - 1; i >= 0; i--) {
            TailVisual visual = mTailVisuals.get(i);
            visual.animateTo(target.getTranslationX() + TAIL_DELTA_X_PX,
                    target.getTranslationY());
            target = visual.view;
        }
    }

    /** Restores a prepared batch before DragController tears down an interrupted drag. */
    public void onDragCancelled() {
        if (mActiveHead == null) return;
        restoreCompanions(true);
        clearTailVisualsImmediately();
        Log.d(TAG, "controller cancel rollback companions=" + mActiveCompanions.size());
        mActiveCompanions.clear();
        mActiveHead = null;
    }

    @Override
    public void onDragEnd() {
        // DragController announces global end before Workspace receives onDropCompleted.  OPPO's
        // BatchDragViewManager lets that source callback consume a successful batch first.  Defer
        // this safety rollback by one UI turn so it only handles paths which truly omitted the
        // source callback (activity interruption, detached source, and similar teardown).
        if (mActiveHead == null) return;
        mLauncher.getDragLayer().post(() -> {
            if (mActiveHead == null) return;
            restoreCompanions(true);
            clearTailVisualsImmediately();
            Log.d(TAG, "deferred drag-end rollback companions=" + mActiveCompanions.size());
            mActiveCompanions.clear();
            mActiveHead = null;
        });
    }

    private boolean placeCompanions(ItemInfo headInfo) {
        List<TargetPosition> targets = findTargets(headInfo, mActiveCompanions.size());
        if (targets.size() != mActiveCompanions.size()) {
            return false;
        }
        Workspace<?> workspace = mLauncher.getWorkspace();
        for (int i = 0; i < mActiveCompanions.size(); i++) {
            OriginalPosition original = mActiveCompanions.get(i);
            TargetPosition target = targets.get(i);
            ItemInfo info = (ItemInfo) original.view.getTag();
            original.view.setVisibility(View.INVISIBLE);
            workspace.removeWorkspaceItem(original.view);
            mLauncher.getModelWriter().moveItemInDatabase(info, target.container,
                    target.screenId, target.cellX, target.cellY);
            workspace.addInScreen(original.view, target.container, target.screenId,
                    target.cellX, target.cellY, original.spanX, original.spanY);
            target.layout.onDropChild(original.view);
        }
        return true;
    }

    /**
     * OPPO's FolderDropManager consumes its reversed drag-view list from tail to head, which
     * restores selection order with the promoted head first. The normal FolderIcon drop has
     * already inserted that head, so append companions in our retained selection order.
     */
    private boolean placeCompanionsInFolder(FolderIcon folderIcon) {
        ArrayList<WorkspaceItemInfo> items = new ArrayList<>();
        for (OriginalPosition original : mActiveCompanions) {
            if (!(original.view.getTag() instanceof WorkspaceItemInfo)) {
                return false;
            }
            items.add((WorkspaceItemInfo) original.view.getTag());
        }
        // The head is already present at this point; validate the remaining capacity atomically.
        if (folderIcon.mInfo.contents.size() + items.size() > getMaxFolderItems()) {
            return false;
        }
        Workspace<?> workspace = mLauncher.getWorkspace();
        for (OriginalPosition original : mActiveCompanions) {
            workspace.removeWorkspaceItem(original.view);
        }
        for (int i = 0; i < items.size(); i++) {
            WorkspaceItemInfo item = items.get(i);
            item.cellX = -1;
            item.cellY = -1;
            folderIcon.mInfo.add(item, folderIcon.mInfo.contents.size(), i == items.size() - 1);
        }
        folderIcon.invalidate();
        folderIcon.requestLayout();
        Log.d(TAG, "folder drop id=" + folderIcon.mInfo.id + " companions=" + items.size());
        return true;
    }

    private int getMaxFolderItems() {
        FolderGridOrganizer organizer = new FolderGridOrganizer(LauncherAppState.getIDP(mLauncher));
        return COLOR_OS_MAX_FOLDER_PAGES * organizer.getMaxItemsPerPage();
    }

    private List<TargetPosition> findTargets(ItemInfo headInfo, int count) {
        ArrayList<TargetPosition> result = new ArrayList<>();
        ArrayList<CellLayout> layouts = new ArrayList<>();
        ArrayList<Integer> screenIds = new ArrayList<>();
        Workspace<?> workspace = mLauncher.getWorkspace();

        if (headInfo.container == CONTAINER_HOTSEAT) {
            layouts.add(mLauncher.getHotseat());
            screenIds.add(0);
        } else if (headInfo.container == CONTAINER_DESKTOP) {
            CellLayout first = workspace.getScreenWithId(headInfo.screenId);
            if (first != null) {
                layouts.add(first);
                screenIds.add(headInfo.screenId);
            }
            for (int i = 0; i < workspace.getPageCount(); i++) {
                int screenId = workspace.getScreenIdForPageIndex(i);
                CellLayout layout = (CellLayout) workspace.getPageAt(i);
                if (layout != null && layout != first
                        && !WorkspaceLayoutManager.EXTRA_EMPTY_SCREEN_IDS.contains(screenId)) {
                    layouts.add(layout);
                    screenIds.add(screenId);
                }
            }
        }

        Set<String> reserved = new LinkedHashSet<>();
        // Workspace.onDrop can update the database before CellLayout's occupied grid is observed
        // here. OPPO's batch object carries the head placement separately, so companions can
        // never reuse its destination. Reserve that full span explicitly as the first layout.
        if (!layouts.isEmpty()) {
            CellLayout headLayout = layouts.get(0);
            for (int y = headInfo.cellY;
                    y < headInfo.cellY + Math.max(1, headInfo.spanY)
                            && y < headLayout.getCountY(); y++) {
                for (int x = headInfo.cellX;
                        x < headInfo.cellX + Math.max(1, headInfo.spanX)
                                && x < headLayout.getCountX(); x++) {
                    if (x >= 0 && y >= 0) {
                        reserved.add("0:" + x + ":" + y);
                    }
                }
            }
        }
        for (int l = 0; l < layouts.size() && result.size() < count; l++) {
            CellLayout layout = layouts.get(l);
            int screenId = screenIds.get(l);
            for (int y = 0; y < layout.getCountY() && result.size() < count; y++) {
                for (int x = 0; x < layout.getCountX() && result.size() < count; x++) {
                    String key = l + ":" + x + ":" + y;
                    if (!reserved.contains(key) && layout.isRegionVacant(x, y, 1, 1)) {
                        reserved.add(key);
                        result.add(new TargetPosition(layout, headInfo.container, screenId, x, y));
                    }
                }
            }
        }
        return result;
    }

    private void restoreCompanions(boolean visible) {
        Workspace<?> workspace = mLauncher.getWorkspace();
        for (OriginalPosition original : mActiveCompanions) {
            if (original.view.getParent() == null) {
                workspace.addInScreen(original.view, original.container, original.screenId,
                        original.cellX, original.cellY, original.spanX, original.spanY);
                CellLayout layout = original.container == CONTAINER_HOTSEAT
                        ? mLauncher.getHotseat() : workspace.getScreenWithId(original.screenId);
                if (layout != null) {
                    layout.onDropChild(original.view);
                }
            }
            original.view.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
            original.view.setColorOsWorkspaceSelected(true, false);
        }
    }

    private DragSnapshot createDragSnapshot(OplusBubbleTextView view) {
        boolean selected = view.isSelected();
        view.setColorOsWorkspaceSelected(false, false);
        try {
            DragPreviewProvider provider = new DragPreviewProvider(view);
            Drawable drawable = provider.createDrawable();
            int[] position = new int[2];
            float scale = provider.getScaleAndPosition(drawable, position);
            Rect visualBounds = new Rect();
            view.getSourceVisualDragBounds(visualBounds);
            return new DragSnapshot(drawable,
                    position[0] + Math.round(visualBounds.left * scale),
                    position[1] + Math.round(visualBounds.top * scale), scale);
        } finally {
            view.setColorOsWorkspaceSelected(selected, false);
        }
    }

    private void startBatchDragVisuals(DragView<?> headDragView) {
        clearTailVisualsImmediately();
        mActiveHeadDragView = headDragView;
        mTailReady = false;
        configureHeadShadow(headDragView);
        addBatchCount(headDragView, 1 + mActiveCompanions.size());

        int headIndex = mLauncher.getDragLayer().indexOfChild(headDragView);
        for (int i = 0; i < mActiveCompanions.size(); i++) {
            OriginalPosition original = mActiveCompanions.get(i);
            if (original.snapshot == null || original.snapshot.drawable == null) continue;
            ImageView tail = new ImageView(mLauncher);
            tail.setScaleType(ImageView.ScaleType.FIT_XY);
            tail.setImageDrawable(original.snapshot.drawable);
            tail.setTranslationX(original.snapshot.x);
            tail.setTranslationY(original.snapshot.y);
            tail.setScaleX(i == mActiveCompanions.size() - 1 ? 0.85f : 0.9f);
            tail.setScaleY(tail.getScaleX());
            tail.setRotation((i & 1) == 0 ? 2f : -2f);
            BaseDragLayer.LayoutParams lp = new BaseDragLayer.LayoutParams(
                    original.snapshot.drawable.getIntrinsicWidth(),
                    original.snapshot.drawable.getIntrinsicHeight());
            lp.customPosition = true;
            int insertAt = Math.max(0, headIndex);
            mLauncher.getDragLayer().addView(tail, insertAt, lp);
            headIndex++;
            TailVisual visual = new TailVisual(tail, original);
            visual.useGatherSprings();
            mTailVisuals.add(visual);
        }
        onDragMove();
        headDragView.postDelayed(() -> {
            if (headDragView != mActiveHeadDragView || mActiveHead == null) return;
            mTailReady = true;
            for (TailVisual visual : mTailVisuals) {
                visual.useTailSprings();
            }
            onDragMove();
            showBatchCount();
        }, GATHER_DURATION_MS);
    }

    private void configureHeadShadow(View head) {
        final float radius = dp(8f);
        if (head instanceof FrameLayout) {
            FrameLayout frame = (FrameLayout) head;
            frame.setClipChildren(false);
            frame.setClipToPadding(false);
        }
        head.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
        head.setElevation(dp(10f));
    }

    private void addBatchCount(DragView<?> head, int count) {
        if (count < 2) return;
        TextView badge = new TextView(mLauncher);
        badge.setText(Integer.toString(count));
        badge.setTextColor(0xffffffff);
        badge.setTextSize(TypedValue.COMPLEX_UNIT_PX, dp(10f));
        badge.setGravity(Gravity.CENTER);
        badge.setSingleLine(true);
        int horizontalPadding = Math.round(dp(5f));
        badge.setPadding(horizontalPadding, 0, horizontalPadding, 0);
        badge.setMinWidth(Math.round(dp(20f)));
        Drawable background = mLauncher.getDrawable(
                R.drawable.coloros_batch_drag_count_background).mutate();
        DrawableCompat.setTint(background, COUIContextUtil.getAttrColor(mLauncher,
                com.coui.appcompat.R.attr.couiColorPrimary, 0xff0066ff));
        badge.setBackground(background);
        badge.setAlpha(0f);
        badge.setScaleX(0f);
        badge.setScaleY(0f);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, Math.round(dp(20f)),
                Gravity.TOP | Gravity.END);
        lp.setMarginEnd(Math.round(dp(-7f)));
        lp.topMargin = Math.round(dp(-3f));
        head.addView(badge, lp);
        mBatchCountView = badge;
    }

    private void showBatchCount() {
        if (mBatchCountView == null) return;
        startSpring(mBatchCountView, COUIDynamicAnimation.ALPHA, 1f, 0f, 0.3f, 1f);
        startSpring(mBatchCountView, COUIDynamicAnimation.SCALE_X, 1f, 0f, 0.3f, 0.002f);
        startSpring(mBatchCountView, COUIDynamicAnimation.SCALE_Y, 1f, 0f, 0.3f, 0.002f);
    }

    private void finishTailVisuals(boolean placed) {
        DragView<?> head = mActiveHeadDragView;
        mActiveHeadDragView = null;
        mTailReady = false;
        for (TailVisual visual : new ArrayList<>(mTailVisuals)) {
            visual.cancelSprings();
            OriginalPosition original = visual.original;
            int[] target = getPreviewPosition(original.view, original.snapshot.drawable);
            boolean hasTarget = target != null && original.view.getParent() != null;
            if (!hasTarget && head != null) {
                target = new int[] {Math.round(head.getTranslationX()),
                        Math.round(head.getTranslationY())};
            }
            if (target == null) {
                removeTailVisual(visual, true);
                continue;
            }
            float finalScale = hasTarget ? original.snapshot.scale : 0.3f;
            COUISpringAnimation x = startSpring(visual.view,
                    COUIDynamicAnimation.TRANSLATION_X, target[0],
                    GATHER_TRANSLATION_BOUNCE, GATHER_TRANSLATION_RESPONSE, 1f);
            startSpring(visual.view, COUIDynamicAnimation.TRANSLATION_Y, target[1],
                    GATHER_TRANSLATION_BOUNCE, GATHER_TRANSLATION_RESPONSE, 1f);
            startSpring(visual.view, COUIDynamicAnimation.SCALE_X, finalScale,
                    0f, GATHER_SCALE_RESPONSE, 0.002f);
            startSpring(visual.view, COUIDynamicAnimation.SCALE_Y, finalScale,
                    0f, GATHER_SCALE_RESPONSE, 0.002f);
            if (!hasTarget) {
                startSpring(visual.view, COUIDynamicAnimation.ALPHA, 0f,
                        0f, 0.3f, 0.00390625f);
            }
            x.addEndListener((animation, canceled, value, velocity) ->
                    removeTailVisual(visual, hasTarget));
            // A spring already at its destination may complete synchronously before the listener
            // above is registered. OPPO always restores every source icon, so keep a bounded
            // completion fallback for that zero-distance case.
            visual.view.postDelayed(() -> removeTailVisual(visual, hasTarget), 600L);
        }
        mBatchCountView = null;
    }

    private int[] getPreviewPosition(View view, Drawable drawable) {
        if (view == null || drawable == null || view.getParent() == null) return null;
        DragPreviewProvider provider = new DragPreviewProvider(view);
        int[] position = new int[2];
        float scale = provider.getScaleAndPosition(drawable, position);
        Rect bounds = new Rect();
        if (view instanceof OplusBubbleTextView) {
            ((OplusBubbleTextView) view).getSourceVisualDragBounds(bounds);
        }
        position[0] += Math.round(bounds.left * scale);
        position[1] += Math.round(bounds.top * scale);
        return position;
    }

    private void removeTailVisual(TailVisual visual, boolean showOriginal) {
        visual.cancelSprings();
        if (visual.view.getParent() != null) {
            mLauncher.getDragLayer().removeView(visual.view);
        }
        if (showOriginal) {
            ensureOriginalAttachedAtCurrentPosition(visual.original);
            if (visual.original.view.getParent() != null) {
                visual.original.view.setVisibility(View.VISIBLE);
            }
        }
        mTailVisuals.remove(visual);
    }

    /**
     * Workspace's single-item drop cleanup may detach a companion that was reparented during the
     * same frame.  OPPO's tail animation resolves each BatchDragView back to the final source view;
     * reconcile that view from its already-updated ItemInfo before revealing it.
     */
    private void ensureOriginalAttachedAtCurrentPosition(OriginalPosition original) {
        if (original.view.getParent() != null
                || !(original.view.getTag() instanceof ItemInfo)) {
            return;
        }
        ItemInfo info = (ItemInfo) original.view.getTag();
        if (info.container != CONTAINER_DESKTOP && info.container != CONTAINER_HOTSEAT) {
            return;
        }
        Workspace<?> workspace = mLauncher.getWorkspace();
        workspace.addInScreen(original.view, info.container, info.screenId,
                info.cellX, info.cellY, info.spanX, info.spanY);
        CellLayout layout = info.container == CONTAINER_HOTSEAT
                ? mLauncher.getHotseat() : workspace.getScreenWithId(info.screenId);
        if (layout != null) {
            layout.onDropChild(original.view);
        }
    }

    private void clearTailVisualsImmediately() {
        for (TailVisual visual : new ArrayList<>(mTailVisuals)) {
            removeTailVisual(visual, false);
        }
        mTailVisuals.clear();
        mActiveHeadDragView = null;
        mBatchCountView = null;
        mTailReady = false;
    }

    private COUISpringAnimation startSpring(View view,
            COUIDynamicAnimation.ViewProperty property, float finalValue, float bounce,
            float response, float minimumVisibleChange) {
        COUISpringForce force = new COUISpringForce(finalValue)
                .setBounce(bounce).setResponse(response);
        COUISpringAnimation animation = new COUISpringAnimation(view, property, finalValue)
                .setSpring(force).setStartValue(property.getValue(view))
                .setMinimumVisibleChange(minimumVisibleChange);
        animation.start();
        return animation;
    }

    private float dp(float value) {
        return value * mLauncher.getResources().getDisplayMetrics().density;
    }

    private void pruneSelection() {
        mSelected.removeIf(view -> !(view.getTag() instanceof ItemInfo)
                || (view.getParent() == null && view != mActiveHead));
    }

    private static final class OriginalPosition {
        final OplusBubbleTextView view;
        final int container;
        final int screenId;
        final int cellX;
        final int cellY;
        final int spanX;
        final int spanY;
        final DragSnapshot snapshot;

        OriginalPosition(OplusBubbleTextView view, int container, int screenId, int cellX,
                int cellY, int spanX, int spanY, DragSnapshot snapshot) {
            this.view = view;
            this.container = container;
            this.screenId = screenId;
            this.cellX = cellX;
            this.cellY = cellY;
            this.spanX = spanX;
            this.spanY = spanY;
            this.snapshot = snapshot;
        }
    }

    private static final class DragSnapshot {
        final Drawable drawable;
        final int x;
        final int y;
        final float scale;

        DragSnapshot(Drawable drawable, int x, int y, float scale) {
            this.drawable = drawable;
            this.x = x;
            this.y = y;
            this.scale = scale;
        }
    }

    private final class TailVisual {
        final ImageView view;
        final OriginalPosition original;
        COUISpringAnimation xSpring;
        COUISpringAnimation ySpring;

        TailVisual(ImageView view, OriginalPosition original) {
            this.view = view;
            this.original = original;
        }

        void useGatherSprings() {
            replaceTranslationSprings(GATHER_TRANSLATION_BOUNCE,
                    GATHER_TRANSLATION_RESPONSE);
            startSpring(view, COUIDynamicAnimation.SCALE_X, 1f,
                    0f, GATHER_SCALE_RESPONSE, 0.002f);
            startSpring(view, COUIDynamicAnimation.SCALE_Y, 1f,
                    0f, GATHER_SCALE_RESPONSE, 0.002f);
        }

        void useTailSprings() {
            replaceTranslationSprings(TAIL_BOUNCE, TAIL_RESPONSE);
        }

        private void replaceTranslationSprings(float bounce, float response) {
            if (xSpring != null) xSpring.cancel();
            if (ySpring != null) ySpring.cancel();
            xSpring = startSpring(view, COUIDynamicAnimation.TRANSLATION_X,
                    view.getTranslationX(), bounce, response, 1f);
            ySpring = startSpring(view, COUIDynamicAnimation.TRANSLATION_Y,
                    view.getTranslationY(), bounce, response, 1f);
        }

        void animateTo(float x, float y) {
            if (xSpring != null) xSpring.animateToFinalPosition(x);
            if (ySpring != null) ySpring.animateToFinalPosition(y);
        }

        void cancelSprings() {
            if (xSpring != null) xSpring.cancel();
            if (ySpring != null) ySpring.cancel();
            xSpring = null;
            ySpring = null;
        }
    }

    private static final class TargetPosition {
        final CellLayout layout;
        final int container;
        final int screenId;
        final int cellX;
        final int cellY;

        TargetPosition(CellLayout layout, int container, int screenId, int cellX, int cellY) {
            this.layout = layout;
            this.container = container;
            this.screenId = screenId;
            this.cellX = cellX;
            this.cellY = cellY;
        }
    }
}
