/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.allapps.search;

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.getSize;
import static android.view.View.MeasureSpec.makeMeasureSpec;

import static com.android.launcher3.Utilities.prefixTextWithIcon;
import static com.android.launcher3.icons.IconNormalizer.ICON_VISIBLE_AREA_FACTOR;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.method.TextKeyListener;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.ExtendedEditText;
import com.android.launcher3.Insettable;
import com.android.launcher3.R;
import com.android.launcher3.allapps.ActivityAllAppsContainerView;
import com.android.launcher3.allapps.AllAppsStore;
import com.android.launcher3.allapps.BaseAllAppsAdapter.AdapterItem;
import com.android.launcher3.allapps.SearchUiManager;
import com.android.launcher3.search.SearchCallback;
import com.android.launcher3.views.ActivityContext;

import java.util.ArrayList;

/**
 * Layout to contain the All-apps search UI.
 */
public class AppsSearchContainerLayout extends ExtendedEditText
        implements SearchUiManager, SearchCallback<AdapterItem>,
        AllAppsStore.OnUpdateListener, Insettable {

    private final ActivityContext mLauncher;
    private final AllAppsSearchBarController mSearchBarController;
    private final SpannableStringBuilder mSearchQueryBuilder;

    private ActivityAllAppsContainerView<?> mAppsView;

    // The amount of pixels to shift down and overlap with the rest of the content.
    private final int mContentOverlap;

    public AppsSearchContainerLayout(Context context) {
        this(context, null);
    }

    public AppsSearchContainerLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppsSearchContainerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mLauncher = ActivityContext.lookupContext(context);
        mSearchBarController = new AllAppsSearchBarController();

        mSearchQueryBuilder = new SpannableStringBuilder();
        Selection.setSelection(mSearchQueryBuilder, 0);
        if (getResources().getBoolean(R.bool.config_coloros_drawer)) {
            // Plain hint + tightly cropped compound icon. Never use prefixTextWithIcon —
            // that ImageSpan + drawableStart stacked two icons and left a huge gap.
            setHint(getResources().getString(R.string.coloros_all_apps_search_hint));
            applyColorOsSearchIcon();
        } else {
            setHint(prefixTextWithIcon(getContext(), R.drawable.ic_allapps_search, getHint()));
        }

        mContentOverlap =
                getResources().getDimensionPixelSize(R.dimen.all_apps_search_bar_content_overlap);
    }

    /**
     * Rasterize the search glyph and crop transparent padding so compound-drawable
     * width matches the visible glass (avoids a large icon→hint gap).
     */
    private void applyColorOsSearchIcon() {
        int iconSize = getResources().getDimensionPixelSize(
                R.dimen.coloros_all_apps_search_icon_size);
        Drawable raw = getResources().getDrawable(
                R.drawable.coloros_all_apps_search_icon, getContext().getTheme());
        if (raw == null) {
            return;
        }
        raw = raw.mutate();
        Bitmap full = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
        // Bitmap is already in device pixels — pin density so BitmapDrawable does not
        // scale it up again (that was leaving a large fake gap before the hint).
        int densityDpi = getResources().getDisplayMetrics().densityDpi;
        full.setDensity(densityDpi);
        Canvas canvas = new Canvas(full);
        raw.setBounds(0, 0, iconSize, iconSize);
        raw.draw(canvas);

        int left = iconSize;
        int top = iconSize;
        int right = -1;
        int bottom = -1;
        int[] pixels = new int[iconSize * iconSize];
        full.getPixels(pixels, 0, iconSize, 0, 0, iconSize, iconSize);
        for (int y = 0; y < iconSize; y++) {
            for (int x = 0; x < iconSize; x++) {
                if ((pixels[y * iconSize + x] >>> 24) > 16) {
                    if (x < left) left = x;
                    if (y < top) top = y;
                    if (x > right) right = x;
                    if (y > bottom) bottom = y;
                }
            }
        }
        BitmapDrawable icon;
        if (right >= left && bottom >= top) {
            left = Math.max(0, left - 1);
            top = Math.max(0, top - 1);
            right = Math.min(iconSize - 1, right + 1);
            bottom = Math.min(iconSize - 1, bottom + 1);
            Bitmap cropped = Bitmap.createBitmap(
                    full, left, top, right - left + 1, bottom - top + 1);
            cropped.setDensity(densityDpi);
            icon = new BitmapDrawable(getResources(), cropped);
        } else {
            icon = new BitmapDrawable(getResources(), full);
        }
        icon.setTargetDensity(densityDpi);
        setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null);
        setCompoundDrawablePadding(getResources().getDimensionPixelSize(
                R.dimen.coloros_all_apps_search_icon_text_gap));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAppsView.getAppsStore().addUpdateListener(this);
        if (getResources().getBoolean(R.bool.config_coloros_drawer)) {
            // Re-apply once WindowMetrics / root insets are available.
            post(this::refreshColorOsBottomMargin);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mAppsView.getAppsStore().removeUpdateListener(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // ColorOS: keep full width minus layout margins so the bar aligns with the
        // icon grid. AOSP shrink-to-hotseat-cell makes it too narrow vs Oppo.
        if (getResources().getBoolean(R.bool.config_coloros_drawer)) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        // Update the width to match the grid padding
        DeviceProfile dp = mLauncher.getDeviceProfile();
        int myRequestedWidth = getSize(widthMeasureSpec);
        int rowWidth = myRequestedWidth - mAppsView.getActiveRecyclerView().getPaddingLeft()
                - mAppsView.getActiveRecyclerView().getPaddingRight();

        int cellWidth = DeviceProfile.calculateCellWidth(rowWidth,
                dp.cellLayoutBorderSpacePx.x, dp.numShownHotseatIcons);
        int iconVisibleSize = Math.round(ICON_VISIBLE_AREA_FACTOR * dp.iconSizePx);
        int iconPadding = cellWidth - iconVisibleSize;

        int myWidth = rowWidth - iconPadding + getPaddingLeft() + getPaddingRight();
        super.onMeasure(makeMeasureSpec(myWidth, EXACTLY), heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        // Shift the widget horizontally so that its centered in the parent (b/63428078)
        View parent = (View) getParent();
        int availableWidth = parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight();
        int myWidth = right - left;
        int expectedLeft = parent.getPaddingLeft() + (availableWidth - myWidth) / 2;
        int shift = expectedLeft - left;
        setTranslationX(shift);

        // Top search bar overlaps the apps list; bottom ColorOS search must not —
        // this +24dp shove was landing the pill on the 3-button back ripple.
        if (!getResources().getBoolean(R.bool.config_coloros_drawer)) {
            offsetTopAndBottom(mContentOverlap);
        }
    }

    @Override
    public void initializeSearch(ActivityAllAppsContainerView<?> appsView) {
        mAppsView = appsView;
        mSearchBarController.initialize(
                new DefaultAppSearchAlgorithm(getContext(), true),
                this, mLauncher, this);
    }

    @Override
    public void onAppsUpdated() {
        mSearchBarController.refreshSearchResult();
    }

    @Override
    public void resetSearch() {
        mSearchBarController.reset();
    }

    @Override
    public void preDispatchKeyEvent(KeyEvent event) {
        // Determine if the key event was actual text, if so, focus the search bar and then dispatch
        // the key normally so that it can process this key event
        if (!mSearchBarController.isSearchFieldFocused() &&
                event.getAction() == KeyEvent.ACTION_DOWN) {
            final int unicodeChar = event.getUnicodeChar();
            final boolean isKeyNotWhitespace = unicodeChar > 0 &&
                    !Character.isWhitespace(unicodeChar) && !Character.isSpaceChar(unicodeChar);
            if (isKeyNotWhitespace) {
                boolean gotKey = TextKeyListener.getInstance().onKeyDown(this, mSearchQueryBuilder,
                        event.getKeyCode(), event);
                if (gotKey && mSearchQueryBuilder.length() > 0) {
                    mSearchBarController.focusSearchField();
                }
            }
        }
    }

    @Override
    public void onSearchResult(String query, ArrayList<AdapterItem> items) {
        if (items != null) {
            mAppsView.setSearchResults(items);
        }
    }

    @Override
    public void clearSearchResult() {
        // Clear the search query
        mSearchQueryBuilder.clear();
        mSearchQueryBuilder.clearSpans();
        Selection.setSelection(mSearchQueryBuilder, 0);
        mAppsView.onClearSearchResult();
    }

    @Override
    public void setInsets(Rect insets) {
        MarginLayoutParams mlp = (MarginLayoutParams) getLayoutParams();
        boolean bottomAligned = mlp instanceof RelativeLayout.LayoutParams
                && ((RelativeLayout.LayoutParams) mlp)
                        .getRule(RelativeLayout.ALIGN_PARENT_BOTTOM) == RelativeLayout.TRUE;
        if (bottomAligned) {
            mlp.topMargin = 0;
            if (getResources().getBoolean(R.bool.config_coloros_drawer)) {
                applyColorOsBottomMargin(mlp, insets != null ? insets.bottom : 0);
            } else {
                int insetBottom = Math.max(0, insets.bottom);
                int baseBottom = getResources().getDimensionPixelSize(
                        R.dimen.coloros_all_apps_search_margin_bottom);
                mlp.bottomMargin = baseBottom + insetBottom;
            }
        } else {
            mlp.topMargin = insets.top;
        }
        requestLayout();
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        if (getResources().getBoolean(R.bool.config_coloros_drawer)) {
            refreshColorOsBottomMargin();
        }
        return super.onApplyWindowInsets(insets);
    }

    private void refreshColorOsBottomMargin() {
        MarginLayoutParams mlp = (MarginLayoutParams) getLayoutParams();
        if (!(mlp instanceof RelativeLayout.LayoutParams)
                || ((RelativeLayout.LayoutParams) mlp)
                        .getRule(RelativeLayout.ALIGN_PARENT_BOTTOM) != RelativeLayout.TRUE) {
            return;
        }
        int before = mlp.bottomMargin;
        applyColorOsBottomMargin(mlp, 0);
        if (mlp.bottomMargin != before) {
            setLayoutParams(mlp);
            requestLayout();
        }
    }

    private void applyColorOsBottomMargin(MarginLayoutParams mlp, int reportedInsetBottom) {
        float density = getResources().getDisplayMetrics().density;
        // Nav bar height + Oppo-style gap so the back-button ripple stays below the pill.
        int nav = Math.max(0, reportedInsetBottom);
        nav = Math.max(nav, resolveNavigationBottomInset());
        nav = Math.max(nav, frameworkNavigationBarHeight());
        nav = Math.max(nav, estimateNavBarHeightFromDisplay());
        nav = Math.max(nav, Math.round(48f * density));
        int gap = getResources().getDimensionPixelSize(
                R.dimen.coloros_all_apps_search_margin_bottom);
        // XML dimen is the gap above the nav bar (not total from screen bottom).
        mlp.bottomMargin = nav + gap;
        setLayoutParams(mlp);
    }

    private int resolveNavigationBottomInset() {
        int bottom = 0;
        WindowInsets wi = getRootWindowInsets();
        if (wi != null) {
            bottom = Math.max(bottom, wi.getInsets(WindowInsets.Type.navigationBars()).bottom);
            bottom = Math.max(bottom, wi.getInsets(WindowInsets.Type.tappableElement()).bottom);
        }
        try {
            WindowManager wm = getContext().getSystemService(WindowManager.class);
            if (wm != null) {
                WindowInsets metricsInsets = wm.getCurrentWindowMetrics().getWindowInsets();
                bottom = Math.max(bottom,
                        metricsInsets.getInsets(WindowInsets.Type.navigationBars()).bottom);
                bottom = Math.max(bottom,
                        metricsInsets.getInsets(WindowInsets.Type.tappableElement()).bottom);
            }
        } catch (RuntimeException ignored) {
            // Fall through.
        }
        return bottom;
    }

    private int frameworkNavigationBarHeight() {
        int resId = getResources().getIdentifier(
                "navigation_bar_height", "dimen", "android");
        return resId > 0 ? getResources().getDimensionPixelSize(resId) : 0;
    }

    /**
     * Last-resort nav height from the stable display frame (matches dumpsys
     * {@code navigationBars frame} bottom inset when WindowInsets is late).
     */
    private int estimateNavBarHeightFromDisplay() {
        try {
            WindowManager wm = getContext().getSystemService(WindowManager.class);
            if (wm == null) {
                return 0;
            }
            android.view.WindowMetrics metrics = wm.getCurrentWindowMetrics();
            android.graphics.Rect bounds = metrics.getBounds();
            WindowInsets wi = metrics.getWindowInsets();
            android.graphics.Insets nav = wi.getInsets(WindowInsets.Type.navigationBars());
            if (nav.bottom > 0) {
                return nav.bottom;
            }
            // Some builds report 0 insets while the bar still occupies the
            // bottom ~48dp of the screen.
            int approx = Math.round(48f * getResources().getDisplayMetrics().density);
            return Math.min(approx, Math.max(0, bounds.height() / 12));
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    @Override
    public ExtendedEditText getEditText() {
        return this;
    }
}
