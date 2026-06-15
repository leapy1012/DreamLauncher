package com.android.launcher3.screenedit;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class GalleryCursorView extends View {
    // 页面数量
    private int pageCount;
    // 起始位置
    private float cursorStartX;
    // 画笔对象
    private Paint cursorPaint;

    public GalleryCursorView(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
        // 初始化画笔
        cursorPaint = new Paint();
        cursorPaint.setAntiAlias(true);
        cursorPaint.setDither(true);
        cursorPaint.setColor(context.getResources().getColor(android.R.color.white));
    }

    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 当页面数量大于 1 时，绘制光标
        if (pageCount > 1) {
            float cursorWidth = (float) getMeasuredWidth() / pageCount;
            canvas.drawRect(cursorStartX, 0.0f, cursorStartX + cursorWidth, (float) getMeasuredHeight(), cursorPaint);
        }
    }

    /**
     * 设置页面数量，并根据页面数量显示或隐藏视图。
     * @param count 页面数量
     */
    public void setCount(int count) {
        this.pageCount = count;
        this.cursorStartX = 0.0f;
        if (count <= 1) {
            // 页面数量小于等于 1 时，隐藏视图
            setVisibility(View.INVISIBLE);
        } else {
            // 页面数量大于 1 时，显示视图并刷新界面
            setVisibility(View.VISIBLE);
            invalidate();
        }
    }

    public void setXY(int pageIndex, float offset) {
        int pageWidth = getMeasuredWidth() / pageCount;
        this.cursorStartX = (float) (pageIndex * pageWidth) + ((float) pageWidth * offset);
        // 刷新界面以更新光标位置
        invalidate();
    }

    public GalleryCursorView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public GalleryCursorView(Context context) {
        this(context, (AttributeSet) null);
    }
}
