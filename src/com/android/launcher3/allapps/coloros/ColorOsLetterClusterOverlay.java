package com.android.launcher3.allapps.coloros;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.R;
import com.android.launcher3.allapps.ActivityAllAppsContainerView;
import com.android.launcher3.allapps.AlphabeticalAppsList;
import com.android.launcher3.allapps.BaseAllAppsAdapter;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.touch.ItemClickHandler;
import com.android.launcher3.touch.ItemLongClickListener;
import com.coui.appcompat.animation.dynamicanimation.COUIDynamicAnimation;
import com.coui.appcompat.animation.dynamicanimation.COUISpringAnimation;
import com.coui.appcompat.animation.dynamicanimation.COUISpringForce;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Oppo {@code OplusClusterLayout} / {@code OplusClusterIconLayout} port:
 * absolute layout for icon cells ({@code layoutSingleRaw}) and section letter
 * ({@code ClusterLayoutData.refreshSectionLayout}), plus
 * {@code ClusterLayoutScaleAnimator} / {@code ClusterSwitchAlphaAnimator} springs.
 */
final class ColorOsLetterClusterOverlay {

    private static final float LAYOUT_REF_WIDTH_DP = 360f;
    private static final float RAW_4_WIDTH_DP = 258f;
    private static final float RAW_5_WIDTH_DP = 280f;
    private static final float MARGIN_LEFT_RAW4_DP = 74f;
    private static final float MARGIN_LEFT_RAW5_DP = 52f;
    private static final float ITEM_HEIGHT_DP = 84f;
    private static final float SECTION_MARGIN_LEFT_DP = 295f;
    private static final float SECTION_WIDTH_DP = 37f;
    private static final float SECTION_HEIGHT_DP = 40f;
    private static final float SECTION_MAX_HEIGHT_DP = 56f;
    private static final float LAYOUT_BEYOND_MARGIN_DP = 5f;
    private static final float MIN_MARGIN_TOP_DP = 120f;
    private static final float SECTION_ALIGN_NUDGE_DP = 15f;
    private static final float BOTTOM_MARGIN_DP = 80f;

    /** Oppo {@code ClusterLayoutScaleAnimator}. */
    private static final float ZOOM_BOUNCE = 0.35f;
    private static final float ZOOM_RESPONSE = 0.4f;
    private static final float ZOOM_START_SCALE = 0.9f;
    private static final float ZOOM_END_SCALE = 1.0f;
    private static final float ZOOM_MIN_VISIBLE = 0.002f;
    /** Oppo {@code ClusterSwitchAlphaAnimator} show response. */
    private static final float ALPHA_SHOW_RESPONSE = 0.2f;
    private static final float ALPHA_MIN_VISIBLE = 0.001f;

    interface DismissListener {
        void onClusterDismissed();
    }

    private final ActivityAllAppsContainerView<?> mContainer;
    private final ClusterRoot mRoot;
    private final TextView mSectionLetter;
    private final ClusterIconPanel mIconPanel;
    private boolean mShowing;
    private String mSection = "";
    private int mLetterCenterY;

    @Nullable private View mTabHeader;
    @Nullable private View mSearchView;
    @Nullable private DismissListener mDismissListener;

    private float mDownRawX;
    private float mDownRawY;
    private boolean mDownOnIcon;
    private final int mTouchSlop;
    private final Rect mTmpRect = new Rect();
    private final List<AppInfo> mApps = new ArrayList<>();

    @Nullable private COUISpringAnimation mZoomX;
    @Nullable private COUISpringAnimation mZoomY;
    @Nullable private COUISpringAnimation mAlphaShow;

    ColorOsLetterClusterOverlay(ActivityAllAppsContainerView<?> container) {
        mContainer = container;
        Context context = container.getContext();
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        mRoot = new ClusterRoot(context);
        mRoot.setVisibility(View.GONE);
        mRoot.setClickable(true);
        mRoot.setFocusable(true);

        mSectionLetter = new TextView(context);
        mSectionLetter.setTextColor(0xFFFFFFFF);
        mSectionLetter.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        mSectionLetter.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
        mSectionLetter.setGravity(android.view.Gravity.CENTER);
        mSectionLetter.setIncludeFontPadding(false);
        mSectionLetter.setClickable(false);
        mSectionLetter.setFocusable(false);
        mRoot.addView(mSectionLetter);

        mIconPanel = new ClusterIconPanel(context);
        mRoot.addView(mIconPanel);

        android.widget.RelativeLayout.LayoutParams rootLp =
                new android.widget.RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
        container.addView(mRoot, rootLp);
    }

