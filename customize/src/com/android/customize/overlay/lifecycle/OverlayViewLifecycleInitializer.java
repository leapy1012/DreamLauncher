package com.android.customize.overlay.lifecycle;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.android.customize.common.extension.ContextExtensionKt;
import com.android.customize.common.lifecycle.ViewLifecycleOwner;
import com.android.customize.overlay.di.OverlayContainer;
import com.android.launcher3.CustomizeLauncher;

public final class OverlayViewLifecycleInitializer {
    public static void init(View target) {
        ViewModelProvider.Factory factory = new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                CustomizeLauncher launcher = ContextExtensionKt.getCustomizeLauncher(target.getContext());
                try {
                    return modelClass.getConstructor(OverlayContainer.class)
                                .newInstance(launcher.getOverlayManager().getOverlayContainer());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        target.addOnAttachStateChangeListener(
                new View.OnAttachStateChangeListener() {

                    @Override
                    public void onViewAttachedToWindow(View v) {
                        OverlayViewLifecycleInitializer.onAttachedToWindow(v, factory);
                    }

                    @Override
                    public void onViewDetachedFromWindow(View v) {
                        OverlayViewLifecycleInitializer.onDetachedFromWindow(v);
                    }
                });
    }

    private static void onAttachedToWindow(View v, ViewModelProvider.Factory factory) {
        ViewLifecycleOwner owner = new OverlayViewLifecycleOwnerImpl(v, factory);
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

