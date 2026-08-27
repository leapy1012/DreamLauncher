/**
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.settings;

import android.annotation.LayoutRes;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.COUIRecyclerView;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.R;
import com.coui.appcompat.toolbar.COUIToolbar;
import com.coui.appcompat.scrollview.COUINestedScrollView;
import com.google.android.material.appbar.COUIDividerAppBarLayout;

/** Base activity for Settings pages — MtkSettings DividerAppBar + COUIToolbar host. */
public class SettingsBaseActivity extends AppCompatActivity {

    /**
     * What type of page transition should be apply.
     */
    public static final String EXTRA_PAGE_TRANSITION_TYPE = "page_transition_type";

    protected static final boolean DEBUG_TIMING = false;
    private static final String TAG = "SettingsBaseActivity";
    protected COUIToolbar mToolbar;
    @Nullable
    protected COUIDividerAppBarLayout mDividerAppBar;

    /**
     * Status bar matches the page gray. Navigation bar is opaque page gray in 3-button
     * mode (content must not draw behind it) and transparent in gesture mode (content may).
     */
    static void applySettingsWindowColors(AppCompatActivity activity) {
        applySettingsWindowColors(activity, /* gestureNavigation */ false);
    }

    static void applySettingsWindowColors(AppCompatActivity activity, boolean gestureNavigation) {
        int pageColor = activity.getColor(R.color.coloros_surface_page);
        activity.getWindow().setStatusBarColor(pageColor);
        activity.getWindow().setNavigationBarColor(
                gestureNavigation ? Color.TRANSPARENT : pageColor);
        activity.getWindow().getDecorView().setBackgroundColor(pageColor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activity.getWindow().setStatusBarContrastEnforced(false);
            activity.getWindow().setNavigationBarContrastEnforced(false);
        }
        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(activity.getWindow(),
                        activity.getWindow().getDecorView());
        if (insetsController != null) {
            boolean light = activity.getResources().getBoolean(
                    R.bool.home_settings_light_status_bar);
            insetsController.setAppearanceLightStatusBars(light);
            insetsController.setAppearanceLightNavigationBars(light);
        }
    }

    /**
     * Gesture nav has a navigation-bar inset but no tappable bottom controls.
     * 3-button nav reports a tappable bottom inset for Back/Home/Recents.
     */
    public static boolean isGestureNavigation(@NonNull WindowInsetsCompat windowInsets) {
        Insets navBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
        Insets tappable = windowInsets.getInsets(WindowInsetsCompat.Type.tappableElement());
        return navBars.bottom > 0 && tappable.bottom == 0;
    }

