/*
 * Copyright (C) 2026 GreenDream
 */
package com.dream.ext.core;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.dream.ext.registry.BaseExtRegistry;
import com.dream.ext.registry.ExtRegistry;

/**
 * Entry point of the Dream ext mechanism. AOSP classes obtain their ext once, e.g.
 * <pre>
 * mExt = ExtLoader.type(IWorkspaceExt.class).base(this).create();
 * </pre>
 * and delegate Dream-specific behavior to it, so the AOSP file only carries the call site.
 */
public final class ExtLoader {
    static final String TAG = "ExtLoader";

    private static volatile boolean sInitialized;

    private ExtLoader() { }

    /** Registers every ext implementation. Must run before any host class is created. */
    public static void init(Context context) {
        if (context == null || sInitialized) {
            return;
        }
        synchronized (ExtLoader.class) {
            if (sInitialized) {
                return;
            }
            BaseExtRegistry.init(context);
            ExtRegistry.init(context, context.getClassLoader());
            sInitialized = true;
        }
    }

    public static <T> ExtBuilder<T> type(Class<T> extClass) {
        return new ExtBuilder<>(extClass);
    }

    static boolean isDebug() {
        return Log.isLoggable(TAG, Log.DEBUG);
    }

    public static final class ExtBuilder<T> {
        private final Class<T> mExtClass;
        private Object mBase;
        private boolean mHasDummyExt = true;

        private ExtBuilder(Class<T> extClass) {
            mExtClass = extClass;
        }

        /** Host object the ext is attached to; also selects host-class specific bindings. */
        public ExtBuilder<T> base(Object base) {
            mBase = base;
            return this;
        }

        /** Fall back to a no-op proxy when nothing is registered (default). */
        public ExtBuilder<T> enableDummyExt() {
            mHasDummyExt = true;
            return this;
        }

        /** Return null when nothing is registered, so call sites must null-check. */
        public ExtBuilder<T> disableDummyExt() {
            mHasDummyExt = false;
            return this;
        }

        @Nullable
        public T create() {
            boolean debug = isDebug();
            IExtCreator<T> creator = ExtRegistry.getExt(mExtClass,
                    mBase != null ? mBase.getClass() : null);
            T ext = creator != null ? creator.createExtWith(mBase) : null;
            if (ext != null) {
                if (debug) {
                    Log.d(TAG, "ext " + mExtClass.getSimpleName() + " -> "
                            + ext.getClass().getName());
                }
                return ext;
            }
            if (mHasDummyExt) {
                return ExtDummy.createDummyExt(mExtClass, debug);
            }
            if (debug) {
                Log.d(TAG, "no ext for " + mExtClass.getName());
            }
            return null;
        }
    }
}
