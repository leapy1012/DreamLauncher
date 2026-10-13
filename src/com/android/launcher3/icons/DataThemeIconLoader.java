/*
 * Copyright (C) 2026 GreenDream
 *
 * Loads OPPO-style theme icons from /data/theme/icons — ColorOS keeps this as a
 * zip ({@code OplusThirdPartUtil} {@code ZipFile(/data/theme/icons)} +
 * {@code res/drawable-xxhdpi/…}). Also supports an expanded directory for older
 * Dream installs that already unpacked the pack.
 */
package com.android.launcher3.icons;

import android.content.ComponentName;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.Nullable;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Maps package / component keys to PNG files under {@code /data/theme/icons}.
 *
 * <p>Unmapped apps (true third-party / OEM packages without a theme PNG) are
 * composited via {@link OplusStyleIconConverter} — the same launcher-pack path
 * ColorOS uses ({@code iphone_style_*} / {@code new_iphone_style_*} +
 * {@code themeInfo.xml}), not {@code zzzz}/color guesses.
 */
public final class DataThemeIconLoader {
    private static final String TAG = "DataThemeIconLoader";
    public static final String THEME_ROOT = "/data/theme";
    public static final String ICONS_DIR = THEME_ROOT + "/icons";
    public static final String SETTINGS_DATA_THEME = "dream_data_theme";

    /** Same drawable search order as {@code OplusThirdPartUtil} for xxhdpi devices. */
    private static final String[] DIRS_DRAWABLE_XXH = {
            "res/drawable-xxhdpi/",
            "res/drawable-xhdpi/",
            "res/drawable-hdpi/",
            "res/drawable/",
    };

    /** Theme PNGs below this opaque ratio are treated as doodle glyphs needing a plate. */
    private static final float TRANSPARENT_THEME_ICON_OPAQUE_RATIO = 0.35f;

    /** Common OPPO drawable stem → AOSP / Dream package names. */
    private static final Map<String, String[]> STEM_TO_PACKAGES = new ArrayMap<>();
    /**
     * DreamDialer hosts Contacts as activities under {@code com.android.dialer}.
     * These must use the contacts PNG, never the dialer package icon.
     */
    private static final String[] DIALER_CONTACTS_COMPONENTS = {
            "com.android.dialer/com.android.contacts.PeopleActivity",
            "com.android.dialer/com.android.contacts.ui.main.ContactMainActivity",
            "com.android.dialer/com.android.contacts.activities.PeopleActivity",
    };

    static {
        putStem("ic_launcher_settings", "com.android.settings");
        // Phone / Dialer only — do not also bind contacts here.
        putStem("ic_launcher_dialer", "com.android.dialer", "com.android.phone");
        // Contacts package + DreamDialer PeopleActivity (see loadThemeDataMap).
        putStem("ic_launcher_oppo_contacts", "com.android.contacts", "com.google.android.contacts");
        putStem("ic_launcher_camera", "com.mediatek.camera", "com.android.camera2", "com.android.camera");
        putStem("ic_launcher_gallery3d", "com.android.gallery3d", "com.android.gallery");
        putStem("ic_launcher_smsmms", "com.android.mms", "com.android.messaging");
        putStem("ic_launcher_calender", "gd.app.calendar", "com.android.calendar", "com.google.android.calendar");
        putStem("ic_launcher_clock", "com.android.deskclock");
        putStem("ic_launcher_calcultor", "com.android.calculator2", "com.android.calculator", "gd.app.calculator");
        putStem("ic_launcher_music", "gd.app.musicplayer", "com.android.music");
        putStem("ic_launcher_video", "gd.app.video");
        putStem("ic_launcher_recorder", "gd.app.soundrecorder", "com.android.soundrecorder");
        // HxyFileBrowser on D960 is com.android.filebrowser (not MTK filemanager).
        putStem("ic_launcher_filemanager",
                "com.android.filebrowser",
                "com.mediatek.filemanager",
                "com.android.documentsui",
                "com.coloros.filemanager",
                "com.oppo.filemanager");
        // OPPO Safe / Phone Manager → D960 System Manager.
        putStem("ic_launcher_securesafe",
                "com.yft.systemmanager",
                "com.coloros.phonemanager",
                "com.coloros.safecenter",
                "com.oppo.safe",
                "com.color.safecenter");
        putStem("ic_launcher_browser", "com.android.browser", "com.android.chrome");
        putStem("ic_launcher_email", "com.android.email");
        putStem("ic_launcher_themespace", "gd.app.themestore");
        putStem("ic_launcher_weather", "com.android.weather");
        // Notes: OPPO NearMe note drawable → Dream Notes.
        putStem("ic_launcher_nearme_note",
                "gd.app.note",
                "com.nearme.note",
                "com.coloros.note",
                "com.oneplus.note");
        putStem("ic_launcher_notes", "gd.app.note", "com.mediatek.notebook");
        // Game mode: OPPO Game Space → D960 Game mode.
        putStem("ic_launcher_gamespace",
                "com.yft.gamemode",
                "com.coloros.gamespace",
                "com.coloros.gamespaceui");
        putStem("users", "com.android.contacts");
        putStem("gear", "com.android.settings");
        putStem("home", "com.android.launcher3");
        putStem("com_oneplus_note", "gd.app.note", "com.oneplus.note");
    }

