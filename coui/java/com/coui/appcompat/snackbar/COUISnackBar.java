package com.coui.appcompat.snackbar;

import com.coui.appcompat.R;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import com.coui.appcompat.animation.dynamicanimation.COUIDynamicAnimation;
import com.coui.appcompat.animation.dynamicanimation.COUISpringAnimation;
import com.coui.appcompat.animation.dynamicanimation.COUISpringForce;
import com.coui.appcompat.contextutil.COUIContextUtil;
import com.coui.appcompat.dialog.AppFeatureUtil;
import com.coui.appcompat.log.COUILog;
import com.coui.appcompat.roundcorner.RoundCornerUtil;
import com.coui.appcompat.textviewcompatutil.COUITextViewCompatUtil;
import com.coui.appcompat.uiutil.ShadowUtils;
import com.coui.appcompat.uiutil.UIUtil;
import com.coui.component.responsiveui.ResponsiveUIModel;
import com.coui.component.responsiveui.layoutgrid.MarginType;
import com.oplus.graphics.OplusOutline;
import com.oplus.graphics.OplusOutlineAdapter;


public class COUISnackBar extends RelativeLayout {
    private static final float DEFAULT_SNACKBAR_DISMISS_SPRING_RESPONSE = 0.25f;
    private static final float DEFAULT_SNACKBAR_SHOW_SPRING_RESPONSE = 0.3f;
    private static final float DEFAULT_SNACKBAR_SPRING_BOUNCE = 0.0f;
    private static final float DEFAULT_SPRING_FACTOR = 10000.0f;
    private static final int DEFAULT_TRANSLATION_END = 0;
    private static final float ONE = 1.0f;
    private static final float POINT_EIGHT = 0.8f;
    private static final int SINGLE_LINE_NUMBER = 1;
    private static final String TAG = "COUISnackBar";
    private static final float ZERO = 0.0f;
    protected static int mCOUISnackBarBottomMargin;
    private final int DEFAULT_ACTION_MARGIN_HORIZONTAL_END;
    private final int DEFAULT_ACTION_MARGIN_HORIZONTAL_START;
    private final int DEFAULT_ACTION_MARGIN_TOP_HORIZONTAL;
    private final int DEFAULT_ACTION_MARGIN_TOP_HORIZONTAL_TINY;
    private final int DEFAULT_ACTION_MARGIN_VERTICAL;
    private final int DEFAULT_ACTION_TEXT_MAX_WIDTH;
    private final int DEFAULT_CONTENT_MARGIN_HORIZONTAL;
    private final int DEFAULT_CONTENT_MARGIN_VERTICAL;
    private final int DEFAULT_CONTEXT_MARGIN_START_WITH_ICON;
    private final int DEFAULT_ICON_WIDTH;
    protected boolean isAdjustLayout;
    protected TextView mActionView;
    protected Runnable mAutoDismissRunnable;
    private ViewGroup mCOUISnackBarParent;
    private int mCardMarginEnd;
    private int mCardMarginStart;
    private String mContentText;
    private int mContentTextWidth;
    protected TextView mContentView;
    private int mDuration;
    protected ImageView mIconDrawableView;
    private boolean mIsDefaultRadius;
    private boolean mIsEntering;
    private boolean mIsSupportSmoothRoundCorner;
    private boolean mIsTiny;
    private int mLastLayoutType;
    private OnStatusChangeListener mOnStatusChangeListener;
    private Rect mRect;
    private ResponsiveUIModel mResponsiveUIModel;
    protected View mRootView;
    private float mSnackBarAnimationProgress;
    protected ViewGroup mSnackBarLayout;
    private final FloatPropertyCompat<Float> mSnackBarProperty;
    private COUISpringAnimation mSpringAnimation;
    private boolean mWithoutAnima;

    public class AutoDismissRunnable implements Runnable {
        public AutoDismissRunnable() {
        }

        @Override
        public void run() {
            COUISnackBar.this.dismiss();
        }
    }

    public interface OnStatusChangeListener {
        void onDismissed(COUISnackBar cOUISnackBar);

        void onShown(COUISnackBar cOUISnackBar);
    }

