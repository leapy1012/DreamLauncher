package com.android.launcher3.folder.large.listview;

import android.content.Context;
import android.view.View;

import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.android.launcher3.R;

import java.util.List;

public class HxyLargeFolderAdapter extends BasePageLinearAdapter<WorkspaceItemInfo> {
    private int mMaxSize = -1;

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

    public void setList(List<WorkspaceItemInfo> list) {
        super.setList(list, false);
    }

    public int getItemCount() {
        int i = this.mMaxSize;
        if (i <= 0 || i >= super.getItemCount()) {
            return super.getItemCount();
        }
        return this.mMaxSize;
    }

    public int getItemLayoutID() {
        return R.layout.hxy_large_folder_icon_item;
    }

    public BasePageLinearAdapter.ItemViewModel<WorkspaceItemInfo> createItemViewModel(View itemView) {
        return new IVM(itemView, this.mContext);
    }

    public boolean isCountOut(int position) {
        int i = this.mMaxSize;
        // Four apps fill the four large slots. Starting with the fifth app,
        // slot four represents a stack containing items 4..7.
        return i > 0 && super.getItemCount() > i && position == i - 1;
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
            this.mIconView.bindTo(data, position, isCountOut(position), getList());
        }

        private boolean isCountOut(int position) {
            return ((HxyLargeFolderAdapter) getAdapter()).isCountOut(position);
        }

        private List<WorkspaceItemInfo> getList() {
            return getAdapter().getList();
        }
    }
}
