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
 */
public class OverviewDockView extends HorizontalScrollView {

    private static final int MAX_ICONS = 20;

    /** ColorOS {@code DockIconView.EDGE_SCALE_DOWN_FACTOR}. */
    private static final float EDGE_SCALE_DOWN_FACTOR = 0.13f;
    /** ColorOS {@code DockIconView.MAX_PAGE_SCRIM_ALPHA}. */
    private static final float MAX_PAGE_SCRIM_ALPHA = 0.2f;

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
     * ColorOS link-scroll: dock is driving Recents. While true, Recents must not
     * push scroll back into the dock (avoids feedback loop).
     */
    private boolean mDockDrivingRecents;

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
        return mDockDrivingRecents;
    }

    /** Place dock row at {@code dockCenterY} (Oppo {@code DockView.setLocation}). */
    public void calculateLocation(Rect insets, int dockCenterY, int taskWidth) {
        int rowHeight = getHeight() > 0 ? getHeight() : mIconSize;
        setTranslationX(0);
        setY(dockCenterY - rowHeight / 2f);
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
            // Still re-center: swipe can leave scroll on the wrong icon.
            TaskView nearest = recentsView.getTaskViewNearestToCenterOfScreen();
            if (nearest != null) {
                onRecentsPageCentered(nearest);
            } else {
                updateCurveProperties();
            }
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
        int target = child.getLeft() + child.getWidth() / 2 + mContainer.getLeft() - getWidth() / 2;
        scrollTo(Math.max(0, target), 0);
        updateCurveProperties();
    }

    /**
     * Keep the dock icon for the centered recents card in view while scrolling.
     * Match by task id (not child index) so RTL reverse-bind cannot mis-center.
     */
    public void onRecentsPageCentered(@Nullable TaskView centered) {
        if (mDockDrivingRecents) {
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
        // never reach onTouchEvent, which would leave mDockDrivingRecents stuck true.
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            endDockDrivenScroll();
        }
        return handled;
    }

    private void beginDockDrivenScroll() {
        mDockDrivingRecents = true;
        if (mRecentsView != null) {
            mRecentsView.abortScrollerAnimation();
        }
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
    }

    private void endDockDrivenScroll() {
        if (!mDockDrivingRecents) {
            return;
        }
        syncRecentsToDockScroll(true /* snap */);
        mDockDrivingRecents = false;
    }

    /**
     * ColorOS dock→Recents link: map dock scroll position onto Recents pages.
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
        float pageWidth = mIconSize + mIconSpacing;
        float viewportCenter = getScrollX() + getWidth() / 2f;
        View first = mContainer.getChildAt(0);
        if (first == null || first.getWidth() == 0) {
            return;
        }
        float firstCenter = mContainer.getLeft() + first.getLeft() + first.getWidth() / 2f;
        float pos = (viewportCenter - firstCenter) / pageWidth;
        int i0 = (int) Math.floor(pos);
        float frac = pos - i0;
        if (i0 < 0) {
            i0 = 0;
            frac = 0f;
        }
        if (i0 >= count - 1) {
            i0 = count - 1;
            frac = 0f;
        }
        int i1 = Math.min(i0 + 1, count - 1);

        TaskView tv0 = taskViewForDockChild(i0);
        TaskView tv1 = i1 != i0 ? taskViewForDockChild(i1) : tv0;
        if (tv0 == null) {
            return;
        }
        int p0 = mRecentsView.indexOfChild(tv0);
        int p1 = tv1 != null ? mRecentsView.indexOfChild(tv1) : p0;
        if (p0 < 0) {
            return;
        }
        if (snap) {
            int targetPage = (frac < 0.5f || p1 < 0) ? p0 : p1;
            mRecentsView.snapToPage(targetPage);
            return;
        }
        if (p1 < 0) {
            p1 = p0;
        }
        int s0 = mRecentsView.getScrollForPage(p0);
        int s1 = mRecentsView.getScrollForPage(p1);
        int scroll = Math.round(s0 + (s1 - s0) * frac);
        mRecentsView.scrollTo(scroll, mRecentsView.getScrollY());
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
        if (mDockDrivingRecents) {
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
        TaskView nearest = recentsView.getTaskViewNearestToCenterOfScreen();
        post(() -> onRecentsPageCentered(nearest));
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
