package com.android.launcher3.folder.large.switchparams;

import android.view.View;
import android.widget.FrameLayout;
import com.android.launcher3.R;
import com.android.launcher3.Launcher;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.model.data.FolderInfo;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.views.BaseDragLayer;
import com.android.launcher3.folder.large.HxyLargeFolderProxy;

public class HxyLargeDragView extends FrameLayout {
    private final int[] mCoordinateXY;
    private DragLayer mDragLayer;
    private FolderIcon mFolderIcon;
    private int mHeight;
    private int mWidth;

    public HxyLargeDragView(Launcher launcher, FolderIcon folderIcon) {
        this(launcher, folderIcon, false);
    }

    public HxyLargeDragView(Launcher launcher, FolderIcon folderIcon, boolean isForceLarge) {
        super(launcher);
        this.mCoordinateXY = new int[2];
        this.mDragLayer = launcher.getDragLayer();
        this.mWidth = folderIcon.getMeasuredWidth();
        this.mHeight = folderIcon.getMeasuredHeight();
        FolderInfo info = (FolderInfo) folderIcon.getTag();
        boolean isLargeFolder = HxyLargeFolderProxy.isLargeFolder((ItemInfo) info);
        FolderInfo newInfo = HxyLargeFolderProxy.cloneFolderInfo(info);
        if (isForceLarge && !isLargeFolder) {
            newInfo.spanX = 2;
            newInfo.spanY = 2;
            this.mWidth *= 2;
            this.mHeight *= 2;
        }
        FolderIcon inflateIcon = FolderIcon.inflateIcon(R.layout.hxy_large_folder_icon, launcher, this, newInfo);
        this.mFolderIcon = inflateIcon;
        addView(inflateIcon);
    }

    public void release() {
        remove();
        this.mDragLayer = null;
        this.mFolderIcon = null;
    }

    public int getDragWidth() {
        return this.mWidth;
    }

    public int getDragHeight() {
        return this.mHeight;
    }

    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(View.MeasureSpec.makeMeasureSpec(this.mWidth, MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(this.mHeight, MeasureSpec.EXACTLY));
        FolderIcon folderIcon = this.mFolderIcon;
        if (folderIcon != null) {
            folderIcon.measure(View.MeasureSpec.makeMeasureSpec(this.mWidth, MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(this.mHeight, MeasureSpec.EXACTLY));
        }
    }

    public int getCoordinateX() {
        return this.mCoordinateXY[0];
    }

    public int getCoordinateY() {
        return this.mCoordinateXY[1];
    }

    public void show(int touchX, int touchY) {
        int[] iArr = this.mCoordinateXY;
        iArr[0] = touchX;
        iArr[1] = touchY;
        this.mDragLayer.addView(this);
        BaseDragLayer.LayoutParams lp = new BaseDragLayer.LayoutParams(this.mWidth, this.mHeight);
        lp.customPosition = true;
        setLayoutParams(lp);
        applyTranslation((float) touchX, (float) touchY);
    }

    public void remove() {
        if (getParent() != null) {
            this.mDragLayer.removeView(this);
        }
    }

    public void move(int touchX, int touchY) {
        int[] iArr = this.mCoordinateXY;
        iArr[0] = touchX;
        iArr[1] = touchY;
        applyTranslation((float) touchX, (float) touchY);
    }

    public void animateMove(float relativeX, float relativeY) {
        int[] iArr = this.mCoordinateXY;
        applyTranslation(((float) iArr[0]) + relativeX, ((float) iArr[1]) + relativeY);
    }

    private void applyTranslation(float translationX, float translationY) {
        setTranslationX(translationX);
        setTranslationY(translationY);
    }
}
