package com.android.launcher.widget;

import android.content.Context;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.R;
import com.android.launcher3.model.WidgetItem;
import com.android.launcher3.widget.WidgetCell;

import java.util.List;

/** Dedicated horizontal widget adapter ported from OPPO's OplusWidgetsHzAdapter. */
public final class OplusWidgetsHzAdapter
        extends RecyclerView.Adapter<OplusWidgetsHzAdapter.OplusWidgetsHzViewHolder> {
    private final Context mContext;
    private final List<WidgetItem> mWidgetList;
    private final View.OnClickListener mOnClickListener;
    private final View.OnLongClickListener mOnLongClickListener;

    public OplusWidgetsHzAdapter(Context context, List<WidgetItem> widgets,
            View.OnClickListener clickListener, View.OnLongClickListener longClickListener) {
        mContext = context;
        mWidgetList = widgets;
        mOnClickListener = clickListener;
        mOnLongClickListener = longClickListener;
    }

    @NonNull
    @Override
    public OplusWidgetsHzViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new OplusWidgetsHzViewHolder(LayoutInflater.from(mContext).inflate(
                R.layout.widget_cell_oplus, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull OplusWidgetsHzViewHolder holder, int position) {
        WidgetCell cell = holder.mWidgetCell;
        cell.clear();
        cell.applyFromCellItem(mWidgetList.get(position));
        cell.setVisibility(View.VISIBLE);
        cell.setOnClickListener(mOnClickListener);
        cell.setOnLongClickListener(mOnLongClickListener);
    }

    @Override
    public int getItemCount() {
        return mWidgetList.size();
    }

    @Override
    public void onViewRecycled(@NonNull OplusWidgetsHzViewHolder holder) {
        holder.mWidgetCell.clear();
        super.onViewRecycled(holder);
    }

    public static final class OplusWidgetsHzViewHolder extends RecyclerView.ViewHolder {
        final WidgetCell mWidgetCell;

        OplusWidgetsHzViewHolder(View itemView) {
            super(itemView);
            mWidgetCell = itemView.findViewById(R.id.widget_cell_id);
        }
    }

    /** Exact decoded OPPO trailing space between horizontal cells. */
    public static final class WidgetsHzItemDecoration extends RecyclerView.ItemDecoration {
        private final int mSpace;

        public WidgetsHzItemDecoration(int space) {
            mSpace = space;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            outRect.set(0, 0, mSpace, 0);
        }
    }
}
