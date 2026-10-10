package org.zstack.kvm.memory;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.Event;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.vo.ResourceVO;
import org.zstack.resourceconfig.APIUpdateResourceConfigsEvent;
import org.zstack.resourceconfig.APIUpdateResourceConfigsMsg;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigApiInterceptor;
import org.zstack.resourceconfig.ResourceConfigDeleteValidatorExtensionPoint;
import org.zstack.resourceconfig.ResourceConfigVO;
import org.zstack.resourceconfig.ResourceConfigFacadeImpl;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.resourceconfig.ResourceConfigTransactionalMutationExtensionPoint;
import org.zstack.resourceconfig.ResourceConfigCanonicalEvents;

import javax.persistence.EntityManager;
import javax.persistence.Entity;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Integration-level ownership guards for the real resource-config entry points.
 *
 * The delete test uses a real H2 EntityManager and Platform Q/SQL lookup path;
 * only the physical delete is observed through a subclass so a veto can prove
 * that it happened before deletion.  The bulk test invokes handleMessage(), not
 * handleMemoryControlledBulk(), and uses a CloudBus proxy only as event
 * transport/observation.  It does not prove the concurrent repository lock.
 */
public class MemoryResourceConfigOwnershipTest {
    private SessionFactory factory;
    private EntityManager em;
    private DatabaseFacade dbf;
    private Object previousPlatformLoader;
    private Object encryptAspect;
    private Field encryptRegistryField;
    private Object previousEncryptRegistry;
    private Object messageSafeAspect;
    private Field messageBusField;
    private Field messageErrorField;
    private Object previousMessageBus;
    private Object previousMessageErrors;
    private Object transactionAspect;
    private Field transactionManagerField;
    private Object previousTransactionManager;

