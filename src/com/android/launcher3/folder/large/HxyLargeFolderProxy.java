package com.android.launcher3.folder.large;

import android.content.Context;
import android.os.UserHandle;
import android.view.View;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.HxyOption;
import com.android.launcher3.model.data.FolderInfo;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.views.ActivityContext;
import com.android.launcher3.R;

public class HxyLargeFolderProxy {
    // ColorOS renders a 2 x 2 large-folder preview. The first three slots are
    // directly launchable apps and the last slot becomes a 2 x 2 stack when
    // more than four items are present.
    private static final int MAX_PREVIEW_SLOT_COUNT = 4;
    private static final int PREVIEW_SPAN_COUNT = 2;
    private static final float PREVIEW_WIDTH_FACTOR = 0.80f;
    private static final float PREVIEW_HEIGHT_FACTOR = 0.87f;
    private static final float PREVIEW_ITEM_WIDTH_FACTOR = 0.38f;
    private static final float PREVIEW_ITEM_HEIGHT_FACTOR = 0.42f;
    public static final boolean SUPPORT_LARGE_FOLDER = HxyOption.HXY_LAUNCHER_SUPPORT_LARGE_FOLDER;
    private static int sCellHeight = 0;
    private static int sCellWidth = 0;
    private static int sFolderIconOutSize = 0;
    private static int sFolderIconOutSpace = 0;
    private static int sFolderIconSize = 0;
    private static int sFolderPreviewHeight = 0;
    private static int sFolderRound = 0;
    private static int sHorizontalSpace = 0;
    private static float sMaxDistanceForFolderCreation = 0.0f;
    private static int sVerticalSpace = 0;
    private static int sFolderIconPaddingTop = 0;
    private static  int sPreviewOffsetY = 0;
    private static  int sPreviewOffsetX = 0;

    public static int getSpanCount() {
        return PREVIEW_SPAN_COUNT;
    }

    public static int getMaxSize() {
        return MAX_PREVIEW_SLOT_COUNT;
    }

    public static float getMaxDistanceForFolderCreation() {
        return sMaxDistanceForFolderCreation;
    }

    public static FolderInfo cloneFolderInfo(FolderInfo info) {
        FolderInfo newInfo = new FolderInfo();
        newInfo.user = info.user;
        newInfo.copyFrom(info);
        newInfo.options = info.options;
        newInfo.suggestedFolderNames = info.suggestedFolderNames;
        newInfo.contents = info.contents;
        return newInfo;
    }

    private static InvariantDeviceProfile getInvariantDeviceProfile(Context context) {
        return LauncherAppState.getInstance(context.getApplicationContext()).getInvariantDeviceProfile();
    }

    public static boolean isGrid(Context context, int spanX, int spanY) {
        return isGrid(getInvariantDeviceProfile(context), spanX, spanY);
    }

    private static boolean isGrid(InvariantDeviceProfile profile, int spanX, int spanY) {
        return profile.numColumns == spanX && profile.numRows == spanY;
    }

    public static boolean is4X5Grid(Context context) {
        return is4X5Grid(getInvariantDeviceProfile(context));
    }

    private static boolean is4X5Grid(InvariantDeviceProfile profile) {
        return profile.numColumns == 4 && profile.numRows == 5;
    }

    public static boolean is4X6Grid(Context context) {
        return is4X6Grid(getInvariantDeviceProfile(context));
    }

    private static boolean is4X6Grid(InvariantDeviceProfile profile) {
        return profile.numColumns == 4 && profile.numRows == 6;
    }

    public static void setFolderPaddingTop(int topPadding) {
        if (sFolderIconPaddingTop != topPadding && topPadding != 0) {
            sFolderIconPaddingTop = topPadding;
        }
    }

    public static int getFolderPaddingTop() {
        return sFolderIconPaddingTop;
    }

    public static void initFolderIconSize(Context context, ActivityContext activity, int cellWidth, int cellHeight, int paddingLeft, int paddingRight,int paddingTop, int paddingBottom) {
        if (cellWidth >= 1 && cellHeight >= 1) {
            sCellWidth = cellWidth;
            sCellHeight = cellHeight;
            initFolderIconSize2(context, activity, cellWidth, cellHeight, paddingLeft, paddingRight, paddingTop, paddingBottom);
        }
    }

    private static void initFolderIconSize2(Context context, ActivityContext activity, int cellWidth, int cellHeight, int paddingLeft, int paddingRight,int paddingTop, int paddingBottom) {
        int folderIconSizePx = activity.getDeviceProfile().folderIconSizePx;
        int availableSpaceX = cellWidth * 2;
        int availableSpaceY = cellHeight * 2;
        int previewWidth = computePreviewWidth(availableSpaceX, folderIconSizePx, 2);
        int previewHeight = Math.round(previewWidth * PREVIEW_HEIGHT_FACTOR);
        int min = Math.round(Math.min(previewWidth * PREVIEW_ITEM_WIDTH_FACTOR,
                previewHeight * PREVIEW_ITEM_HEIGHT_FACTOR));
        sHorizontalSpace = Math.max(0,
                (previewWidth - (min * getSpanCount())) / (getSpanCount() + 1));
        sVerticalSpace = Math.max(0,
                (previewHeight - (min * getSpanCount())) / (getSpanCount() + 1));
        sFolderIconSize = min;
        initFolderIconOutSize(context, min);
        sMaxDistanceForFolderCreation = (float) Math.max(previewWidth, previewHeight);
        sFolderPreviewHeight = previewHeight;
        sPreviewOffsetY = sVerticalSpace;
        sPreviewOffsetX = sHorizontalSpace;
        
    }

