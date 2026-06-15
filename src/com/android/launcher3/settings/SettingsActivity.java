/*
 * Copyright (C) 2015 The Android Open Source Project
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

import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS;

import static com.android.launcher3.config.FeatureFlags.IS_STUDIO_BUILD;
import static com.android.launcher3.states.RotationHelper.ALLOW_ROTATION_PREFERENCE_KEY;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceFragmentCompat.OnPreferenceStartFragmentCallback;
import androidx.preference.PreferenceFragmentCompat.OnPreferenceStartScreenCallback;
import androidx.preference.PreferenceGroup.PreferencePositionCallback;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.LauncherFiles;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.model.WidgetsModel;
import com.android.launcher3.states.RotationHelper;
import com.android.launcher3.uioverrides.flags.DeveloperOptionsFragment;
import com.android.launcher3.uioverrides.plugins.PluginManagerWrapper;
import com.android.launcher3.util.DisplayController;

import java.util.Collections;
import java.util.List;
//hxy-feature: add launcher style function  202312
import android.provider.Settings;
//hxy-feature: add launcher style function  202312
//hxy-feature: desktop theme 202312
import android.content.ContentResolver;
import android.content.Intent;
import androidx.preference.SwitchPreference;
import android.content.res.Resources;
import com.android.launcher3.util.Themes;
import android.os.SystemProperties;
//hxy-feature: desktop theme 202312
import static com.android.launcher3.LauncherPrefs.WORKSPACE_APP_NAME;
import static com.android.launcher3.LauncherPrefs.WORKSPACE_DOCKED_APP;
import static com.android.launcher3.LauncherPrefs.WORKSPACE_LAYOUT_DOCK;
import static com.android.launcher3.LauncherPrefs.WORKSPACE_ICON_SIZE;
import static com.android.launcher3.LauncherPrefs.WORKSPACE_FILL_CELL;
import static com.android.launcher3.LauncherPrefs.WORKSPACE_DOUBLE_TAP;
import static com.android.launcher3.LauncherPrefs.WORKSPACE_TEXT_SIZE;
import static com.android.launcher3.LauncherPrefs.WORKSPACE_INSTALL_BEHAVIOR;
import static com.android.launcher3.LauncherPrefs.WORKSPACE_MEMORY_CLEAN;
import static com.android.launcher3.LauncherPrefs.WORKSPACE_WALLPAPER_SET;
import static com.android.launcher3.LauncherPrefs.WORKSPACE_MINUS;
import static com.android.launcher3.LauncherPrefs.WORKSPACE_PLUS;
import com.android.launcher3.settings.RoundCornerPreferenceAdapter;
import java.util.Objects;
import java.util.Optional;
import android.os.Process;
import android.widget.Toast;
import android.content.ComponentName;
import com.android.launcher3.BuildConfig;
import androidx.preference.PreferenceCategory;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherApplication;
import com.android.launcher3.model.data.ItemInfo;
import android.app.ActionBar;
import android.widget.Toolbar;
import android.util.TypedValue;

/**
 * Settings activity for Launcher. Currently implements the following setting: Allow rotation
 */
