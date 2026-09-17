package com.android.launcher.guide.side;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.COUIRecyclerView;

import com.android.launcher3.R;
import com.coui.appcompat.toolbar.COUIToolbar;
import com.google.android.material.appbar.AppBarLayout;

/**
 * ColorOS-style entry page used by Settings for "Learn gestures".
 */
public class LearningGesturesActivity extends AppCompatActivity
        implements GuideLearningViewAdapter.OnRecycleItemClickListener {

    private static final long QUICK_CLICK_INTERVAL_MS = 1000;

    private AppBarLayout mAppBarLayout;
    private COUIRecyclerView mRecyclerView;
    private COUIToolbar mToolbar;
    private long mLastItemClickTime;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(getFixedDensityContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guide_page_learning);
        mAppBarLayout = findViewById(R.id.appBarLayout);
        mRecyclerView = findViewById(R.id.recycler_view);
        mToolbar = findViewById(R.id.toolbar);
        configureWindow();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initToolBar();
        initRecyclerView();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        initToolBar();
        initRecyclerView();
    }

    private void configureWindow() {
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(
                getColor(R.color.launcher_activity_background_color));
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }

    private void initToolBar() {
        mToolbar.setTitle(R.string.oplus_learn_gestures);
        setSupportActionBar(mToolbar);
        mToolbar.setNavigationOnClickListener(view -> finish());
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(com.coui.appcompat.R.drawable.coui_back_arrow);
        }
        mAppBarLayout.setPadding(0, getStatusBarHeight(), 0, 0);
    }

    private void initRecyclerView() {
        GuideLearningViewAdapter adapter = new GuideLearningViewAdapter(this);
        adapter.setOnItemClickListener(this);
        LearningGesturesGridLayoutManager layoutManager =
                new LearningGesturesGridLayoutManager(this, 2);
        layoutManager.setScrollEnabled(false);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setClipToPadding(false);
        mRecyclerView.setAdapter(adapter);
    }

    @Override
    public void onClick(int position) {
        long now = SystemClock.elapsedRealtime();
        if (now - mLastItemClickTime < QUICK_CLICK_INTERVAL_MS) {
            return;
        }
        mLastItemClickTime = now;

        int guideType = position == 0 ? 1 : position + 2;
        Intent intent = new Intent(this, SideSlipGesturesGuideActivity.class);
        intent.putExtra(SideSlipGesturesGuideActivity.EXTRA_GUIDE_START_TYPE, guideType);
        startActivity(intent);
    }

    private int getStatusBarHeight() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id == 0 ? 0 : getResources().getDimensionPixelSize(id);
    }

    /**
     * OPPO's DisplayDensityUtils restores the display's initial density instead of deriving a
     * synthetic density from the current window width. Keep that behavior local to this activity.
     */
    private static Context getFixedDensityContext(Context context) {
        try {
            int densityDpi = WindowManagerGlobal.getWindowManagerService()
                    .getInitialDisplayDensity(Display.DEFAULT_DISPLAY);
            WindowManager windowManager = context.getSystemService(WindowManager.class);
            if (windowManager != null) {
                DisplayMetrics realMetrics = new DisplayMetrics();
                windowManager.getDefaultDisplay().getRealMetrics(realMetrics);
                int physicalWidth = Math.min(realMetrics.widthPixels, realMetrics.heightPixels);
                if (physicalWidth == 1440) {
                    densityDpi = 640;
                } else if (physicalWidth > 1080 && physicalWidth < 1440) {
                    densityDpi = 560;
                } else if (physicalWidth == 1080 && densityDpi == 420) {
                    densityDpi = 480;
                }
            }
            if (densityDpi >= 160
                    && densityDpi != context.getResources().getConfiguration().densityDpi) {
                Configuration configuration =
                        new Configuration(context.getResources().getConfiguration());
                configuration.densityDpi = densityDpi;
                return context.createConfigurationContext(configuration);
            }
        } catch (RemoteException | RuntimeException ignored) {
            // Retain the current configuration if the platform display service is unavailable.
        }
        return context;
    }
}
