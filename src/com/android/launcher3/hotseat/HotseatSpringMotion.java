package com.android.launcher3.hotseat;

import android.util.ArrayMap;
import android.view.View;

import androidx.dynamicanimation.animation.FloatValueHolder;

import com.android.launcher3.Hotseat;
import com.android.launcher3.ShortcutAndWidgetContainer;
import com.android.launcher3.celllayout.CellLayoutLayoutParams;
import com.coui.appcompat.animation.dynamicanimation.COUISpringAnimation;
import com.coui.appcompat.animation.dynamicanimation.COUISpringForce;

/**
 * Oppo ColorOS phone-dock springs: layout reads {@link CellLayoutLayoutParams#animX}
 * while moving; COUI springs retarget without restarting from a snapped grid seat.
 *
 * <p>Mirrors {@code HotseatSpringCenterLayoutAnimation#getChildAnimXAnim} +
 * {@code DockSpringAnimationSet#animateToFinalPosition} (stiffness 300, no bounce).
 */
public final class HotseatSpringMotion {

    /** Oppo {@code HotseatSpringCenterLayoutAnimation.STIFFNESS}. */
    private static final float STIFFNESS = 300f;

    private final Hotseat mHotseat;
    private final ArrayMap<View, COUISpringAnimation> mAnimXSprings = new ArrayMap<>();
    /** Last spring final requested per child — skip DRAG_OVER noise retargets. */
    private final ArrayMap<View, Integer> mAnimXFinals = new ArrayMap<>();
    private int mRunningCount;
    private boolean mMoving;

    public HotseatSpringMotion(Hotseat hotseat) {
        mHotseat = hotseat;
    }

    /** True while any dock child is springing (Oppo {@code OplusHotseat.isMoving()}). */
    public boolean isMoving() {
        return mMoving || mRunningCount > 0;
    }

    /** Whether this child currently has a running animX spring. */
    public boolean isChildMoving(View child) {
        if (child == null) {
            return false;
        }
        COUISpringAnimation spring = mAnimXSprings.get(child);
        return spring != null && spring.isRunning();
    }

    /**
     * Seed {@code animX}/{@code animY} so the icon stays on its current screen pixel
     * after a pad/cellW change. {@code screenXY} is the child's pre-change screen origin.
     */
    public void seedAnimFromScreen(View child, int screenX, int screenY) {
        if (child == null) {
            return;
        }
        CellLayoutLayoutParams lp = (CellLayoutLayoutParams) child.getLayoutParams();
        if (lp == null) {
            return;
        }
        ShortcutAndWidgetContainer container = mHotseat.getShortcutsAndWidgets();
        if (container == null) {
            return;
        }
        int[] parentLoc = new int[2];
        container.getLocationOnScreen(parentLoc);
        lp.isHotseatChild = true;
        lp.animX = screenX - parentLoc[0];
        lp.animY = screenY - parentLoc[1];
        lp.x = lp.animX;
        lp.y = lp.animY;
    }

    /** Seed animX from the child's current layout (drag-init / idle sync). */
    public void seedAnimFromLayout(View child) {
        if (child == null) {
            return;
        }
        CellLayoutLayoutParams lp = (CellLayoutLayoutParams) child.getLayoutParams();
        if (lp == null) {
            return;
        }
        lp.isHotseatChild = true;
        if (child.getWidth() > 0) {
            lp.animX = child.getLeft();
            lp.animY = child.getTop();
        } else {
            lp.animX = lp.x;
            lp.animY = lp.y;
        }
    }

    /**
     * Compute the locked-to-grid pixel X for {@code child} at its current cellX/tmpCellX
     * without committing that X into the visible seat (animX still owns drawing).
     */
    public int computeTargetX(View child) {
        CellLayoutLayoutParams lp = (CellLayoutLayoutParams) child.getLayoutParams();
        ShortcutAndWidgetContainer container = mHotseat.getShortcutsAndWidgets();
        if (lp == null || container == null) {
            return 0;
        }
        int savedX = lp.x;
        int savedY = lp.y;
        boolean locked = lp.isLockedToGrid;
        lp.isLockedToGrid = true;
        container.setupLp(child);
        int targetX = lp.x;
        lp.x = savedX;
        lp.y = savedY;
        lp.isLockedToGrid = locked;
        return targetX;
    }

