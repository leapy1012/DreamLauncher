package com.android.launcher3.allapps.coloros;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.android.launcher3.allapps.AllAppsStore;
import com.android.launcher3.allapps.BaseAllAppsAdapter.AdapterItem;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.util.ComponentKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Top apps for ColorOS empty-search (Oppo predicted row / LeftScreen
 * {@code Commonly used application}).
 *
 * <p>Same ranking as {@code UsageStatsController}: last-used via
 * {@link UsageStatsManager}, mapped onto drawer {@link AppInfo}s, padded to
 * {@link #targetCount(Context)}. Count is {@code columns * 2} like Oppo
 * {@code PredictedAppManager.getPredictedApps(numAllAppsColumns * 2)}:
 * 10 apps in 5-column drawer, 8 in 4-column.
 */
public final class ColorOsCommonlyUsedAppsProvider {

    /**
     * Oppo {@code PredictedAppManager.DEFAULT_PACKAGES}. Used when usage-stats
     * is empty (launcher is not granted {@code PACKAGE_USAGE_STATS}).
     */
    private static final String[] DEFAULT_PACKAGES = {
            "com.android.settings",
            "com.android.deskclock",
            "com.google.android.deskclock",
            "com.android.camera2",
            "com.android.camera",
            "com.android.mms",
            "com.google.android.apps.messaging",
            "com.android.contacts",
            "com.google.android.dialer",
            "com.android.dialer",
            "com.android.browser",
            "com.android.chrome",
            "com.android.calendar",
            "com.google.android.calendar",
            "com.android.gallery3d",
            "com.android.calculator2",
            "com.android.documentsui",
            "com.android.vending",
    };

    /** Two rows of the current drawer column count. */
    public static int targetCount(@NonNull Context context) {
        int cols = ColorOsDrawerColumns.get(context);
        if (cols <= 0) {
            cols = ColorOsDrawerColumns.COLUMNS_FIVE;
        }
        return cols * 2;
    }

    private ColorOsCommonlyUsedAppsProvider() {}

    /**
     * Ranked drawer apps for All-tab suggestions / empty search.
     * Always fills {@link #targetCount} like Oppo {@code getPredictedApps}:
     * usage stats, then {@link #DEFAULT_PACKAGES}, then remaining store apps.
     */
    @WorkerThread
    @NonNull
    public static List<AppInfo> collect(@NonNull Context context,
            @Nullable AllAppsStore store, boolean padToTarget) {
        int targetCount = targetCount(context);
        ArrayList<AppInfo> out = new ArrayList<>(targetCount);
        if (store == null) {
            return out;
        }
        AppInfo[] all = store.getApps();
        if (all == null || all.length == 0) {
            return out;
        }

        Set<ComponentKey> used = new HashSet<>();
        addUnique(out, used, mapUsageStatsToApps(context, all, targetCount), targetCount);
        if (out.size() < targetCount) {
            addUnique(out, used, matchDefaultPackages(all), targetCount);
        }
        if (padToTarget && out.size() < targetCount) {
            addUnique(out, used, Arrays.asList(all), targetCount);
        }
        return out;
    }

    private static void addUnique(@NonNull List<AppInfo> out, @NonNull Set<ComponentKey> used,
            @NonNull List<AppInfo> candidates, int targetCount) {
        for (AppInfo info : candidates) {
            if (info == null || info.componentName == null) {
                continue;
            }
            if (!used.add(new ComponentKey(info.componentName, info.user))) {
                continue;
            }
            out.add(info);
            if (out.size() >= targetCount) {
                return;
            }
        }
    }

    @NonNull
    private static List<AppInfo> matchDefaultPackages(@NonNull AppInfo[] all) {
        ArrayList<AppInfo> matched = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String pkg : DEFAULT_PACKAGES) {
            if (!seen.add(pkg)) {
                continue;
            }
            for (AppInfo info : all) {
                if (info != null && info.componentName != null
                        && pkg.equals(info.componentName.getPackageName())) {
                    matched.add(info);
                    break;
                }
            }
        }
        return matched;
    }

    /**
     * Build up to {@link #targetCount(Context)} adapter items for empty search.
     * Safe to call off the main thread.
     */
    @WorkerThread
    @NonNull
    public static ArrayList<AdapterItem> build(@NonNull Context context,
            @Nullable AllAppsStore store) {
        List<AppInfo> apps = collect(context, store, true /* padToTarget */);
        ArrayList<AdapterItem> out = new ArrayList<>(apps.size());
        for (AppInfo info : apps) {
            out.add(AdapterItem.asApp(info));
        }
        return out;
    }

    @NonNull
    private static List<AppInfo> mapUsageStatsToApps(@NonNull Context context,
            @NonNull AppInfo[] all, int targetCount) {
        UsageStatsManager usm = context.getSystemService(UsageStatsManager.class);
        if (usm == null) {
            return Collections.emptyList();
        }
        List<UsageStats> stats;
        try {
            stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, 0L,
                    System.currentTimeMillis());
        } catch (RuntimeException e) {
            return Collections.emptyList();
        }
        if (stats == null || stats.isEmpty()) {
            return Collections.emptyList();
        }

        List<UsageStats> sorted = new ArrayList<>(stats);
        Collections.sort(sorted, Comparator.comparingLong(UsageStats::getLastTimeUsed)
                .reversed());

        PackageManager pm = context.getPackageManager();
        List<AppInfo> prioritized = new ArrayList<>();
        Set<String> seenPkgs = new HashSet<>();
        for (UsageStats stat : sorted) {
            String pkg = stat.getPackageName();
            if (TextUtils.isEmpty(pkg) || !seenPkgs.add(pkg)) {
                continue;
            }
            AppInfo match = null;
            Intent launch = pm.getLaunchIntentForPackage(pkg);
            if (launch != null) {
                ResolveInfo ri = pm.resolveActivity(launch, PackageManager.MATCH_DEFAULT_ONLY);
                if (ri != null && ri.activityInfo != null) {
                    String flat = ri.activityInfo.getComponentName().flattenToString();
                    for (AppInfo info : all) {
                        if (info.componentName != null
                                && flat.equals(info.componentName.flattenToString())) {
                            match = info;
                            break;
                        }
                    }
                }
            }
            if (match == null) {
                for (AppInfo info : all) {
                    if (info.componentName != null
                            && pkg.equals(info.componentName.getPackageName())) {
                        match = info;
                        break;
                    }
                }
            }
            if (match != null) {
                prioritized.add(match);
                if (prioritized.size() >= targetCount) {
                    break;
                }
            }
        }
        return prioritized;
    }
}
