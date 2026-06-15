/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.launcher3;

import android.app.Application;
import android.content.Context;

/**
 * Main application class for Launcher
 */
public class LauncherApplication extends Application {
    // 静态成员变量，用于存储全局 Context
    private static Context sContext;
    // 静态成员变量，用于存储全局 Launcher 对象
    private static Launcher sLauncher;
    @Override
    public void onCreate() {
        super.onCreate();
        // 将当前应用的 Context 赋值给静态变量
        sContext = this;
        MainProcessInitializer.initialize(this);
    }

    // 提供一个公共的静态方法，用于获取全局 Context
    public static Context getContext() {
        return sContext;
    }

    // 提供一个公共的静态方法，用于获取全局 Launcher 对象
    public static Launcher getLauncher() {
        return sLauncher;
    }
    // 提供一个公共的静态方法，用于设置全局 Launcher 对象
    public static void setLauncher(Launcher launcher) {
        sLauncher = launcher;
    }    
}