    @Before
    public void setUp() throws Exception {
        Class<?> aspect = Class.forName("org.zstack.core.aspect.EncryptColumnAspect");
        encryptAspect = aspect.getMethod("aspectOf").invoke(null);
        encryptRegistryField = aspect.getDeclaredField("pluginRegistry");
        encryptRegistryField.setAccessible(true);
        previousEncryptRegistry = encryptRegistryField.get(encryptAspect);
        encryptRegistryField.set(encryptAspect, Proxy.newProxyInstance(PluginRegistry.class.getClassLoader(),
                new Class<?>[]{PluginRegistry.class}, (p, m, a) -> Collections.emptyList()));
        java.lang.reflect.Method metadataInit = org.zstack.core.db.EntityMetadata.class
                .getDeclaredMethod("staticInit");
        metadataInit.setAccessible(true);
        metadataInit.invoke(null);

        factory = new Configuration()
                .addAnnotatedClass(ResourceVO.class)
                .addAnnotatedClass(ResourceConfigVO.class)
                .addAnnotatedClass(TestMutationVO.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:rc-owner-" + UUID.randomUUID() + ";MODE=MySQL;NON_KEYWORDS=VALUE")
                .setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.show_sql", "false")
                .buildSessionFactory();
        em = factory.createEntityManager();
        dbf = (DatabaseFacade) Proxy.newProxyInstance(DatabaseFacade.class.getClassLoader(),
                new Class<?>[]{DatabaseFacade.class}, (p, m, a) -> {
                    if (m.getName().equals("getEntityManager")) return em;
                    if (m.getName().equals("getCriteriaBuilder")) return factory.getCriteriaBuilder();
                    if (m.getReturnType().equals(boolean.class)) return false;
                    if (m.getReturnType().equals(long.class)) return 0L;
                    return null;
                });
        previousPlatformLoader = readStatic(Platform.class, "loader");
        ErrorFacade errors = errorFacade();
        ComponentLoader loader = (ComponentLoader) Proxy.newProxyInstance(ComponentLoader.class.getClassLoader(),
                new Class<?>[]{ComponentLoader.class}, (p, m, a) -> {
                    if (m.getName().equals("getComponent") && a != null && a.length == 1
                            && a[0] == DatabaseFacade.class) return dbf;
                    if (m.getName().equals("getComponent") && a != null && a.length == 1
                            && a[0] == ErrorFacade.class) return errors;
                    if (m.getName().equals("getComponentNoExceptionWhenNotExisting")) return null;
                    if (m.getName().equals("hasComponent")) return false;
                    return null;
                });
        writeStatic(Platform.class, "loader", loader);

        em.getTransaction().begin();
        ResourceVO resource = new ResourceVO();
        resource.setUuid("host-owner-test");
        setField(ResourceVO.class, resource, "resourceType", ResourceVO.class.getSimpleName());
        em.persist(resource);
        ResourceConfigVO config = new ResourceConfigVO();
        config.setUuid("rc-owner-test"); config.setResourceUuid(resource.getUuid());
        config.setResourceType(ResourceVO.class.getSimpleName()); config.setCategory("kvm");
        config.setName("host.ksm"); config.setValue("true");
        em.persist(config);
        ResourceConfigVO ordinary = new ResourceConfigVO();
        ordinary.setUuid("rc-ordinary-test"); ordinary.setResourceUuid(resource.getUuid());
        ordinary.setResourceType(ResourceVO.class.getSimpleName()); ordinary.setCategory("memory");
        ordinary.setName("ordinary"); ordinary.setValue("before");
        em.persist(ordinary);
        em.getTransaction().commit();
    }

    @After
    public void tearDown() throws Exception {
        if (em != null && em.isOpen()) em.close();
        if (factory != null) factory.close();
        writeStatic(Platform.class, "loader", previousPlatformLoader);
        if (encryptRegistryField != null) encryptRegistryField.set(encryptAspect, previousEncryptRegistry);
        if (messageBusField != null) messageBusField.set(messageSafeAspect, previousMessageBus);
        if (messageErrorField != null) messageErrorField.set(messageSafeAspect, previousMessageErrors);
        if (transactionManagerField != null) transactionManagerField.set(transactionAspect, previousTransactionManager);
    }

    @Test
    public void deleteValidatorRunsBeforePhysicalDeleteAndVetoKeepsRow() throws Exception {
        GlobalConfig global = global("kvm", "host.ksm", "false");
        ObservedResourceConfig config = new ObservedResourceConfig(global);
        wireResourceType(config);
        config.installDeleteValidatorExtension((resourceUuid, oldValue) -> {
            throw new GlobalConfigException("MEMORY_CONTROLLER_CONFLICT: managed host");
        });

        em.getTransaction().begin();
        try {
            try {
                config.deleteValue("host-owner-test");
                fail("ownership validator must veto delete");
            } catch (GlobalConfigException expected) {
                assertTrue(expected.getMessage().contains("MEMORY_CONTROLLER_CONFLICT"));
            }
            assertNotNull(em.find(ResourceConfigVO.class, "rc-owner-test"));
            assertFalse(config.physicalDeleteCalled);
            em.getTransaction().rollback();
        } catch (Throwable t) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw t;
        }
    }

