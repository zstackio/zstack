package org.zstack.core.plugin;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.CachedIntrospectionResults;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.util.ReflectionUtils;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusGson;
import org.zstack.core.componentloader.PluginExtension;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.componentloader.PluginRegistryImpl;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.DatabaseFacadeImpl;
import org.zstack.core.db.FeaturePluginPersistenceUnits;
import org.zstack.core.log.LogSafeGson;
import org.zstack.core.thread.SyncTaskStatistic;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.thread.ThreadFacadeImpl;
import org.zstack.header.Component;
import org.zstack.header.Service;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.rbac.RBAC;
import org.zstack.header.identity.rbac.RBACDescription;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.utils.FeaturePluginClasses;
import org.zstack.utils.Utils;
import org.zstack.utils.YamlUtils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.beans.Introspector;
import java.io.File;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * Loads the management node part of a feature plugin into the running management node and unloads it, without a
 * restart. A plugin has a class loader, a Spring context (a child of the platform's) and a persistence unit of its
 * own; its parts are registered with the platform and dropped again in the reverse order.
 */
public class FeaturePluginLoader implements ApplicationContextAware, ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(FeaturePluginLoader.class);
    static final long DRAIN_MILLIS = TimeUnit.SECONDS.toMillis(5);
    private static final String SPRING_FOLDER = "pluginSpringConfigXml/";

    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;

    private ApplicationContext platform;
    private volatile boolean nodeReady;

    private static class Plugin {
        String id;
        File dir;
        URLClassLoader classLoader;
        List<Class<?>> classes = new ArrayList<>();
        List<Class<?>> entities = new ArrayList<>();
        EntityManagerFactory emf;
        GenericXmlApplicationContext context;
        final List<PluginExtension> extensions = new ArrayList<>();
        LoadedFeaturePlugin view;

        // what activate registered, for deactivate to undo
        boolean unitRegistered;
        boolean apisAdded;
        RBAC.PluginRbac rbac;
        final List<FeaturePluginRuntimeExtensionPoint> runtimeExtensions = new ArrayList<>();
        boolean extensionsAdded;
        final List<Service> services = new ArrayList<>();
        final List<Component> components = new ArrayList<>();
    }

    private static class Unloaded {
        final String id;
        final WeakReference<ClassLoader> classLoader;
        final long fullGcCount;

        Unloaded(String id, ClassLoader classLoader, long fullGcCount) {
            this.id = id;
            this.classLoader = new WeakReference<>(classLoader);
            this.fullGcCount = fullGcCount;
        }
    }

    private final Map<String, Plugin> loaded = new ConcurrentHashMap<>();
    private final List<Unloaded> unloaded = new CopyOnWriteArrayList<>();

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        platform = ctx;
    }

    @Override
    public void managementNodeReady() {
        nodeReady = true;
    }

    public boolean isLoaded(String id) {
        return loaded.containsKey(id);
    }

    public Set<String> loadedIds() {
        return new HashSet<>(loaded.keySet());
    }

    /**
     * @return plugin id -> the class loader the plugin is loaded by now
     */
    public Map<String, ClassLoader> classLoaders() {
        Map<String, ClassLoader> ret = new HashMap<>();
        loaded.forEach((id, p) -> ret.put(id, p.classLoader));
        return ret;
    }

    /**
     * @param dir the folder zstack-ctl laid the plugin's management node part out in, holding plugin.yaml, lib/ and classes/
     */
    public synchronized LoadedFeaturePlugin load(String id, File dir) {
        if (loaded.containsKey(id)) {
            throw new CloudRuntimeException(String.format("feature plugin %s is already loaded", id));
        }
        reportKeptClassLoaders();
        Plugin p = prepare(id, dir);
        try {
            activate(p);
        } catch (Throwable t) {
            deactivate(p);
            release(p);
            throw failure(id, t);
        }
        loaded.put(id, p);
        logger.info(String.format("loaded feature plugin %s from %s", id, dir));
        return p.view;
    }

    public synchronized void unload(String id) {
        Plugin p = loaded.remove(id);
        if (p == null) {
            return;
        }
        deactivate(p);
        release(p);
        logger.info(String.format("unloaded feature plugin %s", id));
    }

    /**
     * Switches a loaded plugin to another version. The new version is prepared first; its REST requests wait while
     * the old version is taken away and the new one registered. When the new version cannot be registered, the old
     * one is loaded again.
     */
    public synchronized LoadedFeaturePlugin switchTo(String id, File dir) {
        Plugin old = loaded.get(id);
        if (old == null) {
            return load(id, dir);
        }

        reportKeptClassLoaders();
        Plugin next = prepare(id, dir);
        FeaturePluginGate.open(id);
        try {
            loaded.remove(id);
            deactivate(old);
            release(old);
            try {
                activate(next);
            } catch (Throwable t) {
                deactivate(next);
                release(next);
                logger.warn(String.format("feature plugin %s cannot switch to %s, loading %s again", id, dir, old.dir), t);
                Plugin back = prepare(id, old.dir);
                try {
                    activate(back);
                } catch (Throwable t2) {
                    deactivate(back);
                    release(back);
                    logger.warn(String.format("feature plugin %s cannot be loaded from %s again", id, old.dir), t2);
                    throw failure(id, t);
                }
                loaded.put(id, back);
                throw failure(id, t);
            }
            loaded.put(id, next);
            logger.info(String.format("switched feature plugin %s to %s", id, dir));
            return next.view;
        } finally {
            FeaturePluginGate.close(id);
        }
    }

    private static CloudRuntimeException failure(String id, Throwable t) {
        if (t instanceof CloudRuntimeException && t.getMessage() != null && t.getMessage().startsWith("feature plugin " + id)) {
            return (CloudRuntimeException) t;
        }
        return new CloudRuntimeException(String.format("feature plugin %s cannot be loaded: %s", id, t.getMessage()), t);
    }

    @SuppressWarnings("unchecked")
    private Plugin prepare(String id, File dir) {
        Plugin p = new Plugin();
        p.id = id;
        p.dir = dir;
        try {
            Map<String, Object> desc = (Map<String, Object>) YamlUtils.load(
                    FileUtils.readFileToString(new File(dir, "plugin.yaml"), StandardCharsets.UTF_8));
            Map<String, Object> mn = desc.get("mn") == null ? new HashMap<>() : (Map<String, Object>) desc.get("mn");
            List<String> jars = mn.get("jars") == null ? new ArrayList<>() : (List<String>) mn.get("jars");
            List<String> entityNames = mn.get("entities") == null ? new ArrayList<>() : (List<String>) mn.get("entities");

            List<URL> urls = new ArrayList<>();
            for (String jar : jars) {
                urls.add(new File(dir, jar).toURI().toURL());
            }
            p.classLoader = new URLClassLoader(urls.toArray(new URL[0]), getClass().getClassLoader());
            for (String jar : jars) {
                try (JarFile f = new JarFile(new File(dir, jar))) {
                    for (JarEntry e : Collections.list(f.entries())) {
                        if (e.getName().endsWith(".class")) {
                            String name = e.getName().substring(0, e.getName().length() - ".class".length()).replace('/', '.');
                            p.classes.add(Class.forName(name, false, p.classLoader));
                        }
                    }
                }
            }
            for (String name : entityNames) {
                p.entities.add(Class.forName(name, true, p.classLoader));
            }

            Map<String, File> files = new HashMap<>();
            File classesDir = new File(dir, "classes");
            if (classesDir.isDirectory()) {
                for (File f : FileUtils.listFiles(classesDir, null, true)) {
                    files.put(classesDir.toPath().relativize(f.toPath()).toString().replace(File.separatorChar, '/'), f);
                }
            }

            if (!p.entities.isEmpty()) {
                p.emf = FeaturePluginPersistenceUnits.build(id, p.classLoader, p.entities, dataSource(), platformJpaProperties());
            }

            p.context = springContext(p, files);
            p.view = new LoadedFeaturePlugin(id, p.classLoader, p.classes, files, p.entities);
            return p;
        } catch (Throwable t) {
            release(p);
            throw failure(id, t);
        }
    }

    @SuppressWarnings("unchecked")
    private GenericXmlApplicationContext springContext(Plugin p, Map<String, File> files) throws ClassNotFoundException {
        GenericXmlApplicationContext ctx = new GenericXmlApplicationContext();
        ctx.setParent(platform);
        ctx.setClassLoader(p.classLoader);
        ctx.getBeanFactory().setBeanClassLoader(p.classLoader);
        AnnotationConfigUtils.registerAnnotationConfigProcessors(ctx);
        files.entrySet().stream().filter(e -> e.getKey().startsWith(SPRING_FOLDER) && e.getKey().endsWith(".xml"))
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> ctx.load(new FileSystemResource(e.getValue())));

        // <zstack:plugin> registers an extension registry bean of its own in the plugin's context; its declared
        // extensions go to the platform's registry instead, and the bean must not shadow the platform's registry
        Map<String, List<PluginExtension>> declared = new HashMap<>();
        if (ctx.containsBeanDefinition(PluginRegistry.PLUGIN_REGISTRY_BEAN_NAME)) {
            PropertyValue pv = ctx.getBeanDefinition(PluginRegistry.PLUGIN_REGISTRY_BEAN_NAME).getPropertyValues()
                    .getPropertyValue(PluginRegistry.PLUGIN_REGISTRYIMPL_PLUGINS_FIELD_NAME);
            if (pv != null) {
                declared = (Map<String, List<PluginExtension>>) pv.getValue();
            }
            ctx.removeBeanDefinition(PluginRegistry.PLUGIN_REGISTRY_BEAN_NAME);
        }
        ctx.refresh();

        for (List<PluginExtension> exts : declared.values()) {
            for (PluginExtension ext : exts) {
                String bean = ext.getInstanceId() == null || ext.getInstanceId().isEmpty() ? ext.getBeanName() : ext.getInstanceId();
                ext.setInstance(ctx.getBean(bean));
                Class<?> iface = Class.forName(ext.getReferenceInterface(), false, p.classLoader);
                if (!iface.isInstance(ext.getInstance())) {
                    throw new CloudRuntimeException(String.format("bean %s is not an instance of %s", bean, iface.getName()));
                }
                p.extensions.add(ext);
            }
        }
        return ctx;
    }

    private void activate(Plugin p) {
        FeaturePluginClasses.add(p.classLoader);
        if (p.emf != null) {
            FeaturePluginPersistenceUnits.register(p.id, p.emf, dataSource(), p.entities);
            ((DatabaseFacadeImpl) dbf).addPluginEntities(p.entities);
            p.unitRegistered = true;
        }

        APIMessage.addPluginApiClasses(p.classes);
        p.apisAdded = true;
        p.rbac = RBAC.addDescriptions(p.classes.stream().filter(c -> RBACDescription.class.isAssignableFrom(c)
                && !c.isInterface() && !Modifier.isAbstract(c.getModifiers())).collect(Collectors.toList()));

        for (FeaturePluginRuntimeExtensionPoint ext : new ArrayList<>(pluginRgty.getExtensionList(FeaturePluginRuntimeExtensionPoint.class))) {
            ext.featurePluginLoaded(p.view);
            p.runtimeExtensions.add(ext);
        }

        ((PluginRegistryImpl) pluginRgty).addExtensions(p.extensions, p.classLoader);
        p.extensionsAdded = true;
        for (Object o : instancesOf(p, Service.class)) {
            bus.registerService((Service) o);
            p.services.add((Service) o);
        }
        for (Object o : instancesOf(p, Component.class)) {
            if (!((Component) o).start()) {
                throw new CloudRuntimeException(String.format("%s did not start", o.getClass().getName()));
            }
            p.components.add((Component) o);
        }
        if (nodeReady) {
            for (Object o : instancesOf(p, ManagementNodeReadyExtensionPoint.class)) {
                ((ManagementNodeReadyExtensionPoint) o).managementNodeReady();
            }
        }
    }

    private static List<Object> instancesOf(Plugin p, Class<?> iface) {
        List<Object> ret = new ArrayList<>();
        for (PluginExtension ext : p.extensions) {
            if (iface.getName().equals(ext.getReferenceInterface()) && !ret.contains(ext.getInstance())) {
                ret.add(ext.getInstance());
            }
        }
        return ret;
    }

    private static void quietly(String what, Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            logger.warn(String.format("failed to %s", what), t);
        }
    }

    /**
     * Undoes what activate registered, whatever fails on the way. REST routes and API dispatch go first.
     */
    private void deactivate(Plugin p) {
        List<FeaturePluginRuntimeExtensionPoint> exts = new ArrayList<>(p.runtimeExtensions);
        Collections.reverse(exts);
        for (FeaturePluginRuntimeExtensionPoint ext : exts) {
            quietly(String.format("drop feature plugin %s from %s", p.id, ext.getClass().getName()), () -> ext.featurePluginUnloading(p.view));
        }
        p.runtimeExtensions.clear();

        for (Service s : p.services) {
            quietly(String.format("unregister service %s", s.getId()), () -> bus.unregisterService(s));
        }
        drain(p);
        List<Component> components = new ArrayList<>(p.components);
        Collections.reverse(components);
        for (Component c : components) {
            quietly(String.format("stop %s", c.getClass().getName()), c::stop);
        }
        int cancelled = ((ThreadFacadeImpl) thdf).cancelTasksOf(p.classLoader);
        if (cancelled > 0) {
            logger.warn(String.format("feature plugin %s left %d periodic tasks running when it stopped, cancelled them", p.id, cancelled));
        }
        p.services.clear();
        p.components.clear();

        if (p.extensionsAdded) {
            quietly("drop extensions of feature plugin " + p.id, () -> ((PluginRegistryImpl) pluginRgty).removeExtensions(p.extensions, p.classLoader));
            p.extensionsAdded = false;
        }
        if (p.rbac != null) {
            RBAC.remove(p.rbac);
            p.rbac = null;
        }
        if (p.apisAdded) {
            APIMessage.removePluginApiClasses(p.classes);
            p.apisAdded = false;
        }
        if (p.unitRegistered) {
            ((DatabaseFacadeImpl) dbf).removePluginEntities(p.entities);
            FeaturePluginPersistenceUnits.unregister(p.id);
            p.unitRegistered = false;
        }
    }

    // waits for the messages the plugin's services are still handling
    private void drain(Plugin p) {
        Set<String> ids = p.services.stream().map(Service::getId).collect(Collectors.toSet());
        long deadline = System.currentTimeMillis() + DRAIN_MILLIS;
        while (!ids.isEmpty() && System.currentTimeMillis() < deadline) {
            Map<String, SyncTaskStatistic> stats = ((ThreadFacadeImpl) thdf).getSyncTaskStatistics();
            ids.removeIf(id -> {
                SyncTaskStatistic s = stats.get(id);
                return s == null || (s.getCurrentRunningThreadNum() == 0 && s.getPendingTaskNum() == 0);
            });
            if (!ids.isEmpty()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        if (!ids.isEmpty()) {
            logger.warn(String.format("services %s of feature plugin %s still handle messages after %d seconds, unloading it anyway",
                    ids, p.id, TimeUnit.MILLISECONDS.toSeconds(DRAIN_MILLIS)));
        }
    }

    /**
     * Closes what prepare made and drops the platform's cached references to the plugin's classes.
     */
    private void release(Plugin p) {
        if (p.context != null) {
            quietly("close the Spring context of feature plugin " + p.id, p.context::close);
        }
        if (p.emf != null) {
            quietly("close the persistence unit of feature plugin " + p.id, p.emf::close);
        }
        if (p.classLoader == null) {
            return;
        }

        ClassLoader cl = p.classLoader;
        FeaturePluginClasses.remove(cl);
        JSONObjectUtil.forgetTypes(cl);
        CloudBusGson.forgetTypes(cl);
        LogSafeGson.forgetTypes(cl);
        ((ThreadFacadeImpl) thdf).purgeTimers();
        CachedIntrospectionResults.clearClassLoader(cl);
        ReflectionUtils.clearCache();
        AnnotationUtils.clearCache();
        ResolvableType.clearCache();
        Introspector.flushCaches();
        unloaded.add(new Unloaded(p.id, cl, fullGcCount()));
        quietly("close the class loader of feature plugin " + p.id, () -> {
            try {
                p.classLoader.close();
            } catch (java.io.IOException e) {
                throw new CloudRuntimeException(e);
            }
        });
    }

    /**
     * Logs the class loaders of unloaded plugin versions that a full GC since their unload has not collected; something
     * the plugin used still refers to them.
     */
    private void reportKeptClassLoaders() {
        unloaded.removeIf(u -> u.classLoader.get() == null);
        long now = fullGcCount();
        List<String> kept = unloaded.stream().filter(u -> now > u.fullGcCount).map(u -> u.id).collect(Collectors.toList());
        if (!kept.isEmpty()) {
            logger.warn(String.format("%d class loaders of unloaded feature plugins are kept after a full GC: %s", kept.size(), kept));
        }
    }

    private static long fullGcCount() {
        long n = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            String name = gc.getName();
            if (name.contains("MarkSweep") || name.contains("Old") || name.contains("MSC")) {
                n += Math.max(0, gc.getCollectionCount());
            }
        }
        return n;
    }

    private DataSource dataSource() {
        return platform.getBean("DbFacadeDataSource", DataSource.class);
    }

    private Map<String, Object> platformJpaProperties() {
        return ((LocalContainerEntityManagerFactoryBean) platform.getBean("&entityManagerFactory")).getJpaPropertyMap();
    }
}
