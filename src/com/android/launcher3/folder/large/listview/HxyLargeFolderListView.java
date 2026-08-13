package com.android.launcher3.folder.large.listview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.android.launcher3.big.popup.ColorOsFolderStyleView;
import com.android.launcher3.folder.large.HxyLargeFolderProxy;

/** Mode-aware renderer for OPPO's nine-grid, four-grid and highlight folder previews. */
public class HxyLargeFolderListView extends PageLinearLayout {
    private int mChildSize;
    private int mFolderStyle;
    private int mHorizontalGap;
    private int mVerticalGap;

    public HxyLargeFolderListView(Context context) {
        super(context);
    }

    public HxyLargeFolderListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HxyLargeFolderListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setFolderStyle(int style) {
        if (mFolderStyle == style) return;
        mFolderStyle = style;
        setSpanCount(style == ColorOsFolderStyleView.STYLE_FOUR_GRID ? 2 : 3);
        requestLayout();
    }

    public int getPreviewSlotCount() {
        if (mFolderStyle == ColorOsFolderStyleView.STYLE_FOUR_GRID) return 4;
        if (mFolderStyle == ColorOsFolderStyleView.STYLE_HIERARCHICAL) return 6;
        return 9;
    }

    public int getChildSize() {
        return mChildSize;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int fourSize = HxyLargeFolderProxy.getFolderIconSize();
        int sourceGapX = HxyLargeFolderProxy.getHorizontalSpace();
        int sourceGapY = HxyLargeFolderProxy.getVerticalSpace();
        mHorizontalGap = sourceGapX;
        mVerticalGap = sourceGapY;
        boolean fourGrid = mFolderStyle == ColorOsFolderStyleView.STYLE_FOUR_GRID;
        if (fourGrid) {
            mChildSize = fourSize;
            setSpanCount(2);
        } else {
            int previewWidth = (fourSize * 2) + (sourceGapX * 3);
            int previewHeight = (fourSize * 2) + (sourceGapY * 3);
            mChildSize = Math.max(1, Math.min(
                    (previewWidth - (sourceGapX * 4)) / 3,
                    (previewHeight - (sourceGapY * 4)) / 3));
            setSpanCount(3);
        }
        setHorizontalSpace(mHorizontalGap);
        setVerticalSpace(mVerticalGap);
        int count = getChildCount();
        for (int index = 0; index < count; index++) {
            View child = getChildAt(index);
            if (child.getVisibility() == View.GONE) continue;
            int size = isHighlightLead(index)
                    ? (mChildSize * 2) + mHorizontalGap : mChildSize;
            if (child instanceof HxyLargeFolderIconItem) {
                ((HxyLargeFolderIconItem) child).setRenderedSize(size);
            }
            child.measure(MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY));
        }
        int rows = fourGrid ? 2 : 3;
        int contentHeight = (rows * mChildSize) + ((rows - 1) * mVerticalGap)
                + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(widthMeasureSpec,
                MeasureSpec.makeMeasureSpec(contentHeight, MeasureSpec.EXACTLY));
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (mFolderStyle != ColorOsFolderStyleView.STYLE_HIERARCHICAL) {
            super.onLayout(changed, left, top, right, bottom);
            return;
        }
        for (int index = 0; index < getChildCount(); index++) {
            View child = getChildAt(index);
            if (child.getVisibility() != View.VISIBLE) continue;
            int childLeft = getHighlightLeft(index);
            int childTop = getHighlightTop(index);
            child.layout(childLeft, childTop,
                    childLeft + child.getMeasuredWidth(), childTop + child.getMeasuredHeight());
            onChildLayout(child, childLeft, childTop);
        }
    }

    private boolean isHighlightLead(int index) {
        return mFolderStyle == ColorOsFolderStyleView.STYLE_HIERARCHICAL && index == 0;
    }

    private int getHighlightLeft(int index) {
        int step = mChildSize + mHorizontalGap;
        int column;
        if (index == 0) column = 0;
        else if (index <= 2) column = 2;
        else column = index - 3;
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
