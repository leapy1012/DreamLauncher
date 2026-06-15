package com.android.customize.overlay.model

import android.net.Uri
import com.android.launcher3.R

data class PhotoInfo(
    val uri: Uri
) : WidgetInfo() {
    override val cnOrAction: Int
        get() = R.string.config_galleryCnOrAction
}