package com.android.launcher3.folder.large;

import android.content.Context;
import android.os.UserHandle;
import android.util.Log;
import android.view.View;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.HxyOption;
import com.android.launcher3.model.data.FolderInfo;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.views.ActivityContext;
import com.android.launcher3.R;

public class HxyLargeFolderProxy {
    private static final int MAX_3X3_SIZE = 9;
    private static final int SPAN_3X3_COUNT = 3;
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
        return SPAN_3X3_COUNT;
    }

    public static int getMaxSize() {
        return MAX_3X3_SIZE;
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

    private static int getDimension(Context context, int id) {
        return (int) context.getResources().getDimension(id);
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
        if (is4X6Grid(context)) {
            sHorizontalSpace = getDimension(context, R.dimen.hxy_large_folder_horizontal_space_4x6);
            sVerticalSpace = getDimension(context, R.dimen.hxy_large_folder_vertical_space_4x6);
        } else if (is4X5Grid(context)) {
            sHorizontalSpace = getDimension(context, R.dimen.hxy_large_folder_horizontal_space_4x5);
            sVerticalSpace = getDimension(context, R.dimen.hxy_large_folder_vertical_space_4x5);
        } else {
            sHorizontalSpace = getDimension(context, R.dimen.hxy_large_folder_horizontal_space_4x4);
            sVerticalSpace = getDimension(context, R.dimen.hxy_large_folder_vertical_space_4x4);
        }
        sHorizontalSpace = 0;
        // sVerticalSpace = 0;
        int folderIconSizePx = activity.getDeviceProfile().folderIconSizePx;
        int availableSpaceX = cellWidth * 2;
        int availableSpaceY = cellHeight * 2;
        int previewWidth = availableSpaceX - paddingLeft - paddingRight;
        int previewHeight = computePreviewHeight(availableSpaceY, folderIconSizePx, 2);
        int horizontalSpaces = sHorizontalSpace * (getSpanCount() + 1);
        int verticalSpaces = sVerticalSpace * (getSpanCount() + 1);
        int childWidth = (previewWidth - horizontalSpaces - paddingLeft - paddingRight) / getSpanCount();
        int childHeight = (previewHeight - verticalSpaces - paddingTop - paddingBottom) / getSpanCount();
        childWidth = (previewWidth) / getSpanCount();
        childHeight = (previewHeight) / getSpanCount();
        int min = Math.min(childWidth, childHeight);
        sFolderIconSize = min;
        initFolderIconOutSize(context, min);
        sMaxDistanceForFolderCreation = (float) Math.max(previewWidth, previewHeight);
        sFolderPreviewHeight = previewHeight;
        sPreviewOffsetY = previewHeight -  sFolderIconSize * getSpanCount() - sVerticalSpace * (getSpanCount() - 1);
        sPreviewOffsetX = previewWidth - sFolderIconSize* getSpanCount() - sHorizontalSpace * (getSpanCount() - 1);
        Log.d("HxyLargeFolderProxy", "initFolderIconSize previewWidth = " + previewWidth + "; horizontalSpaces = " + horizontalSpaces + "; paddingLeft = " + paddingLeft + "; paddingRight = " + paddingRight
        + "; previewHeight = " + previewHeight + "; verticalSpaces = " + verticalSpaces + "; paddingTop = " + paddingTop + "; paddingBottom = " + paddingBottom);
        
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
        return availableSpaceX - (((availableSpaceX / scale) - folderIconSizePx) / 2);
    }

    public static int computePreviewHeight(View view, int availableSpaceY, int folderIconSizePx) {
        int scale = 2;
        if (view != null && (view.getTag() instanceof ItemInfo)) {
            scale = ((ItemInfo) view.getTag()).spanY;
        }
        return computePreviewHeight(availableSpaceY, folderIconSizePx, scale);
    }

    private static int computePreviewHeight(int availableSpaceY, int folderIconSizePx, int scale) {
        return (availableSpaceY / scale) + folderIconSizePx;
    }

    public static int getPreviewOffsetX(int availableSpaceX, int previewWidth) {
        return (availableSpaceX - previewWidth) / 2;
    }

    public static int getPreviewOffsetY(int topPadding, int folderIconOffsetYPx) {
        return sPreviewOffsetY / 2;
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