    private static void putStem(String stem, String... packages) {
        STEM_TO_PACKAGES.put(stem, packages);
    }

    private DataThemeIconLoader() {}

    public static boolean hasDataThemeIcons() {
        File icons = new File(ICONS_DIR);
        if (isZipFile(icons)) {
            return zipHasIconPngs(icons);
        }
        File dir = resolveIconsDir();
        if (dir == null) return false;
        File[] files = dir.listFiles((d, name) -> name != null && isImageName(name));
        return files != null && files.length > 0;
    }

    /**
     * Expanded-directory drawable folder, or {@code null} when icons is a zip / missing.
     * Prefer {@link #loadThemeDataMap()} which handles both shapes.
     */
    @Nullable
    public static File resolveIconsDir() {
        File dir = new File(ICONS_DIR);
        if (dir.isDirectory()) {
            File nested = new File(dir, "res/drawable-xxhdpi");
            if (nested.isDirectory()) return nested;
            File hdpi = new File(dir, "res/drawable-hdpi");
            if (hdpi.isDirectory()) return hdpi;
            File drawable = new File(dir, "res/drawable");
            if (drawable.isDirectory()) return drawable;
            return dir;
        }
        return null;
    }

    /**
     * Builds package → ThemeData map from PNG files. Keys are package names
     * (and package/class when known).
     */
    public static Map<String, IconProvider.ThemeData> loadThemeDataMap() {
        File icons = new File(ICONS_DIR);
        ArrayMap<String, IconProvider.ThemeData> map = new ArrayMap<>();
        int added;
        if (isZipFile(icons)) {
            added = loadFromZip(icons, map);
            Log.i(TAG, "loaded " + added + " data-theme icon keys from zip " + icons.getAbsolutePath());
        } else {
            File dir = resolveIconsDir();
            if (dir == null) {
                Log.w(TAG, "no icons pack under " + ICONS_DIR);
                return map;
            }
            added = loadFromDirectory(dir, map);
            Log.i(TAG, "loaded " + added + " data-theme icon keys from " + dir.getAbsolutePath());
        }
        clearPlateCache();
        return map;
    }

