package com.android.launcher3.iconresize;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.views.ActivityContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads span-specific morph icon drawables: Oppo uxicon when available, else fallback plate.
 */
public final class MorphIconLoaderHelper {

    private static final String TAG = "MorphIconLoaderHelper";

    private static final Map<String, Drawable> sCache = new HashMap<>();

    private MorphIconLoaderHelper() {}

    public static Drawable loadDisplayDrawable(BubbleTextView view, Drawable innerIcon) {
        Object tag = view.getTag();
        if (!(tag instanceof ItemInfo info) || !IconResizeHelper.canResize(info)
                || !IconResizeHelper.hasExtendedSpan(info)) {
            return innerIcon;
        }
        int spanX = info.spanX;
        int spanY = info.spanY;
        DeviceProfile dp = ActivityContext.lookupContext(view.getContext()).getDeviceProfile();
        Rect morph = IconResizeHelper.getMorphIconBounds(dp, spanX, spanY);
        int w = morph.width();
        int h = morph.height();
        float radius = MorphWorkspaceIconDrawable.getMorphCornerRadius(spanX, spanY, w, h);

        ComponentName cn = info.getTargetComponent();
        String pkg = info.getTargetPackage();
        String cacheKey = buildCacheKey(cn, spanX, spanY, w, h);

        Drawable cached = sCache.get(cacheKey);
        if (cached != null) {
            return cached.getConstantState() != null
                    ? cached.getConstantState().newDrawable().mutate() : cached;
        }

        Context context = view.getContext();
        Drawable morphDrawable = null;
        if (cn != null && OplusMorphIconBridge.isSupportMorphUxIcon(pkg)) {
            morphDrawable = OplusMorphIconBridge.loadMorphIcon(context, cn, spanX, spanY, w, h,
                    radius);
        }

        Drawable innerCopy = innerIcon.getConstantState() != null
                ? innerIcon.getConstantState().newDrawable().mutate()
                : innerIcon;
        Drawable fgSource = MorphPlateColorHelper.loadMorphForeground(context, cn, innerCopy);
        Drawable fg = MorphForegroundHelper.createMorphForeground(fgSource, context.getResources(),
                w, h, spanX, spanY);
        if (fg == null) {
            fg = fgSource;
        }

        Drawable result;
        if (morphDrawable != null) {
            result = morphDrawable;
            Log.d(TAG, "Using Oppo morph asset for " + pkg + " " + spanX + "x" + spanY);
        } else {
            result = new MorphWorkspaceIconDrawable(context, fg, view.getIconSize(), spanX, spanY,
                    w, h, cn, radius, MorphWorkspaceIconDrawable.ScaleMode.MORPH);
            Log.d(TAG, "Using fallback morph plate for " + pkg + " " + spanX + "x" + spanY);
        }
        sCache.put(cacheKey, result);
        return result.getConstantState() != null
                ? result.getConstantState().newDrawable().mutate() : result;
    }

    public static void invalidateCache(@Nullable ItemInfo info) {
        if (info == null || info.getTargetComponent() == null) {
            return;
        }
        ComponentName cn = info.getTargetComponent();
        sCache.keySet().removeIf(k -> k.startsWith(cn.flattenToShortString()));
    }

    private static String buildCacheKey(@Nullable ComponentName cn, int spanX, int spanY,
            int w, int h) {
        String base = cn != null ? cn.flattenToShortString() : "unknown";
        return base + ":" + spanX + "x" + spanY + "@" + w + "x" + h;
    }
}
