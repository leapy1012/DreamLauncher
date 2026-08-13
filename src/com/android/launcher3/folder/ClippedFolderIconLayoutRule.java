package com.android.launcher3.folder;

public class ClippedFolderIconLayoutRule {

    public static final int MAX_NUM_ITEMS_IN_PREVIEW = 9;
    private static final int PREVIEW_GRID_SIZE = 3;

    // OPPO's normal folder preview uses a fixed 3 x 3 grid. Each sub-icon consumes 20% of
    // the preview width, the visual gap is 5%, and the effective outer inset is 15%.
    private static final float ITEM_SIZE_FACTOR = 0.20f;
    private static final float ITEM_GAP_FACTOR = 0.05f;
    private static final float OUTER_INSET_FACTOR = 0.15f;
    // The max amount of overlap the preview items can go outside of the background bounds.
    public static final float ICON_OVERLAP_FACTOR = 1.125f;
    public static final int EXIT_INDEX = -2;
    public static final int ENTER_INDEX = -3;

    private float[] mTmpPoint = new float[2];

    private float mAvailableSpace;
    private float mIconSize;
    private boolean mIsRtl;
    private float mBaselineIconScale;

    public void init(int availableSpace, float intrinsicIconSize, boolean rtl) {
        mAvailableSpace = availableSpace;
        mIconSize = intrinsicIconSize;
        mIsRtl = rtl;
        mBaselineIconScale = availableSpace / (intrinsicIconSize * 1f);
    }

    public PreviewItemDrawingParams computePreviewItemDrawingParams(int index, int curNumItems,
            PreviewItemDrawingParams params) {
        float totalScale = scaleForItem(curNumItems);
        float transX;
        float transY;

        if (index == EXIT_INDEX) {
            // Move exiting items just beyond the first row.
            getGridPosition(0, PREVIEW_GRID_SIZE, mTmpPoint);
        } else if (index == ENTER_INDEX) {
            // Bring entering items in beside the final row.
            getGridPosition(PREVIEW_GRID_SIZE - 1, PREVIEW_GRID_SIZE, mTmpPoint);
        } else if (index >= MAX_NUM_ITEMS_IN_PREVIEW) {
            // Items beyond those displayed in the preview are animated to the center
            mTmpPoint[0] = mTmpPoint[1] = mAvailableSpace / 2 - (mIconSize * totalScale) / 2;
        } else {
            getPosition(index, curNumItems, mTmpPoint);
        }

        transX = mTmpPoint[0];
        transY = mTmpPoint[1];

        if (params == null) {
            params = new PreviewItemDrawingParams(transX, transY, totalScale);
        } else {
            params.update(transX, transY, totalScale);
        }
        return params;
    }

    private void getGridPosition(int row, int col, float[] result) {
        float itemSize = mAvailableSpace * ITEM_SIZE_FACTOR;
        float stride = itemSize + (mAvailableSpace * ITEM_GAP_FACTOR);
        int resolvedCol = mIsRtl ? (PREVIEW_GRID_SIZE - 1) - col : col;
        result[0] = (mAvailableSpace * OUTER_INSET_FACTOR) + (resolvedCol * stride);
        result[1] = (mAvailableSpace * OUTER_INSET_FACTOR) + (row * stride);
    }

    private void getPosition(int index, int curNumItems, float[] result) {
        int row = index / PREVIEW_GRID_SIZE;
        int col = index % PREVIEW_GRID_SIZE;
        getGridPosition(row, col, result);
    }

    public float scaleForItem(int numItems) {
        return ITEM_SIZE_FACTOR * mBaselineIconScale;
    }

    public float getIconSize() {
        return mIconSize;
    }
}
