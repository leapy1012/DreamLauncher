package com.android.launcher3.folder.large;

import android.content.Context;
import android.os.UserHandle;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.model.BgDataModel;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.android.launcher3.util.IntSparseArrayMap;
import java.util.ArrayList;
import java.util.List;

public class HxyLargeFolderUtils {
    public static boolean isInvalidBitmap(WorkspaceItemInfo info) {
        return info.usingLowResIcon();
    }

    public static boolean loadValidBitmapInfo(Context context, ArrayList<WorkspaceItemInfo> contents) {
        List<AppInfo> list = getAllApps(context);
        IntSparseArrayMap<ItemInfo> itemsIdMap = getAllItems(context);
        int failedCount = 0;
        for (int i = 0; i < contents.size(); i++) {
            WorkspaceItemInfo item = contents.get(i);
            if (item.usingLowResIcon()) {
                AppInfo info = getAppInfo(list, item);
                if (info == null || info.usingLowResIcon()) {
                    ItemInfo tempItem = getItem(itemsIdMap, (ItemInfo) item);
                    if (tempItem instanceof WorkspaceItemInfo) {
                        WorkspaceItemInfo temWorkspaceItem = (WorkspaceItemInfo) tempItem;
                        if (!temWorkspaceItem.usingLowResIcon()) {
                            item.bitmap = temWorkspaceItem.bitmap;
                        } else {
                            failedCount++;
                        }
                    } else {
                        failedCount++;
                    }
                } else {
                    item.bitmap = info.bitmap;
                }
            }
        }
        return failedCount < 1;
    }

    public static BgDataModel getBgDataModel(Context context) {
        return LauncherAppState.getInstance(context).getModel().mBgDataModel;
    }

    public static ArrayList<ItemInfo> getWorkspaceItems(Context context) {
        BgDataModel model = getBgDataModel(context);
        if (model != null) {
            return model.workspaceItems;
        }
        return null;
    }

    public static IntSparseArrayMap<ItemInfo> getAllItems(Context context) {
        BgDataModel model = getBgDataModel(context);
        if (model != null) {
            return model.itemsIdMap;
        }
        return null;
    }

    public static ItemInfo getItem(Context context, ItemInfo info) {
        return getItem(getAllItems(context), info);
    }

    public static ItemInfo getItem(IntSparseArrayMap<ItemInfo> list, ItemInfo info) {
        if (list == null || !list.containsKey(info.id)) {
            return null;
        }
        return (ItemInfo) list.get(info.id);
    }

    public static LauncherModel getLauncherModel(Context context) {
        return LauncherAppState.getInstance(context).getModel();
    }

    public static List<AppInfo> getAllApps(Context context) {
        LauncherModel model = getLauncherModel(context);
        if (model != null) {
            return model.mBgAllAppsList.data;
        }
        return null;
    }

    public static boolean equals(ItemInfo info, String packageName, String className, int userId) {
        // todo info.user.getIdentifier() == userId
        return info != null && info.getTargetComponent() != null && equals(info.getTargetComponent().getPackageName(), packageName) && equals(info.getTargetComponent().getClassName(), className);
    }

    public static boolean equals(ItemInfo info, String packageName, int userId) {
        // todo info.user.getIdentifier() == userId
        return info != null && info.getTargetComponent() != null && equals(info.getTargetComponent().getPackageName(), packageName);
    }

    public static boolean equals(ItemInfo info, String packageName, UserHandle user) {
        // todo
        return equals(info, packageName, 0);
    }

    public static String getClassName(WorkspaceItemInfo data) {
        if (data == null || data.getIntent() == null || data.getIntent().getComponent() == null) {
            return null;
        }
        return data.getIntent().getComponent().getClassName();
    }

    public static String getClassName(AppInfo data) {
        if (data == null || data.getIntent() == null || data.getIntent().getComponent() == null) {
            return null;
        }
        return data.getIntent().getComponent().getClassName();
    }

    public static AppInfo getAppInfo(List<AppInfo> list, WorkspaceItemInfo item) {
        String from;
        if (list == null || list.size() < 1 || (from = getClassName(item)) == null || from.length() < 1) {
            return null;
        }
        for (int i = 0; i < list.size(); i++) {
            AppInfo info = list.get(i);
            if (equals(from, getClassName(info))) {
                return info;
            }
        }
        return null;
    }

    public static boolean equals(String from, String to) {
        if (from == null || from.length() < 1 || to == null || to.length() < 1) {
            return false;
        }
        return from.equals(to);
    }

    public static boolean isEmpty(String data) {
        return data == null || data.length() < 1;
    }
}
