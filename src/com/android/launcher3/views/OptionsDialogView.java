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
import android.view.ViewTreeObserver;
import android.view.Gravity;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.testing.TestLogging;
import com.android.launcher3.testing.shared.TestProtocol;
import com.android.launcher3.util.LayoutLockHelper;
import com.android.launcher3.widget.picker.WidgetsFullSheet;
import com.android.launcher3.util.PackageManagerHelper;
import static com.android.launcher3.LauncherState.NORMAL;
import com.android.launcher3.screenedit.ScrollEffectAdapter;
import com.android.launcher3.togglebar.ColorOsLayoutOverlay;
import com.android.launcher3.screenedit.GridGallery;
import com.android.launcher3.screenedit.OverviewPanelStateTransAnimation;
import com.android.launcher3.screenedit.GridGalleryAdapter;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.animation.PathInterpolator;
import java.util.HashMap;
import java.util.List;

public class OptionsDialogView extends AbstractFloatingView {

    // An intent extra to indicate the horizontal scroll of the wallpaper.
    private static final String EXTRA_WALLPAPER_OFFSET = "com.android.launcher3.WALLPAPER_OFFSET";
    private static final String EXTRA_WALLPAPER_FLAVOR = "com.android.launcher3.WALLPAPER_FLAVOR";
    // An intent extra to indicate the launch source by launcher.
    private static final String EXTRA_WALLPAPER_LAUNCH_SOURCE =
            "com.android.wallpaper.LAUNCH_SOURCE";

