package com.android.launcher3.widget.picker;

import android.content.Context;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.R;
import com.android.launcher3.model.WidgetItem;
import com.android.launcher3.widget.WidgetCell;

import java.util.List;

/** Horizontal adapter of catalog widget cells for one app. */
public class WidgetsHzAdapter extends RecyclerView.Adapter<WidgetsHzAdapter.Holder> {

    private final LayoutInflater mLayoutInflater;
    private final List<WidgetItem> mWidgets;
    private final OnClickListener mClickListener;
    private final OnLongClickListener mLongClickListener;

    public WidgetsHzAdapter(Context context, List<WidgetItem> widgets,
            OnClickListener clickListener, OnLongClickListener longClickListener) {
        mLayoutInflater = LayoutInflater.from(context);
        mWidgets = widgets;
        mClickListener = clickListener;
        mLongClickListener = longClickListener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        WidgetCell cell = (WidgetCell) mLayoutInflater.inflate(
                R.layout.widget_cell_catalog, parent, false);
        cell.enableCatalogStyle();
        cell.setAnimatePreview(false);
        return new Holder(cell);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        WidgetCell cell = holder.mCell;
        cell.clear();
        cell.enableCatalogStyle();
        cell.applyFromCellItem(mWidgets.get(position));
        cell.setVisibility(View.VISIBLE);
        cell.setOnClickListener(mClickListener);
        cell.setOnLongClickListener(mLongClickListener);
        View preview = cell.findViewById(R.id.widget_preview_container);
        if (preview != null) {
            preview.setOnClickListener(mClickListener);
            preview.setOnLongClickListener(mLongClickListener);
        }
    }

    @Override
    public int getItemCount() {
        return mWidgets.size();
    }

    @Override
    public void onViewRecycled(@NonNull Holder holder) {
        holder.mCell.clear();
        holder.mCell.setOnClickListener(null);
        holder.mCell.setOnLongClickListener(null);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final WidgetCell mCell;

        Holder(WidgetCell cell) {
            super(cell);
            mCell = cell;
        }
    }

    /** Trailing gap between catalog cards. */
    public static class SpacingDecoration extends RecyclerView.ItemDecoration {
        private final int mSpace;

        public SpacingDecoration(int space) {
            mSpace = space;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            outRect.set(0, 0, mSpace, 0);
        }
    }
}
