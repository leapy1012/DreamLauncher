package com.android.launcher3;

import android.graphics.drawable.Drawable;

import com.android.launcher3.graphics.DragPreviewProvider;

import java.util.List;

/**
 * Head drawable for an OPPO batch drag.
 *
 * <p>OPPO creates one live drag view per selected icon; it does not flatten the stack and count
 * into one bitmap. The manager owns the tail views and count renderer, while this provider keeps
 * the normal Workspace positioning contract for the head icon.</p>
 */
final class ColorOsBatchDragPreviewProvider extends DragPreviewProvider {

    private final OplusBubbleTextView mHead;

    ColorOsBatchDragPreviewProvider(OplusBubbleTextView head,
            List<OplusBubbleTextView> orderedViews) {
        super(head);
        mHead = head;
    }

    @Override
    public Drawable createDrawable() {
        boolean selected = mHead.isSelected();
        // Decoded createBatchDragDrawable() temporarily removes the selection/check renderer so
        // only the application icon participates in the drag visual.
        mHead.setColorOsWorkspaceSelected(false, false);
        try {
            return super.createDrawable();
        } finally {
            mHead.setColorOsWorkspaceSelected(selected, false);
        }
    }
}
