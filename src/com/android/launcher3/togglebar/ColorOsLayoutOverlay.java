package com.android.launcher3.togglebar;

import static com.android.launcher3.LauncherState.NORMAL;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.graphics.drawable.Drawable;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.Insettable;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.InvariantDeviceProfile.GridOption;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutAndWidgetContainer;
import com.android.launcher3.Workspace;
import com.android.launcher3.celllayout.CellLayoutLayoutParams;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.util.GridOccupancy;
import com.android.launcher3.util.LayoutLockHelper;
import com.android.launcher3.util.SystemUiController;
import com.android.launcher3.views.OptionsDialogView;
import com.android.launcher3.widget.LauncherAppWidgetHostView;
import com.coui.appcompat.couiswitch.COUISwitch;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Oppo ToggleBar Layout: live workspace preview plus a bottom sheet of 4×6 / 5×6 / 5×7
 * tiles and Hide icon names. Cancel | Layout | Apply lives on {@code EditSelectionToolbar}.
 */
public class ColorOsLayoutOverlay extends AbstractFloatingView implements Insettable {

    private static final int SHEET_EXTRA_BOTTOM_DP = 12;
    /** Oppo {@code LayoutSpringAnimationHelper} fade-out / fade-in. */
    private static final float PREVIEW_FADE_SCALE = 0.9f;
    private static final long PREVIEW_FADE_OUT_MS = 180;
    private static final long PREVIEW_FADE_IN_MS = 220;

    private Launcher mLauncher;
    private View mSheet;
    private LinearLayout mTiles;
    private COUISwitch mHideNames;
    private final List<GridOption> mOptions = new ArrayList<>();
    private int mSelectedIndex;
    private int mOriginalCols;
    private int mOriginalRows;
    private boolean mOriginalHideNames;
    private boolean mPreviewHideNames;
    private boolean mApplied;
    private boolean mRestoreNavContrast = true;
    private final List<SavedIcon> mSavedIcons = new ArrayList<>();
    private final List<SavedPage> mSavedPages = new ArrayList<>();
    @Nullable
    private Animator mPreviewAnimator;

    public ColorOsLayoutOverlay(Context context) {
        this(context, null);
    }

    public ColorOsLayoutOverlay(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorOsLayoutOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
        setClipChildren(false);
        setClipToPadding(false);
        setClickable(false);
        setFocusable(false);
        setFitsSystemWindows(false);
        setForceDarkAllowed(false);
        LayoutInflater.from(context).inflate(R.layout.coloros_layout_overlay, this, true);
    }

    public static void show(Launcher launcher) {
        if (LayoutLockHelper.checkLockedAndShowMessage(launcher)) {
            return;
        }
        closeOpenViews(launcher, false, TYPE_COLOROS_LAYOUT);
        Context themed = new ContextThemeWrapper(
                launcher, com.coui.appcompat.R.style.Theme_COUI_Blue);
        ColorOsLayoutOverlay overlay = new ColorOsLayoutOverlay(themed);
        overlay.mLauncher = launcher;
        overlay.mIsOpen = true;
        DragLayer.LayoutParams lp = new DragLayer.LayoutParams(
                DragLayer.LayoutParams.MATCH_PARENT,
                DragLayer.LayoutParams.MATCH_PARENT);
        lp.gravity = Gravity.FILL;
        lp.ignoreInsets = true;
        launcher.getDragLayer().addView(overlay, lp);
        overlay.bind();
    }

    private void bind() {
        mSheet = findViewById(R.id.coloros_layout_sheet);
        mTiles = findViewById(R.id.coloros_layout_tiles);
        mHideNames = findViewById(R.id.coloros_layout_hide_names);

        InvariantDeviceProfile idp = InvariantDeviceProfile.INSTANCE.get(mLauncher);
        mOriginalCols = idp.numColumns;
        mOriginalRows = idp.numRows;
        mOriginalHideNames = ColorOsLayoutSettings.isHideIconNames(mLauncher);
        mPreviewHideNames = mOriginalHideNames;
        ColorOsLayoutSettings.setPreviewHideNames(mPreviewHideNames);
        ColorOsLayoutSettings.applyToWorkspace(mLauncher);
        for (GridOption option : idp.parseAllGridOptions(mLauncher)) {
            if (option.numColumns <= 5) {
                mOptions.add(option);
            }
        }
        mSelectedIndex = indexOf(mOriginalCols, mOriginalRows);
        if (mSelectedIndex < 0 && !mOptions.isEmpty()) {
            mSelectedIndex = 0;
        }
        inflateTiles();
        mHideNames.setChecked(mPreviewHideNames);
        mSheet.setClickable(true);
        View hideRow = findViewById(R.id.coloros_layout_hide_names_row);
        hideRow.setOnClickListener(v -> mHideNames.toggle());
        mHideNames.setOnCheckedChangeListener((button, checked) -> {
            mPreviewHideNames = checked;
            ColorOsLayoutSettings.setPreviewHideNames(checked);
            ColorOsLayoutSettings.applyHideNames(mLauncher, checked);
        });
        applySheetInsets(mLauncher.getDeviceProfile().getInsets());
        setupNavBar();
        hideEditChrome();
        setWidgetsVisible(false);
        snapshotIcons();
    }

