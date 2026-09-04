/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.quickstep.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.quickstep.RecentsModel;
import com.android.quickstep.TaskIconCache;
import com.android.systemui.shared.recents.model.Task;
import com.android.systemui.shared.system.ActivityManagerWrapper;

import java.util.Arrays;

/**
 * Oppo-style horizontal strip of recent-task icons under overview cards.
 * <p>
 * Order matches the task carousel left→right (ColorOS {@code DockView.applyLoadPlan}
 * reverse-bind when Recents is RTL). Curve scale/dim from {@code DockIconView}:
 * centered icon scale 1.0 / undimmed; off-center down to 0.87 / 0.2 scrim.
 * Icons are rasterized to a private bitmap so TaskView {@link IconView} cannot shrink them.
 * <p>
 * Bidirectional link-scroll mirrors ColorOS {@code DockView} + {@code LinkScrollState}:
 * {@code dockScroll = dockOrigin + (recentsScroll - recentsOrigin) / mScrollScale} and the
 * inverse when the dock drives. No smoothScroll — per-frame {@code scrollTo} like Oppo.
 */
public class OverviewDockView extends HorizontalScrollView {

    private static final int MAX_ICONS = 20;

    /** ColorOS {@code DockIconView.EDGE_SCALE_DOWN_FACTOR}. */
    private static final float EDGE_SCALE_DOWN_FACTOR = 0.13f;
    /** ColorOS {@code DockIconView.MAX_PAGE_SCRIM_ALPHA}. */
    private static final float MAX_PAGE_SCRIM_ALPHA = 0.2f;

    /** ColorOS {@code LinkScrollState}: neither side is driving. */
    private static final int LINK_UNLINK = -1;
    /** ColorOS {@code LinkScrollState.RECENTS_SCROLL}: Recents drives dock. */
    private static final int LINK_RECENTS = 0;
    /** ColorOS {@code LinkScrollState.DOCK_SCROLL}: dock drives Recents. */
    private static final int LINK_DOCK = 1;

    private final LinearLayout mContainer;
    private final int mIconSize;
    private final int mIconSpacing;
    private final int mDimColor;

    @Nullable
    private RecentsView<?, ?> mRecentsView;
    private int[] mBoundTaskIds = new int[0];
    /** Dock child index currently scrolled to center (visual LTR order). */
    private int mCenteredChildIndex = -1;
    /** When true, Recents child index 0 is on the right — dock must reverse-bind. */
    private boolean mRecentsRtl;
    /**
     * ColorOS {@code LinkScrollState.mState}. While {@link #LINK_DOCK}, Recents must not
     * push scroll back into the dock (avoids feedback loop).
     */
    private int mLinkScrollState = LINK_UNLINK;
    /**
     * ColorOS {@code DockView.mScrollScale}:
     * {@code (taskWidth + recentsPageSpacing) / (iconWidth + dockPageSpacing)}.
     */
    private float mScrollScale = 1f;

    public OverviewDockView(Context context) {
        this(context, null);
    }

    public OverviewDockView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OverviewDockView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setHorizontalScrollBarEnabled(false);
        setClipToPadding(false);
        setClipChildren(false);
        setOverScrollMode(OVER_SCROLL_NEVER);

        mIconSize = getResources().getDimensionPixelSize(R.dimen.overview_dock_icon_size);
        mIconSpacing = getResources().getDimensionPixelSize(R.dimen.overview_dock_icon_spacing);
        mDimColor = RecentsView.getForegroundScrimDimColor(context);

        mContainer = new LinearLayout(context);
        mContainer.setOrientation(LinearLayout.HORIZONTAL);
        mContainer.setGravity(Gravity.CENTER_VERTICAL);
        mContainer.setClipChildren(false);
        mContainer.setClipToPadding(false);
        addView(mContainer, new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }

    public void setRecentsView(RecentsView<?, ?> recentsView) {
        mRecentsView = recentsView;
        if (recentsView != null) {
            mRecentsRtl = recentsView.isRtl();
        }
    }

    /** True while the user is scrolling/flinging the dock (tasks follow). */
    public boolean isDockDrivingRecents() {
        return mLinkScrollState == LINK_DOCK;
    }

