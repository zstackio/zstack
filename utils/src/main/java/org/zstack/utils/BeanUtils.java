package org.zstack.utils;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.*;
import org.reflections.util.ClasspathHelper;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;

/**
 */
public class BeanUtils {
    static CLogger logger = Utils.getLogger(BeanUtils.class);
    public static Reflections reflections = new Reflections(ClasspathHelper.forPackage("org.zstack"),
            new SubTypesScanner(), new MethodAnnotationsScanner(), new FieldAnnotationsScanner(),
            new TypeAnnotationsScanner(), new MethodParameterScanner());

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
