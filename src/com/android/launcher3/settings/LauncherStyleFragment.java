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
package com.android.launcher3.settings;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreference;
import androidx.recyclerview.widget.COUIRecyclerView;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.R;
import com.android.launcher3.LauncherStyle;
import com.coui.appcompat.darkmode.COUIDarkModeUtil;
import com.coui.appcompat.preference.COUIPreferenceFragment;

/**
 * Dev-build only UI allowing developers to toggle flag settings and plugins.
 * See {@link FeatureFlags}.
 */
@TargetApi(Build.VERSION_CODES.O)
public class LauncherStyleFragment extends COUIPreferenceFragment
		implements RadioButtonPreference.OnClickListener {

    private static final String KEY_STYLE_DRAWER = "launcher_style_drawer";
	private static final String KEY_STYLE_NOMAL = "launcher_style_nomal";
	private RadioButtonPreference mDrawer;
	private RadioButtonPreference mNomal;
	private int mCurrentMode;
	
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		setPreferencesFromResource(R.xml.launcher_style, rootKey);
		mDrawer = (RadioButtonPreference)findPreference(KEY_STYLE_DRAWER);
		mNomal = (RadioButtonPreference)findPreference(KEY_STYLE_NOMAL);
		mDrawer.setOnClickListener(this);
		mNomal.setOnClickListener(this);
		int mode = LauncherStyle.get(getContext());
		mDrawer.setChecked(mode == LauncherStyle.APP_DRAWER);
		mNomal.setChecked(mode == LauncherStyle.REGULAR);
        mCurrentMode = mode;
        CharSequence title = getString(R.string.launcher_style_title);
        requireActivity().setTitle(title);
        if (requireActivity() instanceof androidx.appcompat.app.AppCompatActivity) {
            androidx.appcompat.app.ActionBar bar =
                    ((androidx.appcompat.app.AppCompatActivity) requireActivity())
                            .getSupportActionBar();
            if (bar != null) {
                bar.setTitle(title);
            }
        }
    }
	
    @Override
    public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent,
            Bundle savedInstanceState) {
        COUIRecyclerView recyclerView = (COUIRecyclerView) inflater.inflate(
                com.coui.appcompat.R.layout.coui_preference_percent_recyclerview,
                parent, false);
        recyclerView.setEnablePointerDownAction(false);
        recyclerView.setOverScrollEnable(true);
        recyclerView.setClipToPadding(false);
        recyclerView.setNestedScrollingEnabled(true);
        recyclerView.setLayoutManager(onCreateLayoutManager());
        COUIDarkModeUtil.setForceDarkAllow(recyclerView, false);
        return recyclerView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() instanceof SettingsActivity) {
            ((SettingsActivity) getActivity()).bindToolbarScrollDivider(getListView());
        } else if (getActivity() instanceof SettingsBaseActivity) {
            ((SettingsBaseActivity) getActivity()).bindToolbarScrollDivider(getListView());
        }
        requireActivity().getWindow().setBackgroundDrawableResource(
                com.coui.appcompat.R.drawable.coui_window_background_with_card_selector);
        if (requireActivity() instanceof SettingsBaseActivity) {
            SettingsBaseActivity.applySettingsWindowColors(
                    (SettingsBaseActivity) requireActivity());
        } else if (requireActivity() instanceof SettingsActivity) {
            SettingsBaseActivity.applySettingsWindowColors(
                    (androidx.appcompat.app.AppCompatActivity) requireActivity());
        }
        RecyclerView listView = getListView();
        if (listView != null) {
            SettingsBaseActivity.applySettingsListTopGap(listView);
            SettingsBaseActivity.bindListNavigationInsets(listView);
            if (requireActivity() instanceof androidx.appcompat.app.AppCompatActivity) {
                SettingsBaseActivity.bindCouiDividerAppBar(
                        (androidx.appcompat.app.AppCompatActivity) requireActivity(), listView);
            }
        }
    }

	@Override
    public void onRadioButtonClicked(RadioButtonPreference preference) {
		if (preference.isChecked()) {
			return;
		}
		if (mDrawer == preference) {
			LauncherStyle.set(getContext(), LauncherStyle.APP_DRAWER);
			mCurrentMode = LauncherStyle.APP_DRAWER;
		} else if (mNomal == preference) {
			LauncherStyle.set(getContext(), LauncherStyle.REGULAR);
			mCurrentMode = LauncherStyle.REGULAR;
		}
		mDrawer.setChecked(mCurrentMode == LauncherStyle.APP_DRAWER);
		mNomal.setChecked(mCurrentMode == LauncherStyle.REGULAR);
		getActivity().onBackPressed();
	}

}
