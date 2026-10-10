package org.zstack.kvm.memory;

import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.aspectj.AnnotationBeanConfigurerAspect;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.wiring.BeanConfigurerSupport;
import org.springframework.beans.factory.wiring.BeanWiringInfo;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.aspectj.AnnotationTransactionAspect;
import org.zstack.core.aspect.EncryptColumnAspect;
import org.zstack.core.aspect.MessageSafeAspect;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.ConfigMutation;
import org.zstack.core.config.ConfigMutationContext;
import org.zstack.core.config.ConfigTransactionalMutationExtensionPoint;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigVO;
import org.zstack.core.config.GlobalConfigFacadeImpl;
import org.zstack.core.config.APIUpdateGlobalConfigMsg;
import org.zstack.core.config.APIUpdateGlobalConfigEvent;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.DatabaseFacadeImpl;
import org.zstack.core.db.EntityMetadata;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.vo.ResourceVO;
import org.zstack.resourceconfig.APIUpdateResourceConfigsEvent;
import org.zstack.resourceconfig.APIUpdateResourceConfigsMsg;
import org.zstack.resourceconfig.APIUpdateResourceConfigMsg;
import org.zstack.resourceconfig.APIDeleteResourceConfigMsg;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacadeImpl;
import org.zstack.resourceconfig.ResourceConfigStruct;
import org.zstack.resourceconfig.ResourceConfigVO;

import javax.persistence.EntityManager;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * End-to-end test: ResourceConfigFacade, its woven transaction,
 * DatabaseFacade and the real MemoryRepository fixture share one EMF.
 */
public class MemoryStandardConfigurationFrameworkTest {
    private Object repositoryFixture;
    private MemoryRepository repository;
    private SessionFactory factory;
    private EntityManager shared;
    private DatabaseFacadeImpl dbf;
    private JpaTransactionManager txManager;
    private PluginRegistry registry;
    private AtomicLong licenseDeadline;
    private List<APIEvent> events;
    private Object priorPlatformLoader, priorConfigurerSupport, beanConfigurerSupport;
    private Object priorTxManager, txAspect, encryptAspect, priorEncryptRegistry;
    private Field configurerSupportField, transactionManagerField, encryptRegistryField;
    private Object priorMessageBus, priorMessageErrors, messageAspect;
    private Field messageBusField, messageErrorField;
    private ResourceConfigFacadeImpl facade;
    private ResourceConfig enabled, zeroPages;
    private ConfigTransactionalMutationExtensionPoint postWriteFailure;

    @Before public void setUp() throws Exception {
        Class.forName("org.zstack.core.Platform");
        repositoryFixture = new MemoryRepositoryTest();
        invoke(repositoryFixture, "setup");
        factory = (SessionFactory) field(repositoryFixture, "factory");
        repository = (MemoryRepository) field(repositoryFixture, "repository");
        EntityManager seed = factory.createEntityManager();
        seed.getTransaction().begin();
        seed.persist(new ResourceVO(new Object[]{"host1", "host1", "HostVO"}));
        seed.getTransaction().commit(); seed.close();

        txManager = new JpaTransactionManager(factory);
        shared = SharedEntityManagerCreator.createSharedEntityManager(factory);
        dbf = new DatabaseFacadeImpl();
        set(DatabaseFacadeImpl.class, dbf, "entityManagerFactory", factory);
        set(DatabaseFacadeImpl.class, dbf, "entityManager", shared);
        invoke(dbf, "buildEntityInfo");
        installPlatformLoader();
        installWiringAndAspects();

        licenseDeadline = new AtomicLong(System.currentTimeMillis() + 60_000);
        registry = (PluginRegistry) Proxy.newProxyInstance(PluginRegistry.class.getClassLoader(),
                new Class<?>[]{PluginRegistry.class}, (p, m, a) -> {
                    if (m.getName().equals("getExtensionList") && a != null && a.length == 1
                            && a[0] == MemoryLicenseExtensionPoint.class) {
                        return Collections.singletonList((MemoryLicenseExtensionPoint) licenseDeadline::get);
                    }
                    return Collections.emptyList();
                });
        MemoryStandardConfigurationCoordinator coordinator = new MemoryStandardConfigurationCoordinator(repository, registry);
        enabled = resourceConfig(MemoryStandardField.KSM_ENABLED, coordinator);
        zeroPages = resourceConfig(MemoryStandardField.KSM_ZERO_PAGES, coordinator);
        events = new ArrayList<>();
        facade = new ResourceConfigFacadeImpl();
        Map<String, ResourceConfig> configs = new HashMap<>();
        configs.put(identity(MemoryStandardField.KSM_ENABLED), enabled);
        configs.put(identity(MemoryStandardField.KSM_ZERO_PAGES), zeroPages);
        set(ResourceConfigFacadeImpl.class, facade, "resourceConfigs", configs);
        set(ResourceConfigFacadeImpl.class, facade, "bus", cloudBus());
        set(ResourceConfigFacadeImpl.class, facade, "dbf", dbf);
        set(ResourceConfigFacadeImpl.class, facade, "evtf", eventFacade());
    }

