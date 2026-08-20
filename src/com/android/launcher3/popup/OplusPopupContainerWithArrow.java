package com.android.launcher3.popup;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.android.launcher3.InsettableFrameLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.model.data.FolderInfo;
import com.coui.appcompat.animation.COUISpringInterpolator;

/** ColorOS popup shell reconstructed from the decoded Oplus launcher implementation. */
public class OplusPopupContainerWithArrow extends PopupContainerWithArrow<Launcher> {

    private static final int SCALE_DURATION_MS = 330;
    private static final int ALPHA_DURATION_MS = 250;

    public OplusPopupContainerWithArrow(Context context) {
        this(context, null);
    }

    public OplusPopupContainerWithArrow(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OplusPopupContainerWithArrow(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void assignMarginsAndBackgrounds(ViewGroup viewGroup) {
        // The scroll container is one rounded Oplus surface. AOSP would replace it and each row
        // with segmented Material-U backgrounds.
        measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
    }

    @Override
    protected boolean shouldAddArrow() {
        return false;
    }

    /**
     * Folder-only port of OplusPopupContainerWithArrow.orientAboutIcon(). Unlike AOSP, ColorOS
     * aligns the card edge to the folder bounds and chooses above/below from its workspace row.
     */
    @Override
    protected void orientAboutObject() {
        if (getFolderIcon() == null) {
            super.orientAboutObject();
            return;
        }
        measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        getTargetObjectLocation(mTempRect);
        InsettableFrameLayout dragLayer = (InsettableFrameLayout) getPopupContainer();
        Rect insets = dragLayer.getInsets();
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int gap = getResources().getDimensionPixelSize(R.dimen.popup_vertical_padding);

        int left = mTempRect.left;
        if (left + width > dragLayer.getWidth() - insets.right) {
            left = mTempRect.right - width;
        }
        left = Math.max(insets.left,
                Math.min(left, dragLayer.getWidth() - insets.right - width));
        mIsLeftAligned = left == mTempRect.left;
        setX(left - insets.left);

        FolderInfo info = (FolderInfo) getFolderIcon().getTag();
        int lastCellY = info.cellY + Math.max(1, info.spanY) - 1;
        int rowsBelow = mActivityContext.getDeviceProfile().inv.numRows - 1 - lastCellY;
        boolean preferAbove = info.cellY > rowsBelow;
        int spaceAbove = mTempRect.top - insets.top - gap;
        int spaceBelow = dragLayer.getHeight() - insets.bottom - mTempRect.bottom - gap;
        mIsAboveIcon = preferAbove ? spaceAbove >= height
                : spaceBelow < height && spaceAbove >= height;
        mGravity = 0;

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        if (mIsAboveIcon) {
            lp.gravity = Gravity.BOTTOM;
            lp.topMargin = 0;
            lp.bottomMargin = dragLayer.getHeight() - mTempRect.top + gap;
        } else {
            lp.gravity = Gravity.TOP;
            lp.bottomMargin = 0;
            lp.topMargin = mTempRect.bottom + gap;
        }
        setLayoutParams(lp);
    }

    @Override
    protected void onCreateOpenAnimation(AnimatorSet animation) {
        applyOplusTiming(animation);
    }

    @Override
    protected void onCreateCloseAnimation(AnimatorSet animation) {
        applyOplusTiming(animation);
    }

    private void applyOplusTiming(AnimatorSet animation) {
        COUISpringInterpolator spring = new COUISpringInterpolator(0.4d, 0.0d);
        for (Animator child : animation.getChildAnimations()) {
            if (!(child instanceof ObjectAnimator)) {
                continue;
            }
            String property = ((ObjectAnimator) child).getPropertyName();
            if (ALPHA.getName().equals(property)) {
                child.setDuration(ALPHA_DURATION_MS);
            } else if (SCALE_X.getName().equals(property) || SCALE_Y.getName().equals(property)) {
                child.setDuration(SCALE_DURATION_MS);
                child.setInterpolator(spring);
            }
        }
    }
}
