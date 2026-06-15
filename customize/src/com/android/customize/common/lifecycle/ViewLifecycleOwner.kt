package com.android.customize.common.lifecycle

import android.view.View
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner

interface ViewLifecycleOwner : SavedStateRegistryOwner, ViewModelStoreOwner,
    HasDefaultViewModelProviderFactory, OnBackPressedDispatcherOwner {
    fun onCreate()
    fun onDestroy()
}

fun View.findViewLifecycleOwner(): ViewLifecycleOwner? {
    if (tag is ViewLifecycleOwner) {
        return tag as ViewLifecycleOwner
    } else {
        val view = parent as? View
        return view?.findViewLifecycleOwner()
    }
}