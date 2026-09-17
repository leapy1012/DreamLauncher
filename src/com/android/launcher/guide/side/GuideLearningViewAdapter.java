package com.android.launcher.guide.side;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.R;

/** Adapter for the gesture-learning cards. */
final class GuideLearningViewAdapter
        extends RecyclerView.Adapter<GuideLearningViewAdapter.ViewHolder> {

    interface OnRecycleItemClickListener {
        void onClick(int position);
    }

    private static final int[] IMAGE_IDS = {
            R.drawable.gestures_learning_back,
            R.drawable.gestures_learning_home,
            R.drawable.gestures_learning_recent,
            R.drawable.gestures_learning_switchapp
    };

    private static final int[] TITLE_IDS = {
            R.string.side_gestures_back_msg,
            R.string.side_gestures_home_msg,
            R.string.side_gestures_recents_msg,
            R.string.side_gestures_switch_app_msg
    };

    private final LayoutInflater mInflater;
    private OnRecycleItemClickListener mOnItemClickListener;

    GuideLearningViewAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
    }

    void setOnItemClickListener(OnRecycleItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(
                mInflater.inflate(R.layout.guide_page_learning_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.mImage.setImageResource(IMAGE_IDS[position]);
        holder.mTitle.setText(TITLE_IDS[position]);
        holder.mImage.setOnClickListener(view -> {
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onClick(holder.getBindingAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return IMAGE_IDS.length;
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView mImage;
        final TextView mTitle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            mImage = itemView.findViewById(R.id.image);
            mTitle = itemView.findViewById(R.id.title);
        }
    }
}
