package org.zstack.core.db;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.Utils;
import org.zstack.utils.data.JpqlEntityNames;
import org.zstack.utils.logging.CLogger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.AbstractQuery;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Root;
import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The persistence units of feature plugins loaded at runtime. The platform's unit is built once at start and cannot
 * take new entities, so each plugin has a unit of its own on the platform's data source. {@link DatabaseFacadeImpl}
 * hands out an EntityManager that sends each call to the unit that maps its entities, so Q, SQL and dbf work on plugin
 * entities unchanged. Inside a platform transaction a plugin unit uses the transaction's JDBC connection, so plugin and
 * platform changes commit or roll back together.
 */
public class FeaturePluginPersistenceUnits {
    private static final CLogger logger = Utils.getLogger(FeaturePluginPersistenceUnits.class);
    private static final Object PLATFORM = new Object();
    private static final int MAX_CACHED_STATEMENTS = 10000;

    static class Unit {
        final String pluginId;
        final EntityManagerFactory emf;
        final DataSource dataSource;
        final EntityManager shared;
        final Set<Class<?>> entities;

        Unit(String pluginId, EntityManagerFactory emf, DataSource dataSource, Collection<Class<?>> entities) {
            this.pluginId = pluginId;
            this.emf = emf;
            this.dataSource = dataSource;
            this.shared = SharedEntityManagerCreator.createSharedEntityManager(emf);
            this.entities = new HashSet<>(entities);
        }
    }

    private static final Map<String, Unit> units = new ConcurrentHashMap<>();
    private static volatile Map<Class<?>, Unit> byClass = Collections.emptyMap();
    private static volatile Map<String, Unit> byName = Collections.emptyMap();
    private static final Map<String, Object> unitOfStatement = new ConcurrentHashMap<>();
    private static volatile EntityManager platform;

    /**
     * Builds the unit of a plugin with the platform unit's settings, except hibernate-search.
     */
    public static EntityManagerFactory build(String pluginId, ClassLoader cl, List<Class<?>> entities, DataSource ds,
                                             Map<String, Object> platformJpaProperties) {
        LocalContainerEntityManagerFactoryBean f = new LocalContainerEntityManagerFactoryBean();
        f.setPersistenceUnitName("featurePlugin." + pluginId);
        f.setDataSource(ds);
        // no persistence.xml and no META-INF/orm.xml: the one in the core jar names a class that no longer exists
        f.setPersistenceXmlLocation("classpath*:META-INF/feature-plugin-unit-none.xml");
        f.setPackagesToScan();
        f.setMappingResources();
        f.setPersistenceUnitPostProcessors(unit -> {
            entities.forEach(e -> unit.addManagedClassName(e.getName()));
            unit.setExcludeUnlistedClasses(true);
        });
        f.setResourceLoader(new DefaultResourceLoader(cl));
        f.setBeanClassLoader(cl);
        f.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        f.setJpaDialect(new HibernateJpaDialect());

        Map<String, Object> props = new HashMap<>();
        platformJpaProperties.forEach((k, v) -> {
            if (!k.startsWith("hibernate.search.")) {
                props.put(k, v);
            }
        });
        props.put("hibernate.search.autoregister_listeners", "false");
        // inside a platform transaction the session runs on its connection, the platform commits it
        props.put("hibernate.allow_update_outside_transaction", "true");
        f.setJpaPropertyMap(props);

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        try {
            f.afterPropertiesSet();
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
        return f.getObject();
    }

    public static synchronized void register(String pluginId, EntityManagerFactory emf, DataSource ds, Collection<Class<?>> entities) {
        units.put(pluginId, new Unit(pluginId, emf, ds, entities));
        rebuild();
    }

    public static synchronized void unregister(String pluginId) {
        units.remove(pluginId);
        rebuild();
    }

    private static void rebuild() {
        Map<Class<?>, Unit> c = new HashMap<>();
        Map<String, Unit> n = new HashMap<>();
        for (Unit u : units.values()) {
            for (Class<?> e : u.entities) {
                c.put(e, u);
                n.put(e.getSimpleName(), u);
            }
        }
        byClass = c;
        byName = n;
        unitOfStatement.clear();
    }

    static boolean isEmpty() {
        return units.isEmpty();
    }

    static Unit unitOf(Class<?> clz) {
        Map<Class<?>, Unit> m = byClass;
        for (Class<?> c = clz; c != null && c != Object.class; c = c.getSuperclass()) {
            Unit u = m.get(c);
            if (u != null) {
                return u;
            }
        }
        return null;
    }

    static Unit unitOfStatement(String jpql) {
        Object u = unitOfStatement.get(jpql);
        if (u == null) {
            Map<String, Unit> m = byName;
            Set<Object> found = new HashSet<>();
            for (String name : JpqlEntityNames.of(jpql)) {
                Unit unit = m.get(name);
                found.add(unit == null ? PLATFORM : unit);
            }
            if (found.size() > 1) {
                throw new CloudRuntimeException(String.format("a JPQL statement cannot use the entities of a feature" +
                        " plugin together with those of the platform or another plugin: %s", jpql));
            }
            u = found.isEmpty() ? PLATFORM : found.iterator().next();
            if (unitOfStatement.size() < MAX_CACHED_STATEMENTS) {
                unitOfStatement.put(jpql, u);
            }
        }
        return u == PLATFORM ? null : (Unit) u;
    }

    static Unit unitOfCriteria(Object criteria) {
        if (criteria instanceof AbstractQuery) {
            for (Root<?> r : ((AbstractQuery<?>) criteria).getRoots()) {
                Unit u = unitOf(r.getJavaType());
                if (u != null) {
                    return u;
                }
            }
            return null;
        }
        if (criteria instanceof CriteriaUpdate) {
            return unitOf(((CriteriaUpdate<?>) criteria).getRoot().getJavaType());
        }
        if (criteria instanceof CriteriaDelete) {
            return unitOf(((CriteriaDelete<?>) criteria).getRoot().getJavaType());
        }
        return null;
    }

    static EntityManager entityManager(Unit u) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return u.shared;
        }
        EntityManagerHolder h = (EntityManagerHolder) TransactionSynchronizationManager.getResource(u.emf);
        if (h != null) {
            return h.getEntityManager();
        }

