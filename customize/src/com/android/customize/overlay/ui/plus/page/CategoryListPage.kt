package com.android.customize.overlay.ui.plus.page

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.customize.common.extension.flowWithLifecycle
import com.android.customize.common.extension.px
import com.android.customize.common.lifecycle.viewModels
import com.android.customize.overlay.extension.overlayScopeObserve
import com.android.customize.overlay.ui.plus.PluscreenView
import com.android.customize.overlay.ui.plus.view.SearchBarContainer
import com.android.launcher3.Launcher
import com.android.launcher3.R
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@SuppressLint("NewApi")
class CategoryListPage @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    private val store by viewModels<CategoryListStore>()

    private val categoryListAdapter = CategoryListAdapter()

    val searchBarContainer by lazy {
        SearchBarContainer(context).apply {
            id = R.id.search_bar_container
            searchBar.isFocusable = false
            cancel.isVisible = false
        }
    }

    val rvCategory by lazy {
        RecyclerView(context).apply {
            val launcher = Launcher.getLauncher(context)
            layoutManager = GridLayoutManager(context, 4).apply {
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return if (launcher.deviceProfile.isTablet) 1 else 2
                    }
                }
            }
            adapter = categoryListAdapter
            itemAnimator = null
        }
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (!isVisible) return
            val view = PluscreenView.get(this@CategoryListPage)
            view.close(true)
        }
    }

    init {
        searchBarContainer.isInvisible = true
        addView(
            searchBarContainer.apply {
                searchBar.setOnClickListener {
                    showSearchPage()
                }
            }, LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
        )

        rvCategory.setPadding(px(R.dimen.category_margin) / 2)
        addView(
            rvCategory, LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_CONSTRAINT
            ).apply {
                topToBottom = searchBarContainer.id
                bottomToBottom = LayoutParams.PARENT_ID
            })

        overlayScopeObserve {
            store.folderInfosFlow
                .flowWithLifecycle(lifecycle)
                .distinctUntilChanged()
                .onEach {
                    categoryListAdapter.submitList(it)
                }.launchIn(lifecycleScope)

            onBackPressedDispatcher.addCallback(onBackPressedCallback)
        }
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)

        onBackPressedCallback.isEnabled = isVisible
    }

    private fun showSearchPage() {
        val view = PluscreenView.get(this)
        view.searchAppAnimator.open(true)
    }
}