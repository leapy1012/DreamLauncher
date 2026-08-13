package com.android.launcher3.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.PathInterpolator;

import com.android.launcher3.R;

/** ColorOS widget cell with OPPO's decoded 0.9 press-scale response. */
public class OplusWidgetCell extends WidgetCell {
    private static final PathInterpolator PRESS_INTERPOLATOR =
            new PathInterpolator(0.4f, 0f, 0.2f, 1f);
    private ValueAnimator mScaleAnimator;

    public OplusWidgetCell(Context context) {
        this(context, null);
    }

    public OplusWidgetCell(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OplusWidgetCell(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setColorOsPreviewSize(getResources().getDimensionPixelSize(
                R.dimen.toggle_bar_widget_item_width));
        setOnTouchListener(this::onCellTouch);
    }

    private boolean onCellTouch(View view, MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            animatePreviewScale(true);
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP
                || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            animatePreviewScale(false);
        }
        return false;
    }

    private void animatePreviewScale(boolean pressed) {
        View target = getAppWidgetHostViewPreview() != null
                ? getAppWidgetHostViewPreviewContainer() : getWidgetView();
        if (target == null) return;
        if (mScaleAnimator != null) mScaleAnimator.cancel();
        mScaleAnimator = ValueAnimator.ofFloat(target.getScaleX(), pressed ? 0.9f : 1f);
        mScaleAnimator.setDuration(200);
        mScaleAnimator.setInterpolator(PRESS_INTERPOLATOR);
        mScaleAnimator.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            target.setScaleX(scale);
            target.setScaleY(scale);
        });
        mScaleAnimator.start();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return true;
    }
}
