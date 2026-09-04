package com.android.launcher3.allapps.coloros;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.customize.overlay.controller.CategoryController;
import com.android.customize.overlay.model.CategoryInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.allapps.ActivityAllAppsContainerView;
import com.android.launcher3.allapps.AllAppsRecyclerView;
import com.android.launcher3.allapps.AlphabeticalAppsList;
import com.android.launcher3.allapps.AlphabeticalAppsList.FastScrollSectionInfo;
import com.android.launcher3.allapps.BaseAllAppsAdapter;
import com.android.launcher3.allapps.search.AppsSearchContainerLayout;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.settings.SettingsActivity;
import com.android.launcher3.views.RecyclerViewFastScroller;
import com.coui.appcompat.poplist.COUIPopupListWindow;
import com.coui.appcompat.poplist.PopupListItem;
import com.coui.appcompat.segmentbutton.COUISegmentButtonLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ColorOS drawer chrome: COUI segment tabs + reliable letter rail + overflow menu.
 */
public final class ColorOsDrawerChrome {

    private static final int MENU_ID_SELECT = 0;
    private static final int MENU_ID_SORT = 1;
    private static final int MENU_ID_SETTINGS = 2;

    private final ActivityAllAppsContainerView<?> mContainer;
    private final Launcher mLauncher;

    private View mTabHeader;
    private COUISegmentButtonLayout mSegment;
    private ColorOsLetterRail mLetterIndex;
    private ColorOsLetterClusterOverlay mLetterCluster;
    private RecyclerView mCategoryList;
    private boolean mShowingCategories;
    /** Section shown in the last cluster filter; used to land the list on dismiss. */
    @Nullable private String mLastClusterSection;
    private final Rect mInsets = new Rect();

    private COUIPopupListWindow mPopupWindow;
    private final ArrayList<PopupListItem> mPopupItems = new ArrayList<>();

    private final CategoryController mCategoryController = new CategoryController();

    public ColorOsDrawerChrome(ActivityAllAppsContainerView<?> container) {
        mContainer = container;
        mLauncher = Launcher.cast(Launcher.getLauncher(container.getContext()));
    }

    public static boolean isEnabled(Context context) {
        return context.getResources().getBoolean(R.bool.config_coloros_drawer);
    }

