/*
 * Fork of androidx.viewpager2.adapter.FragmentStateAdapter (1.0.0) for COUIViewPager2.
 * OPPO/HeyTap ships the same adapter as a RecyclerView.Adapter (not a thin subclass of
 * FragmentStateAdapter), because FragmentMaxLifecycleEnforcer.inferViewPager() requires
 * ViewPager2 and COUIViewPager2 extends ViewGroup.
 */

package com.coui.appcompat.viewpager.adapter;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;
import androidx.collection.LongSparseArray;
import androidx.core.util.Preconditions;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.StatefulAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.coui.appcompat.viewpager.COUIViewPager2;

import java.util.Set;

/**
 * Fragment pages for {@link COUIViewPager2}. Same contract as
 * {@link androidx.viewpager2.adapter.FragmentStateAdapter}.
 */
public abstract class COUIFragmentStateAdapter
        extends RecyclerView.Adapter<COUIFragmentStateAdapter.FragmentViewHolder>
        implements StatefulAdapter {

    private static final String KEY_PREFIX_FRAGMENT = "f#";
    private static final String KEY_PREFIX_STATE = "s#";
    private static final long GRACE_WINDOW_TIME_MS = 10_000;

    final Lifecycle mLifecycle;
    final FragmentManager mFragmentManager;
    final LongSparseArray<Fragment> mFragments = new LongSparseArray<>();
    private final LongSparseArray<Fragment.SavedState> mSavedStates = new LongSparseArray<>();
    private final LongSparseArray<Integer> mItemIdToViewHolder = new LongSparseArray<>();

    private FragmentMaxLifecycleEnforcer mFragmentMaxLifecycleEnforcer;

    boolean mIsInGracePeriod = false;
    private boolean mHasStaleFragments = false;

    public COUIFragmentStateAdapter(@NonNull FragmentActivity fragmentActivity) {
        this(fragmentActivity.getSupportFragmentManager(), fragmentActivity.getLifecycle());
    }

    public COUIFragmentStateAdapter(@NonNull Fragment fragment) {
        this(fragment.getChildFragmentManager(), fragment.getLifecycle());
    }

    public COUIFragmentStateAdapter(@NonNull FragmentManager fragmentManager,
            @NonNull Lifecycle lifecycle) {
        mFragmentManager = fragmentManager;
        mLifecycle = lifecycle;
        super.setHasStableIds(true);
    }

    @CallSuper
    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        Preconditions.checkArgument(mFragmentMaxLifecycleEnforcer == null);
        mFragmentMaxLifecycleEnforcer = new FragmentMaxLifecycleEnforcer();
        mFragmentMaxLifecycleEnforcer.register(recyclerView);
    }

    @CallSuper
    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        mFragmentMaxLifecycleEnforcer.unregister(recyclerView);
        mFragmentMaxLifecycleEnforcer = null;
    }

    @NonNull
    public abstract Fragment createFragment(int position);

    @NonNull
    @Override
    public final FragmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return FragmentViewHolder.create(parent);
    }

    @Override
    public final void onBindViewHolder(@NonNull final FragmentViewHolder holder, int position) {
        final long itemId = holder.getItemId();
        final int viewHolderId = holder.getContainer().getId();
        final Long boundItemId = itemForViewHolder(viewHolderId);
        if (boundItemId != null && boundItemId != itemId) {
            removeFragment(boundItemId);
            mItemIdToViewHolder.remove(boundItemId);
        }

        mItemIdToViewHolder.put(itemId, viewHolderId);
        ensureFragment(position);

        final FrameLayout container = holder.getContainer();
        if (container.isAttachedToWindow()) {
            placeFragmentInViewHolder(holder);
        }

        gcFragments();
    }

    void gcFragments() {
        if (!mHasStaleFragments || shouldDelayFragmentTransactions()) {
            return;
        }

        Set<Long> toRemove = new ArraySet<>();
        for (int ix = 0; ix < mFragments.size(); ix++) {
            long itemId = mFragments.keyAt(ix);
            if (!containsItem(itemId)) {
                toRemove.add(itemId);
                mItemIdToViewHolder.remove(itemId);
            }
        }

        if (!mIsInGracePeriod) {
            mHasStaleFragments = false;
            for (int ix = 0; ix < mFragments.size(); ix++) {
                long itemId = mFragments.keyAt(ix);
                if (!isFragmentViewBound(itemId)) {
                    toRemove.add(itemId);
                }
            }
        }

        for (Long itemId : toRemove) {
            removeFragment(itemId);
        }
    }

    private boolean isFragmentViewBound(long itemId) {
        if (mItemIdToViewHolder.containsKey(itemId)) {
            return true;
        }
        Fragment fragment = mFragments.get(itemId);
        if (fragment == null) {
            return false;
        }
        View view = fragment.getView();
        if (view == null) {
            return false;
        }
        return view.getParent() != null;
    }

    @Nullable
    private Long itemForViewHolder(int viewHolderId) {
        Long boundItemId = null;
        for (int ix = 0; ix < mItemIdToViewHolder.size(); ix++) {
            if (mItemIdToViewHolder.valueAt(ix) == viewHolderId) {
                if (boundItemId != null) {
                    throw new IllegalStateException("Design assumption violated: "
                            + "a ViewHolder can only be bound to one item at a time.");
                }
                boundItemId = mItemIdToViewHolder.keyAt(ix);
            }
        }
        return boundItemId;
    }

    private void ensureFragment(int position) {
        long itemId = getItemId(position);
        if (!mFragments.containsKey(itemId)) {
            Fragment newFragment = createFragment(position);
            newFragment.setInitialSavedState(mSavedStates.get(itemId));
            mFragments.put(itemId, newFragment);
        }
    }

    @Override
    public final void onViewAttachedToWindow(@NonNull final FragmentViewHolder holder) {
        placeFragmentInViewHolder(holder);
        gcFragments();
    }

    void placeFragmentInViewHolder(@NonNull final FragmentViewHolder holder) {
        Fragment fragment = mFragments.get(holder.getItemId());
        if (fragment == null) {
            throw new IllegalStateException("Design assumption violated.");
        }
        FrameLayout container = holder.getContainer();
        View view = fragment.getView();

        if (!fragment.isAdded() && view != null) {
            throw new IllegalStateException("Design assumption violated.");
        }

        if (fragment.isAdded() && view == null) {
            scheduleViewAttach(fragment, container);
            return;
        }

        if (fragment.isAdded() && view.getParent() != null) {
            if (view.getParent() != container) {
                addViewToContainer(view, container);
            }
            return;
        }

        if (fragment.isAdded()) {
            addViewToContainer(view, container);
            return;
        }

        if (!shouldDelayFragmentTransactions()) {
            scheduleViewAttach(fragment, container);
            fragment.setMenuVisibility(false);
            mFragmentManager.beginTransaction()
                    .add(fragment, "f" + holder.getItemId())
                    .setMaxLifecycle(fragment, Lifecycle.State.STARTED)
                    .commitNow();
            mFragmentMaxLifecycleEnforcer.updateFragmentMaxLifecycle(false);
        } else {
            if (mFragmentManager.isDestroyed()) {
                return;
            }
            mLifecycle.addObserver(new LifecycleEventObserver() {
                @Override
                public void onStateChanged(@NonNull LifecycleOwner source,
                        @NonNull Lifecycle.Event event) {
                    if (shouldDelayFragmentTransactions()) {
                        return;
                    }
                    source.getLifecycle().removeObserver(this);
                    if (holder.getContainer().isAttachedToWindow()) {
                        placeFragmentInViewHolder(holder);
                    }
                }
            });
        }
    }

    private void scheduleViewAttach(final Fragment fragment,
            @NonNull final FrameLayout container) {
        mFragmentManager.registerFragmentLifecycleCallbacks(
                new FragmentManager.FragmentLifecycleCallbacks() {
                    @SuppressWarnings("ReferenceEquality")
                    @Override
                    public void onFragmentViewCreated(@NonNull FragmentManager fm,
                            @NonNull Fragment f, @NonNull View v,
                            @Nullable Bundle savedInstanceState) {
                        if (f == fragment) {
                            fm.unregisterFragmentLifecycleCallbacks(this);
                            addViewToContainer(v, container);
                        }
                    }
                }, false);
    }

    void addViewToContainer(@NonNull View v, @NonNull FrameLayout container) {
        if (container.getChildCount() > 1) {
            throw new IllegalStateException("Design assumption violated.");
        }
        if (v.getParent() == container) {
            return;
        }
        if (container.getChildCount() > 0) {
            container.removeAllViews();
        }
        if (v.getParent() != null) {
            ((ViewGroup) v.getParent()).removeView(v);
        }
        container.addView(v);
    }

    @Override
    public final void onViewRecycled(@NonNull FragmentViewHolder holder) {
        final int viewHolderId = holder.getContainer().getId();
        final Long boundItemId = itemForViewHolder(viewHolderId);
        if (boundItemId != null) {
            removeFragment(boundItemId);
            mItemIdToViewHolder.remove(boundItemId);
        }
    }

    @Override
    public final boolean onFailedToRecycleView(@NonNull FragmentViewHolder holder) {
        return true;
    }

    private void removeFragment(long itemId) {
        Fragment fragment = mFragments.get(itemId);
        if (fragment == null) {
            return;
        }

        if (fragment.getView() != null) {
            ViewParent viewParent = fragment.getView().getParent();
            if (viewParent != null) {
                ((FrameLayout) viewParent).removeAllViews();
            }
        }

        if (!containsItem(itemId)) {
            mSavedStates.remove(itemId);
        }

        if (!fragment.isAdded()) {
            mFragments.remove(itemId);
            return;
        }

        if (shouldDelayFragmentTransactions()) {
            mHasStaleFragments = true;
            return;
        }

        if (fragment.isAdded() && containsItem(itemId)) {
            mSavedStates.put(itemId, mFragmentManager.saveFragmentInstanceState(fragment));
        }
        mFragmentManager.beginTransaction().remove(fragment).commitNow();
        mFragments.remove(itemId);
    }

    boolean shouldDelayFragmentTransactions() {
        return mFragmentManager.isStateSaved();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public boolean containsItem(long itemId) {
        return itemId >= 0 && itemId < getItemCount();
    }

    @Override
    public final void setHasStableIds(boolean hasStableIds) {
        throw new UnsupportedOperationException(
                "Stable Ids are required for the adapter to function properly, and the adapter "
                        + "takes care of setting the flag.");
    }

    @NonNull
    @Override
    public final Parcelable saveState() {
        Bundle savedState = new Bundle(mFragments.size() + mSavedStates.size());

        for (int ix = 0; ix < mFragments.size(); ix++) {
            long itemId = mFragments.keyAt(ix);
            Fragment fragment = mFragments.get(itemId);
            if (fragment != null && fragment.isAdded()) {
                String key = createKey(KEY_PREFIX_FRAGMENT, itemId);
                mFragmentManager.putFragment(savedState, key, fragment);
            }
        }

        for (int ix = 0; ix < mSavedStates.size(); ix++) {
            long itemId = mSavedStates.keyAt(ix);
            if (containsItem(itemId)) {
                String key = createKey(KEY_PREFIX_STATE, itemId);
                savedState.putParcelable(key, mSavedStates.get(itemId));
            }
        }

        return savedState;
    }

    @Override
    @SuppressWarnings("deprecation")
    public final void restoreState(@NonNull Parcelable savedState) {
        if (!mSavedStates.isEmpty() || !mFragments.isEmpty()) {
            throw new IllegalStateException(
                    "Expected the adapter to be 'fresh' while restoring state.");
        }

        Bundle bundle = (Bundle) savedState;
        if (bundle.getClassLoader() == null) {
            bundle.setClassLoader(getClass().getClassLoader());
        }

        for (String key : bundle.keySet()) {
            if (isValidKey(key, KEY_PREFIX_FRAGMENT)) {
                long itemId = parseIdFromKey(key, KEY_PREFIX_FRAGMENT);
                Fragment fragment = mFragmentManager.getFragment(bundle, key);
                mFragments.put(itemId, fragment);
                continue;
            }

            if (isValidKey(key, KEY_PREFIX_STATE)) {
                long itemId = parseIdFromKey(key, KEY_PREFIX_STATE);
                Fragment.SavedState state = bundle.getParcelable(key);
                if (containsItem(itemId)) {
                    mSavedStates.put(itemId, state);
                }
                continue;
            }

            throw new IllegalArgumentException("Unexpected key in savedState: " + key);
        }

        if (!mFragments.isEmpty()) {
            mHasStaleFragments = true;
            mIsInGracePeriod = true;
            gcFragments();
            scheduleGracePeriodEnd();
        }
    }

    private void scheduleGracePeriodEnd() {
        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable runnable = () -> {
            mIsInGracePeriod = false;
            gcFragments();
        };

        mLifecycle.addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(@NonNull LifecycleOwner source,
                    @NonNull Lifecycle.Event event) {
                if (event == Lifecycle.Event.ON_DESTROY) {
                    handler.removeCallbacks(runnable);
                    source.getLifecycle().removeObserver(this);
                }
            }
        });

        handler.postDelayed(runnable, GRACE_WINDOW_TIME_MS);
    }

    @NonNull
    private static String createKey(@NonNull String prefix, long id) {
        return prefix + id;
    }

    private static boolean isValidKey(@NonNull String key, @NonNull String prefix) {
        return key.startsWith(prefix) && key.length() > prefix.length();
    }

    private static long parseIdFromKey(@NonNull String key, @NonNull String prefix) {
        return Long.parseLong(key.substring(prefix.length()));
    }

    /**
     * Pauses (STARTED) all Fragments that are attached and not a primary item.
     * Keeps primary item Fragment RESUMED. Uses {@link COUIViewPager2}.
     */
    class FragmentMaxLifecycleEnforcer {
        private ViewPager2.OnPageChangeCallback mPageChangeCallback;
        private RecyclerView.AdapterDataObserver mDataObserver;
        private LifecycleEventObserver mLifecycleObserver;
        private COUIViewPager2 mViewPager;

        private long mPrimaryItemId = RecyclerView.NO_ID;

        void register(@NonNull RecyclerView recyclerView) {
            mViewPager = inferViewPager(recyclerView);

            mPageChangeCallback = new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageScrollStateChanged(int state) {
                    updateFragmentMaxLifecycle(false);
                }

                @Override
                public void onPageSelected(int position) {
                    updateFragmentMaxLifecycle(false);
                }
            };
            mViewPager.registerOnPageChangeCallback(mPageChangeCallback);

            mDataObserver = new DataSetChangeObserver() {
                @Override
                public void onChanged() {
                    updateFragmentMaxLifecycle(true);
                }
            };
            registerAdapterDataObserver(mDataObserver);

            mLifecycleObserver = (source, event) -> updateFragmentMaxLifecycle(false);
            mLifecycle.addObserver(mLifecycleObserver);
        }

        void unregister(@NonNull RecyclerView recyclerView) {
            COUIViewPager2 viewPager = inferViewPager(recyclerView);
            viewPager.unregisterOnPageChangeCallback(mPageChangeCallback);
            unregisterAdapterDataObserver(mDataObserver);
            mLifecycle.removeObserver(mLifecycleObserver);
            mViewPager = null;
        }

        void updateFragmentMaxLifecycle(boolean dataSetChanged) {
            if (shouldDelayFragmentTransactions()) {
                return;
            }

            if (mViewPager.getScrollState() != COUIViewPager2.SCROLL_STATE_IDLE) {
                return;
            }

            if (mFragments.isEmpty() || getItemCount() == 0) {
                return;
            }

            final int currentItem = mViewPager.getCurrentItem();
            if (currentItem >= getItemCount()) {
                return;
            }

            long currentItemId = getItemId(currentItem);
            if (currentItemId == mPrimaryItemId && !dataSetChanged) {
                return;
            }

            Fragment currentItemFragment = mFragments.get(currentItemId);
            if (currentItemFragment == null || !currentItemFragment.isAdded()) {
                return;
            }

            mPrimaryItemId = currentItemId;
            FragmentTransaction transaction = mFragmentManager.beginTransaction();

            Fragment toResume = null;
            for (int ix = 0; ix < mFragments.size(); ix++) {
                long itemId = mFragments.keyAt(ix);
                Fragment fragment = mFragments.valueAt(ix);

                if (!fragment.isAdded()) {
                    continue;
                }

                if (itemId != mPrimaryItemId) {
                    transaction.setMaxLifecycle(fragment, Lifecycle.State.STARTED);
                } else {
                    toResume = fragment;
                }

                fragment.setMenuVisibility(itemId == mPrimaryItemId);
            }
            if (toResume != null) {
                transaction.setMaxLifecycle(toResume, Lifecycle.State.RESUMED);
            }

            if (!transaction.isEmpty()) {
                transaction.commitNow();
            }
        }

        @NonNull
        private COUIViewPager2 inferViewPager(@NonNull RecyclerView recyclerView) {
            ViewParent parent = recyclerView.getParent();
            if (parent instanceof COUIViewPager2) {
                return (COUIViewPager2) parent;
            }
            throw new IllegalStateException("Expected COUIViewPager2 instance. Got: " + parent);
        }
    }

    public abstract static class DataSetChangeObserver extends RecyclerView.AdapterDataObserver {
        @Override
        public abstract void onChanged();

        @Override
        public final void onItemRangeChanged(int positionStart, int itemCount) {
            onChanged();
        }

        @Override
        public final void onItemRangeChanged(int positionStart, int itemCount,
                @Nullable Object payload) {
            onChanged();
        }

        @Override
        public final void onItemRangeInserted(int positionStart, int itemCount) {
            onChanged();
        }

        @Override
        public final void onItemRangeRemoved(int positionStart, int itemCount) {
            onChanged();
        }

        @Override
        public final void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            onChanged();
        }
    }

    public static final class FragmentViewHolder extends RecyclerView.ViewHolder {
        private FragmentViewHolder(@NonNull FrameLayout container) {
            super(container);
        }

        @NonNull
        static FragmentViewHolder create(@NonNull ViewGroup parent) {
            FrameLayout container = new FrameLayout(parent.getContext());
            container.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            container.setId(ViewCompat.generateViewId());
            container.setSaveEnabled(false);
            return new FragmentViewHolder(container);
        }

        @NonNull
        FrameLayout getContainer() {
            return (FrameLayout) itemView;
        }
    }
}
