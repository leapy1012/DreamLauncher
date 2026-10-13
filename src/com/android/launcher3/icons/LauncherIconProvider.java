/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.launcher3.icons;

import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import com.android.launcher3.R;
import com.android.launcher3.config.DreamFeatureOption;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.util.Themes;

import org.xmlpull.v1.XmlPullParser;

import java.util.Collections;
import java.util.Map;
    //hxy-feature: desktop theme 202312
import android.content.pm.PackageManager.NameNotFoundException;

import android.provider.Settings;
import android.net.Uri;
import android.database.ContentObserver;
import com.android.launcher3.LauncherAppState;
    //hxy-feature: desktop theme 202312

/**
 * Extension of {@link IconProvider} with support for overriding theme icons
 */
public class LauncherIconProvider extends IconProvider {

    private static final String TAG_ICON = "icon";
    private static final String ATTR_PACKAGE = "package";
    private static final String ATTR_CLASS = "className";
    private static final String ATTR_DRAWABLE = "drawable";
    private static final String CONTACTS_PACKAGE = "com.android.contacts";

    private static final String TAG = "LIconProvider";
    private static final Map<String, ThemeData> DISABLED_MAP = Collections.emptyMap();

    private Map<String, ThemeData> mThemedIconMap;
    private Map<String, ThemeData> mDataThemeIconMap;
    private boolean mSupportsIconTheme;
    //hxy-feature: desktop theme 202312
    private String mThemeName = "";
    private String mDataThemeState = "";
    private Context mContetxt;
    private String packageName = "";
    private static final boolean SHOW_THEME_ICON = DreamFeatureOption.isSupportThemeIcon;
    //hxy-feature: desktop theme 202312
    public LauncherIconProvider(Context context) {
        super(context);
        this.mContetxt = context;//hxy-feature: desktop theme 202312
        setIconThemeSupported(Themes.isThemedIconEnabled(context));
        //hxy-feature: desktop theme 202312
        updateTheme();
        reloadDataThemeIcons();
        //hxy-feature: desktop theme 202312
    }
    //hxy-feature: desktop theme 202312
    public void updateTheme(){
        if(SHOW_THEME_ICON){
            String themeName = Themes.getThemedName(mContetxt);
            if(!TextUtils.equals(mThemeName, themeName)){
                mThemedIconMap = null;
                mThemeName = themeName;
                packageName = themeName;
            }
        }
    }

    /** Reload icons from /data/theme/icons (ThemeStore → UXDesign apply). */
    public void reloadDataThemeIcons() {
        reloadDataThemeIcons(false);
    }

    /**
     * @param force when true, always rescan disk (THEME_APPLIED / settings change).
     */
    public void reloadDataThemeIcons(boolean force) {
        String state = DataThemeIconLoader.themeStateToken();
        if (!force
                && TextUtils.equals(mDataThemeState, state)
                && mDataThemeIconMap != null
                && (!mDataThemeIconMap.isEmpty() || !DataThemeIconLoader.hasDataThemeIcons())) {
            return;
        }
        mDataThemeState = state;
        DataThemeIconLoader.clearPlateCache();
        mDataThemeIconMap = DataThemeIconLoader.hasDataThemeIcons()
                ? DataThemeIconLoader.loadThemeDataMap()
                : Collections.emptyMap();
        Log.i(TAG, "data theme icons state=" + state + " size=" + mDataThemeIconMap.size()
                + " force=" + force
                + " oplusThirdPartyStyle=" + DataThemeIconLoader.hasThirdPartyStyle());
    }

    /**
     * ColorOS-style third-party icons: launcher-pack COVER/MASK plate
     * ({@code OplusConvertIcon}) behind the original logo when no theme PNG exists.
     */
    @Override
    protected Drawable wrapUnthemedIcon(ComponentName componentName, Drawable icon) {
        return applyThemePlate(icon);
    }

