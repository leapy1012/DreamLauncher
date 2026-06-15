package com.android.customize.common.lifecycle;

import android.view.View;

import androidx.lifecycle.ViewModelProvider;

public final class ViewLifecycleInitializer {
    public static void init(View target, ViewModelProvider.Factory factory) {
        target.addOnAttachStateChangeListener(
                new View.OnAttachStateChangeListener() {

                    @Override
                    public void onViewAttachedToWindow(View v) {
                        ViewLifecycleInitializer.onAttachedToWindow(v, factory);
                    }

                    @Override
                    public void onViewDetachedFromWindow(View v) {
                        ViewLifecycleInitializer.onDetachedFromWindow(v);
                    }
                });
    }

    private static void onAttachedToWindow(View v, ViewModelProvider.Factory factory) {
        ViewLifecycleOwner owner = new ViewLifecycleOwnerImpl(v, factory);
        owner.onCreate();
        v.setTag(owner);
    }

    private static void onDetachedFromWindow(View root) {
        if (root.getTag() instanceof ViewLifecycleOwner owner) {
            owner.onDestroy();
            root.setTag(null);
        }
    }
}

