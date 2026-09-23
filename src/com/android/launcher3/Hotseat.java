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

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.android.launcher3.celllayout.CellLayoutLayoutParams;
import com.android.launcher3.model.data.ItemInfo;

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
 *       cells (no adaptive side pad / mid-drag reflow) so drop targets stay stable.</li>
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
     * Prevents leave/re-enter from posting a mid-drag {@link #reflowIcons()} shrink.
     */
    private boolean mDragSessionActive;

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
     * While dragging over the dock, expand to max slots with equal cell width so
     * empty ranks are valid drop targets (Oppo expands during pre-drop).
     */
    @Override
    public void onDragEnter() {
        super.onDragEnter();
        beginDragSession();
        if (mHasVerticalHotseat) {
            return;
        }
        DeviceProfile dp = mActivity.getDeviceProfile();
        if (dp.isTablet || dp.isVerticalBarLayout()) {
            return;
        }
        applyExpandedDragMetrics(/* remakeOccupied= */ true);
    }

    @Override
    public void onDragExit() {
        super.onDragExit();
        // Do not reflow here — leave/re-enter would shrink mid-drag. Workspace calls
        // {@link #endDragSession()} after drop/cancel settles.
    }

    /**
     * Ends the dock drag session and restores the packed/centered idle layout.
     * Call once from Workspace after drop completed or cancelled.
     */
    public void endDragSession() {
        if (!mDragSessionActive) {
            // Still reflow — covers remove-from-hotseat and non-session callers.
            removeCallbacks(mReflowRunnable);
            reflowIcons();
            return;
        }
        mDragSessionActive = false;
        removeCallbacks(mReflowRunnable);
        reflowIcons();
    }

    private void beginDragSession() {
        if (mDragSessionActive) {
            return;
        }
        mDragSessionActive = true;
        removeCallbacks(mReflowRunnable);
    }

    private void applyExpandedDragMetrics(boolean remakeOccupied) {
        DeviceProfile dp = mActivity.getDeviceProfile();
        int max = Math.max(1, dp.numShownHotseatIcons);
        if (getCountX() != max) {
            setGridSize(max, 1);
        }
        mAdaptiveSidePad = 0;
        Rect basePad = dp.getHotseatLayoutPadding(getContext());
        setPadding(basePad.left, basePad.top, basePad.right, basePad.bottom);
        int avail = getMeasuredWidth() - basePad.left - basePad.right;
        if (avail <= 0) {
            avail = dp.availableWidthPx - basePad.left - basePad.right;
        }
        if (avail > 0) {
            int cellH = getCellHeight() > 0 ? getCellHeight() : dp.hotseatCellHeightPx;
            setCellDimensions(avail / max, cellH);
        }
        if (remakeOccupied) {
            mOccupied.clear();
            for (View v : collectDockIcons(getShortcutsAndWidgets())) {
                markCellsAsOccupiedForView(v);
            }
            requestLayout();
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

        int divisor = getAdaptiveCellDivisor(iconCount, dp.inv.numColumns, dp.numShownHotseatIcons);
        int cellW = avail / divisor;
        int cellH = getCellHeight() > 0 ? getCellHeight() : dp.hotseatCellHeightPx;
        setCellDimensions(cellW, cellH);

        int used = iconCount > 0 ? iconCount * cellW : 0;
        mAdaptiveSidePad = iconCount > 0 ? Math.max(0, (avail - used) / 2) : 0;
        setPadding(
                basePad.left + mAdaptiveSidePad,
                basePad.top,
                basePad.right + mAdaptiveSidePad,
                basePad.bottom);
    }

    /**
     * Persistent dock icons only — skips predictions and views without ItemInfo.
     */
    private static List<View> collectDockIcons(ShortcutAndWidgetContainer container) {
        List<View> icons = new ArrayList<>();
        if (container == null) {
            return icons;
        }
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child == null || child.getVisibility() == View.GONE) {
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
                if (mDragSessionActive) {
                    // Keep expanded equal-width cells; do not restore idle side pad.
                    // Use the measure width — getMeasuredWidth() may still be stale here.
                    DeviceProfile dragDp = mActivity.getDeviceProfile();
                    int max = Math.max(1, dragDp.numShownHotseatIcons);
                    if (getCountX() != max) {
                        setGridSize(max, 1);
                    }
                    mAdaptiveSidePad = 0;
                    Rect basePad = dragDp.getHotseatLayoutPadding(getContext());
                    setPadding(basePad.left, basePad.top, basePad.right, basePad.bottom);
                    int widthSize = MeasureSpec.getSize(widthMeasureSpec);
                    int avail = widthSize - basePad.left - basePad.right;
                    if (avail > 0) {
                        int cellH = getCellHeight() > 0
                                ? getCellHeight() : dragDp.hotseatCellHeightPx;
                        setCellDimensions(avail / max, cellH);
                    }
                } else {
                    int n = collectDockIcons(getShortcutsAndWidgets()).size();
                    // Temporarily clear side pad so avail width is correct, then re-apply.
                    Rect basePad = dp.getHotseatLayoutPadding(getContext());
                    setPadding(basePad.left, basePad.top, basePad.right, basePad.bottom);
                    mAdaptiveSidePad = 0;
                    int widthSize = MeasureSpec.getSize(widthMeasureSpec);
                    int avail = widthSize - basePad.left - basePad.right;
                    if (avail > 0) {
                        int divisor = getAdaptiveCellDivisor(n, dp.inv.numColumns,
                                dp.numShownHotseatIcons);
                        int cellW = avail / divisor;
                        int cellH = getCellHeight() > 0 ? getCellHeight() : dp.hotseatCellHeightPx;
                        setCellDimensions(cellW, cellH);
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

}