    private static void initFolderIconOutSize(Context context, int iconSize) {
        sFolderIconOutSize = (iconSize - getFolderIconOutSpace(context)) / 2;
    }

    public static int getFolderRound(Context context) {
        if (sFolderRound == 0) {
            sFolderRound = (int) context.getResources().getDimension(R.dimen.hxy_large_folder_round);
        }
        return sFolderRound;
    }

    public static int getFolderPreviewHeight() {
        return sFolderPreviewHeight;
    }

    public static int getHorizontalSpace() {
        return sHorizontalSpace;
    }

    public static int getVerticalSpace() {
        return sVerticalSpace;
    }

    public static int getFolderIconSize() {
        return sFolderIconSize;
    }

    public static int getFolderIconOutSize() {
        return sFolderIconOutSize;
    }

    public static int getFolderIconOutSpace(Context context) {
        if (sFolderIconOutSpace == 0) {
            sFolderIconOutSpace = (int) context.getResources().getDimension(R.dimen.hxy_large_folder_icon_out_space);
        }
        return sFolderIconOutSpace;
    }

    public static boolean isLargeFolder(ItemInfo info) {
        if (SUPPORT_LARGE_FOLDER && info != null && info.itemType == 2) {
            return isLargeFolder(info.spanX, info.spanY);
        }
        return false;
    }

    public static boolean isLargeFolder(int spanX, int spanY) {
        return spanX == 2 && spanY == 2;
    }

    public static boolean isLargeFolder(View view) {
        if (!SUPPORT_LARGE_FOLDER || !(view instanceof HxyLargeFolderIcon)) {
            return false;
        }
        Object info = view.getTag();
        if (info instanceof ItemInfo) {
            return isLargeFolder((ItemInfo) info);
        }
        return false;
    }

    public static int computePreviewWidth(View view, int availableSpaceX, int folderIconSizePx) {
        int scale = 2;
        if (view != null && (view.getTag() instanceof ItemInfo)) {
            scale = ((ItemInfo) view.getTag()).spanX;
        }
        return computePreviewWidth(availableSpaceX, folderIconSizePx, scale);
    }

    private static int computePreviewWidth(int availableSpaceX, int folderIconSizePx, int scale) {
        return scale > 1 ? Math.round(availableSpaceX * PREVIEW_WIDTH_FACTOR) : folderIconSizePx;
    }

    public static int computePreviewHeight(View view, int availableSpaceY, int folderIconSizePx) {
        int scale = 2;
        if (view != null && (view.getTag() instanceof ItemInfo)) {
            scale = ((ItemInfo) view.getTag()).spanY;
        }
        return computePreviewHeight(availableSpaceY, folderIconSizePx, scale);
    }

    private static int computePreviewHeight(int availableSpaceY, int folderIconSizePx, int scale) {
        if (scale > 1 && sCellWidth > 0) {
            int previewWidth = computePreviewWidth(sCellWidth * scale, folderIconSizePx, scale);
            return Math.min(availableSpaceY, Math.round(previewWidth * PREVIEW_HEIGHT_FACTOR));
        }
        return scale > 1 ? Math.round(availableSpaceY * 0.55f) : folderIconSizePx;
    }

    public static int getPreviewOffsetX(int availableSpaceX, int previewWidth) {
        return (availableSpaceX - previewWidth) / 2;
    }

    public static int getPreviewOffsetY(int topPadding, int folderIconOffsetYPx) {
        return topPadding + folderIconOffsetYPx;
    }

    public static View getFloatingIconView(View originalView, String targetPackageName, UserHandle user) {
        return getFloatingIconView(originalView, targetPackageName, 0);
    }

    public static View getFloatingIconView(View originalView, String targetPackageName, int userId) {
        if (!HxyLargeFolderUtils.isEmpty(targetPackageName) && (originalView instanceof HxyLargeFolderIcon)) {
            return ((HxyLargeFolderIcon) originalView).getFirstMatchForAppClose(targetPackageName, userId);
        }
        return originalView;
    }

    public static int getSwitchLabelResId(Object tag) {
        ItemInfo info = (ItemInfo) tag;
        if (info.spanX > 1 || info.spanY > 1) {
            return R.string.hxy_folder_switch_small;
        }
        return R.string.hxy_folder_switch_large;
    }

    public static int getSwitchIconResId(Object tag) {
        ItemInfo info = (ItemInfo) tag;
        if (info.spanX > 1 || info.spanY > 1) {
            return R.drawable.hxy_folder_switch_small;
        }
        return R.drawable.hxy_folder_switch_large;
    }
}
