package com.android.launcher3.editselection;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.Workspace;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.views.OptionsDialogView;

import java.util.Collections;
import java.util.Set;

/**
 * Oppo page-preview bottom chrome while apps are selected:
 * page thumbnails + Create folder | Uninstall.
 */
public class EditSelectionBottomBar extends FrameLayout {

    private LinearLayout mPageStrip;
    private View mCreateFolder;
    private View mUninstall;
    @Nullable
    private ViewTreeObserver.OnScrollChangedListener mWorkspaceScrollListener;
    @Nullable
    private Workspace mListeningWorkspace;

    public EditSelectionBottomBar(Context context) {
        this(context, null);
    }

    public EditSelectionBottomBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EditSelectionBottomBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.edit_selection_bottom_bar, this, true);
        mPageStrip = findViewById(R.id.edit_selection_page_strip);
        mCreateFolder = findViewById(R.id.edit_selection_create_folder);
        mUninstall = findViewById(R.id.edit_selection_uninstall);
        setVisibility(GONE);
        setForceDarkAllowed(false);
        setClipChildren(false);
        setClipToPadding(false);
    }

    public static EditSelectionBottomBar attach(Launcher launcher) {
        DragLayer dragLayer = launcher.getDragLayer();
        View existing = dragLayer.findViewById(R.id.edit_selection_bottom_bar_root);
        if (existing instanceof EditSelectionBottomBar bar) {
            dragLayer.bringChildToFront(bar);
            bar.updateBottomInset();
            return bar;
        }
        EditSelectionBottomBar bar = new EditSelectionBottomBar(launcher);
        bar.setId(R.id.edit_selection_bottom_bar_root);
        DragLayer.LayoutParams lp = new DragLayer.LayoutParams(
                DragLayer.LayoutParams.MATCH_PARENT,
                DragLayer.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        lp.bottomMargin = launcher.getDeviceProfile().getInsets().bottom;
        dragLayer.addView(bar, lp);
        dragLayer.bringChildToFront(bar);
        return bar;
    }

    public void setCreateFolderClickListener(OnClickListener listener) {
        mCreateFolder.setOnClickListener(listener);
    }

    public void setUninstallClickListener(OnClickListener listener) {
        mUninstall.setOnClickListener(listener);
    }

    public void show(int selectedCount, @Nullable java.util.Collection<ItemInfo> selectedItems) {
        updateBottomInset();
        setPageIndicatorVisible(false);
        rebuildPageStrip(selectedItems);
        attachWorkspaceScrollListener();
        boolean canFolder = selectedCount >= 2;
        mCreateFolder.setEnabled(canFolder);
        mCreateFolder.setAlpha(canFolder ? 1f : 0.4f);
        // Oppo: enable when any selected item is uninstallable or a removable shortcut.
        boolean canUninstall = EditSelectionEligibility.isUninstallButtonEnabled(
                getContext(), selectedItems);
        mUninstall.setEnabled(canUninstall);
        mUninstall.setAlpha(canUninstall ? 1f : 0.4f);
        setVisibility(VISIBLE);
        setAlpha(1f);
        bringToFront();
        if (getParent() instanceof DragLayer dragLayer) {
            dragLayer.bringChildToFront(this);
        }
        setOptionsMenuVisible(false);
        syncCurrentPageHighlight();
    }

    public void hide() {
        detachWorkspaceScrollListener();
        setVisibility(GONE);
        setPageIndicatorVisible(true);
        setOptionsMenuVisible(true);
    }

    public void updateForSelectionCount(int count,
            @Nullable java.util.Collection<ItemInfo> selectedItems) {
        if (count <= 0) {
            hide();
            return;
        }
        show(count, selectedItems);
    }

    /** Highlight the preview matching the workspace's current/next page. */
    public void syncCurrentPageHighlight() {
        if (getVisibility() != VISIBLE || mPageStrip == null) {
            return;
        }
        if (!(getContext() instanceof Launcher launcher)) {
            return;
        }
        Workspace workspace = launcher.getWorkspace();
        if (workspace == null) {
            return;
        }
        int page = workspace.getNextPage();
        for (int c = 0; c < mPageStrip.getChildCount(); c++) {
            View child = mPageStrip.getChildAt(c);
            if (child instanceof EditSelectionPagePreviewView preview) {
                preview.setSelectedPage(c == page);
            }
        }
        scrollStripToPage(page, true);
    }

    private void attachWorkspaceScrollListener() {
        if (!(getContext() instanceof Launcher launcher)) {
            return;
        }
        Workspace workspace = launcher.getWorkspace();
        if (workspace == null) {
            return;
        }
        if (mListeningWorkspace == workspace && mWorkspaceScrollListener != null) {
            return;
        }
        detachWorkspaceScrollListener();
        mListeningWorkspace = workspace;
        mWorkspaceScrollListener = this::syncCurrentPageHighlight;
        workspace.getViewTreeObserver().addOnScrollChangedListener(mWorkspaceScrollListener);
    }

    private void detachWorkspaceScrollListener() {
        if (mListeningWorkspace != null && mWorkspaceScrollListener != null) {
            ViewTreeObserver observer = mListeningWorkspace.getViewTreeObserver();
            if (observer.isAlive()) {
                observer.removeOnScrollChangedListener(mWorkspaceScrollListener);
            }
        }
        mListeningWorkspace = null;
        mWorkspaceScrollListener = null;
    }

    @Override
    protected void onDetachedFromWindow() {
        detachWorkspaceScrollListener();
        super.onDetachedFromWindow();
    }

    private void setPageIndicatorVisible(boolean visible) {
        if (!(getContext() instanceof Launcher launcher)) {
            return;
        }
        View indicator = launcher.getWorkspace() != null
                ? launcher.getWorkspace().getPageIndicator()
                : null;
        if (indicator != null) {
            indicator.setVisibility(visible ? VISIBLE : INVISIBLE);
        }
    }

    private void setOptionsMenuVisible(boolean visible) {
        if (!(getContext() instanceof Launcher launcher)) {
            return;
        }
        OptionsDialogView options = AbstractFloatingView.getOpenView(
                launcher, AbstractFloatingView.TYPE_OPTIONS_POPUP_DIALOG);
        if (options != null) {
            options.setVisibility(visible ? VISIBLE : INVISIBLE);
        }
    }

    private void updateBottomInset() {
        if (!(getLayoutParams() instanceof DragLayer.LayoutParams lp)) {
            return;
        }
        if (!(getContext() instanceof Launcher launcher)) {
            return;
        }
        lp.bottomMargin = launcher.getDeviceProfile().getInsets().bottom;
        lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        lp.customPosition = false;
        requestLayout();
    }

    private void rebuildPageStrip(
            @Nullable java.util.Collection<ItemInfo> selectedItems) {
        if (!(getContext() instanceof Launcher launcher)) {
            return;
        }
        Workspace workspace = launcher.getWorkspace();
        if (workspace == null || mPageStrip == null) {
            return;
        }
        Set<ItemInfo> selected = selectedItems != null
                ? (selectedItems instanceof Set
                        ? (Set<ItemInfo>) selectedItems
                        : new java.util.HashSet<>(selectedItems))
                : Collections.emptySet();
        mPageStrip.removeAllViews();
        DeviceProfile dp = launcher.getDeviceProfile();
        int pageCount = workspace.getPageCount();
        int current = workspace.getCurrentPage();
        int width = getResources().getDimensionPixelSize(R.dimen.edit_selection_page_preview_width);
        int height = getResources().getDimensionPixelSize(R.dimen.edit_selection_page_preview_height);
        int gap = getResources().getDimensionPixelSize(R.dimen.edit_selection_page_preview_gap);

        for (int i = 0; i < pageCount; i++) {
            View page = workspace.getChildAt(i);
            CellLayout cell = page instanceof CellLayout ? (CellLayout) page : null;
            EditSelectionPagePreviewView thumb = new EditSelectionPagePreviewView(launcher);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, height);
            if (i < pageCount - 1) {
                lp.setMarginEnd(gap);
            }
            thumb.setLayoutParams(lp);
            thumb.bind(cell, dp.inv.numColumns, dp.inv.numRows, i == current, selected);
            final int pageIndex = i;
            thumb.setOnClickListener(v -> {
                workspace.snapToPage(pageIndex);
                for (int c = 0; c < mPageStrip.getChildCount(); c++) {
                    View child = mPageStrip.getChildAt(c);
                    if (child instanceof EditSelectionPagePreviewView preview) {
                        preview.setSelectedPage(c == pageIndex);
                    }
                }
                scrollStripToPage(pageIndex, true);
            });
            mPageStrip.addView(thumb);
        }
        mPageStrip.post(() -> scrollStripToPage(workspace.getNextPage(), false));
    }

    private void scrollStripToPage(int pageIndex, boolean smooth) {
        View scroll = findViewById(R.id.edit_selection_page_strip_scroll);
        if (!(scroll instanceof android.widget.HorizontalScrollView hsv)) {
            return;
        }
        // When the strip is wrap_content + centered, only scroll if content overflows.
        if (hsv.getLayoutParams() != null
                && hsv.getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT
                && mPageStrip.getMeasuredWidth() <= hsv.getRootView().getWidth()) {
            return;
        }
        if (pageIndex < 0 || pageIndex >= mPageStrip.getChildCount()) {
            return;
        }
        View thumb = mPageStrip.getChildAt(pageIndex);
        if (thumb == null) {
            return;
        }
        int target = thumb.getLeft() - (hsv.getWidth() - thumb.getWidth()) / 2;
        target = Math.max(0, target);
        if (smooth) {
            hsv.smoothScrollTo(target, 0);
        } else {
            hsv.scrollTo(target, 0);
        }
    }
}
