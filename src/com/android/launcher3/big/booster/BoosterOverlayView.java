package com.android.launcher3.big.booster;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.dragndrop.DragLayer;

/**
 * CM Launcher Booster fullscreen overlay: diffuse rings + rocket lift → Optimized.
 */
public class BoosterOverlayView extends AbstractFloatingView {

    private static final int BG_START = Color.parseColor("#1A2A4A");
    private static final int BG_END = Color.parseColor("#4673D1");
    private static final long MASTER_DURATION_MS = 5000L;
    private static final long RESULT_FLIP_MS = 500L;
    private static final long INFO_EXIT_MS = 400L;

    private Launcher mLauncher;
    private DiffuseRingsView mDiffuse;
    private View mRocket;
    private ImageView mFinish;
    private View mInfoRoot;
    private View mResultRoot;
    private TextView mStatus;
    private AnimatorSet mAnim;

    public BoosterOverlayView(Context context) {
        this(context, null);
    }

    public BoosterOverlayView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BoosterOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
        setClickable(true);
        setFocusable(true);
        LayoutInflater.from(context).inflate(R.layout.booster_overlay, this, true);
        mDiffuse = findViewById(R.id.booster_diffuse);
        mRocket = findViewById(R.id.booster_rocket);
        mFinish = findViewById(R.id.booster_finish);
        mInfoRoot = findViewById(R.id.booster_info_root);
        mResultRoot = findViewById(R.id.booster_result_root);
        mStatus = findViewById(R.id.booster_status);
        findViewById(R.id.booster_close).setOnClickListener(v -> close(true));
        setBackgroundColor(BG_START);
    }

    public static void show(Launcher launcher) {
        closeOpenViews(launcher, false, TYPE_BOOSTER_OVERLAY);
        BoosterOverlayView overlay = new BoosterOverlayView(launcher);
        overlay.mLauncher = launcher;
        overlay.mIsOpen = true;
        DragLayer.LayoutParams lp = new DragLayer.LayoutParams(
                DragLayer.LayoutParams.MATCH_PARENT,
                DragLayer.LayoutParams.MATCH_PARENT);
        lp.gravity = Gravity.FILL;
        lp.ignoreInsets = true;
        launcher.getDragLayer().addView(overlay, lp);
        overlay.startAnimation();
    }

    private void startAnimation() {
        mRocket.setScaleX(0.6f);
        mRocket.setScaleY(0.6f);
        mRocket.setAlpha(0f);
        mRocket.setTranslationY(0f);
        mFinish.setVisibility(INVISIBLE);
        mFinish.setRotationY(-90f);
        mResultRoot.setVisibility(GONE);
        mInfoRoot.setVisibility(VISIBLE);
        mStatus.setText(R.string.memory_clean_running_animator);
        mDiffuse.start();

        AnimatorSet set = new AnimatorSet();
        mAnim = set;

        ObjectAnimator rocketInX = ObjectAnimator.ofFloat(mRocket, SCALE_X, 0.6f, 1f);
        ObjectAnimator rocketInY = ObjectAnimator.ofFloat(mRocket, SCALE_Y, 0.6f, 1f);
        ObjectAnimator rocketAlpha = ObjectAnimator.ofFloat(mRocket, ALPHA, 0f, 1f);
        AnimatorSet phase1 = new AnimatorSet();
        phase1.playTogether(rocketInX, rocketInY, rocketAlpha);
        phase1.setDuration(300);
        phase1.setInterpolator(new PathInterpolator(0.3f, 0f, 0.1f, 1f));

        float climb = -getResources().getDisplayMetrics().density * 120f;
        ObjectAnimator climbAnim = ObjectAnimator.ofFloat(mRocket, TRANSLATION_Y, 0f, climb);
        climbAnim.setDuration(1100);
        climbAnim.setStartDelay(300);
        climbAnim.setInterpolator(new AccelerateDecelerateInterpolator());

        ValueAnimator bg = ValueAnimator.ofObject(new ArgbEvaluator(), BG_START, BG_END);
        bg.addUpdateListener(a -> setBackgroundColor((int) a.getAnimatedValue()));
        bg.setDuration(MASTER_DURATION_MS);

        set.playTogether(phase1, climbAnim, bg);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                showResult();
            }
        });
        set.start();

        // Keep CM timing feel: flip to result near end if still running.
        postDelayed(() -> {
            if (mIsOpen && mResultRoot.getVisibility() != VISIBLE) {
                if (mAnim != null) {
                    mAnim.cancel();
                }
                showResult();
            }
        }, MASTER_DURATION_MS);
    }

    private void showResult() {
        mDiffuse.stop();
        mRocket.setVisibility(INVISIBLE);
        mFinish.setVisibility(VISIBLE);
        ObjectAnimator flip = ObjectAnimator.ofFloat(mFinish, ROTATION_Y, -90f, 0f);
        flip.setDuration(RESULT_FLIP_MS);
        flip.start();

        AnimatorSet exitInfo = new AnimatorSet();
        exitInfo.playTogether(
                ObjectAnimator.ofFloat(mInfoRoot, SCALE_X, 1f, 0.3f),
                ObjectAnimator.ofFloat(mInfoRoot, SCALE_Y, 1f, 0.3f),
                ObjectAnimator.ofFloat(mInfoRoot, ALPHA, 1f, 0f),
                ObjectAnimator.ofFloat(mInfoRoot, TRANSLATION_Y, 0f, -200f));
        exitInfo.setDuration(INFO_EXIT_MS);
        exitInfo.setStartDelay(200);
        exitInfo.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mInfoRoot.setVisibility(GONE);
                mResultRoot.setVisibility(VISIBLE);
                mResultRoot.setAlpha(0f);
                ObjectAnimator.ofFloat(mResultRoot, ALPHA, 0f, 1f).setDuration(250).start();
            }
        });
        exitInfo.start();
        setBackgroundColor(BG_END);
    }

    @Override
    protected void handleClose(boolean animate) {
        if (mAnim != null) {
            mAnim.cancel();
            mAnim = null;
        }
        mDiffuse.stop();
        mIsOpen = false;
        if (getParent() instanceof DragLayer) {
            ((DragLayer) getParent()).removeView(this);
        }
    }

    @Override
    protected boolean isOfType(int type) {
        return (type & TYPE_BOOSTER_OVERLAY) != 0;
    }

    @Override
    public void logActionCommand(int command) {
        // no-op
    }
}