    private void inflateTiles() {
        mTiles.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (int i = 0; i < mOptions.size(); i++) {
            GridOption option = mOptions.get(i);
            View tile = inflater.inflate(R.layout.coloros_layout_grid_tile, mTiles, false);
            ColorOsLayoutGridPreview preview = tile.findViewById(R.id.coloros_layout_tile_preview);
            TextView label = tile.findViewById(R.id.coloros_layout_tile_label);
            preview.setColumnRow(option.numColumns, option.numRows);
            label.setText(option.numColumns + "×" + option.numRows);
            tile.setSelected(i == mSelectedIndex);
            final int index = i;
            tile.setOnClickListener(v -> selectGrid(index));
            mTiles.addView(tile);
        }
    }

    private void selectGrid(int index) {
        if (index < 0 || index >= mOptions.size() || index == mSelectedIndex) {
            return;
        }
        mSelectedIndex = index;
        for (int i = 0; i < mTiles.getChildCount(); i++) {
            mTiles.getChildAt(i).setSelected(i == mSelectedIndex);
        }
        GridOption option = mOptions.get(index);
        previewGrid(option.numColumns, option.numRows);
    }

    private void snapshotIcons() {
        mSavedIcons.clear();
        mSavedPages.clear();
        Workspace workspace = mLauncher.getWorkspace();
        if (workspace == null) {
            return;
        }
        for (int page = 0; page < workspace.getPageCount(); page++) {
            View pageView = workspace.getPageAt(page);
            if (!(pageView instanceof CellLayout cell)) {
                continue;
            }
            ViewGroup.LayoutParams lp = cell.getLayoutParams();
            mSavedPages.add(new SavedPage(cell, cell.getPaddingLeft(), cell.getPaddingTop(),
                    cell.getPaddingRight(), cell.getPaddingBottom(),
                    lp != null ? lp.height : ViewGroup.LayoutParams.MATCH_PARENT,
                    cell.getTranslationY()));
            ShortcutAndWidgetContainer container = cell.getShortcutsAndWidgets();
            if (container == null) {
                continue;
            }
            for (int i = 0; i < container.getChildCount(); i++) {
                View child = container.getChildAt(i);
                if (!(child.getLayoutParams() instanceof CellLayoutLayoutParams childLp)) {
                    continue;
                }
                boolean widget = child instanceof LauncherAppWidgetHostView;
                mSavedIcons.add(new SavedIcon(child, page, childLp.getCellX(), childLp.getCellY(),
                        childLp.cellHSpan, childLp.cellVSpan, widget));
            }
        }
        mSavedIcons.sort(Comparator.comparingInt((SavedIcon s) -> s.page)
                .thenComparingInt(s -> s.cellY)
                .thenComparingInt(s -> s.cellX));
    }

    private void previewGrid(int cols, int rows) {
        previewGrid(cols, rows, true);
    }

