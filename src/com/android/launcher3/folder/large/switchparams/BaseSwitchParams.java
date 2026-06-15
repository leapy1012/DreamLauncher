package com.android.launcher3.folder.large.switchparams;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Rect;
import android.util.Property;
import com.android.launcher3.Launcher;
import com.android.launcher3.folder.large.HxyLargeFolderIcon;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public abstract class BaseSwitchParams implements ISwitchFolderAnimation {
    private static final Property<BaseSwitchParams, Float> ANIMATION_PROGRESS = new Property<BaseSwitchParams, Float>(Float.TYPE, "SWITCH_ANIMATION_PROGRESS") {
        public Float get(BaseSwitchParams anim) {
            return Float.valueOf(anim.mAnimationProgress);
        }

        public void set(BaseSwitchParams anim, Float progress) {
            anim.mAnimationProgress = progress.floatValue();
            anim.setAnimationProgress(progress.floatValue());
        }
    };
    public static final int SWITCH_CURRENT_LOCATION = 0;
    public static final int SWITCH_CURRENT_PAGE = 1;
    protected static final int SWITCH_DURATION = 300;
    public static final int SWITCH_NEXT_PAGE = 2;
    protected final boolean isSwitchLarge;
    public float mAnimationProgress = 0.0f;
    protected HxyLargeDragView mDragView;
    protected Launcher mLauncher;
    private ValueAnimator mProgressAnimator = null;
    private SwitchFolderBean mSwitchFolderBean = null;
    private final int mSwitchMode;
    protected HxyLargeFolderIcon mView;

    @Retention(RetentionPolicy.SOURCE)
    public @interface SwitchMode {
    }

    public abstract void startAnimation();

    public abstract void stopAnimation();

    public BaseSwitchParams(Launcher launcher, HxyLargeFolderIcon folderIcon, int switchMode, boolean isSwitchLarge2) {
        this.mLauncher = launcher;
        this.mView = folderIcon;
        this.mSwitchMode = switchMode;
        this.isSwitchLarge = isSwitchLarge2;
    }

    public void release() {
        cancelProgressAnimation();
        releaseDragView();
        SwitchFolderBean switchFolderBean = this.mSwitchFolderBean;
        if (switchFolderBean != null) {
            switchFolderBean.release();
            this.mSwitchFolderBean = null;
        }
        this.mView = null;
        this.mLauncher = null;
        this.mAnimationProgress = 0.0f;
    }

    private void releaseDragView() {
        HxyLargeDragView hxyLargeDragView = this.mDragView;
        if (hxyLargeDragView != null) {
            hxyLargeDragView.release();
            this.mDragView = null;
        }
    }

    public int getDuration() {
        return 300;
    }

    public void showDragView() {
        Rect pos = getDescendantRectRelativeToSelf();
        this.mDragView.show(pos.left, pos.top);
    }

    public Rect getDescendantRectRelativeToSelf() {
        Rect pos = new Rect();
        this.mLauncher.getDragLayer().getDescendantRectRelativeToSelf(this.mView, pos);
        return pos;
    }

    public void setSwitchFolderBean(SwitchFolderBean bean) {
        this.mSwitchFolderBean = bean;
    }

    public void updateFolderData() {
        SwitchFolderBean switchFolderBean = this.mSwitchFolderBean;
        if (switchFolderBean != null) {
            switchFolderBean.updateFolderData();
        }
    }

    public int getSwitchMode() {
        return this.mSwitchMode;
    }

    public void onSwitchFolderBegin() {
        HxyLargeFolderIcon hxyLargeFolderIcon = this.mView;
        if (hxyLargeFolderIcon != null) {
            hxyLargeFolderIcon.onSwitchFolderBegin();
        }
    }

    public void onSwitchFolderEnd() {
        HxyLargeFolderIcon hxyLargeFolderIcon = this.mView;
        if (hxyLargeFolderIcon != null) {
            hxyLargeFolderIcon.onSwitchFolderEnd();
        }
    }

    public void startProgressAnimation() {
        cancelProgressAnimation();
        this.mAnimationProgress = 0.0f;
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(this, ANIMATION_PROGRESS, new float[]{0.0f, 1.0f});
        this.mProgressAnimator = ofFloat;
        ofFloat.setDuration((long) getDuration());
        this.mProgressAnimator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                BaseSwitchParams.this.onProgressAnimationBegin();
            }

            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                BaseSwitchParams.this.onProgressAnimationEnd();
            }
        });
        this.mProgressAnimator.start();
    }

    public void onProgressAnimationBegin() {
    }

    public void onProgressAnimationEnd() {
    }

    public void cancelProgressAnimation() {
        ValueAnimator valueAnimator = this.mProgressAnimator;
        if (valueAnimator != null) {
            valueAnimator.cancel();
            this.mProgressAnimator.removeAllListeners();
            this.mProgressAnimator = null;
        }
    }

    public void setAnimationProgress(float progress) {
    }
}