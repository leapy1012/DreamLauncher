package com.android.launcher3.big.popup;

import android.content.DialogInterface;
import android.view.View;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.folder.large.LargeFolderDisbandHelper;
import com.android.launcher3.folder.large.LargeFolderIcon;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.popup.DreamAlertPopup;
import com.android.launcher3.popup.SystemShortcut;
import com.android.launcher3.views.ActivityContext;

/** Dissolves the folder onto the workspace (Oppo "Ungroup"). */
public class UngroupFolderShortcut extends SystemShortcut<Launcher> {
    private final LargeFolderIcon mFolderIcon;

    public UngroupFolderShortcut(Launcher target, ItemInfo itemInfo, View originalView) {
        super(R.drawable.hxy_folder_ungroup, R.string.hxy_folder_ungroup, target, itemInfo,
                originalView);
        this.mFolderIcon = (LargeFolderIcon) originalView;
    }

    @Override
    public void onClick(View view) {
        AbstractFloatingView.closeAllOpenViews((ActivityContext) this.mTarget);
        CharSequence title = mItemInfo.title;
        if (title == null || title.length() == 0) {
            title = mTarget.getString(R.string.folder_hint_text);
        }
        new DreamAlertPopup(mTarget).createMultiMessageDialog(
                mTarget.getString(R.string.disband_alert_popup_title, title),
                mTarget.getString(R.string.disband_alert_popup_message),
                mTarget.getString(R.string.new_disband_action),
                mTarget.getString(R.string.alert_hide_cancel),
                null,
                (dialog, which) -> {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        LargeFolderDisbandHelper.ungroup(mTarget, mFolderIcon);
                    }
                });
    }
}
