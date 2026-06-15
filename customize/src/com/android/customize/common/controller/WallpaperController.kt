package com.android.customize.common.controller

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import com.android.customize.common.extension.asBlurredForScreen
import com.android.launcher3.util.Executors
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine

@SuppressLint("NewApi", "MissingPermission")
class WallpaperController(context: Context) {
    private val executor = Executors.THREAD_POOL_EXECUTOR
    private val wallpaperManager = WallpaperManager.getInstance(context)

    private val alphaFlow = MutableStateFlow(0f)

    val wallpaperFlow = callbackFlow {
        val onColorsChangedListener = WallpaperManager.OnColorsChangedListener { colors, which ->
            executor.execute {
                getBlurredDrawable(context)?.also {
                    trySend(it)
                }
            }
        }
        wallpaperManager.addOnColorsChangedListener(
            onColorsChangedListener, Handler(Looper.getMainLooper())
        )
        executor.execute {
            getBlurredDrawable(context)?.also {
                trySend(it)
            }
        }
        awaitClose {
            wallpaperManager.removeOnColorsChangedListener(onColorsChangedListener)
        }
    }

    val backgroundFlow = combine(alphaFlow, wallpaperFlow) { alpha, wallpaper ->
        wallpaper.alpha = (alpha * 255).toInt()
        wallpaper
    }

    fun updateAlpha(alpha: Float) {
        alphaFlow.value = alpha
    }

    private fun getBlurredDrawable(context: Context): Drawable? {
        val drawable = wallpaperManager.wallpaperInfo
            ?.loadThumbnail(context.packageManager)
            ?: wallpaperManager.fastDrawable
        return drawable?.asBlurredForScreen(
            context, MAX_BLUR_RADIUS
        )
    }

    companion object {
        private const val MAX_BLUR_RADIUS = 25f
    }
}