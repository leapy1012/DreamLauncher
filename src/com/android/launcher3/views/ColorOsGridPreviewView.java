package com.android.launcher3.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/** Compact ColorOS-style workspace grid preview used by the edit-mode Layout panel. */
public class ColorOsGridPreviewView extends View {

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mBounds = new RectF();
    private int mColumns = 4;
    private int mRows = 6;
    private boolean mChecked;

    public ColorOsGridPreviewView(Context context) {
        this(context, null);
    }

    public ColorOsGridPreviewView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setGridSize(int columns, int rows) {
        mColumns = columns;
        mRows = rows;
        invalidate();
    }

    public void setChecked(boolean checked) {
        mChecked = checked;
        setSelected(checked);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float stroke = mChecked ? density : 0f;
        mBounds.set(stroke, stroke, getWidth() - stroke, getHeight() - stroke);

        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(0x14000000);
        canvas.drawRoundRect(mBounds, 4f * density, 4f * density, mPaint);

        if (mChecked) {
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(2f * density);
            mPaint.setColor(0xFF0066FF);
            canvas.drawRoundRect(mBounds, 4f * density, 4f * density, mPaint);
        }

        float side = 6f * density;
        float top = 6f * density;
        float bottom = 6f * density;
        float usableWidth = getWidth() - side * 2;
        float usableHeight = getHeight() - top - bottom;
        float cellWidth = usableWidth / mColumns;
        float cellHeight = usableHeight / mRows;
        float dotRadius = Math.min(1.64f * density, Math.min(cellWidth, cellHeight) * 0.32f);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(0x99000000);
        for (int row = 0; row < mRows; row++) {
            for (int column = 0; column < mColumns; column++) {
                float cx = side + cellWidth * (column + 0.5f);
                float cy = top + cellHeight * (row + 0.5f);
                canvas.drawCircle(cx, cy, dotRadius, mPaint);
            }
        }

    }
}
