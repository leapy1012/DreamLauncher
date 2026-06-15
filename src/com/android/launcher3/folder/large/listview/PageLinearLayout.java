package com.android.launcher3.folder.large.listview;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import com.android.launcher3.R;

public class PageLinearLayout extends ViewGroup {
    private static final int GRID = 2;
    public static final int HORIZONTAL = 0;
    private static final String TAG = "PageLinearLayout";
    public static final int VERTICAL = 1;
    private Adapter<? extends ViewHolder> mAdapter;
    protected int mChildHeight;
    protected int mChildWidth;
    protected int mHeight;
    private int mHorizontalSpace;
    private int mMaxColumn;
    private int mMaxRow;
    private final AdapterDataObservable mObserver;
    private int mOrientation;
    private int mSpanCount;
    private int mVerticalSpace;
    protected int mWidth;

    public PageLinearLayout(Context context) {
        this(context, (AttributeSet) null);
    }

    public PageLinearLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PageLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mObserver = new DataObservable();
        this.mOrientation = 0;
        this.mVerticalSpace = 0;
        this.mHorizontalSpace = 0;
        this.mSpanCount = 0;
        this.mMaxRow = -1;
        this.mMaxColumn = -1;
        this.mWidth = 0;
        this.mHeight = 0;
        this.mChildWidth = 0;
        this.mChildHeight = 0;
        this.mAdapter = null;
        initData(context, attrs);
    }

    public void release() {
        Adapter<? extends ViewHolder> adapter = this.mAdapter;
        if (adapter != null) {
            adapter.setObservable((AdapterDataObservable) null);
            this.mAdapter = null;
        }
    }

    private void initData(Context context, AttributeSet attrs) {
        TypedArray type = context.obtainStyledAttributes(attrs, R.styleable.PageLinearAttrs);
        this.mOrientation = type.getInt(R.styleable.PageLinearAttrs_orientation, 0);
        this.mVerticalSpace = (int) type.getDimension(R.styleable.PageLinearAttrs_vertical_space, 0.0f);
        this.mHorizontalSpace = (int) type.getDimension(R.styleable.PageLinearAttrs_horizontal_space, 0.0f);
        this.mSpanCount = type.getInt(R.styleable.PageLinearAttrs_span_count, 4);
        this.mMaxRow = type.getInt(R.styleable.PageLinearAttrs_max_row, -1);
        this.mMaxColumn = type.getInt(R.styleable.PageLinearAttrs_max_column, -1);
        type.recycle();
    }

    public void setSpanCount(int spanCount) {
        this.mSpanCount = spanCount;
    }

    public int getSpanCount() {
        return this.mSpanCount;
    }

    public int getOrientation() {
        return this.mOrientation;
    }

    public int getMaxRow() {
        return this.mMaxRow;
    }

    public int getMaxColumn() {
        return this.mMaxColumn;
    }

    public int getVerticalSpace() {
        return this.mVerticalSpace;
    }

    public int getHorizontalSpace() {
        return this.mHorizontalSpace;
    }

    public void setVerticalSpace(int space) {
        this.mVerticalSpace = space;
    }

    public void setHorizontalSpace(int space) {
        this.mHorizontalSpace = space;
    }

    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.mWidth = w;
        this.mHeight = h;
    }

    public boolean isVertical() {
        return 1 == getOrientation();
    }

    public boolean isHorizontal() {
        return getOrientation() == 0;
    }

    public boolean isGrid() {
        return 2 == getOrientation();
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        if (isHorizontal()) {
            onMeasureHorizontal(widthMeasureSpec, heightMeasureSpec);
        } else if (isVertical()) {
            onMeasureVertical(widthMeasureSpec, heightMeasureSpec);
        } else if (isGrid()) {
            onMeasureGrid(widthMeasureSpec, heightMeasureSpec);
        } else {
            onMeasureHorizontal(widthMeasureSpec, heightMeasureSpec);
        }
    }

    private void onMeasureHorizontal(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(measureHorizontalChildrenHeight(), MeasureSpec.EXACTLY));
    }

    private int measureHorizontalChildrenHeight() {
        int childHeight = 0;
        if (getChildCount() >= 1) {
            childHeight = getMeasureChildHeight(0);
        }
        return getPaddingTop() + childHeight + getPaddingBottom();
    }

    private void onMeasureVertical(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(measureVerticalChildrenHeight(), MeasureSpec.EXACTLY));
    }

    private int measureVerticalChildrenHeight() {
        int count = getChildCount();
        int spaceHeight = Math.max((count - 1) * this.mVerticalSpace, 0);
        int childrenHeight = 0;
        for (int i = 0; i < count; i++) {
            childrenHeight += getMeasureChildHeight(i);
        }
        return childrenHeight + spaceHeight + getPaddingTop() + getPaddingBottom();
    }

    public void onMeasureGrid(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(measureGridChildrenHeight(), MeasureSpec.EXACTLY));
    }

    private int measureGridChildrenHeight() {
        int childHeight = getChildCount() < 1 ? 0 : getMeasureChildHeight(0);
        int rowCount = getRowCount();
        return (rowCount * childHeight) + Math.max((rowCount - 1) * this.mVerticalSpace, 0) + getPaddingTop() + getPaddingBottom();
    }

    public int getMeasureChildHeight(int index) {
        return getChildAt(index).getMeasuredHeight();
    }

    public void onLayout(boolean changed, int l, int t, int r, int b) {
        if (isHorizontal()) {
            onHorizontalLayout(changed, l, t, r, b);
        } else if (isVertical()) {
            onVerticalLayout(changed, l, t, r, b);
        } else if (isGrid()) {
            onGridLayout(changed, l, t, r, b);
        } else {
            onHorizontalLayout(changed, l, t, r, b);
        }
        if (this.mChildWidth < 1 && this.mChildHeight < 1) {
            this.mChildWidth = getChildWidth();
            this.mChildHeight = getChildHeight();
        }
    }

    private void onHorizontalLayout(boolean changed, int l, int t, int r, int b) {
        int left = getPaddingLeft();
        int top = getPaddingTop();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == View.VISIBLE) {
                int right = child.getMeasuredWidth() + left;
                child.layout(left, top, right, child.getMeasuredHeight() + top);
                onChildLayout(child, left, top);
                left = this.mHorizontalSpace + right;
            }
        }
    }

    private void onVerticalLayout(boolean changed, int l, int t, int r, int b) {
        int left = getPaddingLeft();
        int top = getPaddingTop();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == View.VISIBLE) {
                int bottom = child.getMeasuredHeight() + top;
                child.layout(left, top, child.getMeasuredWidth() + left, bottom);
                onChildLayout(child, left, top);
                top = this.mVerticalSpace + bottom;
            }
        }
    }

    private void onGridLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == View.VISIBLE) {
                int left = getGridLayoutLeft(i, child.getMeasuredWidth());
                int top = getGridLayoutTop(i, child.getMeasuredHeight());
                child.layout(left, top, child.getMeasuredWidth() + left, child.getMeasuredHeight() + top);
                onChildLayout(child, left, top);
            }
        }
    }

    public int getGridLayoutLeft(int index, int childWidth) {
        return getPaddingLeft() + getGridChildLeft(index, childWidth);
    }

    public int getGridLayoutTop(int index, int childHeight) {
        return getPaddingTop() + getGridChildTop(index, childHeight);
    }

    private int getGridChildTop(int index, int childHeight) {
        return (this.mVerticalSpace + childHeight) * getRowIndex(index);
    }

    private int getGridChildLeft(int index, int childWidth) {
        return (this.mHorizontalSpace + childWidth) * getColumnIndex(index);
    }

    public void onChildLayout(View child, int left, int top) {
    }

    public int getChildWidth() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == View.VISIBLE) {
                return child.getMeasuredWidth();
            }
        }
        return 0;
    }

    public int getChildHeight() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == View.VISIBLE) {
                return child.getMeasuredHeight();
            }
        }
        return 0;
    }

    public int getRowIndex(int index) {
        if (isVertical()) {
            return index;
        }
        if (isGrid()) {
            return index / this.mSpanCount;
        }
        return index >= 0 ? 1 : 0;
    }

    public int getColumnIndex(int index) {
        if (isVertical()) {
            return index >= 0 ? 1 : 0;
        }
        if (isGrid()) {
            return index % this.mSpanCount;
        }
        return index;
    }

    public int getRowCount() {
        if (isVertical()) {
            return getChildCount();
        }
        if (isGrid()) {
            if (getChildCount() % this.mSpanCount > 0) {
                return (getChildCount() / this.mSpanCount) + 1;
            }
            return getChildCount() / this.mSpanCount;
        } else if (getChildCount() > 0) {
            return 1;
        } else {
            return 0;
        }
    }

    public void setAdapter(Adapter<? extends ViewHolder> adapter) {
        this.mAdapter = adapter;
        if (adapter != null) {
            adapter.setObservable(this.mObserver);
            this.mAdapter.notifyDataSetChanged();
        }
    }

    public void notifyChild(int position) {
        this.mAdapter.onBindView(getChildAt(position), position);
    }

    public void notifyChildren() {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            notifyChild(i);
        }
    }

    private boolean isEmpty() {
        Adapter<? extends ViewHolder> adapter = this.mAdapter;
        if (adapter == null || adapter.getItemCount() < 1) {
            return true;
        }
        return false;
    }

    public void removeItemView(int position) {
        if (getChildCount() > position) {
            removeViewAt(position);
        }
        if (this.mAdapter.getItemCount() > position) {
            this.mAdapter.removeItem(position);
        }
        notifyChildren();
    }

    public void computeChildren() {
        if (isEmpty()) {
            removeAllViews();
            return;
        }
        int itemCount = this.mAdapter.getItemCount();
        int childCount = getChildCount();
        if (childCount > itemCount) {
            int offset = childCount - itemCount;
            removeViews(childCount - offset, offset);
        }
        for (int i = childCount; i < itemCount; i++) {
            addView(this.mAdapter.createView(this, i), i);
        }
    }

    private class DataObservable extends AdapterDataObservable {
        private DataObservable() {
        }

        public void notifyDataSetChanged() {
            PageLinearLayout.this.computeChildren();
            PageLinearLayout.this.requestLayout();
            PageLinearLayout.this.notifyChildren();
        }

        public void notifyItemChanged(int position) {
            PageLinearLayout.this.notifyChild(position);
        }
    }

    public static abstract class Adapter<VH extends ViewHolder> {
        private static final int ITEM_VH_KEY = -1;
        private AdapterDataObservable mObservable = null;

        public abstract int getItemCount();

        public abstract void onBindViewHolder(VH vh, int i);

        public abstract VH onCreateViewHolder(ViewGroup viewGroup, int i);

        public abstract void removeItem(int i);

        public void release() {
            this.mObservable = null;
        }

        private VH changeVH(Object object) {
            return (VH) object;
        }

        public void onBindView(View view, int position) {
            VH vh = changeVH(view.getTag(-1));
            vh.mPosition = position;
            onBindViewHolder(vh, position);
        }

        public View createView(ViewGroup parent, int position) {
            VH vh = onCreateViewHolder(parent, position);
            vh.itemView.setTag(-1, vh);
            vh.mPosition = position;
            return vh.itemView;
        }

        public final void setObservable(AdapterDataObservable observable) {
            this.mObservable = observable;
        }

        public final void notifyDataSetChanged() {
            AdapterDataObservable adapterDataObservable = this.mObservable;
            if (adapterDataObservable != null) {
                adapterDataObservable.notifyDataSetChanged();
            }
        }

        public final void notifyItemChanged(int position) {
            AdapterDataObservable adapterDataObservable = this.mObservable;
            if (adapterDataObservable != null) {
                adapterDataObservable.notifyItemChanged(position);
            }
        }
    }

    public static abstract class ViewHolder {
        public static final int NO_POSITION = -1;
        public final View itemView;
        int mPosition = -1;

        public ViewHolder(View itemView2) {
            this.itemView = itemView2;
        }
    }

    public static abstract class AdapterDataObservable {
        public void notifyDataSetChanged() {
        }

        public void notifyItemChanged(int position) {
        }
    }
}
