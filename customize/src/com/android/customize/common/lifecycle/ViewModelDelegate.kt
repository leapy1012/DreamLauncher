package com.android.customize.common.lifecycle

import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class LazyViewModelDelegate<T : ViewModel>(
    private val view: View,
    private val viewModelClass: KClass<T>
) {
    private val viewModel by lazy {
        val owner = view.findViewLifecycleOwner()
            ?: throw RuntimeException("LifecycleAwareDelegateOwner not found")
        val provider = ViewModelProvider(owner, owner.defaultViewModelProviderFactory)
        provider[viewModelClass.java]
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return viewModel
    }
}

inline fun <reified T : ViewModel> View.viewModels(): LazyViewModelDelegate<T> {
    return LazyViewModelDelegate(this, T::class)
}