    /** Pads the activity chrome for status/side bars; bottom only for 3-button nav. */
    static void bindContentParentInsets(@NonNull AppCompatActivity activity,
            @NonNull View contentParent) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(contentParent, (view, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            boolean gesture = isGestureNavigation(windowInsets);
            applySettingsWindowColors(activity, gesture);
            view.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    gesture ? 0 : systemBars.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(contentParent);
    }

    /**
     * Bottom inset for gesture nav. Keep {@code clipToPadding=false} so COUI list
     * overscroll / top gap behave like MtkSettings.
     */
    static void bindListNavigationInsets(@NonNull RecyclerView listView) {
        final int baseBottomPadding = listView.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(listView, (v, windowInsets) -> {
            boolean gesture = isGestureNavigation(windowInsets);
            Insets navInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
            int bottom = baseBottomPadding + (gesture ? navInsets.bottom : 0);
            if (v instanceof ViewGroup) {
                ((ViewGroup) v).setClipToPadding(false);
            }
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(listView);
    }

    /** Top gap under the app bar matching Home Settings / COUI category spacing. */
    static void applySettingsListTopGap(@NonNull RecyclerView listView) {
        int top = listView.getResources().getDimensionPixelSize(R.dimen.coloros_page_top_padding);
        listView.setClipToPadding(false);
        listView.setPadding(
                listView.getPaddingLeft(),
                top,
                listView.getPaddingRight(),
                listView.getPaddingBottom());
    }

    /** Top gap under the app bar for form-style scroll pages. */
    static void applySettingsScrollTopGap(@NonNull View scrollView) {
        int top = scrollView.getResources().getDimensionPixelSize(R.dimen.coloros_page_top_padding);
        if (scrollView instanceof ViewGroup) {
            ((ViewGroup) scrollView).setClipToPadding(false);
        }
        scrollView.setPadding(
                scrollView.getPaddingLeft(),
                top,
                scrollView.getPaddingRight(),
                scrollView.getPaddingBottom());
    }

    /** Bottom inset for gesture nav on scrollable form pages. */
    static void bindScrollNavigationInsets(@NonNull View scrollView) {
        final int baseBottomPadding = scrollView.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(scrollView, (v, windowInsets) -> {
            boolean gesture = isGestureNavigation(windowInsets);
            Insets navInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
            int bottom = baseBottomPadding + (gesture ? navInsets.bottom : 0);
            if (v instanceof ViewGroup) {
                ((ViewGroup) v).setClipToPadding(false);
            }
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(scrollView);
    }

    /** Full COUI scroll + divider setup for Theme / Launcher Style list pages. */
    protected void setupCouiScrollList(@NonNull RecyclerView list) {
        applySettingsListTopGap(list);
        bindCouiDividerAppBar(this, list);
        bindListNavigationInsets(list);
    }

    /** Full COUI scroll + divider setup for Icon Custom-style form pages. */
    protected void setupCouiScrollForm(@NonNull COUINestedScrollView scroll) {
        applySettingsScrollTopGap(scroll);
        bindCouiDividerNestedScroll(this, scroll);
        bindScrollNavigationInsets(scroll);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) {
            return;
        }

        super.setContentView(R.layout.settings_base_activity);
        applySettingsWindowColors(this);
        View contentParent = findViewById(R.id.content_parent);
        if (contentParent != null) {
            bindContentParentInsets(this, contentParent);
        }
        initDirectAppBar(this);
        mToolbar = findViewById(R.id.toolbar);
        mDividerAppBar = findViewById(R.id.abl);
        setupCouiToolbar(this, mToolbar);
    }

    /**
     * MtkSettings {@code initDirectAppBar}: use abl/toolbar, hide decor action_bar.
     */
    static void initDirectAppBar(@NonNull AppCompatActivity activity) {
        View abl = activity.findViewById(R.id.abl);
        if (abl != null) {
            abl.setBackgroundColor(activity.getColor(R.color.coloros_surface_page));
        }
        View decorToolbar = activity.findViewById(R.id.action_bar);
        if (decorToolbar != null) {
            decorToolbar.setVisibility(View.GONE);
        }
    }

    /**
     * Wires {@link COUIToolbar} as the support ActionBar with ColorOS title/back chrome.
     */
    static void setupCouiToolbar(@NonNull AppCompatActivity activity,
            @Nullable COUIToolbar toolbar) {
        if (toolbar == null) {
            return;
        }
        toolbar.setBackgroundColor(activity.getColor(R.color.coloros_surface_page));
        toolbar.setIsTitleCenterStyle(false);
        toolbar.setTitleTextAppearance(activity,
                com.coui.appcompat.R.style.textAppearanceSecondTitle);
        activity.setSupportActionBar(toolbar);
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setHomeAsUpIndicator(com.coui.appcompat.R.drawable.coui_back_arrow);
        }
        toolbar.setNavigationIcon(com.coui.appcompat.R.drawable.coui_back_arrow);
        toolbar.setTitleTextAppearance(activity,
                com.coui.appcompat.R.style.textAppearanceSecondTitle);
    }

    /**
     * MtkSettings {@code SettingsCouiScreenHost.attach}: bind list to
     * {@link COUIDividerAppBarLayout} and enable COUI spring overscroll.
     */
    static void bindCouiDividerAppBar(@NonNull AppCompatActivity activity,
            @Nullable RecyclerView list) {
        View abl = activity.findViewById(R.id.abl);
        if (!(abl instanceof COUIDividerAppBarLayout) || list == null) {
            return;
        }
        COUIDividerAppBarLayout dividerAppBar = (COUIDividerAppBarLayout) abl;
        dividerAppBar.bindRecyclerView(list);
        list.setNestedScrollingEnabled(true);
        list.setClipToPadding(false);
        if (list instanceof COUIRecyclerView) {
            COUIRecyclerView couiList = (COUIRecyclerView) list;
            couiList.setOverScrollEnable(true);
            couiList.setEnablePointerDownAction(false);
        }
    }

    /** Binds a NestedScrollView page to {@link COUIDividerAppBarLayout} (Icon Custom). */
    static void bindCouiDividerNestedScroll(@NonNull AppCompatActivity activity,
            @NonNull NestedScrollView scrollView) {
        View abl = activity.findViewById(R.id.abl);
        if (!(abl instanceof COUIDividerAppBarLayout)) {
            return;
        }
        COUIDividerAppBarLayout dividerAppBar = (COUIDividerAppBarLayout) abl;
        dividerAppBar.bindRecyclerView(null);
        scrollView.setClipToPadding(false);
        scrollView.setNestedScrollingEnabled(true);
        if (!(scrollView instanceof COUINestedScrollView)) {
            return;
        }
        NestedScrollView.OnScrollChangeListener listener =
                (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    dividerAppBar.refreshAppBar(v);
                    dividerAppBar.onDividerChanged();
                };
        scrollView.setOnScrollChangeListener(listener);
        dividerAppBar.refreshAppBar(scrollView);
        dividerAppBar.onDividerChanged();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        final ViewGroup parent = findViewById(R.id.content_frame);
        if (parent != null) {
            parent.removeAllViews();
        }
        LayoutInflater.from(this).inflate(layoutResID, parent);
    }

    @Override
    public void setContentView(View view) {
        ((ViewGroup) findViewById(R.id.content_frame)).addView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        ((ViewGroup) findViewById(R.id.content_frame)).addView(view, params);
    }

    /**
     * SubSetting page should show a toolbar by default. If the page wouldn't show a toolbar,
     * override this method and return false value.
     *
     * @return ture by default
     */
    protected boolean isToolbarEnabled() {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** @deprecated Use {@link #bindCouiDividerAppBar(AppCompatActivity, RecyclerView)}. */
    @Deprecated
    public void bindToolbarScrollDivider(@NonNull RecyclerView recyclerView) {
        bindCouiDividerAppBar(this, recyclerView);
    }

    /** @deprecated Use {@link #bindCouiDividerNestedScroll(AppCompatActivity, NestedScrollView)}. */
    @Deprecated
    public void bindNestedScrollDivider(@NonNull NestedScrollView scrollView) {
        bindCouiDividerNestedScroll(this, scrollView);
    }
}