    void bindChrome(@Nullable View tabHeader, @Nullable View searchView) {
        mTabHeader = tabHeader;
        mSearchView = searchView;
    }

    void setDismissListener(@Nullable DismissListener listener) {
        mDismissListener = listener;
    }

    boolean isShowing() {
        return mShowing;
    }

    @Nullable
    String getSection() {
        return mShowing ? mSection : null;
    }

    void showSection(String section, List<AppInfo> apps, int letterCenterYInOverlay) {
        if (section == null || section.isEmpty()) {
            return;
        }
        boolean entering = !mShowing;
        boolean sectionChanged = !section.equals(mSection);
        mSection = section;
        mLetterCenterY = letterCenterYInOverlay;
        mSectionLetter.setText(section);

        boolean appsChanged = !appsEqual(mApps, apps);
        if (appsChanged) {
            mApps.clear();
            mApps.addAll(apps);
            mIconPanel.bind(apps, resolveColumns());
        }

        if (entering) {
            mShowing = true;
            setChromeVisible(false);
            setDrawerContentVisible(false);
            mRoot.setVisibility(View.VISIBLE);
            mRoot.setAlpha(0f);
            startAlphaShow();
        }
        mRoot.bringToFront();
        mRoot.requestLayout();

        // Oppo refreshUI → showClusterWithAnima on enter and every section refresh.
        if (entering || appsChanged || sectionChanged) {
            mRoot.post(this::startClusterZoomIn);
        }
    }

    void updateLetterY(int letterCenterYInOverlay) {
        if (!mShowing) {
            return;
        }
        mLetterCenterY = letterCenterYInOverlay;
        mRoot.requestLayout();
    }

    void dismiss() {
        if (!mShowing) {
            return;
        }
        mShowing = false;
        mSection = "";
        mDownOnIcon = false;
        mApps.clear();
        cancelClusterAnims();
        mRoot.setVisibility(View.GONE);
        mRoot.setAlpha(1f);
        mIconPanel.setScaleX(1f);
        mIconPanel.setScaleY(1f);
        mIconPanel.clear();
        setDrawerContentVisible(true);
        setChromeVisible(true);
        if (mDismissListener != null) {
            mDismissListener.onClusterDismissed();
        }
    }

    /** Oppo {@code ClusterLayoutScaleAnimator.zoomInView} + pivot on end edge. */
    private void startClusterZoomIn() {
        if (!mShowing) {
            return;
        }
        cancelZoomAnims();
        int w = mIconPanel.getWidth();
        if (w <= 0) {
            mRoot.post(this::startClusterZoomIn);
            return;
        }
        boolean rtl = mRoot.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        mIconPanel.setPivotX(rtl ? 0f : w);
        mIconPanel.setPivotY(mIconPanel.getHeight() / 2f);
        mIconPanel.setScaleX(ZOOM_START_SCALE);
        mIconPanel.setScaleY(ZOOM_START_SCALE);

        COUISpringForce force = new COUISpringForce(ZOOM_END_SCALE);
        force.setBounce(ZOOM_BOUNCE);
        force.setResponse(ZOOM_RESPONSE);

        mZoomX = new COUISpringAnimation(mIconPanel, COUIDynamicAnimation.SCALE_X, ZOOM_END_SCALE);
        mZoomX.setStartValue(ZOOM_START_SCALE);
        mZoomX.setSpring(force);
        mZoomX.setMinimumVisibleChange(ZOOM_MIN_VISIBLE);

        mZoomY = new COUISpringAnimation(mIconPanel, COUIDynamicAnimation.SCALE_Y, ZOOM_END_SCALE);
        mZoomY.setStartValue(ZOOM_START_SCALE);
        mZoomY.setSpring(force);
        mZoomY.setMinimumVisibleChange(ZOOM_MIN_VISIBLE);

        mZoomX.start();
        mZoomY.start();
    }

