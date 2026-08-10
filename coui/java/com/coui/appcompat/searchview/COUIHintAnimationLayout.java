package com.coui.appcompat.searchview;


import com.coui.appcompat.R;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.coui.appcompat.animation.COUIEaseInterpolator;
import com.coui.appcompat.contextutil.COUIContextUtil;
import com.google.android.material.internal.TextWatcherAdapter;
import java.util.ArrayList;
import java.util.List;


public class COUIHintAnimationLayout extends FrameLayout {
    private static final int DURATION_TIME = 600;
    private static final int INTERVAL_TIME = 3000;
    private static final int IN_DELAY_TIME = 150;
    private static final String TAG = "COUIHintAnimationLayout";
    boolean mAnimationIsWorking;
    private AnimatorSet mAnimatorSetIn;
    private AnimatorSet mAnimatorSetOut;
    private COUIHintAnimationChangeListener mCOUIHintAnimationChangeListener;
    private Runnable mChangeHintRunnable;
    private int mCouiSearchBarAnimationTranslateExtra;
    private int mCurrentHintIndex;
    private TextView mCurrentHintTextView;
    private int mCurrentPlayTime;
    private ObjectAnimator mFadeIn;
    private ObjectAnimator mFadeOut;
    private List<String> mHintStrings;
    private TextView mHintTextViewFirst;
    private TextView mHintTextViewThen;
    private ObjectAnimator mMoveIn;
    private ObjectAnimator mMoveOut;
    boolean mNeedRePlay;
    private boolean mNeedStopAnimation;
    private int mRepeatCount;
    private EditText mSearchEditText;
    private String mTempQueryHint;
    private static final TimeInterpolator TRANSLATEINTERPOLATOR = androidx.core.view.animation.PathInterpolatorCompat.create(0.3f, 0.0f, 0.2f, 1.0f);
    private static final COUIEaseInterpolator EASEINTERPOLATOR = new COUIEaseInterpolator();

    public interface COUIHintAnimationChangeListener {
        void hintAnimationChange(int i2, String str, TextView textView);
    }

    public COUIHintAnimationLayout(Context context) {
        this(context, null);
    }

    private boolean animationIsRunning() {
        AnimatorSet animatorSet;
        AnimatorSet animatorSet2 = this.mAnimatorSetIn;
        return (animatorSet2 != null && animatorSet2.isRunning()) || ((animatorSet = this.mAnimatorSetOut) != null && animatorSet.isRunning());
    }


    public void applyHintChangeAnimation(String str) {
        if (this.mCurrentHintTextView == null) {
            return;
        }
        int i2 = this.mCurrentPlayTime + 1;
        this.mCurrentPlayTime = i2;
        int i6 = this.mRepeatCount;
        if (i6 != -1 && i2 > i6) {
            pauseHintsAnimation();
            return;
        }
        this.mTempQueryHint = str;
        int measuredHeight = ((getMeasuredHeight() - this.mCurrentHintTextView.getLineHeight()) / 2) + this.mCouiSearchBarAnimationTranslateExtra;
        if (this.mAnimatorSetOut == null || this.mAnimatorSetIn == null) {
            ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this.mCurrentHintTextView, "translationY", 0.0f, -measuredHeight);
            this.mMoveOut = objectAnimatorOfFloat;
            TimeInterpolator timeInterpolator = TRANSLATEINTERPOLATOR;
            objectAnimatorOfFloat.setInterpolator(timeInterpolator);
            ObjectAnimator objectAnimatorOfFloat2 = ObjectAnimator.ofFloat(this.mCurrentHintTextView, "alpha", 1.0f, 0.0f);
            this.mFadeOut = objectAnimatorOfFloat2;
            COUIEaseInterpolator cOUIEaseInterpolator = EASEINTERPOLATOR;
            objectAnimatorOfFloat2.setInterpolator(cOUIEaseInterpolator);
            AnimatorSet animatorSet = new AnimatorSet();
            this.mAnimatorSetOut = animatorSet;
            animatorSet.playTogether(this.mMoveOut, this.mFadeOut);
            this.mAnimatorSetOut.setDuration(600L);
            ObjectAnimator objectAnimatorOfFloat3 = ObjectAnimator.ofFloat(getNextHintTextView(), "translationY", measuredHeight, 0.0f);
            this.mMoveIn = objectAnimatorOfFloat3;
            objectAnimatorOfFloat3.setInterpolator(timeInterpolator);
            ObjectAnimator objectAnimatorOfFloat4 = ObjectAnimator.ofFloat(getNextHintTextView(), "alpha", 0.0f, 1.0f);
            this.mFadeIn = objectAnimatorOfFloat4;
            objectAnimatorOfFloat4.setInterpolator(cOUIEaseInterpolator);
            AnimatorSet animatorSet2 = new AnimatorSet();
            this.mAnimatorSetIn = animatorSet2;
            animatorSet2.playTogether(this.mMoveIn, this.mFadeIn);
            this.mAnimatorSetIn.setDuration(600L);
            this.mAnimatorSetIn.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    super.onAnimationEnd(animator);
                    COUIHintAnimationLayout cOUIHintAnimationLayout = COUIHintAnimationLayout.this;
                    cOUIHintAnimationLayout.mCurrentHintTextView = cOUIHintAnimationLayout.getNextHintTextView();
                    if (COUIHintAnimationLayout.this.mNeedStopAnimation) {
                        COUIHintAnimationLayout.this.pauseHintsAnimation();
                        COUIHintAnimationLayout.this.mNeedStopAnimation = false;
                    }
                }

