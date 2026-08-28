package com.android.launcher3.folder.large;

import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.folder.FolderGridOrganizer;
import com.android.launcher3.folder.ClippedFolderIconLayoutRule;

public class HxyFolderGridOrganizer extends FolderGridOrganizer {
    public HxyFolderGridOrganizer(InvariantDeviceProfile profile) {
        super(profile);
        this.mMaxCountX = 3;
        this.mMaxCountY = 3;
        this.mMaxItemsPerPage = this.mMaxCountX * this.mMaxCountY;
    }

    @Override
    public void calculateGridSize(int count) {
        int i = this.mCountX;
        int i2 = this.mCountY;
        if (count >= this.mMaxItemsPerPage) {
            int gridCountX = this.mMaxCountX;
            int gridCountY = this.mMaxCountY;
        } else if (count > 3) {
            int gridCountY2 = ((count + 1) / 4) + 1;
        } else {
            int gridCountX2 = count;
        }
        this.mCountX = this.mMaxCountX;
        this.mCountY = this.mMaxCountY;
    }

    public boolean isItemInPreview(int page, int rank) {
        return rank < ClippedFolderIconLayoutRule.MAX_NUM_ITEMS_IN_PREVIEW;
    }
}