    /** Oppo {@code ClusterSwitchAlphaAnimator.alphaShow}. */
    private void startAlphaShow() {
        if (mAlphaShow != null) {
            mAlphaShow.cancel();
            mAlphaShow = null;
        }
        COUISpringForce force = new COUISpringForce(1f);
        force.setBounce(0f);
        force.setResponse(ALPHA_SHOW_RESPONSE);
        mAlphaShow = new COUISpringAnimation(mRoot, COUIDynamicAnimation.ALPHA, 1f);
        mAlphaShow.setStartValue(0f);
        mAlphaShow.setSpring(force);
        mAlphaShow.setMinimumVisibleChange(ALPHA_MIN_VISIBLE);
        mAlphaShow.start();
    }

    private void cancelZoomAnims() {
        if (mZoomX != null) {
            mZoomX.cancel();
            mZoomX = null;
        }
        if (mZoomY != null) {
            mZoomY.cancel();
            mZoomY = null;
        }
    }

    private void cancelClusterAnims() {
        cancelZoomAnims();
        if (mAlphaShow != null) {
            mAlphaShow.cancel();
            mAlphaShow = null;
        }
        mRoot.animate().cancel();
    }

    private boolean hitIcon(float rawX, float rawY) {
        return mIconPanel.hitIcon(rawX, rawY, mTmpRect);
    }

    private void setChromeVisible(boolean visible) {
        float alpha = visible ? 1f : 0f;
        int vis = visible ? View.VISIBLE : View.INVISIBLE;
        if (mTabHeader != null) {
            mTabHeader.animate().cancel();
            mTabHeader.setAlpha(alpha);
            mTabHeader.setVisibility(vis);
            mTabHeader.setEnabled(visible);
        }
        if (mSearchView != null) {
            mSearchView.animate().cancel();
            mSearchView.setAlpha(alpha);
            mSearchView.setVisibility(vis);
            mSearchView.setEnabled(visible);
        }
    }

    private void setDrawerContentVisible(boolean visible) {
        float alpha = visible ? 1f : 0f;
        ViewGroup pager = mContainer.getAppsRecyclerViewContainer();
        if (pager != null) {
            pager.setAlpha(alpha);
            pager.setEnabled(visible);
        }
        View appsList = mContainer.findViewById(R.id.apps_list_view);
        if (appsList != null && appsList != pager) {
            appsList.setAlpha(alpha);
            appsList.setEnabled(visible);
        }
    }

    private int resolveColumns() {
        int cols = ColorOsDrawerColumns.get(mContainer.getContext());
        return cols == ColorOsDrawerColumns.COLUMNS_FOUR
                ? ColorOsDrawerColumns.COLUMNS_FOUR
                : ColorOsDrawerColumns.COLUMNS_FIVE;
    }

    private int layoutPx(float designDp) {
        int width = mRoot.getWidth();
        if (width <= 0) {
            width = mContainer.getWidth();
        }
        if (width <= 0) {
            width = mContainer.getResources().getDisplayMetrics().widthPixels;
        }
        return Math.round(designDp * (width / LAYOUT_REF_WIDTH_DP));
    }

