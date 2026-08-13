package com.android.launcher3.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.COUIRecyclerView;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.DragSource;
import com.android.launcher3.DropTarget.DragObject;
import com.android.launcher3.Launcher;
import com.android.launcher3.PendingAddItemInfo;
import com.android.launcher3.R;
import com.android.launcher3.dragndrop.DragOptions;
import com.android.launcher3.touch.ItemLongClickListener;
import com.android.launcher3.widget.NavigableAppWidgetHostView;
import com.android.launcher3.widget.PendingItemDragHelper;
import com.android.launcher3.widget.WidgetCell;
import com.android.launcher3.widget.WidgetImageView;
import com.android.launcher3.widget.picker.WidgetsFullSheet;
import com.coui.appcompat.animation.COUIEaseInterpolator;
import com.coui.appcompat.animation.COUIOutEaseInterpolator;
import com.coui.appcompat.toolbar.COUIToolbar;

/** ColorOS first-level widget recommendation panel shown before the complete catalog. */
public class ColorOsWidgetHubView extends AbstractFloatingView implements DragSource {

    /** Separate local type mirroring the external Assistant Screen surface used by OPPO. */
    private static final int TYPE_COLOROS_WIDGET_HUB = TYPE_LISTENER;

    private final Launcher mLauncher;
    private View mPanel;
    private View mMask;
    private float mDragDownY;

