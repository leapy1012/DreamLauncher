package com.android.launcher3.big;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.R;

/** Single-row host so Icon Custom scrolls through {@link COUIRecyclerView} like Theme. */
public class IconCustomPageAdapter extends RecyclerView.Adapter<IconCustomPageAdapter.PageHolder> {

    public interface OnPageInflatedListener {
        void onPageInflated(@NonNull View pageRoot);
    }

    private OnPageInflatedListener mListener;

    public void setOnPageInflatedListener(OnPageInflatedListener listener) {
        mListener = listener;
    }

    @NonNull
    @Override
    public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View page = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_icon_size_setting_content, parent, false);
        if (mListener != null) {
            mListener.onPageInflated(page);
        }
        return new PageHolder(page);
    }

    @Override
    public void onBindViewHolder(@NonNull PageHolder holder, int position) {
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    static class PageHolder extends RecyclerView.ViewHolder {
        PageHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
