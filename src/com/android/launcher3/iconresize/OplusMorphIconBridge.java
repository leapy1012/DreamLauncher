package com.android.launcher3.iconresize;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.Nullable;

import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * Optional bridge to Oppo/ColorOS {@code uxicon} morph assets via reflection.
 * On MTK/AOSP devices where uxicon is absent, returns null and callers use the fallback drawable.
 */
final class OplusMorphIconBridge {

    private static final String TAG = "OplusMorphIconBridge";

    private static final String REFLECT_ENTRY =
            "com.oplus.uxicon.ui.morphicon.ReflectEntry";
    private static final String UX_HELPER =
            "com.oplus.uxicon.ui.util.UxIconLoaderHelper";
    private static final String LAUNCHER_ICON_CONFIG =
            "com.android.launcher.icons.theme.LauncherIconConfig";

    private static Boolean sUxIconAvailable;

    private OplusMorphIconBridge() {}

    static boolean isUxIconAvailable() {
        if (sUxIconAvailable == null) {
            try {
                Class.forName(REFLECT_ENTRY);
                sUxIconAvailable = true;
            } catch (Throwable t) {
                sUxIconAvailable = false;
            }
        }
        return sUxIconAvailable;
    }

    static boolean isSupportMorphUxIcon(@Nullable String packageName) {
        if (packageName == null || !isUxIconAvailable()) {
            return false;
        }
        try {
            Class<?> helper = Class.forName(UX_HELPER);
            Method m = helper.getMethod("isSupportMorphUxIcon", String.class);
            Object result = m.invoke(null, packageName);
            return result instanceof Boolean && (Boolean) result;
        } catch (Throwable t) {
            Log.d(TAG, "isSupportMorphUxIcon failed: " + t.getMessage());
            return false;
        }
    }

    @Nullable
    static Drawable loadMorphIcon(Context context, ComponentName componentName, int spanX,
            int spanY, int morphWidth, int morphHeight, float cornerRadius) {
        if (context == null || componentName == null || !isUxIconAvailable()) {
            return null;
        }
        try {
            Object iconConfig = getLauncherIconConfig();
            if (iconConfig == null) {
                return null;
            }
            Class<?> entry = Class.forName(REFLECT_ENTRY);
            Method create = entry.getMethod("createMorphIcon",
                    Context.class,
                    ComponentName.class,
                    int.class,
                    int.class,
                    iconConfig.getClass(),
                    boolean.class,
                    boolean.class,
                    long.class,
                    Float.class,
                    Float.class,
                    Integer.class,
                    Integer.class,
                    HashMap.class);
            Object drawable = create.invoke(null,
                    context,
                    componentName,
                    spanX,
                    spanY,
                    iconConfig,
                    false /* isThemedMonoIcon */,
                    false /* requestUpdateFromRemote */,
                    1000L /* iconDownloadTimeout */,
                    cornerRadius,
                    spanX == 2 && spanY == 2 ? 1.1f : null /* smoothWeight */,
                    morphWidth,
                    morphHeight,
                    null /* outExtras */);
            if (drawable instanceof Drawable d) {
                Log.d(TAG, "Loaded Oppo morph for " + componentName + " " + spanX + "x" + spanY);
                return d;
            }
        } catch (Throwable t) {
            Log.d(TAG, "loadMorphIcon failed: " + t.getMessage());
        }
        return null;
    }

    @Nullable
    private static Object getLauncherIconConfig() {
        try {
            Class<?> configClass = Class.forName(LAUNCHER_ICON_CONFIG);
            Method getInstance = configClass.getMethod("getInstance");
            Object instance = getInstance.invoke(null);
            Method getIconConfig = configClass.getMethod("getIconConfig");
            return getIconConfig.invoke(instance);
        } catch (Throwable t) {
            Log.d(TAG, "getLauncherIconConfig failed: " + t.getMessage());
            return null;
        }
    }
}
