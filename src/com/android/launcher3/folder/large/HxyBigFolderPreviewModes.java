package com.android.launcher3.folder.large;

import com.android.launcher3.model.ModelWriter;
import com.android.launcher3.model.data.FolderInfo;
import com.android.launcher3.model.data.ItemInfo;

/**
 * ColorOS big-folder preview layout modes ({@code FolderInfo} options 16 / 32).
 * Default (neither bit) is the 3×3 nine-grid — avoids clashing with
 * {@link FolderInfo#FLAG_MANUAL_FOLDER_NAME} (bit 8).
 */
public final class HxyBigFolderPreviewModes {
    /** Oppo {@code BIG_FOLDER_TYPE_2_2}. */
    public static final int TYPE_FOUR = 16;
    /** Oppo {@code BIG_FOLDER_TYPE_HIGHLIGHT}. */
    public static final int TYPE_HIGHLIGHT = 32;

    public static final int INDEX_NINE = 0;
    public static final int INDEX_FOUR = 1;
    public static final int INDEX_HIGHLIGHT = 2;

    private HxyBigFolderPreviewModes() {
    }

    public static int getModeIndex(ItemInfo info) {
        if (!(info instanceof FolderInfo)) {
            return INDEX_NINE;
        }
        FolderInfo folder = (FolderInfo) info;
        if (folder.hasOption(TYPE_HIGHLIGHT)) {
            return INDEX_HIGHLIGHT;
        }
        if (folder.hasOption(TYPE_FOUR)) {
            return INDEX_FOUR;
        }
        return INDEX_NINE;
    }

    public static void applyMode(FolderInfo folder, int modeIndex, ModelWriter writer) {
        if (folder == null) {
            return;
        }
        boolean four = modeIndex == INDEX_FOUR;
        boolean highlight = modeIndex == INDEX_HIGHLIGHT;
        folder.setOption(TYPE_FOUR, four, writer);
        folder.setOption(TYPE_HIGHLIGHT, highlight, writer);
    }

    public static int getPreviewSpan(ItemInfo info) {
        int mode = getModeIndex(info);
        if (mode == INDEX_FOUR) {
            return 2;
        }
        return 3;
    }

    public static int getPreviewMaxSize(ItemInfo info) {
        return getPreviewMaxSize(info, 0);
    }

    /**
     * Highlight mode uses a 6-slot layout only on page 0; later pages fall back to
     * the normal 3×3 nine-grid capacity (ColorOS forbids highlight rule off page 0).
     */
    public static int getPreviewMaxSize(ItemInfo info, int page) {
        int mode = getModeIndex(info);
        if (mode == INDEX_FOUR) {
            return 4;
        }
        if (mode == INDEX_HIGHLIGHT && page <= 0) {
            return 6;
        }
        return HxyLargeFolderProxy.getMaxSize();
    }

    public static boolean isHighlightPage(ItemInfo info, int page) {
        return getModeIndex(info) == INDEX_HIGHLIGHT && page <= 0;
    }

    public static boolean isStackOverflow(ItemInfo info, int position, int contentSize) {
        return isStackOverflow(info, position, contentSize, 0);
    }

    public static boolean isStackOverflow(ItemInfo info, int position, int contentSize, int page) {
        int max = getPreviewMaxSize(info, page);
        int withoutStacked = max - 1;
        if (position != withoutStacked) {
            return false;
        }
        return contentSize > max && (contentSize - withoutStacked) > 1;
    }

    /** Full icons per page before the overflow stack cell (nine → 8, four → 3, highlight p0 → 5). */
    public static int getMaxPreviewWithoutStacked(ItemInfo info) {
        return getMaxPreviewWithoutStacked(info, 0);
    }

    public static int getMaxPreviewWithoutStacked(ItemInfo info, int page) {
        return Math.max(1, getPreviewMaxSize(info, page) - 1);
    }

    /**
     * Oppo {@code FlexibleFolderIcon.getPreviewPageCount}: pages of
     * {@code withoutStacked} items, collapsing a trailing orphan when
     * {@code size % withoutStacked == 1}.
     */
    public static int getPreviewPageCount(ItemInfo info, int contentSize) {
        int without = getMaxPreviewWithoutStacked(info, 0);
        if (contentSize <= 0) {
            return 1;
        }
        int ceil = (int) Math.ceil(contentSize / (float) without);
        if (ceil > 1 && contentSize % without == 1) {
            ceil--;
        }
        return Math.max(1, ceil);
    }

    public static int getPageStartIndex(ItemInfo info, int page) {
        // Page 0 highlight uses 5/page; subsequent pages also advance by that stride.
        return getMaxPreviewWithoutStacked(info, 0) * Math.max(0, page);
    }
}
