package com.android.launcher3.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class RadioGroupLinearLayout extends LinearLayout {

    private static final String TAG = "RadioGroupLinearLayout";
    private int iconHeight = 80;
    private int[] iconResIds;
    private int[] iconSelectedResIds;
    private int iconWidth = 80;
    private int indexPosiotion = -1;
    private boolean isTextShow = true;
    private ImageView lastCheckedIcon;
    private boolean mEnable = true;
    public onItemClickListener onItemClickListener;
    private int preIndex = -1;
    private int[] titles;

    public interface onItemClickListener {
        void onItemClick(int i);
    }

    public RadioGroupLinearLayout(Context context) {
        super(context);
        init();
    }

    public RadioGroupLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RadioGroupLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOrientation(LinearLayout.VERTICAL);
    }

    public void setIcons(int[] iconResIds2, int[] iconSelectedResIds2, int[] titles2, int default_index, int iconWidth2, int iconHeight2) {
        iconResIds = iconResIds2;
        iconSelectedResIds = iconSelectedResIds2;
        titles = titles2;
        if (iconSelectedResIds2 == null) {
            iconSelectedResIds = iconResIds2;
        }
        if (titles2 == null) {
            isTextShow = false;
        } else {
            isTextShow = true;
        }
        setIconsSize(iconWidth2, iconHeight2);
        createIconRadioButtons(default_index);
    }

    public void setIconsSize(int iconWidth2, int iconHeight2) {
        iconWidth = dp2px(getContext(), (float) iconWidth2);
        iconHeight = dp2px(getContext(), (float) iconHeight2);
    }

    public static int dp2px(Context context, float dp) {
        return (int) ((dp * context.getResources().getDisplayMetrics().density) + 0.5f);
    }

    private void createIconRadioButtons(int default_index) {
        final int i = default_index;
        removeAllViews();
        int rowCount = (int) Math.ceil(((double) iconResIds.length) / ((double) 4));
        int rowSpacing = dp2px(getContext(), 30.0f);
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            LinearLayout rowLayout = new LinearLayout(getContext());
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            if (rowIndex < rowCount - 1) {
                rowParams.bottomMargin = rowSpacing;
            }
            rowLayout.setLayoutParams(rowParams);
            for (int colIndex = 0; colIndex < 4; colIndex++) {
                int iconIndex = (rowIndex * 4) + colIndex;
                if (iconIndex >= iconResIds.length) {
                    break;
                }
                final ImageView icon = new ImageView(getContext());
                TextView mTextView = new TextView(getContext());
                mTextView.setTextSize(2, 13.0f);
                LinearLayout itemLayout = new LinearLayout(getContext());
                itemLayout.setOrientation(LinearLayout.VERTICAL);
                if (isTextShow) {
                    mTextView.setText(titles[iconIndex]);
                }
                mTextView.setGravity(1);
                if (!isTextShow) {
                    mTextView.setVisibility(View.GONE);
                }
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(iconWidth, !isTextShow ? iconHeight : (iconHeight * 2) + 5);
                params.weight = 1.0f;
                icon.setImageResource(iconResIds[iconIndex]);
                itemLayout.addView(icon);
                itemLayout.addView(mTextView);
                itemLayout.setLayoutParams(params);
                final int finalIconIndex = iconIndex;
                View.OnClickListener clickListener = new View.OnClickListener() {
                    public void onClick(View v) {
                        if (lastCheckedIcon != icon) {
                            if (lastCheckedIcon != null) {
                                if (preIndex == -1) {
                                    lastCheckedIcon.setImageResource(iconResIds[i]);
                                } else {
                                    lastCheckedIcon.setImageResource(iconResIds[preIndex]);
                                }
                            }
                            lastCheckedIcon = icon;
                            lastCheckedIcon.setImageResource(iconSelectedResIds[finalIconIndex]);
                            Log.e(TAG, "click----------finalI=" + finalIconIndex + "///preIndex=" + preIndex);
                            preIndex = finalIconIndex;
                            indexPosiotion = finalIconIndex;
                            if (onItemClickListener != null) {
                                onItemClickListener.onItemClick(finalIconIndex);
                            }
                        }
                    }
                };
                icon.setOnClickListener(clickListener);
                itemLayout.setOnClickListener(clickListener);
                rowLayout.addView(itemLayout);
                if (iconIndex == i) {
                    lastCheckedIcon = icon;
                    indexPosiotion = i;
                    icon.setImageResource(iconSelectedResIds[iconIndex]);
                }
            }
            addView(rowLayout);
        }
    }

    public int getIndexPosition() {
        return indexPosiotion;
    }

    public void setEnable(boolean enable) {
        mEnable = enable;
        ColorDrawable colorDrawable = new ColorDrawable(Color.parseColor("#88ffffff"));
        if (!enable) {
            setForeground(colorDrawable);
        } else {
            setForeground((Drawable) null);
        }
    }

    public void setOnItemClickListener(onItemClickListener onItemClickListener2) {
        onItemClickListener = onItemClickListener2;
    }
}