    public void attach() {
        LayoutInflater inflater = LayoutInflater.from(mContainer.getContext());

        RecyclerViewFastScroller scroller = mContainer.findViewById(R.id.fast_scroller);
        if (scroller != null) {
            scroller.setVisibility(View.GONE);
        }
        View popup = mContainer.findViewById(R.id.fast_scroller_popup);
        if (popup != null) {
            popup.setVisibility(View.GONE);
        }
        View header = mContainer.findViewById(R.id.all_apps_header);
        if (header != null) {
            header.setVisibility(View.GONE);
        }

        mTabHeader = inflater.inflate(R.layout.coloros_all_apps_category_tab_header, mContainer, false);
        RelativeLayout.LayoutParams tabLp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        tabLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        mContainer.addView(mTabHeader, tabLp);
        // Apply status-bar clearance immediately so a later letter-index failure
        // cannot leave tabs stuck under the system icons.
        applyTabTopInset();

        mSegment = mTabHeader.findViewById(R.id.coloros_segment_group);
        ImageView menu = mTabHeader.findViewById(R.id.coloros_all_apps_menu);
        String all = mContainer.getResources().getString(R.string.coloros_floating_tab_all);
        String categories = mContainer.getResources().getString(R.string.coloros_floating_tab_category);
        mSegment.setSegmentButtons(new String[]{all, categories});
        mSegment.setSegmentSelectedTextColor(0xFF000000);
        mSegment.setSegmentUnselectedTextColor(
                mContainer.getResources().getColor(R.color.coloros_all_apps_text, null));
        // Oppo: translucent dark track + solid white selected pill (COUI light-theme
        // track is nearly invisible on the drawer scrim).
        final int trackColor = mContainer.getResources()
                .getColor(R.color.coloros_all_apps_segment_track, null);
        final int selectedColor = mContainer.getResources()
                .getColor(R.color.coloros_all_apps_segment_selected, null);
        mSegment.setSegmentButtonDrawDelegate(
                new COUISegmentButtonLayout.SegmentButtonDrawDelegate() {
                    @Override
                    public Paint[] getCustomBackgroundPaint() {
                        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                        p.setColor(trackColor);
                        return new Paint[]{p};
                    }

                    @Override
                    public Paint[] getCustomIndicatorPaint() {
                        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                        p.setColor(selectedColor);
                        return new Paint[]{p};
                    }
                });
        mSegment.selectSegmentAt(0);
        mSegment.setOnSelectedSegmentChangeListener((from, to, progress) -> {
            if (to == 1) {
                if (!mShowingCategories) {
                    showCategoriesPage();
                }
            } else if (mShowingCategories) {
                showAllPage();
            }
        });
        menu.setOnClickListener(this::showOverflowMenu);
        menu.setImageResource(R.drawable.ic_overflow_menu);
        menu.clearColorFilter();

        mLetterCluster = new ColorOsLetterClusterOverlay(mContainer);
        mLetterCluster.bindChrome(mTabHeader, mContainer.getSearchView());
        mLetterCluster.setDismissListener(this::onLetterClusterDismissed);

        mLetterIndex = new ColorOsLetterRail(mContainer.getContext());
        mLetterIndex.setId(R.id.coloros_letter_index);
        RelativeLayout.LayoutParams letterLp = new RelativeLayout.LayoutParams(
                mContainer.getResources().getDimensionPixelSize(
                        R.dimen.coloros_all_apps_index_width),
                dp(220));
        letterLp.addRule(RelativeLayout.ALIGN_PARENT_END);
        letterLp.addRule(RelativeLayout.BELOW, R.id.coloros_category_tab_header);
        letterLp.setMarginEnd(dp(2));
        mContainer.addView(mLetterIndex, letterLp);
        mLetterIndex.setElevation(dp(8));
        mLetterIndex.setListener(new ColorOsLetterRail.Listener() {
            @Override
            public void onLetterScrubStart() {
                AllAppsRecyclerView rv = resolveAppsRecyclerView();
                if (rv != null) {
                    rv.stopScroll();
                }
            }

            @Override
            public void onLetter(String letter, int centerYInRail) {
                if (mLetterCluster != null && mLetterCluster.isShowing()
                        && letter.equals(mLetterCluster.getSection())) {
                    updateLetterClusterY(centerYInRail);
                    return;
                }
                showLetterCluster(letter, centerYInRail);
                // Keep list scrolled under the cluster so dismiss lands on the section.
                jumpToLetterImmediate(letter);
            }
        });
        attachLetterRailScrollSync();
        updateLetterRailVisibility();
        refreshLetterRailSections();
        // hasValue must track model updates — early refresh with an empty list
        // previously stamped every letter as empty and killed scrub entirely.
        mContainer.getAppsStore().addUpdateListener(this::refreshLetterRailSections);
        mLetterIndex.post(this::refreshLetterRailSections);

        mCategoryList = new RecyclerView(mContainer.getContext());
        mCategoryList.setLayoutManager(new GridLayoutManager(mContainer.getContext(), 2));
        mCategoryList.setAdapter(new CategoryCardAdapter());
        mCategoryList.setVisibility(View.GONE);
        mCategoryList.setClipToPadding(false);
        mCategoryList.setPadding(dp(12), dp(8), dp(12), dp(16));
        RelativeLayout.LayoutParams catLp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        catLp.addRule(RelativeLayout.ABOVE, R.id.search_container_all_apps);
        catLp.addRule(RelativeLayout.BELOW, R.id.coloros_category_tab_header);
        mContainer.addView(mCategoryList, catLp);

        applyDrawerColumns();
        setInsets(mLauncher.getDeviceProfile().getInsets());
        showAllPage();
        mTabHeader.bringToFront();
        mLetterIndex.bringToFront();
        menu.bringToFront();
    }

    public void setInsets(Rect insets) {
        if (insets != null) {
            mInsets.set(insets);
        }
        reapplyContentLayout();
    }