    /** Place dock row at {@code dockCenterY} (Oppo {@code DockView.setLocation}). */
    public void calculateLocation(Rect insets, int dockCenterY, int taskWidth) {
        int rowHeight = getHeight() > 0 ? getHeight() : mIconSize;
        setTranslationX(0);
        setY(dockCenterY - rowHeight / 2f);
        computeScrollScale(taskWidth);
    }

    /**
     * ColorOS {@code DockView.onParentScrollInteractionBegin}: Recents touch/fling owns
     * link-scroll; suppress dock OverScroller so it cannot fight per-frame scrollTo.
     */
    public void onParentScrollInteractionBegin() {
        mLinkScrollState = LINK_RECENTS;
    }

    @Override
    public void computeScroll() {
        // While Recents drives, ignore HSV fling/settle — scrollX comes from onRecentsScrollTo.
        if (mLinkScrollState == LINK_RECENTS) {
            return;
        }
        super.computeScroll();
    }

    @Override
    public void fling(int velocityX) {
        // ColorOS dock settles via linked Recents snap, not free HSV fling.
    }

    /**
     * Rebuild icons from the current RecentsView task list when the set of task ids changes.
     */
    public void syncFromRecents(@Nullable RecentsView<?, ?> recentsView) {
        if (recentsView == null) {
            clearIcons();
            return;
        }
        mRecentsView = recentsView;
        mRecentsRtl = recentsView.isRtl();
        computeScrollScale(recentsView.mTaskWidth);
        int count = Math.min(recentsView.getTaskViewCount(), MAX_ICONS);
        int[] ids = new int[count];
        for (int i = 0; i < count; i++) {
            TaskView tv = recentsView.getTaskViewAt(i);
            if (tv == null) {
                ids[i] = -1;
                continue;
            }
            int[] taskIds = tv.getTaskIds();
            ids[i] = taskIds != null && taskIds.length > 0 ? taskIds[0] : -1;
        }
        if (Arrays.equals(ids, mBoundTaskIds) && mContainer.getChildCount() == count) {
            // Task set unchanged (e.g. post-dismiss re-sync after setCurrentPage) —
            // still re-center active highlight onto the current Recents page.
            recenterOnCurrentRecentsPage();
            return;
        }
        mBoundTaskIds = ids;
        rebuildIcons(recentsView, count);
    }

    /**
     * Map Recents task-list index → dock child index (visual left→right).
     * ColorOS binds {@code getChildAt((size - 1) - i)} when Recents RTL.
     */
    private int taskIndexToDockIndex(int taskIndex, int count) {
        if (count <= 0) {
            return taskIndex;
        }
        return mRecentsRtl ? (count - 1 - taskIndex) : taskIndex;
    }

    /** ColorOS {@code DockView.computeScrollScale}. */
    private void computeScrollScale(int fallbackTaskWidth) {
        if (mRecentsView == null) {
            mScrollScale = 1f;
            return;
        }
        int taskWidth = mRecentsView.mTaskWidth > 0 ? mRecentsView.mTaskWidth : fallbackTaskWidth;
        float dockPage = mIconSize + mIconSpacing;
        if (taskWidth <= 0 || dockPage <= 0f) {
            mScrollScale = 1f;
            return;
        }
        mScrollScale = (taskWidth + mRecentsView.getPageSpacing()) / dockPage;
    }

    private int dockScrollForCenteringChild(int dockIndex) {
        if (dockIndex < 0 || dockIndex >= mContainer.getChildCount() || getWidth() == 0) {
            return 0;
        }
        View child = mContainer.getChildAt(dockIndex);
        if (child == null || child.getWidth() == 0) {
            return 0;
        }
        return child.getLeft() + child.getWidth() / 2 + mContainer.getLeft() - getWidth() / 2;
    }

    private void centerOnDockIndex(int dockIndex) {
        if (dockIndex < 0 || dockIndex >= mContainer.getChildCount()) {
            return;
        }
        View child = mContainer.getChildAt(dockIndex);
        if (child == null || getWidth() == 0 || child.getWidth() == 0) {
            mCenteredChildIndex = dockIndex;
            post(() -> centerOnDockIndex(dockIndex));
            return;
        }
        mCenteredChildIndex = dockIndex;
        scrollTo(Math.max(0, dockScrollForCenteringChild(dockIndex)), 0);
        updateCurveProperties();
    }

