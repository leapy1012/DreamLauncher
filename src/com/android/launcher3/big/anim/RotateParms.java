package com.android.launcher3.big.anim;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;
import com.android.launcher3.R;
import com.android.launcher3.Launcher;
import com.android.launcher3.util.Executors;
import com.android.launcher3.big.BlueTaskWall;
import java.util.function.Consumer;
import android.graphics.drawable.Drawable;
import com.android.launcher3.anim.BaseParams;
import com.android.launcher3.big.HxyAnimBubbleTextView;
import com.android.launcher3.big.LauncherBackgroudView;

public class RotateParms extends BaseParams {
    private static final String TAG = "RotateParms";
    private WallpaperSetCallBack wallpaperSetCallBack;
    private ClearCallBack clearCallBack;

    private float degrees;
    private Context mContext;
    private int mType = 0;

    public RotateParms(Consumer<View> call, Context context, HxyAnimBubbleTextView icon, int type) {
        super(call);
        mType = type;
        mContext = context;
        if (type == 1) {
            clearCallBack = new ClearCallBack(icon);
        } else {
            wallpaperSetCallBack = new WallpaperSetCallBack(icon);
        }
    }

    @Override
    public void init(String path, String zipPath, String themedName) {
        mBg = drawableToBitmap(getWallPaperDrawable(mContext, "choosewallpapaer_bg"));
        mSrc = drawableToBitmap(getWallPaperDrawable(mContext, "choosewallpapaer_src"));
        getRotateDrawable(mContext, themedName);
        if (mBg != null) {
            mBg.getHeight();
        }
        if (mSrc != null) {
            mSrc.getHeight();
        }
    }

    private void getRotateDrawable(Context context, String name) {
        if (name.contains("minimalist") && mSrc == null && mBg == null) {
            if (mType == 1) {
                mSrc = BitmapFactory.decodeResource(context.getResources(), R.mipmap.minimalist_clear_src);
                // mBg = BitmapFactory.decodeResource(context.getResources(), R.mipmap.minimalist_clear_bg);
            } else {
                mSrc = BitmapFactory.decodeResource(context.getResources(), R.mipmap.minimalist_choosewallpapaer_src);
                // mBg = BitmapFactory.decodeResource(context.getResources(), R.mipmap.minimalist_choosewallpapaer_bg);
            }
        } else if (name.contains("forest") && mSrc == null && mBg == null) {
            if (mType == 1) {
                mSrc = BitmapFactory.decodeResource(context.getResources(), R.mipmap.forest_clear_src);
                // mBg = BitmapFactory.decodeResource(context.getResources(), R.mipmap.forest_clear_bg);
            } else {
                mSrc = BitmapFactory.decodeResource(context.getResources(), R.mipmap.forest_choosewallpapaer_src);
                // mBg = BitmapFactory.decodeResource(context.getResources(), R.mipmap.forest_choosewallpapaer_bg);
            }
        } else if (name.contains("glow") && mSrc == null && mBg == null) {
            if (mType == 1) {
                mSrc = BitmapFactory.decodeResource(context.getResources(), R.mipmap.glow_clear_src);
                // mBg = BitmapFactory.decodeResource(context.getResources(), R.mipmap.glow_clear_bg);
            } else {
                mSrc = BitmapFactory.decodeResource(context.getResources(), R.mipmap.glow_choosewallpapaer_src);
                // mBg = BitmapFactory.decodeResource(context.getResources(), R.mipmap.glow_choosewallpapaer_bg);
            }
        } else if (name.contains("night") && mSrc == null && mBg == null) {
            if (mType == 1) {
                mSrc = BitmapFactory.decodeResource(context.getResources(), R.mipmap.night_clear_src);
                // mBg = BitmapFactory.decodeResource(context.getResources(), R.mipmap.night_clear_bg);
            } else {
                mSrc = BitmapFactory.decodeResource(context.getResources(), R.mipmap.night_choosewallpapaer_src);
                // mBg = BitmapFactory.decodeResource(context.getResources(), R.mipmap.night_choosewallpapaer_bg);
            }
        } else if (name.contains("golden") && mSrc == null && mBg == null) {
            mSrc = BitmapFactory.decodeResource(context.getResources(), R.mipmap.golden_choosewallpapaer_src);
            // mBg = BitmapFactory.decodeResource(context.getResources(), R.mipmap.golden_choosewallpapaer_bg);
        } else if (name.contains("hills") && mSrc == null && mBg == null) {
            mSrc = BitmapFactory.decodeResource(context.getResources(), R.mipmap.hills_choosewallpapaer_src);
            // mBg = BitmapFactory.decodeResource(context.getResources(), R.mipmap.hills_choosewallpapaer_bg);
        } else if (name.contains("starry") && mSrc == null && mBg == null) {
            mSrc = BitmapFactory.decodeResource(context.getResources(), R.mipmap.starry_choosewallpapaer_src);
            // mBg = BitmapFactory.decodeResource(context.getResources(), R.mipmap.starry_choosewallpapaer_bg);
        } else if (name.contains("stillness") && mSrc == null && mBg == null) {
            if (mType == 1) {
                mSrc = BitmapFactory.decodeResource(context.getResources(), R.mipmap.stillness_clear_src);
                // mBg = BitmapFactory.decodeResource(context.getResources(), R.mipmap.stillness_clear_bg);
            } else {
                mSrc = BitmapFactory.decodeResource(context.getResources(), R.mipmap.stillness_choosewallpapaer_src);
                // mBg = BitmapFactory.decodeResource(context.getResources(), R.mipmap.stillness_choosewallpapaer_bg);
            }
        } else if (name.contains("supercar") && mSrc == null && mBg == null) {
            if (mType == 1) {
                mSrc = BitmapFactory.decodeResource(context.getResources(), R.mipmap.supercar_clear_src);
                // mBg = BitmapFactory.decodeResource(context.getResources(), R.mipmap.supercar_clear_bg);
            } else {
                mSrc = BitmapFactory.decodeResource(context.getResources(), R.mipmap.supercar_choosewallpapaer_src);
                // mBg = BitmapFactory.decodeResource(context.getResources(), R.mipmap.supercar_choosewallpapaer_bg);
            }
        } else if (name.contains("tunnel") && mSrc == null && mBg == null) {
            mSrc = BitmapFactory.decodeResource(context.getResources(), R.mipmap.tunnel_choosewallpapaer_src);
            // mBg = BitmapFactory.decodeResource(context.getResources(), R.mipmap.tunnel_choosewallpapaer_bg);
        } else if (name.contains("winter") && mSrc == null && mBg == null) {
            if (mType == 1) {
                mSrc = BitmapFactory.decodeResource(context.getResources(), R.mipmap.winter_clear_src);
                // mBg = BitmapFactory.decodeResource(context.getResources(), R.mipmap.winter_clear_bg);
            } else {
                mSrc = BitmapFactory.decodeResource(context.getResources(), R.mipmap.winter_choosewallpapaer_src);
                // mBg = BitmapFactory.decodeResource(context.getResources(), R.mipmap.winter_choosewallpapaer_bg);
            }
        } else if (mSrc == null && mBg == null) {
            // mBg = BitmapFactory.decodeResource(context.getResources(), R.mipmap.choosewallpapaer_bg);
            mSrc = BitmapFactory.decodeResource(context.getResources(), R.mipmap.choosewallpapaer_src);
        }
    }

