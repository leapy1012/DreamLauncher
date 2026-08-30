package com.android.launcher3.folder.large;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.PathInterpolator;
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
import com.coui.appcompat.indicator.COUIPageIndicator2;

import java.util.function.Predicate;

public class HxyLargeFolderIcon extends FolderIcon implements ISwitchFolderAnimation {
    public static final int LARGE_FOLDER_SPAN_X = 2;
    public static final int LARGE_FOLDER_SPAN_Y = 2;
    private static final int RECURSION_LOAD_COUNT = 3;
    private static final long SCROLL_SHOW_DURATION_MS = 300;
    private static final long SCROLL_SHOW_DELAY_MS = 67;
    private static final long SCROLL_HIDE_DURATION_MS = 300;
    private static final long SCROLL_END_RESTORE_DELAY_MS = 500;
    private static final PathInterpolator SCROLL_INDICATOR_PATH =
            new PathInterpolator(0.33f, 0f, 0.67f, 1f);

    private boolean isFirstFolderNameTop;
    private HxyLargeFolderAdapter mAdapter;
    private HxyLargeFolderAdapter mAdjacentAdapter;
    private HxyLargeFolderListView mListView;
    private HxyLargeFolderListView mAdjacentListView;
    private HxyLargeFolderSwitcher mSwitcher;
    private HxyLargeFolderPagingController mPagingController;
    private COUIPageIndicator2 mIndicator;
    private float mScrollDistance;
    private int mPreviewPage;
    private boolean mScrolling;
    private boolean mIndicatorShowing;
    private boolean mPagingTookOver;
    /** Neighbor page currently bound on {@link #mAdjacentListView}. */
    private int mAdjacentBoundPage = -1;
    private float mLastIndicatorFrac = -1f;
    private final Path mPlateClipPath = new Path();
    private int mPlateClipW;
    private int mPlateClipH;
    private float mPlateClipRound = -1f;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mRestoreChromeRunnable = this::restoreNameAfterScroll;

    public HxyLargeFolderIcon(Context context) {
        this(context, (AttributeSet) null);
    }

    public HxyLargeFolderIcon(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mListView = null;
        this.mAdapter = null;
        this.mSwitcher = null;
        this.isFirstFolderNameTop = true;
        initData();
    }

    public void release() {
        mHandler.removeCallbacks(mRestoreChromeRunnable);
        if (mPagingController != null) {
            mPagingController.abort();
            mPagingController = null;
        }
        if (mSwitcher != null) {
            mSwitcher.release();
            mSwitcher = null;
        }
        if (mAdapter != null) {
            mAdapter.release();
            mAdapter = null;
        }
        if (mAdjacentAdapter != null) {
            mAdjacentAdapter.release();
            mAdjacentAdapter = null;
        }
        this.mListView = null;
        this.mAdjacentListView = null;
        this.mIndicator = null;
        mAdjacentBoundPage = -1;
    }

    private void initData() {
        mSwitcher = new HxyLargeFolderSwitcher();
        mPagingController = new HxyLargeFolderPagingController(this);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (isLargeFolder()) {
            // Ensure plate geometry is valid before padding / title / clip.
            ensureLargePreviewBackground();
            setFolderNameTop();
            updateListViewPadding();
            measureChildren(widthMeasureSpec, heightMeasureSpec);
        } else if (this.isFirstFolderNameTop) {
            this.isFirstFolderNameTop = false;
            setFolderNameTop();
        }
        refreshListData();
    }

