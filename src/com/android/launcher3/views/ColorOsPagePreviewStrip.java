package com.android.launcher3.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.android.launcher3.CellLayout;
import com.android.launcher3.DropTarget;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutAndWidgetContainer;
import com.android.launcher3.Workspace;
import com.android.launcher3.celllayout.CellLayoutLayoutParams;
import com.android.launcher3.dragndrop.DragOptions;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.util.Themes;

/** Live ColorOS page-preview list shown while workspace items are selected. */
public final class ColorOsPagePreviewStrip extends HorizontalScrollView implements DropTarget {
    private static final long SELECTION_DURATION = 180L;
    private static final int ITEM_WIDTH_DP = 48;
    private static final int ITEM_HEIGHT_DP = 68;
    private static final int ITEM_DIVIDER_DP = 6;
    private static final int OVERFLOW_PADDING_DP = 40;

    private final Launcher mLauncher;
    private final LinearLayout mItems;
    private int mSelectedPage = -1;
    private int mDragTargetPage = -1;
    private boolean mLastDropDelegated;

    public ColorOsPagePreviewStrip(Context context) {
        this(context, null);
    }

    public ColorOsPagePreviewStrip(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorOsPagePreviewStrip(Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLauncher = Launcher.getLauncher(context);
        setHorizontalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
        setClipToPadding(false);
        mItems = new LinearLayout(context);
        mItems.setOrientation(LinearLayout.HORIZONTAL);
        mItems.setGravity(Gravity.CENTER_VERTICAL);
        addView(mItems, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
    }

    /** Rebuilds every thumbnail from the workspace's current CellLayout children. */
    public void refresh() {
        Workspace<?> workspace = mLauncher.getWorkspace();
        int count = workspace.getPageCount();
        int selected = Math.max(0, Math.min(workspace.getNextPage(), count - 1));
        mSelectedPage = selected;
        mItems.removeAllViews();

        int slotWidth = dp(ITEM_WIDTH_DP + ITEM_DIVIDER_DP * 2);
        int totalWidth = slotWidth * count;
        int availableWidth = getResources().getDisplayMetrics().widthPixels;
        int outerPadding = totalWidth < availableWidth
                ? Math.max(0, (availableWidth - totalWidth) / 2)
                : dp(OVERFLOW_PADDING_DP);
        mItems.setPadding(outerPadding, 0, outerPadding, 0);

        for (int page = 0; page < count; page++) {
            CellLayout layout = (CellLayout) workspace.getPageAt(page);
            PageItem item = new PageItem(getContext(), layout, page);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    dp(ITEM_WIDTH_DP), dp(ITEM_HEIGHT_DP));
            lp.leftMargin = dp(ITEM_DIVIDER_DP);
            lp.rightMargin = dp(ITEM_DIVIDER_DP);
            lp.gravity = Gravity.CENTER_VERTICAL;
            mItems.addView(item, lp);
            item.setSelectedState(page == selected, false);
            final int targetPage = page;
            item.setOnClickListener(v -> selectPage(targetPage, true));
        }
        post(() -> centerSelectedPage(false));
    }

    public void updateSelectedPage() {
        int page = Math.max(0, Math.min(mLauncher.getWorkspace().getNextPage(),
                mItems.getChildCount() - 1));
        selectPage(page, false);
    }

    private void selectPage(int page, boolean snapWorkspace) {
        if (page < 0 || page >= mItems.getChildCount()) return;
        int old = mSelectedPage;
        mSelectedPage = page;
        if (old >= 0 && old < mItems.getChildCount() && old != page) {
            ((PageItem) mItems.getChildAt(old)).setSelectedState(false, true);
        }
        ((PageItem) mItems.getChildAt(page)).setSelectedState(true, true);
        if (snapWorkspace && mLauncher.getWorkspace().getNextPage() != page) {
            mLauncher.getWorkspace().snapToPage(page);
        }
        centerSelectedPage(true);
    }

    @Override
    public boolean isDropEnabled() {
        return isShown() && mItems.getChildCount() > 0;
    }

    @Override
    public void onDragEnter(DragObject dragObject) {
        mLastDropDelegated = false;
        updateDragTarget(dragObject);
    }

    @Override
    public void onDragOver(DragObject dragObject) {
        updateDragTarget(dragObject);
    }

    @Override
    public void onDragExit(DragObject dragObject) {
        if (!dragObject.dragComplete) {
            mDragTargetPage = -1;
        }
    }

    @Override
    public boolean acceptDrop(DragObject dragObject) {
        return findVacantCell(dragObject, null);
    }

    @Override
    public void onDrop(DragObject dragObject, DragOptions options) {
        int[] targetCell = new int[2];
        if (!findVacantCell(dragObject, targetCell)) {
            return;
        }
        Workspace<?> workspace = mLauncher.getWorkspace();
        CellLayout targetLayout = (CellLayout) workspace.getPageAt(mDragTargetPage);
        workspace.snapToPageImmediately(mDragTargetPage);
        // OPPO remains in PAGE_PREVIEW for delivery and returns to TOGGLE_BAR afterwards.
        // Our compatibility states are EDIT_MODE for both surfaces. Prevent AOSP's internal
        // Workspace drop path from treating the preview target as a spring-loaded external drop.
        if (mLauncher.getDragLayer().findViewById(R.id.edit_mode_container) != null
                && !mLauncher.isInState(LauncherState.EDIT_MODE)) {
            mLauncher.getStateManager().goToState(LauncherState.EDIT_MODE, false);
        }

        int[] center = new int[2];
        targetLayout.regionToCenterPoint(targetCell[0], targetCell[1],
                Math.max(1, dragObject.dragInfo.spanX),
                Math.max(1, dragObject.dragInfo.spanY), center);
        float[] point = new float[] {center[0], center[1]};
        mLauncher.getDragLayer().getDescendantCoordRelativeToSelf(targetLayout, point);
        mLauncher.getDragLayer().mapCoordInSelfToDescendant(workspace, point);

        int oldX = dragObject.x;
        int oldY = dragObject.y;
        float[] visualCenter = dragObject.getVisualCenter(null);
        dragObject.x += Math.round(point[0] - visualCenter[0]);
        dragObject.y += Math.round(point[1] - visualCenter[1]);
        workspace.onDragEnter(dragObject);
        workspace.onDragOver(dragObject);
        workspace.onDragExit(dragObject);
        if (workspace.acceptDrop(dragObject)) {
            workspace.onDrop(dragObject, options);
            mLastDropDelegated = true;
        }
        dragObject.x = oldX;
        dragObject.y = oldY;
        refresh();
    }

    @Override
    public void prepareAccessibilityDrop() {
    }

    @Override
    public void getHitRectRelativeToDragLayer(Rect outRect) {
        mLauncher.getDragLayer().getDescendantRectRelativeToSelf(this, outRect);
    }

    public boolean didDelegateLastDrop() {
        return mLastDropDelegated;
    }

    private void updateDragTarget(DragObject dragObject) {
        int page = findPageAt(dragObject.x);
        if (page >= 0 && page != mDragTargetPage) {
            mDragTargetPage = page;
            selectPage(page, true);
        }
    }

    private int findPageAt(int x) {
        for (int i = 0; i < mItems.getChildCount(); i++) {
            View child = mItems.getChildAt(i);
            if (x >= child.getLeft() && x < child.getRight()) {
                return ((PageItem) child).mPage;
            }
        }
        return -1;
    }

    private boolean findVacantCell(DragObject dragObject, @Nullable int[] outCell) {
        if (dragObject == null || dragObject.dragInfo == null
                || mDragTargetPage < 0
                || mDragTargetPage >= mLauncher.getWorkspace().getPageCount()) {
            return false;
        }
        CellLayout layout =
                (CellLayout) mLauncher.getWorkspace().getPageAt(mDragTargetPage);
        int[] cell = outCell != null ? outCell : new int[2];
        return layout != null && layout.findCellForSpan(cell,
                Math.max(1, dragObject.dragInfo.spanX),
                Math.max(1, dragObject.dragInfo.spanY));
    }

    private void centerSelectedPage(boolean smooth) {
        if (mSelectedPage < 0 || mSelectedPage >= mItems.getChildCount()) return;
        View child = mItems.getChildAt(mSelectedPage);
        int target = child.getLeft() + child.getWidth() / 2 - getWidth() / 2;
        target = Math.max(0, target);
        if (smooth) smoothScrollTo(target, 0); else scrollTo(target, 0);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private final class PageItem extends View {
        private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF mRect = new RectF();
        private final CellLayout mLayout;
        private final int mPage;
        private final int mPrimaryColor;
        private final int mFrameColor;
        private final int mSelectedFrameColor;
        private final int mFrameStrokeColor;
        private float mSelectionProgress;
        private float mStrokeProgress;
        private ValueAnimator mAnimator;
        private ValueAnimator mStrokeAnimator;

        PageItem(Context context, CellLayout layout, int page) {
            super(context);
            mLayout = layout;
            mPage = page;
            TypedValue value = new TypedValue();
            if (context.getTheme().resolveAttribute(R.attr.couiColorPrimary, value, true)) {
                mPrimaryColor = value.resourceId != 0
                        ? context.getColor(value.resourceId) : value.data;
            } else {
                mPrimaryColor = 0xFF2D6BFF;
            }
            boolean bright = Themes.getAttrBoolean(context, R.attr.isWorkspaceDarkText);
            // launcher_page_preview_cell_{normal,selected}_bg_color(_dark)
            // from the decoded OPPO resources.
            mFrameColor = bright ? 0x19000000 : 0x19FFFFFF;
            mSelectedFrameColor = bright ? 0x4D000000 : 0x4DFFFFFF;
            mFrameStrokeColor = bright ? 0x99000000 : 0x99FFFFFF;
            setClickable(true);
        }

        void setSelectedState(boolean selected, boolean animate) {
            float target = selected ? 1f : 0f;
            if (mAnimator != null) mAnimator.cancel();
            if (mStrokeAnimator != null) mStrokeAnimator.cancel();
            if (!animate) {
                mSelectionProgress = target;
                mStrokeProgress = target;
                setSelected(selected);
                invalidate();
                return;
            }
            setSelected(selected);
            mAnimator = ValueAnimator.ofFloat(mSelectionProgress, target);
            mAnimator.setDuration(SELECTION_DURATION);
            mAnimator.addUpdateListener(a -> {
                mSelectionProgress = (float) a.getAnimatedValue();
                invalidate();
            });
            mAnimator.start();
            mStrokeAnimator = ValueAnimator.ofFloat(mStrokeProgress, target);
            mStrokeAnimator.setDuration(selected ? 280L : 150L);
            mStrokeAnimator.addUpdateListener(a -> {
                mStrokeProgress = (float) a.getAnimatedValue();
                invalidate();
            });
            mStrokeAnimator.start();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float stroke = dp(1.2f);
            mRect.set(stroke / 2f, stroke / 2f, getWidth() - stroke / 2f,
                    getHeight() - stroke / 2f);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(blend(mFrameColor, mSelectedFrameColor, mSelectionProgress));
            canvas.drawRoundRect(mRect, dp(8), dp(8), mPaint);
            if (mStrokeProgress > 0f) {
                mPaint.setStyle(Paint.Style.STROKE);
                mPaint.setStrokeWidth(stroke);
                mPaint.setColor(Color.argb(Math.round(255 * mStrokeProgress),
                        Color.red(mFrameStrokeColor), Color.green(mFrameStrokeColor),
                        Color.blue(mFrameStrokeColor)));
                canvas.drawRoundRect(mRect, dp(8), dp(8), mPaint);
            }
            drawCells(canvas);
        }

        private void drawCells(Canvas canvas) {
            if (mLayout == null) return;
            int columns = Math.max(1, mLayout.getCountX());
            int rows = Math.max(1, mLayout.getCountY());
            float cellW = dp(6);
            float cellH = dp(4);
            float left = dp(6);
            float top = dp(6);
            float gapX = columns == 1 ? 0
                    : (getWidth() - dp(12) - columns * cellW) / (columns - 1);
            float gapY = rows == 1 ? 0
                    : (getHeight() - dp(12) - rows * cellH) / (rows - 1);
            int normal = Color.argb(51, 255, 255, 255);
            int selected = Color.argb(255, Color.red(mPrimaryColor),
                    Color.green(mPrimaryColor), Color.blue(mPrimaryColor));
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(blend(normal, selected, mSelectionProgress));

            ShortcutAndWidgetContainer children = mLayout.getShortcutsAndWidgets();
            for (int i = 0; i < children.getChildCount(); i++) {
                View child = children.getChildAt(i);
                int cellX;
                int cellY;
                int spanX;
                int spanY;
                if (child.getTag() instanceof ItemInfo) {
                    ItemInfo info = (ItemInfo) child.getTag();
                    cellX = info.cellX;
                    cellY = info.cellY;
                    spanX = Math.max(1, info.spanX);
                    spanY = Math.max(1, info.spanY);
                } else if (child.getLayoutParams() instanceof CellLayoutLayoutParams) {
                    CellLayoutLayoutParams lp =
                            (CellLayoutLayoutParams) child.getLayoutParams();
                    cellX = lp.getCellX();
                    cellY = lp.getCellY();
                    spanX = Math.max(1, lp.cellHSpan);
                    spanY = Math.max(1, lp.cellVSpan);
                } else {
                    continue;
                }
                float x = left + cellX * (cellW + gapX);
                float y = top + cellY * (cellH + gapY);
                float right = x + spanX * cellW + Math.max(0, spanX - 1) * gapX;
                float bottom = y + spanY * cellH + Math.max(0, spanY - 1) * gapY;
                float radius = spanX > 1 || spanY > 1 ? dp(4) : dp(2);
                canvas.drawRoundRect(x, y, right, bottom, radius, radius, mPaint);
            }
        }

        private int blend(int from, int to, float fraction) {
            int a = Math.round(Color.alpha(from)
                    + (Color.alpha(to) - Color.alpha(from)) * fraction);
            int r = Math.round(Color.red(from)
                    + (Color.red(to) - Color.red(from)) * fraction);
            int g = Math.round(Color.green(from)
                    + (Color.green(to) - Color.green(from)) * fraction);
            int b = Math.round(Color.blue(from)
                    + (Color.blue(to) - Color.blue(from)) * fraction);
            return Color.argb(a, r, g, b);
        }

        @Override
        protected void onDetachedFromWindow() {
            if (mAnimator != null) mAnimator.cancel();
            if (mStrokeAnimator != null) mStrokeAnimator.cancel();
            super.onDetachedFromWindow();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mLauncher.getDragController().addDropTarget(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        mLauncher.getDragController().removeDropTarget(this);
        super.onDetachedFromWindow();
    }
}
