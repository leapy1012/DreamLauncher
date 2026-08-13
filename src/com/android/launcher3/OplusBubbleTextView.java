package com.android.launcher3;

import android.content.Context;
import android.util.AttributeSet;

import com.android.launcher3.model.data.PackageItemInfo;
import com.android.launcher3.views.DoubleShadowBubbleTextView;

/** OPPO widget-category label behavior used by OplusWidgetsListAdapter. */
public class OplusBubbleTextView extends DoubleShadowBubbleTextView {

    public OplusBubbleTextView(Context context) {
        this(context, null);
    }

    public OplusBubbleTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OplusBubbleTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // ColorOS restores font padding after BaseIcon disables it in XML.
        setIncludeFontPadding(true);
        if (getIconDisplay() == 2) {
            setLineSpacing(getResources().getDimension(
                    R.dimen.folder_item_title_line_space), 1f);
        } else if (getIconDisplay() == 1) {
            setLineSpacing(0f, 1f);
        } else {
            setLineSpacing(getResources().getDimension(
                    R.dimen.workspace_item_title_line_space), 1f);
        }
    }

    /** Mirrors BubbleTextViewExtImplOplus.applyFromPackageItemInfo in decoded OPPO Launcher. */
    public void applyFromPackageItemInfo(PackageItemInfo packageItemInfo) {
        applyIconAndLabel(packageItemInfo);
        setTag(packageItemInfo);
        verifyHighRes();
    }
}
