package com.android.launcher3.big.memoryclean.utils;

public class HxyAntiShakeUtil {
    private static final int MIN_DELAY_TIME = 1000;
    private static long lastClickTime = 0;

    public static boolean isInvalidClick() {
        boolean flag = true;
        long currentClickTime = System.currentTimeMillis();
        if (currentClickTime - lastClickTime >= 1000) {
            flag = false;
        }
        lastClickTime = currentClickTime;
        return flag;
    }

    public static boolean isInvalidClick(long internalTime) {
        boolean flag = true;
        long currentClickTime = System.currentTimeMillis();
        if (currentClickTime - lastClickTime >= internalTime) {
            flag = false;
        }
        lastClickTime = currentClickTime;
        return flag;
    }
}
