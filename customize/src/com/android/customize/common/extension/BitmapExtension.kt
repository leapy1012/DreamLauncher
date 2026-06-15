package com.android.customize.common.extension

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.core.graphics.drawable.toDrawable

fun Bitmap.asBlurredDrawable(context: Context, radius: Float): Drawable {
    return asBlurred(context, radius).toDrawable(context.resources)
}

fun Bitmap.asBlurred(context: Context, radius: Float): Bitmap {
    val result = Bitmap.createBitmap(this)
    val renderScript = RenderScript.create(context)
    val input = Allocation.createFromBitmap(renderScript, result)
    val output = Allocation.createTyped(renderScript, input.type)
    val scriptIntrinsicBlur = ScriptIntrinsicBlur.create(
        renderScript, Element.U8_4(renderScript)
    )
    scriptIntrinsicBlur.setInput(input)
    scriptIntrinsicBlur.setRadius(radius)
    scriptIntrinsicBlur.forEach(output)
    output.copyTo(result)
    renderScript.destroy()
    return result
}