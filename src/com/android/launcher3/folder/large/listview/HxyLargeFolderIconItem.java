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
import com.android.launcher3.R;
import java.util.ArrayList;
import java.util.List;

/**
 * One cell inside a large-folder preview grid.
 *
 * When {@code mCountOut} is set (Oppo overflow slot), this draws up to 4 mini-icons
 * in a 2×2 stack with ColorOS gap math — not a full app icon.
 */
public class HxyLargeFolderIconItem extends HxyBubbleTextView {
    private static final int MAX_OUT_COUNT = 4;
    private static final int SPAN_COUNT = 2;
    private String mClassName;
    private final int[] mCoordinateXY;
    private boolean mCountOut;
    private final List<Drawable> mDrawableList;
    private int mIconSize;
    private WorkspaceItemInfo mBoundData;
    private int mBoundPosition = -1;
    private List<WorkspaceItemInfo> mBoundList;
    private int mBoundCellSize = -1;

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
        setWillNotDraw(false);
    }

    public void release() {
        this.mDrawableList.clear();
        this.mBoundData = null;
        this.mBoundList = null;
    }

    public void dispatchDraw(@NonNull Canvas canvas) {
        // Preview cells paint icons in onDraw only — skip TextView children/text.
    }

    public void onDraw(Canvas canvas) {
        int save = canvas.save();
        canvas.clipRect(0, 0, getWidth(), getHeight());
        for (int i = 0; i < this.mDrawableList.size(); i++) {
            this.mDrawableList.get(i).draw(canvas);
        }
        canvas.restoreToCount(save);
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
        outBounds.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
    }

    public boolean isCountOut() {
        return this.mCountOut;
    }

    public void bindTo(WorkspaceItemInfo data, int position, boolean isCountOut,
            List<WorkspaceItemInfo> list) {
        this.mBoundData = data;
        this.mBoundPosition = position;
        this.mBoundList = list;
        this.mCountOut = isCountOut;
        this.mBoundCellSize = -1;
        applyBoundDrawables();
    }

    /** Re-apply after measure/layout so stack math uses the real cell size. */
    public void rebindIfNeeded() {
        if (mBoundPosition < 0) {
            return;
        }
        int cellSize = resolveCellSize();
        if (cellSize > 0 && cellSize != mBoundCellSize) {
            applyBoundDrawables();
        }
    }

    /** Force redraw after preview-mode switch (cell size may stay equal for nine↔highlight side tiles). */
    public void forceRebind() {
        mBoundCellSize = -1;
        rebindIfNeeded();
    }

    private int resolveCellSize() {
        if (getMeasuredWidth() > 0 && getMeasuredHeight() > 0) {
            return Math.min(getMeasuredWidth(), getMeasuredHeight());
        }
        int proxy = HxyLargeFolderProxy.getFolderIconSize();
        if (proxy > 0) {
            return proxy;
        }
        return getIconSize();
    }

    private void applyBoundDrawables() {
        WorkspaceItemInfo data = mBoundData;
        boolean isCountOut = mCountOut;
        String className = HxyLargeFolderUtils.getClassName(data);
        if (!isCountOut) {
            setTag(data);
        } else {
            setTag(null);
        }
        // Never show app / placeholder labels inside the preview plate.
        setText("");
        inits(data);
        this.mDrawableList.clear();
        this.mClassName = className;
        this.mIconSize = resolveCellSize();
        this.mBoundCellSize = this.mIconSize;
        if (isCountOut) {
            setIcon(null);
            addOutDrawables(mBoundPosition, mBoundList, this.mIconSize);
        } else if (data != null) {
            Drawable icon = getDrawable(getContext(), data, this.mIconSize);
            this.mDrawableList.add(icon);
            bindIcon();
        }
        invalidate();
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
        // Large-folder preview cells paint via {@link #onDraw}; skip TextView compounds.
    }

    /**
     * Oppo overflow cell: up to 4 mini-icons in a 2×2 that fills the preview cell.
     * Unused slots get {@code hxy_bf_holder_drawable} (ColorOS empty-slot).
     */
    private void addOutDrawables(int position, List<WorkspaceItemInfo> list, int cellSize) {
        if (list == null || list.size() <= position || cellSize <= 0) {
            return;
        }
        int gap = Math.max(1, HxyLargeFolderProxy.getFolderIconOutSpace(getContext()));
        int subIconSize = Math.max(1, (cellSize - gap) / 2);
        while (subIconSize * 2 + gap > cellSize && subIconSize > 1) {
            subIconSize--;
        }
        int used = subIconSize * 2 + gap;
        int origin = Math.max(0, (cellSize - used) / 2);
        int maxApps = Math.min(position + MAX_OUT_COUNT, list.size());
        int index = 0;
        for (int i = position; i < maxApps; i++, index++) {
            Drawable item = getDrawable(getContext(), list.get(i), subIconSize);
            placeOutDrawable(item, index, origin, subIconSize, gap);
            this.mDrawableList.add(item);
        }
        // Pad to a full 2×2 with empty holders (Oppo StackedDrawable + bf_holder).
        Drawable holder = getContext().getDrawable(R.drawable.hxy_bf_holder_drawable);
        while (index < MAX_OUT_COUNT && holder != null) {
            Drawable slot = holder.getConstantState() != null
                    ? holder.getConstantState().newDrawable().mutate()
                    : holder.mutate();
            placeOutDrawable(slot, index, origin, subIconSize, gap);
            this.mDrawableList.add(slot);
            index++;
        }
    }

    private void placeOutDrawable(Drawable item, int index, int origin, int subIconSize, int gap) {
        int col = getColumnIndex(index);
        int row = getRowIndex(index);
        int left = origin + col * (subIconSize + gap);
        int top = origin + row * (subIconSize + gap);
        item.setBounds(left, top, left + subIconSize, top + subIconSize);
    }

    private int getRowIndex(int index) {
        return index / SPAN_COUNT;
    }

    private int getColumnIndex(int index) {
        return index % SPAN_COUNT;
    }

    private Drawable getDrawable(Context context, WorkspaceItemInfo item, int iconSize) {
        Drawable folderDrawable = item.newIcon(context, BitmapInfo.FLAG_THEMED);
        folderDrawable.setBounds(0, 0, iconSize, iconSize);
        return folderDrawable;
    }
}
