package com.android.launcher3.folder.large.listview;

import android.content.Context;
import android.view.View;

import com.android.launcher3.folder.large.HxyBigFolderPreviewModes;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.android.launcher3.R;

import java.util.List;

public class HxyLargeFolderAdapter extends BasePageLinearAdapter<WorkspaceItemInfo> {
    private int mMaxSize = -1;
    private int mPageIndex = 0;
    private ItemInfo mFolderInfo;

    public HxyLargeFolderAdapter(Context context) {
        super(context);
    }

    public void release() {
        super.release();
        this.mList = null;
        this.mFolderInfo = null;
    }

    public void setMaxSize(int maxSize) {
        this.mMaxSize = maxSize;
    }

    public int getMaxSize() {
        return this.mMaxSize;
    }

    public void setFolderInfo(ItemInfo info) {
        mFolderInfo = info;
    }

    public void setPageIndex(int pageIndex) {
        mPageIndex = Math.max(0, pageIndex);
    }

    public int getPageIndex() {
        return mPageIndex;
    }

    public int getPageStart() {
        return HxyBigFolderPreviewModes.getPageStartIndex(mFolderInfo, mPageIndex);
    }

    public void setList(List<WorkspaceItemInfo> list) {
        super.setList(list, false);
    }

    public void setList(List<WorkspaceItemInfo> list, int pageIndex) {
        mPageIndex = Math.max(0, pageIndex);
        super.setList(list, false);
    }

    @Override
    public WorkspaceItemInfo getItem(int position) {
        if (mList == null) {
            return null;
        }
        int index = getPageStart() + position;
        if (index < 0 || index >= mList.size()) {
            return null;
        }
        return mList.get(index);
    }

    public int getItemCount() {
        if (mList == null || mMaxSize <= 0) {
            return 0;
        }
        int start = getPageStart();
        int remaining = mList.size() - start;
        if (remaining <= 0) {
            return 0;
        }
        int withoutStacked = mMaxSize - 1;
        // Exactly withoutStacked+1 apps → show all full icons (no stack).
        if (remaining <= mMaxSize) {
            return remaining;
        }
        // More than a full page → withoutStacked full + 1 stack cell.
        return mMaxSize;
    }

    public int getItemLayoutID() {
        return R.layout.hxy_large_folder_icon_item;
    }

    public BasePageLinearAdapter.ItemViewModel<WorkspaceItemInfo> createItemViewModel(View itemView) {
        return new IVM(itemView, this.mContext);
    }

    /**
     * ColorOS overflow slot on this page: last cell is a 2×2 stack only when
     * remaining content on this page exceeds max.
     */
    public boolean isCountOut(int position) {
        if (this.mMaxSize <= 0 || this.mList == null) {
            return false;
        }
        int withoutStacked = this.mMaxSize - 1;
        if (position != withoutStacked) {
            return false;
        }
        int remaining = this.mList.size() - getPageStart();
        return remaining > this.mMaxSize && (remaining - withoutStacked) > 1;
    }

    public static class IVM extends BasePageLinearAdapter.ItemViewModel<WorkspaceItemInfo> {
        private HxyLargeFolderIconItem mIconView = null;

        public IVM(View root, Context context) {
            super(root, context);
            this.mIconView = root.findViewById(R.id.folder_icon_content_item);
        }

        public boolean isItemClick() {
            return true;
        }

        public boolean isItemLongClick() {
            return true;
        }

        public void bindTo(WorkspaceItemInfo data, int position) {
            this.mPosition = position;
            HxyLargeFolderAdapter adapter = (HxyLargeFolderAdapter) getAdapter();
            // List index for stack overflow must be absolute in the full contents.
            int listIndex = adapter.getPageStart() + position;
            this.mIconView.bindTo(data, listIndex, adapter.isCountOut(position), adapter.getList());
        }
    }
}
