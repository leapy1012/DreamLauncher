package com.coui.appcompat.tablayout;

import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.coui.appcompat.animation.COUIMoveEaseInterpolator;
import com.coui.appcompat.tablayout.COUITabLayout;
import com.coui.appcompat.viewpager.COUIViewPager2;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;


public final class COUITabLayoutMediator {
    private static final int ALL_CUSTOM_TAB_VIEW = 2;
    private static final int INVALID_VALUE = -1;
    private static final int NO_CUSTOM_TAB_VIEW = 0;
    private static final int PART_CUSTOM_TAB_VIEW = 1;
    private static final String SELECT_TAB_NAME = "TabLayout.selectTab(TabLayout.Tab, boolean)";
    private static final String SET_SCROLL_POSITION_NAME = "TabLayout.setScrollPosition(int, float, boolean, boolean)";
    private static Method sSelectTab;
    private static Method sSetScrollPosition;
    private RecyclerView.Adapter mAdapter;
    private boolean mAttached;
    private final boolean mAutoRefresh;
    private int mCustomTabViewType;
    private int mLayoutResAll;
    private SparseIntArray mLayoutResIdMap;
    private final OnConfigureTabCallback mOnConfigureTabCallback;
    private TabLayoutOnPageChangeCallback mOnPageChangeCallback;
    private COUITabLayout.OnTabSelectedListener mOnTabSelectedListener;
    private RecyclerView.AdapterDataObserver mPagerAdapterObserver;
    private Map<Integer, String> mTabContentDescMap;
    private final COUITabLayout mTabLayout;
    private final COUIViewPager2 mViewPager;

    public interface OnConfigureTabCallback {
        void onConfigureTab(COUITab cOUITab, int i2);
    }

    public class PagerAdapterObserver extends RecyclerView.AdapterDataObserver {
        public PagerAdapterObserver() {
        }

        @Override
        public void onChanged() {
            COUITabLayoutMediator.this.populateTabsFromPagerAdapter();
        }

        @Override
        public void onItemRangeChanged(int i2, int i6) {
            COUITabLayoutMediator.this.populateTabsFromPagerAdapter();
        }

        @Override
        public void onItemRangeInserted(int i2, int i6) {
            COUITabLayoutMediator.this.populateTabsFromPagerAdapter();
        }

        @Override
        public void onItemRangeMoved(int i2, int i6, int i10) {
            COUITabLayoutMediator.this.populateTabsFromPagerAdapter();
        }

        @Override
        public void onItemRangeRemoved(int i2, int i6) {
            COUITabLayoutMediator.this.populateTabsFromPagerAdapter();
        }

        @Override
        public void onItemRangeChanged(int i2, int i6, Object obj) {
            COUITabLayoutMediator.this.populateTabsFromPagerAdapter();
        }
    }

    public static class TabLayoutOnPageChangeCallback extends ViewPager2.OnPageChangeCallback {
        private int mPreviousScrollState;
        private int mScrollState;
        private final WeakReference<COUITabLayout> mTabLayoutRef;
        private final WeakReference<COUIViewPager2> mViewPager2Ref;

        public TabLayoutOnPageChangeCallback(COUITabLayout cOUITabLayout, COUIViewPager2 cOUIViewPager2) {
            this.mTabLayoutRef = new WeakReference<>(cOUITabLayout);
            this.mViewPager2Ref = new WeakReference<>(cOUIViewPager2);
            reset();
        }

        @Override
        public void onPageScrollStateChanged(int i2) {
            this.mPreviousScrollState = this.mScrollState;
            this.mScrollState = i2;
        }

