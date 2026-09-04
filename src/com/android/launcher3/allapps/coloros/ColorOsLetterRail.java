package com.android.launcher3.allapps.coloros;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.android.launcher3.R;

/**
 * ColorOS drawer A–Z index: muted letters, pressed blue letter + blue dot.
 * Empty sections are not selectable while scrubbing (Oppo
 * {@code IndexIndicationKey.hasValue} / {@code COUITouchSearchView.getKeyIndices}).
 */
public class ColorOsLetterRail extends View {

    public interface Listener {
        /** Called on ACTION_DOWN before the first letter — stop list / drawer intercept. */
        default void onLetterScrubStart() {}

        /**
         * @param letter selected section key (only fired for sections that have apps)
         * @param centerYInRail local Y of the letter center within this view
         */
        void onLetter(String letter, int centerYInRail);

        /** Finger lifted; cluster stays until empty tap (Oppo). */
        default void onLetterScrubEnd() {}
    }

    static final String[] LETTERS = {
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
            "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "#"
    };

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int mColorNormal;
    private final int mColorActive;
    private final float mDotRadiusPx;
    /** Oppo {@code IndexIndicationKey.hasValue} per LETTERS index. */
    private final boolean[] mHasValue = new boolean[LETTERS.length];

    private Listener mListener;
    private int mActive = -1;
    /** True while finger is on the rail or cluster filter is up (blue + dot). */
    private boolean mScrubStyle;
    private boolean mTouching;
    private final int mColorFollow;

    public ColorOsLetterRail(Context context) {
        this(context, null);
    }

