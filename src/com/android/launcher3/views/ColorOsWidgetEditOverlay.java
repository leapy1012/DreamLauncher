package com.android.launcher3.views;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.ViewParent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.PathInterpolator;

import com.android.launcher3.CellLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.Workspace;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.model.data.LauncherAppWidgetInfo;
import com.android.launcher3.util.Themes;
import com.android.launcher3.widget.LauncherAppWidgetHostView;

import java.util.ArrayList;

/** OPPO-style widget edit decoration, hosted separately in DragLayer. */
public final class ColorOsWidgetEditOverlay implements ViewTreeObserver.OnPreDrawListener {

    private static final long BACKGROUND_DURATION_MS = 200L;
    private static final long INDICATOR_DURATION_MS = 150L;
    private static final PathInterpolator SWITCH_INTERPOLATOR =
            new PathInterpolator(0.33f, 0f, 0.67f, 1f);

    private final Launcher mLauncher;
    private final DragLayer mDragLayer;
    private final ArrayList<WidgetIndicatorView> mIndicators = new ArrayList<>();
    private boolean mAttached;

    public ColorOsWidgetEditOverlay(Launcher launcher) {
        mLauncher = launcher;
        mDragLayer = launcher.getDragLayer();
    }

    public void show() {
        if (mAttached) return;
        mAttached = true;
        rebuildIndicators();
        mDragLayer.getViewTreeObserver().addOnPreDrawListener(this);
        mDragLayer.post(this::rebuildIndicators);
    }

    public void hide(boolean animate) {
        if (!mAttached) return;
        mAttached = false;
        if (mDragLayer.getViewTreeObserver().isAlive()) {
            mDragLayer.getViewTreeObserver().removeOnPreDrawListener(this);
        }
        ArrayList<WidgetIndicatorView> oldIndicators = new ArrayList<>(mIndicators);
        mIndicators.clear();
        for (WidgetIndicatorView indicator : oldIndicators) {
            if (animate) {
                indicator.hide(() -> mDragLayer.removeView(indicator));
            } else {
                mDragLayer.removeView(indicator);
            }
        }
    }

    private void rebuildIndicators() {
        if (!mAttached) return;
        for (WidgetIndicatorView indicator : mIndicators) {
            mDragLayer.removeView(indicator);
        }
        mIndicators.clear();
        for (LauncherAppWidgetHostView host
                : LauncherAppWidgetHostView.getColorOsEditModeHosts()) {
            if (!isEligible(host)) continue;
            WidgetIndicatorView indicator = new WidgetIndicatorView(host);
            DragLayer.LayoutParams lp = new DragLayer.LayoutParams(1, 1);
            lp.customPosition = true;
            mDragLayer.addView(indicator, lp);
            mIndicators.add(indicator);
            updateBounds(indicator);
            indicator.show();
        }
    }

    private boolean isEligible(LauncherAppWidgetHostView host) {
        return host.isAttachedToWindow() && host.getVisibility() == View.VISIBLE
                && host.getWidth() > 0 && host.getHeight() > 0
                && host.getTag() instanceof LauncherAppWidgetInfo;
    }

    @Override
    public boolean onPreDraw() {
        if (!mAttached) return true;
        for (int i = mIndicators.size() - 1; i >= 0; i--) {
            WidgetIndicatorView indicator = mIndicators.get(i);
            if (!isEligible(indicator.mHost)) {
                mIndicators.remove(i);
                mDragLayer.removeView(indicator);
            } else {
                updateBounds(indicator);
            }
        }
        return true;
    }

    private void updateBounds(WidgetIndicatorView indicator) {
        LauncherAppWidgetHostView host = indicator.mHost;
        boolean onCurrentPage = isOnCurrentWorkspacePage(host);
        indicator.setVisibility(onCurrentPage ? View.VISIBLE : View.GONE);
        if (!onCurrentPage) {
            return;
        }
        Utilities.getBoundsForViewInDragLayer(mDragLayer, host,
                new Rect(0, 0, host.getWidth(), host.getHeight()), false,
                indicator.mCoordinateBuffer, indicator.mHostBounds);
        // OPPO draws this decoration as part of the page child, where Workspace clipping keeps
        // adjacent-page widgets out. This implementation is hosted in DragLayer, so reproduce
        // that ownership boundary explicitly and never expose a clipped indicator at an edge.
        if (indicator.mHostBounds.left < 0f
                || indicator.mHostBounds.right > mDragLayer.getWidth()
                || indicator.mHostBounds.width() < indicator.mIndicatorSize
                || indicator.mHostBounds.height() < indicator.mIndicatorSize) {
            indicator.setVisibility(View.GONE);
            return;
        }
        int left = Math.round(indicator.mHostBounds.left);
        int top = Math.round(indicator.mHostBounds.top);
        int width = Math.max(1, Math.round(indicator.mHostBounds.width()));
        int height = Math.max(1, Math.round(indicator.mHostBounds.height()));
        DragLayer.LayoutParams lp = (DragLayer.LayoutParams) indicator.getLayoutParams();
        if (lp.x != left || lp.y != top || lp.width != width || lp.height != height) {
            lp.x = left;
            lp.y = top;
            lp.width = width;
            lp.height = height;
            indicator.setLayoutParams(lp);

        }
    }
    private boolean isOnCurrentWorkspacePage(View host) {
        Workspace<?> workspace = mLauncher.getWorkspace();
        // Resolve ownership from the page that is physically centered. During the transition
        // into overview, PagedView's bookkeeping page can briefly differ from the page on screen.
        // An overlay hosted in DragLayer is not clipped by Workspace, so using that stale index
        // exposes a strip of an adjacent page's widget at the display edge.
        View currentPage = workspace.getChildAt(
                workspace.getPageNearestToCenterOfScreen());
        ViewParent parent = host.getParent();
        while (parent instanceof View) {
            if (parent instanceof CellLayout) {
                return parent == currentPage;
            }
            parent = parent.getParent();
        }
        return false;
    }

