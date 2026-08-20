package com.android.launcher3;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.Nullable;

import com.android.launcher3.celllayout.CellLayoutLayoutParams;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.folder.large.HxyLargeFolderIcon;
import com.android.launcher3.folder.large.switchparams.HxyLargeFolderSwitcher;
import com.android.launcher3.model.data.FolderInfo;
import com.android.launcher3.util.Themes;
import com.coui.appcompat.animation.dynamicanimation.COUIDynamicAnimation;
import com.coui.appcompat.animation.dynamicanimation.COUISpringAnimation;
import com.coui.appcompat.animation.dynamicanimation.COUISpringForce;

/**
 * ColorOS folder selection and resize overlay.
 *
 * The decoded OPPO launcher keeps this frame open independently from the shortcut popup. Its
 * lower trailing arc is the only resize affordance; dragging it chooses one of 1x1, 1x2, 2x1 or
 * 2x2 and delegates placement/reordering to CellLayout.
 */
public final class ColorOsFolderResizeFrame extends AbstractFloatingView {

    private final Launcher mLauncher;
    private final DragLayer mDragLayer;
    private final Paint mFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mHandlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mFrameRect = new RectF();
    private final Rect mFolderRect = new Rect();
    private final Rect mLocalFolderBounds = new Rect();
    private final Path mHandlePath = new Path();

    private final int mHorizontalPadding;
    private final int mVerticalPadding;
    private final int mHandleRadius;
    private final int mHandleTouchSize;
    private final int mHandleTouchSize1x1;

    private HxyLargeFolderIcon mFolderIcon;
    private FolderInfo mInfo;
    private CellLayout mCellLayout;

    private boolean mDraggingHandle;
    private boolean mConsumeOutsideTap;
    private float mDownX;
    private float mDownY;
    private int mStartSpanX;
    private int mStartSpanY;
    private int mAppliedSpanX;
    private int mAppliedSpanY;

    private ValueAnimator mBreathAnimator;
    private COUISpringAnimation mScaleXSpring;
    private COUISpringAnimation mScaleYSpring;

    public ColorOsFolderResizeFrame(Context context) {
        this(context, null);
    }

    public ColorOsFolderResizeFrame(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mLauncher = Launcher.getLauncher(context);
        mDragLayer = mLauncher.getDragLayer();
        mHorizontalPadding = getResources().getDimensionPixelSize(
                R.dimen.coloros_folder_resize_frame_horizontal_padding);
        mVerticalPadding = getResources().getDimensionPixelSize(
                R.dimen.coloros_folder_resize_frame_vertical_padding);
        mHandleRadius = getResources().getDimensionPixelSize(
                R.dimen.coloros_folder_resize_handle_radius);
        mHandleTouchSize = getResources().getDimensionPixelSize(
                R.dimen.coloros_folder_resize_touch_size);
        mHandleTouchSize1x1 = getResources().getDimensionPixelSize(
                R.dimen.coloros_folder_resize_touch_size_1x1);

        setWillNotDraw(false);
        setClipChildren(false);
        setClipToPadding(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);

        mFramePaint.setStyle(Paint.Style.STROKE);
        mFramePaint.setStrokeWidth(getResources().getDimension(
                R.dimen.coloros_folder_resize_frame_stroke));

        mHandlePaint.setStyle(Paint.Style.STROKE);
        mHandlePaint.setStrokeCap(Paint.Cap.ROUND);
        mHandlePaint.setStrokeWidth(getResources().getDimension(
                R.dimen.coloros_folder_resize_handle_stroke));
        boolean brightWallpaper = Themes.getAttrBoolean(context, R.attr.isWorkspaceDarkText);
        mFramePaint.setColor(brightWallpaper ? 0x33000000 : 0x99ffffff);
        mHandlePaint.setColor(brightWallpaper ? 0xff808080 : 0xffffffff);
    }

