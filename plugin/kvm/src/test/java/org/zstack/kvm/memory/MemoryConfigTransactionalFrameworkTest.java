package org.zstack.kvm.memory;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.aspectj.AnnotationBeanConfigurerAspect;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.wiring.BeanConfigurerSupport;
import org.springframework.beans.factory.wiring.BeanWiringInfo;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.aspectj.AnnotationTransactionAspect;
import org.zstack.core.aspect.EncryptColumnAspect;
import org.zstack.core.aspect.MessageSafeAspect;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.ConfigMutation;
import org.zstack.core.config.ConfigMutationContext;
import org.zstack.core.config.ConfigTransactionalMutationExtensionPoint;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.config.GlobalConfigFacadeImpl;
import org.zstack.core.config.GlobalConfigVO;
import org.zstack.core.config.APIUpdateGlobalConfigMsg;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.EntityMetadata;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.APIEvent;
import org.zstack.header.vo.ResourceVO;
import org.zstack.resourceconfig.APIUpdateResourceConfigEvent;
import org.zstack.resourceconfig.APIUpdateResourceConfigMsg;
import org.zstack.resourceconfig.APIUpdateResourceConfigsEvent;
import org.zstack.resourceconfig.APIUpdateResourceConfigsMsg;
import org.zstack.resourceconfig.APIDeleteResourceConfigMsg;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigApiInterceptor;
import org.zstack.resourceconfig.ResourceConfigCanonicalEvents;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.resourceconfig.ResourceConfigFacadeImpl;
import org.zstack.resourceconfig.ResourceConfigVO;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Woven-framework acceptance for the generic config mutation hooks.
 * The regular reactor must weave the configuration/SQL classes. These tests
 * invoke the real interceptor and facade rather than simulating transactions.
 */
public class MemoryConfigTransactionalFrameworkTest {
    private static final String RESOURCE = "a01-host";
    private static final String ALPHA = "a01-alpha";
    private static final String BETA = "a01-beta";
    private static final String ORDINARY = "a01-ordinary";
    private static final String CATEGORY = "memory";

    @Entity(name = "A01MutationMarker") @javax.persistence.Table(name = "A01MutationMarker")
    public static class Marker {
        @Id public String uuid;
        public String requestId;
        public String phase;
    }

    private SessionFactory factory;
    private EntityManager shared;
    private DatabaseFacade dbf;
    private org.zstack.core.db.DatabaseFacadeImpl dbfImpl;
    private JpaTransactionManager txManager;
    private Object previousPlatformLoader;
    private Object beanConfigurerSupport;
    private Object priorConfigurerSupport;
    private Field configurerSupportField;
    private Object transactionAspect;
    private Field transactionManagerField;
    private Object previousTransactionManager;
    private Object messageSafeAspect;
    private Field messageBusField;
    private Field messageErrorField;
    private Object previousMessageBus;
    private Object previousMessageErrors;
    private Object encryptAspect;
    private Field encryptRegistryField;
    private Object previousEncryptRegistry;
    private final List<APIEvent> apiEvents = new ArrayList<>();
    private final List<String> canonicalEvents = new ArrayList<>();
    private final Map<String, EventCallback> callbacks = new HashMap<>();
    private volatile boolean observerSawCommittedState;
    private volatile boolean ordinaryCallbackSawActiveTransaction;
    private volatile String expectedAlpha = "old-alpha";
    private volatile String expectedBeta = "old-beta";
    private volatile String expectedOrdinary = "old-ordinary";
    private volatile long expectedMarkers;
    private volatile String expectedGlobal;
    private volatile boolean failAfter;
    private List<ResourceConfig> configs;
    private SharedParticipant participant;
    private ResourceConfigFacadeImpl facade;

