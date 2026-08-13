package com.android.launcher3.big;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.coui.appcompat.seekbar.COUISeekBar;
import com.coui.appcompat.seekbar.COUISectionSeekBar;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.customer.table.CustomTable;
import com.android.launcher3.customer.tools.PrefTools;
import com.android.launcher3.util.Executors;
import com.android.launcher3.R;
import androidx.core.content.res.ResourcesCompat;
import java.util.ArrayList;
import java.util.List;
import com.android.launcher3.settings.SettingsBaseActivity;
import com.android.launcher3.views.RadioGroupLinearLayout;
import com.android.launcher3.util.Themes;
import android.provider.Settings;
import android.app.ProgressDialog;
import androidx.appcompat.app.ActionBar;

public class IconSizeSettingActivity extends SettingsBaseActivity {
    private static final String TAG = "IconSizeSettingActivity";
    public static String ICON_SIZE_PROGRESS = "icon_size_progress";
    public static String INIT_ICON_SIZE = "init_icon_size";
    public static Float PROGRESS_DEFAULT_VALUE = Float.valueOf(1.0f);
    private IconSizeSettingAdapter adapter;
    private List<CustomTable> list = new ArrayList();
    private RecyclerView mRv;
    private LoadThemeTask task;
    private RadioGroupLinearLayout mRadioIcon;
    private int mDefaultIndex = 0, mCurrentIndex = 0;
    private static final int PROCESS_KILL_DELAY_MS = 3000;

    public static float stringToFloat(String str) {
        try {
            return Float.parseFloat(str);
        } catch (Exception e) {
            e.printStackTrace();
            return 1.0f;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_icon_size_setting);

        LoadThemeTask loadThemeTask = new LoadThemeTask();
        this.task = loadThemeTask;
        loadThemeTask.execute(new Void[0]);
        Float progress = Float.valueOf((PrefTools.getFloat(ICON_SIZE_PROGRESS,
                PROGRESS_DEFAULT_VALUE.floatValue(), this) - 0.75f) / 0.4f);
        final int sectionProgress = Math.max(0, Math.min(4, Math.round(progress * 4f)));
        IconSizeSettingAdapter iconSizeSettingAdapter = new IconSizeSettingAdapter(this, this.list);
        this.adapter = iconSizeSettingAdapter;
        iconSizeSettingAdapter.setProgress(sectionProgress / 4f);
        this.adapter.firstTag = true;
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.iconsizeRv);
        this.mRv = recyclerView;
        recyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        this.mRv.setAdapter(this.adapter);
        this.mRv.setLayerType(View.LAYER_TYPE_HARDWARE, (Paint) null);
        final COUISectionSeekBar sectionSeekBar = findViewById(R.id.demo_5_seek_bar_2);
        sectionSeekBar.setMin(0);
        sectionSeekBar.setMax(4);
        sectionSeekBar.setProgress(sectionProgress);
        sectionSeekBar.setOnSeekBarChangeListener(new COUISeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(COUISeekBar seekBar, int progress, boolean fromUser) {
                IconSizeSettingActivity.this.adapter.setProgress(progress / 4f);
            }

            @Override
            public void onStartTrackingTouch(COUISeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(COUISeekBar seekBar) {
            }
        });
        ((TextView) findViewById(R.id.applyTv)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mDefaultIndex != mCurrentIndex) {
                    mDefaultIndex = mCurrentIndex;

                    Settings.Global.putInt(getContentResolver() , Themes.KEY_THEMED_ICONS, mCurrentIndex);
                }
                ProgressDialog.show(IconSizeSettingActivity.this,
                    null /* title */,
                    IconSizeSettingActivity.this.getString(R.string.icon_shape_override_progress),
                    true /* indeterminate */,
                    true /* cancelable */);

