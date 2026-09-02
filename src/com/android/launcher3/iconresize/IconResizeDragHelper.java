package com.android.launcher3.iconresize;

import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.model.data.ItemInfo;

/**
 * Oppo-style spring resize preview during handle drag: live geometry + backdrop blur.
 */
public final class IconResizeDragHelper {

    private static final String TAG = "IconResizeDragHelper";
    private static final float SPRING_STIFFNESS = 680f;
    private static final float SPRING_DAMPING = 0.82f;

    private final Launcher mLauncher;
    private final BubbleTextView mIcon;
    private final AppIconResizeFrame mFrame;
    private final CellLayout mCellLayout;
    private final IconResizeBlurHelper mBlurHelper = new IconResizeBlurHelper();
    private final Rect mIconRect = new Rect();
    private final Rect mFrameRect = new Rect();

    private int mStartSpanX;
    private int mStartSpanY;
    private int mPreviewSpanX;
    private int mPreviewSpanY;
    private float mDragStartX;
    private float mDragStartY;

    private float mAnimLeft;
    private float mAnimTop;
    private float mAnimWidth;
    private float mAnimHeight;
    private float mAnimRadius;
    private float mFrameLeft;
    private float mFrameTop;
    private float mFrameWidth;
    private float mFrameHeight;

    @Nullable
    private SpringAnimation mLeftSpring;
    @Nullable
    private SpringAnimation mTopSpring;
    @Nullable
    private SpringAnimation mWidthSpring;
    @Nullable
    private SpringAnimation mHeightSpring;
    @Nullable
    private SpringAnimation mRadiusSpring;
    private boolean mActive;

    public IconResizeDragHelper(Launcher launcher, BubbleTextView icon,
            AppIconResizeFrame frame, CellLayout cellLayout) {
        mLauncher = launcher;
        mIcon = icon;
        mFrame = frame;
        mCellLayout = cellLayout;
    }

    public IconResizeBlurHelper getBlurHelper() {
        return mBlurHelper;
    }

    public boolean isActive() {
        return mActive;
    }

    public void start() {
        ItemInfo info = (ItemInfo) mIcon.getTag();
        mStartSpanX = info.spanX;
        mStartSpanY = info.spanY;
        mPreviewSpanX = mStartSpanX;
        mPreviewSpanY = mStartSpanY;
        mActive = true;

        IconResizeHelper.getIconBoundsForSpan(mIcon, mStartSpanX, mStartSpanY, mIconRect);
        mAnimLeft = mIconRect.left;
        mAnimTop = mIconRect.top;
        mAnimWidth = mIconRect.width();
        mAnimHeight = mIconRect.height();
        mAnimRadius = MorphShapeHelper.getFallbackCornerRadius(
                mStartSpanX, mStartSpanY, mIconRect.width(), mIconRect.height());

        mFrameLeft = mAnimLeft;
        mFrameTop = mAnimTop;
        mFrameWidth = mAnimWidth;
        mFrameHeight = mAnimHeight;

        mIcon.enterResizePreviewMode(buildPreviewParams());
        syncFrameLayout(false);
        syncBlurViewLayout();
        invalidateAll();
        Log.d(TAG, "start preview at " + mStartSpanX + "x" + mStartSpanY);
    }

    public void onDragMove(float rawX, float rawY) {
        if (!mActive) {
            return;
        }
        float deltaX = rawX - mDragStartX;
        float deltaY = rawY - mDragStartY;
        int spanX = spanFromDelta(mStartSpanX, deltaX, true);
        int spanY = spanFromDelta(mStartSpanY, deltaY, false);
        spanX = IconResizeHelper.normalizeSpan(spanX);
        spanY = IconResizeHelper.normalizeSpan(spanY);
        if (!IconResizeHelper.isValidPreset(spanX, spanY)) {
            return;
        }
        if (spanX == mPreviewSpanX && spanY == mPreviewSpanY) {
            updateContinuousStretch(deltaX, deltaY);
            return;
        }
        applyPreviewSpan(spanX, spanY);
    }

    public void onDragStart(float rawX, float rawY) {
        mDragStartX = rawX;
        mDragStartY = rawY;
    }

    public void commit() {
        if (!mActive) {
            return;
        }
        cancelSprings();
        int spanX = mPreviewSpanX;
        int spanY = mPreviewSpanY;
        mBlurHelper.clear();
        mIcon.exitResizePreviewMode();
        mActive = false;
        IconResizeHelper.applyIconSpan(mLauncher, mIcon, spanX, spanY, true, false);
    }

    public void cancel() {
        if (!mActive) {
            return;
        }
        cancelSprings();
        mBlurHelper.clear();
        ItemInfo info = (ItemInfo) mIcon.getTag();
        mIcon.exitResizePreviewMode();
        mActive = false;
        if (info.spanX != mStartSpanX || info.spanY != mStartSpanY) {
            IconResizeHelper.applyIconSpan(mLauncher, mIcon, mStartSpanX, mStartSpanY, false, true);
        }
    }

