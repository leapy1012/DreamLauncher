package com.android.launcher3.editselection;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.PathInterpolator;

import androidx.annotation.Nullable;

import com.android.launcher3.CellLayout;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutAndWidgetContainer;
import com.android.launcher3.model.data.ItemInfo;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Mini page thumbnail for the edit-selection strip (Oppo {@code PagePreviewItemView} style).
 * <p>
 * Icon chips: square rounded rects ({@code preview_item_cell_width}=6dp, radius=2dp).
 * Selection stroke: Oppo {@code PressFeedbackPreviewWrapper.selectedStrokeAnimation}
 * (280ms in / 150ms out, PathInterpolator 0.33,0,0.67,1).
 */
public class EditSelectionPagePreviewView extends View {

    private static final int COLOR_CELL_SELECTED = 0xFF3478F6;
    private static final int COLOR_CELL_ACTIVE_PAGE = 0xFFFFFFFF;
    private static final int COLOR_CELL_OTHER_PAGE = 0x66FFFFFF;
    private static final int COLOR_STROKE = 0xFFFFFFFF;

    /** Oppo {@code ToggleBarAnimHelper.INTERPOLATOR_SELECTED_STROKE}. */
    private static final PathInterpolator STROKE_INTERPOLATOR =
            new PathInterpolator(0.33f, 0.0f, 0.67f, 1.0f);
    private static final long STROKE_IN_MS = 280L;
    private static final long STROKE_OUT_MS = 150L;

    private final Paint mCellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mTmpRect = new RectF();
    private final RectF mStrokeRect = new RectF();
    private int mCountX = 4;
    private int mCountY = 6;
    @Nullable
    private CellLayout mCellLayout;
    private boolean mSelectedPage;
    private Set<ItemInfo> mSelectedItems = Collections.emptySet();

    private float mCellSize;
    private float mCellRadius;
    private float mCellPadding;
    private float mWidgetRadius;
    private float mCornerRadius;
    private float mStrokeWidth;
    private int mStrokeAlpha;
    @Nullable
    private ValueAnimator mStrokeAnimator;

    public EditSelectionPagePreviewView(Context context) {
        this(context, null);
    }

    public EditSelectionPagePreviewView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EditSelectionPagePreviewView(Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mCellPaint.setStyle(Paint.Style.FILL);
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mCellSize = getResources().getDimension(R.dimen.edit_selection_page_preview_cell_size);
        mCellRadius = getResources().getDimension(R.dimen.edit_selection_page_preview_cell_radius);
        mCellPadding = getResources().getDimension(R.dimen.edit_selection_page_preview_cell_padding);
        mWidgetRadius = getResources().getDimension(R.dimen.edit_selection_page_preview_widget_radius);
        mCornerRadius = getResources().getDimension(R.dimen.edit_selection_page_preview_corner_radius);
        mStrokeWidth = getResources().getDimension(R.dimen.edit_selection_page_preview_stroke_width);
        mStrokePaint.setStrokeWidth(mStrokeWidth);
        setBackgroundResource(R.drawable.edit_selection_page_preview_bg);
        setSelectedPage(false, false);
    }

    public void bind(@Nullable CellLayout cellLayout, int countX, int countY, boolean selectedPage,
            @Nullable Set<ItemInfo> selectedItems) {
        mCellLayout = cellLayout;
        mCountX = Math.max(1, countX);
        mCountY = Math.max(1, countY);
        mSelectedItems = selectedItems != null
                ? Collections.unmodifiableSet(new HashSet<>(selectedItems))
                : Collections.emptySet();
        setSelectedPage(selectedPage, false);
        invalidate();
    }

    public void setSelectedPage(boolean selected) {
        setSelectedPage(selected, true);
    }

    /**
     * @param animate when true, matches Oppo stroke fade; false snaps (bind / first layout).
     */
    public void setSelectedPage(boolean selected, boolean animate) {
        int targetAlpha = selected ? 255 : 0;
        if (mSelectedPage == selected) {
            // Repair cancelled mid-stroke so early-return never leaves a partial border.
            if (!animate || mStrokeAlpha != targetAlpha) {
                cancelStrokeAnimator();
                mStrokeAlpha = targetAlpha;
                invalidate();
            }
            return;
        }
        mSelectedPage = selected;
        if (animate) {
            // Chip colors flip immediately; stroke alpha eases (Oppo setSelected).
            invalidate();
            animateStroke(selected);
        } else {
            cancelStrokeAnimator();
            mStrokeAlpha = targetAlpha;
            invalidate();
        }
    }

