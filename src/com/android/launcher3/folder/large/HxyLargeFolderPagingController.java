package com.android.launcher3.folder.large;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.animation.PathInterpolator;

import com.android.launcher3.Launcher;
import com.android.launcher3.Workspace;
import com.android.launcher3.views.ActivityContext;

/**
 * ColorOS-style closed big-folder horizontal page swipe (gesture + snap + fling).
 */
public class HxyLargeFolderPagingController {
    private static final float SNAP_THRESHOLD_DP = 20f;
    private static final float FLING_VELOCITY_DP = 200f;
    private static final long SNAP_DURATION_MS = 280;

    private final HxyLargeFolderIcon mIcon;
    private final PointF mDown = new PointF();
    private final PointF mScrollStartPoint = new PointF();
    private final int mTouchSlop;
    private final float mSnapThresholdPx;
    private final float mFlingVelocityPx;

    private boolean mTracking;
    private boolean mScrollStart;
    private float mScrollStartX;
    private int mScrollStartPage;
    private int mPrePage;
    private int mNextPage = -1;
    private int mActivePointerId = -1;
    private ValueAnimator mSnapAnimator;
    private VelocityTracker mVelocityTracker;
    /** Ignore {@link Animator#cancel()} end callbacks from a replaced snap animator. */
    private int mSnapGeneration;

    public HxyLargeFolderPagingController(HxyLargeFolderIcon icon) {
        mIcon = icon;
        Context context = icon.getContext();
        float density = context.getResources().getDisplayMetrics().density;
        ViewConfiguration vc = ViewConfiguration.get(context);
        mTouchSlop = vc.getScaledTouchSlop();
        mSnapThresholdPx = density * SNAP_THRESHOLD_DP;
        mFlingVelocityPx = Math.max(vc.getScaledMinimumFlingVelocity(), density * FLING_VELOCITY_DP);
    }

