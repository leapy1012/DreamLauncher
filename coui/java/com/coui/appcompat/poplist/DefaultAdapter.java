package com.coui.appcompat.poplist;

import com.coui.appcompat.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Space;
import android.widget.TextView;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;
import com.coui.appcompat.contextutil.COUIContextUtil;
import com.coui.appcompat.darkmode.COUIDarkModeUtil;
import com.coui.appcompat.reddot.COUIHintRedDot;
import com.coui.appcompat.state.COUIMaskEffectDrawable;
import com.coui.appcompat.textutil.COUIChangeTextUtil;
import com.coui.appcompat.uiutil.UIUtil;
import java.util.List;
import java.util.Locale;
import java.util.Set;


public class DefaultAdapter extends BaseAdapter {
    private static final float DESCRIPTION_TEXT_SIZE_DP = 12.0f;
    private static final float FIRST_TITLE_SIZE_DP = 12.0f;
    private static final int FLAG_DESCRIPTION = 2;
    private static final int FLAG_HAS_SUB_MENU = 16;
    private static final int FLAG_HINT = 4;
    private static final int FLAG_ICON = 1;
    private static final int FLAG_STATE_ICON = 8;
    private static final String TAG = "DefaultAdapter";
    private static final int TITLE_MAX_LINE_WITHOUT_DESCRIPTION = 3;
    private static final int TITLE_MAX_LINE_WITH_DESCRIPTION = 2;
    private static final float TITLE_TEXT_SIZE_DP = 16.0f;
    public static final int TYPE_DIVIDER_DEFAULT = 1;
    public static final int TYPE_DIVIDER_GROUP = 2;
    public static final int TYPE_DIVIDER_HEADER = 5;
    public static final int TYPE_ITEM_CUSTOM = 3;
    public static final int TYPE_ITEM_DEFAULT = 0;
    public static final int TYPE_ITEM_HEADER = 4;
    private static final int VIEW_TYPE_COUNT = 6;
    private int mAlertColor;
    private final Context mContext;
    private int mDefaultDividerHeight;
    private int mDefaultDividerMarginHorizontal;
    private int mDefaultDividerMarginStartWithIcon;
    private int mDescriptionColor;
    private int mGroupDividerHeight;
    private boolean mHasSubMenuHeader;
    private ColorStateList mItemTintList;
    private List<PopupListItem> mMenuItemList;
    private int mPopupListItemMinHeight;
    private int mPopupListItemPaddingVertical;
    private int mPopupListMenuItemMinHeight;
    private int mPopupListPaddingVertical;
    private ListItemMaskEffectDrawable mSharedDrawable;
    private ColorStateList mStateIconTintList;
    private static final int[] STATE_SELECTED = {16842910, 16842913};
    private static final int[] STATE_DISABLED = {-16842910};
    private static final Drawable DEFAULT_TRANSPARENT_HOLDER = new ColorDrawable(0);
    private static final Typeface MEDIUM_TYPEFACE = Typeface.create(COUIChangeTextUtil.MEDIUM_FONT, 0);
    private final View.AccessibilityDelegate mAccessibilityDelegate = new View.AccessibilityDelegate() {
        @Override
        public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfo accessibilityNodeInfo) {
            super.onInitializeAccessibilityNodeInfo(view, accessibilityNodeInfo);
            accessibilityNodeInfo.setClassName("");
        }
    };
    private int mPrivateFlags = 0;
    private boolean mIsAdaptiveFontSize = false;
    private boolean mIsFixedFontSize = false;
    private Set<Integer> mGroupSets = null;

    public static class HeaderViewHolder {
        TextView mTitle;


        public void initViewHolder(View view) {
            this.mTitle = (TextView) view.findViewById(R.id.popup_list_window_header_item_title);
        }
    }

    public static class ViewHolder {
        CheckBox mCheckbox;
        TextView mDescription;
        Space mHintEndGap;
        LinearLayout mHintLayout;
        ImageView mIcon;
        TextView mTitle;
        Space mTitleEndGap;


        public void initViewHolder(View view) {
            this.mIcon = (ImageView) view.findViewById(R.id.popup_list_window_item_icon);
            this.mTitle = (TextView) view.findViewById(R.id.popup_list_window_item_title);
            this.mDescription = (TextView) view.findViewById(R.id.popup_list_window_item_description);
            this.mTitleEndGap = (Space) view.findViewById(R.id.popup_list_window_item_title_end_gap);
            this.mHintLayout = (LinearLayout) view.findViewById(R.id.popup_list_window_item_hint_layout);
            this.mHintEndGap = (Space) view.findViewById(R.id.popup_list_window_item_hint_end_gap);
            this.mCheckbox = (CheckBox) view.findViewById(R.id.popup_list_window_item_state_icon);
        }
    }

    public DefaultAdapter(Context context, List<PopupListItem> list) {
        this.mContext = context;
        setItemList(list);
        Resources resources = context.getResources();
        this.mDefaultDividerHeight = resources.getDimensionPixelSize(R.dimen.coui_popup_list_divider_height);
        this.mGroupDividerHeight = resources.getDimensionPixelSize(R.dimen.coui_popup_list_group_divider_height);
        this.mPopupListPaddingVertical = resources.getDimensionPixelSize(R.dimen.coui_popup_list_padding_vertical);
        this.mPopupListItemPaddingVertical = resources.getDimensionPixelSize(R.dimen.coui_popup_list_window_item_padding_top_and_bottom);
        this.mPopupListMenuItemMinHeight = resources.getDimensionPixelSize(R.dimen.coui_popup_list_window_header_item_min_height);
        this.mPopupListItemMinHeight = resources.getDimensionPixelSize(R.dimen.coui_popup_list_window_item_min_height);
        this.mDefaultDividerMarginStartWithIcon = resources.getDimensionPixelSize(R.dimen.coui_popup_list_default_divider_margin_start_with_icon);
        this.mDefaultDividerMarginHorizontal = resources.getDimensionPixelSize(R.dimen.coui_popup_list_default_divider_margin_horizontal);
        this.mItemTintList = UIUtil.getColorStateListCompatNoCache(context, R.color.coui_popup_list_window_item_tint_selector);
        this.mStateIconTintList = UIUtil.getColorStateListCompatNoCache(context, R.color.coui_popup_list_window_item_status_icon_tint_selector);
        this.mAlertColor = COUIContextUtil.getAttrColor(context, R.attr.couiColorError, R.color.coui_color_error);
        this.mDescriptionColor = COUIContextUtil.getAttrColor(context, R.attr.couiColorLabelSecondary, R.color.coui_color_secondary_neutral);
    }

    private void configItemVerticalPadding(View view, int i2) {
        if (this.mMenuItemList.size() == 1) {
            view.setMinimumHeight(this.mPopupListItemMinHeight + (this.mPopupListPaddingVertical * 2));
            view.setPadding(view.getPaddingStart(), this.mPopupListItemPaddingVertical + this.mPopupListPaddingVertical, view.getPaddingEnd(), this.mPopupListItemPaddingVertical + this.mPopupListPaddingVertical);
        } else if (i2 == 0) {
            view.setMinimumHeight(this.mPopupListItemMinHeight + this.mPopupListPaddingVertical);
            view.setPadding(view.getPaddingStart(), this.mPopupListItemPaddingVertical + this.mPopupListPaddingVertical, view.getPaddingEnd(), this.mPopupListItemPaddingVertical);
        } else if (i2 == this.mMenuItemList.size() - 1) {
            view.setMinimumHeight(this.mPopupListItemMinHeight + this.mPopupListPaddingVertical);
            view.setPadding(view.getPaddingStart(), this.mPopupListItemPaddingVertical, view.getPaddingEnd(), this.mPopupListItemPaddingVertical + this.mPopupListPaddingVertical);
        } else {
            view.setMinimumHeight(this.mPopupListItemMinHeight);
            view.setPadding(view.getPaddingStart(), this.mPopupListItemPaddingVertical, view.getPaddingEnd(), this.mPopupListItemPaddingVertical);
        }
    }

    @SuppressLint({"ClickableViewAccessibility"})
    private void configStateEffectBackground(View view, PopupListItem popupListItem) {
        ListItemMaskEffectDrawable listItemMaskEffectDrawable;
        if (popupListItem != null && popupListItem.hasSubMenu() && popupListItem.getGroupState() != 2 && !(view.getBackground() instanceof ListItemMaskEffectDrawable)) {
            view.setBackground(new ListItemMaskEffectDrawable(this.mContext, 1, null));
        } else if (popupListItem != null && popupListItem.hasSubMenu() && popupListItem.getGroupState() == 2 && (listItemMaskEffectDrawable = this.mSharedDrawable) != null) {
            view.setBackground(listItemMaskEffectDrawable.getChild());
        } else if (!(view.getBackground() instanceof COUIMaskEffectDrawable)) {
            COUIMaskEffectDrawable cOUIMaskEffectDrawable = new COUIMaskEffectDrawable(this.mContext, 1);
            cOUIMaskEffectDrawable.enableFocusedState(false);
            cOUIMaskEffectDrawable.setIsRoundStyle(false);
            cOUIMaskEffectDrawable.setTouchExited();
            view.setBackground(cOUIMaskEffectDrawable);
        }
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public final boolean onTouch(View view2, MotionEvent motionEvent) {
                return DefaultAdapter.lambda$configStateEffectBackground$0(view2, motionEvent);
            }
        });
    }

    public static int dataIndexToRealPosition(int i2) {
        return i2 * 2;
    }

    @Deprecated
    public static final int dataindexToRealPosition(int i2) {
        return (i2 * 2) - 1;
    }

    private static View.AccessibilityDelegate getAccessibilityDelegate(final int i2) {
        return new View.AccessibilityDelegate() {
            @Override
            public boolean performAccessibilityAction(View view, int i6, Bundle bundle) {
                super.performAccessibilityAction(view, i6, bundle);
                if (i6 != 16) {
                    return true;
                }
                ViewParent parent = view.getParent();
                if (!(parent instanceof COUITouchListView)) {
                    return true;
                }
                COUITouchListView cOUITouchListView = (COUITouchListView) parent;
                int firstVisiblePosition = i2 - cOUITouchListView.getFirstVisiblePosition();
                cOUITouchListView.performItemClick(cOUITouchListView.getChildAt(firstVisiblePosition), firstVisiblePosition, cOUITouchListView.getItemIdAtPosition(firstVisiblePosition));
                return true;
            }
        };
    }

    private View getCustomItemView(int i2, View view, ViewGroup viewGroup) {
        PopupListItem popupListItem = this.mMenuItemList.get(realPositionToDataIndex(i2));
        View customItemView = popupListItem.getCustomItemView();
        if (customItemView == null) {
            Log.e(TAG, "Popup list item custom view is null! Return an empty view.");
            customItemView = new View(viewGroup.getContext());
        }
        if (view == null) {
            customItemView.setClickable(true);
            view = customItemView;
        }
        view.setAccessibilityDelegate(getAccessibilityDelegate(i2));
        configStateEffectBackground(view, popupListItem);
        view.setEnabled(popupListItem.isEnable());
        return customItemView;
    }

    private InsetDrawable getDefaultDividerDrawable(boolean z6) {
        boolean zIsLayoutRTL = isLayoutRTL();
        int i2 = (zIsLayoutRTL || z6 || !hasIcon()) ? this.mDefaultDividerMarginHorizontal : this.mDefaultDividerMarginStartWithIcon;
        int i6 = i2;
        int i10 = (zIsLayoutRTL && !z6 && hasIcon()) ? this.mDefaultDividerMarginStartWithIcon : this.mDefaultDividerMarginHorizontal;
        return new InsetDrawable((Drawable) new ColorDrawable(COUIContextUtil.getAttrColor(this.mContext, R.attr.couiColorDivider)), i6, 0, i10, 0);
    }

    private View getDefaultDividerView(View view, boolean z6) {
        if (view != null) {
            // Leapy added 2026-07-29: Divider rows participate in COUI's press
            // alpha animation. Always normalize recycled rows during binding
            // so interrupted popup dismissal cannot retain alpha zero.
            view.setAlpha(1.0f);
            return view;
        }
        View view2 = new View(this.mContext);
        ViewCompat.setImportantForAccessibility(view2, 2);
        COUIDarkModeUtil.setForceDarkAllow(view2, false);
        view2.setBackground(getDefaultDividerDrawable(z6));
        view2.setLayoutParams(new ViewGroup.LayoutParams(-1, this.mDefaultDividerHeight));
        view2.setFocusable(false);
        return view2;
    }

    private View getDefaultItemView(int i2, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        int iRealPositionToDataIndex = realPositionToDataIndex(i2);
        if (view == null || !(view.getTag() instanceof ViewHolder)) {
            ViewHolder viewHolder2 = new ViewHolder();
            View viewInflate = LayoutInflater.from(this.mContext).inflate(R.layout.coui_popup_list_window_item, viewGroup, false);
            viewHolder2.initViewHolder(viewInflate);
            viewInflate.setClickable(true);
            CheckBox checkBox = viewHolder2.mCheckbox;
            if (checkBox != null) {
                checkBox.setAccessibilityDelegate(this.mAccessibilityDelegate);
                viewHolder2.mCheckbox.setBackground(null);
            }
            viewInflate.setTag(viewHolder2);
            viewHolder = viewHolder2;
            view = viewInflate;
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }
        view.setAccessibilityDelegate(getAccessibilityDelegate(i2));
        // Submenu transition dims rows via View alpha; normalize on every bind so
        // a recycled row cannot stay at the faded "disabled" look.
        view.setAlpha(1.0f);
        configItemVerticalPadding(view, iRealPositionToDataIndex);
        PopupListItem popupListItem = this.mMenuItemList.get(iRealPositionToDataIndex);
        setIcon(viewHolder.mIcon, popupListItem);
        setTitle(viewHolder.mTitle, popupListItem, i2);
        setDescription(viewHolder.mDescription, popupListItem);
        setGap(viewHolder, popupListItem);
        setHint(viewHolder.mHintLayout, popupListItem);
        setStateIcon(viewHolder.mCheckbox, popupListItem);
        view.setEnabled(popupListItem.isEnable());
        configStateEffectBackground(view, popupListItem);
        return view;
    }

    private View getDividerView(int i2, View view, int i6) {
        View defaultDividerView = i6 != 2 ? i6 != 5 ? getDefaultDividerView(view, false) : getDefaultDividerView(view, true) : (this.mHasSubMenuHeader && i2 == 1) ? getDefaultDividerView(view, false) : getGroupDividerView(view);
        defaultDividerView.setFocusable(false);
        return defaultDividerView;
    }

    private View getGroupDividerView(View view) {
        if (view != null) {
            // Leapy added 2026-07-29: Group dividers share the same recycled
            // view path as normal dividers and require the same alpha reset.
            view.setAlpha(1.0f);
            // Leapy end
            return view;
        }
        View view2 = new View(this.mContext);
        ViewCompat.setImportantForAccessibility(view2, 2);
        COUIDarkModeUtil.setForceDarkAllow(view2, false);
        view2.setBackgroundColor(ResourcesCompat.getColor(this.mContext.getResources(), R.color.coui_popup_list_group_divider_color, this.mContext.getTheme()));
        view2.setLayoutParams(new ViewGroup.LayoutParams(-1, this.mGroupDividerHeight));
        return view2;
    }

    private View getHeaderItemView(int i2, View view, ViewGroup viewGroup) {
        HeaderViewHolder headerViewHolder;
        int iRealPositionToDataIndex = realPositionToDataIndex(i2);
        if (view == null || !(view.getTag() instanceof HeaderViewHolder)) {
            HeaderViewHolder headerViewHolder2 = new HeaderViewHolder();
            View viewInflate = LayoutInflater.from(this.mContext).inflate(R.layout.coui_popup_list_window_header_item, viewGroup, false);
            headerViewHolder2.initViewHolder(viewInflate);
            viewInflate.setClickable(false);
            viewInflate.setTag(headerViewHolder2);
            headerViewHolder = headerViewHolder2;
            view = viewInflate;
        } else {
            headerViewHolder = (HeaderViewHolder) view.getTag();
        }
        view.setAccessibilityDelegate(getAccessibilityDelegate(i2));
        configItemVerticalPadding(view, iRealPositionToDataIndex);
        view.setMinimumHeight(this.mPopupListMenuItemMinHeight);
        PopupListItem popupListItem = this.mMenuItemList.get(iRealPositionToDataIndex);
        headerViewHolder.mTitle.setText(popupListItem.getTitle());
        if (!TextUtils.isEmpty(popupListItem.getTitleContentDescription())) {
            headerViewHolder.mTitle.setContentDescription(popupListItem.getTitleContentDescription());
        }
        if (this.mIsFixedFontSize) {
            headerViewHolder.mTitle.setTextSize(1, 12.0f);
        } else if (this.mIsAdaptiveFontSize) {
            COUIChangeTextUtil.adaptFontSize(headerViewHolder.mTitle, 4);
        } else {
            COUIChangeTextUtil.adaptFontSize(headerViewHolder.mTitle, 5);
        }
        return view;
    }

    private View getItemView(int i2, View view, ViewGroup viewGroup, int i6) {
        return i6 != 3 ? i6 != 4 ? getDefaultItemView(i2, view, viewGroup) : getHeaderItemView(i2, view, viewGroup) : getCustomItemView(i2, view, viewGroup);
    }

    private View getRedDotHint(PopupListItem popupListItem) {
        COUIHintRedDot cOUIHintRedDot = new COUIHintRedDot(new ContextThemeWrapper(this.mContext, R.style.Widget_COUI_COUIHintRedDot_Small));
        if (TextUtils.isEmpty(popupListItem.getRedDotText())) {
            cOUIHintRedDot.setPointNumber(popupListItem.getRedDotAmount());
            int redDotAmount = popupListItem.getRedDotAmount();
            if (redDotAmount == -1) {
                cOUIHintRedDot.setPointMode(0);
            } else if (redDotAmount != 0) {
                cOUIHintRedDot.setPointMode(2);
            } else {
                cOUIHintRedDot.setPointMode(1);
            }
        } else {
            cOUIHintRedDot.setPointMode(2);
            cOUIHintRedDot.setPointText(popupListItem.getRedDotText());
        }
        return cOUIHintRedDot;
    }

    private int getTintColorByState(ColorStateList colorStateList, PopupListItem popupListItem) {
        return getTintColorByState(colorStateList, popupListItem, false);
    }

    public static boolean isDataIndex(int i2) {
        return i2 % 2 == 0;
    }

    private boolean isLayoutRTL() {
        return TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == 1;
    }


    public static boolean lambda$configStateEffectBackground$0(View view, MotionEvent motionEvent) {
        if (view == null) {
            return false;
        }
        Drawable background = view.getBackground();
        if (motionEvent.getActionMasked() == 0 && (background instanceof COUIMaskEffectDrawable)) {
            ((COUIMaskEffectDrawable) background).setTouchEntered();
        }
        if ((motionEvent.getActionMasked() == 1 || motionEvent.getActionMasked() == 3) && (background instanceof COUIMaskEffectDrawable)) {
            ((COUIMaskEffectDrawable) background).setTouchExited();
        }
        return false;
    }

    private boolean listNotEmpty(List<?> list) {
        return (list == null || list.isEmpty()) ? false : true;
    }

    public static int realPositionToDataIndex(int i2) {
        return i2 / 2;
    }

    @Deprecated
    public static final int realPositionToDataindex(int i2) {
        return realPositionToDataIndex(i2);
    }

    private void setDescription(TextView textView, PopupListItem popupListItem) {
        if (!(!TextUtils.isEmpty(popupListItem.getDescription()))) {
            textView.setVisibility(8);
            return;
        }
        textView.setVisibility(0);
        textView.setTextAppearance(R.style.couiTextBodyXS);
        textView.setText(popupListItem.getDescription());
        if (this.mIsFixedFontSize) {
            textView.setTextSize(1, 12.0f);
        } else if (this.mIsAdaptiveFontSize) {
            COUIChangeTextUtil.adaptFontSize(textView, 4);
        }
        textView.setTextColor(this.mDescriptionColor);
        textView.setMaxLines(2);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        if (TextUtils.isEmpty(popupListItem.getDescriptionContentDescription())) {
            return;
        }
        textView.setContentDescription(popupListItem.getDescriptionContentDescription());
    }

    private void setGap(ViewHolder viewHolder, PopupListItem popupListItem) {
        boolean z6 = popupListItem.getHintType() != -1;
        boolean z10 = popupListItem.getStateIcon() != null || popupListItem.getStateIconId() != 0 || popupListItem.hasSubMenu() || (popupListItem.getStateIcon() == null && popupListItem.getStateIconId() == 0 && popupListItem.isChecked());
        if (!z6 && !z10) {
            viewHolder.mTitleEndGap.setVisibility(8);
            return;
        }
        viewHolder.mTitleEndGap.setVisibility(4);
        if (z6 && z10) {
            viewHolder.mHintEndGap.setVisibility(4);
        } else {
            viewHolder.mHintEndGap.setVisibility(8);
        }
    }

    private void setHint(ViewGroup viewGroup, PopupListItem popupListItem) {
        if (popupListItem.isEnable()) {
            viewGroup.setVisibility(0);
        } else {
            viewGroup.setVisibility(8);
        }
        viewGroup.removeAllViews();
        if (popupListItem.getHintType() == 0) {
            viewGroup.addView(getRedDotHint(popupListItem), new ViewGroup.LayoutParams(-2, -2));
            return;
        }
        if (popupListItem.getHintType() != 1 || popupListItem.getCustomHintView() == null) {
            return;
        }
        ViewParent parent = popupListItem.getCustomHintView().getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(popupListItem.getCustomHintView());
        }
        viewGroup.addView(popupListItem.getCustomHintView());
    }

    private void setIcon(ImageView imageView, PopupListItem popupListItem) {
        if ((this.mPrivateFlags & 1) != 0) {
            imageView.setVisibility(0);
            Drawable icon = popupListItem.getIcon() != null ? popupListItem.getIcon() : popupListItem.getIconId() != 0 ? ResourcesCompat.getDrawable(this.mContext.getResources(), popupListItem.getIconId(), this.mContext.getTheme()) : null;
            if ((popupListItem.getForceTint() & 1) != 0) {
                setIconTint(icon, this.mItemTintList, popupListItem);
            }
            imageView.setSelected(popupListItem.isChecked());
            imageView.setImageDrawable(icon);
        } else {
            imageView.setVisibility(8);
        }
        imageView.setEnabled(popupListItem.isEnable());
    }

    private void setIconTint(Drawable drawable, ColorStateList colorStateList, PopupListItem popupListItem) {
        setIconTint(drawable, colorStateList, popupListItem, false);
    }

    private void setStateIcon(CheckBox checkBox, PopupListItem popupListItem) {
        if (popupListItem.getStateIcon() != null || popupListItem.getStateIconId() != 0 || popupListItem.hasSubMenu() || popupListItem.isChecked()) {
            checkBox.setVisibility(0);
            Drawable drawableF = DEFAULT_TRANSPARENT_HOLDER;
            if (popupListItem.hasSubMenu()) {
                drawableF = ResourcesCompat.getDrawable(this.mContext.getResources(), R.drawable.coui_list_expandable_indicator, this.mContext.getTheme());
                setIconTint(drawableF, this.mStateIconTintList, popupListItem, true);
            } else {
                if (popupListItem.getStateIcon() != null) {
                    drawableF = popupListItem.getStateIcon();
                } else if (popupListItem.getStateIconId() != 0) {
                    drawableF = ResourcesCompat.getDrawable(this.mContext.getResources(), popupListItem.getStateIconId(), this.mContext.getTheme());
                } else if (popupListItem.isChecked()) {
                    drawableF = ResourcesCompat.getDrawable(this.mContext.getResources(), R.drawable.coui_menu_ic_checkbox, this.mContext.getTheme());
                }
                if ((popupListItem.getForceTint() & 4) != 0) {
                    setIconTint(drawableF, this.mStateIconTintList, popupListItem, true);
                }
            }
            checkBox.setButtonDrawable(drawableF);
            checkBox.setChecked(popupListItem.isChecked());
        } else {
            checkBox.setVisibility(8);
        }
        checkBox.setEnabled(popupListItem.isEnable());
    }

    private void setTextViewTint(TextView textView, ColorStateList colorStateList, PopupListItem popupListItem) {
        if (textView == null) {
            return;
        }
        textView.setTextColor(getTintColorByState(colorStateList, popupListItem));
    }

    private void setTitle(TextView textView, PopupListItem popupListItem, int i2) {
        boolean z6 = !TextUtils.isEmpty(popupListItem.getDescription());
        textView.setTextAppearance(R.style.couiTextBodyL);
        if (popupListItem.getGroupState() == 2 && i2 == 0) {
            textView.setTypeface(MEDIUM_TYPEFACE);
        } else {
            textView.setTypeface(null);
        }
        textView.setText(popupListItem.getTitle());
        if (!TextUtils.isEmpty(popupListItem.getTitleContentDescription())) {
            textView.setContentDescription(popupListItem.getTitleContentDescription());
        }
        if (this.mIsFixedFontSize) {
            textView.setTextSize(1, 16.0f);
        } else if (this.mIsAdaptiveFontSize) {
            COUIChangeTextUtil.adaptFontSize(textView, 4);
        } else {
            COUIChangeTextUtil.adaptFontSize(textView, 5);
        }
        if (z6) {
            textView.setMaxLines(2);
        } else {
            textView.setMaxLines(3);
        }
        textView.setEllipsize(TextUtils.TruncateAt.END);
        if ((popupListItem.getForceTint() & 2) != 0) {
            setTextViewTint(textView, this.mItemTintList, popupListItem);
        } else if (popupListItem.getTitleColorList() != null) {
            textView.setTextColor(popupListItem.getTitleColorList());
        }
        textView.setSelected(popupListItem.isChecked());
        textView.setEnabled(popupListItem.isEnable());
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public int getCount() {
        if (listNotEmpty(this.mMenuItemList)) {
            return dataIndexToRealPosition(this.mMenuItemList.size()) - 1;
        }
        return 0;
    }

    public int getDividerHeight(int i2) {
        if (i2 == 1) {
            return this.mDefaultDividerHeight;
        }
        if (i2 == 2) {
            return this.mGroupDividerHeight;
        }
        return 0;
    }

    @Override
    public Object getItem(int i2) {
        if (realPositionToDataIndex(i2) >= this.mMenuItemList.size()) {
            return null;
        }
        return this.mMenuItemList.get(realPositionToDataIndex(i2));
    }

    @Override
    public long getItemId(int i2) {
        return i2;
    }

    @Override
    public int getItemViewType(int i2) {
        if (!isDataIndex(i2)) {
            if (isHeaderPosition(i2)) {
                return 5;
            }
            return isGroupIndex(i2) ? 2 : 1;
        }
        int iRealPositionToDataIndex = realPositionToDataIndex(i2);
        if (!listNotEmpty(this.mMenuItemList) || iRealPositionToDataIndex >= this.mMenuItemList.size()) {
            return 0;
        }
        int itemType = this.mMenuItemList.get(iRealPositionToDataIndex).getItemType();
        if (itemType != 2) {
            return itemType != 3 ? 0 : 4;
        }
        return 3;
    }

    @Override
    public View getView(int i2, View view, ViewGroup viewGroup) {
        int itemViewType = getItemViewType(i2);
        if (itemViewType != 0) {
            if (itemViewType != 1 && itemViewType != 2) {
                if (itemViewType != 3 && itemViewType != 4) {
                    if (itemViewType != 5) {
                        Log.e(TAG, "View type error!");
                        return null;
                    }
                }
            }
            return getDividerView(i2, view, itemViewType);
        }
        return getItemView(i2, view, viewGroup, itemViewType);
    }

    @Override
    public int getViewTypeCount() {
        return 6;
    }

    public boolean hasIcon() {
        return (this.mPrivateFlags & 1) != 0;
    }

    public boolean hasSubMenu() {
        return (this.mPrivateFlags & 16) != 0;
    }

    @Override
    public boolean isEnabled(int i2) {
        return isDataIndex(i2);
    }

    public boolean isGroupIndex(int i2) {
        Set<Integer> set = this.mGroupSets;
        return (set == null || !set.contains(Integer.valueOf((i2 + 1) / 2)) || isHeaderPosition(i2)) ? false : true;
    }

    public boolean isHeaderPosition(int i2) {
        if (i2 <= 0) {
            return false;
        }
        return listNotEmpty(this.mMenuItemList) && this.mMenuItemList.get(realPositionToDataIndex(i2 - 1)).getItemType() == 3;
    }

    public void setAdapterFontSize(boolean z6) {
        this.mIsAdaptiveFontSize = z6;
    }

    @Deprecated
    public void setGroup(int[] iArr) {
    }

    public void setGroupSets(Set<Integer> set) {
        this.mGroupSets = set;
    }

    public void setIsFixedFontSize(boolean z6) {
        this.mIsFixedFontSize = z6;
    }

    public void setItemList(List<PopupListItem> list) {
        this.mMenuItemList = list;
        if (list == null) {
            return;
        }
        PopupListItem popupListItem = list.get(0);
        this.mHasSubMenuHeader = (popupListItem == null || popupListItem.getGroupState() == 0) ? false : true;
        this.mPrivateFlags = 0;
        for (PopupListItem popupListItem2 : this.mMenuItemList) {
            if (popupListItem2 != null) {
                if (popupListItem2.getIconId() != 0 || popupListItem2.getIcon() != null) {
                    this.mPrivateFlags |= 1;
                }
                if (!TextUtils.isEmpty(popupListItem2.getDescription())) {
                    this.mPrivateFlags |= 2;
                }
                if (popupListItem2.getHintType() != -1) {
                    this.mPrivateFlags |= 4;
                }
                if (popupListItem2.getStateIconId() != 0 || popupListItem2.getStateIcon() != null) {
                    this.mPrivateFlags |= 8;
                }
                if (popupListItem2.hasSubMenu()) {
                    this.mPrivateFlags |= 16;
                }
            }
        }
    }

    @Deprecated
    public void setItemTextColor(ColorStateList colorStateList) {
    }

    @Deprecated
    public void setListView(ListView listView) {
    }

    @Deprecated
    public void setMaxLine(int i2) {
    }

    @Deprecated
    public void setSelectItemColor(int i2) {
    }

    public void setSharedBackground(ListItemMaskEffectDrawable listItemMaskEffectDrawable) {
        this.mSharedDrawable = listItemMaskEffectDrawable;
    }

    private int getTintColorByState(ColorStateList colorStateList, PopupListItem popupListItem, boolean z6) {
        return popupListItem.isEnable() ? popupListItem.getItemType() == 0 ? (!z6 || popupListItem.getGroupState() == 0) ? popupListItem.isChecked() ? colorStateList.getColorForState(STATE_SELECTED, R.color.coui_color_error) : colorStateList.getDefaultColor() : colorStateList.getColorForState(STATE_SELECTED, R.color.coui_color_error) : popupListItem.getItemType() == 1 ? this.mAlertColor : colorStateList.getDefaultColor() : colorStateList.getColorForState(STATE_DISABLED, R.color.coui_color_error);
    }

    private void setIconTint(Drawable drawable, ColorStateList colorStateList, PopupListItem popupListItem, boolean z6) {
        if (drawable == null) {
            return;
        }
        drawable.setTint(getTintColorByState(colorStateList, popupListItem, z6));
    }
}
