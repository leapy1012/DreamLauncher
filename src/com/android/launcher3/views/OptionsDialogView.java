package com.android.launcher3.views;


import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.LayoutAnimationController;
import android.view.animation.PathInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import android.graphics.Paint;
import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.logging.StatsLogManager.EventEnum;
import com.android.launcher3.model.WidgetsModel;
import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.android.launcher3.states.ColorOsWorkspaceEditTransition;

import java.util.ArrayList;
import java.util.List;
import android.view.Gravity;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.testing.TestLogging;
import com.android.launcher3.testing.shared.TestProtocol;
import com.android.launcher3.widget.LauncherAppWidgetHostView;
import com.android.launcher3.widget.picker.WidgetsFullSheet;
import com.android.launcher3.util.PackageManagerHelper;
import static com.android.launcher3.LauncherState.NORMAL;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.screenedit.ScrollEffectAdapter;
import com.android.launcher3.screenedit.GridGallery;
import com.android.launcher3.screenedit.OverviewPanelStateTransAnimation;
import com.android.launcher3.screenedit.GridGalleryAdapter;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import java.util.HashMap;
import com.coui.appcompat.couiswitch.COUISwitch;
import com.coui.appcompat.animation.dynamicanimation.COUIDynamicAnimation;
import com.coui.appcompat.animation.dynamicanimation.COUISpringAnimation;
import com.coui.appcompat.animation.dynamicanimation.COUISpringForce;

public class OptionsDialogView extends AbstractFloatingView {

    // An intent extra to indicate the horizontal scroll of the wallpaper.
    private static final String EXTRA_WALLPAPER_OFFSET = "com.android.launcher3.WALLPAPER_OFFSET";
    private static final String EXTRA_WALLPAPER_FLAVOR = "com.android.launcher3.WALLPAPER_FLAVOR";
    // An intent extra to indicate the launch source by launcher.
    private static final String EXTRA_WALLPAPER_LAUNCH_SOURCE =
            "com.android.wallpaper.LAUNCH_SOURCE";

    private static final long HIDE_DURATION_MS = 180;
    private final ActivityContext mActivity;
    private Runnable mOnDismissed;
    // 启动器实例
    private Launcher launcher;
    // 滚动特效适配器
    private ScrollEffectAdapter scrollEffectAdapter;
    // 主菜单视图
    private View mainMenuView;
    private View mColorOsLayoutPanel;
    private View mEditModeCancel;
    private View mEditModeSecondaryTitle;
    private TextView mEditModeDone;
    private COUISwitch mHideNamesSwitch;
    private String mPendingGridName;
    private boolean mPendingHideIconNames;
    private ColorOsWidgetEditOverlay mWidgetEditOverlay;
    // 特效网格画廊
    private GridGallery effectGridGallery;
    // 动画集合
    private AnimatorSet animationSet;
    private int mPreviousStatusBarColor;
    private int mPreviousNavigationBarColor;
    private int mPreviousNavigationBarDividerColor;
    private boolean mPreviousNavigationBarContrastEnforced;
    private boolean mColorOsSystemBarsApplied;
    private final int[] mTouchPanelLocation = new int[2];
    private final int[] mTouchRootLocation = new int[2];
    // 概览面板状态转换动画
    private OverviewPanelStateTransAnimation stateTransAnimation;
    // 当前面板状态
    private State currentState;
    public enum State {
        NONE,
        MAIN_MENU,
        EFFECTS,
        WIDGET_LIST_PACKAGE,
        WIDGETS
    }
    
