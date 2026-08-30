package com.android.launcher3.big.popup;

import android.view.View;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.folder.large.HxyLargeFolderIcon;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.popup.SystemShortcut;
import com.android.launcher3.touch.ItemClickHandler;
import com.android.launcher3.views.ActivityContext;

/** Opens the folder and starts rename (Oppo "Rename"). */
public class RenameFolderShortcut extends SystemShortcut<Launcher> {
    private final HxyLargeFolderIcon mFolderIcon;

    public RenameFolderShortcut(Launcher target, ItemInfo itemInfo, View originalView) {
        super(R.drawable.hxy_folder_rename, R.string.hxy_folder_rename, target, itemInfo,
                originalView);
        this.mFolderIcon = (HxyLargeFolderIcon) originalView;
    }

    @Override
    public void onClick(View view) {
        AbstractFloatingView.closeAllOpenViews((ActivityContext) this.mTarget);
        this.mFolderIcon.post(() -> {
            ItemClickHandler.onClick(mFolderIcon);
            Folder folder = mFolderIcon.getFolder();
            if (folder != null && !folder.isDestroyed()) {
                folder.post(folder::startEditingFolderName);
            }
        });
    }
}
