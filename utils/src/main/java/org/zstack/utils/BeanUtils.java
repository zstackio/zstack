package org.zstack.utils;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.zstack.utils.function.Function;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;

/**
 */
public class BeanUtils {

    private enum FilterType {
        CLASS,
        ANNOTATION,
    }

    private interface TypeWrapper {
        FilterType filterType();
        Class clazz();
    }

    public static List<Class> scanClass(List<String> pkgNames, List<TypeWrapper> includes, List<TypeWrapper> excludes) {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        if (includes != null) {
            for (TypeWrapper wrapper : includes) {
                if (wrapper.filterType() == FilterType.ANNOTATION) {
                    scanner.addIncludeFilter(new AnnotationTypeFilter(wrapper.clazz()));
                } else if (wrapper.filterType() == FilterType.CLASS) {
                    scanner.addIncludeFilter(new AssignableTypeFilter(wrapper.clazz()));
                } else {
                    DebugUtils.Assert(false, String.format("unknown type: %s", wrapper.filterType()));
                }
            }
        }
        if (excludes != null) {
            for (TypeWrapper wrapper : excludes) {
                if (wrapper.filterType() == FilterType.ANNOTATION) {
                    scanner.addExcludeFilter(new AnnotationTypeFilter(wrapper.clazz()));
                } else if (wrapper.filterType() == FilterType.CLASS) {
                    scanner.addExcludeFilter(new AssignableTypeFilter(wrapper.clazz()));
                } else {
                    DebugUtils.Assert(false, String.format("unknown type: %s", wrapper.filterType()));
                }
            }
        }

        List<Class> ret = new ArrayList<Class>();
        for (String pkg : pkgNames) {
            for (BeanDefinition bd : scanner.findCandidateComponents(pkg)) {
                try {
                    Class clz = Class.forName(bd.getBeanClassName());
                    ret.add(clz);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return ret;
    }

    public static List<Class> scanClassByType(List<String> pkgNames, List<Class> includeTypes, List<Class> excludeTypes) {
        List<TypeWrapper> includes = CollectionUtils.transformToList(includeTypes, new Function<TypeWrapper, Class>() {
            @Override
            public TypeWrapper call(final Class arg) {
                return new TypeWrapper() {
                    @Override
                    public FilterType filterType() {
                        return FilterType.CLASS;
                    }

                    @Override
                    public Class clazz() {
                        return arg;
                    }
                };
            }
        });

        List<TypeWrapper> excludes = CollectionUtils.transformToList(excludeTypes, new Function<TypeWrapper, Class>() {
            @Override
            public TypeWrapper call(final Class arg) {
                return new TypeWrapper() {
                    @Override
                    public FilterType filterType() {
                        return FilterType.CLASS;
                    }

                    @Override
                    public Class clazz() {
                        return arg;
                    }
                };
            }
        });

        return scanClass(pkgNames, includes, excludes);
    }

    public static List<Class> scanClassByType(List<String> pkgNames, List<Class> includeTypes) {
        List<TypeWrapper> includes = CollectionUtils.transformToList(includeTypes, new Function<TypeWrapper, Class>() {
            @Override
            public TypeWrapper call(final Class arg) {
                return new TypeWrapper() {
                    @Override
                    public FilterType filterType() {
                        return FilterType.CLASS;
                    }

                    @Override
                    public Class clazz() {
                        return arg;
                    }
                };
            }
        });
        return scanClass(pkgNames, includes, null);
    }

    public static List<Class> scanClassByType(String pkgName, Class includeType) {
        return scanClassByType(asList(pkgName), asList(includeType));
    }

    public static List<Class> scanClass(List<String> pkgNames, List<Class> includeAnnotations) {
        List<TypeWrapper> includes = CollectionUtils.transformToList(includeAnnotations, new Function<TypeWrapper, Class>() {
            @Override
            public TypeWrapper call(final Class arg) {
                return new TypeWrapper() {
                    @Override
                    public FilterType filterType() {
                        return FilterType.ANNOTATION;
                    }

                    @Override
                    public Class clazz() {
                        return arg;
                    }
                };
            }
        });
        return scanClass(pkgNames, includes, null);
    }

    public static List<Class> scanClass(String pkgName, List<Class> includeAnnotations) {
        return scanClass(asList(pkgName), includeAnnotations);
    }

    public static List<Class> scanClass(String pkgName, Class includeAnnotation) {
        return scanClass(asList(pkgName), asList(includeAnnotation));
    }

    public static List<Class> scanClass(List<String> pkgNames, Class includeAnnotation) {
        return scanClass(pkgNames, asList(includeAnnotation));
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
