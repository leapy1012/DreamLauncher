package com.android.customize.overlay.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PaintFlagsDrawFilter
import android.graphics.Rect
import androidx.core.graphics.createBitmap

object BitmapUtil {
    fun createFolderPreviewBitmap(bitmaps: Array<Bitmap>, intrinsicSize: Int = 144): Bitmap {
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        val srcRect = Rect()
        val dstRect = Rect()

        val safeBitmaps = bitmaps.map {
            if (it.config == Bitmap.Config.HARDWARE) {
                it.copy(Bitmap.Config.ARGB_8888, true)
            } else {
                it
            }
        }

        val previewBitmap = createBitmap(intrinsicSize, intrinsicSize)
        val canvas = Canvas(previewBitmap)

        val size = intrinsicSize.toFloat()
        val iconSpace = size * 0.05f
        val iconSize = (size - iconSpace) / 2f

        canvas.drawFilter = PaintFlagsDrawFilter(
            0,
            Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG,
        )

        for (index in safeBitmaps.indices) {
            if (index >= 4) {
                break
            }

            val bitmap = safeBitmaps[index]
            val i = index % 2
            val j = index / 2

            val x = i * iconSize + i * iconSpace
            val y = j * iconSize + j * iconSpace

            srcRect.set(0, 0, bitmap.width, bitmap.height)
            dstRect.set(
                x.toInt(), y.toInt(),
                (x + iconSize).toInt(), (y + iconSize).toInt()
            )

            canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
        }

        return previewBitmap
    }
}