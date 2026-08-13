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
    public static final int STYLE_NINE_GRID = 0x100;
    public static final int STYLE_FOUR_GRID = 0x200;
    public static final int STYLE_HIERARCHICAL = 0x400;
    private static final int STYLE_MASK =
            STYLE_NINE_GRID | STYLE_FOUR_GRID | STYLE_HIERARCHICAL;

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
        if ((mInfo.options & STYLE_NINE_GRID) != 0) {
            return 0;
        }
        if ((mInfo.options & STYLE_HIERARCHICAL) != 0) {
            return 2;
        }
        return 0;
    }

    private void select(int index) {
        if (index == mSelectedIndex) {
            return;
        }
        mSelectedIndex = index;
        int selectedFlag = index == 0 ? STYLE_NINE_GRID
                : index == 1 ? STYLE_FOUR_GRID : STYLE_HIERARCHICAL;
        mInfo.options = (mInfo.options & ~STYLE_MASK) | selectedFlag;
        mLauncher.getModelWriter().updateItemInDatabase(mInfo);
        updateSelection();
        mFolderIcon.setColorOsFolderStyle(selectedFlag);
    }

    private void updateSelection() {
        int selectedColor = COUIContextUtil.getAttrColor(getContext(),
                com.coui.appcompat.R.attr.couiColorContainerTheme, 0xff0066ff);
        int normalColor = 0xe6000000;
        for (int index = 0; index < mItems.length; index++) {
            boolean selected = index == mSelectedIndex;
            mItems[index].setSelected(selected);
            mItems[index].setBackgroundColor(selected ? 0x0d000000 : Color.TRANSPARENT);
            mIcons[index].setColorFilter(selected ? selectedColor : normalColor);
        }
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
