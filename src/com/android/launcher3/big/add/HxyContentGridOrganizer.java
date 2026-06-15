package com.android.launcher3.big.add;

import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.folder.ClippedFolderIconLayoutRule;
import com.android.launcher3.folder.FolderGridOrganizer;

public class HxyContentGridOrganizer extends FolderGridOrganizer {
    public HxyContentGridOrganizer(InvariantDeviceProfile profile) {
        super(profile);
        this.mMaxCountX = 4;
        this.mMaxCountY = 5;
        this.mMaxItemsPerPage = this.mMaxCountX * this.mMaxCountY;
    }

    @Override
    public void calculateGridSize(int count) {
        this.mCountX = this.mMaxCountX;
        this.mCountY = this.mMaxCountY;
    }

    public boolean isItemInPreview(int page, int rank) {
        if (page > 0 || this.mDisplayingUpperLeftQuadrant) {
            int col = rank % this.mCountX;
            int row = rank / this.mCountX;
            if (col >= 3 || row >= 3) {
                return false;
            }
            return true;
        } else if (rank < ClippedFolderIconLayoutRule.MAX_NUM_ITEMS_IN_PREVIEW) {
            return true;
        } else {
            return false;
        }
    }
}