    private final class WidgetIndicatorView extends View {

        private final LauncherAppWidgetHostView mHost;
        private final Paint mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Drawable mRemoveDrawable;
        private final RectF mHostBounds = new RectF();
        private final float[] mCoordinateBuffer = new float[4];
        private final int mIndicatorSize;
        private final float mCornerRadius;
        private float mBackgroundFraction;
        private float mIndicatorFraction;
        private boolean mIndicatorPressed;
        private ValueAnimator mBackgroundAnimator;
        private ValueAnimator mIndicatorAnimator;

        WidgetIndicatorView(LauncherAppWidgetHostView host) {
            super(mLauncher);
            mHost = host;
            mRemoveDrawable = getContext().getDrawable(R.drawable.launcher_ic_widget_remove);
            mIndicatorSize = getResources().getDimensionPixelSize(
                    R.dimen.coloros_widget_delete_icon_size);
            mCornerRadius = getResources().getDimension(
                    R.dimen.coloros_widget_edit_corner_radius);
            setContentDescription(getResources().getString(R.string.coloros_remove_widget_title));
            mBackgroundPaint.setColor(Themes.getAttrBoolean(mLauncher, R.attr.isWorkspaceDarkText)
                    ? 0xFF000000 : 0xFFFFFFFF);
        }

        void show() {
            animateFractions(1f, null);
        }

        void hide(Runnable endAction) {
            animateFractions(0f, endAction);
        }

        private void animateFractions(float target, Runnable endAction) {
            if (mBackgroundAnimator != null) mBackgroundAnimator.cancel();
            if (mIndicatorAnimator != null) mIndicatorAnimator.cancel();
            mBackgroundAnimator = ValueAnimator.ofFloat(mBackgroundFraction, target);
            mBackgroundAnimator.setDuration(BACKGROUND_DURATION_MS);
            mBackgroundAnimator.setInterpolator(SWITCH_INTERPOLATOR);
            mBackgroundAnimator.addUpdateListener(animation -> {
                mBackgroundFraction = (float) animation.getAnimatedValue();
                invalidate();
            });
            mIndicatorAnimator = ValueAnimator.ofFloat(mIndicatorFraction, target);
            mIndicatorAnimator.setDuration(INDICATOR_DURATION_MS);
            mIndicatorAnimator.setInterpolator(SWITCH_INTERPOLATOR);
            mIndicatorAnimator.addUpdateListener(animation -> {
                mIndicatorFraction = (float) animation.getAnimatedValue();
                invalidate();
            });
            if (endAction != null) {
                mBackgroundAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        endAction.run();
                    }
                });
            }
            mBackgroundAnimator.start();
            mIndicatorAnimator.start();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            mBackgroundPaint.setAlpha(Math.round(40f * mBackgroundFraction));
            canvas.drawRoundRect(0f, 0f, getWidth(), getHeight(), mCornerRadius, mCornerRadius,
                    mBackgroundPaint);
            if (mRemoveDrawable == null || mIndicatorFraction <= 0f) return;
            int left = Utilities.isRtl(getResources()) ? 0 : getWidth() - mIndicatorSize;
            mRemoveDrawable.setBounds(left, 0, left + mIndicatorSize, mIndicatorSize);
            mRemoveDrawable.setAlpha(Math.round(255f * mIndicatorFraction));
            mRemoveDrawable.draw(canvas);
        }

        private boolean isInIndicator(MotionEvent event) {
            float left = Utilities.isRtl(getResources()) ? 0 : getWidth() - mIndicatorSize;
            return event.getX() >= left && event.getX() <= left + mIndicatorSize
                    && event.getY() >= 0 && event.getY() <= mIndicatorSize;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    mIndicatorPressed = isInIndicator(event);
                    return mIndicatorPressed;
                case MotionEvent.ACTION_UP:
                    boolean remove = mIndicatorPressed && isInIndicator(event);
                    mIndicatorPressed = false;
                    if (remove) {
                        performClick();
                        mHost.showColorOsRemoveWidgetDialog();
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    mIndicatorPressed = false;
                    return true;
                default:
                    return mIndicatorPressed;
            }
        }

        @Override
        public boolean performClick() {
            super.performClick();
            return true;
        }
    }
}
