package com.android.launcher3.util;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public class DimenUtils {
    // 基础屏幕密度
    public static final float BASE_SCREEN_DENSITY = 2.0f;
    // 基础屏幕高度
    public static final float BASE_SCREEN_HEIGHT = 1280.0f;
    // 基础屏幕宽度
    public static final float BASE_SCREEN_WIDTH = 720.0f;

    // 维度单位类型：DP
    public static final int UNIT_DP = 1;
    // 维度单位类型：SP
    public static final int UNIT_SP = 2;
    // 维度单位类型：PX 转 DP
    public static final int UNIT_PX_TO_DP = 6;
    // 维度单位类型：PX 转 SP
    public static final int UNIT_PX_TO_SP = 7;
    // 维度单位类型：DP 按高度缩放
    public static final int UNIT_DP_SCALE_H = 8;
    // 维度单位类型：DP 比例按高度缩放
    public static final int UNIT_DP_RATIO_SCALE_H = 9;
    // 维度单位类型：DP 按宽度缩放
    public static final int UNIT_DP_SCALE_W = 10;

    // 高度缩放因子
    public static Float screenHeightScaleFactor;
    // 宽度缩放因子
    public static Float screenWidthScaleFactor;

    /**
     * 根据不同的单位类型转换维度值
     * @param context 上下文对象
     * @param unit 单位类型
     * @param value 要转换的值
     * @param displayMetrics 显示指标
     * @return 转换后的维度值
     */
    public static float convertDimension(Context context, int unit, float value, DisplayMetrics displayMetrics) {
        if (unit == UNIT_DP || unit == UNIT_SP) {
            return TypedValue.applyDimension(unit, value, displayMetrics);
        }
        switch (unit) {
            case UNIT_PX_TO_DP:
                return value / displayMetrics.density;
            case UNIT_PX_TO_SP:
                return value / displayMetrics.scaledDensity;
            case UNIT_DP_SCALE_H:
                return TypedValue.applyDimension(UNIT_DP, value * getScreenHeightScaleFactor(context), displayMetrics);
            case UNIT_DP_RATIO_SCALE_H:
                return value * getScreenHeightScaleFactor(context);
            case UNIT_DP_SCALE_W:
                return TypedValue.applyDimension(UNIT_DP, value * getScreenWidthScaleFactor(context), displayMetrics);
            default:
                return 0.0f;
        }
    }

    /**
     * 将 DP 值转换为 PX 值
     * @param context 上下文对象
     * @param dpValue DP 值
     * @return PX 值
     */
    public static int dpToPx(Context context, float dpValue) {
        return (int) convertDimension(context, UNIT_DP, dpValue, context.getResources().getDisplayMetrics());
    }

    /**
     * 将 DP 值按屏幕高度缩放后转换为 PX 值
     * @param context 上下文对象
     * @param dpValue DP 值
     * @return 缩放后的 PX 值
     */
    public static int dpToPxScaleByHeight(Context context, float dpValue) {
        return (int) convertDimension(context, UNIT_DP_SCALE_H, dpValue, context.getResources().getDisplayMetrics());
    }

    /**
     * 将 DP 值按屏幕宽度缩放后转换为 PX 值
     * @param context 上下文对象
     * @param dpValue DP 值
     * @return 缩放后的 PX 值
     */
    public static int dpToPxScaleByWidth(Context context, float dpValue) {
        return (int) convertDimension(context, UNIT_DP_SCALE_W, dpValue, context.getResources().getDisplayMetrics());
    }

    /**
     * 将 DP 值按屏幕高度缩放
     * @param context 上下文对象
     * @param dpValue DP 值
     * @return 缩放后的 DP 值
     */
    public static int dpScaleByHeight(Context context, float dpValue) {
        return (int) convertDimension(context, UNIT_DP_RATIO_SCALE_H, dpValue, context.getResources().getDisplayMetrics());
    }

    /**
     * 获取屏幕密度
     * @param context 上下文对象
     * @return 屏幕密度
     */
    public static float getScreenDensity(Context context) {
        return context.getResources().getDisplayMetrics().density;
    }

    /**
     * 获取屏幕高度缩放因子
     * @param context 上下文对象
     * @return 屏幕高度缩放因子
     */
    public static float getScreenHeightScaleFactor(Context context) {
        if (screenHeightScaleFactor == null) {
            screenHeightScaleFactor = ((float) getScreenHeight(context)) * 2.0f / (getScreenDensity(context) * BASE_SCREEN_HEIGHT);
        }
        return screenHeightScaleFactor;
    }

    /**
     * 获取屏幕宽度缩放因子
     * @param context 上下文对象
     * @return 屏幕宽度缩放因子
     */
    public static float getScreenWidthScaleFactor(Context context) {
        if (screenWidthScaleFactor == null) {
            screenWidthScaleFactor = ((float) getScreenWidth(context)) * 2.0f / (getScreenDensity(context) * BASE_SCREEN_WIDTH);
        }
        return screenWidthScaleFactor;
    }

    /**
     * 获取屏幕高度
     * @param context 上下文对象
     * @return 屏幕高度
     */
    public static int getScreenHeight(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    /**
     * 获取屏幕宽度
     * @param context 上下文对象
     * @return 屏幕宽度
     */
    public static int getScreenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    /**
     * 将 PX 值转换为 DP 值
     * @param context 上下文对象
     * @param pxValue PX 值
     * @return DP 值
     */
    public static int pxToDp(Context context, float pxValue) {
        return (int) convertDimension(context, UNIT_PX_TO_DP, pxValue, context.getResources().getDisplayMetrics());
    }

    /**
     * 将 PX 值转换为 SP 值
     * @param context 上下文对象
     * @param pxValue PX 值
     * @return SP 值
     */
    public static int pxToSp(Context context, float pxValue) {
        return (int) convertDimension(context, UNIT_PX_TO_SP, pxValue, context.getResources().getDisplayMetrics());
    }

    /**
     * 将 SP 值转换为 PX 值
     * @param context 上下文对象
     * @param spValue SP 值
     * @return PX 值
     */
    public static int spToPx(Context context, float spValue) {
        return (int) convertDimension(context, UNIT_SP, spValue, context.getResources().getDisplayMetrics());
    }

    /**
     * 将 DIP 值转换为 PX 值
     * @param context 上下文对象
     * @param dipValue DIP 值
     * @return PX 值
     */
    public static int dipToPx(Context context, float dipValue) {
        return (int) ((dipValue * context.getResources().getDisplayMetrics().density) + 0.5f);
    }

}
