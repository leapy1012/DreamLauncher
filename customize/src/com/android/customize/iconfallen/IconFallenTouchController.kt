package com.android.customize.iconfallen

import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.android.launcher3.AbstractFloatingView
import com.android.launcher3.Launcher
import com.android.launcher3.LauncherState
import com.android.launcher3.R
import com.android.launcher3.folder.FolderIcon
import com.android.launcher3.folder.large.listview.LargeFolderIconItem
import com.android.launcher3.settings.HomeScreenGestures
import com.android.launcher3.touch.ItemClickHandler
import com.android.launcher3.util.TouchController
import java.io.PrintWriter
import kotlin.math.abs

/**
 * Edge-up Icon pull-down gesture (ColorOS Icon Fallen).
 *
 * Swipe up from the left/right strip on the home screen to pack icons toward that
 * bottom corner; slide onto an icon and release to launch.
 */
class IconFallenTouchController(private val launcher: Launcher) : TouchController {

    companion object {
        private const val AUTO_FALLEN_JUMP_PX = 10f
    }

    private val anim = IconFallenAnimationManager(launcher)
    private val touchSlop: Int =
        ViewConfiguration.get(launcher).scaledTouchSlop
    private val edgeThreshold: Int =
        launcher.resources.getDimensionPixelSize(R.dimen.icon_fallen_tric_distance)
    private val maxDistance: Int =
        launcher.resources.getDimensionPixelSize(R.dimen.icon_fallen_max_distance)

    private var noIntercept = true
    private var tracking = false
    private var falling = false
    private var direction = IconFallenAnimationManager.DIRECTION_LEFT
    private var downX = 0f
    private var downY = 0f
    private var triggerDownY = 0f
    private var lastDistance = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var vibrated = false
    private var autoFallen = false
    private var resetDone = true

    fun isActive(): Boolean = falling || anim.isComplete() || anim.isPrepared()

    fun resetFallenIcons(animate: Boolean = true) {
        if (resetDone && !anim.isPrepared() && !anim.isComplete()) return
        anim.cancelAutoFallen()
        anim.resetFallenIcons(animate)
        falling = false
        tracking = false
        autoFallen = false
        vibrated = false
        lastDistance = 0f
        resetDone = true
    }

    override fun onControllerInterceptTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.actionMasked
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                noIntercept = true
                tracking = false
                falling = false
                autoFallen = false
                vibrated = false
                lastDistance = 0f
                downX = ev.x
                downY = ev.y
                lastTouchX = downX
                lastTouchY = downY

