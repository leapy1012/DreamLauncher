package com.android.customize.common.di

import android.content.Context
import com.android.customize.common.controller.WallpaperController
import com.android.launcher3.util.MainThreadInitializedObject

interface ProcessInitializer {
    fun onInitialized()
}

class ProcessContainer private constructor(context: Context) : ProcessInitializer {

    val wallpaperController by lazy {
        WallpaperController(context)
    }

    override fun onInitialized() {}

    companion object {
        @JvmField
        val INSTANCE = MainThreadInitializedObject { ProcessContainer(it) }

        @JvmStatic
        fun get(context: Context): ProcessContainer = INSTANCE.get(context)
    }
}