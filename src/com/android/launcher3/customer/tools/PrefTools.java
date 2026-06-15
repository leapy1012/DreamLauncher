package com.android.launcher3.customer.tools;
import android.content.Context;

public class PrefTools {
    static String themePref = "themePref";

    public static String getString(String key, String defalut, Context context) {
        return context.getSharedPreferences(themePref, 0).getString(key, defalut);
    }

    public static long getLong(String key, int defalut, Context context) {
        return context.getSharedPreferences(themePref, 0).getLong(key, (long) defalut);
    }

    public static int getInt(String key, int defalut, Context context) {
        return context.getSharedPreferences(themePref, 0).getInt(key, defalut);
    }

    public static boolean getBoolean(String key, boolean defalut, Context context) {
        return context.getSharedPreferences(themePref, 0).getBoolean(key, defalut);
    }

    public static boolean getScreenStyle(String key, boolean defalut, Context context) {
        return getBoolean(key, defalut, context);
    }

    public static void putLong(String key, long value, Context context) {
        context.getSharedPreferences(themePref, 0).edit().putLong(key, value).commit();
    }

    public static float getFloat(String key, float defalut, Context context) {
        return context.getSharedPreferences(themePref, 0).getFloat(key, defalut);
    }

    public static void putFloat(String key, float value, Context context) {
        context.getSharedPreferences(themePref, 0).edit().putFloat(key, value).commit();
    }

    public static void putInt(String key, int value, Context context) {
        context.getSharedPreferences(themePref, 0).edit().putInt(key, value).commit();
    }

    public static void putString(String key, String value, Context context) {
        context.getSharedPreferences(themePref, 0).edit().putString(key, value).commit();
    }

    public static void putBoolean(String key, boolean value, Context context) {
        context.getSharedPreferences(themePref, 0).edit().putBoolean(key, value).commit();
    }
}
