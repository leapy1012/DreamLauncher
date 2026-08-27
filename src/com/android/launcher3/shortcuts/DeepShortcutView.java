/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.shortcuts;

import android.content.Context;
import android.content.pm.ShortcutInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.android.launcher3.popup.ColorOsPopupIcons;
import com.android.launcher3.popup.PopupContainerWithArrow;
import com.android.launcher3.util.Themes;
import com.android.launcher3.views.BubbleTextHolder;

/**
 * A {@link android.widget.FrameLayout} that contains an icon and a {@link BubbleTextView} for text.
 * This lets us animate the child BubbleTextView's background (transparent ripple) separately from
 * the {@link DeepShortcutView} background color.
 */
public class DeepShortcutView extends FrameLayout implements BubbleTextHolder {

    private static final Point sTempPoint = new Point();

    private final Drawable mTransparentDrawable = new ColorDrawable(Color.TRANSPARENT);

    private BubbleTextView mBubbleText;
    private View mIconView;

    private WorkspaceItemInfo mInfo;
    private ShortcutInfo mDetail;
    /** When true, popup icons use ColorOS plate/glyph treatment instead of raw adaptive bitmaps. */
    private boolean mColorOsPopupIcons;
    private ColorOsPopupIcons.Theme mColorOsPopupIconTheme;

    public DeepShortcutView(Context context) {
        this(context, null, 0);
    }

    public DeepShortcutView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DeepShortcutView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /** Enables ColorOS popup icon rendering for this row (deep + system share 24dp optics). */
    public void setColorOsPopupIcons(boolean enabled) {
        mColorOsPopupIcons = enabled;
    }

    public void setColorOsPopupIconTheme(ColorOsPopupIcons.Theme theme) {
        mColorOsPopupIconTheme = theme;
        mColorOsPopupIcons = theme != null;
    }

    public boolean isColorOsPopupIcons() {
        return mColorOsPopupIcons;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mBubbleText = findViewById(R.id.bubble_text);
        mBubbleText.setHideBadge(true);
        mIconView = findViewById(R.id.icon);
        tryUpdateTextBackground();
    }

    @Override
    public void setBackground(Drawable background) {
        super.setBackground(background);
        tryUpdateTextBackground();
    }

    @Override
    public void setBackgroundResource(int resid) {
        super.setBackgroundResource(resid);
        tryUpdateTextBackground();
    }

    /**
     * Updates the text background to match the shape of this background (when applicable).
     */
    private void tryUpdateTextBackground() {
        if (!(getBackground() instanceof GradientDrawable) || mBubbleText == null) {
            return;
        }
        GradientDrawable background = (GradientDrawable) getBackground();

        int color = Themes.getAttrColor(getContext(), android.R.attr.colorControlHighlight);
        GradientDrawable backgroundMask = new GradientDrawable();
        backgroundMask.setColor(color);
        backgroundMask.setShape(GradientDrawable.RECTANGLE);
        if (background.getCornerRadii() != null) {
            backgroundMask.setCornerRadii(background.getCornerRadii());
        } else {
            backgroundMask.setCornerRadius(background.getCornerRadius());
        }

        RippleDrawable drawable = new RippleDrawable(ColorStateList.valueOf(color),
                mTransparentDrawable, backgroundMask);
        mBubbleText.setBackground(drawable);
    }

    @Override
    public BubbleTextView getBubbleText() {
        return mBubbleText;
    }

    public void setWillDrawIcon(boolean willDraw) {
        mIconView.setVisibility(willDraw ? View.VISIBLE : View.INVISIBLE);
    }

    public boolean willDrawIcon() {
        return mIconView.getVisibility() == View.VISIBLE;
    }

    /**
     * Returns the position of the center of the icon relative to the container.
     */
    public Point getIconCenter() {
        sTempPoint.y = sTempPoint.x = getMeasuredHeight() / 2;
        if (Utilities.isRtl(getResources())) {
            sTempPoint.x = getMeasuredWidth() - sTempPoint.x;
        }
        return sTempPoint;
    }

    /** package private **/
    public void applyShortcutInfo(WorkspaceItemInfo info, ShortcutInfo detail,
            PopupContainerWithArrow container) {
        mInfo = info;
        mDetail = detail;
        mBubbleText.applyFromWorkspaceItem(info);
        if (mColorOsPopupIcons) {
            ColorOsPopupIcons.Theme theme = mColorOsPopupIconTheme != null
                    ? mColorOsPopupIconTheme
                    : ColorOsPopupIcons.Theme.fromSurface(
                            getContext().getColor(R.color.coloros_popup_surface));
            // Hide BTV compound icon — otherwise the adaptive shortcut bleeds through plate AA.
            mBubbleText.setIconVisible(false);
            mIconView.setBackground(ColorOsPopupIcons.forDeepShortcut(getContext(), detail, theme));
            applyColorOsPopupTypeface(mBubbleText);
            mBubbleText.setTextColor(getContext().getColor(R.color.coloros_text_primary));
        } else {
            mIconView.setBackground(mBubbleText.getIcon());
        }

        // Use the long label as long as it exists and fits.
        CharSequence longLabel = mDetail.getLongLabel();
        int availableWidth = mBubbleText.getWidth() - mBubbleText.getTotalPaddingLeft()
                - mBubbleText.getTotalPaddingRight();
        boolean usingLongLabel = !TextUtils.isEmpty(longLabel)
                && mBubbleText.getPaint().measureText(longLabel.toString()) <= availableWidth;
        mBubbleText.setText(usingLongLabel ? longLabel : mDetail.getShortLabel());

        if (mColorOsPopupIcons) {
            applyColorOsPopupTypeface(mBubbleText);
            mBubbleText.setTextColor(getContext().getColor(R.color.coloros_text_primary));
        }

        // TODO: Add the click handler to this view directly and not the child view.
        mBubbleText.setOnClickListener(container.getItemClickListener());
        mBubbleText.setOnLongClickListener(container.getItemDragHandler());
        mBubbleText.setOnTouchListener(container.getItemDragHandler());
    }

    /**
     * Returns the shortcut info that is suitable to be added on the homescreen
     */
    public WorkspaceItemInfo getFinalInfo() {
        final WorkspaceItemInfo badged = new WorkspaceItemInfo(mInfo);
        // Queue an update task on the worker thread. This ensures that the badged
        // shortcut eventually gets its icon updated.
        Launcher.getLauncher(getContext()).getModel()
                .updateAndBindWorkspaceItem(badged, mDetail);
        return badged;
    }

    /** Oppo oplus_deep_shortcut: sans-serif-regular / weight 400. */
    public static void applyColorOsPopupTypeface(BubbleTextView label) {
        android.graphics.Typeface base = android.graphics.Typeface.SANS_SERIF;
        android.graphics.Typeface tf;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            tf = android.graphics.Typeface.create(base, 400, /* italic= */ false);
        } else {
            tf = android.graphics.Typeface.create(base, android.graphics.Typeface.NORMAL);
        }
        label.setTypeface(tf);
        label.setElegantTextHeight(false);
        label.getPaint().setFakeBoldText(false);
        label.getPaint().setTypeface(tf);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            label.setLetterSpacing(0f);
        }
    }

    public View getIconView() {
        return mIconView;
    }

    public ShortcutInfo getDetail() {
        return mDetail;
    }
}
