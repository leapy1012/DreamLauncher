package com.android.launcher3.folder.large.listview;

import android.content.Context;
import android.view.View;

import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.android.launcher3.R;

import java.util.List;

public class HxyLargeFolderAdapter extends BasePageLinearAdapter<WorkspaceItemInfo> {
    private int mMaxSize = -1;
    private int mPageOffset;

    public HxyLargeFolderAdapter(Context context) {
        super(context);
    }

    public void release() {
        super.release();
        this.mList = null;
    }

    public void setMaxSize(int maxSize) {
        this.mMaxSize = maxSize;
    }

    public int getMaxSize() {
        return this.mMaxSize;
    }

    public void setPageOffset(int pageOffset) {
        mPageOffset = Math.max(0, pageOffset);
    }

    public int getPageOffset() {
        return mPageOffset;
    }

    public void setList(List<WorkspaceItemInfo> list) {
        super.setList(list, false);
    }

    public int getItemCount() {
        int remaining = Math.max(0, super.getItemCount() - mPageOffset);
        return mMaxSize <= 0 ? remaining : Math.min(mMaxSize, remaining);
    }

    @Override
    public WorkspaceItemInfo getItem(int position) {
        return super.getItem(position + mPageOffset);
    }

    public int getItemLayoutID() {
        return R.layout.hxy_large_folder_icon_item;
    }

    public BasePageLinearAdapter.ItemViewModel<WorkspaceItemInfo> createItemViewModel(View itemView) {
        return new IVM(itemView, this.mContext);
    }

    public boolean isCountOut(int position) {
        int remaining = Math.max(0, super.getItemCount() - mPageOffset);
        return mMaxSize > 0 && remaining > mMaxSize && position == mMaxSize - 1;
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
            this.mIconView.bindTo(data, position + adapter.getPageOffset(),
                    isCountOut(position), getList());
        }

        private boolean isCountOut(int position) {
            return ((HxyLargeFolderAdapter) getAdapter()).isCountOut(position);
        }

        private List<WorkspaceItemInfo> getList() {
            return getAdapter().getList();
        }
    }
}
