package com.android.launcher3.folder.large;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.android.launcher3.CellLayout;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.dragndrop.DragView;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.folder.PreviewItemManager;
import com.android.launcher3.folder.large.listview.BasePageLinearAdapter;
import com.android.launcher3.folder.large.listview.HxyLargeFolderAdapter;
import com.android.launcher3.folder.large.listview.HxyLargeFolderIconItem;
import com.android.launcher3.folder.large.listview.HxyLargeFolderListView;
import com.android.launcher3.folder.large.switchparams.ISwitchFolderAnimation;
import com.android.launcher3.folder.large.switchparams.HxyLargeFolderSwitcher;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.android.launcher3.touch.ItemClickHandler;
import com.android.launcher3.touch.ItemLongClickListener;
import com.android.launcher3.R;
import com.android.launcher3.big.popup.ColorOsFolderStyleView;

import java.util.function.Predicate;

public class HxyLargeFolderIcon extends FolderIcon implements ISwitchFolderAnimation {
    public static final int LARGE_FOLDER_SPAN_X = 2;
    public static final int LARGE_FOLDER_SPAN_Y = 2;
    private static final int RECURSION_LOAD_COUNT = 3;
    private HxyLargeFolderAdapter mAdapter;
    private HxyLargeFolderListView mListView;
    private HxyLargeFolderSwitcher mSwitcher;

    public HxyLargeFolderIcon(Context context) {
        this(context, (AttributeSet) null);
    }

    public HxyLargeFolderIcon(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mListView = null;
        this.mAdapter = null;
        this.mSwitcher = null;
        initData();
    }

    public void release() {
        if (mSwitcher != null) {
            mSwitcher.release();
            mSwitcher = null;
        }
        if (mAdapter != null) {
            mAdapter.release();
            mAdapter = null;
        }
        this.mListView = null;
    }

    private void initData() {
        mSwitcher = new HxyLargeFolderSwitcher();
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (isLargeFolder()) {
            updateListViewPadding();
            measureChildren(widthMeasureSpec, heightMeasureSpec);
        }
        setFolderNameTop();
        refreshListData();
    }

    @Override
    public int measurePaddingTop(int height, int cellHeightPx) {
        if (isLargeFolder() && HxyLargeFolderProxy.isLargeFolder((ItemInfo) getTag())) {
            height /= 2;
        }
        return super.measurePaddingTop(height, cellHeightPx);
    }

    private boolean isLargeFolder() {
        return HxyLargeFolderProxy.isLargeFolder((View) this);
    }

