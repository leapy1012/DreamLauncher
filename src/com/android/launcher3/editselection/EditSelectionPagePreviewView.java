package com.android.launcher3.editselection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

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
 * Colors:
 * <ul>
 *   <li>Selected icons → accent blue</li>
 *   <li>Unselected on the <b>current</b> page → white</li>
 *   <li>Unselected on other pages → gray</li>
 * </ul>
 */
public class EditSelectionPagePreviewView extends View {

    private static final int COLOR_CELL_SELECTED = 0xFF3478F6;
    private static final int COLOR_CELL_ACTIVE_PAGE = 0xFFFFFFFF;
    private static final int COLOR_CELL_OTHER_PAGE = 0x66FFFFFF;

    private final Paint mCellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mTmpRect = new RectF();
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
        mCellSize = getResources().getDimension(R.dimen.edit_selection_page_preview_cell_size);
        mCellRadius = getResources().getDimension(R.dimen.edit_selection_page_preview_cell_radius);
        mCellPadding = getResources().getDimension(R.dimen.edit_selection_page_preview_cell_padding);
        mWidgetRadius = getResources().getDimension(R.dimen.edit_selection_page_preview_widget_radius);
        setSelectedPage(false);
    }

    public void bind(@Nullable CellLayout cellLayout, int countX, int countY, boolean selectedPage,
            @Nullable Set<ItemInfo> selectedItems) {
        mCellLayout = cellLayout;
        mCountX = Math.max(1, countX);
        mCountY = Math.max(1, countY);
        mSelectedItems = selectedItems != null
                ? Collections.unmodifiableSet(new HashSet<>(selectedItems))
                : Collections.emptySet();
        setSelectedPage(selectedPage);
        invalidate();
    }

    public void setSelectedPage(boolean selected) {
        mSelectedPage = selected;
        setBackgroundResource(selected
                ? R.drawable.edit_selection_page_preview_bg_selected
                : R.drawable.edit_selection_page_preview_bg);
        setAlpha(selected ? 1f : 0.78f);
        invalidate();
    }

    public void setSelectedItems(@Nullable Set<ItemInfo> selectedItems) {
        mSelectedItems = selectedItems != null
                ? Collections.unmodifiableSet(new HashSet<>(selectedItems))
                : Collections.emptySet();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
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