    public ColorOsWidgetHubView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLauncher = Launcher.getLauncher(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mPanel = findViewById(R.id.coloros_widget_hub_panel);
        mMask = findViewById(R.id.coloros_widget_hub_mask);
        mMask.setOnClickListener(v -> close(true));

        COUIToolbar toolbar = findViewById(R.id.coloros_widget_hub_toolbar);
        toolbar.setTitle(R.string.coloros_home_screen_widgets);
        toolbar.inflateMenu(R.menu.coloros_widget_store_toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.coloros_widget_hub_close) {
                close(true);
                return true;
            }
            return false;
        });

        findViewById(R.id.coloros_widget_drag_layout).setOnTouchListener(this::onDragHandleTouch);
        COUIRecyclerView recyclerView = findViewById(R.id.coloros_widget_hub_recycler);
        // CardStoreRecyclerView applies these exact two policies in the decoded Assistant
        // Screen source so a settling/overscrolling list cannot accidentally open or drag a card.
        recyclerView.setDispatchEventWhileOverScrolling(false);
        recyclerView.setItemClickableWhileOverScrolling(false);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new ColorOsWidgetHubAdapter(mLauncher, () -> {
            // OPPO starts Launcher WidgetsFullSheet over the Assistant Screen activity. Keep this
            // local stand-in drawn underneath, but exclude it from accessibility traversal.
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            WidgetsFullSheet.show(mLauncher, true);
        }, this::onWidgetClick, this::onWidgetLongClick));
    }

    private void onWidgetClick(View view) {
        Object tag = view.getTag();
        if (!(tag instanceof PendingAddItemInfo)) {
            return;
        }
        // ToggleBarWidgetUIController.addWidgetFromToggleItemClick performs a direct
        // click-to-add. Reuse Launcher's native space search/bind flow for the same result.
        if (mLauncher.getAccessibilityDelegate().addToWorkspace(
                (PendingAddItemInfo) tag, false)) {
            close(true);
        }
    }

    private boolean onWidgetLongClick(View view) {
        view.cancelLongPress();
        if (!ItemLongClickListener.canStartDrag(mLauncher)) {
            return false;
        }

        WidgetCell cell = view instanceof WidgetCell
                ? (WidgetCell) view
                : view.getParent() instanceof WidgetCell
                        ? (WidgetCell) view.getParent() : null;
        if (cell == null) {
            return false;
        }

        WidgetImageView image = cell.getWidgetView();
        NavigableAppWidgetHostView hostPreview = cell.getAppWidgetHostViewPreview();
        if (image.getDrawable() == null && hostPreview == null) {
            return false;
        }

        PendingItemDragHelper dragHelper = new PendingItemDragHelper(cell);
        dragHelper.setRemoteViewsPreview(
                cell.getRemoteViewsPreview(), cell.getAppWidgetHostViewScale());
        dragHelper.setAppWidgetHostViewPreview(hostPreview);

        if (image.getDrawable() != null) {
            int[] location = new int[2];
            mLauncher.getDragLayer().getLocationInDragLayer(image, location);
            dragHelper.startDrag(
                    image.getBitmapBounds(),
                    image.getDrawable().getIntrinsicWidth(),
                    image.getWidth(),
                    new Point(location[0], location[1]),
                    this,
                    new DragOptions());
        } else {
            int[] location = new int[2];
            mLauncher.getDragLayer().getLocationInDragLayer(hostPreview, location);
            Rect bounds = new Rect();
            hostPreview.getWorkspaceVisualDragBounds(bounds);
            dragHelper.startDrag(
                    bounds,
                    hostPreview.getMeasuredWidth(),
                    hostPreview.getMeasuredWidth(),
                    new Point(location[0], location[1]),
                    this,
                    new DragOptions());
        }

        // OPPO hands the operating-card preview to the launcher and retreats its panel.
        close(true);
        return true;
    }

    @Override
    public void onDropCompleted(View target, DragObject dragObject, boolean success) {
        // ToggleBarWidgetUIController returns cancelled/failed drops to WidgetListState and
        // successful desktop drops to ToggleBarState. The hub has already retreated for drag.
        if (!success) {
            mLauncher.getDragLayer().post(() -> {
                if (AbstractFloatingView.getOpenView(
                        mLauncher, TYPE_COLOROS_WIDGET_HUB) == null) {
                    show(mLauncher);
                }
            });
        }
    }

    public static ColorOsWidgetHubView show(Launcher launcher) {
        ColorOsWidgetHubView view = (ColorOsWidgetHubView) launcher.getLayoutInflater().inflate(
                R.layout.coloros_widget_hub_root, launcher.getDragLayer(), false);
        BaseDragLayer.LayoutParams lp = new BaseDragLayer.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.ignoreInsets = true;
        view.setLayoutParams(lp);
        launcher.getDragLayer().addView(view);
        view.mIsOpen = true;
        view.mMask.setAlpha(0f);
        view.mMask.animate().alpha(1f).setDuration(167)
                .setInterpolator(new COUIEaseInterpolator()).start();
        view.mPanel.setTranslationY(launcher.getDragLayer().getHeight());
        view.mPanel.animate().translationY(0f).setDuration(333)
                .setInterpolator(new COUIOutEaseInterpolator()).start();
        return view;
    }

    private boolean onDragHandleTouch(View view, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mDragDownY = event.getRawY();
                mPanel.animate().cancel();
                mMask.animate().cancel();
                return true;
            case MotionEvent.ACTION_MOVE:
                float distance = Math.max(0f, event.getRawY() - mDragDownY);
                mPanel.setTranslationY(distance);
                float height = Math.max(1f, mPanel.getHeight());
                mMask.setAlpha(Math.max(0f, 1f - distance / height));
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mPanel.getTranslationY() > mPanel.getHeight() * 0.12f) {
                    close(true);
                } else {
                    mPanel.animate().translationY(0f).setDuration(250)
                            .setInterpolator(new COUIOutEaseInterpolator()).start();
                    mMask.animate().alpha(1f).setDuration(183)
                            .setInterpolator(new COUIEaseInterpolator()).start();
                }
                return true;
            default:
                return false;
        }
    }

    @Override
    protected void handleClose(boolean animate) {
        if (!animate) {
            mLauncher.getDragLayer().removeView(this);
            return;
        }
        mMask.animate().alpha(0f).setDuration(183)
                .setInterpolator(new COUIEaseInterpolator()).start();
        mPanel.animate().translationY(mPanel.getHeight()).setDuration(333)
                .setInterpolator(new COUIOutEaseInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mLauncher.getDragLayer().removeView(ColorOsWidgetHubView.this);
                    }
                }).start();
    }

    @Override
    protected boolean isOfType(int type) {
        return (type & TYPE_COLOROS_WIDGET_HUB) != 0;
    }

    /** Called by the launcher-owned catalog when its decoded close animation completes. */
    public void restoreAfterCatalog() {
        setVisibility(VISIBLE);
        setAlpha(1f);
        mMask.setAlpha(1f);
        mPanel.setTranslationY(0f);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    }

    @Override
    public boolean onControllerInterceptTouchEvent(android.view.MotionEvent ev) {
        // Assistant Screen owns its window's complete touch stream. Since this local equivalent
        // lives in Launcher's DragLayer, claim the stream before AllAppsSwipeController can turn
        // an in-panel recycler gesture into a workspace-to-all-apps transition.
        return true;
    }

    @Override
    public boolean onControllerTouchEvent(MotionEvent ev) {
        // Forward the controller-owned sequence through the normal hierarchy so COUIRecyclerView,
        // widget cells, the toolbar, mask, and panel drag handle retain their native behavior.
        return dispatchTouchEvent(ev);
    }

    @Override
    public boolean canInterceptEventsInSystemGestureRegion() {
        // The exposed mask strip begins above the panel's 40dp top edge and overlaps the status
        // gesture inset. Assistant Screen owns this area as part of its panel window.
        return true;
    }
}
