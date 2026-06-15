package com.android.launcher3.folder.large.listview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.util.List;

public abstract class BasePageLinearAdapter<D> extends PageLinearLayout.Adapter<BasePageLinearAdapter.MyViewHolder<D>> {
    protected static final String TAG = "PageLinearAdapter";
    protected Context mContext = null;
    private IDataCallback<D> mDataCallback = null;
    protected LayoutInflater mInflater = null;
    private ItemClickListener<D> mItemListener = null;
    private ItemLongClickListener<D> mItemLongListener = null;
    protected List<D> mList = null;

    public interface IDataCallback<D> {
        D getItem(int i);

        int getItemCount();

        void removeItem(int i);
    }

    public interface ItemClickListener<T> {
        void onItemClick(View view, T t);
    }

    public interface ItemLongClickListener<T> {
        void onItemLongClick(View view, T t);
    }

    public abstract ItemViewModel<D> createItemViewModel(View view);

    public abstract int getItemLayoutID();

    public BasePageLinearAdapter(Context context) {
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
    }

    public void release() {
        super.release();
        this.mContext = null;
        this.mInflater = null;
        this.mItemListener = null;
        this.mItemLongListener = null;
        this.mDataCallback = null;
    }

    public void setDataCallback(IDataCallback<D> data) {
        this.mDataCallback = data;
    }

    public void clearList() {
        List<D> list = this.mList;
        if (list != null) {
            list.clear();
            this.mList = null;
        }
    }

    public Context getContext() {
        return this.mContext;
    }

    public void setList(List<D> list) {
        setList(list, true);
    }

    public void setList(List<D> list, boolean isClearList) {
        if (isClearList) {
            clearList();
        }
        this.mList = null;
        this.mList = list;
        refresh();
    }

    public List<D> getList() {
        return this.mList;
    }

    public void setItemListener(ItemClickListener<D> listener) {
        this.mItemListener = listener;
    }

    public void setItemLongListener(ItemLongClickListener<D> listener) {
        this.mItemLongListener = listener;
    }

    public void onItemClick(View itemView, int position) {
        ItemClickListener<D> itemClickListener = this.mItemListener;
        if (itemClickListener != null) {
            itemClickListener.onItemClick(itemView, getItem(position));
        }
    }

    public void onItemLongClick(View itemView, int position) {
        ItemLongClickListener<D> itemLongClickListener = this.mItemLongListener;
        if (itemLongClickListener != null) {
            itemLongClickListener.onItemLongClick(itemView, getItem(position));
        }
    }

    public void refresh() {
        notifyDataSetChanged();
    }

    public void refresh(int position) {
        notifyItemChanged(position);
    }

    public int getItemCount() {
        IDataCallback<D> iDataCallback = this.mDataCallback;
        if (iDataCallback != null) {
            return iDataCallback.getItemCount();
        }
        List<D> list = this.mList;
        if (list == null) {
            return 0;
        }
        return list.size();
    }

    public void removeItem(int position) {
        IDataCallback<D> iDataCallback = this.mDataCallback;
        if (iDataCallback != null) {
            iDataCallback.removeItem(position);
            return;
        }
        List<D> list = this.mList;
        if (list != null && list.size() > position) {
            this.mList.remove(position);
        }
    }

    public D getItem(int position) {
        IDataCallback<D> iDataCallback = this.mDataCallback;
        if (iDataCallback != null) {
            return iDataCallback.getItem(position);
        }
        List<D> list = this.mList;
        if (list == null || list.size() <= position) {
            return null;
        }
        return this.mList.get(position);
    }

    public MyViewHolder<D> onCreateViewHolder(ViewGroup parent, int position) {
        View itemView = this.mInflater.inflate(getItemLayoutID(), parent, false);
        return new MyViewHolder<>(itemView, createItemViewModel(itemView), this);
    }

    public void onBindViewHolder(MyViewHolder<D> holder, int position) {
        holder.bindTo(getItem(position), position);
    }
    public static class MyViewHolder<D> extends PageLinearLayout.ViewHolder {
        private ItemViewModel<D> mModel = null;

