package com.android.launcher3.folder.large;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;

import com.android.launcher3.CellLayout;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.DropTarget;
import com.android.launcher3.Launcher;
import com.android.launcher3.dragndrop.DragController;
import com.android.launcher3.dragndrop.DragOptions;
import com.android.launcher3.dragndrop.DragView;
import com.android.launcher3.dot.FolderDotInfo;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.folder.PreviewItemManager;
import com.android.launcher3.folder.large.listview.BasePageLinearAdapter;
import com.android.launcher3.folder.large.listview.HxyLargeFolderAdapter;
import com.android.launcher3.folder.large.listview.HxyLargeFolderIconItem;
import com.android.launcher3.folder.large.listview.HxyLargeFolderListView;
import com.android.launcher3.folder.large.listview.HxyLargeFolderPageIndicator;
import com.android.launcher3.folder.large.switchparams.ISwitchFolderAnimation;
import com.android.launcher3.folder.large.switchparams.HxyLargeFolderSwitcher;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.android.launcher3.touch.ItemClickHandler;
import com.android.launcher3.touch.ItemLongClickListener;
import com.android.launcher3.R;
import com.android.launcher3.big.popup.ColorOsFolderStyleView;

import java.util.function.Predicate;

public class HxyLargeFolderIcon extends FolderIcon implements ISwitchFolderAnimation,
        DragController.DragListener {
    public static final int LARGE_FOLDER_SPAN_X = 2;
    public static final int LARGE_FOLDER_SPAN_Y = 2;
    private static final int RECURSION_LOAD_COUNT = 3;
    private HxyLargeFolderAdapter mAdapter;
    private HxyLargeFolderListView mListView;
    private HxyLargeFolderAdapter mNextAdapter;
    private HxyLargeFolderListView mNextListView;
    private HxyLargeFolderPageIndicator mPageIndicator;
    private HxyLargeFolderSwitcher mSwitcher;
    private boolean mColorOsDropAnimationRunning;
    private int mCurrentPreviewPage;
    private int mTargetPreviewPage = -1;
    private float mPageDownX;
    private float mPageDownY;
    private boolean mPageDragging;
    private int mDragLowerPage;
    private int mDragUpperPage;
    private int mOverScrollPage = -1;
    private float mOverScrollInput;
    private float mOverScrollAmount;
    private ValueAnimator mPageAnimator;
    private int mOriginalPreviewPage;
    private int mProgrammaticSnapTarget = -1;
    private boolean mDropAcceptedDuringDrag;
    private boolean mDragListenerRegistered;
    private final PathInterpolator mPageIndicatorInterpolator =
            new PathInterpolator(0.33f, 0.0f, 0.67f, 1.0f);
    private final PathInterpolator mProgrammaticSnapInterpolator =
            new PathInterpolator(0.3f, 0.0f, 0.1f, 1.0f);
    private final Runnable mHidePageIndicator = this::hidePageIndicator;

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
        if (mDragListenerRegistered && mActivity instanceof Launcher) {
            ((Launcher) mActivity).getDragController().removeDragListener(this);
            mDragListenerRegistered = false;
        }
        if (mSwitcher != null) {
            mSwitcher.release();
            mSwitcher = null;
        }
        if (mAdapter != null) {
            mAdapter.release();
            mAdapter = null;
        }
        if (mNextAdapter != null) {
            mNextAdapter.release();
            mNextAdapter = null;
        }
        if (mPageAnimator != null) {
            mPageAnimator.cancel();
            mPageAnimator = null;
        }
        removeCallbacks(mHidePageIndicator);
        this.mListView = null;
        this.mNextListView = null;
        this.mPageIndicator = null;
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
        configurePage(mListView, mAdapter, mCurrentPreviewPage);
        if (mNextListView != null && mNextAdapter != null && mTargetPreviewPage >= 0) {
            configurePage(mNextListView, mNextAdapter, mTargetPreviewPage);
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

    @Override
    public void initLargeFolderIcon() {
        this.mListView = (HxyLargeFolderListView) findViewById(R.id.folder_icon_content);
        this.mNextListView = findViewById(R.id.folder_icon_content_next);
        this.mPageIndicator = findViewById(R.id.folder_icon_indicator);
        this.mAdapter = new HxyLargeFolderAdapter(getContext());
        this.mNextAdapter = new HxyLargeFolderAdapter(getContext());
        this.mListView.setAdapter(this.mAdapter);
        this.mNextListView.setAdapter(this.mNextAdapter);
        if (!mDragListenerRegistered && mActivity instanceof Launcher) {
            ((Launcher) mActivity).getDragController().addDragListener(this);
            mDragListenerRegistered = true;
        }
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
        this.mNextAdapter.setItemListener((view, item) -> onFolderItemClick(view, item));
        this.mNextAdapter.setItemLongListener(
                (view, item) -> executeLargeFolderIconLongClick(view, item));
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
        mCurrentPreviewPage = Math.min(mCurrentPreviewPage,
                Math.max(0, getPreviewPageCount() - 1));
        configurePage(mListView, mAdapter, mCurrentPreviewPage);
        if (mPageIndicator != null) {
            mPageIndicator.setPageCount(getPreviewPageCount());
            mPageIndicator.setCurrentPage(mCurrentPreviewPage);
        }
        refreshListView();
        requestLayout();
        invalidate();
    }

    public void switchFolderStyle(int style) {
        setBigFolderType(style);
        setColorOsFolderStyle(style);
    }

    /** Mirrors ColorOS FolderManager#setBigFolderType: keep exactly one persisted type bit. */
    private void setBigFolderType(int style) {
        mInfo.setPreviewStyle(style, com.android.launcher3.Launcher.getLauncher(getContext())
                .getModelWriter());
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
            mCurrentPreviewPage = Math.min(mCurrentPreviewPage,
                    Math.max(0, getPreviewPageCount() - 1));
            configurePage(mListView, mAdapter, mCurrentPreviewPage);
            this.mAdapter.setList(this.mInfo.contents);
            if (mPageIndicator != null) {
                mPageIndicator.setPageCount(getPreviewPageCount());
                mPageIndicator.setCurrentPage(mCurrentPreviewPage);
            }
            return;
        }
        updatePreviewItems(false);
        invalidate();
    }

    @Override
    public void setDotInfo(FolderDotInfo dotInfo) {
        super.setDotInfo(dotInfo);
        // FlexibleFolderIcon reapplies preview drawing params whenever notification state
        // changes; this also recomputes the aggregate for its overflow stack.
        if (isLargeFolder() && mAdapter != null) {
            mAdapter.refresh();
        }
        if (isLargeFolder() && mNextAdapter != null
                && mNextListView != null && mNextListView.getVisibility() == View.VISIBLE) {
            mNextAdapter.refresh();
        }
    }

    private void configurePage(HxyLargeFolderListView listView,
            HxyLargeFolderAdapter adapter, int page) {
        if (listView == null || adapter == null || mInfo == null) return;
        int style = resolveColorOsFolderStyle();
        // ColorOS applies the highlight deletion only to page zero. Later pages use the
        // ordinary nine-grid geometry (BigFolderGridOrganizer#previewItemsForPage).
        if (style == ColorOsFolderStyleView.STYLE_HIERARCHICAL && page > 0) {
            style = ColorOsFolderStyleView.STYLE_NINE_GRID;
        }
        listView.setFolderConfiguration(mInfo, style,
                Math.max(0, getPreviewOffsetX() - getPaddingLeft()),
                Math.max(0, getPreviewOffsetY() - getPaddingTop()),
                getPreviewWidth(), getPreviewHeight());
        adapter.setMaxSize(listView.getPreviewSlotCount());
        adapter.setPageOffset(getPreviewPageOffset(page));
    }

    private int getPreviewPageStride() {
        return Math.max(1, (mInfo.getPreviewColumn() * mInfo.getPreviewRow()) - 1);
    }

    /** Mirrors ColorOS FolderOpenAnimHelper.Companion#getExpandTargetPage. */
    @Override
    public int getOpenFolderPage(int maxItemsPerPage) {
        if (!isLargeFolder() || mCurrentPreviewPage == 0 || maxItemsPerPage <= 0) {
            return 0;
        }
        return ((getPreviewPageStride() * mCurrentPreviewPage) + 1) / maxItemsPerPage;
    }

    /** Mirrors ColorOS BigFolderGridOrganizer#getPreviewPageForClose. */
    private int getPreviewPageForClose(int closePage) {
        if (closePage == 0 || mInfo == null || mInfo.contents.isEmpty()) {
            return 0;
        }
        int stride = getPreviewPageStride();
        return Math.min((closePage * stride) + 1, mInfo.contents.size()) / stride;
    }

    @Override
    public void onFolderClose(int currentPage) {
        super.onFolderClose(currentPage);
        if (!isLargeFolder()) return;
        mCurrentPreviewPage = Math.min(getPreviewPageForClose(currentPage),
                Math.max(0, getPreviewPageCount() - 1));
        configurePage(mListView, mAdapter, mCurrentPreviewPage);
        if (mPageIndicator != null) {
            mPageIndicator.setCurrentPage(mCurrentPreviewPage);
        }
        refreshListView();
    }

    private int getPreviewPageOffset(int page) {
        if (page <= 0) return 0;
        int offset = page * getPreviewPageStride();
        return mInfo.isCurrentDisplayHighlightGrid() ? Math.max(0, offset - 3) : offset;
    }

    /** Exact ColorOS FlexibleFolderIcon.Companion#getPreviewPageCount calculation. */
    private int getPreviewPageCount() {
        if (mInfo == null || mInfo.contents.isEmpty()) return 0;
        int stride = getPreviewPageStride();
        int size = mInfo.contents.size();
        int pageCount = (int) Math.ceil(size / (double) stride);
        if (mInfo.isCurrentDisplayHighlightGrid()) {
            size -= stride - 3;
            pageCount = mInfo.contents.size() > stride - 3
                    ? (int) Math.ceil(size / (double) stride) + 1 : 1;
        }
        return pageCount <= 1 || size % stride != 1 ? pageCount : pageCount - 1;
    }

    /** Exact FlexibleFolderIcon#snapDesPageForDrag page selection. */
    private void snapDesPageForDrag() {
        if (!isLargeFolder() || !mInfo.hasGrid2x2()) return;
        int size = mInfo.contents.size();
        int stride = getPreviewPageStride();
        int adjustedSize = size;
        if (mInfo.isCurrentDisplayHighlightGrid() && size >= (stride + 1) - 3) {
            adjustedSize += 3;
        }
        if (adjustedSize + 1 <= stride) return;

        int destinationPage = mCurrentPreviewPage;
        int pageOffset = getPreviewPageOffset(mCurrentPreviewPage);
        int previewAndStackedCount = Math.min(
                (stride + 4) - (mInfo.isCurrentDisplayHighlightGrid()
                        && mCurrentPreviewPage == 0 ? 3 : 0),
                Math.max(0, size - pageOffset));
        if (mInfo.isCurrentDisplayHighlightGrid() && mCurrentPreviewPage == 0) {
            previewAndStackedCount += 3;
        }
        if (previewAndStackedCount >= stride + 4) {
            int remainder = adjustedSize % stride;
            destinationPage = ((remainder == 1 || remainder == 0)
                    ? getPreviewPageCount()
                    : (int) Math.ceil((adjustedSize + 1) / (double) stride)) - 1;
        }
        snapToPreviewPage(Math.max(mCurrentPreviewPage, destinationPage), true);
    }

    /** Mirrors BigFolderTouchController#snapToPage(page, animate) for non-touch snaps. */
    private void snapToPreviewPage(int requestedPage, boolean animate) {
        if (mListView == null || mNextListView == null || mAdapter == null
                || mNextAdapter == null) return;
        int target = Math.max(0, Math.min(requestedPage,
                Math.max(0, getPreviewPageCount() - 1)));
        if (mPageAnimator != null) mPageAnimator.cancel();
        if (target == mCurrentPreviewPage) {
            mProgrammaticSnapTarget = -1;
            return;
        }

        final int startPage = mCurrentPreviewPage;
        final float width = Math.max(1, getPreviewWidth());
        final float direction = getLayoutDirection() == View.LAYOUT_DIRECTION_RTL ? -1f : 1f;
        configurePage(mListView, mAdapter, startPage);
        mAdapter.setList(mInfo.contents);
        configurePage(mNextListView, mNextAdapter, target);
        mNextAdapter.setList(mInfo.contents);
        mListView.setTranslationX(0f);
        mNextListView.setTranslationX(direction * (target - startPage) * width);
        mNextListView.setVisibility(View.VISIBLE);
        mProgrammaticSnapTarget = target;
        showPageIndicator();

        mPageAnimator = ValueAnimator.ofFloat(0f, 1f);
        mPageAnimator.setDuration(550L);
        mPageAnimator.setInterpolator(mProgrammaticSnapInterpolator);
        mPageAnimator.addUpdateListener(animator -> {
            float fraction = (float) animator.getAnimatedValue();
            mListView.setTranslationX(direction * (startPage - target) * width * fraction);
            mNextListView.setTranslationX(direction * (target - startPage) * width
                    * (1f - fraction));
        });
        mPageAnimator.addListener(new AnimatorListenerAdapter() {
            private boolean mCancelled;

            @Override
            public void onAnimationCancel(Animator animation) {
                mCancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!mCancelled) {
                    mCurrentPreviewPage = target;
                    configurePage(mListView, mAdapter, target);
                    mAdapter.setList(mInfo.contents);
                    if (mPageIndicator != null) mPageIndicator.setCurrentPage(target);
                }
                mListView.setTranslationX(0f);
                mNextListView.setTranslationX(0f);
                mNextListView.setVisibility(View.INVISIBLE);
                mProgrammaticSnapTarget = -1;
                removeCallbacks(mHidePageIndicator);
                postDelayed(mHidePageIndicator, 500L);
            }
        });
        mPageAnimator.start();
        if (!animate) mPageAnimator.end();
    }

    @Override
    public void onDragStart(DropTarget.DragObject dragObject, DragOptions options) {
        if (!mPageDragging && (mPageAnimator == null || !mPageAnimator.isRunning())) {
            mOriginalPreviewPage = mCurrentPreviewPage;
        }
        mDropAcceptedDuringDrag = false;
    }

    @Override
    public void onDragEnd() {
        // FlexibleFolderIcon keeps originalPage/drop state until the next drag begins. In
        // particular, Workspace may deliver the final onDragExit during its own drag-end cleanup.
    }

    @Override
    public void onDragEnter(ItemInfo dragInfo) {
        super.onDragEnter(dragInfo);
        if (isLargeFolder() && acceptDrop(dragInfo)) {
            snapDesPageForDrag();
        }
    }

    @Override
    public void onDragExit() {
        if (isLargeFolder() && !mDropAcceptedDuringDrag) {
            int originalPage = Math.min(mOriginalPreviewPage,
                    Math.max(0, getPreviewPageCount() - 1));
            snapToPreviewPage(originalPage, true);
        }
        super.onDragExit();
    }

    private boolean isInPreviewSwipeArea(float x, float y) {
        Rect bounds = new Rect();
        getColorOsGroupBounds(bounds);
        return bounds.contains((int) x, (int) y);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // BigFolderTouchController starts horizontal paging only for the 2x2 flexible-folder
        // form. Rectangular extended grids retain their stacked preview behavior.
        if (!isLargeFolder() || !mInfo.hasGrid2x2()
                || getPreviewPageCount() <= 1 || mListView == null) {
            return super.dispatchTouchEvent(event);
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (mPageAnimator != null) mPageAnimator.cancel();
                mPageDownX = event.getX();
                mPageDownY = event.getY();
                mPageDragging = false;
                mTargetPreviewPage = -1;
                mDragLowerPage = mCurrentPreviewPage;
                mDragUpperPage = mCurrentPreviewPage;
                mOverScrollPage = -1;
                mOverScrollInput = 0.0f;
                mOverScrollAmount = 0.0f;
                if (isInPreviewSwipeArea(mPageDownX, mPageDownY)) {
                    // OplusWorkspace#setMBigFolderIntercept keeps MOVE events routed to the
                    // flexible folder. AOSP Workspace has no equivalent flag, so claim this
                    // gesture stream at its source and decide horizontal-vs-click locally.
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (!isInPreviewSwipeArea(mPageDownX, mPageDownY)) break;
                float dx = event.getX() - mPageDownX;
                float dy = event.getY() - mPageDownY;
                if (!mPageDragging && Math.abs(dx) > Math.abs(dy)
                        && Math.abs(dx) >= ViewConfiguration.get(getContext())
                                .getScaledPagingTouchSlop()) {
                    mPageDragging = true;
                    getParent().requestDisallowInterceptTouchEvent(true);
                    MotionEvent cancel = MotionEvent.obtain(event);
                    cancel.setAction(MotionEvent.ACTION_CANCEL);
                    super.dispatchTouchEvent(cancel);
                    cancel.recycle();
                    showPageIndicator();
                }
                if (mPageDragging) {
                    updatePageDrag(dx);
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mPageDragging) {
                    float distance = event.getX() - mPageDownX;
                    float snapThreshold = getResources().getDisplayMetrics().density * 20.0f;
                    finishPageDrag(mTargetPreviewPage >= 0
                            && Math.abs(distance) > snapThreshold);
                    getParent().requestDisallowInterceptTouchEvent(false);
                    return true;
                }
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mPageDragging) {
                    finishPageDrag(false);
                    getParent().requestDisallowInterceptTouchEvent(false);
                    return true;
                }
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
            default:
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    private void updatePageDrag(float distance) {
        boolean rtl = getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        float width = Math.max(1, getPreviewWidth());
        int lastPage = Math.max(0, getPreviewPageCount() - 1);
        float pagePosition = mCurrentPreviewPage
                + ((rtl ? distance : -distance) / width);
        if (pagePosition < 0.0f || pagePosition > lastPage) {
            int boundaryPage = pagePosition < 0.0f ? 0 : lastPage;
            float directionSign = rtl ? 1.0f : -1.0f;
            float distanceAtBoundary = ((boundaryPage - mCurrentPreviewPage) * width)
                    / directionSign;
            float overScrollInput = distance - distanceAtBoundary;
            float delta = overScrollInput - mOverScrollInput;
            // Exact ColorOS BigFolderTouchController.Companion#calcRealOverScrollDist:
            // each input delta is reduced by both rigidity 3 and the already-consumed
            // fraction of the preview width.
            mOverScrollAmount += ((1.0f - (Math.abs(mOverScrollAmount) / width))
                    * delta) / 3.0f;
            mOverScrollInput = overScrollInput;
            mOverScrollPage = boundaryPage;
            mTargetPreviewPage = -1;
            if (mDragLowerPage != boundaryPage) {
                mDragLowerPage = boundaryPage;
                configurePage(mListView, mAdapter, boundaryPage);
                mAdapter.setList(mInfo.contents);
            }
            mDragUpperPage = boundaryPage;
            mNextListView.setVisibility(View.INVISIBLE);
            mListView.setTranslationX(mOverScrollAmount);
            return;
        }

        mOverScrollPage = -1;
        mOverScrollInput = 0.0f;
        mOverScrollAmount = 0.0f;
        int lowerPage = Math.max(0, Math.min(lastPage, (int) Math.floor(pagePosition)));
        int upperPage = Math.max(0, Math.min(lastPage, (int) Math.ceil(pagePosition)));
        if (mDragLowerPage != lowerPage) {
            mDragLowerPage = lowerPage;
            configurePage(mListView, mAdapter, lowerPage);
            mAdapter.setList(mInfo.contents);
        }
        if (upperPage != lowerPage) {
            if (mDragUpperPage != upperPage) {
                mDragUpperPage = upperPage;
                configurePage(mNextListView, mNextAdapter, upperPage);
                mNextAdapter.setList(mInfo.contents);
            }
            mNextListView.setVisibility(View.VISIBLE);
        } else {
            mDragUpperPage = lowerPage;
            mNextListView.setVisibility(View.INVISIBLE);
        }
        float layoutDirection = rtl ? -1.0f : 1.0f;
        mListView.setTranslationX(layoutDirection * (lowerPage - pagePosition) * width);
        if (upperPage != lowerPage) {
            mNextListView.setTranslationX(
                    layoutDirection * (upperPage - pagePosition) * width);
        }
        if (pagePosition > mCurrentPreviewPage) {
            mTargetPreviewPage = upperPage;
        } else if (pagePosition < mCurrentPreviewPage) {
            mTargetPreviewPage = lowerPage;
        } else {
            mTargetPreviewPage = mCurrentPreviewPage;
        }
    }

    private void finishPageDrag(boolean commit) {
        mPageDragging = false;
        final boolean overScroll = mOverScrollPage >= 0;
        final int target = overScroll ? mOverScrollPage
                : (commit && mTargetPreviewPage >= 0
                        ? mTargetPreviewPage : mCurrentPreviewPage);
        final float currentStart = mListView.getTranslationX();
        final float nextStart = mNextListView.getTranslationX();
        boolean rtl = getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        float width = Math.max(1, getPreviewWidth());
        float layoutDirection = rtl ? -1.0f : 1.0f;
        final float currentEnd = overScroll ? 0.0f
                : layoutDirection * (mDragLowerPage - target) * width;
        final float nextEnd = overScroll ? nextStart
                : layoutDirection * (mDragUpperPage - target) * width;
        mPageAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
        mPageAnimator.setDuration(overScroll ? 400L : 150L);
        mPageAnimator.setInterpolator(overScroll
                ? mPageIndicatorInterpolator : com.android.launcher3.anim.Interpolators.DEACCEL_2);
        mPageAnimator.addUpdateListener(animator -> {
            float fraction = (float) animator.getAnimatedValue();
            mListView.setTranslationX(currentStart + ((currentEnd - currentStart) * fraction));
            if (!overScroll && mDragUpperPage != mDragLowerPage) {
                mNextListView.setTranslationX(nextStart + ((nextEnd - nextStart) * fraction));
            }
        });
        mPageAnimator.addListener(new AnimatorListenerAdapter() {
            private boolean mCancelled;

            @Override
            public void onAnimationCancel(Animator animation) {
                mCancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!mCancelled) {
                    mCurrentPreviewPage = target;
                    configurePage(mListView, mAdapter, mCurrentPreviewPage);
                    mAdapter.setList(mInfo.contents);
                    mPageIndicator.setCurrentPage(mCurrentPreviewPage);
                }
                mListView.setTranslationX(0.0f);
                mNextListView.setTranslationX(0.0f);
                mNextListView.setVisibility(View.INVISIBLE);
                mTargetPreviewPage = -1;
                mOverScrollPage = -1;
                mOverScrollInput = 0.0f;
                mOverScrollAmount = 0.0f;
                removeCallbacks(mHidePageIndicator);
                postDelayed(mHidePageIndicator, 500L);
            }
        });
        mPageAnimator.start();
    }

    private void showPageIndicator() {
        removeCallbacks(mHidePageIndicator);
        mPageIndicator.setPageCount(getPreviewPageCount());
        mPageIndicator.setCurrentPage(mCurrentPreviewPage);
        mPageIndicator.setVisibility(View.VISIBLE);
        mPageIndicator.animate().cancel();
        mPageIndicator.animate().alpha(1.0f).setStartDelay(67L).setDuration(350L)
                .setInterpolator(mPageIndicatorInterpolator).start();
        getFolderName().animate().cancel();
        getFolderName().animate().alpha(0.0f).setStartDelay(0L).setDuration(300L)
                .setInterpolator(mPageIndicatorInterpolator).start();
    }

    private void hidePageIndicator() {
        if (mPageDragging) return;
        mPageIndicator.animate().cancel();
        mPageIndicator.animate().alpha(0.0f).setStartDelay(0L).setDuration(300L)
                .setInterpolator(mPageIndicatorInterpolator).start();
        getFolderName().animate().cancel();
        getFolderName().animate().alpha(1.0f).setStartDelay(67L).setDuration(350L)
                .setInterpolator(mPageIndicatorInterpolator).start();
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
            // FolderManager#convertFolder preserves an existing type and otherwise chooses
            // nine-grid (option 8) before beginning the conversion animation.
            int type = mInfo.hasOption(ColorOsFolderStyleView.STYLE_NINE_GRID)
                    ? ColorOsFolderStyleView.STYLE_NINE_GRID
                    : mInfo.hasOption(ColorOsFolderStyleView.STYLE_FOUR_GRID)
                            ? ColorOsFolderStyleView.STYLE_FOUR_GRID
                            : mInfo.hasOption(ColorOsFolderStyleView.STYLE_HIERARCHICAL)
                                    ? ColorOsFolderStyleView.STYLE_HIERARCHICAL
                                    : ColorOsFolderStyleView.STYLE_NINE_GRID;
            setBigFolderType(type);
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
        HxyLargeFolderListView destinationList = mProgrammaticSnapTarget >= 0
                ? mNextListView : mListView;
        HxyLargeFolderAdapter destinationAdapter = mProgrammaticSnapTarget >= 0
                ? mNextAdapter : mAdapter;
        int maxPreviewSlots = destinationAdapter.getMaxSize();
        int pageOffset = destinationAdapter.getPageOffset();
        int localIndex = contentIndex - pageOffset;
        int postDropItemCount = mInfo.contents.size() - pageOffset + 1;
        int firstStackedIndex = maxPreviewSlots - 1;
        boolean hasOverflowStack = postDropItemCount > maxPreviewSlots;
        int stackIndex = -1;
        int slot;
        if (localIndex < 0 || localIndex >= firstStackedIndex + 4) {
            slot = maxPreviewSlots - 1;
            stackIndex = 3;
        } else if (hasOverflowStack && localIndex >= firstStackedIndex) {
            slot = maxPreviewSlots - 1;
            stackIndex = Math.min(3, localIndex - firstStackedIndex);
        } else {
            slot = Math.min(Math.max(0, localIndex), maxPreviewSlots - 1);
        }
        int[] coordinate = destinationList.getCoordinateXY(slot);
        Rect groupBounds = new Rect();
        float workspaceScale = mActivity.getDragLayer()
                .getDescendantRectRelativeToSelf(this, groupBounds);
        int renderedSize = destinationList.getRenderedChildSize(slot);
        // The final preview slot becomes OPPO's 2x2 overflow stack. Animate to the same
        // sub-drawable rendered inside that cell, rather than into the whole stack.
        if (stackIndex >= 0) {
            int stackGap = HxyLargeFolderProxy.getFolderIconOutSpace(getContext());
            int stackIconSize = Math.max(1, (renderedSize - stackGap) / 2);
            coordinate[0] += (stackIndex % 2) * (stackIconSize + stackGap);
            coordinate[1] += (stackIndex / 2) * (stackIconSize + stackGap);
            renderedSize = stackIconSize;
        }
        int targetSize = Math.round(renderedSize * workspaceScale);
        int desiredLeft = groupBounds.left
                + Math.round((destinationList.getLeft() + coordinate[0]) * workspaceScale);
        int desiredTop = groupBounds.top
                + Math.round((destinationList.getTop() + coordinate[1]) * workspaceScale);
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
        mDropAcceptedDuringDrag = true;
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
