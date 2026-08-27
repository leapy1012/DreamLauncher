package com.android.launcher3.customer.table;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import com.android.launcher3.icons.FastBitmapDrawable;
import com.android.launcher3.icons.GraphicsUtils;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.customer.tools.ImageUtils;
import java.io.Serializable;
import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

@Table(name = "t_custom_table")
public class CustomTable implements ITable, Serializable {
    public static final String table_name = "t_custom_table";
    public ComponentName component;
    @Column(isId = true, name = "componentName")
    public String componentNameStr;
    @Column(name = "custom_title")
    public String custom_title;
    public Drawable icon;
    /** Unmasked square icon bitmap for live shape preview. */
    public Bitmap iconBitmap;
    @Column(name = "icon_path")
    public boolean icon_path;
    @Column(name = "iconbyte")
    public byte[] iconbyte;
    public int mUserId;
    @Column(name = "replaceComponentName")
    public String replaceComponentNameStr;
    @Column(name = "title")
    public String title;

    public CustomTable toInstance(AppInfo info, Context context) {
        this.componentNameStr = info.componentName.flattenToShortString();
        this.component = info.componentName;
        this.icon = FastBitmapDrawable.newIcon(context, info.bitmap);
        this.iconBitmap = createUnmaskedIconBitmap(context, info);
        this.title = info.title.toString();
        this.mUserId = 0;// todo
        return this;
    }

    /**
     * Build a full-bleed square bitmap (no system shape mask) so Icon Custom can
     * apply preview masks. Falls back to the cached launcher bitmap.
     */
    private static Bitmap createUnmaskedIconBitmap(Context context, AppInfo info) {
        int size = Math.max(1, info.bitmap.icon != null ? info.bitmap.icon.getWidth() : 128);
        try {
            Drawable activityIcon = context.getPackageManager()
                    .getActivityIcon(info.componentName)
                    .mutate();
            if (activityIcon instanceof AdaptiveIconDrawable) {
                AdaptiveIconDrawable adaptive = (AdaptiveIconDrawable) activityIcon;
                Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(out);
                int inset = Math.round(size * AdaptiveIconDrawable.getExtraInsetFraction()
                        / (1 + 2 * AdaptiveIconDrawable.getExtraInsetFraction()));
                Drawable bg = adaptive.getBackground();
                Drawable fg = adaptive.getForeground();
                if (bg != null) {
                    bg = bg.mutate();
                    bg.setBounds(-inset, -inset, size + inset, size + inset);
                    bg.draw(canvas);
                }
                if (fg != null) {
                    fg = fg.mutate();
                    fg.setBounds(-inset, -inset, size + inset, size + inset);
                    fg.draw(canvas);
                }
                return out;
            }
            if (activityIcon != null) {
                // Non-adaptive: draw into a full square so DST_IN masks can reshape it.
                Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(out);
                activityIcon.setBounds(0, 0, size, size);
                activityIcon.draw(canvas);
                return out;
            }
        } catch (PackageManager.NameNotFoundException | RuntimeException ignored) {
        }
        Bitmap bitmap = info.bitmap.icon;
        if (bitmap != null && bitmap.getConfig() == Bitmap.Config.HARDWARE) {
            return bitmap.copy(Bitmap.Config.ARGB_8888, true);
        }
        return bitmap;
    }

    public CustomTable clone() {
        CustomTable result = new CustomTable();
        result.title = this.title;
        result.icon = this.icon;
        result.iconBitmap = this.iconBitmap;
        result.custom_title = this.custom_title;
        result.iconbyte = this.iconbyte;
        result.replaceComponentNameStr = this.replaceComponentNameStr;
        result.componentNameStr = this.componentNameStr;
        result.component = this.component;
        result.mUserId = this.mUserId;
        return result;
    }

    public String getTableName() {
        return table_name;
    }

    public Bitmap toBitmap(byte[] data) {
        return BitmapFactory.decodeByteArray(data, 0, data.length, (BitmapFactory.Options) null);
    }

    public Drawable toDrawble(byte[] data) {
        return ImageUtils.bitmapToDrawable(BitmapFactory.decodeByteArray(data, 0, data.length, (BitmapFactory.Options) null));
    }

    public static byte[] toByte(Bitmap map) {
        return GraphicsUtils.flattenBitmap(map);
    }
}
