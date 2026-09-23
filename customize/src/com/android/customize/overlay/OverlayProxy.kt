package com.android.customize.overlay

import android.content.Context
import com.android.customize.common.logger.MyLogger
import com.android.customize.overlay.preference.OverlayPreference
import com.android.customize.overlay.quickglance.QuickGlanceLauncherClient
import com.android.customize.overlay.quickglance.QuickGlanceRemoteOverlay
import com.android.customize.overlay.ui.minus.MinuscreenView
import com.android.launcher3.CustomizeLauncher
import com.android.systemui.plugins.shared.LauncherOverlayManager.LauncherOverlayCallbacks

/**
 * Oppo-style [com.android.overlay.OverlayProxy]: choose minus-one implementation.
 *
 * Order: remote Quick Glance (AIDL) → in-process MinuscreenView → none.
 */
object OverlayProxy {

    fun createMinus(launcher: CustomizeLauncher): OverlayBase? {
        if (!OverlayPreference.get(launcher).minusEnabled) {
            myLogger.d("minus: none")
            return null
        }
        return when (resolveMinusKind(launcher)) {
            MinusKind.QUICK_GLANCE -> {
                myLogger.d("minus: QuickGlance remote AIDL")
                QuickGlanceRemoteOverlay(launcher).also { it.addView(launcher) }
            }
            MinusKind.LOCAL -> {
                myLogger.d("minus: local MinuscreenView (DQG not installed)")
                val view = MinuscreenView(launcher)
                OverlayDragSource(view).also { it.addView(launcher) }
            }
            MinusKind.NONE -> null
        }
    }

    fun resolveMinusKind(context: Context): MinusKind {
        if (!OverlayPreference.get(context).minusEnabled) return MinusKind.NONE
        return if (QuickGlanceLauncherClient.isPackageAvailable(context)) {
            MinusKind.QUICK_GLANCE
        } else {
            MinusKind.LOCAL
        }
    }

    /** Wire local minus progress after [OverlayCombine] has callbacks. */
    fun wireLocalMinusProgress(minus: OverlayBase?, callbacks: LauncherOverlayCallbacks?) {
        val drag = minus as? OverlayDragSource ?: return
        val view = drag.decorView as? MinuscreenView ?: return
        view.observeProgress { progress ->
            callbacks?.onOverlayScrollChanged(progress)
        }
    }

    enum class MinusKind {
        QUICK_GLANCE,
        LOCAL,
        NONE,
    }

    private val myLogger = MyLogger("OverlayProxy")
}
