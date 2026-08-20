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
import com.android.launcher3.ColorOsBatchDragManager;
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
import com.coui.appcompat.dialog.COUIAlertDialogBuilder;
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
    private ViewGroup mColorOsSelectionActions;
    private ColorOsPagePreviewStrip mColorOsPagePreviewStrip;
    private View mColorOsGenerateFolder;
    private View mColorOsUninstallSelection;
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
    private boolean mPreviousStatusBarVisible;
    private boolean mColorOsSystemBarsApplied;
    private ColorOsEditMaterialView mColorOsMaterialScrim;
    private View mColorOsStatusInsetScrim;
    private final int[] mTouchPanelLocation = new int[2];
    private boolean mRoutingWorkspaceTouch;
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
        mColorOsSelectionActions = findViewById(R.id.coloros_selection_actions);
        mColorOsPagePreviewStrip = findViewById(R.id.coloros_page_preview_strip);
        mColorOsGenerateFolder = findViewById(R.id.coloros_generate_folder);
        mColorOsUninstallSelection = findViewById(R.id.coloros_uninstall_selection);
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
            ColorOsBatchDragManager batchManager = ColorOsBatchDragManager.get(launcher);
            batchManager.setSelectionListener(this::onColorOsSelectionChanged);
            mColorOsGenerateFolder.setOnClickListener(v -> {
                if (batchManager.generateFolderFromSelection()) {
                    mColorOsPagePreviewStrip.refresh();
                }
            });
            mColorOsUninstallSelection.setOnClickListener(v ->
                    showColorOsUninstallConfirmation(batchManager));
            mEditModeCancel.setOnClickListener(v -> {
                if (mColorOsSelectionActions.getVisibility() == View.VISIBLE) {
                    batchManager.clearSelection();
                } else {
                    showColorOsMainMenu();
                }
            });
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

    private void onColorOsSelectionChanged(int count, boolean canCreateFolder,
            boolean canRemove, int removeLabelRes) {
        if (mColorOsSelectionActions == null || mColorOsLayoutPanel == null) return;
        mColorOsGenerateFolder.setEnabled(canCreateFolder);
        mColorOsGenerateFolder.setAlpha(canCreateFolder ? 1f : 0.3f);
        mColorOsUninstallSelection.setEnabled(canRemove);
        mColorOsUninstallSelection.setAlpha(canRemove ? 1f : 0.3f);
        ((TextView) mColorOsUninstallSelection).setText(removeLabelRes);
        if (count > 0) {
            if (mColorOsSelectionActions.getVisibility() != View.VISIBLE) {
                launcher.getWorkspace().addExtraEmptyScreens();
            }
            mColorOsPagePreviewStrip.refresh();
            mEditModeCancel.setVisibility(View.VISIBLE);
            mEditModeSecondaryTitle.setVisibility(View.VISIBLE);
            ((TextView) mEditModeSecondaryTitle).setText(getResources().getQuantityString(
                    R.plurals.page_preview_title_plurals, count, count));
            mainMenuView.animate().cancel();
            mColorOsSelectionActions.animate().cancel();
            mainMenuView.setVisibility(View.GONE);
            mColorOsLayoutPanel.setVisibility(View.GONE);
            mColorOsSelectionActions.setVisibility(View.VISIBLE);
            mColorOsSelectionActions.setAlpha(1f);
            mColorOsSelectionActions.setTranslationY(0f);
            PathInterpolator previewInterpolator =
                    new PathInterpolator(0.17f, 0f, 0.33f, 1f);
            for (int i = 0; i < mColorOsSelectionActions.getChildCount(); i++) {
                View child = mColorOsSelectionActions.getChildAt(i);
                child.animate().cancel();
                child.setAlpha(0f);
                child.setTranslationY(dp(44f));
                child.animate().alpha(1f).translationY(0f).setDuration(367L)
                        .setInterpolator(previewInterpolator).start();
            }
        } else if (mColorOsLayoutPanel.getVisibility() != View.VISIBLE) {
            launcher.getWorkspace().removeExtraEmptyScreenDelayed(0, false, null);
            mEditModeCancel.setVisibility(View.GONE);
            mEditModeSecondaryTitle.setVisibility(View.GONE);
            ((TextView) mEditModeSecondaryTitle).setText(R.string.edit_mode_layout);
            mColorOsSelectionActions.animate().cancel();
            mColorOsSelectionActions.setVisibility(View.GONE);
            mainMenuView.setVisibility(View.VISIBLE);
            mainMenuView.setAlpha(1f);
            mainMenuView.setTranslationY(0f);
        }
    }

    private void showColorOsUninstallConfirmation(ColorOsBatchDragManager batchManager) {
        int uninstallCount = batchManager.getUninstallableCount();
        int shortcutCount = batchManager.getRemovableShortcutCount();
        int titleRes = uninstallCount > 0 && shortcutCount > 0
                ? R.string.uninstall_and_delete_panel_main_title : 0;
        CharSequence title = titleRes != 0 ? getContext().getString(titleRes)
                : uninstallCount > 0
                        ? getResources().getQuantityString(
                                R.plurals.uninstall_panel_apps_title_plurals,
                                uninstallCount, uninstallCount)
                        : getResources().getQuantityString(
                                R.plurals.delete_icon_panel_main_title_plurals,
                                shortcutCount, shortcutCount);
        androidx.appcompat.app.AlertDialog dialog = new COUIAlertDialogBuilder(launcher)
                .setTitle(title)
                .setMessage(uninstallCount > 0
                        ? R.string.uninstall_selected_apps_message
                        : R.string.remove_selected_shortcuts_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(uninstallCount > 0 && shortcutCount > 0
                                ? R.string.both_uninstall_and_remove_action
                                : shortcutCount > 0
                                        ? R.string.remove_action : R.string.uninstall_action,
                        (dialogInterface, which) ->
                                batchManager.removeOrUninstallSelectedItems())
                .create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
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
        scrim.setBackgroundColor(Color.TRANSPARENT);
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
            com.android.launcher3.ColorOsWorkspaceSelectionController.setEditModeActive(
                    (Launcher) activity, true, true);
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
            ColorOsEditMaterialView materialScrim = new ColorOsEditMaterialView(activity);
            // OPPO applies this material blend to the launcher background surface together with
            // radius-240 compositor blur. MTK has no Oplus background-blur compositor, so retain
            // the exact decoded material color behind Workspace content as the deterministic
            // fallback. Keeping it below Workspace avoids tinting icons and edit controls.
            materialScrim.setAlpha(0f);
            DragLayer.LayoutParams scrimParams = new DragLayer.LayoutParams(
                    BaseDragLayer.LayoutParams.MATCH_PARENT,
                    BaseDragLayer.LayoutParams.MATCH_PARENT);
            scrimParams.gravity = Gravity.FILL;
            scrimParams.ignoreInsets = true;
            dragLayer.addView(materialScrim, 0, scrimParams);
            optionsDialog.mColorOsMaterialScrim = materialScrim;
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
        if (mColorOsMaterialScrim != null) {
            mColorOsMaterialScrim.animate().cancel();
            mColorOsMaterialScrim.setAlpha(0f);
            mColorOsMaterialScrim.animate().alpha(1f).setDuration(317L)
                    .setInterpolator(new PathInterpolator(0.42f, 0f, 0.58f, 1f)).start();
        }
        if (mColorOsStatusInsetScrim != null) {
            mColorOsStatusInsetScrim.animate().cancel();
            mColorOsStatusInsetScrim.setAlpha(0f);
            mColorOsStatusInsetScrim.animate().alpha(1f).setDuration(317L)
                    .setInterpolator(new PathInterpolator(0.42f, 0f, 0.58f, 1f)).start();
        }
        done.animate().cancel();
        done.setAlpha(0f);
        done.setTranslationY(0f);
        menu.setAlpha(1f);
        menu.setTranslationY(0f);

        float itemTranslation = dp(44f);
        PathInterpolator itemInterpolator = new PathInterpolator(0.17f, 0f, 0.33f, 1f);
        for (int i = 0; i < menu.getChildCount(); i++) {
            View item = menu.getChildAt(i);
            item.setAlpha(0f);
            item.setTranslationY(itemTranslation);
            item.animate().alpha(1f).translationY(0f).setDuration(367L)
                    .setStartDelay(i * 17L).setInterpolator(itemInterpolator).start();
        }

        done.postDelayed(() -> {
            done.animate().alpha(1f).setDuration(417L)
                    .setInterpolator(new PathInterpolator(0f, 0f, 0.33f, 1f)).start();
        }, 187L);
    }

    @Override
    protected void handleClose(boolean animate) {
        if (mIsOpen) {
            com.android.launcher3.ColorOsBatchDragManager.get(launcher).clearSelection();
            com.android.launcher3.ColorOsBatchDragManager.get(launcher)
                    .setSelectionListener(null);

            com.android.launcher3.ColorOsWorkspaceSelectionController.setEditModeActive(
                    launcher, false, animate);

            LauncherAppWidgetHostView.applyColorOsEditModeToAll(false);
            if (getResources().getBoolean(R.bool.config_hxy_grid)) {
                Launcher launcher = Launcher.getLauncher(getContext());
                launcher.getWorkspace().mapOverItems((info, view) -> {
                    if (view instanceof com.android.launcher3.OplusBubbleTextView) {
                        ((com.android.launcher3.OplusBubbleTextView) view)
                                .clearColorOsWorkspaceSelection();
                    }
                    return false;
                });
            }
            if (mWidgetEditOverlay != null) {
                mWidgetEditOverlay.hide(animate);
                mWidgetEditOverlay = null;
            }
            if (animate) {
                startColorOsExit();
            } else {
                animate().cancel();
                onClosed();
            }
            mIsOpen = false;
        }
    }

    private void startColorOsExit() {
        Launcher launcher = Launcher.getLauncher(getContext());
        launcher.getStateManager().goToState(NORMAL, true, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                startColorOsExitAnimations();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                // ToggleBarMainState destroys its UI from NORMAL's animation-end callback. Keep
                // our wallpaper-backed compositor fallback alive while doing the same; it is
                // released only after normal Workspace and system bars have each drawn a frame.
                finishColorOsAnimatedClose();
            }
        });
    }

    private void startColorOsExitAnimations() {
        View done = findViewById(R.id.edit_mode_done);
        done.animate().cancel();
        done.animate().alpha(0f).setDuration(200L)
                .setInterpolator(new PathInterpolator(0.3f, 0f, 1f, 1f)).start();

        ViewGroup menu = findViewById(R.id.overview_panel_main_menu);
        float translation = dp(44f);
        PathInterpolator exitInterpolator = new PathInterpolator(0.3f, 0f, 0.1f, 1f);
        int count = menu.getChildCount();
        for (int i = 0; i < count; i++) {
            View item = menu.getChildAt(count - 1 - i);
            item.animate().cancel();
            android.animation.ObjectAnimator alpha =
                    android.animation.ObjectAnimator.ofFloat(item, View.ALPHA, 0f);
            alpha.setDuration(280L);
            alpha.setInterpolator(exitInterpolator);
            android.animation.ObjectAnimator move = android.animation.ObjectAnimator.ofFloat(
                    item, View.TRANSLATION_Y, translation);
            move.setDuration(350L - i * 17L);
            move.setStartDelay(i * 17L);
            move.setInterpolator(exitInterpolator);
            AnimatorSet itemExit = new AnimatorSet();
            itemExit.playTogether(alpha, move);
            itemExit.start();
        }

        View scrim = findViewById(R.id.edit_mode_scrim);
        scrim.animate().cancel();
        scrim.animate().alpha(0f).setDuration(317L)
                .setInterpolator(new PathInterpolator(0.42f, 0f, 0.58f, 1f)).start();
        if (mColorOsMaterialScrim != null) {
            // OPPO animates ToggleBar's independent blur channel from 1 to 0 as the state returns
            // to NORMAL. Never fade the backing surface itself: doing so exposes the transparent
            // Launcher window before WallpaperService has committed its replacement frame.
            mColorOsMaterialScrim.animateToNormal(320L,
                    new PathInterpolator(0.42f, 0f, 0.58f, 1f));
        }
        if (mColorOsStatusInsetScrim != null) {
            mColorOsStatusInsetScrim.animate().cancel();
            mColorOsStatusInsetScrim.animate().alpha(0f).setDuration(317L)
                    .setInterpolator(new PathInterpolator(0.42f, 0f, 0.58f, 1f)).start();
        }
    }

    private void onClosed() {
        restoreColorOsSystemBars();
        mActivity.getDragLayer().removeView(this);
        if (mColorOsMaterialScrim != null) {
            mActivity.getDragLayer().removeView(mColorOsMaterialScrim);
            mColorOsMaterialScrim = null;
        }
        if (mColorOsStatusInsetScrim != null) {
            mActivity.getDragLayer().removeView(mColorOsStatusInsetScrim);
            mColorOsStatusInsetScrim = null;
        }
        if (mOnDismissed != null) {
            mOnDismissed.run();
        }
    }

    private void finishColorOsAnimatedClose() {
        BaseDragLayer dragLayer = mActivity.getDragLayer();
        ColorOsEditMaterialView material = mColorOsMaterialScrim;
        mColorOsMaterialScrim = null;

        // Equivalent to ToggleBarManager.finish(): destroy the panel only after NORMAL completes.
        dragLayer.removeView(this);
        if (mColorOsStatusInsetScrim != null) {
            dragLayer.removeView(mColorOsStatusInsetScrim);
            mColorOsStatusInsetScrim = null;
        }
        if (mOnDismissed != null) {
            mOnDismissed.run();
        }

        if (material == null) {
            restoreColorOsSystemBars();
            return;
        }

        // The Oplus implementation synchronizes blur removal with its surface transaction. MTK
        // lacks that compositor API, so retain the now-unblurred wallpaper bitmap for the normal
        // Workspace draw, restore bars without their framework fade, then release it one draw
        // later. At no point is there an uncovered transparent-window frame.
        dragLayer.postOnAnimation(() -> {
            restoreColorOsSystemBars();
            dragLayer.postOnAnimation(() -> {
                if (material.getParent() == dragLayer) {
                    dragLayer.removeView(material);
                }
            });
        });
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
        android.view.WindowInsets rootInsets =
                launcher.getWindow().getDecorView().getRootWindowInsets();
        mPreviousStatusBarVisible = rootInsets == null
                || rootInsets.isVisible(android.view.WindowInsets.Type.statusBars());
        // ToggleBar uses one edge-to-edge blurred launcher surface behind both system bars.
        launcher.getWindow().setStatusBarColor(Color.TRANSPARENT);
        launcher.getWindow().setNavigationBarColor(Color.TRANSPARENT);
        launcher.getWindow().setNavigationBarDividerColor(Color.TRANSPARENT);
        launcher.getWindow().setNavigationBarContrastEnforced(false);
        android.view.WindowInsetsController insetsController =
                launcher.getWindow().getDecorView().getWindowInsetsController();
        if (insetsController != null) {
            setSystemBarAnimationsDisabled(insetsController, true);
            insetsController.hide(android.view.WindowInsets.Type.statusBars());
            setSystemBarAnimationsDisabled(insetsController, false);
        }
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
        android.view.WindowInsetsController insetsController =
                launcher.getWindow().getDecorView().getWindowInsetsController();
        if (mPreviousStatusBarVisible && insetsController != null) {
            setSystemBarAnimationsDisabled(insetsController, true);
            insetsController.show(android.view.WindowInsets.Type.statusBars());
            setSystemBarAnimationsDisabled(insetsController, false);
        }
        launcher.getSystemUiController().updateUiState(
                com.android.launcher3.util.SystemUiController.UI_STATE_WIDGET_BOTTOM_SHEET, 0);
        mColorOsSystemBarsApplied = false;
    }


    private static void setSystemBarAnimationsDisabled(
            android.view.WindowInsetsController controller, boolean disabled) {
        try {
            controller.getClass().getMethod("setAnimationsDisabled", boolean.class)
                    .invoke(controller, disabled);
        } catch (ReflectiveOperationException ignored) {
            // Oplus/platform extension; hide() remains functional on plain Android.
        }
    }

    @Override
    protected boolean isOfType(int type) {
        return (type & TYPE_OPTIONS_POPUP_DIALOG) != 0;
    }
    /**
     * OPPO's ToggleBarRootView is not laid over Workspace. This compatibility view is, so map
     * gestures above the active panel back into Workspace using its real animated transform.
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (getResources().getBoolean(R.bool.config_hxy_grid)) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                mRoutingWorkspaceTouch = isInWorkspaceTouchRegion(event);
            }
            if (mRoutingWorkspaceTouch) {
                Launcher currentLauncher = Launcher.getLauncher(getContext());
                currentLauncher.setColorOsWorkspaceTouchInProgress(true);
                View dragLayer = currentLauncher.getDragLayer();
                int[] rootLocation = new int[2];
                int[] dragLayerLocation = new int[2];
                getLocationOnScreen(rootLocation);
                dragLayer.getLocationOnScreen(dragLayerLocation);
                float[] point = new float[] {
                        rootLocation[0] + event.getX() - dragLayerLocation[0],
                        rootLocation[1] + event.getY() - dragLayerLocation[1]
                };
                if (currentLauncher.getDragController().isDragging()) {
                    // Starting a long-press drag cancels the source child gesture. Because this
                    // compatibility overlay owns that same stream, it receives the synthetic
                    // zero-distance CANCEL as well. OPPO's separate ToggleBarRootView does not;
                    // keep the stream alive so the following MOVE/UP events perform the drop.
                    if (action == MotionEvent.ACTION_CANCEL
                            && currentLauncher.getDragController().getDistanceDragged() == 0f) {
                        return true;
                    }
                    MotionEvent dragEvent = MotionEvent.obtain(event);
                    dragEvent.setLocation(point[0], point[1]);
                    currentLauncher.getDragController().onControllerTouchEvent(dragEvent);
                    dragEvent.recycle();
                    if (action == MotionEvent.ACTION_UP
                            || action == MotionEvent.ACTION_CANCEL) {
                        mRoutingWorkspaceTouch = false;
                        currentLauncher.setColorOsWorkspaceTouchInProgress(false);
                    }
                    return true;
                }
                com.android.launcher3.Utilities.mapCoordInSelfToDescendant(
                        currentLauncher.getWorkspace(), dragLayer, point);
                MotionEvent mapped = MotionEvent.obtain(event);
                mapped.setLocation(point[0], point[1]);
                currentLauncher.getWorkspace().dispatchTouchEvent(mapped);
                mapped.recycle();
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    currentLauncher.setColorOsWorkspaceTouchInProgress(false);
                    mRoutingWorkspaceTouch = false;
                }
                // Retain gesture ownership so all events in the sequence reach Workspace.
                return true;
            }
        }
        return super.dispatchTouchEvent(event);
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
        if (isTouchInsideView(event, mEditModeDone)
                || isTouchInsideView(event, mEditModeCancel)) {
            return false;
        }
        View activePanel;
        if (mColorOsLayoutPanel != null
                && mColorOsLayoutPanel.getVisibility() == View.VISIBLE) {
            activePanel = mColorOsLayoutPanel;
        } else if (mColorOsSelectionActions != null
                && mColorOsSelectionActions.getVisibility() == View.VISIBLE) {
            activePanel = mColorOsSelectionActions;
        } else {
            activePanel = mainMenuView;
        }
        if (activePanel == null || activePanel.getVisibility() != View.VISIBLE) {
            return false;
        }
        activePanel.getLocationOnScreen(mTouchPanelLocation);
        getLocationOnScreen(mTouchRootLocation);
        return event.getY() < mTouchPanelLocation[1] - mTouchRootLocation[1];
    }

    private boolean isTouchInsideView(MotionEvent event, View view) {
        if (view == null || view.getVisibility() != View.VISIBLE) {
            return false;
        }
        view.getLocationOnScreen(mTouchPanelLocation);
        getLocationOnScreen(mTouchRootLocation);
        float screenX = mTouchRootLocation[0] + event.getX();
        float screenY = mTouchRootLocation[1] + event.getY();
        return screenX >= mTouchPanelLocation[0]
                && screenX < mTouchPanelLocation[0] + view.getWidth()
                && screenY >= mTouchPanelLocation[1]
                && screenY < mTouchPanelLocation[1] + view.getHeight();
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
