package com.android.launcher3.folder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;

import androidx.annotation.Nullable;

import com.android.launcher3.folder.large.LargeFolderProxy;
import com.android.launcher3.icons.DreamThirdPartUtil;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.views.ActivityContext;

/**
 * Folder plate that uses the applied theme's folder image when the launcher pack ships one,
 * and the default {@code folderPreviewColor} plate otherwise.
 */
public class DreamPreviewBackground extends PreviewBackground {
    public static final String FOLDER_DRAWABLE_NAME = "ic_launcher_folder.png";
    public static final String FOLDER_DRAWABLE_NAME_1X2 = "folder_one_multiply_two_bg.png";
    public static final String FOLDER_DRAWABLE_NAME_2X1 = "folder_two_multiply_one_bg.png";
    public static final String FOLDER_DRAWABLE_NAME_2X2 = "big_folder_bg.png";

    private final Path mClipPath = new Path();
    @Nullable
    private Drawable mBgDrawable;
    @Nullable
    private String mBgDrawableName;

    public DreamPreviewBackground(Context context) {
        super(context);
    }

    @Override
    public void setup(Context context, ActivityContext activity, View invalidateDelegate,
            int availableSpaceX, int availableSpaceY, int topPadding) {
        super.setup(context, activity, invalidateDelegate, availableSpaceX, availableSpaceY,
                topPadding);
        initBgDrawable(context);
    }

    /** Reloads the plate only when the folder size changes; setup runs on every measure. */
    public void initBgDrawable(Context context) {
        String name = getFolderBgName();
        if (name.equals(mBgDrawableName)) {
            return;
        }
        mBgDrawableName = name;
        mBgDrawable = getNormalDrawable(context);
    }

    @Nullable
    @Override
    public Drawable getBgDrawable() {
        return mBgDrawable;
    }

    @Nullable
    @Override
    public Drawable getNormalDrawable(Context context) {
        Drawable themeDrawable = DreamThirdPartUtil.getLauncherDrawableByName(
                context.getResources(), getFolderBgName());
        if (themeDrawable == null) {
            return null;
        }
        LayerNormalDrawable drawable =
                new LayerNormalDrawable(getDefaultBgDrawable(), themeDrawable);
        drawable.setRadius(LargeFolderProxy.getFolderRound(context));
        return drawable;
    }

    private Drawable getDefaultBgDrawable() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(getBgColor());
        return drawable;
    }

    private boolean isBigGroup() {
        return LargeFolderProxy.isLargeFolder(getInvalidateDelegate());
    }

    private String getFolderBgName() {
        if (!isBigGroup()) {
            return FOLDER_DRAWABLE_NAME;
        }
        ItemInfo info = (ItemInfo) getInvalidateDelegate().getTag();
        if (info.spanX == 1 && info.spanY == 2) {
            return FOLDER_DRAWABLE_NAME_1X2;
        }
        if (info.spanX == 2 && info.spanY == 1) {
            return FOLDER_DRAWABLE_NAME_2X1;
        }
        return FOLDER_DRAWABLE_NAME_2X2;
    }

    @Override
    public void drawBackground(Canvas canvas) {
        if (mBgDrawable == null) {
            super.drawBackground(canvas);
            return;
        }
        int left = getOffsetX();
        int top = getOffsetY();
        int size = getScaledRadius() * 2;
        mBgDrawable.setBounds(left, top, left + size, top + size);
        mBgDrawable.draw(canvas);
        drawShadow(canvas);
    }

    @Override
    protected void drawPreviewBackground(Canvas canvas, Context context, RectF rectF) {
        if (mBgDrawable == null) {
            super.drawPreviewBackground(canvas, context, rectF);
            return;
        }
        int round = LargeFolderProxy.getFolderRound(context);
        mClipPath.reset();
        mClipPath.addRoundRect(rectF, round, round, Path.Direction.CW);
        int saveCount = canvas.save();
        canvas.clipPath(mClipPath);
        mBgDrawable.setBounds(Math.round(rectF.left), Math.round(rectF.top),
                Math.round(rectF.right), Math.round(rectF.bottom));
        mBgDrawable.draw(canvas);
        canvas.restoreToCount(saveCount);
    }
}