    @Before
    public void setUp() throws Exception {
        // Complete the normal Platform bootstrap before installing the narrow
        // test bean factory, so package-wide @StaticInit classes are not
        // mistaken for missing management-node beans.
        Class.forName("org.zstack.core.Platform");
        prepareEntityMetadata();

        factory = new Configuration().addAnnotatedClass(ResourceVO.class)
                .addAnnotatedClass(ResourceConfigVO.class).addAnnotatedClass(GlobalConfigVO.class)
                .addAnnotatedClass(Marker.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:a01_" + UUID.randomUUID()
                        + ";MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.show_sql", "false").buildSessionFactory();
        txManager = new JpaTransactionManager(factory);
        shared = SharedEntityManagerCreator.createSharedEntityManager(factory);
        dbf = databaseFacade();

        EntityManager seed = factory.createEntityManager();
        seed.getTransaction().begin();
        ResourceVO resource = new ResourceVO();
        resource.setUuid(RESOURCE);
        setField(ResourceVO.class, resource, "resourceType", ResourceVO.class.getSimpleName());
        seed.persist(resource);
        seed.persist(configRow(ALPHA, CATEGORY, "alpha", "old-alpha"));
        seed.persist(configRow(BETA, CATEGORY, "beta", "old-beta"));
        seed.persist(configRow(ORDINARY, "compute", "ordinary", "old-ordinary"));
        GlobalConfigVO globalRow = new GlobalConfigVO();
        globalRow.setCategory(CATEGORY); globalRow.setName("global");
        globalRow.setDefaultValue("old-global"); globalRow.setValue("old-global"); seed.persist(globalRow);
        seed.getTransaction().commit(); seed.close();

        installPlatformLoader();
        installDatabaseFacadeImpl();
        installAspectBeanFactory();
        installTransactionAspect();
        installMessageSafeAspect();

        configs = new ArrayList<>();
        participant = new SharedParticipant();
        configs.add(config(CATEGORY, "alpha", "old-alpha", true));
        configs.add(config(CATEGORY, "beta", "old-beta", true));
        configs.add(config("compute", "ordinary", "old-ordinary", false));
        for (ResourceConfig config : configs) {
            String name = ((GlobalConfig) getFieldUnchecked(ResourceConfig.class, config, "globalConfig")).getName();
            if (name.equals("alpha") || name.equals("beta")) {
                config.installConfigMutationExtension(participant);
            }
        }
        facade = new ResourceConfigFacadeImpl();
        setField(ResourceConfigFacadeImpl.class, facade, "resourceConfigs", mapConfigs(configs));
        setField(ResourceConfigFacadeImpl.class, facade, "bus", bus());
    }

    @After
    public void tearDown() throws Exception {
        if (factory != null) factory.close();
        restoreAspectState();
        if (previousPlatformLoader != null) setField(org.zstack.core.Platform.class, null, "loader", previousPlatformLoader);
    }

    @Test
    public void interceptorIsReadOnlyAndBulkFacadeRunsSharedHookOnceInsideWovenTransaction() throws Exception {
        APIUpdateResourceConfigsMsg request = bulk("new-alpha", "new-beta");
        expectedAlpha = "new-alpha"; expectedBeta = "new-beta"; expectedMarkers = 4;
        ResourceConfigApiInterceptor interceptor = interceptor();
        interceptor.intercept(request);
        assertEquals(0, participant.beforeCalls.get());
        assertEquals(0, participant.afterCalls.get());
        assertEquals("old-alpha", configValue("alpha"));
        assertEquals("old-beta", configValue("beta"));
        assertEquals(0, markerCount());

        facade.handleMessage(request);

        assertEquals("one batch participant before", 1, participant.beforeCalls.get());
        assertEquals("one batch participant after", 1, participant.afterCalls.get());
        assertEquals(2, participant.lastChanges.size());
        assertFalse(participant.lastContext.isInternal());
        assertEquals(request.getId(), participant.lastContext.getRequestId());
        assertEquals("new-alpha", configValue("alpha"));
        assertEquals("new-beta", configValue("beta"));
        assertEquals("before/after journal rows for each field commit with config writes", 4, markerCount());
        assertTrue("canonical events must run after commit and see the complete batch", observerSawCommittedState);
        assertEquals("two canonical field events after commit", 2, canonicalEvents.size());
        assertEquals(1, apiEvents.size());
        assertTrue(apiEvents.get(0) instanceof APIUpdateResourceConfigsEvent);
        assertNull(apiEvents.get(0).getError());
    }

    @Test
    public void mutationFailureRollsBackEveryRowAndSuppressesCanonicalEvents() throws Exception {
        failAfter = true;
        facade.handleMessage(bulk("should-rollback-a", "should-rollback-b"));

        assertEquals(1, participant.beforeCalls.get());
        assertEquals(1, participant.afterCalls.get());
        assertEquals("old-alpha", configValue("alpha"));
        assertEquals("old-beta", configValue("beta"));
        assertEquals("ledger writes from both hook phases roll back", 0, markerCount());
        assertEquals("no canonical update escapes rollback", 0, canonicalEvents.size());
        assertEquals(1, apiEvents.size());
        assertNotNull(apiEvents.get(0).getError());
    }

