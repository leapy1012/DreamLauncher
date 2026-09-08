package com.android.launcher3.togglebar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.android.launcher3.R;

/**
 * Mini grid-dot tile from Oppo {@code ToggleBarLayoutItemPreview}.
 * Cells (0,0), (0,1) and (1,0) are omitted and merged into a 2×2 widget block.
 */
public class ColorOsLayoutGridPreview extends View {

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mCell = new RectF();
    private int mColumns = 4;
    private int mRows = 6;

    public ColorOsLayoutGridPreview(Context context) {
        this(context, null);
    }

    public ColorOsLayoutGridPreview(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorOsLayoutGridPreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(context.getColor(R.color.coloros_layout_tile_dot));
    }

    public void setColumnRow(int columns, int rows) {
        mColumns = Math.max(2, columns);
        mRows = Math.max(2, rows);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float pad = getResources().getDimension(R.dimen.coloros_layout_preview_pad);
        float cell = getResources().getDimension(R.dimen.coloros_layout_preview_cell);
        float radius = getResources().getDimension(R.dimen.coloros_layout_preview_radius);
        float widgetRadius = getResources().getDimension(R.dimen.coloros_layout_preview_widget_radius);
        float innerW = getWidth() - pad * 2f;
        float innerH = getHeight() - pad * 2f;
        float gapX = mColumns > 1 ? (innerW - cell * mColumns) / (mColumns - 1) : 0f;
        float gapY = mRows > 1 ? (innerH - cell * mRows) / (mRows - 1) : 0f;
        canvas.save();
        canvas.translate(pad, pad);
        for (int row = 0; row < mRows; row++) {
            for (int col = 0; col < mColumns; col++) {
                boolean skipDot = (row == 0 && col == 0)
                        || (row == 0 && col == 1)
                        || (row == 1 && col == 0);
                if (skipDot) {
                    continue;
                }
                float left = col * (cell + gapX);
                float top = row * (cell + gapY);
                if (row == 1 && col == 1) {
                    mCell.set(0, 0, cell + gapX + cell, cell + gapY + cell);
                    canvas.drawRoundRect(mCell, widgetRadius, widgetRadius, mPaint);
                } else {
                    mCell.set(left, top, left + cell, top + cell);
                    canvas.drawRoundRect(mCell, radius, radius, mPaint);
                }
            }
        }
        canvas.restore();
    }
}
