package com.android.launcher3.big.add;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.android.launcher3.Launcher;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.big.HxyBubbleTextView;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.model.data.WorkspaceItemInfo;

public class HxyAddBubbleTextView extends HxyBubbleTextView implements View.OnClickListener {
    Folder mFolder;

    public HxyAddBubbleTextView(Context context) {
        super(context);
    }

    public HxyAddBubbleTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HxyAddBubbleTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setFolder(Folder folder) {
        this.mFolder = folder;
    }

    public Folder getFolder() {
        return this.mFolder;
    }

    public void onClick(View v) {
        if (((WorkspaceItemInfo) ((ItemInfo) v.getTag())).itemType == Folder.ITEM_TYPE_ADD_FOLDER) {
            AddFolderItemToolFullSheet show = AddFolderItemToolFullSheet.show(Launcher.getLauncher(v.getContext()), true, this);
        }
    }
}
