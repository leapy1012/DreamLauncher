package com.android.launcher3.allapps.coloros;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherApplication;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.LauncherStyle;
import com.android.launcher3.allapps.ActivityAllAppsContainerView;
import com.android.launcher3.allapps.LauncherAllAppsContainerView;

/**
 * ColorOS Home screen → With drawer settings (Oppo {@code LauncherModelFragment} keys).
 */
public final class ColorOsHomeSettings {

    /** Oppo {@code add_app_to_launcher}. */
    public static final String KEY_ADD_APP_TO_HOME = "add_app_to_launcher";
    /** Oppo {@code launcher_is_show_indicated_app}. */
    public static final String KEY_SHOW_APP_SUGGESTIONS = "launcher_is_show_indicated_app";
    /** Oppo {@code key_drawer_default_page}: 0 = All, 2 = Categories. */
    public static final String KEY_DEFAULT_VIEW = "key_drawer_default_page";
    /** Oppo {@code key_drawer_show_app_name}. */
    public static final String KEY_SHOW_APP_NAMES = "key_drawer_show_app_name";

    public static final int DEFAULT_VIEW_ALL = 0;
    public static final int DEFAULT_VIEW_CATEGORIES = 2;

    public static final String VALUE_DEFAULT_VIEW_ALL = "all";
    public static final String VALUE_DEFAULT_VIEW_CATEGORIES = "categories";

    private ColorOsHomeSettings() {}

    public static boolean isAddNewAppsToHome(Context context) {
        return prefs(context).getBoolean(KEY_ADD_APP_TO_HOME, true);
    }

    public static void setAddNewAppsToHome(Context context, boolean add) {
        prefs(context).edit().putBoolean(KEY_ADD_APP_TO_HOME, add).apply();
    }

    /**
     * Regular mode always places new apps on the workspace. Drawer mode honors
     * {@link #isAddNewAppsToHome}.
     */
    public static boolean shouldAddNewAppsToHome(Context context) {
        return LauncherStyle.isRegular(context) || isAddNewAppsToHome(context);
    }

    public static boolean isShowAppSuggestions(Context context) {
        return prefs(context).getBoolean(KEY_SHOW_APP_SUGGESTIONS, false);
    }

    public static void setShowAppSuggestions(Context context, boolean show) {
        prefs(context).edit().putBoolean(KEY_SHOW_APP_SUGGESTIONS, show).apply();
    }

    public static int getDefaultView(Context context) {
        int value = prefs(context).getInt(KEY_DEFAULT_VIEW, DEFAULT_VIEW_ALL);
        return value == DEFAULT_VIEW_CATEGORIES ? DEFAULT_VIEW_CATEGORIES : DEFAULT_VIEW_ALL;
    }

    public static boolean isDefaultCategories(Context context) {
        return getDefaultView(context) == DEFAULT_VIEW_CATEGORIES;
    }

    public static void setDefaultView(Context context, int view) {
        int value = view == DEFAULT_VIEW_CATEGORIES ? DEFAULT_VIEW_CATEGORIES : DEFAULT_VIEW_ALL;
        prefs(context).edit().putInt(KEY_DEFAULT_VIEW, value).apply();
    }

    public static void setDefaultViewFromValue(Context context, String value) {
        setDefaultView(context, VALUE_DEFAULT_VIEW_CATEGORIES.equals(value)
                ? DEFAULT_VIEW_CATEGORIES : DEFAULT_VIEW_ALL);
    }

    public static String getDefaultViewValue(Context context) {
        return isDefaultCategories(context)
                ? VALUE_DEFAULT_VIEW_CATEGORIES : VALUE_DEFAULT_VIEW_ALL;
    }

    public static boolean isShowDrawerAppNames(Context context) {
        return prefs(context).getBoolean(KEY_SHOW_APP_NAMES, true);
    }

    public static void setShowDrawerAppNames(Context context, boolean show) {
        prefs(context).edit().putBoolean(KEY_SHOW_APP_NAMES, show).apply();
    }

    @Nullable
    public static ColorOsDrawerChrome chrome() {
        Launcher launcher = LauncherApplication.getLauncher();
        if (launcher == null) {
            return null;
        }
        ActivityAllAppsContainerView<?> apps = launcher.getAppsView();
        if (apps instanceof LauncherAllAppsContainerView) {
            return ((LauncherAllAppsContainerView) apps).getColorOsChrome();
        }
        return null;
    }

    private static SharedPreferences prefs(Context context) {
        return LauncherPrefs.getPrefs(context);
    }
}
