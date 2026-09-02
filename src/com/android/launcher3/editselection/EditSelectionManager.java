package com.android.launcher3.editselection;

import android.view.View;

import androidx.annotation.Nullable;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.ShortcutAndWidgetContainer;
import com.android.launcher3.Workspace;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.folder.large.listview.HxyLargeFolderIconItem;
import com.android.launcher3.model.data.FolderInfo;
import com.android.launcher3.model.data.ItemInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Oppo-style workspace edit selection: tracks selected icons and drives toolbar + checkmarks
 * + Create folder / Uninstall bottom bar.
 * <p>
 * Selection is keyed by {@link ItemInfo} so apps chosen inside a folder keep their state after
 * the folder closes (views are rebound). Folder icons themselves are not selectable — tap opens
 * the folder; a count badge shows how many contents are selected.
 */
public final class EditSelectionManager {

    private final Launcher mLauncher;
    /** Source of truth — survives folder open/close view recycle. */
    private final LinkedHashSet<ItemInfo> mSelectedItems = new LinkedHashSet<>();
    private boolean mActive;
    @Nullable
    private EditSelectionToolbar mToolbar;
    @Nullable
    private EditSelectionBottomBar mBottomBar;
    @Nullable
    private Listener mListener;

    public interface Listener {
        void onSelectionChanged(int count);
    }

    public EditSelectionManager(Launcher launcher) {
        mLauncher = launcher;
    }

    public void setListener(@Nullable Listener listener) {
        mListener = listener;
    }

    public boolean isActive() {
        return mActive;
    }

    public int getSelectedCount() {
        return mSelectedItems.size();
    }

    public Set<ItemInfo> getSelectedItems() {
        return Collections.unmodifiableSet(mSelectedItems);
    }

    /** Resolve current views for selected items (workspace + open folder). */
    public List<View> getSelectedViews() {
        List<View> out = new ArrayList<>();
        collectMatchingViews(out);
        return out;
    }

    public boolean isSelected(@Nullable View view) {
        if (view == null) {
            return false;
        }
        Object tag = view.getTag();
        return tag instanceof ItemInfo info && containsItem(info);
    }

    public boolean isSelected(@Nullable ItemInfo info) {
        return containsItem(info);
    }

    /** Oppo {@code getFolderSelectedViewCount}: selected apps that live in this folder. */
    public int getFolderSelectedCount(@Nullable FolderInfo folderInfo) {
        if (folderInfo == null || folderInfo.id == ItemInfo.NO_ID) {
            return 0;
        }
        int n = 0;
        for (ItemInfo info : mSelectedItems) {
            if (info.container == folderInfo.id) {
                n++;
            }
        }
        return n;
    }

    public int getFolderSelectedCount(@Nullable FolderIcon folderIcon) {
        if (folderIcon == null || !(folderIcon.getTag() instanceof FolderInfo info)) {
            return 0;
        }
        return getFolderSelectedCount(info);
    }

    /** Enter edit selection UI (checkmarks + top toolbar). */
    public void enter() {
        if (mActive) {
            return;
        }
        mActive = true;
        ensureChrome();
        if (mToolbar != null) {
            mToolbar.show();
            mToolbar.updateCount(0);
        }
        if (mBottomBar != null) {
            mBottomBar.hide();
        }
        invalidateWorkspaceIcons();
        notifyChanged();
    }

    /** Exit selection UI and clear selection. */
    public void exit() {
        if (!mActive && mSelectedItems.isEmpty()) {
            hideChrome();
            return;
        }
        mActive = false;
        mSelectedItems.clear();
        hideChrome();
        invalidateWorkspaceIcons();
        notifyChanged();
    }

    /** Clear selection but stay in edit mode (Oppo Cancel). */
    public void clearSelection() {
        if (mSelectedItems.isEmpty()) {
            return;
        }
        mSelectedItems.clear();
        if (mToolbar != null) {
            mToolbar.updateCount(0);
        }
        if (mBottomBar != null) {
            mBottomBar.hide();
        }
        invalidateWorkspaceIcons();
        notifyChanged();
    }

    /**
     * Toggle selection for a workspace / folder-content icon. Returns true if handled.
     * Folder icons themselves are not toggled (tap opens folder). Hotseat ignored.
     */
    public boolean toggle(View view) {
        if (!mActive || view == null) {
            return false;
        }
        // Preview cells inside large-folder plate — never select in place.
        if (view instanceof HxyLargeFolderIconItem) {
            return false;
        }
        if (view instanceof FolderIcon) {
            return false;
        }
        if (!EditSelectionEligibility.canToggle(mLauncher, view)) {
            return true; // consumed, but not selectable
        }
        if (!(view instanceof BubbleTextView)) {
            return false;
        }
        Object tag = view.getTag();
        if (!(tag instanceof ItemInfo info)) {
            return false;
        }
        if (containsItem(info)) {
            removeItem(info);
        } else {
            mSelectedItems.add(info);
        }
        view.invalidate();
        invalidateFolderIconForItem(info);
        updateChromeForCount();
        notifyChanged();
        return true;
    }

    private boolean containsItem(@Nullable ItemInfo info) {
        if (info == null) {
            return false;
        }
        for (ItemInfo selected : mSelectedItems) {
            if (sameItem(selected, info)) {
                return true;
            }
        }
        return false;
    }

    private void removeItem(ItemInfo info) {
        ItemInfo toRemove = null;
        for (ItemInfo selected : mSelectedItems) {
            if (sameItem(selected, info)) {
                toRemove = selected;
                break;
            }
        }
        if (toRemove != null) {
            mSelectedItems.remove(toRemove);
        }
    }

