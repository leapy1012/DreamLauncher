/*
 * Copyright (C) 2026 GreenDream
 */
package com.dream.quickstep.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.systemui.navigationbar.buttons.KeyButtonRipple;

/**
 * ColorOS-style Recents card header (Oppo {@code OplusTaskHeaderView}): app icon, title and the
 * ⋮ menu button. The icon keeps {@code @id/icon} so the AOSP {@code TaskView} still owns it.
 */
public class DreamTaskHeaderView extends RelativeLayout {

    /** Below this alpha the menu button of an off-center card ignores taps. */
    private static final float MENU_INTERACTIVE_ALPHA = 0.4f;

    private TextView mTitleView;
    private ImageView mMenuBtn;

    public DreamTaskHeaderView(Context context) {
        this(context, null);
    }

    public DreamTaskHeaderView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DreamTaskHeaderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mTitleView = findViewById(R.id.title_view);
        mMenuBtn = findViewById(R.id.menu_button);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // KeyButtonRipple creates a Handler, and ViewPool inflates cards off the main thread,
        // so the ripple can only be created once attached.
        if (!(mMenuBtn.getBackground() instanceof KeyButtonRipple)) {
            // ColorOS OplusKeyButtonRipple oval press feedback on the ⋮ control.
            KeyButtonRipple ripple = new KeyButtonRipple(
                    getContext(), mMenuBtn, R.dimen.color_task_header_view_menu_size);
            ripple.setType(KeyButtonRipple.Type.OVAL);
            mMenuBtn.setBackground(ripple);
        }
    }

    public ImageView getMenuBtn() {
        return mMenuBtn;
    }

    public void setTitle(@Nullable CharSequence title) {
        mTitleView.setText(title);
    }

    /** Oppo {@code setShortCutAlpha}: only the menu glyph fades; icon and title stay. */
    public void setShortCutAlpha(float alpha) {
        float clamped = Utilities.boundToRange(alpha, 0f, 1f);
        mMenuBtn.setAlpha(clamped);
        boolean interactive = clamped > MENU_INTERACTIVE_ALPHA;
        mMenuBtn.setEnabled(interactive);
        mMenuBtn.setClickable(interactive);
        mMenuBtn.setFocusable(interactive);
    }
}
