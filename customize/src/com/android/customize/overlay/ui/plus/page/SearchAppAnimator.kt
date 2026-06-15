package com.android.customize.overlay.ui.plus.page

import android.view.View
import androidx.core.view.isInvisible
import com.android.customize.overlay.ui.BasePageAnimator

class SearchAppAnimator(
    private val srcPage: CategoryListPage,
    private val dstPage: SearchAppPage
) : BasePageAnimator {

    override fun open(animate: Boolean, anchor: View?) {
        srcPage.isInvisible = true
        dstPage.isInvisible = false
    }

    override fun close(animate: Boolean) {
        srcPage.isInvisible = false
        dstPage.isInvisible = true
    }
}