package com.coui.appcompat.poplist;

import com.coui.appcompat.R;

import android.view.View;
import com.coui.appcompat.animation.dynamicanimation.COUIDynamicAnimation;
import com.coui.appcompat.version.COUIVersionUtil;
import java.util.concurrent.atomic.AtomicLong;


abstract class BasePopupMenuAnimationController {

    @Deprecated
    static final float DEFAULT_MAIN_MENU_ALPHA_SPRING_RESPONSE = 0.3f;
    static final float DEFAULT_MAIN_MENU_ENTER_ALPHA_END_VALUE = 1.0f;
    static final float DEFAULT_MAIN_MENU_ENTER_ALPHA_START_VALUE = 0.01f;
    static final float DEFAULT_MAIN_MENU_ENTER_SCALE_END_VALUE = 1.0f;
    static final float DEFAULT_MAIN_MENU_ENTER_SCALE_START_VALUE = 0.0f;

    @Deprecated
    static final float DEFAULT_MAIN_MENU_SCALE_SPRING_RESPONSE = 0.4f;
    static final float DEFAULT_MAIN_MENU_SPRING_BOUNCE = 0.2f;
    static final float DEFAULT_MAIN_MENU_SPRING_EXIT_ALPHA_RESPONSE = 0.25f;
    static final float DEFAULT_MAIN_MENU_SPRING_EXIT_BOUNCE = 0.0f;
    static final float DEFAULT_MAIN_MENU_SPRING_EXIT_SCALE_RESPONSE = 0.3f;
    static final float DEFAULT_MAIN_MENU_SPRING_RESPONSE = 0.35f;
    static final float DEFAULT_SPRING_FACTOR = 10000.0f;
    static final float DEFAULT_SUB_MENU_SPRING_BOUNCE = 0.0f;
    static final float DEFAULT_SUB_MENU_SPRING_RESPONSE = 0.35f;
    private static final int RT_ANIMATION_ADDON_MAJOR_VERSION = 37;
    private static final int RT_ANIMATION_ADDON_SUB_VERSION = 15;
    PopupMenuDomain mDomain;
    View mMainMenuRoot;
    View mRootView;
    View mSubMenuRoot;
    final AtomicLong mMainMenuAnimationGeneration = new AtomicLong(0);
    private boolean mAnimationsCreatedForRt = false;
    OnMenuStateChangedListener mMenuStateChangedListener = null;
    boolean mEnableRenderThreadAnimation = true;
    Runnable mPendingMainMenuAnimationCallback = null;
    final COUIDynamicAnimation.OnAnimationEndListener mMainMenuAnimationEndListener = new COUIDynamicAnimation.OnAnimationEndListener() {
        @Override
        public final void onAnimationEnd(COUIDynamicAnimation cOUIDynamicAnimation, boolean z6, float f2, float f10) {
            BasePopupMenuAnimationController.this.lambda$new$1(cOUIDynamicAnimation, z6, f2, f10);
        }
    };

    public interface OnMenuStateChangedListener {
        void onMainMenuAnimationCanceled();

        void onMainMenuEntered();

        void onMainMenuExited();

        void onMainMenuStartToEnter();

        void onMainMenuStartToExit();

        void onSubMenuAnimationCanceled();

        void onSubMenuEntered();

        void onSubMenuExited();

        void onSubMenuStartToEnter();

        void onSubMenuStartToExit();
    }


    public void lambda$new$0(long j2, boolean z6, float f2) {
        if (j2 == this.mMainMenuAnimationGeneration.get()) {
            notifyMainMenuAnimationStateChange(z6, f2);
        }
        this.mPendingMainMenuAnimationCallback = null;
    }


    public void lambda$new$1(COUIDynamicAnimation cOUIDynamicAnimation, final boolean z6, final float f2, float f10) {
        View view;
        final long j2 = this.mMainMenuAnimationGeneration.get();
        AnimationExecutor executor = getExecutor();
        if (!executor.isAsynchronous() || (view = this.mRootView) == null) {
            notifyMainMenuAnimationStateChange(z6, f2);
            return;
        }
        Runnable runnable = this.mPendingMainMenuAnimationCallback;
        if (runnable != null) {
            view.removeCallbacks(runnable);
        }
        Runnable runnable2 = new Runnable() {
            @Override
            public final void run() {
                BasePopupMenuAnimationController.this.lambda$new$0(j2, z6, f2);
            }
        };
        this.mPendingMainMenuAnimationCallback = runnable2;
        executor.runOnMainThread(this.mRootView, runnable2);
    }