    /** Force PreviewBackground.setup — large folders may lack a reference drawable. */
    private void ensureLargePreviewBackground() {
        if (mActivity == null || getMeasuredWidth() < 1 || getMeasuredHeight() < 1) {
            return;
        }
        getFolderBackground().setup(
                getContext(),
                mActivity,
                this,
                getMeasuredWidth(),
                getMeasuredHeight(),
                getPaddingTop());
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
            int top = isLargeFolder() ? getLargeFolderNameTop() : getFolderNameTop();
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getFolderName().getLayoutParams();
            if (lp.topMargin != top) {
                lp.topMargin = top;
                getFolderName().setLayoutParams(lp);
            }
            if (mIndicator != null) {
                FrameLayout.LayoutParams ilp = (FrameLayout.LayoutParams) mIndicator.getLayoutParams();
                if (ilp.topMargin != top) {
                    ilp.topMargin = top;
                    mIndicator.setLayoutParams(ilp);
                }
            }
        }
    }

    private int getLargeFolderNameTop() {
        // Title sits below the frosted plate. Prefer live plate metrics; fall back to
        // computePreviewHeight when background has not been set up yet.
        DeviceProfile grid = this.mActivity.getDeviceProfile();
        int previewH = getFolderBackground().getPreviewHeight();
        int plateBottom = getFolderBackground().getBasePreviewOffsetY() + previewH;
        if (previewH <= 0) {
            plateBottom = HxyLargeFolderProxy.computePreviewHeight(
                    this, getMeasuredHeight(), grid.folderIconSizePx);
        }
        return plateBottom + grid.iconDrawablePaddingPx;
    }

    private void updateListViewPadding() {
        if (mListView == null || mActivity == null) {
            return;
        }
        DeviceProfile grid = mActivity.getDeviceProfile();
        int previewW = getFolderBackground().getPreviewWidth();
        int previewH = getFolderBackground().getPreviewHeight();
        if (previewW <= 0) {
            previewW = HxyLargeFolderProxy.computePreviewWidth(
                    this, getMeasuredWidth(), grid.folderIconSizePx);
        }
        if (previewH <= 0) {
            previewH = HxyLargeFolderProxy.computePreviewHeight(
                    this, getMeasuredHeight(), grid.folderIconSizePx);
        }
        int platePadX = getPreviewOffsetX();
        int platePadY = Math.max(0, getFolderBackground().getBasePreviewOffsetY() - getPaddingTop());
        int inset = Math.max(
                getResources().getDimensionPixelSize(R.dimen.hxy_large_folder_icon_out_space),
                Math.round(Math.min(previewW, previewH) * 0.06f));

        // Size the list to the frosted plate so measure doesn't eat the title band.
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mListView.getLayoutParams();
        boolean lpChanged = lp.width != previewW || lp.height != previewH
                || lp.leftMargin != platePadX || lp.topMargin != platePadY;
        if (lpChanged) {
            lp.width = previewW;
            lp.height = previewH;
            lp.leftMargin = platePadX;
            lp.topMargin = platePadY;
            lp.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
            mListView.setLayoutParams(lp);
        }
        // Item gutters are set in HxyLargeFolderListView.onMeasure (Oppo-style).
        if (mListView.getPaddingLeft() != inset || mListView.getPaddingTop() != inset) {
            mListView.setPadding(inset, inset, inset, inset);
        }
        if (mAdjacentListView != null) {
            FrameLayout.LayoutParams alp = (FrameLayout.LayoutParams) mAdjacentListView.getLayoutParams();
            if (alp.width != previewW || alp.height != previewH
                    || alp.leftMargin != platePadX || alp.topMargin != platePadY) {
                alp.width = previewW;
                alp.height = previewH;
                alp.leftMargin = platePadX;
                alp.topMargin = platePadY;
                alp.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
                mAdjacentListView.setLayoutParams(alp);
            }
            if (mAdjacentListView.getPaddingLeft() != inset
                    || mAdjacentListView.getPaddingTop() != inset) {
                mAdjacentListView.setPadding(inset, inset, inset, inset);
            }
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
        this.mAdjacentListView =
                (HxyLargeFolderListView) findViewById(R.id.folder_icon_content_adjacent);
        this.mIndicator = findViewById(R.id.folder_icon_indicator);
        this.mAdapter = new HxyLargeFolderAdapter(getContext());
        this.mAdjacentAdapter = new HxyLargeFolderAdapter(getContext());
        if (mPagingController == null) {
            mPagingController = new HxyLargeFolderPagingController(this);
        }
        applyPreviewMode();
        this.mListView.setAdapter(this.mAdapter);
        if (mAdjacentListView != null) {
            mAdjacentListView.setAdapter(mAdjacentAdapter);
        }
        BasePageLinearAdapter.ItemClickListener<WorkspaceItemInfo> click =
                (view, info) -> onFolderItemClick(view, info);
        BasePageLinearAdapter.ItemLongClickListener<WorkspaceItemInfo> longClick =
                (view, info) -> executeLargeFolderIconLongClick(view, info);
        this.mAdapter.setItemListener(click);
        this.mAdapter.setItemLongListener(longClick);
        this.mAdjacentAdapter.setItemListener(click);
        this.mAdjacentAdapter.setItemLongListener(longClick);
        if (mIndicator != null) {
            mIndicator.setAlpha(0f);
            mIndicator.setVisibility(INVISIBLE);
            mIndicator.setPageIndicatorDotsColor(0x66FFFFFF);
            mIndicator.setTraceDotColor(0xFFFFFFFF);
        }
        initLoadListData();
    }

    /** Applies ColorOS nine / four / highlight preview sizing to the closed big folder. */
    public void applyPreviewMode() {
        if (mAdapter == null || mListView == null) {
            return;
        }
        ItemInfo info = (ItemInfo) getTag();
        // Mode change resets paging — layouts differ across pages in highlight mode.
        mPreviewPage = 0;
        mScrollDistance = 0f;
        mAdjacentBoundPage = -1;
        int span = HxyBigFolderPreviewModes.getPreviewSpan(info);
        int max = HxyBigFolderPreviewModes.getPreviewMaxSize(info, mPreviewPage);
        mAdapter.setFolderInfo(info);
        mAdapter.setMaxSize(max);
        mListView.setSpanCount(span);
        mListView.setHighlightLayout(
                HxyBigFolderPreviewModes.isHighlightPage(info, mPreviewPage));
        if (mAdjacentAdapter != null && mAdjacentListView != null) {
            mAdjacentAdapter.setFolderInfo(info);
            mAdjacentAdapter.setMaxSize(
                    HxyBigFolderPreviewModes.getPreviewMaxSize(info, 1));
            mAdjacentListView.setSpanCount(span);
            mAdjacentListView.setHighlightLayout(false);
        }
        refreshListView();
        updatePageIndicator();
        prepareForDragPreviewCapture();
        requestLayout();
        invalidate();
    }

    /**
     * Force plate + cell geometry before {@link DragPreviewProvider} snapshots.
     * Needed when the folder is {@code INVISIBLE} during the popup pre-drag.
     */
    public void prepareForDragPreviewCapture() {
        if (mListView == null) {
            return;
        }
        mListView.forceLayout();
        ensureListLaidOut(mListView);
        forceRebindListChildren(mListView);
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
        if (isLargeFolder() && mAdapter != null && mInfo != null) {
            int pageCount = getPreviewPageCount();
            if (mPreviewPage >= pageCount) {
                mPreviewPage = Math.max(0, pageCount - 1);
                mScrollDistance = mPreviewPage * Math.max(1, getPageWidth());
            }
            mAdapter.setFolderInfo(mInfo);
            mAdapter.setList(mInfo.contents, mPreviewPage);
            if (mListView != null) {
                mListView.setHighlightLayout(
                        HxyBigFolderPreviewModes.isHighlightPage(mInfo, mPreviewPage));
            }
            updatePageIndicator();
            if (!mScrolling) {
                applyIdlePageLayout();
            }
            return;
        }
        updatePreviewItems(false);
        invalidate();
    }

    private void applyIdlePageLayout() {
        if (mListView == null) {
            return;
        }
        mListView.setTranslationX(0f);
        mListView.setAlpha(1f);
        mListView.setVisibility(VISIBLE);
        if (mAdjacentListView != null) {
            // INVISIBLE (not GONE) so the neighbor keeps plate-sized layout for the
            // first frame of a swipe — GONE skipped measure and caused the broken
            // RTL mid-state (empty left / wrong-scale icons).
            mAdjacentListView.setVisibility(INVISIBLE);
            mAdjacentListView.setTranslationX(0f);
            mAdjacentListView.setAlpha(1f);
        }
    }

    private void updatePageIndicator() {
        if (mIndicator == null || mInfo == null) {
            return;
        }
        int count = getPreviewPageCount();
        if (count <= 1) {
            mIndicator.setDotsCount(0);
            mIndicator.setVisibility(INVISIBLE);
            mIndicator.setAlpha(0f);
            return;
        }
        if (mIndicator.getDotsCount() != count) {
            mIndicator.setDotsCount(count);
        }
        mIndicator.setCurrentPosition(mPreviewPage);
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

    @Override
    public void drawPreviewBackground(Canvas canvas) {
        // Skip preview-param recompute while paging — it ran every MOVE invalidate.
        if (mScrolling) {
            getFolderBackground().drawThemeBackground(canvas, getContext());
            return;
        }
        super.drawPreviewBackground(canvas);
    }

    private void dispatchLargeDraw(Canvas canvas) {
        if (!getIconVisible()) {
            return;
        }
        drawPreviewBackground(canvas);
        int save = canvas.save();
        canvas.clipPath(getCachedPlateClipPath());
        if (mListView != null && mListView.getVisibility() == View.VISIBLE) {
            drawChild(canvas, mListView, getDrawingTime());
        }
        if (mAdjacentListView != null && mAdjacentListView.getVisibility() == View.VISIBLE) {
            drawChild(canvas, mAdjacentListView, getDrawingTime());
        }
        canvas.restoreToCount(save);
        if (mIndicator != null && mIndicator.getVisibility() == View.VISIBLE
                && mIndicator.getAlpha() > 0f) {
            drawChild(canvas, mIndicator, getDrawingTime());
        }
        View name = getFolderName();
        if (name != null && name.getVisibility() == View.VISIBLE && name.getAlpha() > 0f) {
            drawChild(canvas, name, getDrawingTime());
        }
    }

    private Path getCachedPlateClipPath() {
        float round = HxyLargeFolderProxy.getFolderRound(getContext());
        int w = getFolderBackground().getPreviewWidth();
        int h = getFolderBackground().getPreviewHeight();
        if (w != mPlateClipW || h != mPlateClipH || round != mPlateClipRound) {
            mPlateClipW = w;
            mPlateClipH = h;
            mPlateClipRound = round;
            mPlateClipPath.set(getFolderBackground().getLargeFolderClipPath(round));
        }
        return mPlateClipPath;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            mPagingTookOver = false;
        }
        // Pre-drag / drag already owns this icon (popup long-press). Do not let plate
        // paging steal MOVE or call requestDisallowIntercept — that blocks DragController
        // from promoting pre-drag into a real workspace move.
        final boolean dragOwnsTouch = mActivity != null
                && mActivity.getDragController() != null
                && mActivity.getDragController().isDragging();
        if (isLargeFolder() && mPagingController != null && !dragOwnsTouch) {
            boolean paging = mPagingController.onTouchEvent(ev);
            if (paging) {
                if (!mPagingTookOver) {
                    mPagingTookOver = true;
                    // Cancel children so click / long-press don't eat a flick.
                    MotionEvent cancel = MotionEvent.obtain(ev);
                    cancel.setAction(MotionEvent.ACTION_CANCEL);
                    super.dispatchTouchEvent(cancel);
                    cancel.recycle();
                    if (getParent() != null) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    mPagingTookOver = false;
                }
                return true;
            }
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            mPagingTookOver = false;
        }
        return super.dispatchTouchEvent(ev);
    }

    // —— Closed big-folder paging (ColorOS) ——

    public boolean canPageSwipe() {
        return isLargeFolder() && getPreviewPageCount() > 1;
    }

    public boolean isInSwipeArea(float x, float y) {
        int left = getFolderBackground().getBasePreviewOffsetX();
        int top = getFolderBackground().getBasePreviewOffsetY();
        int pw = getPreviewWidth();
        int ph = getPreviewHeight();
        if (pw > 0 && ph > 0) {
            return x >= left && x <= left + pw && y >= top && y <= top + ph;
        }
        if (mListView != null && mListView.getWidth() > 0 && mListView.getHeight() > 0) {
            return x >= mListView.getLeft() && x <= mListView.getRight()
                    && y >= mListView.getTop() && y <= mListView.getBottom();
        }
        return x >= 0 && x <= getWidth() && y >= 0 && y <= getHeight();
    }

    public int getPreviewPage() {
        return mPreviewPage;
    }

    public int getPreviewPageCount() {
        if (mInfo == null) {
            return 1;
        }
        return HxyBigFolderPreviewModes.getPreviewPageCount(mInfo, mInfo.contents.size());
    }

    public int getPageWidth() {
        int w = mListView != null ? mListView.getWidth() : 0;
        if (w <= 0) {
            w = getPreviewWidth();
        }
        return Math.max(1, w);
    }

    public float getScrollDistance() {
        return mScrollDistance;
    }

    public void setIndicatorPage(int page) {
        if (mIndicator != null) {
            mIndicator.setCurrentPosition(page);
        }
    }

    public void updateScrollDistance(float scroll, boolean scrolling) {
        mScrollDistance = scroll;
        int pageW = getPageWidth();
        int pageCount = getPreviewPageCount();
        if (pageCount <= 1 || mListView == null) {
            applyIdlePageLayout();
            return;
        }

        int settled = scrolling
                ? Math.max(0, Math.min(pageCount - 1, mPreviewPage))
                : Math.max(0, Math.min(pageCount - 1, Math.round(scroll / pageW)));
        float settledScroll = settled * pageW;
        float delta = scroll - settledScroll;

        if (!scrolling || Math.abs(delta) < 0.5f) {
            bindPageIfNeeded(mAdapter, mListView, settled);
            mListView.setTranslationX(0f);
            mListView.setAlpha(1f);
            mListView.setVisibility(VISIBLE);
            if (mAdjacentListView != null) {
                mAdjacentListView.setVisibility(INVISIBLE);
                mAdjacentListView.setTranslationX(0f);
                mAdjacentListView.setAlpha(1f);
            }
            updateIndicatorThrottled(settled, 0f, true);
            invalidate();
            return;
        }

        if (delta > 0f) {
            int next = Math.min(pageCount - 1, settled + 1);
            float offset = Math.min(delta, pageW);
            float frac = Math.max(0f, Math.min(1f, offset / pageW));
            prepareAdjacentPage(next);
            mListView.setVisibility(VISIBLE);
            mListView.setTranslationX(-offset);
            if (mAdjacentListView != null && next != settled) {
                mAdjacentListView.setVisibility(VISIBLE);
                mAdjacentListView.setTranslationX(pageW - offset);
            }
            updateIndicatorThrottled(settled, frac, false);
        } else {
            int prev = Math.max(0, settled - 1);
            float offset = Math.min(-delta, pageW);
            float frac = Math.max(0f, Math.min(1f, offset / pageW));
            prepareAdjacentPage(prev);
            mListView.setVisibility(VISIBLE);
            mListView.setTranslationX(offset);
            if (mAdjacentListView != null && prev != settled) {
                mAdjacentListView.setVisibility(VISIBLE);
                mAdjacentListView.setTranslationX(-pageW + offset);
            }
            updateIndicatorThrottled(prev, 1f - frac, false);
        }
        invalidate();
    }

    private void updateIndicatorThrottled(int page, float frac, boolean force) {
        if (mIndicator == null) {
            return;
        }
        if (!force && Math.abs(frac - mLastIndicatorFrac) < 0.04f) {
            return;
        }
        mLastIndicatorFrac = frac;
        mIndicator.setCurrentPosition(page, frac);
    }

    /**
     * Bind neighbor page and sync-layout it to the plate size before it becomes visible.
     * Adjacent must never stay GONE — GONE skips measure and yields tiny/wrong icons.
     */
    private void prepareAdjacentPage(int neighborPage) {
        if (mAdjacentListView == null || mAdjacentAdapter == null || mInfo == null
                || mListView == null) {
            return;
        }
        if (mAdjacentBoundPage != neighborPage || mAdjacentAdapter.getPageIndex() != neighborPage) {
            mAdjacentAdapter.setFolderInfo(mInfo);
            mAdjacentAdapter.setList(mInfo.contents, neighborPage);
            mAdjacentBoundPage = neighborPage;
        }
        mAdjacentListView.setHighlightLayout(
                HxyBigFolderPreviewModes.isHighlightPage(mInfo, neighborPage));
        mAdjacentListView.setAlpha(1f);
        // Keep INVISIBLE until caller sets VISIBLE with translation — still lays out.
        if (mAdjacentListView.getVisibility() == GONE) {
            mAdjacentListView.setVisibility(INVISIBLE);
        }
        ensureAdjacentLaidOut();
        rebindAdjacentChildren();
    }

    private void ensureAdjacentLaidOut() {
        if (mAdjacentListView == null) {
            return;
        }
        // Prefer primary list geometry — adjacent may still be 0×0 after GONE/INVISIBLE.
        if (mListView != null && mListView.getWidth() > 0 && mListView.getHeight() > 0) {
            int w = mListView.getWidth();
            int h = mListView.getHeight();
            int left = mListView.getLeft();
            int top = mListView.getTop();
            mAdjacentListView.measure(
                    View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
            mAdjacentListView.layout(left, top, left + w, top + h);
            return;
        }
        ensureListLaidOut(mAdjacentListView);
    }

    private void rebindAdjacentChildren() {
        rebindListChildren(mAdjacentListView);
    }

    private void bindPageIfNeeded(HxyLargeFolderAdapter adapter, HxyLargeFolderListView list,
            int page) {
        if (adapter == null || list == null || mInfo == null) {
            return;
        }
        list.setHighlightLayout(HxyBigFolderPreviewModes.isHighlightPage(mInfo, page));
        if (adapter.getPageIndex() != page) {
            adapter.setFolderInfo(mInfo);
            adapter.setList(mInfo.contents, page);
            if (list == mAdjacentListView) {
                mAdjacentBoundPage = page;
                ensureAdjacentLaidOut();
            } else {
                list.requestLayout();
            }
        }
    }

    public void onFolderScrollPageStart() {
        mScrolling = true;
        mLastIndicatorFrac = -1f;
        mHandler.removeCallbacks(mRestoreChromeRunnable);
        // Pre-layout neighbors so the first MOVE frame has correct cell sizes.
        int pageCount = getPreviewPageCount();
        if (pageCount > 1 && mListView != null) {
            int settled = mPreviewPage;
            if (settled + 1 < pageCount) {
                prepareAdjacentPage(settled + 1);
            } else if (settled - 1 >= 0) {
                prepareAdjacentPage(settled - 1);
            }
        }
        if (mIndicatorShowing) {
            return;
        }
        mIndicatorShowing = true;
        if (mIndicator != null && getPreviewPageCount() > 1) {
            updatePageIndicator();
            mIndicator.setVisibility(VISIBLE);
            mIndicator.animate().cancel();
            mIndicator.animate()
                    .alpha(1f)
                    .setDuration(SCROLL_SHOW_DURATION_MS)
                    .setStartDelay(SCROLL_SHOW_DELAY_MS)
                    .setInterpolator(SCROLL_INDICATOR_PATH)
                    .start();
        }
        View name = getFolderName();
        if (name != null) {
            name.animate().cancel();
            name.animate()
                    .alpha(0f)
                    .setDuration(SCROLL_HIDE_DURATION_MS)
                    .setStartDelay(0)
                    .setInterpolator(SCROLL_INDICATOR_PATH)
                    .start();
        }
    }

    public void onFolderScrollPageEnd(int page) {
        mScrolling = false;
        mPreviewPage = page;
        mScrollDistance = page * getPageWidth();
        mAdjacentBoundPage = -1;
        if (mAdapter != null && mInfo != null) {
            mAdapter.setFolderInfo(mInfo);
            mAdapter.setList(mInfo.contents, page);
        }
        if (mListView != null) {
            mListView.setHighlightLayout(
                    HxyBigFolderPreviewModes.isHighlightPage(mInfo, page));
        }
        applyIdlePageLayout();
        // Sync layout so the first post-swipe frame isn't tiny unbound cells.
        ensureListLaidOut(mListView);
        rebindListChildren(mListView);
        updatePageIndicator();
        mLastIndicatorFrac = -1f;
        mHandler.removeCallbacks(mRestoreChromeRunnable);
        mHandler.postDelayed(mRestoreChromeRunnable, SCROLL_END_RESTORE_DELAY_MS);
    }

    private void ensureListLaidOut(HxyLargeFolderListView list) {
        if (list == null) {
            return;
        }
        int w = list.getWidth();
        int h = list.getHeight();
        if (w <= 0 || h <= 0) {
            w = getPreviewWidth();
            h = getPreviewHeight();
        }
        if (w <= 0 || h <= 0) {
            return;
        }
        int left = list.getLeft();
        int top = list.getTop();
        if (list.getWidth() <= 0) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) list.getLayoutParams();
            if (lp != null) {
                left = lp.leftMargin;
                top = lp.topMargin;
            }
        }
        list.measure(
                View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        list.layout(left, top, left + w, top + h);
    }

    private void rebindListChildren(HxyLargeFolderListView list) {
        if (list == null) {
            return;
        }
        int count = list.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = list.getChildAt(i);
            if (child instanceof HxyLargeFolderIconItem) {
                ((HxyLargeFolderIconItem) child).rebindIfNeeded();
            }
        }
    }

    private void forceRebindListChildren(HxyLargeFolderListView list) {
        if (list == null) {
            return;
        }
        int count = list.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = list.getChildAt(i);
            if (child instanceof HxyLargeFolderIconItem) {
                ((HxyLargeFolderIconItem) child).forceRebind();
            }
        }
    }

    private void restoreNameAfterScroll() {
        if (mScrolling) {
            return;
        }
        // Non-first pages: keep dots visible (ColorOS idle chrome), don't flash back to title.
        if (mPreviewPage > 0) {
            mIndicatorShowing = true;
            if (mIndicator != null) {
                mIndicator.animate().cancel();
                updatePageIndicator();
                mIndicator.setVisibility(VISIBLE);
                mIndicator.setAlpha(1f);
            }
            View name = getFolderName();
            if (name != null) {
                name.animate().cancel();
                name.setAlpha(0f);
            }
            return;
        }
        mIndicatorShowing = false;
        if (mIndicator != null) {
            mIndicator.animate().cancel();
            mIndicator.animate()
                    .alpha(0f)
                    .setDuration(SCROLL_HIDE_DURATION_MS)
                    .setStartDelay(0)
                    .withEndAction(() -> {
                        if (!mIndicatorShowing && mIndicator != null) {
                            mIndicator.setVisibility(INVISIBLE);
                        }
                    })
                    .start();
        }
        View name = getFolderName();
        if (name != null) {
            name.animate().cancel();
            name.animate()
                    .alpha(1f)
                    .setDuration(SCROLL_SHOW_DURATION_MS)
                    .setStartDelay(SCROLL_SHOW_DELAY_MS)
                    .setInterpolator(SCROLL_INDICATOR_PATH)
                    .start();
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