    private void previewGrid(int cols, int rows, boolean animate) {
        Workspace workspace = mLauncher.getWorkspace();
        if (workspace == null) {
            return;
        }
        CellLayout current = workspace.getPageAt(workspace.getCurrentPage()) instanceof CellLayout cell
                ? cell : null;
        cancelPreviewAnim();
        if (!animate || current == null) {
            applyPreviewGridNow(cols, rows);
            resetPageAnim(current);
            return;
        }
        current.setPivotX(workspace.getPivotX());
        current.setPivotY(Math.max(1, current.getHeight()) / 2f);
        AnimatorSet fadeOut = new AnimatorSet();
        fadeOut.playTogether(
                ObjectAnimator.ofFloat(current, View.ALPHA, current.getAlpha(), 0f),
                ObjectAnimator.ofFloat(current, View.SCALE_X, current.getScaleX(), PREVIEW_FADE_SCALE),
                ObjectAnimator.ofFloat(current, View.SCALE_Y, current.getScaleY(), PREVIEW_FADE_SCALE));
        fadeOut.setDuration(PREVIEW_FADE_OUT_MS);
        fadeOut.setInterpolator(Interpolators.DEACCEL);
        fadeOut.addListener(new AnimatorListenerAdapter() {
            boolean mCanceled;

            @Override
            public void onAnimationCancel(Animator animation) {
                mCanceled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (mCanceled) {
                    return;
                }
                applyPreviewGridNow(cols, rows);
                AnimatorSet fadeIn = new AnimatorSet();
                fadeIn.playTogether(
                        ObjectAnimator.ofFloat(current, View.ALPHA, 0f, 1f),
                        ObjectAnimator.ofFloat(current, View.SCALE_X, PREVIEW_FADE_SCALE, 1f),
                        ObjectAnimator.ofFloat(current, View.SCALE_Y, PREVIEW_FADE_SCALE, 1f));
                fadeIn.setDuration(PREVIEW_FADE_IN_MS);
                fadeIn.setInterpolator(Interpolators.ACCEL_DEACCEL);
                mPreviewAnimator = fadeIn;
                fadeIn.start();
            }
        });
        mPreviewAnimator = fadeOut;
        fadeOut.start();
    }

    /**
     * Oppo {@code PreviewGridChangedTask.changeLayout}: leftover horizontal padding,
     * CellLayout height + translationY recenter, {@code setGridCellSize}, then re-place
     * icons. Overflow icons are hidden (Oppo slides them off-screen).
     */
    private void applyPreviewGridNow(int cols, int rows) {
        Workspace workspace = mLauncher.getWorkspace();
        if (workspace == null) {
            return;
        }
        boolean restoreOriginal = cols == mOriginalCols && rows == mOriginalRows;
        DeviceProfile dp = mLauncher.getDeviceProfile();
        Point cellSize = dp.getOppoPreviewCellSize(cols, rows);
        int iconSize = dp.getOppoPreviewIconSizePx(cols);
        for (int page = 0; page < workspace.getPageCount(); page++) {
            View pageView = workspace.getPageAt(page);
            if (!(pageView instanceof CellLayout cell)) {
                continue;
            }
            if (restoreOriginal) {
                restorePageChrome(cell);
                cell.resetCellSize(dp);
                cell.setGridSize(cols, rows);
                restorePageIcons(page);
            } else {
                applyOppoCellLayoutPadding(dp, cell, workspace, cellSize, cols, rows);
                cell.setGridSize(cols, rows);
                cell.setCellDimensions(Math.max(1, cellSize.x), Math.max(1, cellSize.y));
                placePageIcons(page, cols, rows, iconSize);
            }
            cell.requestLayout();
        }
        workspace.requestLayout();
        workspace.invalidate();
    }

    /**
     * Oppo {@code setNewCellLayoutPadding}: padHor is leftover after {@code cols * cellW},
     * height is {@code padTop + padBottom + rows * cellH}, translationY recenters in
     * {@code availableHeightPx}.
     */
    private void applyOppoCellLayoutPadding(DeviceProfile dp, CellLayout cell,
            Workspace workspace, Point cellSize, int cols, int rows) {
        SavedPage saved = findSavedPage(cell);
        int padTop = saved != null ? saved.padT : cell.getPaddingTop();
        int padBottom = saved != null ? saved.padB : cell.getPaddingBottom();
        int padHor = Math.max(0, ((dp.widthPx - (cellSize.x * cols)) / 2)
                - workspace.getPaddingLeft());
        int height = padTop + padBottom + (cellSize.y * rows);
        float translationY = -((dp.availableHeightPx - height
                - workspace.getPaddingTop() - workspace.getPaddingBottom()) / 2f);
        ViewGroup.LayoutParams lp = cell.getLayoutParams();
        if (lp != null) {
            lp.height = height;
            cell.setLayoutParams(lp);
        }
        cell.setTranslationY(translationY);
        cell.setPadding(padHor, padTop, padHor, padBottom);
    }

