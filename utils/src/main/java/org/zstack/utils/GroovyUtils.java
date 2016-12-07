package org.zstack.utils;

import groovy.lang.GroovyClassLoader;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class GroovyUtils {
    public static <T> T newInstance(String scriptPath, ClassLoader parent) {
        try {
            Class clz = getClass(scriptPath, parent);
            return (T)clz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T newInstance(String scriptPath) {
        return newInstance(scriptPath, GroovyUtils.class.getClassLoader());
    }

    public static <T> T newInstance(String scriptPath, Object...params) {
        Class clz = getClass(scriptPath);
        try {

            if (params.length != 0) {
                List<Class> clzs = new ArrayList<>();
                for (Object param : params) {
                    clzs.add(param.getClass());
                }

                return (T) clz.getConstructor(clzs.toArray(new Class[clzs.size()])).newInstance(params);
            } else {
                return newInstance(scriptPath);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Class getClass(String scriptPath) {
        return getClass(scriptPath, GroovyUtils.class.getClassLoader());
    }

    public static Class getClass(String scriptPath, ClassLoader parent) {
        GroovyClassLoader loader = new GroovyClassLoader(parent);
        InputStream in =  parent.getResourceAsStream(scriptPath);
        String script = StringDSL.inputStreamToString(in);
        return loader.parseClass(script);
    }
}
