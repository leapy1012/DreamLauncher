/*
 * Copyright (C) 2026 GreenDream
 */
package com.android.quickstep.views;

import static android.view.Surface.ROTATION_0;

import static com.android.launcher3.anim.Interpolators.ACCEL;
import static com.android.launcher3.anim.Interpolators.EMPHASIZED_DECELERATE;
import static com.android.launcher3.util.Executors.UI_HELPER_EXECUTOR;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Process;
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowInsets;

import androidx.annotation.Nullable;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.R;
import com.android.launcher3.RecentContainerView;
import com.android.launcher3.anim.PendingAnimation;
import com.android.launcher3.statemanager.BaseState;
import com.android.launcher3.statemanager.StatefulActivity;
import com.android.quickstep.BaseActivityInterface;
import com.android.quickstep.GestureState;
import com.android.quickstep.RotationTouchHelper;
import com.android.quickstep.util.SplitSelectStateController;
import com.android.systemui.shared.recents.model.Task;
import com.coui.appcompat.snackbar.COUISnackBar;
import com.dream.quickstep.dock.DockView;
import com.dream.quickstep.views.DreamClearAllPanelView;

import java.util.List;

/**
 * Dream Recents behavior on top of the AOSP {@link RecentsView}: the bottom UI (dock and
 * Clear-all panel, siblings in {@link RecentContainerView}), the task menu fade while scrolling,
 * and memory cleanup after Clear all.
 */