    public ColorOsLetterRail(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorOsLetterRail(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        mPaint.setFakeBoldText(false);
        mColorNormal = context.getColor(R.color.coloros_all_apps_index);
        mColorFollow = context.getColor(R.color.coloros_all_apps_index_follow);
        mColorActive = resolveActiveColor(context);
        mDotPaint.setColor(mColorActive);
        mDotPaint.setStyle(Paint.Style.FILL);
        mDotRadiusPx = dp(2.5f);
        // Until chrome publishes sections, treat all as available (fail open).
        for (int i = 0; i < mHasValue.length; i++) {
            mHasValue[i] = true;
        }
        setClickable(true);
    }

    private static int resolveActiveColor(Context context) {
        Context themed = new android.view.ContextThemeWrapper(
                context, com.coui.appcompat.R.style.Theme_COUI_Blue);
        TypedValue value = new TypedValue();
        if (themed.getTheme().resolveAttribute(
                com.coui.appcompat.R.attr.couiColorPrimary, value, true)) {
            if (value.resourceId != 0) {
                return ContextCompat.getColor(themed, value.resourceId);
            }
            if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT
                    && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                return value.data;
            }
        }
        return context.getColor(R.color.coloros_all_apps_index_active);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    /**
     * Oppo drawer: mark which A–Z/# keys have apps ({@code isSectionHasValue}).
     * Keys with {@code false} are drawn but skipped while scrubbing.
     */
    public void setSectionHasValue(boolean[] hasValue) {
        if (hasValue == null || hasValue.length != mHasValue.length) {
            return;
        }
        System.arraycopy(hasValue, 0, mHasValue, 0, mHasValue.length);
        if (mActive >= 0 && !mHasValue[mActive]) {
            mActive = -1;
            invalidate();
        }
    }

    public boolean hasSectionValue(@Nullable String letter) {
        int index = indexOfLetter(letter);
        return index >= 0 && mHasValue[index];
    }

    /**
     * Oppo idle / scroll follow ({@code updateMoveTouchBarText}): brighter letter,
     * no blue scrub styling.
     */
    public void setFollowLetter(@Nullable String letter) {
        if (mTouching || mScrubStyle) {
            return;
        }
        int index = indexOfLetter(letter);
        if (index >= 0 && !mHasValue[index]) {
            index = -1;
        }
        if (index != mActive || mScrubStyle) {
            mActive = index;
            mScrubStyle = false;
            invalidate();
        }
    }

    /** @deprecated use {@link #setFollowLetter(String)} */
    public void setActiveLetter(@Nullable String letter) {
        setFollowLetter(letter);
    }

    /** Blue letter + blue dot while scrubbing or showing the cluster filter. */
    public void setScrubLetter(@Nullable String letter) {
        int index = indexOfLetter(letter);
        if (index >= 0 && !mHasValue[index]) {
            index = -1;
        }
        if (index != mActive || !mScrubStyle) {
            mActive = index;
            mScrubStyle = index >= 0;
            invalidate();
        }
    }

    /** Leave scrub styling; keep or clear the follow highlight separately. */
    public void endScrubStyle() {
        if (mScrubStyle) {
            mScrubStyle = false;
            invalidate();
        }
    }

    public void clearActiveLetter() {
        if (!mTouching && (mActive != -1 || mScrubStyle)) {
            mActive = -1;
            mScrubStyle = false;
            invalidate();
        }
    }

    /** Center Y of the active letter in this view's coordinates, or -1. */
    public int getActiveLetterCenterY() {
        if (mActive < 0) {
            return -1;
        }
        int h = getHeight() - getPaddingTop() - getPaddingBottom();
        if (h <= 0) {
            return -1;
        }
        float slot = h / (float) LETTERS.length;
        return getPaddingTop() + Math.round(slot * mActive + slot / 2f);
    }

    /** Maps a fast-scroll / app section name onto a rail index, or -1. */
    static int indexForSection(@Nullable String letter) {
        return indexOfLetter(letter);
    }

    private static int indexOfLetter(@Nullable String letter) {
        if (letter == null || letter.isEmpty()) {
            return -1;
        }
        String key = letter.substring(0, 1).toUpperCase();
        if (!Character.isLetter(key.charAt(0))) {
            key = "#";
        }
        for (int i = 0; i < LETTERS.length; i++) {
            if (LETTERS[i].equals(key)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Oppo {@code getKeyIndices}: map touch Y to the last {@code hasValue} key at or
     * before the finger (empty letters are not selected).
     */
    private int resolveHasValueIndex(int rawIndex) {
        rawIndex = Math.max(0, Math.min(LETTERS.length - 1, rawIndex));
        int lastHasValue = -1;
        for (int i = 0; i <= rawIndex; i++) {
            if (mHasValue[i]) {
                lastHasValue = i;
            }
        }
        if (lastHasValue >= 0) {
            return lastHasValue;
        }
        // Finger above the first populated section — snap forward to first hasValue.
        for (int i = rawIndex + 1; i < LETTERS.length; i++) {
            if (mHasValue[i]) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int h = getHeight() - getPaddingTop() - getPaddingBottom();
        int w = getWidth();
        if (h <= 0 || w <= 0) {
            return;
        }
        float slot = h / (float) LETTERS.length;
        float textSize = getResources().getDimension(R.dimen.coloros_all_apps_index_text_size);
        if (slot > 0f && textSize > slot * 0.95f) {
            textSize = slot * 0.95f;
        }
        mPaint.setTextSize(textSize);
        Paint.FontMetrics fm = mPaint.getFontMetrics();
        float baselineOffset = (fm.descent + fm.ascent) / 2f;
        float letterCx = getPaddingStart() + (w - getPaddingStart() - getPaddingEnd()) * 0.62f;
        float dotCx = getPaddingStart() + mDotRadiusPx + dp(1f);
        for (int i = 0; i < LETTERS.length; i++) {
            float cy = getPaddingTop() + slot * i + slot / 2f;
            boolean active = i == mActive;
            if (active && mScrubStyle) {
                mPaint.setColor(mColorActive);
            } else if (active) {
                mPaint.setColor(mColorFollow);
            } else {
                mPaint.setColor(mColorNormal);
            }
            canvas.drawText(LETTERS[i], letterCx, cy - baselineOffset, mPaint);
            // Oppo: blue scrub dot only while touching / in cluster filter.
            if (active && mScrubStyle) {
                canvas.drawCircle(dotCx, cy, mDotRadiusPx, mDotPaint);
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            ViewParent parent = getParent();
            if (parent != null) {
                parent.requestDisallowInterceptTouchEvent(true);
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            ViewParent parent = getParent();
            if (parent != null) {
                parent.requestDisallowInterceptTouchEvent(true);
            }
            mTouching = true;
            if (mListener != null) {
                mListener.onLetterScrubStart();
            }
        }
        if (action == MotionEvent.ACTION_DOWN
                || action == MotionEvent.ACTION_MOVE
                || action == MotionEvent.ACTION_UP) {
            int h = getHeight() - getPaddingTop() - getPaddingBottom();
            if (h > 0) {
                float y = event.getY() - getPaddingTop();
                int rawIndex = (int) (y / (h / (float) LETTERS.length));
                int index = resolveHasValueIndex(rawIndex);
                mTouching = true;
                if (index >= 0) {
                    boolean changed = index != mActive || !mScrubStyle
                            || action == MotionEvent.ACTION_DOWN;
                    mActive = index;
                    mScrubStyle = true;
                    float slot = h / (float) LETTERS.length;
                    int centerY = getPaddingTop() + Math.round(slot * index + slot / 2f);
                    if (changed) {
                        invalidate();
                        if (mListener != null) {
                            mListener.onLetter(LETTERS[index], centerY);
                        }
                    } else if (mListener != null && action == MotionEvent.ACTION_MOVE) {
                        mListener.onLetter(LETTERS[index], centerY);
                    }
                }
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                mTouching = false;
                if (mListener != null) {
                    mListener.onLetterScrubEnd();
                }
                invalidate();
            }
            return true;
        }
        if (action == MotionEvent.ACTION_CANCEL) {
            mTouching = false;
            if (mListener != null) {
                mListener.onLetterScrubEnd();
            }
            invalidate();
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDetachedFromWindow() {
        mTouching = false;
        super.onDetachedFromWindow();
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
