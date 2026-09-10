package com.android.launcher3.big;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

import androidx.core.content.ContextCompat;

import com.android.launcher3.R;
import com.android.launcher3.folder.FolderNameEditText;

public class ColorEditText extends FolderNameEditText {
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

    }

    public ColorEditText(Context context) {
        super(context);
        initCursor();
    }

    public ColorEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        initCursor();
    }

    public ColorEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initCursor();
    }

    private void initCursor() {
        // ColorOS OplusFolderNameEditText: primary-blue caret while renaming.
        setTextCursorDrawable(ContextCompat.getDrawable(getContext(), R.drawable.folder_name_text_cursor));
    }
}
