package com.android.launcher3.config

import com.android.launcher3.config.FeatureFlags.FlagState
import com.android.launcher3.uioverrides.flags.FlagsFactory

/**
 * Debug/developer toggles for customize features.
 *
 * Product-facing gates: prefer [DreamFeatureOption] (Oppo FeatureOption pattern).
 * These flags feed that matrix for overlay minus/plus.
 */
object CustomizeFeatureFlags {
    @JvmField
    val ENABLE_OVERLAY_MINUS: FeatureFlags.BooleanFlag = FlagsFactory.getDebugFlag(
        0, "ENABLE_OVERLAY_MINUS", FlagState.ENABLED,
        "ENABLE_OVERLAY_MINUS"
    )

    @JvmField
    val ENABLE_OVERLAY_PLUS: FeatureFlags.BooleanFlag = FlagsFactory.getDebugFlag(
        0, "ENABLE_OVERLAY_PLUS", FlagState.DISABLED,
        "ENABLE_OVERLAY_PLUS"
    )
}