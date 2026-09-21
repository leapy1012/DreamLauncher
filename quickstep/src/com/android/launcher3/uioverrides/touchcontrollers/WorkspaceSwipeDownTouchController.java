/*
 * Copyright (C) 2026 The Android Open Source Project
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
package com.android.launcher3.uioverrides.touchcontrollers;

import static android.view.MotionEvent.ACTION_CANCEL;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;
import static android.view.WindowManager.LayoutParams.FLAG_SLIPPERY;

import static com.android.launcher3.logging.StatsLogManager.LauncherEvent.LAUNCHER_SWIPE_DOWN_WORKSPACE_NOTISHADE_OPEN;

import android.graphics.PointF;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.settings.HomeScreenGestures;
import com.android.launcher3.util.TouchController;
import com.android.quickstep.SystemUiProxy;

import java.io.PrintWriter;

/**
 * Workspace swipe-down gesture. Routes to Global Search ({@code gd.app.quicksearch}) or the
 * notification shade based on {@link HomeScreenGestures}.
 */
public class WorkspaceSwipeDownTouchController implements TouchController {

    private final Launcher mLauncher;
    private final SystemUiProxy mSystemUiProxy;
    private final float mTouchSlop;
    private final SparseArray<PointF> mDownEvents = new SparseArray<>();

    private boolean mCanIntercept;
    private boolean mThresholdPassed;
    private boolean mForwardToStatusBar;
    private int mLastAction;

    public WorkspaceSwipeDownTouchController(Launcher launcher) {
        mLauncher = launcher;
        mSystemUiProxy = SystemUiProxy.INSTANCE.get(launcher);
        mTouchSlop = 2 * ViewConfiguration.get(launcher).getScaledTouchSlop();
    }

    @Override
    public void dump(String prefix, PrintWriter writer) {
        writer.println(prefix + "mCanIntercept:" + mCanIntercept);
        writer.println(prefix + "mThresholdPassed:" + mThresholdPassed);
        writer.println(prefix + "mForwardToStatusBar:" + mForwardToStatusBar);
        writer.println(prefix + "mLastAction:" + MotionEvent.actionToString(mLastAction));
        writer.println(prefix + "swipeDown:"
                + HomeScreenGestures.getSwipeDownAction(mLauncher));
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        int idx = ev.getActionIndex();
        int pid = ev.getPointerId(idx);
        if (action == ACTION_DOWN) {
            mCanIntercept = canInterceptTouch(ev);
            mThresholdPassed = false;
            mForwardToStatusBar = false;
            if (!mCanIntercept) {
                return false;
            }
            mDownEvents.clear();
            mDownEvents.put(pid, new PointF(ev.getX(), ev.getY()));
        } else if (action == MotionEvent.ACTION_POINTER_DOWN) {
            mDownEvents.put(pid, new PointF(ev.getX(idx), ev.getY(idx)));
        }
        if (!mCanIntercept) {
            return false;
        }
        if (action == ACTION_MOVE) {
            PointF down = mDownEvents.get(pid);
            if (down == null) {
                return false;
            }
            float dy = ev.getY(idx) - down.y;
            float dx = ev.getX(idx) - down.x;
            if (dy > mTouchSlop && dy > Math.abs(dx) && ev.getPointerCount() == 1) {
                mThresholdPassed = true;
                if (HomeScreenGestures.isSwipeDownNotification(mLauncher)
                        && mSystemUiProxy.isActive()) {
                    mForwardToStatusBar = true;
                    MotionEvent downEv = MotionEvent.obtain(ev);
                    downEv.setAction(ACTION_DOWN);
                    dispatchToStatusBar(downEv);
                    downEv.recycle();
                    setWindowSlippery(true);
                }
                return true;
            }
            if (Math.abs(dx) > mTouchSlop) {
                mCanIntercept = false;
            }
        }
        return false;
    }

    @Override
    public boolean onControllerTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        if (mForwardToStatusBar) {
            if (action == ACTION_UP || action == ACTION_CANCEL) {
                dispatchToStatusBar(ev);
                mLauncher.getStatsLogManager().logger()
                        .log(LAUNCHER_SWIPE_DOWN_WORKSPACE_NOTISHADE_OPEN);
                setWindowSlippery(false);
            }
            return true;
        }
        if (action == ACTION_UP && mThresholdPassed
                && HomeScreenGestures.isSwipeDownGlobalSearch(mLauncher)) {
            HomeScreenGestures.launchGlobalSearch(mLauncher);
        }
        return true;
    }

    private void dispatchToStatusBar(MotionEvent ev) {
        if (mSystemUiProxy.isActive()) {
            mLastAction = ev.getActionMasked();
            mSystemUiProxy.onStatusBarMotionEvent(ev);
        }
    }

    private void setWindowSlippery(boolean enable) {
        Window w = mLauncher.getWindow();
        WindowManager.LayoutParams wlp = w.getAttributes();
        if (enable) {
            wlp.flags |= FLAG_SLIPPERY;
        } else {
            wlp.flags &= ~FLAG_SLIPPERY;
        }
        w.setAttributes(wlp);
    }

    private boolean canInterceptTouch(MotionEvent ev) {
        if (!mLauncher.isInState(LauncherState.NORMAL)
                || AbstractFloatingView.getTopOpenViewWithType(mLauncher,
                        AbstractFloatingView.TYPE_STATUS_BAR_SWIPE_DOWN_DISALLOW) != null) {
            return false;
        }
        DeviceProfile dp = mLauncher.getDeviceProfile();
        if (ev.getY() > (mLauncher.getDragLayer().getHeight() - dp.getInsets().bottom)) {
            return false;
        }
        // Global Search works without SystemUiProxy; notification mode needs it.
        return HomeScreenGestures.isSwipeDownGlobalSearch(mLauncher)
                || mSystemUiProxy.isActive();
    }
}