    /** Sub-threshold stretch for live feedback before span snaps. */
    private void updateContinuousStretch(float deltaX, float deltaY) {
        float xThreshold = mCellLayout.getCellWidth() * 0.45f;
        float yThreshold = mCellLayout.getCellHeight() * 0.45f;
        float stretchX = Math.max(-0.15f, Math.min(0.15f, deltaX / xThreshold * 0.12f));
        float stretchY = Math.max(-0.15f, Math.min(0.15f, deltaY / yThreshold * 0.12f));

        IconResizeHelper.getIconBoundsForSpan(mIcon, mPreviewSpanX, mPreviewSpanY, mIconRect);
        float baseW = mIconRect.width();
        float baseH = mIconRect.height();
        mAnimWidth = baseW * (1f + stretchX);
        mAnimHeight = baseH * (1f + stretchY);
        mFrameWidth = mAnimWidth;
        mFrameHeight = mAnimHeight;
        pushPreviewToIcon();
        updateBlur(false);
        syncFrameLayout(false);
        invalidateAll();
    }

    private void applyPreviewSpan(int spanX, int spanY) {
        if (!IconResizeHelper.canPlaceSpan(mLauncher, mIcon, spanX, spanY)) {
            return;
        }
        if (!IconResizeHelper.applyIconSpan(mLauncher, mIcon, spanX, spanY, false, true)) {
            return;
        }
        mPreviewSpanX = spanX;
        mPreviewSpanY = spanY;

        Rect from = new Rect(
                Math.round(mAnimLeft), Math.round(mAnimTop),
                Math.round(mAnimLeft + mAnimWidth), Math.round(mAnimTop + mAnimHeight));
        MorphIconTransitionHelper.cancel(mIcon);
        IconResizeHelper.getIconBoundsForSpan(mIcon, spanX, spanY, mIconRect);

        springToBounds(from, mIconRect);
        Log.d(TAG, "preview span " + spanX + "x" + spanY);
    }

    private void springToBounds(Rect from, Rect to) {
        cancelSprings();
        mAnimLeft = from.left;
        mAnimTop = from.top;
        mAnimWidth = from.width();
        mAnimHeight = from.height();
        mFrameLeft = from.left;
        mFrameTop = from.top;
        mFrameWidth = from.width();
        mFrameHeight = from.height();

        float targetRadius = MorphShapeHelper.getFallbackCornerRadius(
                mPreviewSpanX, mPreviewSpanY, to.width(), to.height());

        mLeftSpring = createSpring(LEFT, mAnimLeft, to.left);
        mTopSpring = createSpring(TOP, mAnimTop, to.top);
        mWidthSpring = createSpring(WIDTH, mAnimWidth, to.width());
        mHeightSpring = createSpring(HEIGHT, mAnimHeight, to.height());
        mRadiusSpring = createSpring(RADIUS, mAnimRadius, targetRadius);
        // Oppo: frame snaps to target while icon springs — size mismatch drives blur.
        mFrameLeft = to.left;
        mFrameTop = to.top;
        mFrameWidth = to.width();
        mFrameHeight = to.height();

        Runnable onUpdate = () -> {
            pushPreviewToIcon();
            updateBlur(true);
            syncFrameLayout(false);
            invalidateAll();
        };
        mLeftSpring.addUpdateListener((a, v, vel) -> onUpdate.run());
        mTopSpring.addUpdateListener((a, v, vel) -> onUpdate.run());
        mWidthSpring.addUpdateListener((a, v, vel) -> onUpdate.run());
        mHeightSpring.addUpdateListener((a, v, vel) -> onUpdate.run());
        mRadiusSpring.addUpdateListener((a, v, vel) -> onUpdate.run());
        mHeightSpring.addEndListener((a, c, v, vel) -> {
            updateBlur(false);
            mBlurHelper.fadeOut(this::invalidateAll);
        });

        pushPreviewToIcon();
        startSpring(mLeftSpring);
        startSpring(mTopSpring);
        startSpring(mWidthSpring);
        startSpring(mHeightSpring);
        startSpring(mRadiusSpring);
    }

    private void pushPreviewToIcon() {
        IconResizePreviewParams params = buildPreviewParams();
        mIcon.updateResizePreviewLayout(params);
    }

    private IconResizePreviewParams buildPreviewParams() {
        IconResizePreviewParams params = new IconResizePreviewParams(
                mAnimRadius,
                Math.round(mAnimWidth),
                Math.round(mAnimHeight),
                mAnimLeft,
                mAnimTop);
        params.setSpanX(mPreviewSpanX);
        params.setSpanY(mPreviewSpanY);
        return params;
    }

    private void updateBlur(boolean animBlock) {
        mIconRect.set(
                Math.round(mAnimLeft), Math.round(mAnimTop),
                Math.round(mAnimLeft + mAnimWidth), Math.round(mAnimTop + mAnimHeight));
        mFrameRect.set(
                Math.round(mFrameLeft), Math.round(mFrameTop),
                Math.round(mFrameLeft + mFrameWidth), Math.round(mFrameTop + mFrameHeight));
        mBlurHelper.updateBlur(mIconRect, mFrameRect, animBlock, this::invalidateAll);
        syncBlurViewLayout();
        mBlurHelper.applyTo(mFrame.getBlurView());
    }

