/*
 * Copyright (C) 2026 GreenDream
 */
package com.dream.ext.registry;

import com.dream.ext.core.IExtCreator;

/**
 * Supplies a fresh creator per host, e.g. {@code WorkspaceDreamImpl::new}. Preferred over
 * class registration because it avoids reflection and survives R8 without keep rules.
 */
@FunctionalInterface
public interface IExtCreatorObjGetter<T> {
    IExtCreator<T> get();
}
