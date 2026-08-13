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
import com.android.launcher3.R;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreference;
import com.coui.appcompat.preference.COUIPreferenceFragment;
import com.coui.appcompat.darkmode.COUIDarkModeUtil;
import androidx.recyclerview.widget.COUIRecyclerView;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
		int mode = Settings.System.getInt(getContext().getContentResolver(), "launcher_style", 0);
		mDrawer.setChecked(mode == 0 ? true : false);
		mNomal.setChecked(mode == 1 ? true : false);
		mCurrentMode = mode;
        requireActivity().setTitle(R.string.launcher_style_title);
    }
	
	@Override
    public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent,
            Bundle savedInstanceState) {
        COUIRecyclerView recyclerView = (COUIRecyclerView) inflater.inflate(
                com.coui.appcompat.R.layout.coui_preference_percent_recyclerview,
                parent, false);
        recyclerView.setEnablePointerDownAction(false);
        recyclerView.setLayoutManager(onCreateLayoutManager());
        COUIDarkModeUtil.setForceDarkAllow(recyclerView, false);
        return recyclerView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().getWindow().setBackgroundDrawableResource(
                com.coui.appcompat.R.drawable.coui_window_background_with_card_selector);
    }

	@Override
    public void onRadioButtonClicked(RadioButtonPreference preference) {
		if (preference.isChecked()) {
			return;
		}
		if (mDrawer == preference) {
			Settings.System.putInt(getContext().getContentResolver(), "launcher_style", 0);
			Settings.System.putInt(getContext().getContentResolver(), "launcher_style_change", 1);
			mCurrentMode = 0;
		} else if (mNomal == preference) {
			Settings.System.putInt(getContext().getContentResolver(), "launcher_style", 1);
			Settings.System.putInt(getContext().getContentResolver(), "launcher_style_change", 1);
			mCurrentMode = 1;
		}
		mDrawer.setChecked(mCurrentMode == 0 ? true : false);
		mNomal.setChecked(mCurrentMode == 1 ? true : false);
		getActivity().onBackPressed();
	}

}
