package com.android.launcher3.big.anim;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import com.android.launcher3.big.HxyAnimBubbleTextView;
import com.android.launcher3.R;
import com.android.launcher3.anim.BaseParams;

import java.util.function.Consumer;

public class TranslateParams extends BaseParams {
    static int GOBACK = 1;
    static int GOTO = 0;
    private float alpha = 255.0f;
    private final ClearCallBack callBack;
    float distance;
    int mDirection;
    float translate;
    int mType = 0;

    public TranslateParams(Consumer<View> call, Context context, int dist, HxyAnimBubbleTextView view) {
        super(call);
        mContext = context;
        distance = (float) dist;
        callBack = new ClearCallBack(view);
    }

    @Override
    public void init(String path, String zipPath, String themedName) {
        mBg = drawableToBitmap(getWallPaperDrawable(mContext, "hxy_clear_bg"));
        mSrc = drawableToBitmap(getWallPaperDrawable(mContext, "hxy_clear_src"));
        getDrawable(mContext, themedName);
        if (mBg != null) {
            mBg.getHeight();
        }
        if (mSrc != null) {
            mSrc.getHeight();
        }
    }

    private void getDrawable(Context context, String name) {
        if (name.contains("golden") && mSrc == null && mBg == null) {
            mType = 1;
            mSrc = BitmapFactory.decodeResource(context.getResources(), R.mipmap.golden_clear_src);
            // mBg = BitmapFactory.decodeResource(context.getResources(), R.mipmap.golden_clear_bg);
        } else if (name.contains("hills") && mSrc == null && mBg == null) {
            mType = 0;
            mSrc = BitmapFactory.decodeResource(context.getResources(), R.mipmap.hills_clear_src);
            // mBg = BitmapFactory.decodeResource(context.getResources(), R.mipmap.hills_clear_bg);
        } else if (name.contains("starry") && mSrc == null && mBg == null) {
            mType = 1;
            mSrc = BitmapFactory.decodeResource(context.getResources(), R.mipmap.starry_clear_src);
            // mBg = BitmapFactory.decodeResource(context.getResources(), R.mipmap.starry_clear_bg);
        } else if (name.contains("tunnel") && mSrc == null && mBg == null) {
            mType = 1;
            mSrc = BitmapFactory.decodeResource(context.getResources(), R.mipmap.tunnel_clear_src);
            // mBg = BitmapFactory.decodeResource(context.getResources(), R.mipmap.tunnel_clear_bg);
        } else {
            mType = 0;
            // mBg = BitmapFactory.decodeResource(context.getResources(), R.mipmap.hxy_clear_bg);
            mSrc = BitmapFactory.decodeResource(context.getResources(), R.mipmap.hxy_clear_src);
        }
    }

    @Override
    public void setAnimationProgress(float progress) {
        super.setAnimationProgress(progress);
        if (mDirection == GOTO) {
            translate = (-distance) * progress;
            alpha = (1.0f - progress) * 255.0f;
            return;
        }
        translate = distance * (1.0f - progress);
        alpha = 255.0f * progress;
    }

    @Override
    public int getDuration() {
        return 350;
    }

    public void goBack(int direction) {
        if ((direction != GOTO || !callBack.onClick()) && mDirection != GOBACK) {
            mDirection = direction;
            startAnimation();
        }
    }

    public void startAnimation() {
        startProgressAnimation();
    }

    public void stopAnimation() {
        cancelProgressAnimation();
        if (mBg != null || !mBg.isRecycled()) {
            mBg.recycle();
            mBg = null;
        }
        if (mSrc != null || !mSrc.isRecycled()) {
            mSrc.recycle();
            mSrc = null;
        }
    }

    public void onClick() {
        if (!callBack.onClick() && mDirection != GOBACK) {
            goUp();
        }
    }

    public void goUp() {
        this.mDirection = GOTO;
        startAnimation();
    }

    @Override
    public void onAttachedToWindow(HxyAnimBubbleTextView icon) {
        super.onAttachedToWindow(icon);
        icon.setText(R.string.app_name_memory_clean);
    }

    @Override
    public void onDetachedFromWindow(HxyAnimBubbleTextView icon) {
        super.onDetachedFromWindow(icon);
    }

    @Override
    public void onDraw(Canvas canvas, Rect rect) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        if (mBg != null) {
            canvas.save();
            canvas.drawBitmap(mBg, (Rect) null, rect, paint);
            canvas.restore();
        }
        if (mSrc != null) {
            canvas.save();
            canvas.clipRect(rect.left, rect.top, rect.right, rect.bottom);
            if (mType == 1) {
                rect.offset((int) -translate, (int) translate);
            } else {
                rect.offset(0, (int) translate);
            }
            paint.setAlpha((int) alpha);
            canvas.drawBitmap(mSrc, (Rect) null, rect, paint);
            canvas.restore();
        }
    }

    @Override
    public void onProgressAnimationEnd() {
        super.onProgressAnimationEnd();
        if (mDirection == GOTO) {
            goBack(GOBACK);
            callBack.onRunning();
            return;
        }
        callBack.onEnd();
        mDirection = GOTO;
    }

    @Override
    public void onProgressAnimationBegin() {
        super.onProgressAnimationBegin();
        callBack.onStart(this);
    }
}
