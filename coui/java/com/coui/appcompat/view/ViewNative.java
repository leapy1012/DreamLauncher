package com.coui.appcompat.view;

import android.util.Log;
import android.view.View;

import com.coui.appcompat.version.COUICompatUtil;

import java.lang.reflect.Field;

/**
 * Writes {@code View.mScrollX}/{@code mScrollY} for COUI spring overscroll.
 *
 * <p>OPPO ColorOS ships {@code ViewWrapper.setScrollYForColor}. On AOSP/MTK that
 * class is absent, so we fall back to a direct field write — the same path OPPO's
 * decoded ViewNative uses. This requires platform signing (or equivalent hidden-API
 * access); without it {@code getDeclaredField("mScrollY")} is blocked.
 *
 * <p>Do not call {@link View#scrollTo(int, int)} here:
 * {@link androidx.recyclerview.widget.RecyclerView} overrides it to a no-op.
 */
public class ViewNative {
    private static final String TAG = "ViewNative";
    private static final String VIEW_WRAPPER_PATH_NEW = "com.oplus.inner.view.ViewWrapper";

    private static Field sScrollXField;
    private static Field sScrollYField;
    private static boolean sFieldsResolved;
    private static boolean sLoggedScrollYFailure;

    private static boolean canReachFrameworkWrapper() {
        try {
            Class.forName(VIEW_WRAPPER_PATH_NEW);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void ensureScrollFields() {
        if (sFieldsResolved) {
            return;
        }
        sFieldsResolved = true;
        try {
            sScrollXField = View.class.getDeclaredField("mScrollX");
            sScrollXField.setAccessible(true);
            sScrollYField = View.class.getDeclaredField("mScrollY");
            sScrollYField.setAccessible(true);
        } catch (Throwable t) {
            Log.w(TAG, "Unable to resolve View mScrollX/mScrollY: " + t);
            sScrollXField = null;
            sScrollYField = null;
        }
    }

    private static boolean setScrollField(Field field, View view, int value) {
        if (field == null) {
            return false;
        }
        try {
            field.setInt(view, value);
            // Field write skips View.setScrollY side effects; force a redraw.
            view.invalidate();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void setScrollX(View view, int scrollX) {
        if (tryWrapperScrollX(view, scrollX)) {
            return;
        }
        ensureScrollFields();
        if (setScrollField(sScrollXField, view, scrollX)) {
            return;
        }
        Log.w(TAG, "setScrollX failed; COUI overscroll will not move horizontally");
    }

    public static void setScrollY(View view, int scrollY) {
        if (tryWrapperScrollY(view, scrollY)) {
            return;
        }
        ensureScrollFields();
        if (setScrollField(sScrollYField, view, scrollY)) {
            return;
        }
        if (!sLoggedScrollYFailure) {
            sLoggedScrollYFailure = true;
            Log.w(TAG, "setScrollY failed; COUIRecyclerView spring overscroll cannot run");
        }
    }

    private static boolean tryWrapperScrollX(View view, int scrollX) {
        String viewNativeName = canReachFrameworkWrapper()
                ? VIEW_WRAPPER_PATH_NEW
                : COUICompatUtil.getInstance().getViewNativeName();
        try {
            Class.forName(viewNativeName)
                    .getDeclaredMethod("setScrollXForColor", View.class, Integer.TYPE)
                    .invoke(null, view, scrollX);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean tryWrapperScrollY(View view, int scrollY) {
        String viewNativeName = canReachFrameworkWrapper()
                ? VIEW_WRAPPER_PATH_NEW
                : COUICompatUtil.getInstance().getViewNativeName();
        try {
            Class.forName(viewNativeName)
                    .getDeclaredMethod("setScrollYForColor", View.class, Integer.TYPE)
                    .invoke(null, view, scrollY);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
