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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.preference.PreferenceCategory;
import androidx.recyclerview.widget.COUIRecyclerView;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.LauncherStyle;
import com.android.launcher3.R;
import com.android.launcher3.allapps.coloros.ColorOsDrawerChrome;
import com.android.launcher3.allapps.coloros.ColorOsDrawerColumns;
import com.android.launcher3.allapps.coloros.ColorOsHomeSettings;
import com.coui.appcompat.button.COUIButton;
import com.coui.appcompat.darkmode.COUIDarkModeUtil;
import com.coui.appcompat.dialog.COUIAlertDialogBuilder;
import com.coui.appcompat.preference.COUIMenuPreference;
import com.coui.appcompat.preference.COUIPreferenceFragment;
import com.coui.appcompat.preference.COUISwitchPreference;

/**
 * Oppo Home screen page: Standard / With drawer picker plus drawer settings.
 */
@TargetApi(Build.VERSION_CODES.O)
public class LauncherStyleFragment extends COUIPreferenceFragment {

    private static final String KEY_MODE = "home_screen_mode";
    private static final String KEY_DRAWER_SETTINGS = "home_screen_drawer_settings";
    private static final String KEY_DRAWER_COLUMNS = "key_drawer_column_switch";
    private static final String STATE_SELECTED_STYLE = "selected_home_style";

    private HomeScreenModePreference mModePref;
    private PreferenceCategory mDrawerCategory;
    private COUISwitchPreference mAddAppsPref;
    private COUISwitchPreference mSuggestionsPref;
    private COUIMenuPreference mDefaultViewPref;
    private COUIMenuPreference mDrawerLayoutPref;
    private COUISwitchPreference mShowNamesPref;
    private COUIButton mApplyButton;