    @After public void tearDown() throws Exception {
        if (repositoryFixture != null) invoke(repositoryFixture, "close");
        if (txAspect != null) ((AnnotationTransactionAspect) txAspect).setTransactionManager(
                (org.springframework.transaction.PlatformTransactionManager) priorTxManager);
        if (configurerSupportField != null) configurerSupportField.set(AnnotationBeanConfigurerAspect.aspectOf(), priorConfigurerSupport);
        if (encryptRegistryField != null) encryptRegistryField.set(encryptAspect, priorEncryptRegistry);
        if (messageBusField != null) messageBusField.set(messageAspect, priorMessageBus);
        if (messageErrorField != null) messageErrorField.set(messageAspect, priorMessageErrors);
        if (priorPlatformLoader != null) set(org.zstack.core.Platform.class, null, "loader", priorPlatformLoader);
    }

    @Test public void publicBulkWritesConfigRevisionAndTaskUsingOneEmf() {
        facade.handleMessage(bulk("true", "true"));
        assertNoApiError();
        assertBulkReplyValue(MemoryStandardField.KSM_ENABLED, "true");
        assertBulkReplyValue(MemoryStandardField.KSM_ZERO_PAGES, "true");
        assertEquals("true", configValue(MemoryStandardField.KSM_ENABLED));
        assertEquals("true", configValue(MemoryStandardField.KSM_ZERO_PAGES));
        assertEquals(1L, repository.getPolicy("Host", "host1").getRevision());
        assertTrue("real coordinator must create its task in the same backing EMF", taskCount() > 0);
    }

    @Test public void expiredLicenseRejectsBeforeAnyPersistentWrite() {
        licenseDeadline.set(System.currentTimeMillis() - 1);
        try {
            facade.handleMessage(bulk("true", "true"));
            fail("expired License must reject the public bulk request");
        } catch (org.zstack.header.errorcode.OperationFailureException expected) {
            assertEquals("MEMORY_LICENSE_UNAVAILABLE", expected.getErrorCode().getCode());
        }
        assertNull(configValue(MemoryStandardField.KSM_ENABLED));
        assertNull(configValue(MemoryStandardField.KSM_ZERO_PAGES));
        assertEquals(0L, repository.getPolicy("Host", "host1").getRevision());
        assertEquals(0L, taskCount());
    }

    @Test public void failureAfterRealCoordinatorWritesRollsBackConfigRevisionAndTasks() {
        java.util.concurrent.atomic.AtomicInteger afterCoordinator = new java.util.concurrent.atomic.AtomicInteger();
        postWriteFailure = new ConfigTransactionalMutationExtensionPoint() {
            @Override public void beforeMutations(EntityManager em, List<ConfigMutation> changes, ConfigMutationContext context) { }
            @Override public void afterMutations(EntityManager em, List<ConfigMutation> changes, ConfigMutationContext context) {
                assertTrue(em.createQuery("select count(t) from MemoryTaskVO t", Long.class).getSingleResult() > 0);
                afterCoordinator.incrementAndGet();
                throw new org.zstack.core.config.GlobalConfigException("forced failure after real coordinator");
            }
        };
        enabled.installConfigMutationExtension(postWriteFailure);
        zeroPages.installConfigMutationExtension(postWriteFailure);
        facade.handleMessage(bulk("true", "true"));
        assertApiError();
        assertEquals("the failure must occur after the real coordinator wrote its task", 1, afterCoordinator.get());
        assertNull(configValue(MemoryStandardField.KSM_ENABLED));
        assertNull(configValue(MemoryStandardField.KSM_ZERO_PAGES));
        assertEquals(0L, repository.getPolicy("Host", "host1").getRevision());
        assertEquals(0L, taskCount());
    }