    /**
     * Re-pin apps list under the tab header and refresh search / letter chrome.
     * Safe to call after {@code setupHeader()} / {@code rebindAdapters()}.
     */
    public void reapplyContentLayout() {
        if (mTabHeader == null) {
            return;
        }
        applyTabTopInset();
        applySearchBottomInset();
        mContainer.layoutColorOsAppsBelowTabs();
        // After tab measures, re-pin once more with the real height.
        mTabHeader.post(mContainer::layoutColorOsAppsBelowTabs);

        int listTop = mContainer.getResources().getDimensionPixelSize(
                R.dimen.coloros_all_apps_content_below_tabs);
        int listBottom = mContainer.getResources().getDimensionPixelSize(
                R.dimen.coloros_all_apps_list_bottom_gap);
        mContainer.applyColorOsListPadding(listTop, listBottom);

        if (mLetterIndex != null) {
            if (mLetterCluster != null) {
                mLetterCluster.bindChrome(mTabHeader, mContainer.getSearchView());
            }
            updateLetterRailVisibility();
            refreshLetterRailSections();
            mLetterIndex.bringToFront();
        }
        mTabHeader.bringToFront();
        View menu = mTabHeader.findViewById(R.id.coloros_all_apps_menu);
        if (menu != null) {
            menu.setVisibility(View.VISIBLE);
            menu.bringToFront();
        }
    }

    private void applyTabTopInset() {
        if (mTabHeader == null) {
            return;
        }
        int statusTop = resolveStatusTop();
        int gap = mContainer.getResources().getDimensionPixelSize(
                R.dimen.coloros_all_apps_tab_gap_below_status);
        RelativeLayout.LayoutParams tabLp =
                (RelativeLayout.LayoutParams) mTabHeader.getLayoutParams();
        if (tabLp == null) {
            return;
        }
        tabLp.topMargin = statusTop + gap;
        tabLp.height = dp(52);
        mTabHeader.setLayoutParams(tabLp);
        mTabHeader.setPadding(0, 0, 0, 0);
    }

    private void applySearchBottomInset() {
        View search = mContainer.getSearchView();
        if (search instanceof AppsSearchContainerLayout) {
            AppsSearchContainerLayout searchView = (AppsSearchContainerLayout) search;
            Runnable apply = () -> {
                Rect r = new Rect(mInsets);
                r.bottom = resolveNavBottom();
                searchView.setInsets(r);
            };
            apply.run();
            // Insets can be 0 during first attach; refresh once the window is ready.
            searchView.post(apply);
        }
    }

    private int resolveStatusTop() {
        int top = Math.max(0, mInsets.top);
        WindowInsets wi = mContainer.getRootWindowInsets();
        if (wi != null) {
            top = Math.max(top, wi.getInsets(WindowInsets.Type.statusBars()).top);
            top = Math.max(top, wi.getInsets(WindowInsets.Type.displayCutout()).top);
        }
        if (top <= 0) {
            top = dp(36);
        }
        return top;
    }

    private int resolveNavBottom() {
        int bottom = Math.max(0, mInsets.bottom);
        WindowInsets wi = mContainer.getRootWindowInsets();
        if (wi != null) {
            bottom = Math.max(bottom, wi.getInsets(WindowInsets.Type.navigationBars()).bottom);
            bottom = Math.max(bottom, wi.getInsets(WindowInsets.Type.tappableElement()).bottom);
        }
        return bottom;
    }

    /** Re-read prefs and push column count into All Apps adapters. */
    public void applyDrawerColumns() {
        int cols = ColorOsDrawerColumns.resolve(
                mContainer.getContext(), mLauncher.getDeviceProfile());
        AllAppsRecyclerView active = mContainer.getActiveRecyclerView();
        applyColumnsToRv(active, cols);
        // Also update search / work holders if present.
        View appsList = mContainer.findViewById(R.id.apps_list_view);
        if (appsList instanceof AllAppsRecyclerView) {
            applyColumnsToRv((AllAppsRecyclerView) appsList, cols);
        }
    }

    private void applyColumnsToRv(@Nullable AllAppsRecyclerView rv, int cols) {
        if (rv == null) {
            return;
        }
        if (rv.getAdapter() instanceof BaseAllAppsAdapter) {
            ((BaseAllAppsAdapter<?>) rv.getAdapter()).setAppsPerRow(cols);
        }
        if (rv.getApps() != null) {
            rv.getApps().setNumAppsPerRowAllApps(cols);
        }
        if (rv.getAdapter() != null) {
            rv.swapAdapter(rv.getAdapter(), true);
            rv.getRecycledViewPool().clear();
        }
    }