public abstract class DreamRecentsViewImpl<ACTIVITY_TYPE extends StatefulActivity<STATE_TYPE>,
        STATE_TYPE extends BaseState<STATE_TYPE>> extends RecentsView<ACTIVITY_TYPE, STATE_TYPE> {

    private static final String TAG = "DreamRecentsViewImpl";

    /** Killed whenever its task is dismissed, so playback stops with it. */
    private static final String MUSIC_PACKAGE = "com.android.music";

    /** Dock and Clear all sit below the task list instead of the AOSP Clear-all page. */
    private final boolean mClearAllCenter;

    @Nullable
    private View mClearAllButtonNew;
    @Nullable
    private DockView mDockView;
    @Nullable
    private DreamClearAllPanelView mClearAllPanel;
    @Nullable
    private Animator mBottomUiAnimator;
    private boolean mBottomUiEntered;
    @Nullable
    private Runnable mPendingBottomUiEnter;
    private boolean mBottomUiEnterRequested;
    private boolean mBottomUiAnimationEntering;

    private boolean mSyncRunning = false;
    private long mNewFreeMemory;
    private long mOldFreeMemory;

    public DreamRecentsViewImpl(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
            BaseActivityInterface sizeStrategy) {
        super(context, attrs, defStyleAttr, sizeStrategy);
        mClearAllCenter = getResources().getBoolean(R.bool.config_clearall_center);
        if (mClearAllCenter) {
            // The AOSP Clear-all page stays in the task list (scroll math relies on it) but is
            // never shown.
            ClearAllButton clearAllButton = getClearAllButton();
            ViewGroup.LayoutParams lp = clearAllButton.getLayoutParams();
            lp.width = 1;
            lp.height = 1;
            clearAllButton.setLayoutParams(lp);
            clearAllButton.setVisibility(INVISIBLE);
        }
    }

    @Override
    public void init(OverviewActionsView actionsView, SplitSelectStateController splitController) {
        super.init(actionsView, splitController);
        if (mClearAllCenter && mClearAllPanel != null) {
            mClearAllButtonNew = mClearAllPanel.getClearButton();
        } else {
            mClearAllButtonNew = mActionsView.getClearAllView();
        }
        if (mClearAllButtonNew != null) {
            mClearAllButtonNew.setOnClickListener(this::dismissAllTasks);
        }
        if (mDockView != null) {
            mDockView.setRecentsView(this);
        }
    }

    /** Oppo-style bottom UI wired from {@link RecentContainerView}. */
    public void setBottomUi(@Nullable DockView dock, @Nullable DreamClearAllPanelView panel) {
        mDockView = dock;
        mClearAllPanel = panel;
        if (mDockView != null) {
            mDockView.setRecentsView(this);
        }
        updateBottomUiInsets();
    }

    public void updateBottomUiInsets() {
        if (!mClearAllCenter || mClearAllPanel == null) {
            return;
        }
        ViewParent parent = getParent();
        if (parent instanceof RecentContainerView) {
            ((RecentContainerView) parent).updateBottomUiInsets();
        }
    }

    public DeviceProfile getDeviceProfile() {
        return mActivity.getDeviceProfile();
    }

    public Rect getOverviewInsets() {
        return mInsets;
    }

    public int getTaskWidth() {
        return mTaskWidth;
    }

    /** Computes task bounds without notifying bottom UI (avoids recursion with container). */
    public void computeTaskSize(Rect outRect) {
        super.getTaskSize(outRect);
    }

    @Override
    public void getTaskSize(Rect outRect) {
        super.getTaskSize(outRect);
        updateBottomUiInsets();
    }

    @Override
    protected void updateTaskSize() {
        super.updateTaskSize();
        updateBottomUiInsets();
    }

    @Override
    public void setInsets(Rect insets) {
        super.setInsets(insets);
        updateBottomUiInsets();
    }

    private void syncDockFromRecents() {
        if (mClearAllCenter && mDockView != null) {
            DeviceProfile dp = getDeviceProfile();
            if (dp.isTablet || dp.isLandscape) {
                mDockView.setVisibility(GONE);
            } else {
                // Always refresh icon data; visibility is gated separately for overview-only.
                mDockView.syncFromRecents(this);
            }
            updateBottomUiOverviewVisibility();
            updateBottomUiInsets();
        }
    }

    /** Whether dock linking can safely use this task list's page coordinates. */
    public boolean isDockScrollReady() {
        return isPageScrollsInitialized() && isLaidOut() && !isLayoutRequested();
    }

    /** Coordinate linking is for movement; a settled dock follows the selected task ID. */
    public boolean isRecentsScrollInProgress() {
        return mGestureActive || isPageInTransition();
    }

    @Override
    public void onViewRemoved(View child) {
        super.onViewRemoved(child);
        if (child instanceof TaskView && child != mSplitHiddenTaskView
                && child != mMovingTaskView) {
            syncDockFromRecents();
        }
    }

    @Override
    public boolean isClearAllHidden() {
        return mClearAllCenter || super.isClearAllHidden();
    }

    @Override
    protected boolean shouldAddClearAllButtonWithRunningTask() {
        return !mClearAllCenter;
    }

    @Override
    public void setDisallowScrollToClearAll(boolean disallowScrollToClearAll) {
        // The hidden Clear-all page must not extend the scroll range past the oldest task.
        super.setDisallowScrollToClearAll(mClearAllCenter || disallowScrollToClearAll);
    }

    @Override
    protected void onPageEndTransition() {
        super.onPageEndTransition();
        if (mDockView != null) {
            mDockView.recenterOnCurrentRecentsPage();
        }
    }

    @Override
    protected void determineScrollingStart(MotionEvent ev, float touchSlopScale) {
        boolean wasHandling = isHandlingTouch();
        super.determineScrollingStart(ev, touchSlopScale);
        // ColorOS DockView.onParentScrollInteractionBegin — Recents owns link-scroll.
        if (!wasHandling && isHandlingTouch() && mDockView != null && mClearAllCenter) {
            mDockView.onParentScrollInteractionBegin();
        }
    }

    @Override
    protected void onTaskStackUpdated() {
        super.onTaskStackUpdated();
        syncDockFromRecents();
        if (!mGestureActive && mFullscreenProgress <= 0.05f && !mBottomUiEntered
                && getContentAlpha() > 0f) {
            playBottomUiEnter();
        }
    }

    @Override
    public void setFullscreenProgress(float fullscreenProgress) {
        super.setFullscreenProgress(fullscreenProgress);
        // An enter request can arrive before the final fullscreen-progress update.
        if (mBottomUiEnterRequested && fullscreenProgress <= 0.05f) {
            scheduleBottomUiEnter();
        }
    }

    @Override
    public void updateCurveProperties() {
        super.updateCurveProperties();
        if (getPageCount() == 0 || getPageAt(0).getMeasuredWidth() == 0) {
            return;
        }
        int scroll = mOrientationHandler.getPrimaryScroll(this);
        updateTaskMenuShortcutAlphas(scroll);
        if (!mClearAllCenter && mActionsView != null) {
            float bottomUiAlpha = mOrientationState.getTouchRotation() == ROTATION_0
                    ? getContentAlpha() : 0f;
            mActionsView.getIndexScrollAlpha().setValue(bottomUiAlpha);
        }
        if (mDockView != null && mDockView.getVisibility() == VISIBLE) {
            if (!mDockView.isDockDrivingRecents()) {
                // ColorOS DockView.onRecentsScrollTo — continuous scale-linked scroll.
                mDockView.onRecentsScrollTo(scroll);
            }
        } else if (mActionsView != null) {
            mActionsView.onRecentsPageCentered(getTaskViewNearestToCenterOfScreen());
        }
    }

    /**
     * Oppo-style menu fade ({@code OplusTaskViewImpl.updateCurveInterpolation} /
     * {@code ScrollState.updateInterpolation}).
     * <p>
     * {@code linearInterpolation} rises slowly (denom ≈ halfPage + halfScreen), then a cosine
     * ease keeps near-center cards near alpha 1. Nearest-to-center is forced to 1 so the focused
     * card never loses the menu.
     */
    private void updateTaskMenuShortcutAlphas(int primaryScroll) {
        int taskCount = getTaskViewCount();
        if (taskCount == 0) {
            return;
        }
        TaskView nearest = getTaskViewNearestToCenterOfScreen();
        if (showAsGrid()) {
            TaskView focused = getTaskViewAt(getNextPage());
            if (focused == null) {
                focused = nearest;
            }
            for (int i = 0; i < taskCount; i++) {
                TaskView tv = requireTaskViewAt(i);
                setMenuShortcutAlpha(tv, tv == focused ? 1f : 0f);
            }
            return;
        }

        float halfScreen = getMeasuredWidth() / 2f;
        float screenCenter = primaryScroll + halfScreen;
        float edgeScale = TaskView.getEdgeScaleDownFactor(mActivity.getDeviceProfile());
        for (int i = 0; i < taskCount; i++) {
            TaskView tv = requireTaskViewAt(i);
            if (tv == nearest) {
                // Active / nearest card always keeps a full-opacity menu (Oppo idle + settle-safe).
                setMenuShortcutAlpha(tv, 1f);
                continue;
            }
            float halfPage = tv.getMeasuredWidth() / 2f;
            if (halfPage <= 0f) {
                setMenuShortcutAlpha(tv, 0f);
                continue;
            }
            // Match OplusBasePagedOrientationHandler.ScrollState.updateInterpolation.
            float childStart = tv.getLeft() + tv.getTranslationX();
            float denom = ((1f - edgeScale) * halfPage) + halfScreen;
            float linear = denom <= 0f ? 1f
                    : Math.min(1f, Math.abs(screenCenter - (childStart + halfPage)) / denom);
            // CURVE_INTERPOLATOR: (-cos(t * PI) / 2) + 0.5
            float curved = (float) ((-Math.cos(linear * Math.PI) / 2.0) + 0.5);
            // getCurveAlphaForCurveInterpolation(curved, minAlpha=0) → lerp(1, 0, curved), 0.1 steps
            float menuAlpha = Math.round((1f - curved) * 10f) / 10f;
            setMenuShortcutAlpha(tv, menuAlpha);
        }
    }

    private static void setMenuShortcutAlpha(TaskView taskView, float alpha) {
        if (taskView instanceof DreamTaskViewImpl) {
            ((DreamTaskViewImpl) taskView).setMenuShortcutAlpha(alpha);
        }
    }

    @Override
    public void reset(boolean isCancelAnimation) {
        super.reset(isCancelAnimation);
        cancelPendingBottomUiEnter();
        hideBottomUiImmediate();
    }

    @Override
    public void onGestureAnimationStart(Task[] runningTasks,
            RotationTouchHelper rotationTouchHelper) {
        super.onGestureAnimationStart(runningTasks, rotationTouchHelper);
        hideBottomUiImmediate();
    }

    @Override
    public void onSwipeUpAnimationSuccess() {
        super.onSwipeUpAnimationSuccess();
        playBottomUiEnter();
    }

    @Override
    public void onGestureAnimationEnd() {
        boolean toRecents = mCurrentGestureEndTarget == GestureState.GestureEndTarget.RECENTS;
        super.onGestureAnimationEnd();
        if (toRecents) {
            playBottomUiEnter();
        }
    }

    @Override
    public void createTaskDismissAnimation(PendingAnimation anim, TaskView dismissedTaskView,
            boolean animateTaskView, boolean shouldRemoveTask, long duration,
            boolean dismissingForSplitSelection) {
        super.createTaskDismissAnimation(anim, dismissedTaskView, animateTaskView,
                shouldRemoveTask, duration, dismissingForSplitSelection);
        if (mDockView != null) {
            mDockView.createIconDismissAnimation(dismissedTaskView, anim, animateTaskView);
        }
    }

    @Override
    protected void onDismissAnimationEnds() {
        super.onDismissAnimationEnds();
        // Dock rebuilt in onViewRemoved before setCurrentPage — re-sync so the centered/active
        // icon matches the page we just snapped to.
        syncDockFromRecents();
    }

    @Override
    protected void onRemoveTaskView(@Nullable TaskView taskView) {
        forceStopIfMusic(taskView);
    }

    @Override
    public PendingAnimation createAllTasksDismissAnimation(long duration) {
        for (int i = getTaskViewCount() - 1; i >= 0; i--) {
            forceStopIfMusic(requireTaskViewAt(i));
        }
        return super.createAllTasksDismissAnimation(duration);
    }

    @Override
    protected void onAllTasksDismissed() {
        UI_HELPER_EXECUTOR.getHandler().postDelayed(this::clearMemorySchedule, 500);
    }

    private void forceStopIfMusic(@Nullable TaskView taskView) {
        if (taskView == null) {
            return;
        }
        try {
            String packageName = taskView.getTask().key.getPackageName();
            if (MUSIC_PACKAGE.equals(packageName)) {
                getContext().getSystemService(ActivityManager.class)
                        .forceStopPackage(packageName);
            }
        } catch (Exception e) {
            Log.e(TAG, "forceStopPackage error" + e);
        }
    }

    @Override
    public void setContentAlpha(float alpha) {
        boolean changed = alpha != getContentAlpha();
        super.setContentAlpha(alpha);
        if (changed) {
            applyBottomUiContentAlpha(getContentAlpha());
        }
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        // Dock + Clear All are siblings — hide them whenever overview itself is not shown.
        updateBottomUiOverviewVisibility();
        if (visibility == VISIBLE && mBottomUiEnterRequested) {
            scheduleBottomUiEnter();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        boolean skipped = !mOverviewStateEnabled && !mFirstLayout;
        super.onLayout(changed, left, top, right, bottom);
        if (!skipped && mDockView != null) {
            // Dock may have laid out before Recents, or kept the same size across a load plan.
            mDockView.recenterOnCurrentRecentsPage();
        }
    }

    @Override
    public void setOverviewFullscreenEnabled(boolean overviewFullscreenEnabled) {
        boolean changed = mOverviewFullscreenEnabled != overviewFullscreenEnabled;
        super.setOverviewFullscreenEnabled(overviewFullscreenEnabled);
        if (changed && !overviewFullscreenEnabled && mBottomUiEnterRequested) {
            scheduleBottomUiEnter();
        }
    }

    /** True when Clear All / dock icons should be on screen (overview only). */
    public boolean shouldShowBottomUi() {
        return mOverviewStateEnabled && !mOverviewFullscreenEnabled
                && getVisibility() == VISIBLE && getContentAlpha() > 0f
                && getTaskViewCount() > 0;
    }

    private void updateBottomUiOverviewVisibility() {
        if (!mClearAllCenter) {
            return;
        }
        boolean show = shouldShowBottomUi();
        if (mClearAllPanel != null) {
            mClearAllPanel.setVisibility(show ? VISIBLE : GONE);
            if (show) {
                applySettledBottomUiAlpha(mClearAllPanel);
            }
        }
        if (mDockView != null) {
            DeviceProfile dp = getDeviceProfile();
            boolean showDock = show && !dp.isTablet && !dp.isLandscape;
            mDockView.setVisibility(showDock ? VISIBLE : GONE);
            if (showDock) {
                applySettledBottomUiAlpha(mDockView);
            }
        }
    }

    private void applyBottomUiContentAlpha(float alpha) {
        if (!mClearAllCenter) {
            return;
        }
        // Leaving recents: drop the footer immediately. Enter is a separate delayed fade.
        if (alpha <= 0f) {
            hideBottomUiImmediate();
        } else {
            updateBottomUiOverviewVisibility();
            if (!mGestureActive && mFullscreenProgress <= 0.05f && !mBottomUiEntered) {
                playBottomUiEnter();
            }
        }
    }

    /**
     * Oppo {@code OplusClearAllPanelView.playEnterAnimation}: dock + RAM + Clear all stay
     * hidden until recents cards have settled, then fade/rise in.
     */
    public void playBottomUiEnter() {
        if (!mClearAllCenter) {
            return;
        }
        cancelPendingBottomUiEnter();
        mBottomUiEnterRequested = true;
        if (mOrientationState.getTouchRotation() != ROTATION_0) {
            hideBottomUiImmediate();
            return;
        }
        // Still in the app-to-recents swipe (BACKGROUND_APP): keep footer hidden.
        if (!mOverviewStateEnabled || mOverviewFullscreenEnabled
                || mGestureActive || mFullscreenProgress > 0.05f) {
            // Keep the request until gesture end, fullscreen progress, or task binding changes.
            return;
        }
        // Icons bind after the gesture; sync first so the dock is VISIBLE for this fade.
        syncDockFromRecents();
        if (!shouldShowBottomUi()) {
            // Task binding/content visibility will retry the retained request.
            return;
        }
        if (mBottomUiEntered && getBottomUiAlpha() > 0.99f) {
            mBottomUiEnterRequested = false;
            applySettledBottomUiAlpha(mDockView);
            return;
        }
        if (mBottomUiAnimator != null && mBottomUiAnimator.isRunning()
                && mBottomUiAnimationEntering) {
            return;
        }
        // An old exit must not prevent a new enter.
        cancelBottomUiAnim();
        float shift = getResources().getDimension(R.dimen.hxy_overview_bottom_ui_enter_shift);
        AnimatorSet anim = new AnimatorSet();
        boolean hasTarget = false;
        if (mClearAllPanel != null && mClearAllPanel.getVisibility() == VISIBLE) {
            mClearAllPanel.setAlpha(0f);
            mClearAllPanel.setTranslationY(shift);
            anim.play(ObjectAnimator.ofFloat(mClearAllPanel, View.ALPHA, 0f, 1f));
            anim.play(ObjectAnimator.ofFloat(mClearAllPanel, View.TRANSLATION_Y, shift, 0f));
            hasTarget = true;
        }
        if (mDockView != null && mDockView.getVisibility() == VISIBLE) {
            // Dock Y is set via DockView.setY() — do not touch translationY.
            mDockView.setAlpha(0f);
            anim.play(ObjectAnimator.ofFloat(mDockView, View.ALPHA, 0f, 1f));
            hasTarget = true;
        }
        if (!hasTarget) {
            return;
        }
        if (mClearAllPanel != null) {
            mClearAllPanel.updateMemoryInfoDisplay();
        }
        anim.setDuration(600);
        anim.setInterpolator(EMPHASIZED_DECELERATE);
        anim.addListener(new AnimatorListenerAdapter() {
            private boolean mCancelled;

            @Override
            public void onAnimationCancel(Animator animation) {
                mCancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (mBottomUiAnimator != animation) {
                    return;
                }
                mBottomUiAnimator = null;
                if (mCancelled || !mBottomUiEnterRequested || !shouldShowBottomUi()) {
                    return;
                }
                mBottomUiEntered = true;
                mBottomUiEnterRequested = false;
                // Dock may bind after this fade; reveal it if it appeared late.
                updateBottomUiOverviewVisibility();
            }
        });
        mBottomUiAnimationEntering = true;
        mBottomUiAnimator = anim;
        anim.start();
    }

    private void scheduleBottomUiEnter() {
        if (!mBottomUiEnterRequested || mPendingBottomUiEnter != null) {
            return;
        }
        mPendingBottomUiEnter = () -> {
            mPendingBottomUiEnter = null;
            playBottomUiEnter();
        };
        postOnAnimation(mPendingBottomUiEnter);
    }

    private void cancelPendingBottomUiEnter() {
        if (mPendingBottomUiEnter != null) {
            removeCallbacks(mPendingBottomUiEnter);
            mPendingBottomUiEnter = null;
        }
    }

    private void applySettledBottomUiAlpha(View view) {
        if (view == null) {
            return;
        }
        if (mBottomUiEntered) {
            view.setAlpha(1f);
        } else if (mBottomUiAnimator == null || !mBottomUiAnimator.isRunning()) {
            view.setAlpha(0f);
        }
    }

    /** Oppo {@code playExitAnimator}: fade footer as soon as we leave Overview. */
    public void playBottomUiExit() {
        if (!mClearAllCenter) {
            return;
        }
        cancelPendingBottomUiEnter();
        mBottomUiEnterRequested = false;
        cancelBottomUiAnim();
        mBottomUiEntered = false;
        if (getBottomUiAlpha() <= 0.01f) {
            hideBottomUiImmediate();
            return;
        }
        AnimatorSet anim = new AnimatorSet();
        if (mClearAllPanel != null) {
            anim.play(ObjectAnimator.ofFloat(
                    mClearAllPanel, View.ALPHA, mClearAllPanel.getAlpha(), 0f));
        }
        if (mDockView != null) {
            anim.play(ObjectAnimator.ofFloat(
                    mDockView, View.ALPHA, mDockView.getAlpha(), 0f));
        }
        anim.setDuration(240);
        anim.setInterpolator(ACCEL);
        anim.addListener(new AnimatorListenerAdapter() {
            private boolean mCancelled;

            @Override
            public void onAnimationCancel(Animator animation) {
                mCancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (mBottomUiAnimator != animation) {
                    return;
                }
                mBottomUiAnimator = null;
                if (!mCancelled) {
                    hideBottomUiImmediate();
                }
            }
        });
        mBottomUiAnimationEntering = false;
        mBottomUiAnimator = anim;
        anim.start();
    }

    public void hideBottomUiImmediate() {
        cancelPendingBottomUiEnter();
        mBottomUiEnterRequested = false;
        cancelBottomUiAnim();
        mBottomUiEntered = false;
        if (mClearAllPanel != null) {
            mClearAllPanel.setAlpha(0f);
            mClearAllPanel.setTranslationY(0f);
        }
        if (mDockView != null) {
            mDockView.setAlpha(0f);
        }
        if (mClearAllCenter) {
            updateBottomUiOverviewVisibility();
        }
    }

    private void cancelBottomUiAnim() {
        if (mBottomUiAnimator != null) {
            // Invalidate ownership before cancel() dispatches onAnimationEnd().
            Animator animator = mBottomUiAnimator;
            mBottomUiAnimator = null;
            animator.cancel();
        }
    }

    private float getBottomUiAlpha() {
        if (mClearAllPanel != null && mClearAllPanel.getVisibility() == VISIBLE) {
            return mClearAllPanel.getAlpha();
        }
        if (mDockView != null && mDockView.getVisibility() == VISIBLE) {
            return mDockView.getAlpha();
        }
        return 0f;
    }

    public void hxyClearAllTasks() {
        runDismissAnimation(createAllTasksDismissAnimation(DISMISS_TASK_DURATION));
    }

    public void clearMemorySchedule() {
        if (mSyncRunning) {
            return;
        }
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        getContext().getSystemService(ActivityManager.class).getMemoryInfo(memInfo);
        mOldFreeMemory = memInfo.availMem;
        new ClearMemorySyncTask().execute(getContext());
    }

    private class ClearMemorySyncTask extends AsyncTask<Context, Integer, Context> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mSyncRunning = true;
        }

        @Override
        protected Context doInBackground(Context... context) {
            ActivityManager activityMgr = context[0].getSystemService(ActivityManager.class);
            PackageManager packageMgr = context[0].getPackageManager();
            List<ActivityManager.RunningAppProcessInfo> runningProcList =
                    activityMgr.getRunningAppProcesses();
            if (runningProcList == null) {
                return context[0];
            }

            String self = context[0].getPackageName();
            Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
            launcherIntent.addCategory(Intent.CATEGORY_HOME);
            ResolveInfo launcher = packageMgr.resolveActivity(launcherIntent,
                    PackageManager.MATCH_DEFAULT_ONLY);

            String[] filtList = getContext().getResources().getStringArray(
                    com.android.internal.R.array.hxy_config_deep_clean_whitelist);
            for (ActivityManager.RunningAppProcessInfo runningAppProcessInfo : runningProcList) {
                String pkgName = runningAppProcessInfo.processName;
                if (runningAppProcessInfo.processName != null) {
                    String[] items = runningAppProcessInfo.processName.split(":");
                    if (items.length > 0) {
                        pkgName = items[0];
                    }
                }

                if (self.equals(pkgName)) {
                    continue;
                }
                if (launcher.activityInfo.packageName.equals(pkgName)) {
                    continue;
                }
                if (Settings.System.getInt(context[0].getContentResolver(), pkgName, 0) == 1) {
                    continue;
                }
                if (containsIgnoreCase(filtList, pkgName)) {
                    continue;
                }

                if (runningAppProcessInfo.importance
                        >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_CANT_SAVE_STATE) {
                    for (String pkg : runningAppProcessInfo.pkgList) {
                        if (Settings.System.getInt(context[0].getContentResolver(), pkg, 0) == 1) {
                            continue;
                        }
                        if (pkg.startsWith("com.google")
                                || pkg.contains("com.mediatek.schpwronoff")
                                || pkg.contains("com.android.mtp")
                                || pkg.contains("android.process.media")
                                || pkg.contains("com.hxy.factorymode")
                                || containsIgnoreCase(filtList, pkg)) {
                            continue;
                        }
                        Log.d(TAG, "kill " + pkg);
                        activityMgr.killBackgroundProcesses(pkg);
                        if (runningAppProcessInfo.uid > Process.FIRST_APPLICATION_UID) {
                            activityMgr.killUid(runningAppProcessInfo.uid,
                                    "ClearMemorySyncTask kill app");
                        }
                    }
                }
            }
            return context[0];
        }

        @Override
        protected void onPostExecute(Context context) {
            mSyncRunning = false;
            showCleanCompleted();
        }
    }

    private static boolean containsIgnoreCase(String[] list, String value) {
        for (String str : list) {
            if (value.equalsIgnoreCase(str)) {
                return true;
            }
        }
        return false;
    }

    private void showCleanCompleted() {
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        getContext().getSystemService(ActivityManager.class).getMemoryInfo(memInfo);
        mNewFreeMemory = memInfo.availMem;
        long freedMemory = mNewFreeMemory - mOldFreeMemory;
        Log.d(TAG, "showCleanCompleted. mNewFreeMemory = " + mNewFreeMemory
                + ", mOldFreeMemory = " + mOldFreeMemory + ". freedMemory = " + freedMemory);

        String message = freedMemory <= 0
                ? getContext().getString(R.string.hxy_clear_memrey_all_locked)
                : getContext().getString(R.string.hxy_clear_memrey_toast,
                        Formatter.formatShortFileSize(getContext(), freedMemory));

        // ColorOS tip: compact wrap-content COUISnackBar on the drag layer (not a stretched
        // Recents child), with bottom inset above the nav bar.
        post(() -> {
            View host = mActivity != null ? mActivity.getDragLayer() : this;
            if (host == null) {
                host = this;
            }
            int bottomMargin = getResources().getDimensionPixelSize(
                    com.coui.appcompat.R.dimen.coui_snack_bar_margin_bottom);
            WindowInsets insets = host.getRootWindowInsets();
            if (insets != null) {
                bottomMargin += insets.getSystemWindowInsetBottom();
            }
            COUISnackBar.make(host.getContext(), host, message, 2500, bottomMargin).show();
        });
    }
}