    @Test
    public void facadeSingleUpdateAndDeleteUseSharedHooksAndAfterCommitReadback() throws Exception {
        APIUpdateResourceConfigMsg update = new APIUpdateResourceConfigMsg();
        expectedAlpha = "single-new"; expectedMarkers = 2;
        update.setCategory(CATEGORY); update.setName("alpha"); update.setResourceUuid(RESOURCE);
        update.setValue("single-new"); update.setSession(session());
        facade.handleMessage(update);
        assertEquals(1, participant.beforeCalls.get());
        assertEquals(1, participant.afterCalls.get());
        assertEquals("single-new", configValue("alpha"));
        assertEquals(2, markerCount());
        assertTrue(observerSawCommittedState);
        assertEquals(1, canonicalEvents.size());
        assertTrue(apiEvents.get(0) instanceof APIUpdateResourceConfigEvent);
        assertNull(apiEvents.get(0).getError());

        ResourceConfig alpha = configs.get(0);
        invokeInstallEventTrigger(alpha);
        int beforeRemote = participant.beforeCalls.get();
        int afterRemote = participant.afterCalls.get();
        ResourceConfigCanonicalEvents.UpdateEvent remote = new ResourceConfigCanonicalEvents.UpdateEvent();
        remote.setResourceUuid(RESOURCE); remote.setResourceType(ResourceVO.class.getSimpleName());
        remote.setOldValue("old-alpha");
        EventCallback callback = callbacks.values().iterator().next();
        Method callbackRun = EventCallback.class.getDeclaredMethod("run", Map.class, Object.class);
        callbackRun.setAccessible(true);
        callbackRun.invoke(callback, Collections.singletonMap("nodeUuid", "remote-management-node"), remote);
        assertEquals("remote canonical replay does not re-enter mutation hook", beforeRemote, participant.beforeCalls.get());
        assertEquals(afterRemote, participant.afterCalls.get());

        APIDeleteResourceConfigMsg delete = new APIDeleteResourceConfigMsg();
        expectedAlpha = null; expectedMarkers = 4;
        delete.setCategory(CATEGORY); delete.setName("alpha"); delete.setResourceUuid(RESOURCE); delete.setSession(session());
        facade.handleMessage(delete);
        assertEquals(2, participant.beforeCalls.get());
        assertEquals(2, participant.afterCalls.get());
        assertNull("resource override removed", configRowValue("alpha"));
        assertEquals("delete before/after markers committed", 4, markerCount());
        assertTrue("delete canonical event sees absent row and committed hook data", observerSawCommittedState);
    }

    @Test
    public void globalFacadeCommitsConfigurationAndHooksBeforeCallbacks() throws Exception {
        GlobalConfig config = global(CATEGORY, "global", "old-global");
        config.installConfigMutationExtension(participant);
        expectedGlobal = "new-global"; expectedMarkers = 2;
        AtomicInteger localNotifications = new AtomicInteger();
        config.installLocalUpdateExtension((oldValue, newValue) -> {
            assertEquals("new-global", globalValue());
            assertEquals(2, markerCount());
            localNotifications.incrementAndGet();
        });
        APIUpdateGlobalConfigMsg request = globalRequest("new-global");
        globalFacade(config).handleMessage(request);
        assertEquals(1, participant.beforeCalls.get()); assertEquals(1, participant.afterCalls.get());
        assertEquals(request.getId(), participant.lastContext.getRequestId());
        assertEquals("new-global", globalValue()); assertEquals("new-global", config.value());
        assertEquals(1, canonicalEvents.size()); assertEquals(1, localNotifications.get());
        assertTrue(observerSawCommittedState);
        assertEquals(1, apiEvents.size()); assertNull(apiEvents.get(0).getError());

        Method init = GlobalConfig.class.getDeclaredMethod("init"); init.setAccessible(true); init.invoke(config);
        org.zstack.core.config.GlobalConfigCanonicalEvents.UpdateEvent event =
                new org.zstack.core.config.GlobalConfigCanonicalEvents.UpdateEvent();
        event.setOldValue("old-global"); event.setNewValue("new-global");
        Method run = EventCallback.class.getDeclaredMethod("run", Map.class, Object.class); run.setAccessible(true);
        run.invoke(callbacks.values().iterator().next(), Collections.singletonMap("nodeUuid", "another-mn"), event);
        assertEquals("remote refresh must not create another mutation", 1, participant.beforeCalls.get());
        assertEquals(1, participant.afterCalls.get()); assertEquals(2, markerCount());
    }