    /**
     * ColorOS {@code DockView.onRecentsScrollTo}: map Recents primary scroll onto dock
     * {@code scrollX} via {@code mScrollScale}. Called every Recents scroll frame.
     */
    public void onRecentsScrollTo(int recentsPrimaryScroll) {
        if (mLinkScrollState == LINK_DOCK) {
            return;
        }
        if (mRecentsView == null || mContainer.getChildCount() == 0 || getWidth() == 0) {
            return;
        }
        computeScrollScale(0);
        if (mScrollScale <= 0f) {
            return;
        }
        TaskView tv0 = taskViewForDockChild(0);
        if (tv0 == null) {
            return;
        }
        int page0 = mRecentsView.indexOfChild(tv0);
        if (page0 < 0) {
            return;
        }
        // Ensure Recents owns the link while it is actively scrolling the dock.
        if (mLinkScrollState == LINK_UNLINK) {
            mLinkScrollState = LINK_RECENTS;
        }
        int recentsOrigin = mRecentsView.getScrollForPage(page0);
        int dockOrigin = dockScrollForCenteringChild(0);
        int maxDock = Math.max(dockOrigin, dockScrollForCenteringChild(mContainer.getChildCount() - 1));
        int minDock = Math.min(dockOrigin, dockScrollForCenteringChild(mContainer.getChildCount() - 1));
        int target = Math.round(dockOrigin + (recentsPrimaryScroll - recentsOrigin) / mScrollScale);
        target = Math.max(minDock, Math.min(maxDock, target));
        if (target != getScrollX()) {
            scrollTo(target, 0);
        } else {
            updateCurveProperties();
        }
        mCenteredChildIndex = findNearestDockIndexToCenter();
    }