    @Test public void publicSingleUpdateThenDeleteRestoresInheritedGlobalValue() {
        String inheritedBeforeOverride = repository.getPolicy("Host", "host1").getEffectivePolicy();
        APIUpdateResourceConfigMsg update = single("host1", "true");
        facade.handleMessage(update);
        assertEquals(1, events.size());
        assertNull(events.get(0).getError());
        assertEquals("true", configValue(MemoryStandardField.KSM_ENABLED));
        assertEquals(1L, repository.getPolicy("Host", "host1").getRevision());

        events.clear();
        APIDeleteResourceConfigMsg delete = new APIDeleteResourceConfigMsg();
        delete.setResourceUuid("host1"); delete.setCategory(MemoryStandardField.KSM_ENABLED.category());
        delete.setName(MemoryStandardField.KSM_ENABLED.configName());
        SessionInventory session = new SessionInventory(); session.setAccountUuid("test-account"); session.setUserUuid("test-user");
        delete.setSession(session);
        facade.handleMessage(delete);
        assertEquals(1, events.size()); assertNull(events.get(0).getError());
        assertNull(configValue(MemoryStandardField.KSM_ENABLED));
        assertEquals(2L, repository.getPolicy("Host", "host1").getRevision());
        assertEquals("delete must restore the exact inherited policy", inheritedBeforeOverride,
                repository.getPolicy("Host", "host1").getEffectivePolicy());
    }

    @Test public void legacyHostNoneMasksInheritedKsmWithoutSendingAToggleAndDeleteRestoresIt() throws Exception {
        EntityManager seed = factory.createEntityManager();
        seed.getTransaction().begin();
        GlobalConfigVO globalRow = seed.createQuery("select c from GlobalConfigVO c where c.category=:category and c.name=:name", GlobalConfigVO.class)
                .setParameter("category", MemoryStandardField.KSM_ENABLED.category())
                .setParameter("name", MemoryStandardField.KSM_ENABLED.configName()).getSingleResult();
        globalRow.setValue("true"); seed.getTransaction().commit(); seed.close();
        EntityManager compatSeed = factory.createEntityManager(); compatSeed.getTransaction().begin();
        MemoryPolicyVO compat = compatSeed.find(MemoryPolicyVO.class, "Host:host1");
        if (compat == null) { compat = new MemoryPolicyVO(); compat.setUuid("Host:host1"); compat.setScope("Host"); compat.setResourceUuid("host1"); }
        compat.setPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":true,\"zeroPagesEnabled\":true,\"pagesToScan\":777,\"sleepMillis\":23}}");
        compatSeed.merge(compat); compatSeed.getTransaction().commit(); compatSeed.close();

        facade.handleMessage(single("host1", "none"));
        assertNoApiError();
        MemoryPolicyInventory unmanaged = repository.getPolicy("Host", "host1");
        assertNull("none must not be coerced to enabled=false or inherit true", MemoryStandardConfigCodec.get(unmanaged.getEffectivePolicy(), "ksm.enabled"));
        assertEquals("Host:host1", unmanaged.getFieldSources().get("ksm.enabled"));
        assertEquals("Unmanaged", unmanaged.getFieldModes().get("ksm.enabled"));
        assertEquals(Boolean.TRUE, MemoryStandardConfigCodec.get(unmanaged.getEffectivePolicy(), "ksm.zeroPagesEnabled"));
        assertEquals(Long.valueOf(777), MemoryStandardConfigCodec.get(unmanaged.getEffectivePolicy(), "ksm.pagesToScan"));
        assertEquals(Long.valueOf(23), MemoryStandardConfigCodec.get(unmanaged.getEffectivePolicy(), "ksm.sleepMillis"));
        MemoryTaskVO task = latestTaskForHost();
        assertNotNull("a compatible live host should produce an Agent task", task);
        assertNull("unmanaged KSM must not send true/false/null", MemoryStandardConfigCodec.get(task.getPolicy(), "ksm.enabled"));

