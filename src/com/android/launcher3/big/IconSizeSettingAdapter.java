package com.android.launcher3.big;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.android.launcher3.R;
import com.android.launcher3.customer.table.CustomTable;
import java.util.ArrayList;
import java.util.List;

public class IconSizeSettingAdapter extends RecyclerView.Adapter<IconSizeSettingAdapter.MyViewHolder> {
    Boolean firstTag = false;
    List<ViewGroup> list = new ArrayList();
    private Context mContext;
    private List<CustomTable> mDatas;
    private ViewGroup mViewGroup;
    private Float progress = Float.valueOf(-1.0f);
    private int translationYD;

    public void setProgress(Float progress2) {
        this.progress = progress2;
        if (this.mViewGroup != null) {
            if (this.list.isEmpty()) {
                for (int i = 0; i < this.mViewGroup.getChildCount(); i++) {
                    this.list.add((ViewGroup) this.mViewGroup.getChildAt(i));
                }
            }
            for (ViewGroup layout : this.list) {
                setViewProgress(layout);
            }
        }
    }

    public void setViewProgress(ViewGroup layout) {
        View icon = layout.findViewById(R.id.imageview);
        View title = layout.findViewById(R.id.textview);
        if (this.progress.floatValue() != -1.0f) {
            icon.setPivotY(0.0f);
            icon.setPivotX((float) this.translationYD);
            icon.setScaleX((float) ((((double) this.progress.floatValue()) * 0.5d) + 1.0d));
            icon.setScaleY((float) ((((double) this.progress.floatValue()) * 0.5d) + 1.0d));
            title.setTranslationY(((float) this.translationYD) * this.progress.floatValue());
            icon.setLayerType(View.LAYER_TYPE_HARDWARE, (Paint) null);
            title.setLayerType(View.LAYER_TYPE_HARDWARE, (Paint) null);
        }
    }

    public IconSizeSettingAdapter(Context context, List<CustomTable> list2) {
        this.mContext = context;
        this.mDatas = list2;
    }

    public MyViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(this.mContext).inflate(R.layout.item_icon_size_setting, viewGroup, false);
        this.mViewGroup = viewGroup;
        ImageView icon = (ImageView) view.findViewById(R.id.imageview);
        TextView title = (TextView) view.findViewById(R.id.textview);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lambda$onCreateViewHolder$0(view);
            }
        });
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) icon.getLayoutParams();
        RelativeLayout.LayoutParams titleParams = (RelativeLayout.LayoutParams) title.getLayoutParams();
        icon.setLayoutParams(params);
        titleParams.topMargin = params.width + mContext.getResources()
                .getDimensionPixelSize(R.dimen.coloros_icon_label_gap);
        title.setLayoutParams(titleParams);
        this.translationYD = (int) (((float) params.width) * 0.5f);
        return new MyViewHolder(view);
    }

    static /* synthetic */ void lambda$onCreateViewHolder$0(View v) {
    }

    public void onBindViewHolder(MyViewHolder viewHolder, int i) {
        if (this.mDatas.size() > 0) {
            viewHolder.setIsRecyclable(false);
            viewHolder.icon.setImageDrawable(this.mDatas.get(i).icon);
            viewHolder.title.setText(this.mDatas.get(i).title);
            if (this.firstTag.booleanValue()) {
                viewHolder.icon.setPivotY(0.0f);
                viewHolder.icon.setPivotX((float) this.translationYD);
                viewHolder.icon.setScaleX((float) ((((double) this.progress.floatValue()) * 0.5d) + 1.0d));
                viewHolder.icon.setScaleY((float) ((((double) this.progress.floatValue()) * 0.5d) + 1.0d));
                viewHolder.title.setTranslationY(((float) this.translationYD) * this.progress.floatValue());
                viewHolder.icon.setLayerType(View.LAYER_TYPE_HARDWARE, (Paint) null);
                viewHolder.title.setLayerType(View.LAYER_TYPE_HARDWARE, (Paint) null);
            }
            if (i == this.mDatas.size() - 1) {
                this.firstTag = false;
            }
        }
    }

    public int getItemCount() {
        return this.mDatas.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        public ImageView icon;
        public TextView title;

        public MyViewHolder(View itemView) {
            super(itemView);
            this.icon = (ImageView) itemView.findViewById(R.id.imageview);
            this.title = (TextView) itemView.findViewById(R.id.textview);
        }
    }
}
