package com.android.launcher3.editselection;

import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.widget.LauncherAppWidgetHostView;
import com.android.launcher3.widget.RoundedCornerEnforcement;

/**
 * Oppo workspace-edit widget chrome:
 * <ul>
 *   <li>{@code SwitchStateRenderer.drawWidgetBackground} — frosted plate under the widget</li>
 *   <li>{@code SwitchStateRenderer.drawDeleteIcon} — minus on the plate corner</li>
 * </ul>
 * The plate is what makes transparent widgets (digital clock) readable in edit mode.
 */
public final class EditSelectionWidgetBadge {

    private static final Rect sVisual = new Rect();
    private static final Rect sIcon = new Rect();
    private static final Rect sBg = new Rect();

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
     * Oppo {@code widgetBackgroundBounds} (non-align path): full host, adjust for
     * asymmetric vertical padding. Drawable itself applies {@code launcher_widget_background_inset}.
     */
    public static void getBackgroundBounds(@NonNull LauncherAppWidgetHostView host,
            @NonNull Rect out) {
        int pb = host.getPaddingBottom();
        int pt = host.getPaddingTop();
        out.set(0, 0, host.getWidth(), host.getHeight() - Math.abs(pb - pt));
    }

    /**
     * Visual card / clock face in host coordinates — used for minus placement when a
     * rounded background child exists; otherwise same as {@link #getBackgroundBounds}.
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
        getBackgroundBounds(host, out);
    }

    public static void getIconRect(@NonNull LauncherAppWidgetHostView host, @NonNull Rect out) {
        // Oppo getWidgetDeleteIconRect: flush to plate corner (no extra inset).
        getBackgroundBounds(host, sVisual);
        int size = host.getResources().getDimensionPixelSize(
                R.dimen.edit_selection_widget_remove_size);
        EditSelectionBadgeLayout.getWidgetRemoveBounds(
                sVisual, size, EditSelectionBadgeLayout.isRtl(host), out);
    }

    /**
     * Oppo {@code drawBackground} before {@code super.dispatchDraw}: plate under widget content.
     */
    public static void drawBackgroundIfNecessary(@NonNull LauncherAppWidgetHostView host,
            @NonNull Canvas canvas) {
        if (!shouldDraw(host)) {
            return;
        }
        boolean bright = isBrightWallpaper(host);
        Drawable plate = host.getContext().getDrawable(bright
                ? R.drawable.launcher_widget_background_bright
                : R.drawable.launcher_widget_background);
        if (plate == null) {
            return;
        }
        getBackgroundBounds(host, sBg);
        plate = plate.mutate();
        plate.setBounds(sBg);
        plate.setAlpha(255);
        plate.draw(canvas);
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

    /** Oppo {@code WallpaperResolver.isWorkspaceEditModeBright}. */
    private static boolean isBrightWallpaper(@NonNull View host) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return false;
        }
        WallpaperColors colors = host.getContext().getSystemService(WallpaperManager.class)
                .getWallpaperColors(WallpaperManager.FLAG_SYSTEM);
        return colors != null
                && (colors.getColorHints() & WallpaperColors.HINT_SUPPORTS_DARK_TEXT) != 0;
    }
}
