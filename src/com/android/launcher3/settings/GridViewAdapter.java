package com.android.launcher3.settings;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.R;
import com.android.launcher3.settings.ThemeActivity.ThemeData;

import java.util.ArrayList;

public class GridViewAdapter extends RecyclerView.Adapter<GridViewAdapter.ThemeViewHolder> {
    private final Context mContext;
    private final ArrayList<ThemeData> mItemList;
    private final LayoutInflater mInflater;
    private OnThemeClickListener mClickListener;

    public interface OnThemeClickListener {
        void onThemeClick(int position);
    }

    public GridViewAdapter(Context context, ArrayList<ThemeData> items) {
        mContext = context;
        mItemList = items;
        mInflater = LayoutInflater.from(context);
    }

    public void setOnThemeClickListener(OnThemeClickListener listener) {
        mClickListener = listener;
    }

    @Override
    public int getItemCount() {
        return mItemList != null ? mItemList.size() : 0;
    }

    @NonNull
    @Override
    public ThemeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.theme_item_list, parent, false);
        return new ThemeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ThemeViewHolder holder, int position) {
        ThemeData item = mItemList.get(position);
        holder.itemView.setSelected(item.selected);
        holder.preview.setImageDrawable(item.preview);
        holder.title.setText(item.themeTitle);
        holder.itemView.setOnClickListener(v -> {
            if (mClickListener != null) {
                mClickListener.onThemeClick(holder.getBindingAdapterPosition());
            }
        });
    }

    static class ThemeViewHolder extends RecyclerView.ViewHolder {
        final ImageView preview;
        final TextView title;

        ThemeViewHolder(@NonNull View itemView) {
            super(itemView);
            preview = itemView.findViewById(R.id.theme_preview);
            title = itemView.findViewById(R.id.theme_title);
        }
    }
}
