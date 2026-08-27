package com.coui.appcompat.touchhelper;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeProviderCompat;
import androidx.customview.widget.ExploreByTouchHelper;
import java.util.List;


public class COUIViewExplorerByTouchHelper extends ExploreByTouchHelper {
    private static final String VIEW_LOG_TAG = "COUIViewExplorerByTouchHelper";
    private COUIViewTalkBalkInteraction mCOUIViewTalkBalkInteraction;
    private View mHostView;
    private final Rect mTempRect;

    public interface COUIViewTalkBalkInteraction {
        CharSequence getClassName();

        int getCurrentPosition();

        int getDisablePosition();

        void getItemBounds(int i2, Rect rect);

        int getItemCounts();

        CharSequence getItemDescription(int i2);

        int getVirtualViewAt(float f2, float f10);

        void performAction(int i2, int i6, boolean z6);
    }

    public COUIViewExplorerByTouchHelper(View view) {
        super(view);
        this.mTempRect = new Rect();
        this.mCOUIViewTalkBalkInteraction = null;
        this.mHostView = view;
    }

    private void getItemBounds(int i2, Rect rect) {
        if (i2 < 0 || i2 >= this.mCOUIViewTalkBalkInteraction.getItemCounts()) {
            return;
        }
        this.mCOUIViewTalkBalkInteraction.getItemBounds(i2, rect);
    }

    public void clearFocusedVirtualView() {
        int focusedVirtualView = getFocusedVirtualView();
        if (focusedVirtualView != Integer.MIN_VALUE) {
            AccessibilityNodeProviderCompat provider = getAccessibilityNodeProvider(this.mHostView);
            provider.performAction(focusedVirtualView, 128, null);
        }
    }

    @Override
    public int getVirtualViewAt(float f2, float f10) {
        int virtualViewAt = this.mCOUIViewTalkBalkInteraction.getVirtualViewAt(f2, f10);
        if (virtualViewAt >= 0) {
            return virtualViewAt;
        }
        return Integer.MIN_VALUE;
    }

    @Override
    public void getVisibleVirtualViews(List<Integer> list) {
        for (int i2 = 0; i2 < this.mCOUIViewTalkBalkInteraction.getItemCounts(); i2++) {
            list.add(Integer.valueOf(i2));
        }
    }

    @Override
    public boolean onPerformActionForVirtualView(int i2, int i6, Bundle bundle) {
        if (i6 != 16) {
            return false;
        }
        this.mCOUIViewTalkBalkInteraction.performAction(i2, 16, false);
        return true;
    }

    @Override
    public void onPopulateEventForVirtualView(int i2, AccessibilityEvent accessibilityEvent) {
        accessibilityEvent.setContentDescription(this.mCOUIViewTalkBalkInteraction.getItemDescription(i2));
    }

    @Override
    public void onPopulateNodeForVirtualView(int i2, AccessibilityNodeInfoCompat nodeInfo) {
        getItemBounds(i2, this.mTempRect);
        nodeInfo.setContentDescription(this.mCOUIViewTalkBalkInteraction.getItemDescription(i2));
        nodeInfo.setBoundsInParent(this.mTempRect);
        if (this.mCOUIViewTalkBalkInteraction.getClassName() != null) {
            nodeInfo.setClassName(this.mCOUIViewTalkBalkInteraction.getClassName());
        }
        nodeInfo.addAction(16);
        if (i2 == this.mCOUIViewTalkBalkInteraction.getCurrentPosition()) {
            nodeInfo.setSelected(true);
        }
        if (i2 == this.mCOUIViewTalkBalkInteraction.getDisablePosition()) {
            nodeInfo.setEnabled(false);
        }
    }

    public void setCOUIViewTalkBalkInteraction(COUIViewTalkBalkInteraction cOUIViewTalkBalkInteraction) {
        this.mCOUIViewTalkBalkInteraction = cOUIViewTalkBalkInteraction;
    }

    public void setFocusedVirtualView(int i2) {
        AccessibilityNodeProviderCompat provider = getAccessibilityNodeProvider(this.mHostView);
        provider.performAction(i2, 64, null);
    }
}
