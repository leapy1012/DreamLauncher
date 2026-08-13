package com.android.launcher3.views;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.R;

/** Invisible 22dp divider used between OPPO widget catalog sections. */
public final class OplusWidgetDividerItemDecoration extends RecyclerView.ItemDecoration {
    private final int mHeight;

    public OplusWidgetDividerItemDecoration(Context context) {
        mHeight = context.getResources().getDimensionPixelSize(
                R.dimen.widget_popup_list_divider_height);
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
            @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        outRect.set(0, 0, 0,
                position > 0 && position < state.getItemCount() - 1 ? mHeight : 0);
    }
}
