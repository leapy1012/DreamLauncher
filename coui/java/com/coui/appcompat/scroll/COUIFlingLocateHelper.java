package com.coui.appcompat.scroll;

import android.content.Context;
import android.graphics.PointF;
import android.view.View;
import android.view.animation.Interpolator;
import androidx.recyclerview.widget.COUIRecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.OrientationHelper;


public class COUIFlingLocateHelper {
    private static final int CENTER_ALIGN = 2;
    private static final int INVALID_ALIGN = 0;
    private static final int INVALID_POSITION = -1;
    private static final float ONE = 1.0f;
    private static final int START_ALIGN = 1;
    private static final String TAG = "COUIFlingLocateHelper";
    private Context mContext;
    private OrientationHelper mHorizontalHelper;
    private RecyclerView.LayoutManager mLayoutManager;
    private OnCalculatePreChildDistanceListener mOnCalculatePreChildDistanceListener;
    private COUIRecyclerView mRecyclerView;
    private int mHorizontalItemAlign = 0;
    private boolean mEnableSnapToCenter = true;
    private Interpolator mCustomInterpolator = null;
    private int mCustomDuration = 0;
    private RecyclerView.OnScrollListener mAlignScrollListener = new RecyclerView.OnScrollListener() {
        boolean mScrolled = false;

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int i2) {
            super.onScrollStateChanged(recyclerView, i2);
            if (i2 == 0 && this.mScrolled) {
                this.mScrolled = false;
                COUIFlingLocateHelper.this.snapToTargetExistingView();
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int i2, int i6) {
            if (i2 == 0 && i6 == 0) {
                return;
            }
            this.mScrolled = true;
        }
    };

    public interface OnCalculatePreChildDistanceListener {
        int onCalculatePreChildDistance();

        int onCalculateTargetPosition(int i2, int i6);
    }

