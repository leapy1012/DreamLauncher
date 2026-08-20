package com.android.launcher3.folder.large;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
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
    private boolean mColorOsDropAnimationRunning;

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
            getPreviewItemManager().recomputePreviewDrawingParams();
            updateListViewPadding();
            updateFolderNamePosition();
            measureChildren(widthMeasureSpec, heightMeasureSpec);
        }
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (!isLargeFolder() || getFolderName() == null) return;
        Rect previewBounds = new Rect();
        getColorOsGroupBounds(previewBounds);
        int nameTop = getHeight() - getFolderName().getMeasuredHeight();
        View name = getFolderName();
        name.layout(0, nameTop, getWidth(), nameTop + name.getMeasuredHeight());
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

    private void updateFolderNamePosition() {
        if (mActivity == null || getFolderName() == null) return;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getFolderName().getLayoutParams();
        int previewTop = Math.max(0, getPreviewOffsetY() - getPaddingTop());
        int top = isLargeFolder() ? previewTop + getPreviewHeight()
                + getResources().getDimensionPixelSize(R.dimen.coloros_large_folder_name_gap)
                : mActivity.getDeviceProfile().iconSizePx
                + mActivity.getDeviceProfile().iconDrawablePaddingPx;
        if (lp.topMargin != top) {
            lp.topMargin = top;
            getFolderName().setLayoutParams(lp);
        }
    }

    private void updateListViewPadding() {
        mListView.setFolderConfiguration(mInfo, resolveColorOsFolderStyle(),
                Math.max(0, getPreviewOffsetX() - getPaddingLeft()),
                Math.max(0, getPreviewOffsetY() - getPaddingTop()),
                getPreviewWidth(), getPreviewHeight());
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
        if (mInfo.hasOption(ColorOsFolderStyleView.STYLE_FOUR_GRID)) {
            return ColorOsFolderStyleView.STYLE_FOUR_GRID;
        }
        if (mInfo.hasOption(ColorOsFolderStyleView.STYLE_HIERARCHICAL)) {
            return ColorOsFolderStyleView.STYLE_HIERARCHICAL;
        }
        return ColorOsFolderStyleView.STYLE_NINE_GRID;
    }

    public void setColorOsFolderStyle(int style) {
        if (mListView == null || mAdapter == null) return;
        mListView.setFolderConfiguration(mInfo, style,
                Math.max(0, getPreviewOffsetX() - getPaddingLeft()),
                Math.max(0, getPreviewOffsetY() - getPaddingTop()),
                getPreviewWidth(), getPreviewHeight());
        mAdapter.setMaxSize(mListView.getPreviewSlotCount());
        refreshListView();
        requestLayout();
        invalidate();
    }

    public void switchFolderStyle(int style) {
        mInfo.setPreviewStyle(style, com.android.launcher3.Launcher.getLauncher(getContext())
                .getModelWriter());
        setColorOsFolderStyle(style);
    }

    public void getColorOsGroupBounds(Rect outBounds) {
        getPreviewItemManager().recomputePreviewDrawingParams();
        getFolderBackground().getBounds(outBounds);
        if (outBounds.isEmpty()) {
            outBounds.set(getPaddingLeft(), getPaddingTop(),
                    getWidth() - getPaddingRight(), getPreviewHeight());
        }
    }

    /** Radius used by both the folder surface and its ColorOS resize frame. */
    public float getColorOsGroupRadius() {
        ItemInfo info = getTag() instanceof ItemInfo ? (ItemInfo) getTag() : null;
        int spanX = info == null ? 1 : info.spanX;
        int spanY = info == null ? 1 : info.spanY;
        return HxyLargeFolderProxy.getFolderRound(getContext(), spanX, spanY);
    }

    /**
     * ColorOS FlexibleFolderIcon reports the complete group background as its workspace visual
     * bounds. FolderIcon's AOSP implementation reports only the small-folder preview, which makes
     * CellLayout measure drag distance from the wrong point after a folder is resized.
     */
    @Override
    public void getWorkspaceVisualDragBounds(Rect bounds) {
        getColorOsGroupBounds(bounds);
    }

    public void onFolderSpanChanged() {
        setColorOsFolderStyle(resolveColorOsFolderStyle());
        initLoadListData(isLargeFolder());
        updateFolderNamePosition();
        requestLayout();
        invalidate();
    }

    /**
     * Maps a child to the drawable actually visible in the closed flexible preview, including
     * each drawable inside the final stacked preview cell.
     */
    public boolean getColorOsPreviewItemBounds(WorkspaceItemInfo item, Rect outBounds) {
        if (mListView == null || item == null) return false;
        int dataPosition = mInfo.contents.indexOf(item);
        if (dataPosition < 0) return false;
        HxyLargeFolderIconItem previewItem = findPreviewItem(mListView, dataPosition);
        if (previewItem == null) return false;
        Rect drawableBounds = new Rect();
        if (!previewItem.getDrawableBoundsForDataPosition(dataPosition, drawableBounds)) {
            return false;
        }
        int[] itemLocation = new int[2];
        int[] iconLocation = new int[2];
        previewItem.getLocationOnScreen(itemLocation);
        getLocationOnScreen(iconLocation);
        outBounds.set(itemLocation[0] - iconLocation[0] + drawableBounds.left,
                itemLocation[1] - iconLocation[1] + drawableBounds.top,
                itemLocation[0] - iconLocation[0] + drawableBounds.right,
                itemLocation[1] - iconLocation[1] + drawableBounds.bottom);
        return !outBounds.isEmpty();
    }

    private HxyLargeFolderIconItem findPreviewItem(View view, int dataPosition) {
        if (view instanceof HxyLargeFolderIconItem) {
            HxyLargeFolderIconItem item = (HxyLargeFolderIconItem) view;
            int start = item.getBoundPosition();
            if (start == dataPosition || (item.isCountOut()
                    && dataPosition >= start && dataPosition < start + 4)) {
                return item;
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                HxyLargeFolderIconItem match = findPreviewItem(group.getChildAt(index),
                        dataPosition);
                if (match != null) return match;
            }
        }
        return null;
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
        if (mColorOsDropAnimationRunning) return;
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
        updateFolderNamePosition();
        setTextVisible(true);
        requestLayout();
        this.mSwitcher.releaseAnimationParams();
    }

    private float computeLargePreviewItemScale() {
        return (((float) HxyLargeFolderProxy.getFolderIconSize()) * 1.0f) / ((float) this.mActivity.getDeviceProfile().folderIconSizePx);
    }

    @Override
    public void computeLargePreviewItemLocation(DragView dragView, int index, Rect to) {
        if (!computeColorOsDropLocation(dragView, index, to)) {
            super.computeLargePreviewItemLocation(dragView, index, to);
        }
    }

    public boolean computeColorOsDropLocation(DragView dragView, int contentIndex, Rect to) {
        if (mListView == null || mAdapter == null || mAdapter.getMaxSize() < 1
                || dragView == null || mActivity == null) return false;
        int maxPreviewSlots = mAdapter.getMaxSize();
        int slot = Math.min(Math.max(0, contentIndex), maxPreviewSlots - 1);
        int[] coordinate = mListView.getCoordinateXY(slot);
        Rect groupBounds = new Rect();
        float workspaceScale = mActivity.getDragLayer()
                .getDescendantRectRelativeToSelf(this, groupBounds);
        int renderedSize = mListView.getRenderedChildSize(slot);
        // The final preview slot becomes OPPO's 2x2 overflow stack. Animate to the same
        // sub-drawable rendered inside that cell, rather than into the whole stack.
        if (mInfo.contents.size() > maxPreviewSlots && contentIndex >= maxPreviewSlots - 1) {
            int stackIndex = Math.min(3, contentIndex - (maxPreviewSlots - 1));
            int stackGap = HxyLargeFolderProxy.getFolderIconOutSpace(getContext());
            int stackIconSize = Math.max(1, (renderedSize - stackGap) / 2);
            coordinate[0] += (stackIndex % 2) * (stackIconSize + stackGap);
            coordinate[1] += (stackIndex / 2) * (stackIconSize + stackGap);
            renderedSize = stackIconSize;
        }
        int targetSize = Math.round(renderedSize * workspaceScale);
        int desiredLeft = groupBounds.left
                + Math.round((mListView.getLeft() + coordinate[0]) * workspaceScale);
        int desiredTop = groupBounds.top
                + Math.round((mListView.getTop() + coordinate[1]) * workspaceScale);
        int centerOffsetX = Math.round((dragView.getMeasuredWidth() - targetSize) / 2f);
        int centerOffsetY = Math.round((dragView.getMeasuredHeight() - targetSize) / 2f);
        to.set(desiredLeft - centerOffsetX, desiredTop - centerOffsetY,
                desiredLeft - centerOffsetX + targetSize,
                desiredTop - centerOffsetY + targetSize);
        return true;
    }

    public int getColorOsDropPreviewSlot(int contentIndex) {
        return mAdapter == null || mAdapter.getMaxSize() < 1 ? 0
                : Math.min(Math.max(0, contentIndex), mAdapter.getMaxSize() - 1);
    }

    public void beginColorOsDropAnimation() {
        mColorOsDropAnimationRunning = true;
    }

    public void finishColorOsDropAnimation() {
        mColorOsDropAnimationRunning = false;
        refreshListData();

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
