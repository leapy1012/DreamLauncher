package com.coui.appcompat.scroll;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.COUIRecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.OrientationHelper;
import com.coui.appcompat.log.COUILog;


public class COUIGradualStopHelper {
    private static final boolean DEBUG;
    private static final int INVALID_POSITION = -1;
    private static final int MAX_ITERATE = 1000;
    private static final float ONE = 1.0f;
    private static final String TAG = "COUIGradualStopHelper";
    private Context mContext;
    private RecyclerView.LayoutManager mLayoutManager;
    private COUIRecyclerView mRecyclerView;
    private OrientationHelper mOrientationHelper = null;
    private int mLastOrientation = -1;

    static {
        DEBUG = COUILog.LOG_DEBUG || COUILog.isLoggable(TAG, 3);
    }

    private View findCenterView(RecyclerView.LayoutManager pVar, OrientationHelper nVar) {
        int childCount = pVar.getChildCount();
        View view = null;
        if (childCount == 0) {
            return null;
        }
        int iN = nVar.getStartAfterPadding() + (nVar.getTotalSpace() / 2);
        int i2 = Integer.MAX_VALUE;
        for (int i6 = 0; i6 < childCount; i6++) {
            View childAt = pVar.getChildAt(i6);
            int iAbs = Math.abs((pVar.getDecoratedLeft(childAt) + (pVar.getDecoratedMeasuredWidth(childAt) / 2)) - iN);
            if (iAbs < i2) {
                view = childAt;
                i2 = iAbs;
            }
        }
        return view;
    }

    private int getDefaultItemCenterOffset(int i2) {
        View viewFindViewByPosition;
        int i6;
        int i10;
        RecyclerView.LayoutManager layoutManager = getLayoutManager();
        if (layoutManager == null || (viewFindViewByPosition = layoutManager.findViewByPosition(i2)) == null || !(viewFindViewByPosition.getLayoutParams() instanceof RecyclerView.LayoutParams)) {
            return 0;
        }
        RecyclerView.LayoutParams qVar = (RecyclerView.LayoutParams) viewFindViewByPosition.getLayoutParams();
        if ((layoutManager instanceof LinearLayoutManager) && ((LinearLayoutManager) layoutManager).getOrientation() == 1) {
            i6 = ((ViewGroup.MarginLayoutParams) qVar).topMargin;
            i10 = ((ViewGroup.MarginLayoutParams) qVar).bottomMargin;
        } else {
            i6 = ((ViewGroup.MarginLayoutParams) qVar).leftMargin;
            i10 = ((ViewGroup.MarginLayoutParams) qVar).rightMargin;
        }
        return i6 - i10;
    }

    private int getDefautDecoratedMeasurement(int i2) {
        int itemCount;
        View childAt;
        RecyclerView.LayoutManager layoutManager = getLayoutManager();
        if (layoutManager == null || (itemCount = layoutManager.getItemCount()) <= 0 || i2 < 0 || i2 >= itemCount) {
            return 0;
        }
        View viewFindViewByPosition = layoutManager.findViewByPosition(i2);
        OrientationHelper orientationHelper = getOrientationHelper(layoutManager);
        if (viewFindViewByPosition != null) {
            return orientationHelper.getDecoratedMeasurement(viewFindViewByPosition);
        }
        if (layoutManager.getChildCount() <= 0 || (childAt = layoutManager.getChildAt(0)) == null) {
            return 0;
        }
        return orientationHelper.getDecoratedMeasurement(childAt);
    }

    private RecyclerView.LayoutManager getLayoutManager() {
        RecyclerView.LayoutManager pVar = this.mLayoutManager;
        if (pVar == null || pVar != this.mRecyclerView.getLayoutManager()) {
            this.mLayoutManager = this.mRecyclerView.getLayoutManager();
        }
        return this.mLayoutManager;
    }

    private OrientationHelper getOrientationHelper(RecyclerView.LayoutManager pVar) {
        int orientation = pVar instanceof LinearLayoutManager ? ((LinearLayoutManager) pVar).getOrientation() : -1;
        OrientationHelper nVar = this.mOrientationHelper;
        if (nVar == null || this.mLastOrientation != orientation || nVar.getLayoutManager() != pVar) {
            this.mLastOrientation = orientation;
            this.mOrientationHelper = orientation == 1 ? OrientationHelper.createVerticalHelper(pVar) : OrientationHelper.createHorizontalHelper(pVar);
        }
        return this.mOrientationHelper;
    }

