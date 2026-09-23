/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import static com.android.launcher3.LauncherSettings.Favorites.CONTAINER_HOTSEAT;
import static com.android.launcher3.LauncherSettings.Favorites.CONTAINER_HOTSEAT_PREDICTION;
import static com.android.launcher3.anim.Interpolators.GRID_CHANGE_INTERPOLATOR;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.android.launcher3.celllayout.CellLayoutLayoutParams;
import com.android.launcher3.hotseat.HotseatSpringMotion;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.util.CellAndSpan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * View class that represents the bottom row of the home screen.
 *
 * Phone dock follows Oppo ColorOS policy:
 * <ul>
 *   <li>Max capacity is {@link DeviceProfile#numShownHotseatIcons} (phone: 5),
 *       independent of workspace columns.</li>
 *   <li>Icons are packed contiguously and centered.</li>
 *   <li>Cell width uses workspace column pitch when {@code n <= numColumns},
 *       otherwise divides by {@code n} (see {@link #getAdaptiveCellDivisor}).</li>
 *   <li>During an active drag over the dock, the grid stays expanded to max equal-width
 *       cells so drop targets stay stable.</li>
 *   <li>When the drag leaves the dock, remaining icons pack contiguously and spring to
 *       center (Oppo drag-out), excluding the INVISIBLE drag source.</li>
 * </ul>
 */
public class Hotseat extends CellLayout implements Insettable {

    // Ratio of empty space, qsb should take up to appear visually centered.
    public static final float QSB_CENTER_FACTOR = .325f;

    @ViewDebug.ExportedProperty(category = "launcher")
    private boolean mHasVerticalHotseat;
    private Workspace<?> mWorkspace;
    private boolean mSendTouchToWorkspace;

    private final View mQsb;

    /** Extra side inset applied so a contiguous icon row sits centered. */
    private int mAdaptiveSidePad;

    /**
     * True from first {@link #onDragEnter()} until {@link #endDragSession()} after drop/cancel.
     * Keeps the dock in a drag session so leave/re-enter does not tear down state early.
     */
    private boolean mDragSessionActive;

    /**
     * True only while the finger is over the dock during {@link #mDragSessionActive}.
     */
    private boolean mDragOverDock;

    /**
     * True after icons are packed without an insert gap (drag-out or external exit).
     */
    private boolean mPackedAway;

    /**
     * True when the active drag originated from this dock (Oppo container == -101).
     * Make-room expand must not undo the lift pack for these drags.
     */
    private boolean mDockSourceDrag;

    /** True while workspace→hotseat make-room uses a centered n+1 grid. */
    private boolean mMakeRoomActive;

    /**
     * True after make-room has switched cell metrics to n+1. Until then, keep packed-n
     * metrics so icons do not jump before the insert spring runs.
     */
    private boolean mMakeRoomLayoutActive;

    /**
     * True after the finger has left the dock during this drag (Oppo TypeToOut ran).
     * Distinguishes first enter-after-lift from a real workspace→dock TypeToIn.
     */
    private boolean mHasLeftDockDuringDrag;

    /**
     * Adaptive cell-width divisor locked for this drag session. Oppo TypeToOut keeps the
     * pre-lift cell width and only recenters; TypeToIn reopens a hole on that same grid.
     * 0 = unlocked (idle / use live icon count).
     */
    private int mSessionMetricCount;

    /** Drag-source views hidden with {@link View#GONE} while the dock is packed away. */
    private final ArrayList<View> mHiddenDragSources = new ArrayList<>();

    private android.animation.ValueAnimator mPackPadAnimator;

    /** Oppo-style COUI animX springs for pack / make-room / insert. */
    private final HotseatSpringMotion mSpringMotion = new HotseatSpringMotion(this);

    private final Runnable mReflowRunnable = this::reflowIcons;

    public Hotseat(Context context) {
        this(context, null);
    }

    public Hotseat(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Hotseat(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mQsb = LayoutInflater.from(context).inflate(R.layout.search_container_hotseat, this, false);
        addView(mQsb);
    }

    /**
     * While animX springs own icon seats, disable clipping so a transient overflow
     * (old X + new cellW) is not hard-cut at the dock edges.
     */
    public void setHotseatClipEnabled(boolean clip) {
        setClipChildren(clip);
        setClipToPadding(clip);
        ShortcutAndWidgetContainer container = getShortcutsAndWidgets();
        if (container != null) {
            container.setClipChildren(clip);
            container.setClipToPadding(clip);
        }
    }

    /** Called by {@link HotseatSpringMotion} when the last spring settles. */
    public void onIconSpringsSettled() {
        setHotseatClipEnabled(true);
        ShortcutAndWidgetContainer container = getShortcutsAndWidgets();
        if (container != null) {
            container.requestLayout();
        }
    }

    /**
     * Oppo {@code OplusHotseat.isMoving()}: ShortcutAndWidgetContainer lays out from
     * {@link CellLayoutLayoutParams#animX} while springs own the seat.
     */
    public boolean isIconSpringMoving() {
        return mSpringMotion.isMoving();
    }

    /**
     * Returns orientation specific cell X given invariant order in the hotseat
     */
    public int getCellXFromOrder(int rank) {
        return mHasVerticalHotseat ? 0 : rank;
    }

    /**
     * Returns orientation specific cell Y given invariant order in the hotseat
     */
    public int getCellYFromOrder(int rank) {
        return mHasVerticalHotseat ? (getCountY() - (rank + 1)) : 0;
    }

    public void resetLayout(boolean hasVerticalHotseat) {
        removeAllViewsInLayout();
        mHasVerticalHotseat = hasVerticalHotseat;
        mAdaptiveSidePad = 0;
        mDragSessionActive = false;
        mDragOverDock = false;
        mPackedAway = false;
        mDockSourceDrag = false;
        mHasLeftDockDuringDrag = false;
        mSessionMetricCount = 0;
        mMakeRoomActive = false;
        mMakeRoomLayoutActive = false;
        mHiddenDragSources.clear();
        cancelPackPadAnimator();
        mSpringMotion.clearAll();
        removeCallbacks(mReflowRunnable);
        DeviceProfile dp = mActivity.getDeviceProfile();
        resetCellSize(dp);
        if (hasVerticalHotseat) {
            setGridSize(1, dp.numShownHotseatIcons);
        } else {
            setGridSize(dp.numShownHotseatIcons, 1);
        }
    }

    /** Number of persistent (non-prediction) dock icons. */
    public int getPersistentIconCount() {
        return collectDockIcons(getShortcutsAndWidgets()).size();
    }

    /** Max dock capacity from the device profile. */
    public int getMaxIconCount() {
        return Math.max(1, mActivity.getDeviceProfile().numShownHotseatIcons);
    }

    /** Whether the dock can accept another workspace/external icon (not already in hotseat). */
    public boolean canAcceptNewIcon() {
        return getPersistentIconCount() < getMaxIconCount();
    }

    /**
     * Oppo-style: reflow dock icons into a contiguous centered row and apply adaptive
     * cell width. Safe to call after bind, drop, or remove. No-op during an active drag
     * session so expand geometry stays stable.
     */
    public void reflowIcons() {
        if (mDragSessionActive) {
            return;
        }
        if (mHasVerticalHotseat) {
            return;
        }
        DeviceProfile dp = mActivity.getDeviceProfile();
        if (dp.isVerticalBarLayout() || dp.isTablet) {
            return;
        }

        ShortcutAndWidgetContainer container = getShortcutsAndWidgets();
        List<View> icons = collectDockIcons(container);
        int n = icons.size();
        int max = Math.max(1, dp.numShownHotseatIcons);

        // Idle: grid matches icon count so adaptive width + side pad can center the row.
        // Empty dock keeps full max for drop targets. Drag-enter expands to max.
        int gridX = n > 0 ? Math.min(n, max) : max;
        if (getCountX() != gridX || getCountY() != 1) {
            setGridSize(gridX, 1);
        }

        // Pack left-to-right by current visual order (cellX / rank).
        icons.sort(Comparator.comparingInt(v -> {
            CellLayoutLayoutParams lp = (CellLayoutLayoutParams) v.getLayoutParams();
            return lp != null ? lp.getCellX() : 0;
        }));

        mOccupied.clear();
        Launcher launcher = mActivity instanceof Launcher ? (Launcher) mActivity : null;
        for (int i = 0; i < n; i++) {
            View v = icons.get(i);
            CellLayoutLayoutParams lp = (CellLayoutLayoutParams) v.getLayoutParams();
            if (lp == null) {
                continue;
            }
            lp.setCellX(i);
            lp.setCellY(0);
            lp.cellHSpan = 1;
            lp.cellVSpan = 1;
            Object tag = v.getTag();
            if (tag instanceof ItemInfo info && isPersistentHotseatItem(info)) {
                boolean changed = info.cellX != i || info.screenId != i
                        || info.container != CONTAINER_HOTSEAT;
                info.cellX = i;
                info.cellY = 0;
                info.screenId = i;
                info.container = CONTAINER_HOTSEAT;
                if (changed && launcher != null && info.id != ItemInfo.NO_ID) {
                    launcher.getModelWriter().moveItemInDatabase(
                            info,
                            CONTAINER_HOTSEAT,
                            i,
                            i,
                            0);
                }
            }
            markCellsAsOccupiedForView(v);
        }

        applyAdaptiveCellMetrics(n);
        requestLayout();
    }

    /**
     * Workspace→hotseat: open a centered make-room slot (Oppo LiveData phantom / n+1).
     * Dock-source TypeToIn only after a real leave (not the first enter after lift).
     */
    @Override
    public void onDragEnter() {
        super.onDragEnter();
        beginDragSession();
        mDragOverDock = true;
        if (mHasVerticalHotseat) {
            return;
        }
        DeviceProfile dp = mActivity.getDeviceProfile();
        if (dp.isTablet || dp.isVerticalBarLayout()) {
            return;
        }
        cancelPackPadAnimator();
        ensureDragSourcesHidden();
        if (mDockSourceDrag && !mHasLeftDockDuringDrag) {
            // Still the lift session — stay packed until finger leaves or insert runs.
            return;
        }
        // External make-room OR dock-source return after TypeToOut.
        mPackedAway = false;
        mMakeRoomActive = true;
        mMakeRoomLayoutActive = false;
        applyMakeRoomMetrics();
    }

    @Override
    public void onDragExit() {
        final boolean packOnExit = mDragSessionActive;
        if (packOnExit) {
            completeAndClearReorderPreviewAnimations();
            setItemPlacementDirty(false);
            setUseTempCoords(false);
        }
        super.onDragExit();
        if (packOnExit) {
            onFingerAwayFromDock();
        }
    }

    /**
     * Ends the dock drag session and restores the packed/centered idle layout.
     */
    public void endDragSession() {
        mDragOverDock = false;
        mPackedAway = false;
        mDockSourceDrag = false;
        mHasLeftDockDuringDrag = false;
        mSessionMetricCount = 0;
        mMakeRoomActive = false;
        mMakeRoomLayoutActive = false;
        cancelPackPadAnimator();
        // Snap in-flight insert springs to their finals, then idle-reflow without
        // restarting a second left/right shuffle (see reflowIconsAfterDrop).
        mSpringMotion.endAll();
        mHiddenDragSources.clear();
        if (!mDragSessionActive) {
            removeCallbacks(mReflowRunnable);
            reflowIconsAfterDrop();
            return;
        }
        mDragSessionActive = false;
        removeCallbacks(mReflowRunnable);
        reflowIconsAfterDrop();
    }

    /**
     * Idle pack after drop/cancel: update cell ranks/metrics but keep icons on their
     * current screen seats when the idle grid matches (no second spring).
     */
    private void reflowIconsAfterDrop() {
        if (mHasVerticalHotseat) {
            reflowIcons();
            return;
        }
        DeviceProfile dp = mActivity.getDeviceProfile();
        if (dp.isVerticalBarLayout() || dp.isTablet) {
            reflowIcons();
            return;
        }
        ShortcutAndWidgetContainer container = getShortcutsAndWidgets();
        List<View> icons = collectDockIcons(container);
        int n = icons.size();
        int max = Math.max(1, dp.numShownHotseatIcons);
        int gridX = n > 0 ? Math.min(n, max) : max;

        icons.sort(Comparator.comparingInt(v -> {
            CellLayoutLayoutParams lp = (CellLayoutLayoutParams) v.getLayoutParams();
            return lp != null ? lp.getCellX() : 0;
        }));

        ArrayMap<View, int[]> origins = captureVisualOrigins(icons);
        boolean gridChanged = getCountX() != gridX || getCountY() != 1;
        if (gridChanged) {
            setGridSize(gridX, 1);
        }

        mOccupied.clear();
        Launcher launcher = mActivity instanceof Launcher ? (Launcher) mActivity : null;
        for (int i = 0; i < n; i++) {
            View v = icons.get(i);
            CellLayoutLayoutParams lp = (CellLayoutLayoutParams) v.getLayoutParams();
            if (lp == null) {
                continue;
            }
            lp.setCellX(i);
            lp.setCellY(0);
            lp.cellHSpan = 1;
            lp.cellVSpan = 1;
            lp.useTmpCoords = false;
            Object tag = v.getTag();
            if (tag instanceof ItemInfo info && isPersistentHotseatItem(info)) {
                boolean changed = info.cellX != i || info.screenId != i
                        || info.container != CONTAINER_HOTSEAT;
                info.cellX = i;
                info.cellY = 0;
                info.screenId = i;
                info.container = CONTAINER_HOTSEAT;
                if (changed && launcher != null && info.id != ItemInfo.NO_ID) {
                    launcher.getModelWriter().moveItemInDatabase(
                            info,
                            CONTAINER_HOTSEAT,
                            i,
                            i,
                            0);
                }
            }
            markCellsAsOccupiedForView(v);
        }

        applyAdaptiveCellMetrics(n);
        // Re-seed so a pad/cellW sync cannot flash icons through a wrong seat.
        seedAnimFromCapturedScreens(origins);
        for (View v : icons) {
            CellLayoutLayoutParams lp = (CellLayoutLayoutParams) v.getLayoutParams();
            if (lp == null) {
                continue;
            }
            lp.isHotseatChild = true;
            int targetX = mSpringMotion.computeTargetX(v);
            if (Math.abs(lp.animX - targetX) > 1) {
                mSpringMotion.animateAnimXTo(v, targetX);
            } else {
                lp.animX = targetX;
                lp.x = targetX;
                lp.isLockedToGrid = true;
            }
        }
        if (!mSpringMotion.isMoving()) {
            requestLayout();
        }
    }

    private void beginDragSession() {
        if (mDragSessionActive) {
            return;
        }
        mDragSessionActive = true;
        removeCallbacks(mReflowRunnable);
        // Oppo handleDragInit: animX = x before any pack/insert spring.
        ShortcutAndWidgetContainer container = getShortcutsAndWidgets();
        if (container != null) {
            for (int i = 0; i < container.getChildCount(); i++) {
                mSpringMotion.seedAnimFromLayout(container.getChildAt(i));
            }
        }
    }

    /**
     * Workspace calls this when a dock icon becomes the drag source.
     * Pack+center remaining icons immediately — do not expand first.
     */
    public void onItemDragStartedFromDock() {
        if (mHasVerticalHotseat) {
            return;
        }
        DeviceProfile dp = mActivity.getDeviceProfile();
        if (dp.isTablet || dp.isVerticalBarLayout()) {
            return;
        }
        beginDragSession();
        mDockSourceDrag = true;
        mHasLeftDockDuringDrag = false;
        mMakeRoomActive = false;
        mMakeRoomLayoutActive = false;
        mDragOverDock = false;
        mPackedAway = true;
        // Lock pre-lift count for TypeToIn reopen only; TypeToOut pack uses remaining n.
        int preLift = collectDockIcons(getShortcutsAndWidgets()).size();
        mSessionMetricCount = Math.max(1, Math.min(preLift, getMaxIconCount()));
        packRemainingIconsForDragOut(/* animate= */ true);
    }

    /**
     * Pack+center when the finger leaves the dock (external exit closes make-room;
     * dock-source lift is already packed).
     */
    public void onFingerAwayFromDock() {
        if (!mDragSessionActive || mPackedAway) {
            // Still mark leave so a later re-enter can TypeToIn.
            mHasLeftDockDuringDrag = true;
            mDragOverDock = false;
            return;
        }
        mDragOverDock = false;
        mHasLeftDockDuringDrag = true;
        mMakeRoomActive = false;
        mMakeRoomLayoutActive = false;
        mPackedAway = true;
        packRemainingIconsForDragOut(/* animate= */ true);
    }

    /**
     * Oppo canMergeFolder preview helper (kept for diagnostics). Prefer Workspace
     * folder drag-mode for suppressing insert — a wide radius falsely covers the
     * whole dock and blocks make-room.
     */
    public boolean isFolderMergeLikely(float pixelX, float pixelY, ItemInfo dragInfo) {
        if (dragInfo == null) {
            return false;
        }
        if (dragInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_FOLDER) {
            return false;
        }
        List<View> icons = collectPackableDockIcons(getShortcutsAndWidgets());
        if (icons.isEmpty()) {
            return false;
        }
        // Tight radius: ~0.35 of icon size so only true center-hits count.
        float radius = Math.max(mActivity.getDeviceProfile().iconSizePx * 0.35f, 1f);
        for (View v : icons) {
            float cx = getIconCenterX(v);
            float cy = getPaddingTop() + getCellHeight() / 2f;
            if (Math.hypot(pixelX - cx, pixelY - cy) <= radius) {
                Object tag = v.getTag();
                if (tag instanceof ItemInfo info && isPersistentHotseatItem(info)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Close any insert gap and pack visible icons at 0..n-1 (folder-merge hover).
     */
    public void clearInsertGapForFolderMerge() {
        if (mDockSourceDrag || mHasVerticalHotseat) {
            return;
        }
        DeviceProfile dp = mActivity.getDeviceProfile();
        if (dp.isTablet || dp.isVerticalBarLayout()) {
            return;
        }
        mMakeRoomActive = false;
        mMakeRoomLayoutActive = false;
        packRemainingIconsForDragOut(/* animate= */ true);
    }

    /**
     * Workspace→hotseat make-room: expand countX to n+1 only. Keep packed-n cell metrics
     * until the first insert spring so icons do not jump away from their on-screen seats.
     */
    private void applyMakeRoomMetrics() {
        DeviceProfile dp = mActivity.getDeviceProfile();
        List<View> icons = collectPackableDockIcons(getShortcutsAndWidgets());
        int n = icons.size();
        int max = Math.max(1, dp.numShownHotseatIcons);
        int slots;
        if (mDockSourceDrag && mSessionMetricCount > 0) {
            slots = Math.min(mSessionMetricCount, max);
        } else {
            slots = n >= max ? Math.min(n, max) : Math.min(n + 1, max);
        }
        if (getCountX() != slots || getCountY() != 1) {
            setGridSize(slots, 1);
        }
        mOccupied.clear();
        for (View v : icons) {
            markCellsAsOccupiedForView(v);
        }
    }

    /**
     * Capture each child's current on-screen origin (absolute screen pixels) so a later
     * pad/cellW change can re-seed {@code animX} into the new container coordinate space.
     */
    private ArrayMap<View, int[]> captureVisualOrigins(List<View> views) {
        ArrayMap<View, int[]> origins = new ArrayMap<>();
        if (views == null || views.isEmpty()) {
            return origins;
        }
        for (View v : views) {
            if (v == null) {
                continue;
            }
            int[] loc = new int[2];
            if (v.getWidth() > 0) {
                v.getLocationOnScreen(loc);
            } else {
                CellLayoutLayoutParams lp = (CellLayoutLayoutParams) v.getLayoutParams();
                ShortcutAndWidgetContainer container = getShortcutsAndWidgets();
                if (lp == null || container == null) {
                    continue;
                }
                int[] parentLoc = new int[2];
                container.getLocationOnScreen(parentLoc);
                loc[0] = parentLoc[0] + lp.x;
                loc[1] = parentLoc[1] + lp.y;
            }
            origins.put(v, loc);
        }
        return origins;
    }

    /**
     * Re-seed animX/animY so each icon stays on its captured screen seat after metrics
     * changed. Uses Hotseat padding (not stale container getLocationOnScreen) because
     * {@link #applyAdaptiveCellMetrics} has not laid out yet.
     * <p>When cellW grows (5→4), keep the pre-change icon <em>center</em> on the same
     * screen X so the spring still has distance to the new packed seat. Do not clamp
     * to the dock bounds here — that pinned edge icons to their final X and skipped
     * their animation; overflow is allowed while {@link #setHotseatClipEnabled}(false).
     */
    private void seedAnimFromCapturedScreens(ArrayMap<View, int[]> origins) {
        if (origins == null) {
            return;
        }
        int[] hsLoc = new int[2];
        getLocationOnScreen(hsLoc);
        int unused = 0;
        if (getMeasuredWidth() > 0 && getCellWidth() > 0) {
            unused = getMeasuredWidth() - getPaddingLeft() - getPaddingRight()
                    - (getCountX() * getCellWidth())
                    - (Math.max(0, getCountX() - 1)
                    * (mBorderSpace != null ? mBorderSpace.x : 0));
        }
        int containerScreenX = hsLoc[0] + getPaddingLeft()
                + (int) Math.ceil(unused / 2f);
        int containerScreenY = hsLoc[1] + getPaddingTop();
        ShortcutAndWidgetContainer container = getShortcutsAndWidgets();
        for (int i = 0; i < origins.size(); i++) {
            View v = origins.keyAt(i);
            int[] screen = origins.valueAt(i);
            if (v == null || screen == null) {
                continue;
            }
            CellLayoutLayoutParams lp = (CellLayoutLayoutParams) v.getLayoutParams();
            if (lp == null) {
                continue;
            }
            lp.isHotseatChild = true;
            // Refresh lp.width to the new cell metrics before placing animX.
            if (container != null) {
                boolean locked = lp.isLockedToGrid;
                lp.isLockedToGrid = true;
                container.setupLp(v);
                lp.isLockedToGrid = locked;
            }
            int childW = Math.max(lp.width, v.getMeasuredWidth());
            int oldW = Math.max(v.getWidth(), 1);
            // Keep the pre-change icon center on the same screen X when cellW grows.
            int oldCenterScreenX = screen[0] + oldW / 2;
            lp.animX = oldCenterScreenX - containerScreenX - childW / 2;
            lp.animY = screen[1] - containerScreenY;
            lp.x = lp.animX;
            lp.y = lp.animY;
            lp.isLockedToGrid = false;
            v.layout(lp.animX, lp.animY,
                    lp.animX + childW,
                    lp.animY + Math.max(v.getMeasuredHeight(), lp.height));
        }
    }

    /**
     * Switch make-room visuals to n+1 metrics without losing the current icon seats.
     * Call immediately before insert springs so animX starts from the packed screen seat.
     */
    private void activateMakeRoomLayoutFromVisualStarts(List<View> icons) {
        if (mMakeRoomLayoutActive || icons == null) {
            return;
        }
        DeviceProfile dp = mActivity.getDeviceProfile();
        int n = icons.size();
        int max = Math.max(1, dp.numShownHotseatIcons);
        // Dock-source TypeToIn: reopen the pre-lift grid (phantom hole). External: n+1.
        int slots;
        if (mDockSourceDrag && mSessionMetricCount > 0) {
            slots = Math.min(mSessionMetricCount, max);
        } else {
            slots = n >= max ? Math.min(n, max) : Math.min(n + 1, max);
        }
        ArrayMap<View, int[]> origins = captureVisualOrigins(icons);
        if (getCountX() != slots || getCountY() != 1) {
            setGridSize(slots, 1);
        }
        applyAdaptiveCellMetrics(slots);
        mMakeRoomLayoutActive = true;
        // Restart springs from packed screen seats after n+1 metrics.
        mSpringMotion.clearAll();
        seedAnimFromCapturedScreens(origins);
    }

    /**
     * Ensure {@link #getCountX()} can hold {@code cellX} before {@code addViewToCellLayout}.
     * Pack-on-exit can shrink the grid while ON_DROP still targets the make-room slot.
     */
    public void ensureGridHoldsCell(int cellX) {
        if (mHasVerticalHotseat || cellX < 0) {
            return;
        }
        DeviceProfile dp = mActivity.getDeviceProfile();
        if (dp.isTablet || dp.isVerticalBarLayout()) {
            return;
        }
        int max = Math.max(1, dp.numShownHotseatIcons);
        int need = Math.min(Math.max(cellX + 1, getCountX()), max);
        if (getCountX() < need) {
            setGridSize(need, 1);
            applyAdaptiveCellMetrics(need);
            mMakeRoomActive = true;
        }
    }

    /** Whether the active drag originated from this dock. */
    public boolean isDockSourceDrag() {
        return mDockSourceDrag;
    }

    /**
     * Re-enable centered n+1 make-room after leaving a folder-merge hover.
     */
    public void ensureMakeRoomForExternalDrag() {
        if (mDockSourceDrag || mHasVerticalHotseat || !mDragOverDock) {
            return;
        }
        DeviceProfile dp = mActivity.getDeviceProfile();
        if (dp.isTablet || dp.isVerticalBarLayout()) {
            return;
        }
        if (mMakeRoomActive) {
            return;
        }
        mPackedAway = false;
        mMakeRoomActive = true;
        applyMakeRoomMetrics();
    }

    /**
     * Oppo drag-out / TypeToOut: hide the drag source, shrink grid to remaining count,
     * apply adaptive center metrics, spring icons to {@code 0..n-1} from their
     * <em>current on-screen</em> seats via animX (not from a pre-snapped grid).
     */
    private void packRemainingIconsForDragOut(boolean animate) {
        if (mHasVerticalHotseat) {
            return;
        }
        DeviceProfile dp = mActivity.getDeviceProfile();
        if (dp.isTablet || dp.isVerticalBarLayout()) {
            return;
        }

        cancelPackPadAnimator();
        hideDragSourcesForPack();
        mMakeRoomActive = false;
        mMakeRoomLayoutActive = false;
        // Drop in-flight springs so we can re-seed animX from screen seats.
        mSpringMotion.clearAll();

        // Drop any in-flight insert temp coords / ValueAnimator reorders.
        completeAndClearReorderPreviewAnimations();
        setItemPlacementDirty(false);
        setUseTempCoords(false);

        List<View> remaining = collectPackableDockIcons(getShortcutsAndWidgets());
        remaining.sort(Comparator.comparingInt(v -> {
            CellLayoutLayoutParams lp = (CellLayoutLayoutParams) v.getLayoutParams();
            return lp != null ? lp.getCellX() : 0;
        }));
        int n = remaining.size();
        int max = Math.max(1, dp.numShownHotseatIcons);

        // Capture screen seats BEFORE grid/pad/cellW change.
        ArrayMap<View, int[]> origins = captureVisualOrigins(remaining);

        int gridX = n > 0 ? Math.min(n, max) : max;
        if (getCountX() != gridX || getCountY() != 1) {
            setGridSize(gridX, 1);
        }
        // TypeToOut metrics = remaining count so mid-drag matches idle after drop
        // (5→4 full-width pack). Clip is disabled while springs run; seed clamps
        // so a wider cellW cannot push edge icons past the dock (4→3 clip bug).
        applyAdaptiveCellMetrics(n, n);
        seedAnimFromCapturedScreens(origins);
        setHotseatClipEnabled(false);

        mOccupied.clear();
        ShortcutAndWidgetContainer container = getShortcutsAndWidgets();
        for (int i = 0; i < n; i++) {
            View v = remaining.get(i);
            CellLayoutLayoutParams lp = (CellLayoutLayoutParams) v.getLayoutParams();
            if (lp == null) {
                continue;
            }
            Object tag = v.getTag();
            if (tag instanceof ItemInfo info && isPersistentHotseatItem(info)) {
                info.cellX = i;
                info.cellY = 0;
                info.screenId = i;
            }
            lp.cellHSpan = 1;
            lp.cellVSpan = 1;
            lp.useTmpCoords = false;
            lp.setCellX(i);
            lp.setCellY(0);
            lp.isHotseatChild = true;
            if (animate) {
                int targetX = mSpringMotion.computeTargetX(v);
                mSpringMotion.animateAnimXTo(v, targetX);
            } else {
                lp.isLockedToGrid = true;
                if (container != null) {
                    container.setupLp(v);
                }
                lp.animX = lp.x;
                lp.animY = lp.y;
            }
            markCellsAsOccupiedForView(v);
        }
        if (!animate) {
            requestLayout();
        }
    }

    private void hideDragSourcesForPack() {
        for (View parked : collectInvisibleDockIcons(getShortcutsAndWidgets())) {
            parked.setVisibility(GONE);
            if (!mHiddenDragSources.contains(parked)) {
                mHiddenDragSources.add(parked);
            }
        }
        // Re-assert GONE on anything we already tracked (second pack must not lose them).
        ensureDragSourcesHidden();
    }

    /** Oppo: drag source is never drawn in the dock until drop — only DragView. */
    private void ensureDragSourcesHidden() {
        for (View parked : mHiddenDragSources) {
            if (parked != null && parked.getParent() != null
                    && parked.getVisibility() != GONE) {
                parked.setVisibility(GONE);
            }
        }
        ShortcutAndWidgetContainer container = getShortcutsAndWidgets();
        if (container == null) {
            return;
        }
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child != null && child.getVisibility() == INVISIBLE) {
                Object tag = child.getTag();
                if (tag instanceof ItemInfo info && isPersistentHotseatItem(info)) {
                    child.setVisibility(GONE);
                    if (!mHiddenDragSources.contains(child)) {
                        mHiddenDragSources.add(child);
                    }
                }
            }
        }
    }

    private void restoreHiddenDragSources() {
        for (View parked : mHiddenDragSources) {
            if (parked.getParent() != null && parked.getVisibility() == GONE) {
                parked.setVisibility(INVISIBLE);
            }
        }
        mHiddenDragSources.clear();
    }

    private void cancelPackPadAnimator() {
        if (mPackPadAnimator != null) {
            mPackPadAnimator.cancel();
            mPackPadAnimator = null;
        }
    }

    /**
     * Oppo {@code HotseatParam.getAdaptiveHotseatCellWidth}: divisor is workspace
     * columns when icon count does not exceed columns; otherwise the icon count.
     */
    public static int getAdaptiveCellDivisor(int iconCount, int numColumns, int maxIcons) {
        int n = Math.max(0, Math.min(iconCount, maxIcons));
        if (n <= 0) {
            return Math.max(1, numColumns);
        }
        if (n <= numColumns) {
            return Math.max(1, numColumns);
        }
        return n;
    }

    private void applyAdaptiveCellMetrics(int iconCount) {
        applyAdaptiveCellMetrics(iconCount, iconCount);
    }

    /**
     * @param metricCount drives cell width (Oppo adaptive divisor)
     * @param visibleCount drives side padding so a shorter row recenters on the same cellW
     */
    private void applyAdaptiveCellMetrics(int metricCount, int visibleCount) {
        DeviceProfile dp = mActivity.getDeviceProfile();
        Rect basePad = dp.getHotseatLayoutPadding(getContext());
        int avail = getMeasuredWidth() - basePad.left - basePad.right;
        if (avail <= 0) {
            // Not measured yet — use profile width estimate.
            avail = dp.availableWidthPx - basePad.left - basePad.right;
        }
        if (avail <= 0) {
            return;
        }

        int divisor = getAdaptiveCellDivisor(metricCount, dp.inv.numColumns,
                dp.numShownHotseatIcons);
        int cellW = avail / divisor;
        int cellH = getCellHeight() > 0 ? getCellHeight() : dp.hotseatCellHeightPx;
        setCellDimensions(cellW, cellH);

        int used = visibleCount > 0 ? visibleCount * cellW : 0;
        mAdaptiveSidePad = visibleCount > 0 ? Math.max(0, (avail - used) / 2) : 0;
        setPadding(
                basePad.left + mAdaptiveSidePad,
                basePad.top,
                basePad.right + mAdaptiveSidePad,
                basePad.bottom);
    }

    /**
     * Persistent dock icons only — skips predictions and views without ItemInfo.
     * Includes INVISIBLE drag sources (they still occupy capacity until drop).
     */
    private static List<View> collectDockIcons(ShortcutAndWidgetContainer container) {
        return collectDockIcons(container, /* includeInvisible= */ true);
    }

    /**
     * Visible dock icons that should pack/center while a sibling is being dragged away.
     */
    private static List<View> collectPackableDockIcons(ShortcutAndWidgetContainer container) {
        return collectDockIcons(container, /* includeInvisible= */ false);
    }

    private static List<View> collectInvisibleDockIcons(ShortcutAndWidgetContainer container) {
        List<View> icons = new ArrayList<>();
        if (container == null) {
            return icons;
        }
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child == null || child.getVisibility() != View.INVISIBLE) {
                continue;
            }
            Object tag = child.getTag();
            if (!(tag instanceof ItemInfo info) || !isPersistentHotseatItem(info)) {
                continue;
            }
            icons.add(child);
        }
        return icons;
    }

    private static List<View> collectDockIcons(ShortcutAndWidgetContainer container,
            boolean includeInvisible) {
        List<View> icons = new ArrayList<>();
        if (container == null) {
            return icons;
        }
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child == null || child.getVisibility() == View.GONE) {
                continue;
            }
            if (!includeInvisible && child.getVisibility() != View.VISIBLE) {
                continue;
            }
            Object tag = child.getTag();
            if (!(tag instanceof ItemInfo info) || !isPersistentHotseatItem(info)) {
                continue;
            }
            icons.add(child);
        }
        return icons;
    }

    private static boolean isPersistentHotseatItem(ItemInfo info) {
        if (info == null) {
            return false;
        }
        // Predictions are visual fillers; never pack/DB-write them as favorites.
        return !info.isPredictedItem()
                && info.container != CONTAINER_HOTSEAT_PREDICTION;
    }

    @Override
    public void setInsets(Rect insets) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        DeviceProfile grid = mActivity.getDeviceProfile();

        if (grid.isVerticalBarLayout()) {
            mQsb.setVisibility(View.GONE);
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
            if (grid.isSeascape()) {
                lp.gravity = Gravity.LEFT;
                lp.width = grid.hotseatBarSizePx + insets.left;
            } else {
                lp.gravity = Gravity.RIGHT;
                lp.width = grid.hotseatBarSizePx + insets.right;
            }
        } else {
            mQsb.setVisibility(View.VISIBLE);
            lp.gravity = Gravity.BOTTOM;
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = grid.hotseatBarSizePx;
            lp.bottomMargin = grid.getOppoHotseatMarginBottomPx();
        }

        Rect padding = grid.getHotseatLayoutPadding(getContext());
        setPadding(padding.left + mAdaptiveSidePad, padding.top,
                padding.right + mAdaptiveSidePad, padding.bottom);
        setLayoutParams(lp);
        InsettableFrameLayout.dispatchInsets(this, insets);
    }

    public void setWorkspace(Workspace<?> w) {
        mWorkspace = w;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // We allow horizontal workspace scrolling from within the Hotseat. We do this by delegating
        // touch intercept the Workspace, and if it intercepts, delegating touch to the Workspace
        // for the remainder of the this input stream.
        int yThreshold = getMeasuredHeight() - getPaddingBottom();
        if (mWorkspace != null && ev.getY() <= yThreshold) {
            mSendTouchToWorkspace = mWorkspace.onInterceptTouchEvent(ev);
            return mSendTouchToWorkspace;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // See comment in #onInterceptTouchEvent
        if (mSendTouchToWorkspace) {
            final int action = event.getAction();
            switch (action & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mSendTouchToWorkspace = false;
            }
            return mWorkspace.onTouchEvent(event);
        }
        // Always let touch follow through to Workspace.
        return false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!mHasVerticalHotseat) {
            DeviceProfile dp = mActivity.getDeviceProfile();
            if (!dp.isVerticalBarLayout() && !dp.isTablet) {
                if (mDragSessionActive && mDragOverDock && mMakeRoomActive) {
                    // Oppo make-room / TypeToIn: countX = phantom slots. Dock-source keeps
                    // pre-lift cellW; external keeps packed-n metrics until first insert spring.
                    int n = collectPackableDockIcons(getShortcutsAndWidgets()).size();
                    int max = Math.max(1, dp.numShownHotseatIcons);
                    int slots;
                    if (mDockSourceDrag && mSessionMetricCount > 0) {
                        slots = Math.min(mSessionMetricCount, max);
                    } else {
                        slots = n >= max ? Math.min(Math.max(n, 1), max)
                                : Math.min(n + 1, max);
                    }
                    if (getCountX() != slots) {
                        setGridSize(slots, 1);
                    }
                    int metricCount;
                    if (mDockSourceDrag && mSessionMetricCount > 0) {
                        metricCount = mSessionMetricCount;
                    } else {
                        metricCount = mMakeRoomLayoutActive ? slots : Math.max(n, 1);
                    }
                    Rect basePad = dp.getHotseatLayoutPadding(getContext());
                    setPadding(basePad.left, basePad.top, basePad.right, basePad.bottom);
                    mAdaptiveSidePad = 0;
                    int widthSize = MeasureSpec.getSize(widthMeasureSpec);
                    int avail = widthSize - basePad.left - basePad.right;
                    if (avail > 0) {
                        int divisor = getAdaptiveCellDivisor(metricCount, dp.inv.numColumns,
                                dp.numShownHotseatIcons);
                        int cellW = avail / divisor;
                        int cellH = getCellHeight() > 0 ? getCellHeight() : dp.hotseatCellHeightPx;
                        setCellDimensions(cellW, cellH);
                        int used = metricCount > 0 ? metricCount * cellW : 0;
                        mAdaptiveSidePad = metricCount > 0
                                ? Math.max(0, (avail - used) / 2) : 0;
                        setPadding(basePad.left + mAdaptiveSidePad, basePad.top,
                                basePad.right + mAdaptiveSidePad, basePad.bottom);
                    }
                } else if (mPackPadAnimator != null && mPackPadAnimator.isRunning()) {
                    // Pack TypeToOut animator owns pad/cellW this frame.
                } else {
                    // Idle, dock-source pack, or after exit: pack+center visible icons.
                    // cellX must already be contiguous 0..n-1 (packRemainingIconsForDragOut /
                    // reflowIcons); only refresh adaptive pad + cellW here.
                    int n = mDragSessionActive
                            ? collectPackableDockIcons(getShortcutsAndWidgets()).size()
                            : collectDockIcons(getShortcutsAndWidgets()).size();
                    // TypeToOut / idle: metrics follow visible count (mid-drag == final).
                    int metricCount = n;
                    Rect basePad = dp.getHotseatLayoutPadding(getContext());
                    setPadding(basePad.left, basePad.top, basePad.right, basePad.bottom);
                    mAdaptiveSidePad = 0;
                    int widthSize = MeasureSpec.getSize(widthMeasureSpec);
                    int avail = widthSize - basePad.left - basePad.right;
                    if (avail > 0) {
                        int divisor = getAdaptiveCellDivisor(metricCount, dp.inv.numColumns,
                                dp.numShownHotseatIcons);
                        int cellW = avail / divisor;
                        int cellH = getCellHeight() > 0 ? getCellHeight() : dp.hotseatCellHeightPx;
                        setCellDimensions(cellW, cellH);
                        // Side pad centers the *visible* row (n), not the metric divisor.
                        int used = n > 0 ? n * cellW : 0;
                        mAdaptiveSidePad = n > 0 ? Math.max(0, (avail - used) / 2) : 0;
                        setPadding(basePad.left + mAdaptiveSidePad, basePad.top,
                                basePad.right + mAdaptiveSidePad, basePad.bottom);
                    }
                }
            }
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        DeviceProfile dp = mActivity.getDeviceProfile();
        mQsb.measure(MeasureSpec.makeMeasureSpec(dp.hotseatQsbWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(dp.hotseatQsbHeight, MeasureSpec.EXACTLY));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        int qsbMeasuredWidth = mQsb.getMeasuredWidth();
        int left;
        DeviceProfile dp = mActivity.getDeviceProfile();
        if (dp.isQsbInline) {
            int qsbSpace = dp.hotseatBorderSpace;
            left = Utilities.isRtl(getResources()) ? r - getPaddingRight() + qsbSpace
                    : l + getPaddingLeft() - qsbMeasuredWidth - qsbSpace;
        } else {
            left = (r - l - qsbMeasuredWidth) / 2;
        }
        int right = left + qsbMeasuredWidth;

        int bottom = b - t - dp.getQsbOffsetY();
        int top = bottom - dp.hotseatQsbHeight;
        mQsb.layout(left, top, right, bottom);
    }

    /**
     * Sets the alpha value of just our ShortcutAndWidgetContainer.
     */
    public void setIconsAlpha(float alpha) {
        getShortcutsAndWidgets().setAlpha(alpha);
    }

    /**
     * Sets the alpha value of just our QSB.
     */
    public void setQsbAlpha(float alpha) {
        mQsb.setAlpha(alpha);
    }

    public float getIconsAlpha() {
        return getShortcutsAndWidgets().getAlpha();
    }

    @Override
    public void setAlpha(float alpha) {
        super.setAlpha(alpha);
    }

    /**
     * Returns the QSB inside hotseat
     */
    public View getQsb() {
        return mQsb;
    }

    // ---- Phase C: Oppo-style insert-by-finger-index reorder --------------------

    private boolean shouldUseInsertReorder() {
        if (mHasVerticalHotseat) {
            return false;
        }
        DeviceProfile dp = mActivity.getDeviceProfile();
        return !dp.isTablet && !dp.isVerticalBarLayout();
    }

    /**
     * Oppo {@code getTargetCellx}: insertion index among current dock icon centers.
     */
    public int getTargetCellX(int pixelX) {
        return getTargetCellX(pixelX, collectDockIcons(getShortcutsAndWidgets()));
    }

    private int getTargetCellX(int pixelX, List<View> icons) {
        if (icons.isEmpty()) {
            return 0;
        }
        // Walk left→right by permanent cellX order. Mid-insert tmp/anim centers flicker
        // the insert index every frame and produce overlap (device v6_d_make_room).
        List<View> ordered = new ArrayList<>(icons);
        ordered.sort(Comparator.comparingInt(v -> {
            CellLayoutLayoutParams lp = (CellLayoutLayoutParams) v.getLayoutParams();
            return lp != null ? lp.getCellX() : 0;
        }));

        // Hit-test against packed-n centers, not the make-room n+1 grid pitch.
        // With slots=n+1, icons still live at cellX 0..n-1 on the left of a wider
        // grid — using that pitch left-shifts centers and opens the gap too far right
        // (Camera DragView overlaps Gallery while Settings alone jumps right).
        float[] centers = computePackedInsertCenters(ordered.size());
        int insert = 0;
        for (int i = 0; i < centers.length; i++) {
            if (centers[i] < pixelX) {
                insert++;
            } else {
                break;
            }
        }
        return insert;
    }

    /**
     * Centers for insert hit-testing: adaptive pack of {@code n} icons (idle metrics),
     * independent of the current make-room {@code n+1} cell width.
     */
    private float[] computePackedInsertCenters(int n) {
        float[] centers = new float[Math.max(0, n)];
        if (n <= 0) {
            return centers;
        }
        DeviceProfile dp = mActivity.getDeviceProfile();
        Rect basePad = dp.getHotseatLayoutPadding(getContext());
        int avail = getMeasuredWidth() - basePad.left - basePad.right;
        if (avail <= 0) {
            avail = dp.availableWidthPx - basePad.left - basePad.right;
        }
        int divisor = getAdaptiveCellDivisor(n, dp.inv.numColumns, dp.numShownHotseatIcons);
        int cellW = avail > 0 ? avail / Math.max(1, divisor) : Math.max(1, getCellWidth());
        int side = avail > 0 ? Math.max(0, (avail - n * cellW) / 2) : 0;
        float origin = basePad.left + side;
        for (int i = 0; i < n; i++) {
            centers[i] = origin + i * cellW + cellW / 2f;
        }
        return centers;
    }

    /** Visual / folder-merge center (may include tmp cell). */
    private float getIconCenterX(View v) {
        CellLayoutLayoutParams lp = (CellLayoutLayoutParams) v.getLayoutParams();
        int cellW = getCellWidth() > 0 ? getCellWidth() : mActivity.getDeviceProfile().iconSizePx;
        if (lp != null) {
            int cellX = lp.useTmpCoords ? lp.getTmpCellX() : lp.getCellX();
            return getPaddingLeft() + cellX * cellW + cellW / 2f;
        }
        return v.getLeft() + v.getWidth() / 2f;
    }

    /**
     * For folder merge: nearest occupied dock cell. Insert index is handled by
     * {@link #performReorder}.
     */
    @Override
    public int[] findNearestAreaIgnoreOccupied(int pixelX, int pixelY, int spanX, int spanY,
            int[] result) {
        if (!shouldUseInsertReorder()) {
            return super.findNearestAreaIgnoreOccupied(pixelX, pixelY, spanX, spanY, result);
        }
        if (result == null) {
            result = new int[2];
        }
        List<View> icons = collectDockIcons(getShortcutsAndWidgets());
        if (icons.isEmpty()) {
            result[0] = 0;
            result[1] = 0;
            return result;
        }
        double bestDist = Double.MAX_VALUE;
        int bestX = 0;
        for (View v : icons) {
            CellLayoutLayoutParams lp = (CellLayoutLayoutParams) v.getLayoutParams();
            if (lp == null) {
                continue;
            }
            int cellX = lp.useTmpCoords ? lp.getTmpCellX() : lp.getCellX();
            float cx = getIconCenterX(v);
            float cy = getPaddingTop() + getCellHeight() / 2f;
            double dist = Math.hypot(pixelX - cx, pixelY - cy);
            if (dist < bestDist) {
                bestDist = dist;
                bestX = cellX;
            }
        }
        result[0] = bestX;
        result[1] = 0;
        return result;
    }

    @Override
    int[] performReorder(int pixelX, int pixelY, int minSpanX, int minSpanY, int spanX, int spanY,
            View dragView, int[] result, int[] resultSpan, int mode) {
        if (!shouldUseInsertReorder()) {
            return super.performReorder(pixelX, pixelY, minSpanX, minSpanY, spanX, spanY,
                    dragView, result, resultSpan, mode);
        }
        if (resultSpan == null) {
            resultSpan = new int[]{-1, -1};
        }
        if (result == null) {
            result = new int[]{-1, -1};
        }
        // Dock-source still in TypeToOut: do not run calculateInsertSolution (it expands
        // the grid / opens a hole). Keep the packed row from onItemDragStartedFromDock.
        if (mDockSourceDrag && (!mHasLeftDockDuringDrag || mPackedAway)) {
            result[0] = result[1] = resultSpan[0] = resultSpan[1] = -1;
            return result;
        }

        ItemConfiguration solution = calculateInsertSolution(pixelX, dragView, spanX, spanY, mode);
        mPreviousSolution = (mode == MODE_ON_DROP || mode == MODE_ON_DROP_EXTERNAL)
                ? null : solution;
        if (solution == null || !solution.isSolution) {
            result[0] = result[1] = resultSpan[0] = resultSpan[1] = -1;
            return result;
        }
        result[0] = solution.cellX;
        result[1] = solution.cellY;
        resultSpan[0] = solution.spanX;
        resultSpan[1] = solution.spanY;
        performReorder(solution, dragView, mode);
        return result;
    }

    /**
     * Build an Oppo-style pack insert: drag occupies {@code insertIndex}, icons at/after
     * that index shift right by one. Dock-source return uses a LiveData-style phantom
     * gap (n+1) while the source View stays GONE.
     */
    private ItemConfiguration calculateInsertSolution(int pixelX, View dragView,
            int spanX, int spanY, int mode) {
        // Oppo: logical list excludes the dragged item (removed on TypeToOut). Always
        // use visible packable icons for dock-source; never let a GONE/INVISIBLE source
        // participate in neighbor layout.
        boolean dragInHotseat = dragView != null
                && dragView.getParent() == getShortcutsAndWidgets();
        List<View> icons = collectPackableDockIcons(getShortcutsAndWidgets());
        if (dragInHotseat) {
            icons.remove(dragView);
        }
        icons.sort(Comparator.comparingInt(v -> {
            CellLayoutLayoutParams lp = (CellLayoutLayoutParams) v.getLayoutParams();
            return lp != null ? lp.getCellX() : 0;
        }));

        int max = getMaxIconCount();
        boolean isNewIcon = !dragInHotseat && !mDockSourceDrag;
        if (isNewIcon && icons.size() >= max) {
            ItemConfiguration fail = new ItemConfiguration();
            fail.isSolution = false;
            return fail;
        }

        // Phantom gap: external drop OR dock-source TypeToIn while under max.
        boolean reserveInsertGap = isNewIcon || mDockSourceDrag;
        int slots;
        if (mDockSourceDrag && mSessionMetricCount > 0) {
            slots = Math.min(mSessionMetricCount, max);
        } else {
            slots = reserveInsertGap
                    ? Math.min(icons.size() + 1, max)
                    : Math.min(Math.max(icons.size(), getCountX()), max);
        }
        if (reserveInsertGap) {
            if (getCountX() != slots || getCountY() != 1) {
                setGridSize(slots, 1);
            }
            mMakeRoomActive = true;
            mPackedAway = false;
            ensureDragSourcesHidden();
        }

        int insert = getTargetCellX(pixelX, icons);
        insert = Math.max(0, Math.min(insert, icons.size()));
        if (slots > 0) {
            insert = Math.min(insert, slots - 1);
        }

        ItemConfiguration config = new ItemConfiguration();
        config.isSolution = true;
        config.cellX = insert;
        config.cellY = 0;
        config.spanX = Math.max(1, spanX);
        config.spanY = Math.max(1, spanY);

        for (int i = 0; i < icons.size(); i++) {
            View v = icons.get(i);
            int newX = i < insert ? i : i + 1;
            if (newX >= getCountX()) {
                setGridSize(Math.min(newX + 1, max), 1);
                mMakeRoomActive = true;
            }
            config.map.put(v, new CellAndSpan(newX, 0, 1, 1));
            config.sortedViews.add(v);
        }
        return config;
    }

    /** ColorOS dock: no AOSP hollow drop chip — gap is shown by neighbor spring only. */
    @Override
    public void visualizeDropLocation(int cellX, int cellY, int spanX, int spanY,
            DropTarget.DragObject dragObject) {
        clearDragOutlines();
        if (dragObject != null && dragObject.stateAnnouncer != null) {
            dragObject.stateAnnouncer.announce(getItemMoveDescription(cellX, cellY));
        }
    }

    /**
     * Oppo-style insert motion: slide neighbors with a soft spring, no shake preview.
     * Drop only commits the drag-over seats — re-springing here makes icons to the
     * right of the insert shuffle left/right before settling.
     */
    @Override
    public void performReorder(ItemConfiguration solution, View dragView, int mode) {
        if (!shouldUseInsertReorder()) {
            super.performReorder(solution, dragView, mode);
            return;
        }
        // Dock-source TypeToOut: keep the packed row until a real leave→re-enter.
        // Opening an insert hole here is what produced mtk.png (gap mid-drag).
        if (mDockSourceDrag && !mHasLeftDockDuringDrag) {
            return;
        }
        if (mDockSourceDrag && mPackedAway) {
            return;
        }
        if (mode == MODE_SHOW_REORDER_HINT || mode == MODE_ACCEPT_DROP) {
            return;
        }
        if (mode != MODE_DRAG_OVER && mode != MODE_ON_DROP && mode != MODE_ON_DROP_EXTERNAL) {
            super.performReorder(solution, dragView, mode);
            return;
        }

        setUseTempCoords(true);
        copySolutionToTempState(solution, dragView);
        setItemPlacementDirty(true);

        if (mode == MODE_DRAG_OVER) {
            animateInsertSpring(solution, dragView, /* commitDragView= */ false);
            // Skip requestLayout during DRAG_OVER — it can snap icons before springs
            // read their visual start positions.
            return;
        }

        // MODE_ON_DROP / MODE_ON_DROP_EXTERNAL: commit preview gap, do not re-target springs.
        ensureNeighborsAtSolutionSeats(solution, dragView);
        commitTempPlacement(dragView);
        completeAndClearReorderPreviewAnimations();
        setItemPlacementDirty(false);
        setUseTempCoords(false);
    }

    /**
     * After drop, keep neighbors on the drag-over insert seats. If a spring is already
     * running toward that X, leave it; otherwise snap animX to the committed cell.
     */
    private void ensureNeighborsAtSolutionSeats(ItemConfiguration solution, View dragView) {
        if (solution == null || solution.map == null) {
            return;
        }
        if (mMakeRoomActive && !mMakeRoomLayoutActive) {
            // Drop before the first drag-over spring activated n+1 metrics — finish that once.
            activateMakeRoomLayoutFromVisualStarts(
                    collectPackableDockIcons(getShortcutsAndWidgets()));
        }
        for (int i = 0; i < getShortcutsAndWidgets().getChildCount(); i++) {
            View child = getShortcutsAndWidgets().getChildAt(i);
            if (child == null || child == dragView || child.getVisibility() == GONE) {
                continue;
            }
            CellAndSpan c = solution.map.get(child);
            if (c == null) {
                continue;
            }
            CellLayoutLayoutParams lp = (CellLayoutLayoutParams) child.getLayoutParams();
            if (lp == null) {
                continue;
            }
            lp.setTmpCellX(c.cellX);
            lp.setTmpCellY(c.cellY);
            lp.useTmpCoords = true;
            lp.isHotseatChild = true;
            int targetX = mSpringMotion.computeTargetX(child);
            if (mSpringMotion.isChildMoving(child)) {
                // Already springing toward the gap from drag-over — keep that motion.
                mSpringMotion.animateAnimXTo(child, targetX);
            } else if (Math.abs(lp.animX - targetX) > 1) {
                // Rare: drop without a prior drag-over spring (e.g. quick flick).
                mSpringMotion.seedAnimFromLayout(child);
                mSpringMotion.animateAnimXTo(child, targetX);
            } else {
                lp.animX = targetX;
                lp.x = targetX;
                lp.isLockedToGrid = false;
            }
        }
    }

    private void animateInsertSpring(ItemConfiguration solution, View dragView,
            boolean commitDragView) {
        if (solution == null || solution.map == null) {
            return;
        }
        // Oppo: source never drawn in the dock during TypeToIn — only DragView.
        if (mDockSourceDrag) {
            ensureDragSourcesHidden();
            if (dragView != null && dragView.getParent() == getShortcutsAndWidgets()) {
                dragView.setVisibility(GONE);
                if (!mHiddenDragSources.contains(dragView)) {
                    mHiddenDragSources.add(dragView);
                }
            }
        }
        List<View> movers = new ArrayList<>();
        for (int i = 0; i < getShortcutsAndWidgets().getChildCount(); i++) {
            View child = getShortcutsAndWidgets().getChildAt(i);
            if (child == dragView || solution.map.get(child) == null) {
                continue;
            }
            if (child.getVisibility() == GONE) {
                continue;
            }
            movers.add(child);
        }
        // First insert after pack: switch to n+1 metrics from seats the user still sees
        // (Oppo animX seed before cellW/pad change).
        if (mMakeRoomActive && !mMakeRoomLayoutActive) {
            activateMakeRoomLayoutFromVisualStarts(
                    collectPackableDockIcons(getShortcutsAndWidgets()));
        } else {
            for (View child : movers) {
                if (!mSpringMotion.isMoving()) {
                    mSpringMotion.seedAnimFromLayout(child);
                }
            }
        }
        for (View child : movers) {
            CellAndSpan c = solution.map.get(child);
            if (c == null) {
                continue;
            }
            CellLayoutLayoutParams lp = (CellLayoutLayoutParams) child.getLayoutParams();
            if (lp == null) {
                continue;
            }
            lp.setTmpCellX(c.cellX);
            lp.setTmpCellY(c.cellY);
            lp.useTmpCoords = true;
            lp.isHotseatChild = true;
            int targetX = mSpringMotion.computeTargetX(child);
            mSpringMotion.animateAnimXTo(child, targetX);
        }
        if (commitDragView) {
            mTmpOccupied.markCells(solution, true);
        }
    }

}