    public OptionsDialogView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OptionsDialogView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mActivity = ActivityContext.lookupContext(context);
        inflate(context, R.layout.options_menu_layout, this);
        findViewById(R.id.theme_button).setOnClickListener(v -> {
            startThemePicker(v);
            resetState();
        });
        findViewById(R.id.wallpaper_button).setOnClickListener(v -> {
            startPersonalPicker(v);
            resetState();
        });
        findViewById(R.id.widget_button).setOnClickListener(v -> {
            onWidgetsClicked(v);
        });
        findViewById(R.id.effect_button).setOnClickListener(v -> {
            switchToEffectsState();
        });
        findViewById(R.id.layout_button).setOnClickListener(v -> {
            showColorOsLayoutPanel();
        });
        findViewById(R.id.settings_button).setOnClickListener(v -> {
            startSettings(v);
            resetState();
        });
        mainMenuView = findViewById(R.id.overview_panel_main_menu);
        mColorOsLayoutPanel = findViewById(R.id.coloros_layout_panel);
        effectGridGallery = findViewById(R.id.effect_gallery);
        currentState = State.MAIN_MENU;
        launcher = Launcher.getLauncher(context);
        final boolean colorOsEditMode = getResources().getBoolean(R.bool.config_hxy_grid);
        View scrim = findViewById(R.id.edit_mode_scrim);
        View done = findViewById(R.id.edit_mode_done);
        mEditModeDone = (TextView) done;
        mEditModeCancel = findViewById(R.id.edit_mode_cancel);
        mEditModeSecondaryTitle = findViewById(R.id.edit_mode_secondary_title);
        mHideNamesSwitch = findViewById(R.id.coloros_hide_names_switch);
        scrim.setVisibility(colorOsEditMode ? View.VISIBLE : View.GONE);
        if (colorOsEditMode) {
            configureColorOsFallbackScrim(scrim);
        }
        done.setVisibility(colorOsEditMode ? View.VISIBLE : View.GONE);
        if (done != null) {
            done.setOnClickListener(v -> {
                if (mColorOsLayoutPanel.getVisibility() == View.VISIBLE) {
                    applyColorOsLayout();
                } else {
                    close(true);
                }
            });
        }
        if (colorOsEditMode) {
            findViewById(R.id.edit_mode_container).getLayoutParams().height =
                    ViewGroup.LayoutParams.MATCH_PARENT;
            ColorOsGridPreviewView grid4x6 = findViewById(R.id.coloros_grid_4x6_preview);
            ColorOsGridPreviewView grid5x6 = findViewById(R.id.coloros_grid_5x6_preview);
            ColorOsGridPreviewView grid5x7 = findViewById(R.id.coloros_grid_5x7_preview);
            grid4x6.setGridSize(4, 6);
            grid5x6.setGridSize(5, 6);
            grid5x7.setGridSize(5, 7);
            findViewById(R.id.coloros_grid_4x6).setOnClickListener(
                    v -> selectColorOsGrid("4_by_6"));
            findViewById(R.id.coloros_grid_5x6).setOnClickListener(
                    v -> selectColorOsGrid("5_by_6"));
            findViewById(R.id.coloros_grid_5x7).setOnClickListener(
                    v -> selectColorOsGrid("5_by_7"));
            mEditModeCancel.setOnClickListener(v -> showColorOsMainMenu());
            mHideNamesSwitch.setOnCheckedChangeListener((button, checked) ->
                    mPendingHideIconNames = checked);
        }
        stateTransAnimation = new OverviewPanelStateTransAnimation(launcher, this);
        scrollEffectAdapter = new ScrollEffectAdapter(context);
        android.util.Log.d("zr_overPanel", " OverviewPanel ");
    }

    public final void switchToEffectsState() {
        GridGalleryAdapter adapter = effectGridGallery.getAdapter();
        if (adapter != scrollEffectAdapter) {
            effectGridGallery.setAdapter(scrollEffectAdapter);
        }
        switchPanelState(State.EFFECTS, false);
    }

    public final void switchToMainMenuState() {
        if (mColorOsLayoutPanel != null && mColorOsLayoutPanel.getVisibility() == View.VISIBLE) {
            showColorOsMainMenu();
            return;
        }
        switchPanelState(State.MAIN_MENU, false);
    }

    private void showColorOsLayoutPanel() {
        mPendingGridName = InvariantDeviceProfile.getCurrentGridName(getContext());
        mPendingHideIconNames = LauncherPrefs.getPrefs(getContext())
                .getBoolean("coloros_hide_icon_names", false);
        mHideNamesSwitch.setChecked(mPendingHideIconNames, false);
        updateGridSelection();
        mEditModeCancel.setVisibility(View.VISIBLE);
        mEditModeSecondaryTitle.setVisibility(View.VISIBLE);
        mEditModeDone.setText(R.string.edit_mode_apply);
        mColorOsLayoutPanel.setAlpha(0f);
        mColorOsLayoutPanel.setTranslationY(dp(20));
        mainMenuView.animate().cancel();
        mainMenuView.animate()
                .alpha(0f)
                .translationY(dp(18))
                .setDuration(160)
                .withEndAction(() -> {
                    mainMenuView.setVisibility(View.GONE);
                    mColorOsLayoutPanel.setVisibility(View.VISIBLE);
                    mColorOsLayoutPanel.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(240)
                            .setInterpolator(Interpolators.EMPHASIZED_DECELERATE)
                            .start();
                })
                .start();
    }

    private void showColorOsMainMenu() {
        mEditModeCancel.setVisibility(View.GONE);
        mEditModeSecondaryTitle.setVisibility(View.GONE);
        mEditModeDone.setText(R.string.edit_mode_done);
        mColorOsLayoutPanel.animate().cancel();
        mColorOsLayoutPanel.animate()
                .alpha(0f)
                .translationY(dp(18))
                .setDuration(150)
                .withEndAction(() -> {
                    mColorOsLayoutPanel.setVisibility(View.GONE);
                    mainMenuView.setVisibility(View.VISIBLE);
                    mainMenuView.setAlpha(0f);
                    mainMenuView.setTranslationY(dp(18));
                    mainMenuView.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(240)
                            .setInterpolator(Interpolators.EMPHASIZED_DECELERATE)
                            .start();
                })
                .start();
    }

    private void updateGridSelection() {
        ((ColorOsGridPreviewView) findViewById(R.id.coloros_grid_4x6_preview))
                .setChecked("4_by_6".equals(mPendingGridName));
        ((ColorOsGridPreviewView) findViewById(R.id.coloros_grid_5x6_preview))
                .setChecked("5_by_6".equals(mPendingGridName));
        ((ColorOsGridPreviewView) findViewById(R.id.coloros_grid_5x7_preview))
                .setChecked("5_by_7".equals(mPendingGridName));
    }

    private void selectColorOsGrid(String gridName) {
        mPendingGridName = gridName;
        updateGridSelection();
    }

    private void applyColorOsLayout() {
        LauncherPrefs.getPrefs(getContext()).edit()
                .putBoolean("coloros_hide_icon_names", mPendingHideIconNames).apply();
        launcher.getWorkspace().mapOverItems((info, view) -> {
            boolean showLabel = !mPendingHideIconNames
                    && info.container != LauncherSettings.Favorites.CONTAINER_HOTSEAT
                    && info.container != LauncherSettings.Favorites.CONTAINER_HOTSEAT_PREDICTION;
            if (view instanceof com.android.launcher3.BubbleTextView) {
                ((com.android.launcher3.BubbleTextView) view).setTextVisibility(showLabel);
            } else if (view instanceof com.android.launcher3.folder.FolderIcon) {
                ((com.android.launcher3.folder.FolderIcon) view).setTextVisibility(showLabel);
            }
            return false;
        });
        if (!mPendingGridName.equals(InvariantDeviceProfile.getCurrentGridName(getContext()))) {
            InvariantDeviceProfile.INSTANCE.get(getContext()).setCurrentGrid(
                    getContext(), mPendingGridName);
        }
        close(true);
    }

    private void configureColorOsFallbackScrim(View scrim) {
        int color = Color.TRANSPARENT;
        if (!com.android.systemui.shared.system.BlurUtils.supportsBlursOnWindows()) {
            boolean bright = false;
            android.app.WallpaperColors colors = android.app.WallpaperManager.getInstance(
                    getContext()).getWallpaperColors(android.app.WallpaperManager.FLAG_SYSTEM);
            if (colors != null && colors.getPrimaryColor() != null) {
                bright = androidx.core.graphics.ColorUtils.calculateLuminance(
                        colors.getPrimaryColor().toArgb()) > 0.5;
            }
            // ToggleBarState returns root alpha 26 when blur is unavailable. OPPO chooses the
            // same black/white drawable from the current wallpaper brightness.
            color = bright ? Color.argb(26, 255, 255, 255) : Color.argb(26, 0, 0, 0);
        }
        scrim.setBackgroundColor(color);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    public final void switchPanelState(State newState, boolean withAnimation) {
        final HashMap<View, Integer> hashMap = new HashMap<>();
        Animator animation = getAnimationToState(newState, withAnimation, hashMap);
        if (withAnimation) {
            final AnimatorSet animatorSet = OverviewPanelStateTransAnimation.createAnimatorSet();
            this.animationSet = animatorSet;
            if (animation != null) {
                animatorSet.play(animation);
            }
            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    for (View view : hashMap.keySet()) {
                        if (((Integer) hashMap.get(view)).intValue() == 1) {
                            view.setLayerType(View.LAYER_TYPE_NONE, (Paint) null);
                        }
                    }
                    resetAnimationSet();
                }
            });
            post(new Runnable() {
                public void run() {
                    if (OptionsDialogView.this.animationSet == animatorSet) {
                        for (View view : hashMap.keySet()) {
                            if (((Integer) hashMap.get(view)).intValue() == 1) {
                                view.setLayerType(View.LAYER_TYPE_HARDWARE, (Paint) null);
                            }
                            if (ViewCompat.isAttachedToWindow(view)) {
                                view.buildLayer();
                            }
                        }
                        animatorSet.start();
                    }
                }
            });
        }
    }

    public final Animator getAnimationToState(State newState, boolean withAnimation, HashMap<View, Integer> hashMap) {
        AnimatorSet animationToState = stateTransAnimation.getAnimationToState(currentState, newState, withAnimation, hashMap);
        currentState = newState;
        return animationToState;
    }

    public void resetAnimationSet() {
        this.animationSet = null;
    }

    public static <T extends Context & ActivityContext> void show(T activity, Runnable onDismissed) {
        show(activity, onDismissed, null);
    }

    public static <T extends Context & ActivityContext> void show(T activity, Runnable onDismissed, @Nullable Runnable onActionClicked) {
        closeOpenViews(activity, true, TYPE_OPTIONS_POPUP_DIALOG);
        OptionsDialogView optionsDialog = new OptionsDialogView(activity, null);
        if (activity.getResources().getBoolean(R.bool.config_hxy_grid)
                && activity instanceof Launcher) {
            LauncherAppWidgetHostView.applyColorOsEditModeToAll(true);
            ColorOsWorkspaceEditTransition.prepareWorkspacePivot((Launcher) activity);
            ((Launcher) activity).getStateManager().goToState(
                    com.android.launcher3.LauncherState.EDIT_MODE);
        }
        optionsDialog.mIsOpen = true;
        optionsDialog.mOnDismissed = onDismissed;
        BaseDragLayer dragLayer = activity.getDragLayer();
        dragLayer.addView(optionsDialog);
        DragLayer.LayoutParams params = (DragLayer.LayoutParams) optionsDialog.getLayoutParams();
        params.width = BaseDragLayer.LayoutParams.MATCH_PARENT;
        params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        if (activity.getResources().getBoolean(R.bool.config_hxy_grid)) {
            optionsDialog.applyColorOsSystemBars();
            params.height = BaseDragLayer.LayoutParams.MATCH_PARENT;
            params.gravity = Gravity.FILL;
            optionsDialog.setLayoutParams(params);
            optionsDialog.setAlpha(1f);
            optionsDialog.setTranslationY(0f);
            optionsDialog.startColorOsEntrance();
            optionsDialog.mWidgetEditOverlay = new ColorOsWidgetEditOverlay((Launcher) activity);
            optionsDialog.mWidgetEditOverlay.show();
        }
    }

    private void startColorOsEntrance() {
        final View scrim = findViewById(R.id.edit_mode_scrim);
        final View done = findViewById(R.id.edit_mode_done);
        final ViewGroup menu = findViewById(R.id.overview_panel_main_menu);
        scrim.animate().cancel();
        scrim.setAlpha(0f);
        scrim.animate().alpha(1f).setDuration(317L)
                .setInterpolator(new PathInterpolator(0.42f, 0f, 0.58f, 1f)).start();
        done.animate().cancel();
        done.setAlpha(0f);
        done.setTranslationY(0f);
        menu.setAlpha(1f);
        menu.setTranslationY(0f);

        android.view.animation.AnimationSet itemAnimation =
                new android.view.animation.AnimationSet(true);
        TranslateAnimation translation = new TranslateAnimation(
                TranslateAnimation.RELATIVE_TO_SELF, 0f,
                TranslateAnimation.RELATIVE_TO_SELF, 0f,
                TranslateAnimation.RELATIVE_TO_SELF, 1f,
                TranslateAnimation.RELATIVE_TO_SELF, 0f);
        AlphaAnimation alpha = new AlphaAnimation(0f, 1f);
        itemAnimation.addAnimation(translation);
        itemAnimation.addAnimation(alpha);
        itemAnimation.setDuration(367L);
        itemAnimation.setInterpolator(new PathInterpolator(0.17f, 0f, 0.33f, 1f));
        LayoutAnimationController controller = new LayoutAnimationController(itemAnimation, 0.046f);
        controller.setOrder(LayoutAnimationController.ORDER_NORMAL);
        menu.setLayoutAnimation(controller);
        menu.post(menu::startLayoutAnimation);

        done.postDelayed(() -> {
            COUISpringForce force = new COUISpringForce(1f).setResponse(0.3f).setBounce(0f);
            new COUISpringAnimation(done, COUIDynamicAnimation.ALPHA, 1f)
                    .setMinimumVisibleChange(COUIDynamicAnimation.MIN_VISIBLE_CHANGE_ALPHA)
                    .setSpring(force)
                    .start();
        }, 187L);
    }

    @Override
    protected void handleClose(boolean animate) {
        if (mIsOpen) {
            LauncherAppWidgetHostView.applyColorOsEditModeToAll(false);
            if (mWidgetEditOverlay != null) {
                mWidgetEditOverlay.hide(animate);
                mWidgetEditOverlay = null;
            }
            if (animate) {
                animate().alpha(0f)
                        .withLayer()
                        .setStartDelay(0)
                        .setDuration(HIDE_DURATION_MS)
                        .setInterpolator(Interpolators.ACCEL)
                        .withEndAction(this::onClosed)
                        .start();
            } else {
                animate().cancel();
                onClosed();
            }
            mIsOpen = false;
        }
    }

    private void onClosed() {
        restoreColorOsSystemBars();
        mActivity.getDragLayer().removeView(this);
        if (mOnDismissed != null) {
            mOnDismissed.run();
        }
        Launcher launcher = Launcher.getLauncher(this.getContext());
        launcher.getStateManager().goToState(NORMAL);
    }

    private void applyColorOsSystemBars() {
        if (mColorOsSystemBarsApplied) {
            return;
        }
        Launcher launcher = Launcher.getLauncher(getContext());
        mPreviousStatusBarColor = launcher.getWindow().getStatusBarColor();
        mPreviousNavigationBarColor = launcher.getWindow().getNavigationBarColor();
        mPreviousNavigationBarDividerColor =
                launcher.getWindow().getNavigationBarDividerColor();
        mPreviousNavigationBarContrastEnforced =
                launcher.getWindow().isNavigationBarContrastEnforced();
        // ToggleBar uses one edge-to-edge blurred launcher surface behind both system bars.
        launcher.getWindow().setStatusBarColor(Color.TRANSPARENT);
        launcher.getWindow().setNavigationBarColor(Color.TRANSPARENT);
        launcher.getWindow().setNavigationBarDividerColor(Color.TRANSPARENT);
        launcher.getWindow().setNavigationBarContrastEnforced(false);
        launcher.getSystemUiController().updateUiState(
                com.android.launcher3.util.SystemUiController.UI_STATE_WIDGET_BOTTOM_SHEET, 0);
        mColorOsSystemBarsApplied = true;
    }

    private void restoreColorOsSystemBars() {
        if (!mColorOsSystemBarsApplied) {
            return;
        }
        Launcher launcher = Launcher.getLauncher(getContext());
        launcher.getWindow().setStatusBarColor(mPreviousStatusBarColor);
        launcher.getWindow().setNavigationBarColor(mPreviousNavigationBarColor);
        launcher.getWindow().setNavigationBarDividerColor(mPreviousNavigationBarDividerColor);
        launcher.getWindow().setNavigationBarContrastEnforced(
                mPreviousNavigationBarContrastEnforced);
        launcher.getSystemUiController().updateUiState(
                com.android.launcher3.util.SystemUiController.UI_STATE_WIDGET_BOTTOM_SHEET, 0);
        mColorOsSystemBarsApplied = false;
    }


    @Override
    protected boolean isOfType(int type) {
        return (type & TYPE_OPTIONS_POPUP_DIALOG) != 0;
    }

    /**
     * OPPO's ToggleBarRootView occupies only the bottom panel, leaving Workspace as the native
     * touch target above it. Our compatibility view is full-screen so it can host the scrim and
     * toolbar; do not let AbstractFloatingView consume that transparent workspace region.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (getResources().getBoolean(R.bool.config_hxy_grid)
                && isInWorkspaceTouchRegion(event)) {
            return false;
        }
        return super.onTouchEvent(event);
    }

    private boolean isInWorkspaceTouchRegion(MotionEvent event) {
        View activePanel = mColorOsLayoutPanel != null
                && mColorOsLayoutPanel.getVisibility() == View.VISIBLE
                ? mColorOsLayoutPanel : mainMenuView;
        if (activePanel == null || activePanel.getVisibility() != View.VISIBLE) {
            return false;
        }
        activePanel.getLocationOnScreen(mTouchPanelLocation);
        getLocationOnScreen(mTouchRootLocation);
        return event.getY() < mTouchPanelLocation[1] - mTouchRootLocation[1];
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        return false;
    }

    private static boolean onWidgetsClicked(View view) {
        Launcher launcher = Launcher.getLauncher(view.getContext());
        if (launcher.getResources().getBoolean(R.bool.config_hxy_grid)) {
            AbstractFloatingView.closeAllOpenViews(launcher, false);
            // Closing edit mode also finishes its state transition. Attach the widget hub on the
            // next frame so that transition cleanup cannot remove the newly-added floating view.
            launcher.getDragLayer().postDelayed(() -> ColorOsWidgetHubView.show(launcher), 400L);
            return true;
        }
        return openWidgets(launcher) != null;
    }

     /** Returns WidgetsFullSheet that was opened, or null if nothing was opened. */
     @Nullable
     public static WidgetsFullSheet openWidgets(Launcher launcher) {
         if (launcher.getPackageManager().isSafeMode()) {
             Toast.makeText(launcher, R.string.safemode_widget_error, Toast.LENGTH_SHORT).show();
             return null;
         } else {
             AbstractFloatingView floatingView = AbstractFloatingView.getTopOpenViewWithType(
                     launcher, TYPE_WIDGETS_FULL_SHEET);
             if (floatingView != null) {
                 return (WidgetsFullSheet) floatingView;
             }
             return WidgetsFullSheet.show(launcher, true /* animated */);
         }
     }

    private static boolean startSettings(View view) {
        TestLogging.recordEvent(TestProtocol.SEQUENCE_MAIN, "start: startSettings");
        Launcher launcher = Launcher.getLauncher(view.getContext());
        launcher.startActivity(new Intent(Intent.ACTION_APPLICATION_PREFERENCES)
                .setPackage(launcher.getPackageName())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        return true;
    }

    public View getEffectsView() {
        return effectGridGallery;
    }

    public View getMainMenu() {
        return mainMenuView;
    }

    private static boolean startThemePicker(View v) {
        Launcher launcher = Launcher.getLauncher(v.getContext());
        String pickerAction = launcher.getString(R.string.theme_picker_class);
        if (!TextUtils.isEmpty(pickerAction)) {
            Intent intent = new Intent(pickerAction)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            return launcher.startActivitySafely(v, intent, placeholderInfo(intent)) != null;
        } else {
            return false;
        }
    }

    private static boolean startPersonalPicker(View v) {
        Launcher launcher = Launcher.getLauncher(v.getContext());
        String personalAction = launcher.getString(R.string.personal_picker_class);
        if (!TextUtils.isEmpty(personalAction)) {
            Intent intent = new Intent(personalAction)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            return launcher.startActivitySafely(v, intent, placeholderInfo(intent)) != null;
        } else {
            return false;
        }
    }

    /**
     * Event handler for the wallpaper picker button that appears after a long press
     * on the home screen.
     */
    private static boolean startWallpaperPicker(View v) {
        Launcher launcher = Launcher.getLauncher(v.getContext());
        if (!Utilities.isWallpaperAllowed(launcher)) {
            String message = launcher.getStringCache() != null
                    ? launcher.getStringCache().disabledByAdminMessage
                    : launcher.getString(R.string.msg_disabled_by_admin);
            Toast.makeText(launcher, message, Toast.LENGTH_SHORT).show();
            return false;
        }
        Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra(EXTRA_WALLPAPER_OFFSET,
                        launcher.getWorkspace().getWallpaperOffsetForCenterPage())
                .putExtra(EXTRA_WALLPAPER_LAUNCH_SOURCE, "app_launched_launcher");
        if (!styleWallpapersExists(launcher)) {
            intent.putExtra(EXTRA_WALLPAPER_FLAVOR, "wallpaper_only");
        } else {
            intent.putExtra(EXTRA_WALLPAPER_FLAVOR, "focus_wallpaper");
        }
        String pickerPackage = launcher.getString(R.string.wallpaper_picker_package);
        if (!TextUtils.isEmpty(pickerPackage)) {
            intent.setPackage(pickerPackage);
        }
        return launcher.startActivitySafely(v, intent, placeholderInfo(intent)) != null;
    }

    private static boolean styleWallpapersExists(Context context) {
        return context.getPackageManager().resolveActivity(
                PackageManagerHelper.getStyleWallpapersIntent(context), 0) != null;
    }

    static WorkspaceItemInfo placeholderInfo(Intent intent) {
        WorkspaceItemInfo placeholderInfo = new WorkspaceItemInfo();
        placeholderInfo.intent = intent;
        placeholderInfo.itemType = LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT;
        placeholderInfo.container = LauncherSettings.Favorites.CONTAINER_SETTINGS;
        return placeholderInfo;
    }

    private void resetState() {
        handleClose(false);
    }

    @Override
    public void onBackInvoked() {
        if (mColorOsLayoutPanel != null && mColorOsLayoutPanel.getVisibility() == View.VISIBLE) {
            showColorOsMainMenu();
        } else if (currentState != State.EFFECTS) {
            close(true);
        } else {
            switchToMainMenuState();
        }
    }
}
