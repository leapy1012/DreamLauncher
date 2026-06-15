package com.android.launcher3;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.view.View;
import android.view.ViewPropertyAnimator;
import com.android.launcher3.FirstFrameAnimatorHelper;
import com.android.launcher3.screenedit.OverviewPanelStateTransAnimation;

import java.util.ArrayList;
import java.util.EnumSet;

public class LauncherViewPropertyAnimator extends Animator implements Animator.AnimatorListener {

    // 存储要应用的属性集合
    public EnumSet<Properties> propertiesToApply = EnumSet.noneOf(Properties.class);
    // 视图属性动画器
    public ViewPropertyAnimator viewPropertyAnimator;
    // 目标视图
    public View targetView;
    // 平移 X 轴的值
    public float translationXValue;
    // 平移 Y 轴的值
    public float translationYValue;
    // X 轴缩放比例
    public float scaleXValue;
    // Y 轴缩放比例
    public float scaleYValue;
    // 旋转角度
    public float rotationValue;
    // 透明度值
    public float alphaValue;
    // 动画持续时间
    public long animationDuration;
    // 动画开始延迟时间
    public long animationStartDelay;
    // 时间插值器
    public TimeInterpolator timeInterpolator;
    // 动画监听器列表
    public ArrayList<Animator.AnimatorListener> animatorListeners = new ArrayList<>();
    // 动画是否正在运行
    public boolean isAnimationRunning = false;
    // 第一帧动画帮助器
    public FirstFrameAnimatorHelper firstFrameAnimatorHelper;

    public enum Properties {
        TRANSLATION_X,
        TRANSLATION_Y,
        SCALE_X,
        SCALE_Y,
        ROTATION,
        ALPHA,
        START_DELAY,
        DURATION,
        INTERPOLATOR,
        WITH_LAYER
    }

    public LauncherViewPropertyAnimator(View view) {
        this.targetView = view;
    }

    public void addListener(Animator.AnimatorListener animatorListener) {
        this.animatorListeners.add(animatorListener);
    }

    public LauncherViewPropertyAnimator alpha(float alpha) {
        this.propertiesToApply.add(Properties.ALPHA);
        this.alphaValue = alpha;
        return this;
    }

    public void cancel() {
        ViewPropertyAnimator viewPropertyAnimator = this.viewPropertyAnimator;
        if (viewPropertyAnimator != null) {
            viewPropertyAnimator.cancel();
        }
    }

    public void end() {
    }

    // 获取动画持续时间
    public long getDuration() {
        return this.animationDuration;
    }

    // 获取动画监听器列表
    public ArrayList<Animator.AnimatorListener> getListeners() {
        return this.animatorListeners;
    }

    // 获取动画开始延迟时间
    public long getStartDelay() {
        return this.animationStartDelay;
    }

    // 检查动画是否正在运行
    public boolean isRunning() {
        return this.isAnimationRunning;
    }

    // 检查动画是否已启动
    public boolean isStarted() {
        return this.viewPropertyAnimator != null;
    }

    // 动画取消时的回调
    public void onAnimationCancel(Animator animator) {
        for (int i = 0; i < this.animatorListeners.size(); i++) {
            this.animatorListeners.get(i).onAnimationCancel(this);
        }
        this.isAnimationRunning = false;
    }

    // 动画结束时的回调
    public void onAnimationEnd(Animator animator) {
        for (int i = 0; i < this.animatorListeners.size(); i++) {
            this.animatorListeners.get(i).onAnimationEnd(this);
        }
        this.isAnimationRunning = false;
    }

    // 动画重复时的回调
    public void onAnimationRepeat(Animator animator) {
        for (int i = 0; i < this.animatorListeners.size(); i++) {
            this.animatorListeners.get(i).onAnimationRepeat(this);
        }
    }

    // 动画开始时的回调
    public void onAnimationStart(Animator animator) {
        for (int i = 0; i < this.animatorListeners.size(); i++) {
            this.animatorListeners.get(i).onAnimationStart(this);
        }
        this.isAnimationRunning = true;
    }

