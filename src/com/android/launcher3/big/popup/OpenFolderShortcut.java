package com.android.launcher3.big.popup;

import android.view.View;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.folder.large.HxyLargeFolderIcon;
import com.android.launcher3.folder.large.HxyLargeFolderProxy;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.popup.SystemShortcut;
import com.android.launcher3.touch.ItemClickHandler;
import com.android.launcher3.views.ActivityContext;

/** Opens the long-pressed folder (Oppo "Open folder"). */
public class OpenFolderShortcut extends SystemShortcut<Launcher> {
    private final HxyLargeFolderIcon mFolderIcon;

    public OpenFolderShortcut(Launcher target, ItemInfo itemInfo, View originalView) {
        super(R.drawable.hxy_folder_open, R.string.hxy_folder_open, target, itemInfo, originalView);
        this.mFolderIcon = (HxyLargeFolderIcon) originalView;
    }

    @Override
    public boolean isEnabled() {
        return HxyLargeFolderProxy.isLargeFolder(mItemInfo);
    }

    @Override
    public void onClick(View view) {
        AbstractFloatingView.closeAllOpenViews((ActivityContext) this.mTarget);
        this.mFolderIcon.post(() -> ItemClickHandler.onClick(mFolderIcon));
    }
}
