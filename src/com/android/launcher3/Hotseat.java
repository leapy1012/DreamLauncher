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
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.android.launcher3.celllayout.CellLayoutLayoutParams;
import com.android.launcher3.hotseat.HotseatSpringMotion;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.model.data.WorkspaceItemInfo;
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
     * Last make-room insert map from {@link #MODE_DRAG_OVER}. Survives
     * {@link #onDragExit()} / {@link CellLayout#mPreviousSolution} clear so
     * {@link #MODE_ON_DROP} can still seat the icon in the hovered gap.
     */
    private ItemConfiguration mPendingDropSolution;

    /**
     * True after {@link #prepareFinalDropLanding(View)} until the DragView flight
     * finishes and the dropped child is visible again. Holds make-room metrics so
     * {@link #onMeasure} does not pack n−1 while the child is {@link View#INVISIBLE}.
     */
    private boolean mDropLandingPrepared;

    /**
     * While true, layout must not overwrite {@code animX} with grid X (drop flight /
     * make-room springs own the visual seats). Idle never sets this — otherwise icons
     * stick at a stale left edge and the dock fails to center.
     */
    public boolean shouldHoldVisualSeats() {
        if (mDropLandingPrepared) {
            return true;
        }
        // Field may be read during early measure; never block idle centering.
        if (mSpringMotion == null) {
            return false;
        }
        return (mMakeRoomActive || mPackedAway) && mSpringMotion.isMoving();
    }

    private final Runnable mFinishDropLandingRunnable = this::finishDropLandingIfReady;

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
        // Mid-drag TypeToOut / make-room / drop-flight still owns animX — keep clip
        // off so a settled spring cannot hard-cut neighbors that are still outside
        // the pad (or mid DragView flight after mDragSessionActive cleared).
        if (mDragSessionActive || mDropLandingPrepared) {
            return;
        }
        setHotseatClipEnabled(true);
        // Lock seats that have reached their grid x so the next measureChild does not
        // fight animX (and so unlocked make-room seats can finally sync).
        ShortcutAndWidgetContainer container = getShortcutsAndWidgets();
        if (container == null) {
            return;
        }
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child == null || child.getVisibility() == GONE) {
                continue;
            }
            CellLayoutLayoutParams lp = (CellLayoutLayoutParams) child.getLayoutParams();
            if (lp == null || !lp.isHotseatChild) {
                continue;
            }
            int targetX = mSpringMotion.computeTargetX(child);
            if (Math.abs(lp.animX - targetX) <= 2) {
                lp.animX = targetX;
                lp.x = targetX;
                lp.isLockedToGrid = true;
            }
        }
        container.requestLayout();
    }

    /**
     * Oppo {@code OplusHotseat.isMoving()}: ShortcutAndWidgetContainer lays out from
     * {@link CellLayoutLayoutParams#animX} while springs own the seat.
     */
    public boolean isIconSpringMoving() {
        return mSpringMotion.isMoving();
    }

    /** True while {@code child} has an in-flight dock spring. */
    public boolean isChildIconSpringMoving(View child) {
        return mSpringMotion.isChildMoving(child);
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
        mPendingDropSolution = null;
        mDropLandingPrepared = false;
        removeCallbacks(mFinishDropLandingRunnable);
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
     * When true, {@link #reflowIcons()} is a no-op. Used while dissolving a hotseat folder
     * so remaining icons are not packed into the folder seat before the final item is
     * re-inserted (that collision hid Cleanup under Notes).
     */
    private boolean mDeferReflow;

    /** Defer idle pack until {@link #endDeferReflow()} (folder → single-icon replace). */
    public void beginDeferReflow() {
        mDeferReflow = true;
    }

    /** Clear deferral and pack now that the replacement icon is in the dock. */
    public void endDeferReflow() {
        mDeferReflow = false;
        // Folder dissolve can finish after a stale drag session (folder was drag source).
        // Force a real pack so Notes and neighbors get unique seats.
        if (mDragSessionActive) {
            mDragSessionActive = false;
            restoreHiddenDragSources();
            mDragOverDock = false;
            mSessionMetricCount = 0;
            mMakeRoomActive = false;
            mMakeRoomLayoutActive = false;
            mPackedAway = false;
            mSpringMotion.endAll();
        }
        reflowIcons();
    }

    /**
     * Oppo-style: reflow dock icons into a contiguous centered row and apply adaptive
     * cell width. Safe to call after bind, drop, or remove. No-op during an active drag
     * session so expand geometry stays stable.
     */
    public void reflowIcons() {
        if (mDeferReflow || mDragSessionActive) {
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
        mPendingDropSolution = null;
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
        // Defer applyMakeRoomMetrics until drag-over confirms we are not over an
        // icon for folder-merge (otherwise the target springs out from under the
        // finger and folder create fails when n < max).
        mPackedAway = false;
        mMakeRoomActive = true;
        mMakeRoomLayoutActive = false;
        if (mDockSourceDrag) {
            applyMakeRoomMetrics();
        }
    }

    @Override
    public void onDragExit() {
        // Finger-up over the dock: Workspace.onDragExit runs BEFORE onDrop and
        // clears CellLayout.mPreviousSolution via super.onDragExit(). Persist the
        // last insert map so ON_DROP still seats into the hovered gap.
        if (mPreviousSolution != null && mPreviousSolution.isSolution) {
            mPendingDropSolution = mPreviousSolution;
        }
        if (mDragSessionActive) {
            completeAndClearReorderPreviewAnimations();
            cancelReorderAnimators();
            setItemPlacementDirty(false);
            setUseTempCoords(false);
        }
        super.onDragExit();
        // Do NOT pack here. Packing before onDrop collapses make-room padding so a
        // recalculated insert maps to cell 0. Mid-drag leave is handled by
        // Workspace → onFingerAwayFromDock(); drop/cancel cleanup is endDragSession().
    }

    /**
     * Workspace{@link Workspace#setCurrentDropLayout} calls {@code revertTempState()}
     * <em>before</em> {@link #onDragExit()}. AOSP then runs
     * {@link #animateChildToPosition} back to the pre-insert cellX, which layouts via
     * {@code lp.x} and slides the rightmost dock icon off its make-room seat (visible
     * vanish / retarget for ~1 frame after drop). Insert springs already own the seats.
     */
    @Override
    void revertTempState() {
        if (shouldUseInsertReorder()) {
            completeAndClearReorderPreviewAnimations();
            cancelReorderAnimators();
            setItemPlacementDirty(false);
            return;
        }
        super.revertTempState();
    }

    private void dumpSeats(String where) {
        ShortcutAndWidgetContainer container = getShortcutsAndWidgets();
        if (container == null) {
            Log.i("HSDrop", where + " container=null");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(where)
                .append(" sess=").append(mSessionMetricCount)
                .append(" makeRoom=").append(mMakeRoomActive)
                .append("/").append(mMakeRoomLayoutActive)
                .append(" dropPrep=").append(mDropLandingPrepared)
                .append(" dragSess=").append(mDragSessionActive)
                .append(" countX=").append(getCountX())
                .append(" cellW=").append(getCellWidth())
                .append(" padL=").append(getPaddingLeft())
                .append(" sidePad=").append(mAdaptiveSidePad)
                .append(" |");
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child == null) {
                continue;
            }
            CellLayoutLayoutParams lp = (CellLayoutLayoutParams) child.getLayoutParams();
            Object tag = child.getTag();
            String name = tag instanceof ItemInfo info && info.title != null
                    ? info.title.toString() : "?";
            int cell = lp != null ? lp.getCellX() : -1;
            int anim = lp != null ? lp.animX : -1;
            int x = lp != null ? lp.x : -1;
            boolean locked = lp != null && lp.isLockedToGrid;
            sb.append(' ').append(name)
                    .append("{vis=").append(child.getVisibility())
                    .append(" cell=").append(cell)
                    .append(" animX=").append(anim)
                    .append(" x=").append(x)
                    .append(" lock=").append(locked)
                    .append('}');
        }
        Log.i("HSDrop", sb.toString());
    }

    /**
     * Ends the dock drag session and restores the packed/centered idle layout.
     */
    public void endDragSession() {
        // Workspace.onDropCompleted and onDragEnd both call this; skip a second
        // reflow that would retarget neighbors after seats already settled.
        if (!mDragSessionActive && !mDropLandingPrepared && !mMakeRoomActive
                && mSessionMetricCount == 0 && mHiddenDragSources.isEmpty()) {
            return;
        }
        dumpSeats("endDragSession:enter");
        cancelPackPadAnimator();
        removeCallbacks(mReflowRunnable);
        mPendingDropSolution = null;
        mDockSourceDrag = false;
        mHasLeftDockDuringDrag = false;
        mPackedAway = false;

        // onDropCompleted runs in the same stack as onDrop — BEFORE DragView finishes.
        // Keep make-room metrics until the dropped child is visible again.
        if (mDropLandingPrepared) {
            // Drop flight owns source visibility; do not force VISIBLE mid-flight.
            mHiddenDragSources.clear();
            mDragSessionActive = false;
            removeCallbacks(mFinishDropLandingRunnable);
            postDelayed(mFinishDropLandingRunnable, 32);
            return;
        }

        // Cancel / external drop: un-hide any dock seats we GONE'd for TypeToOut pack.
        restoreHiddenDragSources();

        mDragOverDock = false;
        mSessionMetricCount = 0;
        mMakeRoomActive = false;
        mMakeRoomLayoutActive = false;
        mSpringMotion.endAll();
        if (!mDragSessionActive) {
            reflowIconsAfterDrop();
            return;
        }
        mDragSessionActive = false;
        reflowIconsAfterDrop();
    }

    /**
     * After DragView flight: leave make-room measure path. Reposts while the dropped
     * child is still {@link View#INVISIBLE}.
     * <p>Oppo success path does not retarget to a <em>new</em> seat — but it also does
     * not freeze mid-spring. draganddrop can commit before make-room springs reach
     * their finals; we must keep driving {@code animX} to the session-locked idle
     * target (same pixel as hover), never {@code clearAll}+lock at the in-between X.
     */
    private void finishDropLandingIfReady() {
        if (!mDropLandingPrepared) {
            return;
        }
        dumpSeats("finishDropLanding:check");
        ShortcutAndWidgetContainer container = getShortcutsAndWidgets();
        if (container != null) {
            for (int i = 0; i < container.getChildCount(); i++) {
                View child = container.getChildAt(i);
                if (child == null || child.getVisibility() != INVISIBLE) {
                    continue;
                }
                Object tag = child.getTag();
                if (tag instanceof ItemInfo info && isPersistentHotseatItem(info)) {
                    postDelayed(mFinishDropLandingRunnable, 32);
                    return;
                }
            }
        }

        List<View> icons = collectDockIcons(container);
        icons.sort(Comparator.comparingInt(v -> {
            CellLayoutLayoutParams lp = (CellLayoutLayoutParams) v.getLayoutParams();
            return lp != null ? lp.getCellX() : 0;
        }));

        mDropLandingPrepared = false;
        mDragOverDock = false;
        mMakeRoomActive = false;
        mMakeRoomLayoutActive = false;
        mPackedAway = false;

        int n = icons.size();
        int max = Math.max(1, mActivity.getDeviceProfile().numShownHotseatIcons);
        int gridX = n > 0 ? Math.min(n, max) : max;

        // Capture screen seats BEFORE clearing session metrics / cellW — otherwise
        // applyAdaptiveCellMetrics jumps neighbors to a new pitch (visible retarget).
        ArrayMap<View, int[]> origins = captureVisualOrigins(icons);
        int lockedMetric = mSessionMetricCount > 0
                ? Math.min(mSessionMetricCount, max) : n;
        mSessionMetricCount = 0;

        if (getCountX() != gridX || getCountY() != 1) {
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
                            info, CONTAINER_HOTSEAT, i, i, 0);
                }
            }
            markCellsAsOccupiedForView(v);
        }

        // Prefer hover-locked pitch when it matches final count (no cellW change).
        int metric = (lockedMetric == n) ? lockedMetric : n;
        applyAdaptiveCellMetrics(metric);
        seedAnimFromCapturedScreens(origins);

        // Snap to grid seats. With locked metric, seed already matches — no visible jump.
        // Never leave stale animX (that stuck Music on the far left).
        for (View v : icons) {
            CellLayoutLayoutParams lp = (CellLayoutLayoutParams) v.getLayoutParams();
            if (lp == null) {
                continue;
            }
            lp.isHotseatChild = true;
            lp.useTmpCoords = false;
            int targetX = mSpringMotion.computeTargetX(v);
            lp.animX = targetX;
            lp.x = targetX;
            lp.isLockedToGrid = true;
        }
        mSpringMotion.clearAll();
        setHotseatClipEnabled(true);
        dumpSeats("finishDropLanding:done");
        requestLayout();
    }

    /**
     * Oppo: on drop, make-room seats are already final — do not retarget neighbors.
     * Only set the dropped child's {@code lp.x} for {@code animateViewIntoPosition}.
     * Keep make-room measure active through DragView flight (child is INVISIBLE).
     */
    public void prepareFinalDropLanding(View dropped) {
        removeCallbacks(mFinishDropLandingRunnable);
        mDropLandingPrepared = true;
        dumpSeats("prepareFinalDropLanding:start");
        if (mHasVerticalHotseat || dropped == null) {
            return;
        }
        DeviceProfile dp = mActivity.getDeviceProfile();
        if (dp.isVerticalBarLayout() || dp.isTablet) {
            return;
        }
        ShortcutAndWidgetContainer container = getShortcutsAndWidgets();
        if (container == null) {
            return;
        }
        mMakeRoomActive = true;
        mDragOverDock = true;
        if (mSessionMetricCount <= 0) {
            int n = collectDockIcons(container).size();
            mSessionMetricCount = Math.max(1, Math.min(n, dp.numShownHotseatIcons));
        }
        if (!mMakeRoomLayoutActive) {
            activateMakeRoomLayoutFromVisualStarts(
                    collectPackableDockIcons(container));
        }

        CellLayoutLayoutParams lp = (CellLayoutLayoutParams) dropped.getLayoutParams();
        if (lp != null) {
            lp.isHotseatChild = true;
            lp.useTmpCoords = false;
            lp.isLockedToGrid = true;
            container.setupLp(dropped);
            lp.animX = lp.x;
            lp.animY = lp.y;
        }
        dumpSeats("prepareFinalDropLanding:done");
    }

    /**
     * Idle pack after drop/cancel: update cell ranks/metrics but keep icons on their
     * current screen seats when the idle grid matches (no second spring).
     */
    private void reflowIconsAfterDrop() {
        reflowIconsAfterDrop(/* animateNeighbors= */ true);
    }

    private void reflowIconsAfterDrop(boolean animateNeighbors) {
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
        if (getCountX() != gridX || getCountY() != 1) {
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
        seedAnimFromCapturedScreens(origins);
        // Idle pack must correct seats. Prefer snap over spring to avoid a second
        // retarget animation; seed keeps continuity when pitch is unchanged.
        for (View v : icons) {
            CellLayoutLayoutParams lp = (CellLayoutLayoutParams) v.getLayoutParams();
            if (lp == null) {
                continue;
            }
            lp.isHotseatChild = true;
            int targetX = mSpringMotion.computeTargetX(v);
            lp.animX = targetX;
            lp.x = targetX;
            lp.isLockedToGrid = true;
        }
        mSpringMotion.clearAll();
        requestLayout();
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
        // Leaving for workspace mid-drag — hover insert is no longer valid.
        mPendingDropSolution = null;
        packRemainingIconsForDragOut(/* animate= */ true);
    }

    /**
     * Oppo canMergeFolder: nearest dock icon under the finger for folder create/add.
     * Make-room vacant cells make {@link #getChildAt(int, int)} miss the real target
     * when {@code n < max}, so folder create only worked on a full dock.
     */
    @Nullable
    public View findFolderMergeTarget(float pixelX, float pixelY, ItemInfo dragInfo) {
        if (dragInfo == null
                || dragInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_FOLDER) {
            return null;
        }
        List<View> icons = collectPackableDockIcons(getShortcutsAndWidgets());
        if (icons.isEmpty()) {
            return null;
        }
        DeviceProfile dp = mActivity.getDeviceProfile();
        // At least half a cell — make-room springs can shift the target ~0.4 cell
        // before folder mode suppresses insert; stay hittable under the finger.
        float radius = Math.max(dp.iconSizePx * 0.75f, getCellWidth() * 0.5f);
        View best = null;
        double bestDist = radius;
        for (View v : icons) {
            Object tag = v.getTag();
            if (!(tag instanceof WorkspaceItemInfo)
                    || !isPersistentHotseatItem((ItemInfo) tag)) {
                continue;
            }
            float cx = getIconCenterX(v);
            float cy = getPaddingTop() + getCellHeight() / 2f;
            double dist = Math.hypot(pixelX - cx, pixelY - cy);
            if (dist <= bestDist) {
                bestDist = dist;
                best = v;
            }
        }
        return best;
    }

    /**
     * Oppo canMergeFolder preview helper (kept for diagnostics). Prefer Workspace
     * folder drag-mode for suppressing insert — a wide radius falsely covers the
     * whole dock and blocks make-room.
     */
    public boolean isFolderMergeLikely(float pixelX, float pixelY, ItemInfo dragInfo) {
        return findFolderMergeTarget(pixelX, pixelY, dragInfo) != null;
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
        mSessionMetricCount = 0;
        // Instant pack — springing here shuffles right icons while entering folder mode.
        packRemainingIconsForDragOut(/* animate= */ false);
    }

    /**
     * Workspace→hotseat make-room: expand countX to n+1 only. Keep packed-n cell metrics
     * until the first insert spring so icons do not jump away from their on-screen seats.
     * Locks {@link #mSessionMetricCount} to the post-drop icon count so hover seats and
     * idle land seats share one cellW/pad (avoids drop ping-pong retargets).
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
            // External: final idle count after this drop is slots — lock pitch now.
            if (!mDockSourceDrag && mSessionMetricCount <= 0) {
                mSessionMetricCount = slots;
            }
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
        if (mSessionMetricCount > 0) {
            slots = Math.min(mSessionMetricCount, max);
        } else if (mDockSourceDrag) {
            slots = n >= max ? Math.min(n, max) : Math.min(n + 1, max);
        } else {
            slots = n >= max ? Math.min(n, max) : Math.min(n + 1, max);
            mSessionMetricCount = slots;
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
        mPackedAway = false;
        mMakeRoomActive = true;
        // onDragEnter may have flagged make-room without applying metrics (folder-merge
        // deferral). Apply once we know this is an insert, not a folder hover.
        if (!mMakeRoomLayoutActive) {
            applyMakeRoomMetrics();
        }
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

    /**
     * Restore seats still parented in this hotseat after a cancelled / aborted drag.
     * Views that left the dock (successful move) are left alone.
     */
    private void restoreHiddenDragSources() {
        ShortcutAndWidgetContainer container = getShortcutsAndWidgets();
        for (View parked : mHiddenDragSources) {
            if (parked == null) {
                continue;
            }
            if (container != null && parked.getParent() == container
                    && parked.getVisibility() != VISIBLE) {
                parked.setVisibility(VISIBLE);
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
                if ((mDragSessionActive && mDragOverDock && mMakeRoomActive)
                        || (mDropLandingPrepared && mMakeRoomActive)) {
                    // Oppo make-room / drop-flight: countX = phantom slots. Session-locked
                    // metricCount keeps hover cellW == post-drop idle cellW. During
                    // DragView flight mDragSessionActive is already false — still use
                    // this branch while mDropLandingPrepared so INVISIBLE drop child
                    // does not shrink the row to n−1.
                    int n = collectPackableDockIcons(getShortcutsAndWidgets()).size();
                    int max = Math.max(1, dp.numShownHotseatIcons);
                    int slots;
                    if (mSessionMetricCount > 0) {
                        slots = Math.min(mSessionMetricCount, max);
                    } else {
                        slots = n >= max ? Math.min(Math.max(n, 1), max)
                                : Math.min(n + 1, max);
                    }
                    if (getCountX() != slots) {
                        setGridSize(slots, 1);
                    }
                    int metricCount;
                    if (mSessionMetricCount > 0 && mMakeRoomLayoutActive) {
                        metricCount = mSessionMetricCount;
                    } else if (mDockSourceDrag && mSessionMetricCount > 0) {
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
                        if (mDropLandingPrepared || mMakeRoomActive) {
                            Log.i("HSDrop", "onMeasure makeRoom/dropPrep metric="
                                    + metricCount + " nPack=" + n
                                    + " cellW=" + cellW + " sidePad=" + mAdaptiveSidePad
                                    + " padL=" + getPaddingLeft());
                        }
                    }
                } else if (mPackPadAnimator != null && mPackPadAnimator.isRunning()) {
                    // Pack TypeToOut animator owns pad/cellW this frame.
                } else {
                    // Idle / TypeToOut / post-drop. During DragView flight the dropped
                    // child is INVISIBLE — never use packable-only count while a session
                    // metric lock is active (that packed n−1 and retargeted neighbors).
                    int max = Math.max(1, dp.numShownHotseatIcons);
                    int n;
                    int metricCount;
                    if (mDropLandingPrepared && mSessionMetricCount > 0) {
                        n = collectDockIcons(getShortcutsAndWidgets()).size();
                        metricCount = Math.min(mSessionMetricCount, max);
                        n = Math.max(n, metricCount);
                    } else if (mDragSessionActive) {
                        n = collectPackableDockIcons(getShortcutsAndWidgets()).size();
                        metricCount = Math.max(n, 1);
                    } else {
                        n = collectDockIcons(getShortcutsAndWidgets()).size();
                        metricCount = n;
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
        // Prefer the drawn seat (animX). Grid cell centers miss during make-room springs
        // and blocked folder-merge hit tests when n < max.
        if (lp != null && lp.isHotseatChild) {
            int w = lp.width > 0 ? lp.width
                    : (getCellWidth() > 0 ? getCellWidth()
                    : mActivity.getDeviceProfile().iconSizePx);
            return getPaddingLeft() + lp.animX + w / 2f;
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

        // Finger-up clears mPreviousSolution in onDragExit before onDrop. Prefer the
        // pending hover insert so the icon seats in the gap the user already saw —
        // recalculating after a pack maps the same screen point to cell 0.
        ItemConfiguration solution;
        boolean isDrop = mode == MODE_ON_DROP || mode == MODE_ON_DROP_EXTERNAL;
        if (isDrop && mPendingDropSolution != null && mPendingDropSolution.isSolution) {
            solution = mPendingDropSolution;
        } else if (isDrop && mPreviousSolution != null && mPreviousSolution.isSolution) {
            solution = mPreviousSolution;
        } else {
            solution = calculateInsertSolution(pixelX, dragView, spanX, spanY, mode);
        }
        if (mode == MODE_DRAG_OVER) {
            mPreviousSolution = solution;
            if (solution != null && solution.isSolution) {
                mPendingDropSolution = solution;
            }
        } else if (isDrop) {
            mPreviousSolution = null;
            mPendingDropSolution = null;
        }
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
        if (mSessionMetricCount > 0) {
            slots = Math.min(mSessionMetricCount, max);
        } else {
            slots = reserveInsertGap
                    ? Math.min(icons.size() + 1, max)
                    : Math.min(Math.max(icons.size(), getCountX()), max);
            if (reserveInsertGap && !mDockSourceDrag) {
                mSessionMetricCount = slots;
            }
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
        cancelReorderAnimators();
        ensureNeighborsAtSolutionSeats(solution, dragView);
        commitTempPlacement(dragView);
        completeAndClearReorderPreviewAnimations();
        setItemPlacementDirty(false);
        setUseTempCoords(false);
    }

    /**
     * After drop, bake the drag-over insert map into cellX. Do <b>not</b> call
     * {@link HotseatSpringMotion#animateAnimXTo} — that retargets right-side icons
     * that are already on (or springing to) the make-room seat.
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
            lp.setCellX(c.cellX);
            lp.setCellY(c.cellY);
            lp.useTmpCoords = false;
            lp.isHotseatChild = true;
            // Keep hover animX when already on the committed seat; otherwise snap.
            int targetX = mSpringMotion.computeTargetX(child);
            if (Math.abs(lp.animX - targetX) <= 4) {
                lp.animX = targetX;
                lp.x = targetX;
            } else {
                // Do not spring — but do snap to the committed seat (avoid stale animX).
                lp.animX = targetX;
                lp.x = targetX;
            }
            lp.isLockedToGrid = true;
        }
        dumpSeats("ensureNeighbors:done");
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
