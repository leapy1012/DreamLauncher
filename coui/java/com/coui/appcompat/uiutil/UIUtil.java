package com.coui.appcompat.uiutil;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.ColorStateList;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import com.coui.appcompat.R;
import com.coui.appcompat.log.COUILog;
import java.util.List;

public class UIUtil {
    public static final int ANIM_LEVEL_HIGN_END = 1;
    public static final int ANIM_LEVEL_INVALID = -1;
    public static final int ANIM_LEVEL_LOW_END = 3;
    public static final int ANIM_LEVEL_MID_END = 2;
    public static final int ANIM_LEVEL_ULTRA_LOW_END = 4;
    public static final int CONSTANT_COLOR_MASK = 16777215;
    public static final int CONSTANT_INT_EIGHT = 8;
    public static final int CONSTANT_INT_EIGHTEEN = 18;
    public static final int CONSTANT_INT_ELEVEN = 11;
    public static final int CONSTANT_INT_FIFTEEN = 15;
    public static final int CONSTANT_INT_FIVE = 5;
    public static final int CONSTANT_INT_FORE = 4;
    public static final int CONSTANT_INT_FOURTEEN = 14;
    public static final int CONSTANT_INT_NINE = 9;
    public static final int CONSTANT_INT_NINETY = 90;
    public static final int CONSTANT_INT_ONE_HUNDRED = 100;
    public static final int CONSTANT_INT_ONE_HUNDRED_TEENTY = 120;
    public static final int CONSTANT_INT_ONE_THOUSAND = 1000;
    public static final int CONSTANT_INT_SEVEN = 7;
    public static final int CONSTANT_INT_SIX = 6;
    public static final int CONSTANT_INT_SIXTEEN = 16;
    public static final int CONSTANT_INT_SIXTY = 60;
    public static final int CONSTANT_INT_TEN = 10;
    public static final int CONSTANT_INT_THIRTEEN = 13;
    public static final int CONSTANT_INT_THIRTY = 30;
    public static final int CONSTANT_INT_THREE = 3;
    public static final int CONSTANT_INT_THREE_HUNDRED = 300;
    public static final int CONSTANT_INT_THTEE_HUNDRED_THIRTY = 330;
    public static final int CONSTANT_INT_TWELVE = 12;
    public static final int CONSTANT_INT_TWO_HUNDRED_SEVENTY = 270;
    public static final boolean DEBUG = false;
    public static final int DIRECTION_BOTTOM = 3;
    public static final int DIRECTION_END = 4;
    public static final int DIRECTION_LEFT = 0;
    public static final int DIRECTION_RIGHT = 2;
    public static final int DIRECTION_START = 5;
    public static final int DIRECTION_TOP = 1;
    public static final int INT_TWENTY_FOUR = 24;
    public static final int LARGE_WIDTH_DP = 840;
    public static final int MEDIUM_WIDTH_DP = 600;
    private static final String TAG = "UIUtil";
    public static final int TWO_FIVE_FIVE = 255;
    private static SparseArray<String> sStringMap;
    public static final AnimLevel ANIM_LEVEL_SUPPORT_BLUR_MIN = AnimLevel.MID_END;
    private static int sCurrentAnimLevel = -1;

    public static int alphaColor(int i2, float f2) {
        return (i2 & CONSTANT_COLOR_MASK) | (((int) (f2 * 255.0f)) << 24);
    }

    public static float[] colorToFloats(int i2) {
        return new float[]{((i2 >> 16) & 255) / 255.0f, ((i2 >> 8) & 255) / 255.0f, (i2 & 255) / 255.0f, ((i2 >> 24) & 255) / 255.0f};
    }

    public static boolean confirmLevelAnim(AnimLevel animLevel) {
        if (sCurrentAnimLevel == -1) {
            sCurrentAnimLevel = getAnimLevelVersion();
        }
        return sCurrentAnimLevel <= animLevel.getIntValue() && sCurrentAnimLevel != -1;
    }

    public static Activity contextToActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    public static int dip2px(Context context, float f2) {
        return Math.round(f2 * context.getResources().getDisplayMetrics().density);
    }

    public static int getAdjustmentPointerIndex(MotionEvent motionEvent, int i2) {
        return Math.min(Math.max(0, i2), motionEvent.getPointerCount() - 1);
    }

    public static int getAnimLevel() {
        if (sCurrentAnimLevel == -1) {
            sCurrentAnimLevel = getAnimLevelVersion();
        }
        return sCurrentAnimLevel;
    }

    public static int getAnimLevelVersion() {
        try {
            String str = (String) Class.forName("android.os.SystemProperties").getMethod("get", String.class).invoke(null, "persist.sys.oplus.anim_level");
            if (TextUtils.isEmpty(str)) {
                return -1;
            }
            return Integer.parseInt(str.trim());
        } catch (Exception e2) {
            COUILog.e(TAG, "getAnimLevelVersion e: " + e2);
            return -1;
        }
    }

    public static ColorStateList getColorStateListCompatNoCache(Context context, int i2) {
        return context.getResources().getColorStateList(i2, context.getTheme());
    }

    public static final float getConvertedFraction(float f2, float f10, float f11) {
        return f2 + ((f10 - f2) * f11);
    }

    public static String getPrice(double d2) {
        return removeZero(d2 + "");
    }

    public static int getPx(Context context, int i2) {
        return context.getResources().getDimensionPixelSize(i2);
    }

    public static int getScreenHeightMetrics(Context context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.heightPixels;
    }

    public static int getScreenHeightRealSize(Context context) {
        return getScreenSize(context).y;
    }