    @Test
    public void globalFacadeRollbackLeavesCacheRowsAndNotificationsUnchanged() throws Exception {
        GlobalConfig config = global(CATEGORY, "global", "old-global");
        config.installConfigMutationExtension(participant);
        AtomicInteger localNotifications = new AtomicInteger();
        config.installLocalUpdateExtension((oldValue, newValue) -> localNotifications.incrementAndGet());
        failAfter = true;
        globalFacade(config).handleMessage(globalRequest("must-rollback"));
        assertEquals("old-global", globalValue()); assertEquals("old-global", config.value());
        assertEquals(0, markerCount()); assertEquals(0, canonicalEvents.size());
        assertEquals(0, localNotifications.get());
        assertEquals(1, apiEvents.size()); assertNotNull(apiEvents.get(0).getError());
    }

    private APIUpdateGlobalConfigMsg globalRequest(String value) {
        APIUpdateGlobalConfigMsg request = new APIUpdateGlobalConfigMsg();
        request.setCategory(CATEGORY); request.setName("global"); request.setValue(value); request.setSession(session());
        return request;
    }

    private GlobalConfigFacadeImpl globalFacade(GlobalConfig config) throws Exception {
        GlobalConfigFacadeImpl result = new GlobalConfigFacadeImpl();
        setField(GlobalConfigFacadeImpl.class, result, "allConfig", Collections.singletonMap(config.getIdentity(), config));
        setField(GlobalConfigFacadeImpl.class, result, "bus", bus());
        setField(GlobalConfigFacadeImpl.class, result, "pluginRgty", Proxy.newProxyInstance(
                PluginRegistry.class.getClassLoader(), new Class<?>[]{PluginRegistry.class},
                (p, m, a) -> Collections.emptyList()));
        return result;
    }

    private String globalValue() {
        EntityManager observer = factory.createEntityManager();
        try { return observer.createQuery("select c.value from GlobalConfigVO c where c.category=:category and c.name='global'", String.class)
                .setParameter("category", CATEGORY).getSingleResult(); }
        finally { observer.close(); }
    }

    @Test
    public void unconfiguredNonMemoryFieldKeepsExistingResourceConfigBehavior() throws Exception {
        ResourceConfig ordinary = configs.get(2);
        ordinary.installUpdateExtension((config, uuid, type, oldValue, newValue) -> {
            ordinaryCallbackSawActiveTransaction = org.springframework.transaction.support.TransactionSynchronizationManager
                    .isActualTransactionActive();
        });
        APIUpdateResourceConfigMsg update = new APIUpdateResourceConfigMsg();
        expectedOrdinary = "ordinary-new"; expectedMarkers = 0;
        update.setCategory("compute"); update.setName("ordinary"); update.setResourceUuid(RESOURCE);
        update.setValue("ordinary-new"); update.setSession(session());
        facade.handleMessage(update);

        assertEquals("ordinary-new", configValue("ordinary"));
        assertEquals("non-memory config does not join memory mutation coordinator", 0, participant.beforeCalls.get());
        assertEquals(0, participant.afterCalls.get());
        assertEquals(0, markerCount());
        assertEquals(1, canonicalEvents.size());
        assertTrue("ordinary update extension executes synchronously inside the transaction",
                ordinaryCallbackSawActiveTransaction);
        assertFalse("independent observer must not see an uncommitted ordinary update",
                observerSawCommittedState);
        assertTrue(apiEvents.get(0) instanceof APIUpdateResourceConfigEvent);
        assertNull(apiEvents.get(0).getError());
    }