    @Test
    public void bulkHandlerPrevalidatesKsmAfterOrdinaryItemWithoutMutationOrSuccess() throws Exception {
        ResourceConfigFacadeImpl facade = new ResourceConfigFacadeImpl();
        List<Event> published = new ArrayList<>();
        CloudBus bus = (CloudBus) Proxy.newProxyInstance(CloudBus.class.getClassLoader(),
                new Class<?>[]{CloudBus.class}, (p, m, a) -> {
                    if (m.getName().equals("publish") && a != null && a.length == 1) {
                        published.add((Event) a[0]);
                    }
                    if (m.getName().equals("makeLocalServiceId")) return "local:resource-config";
                    return null;
                });
        installMessageSafeAspect(bus);
        setField(ResourceConfigFacadeImpl.class, facade, "bus", bus);

        CountingResourceConfig ordinary = new CountingResourceConfig();
        RejectingResourceConfig ksm = new RejectingResourceConfig();
        // The actual atomic handler now builds mutation descriptors after pure
        // validation; use bound configurations rather than null-global doubles.
        setField(ResourceConfig.class, ordinary, "globalConfig", global("memory", "ordinary", "before"));
        setField(ResourceConfig.class, ksm, "globalConfig", global("kvm", "host.ksm", "false"));
        wireResourceType(ordinary);
        wireResourceType(ksm);
        Map<String, ResourceConfig> configs = new HashMap<>();
        configs.put(GlobalConfig.produceIdentity("memory", "ordinary"), ordinary);
        configs.put(GlobalConfig.produceIdentity("kvm", "host.ksm"), ksm);
        setField(ResourceConfigFacadeImpl.class, facade, "resourceConfigs", configs);

        APIUpdateResourceConfigsMsg msg = new APIUpdateResourceConfigsMsg();
        msg.setResourceUuid("host-owner-test");
        APIUpdateResourceConfigsMsg.ResourceConfigAO normal = new APIUpdateResourceConfigsMsg.ResourceConfigAO();
        normal.setCategory("memory"); normal.setName("ordinary"); normal.setValue("new");
        APIUpdateResourceConfigsMsg.ResourceConfigAO conflict = new APIUpdateResourceConfigsMsg.ResourceConfigAO();
        conflict.setCategory("kvm"); conflict.setName("host.ksm"); conflict.setValue("false");
        msg.setResourceConfigs(Arrays.asList(normal, conflict));

        facade.handleMessage(msg);

        assertEquals("one rejected event", 1, published.size());
        assertTrue(published.get(0) instanceof APIUpdateResourceConfigsEvent);
        assertNotNull(((APIUpdateResourceConfigsEvent) published.get(0)).getError());
        assertTrue(((APIUpdateResourceConfigsEvent) published.get(0)).getError().getDetails()
                .contains("MEMORY_CONTROLLER_CONFLICT"));
        assertEquals(0, ordinary.updateCalls);
        assertEquals(1, ksm.validationCalls);
        assertEquals("no success event", 0, published.stream()
                .filter(it -> it instanceof APIUpdateResourceConfigsEvent
                        && ((APIUpdateResourceConfigsEvent) it).getError() == null).count());
    }

