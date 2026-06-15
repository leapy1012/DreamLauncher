package com.android.customize.overlay.ui.plus.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.view.isVisible
import com.android.customize.overlay.ui.plus.page.CategoryMorePage
import com.android.launcher3.BubbleTextView
import com.android.launcher3.CellLayout
import com.android.launcher3.Launcher
import com.android.launcher3.PagedView
import com.android.launcher3.R
import com.android.launcher3.celllayout.CellLayoutLayoutParams
import com.android.launcher3.model.data.FolderInfo
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.model.data.WorkspaceItemInfo
import com.android.launcher3.pageindicators.PageIndicatorDots
import com.android.launcher3.touch.CustomizeItemClickHandler
import com.android.launcher3.touch.CustomizeItemLongClickListener

class FolderPagedView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : PagedView<PageIndicatorDots>(context, attrs) {

    var folderInfo: FolderInfo? = null

    private var isBound = false
    private val viewCache by lazy {
        Launcher.getLauncher(context).viewCache
    }

    val organizer by lazy {
        FolderGridOrganizer(3, 3)
    }

    fun setFolder(categoryMorePage: CategoryMorePage) {
        mPageIndicator = categoryMorePage.pageIndicator
    }

    fun bind(folderInfo: FolderInfo, numFolderColumns: Int) {
        mPageIndicator.setMarkersCount(childCount / panelCount)
        setCurrentPage(0, currentPage)
        mPageIndicator.setScroll(0, mMaxScroll)

        organizer.setNumFolderColumns(numFolderColumns)
        if (isBound) unbind()
        this.folderInfo = folderInfo
        arrangeChildren(folderInfo.contents.map { createNewView(it) })
        isBound = true
    }

    private fun unbind() {
        for (i in childCount - 1 downTo 0) {
            val page = getChildAt(i) as CellLayout
            val container = page.shortcutsAndWidgets
            for (j in container.childCount - 1 downTo 0) {
                container.getChildAt(j).isVisible = true
                viewCache.recycleView(
                    R.layout.category_folder_application,
                    container.getChildAt(j)
                )
            }
            page.removeAllViews()
            viewCache.recycleView(R.layout.category_folder_page, page)
        }
        removeAllViews()
        isBound = false
    }

    fun arrangeChildren(list: List<View>) {
        val itemCount = list.size
        val pages = ArrayList<CellLayout?>()
        for (i in 0 until childCount) {
            val page = getChildAt(i) as CellLayout
            page.removeAllViews()
            pages.add(page)
        }
        organizer.setFolderInfo(folderInfo)
        setupContentDimensions(itemCount)

        val pageItr = pages.iterator()
        var currentPage: CellLayout? = null

        var position = 0
        var rank = 0

        for (i in 0 until itemCount) {
            val v = if (list.size > i) list[i] else null
            if (currentPage == null || position >= organizer.maxItemsPerPage) {
                // Next page
                currentPage = if (pageItr.hasNext()) {
                    pageItr.next()
                } else {
                    createAndAddNewPage()
                }
                position = 0
            }

            if (v != null) {
                val lp = v.layoutParams as CellLayoutLayoutParams
                val info = v.tag as ItemInfo
                lp.setCellXY(organizer.getPosForRank(rank))
                currentPage!!.addViewToCellLayout(
                    v, -1, info.viewId, lp, true
                )

                if (organizer.isItemInPreview(rank) && v is BubbleTextView) {
                    v.verifyHighRes()
                }
            }

            rank++
            position++
        }

        var removed = false
        while (pageItr.hasNext()) {
            removeView(pageItr.next())
            removed = true
        }
        if (removed) {
            setCurrentPage(0)
        }

        setEnableOverscroll(pageCount > 1)

        mPageIndicator.visibility = if (pageCount > 1) VISIBLE else GONE
    }

    private fun setupContentDimensions(count: Int) {
        organizer.setContentSize(count)
        for (i in pageCount - 1 downTo 0) {
            getPageAt(i)?.setGridSize(organizer.countX, organizer.countY)
        }
    }

    fun createNewView(item: WorkspaceItemInfo): View {
        val textView: BubbleTextView = viewCache.getView(
            R.layout.category_folder_application, context, null
        )
        textView.applyFromWorkspaceItem(item)
        textView.setOnClickListener(CustomizeItemClickHandler.INSTANCE)
        textView.setOnLongClickListener(CustomizeItemLongClickListener.INSTANCE)
        val lp = textView.layoutParams as CellLayoutLayoutParams?
        if (lp == null) {
            textView.setLayoutParams(
                CellLayoutLayoutParams(
                    item.cellX, item.cellY,
                    item.spanX, item.spanY
                )
            )
        } else {
            lp.cellX = item.cellX
            lp.cellY = item.cellY
            lp.cellVSpan = 1
            lp.cellHSpan = 1
        }
        return textView
    }

    private fun createAndAddNewPage(): CellLayout {
        val launcher = Launcher.getLauncher(context)
        val grid = launcher.deviceProfile
        val page = viewCache.getView<CellLayout>(
            R.layout.category_folder_page, context, this
        )
        page.setCellDimensions(grid.folderCellWidthPx, grid.folderCellHeightPx)
        page.shortcutsAndWidgets.isMotionEventSplittingEnabled = false
        page.setInvertIfRtl(true)
        page.setGridSize(organizer.countX, organizer.countY)

        addView(page, -1, generateDefaultLayoutParams())
        return page
    }

    override fun getPageAt(index: Int): CellLayout? {
        return super.getPageAt(index) as? CellLayout
    }

    fun getCurrentCellLayout(): CellLayout? {
        return getPageAt(nextPage)
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        if (mMaxScroll > 0) mPageIndicator.setScroll(l, mMaxScroll)
    }
}