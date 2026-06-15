/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.launcher3.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.android.launcher3.R;


public class WallpaperChangeActivity extends Activity{
    private static final String TAG = "WallpaperChangeActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getAction().equals("android.intent.action.CREATE_SHORTCUT")) {
            Intent.ShortcutIconResource icon = Intent.ShortcutIconResource.fromContext(this, R.mipmap.choosewallpapaer_bg);
            Intent intent = new Intent();
            Intent launchIntent = new Intent(this, getClass());
            intent.putExtra("android.intent.extra.shortcut.INTENT", launchIntent);
            intent.putExtra("android.intent.extra.shortcut.NAME", getResources().getString(R.string.wallpaper_choose_widget_title));
            intent.putExtra("android.intent.extra.shortcut.ICON_RESOURCE", icon);
            setResult(-1, intent);
            finish();
        }
    }
}
