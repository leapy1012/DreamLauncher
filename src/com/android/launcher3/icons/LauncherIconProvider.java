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

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import com.android.launcher3.R;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.util.Themes;

import org.xmlpull.v1.XmlPullParser;

import java.util.Collections;
import java.util.Map;
    //hxy-feature: desktop theme 202312
import android.os.SystemProperties;
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
    private static final String ATTR_DRAWABLE = "drawable";

    private static final String TAG = "LIconProvider";
    private static final Map<String, ThemeData> DISABLED_MAP = Collections.emptyMap();

    private Map<String, ThemeData> mThemedIconMap;
    private boolean mSupportsIconTheme;
    //hxy-feature: desktop theme 202312
    private String mThemeName = "";
    private Context mContetxt;
    private String packageName = "";
    private static final boolean SHOW_THEME_ICON = SystemProperties.getInt("persist.sys.hxy_theme_icon", 0) == 1;
    //hxy-feature: desktop theme 202312
    public LauncherIconProvider(Context context) {
        super(context);
        this.mContetxt = context;//hxy-feature: desktop theme 202312
        setIconThemeSupported(Themes.isThemedIconEnabled(context));
        //hxy-feature: desktop theme 202312
        updateTheme();
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
    protected ThemeData getThemeDataForPackage(String packageName) {
        return getThemedIconMap().get(packageName);
    }

    @Override
    public String getSystemIconState() {
        return super.getSystemIconState() /**hxy-feature: desktop theme 202312*/+ mThemeName/**hxy-feature: desktop theme 202312*/ + (mSupportsIconTheme ? ",with-theme" : ",no-theme");
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
                    String iconName = parser.getAttributeValue(null, ATTR_DRAWABLE);
                    if(!TextUtils.isEmpty(iconName) && !TextUtils.isEmpty(pkg) && resall != null) {

                        int iconId = resall.getIdentifier(
                            iconName , "drawable", packageName);
                        if(iconId > 0){
                            map.put(pkg, new ThemeData(resall, iconId));
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
        context.getContentResolver().registerContentObserver(Settings.Global.getUriFor(Themes.KEY_THEMED), false, mObserver);
    }

    public void unregisterThemechange(Context context){
        context.getContentResolver().unregisterContentObserver(mObserver);}
    
    public ContentObserver mObserver = new ContentObserver(null) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    updateThemeWithChange();
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
    //hxy-feature: desktop theme 202312
}