    @Test
    public void ordinaryResourceConfigCallbackExceptionIsSynchronousAndReported() throws Exception {
        ResourceConfig ordinary = configs.get(2);
        AtomicInteger callbackCalls = new AtomicInteger();
        ordinary.installUpdateExtension((config, uuid, type, oldValue, newValue) -> {
            callbackCalls.incrementAndGet();
            assertTrue("extension is invoked before transaction completion",
                    org.springframework.transaction.support.TransactionSynchronizationManager
                            .isActualTransactionActive());
            throw new org.zstack.core.config.GlobalConfigException("ordinary callback rejected update");
        });
        APIUpdateResourceConfigMsg update = new APIUpdateResourceConfigMsg();
        update.setCategory("compute"); update.setName("ordinary"); update.setResourceUuid(RESOURCE);
        update.setValue("must-not-commit"); update.setSession(session());

        facade.handleMessage(update);

        assertEquals("callback exception is propagated once", 1, callbackCalls.get());
        assertEquals("transaction rollback preserves prior value", "old-ordinary", configValue("ordinary"));
        assertEquals(1, apiEvents.size());
        assertNotNull("facade must report the extension failure", apiEvents.get(0).getError());
        assertTrue(apiEvents.get(0) instanceof APIUpdateResourceConfigEvent);
    }
    private ResourceConfigApiInterceptor interceptor() throws Exception {
        ResourceConfigApiInterceptor result = new ResourceConfigApiInterceptor();
        GlobalConfig alpha = global(CATEGORY, "alpha", "old-alpha");
        GlobalConfig beta = global(CATEGORY, "beta", "old-beta");
        Map<String, GlobalConfig> all = new HashMap<>(); all.put(alpha.getIdentity(), alpha); all.put(beta.getIdentity(), beta);
        GlobalConfigFacade gcf = (GlobalConfigFacade) Proxy.newProxyInstance(GlobalConfigFacade.class.getClassLoader(),
                new Class<?>[]{GlobalConfigFacade.class}, (p, m, a) -> m.getName().equals("getAllConfig") ? all : null);
        ResourceConfigFacade rcf = (ResourceConfigFacade) Proxy.newProxyInstance(ResourceConfigFacade.class.getClassLoader(),
                new Class<?>[]{ResourceConfigFacade.class}, (p, m, a) -> m.getName().equals("getResourceConfig")
                        ? configByIdentity((String) a[0]) : null);
        setField(ResourceConfigApiInterceptor.class, result, "gcf", gcf);
        setField(ResourceConfigApiInterceptor.class, result, "rcf", rcf);
        return result;
    }

    private final class SharedParticipant implements ConfigTransactionalMutationExtensionPoint {
        final AtomicInteger beforeCalls = new AtomicInteger();
        final AtomicInteger afterCalls = new AtomicInteger();
        volatile List<ConfigMutation> lastChanges = Collections.emptyList();
        volatile ConfigMutationContext lastContext;

        @Override public void beforeMutations(EntityManager em, List<ConfigMutation> changes, ConfigMutationContext context) {
            beforeCalls.incrementAndGet(); lastChanges = changes; lastContext = context;
            for (int i = 0; i < changes.size(); i++) persistMarker(em, context, "before-" + i);
        }

        @Override public void afterMutations(EntityManager em, List<ConfigMutation> changes, ConfigMutationContext context) {
            afterCalls.incrementAndGet();
            for (int i = 0; i < changes.size(); i++) persistMarker(em, context, "after-" + i);
            if (failAfter) throw new org.zstack.core.config.GlobalConfigException("A01 coordinated write veto");
        }

        private void persistMarker(EntityManager em, ConfigMutationContext context, String phase) {
            Marker marker = new Marker(); marker.uuid = UUID.randomUUID().toString();
            marker.requestId = context.getRequestId(); marker.phase = phase; em.persist(marker);
        }
    }

    private ResourceConfig config(String category, String name, String defaultValue, boolean managed) throws Exception {
        ResourceConfig result = new ResourceConfig();
        setField(ResourceConfig.class, result, "globalConfig", global(category, name, defaultValue));
        setField(ResourceConfig.class, result, "dbf", dbf);
        setField(ResourceConfig.class, result, "evtf", eventFacade());
        setField(ResourceConfig.class, result, "transactionManager", txManager);
        setField(ResourceConfig.class, result, "resourceClasses", Collections.singletonList(ResourceVO.class));
        Method initGetters = ResourceConfig.class.getDeclaredMethod("initResourceConfigNodes");
        initGetters.setAccessible(true); initGetters.invoke(result);
        if (managed) result.installValidatorExtension((uuid, oldValue, newValue) -> { });
        return result;
    }

    private GlobalConfig global(String category, String name, String value) {
        GlobalConfig result = new GlobalConfig(category, name);
        setFieldUnchecked(GlobalConfig.class, result, "value", value);
        setFieldUnchecked(GlobalConfig.class, result, "defaultValue", value);
        return result;
    }

