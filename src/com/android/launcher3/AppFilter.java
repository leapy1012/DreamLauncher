package com.android.launcher3;

import android.content.ComponentName;
import android.content.Context;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class to filter out components from various lists
 */
public class AppFilter {

    private final Set<ComponentName> mFilteredComponents;

    /** Components that share a package badge but should never show a launcher dot. */
    private static Set<ComponentName> sHideDotComponents;

    public AppFilter(Context context) {
        mFilteredComponents = Arrays.stream(
                context.getResources().getStringArray(R.array.filtered_components))
                .map(ComponentName::unflattenFromString)
                .collect(Collectors.toSet());
    }

    public boolean shouldShowApp(ComponentName app) {
        return app == null || !mFilteredComponents.contains(app);
    }

    /**
     * Returns true when {@code component} is listed in {@code hide_dot_components}
     * (Oppo-style: Contacts icon must not inherit Dialer's missed-call badge).
     */
    public static boolean isHideDot(Context context, ComponentName component) {
        if (component == null) {
            return false;
        }
        if (sHideDotComponents == null) {
            String[] entries = context.getResources().getStringArray(R.array.hide_dot_components);
            if (entries == null || entries.length == 0) {
                sHideDotComponents = Collections.emptySet();
            } else {
                sHideDotComponents = Arrays.stream(entries)
                        .map(ComponentName::unflattenFromString)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
            }
        }
        return sHideDotComponents.contains(component);
    }
}
