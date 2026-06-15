package com.android.launcher3.big.add;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewDebug;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.BaseActivity;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.PagedView;
import com.android.launcher3.ShortcutAndWidgetContainer;
import com.android.launcher3.Utilities;
import com.android.launcher3.Workspace;
import com.android.launcher3.celllayout.CellLayoutLayoutParams;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.folder.FolderGridOrganizer;
import com.android.launcher3.big.HxyCheckBubbleTextView;
import com.android.launcher3.keyboard.ViewGroupFocusHelper;
import com.android.launcher3.model.data.FolderInfo;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.android.launcher3.pageindicators.PageIndicatorDots;
import com.android.launcher3.util.Executors;
import com.android.launcher3.util.IntArray;
import com.android.launcher3.util.IntSet;
import com.android.launcher3.util.LauncherBindableItemsContainer;
import com.android.launcher3.util.ViewCache;
import com.android.launcher3.R;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import com.android.launcher3.BuildConfig;

public class HxyContentPagedView extends PagedView<PageIndicatorDots> implements View.OnClickListener {
    private static final boolean DEBUG = true;
    private static final int REORDER_ANIMATION_DURATION = 230;
    private static final float SCROLL_HINT_FRACTION = 0.07f;
    private static final int START_VIEW_REORDER_DELAY = 30;
    private static final String TAG = "FolderPagedView";
    private static final float VIEW_REORDER_DELAY_FACTOR = 0.9f;
    private static final int[] sTmpArray = new int[2];
    public List<WorkspaceItemInfo> mAddsToFolder = new ArrayList();
    private int mAllocatedContentSize;
    private final ViewGroupFocusHelper mFocusIndicatorHelper;
    private Folder mFolder;
    public List<WorkspaceItemInfo> mFolderLists = new ArrayList();
    private AbstractFloatingView mFullSheet;
    @ViewDebug.ExportedProperty(category = "launcher")
    private int mGridCountX;
    @ViewDebug.ExportedProperty(category = "launcher")
    private int mGridCountY;
    private final LayoutInflater mInflater;
    private boolean mIsEditingName = false;
    public final boolean mIsRtl;
    private Launcher mLauncher;
    private final FolderGridOrganizer mOrganizer;
    final ArrayMap<View, Runnable> mPendingAnimations = new ArrayMap<>();
    public List<WorkspaceItemInfo> mRemovesFromFolder = new ArrayList();
    private final ViewCache mViewCache;
    private boolean mViewsBound = false;
    private String sDefaultFolderName;

    public void setFolder(Folder f) {
        this.mFolder = f;
        this.mLauncher = Launcher.getLauncher(getContext());
    }