    private APIUpdateResourceConfigsMsg bulk(String alpha, String beta) {
        APIUpdateResourceConfigsMsg msg = new APIUpdateResourceConfigsMsg();
        msg.setResourceUuid(RESOURCE); msg.setSession(session());
        APIUpdateResourceConfigsMsg.ResourceConfigAO first = new APIUpdateResourceConfigsMsg.ResourceConfigAO();
        first.setCategory(CATEGORY); first.setName("alpha"); first.setValue(alpha);
        APIUpdateResourceConfigsMsg.ResourceConfigAO second = new APIUpdateResourceConfigsMsg.ResourceConfigAO();
        second.setCategory(CATEGORY); second.setName("beta"); second.setValue(beta);
        msg.setResourceConfigs(Arrays.asList(first, second)); return msg;
    }

    private SessionInventory session() {
        SessionInventory session = new SessionInventory(); session.setAccountUuid("test-account");
        session.setUserUuid("test-user"); return session;
    }

    private Map<String, ResourceConfig> mapConfigs(List<ResourceConfig> values) {
        Map<String, ResourceConfig> map = new HashMap<>();
        for (ResourceConfig config : values) {
            GlobalConfig global = (GlobalConfig) getFieldUnchecked(ResourceConfig.class, config, "globalConfig");
            map.put(global.getIdentity(), config);
        }
        return map;
    }

    private ResourceConfig configByIdentity(String identity) {
        return configs.stream().filter(it -> ((GlobalConfig) getFieldUnchecked(ResourceConfig.class, it, "globalConfig"))
                .getIdentity().equals(identity)).findFirst().orElse(null);
    }

    private EventFacade eventFacade() {
        return (EventFacade) Proxy.newProxyInstance(EventFacade.class.getClassLoader(), new Class<?>[]{EventFacade.class}, (p, m, a) -> {
            if (m.getName().equals("on") && a != null && a.length == 2 && a[1] instanceof EventCallback) {
                callbacks.put(String.valueOf(a[0]), (EventCallback) a[1]);
            } else if (m.getName().equals("fire")) {
                canonicalEvents.add(String.valueOf(a[0]));
                EntityManager observer = factory.createEntityManager();
                try {
                    boolean alphaPresent = resourceRowExists(observer, "alpha");
                    String alpha = configRowValue(observer, "alpha");
                    String beta = configRowValue(observer, "beta");
                    String ordinary = configRowValue(observer, "ordinary");
                    Long markers = observer.createQuery("select count(m) from A01MutationMarker m", Long.class).getSingleResult();
                    observerSawCommittedState = Objects.equals(alpha, expectedAlpha)
                            && Objects.equals(beta, expectedBeta) && Objects.equals(ordinary, expectedOrdinary)
                            && markers == expectedMarkers && (alphaPresent == (expectedAlpha != null))
                            && (expectedGlobal == null || Objects.equals(expectedGlobal, globalValue()));
                } finally { observer.close(); }
            }
            return null;
        });
    }

    private CloudBus bus() {
        return (CloudBus) Proxy.newProxyInstance(CloudBus.class.getClassLoader(), new Class<?>[]{CloudBus.class}, (p, m, a) -> {
            if (m.getName().equals("publish") && a != null && a.length == 1 && a[0] instanceof APIEvent) apiEvents.add((APIEvent) a[0]);
            if (m.getName().equals("makeLocalServiceId")) return "local:resource-config";
            return defaultValue(m.getReturnType());
        });
    }

    private DatabaseFacade databaseFacade() {
        return (DatabaseFacade) Proxy.newProxyInstance(DatabaseFacade.class.getClassLoader(), new Class<?>[]{DatabaseFacade.class},
                (p, m, a) -> {
                    if (m.getName().equals("getEntityManager")) return shared;
                    if (m.getName().equals("getCriteriaBuilder")) return factory.getCriteriaBuilder();
                    return defaultValue(m.getReturnType());
                });
    }

