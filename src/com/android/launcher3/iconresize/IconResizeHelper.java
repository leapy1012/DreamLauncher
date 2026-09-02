package com.android.launcher3.iconresize;

import static com.android.launcher3.LauncherSettings.Favorites.CONTAINER_DESKTOP;
import static com.android.launcher3.LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.CellLayout;
import static com.android.launcher3.AbstractFloatingView.TYPE_ACTION_POPUP;
import static com.android.launcher3.AbstractFloatingView.TYPE_WIDGET_RESIZE_FRAME;
import static com.android.launcher3.CellLayout.WORKSPACE;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutAndWidgetContainer;
import com.android.launcher3.Workspace;
import com.android.launcher3.celllayout.CellLayoutLayoutParams;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.android.launcher3.dragndrop.DragOptions;
import com.android.launcher3.dragndrop.DragView;
import com.android.launcher3.DropTarget;
import com.android.launcher3.views.ActivityContext;

import java.util.Arrays;
import java.util.List;

/**
 * Workspace app-icon resize (Oppo-style 1×1 / 1×2 / 2×1 / 2×2) behind a feature flag.
 * Long-press shows a rounded outline + bottom-right arc handle; drag the handle to resize.
 */
public final class IconResizeHelper {

    private static final String TAG = "IconResizeHelper";

    public static final int MIN_SPAN = 1;
    public static final int MAX_SPAN = 2;

    private IconResizeHelper() {}

    public static boolean isEnabled() {
        return FeatureFlags.ENABLE_WORKSPACE_ICON_RESIZE.get();
    }

    /** Clamps span read from DB when the feature is off. */
    public static int normalizeSpan(int span) {
        return Math.max(MIN_SPAN, Math.min(MAX_SPAN, span));
    }

    public static boolean isValidPreset(int spanX, int spanY) {
        spanX = normalizeSpan(spanX);
        spanY = normalizeSpan(spanY);
        return (spanX == 1 && spanY == 1)
                || (spanX == 1 && spanY == 2)
                || (spanX == 2 && spanY == 1)
                || (spanX == 2 && spanY == 2);
    }

    public static boolean canResize(@Nullable ItemInfo info) {
        if (!isEnabled() || info == null) {
            return false;
        }
        if (info.container != CONTAINER_DESKTOP) {
            return false;
        }
        if (info.itemType != ITEM_TYPE_APPLICATION
                && info.itemType != LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT
                && info.itemType != LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT) {
            return false;
        }
        if (info instanceof WorkspaceItemInfo wii && wii.isDisabled()) {
            return false;
        }
        return true;
    }

    public static boolean hasExtendedSpan(@Nullable ItemInfo info) {
        return info != null && (info.spanX > MIN_SPAN || info.spanY > MIN_SPAN);
    }

    /**
     * Morph icon region in view coordinates (Oppo {@code IconUtils.getIconBoundsWhenMorphed}).
     */
    public static Rect getMorphIconBounds(DeviceProfile dp, int spanX, int spanY) {
        spanX = normalizeSpan(spanX);
        spanY = normalizeSpan(spanY);
        int cellWidth = dp.cellWidthPx;
        int cellHeight = dp.cellHeightPx;
        int iconSize = dp.iconSizePx;
        int contentHeight = dp.useOppoWorkspaceMetrics()
                ? dp.getOppoWorkspaceContentHeight()
                : dp.getCellContentHeight(WORKSPACE);
        float iconFactor = dp.useOppoWorkspaceMetrics()
                ? dp.getOppoIconTopFactor()
                : 0.5f;
        int top = (int) ((cellHeight - contentHeight) * iconFactor);
        int horizontalInset = dp.cellLayoutBorderSpacePx.x > 0 ? 0 : dp.workspaceCellPaddingXPx;
        return getBoundsBySpan(top, horizontalInset, cellWidth, cellHeight, iconSize, spanX, spanY);
    }

