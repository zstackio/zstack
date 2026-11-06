package org.zstack.core.plugin;

import org.springframework.context.ApplicationContext;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The management node part of a feature plugin loaded at runtime: its class loader, its classes, the files under its
 * classes/ folder keyed by relative path (e.g. serviceConfig/net-probe.xml) and its entities.
 */
public class LoadedFeaturePlugin {
    private final String id;
    private final ClassLoader classLoader;
    private final List<Class<?>> classes;
    private final Map<String, File> files;
    private final List<Class<?>> entities;
    private final ApplicationContext context;

    public LoadedFeaturePlugin(String id, ClassLoader classLoader, List<Class<?>> classes, Map<String, File> files,
                               List<Class<?>> entities, ApplicationContext context) {
        this.context = context;
        this.id = id;
        this.classLoader = classLoader;
        this.classes = Collections.unmodifiableList(classes);
        this.files = Collections.unmodifiableMap(files);
        this.entities = Collections.unmodifiableList(entities);
    }

    /**
     * The plugin's Spring context, whose parent is the platform's: it finds the beans of both.
     */
    public ApplicationContext getApplicationContext() {
        return context;
    }

    public String getId() {
        return id;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public List<Class<?>> getClasses() {
        return classes;
    }

    public List<Class<?>> getEntities() {
        return entities;
    }

    /**
     * @param folder a folder under classes/ ending with a slash, e.g. "globalConfig/"
     */
    public List<File> filesUnder(String folder) {
        return files.entrySet().stream().filter(e -> e.getKey().startsWith(folder))
                .sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).collect(Collectors.toList());
    }

    public List<Class<?>> classesAnnotatedWith(Class<? extends Annotation> annotation) {
        return classes.stream().filter(c -> c.isAnnotationPresent(annotation)).collect(Collectors.toList());
    }

    public List<Class<?>> subTypesOf(Class<?> type) {
        return classes.stream().filter(c -> type.isAssignableFrom(c) && c != type).collect(Collectors.toList());
    }
}
