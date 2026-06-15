package com.android.launcher3.folder.large.switchparams;

import android.graphics.Rect;
import com.android.launcher3.Launcher;
import com.android.launcher3.folder.large.HxyLargeFolderIcon;

public class CurrentPageParams extends BaseSwitchParams {
    private float mDragBeginAlpha = 0.0f;
    private float mDragBeginScaleX = 0.0f;
    private float mDragBeginScaleY = 0.0f;
    private float mDragOffsetAlpha = 0.0f;
    private float mDragOffsetScaleX = 0.0f;
    private float mDragOffsetScaleY = 0.0f;
    private int mDragRelativeX = 0;
    private int mDragRelativeY = 0;

    public CurrentPageParams(Launcher launcher, HxyLargeFolderIcon folderIcon, boolean isSwitchLarge) {
        super(launcher, folderIcon, 1, isSwitchLarge);
        this.mDragView = new HxyLargeDragView(this.mLauncher, this.mView, true);
    }

    private void prepare() {
        showDragView();
        this.mView.setAlpha(0.0f);
        updateFolderData();
        prepareFolderIconView();
        prepareDragView();
    }

    private void prepareFolderIconView() {
        this.mView.setPivotX(0.0f);
        this.mView.setPivotY(0.0f);
        this.mView.initLoadListData();
        this.mView.requestLayout();
    }

    private void prepareDragView() {
        this.mDragBeginScaleX = 0.5f;
        this.mDragBeginScaleY = 0.5f;
        this.mDragOffsetScaleX = 0.5f;
        this.mDragOffsetScaleY = 0.5f;
        this.mDragBeginAlpha = 0.5f;
        this.mDragOffsetAlpha = 0.5f;
        this.mDragView.setAlpha(this.mDragBeginAlpha);
        this.mDragView.setScaleX(this.mDragBeginScaleX);
        this.mDragView.setScaleY(this.mDragBeginScaleY);
        this.mDragView.move(this.mDragView.getCoordinateX() - (this.mDragView.getDragWidth() / 4), this.mDragView.getCoordinateY() - (this.mDragView.getDragHeight() / 4));
    }

    private void prepareDragViewScale() {
        Rect target = getDescendantRectRelativeToSelf();
        this.mDragRelativeX = target.left - this.mDragView.getCoordinateX();
        this.mDragRelativeY = target.top - this.mDragView.getCoordinateY();
    }

    @Override
    public void startAnimation() {
        prepare();
        postAnimation();
    }

    private void postAnimation() {
        this.mView.postDelayed(new CurrentPageRunnable(this), 500);
    }

    public void executeAnimation() {
        prepareDragViewScale();
        startProgressAnimation();
    }

    @Override
    public void stopAnimation() {
        cancelProgressAnimation();
        resetFolderIconState();
        onSwitchFolderEnd();
    }

    @Override
    public void setAnimationProgress(float progress) {
        super.setAnimationProgress(progress);
        float dragScaleX = this.mDragBeginScaleX + (this.mDragOffsetScaleX * progress);
        float dragScaleY = this.mDragBeginScaleY + (this.mDragOffsetScaleY * progress);
        this.mDragView.setAlpha(this.mDragBeginAlpha + (this.mDragOffsetAlpha * progress));
        this.mDragView.setScaleX(dragScaleX);
        this.mDragView.setScaleY(dragScaleY);
        this.mDragView.animateMove(((float) this.mDragRelativeX) * progress, ((float) this.mDragRelativeY) * progress);
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
        }
    }

    public record CurrentPageRunnable(CurrentPageParams currentPageParams) implements Runnable {

        public void run() {
                this.currentPageParams.executeAnimation();
            }
        }
}
