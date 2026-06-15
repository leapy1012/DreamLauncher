package com.android.customize.overlay.model

import android.content.Context
import android.content.Intent
import com.android.customize.common.extension.parseIntent
import com.android.launcher3.LauncherSettings
import com.android.launcher3.model.data.ItemInfo

abstract class WidgetInfo : ItemInfo() {

    init {
        itemType = LauncherSettings.Favorites.ITEM_TYPE_FOLDER
    }

    abstract val cnOrAction: Int

    private var intent: Intent? = null

    fun bind(context: Context) {
        intent = context.parseIntent(cnOrAction)
    }

    override fun getIntent(): Intent? {
        return intent
    }
}