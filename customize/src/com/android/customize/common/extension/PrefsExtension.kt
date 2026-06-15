package com.android.customize.common.extension

import android.content.SharedPreferences
import com.android.launcher3.ContextualItem
import com.android.launcher3.LauncherPrefs
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

fun <T> LauncherPrefs.getAsFlow(item: ContextualItem<T>) = callbackFlow {
    val listener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
        if (key == item.sharedPrefKey) trySend(get(item))
    }
    addListener(listener, item)
    trySend(get(item))
    awaitClose {
        removeListener(listener)
    }
}