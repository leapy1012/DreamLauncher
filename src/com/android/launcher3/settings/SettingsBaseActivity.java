/**
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.settings;

import android.annotation.LayoutRes;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.android.launcher3.R;
import com.coui.appcompat.toolbar.COUIToolbar;

import android.view.MenuItem;

/** Base activity for Settings pages */
public class SettingsBaseActivity extends AppCompatActivity {

    /**
     * What type of page transition should be apply.
     */
    public static final String EXTRA_PAGE_TRANSITION_TYPE = "page_transition_type";

    protected static final boolean DEBUG_TIMING = false;
    private static final String TAG = "SettingsBaseActivity";
    private static final int DEFAULT_REQUEST = -1;
    protected COUIToolbar mToolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) {
            return;
        }

        super.setContentView(R.layout.settings_base_activity);
        mToolbar = findViewById(R.id.action_bar);
        setSupportActionBar(mToolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            android.util.Log.d(TAG, "setActionBar");
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
        }
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        final ViewGroup parent = findViewById(R.id.content_frame);
        if (parent != null) {
            parent.removeAllViews();
        }
        LayoutInflater.from(this).inflate(layoutResID, parent);
    }

    @Override
    public void setContentView(View view) {
        ((ViewGroup) findViewById(R.id.content_frame)).addView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        ((ViewGroup) findViewById(R.id.content_frame)).addView(view, params);
    }

    /**
     * SubSetting page should show a toolbar by default. If the page wouldn't show a toolbar,
     * override this method and return false value.
     *
     * @return ture by default
     */
    protected boolean isToolbarEnabled() {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