    private boolean isRtlMode(Context context) {
        COUIRecyclerView cOUIRecyclerView = this.mRecyclerView;
        return cOUIRecyclerView != null ? ViewCompat.getLayoutDirection(cOUIRecyclerView) == 1 : context != null && context.getResources().getConfiguration().getLayoutDirection() == 1;
    }

    private float iterateDisplacement(int i2, boolean z6, float f2, int i6, int i10) {
        int iAbs = Math.abs(i6);
        int i11 = 1;
        int i12 = z6 ? 1 : -1;
        if (isRtlMode(this.mContext)) {
            i12 = z6 ? -1 : 1;
        }
        float decoratedMeasurement = f2;
        int i13 = 0;
        int i14 = i2;
        while (decoratedMeasurement < iAbs && i14 >= 0 && i14 < i10) {
            int itemCenterOffset = getItemCenterOffset(i14);
            float decoratedMeasurement2 = decoratedMeasurement + ((getDecoratedMeasurement(i14) - (i12 * itemCenterOffset)) / 2.0f);
            boolean z10 = DEBUG;
            if (z10) {
                Log.d(TAG, "displacement:" + decoratedMeasurement2 + " nextPos:" + i14 + " offset:" + itemCenterOffset);
            }
            i14 = z6 ? i14 + 1 : i14 - 1;
            i13 += i11;
            if (i14 < 0 || i14 >= i10 || i13 > 1000) {
                return i6;
            }
            int decoratedMeasurement3 = getDecoratedMeasurement(i14);
            if (decoratedMeasurement3 == -1 || decoratedMeasurement3 < 0) {
                return i6;
            }
            int itemCenterOffset2 = getItemCenterOffset(i14);
            decoratedMeasurement = decoratedMeasurement2 + ((getDecoratedMeasurement(i14) + (i12 * itemCenterOffset2)) / 2.0f);
            if (z10) {
                Log.d(TAG, "displacement:" + decoratedMeasurement + " nextPos:" + i14 + " offset:" + itemCenterOffset2 + " nextItemWidth:" + decoratedMeasurement3);
            }
            i11 = 1;
        }
        return i12 * decoratedMeasurement;
    }

    public void attachToRecyclerView(COUIRecyclerView cOUIRecyclerView) {
        this.mRecyclerView = cOUIRecyclerView;
        this.mContext = cOUIRecyclerView.getContext();
    }

    public View getCenterItemView() {
        RecyclerView.LayoutManager layoutManager = getLayoutManager();
        int childCount = layoutManager.getChildCount();
        OrientationHelper orientationHelper = getOrientationHelper(layoutManager);
        float fN = orientationHelper.getStartAfterPadding() + (orientationHelper.getTotalSpace() / 2.0f);
        for (int i2 = 0; i2 < childCount; i2++) {
            View childAt = layoutManager.getChildAt(i2);
            if (childAt != null) {
                int iG = orientationHelper.getDecoratedStart(childAt);
                int iD = orientationHelper.getDecoratedEnd(childAt);
                if (fN >= iG && fN <= iD) {
                    return childAt;
                }
            }
        }
        return null;
    }

    public float getCenterToEdgeOffsetInVelocityDirection(int i2) {
        int childCount;
        RecyclerView.LayoutManager layoutManager = getLayoutManager();
        if (layoutManager == null || (childCount = layoutManager.getChildCount()) == 0) {
            return 0.0f;
        }
        OrientationHelper orientationHelper = getOrientationHelper(layoutManager);
        float fN = orientationHelper.getStartAfterPadding() + (orientationHelper.getTotalSpace() / 2.0f);
        for (int i6 = 0; i6 < childCount; i6++) {
            View childAt = layoutManager.getChildAt(i6);
            if (childAt != null) {
                int iG = orientationHelper.getDecoratedStart(childAt);
                int iD = orientationHelper.getDecoratedEnd(childAt);
                float f2 = iG;
                if (fN >= f2) {
                    float f10 = iD;
                    if (fN <= f10) {
                        if (i2 > 0) {
                            return f10 - fN;
                        }
                        if (i2 < 0) {
                            return f2 - fN;
                        }
                        float f11 = f2 - fN;
                        float f12 = f10 - fN;
                        return Math.abs(f11) <= Math.abs(f12) ? f11 : f12;
                    }
                } else {
                    continue;
                }
            }
        }
        if (DEBUG) {
            Log.d(TAG, "getCenterToEdgeOffsetInVelocityDirection has no center item");
        }
        return 0.0f;
    }

