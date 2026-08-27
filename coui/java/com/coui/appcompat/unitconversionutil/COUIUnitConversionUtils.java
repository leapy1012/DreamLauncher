package com.coui.appcompat.unitconversionutil;

import android.content.Context;
import android.util.Log;
import com.coui.appcompat.R;
import com.coui.appcompat.log.COUILog;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class COUIUnitConversionUtils {
    private static final boolean COUI_DEBUG;
    private static final double HUNDRED = 100.0d;
    private static final double MILLION = 1000000.0d;
    private static final String NOPOINT = "0";
    private static final String ONEPOINT = "0.0";
    private static final float ONE_MILLION = 1.0E-7f;
    private static final double POINT_NINE_EIGHT = 0.98d;
    private static final String SIXPOINT = "0.00000";
    private static final double SPECIAL = 1024.0d;
    private static final int SQUARE_FIVE = 5;
    private static final int SQUARE_FOUR = 4;
    private static final int SQUARE_THREE = 3;
    private static final String TAG = "COUIUnitConversionUtils";
    private static final double TEN = 10.0d;
    private static final double THOUSAND = 1000.0d;
    private static final String TWOPOINT = "0.00";
    private String mByteShort;
    private String mByteSpeed;
    private Context mContext;
    private String mGigaByteShort;
    private String mGigaByteSpeed;
    private boolean mIfShowNormal;
    private String mKiloByteShort;
    private String mKiloByteSpeed;
    private String mMegaByteShort;
    private String mMegaByteSpeed;
    private String mMoreDownLoad;
    private String mMostDownLoad;
    private String mPetaByteShort;
    private String mPetaByteSpeed;
    private String mSpecialPoint;
    private String mTeraByteShort;
    private String mTeraByteSpeed;

    static {
        COUI_DEBUG = COUILog.LOG_DEBUG || COUILog.isLoggable(TAG, 3);
    }

    public COUIUnitConversionUtils(Context context) {
        this.mMoreDownLoad = null;
        this.mMostDownLoad = null;
        this.mSpecialPoint = "0.98";
        this.mContext = context;
        this.mIfShowNormal = context.getResources().getBoolean(R.bool.show_direction_normal);
        this.mMoreDownLoad = context.getResources().getString(R.string.more_time_download);
        this.mMostDownLoad = context.getResources().getString(R.string.most_time_download);
        this.mSpecialPoint = formatLocaleNumber(POINT_NINE_EIGHT, TWOPOINT);
        this.mByteShort = this.mContext.getResources().getString(R.string.byteShort);
        this.mKiloByteShort = this.mContext.getResources().getString(R.string.kilobyteShort);
        this.mMegaByteShort = this.mContext.getResources().getString(R.string.megabyteShort);
        this.mGigaByteShort = this.mContext.getResources().getString(R.string.gigabyteShort);
        this.mTeraByteShort = this.mContext.getResources().getString(R.string.terabyteShort);
        this.mPetaByteShort = this.mContext.getResources().getString(R.string.petabyteShort);
        this.mByteSpeed = this.mContext.getResources().getString(R.string.byteSpeed);
        this.mKiloByteSpeed = this.mContext.getResources().getString(R.string.kiloByteSpeed);
        this.mMegaByteSpeed = this.mContext.getResources().getString(R.string.megaByteSpeed);
        this.mGigaByteSpeed = this.mContext.getResources().getString(R.string.gigaByteSpeed);
        this.mTeraByteSpeed = this.mContext.getResources().getString(R.string.teraByteSpeed);
        this.mPetaByteSpeed = this.mContext.getResources().getString(R.string.petaByteSpeed);
    }

    private String formatLocaleNumber(double d2, String str) {
        return new DecimalFormat(str, new DecimalFormatSymbols(this.mContext.getResources().getConfiguration().locale)).format(d2);
    }

    private String formatNumber(double d2, String str, boolean z6) {
        DecimalFormat decimalFormat = new DecimalFormat(str, new DecimalFormatSymbols(Locale.CHINA));
        if (z6) {
            decimalFormat.setRoundingMode(RoundingMode.HALF_UP);
        } else {
            decimalFormat.setRoundingMode(RoundingMode.FLOOR);
        }
        return decimalFormat.format(d2);
    }

    private String getChineseDownloadValue(long j2) {
        if (0 <= j2 && j2 < 10000.0d) {
            if (j2 == 0) {
                j2++;
            }
            return j2 + " ";
        }
        double d2 = j2;
        if (10000.0d <= d2 && d2 < 100000.0d) {
            double dDoubleValue = Double.valueOf(formatNumber(d2 / 10000.0d, ONEPOINT, true)).doubleValue();
            int i2 = (int) dDoubleValue;
            if (dDoubleValue == i2) {
                return i2 + this.mMoreDownLoad;
            }
            return dDoubleValue + this.mMoreDownLoad;
        }
        if (100000.0d <= d2 && d2 < MILLION) {
            double dDoubleValue2 = Double.valueOf(formatNumber(d2 / 10000.0d, ONEPOINT, true)).doubleValue();
            int i6 = (int) dDoubleValue2;
            if (dDoubleValue2 == i6) {
                return i6 + this.mMoreDownLoad;
            }
            return dDoubleValue2 + this.mMoreDownLoad;
        }
        if (MILLION <= d2 && d2 < 1.0E7d) {
            return ((int) Double.valueOf(formatNumber(d2 / 10000.0d, TWOPOINT, true)).doubleValue()) + this.mMoreDownLoad;
        }
        if (1.0E7d <= d2 && d2 < 1.0E8d) {
            return ((int) Double.valueOf(formatNumber(d2 / 10000.0d, TWOPOINT, true)).doubleValue()) + this.mMoreDownLoad;
        }
        if (d2 < 1.0E8d) {
            throw new IllegalArgumentException("the value of the incoming is wrong");
        }
        return formatNumber(Double.valueOf(formatNumber(d2 / 1.0E8d, SIXPOINT, true)).doubleValue(), ONEPOINT, false) + this.mMostDownLoad;
    }

    private String getChineseStripValue(long j2) {
        if (0 <= j2 && j2 < 10000.0d) {
            return j2 + " ";
        }
        double d2 = j2;
        if (10000.0d > d2 || d2 >= MILLION) {
            if (MILLION > d2 || d2 >= 1.0E8d) {
                throw new IllegalArgumentException("the value of the incoming is wrong");
            }
            return formatNumber(d2 / 10000.0d, NOPOINT, true) + this.mMoreDownLoad;
        }
        double dDoubleValue = Double.valueOf(formatNumber(d2 / 10000.0d, ONEPOINT, true)).doubleValue();
        int i2 = (int) dDoubleValue;
        if (dDoubleValue == i2) {
            return i2 + this.mMoreDownLoad;
        }
        return dDoubleValue + this.mMoreDownLoad;
    }

    private String getEnglishDownloadValue(long j2) {
        if (0 <= j2 && j2 < 10000.0d) {
            if (j2 == 0) {
                j2++;
            }
            return j2 + " ";
        }
        double d2 = j2;
        if (10000.0d <= d2 && d2 < 100000.0d) {
            return ((int) (Double.valueOf(formatNumber(d2 / 10000.0d, ONEPOINT, true)).doubleValue() * TEN)) + this.mMoreDownLoad;
        }
        if (100000.0d <= d2 && d2 < MILLION) {
            return ((int) (Double.valueOf(formatNumber(d2 / 10000.0d, ONEPOINT, true)).doubleValue() * TEN)) + this.mMoreDownLoad;
        }
        if (MILLION <= d2 && d2 < 1.0E7d) {
            String number = formatNumber(d2 / 10000.0d, TWOPOINT, true);
            double dDoubleValue = Double.valueOf(number).doubleValue() / HUNDRED;
            int i2 = (int) dDoubleValue;
            if (Math.abs(dDoubleValue - ((double) i2)) < 1.0000000116860974E-7d) {
                return i2 + this.mMostDownLoad;
            }
            return Double.valueOf(number) + this.mMostDownLoad;
        }
        if (1.0E7d > d2 || d2 >= 1.0E8d) {
            if (d2 < 1.0E8d) {
                throw new IllegalArgumentException("the value of the incoming is wrong");
            }
            return ((int) (Double.valueOf(formatNumber(Double.valueOf(formatNumber(d2 / 1.0E8d, SIXPOINT, true)).doubleValue(), ONEPOINT, false)).doubleValue() * HUNDRED)) + this.mMostDownLoad;
        }
        String number2 = formatNumber(d2 / 10000.0d, TWOPOINT, true);
        double dDoubleValue2 = Double.valueOf(number2).doubleValue() / HUNDRED;
        int i6 = (int) dDoubleValue2;
        if (Math.abs(dDoubleValue2 - ((double) i6)) < 1.0000000116860974E-7d) {
            return i6 + this.mMostDownLoad;
        }
        return Double.valueOf(number2) + this.mMostDownLoad;
    }

    private String getEnglishStripValue(long j2) {
        if (0 <= j2 && j2 < 10000.0d) {
            return j2 + " ";
        }
        double d2 = j2;
        if (10000.0d <= d2 && d2 < MILLION) {
            return ((int) (Double.valueOf(formatNumber(d2 / 10000.0d, ONEPOINT, true)).doubleValue() * TEN)) + this.mMoreDownLoad;
        }
        if (MILLION > d2 || d2 >= 1.0E8d) {
            throw new IllegalArgumentException("the value of the incoming is wrong");
        }
        return ((int) (Double.valueOf(formatNumber(d2 / 10000.0d, NOPOINT, true)).doubleValue() * TEN)) + this.mMoreDownLoad;
    }

    private String getStringComposite(String str, String str2) {
        if (COUI_DEBUG) {
            Log.d(TAG, "getStringComposite content:" + str + ",unit:" + str + ",mIfShowNormal:" + this.mIfShowNormal);
        }
        if (this.mIfShowNormal) {
            return str + str2;
        }
        return str2 + " " + str;
    }

    private boolean isChinese() {
        String country = this.mContext.getResources().getConfiguration().locale.getCountry();
        if (country != null) {
            return country.equalsIgnoreCase("CN") || country.equalsIgnoreCase("TW") || country.equalsIgnoreCase("HK");
        }
        return false;
    }

    public String getDownLoadValue(long j2) {
        return isChinese() ? getChineseDownloadValue(j2) : getEnglishDownloadValue(j2);
    }

    public String getSpeedValue(long j2) {
        if (0 <= j2) {
            double d2 = j2;
            if (d2 < THOUSAND) {
                String number = formatNumber(d2, NOPOINT, true);
                long j6 = Long.parseLong(number);
                String localeNumber = formatLocaleNumber(Double.valueOf(number).doubleValue(), NOPOINT);
                double d7 = j6;
                if (THOUSAND <= d7 && d7 < SPECIAL) {
                    return getUnitValue(j6);
                }
                return localeNumber + this.mByteSpeed;
            }
        }
        double d10 = j2;
        if (THOUSAND <= d10 && d10 < 1024000.0d) {
            String number2 = formatNumber(d10 / SPECIAL, NOPOINT, true);
            long j10 = Long.parseLong(number2) * 1024;
            String localeNumber2 = formatLocaleNumber(Double.valueOf(number2).doubleValue(), NOPOINT);
            double d11 = j10;
            if (1024000.0d <= d11 && d11 < Math.pow(SPECIAL, 2.0d) * HUNDRED) {
                return getUnitValue(j10);
            }
            return localeNumber2 + this.mKiloByteSpeed;
        }
        if (1024000.0d <= d10 && d10 < Math.pow(SPECIAL, 2.0d) * HUNDRED) {
            String number3 = formatNumber(d10 / Math.pow(SPECIAL, 2.0d), ONEPOINT, true);
            long jDoubleValue = (long) (Double.valueOf(number3).doubleValue() * Math.pow(SPECIAL, 2.0d));
            String localeNumber3 = formatLocaleNumber(Double.valueOf(number3).doubleValue(), ONEPOINT);
            double d12 = jDoubleValue;
            if (Math.pow(SPECIAL, 2.0d) * HUNDRED <= d12 && d12 < Math.pow(SPECIAL, 2.0d) * THOUSAND) {
                return getUnitValue(jDoubleValue);
            }
            return localeNumber3 + this.mMegaByteSpeed;
        }
        if (Math.pow(SPECIAL, 2.0d) * HUNDRED <= d10 && d10 < Math.pow(SPECIAL, 2.0d) * THOUSAND) {
            String number4 = formatNumber(d10 / Math.pow(SPECIAL, 2.0d), NOPOINT, true);
            long jDoubleValue2 = (long) (Double.valueOf(number4).doubleValue() * Math.pow(SPECIAL, 2.0d));
            String localeNumber4 = formatLocaleNumber(Double.valueOf(number4).doubleValue(), NOPOINT);
            double d13 = jDoubleValue2;
            if (Math.pow(SPECIAL, 2.0d) * THOUSAND <= d13 && d13 < Math.pow(SPECIAL, 3.0d)) {
                return getUnitValue(jDoubleValue2);
            }
            return localeNumber4 + this.mMegaByteSpeed;
        }
        if (Math.pow(SPECIAL, 2.0d) * THOUSAND <= d10 && d10 < Math.pow(SPECIAL, 3.0d)) {
            if (d10 > Math.pow(SPECIAL, 2.0d) * 1023.0d) {
                return getUnitValue((long) Math.pow(SPECIAL, 3.0d));
            }
            return this.mSpecialPoint + this.mGigaByteSpeed;
        }
        if (Math.pow(SPECIAL, 3.0d) <= d10 && d10 < Math.pow(SPECIAL, 3.0d) * TEN) {
            String number5 = formatNumber(d10 / Math.pow(SPECIAL, 3.0d), TWOPOINT, true);
            long jDoubleValue3 = (long) (Double.valueOf(number5).doubleValue() * Math.pow(SPECIAL, 3.0d));
            String localeNumber5 = formatLocaleNumber(Double.valueOf(number5).doubleValue(), TWOPOINT);
            double d14 = jDoubleValue3;
            if (Math.pow(SPECIAL, 3.0d) * TEN <= d14 && d14 < Math.pow(SPECIAL, 3.0d) * HUNDRED) {
                return getUnitValue(jDoubleValue3);
            }
            return localeNumber5 + this.mGigaByteSpeed;
        }
        if (Math.pow(SPECIAL, 3.0d) * TEN <= d10 && d10 < Math.pow(SPECIAL, 3.0d) * HUNDRED) {
            String number6 = formatNumber(d10 / Math.pow(SPECIAL, 3.0d), ONEPOINT, true);
            long jDoubleValue4 = (long) (Double.valueOf(number6).doubleValue() * Math.pow(SPECIAL, 3.0d));
            String localeNumber6 = formatLocaleNumber(Double.valueOf(number6).doubleValue(), ONEPOINT);
            double d15 = jDoubleValue4;
            if (Math.pow(SPECIAL, 3.0d) * HUNDRED <= d15 && d15 < Math.pow(SPECIAL, 3.0d) * THOUSAND) {
                return getUnitValue(jDoubleValue4);
            }
            return localeNumber6 + this.mGigaByteSpeed;
        }
        if (Math.pow(SPECIAL, 3.0d) * HUNDRED <= d10 && d10 < Math.pow(SPECIAL, 3.0d) * THOUSAND) {
            String number7 = formatNumber(d10 / Math.pow(SPECIAL, 3.0d), NOPOINT, true);
            long jDoubleValue5 = (long) (Double.valueOf(number7).doubleValue() * Math.pow(SPECIAL, 3.0d));
            String localeNumber7 = formatLocaleNumber(Double.valueOf(number7).doubleValue(), NOPOINT);
            double d16 = jDoubleValue5;
            if (Math.pow(SPECIAL, 3.0d) * THOUSAND <= d16 && d16 < Math.pow(SPECIAL, 4.0d)) {
                return getUnitValue(jDoubleValue5);
            }
            return localeNumber7 + this.mGigaByteSpeed;
        }
        if (Math.pow(SPECIAL, 3.0d) * THOUSAND <= d10 && d10 < Math.pow(SPECIAL, 4.0d)) {
            if (d10 > Math.pow(SPECIAL, 3.0d) * 1023.0d) {
                return getUnitValue((long) Math.pow(SPECIAL, 4.0d));
            }
            return this.mSpecialPoint + this.mTeraByteSpeed;
        }
        if (Math.pow(SPECIAL, 4.0d) <= d10 && d10 < Math.pow(SPECIAL, 4.0d) * TEN) {
            String number8 = formatNumber(d10 / Math.pow(SPECIAL, 4.0d), TWOPOINT, true);
            long jDoubleValue6 = (long) (Double.valueOf(number8).doubleValue() * Math.pow(SPECIAL, 4.0d));
            String localeNumber8 = formatLocaleNumber(Double.valueOf(number8).doubleValue(), TWOPOINT);
            double d17 = jDoubleValue6;
            if (Math.pow(SPECIAL, 4.0d) * TEN <= d17 && d17 < Math.pow(SPECIAL, 4.0d) * HUNDRED) {
                return getUnitValue(jDoubleValue6);
            }
            return localeNumber8 + this.mTeraByteSpeed;
        }
        if (Math.pow(SPECIAL, 4.0d) * TEN <= d10 && d10 < Math.pow(SPECIAL, 4.0d) * HUNDRED) {
            String number9 = formatNumber(d10 / Math.pow(SPECIAL, 4.0d), ONEPOINT, true);
            long jDoubleValue7 = (long) (Double.valueOf(number9).doubleValue() * Math.pow(SPECIAL, 4.0d));
            String localeNumber9 = formatLocaleNumber(Double.valueOf(number9).doubleValue(), ONEPOINT);
            double d18 = jDoubleValue7;
            if (Math.pow(SPECIAL, 4.0d) * HUNDRED <= d18 && d18 < Math.pow(SPECIAL, 4.0d) * THOUSAND) {
                return getUnitValue(jDoubleValue7);
            }
            return localeNumber9 + this.mTeraByteSpeed;
        }
        if (Math.pow(SPECIAL, 4.0d) * HUNDRED <= d10 && d10 < Math.pow(SPECIAL, 4.0d) * THOUSAND) {
            String number10 = formatNumber(d10 / Math.pow(SPECIAL, 4.0d), NOPOINT, true);
            long jDoubleValue8 = (long) (Double.valueOf(number10).doubleValue() * Math.pow(SPECIAL, 4.0d));
            double d19 = jDoubleValue8;
            if (Math.pow(SPECIAL, 4.0d) * THOUSAND <= d19 && d19 < Math.pow(SPECIAL, 5.0d)) {
                return getUnitValue(jDoubleValue8);
            }
            return number10 + this.mTeraByteSpeed;
        }
        if (Math.pow(SPECIAL, 4.0d) * THOUSAND <= d10 && d10 < Math.pow(SPECIAL, 5.0d)) {
            if (d10 > Math.pow(SPECIAL, 4.0d) * 1023.0d) {
                return getUnitValue((long) Math.pow(SPECIAL, 5.0d));
            }
            return this.mSpecialPoint + this.mPetaByteSpeed;
        }
        if (Math.pow(SPECIAL, 5.0d) <= d10 && d10 < Math.pow(SPECIAL, 5.0d) * TEN) {
            String number11 = formatNumber(d10 / Math.pow(SPECIAL, 5.0d), TWOPOINT, true);
            long jDoubleValue9 = (long) (Double.valueOf(number11).doubleValue() * Math.pow(SPECIAL, 5.0d));
            String localeNumber10 = formatLocaleNumber(Double.valueOf(number11).doubleValue(), TWOPOINT);
            double d20 = jDoubleValue9;
            if (Math.pow(SPECIAL, 5.0d) * TEN <= d20 && d20 < Math.pow(SPECIAL, 5.0d) * HUNDRED) {
                return getUnitValue(jDoubleValue9);
            }
            return localeNumber10 + this.mPetaByteSpeed;
        }
        if (Math.pow(SPECIAL, 5.0d) * TEN > d10 || d10 >= Math.pow(SPECIAL, 5.0d) * HUNDRED) {
            if (Math.pow(SPECIAL, 5.0d) * HUNDRED > d10 || d10 >= Math.pow(SPECIAL, 5.0d) * THOUSAND) {
                throw new IllegalArgumentException("the value of the incoming is wrong");
            }
            return formatLocaleNumber(d10 / Math.pow(SPECIAL, 5.0d), NOPOINT) + this.mPetaByteSpeed;
        }
        String number12 = formatNumber(d10 / Math.pow(SPECIAL, 5.0d), ONEPOINT, true);
        long jDoubleValue10 = (long) (Double.valueOf(number12).doubleValue() * Math.pow(SPECIAL, 5.0d));
        String localeNumber11 = formatLocaleNumber(Double.valueOf(number12).doubleValue(), ONEPOINT);
        double d21 = jDoubleValue10;
        if (Math.pow(SPECIAL, 5.0d) * HUNDRED <= d21 && d21 < Math.pow(SPECIAL, 5.0d) * THOUSAND) {
            return getUnitValue(jDoubleValue10);
        }
        return localeNumber11 + this.mPetaByteSpeed;
    }

    public String getStripValue(long j2) {
        return isChinese() ? getChineseStripValue(j2) : getEnglishStripValue(j2);
    }

    public String getTransformUnitValue(long j2, double d2) {
        double d6 = j2;
        if (0 <= j2 && d6 < THOUSAND) {
            String number = formatNumber(d6, NOPOINT, true);
            long j6 = Long.parseLong(number);
            String localeNumber = formatLocaleNumber(Double.valueOf(number).doubleValue(), NOPOINT);
            double d7 = j6;
            if (THOUSAND <= d7 && d7 < SPECIAL) {
                return getUnitValue(j6);
            }
            return getStringComposite(localeNumber, this.mByteShort);
        }
        if (THOUSAND <= d6 && d6 < 1024000.0d) {
            String number2 = formatNumber(d6 / d2, NOPOINT, true);
            long j10 = Long.parseLong(number2) * ((long) d2);
            String localeNumber2 = formatLocaleNumber(Double.valueOf(number2).doubleValue(), NOPOINT);
            double d8 = j10;
            if (1024000.0d <= d8 && d8 < Math.pow(SPECIAL, 2.0d) * HUNDRED) {
                return getTransformUnitValue(j10, d2);
            }
            return getStringComposite(localeNumber2, this.mKiloByteShort);
        }
        if (1024000.0d <= d6 && d6 < Math.pow(SPECIAL, 2.0d) * HUNDRED) {
            String number3 = formatNumber(d6 / Math.pow(d2, 2.0d), ONEPOINT, true);
            long jDoubleValue = (long) (Double.valueOf(number3).doubleValue() * Math.pow(d2, 2.0d));
            String localeNumber3 = formatLocaleNumber(Double.valueOf(number3).doubleValue(), ONEPOINT);
            double d10 = jDoubleValue;
            if (Math.pow(SPECIAL, 2.0d) * HUNDRED <= d10 && d10 < Math.pow(SPECIAL, 2.0d) * THOUSAND) {
                return getTransformUnitValue(jDoubleValue, d2);
            }
            return getStringComposite(localeNumber3, this.mMegaByteShort);
        }
        if (Math.pow(SPECIAL, 2.0d) * HUNDRED <= d6 && d6 < Math.pow(SPECIAL, 2.0d) * THOUSAND) {
            String number4 = formatNumber(d6 / Math.pow(d2, 2.0d), NOPOINT, true);
            long jDoubleValue2 = (long) (Double.valueOf(number4).doubleValue() * Math.pow(d2, 2.0d));
            String localeNumber4 = formatLocaleNumber(Double.valueOf(number4).doubleValue(), NOPOINT);
            double d11 = jDoubleValue2;
            if (Math.pow(SPECIAL, 2.0d) * THOUSAND <= d11 && d11 < Math.pow(SPECIAL, 3.0d)) {
                return getTransformUnitValue(jDoubleValue2, d2);
            }
            return getStringComposite(localeNumber4, this.mMegaByteShort);
        }
        if (Math.pow(SPECIAL, 2.0d) * THOUSAND <= d6 && d6 < Math.pow(SPECIAL, 3.0d)) {
            if (d2 == THOUSAND) {
                String number5 = formatNumber(d6 / Math.pow(d2, 3.0d), TWOPOINT, true);
                return getStringComposite(formatLocaleNumber(Double.valueOf(number5).doubleValue(), TWOPOINT), this.mGigaByteShort);
            }
            if (d2 != SPECIAL) {
                return null;
            }
            if (d6 > Math.pow(SPECIAL, 2.0d) * 1023.0d) {
                return getUnitValue((long) Math.pow(SPECIAL, 3.0d));
            }
            return getStringComposite(this.mSpecialPoint, this.mGigaByteShort);
        }
        if (Math.pow(SPECIAL, 3.0d) <= d6 && d6 < Math.pow(SPECIAL, 3.0d) * TEN) {
            String number6 = formatNumber(d6 / Math.pow(d2, 3.0d), TWOPOINT, true);
            long jDoubleValue3 = (long) (Double.valueOf(number6).doubleValue() * Math.pow(d2, 3.0d));
            String localeNumber5 = formatLocaleNumber(Double.valueOf(number6).doubleValue(), TWOPOINT);
            double d12 = jDoubleValue3;
            if (Math.pow(SPECIAL, 3.0d) * TEN <= d12 && d12 < Math.pow(SPECIAL, 3.0d) * HUNDRED) {
                return getTransformUnitValue(jDoubleValue3, d2);
            }
            return getStringComposite(localeNumber5, this.mGigaByteShort);
        }
        if (Math.pow(SPECIAL, 3.0d) * TEN <= d6 && d6 < Math.pow(SPECIAL, 3.0d) * HUNDRED) {
            String number7 = formatNumber(d6 / Math.pow(d2, 3.0d), ONEPOINT, true);
            long jDoubleValue4 = (long) (Double.valueOf(number7).doubleValue() * Math.pow(d2, 3.0d));
            String localeNumber6 = formatLocaleNumber(Double.valueOf(number7).doubleValue(), ONEPOINT);
            double d13 = jDoubleValue4;
            if (Math.pow(SPECIAL, 3.0d) * HUNDRED <= d13 && d13 < Math.pow(SPECIAL, 3.0d) * THOUSAND) {
                return getTransformUnitValue(jDoubleValue4, d2);
            }
            return getStringComposite(localeNumber6, this.mGigaByteShort);
        }
        if (Math.pow(SPECIAL, 3.0d) * HUNDRED <= d6 && d6 < Math.pow(SPECIAL, 3.0d) * THOUSAND) {
            String number8 = formatNumber(d6 / Math.pow(d2, 3.0d), NOPOINT, true);
            long jDoubleValue5 = (long) (Double.valueOf(number8).doubleValue() * Math.pow(d2, 3.0d));
            String localeNumber7 = formatLocaleNumber(Double.valueOf(number8).doubleValue(), NOPOINT);
            double d14 = jDoubleValue5;
            if (Math.pow(SPECIAL, 3.0d) * THOUSAND <= d14 && d14 < Math.pow(SPECIAL, 4.0d)) {
                return getTransformUnitValue(jDoubleValue5, d2);
            }
            return getStringComposite(localeNumber7, this.mGigaByteShort);
        }
        if (Math.pow(SPECIAL, 3.0d) * THOUSAND <= d6 && d6 < Math.pow(SPECIAL, 4.0d)) {
            if (d2 == THOUSAND) {
                String number9 = formatNumber(d6 / Math.pow(d2, 4.0d), TWOPOINT, true);
                return getStringComposite(formatLocaleNumber(Double.valueOf(number9).doubleValue(), TWOPOINT), this.mTeraByteShort);
            }
            if (d2 != SPECIAL) {
                return null;
            }
            if (d6 > Math.pow(SPECIAL, 3.0d) * 1023.0d) {
                return getUnitValue((long) Math.pow(SPECIAL, 4.0d));
            }
            return getStringComposite(this.mSpecialPoint, this.mTeraByteShort);
        }
        if (Math.pow(SPECIAL, 4.0d) <= d6 && d6 < Math.pow(SPECIAL, 4.0d) * TEN) {
            String number10 = formatNumber(d6 / Math.pow(d2, 4.0d), TWOPOINT, true);
            long jDoubleValue6 = (long) (Double.valueOf(number10).doubleValue() * Math.pow(d2, 4.0d));
            String localeNumber8 = formatLocaleNumber(Double.valueOf(number10).doubleValue(), TWOPOINT);
            double d15 = jDoubleValue6;
            if (Math.pow(SPECIAL, 4.0d) * TEN <= d15 && d15 < Math.pow(SPECIAL, 4.0d) * HUNDRED) {
                return getTransformUnitValue(jDoubleValue6, d2);
            }
            return getStringComposite(localeNumber8, this.mTeraByteShort);
        }
        if (Math.pow(SPECIAL, 4.0d) * TEN <= d6 && d6 < Math.pow(SPECIAL, 4.0d) * HUNDRED) {
            String number11 = formatNumber(d6 / Math.pow(d2, 4.0d), ONEPOINT, true);
            long jDoubleValue7 = (long) (Double.valueOf(number11).doubleValue() * Math.pow(d2, 4.0d));
            String localeNumber9 = formatLocaleNumber(Double.valueOf(number11).doubleValue(), ONEPOINT);
            double d16 = jDoubleValue7;
            if (Math.pow(SPECIAL, 4.0d) * HUNDRED <= d16 && d16 < Math.pow(SPECIAL, 4.0d) * THOUSAND) {
                return getTransformUnitValue(jDoubleValue7, d2);
            }
            return getStringComposite(localeNumber9, this.mTeraByteShort);
        }
        if (Math.pow(SPECIAL, 4.0d) * HUNDRED <= d6 && d6 < Math.pow(SPECIAL, 4.0d) * THOUSAND) {
            String number12 = formatNumber(d6 / Math.pow(d2, 4.0d), NOPOINT, true);
            long jDoubleValue8 = (long) (Double.valueOf(number12).doubleValue() * Math.pow(d2, 4.0d));
            String localeNumber10 = formatLocaleNumber(Double.valueOf(number12).doubleValue(), NOPOINT);
            double d17 = jDoubleValue8;
            if (Math.pow(SPECIAL, 4.0d) * THOUSAND <= d17 && d17 < Math.pow(SPECIAL, 5.0d)) {
                return getTransformUnitValue(jDoubleValue8, d2);
            }
            return getStringComposite(localeNumber10, this.mTeraByteShort);
        }
        if (Math.pow(SPECIAL, 4.0d) * THOUSAND <= d6 && d6 < Math.pow(SPECIAL, 5.0d)) {
            if (d2 == THOUSAND) {
                String number13 = formatNumber(d6 / Math.pow(d2, 5.0d), TWOPOINT, true);
                return getStringComposite(formatLocaleNumber(Double.valueOf(number13).doubleValue(), TWOPOINT), this.mPetaByteShort);
            }
            if (d2 != SPECIAL) {
                return null;
            }
            if (d6 > Math.pow(SPECIAL, 4.0d) * 1023.0d) {
                return getUnitValue((long) Math.pow(SPECIAL, 5.0d));
            }
            return getStringComposite(this.mSpecialPoint, this.mPetaByteShort);
        }
        if (Math.pow(SPECIAL, 5.0d) <= d6 && d6 < Math.pow(SPECIAL, 5.0d) * TEN) {
            String number14 = formatNumber(d6 / Math.pow(SPECIAL, 5.0d), TWOPOINT, true);
            long jDoubleValue9 = (long) (Double.valueOf(number14).doubleValue() * Math.pow(SPECIAL, 5.0d));
            String localeNumber11 = formatLocaleNumber(Double.valueOf(number14).doubleValue(), TWOPOINT);
            double d18 = jDoubleValue9;
            if (Math.pow(SPECIAL, 5.0d) * TEN <= d18 && d18 < Math.pow(SPECIAL, 5.0d) * HUNDRED) {
                return getUnitValue(jDoubleValue9);
            }
            return getStringComposite(localeNumber11, this.mPetaByteShort);
        }
        if (Math.pow(SPECIAL, 5.0d) * TEN > d6 || d6 >= Math.pow(SPECIAL, 5.0d) * HUNDRED) {
            if (Math.pow(SPECIAL, 5.0d) * HUNDRED <= d6 && d6 < Math.pow(SPECIAL, 5.0d) * THOUSAND) {
                return getStringComposite(formatLocaleNumber(d6 / Math.pow(SPECIAL, 5.0d), NOPOINT), this.mPetaByteShort);
            }
            throw new IllegalArgumentException("the value of the incoming is wrong");
        }
        String number15 = formatNumber(d6 / Math.pow(SPECIAL, 5.0d), ONEPOINT, true);
        long jDoubleValue10 = (long) (Double.valueOf(number15).doubleValue() * Math.pow(SPECIAL, 5.0d));
        String localeNumber12 = formatLocaleNumber(Double.valueOf(number15).doubleValue(), ONEPOINT);
        double d19 = jDoubleValue10;
        if (Math.pow(SPECIAL, 5.0d) * HUNDRED <= d19 && d19 < Math.pow(SPECIAL, 5.0d) * THOUSAND) {
            return getUnitValue(jDoubleValue10);
        }
        return getStringComposite(localeNumber12, this.mPetaByteShort);
    }

    public String getUnitThousandValue(long j2) {
        return getTransformUnitValue(j2, THOUSAND);
    }

    public String getUnitValue(long j2) {
        return getTransformUnitValue(j2, SPECIAL);
    }

    public String getDownLoadValue(long j2, Locale locale) {
        String country = locale != null ? locale.getCountry() : null;
        if (country != null && (country.equalsIgnoreCase("CN") || country.equalsIgnoreCase("TW") || country.equalsIgnoreCase("HK"))) {
            return getChineseDownloadValue(j2);
        }
        if (country != null) {
            return getEnglishDownloadValue(j2);
        }
        return null;
    }

    public String getStripValue(long j2, Locale locale) {
        String country = locale != null ? locale.getCountry() : null;
        if (country != null && (country.equalsIgnoreCase("CN") || country.equalsIgnoreCase("TW") || country.equalsIgnoreCase("HK"))) {
            return getChineseStripValue(j2);
        }
        if (country != null) {
            return getEnglishStripValue(j2);
        }
        return null;
    }
}