    /**
     * Oppo {@code dismissPopupWindow}: tear down immediately so Home / pause / sort
     * cannot leave a floating or dimmed {@link COUIPopupListWindow}.
     * Also exits letter-cluster filter mode.
     */
    public void dismissPopupWindow() {
        dismissLetterCluster();
        if (mPopupWindow == null) {
            return;
        }
        if (mPopupWindow.isShowing()) {
            mPopupWindow.forceDismiss();
        }
    }

    /** Exit Oppo-style A–Z filtered / cluster overlay. */
    public void dismissLetterCluster() {
        if (mLetterCluster != null && mLetterCluster.isShowing()) {
            mLetterCluster.dismiss();
        }
    }

    /**
     * Oppo: after leaving cluster, drop scrub (blue) styling. Prefer the filtered
     * section as the follow highlight — with few apps the list often cannot scroll
     * that section to row 0, so syncing from first-visible always stuck on B.
     */
    private void onLetterClusterDismissed() {
        if (mLetterIndex == null) {
            return;
        }
        mLetterIndex.endScrubStyle();
        AllAppsRecyclerView rv = resolveAppsRecyclerView();
        String section = mLastClusterSection;
        if (section != null) {
            jumpToLetterImmediate(section);
            mLetterIndex.setFollowLetter(section);
        } else if (rv != null) {
            rv.post(() -> syncLetterRailFromScroll(rv));
        } else {
            mLetterIndex.clearActiveLetter();
        }
    }

    private void showLetterCluster(String letter, int centerYInRail) {
        if (mLetterCluster == null || mShowingCategories || letter == null) {
            return;
        }
        AllAppsRecyclerView rv = resolveAppsRecyclerView();
        if (rv == null || rv.getApps() == null) {
            return;
        }
        List<AppInfo> apps = ColorOsLetterClusterOverlay.appsForSection(rv.getApps(), letter);
        if (apps.isEmpty()) {
            // Oppo never selects empty sections; keep previous cluster if any.
            return;
        }
        mLastClusterSection = letter;
        mLetterIndex.setScrubLetter(letter);
        // Overlay is full-screen; map rail letter center into container coordinates.
        int[] railLoc = new int[2];
        int[] containerLoc = new int[2];
        mLetterIndex.getLocationInWindow(railLoc);
        mContainer.getLocationInWindow(containerLoc);
        int yInOverlay = (railLoc[1] - containerLoc[1]) + centerYInRail;
        mLetterCluster.showSection(letter, apps, yInOverlay);
        // Letter rail stays above the cluster (Oppo keeps A–Z while filtering).
        mLetterIndex.bringToFront();
    }

    private void updateLetterClusterY(int centerYInRail) {
        if (mLetterCluster == null || mLetterIndex == null) {
            return;
        }
        int[] railLoc = new int[2];
        int[] containerLoc = new int[2];
        mLetterIndex.getLocationInWindow(railLoc);
        mContainer.getLocationInWindow(containerLoc);
        mLetterCluster.updateLetterY((railLoc[1] - containerLoc[1]) + centerYInRail);
    }

    /**
     * Oppo {@code handleManagerClick}: {@link COUIPopupListWindow} with Select / Sort / Settings.
     */
    private void showOverflowMenu(View anchor) {
        ensurePopupWindow();
        // Oppo ignores re-click while showing; we force-dismiss so a dimmed
        // in-flight exit cannot leave a stuck floating window.
        if (mPopupWindow.isShowing()) {
            mPopupWindow.forceDismiss();
            return;
        }
        rebuildPopupItems();
        mPopupWindow.setItemList(mPopupItems);
        mPopupWindow.show(anchor);
    }

    private void ensurePopupWindow() {
        if (mPopupWindow != null) {
            return;
        }
        // COUIPopupListWindow layouts resolve ?couiRoundCornerM etc.; Launcher theme
        // alone is insufficient — wrap with Theme.COUI like TaskMenuView / Oppo.
        // Oppo drawer overflow uses blue accent (selected text / check / expand).
        Context couiContext = new ContextThemeWrapper(
                mContainer.getContext(), com.coui.appcompat.R.style.Theme_COUI_Blue);
        mPopupWindow = new COUIPopupListWindow(couiContext);
        // Keep solid white panels so the main menu stays visible under the Sort
        // submenu. System blur clears the wrapper background; without ColorOS blur
        // that leaves Select invisible on the dark drawer.
        mPopupWindow.setUseBackgroundBlur(false);
        mPopupWindow.setDismissTouchOutside(true);
        mPopupWindow.setOnItemClickListener(this::onPopupMainItemClick);
        mPopupWindow.setSubMenuClickListener(this::onPopupSubMenuItemClick);
    }

