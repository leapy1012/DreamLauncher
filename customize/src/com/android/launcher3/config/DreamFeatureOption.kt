package com.android.launcher3.config

import android.os.SystemProperties
import com.android.launcher3.HxyOption

/**
 * Product feature matrix (Oppo [com.android.common.config.FeatureOption] style).
 *
 * Single place for DreamLauncher ColorOS-parity gates. Prefer this over scattering
 * [SystemProperties] / [HxyOption] / [CustomizeFeatureFlags] reads at call sites.
 *
 * Runtime user prefs (swipe-right, minus enabled) stay in SharedPreferences /
 * [com.android.customize.overlay.preference.OverlayPreference] — this matrix only
 * answers "is the product capable / compiled-in".
 */
object DreamFeatureOption {

    // ---- Folders ------------------------------------------------------------

    /** ColorOS-style large / big folder (2×2 preview, expand, switch size). */
    @JvmField
    val isSupportLargeFolder: Boolean = HxyOption.HXY_LAUNCHER_SUPPORT_LARGE_FOLDER

    // ---- Overlay / Quick Glance --------------------------------------------

    /** Minus-one (Quick Glance remote AIDL or local Minuscreen). */
    @JvmStatic
    fun isSupportOverlayMinus(): Boolean = CustomizeFeatureFlags.ENABLE_OVERLAY_MINUS.get()

    /** Plus-one / category panel overlay. */
    @JvmStatic
    fun isSupportOverlayPlus(): Boolean = CustomizeFeatureFlags.ENABLE_OVERLAY_PLUS.get()

    /** Swipe-right → Quick Glance is allowed only when minus overlay is product-enabled. */
    @JvmStatic
    fun isSupportQuickGlanceGesture(): Boolean = isSupportOverlayMinus()

    // ---- Chrome / theming --------------------------------------------------

    /** Theme-pack icon masking (`persist.sys.hxy_theme_icon`). */
    @JvmField
    val isSupportThemeIcon: Boolean =
        SystemProperties.getInt("persist.sys.hxy_theme_icon", 0) == 1

    /** Launcher style picker in settings (`ro.launcher.style`). */
    @JvmField
    val isSupportLauncherStyle: Boolean =
        SystemProperties.getBoolean("ro.launcher.style", false)

    /** Transparent All Apps scrim (`ro.launcher.allapp.bgtransp`). */
    @JvmField
    val isSupportAllAppsTransparentBg: Boolean =
        SystemProperties.getBoolean("ro.launcher.allapp.bgtransp", false)

    // ---- System UI integration ---------------------------------------------

    /** Taskbar / desktop mode chrome (`persist.sys.taskbar.enable`). */
    @JvmField
    val isSupportTaskbar: Boolean =
        SystemProperties.get("persist.sys.taskbar.enable", "1") != "0"

    /** Overview depth wallpaper blur (`ro.launcher.depth.overview`). */
    @JvmField
    val isSupportOverviewDepth: Boolean =
        SystemProperties.getBoolean("ro.launcher.depth.overview", true)

    /** Widget depth blur (`ro.launcher.depth.widget`). */
    @JvmField
    val isSupportWidgetDepth: Boolean =
        SystemProperties.getBoolean("ro.launcher.depth.widget", true)
}
