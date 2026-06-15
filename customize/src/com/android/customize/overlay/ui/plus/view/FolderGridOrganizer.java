package com.android.customize.overlay.ui.plus.view;

import static com.android.launcher3.folder.ClippedFolderIconLayoutRule.MAX_NUM_ITEMS_IN_PREVIEW;

import android.graphics.Point;

import com.android.launcher3.model.data.FolderInfo;

public class FolderGridOrganizer {

    private final Point mPoint = new Point();
    private final int mMaxCountY;
    private int mMaxItemsPerPage;
    private int mMaxCountX;
    private int mNumItemsInFolder;
    private int mCountX;
    private int mCountY;
    private boolean mDisplayingUpperLeftQuadrant = false;

    public FolderGridOrganizer(int numFolderColumns, int numFolderRows) {
        mMaxCountX = numFolderColumns;
        mMaxCountY = numFolderRows;
        mMaxItemsPerPage = mMaxCountX * mMaxCountY;
    }

    public void setNumFolderColumns(int numFolderColumns) {
        mMaxCountX = numFolderColumns;
        mMaxItemsPerPage = mMaxCountX * mMaxCountY;
        calculateGridSize(mNumItemsInFolder);
    }

    public FolderGridOrganizer setFolderInfo(FolderInfo info) {
        return setContentSize(info.contents.size());
    }

    public FolderGridOrganizer setContentSize(int contentSize) {
        if (contentSize != mNumItemsInFolder) {
            calculateGridSize(contentSize);

            mDisplayingUpperLeftQuadrant = contentSize > MAX_NUM_ITEMS_IN_PREVIEW;
            mNumItemsInFolder = contentSize;
        }
        return this;
    }

    public int getCountX() {
        return mCountX;
    }

    public int getCountY() {
        return mCountY;
    }

    public int getMaxItemsPerPage() {
        return mMaxItemsPerPage;
    }

    private void calculateGridSize(int count) {
        mCountX = mMaxCountX;

        mCountY = (int) Math.ceil((double) count / mCountX);

        if (mCountY > mMaxCountY) {
            mCountY = mMaxCountY;
        }
    }

    public Point getPosForRank(int rank) {
        int pagePos = rank % mMaxItemsPerPage;
        mPoint.x = pagePos % mCountX;
        mPoint.y = pagePos / mCountX;
        return mPoint;
    }

    public boolean isItemInPreview(int rank) {
        return isItemInPreview(0, rank);
    }

    public boolean isItemInPreview(int page, int rank) {
        if (page > 0 || mDisplayingUpperLeftQuadrant) {
            int col = rank % mCountX;
            int row = rank / mCountX;
            return col < mCountX && row < mCountY;
        }
        return rank < MAX_NUM_ITEMS_IN_PREVIEW;
    }
}
