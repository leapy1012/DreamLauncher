package com.android.customize.iconfallen

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.PathInterpolator
import android.widget.Toast
import com.android.launcher3.BubbleTextView
import com.android.launcher3.CellLayout
import com.android.launcher3.Hotseat
import com.android.launcher3.Launcher
import com.android.launcher3.R
import com.android.launcher3.Utilities
import com.android.launcher3.folder.FolderIcon
import com.android.launcher3.folder.large.LargeFolderIcon
import com.android.launcher3.folder.large.LargeFolderProxy
import com.android.launcher3.folder.large.listview.LargeFolderIconItem
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.touch.ItemClickHandler
import com.android.launcher3.widget.LauncherAppWidgetHostView
import kotlin.math.max
import kotlin.math.min

/**
 * Home-page Icon Fallen animation (ColorOS Icon pull-down v1).
 * Packs current-page icons toward a bottom corner; hover-scales; launches on release.
 */
class IconFallenAnimationManager(private val launcher: Launcher) {

    companion object {
        const val DIRECTION_LEFT = 0
        const val DIRECTION_RIGHT = 1
        private const val FINISH_SCALE = 0.92f
        private const val FOCUS_SCALE_DELTA = 0.1f
        private const val WIDGET_SCALE = 0.75f
        private const val RESET_DURATION_MS = 330L
        private val FALL_INTERPOLATOR = PathInterpolator(0.33f, 0f, 0.1f, 1f)
        private val RESET_INTERPOLATOR = DecelerateInterpolator()
    }

    private data class FallenItem(
        val view: View,
        val targetTx: Float,
        val targetTy: Float,
        val endScale: Float,
        val fade: Boolean,
        val isLaunchable: Boolean,
    )

    private val fallenItems = ArrayList<FallenItem>()
    private var page: CellLayout? = null
    private var hotseat: Hotseat? = null
    private var pageIndicator: View? = null
    private var workspaceOffsetY = 0f
    private var hotseatOffsetY = 0f
    private var maxDistance = 0f
    private var finishScale = FINISH_SCALE
    private var focusScale = FINISH_SCALE + FOCUS_SCALE_DELTA
    private var progress = 0f
    private var prepared = false
    private var complete = false
    private var lastTouchedView: View? = null
    private var focusedView: View? = null
    private var autoAnimator: ValueAnimator? = null
    private var resetAnimator: AnimatorSet? = null
    private var savedPageClipChildren = true
    private var savedPageClipToPadding = true
    private var savedContainerClipChildren = true
    private var savedWorkspaceClipChildren = true
    private var savedWorkspaceClipToPadding = true
    private var clipOverrideActive = false
    private val tmpLoc = IntArray(2)

    fun isPrepared(): Boolean = prepared
    fun isComplete(): Boolean = complete
    fun getLastTouchedView(): View? = lastTouchedView
    fun getProgress(): Float = progress

    fun cancelAutoFallen() {
        autoAnimator?.cancel()
        autoAnimator = null
    }

    fun forceEndReset() {
        resetAnimator?.end()
        resetAnimator = null
    }

