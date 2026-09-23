package com.android.launcher3.folder.large;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import com.android.launcher3.folder.Folder;

public class LargeFolder extends Folder {
    public LargeFolder(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void closeComplete(boolean wasAnimated) {
        super.closeComplete(wasAnimated);
        if (!(getFolderIcon() instanceof LargeFolderIcon icon)) {
            return;
        }
        if (LargeFolderProxy.isLargeFolder((View) icon)) {
            icon.refreshListData();
        } else {
            // After shrink, ensure list preview stays hidden (clipped preview only).
            icon.initLoadListData();
        }
    }
}
