package com.android.launcher3.folder.large.listview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewParent;

import androidx.annotation.NonNull;

import com.android.launcher3.Utilities;
import com.android.launcher3.dot.DotDrawUtils;
import com.android.launcher3.dot.DotInfo;
import com.android.launcher3.dot.NumberDotRenderer;
import com.android.launcher3.graphics.IconShape;
import com.android.launcher3.icons.BitmapInfo;
import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.android.launcher3.big.HxyBubbleTextView;
import com.android.launcher3.folder.large.LargeFolderProxy;
import com.android.launcher3.folder.large.LargeFolderUtils;
import com.android.launcher3.util.Themes;
import com.android.launcher3.views.ActivityContext;
import com.android.launcher3.R;
import java.util.ArrayList;
import java.util.List;

/**
 * One cell inside a large-folder preview grid.
 *
 * When {@code mCountOut} is set (Oppo overflow slot), this draws up to 4 mini-icons
 * in a 2×2 stack with ColorOS gap math — not a full app icon.
 * Notification badges are drawn per cell (ColorOS BigFolderPreviewItemManager), not on
 * the folder plate.
 */
public class LargeFolderIconItem extends HxyBubbleTextView {
    private static final int MAX_OUT_COUNT = 4;
    private static final int SPAN_COUNT = 2;
    /** ColorOS {@code BigFolderNumBadgeRender.DOT_SCALE} — outward nudge into the gutter. */
    private static final float STACK_DOT_CORNER_OFFSET_FRAC = 0.05555f;
    /** ColorOS {@code red_dot_size} (14dp) × {@code dealDotSizePercent(false)} (0.8). */
    private static final float STACK_DOT_DIAMETER_DP = 14f * 0.8f;
    private String mClassName;
    private final int[] mCoordinateXY;
    private boolean mCountOut;
    private final List<Drawable> mDrawableList;
    private int mIconSize;
    private WorkspaceItemInfo mBoundData;
    private int mBoundPosition = -1;
    private List<WorkspaceItemInfo> mBoundList;
    private int mBoundCellSize = -1;
    /** Dedicated params — parent {@code HxyCheckBubbleTextView} shadows BTV's NumberDot params. */
    private final NumberDotRenderer.DrawParams mPreviewDotParams =
            new NumberDotRenderer.DrawParams(0);
    private final Paint mStackDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public LargeFolderIconItem(Context context) {
        this(context, (AttributeSet) null);
    }

    public LargeFolderIconItem(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LargeFolderIconItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mCoordinateXY = new int[2];
        this.mClassName = null;
        this.mDrawableList = new ArrayList<>();
        this.mCountOut = false;
        this.mIconSize = -1;
        setWillNotDraw(false);
        // Allow number badges to slightly overhang into preview gutters.
        setClipToOutline(false);
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
        // ColorOS: per-preview-cell notification badge (plate badge is suppressed).
        drawPreviewDotIfNecessary(canvas);
    }

    /**
     * Mirrors ColorOS {@code BigFolderPreviewItemManager.drawDotIfNecessary}:
     * <ul>
     *   <li>Normal cells → that app's {@link DotInfo} (number or red dot)</li>
     *   <li>Overflow stack → {@code StackedDotInfo}: aggregate remaining apps, but always
     *       draw a <b>red dot only</b> (never the count)</li>
     * </ul>
     */
    private void drawPreviewDotIfNecessary(Canvas canvas) {
        ActivityContext activity = ActivityContext.lookupContext(getContext());
        if (activity == null) {
            return;
        }
        NumberDotRenderer renderer = activity.getDeviceProfile().mDotRendererWorkSpace;
        if (renderer == null) {
            return;
        }

        mPreviewDotParams.scale = 1.0f;
        mPreviewDotParams.dotColor = Themes.getAttrColor(getContext(), R.attr.notificationDotColor);

        if (mCountOut) {
            // ColorOS StackedDotInfo.canShowDot() is true for number OR dot → red-dot branch.
            if (!stackedRangeHasBadge(activity)) {
                return;
            }
            mPreviewDotParams.unreadNum = 0;
            drawStackedCellRedDot(canvas);
            return;
        }

        if (mBoundData == null) {
            return;
        }
        DotInfo dotInfo = activity.getDotInfoForItem(mBoundData);
        if (dotInfo == null) {
            return;
        }
        int unreadNum = dotInfo.getNotificationCount();
        if (renderer.mShowNumber && unreadNum <= 0) {
            return;
        }
        mPreviewDotParams.unreadNum = unreadNum;
        getIconBounds(mPreviewDotParams.iconBounds);
        int iconSize = mIconSize > 0 ? mIconSize : Math.max(getWidth(), 1);
        if (renderer.mShowNumber) {
            // Match workspace/folder plate badges — corner anchor, no glyph shrink.
            DotDrawUtils.draw(canvas, getContext(),
                    new DotDrawUtils.DotNumParams(renderer, iconSize, mPreviewDotParams));
        } else {
            Utilities.scaleRectAboutCenter(mPreviewDotParams.iconBounds,
                    IconShape.getNormalizationScale());
            renderer.draw(canvas, mPreviewDotParams);
        }
    }