    /**
     * Snapshot current page icons and compute pack-toward-corner targets.
     * @return false if there is nothing to fall.
     */
    fun prepare(direction: Int): Boolean {
        forceEndReset()
        cancelAutoFallen()
        clearTransformsImmediate()
        fallenItems.clear()
        prepared = false
        complete = false
        progress = 0f
        lastTouchedView = null
        focusedView = null

        val workspace = launcher.workspace ?: return false
        val cell = workspace.getPageAt(workspace.currentPage) as? CellLayout ?: return false
        page = cell
        hotseat = launcher.hotseat
        pageIndicator = workspace.pageIndicator as? View
        val dp = launcher.deviceProfile
        val res = launcher.resources
        maxDistance = res.getDimension(R.dimen.icon_fallen_max_distance)
        workspaceOffsetY = res.getDimension(R.dimen.icon_fallen_workspace_translation_y)
        hotseatOffsetY = res.getDimension(R.dimen.icon_fallen_hotseat_translation_y)
        finishScale = FINISH_SCALE
        focusScale = finishScale + FOCUS_SCALE_DELTA

        val cols = dp.inv.numColumns
        val rows = dp.inv.numRows
        if (cols <= 0 || rows <= 0) return false

        val container = cell.shortcutsAndWidgets
        val children = ArrayList<Pair<View, ItemInfo>>()
        for (i in 0 until container.childCount) {
            val v = container.getChildAt(i)
            val info = v.tag as? ItemInfo ?: continue
            children.add(v to info)
        }
        if (children.isEmpty()) return false

        val screenW = dp.widthPx.toFloat()
        val screenH = dp.heightPx.toFloat()
        val padLR = res.getDimension(R.dimen.icon_fallen_padding_left_right)
        val padBottom = res.getDimension(R.dimen.icon_fallen_padding_bottom)
        val padTop = res.getDimension(R.dimen.icon_fallen_padding_top)
        val gap = res.getDimension(R.dimen.icon_fallen_item_default_gap)
        // Oppo compact cell step: scaled icon size + default gap.
        val compact = dp.iconSizePx * finishScale + gap
        val iconPadX = (dp.cellWidthPx - dp.iconSizePx) / 2f
        val navOffset = if (dp.insets.bottom > 0) {
            res.getDimension(R.dimen.icon_fallen_offset_have_navigation_bar_padding_bottom)
        } else {
            0f
        }
        val rtl = Utilities.isRtl(res)
        val leftDir = if (rtl) direction == DIRECTION_RIGHT else direction == DIRECTION_LEFT

        val origX = FloatArray(cols) { Float.NaN }
        val origY = FloatArray(rows) { Float.NaN }
        var firstLine = rows
        var lastLine = -1
        for ((v, info) in children) {
            val cx = info.cellX
            val cy = info.cellY
            if (cx in 0 until cols && origX[cx].isNaN()) {
                v.getLocationInWindow(tmpLoc)
                origX[cx] = tmpLoc[0].toFloat()
            }
            if (cy in 0 until rows) {
                if (origY[cy].isNaN()) {
                    v.getLocationInWindow(tmpLoc)
                    origY[cy] = tmpLoc[1].toFloat()
                }
                if (cy < firstLine) firstLine = cy
                var bottom = cy + max(1, info.spanY) - 1
                if (bottom >= rows) bottom = rows - 1
                if (bottom > lastLine) lastLine = bottom
            }
        }
        if (lastLine < 0) {
            firstLine = 0
            lastLine = rows - 1
        }

        val endX = FloatArray(cols)
        val endY = FloatArray(rows)
        for (c in 0 until cols) {
            endX[c] = if (leftDir) {
                (c * compact) + (padLR - iconPadX)
            } else {
                screenW - (((cols - c) * compact) + (padLR + iconPadX))
            }
        }
        // Tentative pack from screen bottom (Oppo sEndPositionMaxLayoutY).
        for (r in 0 until rows) {
            endY[r] = screenH - (((rows - r) * compact) + padBottom)
        }

        // Oppo boundaryCheck, with an extra clamp to the CellLayout window bottom so
        // the last occupied row cannot land past the page clip edge.
        cell.getLocationInWindow(tmpLoc)
        val pageBottomLimit = (tmpLoc[1] + cell.height).toFloat()
        val bottomLimit = min(screenH - padBottom, pageBottomLimit)
        applyTopBoundaryCheck(endY, firstLine, padTop)
        applyBottomBoundaryCheck(endY, lastLine, compact, bottomLimit)

        // Oppo: clamp workspace page offset; icon Y subtracts it so the page
        // translation carries most of the downward shift.
        val maxUp = -res.getDimension(R.dimen.icon_fallen_workspace_max_y_offset)
        val minUp = res.getDimension(R.dimen.icon_fallen_workspace_min_y_offset)
        workspaceOffsetY = workspaceOffsetY.coerceIn(maxUp, minUp)

        val pivotY = if (rows <= 4) {
            res.getDimension(R.dimen.icon_fallen_scaley_center_row_grid_4)
        } else {
            res.getDimension(R.dimen.icon_fallen_scaley_center)
        }

        // Oppo folder path disables clipChildren while falling; workspace needs the
        // same or lower rows are sliced by CellLayout even with correct end Y.
        enableFallenClipOverride(cell)

        for ((v, info) in children) {
            val cx = info.cellX
            val cy = info.cellY
            if (cx !in 0 until cols || cy !in 0 until rows) continue
            if (origX[cx].isNaN() || origY[cy].isNaN()) continue

            val isLaunchable = v is BubbleTextView || v is FolderIcon
            val isWidget = v is LauncherAppWidgetHostView || (!isLaunchable && info.spanX * info.spanY > 1)
            val tx = endX[cx] - origX[cx]
            // Match Oppo: (endY - originalY) - workspaceOffset - navOffset.
            val ty = (endY[cy] - origY[cy]) - workspaceOffsetY - navOffset
            val scale = if (isWidget) WIDGET_SCALE else finishScale

            v.pivotX = v.width / 2f
            v.pivotY = if (v is BubbleTextView || v is FolderIcon) pivotY else v.height / 2f

            fallenItems.add(
                FallenItem(
                    view = v,
                    targetTx = tx,
                    targetTy = ty,
                    endScale = scale,
                    fade = isWidget,
                    isLaunchable = isLaunchable,
                )
            )
        }

        prepared = fallenItems.isNotEmpty()
        if (!prepared) {
            restoreFallenClipOverride()
        }
        return prepared
    }

