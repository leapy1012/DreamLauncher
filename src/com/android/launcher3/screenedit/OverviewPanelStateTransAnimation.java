package com.android.launcher3.screenedit;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.view.View;

import androidx.annotation.NonNull;

import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherViewPropertyAnimator;
import com.android.launcher3.anim.AlphaUpdateListener;
import java.util.HashMap;
import java.util.WeakHashMap;
import com.android.launcher3.views.OptionsDialogView;

public class OverviewPanelStateTransAnimation {

    public static WeakHashMap<Animator, Object> runningAnimations = new WeakHashMap<>();

    public static Animator.AnimatorListener animationListener = new Animator.AnimatorListener() {
        public void onAnimationCancel(@NonNull Animator animator) {
            runningAnimations.remove(animator);
        }

        public void onAnimationEnd(@NonNull Animator animator) {
            runningAnimations.remove(animator);
        }

        public void onAnimationRepeat(@NonNull Animator animator) {
        }

        public void onAnimationStart(@NonNull Animator animator) {
            runningAnimations.put(animator, (Object) null);
        }
    };

    public Launcher launcher;

    public OptionsDialogView optionsDialogView;

    public AnimatorSet currentAnimatorSet;

    public class TransitionStates {
        private final boolean isFromMainMenu;
        private final boolean isFromEffects;
        private final boolean isFromWidgetListPackage;
        private final boolean isFromWidgets;
        private final boolean isToMainMenu;
        private final boolean isToEffects;
        private final boolean isToWidgetListPackage;
        private final boolean isToWidgets;
        private final boolean involvesMainMenu;
        private final boolean involvesEffects;
        private final boolean involvesWidgetListPackage;
        private final boolean involvesWidgets;

        public TransitionStates(OptionsDialogView.State fromState, OptionsDialogView.State toState) {
            this.isFromMainMenu = fromState == OptionsDialogView.State.MAIN_MENU;
            this.isFromEffects = fromState == OptionsDialogView.State.EFFECTS;
            this.isFromWidgetListPackage = fromState == OptionsDialogView.State.WIDGET_LIST_PACKAGE;
            this.isFromWidgets = fromState == OptionsDialogView.State.WIDGETS;
            this.isToMainMenu = toState == OptionsDialogView.State.MAIN_MENU;
            this.isToEffects = toState == OptionsDialogView.State.EFFECTS;
            this.isToWidgetListPackage = toState == OptionsDialogView.State.WIDGET_LIST_PACKAGE;
            this.isToWidgets = toState == OptionsDialogView.State.WIDGETS;
            this.involvesMainMenu = isFromMainMenu || isToMainMenu;
            this.involvesEffects = isFromEffects || isToEffects;
            this.involvesWidgetListPackage = isFromWidgetListPackage || isToWidgetListPackage;
            this.involvesWidgets = isFromWidgets || isToWidgets;
        }
    }

    public OverviewPanelStateTransAnimation(Launcher launcher, OptionsDialogView optionsDialogView) {
        this.launcher = launcher;
        this.optionsDialogView = optionsDialogView;
    }

    public static void cancelOnDestroyActivity(Animator animator) {
        animator.addListener(animationListener);
    }

    public static AnimatorSet createAnimatorSet() {
        AnimatorSet animatorSet = new AnimatorSet();
        cancelOnDestroyActivity(animatorSet);
        return animatorSet;
    }

    public final void performTransitionAnimation(TransitionStates transitionStates, boolean useAnimation, int duration, HashMap<View, Integer> hashMap) {
        cancelCurrentAnimation();
        if (useAnimation) {
            currentAnimatorSet = createAnimatorSet();
        }
        float mainMenuAlpha = transitionStates.isToMainMenu ? 1.0f : 0.0f;
        float effectsAlpha = transitionStates.isToEffects ? 1.0f : 0.0f;
        float mainMenuScaleFrom = transitionStates.isToMainMenu ? 1.5f : 1.0f;
        float mainMenuScaleTo = transitionStates.isToMainMenu ? 1.0f : 1.5f;

        View mainMenuView = optionsDialogView.getMainMenu();
        View effectsView = optionsDialogView.getEffectsView();
        if (useAnimation) {
            if (transitionStates.involvesMainMenu) {
                mainMenuView.setVisibility(View.VISIBLE);
                mainMenuView.setScaleX(mainMenuScaleFrom);
                mainMenuView.setScaleY(mainMenuScaleFrom);
                LauncherViewPropertyAnimator mainMenuAnimator = new LauncherViewPropertyAnimator(mainMenuView);
                mainMenuAnimator.scaleX(mainMenuScaleTo).scaleY(mainMenuScaleTo).alpha(mainMenuAlpha).setDuration(duration);
                mainMenuAnimator.addListener(new AlphaUpdateListener(mainMenuView));
                currentAnimatorSet.play(mainMenuAnimator);
            }
            if (transitionStates.involvesEffects) {
                effectsView.setVisibility(View.VISIBLE);
                LauncherViewPropertyAnimator effectsAnimator = new LauncherViewPropertyAnimator(effectsView);
                effectsAnimator.alpha(effectsAlpha).setDuration(duration);
                effectsAnimator.addListener(new AlphaUpdateListener(effectsView));
                currentAnimatorSet.play(effectsAnimator);
            }
            currentAnimatorSet.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animator) {
                    currentAnimatorSet = null;
                }
            });
            return;
        }
        mainMenuView.setAlpha(mainMenuAlpha);
        effectsView.setAlpha(effectsAlpha);
        AlphaUpdateListener.updateVisibility(mainMenuView);
        AlphaUpdateListener.updateVisibility(effectsView);
    }

    public final void cancelCurrentAnimation() {
        if (currentAnimatorSet != null) {
            currentAnimatorSet.setDuration(0);
            currentAnimatorSet.cancel();
        }
        currentAnimatorSet = null;
    }

    public final int getAnimationDuration(TransitionStates transitionStates) {
        return 200;
    }

    public AnimatorSet getAnimationToState(OptionsDialogView.State fromState, OptionsDialogView.State toState, boolean useAnimation, HashMap<View, Integer> hashMap) {
        TransitionStates transitionStates = new TransitionStates(fromState, toState);
        performTransitionAnimation(transitionStates, useAnimation, getAnimationDuration(transitionStates), hashMap);
        return currentAnimatorSet;
    }
}