    /**
     * ColorOS {@code getBFParamsDotInfos}: any badge from the first stacked index through
     * the end of folder {@code contents} (apps past the 4 minis / later pages).
     */
    private boolean stackedRangeHasBadge(ActivityContext activity) {
        if (mBoundList == null || mBoundPosition < 0) {
            return false;
        }
        for (int i = mBoundList.size() - 1; i >= mBoundPosition; i--) {
            if (activity.getDotInfoForItem(mBoundList.get(i)) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * ColorOS overflow slot red dot ({@code BigFolderNumBadgeRender.draw}), not workspace
     * {@link NumberDotRenderer}.
     * <p>
     * ColorOS expands the slot by {@code mPreviewSubIconGap} (half the inter-cell gutter),
     * then pins the dot to the top-right of the slot with a small outward offset so it sits
     * in the gutter — not on a mini-icon. Our list gap is the full gutter, so half of
     * {@link PageLinearLayout#getHorizontalSpace()} matches {@code mPreviewSubIconGap}.
     */
    private void drawStackedCellRedDot(Canvas canvas) {
        final int w = Math.max(getWidth(), 1);
        final int h = Math.max(getHeight(), 1);

        float gapX = 0f;
        float gapY = 0f;
        ViewParent parent = getParent();
        if (parent instanceof PageLinearLayout) {
            PageLinearLayout list = (PageLinearLayout) parent;
            // ColorOS: mPreviewSubIconGap = fullGutter / (columns*2) * columns… = half gutter.
            gapX = list.getHorizontalSpace() * 0.5f;
            gapY = list.getVerticalSpace() * 0.5f;
        }

        final float density = getResources().getDisplayMetrics().density;
        final float dotSize = Math.max(1f, STACK_DOT_DIAMETER_DP * density);
        final float endOffset = w * STACK_DOT_CORNER_OFFSET_FRAC;
        final float topOffset = h * STACK_DOT_CORNER_OFFSET_FRAC;

        // Expanded coordinate space: translate(-gap) then iconBounds=(gap,gap,gap+w,gap+h).
        // Right-align (LTR): same as BigFolderNumBadgeRender.draw.
        final float iconRight = gapX + w;
        final float iconTop = gapY;
        final float containerRight = gapX * 2f + w;
        float left = Math.min(containerRight - dotSize, (iconRight - dotSize) + endOffset);
        float top = Math.max(0f, iconTop - topOffset);
        float centerX = left + dotSize * 0.5f;
        float centerY = top + dotSize * 0.5f;

        // Convert expanded-space center back to cell-local coords.
        centerX -= gapX;
        centerY -= gapY;

        mStackDotPaint.setColor(mPreviewDotParams.dotColor);
        mStackDotPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(centerX, centerY, dotSize * 0.5f, mStackDotPaint);
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
        int proxy = LargeFolderProxy.getFolderIconSize();
        if (proxy > 0) {
            return proxy;
        }
        return getIconSize();
    }

    private void applyBoundDrawables() {
        WorkspaceItemInfo data = mBoundData;
        boolean isCountOut = mCountOut;
        String className = LargeFolderUtils.getClassName(data);
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
            setCompoundDrawables(null, null, null, null);
            addOutDrawables(mBoundPosition, mBoundList, this.mIconSize);
        } else if (data != null) {
            // Draw only via mDrawableList in onDraw. Never setIcon/setIconDrawable —
            // sharing the same FastBitmapDrawable with BubbleTextView.mIcon caused a
            // second up-left ghost in large-folder preview cells.
            setIcon(null);
            setCompoundDrawables(null, null, null, null);
            Drawable icon = getDrawable(getContext(), data, this.mIconSize);
            this.mDrawableList.add(icon);
        }
        invalidate();
    }

    public void setIconVisible(boolean visible) {
        setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    public void applyCompoundDrawables(Drawable icon) {
        // Large-folder preview cells paint via {@link #onDraw}; skip TextView compounds.
    }

    /** Max mini-icons drawn in the overflow 2×2 cell (ColorOS StackedDrawable). */
    public static int getMaxOutCount() {
        return MAX_OUT_COUNT;
    }

    /**
     * Local bounds of mini-icon {@code stackIndex} (0–3) inside a square overflow cell,
     * matching {@link #addOutDrawables} layout. Returns false if inputs are invalid.
     */
    public static boolean computeStackedSubBounds(Context context, int cellSize,
            int stackIndex, Rect out) {
        if (context == null || out == null || cellSize <= 0
                || stackIndex < 0 || stackIndex >= MAX_OUT_COUNT) {
            return false;
        }
        int gap = Math.max(1, LargeFolderProxy.getFolderIconOutSpace(context));
        int subIconSize = Math.max(1, (cellSize - gap) / 2);
        while (subIconSize * 2 + gap > cellSize && subIconSize > 1) {
            subIconSize--;
        }
        int used = subIconSize * 2 + gap;
        int origin = Math.max(0, (cellSize - used) / 2);
        int col = stackIndex % SPAN_COUNT;
        int row = stackIndex / SPAN_COUNT;
        int left = origin + col * (subIconSize + gap);
        int top = origin + row * (subIconSize + gap);
        out.set(left, top, left + subIconSize, top + subIconSize);
        return true;
    }

    /**
     * Oppo overflow cell: up to 4 mini-icons in a 2×2 that fills the preview cell.
     * Unused slots get {@code hxy_bf_holder_drawable} (ColorOS empty-slot).
     */
    private void addOutDrawables(int position, List<WorkspaceItemInfo> list, int cellSize) {
        if (list == null || list.size() <= position || cellSize <= 0) {
            return;
        }
        Rect sub = new Rect();
        int maxApps = Math.min(position + MAX_OUT_COUNT, list.size());
        int index = 0;
        for (int i = position; i < maxApps; i++, index++) {
            if (!computeStackedSubBounds(getContext(), cellSize, index, sub)) {
                break;
            }
            Drawable item = getDrawable(getContext(), list.get(i), sub.width());
            item.setBounds(sub);
            this.mDrawableList.add(item);
        }
        // Pad to a full 2×2 with empty holders (Oppo StackedDrawable + bf_holder).
        Drawable holder = getContext().getDrawable(R.drawable.hxy_bf_holder_drawable);
        while (index < MAX_OUT_COUNT && holder != null) {
            if (!computeStackedSubBounds(getContext(), cellSize, index, sub)) {
                break;
            }
            Drawable slot = holder.getConstantState() != null
                    ? holder.getConstantState().newDrawable().mutate()
                    : holder.mutate();
            slot.setBounds(sub);
            this.mDrawableList.add(slot);
            index++;
        }
    }

    private Drawable getDrawable(Context context, WorkspaceItemInfo item, int iconSize) {
        Drawable folderDrawable = item.newIcon(context, BitmapInfo.FLAG_THEMED);
        folderDrawable.setBounds(0, 0, iconSize, iconSize);
        return folderDrawable;
    }
}
