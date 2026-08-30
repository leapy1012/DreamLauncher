package com.android.launcher3.folder.large.listview;


import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import com.android.launcher3.R;
import com.android.launcher3.folder.large.HxyLargeFolderProxy;

/**
 * Closed big-folder preview grid. Supports ColorOS nine / four layouts and the
 * highlight layout (index 0 = 2×2 featured icon; indices 1–2 right column;
 * 3–4 bottom row; 5 = overflow stack).
 */
public class HxyLargeFolderListView extends PageLinearLayout {
    /**
     * Fraction of the equal cell stride used by the icon itself. Leftover becomes the
     * gutter between icons (ColorOS uses ~0.63–0.67 of bubble; our stride is already
     * plate/span so a higher factor still leaves a clear gap).
     */
    private static final float PREVIEW_ICON_SCALE = 0.78f;
    /** Highlight: logical 3×3 with 3 cells merged into the featured icon. */
    private static final int HIGHLIGHT_SPAN = 3;

    private int mChildSize = 0;
    private int mHighlightLargeSize = 0;
    private boolean mHighlightLayout;
    private Context mContext;

    public HxyLargeFolderListView(Context context) {
        super(context);
        mContext = context;
    }

    public HxyLargeFolderListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public HxyLargeFolderListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
    }

    public int getChildSize() {
        return mChildSize;
    }

    public void setHighlightLayout(boolean highlight) {
        if (mHighlightLayout != highlight) {
            mHighlightLayout = highlight;
            requestLayout();
        }
    }

    public boolean isHighlightLayout() {
        return mHighlightLayout;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int span = Math.max(1, mHighlightLayout ? HIGHLIGHT_SPAN : getSpanCount());
        int availW = View.MeasureSpec.getSize(widthMeasureSpec)
                - getPaddingLeft() - getPaddingRight();
        int availH = View.MeasureSpec.getSize(heightMeasureSpec)
                - getPaddingTop() - getPaddingBottom();

        int folderIconSize = 1;
        int gap = 0;
        if (availW > 0 && availH > 0) {
            // Square content so H/V gutters match (Oppo plate is treated symmetrically).
            int content = Math.min(availW, availH);
            int minGap = mContext.getResources()
                    .getDimensionPixelSize(R.dimen.hxy_large_folder_preview_item_gap);
            // Icon takes PREVIEW_ICON_SCALE of each equal stride; rest is gutter.
            int stride = Math.max(1, content / span);
            folderIconSize = Math.max(1, Math.round(stride * PREVIEW_ICON_SCALE));
            if (span > 1) {
                gap = Math.max(minGap, (content - folderIconSize * span) / (span - 1));
                // Keep span*icon + (span-1)*gap <= content.
                while (folderIconSize * span + gap * (span - 1) > content && folderIconSize > 1) {
                    folderIconSize--;
                }
                gap = Math.max(minGap, (content - folderIconSize * span) / (span - 1));
            }
        }
        setHorizontalSpace(gap);
        setVerticalSpace(gap);
        mChildSize = folderIconSize;
        // Featured icon spans two cells + the gutter between them.
        mHighlightLargeSize = folderIconSize * 2 + gap;
        if (mHighlightLayout) {
            measureHighlightChildren(folderIconSize, mHighlightLargeSize);
        } else {
            measureChildren3(folderIconSize, folderIconSize);
        }
        // Honor EXACTLY plate size from the folder icon — PageLinearLayout.onMeasureGrid
        // otherwise shrinks height to content and breaks neighbor-page layout/capture.
        if (View.MeasureSpec.getMode(widthMeasureSpec) == View.MeasureSpec.EXACTLY
                && View.MeasureSpec.getMode(heightMeasureSpec) == View.MeasureSpec.EXACTLY) {
            setMeasuredDimension(
                    View.MeasureSpec.getSize(widthMeasureSpec),
                    View.MeasureSpec.getSize(heightMeasureSpec));
        } else {
            onMeasureGrid(widthMeasureSpec, heightMeasureSpec);
        }
    }

    private void measureChildren3(int childWidth, int childHeight) {
        int size = getChildCount();
        for (int i = 0; i < size; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != View.GONE) {
                child.measure(
                        View.MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY));
            }
        }
    }

    private void measureHighlightChildren(int cell, int large) {
        int size = getChildCount();
        for (int i = 0; i < size; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == View.GONE) {
                continue;
            }
            int dim = (i == 0) ? large : cell;
            child.measure(
                    View.MeasureSpec.makeMeasureSpec(dim, MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(dim, MeasureSpec.EXACTLY));
        }
    }

    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        // Center the span×span grid inside the plate when leftover remains on one axis.
        // Do NOT call setPadding() here — it requestLayouts mid-pass and overlaps children.
        int span = Math.max(1, mHighlightLayout ? HIGHLIGHT_SPAN : getSpanCount());
        int gap = getHorizontalSpace();
        int gridW = mChildSize * span + gap * Math.max(0, span - 1);
        int gridH = mChildSize * span + gap * Math.max(0, span - 1);
        int availW = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        int availH = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
        int extraX = Math.max(0, (availW - gridW) / 2);
        int extraY = Math.max(0, (availH - gridH) / 2);
        if (mHighlightLayout) {
            layoutHighlightChildren(extraX, extraY);
        } else {
            layoutGridChildren(extraX, extraY);
        }
        // Rebind after layout so overflow stack / featured icon use measured cell size.
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child instanceof HxyLargeFolderIconItem) {
                ((HxyLargeFolderIconItem) child).rebindIfNeeded();
            }
        }
    }

    private void layoutGridChildren(int extraX, int extraY) {
        int gapH = getHorizontalSpace();
        int gapV = getVerticalSpace();
        int cell = mChildSize;
        int span = Math.max(1, getSpanCount());
        int originX = getPaddingLeft() + extraX;
        int originY = getPaddingTop() + extraY;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == View.GONE) {
                continue;
            }
            int row = i / span;
            int col = i % span;
            int left = originX + col * (cell + gapH);
            int top = originY + row * (cell + gapV);
            child.layout(left, top, left + cell, top + cell);
            onChildLayout(child, left, top);
        }
    }

    /**
     * ColorOS highlight cell map (LTR):
     * 0 → (0,0) large 2×2; 1 → (0,2); 2 → (1,2); 3 → (2,0); 4 → (2,1); 5 → (2,2).
     */
    private void layoutHighlightChildren(int extraX, int extraY) {
        int gap = getHorizontalSpace();
        int cell = mChildSize;
        int large = mHighlightLargeSize > 0 ? mHighlightLargeSize : cell * 2 + gap;
        int originX = getPaddingLeft() + extraX;
        int originY = getPaddingTop() + extraY;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == View.GONE) {
                continue;
            }
            int row;
            int col;
            int w;
            int h;
            if (i == 0) {
                row = 0;
                col = 0;
                w = large;
                h = large;
            } else {
                int mapped = highlightMappedIndex(i);
                row = mapped / HIGHLIGHT_SPAN;
                col = mapped % HIGHLIGHT_SPAN;
                w = cell;
                h = cell;
            }
            int left = originX + col * (cell + gap);
            int top = originY + row * (cell + gap);
            child.layout(left, top, left + w, top + h);
            onChildLayout(child, left, top);
        }
    }

    /** Remap preview index → 3×3 cell index (Oppo DEL_ICON_NUM deltas). */
    private static int highlightMappedIndex(int previewIndex) {
        if (previewIndex <= 0) {
            return 0;
        }
        if (previewIndex == 1) {
            return 2; // (0,2)
        }
        // 2→5 (1,2), 3→6 (2,0), 4→7 (2,1), 5→8 (2,2)
        return previewIndex + 3;
    }

    public void onChildLayout(View child, int left, int top) {
        super.onChildLayout(child, left, top);
        if (child instanceof HxyLargeFolderIconItem) {
            ((HxyLargeFolderIconItem) child).setCoordinateXY(left, top);
        }
    }

    public int[] getCoordinateXY(int index) {
        if (mHighlightLayout) {
            int gap = getHorizontalSpace();
            int cell = mChildSize;
            int mapped = index == 0 ? 0 : highlightMappedIndex(index);
            int row = mapped / HIGHLIGHT_SPAN;
            int col = mapped % HIGHLIGHT_SPAN;
            return new int[]{
                    getPaddingLeft() + col * (cell + gap),
                    getPaddingTop() + row * (cell + gap)
            };
        }
        return new int[]{getGridLayoutLeft(index, getChildSize()), getGridLayoutTop(index, getChildSize())};
    }
}
