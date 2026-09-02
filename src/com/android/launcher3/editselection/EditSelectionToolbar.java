package com.android.launcher3.editselection;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.dragndrop.DragLayer;

/**
 * Oppo {@code ToggleStateToolbar}: Cancel | N selected | Done over the workspace in edit mode.
 */
public class EditSelectionToolbar extends FrameLayout {

    private TextView mCancel;
    private TextView mTitle;
    private TextView mDone;

    public EditSelectionToolbar(Context context) {
        this(context, null);
    }

    public EditSelectionToolbar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EditSelectionToolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.edit_selection_toolbar, this, true);
        mCancel = findViewById(R.id.edit_selection_cancel);
        mTitle = findViewById(R.id.edit_selection_title);
        mDone = findViewById(R.id.edit_selection_done);
        mCancel.setClickable(true);
        mCancel.setFocusable(true);
        mDone.setClickable(true);
        mDone.setFocusable(true);
        applyPillBackground(mCancel);
        applyPillBackground(mDone);
        setVisibility(GONE);
        setClipChildren(false);
        setClipToPadding(false);
        setForceDarkAllowed(false);
        // Do not consume empty-area taps; only Cancel/Done should receive clicks.
        setClickable(false);
        setFocusable(false);
    }

    /**
     * Oppo PressFeedbackButton fill: {@code #66e0e0e0} on dark wallpaper
     * (composites to ~#595959). Drawn in code so theme/force-dark cannot strip it.
     */
    private static void applyPillBackground(TextView button) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        int color = button.getResources().getColor(R.color.edit_selection_toolbar_btn_bg, null);
        bg.setColor(color);
        float radius = button.getResources().getDimension(R.dimen.edit_selection_toolbar_btn_height)
                / 2f;
        bg.setCornerRadius(radius);
        button.setBackground(bg);
        button.setForceDarkAllowed(false);
        button.setElevation(0f);
        button.setStateListAnimator(null);
        button.setIncludeFontPadding(false);
    }

    public static EditSelectionToolbar attach(Launcher launcher) {
        DragLayer dragLayer = launcher.getDragLayer();
        View existing = dragLayer.findViewById(R.id.edit_selection_toolbar_root);
        if (existing instanceof EditSelectionToolbar toolbar) {
            dragLayer.bringChildToFront(toolbar);
            toolbar.updateLayoutPosition();
            return toolbar;
        }
        EditSelectionToolbar toolbar = new EditSelectionToolbar(launcher);
        toolbar.setId(R.id.edit_selection_toolbar_root);
        // Use normal FrameLayout gravity — customPosition requires absolute px size and
        // breaks hit-testing when width/height are MATCH_PARENT / WRAP_CONTENT.
        DragLayer.LayoutParams lp = new DragLayer.LayoutParams(
                DragLayer.LayoutParams.MATCH_PARENT,
                DragLayer.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.topMargin = launcher.getDeviceProfile().getInsets().top;
        dragLayer.addView(toolbar, lp);
        dragLayer.bringChildToFront(toolbar);
        return toolbar;
    }

    public void setCancelClickListener(OnClickListener listener) {
        mCancel.setOnClickListener(listener);
    }

    public void setDoneClickListener(OnClickListener listener) {
        mDone.setOnClickListener(listener);
    }

    public void show() {
        applyPillBackground(mCancel);
        applyPillBackground(mDone);
        updateLayoutPosition();
        setVisibility(VISIBLE);
        setAlpha(1f);
        bringToFront();
        if (getParent() instanceof DragLayer dragLayer) {
            dragLayer.bringChildToFront(this);
        }
        updateCount(0);
    }

    public void hide() {
        setVisibility(GONE);
    }

    public void updateCount(int count) {
        if (count <= 0) {
            mCancel.setVisibility(GONE);
            mTitle.setVisibility(GONE);
            mTitle.setText("");
        } else {
            mCancel.setVisibility(VISIBLE);
            mTitle.setVisibility(VISIBLE);
            mTitle.setText(getResources().getQuantityString(
                    R.plurals.edit_selection_count, count, count));
            // Keep Cancel above the options panel / workspace for reliable taps.
            bringToFront();
            if (getParent() instanceof DragLayer dragLayer) {
                dragLayer.bringChildToFront(this);
            }
        }
        mDone.setVisibility(VISIBLE);
    }

    private void updateLayoutPosition() {
        if (!(getLayoutParams() instanceof DragLayer.LayoutParams lp)) {
            return;
        }
        if (!(getContext() instanceof Launcher launcher)) {
            return;
        }
        lp.topMargin = launcher.getDeviceProfile().getInsets().top;
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.customPosition = false;
        requestLayout();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateLayoutPosition();
    }
}
