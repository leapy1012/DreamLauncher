package com.android.launcher3.views;

import static android.view.HapticFeedbackConstants.CLOCK_TICK;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.android.launcher3.R;

/** Compact ColorOS-style alphabet rail which retains Launcher3 fast-scroll behavior. */
public class ColorOsAlphabetScroller extends RecyclerViewFastScroller {

    private static final char[] LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ#".toCharArray();
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int mActiveIndex = -1;

    public ColorOsAlphabetScroller(Context context) {
        this(context, null);
    }

    public ColorOsAlphabetScroller(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorOsAlphabetScroller(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTypeface(android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL));
        mTextPaint.setTextSize(getResources().getDimension(R.dimen.coloros_all_apps_index_text_size));
        setContentDescription(getResources().getString(R.string.coloros_all_apps_alphabet_index));
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mRv == null) return;
        float spacing = getResources().getDimension(R.dimen.coloros_all_apps_index_spacing);
        float totalHeight = spacing * LETTERS.length;
        float top = Math.max(mRv.getScrollBarTop(), (getHeight() - totalHeight) / 2f);
        Paint.FontMetrics fm = mTextPaint.getFontMetrics();
        float baselineOffset = (spacing - (fm.descent - fm.ascent)) / 2f - fm.ascent;
        for (int i = 0; i < LETTERS.length; i++) {
            mTextPaint.setColor(getResources().getColor(
                    i == mActiveIndex ? R.color.coloros_all_apps_index_active
                            : R.color.coloros_all_apps_index, getContext().getTheme()));
            canvas.drawText(String.valueOf(LETTERS[i]), getWidth() / 2f,
                    top + i * spacing + baselineOffset, mTextPaint);
        }
    }

    @Override
    public boolean handleTouchEvent(MotionEvent event, Point offset) {
        if (mRv == null || !mRv.supportsFastScrolling()) return false;
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            float spacing = getResources().getDimension(R.dimen.coloros_all_apps_index_spacing);
            float totalHeight = spacing * LETTERS.length;
            float top = Math.max(mRv.getScrollBarTop(), (getHeight() - totalHeight) / 2f);
            float localY = event.getY() - offset.y;
            int index = Math.max(0, Math.min(LETTERS.length - 1,
                    (int) ((localY - top) / spacing)));
            if (index != mActiveIndex) {
                mActiveIndex = index;
                mRv.scrollToPositionAtProgress(index / (float) (LETTERS.length - 1));
                performHapticFeedback(CLOCK_TICK);
                invalidate();
            }
            return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            mRv.onFastScrollCompleted();
            mActiveIndex = -1;
            invalidate();
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldBlockIntercept(int x, int y) {
        return x >= 0 && x < getWidth() && y >= 0 && y < getHeight();
    }
}
