package com.coui.appcompat.uiutil;

import android.content.res.Resources;
import android.graphics.Color;
import android.view.View;
import com.coui.appcompat.R;
import com.coui.appcompat.contextutil.COUIContextUtil;
import com.coui.appcompat.log.COUILog;
import com.coui.appcompat.version.COUIVersionUtil;
import com.oplus.view.OplusView;

public class ShadowUtils {
    public static final int SDK_SUB_VERSION = 2;
    public static final int SDK_VERSION = 34;
    public static final int SHADOW_LV1 = 0;
    public static final int SHADOW_LV2 = 1;
    public static final int SHADOW_LV3 = 2;
    public static final int SHADOW_LV4 = 3;
    public static final int SHADOW_LV5 = 4;
    private static final String TAG = "ShadowUtils";

    public static boolean checkOPlusViewElevationSDK() {
        return COUIVersionUtil.checkOPlusViewSubSDK(34, 2);
    }

    public static void clearShadow(View view) {
        if (checkOPlusViewElevationSDK()) {
            setElevationToViewFromOPlusView(view, 0, 0, 0, 0, 0, -1);
        } else {
            setElevationToViewFromLower(view, 0, 0, 0);
        }
    }

    public static void setElevationToFloatingActionButton(View view, int i2, int i6) {
        setElevationToFloatingActionButton(view, i2, i6, COUIContextUtil.getAttrColor(view.getContext(), R.attr.couiColorPrimary));
    }

    public static void setElevationToView(View view, int i2) {
        setElevationToView(view, i2, 0, 0, 0);
    }

    public static void setElevationToViewFromLower(View view, int i2, int i6, int i10) {
        if (view == null) {
            return;
        }
        view.setOutlineSpotShadowColor(i6);
        view.setElevation(i2);
    }

    public static void setElevationToViewFromOPlusView(View view, int i2, int i6, int i10, int i11, int i12, int i13) {
        if (view != null && checkOPlusViewElevationSDK()) {
            view.setOutlineAmbientShadowColor(i6);
            view.setOutlineSpotShadowColor(i6);
            view.setElevation(i2);
            try {
                new OplusView(view).setOverrideLightSourceGeometry(-1.0f, i10, i11, i12, i13);
            } catch (Exception e2) {
                COUILog.d(TAG, "setOverrideLightSourceGeometry error:" + e2.getMessage());
            }
        }
    }

    public static void setElevationToView(View view, int i2, int i6, int i10) {
        setElevationToView(view, i2, i6, view.getResources().getDimensionPixelOffset(R.dimen.support_shadow_size_level_for_lowerP), i10);
    }

    public static void setElevationToFloatingActionButton(View view, int i2, int i6, int i10) {
        if (view == null) {
            COUILog.e(TAG, "setElevationToFloatingActionButton target view is null");
            return;
        }
        if (i10 == -1) {
            i10 = COUIContextUtil.getAttrColor(view.getContext(), R.attr.couiColorPrimary);
        }
        if (checkOPlusViewElevationSDK()) {
            Resources resources = view.getContext().getResources();
            setElevationToViewFromOPlusView(view, resources.getDimensionPixelSize(R.dimen.coui_float_btn_shadow_elevation), Color.argb(resources.getInteger(R.integer.coui_shadow_color_float_btn), Color.red(i10), Color.green(i10), Color.blue(i10)), resources.getDimensionPixelSize(R.dimen.coui_float_btn_shadow_light_y), resources.getDimensionPixelSize(R.dimen.coui_float_btn_shadow_light_z), resources.getDimensionPixelSize(R.dimen.coui_float_btn_shadow_light_r), resources.getDimensionPixelSize(R.dimen.coui_float_btn_shadow_blur_r));
        } else {
            setElevationToViewFromLower(view, i2, i6, view.getResources().getDimensionPixelOffset(R.dimen.support_shadow_size_level_for_lowerP));
        }
    }

    public static void setElevationToView(View view, int i2, int i6, int i10, int i11) {
        if (view == null) {
            COUILog.e(TAG, "setElevationToView view is null");
            return;
        }
        if (checkOPlusViewElevationSDK()) {
            Resources resources = view.getContext().getResources();
            int dimensionPixelSize = resources.getDimensionPixelSize(R.dimen.coui_shadow_elevation_default);
            if (i2 == 0) {
                setElevationToViewFromOPlusView(view, dimensionPixelSize, Color.argb(resources.getInteger(R.integer.coui_shadow_color_lv1), 0, 0, 0), resources.getDimensionPixelSize(R.dimen.coui_shadow_light_y_level1), resources.getDimensionPixelSize(R.dimen.coui_shadow_light_z_level1), resources.getDimensionPixelSize(R.dimen.coui_shadow_light_r_level1), resources.getDimensionPixelSize(R.dimen.coui_shadow_blur_r_level1));
                return;
            }
            if (i2 == 1) {
                setElevationToViewFromOPlusView(view, dimensionPixelSize, Color.argb(resources.getInteger(R.integer.coui_shadow_color_lv2), 0, 0, 0), resources.getDimensionPixelSize(R.dimen.coui_shadow_light_y_level2), resources.getDimensionPixelSize(R.dimen.coui_shadow_light_z_level2), resources.getDimensionPixelSize(R.dimen.coui_shadow_light_r_level2), resources.getDimensionPixelSize(R.dimen.coui_shadow_blur_r_level2));
                return;
            }
            if (i2 == 2) {
                setElevationToViewFromOPlusView(view, dimensionPixelSize, Color.argb(resources.getInteger(R.integer.coui_shadow_color_lv3), 0, 0, 0), resources.getDimensionPixelSize(R.dimen.coui_shadow_light_y_level3), resources.getDimensionPixelSize(R.dimen.coui_shadow_light_z_level3), resources.getDimensionPixelSize(R.dimen.coui_shadow_light_r_level3), resources.getDimensionPixelSize(R.dimen.coui_shadow_blur_r_level3));
                return;
            } else if (i2 == 3) {
                setElevationToViewFromOPlusView(view, resources.getDimensionPixelSize(R.dimen.coui_shadow_elevation_four), Color.argb(resources.getInteger(R.integer.coui_shadow_color_lv4), 0, 0, 0), resources.getDimensionPixelSize(R.dimen.coui_shadow_light_y_level4), resources.getDimensionPixelSize(R.dimen.coui_shadow_light_z_level4), resources.getDimensionPixelSize(R.dimen.coui_shadow_light_r_level4), resources.getDimensionPixelSize(R.dimen.coui_shadow_blur_r_level4));
                return;
            } else {
                if (i2 == 4) {
                    setElevationToViewFromOPlusView(view, resources.getDimensionPixelSize(R.dimen.coui_shadow_elevation_five), Color.argb(resources.getInteger(R.integer.coui_shadow_color_lv5), 0, 0, 0), resources.getDimensionPixelSize(R.dimen.coui_shadow_light_y_level5), resources.getDimensionPixelSize(R.dimen.coui_shadow_light_z_level5), resources.getDimensionPixelSize(R.dimen.coui_shadow_light_r_level5), resources.getDimensionPixelSize(R.dimen.coui_shadow_blur_r_level5));
                    return;
                }
                return;
            }
        }
        setElevationToViewFromLower(view, i6, i11, i10);
    }
}