    private void cancelPreviewAnim() {
        if (mPreviewAnimator != null) {
            mPreviewAnimator.removeAllListeners();
            mPreviewAnimator.cancel();
            mPreviewAnimator = null;
        }
    }

    private static void resetPageAnim(@Nullable CellLayout cell) {
        if (cell == null) {
            return;
        }
        cell.animate().cancel();
        cell.setAlpha(1f);
        cell.setScaleX(1f);
        cell.setScaleY(1f);
    }

    @Nullable
    private SavedPage findSavedPage(CellLayout cell) {
        for (SavedPage saved : mSavedPages) {
            if (saved.cell == cell) {
                return saved;
            }
        }
        return null;
    }

    private void restorePageChrome(CellLayout cell) {
        SavedPage saved = findSavedPage(cell);
        if (saved == null) {
            cell.setTranslationY(0f);
            resetPageAnim(cell);
            return;
        }
        cell.setPadding(saved.padL, saved.padT, saved.padR, saved.padB);
        ViewGroup.LayoutParams lp = cell.getLayoutParams();
        if (lp != null) {
            lp.height = saved.height;
            cell.setLayoutParams(lp);
        }
        cell.setTranslationY(saved.translationY);
        resetPageAnim(cell);
    }

    private void restorePageIcons(int page) {
        for (SavedIcon saved : mSavedIcons) {
            if (saved.page != page) {
                continue;
            }
            if (saved.view.getLayoutParams() instanceof CellLayoutLayoutParams lp) {
                lp.setCellX(saved.cellX);
                lp.setCellY(saved.cellY);
                lp.cellHSpan = saved.spanX;
                lp.cellVSpan = saved.spanY;
                lp.useTmpCoords = false;
            }
            saved.view.setScaleX(1f);
            saved.view.setScaleY(1f);
            if (saved.widget) {
                saved.view.setVisibility(INVISIBLE);
            } else {
                saved.view.setVisibility(VISIBLE);
                applyPreviewIconSize(saved.view, 0);
            }
        }
    }

    /**
     * Oppo {@code arrangeItemsOnScreen}: widgets keep occupancy while hidden; icons
     * pack into vacant cells (original grid restore uses {@link #restorePageIcons}).
     */
    private void placePageIcons(int page, int cols, int rows, int iconSizePx) {
        GridOccupancy occupancy = new GridOccupancy(cols, rows);
        int[] vacant = new int[2];
        for (SavedIcon saved : mSavedIcons) {
            if (saved.page != page || !saved.widget) {
                continue;
            }
            markOrHide(saved, occupancy, vacant, cols, rows);
        }
        for (SavedIcon saved : mSavedIcons) {
            if (saved.page != page || saved.widget) {
                continue;
            }
            int destX;
            int destY;
            int spanX = Math.max(1, Math.min(saved.spanX, cols));
            int spanY = Math.max(1, Math.min(saved.spanY, rows));
            if (!occupancy.findVacantCell(vacant, spanX, spanY)) {
                saved.view.setVisibility(INVISIBLE);
                continue;
            }
            destX = vacant[0];
            destY = vacant[1];
            occupancy.markCells(destX, destY, spanX, spanY, true);
            if (saved.view.getLayoutParams() instanceof CellLayoutLayoutParams lp) {
                lp.setCellX(destX);
                lp.setCellY(destY);
                lp.cellHSpan = spanX;
                lp.cellVSpan = spanY;
                lp.useTmpCoords = false;
            }
            saved.view.setScaleX(1f);
            saved.view.setScaleY(1f);
            saved.view.setVisibility(VISIBLE);
            applyPreviewIconSize(saved.view, iconSizePx);
        }
    }

    private static void markOrHide(SavedIcon saved, GridOccupancy occupancy, int[] vacant,
            int cols, int rows) {
        int spanX = Math.max(1, Math.min(saved.spanX, cols));
        int spanY = Math.max(1, Math.min(saved.spanY, rows));
        int destX;
        int destY;
        if (occupancy.isRegionVacant(saved.cellX, saved.cellY, spanX, spanY)) {
            destX = saved.cellX;
            destY = saved.cellY;
        } else if (occupancy.findVacantCell(vacant, spanX, spanY)) {
            destX = vacant[0];
            destY = vacant[1];
        } else {
            return;
        }
        occupancy.markCells(destX, destY, spanX, spanY, true);
        if (saved.view.getLayoutParams() instanceof CellLayoutLayoutParams lp) {
            lp.setCellX(destX);
            lp.setCellY(destY);
            lp.cellHSpan = spanX;
            lp.cellVSpan = spanY;
            lp.useTmpCoords = false;
        }
    }

