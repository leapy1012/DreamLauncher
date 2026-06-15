package com.android.launcher3.big;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.content.res.ResourcesCompat;

import com.android.launcher3.anim.BaseParams;
import com.android.launcher3.icons.FastBitmapDrawable;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.model.data.ItemInfoWithIcon;
import com.android.launcher3.views.DoubleShadowBubbleTextView;
import com.android.launcher3.R;
import com.android.launcher3.big.anim.TranslateParams;
import com.android.launcher3.big.anim.RotateParms;
import com.android.launcher3.BuildConfig;
import static com.android.launcher3.folder.Folder.ITEM_TYPE_ADD_FOLDER;
import android.provider.Settings;
import static com.android.launcher3.util.Themes.KEY_THEMED;

@RemoteViews.RemoteView
public class HxyAnimBubbleTextView extends DoubleShadowBubbleTextView {
    private static final String TAG = "HxyAnimBubbleTextView";
    boolean isClear;
    boolean isFlashLight;
    boolean isWallpaperSet;
    BaseParams mDrawParams;
    private FastBitmapDrawable mAddFolderIconDrawable; // 缓存图标
    private ComponentName mClean;
	private ComponentName mWallpaper;
    private Context mContext;

    public HxyAnimBubbleTextView(Context context) {
        super(context);
        initIcons(context);
    }

    public HxyAnimBubbleTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HxyAnimBubbleTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initIcons(context);
    }

    private void initIcons(Context context) {
        mContext = context;
        mClean = new ComponentName(BuildConfig.APPLICATION_ID, "com.android.launcher3.big.memoryclean.MemoryCleanActivity");
		mWallpaper = new ComponentName(BuildConfig.APPLICATION_ID, "com.android.launcher3.settings.WallpaperChangeActivity");
        try {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) ResourcesCompat.getDrawable(context.getResources(), R.drawable.add_folder_icon, null);
            if (bitmapDrawable != null) {
                mAddFolderIconDrawable = new FastBitmapDrawable(bitmapDrawable.getBitmap());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load add_folder_icon", e);
        }
    }

    @Override
    public void setItemInfo(ItemInfoWithIcon itemInfo) {
        super.setItemInfo(itemInfo);
        if (getTag() != null) {
            inits((ItemInfo) getTag());
        }
    }

    public void inits(ItemInfo info) {
        String themedName = Settings.Global.getString(mContext.getContentResolver(), KEY_THEMED);
        android.util.Log.d(TAG, "init: themedName = " + themedName);
        if (info != null && info.getIntent() != null) {
            ComponentName cn = info.getIntent().getComponent();
            if (themedName.contains("minimalist") || themedName.contains("forest") || themedName.contains("glow") || themedName.contains("night") || themedName.contains("stillness") || themedName.contains("supercar") || themedName.contains("winter")) {
                if (cn != null && cn.equals(mWallpaper)) {
                    isWallpaperSet = true;
                    RotateParms rotateParms = new RotateParms(view -> {
                        invalidate();
                        setLayerType(View.LAYER_TYPE_HARDWARE, null);
                    }, getContext(), this, 0);
                    mDrawParams = rotateParms;
                    rotateParms.init(null, null, themedName);
                } else if (cn != null && cn.equals(mClean)) {
                    isClear = true;
                    RotateParms rotateParms = new RotateParms(view -> {
                        invalidate();
                        setLayerType(View.LAYER_TYPE_HARDWARE, null);
                    }, getContext(), this, 1);
                    mDrawParams = rotateParms;
                    rotateParms.init(null, null, themedName);
                }
            } else {
                if (cn != null && cn.equals(mClean)) {
                    this.isClear = true;
                    TranslateParams translateParams = new TranslateParams(view -> {
                        invalidate();
                        setLayerType(View.LAYER_TYPE_HARDWARE, null);
                    }, getContext(), mIconSize, this);
                    mDrawParams = translateParams;
                    translateParams.init(null, null, themedName);
                } else if (cn != null && cn.equals(mWallpaper)) {
                    this.isWallpaperSet = true;
                    RotateParms rotateParms = new RotateParms(view -> {
                        invalidate();
                        setLayerType(View.LAYER_TYPE_HARDWARE, null);
                    }, getContext(), this, 0);
                    mDrawParams = rotateParms;
                    rotateParms.init(null, null, themedName);
                }
            }
        }
    }
	
	

    @Override
    public void onDraw(Canvas canvas) {
        if (getTag() != null) {
            ItemInfo info = (ItemInfo) getTag();
            if (info != null && info.itemType == ITEM_TYPE_ADD_FOLDER && TextUtils.isEmpty(getText())) {
                if (mAddFolderIconDrawable != null) {
                    setIcon(mAddFolderIconDrawable);
                }
                setText(getContext().getResources().getString(R.string.str_add_to));
            }
        }

        super.onDraw(canvas);
        if (isDrawableAniView() && mDrawParams != null) {
            mDrawParams.onDraw(canvas, getIconRect());
        }
    }

    public boolean isDrawableAniView() {
        return this.isClear || this.isWallpaperSet || this.isFlashLight;
    }

    public void onClick() {
        if (mDrawParams != null) {
            mDrawParams.onClick();
        }
    }
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (getTag() instanceof ItemInfo) {
            inits((ItemInfo) getTag());
        }
        if (mDrawParams != null) {
            mDrawParams.onAttachedToWindow(this);
        }
    }

    public void reset() {
        mDrawParams = null;
    }
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mDrawParams != null) {
            mDrawParams.onDetachedFromWindow(this);
        }
        reset();
    }
}
