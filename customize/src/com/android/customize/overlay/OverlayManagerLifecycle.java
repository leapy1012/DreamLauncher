package com.android.customize.overlay;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import com.android.systemui.plugins.shared.LauncherOverlayManager;

public class OverlayManagerLifecycle implements LauncherOverlayManager, LifecycleOwner {

    private final LifecycleRegistry mRegistry = new LifecycleRegistry(this);

    public OverlayManagerLifecycle() {
        mRegistry.setCurrentState(Lifecycle.State.INITIALIZED);
    }

    @Override
    public void onAttachedToWindow() {
        mRegistry.setCurrentState(Lifecycle.State.CREATED);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        mRegistry.setCurrentState(Lifecycle.State.STARTED);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        mRegistry.setCurrentState(Lifecycle.State.RESUMED);
    }

    @Override
    public void onDetachedFromWindow() {
        mRegistry.setCurrentState(Lifecycle.State.DESTROYED);
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return mRegistry;
    }
}