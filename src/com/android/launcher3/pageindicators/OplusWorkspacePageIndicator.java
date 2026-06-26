package com.android.launcher3.pageindicators;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
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

/**
 * Small launcher page indicator matching Oppo's workspace dot/pill layout contract.
 */
public class OplusWorkspacePageIndicator extends View implements Insettable, PageIndicator {

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mActiveRect = new RectF();
    private final boolean mIsRtl;
    private final float mDotRadius;
    private final float mActiveWidth;
    private final float mPitch;
    private final int mSelectedColor;
    private final int mUnselectedColor;
    private final int mTouchSlop;

    private int mNumPages;
    private float mCurrentPosition;
    private boolean mShouldAutoHide;
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
        float dotSize = res.getDimension(R.dimen.page_indicator_dot_size);
        mDotRadius = dotSize / 2f;
        mActiveWidth = dotSize * 2f;
        mPitch = dotSize + res.getDimensionPixelSize(
                R.dimen.workspace_page_indicator_horizontal_padding) / 2f;
        mSelectedColor = res.getColor(R.color.launcher_page_indicator_select_color, null);
        mUnselectedColor = res.getColor(R.color.launcher_page_indicator_unselect_color, null);
        mIsRtl = Utilities.isRtl(res);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        int horizontalPadding = res.getDimensionPixelSize(
                R.dimen.workspace_page_indicator_horizontal_padding);
        int verticalPadding = res.getDimensionPixelSize(
                R.dimen.workspace_page_indicator_vertical_padding);
        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
        setClickable(true);
        setWillNotDraw(false);
    }

    @Override
    public void setScroll(int currentScroll, int totalScroll) {
        if (mNumPages <= 1) {
            return;
        }
        if (totalScroll <= 0) {
            mCurrentPosition = 0;
        } else {
            float progress = Utilities.boundToRange(currentScroll / (float) totalScroll, 0f, 1f);
            mCurrentPosition = progress * (mNumPages - 1);
        }
        if (mIsRtl) {
            mCurrentPosition = (mNumPages - 1) - mCurrentPosition;
        }
        if (mShouldAutoHide) {
            setAlpha(1f);
        }
        invalidate();
    }

    @Override
    public void setActiveMarker(int activePage) {
        mCurrentPosition = Utilities.boundToRange(activePage, 0, Math.max(0, mNumPages - 1));
        invalidate();
    }

    @Override
    public void setMarkersCount(int numMarkers) {
        mNumPages = Math.max(0, numMarkers);
        mCurrentPosition = Utilities.boundToRange(mCurrentPosition, 0, Math.max(0, mNumPages - 1));
        setVisibility(mNumPages > 1 ? VISIBLE : INVISIBLE);
        requestLayout();
        invalidate();
    }

    @Override
    public void setShouldAutoHide(boolean shouldAutoHide) {
        mShouldAutoHide = shouldAutoHide;
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
        float contentWidth = mActiveWidth + ((mNumPages - 1) * mPitch);
        float startX = (getWidth() - contentWidth) / 2f;
        float relative = Utilities.boundToRange(x - startX, 0f, contentWidth);
        int page = Math.round((relative - (mActiveWidth / 2f)) / mPitch);
        page = Utilities.boundToRange(page, 0, mNumPages - 1);
        return mIsRtl ? (mNumPages - 1) - page : page;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = getPaddingLeft() + getPaddingRight();
        if (mNumPages > 0) {
            desiredWidth += Math.round(mActiveWidth + ((mNumPages - 1) * mPitch));
        }
        int desiredHeight = getPaddingTop() + getPaddingBottom() + Math.round(mDotRadius * 2f);

        int width = resolveSize(desiredWidth, widthMeasureSpec);
        int height = resolveSize(desiredHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mNumPages <= 1) {
            return;
        }

        float contentWidth = mActiveWidth + ((mNumPages - 1) * mPitch);
        float startX = (getWidth() - contentWidth) / 2f;
        float centerY = getHeight() / 2f;

        mPaint.setColor(mUnselectedColor);
        for (int i = 0; i < mNumPages; i++) {
            canvas.drawCircle(startX + (i * mPitch) + (mActiveWidth / 2f), centerY, mDotRadius,
                    mPaint);
        }

        float activeCenter = startX + (mCurrentPosition * mPitch) + (mActiveWidth / 2f);
        mActiveRect.set(activeCenter - (mActiveWidth / 2f), centerY - mDotRadius,
                activeCenter + (mActiveWidth / 2f), centerY + mDotRadius);
        mPaint.setColor(mSelectedColor);
        canvas.drawRoundRect(mActiveRect, mDotRadius, mDotRadius, mPaint);
    }
}