    private void rebuildPopupItems() {
        Context context = mContainer.getContext();
        mPopupItems.clear();

        PopupListItem.Builder builder = new PopupListItem.Builder();
        builder.setTitle(context.getString(R.string.coloros_drawer_app_sort_select));
        builder.setIsEnable(true);
        builder.setId(MENU_ID_SELECT);
        mPopupItems.add(builder.build());

        builder.reset();
        builder.setTitle(context.getString(R.string.coloros_drawer_app_sort_sort));
        builder.setIsEnable(true);
        builder.setId(MENU_ID_SORT);
        attachSortSubMenu(builder);
        mPopupItems.add(builder.build());

        builder.reset();
        builder.setTitle(context.getString(R.string.coloros_drawer_category_settings));
        builder.setIsEnable(true);
        builder.setId(MENU_ID_SETTINGS);
        mPopupItems.add(builder.build());
    }

    private void attachSortSubMenu(PopupListItem.Builder builder) {
        Context context = mContainer.getContext();
        int current = ColorOsDrawerSort.getSortRule(context);
        String[] options = ColorOsDrawerSort.getSortOptionLabels(context);
        ArrayList<PopupListItem> sub = new ArrayList<>(options.length);
        for (int i = 0; i < options.length; i++) {
            PopupListItem.Builder subBuilder = new PopupListItem.Builder();
            subBuilder.setIcon(null);
            subBuilder.setTitle(options[i]);
            subBuilder.setIsChecked(i == current);
            subBuilder.setIsEnable(true);
            subBuilder.setId(i);
            sub.add(subBuilder.build());
        }
        builder.setDescription(options[Math.max(0, Math.min(current, options.length - 1))]);
        builder.setSubMenuItemList(sub);
    }