    private float computeDistancePerChild(RecyclerView.LayoutManager pVar, OrientationHelper nVar) {
        int childCount = pVar.getChildCount();
        if (childCount == 0) {
            return 1.0f;
        }
        View view = null;
        int i2 = Integer.MIN_VALUE;
        int i6 = Integer.MAX_VALUE;
        View view2 = null;
        for (int i10 = 0; i10 < childCount; i10++) {
            View childAt = pVar.getChildAt(i10);
            int position = pVar.getPosition(childAt);
            if (position != -1 && position != pVar.getItemCount() - 1 && position != 0) {
                if (position < i6) {
                    view = childAt;
                    i6 = position;
                }
                if (position > i2) {
                    view2 = childAt;
                    i2 = position;
                }
            }
        }
        if (view == null || view2 == null) {
            return 1.0f;
        }
        int iMax = Math.max(nVar.getDecoratedEnd(view), nVar.getDecoratedEnd(view2)) - Math.min(nVar.getDecoratedStart(view), nVar.getDecoratedStart(view2));
        if (iMax == 0) {
            return 1.0f;
        }
        return (iMax * 1.0f) / ((i2 - i6) + 1);
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

    private View findStartView(RecyclerView.LayoutManager pVar, OrientationHelper nVar) {
        int childCount = pVar.getChildCount();
        View view = null;
        if (childCount == 0) {
            return null;
        }
        if (pVar instanceof LinearLayoutManager) {
            LinearLayoutManager linearLayoutManager = (LinearLayoutManager) pVar;
            boolean z6 = linearLayoutManager.findFirstCompletelyVisibleItemPosition() == pVar.getItemCount() - 1;
            boolean z10 = linearLayoutManager.findLastCompletelyVisibleItemPosition() == pVar.getItemCount() - 1;
            if (z6 || z10) {
                return null;
            }
        }
        int i2 = isRtlMode(this.mContext) ? nVar.getEndAfterPadding() : nVar.getStartAfterPadding();
        int i6 = Integer.MAX_VALUE;
        for (int i10 = 0; i10 < childCount; i10++) {
            View childAt = pVar.getChildAt(i10);
            int iAbs = Math.abs((isRtlMode(this.mContext) ? nVar.getDecoratedEnd(childAt) : nVar.getDecoratedStart(childAt)) - i2);
            if (iAbs < i6) {
                view = childAt;
                i6 = iAbs;
            }
        }
        return view;
    }

    private OrientationHelper getHorizontalHelper(RecyclerView.LayoutManager pVar) {
        OrientationHelper nVar = this.mHorizontalHelper;
        if (nVar == null || nVar.getLayoutManager() != pVar) {
            this.mHorizontalHelper = OrientationHelper.createHorizontalHelper(pVar);
        }
        return this.mHorizontalHelper;
    }

    private RecyclerView.LayoutManager getLayoutManager() {
        RecyclerView.LayoutManager pVar = this.mLayoutManager;
        if (pVar == null || pVar != this.mRecyclerView.getLayoutManager()) {
            this.mLayoutManager = this.mRecyclerView.getLayoutManager();
        }
        return this.mLayoutManager;
    }

    private boolean isRtlMode(Context context) {
        return context != null && context.getResources().getConfiguration().getLayoutDirection() == 1;
    }

    private void smoothScrollBy(int i2, int i6) {
        int i10;
        Interpolator interpolator = this.mCustomInterpolator;
        if (interpolator == null || (i10 = this.mCustomDuration) == 0) {
            this.mRecyclerView.smoothScrollBy(i2, i6);
        } else {
            this.mRecyclerView.smoothScrollBy(i2, i6, interpolator, i10);
        }
    }


    public void snapToTargetExistingView() {
        RecyclerView.LayoutManager layoutManager;
        View viewFindSnapView;
        int iG;
        int iN;
        if ((!this.mEnableSnapToCenter && this.mHorizontalItemAlign == 2) || (layoutManager = getLayoutManager()) == null || (viewFindSnapView = findSnapView(layoutManager)) == null) {
            return;
        }
        int i2 = this.mHorizontalItemAlign;
        if (i2 == 2) {
            int iN2 = getHorizontalHelper(layoutManager).getStartAfterPadding() + (getHorizontalHelper(layoutManager).getTotalSpace() / 2);
            int itemCount = layoutManager.getItemCount() - 1;
            if (layoutManager.getPosition(viewFindSnapView) == 0) {
                iN2 = isRtlMode(this.mContext) ? getHorizontalHelper(layoutManager).getEndAfterPadding() - (getHorizontalHelper(layoutManager).getDecoratedMeasurement(viewFindSnapView) / 2) : getHorizontalHelper(layoutManager).getStartAfterPadding() + (getHorizontalHelper(layoutManager).getDecoratedMeasurement(viewFindSnapView) / 2);
            }
            if (layoutManager.getPosition(viewFindSnapView) == itemCount) {
                iN2 = isRtlMode(this.mContext) ? getHorizontalHelper(layoutManager).getStartAfterPadding() + (getHorizontalHelper(layoutManager).getDecoratedMeasurement(viewFindSnapView) / 2) : getHorizontalHelper(layoutManager).getEndAfterPadding() - (getHorizontalHelper(layoutManager).getDecoratedMeasurement(viewFindSnapView) / 2);
            }
            int iG2 = (getHorizontalHelper(layoutManager).getDecoratedStart(viewFindSnapView) + (getHorizontalHelper(layoutManager).getDecoratedMeasurement(viewFindSnapView) / 2)) - iN2;
            if (Math.abs(iG2) > 1.0f) {
                smoothScrollBy(iG2, 0);
                return;
            }
            return;
        }
        if (i2 == 1) {
            if (isRtlMode(this.mContext)) {
                iG = getHorizontalHelper(layoutManager).getDecoratedEnd(viewFindSnapView);
                iN = getHorizontalHelper(layoutManager).getEndAfterPadding();
            } else {
                iG = getHorizontalHelper(layoutManager).getDecoratedStart(viewFindSnapView);
                iN = getHorizontalHelper(layoutManager).getStartAfterPadding();
            }
            int i6 = iG - iN;
            if (Math.abs(i6) > 1.0f) {
                smoothScrollBy(i6, 0);
            }
        }
    }

    public void attachToRecyclerView(COUIRecyclerView cOUIRecyclerView) {
        this.mRecyclerView = cOUIRecyclerView;
        this.mContext = cOUIRecyclerView.getContext();
    }

    public void cancelHorizontalItemAlign() {
        this.mHorizontalItemAlign = 0;
        this.mRecyclerView.removeOnScrollListener(this.mAlignScrollListener);
    }

    public View findSnapView(RecyclerView.LayoutManager pVar) {
        if (pVar.canScrollHorizontally()) {
            int i2 = this.mHorizontalItemAlign;
            if (i2 == 2) {
                return findCenterView(pVar, getHorizontalHelper(pVar));
            }
            if (i2 == 1) {
                return findStartView(pVar, getHorizontalHelper(pVar));
            }
        }
        return null;
    }

    public int getHorizontalItemAlign() {
        return this.mHorizontalItemAlign;
    }


    public int getTargetViewDistance(int i2) {
        View viewFindSnapView;
        float fOnCalculatePreChildDistance;
        int iRound;
        int iG;
        RecyclerView.LayoutManager layoutManager = getLayoutManager();
        int itemCount = layoutManager.getItemCount();
        if (itemCount == 0 || (viewFindSnapView = findSnapView(layoutManager)) == null) {
            return -1;
        }
        int position = layoutManager.getPosition(viewFindSnapView);
        int i6 = itemCount - 1;
        PointF pointFComputeScrollVectorForPosition = ((RecyclerView.SmoothScroller.ScrollVectorProvider) layoutManager).computeScrollVectorForPosition(i6);
        if (pointFComputeScrollVectorForPosition == null) {
            return -1;
        }
        if (layoutManager.canScrollHorizontally()) {
            fOnCalculatePreChildDistance = this.mOnCalculatePreChildDistanceListener != null ? this.mOnCalculatePreChildDistanceListener.onCalculatePreChildDistance() : computeDistancePerChild(layoutManager, getHorizontalHelper(layoutManager));
            iRound = Math.round(i2 / fOnCalculatePreChildDistance);
            if (pointFComputeScrollVectorForPosition.x < 0.0f) {
                iRound = -iRound;
            }
        } else {
            fOnCalculatePreChildDistance = 1.0f;
            iRound = 0;
        }
        OnCalculatePreChildDistanceListener onCalculatePreChildDistanceListener = this.mOnCalculatePreChildDistanceListener;
        int iOnCalculateTargetPosition = onCalculatePreChildDistanceListener != null ? onCalculatePreChildDistanceListener.onCalculateTargetPosition(position, iRound) : iRound + position;
        if (iOnCalculateTargetPosition != position && iOnCalculateTargetPosition >= 0 && iOnCalculateTargetPosition < itemCount) {
            int i10 = this.mHorizontalItemAlign;
            if (i10 == 2) {
                View childAt = (layoutManager.getPosition(viewFindSnapView) != 0 || layoutManager.getChildCount() == 0) ? null : layoutManager.getChildAt(layoutManager.getChildCount() - 1);
                if (layoutManager.getPosition(viewFindSnapView) == i6 && layoutManager.getChildCount() != 0) {
                    childAt = layoutManager.getChildAt(0);
                }
                int iN = getHorizontalHelper(layoutManager).getStartAfterPadding() + (getHorizontalHelper(layoutManager).getTotalSpace() / 2);
                if (childAt != null) {
                    iG = getHorizontalHelper(layoutManager).getDecoratedStart(childAt) + (getHorizontalHelper(layoutManager).getDecoratedMeasurement(childAt) / 2) + (isRtlMode(this.mContext) ? -((int) ((iOnCalculateTargetPosition - layoutManager.getPosition(childAt)) * fOnCalculatePreChildDistance)) : (int) ((iOnCalculateTargetPosition - layoutManager.getPosition(childAt)) * fOnCalculatePreChildDistance));
                } else {
                    iG = getHorizontalHelper(layoutManager).getDecoratedStart(viewFindSnapView) + (getHorizontalHelper(layoutManager).getDecoratedMeasurement(viewFindSnapView) / 2) + (isRtlMode(this.mContext) ? -((int) ((iOnCalculateTargetPosition - layoutManager.getPosition(viewFindSnapView)) * fOnCalculatePreChildDistance)) : (int) ((iOnCalculateTargetPosition - layoutManager.getPosition(viewFindSnapView)) * fOnCalculatePreChildDistance));
                }
                return iG - iN;
            }
            if (i10 == 1) {
                int i11 = iOnCalculateTargetPosition - position;
                return ((isRtlMode(this.mContext) ? getHorizontalHelper(layoutManager).getDecoratedEnd(viewFindSnapView) : getHorizontalHelper(layoutManager).getDecoratedStart(viewFindSnapView)) + (isRtlMode(this.mContext) ? -((int) (i11 * fOnCalculatePreChildDistance)) : (int) (i11 * fOnCalculatePreChildDistance))) - (isRtlMode(this.mContext) ? getHorizontalHelper(layoutManager).getEndAfterPadding() : getHorizontalHelper(layoutManager).getStartAfterPadding());
            }
        }
        return -1;
    }

    public void setCustomSmooth(Interpolator interpolator, int i2) {
        this.mCustomInterpolator = interpolator;
        this.mCustomDuration = i2;
    }

    public void setEnableSnapToCenter(boolean z6) {
        this.mEnableSnapToCenter = z6;
    }

    public void setHorizontalItemAlign(int i2) {
        this.mHorizontalItemAlign = i2;
        this.mRecyclerView.addOnScrollListener(this.mAlignScrollListener);
    }

    public void setOnCalculatePreChildDistanceListener(OnCalculatePreChildDistanceListener onCalculatePreChildDistanceListener) {
        this.mOnCalculatePreChildDistanceListener = onCalculatePreChildDistanceListener;
    }

    public void trySnapToTargetExistingView() {
        if (this.mHorizontalItemAlign != 0) {
            snapToTargetExistingView();
        }
    }
}
