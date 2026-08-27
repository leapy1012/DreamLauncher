package com.android.launcher3.big;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.R;
import com.android.launcher3.customer.table.CustomTable;
import com.android.launcher3.customer.tools.ImageUtils;

import java.util.List;

public class IconSizeSettingAdapter extends RecyclerView.Adapter<IconSizeSettingAdapter.MyViewHolder> {
    private static final int[] SHAPE_MASKS = {
            R.drawable.icon_mask_default,
            R.drawable.icon_mask_neighbourhood,
            R.drawable.icon_mask_droplet,
            R.drawable.icon_mask_circle,
    };
    private static final Object PAYLOAD_SHAPE = new Object();
    private static final Object PAYLOAD_PROGRESS = new Object();

    private final Context mContext;
    private final List<CustomTable> mDatas;
    private RecyclerView mRecyclerView;
    private Float progress = Float.valueOf(-1.0f);
    private int mShapeIndex;
    private int translationYD;
    private int mPreviewIconSizePx;

    public void setProgress(Float progress2) {
        this.progress = progress2;
        refreshVisibleItems(PAYLOAD_PROGRESS);
    }

    public void setShapeIndex(int shapeIndex) {
        if (shapeIndex < 0 || shapeIndex >= SHAPE_MASKS.length || shapeIndex == mShapeIndex) {
            return;
        }
        mShapeIndex = shapeIndex;
        refreshVisibleItems(PAYLOAD_SHAPE);
    }

    private void refreshVisibleItems(Object payload) {
        if (mRecyclerView == null) {
            return;
        }
        boolean reshape = payload == PAYLOAD_SHAPE;
        for (int i = 0; i < getItemCount(); i++) {
            RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(i);
            if (!(holder instanceof MyViewHolder) || i >= mDatas.size()) {
                continue;
            }
            MyViewHolder vh = (MyViewHolder) holder;
            if (reshape) {
                updateIconDrawable(vh, mDatas.get(i));
            } else {
                applyProgress(vh);
            }
        }
        if (reshape) {
            mRecyclerView.invalidate();
        }
    }

    private void applyProgress(MyViewHolder vh) {
        if (this.progress.floatValue() == -1.0f) {
            return;
        }
        View icon = vh.icon;
        View title = vh.title;
        icon.setPivotY(0.0f);
        icon.setPivotX((float) this.translationYD);
        float scale = (float) ((((double) this.progress.floatValue()) * 0.5d) + 1.0d);
        icon.setScaleX(scale);
        icon.setScaleY(scale);
        title.setTranslationY(((float) this.translationYD) * this.progress.floatValue());
    }

    private void updateIconDrawable(MyViewHolder vh, CustomTable item) {
        vh.icon.setImageDrawable(applyShapeMask(item));
    }

    private Bitmap toSoftwareBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        if (bitmap.getConfig() == Bitmap.Config.HARDWARE) {
            return bitmap.copy(Bitmap.Config.ARGB_8888, true);
        }
        if (!bitmap.isMutable() && bitmap.getConfig() != Bitmap.Config.ARGB_8888) {
            return bitmap.copy(Bitmap.Config.ARGB_8888, true);
        }
        return bitmap;
    }

    private Bitmap getSourceBitmap(CustomTable item) {
        Bitmap source = toSoftwareBitmap(item.iconBitmap);
        if (source != null && source.getWidth() > 1 && source.getHeight() > 1) {
            return source;
        }
        Drawable drawable = item.icon;
        if (drawable != null) {
            return ImageUtils.drawableToBitmap(drawable);
        }
        return null;
    }

    private Bitmap createMaskBitmap(int size) {
        Drawable maskDrawable = ContextCompat.getDrawable(mContext, SHAPE_MASKS[mShapeIndex]);
        if (maskDrawable == null) {
            return null;
        }
        maskDrawable = maskDrawable.mutate();
        Bitmap mask = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mask);
        maskDrawable.setBounds(0, 0, size, size);
        maskDrawable.draw(canvas);
        return mask;
    }

    private Drawable applyShapeMask(CustomTable item) {
        Bitmap source = getSourceBitmap(item);
        if (source == null) {
            return item.icon;
        }
        int size = Math.max(1, mPreviewIconSizePx);
        Bitmap scaledSource = source;
        if (source.getWidth() != size || source.getHeight() != size) {
            scaledSource = Bitmap.createScaledBitmap(source, size, size, true);
        }
        scaledSource = toSoftwareBitmap(scaledSource);
        Bitmap maskBitmap = createMaskBitmap(size);
        if (maskBitmap == null) {
            return new BitmapDrawable(mContext.getResources(), scaledSource);
        }

        Bitmap result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        int layer = canvas.saveLayer(0, 0, size, size, null);
        canvas.drawBitmap(scaledSource, 0, 0, null);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        canvas.drawBitmap(maskBitmap, 0, 0, paint);
        paint.setXfermode(null);
        canvas.restoreToCount(layer);
        BitmapDrawable drawable = new BitmapDrawable(mContext.getResources(), result);
        drawable.setFilterBitmap(true);
        return drawable;
    }

    public IconSizeSettingAdapter(Context context, List<CustomTable> list2) {
        this.mContext = context;
        this.mDatas = list2;
        this.mPreviewIconSizePx =
                context.getResources().getDimensionPixelSize(R.dimen.coloros_icon_preview_size);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        if (mRecyclerView == recyclerView) {
            mRecyclerView = null;
        }
    }

    public MyViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(this.mContext).inflate(R.layout.item_icon_size_setting, viewGroup, false);
        ImageView icon = view.findViewById(R.id.imageview);
        TextView title = view.findViewById(R.id.textview);
        view.setOnClickListener(v -> { });
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) icon.getLayoutParams();
        RelativeLayout.LayoutParams titleParams = (RelativeLayout.LayoutParams) title.getLayoutParams();
        icon.setLayoutParams(params);
        titleParams.topMargin = params.width + 10;
        title.setLayoutParams(titleParams);
        this.translationYD = (int) (((float) params.width) * 0.5f);
        this.mPreviewIconSizePx = params.width;
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyViewHolder viewHolder, int i, List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(viewHolder, i);
            return;
        }
        if (this.mDatas.isEmpty() || i >= this.mDatas.size()) {
            return;
        }
        CustomTable item = this.mDatas.get(i);
        for (Object payload : payloads) {
            if (payload == PAYLOAD_SHAPE) {
                updateIconDrawable(viewHolder, item);
            } else if (payload == PAYLOAD_PROGRESS) {
                applyProgress(viewHolder);
            }
        }
    }

    public void onBindViewHolder(MyViewHolder viewHolder, int i) {
        if (this.mDatas.isEmpty() || i >= this.mDatas.size()) {
            return;
        }
        CustomTable item = this.mDatas.get(i);
        updateIconDrawable(viewHolder, item);
        CharSequence title = item.title;
        if (!title.equals(viewHolder.title.getText())) {
            viewHolder.title.setText(title);
        }
        applyProgress(viewHolder);
    }

    public int getItemCount() {
        return this.mDatas.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        public ImageView icon;
        public TextView title;

        public MyViewHolder(View itemView) {
            super(itemView);
            this.icon = itemView.findViewById(R.id.imageview);
            this.title = itemView.findViewById(R.id.textview);
        }
    }
}
