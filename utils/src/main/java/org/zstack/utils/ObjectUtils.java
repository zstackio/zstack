package org.zstack.utils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class ObjectUtils {
    public static <T> T copy(T dst, Object src) {
        Class dstClass = dst.getClass();
        Class srcClass = null;
        Class clazz = src.getClass();
        do {
            if (clazz.isAssignableFrom(dstClass)) {
                srcClass = clazz;
                break;
            }
            clazz = clazz.getSuperclass();
        } while (clazz != Object.class);

        if (srcClass == null) {
            throw new RuntimeException(String.format("%s is not assignable from %s", src.getClass().getName(), dstClass.getName()));
        }

        try {
            Map<String, Field> dstFields = new HashMap<String, Field>();
            for (Field f : FieldUtils.getAllFields(dstClass)) {
                dstFields.put(f.getName(), f);
            }

            for (Field f : FieldUtils.getAllFields(srcClass)) {
                f.setAccessible(true);
                Field dstf = dstFields.get(f.getName());
                dstf.setAccessible(true);
                dstf.set(dst, f.get(src));
            }

            return dst;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T newAndCopy(Object src, Class<T> dstClass) {
        if (src == null) {
            return null;
        }

        try {
            T dst = dstClass.newInstance();
            return copy(dst, src);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