    /** Oppo {@code topBoundaryCheck}: shift stack down if first line is above padTop. */
    private fun applyTopBoundaryCheck(endY: FloatArray, firstLine: Int, padTop: Float) {
        if (firstLine !in endY.indices) return
        val deficit = endY[firstLine] - padTop
        if (deficit >= 0f) return
        for (i in endY.indices) {
            endY[i] -= deficit
        }
    }

    /**
     * Oppo {@code bottomBoundaryCheck}: if the last occupied row would sit past
     * {@code bottomLimit}, shift the entire end-Y stack upward.
     */
    private fun applyBottomBoundaryCheck(
        endY: FloatArray,
        lastLine: Int,
        compact: Float,
        bottomLimit: Float,
    ) {
        if (lastLine !in endY.indices) return
        val overflow = (endY[lastLine] + compact) - bottomLimit
        if (overflow <= 0f) return
        for (i in endY.indices) {
            endY[i] -= overflow
        }
    }

    private fun enableFallenClipOverride(cell: CellLayout) {
        if (clipOverrideActive) return
        savedPageClipChildren = cell.clipChildren
        savedPageClipToPadding = cell.clipToPadding
        cell.clipChildren = false
        cell.clipToPadding = false
        val container = cell.shortcutsAndWidgets
        savedContainerClipChildren = container.clipChildren
        container.clipChildren = false
        container.clipToPadding = false
        launcher.workspace?.let { ws ->
            savedWorkspaceClipChildren = ws.clipChildren
            savedWorkspaceClipToPadding = ws.clipToPadding
            ws.clipChildren = false
            ws.clipToPadding = false
        }
        clipOverrideActive = true
    }

    private fun restoreFallenClipOverride() {
        if (!clipOverrideActive) return
        page?.let { cell ->
            cell.clipChildren = savedPageClipChildren
            cell.clipToPadding = savedPageClipToPadding
            val container = cell.shortcutsAndWidgets
            container.clipChildren = savedContainerClipChildren
            container.clipToPadding = savedContainerClipChildren
        }
        launcher.workspace?.let { ws ->
            ws.clipChildren = savedWorkspaceClipChildren
            ws.clipToPadding = savedWorkspaceClipToPadding
        }
        clipOverrideActive = false
    }

    fun iconFallenWithDistance(direction: Int, distance: Float) {
        if (!prepared && !prepare(direction)) {
            return
        }
        val threshold = if (maxDistance > 0f) maxDistance else 1f
        val t = FALL_INTERPOLATOR.getInterpolation(min(1f, max(0f, distance / threshold)))
        applyProgress(t)
    }

