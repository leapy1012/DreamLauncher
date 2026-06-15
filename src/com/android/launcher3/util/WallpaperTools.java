package com.android.launcher3.util;

import android.app.WallpaperManager;
import android.content.Context;
import com.android.launcher3.util.Executors;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class WallpaperTools {
    public static InputStream getSavedWallpaper(String path) {
        File imgFile = new File(path);
        if (!imgFile.exists()) {
            return null;
        }
        try {
            FileInputStream fis = new FileInputStream(path);
            return fis;
        } catch (Exception e) {
            return null;
        }
    }

    public static void setWallpaper(final Context c, final int wallpaperId, final String path) {
        Executors.MODEL_EXECUTOR.post(new Runnable() { 
            @Override
            public final void run() {
                InputStream is = getSavedWallpaper(path);
                if (is != null) {
                    WallpaperManager wallpaperManager = WallpaperManager.getInstance(c.getApplicationContext());
                    try {
                        wallpaperManager.setStream(is, null, true, wallpaperId);
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}