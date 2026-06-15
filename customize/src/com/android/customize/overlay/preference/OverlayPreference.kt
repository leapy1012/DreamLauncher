package com.android.customize.overlay.preference

import android.content.Context
import com.android.customize.common.extension.getAsFlow
import com.android.launcher3.LauncherPrefs
import com.android.launcher3.R
import com.android.launcher3.config.CustomizeFeatureFlags
import com.android.launcher3.util.MainThreadInitializedObject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class OverlayPreference private constructor(context: Context) {
    private val prefs = LauncherPrefs.get(context)

    val minusEnabled get() = prefs.get(MINUS_ENABLED)
            && CustomizeFeatureFlags.ENABLE_OVERLAY_MINUS.get()
    val minusEnabledFlow = prefs.getAsFlow(MINUS_ENABLED)
        .map {
            it && CustomizeFeatureFlags.ENABLE_OVERLAY_MINUS.get()
        }
    fun setMinusEnabled(enabled: Boolean) {
        prefs.put(MINUS_ENABLED, enabled)
    }
    val plusEnabled get() = prefs.get(PLUS_ENABLED)
            && CustomizeFeatureFlags.ENABLE_OVERLAY_PLUS.get()
    val plusEnabledFlow = prefs.getAsFlow(PLUS_ENABLED)
        .map {
            it && CustomizeFeatureFlags.ENABLE_OVERLAY_PLUS.get()
        }
    fun setPlusEnabled(enabled: Boolean) {
        prefs.put(PLUS_ENABLED, enabled)
    }

    val overlayEnabled get() = minusEnabled || plusEnabled
    val overlayEnabledFlow = combine(
        minusEnabledFlow, plusEnabledFlow
    ) { minus, plus -> minus || plus }

    companion object {
        @JvmField
        val INSTANCE = MainThreadInitializedObject { OverlayPreference(it) }

        @JvmStatic
        fun get(context: Context): OverlayPreference = INSTANCE.get(context)

        val MINUS_ENABLED = LauncherPrefs.backedUpItem(
            "pref_minusEnabled",
            Boolean::class.java, false,
        ) { it.resources.getBoolean(R.bool.config_minusEnabled) }

        val PLUS_ENABLED = LauncherPrefs.backedUpItem(
            "pref_plusEnabled",
            Boolean::class.java, false,
        ) { it.resources.getBoolean(R.bool.config_plusEnabled) }
    }
}