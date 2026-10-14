/*
 * Copyright (C) 2026 GreenDream
 */
package com.dream.ext.registry;

import androidx.annotation.Nullable;

import com.dream.ext.core.IExtCreator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Host class -> creator getter bindings for one ext interface. */
final class MultiObjCreator {
    private final Map<Class<?>, IExtCreatorObjGetter<?>> mGetters = new ConcurrentHashMap<>();

    MultiObjCreator(Class<?> hostClass, IExtCreatorObjGetter<?> getter) {
        putGetter(hostClass, getter);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    <T> IExtCreator<T> getObject(@Nullable Class<?> hostClass) {
        IExtCreatorObjGetter<?> getter = hostClass == null ? null : mGetters.get(hostClass);
        return getter == null ? null : (IExtCreator<T>) getter.get();
    }

    void putGetter(Class<?> hostClass, IExtCreatorObjGetter<?> getter) {
        mGetters.put(hostClass, getter);
    }
}
