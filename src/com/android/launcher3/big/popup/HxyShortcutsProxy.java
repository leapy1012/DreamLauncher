package com.android.launcher3.big.popup;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.Launcher;
import com.android.launcher3.dragndrop.DragOptions;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.popup.PopupContainerWithArrow;
import com.android.launcher3.popup.SystemShortcut;
import com.android.launcher3.widget.LauncherAppWidgetHostView;
import com.android.launcher3.folder.large.HxyLargeFolderIcon;
import java.util.List;

public class HxyShortcutsProxy {
    public static DragOptions.PreDragCondition startLongPressActionFolder(HxyLargeFolderIcon view) {
        PopupContainerWithArrow popup = PopupContainerWithArrow.showForFolder(view);
        if (popup != null) {
            return popup.createPreDragCondition(true);
        }
        return null;
    }

    public static int getWidgetContentHeight(LauncherAppWidgetHostView view) {
        int contentHeight = 0;
        int count = view.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = view.getChildAt(i);
            if (child instanceof ViewGroup) {
                ViewGroup childGroup = (ViewGroup) child;
                int childCount = childGroup.getChildCount();
                for (int j = 0; j < childCount; j++) {
                    contentHeight += getWidgetContentHeight(childGroup.getChildAt(j));
                }
            } else {
                contentHeight += child.getMeasuredHeight();
            }
        }
        return contentHeight;
    }

    private static int getWidgetContentHeight(View child) {
        int contentHeight = child.getMeasuredHeight();
        ViewGroup.LayoutParams params = child.getLayoutParams();
        if (params instanceof ViewGroup.MarginLayoutParams) {
            return contentHeight + ((ViewGroup.MarginLayoutParams) params).topMargin;
        }
        return contentHeight;
    }
}