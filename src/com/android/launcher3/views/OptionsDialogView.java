package com.android.launcher3.views;


import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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

import java.util.ArrayList;
import java.util.List;
import android.view.Gravity;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.testing.TestLogging;
import com.android.launcher3.testing.shared.TestProtocol;
import com.android.launcher3.util.LayoutLockHelper;
import com.android.launcher3.widget.picker.WidgetsFullSheet;
import com.android.launcher3.util.PackageManagerHelper;
import static com.android.launcher3.LauncherState.NORMAL;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.InvariantDeviceProfile.GridOption;
import com.android.launcher3.screenedit.ScrollEffectAdapter;
import com.android.launcher3.screenedit.GridGallery;
import com.android.launcher3.screenedit.OverviewPanelStateTransAnimation;
import com.android.launcher3.screenedit.GridGalleryAdapter;
import com.coui.appcompat.dialog.COUIAlertDialogBuilder;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import java.util.HashMap;
import java.util.List;

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
    // 特效网格画廊
    private GridGallery effectGridGallery;
    // 动画集合
    private AnimatorSet animationSet;
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
        findViewById(R.id.widget_button).setOnClickListener(v -> {
            onWidgetsClicked(v);
        });
        findViewById(R.id.wallpaper_button).setOnClickListener(v -> {
            startPersonalPicker(v);
            resetState();
        });
        findViewById(R.id.layout_button).setOnClickListener(v -> {
            showLayoutPicker(v);
        });
        findViewById(R.id.settings_button).setOnClickListener(v -> {
            startSettings(v);
            resetState();
        });
        mainMenuView = findViewById(R.id.overview_panel_main_menu);
        effectGridGallery = findViewById(R.id.effect_gallery);
        currentState = State.MAIN_MENU;
        launcher = Launcher.getLauncher(context);
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
        switchPanelState(State.MAIN_MENU, false);
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
        optionsDialog.mIsOpen = true;
        BaseDragLayer dragLayer = activity.getDragLayer();
        dragLayer.addView(optionsDialog);
        DragLayer.LayoutParams params = (DragLayer.LayoutParams) optionsDialog.getLayoutParams();
        params.width = BaseDragLayer.LayoutParams.MATCH_PARENT;
        params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
    }

    @Override
    protected void handleClose(boolean animate) {
        if (mIsOpen) {
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
        mActivity.getDragLayer().removeView(this);
        if (mOnDismissed != null) {
            mOnDismissed.run();
        }
        Launcher launcher = Launcher.getLauncher(this.getContext());
        launcher.getEditSelectionManager().exit();
        launcher.getStateManager().goToState(NORMAL);
    }


    @Override
    protected boolean isOfType(int type) {
        return (type & TYPE_OPTIONS_POPUP_DIALOG) != 0;
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        return false;
    }

    private static boolean onWidgetsClicked(View view) {
        return openWidgets(Launcher.getLauncher(view.getContext())) != null;
    }

     /** Returns WidgetsFullSheet that was opened, or null if nothing was opened. */
     @Nullable
     public static WidgetsFullSheet openWidgets(Launcher launcher) {
         if (LayoutLockHelper.checkLockedAndShowMessage(launcher)) {
             return null;
         }
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

    /** Oppo-style Layout entry: pick workspace grid (cols × rows). */
    private static void showLayoutPicker(View view) {
        Launcher launcher = Launcher.getLauncher(view.getContext());
        if (LayoutLockHelper.checkLockedAndShowMessage(launcher)) {
            return;
        }
        InvariantDeviceProfile idp = InvariantDeviceProfile.INSTANCE.get(launcher);
        List<GridOption> options = idp.parseAllGridOptions(launcher);
        if (options.isEmpty()) {
            return;
        }

        CharSequence[] labels = new CharSequence[options.size()];
        int checkedItem = -1;
        for (int i = 0; i < options.size(); i++) {
            GridOption option = options.get(i);
            labels[i] = option.numColumns + " x " + option.numRows;
            if (idp.numColumns == option.numColumns && idp.numRows == option.numRows) {
                checkedItem = i;
            }
        }

        new COUIAlertDialogBuilder(launcher)
                .setTitle(R.string.tog_title_layout)
                .setSingleChoiceItems(labels, checkedItem, (dialog, which) -> {
                    GridOption selected = options.get(which);
                    idp.setCurrentGrid(launcher, selected.name);
                    dialog.dismiss();
                })
                .show();
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
        if (currentState != State.EFFECTS) {
            close(true);
        } else {
            switchToMainMenuState();
        }
    }
}