    // 移除所有动画监听器
    public void removeAllListeners() {
        this.animatorListeners.clear();
    }

    // 移除指定的动画监听器
    public void removeListener(Animator.AnimatorListener animatorListener) {
        this.animatorListeners.remove(animatorListener);
    }

    // 设置旋转角度
    public LauncherViewPropertyAnimator rotation(float rotation) {
        this.propertiesToApply.add(Properties.ROTATION);
        this.rotationValue = rotation;
        return this;
    }

    // 设置 X 轴缩放比例
    public LauncherViewPropertyAnimator scaleX(float scaleX) {
        this.propertiesToApply.add(Properties.SCALE_X);
        this.scaleXValue = scaleX;
        return this;
    }

    // 设置 Y 轴缩放比例
    public LauncherViewPropertyAnimator scaleY(float scaleY) {
        this.propertiesToApply.add(Properties.SCALE_Y);
        this.scaleYValue = scaleY;
        return this;
    }

    // 设置动画持续时间
    public Animator setDuration(long duration) {
        this.propertiesToApply.add(Properties.DURATION);
        this.animationDuration = duration;
        return this;
    }

    // 设置时间插值器
    public void setInterpolator(TimeInterpolator interpolator) {
        this.propertiesToApply.add(Properties.INTERPOLATOR);
        this.timeInterpolator = interpolator;
    }

    // 设置动画开始延迟时间
    public void setStartDelay(long startDelay) {
        this.propertiesToApply.add(Properties.START_DELAY);
        this.animationStartDelay = startDelay;
    }

    public void setTarget(Object obj) {
        throw new RuntimeException("Not implemented");
    }

    public void setupEndValues() {
    }

    public void setupStartValues() {
    }

    public void start() {
        this.viewPropertyAnimator = this.targetView.animate();
        if (this.propertiesToApply.contains(Properties.TRANSLATION_X)) {
            this.viewPropertyAnimator.translationX(this.translationXValue);
        }
        if (this.propertiesToApply.contains(Properties.TRANSLATION_Y)) {
            this.viewPropertyAnimator.translationY(this.translationYValue);
        }
        if (this.propertiesToApply.contains(Properties.SCALE_X)) {
            this.viewPropertyAnimator.scaleX(this.scaleXValue);
        }
        if (this.propertiesToApply.contains(Properties.ROTATION)) {
            this.viewPropertyAnimator.rotation(this.rotationValue);
        }
        if (this.propertiesToApply.contains(Properties.SCALE_Y)) {
            this.viewPropertyAnimator.scaleY(this.scaleYValue);
        }
        if (this.propertiesToApply.contains(Properties.ALPHA)) {
            this.viewPropertyAnimator.alpha(this.alphaValue);
        }
        if (this.propertiesToApply.contains(Properties.START_DELAY)) {
            this.viewPropertyAnimator.setStartDelay(this.animationStartDelay);
        }
        if (this.propertiesToApply.contains(Properties.DURATION)) {
            this.viewPropertyAnimator.setDuration(this.animationStartDelay);
        }
        if (this.propertiesToApply.contains(Properties.INTERPOLATOR)) {
            this.viewPropertyAnimator.setInterpolator(this.timeInterpolator);
        }
        if (this.propertiesToApply.contains(Properties.WITH_LAYER)) {
            this.viewPropertyAnimator.withLayer();
        }
        this.viewPropertyAnimator.setListener(this);
        this.viewPropertyAnimator.start();
        OverviewPanelStateTransAnimation.cancelOnDestroyActivity(this);
    }

    public LauncherViewPropertyAnimator translationX(float f) {
        this.propertiesToApply.add(Properties.TRANSLATION_X);
        this.translationXValue = f;
        return this;
    }

    public LauncherViewPropertyAnimator translationY(float f) {
        this.propertiesToApply.add(Properties.TRANSLATION_Y);
        this.translationYValue = f;
        return this;
    }

    public LauncherViewPropertyAnimator withLayer() {
        this.propertiesToApply.add(Properties.WITH_LAYER);
        return this;
    }

    public Animator clone() {
        throw new RuntimeException("Not implemented");
    }
}
