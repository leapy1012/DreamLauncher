package com.coui.appcompat.lockview;

import android.content.Context;
import android.util.Log;
import com.coui.appcompat.lockview.COUILockPatternView;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/* JADX INFO: loaded from: classes11.dex */
public class COUILockPatternUtils {
    private static final int COLUMNS = 3;
    private static final boolean DEBUG = false;
    private static final int ROWS = 3;
    private static final String TAG = "COUILockPatternUtils";
    private static final String UTF_8 = "UTF-8";
    private final Context mContext;

    public COUILockPatternUtils(Context context) {
        this.mContext = context;
    }

    public static String patternToString(List<COUILockPatternView.Cell> list) {
        if (list == null) {
            return "";
        }
        int size = list.size();
        byte[] bArr = new byte[size];
        for (int i2 = 0; i2 < size; i2++) {
            COUILockPatternView.Cell cell = list.get(i2);
            bArr[i2] = (byte) ((cell.getRow() * 3) + cell.getColumn() + 49);
        }
        try {
            return new String(bArr, "UTF-8");
        } catch (UnsupportedEncodingException e2) {
            Log.e(TAG, "patternToString e:" + e2.getMessage());
            e2.printStackTrace();
            return null;
        }
    }

    public static List<COUILockPatternView.Cell> stringToPattern(String str) {
        byte[] bytes = null;
        if (str == null) {
            return null;
        }
        ArrayList arrayList = new ArrayList();
        try {
            bytes = str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e2) {
            Log.e(TAG, "stringToPattern e:" + e2.getMessage());
            e2.printStackTrace();
        }
        for (byte b2 : bytes) {
            byte b3 = (byte) (b2 - 49);
            arrayList.add(COUILockPatternView.Cell.of(b3 / 3, b3 % 3));
        }
        return arrayList;
    }
}
