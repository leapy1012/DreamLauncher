package com.android.launcher3.big;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Outline;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.COUIRecyclerView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.R;
import com.android.launcher3.customer.table.CustomTable;
import com.android.launcher3.customer.tools.ImageUtils;
import com.android.launcher3.customer.tools.PrefTools;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.settings.SettingsBaseActivity;
import com.android.launcher3.util.Executors;
import com.android.launcher3.util.Themes;
import com.android.launcher3.views.RadioGroupLinearLayout;
import com.coui.appcompat.button.COUILoadingButton;
import com.coui.appcompat.seekbar.COUISeekBar;

import java.util.ArrayList;
import java.util.List;

public class IconSizeSettingActivity extends SettingsBaseActivity {

    private static final String TAG = "IconSizeSettingActivity";
    public static String ICON_SIZE_PROGRESS = "icon_size_progress";
    public static String INIT_ICON_SIZE = "init_icon_size";
    public static Float PROGRESS_DEFAULT_VALUE = Float.valueOf(1.0f);

    private static final int SEEK_MAX = 100;
    private static final float SIZE_MIN = 0.75f;
    private static final float SIZE_RANGE = 0.4f;
    private static final float WALLPAPER_BLUR_SCALE = 0.25f;
    private static final int PROCESS_KILL_DELAY_MS = 3000;

    private IconSizeSettingAdapter adapter;
    private List<CustomTable> list = new ArrayList<>();
    private RecyclerView mRv;
    private ImageView mPreviewWallpaper;
    private View mPreviewPanel;
    private TextView mSizeDefaultLabel;
    private TextView mSizeReset;
    private COUISeekBar mSizeSeekBar;
    private COUILoadingButton mApplyButton;
    private LoadThemeTask task;
    private LoadWallpaperTask wallpaperTask;
    private RadioGroupLinearLayout mRadioIcon;
    private int mDefaultIndex = 0;
    private int mCurrentIndex = 0;
    private float mCurrentProgress;
    private boolean mIconSizeAtDefault = true;
    private boolean mSizeGuideFirstLoad = true;
    private boolean mIsTrackingSeekBar;
    private boolean mRestoringDefault;
    private android.animation.AnimatorSet mSizeGuideAnimator;
    private static final long SIZE_GUIDE_CROSSFADE_MS = 200L;
    /** Oppo UxInteractView hardcodes default seek progress at 62. */
    private static final int DEFAULT_ICON_SIZE_PROGRESS = 62;

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
        setTitle(R.string.home_screen_icon_custom);

        String maskTyp = android.os.SystemProperties.get("persist.sys.mask_typ", "");
        if (!android.text.TextUtils.isEmpty(maskTyp)) {
            try {
                mDefaultIndex = Integer.parseInt(maskTyp);
            } catch (NumberFormatException ignored) {
                mDefaultIndex = 0;
            }
        }
        mCurrentIndex = mDefaultIndex;

        COUIRecyclerView pageList = findViewById(R.id.icon_custom_list);
        IconCustomPageAdapter pageAdapter = new IconCustomPageAdapter();
        pageAdapter.setOnPageInflatedListener(this::bindPageContent);
        pageList.setAdapter(pageAdapter);
        pageList.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        setupCouiScrollList(pageList);