    public void abort() {
        if (mSnapAnimator != null) {
            mSnapAnimator.cancel();
            mSnapAnimator = null;
        }
        recycleVelocity();
        mScrollStart = false;
        mTracking = false;
        setWorkspaceIntercept(false);
        // Allow DragLayer / ancestors to intercept again (e.g. after long-press → drag).
        if (mIcon.getParent() != null) {
            mIcon.getParent().requestDisallowInterceptTouchEvent(false);
        }
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (!mIcon.canPageSwipe()) {
            return false;
        }
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                return onDown(ev);
            case MotionEvent.ACTION_MOVE:
                return onMove(ev);
            case MotionEvent.ACTION_UP:
                return onUp(false, ev);
            case MotionEvent.ACTION_CANCEL:
                return onUp(true, ev);
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                return mScrollStart;
            default:
                return mScrollStart;
        }
    }

    private boolean onDown(MotionEvent ev) {
        if (mSnapAnimator != null && mSnapAnimator.isRunning()) {
            mSnapAnimator.cancel();
        }
        mActivePointerId = ev.getPointerId(0);
        mDown.set(ev.getX(), ev.getY());
        mScrollStartPoint.set(mDown);
        mTracking = mIcon.isInSwipeArea(ev.getX(), ev.getY());
        mScrollStart = false;
        mNextPage = -1;
        mScrollStartPage = mIcon.getPreviewPage();
        mPrePage = mScrollStartPage;
        mScrollStartX = mIcon.getScrollDistance();
        if (mTracking) {
            // Claim Workspace early so home pages don't steal the gesture, but do NOT
            // requestDisallowIntercept yet — that blocks DragLayer from promoting
            // long-press pre-drag into a real drag on multi-page folders.
            setWorkspaceIntercept(true);
        }
        return false;
    }

    private boolean onMove(MotionEvent ev) {
        if (!mTracking) {
            return false;
        }
        int index = ev.findPointerIndex(mActivePointerId);
        if (index < 0) {
            return false;
        }
        float x = ev.getX(index);
        float y = ev.getY(index);
        float dx = x - mDown.x;
        float dy = y - mDown.y;
        boolean horiz = Math.abs(dx) > Math.abs(dy);

        if (!mScrollStart) {
            if (Math.abs(dx) > mTouchSlop || Math.abs(dy) > mTouchSlop) {
                if (!horiz) {
                    mTracking = false;
                    setWorkspaceIntercept(false);
                    if (mIcon.getParent() != null) {
                        mIcon.getParent().requestDisallowInterceptTouchEvent(false);
                    }
                    return false;
                }
                mScrollStart = true;
                mScrollStartPoint.set(x, y);
                mScrollStartX = mIcon.getScrollDistance();
                mScrollStartPage = mIcon.getPreviewPage();
                mPrePage = mScrollStartPage;
                setWorkspaceIntercept(true);
                // Paging committed — now block ancestors from intercepting the swipe.
                if (mIcon.getParent() != null) {
                    mIcon.getParent().requestDisallowInterceptTouchEvent(true);
                }
                mIcon.cancelLongPress();
                mIcon.onFolderScrollPageStart();
            }
        }
        if (!mScrollStart) {
            return false;
        }

        float deltaX = x - mScrollStartPoint.x;
        applyScroll(deltaX);
        return true;
    }

    private boolean onUp(boolean cancelled, MotionEvent ev) {
        setWorkspaceIntercept(false);
        if (mIcon.getParent() != null) {
            mIcon.getParent().requestDisallowInterceptTouchEvent(false);
        }
        if (!mScrollStart) {
            mTracking = false;
            recycleVelocity();
            return false;
        }

        float velocityX = 0f;
        if (mVelocityTracker != null && !cancelled) {
            mVelocityTracker.computeCurrentVelocity(1000);
            velocityX = mVelocityTracker.getXVelocity(mActivePointerId);
        }
        int index = ev.findPointerIndex(mActivePointerId);
        float upX = index >= 0 ? ev.getX(index) : ev.getX();
        float fingerDx = upX - mDown.x;
        recycleVelocity();

        int pageCount = mIcon.getPreviewPageCount();
        int pageW = Math.max(1, mIcon.getPageWidth());
        float scroll = mIcon.getScrollDistance();
        int startPage = mScrollStartPage;
        int target = startPage;

        if (!cancelled) {
            boolean fling = Math.abs(velocityX) > mFlingVelocityPx;
            boolean dragged = Math.abs(fingerDx) > mSnapThresholdPx
                    || Math.abs(scroll - startPage * pageW) > pageW * 0.35f;
            if (fling) {
                // Finger left (vx < 0) → next page; right → previous.
                target = velocityX < 0
                        ? Math.min(pageCount - 1, startPage + 1)
                        : Math.max(0, startPage - 1);
            } else if (dragged) {
                // Prefer neighbor tracked during drag; else nearest page.
                if (mNextPage != -1 && mNextPage != startPage) {
                    target = mNextPage;
                } else {
                    target = Math.round(scroll / pageW);
                }
            } else {
                target = startPage;
            }
        }

        target = Math.max(0, Math.min(pageCount - 1, target));
        snapToPage(target, true);
        mScrollStart = false;
        mTracking = false;
        mNextPage = -1;
        return true;
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        int pointerIndex = ev.getActionIndex();
        if (ev.getPointerId(pointerIndex) == mActivePointerId) {
            int newIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = ev.getPointerId(newIndex);
            mScrollStartPoint.set(ev.getX(newIndex), ev.getY(newIndex));
            mScrollStartX = mIcon.getScrollDistance();
        }
    }

    /**
     * @param deltaX finger travel since scroll start (positive = finger moved right)
     */
    private void applyScroll(float deltaX) {
        int pageCount = mIcon.getPreviewPageCount();
        int pageW = Math.max(1, mIcon.getPageWidth());
        float max = (pageCount - 1) * pageW;
        // LTR: finger right → content follows → scroll decreases.
        float scroll = mScrollStartX - deltaX;
        if (scroll < 0) {
            scroll = scroll / 3f;
        } else if (scroll > max) {
            scroll = max + (scroll - max) / 3f;
        }

        float pageFloat = scroll / pageW;
        int floor = (int) Math.floor(pageFloat);
        int ceil = (int) Math.ceil(pageFloat);
        // Oppo: swap so mPrePage is the page behind the drag and mNextPage is the destination.
        if (deltaX > 0f) {
            int tmp = floor;
            floor = ceil;
            ceil = tmp;
        }
        if (floor >= 0 && floor < pageCount) {
            mPrePage = floor;
        }
        if (ceil >= 0 && ceil < pageCount) {
            mNextPage = ceil;
        } else {
            mNextPage = -1;
        }
        if (mPrePage == mNextPage) {
            mNextPage = -1;
        }
        mIcon.updateScrollDistance(scroll, true);
    }

    public void snapToPage(int page, boolean animate) {
        int pageCount = mIcon.getPreviewPageCount();
        page = Math.max(0, Math.min(pageCount - 1, page));
        float target = page * Math.max(1, mIcon.getPageWidth());
        if (mSnapAnimator != null) {
            mSnapAnimator.cancel();
            mSnapAnimator = null;
        }
        if (!animate) {
            mIcon.updateScrollDistance(target, false);
            mIcon.onFolderScrollPageEnd(page);
            return;
        }
        final int targetPage = page;
        final int generation = ++mSnapGeneration;
        float start = mIcon.getScrollDistance();
        mSnapAnimator = ValueAnimator.ofFloat(start, target);
        mSnapAnimator.setDuration(SNAP_DURATION_MS);
        mSnapAnimator.setInterpolator(new PathInterpolator(0.25f, 0.1f, 0.25f, 1f));
        mSnapAnimator.addUpdateListener(a -> {
            if (generation != mSnapGeneration) {
                return;
            }
            mIcon.updateScrollDistance((Float) a.getAnimatedValue(), true);
        });
        mSnapAnimator.addListener(new AnimatorListenerAdapter() {
            private boolean mCancelled;

            @Override
            public void onAnimationCancel(Animator animation) {
                mCancelled = true;
            }

            @Override
            public void onAnimationStart(Animator animation) {
                if (generation != mSnapGeneration) {
                    return;
                }
                mIcon.onFolderScrollPageStart();
                mIcon.setIndicatorPage(targetPage);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (mCancelled || generation != mSnapGeneration) {
                    return;
                }
                mSnapAnimator = null;
                mIcon.updateScrollDistance(target, false);
                mIcon.onFolderScrollPageEnd(targetPage);
            }
        });
        mSnapAnimator.start();
    }

    private void recycleVelocity() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private void setWorkspaceIntercept(boolean intercept) {
        ActivityContext activity = ActivityContext.lookupContext(mIcon.getContext());
        if (activity instanceof Launcher) {
            Workspace workspace = ((Launcher) activity).getWorkspace();
            if (workspace != null) {
                workspace.setBigFolderIntercept(intercept);
            }
        }
    }
}