    public COUISnackBar(Context context) {
        super(context);
        this.DEFAULT_ACTION_TEXT_MAX_WIDTH = getResources().getDimensionPixelSize(R.dimen.coui_snack_bar_action_max_width);
        this.DEFAULT_CONTENT_MARGIN_VERTICAL = getResources().getDimensionPixelSize(R.dimen.coui_snack_bar_child_margin_vertical);
        this.DEFAULT_CONTENT_MARGIN_HORIZONTAL = getResources().getDimensionPixelSize(R.dimen.coui_snack_bar_child_margin_horizontal_start);
        this.DEFAULT_ACTION_MARGIN_VERTICAL = getResources().getDimensionPixelSize(R.dimen.coui_snack_bar_action_margin_vertical);
        this.DEFAULT_CONTEXT_MARGIN_START_WITH_ICON = getResources().getDimensionPixelSize(R.dimen.coui_snack_bar_context_margin_start_with_icon);
        this.DEFAULT_ICON_WIDTH = getResources().getDimensionPixelSize(R.dimen.coui_snack_bar_icon_width);
        this.DEFAULT_ACTION_MARGIN_TOP_HORIZONTAL = getResources().getDimensionPixelSize(R.dimen.coui_snack_bar_action_margin_top_horizontal);
        this.DEFAULT_ACTION_MARGIN_TOP_HORIZONTAL_TINY = getResources().getDimensionPixelSize(R.dimen.coui_snack_bar_action_margin_top_horizontal_tiny);
        this.DEFAULT_ACTION_MARGIN_HORIZONTAL_START = getResources().getDimensionPixelSize(R.dimen.coui_snack_bar_action_margin_horizontal_start);
        this.DEFAULT_ACTION_MARGIN_HORIZONTAL_END = getResources().getDimensionPixelSize(R.dimen.coui_snack_bar_action_margin_horizontal_end);
        this.mWithoutAnima = false;
        this.mRect = new Rect();
        this.mResponsiveUIModel = new ResponsiveUIModel(getContext(), 0, 0);
        this.mIsDefaultRadius = true;
        this.isAdjustLayout = true;
        this.mIsSupportSmoothRoundCorner = false;
        this.mSnackBarAnimationProgress = 0.0f;
        this.mLastLayoutType = -1;
        this.mSnackBarProperty = new FloatPropertyCompat<Float>("snackBarProperty") {
            @Override
            public float getValue(Float f2) {
                return COUISnackBar.this.mSnackBarAnimationProgress;
            }

            @Override
            public void setValue(Float f2, float f10) {
                COUISnackBar.this.setSnackBarProgress(f10);
            }
        };
        initCOUISnackBar(context, null);
    }

    private void alignCenter(View view, int i2) {
        if (view == null || getViewTotalHeight(view) == i2) {
            return;
        }
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
        int measuredHeight = (i2 - view.getMeasuredHeight()) / 2;
        if (this.mLastLayoutType != 0) {
            view.offsetTopAndBottom(measuredHeight - layoutParams.topMargin);
        }
        layoutParams.topMargin = measuredHeight;
        layoutParams.bottomMargin = measuredHeight;
    }

    private void animateSpring(final boolean z6) {
        this.mIsEntering = z6;
        this.mSpringAnimation = new COUISpringAnimation(Float.valueOf(this.mSnackBarAnimationProgress), this.mSnackBarProperty);
        COUISpringForce cOUISpringForce = new COUISpringForce();
        cOUISpringForce.setBounce(0.0f);
        if (z6) {
            cOUISpringForce.setResponse(0.3f);
        } else {
            cOUISpringForce.setResponse(DEFAULT_SNACKBAR_DISMISS_SPRING_RESPONSE);
        }
        this.mSpringAnimation.setSpring(cOUISpringForce);
        this.mSpringAnimation.addEndListener(new COUIDynamicAnimation.OnAnimationEndListener() {
            @Override
            public void onAnimationEnd(COUIDynamicAnimation cOUIDynamicAnimation, boolean z10, float f2, float f10) {
                if (z6) {
                    return;
                }
                COUISnackBar.this.dismissView();
            }
        });
        this.mSpringAnimation.setStartValue(0.0f);
        this.mSpringAnimation.animateToFinalPosition(10000.0f);
    }

    private void animationIn() {
        setVisibility(0);
        setTranslationY(0.0f);
        animateSpring(true);
    }