    public HxyContentPagedView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mOrganizer = new HxyContentGridOrganizer(LauncherAppState.getIDP(context));
        this.mIsRtl = Utilities.isRtl(getResources());
        setImportantForAccessibility(1);
        this.mFocusIndicatorHelper = new ViewGroupFocusHelper(this);
        this.mViewCache = BaseActivity.fromContext(context).getViewCache();
        this.mInflater = LayoutInflater.from(context);
    }

    public void setPageIndicator(View container) {
        this.mFullSheet = (AbstractFloatingView) container;
        this.mPageIndicator = container.findViewById(R.id.add_folder_page_indicator);
        View complete = container.findViewById(R.id.complete_text);
        complete.setOnClickListener(this);
        View cancel = container.findViewById(R.id.cancel_text);
        cancel.setOnClickListener(this);
        initParentViews(container);
    }

    public boolean isEditingName() {
        return this.mIsEditingName;
    }

    private void setupContentDimensions(int count) {
        this.mAllocatedContentSize = count;
        this.mOrganizer.setContentSize(count);
        this.mGridCountX = this.mOrganizer.getCountX();
        this.mGridCountY = this.mOrganizer.getCountY();
        for (int i = getPageCount() - 1; i >= 0; i--) {
            getPageAt(i).setGridSize(this.mGridCountX, this.mGridCountY);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        this.mFocusIndicatorHelper.draw(canvas);
        super.dispatchDraw(canvas);
    }

    public void clear() {
        this.mFolderLists.clear();
        this.mRemovesFromFolder.clear();
        this.mAddsToFolder.clear();
    }

    public void bindItems(List<WorkspaceItemInfo> items) {
        Executors.MODEL_EXECUTOR.execute(() -> bindItemsFunction(items));
    }

    private void bindItemsFunction(List<WorkspaceItemInfo> items) {
        List<WorkspaceItemInfo> list = new ArrayList<>();
        List<WorkspaceItemInfo> list2 = (List) this.mFolder.getInfo().contents.clone();
        this.mFolderLists = list2;
        list.addAll(list2);
        list.forEach(workspaceItemInfo -> workspaceItemInfo.isSelect = true);
        items.forEach(workspaceItemInfo -> workspaceItemInfo.isSelect = false);
        List<WorkspaceItemInfo> result = items.stream().filter(this::bindItemsFilter).collect(Collectors.toList());
        List<WorkspaceItemInfo> newList = new ArrayList<>();
        // 过滤一键清理快捷方式
        items.removeAll(result);
        items.forEach(item -> {
            WorkspaceItemInfo clone = item.clone();
            clone.container = -183;
            newList.add(clone);
        });
        if (this.mViewsBound) {
            unbindItems();
        }
        list.addAll(newList);
        Executors.MAIN_EXECUTOR.execute(() -> {
            List<View> temps = new ArrayList<>();
            for (WorkspaceItemInfo workspaceItemInfo : list) {
                temps.add(createNewView(workspaceItemInfo));
            }
            arrangeChildren(temps);
        });
        this.mViewsBound = true;
    }

	private boolean bindItemsFilter(WorkspaceItemInfo workspaceItemInfo) {
        return workspaceItemInfo != null && workspaceItemInfo.getTargetComponent() != null && workspaceItemInfo.getTargetComponent().getPackageName().equals(BuildConfig.APPLICATION_ID);
    }

    public void unbindItems() {
        int i = getChildCount();
        while (true) {
            i--;
            if (i >= 0) {
                CellLayout page = (CellLayout) getChildAt(i);
                ShortcutAndWidgetContainer container = page.getShortcutsAndWidgets();
                for (int j = container.getChildCount() - 1; j >= 0; j--) {
                    container.getChildAt(j).setVisibility(View.VISIBLE);
                    this.mViewCache.recycleView(R.layout.hxy_content_select_folder_application, container.getChildAt(j));
                }
                page.removeAllViews();
                this.mViewCache.recycleView(R.layout.folder_page, page);
            } else {
                removeAllViews();
                this.mViewsBound = false;
                return;
            }
        }
    }

    public boolean areViewsBound() {
        return this.mViewsBound;
    }

    public void addViewForRank(View view, WorkspaceItemInfo item, int rank) {
        CellLayoutLayoutParams lp = (CellLayoutLayoutParams) view.getLayoutParams();
        lp.setCellXY(this.mOrganizer.getPosForRank(rank));
        getPageAt(rank / this.mOrganizer.getMaxItemsPerPage()).addViewToCellLayout(view, -1, item.getViewId(), lp, true);
    }

    public View createNewView(WorkspaceItemInfo item) {
        if (item == null) {
            return null;
        }
        HxyCheckBubbleTextView textView = this.mViewCache.getView(R.layout.hxy_content_select_folder_application, getContext(), null);
        if (item.isSelect) {
            textView.isSelect = true;
        } else {
            textView.isSelect = false;
        }
        textView.applyFromWorkspaceItem(item);
        textView.setOnClickListener(this);
        textView.setTextColor(Color.parseColor("#ffffff"));
        textView.setmForceCheckHideDot(false);
        textView.updateDotScale(false, textView.isSelect);
        textView.setOnFocusChangeListener(this.mFocusIndicatorHelper);
        CellLayoutLayoutParams lp = (CellLayoutLayoutParams) textView.getLayoutParams();
        if (lp == null) {
            textView.setLayoutParams(new CellLayoutLayoutParams(item.cellX, item.cellY, item.spanX, item.spanY));
        } else {
            lp.setCellX(item.cellX);
            lp.setCellY(item.cellY);
            lp.cellVSpan = 1;
            lp.cellHSpan = 1;
        }
        return textView;
    }

    public CellLayout getPageAt(int index) {
        return (CellLayout) getChildAt(index);
    }

    public CellLayout getCurrentCellLayout() {
        return getPageAt(getNextPage());
    }

    private CellLayout createAndAddNewPage() {
        DeviceProfile grid = Launcher.getLauncher(getContext()).getDeviceProfile();
        CellLayout page = (CellLayout) this.mInflater.inflate(R.layout.folder_page, this, false);
        int i = grid.widthPx / this.mGridCountX;
        page.getShortcutsAndWidgets().setMotionEventSplittingEnabled(false);
        page.setInvertIfRtl(true);
        page.setGridSize(this.mGridCountX, this.mGridCountY);
        addView(page, -1, generateDefaultLayoutParams());
        return page;
    }

    @Override
    protected int getChildGap(int fromIndex, int toIndex) {
        return getPaddingLeft() + getPaddingRight();
    }

    public void setFixedSize(int width, int height) {
        int width2 = width - (getPaddingLeft() + getPaddingRight());
        int height2 = height - (getPaddingTop() + getPaddingBottom());
        for (int i = getChildCount() - 1; i >= 0; i--) {
            ((CellLayout) getChildAt(i)).setFixedSize(width2, height2);
        }
    }

    public void removeItem(View v) {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            getPageAt(i).removeView(v);
        }
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (this.mMaxScroll > 0) {
            ((PageIndicatorDots) this.mPageIndicator).setScroll(l, this.mMaxScroll);
        }
    }

    public void arrangeChildren(java.util.List<android.view.View> viewsToArrange) {

        // 缓存现有页面并清理子视图
        List<CellLayout> cachedPages = new ArrayList<>();
        for (int i = 0; i < getChildCount(); i++) {
            CellLayout page = (CellLayout) getChildAt(i);
            page.removeAllViews();
            cachedPages.add(page);
        }

        // 初始化布局参数
        int totalItemCount = viewsToArrange.size();
        mOrganizer.setContentSize(totalItemCount);
        setupContentDimensions(totalItemCount);
        Iterator<CellLayout> cachedPageIterator = cachedPages.iterator();

        // 核心排列逻辑
        CellLayout currentPage = null;
        int currentPageItemCount = 0;
        int maxItemsPerPage = mOrganizer.getMaxItemsPerPage();
        for (int itemIndex = 0; itemIndex < totalItemCount; itemIndex++) {
            View view = viewsToArrange.get(itemIndex);

            // 检查是否需要创建新页面
            if (currentPage == null || currentPageItemCount >= maxItemsPerPage) {
                if (cachedPageIterator.hasNext()) {
                    currentPage = cachedPageIterator.next();
                } else {
                    currentPage = createAndAddNewPage();
                }
                currentPageItemCount = 0;
            }

            if (view != null) {
                // 设置视图布局参数
                CellLayoutLayoutParams lp = (CellLayoutLayoutParams) view.getLayoutParams();
                ItemInfo itemInfo = (ItemInfo) view.getTag();
                Point cellPos = mOrganizer.getPosForRank(itemIndex);
                lp.setCellXY(cellPos);

                // 添加视图到页面
                currentPage.addViewToCellLayout(
                        view,
                        -1,
                        itemInfo.getViewId(),
                        lp,
                        true
                );

                // 特殊处理BubbleTextView的高分辨率验证
                if (mOrganizer.isItemInPreview(itemIndex) && view instanceof BubbleTextView) {
                    Intent intent = itemInfo.getIntent();
                    if (intent != null) {
                        ((BubbleTextView) view).verifyHighRes();
                    }
                }
            }

            currentPageItemCount++;
        }

        // 移除未使用的缓存页面
        boolean hasRemovedPages = false;
        while (cachedPageIterator.hasNext()) {
            View pageToRemove = cachedPageIterator.next();
            removeView(pageToRemove);
            hasRemovedPages = true;
        }

        // 页面状态收尾
        setCurrentPage(0);
        setEnableOverscroll(true);

        // 更新页面指示器可见性
        PageIndicatorDots indicator = (PageIndicatorDots) mPageIndicator;
        indicator.setVisibility(getPageCount() > 1 ? View.VISIBLE : View.GONE);
    }

    public int getDesiredWidth() {
        if (getPageCount() <= 0) {
            return 0;
        }
        return getPaddingRight() + getPageAt(0).getDesiredWidth() + getPaddingLeft();
    }

    public int getDesiredHeight() {
        if (getPageCount() <= 0) {
            return 0;
        }
        return getPaddingBottom() + getPageAt(0).getDesiredHeight() + getPaddingTop();
    }

    public View getFirstItem() {
        return getViewInCurrentPage(c -> 0);
    }

    public View getLastItem() {
        return getViewInCurrentPage(c -> c.getChildCount() - 1);
    }

    private View getViewInCurrentPage(ToIntFunction<ShortcutAndWidgetContainer> rankProvider) {
        if (getChildCount() < 1) {
            return null;
        }
        ShortcutAndWidgetContainer container = getCurrentCellLayout().getShortcutsAndWidgets();
        int rank = rankProvider.applyAsInt(container);
        int i = this.mGridCountX;
        if (i > 0) {
            return container.getChildAt(rank % i, rank / i);
        }
        return container.getChildAt(rank);
    }

    public View iterateOverItems(LauncherBindableItemsContainer.ItemOperator op) {
        for (int k = 0; k < getChildCount(); k++) {
            CellLayout page = getPageAt(k);
            for (int j = 0; j < page.getCountY(); j++) {
                for (int i = 0; i < page.getCountX(); i++) {
                    View v = page.getChildAt(i, j);
                    if (v != null && op.evaluate((ItemInfo) v.getTag(), v)) {
                        return v;
                    }
                }
            }
        }
        return null;
    }

    public String getAccessibilityDescription() {
        return getContext().getString(R.string.folder_opened, new Object[]{Integer.valueOf(this.mGridCountX), Integer.valueOf(this.mGridCountY)});
    }

    public void setFocusOnFirstChild() {
        View firstChild;
        if (getCurrentCellLayout() != null && (firstChild = getCurrentCellLayout().getChildAt(0, 0)) != null) {
            firstChild.requestFocus();
        }
    }

    public void notifyPageSwitchListener(int prevPage) {
        super.notifyPageSwitchListener(prevPage);
    }

    public void showScrollHint(int direction) {
        int delta = (getScrollForPage(getNextPage()) + ((int) (((float) getWidth()) * ((direction == 0) ^ this.mIsRtl ? -0.07f : 0.07f)))) - getScrollX();
        if (delta != 0) {
            this.mScroller.startScroll(getScrollX(), delta, 0, 500);
            invalidate();
        }
    }

    public void clearScrollHint() {
        if (getScrollX() != getScrollForPage(getNextPage())) {
            snapToPage(getNextPage());
        }
    }

    public void completePendingPageChanges() {
        if (!this.mPendingAnimations.isEmpty()) {
            for (Map.Entry<View, Runnable> e : new ArrayMap<>(this.mPendingAnimations).entrySet()) {
                e.getKey().animate().cancel();
                e.getValue().run();
            }
        }
    }

    public boolean rankOnCurrentPage(int rank) {
        return rank / this.mOrganizer.getMaxItemsPerPage() == getNextPage();
    }

    public void onPageBeginTransition() {
        super.onPageBeginTransition();
        verifyVisibleHighResIcons(getCurrentPage() - 1);
        verifyVisibleHighResIcons(getCurrentPage() + 1);
    }

    public void verifyVisibleHighResIcons(int pageNo) {
        CellLayout page = getPageAt(pageNo);
        if (page != null) {
            ShortcutAndWidgetContainer parent = page.getShortcutsAndWidgets();
            for (int i = parent.getChildCount() - 1; i >= 0; i--) {
                BubbleTextView icon = (BubbleTextView) parent.getChildAt(i);
                icon.verifyHighRes();
                Drawable d = icon.getCompoundDrawables()[1];
                if (d != null) {
                    d.setCallback(icon);
                }
            }
        }
    }

    public int getAllocatedContentSize() {
        return this.mAllocatedContentSize;
    }

    @SuppressLint("WrongConstant")
    public boolean canScroll(float absVScroll, float absHScroll) {
        return AbstractFloatingView.getTopOpenViewWithType(Launcher.getLauncher(getContext()), 4194302) == null;
    }

    public int itemsPerPage() {
        return this.mOrganizer.getMaxItemsPerPage();
    }

    public void pageEndTransition() {
        super.pageEndTransition();
        Launcher launcher = Launcher.getLauncher(getContext());
    }

    public int findSpaceOnWorkspace(ItemInfo info, int[] outCoordinates) {
        Workspace workspace = this.mLauncher.getWorkspace();
        IntArray workspaceScreens = workspace.getScreenOrder();
        int screenIndex = workspace.getCurrentPage();
        int screenId = workspaceScreens.get(screenIndex);
        boolean found = ((CellLayout) workspace.getPageAt(screenIndex)).findCellForSpan(outCoordinates, info.spanX, info.spanY);
        int screenIndex2 = 0;
        while (!found && screenIndex2 < workspaceScreens.size()) {
            screenId = workspaceScreens.get(screenIndex2);
            found = ((CellLayout) workspace.getPageAt(screenIndex2)).findCellForSpan(outCoordinates, info.spanX, info.spanY);
            screenIndex2++;
        }
        if (found) {
            return screenId;
        }
        workspace.addExtraEmptyScreens();
        IntSet emptyScreenIds = workspace.commitExtraEmptyScreens();
        if (emptyScreenIds.isEmpty()) {
            return -1;
        }
        int screenId2 = emptyScreenIds.getArray().get(0);
        if (!workspace.getScreenWithId(screenId2).findCellForSpan(outCoordinates, info.spanX, info.spanY)) {
            Log.wtf(TAG, "Not enough space on an empty screen");
        }
        return screenId2;
    }

    public WorkspaceItemInfo getOriginItemInfo(int id) {
        return (WorkspaceItemInfo) LauncherAppState.getInstance(getContext()).getModel().mBgDataModel.itemsIdMap.get(id);
    }

    public void onClick(View v) {
        if (v.getId() == R.id.complete_text) {
            Workspace workspace = Launcher.getLauncher(getContext()).getWorkspace();
            this.mAddsToFolder.forEach(item -> executeClick(workspace, item));
            Iterator<WorkspaceItemInfo> it = this.mRemovesFromFolder.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                WorkspaceItemInfo item = it.next();
                item.isSelect = false;
                int[] coordinates = new int[2];
                int screenId = findSpaceOnWorkspace(item, coordinates);
                item.cellX = coordinates[0];
                item.cellY = coordinates[1];
                item.screenId = screenId;
                this.mFolder.getInfo().remove(item, false);
                View newIcon = this.mLauncher.createShortcut(workspace.getScreenWithId(screenId), item);
                this.mLauncher.getModelWriter().addOrMoveItemInDatabase(item, -100, item.screenId, item.cellX, item.cellY);
                if (newIcon != null) {
                    this.mLauncher.getWorkspace().addInScreenFromBind(newIcon, item);
                    newIcon.requestFocus();
                }
                if (this.mFolder.getInfo().container != -101 && this.mFolder.getInfo().contents.size() == 1) {
                    this.mFolder.getInfo().contents.get(0).isSelect = false;
                    break;
                }
            }
            FolderInfo info = this.mFolder.getInfo();
            if (info.contents.isEmpty()) {
                int i = info.container;
            }
            this.mAddsToFolder.clear();
            this.mRemovesFromFolder.clear();
            this.mFullSheet.close(true);
            return;
        } else if (v.getId() == R.id.cancel_text) {
            this.mFullSheet.close(true);
            return;
        }
        HxyCheckBubbleTextView checkView = (HxyCheckBubbleTextView) v;
        checkView.setmForceCheckHideDot(false);
        checkView.isSelect = !checkView.isSelect;
        checkView.updateDotScale(true, checkView.isSelect);
        WorkspaceItemInfo info2 = (WorkspaceItemInfo) v.getTag();
        if (this.mFolderLists.contains(info2)) {
            if (!checkView.isSelect) {
                if (!this.mRemovesFromFolder.contains(info2)) {
                    this.mRemovesFromFolder.add(info2);
                }
            } else if (this.mRemovesFromFolder.contains(info2)) {
                this.mRemovesFromFolder.remove(info2);
            }
        } else if (checkView.isSelect) {
            if (!this.mAddsToFolder.contains(info2)) {
                this.mAddsToFolder.add(info2);
            }
        } else if (this.mAddsToFolder.contains(info2)) {
            this.mAddsToFolder.remove(info2);
        }
    }

    public void executeClick(Workspace workspace, WorkspaceItemInfo item) {
        item.isSelect = false;
        View view = Launcher.getLauncher(getContext()).getWorkspace().getHomescreenIconByItemId(item.id);
        if (workspace.getParentCellLayoutForView(view) != null) {
            workspace.getParentCellLayoutForView(view).removeView(view);
            int i = ((ItemInfo) view.getTag()).container;
            this.mLauncher.getWorkspace().removeExtraEmptyScreen(true);
        }
        this.mFolder.getInfo().add(getOriginItemInfo(item.id), true);
    }
}