public class SettingsActivity extends FragmentActivity
        implements OnPreferenceStartFragmentCallback, OnPreferenceStartScreenCallback,
        SharedPreferences.OnSharedPreferenceChangeListener{

    /** List of fragments that can be hosted by this activity. */
    private static final List<String> VALID_PREFERENCE_FRAGMENTS =
            !Utilities.IS_DEBUG_DEVICE ? Collections.emptyList()
                    : Collections.singletonList(DeveloperOptionsFragment.class.getName());

    private static final String DEVELOPER_OPTIONS_KEY = "pref_developer_options";
    private static final String FLAGS_PREFERENCE_KEY = "flag_toggler";

    private static final String NOTIFICATION_DOTS_PREFERENCE_KEY = "pref_icon_badging";

    public static final String EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key";
    public static final String EXTRA_SHOW_FRAGMENT_ARGS = ":settings:show_fragment_args";
    private static final int DELAY_HIGHLIGHT_DURATION_MILLIS = 600;
    public static final String SAVE_HIGHLIGHTED_KEY = "android:preference_highlighted";
    //hxy-feature: desktop theme 202312
    private static final boolean SHOW_THEME_ICON = SystemProperties.getInt("persist.sys.hxy_theme_icon", 0) == 1;//added by zhushuangqian for theme
    //hxy-feature: desktop theme 202312
    @VisibleForTesting
    static final String EXTRA_FRAGMENT = ":settings:fragment";
    @VisibleForTesting
    static final String EXTRA_FRAGMENT_ARGS = ":settings:fragment_args";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        setActionBar(findViewById(R.id.action_bar));

        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_FRAGMENT) || intent.hasExtra(EXTRA_FRAGMENT_ARGS)
                || intent.hasExtra(EXTRA_FRAGMENT_ARG_KEY)) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            Bundle args = intent.getBundleExtra(EXTRA_FRAGMENT_ARGS);
            if (args == null) {
                args = new Bundle();
            }

            String prefKey = intent.getStringExtra(EXTRA_FRAGMENT_ARG_KEY);
            if (!TextUtils.isEmpty(prefKey)) {
                args.putString(EXTRA_FRAGMENT_ARG_KEY, prefKey);
            }

            final FragmentManager fm = getSupportFragmentManager();
            final Fragment f = fm.getFragmentFactory().instantiate(getClassLoader(),
                    getPreferenceFragment());
            f.setArguments(args);
            // Display the fragment as the main content.
            fm.beginTransaction().replace(R.id.content_frame, f).commit();
        }

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            android.util.Log.d("liu-db", "setActionBar");
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.hxy_ic_back);
        }
        Toolbar toolbar = findViewById(R.id.action_bar);
        if (toolbar != null) {
            android.util.Log.d("liu-db", "setToolbar");
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.colorPrimaryDark, typedValue, true);
            toolbar.setBackgroundColor(typedValue.data);
        }
    }

    /**
     * Obtains the preference fragment to instantiate in this activity.
     *
     * @return the preference fragment class
     * @throws IllegalArgumentException if the fragment is unknown to this activity
     */
    private String getPreferenceFragment() {
        String preferenceFragment = getIntent().getStringExtra(EXTRA_FRAGMENT);
        String defaultFragment = getString(R.string.settings_fragment_name);

        if (TextUtils.isEmpty(preferenceFragment)) {
            return defaultFragment;
        } else if (!preferenceFragment.equals(defaultFragment)
                && !VALID_PREFERENCE_FRAGMENTS.contains(preferenceFragment)
                && !android.os.SystemProperties.getBoolean("ro.launcher.style",false)) {//hxy-feature: add launcher style function  202312
            throw new IllegalArgumentException(
                    "Invalid fragment for this activity: " + preferenceFragment);
        } else {
            return preferenceFragment;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        android.util.Log.e("zsq","onSharedPreferenceChanged: key = " + key);
        if (LauncherPrefs.WORKSPACE_DOCKED_APP.equals(key) || LauncherPrefs.WORKSPACE_APP_NAME.equals(key)) {
            LauncherAppState.getInstance(LauncherApplication.getContext()).getInvariantDeviceProfile().onConfigChanged(LauncherApplication.getContext());
        } else if (LauncherPrefs.WORKSPACE_DOUBLE_TAP.equals(key)) {
            Settings.Global.putInt(getContentResolver(), "persist.sys.double_tap_to_off", sharedPreferences.getBoolean("pref_double_tap", false) ? 1 : 0);
        } else if (LauncherPrefs.WORKSPACE_LAYOUT_DOCK.equals(key)) {
            Settings.Global.putInt(getContentResolver(), "persist.sys.desktop_layout_docked", sharedPreferences.getBoolean("pref_layout_dock", false) ? 1 : 0);
        } else if (LauncherPrefs.WORKSPACE_MINUS.equals(key)) {
            Settings.Global.putInt(getContentResolver(), "persist.sys.desktop_minus", LauncherPrefs.getPrefs(this).getBoolean("pref_minusEnabled", true) ? 1 : 0);
        } else if (LauncherPrefs.WORKSPACE_PLUS.equals(key)) {
            Settings.Global.putInt(getContentResolver(), "persist.sys.desktop_plus", LauncherPrefs.getPrefs(this).getBoolean("pref_plusEnabled", true) ? 1 : 0);
        } else {
            if (LauncherPrefs.WORKSPACE_MEMORY_CLEAN.equals(key) && !sharedPreferences.getBoolean(key, false) && LauncherApplication.getLauncher() != null) {
                ComponentName componentName = new ComponentName(BuildConfig.APPLICATION_ID, "com.android.launcher3.big.memoryclean.MemoryCleanActivity");
                Optional<ItemInfo> foundItem = LauncherApplication.getLauncher().getModel().mBgDataModel.workspaceItems.stream()
                    .filter(itemInfo -> itemInfo.user.equals(Process.myUserHandle())
                            && itemInfo.getTargetComponent() != null
                            && itemInfo.getTargetComponent().equals(componentName))
                    .findAny();
                foundItem.ifPresent(itemInfo -> LauncherApplication.getLauncher().getModelWriter().deleteItemFromDatabase(itemInfo, ""));
            } else if (LauncherPrefs.WORKSPACE_WALLPAPER_SET.equals(key) && !sharedPreferences.getBoolean(key, false) && LauncherApplication.getLauncher() != null) {
                ComponentName componentName = new ComponentName(BuildConfig.APPLICATION_ID, "com.android.launcher3.settings.WallpaperChangeActivity");
                Optional<ItemInfo> foundItem = LauncherApplication.getLauncher().getModel().mBgDataModel.workspaceItems.stream()
                    .filter(itemInfo -> itemInfo.user.equals(Process.myUserHandle())
                            && itemInfo.getTargetComponent() != null
                            && itemInfo.getTargetComponent().equals(componentName))
                    .findAny();
                foundItem.ifPresent(itemInfo -> LauncherApplication.getLauncher().getModelWriter().deleteItemFromDatabase(itemInfo, ""));
            }
            LauncherAppState.getInstance(this).getModel().forceReload();
        }
    }

    private boolean startPreference(String fragment, Bundle args, String key) {
        if (Utilities.ATLEAST_P && getSupportFragmentManager().isStateSaved()) {
            // Sometimes onClick can come after onPause because of being posted on the handler.
            // Skip starting new preferences in that case.
            return false;
        }
        final FragmentManager fm = getSupportFragmentManager();
        final Fragment f = fm.getFragmentFactory().instantiate(getClassLoader(), fragment);
        if (f instanceof DialogFragment) {
            f.setArguments(args);
            ((DialogFragment) f).show(fm, key);
        } else {
            startActivity(new Intent(this, SettingsActivity.class)
                    .putExtra(EXTRA_FRAGMENT, fragment)
                    .putExtra(EXTRA_FRAGMENT_ARGS, args));
        }
        return true;
    }

    @Override
    public boolean onPreferenceStartFragment(
            PreferenceFragmentCompat preferenceFragment, Preference pref) {
        return startPreference(pref.getFragment(), pref.getExtras(), pref.getKey());
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat caller, PreferenceScreen pref) {
        Bundle args = new Bundle();
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, pref.getKey());
        return startPreference(getString(R.string.settings_fragment_name), args, pref.getKey());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LauncherPrefs.getPrefs(getApplicationContext())
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LauncherPrefs.getPrefs(getApplicationContext())
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * This fragment shows the launcher preferences.
     */
    public static class LauncherSettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener {

        private String mHighLightKey;
        private boolean mPreferenceHighlighted = false;
        private Preference mDeveloperOptionPref;

		//hxy-feature: add launcher style function  202312
		private static final String LAUNCHER_STYLE_PREFERENCE_KEY = "pref_launcher_style";
		private Preference mLauncherStylePref;
		//hxy-feature: add launcher style function  202312
        //hxy-feature: desktop theme 202312
        private static final String LAUNCHER_THEME_PREFERENCE_KEY = "pref_theme_style";
        private Preference mLauncherThemePref;
        //hxy-feature: desktop theme 202312
        private SwitchPreference mPlusPref;
        private SwitchPreference mMinusPref;
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            final Bundle args = getArguments();
            mHighLightKey = args == null ? null : args.getString(EXTRA_FRAGMENT_ARG_KEY);
            if (rootKey == null && !TextUtils.isEmpty(mHighLightKey)) {
                rootKey = getParentKeyForPref(mHighLightKey);
            }

            if (savedInstanceState != null) {
                mPreferenceHighlighted = savedInstanceState.getBoolean(SAVE_HIGHLIGHTED_KEY);
            }

            getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
            setPreferencesFromResource(R.xml.launcher_preferences, rootKey);

			//hxy-feature: add launcher style function  202312
			int style = Settings.System.getInt(getActivity().getContentResolver(), "launcher_style", 0);
			mLauncherStylePref = (Preference) findPreference(LAUNCHER_STYLE_PREFERENCE_KEY);
			if (mLauncherStylePref != null) {
				mLauncherStylePref.setSummary(style == 0 ? 
					getString(R.string.home_screen_style_single) : getString(R.string.home_screen_style_regular));
                mLauncherStylePref.setOnPreferenceClickListener(this);
			}
			//hxy-feature: add launcher style function  202312
            //hxy-feature: desktop theme 202312
            mLauncherThemePref = (Preference) findPreference(LAUNCHER_THEME_PREFERENCE_KEY);
            //hxy-feature: desktop theme 202312
            Preference pref = findPreference("pref_plusEnabled");
            if (pref instanceof SwitchPreference) {
                mPlusPref = (SwitchPreference) pref;
            }
            pref = findPreference("pref_minusEnabled");
            if (pref instanceof SwitchPreference) {
                mMinusPref = (SwitchPreference) pref;
            }
            PreferenceScreen screen = getPreferenceScreen();
            for (int i = screen.getPreferenceCount() - 1; i >= 0; i--) {
                Preference preference = screen.getPreference(i);
                traversePreferenceTree(preference);
            }

            if (getActivity() != null && !TextUtils.isEmpty(getPreferenceScreen().getTitle())) {
                if (getPreferenceScreen().getTitle().equals(
                        getResources().getString(R.string.search_pref_screen_title))){
                    DeviceProfile mDeviceProfile = InvariantDeviceProfile.INSTANCE.get(
                            getContext()).getDeviceProfile(getContext());
                    getPreferenceScreen().setTitle(mDeviceProfile.isMultiDisplay
                            || mDeviceProfile.isPhone ?
                            R.string.search_pref_screen_title :
                            R.string.search_pref_screen_title_tablet);
                }
                getActivity().setTitle(getPreferenceScreen().getTitle());
            }
        }

        private void traversePreferenceTree(Preference preference) {
            if (preference instanceof PreferenceCategory preferenceCategory) {
                for (int i = preferenceCategory.getPreferenceCount() - 1; i >= 0; i--) {
                    traversePreferenceTree(preferenceCategory.getPreference(i));
                }
            } else if (initPreference(preference)) {
                if (IS_STUDIO_BUILD && preference == mDeveloperOptionPref) {
                    preference.setOrder(0);
                }
            } else {
                Objects.requireNonNull(preference.getParent()).removePreference(preference);
            }
        }

        //hxy-feature: desktop theme 202312
        private void updateThemePref(){
            android.util.Log.e("zsq","SHOW_THEME_ICON = " + SHOW_THEME_ICON);
            if(!SHOW_THEME_ICON){
                return ;
            }
            String summary = "";
            android.util.Log.e("zsq","mLauncherThemePref = null " +(mLauncherThemePref == null));
            if (mLauncherThemePref != null) {
                String themeName = Settings.Global.getString(getActivity().getContentResolver(), Themes.KEY_THEMED);
                if(TextUtils.isEmpty(themeName) || "none".equals(themeName)){
                    summary = getString(R.string.none);
                }else{
                    try {
                        String packageName = themeName;
                        Resources resall = getActivity().getPackageManager().getResourcesForApplication(packageName);
                        summary = resall.getString(resall.getIdentifier("theme_title", "string", packageName));
                    }catch (Exception e) {
                        summary = "";
                    }
                }
				mLauncherThemePref.setSummary(summary);
			}
        }
        //hxy-feature: desktop theme 202312

        @Override
        public boolean onPreferenceClick(Preference preference) {
            android.util.Log.e("zsq","onPreferenceClick: key = " + preference.getKey());
            if ("pref_launcher_style".equals(preference.getKey())) {
                if (!LauncherPrefs.getPrefs(getContext()).getBoolean(LauncherPrefs.WORKSPACE_LAYOUT_DOCK, false)) {
                    return false;
                } else {
                    Toast.makeText(getContext(), R.string.home_screen_layout_lock_tips, Toast.LENGTH_SHORT).show();
                    return true;
                }
            }
            return false;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            View listView = getListView();
            final int bottomPadding = listView.getPaddingBottom();
            listView.setOnApplyWindowInsetsListener((v, insets) -> {
                v.setPadding(
                        v.getPaddingLeft(),
                        v.getPaddingTop(),
                        v.getPaddingRight(),
                        bottomPadding + insets.getSystemWindowInsetBottom());
                return insets.consumeSystemWindowInsets();
            });
            // Overriding Text Direction in the Androidx preference library to support RTL
            view.setTextDirection(View.TEXT_DIRECTION_LOCALE);
        }

        @NonNull
        @Override
        protected RecyclerView.Adapter onCreateAdapter(@NonNull PreferenceScreen preferenceScreen) {
            return new RoundCornerPreferenceAdapter(preferenceScreen);
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putBoolean(SAVE_HIGHLIGHTED_KEY, mPreferenceHighlighted);
        }

        protected String getParentKeyForPref(String key) {
            return null;
        }

        /**
         * Initializes a preference. This is called for every preference. Returning false here
         * will remove that preference from the list.
         */
        protected boolean initPreference(Preference preference) {
            boolean isTable = android.os.SystemProperties.get("ro.build.characteristics").equals("tablet");

            switch (preference.getKey()) {
                case NOTIFICATION_DOTS_PREFERENCE_KEY:
                    return !WidgetsModel.GO_DISABLE_NOTIFICATION_DOTS;

                case ALLOW_ROTATION_PREFERENCE_KEY:
                    DisplayController.Info info =
                            DisplayController.INSTANCE.get(getContext()).getInfo();
                    if (info.isTablet(info.realBounds)) {
                        // Launcher supports rotation by default. No need to show this setting.
                        return false;
                    }
                    // Initialize the UI once
                    // preference.setDefaultValue(RotationHelper.getAllowRotationDefaultValue(info));
                    if(isTable) return true;
                    return false;//true; modify for hide rotaton item at 20231206

                case FLAGS_PREFERENCE_KEY:
                    // Only show flag toggler UI if this build variant implements that.
                    return FeatureFlags.showFlagTogglerUi(getContext());

                case DEVELOPER_OPTIONS_KEY:
                    mDeveloperOptionPref = preference;
                    return updateDeveloperOption();
				//hxy-feature: add launcher style function  202312
				case LAUNCHER_STYLE_PREFERENCE_KEY:
					if (android.os.SystemProperties.getBoolean("ro.launcher.style",false)) {
						return true;
					}
					return false;
				//hxy-feature: add launcher style function  202312
                //hxy-feature: desktop theme 202312
                case LAUNCHER_THEME_PREFERENCE_KEY:
                     if (SHOW_THEME_ICON) {
                         return true;
                     }
                    return false;
                //hxy-feature: desktop theme 202312
				/// &&}}
                case WORKSPACE_WALLPAPER_SET:
                    return false;
                case WORKSPACE_MEMORY_CLEAN:
                    return false;
                case WORKSPACE_MINUS:
                    return true;
                case WORKSPACE_PLUS:
                    return true;
            }

            return true;
        }

        /**
         * Show if plugins are enabled or flag UI is enabled.
         * @return True if we should show the preference option.
         */
        private boolean updateDeveloperOption() {
            boolean showPreference = FeatureFlags.showFlagTogglerUi(getContext())
                    || PluginManagerWrapper.hasPlugins(getContext());
            if (mDeveloperOptionPref != null) {
                mDeveloperOptionPref.setEnabled(showPreference);
                if (showPreference) {
                    getPreferenceScreen().addPreference(mDeveloperOptionPref);
                } else {
                    getPreferenceScreen().removePreference(mDeveloperOptionPref);
                }
            }
            return showPreference;
        }

        @Override
        public void onResume() {
            super.onResume();

            updateDeveloperOption();

            if (isAdded() && !mPreferenceHighlighted) {
                PreferenceHighlighter highlighter = createHighlighter();
                if (highlighter != null) {
                    getView().postDelayed(highlighter, DELAY_HIGHLIGHT_DURATION_MILLIS);
                    mPreferenceHighlighted = true;
                } else {
                    requestAccessibilityFocus(getListView());
                }
            }
			//hxy-feature: add launcher style function  202312
            int style = Settings.System.getInt(getActivity().getContentResolver(), "launcher_style", 0);
			if (mLauncherStylePref != null) {
				mLauncherStylePref.setSummary(style == 0 ? 
					getString(R.string.home_screen_style_single) : getString(R.string.home_screen_style_regular));
			}
			//hxy-feature: add launcher style function  202312
            updateThemePref();  //hxy-feature: desktop theme 202312
            if (mPlusPref != null) {
                android.util.Log.e("zsq","SHOW_PLUS = " + Settings.Global.getInt(getActivity().getContentResolver(), "persist.sys.desktop_plus", 1));
                mPlusPref.setChecked(Settings.Global.getInt(getActivity().getContentResolver(), "persist.sys.desktop_plus", 1) == 1);
            }
            if (mMinusPref != null) {
                android.util.Log.e("zsq","SHOW_MINUS = " + Settings.Global.getInt(getActivity().getContentResolver(), "persist.sys.desktop_minus", 1));
                mMinusPref.setChecked(Settings.Global.getInt(getActivity().getContentResolver(), "persist.sys.desktop_minus", 1) == 1);
            }
        }

        private PreferenceHighlighter createHighlighter() {
            if (TextUtils.isEmpty(mHighLightKey)) {
                return null;
            }

            PreferenceScreen screen = getPreferenceScreen();
            if (screen == null) {
                return null;
            }

            RecyclerView list = getListView();
            PreferencePositionCallback callback = (PreferencePositionCallback) list.getAdapter();
            int position = callback.getPreferenceAdapterPosition(mHighLightKey);
            return position >= 0 ? new PreferenceHighlighter(
                    list, position, screen.findPreference(mHighLightKey))
                    : null;
        }

        private void requestAccessibilityFocus(@NonNull final RecyclerView rv) {
            rv.post(() -> {
                if (!rv.hasFocus() && rv.getChildCount() > 0) {
                    rv.getChildAt(0)
                            .performAccessibilityAction(ACTION_ACCESSIBILITY_FOCUS, null);
                }
            });
        }
    }
}
