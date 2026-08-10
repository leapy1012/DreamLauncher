/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.wm.shell.common;

import android.os.Build;
import android.os.Debug;
import android.util.ArrayMap;
import android.util.Pools;
import android.util.Slog;
import android.view.SurfaceControl;

/**
 * Provides a synchronized pool of {@link SurfaceControl.Transaction}s to minimize allocations.
 */
public class TransactionPool {
    private final Pools.SynchronizedPool<SurfaceControl.Transaction> mTransactionPool =
            new Pools.SynchronizedPool<>(4);

    private static boolean mDebugReleaseStack = Build.isDebuggable();
    private ArrayMap<Long, String> mReleaseStackMap = new ArrayMap();

    public TransactionPool() {
    }

    /** Gets a transaction from the pool. */
    public SurfaceControl.Transaction acquire() {
        SurfaceControl.Transaction t = mTransactionPool.acquire();
        SurfaceControl.Transaction st = t != null ? t : new SurfaceControl.Transaction();
        if (mDebugReleaseStack && st.mNativeObject != 0
                               && mReleaseStackMap.indexOfKey(st.mNativeObject) < 0) {
            mReleaseStackMap.put(st.mNativeObject, null);
        }
        return st;
    }

    public void release(SurfaceControl.Transaction t) {
        release(t, false);
    }

    /**
     * Return a transaction to the pool. DO NOT call {@link SurfaceControl.Transaction#close()} if
     * returning to pool.
     */
    public void release(SurfaceControl.Transaction t, boolean isTracking) {
        if (isTracking && mDebugReleaseStack && t.mNativeObject != 0) {
            String releaseStack = mReleaseStackMap.get(t.mNativeObject);
            if (releaseStack != null) {
                Slog.w("TransactionPool", " transaction " + t
                                          + " already released from pool,"+releaseStack);
            }
        }
        if (!mTransactionPool.release(t)) {
            t.close();
        } else if (mDebugReleaseStack && t.mNativeObject != 0) {
            if (mReleaseStackMap.indexOfKey(t.mNativeObject) >= 0) {
                mReleaseStackMap.put(t.mNativeObject, Debug.getCallers(5));
            }
        }
    }
}