    private void onPopupMainItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position < 0 || position >= mPopupItems.size()) {
            return;
        }
        int itemId = mPopupItems.get(position).getId();
        if (itemId == MENU_ID_SELECT) {
            mPopupWindow.forceDismiss();
            // Oppo enters multi-select edit; not ported yet — keep menu parity only.
            return;
        }
        if (itemId == MENU_ID_SETTINGS) {
            mPopupWindow.forceDismiss();
            Intent intent = new Intent(mLauncher, SettingsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            mLauncher.startActivity(intent);
        }
        // MENU_ID_SORT opens COUI submenu; ignore main click.
    }

    private void onPopupSubMenuItemClick(AdapterView<?> parent, View view, int position, long id) {
        applySortRule(position);
        // forceDismiss clears submenu ListView alpha; animated dismiss can leave it.
        mPopupWindow.forceDismiss();
    }

    private void applySortRule(int rule) {
        ColorOsDrawerSort.setSortRule(mContainer.getContext(), rule);
        AllAppsRecyclerView rv = mContainer.getActiveRecyclerView();
        if (rv != null && rv.getApps() != null) {
            rv.getApps().onDrawerSortRuleChanged();
        }
        View appsList = mContainer.findViewById(R.id.apps_list_view);
        if (appsList instanceof AllAppsRecyclerView) {
            AlphabeticalAppsList<?> apps = ((AllAppsRecyclerView) appsList).getApps();
            if (apps != null && (rv == null || apps != rv.getApps())) {
                apps.onDrawerSortRuleChanged();
            }
        }
        updateLetterRailVisibility();
        // Sort rebuild is async; refresh populated sections after adapters update.
        mContainer.post(this::refreshLetterRailSections);
    }

    /** Oppo hides A–Z rail when sort is not by name. */
    private void updateLetterRailVisibility() {
        if (mLetterIndex == null) {
            return;
        }
        boolean show = !mShowingCategories
                && ColorOsDrawerSort.getSortRule(mContainer.getContext())
                == ColorOsDrawerSort.SORT_BY_NAME;
        mLetterIndex.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) {
            dismissLetterCluster();
            mLetterIndex.clearActiveLetter();
        } else {
            mLetterIndex.bringToFront();
            refreshLetterRailSections();
        }
    }

    /**
     * Oppo {@code isSectionHasValue} / {@code IndexIndicationKey.hasValue}: empty
     * A–Z keys are not selectable while scrubbing.
     * If the app list is not ready yet, leave the rail fail-open (all selectable)
     * so scrubbing still works.
     */
    private void refreshLetterRailSections() {
        if (mLetterIndex == null) {
            return;
        }
        boolean[] hasValue = new boolean[ColorOsLetterRail.LETTERS.length];
        boolean foundAny = false;
        AllAppsRecyclerView rv = resolveAppsRecyclerView();
        AlphabeticalAppsList<?> apps = rv != null ? rv.getApps() : null;
        if (apps != null) {
            List<FastScrollSectionInfo> sections = apps.getFastScrollerSections();
            if (sections != null) {
                for (FastScrollSectionInfo info : sections) {
                    int index = ColorOsLetterRail.indexForSection(info.sectionName);
                    if (index >= 0) {
                        hasValue[index] = true;
                        foundAny = true;
                    }
                }
            }
            if (!foundAny) {
                for (int i = 0; i < ColorOsLetterRail.LETTERS.length; i++) {
                    if (!ColorOsLetterClusterOverlay
                            .appsForSection(apps, ColorOsLetterRail.LETTERS[i]).isEmpty()) {
                        hasValue[i] = true;
                        foundAny = true;
                    }
                }
            }
        }
        if (!foundAny) {
            // Model not ready — do not stamp all-false (that disables scrub).
            return;
        }
        mLetterIndex.setSectionHasValue(hasValue);
    }

    /**
     * Oppo {@code updateMoveTouchBarText}: highlight the section for the first visible icon.
     */
    private void attachLetterRailScrollSync() {
        AllAppsRecyclerView rv = mContainer.getActiveRecyclerView();
        if (rv == null) {
            View appsList = mContainer.findViewById(R.id.apps_list_view);
            if (appsList instanceof AllAppsRecyclerView) {
                rv = (AllAppsRecyclerView) appsList;
            }
        }
        if (rv == null) {
            return;
        }
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                // Ignore layout-only callbacks (dy==0). With few apps the list cannot
                // scroll the filtered section to row 0; a layout pass would otherwise
                // always reset the follow highlight back to B.
                if (dy != 0) {
                    syncLetterRailFromScroll(recyclerView);
                }
            }
        });
    }

    private void syncLetterRailFromScroll(RecyclerView recyclerView) {
        if (mLetterIndex == null || mLetterIndex.getVisibility() != View.VISIBLE) {
            return;
        }
        // Cluster owns the active letter while filtered.
        if (mLetterCluster != null && mLetterCluster.isShowing()) {
            return;
        }
        if (!(recyclerView instanceof AllAppsRecyclerView)) {
            return;
        }
        AlphabeticalAppsList<?> apps = ((AllAppsRecyclerView) recyclerView).getApps();
        if (apps == null) {
            return;
        }
        RecyclerView.LayoutManager lm = recyclerView.getLayoutManager();
        if (!(lm instanceof androidx.recyclerview.widget.LinearLayoutManager)) {
            return;
        }
        int first = ((androidx.recyclerview.widget.LinearLayoutManager) lm)
                .findFirstVisibleItemPosition();
        if (first == RecyclerView.NO_POSITION) {
            return;
        }
        List<?> items = apps.getAdapterItems();
        if (first < 0 || first >= items.size()) {
            return;
        }
        Object item = items.get(first);
        if (!(item instanceof BaseAllAppsAdapter.AdapterItem)) {
            return;
        }
        AppInfo info = ((BaseAllAppsAdapter.AdapterItem) item).itemInfo;
        if (info == null || info.sectionName == null || info.sectionName.isEmpty()) {
            // Walk forward to first icon with a section.
            for (int i = first; i < Math.min(first + 8, items.size()); i++) {
                Object o = items.get(i);
                if (o instanceof BaseAllAppsAdapter.AdapterItem) {
                    AppInfo ai = ((BaseAllAppsAdapter.AdapterItem) o).itemInfo;
                    if (ai != null && ai.sectionName != null && !ai.sectionName.isEmpty()) {
                        mLetterIndex.setFollowLetter(ai.sectionName);
                        return;
                    }
                }
            }
            return;
        }
        mLetterIndex.setFollowLetter(info.sectionName);
    }

    /**
     * Immediate scroll (not smooth) so the list lands under the cluster even while
     * the recycler is alpha-hidden.
     */
    private void jumpToLetterImmediate(@Nullable String letter) {
        if (letter == null || mShowingCategories) {
            return;
        }
        AllAppsRecyclerView rv = resolveAppsRecyclerView();
        if (rv == null || rv.getApps() == null) {
            return;
        }
        AlphabeticalAppsList<?> apps = rv.getApps();
        List<?> items = apps.getAdapterItems();
        if (items == null || items.isEmpty()) {
            return;
        }
        String target = letter.toUpperCase(Locale.US);
        int targetPos = -1;
        for (int i = 0; i < items.size(); i++) {
            Object o = items.get(i);
            if (!(o instanceof BaseAllAppsAdapter.AdapterItem)) {
                continue;
            }
            AppInfo info = ((BaseAllAppsAdapter.AdapterItem) o).itemInfo;
            if (info == null) {
                continue;
            }
            char head = 0;
            if (info.sectionName != null && !info.sectionName.isEmpty()) {
                head = Character.toUpperCase(info.sectionName.charAt(0));
            } else if (info.title != null && info.title.length() > 0) {
                head = Character.toUpperCase(info.title.charAt(0));
            }
            if (head == 0) {
                continue;
            }
            boolean match;
            if ("#".equals(target)) {
                match = !Character.isLetter(head);
            } else {
                match = head == target.charAt(0);
            }
            if (match) {
                targetPos = i;
                break;
            }
        }
        if (targetPos < 0) {
            return;
        }
        final int pos = targetPos;
        rv.stopScroll();
        Runnable scroll = () -> {
            RecyclerView.LayoutManager lm = rv.getLayoutManager();
            if (lm instanceof androidx.recyclerview.widget.LinearLayoutManager) {
                ((androidx.recyclerview.widget.LinearLayoutManager) lm)
                        .scrollToPositionWithOffset(pos, 0);
            } else {
                rv.scrollToPosition(pos);
            }
        };
        scroll.run();
        // Second pass after the list is shown again (dismiss path).
        rv.post(scroll);
    }

    private void showAllPage() {
        mShowingCategories = false;
        dismissLetterCluster();
        updateLetterRailVisibility();
        if (mCategoryList != null) {
            mCategoryList.setVisibility(View.GONE);
        }
        AllAppsRecyclerView rv = mContainer.getActiveRecyclerView();
        if (rv != null) {
            rv.setVisibility(View.VISIBLE);
        }
        View pager = mContainer.findViewById(R.id.apps_list_view);
        if (pager != null) {
            pager.setVisibility(View.VISIBLE);
        }
    }

    private void showCategoriesPage() {
        mShowingCategories = true;
        dismissLetterCluster();
        if (mLetterIndex != null) {
            mLetterIndex.setVisibility(View.GONE);
        }
        AllAppsRecyclerView rv = mContainer.getActiveRecyclerView();
        if (rv != null) {
            rv.setVisibility(View.INVISIBLE);
        }
        View mainList = mContainer.findViewById(R.id.apps_list_view);
        if (mainList != null) {
            mainList.setVisibility(View.INVISIBLE);
        }
        bindCategories();
        mCategoryList.setVisibility(View.VISIBLE);
    }

    private void jumpToLetter(@Nullable String letter) {
        if (letter == null || mShowingCategories) {
            return;
        }
        AllAppsRecyclerView rv = resolveAppsRecyclerView();
        if (rv == null || rv.getApps() == null) {
            return;
        }
        AlphabeticalAppsList<?> apps = rv.getApps();
        List<FastScrollSectionInfo> sections = apps.getFastScrollerSections();
        if (sections == null || sections.isEmpty()) {
            return;
        }
        String target = letter.toUpperCase(Locale.US);
        FastScrollSectionInfo exact = null;
        FastScrollSectionInfo next = null;
        for (FastScrollSectionInfo info : sections) {
            if (info.sectionName == null || info.sectionName.isEmpty()) {
                continue;
            }
            String section = info.sectionName.toUpperCase(Locale.US);
            char head = section.charAt(0);
            if ("#".equals(target)) {
                if (!Character.isLetter(head)) {
                    exact = info;
                    break;
                }
                continue;
            }
            if (section.startsWith(target) || String.valueOf(head).equals(target)) {
                exact = info;
                break;
            }
            // Oppo-style: if this letter has no apps, land on the next section after it.
            if (next == null && Character.isLetter(head) && head > target.charAt(0)) {
                next = info;
            }
        }
        FastScrollSectionInfo go = exact != null ? exact : next;
        if (go == null) {
            return;
        }
        rv.scrollToFastScrollSection(go);
    }

    @Nullable
    private AllAppsRecyclerView resolveAppsRecyclerView() {
        AllAppsRecyclerView rv = mContainer.getActiveRecyclerView();
        if (rv != null) {
            return rv;
        }
        View appsList = mContainer.findViewById(R.id.apps_list_view);
        return appsList instanceof AllAppsRecyclerView
                ? (AllAppsRecyclerView) appsList
                : null;
    }

    private void bindCategories() {
        List<AppInfo> apps = new ArrayList<>();
        for (AppInfo appInfo : mContainer.getAppsStore().getApps()) {
            if (appInfo != null) {
                apps.add(appInfo);
            }
        }
        List<CategoryInfo> categories = mCategoryController.getCategories(mLauncher, apps);
        ((CategoryCardAdapter) mCategoryList.getAdapter()).setItems(categories);
    }

    private int dp(int value) {
        return Math.round(value * mContainer.getResources().getDisplayMetrics().density);
    }

    private final class CategoryCardAdapter extends RecyclerView.Adapter<CategoryCardVH> {
        private List<CategoryInfo> mItems = List.of();

        void setItems(List<CategoryInfo> items) {
            mItems = items == null ? List.of() : items;
            notifyDataSetChanged();
        }

        @Override
        public CategoryCardVH onCreateViewHolder(ViewGroup parent, int viewType) {
            FrameLayout card = new FrameLayout(parent.getContext());
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(148));
            lp.setMargins(dp(6), dp(6), dp(6), dp(6));
            card.setLayoutParams(lp);
            card.setBackgroundResource(R.drawable.coloros_category_card_bg);

            TextView title = new TextView(parent.getContext());
            title.setId(android.R.id.title);
            FrameLayout.LayoutParams titleLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            titleLp.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL;
            title.setGravity(android.view.Gravity.CENTER);
            title.setPadding(dp(8), dp(4), dp(8), dp(10));
            title.setTextColor(0xFFFFFFFF);
            title.setTextSize(13);
            card.addView(title, titleLp);

            android.widget.GridLayout icons = new android.widget.GridLayout(parent.getContext());
            icons.setId(android.R.id.icon);
            icons.setColumnCount(2);
            icons.setRowCount(2);
            FrameLayout.LayoutParams iconsLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            iconsLp.setMargins(dp(12), dp(12), dp(12), dp(36));
            card.addView(icons, iconsLp);

            return new CategoryCardVH(card, title, icons);
        }

        @Override
        public void onBindViewHolder(CategoryCardVH holder, int position) {
            CategoryInfo info = mItems.get(position);
            holder.title.setText(info.getFolderName());
            holder.icons.removeAllViews();
            int shown = 0;
            for (String component : info.getComponentNames()) {
                if (shown >= 4) {
                    break;
                }
                AppInfo app = mCategoryController.getAppInfo(component);
                if (app == null) {
                    for (AppInfo ai : mContainer.getAppsStore().getApps()) {
                        if (ai != null && ai.componentName != null
                                && ai.componentName.flattenToString().contains(component)) {
                            app = ai;
                            break;
                        }
                    }
                }
                if (app == null) {
                    continue;
                }
                ImageView iv = new ImageView(holder.itemView.getContext());
                android.widget.GridLayout.LayoutParams glp =
                        new android.widget.GridLayout.LayoutParams();
                glp.width = 0;
                glp.height = 0;
                glp.columnSpec = android.widget.GridLayout.spec(shown % 2, 1f);
                glp.rowSpec = android.widget.GridLayout.spec(shown / 2, 1f);
                glp.setMargins(dp(4), dp(4), dp(4), dp(4));
                if (app.bitmap != null) {
                    iv.setImageBitmap(app.bitmap.icon);
                }
                iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                holder.icons.addView(iv, glp);
                shown++;
            }
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }
    }

    private static final class CategoryCardVH extends RecyclerView.ViewHolder {
        final TextView title;
        final android.widget.GridLayout icons;

        CategoryCardVH(View itemView, TextView title, android.widget.GridLayout icons) {
            super(itemView);
            this.title = title;
            this.icons = icons;
        }
    }
}