        LoadThemeTask loadThemeTask = new LoadThemeTask();
        this.task = loadThemeTask;
        loadThemeTask.execute(new Void[0]);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.home_screen_icon_custom);
        }
    }

    private void bindPageContent(@NonNull View pageRoot) {
        mCurrentProgress = (PrefTools.getFloat(ICON_SIZE_PROGRESS,
                PROGRESS_DEFAULT_VALUE.floatValue(), this) - SIZE_MIN) / SIZE_RANGE;
        mCurrentProgress = Math.max(0f, Math.min(1f, mCurrentProgress));

        IconSizeSettingAdapter iconSizeSettingAdapter = new IconSizeSettingAdapter(this, this.list);
        this.adapter = iconSizeSettingAdapter;
        iconSizeSettingAdapter.setShapeIndex(mCurrentIndex);
        iconSizeSettingAdapter.setProgress(Float.valueOf(mCurrentProgress));

        mPreviewPanel = pageRoot.findViewById(R.id.icon_preview_panel);
        mPreviewWallpaper = pageRoot.findViewById(R.id.icon_preview_wallpaper);
        applyPreviewRoundOutline(mPreviewPanel);

        RecyclerView recyclerView = pageRoot.findViewById(R.id.iconsizeRv);
        this.mRv = recyclerView;
        recyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        recyclerView.setItemAnimator(null);
        this.mRv.setAdapter(this.adapter);
        this.mRv.setLayerType(View.LAYER_TYPE_NONE, null);

        mSizeDefaultLabel = pageRoot.findViewById(R.id.icon_size_default_label);
        mSizeReset = pageRoot.findViewById(R.id.icon_size_reset);
        TextView sizeLabel = pageRoot.findViewById(R.id.icon_size_label);
        TextView shapeLabel = pageRoot.findViewById(R.id.icon_shape_label);
        Typeface regular = Typeface.create("sans-serif", Typeface.NORMAL);
        if (sizeLabel != null) {
            sizeLabel.setTypeface(regular);
        }
        if (shapeLabel != null) {
            shapeLabel.setTypeface(regular);
        }
        if (mSizeDefaultLabel != null) {
            mSizeDefaultLabel.setTypeface(regular);
        }
        mSizeSeekBar = pageRoot.findViewById(R.id.icon_size_seek_bar);
        mSizeSeekBar.setMax(SEEK_MAX);
        int seekProgress = Math.round(mCurrentProgress * SEEK_MAX);
        mSizeSeekBar.setProgress(seekProgress);
        // Oppo: Default only when progress is within ±1 of factory default (62).
        mIconSizeAtDefault = Math.abs(seekProgress - DEFAULT_ICON_SIZE_PROGRESS) <= 1;
        applySizeGuide(mIconSizeAtDefault, /* animate */ false);
        mSizeGuideFirstLoad = false;
        mSizeReset.setOnClickListener(v -> restoreDefaultIconSize());
        mSizeSeekBar.setOnSeekBarChangeListener(new COUISeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(COUISeekBar seekBar, int progress, boolean fromUser) {
                mCurrentProgress = progress / (float) SEEK_MAX;
                if (adapter != null) {
                    adapter.setProgress(Float.valueOf(mCurrentProgress));
                }
                if (mRestoringDefault) {
                    return;
                }
                // Oppo: only user-driven moves leave the Default state.
                // Programmatic setProgress (Restore / init) must not flip to Restore.
                if (fromUser && mIconSizeAtDefault) {
                    mIconSizeAtDefault = false;
                    applySizeGuide(false, true);
                }
            }

            @Override
            public void onStartTrackingTouch(COUISeekBar seekBar) {
                mIsTrackingSeekBar = true;
                // Oppo: finger-down on the default thumb immediately shows Restore.
                if (mIconSizeAtDefault
                        && Math.abs(seekBar.getProgress() - DEFAULT_ICON_SIZE_PROGRESS) <= 1) {
                    mIconSizeAtDefault = false;
                    applySizeGuide(false, true);
                }
            }

            @Override
            public void onStopTrackingTouch(COUISeekBar seekBar) {
                mIsTrackingSeekBar = false;
                // Oppo: Default returns only after release when progress equals factory.
                if (Math.abs(seekBar.getProgress() - DEFAULT_ICON_SIZE_PROGRESS) <= 1) {
                    mCurrentProgress = DEFAULT_SEEKBAR_PROGRESS;
                    mIconSizeAtDefault = true;
                    applySizeGuide(true, true);
                }
            }
        });

        mApplyButton = pageRoot.findViewById(R.id.applyTv);
        mApplyButton.setOnClickListener(v -> {
            if (mApplyButton.getButtonState() == COUILoadingButton.LOADING_STATE) {
                return;
            }
            if (mDefaultIndex != mCurrentIndex) {
                mDefaultIndex = mCurrentIndex;
                Settings.Global.putInt(getContentResolver(), Themes.KEY_THEMED_ICONS, mCurrentIndex);
            }
            mApplyButton.setEnabled(false);
            mApplyButton.switchToLoadingState();
            PrefTools.putFloat(IconSizeSettingActivity.ICON_SIZE_PROGRESS,
                    (mCurrentProgress * SIZE_RANGE) + SIZE_MIN, IconSizeSettingActivity.this);
            Executors.MODEL_EXECUTOR.execute(new OverrideApplyHandler(IconSizeSettingActivity.this));
        });

        mRadioIcon = pageRoot.findViewById(R.id.radioIcon);
        mRadioIcon.setIcons(
                new int[]{R.drawable.icon_mask_default, R.drawable.icon_mask_neighbourhood,
                        R.drawable.icon_mask_droplet, R.drawable.icon_mask_circle},
                new int[]{R.drawable.icon_mask_default_selected,
                        R.drawable.icon_mask_neighbourhood_selected,
                        R.drawable.icon_mask_droplet_selected, R.drawable.icon_mask_circle_selected},
                new int[]{R.string.home_screen_icon_shape_default,
                        R.string.home_screen_icon_shape_neighbourhood,
                        R.string.home_screen_icon_shape_droplet,
                        R.string.home_screen_icon_shape_circle},
                mDefaultIndex, 20, 20);
        mRadioIcon.setOnItemClickListener(index -> {
            mCurrentIndex = index;
            if (adapter != null) {
                adapter.setShapeIndex(index);
            }
        });

        wallpaperTask = new LoadWallpaperTask();
        wallpaperTask.execute();
    }

    private void applyPreviewRoundOutline(@NonNull View panel) {
        final float radius = getResources().getDimension(R.dimen.coloros_card_radius);
        panel.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
        panel.setClipToOutline(true);
    }

    private static final float DEFAULT_SEEKBAR_PROGRESS =
            DEFAULT_ICON_SIZE_PROGRESS / (float) SEEK_MAX;

    private void restoreDefaultIconSize() {
        if (mSizeSeekBar == null) {
            return;
        }
        if (mIconSizeAtDefault
                && Math.abs(mSizeSeekBar.getProgress() - DEFAULT_ICON_SIZE_PROGRESS) <= 1) {
            return;
        }
        // Oppo: cancel physics fling, then move seekbar to factory default (62).
        mSizeSeekBar.stopPhysicsMove();
        mSizeSeekBar.stopClikAnim();
        mRestoringDefault = true;
        mIconSizeAtDefault = true;
        mCurrentProgress = DEFAULT_SEEKBAR_PROGRESS;
        // Non-animated set keeps label/state in sync (animated end callback is fromUser=false
        // and must not be treated as leaving Default).
        mSizeSeekBar.setProgress(DEFAULT_ICON_SIZE_PROGRESS, false, false);
        applySizeGuide(true, true);
        mRestoringDefault = false;
        if (adapter != null) {
            adapter.setProgress(Float.valueOf(mCurrentProgress));
        }
    }

    /**
     * Oppo {@code updateTextViewGuide}: show gray Default, or blue Restore.
     * {@code showDefault=true} → Default; false → Restore.
     */
    private void applySizeGuide(boolean showDefault, boolean animate) {
        if (mSizeDefaultLabel == null || mSizeReset == null) {
            return;
        }
        final TextView showView = showDefault ? mSizeDefaultLabel : mSizeReset;
        final TextView hideView = showDefault ? mSizeReset : mSizeDefaultLabel;

        if (mSizeGuideAnimator != null) {
            mSizeGuideAnimator.cancel();
            mSizeGuideAnimator = null;
        }
        showView.animate().cancel();
        hideView.animate().cancel();
        showView.setLayerType(View.LAYER_TYPE_NONE, null);
        hideView.setLayerType(View.LAYER_TYPE_NONE, null);

        // Keep Restore clickable only while it is the active guide.
        mSizeReset.setClickable(!showDefault);
        mSizeReset.setEnabled(!showDefault);

        if (!animate || mSizeGuideFirstLoad) {
            hideView.setAlpha(1f);
            hideView.setVisibility(View.INVISIBLE);
            showView.setAlpha(1f);
            showView.setVisibility(View.VISIBLE);
            return;
        }

        // Already showing the target — nothing to do.
        if (showView.getVisibility() == View.VISIBLE && showView.getAlpha() >= 0.99f
                && (hideView.getVisibility() != View.VISIBLE || hideView.getAlpha() <= 0.01f)) {
            return;
        }

        // Overlapping crossfade with hardware layers — TextView alpha without a
        // layer redraws glyphs each frame and looks like a blink.
        float hideStart = hideView.getVisibility() == View.VISIBLE ? hideView.getAlpha() : 1f;
        float showStart = showView.getVisibility() == View.VISIBLE ? showView.getAlpha() : 0f;
        hideView.setVisibility(View.VISIBLE);
        hideView.setAlpha(hideStart);
        showView.setVisibility(View.VISIBLE);
        showView.setAlpha(showStart);
        hideView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        showView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        android.animation.ObjectAnimator hideAnim =
                android.animation.ObjectAnimator.ofFloat(hideView, View.ALPHA, hideStart, 0f);
        android.animation.ObjectAnimator showAnim =
                android.animation.ObjectAnimator.ofFloat(showView, View.ALPHA, showStart, 1f);
        hideAnim.setDuration(SIZE_GUIDE_CROSSFADE_MS);
        showAnim.setDuration(SIZE_GUIDE_CROSSFADE_MS);

        android.animation.AnimatorSet set = new android.animation.AnimatorSet();
        set.playTogether(hideAnim, showAnim);
        set.addListener(new android.animation.AnimatorListenerAdapter() {
            private boolean mCanceled;

            @Override
            public void onAnimationCancel(android.animation.Animator animation) {
                mCanceled = true;
            }

            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                hideView.setLayerType(View.LAYER_TYPE_NONE, null);
                showView.setLayerType(View.LAYER_TYPE_NONE, null);
                if (mCanceled) {
                    return;
                }
                hideView.setAlpha(1f);
                hideView.setVisibility(View.INVISIBLE);
                showView.setAlpha(1f);
                showView.setVisibility(View.VISIBLE);
                mSizeGuideAnimator = null;
            }
        });
        mSizeGuideAnimator = set;
        set.start();
    }

    private Bitmap createBlurredWallpaper(Context context) {
        try {
            WallpaperManager wm = WallpaperManager.getInstance(context);
            Drawable wallpaper = wm.getDrawable();
            if (wallpaper == null) {
                wallpaper = wm.getBuiltInDrawable();
            }
            if (wallpaper == null) {
                return null;
            }
            Bitmap source = ImageUtils.drawableToBitmap(wallpaper);
            if (source == null) {
                return null;
            }
            if (source.getConfig() == Bitmap.Config.HARDWARE) {
                source = source.copy(Bitmap.Config.ARGB_8888, false);
            }
            int targetW = Math.max(1, Math.round(source.getWidth() * WALLPAPER_BLUR_SCALE));
            int targetH = Math.max(1, Math.round(source.getHeight() * WALLPAPER_BLUR_SCALE));
            Bitmap scaled = ImageUtils.resizeHxy(source, targetW, targetH);
            if (scaled == null) {
                scaled = Bitmap.createScaledBitmap(source, targetW, targetH, true);
            }
            return ImageUtils.blurBitmap(context, scaled, 20);
        } catch (Exception e) {
            Log.e(TAG, "Failed to build wallpaper blur", e);
            return null;
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
            if (t.componentNameStr == null
                    || !t.componentNameStr.equals("com.android.launcher3/.big.HxyCardLauncher")) {
                customs.add(t);
            }
        }
        return customs;
    }

    class LoadWallpaperTask extends AsyncTask<Void, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(Void... voids) {
            return createBlurredWallpaper(IconSizeSettingActivity.this);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap == null || mPreviewWallpaper == null) {
                return;
            }
            mPreviewWallpaper.setImageDrawable(
                    new BitmapDrawable(getResources(), bitmap));
            if (mPreviewPanel != null) {
                mPreviewPanel.invalidateOutline();
            }
        }
    }

    class LoadThemeTask extends AsyncTask<Void, Void, List<CustomTable>> {
        @Override
        protected List<CustomTable> doInBackground(Void... params) {
            List<CustomTable> customTables = new ArrayList<>();
            for (int attempt = 0; attempt < 20; attempt++) {
                customTables = IconSizeSettingActivity.load(IconSizeSettingActivity.this);
                if (!customTables.isEmpty()) {
                    break;
                }
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            return customTables.size() > 12 ? customTables.subList(0, 12) : customTables;
        }

        @Override
        protected void onPostExecute(List<CustomTable> themeTables) {
            super.onPostExecute(themeTables);
            IconSizeSettingActivity.this.list.addAll(themeTables);
            if (IconSizeSettingActivity.this.adapter != null) {
                IconSizeSettingActivity.this.adapter.notifyDataSetChanged();
                IconSizeSettingActivity.this.adapter.setShapeIndex(mCurrentIndex);
                IconSizeSettingActivity.this.adapter.setProgress(Float.valueOf(mCurrentProgress));
            }
        }
    }

    class OverrideApplyHandler implements Runnable {
        private final Context mContext;

        private OverrideApplyHandler(Context context) {
            mContext = context;
        }

        @Override
        public void run() {
            LauncherAppState.getInstance(mContext).getIconCache().clear();
            try {
                Thread.sleep(PROCESS_KILL_DELAY_MS);
            } catch (Exception e) {
                Log.e(TAG, "Error waiting", e);
            }
            Intent homeIntent = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            mContext.startActivity(homeIntent);
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
        if (wallpaperTask != null && wallpaperTask.getStatus() == AsyncTask.Status.RUNNING) {
            wallpaperTask.cancel(true);
        }
    }
}
