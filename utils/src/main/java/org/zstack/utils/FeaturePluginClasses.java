package org.zstack.utils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Finds a class by name among the platform classes and then the classes of the feature plugins loaded at runtime,
 * each of which has a class loader of its own. Used where a class name arrives in data, e.g. a message from another
 * management node or a stored API result.
 */
public class FeaturePluginClasses {
    private static final List<ClassLoader> loaders = new CopyOnWriteArrayList<>();

    public static void add(ClassLoader cl) {
        loaders.add(cl);
    }

    public static void remove(ClassLoader cl) {
        loaders.remove(cl);
    }

    public static Class<?> forName(String name) throws ClassNotFoundException {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            for (ClassLoader cl : loaders) {
                try {
                    return Class.forName(name, true, cl);
                } catch (ClassNotFoundException ignored) {
                    // the next plugin
                }
            }
            throw e;
        }
    }
}