    @Test
    public void actualInterceptorValidationIsReadOnlyAndWovenResourceConfigDefersEventUntilCommit() throws Exception {
        org.springframework.orm.jpa.JpaTransactionManager manager =
                new org.springframework.orm.jpa.JpaTransactionManager(factory);
        org.springframework.transaction.aspectj.AnnotationTransactionAspect aspect =
                org.springframework.transaction.aspectj.AnnotationTransactionAspect.aspectOf();
        transactionAspect = aspect;
        transactionManagerField = org.springframework.transaction.interceptor.TransactionAspectSupport.class
                .getDeclaredField("transactionManager");
        transactionManagerField.setAccessible(true);
        previousTransactionManager = transactionManagerField.get(aspect);
        aspect.setTransactionManager(manager);

        EntityManager shared = org.springframework.orm.jpa.SharedEntityManagerCreator.createSharedEntityManager(factory);
        dbf = (DatabaseFacade) Proxy.newProxyInstance(DatabaseFacade.class.getClassLoader(),
                new Class<?>[]{DatabaseFacade.class}, (p, m, a) -> {
                    if (m.getName().equals("getEntityManager")) return shared;
                    if (m.getName().equals("getCriteriaBuilder")) return factory.getCriteriaBuilder();
                    if (m.getReturnType().equals(boolean.class)) return false;
                    if (m.getReturnType().equals(long.class)) return 0L;
                    return null;
                });

        GlobalConfig global = global("kvm", "host.ksm", "false");
        ResourceConfig config = new ResourceConfig();
        wireResourceType(config);
        setField(ResourceConfig.class, config, "dbf", dbf);
        setField(ResourceConfig.class, config, "globalConfig", global);
        setField(ResourceConfig.class, config, "transactionManager", manager);
        final boolean[] eventSawCommittedWrites = {false};
        org.zstack.core.cloudbus.EventFacade events = (org.zstack.core.cloudbus.EventFacade)
                Proxy.newProxyInstance(org.zstack.core.cloudbus.EventFacade.class.getClassLoader(),
                        new Class<?>[]{org.zstack.core.cloudbus.EventFacade.class}, (p, m, a) -> {
                            if (m.getName().equals("fire")) {
                                EntityManager observer = factory.createEntityManager();
                                try {
                                    String value = observer.createQuery("select c.value from ResourceConfigVO c " +
                                                    "where c.resourceUuid = :uuid and c.name = :name", String.class)
                                            .setParameter("uuid", "host-owner-test").setParameter("name", "host.ksm").getSingleResult();
                                    Long count = observer.createQuery("select count(m) from TestMutationVO m", Long.class).getSingleResult();
                                    eventSawCommittedWrites[0] = "false".equals(value) && count == 2L
                                            && a != null && a.length == 2
                                            && "ResourceVO".equals(((ResourceConfigCanonicalEvents.UpdateEvent) a[1]).getResourceType());
                                } finally { observer.close(); }
                            }
                            return null;
                        });
        setField(ResourceConfig.class, config, "evtf", events);
        config.installUpdateExtension((ignored, uuid, type, oldValue, newValue) -> {
            writeWithRequiredTransaction("after-commit-write");
        });
        config.installUpdateExtension((ignored, uuid, type, oldValue, newValue) -> {
            throw new IllegalStateException("post-commit callback failure must be isolated");
        });
        config.installTransactionalMutationExtension(new ResourceConfigTransactionalMutationExtensionPoint() {
            @Override public boolean requiresAtomicBulkTransaction() { return true; }
            @Override public void beforeUpdate(EntityManager entityManager, ResourceConfig ignored,
                    String resourceUuid, String resourceType, String oldValue, String newValue) {
                TestMutationVO marker = new TestMutationVO(); marker.uuid = "committed"; entityManager.persist(marker);
            }
            @Override public void beforeDelete(EntityManager entityManager, ResourceConfig ignored,
                    String resourceUuid, String resourceType, String oldValue) { }
        });

        GlobalConfigFacade gcf = (GlobalConfigFacade) Proxy.newProxyInstance(GlobalConfigFacade.class.getClassLoader(),
                new Class<?>[]{GlobalConfigFacade.class}, (p, m, a) -> m.getName().equals("getAllConfig")
                        ? Collections.singletonMap(global.getIdentity(), global) : null);
        ResourceConfigFacade rcf = (ResourceConfigFacade) Proxy.newProxyInstance(ResourceConfigFacade.class.getClassLoader(),
                new Class<?>[]{ResourceConfigFacade.class}, (p, m, a) -> m.getName().equals("getResourceConfig") ? config : null);
        ResourceConfigApiInterceptor interceptor = new ResourceConfigApiInterceptor();
        setField(ResourceConfigApiInterceptor.class, interceptor, "gcf", gcf);
        setField(ResourceConfigApiInterceptor.class, interceptor, "rcf", rcf);
        APIUpdateResourceConfigsMsg request = new APIUpdateResourceConfigsMsg();
        request.setResourceUuid("host-owner-test");
        APIUpdateResourceConfigsMsg.ResourceConfigAO item = new APIUpdateResourceConfigsMsg.ResourceConfigAO();
        item.setCategory("kvm"); item.setName("host.ksm"); item.setValue("false");
        request.setResourceConfigs(Collections.singletonList(item));

        interceptor.intercept(request);
        assertEquals("actual API pre-validation cannot execute transaction mutation hooks", 0L, countMutations());
        assertEquals("actual API pre-validation must not change configuration", "true", readConfigValue());

        APIUpdateResourceConfigsMsg rejected = new APIUpdateResourceConfigsMsg();
        rejected.setResourceUuid("host-owner-test");
        APIUpdateResourceConfigsMsg.ResourceConfigAO invalid = new APIUpdateResourceConfigsMsg.ResourceConfigAO();
        invalid.setCategory("missing"); invalid.setName("not-bound"); invalid.setValue("x");
        rejected.setResourceConfigs(Arrays.asList(item, invalid));
        try {
            interceptor.intercept(rejected);
            fail("a later invalid configuration must reject the whole API request");
        } catch (ApiMessageInterceptionException expected) {
            assertEquals(0L, countMutations());
            assertEquals("true", readConfigValue());
        }

        config.updateValue("host-owner-test", "false");
        assertEquals("same-transaction hook and committed callback write both persist", 2L, countMutations());
        assertEquals("false", readConfigValue());
        assertTrue("canonical event must observe committed config and coordinated mutation", eventSawCommittedWrites[0]);

        ResourceConfig ordinary = new ResourceConfig();
        wireResourceType(ordinary);
        setField(ResourceConfig.class, ordinary, "dbf", dbf);
        setField(ResourceConfig.class, ordinary, "globalConfig", global("memory", "ordinary", "default"));
        setField(ResourceConfig.class, ordinary, "transactionManager", manager);
        final boolean[] ordinaryEventSawCommit = {false};
        final boolean[] ordinaryEventInTransaction = {false};
        setField(ResourceConfig.class, ordinary, "evtf", Proxy.newProxyInstance(
                org.zstack.core.cloudbus.EventFacade.class.getClassLoader(),
                new Class<?>[]{org.zstack.core.cloudbus.EventFacade.class}, (p, m, a) -> {
                    if (m.getName().equals("fire")) {
                        ordinaryEventInTransaction[0] = org.springframework.transaction.support.TransactionSynchronizationManager
                                .isActualTransactionActive();
                        EntityManager observer = factory.createEntityManager();
                        try {
                            String value = observer.createQuery("select c.value from ResourceConfigVO c " +
                                            "where c.resourceUuid = :uuid and c.name = :name", String.class)
                                    .setParameter("uuid", "host-owner-test").setParameter("name", "ordinary").getSingleResult();
                            Long count = observer.createQuery("select count(m) from TestMutationVO m", Long.class).getSingleResult();
                            ordinaryEventSawCommit[0] = "after".equals(value) && count == 3L;
                        } finally { observer.close(); }
                    }
                    return null;
                }));
        ordinary.installUpdateExtension((ignored, uuid, type, oldValue, newValue) ->
                writeWithRequiredTransaction("ordinary-after-commit-write"));
        ordinary.updateValue("host-owner-test", "after");
        assertEquals("after", readOrdinaryConfigValue());
        assertTrue("ordinary nonparticipant keeps synchronous event timing", ordinaryEventInTransaction[0]);
        assertFalse("independent reader must not see the ordinary update before commit", ordinaryEventSawCommit[0]);
        assertEquals("ordinary callback joins its caller's active transaction", 3L, countMutations());
    }

