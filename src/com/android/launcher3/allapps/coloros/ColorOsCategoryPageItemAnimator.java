package com.android.launcher3.allapps.coloros;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewPropertyAnimator;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.coui.appcompat.animation.COUIEaseInterpolator;
import com.coui.appcompat.animation.COUIMoveEaseInterpolator;
import com.coui.appcompat.animation.COUISpringInterpolator;

import java.util.ArrayList;
import java.util.List;

/**
 * Oppo {@code CategoryPageItemAnimator} for Categories reorder.
 *
 * <p>Move uses {@link COUISpringInterpolator}{@code 0.5, 0.0} over 600ms so sibling cards
 * spring into their new slots while a group is dragged.
 */
final class ColorOsCategoryPageItemAnimator extends SimpleItemAnimator {

    private static final long MOVE_DURATION_MS = 600L;
    private static final long REMOVE_DURATION_MS = 400L;
    private static final long ADD_DURATION_MS = 350L;

    private static final TimeInterpolator sDefaultInterpolator =
            new ValueAnimator().getInterpolator();

    private final ArrayList<RecyclerView.ViewHolder> mPendingRemovals = new ArrayList<>();
    private final ArrayList<RecyclerView.ViewHolder> mPendingAdditions = new ArrayList<>();
    private final ArrayList<MoveInfo> mPendingMoves = new ArrayList<>();

    final ArrayList<ArrayList<RecyclerView.ViewHolder>> mAdditionsList = new ArrayList<>();
    final ArrayList<ArrayList<MoveInfo>> mMovesList = new ArrayList<>();

    final ArrayList<RecyclerView.ViewHolder> mAddAnimations = new ArrayList<>();
    final ArrayList<RecyclerView.ViewHolder> mMoveAnimations = new ArrayList<>();
    final ArrayList<RecyclerView.ViewHolder> mRemoveAnimations = new ArrayList<>();

    private boolean mIgnoreAnimation;

    ColorOsCategoryPageItemAnimator() {
        setSupportsChangeAnimations(false);
        setMoveDuration(MOVE_DURATION_MS);
        setRemoveDuration(REMOVE_DURATION_MS);
        setAddDuration(ADD_DURATION_MS);
    }

    void setIgnoreAnimation(boolean ignore) {
        mIgnoreAnimation = ignore;
    }

    private static boolean isCategoryRowType(int viewType) {
        return viewType == ColorOsCategoryAdapter.TYPE_RECENT
                || viewType == ColorOsCategoryAdapter.TYPE_CATEGORY;
    }

    @Override
    public boolean animateMove(@NonNull RecyclerView.ViewHolder holder,
            int fromX, int fromY, int toX, int toY) {
        if (!isCategoryRowType(holder.getItemViewType()) || mIgnoreAnimation) {
            dispatchMoveFinished(holder);
            return false;
        }
        View view = holder.itemView;
        int startX = fromX + (int) view.getTranslationX();
        int startY = fromY + (int) view.getTranslationY();
        resetAnimation(holder);
        int deltaX = toX - startX;
        int deltaY = toY - startY;
        if (deltaX == 0 && deltaY == 0) {
            dispatchMoveFinished(holder);
            return false;
        }
        if (deltaX != 0) {
            view.setTranslationX(-deltaX);
        }
        if (deltaY != 0) {
            view.setTranslationY(-deltaY);
        }
        mPendingMoves.add(new MoveInfo(holder, startX, startY, toX, toY));
        return true;
    }

