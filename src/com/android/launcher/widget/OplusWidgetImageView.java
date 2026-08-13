package com.android.launcher.widget;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;

import com.android.launcher3.model.WidgetItem;
import com.android.launcher3.widget.WidgetImageView;

/** OPPO widget-preview image renderer ported from the decoded launcher source. */
public class OplusWidgetImageView extends WidgetImageView {
    private WidgetItem mItem;

    public OplusWidgetImageView(Context context) {
        this(context, null);
    }

    public OplusWidgetImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OplusWidgetImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setWidgetItem(WidgetItem item) {
        mItem = item;
        invalidate();
    }

    @Override
    public Rect getBitmapBoundsPlugin() {
        Rect bounds = super.getBitmapBoundsPlugin();
        if (mItem != null && !mItem.isShortcut() && mItem.spanX == 1 && mItem.spanY == 1) {
            int quarterWidth = bounds.width() / 4;
            int quarterHeight = bounds.height() / 4;
            return new Rect(bounds.left + quarterWidth, bounds.top + quarterHeight,
                    bounds.left + quarterWidth * 3, bounds.top + quarterHeight * 3);
        }
        return bounds;
    }

    @Override
    protected void updateDstRectF() {
        if (mDrawable == null || mDrawable.getIntrinsicWidth() <= 0
                || mDrawable.getIntrinsicHeight() <= 0) {
            mDstRectF.setEmpty();
            return;
        }
        float width = getWidth();
        float height = getHeight();
        float availableWidth = width - getPaddingLeft() - getPaddingRight();
        float availableHeight = height - getPaddingTop() - getPaddingBottom();
        float scale = mDrawable.getIntrinsicWidth() / (float) mDrawable.getIntrinsicHeight()
                > availableWidth / availableHeight
                ? availableWidth / mDrawable.getIntrinsicWidth()
                : availableHeight / mDrawable.getIntrinsicHeight();
        float drawnWidth = mDrawable.getIntrinsicWidth() * scale;
        float drawnHeight = mDrawable.getIntrinsicHeight() * scale;
        mDstRectF.left = (width - drawnWidth) / 2f;
        mDstRectF.right = (width + drawnWidth) / 2f;
        if (drawnHeight > height) {
            mDstRectF.top = getPaddingTop();
            mDstRectF.bottom = drawnHeight;
        } else {
            mDstRectF.top = (height - drawnHeight) / 2f;
            mDstRectF.bottom = (height + drawnHeight) / 2f;
        }
    }
}