    /**
     * Ensure a per-child animX spring exists and retarget it to {@code targetX}.
     * If already running (or settled) within 2px of {@code targetX}, do nothing —
     * every DRAG_OVER was re-calling this with ±1px noise and that is the visible
     * right-icon "retarget" shuffle.
     */
    public void animateAnimXTo(View child, int targetX) {
        if (child == null) {
            return;
        }
        CellLayoutLayoutParams lp = (CellLayoutLayoutParams) child.getLayoutParams();
        if (lp == null) {
            return;
        }
        Integer prevFinal = mAnimXFinals.get(child);
        if (prevFinal != null && Math.abs(prevFinal - targetX) <= 2) {
            // Same seat as last request. Skip only if already settling there or on it —
            // otherwise a cancelled spring would leave the icon stranded mid-way.
            if (isChildMoving(child) || Math.abs(lp.animX - targetX) <= 2) {
                return;
            }
        }
        if (Math.abs(lp.animX - targetX) <= 2 && !isChildMoving(child)) {
            lp.animX = targetX;
            lp.x = targetX;
            lp.isLockedToGrid = true;
            mAnimXFinals.put(child, targetX);
            return;
        }

        lp.isHotseatChild = true;
        lp.isLockedToGrid = false;
        mMoving = true;
        mAnimXFinals.put(child, targetX);

        COUISpringAnimation spring = mAnimXSprings.get(child);
        if (spring == null) {
            FloatValueHolder holder = new FloatValueHolder(lp.animX);
            spring = new COUISpringAnimation(holder);
            COUISpringForce force = new COUISpringForce()
                    .setStiffness(STIFFNESS)
                    .setDampingRatio(COUISpringForce.DAMPING_RATIO_NO_BOUNCY);
            spring.setSpring(force);
            spring.setMinimumVisibleChange(1f);
            final View target = child;
            final CellLayoutLayoutParams targetLp = lp;
            spring.addUpdateListener((animation, value, velocity) -> {
                targetLp.animX = Math.round(value);
                targetLp.x = targetLp.animX;
                target.requestLayout();
            });
            spring.addEndListener((animation, canceled, value, velocity) -> {
                mRunningCount = Math.max(0, mRunningCount - 1);
                if (!canceled) {
                    targetLp.animX = Math.round(value);
                    targetLp.x = targetLp.animX;
                    // Insert preview keeps tmp coords; only lock when settled permanently.
                    if (!targetLp.useTmpCoords) {
                        targetLp.isLockedToGrid = true;
                    }
                }
                if (mRunningCount == 0) {
                    mMoving = false;
                    mHotseat.onIconSpringsSettled();
                }
            });
            mAnimXSprings.put(child, spring);
        }

        if (!spring.isRunning()) {
            spring.setStartValue(lp.animX);
            mRunningCount++;
        }
        mHotseat.setHotseatClipEnabled(false);
        android.util.Log.i("HSDrop", "animateAnimXTo "
                + (child.getTag() instanceof com.android.launcher3.model.data.ItemInfo info
                && info.title != null ? info.title : "?")
                + " from=" + lp.animX + " to=" + targetX);
        spring.animateToFinalPosition(targetX);
    }

    /** Snap every running spring to its final value and clear. */
    public void endAll() {
        for (int i = 0; i < mAnimXSprings.size(); i++) {
            COUISpringAnimation spring = mAnimXSprings.valueAt(i);
            if (spring != null && spring.isRunning()) {
                spring.skipToEnd();
            }
        }
        mAnimXSprings.clear();
        mAnimXFinals.clear();
        mRunningCount = 0;
        mMoving = false;
        mHotseat.onIconSpringsSettled();
    }

    /** Cancel without finishing (drag cancelled / session reset). */
    public void clearAll() {
        for (int i = 0; i < mAnimXSprings.size(); i++) {
            COUISpringAnimation spring = mAnimXSprings.valueAt(i);
            if (spring != null) {
                spring.cancel();
            }
        }
        mAnimXSprings.clear();
        mAnimXFinals.clear();
        mRunningCount = 0;
        mMoving = false;
        mHotseat.onIconSpringsSettled();
    }

    /** Drop one child's spring so the next animate starts from a freshly seeded animX. */
    public void clearChild(View child) {
        COUISpringAnimation spring = mAnimXSprings.remove(child);
        mAnimXFinals.remove(child);
        if (spring != null) {
            if (spring.isRunning()) {
                spring.cancel();
                mRunningCount = Math.max(0, mRunningCount - 1);
            }
        }
        if (mRunningCount == 0) {
            mMoving = false;
        }
    }
}
