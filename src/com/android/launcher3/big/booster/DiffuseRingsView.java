package com.android.launcher3.big.booster;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * ColorOS/CM DiffuseView: concentric white rings expand outward (the "spread").
 */
public class DiffuseRingsView extends View {

    private static final int MAX_RINGS = 4;
    private static final long SPAWN_INTERVAL_MS = 280L;

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Ring> mRings = new ArrayList<>();
    private boolean mRunning;
    private float mStartRadiusPx;
    private float mEndRadiusPx;
    private float mStrokeWidthPx;
    private float mSpeedPx;
    private long mLastSpawnMs;

    public DiffuseRingsView(Context context) {
        this(context, null);
    }

    public DiffuseRingsView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DiffuseRingsView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        float density = getResources().getDisplayMetrics().density;
        mStartRadiusPx = 91f * density;
        mEndRadiusPx = 116f * density;
        mStrokeWidthPx = 0.5f * density;
        mSpeedPx = 2f * density;
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mStrokeWidthPx);
        mPaint.setColor(0xFFFFFFFF);
    }

    public void start() {
        mRunning = true;
        mRings.clear();
        mLastSpawnMs = 0L;
        spawnRing();
        invalidate();
    }

    public void stop() {
        mRunning = false;
        mRings.clear();
        invalidate();
    }

    private void spawnRing() {
        if (mRings.size() >= MAX_RINGS) {
            return;
        }
        Ring ring = new Ring();
        ring.radius = mStartRadiusPx;
        ring.alpha = 255;
        mRings.add(ring);
        mLastSpawnMs = System.currentTimeMillis();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!mRunning && mRings.isEmpty()) {
            return;
        }
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        Iterator<Ring> it = mRings.iterator();
        while (it.hasNext()) {
            Ring ring = it.next();
            ring.radius += mSpeedPx;
            ring.alpha = Math.max(0, (int) (255f * (1f - (ring.radius - mStartRadiusPx)
                    / Math.max(1f, mEndRadiusPx - mStartRadiusPx))));
            if (ring.radius >= mEndRadiusPx || ring.alpha <= 0) {
                it.remove();
                continue;
            }
            mPaint.setAlpha(ring.alpha);
            canvas.drawCircle(cx, cy, ring.radius, mPaint);
        }
        if (mRunning) {
            long now = System.currentTimeMillis();
            if (now - mLastSpawnMs >= SPAWN_INTERVAL_MS) {
                spawnRing();
            }
            postInvalidateOnAnimation();
        }
    }

    private static final class Ring {
        float radius;
        int alpha;
    }
}
