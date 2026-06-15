package com.android.customize.overlay.ui.plus

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.android.customize.common.extension.getCustomizeLauncher
import com.android.customize.common.lifecycle.findViewLifecycleOwner
import com.android.customize.overlay.extension.overlayScopeObserve
import com.android.customize.overlay.lifecycle.OverlayViewLifecycleInitializer
import com.android.customize.overlay.ui.plus.page.CategoryListPage
import com.android.customize.overlay.ui.plus.page.CategoryMoreAnimator
import com.android.customize.overlay.ui.plus.page.CategoryMorePage
import com.android.customize.overlay.ui.plus.page.SearchAppAnimator
import com.android.customize.overlay.ui.plus.page.SearchAppPage
import com.android.launcher3.DragSource
import com.android.launcher3.DropTarget
import com.android.launcher3.touch.SingleAxisSwipeDetector
import com.android.launcher3.views.AbsSlideTouchView
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class PluscreenView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AbsSlideTouchView(context, attrs), DragSource {

    val categoryListPage by lazy {
        CategoryListPage(context)
    }

    val categoryMorePage by lazy {
        CategoryMorePage(context)
    }

    val searchAppPage by lazy {
        SearchAppPage(context)
    }

    val categoryMoreAnimator by lazy {
        CategoryMoreAnimator(
            categoryListPage,
            categoryMorePage
        )
    }

    val searchAppAnimator by lazy {
        SearchAppAnimator(
            categoryListPage,
            searchAppPage
        )
    }

    init {
        OverlayViewLifecycleInitializer.init(this)

        rootView.addView(
            categoryListPage, LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
        )

        categoryMorePage.isInvisible = true
        rootView.addView(
            categoryMorePage, LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
        )

        searchAppPage.isInvisible = true
        rootView.addView(
            searchAppPage, LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
        )

        overlayScopeObserve {
            val launcher = context.getCustomizeLauncher()
            launcher.container.deviceProfileController
                .deviceProfileFlow
                .flowWithLifecycle(lifecycle)
                .onEach {
                    searchAppAnimator.close(false)
                    categoryMoreAnimator.close(false)
                }.launchIn(lifecycleScope)
        }
    }

    override val direction: SingleAxisSwipeDetector.Direction
        get() = SingleAxisSwipeDetector.HORIZONTAL


    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (isVisible) {
            searchAppAnimator.close(false)
            categoryMoreAnimator.close(false)
        }
    }

    override fun onControllerInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!categoryListPage.isVisible) {
            return false
        }
        return super.onControllerInterceptTouchEvent(ev)
    }

    override fun onBackInvoked() {
        val viewLifecycleOwner = findViewLifecycleOwner()
        viewLifecycleOwner?.onBackPressedDispatcher?.onBackPressed()
    }

    override fun isOfType(type: Int): Boolean {
        return (type and TYPE_ON_BOARD_POPUP) != 0
    }

    override fun onDropCompleted(
        target: View?,
        d: DropTarget.DragObject?,
        success: Boolean
    ) {

    }

    companion object {
        fun get(view: View): PluscreenView {
            return view as? PluscreenView ?: get(view.parent as View)
        }
    }
}