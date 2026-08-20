package com.android.launcher3;

import android.animation.ValueAnimator;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.PathInterpolator;

import com.android.launcher3.icons.FastBitmapDrawable;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.model.data.PackageItemInfo;
import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.android.launcher3.views.ActivityContext;
import com.android.launcher3.views.DoubleShadowBubbleTextView;

/** ColorOS icon host behavior shared by workspace, all-apps and widget-category icons. */
public class OplusBubbleTextView extends DoubleShadowBubbleTextView {

    // Decoded IconPressAnimManager.NormalAnimation / IconViewPressAnimatorImpl values.
    private static final float PRESS_SCALE = 0.85f;
    private static final float PRESS_DARK_ALPHA = 0.4f;
    private static final long PRESS_DURATION_MS = 200L;
    private static final PathInterpolator PRESS_INTERPOLATOR =
            new PathInterpolator(0.4f, 0f, 0.2f, 1f);

    private ValueAnimator mPressAnimator;
    private float mPressAnimationCurrentValue = 1f;
    private final Rect mSelectionIndicatorBounds = new Rect();
    private final Drawable mSelectedIndicator;
    private final Drawable mUnselectedIndicator;
    private AnimatorSet mSelectionAnimator;
    private float mSelectionProgress;
    private boolean mWorkspaceSelectionVisible;
    private final PathInterpolator mSelectionHoldInterpolator =
            new PathInterpolator(0.33f, 0f, 0.67f, 1f);
    private final PathInterpolator mSelectionColorInterpolator =
            new PathInterpolator(0.33f, 0f, 0.25f, 1f);

    public OplusBubbleTextView(Context context) {
        this(context, null);
    }

    public OplusBubbleTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OplusBubbleTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mSelectedIndicator = context.getDrawable(
                R.drawable.launcher_ic_app_selected).mutate();
        mUnselectedIndicator = context.getDrawable(
                R.drawable.launcher_ic_app_unselected).mutate();

