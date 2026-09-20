package com.android.launcher3.util;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Point;
import android.view.View;
import android.view.ViewGroup;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.DropTarget;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.dragndrop.DragOptions;
import com.android.launcher3.settings.SettingsActivity;
import com.coui.appcompat.snackbar.COUISnackBar;

/**
 * Home-screen layout lock: gate edits and show an Oppo-style {@link COUISnackBar}
 * with a "View settings" action (see ColorOS {@code ToastUtils.showSnackBar} /
 * {@code WorkspaceInjector} pre-drag threshold).
 */
public final class LayoutLockHelper {

    private static final int SNACKBAR_DURATION_MS = 2500;
    private static final long SHOW_DELAY_MS = 200L;

    /** Floating views that should not sit on top of the lock snackbar. */
    private static final int FLOATING_TO_DISMISS =
            AbstractFloatingView.TYPE_ACTION_POPUP
                    | AbstractFloatingView.TYPE_OPTIONS_POPUP
                    | AbstractFloatingView.TYPE_OPTIONS_POPUP_DIALOG
                    | AbstractFloatingView.TYPE_SNACKBAR;

    private LayoutLockHelper() { }

    public static boolean isLayoutLocked(Context context) {
        return LauncherPrefs.getPrefs(context)
                .getBoolean(LauncherPrefs.WORKSPACE_LAYOUT_DOCK, false);
    }

    public static boolean checkLockedAndShowMessage(Context context) {
        if (!isLayoutLocked(context)) {
            return false;
        }
        Launcher launcher = resolveLauncher(context);
        if (launcher != null) {
            showLockedSnackbar(launcher);
        }
        return true;
    }

    public static boolean checkLockedAndShowMessage(Launcher launcher) {
        if (!isLayoutLocked(launcher)) {
            return false;
        }
        showLockedSnackbar(launcher);
        return true;
    }

    /**
     * Oppo: toast + cancel only after the finger moves past the deep-shortcut
     * drag threshold — not on long-press alone.
     */
    public static DragOptions.PreDragCondition wrapPreDragCondition(
            Launcher launcher, DragOptions.PreDragCondition inner) {
        if (launcher == null || !isLayoutLocked(launcher)) {
            return inner;
        }
        final int threshold = launcher.getResources().getDimensionPixelSize(
                R.dimen.deep_shortcuts_start_drag_threshold);
        return new DragOptions.PreDragCondition() {
            private boolean mLockHandled;

            @Override
            public boolean shouldStartDrag(double distanceDragged) {
                // Below threshold: stay in pre-drag (allow popup / lift).
                if (distanceDragged <= threshold) {
                    return false;
                }
                // Past threshold while locked: snackbar once, cancel, never promote.
                if (!mLockHandled) {
                    mLockHandled = true;
                    if (checkLockedAndShowMessage(launcher)) {
                        launcher.getDragController().cancelDrag();
                        return false;
                    }
                } else if (isLayoutLocked(launcher)) {
                    return false;
                }
                return inner == null || inner.shouldStartDrag(distanceDragged);
            }

            @Override
            public void onPreDragStart(DropTarget.DragObject dragObject) {
                if (inner != null) {
                    inner.onPreDragStart(dragObject);
                }
            }

            @Override
            public void onPreDragEnd(DropTarget.DragObject dragObject, boolean dragStarted) {
                if (inner != null) {
                    inner.onPreDragEnd(dragObject, dragStarted);
                }
            }

            @Override
            public Point getDragOffset() {
                return inner != null ? inner.getDragOffset() : new Point(0, 0);
            }
        };
    }

    private static Launcher resolveLauncher(Context context) {
        if (context instanceof Launcher) {
            return (Launcher) context;
        }
        Context walk = context;
        while (walk instanceof ContextWrapper) {
            walk = ((ContextWrapper) walk).getBaseContext();
            if (walk instanceof Launcher) {
                return (Launcher) walk;
            }
        }
        return Launcher.ACTIVITY_TRACKER.getCreatedActivity();
    }

    private static void showLockedSnackbar(Launcher launcher) {
        // Oppo: dismiss app/options popups so the snackbar is the only chrome.
        AbstractFloatingView.closeOpenViews(launcher, true /* animate */, FLOATING_TO_DISMISS);

        View anchor = launcher.getDragLayer();
        if (anchor == null) {
            anchor = launcher.getWindow().getDecorView();
        }
        final View host = anchor;
        host.postDelayed(() -> {
            if (launcher.isFinishing() || launcher.getWindow() == null) {
                return;
            }
            dismissExistingCouiSnackBars(host);

            int bottomMargin = launcher.getResources().getDimensionPixelSize(
                    com.coui.appcompat.R.dimen.coui_snack_bar_margin_bottom);
            // Gesture nav: keep clear of the system gesture inset.
            bottomMargin += launcher.getDeviceProfile().getInsets().bottom;

            String message = launcher.getString(R.string.home_screen_layout_lock_tips);
            COUISnackBar snackBar = COUISnackBar.make(
                    launcher, host, message, SNACKBAR_DURATION_MS, bottomMargin);
            snackBar.setOnAction(R.string.launcher_locked_toast_look_setting, v -> {
                launcher.startActivity(new Intent(Intent.ACTION_APPLICATION_PREFERENCES)
                        .setPackage(launcher.getPackageName())
                        .putExtra(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY,
                                LauncherPrefs.WORKSPACE_LAYOUT_DOCK)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                snackBar.dismiss();
            });
            snackBar.show();
        }, SHOW_DELAY_MS);
    }

    private static void dismissExistingCouiSnackBars(View host) {
        ViewGroup parent = COUISnackBar.findSuitableParent(host);
        if (parent == null) {
            return;
        }
        for (int i = parent.getChildCount() - 1; i >= 0; i--) {
            View child = parent.getChildAt(i);
            if (child instanceof COUISnackBar) {
                ((COUISnackBar) child).dismiss(true);
            }
        }
    }
}
