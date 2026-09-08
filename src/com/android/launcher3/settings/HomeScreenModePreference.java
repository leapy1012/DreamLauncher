package com.android.launcher3.settings;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;
import com.android.launcher3.LauncherStyle;
import com.android.launcher3.R;
import com.android.launcher3.allapps.coloros.ColorOsDrawerColumns;
import com.coui.appcompat.cardlist.COUICardListHelper;
import com.coui.appcompat.contextutil.COUIContextUtil;

/**
 * Side-by-side Standard / With drawer picker (Oppo {@code LauncherModelIntroductionPreference}).
 * Tapping a tile only selects it; the fragment Apply button commits the mode.
 */
public class HomeScreenModePreference extends Preference {

    public interface OnModeSelectedListener {
        void onModeSelected(int style);
    }

    private int mSelectedStyle = LauncherStyle.APP_DRAWER;
    private int mDrawerColumns = ColorOsDrawerColumns.DEFAULT;
    private OnModeSelectedListener mListener;
    private int mSelectColor;
    private int mUnselectedColor;

    public HomeScreenModePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public HomeScreenModePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HomeScreenModePreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        setLayoutResource(R.layout.home_screen_mode_picker);
        setSelectable(false);
        setPersistent(false);
        mSelectColor = COUIContextUtil.getAttrColor(getContext(),
                com.coui.appcompat.R.attr.couiColorPrimary);
        mUnselectedColor = getContext().getColor(R.color.coloros_text_tertiary);
        mDrawerColumns = ColorOsDrawerColumns.get(getContext());
        if (mDrawerColumns != ColorOsDrawerColumns.COLUMNS_FOUR
                && mDrawerColumns != ColorOsDrawerColumns.COLUMNS_FIVE) {
            mDrawerColumns = ColorOsDrawerColumns.DEFAULT;
        }
    }

    public void setOnModeSelectedListener(OnModeSelectedListener listener) {
        mListener = listener;
    }

    public int getSelectedStyle() {
        return mSelectedStyle;
    }

    public void setSelectedStyle(int style) {
        if (mSelectedStyle == style) {
            return;
        }
        mSelectedStyle = style;
        notifyChanged();
    }

    public void setDrawerColumns(int columns) {
        int next = columns == ColorOsDrawerColumns.COLUMNS_FOUR
                ? ColorOsDrawerColumns.COLUMNS_FOUR
                : ColorOsDrawerColumns.COLUMNS_FIVE;
        if (mDrawerColumns == next) {
            return;
        }
        mDrawerColumns = next;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        COUICardListHelper.setItemCardBackground(holder.itemView, COUICardListHelper.FULL);
        bindColumn(holder.itemView, R.id.home_screen_mode_standard, LauncherStyle.REGULAR);
        bindColumn(holder.itemView, R.id.home_screen_mode_drawer, LauncherStyle.APP_DRAWER);
    }

    private void bindColumn(View itemView, int columnId, int style) {
        View column = itemView.findViewById(columnId);
        if (column == null) {
            return;
        }
        LottieAnimationView preview = column.findViewById(R.id.home_screen_mode_preview);
        if (preview != null) {
            int anim = previewAnim(style);
            Object tag = preview.getTag();
            if (!(tag instanceof Integer) || (Integer) tag != anim) {
                preview.setTag(anim);
                preview.setAnimation(anim);
                preview.setRepeatCount(LottieDrawable.INFINITE);
                preview.playAnimation();
            }
        }
        applyColumnSelection(column, style);
        column.setOnClickListener(v -> {
            if (mSelectedStyle == style) {
                return;
            }
            mSelectedStyle = style;
            applyColumnSelection(itemView.findViewById(R.id.home_screen_mode_standard),
                    LauncherStyle.REGULAR);
            applyColumnSelection(itemView.findViewById(R.id.home_screen_mode_drawer),
                    LauncherStyle.APP_DRAWER);
            if (mListener != null) {
                mListener.onModeSelected(style);
            }
        });
    }

    private void applyColumnSelection(View column, int style) {
        if (column == null) {
            return;
        }
        boolean selected = mSelectedStyle == style;
        TextView title = column.findViewById(R.id.home_screen_mode_title);
        RadioButton radio = column.findViewById(R.id.home_screen_mode_radio);
        if (title != null) {
            title.setText(style == LauncherStyle.APP_DRAWER
                    ? R.string.home_screen_style_single
                    : R.string.home_screen_style_regular);
            title.setTextColor(selected ? mSelectColor : mUnselectedColor);
        }
        if (radio != null) {
            radio.setClickable(false);
            radio.setFocusable(false);
            radio.setButtonTintList(null);
            radio.setChecked(selected);
        }
    }

    private int previewAnim(int style) {
        if (style != LauncherStyle.APP_DRAWER) {
            return R.raw.launcher_standard_mode_preview_anim;
        }
        return mDrawerColumns == ColorOsDrawerColumns.COLUMNS_FOUR
                ? R.raw.launcher_mode_preview_anim
                : R.raw.launcher_mode_preview_anim_5columns;
    }
}
