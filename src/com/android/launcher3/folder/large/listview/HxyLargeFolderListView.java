package com.android.launcher3.folder.large.listview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.android.launcher3.big.popup.ColorOsFolderStyleView;
import com.android.launcher3.folder.large.HxyLargeFolderProxy;
import com.android.launcher3.model.data.FolderInfo;

/** Mode-aware renderer for OPPO's nine-grid, four-grid and highlight folder previews. */
public class HxyLargeFolderListView extends PageLinearLayout {
    private int mChildSize;
    private int mFolderStyle;
    private int mHorizontalGap;
    private int mVerticalGap;
    private int mPreviewColumns = 3;
    private int mPreviewRows = 3;
    private int mPreviewLeft;
    private int mPreviewTop;
    private int mPreviewWidth;
    private int mPreviewHeight;

    public HxyLargeFolderListView(Context context) {
        super(context);
    }

    public HxyLargeFolderListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HxyLargeFolderListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setFolderConfiguration(FolderInfo info, int style, int previewLeft,
            int previewTop, int previewWidth, int previewHeight) {
        boolean is2x2 = info.hasGrid2x2();
        mFolderStyle = is2x2 ? style : ColorOsFolderStyleView.STYLE_NINE_GRID;
        mPreviewColumns = info.getPreviewColumn();
        mPreviewRows = info.getPreviewRow();
        mPreviewLeft = previewLeft;
        mPreviewTop = previewTop;
        mPreviewWidth = previewWidth;
        mPreviewHeight = previewHeight;
        setSpanCount(mPreviewColumns);
        requestLayout();
    }

    public int getPreviewSlotCount() {
        if (mFolderStyle == ColorOsFolderStyleView.STYLE_HIERARCHICAL
                && mPreviewColumns == 3 && mPreviewRows == 3) return 6;
        return mPreviewColumns * mPreviewRows;
    }

    public int getChildSize() {
        return mChildSize;
    }

    public int getRenderedChildSize(int index) {
        return isHighlightLead(index) ? (mChildSize * 2) + mHorizontalGap : mChildSize;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int fourSize = HxyLargeFolderProxy.getFolderIconSize();
        boolean fourGrid = mFolderStyle == ColorOsFolderStyleView.STYLE_FOUR_GRID
                && mPreviewColumns == 2 && mPreviewRows == 2;
        if (fourGrid) {
            mChildSize = fourSize;
        } else {
            mChildSize = Math.max(1, Math.round(fourSize * 0.6666667f));
        }
        setSpanCount(mPreviewColumns);
        mHorizontalGap = mPreviewColumns > 1 ? Math.max(0,
                (mPreviewWidth - (mPreviewColumns * mChildSize))
                        / (mPreviewColumns + 1)) : 0;
        mVerticalGap = mPreviewRows > 1 ? Math.max(0,
                (mPreviewHeight - (mPreviewRows * mChildSize))
                        / (mPreviewRows + 1)) : 0;
        int contentWidth = (mPreviewColumns * mChildSize)
                + ((mPreviewColumns - 1) * mHorizontalGap);
        int contentHeight = (mPreviewRows * mChildSize)
                + ((mPreviewRows - 1) * mVerticalGap);
        int leftInset = Math.max(0, (mPreviewWidth - contentWidth) / 2);
        int topInset = Math.max(0, (mPreviewHeight - contentHeight) / 2);
        setPadding(Math.max(0, mPreviewLeft + leftInset),
                Math.max(0, mPreviewTop + topInset), 0, 0);
        setHorizontalSpace(mHorizontalGap);
        setVerticalSpace(mVerticalGap);
        int count = getChildCount();
        for (int index = 0; index < count; index++) {
            View child = getChildAt(index);
            if (child.getVisibility() == View.GONE) continue;
            int size = isHighlightLead(index)
                    ? (mChildSize * 2) + mHorizontalGap : mChildSize;
            child.measure(MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY));
            if (child instanceof HxyLargeFolderIconItem) {
                ((HxyLargeFolderIconItem) child).setRenderedSize(size);
            }
        }
        // OPPO's preview overlay keeps the complete workspace-cell bounds. A packed MeasureSpec
        // cannot be passed as a pixel width, and shrinking height clips rectangular previews.
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec),
                MeasureSpec.getSize(heightMeasureSpec));
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (mFolderStyle != ColorOsFolderStyleView.STYLE_HIERARCHICAL
                || mPreviewColumns != 3 || mPreviewRows != 3) {
            super.onLayout(changed, left, top, right, bottom);
            return;
        }
        for (int index = 0; index < getChildCount(); index++) {
            View child = getChildAt(index);
            if (index >= getPreviewSlotCount()) {
                child.setVisibility(View.GONE);
                continue;
            }
            if (child.getVisibility() != View.VISIBLE) continue;
            int childLeft = getHighlightLeft(index);
            int childTop = getHighlightTop(index);
            child.layout(childLeft, childTop,
                    childLeft + child.getMeasuredWidth(), childTop + child.getMeasuredHeight());
            onChildLayout(child, childLeft, childTop);
        }
    }

    private boolean isHighlightLead(int index) {
        return mFolderStyle == ColorOsFolderStyleView.STYLE_HIERARCHICAL
                && mPreviewColumns == 3 && mPreviewRows == 3 && index == 0;
    }

    private int getHighlightLeft(int index) {
        int step = mChildSize + mHorizontalGap;
        int column;
        boolean rtl = getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        // Exact OplusClippedFolderIconLayoutRule highlight mapping. Its synthetic
        // grid index is index + {0,1,3,3,3,3} in LTR and
        // index + {1,1,3,3,3,3} in RTL, then invertCellX is applied.
        int syntheticIndex;
        if (index == 0) syntheticIndex = rtl ? 1 : 0;
        else if (index == 1) syntheticIndex = 2;
        else syntheticIndex = index + 3;
        column = syntheticIndex % mPreviewColumns;
        if (rtl) column = mPreviewColumns - column - 1;
        return getPaddingLeft() + (column * step);
    }

    private int getHighlightTop(int index) {
        int step = mChildSize + mVerticalGap;
        int row;
        if (index == 0 || index == 1) row = 0;
        else if (index == 2) row = 1;
        else row = 2;
        return getPaddingTop() + (row * step);
    }

    @Override
    public void onChildLayout(View child, int left, int top) {
        super.onChildLayout(child, left, top);
        if (child instanceof HxyLargeFolderIconItem) {
            ((HxyLargeFolderIconItem) child).setCoordinateXY(left, top);
        }
    }

    public int[] getCoordinateXY(int index) {
        if (mFolderStyle == ColorOsFolderStyleView.STYLE_HIERARCHICAL) {
            return new int[] {getHighlightLeft(index), getHighlightTop(index)};
        }
        return new int[] {getGridLayoutLeft(index, getChildSize()),
                getGridLayoutTop(index, getChildSize())};
    }
}
