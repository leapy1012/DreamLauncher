package com.android.launcher3.pageindicators;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.Nullable;

import com.android.launcher3.Insettable;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.Workspace;
import com.android.launcher3.util.Themes;

/**
 * Workspace page indicator matching Oppo's dot/pill layout and ColorOS scroll morphing.
 */
public class OplusWorkspacePageIndicator extends View implements Insettable, PageIndicator {

    private static final float SHIFT_PER_ANIMATION = 0.5f;
    private static final int DOT_GAP_FACTOR = 4;
    private static final float DOT_ALPHA_FRACTION = 0.5f;

    private static final RectF sTempRect = new RectF();

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final boolean mIsRtl;
    private final float mDotRadius;
    private final float mCircleGap;
    private final int mTouchSlop;

    private int mSelectedColor;
    private int mUnselectedColor;

    private int mNumPages;
    private int mActivePage;
    private float mCurrentPosition;
    private float mDownX;
    private float mDownY;
    private boolean mIsDragging;

    public OplusWorkspacePageIndicator(Context context) {
        this(context, null);
    }

    public OplusWorkspacePageIndicator(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OplusWorkspacePageIndicator(Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        Resources res = context.getResources();
        mDotRadius = res.getDimension(R.dimen.page_indicator_dot_size_v2) / 2f;
        mCircleGap = DOT_GAP_FACTOR * mDotRadius;
        mIsRtl = Utilities.isRtl(res);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        updateColors(context);

        int horizontalPadding = res.getDimensionPixelSize(
                R.dimen.workspace_page_indicator_horizontal_padding);
        int verticalPadding = res.getDimensionPixelSize(
                R.dimen.workspace_page_indicator_vertical_padding);
        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
        setClickable(true);
        setWillNotDraw(false);
    }

    private void updateColors(Context context) {
        boolean darkText = Themes.getAttrBoolean(context, R.attr.isWorkspaceDarkText);
        Resources res = context.getResources();
        if (darkText) {
            mSelectedColor = res.getColor(R.color.launcher_page_indicator_bright_active_color,
                    null);
            mUnselectedColor = res.getColor(R.color.launcher_page_indicator_bright_color, null);
        } else {
            mSelectedColor = res.getColor(R.color.launcher_page_indicator_select_color, null);
            mUnselectedColor = res.getColor(R.color.launcher_page_indicator_unselect_color, null);
        }
    }

    @Override
    public void setScroll(int currentScroll, int totalScroll) {
        if (mNumPages <= 1) {
            return;
        }

        if (mIsRtl) {
            currentScroll = totalScroll - currentScroll;
        }

        if (totalScroll <= 0) {
            mCurrentPosition = mActivePage;
        } else {
            int scrollPerPage = totalScroll / (mNumPages - 1);
            if (scrollPerPage <= 0) {
                mCurrentPosition = mActivePage;
            } else {
                mCurrentPosition = Utilities.boundToRange(
                        currentScroll / (float) scrollPerPage, 0f, mNumPages - 1);
            }
        }
        invalidate();
    }

    @Override
    public void setActiveMarker(int activePage) {
        mActivePage = Utilities.boundToRange(activePage, 0, Math.max(0, mNumPages - 1));
        mCurrentPosition = mActivePage;
        invalidate();
    }

    @Override
    public void setMarkersCount(int numMarkers) {
        mNumPages = Math.max(0, numMarkers);
        mActivePage = Utilities.boundToRange(mActivePage, 0, Math.max(0, mNumPages - 1));
        mCurrentPosition = Utilities.boundToRange(mCurrentPosition, 0, Math.max(0, mNumPages - 1));
        setVisibility(mNumPages > 1 ? VISIBLE : INVISIBLE);
        requestLayout();
        invalidate();
    }

    @Override
    public void setShouldAutoHide(boolean shouldAutoHide) {
        // Oppo parity: page dots stay visible on multi-page workspace in NORMAL state.
    }

    @Override
    public void setPaintColor(int color) {
        mSelectedColor = color;
        mUnselectedColor = Color.argb((int) (255 * DOT_ALPHA_FRACTION), Color.red(color),
                Color.green(color), Color.blue(color));
        invalidate();
    }

    @Override
    public void setInsets(Rect insets) { }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mNumPages <= 1) {
            return false;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = event.getX();
                mDownY = event.getY();
                mIsDragging = false;
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!mIsDragging
                        && Math.hypot(event.getX() - mDownX, event.getY() - mDownY)
                                > mTouchSlop) {
                    mIsDragging = true;
                }
                if (mIsDragging) {
                    snapWorkspaceToTouch(event.getX());
                }
                return true;
            case MotionEvent.ACTION_UP:
                snapWorkspaceToTouch(event.getX());
                performClick();
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void snapWorkspaceToTouch(float x) {
        Launcher launcher = Launcher.getLauncher(getContext());
        Workspace<?> workspace = launcher.getWorkspace();
        if (workspace == null || workspace.getPageCount() <= 1) {
            return;
        }

        int page = getPageForTouchX(x);
        if (page >= 0 && page < workspace.getPageCount() && page != workspace.getNextPage()) {
            workspace.snapToPage(page);
            setActiveMarker(page);
        }
    }

