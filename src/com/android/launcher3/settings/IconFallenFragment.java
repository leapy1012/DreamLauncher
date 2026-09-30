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
package com.android.launcher3.settings;

import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.LauncherFiles;
import com.android.launcher3.R;
import com.coui.appcompat.preference.COUIPreferenceFragment;
import com.coui.appcompat.preference.COUISwitchPreference;

/**
 * Oppo {@code LauncherIconFallenFragment}: Icon pull-down gesture detail page
 * (illustration tip card + enable switch).
 */
public class IconFallenFragment extends COUIPreferenceFragment {

    private static final String KEY_INTRO = "key_launcher_icon_fallen_intro";

    private IconFallenIntroPreference mIntroPref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
        setPreferencesFromResource(R.xml.launcher_icon_fallen_settings, rootKey);

        CharSequence title = getString(R.string.launcher_icon_fallen_title);
        requireActivity().setTitle(title);
        if (requireActivity() instanceof AppCompatActivity) {
            ActionBar bar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
            if (bar != null) {
                bar.setTitle(title);
            }
        }

        DeviceProfile dp = InvariantDeviceProfile.INSTANCE.get(requireContext())
                .getDeviceProfile(requireContext());
        if (dp.isTablet) {
            requireActivity().finish();
            return;
        }

        mIntroPref = findPreference(KEY_INTRO);
        COUISwitchPreference switchPref = findPreference(HomeScreenGestures.PREF_ICON_FALLEN);
        if (switchPref != null) {
            switchPref.setChecked(HomeScreenGestures.isIconFallenEnabled(requireContext()));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        COUISwitchPreference switchPref = findPreference(HomeScreenGestures.PREF_ICON_FALLEN);
        if (switchPref != null) {
            switchPref.setChecked(HomeScreenGestures.isIconFallenEnabled(requireContext()));
        }
    }

    @Override
    public void onDestroy() {
        if (mIntroPref != null) {
            mIntroPref.destroy();
            mIntroPref = null;
        }
        super.onDestroy();
    }
}
