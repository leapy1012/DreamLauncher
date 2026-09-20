package com.android.launcher3.screenedit;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.R;

/** OPPO-style horizontal transition strip backed by RecyclerView. */
public class GridGallery extends RecyclerView {

    private GridGalleryAdapter mGalleryAdapter;

    public GridGallery(Context context) {
        this(context, null);
    }

    public GridGallery(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GridGallery(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutManager(new LinearLayoutManager(context, HORIZONTAL, false));
        setClipChildren(false);
        setClipToPadding(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
        setHorizontalScrollBarEnabled(false);
        int sidePadding = getResources().getDimensionPixelSize(
                R.dimen.transition_effect_list_side_peek);
        int panelHeight = getResources().getDimensionPixelSize(
                R.dimen.transition_effect_panel_height);
        int itemHeight = getResources().getDimensionPixelSize(
                R.dimen.transition_effect_item_height);
        int verticalPadding = Math.max(0, (panelHeight - itemHeight) / 2);
        setPadding(sidePadding, verticalPadding, sidePadding, verticalPadding);
        int divider = getResources().getDimensionPixelSize(
                R.dimen.transition_effect_item_divider);
        addItemDecoration(new ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                    @NonNull RecyclerView parent, @NonNull State state) {
                outRect.left = divider;
                outRect.right = divider;
            }
        });
    }

    public GridGalleryAdapter getAdapterData() {
        return mGalleryAdapter;
    }

    public void setGalleryAdapter(GridGalleryAdapter adapter) {
        mGalleryAdapter = adapter;
        setAdapter(adapter == null ? null : new BridgeAdapter(adapter));
    }

    public boolean handleBackPressed() {
        return mGalleryAdapter != null && mGalleryAdapter.onBackPressed();
    }

    public void setCurrentItem(int position) {
        if (position <= 0) {
            return;
        }
        scrollToPosition(position);
        post(() -> smoothScrollToPosition(position));
    }

    private final class BridgeAdapter extends Adapter<BridgeHolder> {
        private final GridGalleryAdapter mDelegate;

        BridgeAdapter(GridGalleryAdapter delegate) {
            mDelegate = delegate;
            setHasStableIds(true);
        }

        @NonNull
        @Override
        public BridgeHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new BridgeHolder(mDelegate.getView(0, null, parent));
        }

        @Override
        public void onBindViewHolder(@NonNull BridgeHolder holder, int position) {
            // Re-bind so click listeners / selection state stay correct (OPPO binds onBind).
            mDelegate.getView(position, holder.itemView, GridGallery.this);
            holder.itemView.setAlpha(0f);
            holder.itemView.setTranslationY(100f);
            holder.itemView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(position * 17L)
                    .setDuration(420L)
                    .start();
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return mDelegate.getCount();
        }
    }

    private static final class BridgeHolder extends ViewHolder {
        BridgeHolder(View itemView) {
            super(itemView);
        }
    }
}
