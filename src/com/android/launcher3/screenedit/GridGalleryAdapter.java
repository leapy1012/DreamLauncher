package com.android.launcher3.screenedit;

import android.view.View;
import android.view.ViewGroup;

public abstract class GridGalleryAdapter {
    public static final int GRID_COLUMNS_SIZE_THREE = 4;

    public static final int ROW_SIZE = 1;

    public static final int COLUMN_SIZE = 4;

    public abstract int getCount();

    public int getGridColumns() {
        return COLUMN_SIZE;
    }

    public int getGridRows() {
        return ROW_SIZE;
    }

    public Object getItem(int i) {
        return null;
    }

    public abstract View getView(int i, View view, ViewGroup viewGroup);

    public boolean onBackPressed() {
        return false;
    }
}
