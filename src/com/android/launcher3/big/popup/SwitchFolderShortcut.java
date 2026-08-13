package com.android.launcher3.big.popup;

import android.view.View;
import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.popup.SystemShortcut;
import com.android.launcher3.views.ActivityContext;
import com.android.launcher3.folder.large.HxyLargeFolderIcon;
import com.android.launcher3.folder.large.HxyLargeFolderProxy;

public class SwitchFolderShortcut extends SystemShortcut<Launcher> {
    private final HxyLargeFolderIcon mView;

    public SwitchFolderShortcut(Launcher target, ItemInfo itemInfo, View originalView) {
        super(HxyLargeFolderProxy.isLargeFolder(itemInfo)
                        ? R.drawable.ic_folder_convert_icon_minification
                        : R.drawable.ic_folder_convert_icon_enlarge,
                HxyLargeFolderProxy.isLargeFolder(itemInfo)
                        ? R.string.coloros_folder_shrink : R.string.coloros_folder_enlarge,
                target, itemInfo, originalView);
        this.mView = (HxyLargeFolderIcon) originalView;
    }

    @Override
    public void onClick(View view) {
        AbstractFloatingView.closeAllOpenViews((ActivityContext) this.mTarget);
        this.mView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mView.switchFolderSize();
            }
        }, 300);
    }
}
