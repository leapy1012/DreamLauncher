package com.android.launcher3.allapps.coloros;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.R;

/**
 * Oppo {@code FocusedItemDecorator} drag placeholders: white stroke round-rects at the
 * current (and previous) category slot while reordering.
 *
 * <p>Uses layout bounds ({@link View#getLeft()}/{@link View#getTop()}) so the outline stays
 * in the empty home slot while {@link androidx.recyclerview.widget.ItemTouchHelper} moves the
 * card via translation.
 */
final class ColorOsCategoryDragDecoration extends RecyclerView.ItemDecoration {

    /** Oppo {@code FocusedItemDecorator.MAX_ALPHA} — 51/255 ≈ 20% white. */
    private static final float MAX_ALPHA = 51f;

    private final Paint mPaint;
    private final Paint mLastPaint;
    private final float mDefaultRadius;
    private final float mDefaultPadding;

    private int mCurrentPosition = -1;
    private int mLastPosition = -1;
    private float mBgHeight;
    private float mBgPadding;
    private float mBgRadius;
    private float mBgAlpha;
    private float mLastBgAlpha;

    ColorOsCategoryDragDecoration(@NonNull Context context) {
        float stroke = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 1f, context.getResources().getDisplayMetrics());
        mDefaultRadius = context.getResources().getDimension(R.dimen.coloros_category_card_radius);
        mDefaultPadding = context.getResources().getDimension(
                R.dimen.coloros_category_folder_padding) / 2f;
        mBgRadius = mDefaultRadius;
        mBgPadding = mDefaultPadding;

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(stroke);
        mPaint.setColor(0xFFFFFFFF);

        mLastPaint = new Paint(mPaint);
    }

    void updateDraggedPosition(int position) {
        mCurrentPosition = position;
    }

    void updateLastPosition(int position) {
        mLastPosition = position;
    }

    void setBgHeight(float height) {
        mBgHeight = height;
    }

    void setBgPadding(float padding) {
        mBgPadding = padding;
    }

    void setBgRadius(float radius) {
        mBgRadius = radius;
    }

    float getBgAlpha() {
        return mBgAlpha;
    }

    void setBgAlpha(float alpha) {
        mBgAlpha = Math.max(0f, Math.min(1f, alpha));
    }

    float getLastBgAlpha() {
        return mLastBgAlpha;
    }

    void setLastBgAlpha(float alpha) {
        mLastBgAlpha = Math.max(0f, Math.min(1f, alpha));
    }

    void clear() {
        mCurrentPosition = -1;
        mLastPosition = -1;
        mBgAlpha = 0f;
        mLastBgAlpha = 0f;
    }

    /** Measure card height/padding from a category row before first outline draw. */
    void captureMetricsFrom(@NonNull View itemView) {
        View card = findCard(itemView);
        if (card == null) {
            mBgHeight = itemView.getHeight();
            return;
        }
        float scaleY = itemView.getScaleY() == 0f ? 1f : itemView.getScaleY();
        mBgHeight = card.getHeight() / scaleY;
        ViewGroup.MarginLayoutParams lp = card.getLayoutParams() instanceof ViewGroup.MarginLayoutParams
                ? (ViewGroup.MarginLayoutParams) card.getLayoutParams() : null;
        if (lp != null) {
            mBgPadding = (lp.getMarginStart() + lp.getMarginEnd()) / 2f;
            if (mBgPadding <= 0f) {
                mBgPadding = mDefaultPadding;
            }
        } else {
            mBgPadding = mDefaultPadding;
        }
        mBgRadius = mDefaultRadius;
    }

    @Override
    public void onDraw(@NonNull Canvas canvas, @NonNull RecyclerView parent,
            @NonNull RecyclerView.State state) {
        if (mCurrentPosition == -1) {
            return;
        }
        drawHolderBackground(canvas, parent, mCurrentPosition, mPaint, mBgAlpha);
        drawHolderBackground(canvas, parent, mLastPosition, mLastPaint, mLastBgAlpha);
    }

    private void drawHolderBackground(@NonNull Canvas canvas, @NonNull RecyclerView parent,
            int position, @NonNull Paint paint, float alpha) {
        if (position < 0 || alpha <= 0f) {
            return;
        }
        RecyclerView.ViewHolder vh = parent.findViewHolderForAdapterPosition(position);
        if (vh == null) {
            return;
        }
        View item = vh.itemView;
        View card = findCard(item);
        float top;
        float left;
        float right;
        float height = mBgHeight;
        if (card != null) {
            // Layout coords (ignore translation) so outline stays in the empty slot.
            left = item.getLeft() + card.getLeft();
            top = item.getTop() + card.getTop();
            right = left + card.getWidth();
            if (height <= 0f) {
                height = card.getHeight();
            }
        } else {
            left = item.getLeft() + mBgPadding;
            top = item.getTop() + item.getPaddingTop();
            right = item.getRight() - mBgPadding;
            if (height <= 0f) {
                height = item.getHeight();
            }
        }
        paint.setAlpha((int) (alpha * MAX_ALPHA));
        RectF rect = new RectF(left, top, right, top + height);
        float radius = mBgRadius > 0f ? mBgRadius : mDefaultRadius;
        canvas.drawRoundRect(rect, radius, radius, paint);
    }

    @Nullable
    private static View findCard(@NonNull View itemView) {
        if (!(itemView instanceof ViewGroup)) {
            return null;
        }
        ViewGroup root = (ViewGroup) itemView;
        return root.getChildCount() > 0 ? root.getChildAt(0) : null;
    }
}