    private void installPlatformLoader() throws Exception {
        previousPlatformLoader = readStatic(org.zstack.core.Platform.class, "loader");
        ErrorFacade errors = (ErrorFacade) Proxy.newProxyInstance(ErrorFacade.class.getClassLoader(), new Class<?>[]{ErrorFacade.class},
                (p, m, a) -> m.getReturnType() == ErrorCode.class ? new ErrorCode("A01", "test") : defaultValue(m.getReturnType()));
        ComponentLoader loader = (ComponentLoader) Proxy.newProxyInstance(ComponentLoader.class.getClassLoader(),
                new Class<?>[]{ComponentLoader.class}, (p, m, a) -> {
                    if (m.getName().equals("getComponent") && a != null && a.length == 1 && a[0] == DatabaseFacade.class) return dbf;
                    if (m.getName().equals("getComponent") && a != null && a.length == 1 && a[0] == ErrorFacade.class) return errors;
                    if (m.getName().equals("getComponentNoExceptionWhenNotExisting")) return null;
                    if (m.getReturnType() == boolean.class) return false;
                    return defaultValue(m.getReturnType());
                });
        setField(org.zstack.core.Platform.class, null, "loader", loader);
    }

    private void installAspectBeanFactory() throws Exception {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        AutowiredAnnotationBeanPostProcessor autowired = new AutowiredAnnotationBeanPostProcessor();
        autowired.setBeanFactory(beanFactory);
        beanFactory.addBeanPostProcessor(autowired);
        beanFactory.registerSingleton("databaseFacadeImpl", dbfImpl);
        beanFactory.registerSingleton("eventFacade", eventFacade());
        beanFactory.registerSingleton("transactionManager", txManager);
        beanFactory.registerSingleton("cloudBus", bus());
        beanFactory.registerSingleton("resourceConfigFacade", Proxy.newProxyInstance(ResourceConfigFacade.class.getClassLoader(),
                new Class<?>[]{ResourceConfigFacade.class}, (p, m, a) -> m.getName().equals("getResourceConfig")
                        ? configByIdentity((String) a[0]) : defaultValue(m.getReturnType())));
        beanFactory.registerSingleton("accountManager", Proxy.newProxyInstance(org.zstack.identity.AccountManager.class.getClassLoader(),
                new Class<?>[]{org.zstack.identity.AccountManager.class}, (p, m, a) -> defaultValue(m.getReturnType())));
        beanFactory.registerSingleton("globalConfigFacade", Proxy.newProxyInstance(GlobalConfigFacade.class.getClassLoader(),
                new Class<?>[]{GlobalConfigFacade.class}, (p, m, a) -> defaultValue(m.getReturnType())));
        AnnotationBeanConfigurerAspect aspect = AnnotationBeanConfigurerAspect.aspectOf();
        configurerSupportField = AnnotationBeanConfigurerAspect.class.getDeclaredField("beanConfigurerSupport");
        configurerSupportField.setAccessible(true); priorConfigurerSupport = configurerSupportField.get(aspect);
        beanConfigurerSupport = new BeanConfigurerSupport(); configurerSupportField.set(aspect, beanConfigurerSupport);
        aspect.setBeanFactory(beanFactory);
        ((BeanConfigurerSupport) beanConfigurerSupport).setBeanWiringInfoResolver(bean -> {
            return new BeanWiringInfo(BeanWiringInfo.AUTOWIRE_BY_TYPE, false);
        });
        aspect.afterPropertiesSet();
    }

    private void installDatabaseFacadeImpl() throws Exception {
        dbfImpl = new org.zstack.core.db.DatabaseFacadeImpl();
        setField(org.zstack.core.db.DatabaseFacadeImpl.class, dbfImpl, "entityManagerFactory", factory);
        setField(org.zstack.core.db.DatabaseFacadeImpl.class, dbfImpl, "entityManager", shared);
        Method buildInfo = org.zstack.core.db.DatabaseFacadeImpl.class.getDeclaredMethod("buildEntityInfo");
        buildInfo.setAccessible(true); buildInfo.invoke(dbfImpl);
        dbf = dbfImpl;
    }

    private void installTransactionAspect() throws Exception {
        AnnotationTransactionAspect aspect = AnnotationTransactionAspect.aspectOf(); transactionAspect = aspect;
        transactionManagerField = org.springframework.transaction.interceptor.TransactionAspectSupport.class.getDeclaredField("transactionManager");
        transactionManagerField.setAccessible(true); previousTransactionManager = transactionManagerField.get(aspect);
        aspect.setTransactionManager(txManager);
    }

