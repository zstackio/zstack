package org.zstack.utils.gson;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Map;

/**
 * Gson keeps the type adapter of every type it has handled for good. The types of a feature plugin that is unloaded
 * would keep the plugin's class loader, so they are dropped when the plugin is unloaded.
 */
public class GsonTypeCache {
    private static final Field CACHE;

    static {
        try {
            CACHE = Gson.class.getDeclaredField("typeTokenCache");
            CACHE.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @return how many cached types were dropped
     */
    @SuppressWarnings("unchecked")
    public static int forget(Gson gson, ClassLoader cl) {
        Map<TypeToken<?>, ?> cache;
        try {
            cache = (Map<TypeToken<?>, ?>) CACHE.get(gson);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        int before = cache.size();
        cache.keySet().removeIf(t -> refersTo(t.getType(), cl));
        return before - cache.size();
    }

    public static boolean refersTo(Type t, ClassLoader cl) {
        if (t instanceof Class) {
            Class<?> c = (Class<?>) t;
            return c.isArray() ? refersTo(c.getComponentType(), cl) : c.getClassLoader() == cl;
        }
        if (t instanceof ParameterizedType) {
            ParameterizedType p = (ParameterizedType) t;
            if (refersTo(p.getRawType(), cl)) {
                return true;
            }
            for (Type a : p.getActualTypeArguments()) {
                if (refersTo(a, cl)) {
                    return true;
                }
            }
            return false;
        }
        if (t instanceof GenericArrayType) {
            return refersTo(((GenericArrayType) t).getGenericComponentType(), cl);
        }
        if (t instanceof WildcardType) {
            WildcardType w = (WildcardType) t;
            for (Type b : w.getUpperBounds()) {
                if (refersTo(b, cl)) {
                    return true;
                }
            }
            for (Type b : w.getLowerBounds()) {
                if (refersTo(b, cl)) {
                    return true;
                }
            }
        }
        return false;
    }
}
