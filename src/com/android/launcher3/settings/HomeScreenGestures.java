/*
 * Copyright (C) 2026 The Android Open Source Project
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
package com.android.launcher3.settings;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;

import com.android.customize.overlay.preference.OverlayPreference;
import com.android.launcher3.LauncherFiles;
import com.android.launcher3.R;

/**
 * Home-screen gesture preferences (ColorOS Gestures category parity).
 */
public final class HomeScreenGestures {
    private static final String TAG = "HomeScreenGestures";

    public static final String PREF_SWIPE_DOWN = "pref_swipe_down";
    public static final String PREF_SWIPE_RIGHT = "pref_swipe_right";

    public static final String SWIPE_DOWN_GLOBAL_SEARCH = "global_search";
    public static final String SWIPE_DOWN_NOTIFICATION = "notification";

    public static final String SWIPE_RIGHT_NONE = "none";
    public static final String SWIPE_RIGHT_QUICK_GLANCE = "quick_glance";

    public static final String QUICK_SEARCH_PACKAGE = "gd.app.quicksearch";
    public static final String QUICK_SEARCH_ACTIVITY =
            "gd.app.quicksearch.ui.activity.SearchHomeActivity";

    private HomeScreenGestures() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(LauncherFiles.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
    }

    public static String getSwipeDownAction(Context context) {
        return prefs(context).getString(PREF_SWIPE_DOWN, SWIPE_DOWN_GLOBAL_SEARCH);
    }

    public static boolean isSwipeDownGlobalSearch(Context context) {
        return SWIPE_DOWN_GLOBAL_SEARCH.equals(getSwipeDownAction(context));
    }

    public static boolean isSwipeDownNotification(Context context) {
        return SWIPE_DOWN_NOTIFICATION.equals(getSwipeDownAction(context));
    }

    public static String getSwipeRightAction(Context context) {
        return prefs(context).getString(PREF_SWIPE_RIGHT, SWIPE_RIGHT_NONE);
    }

    public static boolean isSwipeRightQuickGlance(Context context) {
        return SWIPE_RIGHT_QUICK_GLANCE.equals(getSwipeRightAction(context));
    }

    /** Apply swipe-right preference to the -1 / Quick Glance overlay gate.
     * When enabled and gd.app.hiboard is installed, OverlayCombine binds the remote
     * WindowServer (OPPO-style); otherwise falls back to in-process MinuscreenView. */
    public static void applySwipeRightPreference(Context context, String value) {
        boolean enableGlance = SWIPE_RIGHT_QUICK_GLANCE.equals(value);
        OverlayPreference.get(context).setMinusEnabled(enableGlance);
    }

    public static void syncSwipeRightFromOverlay(Context context) {
        SharedPreferences p = prefs(context);
        if (!p.contains(PREF_SWIPE_RIGHT)) {
            boolean enabled = OverlayPreference.get(context).getMinusEnabled();
            p.edit().putString(PREF_SWIPE_RIGHT,
                    enabled ? SWIPE_RIGHT_QUICK_GLANCE : SWIPE_RIGHT_NONE).apply();
        }
    }

    /**
     * Keep SharedPreferences swipe-right and OverlayPreference.minusEnabled aligned.
     * Call from launcher start so a stale "none" default cannot leave Quick Glance
     * enabled in overlay prefs (or vice versa) after reboot.
     */
    public static void reconcileSwipeRightWithOverlay(Context context) {
        syncSwipeRightFromOverlay(context);
        applySwipeRightPreference(context, getSwipeRightAction(context));
    }

    public static CharSequence labelForSwipeDown(Context context, String value) {
        if (SWIPE_DOWN_NOTIFICATION.equals(value)) {
            return context.getString(R.string.home_swipe_down_notification);
        }
        return context.getString(R.string.home_swipe_down_global_search);
    }

    public static CharSequence labelForSwipeRight(Context context, String value) {
        if (SWIPE_RIGHT_QUICK_GLANCE.equals(value)) {
            return context.getString(R.string.home_swipe_right_quick_glance);
        }
        return context.getString(R.string.home_swipe_right_none);
    }

    public static boolean launchGlobalSearch(Context context) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setComponent(new ComponentName(QUICK_SEARCH_PACKAGE, QUICK_SEARCH_ACTIVITY));
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PackageManager pm = context.getPackageManager();
        if (intent.resolveActivity(pm) == null) {
            // Fallback: package launcher activity.
            intent = pm.getLaunchIntentForPackage(QUICK_SEARCH_PACKAGE);
            if (intent == null) {
                Log.w(TAG, "Global Search package not found: " + QUICK_SEARCH_PACKAGE);
                return false;
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        try {
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Failed to launch Global Search", e);
            return false;
        }
    }
}
