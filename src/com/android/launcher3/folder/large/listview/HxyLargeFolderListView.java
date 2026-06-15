package com.android.launcher3.folder.large.listview;


import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import com.android.launcher3.R;
import com.android.launcher3.folder.large.HxyLargeFolderProxy;

public class HxyLargeFolderListView extends PageLinearLayout {
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
        int folderIconSize = HxyLargeFolderProxy.getFolderIconSize();
        mChildSize = folderIconSize;
        // 大文件夹布局调参
        int left = 0, right = 0, top = 0, bottom = 0;
        if (HxyLargeFolderProxy.isGrid(mContext, 3, 3)) {
            top = mContext.getResources().getDimensionPixelSize(R.dimen.hxy_large_folder_padding_top_3x3);
        } else if(HxyLargeFolderProxy.isGrid(mContext, 4, 4)) {
            top = mContext.getResources().getDimensionPixelSize(R.dimen.hxy_large_folder_padding_top_4x4);
        } else if (HxyLargeFolderProxy.isGrid(mContext, 4, 5)) {
            top = mContext.getResources().getDimensionPixelSize(R.dimen.hxy_large_folder_padding_top_4x5);
        }
        setPadding(left, top, right, bottom);
        measureChildren3(folderIconSize, folderIconSize);
        onMeasureGrid(widthMeasureSpec, heightMeasureSpec);
        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();
        android.util.Log.d("liu-db", "onMeasure width:" + measuredWidth + " height:" + measuredHeight);
    }

    private void measureChildren2(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        int spanCount = getSpanCount();
        int min = Math.min((((widthSize - (getHorizontalSpace() * (spanCount - 1))) - getPaddingLeft()) - getPaddingRight()) / spanCount, (((heightSize - (getVerticalSpace() * (spanCount - 1))) - getPaddingTop()) - getPaddingBottom()) / spanCount);
        mChildSize = min;
        measureChildren3(min, min);
    }

    private void measureChildren3(int childWidth, int childHeight) {
        int size = getChildCount();
        for (int i = 0; i < size; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != View.GONE) {
                int width = View.MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY);
                int height = View.MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY);
                android.util.Log.d("liu-db", "measureChildren3 width:" + childWidth + " height:" + childHeight);
                child.measure(View.MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY));
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
