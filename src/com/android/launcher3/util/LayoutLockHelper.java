package com.android.launcher3.util;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.settings.SettingsActivity;
import com.android.launcher3.views.Snackbar;

public final class LayoutLockHelper {

    private LayoutLockHelper() { }

    public static boolean isLayoutLocked(Context context) {
        return LauncherPrefs.getPrefs(context)
                .getBoolean(LauncherPrefs.WORKSPACE_LAYOUT_DOCK, false);
    }

    public static boolean checkLockedAndShowMessage(Context context) {
        if (!isLayoutLocked(context)) {
            return false;
        }

        if (context instanceof Launcher) {
            showLockedSnackbar((Launcher) context);
        } else {
            Toast.makeText(context, R.string.home_screen_layout_lock_tips, Toast.LENGTH_SHORT)
                    .show();
        }
        return true;
    }

    public static boolean checkLockedAndShowMessage(Launcher launcher) {
        if (!isLayoutLocked(launcher)) {
            return false;
        }
        showLockedSnackbar(launcher);
        return true;
    }

    private static void showLockedSnackbar(Launcher launcher) {
        Snackbar.show(launcher, R.string.home_screen_layout_lock_tips,
                R.string.launcher_locked_toast_look_setting, null,
                () -> launcher.startActivity(new Intent(Intent.ACTION_APPLICATION_PREFERENCES)
                        .setPackage(launcher.getPackageName())
                        .putExtra(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY,
                                LauncherPrefs.WORKSPACE_LAYOUT_DOCK)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
    }
}
