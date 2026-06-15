package com.android.customize.overlay.ui.plus.page

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
import android.util.Pair
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import androidx.core.animation.addListener
import androidx.core.view.children
import androidx.core.view.isInvisible
import com.android.customize.overlay.ui.BasePageAnimator
import com.android.launcher3.BubbleTextView
import java.lang.ref.WeakReference
import kotlin.math.min

class CategoryMoreAnimator(
    private val srcPage: CategoryListPage,
    private val dstPage: CategoryMorePage
) : BasePageAnimator {

    private var isAnimating = false
    private var srcViewWR: WeakReference<ViewGroup>? = null

    override fun open(animate: Boolean, anchor: View?) {
        if (isAnimating) return
        val srcView = anchor as ViewGroup
        srcViewWR = WeakReference(srcView)

        resetTranslations(srcView)
        createOpenAnimator(srcView).apply {
            addListener(onStart = {
                isAnimating = true
                dstPage.isInvisible = false
            }, onEnd = {
                isAnimating = false
                srcPage.isInvisible = true
            })
            duration = if (animate) 350 else 0
            interpolator = OPEN_FOLDER
        }.start()
    }

    override fun close(animate: Boolean) {
        if (isAnimating) return
        val srcView = srcViewWR?.get() ?: return

        resetTranslations(srcView)
        createOpenAnimator(srcView).apply {
            addListener(onStart = {
                isAnimating = true
                srcPage.isInvisible = false
            }, onEnd = {
                isAnimating = false
                dstPage.isInvisible = true
            })
            duration = if (animate) 350 else 0
            interpolator = CLOSE_FOLDER
        }.reverse()
    }

    @SuppressLint("ObjectAnimatorBinding")
    @OptIn(ExperimentalStdlibApi::class)
    private fun createOpenAnimator(srcView: ViewGroup): AnimatorSet {
        val animatorSet = AnimatorSet()

        val srcPageAlpha = PropertyValuesHolder.ofFloat(
            "alpha", 1f, 0f
        )
        val srcPageAlphaAnim = ObjectAnimator.ofPropertyValuesHolder(
            srcPage, srcPageAlpha
        )
        animatorSet.play(srcPageAlphaAnim)

        val dstView = getDstView()
        if (dstView.childCount <= 0) {
            return animatorSet
        }

        val dstCornerViewCenters = getDstCornerViewCenters(dstView)
        val srcChildren = srcView.children
            .filter { it is BubbleTextView }.toList()
        for (i in srcChildren.indices) {
            val srcChildView = srcChildren[i]
            val srcChildViewCenter = getViewCenter(srcChildView)
            val dstChildViewCenter = dstCornerViewCenters[i]
            val deltaX = dstChildViewCenter.first - srcChildViewCenter.first
            val deltaY = dstChildViewCenter.second - srcChildViewCenter.second
            val translationX = PropertyValuesHolder.ofFloat(
                "translationX", 0f, deltaX.toFloat()
            )
            val translationY = PropertyValuesHolder.ofFloat(
                "translationY", 0f, deltaY.toFloat()
            )
            val srcTranslationAnim = ObjectAnimator.ofPropertyValuesHolder(
                srcChildView, translationX, translationY
            )

            animatorSet.play(srcTranslationAnim)
        }

        val dstPageAlpha = PropertyValuesHolder.ofFloat(
            "alpha", 0f, 1f
        )
        val dstPageAlphaAnim = ObjectAnimator.ofPropertyValuesHolder(
            dstPage, dstPageAlpha
        )
        animatorSet.play(dstPageAlphaAnim)

        val srvViewCenter = getViewCenter(srcView)
        val dstChildren = listOf(dstPage.name) + dstView.children
        for (childView in dstChildren) {
            val dstChildViewCenter = getViewCenter(childView)
            val deltaX = srvViewCenter.first - dstChildViewCenter.first
            val deltaY = srvViewCenter.second - dstChildViewCenter.second
            val translationX = PropertyValuesHolder.ofFloat(
                "translationX", deltaX.toFloat(), 0f
            )
            val translationY = PropertyValuesHolder.ofFloat(
                "translationY", deltaY.toFloat(), 0f
            )
            val dstTranslationAnim = ObjectAnimator.ofPropertyValuesHolder(
                childView, translationX, translationY
            )

            animatorSet.play(dstTranslationAnim)

            val scaleX = PropertyValuesHolder.ofFloat(
                "scaleX", 0.6f, 1f
            )
            val scaleY = PropertyValuesHolder.ofFloat(
                "scaleY", 0.6f, 1f
            )
            val scaleAnim = ObjectAnimator.ofPropertyValuesHolder(
                childView, scaleX, scaleY
            )
            animatorSet.play(scaleAnim)
        }

        return animatorSet
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun resetTranslations(srcView: ViewGroup) {
        val children = srcView.children
            .filter { it is BubbleTextView }.toList()
        for (i in children.indices) {
            val srcChildView = children[i]
            srcChildView.translationX = 0f
            srcChildView.translationY = 0f
        }
        val dstView = getDstView()
        for (i in 0..<dstView.childCount) {
            val dstChildView = dstView.getChildAt(i)
            dstChildView.translationX = 0f
            dstChildView.translationY = 0f
        }
    }

    private fun getDstCornerViewCenters(dstView: ViewGroup): Array<Pair<Int, Int>> {
        val children = dstView.children.toList()

        val spanCount = getSpanCount()

        val topStartChild = children[0]
        val topStartViewCenter = getViewCenter(topStartChild)

        val topEndChild = children[min(spanCount - 1, dstView.childCount - 1)]
        val topEndViewCenter = getViewCenter(topEndChild)

        val row = (dstView.childCount - 1) / spanCount
        val bottomStart = children[min(row * spanCount, dstView.childCount - 1)]
        val bottomStartViewCenter = getViewCenter(bottomStart)

        val bottomEndViewCenter = Pair.create(bottomStartViewCenter.first, topEndViewCenter.second)

        return arrayOf(
            topStartViewCenter,
            topEndViewCenter,
            bottomStartViewCenter,
            bottomEndViewCenter
        )
    }

    private fun getViewCenter(view: View): Pair<Int, Int> {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return Pair.create(
            location[0] + view.width / 2,
            location[1] + view.height / 2
        )
    }

    private fun getDstView(): ViewGroup {
        return dstPage.folderPagedView
            .getPageAt(dstPage.folderPagedView.currentPage)
            ?.shortcutsAndWidgets as ViewGroup
    }

    private fun getSpanCount(): Int {
        return dstPage.folderPagedView.organizer.countX
    }

    companion object {
        private val OPEN_FOLDER = PathInterpolator(
            0.16f, 0.2f, 0f, 1f
        )
        private val CLOSE_FOLDER = PathInterpolator(
            1f, 0f, 0.84f, 0.8f
        )
    }
}