    /**
     * Oppo {@code getIconSizeTmp}: change the drawable bounds, not the whole view
     * scale (that also shrinks the label).
     */
    private static void applyPreviewIconSize(View view, int iconSizePx) {
        if (!(view instanceof BubbleTextView icon)) {
            return;
        }
        Drawable drawable = icon.getIcon();
        if (drawable == null) {
            return;
        }
        int size = iconSizePx > 0 ? iconSizePx : icon.getIconSize();
        drawable.setBounds(0, 0, size, size);
        icon.invalidate();
    }

    private static final class SavedIcon {
        final View view;
        final int page;
        final int cellX;
        final int cellY;
        final int spanX;
        final int spanY;
        final boolean widget;

        SavedIcon(View view, int page, int cellX, int cellY, int spanX, int spanY, boolean widget) {
            this.view = view;
            this.page = page;
            this.cellX = cellX;
            this.cellY = cellY;
            this.spanX = spanX;
            this.spanY = spanY;
            this.widget = widget;
        }
    }

    private static final class SavedPage {
        final CellLayout cell;
        final int padL;
        final int padT;
        final int padR;
        final int padB;
        final int height;
        final float translationY;

        SavedPage(CellLayout cell, int padL, int padT, int padR, int padB,
                int height, float translationY) {
            this.cell = cell;
            this.padL = padL;
            this.padT = padT;
            this.padR = padR;
            this.padB = padB;
            this.height = height;
            this.translationY = translationY;
        }
    }

    private void applyAndExit() {
        GridOption selected = currentOption();
        boolean gridChanged = selected != null
                && (selected.numColumns != mOriginalCols || selected.numRows != mOriginalRows);
        boolean namesChanged = mPreviewHideNames != mOriginalHideNames;
        if (!gridChanged && !namesChanged) {
            close(true);
            return;
        }
        mApplied = true;
        cancelPreviewAnim();
        applyPreviewGridNow(mOriginalCols, mOriginalRows);
        ColorOsLayoutSettings.setHideIconNames(mLauncher, mPreviewHideNames);
        ColorOsLayoutSettings.setPreviewHideNames(null);
        if (gridChanged) {
            InvariantDeviceProfile.INSTANCE.get(mLauncher)
                    .setCurrentGrid(mLauncher, selected.name);
        } else {
            ColorOsLayoutSettings.applyToWorkspace(mLauncher);
        }
        OptionsDialogView options = AbstractFloatingView.getOpenView(
                mLauncher, TYPE_OPTIONS_POPUP_DIALOG);
        if (options != null) {
            options.close(false);
        } else {
            mLauncher.getEditSelectionManager().exit();
            mLauncher.getStateManager().goToState(NORMAL);
        }
        close(false);
    }

    @Override
    protected void handleClose(boolean animate) {
        if (!mIsOpen) {
            return;
        }
        mIsOpen = false;
        cancelPreviewAnim();
        ColorOsLayoutSettings.setPreviewHideNames(null);
        if (!mApplied) {
            previewGrid(mOriginalCols, mOriginalRows, false);
            ColorOsLayoutSettings.applyToWorkspace(mLauncher);
        }
        restoreEditChrome();
        setWidgetsVisible(true);
        clearNavBar();
        if (getParent() instanceof ViewGroup parent) {
            parent.removeView(this);
        }
    }

    @Override
    public void setInsets(Rect insets) {
        applySheetInsets(insets);
    }

    private void applySheetInsets(Rect insets) {
        if (mSheet == null || insets == null) {
            return;
        }
        int extra = Math.round(SHEET_EXTRA_BOTTOM_DP
                * getResources().getDisplayMetrics().density);
        mSheet.setPadding(mSheet.getPaddingLeft(), mSheet.getPaddingTop(),
                mSheet.getPaddingRight(), insets.bottom + extra);
    }