    @Test
    public void wovenResourceConfigRollbackSuppressesCoordinatorWritesAndCanonicalEvent() throws Exception {
        org.springframework.orm.jpa.JpaTransactionManager manager =
                new org.springframework.orm.jpa.JpaTransactionManager(factory);
        org.springframework.transaction.aspectj.AnnotationTransactionAspect aspect =
                org.springframework.transaction.aspectj.AnnotationTransactionAspect.aspectOf();
        transactionAspect = aspect;
        transactionManagerField = org.springframework.transaction.interceptor.TransactionAspectSupport.class
                .getDeclaredField("transactionManager");
        transactionManagerField.setAccessible(true);
        previousTransactionManager = transactionManagerField.get(aspect);
        aspect.setTransactionManager(manager);
        EntityManager shared = org.springframework.orm.jpa.SharedEntityManagerCreator.createSharedEntityManager(factory);
        dbf = (DatabaseFacade) Proxy.newProxyInstance(DatabaseFacade.class.getClassLoader(),
                new Class<?>[]{DatabaseFacade.class}, (p, m, a) -> {
                    if (m.getName().equals("getEntityManager")) return shared;
                    if (m.getName().equals("getCriteriaBuilder")) return factory.getCriteriaBuilder();
                    return null;
                });

        ResourceConfig config = new ResourceConfig();
        wireResourceType(config);
        setField(ResourceConfig.class, config, "dbf", dbf);
        setField(ResourceConfig.class, config, "globalConfig", global("kvm", "host.ksm", "false"));
        setField(ResourceConfig.class, config, "transactionManager", manager);
        final int[] events = {0};
        setField(ResourceConfig.class, config, "evtf", Proxy.newProxyInstance(
                org.zstack.core.cloudbus.EventFacade.class.getClassLoader(),
                new Class<?>[]{org.zstack.core.cloudbus.EventFacade.class}, (p, m, a) -> {
                    if (m.getName().equals("fire")) events[0]++;
                    return null;
                }));
        config.installTransactionalMutationExtension(new ResourceConfigTransactionalMutationExtensionPoint() {
            @Override public boolean requiresAtomicBulkTransaction() { return true; }
            @Override public void beforeUpdate(EntityManager entityManager, ResourceConfig ignored,
                    String resourceUuid, String resourceType, String oldValue, String newValue) {
                TestMutationVO marker = new TestMutationVO(); marker.uuid = "rolled-back"; entityManager.persist(marker);
                throw new IllegalStateException("reject after same-transaction mutation");
            }
            @Override public void beforeDelete(EntityManager entityManager, ResourceConfig ignored,
                    String resourceUuid, String resourceType, String oldValue) { }
        });

        try {
            config.updateValue("host-owner-test", "false");
            fail("the same-transaction mutation veto must abort the woven write");
        } catch (IllegalStateException expected) {
            assertEquals("reject after same-transaction mutation", expected.getMessage());
        }
        assertEquals("config value must survive rollback", "true", readConfigValue());
        assertEquals("coordinator mutation must survive rollback neither", 0L, countMutations());
        assertEquals("rolled-back changes must not publish canonical events", 0, events[0]);
    }