        public MyViewHolder(View root, ItemViewModel<D> model, BasePageLinearAdapter<D> listener) {
            super(root);
            model.initListener(listener);
            this.mModel = model;
        }

        public void bindTo(D data, int position) {
            this.mModel.setPosition(position);
            this.mModel.bindTo(data, position);
        }
    }

    public static abstract class ItemViewModel<D> {
        private WeakReference<Context> mContext;
        private WeakReference<BasePageLinearAdapter<D>> mListener;
        protected int mPosition;
        protected View mRoot;

        public abstract void bindTo(D d, int i);

        public ItemViewModel(View root) {
            this(root, (Context) null);
        }

        public ItemViewModel(View root, Context context) {
            this.mListener = null;
            this.mContext = null;
            this.mPosition = -1;
            this.mRoot = root;
            if (context != null) {
                this.mContext = new WeakReference<>(context);
            }
            if (isItemClick()) {
                root.setOnClickListener(new ItemViewModelOnClickListener<>(this));
            }
            if (isItemLongClick()) {
                root.setOnLongClickListener(new ItemViewModelOnLongClickListener<>(this));
            }
        }
        public boolean executeLongClick(View v) {
            onItemLongClick(v);
            return true;
        }
        public boolean isItemClick() {
            return false;
        }
        public boolean isItemLongClick() {
            return false;
        }
        public void setPosition(int mPosition2) {
            this.mPosition = mPosition2;
        }
        public void onItemClick(View itemView) {
            BasePageLinearAdapter<D> adapter = getAdapter();
            if (adapter != null) {
                adapter.onItemClick(itemView, this.mPosition);
            }
        }

        private void onItemLongClick(View itemView) {
            BasePageLinearAdapter<D> adapter = getAdapter();
            if (adapter != null) {
                adapter.onItemLongClick(itemView, this.mPosition);
            }
        }

        protected String getString(int id) {
            if (getContext() != null) {
                return getContext().getResources().getString(id);
            }
            return "";
        }

        public Context getContext() {
            WeakReference<Context> weakReference = this.mContext;
            if (weakReference != null) {
                return (Context) weakReference.get();
            }
            return null;
        }

        public BasePageLinearAdapter<D> getAdapter() {
            WeakReference<BasePageLinearAdapter<D>> weakReference = this.mListener;
            if (weakReference == null || weakReference.get() == null) {
                return null;
            }
            return this.mListener.get();
        }

        public void initListener(BasePageLinearAdapter<D> listener) {
            this.mListener = new WeakReference<>(listener);
        }

        public boolean isLastPosition(int position) {
            WeakReference<BasePageLinearAdapter<D>> weakReference;
            if (position >= 0 && (weakReference = this.mListener) != null && weakReference.get() != null && ((BasePageLinearAdapter<D>) this.mListener.get()).getItemCount() == position + 1) {
                return true;
            }
            return false;
        }

        public boolean isFirstPosition(int position) {
            WeakReference<BasePageLinearAdapter<D>> weakReference;
            if (position >= 0 && (weakReference = this.mListener) != null && weakReference.get() != null && position == 0) {
                return true;
            }
            return false;
        }

        public static class ItemViewModelOnClickListener<D> implements View.OnClickListener {
            public WeakReference<ItemViewModel<D>> itemViewModelRef;

            public ItemViewModelOnClickListener(ItemViewModel<D> itemViewModel) {
                this.itemViewModelRef = new WeakReference<>(itemViewModel);
            }

            public final void onClick(View view) {
                ItemViewModel<D> itemViewModel = itemViewModelRef.get();
                if (itemViewModel != null) {
                    itemViewModel.onItemClick(view);
                }
            }
        }

        public static class ItemViewModelOnLongClickListener<D> implements View.OnLongClickListener {
            public WeakReference<ItemViewModel<D>> itemViewModelRef;

            public ItemViewModelOnLongClickListener(ItemViewModel<D> itemViewModel) {
                this.itemViewModelRef = new WeakReference<>(itemViewModel);
            }

            public final boolean onLongClick(View view) {
                return itemViewModelRef.get().executeLongClick(view);
            }
        }
    }
}
