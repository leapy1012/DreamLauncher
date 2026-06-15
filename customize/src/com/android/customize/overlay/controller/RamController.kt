package com.android.customize.overlay.controller

import android.app.ActivityManager
import android.content.Context
import com.android.customize.overlay.model.RamInfo
import kotlinx.coroutines.flow.MutableStateFlow
import com.android.customize.overlay.util.RamUtil

class RamController(val context: Context) {

    private val am = context.getSystemService(ActivityManager::class.java)
    val ramFlow = MutableStateFlow(getRamInfo())

    fun refresh() {
        ramFlow.value = getRamInfo()
    }

    private fun getRamInfo(): RamInfo {
        return RamInfo(RamUtil.getAvailMemoryLong(context), RamUtil.getTotalMemoryLong(context), RamUtil.getAvailMemoryDesc(context), RamUtil.getTotalMemoryDesc(context))
    }
}