    /** Oppo {@code IconUtils.getBoundsBySpan}. */
    private static Rect getBoundsBySpan(int top, int horizontalInset, int cellWidth,
            int cellHeight, int iconSize, int spanX, int spanY) {
        int left = horizontalInset;
        int right;
        int bottom = top + iconSize;
        if (spanX == 1 && spanY == 1) {
            left = (cellWidth - iconSize) / 2;
            right = left + iconSize;
        } else if (spanX == 1 && spanY == 2) {
            left = (cellWidth - iconSize) / 2;
            right = left + iconSize;
            bottom += cellHeight;
        } else if (spanX == 2 && spanY == 1) {
            right = (cellWidth * 2) - horizontalInset;
        } else if (spanX == 2 && spanY == 2) {
            bottom += cellHeight;
            right = (cellWidth * 2) - horizontalInset;
        } else {
            left = (cellWidth - iconSize) / 2;
            right = left + iconSize;
        }
        return new Rect(left, top, right, bottom);
    }

    /**
     * Compound-drawable bounds: morph plate size for extended spans, 1×1 otherwise.
     */
    public static Rect getIconDrawableBounds(BubbleTextView view, int spanX, int spanY) {
        spanX = normalizeSpan(spanX);
        spanY = normalizeSpan(spanY);
        int iconSize = view.getIconSize();
        if (spanX == 1 && spanY == 1) {
            return new Rect(0, 0, iconSize, iconSize);
        }
        DeviceProfile dp = ActivityContext.lookupContext(view.getContext()).getDeviceProfile();
        Rect morph = getMorphIconBounds(dp, spanX, spanY);
        return new Rect(0, 0, morph.width(), morph.height());
    }

    /**
     * Step 3: Oppo morph asset when uxicon is present; else FILL morph plate (no stretch).
     */
    public static Drawable wrapMorphDisplayDrawable(BubbleTextView view, Drawable icon) {
        Object tag = view.getTag();
        if (!(tag instanceof ItemInfo info) || !canResize(info) || !hasExtendedSpan(info)) {
            return icon;
        }
        return MorphIconLoaderHelper.loadDisplayDrawable(view, icon);
    }

    /** Refreshes compound drawable after span or layout change. */
    public static void refreshIconDisplay(BubbleTextView view) {
        if (!isEnabled() || view.getIcon() == null) {
            return;
        }
        Object tag = view.getTag();
        if (!(tag instanceof ItemInfo info) || !canResize(info)) {
            return;
        }
        view.refreshWorkspaceIconDisplay();
    }

    /** Bounds for the resize frame overlay, in {@link BubbleTextView} coordinates. */
    public static void getResizeFrameBounds(BubbleTextView view, int spanX, int spanY, Rect out) {
        getIconBoundsForSpan(view, spanX, spanY, out);
    }

    /**
     * Icon bounds for a span preset in {@link BubbleTextView} coordinates (Oppo morph region).
     */
    public static void getIconBoundsForSpan(BubbleTextView view, int spanX, int spanY, Rect out) {
        view.getIconBoundsForSpan(spanX, spanY, out);
    }

    /** Dismisses the shortcuts popup opened during icon long-press. */
    public static void dismissShortcutsPopup(ActivityContext activity) {
        AbstractFloatingView popup = AbstractFloatingView.getOpenView(activity, TYPE_ACTION_POPUP);
        if (popup != null) {
            popup.close(true);
        }
    }

    /** Dismisses the workspace icon resize outline if shown. */
    public static void dismissResizeFrame(ActivityContext activity) {
        AbstractFloatingView.closeOpenViews(activity, true, TYPE_WIDGET_RESIZE_FRAME);
    }

    public static void applyIconDrawableBounds(BubbleTextView view) {
        refreshIconDisplay(view);
    }