    @Test
    public void wovenResourceConfigDeletePublishesOnlyAfterCommittedDelete() throws Exception {
        org.springframework.orm.jpa.JpaTransactionManager manager =
                new org.springframework.orm.jpa.JpaTransactionManager(factory);
        org.springframework.transaction.aspectj.AnnotationTransactionAspect aspect =
                org.springframework.transaction.aspectj.AnnotationTransactionAspect.aspectOf();
        transactionAspect = aspect;
        transactionManagerField = org.springframework.transaction.interceptor.TransactionAspectSupport.class
                .getDeclaredField("transactionManager");
        transactionManagerField.setAccessible(true);
        previousTransactionManager = transactionManagerField.get(aspect);
        aspect.setTransactionManager(manager);
        EntityManager shared = org.springframework.orm.jpa.SharedEntityManagerCreator.createSharedEntityManager(factory);
        dbf = (DatabaseFacade) Proxy.newProxyInstance(DatabaseFacade.class.getClassLoader(),
                new Class<?>[]{DatabaseFacade.class}, (p, m, a) -> {
                    if (m.getName().equals("getEntityManager")) return shared;
                    if (m.getName().equals("getCriteriaBuilder")) return factory.getCriteriaBuilder();
                    return null;
                });

        ResourceConfig config = new ResourceConfig() {
            @Override protected void deleteInDb(String resourceUuid) {
                dbf.getEntityManager().createQuery("delete from ResourceConfigVO c where c.resourceUuid = :uuid and c.name = :name")
                        .setParameter("uuid", resourceUuid).setParameter("name", "host.ksm").executeUpdate();
            }
        };
        wireResourceType(config);
        setField(ResourceConfig.class, config, "dbf", dbf);
        setField(ResourceConfig.class, config, "globalConfig", global("kvm", "host.ksm", "false"));
        setField(ResourceConfig.class, config, "transactionManager", manager);
        final boolean[] eventSawCommittedDelete = {false};
        setField(ResourceConfig.class, config, "evtf", Proxy.newProxyInstance(
                org.zstack.core.cloudbus.EventFacade.class.getClassLoader(),
                new Class<?>[]{org.zstack.core.cloudbus.EventFacade.class}, (p, m, a) -> {
                    if (m.getName().equals("fire")) {
                        EntityManager observer = factory.createEntityManager();
                        try {
                            Long count = observer.createQuery("select count(c) from ResourceConfigVO c " +
                                            "where c.resourceUuid = :uuid and c.name = :name", Long.class)
                                    .setParameter("uuid", "host-owner-test").setParameter("name", "host.ksm").getSingleResult();
                            Long writes = observer.createQuery("select count(m) from TestMutationVO m where m.uuid = :uuid", Long.class)
                                    .setParameter("uuid", "after-delete-commit").getSingleResult();
                            eventSawCommittedDelete[0] = count == 0L && writes == 1L;
                        } finally { observer.close(); }
                    }
                    return null;
                }));
        config.installTransactionalMutationExtension(new ResourceConfigTransactionalMutationExtensionPoint() {
            @Override public boolean requiresAtomicBulkTransaction() { return true; }
            @Override public void beforeUpdate(EntityManager em, ResourceConfig ignored,
                    String resourceUuid, String resourceType, String oldValue, String newValue) { }
            @Override public void beforeDelete(EntityManager em, ResourceConfig ignored,
                    String resourceUuid, String resourceType, String oldValue) { }
        });
        config.installDeleteExtension((ignored, uuid, type, oldValue) -> writeWithRequiredTransaction("after-delete-commit"));

        config.deleteValue("host-owner-test");
        assertTrue("delete event must observe both the committed deletion and a callback SQLBatch write", eventSawCommittedDelete[0]);
    }