    /**
     * Theme PNGs that are mostly transparent (doodle glyphs) also need the shared
     * plate — otherwise only the stroke shows and third-party-style apps look unthemed.
     */
    @Override
    protected Drawable customizeLoadedThemeIcon(ComponentName componentName, Drawable icon) {
        if (icon == null || !DataThemeIconLoader.isSparseThemeGlyph(icon)) {
            return icon;
        }
        return applyThemePlate(icon);
    }

    private Drawable applyThemePlate(Drawable icon) {
        if (icon == null || !DataThemeIconLoader.hasThirdPartyStyle()) {
            return icon;
        }
        Drawable composed = DataThemeIconLoader.composeThirdPartyIcon(icon);
        return composed != null ? composed : icon;
    }
   //hxy-feature: desktop theme 202312
    /**
     * Enables or disables icon theme support
     */
    public void setIconThemeSupported(boolean isSupported) {
        mSupportsIconTheme = isSupported;
        mThemedIconMap = isSupported && FeatureFlags.USE_LOCAL_ICON_OVERRIDES.get()
                ? null : DISABLED_MAP;
        //hxy-feature: desktop theme 202312
        updateTheme();
        //hxy-feature: desktop theme 202312
    }
//hxy-feature: desktop theme 202312
    public void setIconThemeName(String themeName) {
        updateTheme();
    }
//hxy-feature: desktop theme 202312
    @Override
    protected ThemeData getThemeDataForComponent(ComponentName componentName) {
        if (componentName == null) {
            return null;
        }
        // Prefer /data/theme icons when present (ThemeStore apply path).
        if (mDataThemeIconMap == null) {
            reloadDataThemeIcons();
        }
        // /data/theme path already distinguishes Dialer Contacts activities.
        ThemeData dataTd = DataThemeIconLoader.getThemeDataForComponent(
                mDataThemeIconMap, componentName);
        if (dataTd != null) {
            return dataTd;
        }
        // Contacts under com.android.dialer: do not inherit package-level dialer icon
        // from either data-theme or XML theme maps.
        if (isDialerContactsActivity(componentName)) {
            Map<String, ThemeData> map = getThemedIconMap();
            ThemeData td = map.get(componentName.flattenToString());
            if (td == null) {
                td = map.get(componentName.flattenToShortString());
            }
            if (td == null) {
                td = map.get(CONTACTS_PACKAGE);
            }
            return td;
        }
        Map<String, ThemeData> map = getThemedIconMap();
        ThemeData td = map.get(componentName.flattenToString());
        if (td == null) {
            td = map.get(componentName.flattenToShortString());
        }
        if (td == null) {
            td = map.get(componentName.getPackageName());
        }
        return td;
    }

    @Override
    protected ThemeData getThemeDataForPackage(String packageName) {
        if (mDataThemeIconMap == null) {
            reloadDataThemeIcons();
        }
        if (mDataThemeIconMap != null) {
            ThemeData dataTd = mDataThemeIconMap.get(packageName);
            if (dataTd != null) {
                return dataTd;
            }
        }
        return getThemedIconMap().get(packageName);
    }

    @Override
    public String getSystemIconState() {
        return super.getSystemIconState()
                /**hxy-feature: desktop theme 202312*/ + mThemeName
                /**hxy-feature: desktop theme 202312*/
                + "," + mDataThemeState
                + (mSupportsIconTheme ? ",with-theme" : ",no-theme");
    }
    //hxy-feature: desktop theme 202312
    public int getIconMap(String name){
        if(TextUtils.isEmpty(name)){
            return 0;
        }else{
            return R.xml.icon_map_icon_package;
        }
    }

