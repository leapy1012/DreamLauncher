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
package com.android.launcher3.util;

import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

/**
 * Helpers for showing/hiding the system status bar (Oppo ToggleBar-style edit mode).
 */
public final class SystemBarHelper {

    private SystemBarHelper() {}

    /** Hides the status bar. When {@code animate} is false, inset animation is briefly disabled. */
    public static void hideStatusBar(Window window, boolean animate) {
        if (window == null) {
            return;
        }
        WindowInsetsController controller = window.getInsetsController();
        if (controller == null) {
            return;
        }
        if (animate) {
            controller.setAnimationsDisabled(false);
            controller.hide(WindowInsets.Type.statusBars());
        } else {
            controller.setAnimationsDisabled(true);
            controller.hide(WindowInsets.Type.statusBars());
            controller.setAnimationsDisabled(false);
        }
    }

    /** Shows the status bar if it is currently requested hidden. */
    public static void showStatusBar(Window window) {
        if (window == null) {
            return;
        }
        WindowInsetsController controller = window.getInsetsController();
        if (controller == null) {
            return;
        }
        controller.setAnimationsDisabled(false);
        if ((controller.getRequestedVisibleTypes() & WindowInsets.Type.statusBars()) == 0) {
            controller.show(WindowInsets.Type.statusBars());
        }
    }
}