    private static boolean sameItem(ItemInfo a, ItemInfo b) {
        if (a == b) {
            return true;
        }
        if (a.id != ItemInfo.NO_ID && a.id == b.id) {
            return true;
        }
        return false;
    }

    private void invalidateFolderIconForItem(ItemInfo info) {
        if (info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP
                || info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT
                || info.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT_PREDICTION) {
            return;
        }
        FolderIcon icon = findFolderIcon(info.container);
        if (icon != null) {
            icon.invalidate();
        }
    }

    @Nullable
    private FolderIcon findFolderIcon(int folderId) {
        Workspace workspace = mLauncher.getWorkspace();
        if (workspace == null) {
            return null;
        }
        int count = workspace.getChildCount();
        for (int i = 0; i < count; i++) {
            if (!(workspace.getChildAt(i) instanceof CellLayout cell)) {
                continue;
            }
            ShortcutAndWidgetContainer container = cell.getShortcutsAndWidgets();
            if (container == null) {
                continue;
            }
            for (int j = 0; j < container.getChildCount(); j++) {
                View child = container.getChildAt(j);
                if (child instanceof FolderIcon fi
                        && child.getTag() instanceof FolderInfo folderInfo
                        && folderInfo.id == folderId) {
                    return fi;
                }
            }
        }
        return null;
    }

    private void updateChromeForCount() {
        int count = mSelectedItems.size();
        if (mToolbar != null) {
            mToolbar.updateCount(count);
            mToolbar.bringToFront();
        }
        if (mBottomBar != null) {
            mBottomBar.updateForSelectionCount(count, mSelectedItems);
            if (count > 0) {
                mBottomBar.bringToFront();
            }
        }
    }

    /** Keep page-preview highlight in sync when the workspace is swiped. */
    public void onWorkspacePageChanged() {
        if (!mActive || mBottomBar == null || mSelectedItems.isEmpty()) {
            return;
        }
        mBottomBar.syncCurrentPageHighlight();
    }

    private void ensureChrome() {
        if (mToolbar == null) {
            mToolbar = EditSelectionToolbar.attach(mLauncher);
        }
        if (mBottomBar == null) {
            mBottomBar = EditSelectionBottomBar.attach(mLauncher);
        }
        bindListeners();
    }

    private void bindListeners() {
        if (mToolbar != null) {
            mToolbar.setCancelClickListener(v -> clearSelection());
            mToolbar.setDoneClickListener(v -> {
                AbstractFloatingView.closeOpenViews(mLauncher, true,
                        AbstractFloatingView.TYPE_OPTIONS_POPUP_DIALOG);
                exit();
                mLauncher.getStateManager().goToState(com.android.launcher3.LauncherState.NORMAL);
            });
        }
        if (mBottomBar != null) {
            mBottomBar.setCreateFolderClickListener(v -> {
                EditSelectionActions.createFolder(mLauncher, getSelectedViews());
                clearSelection();
            });
            mBottomBar.setUninstallClickListener(v -> {
                EditSelectionActions.uninstallOrRemove(mLauncher, getSelectedViews());
                clearSelection();
            });
        }
    }

    private void hideChrome() {
        if (mToolbar != null) {
            mToolbar.hide();
        }
        if (mBottomBar != null) {
            mBottomBar.hide();
        }
    }

    private void notifyChanged() {
        if (mListener != null) {
            mListener.onSelectionChanged(mSelectedItems.size());
        }
    }

    private void collectMatchingViews(List<View> out) {
        Workspace workspace = mLauncher.getWorkspace();
        if (workspace != null) {
            int count = workspace.getChildCount();
            for (int i = 0; i < count; i++) {
                if (!(workspace.getChildAt(i) instanceof CellLayout cell)) {
                    continue;
                }
                ShortcutAndWidgetContainer container = cell.getShortcutsAndWidgets();
                if (container == null) {
                    continue;
                }
                for (int j = 0; j < container.getChildCount(); j++) {
                    View child = container.getChildAt(j);
                    if (child instanceof BubbleTextView && isSelected(child)
                            && !(child instanceof HxyLargeFolderIconItem)) {
                        out.add(child);
                    }
                }
            }
        }
        Folder openFolder = Folder.getOpen(mLauncher);
        if (openFolder != null) {
            for (View icon : openFolder.getIconsInReadingOrder()) {
                if (icon != null && isSelected(icon)) {
                    out.add(icon);
                }
            }
        }
    }

    public void invalidateWorkspaceIcons() {
        Workspace workspace = mLauncher.getWorkspace();
        if (workspace == null) {
            return;
        }
        int count = workspace.getChildCount();
        for (int i = 0; i < count; i++) {
            if (!(workspace.getChildAt(i) instanceof CellLayout cell)) {
                continue;
            }
            ShortcutAndWidgetContainer container = cell.getShortcutsAndWidgets();
            if (container == null) {
                continue;
            }
            container.setClipChildren(false);
            container.setClipToPadding(false);
            cell.setClipChildren(false);
            cell.setClipToPadding(false);
            int childCount = container.getChildCount();
            for (int j = 0; j < childCount; j++) {
                View child = container.getChildAt(j);
                if (child instanceof BubbleTextView || child instanceof FolderIcon) {
                    child.invalidate();
                }
            }
        }
        // Also refresh icons inside an open folder (Oppo: select apps in folder).
        Folder openFolder = Folder.getOpen(mLauncher);
        if (openFolder != null) {
            for (View icon : openFolder.getIconsInReadingOrder()) {
                if (icon != null) {
                    icon.invalidate();
                }
            }
        }
    }
}