    //hxy-feature: desktop theme 202312
    private Map<String, ThemeData> getThemedIconMap() {
        if (mThemedIconMap != null) {
            return mThemedIconMap;
        }
        ArrayMap<String, ThemeData> map = new ArrayMap<>();
        Resources res = mContext.getResources();
//hxy-feature: desktop theme 202312
        Resources resall = null;
        int pathType = 0;
        try {
            resall = mContext.getPackageManager().getResourcesForApplication(packageName);
            int pathTypeId = resall.getIdentifier(
                        "theme_shape" , "integer", packageName);
            pathType = resall.getInteger(pathTypeId);
            Settings.Global.putInt(mContext.getContentResolver(), "persist.sys.theme.type", pathType);
        } catch (NameNotFoundException e) {
            resall = null;
            pathType = 0;
            Settings.Global.putInt(mContext.getContentResolver(), "persist.sys.theme.type", 0);
            e.printStackTrace();
        }
        //added by zhushuangqian for theme start
        int resId = getIconMap(mThemeName);
        if( resId <= 0 ){
            android.util.Log.e("zsq","get resId = null " );
            mThemedIconMap = DISABLED_MAP;
            return mThemedIconMap;
        }
        //added by zhushuangqian for theme end
//try (XmlResourceParser parser = res.getXml(R.xml.grayscale_icon_map)) {
        try (XmlResourceParser parser = res.getXml(resId)) {
            final int depth = parser.getDepth();
            int type;
            while ((type = parser.next()) != XmlPullParser.START_TAG
                    && type != XmlPullParser.END_DOCUMENT);

            while (((type = parser.next()) != XmlPullParser.END_TAG
                    || parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
                if (type != XmlPullParser.START_TAG) {
                    continue;
                }
                if (TAG_ICON.equals(parser.getName())) {
                    String pkg = parser.getAttributeValue(null, ATTR_PACKAGE);
                    String cls = parser.getAttributeValue(null, ATTR_CLASS);
                    String iconName = parser.getAttributeValue(null, ATTR_DRAWABLE);
                    if (!TextUtils.isEmpty(iconName) && !TextUtils.isEmpty(pkg) && resall != null) {
                        int iconId = resall.getIdentifier(
                            iconName , "drawable", packageName);
                        if (iconId > 0) {
                            String key = TextUtils.isEmpty(cls) ? pkg : pkg + "/" + cls;
                            map.put(key, new ThemeData(resall, iconId));
                        }
                    }
            ////hxy-feature: desktop theme 202312
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to parse icon map", e);
        }
        mThemedIconMap = map;
        return mThemedIconMap;
    }
        //hxy-feature: desktop theme 202312
    public void registerThemechange(Context context){
        context.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Themes.KEY_THEMED), false, mObserver);
        context.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(DataThemeIconLoader.SETTINGS_DATA_THEME),
                false, mDataThemeObserver);
    }

    public void unregisterThemechange(Context context){
        context.getContentResolver().unregisterContentObserver(mObserver);
        context.getContentResolver().unregisterContentObserver(mDataThemeObserver);
    }
    
    public ContentObserver mObserver = new ContentObserver(null) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    updateThemeWithChange();
                }
            };

    private final ContentObserver mDataThemeObserver = new ContentObserver(null) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            updateDataThemeWithLoader();
        }
    };
    
    public void updateThemeWithLoader(){
        if(SHOW_THEME_ICON){
            String themeName = Themes.getThemedName(mContext);
            if(!TextUtils.equals(mThemeName, themeName)){
                mThemedIconMap = null;
                mThemeName = themeName;
                packageName = mThemeName;
                getThemedIconMap();
                LauncherAppState.getInstance(mContext).refreshAndReloadLauncher();
            }
        }
    }
    public void updateThemeWithChange(){
        updateThemeWithLoader();
    }

    /**
     * Called when UXDesign writes Settings.Global {@code dream_data_theme}.
     * OPPO uses config → {@code Launcher.recreate()}; Dream coalesces with
     * {@code THEME_APPLIED} via {@link LauncherAppState#scheduleDataThemeRecreate()}.
     */
    public void updateDataThemeWithLoader() {
        LauncherAppState app = LauncherAppState.getInstanceNoCreate();
        if (app != null) {
            app.scheduleDataThemeRecreate();
        } else {
            reloadDataThemeIcons(true);
            LauncherAppState.getInstance(mContext).refreshAndReloadLauncher();
        }
    }
    //hxy-feature: desktop theme 202312
}
