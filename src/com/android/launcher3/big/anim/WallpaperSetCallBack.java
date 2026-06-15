package com.android.launcher3.big.anim;

import android.provider.Settings;
import android.util.Log;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import com.android.launcher3.util.WallpaperTools;
import android.content.Context;
import com.android.launcher3.big.HxyAnimBubbleTextView;

public class WallpaperSetCallBack extends BaseCallback {
    private static final String TAG = "WallpaperSetCallBack";
    private int tmp = -1;

    private ArrayList<String> mPlayFileList = new ArrayList<>();

    public WallpaperSetCallBack(HxyAnimBubbleTextView icon) {
        super(icon);
    }
    public WallpaperSetCallBack() {
        super();
    }

    @Override
    public void onStart() {

    }

    @Override
    public void onRunning() {
    }

    @Override
    public void onEnd() {
        if(mIcon!=null)
            mIcon.setIconVisible(true);
    }

    public void onSetWallpaper() {
        setWallpaper();
    }

    public void setWallpaper() {
         String localpath = getLocalPaper();
         Log.d(TAG, " wallpaper localpath: " + localpath);
         if (localpath != null) {
            WallpaperTools.setWallpaper(mIcon.getContext(), 3, localpath);
         }
     }

     public void setWallpaper_lock() {
          String localpath = getLocalPaper_lock(mIcon.getContext());
          Log.d(TAG, " wallpaper localpath: " + localpath);
          if (localpath != null) {
              WallpaperTools.setWallpaper(mIcon.getContext(), 2, localpath);
          }
      }


     private String getLocalPaper() {
         listFiles(mPlayFileList,"system/media/config/wallpaper/");
         if (mPlayFileList.size() <= 0) {
             return null;
         }
         Random random = new Random();
         int wallpaper_num = Settings.System.getInt(mIcon.getContext().getContentResolver(), "wallpaper_num", 0);
         int n = random.nextInt(this.mPlayFileList.size());
         if (wallpaper_num == n) {
             while (wallpaper_num == n) {
                 n = random.nextInt(this.mPlayFileList.size());
             }
         }
         Log.d(TAG, "hxy_widget_wallpaper_num:" + wallpaper_num + " n:" + n);
         Settings.System.putInt(this.mIcon.getContext().getContentResolver(), "wallpaper_num", n);
         return this.mPlayFileList.get(n);
     }


     private String getLocalPaper_lock(Context context) {
         listFiles(this.mPlayFileList,"system/media/config/wallpaper/");
         if (this.mPlayFileList.size() <= 0) {
             return null;
         }
         Random random = new Random();
         int wallpaper_num = Settings.System.getInt(context.getContentResolver(), "wallpaper_num", 0);
         int n = random.nextInt(this.mPlayFileList.size());
         if (wallpaper_num == n) {
             while (wallpaper_num == n) {
                 n = random.nextInt(this.mPlayFileList.size());
             }
         }
         Log.d(TAG, "hxy_widget_wallpaper_num:" + wallpaper_num + " n:" + n);
         Settings.System.putInt(context.getContentResolver(), "wallpaper_num", n);
         return this.mPlayFileList.get(n);
     }

     private static void listFiles(ArrayList<String> arrayList,String path) {
         arrayList.clear();
         File[] listFiles = new File(path).listFiles();
         if (listFiles == null) {
             Log.d(TAG, "file in system/media/config/wallpaper/ is no files");
             return;
         }
         Arrays.sort(listFiles, new Comparator<File>() {
             public int compare(File o1, File o2) {
                 return o1.getName().compareTo(o2.getName());
             }
         });
         for (File file : listFiles) {
             if (file.isFile() && file.getName().endsWith(".jpg") && !file.getName().endsWith("small.jpg")) {
                 arrayList.add(file.getPath());
             }
             if (file.isFile() && file.getName().endsWith(".png") && !file.getName().endsWith("small.png")) {
                 arrayList.add(file.getPath());
             }
         }
     }

    @Override
    public boolean onClick() {
        return false;
    }
}
