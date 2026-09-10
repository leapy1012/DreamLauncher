package com.android.launcher3.folder.large;

import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.folder.FolderGridOrganizer;
import com.android.launcher3.folder.ClippedFolderIconLayoutRule;

public class HxyFolderGridOrganizer extends FolderGridOrganizer {
    public HxyFolderGridOrganizer(InvariantDeviceProfile profile) {
        super(profile);
        // Keep profile folder grid (ColorOS hxy: 3 cols x 4 rows).
        this.mMaxItemsPerPage = this.mMaxCountX * this.mMaxCountY;
    }

    @Override
    public void calculateGridSize(int count) {
        // Full-folder style: always use the max page grid so open layout matches Oppo.
        this.mCountX = this.mMaxCountX;
        this.mCountY = this.mMaxCountY;
    }

    public boolean isItemInPreview(int page, int rank) {
        return rank < ClippedFolderIconLayoutRule.MAX_NUM_ITEMS_IN_PREVIEW;
    }
}