        // ColorOS restores font padding after BaseIcon disables it in XML.
        setIncludeFontPadding(true);
        if (getIconDisplay() == 2) {
            setLineSpacing(getResources().getDimension(
                    R.dimen.folder_item_title_line_space), 1f);
        } else if (getIconDisplay() == 1) {
            setLineSpacing(0f, 1f);
        } else {
            setLineSpacing(getResources().getDimension(
                    R.dimen.workspace_item_title_line_space), 1f);
        }
    }

    @Override
    protected void applyCompoundDrawables(Drawable icon) {
        if (icon instanceof FastBitmapDrawable) {
            ((FastBitmapDrawable) icon).setPressedScaleEnabled(false);
        }
        super.applyCompoundDrawables(icon);
    }

    @Override
    protected boolean shouldIgnoreTouchDown(float x, float y) {
        // Decoded ValidTouchAreaController: icon bounds grow by 1.2x, then include the
        // drawable-to-label gap and two text lines for vertical workspace icons.
        Rect validArea = new Rect();
        getIconBounds(validArea);
        Utilities.scaleRectAboutCenter(validArea, 1.2f);
        if (isLayoutHorizontal()) {
            int labelWidth = getCompoundDrawablePadding() + getLineHeight() * 3;
            if (Utilities.isRtl(getResources())) {
                validArea.left -= labelWidth;
            } else {
                validArea.right += labelWidth;
            }
        } else {
            validArea.bottom += getCompoundDrawablePadding() + getLineHeight() * 2;
        }
        return !validArea.contains((int) x, (int) y);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event == null) {
            return false;
        }
        final boolean hasItem = getTag() instanceof ItemInfo;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (hasItem && !shouldIgnoreTouchDown(event.getX(), event.getY())) {
                    playPressFeedback(true);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (hasItem) {
                    playPressFeedback(false);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (shouldIgnoreTouchDown(event.getX(), event.getY())) {
                    resetPressAnimatorStateForTouch(true);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                resetPressAnimatorStateForTouch(true);
                break;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    private void playPressFeedback(boolean down) {
        cancelPressAnimator();
        final float target = down ? PRESS_SCALE : 1f;
        mPressAnimator = ValueAnimator.ofFloat(mPressAnimationCurrentValue, target);
        mPressAnimator.setDuration(PRESS_DURATION_MS);
        mPressAnimator.setInterpolator(PRESS_INTERPOLATOR);
        mPressAnimator.addUpdateListener(animation -> {
            mPressAnimationCurrentValue = (float) animation.getAnimatedValue();
            setPivotX(getWidth() * 0.5f);
            setPivotY(getHeight() * 0.5f);
            setScaleX(mPressAnimationCurrentValue);
            setScaleY(mPressAnimationCurrentValue);
            float fraction = animation.getAnimatedFraction();
            updatePressDarkFilter(down ? fraction : 1f - fraction);
        });
        mPressAnimator.start();
    }

    private void updatePressDarkFilter(float progress) {
        FastBitmapDrawable icon = getIcon();
        if (icon == null) {
            return;
        }
        int alpha = (int) (255f * PRESS_DARK_ALPHA
                * Utilities.boundToRange(progress, 0f, 1f));
        icon.setColorFilter(alpha == 0 ? null
                : new PorterDuffColorFilter(alpha << 24, PorterDuff.Mode.SRC_ATOP));
    }

    private void cancelPressAnimator() {
        if (mPressAnimator != null) {
            mPressAnimator.cancel();
            mPressAnimator = null;
        }
    }

    /** Instantaneous ColorOS press scale used as the drag-view starting scale. */
    public float getPressAnimationCurrentValue() {
        return mPressAnimationCurrentValue;
    }

    /** Restores the source icon after Workspace captures its pressed scale for dragging. */
    public void resetPressAnimStateForLongClick() {
        cancelPressAnimator();
        setPressStateImmediately(1f);
    }

    public void resetPressAnimatorStateForTouch(boolean animate) {
        if (animate && mPressAnimationCurrentValue != 1f) {
            playPressFeedback(false);
        } else {
            cancelPressAnimator();
            setPressStateImmediately(1f);
        }
    }

    private void setPressStateImmediately(float scale) {
        mPressAnimationCurrentValue = scale;
        setScaleX(scale);
        setScaleY(scale);
        updatePressDarkFilter(scale == 1f ? 0f : 1f);
    }

    private boolean isColorOsWorkspaceSelectionVisible() {
        if (!mWorkspaceSelectionVisible
                || !getResources().getBoolean(R.bool.config_hxy_grid)
                || !(getTag() instanceof WorkspaceItemInfo)) {
            return false;
        }
        WorkspaceItemInfo info = (WorkspaceItemInfo) getTag();
        if (info.container != LauncherSettings.Favorites.CONTAINER_DESKTOP
                && info.container != LauncherSettings.Favorites.CONTAINER_HOTSEAT) {
            return false;
        }
        return true;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!isColorOsWorkspaceSelectionVisible()) {
            return;
        }
        if (mSelectionAnimator == null || !mSelectionAnimator.isRunning()) {
            mSelectionProgress = isSelected() ? 1f : 0f;
        }

        Rect iconBounds = new Rect();
        getIconBounds(iconBounds);
        int size = getResources().getDimensionPixelSize(R.dimen.selected_icon_weight);
        int endOffset =
                getResources().getDimensionPixelSize(R.dimen.select_icon_right_offset);
        int topOffset =
                getResources().getDimensionPixelSize(R.dimen.select_icon_top_offset);
        int centerX = Utilities.isRtl(getResources())
                ? iconBounds.left + endOffset
                : iconBounds.right - endOffset;
        int centerY = iconBounds.top + topOffset;
        int half = size / 2;
        // OPPO SwitchStateRenderer moves the mark diagonally inward when it crosses the clip.
        Rect clipBounds = canvas.getClipBounds();
        int overflowTop = Math.max(0, clipBounds.top - (centerY - half));
        if (Utilities.isRtl(getResources())) {
            int overflowStart = Math.max(0, clipBounds.left - (centerX - half));
            int correction = Math.max(overflowStart, overflowTop);
            centerX += correction;
            centerY += correction;
        } else {
            int overflowEnd = Math.max(0, (centerX + half) - clipBounds.right);
            int correction = Math.max(overflowEnd, overflowTop);
            centerX -= correction;
            centerY += correction;
        }
        mSelectionIndicatorBounds.set(
                centerX - half, centerY - half, centerX + half, centerY + half);

        int selectedAlpha = Math.round(255f * mSelectionProgress);
        int unselectedAlpha = 255 - selectedAlpha;
        if (unselectedAlpha > 0) {
            mUnselectedIndicator.setBounds(mSelectionIndicatorBounds);
            mUnselectedIndicator.setAlpha(unselectedAlpha);
            mUnselectedIndicator.draw(canvas);
        }
        if (selectedAlpha > 0) {
            mSelectedIndicator.setBounds(mSelectionIndicatorBounds);
            mSelectedIndicator.setAlpha(selectedAlpha);
            mSelectedIndicator.draw(canvas);
        }
    }

    /**
     * Implements decoded SelectStateIconRenderer.setSelectedWithAnim:
     * a 33 ms state-switch phase followed by a 147 ms color transition.
     */
    public boolean toggleColorOsWorkspaceSelection() {
        if (!isColorOsWorkspaceSelectionVisible()) {
            return false;
        }
        if (mSelectionAnimator != null) {
            mSelectionAnimator.cancel();
        }
        final boolean targetSelected = !isSelected();
        ColorOsBatchDragManager manager = ColorOsBatchDragManager.get(
                Launcher.getLauncher(getContext()));
        if (targetSelected && !manager.canSelectAnother()) {
            return true;
        }
        manager.onSelectionChanged(this, targetSelected);

        final float startProgress = mSelectionProgress;
        final float endProgress = targetSelected ? 1f : 0f;

        ValueAnimator stateSwitch = ValueAnimator.ofFloat(0f, 1f);
        stateSwitch.setDuration(33L);
        stateSwitch.setInterpolator(mSelectionHoldInterpolator);
        stateSwitch.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setSelected(targetSelected);
            }
        });

        ValueAnimator colorTransition =
                ValueAnimator.ofFloat(startProgress, endProgress);
        colorTransition.setDuration(147L);
        colorTransition.setInterpolator(mSelectionColorInterpolator);
        colorTransition.addUpdateListener(animation -> {
            mSelectionProgress = (float) animation.getAnimatedValue();
            invalidate();
        });

        mSelectionAnimator = new AnimatorSet();
        mSelectionAnimator.playSequentially(stateSwitch, colorTransition);
        mSelectionAnimator.start();
        return true;
    }

    /** Applies a selection state without replaying the click transition. */
    public void setColorOsWorkspaceSelected(boolean selected, boolean animate) {
        if (animate && selected != isSelected()) {
            toggleColorOsWorkspaceSelection();
            return;
        }
        if (mSelectionAnimator != null) {
            mSelectionAnimator.cancel();
            mSelectionAnimator = null;
        }
        setSelected(selected);
        mSelectionProgress = selected ? 1f : 0f;
        invalidate();

    }
    /** Explicit state propagation; visibility never depends on panel attachment timing. */
    public void setColorOsWorkspaceSelectionVisible(boolean visible, boolean animate) {
        if (mWorkspaceSelectionVisible == visible) {
            if (visible) invalidate();
            return;
        }
        mWorkspaceSelectionVisible = visible;
        if (!visible) clearColorOsWorkspaceSelection();
        else invalidate();
    }
    public void clearColorOsWorkspaceSelection() {
        if (mSelectionAnimator != null) {
            mSelectionAnimator.cancel();
            mSelectionAnimator = null;
        }
        setSelected(false);
        mSelectionProgress = 0f;
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        // OPPO keeps the selected state while an edit-mode page is temporarily detached.
        // Clearing here desynchronizes the view from ColorOsBatchDragManager on reattachment.
        Launcher launcher = findLauncherHost();
        if (launcher == null
                || !ColorOsWorkspaceSelectionController.isEditModeActive(launcher)) {
            clearColorOsWorkspaceSelection();
        }
        cancelPressAnimator();
        setPressStateImmediately(1f);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Launcher launcher = findLauncherHost();
        setColorOsWorkspaceSelectionVisible(
                launcher != null
                        && ColorOsWorkspaceSelectionController.isEditModeActive(launcher), false);
    }

    /** Workspace selection belongs only to the real Launcher, never preview/sandbox hosts. */
    private Launcher findLauncherHost() {
        ActivityContext host = ActivityContext.lookupContextNoThrow(getContext());
        return host instanceof Launcher ? (Launcher) host : null;
    }

    /** Mirrors BubbleTextViewExtImplOplus.applyFromPackageItemInfo in decoded OPPO Launcher. */
    public void applyFromPackageItemInfo(PackageItemInfo packageItemInfo) {
        applyIconAndLabel(packageItemInfo);
        setTag(packageItemInfo);
        verifyHighRes();
    }
}
