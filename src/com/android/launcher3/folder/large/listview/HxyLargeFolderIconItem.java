package com.android.launcher3.folder.large.listview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

import com.android.launcher3.icons.BitmapInfo;
import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.android.launcher3.big.HxyBubbleTextView;
import com.android.launcher3.folder.large.HxyLargeFolderProxy;
import com.android.launcher3.folder.large.HxyLargeFolderUtils;
import java.util.ArrayList;
import java.util.List;

public class HxyLargeFolderIconItem extends HxyBubbleTextView {
    private static final int MAX_OUT_COUNT = 4;
    private static final int SPAN_COUNT = 2;
    private String mClassName;
    private final int[] mCoordinateXY;
    private boolean mCountOut;
    private final List<Drawable> mDrawableList;
    private int mIconSize;

    public HxyLargeFolderIconItem(Context context) {
        this(context, (AttributeSet) null);
    }

    public HxyLargeFolderIconItem(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HxyLargeFolderIconItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mCoordinateXY = new int[2];
        this.mClassName = null;
        this.mDrawableList = new ArrayList<>();
        this.mCountOut = false;
        this.mIconSize = -1;
    }

    public void release() {
        this.mDrawableList.clear();
    }
    public void dispatchDraw(@NonNull Canvas canvas) {
        super.dispatchDraw(canvas);
    }

    public void onDraw(Canvas canvas) {
        for (int i = 0; i < this.mDrawableList.size(); i++) {
            this.mDrawableList.get(i).draw(canvas);
        }
    }

    public void setCoordinateXY(int x, int y) {
        int[] iArr = this.mCoordinateXY;
        iArr[0] = x;
        iArr[1] = y;
    }

    public int getCoordinateX() {
        return this.mCoordinateXY[0];
    }

    public int getCoordinateY() {
        return this.mCoordinateXY[1];
    }

    public void getIconBounds(Rect outBounds) {
        outBounds.set(0, 0, getMeasuredWidth() + 0, getMeasuredHeight() + 0);
    }

    public boolean isCountOut() {
        return this.mCountOut;
    }

    private boolean isChangeData(WorkspaceItemInfo data, boolean isCountOut, String className) {
        return true;
    }

    public void bindTo(WorkspaceItemInfo data, int position, boolean isCountOut, List<WorkspaceItemInfo> list) {
        this.mCountOut = isCountOut;
        String className = HxyLargeFolderUtils.getClassName(data);
        if (isChangeData(data, isCountOut, className)) {
            if (!isCountOut) {
                setTag(data);
            }
            inits(data);
            this.mDrawableList.clear();
            this.mClassName = className;
            this.mIconSize = HxyLargeFolderProxy.getFolderIconSize();
            int iconOutSize = HxyLargeFolderProxy.getFolderIconOutSize();
            int iconOutSpace = HxyLargeFolderProxy.getFolderIconOutSpace(getContext());
            int size = isCountOut ? iconOutSize : this.mIconSize;
            if (isCountOut) {
                addOutDrawables(position, list, iconOutSize, iconOutSpace);
            } else {
                this.mDrawableList.add(getDrawable(getContext(), data, size));
            }
            bindIcon();
            invalidate();
        }
    }

    private void bindIcon() {
        if (!this.mDrawableList.isEmpty()) {
            setIconDrawable(this.mDrawableList.get(0));
        }
    }

    public void setIconVisible(boolean visible) {
        setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    public void applyCompoundDrawables(Drawable icon) {
    }

    private void addOutDrawables(int position, List<WorkspaceItemInfo> list, int iconOutSize, int iconOutSpace) {
        if (list != null && list.size() > position) {
            int maxSize = Math.min(position + 4, list.size());
            int i = position;
            int index = 0;
            while (i < maxSize) {
                Drawable item = getDrawable(getContext(), list.get(i), iconOutSize);
                int left = getOutLeft(index, iconOutSize, iconOutSpace);
                int top = getOutTop(index, iconOutSize, iconOutSpace);
                item.setBounds(left, top, left + iconOutSize, top + iconOutSize);
                this.mDrawableList.add(item);
                i++;
                index++;
            }
        }
    }

    private int getOutLeft(int index, int iconOutSize, int iconOutSpace) {
        return getColumnIndex(index) * (iconOutSize + iconOutSpace);
    }

    private int getOutTop(int index, int iconOutSize, int iconOutSpace) {
        return getRowIndex(index) * (iconOutSize + iconOutSpace);
    }

    private int getRowIndex(int index) {
        return index / 2;
    }

    private int getColumnIndex(int index) {
        return index % 2;
    }

    private Drawable getDrawable(Context context, WorkspaceItemInfo item, int iconSize) {
        Drawable folderDrawable = item.newIcon(context, BitmapInfo.FLAG_THEMED);
        folderDrawable.setBounds(0, 0, iconSize, iconSize);
        return folderDrawable;
    }
}