    public float getCenterToItemCenterOffsetUnderCenter() {
        int childCount;
        RecyclerView.LayoutManager layoutManager = getLayoutManager();
        float f2 = 0.0f;
        if (layoutManager == null || (childCount = layoutManager.getChildCount()) == 0) {
            return 0.0f;
        }
        OrientationHelper orientationHelper = getOrientationHelper(layoutManager);
        float fN = orientationHelper.getStartAfterPadding() + (orientationHelper.getTotalSpace() / 2.0f);
        for (int i2 = 0; i2 < childCount; i2++) {
            View childAt = layoutManager.getChildAt(i2);
            if (childAt != null) {
                int iG = orientationHelper.getDecoratedStart(childAt);
                int iD = orientationHelper.getDecoratedEnd(childAt);
                if (fN >= iG && fN <= iD) {
                    return getItemViewCenter(childAt) - fN;
                }
            }
        }
        float f10 = Float.MAX_VALUE;
        for (int i6 = 0; i6 < childCount; i6++) {
            View childAt2 = layoutManager.getChildAt(i6);
            if (childAt2 != null) {
                float itemViewCenter = getItemViewCenter(childAt2) - fN;
                float fAbs = Math.abs(itemViewCenter);
                if (fAbs < f10) {
                    f2 = itemViewCenter;
                    f10 = fAbs;
                }
            }
        }
        return f2;
    }

    public float getCenterViewNextPositionCenter(View view, int i2, boolean z6) {
        RecyclerView.LayoutManager layoutManager = getLayoutManager();
        if (layoutManager == null) {
            return 0.0f;
        }
        OrientationHelper orientationHelper = getOrientationHelper(layoutManager);
        int iD = orientationHelper.getDecoratedEnd(view);
        int iG = orientationHelper.getDecoratedStart(view);
        int itemCenterOffset = getItemCenterOffset(i2);
        int decoratedMeasurement = getDecoratedMeasurement(i2);
        int i6 = z6 ? 1 : -1;
        if (isRtlMode(this.mContext)) {
            i6 = z6 ? -1 : 1;
        }
        if (i6 != 1) {
            iD = iG;
        }
        return iD + ((i6 * (decoratedMeasurement + (itemCenterOffset * i6))) / 2.0f);
    }

    public int getDecoratedMeasurement(int i2) {
        if (getLayoutManager() == null) {
            return 0;
        }
        this.mRecyclerView.getAdapter();
        return getDefautDecoratedMeasurement(i2);
    }