    private static boolean appsEqual(List<AppInfo> a, List<AppInfo> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i) != b.get(i)) {
                return false;
            }
        }
        return true;
    }

    static List<AppInfo> appsForSection(AlphabeticalAppsList<?> list, String section) {
        ArrayList<AppInfo> out = new ArrayList<>();
        if (list == null || section == null || section.isEmpty()) {
            return out;
        }
        String target = section.toUpperCase(Locale.US);
        for (BaseAllAppsAdapter.AdapterItem item : list.getAdapterItems()) {
            if (item == null || item.itemInfo == null) {
                continue;
            }
            AppInfo info = item.itemInfo;
            char c = sectionHead(info);
            if (c == 0) {
                continue;
            }
            if ("#".equals(target)) {
                if (!Character.isLetter(c)) {
                    out.add(info);
                }
            } else if (c == target.charAt(0)) {
                out.add(info);
            }
        }
        return out;
    }

    /** Prefer {@link AppInfo#sectionName}; fall back to title first char. */
    private static char sectionHead(AppInfo info) {
        String sn = info.sectionName;
        if (sn != null && !sn.isEmpty()) {
            return Character.toUpperCase(sn.charAt(0));
        }
        CharSequence title = info.title;
        if (title != null && title.length() > 0) {
            return Character.toUpperCase(title.charAt(0));
        }
        return 0;
    }

    /** Oppo {@code OplusClusterLayout}: owns absolute placement of letter + icon panel. */
    private final class ClusterRoot extends ViewGroup {
        ClusterRoot(Context context) {
            super(context);
            setClipChildren(false);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent ev) {
            if (!mShowing) {
                return super.dispatchTouchEvent(ev);
            }
            int action = ev.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                mDownRawX = ev.getRawX();
                mDownRawY = ev.getRawY();
                mDownOnIcon = hitIcon(ev.getRawX(), ev.getRawY());
                if (mDownOnIcon) {
                    return super.dispatchTouchEvent(ev);
                }
                return true;
            }
            if (mDownOnIcon) {
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    mDownOnIcon = false;
                }
                return super.dispatchTouchEvent(ev);
            }
            if (action == MotionEvent.ACTION_UP) {
                float dx = Math.abs(ev.getRawX() - mDownRawX);
                float dy = Math.abs(ev.getRawY() - mDownRawY);
                if (dx < mTouchSlop && dy < mTouchSlop) {
                    dismiss();
                }
                return true;
            }
            return action == MotionEvent.ACTION_MOVE
                    || action == MotionEvent.ACTION_CANCEL;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int w = MeasureSpec.getSize(widthMeasureSpec);
            int h = MeasureSpec.getSize(heightMeasureSpec);
            setMeasuredDimension(w, h);
            // Children measured in onLayout after geometry is known.
            mIconPanel.prepareMetrics(resolveColumns());
            int cols = resolveColumns();
            int panelW = layoutPx(cols == 5 ? RAW_5_WIDTH_DP : RAW_4_WIDTH_DP);
            int beyond = layoutPx(LAYOUT_BEYOND_MARGIN_DP);
            int itemH = layoutPx(ITEM_HEIGHT_DP);
            int rows = Math.max(1, mIconPanel.getRowCount());
            mIconPanel.setLayoutMetrics(panelW, itemH, cols, beyond);
            mIconPanel.measure(
                    MeasureSpec.makeMeasureSpec(panelW + beyond, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(rows * itemH + beyond, MeasureSpec.EXACTLY));
            mSectionLetter.measure(
                    MeasureSpec.makeMeasureSpec(layoutPx(SECTION_WIDTH_DP), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(layoutPx(SECTION_HEIGHT_DP), MeasureSpec.EXACTLY));
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            if (!mShowing) {
                return;
            }
            int cols = resolveColumns();
            int panelW = layoutPx(cols == 5 ? RAW_5_WIDTH_DP : RAW_4_WIDTH_DP);
            int offsetX = layoutPx(cols == 5 ? MARGIN_LEFT_RAW5_DP : MARGIN_LEFT_RAW4_DP);
            int itemH = layoutPx(ITEM_HEIGHT_DP);
            int beyond = layoutPx(LAYOUT_BEYOND_MARGIN_DP);
            int sectionMaxH = layoutPx(SECTION_MAX_HEIGHT_DP);
            int sectionW = layoutPx(SECTION_WIDTH_DP);
            int sectionH = layoutPx(SECTION_HEIGHT_DP);
            int sectionLeft = layoutPx(SECTION_MARGIN_LEFT_DP);
            int nudge = layoutPx(SECTION_ALIGN_NUDGE_DP);
            int minTop = layoutPx(MIN_MARGIN_TOP_DP);
            int bottomMargin = layoutPx(BOTTOM_MARGIN_DP);

            int rows = Math.max(1, mIconPanel.getRowCount());
            int iconsH = rows * itemH;
            int layoutH = b - t;
            int sectionMid = mLetterCenterY;

            // Oppo refreshOffsetY
            int offsetY = sectionMid - (itemH - nudge);
            if (offsetY + iconsH > layoutH - bottomMargin) {
                offsetY = Math.max(
                        minTop + sectionMaxH,
                        offsetY - ((offsetY + iconsH) - (layoutH - bottomMargin)));
            }
            offsetY = Math.max(offsetY, minTop + sectionMaxH);

            // Oppo layoutIconView:
            // Rect(offsetX, offsetY - beyond, offsetX + panelW + beyond, offsetY + iconsH)
            int iconLeft = offsetX;
            int iconTop = offsetY - beyond;
            int iconRight = offsetX + panelW + beyond;
            int iconBottom = offsetY + iconsH;
            mIconPanel.setLayoutMetrics(panelW, itemH, cols, beyond);
            mIconPanel.layout(iconLeft, iconTop, iconRight, iconBottom);

            // Oppo layoutSection / refreshSectionLayout — letter ABOVE icons
            int letterTop = offsetY - sectionMaxH;
            mSectionLetter.layout(
                    sectionLeft, letterTop, sectionLeft + sectionW, letterTop + sectionH);
        }
    }

    /** Oppo {@code OplusClusterIconLayout}. */
    private final class ClusterIconPanel extends ViewGroup {
        private int mCols = 5;
        private int mRowCount;
        private int mPanelW;
        private int mItemH;
        private int mBeyond;

        ClusterIconPanel(Context context) {
            super(context);
            setClipChildren(false);
            setClipToPadding(false);
        }

        void clear() {
            removeAllViews();
            mRowCount = 0;
        }

        void bind(List<AppInfo> apps, int cols) {
            removeAllViews();
            mCols = cols;
            mRowCount = apps.isEmpty() ? 0 : (apps.size() + cols - 1) / cols;
            LayoutInflater inflater = LayoutInflater.from(getContext());
            for (AppInfo info : apps) {
                BubbleTextView icon = (BubbleTextView) inflater.inflate(
                        R.layout.all_apps_icon, this, false);
                icon.setOnClickListener(ItemClickHandler.INSTANCE);
                icon.setOnLongClickListener(ItemLongClickListener.INSTANCE_ALL_APPS);
                icon.applyFromApplicationInfo(info);
                addView(icon);
            }
        }

        void prepareMetrics(int cols) {
            mCols = cols;
            int n = getChildCount();
            mRowCount = n == 0 ? 0 : (n + cols - 1) / cols;
        }

        void setLayoutMetrics(int panelW, int itemH, int cols, int beyond) {
            mPanelW = panelW;
            mItemH = itemH;
            mCols = cols;
            mBeyond = beyond;
        }

        int getRowCount() {
            return mRowCount;
        }

        boolean hitIcon(float rawX, float rawY, Rect tmp) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (child.getVisibility() != VISIBLE) {
                    continue;
                }
                child.getGlobalVisibleRect(tmp);
                if (tmp.contains(Math.round(rawX), Math.round(rawY))) {
                    return true;
                }
            }
            return false;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int w = MeasureSpec.getSize(widthMeasureSpec);
            int h = MeasureSpec.getSize(heightMeasureSpec);
            setMeasuredDimension(w, h);
            if (mCols <= 0) {
                return;
            }
            int panelW = mPanelW > 0 ? mPanelW : Math.max(mCols, w - mBeyond);
            int cellW = panelW / mCols;
            int cellH = mItemH > 0 ? mItemH : layoutPx(ITEM_HEIGHT_DP);
            int childW = MeasureSpec.makeMeasureSpec(cellW, MeasureSpec.EXACTLY);
            int childH = MeasureSpec.makeMeasureSpec(cellH, MeasureSpec.EXACTLY);
            for (int i = 0; i < getChildCount(); i++) {
                getChildAt(i).measure(childW, childH);
            }
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            int count = getChildCount();
            if (count == 0 || mCols <= 0) {
                return;
            }
            int panelW = mPanelW > 0 ? mPanelW : Math.max(mCols, (r - l) - mBeyond);
            int cellW = panelW / mCols;
            int itemH = mItemH > 0 ? mItemH : layoutPx(ITEM_HEIGHT_DP);
            int beyond = mBeyond > 0 ? mBeyond : layoutPx(LAYOUT_BEYOND_MARGIN_DP);
            int index = 0;
            int row = 0;
            while (index < count) {
                int inRow = Math.min(mCols, count - index);
                // Oppo layoutSingleRaw LTR: pad (cols - inRow) * itemWidth → right-align.
                int startX = (mCols - inRow) * cellW;
                int offsetY = row * itemH + beyond;
                for (int c = 0; c < inRow; c++) {
                    View child = getChildAt(index + c);
                    int left = startX + c * cellW;
                    int top = offsetY;
                    child.layout(left, top, left + cellW, top + itemH);
                }
                index += inRow;
                row++;
            }
            mRowCount = row;
        }
    }
}
