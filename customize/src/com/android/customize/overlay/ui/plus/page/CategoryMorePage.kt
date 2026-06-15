@file:OptIn(ExperimentalStdlibApi::class)

package com.android.customize.overlay.ui.plus.page

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.android.customize.common.extension.color
import com.android.customize.common.extension.px
import com.android.customize.common.extension.getScreenHeight
import com.android.customize.overlay.extension.overlayScopeObserve
import com.android.customize.overlay.ui.plus.PluscreenView
import com.android.customize.overlay.ui.plus.view.FolderPagedView
import com.android.launcher3.Launcher
import com.android.launcher3.R
import com.android.launcher3.model.data.FolderInfo
import com.android.launcher3.pageindicators.PageIndicatorDots

@SuppressLint("ClickableViewAccessibility", "NewApi")
class CategoryMorePage @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    val name by lazy {
        TextView(context).apply {
            textSize = 18f
            setTextColor(color(R.color.category_more_name))
            gravity = Gravity.CENTER
        }
    }

    val folderPagedView by lazy {
        FolderPagedView(context).apply {
            id = generateViewId()
            setFolder(this@CategoryMorePage)
        }
    }

    val pageIndicator by lazy {
        PageIndicatorDots(context)
    }

    private val gestureDetector = GestureDetector(context, object :
        GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            animateClose()
            return true
        }
    })

    private val onBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (!isVisible) return
            animateClose()
        }
    }

    init {
        addView(
            folderPagedView, LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_CONSTRAINT
            ).apply {
                topToTop = LayoutParams.PARENT_ID
                bottomToBottom = LayoutParams.PARENT_ID
                matchConstraintPercentHeight = px(R.dimen.folder_more_height).toFloat() / context.getScreenHeight().toFloat()
            })

        addView(
            name, LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                bottomToTop = folderPagedView.id
                bottomMargin = px(R.dimen.category_folder_margin)
            })

        addView(
            pageIndicator, LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                topToBottom = folderPagedView.id
                topMargin = px(R.dimen.category_folder_margin)
            })

        setOnClickListener { animateClose() }
        folderPagedView.setOnTouchListener { v, ev ->
            gestureDetector.onTouchEvent(ev)
        }

        overlayScopeObserve {
            onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        }
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)

        onBackPressedCallback.isEnabled = isVisible
    }

    fun bind(folderInfo: FolderInfo, onComplete: () -> Unit) {
        name.text = folderInfo.title
        val launcher = Launcher.getLauncher(context)
        val isTablet = launcher.deviceProfile.isTablet
        val numFolderColumns = if (isTablet) 6 else 3
        folderPagedView.bind(folderInfo, numFolderColumns)
        folderPagedView.postDelayed(onComplete, 300)
    }

    private fun animateClose() {
        val view = PluscreenView.get(this)
        view.categoryMoreAnimator.close(true)
    }
}