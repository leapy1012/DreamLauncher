package com.android.launcher3.widget.picker;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.R;

/** Row with app header + horizontal widget carousel. */
public final class WidgetsCatalogRowViewHolder extends ViewHolder {

    public final BubbleTextView title;
    public final WidgetsHzRecyclerView recyclerView;

    public WidgetsCatalogRowViewHolder(View itemView) {
        super(itemView);
        title = itemView.findViewById(R.id.section);
        recyclerView = itemView.findViewById(R.id.widgets_scroll_container);
    }
}