    public static Point getScreenSize(Context context) {
        WindowManager windowManager;
        Display defaultDisplay;
        Point point = new Point();
        if (context != null && (windowManager = (WindowManager) context.getSystemService("window")) != null && (defaultDisplay = windowManager.getDefaultDisplay()) != null) {
            defaultDisplay.getRealSize(point);
        }
        return point;
    }

    public static int getScreenWidthMetrics(Context context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.widthPixels;
    }

    public static int getScreenWidthRealSize(Context context) {
        return getScreenSize(context).x;
    }

    public static int getStatusBarHeight(Context context) {
        int identifier = context.getApplicationContext().getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (identifier > 0) {
            try {
                return context.getApplicationContext().getResources().getDimensionPixelSize(identifier);
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return -1;
    }

    public static String getString(Context context, int i2) {
        if (sStringMap == null) {
            sStringMap = new SparseArray<>();
        }
        String str = sStringMap.get(i2);
        if (str != null) {
            return str;
        }
        String string = context.getString(i2);
        sStringMap.put(i2, string);
        return string;
    }

    public static void initEditViewCursor(TextView textView, int i2) {
        if (textView == null) {
            return;
        }
        CharSequence text = textView.getText();
        if (text instanceof Spannable) {
            Spannable spannable = (Spannable) text;
            if (i2 <= text.length()) {
                Selection.setSelection(spannable, i2);
                return;
            }
            try {
                throw new Exception("the cursor of EditText is indexOutOfBoundException!!!!!");
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    public static boolean isInMarketStack(Context context) {
        List<ActivityManager.RunningTaskInfo> runningTasks = ((ActivityManager) context.getApplicationContext().getSystemService("activity")).getRunningTasks(1);
        if (runningTasks == null || runningTasks.isEmpty()) {
            return false;
        }
        return context.getPackageName().equals(runningTasks.get(0).baseActivity.getPackageName());
    }

    public static boolean isInVisibleRect(View view) {
        return view.getLocalVisibleRect(new Rect()) && view.getVisibility() == 0 && view.isShown();
    }

    public static boolean isLargeScreenBaseOnRealSize(Context context) {
        return px2dip(context, getScreenWidthRealSize(context)) >= 840;
    }

    public static boolean isSmallScreenBaseOnRealSize(Context context) {
        return px2dip(context, getScreenWidthRealSize(context)) < 600;
    }

    public static GradientDrawable makeGradientDrawable(int i2, int i6) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColors(new int[]{i2, i6});
        return gradientDrawable;
    }

    public static StateListDrawable makeSelector(Drawable drawable, Drawable drawable2) {
        StateListDrawable stateListDrawable = new StateListDrawable();
        stateListDrawable.addState(new int[]{-16842908, -16842913, -16842919}, drawable);
        stateListDrawable.addState(new int[]{-16842908, 16842913, -16842919}, drawable2);
        stateListDrawable.addState(new int[]{16842908, -16842913, -16842919}, drawable2);
        stateListDrawable.addState(new int[]{16842908, 16842913, -16842919}, drawable2);
        stateListDrawable.addState(new int[]{16842913, 16842919}, drawable2);
        stateListDrawable.addState(new int[]{16842919}, drawable2);
        return stateListDrawable;
    }

    public static GradientDrawable makeShapeDrawable(float f2, int i2, int i6, int i10) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(i10);
        if (f2 > 0.0f) {
            gradientDrawable.setCornerRadius(f2);
        }
        if (i2 > 0) {
            gradientDrawable.setStroke(i2, i6);
        }
        return gradientDrawable;
    }

    public static int px2dip(Context context, int i2) {
        return Math.round(i2 / context.getResources().getDisplayMetrics().density);
    }

    public static String removeZero(String str) {
        return str.indexOf(".") > 0 ? str.replaceAll("0+?$", "").replaceAll("[.]$", "") : str;
    }

    public static void safeForceHasOverlappingRendering(View view, boolean z6) {
        if (view == null) {
            return;
        }
        view.forceHasOverlappingRendering(z6);
    }

    public static void safeSetOutlineAmbientShadowColor(View view, int i2) {
        if (view == null) {
            return;
        }
        view.setOutlineAmbientShadowColor(i2);
    }

    public static void safeSetOutlineSpotShadowColor(View view, int i2) {
        if (view == null) {
            return;
        }
        view.setOutlineSpotShadowColor(i2);
    }

    public static void setElevationToView(View view, int i2, int i6) {
        setElevationToView(view, i2, i6, view.getResources().getDimensionPixelOffset(R.dimen.support_shadow_size_level_for_lowerP));
    }

    public static void setMargin(View view, int i2, int i6) {
        if (view != null) {
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                if (i2 == 0) {
                    ((ViewGroup.MarginLayoutParams) layoutParams).leftMargin = i6;
                } else if (i2 == 1) {
                    ((ViewGroup.MarginLayoutParams) layoutParams).topMargin = i6;
                } else if (i2 == 2) {
                    ((ViewGroup.MarginLayoutParams) layoutParams).rightMargin = i6;
                } else if (i2 == 3) {
                    ((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin = i6;
                } else if (i2 == 4) {
                    ((ViewGroup.MarginLayoutParams) layoutParams).setMarginEnd(i6);
                } else if (i2 == 5) {
                    ((ViewGroup.MarginLayoutParams) layoutParams).setMarginStart(i6);
                }
                view.setLayoutParams(layoutParams);
            }
        }
    }

    public static void setElevationToView(View view, int i2, int i6, int i10) {
        if (view == null) {
            return;
        }
        view.setOutlineSpotShadowColor(i6);
        view.setElevation(i2);
    }

    public static StateListDrawable makeSelector(int i2, int i6) {
        return makeSelector(new ColorDrawable(i2), new ColorDrawable(i6));
    }
}
