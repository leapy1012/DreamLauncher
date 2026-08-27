package com.coui.appcompat.lunarutil;

import android.text.TextUtils;
import android.util.Log;
import com.coui.appcompat.picker.COUILunarDatePicker;
import com.coui.appcompat.vibrateutil.VibrateUtils;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class COUILunarUtil {
    public static final int DECREATE_A_LUANR_YEAR = -1;
    private static final int DEFAULT_END_YEAR = 2100;
    private static final int DEFAULT_START_YEAR = 1900;
    private static final int HIGH_BIT_VALUE = 32768;
    public static final int INCREASE_A_LUANR_YEAR = 1;
    private static final int LAUN_LEAP_MONTH_DAYS = 29;
    private static final int LAUN_MONTH_DAYS = 30;
    private static final int LEAPMONTH_BIT_FLAG = 15;
    private static final int LEAPMONTH_BIT_MASK = 65536;
    public static final int LEAP_MONTH = 0;
    private static final int LOW_BIT_VALUE = 8;
    private static final int MAX_YEAR = 2100;
    private static final int MIN_YEAR = 1900;
    public static final int NORMAL_MONTH = 1;
    private static final int ONE = 1;
    private static final String START_DATE = "19000130";
    private static final String TAG = "COUILunar";
    private static final int THIRTY = 30;
    private static final int TWELVE = 12;
    private static final int YEAR_OF_MONTH = 12;
    private static final String[] CHINESE_NUMBER = {"一", "二", "三", "四", "五", "六", "七", "八", "九", "十", "十一", "十二"};
    private static final String[] ALL_SC_SOLAR_TERM_NAMES = {"小寒", "大寒", "立春", "雨水", "惊蛰", "春分", "清明", "谷雨", "立夏", "小满", "芒种", "夏至", "小暑", "大暑", "立秋", "处暑", "白露", "秋分", "寒露", "霜降", "立冬", "小雪", "大雪", "冬至"};
    private static final String[] ALL_TC_SOLAR_TERM_NAMES = {"小寒", "大寒", "立春", "雨水", "驚蟄", "春分", "清明", "穀雨", "立夏", "小滿", "芒種", "夏至", "小暑", "大暑", "立秋", "處暑", "白露", "秋分", "寒露", "霜降", "立冬", "小雪", "大雪", "冬至"};
    private static final long[] LUNAR_INFO = {19416, 19168, 42352, 21717, 53856, 55632, 91476, 22176, 39632, 21970, 19168, 42422, 42192, 53840, 119381, 46400, 54944, 44450, 38320, 84343, 18800, 42160, 46261, 27216, 27968, 109396, 11104, 38256, 21234, 18800, 25958, 54432, 59984, 92821, 23248, 11104, 100067, 37600, 116951, 51536, 54432, 120998, 46416, 22176, 107956, 9680, 37584, 53938, 43344, 46423, 27808, 46416, 86869, 19872, 42416, 83315, 21168, 43432, 59728, 27296, 44710, 43856, 19296, 43748, 42352, 21088, 62051, 55632, 23383, 22176, 38608, 19925, 19152, 42192, 54484, 53840, 54616, 46400, 46752, 103846, 38320, 18864, 43380, 42160, 45690, 27216, 27968, 44870, 43872, 38256, 19189, 18800, 25776, 29859, 59984, 27480, 23232, 43872, 38613, 37600, 51552, 55636, 54432, 55888, 30034, 22176, 43959, 9680, 37584, 51893, 43344, 46240, 47780, 44368, 21977, 19360, 42416, 86390, 21168, 43312, 31060, 27296, 44368, 23378, 19296, 42726, 42208, 53856, 60005, 54576, 23200, 30371, 38608, 19195, 19152, 42192, 118966, 53840, 54560, 56645, 46496, 22224, 21938, 18864, 42359, 42160, 43600, 111189, 27936, 44448, 84835, 37744, 18936, 18800, 25776, 92326, 59984, 27424, 108228, 43744, 37600, 53987, 51552, 54615, 54432, 55888, 23893, 22176, 42704, 21972, 21200, 43448, 43344, 46240, 46758, 44368, 21920, 43940, 42416, 21168, 45683, 26928, 29495, 27296, 44368, 84821, 19296, 42352, 21732, 53600, 59752, 54560, 55968, 92838, 22224, 19168, 43476, 41680, 53584, 62034, 54560};
    private static final int[][] SOLAR_TERM_DAYS = {new int[]{6, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 22, 7, 23, 8, 23, 8, 23, 9, 24, 8, 23, 7, 22}, new int[]{6, 21, 4, 19, 6, 21, 5, 21, 6, 22, 6, 22, 8, 23, 8, 24, 8, 24, 9, 24, 8, 23, 8, 22}, new int[]{6, 21, 5, 19, 5, 20, 5, 20, 5, 21, 5, 21, 7, 23, 7, 23, 7, 23, 8, 23, 7, 22, 7, 22}, new int[]{5, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 21, 7, 23, 8, 23, 8, 23, 8, 23, 7, 22, 7, 22}, new int[]{6, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 22, 7, 23, 8, 23, 8, 23, 9, 24, 8, 22, 7, 22}, new int[]{6, 20, 4, 19, 6, 21, 5, 21, 6, 22, 6, 22, 8, 23, 8, 24, 8, 24, 9, 24, 8, 23, 8, 22}, new int[]{6, 21, 4, 19, 5, 20, 4, 20, 5, 21, 5, 21, 7, 23, 7, 23, 7, 23, 8, 23, 7, 22, 7, 22}, new int[]{5, 20, 4, 19, 5, 21, 5, 20, 5, 21, 6, 21, 7, 23, 7, 23, 8, 23, 8, 23, 7, 22, 7, 22}, new int[]{5, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 22, 7, 23, 8, 23, 8, 23, 8, 24, 8, 23, 7, 22}, new int[]{6, 20, 4, 19, 6, 21, 5, 21, 6, 22, 6, 22, 8, 23, 8, 24, 8, 23, 9, 24, 8, 23, 8, 22}, new int[]{6, 21, 4, 19, 5, 20, 4, 20, 5, 21, 5, 21, 7, 23, 7, 23, 7, 23, 8, 23, 7, 22, 7, 21}, new int[]{5, 20, 4, 19, 5, 21, 5, 20, 5, 21, 6, 21, 7, 23, 7, 23, 8, 23, 8, 23, 7, 22, 7, 22}, new int[]{5, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 22, 7, 23, 8, 23, 8, 23, 8, 24, 8, 22, 7, 22}, new int[]{6, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 22, 8, 23, 8, 24, 8, 23, 9, 24, 8, 23, 8, 22}, new int[]{6, 21, 4, 19, 5, 20, 4, 20, 5, 21, 5, 21, 7, 23, 7, 23, 7, 23, 8, 23, 7, 22, 7, 21}, new int[]{5, 20, 4, 19, 5, 20, 5, 20, 5, 21, 6, 21, 7, 23, 7, 23, 8, 23, 8, 23, 7, 22, 7, 22}, new int[]{5, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 22, 7, 23, 8, 23, 8, 23, 8, 24, 8, 22, 7, 22}, new int[]{6, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 22, 8, 23, 8, 24, 8, 23, 9, 24, 8, 23, 7, 22}, new int[]{6, 21, 4, 19, 5, 20, 4, 20, 5, 21, 5, 21, 7, 23, 7, 23, 7, 23, 8, 23, 7, 22, 7, 21}, new int[]{5, 20, 4, 18, 5, 20, 5, 20, 5, 21, 6, 21, 7, 23, 7, 23, 8, 23, 8, 23, 7, 22, 7, 22}, new int[]{5, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 21, 7, 23, 8, 23, 8, 23, 8, 24, 8, 22, 7, 22}, new int[]{6, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 22, 7, 23, 8, 23, 8, 23, 9, 24, 8, 23, 7, 22}, new int[]{6, 21, 4, 19, 5, 20, 4, 20, 5, 21, 5, 21, 7, 22, 7, 23, 7, 23, 8, 23, 7, 22, 7, 21}, new int[]{5, 20, 4, 18, 5, 20, 5, 20, 5, 21, 6, 21, 7, 23, 7, 23, 7, 23, 8, 23, 7, 22, 7, 22}, new int[]{5, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 22, 7, 23, 8, 23, 8, 23, 8, 23, 7, 22, 7, 22}, new int[]{6, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 22, 7, 23, 8, 23, 8, 23, 9, 24, 8, 23, 7, 22}, new int[]{6, 21, 4, 19, 5, 20, 4, 20, 5, 21, 5, 21, 7, 22, 7, 23, 7, 23, 8, 23, 7, 22, 7, 21}, new int[]{5, 20, 4, 18, 5, 20, 5, 20, 5, 21, 6, 21, 7, 23, 7, 23, 7, 23, 8, 23, 7, 22, 7, 22}, new int[]{5, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 21, 7, 23, 8, 23, 8, 23, 8, 23, 7, 22, 7, 22}, new int[]{6, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 22, 7, 23, 8, 23, 8, 23, 9, 24, 8, 23, 7, 22}, new int[]{6, 21, 4, 19, 5, 20, 4, 20, 5, 21, 5, 21, 7, 22, 7, 23, 7, 23, 8, 23, 7, 22, 7, 21}, new int[]{5, 20, 4, 18, 5, 20, 5, 20, 5, 21, 5, 21, 7, 23, 7, 23, 7, 23, 8, 23, 7, 22, 7, 22}, new int[]{5, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 21, 7, 23, 8, 23, 8, 23, 8, 23, 7, 22, 7, 22}, new int[]{6, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 22, 7, 23, 8, 23, 8, 23, 9, 24, 8, 23, 7, 22}, new int[]{6, 21, 4, 19, 5, 20, 4, 20, 5, 21, 5, 21, 7, 22, 7, 23, 7, 23, 8, 23, 7, 22, 7, 21}, new int[]{5, 20, 4, 18, 5, 20, 5, 20, 5, 21, 5, 21, 7, 23, 7, 23, 7, 23, 8, 23, 7, 22, 7, 22}, new int[]{5, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 21, 7, 23, 8, 23, 8, 23, 8, 23, 7, 22, 7, 22}, new int[]{6, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 22, 7, 23, 8, 23, 8, 23, 9, 24, 8, 22, 7, 22}, new int[]{6, 20, 4, 19, 5, 20, 4, 20, 5, 21, 5, 21, 7, 22, 7, 23, 7, 23, 8, 23, 7, 22, 7, 21}, new int[]{5, 20, 3, 18, 5, 20, 4, 20, 5, 21, 5, 21, 7, 23, 7, 23, 7, 23, 8, 23, 7, 22, 7, 22}, new int[]{5, 20, 4, 19, 6, 21, 5, 20, 5, 21, 6, 21, 7, 23, 7, 23, 8, 23, 8, 23, 7, 22, 7, 22}, new int[]{6, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 22, 7, 23, 8, 23, 8, 23, 8, 24, 8, 23, 7, 22}, new int[]{6, 21, 4, 19, 5, 20, 4, 20, 5, 20, 5, 21, 7, 22, 7, 23, 7, 22, 8, 23, 7, 22, 7, 21}, new int[]{5, 20, 4, 18, 5, 20, 4, 20, 5, 21, 5, 21, 7, 22, 7, 23, 7, 23, 8, 23, 7, 22, 7, 22}, new int[]{5, 20, 4, 19, 5, 21, 5, 20, 5, 21, 6, 21, 7, 23, 7, 23, 8, 23, 8, 23, 7, 22, 7, 22}, new int[]{5, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 22, 7, 23, 8, 23, 8, 23, 8, 24, 8, 22, 7, 22}, new int[]{6, 20, 4, 19, 5, 20, 4, 19, 5, 20, 5, 21, 7, 22, 7, 23, 7, 22, 8, 23, 7, 22, 6, 21}, new int[]{5, 20, 3, 18, 5, 20, 4, 20, 5, 21, 5, 21, 7, 23, 7, 23, 7, 23, 8, 23, 7, 22, 7, 21}, new int[]{5, 20, 4, 18, 5, 20, 5, 20, 5, 21, 6, 21, 7, 23, 7, 23, 8, 23, 8, 23, 7, 22, 7, 22}, new int[]{5, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 21, 7, 23, 8, 23, 8, 23, 8, 24, 8, 22, 7, 22}, new int[]{6, 20, 4, 19, 5, 20, 4, 19, 5, 20, 5, 21, 7, 22, 7, 23, 7, 22, 8, 23, 7, 22, 6, 21}, new int[]{5, 20, 3, 18, 5, 20, 4, 20, 5, 21, 5, 21, 7, 22, 7, 23, 7, 23, 8, 23, 7, 22, 7, 21}, new int[]{5, 20, 4, 18, 5, 20, 5, 20, 5, 21, 6, 21, 7, 23, 7, 23, 8, 23, 8, 23, 7, 22, 7, 22}, new int[]{5, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 22, 7, 23, 8, 23, 8, 23, 8, 23, 7, 22, 7, 22}, new int[]{6, 20, 4, 19, 5, 20, 4, 19, 5, 20, 5, 21, 6, 22, 7, 23, 7, 22, 8, 23, 7, 22, 6, 21}, new int[]{5, 20, 3, 18, 5, 20, 4, 20, 5, 21, 5, 21, 7, 22, 7, 23, 7, 23, 8, 23, 7, 22, 7, 21}, new int[]{5, 20, 4, 18, 5, 20, 5, 20, 5, 21, 6, 21, 7, 23, 7, 23, 7, 23, 8, 23, 7, 22, 7, 22}, new int[]{5, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 21, 7, 23, 8, 23, 8, 23, 8, 23, 7, 22, 7, 22}, new int[]{6, 20, 4, 19, 5, 20, 4, 19, 5, 20, 5, 21, 6, 22, 7, 22, 7, 22, 8, 23, 7, 22, 6, 21}, new int[]{5, 20, 3, 18, 5, 20, 4, 20, 5, 21, 5, 21, 7, 22, 7, 23, 7, 23, 8, 23, 7, 22, 7, 21}, new int[]{5, 20, 4, 18, 5, 20, 5, 20, 5, 21, 6, 21, 7, 23, 7, 23, 7, 23, 8, 23, 7, 22, 7, 22}, new int[]{5, 20, 4, 19, 6, 21, 5, 20, 6, 21, 6, 21, 7, 23, 8, 23, 8, 23, 8, 23, 7, 22, 7, 22}, new int[]{6, 20, 4, 19, 5, 20, 4, 19, 5, 20, 5, 21, 6, 22, 7, 22, 7, 22, 8, 23, 7, 22, 6, 21}, new int[]{5, 20, 3, 18, 5, 20, 4, 20, 5, 21, 5, 21, 7, 22, 7, 23, 7, 23, 8, 23, 7, 22, 7, 21}, new int[]{5, 20, 4, 18, 5, 20, 5, 20, 5, 21, 5, 21, 7, 23, 7, 23, 7, 23, 8, 23, 7, 22, 7, 22}, new int[]{5, 20, 4, 19, 6, 21, 5, 20, 5, 21, 6, 21, 7, 23, 7, 23, 8, 23, 8, 23, 7, 22, 7, 22}, new int[]{6, 20, 4, 19, 5, 20, 4, 19, 5, 20, 5, 21, 6, 22, 7, 22, 7, 22, 8, 23, 7, 22, 6, 21}};
    private static SimpleDateFormat sChineseDateFormat = new SimpleDateFormat("yyyy年MM月dd日");

    public static int[] calculateLunarByGregorian(int i2, int i6, int i10) {
        Date date;
        int[] iArr = {VibrateUtils.STRENGTH_MAX_STEP, 1, 1, 1};
        if (i2 == Integer.MIN_VALUE) {
            iArr[0] = i2;
            int i11 = i6 - 1;
            iArr[1] = (i11 % 12) + 1;
            iArr[2] = i10;
            iArr[3] = i11 / 12 > 0 ? 0 : 1;
            return iArr;
        }
        Date date2 = null;
        try {
            date = sChineseDateFormat.parse("1900年1月31日");
        } catch (ParseException e2) {
            Log.e(TAG, "calculateLunarByGregorian(),parse baseDate error.");
            e2.printStackTrace();
            date = null;
        }
        if (date == null) {
            Log.e(TAG, "baseDate is null,return lunar date:2000.1.1");
            return iArr;
        }
        try {
            date2 = sChineseDateFormat.parse(i2 + "年" + i6 + "月" + i10 + "日");
        } catch (ParseException e10) {
            Log.e(TAG, "calculateLunarByGregorian(),parse currentDate error.");
            e10.printStackTrace();
        }
        if (date2 == null) {
            Log.e(TAG, "currentDate is null,return lunar date:2000.1.1");
            return iArr;
        }
        int iRound = Math.round((date2.getTime() - date.getTime()) / 8.64E7f);
        int i12 = 1900;
        int iDaysOfLunarYear = 0;
        while (i12 < 10000 && iRound > 0) {
            iDaysOfLunarYear = daysOfLunarYear(i12);
            iRound -= iDaysOfLunarYear;
            i12++;
        }
        if (iRound < 0) {
            iRound += iDaysOfLunarYear;
            i12--;
        }
        int iLeapMonth = leapMonth(i12);
        int i13 = 1;
        int i14 = 0;
        int iDaysOfALunarMonth = 0;
        while (i13 < 13 && iRound > 0) {
            if (iLeapMonth > 0 && i13 == iLeapMonth + 1 && i14 == 0) {
                i13--;
                iDaysOfALunarMonth = daysOfLeapMonthInLunarYear(i12);
                i14 = 1;
            } else {
                iDaysOfALunarMonth = daysOfALunarMonth(i12, i13);
            }
            iRound -= iDaysOfALunarMonth;
            if (i14 != 0 && i13 == iLeapMonth + 1) {
                i14 = 0;
            }
            i13++;
        }
        if (iRound == 0 && iLeapMonth > 0 && i13 == iLeapMonth + 1) {
            if (i14 != 0) {
                i14 = 0;
            } else {
                i13--;
                i14 = 1;
            }
        }
        if (iRound < 0) {
            iRound += iDaysOfALunarMonth;
            i13--;
        }
        iArr[0] = i12;
        iArr[1] = i13;
        iArr[2] = iRound + 1;
        iArr[3] = i14 ^ 1;
        return iArr;
    }

    public static COUILunarDatePicker.IncompleteDate changeALunarYear(int i2, int i6, int i10, int i11) {
        int[] iArrClampMonth = clampMonth(i2, i6, i11);
        Date dateLunarToSolar = lunarToSolar(i2, iArrClampMonth[0], clampDay(i2, iArrClampMonth[0], i10, iArrClampMonth[1] == 0), iArrClampMonth[1] == 0);
        COUILunarDatePicker.IncompleteDate incompleteDate = new COUILunarDatePicker.IncompleteDate();
        if (dateLunarToSolar != null) {
            incompleteDate.setTimeInMillis(dateLunarToSolar.getTime());
        }
        return incompleteDate;
    }

    @Deprecated
    public static Calendar changeALunarYearByOne(Calendar calendar, int i2, int i6, int i10, int i11, int i12) {
        Calendar calendar2 = Calendar.getInstance();
        calendar2.setTimeInMillis(calendar.getTimeInMillis());
        int days = getDays(i11, i2, i6, i10);
        int days2 = getDays(i12, i2, i6, i10);
        if (i11 > i12) {
            calendar2.add(5, -((daysOfLunarYear(i12) - days2) + days));
        } else if (i11 < i12) {
            calendar2.add(5, (daysOfLunarYear(i11) + days2) - days);
        }
        return calendar2;
    }

    public static String chineseStringOfALunarDay(int i2) {
        String[] strArr = {"初", "十", "廿", "卅"};
        int i6 = i2 % 10;
        int i10 = i6 == 0 ? 9 : i6 - 1;
        if (i2 > 30) {
            return "";
        }
        if (i2 == 10) {
            return "初十";
        }
        if (i2 == 20) {
            return "二十";
        }
        if (i2 == 30) {
            return "三十";
        }
        return strArr[i2 / 10] + CHINESE_NUMBER[i10];
    }

    public static int clampDay(int i2, int i6, int i10, boolean z6) {
        int iDaysOfALunarMonth = !z6 ? daysOfALunarMonth(i2, i6) : daysOfLeapMonthInLunarYear(i2);
        return i10 > iDaysOfALunarMonth ? iDaysOfALunarMonth : i10;
    }

    private static int[] clampMonth(int i2, int i6, int i10) {
        return new int[]{i6, ((i10 == 0 && leapMonth(i2) == i6) ? 1 : 0) ^ 1};
    }

    public static int daysOfALunarMonth(int i2, int i6) {
        if (i2 != Integer.MIN_VALUE && i2 >= 1900) {
            if ((((long) (LEAPMONTH_BIT_MASK >> i6)) & LUNAR_INFO[i2 - 1900]) == 0) {
                return 29;
            }
        }
        return 30;
    }

    public static int daysOfLeapMonthInLunarYear(int i2) {
        if (leapMonth(i2) != 0) {
            return (LUNAR_INFO[i2 + (-1900)] & 65536) != 0 ? 30 : 29;
        }
        return 0;
    }

    public static int daysOfLunarYear(int i2) {
        if (i2 == Integer.MIN_VALUE) {
            return 0;
        }
        int i6 = 348;
        for (int i10 = HIGH_BIT_VALUE; i10 > 8; i10 >>= 1) {
            int i11 = i2 - 1900;
            if (i11 >= 0) {
                long[] jArr = LUNAR_INFO;
                if (i11 < jArr.length && (jArr[i11] & ((long) i10)) != 0) {
                    i6++;
                }
            }
        }
        return i6 + daysOfLeapMonthInLunarYear(i2);
    }

    private static int[] getAMonthSolarTermDays(int i2, int i6) {
        int i10 = (i6 - 1) * 2;
        int[] iArr = {0, 0};
        if (i2 > 1969 && i2 < 2037) {
            int[] iArr2 = SOLAR_TERM_DAYS[i2 - 1970];
            int i11 = iArr2[i10];
            int i12 = iArr2[i10 + 1];
            iArr[0] = i11;
            iArr[1] = i12;
        }
        return iArr;
    }

    private static String[] getAMonthSolarTermNames(int i2) {
        if (i2 >= 1 && i2 <= 12) {
            int i6 = (i2 - 1) * 2;
            String[] strArr = ALL_TC_SOLAR_TERM_NAMES;
            return new String[]{strArr[i6], strArr[i6 + 1]};
        }
        Log.e(TAG, "getAMonthSolarTermNames(),param gregorianMonth:" + i2 + " is error");
        return new String[]{"", ""};
    }

    public static int getDays(int i2, int i6, int i10, int i11) {
        int iDaysOfALunarMonth;
        if (i2 == Integer.MIN_VALUE) {
            return 0;
        }
        for (int i12 = 1; i12 < i6; i12++) {
            i10 += daysOfALunarMonth(i2, i12);
        }
        if (leapMonth(i2) < i6) {
            iDaysOfALunarMonth = daysOfLeapMonthInLunarYear(i2);
        } else {
            if (leapMonth(i2) != i6 || i11 != 0) {
                return i10;
            }
            iDaysOfALunarMonth = daysOfALunarMonth(i2, i6);
        }
        return i10 + iDaysOfALunarMonth;
    }

    private static String getGregFestival(int i2, int i6) {
        if (i2 == 1 && i6 == 1) {
            return "";
        }
        if (i2 == 5 && i6 == 1) {
            return "";
        }
        if (i2 == 10 && i6 == 1) {
            return "";
        }
        return null;
    }

    public static String getLunarDateString(Calendar calendar) {
        int[] iArrCalculateLunarByGregorian = calculateLunarByGregorian(calendar.get(1), calendar.get(2) + 1, calendar.get(5));
        return getLunarDateString(iArrCalculateLunarByGregorian[0], iArrCalculateLunarByGregorian[1], iArrCalculateLunarByGregorian[2], iArrCalculateLunarByGregorian[3]);
    }

    private static String getLunarFestival(int i2, int i6) {
        if (i2 == 1 && i6 == 1) {
            return "春節";
        }
        if (i2 == 5 && i6 == 5) {
            return "端午";
        }
        if (i2 == 8 && i6 == 15) {
            return "中秋";
        }
        return null;
    }

    public static String getLunarFestivalChineseString(int i2, int i6, int i10) {
        String gregFestival = getGregFestival(i6, i10);
        if (!TextUtils.isEmpty(gregFestival)) {
            return gregFestival;
        }
        int[] iArrCalculateLunarByGregorian = calculateLunarByGregorian(i2, i6, i10);
        String lunarFestival = getLunarFestival(iArrCalculateLunarByGregorian[1], iArrCalculateLunarByGregorian[2]);
        if (!TextUtils.isEmpty(lunarFestival)) {
            return lunarFestival;
        }
        String solarTerm = getSolarTerm(i2, i6, i10);
        if (TextUtils.isEmpty(solarTerm)) {
            return getLunarNumber(iArrCalculateLunarByGregorian[1], iArrCalculateLunarByGregorian[2], iArrCalculateLunarByGregorian[3] == 0);
        }
        return solarTerm;
    }

    private static String getLunarNumber(int i2, int i6, boolean z6) {
        if (i6 != 1) {
            return chineseStringOfALunarDay(i6);
        }
        if (z6) {
            return "闰" + CHINESE_NUMBER[i2 - 1];
        }
        return CHINESE_NUMBER[i2 - 1] + "月";
    }

    public static String getSolarTerm(int i2, int i6, int i10) {
        int[] aMonthSolarTermDays = getAMonthSolarTermDays(i2, i6);
        if (i10 != aMonthSolarTermDays[0] && i10 != aMonthSolarTermDays[1]) {
            return null;
        }
        String[] aMonthSolarTermNames = getAMonthSolarTermNames(i6);
        if (i10 == aMonthSolarTermDays[0]) {
            return aMonthSolarTermNames[0];
        }
        if (i10 == aMonthSolarTermDays[1]) {
            return aMonthSolarTermNames[1];
        }
        return null;
    }

    private static int getYearDays(int i2) {
        int i6 = 348;
        for (int i10 = HIGH_BIT_VALUE; i10 >= 8; i10 >>= 1) {
            if ((LUNAR_INFO[i2 - 1900] & 65520 & ((long) i10)) != 0) {
                i6++;
            }
        }
        return i6 + daysOfLeapMonthInLunarYear(i2);
    }

    private static boolean isLunarDate(int i2, int i6, int i10, boolean z6) {
        if (i2 < 1900 || i2 > 2100 || i6 < 1 || i6 > 12 || i10 < 1 || i10 > 30) {
            return false;
        }
        return !z6 || i6 == leapMonth(i2);
    }

    public static int leapMonth(int i2) {
        if (i2 >= 1900 && i2 <= 2100) {
            return (int) (LUNAR_INFO[i2 - 1900] & 15);
        }
        Log.e(TAG, "get leapMonth:" + i2 + "is out of range.return 0.");
        return 0;
    }

    public static Date lunarToSolar(int i2, int i6, int i10, boolean z6) {
        if (!isLunarDate(i2, i6, i10, z6)) {
            return null;
        }
        int iDaysOfALunarMonth = 0;
        for (int i11 = 1900; i11 < i2; i11++) {
            iDaysOfALunarMonth += getYearDays(i11);
        }
        int iLeapMonth = leapMonth(i2);
        if (z6 && iLeapMonth != i6) {
            return null;
        }
        int i12 = 1;
        if (iLeapMonth == 0 || i6 < iLeapMonth || (i6 == iLeapMonth && !z6)) {
            while (i12 < i6) {
                iDaysOfALunarMonth += daysOfALunarMonth(i2, i12);
                i12++;
            }
            if (i10 > daysOfALunarMonth(i2, i6)) {
                return null;
            }
        } else {
            while (i12 < i6) {
                iDaysOfALunarMonth += daysOfALunarMonth(i2, i12);
                i12++;
            }
            if (i6 > iLeapMonth) {
                iDaysOfALunarMonth += daysOfLeapMonthInLunarYear(i2);
                if (i10 > daysOfALunarMonth(i2, i6)) {
                    return null;
                }
            } else {
                iDaysOfALunarMonth += daysOfALunarMonth(i2, i6);
                if (i10 > daysOfLeapMonthInLunarYear(i2)) {
                    return null;
                }
            }
        }
        int i13 = iDaysOfALunarMonth + i10;
        try {
            Date date = new SimpleDateFormat("yyyyMMdd").parse(START_DATE);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(5, i13);
            return calendar.getTime();
        } catch (ParseException e2) {
            e2.printStackTrace();
            return null;
        }
    }

    public static String getLunarDateString(int i2, int i6, int i10) {
        int[] iArrCalculateLunarByGregorian = calculateLunarByGregorian(i2, i6, i10);
        return getLunarDateString(iArrCalculateLunarByGregorian[0], iArrCalculateLunarByGregorian[1], iArrCalculateLunarByGregorian[2], iArrCalculateLunarByGregorian[3]);
    }

    private static String getLunarDateString(int i2, int i6, int i10, int i11) {
        if (i2 != Integer.MIN_VALUE) {
            StringBuilder sb = new StringBuilder();
            sb.append(i2);
            sb.append("年");
            sb.append(i11 == 0 ? "闰" : "");
            sb.append(CHINESE_NUMBER[i6 - 1]);
            sb.append("月");
            sb.append(chineseStringOfALunarDay(i10));
            return sb.toString();
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append(i11 == 0 ? "闰" : "");
        sb2.append(CHINESE_NUMBER[i6 - 1]);
        sb2.append("月");
        sb2.append(chineseStringOfALunarDay(i10));
        return sb2.toString();
    }
}
