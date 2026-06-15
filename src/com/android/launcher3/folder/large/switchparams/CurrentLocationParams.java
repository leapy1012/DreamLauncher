package com.android.launcher3.folder.large.switchparams;

import com.android.launcher3.Launcher;
import com.android.launcher3.folder.large.HxyLargeFolderIcon;

public class CurrentLocationParams extends BaseSwitchParams {
    private int mAlphaDuration = 0;
    private float mBeginAlpha = 0.0f;
    private float mBeginScaleX = 0.0f;
    private float mBeginScaleY = 0.0f;
    private int mDragAlphaDuration = 0;
    private float mDragBeginAlpha = 0.0f;
    private float mDragBeginScaleX = 0.0f;
    private float mDragBeginScaleY = 0.0f;
    private float mDragOffsetAlpha = 0.0f;
    private float mDragOffsetScaleX = 0.0f;
    private float mDragOffsetScaleY = 0.0f;
    private int mDragRelativeX = 0;
    private int mDragRelativeY = 0;
    private float mOffsetAlpha = 0.0f;
    private float mOffsetScaleX = 0.0f;
    private float mOffsetScaleY = 0.0f;

    public CurrentLocationParams(Launcher launcher, HxyLargeFolderIcon folderIcon, boolean isSwitchLarge) {
        super(launcher, folderIcon, 0, isSwitchLarge);
        this.mDragView = new HxyLargeDragView(this.mLauncher, this.mView);
    }

    public void setAnimationProgress(float progress) {
        super.setAnimationProgress(progress);
        float time = ((float) getDuration()) * progress;
        float alpha = this.mBeginAlpha + (this.mOffsetAlpha * (time / ((float) this.mAlphaDuration)));
        if (alpha > 1.0f) {
            alpha = 1.0f;
        }
        float scaleX = this.mBeginScaleX + (this.mOffsetScaleX * progress);
        float scaleY = this.mBeginScaleY + (this.mOffsetScaleY * progress);
        this.mView.setScaleX(scaleX);
        this.mView.setScaleY(scaleY);
        if (this.mView.getAlpha() != alpha) {
            this.mView.setAlpha(alpha);
        }
        float dragScaleX = this.mDragBeginScaleX + (this.mDragOffsetScaleX * progress);
        float dragScaleY = this.mDragBeginScaleY + (this.mDragOffsetScaleY * progress);
        this.mDragView.setScaleX(dragScaleX);
        this.mDragView.setScaleY(dragScaleY);
        if (this.isSwitchLarge) {
            this.mDragView.animateMove(((float) this.mDragRelativeX) * progress, ((float) this.mDragRelativeY) * progress);
        }
        float dragAlpha = this.mDragBeginAlpha - (this.mDragOffsetAlpha * (time / ((float) this.mDragAlphaDuration)));
        if (dragAlpha < 0.0f) {
            dragAlpha = 0.0f;
        }
        if (this.mDragView.getAlpha() != dragAlpha) {
            this.mDragView.setAlpha(dragAlpha);
        }
    }

    private void prepare() {
        showDragView();
        updateFolderData();
        prepareFolderIconView();
        prepareDragView();
    }

    private void prepareFolderIconView() {
        if (this.isSwitchLarge) {
            this.mBeginScaleX = 0.5f;
            this.mBeginScaleY = 0.5f;
            this.mOffsetScaleX = 0.5f;
            this.mOffsetScaleY = 0.5f;
        } else {
            this.mBeginScaleX = 2.0f;
            this.mBeginScaleY = 2.0f;
            this.mOffsetScaleX = -1.0f;
            this.mOffsetScaleY = -1.0f;
        }
        this.mBeginAlpha = 0.0f;
        this.mOffsetAlpha = 1.0f;
        this.mAlphaDuration = getDuration();
        this.mView.setPivotX(0.0f);
        this.mView.setPivotY(0.0f);
        this.mView.initLoadListData();
        this.mView.requestLayout();
        this.mView.setScaleX(this.mBeginScaleX);
        this.mView.setScaleY(this.mBeginScaleY);
        this.mView.setAlpha(this.mBeginAlpha);
    }

    private void prepareDragView() {
        if (this.isSwitchLarge) {
            this.mDragBeginScaleX = 1.0f;
            this.mDragBeginScaleY = 1.0f;
            this.mDragOffsetScaleX = 1.0f;
            this.mDragOffsetScaleY = 1.0f;
            this.mDragRelativeX = this.mDragView.getDragWidth() / 2;
            this.mDragRelativeY = this.mDragView.getDragHeight() / 2;
            this.mDragAlphaDuration = 30;
            this.mDragBeginAlpha = 0.3f;
            this.mDragOffsetAlpha = 0.3f;
        } else {
            this.mDragBeginScaleX = 1.0f;
            this.mDragBeginScaleY = 1.0f;
            this.mDragOffsetScaleX = -0.5f;
            this.mDragOffsetScaleY = -0.5f;
            this.mDragRelativeX = 0;
            this.mDragRelativeY = 0;
            this.mDragAlphaDuration = 100;
            this.mDragBeginAlpha = 0.5f;
            this.mDragOffsetAlpha = 0.5f;
        }
        this.mDragView.setScaleX(this.mDragBeginScaleX);
        this.mDragView.setScaleY(this.mDragBeginScaleY);
        this.mDragView.setAlpha(this.mDragBeginAlpha);
    }

    @Override
    public void startAnimation() {
        prepare();
        startProgressAnimation();
    }

    @Override
    public void stopAnimation() {
        cancelProgressAnimation();
        resetFolderIconState();
        onSwitchFolderEnd();
    }

    @Override
    public void onProgressAnimationEnd() {
        super.onProgressAnimationEnd();
        resetFolderIconState();
        onSwitchFolderEnd();
    }

    private void resetFolderIconState() {
        if (this.mView != null) {
            this.mView.setAlpha(1.0f);
            this.mView.setScaleX(1.0f);
            this.mView.setScaleY(1.0f);
        }
    }
}
