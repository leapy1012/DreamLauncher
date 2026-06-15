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

import android.content.Intent;
import android.os.Bundle;
import android.telecom.Call;

import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import android.app.ActivityTaskManager;
import androidx.lifecycle.LiveData;
import com.android.launcher3.util.Themes;
import android.provider.Settings;
import android.text.TextUtils;
import android.content.res.Resources;
import com.android.launcher3.R;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.graphics.drawable.Drawable;
import java.util.ArrayList;
import android.widget.GridView;
import android.content.Context;
import android.view.View;
import android.content.om.IOverlayManager;
import android.content.om.OverlayManager;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.ServiceManager;
import android.app.WallpaperManager;
import android.graphics.drawable.Drawable;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.PixelFormat;
import android.content.pm.PackageInfo;
import android.content.om.OverlayInfo;
import android.content.om.OverlayableInfo;
import java.util.List;
import android.os.RemoteException;
import android.os.UserHandle;
import android.graphics.drawable.BitmapDrawable;
import com.android.launcher3.settings.SettingsBaseActivity;
import android.widget.TextView;
import android.app.ProgressDialog;
import android.app.ActionBar;
import android.app.AlertDialog;
import com.android.launcher3.util.Executors;

public class ThemeActivity extends SettingsBaseActivity {
    private static final String TAG = "ThemeActivity";
    private GridViewAdapter mAdapter;

    private String mCurrentTheme;
    GridView allThemeGridView;
    private ArrayList<ThemeData> mGridViewItems;
    private Context mContext;
    private int mChooseitemId;
    private final String THEME_KEY = "theme_key";
    private final String THEME_TITLE = "theme_title";
    private final String THEME_PREVIEW = "preview_theme";
    private final String KEY_THEME = Themes.KEY_THEMED;

    private final boolean  SHAPE_OVERLAY_ALLOWED = false;
    private final boolean  WALLPAPER_SET_ALLOWED = true;