    public void cleanupPendingMainMenuCallbacks() {
        Runnable runnable;
        View view = this.mRootView;
        if (view == null || (runnable = this.mPendingMainMenuAnimationCallback) == null) {
            return;
        }
        view.removeCallbacks(runnable);
        this.mPendingMainMenuAnimationCallback = null;
    }

    public void detach() {
    }

    public AnimationExecutor getExecutor() {
        return shouldUseRenderThreadAnimation() ? RenderThreadAnimationExecutor.INSTANCE : NormalAnimationExecutor.INSTANCE;
    }

    public void invalidatePendingMainMenuCallbacks() {
        Runnable runnable;
        this.mMainMenuAnimationGeneration.incrementAndGet();
        View view = this.mRootView;
        if (view == null || (runnable = this.mPendingMainMenuAnimationCallback) == null) {
            return;
        }
        view.removeCallbacks(runnable);
        this.mPendingMainMenuAnimationCallback = null;
    }

    public void notifyMainMenuAnimationStateChange(boolean z6, float f2) {
        if (z6) {
            OnMenuStateChangedListener onMenuStateChangedListener = this.mMenuStateChangedListener;
            if (onMenuStateChangedListener != null) {
                onMenuStateChangedListener.onMainMenuAnimationCanceled();
                return;
            }
            return;
        }
        if (f2 == 0.0f) {
            OnMenuStateChangedListener onMenuStateChangedListener2 = this.mMenuStateChangedListener;
            if (onMenuStateChangedListener2 != null) {
                onMenuStateChangedListener2.onMainMenuExited();
                return;
            }
            return;
        }
        OnMenuStateChangedListener onMenuStateChangedListener3 = this.mMenuStateChangedListener;
        if (onMenuStateChangedListener3 != null) {
            onMenuStateChangedListener3.onMainMenuEntered();
        }
    }

    public void setDomain(PopupMenuDomain popupMenuDomain) {
        this.mDomain = popupMenuDomain;
    }

    public void setEnableRenderThreadAnimation(boolean z6) {
        this.mEnableRenderThreadAnimation = z6;
    }

    public void setMainMenuView(View view) {
        this.mMainMenuRoot = view;
    }

    public void setMenuRootView(View view) {
        this.mRootView = view;
    }

    public void setOnSubMenuStateChangedListener(OnMenuStateChangedListener onMenuStateChangedListener) {
        this.mMenuStateChangedListener = onMenuStateChangedListener;
    }

    public void setSubMenuView(View view) {
        this.mSubMenuRoot = view;
    }

    public boolean shouldReuseMainMenuAnimations(boolean z6) {
        boolean zShouldUseRenderThreadAnimation = shouldUseRenderThreadAnimation();
        if (z6 && this.mAnimationsCreatedForRt == zShouldUseRenderThreadAnimation) {
            return true;
        }
        this.mAnimationsCreatedForRt = zShouldUseRenderThreadAnimation;
        return false;
    }

    public boolean shouldUseRenderThreadAnimation() {
        if (!this.mEnableRenderThreadAnimation || !COUIVersionUtil.checkOPlusViewSubSDK(37, 15)) {
            return false;
        }
        View view = this.mMainMenuRoot;
        return (view instanceof RoundFrameLayout) && !((RoundFrameLayout) view).getUseBackgroundBlur();
    }

    public void startMainMenuEnter(boolean z6) {
    }

    public void startMainMenuExit(boolean z6) {
    }

    public void startSubMenuEnter(boolean z6) {
    }

    public void startSubMenuExit(boolean z6) {
    }

    public void stopAllAnimation() {
    }

    public final void startMainMenuEnter() {
        startMainMenuEnter(true);
    }

    public final void startMainMenuExit() {
        startMainMenuExit(true);
    }

    public final void startSubMenuEnter() {
        startSubMenuEnter(true);
    }

    public final void startSubMenuExit() {
        startSubMenuExit(true);
    }
}
