package com.android.customize.overlay.ui.plus.view

import android.content.Context
import android.util.AttributeSet
import com.android.launcher3.BubbleTextView
import com.android.launcher3.dragndrop.DragOptions

class CategoryBubbleTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : BubbleTextView(context, attrs) {

    override fun startLongPressAction(): DragOptions.PreDragCondition? {
        return null
    }
}