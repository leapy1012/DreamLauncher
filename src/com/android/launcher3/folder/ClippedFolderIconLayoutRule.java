package com.android.launcher3.folder;

/**
 * Closed-folder preview layout.
 *
 * ColorOS default 1×1 folders use a fixed <b>3×3</b> grid with a small baseline
 * scale (~0.18 of the folder plate) plus explicit edge padding and inter-icon gaps
 * ({@code OplusClippedFolderIconLayoutRule#initForDefaultIcon} /
 * {@code FolderInfo#getPreviewColumn} → 3 /
 * {@code SizeSpacingConfig.SMALL_FOLDER_ICON_SCALE_FACTOR}).
 */
public class ClippedFolderIconLayoutRule {

    /** ColorOS small-folder preview: 3×3 = up to 9 icons. */
    public static final int MAX_NUM_ITEMS_IN_PREVIEW = 9;
    private static final int PREVIEW_COLUMNS = 3;

    /**
     * Kept for DeviceProfile / hit-box consumers that still expect AOSP's slight
     * overflow factor. Preview icons themselves stay inside the plate.
     */
    private static final float MAX_RADIUS_DILATION = 0.25f;
    public static final float ICON_OVERLAP_FACTOR = 1 + (MAX_RADIUS_DILATION / 2f);

    /**
     * ColorOS {@code SizeSpacingConfig.SMALL_FOLDER_ICON_SCALE_FACTOR}:
     * 0.18 (round/oval) / 0.20 (rectangle).
     */
    private static final float SMALL_FOLDER_ICON_SCALE = 0.18f;

    /**
     * ColorOS {@code getContentPaddingFactor} for 1×1 small folder (oval):
     * {@code (6 * 0.2) + 2 = 3.2}.
     */
    private static final float CONTENT_PADDING_FACTOR = 3.2f;

    public static final int EXIT_INDEX = -2;
    public static final int ENTER_INDEX = -3;

    private final float[] mTmpPoint = new float[2];

    private float mAvailableSpace;
    private float mIconSize;
    private boolean mIsRtl;
    private float mBaselineIconScale;
    private float mBaselineIconSize;
    private float mPreviewPadding;
    private float mPreviewSubIconGap;

    public void init(int availableSpace, float intrinsicIconSize, boolean rtl) {
        mAvailableSpace = availableSpace;
        mIconSize = intrinsicIconSize;
        mIsRtl = rtl;

        // Layout slot size is a fraction of the folder plate; drawable scale is
        // chosen so the drawn icon matches that slot even if intrinsic ≠ plate.
        mBaselineIconSize = mAvailableSpace * SMALL_FOLDER_ICON_SCALE;
        mBaselineIconScale = intrinsicIconSize > 0
                ? mBaselineIconSize / intrinsicIconSize
                : SMALL_FOLDER_ICON_SCALE;

        float cols = PREVIEW_COLUMNS;
        mPreviewPadding = (mAvailableSpace - (mBaselineIconSize * cols))
                / CONTENT_PADDING_FACTOR;
        mPreviewSubIconGap = ((mAvailableSpace - (mPreviewPadding * 2f))
                - (cols * mBaselineIconSize)) / (cols * 2f);
    }

    public PreviewItemDrawingParams computePreviewItemDrawingParams(int index, int curNumItems,
            PreviewItemDrawingParams params) {
        float totalScale = scaleForItem(curNumItems);

        if (index == EXIT_INDEX) {
            // Slide-out target just past the right of the 3×3 grid.
            getGridPosition(0, PREVIEW_COLUMNS, mTmpPoint);
        } else if (index == ENTER_INDEX) {
            getGridPosition(1, PREVIEW_COLUMNS, mTmpPoint);
        } else if (index >= MAX_NUM_ITEMS_IN_PREVIEW) {
            mTmpPoint[0] = mTmpPoint[1] =
                    mAvailableSpace / 2f - (mIconSize * totalScale) / 2f;
        } else {
            getGridPosition(index / PREVIEW_COLUMNS, index % PREVIEW_COLUMNS, mTmpPoint);
        }

        float transX = mTmpPoint[0];
        float transY = mTmpPoint[1];

        if (params == null) {
            params = new PreviewItemDrawingParams(transX, transY, totalScale);
        } else {
            params.update(transX, transY, totalScale);
        }
        return params;
    }

    private void getGridPosition(int row, int col, float[] result) {
        int drawCol = col;
        if (mIsRtl) {
            drawCol = PREVIEW_COLUMNS - 1 - col;
        }
        float stride = mBaselineIconSize + (mPreviewSubIconGap * 2f);
        result[0] = mPreviewPadding + mPreviewSubIconGap + (drawCol * stride);
        result[1] = mPreviewPadding + mPreviewSubIconGap + (row * stride);
    }

    public float scaleForItem(int numItems) {
        return mBaselineIconScale;
    }

    public float getIconSize() {
        return mIconSize;
    }
}
