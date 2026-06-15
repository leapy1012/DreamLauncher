package com.android.launcher3.screenedit;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroupOverlay;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.android.launcher3.R;

import java.util.ArrayList;
import java.util.List;

public class GridGallery extends LinearLayout {
    // 布局加载器，用于加载布局文件
    public LayoutInflater layoutInflater;
    // 视图翻页器，用于实现页面切换
    public ViewPager viewPager;
    // 画廊光标视图，用于显示光标
    public GalleryCursorView galleryCursorView;
    // 页面适配器，用于管理页面数据
    public PageAdapter pageAdapter;
    // 页面数量
    public int pageCount;
    // 每页的项数
    public int itemsPerPage;
    // 网格的列数
    public int gridColumns;
    // 网格的行数
    public int gridRows;
    // 总项数
    public int totalItems;
    // 视图列表，存储所有页面视图
    public List<View> pageViews;
    // 网格画廊适配器，用于管理网格数据
    public GridGalleryAdapter gridGalleryAdapter;

    public class GridViewAdapter extends BaseAdapter {
        // 起始项索引
        public int startItemIndex;
        // 项数
        public int itemCount;

        public GridViewAdapter(int startItemIndex, int itemCount) {
            this.startItemIndex = startItemIndex;
            this.itemCount = itemCount;
        }

        public int getCount() {
            return this.itemCount;
        }

        public Object getItem(int position) {
            return GridGallery.this.gridGalleryAdapter.getItem(position + this.startItemIndex);
        }

        public long getItemId(int position) {
            return (long) position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            return GridGallery.this.gridGalleryAdapter.getView(position + this.startItemIndex, convertView, parent);
        }
    }

    public class PageAdapter extends PagerAdapter {
        // 页面视图列表
        public List<View> pageViews = new ArrayList<>();

        public PageAdapter() {
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            try {
                container.removeView(this.pageViews.get(position));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getCount() {
            return this.pageViews.size();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = this.pageViews.get(position);
            try {
                ViewGroup parent = (ViewGroup) view.getParent();
                if (parent != null) {
                    parent.removeAllViews();
                }
                container.addView(view);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return this.pageViews.get(position);
        }

        public boolean isViewFromObject(View view, Object obj) {
            return view == obj;
        }

        public void setData(List<View> views) {
            this.pageViews.clear();
            this.pageViews.addAll(views);
            notifyDataSetChanged();
        }
    }

    public GridGallery(Context context) {
        this(context, (AttributeSet) null);
    }

    /**
     * 创建指定位置的网格视图页面。
     * @param parent 父视图组
     * @param pageIndex 页面索引
     * @return 网格视图页面
     */
    public final View createGridViewPage(ViewGroup parent, int pageIndex) {
        GridView gridView = (GridView) this.layoutInflater.inflate(R.layout.grid_gallery_page, parent, false);
        gridView.setNumColumns(this.gridColumns);
        int remainingItems = this.totalItems - (pageIndex * this.itemsPerPage);
        int itemsToShow = Math.min(remainingItems, this.itemsPerPage);
        gridView.setAdapter(new GridViewAdapter(pageIndex * this.itemsPerPage, itemsToShow));
        return gridView;
    }

    /**
     * 初始化网格画廊的参数。
     */
    public final void initializeGalleryParams() {
        this.totalItems = this.gridGalleryAdapter.getCount();
        this.gridColumns = this.gridGalleryAdapter.getGridColumns();
        this.gridRows = this.gridGalleryAdapter.getGridRows();
        this.itemsPerPage = this.gridColumns * this.gridRows;
        this.pageCount = (int) Math.ceil((double) this.totalItems / this.itemsPerPage);
        this.galleryCursorView.setCount(this.pageCount);
        refreshPageViews();
    }

    /**
     * 处理返回按键事件。
     * @return 如果事件被处理返回 true，否则返回 false
     */
    public boolean handleBackPressed() {
        if (this.gridGalleryAdapter != null) {
            return this.gridGalleryAdapter.onBackPressed();
        }
        return false;
    }

    /**
     * 刷新页面视图。
     */
    public final void refreshPageViews() {
        this.viewPager.removeAllViews();
        this.pageViews.clear();
        for (int i = 0; i < this.pageCount; i++) {
            this.pageViews.add(createGridViewPage(this.viewPager, i));
        }
        this.pageAdapter.setData(this.pageViews);
    }

    public LayoutParams generateDefaultLayoutParams() {
        return super.generateDefaultLayoutParams();
    }

    public LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return super.generateLayoutParams(attributeSet);
    }

    public GridGalleryAdapter getAdapter() {
        return this.gridGalleryAdapter;
    }

    public ViewGroupOverlay getOverlay() {
        return super.getOverlay();
    }

    public void setAdapter(GridGalleryAdapter adapter) {
        this.gridGalleryAdapter = adapter;
        initializeGalleryParams();
    }

    /**
     * 设置当前显示的页面。
     * @param position 页面索引
     */
    public void setCurrentItem(int position) {
        if (this.viewPager != null) {
            this.viewPager.setCurrentItem(position);
        }
    }

    public GridGallery(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public LayoutParams generateLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return super.generateLayoutParams(layoutParams);
    }

    public GridGallery(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.pageViews = new ArrayList<>();
        this.layoutInflater = LayoutInflater.from(context);
        View.inflate(context, R.layout.grid_gallery, this);
        this.viewPager = (ViewPager) findViewById(R.id.viewPager);
        this.galleryCursorView = (GalleryCursorView) findViewById(R.id.pageCursorView);
        this.viewPager.setOffscreenPageLimit(10);
        PageAdapter pageAdapter = new PageAdapter();
        this.pageAdapter = pageAdapter;
        this.viewPager.setAdapter(pageAdapter);
        this.viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int i) {
            }

            public void onPageScrolled(int i, float f, int i2) {
                GridGallery.this.galleryCursorView.setXY(i, f);
            }

            public void onPageSelected(int i) {
            }
        });
    }
}