        @Override
        public void onPageScrolled(int i2, float f2, int i6) {
            COUIViewPager2 cOUIViewPager2 = this.mViewPager2Ref.get();
            COUITabLayout cOUITabLayout = this.mTabLayoutRef.get();
            if (cOUITabLayout == null || cOUIViewPager2 == null || cOUIViewPager2.isFakeDragging()) {
                return;
            }
            int i10 = this.mScrollState;
            boolean z6 = true;
            boolean z10 = i10 != 2 || this.mPreviousScrollState == 1;
            if (i10 == 2 && this.mPreviousScrollState == 0) {
                z6 = false;
            }
            if (i10 == 0 && this.mPreviousScrollState == 0 && f2 != 0.0f) {
                ((RecyclerView) cOUIViewPager2.getChildAt(0)).scrollBy(i6, 0);
                cOUITabLayout.selectTab(cOUITabLayout.getTabAt(i2));
            } else {
                COUITabLayoutMediator.setScrollPosition(cOUITabLayout, i2, f2, z10, z6);
            }
            if (f2 != 0.0f || i2 == cOUITabLayout.getSelectedTabPosition()) {
                return;
            }
            cOUITabLayout.selectTab(cOUITabLayout.getTabAt(i2));
        }

        @Override
        public void onPageSelected(int i2) {
            COUITabLayout cOUITabLayout = this.mTabLayoutRef.get();
            if (cOUITabLayout == null || cOUITabLayout.getSelectedTabPosition() == i2 || i2 >= cOUITabLayout.getTabCount()) {
                return;
            }
            int i6 = this.mScrollState;
            COUITabLayoutMediator.selectTab(cOUITabLayout, cOUITabLayout.getTabAt(i2), i6 == 0 || (i6 == 2 && this.mPreviousScrollState == 0));
        }

        public void reset() {
            this.mScrollState = 0;
            this.mPreviousScrollState = 0;
        }
    }

    public static class ViewPagerOnTabSelectedListener implements COUITabLayout.OnTabSelectedListener {
        private int[] mScrollDistanceAndDuration = new int[2];
        private PathInterpolator mScrollPathInterpolator = new COUIMoveEaseInterpolator();
        private final COUIViewPager2 mViewPager;

        public ViewPagerOnTabSelectedListener(COUIViewPager2 cOUIViewPager2) {
            this.mViewPager = cOUIViewPager2;
        }

        private void getScrollDistanceAndDuration(LinearLayoutManager linearLayoutManager, RecyclerView recyclerView, int i2) {
            View viewFindViewByPosition;
            int[] iArr = this.mScrollDistanceAndDuration;
            iArr[0] = 0;
            iArr[1] = 0;
            int iFindFirstVisibleItemPosition = linearLayoutManager.findFirstVisibleItemPosition();
            if (iFindFirstVisibleItemPosition == -1 || (viewFindViewByPosition = linearLayoutManager.findViewByPosition(iFindFirstVisibleItemPosition)) == null) {
                return;
            }
            int leftDecorationWidth = linearLayoutManager.getLeftDecorationWidth(viewFindViewByPosition);
            int rightDecorationWidth = linearLayoutManager.getRightDecorationWidth(viewFindViewByPosition);
            ViewGroup.LayoutParams layoutParams = viewFindViewByPosition.getLayoutParams();
            if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
                leftDecorationWidth += marginLayoutParams.leftMargin;
                rightDecorationWidth += marginLayoutParams.rightMargin;
            }
            int width = viewFindViewByPosition.getWidth() + leftDecorationWidth + rightDecorationWidth;
            int left = (viewFindViewByPosition.getLeft() - leftDecorationWidth) - recyclerView.getPaddingLeft();
            if (linearLayoutManager.getLayoutDirection() == 1) {
                width = -width;
            }
            int i6 = ((i2 - iFindFirstVisibleItemPosition) * width) + left;
            int[] iArr2 = this.mScrollDistanceAndDuration;
            iArr2[0] = i6;
            iArr2[1] = getScrollDuration(Math.abs(i6), Math.abs(width));
        }

        private int getScrollDuration(int i2, int i6) {
            float f2 = i6 * 3;
            if (i2 <= i6) {
                return 350;
            }
            float f10 = i2;
            if (f10 > f2) {
                return 650;
            }
            return (int) (((f10 / f2) * 300.0f) + 350.0f);
        }

        @Override
        public void onTabReselected(COUITab cOUITab) {
        }

