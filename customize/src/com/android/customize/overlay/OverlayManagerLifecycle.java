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

    /**
     * The only teardown: a config-change relaunch keeps the window attached, so the old
     * instance never sees {@link #onDetachedFromWindow}.
     */
    @Override
    public void onActivityDestroyed(Activity activity) {
        // INITIALIZED -> DESTROYED is illegal; nothing was started before CREATED anyway.
        if (mRegistry.getCurrentState().isAtLeast(Lifecycle.State.CREATED)) {
            mRegistry.setCurrentState(Lifecycle.State.DESTROYED);
        }
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return mRegistry;
    }
}