    private int getPageForTouchX(float x) {
        float startX = getDotStartX();
        float relative = Utilities.boundToRange(x - startX, 0f, mNumPages * mCircleGap);
        int page = Math.round((relative - mDotRadius) / mCircleGap);
        page = Utilities.boundToRange(page, 0, mNumPages - 1);
        return mIsRtl ? (mNumPages - 1) - page : page;
    }

    private float getDotStartX() {
        return (getWidth() - (mNumPages * mCircleGap) + mDotRadius) / 2f;
    }

    private RectF getActiveRect() {
        float startCircle = (int) mCurrentPosition;
        float delta = mCurrentPosition - startCircle;
        float diameter = 2 * mDotRadius;
        float startX = getDotStartX();

        sTempRect.top = (getHeight() * 0.5f) - mDotRadius;
        sTempRect.bottom = (getHeight() * 0.5f) + mDotRadius;
        sTempRect.left = startX + (startCircle * mCircleGap);
        sTempRect.right = sTempRect.left + diameter;

        if (delta < SHIFT_PER_ANIMATION) {
            // Stretch toward the next page.
            sTempRect.right += delta * mCircleGap * 2;
        } else {
            // Collapse from the previous page.
            sTempRect.right += mCircleGap;
            delta -= SHIFT_PER_ANIMATION;
            sTempRect.left += delta * mCircleGap * 2;
        }

        if (mIsRtl) {
            float rectWidth = sTempRect.width();
            sTempRect.right = getWidth() - sTempRect.left;
            sTempRect.left = sTempRect.right - rectWidth;
        }
        return sTempRect;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int contentWidth = mNumPages > 0 ? (int) ((mNumPages * 3 + 2) * mDotRadius) : 0;
        int contentHeight = (int) (4 * mDotRadius);
        int width = resolveSize(contentWidth + getPaddingLeft() + getPaddingRight(),
                widthMeasureSpec);
        int height = resolveSize(contentHeight + getPaddingTop() + getPaddingBottom(),
                heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mNumPages <= 1) {
            return;
        }

        float x = getDotStartX() + mDotRadius;
        float y = getHeight() / 2f;

        mPaint.setColor(mUnselectedColor);
        for (int i = 0; i < mNumPages; i++) {
            float drawX = mIsRtl ? getWidth() - x : x;
            canvas.drawCircle(drawX, y, mDotRadius, mPaint);
            x += mCircleGap;
        }

        mPaint.setColor(mSelectedColor);
        canvas.drawRoundRect(getActiveRect(), mDotRadius, mDotRadius, mPaint);
    }
}
