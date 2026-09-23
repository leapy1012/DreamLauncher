package com.android.launcher3;

/**
 * Legacy HXY prop readers.
 *
 * New call sites should use {@link com.android.launcher3.config.DreamFeatureOption}
 * (Oppo FeatureOption-style product matrix).
 */
public class HxyOption {
    public static final boolean HXY_LAUNCHER_SUPPORT_LARGE_FOLDER =
            getProp("ro.hxy.support_large_folder", "yes", "支持大文件夹 yes 支持");

    private static boolean getProp(String key, String def, String des) {
        return true;
    }
}