    public void dismissView() {
        ViewGroup viewGroup = this.mSnackBarLayout;
        if (viewGroup != null) {
            viewGroup.setVisibility(8);
        }
        ViewGroup viewGroup2 = this.mCOUISnackBarParent;
        if (viewGroup2 != null) {
            viewGroup2.removeView(this.mRootView);
        }
        OnStatusChangeListener onStatusChangeListener = this.mOnStatusChangeListener;
        if (onStatusChangeListener != null) {
            onStatusChangeListener.onDismissed(this);
        }
    }

    public static ViewGroup findSuitableParent(View view) {
        ViewGroup viewGroup = null;
        while (!(view instanceof CoordinatorLayout)) {
            if (view instanceof FrameLayout) {
                if (view.getId() == 16908290) {
                    return (ViewGroup) view;
                }
                viewGroup = (ViewGroup) view;
            }
            if (view != null) {
                Object parent = view.getParent();
                view = parent instanceof View ? (View) parent : null;
            }
            if (view == null) {
                return viewGroup;
            }
        }
        return (ViewGroup) view;
    }

    private int getContainerWidth() {
        int paddingLeft = this.mContentTextWidth + this.mSnackBarLayout.getPaddingLeft() + this.mSnackBarLayout.getPaddingRight();
        if (this.mActionView.getVisibility() == 0) {
            paddingLeft += this.mActionView.getMeasuredWidth() + this.DEFAULT_ACTION_MARGIN_HORIZONTAL_END;
        }
        return isCOUISnackBarHasIcon() ? paddingLeft + this.DEFAULT_ICON_WIDTH + this.DEFAULT_CONTEXT_MARGIN_START_WITH_ICON : paddingLeft;
    }

    private int getMaxWidth() {
        getWindowVisibleDisplayFrame(this.mRect);
        this.mResponsiveUIModel.rebuild(Math.max(0, this.mRect.width()), Math.max(0, this.mRect.height())).chooseMargin(MarginType.MARGIN_SMALL);
        return this.mResponsiveUIModel.calculateGridWidth(6);
    }

    private int getViewTotalHeight(View view) {
        if (view == null) {
            return 0;
        }
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
        return view.getMeasuredHeight() + layoutParams.topMargin + layoutParams.bottomMargin;
    }

    private boolean isCOUISnackBarHasIcon() {
        return this.mIconDrawableView.getDrawable() != null;
    }

    private boolean isInSecondaryDisplay(Context context) {
        try {
            return context.getDisplay().getDisplayId() == 1;
        } catch (UnsupportedOperationException e2) {
            Log.w(TAG, e2.toString());
            return AppFeatureUtil.isSecondaryScreen(context);
        } catch (RuntimeException e10) {
            Log.w(TAG, e10.toString());
            return AppFeatureUtil.isSecondaryScreen(context);
        }
    }

    private boolean isVertical(int i2) {
        return this.mContentView.getLineCount() > 1 || (getContainerWidth() > i2);
    }

    private void layoutHorizontally() {
        int iMax = Math.max(getViewTotalHeight(this.mContentView), getViewTotalHeight(this.mActionView));
        if (this.mIsTiny) {
            setTinyParams(this.mContentView);
            setTinyParams(this.mActionView);
            return;
        }
        if (isCOUISnackBarHasIcon()) {
            iMax = Math.max(getViewTotalHeight(this.mIconDrawableView), iMax);
            alignCenter(this.mIconDrawableView, iMax);
        }
        alignCenter(this.mContentView, iMax);
        alignCenter(this.mActionView, iMax);
    }

