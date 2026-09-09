package com.android.launcher3.screenedit;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Picture;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.R;
import com.android.launcher3.Workspace;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.views.OptionsDialogView;

/**
 * CM {@code ScreenManageView}: 3D Y-axis carousel of workspace page thumbnails.
 */
public class ScreenManagerOverlay extends AbstractFloatingView {

    private static final float PAGE_SCALE = 0.42f;
    private static final int GAP_DP = 7;
    private static final float DRAG_ROT_FACTOR = 6f; // CM: ΔrotY = Δx * 360 / (6 * radius)

    private Launcher mLauncher;
    private FrameLayout mCarousel;
    private float mRadiusPx;
    private float mContainerRotY;
    private float mDownX;
    private float mDownRot;
    private boolean mDragging;
    private final int mTouchSlop;

    public ScreenManagerOverlay(Context context) {
        this(context, null);
    }

    public ScreenManagerOverlay(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScreenManagerOverlay(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
        setClickable(true);
        setFocusable(true);
        setBackgroundColor(0xCC0A1220);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mCarousel = new FrameLayout(context);
        mCarousel.setClipChildren(false);
        mCarousel.setClipToPadding(false);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f);
        addView(mCarousel, lp);
    }

    public static void show(Launcher launcher) {
        OptionsDialogView options = AbstractFloatingView.getOpenView(
                launcher, TYPE_OPTIONS_POPUP_DIALOG);
        if (options != null) {
            options.close(false);
        }
        closeOpenViews(launcher, false, TYPE_SCREEN_MANAGER);
        ScreenManagerOverlay overlay = new ScreenManagerOverlay(launcher);
        overlay.mLauncher = launcher;
        overlay.mIsOpen = true;
        DragLayer.LayoutParams lp = new DragLayer.LayoutParams(
                DragLayer.LayoutParams.MATCH_PARENT,
                DragLayer.LayoutParams.MATCH_PARENT);
        lp.gravity = Gravity.FILL;
        lp.ignoreInsets = true;
        launcher.getDragLayer().addView(overlay, lp);
        overlay.post(overlay::buildCarousel);
    }