    public static void showForFolder(HxyLargeFolderIcon folderIcon) {
        Launcher launcher = Launcher.getLauncher(folderIcon.getContext());
        AbstractFloatingView existing = AbstractFloatingView.getOpenView(
                launcher, TYPE_WIDGET_RESIZE_FRAME);
        if (existing instanceof ColorOsFolderResizeFrame) {
            existing.close(false);
        }

        CellLayout cellLayout = HxyLargeFolderSwitcher.getCellLayout(folderIcon);
        if (cellLayout == null || launcher.isHotseatLayout(cellLayout)) {
            return;
        }

        ColorOsFolderResizeFrame frame = new ColorOsFolderResizeFrame(launcher);
        frame.setup(folderIcon, cellLayout);
        DragLayer.LayoutParams lp = new DragLayer.LayoutParams(1, 1);
        lp.customPosition = true;
        frame.setLayoutParams(lp);
        launcher.getDragLayer().addView(frame);
        frame.mIsOpen = true;
        frame.syncToFolder();
        frame.setAlpha(0f);
        frame.animate().alpha(1f).setDuration(160L).start();
        frame.startBreathing();
    }

    private void startBreathing() {
        final float minStroke = getResources().getDisplayMetrics().density * 2f;
        final float maxStroke = getResources().getDisplayMetrics().density * 4f;
        mBreathAnimator = ValueAnimator.ofFloat(0f, 1f);
        mBreathAnimator.setDuration(800L);
        mBreathAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mBreathAnimator.setRepeatMode(ValueAnimator.REVERSE);
        mBreathAnimator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue();
            mFramePaint.setStrokeWidth(minStroke + ((maxStroke - minStroke) * fraction));
            mFramePaint.setAlpha(Math.round(128 + (127 * fraction)));
            invalidate();
        });
        mBreathAnimator.start();
    }

    private void setup(HxyLargeFolderIcon folderIcon, CellLayout cellLayout) {
        mFolderIcon = folderIcon;
        mInfo = (FolderInfo) folderIcon.getTag();
        mCellLayout = cellLayout;
        mAppliedSpanX = clampSpan(mInfo.spanX);
        mAppliedSpanY = clampSpan(mInfo.spanY);
        mFolderIcon.setForceHideDot(true);
        mFolderIcon.setTextVisible(false);
    }

    /** Repositions the floating frame from the folder's actual preview-background bounds. */
    public void syncToFolder() {
        if (mFolderIcon == null || !mFolderIcon.isAttachedToWindow()) {
            close(false);
            return;
        }
        mFolderIcon.getColorOsGroupBounds(mLocalFolderBounds);
        mDragLayer.getDescendantRectRelativeToSelf(mFolderIcon, mFolderRect);
        int left = mFolderRect.left + mLocalFolderBounds.left;
        int top = mFolderRect.top + mLocalFolderBounds.top;
        int right = mFolderRect.left + mLocalFolderBounds.right;
        int bottom = mFolderRect.top + mLocalFolderBounds.bottom;

        DragLayer.LayoutParams lp = (DragLayer.LayoutParams) getLayoutParams();
        lp.x = left - mHorizontalPadding;
        lp.y = top - mVerticalPadding;
        lp.width = Math.max(1, right - left + (mHorizontalPadding * 2));
        lp.height = Math.max(1, bottom - top + (mVerticalPadding * 2));
        setLayoutParams(lp);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float halfStroke = mFramePaint.getStrokeWidth() / 2f;
        mFrameRect.set(mHorizontalPadding - halfStroke, mVerticalPadding - halfStroke,
                getWidth() - mHorizontalPadding + halfStroke,
                getHeight() - mVerticalPadding + halfStroke);
        // OPPO IResizeFramePainter adds half the animated stroke to the item's own radius.
        float radius = (mFolderIcon == null
                ? getResources().getDimension(R.dimen.coloros_folder_preview_radius)
                : mFolderIcon.getColorOsGroupRadius()) + halfStroke;
        canvas.drawRoundRect(mFrameRect, radius, radius, mFramePaint);

        mHandlePath.reset();
        RectF handleOval = new RectF(
                mFrameRect.right - (mHandleRadius * 2f),
                mFrameRect.bottom - (mHandleRadius * 2f),
                mFrameRect.right, mFrameRect.bottom);
        handleOval.offset(halfStroke, halfStroke);
        boolean oneByOne = mInfo != null && mInfo.spanX == 1 && mInfo.spanY == 1;
        float arcOffset = oneByOne ? 20f : 0f;
        float arcSweep = oneByOne ? 50f : 90f;
        mHandlePath.addArc(handleOval, 90f - arcOffset, -arcSweep);
        canvas.drawPath(mHandlePath, mHandlePaint);
    }

    private boolean isInHandle(float dragLayerX, float dragLayerY) {
        DragLayer.LayoutParams lp = (DragLayer.LayoutParams) getLayoutParams();
        float cornerX = lp.x + getWidth() - mHorizontalPadding;
        float cornerY = lp.y + getHeight() - mVerticalPadding;
        int touchSize = mInfo.spanX == 1 && mInfo.spanY == 1
                ? mHandleTouchSize1x1 : mHandleTouchSize;
        return dragLayerX >= cornerX - touchSize
                && dragLayerX <= cornerX + (touchSize / 2f)
                && dragLayerY >= cornerY - touchSize
                && dragLayerY <= cornerY + (touchSize / 2f);
    }


    /**
     * Used by the folder shortcut popup to hand the resize gesture to this frame. ColorOS keeps
     * the popup and resize frame open together, so the popup is initially the top floating view.
     */
    public boolean isHandleEvent(MotionEvent event) {
        return event != null && isInHandle(event.getX(), event.getY());
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_DOWN) {
            return false;
        }
        if (isInHandle(event.getX(), event.getY())) {
            mDraggingHandle = true;
            AbstractFloatingView popup = AbstractFloatingView.getOpenView(
                    mLauncher, TYPE_ACTION_POPUP);
            if (popup != null) {
                popup.close(false);
            }
            mDownX = event.getX();
            mDownY = event.getY();
            mStartSpanX = clampSpan(mInfo.spanX);
            mStartSpanY = clampSpan(mInfo.spanY);
            mAppliedSpanX = mStartSpanX;
            mAppliedSpanY = mStartSpanY;
            return true;
        }
        AbstractFloatingView popup = AbstractFloatingView.getOpenView(
                mLauncher, TYPE_ACTION_POPUP);
        if (popup != null && mDragLayer.isEventOverView(popup, event)) {
            return false;
        }
        if (popup != null) {
            popup.close(true);
        } else {
            close(true);
        }
        mConsumeOutsideTap = true;
        return true;
    }

    @Override
    public boolean onControllerTouchEvent(MotionEvent event) {
        if (mConsumeOutsideTap) {
            if (event.getActionMasked() == MotionEvent.ACTION_UP
                    || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                mConsumeOutsideTap = false;
            }
            return true;
        }
        if (!mDraggingHandle) {
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                updateForDrag(event.getX() - mDownX, event.getY() - mDownY, false);
                return true;
            case MotionEvent.ACTION_UP:
                updateForDrag(event.getX() - mDownX, event.getY() - mDownY, true);
                mDraggingHandle = false;
                return true;
            case MotionEvent.ACTION_CANCEL:
                commitAppliedSpan();
                mDraggingHandle = false;
                return true;
            default:
                return true;
        }
    }

    private void updateForDrag(float deltaX, float deltaY, boolean commit) {
        float xThreshold = (mCellLayout.getCellWidth()
                + mLauncher.getDeviceProfile().cellLayoutBorderSpacePx.x) * 0.35f;
        float yThreshold = (mCellLayout.getCellHeight()
                + mLauncher.getDeviceProfile().cellLayoutBorderSpacePx.y) * 0.35f;
        int targetSpanX = clampSpan(mStartSpanX + thresholdDelta(deltaX, xThreshold));
        int targetSpanY = clampSpan(mStartSpanY + thresholdDelta(deltaY, yThreshold));

        if (targetSpanX != mAppliedSpanX || targetSpanY != mAppliedSpanY) {
            applySpan(targetSpanX, targetSpanY, false);
        }
        if (commit) {
            commitAppliedSpan();
        }
    }

    private static int thresholdDelta(float delta, float threshold) {
        if (delta > threshold) {
            return 1;
        }
        if (delta < -threshold) {
            return -1;
        }
        return 0;
    }

    private static int clampSpan(int span) {
        return Math.max(1, Math.min(2, span));
    }

    private boolean applySpan(int spanX, int spanY, boolean commit) {
        if (!(mFolderIcon.getLayoutParams() instanceof CellLayoutLayoutParams)) {
            return false;
        }
        CellLayoutLayoutParams lp =
                (CellLayoutLayoutParams) mFolderIcon.getLayoutParams();
        int[] direction = {
                Integer.compare(spanX, mAppliedSpanX),
                Integer.compare(spanY, mAppliedSpanY)
        };
        if (!mCellLayout.createAreaForResize(mInfo.cellX, mInfo.cellY,
                spanX, spanY, mFolderIcon, direction, commit)) {
            return false;
        }
        boolean spanChanged = spanX != mAppliedSpanX || spanY != mAppliedSpanY;

        lp.setTmpCellX(mInfo.cellX);
        lp.setTmpCellY(mInfo.cellY);
        lp.cellHSpan = spanX;
        lp.cellVSpan = spanY;
        mInfo.spanX = spanX;
        mInfo.spanY = spanY;
        mInfo.minSpanX = spanX;
        mInfo.minSpanY = spanY;
        mAppliedSpanX = spanX;
        mAppliedSpanY = spanY;
        if (spanChanged) {
            mFolderIcon.onFolderSpanChanged();
            startFolderMorph();
        }
        mFolderIcon.post(this::syncToFolder);
        return true;
    }

    private void startFolderMorph() {
        if (mScaleXSpring != null) mScaleXSpring.cancel();
        if (mScaleYSpring != null) mScaleYSpring.cancel();
        mFolderIcon.setScaleX(0.96f);
        mFolderIcon.setScaleY(0.96f);
        COUISpringForce xForce = new COUISpringForce(1f)
                .setResponse(0.15f).setBounce(0.45f);
        COUISpringForce yForce = new COUISpringForce(1f)
                .setResponse(0.15f).setBounce(0.45f);
        mScaleXSpring = new COUISpringAnimation(
                mFolderIcon, COUIDynamicAnimation.SCALE_X, 1f)
                .setSpring(xForce).setStartValue(0.96f);
        mScaleYSpring = new COUISpringAnimation(
                mFolderIcon, COUIDynamicAnimation.SCALE_Y, 1f)
                .setSpring(yForce).setStartValue(0.96f);
        mScaleXSpring.start();
        mScaleYSpring.start();
    }

    private void commitAppliedSpan() {
        applySpan(mAppliedSpanX, mAppliedSpanY, true);
        CellLayoutLayoutParams lp =
                (CellLayoutLayoutParams) mFolderIcon.getLayoutParams();
        lp.setCellX(lp.getTmpCellX());
        lp.setCellY(lp.getTmpCellY());
        mInfo.cellX = lp.getCellX();
        mInfo.cellY = lp.getCellY();
        mLauncher.getModelWriter().modifyItemInDatabase(mInfo, mInfo.container,
                mInfo.screenId, mInfo.cellX, mInfo.cellY, mInfo.spanX, mInfo.spanY);
    }

    @Override
    protected void handleClose(boolean animate) {
        if (mBreathAnimator != null) {
            mBreathAnimator.cancel();
        }
        if (mFolderIcon != null) {
            mFolderIcon.setForceHideDot(false);
            mFolderIcon.setTextVisible(true);
        }
        if (getParent() == mDragLayer) {
            mDragLayer.removeView(this);
        }
    }

    @Override
    protected boolean isOfType(int type) {
        return (type & TYPE_WIDGET_RESIZE_FRAME) != 0;
    }
}

