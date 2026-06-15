package com.android.customize.overlay.controller

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.android.customize.common.logger.MyLogger
import com.android.customize.overlay.model.PhotoInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class PhotoController(context: Context) {
    private val resolver = context.contentResolver

    private val photoUrisFlow = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)

                trySend(getImageUris())
            }
        }

        resolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true, observer
        )

        trySend(getImageUris())

        awaitClose {
            resolver.unregisterContentObserver(observer)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val randomPhotoFlow = photoUrisFlow.flatMapLatest { imageUris ->
        if (imageUris.isEmpty()) {
            flow { emit(null) }
        } else {
            flow {
                while (true) {
                    val randomIndex = Random.nextInt(imageUris.size)
                    emit(PhotoInfo(imageUris[randomIndex]))
                    delay(TimeUnit.MINUTES.toMillis(5))
                }
            }
        }
    }

    private fun getImageUris(): MutableList<Uri> {
        myLogger.i("getImageUris")
        val imageIDs = mutableListOf<Uri>()
        try {
            val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            resolver.query(
                uri,
                arrayOf(MediaStore.Images.Media._ID),
                null, null, null
            )?.use {
                myLogger.i("getImageUris: ${it.count}")
                while (it.moveToNext()) {
                    val id = it.getLong(0)
                    imageIDs.add(Uri.withAppendedPath(uri, "$id"))
                }
            }
        } catch (e: Exception) {
            myLogger.e("getImageUris: ${e.message}", e)
        }
        return imageIDs
    }

    companion object {
        private val myLogger = MyLogger("PhotoController")
    }
}