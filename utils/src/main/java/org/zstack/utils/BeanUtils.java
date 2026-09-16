package org.zstack.utils;

import org.apache.commons.beanutils.PropertyUtils;
import org.reflections.Reflections;
import org.reflections.scanners.*;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;

/**
 */
public class BeanUtils {
    public static Reflections reflections;

    private static final Pattern INDEXED_PATH = Pattern.compile("(.*)\\[(\\d+)]");

    static {
        ConfigurationBuilder builder = ConfigurationBuilder.build()
                .setUrls(ClasspathHelper.forPackage("org.zstack"))
                .setScanners(Scanners.SubTypes, Scanners.MethodsAnnotated,
                        Scanners.FieldsAnnotated, Scanners.TypesAnnotated,
                        Scanners.MethodsParameter)
                .setExpandSuperTypes(false)
                .filterInputsBy(new FilterBuilder().includePackage("org.zstack"));
        reflections = new Reflections(builder);
    }

    private static Object getProperty(Object bean, Iterator<String> it) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        String path = it.next();
        if (bean instanceof Map) {
            Pattern re = Pattern.compile("(.*)\\[(\\d+)]");
            Matcher m = re.matcher(path);
            if (m.find()) {
                path = String.format("(%s)[%s]", m.group(1), m.group(2));
            }
        }

        Object val = PropertyUtils.getProperty(bean, path);

        if (it.hasNext()) {
            return getProperty(val, it);
        } else {
            return val;
        }
    }

    public static Object getProperty(Object bean, String path) {
        List<String> paths = asList(path.split("\\."));
        try {
            return getProperty(bean, paths.iterator());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Object getPropertyOrField(Object bean, Iterator<String> it)
            throws IllegalAccessException, InvocationTargetException {
        String segment = it.next();
        Matcher m = INDEXED_PATH.matcher(segment);
        String index = null;
        if (m.matches()) {
            segment = m.group(1);
            index = m.group(2);
        }

        Object val = bean instanceof Map ? ((Map) bean).get(segment) : readPropertyOrField(bean, segment);
        if (index != null) {
            int position = Integer.parseInt(index);
            val = val instanceof List && position < ((List) val).size() ? ((List) val).get(position) : null;
        }

        if (val == null || !it.hasNext()) {
            return val;
        }
        return getPropertyOrField(val, it);
    }

    private static Object readPropertyOrField(Object bean, String name)
            throws IllegalAccessException, InvocationTargetException {
        try {
            return PropertyUtils.getProperty(bean, name);
        } catch (NoSuchMethodException e) {
            Field f = FieldUtils.getField(name, bean.getClass());
            if (f == null) {
                return null;
            }

            f.setAccessible(true);
            return f.get(bean);
        }
    }

    /**
     * Reads a nested path like {@link #getProperty(Object, String)} but also accepts
     * plain public fields, because cross-MN message schema restoration must not
     * require transport DTOs to be JavaBeans. Returns null when the path cannot be
     * resolved, so callers can decide whether that is fatal.
     */
    public static Object getPropertyOrField(Object bean, String path) {
        try {
            return getPropertyOrField(bean, asList(path.split("\\.")).iterator());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Writes a nested path, preferring the JavaBean setter and falling back to the
     * public field. Returns false when the path cannot be written.
     */
    public static boolean setPropertyOrField(Object bean, String path, Object val) {
        List<String> paths = asList(path.split("\\."));
        String name = paths.get(paths.size() - 1);

        try {
            Object target = bean;
            for (String segment : paths.subList(0, paths.size() - 1)) {
                target = getPropertyOrField(target, segment);
                if (target == null) {
                    return false;
                }
            }

            Matcher m = INDEXED_PATH.matcher(name);
            if (m.matches()) {
                Object container = target instanceof Map
                        ? ((Map) target).get(m.group(1)) : readPropertyOrField(target, m.group(1));
                if (!(container instanceof List)) {
                    return false;
                }

                ((List) container).set(Integer.parseInt(m.group(2)), val);
                return true;
            }

            if (target instanceof Map) {
                ((Map) target).put(name, val);
                return true;
            }

            try {
                PropertyUtils.setProperty(target, name, val);
                return true;
            } catch (NoSuchMethodException e) {
                Field f = FieldUtils.getField(name, target.getClass());
                if (f == null) {
                    return false;
                }

                f.setAccessible(true);
                f.set(target, val);
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private static void setProperty(Object bean, Iterator<String> it, String fieldName, Object val) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        if (it.hasNext()) {
            bean = getProperty(bean, it);
        }

        if (bean instanceof Map) {
            Pattern re = Pattern.compile("(.*)\\[(\\d+)]");
            Matcher m = re.matcher(fieldName);
            if (m.find()) {
                fieldName = String.format("(%s)[%s]", m.group(1), m.group(2));
            }
        }

        PropertyUtils.setProperty(bean, fieldName, val);
    }

    public static void setProperty(Object bean, String path, Object val) {
        List<String> paths = asList(path.split("\\."));
        String fieldName = paths.get(paths.size()-1);
        paths = paths.subList(0, paths.size()-1);

        try {
            setProperty(bean, paths.iterator(), fieldName, val);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
