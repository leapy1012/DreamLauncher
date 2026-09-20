package com.android.launcher3.editselection;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;

/**
 * Oppo {@code SwitchStateRenderer} badge placement.
 * <p>
 * App / folder select dots: center on the icon corner, inset by
 * {@code select_icon_right_offset} / {@code select_icon_top_offset} (3dp).
 * Widget delete (−): flush to the top-end corner of {@code widgetBackgroundBounds}
 * (half-icon offset math → fully inside the plate).
 */
public final class EditSelectionBadgeLayout {

    private EditSelectionBadgeLayout() {}

    /**
     * Oppo {@code drawSelectIcon} (phone LTR/RTL): badge centered on
     * {@code (icon.right - rightOffset, icon.top + topOffset)} (mirrored in RTL).
     */
    public static void getAppCheckBounds(@NonNull Rect iconBounds, int sizePx,
            int rightOffsetPx, int topOffsetPx, boolean rtl, @NonNull Rect out) {
        float half = sizePx / 2f;
        float cx = rtl
                ? iconBounds.left + rightOffsetPx
                : iconBounds.right - rightOffsetPx;
        // Oppo LTR: top + offset; RTL constructor negates offsets so result is also top + |offset|.
        float cy = iconBounds.top + topOffsetPx;
        out.set(Math.round(cx - half), Math.round(cy - half),
                Math.round(cx + half), Math.round(cy + half));
    }

    /**
     * Oppo {@code getWidgetDeleteIconRect} / {@code drawDeleteIcon} for non-align widgets:
     * (−) sits flush in the top-end corner of the plate bounds.
     */
    public static void getWidgetRemoveBounds(@NonNull Rect backgroundBounds, int sizePx,
            boolean rtl, @NonNull Rect out) {
        float half = sizePx / 2f;
        // offset = size/2 → left = right - size, top = top (flush corner).
        float cx = rtl
                ? backgroundBounds.left + half
                : backgroundBounds.right - half;
        float cy = backgroundBounds.top + half;
        out.set(Math.round(cx - half), Math.round(cy - half),
                Math.round(cx + half), Math.round(cy + half));
    }

    public static boolean isRtl(@NonNull View view) {
        return view.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
    }
}
