package org.zstack.utils;

import groovy.lang.GroovyClassLoader;

import java.io.InputStream;

/**
 */
public class GroovyUtils {
    public static <T> T loadClass(String scriptPath, ClassLoader parent) {
        GroovyClassLoader loader = new GroovyClassLoader(parent);
        InputStream in =  parent.getResourceAsStream(scriptPath);
        String script = StringDSL.inputStreamToString(in);
        Class writerClass = loader.parseClass(script);
        try {
            Object obj = writerClass.newInstance();
            return (T)obj;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