    private void layoutVertically() {
        Resources resources;
        int i2;
        int dimensionPixelSize = getResources().getDimensionPixelSize(R.dimen.coui_snack_bar_child_margin_vertical_multi_lines);
        if (isCOUISnackBarHasIcon()) {
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) this.mIconDrawableView.getLayoutParams();
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) this.mContentView.getLayoutParams();
            int measuredHeight = this.mContentView.getMeasuredHeight();
            int measuredHeight2 = this.mIconDrawableView.getMeasuredHeight();
            if (measuredHeight2 > measuredHeight) {
                layoutParams.topMargin = dimensionPixelSize;
                marginLayoutParams.topMargin = ((measuredHeight2 - measuredHeight) / 2) + dimensionPixelSize;
            } else {
                marginLayoutParams.topMargin = dimensionPixelSize;
                layoutParams.topMargin = ((measuredHeight - measuredHeight2) / 2) + dimensionPixelSize;
            }
            this.mContentView.setLayoutParams(marginLayoutParams);
        } else {
            ViewGroup.MarginLayoutParams marginLayoutParams2 = (ViewGroup.MarginLayoutParams) this.mContentView.getLayoutParams();
            marginLayoutParams2.topMargin = dimensionPixelSize;
            this.mContentView.setLayoutParams(marginLayoutParams2);
        }
        RelativeLayout.LayoutParams layoutParams2 = (RelativeLayout.LayoutParams) this.mActionView.getLayoutParams();
        layoutParams2.topMargin = dimensionPixelSize + this.mContentView.getMeasuredHeight() + (this.mIsTiny ? this.DEFAULT_ACTION_MARGIN_TOP_HORIZONTAL_TINY : this.DEFAULT_ACTION_MARGIN_TOP_HORIZONTAL);
        if (this.mIsTiny) {
            resources = getResources();
            i2 = R.dimen.coui_snack_bar_action_margin_bottom_multi_lines_tiny;
        } else {
            resources = getResources();
            i2 = R.dimen.coui_snack_bar_action_margin_bottom_multi_lines;
        }
        layoutParams2.bottomMargin = resources.getDimensionPixelSize(i2);
        this.mActionView.setLayoutParams(layoutParams2);
        if (this.mIsTiny) {
            int dimensionPixelSize2 = getResources().getDimensionPixelSize(R.dimen.coui_snack_bar_action_padding_tiny);
            TextView textView = this.mActionView;
            textView.setPadding(textView.getPaddingLeft(), dimensionPixelSize2, this.mActionView.getPaddingRight(), dimensionPixelSize2);
        }
    }

    public static COUISnackBar make(View view, String str, int i2) {
        return make(view.getContext(), view, str, i2);
    }

    private void resetMarginHorizontal() {
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) this.mIconDrawableView.getLayoutParams();
        Resources resources = getResources();
        int i2 = R.dimen.coui_snack_bar_icon_margin_top_horizontal;
        layoutParams.topMargin = resources.getDimensionPixelSize(i2);
        layoutParams.bottomMargin = getResources().getDimensionPixelSize(i2);
        this.mIconDrawableView.setLayoutParams(layoutParams);
        RelativeLayout.LayoutParams layoutParams2 = (RelativeLayout.LayoutParams) this.mContentView.getLayoutParams();
        Resources resources2 = getResources();
        int i6 = R.dimen.coui_snack_bar_child_margin_vertical;
        layoutParams2.topMargin = resources2.getDimensionPixelSize(i6);
        layoutParams2.bottomMargin = getResources().getDimensionPixelSize(i6);
        this.mContentView.setLayoutParams(layoutParams2);
        RelativeLayout.LayoutParams layoutParams3 = (RelativeLayout.LayoutParams) this.mActionView.getLayoutParams();
        Resources resources3 = getResources();
        int i10 = R.dimen.coui_snack_bar_action_margin_vertical;
        layoutParams3.topMargin = resources3.getDimensionPixelSize(i10);
        layoutParams3.bottomMargin = getResources().getDimensionPixelSize(i10);
        this.mActionView.setLayoutParams(layoutParams3);
    }

    private void setActionText(String str) {
        this.mActionView.setText(str);
    }


    public void setSnackBarProgress(float f2) {
        float f10;
        this.mSnackBarAnimationProgress = f2;
        float f11 = f2 / 10000.0f;
        boolean z6 = this.mIsEntering;
        float f12 = POINT_EIGHT;
        float f13 = 0.0f;
        float f14 = 1.0f;
        if (z6) {
            f10 = 1.0f;
        } else {
            f10 = 0.0f;
            f13 = 1.0f;
            f14 = 0.8f;
            f12 = 1.0f;
        }
        this.mSnackBarLayout.setScaleX(UIUtil.getConvertedFraction(f12, f14, f11));
        this.mSnackBarLayout.setScaleY(UIUtil.getConvertedFraction(f12, f14, f11));
        this.mSnackBarLayout.setAlpha(UIUtil.getConvertedFraction(f13, f10, f11));
    }

    private void setTinyParams(TextView textView) {
        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) textView.getLayoutParams();
        Resources resources = getResources();
        int i2 = R.dimen.coui_snack_bar_action_margin_bottom_single_lines_tiny;
        marginLayoutParams.topMargin = resources.getDimensionPixelSize(i2);
        marginLayoutParams.bottomMargin = getResources().getDimensionPixelSize(i2);
        textView.setLayoutParams(marginLayoutParams);
        textView.setPadding(textView.getPaddingStart(), 0, textView.getPaddingEnd(), 0);
    }

    public void adjustLayout() {
        if (isVertical(this.mSnackBarLayout.getMeasuredWidth())) {
            this.mIsDefaultRadius = false;
            layoutVertically();
            this.mLastLayoutType = 1;
        } else {
            this.mIsDefaultRadius = true;
            layoutHorizontally();
            this.mLastLayoutType = 0;
        }
    }

    public void dismiss() {
        COUISpringAnimation cOUISpringAnimation = this.mSpringAnimation;
        if (cOUISpringAnimation != null && cOUISpringAnimation.isRunning() && !this.mIsEntering) {
            COUILog.d(TAG, "is in dismissing");
            return;
        }
        Runnable runnable = this.mAutoDismissRunnable;
        if (runnable != null) {
            removeCallbacks(runnable);
        }
        animateSpring(false);
    }

    public String getActionText() {
        return String.valueOf(this.mActionView.getText());
    }

    public TextView getActionView() {
        return this.mActionView;
    }

    public String getContentText() {
        return String.valueOf(this.mContentView.getText());
    }

    public TextView getContentView() {
        return this.mContentView;
    }

    public int getDuration() {
        return this.mDuration;
    }

    public void initCOUISnackBar(Context context, AttributeSet attributeSet) {
        View viewInflate = View.inflate(context, R.layout.coui_snack_bar_item, this);
        this.mRootView = viewInflate;
        this.mSnackBarLayout = (ViewGroup) viewInflate.findViewById(R.id.snack_bar);
        this.mContentView = (TextView) this.mRootView.findViewById(R.id.tv_snack_bar_content);
        this.mActionView = (TextView) this.mRootView.findViewById(R.id.tv_snack_bar_action);
        this.mIconDrawableView = (ImageView) this.mRootView.findViewById(R.id.iv_snack_bar_icon);
        this.mIsTiny = isInSecondaryDisplay(getContext());
        mCOUISnackBarBottomMargin = new ViewGroup.MarginLayoutParams(context, attributeSet).bottomMargin;
        setVisibility(8);
        this.mAutoDismissRunnable = new AutoDismissRunnable();
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.COUISnackBar, 0, 0);
        try {
            try {
                int i2 = R.styleable.COUISnackBar_defaultSnackBarContentText;
                if (typedArrayObtainStyledAttributes.getString(i2) != null) {
                    setContentText(typedArrayObtainStyledAttributes.getString(i2));
                    setDuration(typedArrayObtainStyledAttributes.getInt(R.styleable.COUISnackBar_snackBarDisappearTime, 0));
                }
                setIconDrawable(typedArrayObtainStyledAttributes.getDrawable(R.styleable.COUISnackBar_couiSnackBarIcon));
            } catch (Exception e2) {
                Log.e(TAG, "Failure setting COUISnackBar " + e2.getMessage());
            }
            typedArrayObtainStyledAttributes.recycle();
            this.mIsSupportSmoothRoundCorner = RoundCornerUtil.isVersionSupport();
            final int attrDimens = COUIContextUtil.getAttrDimens(context, R.attr.couiRoundCornerXXL);
            final int attrDimens2 = COUIContextUtil.getAttrDimens(context, R.attr.couiRoundCornerL);
            final int attrDimens3 = COUIContextUtil.getAttrDimens(context, R.attr.couiRoundCornerLRadius);
            final float attrFloat = COUIContextUtil.getAttrFloat(context, R.attr.couiRoundCornerLWeight);
            this.mSnackBarLayout.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    if (RoundCornerUtil.getSmoothStyleType() == 1) {
                        new OplusOutlineAdapter(outline, 1).setSmoothRoundRect(new Rect(0, 0, view.getWidth(), view.getHeight()), COUISnackBar.this.mIsDefaultRadius ? view.getHeight() / 2.0f : attrDimens2);
                        return;
                    }
                    if (RoundCornerUtil.getSmoothStyleType() != 0) {
                        outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), COUISnackBar.this.mIsDefaultRadius ? attrDimens : attrDimens2);
                    } else if (COUISnackBar.this.mIsDefaultRadius) {
                        outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), attrDimens);
                    } else {
                        new OplusOutline(outline).setSmoothRoundRect(0, 0, view.getWidth(), view.getHeight(), attrDimens3, attrFloat);
                    }
                }
            });
            this.mSnackBarLayout.setClipToOutline(true);
            ShadowUtils.setElevationToView(this.mSnackBarLayout, 2, getContext().getResources().getDimensionPixelOffset(R.dimen.coui_snack_bar_shadow_size), context.getResources().getDimensionPixelOffset(R.dimen.support_shadow_size_level_one), getContext().getResources().getColor(R.color.coui_snack_bar_background_shadow_color));
            Resources resources = getResources();
            int i6 = R.dimen.grid_guide_column_card_margin_start;
            this.mCardMarginStart = resources.getDimensionPixelOffset(i6) - this.mSnackBarLayout.getPaddingStart();
            this.mCardMarginEnd = getResources().getDimensionPixelOffset(i6) - this.mSnackBarLayout.getPaddingEnd();
        } catch (Throwable th) {
            typedArrayObtainStyledAttributes.recycle();
            throw th;
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(this.mAutoDismissRunnable);
        this.mCOUISnackBarParent = null;
    }

    @Override
    public void onLayout(boolean z6, int i2, int i6, int i10, int i11) {
        super.onLayout(z6, i2, i6, i10, i11);
        if (z6 && this.isAdjustLayout) {
            adjustLayout();
        }
    }

    @Override
    public void onMeasure(int i2, int i6) {
        int i10;
        int mode = View.MeasureSpec.getMode(i2);
        int size = View.MeasureSpec.getSize(i2);
        this.mContentTextWidth = ((int) this.mContentView.getPaint().measureText(this.mContentText)) + (this.DEFAULT_CONTENT_MARGIN_HORIZONTAL << 1);
        int maxWidth = getMaxWidth() + this.mSnackBarLayout.getPaddingLeft() + this.mSnackBarLayout.getPaddingRight();
        if (maxWidth > size) {
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) this.mSnackBarLayout.getLayoutParams();
            layoutParams.setMarginStart(this.mCardMarginStart);
            layoutParams.setMarginEnd(this.mCardMarginEnd);
            this.mSnackBarLayout.setLayoutParams(layoutParams);
            i10 = (size - this.mCardMarginStart) - this.mCardMarginEnd;
        } else {
            i10 = (maxWidth <= 0 || mode == 0) ? size : maxWidth;
        }
        if (!isVertical(i10) && this.mLastLayoutType == 1) {
            resetMarginHorizontal();
        }
        if (maxWidth > 0 && mode != 0) {
            i2 = View.MeasureSpec.makeMeasureSpec(Math.min(maxWidth, size), mode);
        }
        if (this.mIsTiny) {
            RelativeLayout.LayoutParams layoutParams2 = (RelativeLayout.LayoutParams) this.mSnackBarLayout.getLayoutParams();
            Resources resources = getResources();
            int i11 = R.dimen.coui_snack_bar_layout_margin_tiny;
            layoutParams2.setMarginStart(resources.getDimensionPixelOffset(i11));
            layoutParams2.setMarginEnd(getResources().getDimensionPixelOffset(i11));
            this.mSnackBarLayout.setLayoutParams(layoutParams2);
        }
        super.onMeasure(i2, i6);
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            Runnable runnable = this.mAutoDismissRunnable;
            if (runnable != null) {
                removeCallbacks(runnable);
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            Runnable runnable = this.mAutoDismissRunnable;
            if (runnable != null && getDuration() != 0) {
                removeCallbacks(runnable);
                postDelayed(this.mAutoDismissRunnable, getDuration());
            }
        }
        return true;
    }

    public void setContentText(int i2) {
        setContentText(getResources().getString(i2));
    }

    public void setDismissWithoutAnimate(boolean z6) {
        this.mWithoutAnima = z6;
    }

    public void setDuration(int i2) {
        this.mDuration = i2;
    }

    @Override
    public void setEnabled(boolean z6) {
        Runnable runnable;
        super.setEnabled(z6);
        this.mActionView.setEnabled(z6);
        this.mContentView.setEnabled(z6);
        this.mIconDrawableView.setEnabled(z6);
        if (getDuration() == 0 || (runnable = this.mAutoDismissRunnable) == null) {
            return;
        }
        removeCallbacks(runnable);
        postDelayed(this.mAutoDismissRunnable, getDuration());
    }

    @Deprecated
    public void setIconDrawable(int i2) {
        setIconDrawable(getResources().getDrawable(i2, getContext().getTheme()));
    }

    public void setOnAction(int i2, View.OnClickListener onClickListener) {
        setOnAction(getResources().getString(i2), onClickListener);
    }

    public void setOnStatusChangeListener(OnStatusChangeListener onStatusChangeListener) {
        this.mOnStatusChangeListener = onStatusChangeListener;
    }

    public void setParent(ViewGroup viewGroup) {
        this.mCOUISnackBarParent = viewGroup;
    }

    public void show() {
        Runnable runnable;
        if (getDuration() != 0 && (runnable = this.mAutoDismissRunnable) != null) {
            removeCallbacks(runnable);
            postDelayed(this.mAutoDismissRunnable, getDuration());
        }
        OnStatusChangeListener onStatusChangeListener = this.mOnStatusChangeListener;
        if (onStatusChangeListener != null) {
            onStatusChangeListener.onShown(this);
        }
        animationIn();
        if (getParent() instanceof ViewGroup) {
            ((ViewGroup) getParent()).setClipChildren(false);
        }
    }

    public static COUISnackBar make(Context context, View view, String str, int i2) {
        return make(context, view, str, i2, context.getResources().getDimensionPixelSize(R.dimen.coui_snack_bar_margin_bottom));
    }

    public void setContentText(String str) {
        if (!TextUtils.isEmpty(str)) {
            this.mContentView.setText(str);
            this.mContentText = str;
            return;
        }
        this.mContentView.setVisibility(8);
        Runnable runnable = this.mAutoDismissRunnable;
        if (runnable != null) {
            removeCallbacks(runnable);
        }
    }

    @Deprecated
    public void setIconDrawable(Drawable drawable) {
        if (drawable == null) {
            this.mIconDrawableView.setVisibility(8);
            ((ViewGroup.MarginLayoutParams) this.mContentView.getLayoutParams()).setMarginStart(getContext().getResources().getDimensionPixelSize(R.dimen.coui_snack_bar_child_margin_horizontal_no_icon_start));
        } else {
            this.mIconDrawableView.setVisibility(0);
            this.mIconDrawableView.setImageDrawable(drawable);
            ((ViewGroup.MarginLayoutParams) this.mContentView.getLayoutParams()).setMarginStart(this.DEFAULT_CONTENT_MARGIN_HORIZONTAL);
        }
    }

    public void setOnAction(String str, final View.OnClickListener onClickListener) {
        if (TextUtils.isEmpty(str)) {
            this.mActionView.setVisibility(8);
            this.mActionView.setOnClickListener(null);
            Runnable runnable = this.mAutoDismissRunnable;
            if (runnable != null) {
                removeCallbacks(runnable);
                return;
            }
            return;
        }
        this.mActionView.setVisibility(0);
        setActionText(str);
        if (onClickListener != null) {
            COUITextViewCompatUtil.setPressRippleDrawable(this.mActionView);
            this.mActionView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onClickListener.onClick(view);
                    COUISnackBar cOUISnackBar = COUISnackBar.this;
                    cOUISnackBar.dismiss(cOUISnackBar.mWithoutAnima);
                }
            });
        }
    }

    public static COUISnackBar make(View view, String str, int i2, int i6) {
        return make(view.getContext(), view, str, i2, i6);
    }

    public static COUISnackBar make(Context context, View view, String str, int i2, int i6) {
        ViewGroup viewGroupFindSuitableParent = findSuitableParent(view);
        if (viewGroupFindSuitableParent != null) {
            TypedValue typedValue = new TypedValue();
            if (!context.getTheme().resolveAttribute(R.attr.couiColorSurfaceTop, typedValue, true) || !context.getTheme().resolveAttribute(R.attr.couiColorPrimaryNeutral, typedValue, true)) {
                ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(context, R.style.Theme_COUI_Main);
                Log.e(TAG, "Expected theme to define couiColorSurfaceTop and couiColorPrimaryNeutral.");
                context = contextThemeWrapper;
            }
            COUISnackBar cOUISnackBar = (COUISnackBar) LayoutInflater.from(context).inflate(R.layout.coui_snack_bar_show_layout, viewGroupFindSuitableParent, false);
            cOUISnackBar.setContentText(str);
            cOUISnackBar.setDuration(i2);
            cOUISnackBar.setParent(viewGroupFindSuitableParent);
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) cOUISnackBar.getLayoutParams();
            marginLayoutParams.bottomMargin = i6;
            mCOUISnackBarBottomMargin = i6;
            cOUISnackBar.setTranslationY(cOUISnackBar.getHeight() + i6);
            boolean z6 = false;
            for (int i10 = 0; i10 < viewGroupFindSuitableParent.getChildCount(); i10++) {
                if (viewGroupFindSuitableParent.getChildAt(i10) instanceof COUISnackBar) {
                    z6 = viewGroupFindSuitableParent.getChildAt(i10).getVisibility() != 8;
                }
            }
            if (!z6) {
                viewGroupFindSuitableParent.addView(cOUISnackBar, marginLayoutParams);
            }
            return cOUISnackBar;
        }
        throw new IllegalArgumentException("No suitable parent found from the given view. Please provide a valid view.");
    }

    public void dismiss(boolean z6) {
        if (z6) {
            COUISpringAnimation cOUISpringAnimation = this.mSpringAnimation;
            if (cOUISpringAnimation != null && cOUISpringAnimation.isRunning() && !this.mIsEntering) {
                this.mSpringAnimation.cancel();
            }
            Runnable runnable = this.mAutoDismissRunnable;
            if (runnable != null) {
                removeCallbacks(runnable);
            }
            dismissView();
            return;
        }
        dismiss();
    }

    public COUISnackBar(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.DEFAULT_ACTION_TEXT_MAX_WIDTH = getResources().getDimensionPixelSize(R.dimen.coui_snack_bar_action_max_width);
        this.DEFAULT_CONTENT_MARGIN_VERTICAL = getResources().getDimensionPixelSize(R.dimen.coui_snack_bar_child_margin_vertical);
        this.DEFAULT_CONTENT_MARGIN_HORIZONTAL = getResources().getDimensionPixelSize(R.dimen.coui_snack_bar_child_margin_horizontal_start);
        this.DEFAULT_ACTION_MARGIN_VERTICAL = getResources().getDimensionPixelSize(R.dimen.coui_snack_bar_action_margin_vertical);
        this.DEFAULT_CONTEXT_MARGIN_START_WITH_ICON = getResources().getDimensionPixelSize(R.dimen.coui_snack_bar_context_margin_start_with_icon);
        this.DEFAULT_ICON_WIDTH = getResources().getDimensionPixelSize(R.dimen.coui_snack_bar_icon_width);
        this.DEFAULT_ACTION_MARGIN_TOP_HORIZONTAL = getResources().getDimensionPixelSize(R.dimen.coui_snack_bar_action_margin_top_horizontal);
        this.DEFAULT_ACTION_MARGIN_TOP_HORIZONTAL_TINY = getResources().getDimensionPixelSize(R.dimen.coui_snack_bar_action_margin_top_horizontal_tiny);
        this.DEFAULT_ACTION_MARGIN_HORIZONTAL_START = getResources().getDimensionPixelSize(R.dimen.coui_snack_bar_action_margin_horizontal_start);
        this.DEFAULT_ACTION_MARGIN_HORIZONTAL_END = getResources().getDimensionPixelSize(R.dimen.coui_snack_bar_action_margin_horizontal_end);
        this.mWithoutAnima = false;
        this.mRect = new Rect();
        this.mResponsiveUIModel = new ResponsiveUIModel(getContext(), 0, 0);
        this.mIsDefaultRadius = true;
        this.isAdjustLayout = true;
        this.mIsSupportSmoothRoundCorner = false;
        this.mSnackBarAnimationProgress = 0.0f;
        this.mLastLayoutType = -1;
        this.mSnackBarProperty = new FloatPropertyCompat<Float>("snackBarProperty") {
            @Override
            public float getValue(Float f2) {
                return COUISnackBar.this.mSnackBarAnimationProgress;
            }

            @Override
            public void setValue(Float f2, float f10) {
                COUISnackBar.this.setSnackBarProgress(f10);
            }
        };
        initCOUISnackBar(context, attributeSet);
    }
}
