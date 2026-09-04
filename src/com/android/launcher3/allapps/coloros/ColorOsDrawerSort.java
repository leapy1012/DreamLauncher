package com.android.launcher3.allapps.coloros;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import com.android.launcher3.R;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.util.ComponentKey;

import java.util.Comparator;

/**
 * Drawer app-sort prefs matching Oppo {@code draw_app_sort_rule_key} / {@code draw_sort_options}.
 */
public final class ColorOsDrawerSort {

    public static final int SORT_BY_NAME = 0;
    public static final int SORT_BY_USAGE = 1;
    public static final int SORT_BY_INSTALL_TIME = 2;

    private static final String PREFS = "coloros_drawer_prefs";
    private static final String KEY_SORT = "draw_app_sort_rule_key";
    private static final String KEY_CLICK_PREFIX = "click_";

    private ColorOsDrawerSort() {
    }

    public static int getSortRule(@NonNull Context context) {
        return prefs(context).getInt(KEY_SORT, SORT_BY_NAME);
    }

    public static void setSortRule(@NonNull Context context, int rule) {
        prefs(context).edit().putInt(KEY_SORT, rule).apply();
    }

    public static String[] getSortOptionLabels(@NonNull Context context) {
        return context.getResources().getStringArray(R.array.coloros_draw_sort_options);
    }

    public static String getSortOptionLabel(@NonNull Context context, int rule) {
        String[] labels = getSortOptionLabels(context);
        if (rule < 0 || rule >= labels.length) {
            return labels[0];
        }
        return labels[rule];
    }

    public static void recordAppLaunch(@NonNull Context context, @NonNull AppInfo info) {
        if (info.getTargetComponent() == null) {
            return;
        }
        String prefKey = KEY_CLICK_PREFIX
                + new ComponentKey(info.getTargetComponent(), info.user).toString();
        SharedPreferences p = prefs(context);
        p.edit().putInt(prefKey, p.getInt(prefKey, 0) + 1).apply();
    }

    public static Comparator<AppInfo> comparatorFor(
            @NonNull Context context, int rule, @NonNull Comparator<AppInfo> nameComparator) {
        if (rule == SORT_BY_INSTALL_TIME) {
            PackageManager pm = context.getPackageManager();
            return (a, b) -> {
                int c = Long.compare(installTime(pm, b), installTime(pm, a));
                return c != 0 ? c : nameComparator.compare(a, b);
            };
        }
        if (rule == SORT_BY_USAGE) {
            SharedPreferences p = prefs(context);
            PackageManager pm = context.getPackageManager();
            return (a, b) -> {
                int c = Integer.compare(clickTimes(p, b), clickTimes(p, a));
                if (c != 0) {
                    return c;
                }
                c = Long.compare(installTime(pm, a), installTime(pm, b));
                return c != 0 ? c : nameComparator.compare(a, b);
            };
        }
        return nameComparator;
    }

    private static int clickTimes(SharedPreferences prefs, AppInfo info) {
        if (info.getTargetComponent() == null) {
            return 0;
        }
        return prefs.getInt(
                KEY_CLICK_PREFIX
                        + new ComponentKey(info.getTargetComponent(), info.user).toString(),
                0);
    }

    private static long installTime(PackageManager pm, AppInfo info) {
        try {
            if (info.getTargetComponent() == null) {
                return 0L;
            }
            return pm.getPackageInfo(info.getTargetComponent().getPackageName(), 0).firstInstallTime;
        } catch (Exception e) {
            return 0L;
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
