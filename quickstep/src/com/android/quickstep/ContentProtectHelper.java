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

package com.android.quickstep;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.systemui.shared.recents.model.Task;

/**
 * Local prefs for Recents "Hide content" / "Show content" (ColorOS-style thumbnail privacy),
 * without Oppo SafeCenter / payment-protect integration.
 */
public final class ContentProtectHelper {

    private static final String PREFS = "recents_content_protect";

    private ContentProtectHelper() {}

    @NonNull
    public static String keyFor(@NonNull Task.TaskKey taskKey) {
        String pkg = taskKey.getPackageName();
        if (pkg == null) {
            pkg = "";
        }
        return taskKey.userId + "|" + pkg;
    }

    public static boolean isProtected(@NonNull Context context, @Nullable Task.TaskKey taskKey) {
        if (taskKey == null) {
            return false;
        }
        return prefs(context).getBoolean(keyFor(taskKey), false);
    }

    public static void setProtected(@NonNull Context context, @Nullable Task.TaskKey taskKey,
            boolean protectedContent) {
        if (taskKey == null) {
            return;
        }
        prefs(context).edit().putBoolean(keyFor(taskKey), protectedContent).apply();
    }

    /** Apply stored protect flag onto the in-memory task. */
    public static void applyToTask(@NonNull Context context, @Nullable Task task) {
        if (task == null || task.key == null) {
            return;
        }
        task.isContentProtect = isProtected(context, task.key);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
