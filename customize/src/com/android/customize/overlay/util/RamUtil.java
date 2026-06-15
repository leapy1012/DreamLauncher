package com.android.customize.overlay.util;

import android.app.ActivityManager;
import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import android.text.TextUtils;
import android.text.BidiFormatter;
import android.text.format.Formatter;
import java.text.DecimalFormat;

public class RamUtil {
    public static long getAvailMemoryLong(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        if (android.os.SystemProperties.get("persist.sys.hxycustom.memory_expansion_size","-1").trim().equals("-1")) {
            return mi.availMem;
        } else {
            int hxy_memory_expansion_enable = android.provider.Settings.System.getInt(context.getContentResolver(),"hxy_memory_expansion_enable",0);
            if (hxy_memory_expansion_enable == 1) {
                String expand = android.os.SystemProperties.get("persist.sys.hxycustom.memory_expansion_size","1");
                if (!TextUtils.isEmpty(expand)) {
                    return mi.availMem + Integer.parseInt(expand) * 1024 * 1024 * 1024;
                }
                return mi.availMem;
            } else {
                return mi.availMem;
            }
        }
    }

    private static Float getAvailMemoryFloat(Context context) {
		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
		am.getMemoryInfo(mi);
		return (float)mi.availMem / 1024 / 1024 / 1024;
	}

    public static String getAvailMemoryDesc(Context context) {
        Float availMemory = getAvailMemoryFloat(context);
        DecimalFormat df = new DecimalFormat("#.0");
        if (android.os.SystemProperties.get("persist.sys.hxycustom.memory_expansion_size","-1").trim().equals("-1")) {
            return df.format(availMemory) + "GB";
        } else {
            int hxy_memory_expansion_enable = android.provider.Settings.System.getInt(context.getContentResolver(),"hxy_memory_expansion_enable",0);
            if (hxy_memory_expansion_enable == 1) {
                String expand = android.os.SystemProperties.get("persist.sys.hxycustom.memory_expansion_size","1");
                if (!TextUtils.isEmpty(expand)) {
                    availMemory = availMemory + Float.parseFloat(expand);
                }
                return df.format(availMemory) + "GB";
            } else {
                return df.format(availMemory) + "GB";
            }
        }
    }

    public static long getTotalMemoryLong(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        if (android.os.SystemProperties.get("persist.sys.hxycustom.memory_expansion_size","-1").trim().equals("-1")) {
            return mi.totalMem;
        } else {
            int hxy_memory_expansion_enable = android.provider.Settings.System.getInt(context.getContentResolver(),"hxy_memory_expansion_enable",0);
            if (hxy_memory_expansion_enable == 1) {
                String expand = android.os.SystemProperties.get("persist.sys.hxycustom.memory_expansion_size","1");
                if (!TextUtils.isEmpty(expand)) {
                    return Integer.parseInt(expand) * 1024 * 1024 * 1024 + mi.totalMem;
                }
                return mi.totalMem;
            } else {
                return mi.totalMem;
            }
        }
    }

    public static String getTotalMemoryDesc(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        BidiFormatter bidiFormatter = BidiFormatter.getInstance();
        if (android.os.SystemProperties.get("persist.sys.hxycustom.memory_expansion_size","-1").trim().equals("-1")) {
            return bidiFormatter.unicodeWrap(Formatter.formatShortFileSize(context, mi.totalMem));
        } else {
            int hxy_memory_expansion_enable = android.provider.Settings.System.getInt(context.getContentResolver(),"hxy_memory_expansion_enable",0);
            if (hxy_memory_expansion_enable == 1) {
                String expand = android.os.SystemProperties.get("persist.sys.hxycustom.memory_expansion_size","1");
                if (!TextUtils.isEmpty(expand)) {
                    return expand + "+" + bidiFormatter.unicodeWrap(Formatter.formatShortFileSize(context, mi.totalMem));
                }
                return bidiFormatter.unicodeWrap(Formatter.formatShortFileSize(context, mi.totalMem));
            } else {
                return bidiFormatter.unicodeWrap(Formatter.formatShortFileSize(context, mi.totalMem));
            }
        }
    }

    public static synchronized String runShell(String cmd) {
        String line = null;
        String[] cmdline = {"sh", "-c", cmd};
        try {
            Runtime runtime = Runtime.getRuntime();
            Process proc = runtime.exec(cmdline);
            InputStream stdout = proc.getInputStream();
            InputStreamReader osr = new InputStreamReader(stdout);
            BufferedReader obr = new BufferedReader (osr);
            while ( (line = obr.readLine()) != null ) {
                obr.close();
                osr.close();
                return line;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
