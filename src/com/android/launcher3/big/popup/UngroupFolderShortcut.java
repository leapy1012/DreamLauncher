package com.android.launcher3.big.popup;

import android.app.AlertDialog;
import android.view.View;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.folder.large.HxyFolderDisbandHelper;
import com.android.launcher3.folder.large.HxyLargeFolderIcon;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.popup.SystemShortcut;
import com.android.launcher3.views.ActivityContext;

/** Dissolves the folder onto the workspace (Oppo "Ungroup"). */
public class UngroupFolderShortcut extends SystemShortcut<Launcher> {
    private final HxyLargeFolderIcon mFolderIcon;

    public UngroupFolderShortcut(Launcher target, ItemInfo itemInfo, View originalView) {
        super(R.drawable.hxy_folder_ungroup, R.string.hxy_folder_ungroup, target, itemInfo,
                originalView);
        this.mFolderIcon = (HxyLargeFolderIcon) originalView;
    }

    @Override
    public void onClick(View view) {
        AbstractFloatingView.closeAllOpenViews((ActivityContext) this.mTarget);
        CharSequence title = mItemInfo.title;
        if (title == null || title.length() == 0) {
            title = mTarget.getString(R.string.folder_hint_text);
        }
        String message = mTarget.getString(R.string.hxy_folder_ungroup_message, title);
        new AlertDialog.Builder(mTarget)
                .setTitle(R.string.hxy_folder_ungroup)
                .setMessage(message)
                .setPositiveButton(R.string.hxy_folder_ungroup, (d, which) ->
                        HxyFolderDisbandHelper.ungroup(mTarget, mFolderIcon))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
