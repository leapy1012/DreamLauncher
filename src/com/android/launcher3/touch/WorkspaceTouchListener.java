/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.launcher3.touch;

import static android.view.MotionEvent.ACTION_CANCEL;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_POINTER_UP;
import static android.view.MotionEvent.ACTION_UP;

import static com.android.launcher3.LauncherState.ALL_APPS;
import static com.android.launcher3.LauncherState.NORMAL;
import static com.android.launcher3.logging.StatsLogManager.LauncherEvent.LAUNCHER_ALLAPPS_CLOSE_TAP_OUTSIDE;
import static com.android.launcher3.logging.StatsLogManager.LauncherEvent.LAUNCHER_WORKSPACE_LONGPRESS;

import android.graphics.PointF;
import android.graphics.Rect;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.Workspace;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.logger.LauncherAtom;
import com.android.launcher3.testing.TestLogging;
import com.android.launcher3.testing.shared.TestProtocol;
import com.android.launcher3.util.TouchUtil;
import android.os.Vibrator;
import android.content.Context;

import static com.android.launcher3.LauncherState.SPRING_LOADED;
import android.widget.Toast;
import com.android.launcher3.R;
import com.android.launcher3.LauncherPrefs;

import android.content.Intent;
import android.provider.Settings;

/**
 * Helper class to handle touch on empty space in workspace and show options popup on long press
 */