    private void installMessageSafeAspect() throws Exception {
        messageSafeAspect = MessageSafeAspect.aspectOf();
        messageBusField = MessageSafeAspect.class.getDeclaredField("bus"); messageBusField.setAccessible(true);
        messageErrorField = MessageSafeAspect.class.getDeclaredField("errf"); messageErrorField.setAccessible(true);
        previousMessageBus = messageBusField.get(messageSafeAspect); previousMessageErrors = messageErrorField.get(messageSafeAspect);
        messageBusField.set(messageSafeAspect, bus());
        messageErrorField.set(messageSafeAspect, Proxy.newProxyInstance(ErrorFacade.class.getClassLoader(), new Class<?>[]{ErrorFacade.class},
                (p, m, a) -> m.getReturnType() == ErrorCode.class ? new ErrorCode("A01", "test") : defaultValue(m.getReturnType())));
    }

    private void prepareEntityMetadata() throws Exception {
        encryptAspect = EncryptColumnAspect.aspectOf();
        encryptRegistryField = EncryptColumnAspect.class.getDeclaredField("pluginRegistry"); encryptRegistryField.setAccessible(true);
        previousEncryptRegistry = encryptRegistryField.get(encryptAspect);
        encryptRegistryField.set(encryptAspect, Proxy.newProxyInstance(PluginRegistry.class.getClassLoader(),
                new Class<?>[]{PluginRegistry.class}, (p, m, a) -> Collections.emptyList()));
        Method init = EntityMetadata.class.getDeclaredMethod("staticInit"); init.setAccessible(true); init.invoke(null);
    }

    private void restoreAspectState() throws Exception {
        if (transactionAspect != null) ((AnnotationTransactionAspect) transactionAspect).setTransactionManager(
                (org.springframework.transaction.PlatformTransactionManager) previousTransactionManager);
        if (configurerSupportField != null) configurerSupportField.set(AnnotationBeanConfigurerAspect.aspectOf(), priorConfigurerSupport);
        if (messageBusField != null) messageBusField.set(messageSafeAspect, previousMessageBus);
        if (messageErrorField != null) messageErrorField.set(messageSafeAspect, previousMessageErrors);
        if (encryptRegistryField != null) encryptRegistryField.set(encryptAspect, previousEncryptRegistry);
    }

    private void invokeInstallEventTrigger(ResourceConfig config) throws Exception {
        Method method = ResourceConfig.class.getDeclaredMethod("installEventTrigger"); method.setAccessible(true); method.invoke(config);
    }

    private ResourceConfigVO configRow(String uuid, String category, String name, String value) {
        ResourceConfigVO row = new ResourceConfigVO(); row.setUuid("row-" + uuid); row.setResourceUuid(RESOURCE);
        row.setResourceType(ResourceVO.class.getSimpleName()); row.setCategory(category); row.setName(name); row.setValue(value);
        return row;
    }

    private String configValue(String name) { EntityManager em = factory.createEntityManager(); try { return configRowValue(em, name); } finally { em.close(); } }
    private String configRowValue(String name) { EntityManager em = factory.createEntityManager(); try { return configRowValue(em, name); } finally { em.close(); } }
    private String configRowValue(EntityManager em, String name) {
        List<String> values = em.createQuery("select c.value from ResourceConfigVO c where c.resourceUuid=:uuid and c.name=:name", String.class)
                .setParameter("uuid", RESOURCE).setParameter("name", name).getResultList();
        return values.isEmpty() ? null : values.get(0);
    }
    private boolean resourceRowExists(EntityManager em, String name) { return configRowValue(em, name) != null; }
    private long markerCount() { EntityManager em = factory.createEntityManager(); try { return em.createQuery("select count(m) from A01MutationMarker m", Long.class).getSingleResult(); } finally { em.close(); } }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false; if (type == int.class) return 0; if (type == long.class) return 0L;
        if (type == double.class) return 0d; if (type == float.class) return 0f; if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0; if (type == char.class) return (char) 0; return null;
    }
    private static Object readStatic(Class<?> type, String name) throws Exception { return getField(type, null, name); }
    private static Object getField(Class<?> type, Object target, String name) throws Exception { Field f=type.getDeclaredField(name); f.setAccessible(true); return f.get(target); }
    private static void setField(Class<?> type, Object target, String name, Object value) throws Exception { Field f=type.getDeclaredField(name); f.setAccessible(true); f.set(target, value); }
    private static Object getFieldUnchecked(Class<?> type, Object target, String name) { try { return getField(type, target, name); } catch (Exception e) { throw new AssertionError(e); } }
    private static void setFieldUnchecked(Class<?> type, Object target, String name, Object value) { try { setField(type, target, name, value); } catch (Exception e) { throw new AssertionError(e); } }
}