        events.clear();
        APIDeleteResourceConfigMsg delete = new APIDeleteResourceConfigMsg();
        delete.setResourceUuid("host1"); delete.setCategory(MemoryStandardField.KSM_ENABLED.category());
        delete.setName(MemoryStandardField.KSM_ENABLED.configName());
        SessionInventory session = new SessionInventory(); session.setAccountUuid("test-account"); session.setUserUuid("test-user"); delete.setSession(session);
        facade.handleMessage(delete);
        assertNoApiError();
        MemoryPolicyInventory inherited = repository.getPolicy("Host", "host1");
        assertEquals(Boolean.TRUE, MemoryStandardConfigCodec.get(inherited.getEffectivePolicy(), "ksm.enabled"));
        assertFalse(inherited.getFieldModes().containsKey("ksm.enabled"));
    }

    @Test public void ordinaryNonParticipantCallbacksRemainSynchronousAndPropagate() throws Exception {
        ResourceConfig ordinary = resourceConfig(MemoryStandardField.KSM_ENABLED, null);
        ordinary.installUpdateExtension((config, uuid, type, oldValue, newValue) -> {
            throw new IllegalStateException("ordinary callback failure");
        });
        try {
            ordinary.updateValue("host1", "true");
            fail("nonparticipant callback exception must propagate synchronously");
        } catch (IllegalStateException expected) {
            assertEquals("ordinary callback failure", expected.getMessage());
        }
        assertNull("transaction must roll back when ordinary callback throws", configValue(MemoryStandardField.KSM_ENABLED));

        EntityManager seed = factory.createEntityManager(); seed.getTransaction().begin();
        ResourceConfigVO row = new ResourceConfigVO();
        row.setUuid(UUID.randomUUID().toString().replace("-", ""));
        row.setCategory(MemoryStandardField.KSM_ENABLED.category()); row.setName(MemoryStandardField.KSM_ENABLED.configName());
        row.setValue("false"); row.setResourceUuid("host1"); row.setResourceType("HostVO"); seed.persist(row);
        seed.getTransaction().commit(); seed.close();
        ordinary.installDeleteExtension((config, uuid, type, originValue) -> {
            throw new IllegalStateException("ordinary delete callback failure");
        });
        try {
            ordinary.deleteValue("host1");
            fail("nonparticipant delete callback exception must propagate synchronously");
        } catch (IllegalStateException expected) {
            assertEquals("ordinary delete callback failure", expected.getMessage());
        }
        // Ordinary delete uses the platform's pre-existing SQL helper boundary;
        // N06 restores callback propagation, not a new rollback guarantee there.
        assertNull(configValue(MemoryStandardField.KSM_ENABLED));
    }

    @Test public void globalSingleUpdateFailureAfterCoordinatorRollsBackEverything() throws Exception {
        java.util.concurrent.atomic.AtomicInteger afterCoordinator = new java.util.concurrent.atomic.AtomicInteger();
        ConfigTransactionalMutationExtensionPoint failAfterCoordinator = new ConfigTransactionalMutationExtensionPoint() {
            @Override public void beforeMutations(EntityManager em, List<ConfigMutation> changes, ConfigMutationContext context) { }
            @Override public void afterMutations(EntityManager em, List<ConfigMutation> changes, ConfigMutationContext context) {
                assertTrue(em.createQuery("select count(t) from MemoryTaskVO t", Long.class).getSingleResult() > 0);
                afterCoordinator.incrementAndGet();
                throw new org.zstack.core.config.GlobalConfigException("global post-write failure");
            }
        };
        GlobalConfig global = new GlobalConfig(MemoryStandardField.KSM_ENABLED.category(), MemoryStandardField.KSM_ENABLED.configName());
        set(GlobalConfig.class, global, "value", "none"); set(GlobalConfig.class, global, "defaultValue", "none");
        set(GlobalConfig.class, global, "dbf", dbf); set(GlobalConfig.class, global, "evtf", eventFacade());
        set(GlobalConfig.class, global, "transactionManager", txManager);
        global.installConfigMutationExtension(new MemoryStandardConfigurationCoordinator(repository, registry));
        global.installConfigMutationExtension(failAfterCoordinator);
        GlobalConfigFacadeImpl globalFacade = new GlobalConfigFacadeImpl();
        Map<String, GlobalConfig> globalConfigs = new HashMap<>(); globalConfigs.put(global.getIdentity(), global);
        set(GlobalConfigFacadeImpl.class, globalFacade, "allConfig", globalConfigs);
        set(GlobalConfigFacadeImpl.class, globalFacade, "dbf", dbf); set(GlobalConfigFacadeImpl.class, globalFacade, "evtf", eventFacade());
        set(GlobalConfigFacadeImpl.class, globalFacade, "bus", cloudBus()); set(GlobalConfigFacadeImpl.class, globalFacade, "pluginRgty", registry);
        APIUpdateGlobalConfigMsg request = new APIUpdateGlobalConfigMsg();
        request.setCategory(global.getCategory()); request.setName(global.getName()); request.setValue("true");
        SessionInventory session = new SessionInventory(); session.setAccountUuid("test-account"); session.setUserUuid("test-user"); request.setSession(session);
        globalFacade.handleMessage(request);
        assertEquals(1, events.size()); assertNotNull(events.get(0).getError());
        assertEquals("the failure must occur after the real coordinator wrote its task", 1, afterCoordinator.get());
        assertEquals(0L, repository.getPolicy("Global", "global").getRevision());
        assertEquals(0L, taskCount());
        EntityManager em = factory.createEntityManager();
        try {
            String stored = em.createQuery("select c.value from GlobalConfigVO c where c.category=:category and c.name=:name", String.class)
                    .setParameter("category", MemoryStandardField.KSM_ENABLED.category())
                    .setParameter("name", MemoryStandardField.KSM_ENABLED.configName()).getSingleResult();
            assertEquals("none", stored);
        } finally { em.close(); }
    }

    private ResourceConfig resourceConfig(MemoryStandardField field, ConfigTransactionalMutationExtensionPoint coordinator) throws Exception {
        ResourceConfig rc = new ResourceConfig();
        GlobalConfig gc = new GlobalConfig(field.category(), field.configName());
        set(GlobalConfig.class, gc, "value", "false"); set(GlobalConfig.class, gc, "defaultValue", "false");
        set(ResourceConfig.class, rc, "globalConfig", gc); set(ResourceConfig.class, rc, "dbf", dbf);
        set(ResourceConfig.class, rc, "evtf", eventFacade()); set(ResourceConfig.class, rc, "transactionManager", txManager);
        set(ResourceConfig.class, rc, "resourceClasses", Collections.singletonList(HostVO.class));
        invoke(rc, "initResourceConfigNodes");
        if (coordinator != null) { rc.installConfigMutationExtension(coordinator); }
        return rc;
    }

    private MemoryTaskVO latestTaskForHost() {
        EntityManager em = factory.createEntityManager();
        try {
            List<MemoryTaskVO> tasks = em.createQuery("select t from MemoryTaskVO t where t.hostUuid='host1' order by t.createDate desc", MemoryTaskVO.class)
                    .setMaxResults(1).getResultList();
            return tasks.isEmpty() ? null : tasks.get(0);
        } finally { em.close(); }
    }

    private APIUpdateResourceConfigsMsg bulk(String enabledValue, String zeroValue) {
        APIUpdateResourceConfigsMsg msg = new APIUpdateResourceConfigsMsg();
        msg.setResourceUuid("host1");
        SessionInventory session = new SessionInventory(); session.setAccountUuid("test-account"); session.setUserUuid("test-user");
        msg.setSession(session);
        msg.setResourceConfigs(Arrays.asList(ao(MemoryStandardField.KSM_ENABLED, enabledValue),
                ao(MemoryStandardField.KSM_ZERO_PAGES, zeroValue)));
        return msg;
    }
    private APIUpdateResourceConfigMsg single(String resourceUuid, String value) {
        APIUpdateResourceConfigMsg msg = new APIUpdateResourceConfigMsg();
        msg.setResourceUuid(resourceUuid); msg.setCategory(MemoryStandardField.KSM_ENABLED.category());
        msg.setName(MemoryStandardField.KSM_ENABLED.configName()); msg.setValue(value);
        SessionInventory session = new SessionInventory(); session.setAccountUuid("test-account"); session.setUserUuid("test-user");
        msg.setSession(session); return msg;
    }
    private APIUpdateResourceConfigsMsg.ResourceConfigAO ao(MemoryStandardField field, String value) {
        APIUpdateResourceConfigsMsg.ResourceConfigAO ao = new APIUpdateResourceConfigsMsg.ResourceConfigAO();
        ao.setCategory(field.category()); ao.setName(field.configName()); ao.setValue(value); return ao;
    }
    private String identity(MemoryStandardField field) { return GlobalConfig.produceIdentity(field.category(), field.configName()); }
    private String configValue(MemoryStandardField field) {
        EntityManager em = factory.createEntityManager();
        try {
            List<String> rows = em.createQuery("select c.value from ResourceConfigVO c where c.resourceUuid='host1' and c.category=:category and c.name=:name", String.class)
                    .setParameter("category", field.category()).setParameter("name", field.configName()).getResultList();
            return rows.isEmpty() ? null : rows.get(0);
        } finally { em.close(); }
    }
    private long taskCount() {
        EntityManager em = factory.createEntityManager();
        try { return em.createQuery("select count(t) from MemoryTaskVO t", Long.class).getSingleResult(); }
        finally { em.close(); }
    }
    private void assertApiError() { assertEquals(1, events.size()); assertNotNull(events.get(0).getError()); }
    private void assertNoApiError() { assertEquals(1, events.size()); assertNull(events.get(0).getError()); }
    private void assertBulkReplyValue(MemoryStandardField field, String expected) {
        assertTrue(events.get(0) instanceof APIUpdateResourceConfigsEvent);
        List<ResourceConfigStruct> inventories = ((APIUpdateResourceConfigsEvent) events.get(0)).getInventories();
        assertNotNull(inventories);
        ResourceConfigStruct result = inventories.stream().filter(it -> it.getName().equals(field.configName())).findFirst()
                .orElseThrow(() -> new AssertionError("missing bulk response entry for " + field.configName()));
        assertEquals(expected, result.getValue());
        assertNotNull(result.getEffectiveConfigs());
        assertFalse(result.getEffectiveConfigs().isEmpty());
        assertEquals(expected, result.getEffectiveConfigs().get(0).getValue());
    }

    private EventFacade eventFacade() {
        return (EventFacade) Proxy.newProxyInstance(EventFacade.class.getClassLoader(), new Class<?>[]{EventFacade.class}, (p,m,a) -> null);
    }
    private CloudBus cloudBus() {
        return (CloudBus) Proxy.newProxyInstance(CloudBus.class.getClassLoader(), new Class<?>[]{CloudBus.class}, (p,m,a) -> {
            if (m.getName().equals("publish") && a != null && a.length == 1 && a[0] instanceof APIEvent) events.add((APIEvent) a[0]);
            if (m.getReturnType() == String.class) return "local:resource-config";
            return defaultValue(m.getReturnType());
        });
    }
    private void installPlatformLoader() throws Exception {
        priorPlatformLoader = get(org.zstack.core.Platform.class, null, "loader");
        ErrorFacade errors = (ErrorFacade) Proxy.newProxyInstance(ErrorFacade.class.getClassLoader(), new Class<?>[]{ErrorFacade.class},
                (p,m,a) -> m.getReturnType() == ErrorCode.class ? new ErrorCode("MEMORY_TEST", "fixture") : defaultValue(m.getReturnType()));
        DatabaseFacade dbfProxy = (DatabaseFacade) Proxy.newProxyInstance(DatabaseFacade.class.getClassLoader(), new Class<?>[]{DatabaseFacade.class},
                (p,m,a) -> m.getName().equals("getEntityManager") ? shared : m.getName().equals("getCriteriaBuilder")
                        ? factory.getCriteriaBuilder() : defaultValue(m.getReturnType()));
        ComponentLoader loader = (ComponentLoader) Proxy.newProxyInstance(ComponentLoader.class.getClassLoader(), new Class<?>[]{ComponentLoader.class},
                (p,m,a) -> m.getName().equals("getComponent") && a != null && a.length == 1 && a[0] == DatabaseFacade.class
                        ? dbfProxy : m.getName().equals("getComponent") && a != null && a.length == 1 && a[0] == ErrorFacade.class
                        ? errors : defaultValue(m.getReturnType()));
        set(org.zstack.core.Platform.class, null, "loader", loader);
    }
    private void installWiringAndAspects() throws Exception {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        AutowiredAnnotationBeanPostProcessor autowired = new AutowiredAnnotationBeanPostProcessor(); autowired.setBeanFactory(beanFactory); beanFactory.addBeanPostProcessor(autowired);
        beanFactory.registerSingleton("databaseFacadeImpl", dbf); beanFactory.registerSingleton("eventFacade", eventFacade());
        beanFactory.registerSingleton("transactionManager", txManager); beanFactory.registerSingleton("cloudBus", cloudBus());
        AnnotationBeanConfigurerAspect configAspect = AnnotationBeanConfigurerAspect.aspectOf();
        configurerSupportField = AnnotationBeanConfigurerAspect.class.getDeclaredField("beanConfigurerSupport"); configurerSupportField.setAccessible(true);
        priorConfigurerSupport = configurerSupportField.get(configAspect); beanConfigurerSupport = new BeanConfigurerSupport();
        configurerSupportField.set(configAspect, beanConfigurerSupport); configAspect.setBeanFactory(beanFactory);
        ((BeanConfigurerSupport) beanConfigurerSupport).setBeanWiringInfoResolver(bean -> new BeanWiringInfo(BeanWiringInfo.AUTOWIRE_BY_TYPE, false)); configAspect.afterPropertiesSet();
        AnnotationTransactionAspect tx = AnnotationTransactionAspect.aspectOf(); txAspect = tx;
        transactionManagerField = org.springframework.transaction.interceptor.TransactionAspectSupport.class.getDeclaredField("transactionManager"); transactionManagerField.setAccessible(true);
        priorTxManager = transactionManagerField.get(tx); tx.setTransactionManager(txManager);
        encryptAspect = EncryptColumnAspect.aspectOf(); encryptRegistryField = EncryptColumnAspect.class.getDeclaredField("pluginRegistry"); encryptRegistryField.setAccessible(true);
        priorEncryptRegistry = encryptRegistryField.get(encryptAspect);
        encryptRegistryField.set(encryptAspect, Proxy.newProxyInstance(PluginRegistry.class.getClassLoader(), new Class<?>[]{PluginRegistry.class}, (p,m,a) -> Collections.emptyList()));
        Method metadataInit = EntityMetadata.class.getDeclaredMethod("staticInit"); metadataInit.setAccessible(true); metadataInit.invoke(null);
        messageAspect = MessageSafeAspect.aspectOf(); messageBusField = MessageSafeAspect.class.getDeclaredField("bus"); messageBusField.setAccessible(true);
        messageErrorField = MessageSafeAspect.class.getDeclaredField("errf"); messageErrorField.setAccessible(true);
        priorMessageBus = messageBusField.get(messageAspect); priorMessageErrors = messageErrorField.get(messageAspect);
        messageBusField.set(messageAspect, cloudBus()); messageErrorField.set(messageAspect,
                Proxy.newProxyInstance(ErrorFacade.class.getClassLoader(), new Class<?>[]{ErrorFacade.class}, (p,m,a) -> defaultValue(m.getReturnType())));
    }
    private static Object defaultValue(Class<?> c) { if (c==boolean.class)return false; if(c==int.class)return 0; if(c==long.class)return 0L; if(c==short.class)return (short)0; if(c==byte.class)return (byte)0; if(c==float.class)return 0f; if(c==double.class)return 0d; if(c==char.class)return (char)0; return null; }
    private static Object field(Object target, String name) throws Exception { return get(target.getClass(), target, name); }
    private static Object get(Class<?> type, Object target, String name) throws Exception { Field f=type.getDeclaredField(name); f.setAccessible(true); return f.get(target); }
    private static void set(Class<?> type, Object target, String name, Object value) throws Exception { Field f=type.getDeclaredField(name); f.setAccessible(true); f.set(target,value); }
    private static Object invoke(Object target, String name) throws Exception { Method m=target.getClass().getDeclaredMethod(name); m.setAccessible(true); return m.invoke(target); }
}