                Executors.MODEL_EXECUTOR.execute(new OverrideApplyHandler(IconSizeSettingActivity.this));
                PrefTools.putFloat(IconSizeSettingActivity.ICON_SIZE_PROGRESS,
                        ((sectionSeekBar.getProgress() / 4f) * 0.4f) + 0.75f,
                        IconSizeSettingActivity.this);
                // Intent intent = new Intent();
                // intent.setAction("android.intent.action.MAIN");
                // intent.addCategory("android.intent.category.HOME");
                // IconSizeSettingActivity.this.startActivity(intent);
                // IconSizeSettingActivity.this.finish();
                // Executors.MAIN_EXECUTOR.execute(() -> InvariantDeviceProfile.INSTANCE.get(IconSizeSettingActivity.this).onConfigChanged(IconSizeSettingActivity.this));
            }
        });
        String mask_typ = android.os.SystemProperties.get("persist.sys.mask_typ", "");
        if (!android.text.TextUtils.isEmpty(mask_typ)) {
            mDefaultIndex = Integer.parseInt(mask_typ);
        }
        mRadioIcon = (RadioGroupLinearLayout) findViewById(R.id.radioIcon);
        mRadioIcon.setIcons(
            new int[]{R.drawable.icon_mask_default, R.drawable.icon_mask_neighbourhood, R.drawable.icon_mask_droplet, R.drawable.icon_mask_circle}, 
            new int[]{R.drawable.icon_mask_default_selected, R.drawable.icon_mask_neighbourhood_selected, R.drawable.icon_mask_droplet_selected, R.drawable.icon_mask_circle_selected}, 
            new int[]{R.string.home_screen_icon_shape_default, R.string.home_screen_icon_shape_neighbourhood, R.string.home_screen_icon_shape_droplet, R.string.home_screen_icon_shape_circle}, mDefaultIndex, 20, 20);
        mRadioIcon.setOnItemClickListener(new RadioGroupLinearLayout.onItemClickListener() {
            public void onItemClick(int index) {
                mCurrentIndex = index;
            }
        });
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.hxy_ic_back);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static List<CustomTable> load(Context context) {
        List<CustomTable> customs = new ArrayList<>();
        for (Object info : (List) LauncherAppState.getInstance(context).getModel().mBgAllAppsList.data.clone()) {
            CustomTable t = new CustomTable();
            t.toInstance((AppInfo) info, context);
            if (t.componentNameStr == null || !t.componentNameStr.equals("com.android.launcher3/.big.HxyCardLauncher")) {
                customs.add(t);
            }
        }
        return customs;
    }

    class LoadThemeTask extends AsyncTask<Void, Void, List<CustomTable>> {
        LoadThemeTask() {
        }

        @Override
        protected List<CustomTable> doInBackground(Void... params) {
            List<CustomTable> customTables = IconSizeSettingActivity.load(IconSizeSettingActivity.this);
            return customTables.size() > 12 ? customTables.subList(0, 12) : customTables;
        }

        @Override
        protected void onPostExecute(List<CustomTable> themeTables) {
            super.onPostExecute(themeTables);
            IconSizeSettingActivity.this.list.addAll(themeTables);
            IconSizeSettingActivity.this.adapter.notifyDataSetChanged();
        }
    }

    class OverrideApplyHandler implements Runnable {
        private final Context mContext;
        private final static int RESTART_REQUEST_CODE = 1001;

        private OverrideApplyHandler(Context context) {
            mContext = context;
        }

        @Override
        public void run() {
            // Synchronously write the preference.

            // Clear the icon cache.
            LauncherAppState.getInstance(mContext).getIconCache().clear();

            // Wait for it
            try {
                Thread.sleep(PROCESS_KILL_DELAY_MS);
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error waiting", e);
            }

            // lunch home activity
            Intent homeIntent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            mContext.startActivity(homeIntent);

            // Kill process
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setTitle(getString(R.string.home_screen_icon_custom));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LoadThemeTask loadThemeTask = this.task;
        if (loadThemeTask != null && loadThemeTask.getStatus() == AsyncTask.Status.RUNNING) {
            this.task.cancel(true);
        }
    }
}
