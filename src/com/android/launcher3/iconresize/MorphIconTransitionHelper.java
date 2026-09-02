package com.android.launcher3.iconresize;

import android.content.ComponentName;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.views.ActivityContext;

import java.lang.ref.WeakReference;

/**
 * Spring morph between span presets when the resize handle changes icon size (Oppo-style).
 */
public final class MorphIconTransitionHelper {

    private static final float SPRING_STIFFNESS = 680f;
    private static final float SPRING_DAMPING = 0.82f;

    private static TransitionState sActive;

    private MorphIconTransitionHelper() {}

    private static final class TransitionState {
        final WeakReference<BubbleTextView> viewRef;
        final MorphTransitionDrawable drawable;
        @Nullable SpringAnimation spring;

        TransitionState(BubbleTextView view, MorphTransitionDrawable drawable) {
            viewRef = new WeakReference<>(view);
            this.drawable = drawable;
        }
    }

    @Nullable
    public static Drawable getActiveDrawable(BubbleTextView view) {
        TransitionState state = sActive;
        if (state != null && state.viewRef.get() == view) {
            return state.drawable;
        }
        return null;
    }

    /** Unwraps morph wrappers so preview can rebuild at arbitrary bounds. */
    public static Drawable unwrapBaseIcon(Drawable icon) {
        return MorphTransitionDrawable.mutateInner(icon);
    }

    public static boolean isAnimating(BubbleTextView view) {
        return getActiveDrawable(view) != null;
    }

    public static void cancel(BubbleTextView view) {
        TransitionState state = sActive;
        if (state != null && state.viewRef.get() == view) {
            if (state.spring != null) {
                state.spring.cancel();
            }
            sActive = null;
            view.refreshWorkspaceIconDisplay();
        }
    }

    /**
     * Morph plate from {@code fromSpan} to {@code toSpan} after grid layout has been updated.
     */
    public static void animateSpanChange(BubbleTextView view, int fromSpanX, int fromSpanY,
            int toSpanX, int toSpanY, boolean commit) {
        if (fromSpanX == toSpanX && fromSpanY == toSpanY) {
            return;
        }
        cancel(view);

        Drawable base = MorphTransitionDrawable.mutateInner(view.getIcon());
        ComponentName cn = null;
        Object tag = view.getTag();
        if (tag instanceof com.android.launcher3.model.data.ItemInfo info) {
            cn = info.getTargetComponent();
            base = MorphPlateColorHelper.loadMorphForeground(view.getContext(), cn, base);
        }
        DeviceProfile dp = ActivityContext.lookupContext(view.getContext()).getDeviceProfile();
        Rect fromMorph = IconResizeHelper.getMorphIconBounds(dp, fromSpanX, fromSpanY);
        Rect toMorph = IconResizeHelper.getMorphIconBounds(dp, toSpanX, toSpanY);
        int fromW = fromSpanX == 1 && fromSpanY == 1 ? view.getIconSize() : fromMorph.width();
        int fromH = fromSpanX == 1 && fromSpanY == 1 ? view.getIconSize() : fromMorph.height();
        int toW = toSpanX == 1 && toSpanY == 1 ? view.getIconSize() : toMorph.width();
        int toH = toSpanX == 1 && toSpanY == 1 ? view.getIconSize() : toMorph.height();

        MorphTransitionDrawable morph = new MorphTransitionDrawable(view.getContext(), base, cn,
                view.getIconSize(), fromSpanX, fromSpanY, fromW, fromH, toSpanX, toSpanY, toW, toH);
        morph.setProgress(0f);

        TransitionState state = new TransitionState(view, morph);
        sActive = state;
        view.applyMorphTransitionDrawable(morph, toSpanX, toSpanY);

        SpringAnimation spring = new SpringAnimation(morph, MorphTransitionDrawable.PROGRESS, 1f);
        spring.setSpring(new SpringForce(1f)
                .setStiffness(SPRING_STIFFNESS)
                .setDampingRatio(SPRING_DAMPING));
        spring.addEndListener((animation, canceled, value, velocity) -> {
            if (sActive == state) {
                sActive = null;
            }
            BubbleTextView btv = state.viewRef.get();
            if (btv != null) {
                MorphIconLoaderHelper.invalidateCache((com.android.launcher3.model.data.ItemInfo)
                        btv.getTag());
                btv.refreshWorkspaceIconDisplay();
                if (commit) {
                    IconResizeHelper.playCommitAnim(btv);
                }
            }
        });
        state.spring = spring;
        spring.start();
    }

    /** Animate resize frame bounds in drag layer coordinates. */
    public static void animateFrameTo(View frame, int toX, int toY, int toW, int toH,
            @Nullable Runnable onEnd) {
        Object lpObj = frame.getLayoutParams();
        if (!(lpObj instanceof com.android.launcher3.dragndrop.DragLayer.LayoutParams)) {
            if (onEnd != null) {
                onEnd.run();
            }
            return;
        }
        com.android.launcher3.dragndrop.DragLayer.LayoutParams lp =
                (com.android.launcher3.dragndrop.DragLayer.LayoutParams) lpObj;
        final int fromX = lp.x;
        final int fromY = lp.y;
        final int fromW = lp.width;
        final int fromH = lp.height;

        SpringAnimation progress = new SpringAnimation(new FrameAnimHolder(frame, lp, fromX, fromY,
                fromW, fromH, toX, toY, toW, toH), FrameAnimHolder.PROGRESS, 1f);
        progress.setSpring(new SpringForce(1f)
                .setStiffness(SPRING_STIFFNESS)
                .setDampingRatio(SPRING_DAMPING));
        if (onEnd != null) {
            progress.addEndListener((a, c, v, vel) -> onEnd.run());
        }
        progress.start();
    }

    private static final class FrameAnimHolder {
        static final FloatPropertyCompat<FrameAnimHolder> PROGRESS =
                new FloatPropertyCompat<FrameAnimHolder>("frameProgress") {
                    @Override
                    public float getValue(FrameAnimHolder object) {
                        return object.progress;
                    }

                    @Override
                    public void setValue(FrameAnimHolder object, float value) {
                        object.progress = value;
                        object.apply();
                    }
                };

        private final View mFrame;
        private final com.android.launcher3.dragndrop.DragLayer.LayoutParams mLp;
        private final int mFromX;
        private final int mFromY;
        private final int mFromW;
        private final int mFromH;
        private final int mToX;
        private final int mToY;
        private final int mToW;
        private final int mToH;
        private float progress;

        FrameAnimHolder(View frame,
                com.android.launcher3.dragndrop.DragLayer.LayoutParams lp,
                int fromX, int fromY, int fromW, int fromH,
                int toX, int toY, int toW, int toH) {
            mFrame = frame;
            mLp = lp;
            mFromX = fromX;
            mFromY = fromY;
            mFromW = fromW;
            mFromH = fromH;
            mToX = toX;
            mToY = toY;
            mToW = toW;
            mToH = toH;
        }

        void apply() {
            float t = progress;
            mLp.x = Math.round(mFromX + (mToX - mFromX) * t);
            mLp.y = Math.round(mFromY + (mToY - mFromY) * t);
            mLp.width = Math.round(mFromW + (mToW - mFromW) * t);
            mLp.height = Math.round(mFromH + (mToH - mFromH) * t);
            mFrame.requestLayout();
        }
    }
}