    fun iconFallenAuto(direction: Int, currentRadio: Float, onComplete: Runnable) {
        cancelAutoFallen()
        if (!prepared && !prepare(direction)) {
            onComplete.run()
            return
        }
        val start = progress.coerceIn(0f, 1f).let {
            // currentRadio is remaining distance ratio from Oppo (1 - last/max).
            max(it, 1f - currentRadio.coerceIn(0f, 1f))
        }
        val duration = (80 + (220 * (1f - start))).toLong()
        autoAnimator = ValueAnimator.ofFloat(start, 1f).apply {
            this.duration = duration
            interpolator = FALL_INTERPOLATOR
            addUpdateListener { applyProgress(it.animatedValue as Float) }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onIconFallenComplete()
                    onComplete.run()
                }
            })
            start()
        }
    }

    fun onIconFallenComplete() {
        applyProgress(1f)
        complete = true
        pageIndicator?.alpha = 0f
        launcher.dragLayer.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
    }

    fun iconFallenScale(x: Float, y: Float) {
        if (!complete) return
        var found: View? = null
        for (item in fallenItems) {
            if (!item.isLaunchable) continue
            val v = item.view
            v.getLocationInWindow(tmpLoc)
            val scale = v.scaleX
            val w = v.width * scale
            val h = v.height * scale
            // Bias hit box toward the icon glyph (top portion of BubbleTextView).
            val hitH = if (v is BubbleTextView) min(h, launcher.deviceProfile.iconSizePx * scale * 1.35f) else h
            if (x >= tmpLoc[0] && x <= tmpLoc[0] + w && y >= tmpLoc[1] && y <= tmpLoc[1] + hitH) {
                // Oppo handlerTouchView → clickBigFolderItemIcon: large-folder plate
                // resolves to a preview cell (launch) or the folder (open / overflow).
                found = if (v is LargeFolderIcon && LargeFolderProxy.isLargeFolder(v)) {
                    v.resolveFallenPreviewTarget(x, y) ?: v
                } else {
                    v
                }
                break
            }
        }
        if (found == focusedView) {
            lastTouchedView = found
            return
        }
        clearFocus(animated = true)
        focusedView = found
        lastTouchedView = found
        if (found != null) {
            val targetScale = focusScaleFor(found)
            found.animate()
                .scaleX(targetScale)
                .scaleY(targetScale)
                .setDuration(100)
                .start()
        }
    }

    /**
     * Preview cells live inside an already-fallen folder; bump them relative to 1f.
     * Workspace icons / small folders use the packed finish scale + focus delta.
     */
    private fun focusScaleFor(view: View): Float {
        return if (view is LargeFolderIconItem) 1f + FOCUS_SCALE_DELTA else focusScale
    }

    private fun restingScaleFor(view: View): Float {
        return when {
            view is LargeFolderIconItem -> 1f
            complete -> finishScale
            else -> 1f
        }
    }

    fun performFallenIconClick() {
        val target = lastTouchedView
        lastTouchedView = null
        clearFocus(animated = false)
        if (target != null && complete) {
            ItemClickHandler.onClick(target)
        }
    }

    fun resetFallenIcons(animate: Boolean) {
        cancelAutoFallen()
        // Always end an in-flight reset first — immediate clear can otherwise be
        // overwritten when the old AnimatorSet keeps running.
        forceEndReset()
        clearFocus(animated = false)
        lastTouchedView = null
        complete = false
        if (!prepared && fallenItems.isEmpty()) {
            clearTransformsImmediate()
            restoreFallenClipOverride()
            prepared = false
            progress = 0f
            return
        }
        if (!animate) {
            clearTransformsImmediate()
            restoreFallenClipOverride()
            fallenItems.clear()
            prepared = false
            progress = 0f
            return
        }
        val set = AnimatorSet()
        val anims = ArrayList<Animator>()
        page?.let { cell ->
            anims.add(ObjectAnimator.ofFloat(cell, View.TRANSLATION_Y, 0f))
        }
        hotseat?.let { hs ->
            anims.add(ObjectAnimator.ofFloat(hs, View.TRANSLATION_Y, 0f))
            anims.add(ObjectAnimator.ofFloat(hs, View.ALPHA, 1f))
        }
        pageIndicator?.let { pi ->
            anims.add(ObjectAnimator.ofFloat(pi, View.TRANSLATION_Y, 0f))
            anims.add(ObjectAnimator.ofFloat(pi, View.ALPHA, 1f))
        }
        for (item in fallenItems) {
            val v = item.view
            anims.add(ObjectAnimator.ofFloat(v, View.TRANSLATION_X, 0f))
            anims.add(ObjectAnimator.ofFloat(v, View.TRANSLATION_Y, 0f))
            anims.add(ObjectAnimator.ofFloat(v, View.SCALE_X, 1f))
            anims.add(ObjectAnimator.ofFloat(v, View.SCALE_Y, 1f))
            anims.add(ObjectAnimator.ofFloat(v, View.ALPHA, 1f))
            when (v) {
                is BubbleTextView -> v.setTextAlpha(1f)
                is FolderIcon -> v.folderName?.setTextAlpha(1f)
            }
        }
        set.playTogether(anims)
        set.duration = RESET_DURATION_MS
        set.interpolator = RESET_INTERPOLATOR
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                restoreFallenClipOverride()
                fallenItems.clear()
                prepared = false
                progress = 0f
                resetAnimator = null
            }
        })
        resetAnimator = set
        set.start()
    }

    fun showNoIconToast() {
        Toast.makeText(launcher, R.string.launcher_icon_fallen_toast, Toast.LENGTH_SHORT).show()
    }

    fun recycle() {
        cancelAutoFallen()
        forceEndReset()
        clearTransformsImmediate()
        restoreFallenClipOverride()
        fallenItems.clear()
        prepared = false
        complete = false
        page = null
        hotseat = null
        pageIndicator = null
    }

    private fun applyProgress(t: Float) {
        progress = t.coerceIn(0f, 1f)
        val alpha = max(0f, 1f - 2f * progress)
        page?.translationY = workspaceOffsetY * progress
        hotseat?.let {
            it.translationY = hotseatOffsetY * progress
            it.alpha = alpha
        }
        pageIndicator?.let {
            it.translationY = hotseatOffsetY * progress
            it.alpha = alpha
        }
        for (item in fallenItems) {
            val v = item.view
            v.translationX = item.targetTx * progress
            v.translationY = item.targetTy * progress
            val scale = 1f + (item.endScale - 1f) * progress
            v.scaleX = scale
            v.scaleY = scale
            if (item.fade) {
                v.alpha = alpha
            }
            when (v) {
                is BubbleTextView -> v.setTextAlpha(alpha)
                is FolderIcon -> v.folderName?.setTextAlpha(alpha)
            }
        }
    }

    private fun clearFocus(animated: Boolean) {
        val prev = focusedView ?: return
        focusedView = null
        val resting = restingScaleFor(prev)
        if (animated) {
            prev.animate().scaleX(resting).scaleY(resting).setDuration(100).start()
        } else {
            prev.animate().cancel()
            prev.scaleX = resting
            prev.scaleY = resting
        }
    }

    private fun clearTransformsImmediate() {
        page?.translationY = 0f
        hotseat?.let {
            it.translationY = 0f
            it.alpha = 1f
        }
        pageIndicator?.let {
            it.translationY = 0f
            it.alpha = 1f
        }
        for (item in fallenItems) {
            val v = item.view
            v.animate().cancel()
            v.translationX = 0f
            v.translationY = 0f
            v.scaleX = 1f
            v.scaleY = 1f
            v.alpha = 1f
            when (v) {
                is BubbleTextView -> v.setTextAlpha(1f)
                is FolderIcon -> v.folderName?.setTextAlpha(1f)
            }
        }
    }
}