    /**
     * Snap dock to the icon for {@code centered} (bind / rebuild only). Prefer
     * {@link #onRecentsScrollTo} during scroll for ColorOS-smooth tracking.
     */
    public void onRecentsPageCentered(@Nullable TaskView centered) {
        if (mLinkScrollState == LINK_DOCK) {
            return;
        }
        if (centered == null || mContainer.getChildCount() == 0) {
            return;
        }
        int[] taskIds = centered.getTaskIds();
        if (taskIds == null || taskIds.length == 0) {
            return;
        }
        final int targetTaskId = taskIds[0];
        int count = mContainer.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = mContainer.getChildAt(i);
            Object tag = child.getTag();
            if (tag instanceof Integer && ((Integer) tag) == targetTaskId) {
                centerOnDockIndex(i);
                return;
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            beginDockDrivenScroll();
        }
        boolean handled = super.dispatchTouchEvent(ev);
        // Always pair UP/CANCEL here: clicks are handled by child ImageViews and may
        // never reach onTouchEvent, which would leave LINK_DOCK stuck.
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            endDockDrivenScroll();
        }
        return handled;
    }

    private void beginDockDrivenScroll() {
        mLinkScrollState = LINK_DOCK;
        if (mRecentsView != null) {
            mRecentsView.abortScrollerAnimation();
        }
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
    }

    private void endDockDrivenScroll() {
        if (mLinkScrollState != LINK_DOCK) {
            return;
        }
        syncRecentsToDockScroll(true /* snap */);
        // ColorOS sets UNLINK after dock-driven page end; Recents snap then re-enters
        // LINK_RECENTS via onRecentsScrollTo / onParentScrollInteractionBegin.
        mLinkScrollState = LINK_UNLINK;
    }

    private int findNearestDockIndexToCenter() {
        int count = mContainer.getChildCount();
        if (count == 0 || getWidth() == 0) {
            return 0;
        }
        float viewportCenter = getScrollX() + getWidth() / 2f;
        int best = 0;
        float bestDist = Float.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            View child = mContainer.getChildAt(i);
            if (child == null) {
                continue;
            }
            float iconCenter = mContainer.getLeft() + child.getLeft() + child.getWidth() / 2f;
            float dist = Math.abs(viewportCenter - iconCenter);
            if (dist < bestDist) {
                bestDist = dist;
                best = i;
            }
        }
        return best;
    }

    /**
     * ColorOS dock→Recents link: {@code recentsScroll = origin + dockDelta * mScrollScale}.
     * {@code snap} settles to the nearest page; otherwise scrolls proportionally.
     */
    private void syncRecentsToDockScroll(boolean snap) {
        if (mRecentsView == null || getWidth() == 0 || mIconSize == 0) {
            return;
        }
        int count = mContainer.getChildCount();
        if (count == 0) {
            return;
        }
        computeScrollScale(0);
        if (mScrollScale <= 0f) {
            return;
        }
        TaskView tv0 = taskViewForDockChild(0);
        if (tv0 == null) {
            return;
        }
        int page0 = mRecentsView.indexOfChild(tv0);
        if (page0 < 0) {
            return;
        }
        int dockOrigin = dockScrollForCenteringChild(0);
        int recentsOrigin = mRecentsView.getScrollForPage(page0);

        if (snap) {
            int nearestDock = findNearestDockIndexToCenter();
            TaskView tv = taskViewForDockChild(nearestDock);
            if (tv != null) {
                int page = mRecentsView.indexOfChild(tv);
                if (page >= 0) {
                    mRecentsView.snapToPage(page);
                }
            }
            return;
        }
        int targetScroll = Math.round(
                recentsOrigin + (getScrollX() - dockOrigin) * mScrollScale);
        mRecentsView.scrollTo(targetScroll, mRecentsView.getScrollY());
        mRecentsView.updateCurveProperties();
    }

    @Nullable
    private TaskView taskViewForDockChild(int dockIndex) {
        if (mRecentsView == null || dockIndex < 0 || dockIndex >= mContainer.getChildCount()) {
            return null;
        }
        Object tag = mContainer.getChildAt(dockIndex).getTag();
        if (!(tag instanceof Integer)) {
            return null;
        }
        return mRecentsView.getTaskViewByTaskId((Integer) tag);
    }

    /**
     * ColorOS {@code DockView.updateCurveProperties}: scale/dim by distance to viewport center.
     * Centered → scale 1.0 / undimmed; ≥1 page away → scale 0.87 / 0.2 scrim.
     */
    public void updateCurveProperties() {
        int count = mContainer.getChildCount();
        if (count == 0 || getWidth() == 0 || mIconSize == 0) {
            return;
        }
        float pageWidth = mIconSize + mIconSpacing;
        float viewportCenterX = getScrollX() + getWidth() / 2f;
        for (int i = 0; i < count; i++) {
            View child = mContainer.getChildAt(i);
            if (!(child instanceof ImageView)) {
                continue;
            }
            float iconCenterX = mContainer.getLeft() + child.getLeft() + child.getTranslationX()
                    + child.getWidth() / 2f;
            float linear = Math.min(1f, Math.abs(viewportCenterX - iconCenterX) / pageWidth);
            float curve = (float) ((-Math.cos(linear * Math.PI) / 2.0) + 0.5);
            float scale = 1f - (curve * EDGE_SCALE_DOWN_FACTOR);
            float dim = curve * MAX_PAGE_SCRIM_ALPHA;
            applyIconVisuals((ImageView) child, scale, dim);
        }
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        updateCurveProperties();
        if (mLinkScrollState == LINK_DOCK) {
            syncRecentsToDockScroll(false /* snap */);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateSidePadding(w);
        if (mCenteredChildIndex >= 0) {
            final int dockIndex = mCenteredChildIndex;
            post(() -> centerOnDockIndex(dockIndex));
        } else {
            updateCurveProperties();
        }
    }

    private void updateSidePadding(int viewportWidth) {
        if (viewportWidth <= 0) {
            return;
        }
        int sidePad = Math.max(0, (viewportWidth - mIconSize) / 2);
        if (mContainer.getPaddingLeft() != sidePad || mContainer.getPaddingRight() != sidePad) {
            mContainer.setPadding(sidePad, 0, sidePad, 0);
        }
    }

    private void rebuildIcons(RecentsView<?, ?> recentsView, int count) {
        mContainer.removeAllViews();
        mCenteredChildIndex = -1;
        if (count == 0) {
            setVisibility(GONE);
            return;
        }
        if (getWidth() > 0) {
            updateSidePadding(getWidth());
        }
        TaskIconCache iconCache = RecentsModel.INSTANCE.get(getContext()).getIconCache();
        for (int visual = 0; visual < count; visual++) {
            int taskIndex = taskIndexToDockIndex(visual, count);
            TaskView tv = recentsView.getTaskViewAt(taskIndex);
            if (tv == null) {
                continue;
            }
            Task task = tv.getTask();
            ImageView icon = createIconView();
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(mIconSize, mIconSize);
            if (mContainer.getChildCount() > 0) {
                lp.setMarginStart(mIconSpacing);
            }
            mContainer.addView(icon, lp);

            final int taskId = taskIndex < mBoundTaskIds.length ? mBoundTaskIds[taskIndex] : -1;
            icon.setTag(taskId);
            icon.setOnClickListener(v -> launchTask(taskId));

            if (task != null && task.icon != null) {
                setDockIconDrawable(icon, task.icon);
            } else if (task != null) {
                iconCache.updateIconInBackground(task, loaded -> {
                    if (icon.getTag() instanceof Integer
                            && ((Integer) icon.getTag()) == taskId
                            && loaded.icon != null) {
                        setDockIconDrawable(icon, loaded.icon);
                        updateCurveProperties();
                    }
                });
            } else {
                Drawable fromHeader = tv.getIconView() != null
                        ? tv.getIconView().getDrawable() : null;
                setDockIconDrawable(icon, fromHeader);
            }
            applyIconVisuals(icon, 1f - EDGE_SCALE_DOWN_FACTOR, MAX_PAGE_SCRIM_ALPHA);
        }
        // Resolve the focused task after layout/page settle — capturing nearest during
        // dismiss removeViewInLayout is too early (current page not updated yet).
        post(this::recenterOnCurrentRecentsPage);
    }

    /**
     * Center/curve-highlight the dock icon for Recents' current page (or nearest card).
     * Called after rebuild and after dismiss snap so active state stays in sync.
     */
    public void recenterOnCurrentRecentsPage() {
        if (mRecentsView == null || mContainer.getChildCount() == 0) {
            return;
        }
        TaskView focus = mRecentsView.getCurrentPageTaskView();
        if (focus == null) {
            focus = mRecentsView.getTaskViewNearestToCenterOfScreen();
        }
        if (focus != null) {
            onRecentsPageCentered(focus);
        }
        // Link-scroll so scrollX + curve dim match the centered card even if page
        // centering missed (e.g. Clear All page / missing task id).
        mLinkScrollState = LINK_RECENTS;
        onRecentsScrollTo(mRecentsView.mOrientationHandler.getPrimaryScroll(mRecentsView));
    }

    /**
     * ColorOS {@code DockIconView.refresh}: draw into a private bitmap so TaskView IconView
     * setBounds/colorFilter on {@code task.icon} cannot shrink dock icons after swipe.
     */
    private void setDockIconDrawable(ImageView icon, @Nullable Drawable src) {
        if (src == null) {
            icon.setImageDrawable(null);
            return;
        }
        Drawable toDraw = src.getConstantState() != null
                ? src.getConstantState().newDrawable().mutate()
                : src.mutate();
        Bitmap bitmap = Bitmap.createBitmap(mIconSize, mIconSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        toDraw.setBounds(0, 0, mIconSize, mIconSize);
        toDraw.draw(canvas);
        BitmapDrawable bd = new BitmapDrawable(getResources(), bitmap);
        bd.setBounds(0, 0, mIconSize, mIconSize);
        icon.setImageDrawable(bd);
    }

    private void applyIconVisuals(ImageView icon, float scale, float dimAlpha) {
        icon.setPivotX(icon.getWidth() > 0 ? icon.getWidth() / 2f : mIconSize / 2f);
        icon.setPivotY(icon.getHeight() > 0 ? icon.getHeight() / 2f : mIconSize / 2f);
        icon.setScaleX(scale);
        icon.setScaleY(scale);
        icon.setColorFilter(Utilities.makeColorTintingColorFilter(mDimColor, dimAlpha));
    }

    private ImageView createIconView() {
        ImageView icon = new ImageView(getContext());
        icon.setScaleType(ImageView.ScaleType.FIT_XY);
        icon.setClickable(true);
        icon.setFocusable(true);
        return icon;
    }

    private void launchTask(int taskId) {
        if (taskId < 0 || mRecentsView == null) {
            return;
        }
        TaskView tv = mRecentsView.getTaskViewByTaskId(taskId);
        if (tv != null) {
            tv.launchTask(success -> { });
            return;
        }
        ActivityManagerWrapper.getInstance().startActivityFromRecents(taskId, null);
    }

    private void clearIcons() {
        mBoundTaskIds = new int[0];
        mContainer.removeAllViews();
        mCenteredChildIndex = -1;
        setVisibility(GONE);
    }
}
