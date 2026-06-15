package com.android.launcher3.big;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.RemoteViews;

import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.model.data.ItemInfoWithIcon;

@RemoteViews.RemoteView
public class HxyBubbleTextView extends HxyCheckBubbleTextView {
    public boolean isDisabled;
    Context mContext;

    public HxyBubbleTextView(Context context) {
        super(context);
        this.isDisabled = false;
    }

    public HxyBubbleTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HxyBubbleTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.isDisabled = false;
        this.mContext = context;
    }

    public void onTick() {
    }

    public void refreshDrawableState() {
        if (!isDrawbleView()) {
            super.refreshDrawableState();
        }
    }

    public void clear() {
        System.gc();
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), drawable.getOpacity() != PixelFormat.UNKNOWN ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private boolean isValidPackageComponent(PackageManager pm, ComponentName cn) {
        return true;
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    public void setItemInfo(ItemInfoWithIcon itemInfo) {
        super.setItemInfo(itemInfo);
        if (getTag() != null) {
            ItemInfo info = (ItemInfo) getTag();
            init(info);
            this.isDisabled = info.isDisabled();
        }
    }

    public void init(ItemInfo info) {
        if (info != null) {
            info.getIntent();
        }
    }

    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    public void applyIconAndLabel(ItemInfoWithIcon info) {
        super.applyIconAndLabel(info);
    }

    public boolean isDrawbleView() {
        return false;
    }

    public void drawDotIfNecessary(Canvas canvas) {
        super.drawDotIfNecessary(canvas);
    }

    public int[] onCreateDrawableState(int extraSpace) {
        int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (this.mStayPressed) {
            mergeDrawableStates(drawableState, STATE_PRESSED);
        }
        return drawableState;
    }

    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
    }
}