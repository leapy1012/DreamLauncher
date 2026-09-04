package com.android.launcher3.allapps.coloros;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;

/**
 * ColorOS drawer column count — independent of home grid (Oppo {@code drawer_layout_columns}).
 */
public final class ColorOsDrawerColumns {

    public static final String PREF_KEY = "drawer_layout_columns";
    public static final int COLUMNS_FOUR = 4;
    public static final int COLUMNS_FIVE = 5;
    /** Match Oppo reference screenshots used for parity (pref still overridable). */
    public static final int DEFAULT = COLUMNS_FIVE;

    private ColorOsDrawerColumns() {}

    public static boolean isEnabled(Context context) {
        return context.getResources().getBoolean(R.bool.config_coloros_drawer);
    }

    public static int get(Context context) {
        if (!isEnabled(context)) {
            return -1;
        }
        SharedPreferences prefs = LauncherPrefs.getPrefs(context);
        int cols = prefs.getInt(PREF_KEY, DEFAULT);
        return cols == COLUMNS_FOUR ? COLUMNS_FOUR : COLUMNS_FIVE;
    }

    public static void set(Context context, int columns) {
        int cols = columns == COLUMNS_FOUR ? COLUMNS_FOUR : COLUMNS_FIVE;
        LauncherPrefs.getPrefs(context).edit().putInt(PREF_KEY, cols).apply();
    }

    /** Resolved apps-per-row for All Apps adapters. */
    public static int resolve(Context context, DeviceProfile dp) {
        int colorOs = get(context);
        return colorOs > 0 ? colorOs : dp.numShownAllAppsColumns;
    }
}
