package com.android.customize.overlay.ui.plus.page

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.customize.common.extension.color
import com.android.customize.common.extension.flowWithLifecycle
import com.android.customize.common.extension.px
import com.android.customize.common.lifecycle.viewModels
import com.android.customize.overlay.extension.overlayScopeObserve
import com.android.customize.overlay.ui.plus.PluscreenView
import com.android.customize.overlay.ui.plus.view.SearchBarContainer
import com.android.customize.overlay.ui.widget.LetterSideBar
import com.android.launcher3.Launcher
import com.android.launcher3.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@SuppressLint("ClickableViewAccessibility", "NewApi")
class SearchAppPage @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    private val store by viewModels<SearchAppStore>()
    private val searchAppAdapter = SearchAppAdapter()

    val searchBarContainer by lazy {
        SearchBarContainer(context).apply {
            id = generateViewId()
            searchBar.addTextChangedListener {
                store.search(it?.trim()?.toString())
            }
            cancel.setOnClickListener {
                setSearching(false)
                animateClose()
            }
        }
    }

    val rvApps by lazy {
        RecyclerView(context).apply {
            val launcher = Launcher.getLauncher(context)
            layoutManager = GridLayoutManager(context, 8).apply {
                spanSizeLookup = searchAppAdapter.createSpanSizeLookup(launcher)
            }
            adapter = searchAppAdapter
            itemAnimator = null

            setOnTouchListener { _, ev ->
                gestureDetector.onTouchEvent(ev)
            }
        }
    }

    val tvEmpty by lazy {
        TextView(context).apply {
            id = generateViewId()
            gravity = Gravity.CENTER
            text = context.getString(R.string.search_error_empty)
            setTextColor(color(R.color.text_color_primary_dark))
        }
    }

    val letterSideBar by lazy {
        LetterSideBar(context).apply {
            id = generateViewId()
            onLetterChange = { letter ->
                searchBarContainer.searchBar.hideKeyboard()
                val position = searchAppAdapter.currentList.indexOfFirst {
                    it is String && it == letter
                }
                if (position >= 0) {
                    val glm = rvApps.layoutManager as GridLayoutManager
                    glm.scrollToPositionWithOffset(position, 0)
                }
            }
        }
    }

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                searchBarContainer.searchBar.hideKeyboard()
                return true
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                searchBarContainer.searchBar.hideKeyboard()
                return super.onScroll(e1, e2, distanceX, distanceY)
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
            searchBarContainer, LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
        )

        rvApps.setPadding(px(R.dimen.search_app_padding))
        addView(
            rvApps, LayoutParams(
                LayoutParams.MATCH_CONSTRAINT,
                LayoutParams.MATCH_CONSTRAINT
            ).apply {
                startToStart = LayoutParams.PARENT_ID
                endToEnd = LayoutParams.PARENT_ID
                topToBottom = searchBarContainer.id
                bottomToBottom = LayoutParams.PARENT_ID
            })

        letterSideBar.setPadding(px(R.dimen.search_sidebar_padding))
        addView(
            letterSideBar, LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_CONSTRAINT
            ).apply {
                endToEnd = LayoutParams.PARENT_ID
                topToBottom = searchBarContainer.id
                bottomToBottom = LayoutParams.PARENT_ID
            })

        addView(tvEmpty, LayoutParams(
                LayoutParams.MATCH_CONSTRAINT,
                LayoutParams.MATCH_CONSTRAINT
            ).apply {
                startToStart = LayoutParams.PARENT_ID
                endToEnd = LayoutParams.PARENT_ID
                topToBottom = searchBarContainer.id
                bottomToBottom = LayoutParams.PARENT_ID
            })

        overlayScopeObserve {
            store.dataFlow
                .flowWithLifecycle(lifecycle)
                .onEach { data ->
                    tvEmpty.isVisible = data.isEmpty()
                    letterSideBar.isVisible = data.isNotEmpty()
                    searchAppAdapter.submitList(data)
                    letterSideBar.activeLetters = data
                        .filter { it is String }
                        .map { it as String }
                }.launchIn(lifecycleScope)

            onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        }
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)

        searchBarContainer.setSearching(isVisible)
        if (isVisible) {
            rvApps.scrollToPosition(0)
        }

        onBackPressedCallback.isEnabled = isVisible
    }

    private fun animateClose() {
        val view = PluscreenView.get(this)
        view.searchAppAnimator.close(true)
    }
}