    /** Called from {@code Workspace.beginDragShared} when a workspace app long-press starts. */
    public static void showResizeFrameOnLongPress(Launcher launcher, BubbleTextView icon,
            CellLayout cellLayout, @Nullable DragView<?> dragView) {
        ItemInfo info = (ItemInfo) icon.getTag();
        if (!canResize(info)) {
            Log.d(TAG, "showResizeFrameOnLongPress: skip canResize=false info=" + info);
            return;
        }
        Log.d(TAG, "showResizeFrameOnLongPress: showing for " + info.title);
        AppIconResizeFrame.showForIcon(icon, cellLayout, dragView);
    }

    /**
     * Prevents the icon from lifting while the user is dragging the resize handle.
     */
    public static DragOptions.PreDragCondition wrapPreDragCondition(
            DragOptions.PreDragCondition inner) {
        if (inner == null) {
            return null;
        }
        return new DragOptions.PreDragCondition() {
            @Override
            public boolean shouldStartDrag(double distanceDragged) {
                if (AppIconResizeFrame.isHandleDragging()) {
                    return false;
                }
                return inner.shouldStartDrag(distanceDragged);
            }

            @Override
            public void onPreDragStart(DropTarget.DragObject dragObject) {
                inner.onPreDragStart(dragObject);
            }

            @Override
            public void onPreDragEnd(DropTarget.DragObject dragObject, boolean dragStarted) {
                inner.onPreDragEnd(dragObject, dragStarted);
            }

            @Override
            public android.graphics.Point getDragOffset() {
                return inner.getDragOffset();
            }
        };
    }

    /**
     * Applies a new span to a workspace icon: grid placement, view layout, optional DB write.
     *
     * @param persist when true, writes to DB and plays commit animation
     * @return true if the resize succeeded
     */
    public static boolean applyIconSpan(Launcher launcher, View iconView, int newSpanX,
            int newSpanY) {
        return applyIconSpan(launcher, iconView, newSpanX, newSpanY, true);
    }

    public static boolean applyIconSpan(Launcher launcher, View iconView, int newSpanX,
            int newSpanY, boolean persist) {
        return applyIconSpan(launcher, iconView, newSpanX, newSpanY, persist, false);
    }

    /** Whether {@code newSpanX/Y} can be placed without moving the icon off-screen. */
    public static boolean canPlaceSpan(Launcher launcher, View iconView, int newSpanX,
            int newSpanY) {
        if (!canResize((ItemInfo) iconView.getTag())) {
            return false;
        }
        newSpanX = normalizeSpan(newSpanX);
        newSpanY = normalizeSpan(newSpanY);
        if (!isValidPreset(newSpanX, newSpanY)) {
            return false;
        }
        ItemInfo info = (ItemInfo) iconView.getTag();
        if (info.spanX == newSpanX && info.spanY == newSpanY) {
            return true;
        }
        Workspace workspace = launcher.getWorkspace();
        CellLayout cellLayout = workspace.getParentCellLayoutForView(iconView);
        if (cellLayout == null) {
            return false;
        }
        ViewGroup.LayoutParams lp = iconView.getLayoutParams();
        if (!(lp instanceof CellLayoutLayoutParams clp)) {
            return false;
        }
        int cellX = clp.getCellX();
        int cellY = clp.getCellY();
        cellLayout.markCellsAsUnoccupiedForView(iconView);
        boolean canPlace = cellLayout.isRegionVacant(cellX, cellY, newSpanX, newSpanY)
                || cellLayout.findCellForSpan(new int[2], newSpanX, newSpanY);
        cellLayout.markCellsAsOccupiedForView(iconView);
        return canPlace;
    }

