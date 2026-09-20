/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.launcher3.notification;

import android.app.Notification;
import android.app.Person;
import android.content.Context;
import android.content.pm.PackageManager;
import android.service.notification.StatusBarNotification;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.launcher3.Utilities;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The key data associated with the notification, used to determine what to include
 * in dots and stub popup views before they are populated.
 *
 * @see NotificationInfo for the full data used when populating the stub views.
 */
public class NotificationKeyData {
    private static final Pattern NUMBER_ONLY = Pattern.compile("^\\s*(\\d{1,3})\\s*$");
    private static final Pattern MISSED_COUNT =
            Pattern.compile("(?i)(\\d{1,3})\\s*miss");

    public final String notificationKey;
    public final String shortcutId;
    @NonNull
    public final String[] personKeysFromNotification;
    public int count;

    private NotificationKeyData(String notificationKey, String shortcutId, int count,
            String[] personKeysFromNotification) {
        this.notificationKey = notificationKey;
        this.shortcutId = shortcutId;
        this.count = Math.max(1, count);
        this.personKeysFromNotification = personKeysFromNotification;
    }

    public static NotificationKeyData fromNotification(StatusBarNotification sbn) {
        return fromNotification(sbn, null);
    }

    public static NotificationKeyData fromNotification(StatusBarNotification sbn,
            @Nullable Context context) {
        Notification notif = sbn.getNotification();
        int count = notif.number;
        if (count <= 0) {
            count = resolveBadgeCount(notif, sbn, context);
        }
        return new NotificationKeyData(sbn.getKey(), notif.getShortcutId(), count,
                extractPersonKeyOnly(notif.extras.getParcelableArrayList(
                        Notification.EXTRA_PEOPLE_LIST)));
    }

    /**
     * Dialer missed-call posts often leave {@link Notification#number} at 0 and put the
     * real count only inside custom {@link RemoteViews} text (no EXTRA_TITLE/TEXT).
     */
    private static int resolveBadgeCount(Notification notif, StatusBarNotification sbn,
            @Nullable Context context) {
        int fromExtras = extractNumber(notif.extras.getCharSequence(Notification.EXTRA_TITLE));
        if (fromExtras <= 0) {
            fromExtras = extractNumber(notif.extras.getCharSequence(Notification.EXTRA_TEXT));
        }
        if (fromExtras <= 0) {
            fromExtras = extractNumber(notif.extras.getCharSequence(Notification.EXTRA_INFO_TEXT));
        }
        if (fromExtras > 0) {
            return fromExtras;
        }
        int fromViews = extractCountFromRemoteViews(notif.contentView);
        if (fromViews <= 0) {
            fromViews = extractCountFromRemoteViews(notif.bigContentView);
        }
        if (fromViews <= 0 && context != null) {
            fromViews = extractCountByApplyingRemoteViews(context, sbn);
        }
        return fromViews;
    }

    private static int extractNumber(@Nullable CharSequence text) {
        if (text == null) {
            return 0;
        }
        String s = text.toString().trim();
        Matcher missed = MISSED_COUNT.matcher(s);
        if (missed.find()) {
            return Integer.parseInt(missed.group(1));
        }
        // Standalone badge digit only — avoid phone numbers / multi-token strings.
        Matcher only = NUMBER_ONLY.matcher(s);
        if (only.matches()) {
            return Integer.parseInt(only.group(1));
        }
        return 0;
    }

    private static int extractCountFromRemoteViews(@Nullable RemoteViews remoteViews) {
        if (remoteViews == null) {
            return 0;
        }
        try {
            Field actionsField = RemoteViews.class.getDeclaredField("mActions");
            actionsField.setAccessible(true);
            Object actionsObj = actionsField.get(remoteViews);
            if (!(actionsObj instanceof ArrayList)) {
                return 0;
            }
            int missedStyle = 0;
            int standalone = 0;
            for (Object action : (ArrayList<?>) actionsObj) {
                if (action == null) {
                    continue;
                }
                CharSequence text = readCharSequenceField(action, "value");
                if (text == null) {
                    text = readCharSequenceField(action, "mText");
                }
                if (text == null) {
                    text = readCharSequenceField(action, "charSequenceValue");
                }
                if (text == null) {
                    continue;
                }
                String s = text.toString().trim();
                Matcher missed = MISSED_COUNT.matcher(s);
                if (missed.find()) {
                    missedStyle = Math.max(missedStyle, Integer.parseInt(missed.group(1)));
                    continue;
                }
                Matcher only = NUMBER_ONLY.matcher(s);
                if (only.matches()) {
                    standalone = Math.max(standalone, Integer.parseInt(only.group(1)));
                }
            }
            return missedStyle > 0 ? missedStyle : standalone;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    /**
     * Inflate custom RemoteViews and read TextView content (MTK Dialer missedCall).
     */
    private static int extractCountByApplyingRemoteViews(Context context,
            StatusBarNotification sbn) {
        Notification notif = sbn.getNotification();
        RemoteViews rv = notif.contentView != null ? notif.contentView : notif.bigContentView;
        if (rv == null) {
            return 0;
        }
        try {
            Context pkgContext = context.createPackageContextAsUser(
                    sbn.getPackageName(), Context.CONTEXT_RESTRICTED, sbn.getUser());
            FrameLayout host = new FrameLayout(pkgContext);
            View applied = rv.apply(pkgContext, host);
            int[] result = new int[2]; // [missedStyle, standalone]
            collectCountsFromView(applied, result);
            return result[0] > 0 ? result[0] : result[1];
        } catch (PackageManager.NameNotFoundException | RuntimeException ignored) {
            return 0;
        }
    }

    private static void collectCountsFromView(@Nullable View view, int[] result) {
        if (view == null) {
            return;
        }
        if (view instanceof TextView) {
            CharSequence text = ((TextView) view).getText();
            if (text != null) {
                String s = text.toString().trim();
                Matcher missed = MISSED_COUNT.matcher(s);
                if (missed.find()) {
                    result[0] = Math.max(result[0], Integer.parseInt(missed.group(1)));
                } else {
                    Matcher only = NUMBER_ONLY.matcher(s);
                    if (only.matches()) {
                        result[1] = Math.max(result[1], Integer.parseInt(only.group(1)));
                    }
                }
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collectCountsFromView(group.getChildAt(i), result);
            }
        }
    }

    @Nullable
    private static CharSequence readCharSequenceField(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(target);
            return value instanceof CharSequence ? (CharSequence) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static List<String> extractKeysOnly(
            @NonNull List<NotificationKeyData> notificationKeys) {
        List<String> keysOnly = new ArrayList<>(notificationKeys.size());
        for (NotificationKeyData notificationKeyData : notificationKeys) {
            keysOnly.add(notificationKeyData.notificationKey);
        }
        return keysOnly;
    }

    private static String[] extractPersonKeyOnly(@Nullable ArrayList<Person> people) {
        if (people == null || people.isEmpty()) {
            return Utilities.EMPTY_STRING_ARRAY;
        }
        return people.stream().filter(person -> person.getKey() != null)
                .map(Person::getKey).sorted().toArray(String[]::new);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof NotificationKeyData)) {
            return false;
        }
        // Only compare the keys.
        return ((NotificationKeyData) obj).notificationKey.equals(notificationKey);
    }
}
