package com.android.launcher3.touch

import android.view.View
import androidx.core.view.isInvisible
import com.android.customize.overlay.ui.plus.PluscreenView
import com.android.launcher3.DropTarget
import com.android.launcher3.Launcher
import com.android.launcher3.dragndrop.DragController
import com.android.launcher3.dragndrop.DragOptions

object CustomizeItemLongClickListener {
    @JvmField
    val INSTANCE = View.OnLongClickListener {
        return@OnLongClickListener false
        it.cancelLongPress()

        val launcher = Launcher.getLauncher(it.context)
        if (!ItemLongClickListener.canStartDrag(launcher)) return@OnLongClickListener false
        if (launcher.workspace.isSwitchingState) return@OnLongClickListener false
        if (!launcher.isDraggingEnabled) return@OnLongClickListener true

        val dragSource = PluscreenView.get(it)
        val dragController = launcher.dragController
        dragController.addDragListener(object : DragController.DragListener {
            override fun onDragStart(dragObject: DropTarget.DragObject?, options: DragOptions?) {
                it.isInvisible = true
                dragSource.close(false)
            }

            override fun onDragEnd() {
                it.isInvisible = false
                dragController.removeDragListener(this)
            }
        })
        launcher.workspace.beginDragShared(it, dragSource, DragOptions())
        true
    }
}