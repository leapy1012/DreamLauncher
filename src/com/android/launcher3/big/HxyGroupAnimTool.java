package com.android.launcher3.big;

import android.view.View;
import com.android.launcher3.R;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.pageindicators.PageIndicatorDots;
import com.android.launcher3.statemanager.BaseState;
import com.android.launcher3.util.Executors;
import java.util.function.Consumer;

public class HxyGroupAnimTool<STATE_TYPE extends BaseState<STATE_TYPE>> {
    public static int mDuration = 500;
    private String TAG = "HxyGroupAnimTool";
    boolean haset = false;
    private Launcher mLauncher;
    public PageIndicatorDots mPageIndicator;
    public LauncherBackgroudView mWallpaperBg;
    boolean successful = false;

    public LauncherBackgroudView getWallpaperBg() {
        return this.mWallpaperBg;
    }

    public PageIndicatorDots getPageIndicator() {
        return this.mPageIndicator;
    }

    public HxyGroupAnimTool(Launcher mLauncher2) {
        this.mLauncher = mLauncher2;
    }

    public void init(Launcher l) {
        mLauncher = l;
        mWallpaperBg = mLauncher.findViewById(R.id.wallpaper_set_anim);
        setBlurWallpaper(mWallpaperBg, mLauncher);
    }

    public void onBackPressed() {
    }

    public void onStateTransitionStart(STATE_TYPE state) {
        if (state == LauncherState.SPRING_LOADED) {
            this.mLauncher.getWorkspace().setLayerType(View.LAYER_TYPE_NONE, null);
        }
        this.mLauncher.isInState(LauncherState.NORMAL);
        this.mLauncher.isInState(LauncherState.OVERVIEW);
    }

    public void onStateTransitionEnd(LauncherState state) {
        if (state == LauncherState.NORMAL) {
            if (!this.mLauncher.getDragController().isDragging()) {
                this.mLauncher.getWorkspace().removeExtraEmptyScreen(true);
            }
        }
        if (state == LauncherState.SPRING_LOADED) {
            this.successful = false;
        }
    }

    public void setBlurWallpaper(View v, Launcher l) {
        setBlueWallpaperTask(v, l);
    }

    public void setBlueWallpaperTask(View v, final Launcher l) {
        if (!this.haset) {
            this.haset = true;
            l.getWorkspace().postDelayed(new Runnable() {
                @Override
                public void run() {
                    HxyGroupAnimTool.this.haset = false;
                    ((LauncherBackgroudView) l.findViewById(R.id.wallpaper_set_anim)).startAnim(View.ALPHA, 0.0f, 1.0f, false, new Consumer<String>() {
                        @Override
                        public void accept(String s) {
                            Executors.MODEL_EXECUTOR.execute(new Runnable() {
                                @Override
                                public void run() {
                                    BlueTaskWall b = new BlueTaskWall(l, mWallpaperBg);
                                    Executors.MAIN_EXECUTOR.execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            b.blurBackground(b.load());
                                        }
                                    });
                                }
                            });
                        }
                    }, new Consumer<String>() {
                        @Override
                        public void accept(String s) {

                        }
                    });
                }
            }, mDuration);
        }
    }

    public void closeFolder() {
        this.mWallpaperBg.destoryIconBlue();
        this.mWallpaperBg.invalidate();
    }
}