    /** Oppo BlurView: clipped to live icon bounds inside the resize frame. */
    private void syncBlurViewLayout() {
        IconResizeBlurHelper.IconResizeBlurView blurView = mFrame.getBlurView();
        if (blurView == null) {
            return;
        }
        LinearLayout.LayoutParams blp = (LinearLayout.LayoutParams) blurView.getLayoutParams();
        int left = Math.round(mAnimLeft - mFrameLeft);
        int top = Math.round(mAnimTop - mFrameTop);
        int w = Math.max(1, Math.round(mAnimWidth));
        int h = Math.max(1, Math.round(mAnimHeight));
        if (blp.leftMargin != left || blp.topMargin != top || blp.width != w || blp.height != h) {
            blp.leftMargin = left;
            blp.topMargin = top;
            blp.width = w;
            blp.height = h;
            blurView.setLayoutParams(blp);
        }
        blurView.setCornerRadius(mAnimRadius);
    }

    private void syncFrameLayout(boolean animate) {
        if (mFrame.getDragLayer() == null) {
            return;
        }
        mFrame.getDragLayer().getDescendantRectRelativeToSelf(mIcon, mIconRect);
        int x = mIconRect.left + Math.round(mFrameLeft);
        int y = mIconRect.top + Math.round(mFrameTop);
        int w = Math.round(mFrameWidth);
        int h = Math.round(mFrameHeight);
        if (animate) {
            MorphIconTransitionHelper.animateFrameTo(mFrame, x, y, w, h, null);
        } else {
            DragLayer.LayoutParams lp = (DragLayer.LayoutParams) mFrame.getLayoutParams();
            lp.x = x;
            lp.y = y;
            lp.width = w;
            lp.height = h;
            mFrame.requestLayout();
        }
        syncBlurViewLayout();
    }

    private void invalidateAll() {
        mIcon.invalidate();
        mFrame.invalidate();
        mBlurHelper.applyTo(mFrame.getBlurView());
    }

    private int spanFromDelta(int startSpan, float delta, boolean horizontal) {
        float threshold = (horizontal ? mCellLayout.getCellWidth() : mCellLayout.getCellHeight())
                * 0.45f;
        if (delta > threshold) {
            return 2;
        }
        if (delta < -threshold) {
            return 1;
        }
        return startSpan;
    }

    private void cancelSprings() {
        cancelSpring(mLeftSpring);
        cancelSpring(mTopSpring);
        cancelSpring(mWidthSpring);
        cancelSpring(mHeightSpring);
        cancelSpring(mRadiusSpring);
        mLeftSpring = mTopSpring = mWidthSpring = mHeightSpring = mRadiusSpring = null;
    }

    private static void cancelSpring(@Nullable SpringAnimation spring) {
        if (spring != null) {
            spring.cancel();
        }
    }

    private static void startSpring(@Nullable SpringAnimation spring) {
        if (spring != null) {
            spring.start();
        }
    }

    private SpringAnimation createSpring(FloatPropertyCompat<IconResizeDragHelper> prop,
            float start, float end) {
        SpringAnimation anim = new SpringAnimation(this, prop, end);
        anim.setStartValue(start);
        anim.setSpring(new SpringForce(end)
                .setStiffness(SPRING_STIFFNESS)
                .setDampingRatio(SPRING_DAMPING));
        return anim;
    }

    private static final FloatPropertyCompat<IconResizeDragHelper> LEFT =
            floatProp("left", (h, v) -> h.mAnimLeft = v, h -> h.mAnimLeft);
    private static final FloatPropertyCompat<IconResizeDragHelper> TOP =
            floatProp("top", (h, v) -> h.mAnimTop = v, h -> h.mAnimTop);
    private static final FloatPropertyCompat<IconResizeDragHelper> WIDTH =
            floatProp("width", (h, v) -> h.mAnimWidth = v, h -> h.mAnimWidth);
    private static final FloatPropertyCompat<IconResizeDragHelper> HEIGHT =
            floatProp("height", (h, v) -> h.mAnimHeight = v, h -> h.mAnimHeight);
    private static final FloatPropertyCompat<IconResizeDragHelper> RADIUS =
            floatProp("radius", (h, v) -> h.mAnimRadius = v, h -> h.mAnimRadius);
    private interface FloatSetter {
        void set(IconResizeDragHelper helper, float value);
    }

    private interface FloatGetter {
        float get(IconResizeDragHelper helper);
    }

    private static FloatPropertyCompat<IconResizeDragHelper> floatProp(
            String name, FloatSetter setter, FloatGetter getter) {
        return new FloatPropertyCompat<IconResizeDragHelper>(name) {
            @Override
            public float getValue(IconResizeDragHelper object) {
                return getter.get(object);
            }

            @Override
            public void setValue(IconResizeDragHelper object, float value) {
                setter.set(object, value);
            }
        };
    }
}
