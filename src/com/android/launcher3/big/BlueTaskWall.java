package com.android.launcher3.big;

import android.app.WallpaperManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.customer.tools.ImageUtils;


public class BlueTaskWall {
    private static final String TAG = "BlueTaskWall";
    private Launcher mLauncher;
    private LauncherBackgroudView mWallpaperView;

    public BlueTaskWall(Launcher mLauncher, LauncherBackgroudView mWallpaperView) {
        this.mLauncher = mLauncher;
        this.mWallpaperView = mWallpaperView;
    }

    public void blurBackground(Drawable bac_launcher) {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(mLauncher);
        mWallpaperView.setLauncherBg(bac_launcher, null);
        if (wallpaperManager.getWallpaperInfo() != null) {
            //mWallpaperView.setLauncherBg(null, null);
        }
        mWallpaperView.invalidate();
        mWallpaperView = null;
    }

    public Drawable load() {
        Drawable bg_drwable = null;
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(this.mLauncher.getApplicationContext());
        try {
            int widthPx = mLauncher.getDeviceProfile().widthPx;
            int heightPx = mLauncher.getDeviceProfile().heightPx;
            Bitmap bm = wallpaperManager.getBitmap();
            if (bm == null) {
                Drawable temp = wallpaperManager.getBuiltInDrawable();
                bm = ImageUtils.drawableToBitmap(temp);
            }
            Bitmap.createBitmap(bm, 0, 0, Math.min(bm.getWidth(), widthPx), Math.min(bm.getHeight(), heightPx));
            int w = (int) (bm.getWidth() * 0.15f);
            int h = (int) (bm.getHeight() * 0.15f);
            Bitmap blu = ImageUtils.blurBitmap(mLauncher, ImageUtils.resize(bm, w, h));
            if (blu != null) {
                Canvas canvas = new Canvas(blu);
                canvas.drawColor(1275068416);
                canvas.release();
            }
            bg_drwable = new BitmapDrawable(blu);
            return bg_drwable;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bg_drwable;
    }
}