    @Entity(name = "TestMutationVO") @javax.persistence.Table(name = "TestResourceConfigMutation")
    public static class TestMutationVO {
        @javax.persistence.Id public String uuid;
    }

    private long countMutations() {
        EntityManager observer = factory.createEntityManager();
        try { return observer.createQuery("select count(m) from TestMutationVO m", Long.class).getSingleResult(); }
        finally { observer.close(); }
    }

    private void writeWithRequiredTransaction(String uuid) {
        org.zstack.core.db.SQLBatchWithReturn<Void> batch = new org.zstack.core.db.SQLBatchWithReturn<Void>() {
            @Override protected Void scripts() {
                TestMutationVO marker = new TestMutationVO(); marker.uuid = uuid;
                databaseFacade.getEntityManager().persist(marker);
                return null;
            }
        };
        try { setField(org.zstack.core.db.SQLBatchWithReturn.class, batch, "databaseFacade", dbf); }
        catch (Exception e) { throw new AssertionError(e); }
        batch.execute();
    }

    private String readConfigValue() {
        EntityManager observer = factory.createEntityManager();
        try { return observer.createQuery("select c.value from ResourceConfigVO c where c.resourceUuid = :uuid and c.name = :name", String.class)
                .setParameter("uuid", "host-owner-test").setParameter("name", "host.ksm").getSingleResult(); }
        finally { observer.close(); }
    }

