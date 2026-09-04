/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.launcher3.allapps;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import com.android.launcher3.R;
import com.android.launcher3.allapps.BaseAllAppsAdapter.AdapterItem;
import com.android.launcher3.allapps.coloros.ColorOsDrawerSort;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.util.LabelComparator;
import com.android.launcher3.views.ActivityContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The alphabetically sorted list of applications.
 *
 * @param <T> Type of context inflating this view.
 */
public class AlphabeticalAppsList<T extends Context & ActivityContext> implements
        AllAppsStore.OnUpdateListener {

    public static final String TAG = "AlphabeticalAppsList";

    /** Oppo {@code VIEW_TYPE_ICON_CHANGE_APPSORT_PAYLOAD} — in-place icon blink on sort. */
    public static final int VIEW_TYPE_ICON_CHANGE_APPSORT_PAYLOAD = 2;

    private final WorkProfileManager mWorkProviderManager;

    /**
     * When true, DiffUtil treats every icon as the same item so cells stay put and
     * only contents refresh (Oppo drawer sort blink). Set around {@link #onDrawerSortRuleChanged()}.
     */
    private boolean mIsChangeAppSortRule;

    /**
     * Info about a fast scroller section, depending if sections are merged, the fast scroller
     * sections will not be the same set as the section headers.
     */
    public static class FastScrollSectionInfo {
        // The section name
        public final String sectionName;
        // The item position
        public final int position;

        public FastScrollSectionInfo(String sectionName, int position) {
            this.sectionName = sectionName;
            this.position = position;
        }
    }


    private final T mActivityContext;

    // The set of apps from the system
    private final List<AppInfo> mApps = new ArrayList<>();
    @Nullable
    private final AllAppsStore mAllAppsStore;

    // The number of results in current adapter
    private int mAccessibilityResultsCount = 0;
    // The current set of adapter items
    private final ArrayList<AdapterItem> mAdapterItems = new ArrayList<>();
    // The set of sections that we allow fast-scrolling to (includes non-merged sections)
    private final List<FastScrollSectionInfo> mFastScrollerSections = new ArrayList<>();

    // The of ordered component names as a result of a search query
    private final ArrayList<AdapterItem> mSearchResults = new ArrayList<>();
    private BaseAllAppsAdapter<T> mAdapter;
    private AppInfoComparator mAppNameComparator;
    private int mNumAppsPerRowAllApps;
    private int mNumAppRowsInAdapter;
    private Predicate<ItemInfo> mItemFilter;

    public AlphabeticalAppsList(Context context, @Nullable AllAppsStore appsStore,
            WorkProfileManager workProfileManager) {
        mAllAppsStore = appsStore;
        mActivityContext = ActivityContext.lookupContext(context);
        mAppNameComparator = new AppInfoComparator(context);
        mWorkProviderManager = workProfileManager;
        mNumAppsPerRowAllApps = com.android.launcher3.allapps.coloros.ColorOsDrawerColumns
                .resolve(context, mActivityContext.getDeviceProfile());
        if (mAllAppsStore != null) {
            mAllAppsStore.addUpdateListener(this);
        }
    }

    /** Used when ColorOS drawer column pref changes (4 ↔ 5). */
    public void setNumAppsPerRowAllApps(int numAppsPerRow) {
        if (numAppsPerRow > 0 && mNumAppsPerRowAllApps != numAppsPerRow) {
            mNumAppsPerRowAllApps = numAppsPerRow;
            onAppsUpdated();
        }
    }

    public void updateItemFilter(Predicate<ItemInfo> itemFilter) {
        this.mItemFilter = itemFilter;
        onAppsUpdated();
    }

    /**
     * Sets the adapter to notify when this dataset changes.
     */
    public void setAdapter(BaseAllAppsAdapter<T> adapter) {
        mAdapter = adapter;
    }

    /**
     * Returns fast scroller sections of all the current filtered applications.
     */
    public List<FastScrollSectionInfo> getFastScrollerSections() {
        return mFastScrollerSections;
    }

    /**
     * Returns the current filtered list of applications broken down into their sections.
     */
    public List<AdapterItem> getAdapterItems() {
        return mAdapterItems;
    }

    /**
     * Returns the child adapter item with IME launch focus.
     */
    public AdapterItem getFocusedChild() {
        if (mAdapterItems.size() == 0 || getFocusedChildIndex() == -1) {
            return null;
        }
        return mAdapterItems.get(getFocusedChildIndex());
    }

    /**
     * Returns the index of the child with IME launch focus.
     */
    public int getFocusedChildIndex() {
        for (AdapterItem item : mAdapterItems) {
            if (item.isCountedForAccessibility()) {
                return mAdapterItems.indexOf(item);
            }
        }
        return -1;
    }

    /**
     * Returns the number of rows of applications
     */
    public int getNumAppRows() {
        return mNumAppRowsInAdapter;
    }

    /**
     * Returns the number of applications in this list.
     */
    public int getNumFilteredApps() {
        return mAccessibilityResultsCount;
    }

    /**
     * Returns whether there are search results which will hide the A-Z list.
     */
    public boolean hasSearchResults() {
        return !mSearchResults.isEmpty();
    }

    /**
     * Sets results list for search
     */
    public boolean setSearchResults(ArrayList<AdapterItem> results) {
        if (Objects.equals(results, mSearchResults)) {
            return false;
        }
        mSearchResults.clear();
        if (results != null) {
            mSearchResults.addAll(results);
        }
        updateAdapterItems();
        return true;
    }

    /**
     * Updates internals when the set of apps are updated.
     */
    @Override
    public void onAppsUpdated() {
        if (mAllAppsStore == null) {
            return;
        }
        // Sort the list of apps
        mApps.clear();

        Stream<AppInfo> appSteam = Stream.of(mAllAppsStore.getApps());
        if (!hasSearchResults() && mItemFilter != null) {
            appSteam = appSteam.filter(mItemFilter);
        }
        Comparator<AppInfo> comparator = mAppNameComparator;
        if (mActivityContext.getResources().getBoolean(R.bool.config_coloros_drawer)) {
            comparator = ColorOsDrawerSort.comparatorFor(
                    mActivityContext,
                    ColorOsDrawerSort.getSortRule(mActivityContext),
                    mAppNameComparator);
        }
        appSteam = appSteam.sorted(comparator);

        // As a special case for some languages (currently only Simplified Chinese), we may need to
        // coalesce sections
        Locale curLocale = mActivityContext.getResources().getConfiguration().locale;
        boolean localeRequiresSectionSorting = curLocale.equals(Locale.SIMPLIFIED_CHINESE);
        if (localeRequiresSectionSorting) {
            // Compute the section headers. We use a TreeMap with the section name comparator to
            // ensure that the sections are ordered when we iterate over it later
            appSteam = appSteam.collect(Collectors.groupingBy(
                    info -> info.sectionName,
                    () -> new TreeMap<>(new LabelComparator()),
                    Collectors.toCollection(ArrayList::new)))
                    .values()
                    .stream()
                    .flatMap(ArrayList::stream);
        }

        appSteam.forEachOrdered(mApps::add);
        // Recompose the set of adapter items from the current set of apps
        if (mSearchResults.isEmpty()) {
            updateAdapterItems();
        }
    }

    /**
     * Oppo {@code drawAppSortUpdate}: reorder with in-place DiffUtil + blink payload.
     */
    public void onDrawerSortRuleChanged() {
        mIsChangeAppSortRule = true;
        try {
            onAppsUpdated();
        } finally {
            mIsChangeAppSortRule = false;
        }
    }

    /**
     * Updates the set of filtered apps with the current filter. At this point, we expect
     * mCachedSectionNames to have been calculated for the set of all apps in mApps.
     */
    public void updateAdapterItems() {
        List<AdapterItem> oldItems = new ArrayList<>(mAdapterItems);
        // Prepare to update the list of sections, filtered apps, etc.
        mFastScrollerSections.clear();
        mAdapterItems.clear();
        mAccessibilityResultsCount = 0;

        // Recreate the filtered and sectioned apps (for convenience for the grid layout) from the
        // ordered set of sections
        if (hasSearchResults()) {
            mAdapterItems.addAll(mSearchResults);
        } else {
            int position = 0;
            boolean addApps = true;
            if (mWorkProviderManager != null) {
                position += mWorkProviderManager.addWorkItems(mAdapterItems);
                addApps = mWorkProviderManager.shouldShowWorkApps();
            }
            if (addApps) {
                String lastSectionName = null;
                for (AppInfo info : mApps) {
                    mAdapterItems.add(AdapterItem.asApp(info));

                    String sectionName = info.sectionName;
                    // Create a new section if the section names do not match
                    if (!sectionName.equals(lastSectionName)) {
                        lastSectionName = sectionName;
                        mFastScrollerSections.add(new FastScrollSectionInfo(sectionName, position));
                    }
                    position++;
                }
            }
        }
        mAccessibilityResultsCount = (int) mAdapterItems.stream()
                .filter(AdapterItem::isCountedForAccessibility).count();

        if (mNumAppsPerRowAllApps != 0) {
            // Update the number of rows in the adapter after we do all the merging (otherwise, we
            // would have to shift the values again)
            int numAppsInSection = 0;
            int numAppsInRow = 0;
            int rowIndex = -1;
            for (AdapterItem item : mAdapterItems) {
                item.rowIndex = 0;
                if (BaseAllAppsAdapter.isDividerViewType(item.viewType)) {
                    numAppsInSection = 0;
                } else if (BaseAllAppsAdapter.isIconViewType(item.viewType)) {
                    if (numAppsInSection % mNumAppsPerRowAllApps == 0) {
                        numAppsInRow = 0;
                        rowIndex++;
                    }
                    item.rowIndex = rowIndex;
                    item.rowAppIndex = numAppsInRow;
                    numAppsInSection++;
                    numAppsInRow++;
                }
            }
            mNumAppRowsInAdapter = rowIndex + 1;
        }

        if (mAdapter != null) {
            // Oppo sort: positional DiffUtil (no moves). Otherwise keep AOSP detectMoves=false.
            DiffUtil.calculateDiff(
                    new MyDiffCallback(oldItems, mAdapterItems, mIsChangeAppSortRule),
                    false)
                    .dispatchUpdatesTo(mAdapter);
        }
    }

    private static class MyDiffCallback extends DiffUtil.Callback {

        private final List<AdapterItem> mOldList;
        private final List<AdapterItem> mNewList;
        private final boolean mIsChangeAppSortRule;

        MyDiffCallback(List<AdapterItem> oldList, List<AdapterItem> newList,
                boolean isChangeAppSortRule) {
            mOldList = oldList;
            mNewList = newList;
            mIsChangeAppSortRule = isChangeAppSortRule;
        }

        @Override
        public int getOldListSize() {
            return mOldList.size();
        }

        @Override
        public int getNewListSize() {
            return mNewList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            AdapterItem oldItem = mOldList.get(oldItemPosition);
            AdapterItem newItem = mNewList.get(newItemPosition);
            // Oppo sort: every icon is "the same item" so ViewHolders stay in grid cells.
            if (mIsChangeAppSortRule
                    && oldItem.viewType == BaseAllAppsAdapter.VIEW_TYPE_ICON
                    && newItem.viewType == BaseAllAppsAdapter.VIEW_TYPE_ICON) {
                return true;
            }
            return oldItem.isSameAs(newItem);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            AdapterItem oldItem = mOldList.get(oldItemPosition);
            AdapterItem newItem = mNewList.get(newItemPosition);
            if (mIsChangeAppSortRule
                    && oldItem.viewType == BaseAllAppsAdapter.VIEW_TYPE_ICON
                    && newItem.viewType == BaseAllAppsAdapter.VIEW_TYPE_ICON) {
                return isSameAppComponent(oldItem, newItem);
            }
            return oldItem.isContentSame(newItem);
        }

        @Nullable
        @Override
        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
            AdapterItem oldItem = mOldList.get(oldItemPosition);
            if (mIsChangeAppSortRule
                    && oldItem.viewType == BaseAllAppsAdapter.VIEW_TYPE_ICON) {
                return VIEW_TYPE_ICON_CHANGE_APPSORT_PAYLOAD;
            }
            return null;
        }

        private static boolean isSameAppComponent(AdapterItem a, AdapterItem b) {
            if (a.itemInfo == null || b.itemInfo == null) {
                return a.itemInfo == b.itemInfo;
            }
            return Objects.equals(a.itemInfo.componentName, b.itemInfo.componentName)
                    && Objects.equals(a.itemInfo.user, b.itemInfo.user);
        }
    }

}
