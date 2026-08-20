package com.android.launcher3;

import android.view.View;
import java.util.WeakHashMap;

/** Owns ColorOS selection-indicator state independently from the edit panel view. */
public final class ColorOsWorkspaceSelectionController {
    private static final WeakHashMap<Launcher, Boolean> EDIT_MODE_ACTIVE = new WeakHashMap<>();
    private ColorOsWorkspaceSelectionController() { }

    public static synchronized boolean isEditModeActive(Launcher launcher) {
        return launcher != null && Boolean.TRUE.equals(EDIT_MODE_ACTIVE.get(launcher));
    }

    public static void setEditModeActive(Launcher launcher, boolean active, boolean animate) {
        if (launcher == null) return;
        synchronized (ColorOsWorkspaceSelectionController.class) {
            if (active) EDIT_MODE_ACTIVE.put(launcher, true);
            else EDIT_MODE_ACTIVE.remove(launcher);
        }
        launcher.getWorkspace().mapOverItems((info, view) -> {
            applyToView(view, active, animate);
            return false;
        });
        launcher.getWorkspace().invalidate();
    }

    public static void applyCurrentState(Launcher launcher, View view) {
        applyToView(view, isEditModeActive(launcher), false);
    }

    private static void applyToView(View view, boolean active, boolean animate) {
        if (view instanceof OplusBubbleTextView) {
            ((OplusBubbleTextView) view).setColorOsWorkspaceSelectionVisible(active, animate);
        }
    }
}
