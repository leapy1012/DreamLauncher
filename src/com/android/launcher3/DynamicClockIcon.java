package com.android.launcher3;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.SystemClock;
import androidx.core.content.res.ResourcesCompat;
import com.android.launcher3.icons.FastBitmapDrawable;
import com.android.launcher3.icons.LauncherIcons;
import java.util.Objects;
import java.util.Calendar;
import android.provider.Settings;

public class DynamicClockIcon extends FastBitmapDrawable {

    private Context mContext;
    private static float clockSecondLengthScale = 0.3f;
    private static float clockMinuteLengthScale = 0.25f;
    private static float clockHourLengthScale = 0.2f;
    private static float clockPointerEndScale = 0.2f;

    private final Config mConfig;
    private final Camera mCamera;
    private final Matrix mMatrix;

    private final int mCenterX;
    private final int mCenterY;

    private int mSecond;
    private int mMinute;
    private int mHour;

    private BroadcastReceiver mTimeTickReceiver;
    private IntentFilter mIntentFilter;

    private static Calendar mCalendar;

    private Handler mHandler;
    private boolean mHasSecond = false;
    private static final float ICON_SCALE = 0.83f;

    public DynamicClockIcon(Context context, int resourceId) {
        super(LauncherIcons.obtain(context).createBadgedIconBitmap(Objects.requireNonNull(ResourcesCompat.getDrawable(context.getResources(), resourceId, null))));
        mContext = context;
        mHasSecond = mContext.getResources().getBoolean(R.bool.show_clock_second);
        String themedName = Settings.Global.getString(mContext.getContentResolver(), "themed");
        mConfig = new Config(themedName);
        mCamera = new Camera();
        mMatrix = new Matrix();

        mCalendar = Calendar.getInstance();
        mCenterX = mConfig.minuteIcon.getWidth();
        mCenterY = mConfig.minuteIcon.getHeight();

        mSecond = mCalendar.get(Calendar.SECOND);
        mMinute = mCalendar.get(Calendar.MINUTE);
        mHour = mCalendar.get(Calendar.HOUR_OF_DAY);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(Intent.ACTION_TIME_TICK);
        mIntentFilter.addAction(Intent.ACTION_TIME_CHANGED);
        mIntentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        mTimeTickReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                refreshView();
                run();
            }
        };

        mHandler = new Handler();
    }

    @Override
    protected void drawInternal(Canvas canvas, Rect bounds) {
        super.drawInternal(canvas, bounds);
        float scaleX = getBounds().width() / (float) mBitmap.getWidth();
        float scaleY = getBounds().height() / (float) mBitmap.getHeight();
        canvas.save();
        canvas.scale(scaleX, scaleY);
        drawTime(canvas);
        canvas.restore();
    }

    private void drawTime(Canvas canvas) {
        drawHour(canvas);
        drawMinute(canvas);
        drawSecond(canvas);
    }

    private void drawSecond(Canvas canvas) {
        if (!hasSecond() || mConfig.secondIcon == null) {
            return;
        }

        final float second = Calendar.getInstance().get(Calendar.SECOND);
        final float delta = (360.0f / 60) * second;
        canvas.save();
        // 移动到画布中心
        float centerX = mBitmap.getWidth() / 2.0f - mCenterX / 2.0f;
        float centerY = mBitmap.getHeight() / 2.0f - mCenterY / 2.0f;
        canvas.translate(centerX, centerY);
        rotate(delta);
        mMatrix.postScale(ICON_SCALE, ICON_SCALE, mCenterX / 2.0f, mCenterY / 2.0f);
        canvas.drawBitmap(mConfig.secondIcon, mMatrix, mPaint);
        canvas.restore();
    }

    private void drawMinute(Canvas canvas) {
        final float minute = Calendar.getInstance().get(Calendar.MINUTE);
        final float delta = (360.0f / 60) * minute;
        canvas.save();
        // 移动到画布中心
        float centerX = mBitmap.getWidth() / 2.0f - mCenterX / 2.0f;
        float centerY = mBitmap.getHeight() / 2.0f - mCenterY / 2.0f;
        canvas.translate(centerX, centerY);
        rotate(delta);
        mMatrix.postScale(ICON_SCALE, ICON_SCALE, mCenterX / 2.0f, mCenterY / 2.0f);
        canvas.drawBitmap(mConfig.minuteIcon, mMatrix, mPaint);
        canvas.restore();
    }

    private void drawHour(Canvas canvas) {
        final float hour = Calendar.getInstance().get(Calendar.HOUR);
        final float delta = (360.0f / 12) * (hour + Calendar.getInstance().get(Calendar.MINUTE) / 60.0f);
        canvas.save();
        // 移动到画布中心
        float centerX = mBitmap.getWidth() / 2.0f - mCenterX / 2.0f;
        float centerY = mBitmap.getHeight() / 2.0f - mCenterY / 2.0f;
        canvas.translate(centerX, centerY);
        rotate(delta);
        mMatrix.postScale(ICON_SCALE, ICON_SCALE, mCenterX / 2.0f, mCenterY / 2.0f);
        canvas.drawBitmap(mConfig.hourIcon, mMatrix, mPaint);
        canvas.restore();
    }

    private void rotate(float delta) {
        mMatrix.reset();
        mCamera.save();
        mCamera.rotateZ(-delta);
        mCamera.getMatrix(mMatrix);
        mCamera.restore();
        mMatrix.preTranslate(-mCenterX / 2.0f, -mCenterY / 2.0f);
        mMatrix.postTranslate(mCenterX / 2.0f, mCenterY / 2.0f);
    }

    public void refreshView() {
        invalidateSelf();
    }

    public void run() {
        mHandler.removeCallbacks(mTicker);
        mHandler.post(mTicker);
    }

    private boolean hasSecond() {
        return mHasSecond;
    }

    private final Runnable mTicker = new Runnable() {
        public void run() {
            refreshView();
            long now = SystemClock.uptimeMillis();
            long next = now + (1000 - now % 1000);
            mHandler.postAtTime(mTicker, next);
        }
    };

    class Config {

        public Bitmap secondIcon;
        public Bitmap minuteIcon;
        public Bitmap hourIcon;

        public Config(String themedName) {
            if (themedName.contains("hills")) {
                minuteIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.theme_hills_clock_minute);
                hourIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.theme_hills_clock_hour);
                mHasSecond = false;
            } else if (themedName.contains("tunnel")) {
                secondIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.theme_tunnel_clock_second);
                minuteIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.theme_tunnel_clock_minute);
                hourIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.theme_tunnel_clock_hour);
                mHasSecond = true;
            } else if (themedName.contains("forest")) {
                minuteIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.theme_forest_clock_minute);
                hourIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.theme_forest_clock_hour);
                mHasSecond = false;
            } else if (themedName.contains("winter")) {
                minuteIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.theme_winter_clock_minute);
                hourIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.theme_winter_clock_hour);
                mHasSecond = false;
            } else if (themedName.contains("golden")) {
                secondIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.theme_golden_clock_second);
                minuteIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.theme_golden_clock_minute);
                hourIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.theme_golden_clock_hour);
                mHasSecond = true;
            } else if (themedName.contains("night")) {
                minuteIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.theme_night_clock_minute);
                hourIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.theme_night_clock_hour);
                mHasSecond = false;
            } else if (themedName.contains("minimalist")) {
                minuteIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.theme_default_clock_minute);
                hourIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.theme_default_clock_hour);
                mHasSecond = false;
            } else if (themedName.contains("glow")) {
                minuteIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.theme_glow_clock_minute);
                hourIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.theme_glow_clock_hour);
                mHasSecond = false;
            } else if (themedName.contains("supercar")) {
                secondIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.theme_supercar_clock_second);
                minuteIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.theme_supercar_clock_minute);
                hourIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.theme_supercar_clock_hour);
                mHasSecond = true;
            } else if (themedName.contains("stillness")) {
                secondIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.theme_stillness_clock_second);
                minuteIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.theme_stillness_clock_minute);
                hourIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.theme_stillness_clock_hour);
                mHasSecond = true;
            } else if (themedName.contains("starry")) {
                mHasSecond = false;
            } else {
                secondIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_live_clock_second);
                minuteIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_live_clock_minute);
                hourIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_live_clock_hour);
                mHasSecond = true;
            }
        }
    }
}
