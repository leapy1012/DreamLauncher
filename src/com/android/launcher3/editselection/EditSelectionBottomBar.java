package com.android.launcher3.editselection;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

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
import java.util.HashSet;
import java.util.Set;

/**
 * Oppo page-preview bottom chrome while apps are selected:
 * page thumbnails + Create folder | Remove (drawer) or Uninstall (standard).
 * <p>
 * Highlight source of truth is workspace destination ({@link Workspace#getNextPage()}),
 * which equals {@link Workspace#getCurrentPage()} once settled. Selection toggles update
 * chip state in place — they must not rebuild the strip and clobber an in-flight snap.
 */
public class EditSelectionBottomBar extends FrameLayout {

    private LinearLayout mPageStrip;
    private View mCreateFolder;
    private View mUninstall;
    /** Last page with the selection stroke; matches workspace destination when in sync. */
    private int mHighlightedPage = -1;

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
        boolean canFolder = selectedCount >= 2;
        mCreateFolder.setEnabled(canFolder);
        mCreateFolder.setAlpha(canFolder ? 1f : 0.4f);
        updateRemoveButton(selectedItems);
        setVisibility(VISIBLE);
        setAlpha(1f);
        bringToFront();
        if (getParent() instanceof DragLayer dragLayer) {
            dragLayer.bringChildToFront(this);
        }
        setOptionsMenuVisible(false);
        scrollStripToHighlighted(false);
    }

    public void hide() {
        setVisibility(GONE);
        setPageIndicatorVisible(true);
        setOptionsMenuVisible(true);
        mHighlightedPage = -1;
    }

    /**
     * Selection-count updates: refresh chips/actions in place when the strip already matches
     * workspace page count. Full rebuild only when first shown or page count changed.
     */
    public void updateForSelectionCount(int count,
            @Nullable java.util.Collection<ItemInfo> selectedItems) {
        if (count <= 0) {
            hide();
            return;
        }
        if (!(getContext() instanceof Launcher launcher)) {
            return;
        }
        Workspace workspace = launcher.getWorkspace();
        boolean needRebuild = getVisibility() != VISIBLE
                || mPageStrip == null
                || mPageStrip.getChildCount() == 0
                || (workspace != null && mPageStrip.getChildCount() != workspace.getPageCount());
        if (needRebuild) {
            show(count, selectedItems);
            return;
        }

        Set<ItemInfo> selected = toSelectedSet(selectedItems);
        for (int i = 0; i < mPageStrip.getChildCount(); i++) {
            View child = mPageStrip.getChildAt(i);
            if (child instanceof EditSelectionPagePreviewView preview) {
                preview.setSelectedItems(selected);
            }
        }
        boolean canFolder = count >= 2;
        mCreateFolder.setEnabled(canFolder);
        mCreateFolder.setAlpha(canFolder ? 1f : 0.4f);
        updateRemoveButton(selectedItems);
        setPageIndicatorVisible(false);
        setOptionsMenuVisible(false);
        bringToFront();
        if (getParent() instanceof DragLayer dragLayer) {
            dragLayer.bringChildToFront(this);
        }
        // Re-assert destination highlight without clobbering an in-flight snap target.
        syncCurrentPageHighlight();
    }

    /**
     * Oppo {@code PagePreviewButtonContainer.updateRemoveBtnText}: drawer mode is always
     * {@code remove_action}; standard mode is Uninstall / Remove from the selection.
     */
    private void updateRemoveButton(@Nullable java.util.Collection<ItemInfo> selectedItems) {
        if (mUninstall instanceof TextView title) {
            title.setText(EditSelectionEligibility.removeButtonLabel(getContext(), selectedItems));
        }
        boolean enabled = EditSelectionEligibility.isRemoveButtonEnabled(
                getContext(), selectedItems);
        mUninstall.setEnabled(enabled);
        mUninstall.setAlpha(enabled ? 1f : 0.4f);
    }

    /**
     * Oppo {@code PagePreviewListContainer.onPageEndTransition}: update selection stroke
     * after the workspace page settles. Uses {@link Workspace#getNextPage()} so an in-flight
     * destination (click or fling) wins over a stale {@code getCurrentPage()}.
     */
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
        int page = resolveHighlightPage(workspace);
        if (page == mHighlightedPage) {
            return;
        }
        applyHighlight(page, true /* animate */);
        scrollStripToPage(page, true);
    }

    /** Used to detect empty-page strip / page-count changes without always rebuilding. */
    public int getPageStripChildCount() {
        return mPageStrip != null ? mPageStrip.getChildCount() : 0;
    }

    /**
     * Destination page while settling; equals current page once the scroller finishes.
     */
    private static int resolveHighlightPage(Workspace workspace) {
        return workspace.getNextPage();
    }

    private void applyHighlight(int page, boolean animate) {
        if (mPageStrip == null) {
            return;
        }
        mHighlightedPage = page;
        for (int c = 0; c < mPageStrip.getChildCount(); c++) {
            View child = mPageStrip.getChildAt(c);
            if (child instanceof EditSelectionPagePreviewView preview) {
                preview.setSelectedPage(c == page, animate);
            }
        }
    }

    private void scrollStripToHighlighted(boolean smooth) {
        if (mHighlightedPage >= 0) {
            scrollStripToPage(mHighlightedPage, smooth);
        }
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
        Set<ItemInfo> selected = toSelectedSet(selectedItems);
        // Prefer destination so a rebuild mid-snap does not snap the stroke back to the old page.
        int highlight = resolveHighlightPage(workspace);
        mPageStrip.removeAllViews();
        DeviceProfile dp = launcher.getDeviceProfile();
        int pageCount = workspace.getPageCount();
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
            thumb.bind(cell, dp.inv.numColumns, dp.inv.numRows, i == highlight, selected);
            final int pageIndex = i;
            thumb.setOnClickListener(v -> {
                // Oppo click → updateSelectedPos before snap settles.
                applyHighlight(pageIndex, true);
                scrollStripToPage(pageIndex, true);
                workspace.snapToPage(pageIndex);
            });
            mPageStrip.addView(thumb);
        }
        mHighlightedPage = highlight;
        final int scrollTo = highlight;
        mPageStrip.post(() -> scrollStripToPage(scrollTo, false));
    }

    private static Set<ItemInfo> toSelectedSet(
            @Nullable java.util.Collection<ItemInfo> selectedItems) {
        if (selectedItems == null) {
            return Collections.emptySet();
        }
        if (selectedItems instanceof Set) {
            return (Set<ItemInfo>) selectedItems;
        }
        return new HashSet<>(selectedItems);
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
