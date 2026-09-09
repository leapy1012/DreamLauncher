package com.android.launcher3.editselection;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.widget.LauncherAppWidgetHostView;
import com.android.launcher3.widget.RoundedCornerEnforcement;

/**
 * Oppo workspace-edit minus on widgets ({@code launcher_ic_widget_remove} via
 * {@code SwitchStateRenderer.drawDeleteIcon} on {@code widgetBackgroundBounds}).
 */
public final class EditSelectionWidgetBadge {

    private static final Rect sVisual = new Rect();
    private static final Rect sIcon = new Rect();

    private EditSelectionWidgetBadge() {}

    public static boolean shouldDraw(@Nullable LauncherAppWidgetHostView host) {
        if (host == null || !(host.getContext() instanceof Launcher launcher)) {
            return false;
        }
        if (!(host.getTag() instanceof ItemInfo)) {
            return false;
        }
        return launcher.getEditSelectionManager().shouldDrawChecks();
    }

    /**
     * Visual card / clock face in host coordinates. Oppo {@code widgetBackgroundBounds}
     * / {@code RoundedCornerEnforcement} background — not the padded host box.
     */
    public static void getVisualBounds(@NonNull LauncherAppWidgetHostView host,
            @NonNull Rect out) {
        View bg = RoundedCornerEnforcement.findBackground(host);
        if (bg != null && bg.getWidth() > 0 && bg.getHeight() > 0) {
            RoundedCornerEnforcement.computeRoundedRectangle(host, bg, out);
            if (out.width() > 0 && out.height() > 0) {
                return;
            }
        }
        int pl = host.getPaddingLeft();
        int pt = host.getPaddingTop();
        int pr = host.getPaddingRight();
        int pb = host.getPaddingBottom();
        if (pl + pr < host.getWidth() && pt + pb < host.getHeight()) {
            out.set(pl, pt, host.getWidth() - pr, host.getHeight() - pb);
        } else {
            out.set(0, 0, host.getWidth(),
                    host.getHeight() - Math.abs(pb - pt));
        }
    }

    public static void getIconRect(@NonNull LauncherAppWidgetHostView host, @NonNull Rect out) {
        getVisualBounds(host, sVisual);
        int size = host.getResources().getDimensionPixelSize(
                R.dimen.edit_selection_widget_remove_size);
        boolean rtl = host.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        if (rtl) {
            out.set(sVisual.left, sVisual.top, sVisual.left + size, sVisual.top + size);
        } else {
            out.set(sVisual.right - size, sVisual.top, sVisual.right, sVisual.top + size);
        }
    }

    public static void drawIfNecessary(@NonNull LauncherAppWidgetHostView host,
            @NonNull Canvas canvas) {
        if (!shouldDraw(host)) {
            return;
        }
        Drawable icon = host.getContext().getDrawable(R.drawable.launcher_ic_widget_remove);
        if (icon == null) {
            return;
        }
        getIconRect(host, sIcon);
        icon = icon.mutate();
        icon.setBounds(sIcon);
        icon.draw(canvas);
    }

    public static boolean isTouchOnIcon(@NonNull LauncherAppWidgetHostView host, float x, float y) {
        if (!shouldDraw(host)) {
            return false;
        }
        getIconRect(host, sIcon);
        return sIcon.contains(Math.round(x), Math.round(y));
    }
}
