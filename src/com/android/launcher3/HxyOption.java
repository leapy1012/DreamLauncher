package com.android.launcher3;

import android.os.SystemProperties;

public class HxyOption {
    public static final boolean HXY_LAUNCHER_SUPPORT_LARGE_FOLDER = getProp("ro.hxy.support_large_folder", "yes", "支持大文件夹 yes 支持");

    private static boolean getProp(String key, String def, String des) {
        return true;
    }
}