    private IOverlayManager mOverlayService;
    private PackageManager mPackageManager;
    private  WallpaperManager mWallpaperManager;
    private static final int PROCESS_KILL_DELAY_MS = 1000;
    ProgressDialog mProgressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.theme_actvity);
        mContext = this ;
        mOverlayService = IOverlayManager.Stub
                .asInterface(ServiceManager.getService(Context.OVERLAY_SERVICE));
        mWallpaperManager =  (WallpaperManager) mContext.getSystemService(mContext.WALLPAPER_SERVICE);
        mPackageManager = mContext.getPackageManager();
        allThemeGridView = (GridView) this.findViewById(R.id.all_items_grid);
        allThemeGridView.setNumColumns(2);
        mAdapter = initAdapter();
        allThemeGridView.setAdapter(mAdapter);
        allThemeGridView.setOnItemClickListener(new OnGridviewItemClick());
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.hxy_ic_back);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setTitle(R.string.launcher_theme_title);
    }

    private void initThemeData(){
        String themeName = Settings.Global.getString(mContext.getContentResolver(), KEY_THEME);
        String[] themeStringList = getAvailableThemes(false);
                    //mContext.getResources().getStringArray(R.array.theme_list);
        mGridViewItems = new ArrayList<>();
        mChooseitemId = 0;

        for(int i = 1 ; i < themeStringList.length; i ++){
            String title = "";
            Drawable preview = null;
            ThemeData themeData = new ThemeData();
            try {
                String packageName = themeStringList[i];
                Resources resall = mPackageManager.getResourcesForApplication(packageName);
                if(TextUtils.equals(themeStringList[i],themeName)) {
                    mChooseitemId = i;
                }

                title = resall.getString(resall.getIdentifier(THEME_TITLE, "string", packageName));
                preview =  resall.getDrawable(resall.getIdentifier(THEME_PREVIEW , "drawable", packageName));

                if(!TextUtils.isEmpty(title)){
                    themeData.key = themeStringList[i];
                    themeData.themeTitle = title;
                    themeData.preview = preview;
                    mGridViewItems.add(themeData);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private GridViewAdapter initAdapter() {
        initThemeData();
        return new GridViewAdapter(mContext, mGridViewItems);
    }

    private void killProcess() {
         android.os.Process.killProcess(android.os.Process.myPid());
    }

    public static class ThemeData {
        public Drawable preview = null;
        public String themeTitle = "";
        public String key = "";
    }

    private class OnGridviewItemClick implements OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> arg0, View view, int position,
                long id) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ThemeActivity.this);
            builder.setTitle(getString(R.string.settings_style));
            builder.setMessage(getString(R.string.sure_set_theme));
            builder.setCancelable(false);
            builder.setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                dialog.dismiss();

                mProgressDialog = ProgressDialog.show(ThemeActivity.this,
                    null /* title */,
                    ThemeActivity.this.getString(R.string.hxy_apply_theme),
                    true /* indeterminate */,
                    true /* cancelable */);
                String theme = ((ThemeData) mGridViewItems.get(position)).key;
                if (TextUtils.equals("default", theme)) {
                    theme = "";
                }
                Executors.MODEL_EXECUTOR.execute(new ApplyThemeHandler(ThemeActivity.this, theme));
            });
            builder.setNegativeButton(getString(R.string.cancel),
                    (dialog, which) -> dialog.dismiss());
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }
    private Resources getThemeResource(String packageName){
        Resources resall = null;
        try {
            resall = mPackageManager.getResourcesForApplication(packageName);
        } catch (NameNotFoundException e) {
            resall = null;
            e.printStackTrace();
        }
        return resall;
    }

    private boolean isWallpaperAllowed() {
        return mWallpaperManager.isSetWallpaperAllowed();
    }

    private void setWallpaper(Resources resall,String packageName){
        if(!WALLPAPER_SET_ALLOWED && !isWallpaperAllowed()){
            return;
        }
        Drawable wallpaper = null;
        try{
            if( TextUtils.isEmpty(packageName) || "none".equals(packageName) ){
                mWallpaperManager.clearWallpaper();
            }else {
                wallpaper = resall.getDrawable(resall.getIdentifier("wallpaper", "drawable", packageName));
                if(null != wallpaper){
                    mWallpaperManager.setBitmap(drawableToBitamp(wallpaper));
                }
            }
        }catch(Exception e){
            e.printStackTrace();
            wallpaper = null;
        }
    }

    private void disableOverlayShape(String packageName){
        try {
            mOverlayService.setEnabled(packageName,false,UserHandle.myUserId());
        } catch (RemoteException re) {
            throw re.rethrowFromSystemServer();
        }
    }

    private void setOverlayShape(String lastThemeName, String packageName){
        if(!SHAPE_OVERLAY_ALLOWED){
            return;
        }
        try {
            if(TextUtils.isEmpty(packageName) || "none".equals(packageName)){
                mOverlayService.setEnabled(lastThemeName,false,UserHandle.myUserId());
            }else{
                mOverlayService.setEnabledExclusiveInCategory(packageName, UserHandle.myUserId());
            }

        } catch (RemoteException re) {
            throw re.rethrowFromSystemServer();
        }
    }

    private Bitmap drawableToBitamp(Drawable drawable){
        BitmapDrawable bd = (BitmapDrawable) drawable;
        Bitmap bitmap = bd.getBitmap();
        return bitmap;
    }
    
    String[] getAvailableThemes(boolean currentThemeOnly) {
        List<OverlayInfo> infos;
        List<String> pkgs;
        try {
            infos = mOverlayService.getOverlayInfosForTarget("android", UserHandle.myUserId());
            pkgs = new ArrayList<>(infos.size());
            pkgs.add("none");
            for (int i = 0, size = infos.size(); i < size; i++) {
                if (isTheme(infos.get(i))) {
                    if (infos.get(i).isEnabled() && currentThemeOnly) {
                        return new String[] {infos.get(i).packageName};
                    } else {
                        pkgs.add(infos.get(i).packageName);
                    }
                }
            }
        } catch (RemoteException re) {
            throw re.rethrowFromSystemServer();
        }

        // Current enabled theme is not found.
        if (currentThemeOnly) {
            return new String[0];
        }
        return pkgs.toArray(new String[pkgs.size()]);
    }
    private boolean isTheme(OverlayInfo oi) {
        if (!"android.theme.customization.icon_pack.android".equals(oi.category)) {
            return false;
        }
        try {
            PackageInfo pi = mPackageManager.getPackageInfo(oi.packageName, 0);
            return pi != null && !pi.isStaticOverlayPackage();
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private static void dumpBitmap(Bitmap b, String name) {
        if (name == null) {
            name = "Bitmap";
        }
        if (b != null) {
            try{
            }catch(Exception e){
                e.printStackTrace();
            }
            java.io.FileOutputStream fos = null;
            try {
                String time= new java.text.SimpleDateFormat("yyyyMMdd_HHmmss_SSS") .format(new java.util.Date());
                String filename = android.os.Environment.getExternalStorageDirectory().toString()+"/DCIM/" + name + "["+ time + "].png";

                fos = new java.io.FileOutputStream(filename);
                b.compress(Bitmap.CompressFormat.PNG, 100, fos);
            } catch (java.io.IOException ex) {
            }
            finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (java.io.IOException ex) {
                    }
                }
            }
        }
    }

    class ApplyThemeHandler implements Runnable {
        private final Context mContext;
        private final String themeName;
        private final static int RESTART_REQUEST_CODE = 1001;

        private ApplyThemeHandler(Context context, String name) {
            mContext = context;
            themeName = name;
        }

        @Override
        public void run() {

            // Wait for it
            try {
                Thread.sleep(PROCESS_KILL_DELAY_MS);
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error waiting", e);
            }
            String lastThemeName = Settings.Global.getString(mContext.getContentResolver(), KEY_THEME);
            if (!TextUtils.equals(lastThemeName, themeName)) {
                setOverlayShape(lastThemeName,themeName);
                Resources currentThemeResouce = getThemeResource(themeName);
                setWallpaper(currentThemeResouce, themeName);
                Settings.Global.putString(mContext.getContentResolver() , KEY_THEME, themeName);
            }
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            Intent homeIntent = new Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            mContext.startActivity(homeIntent);
            killProcess();
        }
    }
}