    @Override
    public void setAnimationProgress(float progress) {
        degrees = 360.0f * progress;
        super.setAnimationProgress(progress);
    }

    @Override
    public int getDuration() {
        return 1000;
    }

    @Override
    public void startAnimation() {
    }

    @Override
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

    @Override
    public void onClick() {
        if (wallpaperSetCallBack != null && !wallpaperSetCallBack.onClick()) {
            LauncherBackgroudView view = Launcher.getLauncher(mContext).findViewById(R.id.wallpaper_set_anim);
            if (!view.isRunning()) {
                view.startAnim(View.ALPHA, 0.0f, 1.0f, true, (Consumer<String>) null, (Consumer) obj -> wallpaperSetCallBack.onSetWallpaper());
                startProgressAnimation();
            }
        } else if (clearCallBack != null && !clearCallBack.onClick()) {
            startProgressAnimation();
        }
    }

    @Override
    public void onAttachedToWindow(HxyAnimBubbleTextView icon) {
        super.onAttachedToWindow(icon);
        if (wallpaperSetCallBack != null) {
            icon.setText(R.string.wallpaper_choose_widget_title);
        } else if (clearCallBack != null) {
            icon.setText(R.string.app_name_memory_clean);
        }
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
			canvas.rotate(degrees, rect.centerX(), rect.centerY());
			canvas.drawBitmap(mSrc, (Rect) null, rect, paint);
			canvas.restore();
		}
    }

    @Override
    public void onProgressAnimationBegin() {
        super.onProgressAnimationBegin();
        mIsEnd = false;
        if (wallpaperSetCallBack != null) {
            wallpaperSetCallBack.onStart();
        } else if (clearCallBack != null) {
            clearCallBack.onStart();
        }
    }

    @Override
    public void onProgressAnimationEnd() {
        super.onProgressAnimationEnd();
        if (wallpaperSetCallBack != null) {
            wallpaperSetCallBack.onEnd();
        } else if (clearCallBack != null) {
            clearCallBack.onEnd();
        }
    }
}
