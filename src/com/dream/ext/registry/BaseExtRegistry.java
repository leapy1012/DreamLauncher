/*
 * Copyright (C) 2026 GreenDream
 */
package com.dream.ext.registry;

import android.content.Context;

/**
 * Single place where every ext implementation is registered. Each AOSP area adds one line
 * here: either {@code ExtRegistry.registerObj(IFooExt.class, FooDreamImpl::new)} or, when the
 * implementation depends on the device form factor, a {@code FooExtRegistry.registerObj()}
 * helper next to the ext interface.
 */
public final class BaseExtRegistry {

    private BaseExtRegistry() { }

    public static void init(Context context) {
    }
}