    public void setSelectedItems(@Nullable Set<ItemInfo> selectedItems) {
        mSelectedItems = selectedItems != null
                ? Collections.unmodifiableSet(new HashSet<>(selectedItems))
                : Collections.emptySet();
        invalidate();
    }

    private void animateStroke(boolean selected) {
        cancelStrokeAnimator();
        int target = selected ? 255 : 0;
        if (mStrokeAlpha == target) {
            invalidate();
            return;
        }
        ValueAnimator anim = ValueAnimator.ofInt(mStrokeAlpha, target);
        mStrokeAnimator = anim;
        anim.setDuration(selected ? STROKE_IN_MS : STROKE_OUT_MS);
        anim.setInterpolator(STROKE_INTERPOLATOR);
        anim.addUpdateListener(a -> {
            mStrokeAlpha = (Integer) a.getAnimatedValue();
            invalidate();
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mStrokeAnimator == animation) {
                    mStrokeAnimator = null;
                }
            }
        });
        anim.start();
    }

    private void cancelStrokeAnimator() {
        if (mStrokeAnimator != null) {
            mStrokeAnimator.cancel();
            mStrokeAnimator = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelStrokeAnimator();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawCells(canvas);
        drawSelectionStroke(canvas);
    }

    private void drawSelectionStroke(Canvas canvas) {
        if (mStrokeAlpha <= 0 || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        float half = mStrokeWidth / 2f;
        mStrokeRect.set(half, half, getWidth() - half, getHeight() - half);
        mStrokePaint.setColor(Color.argb(mStrokeAlpha,
                Color.red(COLOR_STROKE), Color.green(COLOR_STROKE), Color.blue(COLOR_STROKE)));
        float radius = Math.max(0f, mCornerRadius - mStrokeWidth);
        canvas.drawRoundRect(mStrokeRect, radius, radius, mStrokePaint);
    }

    private void drawCells(Canvas canvas) {
        if (mCellLayout == null) {
            return;
        }
        ShortcutAndWidgetContainer container = mCellLayout.getShortcutsAndWidgets();
        if (container == null) {
            return;
        }

        // Oppo PagePreviewItemView: fixed square chips + distribute remaining space as gaps.
        float padH = mCellPadding;
        float padV = mCellPadding;
        float contentW = getWidth() - padH * 2f;
        float contentH = getHeight() - padV * 2f;
        float cellsW = mCellSize * mCountX;
        float cellsH = mCellSize * mCountY;
        // Shrink padding if the fixed chip grid would overflow (Oppo onLayout loop).
        if (cellsW > contentW && contentW > 0) {
            padH = Math.max(0f, (getWidth() - cellsW) / 2f);
            contentW = getWidth() - padH * 2f;
        }
        if (cellsH > contentH && contentH > 0) {
            padV = Math.max(0f, (getHeight() - cellsH) / 2f);
            contentH = getHeight() - padV * 2f;
        }
        if (contentW <= 0 || contentH <= 0) {
            return;
        }

        float gapX = mCountX > 1 ? Math.max(0f, (contentW - cellsW) / (mCountX - 1)) : 0f;
        float gapY = mCountY > 1 ? Math.max(0f, (contentH - cellsH) / (mCountY - 1)) : 0f;
        float strideX = mCellSize + gapX;
        float strideY = mCellSize + gapY;

        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child.getVisibility() != VISIBLE || child.getAlpha() <= 0f) {
                continue;
            }
            Object tag = child.getTag();
            if (!(tag instanceof ItemInfo info)) {
                continue;
            }
            int x = info.cellX;
            int y = info.cellY;
            if (x < 0 || y < 0) {
                continue;
            }
            int spanX = Math.max(1, info.spanX);
            int spanY = Math.max(1, info.spanY);
            boolean isSelectedItem = mSelectedItems.contains(info);
            if (isSelectedItem) {
                mCellPaint.setColor(COLOR_CELL_SELECTED);
            } else if (mSelectedPage) {
                mCellPaint.setColor(COLOR_CELL_ACTIVE_PAGE);
            } else {
                mCellPaint.setColor(COLOR_CELL_OTHER_PAGE);
            }

            // Oppo cellItemToPreviewRect: span expands by cell+gap.
            float left = padH + x * strideX;
            float top = padV + y * strideY;
            float right = left + spanX * mCellSize + (spanX - 1) * gapX;
            float bottom = top + spanY * mCellSize + (spanY - 1) * gapY;
            mTmpRect.set(left, top, right, bottom);
            float radius = (spanX == 1 && spanY == 1) ? mCellRadius : mWidgetRadius;
            canvas.drawRoundRect(mTmpRect, radius, radius, mCellPaint);
        }
    }
}
