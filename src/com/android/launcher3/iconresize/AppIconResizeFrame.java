package com.android.launcher3.iconresize;

import static com.android.launcher3.AbstractFloatingView.TYPE_WIDGET_RESIZE_FRAME;
import static com.android.launcher3.Utilities.squaredHypot;
import static com.android.launcher3.Utilities.squaredTouchSlop;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.DropTarget;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.dragndrop.DragController;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.dragndrop.DragOptions;
import com.android.launcher3.dragndrop.DragView;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.popup.PopupContainerWithArrow;
import com.android.launcher3.touch.ItemClickHandler;
import com.android.launcher3.views.ActivityContext;

/**
 * Oppo-style resize overlay for workspace app icons. Shown on long-press (with the shortcuts
 * popup). Drag the bottom-right arc handle to change between 1×1, 1×2, 2×1, and 2×2.
 */
public class AppIconResizeFrame extends AbstractFloatingView
        implements DragController.DragListener {

    private static final String TAG = "AppIconResizeFrame";
    private static final Rect sTmpRect = new Rect();
    private static final Rect sIconLocalRect = new Rect();

    /** While true, workspace pre-drag must not promote to a full icon drag. */
    private static boolean sHandleDragging;

    private Launcher mLauncher;
    private BubbleTextView mIconView;
    private CellLayout mCellLayout;
    private DragLayer mDragLayer;

    private int mStartSpanX;
    private int mStartSpanY;
    private boolean mDraggingHandle;

    private float mXDown;
    private float mYDown;
    private long mDownTime;
    private boolean mTouchOnIconBody;

    @Nullable
    private DragView<?> mDragView;

    @Nullable
    private IconResizeBlurHelper.IconResizeBlurView mBlurView;
    @Nullable
    private IconResizeDragHelper mDragHelper;

    public AppIconResizeFrame(Context context) {
        this(context, null);
    }

    public AppIconResizeFrame(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppIconResizeFrame(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public static boolean isHandleDragging() {
        return sHandleDragging;
    }

    /**
     * Oppo {@code ItemResizeFrame.tryFastHandleMotionEvent}: on ACTION_DOWN while the resize frame
     * is open, dismiss the shortcuts popup when the touch is on the handle or icon bounds.
     */
    public static boolean tryFastClosePopupIfNeeded(ActivityContext activity, MotionEvent ev) {
        if (ev == null || ev.getActionMasked() != MotionEvent.ACTION_DOWN) {
            return false;
        }
        AbstractFloatingView frameView = AbstractFloatingView.getTopOpenViewWithType(
                activity, TYPE_WIDGET_RESIZE_FRAME);
        if (!(frameView instanceof AppIconResizeFrame frame)) {
            return false;
        }
        return frame.fastClosePopupIfNeeded(ev);
    }

    /**
     * Shows the resize frame around {@code icon} during long-press, matching Oppo behavior.
     * The shortcuts popup is left open and brought to front after the frame is shown.
     */
    public static void showForIcon(BubbleTextView icon, CellLayout cellLayout) {
        showForIcon(icon, cellLayout, null);
    }

    public static void showForIcon(BubbleTextView icon, CellLayout cellLayout,
            @Nullable DragView<?> dragView) {
        if (!IconResizeHelper.canResize((ItemInfo) icon.getTag())) {
            Log.d(TAG, "showForIcon: canResize=false for " + icon.getTag());
            return;
        }
        Launcher launcher = Launcher.getLauncher(cellLayout.getContext());
        AbstractFloatingView.closeOpenViews(launcher, true, TYPE_WIDGET_RESIZE_FRAME);

        DragLayer dragLayer = launcher.getDragLayer();
        AppIconResizeFrame frame = (AppIconResizeFrame) launcher.getLayoutInflater()
                .inflate(R.layout.app_icon_resize_frame, dragLayer, false);
        frame.setup(icon, cellLayout, dragLayer, dragView);
        DragLayer.LayoutParams lp = (DragLayer.LayoutParams) frame.getLayoutParams();
        lp.customPosition = true;
        dragLayer.addView(frame);
        dragLayer.bringChildToFront(frame);
        frame.mIsOpen = true;
        frame.post(frame::ensureFrameAboveDragGhost);
        frame.requestFocus();
        launcher.getDragController().addDragListener(frame);

        PopupContainerWithArrow popup = PopupContainerWithArrow.getOpen(launcher);
        if (popup != null) {
            dragLayer.bringChildToFront(popup);
        }

        Log.d(TAG, "showForIcon: added frame for " + ((ItemInfo) icon.getTag()).title);
    }

    private void setup(BubbleTextView icon, CellLayout cellLayout, DragLayer dragLayer,
            @Nullable DragView<?> dragView) {
        mLauncher = Launcher.getLauncher(getContext());
        mIconView = icon;
        mCellLayout = cellLayout;
        mDragLayer = dragLayer;
        mDragView = dragView;
        ItemInfo info = (ItemInfo) icon.getTag();
        mStartSpanX = info.spanX;
        mStartSpanY = info.spanY;

        ensureFrameAboveDragGhost();
        icon.setResizeFrameStrokeActive(true, this::invalidate);
        attachBlurView();
        mDragHelper = new IconResizeDragHelper(mLauncher, icon, this, cellLayout);
    }

    private void attachBlurView() {
        if (mBlurView != null) {
            return;
        }
        mBlurView = IconResizeBlurHelper.createBlurView(getContext());
        LinearLayout.LayoutParams blurLp = new LinearLayout.LayoutParams(1, 1);
        blurLp.leftMargin = 0;
        blurLp.topMargin = 0;
        addView(mBlurView, 0, blurLp);
    }

    IconResizeBlurHelper.IconResizeBlurView getBlurView() {
        return mBlurView;
    }

    DragLayer getDragLayer() {
        return mDragLayer;
    }

    /**
     * Called from {@link com.android.launcher3.dragndrop.DragLayer#findActiveController} so
     * handle drags work while the shortcuts popup is still open (Oppo ItemResizeFrame parity).
     */
    public boolean shouldTakeResizeTouch(MotionEvent ev) {
        if (!mIsOpen || mDragLayer == null) {
            return false;
        }
        if (!isTouchOnFrame(ev)) {
            return false;
        }
        ItemInfo info = mIconView != null ? (ItemInfo) mIconView.getTag() : null;
        if (info == null) {
            return false;
        }
        DragLayer.LayoutParams lp = (DragLayer.LayoutParams) getLayoutParams();
        int localX = (int) ev.getX() - lp.x;
        int localY = (int) ev.getY() - lp.y;
        if (mDraggingHandle || mTouchOnIconBody) {
            return true;
        }
        if (isInHandleHotRect(localX, localY, lp, info)) {
            return true;
        }
        return isTouchOnIconBody(localX, localY, info);
    }

    private boolean isInHandleHotRect(int localX, int localY, DragLayer.LayoutParams lp,
            ItemInfo info) {
        return ResizeFramePathHelper.isInHandleHotRect(
                localX, localY, lp.width, lp.height, info.spanX, info.spanY, getContext());
    }

    private boolean isTouchOnIconBody(int localX, int localY, ItemInfo info) {
        IconResizeHelper.getResizeFrameBounds(mIconView, info.spanX, info.spanY, sIconLocalRect);
        return sIconLocalRect.contains(localX, localY);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mIconView == null) {
            return;
        }
        ResizeFrameStrokeState state = mIconView.getResizeStrokeState();
        if (state == null || !state.isActive()) {
            return;
        }
        ItemInfo info = (ItemInfo) mIconView.getTag();
        int w = getWidth();
        int h = getHeight();
        IconResizeFramePainter.drawHandle(canvas, w, h, getContext(), mIconView.getIconSize(),
                info.spanX, info.spanY, state);
    }

    /**
     * Oppo draws the resize stroke on {@link ItemResizeFrame} aligned to morph bounds — not on
     * the drag ghost, which scales independently and misaligns the outline.
     */
    private void ensureFrameAboveDragGhost() {
        snapToIcon(false);
        float frameElevation = getResources().getDimension(R.dimen.drag_elevation) + 1f;
        if (mDragView != null) {
            frameElevation = mDragView.getElevation() + 2f;
        }
        setElevation(frameElevation);
        if (mDragLayer != null) {
            mDragLayer.bringChildToFront(this);
            // Oppo keeps the shortcuts popup above the frame for outside-tap routing.
            PopupContainerWithArrow popup = PopupContainerWithArrow.getOpen(mLauncher);
            if (popup != null) {
                mDragLayer.bringChildToFront(popup);
            }
        }
    }

    private void snapToIcon(boolean animate) {
        if (mIconView == null || mDragLayer == null) {
            return;
        }
        ItemInfo info = (ItemInfo) mIconView.getTag();
        IconResizeHelper.getResizeFrameBounds(mIconView, info.spanX, info.spanY, sIconLocalRect);
        if (sIconLocalRect.width() <= 0 || sIconLocalRect.height() <= 0) {
            sIconLocalRect.set(0, 0, mIconView.getIconSize(), mIconView.getIconSize());
        }

        mDragLayer.getDescendantRectRelativeToSelf(mIconView, sTmpRect);
        int toX = sTmpRect.left + sIconLocalRect.left;
        int toY = sTmpRect.top + sIconLocalRect.top;
        int toW = sIconLocalRect.width();
        int toH = sIconLocalRect.height();

        if (animate) {
            MorphIconTransitionHelper.animateFrameTo(this, toX, toY, toW, toH, null);
        } else {
            DragLayer.LayoutParams lp = (DragLayer.LayoutParams) getLayoutParams();
            lp.x = toX;
            lp.y = toY;
            lp.width = toW;
            lp.height = toH;
            requestLayout();
        }
        Log.d(TAG, "snapToIcon: frame at " + toX + "," + toY + " " + toW + "x" + toH);
    }

    private boolean handleResizeTouch(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                IconResizeHelper.dismissShortcutsPopup(mLauncher);
                mDraggingHandle = true;
                sHandleDragging = true;
                if (mDragHelper != null) {
                    mDragHelper.onDragStart(event.getRawX(), event.getRawY());
                    if (!mDragHelper.isActive()) {
                        mDragHelper.start();
                    }
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (mDraggingHandle && mDragHelper != null) {
                    mDragHelper.onDragMove(event.getRawX(), event.getRawY());
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mDraggingHandle) {
                    if (mDragHelper != null) {
                        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                            mDragHelper.commit();
                        } else {
                            mDragHelper.cancel();
                        }
                    }
                    ItemInfo info = (ItemInfo) mIconView.getTag();
                    if (info != null) {
                        mStartSpanX = info.spanX;
                        mStartSpanY = info.spanY;
                    }
                    mDraggingHandle = false;
                    sHandleDragging = false;
                    snapToIcon(false);
                }
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        if (shouldTakeResizeTouch(ev)) {
            return true;
        }
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (!isTouchOnFrame(ev)) {
                close(true);
                if (mIconView != null && mDragLayer.isEventOverView(mIconView, ev)) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onControllerTouchEvent(MotionEvent ev) {
        ItemInfo info = mIconView != null ? (ItemInfo) mIconView.getTag() : null;
        if (info != null) {
            DragLayer.LayoutParams lp = (DragLayer.LayoutParams) getLayoutParams();
            int localX = (int) ev.getX() - lp.x;
            int localY = (int) ev.getY() - lp.y;
            if (mDraggingHandle || isInHandleHotRect(localX, localY, lp, info)) {
                return handleResizeTouch(ev);
            }
            if (mTouchOnIconBody || isTouchOnIconBody(localX, localY, info)) {
                return handleIconBodyTouch(ev);
            }
        }
        if (ev.getAction() == MotionEvent.ACTION_DOWN && !isTouchOnFrame(ev)) {
            close(true);
            return true;
        }
        return false;
    }

    /**
     * Oppo {@code ItemResizeFrame.onSingleTapIfNecessary}: tap the morph icon while the resize
     * frame is open should launch the app (not just dismiss the popup).
     */
    private boolean handleIconBodyTouch(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                IconResizeHelper.dismissShortcutsPopup(mLauncher);
                mXDown = event.getX();
                mYDown = event.getY();
                mDownTime = SystemClock.uptimeMillis();
                mTouchOnIconBody = true;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (mTouchOnIconBody
                        && squaredHypot(event.getX() - mXDown, event.getY() - mYDown)
                        > squaredTouchSlop(getContext())) {
                    mTouchOnIconBody = false;
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (mTouchOnIconBody && isSingleTap(event)) {
                    launchIcon();
                    close(false);
                }
                mTouchOnIconBody = false;
                return true;
            case MotionEvent.ACTION_CANCEL:
                mTouchOnIconBody = false;
                return true;
            default:
                return false;
        }
    }

    private boolean isSingleTap(MotionEvent event) {
        long elapsed = SystemClock.uptimeMillis() - mDownTime;
        return elapsed <= ViewConfiguration.getTapTimeout()
                && squaredHypot(event.getX() - mXDown, event.getY() - mYDown)
                < squaredTouchSlop(getContext());
    }

    private void launchIcon() {
        if (mIconView != null) {
            ItemClickHandler.onClick(mIconView);
        }
    }

    @Override
    public void onDragStart(DropTarget.DragObject dragObject, DragOptions options) {
        if (mIconView != null) {
            MorphIconTransitionHelper.cancel(mIconView);
        }
        if (mIconView != null) {
            mIconView.setVisibility(View.INVISIBLE);
        }
        close(false);
    }

    @Override
    public void onDragEnd() {
        // Keep frame open after pre-drag ends (Oppo ItemResizeFrame.onDragEnd is empty).
    }

    private boolean fastClosePopupIfNeeded(MotionEvent ev) {
        if (mLauncher == null || mIconView == null || mDragLayer == null) {
            return false;
        }
        DragLayer.LayoutParams lp = (DragLayer.LayoutParams) getLayoutParams();
        if (lp == null) {
            return false;
        }
        ItemInfo info = (ItemInfo) mIconView.getTag();
        int localX = (int) ev.getX() - lp.x;
        int localY = (int) ev.getY() - lp.y;
        boolean onHandle = ResizeFramePathHelper.isInHandleHotRect(
                localX, localY, lp.width, lp.height, info.spanX, info.spanY, getContext());
        IconResizeHelper.getResizeFrameBounds(mIconView, info.spanX, info.spanY, sIconLocalRect);
        boolean onIcon = sIconLocalRect.contains(localX, localY);
        if (onHandle || onIcon) {
            IconResizeHelper.dismissShortcutsPopup(mLauncher);
            return true;
        }
        return false;
    }

    private boolean isTouchOnFrame(MotionEvent ev) {
        DragLayer.LayoutParams lp = (DragLayer.LayoutParams) getLayoutParams();
        if (lp == null) {
            return false;
        }
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        return x >= lp.x && x < lp.x + lp.width && y >= lp.y && y < lp.y + lp.height;
    }

    @Override
    protected void handleClose(boolean animate) {
        IconResizeHelper.dismissShortcutsPopup(mLauncher);
        sHandleDragging = false;
        if (mDragHelper != null && mDragHelper.isActive()) {
            mDragHelper.cancel();
        }
        mDragHelper = null;
        if (mIconView != null) {
            MorphIconTransitionHelper.cancel(mIconView);
            mIconView.exitResizePreviewMode();
            mIconView.setResizeFrameStrokeActive(false, null);
        }
        mDragView = null;
        if (mLauncher != null) {
            mLauncher.getDragController().removeDragListener(this);
        }
        if (mDragLayer != null) {
            mDragLayer.removeView(this);
        }
    }

    @Override
    protected boolean isOfType(@FloatingViewType int type) {
        return (type & TYPE_WIDGET_RESIZE_FRAME) != 0;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }
}