                if (anim.isComplete()) {
                    // Only keep owning the stream while still on home. Leaving NORMAL
                    // (All Apps / Overview) must clear fallen state — never eat drawer taps.
                    if (!launcher.isInState(LauncherState.NORMAL)
                        || !HomeScreenGestures.isIconFallenEnabled(launcher)
                    ) {
                        resetFallenIcons(false)
                        return false
                    }
                    noIntercept = false
                    falling = true
                    return true
                }
                // End any in-flight reset before considering a new gesture.
                if (anim.isPrepared()) {
                    anim.forceEndReset()
                    anim.resetFallenIcons(false)
                    resetDone = true
                }
                if (!canStartGesture(ev)) {
                    return false
                }
                if (shouldIconFallen(downX)) {
                    noIntercept = false
                    tracking = true
                }
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                if (noIntercept) return false
                if (anim.isComplete() || falling) {
                    // Drop ownership if we left home mid-gesture.
                    if (!launcher.isInState(LauncherState.NORMAL)) {
                        resetFallenIcons(false)
                        noIntercept = true
                        return false
                    }
                    return true
                }
                if (!tracking) return false
                if (ev.pointerCount > 1) {
                    noIntercept = true
                    tracking = false
                    return false
                }
                val dx = abs(ev.x - downX)
                val dy = downY - ev.y
                if (dy > touchSlop && dy > dx) {
                    // Claim the gesture: edge-up past slop.
                    falling = true
                    triggerDownY = downY
                    resetDone = false
                    if (!anim.prepare(direction)) {
                        anim.showNoIconToast()
                        resetFallenIcons(false)
                        noIntercept = true
                        return false
                    }
                    return true
                }
                if (dx > touchSlop && dx > abs(dy)) {
                    // Horizontal — yield to workspace paging.
                    noIntercept = true
                    tracking = false
                }
                return false
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (anim.isComplete()) {
                    return true
                }
                tracking = false
                falling = false
            }
        }
        return false
    }

    override fun onControllerTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (!handleMove(ev)) return false
                lastTouchX = ev.x
                lastTouchY = ev.y
            }
            MotionEvent.ACTION_UP -> touchUp()
            MotionEvent.ACTION_CANCEL -> resetFallenIcons(true)
        }
        return true
    }

    override fun dump(prefix: String, writer: PrintWriter) {
        writer.println(
            prefix + "IconFallen tracking=$tracking falling=$falling complete=${anim.isComplete()}" +
                " dir=$direction enabled=${HomeScreenGestures.isIconFallenEnabled(launcher)}"
        )
    }

    private fun handleMove(ev: MotionEvent): Boolean {
        if (!launcher.isInState(LauncherState.NORMAL)) {
            resetFallenIcons(false)
            return false
        }
        val workspace = launcher.workspace ?: return false
        if (workspace.isPageInTransitionForEffect) {
            return false
        }
        if (autoFallen && !anim.isComplete()) {
            return true
        }
        if (anim.isComplete()) {
            val movedX = abs(ev.x - lastTouchX) > touchSlop
            val movedY = abs(ev.y - lastTouchY) > touchSlop
            if (movedX || movedY) {
                anim.iconFallenScale(ev.x, ev.y)
            }
            return true
        }
        if (!falling) return true

        resetDone = false
        val distance = (triggerDownY - ev.y).toInt()
        if (distance <= 0) return true

        // Fast upward jump → auto-complete spring.
        if (distance - lastDistance > AUTO_FALLEN_JUMP_PX && !vibrated) {
            autoFallen = true
            if (lastDistance == 0f) {
                anim.iconFallenWithDistance(direction, 0f)
            }
            anim.iconFallenAuto(direction, 1f - lastDistance / maxDistance) {
                // complete callback already marks complete inside manager
            }
            return true
        }

        if (distance >= maxDistance || vibrated) {
            anim.iconFallenWithDistance(direction, maxDistance.toFloat())
            if (!vibrated) {
                vibrated = true
                anim.onIconFallenComplete()
            }
            lastDistance = 0f
        } else {
            vibrated = false
            anim.iconFallenWithDistance(direction, distance.toFloat())
            lastDistance = distance.toFloat()
        }
        return true
    }

    private fun touchUp() {
        if (!anim.isComplete()) {
            resetFallenIcons(true)
            return
        }
        val target = anim.getLastTouchedView()
        // Oppo clickBigFolderItemIcon:
        //   preview app  → BigFolderItemIcon / LargeFolderIconItem → launch
        //   overflow     → FlexibleFolderIcon / FolderIcon → open
        // Delay fall-reset for folder open; clear instantly first so animateOpen()
        // is not raced by an animated reset (scrim / centerAboutIcon).
        when (target) {
            is FolderIcon -> {
                resetFallenIcons(false)
                ItemClickHandler.onClick(target)
            }
            is LargeFolderIconItem -> {
                if (target.isCountOut()) {
                    val folder = findParentFolderIcon(target)
                    resetFallenIcons(false)
                    if (folder != null) ItemClickHandler.onClick(folder)
                } else {
                    anim.performFallenIconClick()
                    resetFallenIcons(true)
                }
            }
            else -> {
                anim.performFallenIconClick()
                resetFallenIcons(true)
            }
        }
    }

    private fun findParentFolderIcon(view: View): FolderIcon? {
        var p = view.parent
        while (p != null) {
            if (p is FolderIcon) return p
            p = (p as? View)?.parent
        }
        return null
    }

    private fun canStartGesture(ev: MotionEvent): Boolean {
        if (!HomeScreenGestures.isIconFallenEnabled(launcher)) return false
        if (launcher.deviceProfile.isTablet) return false
        if (!launcher.isInState(LauncherState.NORMAL)) return false
        if (AbstractFloatingView.getTopOpenView(launcher) != null) return false
        val workspace = launcher.workspace ?: return false
        if (workspace.isPageInTransitionForEffect) return false
        if (workspace.isOverlayShown) return false
        if (workspace.scaleX != 1f) return false
        if (!launcher.isDraggingEnabled) return false
        // Yield the bottom gesture band to All Apps / home-to-overview.
        val insets = launcher.deviceProfile.insets
        val hotseatBand = launcher.deviceProfile.hotseatBarSizePx
        val bottomGuard = insets.bottom + hotseatBand
        if (ev.y > launcher.dragLayer.height - bottomGuard) return false
        return true
    }

    private fun shouldIconFallen(x: Float): Boolean {
        val width = launcher.deviceProfile.widthPx
        return when {
            x < edgeThreshold -> {
                direction = IconFallenAnimationManager.DIRECTION_LEFT
                true
            }
            x > width - edgeThreshold -> {
                direction = IconFallenAnimationManager.DIRECTION_RIGHT
                true
            }
            else -> false
        }
    }
}
