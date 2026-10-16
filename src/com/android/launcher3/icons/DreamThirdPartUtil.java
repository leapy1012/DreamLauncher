package com.android.launcher3.icons;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Reads drawables from the applied theme's launcher pack under {@code /data/theme/}
 * (zip from the theme store, or an expanded directory from ThemeApplyEngine).
 */
public final class DreamThirdPartUtil {
    private static final String TAG = "DreamThirdPartUtil";

    static final String THEME_ROOT = "/data/theme/";
    private static final String LAUNCHER_ANDROID = "com.android.launcher";
    private static final String LAUNCHER_OPPO = "com.oppo.launcher";

    private static final String[] DIRS_DRAWABLE_XXH = {
            "res/drawable-xxhdpi/",
            "res/drawable-xhdpi/",
            "res/drawable-hdpi/",
    };

    private static final Object LOCK = new Object();
    private static final Map<String, Drawable.ConstantState> sDrawables = new HashMap<>();
    private static String sCachePackKey;

    private DreamThirdPartUtil() {}

    /** Theme launcher pack; prefers {@code com.android.launcher} over {@code com.oppo.launcher}. */
    @Nullable
    public static File getLauncherName() {
        File android = new File(THEME_ROOT + LAUNCHER_ANDROID);
        if (android.exists()) return android;
        File oppo = new File(THEME_ROOT + LAUNCHER_OPPO);
        if (oppo.exists()) return oppo;
        return null;
    }

    /**
     * Drawable {@code name} (e.g. {@code big_folder_bg.png}) from the applied launcher pack,
     * or null when no theme is applied or the theme doesn't ship it.
     */
    @Nullable
    public static Drawable getLauncherDrawableByName(@NonNull Resources res,
            @NonNull String name) {
        File pack = getLauncherName();
        if (pack == null) return null;
        String packKey = pack.getAbsolutePath() + ":" + pack.lastModified() + ":" + pack.length();
        synchronized (LOCK) {
            if (!packKey.equals(sCachePackKey)) {
                sDrawables.clear();
                sCachePackKey = packKey;
            }
            if (sDrawables.containsKey(name)) {
                Drawable.ConstantState state = sDrawables.get(name);
                return state == null ? null : state.newDrawable(res);
            }
            Bitmap bitmap = decodeAsset(pack, name);
            if (bitmap == null) {
                sDrawables.put(name, null);
                return null;
            }
            BitmapDrawable drawable = new BitmapDrawable(res, bitmap);
            sDrawables.put(name, drawable.getConstantState());
            return drawable;
        }
    }

    public static void clearCache() {
        synchronized (LOCK) {
            sDrawables.clear();
            sCachePackKey = null;
        }
    }

    static boolean assetExists(@NonNull File pack, @NonNull String name) {
        AssetStream as = openAsset(pack, name);
        if (as == null) return false;
        as.close();
        return true;
    }

    @Nullable
    static Bitmap decodeAsset(@NonNull File pack, @NonNull String name) {
        AssetStream as = openAsset(pack, name);
        if (as == null) return null;
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeStream(as.stream, null, opts);
        } finally {
            as.close();
        }
    }

    /** Opens {@code name} from the first matching drawable density folder of the pack. */
    @Nullable
    private static AssetStream openAsset(@NonNull File pack, @NonNull String name) {
        if (pack.isDirectory()) {
            for (String dir : DIRS_DRAWABLE_XXH) {
                File f = new File(pack, dir + name);
                if (f.isFile()) {
                    try {
                        return new AssetStream(new BufferedInputStream(new FileInputStream(f)),
                                null);
                    } catch (Exception e) {
                        return null;
                    }
                }
            }
            return null;
        }
        if (!pack.isFile()) return null;
        try {
            ZipFile zip = new ZipFile(pack);
            for (String dir : DIRS_DRAWABLE_XXH) {
                ZipEntry entry = zip.getEntry(dir + name);
                if (entry != null) {
                    return new AssetStream(zip.getInputStream(entry), zip);
                }
            }
            // Some packs use different path casing / nesting
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                String n = e.getName();
                if (n != null && (n.endsWith("/" + name) || name.equals(n))) {
                    return new AssetStream(zip.getInputStream(e), zip);
                }
            }
            zip.close();
        } catch (Exception e) {
            Log.w(TAG, "openAsset zip failed " + name + ": " + e.getMessage());
        }
        return null;
    }

    private static final class AssetStream {
        final InputStream stream;
        final ZipFile zipToClose;

        AssetStream(InputStream stream, ZipFile zipToClose) {
            this.stream = stream;
            this.zipToClose = zipToClose;
        }

        void close() {
            try {
                stream.close();
            } catch (Exception ignored) {
            }
            if (zipToClose != null) {
                try {
                    zipToClose.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
