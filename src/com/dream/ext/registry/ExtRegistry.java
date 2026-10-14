/*
 * Copyright (C) 2026 GreenDream
 */
package com.dream.ext.registry;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.launcher3.R;
import com.dream.ext.core.IExtCreator;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ext interface -> implementation bindings.
 *
 * <p>Lookup order for {@code getExt(ext, hostClass)}:
 * <ol>
 *   <li>getter registered for {@code (ext, hostClass)}</li>
 *   <li>getter registered for {@code ext}</li>
 *   <li>creator class registered for {@code (ext, hostClass)}</li>
 *   <li>creator class registered for {@code ext}, including {@code ext_api_override}</li>
 * </ol>
 *
 * <p>{@code R.array.ext_api_override} items are {@code "ext.Interface/impl.Creator"} pairs, so
 * an overlay can rebind an ext without code changes.
 */
public final class ExtRegistry {
    private static final String TAG = "ExtRegistry";

    private static final Map<Class<?>, Class<?>> EXT_CLASS_CREATORS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, MultiCreator> BASE_CLASS_EXT_MAP =
            new ConcurrentHashMap<>();
    private static final Map<Class<?>, IExtCreatorObjGetter<?>> EXT_OBJ_GETTERS =
            new ConcurrentHashMap<>();
    private static final Map<Class<?>, MultiObjCreator> BASE_CLASS_EXT_OBJ_MAP =
            new ConcurrentHashMap<>();

    private ExtRegistry() { }

    /** Applies {@code R.array.ext_api_override} on top of the code registrations. */
    public static void init(Context context, ClassLoader classLoader) {
        String[] overrides = context.getResources().getStringArray(R.array.ext_api_override);
        for (String item : overrides) {
            String[] pair = item.split("/");
            if (pair.length != 2) {
                Log.w(TAG, "malformed ext_api_override item: " + item);
                continue;
            }
            try {
                Class<?> extClass = classLoader.loadClass(pair[0].trim());
                Class<?> creatorClass = classLoader.loadClass(pair[1].trim());
                if (createsExt(creatorClass, extClass)) {
                    EXT_CLASS_CREATORS.put(extClass, creatorClass);
                    // A class override must win over code-registered getters.
                    EXT_OBJ_GETTERS.remove(extClass);
                } else {
                    Log.w(TAG, pair[1] + " does not implement IExtCreator<" + pair[0] + ">");
                }
            } catch (ClassNotFoundException e) {
                Log.w(TAG, "ext_api_override class not found: " + item);
            }
        }
    }

    public static <T, C extends IExtCreator<T>> void registerExt(Class<T> extClass,
            Class<C> creatorClass) {
        registerExt(extClass, null, creatorClass);
    }

    /** Binds {@code creatorClass} for {@code extClass}, optionally only for {@code hostClass}. */
    public static <T, C extends IExtCreator<T>> void registerExt(Class<T> extClass,
            @Nullable Class<?> hostClass, Class<C> creatorClass) {
        if (extClass == null || creatorClass == null) {
            return;
        }
        if (hostClass == null) {
            EXT_CLASS_CREATORS.put(extClass, creatorClass);
            return;
        }
        MultiCreator multi = BASE_CLASS_EXT_MAP.get(extClass);
        if (multi != null) {
            multi.putClass(hostClass, creatorClass);
        } else {
            BASE_CLASS_EXT_MAP.put(extClass, new MultiCreator(hostClass, creatorClass));
        }
    }

    public static <T> void registerObj(Class<T> extClass, IExtCreatorObjGetter<T> getter) {
        registerObj(extClass, null, getter);
    }

    /** Binds {@code getter} for {@code extClass}, optionally only for {@code hostClass}. */
    public static <T> void registerObj(Class<T> extClass, @Nullable Class<?> hostClass,
            IExtCreatorObjGetter<T> getter) {
        if (extClass == null || getter == null) {
            return;
        }
        if (hostClass == null) {
            EXT_OBJ_GETTERS.put(extClass, getter);
            return;
        }
        MultiObjCreator multi = BASE_CLASS_EXT_OBJ_MAP.get(extClass);
        if (multi != null) {
            multi.putGetter(hostClass, getter);
        } else {
            BASE_CLASS_EXT_OBJ_MAP.put(extClass, new MultiObjCreator(hostClass, getter));
        }
    }

    @Nullable
    public static <T> IExtCreator<T> getExt(Class<T> extClass) {
        return getExt(extClass, null);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> IExtCreator<T> getExt(Class<T> extClass, @Nullable Class<?> hostClass) {
        if (extClass == null) {
            return null;
        }
        MultiObjCreator multiObj = BASE_CLASS_EXT_OBJ_MAP.get(extClass);
        IExtCreator<T> creator = multiObj != null ? multiObj.getObject(hostClass) : null;
        if (creator == null) {
            IExtCreatorObjGetter<?> getter = EXT_OBJ_GETTERS.get(extClass);
            if (getter != null) {
                creator = (IExtCreator<T>) getter.get();
            }
        }
        if (creator != null) {
            return creator;
        }
        MultiCreator multi = BASE_CLASS_EXT_MAP.get(extClass);
        Class<?> creatorClass = multi != null ? multi.getCreator(hostClass) : null;
        if (creatorClass == null) {
            creatorClass = EXT_CLASS_CREATORS.get(extClass);
        }
        if (creatorClass == null) {
            return null;
        }
        try {
            return (IExtCreator<T>) creatorClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | ClassCastException e) {
            Log.e(TAG, "cannot instantiate " + creatorClass.getName(), e);
            return null;
        }
    }

    private static boolean createsExt(Class<?> creatorClass, Class<?> extClass) {
        for (Class<?> c = creatorClass; c != null; c = c.getSuperclass()) {
            for (Type type : c.getGenericInterfaces()) {
                if (type instanceof ParameterizedType
                        && ((ParameterizedType) type).getRawType() == IExtCreator.class
                        && ((ParameterizedType) type).getActualTypeArguments()[0] == extClass) {
                    return true;
                }
            }
        }
        return false;
    }
}