    public static boolean applyIconSpan(Launcher launcher, View iconView, int newSpanX,
            int newSpanY, boolean persist, boolean skipMorphAnimation) {
        if (!canResize((ItemInfo) iconView.getTag())) {
            return false;
        }
        if (!(iconView instanceof BubbleTextView btv)) {
            return false;
        }
        ItemInfo info = (ItemInfo) iconView.getTag();
        newSpanX = normalizeSpan(newSpanX);
        newSpanY = normalizeSpan(newSpanY);
        if (!isValidPreset(newSpanX, newSpanY)) {
            return false;
        }
        if (info.spanX == newSpanX && info.spanY == newSpanY) {
            return true;
        }

        int oldSpanX = info.spanX;
        int oldSpanY = info.spanY;

        Workspace workspace = launcher.getWorkspace();
        CellLayout cellLayout = workspace.getParentCellLayoutForView(iconView);
        if (cellLayout == null) {
            return false;
        }
        ViewGroup.LayoutParams lp = iconView.getLayoutParams();
        if (!(lp instanceof CellLayoutLayoutParams clp)) {
            return false;
        }

        int cellX = clp.getCellX();
        int cellY = clp.getCellY();

        if (!canPlaceSpan(launcher, iconView, newSpanX, newSpanY)) {
            if (persist) {
                Toast.makeText(launcher, R.string.icon_resize_no_space, Toast.LENGTH_SHORT).show();
            }
            return false;
        }

        cellLayout.markCellsAsUnoccupiedForView(iconView);
        int targetX = cellX;
        int targetY = cellY;
        if (!cellLayout.isRegionVacant(targetX, targetY, newSpanX, newSpanY)) {
            int[] vacant = new int[2];
            cellLayout.findCellForSpan(vacant, newSpanX, newSpanY);
            targetX = vacant[0];
            targetY = vacant[1];
        }

        info.spanX = newSpanX;
        info.spanY = newSpanY;
        info.cellX = targetX;
        info.cellY = targetY;
        MorphIconLoaderHelper.invalidateCache(info);
        clp.setCellX(targetX);
        clp.setCellY(targetY);
        clp.cellHSpan = newSpanX;
        clp.cellVSpan = newSpanY;

        cellLayout.markCellsAsOccupiedForView(iconView);

        ShortcutAndWidgetContainer container = cellLayout.getShortcutsAndWidgets();
        container.setupLp(iconView);
        container.measureChild(iconView);
        cellLayout.requestLayout();

        if (!skipMorphAnimation) {
            MorphIconTransitionHelper.animateSpanChange(btv, oldSpanX, oldSpanY, newSpanX, newSpanY,
                    persist);
        } else if (persist) {
            refreshIconDisplay(btv);
        }

        if (persist) {
            launcher.getModelWriter().modifyItemInDatabase(info, info.container, info.screenId,
                    targetX, targetY, newSpanX, newSpanY);
        }
        return true;
    }

    /** Subtle bounce when the user commits a new span preset. */
    public static void playCommitAnim(BubbleTextView btv) {
        playResizeAnim(btv);
    }

    private static void playResizeAnim(BubbleTextView btv) {
        ObjectAnimator sx = ObjectAnimator.ofFloat(btv, View.SCALE_X, 0.92f, 1f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(btv, View.SCALE_Y, 0.92f, 1f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(sx, sy);
        set.setDuration(200);
        set.setInterpolator(new DecelerateInterpolator());
        set.start();
    }

    /** Sanitize span loaded from DB; invalid values fall back to 1×1. */
    public static void sanitizeLoadedSpan(ItemInfo info, int dbSpanX, int dbSpanY) {
        if (!isEnabled()
                || info.container != CONTAINER_DESKTOP
                || info.itemType != ITEM_TYPE_APPLICATION) {
            info.spanX = MIN_SPAN;
            info.spanY = MIN_SPAN;
            return;
        }
        int spanX = normalizeSpan(dbSpanX);
        int spanY = normalizeSpan(dbSpanY);
        if (!isValidPreset(spanX, spanY)) {
            info.spanX = MIN_SPAN;
            info.spanY = MIN_SPAN;
        } else {
            info.spanX = spanX;
            info.spanY = spanY;
        }
    }

    /** All valid workspace icon size presets. */
    public static List<int[]> getAllPresets() {
        return Arrays.asList(
                new int[]{1, 1},
                new int[]{1, 2},
                new int[]{2, 1},
                new int[]{2, 2});
    }
}
