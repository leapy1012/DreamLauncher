package com.android.customize.common.extension

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withSave

private const val COMPRESS = 8f

fun Drawable.asBlurredForScreen(context: Context, radius: Float): Drawable? {
    val dm = context.resources.displayMetrics
    val width = dm.widthPixels
    val height = dm.heightPixels
    val sourceBitmap = toBitmap()
    val overlay = createBitmap(
        (width / COMPRESS).toInt(),
        (height / COMPRESS).toInt()
    )
    val canvas = Canvas(overlay)
    canvas.scale(1 / COMPRESS, 1 / COMPRESS)
    val paint = Paint()
    paint.flags = Paint.FILTER_BITMAP_FLAG
    val matrix = Matrix()
    val scaleX = width / sourceBitmap.width.toFloat()
    val scaleY = height / sourceBitmap.height.toFloat()
    matrix.setScale(scaleX, scaleY)
    canvas.drawBitmap(sourceBitmap, matrix, paint)
    var result = createBitmap(
        (width / COMPRESS).toInt(),
        (height / COMPRESS).toInt()
    )
    canvas.withSave {
        canvas.setBitmap(result)
        canvas.scale(1 / COMPRESS, 1 / COMPRESS)
        paint.colorFilter = PorterDuffColorFilter(
            "#26232236".toColorInt(),
            PorterDuff.Mode.SRC_OVER
        )
        canvas.drawBitmap(sourceBitmap, matrix, paint)
        result = result.asBlurred(context, radius)
    }
    return result.toDrawable(context.resources)
}