    private int mAppliedStyle;
    private int mSelectedStyle;
    private int mListBottomPad;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.launcher_style, rootKey);
        mAppliedStyle = LauncherStyle.get(getContext());
        mSelectedStyle = savedInstanceState != null
                ? savedInstanceState.getInt(STATE_SELECTED_STYLE, mAppliedStyle)
                : mAppliedStyle;

        mModePref = findPreference(KEY_MODE);
        mDrawerCategory = findPreference(KEY_DRAWER_SETTINGS);
        mAddAppsPref = findPreference(ColorOsHomeSettings.KEY_ADD_APP_TO_HOME);
        mSuggestionsPref = findPreference(ColorOsHomeSettings.KEY_SHOW_APP_SUGGESTIONS);
        mDefaultViewPref = findPreference(ColorOsHomeSettings.KEY_DEFAULT_VIEW);
        mDrawerLayoutPref = findPreference(KEY_DRAWER_COLUMNS);
        mShowNamesPref = findPreference(ColorOsHomeSettings.KEY_SHOW_APP_NAMES);

        if (mModePref != null) {
            mModePref.setSelectedStyle(mSelectedStyle);
            mModePref.setOnModeSelectedListener(style -> {
                mSelectedStyle = style;
                updateDrawerSettingsVisibility();
                updateApplyButton();
            });
        }
        bindDrawerSettings();
        updateDrawerSettingsVisibility();

        CharSequence title = getString(R.string.home_screen_style_title);
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_STYLE, mSelectedStyle);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View list = super.onCreateView(inflater, container, savedInstanceState);
        FrameLayout root = new FrameLayout(requireContext());
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(list, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        inflater.inflate(R.layout.home_screen_apply_bar, root, true);
        return root;
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
            mListBottomPad = listView.getPaddingBottom();
        }
        mApplyButton = view.findViewById(R.id.home_screen_apply);
        if (mApplyButton != null) {
            mApplyButton.setOnClickListener(v -> confirmAndApplyMode());
        }
        updateApplyButton();
    }

    private void bindDrawerSettings() {
        if (mAddAppsPref != null) {
            mAddAppsPref.setChecked(ColorOsHomeSettings.isAddNewAppsToHome(getContext()));
            mAddAppsPref.setOnPreferenceChangeListener((preference, newValue) -> {
                ColorOsHomeSettings.setAddNewAppsToHome(getContext(), (Boolean) newValue);
                return true;
            });
        }
        if (mSuggestionsPref != null) {
            mSuggestionsPref.setChecked(ColorOsHomeSettings.isShowAppSuggestions(getContext()));
            mSuggestionsPref.setOnPreferenceChangeListener((preference, newValue) -> {
                ColorOsHomeSettings.setShowAppSuggestions(getContext(), (Boolean) newValue);
                ColorOsDrawerChrome chrome = ColorOsHomeSettings.chrome();
                if (chrome != null) {
                    chrome.refreshAppSuggestions();
                }
                return true;
            });
        }
        if (mDefaultViewPref != null) {
            String value = ColorOsHomeSettings.getDefaultViewValue(getContext());
            mDefaultViewPref.setValue(value);
            mDefaultViewPref.setAssignment(defaultViewLabel(value));
            mDefaultViewPref.setOnPreferenceChangeListener((preference, newValue) -> {
                String next = String.valueOf(newValue);
                ColorOsHomeSettings.setDefaultViewFromValue(getContext(), next);
                mDefaultViewPref.setAssignment(defaultViewLabel(next));
                ColorOsDrawerChrome chrome = ColorOsHomeSettings.chrome();
                if (chrome != null) {
                    chrome.applyDefaultView();
                }
                return true;
            });
        }
        if (mDrawerLayoutPref != null) {
            int cols = ColorOsDrawerColumns.get(getContext());
            if (cols != ColorOsDrawerColumns.COLUMNS_FOUR
                    && cols != ColorOsDrawerColumns.COLUMNS_FIVE) {
                cols = ColorOsDrawerColumns.DEFAULT;
            }
            String value = String.valueOf(cols);
            mDrawerLayoutPref.setValue(value);
            mDrawerLayoutPref.setAssignment(drawerLayoutLabel(cols));
            mDrawerLayoutPref.setOnPreferenceChangeListener((preference, newValue) -> {
                int next = parseColumns(String.valueOf(newValue));
                ColorOsDrawerColumns.set(getContext(), next);
                mDrawerLayoutPref.setAssignment(drawerLayoutLabel(next));
                if (mModePref != null) {
                    mModePref.setDrawerColumns(next);
                }
                ColorOsDrawerChrome chrome = ColorOsHomeSettings.chrome();
                if (chrome != null) {
                    chrome.applyDrawerColumns();
                }
                return true;
            });
        }
        if (mShowNamesPref != null) {
            mShowNamesPref.setChecked(ColorOsHomeSettings.isShowDrawerAppNames(getContext()));
            mShowNamesPref.setOnPreferenceChangeListener((preference, newValue) -> {
                ColorOsHomeSettings.setShowDrawerAppNames(getContext(), (Boolean) newValue);
                ColorOsDrawerChrome chrome = ColorOsHomeSettings.chrome();
                if (chrome != null) {
                    chrome.refreshDrawerAppNames();
                }
                return true;
            });
        }
    }

    private int parseColumns(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return ColorOsDrawerColumns.DEFAULT;
        }
    }

    private CharSequence defaultViewLabel(String value) {
        return ColorOsHomeSettings.VALUE_DEFAULT_VIEW_CATEGORIES.equals(value)
                ? getString(R.string.coloros_floating_tab_category)
                : getString(R.string.coloros_floating_tab_all);
    }

    private CharSequence drawerLayoutLabel(int columns) {
        return getString(columns == ColorOsDrawerColumns.COLUMNS_FOUR
                ? R.string.coloros_drawer_columns_four
                : R.string.coloros_drawer_columns_five);
    }

    private void updateDrawerSettingsVisibility() {
        if (mDrawerCategory != null) {
            mDrawerCategory.setVisible(mSelectedStyle == LauncherStyle.APP_DRAWER);
        }
    }

    private void updateApplyButton() {
        boolean pending = mSelectedStyle != mAppliedStyle;
        if (mApplyButton != null) {
            mApplyButton.setVisibility(pending ? View.VISIBLE : View.GONE);
        }
        RecyclerView listView = getListView();
        if (listView != null) {
            int extra = pending
                    ? getResources().getDimensionPixelSize(R.dimen.coloros_home_screen_apply_space)
                    : 0;
            listView.setPadding(listView.getPaddingLeft(), listView.getPaddingTop(),
                    listView.getPaddingRight(), mListBottomPad + extra);
        }
    }

    private void confirmAndApplyMode() {
        if (mSelectedStyle == mAppliedStyle) {
            return;
        }
        // OPPO LauncherModelFragment.showAlertDialog: long copy is the title (centered),
        // positive button is "Switch" — not setMessage (left-aligned when multi-line) + Apply.
        new COUIAlertDialogBuilder(requireContext())
                .setTitle(R.string.coloros_switch_home_screen_mode_message)
                .setPositiveButton(R.string.layout_apply_change_positive, (dialog, which) -> {
                    LauncherStyle.set(requireContext(), mSelectedStyle);
                    mAppliedStyle = mSelectedStyle;
                    requireActivity().onBackPressed();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
