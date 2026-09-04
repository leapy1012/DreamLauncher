package com.android.launcher3;

import android.content.Context;
import android.provider.Settings;

/**
 * Home-screen style (Regular vs App drawer).
 * <p>
 * Values match {@link com.android.launcher3.settings.LauncherStyleFragment}:
 * {@code 0} = App drawer (All Apps swipe enabled),
 * {@code 1} = Regular (no All Apps drawer; apps live on the workspace).
 */
public final class LauncherStyle {

    public static final String SETTINGS_KEY = "launcher_style";
    public static final String SETTINGS_CHANGE_KEY = "launcher_style_change";

    /** App drawer mode — All Apps gesture enabled. */
    public static final int APP_DRAWER = 0;
    /** Regular mode — no All Apps drawer. */
    public static final int REGULAR = 1;

    private LauncherStyle() {}

    /** Factory default from {@code config_default_launcher_style}. */
    public static int getDefault(Context context) {
        return context.getResources().getInteger(R.integer.config_default_launcher_style);
    }

    public static int get(Context context) {
        return Settings.System.getInt(
                context.getContentResolver(), SETTINGS_KEY, getDefault(context));
    }

    public static boolean isRegular(Context context) {
        return get(context) == REGULAR;
    }

    public static boolean isAppDrawer(Context context) {
        return get(context) == APP_DRAWER;
    }

    public static void set(Context context, int style) {
        Settings.System.putInt(context.getContentResolver(), SETTINGS_KEY, style);
        Settings.System.putInt(context.getContentResolver(), SETTINGS_CHANGE_KEY, 1);
    }
}
