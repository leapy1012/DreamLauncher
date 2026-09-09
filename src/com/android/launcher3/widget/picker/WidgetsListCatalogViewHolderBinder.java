package com.android.launcher3.widget.picker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.R;
import com.android.launcher3.recyclerview.ViewHolderBinder;
import com.android.launcher3.widget.model.WidgetsListContentEntry;

import java.util.List;

/** Binds always-expanded ColorOS catalog rows. */
public final class WidgetsListCatalogViewHolderBinder
        implements ViewHolderBinder<WidgetsListContentEntry, WidgetsCatalogRowViewHolder> {

    private final LayoutInflater mLayoutInflater;
    private final OnClickListener mClickListener;
    private final OnLongClickListener mLongClickListener;
    private final int mItemSpacing;
    private final RecyclerView.RecycledViewPool mViewPool = new RecyclerView.RecycledViewPool();

    public WidgetsListCatalogViewHolderBinder(
            @NonNull Context context,
            LayoutInflater layoutInflater,
            OnClickListener clickListener,
            OnLongClickListener longClickListener) {
        mLayoutInflater = layoutInflater;
        mClickListener = clickListener;
        mLongClickListener = longClickListener;
        mItemSpacing = context.getResources().getDimensionPixelSize(
                R.dimen.toggle_bar_widget_hz_item_spacing);
        mViewPool.setMaxRecycledViews(0, 16);
    }

    @Override
    public WidgetsCatalogRowViewHolder newViewHolder(ViewGroup parent) {
        WidgetsCatalogRowViewHolder holder = new WidgetsCatalogRowViewHolder(
                mLayoutInflater.inflate(R.layout.widgets_list_row_view, parent, false));
        holder.recyclerView.addItemDecoration(new WidgetsHzAdapter.SpacingDecoration(mItemSpacing));
        holder.recyclerView.setRecycledViewPool(mViewPool);
        return holder;
    }

    @Override
    public void bindViewHolder(WidgetsCatalogRowViewHolder holder, WidgetsListContentEntry entry,
            @ListPosition int position, List<Object> payloads) {
        holder.title.applyFromItemInfoWithIcon(entry.mPkgItem);
        holder.title.setOnClickListener(null);
        holder.title.setClickable(false);
        holder.recyclerView.setAdapter(new WidgetsHzAdapter(
                holder.itemView.getContext(), entry.mWidgets,
                mClickListener, mLongClickListener));
        holder.recyclerView.scrollToPosition(0);
    }

    @Override
    public void unbindViewHolder(WidgetsCatalogRowViewHolder holder) {
        holder.recyclerView.setAdapter(null);
    }
}
