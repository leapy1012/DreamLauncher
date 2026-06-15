package com.android.customize.overlay.ui.minus.page

import android.content.Context
import android.util.AttributeSet
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.customize.common.extension.flowWithLifecycle
import com.android.customize.common.extension.px
import com.android.customize.common.lifecycle.viewModels
import com.android.customize.overlay.extension.overlayScopeObserve
import com.android.customize.overlay.ui.minus.MinuscreenView
import com.android.launcher3.Launcher
import com.android.launcher3.R
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class WidgetListPage(
    context: Context,
    attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {
    private val store by viewModels<WidgetListStore>()

    private val widgetListAdapter = WidgetListAdapter()
    val rvWidgets by lazy {
        RecyclerView(context).apply {
            val launcher = Launcher.getLauncher(context)
            layoutManager = GridLayoutManager(context, 4).apply {
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return if (position == 0)
                            spanCount else if (launcher.deviceProfile.isTwoPanels)
                            spanCount / 4 else spanCount / 2
                    }
                }
            }
            adapter = widgetListAdapter
            itemAnimator = null
        }
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (!isVisible) return
            val view = MinuscreenView.get(this@WidgetListPage)
            view.close(true)
        }
    }

    init {
        rvWidgets.setPadding(px(R.dimen.widget_margin) / 2)
        addView(
            rvWidgets, LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
        )

        overlayScopeObserve {
            store.widgetsFlow
                .flowWithLifecycle(lifecycle)
                .distinctUntilChanged()
                .onEach {
                    widgetListAdapter.submitList(it)
                }.launchIn(lifecycleScope)

            onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    store.refresh()
                }
            }
        }
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)

        onBackPressedCallback.isEnabled = isVisible

        if (isVisible) {
            store.refresh()
        }
    }
}