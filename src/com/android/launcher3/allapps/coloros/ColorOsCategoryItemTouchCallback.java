package com.android.launcher3.allapps.coloros;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.function.BooleanSupplier;

/**
 * Oppo Categories long-press reorder ({@code OplusCategoryItemTouchHelperCallback}).
 *
 * <p>Drives {@link ColorOsCategoryDragDecoration} outlines, Recent dimming, and select scale.
 * Chrome fade (tabs / menu / search) is handled by the host via {@code dragStateChanged}.
 */
final class ColorOsCategoryItemTouchCallback extends ItemTouchHelper.Callback {

    private static final PathInterpolator PRESS_IN =
            new PathInterpolator(0.17f, 0f, 0.83f, 1f);
    private static final PathInterpolator PRESS_OUT =
            new PathInterpolator(0.4f, 0f, 0.2f, 1f);
    /** Oppo suggestion gray alpha ({@code setAlphaAnimation(..., true)} → 0.2). */
    private static final float RECENT_DIM_ALPHA = 0.2f;
    private static final float START_OUTLINE_DISTANCE_DP = 20f;

    private final ColorOsCategoryAdapter mAdapter;
    private final ColorOsCategoryDragDecoration mDecoration;
    private final BooleanSupplier mDragAllowed;
    @Nullable private final Runnable mDragStateChanged;

    @Nullable private AnimatorSet mScaleSet;
    @Nullable private ValueAnimator mBgAlphaAnim;
    @Nullable private ValueAnimator mLastBgAlphaAnim;
    @Nullable private RecyclerView mRecyclerView;
    private boolean mDragging;
    private boolean mNeedBgShowAnim;
    private float mStartOutlineDistancePx;

    ColorOsCategoryItemTouchCallback(@NonNull ColorOsCategoryAdapter adapter,
            @NonNull ColorOsCategoryDragDecoration decoration,
            @NonNull BooleanSupplier dragAllowed,
            @Nullable Runnable dragStateChanged) {
        mAdapter = adapter;
        mDecoration = decoration;
        mDragAllowed = dragAllowed;
        mDragStateChanged = dragStateChanged;
    }

    boolean isDragging() {
        return mDragging;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return mDragAllowed.getAsBoolean();
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView,
            @NonNull RecyclerView.ViewHolder viewHolder) {
        if (!mDragAllowed.getAsBoolean()
                || !mAdapter.isCategoryRow(viewHolder.getBindingAdapterPosition())) {
            return 0;
        }
        return makeMovementFlags(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN
                        | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT,
                0);
    }