public class WorkspaceTouchListener extends GestureDetector.SimpleOnGestureListener
        implements OnTouchListener {

    /**
     * STATE_PENDING_PARENT_INFORM is the state between longPress performed & the next motionEvent.
     * This next event is used to send an ACTION_CANCEL to Workspace, to that it clears any
     * temporary scroll state. After that, the state is set to COMPLETED, and we just eat up all
     * subsequent motion events.
     */
    private static final int STATE_CANCELLED = 0;
    private static final int STATE_REQUESTED = 1;
    private static final int STATE_PENDING_PARENT_INFORM = 2;
    private static final int STATE_COMPLETED = 3;

    private final Rect mTempRect = new Rect();
    private final Launcher mLauncher;
    private final Workspace<?> mWorkspace;
    private final PointF mTouchDownPoint = new PointF();
    private final float mTouchSlop;
    private float mTouchCoordX, mTouchCoordY;

    private int mLongPressState = STATE_CANCELLED;

    private final GestureDetector mGestureDetector;

    public WorkspaceTouchListener(Launcher launcher, Workspace<?> workspace) {
        mLauncher = launcher;
        mWorkspace = workspace;
        // Use twice the touch slop as we are looking for long press which is more
        // likely to cause movement.
        mTouchSlop = 2 * ViewConfiguration.get(launcher).getScaledTouchSlop();
        mGestureDetector = new GestureDetector(workspace.getContext(), this);
    }

    @Override
    public boolean onTouch(View view, MotionEvent ev) {
        mGestureDetector.onTouchEvent(ev);

        int action = ev.getActionMasked();
		android.util.Log.d("hws", "onTouch.....");
        if (action == ACTION_DOWN) {
            mTouchCoordX = ev.getX();
            mTouchCoordY = ev.getY();
            // Check if we can handle long press.
            boolean handleLongPress = canHandleLongPress();

            if (handleLongPress) {
                // Check if the event is not near the edges
                DeviceProfile dp = mLauncher.getDeviceProfile();
                DragLayer dl = mLauncher.getDragLayer();
                Rect insets = dp.getInsets();

                mTempRect.set(insets.left, insets.top, dl.getWidth() - insets.right,
                        dl.getHeight() - insets.bottom);
                mTempRect.inset(dp.edgeMarginPx, dp.edgeMarginPx);
                handleLongPress = mTempRect.contains((int) ev.getX(), (int) ev.getY());
            }

            if (handleLongPress) {
                mLongPressState = STATE_REQUESTED;
                mTouchDownPoint.set(ev.getX(), ev.getY());
                // Mouse right button's ACTION_DOWN should immediately show menu
                if (TouchUtil.isMouseRightClickDownOrMove(ev)) {
                    maybeShowMenu();
                    return true;
                }
            }

            mWorkspace.onTouchEvent(ev);
            // Return true to keep receiving touch events
            return true;
        }

        if (mLongPressState == STATE_PENDING_PARENT_INFORM) {
            // Inform the workspace to cancel touch handling
            ev.setAction(ACTION_CANCEL);
            mWorkspace.onTouchEvent(ev);

            ev.setAction(action);
            mLongPressState = STATE_COMPLETED;
        }

        boolean isInAllAppsBottomSheet = mLauncher.isInState(ALL_APPS)
                && mLauncher.getDeviceProfile().isTablet;

        final boolean result;
        if (mLongPressState == STATE_COMPLETED) {
            // We have handled the touch, so workspace does not need to know anything anymore.
            result = true;
        } else if (mLongPressState == STATE_REQUESTED) {
            mWorkspace.onTouchEvent(ev);
            if (mWorkspace.isHandlingTouch()) {
                cancelLongPress();
            } else if (action == ACTION_MOVE && PointF.length(
                    mTouchDownPoint.x - ev.getX(), mTouchDownPoint.y - ev.getY()) > mTouchSlop) {
                cancelLongPress();
            }

            result = true;
        } else {
            // We don't want to handle touch unless we're in AllApps bottom sheet, let workspace
            // handle it as usual.
            result = isInAllAppsBottomSheet;
        }

        if (action == ACTION_UP || action == ACTION_POINTER_UP) {
            if (!mWorkspace.isHandlingTouch()) {
                final CellLayout currentPage =
                        (CellLayout) mWorkspace.getChildAt(mWorkspace.getCurrentPage());
                if (currentPage != null) {
                    mWorkspace.onWallpaperTap(ev);
                }
            }
        }

        if (action == ACTION_UP || action == ACTION_CANCEL) {
            cancelLongPress();
        }
        if (action == ACTION_UP && isInAllAppsBottomSheet) {
            mLauncher.getStateManager().goToState(NORMAL);
            mLauncher.getStatsLogManager().logger()
                    .withSrcState(ALL_APPS.statsLogOrdinal)
                    .withDstState(NORMAL.statsLogOrdinal)
                    .withContainerInfo(LauncherAtom.ContainerInfo.newBuilder()
                            .setWorkspace(
                                    LauncherAtom.WorkspaceContainer.newBuilder()
                                            .setPageIndex(
                                                    mLauncher.getWorkspace().getCurrentPage()))
                            .build())
                    .log(LAUNCHER_ALLAPPS_CLOSE_TAP_OUTSIDE);
        }
        if (mLauncher.getStateManager().getState() != NORMAL) {
            if (Math.abs(ev.getX() - mTouchCoordX) < 5 && Math.abs(ev.getY() - mTouchCoordY) < 5 && action == ACTION_UP) {
                AbstractFloatingView topView = AbstractFloatingView.getTopOpenView(mLauncher);
                if (topView != null) {
                    topView.close(false);
                }
                mLauncher.getStateManager().goToState(NORMAL);
            } else if (mWorkspace.getCurrentPage() == 0 && ev.getX() - mTouchCoordX > 0) {
                return true;
            }
        }
        return result;
    }

    private boolean canHandleLongPress() {
        return AbstractFloatingView.getTopOpenView(mLauncher) == null
                && mLauncher.isInState(NORMAL);
    }

    private void cancelLongPress() {
        mLongPressState = STATE_CANCELLED;
    }

    @Override
    public void onLongPress(MotionEvent event) {
        maybeShowMenu();
    }

    // add by hxy liu-db for double tap to wake off start
    @Override
    public boolean onDoubleTap(MotionEvent event) {
        if (Settings.Global.getInt(mLauncher.getContentResolver(), "persist.sys.double_tap_to_off", 0) == 1) {
            Intent intent = new Intent("com.yft.power.wake_off");
            mLauncher.sendBroadcast(intent);
        }
        return true;
    }
    // add by hxy liu-db for double tap to wake off end

    private void maybeShowMenu() {
        if (mLongPressState == STATE_REQUESTED) {
            TestLogging.recordEvent(TestProtocol.SEQUENCE_MAIN, "Workspace.longPress");
            if (canHandleLongPress()) {
                if (!mLauncher.getSharedPrefs().getBoolean(LauncherPrefs.WORKSPACE_LAYOUT_DOCK, false)) {
                    Toast.makeText(mLauncher, R.string.shake_device_tips,Toast.LENGTH_SHORT).show();
                }
                mLongPressState = STATE_PENDING_PARENT_INFORM;
                mWorkspace.getParent().requestDisallowInterceptTouchEvent(true);
                  android.util.Log.d("hws", "maybeShowMenu.....");
                  Vibrator vibrator=(Vibrator) mWorkspace.getContext().getSystemService(Context.VIBRATOR_SERVICE);
				  vibrator.vibrate(20);
            //    mWorkspace.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS,
            //            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
                mLauncher.getStatsLogManager().logger().log(LAUNCHER_WORKSPACE_LONGPRESS);
                mLauncher.showDefaultOptions(mTouchDownPoint.x, mTouchDownPoint.y);
                mLauncher.getStateManager().goToState(SPRING_LOADED);
                mLauncher.getHotseat().setVisibility(View.GONE);
            } else {
                cancelLongPress();
            }
        }
    }
}
