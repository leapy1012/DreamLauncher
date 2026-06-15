package com.android.launcher3.config

import com.android.launcher3.config.FeatureFlags.FlagState
import com.android.launcher3.uioverrides.flags.FlagsFactory

object CustomizeFeatureFlags {
    @JvmField
    val ENABLE_OVERLAY_MINUS: FeatureFlags.BooleanFlag = FlagsFactory.getDebugFlag(
        0, "ENABLE_OVERLAY_MINUS", FlagState.ENABLED,
        "ENABLE_OVERLAY_MINUS"
    )

    @JvmField
    val ENABLE_OVERLAY_PLUS: FeatureFlags.BooleanFlag = FlagsFactory.getDebugFlag(
        0, "ENABLE_OVERLAY_PLUS", FlagState.ENABLED,
        "ENABLE_OVERLAY_PLUS"
    )
}