    void animateMoveImpl(@NonNull final RecyclerView.ViewHolder holder,
            int fromX, int fromY, int toX, int toY) {
        final View view = holder.itemView;
        final int deltaX = toX - fromX;
        final int deltaY = toY - fromY;
        if (deltaX != 0) {
            view.animate().translationX(0f);
        }
        if (deltaY != 0) {
            view.animate().translationY(0f);
        }
        final ViewPropertyAnimator animator = view.animate();
        mMoveAnimations.add(holder);
        // Oppo: COUISpringInterpolator(0.5, 0.0), 600ms.
        animator.setInterpolator(new COUISpringInterpolator(0.5d, 0.0d))
                .setDuration(MOVE_DURATION_MS)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        if (view.getElevation() <= 1f) {
                            view.setElevation(1f);
                        }
                        dispatchMoveStarting(holder);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        if (view.getElevation() <= 1f) {
                            view.setElevation(0f);
                        }
                        if (deltaX != 0) {
                            view.setTranslationX(0f);
                        }
                        if (deltaY != 0) {
                            view.setTranslationY(0f);
                        }
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (view.getElevation() <= 1f) {
                            view.setElevation(0f);
                        }
                        animator.setListener(null);
                        dispatchMoveFinished(holder);
                        mMoveAnimations.remove(holder);
                        dispatchFinishedWhenDone();
                    }
                }).start();
    }

    @Override
    public boolean animateAdd(@NonNull RecyclerView.ViewHolder holder) {
        if (!isCategoryRowType(holder.getItemViewType()) || mIgnoreAnimation) {
            dispatchAddFinished(holder);
            return false;
        }
        resetAnimation(holder);
        holder.itemView.setAlpha(0f);
        holder.itemView.setScaleX(1f);
        holder.itemView.setScaleY(1f);
        mPendingAdditions.add(holder);
        return true;
    }

    void animateAddImpl(@NonNull final RecyclerView.ViewHolder holder) {
        final View view = holder.itemView;
        view.setAlpha(0f);
        view.setScaleX(0.8f);
        view.setScaleY(0.8f);
        mAddAnimations.add(holder);
        final ViewPropertyAnimator animator = view.animate();
        animator.alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(ADD_DURATION_MS)
                .setInterpolator(new COUIEaseInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        dispatchAddStarting(holder);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        view.setAlpha(1f);
                        if (!mIgnoreAnimation) {
                            view.setScaleX(1f);
                            view.setScaleY(1f);
                        }
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        animator.setListener(null);
                        view.setAlpha(1f);
                        view.setScaleX(1f);
                        view.setScaleY(1f);
                        dispatchAddFinished(holder);
                        mAddAnimations.remove(holder);
                        dispatchFinishedWhenDone();
                    }
                }).start();
    }

    @Override
    public boolean animateRemove(@NonNull RecyclerView.ViewHolder holder) {
        if (!isCategoryRowType(holder.getItemViewType()) || mIgnoreAnimation) {
            dispatchRemoveFinished(holder);
            return false;
        }
        resetAnimation(holder);
        holder.itemView.setAlpha(1f);
        holder.itemView.setScaleX(1f);
        holder.itemView.setScaleY(1f);
        mPendingRemovals.add(holder);
        return true;
    }

    private void animateRemoveImpl(@NonNull final RecyclerView.ViewHolder holder) {
        final View view = holder.itemView;
        final ViewPropertyAnimator animator = view.animate();
        mRemoveAnimations.add(holder);
        animator.setInterpolator(new COUIMoveEaseInterpolator())
                .setDuration(REMOVE_DURATION_MS)
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        dispatchRemoveStarting(holder);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        animator.setListener(null);
                        view.setAlpha(1f);
                        if (!mIgnoreAnimation) {
                            view.setScaleX(1f);
                            view.setScaleY(1f);
                        }
                        dispatchRemoveFinished(holder);
                        mRemoveAnimations.remove(holder);
                        dispatchFinishedWhenDone();
                    }
                }).start();
    }

    @Override
    public boolean animateChange(@NonNull RecyclerView.ViewHolder oldHolder,
            @NonNull RecyclerView.ViewHolder newHolder,
            int fromLeft, int fromTop, int toLeft, int toTop) {
        // Change not used for category reorder; finish immediately.
        resetAnimation(oldHolder);
        dispatchChangeFinished(oldHolder, true);
        if (newHolder != oldHolder) {
            resetAnimation(newHolder);
            dispatchChangeFinished(newHolder, false);
        }
        return false;
    }

    @Override
    public void runPendingAnimations() {
        boolean removalsPending = !mPendingRemovals.isEmpty();
        boolean movesPending = !mPendingMoves.isEmpty();
        boolean additionsPending = !mPendingAdditions.isEmpty();
        if (!removalsPending && !movesPending && !additionsPending) {
            return;
        }

        for (RecyclerView.ViewHolder holder : mPendingRemovals) {
            animateRemoveImpl(holder);
        }
        mPendingRemovals.clear();

        if (movesPending) {
            final ArrayList<MoveInfo> moves = new ArrayList<>(mPendingMoves);
            mMovesList.add(moves);
            mPendingMoves.clear();
            Runnable mover = () -> {
                for (MoveInfo move : moves) {
                    animateMoveImpl(move.holder, move.fromX, move.fromY, move.toX, move.toY);
                }
                moves.clear();
                mMovesList.remove(moves);
            };
            if (!removalsPending) {
                mover.run();
            } else {
                ViewCompat.postOnAnimationDelayed(
                        moves.get(0).holder.itemView, mover, getRemoveDuration());
            }
        }

        if (additionsPending) {
            final ArrayList<RecyclerView.ViewHolder> additions =
                    new ArrayList<>(mPendingAdditions);
            mAdditionsList.add(additions);
            mPendingAdditions.clear();
            Runnable adder = () -> {
                for (RecyclerView.ViewHolder holder : additions) {
                    animateAddImpl(holder);
                }
                additions.clear();
                mAdditionsList.remove(additions);
            };
            if (!removalsPending && !movesPending) {
                adder.run();
            } else {
                long delay = (!removalsPending ? 0L : getRemoveDuration())
                        + (!movesPending ? 0L : getMoveDuration());
                ViewCompat.postOnAnimationDelayed(additions.get(0).itemView, adder, delay);
            }
        }
    }

    @Override
    public void endAnimation(@NonNull RecyclerView.ViewHolder item) {
        View view = item.itemView;
        view.animate().cancel();

        for (int i = mPendingMoves.size() - 1; i >= 0; i--) {
            if (mPendingMoves.get(i).holder == item) {
                view.setTranslationY(0f);
                view.setTranslationX(0f);
                dispatchMoveFinished(item);
                mPendingMoves.remove(i);
            }
        }
        if (mPendingRemovals.remove(item)) {
            view.setAlpha(1f);
            dispatchRemoveFinished(item);
        }
        if (mPendingAdditions.remove(item)) {
            view.setAlpha(1f);
            dispatchAddFinished(item);
        }

        for (int i = mMovesList.size() - 1; i >= 0; i--) {
            ArrayList<MoveInfo> moves = mMovesList.get(i);
            for (int j = moves.size() - 1; j >= 0; j--) {
                if (moves.get(j).holder == item) {
                    view.setTranslationY(0f);
                    view.setTranslationX(0f);
                    dispatchMoveFinished(item);
                    moves.remove(j);
                    if (moves.isEmpty()) {
                        mMovesList.remove(i);
                    }
                    break;
                }
            }
        }
        for (int i = mAdditionsList.size() - 1; i >= 0; i--) {
            ArrayList<RecyclerView.ViewHolder> additions = mAdditionsList.get(i);
            if (additions.remove(item)) {
                view.setAlpha(1f);
                dispatchAddFinished(item);
                if (additions.isEmpty()) {
                    mAdditionsList.remove(i);
                }
            }
        }

        mRemoveAnimations.remove(item);
        mAddAnimations.remove(item);
        mMoveAnimations.remove(item);
        dispatchFinishedWhenDone();
    }

    @Override
    public void endAnimations() {
        for (int i = mPendingMoves.size() - 1; i >= 0; i--) {
            MoveInfo move = mPendingMoves.get(i);
            View view = move.holder.itemView;
            view.setTranslationY(0f);
            view.setTranslationX(0f);
            dispatchMoveFinished(move.holder);
            mPendingMoves.remove(i);
        }
        for (int i = mPendingRemovals.size() - 1; i >= 0; i--) {
            dispatchRemoveFinished(mPendingRemovals.get(i));
            mPendingRemovals.remove(i);
        }
        for (int i = mPendingAdditions.size() - 1; i >= 0; i--) {
            RecyclerView.ViewHolder holder = mPendingAdditions.get(i);
            holder.itemView.setAlpha(1f);
            dispatchAddFinished(holder);
            mPendingAdditions.remove(i);
        }
        if (!isRunning()) {
            return;
        }
        for (int i = mMovesList.size() - 1; i >= 0; i--) {
            ArrayList<MoveInfo> moves = mMovesList.get(i);
            for (int j = moves.size() - 1; j >= 0; j--) {
                MoveInfo move = moves.get(j);
                View view = move.holder.itemView;
                view.setTranslationY(0f);
                view.setTranslationX(0f);
                dispatchMoveFinished(move.holder);
                moves.remove(j);
                if (moves.isEmpty()) {
                    mMovesList.remove(moves);
                }
            }
        }
        for (int i = mAdditionsList.size() - 1; i >= 0; i--) {
            ArrayList<RecyclerView.ViewHolder> additions = mAdditionsList.get(i);
            for (int j = additions.size() - 1; j >= 0; j--) {
                RecyclerView.ViewHolder holder = additions.get(j);
                holder.itemView.setAlpha(1f);
                dispatchAddFinished(holder);
                additions.remove(j);
                if (additions.isEmpty()) {
                    mAdditionsList.remove(additions);
                }
            }
        }
        cancelAll(mRemoveAnimations);
        cancelAll(mMoveAnimations);
        cancelAll(mAddAnimations);
        dispatchAnimationsFinished();
    }

    @Override
    public boolean isRunning() {
        return !mPendingAdditions.isEmpty()
                || !mPendingMoves.isEmpty()
                || !mPendingRemovals.isEmpty()
                || !mMoveAnimations.isEmpty()
                || !mRemoveAnimations.isEmpty()
                || !mAddAnimations.isEmpty()
                || !mMovesList.isEmpty()
                || !mAdditionsList.isEmpty();
    }

    @Override
    public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder,
            @NonNull List<Object> payloads) {
        return true;
    }

    void dispatchFinishedWhenDone() {
        if (!isRunning()) {
            dispatchAnimationsFinished();
        }
    }

    private void resetAnimation(@NonNull RecyclerView.ViewHolder holder) {
        holder.itemView.animate().setInterpolator(sDefaultInterpolator);
        endAnimation(holder);
    }

    private static void cancelAll(@NonNull List<RecyclerView.ViewHolder> viewHolders) {
        for (int i = viewHolders.size() - 1; i >= 0; i--) {
            viewHolders.get(i).itemView.animate().cancel();
        }
    }

    private static final class MoveInfo {
        final RecyclerView.ViewHolder holder;
        final int fromX;
        final int fromY;
        final int toX;
        final int toY;

        MoveInfo(RecyclerView.ViewHolder holder, int fromX, int fromY, int toX, int toY) {
            this.holder = holder;
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
        }
    }
}
