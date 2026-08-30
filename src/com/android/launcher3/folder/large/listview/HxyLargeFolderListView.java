package com.android.launcher3.folder.large.listview;


import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import com.android.launcher3.R;
import com.android.launcher3.folder.large.HxyLargeFolderProxy;

public class HxyLargeFolderListView extends PageLinearLayout {
    /**
     * Fraction of the equal cell stride used by the icon itself. Leftover becomes the
     * gutter between icons (ColorOS uses ~0.63–0.67 of bubble; our stride is already
     * plate/span so a higher factor still leaves a clear gap).
     */
    private static final float PREVIEW_ICON_SCALE = 0.78f;

    private int mChildSize = 0;
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

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int span = Math.max(1, getSpanCount());
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
        measureChildren3(folderIconSize, folderIconSize);
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

    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        // Center the span×span grid inside the plate when leftover remains on one axis.
        int span = Math.max(1, getSpanCount());
        int gridW = mChildSize * span + getHorizontalSpace() * Math.max(0, span - 1);
        int gridH = mChildSize * span + getVerticalSpace() * Math.max(0, span - 1);
        int availW = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        int availH = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
        int extraX = Math.max(0, (availW - gridW) / 2);
        int extraY = Math.max(0, (availH - gridH) / 2);
        if (extraX > 0 || extraY > 0) {
            // Temporarily shift children by adjusting padding for this layout pass only
            // via translating layout origins — PageLinearLayout uses getPadding* in grid math.
            int pl = getPaddingLeft();
            int pt = getPaddingTop();
            int pr = getPaddingRight();
            int pb = getPaddingBottom();
            setPadding(pl + extraX, pt + extraY, pr + extraX, pb + extraY);
            super.onLayout(changed, l, t, r, b);
            setPadding(pl, pt, pr, pb);
        } else {
            super.onLayout(changed, l, t, r, b);
        }
        // Rebind after layout so overflow stack uses measured cell size.
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child instanceof HxyLargeFolderIconItem) {
                ((HxyLargeFolderIconItem) child).rebindIfNeeded();
            }
        }
    }

    public void onChildLayout(View child, int left, int top) {
        super.onChildLayout(child, left, top);
        if (child instanceof HxyLargeFolderIconItem) {
            ((HxyLargeFolderIconItem) child).setCoordinateXY(left, top);
        }
    }

    public int[] getCoordinateXY(int index) {
        return new int[]{getGridLayoutLeft(index, getChildSize()), getGridLayoutTop(index, getChildSize())};
    }
}
