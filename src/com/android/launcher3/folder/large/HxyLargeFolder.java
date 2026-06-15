package com.android.launcher3.folder.large;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import com.android.launcher3.folder.Folder;

public class HxyLargeFolder extends Folder {
    public HxyLargeFolder(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void closeComplete(boolean wasAnimated) {
        super.closeComplete(wasAnimated);
        if ((getFolderIcon() instanceof HxyLargeFolderIcon) && HxyLargeFolderProxy.isLargeFolder((View) getFolderIcon())) {
            ((HxyLargeFolderIcon) getFolderIcon()).refreshListData();
        }
    }
}
