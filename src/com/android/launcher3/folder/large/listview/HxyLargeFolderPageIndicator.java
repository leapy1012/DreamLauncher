package com.android.launcher3.folder.large.listview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/** The 5dp ColorOS flexible-folder page dots shown in place of the folder name while paging. */
public class HxyLargeFolderPageIndicator extends View {
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int mDotSize;
    private final int mDotSpacing;
    private int mPageCount;
    private int mCurrentPage;

    public HxyLargeFolderPageIndicator(Context context) {
        this(context, null);
    }

    public HxyLargeFolderPageIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;
        mDotSize = Math.round(5.0f * density);
        mDotSpacing = Math.round(3.0f * density);
    }

    public void setPageCount(int pageCount) {
        pageCount = Math.max(0, pageCount);
        if (mPageCount != pageCount) {
            mPageCount = pageCount;
            mCurrentPage = Math.min(mCurrentPage, Math.max(0, pageCount - 1));
            requestLayout();
            invalidate();
        }
    }

    public void setCurrentPage(int currentPage) {
        int bounded = Math.max(0, Math.min(currentPage, Math.max(0, mPageCount - 1)));
        if (mCurrentPage != bounded) {
            mCurrentPage = bounded;
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getPaddingLeft() + getPaddingRight();
        if (mPageCount > 0) {
            width += (mPageCount * mDotSize) + ((mPageCount - 1) * mDotSpacing);
        }
        setMeasuredDimension(resolveSize(width, widthMeasureSpec),
                resolveSize(mDotSize + getPaddingTop() + getPaddingBottom(), heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mPageCount <= 1) return;
        float totalWidth = (mPageCount * mDotSize) + ((mPageCount - 1) * mDotSpacing);
        float x = (getWidth() - totalWidth) / 2.0f + (mDotSize / 2.0f);
        float y = getHeight() / 2.0f;
        for (int page = 0; page < mPageCount; page++) {
            // Exact flexible-folder colors from ColorOS resources.
            mPaint.setColor(page == mCurrentPage ? 0xD9FFFFFF : 0x4CFFFFFF);
            canvas.drawCircle(x, y, mDotSize / 2.0f, mPaint);
            x += mDotSize + mDotSpacing;
        }
    }
}