    private void setFolderNameTop() {
        if (this.mActivity != null && getMeasuredHeight() >= 1) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getFolderName().getLayoutParams();
            if (isLargeFolder() && lp.topMargin != getLargeFolderNameTop()) {
                lp.topMargin = getLargeFolderNameTop();
                getFolderName().setLayoutParams(lp);
            } else if (!isLargeFolder() && lp.topMargin != getFolderNameTop()) {
                lp.topMargin = getFolderNameTop();
                getFolderName().setLayoutParams(lp);
            }
        }
    }

    private int getLargeFolderNameTop() {
        return HxyLargeFolderProxy.getFolderPreviewHeight()
                + getResources().getDimensionPixelSize(R.dimen.coloros_large_folder_name_gap);
    }

    private void updateListViewPadding() {
        int paddingLeft = Math.max(0, getPreviewOffsetX()
                + HxyLargeFolderProxy.getHorizontalSpace() - getPaddingLeft());
        int paddingTop = Math.max(0, getPreviewOffsetY()
                + HxyLargeFolderProxy.getVerticalSpace() - getPaddingTop());
        if (paddingLeft != this.mListView.getPaddingLeft() || paddingTop != this.mListView.getPaddingTop()) {
            this.mListView.setVerticalSpace(HxyLargeFolderProxy.getVerticalSpace());
            this.mListView.setHorizontalSpace(HxyLargeFolderProxy.getHorizontalSpace());
            this.mListView.setPadding(paddingLeft, paddingTop, paddingLeft, paddingTop);
        }
    }

    public int getPreviewWidth() {
        return getFolderBackground().getPreviewWidth();
    }

    public int getPreviewHeight() {
        return getFolderBackground().getPreviewHeight();
    }

    private int getPreviewOffsetY() {
        if (this.mActivity == null) {
            return 0;
        }
        return HxyLargeFolderProxy.getPreviewOffsetY(getPaddingTop(), this.mActivity.getDeviceProfile().folderIconOffsetYPx);
    }

    private int getPreviewOffsetX() {
        if (this.mActivity == null) {
            return 0;
        }
        return HxyLargeFolderProxy.getPreviewOffsetX(getMeasuredWidth(), HxyLargeFolderProxy.computePreviewWidth((View) this, getMeasuredWidth(), this.mActivity.getDeviceProfile().folderIconSizePx));
    }

    private int getFolderNameTop() {
        DeviceProfile grid = this.mActivity.getDeviceProfile();
        if (!isInHotseat() || grid.isMultiWindowMode) {
            return grid.iconSizePx + grid.iconDrawablePaddingPx;
        }
        return grid.iconSizePx + (grid.iconDrawablePaddingPx / 3);
    }

    @Override
    public void initLargeFolderIcon() {
        this.mListView = (HxyLargeFolderListView) findViewById(R.id.folder_icon_content);
        this.mAdapter = new HxyLargeFolderAdapter(getContext());
        this.mListView.setAdapter(this.mAdapter);
        setColorOsFolderStyle(resolveColorOsFolderStyle());
        this.mAdapter.setItemListener(new BasePageLinearAdapter.ItemClickListener<WorkspaceItemInfo>() {
            @Override
            public void onItemClick(View view, WorkspaceItemInfo workspaceItemInfo) {
                onFolderItemClick(view, workspaceItemInfo);
            }
        });
        this.mAdapter.setItemLongListener(new BasePageLinearAdapter.ItemLongClickListener<WorkspaceItemInfo>() {
            @Override
            public void onItemLongClick(View view, WorkspaceItemInfo workspaceItemInfo) {
                executeLargeFolderIconLongClick(view, workspaceItemInfo);
            }
        });
        // 引起Fatal signal 11 (SIGSEGV), code 1 (SEGV_MAPERR)
        // this.mListView.setOnLongClickListener(new HxyLargeFolderIconLongClickListener(this));
        // this.mListView.setOnClickListener(new HxyLargeFolderIconClickListener(this));
        initLoadListData();
    }

    private int resolveColorOsFolderStyle() {
        if ((mInfo.options & ColorOsFolderStyleView.STYLE_FOUR_GRID) != 0) {
            return ColorOsFolderStyleView.STYLE_FOUR_GRID;
        }
        if ((mInfo.options & ColorOsFolderStyleView.STYLE_HIERARCHICAL) != 0) {
            return ColorOsFolderStyleView.STYLE_HIERARCHICAL;
        }
        return ColorOsFolderStyleView.STYLE_NINE_GRID;
    }

    public void setColorOsFolderStyle(int style) {
        if (mListView == null || mAdapter == null) return;
        mListView.setFolderStyle(style);
        mAdapter.setMaxSize(mListView.getPreviewSlotCount());
        refreshListView();
        requestLayout();
        invalidate();
    }

    private void executeLargeFolderIconLongClick(View view, WorkspaceItemInfo data) {
        onFolderLongClick();
    }

    private boolean executeInitLargeFolderIcon(View v) {
        onFolderLongClick();
        return true;
    }

    private void executeInitLargeFolderIconOnCLick(View v) {
        ItemClickHandler.onClick(this);
    }

    @Override
    public void onItemsChanged(boolean animate) {
        super.onItemsChanged(animate);
        initLoadListData();
    }

    public void updatePreviewItems(Predicate<ItemInfo> itemCheck) {
        super.updatePreviewItems(itemCheck);
        if (isLargeFolder()) {
            refreshListView();
        }
    }

    public void initLoadListData() {
        initLoadListData(isLargeFolder());
    }

    private void initLoadListData(boolean isLargeFolder) {
        setListViewVisible(isLargeFolder);
        recursionLoadBitmapInfo(0);
    }

    private void recursionLoadBitmapInfo(int index) {
        if (index <= 3) {
            boolean result = loadValidBitmapInfo();
            refreshListView();
            if (!result) {
                postDelayed(new HxyLargeFolderIconRunnable(this, index), 800);
            }
        }
    }

    private void executeRecursionLoadBitmapInfo(int index) {
        recursionLoadBitmapInfo(index + 1);
    }

    public void refreshListData() {
        loadValidBitmapInfo();
        refreshListView();
    }

    private void refreshListView() {
        if (isLargeFolder()) {
            this.mAdapter.setList(this.mInfo.contents);
            return;
        }
        updatePreviewItems(false);
        invalidate();
    }

    private void onFolderLongClick() {
        ItemLongClickListener.onWorkspaceItemLongClick(this);
    }

    private void onFolderItemClick(View view, WorkspaceItemInfo data) {
        HxyLargeFolderIconItem itemView = (HxyLargeFolderIconItem) view;
        if (itemView.isCountOut()) {
            ItemClickHandler.onClick(this);
        } else {
            ItemClickHandler.onClick(itemView);
        }
    }

    private boolean isSupportSwitchFolderSize() {
        return !isHotseatLayout();
    }

    private boolean isHotseatLayout() {
        return HxyLargeFolderSwitcher.isHotseatLayout(this.mActivity, HxyLargeFolderSwitcher.getCellLayout(this));
    }

    public void switchFolderSize() {
        boolean isLargeFolder = isLargeFolder();
        int spanY = 1;
        int spanX = isLargeFolder ? 1 : 2;
        if (!isLargeFolder) {
            spanY = 2;
        }
        this.mSwitcher.switchLargeFolder(this.mActivity, this, spanX, spanY);
    }

    private boolean loadValidBitmapInfo() {
        return HxyLargeFolderUtils.loadValidBitmapInfo(getContext(), this.mInfo.contents);
    }

    public View getFirstMatchForAppClose(String packageName, int userId) {
        int count = this.mListView.getChildCount();
        if (count > HxyLargeFolderProxy.getMaxSize()) {
            count--;
        }
        for (int i = 0; i < count; i++) {
            HxyLargeFolderIconItem child = (HxyLargeFolderIconItem) this.mListView.getChildAt(i);
            if (HxyLargeFolderUtils.equals((ItemInfo) (WorkspaceItemInfo) child.getTag(), packageName, userId)) {
                return child;
            }
        }
        return null;
    }

    private void setListViewVisible(boolean visible) {
        if (visible && this.mListView.getVisibility() != View.VISIBLE) {
            this.mListView.setVisibility(View.VISIBLE);
        } else if (!visible && this.mListView.getVisibility() == View.VISIBLE) {
            this.mListView.setVisibility(View.GONE);
        }
    }

    @Override
    public void dispatchDraw(Canvas canvas) {
        if (isLargeFolder()) {
            dispatchLargeDraw(canvas);
        } else {
            super.dispatchDraw(canvas);
        }
    }

    private void dispatchLargeDraw(Canvas canvas) {
        if (getIconVisible()) {
            drawPreviewBackground(canvas);
            superDispatchDraw(canvas);
            dispatchDrawEnd(canvas);
        }
    }

    @Override
    public void drawPreviewItems(Canvas canvas, PreviewItemManager manager) {
        if (!isLargeFolder()) {
            super.drawPreviewItems(canvas, manager);
        }
    }

    public void onSwitchFolderBegin() {
        setTextVisible(false);
    }

    public void onSwitchFolderEnd() {
        initLoadListData(isLargeFolder());
        CellLayout layout = HxyLargeFolderSwitcher.getCellLayout(this);
        if (layout != null) {
            layout.markCellsAsOccupiedForView(this);
        }
        setFolderNameTop();
        setTextVisible(true);
        requestLayout();
        this.mSwitcher.releaseAnimationParams();
    }

    private float computeLargePreviewItemScale() {
        return (((float) HxyLargeFolderProxy.getFolderIconSize()) * 1.0f) / ((float) this.mActivity.getDeviceProfile().folderIconSizePx);
    }

    @Override
    public void computeLargePreviewItemLocation(DragView dragView, int index, Rect to) {
        int[] coordinate;
        super.computeLargePreviewItemLocation(dragView, index, to);
        HxyLargeFolderListView hxyLargeFolderListView = this.mListView;
        if (hxyLargeFolderListView != null && hxyLargeFolderListView.getChildCount() >= 1) {
            if (index >= this.mAdapter.getMaxSize() || index >= this.mListView.getChildCount()) {
                coordinate = this.mListView.getCoordinateXY(this.mAdapter.getMaxSize() - 1);
            } else {
                coordinate = this.mListView.getCoordinateXY(index);
            }
            coordinate[1] = coordinate[1] + getPaddingTop();
            Rect pos = new Rect();
            this.mActivity.getDragLayer().getDescendantRectRelativeToSelf(this, pos);
            float offsetScale = (1.0f - computeLargePreviewItemScale()) / 2.0f;
            int offsetX = Math.round(((float) dragView.getMeasuredWidth()) * offsetScale);
            int offsetY = Math.round(((float) dragView.getMeasuredHeight()) * offsetScale);
            to.left = (pos.left + coordinate[0]) - offsetX;
            to.top = (pos.top + coordinate[1]) - offsetY;
            to.right = to.left + this.mListView.getChildSize();
            to.bottom = to.top + this.mListView.getChildSize();
        }
    }

    @Override
    public void hideLargePreviewItem(int index) {
        super.hideLargePreviewItem(index);
        setPreviewItem(index, false);
    }

    @Override
    public void showLargePreviewItem(int index) {
        super.showLargePreviewItem(index);
        setPreviewItem(index, true);
    }

    private void setPreviewItem(int index, boolean visible) {
        if (mListView != null && mListView.getChildCount() >= 1 && index < mAdapter.getMaxSize() && index < mListView.getChildCount()) {
            ((HxyLargeFolderIconItem) mListView.getChildAt(index)).setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private static class HxyLargeFolderIconLongClickListener implements View.OnLongClickListener {
        public HxyLargeFolderIcon hxyLargeFolderIcon;

        public HxyLargeFolderIconLongClickListener(HxyLargeFolderIcon hxyLargeFolderIcon) {
            this.hxyLargeFolderIcon = hxyLargeFolderIcon;
        }

        public boolean onLongClick(View view) {
            return this.hxyLargeFolderIcon.executeInitLargeFolderIcon(view);
        }
    }

    private record HxyLargeFolderIconClickListener(HxyLargeFolderIcon hxyLargeFolderIcon) implements OnClickListener {

        public void onClick(View view) {
                this.hxyLargeFolderIcon.executeInitLargeFolderIconOnCLick(view);
            }
        }

    private static final class HxyLargeFolderIconRunnable implements Runnable {
        public final HxyLargeFolderIcon hxyLargeFolderIcon;
        public final int index;

        public HxyLargeFolderIconRunnable(HxyLargeFolderIcon hxyLargeFolderIcon, int i) {
            this.hxyLargeFolderIcon = hxyLargeFolderIcon;
            this.index = i;
        }

        public  void run() {
            this.hxyLargeFolderIcon.executeRecursionLoadBitmapInfo(this.index);
        }
    }
}