    private static int loadFromZip(File zipFile, Map<String, IconProvider.ThemeData> map) {
        // stem → (dpiRank, ThemeData); lower dpiRank wins (xxhdpi=0).
        ArrayMap<String, Integer> stemRank = new ArrayMap<>();
        ArrayMap<String, IconProvider.ThemeData> stemIcons = new ArrayMap<>();
        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                ZipEntry entry = en.nextElement();
                if (entry.isDirectory()) continue;
                String path = entry.getName();
                if (path == null) continue;
                String lower = path.toLowerCase(Locale.US);
                if (!isImageName(lower)) continue;
                int slash = path.lastIndexOf('/');
                String fileName = slash >= 0 ? path.substring(slash + 1) : path;
                String stem = stemOf(fileName);
                if (stem == null) continue;
                int rank = drawableDirRank(path);
                Integer prev = stemRank.get(stem);
                if (prev != null && prev <= rank) {
                    continue;
                }
                Drawable d = loadDrawable(zip.getInputStream(entry));
                if (d == null) continue;
                stemRank.put(stem, rank);
                stemIcons.put(stem, new IconProvider.ThemeData(d));
            }
            int added = bindStemIcons(stemIcons, map);
            added += applyAllAppsXmlMappingsFromZip(zip, map);
            return added;
        } catch (Exception e) {
            Log.w(TAG, "loadFromZip failed: " + e.getMessage());
            return 0;
        }
    }

    private static int loadFromDirectory(File dir, Map<String, IconProvider.ThemeData> map) {
        File[] files = dir.listFiles();
        if (files == null) return 0;
        ArrayMap<String, IconProvider.ThemeData> stemIcons = new ArrayMap<>();
        for (File file : files) {
            if (!file.isFile()) continue;
            String name = file.getName();
            if (!isImageName(name)) continue;
            String stem = stemOf(name);
            if (stem == null) continue;
            Drawable d = loadDrawable(file);
            if (d == null) continue;
            stemIcons.put(stem, new IconProvider.ThemeData(d));
        }
        int added = bindStemIcons(stemIcons, map);
        added += applyAllAppsXmlMappingsFromFile(resolveAllAppsXmlFile(dir), map);
        return added;
    }

    private static int bindStemIcons(
            Map<String, IconProvider.ThemeData> stemIcons,
            Map<String, IconProvider.ThemeData> map) {
        int added = 0;
        for (Map.Entry<String, IconProvider.ThemeData> e : stemIcons.entrySet()) {
            String stem = e.getKey();
            IconProvider.ThemeData td = e.getValue();
            if (stem.contains(".")) {
                // Filename is a package name, e.g. com.android.chrome.png
                map.put(stem, td);
                added++;
                continue;
            }
            // zzzz / AAA_* are third-party plate templates (not package icons).
            if (isPlateStem(stem) || "share".equalsIgnoreCase(stem)
                    || "pay".equalsIgnoreCase(stem) || "tv".equalsIgnoreCase(stem)) {
                continue;
            }

            String[] packages = STEM_TO_PACKAGES.get(stem);
            if (packages != null) {
                for (String pkg : packages) {
                    if (!map.containsKey(pkg)) {
                        map.put(pkg, td);
                        added++;
                    }
                }
                // Contacts PNG must also key DreamDialer PeopleActivity components.
                if ("ic_launcher_oppo_contacts".equals(stem)) {
                    for (String comp : DIALER_CONTACTS_COMPONENTS) {
                        map.put(comp, td);
                        added++;
                    }
                }
            } else if (stem.startsWith("ic_launcher_")) {
                // Best-effort: keep stem as debug key only; also try underscore→dot.
                String guess = stem.substring("ic_launcher_".length()).replace('_', '.');
                if (guess.contains(".")) {
                    map.put(guess, td);
                    added++;
                }
            } else if (stem.startsWith("com_") || stem.startsWith("net_") || stem.startsWith("cn_")) {
                String pkg = stem.replace('_', '.');
                map.put(pkg, td);
                added++;
            }
            // Keep stem→ThemeData so allApps.xml can resolve by icon file name.
            if (!map.containsKey(stem)) {
                map.put(stem, td);
            }
        }
        return added;
    }

    /**
     * OPPO {@code allApps.xml}: {@code <icon name="ic_xxx.png" package="com.foo"/>}.
     * Binds theme PNG stems to packages we do not have in {@link #STEM_TO_PACKAGES}.
     */
    private static int applyAllAppsXmlMappingsFromZip(
            ZipFile zip, Map<String, IconProvider.ThemeData> map) {
        ZipEntry entry = zip.getEntry("allApps.xml");
        if (entry == null) {
            // Some packs nest under icons/ prefix (shouldn't after nested extract).
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                String n = e.getName();
                if (n != null && n.endsWith("allApps.xml")) {
                    entry = e;
                    break;
                }
            }
        }
        if (entry == null) return 0;
        try (InputStream in = zip.getInputStream(entry)) {
            return parseAllAppsXml(in, map, "zip:" + entry.getName());
        } catch (Exception e) {
            Log.w(TAG, "allApps.xml zip parse failed: " + e.getMessage());
            return 0;
        }
    }

    private static int applyAllAppsXmlMappingsFromFile(
            @Nullable File xml, Map<String, IconProvider.ThemeData> map) {
        if (xml == null || !xml.isFile()) {
            return 0;
        }
        try (InputStream in = new FileInputStream(xml)) {
            return parseAllAppsXml(in, map, xml.getAbsolutePath());
        } catch (Exception e) {
            Log.w(TAG, "allApps.xml parse failed: " + e.getMessage());
            return 0;
        }
    }

    @Nullable
    private static File resolveAllAppsXmlFile(@Nullable File iconsDir) {
        File xml = new File(ICONS_DIR, "allApps.xml");
        if (xml.isFile()) return xml;
        if (iconsDir != null) {
            File res = iconsDir.getParentFile();
            File iconsRoot = res != null ? res.getParentFile() : null;
            if (iconsRoot != null) {
                File nested = new File(iconsRoot, "allApps.xml");
                if (nested.isFile()) return nested;
            }
            File beside = new File(iconsDir, "allApps.xml");
            if (beside.isFile()) return beside;
        }
        return null;
    }

    private static int parseAllAppsXml(
            InputStream in, Map<String, IconProvider.ThemeData> map, String source) throws Exception {
        int added = 0;
        XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
        parser.setInput(in, "UTF-8");
        int type;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
            if (type != XmlPullParser.START_TAG || !"icon".equals(parser.getName())) {
                continue;
            }
            String name = parser.getAttributeValue(null, "name");
            String pkg = parser.getAttributeValue(null, "package");
            if (name == null || pkg == null || pkg.isEmpty()) {
                continue;
            }
            String stem = stemOf(name);
            if (stem == null) stem = name;
            IconProvider.ThemeData td = map.get(stem);
            if (td == null) {
                td = map.get(name);
            }
            if (td == null) {
                continue;
            }
            if (!map.containsKey(pkg)) {
                map.put(pkg, td);
                added++;
            }
        }
        Log.i(TAG, "allApps.xml mapped +" + added + " packages from " + source);
        return added;
    }

    /** Drop cached OPPO launcher-pack style so the next wrap reloads from disk. */
    public static void clearPlateCache() {
        OplusStyleIconConverter.clearCache();
    }

    private static boolean isPlateStem(String stem) {
        if (stem == null) return false;
        String s = stem.toLowerCase(Locale.US);
        return "zzzz".equals(s) || s.startsWith("aaa_");
    }

    /**
     * Whether ColorOS-style third-party wrapping is available for the active theme
     * (launcher pack has COVER/MASK assets).
     */
    public static boolean hasThirdPartyStyle() {
        return OplusStyleIconConverter.hasThirdPartyStyle();
    }

    /**
     * ColorOS third-party wrap via {@link OplusStyleIconConverter}
     * ({@code OplusConvertIcon} COVER/MASK path).
     */
    @Nullable
    public static Drawable composeThirdPartyIcon(@Nullable Drawable original) {
        if (original == null) return null;
        return OplusStyleIconConverter.wrapThirdPartyIcon(original);
    }

    /**
     * True when a theme PNG is mostly transparent (OPPO doodle / line-art glyph)
     * and needs the shared plate underneath — same look as third-party wrap.
     */
    public static boolean isSparseThemeGlyph(@Nullable Drawable icon) {
        if (!(icon instanceof BitmapDrawable)) {
            return false;
        }
        Bitmap bmp = ((BitmapDrawable) icon).getBitmap();
        if (bmp == null || bmp.getWidth() < 4 || bmp.getHeight() < 4) {
            return false;
        }
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int opaque = 0;
        int sampled = 0;
        for (int y = 0; y < h; y += 3) {
            for (int x = 0; x < w; x += 3) {
                sampled++;
                if (Color.alpha(bmp.getPixel(x, y)) >= 200) {
                    opaque++;
                }
            }
        }
        return sampled > 0 && (opaque / (float) sampled) < TRANSPARENT_THEME_ICON_OPAQUE_RATIO;
    }

    @Nullable
    public static IconProvider.ThemeData getThemeDataForComponent(
            Map<String, IconProvider.ThemeData> map, ComponentName cn) {
        if (map == null || map.isEmpty() || cn == null) return null;
        IconProvider.ThemeData td = map.get(cn.flattenToString());
        if (td == null) td = map.get(cn.flattenToShortString());
        // DreamDialer Contacts activities share package with Phone — never use dialer PNG.
        if (td == null && IconProvider.isDialerContactsActivity(cn)) {
            td = map.get("com.android.contacts");
            if (td == null) {
                for (String comp : DIALER_CONTACTS_COMPONENTS) {
                    td = map.get(comp);
                    if (td != null) break;
                }
            }
            return td;
        }
        if (td == null) td = map.get(cn.getPackageName());
        return td;
    }

    @Nullable
    private static Drawable loadDrawable(File file) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
            if (bmp == null) return null;
            return new BitmapDrawable(null, bmp);
        } catch (Throwable t) {
            Log.w(TAG, "decode failed " + file.getAbsolutePath() + ": " + t.getMessage());
            return null;
        }
    }

    @Nullable
    private static Drawable loadDrawable(InputStream in) {
        try (InputStream buffered = new BufferedInputStream(in)) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bmp = BitmapFactory.decodeStream(buffered, null, opts);
            if (bmp == null) return null;
            return new BitmapDrawable(null, bmp);
        } catch (Throwable t) {
            Log.w(TAG, "decode stream failed: " + t.getMessage());
            return null;
        }
    }

    private static boolean isZipFile(File file) {
        if (file == null || !file.isFile() || file.length() < 4L) return false;
        try (InputStream in = new FileInputStream(file)) {
            int b0 = in.read();
            int b1 = in.read();
            return b0 == 0x50 && b1 == 0x4B; // PK
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean zipHasIconPngs(File zipFile) {
        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (e.isDirectory()) continue;
                String n = e.getName();
                if (n != null && isImageName(n) && n.toLowerCase(Locale.US).contains("drawable")) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "zipHasIconPngs: " + e.getMessage());
        }
        return false;
    }

    private static boolean isImageName(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase(Locale.US);
        return lower.endsWith(".png") || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg") || lower.endsWith(".webp");
    }

    @Nullable
    private static String stemOf(String fileName) {
        if (fileName == null) return null;
        int slash = fileName.lastIndexOf('/');
        String base = slash >= 0 ? fileName.substring(slash + 1) : fileName;
        int dot = base.lastIndexOf('.');
        if (dot <= 0) return base.isEmpty() ? null : base;
        return base.substring(0, dot);
    }

    /** Lower is better; matches {@code OplusThirdPartUtil} XXH preference. */
    private static int drawableDirRank(String path) {
        String lower = path.toLowerCase(Locale.US).replace('\\', '/');
        for (int i = 0; i < DIRS_DRAWABLE_XXH.length; i++) {
            if (lower.contains(DIRS_DRAWABLE_XXH[i])) return i;
        }
        return DIRS_DRAWABLE_XXH.length;
    }

    public static String themeStateToken() {
        long newest = 0;
        File icons = new File(ICONS_DIR);
        if (icons.exists()) newest = Math.max(newest, icons.lastModified());
        File dir = resolveIconsDir();
        if (dir != null) newest = Math.max(newest, dir.lastModified());
        File applied = new File(THEME_ROOT, "applied.json");
        if (applied.isFile()) newest = Math.max(newest, applied.lastModified());
        File launcher = new File(THEME_ROOT, "com.android.launcher");
        if (!launcher.exists()) launcher = new File(THEME_ROOT, "com.oppo.launcher");
        if (launcher.exists()) newest = Math.max(newest, launcher.lastModified());
        if (newest == 0 && !icons.exists()) return "none";
        return "data-theme-" + newest + "-" + OplusStyleIconConverter.stateToken();
    }
}
