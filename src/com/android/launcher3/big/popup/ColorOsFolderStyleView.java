package com.android.launcher3.big.popup;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import com.android.launcher3.Launcher;
import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.R;
import com.android.launcher3.folder.large.HxyLargeFolderIcon;
import com.android.launcher3.model.data.FolderInfo;
import com.coui.appcompat.contextutil.COUIContextUtil;

/**
 * The three folder-preview selectors used by ColorOS above the folder action rows.
 *
 * <p>The option bits deliberately live outside AOSP FolderInfo's low-bit flags. This keeps the
 * ColorOS preview selection persistent without colliding with manual-name or page-animation
 * state.</p>
 */
public final class ColorOsFolderStyleView extends LinearLayout {
    public static final int STYLE_NINE_GRID = FolderInfo.FLAG_PREVIEW_NINE_GRID;
    public static final int STYLE_FOUR_GRID = FolderInfo.FLAG_PREVIEW_FOUR_GRID;
    public static final int STYLE_HIERARCHICAL = FolderInfo.FLAG_PREVIEW_HIGHLIGHT;

    private final Launcher mLauncher;
    private final HxyLargeFolderIcon mFolderIcon;
    private final FolderInfo mInfo;
    private final FrameLayout[] mItems = new FrameLayout[3];
    private final ImageView[] mIcons = new ImageView[3];
    private int mSelectedIndex;

    public ColorOsFolderStyleView(Launcher launcher, HxyLargeFolderIcon folderIcon) {
        super(launcher);
        mLauncher = launcher;
        mFolderIcon = folderIcon;
        mInfo = (FolderInfo) folderIcon.getTag();
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setLayoutParams(new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, dp(52)));

        int[] icons = {
                R.drawable.folder_transform_to_nine_grid,
                R.drawable.folder_transform_to_four_grid,
                R.drawable.folder_transform_to_highlight
        };
        int[] descriptions = {
                R.string.coloros_folder_style_nine_grid,
                R.string.coloros_folder_style_four_grid,
                R.string.coloros_folder_style_hierarchical
        };
        mSelectedIndex = getSelectedIndex();
        for (int index = 0; index < icons.length; index++) {
            addSelector(index, icons[index], descriptions[index]);
        }
        updateSelection();
    }

    private void addSelector(int index, int drawableRes, int descriptionRes) {
        FrameLayout item = new FrameLayout(getContext());
        LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(0, dp(36), 1f);
        itemLp.setMarginStart(dp(8));
        item.setLayoutParams(itemLp);
        item.setClickable(true);
        item.setFocusable(true);
        item.setBackground(createRippleBackground());

        ImageView icon = new ImageView(getContext());
        Drawable drawable = ContextCompat.getDrawable(getContext(), drawableRes);
        icon.setImageDrawable(drawable);
        int selectedColor = COUIContextUtil.getAttrColor(getContext(),
                com.coui.appcompat.R.attr.couiColorContainerTheme, 0xff2d7dff);
        int normalColor = ContextCompat.getColor(getContext(),
                R.color.coloros_folder_style_icon_normal);
        icon.setImageTintList(new ColorStateList(
                new int[][] {new int[] {android.R.attr.state_selected}, new int[0]},
                new int[] {selectedColor, normalColor}));
        icon.setContentDescription(getResources().getString(descriptionRes));
        FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(dp(32), dp(32), Gravity.CENTER);
        item.addView(icon, iconLp);
        item.setOnClickListener(view -> select(index));

        mItems[index] = item;
        mIcons[index] = icon;
        addView(item);
    }

    private Drawable createRippleBackground() {
        int rippleColor = COUIContextUtil.getAttrColor(getContext(),
                com.coui.appcompat.R.attr.couiColorPress, 0x14000000);
        GradientDrawable mask = new GradientDrawable();
        mask.setColor(Color.WHITE);
        mask.setCornerRadius(dp(12));
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), null, mask);
    }

    private int getSelectedIndex() {
        if (mInfo.hasOption(STYLE_HIERARCHICAL)) {
            return 2;
        }
        if (mInfo.hasOption(STYLE_FOUR_GRID)) {
            return 1;
        }
        return 0;
    }

    private void select(int index) {
        if (index == mSelectedIndex) {
            return;
        }
        int selectedFlag = index == 0 ? STYLE_NINE_GRID
                : index == 1 ? STYLE_FOUR_GRID : STYLE_HIERARCHICAL;
        mSelectedIndex = index;
        updateSelection();
        AbstractFloatingView.closeOpenContainer(mLauncher,
                AbstractFloatingView.TYPE_WIDGET_RESIZE_FRAME);
        AbstractFloatingView.closeOpenContainer(mLauncher,
                AbstractFloatingView.TYPE_ACTION_POPUP);
        // Decoded FolderManager performs conversion only after the 200 ms popup close.
        mFolderIcon.postDelayed(() -> {
            if (mFolderIcon.isAttachedToWindow()) {
                mFolderIcon.switchFolderStyle(selectedFlag);
            }
        }, 200L);
    }

    private void updateSelection() {
        for (int index = 0; index < mItems.length; index++) {
            boolean selected = index == mSelectedIndex;
            mItems[index].setSelected(selected);
            mIcons[index].setSelected(selected);
        }
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