        // the connection the platform transaction manager bound for this transaction
        Connection con = DataSourceUtils.getConnection(u.dataSource);
        Session session = u.emf.unwrap(SessionFactory.class).withOptions().connection(con).openSession();
        EntityManagerHolder holder = new EntityManagerHolder(session);
        TransactionSynchronizationManager.bindResource(u.emf, holder);
        TransactionSynchronizationManager.registerSynchronization(new UnitSynchronization(u, holder, con));
        return session;
    }

    private static class UnitSynchronization extends TransactionSynchronizationAdapter {
        private final Unit unit;
        private final EntityManagerHolder holder;
        private final Connection con;

        UnitSynchronization(Unit unit, EntityManagerHolder holder, Connection con) {
            this.unit = unit;
            this.holder = holder;
            this.con = con;
        }

        @Override
        public void suspend() {
            TransactionSynchronizationManager.unbindResourceIfPossible(unit.emf);
        }

        @Override
        public void resume() {
            TransactionSynchronizationManager.bindResource(unit.emf, holder);
        }

        @Override
        public void beforeCommit(boolean readOnly) {
            if (readOnly) {
                return;
            }
            // platform rows first, plugin rows may refer to them
            if (platform != null) {
                platform.flush();
            }
            holder.getEntityManager().flush();
        }

        @Override
        public void afterCompletion(int status) {
            try {
                holder.getEntityManager().close();
            } catch (Throwable t) {
                logger.warn(String.format("failed to close the session of feature plugin %s", unit.pluginId), t);
            } finally {
                TransactionSynchronizationManager.unbindResourceIfPossible(unit.emf);
                DataSourceUtils.releaseConnection(con, unit.dataSource);
            }
        }
    }

    private static void flushPluginUnits() {
        for (Unit u : units.values()) {
            EntityManagerHolder h = (EntityManagerHolder) TransactionSynchronizationManager.getResource(u.emf);
            if (h != null) {
                h.getEntityManager().flush();
            }
        }
    }

    private static Unit route(Method m, Object[] args) {
        if (args == null || args.length == 0 || args[0] == null) {
            return null;
        }
        Object a = args[0];
        switch (m.getName()) {
            case "createQuery":
                return a instanceof String ? unitOfStatement((String) a) : unitOfCriteria(a);
            case "persist":
            case "merge":
            case "remove":
            case "refresh":
            case "detach":
            case "contains":
            case "lock":
            case "getLockMode":
                return unitOf(a.getClass());
            case "find":
            case "getReference":
                return a instanceof Class ? unitOf((Class<?>) a) : null;
            default:
                return null;
        }
    }

    /**
     * @return an EntityManager that sends each call to the unit mapping its entities, the platform's otherwise
     */
    static EntityManager routing(EntityManager platformEntityManager) {
        platform = platformEntityManager;
        return (EntityManager) Proxy.newProxyInstance(EntityManager.class.getClassLoader(), new Class[]{EntityManager.class},
                (proxy, m, args) -> {
                    if (m.getDeclaringClass() == Object.class) {
                        return m.invoke(platformEntityManager, args);
                    }
                    Unit u = route(m, args);
                    if (u == null && "flush".equals(m.getName())) {
                        flushPluginUnits();
                    }
                    try {
                        return m.invoke(u == null ? platformEntityManager : entityManager(u), args);
                    } catch (InvocationTargetException e) {
                        throw e.getTargetException();
                    }
                });
    }
}
