package com.android.customize.common.lifecycle;

import android.os.Build;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;
import androidx.savedstate.SavedStateRegistry;
import androidx.savedstate.SavedStateRegistryController;

import com.android.launcher3.Utilities;

public class ViewLifecycleOwnerImpl implements ViewLifecycleOwner {

    private final ViewModelStore mViewModelStore = new ViewModelStore();
    private final OnBackPressedDispatcher mOnBackPressedDispatcher = new OnBackPressedDispatcher();
    private final ViewModelProvider.Factory mFactory;
    private final LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(this);
    private final SavedStateRegistryController mSavedStateRegistryController =
            SavedStateRegistryController.create(this);
    private final View mView;
    private final ViewTreeObserver.OnWindowFocusChangeListener mWindowFocusListener =
            hasFocus -> updateState();
    private final Api34Impl mApi34Impl;

    public ViewLifecycleOwnerImpl(View view, ViewModelProvider.Factory factory) {
        mView = view;
        if (Utilities.ATLEAST_U) {
            mApi34Impl = new Api34Impl();
        } else {
            mApi34Impl = null;
        }
        mFactory = factory;

        mSavedStateRegistryController.performRestore(null);

        view.addOnAttachStateChangeListener(
                new View.OnAttachStateChangeListener() {

                    @Override
                    public void onViewAttachedToWindow(View v) {
                        onCreate();
                    }

                    @Override
                    public void onViewDetachedFromWindow(View v) {
                        onDestroy();
                    }
                });
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return mLifecycleRegistry;
    }

    @NonNull
    @Override
    public SavedStateRegistry getSavedStateRegistry() {
        return mSavedStateRegistryController.getSavedStateRegistry();
    }

    @Override
    public void onCreate() {
        mLifecycleRegistry.setCurrentState(Lifecycle.State.CREATED);
        if (Utilities.ATLEAST_U) {
            mApi34Impl.addOnWindowVisibilityChangeListener();
        }
        mView.getViewTreeObserver().addOnWindowFocusChangeListener(
                mWindowFocusListener);
        updateState();
    }

    @Override
    public void onDestroy() {
        if (Utilities.ATLEAST_U) {
            mApi34Impl.removeOnWindowVisibilityChangeListener();
        }
        mView.getViewTreeObserver().removeOnWindowFocusChangeListener(
                mWindowFocusListener);
        mLifecycleRegistry.setCurrentState(Lifecycle.State.DESTROYED);
    }

    private void updateState() {
        Lifecycle.State state =
                mView.getWindowVisibility() != View.VISIBLE ? Lifecycle.State.CREATED
                        : (!mView.hasWindowFocus() ? Lifecycle.State.STARTED
                        : Lifecycle.State.RESUMED);
        mLifecycleRegistry.setCurrentState(state);
    }

    @NonNull
    @Override
    public final ViewModelStore getViewModelStore() {
        return mViewModelStore;
    }

    @NonNull
    @Override
    public OnBackPressedDispatcher getOnBackPressedDispatcher() {
        return mOnBackPressedDispatcher;
    }

    @NonNull
    @Override
    public ViewModelProvider.Factory getDefaultViewModelProviderFactory() {
        return mFactory;
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private class Api34Impl {
        private final ViewTreeObserver.OnWindowVisibilityChangeListener
                mWindowVisibilityListener =
                visibility -> updateState();

        void addOnWindowVisibilityChangeListener() {
            mView.getViewTreeObserver().addOnWindowVisibilityChangeListener(
                    mWindowVisibilityListener);
        }

        void removeOnWindowVisibilityChangeListener() {
            mView.getViewTreeObserver().removeOnWindowVisibilityChangeListener(
                    mWindowVisibilityListener);
        }
    }
}