    private void buildCarousel() {
        mCarousel.removeAllViews();
        Workspace<?> workspace = mLauncher.getWorkspace();
        int pageCount = workspace.getPageCount();
        if (pageCount <= 0) {
            close(true);
            return;
        }

        int screenW = getWidth() > 0 ? getWidth() : mLauncher.getDeviceProfile().widthPx;
        int screenH = getHeight() > 0 ? getHeight() : mLauncher.getDeviceProfile().heightPx;
        int thumbW = Math.round(screenW * PAGE_SCALE);
        int thumbH = Math.round(screenH * PAGE_SCALE);
        float gap = GAP_DP * getResources().getDisplayMetrics().density;
        float stepDeg = 360f / Math.max(1, pageCount);
        double halfStepRad = Math.toRadians(stepDeg / 2.0);
        mRadiusPx = (float) ((thumbW / 2f + gap) / Math.tan(halfStepRad));

        float density = getResources().getDisplayMetrics().density;
        mCarousel.setCameraDistance(density * 8000f);
        mCarousel.setPivotX(screenW / 2f);
        mCarousel.setPivotY(screenH * 0.42f);
        mCarousel.setPivotZ(-mRadiusPx);

        for (int i = 0; i < pageCount; i++) {
            View page = workspace.getPageAt(i);
            Bitmap bmp = snapshotPage(page, thumbW, thumbH);
            ImageView card = new ImageView(getContext());
            card.setScaleType(ImageView.ScaleType.FIT_XY);
            if (bmp != null) {
                card.setImageBitmap(bmp);
            } else {
                GradientDrawable placeholder = new GradientDrawable();
                placeholder.setColor(0xFF2A3548);
                placeholder.setCornerRadius(16f * density);
                card.setImageDrawable(placeholder);
            }
            card.setBackgroundColor(Color.BLACK);
            card.setElevation(8f * density);
            card.setClipToOutline(true);
            float radius = 12f * density;
            card.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
                }
            });
            final int pageIndex = i;
            card.setOnClickListener(v -> exitToPage(pageIndex));

            FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(thumbW, thumbH);
            clp.gravity = Gravity.CENTER;
            mCarousel.addView(card, clp);
            placeCard(card, i, pageCount, thumbW);
        }
        // Start with current page facing forward.
        float face = -workspace.getCurrentPage() * stepDeg;
        applyContainerRotation(face, false);
    }

    private void placeCard(View card, int index, int pageCount, int thumbW) {
        float stepDeg = 360f / Math.max(1, pageCount);
        float angle = index * stepDeg;
        double rad = Math.toRadians(angle);
        float tx = (float) (mRadiusPx * Math.sin(rad));
        float tz = (float) (-mRadiusPx + mRadiusPx * Math.cos(rad));
        card.setRotationY(angle);
        card.setTranslationX(tx);
        card.setTranslationZ(tz);
        card.setCameraDistance(getResources().getDisplayMetrics().density * 8000f);
        // Cull roughly-backfacing cards.
        float facing = ((angle % 360f) + 360f) % 360f;
        boolean back = facing > 90f && facing < 270f;
        card.setAlpha(back ? 0.15f : 1f);
    }

    private void applyContainerRotation(float rotY, boolean animate) {
        if (animate) {
            ValueAnimator anim = ValueAnimator.ofFloat(mContainerRotY, rotY);
            anim.setDuration(Math.max(100, (long) (Math.abs(rotY - mContainerRotY) / 360f * 800)));
            anim.addUpdateListener(a -> {
                mContainerRotY = (float) a.getAnimatedValue();
                mCarousel.setRotationY(mContainerRotY);
                updateCardCull();
            });
            anim.start();
        } else {
            mContainerRotY = rotY;
            mCarousel.setRotationY(mContainerRotY);
            updateCardCull();
        }
    }

    private void updateCardCull() {
        int n = mCarousel.getChildCount();
        if (n == 0) {
            return;
        }
        float step = 360f / n;
        for (int i = 0; i < n; i++) {
            View card = mCarousel.getChildAt(i);
            float world = ((i * step + mContainerRotY) % 360f + 360f) % 360f;
            boolean back = world > 90f && world < 270f;
            card.setAlpha(back ? 0.15f : 1f);
            // Highlight nearest front card with white border via elevation.
            float dist = Math.min(world, 360f - world);
            card.setScaleX(dist < 25f ? 1.05f : 1f);
            card.setScaleY(dist < 25f ? 1.05f : 1f);
        }
    }

    @Nullable
    private Bitmap snapshotPage(View page, int outW, int outH) {
        if (page == null || page.getWidth() <= 0 || page.getHeight() <= 0) {
            // Force a layout pass for offscreen pages if needed.
            if (page instanceof CellLayout) {
                int w = mLauncher.getDeviceProfile().widthPx;
                int h = mLauncher.getDeviceProfile().heightPx;
                        page.measure(
                                View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
                page.layout(0, 0, w, h);
            } else {
                return null;
            }
        }
        int srcW = Math.max(1, page.getWidth());
        int srcH = Math.max(1, page.getHeight());
        try {
            Picture picture = new Picture();
            Canvas recording = picture.beginRecording(srcW, srcH);
            page.draw(recording);
            picture.endRecording();
            Bitmap full = Bitmap.createBitmap(picture);
            if (full.getWidth() == outW && full.getHeight() == outH) {
                return full;
            }
            Bitmap scaled = Bitmap.createScaledBitmap(full, outW, outH, true);
            if (scaled != full) {
                full.recycle();
            }
            return scaled;
        } catch (Throwable t) {
            return null;
        }
    }

    private void exitToPage(int pageIndex) {
        Workspace<?> workspace = mLauncher.getWorkspace();
        workspace.snapToPage(pageIndex);
        mLauncher.getStateManager().goToState(LauncherState.NORMAL);
        close(true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = event.getX();
                mDownRot = mContainerRotY;
                mDragging = false;
                return true;
            case MotionEvent.ACTION_MOVE: {
                float dx = event.getX() - mDownX;
                if (!mDragging && Math.abs(dx) > mTouchSlop) {
                    mDragging = true;
                }
                if (mDragging && mRadiusPx > 0f) {
                    // CM: ΔrotY = Δx * 360 / (6 * radius)
                    float dRot = dx * 360f / (DRAG_ROT_FACTOR * mRadiusPx);
                    applyContainerRotation(mDownRot + dRot, false);
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mDragging) {
                    snapToNearestPage();
                } else if (event.getActionMasked() == MotionEvent.ACTION_UP
                        && Math.abs(event.getX() - mDownX) < mTouchSlop) {
                    // Empty-space tap closes.
                    Rect carouselHit = new Rect();
                    mCarousel.getHitRect(carouselHit);
                    if (!carouselHit.contains((int) event.getX(), (int) event.getY())) {
                        close(true);
                        mLauncher.getStateManager().goToState(LauncherState.NORMAL);
                    }
                }
                mDragging = false;
                return true;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    private void snapToNearestPage() {
        int n = mCarousel.getChildCount();
        if (n <= 0) {
            return;
        }
        float step = 360f / n;
        float target = Math.round(-mContainerRotY / step) * -step;
        applyContainerRotation(target, true);
    }

    @Override
    protected void handleClose(boolean animate) {
        mIsOpen = false;
        if (getParent() instanceof DragLayer) {
            ((DragLayer) getParent()).removeView(this);
        }
    }

    @Override
    protected boolean isOfType(int type) {
        return (type & TYPE_SCREEN_MANAGER) != 0;
    }

    @Override
    public void logActionCommand(int command) {
        // no-op
    }
}
