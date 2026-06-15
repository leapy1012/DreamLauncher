package com.android.customize.common.extension

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.android.launcher3.CustomizeLauncher
import com.android.launcher3.Launcher
import android.util.DisplayMetrics
import android.view.WindowManager

fun Context.parseComponentName(resId: Int): ComponentName? {
    val cn = getString(resId).apply {
        if (isBlank()) return null
    }
    return ComponentName.unflattenFromString(cn)
}

fun Context.parseIntent(resId: Int): Intent? {
    val cn = parseComponentName(resId)
    return if (cn != null) {
        Intent().setComponent(cn)
    } else {
        Intent().setAction(getString(resId))
    }
}

fun Context.getCustomizeLauncher(): CustomizeLauncher {
    return Launcher.getLauncher(this) as CustomizeLauncher
}

/**
 * 获取屏幕高度（像素）
 */
fun Context.getScreenHeight(): Int {
    val displayMetrics = DisplayMetrics()
    val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    windowManager.defaultDisplay.getMetrics(displayMetrics)
    return displayMetrics.heightPixels
}