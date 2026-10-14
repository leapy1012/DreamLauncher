/*
 * Copyright (C) 2026 GreenDream
 */
package com.dream.ext.core;

import androidx.annotation.Nullable;

/**
 * Factory side of an ext implementation. An implementation class usually implements both its
 * ext interface {@code T} and {@code IExtCreator<T>}, and binds itself to the host in
 * {@link #createExtWith(Object)}.
 *
 * @param <T> ext interface type
 */
public interface IExtCreator<T> {

    /**
     * @param base host object the ext is attached to (e.g. the {@code Workspace}), or null for
     *             host-less exts
     * @return the ext instance bound to {@code base}, or null to fall back to the default
     */
    @Nullable
    T createExtWith(@Nullable Object base);
}