        @Override
        public void onTabSelected(COUITab cOUITab) {
            RecyclerView.Adapter adapter;
            if (cOUITab.mView.getSelectedByClick() && (adapter = this.mViewPager.getAdapter()) != null && adapter.getItemCount() > 0) {
                int iMin = Math.min(Math.max(cOUITab.getPosition(), 0), adapter.getItemCount() - 1);
                if (this.mViewPager.getChildAt(0) instanceof RecyclerView) {
                    this.mViewPager.setCurrentItemWithoutAnimation(iMin);
                    RecyclerView recyclerView = (RecyclerView) this.mViewPager.getChildAt(0);
                    LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (linearLayoutManager == null) {
                        return;
                    }
                    getScrollDistanceAndDuration(linearLayoutManager, recyclerView, iMin);
                    this.mViewPager.beginFakeDrag();
                    int[] iArr = this.mScrollDistanceAndDuration;
                    recyclerView.smoothScrollBy(iArr[0], 0, this.mScrollPathInterpolator, iArr[1]);
                }
            }
        }

        @Override
        public void onTabUnselected(COUITab cOUITab) {
        }
    }

    static {
        try {
            Class cls = Boolean.TYPE;
            Method declaredMethod = COUITabLayout.class.getDeclaredMethod("setScrollPosition", Integer.TYPE, Float.TYPE, cls, cls);
            sSetScrollPosition = declaredMethod;
            declaredMethod.setAccessible(true);
            Method declaredMethod2 = COUITabLayout.class.getDeclaredMethod("selectTab", COUITab.class, cls);
            sSelectTab = declaredMethod2;
            declaredMethod2.setAccessible(true);
        } catch (NoSuchMethodException unused) {
            throw new IllegalStateException("Can't reflect into method TabLayout.setScrollPosition(int, float, boolean, boolean)");
        }
    }

    public COUITabLayoutMediator(COUITabLayout cOUITabLayout, COUIViewPager2 cOUIViewPager2, OnConfigureTabCallback onConfigureTabCallback) {
        this(cOUITabLayout, cOUIViewPager2, true, onConfigureTabCallback);
    }

    public static void selectTab(COUITabLayout cOUITabLayout, COUITab cOUITab, boolean z6) {
        try {
            Method method = sSelectTab;
            if (method != null) {
                method.invoke(cOUITabLayout, cOUITab, Boolean.valueOf(z6));
            } else {
                throwMethodNotFound(SELECT_TAB_NAME);
            }
        } catch (Exception unused) {
            throwInvokeFailed(SELECT_TAB_NAME);
        }
    }

    public static void setScrollPosition(COUITabLayout cOUITabLayout, int i2, float f2, boolean z6, boolean z10) {
        try {
            Method method = sSetScrollPosition;
            if (method != null) {
                method.invoke(cOUITabLayout, Integer.valueOf(i2), Float.valueOf(f2), Boolean.valueOf(z6), Boolean.valueOf(z10));
            } else {
                throwMethodNotFound(SET_SCROLL_POSITION_NAME);
            }
        } catch (Exception unused) {
            throwInvokeFailed(SET_SCROLL_POSITION_NAME);
        }
    }

    private static void throwInvokeFailed(String str) {
        throw new IllegalStateException("Couldn't invoke method " + str);
    }

    private static void throwMethodNotFound(String str) {
        throw new IllegalStateException("Method " + str + " not found");
    }

    public void attach() {
        if (this.mAttached) {
            throw new IllegalStateException("TabLayoutMediator is already attached");
        }
        RecyclerView.Adapter adapter = this.mViewPager.getAdapter();
        this.mAdapter = adapter;
        if (adapter == null) {
            throw new IllegalStateException("TabLayoutMediator attached before ViewPager2 has an adapter");
        }
        this.mAttached = true;
        TabLayoutOnPageChangeCallback tabLayoutOnPageChangeCallback = new TabLayoutOnPageChangeCallback(this.mTabLayout, this.mViewPager);
        this.mOnPageChangeCallback = tabLayoutOnPageChangeCallback;
        this.mViewPager.registerOnPageChangeCallback(tabLayoutOnPageChangeCallback);
        ViewPagerOnTabSelectedListener viewPagerOnTabSelectedListener = new ViewPagerOnTabSelectedListener(this.mViewPager);
        this.mOnTabSelectedListener = viewPagerOnTabSelectedListener;
        this.mTabLayout.addOnTabSelectedListener(viewPagerOnTabSelectedListener);
        if (this.mAutoRefresh) {
            PagerAdapterObserver pagerAdapterObserver = new PagerAdapterObserver();
            this.mPagerAdapterObserver = pagerAdapterObserver;
            this.mAdapter.registerAdapterDataObserver(pagerAdapterObserver);
        }
        populateTabsFromPagerAdapter();
        this.mTabLayout.setScrollPosition(this.mViewPager.getCurrentItem(), 0.0f, true);
    }

    public void detach() {
        if (this.mAutoRefresh) {
            this.mAdapter.unregisterAdapterDataObserver(this.mPagerAdapterObserver);
        }
        this.mTabLayout.removeOnTabSelectedListener(this.mOnTabSelectedListener);
        this.mViewPager.unregisterOnPageChangeCallback(this.mOnPageChangeCallback);
        this.mPagerAdapterObserver = null;
        this.mOnTabSelectedListener = null;
        this.mOnPageChangeCallback = null;
        this.mAttached = false;
    }

    public void populateTabsFromPagerAdapter() {
        this.mTabLayout.removeAllTabs();
        RecyclerView.Adapter hVar = this.mAdapter;
        if (hVar != null) {
            int itemCount = hVar.getItemCount();
            for (int i2 = 0; i2 < itemCount; i2++) {
                COUITab cOUITabNewTab = this.mTabLayout.newTab();
                int i6 = this.mCustomTabViewType;
                if (i6 != 1) {
                    if (i6 == 2) {
                        cOUITabNewTab.setCustomView(this.mLayoutResAll);
                    }
                } else if (this.mLayoutResIdMap.get(i2, -1) != -1) {
                    cOUITabNewTab.setCustomView(this.mLayoutResIdMap.get(i2));
                }
                String str = this.mTabContentDescMap.get(Integer.valueOf(i2));
                if (str != null) {
                    cOUITabNewTab.setContentDescription(str);
                }
                this.mOnConfigureTabCallback.onConfigureTab(cOUITabNewTab, i2);
                this.mTabLayout.addTab(cOUITabNewTab, false);
            }
            if (itemCount > 0) {
                int currentItem = this.mViewPager.getCurrentItem();
                COUITab tabAt = this.mTabLayout.getTabAt(currentItem);
                if (currentItem == this.mTabLayout.getSelectedTabPosition() || tabAt == null) {
                    return;
                }
                tabAt.select();
            }
        }
    }

    public void setTabCustomView(int i2) {
        this.mCustomTabViewType = 2;
        this.mLayoutResAll = i2;
        if (this.mAttached) {
            populateTabsFromPagerAdapter();
            this.mTabLayout.setScrollPosition(this.mViewPager.getCurrentItem(), 0.0f, true);
        }
    }

    public COUITabLayoutMediator(COUITabLayout cOUITabLayout, COUIViewPager2 cOUIViewPager2, boolean z6, OnConfigureTabCallback onConfigureTabCallback) {
        this.mTabLayout = cOUITabLayout;
        cOUITabLayout.setUpdateindicatorposition(true);
        this.mViewPager = cOUIViewPager2;
        this.mAutoRefresh = z6;
        this.mOnConfigureTabCallback = onConfigureTabCallback;
        this.mCustomTabViewType = 0;
        this.mLayoutResIdMap = new SparseIntArray();
        this.mTabContentDescMap = new HashMap();
    }

    public void setTabCustomView(int i2, int i6) {
        setTabCustomView(i2, i6, null);
    }

    public void setTabCustomView(int i2, int i6, String str) {
        this.mCustomTabViewType = 1;
        this.mLayoutResIdMap.put(i6, i2);
        this.mTabContentDescMap.put(Integer.valueOf(i6), str);
        if (this.mAttached) {
            populateTabsFromPagerAdapter();
            this.mTabLayout.setScrollPosition(this.mViewPager.getCurrentItem(), 0.0f, true);
        }
    }
}