    public float getDisplacementToAlignCenter(int i2, int i6) {
        float fAbs;
        int position;
        COUIGradualStopHelper cOUIGradualStopHelper = this;
        if (!validation()) {
            return 0.0f;
        }
        RecyclerView.LayoutManager layoutManager = getLayoutManager();
        OrientationHelper orientationHelper = cOUIGradualStopHelper.getOrientationHelper(layoutManager);
        float fN = orientationHelper.getStartAfterPadding() + (orientationHelper.getTotalSpace() / 2.0f);
        int i10 = 0;
        boolean z6 = true;
        if (!cOUIGradualStopHelper.isRtlMode(cOUIGradualStopHelper.mContext) ? i2 <= 0 : i2 >= 0) {
            z6 = false;
        }
        int childCount = layoutManager.getChildCount();
        int itemCount = layoutManager.getItemCount();
        View centerItemView = getCenterItemView();
        int position2 = centerItemView != null ? layoutManager.getPosition(centerItemView) : -1;
        if (centerItemView == null || position2 == -1) {
            fAbs = 0.0f;
            position = 0;
        } else {
            position = z6 ? position2 + 1 : position2 - 1;
            if (position < 0 || position >= itemCount) {
                float itemViewCenter = cOUIGradualStopHelper.getItemViewCenter(centerItemView) - fN;
                return (((float) i2) * itemViewCenter <= 0.0f || Math.abs(itemViewCenter) <= ((float) Math.abs(i6))) ? i6 : itemViewCenter;
            }
            fAbs = Math.abs(cOUIGradualStopHelper.getCenterViewNextPositionCenter(centerItemView, position, z6) - fN);
        }
        if (fAbs == 0.0f) {
            float f2 = Float.MAX_VALUE;
            while (i10 < childCount) {
                View childAt = layoutManager.getChildAt(i10);
                if (childAt != null) {
                    float itemViewCenter2 = cOUIGradualStopHelper.getItemViewCenter(childAt) - fN;
                    if (i2 * itemViewCenter2 > 0.0f && Math.abs(itemViewCenter2) < Math.abs(f2)) {
                        float fAbs2 = Math.abs(itemViewCenter2);
                        position = layoutManager.getPosition(childAt);
                        f2 = itemViewCenter2;
                        fAbs = fAbs2;
                    }
                }
                i10++;
                cOUIGradualStopHelper = this;
            }
        }
        float f10 = fAbs;
        if (DEBUG) {
            Log.d(TAG, "initialVelocity:" + i2 + " distance:" + i6 + " centerAdapterPosition:" + position2 + " startPos:" + position + " displacement:" + f10 + " itemCount:" + itemCount);
        }
        return iterateDisplacement(position, z6, f10, i6, itemCount);
    }

    public int getItemCenterOffset(int i2) {
        if (getLayoutManager() == null) {
            return 0;
        }
        this.mRecyclerView.getAdapter();
        return getDefaultItemCenterOffset(i2);
    }

    public float getItemViewCenter(View view) {
        RecyclerView.LayoutManager layoutManager = getLayoutManager();
        if (layoutManager == null || view == null) {
            return 0.0f;
        }
        OrientationHelper orientationHelper = getOrientationHelper(layoutManager);
        return orientationHelper.getDecoratedStart(view) + ((orientationHelper.getDecoratedMeasurement(view) + getItemCenterOffset(layoutManager.getPosition(view))) / 2.0f);
    }

    public void trySnapToTargetExistingView() {
        OrientationHelper orientationHelper;
        View viewFindCenterView;
        RecyclerView.LayoutManager layoutManager = getLayoutManager();
        if (layoutManager == null || (viewFindCenterView = findCenterView(layoutManager, (orientationHelper = getOrientationHelper(layoutManager)))) == null) {
            return;
        }
        int iN = orientationHelper.getStartAfterPadding() + (orientationHelper.getTotalSpace() / 2);
        int itemCount = layoutManager.getItemCount() - 1;
        if (layoutManager.getPosition(viewFindCenterView) == 0) {
            iN = isRtlMode(this.mContext) ? orientationHelper.getEndAfterPadding() - (orientationHelper.getDecoratedMeasurement(viewFindCenterView) / 2) : orientationHelper.getStartAfterPadding() + (orientationHelper.getDecoratedMeasurement(viewFindCenterView) / 2);
        }
        if (layoutManager.getPosition(viewFindCenterView) == itemCount) {
            iN = isRtlMode(this.mContext) ? orientationHelper.getStartAfterPadding() + (orientationHelper.getDecoratedMeasurement(viewFindCenterView) / 2) : orientationHelper.getEndAfterPadding() - (orientationHelper.getDecoratedMeasurement(viewFindCenterView) / 2);
        }
        int iG = (orientationHelper.getDecoratedStart(viewFindCenterView) + (orientationHelper.getDecoratedMeasurement(viewFindCenterView) / 2)) - iN;
        if (Math.abs(iG) > 1.0f) {
            if ((layoutManager instanceof LinearLayoutManager) && ((LinearLayoutManager) layoutManager).getOrientation() == 1) {
                this.mRecyclerView.smoothScrollBy(0, iG);
            } else {
                this.mRecyclerView.smoothScrollBy(iG, 0);
            }
        }
    }

    public boolean validation() {
        RecyclerView.LayoutManager layoutManager = getLayoutManager();
        if (layoutManager == null) {
            return false;
        }
        return (layoutManager.getChildCount() == 0 || layoutManager.getItemCount() == 0) ? false : true;
    }
}
