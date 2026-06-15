/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.model;

import android.os.UserHandle;
import android.os.UserManager;
import android.util.LongSparseArray;
import android.util.SparseBooleanArray;

import com.android.launcher3.pm.UserCache;
//hxy-bug clone app bug 20240416 by yangj
import android.content.pm.UserInfo;
//hxy-bug clone app bug 20240416 by yangj

/**
 * Utility class to manager store and user manager state at any particular time
 */
public class UserManagerState {

    public final LongSparseArray<UserHandle> allUsers = new LongSparseArray<>();

    private final LongSparseArray<Boolean> mQuietUsersSerialNoMap = new LongSparseArray<>();
    private final SparseBooleanArray mQuietUsersHashCodeMap = new SparseBooleanArray();

    /**
     * Initialises the state values for all users
     */
    public void init(UserCache userCache, UserManager userManager) {
        for (UserHandle user : userManager.getUserProfiles()) {
            //hxy-bug clone app bug 20240416 by yangj
           // boolean isClone = isUserTypeProfile(userManager, user,
           //         UserManager.USER_TYPE_PROFILE_CLONE);
           // if(isClone){
               // continue;
           // }
            //hxy-bug clone app bug 20240416 by yangj
            long serialNo = userCache.getSerialNumberForUser(user);
            boolean isUserQuiet = userManager.isQuietModeEnabled(user);
            allUsers.put(serialNo, user);
            mQuietUsersHashCodeMap.put(user.hashCode(), isUserQuiet);
            mQuietUsersSerialNoMap.put(serialNo, isUserQuiet);
        }
    }

    /**
     * Returns true if quiet mode is enabled for the provided user
     */
    public boolean isUserQuiet(long serialNo) {
        return mQuietUsersSerialNoMap.get(serialNo);
    }

    /**
     * Returns true if quiet mode is enabled for the provided user
     */
    public boolean isUserQuiet(UserHandle user) {
        return mQuietUsersHashCodeMap.get(user.hashCode());
    }

    /**
     * Returns true if any user profile has quiet mode enabled.
     */
    public boolean isAnyProfileQuietModeEnabled() {
        for (int i = mQuietUsersHashCodeMap.size() - 1; i >= 0; i--) {
            if (mQuietUsersHashCodeMap.valueAt(i)) {
                return true;
            }
        }
        return false;
    }
    //hxy-bug clone app bug 20240416 by yangj
    public boolean isUserTypeProfile ( UserManager userManager, UserHandle userHandle, String type) {
        UserInfo userInfo = userManager.getUserInfo(userHandle.getIdentifier());
        boolean result = false;
        if(userInfo != null)
        switch (type) {
            case UserManager.USER_TYPE_PROFILE_CLONE:
                result = userInfo != null && userInfo.isCloneProfile();
                break;
            case UserManager.USER_TYPE_PROFILE_MANAGED:
                result = userInfo != null && userInfo.isManagedProfile();
                break;
            default:
                break;
        }
        return result;
    }
    //hxy-bug clone app bug 20240416 by yangj
}
