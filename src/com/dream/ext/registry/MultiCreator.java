/*
 * Copyright (C) 2026 GreenDream
 */
package com.dream.ext.registry;

import androidx.annotation.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Host class -> creator class bindings for one ext interface. */
final class MultiCreator {
    private final Map<Class<?>, Class<?>> mCreators = new ConcurrentHashMap<>();

    MultiCreator(Class<?> hostClass, Class<?> creatorClass) {
        putClass(hostClass, creatorClass);
    }

    @Nullable
    Class<?> getCreator(@Nullable Class<?> hostClass) {
        return hostClass == null ? null : mCreators.get(hostClass);
    }

    void putClass(Class<?> hostClass, Class<?> creatorClass) {
        mCreators.put(hostClass, creatorClass);
    }
}
