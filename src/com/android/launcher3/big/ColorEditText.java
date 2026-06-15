package com.android.launcher3.big;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

import com.android.launcher3.folder.FolderNameEditText;

public class ColorEditText extends FolderNameEditText {
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

    }

    public ColorEditText(Context context) {
        super(context);
    }

    public ColorEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ColorEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

}