    @Override
    public boolean canDropOver(@NonNull RecyclerView recyclerView,
            @NonNull RecyclerView.ViewHolder current,
            @NonNull RecyclerView.ViewHolder target) {
        return mAdapter.isCategoryRow(current.getBindingAdapterPosition())
                && mAdapter.isCategoryRow(target.getBindingAdapterPosition());
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
            @NonNull RecyclerView.ViewHolder viewHolder,
            @NonNull RecyclerView.ViewHolder target) {
        int from = viewHolder.getBindingAdapterPosition();
        int to = target.getBindingAdapterPosition();
        if (!mAdapter.swapPosition(from, to)) {
            return false;
        }
        // Oppo: outline hops to the new slot; previous slot fades out.
        mDecoration.updateLastPosition(from);
        mDecoration.updateDraggedPosition(to);
        animateBgMove();
        recyclerView.invalidateItemDecorations();
        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        // Swipe disabled.
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
            @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
            int actionState, boolean isCurrentlyActive) {
        // Oppo: outline fades in after the finger moves ~20dp (not on press alone).
        if (isCurrentlyActive && mNeedBgShowAnim) {
            if (mStartOutlineDistancePx <= 0f) {
                mStartOutlineDistancePx = START_OUTLINE_DISTANCE_DP
                        * recyclerView.getResources().getDisplayMetrics().density;
            }
            if (Math.hypot(dX, dY) > mStartOutlineDistancePx) {
                mNeedBgShowAnim = false;
                animateBgAlpha(true);
            }
        }
        // Keep the dragged card fully opaque (default UI util can look washed-out).
        viewHolder.itemView.setAlpha(1f);
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    @Override
    public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
            mDragging = true;
            mNeedBgShowAnim = true;
            if (viewHolder.itemView.getParent() instanceof RecyclerView) {
                mRecyclerView = (RecyclerView) viewHolder.itemView.getParent();
            }
            int pos = viewHolder.getBindingAdapterPosition();
            mDecoration.updateDraggedPosition(pos);
            mDecoration.updateLastPosition(-1);
            mDecoration.captureMetricsFrom(viewHolder.itemView);
            mDecoration.setBgAlpha(0f);
            mDecoration.setLastBgAlpha(0f);
            viewHolder.setIsRecyclable(false);
            viewHolder.itemView.setAlpha(1f);
            viewHolder.itemView.bringToFront();
            startSelectAnim(viewHolder.itemView, true);
            setRecentDimmed(mRecyclerView, true);
            notifyDragState();
            return;
        }
        if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
            if (!mNeedBgShowAnim) {
                animateBgAlpha(false);
            }
            mNeedBgShowAnim = false;
            mDragging = false;
            notifyDragState();
        }
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView,
            @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        startSelectAnim(viewHolder.itemView, false);
        setRecentDimmed(recyclerView, false);
        viewHolder.setIsRecyclable(true);
        viewHolder.itemView.setAlpha(1f);
        viewHolder.itemView.setElevation(0f);
        viewHolder.itemView.setTranslationZ(0f);
        mDecoration.clear();
        recyclerView.invalidateItemDecorations();
        mDragging = false;
        mNeedBgShowAnim = false;
        notifyDragState();
    }

    private void notifyDragState() {
        if (mDragStateChanged != null) {
            mDragStateChanged.run();
        }
    }

    private void animateBgAlpha(boolean show) {
        cancelBgAlphaAnim();
        float from = mDecoration.getBgAlpha();
        float to = show ? 1f : 0f;
        mBgAlphaAnim = ValueAnimator.ofFloat(from, to);
        mBgAlphaAnim.setDuration(220);
        mBgAlphaAnim.addUpdateListener(a -> {
            mDecoration.setBgAlpha((float) a.getAnimatedValue());
            if (mRecyclerView != null) {
                mRecyclerView.invalidateItemDecorations();
            }
        });
        mBgAlphaAnim.start();
    }

    private void animateBgMove() {
        cancelBgAlphaAnim();
        mDecoration.setBgAlpha(1f);
        mDecoration.setLastBgAlpha(1f);
        mBgAlphaAnim = ValueAnimator.ofFloat(0f, 1f);
        mBgAlphaAnim.setDuration(220);
        mBgAlphaAnim.addUpdateListener(a -> {
            // Current outline stays fully visible; last slot fades out.
            float t = (float) a.getAnimatedValue();
            mDecoration.setBgAlpha(1f);
            mDecoration.setLastBgAlpha(1f - t);
            if (mRecyclerView != null) {
                mRecyclerView.invalidateItemDecorations();
            }
        });
        mBgAlphaAnim.start();
        mLastBgAlphaAnim = mBgAlphaAnim;
    }

    private void cancelBgAlphaAnim() {
        if (mBgAlphaAnim != null) {
            mBgAlphaAnim.cancel();
            mBgAlphaAnim = null;
        }
        if (mLastBgAlphaAnim != null && mLastBgAlphaAnim != mBgAlphaAnim) {
            mLastBgAlphaAnim.cancel();
            mLastBgAlphaAnim = null;
        }
    }

    private void startSelectAnim(@NonNull View itemView, boolean selecting) {
        cancelScaleAnim();
        TextView title = findTitle(itemView);
        View card = findCard(itemView);
        if (selecting) {
            float elev = 8f * itemView.getResources().getDisplayMetrics().density;
            itemView.setElevation(elev);
            itemView.setTranslationZ(elev);
            ObjectAnimator sx1 = ObjectAnimator.ofFloat(itemView, View.SCALE_X,
                    itemView.getScaleX(), 0.96f);
            ObjectAnimator sy1 = ObjectAnimator.ofFloat(itemView, View.SCALE_Y,
                    itemView.getScaleY(), 0.96f);
            sx1.setDuration(200);
            sy1.setDuration(200);
            sx1.setInterpolator(PRESS_IN);
            sy1.setInterpolator(PRESS_IN);
            ObjectAnimator sx2 = ObjectAnimator.ofFloat(itemView, View.SCALE_X, 0.96f, 1.05f);
            ObjectAnimator sy2 = ObjectAnimator.ofFloat(itemView, View.SCALE_Y, 0.96f, 1.05f);
            sx2.setDuration(355);
            sy2.setDuration(355);
            sx2.setStartDelay(200);
            sy2.setStartDelay(200);
            sx2.setInterpolator(PRESS_OUT);
            sy2.setInterpolator(PRESS_OUT);
            mScaleSet = new AnimatorSet();
            mScaleSet.playTogether(sx1, sy1, sx2, sy2);
            mScaleSet.start();
            if (title != null) {
                title.animate().alpha(0f).setDuration(180).start();
            }
            if (card != null) {
                card.setPressed(false);
            }
        } else {
            itemView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(250)
                    .setInterpolator(PRESS_OUT)
                    .start();
            if (title != null) {
                title.animate().alpha(1f).setDuration(200).start();
            }
        }
    }

    private void cancelScaleAnim() {
        if (mScaleSet != null) {
            mScaleSet.cancel();
            mScaleSet = null;
        }
    }

    private static void setRecentDimmed(@Nullable RecyclerView recyclerView, boolean dim) {
        if (recyclerView == null) {
            return;
        }
        float target = dim ? RECENT_DIM_ALPHA : 1f;
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            View child = recyclerView.getChildAt(i);
            RecyclerView.ViewHolder vh = recyclerView.getChildViewHolder(child);
            if (vh != null && vh.getItemViewType() == ColorOsCategoryAdapter.TYPE_RECENT) {
                child.animate().cancel();
                child.animate().alpha(target).setDuration(220).start();
                child.setEnabled(!dim);
            }
        }
    }

    @Nullable
    private static View findCard(@NonNull View itemView) {
        if (!(itemView instanceof ViewGroup)) {
            return null;
        }
        ViewGroup root = (ViewGroup) itemView;
        return root.getChildCount() > 0 ? root.getChildAt(0) : null;
    }

    @Nullable
    private static TextView findTitle(@NonNull View itemView) {
        if (!(itemView instanceof ViewGroup)) {
            return null;
        }
        ViewGroup root = (ViewGroup) itemView;
        if (root.getChildCount() > 1 && root.getChildAt(1) instanceof TextView) {
            return (TextView) root.getChildAt(1);
        }
        return null;
    }
}
