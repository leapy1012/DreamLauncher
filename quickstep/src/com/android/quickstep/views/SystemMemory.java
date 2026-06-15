/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.quickstep.views;


import android.app.ActivityManager;
import android.util.Log;
import android.widget.TextView;
import java.io.FileReader;
import java.io.BufferedReader;
import android.text.format.Formatter;
import static android.text.format.Formatter.FLAG_CALCULATE_ROUNDED;
import java.io.IOException;
import android.content.Context;
import java.text.DecimalFormat;


class SystemMemory {

	public static String getAvailMemory(Context context) {
		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
		am.getMemoryInfo(mi);
		//
		DecimalFormat df  = new DecimalFormat("0.0");
		return "" + df.format(((float)mi.availMem / 1024 / 1024 / 1024)) + "GB";
		//return Formatter.formatFileSize(context, mi.availMem,FLAG_CALCULATE_ROUNDED);
	}

	public static Float getAvailMemoryFloat(Context context) {
		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
		am.getMemoryInfo(mi);
		//
		DecimalFormat df  = new DecimalFormat("0.0");
		return (float)mi.availMem / 1024 / 1024 / 1024;
		//return Formatter.formatFileSize(context, mi.availMem,FLAG_CALCULATE_ROUNDED);
	}
	 
	public static String getTotalMemory(Context context) {
		String str1 = "/proc/meminfo";
		String str2;
		DecimalFormat df = new DecimalFormat("0.0");
		String[] arrayOfString;
		long initial_memory = 0;
		try {
			FileReader localFileReader = new FileReader(str1);
			BufferedReader localBufferedReader = new BufferedReader(localFileReader, 8192);
			str2 = localBufferedReader.readLine();
			arrayOfString = str2.split("\\s+");
			for (String num : arrayOfString) {
				Log.i(str2, num + "\t");
			}
			int i = Integer.valueOf(arrayOfString[1]).intValue();
			initial_memory = new Long((long)i*1024);
			localBufferedReader.close();
		} catch (IOException e) {
		}
		return "" + df.format(((float)initial_memory / 1024 / 1024 / 1024)) + "GB";
	}
		

    public static String getRealTotalRam(Context context){
        String path = "/proc/meminfo";
        String firstLine = null;
        int totalRam = 0 ;
        try{
            FileReader fileReader = new FileReader(path);
            BufferedReader br = new BufferedReader(fileReader,8192);
            firstLine = br.readLine().split("\\s+")[1];
            br.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        if(firstLine != null){
            totalRam = (int)Math.ceil((new Float(Float.valueOf(firstLine) / (1024 * 1024)).doubleValue()));
        }
        return totalRam + "GB";
    }

    public  static String getSystemAvailMemory(Context context) {
        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        if (activityManager != null) {
            activityManager.getMemoryInfo(memoryInfo);
        }
        android.util.Log.i("XIANG_1133", "getSystemAvailMemory:  " + memoryInfo.availMem);
        return Formatter.formatShortFileSize(context, memoryInfo.availMem);
    }
}