    private String readOrdinaryConfigValue() {
        EntityManager observer = factory.createEntityManager();
        try { return observer.createQuery("select c.value from ResourceConfigVO c where c.resourceUuid = :uuid and c.name = :name", String.class)
                .setParameter("uuid", "host-owner-test").setParameter("name", "ordinary").getSingleResult(); }
        finally { observer.close(); }
    }

    private void wireResourceType(ResourceConfig config) throws Exception {
        Map<String, Object> getters = new HashMap<>();
        getters.put(ResourceVO.class.getSimpleName(), new Object());
        setField(ResourceConfig.class, config, "configGetter", getters);
        setField(ResourceConfig.class, config, "dbf", dbf);
    }

    private static GlobalConfig global(String category, String name, String value) throws Exception {
        GlobalConfig global = new GlobalConfig(category, name);
        setField(GlobalConfig.class, global, "value", value);
        setField(GlobalConfig.class, global, "defaultValue", value);
        return global;
    }

    private static class ObservedResourceConfig extends ResourceConfig {
        boolean physicalDeleteCalled;
        ObservedResourceConfig(GlobalConfig global) { setFieldUnchecked(ResourceConfig.class, this, "globalConfig", global); }
        @Override protected void deleteInDb(String resourceUuid) { physicalDeleteCalled = true; }
    }

    private static class CountingResourceConfig extends ResourceConfig {
        int updateCalls;
        @Override public void validateNewValue(String resourceUuid, String value) { }
        @Override public void updateValue(String resourceUuid, String value) { updateCalls++; }
    }

    private static class RejectingResourceConfig extends ResourceConfig {
        int validationCalls;
        @Override public boolean requiresAtomicBulkTransaction() { return true; }
        @Override public void validateNewValue(String resourceUuid, String value) {
            validationCalls++;
            throw new GlobalConfigException("MEMORY_CONTROLLER_CONFLICT: managed host");
        }
    }

    private static Object readStatic(Class<?> type, String name) throws Exception {
        Field f = type.getDeclaredField(name); f.setAccessible(true); return f.get(null);
    }
    private static void writeStatic(Class<?> type, String name, Object value) throws Exception {
        Field f = type.getDeclaredField(name); f.setAccessible(true); f.set(null, value);
    }
    private static void setField(Class<?> type, Object target, String name, Object value) throws Exception {
        Field f = type.getDeclaredField(name); f.setAccessible(true); f.set(target, value);
    }
    private static void setFieldUnchecked(Class<?> type, Object target, String name, Object value) {
        try { setField(type, target, name, value); } catch (Exception e) { throw new AssertionError(e); }
    }

    private void installMessageSafeAspect(CloudBus bus) throws Exception {
        Class<?> aspectType = Class.forName("org.zstack.core.aspect.MessageSafeAspect");
        messageSafeAspect = aspectType.getMethod("aspectOf").invoke(null);
        messageBusField = aspectType.getDeclaredField("bus"); messageBusField.setAccessible(true);
        messageErrorField = aspectType.getDeclaredField("errf"); messageErrorField.setAccessible(true);
        previousMessageBus = messageBusField.get(messageSafeAspect);
        previousMessageErrors = messageErrorField.get(messageSafeAspect);
        messageBusField.set(messageSafeAspect, bus);
        messageErrorField.set(messageSafeAspect, errorFacade());
    }

    private static ErrorFacade errorFacade() {
        return (ErrorFacade) Proxy.newProxyInstance(ErrorFacade.class.getClassLoader(),
                new Class<?>[]{ErrorFacade.class}, (p, m, a) -> {
                    if (m.getReturnType() == ErrorCode.class) {
                        ErrorCode error = new ErrorCode("TEST", "test");
                        error.setDetails(a != null && a.length > 1 ? String.valueOf(a[1]) : "");
                        return error;
                    }
                    return null;
                });
    }
}