                @Override
                public void onAnimationStart(Animator animator) {
                    super.onAnimationStart(animator);
                    COUIHintAnimationLayout.this.getNextHintTextView().setText(COUIHintAnimationLayout.this.mTempQueryHint);
                    COUIHintAnimationLayout.this.getNextHintTextView().setVisibility(0);
                }
            });
        } else {
            this.mMoveOut.setTarget(this.mCurrentHintTextView);
            this.mFadeOut.setTarget(this.mCurrentHintTextView);
            this.mMoveIn.setTarget(getNextHintTextView());
            this.mFadeIn.setTarget(getNextHintTextView());
        }
        postDelayed(new Runnable() {
            @Override
            public void run() {
                COUIHintAnimationLayout.this.mAnimatorSetIn.start();
            }
        }, 150L);
        this.mAnimatorSetOut.start();
        COUIHintAnimationChangeListener cOUIHintAnimationChangeListener = this.mCOUIHintAnimationChangeListener;
        if (cOUIHintAnimationChangeListener != null) {
            cOUIHintAnimationChangeListener.hintAnimationChange(this.mCurrentHintIndex, str, this.mCurrentHintTextView);
        }
    }


    public void cancelAnimation() {
        AnimatorSet animatorSet = this.mAnimatorSetIn;
        if (animatorSet != null && animatorSet.isRunning()) {
            this.mAnimatorSetIn.cancel();
        }
        AnimatorSet animatorSet2 = this.mAnimatorSetOut;
        if (animatorSet2 == null || !animatorSet2.isRunning()) {
            return;
        }
        this.mAnimatorSetOut.cancel();
    }


    public TextView getNextHintTextView() {
        TextView textView = this.mCurrentHintTextView;
        TextView textView2 = this.mHintTextViewFirst;
        return textView == textView2 ? this.mHintTextViewThen : textView2;
    }

    private void initHintAnimationTextView() {
        if (this.mHintTextViewFirst == null && this.mHintTextViewThen == null) {
            Context context = getContext();
            int i2 = R.style.Widget_COUI_EditText_SearchViewStyle_HintText;
            this.mHintTextViewFirst = new TextView(new ContextThemeWrapper(context, i2), null);
            this.mHintTextViewThen = new TextView(new ContextThemeWrapper(getContext(), i2), null);
            this.mHintTextViewFirst.setImportantForAccessibility(2);
            this.mHintTextViewThen.setImportantForAccessibility(2);
            TextView textView = this.mHintTextViewFirst;
            int i6 = R.style.couiTextAppearanceBodyL;
            textView.setTextAppearance(i6);
            this.mHintTextViewThen.setTextAppearance(i6);
            TextView textView2 = this.mHintTextViewFirst;
            Context context2 = getContext();
            int i10 = R.attr.couiColorLabelSecondary;
            textView2.setTextColor(COUIContextUtil.getAttrColor(context2, i10));
            this.mHintTextViewThen.setTextColor(COUIContextUtil.getAttrColor(getContext(), i10));
            this.mHintTextViewFirst.setId(R.id.coui_hint_text_view_first);
            this.mHintTextViewThen.setId(R.id.coui_hint_text_view_then);
            addView(this.mHintTextViewFirst);
            addView(this.mHintTextViewThen);
        }
    }

    private void initTextHintAnimation() {
        if (this.mChangeHintRunnable == null) {
            this.mHintStrings = new ArrayList();
            this.mChangeHintRunnable = new Runnable() {
                @Override
                public void run() {
                    if (COUIHintAnimationLayout.this.mHintStrings.isEmpty()) {
                        return;
                    }
                    COUIHintAnimationLayout cOUIHintAnimationLayout = COUIHintAnimationLayout.this;
                    cOUIHintAnimationLayout.mCurrentHintIndex = (cOUIHintAnimationLayout.mCurrentHintIndex + 1) % COUIHintAnimationLayout.this.mHintStrings.size();
                    COUIHintAnimationLayout cOUIHintAnimationLayout2 = COUIHintAnimationLayout.this;
                    if (cOUIHintAnimationLayout2.mAnimationIsWorking) {
                        cOUIHintAnimationLayout2.applyHintChangeAnimation((String) cOUIHintAnimationLayout2.mHintStrings.get(COUIHintAnimationLayout.this.mCurrentHintIndex));
                    }
                    COUIHintAnimationLayout.this.postDelayed(this, 3000L);
                }
            };
            this.mSearchEditText.addTextChangedListener(new TextWatcherAdapter() {
                @Override
                public void afterTextChanged(Editable editable) {
                    if (TextUtils.isEmpty(COUIHintAnimationLayout.this.mSearchEditText.getText().toString())) {
                        if (TextUtils.isEmpty(COUIHintAnimationLayout.this.mTempQueryHint)) {
                            return;
                        }
                        COUIHintAnimationLayout.this.mCurrentHintTextView.setText(COUIHintAnimationLayout.this.mTempQueryHint);
                        COUIHintAnimationLayout.this.mCurrentHintTextView.setVisibility(0);
                        COUIHintAnimationLayout.this.getNextHintTextView().setVisibility(8);
                        return;
                    }
                    COUIHintAnimationLayout cOUIHintAnimationLayout = COUIHintAnimationLayout.this;
                    cOUIHintAnimationLayout.removeCallbacks(cOUIHintAnimationLayout.mChangeHintRunnable);
                    COUIHintAnimationLayout.this.mHintTextViewFirst.setVisibility(8);
                    COUIHintAnimationLayout.this.mHintTextViewThen.setVisibility(8);
                    COUIHintAnimationLayout.this.cancelAnimation();
                    COUIHintAnimationLayout.this.mAnimationIsWorking = false;
                }
            });
        }
    }

    private void resetHintTextView() {
        this.mHintTextViewFirst.setTranslationY(0.0f);
        this.mHintTextViewFirst.setAlpha(1.0f);
        this.mHintTextViewThen.setTranslationY(0.0f);
        this.mHintTextViewThen.setAlpha(1.0f);
    }

    public TextView getCurrentHintTextView() {
        return this.mCurrentHintTextView;
    }

    public List<String> getHintStrings() {
        return this.mHintStrings;
    }

    @Override
    public void onAttachedToWindow() {
        resumeHintsAnimation();
        super.onAttachedToWindow();
    }

    @Override
    public void onDetachedFromWindow() {
        pauseHintsAnimation();
        super.onDetachedFromWindow();
    }

    @Override
    public void onWindowVisibilityChanged(int i2) {
        super.onWindowVisibilityChanged(i2);
        if (i2 == 0) {
            if (this.mNeedRePlay) {
                resumeHintsAnimation();
                this.mNeedRePlay = false;
                return;
            }
            return;
        }
        if (this.mAnimationIsWorking) {
            pauseHintsAnimation();
            this.mNeedRePlay = true;
        }
    }

    public void pauseHintsAnimation() {
        List<String> list;
        removeCallbacks(this.mChangeHintRunnable);
        if (!this.mAnimationIsWorking || (list = this.mHintStrings) == null || list.size() == 0) {
            Log.e(TAG, "pauseHintsAnimation return");
            return;
        }
        if (animationIsRunning()) {
            this.mNeedStopAnimation = true;
            return;
        }
        resetHintTextView();
        this.mAnimationIsWorking = false;
        if (TextUtils.isEmpty(this.mTempQueryHint)) {
            this.mHintTextViewFirst.setVisibility(8);
            this.mHintTextViewThen.setVisibility(8);
        } else {
            this.mCurrentHintTextView.setText(this.mTempQueryHint);
            this.mCurrentHintTextView.setVisibility(0);
            getNextHintTextView().setVisibility(8);
        }
    }

    public void resumeHintsAnimation() {
        setHintsAnimation(this.mHintStrings);
    }

    public void setCOUIHintAnimationChangeListener(COUIHintAnimationChangeListener cOUIHintAnimationChangeListener) {
        this.mCOUIHintAnimationChangeListener = cOUIHintAnimationChangeListener;
    }

    public void setHintsAnimation(List<String> list) {
        if (list == null || list.size() == 0) {
            return;
        }
        if (this.mSearchEditText == null) {
            if (!(getChildAt(0) instanceof EditText)) {
                Log.e(TAG, "Before calling this method, you must ensure that there is an edittext object in the container:1, you can call setSearchEditText or add an edittext yourself, refer to COUISearchBar2, you can put an edittext object in xml ( Refer to coui_search_view_animated_support_layout)to use the related functions of this animation container");
                return;
            }
            this.mSearchEditText = (EditText) getChildAt(0);
        }
        if (!TextUtils.isEmpty(this.mSearchEditText.getText().toString())) {
            Log.e(TAG, "Setting hints animation content is invalid when the searchEdittext has a value");
            return;
        }
        initHintAnimationTextView();
        initTextHintAnimation();
        if (!this.mHintStrings.equals(list)) {
            this.mHintStrings.clear();
            this.mHintStrings.addAll(list);
        }
        if (this.mCurrentHintTextView == null) {
            this.mCurrentHintTextView = this.mHintTextViewFirst;
        }
        if (TextUtils.isEmpty(this.mTempQueryHint)) {
            this.mTempQueryHint = this.mHintStrings.get(this.mCurrentHintIndex);
        }
        this.mCurrentHintTextView.setText(this.mTempQueryHint);
        this.mCurrentHintTextView.setVisibility(0);
        removeCallbacks(this.mChangeHintRunnable);
        this.mSearchEditText.setHint("");
        postDelayed(this.mChangeHintRunnable, 3000L);
        this.mAnimationIsWorking = true;
    }

    public void setRepeatCount(int i2) {
        if (i2 <= 0) {
            Log.e(TAG, "RepeatCount must be greater than zero");
        } else {
            this.mRepeatCount = i2;
        }
    }

    public void setSearchEditText(EditText editText) {
        this.mSearchEditText = editText;
        if (getChildCount() == 0) {
            addView(this.mSearchEditText, new FrameLayout.LayoutParams(-1, -1));
        } else {
            Log.e(TAG, "setSearchEditText() can only be executed once");
        }
    }

    public void setTextSize(int i2) {
        TextView textView = this.mHintTextViewFirst;
        if (textView == null || this.mHintTextViewThen == null) {
            return;
        }
        float f2 = i2;
        textView.setTextSize(0, f2);
        this.mHintTextViewThen.setTextSize(0, f2);
    }

    public COUIHintAnimationLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mCurrentHintIndex = 0;
        this.mAnimationIsWorking = false;
        this.mNeedRePlay = false;
        this.mNeedStopAnimation = false;
        this.mRepeatCount = -1;
        this.mCurrentPlayTime = 0;
        this.mCouiSearchBarAnimationTranslateExtra = context.getResources().getDimensionPixelSize(R.dimen.coui_search_bar_animation_translate_extra);
    }
}