    @Override
    protected boolean isOfType(int type) {
        return (type & TYPE_COLOROS_LAYOUT) != 0;
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        // Returning true makes DragLayer steal the gesture and cancel our children,
        // so tiles / the switch never receive ACTION_UP.
        return false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (isTouchOnChrome(ev)) {
            return super.dispatchTouchEvent(ev);
        }
        if (isTouchOnToolbarButtons(ev)) {
            return false;
        }
        // Empty workspace: same as Cancel — restore preview and stay in edit mode.
        // Consume so WorkspaceTouchListener does not goToState(NORMAL).
        if (ev.getActionMasked() == MotionEvent.ACTION_UP) {
            cancelToEditMode();
        }
        return true;
    }

    private boolean isTouchOnChrome(MotionEvent ev) {
        return isInView(mSheet, ev);
    }

    private boolean isTouchOnToolbarButtons(MotionEvent ev) {
        if (mLauncher == null || mLauncher.getDragLayer() == null) {
            return false;
        }
        View cancel = mLauncher.getDragLayer().findViewById(R.id.edit_selection_cancel);
        View apply = mLauncher.getDragLayer().findViewById(R.id.edit_selection_done);
        return isInView(cancel, ev) || isInView(apply, ev);
    }

    private static boolean isInView(@Nullable View view, MotionEvent ev) {
        if (view == null || view.getVisibility() != VISIBLE) {
            return false;
        }
        int[] loc = new int[2];
        view.getLocationOnScreen(loc);
        float x = ev.getRawX();
        float y = ev.getRawY();
        return x >= loc[0] && x < loc[0] + view.getWidth()
                && y >= loc[1] && y < loc[1] + view.getHeight();
    }

    private void hideEditChrome() {
        OptionsDialogView options = AbstractFloatingView.getOpenView(
                mLauncher, TYPE_OPTIONS_POPUP_DIALOG);
        if (options != null) {
            options.setVisibility(INVISIBLE);
        }
        mLauncher.getEditSelectionManager().showLayoutChrome(
                v -> cancelToEditMode(), v -> applyAndExit());
    }

    /** Dismiss Layout and stay in workspace edit mode (Oppo Cancel / back / tap-outside). */
    private void cancelToEditMode() {
        close(true);
    }

    @Override
    public void onBackInvoked() {
        cancelToEditMode();
    }

    private void restoreEditChrome() {
        OptionsDialogView options = AbstractFloatingView.getOpenView(
                mLauncher, TYPE_OPTIONS_POPUP_DIALOG);
        if (options != null) {
            options.setVisibility(VISIBLE);
        }
        mLauncher.getEditSelectionManager().restoreChromeAfterLayout();
    }

    private void setupNavBar() {
        Window window = mLauncher.getWindow();
        if (window != null) {
            window.setNavigationBarColor(Color.TRANSPARENT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mRestoreNavContrast = window.isNavigationBarContrastEnforced();
                window.setNavigationBarContrastEnforced(false);
            }
        }
        mLauncher.getSystemUiController().updateUiState(
                SystemUiController.UI_STATE_WIDGET_BOTTOM_SHEET,
                SystemUiController.FLAG_LIGHT_NAV);
    }

    private void clearNavBar() {
        if (mLauncher.getWindow() != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mLauncher.getWindow().setNavigationBarContrastEnforced(mRestoreNavContrast);
        }
        mLauncher.getSystemUiController().updateUiState(
                SystemUiController.UI_STATE_WIDGET_BOTTOM_SHEET, 0);
    }

    private void setWidgetsVisible(boolean visible) {
        Workspace workspace = mLauncher.getWorkspace();
        if (workspace == null) {
            return;
        }
        for (int i = 0; i < workspace.getPageCount(); i++) {
            View page = workspace.getChildAt(i);
            if (!(page instanceof CellLayout cell)) {
                continue;
            }
            ShortcutAndWidgetContainer container = cell.getShortcutsAndWidgets();
            if (container == null) {
                continue;
            }
            for (int c = 0; c < container.getChildCount(); c++) {
                View child = container.getChildAt(c);
                if (child instanceof LauncherAppWidgetHostView) {
                    child.setVisibility(visible ? VISIBLE : INVISIBLE);
                }
            }
        }
    }

    @Nullable
    private GridOption currentOption() {
        if (mSelectedIndex < 0 || mSelectedIndex >= mOptions.size()) {
            return null;
        }
        return mOptions.get(mSelectedIndex);
    }

    private int indexOf(int cols, int rows) {
        for (int i = 0; i < mOptions.size(); i++) {
            GridOption option = mOptions.get(i);
            if (option.numColumns == cols && option.numRows == rows) {
                return i;
            }
        }
        return -1;
    }
}
