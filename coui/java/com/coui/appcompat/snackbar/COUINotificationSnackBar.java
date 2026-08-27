package com.coui.appcompat.snackbar;

import com.coui.appcompat.R;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Outline;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Property;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.PathInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import com.coui.appcompat.animation.COUIMoveEaseInterpolator;
import com.coui.appcompat.animation.dynamicanimation.COUIDynamicAnimation;
import com.coui.appcompat.animation.dynamicanimation.COUISpringAnimation;
import com.coui.appcompat.button.COUIButton;
import com.coui.appcompat.contextutil.COUIContextUtil;
import com.coui.appcompat.roundcorner.RoundCornerUtil;
import com.coui.appcompat.snackbar.COUISnackBar;
import com.coui.appcompat.uiutil.ShadowUtils;
import com.oplus.graphics.OplusOutline;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


public class COUINotificationSnackBar extends COUISnackBar {
    private static final int MAX_DURATION = 10000;
    private static final int MIN_DURATION = 4000;
    private static final PathInterpolator MOVE_EASE_INTERPOLATOR = new COUIMoveEaseInterpolator();
    private static final int SLIDING_LIMIT_VALUE = 8;
    private static final float SLIDING_SPEED = 1.75f;
    private static final int SWIPED_INDEX_X = 1;
    private static final int SWIPED_INDEX_Y = 2;
    private static final String TAG = "COUINotificationSnackBar";
    private View.OnClickListener mButtonClickListener;
    private COUIButton mButtonTv;
    private Drawable mCloseDrawable;
    private ImageView mCloseIv;
    private View.OnClickListener mCloseIvClickListener;
    private COUISpringAnimation mCouiSpringAnimationX;
    private COUISpringAnimation mCouiSpringAnimationY;
    private int mHorizontalImageHeight;
    private int mHorizontalImageWidth;
    private int mInterceptX;
    private int mInterceptY;
    private boolean mIsSupportSmoothRoundCorner;
    private int mLastX;
    private int mLastY;
    private ImageView mNotificationIcon;
    private int mScreenWidth;
    private int mSquareImageWidth;
    private TextView mSubContentTv;
    private int mSwipedIndex;
    private int mVerticalImageHeight;
    private int mVerticalImageWidth;

    @Target({ElementType.TYPE, ElementType.PARAMETER, ElementType.FIELD})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ImageType {
        public static final int Horizontal = 2;
        public static final int Square = 0;
        public static final int Vertical = 1;
    }

    public COUINotificationSnackBar(Context context) {
        super(context);
    }

    private boolean checkSwipedFarEnoughByX() {
        return Math.abs(getTranslationX()) > ((float) (getMeasuredWidth() / 8));
    }

    private boolean checkSwipedFarEnoughByY() {
        float translationY = getTranslationY();
        return Math.abs(translationY) > ((float) (getMeasuredWidth() / 8)) && translationY > 0.0f;
    }

    private ObjectAnimator createTranslationAnimation(View view, float f2, Property<View, Float> property) {
        return ObjectAnimator.ofFloat(view, property, f2);
    }

    private COUISpringAnimation getViewSpringTranslationAnimator(View view, float f2, COUIDynamicAnimation.ViewProperty viewProperty) {
        COUISpringAnimation cOUISpringAnimation = new COUISpringAnimation(view, viewProperty, f2);
        cOUISpringAnimation.getSpring().setBounce(0.0f);
        cOUISpringAnimation.getSpring().setResponse(0.35f);
        return cOUISpringAnimation;
    }

    private Animator getViewTranslationAnimator(View view, float f2, int i2, Property<View, Float> property) {
        ObjectAnimator objectAnimatorCreateTranslationAnimation = createTranslationAnimation(view, f2, property);
        objectAnimatorCreateTranslationAnimation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                COUINotificationSnackBar.this.dismiss();
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }

            @Override
            public void onAnimationStart(Animator animator) {
            }
        });
        objectAnimatorCreateTranslationAnimation.setDuration(i2);
        objectAnimatorCreateTranslationAnimation.setInterpolator(MOVE_EASE_INTERPOLATOR);
        return objectAnimatorCreateTranslationAnimation;
    }

    @SuppressLint({"UseCompatLoadingForDrawables"})
    private void initResource() {
        this.mScreenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        this.mSquareImageWidth = getContext().getResources().getDimensionPixelSize(R.dimen.coui_notification_snack_bar_icon_width);
        this.mHorizontalImageWidth = getContext().getResources().getDimensionPixelSize(R.dimen.coui_notification_snack_bar_horizontal_icon_width);
        this.mHorizontalImageHeight = getContext().getResources().getDimensionPixelSize(R.dimen.coui_notification_snack_bar_horizontal_icon_height);
        this.mVerticalImageWidth = getContext().getResources().getDimensionPixelSize(R.dimen.coui_notification_snack_bar_vertical_icon_width);
        this.mVerticalImageHeight = getContext().getResources().getDimensionPixelSize(R.dimen.coui_notification_snack_bar_vertical_icon_height);
        Drawable drawable = getContext().getResources().getDrawable(R.drawable.coui_menu_ic_cancel_normal, getContext().getTheme());
        this.mCloseDrawable = drawable;
        drawable.setColorFilter(COUIContextUtil.getAttrColor(getContext(), R.attr.couiColorLabelTertiary, 0), PorterDuff.Mode.SRC_IN);
    }


    public void lambda$setOnClick$0(View view) {
        View.OnClickListener onClickListener = this.mCloseIvClickListener;
        if (onClickListener != null) {
            onClickListener.onClick(this.mCloseIv);
        }
        dismiss();
    }


    public void lambda$setOnClick$1(View view) {
        View.OnClickListener onClickListener = this.mButtonClickListener;
        if (onClickListener != null) {
            onClickListener.onClick(this.mButtonTv);
        }
    }

    public static COUINotificationSnackBar make(View view, String str, String str2, int i2) {
        return make(view.getContext(), view, str, str2, i2);
    }

    private void setButtonText(String str) {
        if (this.mButtonTv == null || TextUtils.isEmpty(str)) {
            return;
        }
        this.mButtonTv.setText(str);
        this.mButtonTv.setVisibility(0);
    }

    private void setCloseIcon() {
        ImageView imageView = this.mCloseIv;
        if (imageView == null) {
            return;
        }
        imageView.setImageDrawable(this.mCloseDrawable);
        this.mCloseIv.setVisibility(0);
    }

    @SuppressLint({"ClickableViewAccessibility"})
    private void setOnClick() {
        ImageView imageView = this.mCloseIv;
        if (imageView != null) {
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public final void onClick(View view) {
                    COUINotificationSnackBar.this.lambda$setOnClick$0(view);
                }
            });
        }
        COUIButton cOUIButton = this.mButtonTv;
        if (cOUIButton != null) {
            cOUIButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public final void onClick(View view) {
                    COUINotificationSnackBar.this.lambda$setOnClick$1(view);
                }
            });
        }
    }

    private void startDismissAnimationX() {
        float translationX = getTranslationX();
        int measuredWidth = ((this.mScreenWidth - getMeasuredWidth()) / 2) + getMeasuredWidth();
        float f2 = measuredWidth;
        int iAbs = (int) ((f2 - Math.abs(translationX)) / SLIDING_SPEED);
        if (translationX < 0.0f) {
            f2 = -measuredWidth;
        }
        ((ObjectAnimator) getViewTranslationAnimator(this, f2, iAbs, View.TRANSLATION_X)).start();
    }

    private void startDismissAnimationY() {
        float bottom = getBottom() + getMeasuredHeight();
        ((ObjectAnimator) getViewTranslationAnimator(this, bottom, (int) (bottom / SLIDING_SPEED), View.TRANSLATION_Y)).start();
    }

    private void startOriginalAnimationX() {
        if (this.mCouiSpringAnimationX == null) {
            this.mCouiSpringAnimationX = getViewSpringTranslationAnimator(this, 0.0f, COUIDynamicAnimation.TRANSLATION_X);
        }
        this.mCouiSpringAnimationX.start();
    }

    private void startOriginalAnimationY() {
        if (this.mCouiSpringAnimationY == null) {
            this.mCouiSpringAnimationY = getViewSpringTranslationAnimator(this, 0.0f, COUIDynamicAnimation.TRANSLATION_Y);
        }
        this.mCouiSpringAnimationY.start();
    }

    public COUIButton getNotificationButton() {
        return this.mButtonTv;
    }

    public ViewGroup getSnackBarLayout() {
        return this.mSnackBarLayout;
    }

    public TextView getSubContentView() {
        return this.mSubContentTv;
    }

    @Override
    @SuppressLint({"LongLogTag"})
    public void initCOUISnackBar(final Context context, AttributeSet attributeSet) {
        initResource();
        View viewInflate = View.inflate(context, R.layout.coui_notification_snack_bar_item, this);
        this.mRootView = viewInflate;
        this.mSnackBarLayout = (ViewGroup) viewInflate.findViewById(R.id.snack_bar);
        this.mContentView = (TextView) this.mRootView.findViewById(R.id.tv_snack_bar_content);
        this.mActionView = (TextView) this.mRootView.findViewById(R.id.tv_snack_bar_action);
        this.mIconDrawableView = (ImageView) this.mRootView.findViewById(R.id.iv_snack_bar_icon);
        this.mCloseIv = (ImageView) this.mRootView.findViewById(R.id.iv_notification_snack_bar_close);
        this.mButtonTv = (COUIButton) this.mRootView.findViewById(R.id.bt_notification_snack_bar);
        this.mNotificationIcon = (ImageView) this.mRootView.findViewById(R.id.iv_notification_snack_bar_icon);
        this.mSubContentTv = (TextView) this.mRootView.findViewById(R.id.tv_snack_bar_sub_content);
        COUISnackBar.mCOUISnackBarBottomMargin = new ViewGroup.MarginLayoutParams(context, attributeSet).bottomMargin;
        setVisibility(8);
        this.mAutoDismissRunnable = new COUISnackBar.AutoDismissRunnable();
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.COUISnackBar, 0, 0);
        try {
            try {
                int i2 = R.styleable.COUISnackBar_defaultSnackBarContentText;
                if (typedArrayObtainStyledAttributes.getString(i2) != null) {
                    setContentText(typedArrayObtainStyledAttributes.getString(i2));
                    setDuration(typedArrayObtainStyledAttributes.getInt(R.styleable.COUISnackBar_snackBarDisappearTime, 0));
                }
            } catch (Exception e2) {
                Log.e(TAG, "Failure setting COUINotificationSnackBar " + e2.getMessage());
            }
            this.mIsSupportSmoothRoundCorner = RoundCornerUtil.isVersionSupport();
            ViewOutlineProvider viewOutlineProvider = new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    if (!COUINotificationSnackBar.this.mIsSupportSmoothRoundCorner) {
                        outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), COUIContextUtil.getAttrDimens(context, R.attr.couiRoundCornerL));
                    } else {
                        new OplusOutline(outline).setSmoothRoundRect(0, 0, view.getWidth(), view.getHeight(), COUIContextUtil.getAttrDimens(context, R.attr.couiRoundCornerLRadius), COUIContextUtil.getAttrFloat(context, R.attr.couiRoundCornerLWeight));
                    }
                }
            };
            setOnClick();
            this.mSnackBarLayout.setOutlineProvider(viewOutlineProvider);
            this.mSnackBarLayout.setClipToOutline(true);
            ShadowUtils.setElevationToView(this.mSnackBarLayout, 2, COUIContextUtil.getAttrDimens(context, R.attr.couiRoundCornerL), context.getResources().getDimensionPixelOffset(R.dimen.support_shadow_size_level_four), getContext().getResources().getColor(R.color.coui_snack_bar_background_shadow_color));
        } finally {
            typedArrayObtainStyledAttributes.recycle();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        int rawY = (int) motionEvent.getRawY();
        int rawX = (int) motionEvent.getRawX();
        int action = motionEvent.getAction();
        if (action == 0) {
            this.mInterceptX = rawX;
            this.mInterceptY = rawY;
            return false;
        }
        if (action != 2) {
            return false;
        }
        this.mLastX = rawX;
        this.mLastY = rawY;
        if (Math.abs(rawX - this.mInterceptX) <= 0 && Math.abs(this.mLastY - this.mInterceptY) <= 0) {
            return false;
        }
        this.mSwipedIndex = -1;
        return true;
    }

    @Override
    public void onLayout(boolean z6, int i2, int i6, int i10, int i11) {
        this.isAdjustLayout = false;
        super.onLayout(z6, i2, i6, i10, i11);
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent motionEvent) {
        int rawX = (int) motionEvent.getRawX();
        int rawY = (int) motionEvent.getRawY();
        int deltaX = rawX - this.mLastX;
        int deltaY = rawY - this.mLastY;
        int action = motionEvent.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            this.mSwipedIndex = -1;
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (this.mSwipedIndex == -1 && (deltaX != 0 || deltaY != 0)) {
                this.mSwipedIndex = Math.abs(deltaX) > Math.abs(deltaY) ? SWIPED_INDEX_X : SWIPED_INDEX_Y;
            }
            if (this.mSwipedIndex != -1) {
                if (this.mSwipedIndex == SWIPED_INDEX_X) {
                    setTranslationX(getTranslationX() + deltaX);
                } else {
                    float translationY = getTranslationY() + deltaY;
                    int limit = getMeasuredWidth() / SLIDING_LIMIT_VALUE;
                    if (translationY < 0.0f) {
                        translationY = Math.max(-limit, translationY);
                    }
                    setTranslationY(translationY);
                }
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (this.mSwipedIndex == SWIPED_INDEX_X) {
                if (checkSwipedFarEnoughByX()) {
                    startDismissAnimationX();
                } else {
                    startOriginalAnimationX();
                }
            } else if (checkSwipedFarEnoughByY()) {
                startDismissAnimationY();
            } else {
                startOriginalAnimationY();
            }
        }
        this.mLastX = rawX;
        this.mLastY = rawY;
        return super.onTouchEvent(motionEvent);
    }

    public void setButtonClickListener(View.OnClickListener onClickListener) {
        this.mButtonClickListener = onClickListener;
    }

    public void setCloseButtonClickListener(View.OnClickListener onClickListener) {
        this.mCloseIvClickListener = onClickListener;
    }

    public void setNotificationIcon(Drawable drawable, int i2) {
        ImageView imageView = this.mNotificationIcon;
        if (imageView == null || drawable == null) {
            return;
        }
        ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
        if (i2 == 0) {
            int i6 = this.mSquareImageWidth;
            layoutParams.width = i6;
            layoutParams.height = i6;
        } else if (i2 == 1) {
            layoutParams.width = this.mVerticalImageWidth;
            layoutParams.height = this.mVerticalImageHeight;
        } else if (i2 == 2) {
            layoutParams.width = this.mHorizontalImageWidth;
            layoutParams.height = this.mHorizontalImageHeight;
        }
        this.mNotificationIcon.setLayoutParams(layoutParams);
        this.mNotificationIcon.setImageDrawable(drawable);
        this.mNotificationIcon.setVisibility(0);
    }

    public void setSubContent(String str) {
        if (this.mSubContentTv == null || TextUtils.isEmpty(str)) {
            return;
        }
        this.mSubContentTv.setText(str);
        this.mSubContentTv.setVisibility(0);
    }

    public COUINotificationSnackBar(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public static COUINotificationSnackBar make(Context context, View view, String str, String str2, int i2) {
        return make(context, view, str, str2, i2, context.getResources().getDimensionPixelSize(R.dimen.coui_snack_bar_margin_bottom));
    }

    public static COUINotificationSnackBar make(View view, String str, String str2, int i2, int i6) {
        return make(view.getContext(), view, str, str2, i2, i6);
    }

    public static COUINotificationSnackBar make(Context context, View view, String str, String str2, int i2, int i6) {
        ViewGroup viewGroupFindSuitableParent = COUISnackBar.findSuitableParent(view);
        if (viewGroupFindSuitableParent != null) {
            if (!TextUtils.isEmpty(str) && !TextUtils.isEmpty(str2)) {
                int iMin = Math.min(Math.max(i2, MIN_DURATION), MAX_DURATION);
                TypedValue typedValue = new TypedValue();
                if (!context.getTheme().resolveAttribute(R.attr.couiColorSurfaceTop, typedValue, true) || !context.getTheme().resolveAttribute(R.attr.couiColorPrimaryNeutral, typedValue, true)) {
                    context = new ContextThemeWrapper(context, R.style.Theme_COUI_Main);
                }
                COUINotificationSnackBar cOUINotificationSnackBar = (COUINotificationSnackBar) LayoutInflater.from(context).inflate(R.layout.coui_notification_snack_bar_show_layout, viewGroupFindSuitableParent, false);
                cOUINotificationSnackBar.setContentText(str);
                cOUINotificationSnackBar.setDuration(iMin);
                cOUINotificationSnackBar.setParent(viewGroupFindSuitableParent);
                cOUINotificationSnackBar.setButtonText(str2);
                cOUINotificationSnackBar.setCloseIcon();
                ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) cOUINotificationSnackBar.getLayoutParams();
                marginLayoutParams.bottomMargin = i6;
                cOUINotificationSnackBar.setTranslationY(cOUINotificationSnackBar.getHeight() + i6);
                boolean z6 = false;
                for (int i10 = 0; i10 < viewGroupFindSuitableParent.getChildCount(); i10++) {
                    if (viewGroupFindSuitableParent.getChildAt(i10) instanceof COUISnackBar) {
                        z6 = viewGroupFindSuitableParent.getChildAt(i10).getVisibility() != 8;
                    }
                }
                if (!z6) {
                    viewGroupFindSuitableParent.addView(cOUINotificationSnackBar, marginLayoutParams);
                }
                return cOUINotificationSnackBar;
            }
            throw new IllegalArgumentException("Content text and button text can not be empty");
        }
        throw new IllegalArgumentException("No suitable parent found from the given view. Please provide a valid view.");
    }

    @SuppressLint({"UseCompatLoadingForDrawables"})
    public void setNotificationIcon(int i2, int i6) {
        setNotificationIcon(getResources().getDrawable(i2, getContext().getTheme()), i6);
    }
}
