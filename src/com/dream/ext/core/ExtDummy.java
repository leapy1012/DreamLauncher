/*
 * Copyright (C) 2026 GreenDream
 */
package com.dream.ext.core;

import android.util.Log;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

/**
 * No-op ext used when no implementation is registered: interface default methods run as
 * written, everything else returns a zero value ({@code false}, {@code 0}, {@code ""}) or null.
 */
final class ExtDummy {
    private static final String TAG = "ExtDummy";

    private static final Map<Class<?>, Object> DEFAULT_VALUES = Map.ofEntries(
            Map.entry(boolean.class, false),
            Map.entry(byte.class, (byte) 0),
            Map.entry(short.class, (short) 0),
            Map.entry(char.class, (char) 0),
            Map.entry(int.class, 0),
            Map.entry(long.class, 0L),
            Map.entry(float.class, 0f),
            Map.entry(double.class, 0d),
            Map.entry(Boolean.class, false),
            Map.entry(Byte.class, (byte) 0),
            Map.entry(Short.class, (short) 0),
            Map.entry(Character.class, (char) 0),
            Map.entry(Integer.class, 0),
            Map.entry(Long.class, 0L),
            Map.entry(Float.class, 0f),
            Map.entry(Double.class, 0d),
            Map.entry(String.class, ""));

    private ExtDummy() { }

    @SuppressWarnings("unchecked")
    static <T> T createDummyExt(Class<T> extClass, boolean debug) {
        if (!extClass.isInterface()) {
            return null;
        }
        T proxy = (T) Proxy.newProxyInstance(extClass.getClassLoader(), new Class<?>[]{extClass},
                (self, method, args) -> invoke(extClass, self, method, args, debug));
        if (debug) {
            Log.d(TAG, "dummy ext for " + extClass.getName());
        }
        return proxy;
    }

    private static Object invoke(Class<?> extClass, Object self, Method method, Object[] args,
            boolean debug) {
        if (method.getDeclaringClass() == Object.class) {
            switch (method.getName()) {
                case "hashCode":
                    return System.identityHashCode(self);
                case "equals":
                    return self == args[0];
                case "toString":
                    return "ExtDummy[" + extClass.getName() + "]";
                default:
                    return null;
            }
        }
        if (method.isDefault()) {
            try {
                return MethodHandles.lookup()
                        .findSpecial(method.getDeclaringClass(), method.getName(),
                                MethodType.methodType(method.getReturnType(),
                                        method.getParameterTypes()),
                                method.getDeclaringClass())
                        .bindTo(self)
                        .invokeWithArguments(args == null ? new Object[0] : args);
            } catch (Throwable t) {
                Log.e(TAG, "default method failed: " + extClass.getName() + "."
                        + method.getName(), t);
            }
        }
        Object value = DEFAULT_VALUES.get(method.getReturnType());
        if (debug) {
            Log.d(TAG, extClass.getSimpleName() + "." + method.getName() + "() -> " + value);
        }
        return value;
    }
}