    /**
     * Slide distance fallback (Oppo uses 320px). Prefer measured height so the bar
     * starts fully off-screen.
     */
    private static final float TOGGLE_BAR_ENTER_TRANSLATION_Y_FALLBACK = 320f;
    /** Match SpringLoadedState / Oppo workspace scale timing so bar and desktop finish together. */
    private static final long ENTER_EXIT_DURATION_MS = 420L;
    /** Oppo ToggleBarAnimHelper.INTERPOLATOR_WORKSPACE_SCALE */
    private static final PathInterpolator ENTER_TRANSLATION_INTERPOLATOR =
            new PathInterpolator(0.33f, 0f, 0.67f, 1f);
    /** Oppo ToggleBarAnimHelper.INTERPOLATOR_GAUSSIAN_VIEW */
    private static final PathInterpolator ENTER_ALPHA_INTERPOLATOR =
            new PathInterpolator(0.42f, 0f, 0.58f, 1f);
    /** Oppo ToggleBarAnimHelper.INTERPOLATOR_TOGGLE_BAR (exit). */
    private static final PathInterpolator EXIT_INTERPOLATOR =
            new PathInterpolator(0.3f, 0f, 0.1f, 1f);

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
    @Nullable private AnimatorSet mEnterExitAnimator;
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
        optionsDialog.mOnDismissed = onDismissed;
        optionsDialog.mIsOpen = true;
        BaseDragLayer dragLayer = activity.getDragLayer();
        dragLayer.addView(optionsDialog);
        DragLayer.LayoutParams params = (DragLayer.LayoutParams) optionsDialog.getLayoutParams();
        params.width = BaseDragLayer.LayoutParams.MATCH_PARENT;
        params.height = BaseDragLayer.LayoutParams.WRAP_CONTENT;
        params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        params.ignoreInsets = true;
        params.bottomMargin = 0;
        int navInset = 0;
        if (activity instanceof Launcher) {
            navInset = ((Launcher) activity).getDeviceProfile().getInsets().bottom;
        }
        optionsDialog.setPadding(optionsDialog.getPaddingLeft(), optionsDialog.getPaddingTop(),
                optionsDialog.getPaddingRight(), navInset);
        optionsDialog.setAlpha(0f);
        optionsDialog.setTranslationY(TOGGLE_BAR_ENTER_TRANSLATION_Y_FALLBACK);
        // Above hotseat/toolbar so the bar is actually visible while animating.
        dragLayer.bringChildToFront(optionsDialog);
        optionsDialog.scheduleEnterAnimationAfterLayout();
    }

    private float getEnterTranslationY() {
        int h = getHeight();
        return h > 0 ? h : TOGGLE_BAR_ENTER_TRANSLATION_Y_FALLBACK;
    }

    private void scheduleEnterAnimationAfterLayout() {
        // IMPORTANT: do not wait for "stable height". goToState(EDIT_MODE) keeps
        // relayouting DragLayer, which previously reset our stable-frame counter forever
        // and left this view stuck at alpha=0 (edit chrome visible, toggle bar missing).
        getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (getWidth() <= 0) {
                    return true;
                }
                ViewTreeObserver observer = getViewTreeObserver();
                if (observer.isAlive()) {
                    observer.removeOnPreDrawListener(this);
                }
                // post so this pre-draw can finish; then run enter on next frame.
                post(() -> playEnterAnimation());
                return true;
            }
        });
    }

    /**
     * Enter: slide up from below + fade (420ms PathInterpolator, synced with workspace).
     */
    private void playEnterAnimation() {
        if (!mIsOpen || getParent() == null) {
            return;
        }
        cancelEnterExitAnimation();
        resetMenuItemTransforms();
        float startY = getEnterTranslationY();
        setTranslationY(startY);
        setAlpha(0f);

        ObjectAnimator ty = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, startY, 0f);
        ty.setInterpolator(ENTER_TRANSLATION_INTERPOLATOR);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(this, View.ALPHA, 0f, 1f);
        alpha.setInterpolator(ENTER_ALPHA_INTERPOLATOR);

        mEnterExitAnimator = new AnimatorSet();
        mEnterExitAnimator.playTogether(ty, alpha);
        mEnterExitAnimator.setDuration(ENTER_EXIT_DURATION_MS);
        mEnterExitAnimator.start();
    }

    private void cancelEnterExitAnimation() {
        if (mEnterExitAnimator != null) {
            mEnterExitAnimator.cancel();
            mEnterExitAnimator = null;
        }
        animate().cancel();
    }

    @Override
    protected void handleClose(boolean animate) {
        if (mIsOpen) {
            mIsOpen = false;
            if (animate) {
                cancelEnterExitAnimation();
                float endY = getEnterTranslationY();
                ObjectAnimator ty = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, getTranslationY(), endY);
                ty.setInterpolator(EXIT_INTERPOLATOR);
                ObjectAnimator alpha = ObjectAnimator.ofFloat(this, View.ALPHA, getAlpha(), 0f);
                alpha.setInterpolator(EXIT_INTERPOLATOR);
                mEnterExitAnimator = new AnimatorSet();
                mEnterExitAnimator.playTogether(ty, alpha);
                mEnterExitAnimator.setDuration(ENTER_EXIT_DURATION_MS);
                mEnterExitAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        onClosed();
                    }
                });
                mEnterExitAnimator.start();
            } else {
                cancelEnterExitAnimation();
                resetMenuItemTransforms();
                onClosed();
            }
        }
    }

    private void resetMenuItemTransforms() {
        if (!(mainMenuView instanceof ViewGroup)) {
            return;
        }
        ViewGroup menu = (ViewGroup) mainMenuView;
        for (int i = 0; i < menu.getChildCount(); i++) {
            View child = menu.getChildAt(i);
            child.setAlpha(1f);
            child.setTranslationY(0f);
        }
    }

    private void onClosed() {
        cancelEnterExitAnimation();
        setTranslationY(0f);
        setAlpha(1f);
        resetMenuItemTransforms();
        mActivity.getDragLayer().removeView(this);
        if (mOnDismissed != null) {
            mOnDismissed.run();
            mOnDismissed = null;
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

    /** Oppo ToggleBar Layout: live workspace preview + grid sheet. */
    private static void showLayoutPicker(View view) {
        ColorOsLayoutOverlay.show(Launcher.getLauncher(view.getContext()));
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
