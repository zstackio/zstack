package org.zstack.kvm.memory;

import org.zstack.core.Platform;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zstack.header.longjob.LongJobState;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.host.HostStatus;
import javax.persistence.*;
import java.util.*;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.function.Function;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfigVO;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigVO;
import org.zstack.header.vo.ResourceVO;
import static org.junit.Assert.*;

/** Actual transactions and unique/row-lock schema, isolated from any product database. */
public class MemoryRepositoryTest {
    @Entity(name = "HostVO") @Table(name = "TestMemoryHost")
    public static class TestHost {
        @Id public String uuid;
        public String hypervisorType;
        public String clusterUuid;
        public String zoneUuid;
        @Enumerated(EnumType.STRING)
        public HostStatus status;
    }
    @Entity(name = "ClusterVO") @Table(name = "TestMemoryCluster")
    public static class TestCluster {
        @Id public String uuid;
        public String hypervisorType;
    }
    @Entity(name = "VmInstanceVO") @Table(name = "TestMemoryVm")
    public static class TestVm {
        @Id public String uuid;
        public String hostUuid;
    }
    @Entity(name = "TestMemoryResourceVO") @Table(name = "TestMemoryResource")
    public static class TestResource {
        @Id public String uuid;
        public String resourceType;
    }
    private SessionFactory factory;
    private MemoryRepository repository;
    private final MemoryStandardConfigAdapter standardConfigAdapter = new MemoryStandardConfigAdapter();
    private Field registryField;
    private Object encryptAspect;
    private Object previousRegistry;
    private Class<?> previousResourceBaseType;
    private String failResultUuid;
    private final List<Object[]> firedEvents = new ArrayList<>();
    private boolean failAfterResourceConfigWrite;

    @Before public void setup() throws Exception {
        previousResourceBaseType = org.zstack.header.vo.ResourceTypeMetadata.concreteBaseTypeMapping.get(ResourceVO.class);
        failResultUuid = null;
        failAfterResourceConfigWrite = false;
        org.zstack.header.vo.ResourceTypeMetadata.concreteBaseTypeMapping.put(ResourceVO.class,
                org.zstack.header.host.HostVO.class);
        // The product's woven persist advice requires a registry even for entities
        // with no encrypted fields. Keep that advice active with an empty test registry.
        Class<?> aspect = Class.forName("org.zstack.core.aspect.EncryptColumnAspect");
        encryptAspect = aspect.getMethod("aspectOf").invoke(null);
        registryField = aspect.getDeclaredField("pluginRegistry"); registryField.setAccessible(true);
        previousRegistry = registryField.get(encryptAspect);
        registryField.set(encryptAspect, Proxy.newProxyInstance(PluginRegistry.class.getClassLoader(),
                new Class<?>[]{PluginRegistry.class}, (proxy, method, args) -> Collections.emptyList()));
        java.lang.reflect.Method metadataInit = org.zstack.core.db.EntityMetadata.class.getDeclaredMethod("staticInit");
        metadataInit.setAccessible(true); metadataInit.invoke(null);
        Configuration configuration = new Configuration();
        try { configuration.addAnnotatedClass(Class.forName("org.zstack.kvm.memory.MemoryVmExclusionVO")); }
        catch (ClassNotFoundException ignored) { }
        factory = configuration.addAnnotatedClass(MemoryPolicyVO.class)
                .addAnnotatedClass(MemoryTaskVO.class)
                .addAnnotatedClass(MemoryMigrationVO.class)
                .addAnnotatedClass(MemoryStateVO.class)
                .addAnnotatedClass(HostMemoryStateVO.class).addAnnotatedClass(LongJobVO.class)
                .addAnnotatedClass(TestHost.class).addAnnotatedClass(TestVm.class).addAnnotatedClass(TestCluster.class)
                .addAnnotatedClass(TestResource.class).addAnnotatedClass(ResourceVO.class)
                .addAnnotatedClass(GlobalConfigVO.class).addAnnotatedClass(ResourceConfigVO.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:memory" + UUID.randomUUID() + ";MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.check_nullability", "true")
                .setProperty("hibernate.generate_statistics", "true")
                .setProperty("hibernate.show_sql", "false").buildSessionFactory();
        ThreadLocal<EntityManager> activeEm = new ThreadLocal<>();
        repository = new MemoryRepository() {
            @Override public void result(String uuid, String status, String reason, MemoryAgentResponse response) {
                if (Objects.equals(failResultUuid, uuid)) {
                    throw new IllegalStateException("injected stale-result failure");
                }
                super.result(uuid, status, reason, response);
            }

            @Override protected <T> T transaction(Function<EntityManager, T> function) {
                EntityManager existing = activeEm.get();
                if (existing != null) { return function.apply(existing); }
                EntityManager em = factory.createEntityManager();
                boolean synchronize = !org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive();
                try {
                    em.getTransaction().begin(); activeEm.set(em);
                    if (synchronize) { org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization(); }
                    EntityManager transactionEm = em;
                    if (failAfterResourceConfigWrite) {
                        transactionEm = (EntityManager) Proxy.newProxyInstance(EntityManager.class.getClassLoader(),
                                new Class<?>[]{EntityManager.class}, (proxy, method, args) -> {
                                    try {
                                        Object result = method.invoke(em, args);
                                        if ("persist".equals(method.getName()) && args != null && args.length == 1
                                                && args[0] instanceof ResourceConfigVO) {
                                            failAfterResourceConfigWrite = false;
                                            throw new IllegalStateException("injected post-write config failure");
                                        }
                                        return result;
                                    } catch (java.lang.reflect.InvocationTargetException failure) {
                                        throw failure.getCause();
                                    }
                                });
                    }
                    T result = function.apply(transactionEm);
                    em.getTransaction().commit();
                    if (synchronize) {
                        for (org.springframework.transaction.support.TransactionSynchronization callback :
                                org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations()) {
                            callback.afterCommit();
                            callback.afterCompletion(org.springframework.transaction.support.TransactionSynchronization.STATUS_COMMITTED);
                        }
                    }
                    return result;
                } catch (RuntimeException e) {
                    if (em.getTransaction().isActive()) { em.getTransaction().rollback(); }
                    if (synchronize && org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
                        for (org.springframework.transaction.support.TransactionSynchronization callback :
                                org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations()) {
                            callback.afterCompletion(org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK);
                        }
                    }
                    throw e;
                } finally {
                    if (synchronize && org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
                        org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
                    }
                    activeEm.remove(); em.close();
                }
            }
        };
        Field eventField = MemoryRepository.class.getDeclaredField("evtf"); eventField.setAccessible(true);
        eventField.set(repository, Proxy.newProxyInstance(EventFacade.class.getClassLoader(), new Class<?>[]{EventFacade.class},
                (proxy, method, args) -> {
                    if ("fire".equals(method.getName())) { firedEvents.add(args); }
                    return null;
                }));
        repository.transaction(em -> {
            for (MemoryStandardField field : MemoryStandardField.values()) {
                String defaultValue = standardDefault(field);
                GlobalConfigVO config = new GlobalConfigVO(); config.setCategory(field.category());
                config.setName(field.configName()); config.setDescription(field.config().getDescription());
                config.setDefaultValue(defaultValue); config.setValue(defaultValue); em.persist(config);
            }
            return null;
        });
        repository.initialize();
        repository.transaction(em -> {
            TestCluster cluster = new TestCluster(); cluster.uuid = "cluster1";
            cluster.hypervisorType = "KVM"; em.persist(cluster);
            for (String id : Arrays.asList("host1", "host2")) {
                TestHost host = new TestHost(); host.uuid = id; host.hypervisorType = "KVM";
                host.clusterUuid = "cluster1"; host.zoneUuid = id.equals("host1") ? "zone1" : "zone2";
                host.status = HostStatus.Connected; em.persist(host);
            }
            TestVm vm = new TestVm(); vm.uuid = "vm1"; vm.hostUuid = "host1"; em.persist(vm);
            // Default fixtures represent Hosts with a current KSM/ZRAM capability observation.
            // Tests for missing/stale observations explicitly replace/remove this row.
            for (String id : Arrays.asList("host1", "host2")) {
                MemoryStateVO state = new MemoryStateVO(); state.setHostUuid(id); state.setStatus("Succeeded");
                state.setLastSampleTime(System.currentTimeMillis());
                state.setCapabilities("{\"supported\":true,\"ksm\":true,\"ksmZeroPages\":true,\"zram\":true,\"writeback\":true}");
                state.setState("{}"); em.persist(state);
            }
            return null;
        });
    }
    @After public void close() throws Exception {
        if (factory != null) { factory.close(); }
        if (registryField != null) { registryField.set(encryptAspect, previousRegistry); }
        if (previousResourceBaseType == null) {
            org.zstack.header.vo.ResourceTypeMetadata.concreteBaseTypeMapping.remove(ResourceVO.class);
        } else {
            org.zstack.header.vo.ResourceTypeMetadata.concreteBaseTypeMapping.put(ResourceVO.class, previousResourceBaseType);
        }
    }

    @Test public void serviceObservationReusesConnectedPolicyAndExistingControlFencesWithoutWrites() {
        assertNotNull(repository.serviceObservationPolicy("host1"));
        assertNull(repository.serviceObservationPolicy("absent"));
        for (String status : Arrays.asList("Unknown", "Succeeded")) {
            repository.transaction(em -> {
                MemoryStateVO state = em.find(MemoryStateVO.class, "host1"); state.setStatus(status);
                state.setActiveTaskUuid("Succeeded".equals(status) ? "inflight" : null);
                return null;
            });
            assertNull(repository.serviceObservationPolicy("host1"));
        }
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1"); state.setActiveTaskUuid(null);
            state.setState("{\"lifecycle\":{\"paused\":true,\"activeState\":\"paused\"}}"); return null;
        });
        assertNull(repository.serviceObservationPolicy("host1"));
        repository.transaction(em -> {
            em.find(MemoryStateVO.class, "host1").setState("{}");
            em.find(TestHost.class, "host1").status = HostStatus.Disconnected; return null;
        });
        assertNull(repository.serviceObservationPolicy("host1"));
        repository.transaction(em -> { em.find(TestHost.class, "host1").status = HostStatus.Connected; return null; });
        factory.getStatistics().clear();
        assertNotNull(repository.serviceObservationPolicy("host1"));
        assertEquals(0, factory.getStatistics().getEntityInsertCount());
        assertEquals(0, factory.getStatistics().getEntityUpdateCount());
        assertEquals(0, factory.getStatistics().getEntityDeleteCount());
    }

    @Test public void summaryTargetsIntersectZoneAndHostSelection() {
        assertEquals(Arrays.asList("host1", "host2"), repository.summaryTargets(null, null));
        assertEquals(Collections.singletonList("host1"), repository.summaryTargets(null, "zone1"));
        assertEquals(Collections.singletonList("host2"), repository.summaryTargets(Arrays.asList("host2", "host2"), null));
        assertTrue(repository.summaryTargets(Collections.singletonList("host2"), "zone1").isEmpty());
        assertTrue(repository.summaryTargets(null, "absent").isEmpty());
        assertTrue(repository.summaryTargets(Collections.emptyList(), "zone1").isEmpty());
        assertTrue(repository.summaryTargets(Collections.emptyList(), null).isEmpty());
    }

    @Test public void summaryTargetsRejectInvalidOrNonKvmHostsBeforeZoneIntersection() {
        repository.transaction(em -> {
            TestHost esx = new TestHost(); esx.uuid = "esx1"; esx.hypervisorType = "ESX";
            esx.zoneUuid = "zone2"; esx.status = HostStatus.Connected; em.persist(esx); return null;
        });
        for (List<String> hosts : Arrays.asList(Collections.singletonList("absent"),
                Arrays.asList("host1", "absent"), Collections.singletonList("esx1"))) {
            try {
                repository.summaryTargets(hosts, "zone1");
                fail("invalid current Host selection must not become zero coverage: " + hosts);
            } catch (MemoryOperationException expected) {
                assertEquals("MEMORY_INVALID_SCOPE", expected.getCode());
            }
        }
    }

    private void registerStandardResources() {
        repository.transaction(em -> {
            em.persist(new ResourceVO(new Object[]{"host1", "host1", "HostVO"}));
            em.persist(new ResourceVO(new Object[]{"host2", "host2", "HostVO"}));
            em.persist(new ResourceVO(new Object[]{"cluster1", "cluster1", "ClusterVO"}));
            em.flush();
            // The isolated callback defaults to HostVO, unlike actual Cluster creation.
            em.createQuery("update ResourceVO r set r.resourceType = :type where r.uuid = :uuid")
                    .setParameter("type", "ClusterVO").setParameter("uuid", "cluster1").executeUpdate();
            return null;
        });
    }

    private void registerPolicyReadResource(String uuid, String type) {
        repository.transaction(em -> {
            em.persist(new ResourceVO(new Object[]{uuid, uuid, type}));
            em.flush();
            // ResourceVO's persist callback in this isolated fixture defaults to
            // HostVO. Bind the same registry type that real resource creation stores.
            em.createQuery("update ResourceVO r set r.resourceType = :type where r.uuid = :uuid")
                    .setParameter("type", type).setParameter("uuid", uuid).executeUpdate();
            return null;
        });
    }

    private static final class StandardChange {
        final MemoryStandardField field; final String resource, type, value;
        StandardChange(MemoryStandardField field, String resource, String type, String value) {
            this.field = field; this.resource = resource; this.type = type; this.value = value;
        }
    }

    private StandardChange standardChange(MemoryStandardField field, String resource, String type, String value) {
        return new StandardChange(field, resource, type, value);
    }

    /** Mirrors the GlobalConfig/ResourceConfig validate, persist and post-update hooks. */
    private void standardCommit(List<StandardChange> changes, Object ignored, boolean rollback) {
        for (StandardChange change : changes) {
            repository.validateStandardChange(change.field, change.resource, change.value);
        }
        repository.transaction(em -> {
            for (StandardChange change : changes) {
                if ("ksm.enabled".equals(change.field.path()) && "none".equalsIgnoreCase(change.value)) {
                    persistLegacyKsmValue(em, change.resource, change.type, "none");
                } else {
                    Object value = MemoryStandardConfigCodec.decode(change.field.path(), change.value);
                    if (change.resource == null) { writeGlobal(em, change.field.path(), value); }
                    else { writeOverride(em, change.resource, change.type, change.field.path(), value); }
                }
            }
            if (rollback) { throw new IllegalStateException("forced standard transaction rollback"); }
            return null;
        });
        changes.stream().map(change -> change.resource).distinct()
                .forEach(resource -> repository.standardConfigurationChanged(resource));
    }

    private void writeGlobal(EntityManager em, String path, Object value) {
        MemoryStandardField field = MemoryStandardField.forPath(path);
        GlobalConfigVO row = em.createQuery("select g from GlobalConfigVO g where g.category = :category and g.name = :name",
                GlobalConfigVO.class).setParameter("category", field.category())
                .setParameter("name", field.configName()).getSingleResult();
        String encoded = MemoryStandardConfigCodec.encode(path, value);
        row.setValue(encoded == null ? row.getDefaultValue() : encoded);
        em.merge(row);
    }

    private void writeOverride(EntityManager em, String resource, String type, String path, Object value) {
        standardConfigAdapter.writeOverrides(em, resource, type, Collections.singletonMap(path, value));
    }

    @Test public void standardGlobalMixedHostsPersistDesiredButPreserveUnknownFence() {
        registerStandardResources();
        repository.transaction(em -> {
            MemoryStateVO unknown = em.find(MemoryStateVO.class, "host1");
            unknown.setStatus("Unknown"); unknown.setActiveTaskUuid("old-unknown");
            unknown.setControlOperationUuid("old-unknown");
            em.find(TestHost.class, "host2").status = HostStatus.Disconnected; return null;
        });
        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.KSM_ENABLED,
                null, null, "true")), null, false);
        assertEquals(Boolean.TRUE, MemoryPolicyRules.decode(effectivePolicy("Global", "global")).ksm.enabled);
        assertEquals("old-unknown", host1State().getActiveTaskUuid());
        assertEquals("old-unknown", host1State().getControlOperationUuid());
        assertEquals("Unknown", host1State().getStatus());
        assertEquals("Blocked", host1State().getPolicyPlanStatus());
        assertTrue(claimAll().isEmpty());
        assertEquals("deferred Hosts keep their plan on the Host state, not as tasks", 0L, taskCount());
    }

    @Test public void standardPureKsmCanDispatchFromFreshPartialOnlyWhenBothControlFencesAreClear() {
        registerStandardResources();
        repository.transaction(em -> {
            for (String hostUuid : Arrays.asList("host1", "host2")) {
                MemoryStateVO state = em.find(MemoryStateVO.class, hostUuid);
                state.setStatus("Unknown"); state.setLastSampleTime(null);
                state.setActiveTaskUuid(null); state.setControlOperationUuid(null);
            }
            return null;
        });
        final long sample1 = System.currentTimeMillis();
        MemoryAgentResponse partial1 = coldKsmPartial(sample1, true);
        repository.observePartialKsm("host1", partial1);
        MemoryStateVO beforeForeign = host1State();
        String capabilitiesBeforeForeign = beforeForeign.getCapabilities();
        String stateBeforeForeign = beforeForeign.getState();
        Map<String, Object> coldObservationBeforeForeign = MemoryCapabilityRules.coldKsmObservation(
                beforeForeign, System.currentTimeMillis());
        assertNotNull("a partial without optional Host UUID remains compatible", coldObservationBeforeForeign);

        MemoryAgentResponse foreignPartial = coldKsmPartial(sample1 + 1, false);
        foreignPartial.hostUuid = "host2";
        foreignPartial.bootId = "boot-host2";
        foreignPartial.state.put("bootId", foreignPartial.bootId);
        repository.observePartialKsm("host1", foreignPartial);

        MemoryStateVO afterForeign = host1State();
        assertEquals("foreign partial must not overwrite target capability data",
                capabilitiesBeforeForeign, afterForeign.getCapabilities());
        assertEquals("foreign partial must not overwrite target native observation data",
                stateBeforeForeign, afterForeign.getState());
        assertEquals("foreign partial must not revoke or replace the target cold-admission proof",
                coldObservationBeforeForeign,
                MemoryCapabilityRules.coldKsmObservation(afterForeign, System.currentTimeMillis()));
        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.KSM_ENABLED,
                "host1", "HostVO", "true")), null, false);

        MemoryStateVO queued = host1State();
        assertNotNull("only a pure KSM first dispatch may consume independently observed KSM capability; status="
                + queued.getStatus() + ", plan=" + queued.getPolicyPlanStatus() + ", blockedFields="
                + queued.getPolicyBlockedFields() + ", reason=" + queued.getReason(),
                queued.getActiveTaskUuid());
        assertEquals("Queued", queued.getStatus());
        assertNotNull(queued.getControlOperationUuid());
        assertEquals(2L, taskCount());

        final long sample2 = System.currentTimeMillis();
        repository.observePartialKsm("host2", coldKsmPartial(sample2, false));
        assertEquals("Agent-local Unknown with empty DB fences must be preserved in the persisted partial state",
                "LOCAL_CONTROL_FENCE_UNKNOWN", MemoryCapabilityRules.coldKsmAdmissionReason(
                        repository.states(Collections.singletonList("host2"), 0, 1).get(0),
                        System.currentTimeMillis()));
        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.KSM_ENABLED,
                "host2", "HostVO", "true")), null, false);
        MemoryStateVO blocked = repository.states(Collections.singletonList("host2"), 0, 1).get(0);
        assertNull("Agent-local Unknown must not be bypassed because DB activeTask is empty",
                blocked.getActiveTaskUuid());
        assertEquals("Unknown", blocked.getStatus());
        assertEquals("Blocked", blocked.getPolicyPlanStatus());
        assertEquals(2L, taskCount());
    }

    private static MemoryAgentResponse coldKsmPartial(long sampleTime, boolean admissionReady) {
        MemoryAgentResponse response = new MemoryAgentResponse();
        response.setSuccess(false); response.status = "Unknown";
        response.observationKind = "KSM_NATIVE_PARTIAL";
        response.reasonCode = "LOCAL_SERVICE_UNAVAILABLE";
        response.bootId = "boot-a"; response.sampleTime = sampleTime;
        response.coldKsmAdmissionReady = admissionReady;
        response.coldKsmAdmissionReason = admissionReady ? "LOCAL_CONTROL_FENCE_CLEAR" : "LOCAL_CONTROL_FENCE_UNKNOWN";
        response.capabilities = new LinkedHashMap<>(); response.capabilities.put("supported", true);
        response.capabilities.put("ksm", true); response.capabilities.put("ksmZeroPages", false);
        response.capabilities.put("zram", false); response.capabilities.put("writeback", false);
        response.state = new LinkedHashMap<>(); response.state.put("bootId", "boot-a");
        Map<String, Object> ksm = new LinkedHashMap<>(); ksm.put("enabled", true);
        ksm.put("pagesToScan", 1250); ksm.put("sleepMillis", 10);
        Map<String, Object> actual = new LinkedHashMap<>(); actual.put("ksm", ksm);
        response.state.put("actual", actual);
        return response;
    }

    @Test public void partialNativeKsmPersistsSeparatelyAndNeverRefreshesUnknownControlState() {
        registerStandardResources();
        Map<?, ?> beforeCapabilities = org.zstack.utils.gson.JSONObjectUtil.toObject(
                host1State().getCapabilities(), Map.class);
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Unknown"); state.setActiveTaskUuid("old-unknown");
            state.setControlOperationUuid("old-unknown"); state.setDesiredRevision(9);
            state.setAppliedRevision(6L); state.setLastSampleTime(null);
            state.setState("{\"phase\":\"UNKNOWN\",\"bootId\":\"old-boot\"}");
            return null;
        });
        MemoryAgentResponse partial = new MemoryAgentResponse();
        partial.setSuccess(false); partial.status = "Unknown";
        partial.observationKind = "KSM_NATIVE_PARTIAL";
        partial.reasonCode = "LOCAL_SERVICE_UNAVAILABLE";
        partial.bootId = "boot-a"; partial.sampleTime = System.currentTimeMillis();
        partial.coldKsmAdmissionReady = true; partial.coldKsmAdmissionReason = "LOCAL_CONTROL_FENCE_CLEAR";
        partial.capabilities = new LinkedHashMap<>(); partial.capabilities.put("supported", true);
        partial.capabilities.put("ksm", true); partial.capabilities.put("ksmZeroPages", false);
        partial.capabilities.put("zram", false); partial.capabilities.put("writeback", false);
        partial.state = new LinkedHashMap<>(); partial.state.put("bootId", "boot-a");
        Map<String, Object> ksm = new LinkedHashMap<>(); ksm.put("enabled", true);
        ksm.put("pagesToScan", 1250); ksm.put("sleepMillis", 10);
        Map<String, Object> actual = new LinkedHashMap<>(); actual.put("ksm", ksm);
        partial.state.put("actual", actual);

        repository.observePartialKsm("host1", partial);

        MemoryStateVO stored = host1State();
        assertEquals("Unknown", stored.getStatus());
        assertEquals("old-unknown", stored.getActiveTaskUuid());
        assertEquals("old-unknown", stored.getControlOperationUuid());
        assertEquals(9L, stored.getDesiredRevision());
        assertEquals(Long.valueOf(6L), stored.getAppliedRevision());
        assertNull("partial KSM does not refresh the complete monitoring clock", stored.getLastSampleTime());
        Map<?, ?> caps = org.zstack.utils.gson.JSONObjectUtil.toObject(stored.getCapabilities(), Map.class);
        assertEquals("KSM_NATIVE_PARTIAL", ((Map<?, ?>) caps.get("nativeKsmPartial")).get("observationKind"));
        Map<?, ?> partialCapabilities = (Map<?, ?>) ((Map<?, ?>) caps.get("nativeKsmPartial")).get("capabilities");
        assertEquals(Boolean.TRUE, partialCapabilities.get("supported"));
        assertEquals(Boolean.TRUE, partialCapabilities.get("ksm"));
        Map<String, Object> fullCapabilitiesAfter = new LinkedHashMap<>((Map<String, Object>) caps);
        fullCapabilitiesAfter.remove("nativeKsmPartial");
        assertEquals("partial KSM must not overwrite full-observation capability fields",
                beforeCapabilities, fullCapabilitiesAfter);
        Map<?, ?> state = org.zstack.utils.gson.JSONObjectUtil.toObject(stored.getState(), Map.class);
        assertEquals("UNKNOWN", state.get("phase"));
        assertEquals("old-boot", state.get("bootId"));
        assertTrue(state.containsKey("nativeKsmObservation"));
        assertEquals("HOST_OPERATION_UNRESOLVED",
                MemoryCapabilityRules.coldKsmAdmissionReason(stored, System.currentTimeMillis()));
    }

    @Test public void standardGlobalWithNoHostCanSaveAndHostOnlyFieldCannot() {
        repository.transaction(em -> { em.createQuery("delete from HostVO").executeUpdate(); return null; });
        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.KSM_ENABLED,
                null, null, "true")), null, false);
        assertEquals("a save without Hosts records no operation", 0L, taskCount());
        try {
            standardCommit(Collections.singletonList(standardChange(MemoryStandardField.WRITEBACK_BACKEND_RESOURCE,
                    null, null, "0123456789abcdef0123456789abcdef")), null, false);
            fail("backend identity is Host-only");
        } catch (MemoryOperationException expected) { assertEquals("MEMORY_INVALID_SCOPE", expected.getCode()); }
    }

    @Test public void standardZramCapacityChangeQueuesDrainBeforeCapacityApply() {
        prepareCapacityObservation("Succeeded", false, false, false);

        standardCommit(Collections.singletonList(standardChange(
                MemoryStandardField.ZRAM_LOGICAL_CAPACITY, null, null, "16384")),
                null, false);

        List<MemoryTaskVO> tasks = repository.transaction(em -> em.createQuery(
                "from MemoryTaskVO t where t.hostUuid = :host order by t.createDate", MemoryTaskVO.class)
                .setParameter("host", "host1").getResultList());
        assertEquals("capacity change must queue a drain before any capacity apply", "drain",
                tasks.get(tasks.size() - 1).getAction());
    }

    @Test public void standardZramPhysicalCapacityChangeQueuesDrainBeforeCapacityApply() {
        prepareCapacityObservation("Succeeded", false, false, false);
        standardCommit(Collections.singletonList(standardChange(
                MemoryStandardField.ZRAM_RAM_LIMIT, null, null, "8192")),
                null, false);
        List<MemoryTaskVO> tasks = repository.transaction(em -> em.createQuery(
                "from MemoryTaskVO t where t.hostUuid = :host order by t.createDate", MemoryTaskVO.class)
                .setParameter("host", "host1").getResultList());
        assertEquals("physical capacity change must queue drain", "drain",
                tasks.get(tasks.size() - 1).getAction());
    }

    @Test public void restoringDefaultCapacityQueuesDrain() {
        prepareCapacityObservation("Succeeded", false, false, false);
        standardCommit(Collections.singletonList(standardChange(
                MemoryStandardField.ZRAM_RAM_LIMIT, null, null, "")),
                null, false);
        List<MemoryTaskVO> tasks = repository.transaction(em -> em.createQuery(
                "from MemoryTaskVO t where t.hostUuid = :host order by t.createDate", MemoryTaskVO.class)
                .setParameter("host", "host1").getResultList());
        assertEquals("restoring the default capacity must rebuild the pool", "drain",
                tasks.get(tasks.size() - 1).getAction());
    }

    @Test public void restoringDefaultCapacitySendsServiceDefaultAndKeepsAppliedPolicyConcrete() {
        prepareCapacityObservation("Succeeded", false, false, false);
        standardCommit(Arrays.asList(
                standardChange(MemoryStandardField.ZRAM_ENABLED, null, null, "false"),
                standardChange(MemoryStandardField.ZRAM_RAM_LIMIT, null, null, "")),
                null, false);
        MemoryTaskVO apply = claimAll().stream()
                .filter(task -> Arrays.asList("apply", "clearOverride").contains(task.getAction()))
                .findFirst().orElse(null);
        assertNotNull("the disabled pool takes the capacity change without a drain", apply);
        com.google.gson.JsonObject zram = new com.google.gson.JsonParser().parse(apply.getPolicy())
                .getAsJsonObject().getAsJsonObject("zram");
        assertEquals(8192L, zram.get("logicalCapacityBytes").getAsLong());
        assertTrue("the default side asks the service default: " + zram,
                zram.has("ramLimitBytes") && zram.get("ramLimitBytes").isJsonNull());

        MemoryAgentResponse applied = new MemoryAgentResponse(); applied.appliedRevision = apply.getDesiredRevision();
        repository.result(apply.getUuid(), "Succeeded", "default capacity applied", applied);
        com.google.gson.JsonObject appliedZram = new com.google.gson.JsonParser()
                .parse(host1State().getAppliedPolicy()).getAsJsonObject().getAsJsonObject("zram");
        assertFalse("a default side is not an applied override: " + appliedZram, appliedZram.has("ramLimitBytes"));
        assertEquals(8192L, appliedZram.get("logicalCapacityBytes").getAsLong());
        assertEquals("Applied", host1State().getPolicyPlanStatus());
    }

    @Test public void unchangedCapacityInAFeatureClearSnapshotDoesNotRestoreItsDefaultSide() {
        prepareCapacityObservation("Succeeded", false, false, false);
        repository.transaction(em -> {
            writeGlobal(em, "zram.ramLimitBytes", null);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setAppliedPolicy(state.getAppliedPolicy().replace(",\"ramLimitBytes\":4096", ""));
            state.setAppliedPolicyHash(sha256(state.getAppliedPolicy()));
            return null;
        });
        standardCommit(Collections.singletonList(standardChange(
                MemoryStandardField.KSM_ZERO_PAGES, null, null, "true")),
                null, false);
        List<MemoryTaskVO> tasks = repository.transaction(em -> em.createQuery(
                "from MemoryTaskVO t where t.hostUuid = :host and t.action in ('apply', 'clearOverride') order by t.createDate",
                MemoryTaskVO.class).setParameter("host", "host1").getResultList());
        assertFalse(tasks.isEmpty());
        com.google.gson.JsonObject zram = new com.google.gson.JsonParser().parse(tasks.get(tasks.size() - 1).getPolicy())
                .getAsJsonObject().getAsJsonObject("zram");
        assertTrue("an unchanged capacity must not write its default side: " + zram,
                zram == null || !zram.has("ramLimitBytes"));
    }

    @Test public void desiredCapacityMatchingEffectiveReadbackDoesNotDrainDespiteStaleAppliedPolicy() {
        prepareCapacityObservation("Succeeded", false, false, false, 16384L, 4096L);
        standardCommit(Collections.singletonList(standardChange(
                MemoryStandardField.ZRAM_LOGICAL_CAPACITY, null, null, "16384")),
                null, false);
        assertEquals(0L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t where t.hostUuid = :host and t.action = :action", Long.class)
                .setParameter("host", "host1").setParameter("action", "drain").getSingleResult()));
    }

    @Test public void staleQueuedDrainIsNotDispatchedAfterCapacityTargetChanges() {
        prepareCapacityObservation("Succeeded", false, false, false);
        standardCommit(Collections.singletonList(standardChange(
                MemoryStandardField.ZRAM_LOGICAL_CAPACITY, null, null, "16384")),
                null, false);
        assertEquals(1L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t where t.hostUuid = :host and t.action = :action and t.status = :status",
                Long.class).setParameter("host", "host1").setParameter("action", "drain")
                .setParameter("status", "Queued").getSingleResult()));
        try {
            standardCommit(Collections.singletonList(standardChange(
                    MemoryStandardField.ZRAM_LOGICAL_CAPACITY, null, null, "32768")),
                    null, false);
        } catch (RuntimeException ignored) {
            // The RED must still prove the old queued drain is not dispatched.
        }
        assertTrue("old drain must not be sent for a newer capacity target",
                claimAll().isEmpty());
        assertTrue("new target must remain blocked or unknown",
                Arrays.asList("Blocked", "Unknown").contains(host1State().getPolicyPlanStatus())
                        || Arrays.asList("Blocked", "Unknown").contains(host1State().getStatus()));
    }

    @Test public void successfulCapacityDrainQueuesPoolPreparationFromFreshAgentInventory() {
        prepareCapacityObservation("Succeeded", false, false, false);
        standardCommit(Collections.singletonList(standardChange(
                MemoryStandardField.ZRAM_LOGICAL_CAPACITY, null, null, "16384")),
                null, false);
        assertEquals(1L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t where t.hostUuid = :host and t.action = :action and t.status = :status",
                Long.class).setParameter("host", "host1").setParameter("action", "drain")
                .setParameter("status", "Queued").getSingleResult()));
        MemoryTaskVO drain = claimAll().stream()
                .filter(task -> "drain".equals(task.getAction())).findFirst().orElse(null);
        assertNotNull("capacity maintenance must claim a drain task", drain);

        String boot = "01234567-89ab-cdef-0123-456789abcdef";
        String pool = "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";
        Map<String, Object> backend = new LinkedHashMap<>();
        backend.put("hostUuid", "host1");
        backend.put("bootId", boot);
        backend.put("observedAt", Instant.now().toString());
        backend.put("status", "AVAILABLE");
        backend.put("poolGeneration", pool);
        backend.put("controlOperationUuid", drain.getUuid());
        backend.put("canPrepareZramPool", true);
        backend.put("zramPoolPreparationQuality", "Fresh");
        backend.put("zramPoolPreparationObservedAt", System.currentTimeMillis());
        backend.put("reasons", Collections.emptyList());
        backend.put("candidates", Collections.emptyList());
        Map<String, Object> actual = new LinkedHashMap<>();
        Map<String, Object> nativeReadback = new LinkedHashMap<>();
        nativeReadback.put("pool_generation", pool); nativeReadback.put("device", "/dev/zram0");
        nativeReadback.put("state", "WITHDRAWN_SWAP_INACTIVE_POOL_PRESERVED");
        nativeReadback.put("active", false); nativeReadback.put("reset_performed", false);
        Map<String, Object> receipt = new LinkedHashMap<>(nativeReadback);
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("hostUuid", "host1"); state.put("bootId", boot);
        state.put("observedAt", Instant.now().toString()); state.put("status", "AVAILABLE");
        state.put("poolGeneration", pool); state.put("controlOperationUuid", drain.getUuid());
        state.put("canPrepareZramPool", true);
        state.put("zramPoolPreparationQuality", "Fresh");
        state.put("zramPoolPreparationObservedAt", System.currentTimeMillis());
        state.put("reasons", Collections.emptyList()); state.put("candidates", Collections.emptyList());
        state.put("phase", "WITHDRAWN_SWAP_INACTIVE_POOL_PRESERVED"); state.put("receipt", receipt);
        state.put("nativeReadback", nativeReadback);

        MemoryAgentResponse response = new MemoryAgentResponse();
        response.operationUuid = drain.getUuid(); response.status = "Succeeded";
        response.setSuccess(true); response.hostUuid = "host1";
        response.appliedRevision = drain.getDesiredRevision(); response.state = state;
        MemoryAgentResponse observation = new MemoryAgentResponse();
        observation.operationUuid = drain.getUuid(); observation.status = "Succeeded";
        observation.setSuccess(true); observation.hostUuid = "host1";
        observation.appliedRevision = drain.getDesiredRevision(); observation.state = state;
        repository.result(drain.getUuid(), "Succeeded", "drain completed", response, observation);

        List<MemoryTaskVO> preparation = repository.transaction(em -> em.createQuery(
                "from MemoryTaskVO t where t.hostUuid = :host and t.action = :action", MemoryTaskVO.class)
                .setParameter("host", "host1").setParameter("action", "prepareZramPool").getResultList());
        assertFalse("fresh eligible inventory must create a pool-preparation sibling", preparation.isEmpty());
        MemoryTaskVO root = repository.findTask(drain.getParentUuid());
        assertNotNull(root);
        assertNotEquals("drain root must not be reported Succeeded before preparation", "Succeeded", root.getStatus());
    }

    @Test public void drainReceiptWithWrongPoolIdentityDoesNotQueuePoolPreparation() {
        prepareCapacityObservation("Succeeded", false, false, false);
        standardCommit(Collections.singletonList(standardChange(
                MemoryStandardField.ZRAM_LOGICAL_CAPACITY, null, null, "16384")),
                null, false);
        MemoryTaskVO drain = claimAll().stream()
                .filter(task -> "drain".equals(task.getAction())).findFirst().orElse(null);
        assertNotNull(drain);
        String boot = "01234567-89ab-cdef-0123-456789abcdef";
        String pool = "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";
        Map<String, Object> nativeReadback = new LinkedHashMap<>();
        nativeReadback.put("pool_generation", pool);
        nativeReadback.put("device", "/dev/zram9");
        nativeReadback.put("state", "WITHDRAWN_SWAP_ACTIVE_POOL_PRESERVED");
        nativeReadback.put("active", false); nativeReadback.put("reset_performed", false);
        Map<String, Object> receipt = new LinkedHashMap<>(nativeReadback);
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("hostUuid", "host1"); state.put("bootId", boot);
        state.put("observedAt", Instant.now().toString()); state.put("status", "AVAILABLE");
        state.put("poolGeneration", pool); state.put("controlOperationUuid", drain.getUuid());
        state.put("canPrepareZramPool", true); state.put("zramPoolPreparationQuality", "Fresh");
        state.put("zramPoolPreparationObservedAt", System.currentTimeMillis());
        state.put("reasons", Collections.emptyList()); state.put("candidates", Collections.emptyList());
        state.put("phase", "WITHDRAWN_SWAP_ACTIVE_POOL_PRESERVED"); state.put("receipt", receipt);
        state.put("nativeReadback", nativeReadback);
        MemoryAgentResponse response = new MemoryAgentResponse();
        response.operationUuid = drain.getUuid(); response.status = "Succeeded";
        response.setSuccess(true); response.hostUuid = "host1";
        response.appliedRevision = drain.getDesiredRevision(); response.state = state;
        repository.result(drain.getUuid(), "Succeeded", "drain completed", response, response);
        assertTrue(repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t where t.hostUuid = :host and t.action = :action", Long.class)
                .setParameter("host", "host1").setParameter("action", "prepareZramPool").getSingleResult() == 0L));
    }

    @Test public void writebackBackendCapacityChangeRequiresBackendPreparationBeforeRamPoolPreparation() {
        assertWritebackCapacityChange(null);
    }

    @Test public void automaticCapacityDrainQueryContinuesOriginalRootWithoutReplayingDrain() {
        assertWritebackCapacityChange("drain");
    }

    @Test public void automaticCapacityDrainUsesFlatBackendWhenResourceIsNull() {
        assertWritebackCapacityChange("drain-null-resource");
    }

    @Test public void automaticCapacityDrainPersistedWithTheWholeBackendViewStillContinues() {
        assertWritebackCapacityChange("drain-persisted-view");
    }

    @Test public void automaticCapacityPreparationQueryContinuesOriginalRootWithoutReplayingPreparation() {
        assertWritebackCapacityChange("preparation");
    }

    @Test public void automaticCapacityActivationQueryRestoresWritebackUnderOriginalRoot() {
        assertWritebackCapacityChange("activation");
    }

    @Test public void automaticCapacityDrainRejectsDifferentBackendOwner() {
        assertWritebackCapacityChange("drain-owner-mismatch");
    }

    @Test public void automaticCapacityDrainRejectsExpiredBackendInventory() {
        assertWritebackCapacityChange("drain-expired-inventory");
    }

    @Test public void automaticCapacityDrainDoesNotTrustUnknownInventoryWithCompleteFields() {
        assertWritebackCapacityChange("drain-unknown-inventory");
    }

    @Test public void automaticCapacityPreparationRejectsReplacedBackendResource() {
        assertWritebackCapacityChange("preparation-owner-mismatch");
    }

    @Test public void automaticCapacityActivationRejectsChangedBoot() {
        assertWritebackCapacityChange("activation-boot-mismatch");
    }

    @Test public void automaticCapacityActivationRejectsWrongNativeCapacity() {
        assertWritebackCapacityChange("activation-capacity-mismatch");
    }

    @Test public void automaticCapacityActivationDoesNotEnableDifferentBackend() {
        assertWritebackCapacityChange("activation-backend-mismatch");
    }

    private void assertWritebackCapacityChange(String queryStage) {
        boolean persistedView = "drain-persisted-view".equals(queryStage);
        boolean nullResource = "drain-null-resource".equals(queryStage);
        if (persistedView || nullResource) { queryStage = "drain"; }
        prepareCapacityObservation("Succeeded", false, false, false);
        String resource = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        String fingerprint = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
        String localFingerprint = "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc";
        String boot = "01234567-89ab-cdef-0123-456789abcdef";
        String pool = "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";
        String candidateId = "candidate:" + localFingerprint;
        repository.transaction(em -> {
            writeGlobal(em, "writeback.enabled", true);
            writeOverride(em, "host1", "HostVO", "writeback.backendResourceUuid", resource);
            writeOverride(em, "host1", "HostVO", "writeback.backendCapacityBytes", 8192L);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setCapabilities("{\"supported\":true,\"ksm\":true,\"zram\":true,\"writeback\":true}");
            com.google.gson.JsonObject current = new com.google.gson.JsonParser().parse(state.getState()).getAsJsonObject();
            com.google.gson.JsonObject backend = new com.google.gson.JsonObject();
            backend.addProperty("resource_uuid", resource); backend.addProperty("state", "ACTIVE");
            backend.addProperty("active", true); backend.addProperty("fingerprint", fingerprint);
            backend.addProperty("capacity_bytes", 8192L); backend.addProperty("observed_at", Instant.now().toString());
            // The Agent reports the service backend view, which wraps the resource.
            com.google.gson.JsonObject view = new com.google.gson.JsonObject(); view.add("backend_resource", backend);
            if (nullResource) { view = backend.deepCopy(); view.add("backend_resource", com.google.gson.JsonNull.INSTANCE); }
            com.google.gson.JsonObject writeback = new com.google.gson.JsonObject(); writeback.add("backend", view);
            current.getAsJsonObject("actual").add("writeback", writeback); state.setState(current.toString());
            state.setAppliedPolicy(MemoryPolicyRules.merge(state.getAppliedPolicy(),
                    "{\"schemaVersion\":1,\"writeback\":{\"enabled\":true,\"backendResourceUuid\":\""
                    + resource + "\",\"backendCapacityBytes\":8192}}", "Host"));
            state.setLastSampleTime(System.currentTimeMillis());
            return null;
        });
        standardCommit(Collections.singletonList(
                standardChange(MemoryStandardField.ZRAM_LOGICAL_CAPACITY, "host1", "HostVO", "16384")),
                null, false);
        MemoryTaskVO drain = claimAll().stream()
                .filter(task -> "drain".equals(task.getAction())).findFirst().orElse(null);
        assertNotNull("capacity change with an existing backend must drain first: "
                + host1State().getPolicyBlockedFields(), drain);
        if (persistedView) {
            repository.transaction(em -> {
                MemoryTaskVO stored = em.find(MemoryTaskVO.class, drain.getUuid());
                com.google.gson.JsonObject request = new com.google.gson.JsonParser().parse(stored.getPolicy()).getAsJsonObject();
                com.google.gson.JsonObject view = new com.google.gson.JsonObject();
                view.add("backend_resource", request.get("backend"));
                request.add("backend", view);
                stored.setPolicy(request.toString());
                return null;
            });
        }
        Map<String, Object> stableIdentity = new LinkedHashMap<>();
        stableIdentity.put("bootId", boot); stableIdentity.put("device", "/dev/vdb");
        stableIdentity.put("byIdPath", "/dev/disk/by-id/local"); stableIdentity.put("majorMinor", "252:16");
        stableIdentity.put("serial", "serial-local"); stableIdentity.put("wwn", "");
        stableIdentity.put("capacityBytes", 8192L); stableIdentity.put("transport", "virtio");
        stableIdentity.put("sysfsPath", "/sys/devices/local"); stableIdentity.put("sysfsInode", "1");
        stableIdentity.put("rdev", 64528L); stableIdentity.put("nodeInode", 2L);
        Map<String, Object> ownerProof = new LinkedHashMap<>();
        ownerProof.put("schemaVersion", 1); ownerProof.put("hostUuid", "host1");
        ownerProof.put("controlOperationUuid", drain.getUuid()); ownerProof.put("resourceUuid", resource);
        ownerProof.put("fingerprint", fingerprint); ownerProof.put("stableIdentity", stableIdentity);
        ownerProof.put("bootId", boot); ownerProof.put("poolGeneration", pool);
        ownerProof.put("capacityBytes", 8192L); ownerProof.put("zramDevice", "/dev/zram0");
        ownerProof.put("candidateIdentity", fingerprint); ownerProof.put("candidateState", "ACTIVE");
        ownerProof.put("ownershipGeneration", resource);
        ownerProof.put("observedAt", Instant.now().toString());
        Map<String, Object> nativeReadback = new LinkedHashMap<>();
        nativeReadback.put("pool_generation", pool);
        nativeReadback.put("device", "/dev/zram0"); nativeReadback.put("state", "WITHDRAWN_SWAP_INACTIVE_POOL_PRESERVED");
        nativeReadback.put("active", false); nativeReadback.put("reset_performed", false);
        Map<String, Object> receiptState = new LinkedHashMap<>();
        receiptState.put("receipt", nativeReadback); receiptState.put("nativeReadback", nativeReadback);
        receiptState.put("backendOwnerProof", ownerProof);
        MemoryAgentResponse response = new MemoryAgentResponse(); response.operationUuid = drain.getUuid();
        response.status = "Succeeded"; response.setSuccess(true); response.hostUuid = "host1";
        response.appliedRevision = drain.getDesiredRevision(); response.state = receiptState;
        MemoryAgentResponse observation = new MemoryAgentResponse(); observation.status = "Succeeded";
        observation.setSuccess(true); observation.hostUuid = "host1"; observation.bootId = boot;
        observation.sampleTime = System.currentTimeMillis(); observation.state = new LinkedHashMap<>();
        observation.state.put("hostUuid", "host1"); observation.state.put("bootId", boot);
        observation.state.put("observedAt", Instant.now().toString()); observation.state.put("status", "AVAILABLE");
        observation.state.put("poolGeneration", pool); observation.state.put("controlOperationUuid", drain.getUuid());
        observation.state.put("canPrepare", true); observation.state.put("canPrepareZramPool", false);
        Map<String, Object> candidate = new LinkedHashMap<>();
        candidate.put("resourceUuid", null); candidate.put("identity", candidateId);
        candidate.put("fingerprint", localFingerprint); candidate.put("stableIdentity", stableIdentity);
        candidate.put("eligible", true); candidate.put("writebackReady", false); candidate.put("active", false);
        candidate.put("type", "block"); candidate.put("device", "/dev/vdb");
        candidate.put("state", "NEEDS_OFFLINE_PREPARATION"); candidate.put("hostBootId", boot); candidate.put("capacityBytes", 8192L);
        observation.state.put("candidates", Collections.singletonList(candidate));
        if ("drain-owner-mismatch".equals(queryStage)) { ownerProof.put("ownershipGeneration", "different-owner"); }
        if ("drain-expired-inventory".equals(queryStage)) {
            observation.state.put("observedAt", Instant.now().minusMillis(
                    MemoryOptimizationGlobalConfig.displayTtlMillis() + 1000).toString());
        }
        if ("drain-unknown-inventory".equals(queryStage)) { observation.status = "Unknown"; }
        MemoryTaskVO drainResult = capacityResultTask(drain, "drain".equals(queryStage));
        repository.result(drainResult.getUuid(), "Succeeded", "drain completed", response, observation);
        if (queryStage != null && queryStage.startsWith("drain-")) {
            assertCapacityFailureRemainsUnresolved(drain, 1L);
            return;
        }
        assertTrue("writeback backend must be prepared before RAM pool preparation",
                repository.transaction(em -> em.createQuery(
                        "select count(t) from MemoryTaskVO t where t.parentUuid=:p and t.action='prepareWritebackBackend'",
                        Long.class).setParameter("p", drain.getParentUuid()).getSingleResult() > 0L));
        assertEquals(0L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t where t.parentUuid=:p and t.action='prepareZramPool'",
                Long.class).setParameter("p", drain.getParentUuid()).getSingleResult()));
        MemoryTaskVO preparation = claimAll().stream()
                .filter(task -> "prepareWritebackBackend".equals(task.getAction())).findFirst().orElse(null);
        assertNotNull(preparation);
        MemoryTaskVO restore = assertCapacityPreparationQueuesRestore(drain, preparation, queryStage);
        if ("preparation-owner-mismatch".equals(queryStage)) { return; }
        assertTrue(claimAll().stream().anyMatch(task -> task.getUuid().equals(restore.getUuid())));
        MemoryAgentResponse activated = new MemoryAgentResponse();
        activated.operationUuid = restore.getUuid(); activated.status = "Succeeded"; activated.setSuccess(true);
        activated.hostUuid = "host1"; activated.bootId = boot;
        activated.appliedRevision = restore.getDesiredRevision(); activated.sampleTime = System.currentTimeMillis();
        activated.capabilities = new LinkedHashMap<>();
        for (String capability : Arrays.asList("supported", "ksm", "ksmZeroPages", "zram", "writeback")) {
            activated.capabilities.put(capability, true);
        }
        activated.state = completeState(0L, "activated");
        activated.state.put("bootId", boot);
        activated.state.put("lastConfirmedOperationUuid", restore.getUuid());
        activated.state.put("lastConfirmedAppliedRevision", restore.getDesiredRevision());
        activated.state.put("poolGeneration", "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc");
        Map<String, Object> lifecycle = new LinkedHashMap<>();
        lifecycle.put("paused", false); lifecycle.put("activeState", "running");
        activated.state.put("lifecycle", lifecycle);
        Map<String, Object> capacity = new LinkedHashMap<>();
        capacity.put("effective_logical_bytes", 16384L); capacity.put("effective_physical_limit_bytes", 4096L);
        Map<String, Object> zram = new LinkedHashMap<>();
        zram.put("enabled", true); zram.put("efficiencyEnabled", true); zram.put("capacity", capacity);
        Map<String, Object> backend = new LinkedHashMap<>();
        backend.put("resource_uuid", resource); backend.put("state", "ACTIVE"); backend.put("active", true);
        backend.put("fingerprint", fingerprint); backend.put("capacity_bytes", 8192L);
        backend.put("observed_at", Instant.now().toString());
        Map<String, Object> actual = (Map<String, Object>) activated.state.get("actual");
        actual.put("zram", zram);
        actual.put("writeback", Collections.singletonMap("backend", Collections.singletonMap("backend_resource", backend)));
        if ("activation-boot-mismatch".equals(queryStage)) { activated.bootId = "different-boot"; }
        if ("activation-capacity-mismatch".equals(queryStage)) { capacity.put("effective_logical_bytes", 8192L); }
        if ("activation-backend-mismatch".equals(queryStage)) { backend.put("resource_uuid", "different-owner"); }
        MemoryTaskVO activationResult = capacityResultTask(restore, "activation".equals(queryStage));
        repository.result(activationResult.getUuid(), "Succeeded", "pool activated", activated, activated);
        if (queryStage != null && queryStage.startsWith("activation-")) {
            assertCapacityFailureRemainsUnresolved(restore, 3L);
            return;
        }
        MemoryTaskVO writeback = repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            return state.getActiveTaskUuid() == null ? null : em.find(MemoryTaskVO.class, state.getActiveTaskUuid());
        });
        assertNotNull("activation must queue retained writeback before closing the root", writeback);
        assertEquals(drain.getParentUuid(), writeback.getParentUuid());
        assertEquals(writeback.getReason(), "Queued", writeback.getStatus());
        assertEquals(Boolean.TRUE, MemoryPolicyRules.decode(writeback.getPolicy()).writeback.enabled);
        assertEquals(resource, MemoryPolicyRules.decode(writeback.getPolicy()).writeback.backendResourceUuid);
        assertNotEquals("Succeeded", repository.findTask(drain.getParentUuid()).getStatus());
        repository.result(activationResult.getUuid(), "Succeeded", "duplicate activation", activated, activated);
        assertEquals(writeback.getUuid(), host1State().getActiveTaskUuid());
        assertTrue(claimAll().stream()
                .anyMatch(task -> task.getUuid().equals(writeback.getUuid())));
        MemoryAgentResponse enabled = new MemoryAgentResponse();
        enabled.operationUuid = writeback.getUuid(); enabled.status = "Succeeded"; enabled.setSuccess(true);
        enabled.appliedRevision = writeback.getDesiredRevision();
        repository.result(writeback.getUuid(), "Succeeded", "writeback restored", enabled);
        assertEquals("writeback restore must close the root; restore=" + restore.getStatus() + "/"
                + restore.getReason() + ", host=" + host1State().getActiveTaskUuid() + "/"
                + host1State().getStatus() + "/" + host1State().getReason() + ", children="
                + repository.transaction(em -> em.createQuery(
                        "select concat(t.hostUuid, '/', t.action, '/', t.status, '/', t.desiredRevision, '/', t.policy) "
                                + "from MemoryTaskVO t where t.parentUuid=:p order by t.uuid", String.class)
                        .setParameter("p", drain.getParentUuid()).getResultList()),
                "Succeeded", repository.findTask(drain.getParentUuid()).getStatus());
        assertEquals(Boolean.TRUE, MemoryPolicyRules.decode(host1State().getAppliedPolicy()).writeback.enabled);
    }

    private void assertCapacityFailureRemainsUnresolved(MemoryTaskVO operation, long children) {
        assertEquals("Unknown", repository.findTask(operation.getUuid()).getStatus());
        assertEquals(operation.getUuid(), host1State().getActiveTaskUuid());
        assertEquals("Unknown", repository.findTask(operation.getParentUuid()).getStatus());
        assertEquals(children, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t where t.parentUuid=:parent", Long.class)
                .setParameter("parent", operation.getParentUuid()).getSingleResult()));
    }

    private MemoryTaskVO capacityResultTask(MemoryTaskVO original, boolean query) {
        if (!query) { return original; }
        repository.result(original.getUuid(), "Unknown", "transport lost after execution", null);
        MemoryActionRequest reconcile = request();
        reconcile.setScope("Host"); reconcile.setResourceUuid("host1");
        reconcile.setAction("reconcile"); reconcile.setPolicy("{}");
        String accepted = repository.submit(reconcile);
        MemoryTaskVO alias = claimAll().stream()
                .filter(task -> accepted.equals(task.getUuid()) || accepted.equals(task.getParentUuid())).findFirst().get();
        assertEquals(original.getUuid(), alias.getReconcileOperationUuid());
        return alias;
    }

    private MemoryTaskVO assertCapacityPreparationQueuesRestore(MemoryTaskVO drain, MemoryTaskVO preparation,
            String queryStage) {
        return assertCapacityPreparationQueuesRestore(drain, preparation, queryStage, drain.getPolicy(), drain.getParentUuid());
    }

    private MemoryTaskVO assertCapacityPreparationQueuesRestore(MemoryTaskVO drain, MemoryTaskVO preparation,
            String queryStage, String contextPolicy, String rootUuid) {
        com.google.gson.JsonObject context = new com.google.gson.JsonParser().parse(contextPolicy).getAsJsonObject();
        String boot = context.get("bootId").getAsString(), pool = context.get("poolGeneration").getAsString();
        Map<String, Object> proof = new LinkedHashMap<>();
        proof.put("drainOperationUuid", drain.getUuid()); proof.put("oldPoolGeneration", pool);
        proof.put("maintenanceReady", true); proof.put("maintenanceArchived", true);
        proof.put("executorExited", true); proof.put("activeOperations", 0);
        proof.put("readyForInitialization", true); proof.put("originalDeviceInactive", true);
        proof.put("oldPoolOwnershipAbsent", true); proof.put("poolOwnedByService", false);
        Map<String, Object> readyState = new LinkedHashMap<>();
        readyState.put("managed", true);
        readyState.put("phase", "POOL_READY"); readyState.put("bootId", boot);
        readyState.put("lastConfirmedOperationUuid", drain.getUuid());
        readyState.put("lastConfirmedAppliedRevision", drain.getDesiredRevision());
        Map<String, Object> readyLifecycle = new LinkedHashMap<>();
        readyLifecycle.put("paused", true); readyLifecycle.put("activeState", "paused");
        readyState.put("lifecycle", readyLifecycle);
        readyState.put("maintenanceProof", proof);
        readyState.putAll(completeState(0L, "ready"));
        Map<String, Object> readyActual = new HashMap<>((Map<String, Object>) readyState.get("actual"));
        Map<String, Object> readyZram = new HashMap<>((Map<String, Object>) readyActual.get("zram"));
        readyZram.put("enabled", false);
        readyZram.put("efficiencyEnabled", false);
        readyActual.put("zram", readyZram);
        readyState.put("actual", readyActual);
        readyState.put("phase", "POOL_READY"); readyState.put("bootId", boot);
        readyState.put("lastConfirmedOperationUuid", drain.getUuid());
        readyState.put("lastConfirmedAppliedRevision", drain.getDesiredRevision());
        readyState.put("lifecycle", readyLifecycle);
        MemoryAgentResponse prepared = new MemoryAgentResponse();
        prepared.operationUuid = preparation.getUuid(); prepared.status = "Succeeded";
        prepared.setSuccess(true);
        Map<String, Object> prepareState = new LinkedHashMap<>();
        prepareState.put("phase", "APPLIED"); prepareState.put("writebackReady", false);
        Map<String, Object> prepareReceipt = new LinkedHashMap<>();
        prepareReceipt.put("stage", "APPLIED"); prepareReceipt.put("request_id", preparation.getUuid());
        if ("prepareZramPool".equals(preparation.getAction())) {
            prepareReceipt.put("expected_control_operation_uuid", drain.getUuid());
        } else {
            com.google.gson.JsonObject request = new com.google.gson.JsonParser()
                    .parse(preparation.getPolicy()).getAsJsonObject();
            prepareReceipt.put("candidate_id", request.get("candidateId").getAsString());
            prepareReceipt.put("backend_resource_uuid", context.getAsJsonObject("backend")
                    .get("resource_uuid").getAsString());
            Map<String, Object> identity = new LinkedHashMap<>();
            identity.put("boot_id", boot); identity.put("canonical_device", "/dev/vdb");
            identity.put("capacity_bytes", request.get("backendCapacityBytes").getAsLong());
            prepareReceipt.put("target_identity", identity);
        }
        prepareReceipt.put("host_boot_id", boot); prepareReceipt.put("pool_generation", pool);
        prepareState.put("maintenance", prepareReceipt); prepared.state = prepareState;
        MemoryAgentResponse observation = new MemoryAgentResponse();
        observation.status = "Succeeded"; observation.setSuccess(true); observation.hostUuid = "host1";
        observation.capabilities = new LinkedHashMap<>(); observation.capabilities.put("supported", true);
        observation.capabilities.put("ksm", true); observation.capabilities.put("ksmZeroPages", true);
        observation.capabilities.put("zram", true); observation.capabilities.put("writeback", false);
        observation.appliedRevision = drain.getDesiredRevision(); observation.sampleTime = System.currentTimeMillis();
        observation.bootId = boot; observation.state = readyState;
        if ("preparation-owner-mismatch".equals(queryStage)) { prepareReceipt.put("backend_resource_uuid", "different-owner"); }
        MemoryTaskVO resultTask = capacityResultTask(preparation, "preparation".equals(queryStage));
        repository.result(resultTask.getUuid(), "Succeeded", "pool prepared", prepared, observation);
        if ("preparation-owner-mismatch".equals(queryStage)) {
            assertCapacityFailureRemainsUnresolved(preparation, 2L);
            return null;
        }

        MemoryTaskVO restore = repository.transaction(em -> {
            List<MemoryTaskVO> rows = em.createQuery(
                "from MemoryTaskVO t where t.parentUuid = :parent and t.action in ('apply', 'clearOverride') "
                        + "and t.expectedControlOperationUuid = :expected",
                    MemoryTaskVO.class).setParameter("parent", rootUuid)
                .setParameter("expected", drain.getUuid())
                .setMaxResults(1).getResultList();
            return rows.isEmpty() ? null : rows.get(0);
        });
        assertNotNull("successful preparation must queue the retained restore target; prep="
                + repository.findTask(preparation.getUuid()).getStatus() + "/"
                + repository.findTask(preparation.getUuid()).getReason() + ", host="
                + host1State().getStatus() + "/" + host1State().getReason()
                + ", drainPolicy=" + drain.getPolicy() + ", effective="
                + effectivePolicy("Host", "host1"), restore);
        assertTrue("restore action must remain a standard activation action: " + restore.getAction(),
                "apply".equals(restore.getAction()) || "clearOverride".equals(restore.getAction()));
        assertEquals("restore=" + restore.getUuid() + ", action=" + restore.getAction()
                + ", parent=" + restore.getParentUuid() + ", expected=" + restore.getExpectedControlOperationUuid(),
                "Queued", restore.getStatus());
        assertEquals(rootUuid, restore.getParentUuid());
        assertEquals(drain.getUuid(), restore.getExpectedControlOperationUuid());
        return restore;
    }

    @Test public void standardZramCapacityChangeWithMatchingEffectiveCapacityDoesNotDrain() {
        prepareCapacityObservation("Succeeded", false, false, false);

        standardCommit(Collections.singletonList(standardChange(
                MemoryStandardField.ZRAM_LOGICAL_CAPACITY, null, null, "8192")),
                null, false);

        assertTrue("matching effective capacity must not queue a drain", repository.transaction(em ->
                em.createQuery("select count(t) from MemoryTaskVO t where t.hostUuid = :host and t.action = :action", Long.class)
                        .setParameter("host", "host1").setParameter("action", "drain").getSingleResult() == 0L));
    }

    private void prepareCapacityObservation(String status, boolean paused, boolean stale, boolean ksmOnly) {
        prepareCapacityObservation(status, paused, stale, ksmOnly, 8192L, 4096L);
    }

    private void prepareCapacityObservation(String status, boolean paused, boolean stale, boolean ksmOnly,
                                            long effectiveLogical, long effectivePhysical) {
        registerStandardResources();
        repository.transaction(em -> {
            writeGlobal(em, "zram.enabled", true);
            writeGlobal(em, "zram.logicalCapacityBytes", 8192L);
            writeGlobal(em, "zram.ramLimitBytes", 4096L);
            writeGlobal(em, "writeback.enabled", false);
            MemoryTaskVO control = testTask("0123456789abcdef0123456789abcdef", null, "apply", "Succeeded", null);
            em.persist(control);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus(status); state.setDesiredRevision(0L); state.setAppliedRevision(0L);
            state.setControlOperationUuid(control.getUuid());
            long sample = stale ? System.currentTimeMillis() - 120_000L : System.currentTimeMillis();
            String boot = "01234567-89ab-cdef-0123-456789abcdef";
            String pool = "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";
            state.setAppliedPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":false,"
                    + "\"pagesToScan\":2000,\"sleepMillis\":20},\"zram\":{\"enabled\":true,"
                    + "\"logicalCapacityBytes\":8192,\"ramLimitBytes\":4096},\"writeback\":{\"enabled\":false}}");
            state.setAppliedPolicyHash(sha256(state.getAppliedPolicy()));
            state.setState("{\"bootId\":\"" + boot + "\",\"managed\":true,"
                    + "\"lastConfirmedOperationUuid\":\"" + control.getUuid() + "\","
                    + "\"lastConfirmedAppliedRevision\":0,\"ksmOnly\":" + ksmOnly + ","
                    + "\"lifecycle\":{\"paused\":" + paused + ",\"activeState\":\""
                    + (paused ? "paused" : "active") + "\"},\"poolGeneration\":\"" + pool + "\","
                    + "\"host_zram\":{\"quality\":\"native_host_device\","
                    + "\"device\":\"/dev/zram0\",\"pool_generation\":\"" + pool + "\","
                    + "\"observed_at\":\"" + Instant.now() + "\"},"
                    + "\"actual\":{\"zram\":{\"efficiencyEnabled\":true,"
                    + "\"capacity\":{\"effective_logical_bytes\":" + effectiveLogical + ","
                    + "\"effective_physical_limit_bytes\":" + effectivePhysical
                    + ",\"apply_state\":\"APPLIED\"}}}}");
            state.setLastSampleTime(sample);
            return null;
        });
    }

    @Test public void disconnectedCapacitySaveReconnectQueuesDrainFromFreshPoolObservation() {
        prepareCapacityObservation("Succeeded", false, false, false);
        repository.transaction(em -> {
            em.find(TestHost.class, "host1").status = HostStatus.Disconnected;
            return null;
        });
        standardCommit(Collections.singletonList(standardChange(
                MemoryStandardField.ZRAM_LOGICAL_CAPACITY, null, null, "16384")),
                null, false);
        repository.transaction(em -> {
            em.find(TestHost.class, "host1").status = HostStatus.Connected;
            return null;
        });
        assertTrue("reconnect must queue drain for a changed effective capacity",
                repository.applyEffectivePolicyAfterConnect("host1"));
        MemoryTaskVO child = claimAll().stream()
                .filter(task -> "host1".equals(task.getHostUuid())).findFirst().orElse(null);
        assertNotNull(child);
        assertEquals("drain", child.getAction());
    }

    @Test public void noOpDoesNotClearBlockedPlanForUnknownHost() {
        prepareNoopBlockedProtection();
        repository.transaction(em -> { em.find(MemoryStateVO.class, "host1").setStatus("Unknown"); return null; });
        assertNoopKeepsBlockedPlan();
    }

    @Test public void noOpDoesNotClearBlockedPlanForActiveHostOperation() {
        prepareNoopBlockedProtection();
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setActiveTaskUuid("noop-active");
            em.persist(testTask("noop-active", "host1", "apply", "Applying", null));
            return null;
        });
        assertNoopKeepsBlockedPlan();
    }

    @Test public void noOpDoesNotClearNeedsReviewProtection() {
        prepareNoopBlockedProtection();
        repository.transaction(em -> { em.find(MemoryStateVO.class, "host1").setPolicyPlanStatus("NeedsReview"); return null; });
        assertNoopKeepsPlan("NeedsReview");
    }

    @Test public void noOpDoesNotClearBlockedPlanForPausedHost() {
        prepareNoopBlockedProtection();
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setControlOperationUuid("noop-pause");
            state.setState("{\"lifecycle\":{\"paused\":true,\"activeState\":\"paused\"}}");
            em.persist(testTask("noop-pause", null, "pause", "Succeeded", null));
            return null;
        });
        assertNoopKeepsBlockedPlan();
    }

    @Test public void noOpDoesNotClearBlockedPlanForStaleHostObservation() {
        prepareNoopBlockedProtection();
        repository.transaction(em -> { em.find(MemoryStateVO.class, "host1")
                .setLastSampleTime(System.currentTimeMillis() - 120_000L); return null; });
        assertNoopKeepsBlockedPlan();
    }

    private void prepareNoopBlockedProtection() {
        prepareCapacityObservation("Succeeded", false, false, false);
        repository.transaction(em -> {
            writeGlobal(em, "ksm.enabled", false);
            em.find(MemoryStateVO.class, "host1").setPolicyPlanStatus("Blocked");
            return null;
        });
    }

    private void assertNoopKeepsBlockedPlan() { assertNoopKeepsPlan("Blocked"); }

    private void assertNoopKeepsPlan(String expected) {
        standardCommit(Collections.singletonList(standardChange(
                MemoryStandardField.ZRAM_LOGICAL_CAPACITY, null, null, "8192")),
                null, false);
        assertEquals(expected, host1State().getPolicyPlanStatus());
    }

    @Test public void capacityChangeDoesNotDrainUnknownHost() {
        prepareCapacityObservation("Unknown", false, false, false);
        standardCommit(Collections.singletonList(standardChange(
                MemoryStandardField.ZRAM_LOGICAL_CAPACITY, null, null, "16384")),
                null, false);
        assertEquals("Blocked", host1State().getPolicyPlanStatus());
        assertEquals(0L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t where t.action = :action", Long.class)
                .setParameter("action", "drain").getSingleResult()));
    }

    @Test public void capacityChangeDoesNotDrainPausedHost() {
        prepareCapacityObservation("Succeeded", true, false, false);
        standardCommit(Collections.singletonList(standardChange(
                MemoryStandardField.ZRAM_LOGICAL_CAPACITY, null, null, "16384")),
                null, false);
        assertEquals("Blocked", host1State().getPolicyPlanStatus());
        assertEquals(0L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t where t.action = :action", Long.class)
                .setParameter("action", "drain").getSingleResult()));
    }

    @Test public void capacityChangeDoesNotDrainStaleObservation() {
        prepareCapacityObservation("Succeeded", false, true, false);
        standardCommit(Collections.singletonList(standardChange(
                MemoryStandardField.ZRAM_LOGICAL_CAPACITY, null, null, "16384")),
                null, false);
        assertEquals("Blocked", host1State().getPolicyPlanStatus());
        assertEquals(0L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t where t.action = :action", Long.class)
                .setParameter("action", "drain").getSingleResult()));
    }

    @Test public void ksmOnlyChangeDoesNotDrainZramPool() {
        prepareCapacityObservation("Succeeded", false, false, true);
        standardCommit(Collections.singletonList(standardChange(
                MemoryStandardField.KSM_ZERO_PAGES, null, null, "true")),
                null, false);
        assertEquals(0L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t where t.action = :action", Long.class)
                .setParameter("action", "drain").getSingleResult()));
    }

    private void seedCompatibilityDeltaBaseline(boolean zramEnabled) {
        repository.transaction(em -> {
            writeGlobal(em, "ksm.enabled", true);
            writeGlobal(em, "ksm.zeroPagesEnabled", false);
            writeGlobal(em, "zram.enabled", zramEnabled);
            writeGlobal(em, "writeback.enabled", false);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setDesiredRevision(1L);
            state.setAppliedRevision(1L);
            state.setAppliedPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":true,\"zeroPagesEnabled\":false,"
                    + "\"pagesToScan\":2000,\"sleepMillis\":20},\"zram\":{\"enabled\":"
                    + zramEnabled + "},\"writeback\":{\"enabled\":false}}");
            state.setAppliedPolicyHash("compatibility-delta-baseline");
            return null;
        });
    }

    private Map<String, Object> dispatchCompatibilityTask(MemoryTaskVO task) throws Exception {
        MemoryOptimizationManager manager = new MemoryOptimizationManager();
        Field repositoryField = MemoryOptimizationManager.class.getDeclaredField("repository");
        repositoryField.setAccessible(true); repositoryField.set(manager, repository);
        MemoryLicenseExtensionPoint license = () -> System.currentTimeMillis() + 3600000L;
        PluginRegistry licensedRegistry = (PluginRegistry) Proxy.newProxyInstance(
                PluginRegistry.class.getClassLoader(), new Class<?>[]{PluginRegistry.class},
                (proxy, method, args) -> "getExtensionList".equals(method.getName())
                        && args != null && args.length == 1 && args[0] == MemoryLicenseExtensionPoint.class
                        ? Collections.singletonList(license) : Collections.emptyList());
        Field registryField = MemoryOptimizationManager.class.getDeclaredField("pluginRgty");
        registryField.setAccessible(true); registryField.set(manager, licensedRegistry);
        List<org.zstack.kvm.KVMHostAsyncHttpCallMsg> requests = new ArrayList<>();
        CloudBus bus = (CloudBus) Proxy.newProxyInstance(CloudBus.class.getClassLoader(),
                new Class<?>[]{CloudBus.class}, (proxy, method, args) -> {
                    if ("send".equals(method.getName()) && args != null && args.length == 2
                            && args[0] instanceof org.zstack.kvm.KVMHostAsyncHttpCallMsg) {
                        requests.add((org.zstack.kvm.KVMHostAsyncHttpCallMsg) args[0]);
                    }
                    if (method.getReturnType() == boolean.class) { return false; }
                    if (method.getReturnType() == int.class) { return 0; }
                    if (method.getReturnType() == long.class) { return 0L; }
                    return null;
                });
        Field busField = MemoryOptimizationManager.class.getDeclaredField("bus");
        busField.setAccessible(true); busField.set(manager, bus);
        java.lang.reflect.Method dispatch = MemoryOptimizationManager.class
                .getDeclaredMethod("dispatch", MemoryTaskVO.class, boolean.class, Runnable.class);
        dispatch.setAccessible(true); dispatch.invoke(manager, task, false, (Runnable) () -> { });
        assertEquals(1, requests.size());
        assertEquals("host1", requests.get(0).getHostUuid());
        return org.zstack.utils.gson.JSONObjectUtil.toObject(requests.get(0).getCommand(), Map.class);
    }

    @Test public void standardWritesPreservePauseFenceEvenWithUnpausedObservation() {
        registerStandardResources();
        repository.transaction(em -> {
            em.persist(testTask("standard-pause", null, "pause", "Succeeded", null));
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setControlOperationUuid("standard-pause");
            state.setState("{\"lifecycle\":{\"paused\":false,\"activeState\":\"running\"}}");
            return null;
        });
        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.KSM_ENABLED,
                "host1", "HostVO", "true")), null, false);
        assertNull(host1State().getActiveTaskUuid());
        assertEquals("standard-pause", host1State().getControlOperationUuid());
        assertEquals("Blocked", host1State().getPolicyPlanStatus());
        assertTrue(claimAll().isEmpty());
        assertFalse("reconnection cannot bypass the same persistent pause fence",
                repository.applyEffectivePolicyAfterConnect("host1"));
        assertEquals("standard-pause", host1State().getControlOperationUuid());
    }

    @Test public void completedDrainWithReadyDeviceActivatesOnlyThroughRecovery() {
        registerStandardResources();
        repository.transaction(em -> {
            em.persist(testTask("completed-drain", null, "drain", "Succeeded", null));
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Succeeded"); state.setControlOperationUuid("completed-drain");
            state.setLastSampleTime(System.currentTimeMillis());
            state.setState("{\"bootId\":\"boot-a\",\"lastConfirmedOperationUuid\":\"completed-drain\","
                    + "\"lifecycle\":{\"paused\":true,\"activeState\":\"paused\"},"
                    + "\"maintenanceProof\":{\"maintenanceReady\":true,\"maintenanceArchived\":true,"
                    + "\"drainOperationUuid\":\"completed-drain\",\"oldPoolGeneration\":\"old-pool\","
                    + "\"activeOperations\":0,\"poolOwnedByService\":false,\"readyForInitialization\":true,"
                    + "\"originalDeviceInactive\":true,\"oldPoolOwnershipAbsent\":true,\"executorExited\":true}}");
            return null;
        });
        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.KSM_ENABLED,
                "host1", "HostVO", "true")), null, false);
        assertNull("Agent maintenance apply requires an enabled ZRAM initialization payload",
                host1State().getActiveTaskUuid());
        assertEquals("completed-drain", host1State().getControlOperationUuid());
        assertEquals("Blocked", host1State().getPolicyPlanStatus());
        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.ZRAM_ENABLED,
                "host1", "HostVO", "true")), null, false);
        assertNull("planning alone does not undo a drain", host1State().getActiveTaskUuid());
        MemoryAgentResponse inventory = drainInventory(null, false, Collections.singletonList("DRAIN_PROOF_UNAVAILABLE"));
        inventory.state.put("bootId", "boot-a"); inventory.state.put("controlOperationUuid", "completed-drain");
        repository.recoverDrain("host1", "system:memory-zram-reenabled", inventory);
        assertNotNull(host1State().getActiveTaskUuid());
        assertNotEquals("completed-drain", host1State().getControlOperationUuid());
        MemoryTaskVO activation = claimAll().get(0);
        assertEquals("completed-drain", activation.getExpectedControlOperationUuid());
        assertEquals(Boolean.TRUE, MemoryPolicyRules.decode(activation.getPolicy()).zram.enabled);
    }

    private void prepareAppliedKsmForStandardClear() {
        registerStandardResources();
        repository.transaction(em -> {
            writeOverride(em, "host1", "HostVO", "ksm.enabled", false);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setDesiredRevision(1); state.setAppliedRevision(1L);
            state.setAppliedPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":false}}");
            state.setAppliedPolicyHash("previous-ksm-policy");
            state.setControlOperationUuid("previous-ksm-apply");
            MemoryTaskVO original = testTask("previous-ksm-apply", null, "apply", "Succeeded", null);
            original.setDesiredRevision(1L); original.setPolicy(state.getAppliedPolicy());
            em.persist(original);
            return null;
        });
    }

    private void deleteStandardKsmEnabled() {
        repository.transaction(em -> {
            writeOverride(em, "host1", "HostVO", MemoryStandardField.KSM_ENABLED.path(), null);
            return null;
        });
        repository.standardConfigurationChanged("host1");
    }

    @Test public void reconnectRestoresOnlyPreviouslyConfirmedKsmFieldsAcrossBoot() {
        registerStandardResources();
        String policy = "{\"schemaVersion\":1,\"ksm\":{\"enabled\":true,"
                + "\"zeroPagesEnabled\":true,\"pagesToScan\":3000,\"sleepMillis\":30}}";
        seedConfirmedKsmReconnect(policy);
        long tasksBefore = taskCount();

        assertTrue(repository.applyEffectivePolicyAfterConnect("host1"));
        MemoryTaskVO restore = claimAll().stream()
                .filter(t -> "host1".equals(t.getHostUuid())).findFirst().get();
        assertEquals("apply", restore.getAction());
        assertEquals("confirmed-ksm-r8", restore.getExpectedControlOperationUuid());
        assertEquals("system:memory-host-connect-policy", restore.getActorUuid());
        assertEquals(Long.valueOf(8), host1State().getAppliedRevision());
        assertEquals(9L, restore.getDesiredRevision());
        Map<String, Object> payload = org.zstack.utils.gson.JSONObjectUtil.toObject(restore.getPolicy(), Map.class);
        assertTrue(payload.containsKey("ksm"));
        Map<String, Object> payloadKsm = (Map<String, Object>) payload.get("ksm");
        assertEquals(Boolean.TRUE, payloadKsm.get("enabled"));
        assertEquals(3000L, ((Number) payloadKsm.get("pagesToScan")).longValue());
        assertEquals(30L, ((Number) payloadKsm.get("sleepMillis")).longValue());
        assertEquals(Boolean.TRUE, payloadKsm.get("zeroPagesEnabled"));
        assertFalse("a reconnect KSM restore never piggybacks ZRAM", payload.containsKey("zram"));
        assertFalse("a reconnect KSM restore never piggybacks writeback", payload.containsKey("writeback"));
        assertTrue(repository.isConfirmedKsmReconnectTask(restore.getUuid()));
        assertFalse("duplicate reconnect callbacks cannot create another task",
                repository.applyEffectivePolicyAfterConnect("host1"));
        assertEquals(tasksBefore + 2, taskCount());
    }

    @Test public void reconnectRestoresConfirmedDeltaAgainstTheCompleteAppliedKsmTarget() {
        registerStandardResources();
        String policy = "{\"schemaVersion\":1,\"ksm\":{\"enabled\":false,"
                + "\"zeroPagesEnabled\":true,\"pagesToScan\":2000,\"sleepMillis\":20}}";
        String delta = "{\"schemaVersion\":1,\"ksm\":{\"pagesToScan\":2000,\"sleepMillis\":20}}";
        for (String drift : Arrays.asList("scan", "enabled")) {
            repository.transaction(em -> { em.createQuery("delete from MemoryTaskVO").executeUpdate(); return null; });
            seedConfirmedKsmReconnect(policy);
            repository.transaction(em -> {
                em.find(MemoryTaskVO.class, "confirmed-ksm-r8").setPolicy(delta);
                MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
                state.setAppliedPolicyHash(sha256(delta));
                state.setState("{\"bootId\":\"boot-b\",\"actual\":{\"ksm\":{\"enabled\":"
                        + ("enabled".equals(drift) ? "true" : "false")
                        + ",\"zeroPagesEnabled\":false,\"pagesToScan\":"
                        + ("scan".equals(drift) ? "100" : "2000") + ",\"sleepMillis\":20}}}");
                return null;
            });
            long before = taskCount();
            assertTrue(drift, repository.applyEffectivePolicyAfterConnect("host1"));
            MemoryTaskVO restore = claimAll().stream()
                    .filter(t -> "host1".equals(t.getHostUuid())).findFirst().get();
            Map<String, Object> payload = org.zstack.utils.gson.JSONObjectUtil.toObject(restore.getPolicy(), Map.class);
            Map<String, Object> ksm = (Map<String, Object>) payload.get("ksm");
            assertEquals(Boolean.FALSE, ksm.get("enabled"));
            assertEquals(2000L, ((Number) ksm.get("pagesToScan")).longValue());
            assertEquals(20L, ((Number) ksm.get("sleepMillis")).longValue());
            assertFalse("zero-page authority is not inferred from a scan-only request", ksm.containsKey("zeroPagesEnabled"));
            assertFalse(payload.containsKey("zram"));
            assertFalse(payload.containsKey("writeback"));
            assertTrue(repository.isConfirmedKsmReconnectTask(restore.getUuid()));
            assertFalse(repository.applyEffectivePolicyAfterConnect("host1"));
            assertEquals(before + 2, taskCount());
            assertEquals(delta, repository.transaction(em -> em.find(MemoryTaskVO.class, "confirmed-ksm-r8").getPolicy()));
        }
    }

    @Test public void rebootReissuesUnchangedAppliedZramOnceForSameBootConfirmation() {
        registerStandardResources();
        String policy = "{\"schemaVersion\":1,\"zram\":{\"enabled\":true}}";
        repository.transaction(em -> {
            writeOverride(em, "host1", "HostVO", "zram.enabled", true);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Succeeded"); state.setDesiredRevision(2); state.setAppliedRevision(2L);
            state.setActiveTaskUuid(null); state.setPolicyPlanHash(null);
            state.setAppliedPolicy(policy); state.setAppliedPolicyHash(sha256(policy));
            state.setControlOperationUuid("zram-applied-r2");
            state.setCapabilities("{\"supported\":true,\"ksm\":true,\"zram\":true,\"writeback\":true}");
            state.setLastSampleTime(System.currentTimeMillis());
            // A new boot: the service rebuilt the pool but the Agent has no same-boot confirmation.
            state.setState("{\"bootId\":\"boot-new\",\"managed\":true,\"actual\":{\"zram\":{\"efficiencyEnabled\":true}}}");
            MemoryTaskVO control = testTask("zram-applied-r2", null, "apply", "Succeeded", null);
            control.setDesiredRevision(2); control.setPolicy(policy); em.persist(control);
            return null;
        });
        long tasksBefore = taskCount();

        assertTrue(repository.applyEffectivePolicyAfterConnect("host1"));
        MemoryTaskVO reconfirm = claimAll().stream()
                .filter(t -> "host1".equals(t.getHostUuid())).findFirst().get();
        assertEquals("apply", reconfirm.getAction());
        assertEquals("system:memory-host-connect-policy", reconfirm.getActorUuid());
        Map<String, Object> payload = org.zstack.utils.gson.JSONObjectUtil.toObject(reconfirm.getPolicy(), Map.class);
        assertEquals(Boolean.TRUE, ((Map<String, Object>) payload.get("zram")).get("enabled"));
        assertFalse(payload.containsKey("ksm"));
        assertFalse("the same boot never re-issues again", repository.applyEffectivePolicyAfterConnect("host1"));
        assertEquals(tasksBefore + 2, taskCount());

        // A same-boot confirmation means there is nothing to re-issue.
        repository.transaction(em -> {
            em.createQuery("delete from MemoryTaskVO where actorUuid = 'system:memory-host-connect-policy'").executeUpdate();
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Succeeded"); state.setDesiredRevision(2); state.setActiveTaskUuid(null);
            state.setControlOperationUuid("zram-applied-r2"); state.setPolicyPlanHash(null);
            state.setState("{\"bootId\":\"boot-new\",\"managed\":true,\"lastConfirmedOperationUuid\":\"zram-applied-r2\","
                    + "\"lastConfirmedAppliedRevision\":2,\"actual\":{\"zram\":{\"efficiencyEnabled\":true}}}");
            return null;
        });
        assertFalse(repository.applyEffectivePolicyAfterConnect("host1"));
    }

    @Test public void rebootReissuesAppliedKsmTheKernelReset() {
        registerStandardResources();
        String policy = "{\"schemaVersion\":1,\"ksm\":{\"enabled\":true,\"zeroPagesEnabled\":true,"
                + "\"pagesToScan\":100,\"sleepMillis\":20}}";
        seedConfirmedKsmReconnect(policy);
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            // The last control operation is an older apply, so the confirmed-reconnect path does not apply.
            state.setControlOperationUuid("older-apply-r7");
            MemoryTaskVO older = testTask("older-apply-r7", null, "apply", "Succeeded", null);
            older.setDesiredRevision(7); older.setPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":true}}");
            em.persist(older);
            state.setState("{\"bootId\":\"boot-new\",\"actual\":{\"ksm\":{\"enabled\":true,"
                    + "\"zeroPagesEnabled\":false,\"pagesToScan\":100,\"sleepMillis\":20}}}");
            return null;
        });

        assertTrue(repository.applyEffectivePolicyAfterConnect("host1"));
        MemoryTaskVO reissue = claimAll().stream()
                .filter(t -> "host1".equals(t.getHostUuid())).findFirst().get();
        Map<String, Object> payload = org.zstack.utils.gson.JSONObjectUtil.toObject(reissue.getPolicy(), Map.class);
        Map<String, Object> ksm = (Map<String, Object>) payload.get("ksm");
        assertEquals(Boolean.TRUE, ksm.get("zeroPagesEnabled"));
        assertEquals(100, ((Number) ksm.get("pagesToScan")).intValue());
        assertFalse("the same boot never re-issues again", repository.applyEffectivePolicyAfterConnect("host1"));
    }

    @Test public void zramOnlyUpdateCarriesAppliedKsmForLaterPause() {
        registerStandardResources();
        String policy = "{\"schemaVersion\":1,\"ksm\":{\"enabled\":true,\"zeroPagesEnabled\":true,"
                + "\"pagesToScan\":100,\"sleepMillis\":20}}";
        seedConfirmedKsmReconnect(policy);
        repository.transaction(em -> {
            writeOverride(em, "host1", "HostVO", "zram.enabled", true);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setCapabilities("{\"supported\":true,\"ksm\":true,\"ksmZeroPages\":true,\"zram\":true}");
            state.setState("{\"bootId\":\"boot-b\",\"lastConfirmedOperationUuid\":\"confirmed-ksm-r8\","
                    + "\"lastConfirmedAppliedRevision\":8,\"actual\":{\"ksm\":{\"enabled\":true,"
                    + "\"zeroPagesEnabled\":true,\"pagesToScan\":100,\"sleepMillis\":20}}}");
            return null;
        });

        assertTrue(repository.applyEffectivePolicyAfterConnect("host1"));
        MemoryTaskVO apply = claimAll().stream()
                .filter(t -> "host1".equals(t.getHostUuid())).findFirst().get();
        Map<String, Object> payload = org.zstack.utils.gson.JSONObjectUtil.toObject(apply.getPolicy(), Map.class);
        assertEquals(Boolean.TRUE, ((Map<String, Object>) payload.get("zram")).get("enabled"));
        Map<String, Object> ksm = (Map<String, Object>) payload.get("ksm");
        assertEquals(Boolean.TRUE, ksm.get("enabled"));
        assertEquals(Boolean.TRUE, ksm.get("zeroPagesEnabled"));
    }

    private void seedConfirmedKsmReconnect(String policy) {
        repository.transaction(em -> {
            Map<String, Object> ksm = (Map<String, Object>) org.zstack.utils.gson.JSONObjectUtil.toObject(policy, Map.class).get("ksm");
            writeOverride(em, "host1", "HostVO", "ksm.enabled", ksm.get("enabled"));
            writeOverride(em, "host1", "HostVO", "ksm.zeroPagesEnabled", ksm.get("zeroPagesEnabled"));
            // Scan rates are no longer configurable; an upgraded Host policy row may still carry them.
            MemoryPolicyVO own = em.find(MemoryPolicyVO.class, "Host:host1");
            if (own == null) { persistPolicy(em, "Host", "host1"); own = em.find(MemoryPolicyVO.class, "Host:host1"); }
            own.setPolicy("{\"schemaVersion\":1,\"ksm\":{\"pagesToScan\":" + ksm.get("pagesToScan")
                    + ",\"sleepMillis\":" + ksm.get("sleepMillis") + "}}");
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Succeeded"); state.setDesiredRevision(8); state.setAppliedRevision(8L);
            state.setActiveTaskUuid(null);
            state.setPolicyPlanHash(null);
            state.setAppliedPolicy(policy); state.setAppliedPolicyHash(sha256(policy));
            state.setControlOperationUuid("confirmed-ksm-r8");
            state.setCapabilities("{\"supported\":true,\"ksm\":true,\"ksmZeroPages\":true}");
            state.setLastSampleTime(System.currentTimeMillis());
            state.setState("{\"bootId\":\"boot-b\",\"actual\":{\"ksm\":{\"enabled\":false,"
                    + "\"zeroPagesEnabled\":false,\"pagesToScan\":100,\"sleepMillis\":20}}}");
            MemoryTaskVO control = testTask("confirmed-ksm-r8", null, "apply", "Succeeded", null);
            control.setDesiredRevision(8); control.setPolicy(policy); em.persist(control);
            return null;
        });
    }

    private static String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) { result.append(String.format("%02x", b & 255)); }
            return result.toString();
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    @Test public void standardKsmClearUnknownReceiptKeepsPriorAppliedSnapshotAndFence() {
        prepareAppliedKsmForStandardClear(); deleteStandardKsmEnabled();
        MemoryTaskVO clear = claimAll().stream()
                .filter(t -> "host1".equals(t.getHostUuid())).findFirst().get();
        repository.result(clear.getUuid(), "Unknown", "native proof unavailable", null);
        MemoryStateVO unknown = host1State();
        assertEquals(Long.valueOf(1), unknown.getAppliedRevision());
        assertEquals(Boolean.FALSE, MemoryPolicyRules.decode(unknown.getAppliedPolicy()).ksm.enabled);
        assertEquals(clear.getUuid(), unknown.getActiveTaskUuid());
        assertEquals(clear.getUuid(), unknown.getControlOperationUuid());
        assertFalse(repository.applyEffectivePolicyAfterConnect("host1"));
    }

    @Test public void standardFinalKsmDeleteWhileDisconnectedReconcilesOnceAfterConnect() {
        prepareAppliedKsmForStandardClear();
        repository.transaction(em -> { em.find(TestHost.class, "host1").status = HostStatus.Disconnected; return null; });
        deleteStandardKsmEnabled();
        assertEquals("Blocked", host1State().getPolicyPlanStatus());
        assertTrue(host1State().getPolicyBlockedFields().contains("HOST_NOT_CONNECTED"));
        assertTrue(claimAll().isEmpty());
        repository.transaction(em -> { em.find(TestHost.class, "host1").status = HostStatus.Connected; return null; });
        assertTrue("clearing the final field must not disable reconnect reconciliation",
                repository.applyEffectivePolicyAfterConnect("host1"));
        assertFalse(repository.applyEffectivePolicyAfterConnect("host1"));
        MemoryTaskVO clear = claimAll().stream()
                .filter(t -> "host1".equals(t.getHostUuid())).findFirst().get();
        assertEquals("clearOverride", clear.getAction());
        assertEquals("{\"schemaVersion\":1,\"ksm\":{}}", clear.getPolicy());
    }

    @Test public void standardKsmDeleteDoesNotReleaseOldUnknown() {
        prepareAppliedKsmForStandardClear();
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Unknown"); state.setActiveTaskUuid("previous-ksm-apply");
            em.find(MemoryTaskVO.class, "previous-ksm-apply").setStatus("Unknown"); return null;
        });
        deleteStandardKsmEnabled();
        MemoryTaskVO query = claimAll().stream().findFirst().get();
        assertTrue(MemoryRepository.isConfigurationReconcile(query));
        assertEquals("previous-ksm-apply", query.getReconcileOperationUuid());
        assertEquals(query.getUuid(), host1State().getActiveTaskUuid());
        assertEquals("previous-ksm-apply", host1State().getControlOperationUuid());
        assertEquals(Long.valueOf(1), host1State().getAppliedRevision());
        assertEquals("Unknown", host1State().getStatus());
        assertEquals("Unknown", repository.findTask("previous-ksm-apply").getStatus());
        assertFalse(repository.applyEffectivePolicyAfterConnect("host1"));
        repository.result(query.getUuid(), "Unknown", "original receipt unavailable", null);
        assertEquals("previous-ksm-apply", host1State().getControlOperationUuid());
        assertEquals("Unknown", repository.findTask("previous-ksm-apply").getStatus());
        assertTrue(claimAll().isEmpty());
    }

    @Test public void standardSaveQueriesUnknownOnceAndAppliesOnlyLatestDesiredAfterCurrentProof() {
        prepareAppliedKsmForStandardClear();
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Unknown"); state.setActiveTaskUuid("previous-ksm-apply");
            state.setState("{\"bootId\":\"boot-1\"}");
            em.find(MemoryTaskVO.class, "previous-ksm-apply").setStatus("Unknown");
            return null;
        });
        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.KSM_ZERO_PAGES,
                "host1", "HostVO", "true")), null, false);
        MemoryTaskVO query = claimAll().stream().findFirst().get();
        assertTrue(MemoryRepository.isConfigurationReconcile(query));
        assertEquals("previous-ksm-apply", query.getExpectedControlOperationUuid());
        assertEquals(1L, query.getDesiredRevision());
        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.KSM_ZERO_PAGES,
                "host1", "HostVO", "false")), null, false);
        assertEquals(query.getUuid(), host1State().getActiveTaskUuid());
        assertEquals(1L, host1State().getDesiredRevision());
        assertTrue(claimAll().isEmpty());
        MemoryAgentResponse receipt = new MemoryAgentResponse(); receipt.setSuccess(true);
        receipt.status = "Succeeded"; receipt.operationUuid = "previous-ksm-apply";
        receipt.appliedRevision = 1L; receipt.hostUuid = "host1"; receipt.bootId = "boot-1";
        MemoryAgentResponse current = new MemoryAgentResponse(); current.setSuccess(true);
        current.status = "Succeeded"; current.hostUuid = "host1"; current.bootId = "boot-1";
        current.sampleTime = System.currentTimeMillis(); current.appliedRevision = 1L;
        current.state = new LinkedHashMap<>(); current.state.put("bootId", "boot-1");
        current.state.put("lastConfirmedOperationUuid", "previous-ksm-apply");
        current.state.put("lastConfirmedAppliedRevision", 1L);
        current.capabilities = new LinkedHashMap<>(); current.capabilities.put("supported", true);
        current.capabilities.put("ksm", true); current.capabilities.put("ksmZeroPages", true);
        repository.result(query.getUuid(), "Succeeded", "immutable original receipt", receipt, current);
        assertEquals("Succeeded", repository.findTask("previous-ksm-apply").getStatus());
        assertNull(host1State().getActiveTaskUuid());
        assertEquals("previous-ksm-apply", host1State().getControlOperationUuid());
        assertEquals(Boolean.FALSE, MemoryPolicyRules.decode(effectivePolicy("Host", "host1")).ksm.zeroPagesEnabled);
        assertTrue(repository.applyEffectivePolicyAfterConnect("host1"));
        assertFalse(repository.applyEffectivePolicyAfterConnect("host1"));
        MemoryTaskVO next = claimAll().stream().findFirst().get();
        assertEquals(Boolean.FALSE, MemoryPolicyRules.decode(next.getPolicy()).ksm.zeroPagesEnabled);
        assertEquals(2L, next.getDesiredRevision());
        repository.result(query.getUuid(), "Succeeded", "late duplicate", receipt, current);
        assertEquals(next.getUuid(), host1State().getActiveTaskUuid());
        assertEquals(next.getUuid(), host1State().getControlOperationUuid());
        MemoryAgentResponse applied = new MemoryAgentResponse(); applied.appliedRevision = next.getDesiredRevision();
        repository.result(next.getUuid(), "Succeeded", "latest desired applied", applied);
        assertEquals("Succeeded", host1State().getStatus());
        assertEquals("Applied", host1State().getPolicyPlanStatus());
        assertEquals(Boolean.FALSE, MemoryPolicyRules.decode(host1State().getAppliedPolicy()).ksm.zeroPagesEnabled);
        assertNull(host1State().getActiveTaskUuid());
        assertFalse(repository.applyEffectivePolicyAfterConnect("host1"));
    }

    @Test public void savingUnchangedDesiredCanQueryUnknownButCannotReplayConfiguration() {
        prepareAppliedKsmForStandardClear();
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Unknown"); state.setActiveTaskUuid("previous-ksm-apply");
            em.find(MemoryTaskVO.class, "previous-ksm-apply").setStatus("Unknown"); return null;
        });
        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.KSM_ENABLED,
                "host1", "HostVO", "false")), null, false);
        List<MemoryTaskVO> tasks = claimAll();
        assertEquals(1, tasks.size());
        assertTrue(MemoryRepository.isConfigurationReconcile(tasks.get(0)));
        assertEquals("previous-ksm-apply", host1State().getControlOperationUuid());
        assertEquals(1L, host1State().getDesiredRevision());
        assertEquals("Unknown", host1State().getStatus());
    }

    @Test public void upgradedAgentObservationRechecksUnsupportedCompletionOnlyOnce() {
        prepareAppliedKsmForStandardClear();
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Unknown"); state.setActiveTaskUuid("previous-ksm-apply");
            em.find(MemoryTaskVO.class, "previous-ksm-apply").setStatus("Unknown"); return null;
        });
        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.KSM_ZERO_PAGES,
                "host1", "HostVO", "true")), null, false);
        MemoryTaskVO query = claimAll().get(0);
        repository.result(query.getUuid(), "Unknown", "READ_ONLY_COMPLETION_UNSUPPORTED", null);
        assertFalse(repository.applyEffectivePolicyAfterConnect("host1"));

        MemoryAgentResponse observation = new MemoryAgentResponse(); observation.setSuccess(true);
        observation.status = "Succeeded"; observation.hostUuid = "host1"; observation.bootId = "boot-1";
        observation.state = new LinkedHashMap<>(); observation.state.put("hostUuid", "host1");
        observation.state.put("bootId", "boot-1"); observation.capabilities = new LinkedHashMap<>();
        observation.capabilities.put("supported", true);
        for (Object marker : new Object[]{false, "true", null}) {
            observation.sampleTime = System.currentTimeMillis();
            observation.capabilities.put("readOnlyCompletedReconcile", marker);
            repository.observe("host1", observation);
            assertFalse(repository.applyEffectivePolicyAfterConnect("host1"));
        }
        observation.capabilities.put("readOnlyCompletedReconcile", true);
        observation.sampleTime = System.currentTimeMillis(); repository.observe("host1", observation);
        repository.transaction(em -> {
            em.find(MemoryTaskVO.class, query.getUuid()).setReason("Original receipt unavailable"); return null;
        });
        assertFalse("other Unknown reasons must not become periodic retries", repository.applyEffectivePolicyAfterConnect("host1"));
        repository.transaction(em -> {
            em.find(MemoryTaskVO.class, query.getUuid()).setReason("READ_ONLY_COMPLETION_UNSUPPORTED");
            em.find(MemoryStateVO.class, "host1").setLastSampleTime(1L); return null;
        });
        assertFalse(repository.applyEffectivePolicyAfterConnect("host1"));
        observation.sampleTime = System.currentTimeMillis(); repository.observe("host1", observation);
        repository.transaction(em -> {
            em.find(MemoryTaskVO.class, "previous-ksm-apply").setDesiredRevision(2L); return null;
        });
        assertFalse("upgrading cannot waive the original revision fence", repository.applyEffectivePolicyAfterConnect("host1"));
        repository.transaction(em -> {
            em.find(MemoryTaskVO.class, "previous-ksm-apply").setDesiredRevision(1L); return null;
        });
        assertTrue(repository.applyEffectivePolicyAfterConnect("host1"));
        assertFalse("another MN/observation must not duplicate the active query", repository.applyEffectivePolicyAfterConnect("host1"));
        MemoryTaskVO retry = claimAll().get(0);
        assertTrue(MemoryRepository.isConfigurationReconcile(retry));
        assertEquals(query.getParentUuid(), retry.getParentUuid());
        assertEquals(query.getReconcileOperationUuid(), retry.getReconcileOperationUuid());
        assertEquals("previous-ksm-apply", host1State().getControlOperationUuid());
        assertEquals(1L, host1State().getDesiredRevision());
        assertEquals("Unknown", repository.findTask(query.getUuid()).getStatus());
        repository.result(retry.getUuid(), "Unknown", "READ_ONLY_COMPLETION_UNSUPPORTED", null);
        for (int i = 0; i < 3; i++) {
            observation.sampleTime = System.currentTimeMillis(); repository.observe("host1", observation);
            assertFalse("only one upgrade recheck per saved request", repository.applyEffectivePolicyAfterConnect("host1"));
        }
        assertTrue(claimAll().isEmpty());
    }

    @Test public void standardKsmDeleteDoesNotResumePausedController() {
        prepareAppliedKsmForStandardClear();
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setControlOperationUuid("pause-ksm");
            em.persist(testTask("pause-ksm", null, "pause", "Succeeded", null)); return null;
        });
        deleteStandardKsmEnabled();
        assertTrue(host1State().getPolicyBlockedFields().contains("HOST_POLICY_PAUSED"));
        assertEquals("pause-ksm", host1State().getControlOperationUuid());
        assertEquals(Long.valueOf(1), host1State().getAppliedRevision());
        assertNull(host1State().getActiveTaskUuid());
        assertTrue(claimAll().isEmpty());
        assertFalse(repository.applyEffectivePolicyAfterConnect("host1"));
    }

    @Test public void standardKsmDeleteRequiresFreshCapabilityBeforeDispatch() {
        prepareAppliedKsmForStandardClear();
        repository.transaction(em -> {
            em.find(MemoryStateVO.class, "host1").setLastSampleTime(1L); return null;
        });
        deleteStandardKsmEnabled();
        assertTrue(host1State().getPolicyBlockedFields().contains("CAPABILITY_STALE"));
        assertEquals("previous-ksm-apply", host1State().getControlOperationUuid());
        assertNull(host1State().getActiveTaskUuid());
        assertTrue(claimAll().isEmpty());
    }

    private void persistLegacyKsmValue(EntityManager em, String resource, String type, String value) {
        MemoryStandardField field = MemoryStandardField.forPath("ksm.enabled");
        ResourceConfigVO row = new ResourceConfigVO(); row.setUuid(UUID.randomUUID().toString().replace("-", ""));
        row.setCategory(field.category()); row.setName(field.configName()); row.setValue(value);
        row.setResourceUuid(resource); row.setResourceType(type); em.persist(row);
    }

    @Test public void lowerLevelNoneStopsManagingInsteadOfInheriting() {
        registerStandardResources();
        for (MemoryStandardField field : Arrays.asList(MemoryStandardField.KSM_ENABLED, MemoryStandardField.ZRAM_ENABLED)) {
            repository.transaction(em -> {
                em.createQuery("delete from ResourceConfigVO").executeUpdate();
                writeOverride(em, "cluster1", "ClusterVO", field.path(), true);
                return null;
            });
            assertEquals(field.path(), Boolean.TRUE,
                    MemoryStandardConfigCodec.get(effectivePolicy("Host", "host1"), field.path()));

            for (String[] layer : new String[][]{{"cluster1", "ClusterVO"}, {"host1", "HostVO"}}) {
                repository.transaction(em -> {
                    em.createQuery("delete from ResourceConfigVO").executeUpdate();
                    if ("host1".equals(layer[0])) { writeOverride(em, "cluster1", "ClusterVO", field.path(), true); }
                    else { writeGlobal(em, field.path(), true); }
                    ResourceConfigVO row = new ResourceConfigVO(); row.setUuid(UUID.randomUUID().toString().replace("-", ""));
                    row.setCategory(field.category()); row.setName(field.configName()); row.setValue("none");
                    row.setResourceUuid(layer[0]); row.setResourceType(layer[1]); em.persist(row);
                    return null;
                });
                assertNull(field.path() + " none on " + layer[1] + " must not inherit the upper value",
                        MemoryStandardConfigCodec.get(effectivePolicy("Host", "host1"), field.path()));
            }
            repository.transaction(em -> { writeGlobal(em, field.path(), null); return null; });
        }
    }

    @Test public void policyRuleErrorTranslationPreservesMemoryCodesAndPassesThroughOtherFailures() {
        RuntimeException memory = MemoryRepository.translatePolicyRuleFailure(
                new IllegalArgumentException("MEMORY_INVALID_POLICY: capacity limits conflict"));
        assertTrue(memory instanceof MemoryOperationException);
        assertEquals("MEMORY_INVALID_POLICY", ((MemoryOperationException) memory).getCode());
        assertEquals("capacity limits conflict", memory.getMessage());

        IllegalArgumentException unrelated = new IllegalArgumentException("non-memory validation detail");
        assertSame("non-MEMORY failures must retain their original type and detail", unrelated,
                MemoryRepository.translatePolicyRuleFailure(unrelated));
    }

    @Test public void firstObservationPersistsBeforeAnyPolicyHasBeenApplied() {
        MemoryAgentResponse response = new MemoryAgentResponse();
        response.status = "Succeeded";
        response.sampleTime = System.currentTimeMillis();
        response.state = Collections.singletonMap("managed", false);
        repository.observe("host1", response);
        MemoryStateVO state = repository.states(Collections.singletonList("host1"), 0, 10).get(0);
        assertEquals("Succeeded", state.getStatus());
        assertEquals(response.sampleTime, state.getLastSampleTime());
        response.status = null;
        repository.observe("host2", response);
        assertEquals("Unknown", repository.states(Collections.singletonList("host2"), 0, 10).get(0).getStatus());
    }

    @Test public void schemaOnlyTargetWithoutAnyKsmOwnerDoesNotInventCapabilityBlocker() {
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Succeeded");
            state.setAppliedPolicy("{\"schemaVersion\":1,\"zram\":{\"enabled\":false},\"writeback\":{\"enabled\":false}}");
            state.setCapabilities("{\"supported\":true,\"ksm\":true,\"zram\":false,\"writeback\":false}");
            return null;
        });

        assertFalse("an empty sparse target is not a request to adopt unmanaged native KSM",
                repository.applyEffectivePolicyAfterConnect("host1"));
        MemoryStateVO unchanged = repository.states(Collections.singletonList("host1"), 0, 1).get(0);
        assertNotEquals("Blocked", unchanged.getPolicyPlanStatus());
        assertNull(unchanged.getActiveTaskUuid());
        assertEquals(0L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t where t.hostUuid = 'host1' and t.action = 'apply'",
                Long.class).getSingleResult()));
    }

    @Test public void firstUseRejectedBeforeDispatchIsNotTreatedAsLegacyHistory() {
        repository.transaction(em -> {
            MemoryPolicyVO global = em.find(MemoryPolicyVO.class, "Global:global");
            global.setPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":true}}");
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Failed"); state.setAppliedPolicyHash(null); state.setAppliedPolicy(null);
            state.setDesiredRevision(3); state.setAppliedRevision(null); state.setControlOperationUuid("never-issued");
            state.setState("{\"phase\":\"RUNNING\",\"lifecycle\":{\"paused\":false,\"activeState\":\"running\"}}");
            em.persist(testTask("never-issued", null, "apply", "Failed", null));
            return null;
        });
        MemoryAgentResponse observation = new MemoryAgentResponse();
        observation.status = "Succeeded"; observation.sampleTime = System.currentTimeMillis();
        observation.capabilities = new HashMap<>(); observation.capabilities.put("supported", true);
        observation.capabilities.put("ksm", true); observation.capabilities.put("ksmZeroPages", true);
        observation.capabilities.put("zram", true); observation.capabilities.put("writeback", true);
        observation.state = Collections.<String, Object>singletonMap("phase", "RUNNING");
        repository.observe("host1", observation);
        repository.applyEffectivePolicyAfterConnect("host1");
        MemoryStateVO state = repository.states(Collections.singletonList("host1"), 0, 1).get(0);
        assertNotEquals("nothing was ever applied, so a retry is a first use", "NeedsReview", state.getPolicyPlanStatus());
    }

    @Test public void historicalDrainWithoutAppliedPolicyHashIsNotAdoptedByReconnect() {
        repository.transaction(em -> {
            MemoryPolicyVO global = em.find(MemoryPolicyVO.class, "Global:global");
            global.setPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":true}}");
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Succeeded"); state.setAppliedPolicyHash(null); state.setAppliedPolicy(null);
            state.setDesiredRevision(3); state.setAppliedRevision(2L); state.setControlOperationUuid("old-drain");
            state.setState("{\"phase\":\"DRAINED\",\"lifecycle\":{\"paused\":false,\"activeState\":\"running\"}}");
            em.persist(testTask("old-drain", null, "drain", "Succeeded", null));
            return null;
        });
        for (int i = 0; i < 3; i++) {
            MemoryAgentResponse observation = new MemoryAgentResponse();
            observation.status = "Succeeded"; observation.sampleTime = System.currentTimeMillis() + i;
            observation.capabilities = new HashMap<>(); observation.capabilities.put("supported", true);
            observation.capabilities.put("ksm", true); observation.capabilities.put("ksmZeroPages", true);
            observation.capabilities.put("zram", true); observation.capabilities.put("writeback", true);
            observation.state = Collections.<String, Object>singletonMap("phase", "DRAINED");
            repository.observe("host1", observation);
            assertFalse(repository.applyEffectivePolicyAfterConnect("host1"));
        }
        MemoryStateVO state = repository.states(Collections.singletonList("host1"), 0, 1).get(0);
        assertEquals("old-drain", state.getControlOperationUuid());
        assertEquals("NeedsReview", state.getPolicyPlanStatus());
        assertTrue(state.getPolicyBlockedFields().contains("LEGACY_APPLIED_POLICY_UNKNOWN"));
        assertEquals(0L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t where t.hostUuid = 'host1' and t.action = 'apply'",
                Long.class).getSingleResult()));
    }

    private void seedLegacyPolicyNeedsReview(String hostUuid) {
        repository.transaction(em -> {
            MemoryPolicyVO policy = new MemoryPolicyVO();
            policy.setUuid("Host:" + hostUuid); policy.setScope("Host"); policy.setResourceUuid(hostUuid);
            policy.setPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":true}}");
            em.persist(policy);
            MemoryStateVO state = em.find(MemoryStateVO.class, hostUuid);
            state.setStatus("Succeeded"); state.setAppliedPolicyHash(null); state.setAppliedPolicy(null);
            // Historical revisions are sufficient to reproduce the unknown
            // legacy applied policy without leaving a live drain fence that
            // would correctly reject a new explicit policy submission.
            state.setDesiredRevision(3); state.setAppliedRevision(2L); state.setControlOperationUuid(null);
            state.setState("{\"phase\":\"RUNNING\",\"lifecycle\":{\"paused\":false,\"activeState\":\"running\"}}");
            return null;
        });
        assertFalse(repository.applyEffectivePolicyAfterConnect(hostUuid));
        MemoryStateVO state = repository.states(Collections.singletonList(hostUuid), 0, 1).get(0);
        assertEquals("NeedsReview", state.getPolicyPlanStatus());
        assertTrue(state.getPolicyBlockedFields().contains("LEGACY_APPLIED_POLICY_UNKNOWN"));
    }

    private MemoryActionRequest hostApply(String hostUuid) {
        MemoryActionRequest apply = request();
        apply.setScope("Host"); apply.setResourceUuid(hostUuid);
        apply.setPolicy("{\"ksm\":{\"enabled\":true}}");
        return apply;
    }

    @Test public void unknownApplyPlanIsNotAutomaticallyResubmitted() {
        repository.transaction(em -> {
            MemoryPolicyVO explicit = new MemoryPolicyVO(); explicit.setUuid("Host:host1");
            explicit.setScope("Host"); explicit.setResourceUuid("host1");
            explicit.setPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":false}}"); em.persist(explicit);
            return null;
        });
        assertTrue(repository.applyEffectivePolicyAfterConnect("host1"));
        MemoryTaskVO task = claimAll().stream().filter(t -> "host1".equals(t.getHostUuid())).findFirst().get();
        repository.result(task.getUuid(), "Unknown", "timeout", null);
        assertFalse(repository.applyEffectivePolicyAfterConnect("host1"));
        assertEquals(1L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t where t.hostUuid = 'host1'", Long.class).getSingleResult()));
    }

    @Test public void inertUpgradeDefaultsDoNotTakeOverLegacyKsmAndUnknownHostIsNotRetried() {
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setAppliedPolicyHash(null); // Legacy row after the additive migration.
            state.setState("{\"actual\":{\"ksm\":{\"run\":1,\"managed\":false}}}");
            return null;
        });
        assertFalse(repository.applyEffectivePolicyAfterConnect("host1"));
        assertEquals(0L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t", Long.class).getSingleResult()));

        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host2");
            state.setStatus("Unknown");
            state.setActiveTaskUuid(null);
            state.setAppliedPolicyHash(null);
            return null;
        });
        assertFalse(repository.applyEffectivePolicyAfterConnect("host2"));
        assertEquals(0L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t", Long.class).getSingleResult()));
    }

    @Test public void explicitFalseKsmPolicyTakesOverAndDisablesBootEnabledKsm() {
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setAppliedPolicyHash(null);
            state.setState("{\"actual\":{\"ksm\":{\"run\":1,\"managed\":false}}}");
            MemoryPolicyVO explicit = new MemoryPolicyVO(); explicit.setUuid("Host:host1");
            explicit.setScope("Host"); explicit.setResourceUuid("host1");
            explicit.setPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":false}}");
            em.persist(explicit);
            return null;
        });
        assertTrue(repository.applyEffectivePolicyAfterConnect("host1"));
        MemoryTaskVO child = claimAll().stream()
                .filter(task -> "host1".equals(task.getHostUuid())).findFirst().get();
        assertFalse(Boolean.TRUE.equals(MemoryPolicyRules.decode(child.getPolicy()).ksm.enabled));
        assertEquals("Applying", repository.findTask(child.getUuid()).getStatus());
    }


    @Test public void backendPreparationKeepsPolicyAndDrainFence() {
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Succeeded"); state.setControlOperationUuid("completed-drain"); return null;
        });
        MemoryActionRequest msg = request();
        msg.setScope("Host"); msg.setResourceUuid("host1");
        msg.setAction(MemoryBackendPreparationRules.ACTION); msg.setPolicy("{}");
        msg.setExpectedControlOperationUuid("completed-drain");
        msg.setBackendPreparation(MemoryBackendPreparationRulesTest.preparation());
        String parent = repository.submit(msg);
        assertEquals("{\"schemaVersion\":1}", ownPolicy("Host", "host1"));
        MemoryStateVO state = repository.states(Collections.singletonList("host1"), 0, 10).get(0);
        assertEquals("completed-drain", state.getControlOperationUuid());
        assertNotNull(state.getActiveTaskUuid());
        MemoryTaskVO child = claimAll().get(0);
        assertEquals(MemoryBackendPreparationRules.ACTION, child.getAction());
        assertTrue(child.getPolicy().contains("candidateId"));
        try { repository.submit(msg); fail("unresolved preparation must serialize with other Host tasks"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_HOST_BUSY", expected.getCode()); }
        repository.result(child.getUuid(), "Succeeded", "prepared; optimization still disabled", null);
    }

    @Test public void selectedBackendCandidatePreparationQueuesDrainBeforePreparingNewDevice() {
        selectedBackendCandidatePreparationFlow(false, false, false, false);
    }

    @Test public void selectedBackendPreparationAfterManualDrainRestoresWriteback() {
        selectedBackendCandidatePreparationFlow(true, false, false, false);
    }

    @Test public void selectedBackendPreparationRejectsMismatchedReceiptWithoutRegisteringOwner() {
        selectedBackendCandidatePreparationFlow(true, true, false, false);
    }

    @Test public void selectedBackendConfigWriteFailureRollsBackPreparationResultAtomically() {
        selectedBackendCandidatePreparationFlow(true, false, true, false);
    }

    @Test public void selectedBackendPreparationAfterManualDrainKeepsZramDisabled() {
        selectedBackendCandidatePreparationFlow(true, false, false, true);
    }

    @Test public void selectedBackendPreparationRejectsTargetChangedBeforePrepareResult() {
        selectedBackendCandidatePreparationFlow(true, false, false, false, true, false, false, false);
    }

    @Test public void selectedBackendPreparationRejectsExpiredFreshObservation() {
        selectedBackendCandidatePreparationFlow(true, false, false, false, false, true, false, false);
    }

    @Test public void selectedBackendPreparationRejectsWrongFreshBackendIdentity() {
        selectedBackendCandidatePreparationFlow(true, false, false, false, false, false, true, false);
    }

    @Test public void duplicateSuccessfulSelectedPreparationResultDoesNotRepeatWrites() {
        selectedBackendCandidatePreparationFlow(true, false, false, false, false, false, false, true);
    }

    private void selectedBackendCandidatePreparationFlow(
            boolean manualDrain, boolean mismatchPrepareReceipt, boolean failConfigWrite, boolean zramDisabled) {
        selectedBackendCandidatePreparationFlow(manualDrain, mismatchPrepareReceipt, failConfigWrite, zramDisabled,
                false, false, false, false);
    }

    private void selectedBackendCandidatePreparationFlow(boolean manualDrain, boolean mismatchPrepareReceipt,
            boolean failConfigWrite, boolean zramDisabled, boolean targetChangesBeforePrepareResult,
            boolean staleFreshObservation, boolean wrongFreshBackendIdentity, boolean duplicatePrepareResult) {
        prepareCapacityObservation("Succeeded", false, false, false);
        MemoryTaskVO manualDrainTask = null;
        if (manualDrain) {
            MemoryTaskVO parent = testTask("manual-selected-drain-parent", null, "control", "Applying", null);
            MemoryTaskVO drain = testTask("manual-selected-drain", parent.getUuid(), "drain", "Applying", null);
            manualDrainTask = drain;
            repository.transaction(em -> {
                em.persist(parent); em.persist(drain);
                MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
                state.setActiveTaskUuid(drain.getUuid()); state.setControlOperationUuid(drain.getUuid());
                writeGlobal(em, "writeback.enabled", !zramDisabled);
                if (zramDisabled) {
                    writeGlobal(em, "ksm.enabled", false);
                    writeGlobal(em, "zram.enabled", false);
                    state.setAppliedPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":false},"
                            + "\"zram\":{\"enabled\":false,\"logicalCapacityBytes\":8192,\"ramLimitBytes\":4096},\"writeback\":{\"enabled\":false}}");
                    state.setAppliedPolicyHash(sha256(state.getAppliedPolicy()));
                    com.google.gson.JsonObject stateJson = new com.google.gson.JsonParser().parse(state.getState()).getAsJsonObject();
                    com.google.gson.JsonObject actualZram = stateJson.getAsJsonObject("actual").getAsJsonObject("zram");
                    actualZram.addProperty("enabled", false); actualZram.addProperty("efficiencyEnabled", false);
                    state.setState(stateJson.toString());
                }
                return null;
            });
            Map<String, Object> proof = new LinkedHashMap<>();
            proof.put("device", "/dev/zram0"); proof.put("pool_generation",
                    "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789");
            proof.put("state", "WITHDRAWN_SWAP_INACTIVE_POOL_PRESERVED");
            proof.put("active", false); proof.put("reset_performed", false);
            Map<String, Object> state = new LinkedHashMap<>();
            state.put("hostUuid", "host1"); state.put("bootId", "01234567-89ab-cdef-0123-456789abcdef");
            state.put("poolGeneration", proof.get("pool_generation")); state.put("controlOperationUuid", drain.getUuid());
            state.put("observedAt", Instant.now().toString()); state.put("status", "AVAILABLE");
            state.put("phase", "WITHDRAWN_SWAP_INACTIVE_POOL_PRESERVED");
            state.put("receipt", proof); state.put("nativeReadback", proof);
            MemoryAgentResponse drained = new MemoryAgentResponse(); drained.operationUuid = drain.getUuid();
            drained.status = "Succeeded"; drained.setSuccess(true); drained.hostUuid = "host1";
            drained.bootId = "01234567-89ab-cdef-0123-456789abcdef";
            drained.appliedRevision = drain.getDesiredRevision(); drained.sampleTime = System.currentTimeMillis();
            drained.state = state;
            repository.result(drain.getUuid(), "Succeeded", "manual drain completed", drained, drained);
        }
        String boot = "01234567-89ab-cdef-0123-456789abcdef";
        String pool = "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";
        String fingerprint = "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc";
        String candidateId = "candidate:" + fingerprint;
        String control = host1State().getControlOperationUuid();
        Map<String, Object> stableIdentity = new LinkedHashMap<>();
        stableIdentity.put("bootId", boot); stableIdentity.put("device", "/dev/vdb");
        stableIdentity.put("byIdPath", "/dev/disk/by-id/local"); stableIdentity.put("majorMinor", "252:16");
        stableIdentity.put("serial", "serial-local"); stableIdentity.put("wwn", "");
        stableIdentity.put("capacityBytes", 8192L); stableIdentity.put("transport", "virtio");
        stableIdentity.put("sysfsPath", "/sys/devices/local"); stableIdentity.put("sysfsInode", "1");
        stableIdentity.put("rdev", 64528L); stableIdentity.put("nodeInode", 2L);
        Map<String, Object> candidate = new LinkedHashMap<>();
        candidate.put("resourceUuid", null); candidate.put("identity", candidateId);
        candidate.put("fingerprint", fingerprint); candidate.put("stableIdentity", stableIdentity);
        candidate.put("eligible", true); candidate.put("writebackReady", false); candidate.put("active", false);
        candidate.put("type", "block"); candidate.put("device", "/dev/vdb");
        candidate.put("state", "NEEDS_OFFLINE_PREPARATION"); candidate.put("hostBootId", boot);
        candidate.put("capacityBytes", 8192L);
        MemoryAgentResponse observation = new MemoryAgentResponse();
        observation.status = "Succeeded"; observation.setSuccess(true); observation.hostUuid = "host1";
        observation.bootId = boot; observation.sampleTime = System.currentTimeMillis();
        observation.state = new LinkedHashMap<>();
        observation.state.put("hostUuid", "host1"); observation.state.put("bootId", boot);
        observation.state.put("poolGeneration", pool); observation.state.put("controlOperationUuid", control);
        observation.state.put("observedAt", Instant.now().toString()); observation.state.put("status", "AVAILABLE");
        observation.state.put("canPrepare", manualDrain);
        observation.state.put("preparationReasons", manualDrain ? Collections.emptyList()
                : Arrays.asList("MEMORY_SERVICE_NOT_STOPPED", "DRAIN_PROOF_UNAVAILABLE"));
        Map<String, Object> hostZram = new LinkedHashMap<>();
        hostZram.put("device", "/dev/zram0"); hostZram.put("pool_generation", pool);
        observation.state.put("host_zram", hostZram);
        observation.state.put("candidates", Collections.singletonList(candidate));

        MemoryActionRequest request = request();
        request.setScope("Host"); request.setResourceUuid("host1");
        request.setAction("prepareWritebackBackend"); request.setPolicy("{}");
        request.setExpectedControlOperationUuid(control);
        MemoryBackendPreparation selected = new MemoryBackendPreparation();
        selected.setCandidateId(candidateId); selected.setExpectedHostBootId(boot);
        selected.setExpectedPoolGeneration(pool); selected.setBackendCapacityBytes(8192L);
        selected.setResetConfirmed(true); request.setBackendPreparation(selected);

        long beforeInvalid = taskCount();
        if (!manualDrain) {
        String originalHostState = host1State().getState();
        observation.state.put("observedAt", Instant.now().minusMillis(
                MemoryOptimizationGlobalConfig.displayTtlMillis() + 1000).toString());
        expectSelectedPreparationRejected(request, observation);
        observation.state.put("observedAt", Instant.now().toString());
        observation.state.put("candidates", Arrays.asList(candidate, candidate));
        expectSelectedPreparationRejected(request, observation);
        observation.state.put("candidates", Collections.singletonList(candidate));
        selected.setBackendCapacityBytes(4096L);
        expectSelectedPreparationRejected(request, observation);
        selected.setBackendCapacityBytes(8192L); selected.setResetConfirmed(false);
        expectSelectedPreparationRejected(request, observation);
        selected.setResetConfirmed(true); candidate.put("active", true);
        observation.state.put("status", "UNKNOWN");
        expectSelectedPreparationRejected(request, observation);
        candidate.put("active", false); observation.state.put("status", "AVAILABLE");
        observation.state.put("preparationReasons", Collections.emptyList());
        if (!manualDrain) { expectSelectedPreparationRejected(request, observation); }
        observation.state.put("preparationReasons", Arrays.asList("MEMORY_SERVICE_NOT_STOPPED", "DRAIN_PROOF_UNAVAILABLE"));
        observation.state.remove("canPrepare");
        expectSelectedPreparationRejected(request, observation);
        observation.state.put("canPrepare", false);
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            com.google.gson.JsonObject root = new com.google.gson.JsonParser().parse(state.getState()).getAsJsonObject();
            root.getAsJsonObject("host_zram").remove("device"); state.setState(root.toString()); return null;
        });
        expectSelectedPreparationRejected(request, observation);
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            com.google.gson.JsonObject root = new com.google.gson.JsonParser().parse(state.getState()).getAsJsonObject();
            root.getAsJsonObject("host_zram").addProperty("device", "/dev/sda"); state.setState(root.toString()); return null;
        });
        expectSelectedPreparationRejected(request, observation);
        repository.transaction(em -> { em.find(MemoryStateVO.class, "host1").setState(originalHostState); return null; });
        stableIdentity.remove("rdev");
        expectSelectedPreparationRejected(request, observation);
        stableIdentity.put("rdev", 64528L);
        assertEquals(beforeInvalid, taskCount());
        }

        String parent = repository.submit(request, observation);
        assertEquals("prepareWritebackBackend", repository.findTask(parent).getAction());
        MemoryTaskVO drain = manualDrain ? manualDrainTask : claimAll().stream()
                .filter(task -> "drain".equals(task.getAction())).findFirst().orElse(null);
        assertNotNull(drain);
        if (manualDrain) {
            assertNotEquals("selected preparation must have its own root, not reuse the manual drain root",
                    drain.getParentUuid(), parent);
        } else {
        assertEquals(1L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t where t.parentUuid=:p and t.action='drain'", Long.class)
                .setParameter("p", parent).getSingleResult()));
        Map<String, Object> drained = new LinkedHashMap<>();
        drained.put("hostUuid", "host1"); drained.put("bootId", boot);
        drained.put("observedAt", Instant.now().toString()); drained.put("status", "AVAILABLE");
        drained.put("poolGeneration", pool); drained.put("controlOperationUuid", drain.getUuid());
        drained.put("canPrepare", true); drained.put("canPrepareZramPool", false);
        drained.put("preparationReasons", Collections.emptyList()); drained.put("candidates", Collections.singletonList(candidate));
        drained.put("phase", "WITHDRAWN_SWAP_INACTIVE_POOL_PRESERVED");
        Map<String, Object> nativeReadback = new LinkedHashMap<>();
        nativeReadback.put("device", "/dev/zram0"); nativeReadback.put("pool_generation", pool);
        nativeReadback.put("state", "WITHDRAWN_SWAP_INACTIVE_POOL_PRESERVED");
        nativeReadback.put("active", false); nativeReadback.put("reset_performed", false);
        drained.put("receipt", nativeReadback); drained.put("nativeReadback", nativeReadback);
        MemoryAgentResponse drainResponse = new MemoryAgentResponse();
        drainResponse.operationUuid = drain.getUuid(); drainResponse.status = "Succeeded";
        drainResponse.setSuccess(true); drainResponse.hostUuid = "host1";
        drainResponse.bootId = boot; drainResponse.appliedRevision = drain.getDesiredRevision();
        drainResponse.sampleTime = System.currentTimeMillis(); drainResponse.state = drained;
        repository.result(drain.getUuid(), "Succeeded", "drain completed", drainResponse, drainResponse);
        }
        MemoryTaskVO preparation = claimAll().stream()
                .filter(task -> "prepareWritebackBackend".equals(task.getAction())).findFirst().orElse(null);
        assertNotNull("successful drain must queue selected backend preparation; host="
                + host1State().getStatus() + "/" + host1State().getReason() + "/"
                + host1State().getActiveTaskUuid() + "/" + host1State().getPolicyBlockedFields(), preparation);

        // A first-use candidate has no owner UUID in the original target.  The
        // trusted Agent receipt must therefore be the source of the new owner
        // identity before the normal activation planner is admitted.
        String newResource = "dddddddddddddddddddddddddddddddd";
        Map<String, Object> targetIdentity = new LinkedHashMap<>();
        targetIdentity.put("boot_id", boot); targetIdentity.put("canonical_device", "/dev/vdb");
        targetIdentity.put("capacity_bytes", 8192L); targetIdentity.put("rdev", 64528L);
        targetIdentity.put("node_inode", 2L); targetIdentity.put("sysfs_inode", mismatchPrepareReceipt ? 9L : 1L);
        targetIdentity.put("stack_sha256", "0dfe6c2101c268391514432dd0d786b952739c2e72f5d1896e9b389f4b17dfe4");
        targetIdentity.put("direct_io", false);
        Map<String, Object> maintenance = new LinkedHashMap<>();
        maintenance.put("stage", "APPLIED"); maintenance.put("request_id", preparation.getUuid());
        maintenance.put("candidate_id", candidateId); maintenance.put("backend_resource_uuid", newResource);
        maintenance.put("host_boot_id", boot); maintenance.put("pool_generation", pool);
        maintenance.put("target_identity", targetIdentity);
        MemoryAgentResponse prepared = new MemoryAgentResponse();
        prepared.operationUuid = preparation.getUuid(); prepared.status = "Succeeded";
        prepared.setSuccess(true); prepared.hostUuid = "host1"; prepared.bootId = boot;
        prepared.appliedRevision = preparation.getDesiredRevision(); prepared.sampleTime = System.currentTimeMillis();
        prepared.state = new LinkedHashMap<>(); prepared.state.put("maintenance", maintenance);
        Map<String, Object> preparedState = new LinkedHashMap<>();
        preparedState.put("bootId", boot); preparedState.put("poolGeneration", pool);
        preparedState.put("lastConfirmedOperationUuid", drain.getUuid());
        preparedState.put("lastConfirmedAppliedRevision", drain.getDesiredRevision());
        preparedState.put("managed", true); preparedState.put("phase", "POOL_READY");
        Map<String, Object> lifecycle = new LinkedHashMap<>();
        lifecycle.put("paused", true); lifecycle.put("activeState", "paused");
        preparedState.put("lifecycle", lifecycle);
        Map<String, Object> maintenanceProof = new LinkedHashMap<>();
        maintenanceProof.put("drainOperationUuid", drain.getUuid()); maintenanceProof.put("oldPoolGeneration", pool);
        maintenanceProof.put("maintenanceReady", true); maintenanceProof.put("maintenanceArchived", true);
        maintenanceProof.put("executorExited", true); maintenanceProof.put("activeOperations", 0L);
        maintenanceProof.put("readyForInitialization", true); maintenanceProof.put("originalDeviceInactive", true);
        maintenanceProof.put("oldPoolOwnershipAbsent", true); maintenanceProof.put("poolOwnedByService", false);
        preparedState.put("maintenanceProof", maintenanceProof);
        Map<String, Object> backendResource = new LinkedHashMap<>();
        backendResource.put("resource_uuid", newResource); backendResource.put("capacity_bytes", 8192L);
        backendResource.put("state", "PREPARED"); backendResource.put("active", false);
        backendResource.put("fingerprint", fingerprint);
        backendResource.put("observed_at", Instant.now().toString());
        Map<String, Object> backend = new LinkedHashMap<>();
        backend.put("backend_resource", backendResource);
        Map<String, Object> actual = new LinkedHashMap<>();
        Map<String, Object> zram = new LinkedHashMap<>();
        zram.put("enabled", false); zram.put("efficiencyEnabled", false);
        Map<String, Object> capacity = new LinkedHashMap<>();
        capacity.put("effective_logical_bytes", 8192L); capacity.put("effective_physical_limit_bytes", 4096L);
        capacity.put("apply_state", "APPLIED");
        zram.put("capacity", capacity);
        actual.put("ksm", Collections.singletonMap("enabled", true));
        actual.put("zram", zram); actual.put("writeback", Collections.singletonMap("backend", backend));
        preparedState.put("actual", actual); prepared.state.putAll(preparedState);
        Map<String, Object> savings = new LinkedHashMap<>();
        savings.put("quality", "Fresh"); savings.put("formulaVersion", "mechanism-estimate-v1");
        savings.put("sampleTime", System.currentTimeMillis());
        preparedState.put("savings", savings);
        MemoryAgentResponse fresh = new MemoryAgentResponse(); fresh.status = "Succeeded";
        fresh.setSuccess(true); fresh.hostUuid = "host1"; fresh.bootId = boot;
        fresh.sampleTime = System.currentTimeMillis(); fresh.state = preparedState;
        fresh.capabilities = new LinkedHashMap<>();
        for (String capability : Arrays.asList("supported", "ksm", "ksmZeroPages", "zram", "writeback")) {
            fresh.capabilities.put(capability, true);
        }
        if (staleFreshObservation) {
            fresh.sampleTime = System.currentTimeMillis() - MemoryOptimizationGlobalConfig.displayTtlMillis() - 1000L;
        }
        if (wrongFreshBackendIdentity) {
            Map<String, Object> freshBackend = (Map<String, Object>) ((Map<String, Object>)
                    ((Map<String, Object>) preparedState.get("actual")).get("writeback")).get("backend");
            Map<String, Object> freshBackendResource = (Map<String, Object>) freshBackend.get("backend_resource");
            freshBackendResource.put("resource_uuid", "ffffffffffffffffffffffffffffffff");
        }
        assertTrue("selected preparation fixture must carry an admissible maintenance archive: "
                        + org.zstack.utils.gson.JSONObjectUtil.toJsonString(preparedState),
                MemoryRepository.isMaintenanceRecoveryState(
                        org.zstack.utils.gson.JSONObjectUtil.toJsonString(preparedState), drain.getUuid()));
        if (failConfigWrite) {
            MemoryStateVO stateBeforeResult = host1State();
            String stateTextBeforeResult = stateBeforeResult.getState();
            String activeTaskBeforeResult = stateBeforeResult.getActiveTaskUuid();
            String parentStatusBeforeResult = repository.findTask(parent).getStatus();
            String preparationStatusBeforeResult = repository.findTask(preparation.getUuid()).getStatus();
            long taskCountBeforeResult = taskCount();
            failAfterResourceConfigWrite = true;
            try {
                repository.result(preparation.getUuid(), "Succeeded", "backend prepared", prepared, fresh);
                fail("post-write configuration failure must abort selected preparation result");
            } catch (IllegalStateException expected) {
                assertEquals("injected post-write config failure", expected.getMessage());
            } finally {
                failAfterResourceConfigWrite = false;
            }
            assertEquals(stateTextBeforeResult, host1State().getState());
            assertEquals(activeTaskBeforeResult, host1State().getActiveTaskUuid());
            assertEquals(parentStatusBeforeResult, repository.findTask(parent).getStatus());
            assertEquals(preparationStatusBeforeResult, repository.findTask(preparation.getUuid()).getStatus());
            assertEquals(taskCountBeforeResult, taskCount());
            assertNull(repository.transaction(em -> standardConfigAdapter.readOverrideOwn(
                    em, "writeback.backendResourceUuid", "host1")));
            assertNull(repository.transaction(em -> standardConfigAdapter.readOverrideOwn(
                    em, "writeback.backendCapacityBytes", "host1")));
            assertTrue(repository.transaction(em -> em.createQuery(
                    "select count(t) from MemoryTaskVO t where t.parentUuid=:p and t.action in ('apply','clearOverride')",
                    Long.class).setParameter("p", parent).getSingleResult() == 0L));
            return;
        }
        if (targetChangesBeforePrepareResult) {
            repository.transaction(em -> {
                writeGlobal(em, "writeback.enabled", false);
                return null;
            });
        }
        String appliedPolicyBeforePreparationResult = host1State().getAppliedPolicy();
        String controlBeforePreparationResult = host1State().getControlOperationUuid();
        long taskCountBeforePrepareResult = taskCount();
        repository.result(preparation.getUuid(), "Succeeded", "backend prepared", prepared, fresh);
        if (targetChangesBeforePrepareResult || staleFreshObservation || wrongFreshBackendIdentity) {
            MemoryTaskVO rejectedPreparation = repository.findTask(preparation.getUuid());
            String expectedReason = targetChangesBeforePrepareResult ? "MAINTENANCE_TARGET_CHANGED"
                    : staleFreshObservation ? "CURRENT_POOL_PREPARATION_NOT_PROVEN"
                    : "CURRENT_SELECTED_BACKEND_NOT_PROVEN";
            assertEquals("untrusted completion must keep preparation fenced; reason=" + rejectedPreparation.getReason(),
                    "Unknown", rejectedPreparation.getStatus());
            assertTrue(rejectedPreparation.getReason(), rejectedPreparation.getReason().contains(expectedReason));
            assertNull(repository.transaction(em -> standardConfigAdapter.readOverrideOwn(
                    em, "writeback.backendResourceUuid", "host1")));
            assertNull(repository.transaction(em -> standardConfigAdapter.readOverrideOwn(
                    em, "writeback.backendCapacityBytes", "host1")));
            assertEquals(taskCountBeforePrepareResult, taskCount());
            assertEquals(new com.google.gson.JsonParser().parse(appliedPolicyBeforePreparationResult),
                    new com.google.gson.JsonParser().parse(host1State().getAppliedPolicy()));
            assertEquals(controlBeforePreparationResult, host1State().getControlOperationUuid());
            assertTrue(repository.transaction(em -> em.createQuery(
                    "select count(t) from MemoryTaskVO t where t.parentUuid=:p and t.action in ('apply','clearOverride')",
                    Long.class).setParameter("p", parent).getSingleResult() == 0L));
            return;
        }
        if (mismatchPrepareReceipt) {
            Object rejectedOwner = repository.transaction(em ->
                    standardConfigAdapter.readOverrideOwn(em, "writeback.backendResourceUuid", "host1"));
            Object rejectedCapacity = repository.transaction(em ->
                    standardConfigAdapter.readOverrideOwn(em, "writeback.backendCapacityBytes", "host1"));
            assertNull("untrusted prepare identity must not register a backend owner", rejectedOwner);
            assertNull("untrusted prepare identity must not register backend capacity", rejectedCapacity);
            assertFalse("untrusted prepare identity must not admit activation", repository.transaction(em -> !em.createQuery(
                    "select t from MemoryTaskVO t where t.parentUuid=:p and t.action in ('apply','clearOverride')",
                    MemoryTaskVO.class).setParameter("p", parent).getResultList().isEmpty()));
            return;
        }
        if (zramDisabled) {
            Object preparedOwner = repository.transaction(em ->
                    standardConfigAdapter.readOverrideOwn(em, "writeback.backendResourceUuid", "host1"));
            Object preparedCapacity = repository.transaction(em ->
                    standardConfigAdapter.readOverrideOwn(em, "writeback.backendCapacityBytes", "host1"));
            List<MemoryTaskVO> rootTasks = repository.transaction(em -> em.createQuery(
                    "from MemoryTaskVO t where t.parentUuid=:p order by t.createDate", MemoryTaskVO.class)
                    .setParameter("p", parent).getResultList());
            StringBuilder taskDetails = new StringBuilder();
            for (MemoryTaskVO task : rootTasks) {
                taskDetails.append(task.getAction()).append('/').append(task.getStatus())
                        .append(':').append(task.getPolicy()).append(';');
            }
            MemoryStateVO postPrepareState = host1State();
            assertEquals("selected prepare result=" + repository.findTask(preparation.getUuid()).getStatus() + "/"
                    + repository.findTask(preparation.getUuid()).getReason() + ", root="
                    + repository.findTask(parent).getStatus() + "/" + repository.findTask(parent).getReason()
                    + ", host=" + host1State().getStatus() + "/" + host1State().getReason() + "/"
                    + ", children=" + taskDetails + ", effective="
                    + effectivePolicy("Host", "host1") + ", applied="
                    + postPrepareState.getAppliedPolicy() + ", plan=" + postPrepareState.getPolicyPlanStatus()
                    + ", blocked=" + postPrepareState.getPolicyBlockedFields(),
                    newResource, preparedOwner == null ? null : preparedOwner.toString());
            assertEquals("8192", preparedCapacity == null ? null : preparedCapacity.toString());
            String expectedAppliedPolicy = MemoryPolicyRules.merge(appliedPolicyBeforePreparationResult,
                    "{\"writeback\":{\"backendResourceUuid\":\"" + newResource
                            + "\",\"backendCapacityBytes\":8192}}", "Host");
            assertEquals("trusted prepare binding may add only the verified owner and whole-disk capacity",
                    new com.google.gson.JsonParser().parse(expectedAppliedPolicy),
                    new com.google.gson.JsonParser().parse(host1State().getAppliedPolicy()));
            assertEquals(controlBeforePreparationResult, host1State().getControlOperationUuid());
            assertEquals("Succeeded", repository.findTask(preparation.getUuid()).getStatus());
            assertEquals("Succeeded", repository.findTask(parent).getStatus());
            MemoryPolicyConfig effective = MemoryPolicyRules.decode(effectivePolicy("Host", "host1"));
            assertEquals(Boolean.FALSE, effective.zram.enabled);
            assertEquals(Boolean.FALSE, effective.writeback.enabled);
            assertNull(host1State().getActiveTaskUuid());
            List<MemoryTaskVO> followups = repository.transaction(em -> em.createQuery(
                    "from MemoryTaskVO t where t.parentUuid=:p and t.action in ('apply','clearOverride')",
                    MemoryTaskVO.class).setParameter("p", parent).getResultList());
            assertTrue("trusted PREPARED proof must bind owner/capacity without a normal apply child", followups.isEmpty());
            return;
        }
        if (duplicatePrepareResult) {
            String activeTaskBeforeDuplicate = host1State().getActiveTaskUuid();
            String appliedPolicyBeforeDuplicate = host1State().getAppliedPolicy();
            Object ownerBeforeDuplicate = repository.transaction(em ->
                    standardConfigAdapter.readOverrideOwn(em, "writeback.backendResourceUuid", "host1"));
            Object capacityBeforeDuplicate = repository.transaction(em ->
                    standardConfigAdapter.readOverrideOwn(em, "writeback.backendCapacityBytes", "host1"));
            long taskCountBeforeDuplicate = taskCount();
            repository.result(preparation.getUuid(), "Succeeded", "duplicate backend prepared", prepared, fresh);
            assertEquals(taskCountBeforeDuplicate, taskCount());
            assertEquals(appliedPolicyBeforeDuplicate, host1State().getAppliedPolicy());
            assertEquals(activeTaskBeforeDuplicate, host1State().getActiveTaskUuid());
            assertEquals(ownerBeforeDuplicate, repository.transaction(em ->
                    standardConfigAdapter.readOverrideOwn(em, "writeback.backendResourceUuid", "host1")));
            assertEquals(capacityBeforeDuplicate, repository.transaction(em ->
                    standardConfigAdapter.readOverrideOwn(em, "writeback.backendCapacityBytes", "host1")));
            assertEquals(1L, (long) repository.transaction(em -> em.createQuery(
                    "select count(t) from MemoryTaskVO t where t.parentUuid=:p and t.action in ('apply','clearOverride')",
                    Long.class).setParameter("p", parent).getSingleResult()));
        }
        MemoryTaskVO activation = repository.transaction(em -> {
            List<MemoryTaskVO> rows = em.createQuery(
                    "from MemoryTaskVO t where t.parentUuid=:p and t.action in ('apply','clearOverride') order by t.createDate",
                    MemoryTaskVO.class).setParameter("p", parent).getResultList();
            return rows.isEmpty() ? null : rows.get(rows.size() - 1);
        });
        assertNotNull("trusted first-use preparation must queue activation and register its owner UUID; prep="
                + repository.findTask(preparation.getUuid()).getStatus() + "/"
                + repository.findTask(preparation.getUuid()).getReason() + ", host="
                + host1State().getStatus() + "/" + host1State().getReason()
                + ", drainPolicy=" + drain.getPolicy() + ", effective="
                + effectivePolicy("Host", "host1"), activation);
        MemoryPolicyConfig activationPolicy = MemoryPolicyRules.decode(activation.getPolicy());
        assertEquals(Boolean.FALSE, activationPolicy.writeback.enabled);
        assertNull("first activation must not replay the new backend owner in its payload",
                activationPolicy.writeback.backendResourceUuid);
        Object savedOwner = repository.transaction(em ->
                standardConfigAdapter.readOverrideOwn(em, "writeback.backendResourceUuid", "host1"));
        Object savedCapacity = repository.transaction(em ->
                standardConfigAdapter.readOverrideOwn(em, "writeback.backendCapacityBytes", "host1"));
        assertEquals(newResource, savedOwner == null ? null : savedOwner.toString());
        assertEquals("8192", savedCapacity == null ? null : savedCapacity.toString());
        assertTrue("activation must remain dispatchable after owner registration: status="
                        + activation.getStatus() + "/" + activation.getReason()
                        + ", planHash=" + activation.getPolicyPlanHash()
                        + ", hostTarget=" + host1State().getPolicyTargetHash(),
                claimAll().stream()
                        .anyMatch(task -> task.getUuid().equals(activation.getUuid())));

        MemoryAgentResponse activated = new MemoryAgentResponse();
        activated.operationUuid = activation.getUuid(); activated.status = "Succeeded";
        activated.setSuccess(true); activated.hostUuid = "host1"; activated.bootId = boot;
        activated.appliedRevision = activation.getDesiredRevision(); activated.sampleTime = System.currentTimeMillis();
        activated.capabilities = new LinkedHashMap<>();
        for (String capability : Arrays.asList("supported", "ksm", "ksmZeroPages", "zram", "writeback")) {
            activated.capabilities.put(capability, true);
        }
        Map<String, Object> activatedState = new LinkedHashMap<>();
        activatedState.put("bootId", boot); activatedState.put("lastConfirmedOperationUuid", activation.getUuid());
        activatedState.put("lastConfirmedAppliedRevision", activation.getDesiredRevision());
        activatedState.put("poolGeneration", "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee");
        Map<String, Object> activeLifecycle = new LinkedHashMap<>();
        activeLifecycle.put("paused", false); activeLifecycle.put("activeState", "running");
        activatedState.put("lifecycle", activeLifecycle);
        Map<String, Object> activeCapacity = new LinkedHashMap<>();
        activeCapacity.put("effective_logical_bytes", 8192L); activeCapacity.put("effective_physical_limit_bytes", 4096L);
        Map<String, Object> activeZram = new LinkedHashMap<>();
        activeZram.put("enabled", true); activeZram.put("efficiencyEnabled", true);
        activeZram.put("capacity", activeCapacity);
        Map<String, Object> activeBackendResource = new LinkedHashMap<>();
        activeBackendResource.put("resource_uuid", newResource); activeBackendResource.put("capacity_bytes", 8192L);
        activeBackendResource.put("state", "PREPARED"); activeBackendResource.put("active", false);
        activeBackendResource.put("fingerprint", fingerprint); activeBackendResource.put("observed_at", Instant.now().toString());
        Map<String, Object> activeBackend = new LinkedHashMap<>();
        activeBackend.put("backend_resource", activeBackendResource);
        Map<String, Object> activeActual = new LinkedHashMap<>(); activeActual.put("zram", activeZram);
        activeActual.put("writeback", Collections.singletonMap("backend", activeBackend));
        activatedState.put("actual", activeActual); activated.state = activatedState;
        repository.result(activation.getUuid(), "Succeeded", "pool activated", activated, activated);
        MemoryTaskVO restore = repository.findTask(host1State().getActiveTaskUuid());
        assertNotNull("activation must queue retained backend restore before closing root", restore);
        assertTrue("restore must remain a normal policy action", Arrays.asList("apply", "clearOverride")
                .contains(restore.getAction()));
        MemoryPolicyConfig restorePolicy = MemoryPolicyRules.decode(restore.getPolicy());
        assertTrue("sparse restore must not disable the original writeback intent",
                manualDrain ? restorePolicy.writeback.enabled == null || Boolean.TRUE.equals(restorePolicy.writeback.enabled)
                        : restorePolicy.writeback.enabled == null || Boolean.FALSE.equals(restorePolicy.writeback.enabled));
        assertEquals(newResource, restorePolicy.writeback.backendResourceUuid);
        assertEquals(Long.valueOf(8192L), restorePolicy.writeback.backendCapacityBytes);
        assertTrue(claimAll().stream()
                .anyMatch(task -> task.getUuid().equals(restore.getUuid())));
        activated.operationUuid = restore.getUuid(); activated.appliedRevision = restore.getDesiredRevision();
        activated.state.put("lastConfirmedOperationUuid", restore.getUuid());
        activated.state.put("lastConfirmedAppliedRevision", restore.getDesiredRevision());
        repository.result(restore.getUuid(), "Succeeded", "retained backend restored", activated, activated);
        assertEquals(parent, restore.getParentUuid());
        assertEquals("Succeeded", repository.findTask(parent).getStatus());
        assertNull(host1State().getActiveTaskUuid());
        assertEquals(manualDrain ? Boolean.TRUE : Boolean.FALSE,
                MemoryPolicyRules.decode(effectivePolicy("Host", "host1")).writeback.enabled);
        if (manualDrain) {
            assertEquals("Succeeded", repository.findTask(drain.getUuid()).getStatus());
            assertEquals("Succeeded", repository.findTask(drain.getParentUuid()).getStatus());
        }
        Object retainedOwner = repository.transaction(em ->
                standardConfigAdapter.readOverrideOwn(em, "writeback.backendResourceUuid", "host1"));
        assertEquals(newResource, retainedOwner == null ? null : retainedOwner.toString());
        repository.result(restore.getUuid(), "Succeeded", "late duplicate restore", activated, activated);
        assertEquals("Succeeded", repository.findTask(parent).getStatus());
        assertNull(host1State().getActiveTaskUuid());
    }

    @Test public void selectedBackendPreparationWithNoPoolKeepsZramDisabled() {
        selectedBackendPreparationWithNoPoolKeepsZramDisabled(false, false);
    }

    @Test public void selectedBackendPreparationWithNoPoolKeepsExistingKsmControl() {
        selectedBackendPreparationWithNoPoolKeepsZramDisabled(true, false);
    }

    @Test public void selectedBackendPreparationWithNoPoolAcceptsBoundDisabledWritebackControl() {
        selectedBackendPreparationWithNoPoolKeepsZramDisabled(true, false, false, false, false, false, true);
    }

    @Test public void selectedBackendPreparationReconcilesBoundDisabledWritebackControlReadOnly() {
        selectedBackendPreparationWithNoPoolKeepsZramDisabled(true, false, false, false, true, false, true);
    }

    @Test public void selectedBackendPreparationWithNoPoolRejectsNonKsmControl() {
        selectedBackendPreparationWithNoPoolKeepsZramDisabled(true, true);
    }

    @Test public void selectedBackendPreparationWithActiveServiceAndNoPoolSkipsDrain() {
        selectedBackendPreparationWithNoPoolKeepsZramDisabled(false, false, true);
    }

    @Test public void selectedBackendPreparationWithActiveServiceAndNoPoolKeepsKsmControl() {
        selectedBackendPreparationWithNoPoolKeepsZramDisabled(true, false, true);
    }

    @Test public void selectedBackendPreparationWithEmptyKsmHistoryUsesCurrentPreparationProofNotOldAck() {
        selectedBackendPreparationWithNoPoolKeepsZramDisabled(true, false, true, true);
    }

    @Test public void standardSaveCompletesPreparedFirstBackendWithoutReplayingItAndUsesLatestPolicy() {
        selectedBackendPreparationWithNoPoolKeepsZramDisabled(true, false, true, true, true);
    }

    @Test public void selectedBackendPreparationWithNoPoolKeepsConfirmedDisabledServiceControl() {
        selectedBackendPreparationWithNoPoolKeepsZramDisabled(true, false, false, false, false, true);
    }

    @Test public void standardSaveCompletesPreparedBackendAfterConfirmedServiceDisable() {
        selectedBackendPreparationWithNoPoolKeepsZramDisabled(true, false, true, false, true, true);
    }

    private void selectedBackendPreparationWithNoPoolKeepsZramDisabled(
            boolean priorKsmControl, boolean priorControlIncludesZram) {
        selectedBackendPreparationWithNoPoolKeepsZramDisabled(priorKsmControl, priorControlIncludesZram, false);
    }

    private void selectedBackendPreparationWithNoPoolKeepsZramDisabled(
            boolean priorKsmControl, boolean priorControlIncludesZram, boolean activeServiceNotStopped) {
        selectedBackendPreparationWithNoPoolKeepsZramDisabled(priorKsmControl,
                priorControlIncludesZram, activeServiceNotStopped, false);
    }

    private void selectedBackendPreparationWithNoPoolKeepsZramDisabled(
            boolean priorKsmControl, boolean priorControlIncludesZram, boolean activeServiceNotStopped, boolean emptyKsm) {
        selectedBackendPreparationWithNoPoolKeepsZramDisabled(priorKsmControl,
                priorControlIncludesZram, activeServiceNotStopped, emptyKsm, false);
    }

    private void selectedBackendPreparationWithNoPoolKeepsZramDisabled(
            boolean priorKsmControl, boolean priorControlIncludesZram, boolean activeServiceNotStopped,
            boolean emptyKsm, boolean automaticCompletion) {
        selectedBackendPreparationWithNoPoolKeepsZramDisabled(priorKsmControl,
                priorControlIncludesZram, activeServiceNotStopped, emptyKsm, automaticCompletion, false);
    }

    private void selectedBackendPreparationWithNoPoolKeepsZramDisabled(
            boolean priorKsmControl, boolean priorControlIncludesZram, boolean activeServiceNotStopped,
            boolean emptyKsm, boolean automaticCompletion, boolean serviceDisableControl) {
        selectedBackendPreparationWithNoPoolKeepsZramDisabled(priorKsmControl, priorControlIncludesZram,
                activeServiceNotStopped, emptyKsm, automaticCompletion, serviceDisableControl, false);
    }

    private void selectedBackendPreparationWithNoPoolKeepsZramDisabled(
            boolean priorKsmControl, boolean priorControlIncludesZram, boolean activeServiceNotStopped,
            boolean emptyKsm, boolean automaticCompletion, boolean serviceDisableControl,
            boolean boundDisabledWritebackControl) {
        prepareCapacityObservation("Succeeded", false, false, false);
        String boot = "01234567-89ab-cdef-0123-456789abcdef";
        String boundResource = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        String candidateId = "candidate:dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd";
        String fingerprint = candidateId.substring("candidate:".length());
        String control = priorKsmControl ? "0123456789abcdef0123456789abcdef" : null;
        repository.transaction(em -> {
            writeGlobal(em, "zram.enabled", false);
            writeGlobal(em, "writeback.enabled", false);
            writeGlobal(em, "ksm.enabled", false);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Succeeded"); state.setControlOperationUuid(control); state.setActiveTaskUuid(null);
            state.setAppliedPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":false},"
                    + "\"zram\":{\"enabled\":false,\"logicalCapacityBytes\":8192,\"ramLimitBytes\":4096},"
                    + "\"writeback\":{\"enabled\":false}}");
            state.setState("{\"bootId\":\"" + boot + "\",\"managed\":true,\"poolGeneration\":null,"
                    + (priorKsmControl ? "\"lastConfirmedOperationUuid\":\"" + control
                            + "\",\"lastConfirmedAppliedRevision\":0," : "")
                    + "\"lifecycle\":{\"paused\":false,\"activeState\":\"running\"},"
                    + "\"actual\":{\"ksm\":{\"enabled\":false},\"zram\":{\"enabled\":false,"
                    + "\"efficiencyEnabled\":false,\"capacity\":{\"effective_logical_bytes\":8192,"
                    + "\"effective_physical_limit_bytes\":4096,\"apply_state\":\"APPLIED\"}}}}");
            if (boundDisabledWritebackControl) {
                com.google.gson.JsonObject observed = new com.google.gson.JsonParser()
                        .parse(state.getState()).getAsJsonObject();
                observed.addProperty("ksmOnly", true);
                com.google.gson.JsonObject actual = observed.getAsJsonObject("actual");
                com.google.gson.JsonObject currentBackend = new com.google.gson.JsonObject();
                currentBackend.addProperty("resource_uuid", boundResource);
                currentBackend.addProperty("capacity_bytes", 8192L);
                currentBackend.addProperty("state", "PREPARED");
                currentBackend.addProperty("active", false);
                actual.add("writeback", new com.google.gson.JsonParser().parse(
                        "{\"tuning\":{\"enabled\":false},\"backend\":{\"backend_resource\":"
                                + currentBackend + "}}").getAsJsonObject());
                observed.add("backend_resource", currentBackend.deepCopy());
                state.setState(observed.toString());
                state.setAppliedPolicy(MemoryPolicyRules.merge(state.getAppliedPolicy(),
                        "{\"writeback\":{\"backendResourceUuid\":\"" + boundResource
                                + "\",\"backendCapacityBytes\":8192}}", "Host"));
            }
            state.setLastSampleTime(System.currentTimeMillis());
            if (priorKsmControl) {
                MemoryTaskVO controlTask = em.find(MemoryTaskVO.class, control);
                controlTask.setPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":false}"
                        + (priorControlIncludesZram ? ",\"zram\":{\"enabled\":false}" : "") + "}");
                if (emptyKsm) { controlTask.setPolicy("{\"schemaVersion\":1,\"ksm\":{}}"); }
                if (serviceDisableControl) {
                    controlTask.setPolicy("{\"schemaVersion\":1,\"writeback\":{\"enabled\":false}}");
                    com.google.gson.JsonObject observed = new com.google.gson.JsonParser()
                            .parse(state.getState()).getAsJsonObject();
                    observed.getAsJsonObject("actual").add("writeback", new com.google.gson.JsonParser()
                            .parse("{\"tuning\":{\"enabled\":false}}").getAsJsonObject());
                    state.setState(observed.toString());
                }
                if (boundDisabledWritebackControl) {
                    controlTask.setPolicy("{\"schemaVersion\":1,\"ksm\":{},\"writeback\":{\"enabled\":false,"
                            + "\"backendResourceUuid\":\"" + boundResource
                            + "\",\"backendCapacityBytes\":8192}}");
                    controlTask.setAction("clearOverride");
                    controlTask.setDesiredRevision(0L);
                }
            }
            return null;
        });
        Map<String, Object> identity = new LinkedHashMap<>();
        identity.put("bootId", boot); identity.put("device", "/dev/vdb"); identity.put("byIdPath", "/dev/disk/by-id/cold");
        identity.put("majorMinor", "252:16"); identity.put("serial", "cold-serial"); identity.put("wwn", "");
        identity.put("capacityBytes", 8192L); identity.put("transport", "virtio"); identity.put("sysfsPath", "/sys/devices/cold");
        identity.put("sysfsInode", "1"); identity.put("rdev", 64528L); identity.put("nodeInode", 2L);
        Map<String, Object> candidate = new LinkedHashMap<>();
        candidate.put("resourceUuid", null); candidate.put("identity", candidateId); candidate.put("fingerprint", fingerprint);
        candidate.put("stableIdentity", identity); candidate.put("eligible", true); candidate.put("writebackReady", false);
        candidate.put("active", false); candidate.put("type", "block"); candidate.put("device", "/dev/vdb");
        candidate.put("state", "NEEDS_OFFLINE_PREPARATION"); candidate.put("hostBootId", boot); candidate.put("capacityBytes", 8192L);
        MemoryAgentResponse observation = new MemoryAgentResponse(); observation.status = "Succeeded"; observation.setSuccess(true);
        observation.hostUuid = "host1"; observation.bootId = boot; observation.sampleTime = System.currentTimeMillis();
        observation.state = new LinkedHashMap<>(); observation.state.put("hostUuid", "host1"); observation.state.put("bootId", boot);
        observation.state.put("poolGeneration", "none"); observation.state.put("controlOperationUuid", control);
        if (priorKsmControl) {
            observation.state.put("lastConfirmedOperationUuid", control);
            observation.state.put("lastConfirmedAppliedRevision", 0L);
        }
        Map<String, Object> observedZram = new LinkedHashMap<>(); observedZram.put("efficiencyEnabled", false);
        observation.state.put("actual", Collections.singletonMap("zram", observedZram));
        observation.state.put("observedAt", Instant.now().toString()); observation.state.put("status", "AVAILABLE");
        observation.state.put("canPrepare", !activeServiceNotStopped);
        observation.state.put("preparationReasons", activeServiceNotStopped
                ? Collections.singletonList("MEMORY_SERVICE_NOT_STOPPED") : Collections.emptyList());
        observation.state.put("candidates", Collections.singletonList(candidate));
        MemoryActionRequest request = request(); request.setScope("Host"); request.setResourceUuid("host1");
        request.setAction("prepareWritebackBackend"); request.setPolicy("{}");
        request.setExpectedControlOperationUuid(control);
        MemoryBackendPreparation selected = new MemoryBackendPreparation(); selected.setCandidateId(candidateId);
        selected.setExpectedHostBootId(boot); selected.setExpectedPoolGeneration("none"); selected.setBackendCapacityBytes(8192L);
        selected.setResetConfirmed(true); request.setBackendPreparation(selected);
        MemoryBackendPreparationRules.validate(request);
        if (priorControlIncludesZram) {
            expectSelectedPreparationRejected(request, observation);
            assertTrue(claimAll().isEmpty());
            return;
        }
        if (serviceDisableControl) {
            String savedState = host1State().getState();
            for (String defect : Arrays.asList("control", "revision", "missingRevision", "boot", "differentBoot",
                    "cpuEnabled", "tuningEnabled", "tuningMissing", "stale", "enablePolicy", "capacityPolicy")) {
                repository.transaction(em -> {
                    MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
                    com.google.gson.JsonObject changed = new com.google.gson.JsonParser()
                            .parse(savedState).getAsJsonObject();
                    if ("control".equals(defect)) { changed.addProperty("lastConfirmedOperationUuid", "another-control"); }
                    if ("revision".equals(defect)) { changed.addProperty("lastConfirmedAppliedRevision", 99L); }
                    if ("missingRevision".equals(defect)) { changed.remove("lastConfirmedAppliedRevision"); }
                    if ("boot".equals(defect)) { changed.remove("bootId"); }
                    if ("differentBoot".equals(defect)) { changed.addProperty("bootId", "different-boot"); }
                    if ("cpuEnabled".equals(defect)) {
                        changed.getAsJsonObject("actual").getAsJsonObject("zram").addProperty("efficiencyEnabled", true);
                    }
                    if ("tuningEnabled".equals(defect)) {
                        changed.getAsJsonObject("actual").getAsJsonObject("writeback")
                                .getAsJsonObject("tuning").addProperty("enabled", true);
                    }
                    if ("tuningMissing".equals(defect)) { changed.getAsJsonObject("actual").remove("writeback"); }
                    state.setState(changed.toString());
                    state.setLastSampleTime("stale".equals(defect) ? 1L : System.currentTimeMillis());
                    MemoryTaskVO task = em.find(MemoryTaskVO.class, control);
                    task.setPolicy("enablePolicy".equals(defect) ? "{\"writeback\":{\"enabled\":true}}"
                            : "capacityPolicy".equals(defect)
                            ? "{\"zram\":{\"enabled\":false,\"logicalCapacityBytes\":8192}}"
                            : "{\"schemaVersion\":1,\"writeback\":{\"enabled\":false}}");
                    return null;
                });
                expectSelectedPreparationRejected(request, observation);
                assertTrue("unproven service control must not dispatch: " + defect,
                        claimAll().isEmpty());
            }
            repository.transaction(em -> {
                MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
                state.setState(savedState); state.setLastSampleTime(System.currentTimeMillis());
                em.find(MemoryTaskVO.class, control).setPolicy("{\"schemaVersion\":1,\"writeback\":{\"enabled\":false}}");
                return null;
            });
        }
        if (boundDisabledWritebackControl) {
            String savedState = host1State().getState();
            String savedPolicy = repository.findTask(control).getPolicy();
            for (String defect : Arrays.asList("missingUuid", "missingCapacity", "extraField", "capacityType",
                    "enabled", "policyIdentity", "policyCapacity", "stateIdentity", "actualIdentity",
                    "actualCapacity", "boot")) {
                repository.transaction(em -> {
                    MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
                    MemoryTaskVO task = em.find(MemoryTaskVO.class, control);
                    com.google.gson.JsonObject policy = new com.google.gson.JsonParser()
                            .parse(savedPolicy).getAsJsonObject();
                    com.google.gson.JsonObject writeback = policy.getAsJsonObject("writeback");
                    com.google.gson.JsonObject observed = new com.google.gson.JsonParser()
                            .parse(savedState).getAsJsonObject();
                    if ("missingUuid".equals(defect)) { writeback.remove("backendResourceUuid"); }
                    if ("missingCapacity".equals(defect)) { writeback.remove("backendCapacityBytes"); }
                    if ("extraField".equals(defect)) { writeback.addProperty("unknown", false); }
                    if ("capacityType".equals(defect)) { writeback.addProperty("backendCapacityBytes", "8192"); }
                    if ("enabled".equals(defect)) { writeback.addProperty("enabled", true); }
                    if ("policyIdentity".equals(defect)) { writeback.addProperty("backendResourceUuid", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"); }
                    if ("policyCapacity".equals(defect)) { writeback.addProperty("backendCapacityBytes", 4096L); }
                    if ("stateIdentity".equals(defect)) {
                        observed.getAsJsonObject("backend_resource").addProperty("resource_uuid", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
                    }
                    if ("actualIdentity".equals(defect)) {
                        observed.getAsJsonObject("actual").getAsJsonObject("writeback")
                                .getAsJsonObject("backend").getAsJsonObject("backend_resource")
                                .addProperty("resource_uuid", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
                    }
                    if ("actualCapacity".equals(defect)) {
                        observed.getAsJsonObject("actual").getAsJsonObject("writeback")
                                .getAsJsonObject("backend").getAsJsonObject("backend_resource")
                                .addProperty("capacity_bytes", 4096L);
                    }
                    if ("boot".equals(defect)) { observed.addProperty("bootId", "different-boot"); }
                    task.setPolicy(policy.toString()); state.setState(observed.toString());
                    return null;
                });
                expectSelectedPreparationRejected(request, observation);
                assertTrue("unproven bound writeback control must not dispatch: " + defect,
                        claimAll().isEmpty());
            }
            repository.transaction(em -> {
                em.find(MemoryStateVO.class, "host1").setState(savedState);
                em.find(MemoryTaskVO.class, control).setPolicy(savedPolicy);
                return null;
            });
        }
        if (activeServiceNotStopped) {
            observation.state.put("preparationReasons", Collections.singletonList("UNKNOWN_PREPARATION_BLOCKER"));
            expectSelectedPreparationRejected(request, observation);
            observation.state.put("preparationReasons", Arrays.asList(
                    "MEMORY_SERVICE_NOT_STOPPED", "DRAIN_PROOF_UNAVAILABLE"));
            expectSelectedPreparationRejected(request, observation);
            observation.state.put("preparationReasons", Collections.singletonList("MEMORY_SERVICE_NOT_STOPPED"));
            repository.transaction(em -> {
                MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
                com.google.gson.JsonObject freshState = new com.google.gson.JsonParser()
                        .parse(state.getState()).getAsJsonObject();
                freshState.getAsJsonObject("actual").getAsJsonObject("zram")
                        .addProperty("efficiencyEnabled", true);
                state.setState(freshState.toString());
                return null;
            });
            expectSelectedPreparationRejected(request, observation);
            repository.transaction(em -> {
                MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
                com.google.gson.JsonObject freshState = new com.google.gson.JsonParser()
                        .parse(state.getState()).getAsJsonObject();
                freshState.getAsJsonObject("actual").getAsJsonObject("zram").remove("efficiencyEnabled");
                state.setState(freshState.toString());
                return null;
            });
            expectSelectedPreparationRejected(request, observation);
            repository.transaction(em -> {
                MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
                com.google.gson.JsonObject freshState = new com.google.gson.JsonParser()
                        .parse(state.getState()).getAsJsonObject();
                freshState.getAsJsonObject("actual").getAsJsonObject("zram")
                        .addProperty("efficiencyEnabled", false);
                freshState.addProperty("bootId", "different-boot");
                state.setState(freshState.toString());
                return null;
            });
            expectSelectedPreparationRejected(request, observation);
            repository.transaction(em -> {
                MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
                com.google.gson.JsonObject freshState = new com.google.gson.JsonParser()
                        .parse(state.getState()).getAsJsonObject();
                freshState.addProperty("bootId", boot);
                state.setState(freshState.toString());
                return null;
            });
            repository.transaction(em -> {
                MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
                com.google.gson.JsonObject freshState = new com.google.gson.JsonParser()
                        .parse(state.getState()).getAsJsonObject();
                freshState.addProperty("poolGeneration", "native-existing-pool");
                state.setState(freshState.toString());
                return null;
            });
            expectSelectedPreparationRejected(request, observation);
            repository.transaction(em -> {
                MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
                com.google.gson.JsonObject freshState = new com.google.gson.JsonParser()
                        .parse(state.getState()).getAsJsonObject();
                freshState.add("poolGeneration", com.google.gson.JsonNull.INSTANCE);
                state.setState(freshState.toString());
                return null;
            });
            repository.transaction(em -> {
                em.find(MemoryStateVO.class, "host1").setActiveTaskUuid("unresolved-selected-control");
                return null;
            });
            expectSelectedPreparationRejected(request, observation);
            repository.transaction(em -> {
                em.find(MemoryStateVO.class, "host1").setActiveTaskUuid(null);
                return null;
            });
        }
        String root;
        try { root = repository.submit(request, observation); }
        catch (MemoryOperationException rejected) {
            fail("cold selected backend should be accepted for preparation: " + rejected.getCode()
                    + "/" + rejected.getMessage()); return;
        }
        assertEquals("prepareWritebackBackend", repository.findTask(root).getAction());
        if (activeServiceNotStopped) {
            long drainChildren = repository.transaction(em -> em.createQuery(
                    "select count(t) from MemoryTaskVO t where t.parentUuid=:p and t.action='drain'",
                    Long.class).setParameter("p", root).getSingleResult());
            assertEquals("the cold active-service exception must not create a drain child", 0L, drainChildren);
        }
        assertEquals(Boolean.FALSE, MemoryPolicyRules.decode(host1State().getAppliedPolicy()).zram.enabled);
        MemoryTaskVO preparation = claimAll().stream()
                .filter(task -> "prepareWritebackBackend".equals(task.getAction())).findFirst().orElse(null);
        assertNotNull(preparation);
        assertEquals("selected preparation must retain the caller's current control anchor",
                control, preparation.getExpectedControlOperationUuid());
        String newResource = "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee";
        Map<String, Object> targetIdentity = new LinkedHashMap<>();
        targetIdentity.put("boot_id", boot); targetIdentity.put("canonical_device", "/dev/vdb");
        targetIdentity.put("capacity_bytes", 8192L); targetIdentity.put("rdev", 64528L);
        targetIdentity.put("node_inode", 2L); targetIdentity.put("sysfs_inode", 1L);
        targetIdentity.put("stack_sha256", "4f467c52a18bfc7b2d1c844de18d4262913a3f5f24c7f1614ed06d8c636a2e95");
        targetIdentity.put("direct_io", false);
        Map<String, Object> maintenance = new LinkedHashMap<>(); maintenance.put("stage", "APPLIED");
        maintenance.put("request_id", preparation.getUuid()); maintenance.put("candidate_id", candidateId);
        maintenance.put("backend_resource_uuid", newResource); maintenance.put("host_boot_id", boot);
        maintenance.put("target_identity", targetIdentity);
        MemoryAgentResponse prepared = new MemoryAgentResponse(); prepared.operationUuid = preparation.getUuid();
        prepared.status = "Succeeded"; prepared.setSuccess(true); prepared.hostUuid = "host1";
        prepared.bootId = boot; prepared.appliedRevision = preparation.getDesiredRevision();
        prepared.sampleTime = System.currentTimeMillis(); prepared.state = new LinkedHashMap<>();
        prepared.state.put("maintenance", maintenance);
        Map<String, Object> backendResource = new LinkedHashMap<>(); backendResource.put("resource_uuid", newResource);
        backendResource.put("capacity_bytes", 8192L); backendResource.put("state", "PREPARED");
        backendResource.put("active", false); backendResource.put("fingerprint", fingerprint);
        backendResource.put("observed_at", Instant.now().toString());
        Map<String, Object> backend = new LinkedHashMap<>(); backend.put("backend_resource", backendResource);
        Map<String, Object> actual = new LinkedHashMap<>();
        Map<String, Object> zram = new LinkedHashMap<>(); zram.put("enabled", false); zram.put("efficiencyEnabled", false);
        Map<String, Object> capacity = new LinkedHashMap<>();
        capacity.put("effective_logical_bytes", 8192L); capacity.put("effective_physical_limit_bytes", 4096L);
        zram.put("capacity", capacity); actual.put("zram", zram);
        Map<String, Object> writeback = new LinkedHashMap<>(); writeback.put("backend", backend);
        actual.put("writeback", writeback);
        Map<String, Object> freshState = new LinkedHashMap<>(); freshState.put("bootId", boot);
        freshState.put("poolGeneration", "none"); freshState.put("actual", actual);
        if (serviceDisableControl) {
            writeback.put("tuning", Collections.singletonMap("enabled", false));
        }
        if (priorKsmControl && !emptyKsm) {
            freshState.put("lastConfirmedOperationUuid", control);
            freshState.put("lastConfirmedAppliedRevision", 0L);
        }
        Map<String, Object> lifecycle = new LinkedHashMap<>(); lifecycle.put("paused", false); lifecycle.put("activeState", "running");
        freshState.put("lifecycle", lifecycle);
        MemoryAgentResponse fresh = new MemoryAgentResponse(); fresh.status = "Succeeded"; fresh.setSuccess(true);
        fresh.hostUuid = "host1"; fresh.bootId = boot; fresh.sampleTime = System.currentTimeMillis();
        fresh.state = freshState; fresh.capabilities = new LinkedHashMap<>(); fresh.capabilities.put("supported", true);
        fresh.capabilities.put("ksm", true); fresh.capabilities.put("ksmZeroPages", true);
        fresh.capabilities.put("zram", true); fresh.capabilities.put("writeback", true);
        String appliedPolicyBeforePreparationResult = host1State().getAppliedPolicy();
        String controlBeforePreparationResult = host1State().getControlOperationUuid();
        MemoryTaskVO resultTask = preparation;
        if (automaticCompletion) {
            repository.result(preparation.getUuid(), "Unknown", "earlier completion proof unavailable", null);
            standardCommit(Collections.singletonList(standardChange(MemoryStandardField.KSM_ZERO_PAGES,
                    "host1", "HostVO", "true")), null, false);
            resultTask = claimAll().stream().findFirst().get();
            assertTrue(MemoryRepository.isConfigurationReconcile(resultTask));
            assertEquals(preparation.getUuid(), resultTask.getReconcileOperationUuid());
            assertEquals(control, host1State().getControlOperationUuid());
            assertEquals("Unknown", repository.findTask(preparation.getUuid()).getStatus());
            // The immutable receipt alone must not advance the new desired
            // configuration across a reboot. No device command is requeued.
            fresh.bootId = "another-boot";
            repository.result(resultTask.getUuid(), "Succeeded", "completed receipt", prepared, fresh);
            assertEquals("Unknown", host1State().getStatus());
            assertEquals(control, host1State().getControlOperationUuid());
            assertNull(repository.transaction(em -> standardConfigAdapter.readOverrideOwn(
                    em, "writeback.backendResourceUuid", "host1")));
            assertTrue(claimAll().isEmpty());
            fresh.bootId = boot;
        }
        repository.result(resultTask.getUuid(), "Succeeded", "cold backend prepared", prepared, fresh);
        Object savedOwner = repository.transaction(em ->
                standardConfigAdapter.readOverrideOwn(em, "writeback.backendResourceUuid", "host1"));
        MemoryTaskVO persistedPreparation = repository.findTask(preparation.getUuid());
        assertEquals("trusted prepare must register the verified owner; prep=" + persistedPreparation.getStatus()
                        + "/" + persistedPreparation.getReason(),
                newResource, savedOwner == null ? null : savedOwner.toString());
        Object savedCapacity = repository.transaction(em ->
                standardConfigAdapter.readOverrideOwn(em, "writeback.backendCapacityBytes", "host1"));
        assertEquals("8192", savedCapacity == null ? null : savedCapacity.toString());
        if (automaticCompletion) {
            assertEquals("Succeeded", persistedPreparation.getStatus());
            assertEquals(Boolean.TRUE, MemoryPolicyRules.decode(effectivePolicy("Host", "host1")).ksm.zeroPagesEnabled);
            MemoryTaskVO next = claimAll().stream().findFirst().get();
            assertTrue(Arrays.asList("apply", "clearOverride").contains(next.getAction()));
            assertEquals(Boolean.TRUE, MemoryPolicyRules.decode(next.getPolicy()).ksm.zeroPagesEnabled);
            assertEquals(1L, (long) repository.transaction(em -> em.createQuery(
                    "select count(t) from MemoryTaskVO t where t.hostUuid='host1' and t.action='prepareWritebackBackend'",
                    Long.class).getSingleResult()));
            return;
        }
        String expectedAppliedPolicy = MemoryPolicyRules.merge(appliedPolicyBeforePreparationResult,
                "{\"writeback\":{\"backendResourceUuid\":\"" + newResource
                        + "\",\"backendCapacityBytes\":8192}}", "Host");
        assertEquals("trusted prepare binding may add only the verified owner and whole-disk capacity",
                new com.google.gson.JsonParser().parse(expectedAppliedPolicy),
                new com.google.gson.JsonParser().parse(host1State().getAppliedPolicy()));
        assertEquals("trusted prepare must preserve the current control; prep="
                        + persistedPreparation.getStatus() + "/" + persistedPreparation.getReason(),
                controlBeforePreparationResult, host1State().getControlOperationUuid());
        MemoryTaskVO rootNow = repository.findTask(root);
        assertEquals("Succeeded", rootNow.getStatus());
        assertNull(host1State().getActiveTaskUuid());
        assertEquals(Boolean.FALSE, MemoryPolicyRules.decode(
                effectivePolicy("Host", "host1")).zram.enabled);
        assertEquals(Boolean.FALSE, MemoryPolicyRules.decode(
                effectivePolicy("Host", "host1")).writeback.enabled);
        assertTrue("trusted PREPARED proof must bind owner/capacity without a normal apply child",
                repository.transaction(em -> em.createQuery(
                        "select count(t) from MemoryTaskVO t where t.parentUuid=:p and t.action in ('apply','clearOverride')",
                        Long.class).setParameter("p", root).getSingleResult() == 0L));
    }

    private void expectSelectedPreparationRejected(MemoryActionRequest request,
                                                    MemoryAgentResponse observation) {
        try {
            repository.submit(request, observation);
            fail("invalid selected backend evidence must be rejected");
        } catch (MemoryOperationException expected) { }
    }

    private static final String DRAIN_BOOT = "01234567-89ab-cdef-0123-456789abcdef";
    private static final String DRAIN_POOL = "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";

    private MemoryTaskVO prepareDrainedHost(boolean poolReadyProof) {
        prepareCapacityObservation("Succeeded", true, false, false);
        return repository.transaction(em -> {
            MemoryTaskVO drain = testTask("user-drain", null, "drain", "Succeeded", null);
            em.persist(drain);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setControlOperationUuid(drain.getUuid());
            if (poolReadyProof) {
                com.google.gson.JsonObject observed = new com.google.gson.JsonParser().parse(state.getState()).getAsJsonObject();
                observed.addProperty("lastConfirmedOperationUuid", drain.getUuid());
                com.google.gson.JsonObject proof = new com.google.gson.JsonObject();
                proof.addProperty("drainOperationUuid", drain.getUuid()); proof.addProperty("oldPoolGeneration", DRAIN_POOL);
                proof.addProperty("maintenanceReady", true); proof.addProperty("maintenanceArchived", true);
                proof.addProperty("executorExited", true); proof.addProperty("activeOperations", 0);
                proof.addProperty("readyForInitialization", true); proof.addProperty("originalDeviceInactive", true);
                proof.addProperty("oldPoolOwnershipAbsent", true); proof.addProperty("poolOwnedByService", false);
                observed.add("maintenanceProof", proof);
                state.setState(observed.toString());
            }
            return drain;
        });
    }

    private MemoryAgentResponse drainInventory(String pool, boolean canPrepare, List<String> reasons) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("hostUuid", "host1"); state.put("bootId", DRAIN_BOOT);
        state.put("observedAt", Instant.now().toString()); state.put("status", "AVAILABLE");
        state.put("poolGeneration", pool); state.put("controlOperationUuid", "user-drain");
        state.put("canPrepareZramPool", canPrepare);
        state.put("zramPoolPreparationQuality", canPrepare ? "Fresh" : "Ineligible");
        state.put("zramPoolPreparationObservedAt", System.currentTimeMillis());
        state.put("zramPoolPreparationReasons", reasons);
        state.put("reasons", Collections.emptyList()); state.put("candidates", Collections.emptyList());
        MemoryAgentResponse observation = new MemoryAgentResponse();
        observation.setSuccess(true); observation.status = "Succeeded"; observation.hostUuid = "host1";
        observation.state = state;
        return observation;
    }

    @Test public void resumeOfDrainedHostResetsThePreservedPoolAndActivatesIt() {
        MemoryTaskVO drain = prepareDrainedHost(false);
        assertTrue(repository.isDrainedHost("host1"));
        String root = repository.recoverDrain("host1", "admin:actor",
                drainInventory(DRAIN_POOL, true, Collections.emptyList()));
        assertEquals("resume", repository.findTask(root).getAction());
        assertEquals("the drain stays the Host fence until the reset is proven", "user-drain",
                host1State().getControlOperationUuid());
        MemoryTaskVO preparation = claimAll().get(0);
        assertEquals(MemoryZramPoolPreparationRules.ACTION, preparation.getAction());
        assertEquals("user-drain", preparation.getExpectedControlOperationUuid());
        assertTrue(preparation.getPolicy().contains(DRAIN_POOL));
        MemoryTaskVO restore = assertCapacityPreparationQueuesRestore(drain, preparation, null,
                repository.findTask(root).getPolicy(), root);
        assertEquals(Boolean.TRUE, MemoryPolicyRules.decode(restore.getPolicy()).zram.enabled);
    }

    @Test public void resumeAfterRebootActivatesTheDeviceTheAgentProvesBlank() {
        prepareDrainedHost(true);
        repository.transaction(em -> {
            // The drain journal belongs to the previous boot, so the Agent no longer confirms it.
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            com.google.gson.JsonObject observed = new com.google.gson.JsonParser().parse(state.getState()).getAsJsonObject();
            observed.remove("lastConfirmedOperationUuid");
            state.setState(observed.toString());
            return null;
        });
        String root = repository.recoverDrain("host1", "admin:actor", drainInventory(null, false, Arrays.asList("DRAIN_PROOF_UNAVAILABLE", "MEMORY_SERVICE_NOT_STOPPED")));
        MemoryTaskVO activation = claimAll().get(0);
        assertEquals(root, activation.getParentUuid());
        assertEquals("user-drain", activation.getExpectedControlOperationUuid());
        assertEquals(Boolean.TRUE, MemoryPolicyRules.decode(activation.getPolicy()).zram.enabled);
        assertEquals(activation.getUuid(), host1State().getControlOperationUuid());
    }

    @Test public void rejectedRecoveryActivationKeepsTheHostRecoverable() {
        prepareDrainedHost(true);
        repository.recoverDrain("host1", "admin:actor", drainInventory(null, false, Arrays.asList("DRAIN_PROOF_UNAVAILABLE", "MEMORY_SERVICE_NOT_STOPPED")));
        MemoryTaskVO activation = claimAll().get(0);
        MemoryAgentResponse rejected = new MemoryAgentResponse();
        rejected.setSuccess(false); rejected.status = "Failed"; rejected.hostUuid = "host1";
        rejected.reasonCode = "MAINTENANCE_ACTIVATION_VM_INVENTORY_NOT_READY";
        rejected.state = Collections.singletonMap("activationRejected", true);
        repository.result(activation.getUuid(), "Failed", rejected.reasonCode, rejected, null);
        assertEquals("user-drain", host1State().getControlOperationUuid());
        assertTrue("a rejected activation leaves the Agent under the drain", repository.isDrainedHost("host1"));
    }

    @Test public void drainRecoveryRefusesHostsItCannotProve() {
        prepareDrainedHost(false);
        try {
            repository.recoverDrain("host1", "admin:actor", drainInventory(DRAIN_POOL, false, Collections.singletonList("OPERATION_JOURNAL_UNRESOLVED")));
            fail("an ineligible drained Host must not be recovered");
        } catch (MemoryOperationException expected) {
            assertEquals("MEMORY_DRAIN_RECOVERY_UNAVAILABLE", expected.getCode());
        }
        assertNull(host1State().getActiveTaskUuid());
        repository.transaction(em -> { em.find(MemoryStateVO.class, "host1").setControlOperationUuid(null); return null; });
        assertFalse(repository.isDrainedHost("host1"));
        try {
            repository.recoverDrain("host1", "admin:actor", drainInventory(DRAIN_POOL, true, Collections.emptyList()));
            fail("only a completed drain can be recovered");
        } catch (MemoryOperationException expected) {
            assertEquals("MEMORY_CONTROL_OPERATION_FENCED", expected.getCode());
        }
    }

    @Test public void ordinaryPlanningDoesNotReactivateADrainedHost() {
        prepareDrainedHost(true);
        assertFalse("a reconnect must not undo a drain", repository.applyEffectivePolicyAfterConnect("host1"));
        assertNull(host1State().getActiveTaskUuid());
        assertEquals("user-drain", host1State().getControlOperationUuid());
    }

    @Test public void resumeRejectsDrainFenceAndOnlyOffersResumeForSuccessfulPause() {
        long now = System.currentTimeMillis();
        repository.transaction(em -> {
            MemoryTaskVO drain = testTask("successful-drain", null, "drain", "Succeeded", null);
            em.persist(drain);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Succeeded"); state.setControlOperationUuid(drain.getUuid());
            state.setLastSampleTime(now);
            state.setState("{\"bootId\":\"boot-a\",\"lastConfirmedOperationUuid\":\"successful-drain\","
                    + "\"lifecycle\":{\"paused\":true,\"activeState\":\"paused\"}}");
            return null;
        });
        assertFalse(repository.canResumeControl("host1", "successful-drain"));
        MemoryActionRequest resume = request();
        resume.setScope("Host"); resume.setResourceUuid("host1");
        resume.setAction("resume"); resume.setPolicy("{}");
        resume.setExpectedControlOperationUuid("successful-drain");
        try { repository.submit(resume); fail("a drain fence is not a resumable pause fence"); }
        catch (MemoryOperationException expected) {
            assertEquals("MEMORY_CONTROL_OPERATION_FENCED", expected.getCode());
        }
        assertNull(repository.states(Collections.singletonList("host1"), 0, 1).get(0).getActiveTaskUuid());
        assertEquals("successful-drain", repository.states(Collections.singletonList("host1"), 0, 1)
                .get(0).getControlOperationUuid());
    }

    @Test public void rejectedResumeRecoveryRestoresFenceOnlyAfterExactAgentProof() {
        long now = System.currentTimeMillis();
        repository.transaction(em -> {
            em.persist(testTask("prior-drain", null, "drain", "Succeeded", null));
            MemoryTaskVO failed = testTask("rejected-resume", null, "resume", "Failed", null);
            failed.setExpectedControlOperationUuid("prior-drain");
            failed.setReason("CONTROL_OPERATION_FENCED");
            em.persist(failed);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            // A later read-only Agent observation may refresh the Host status
            // to Succeeded while the failed resume remains the control owner.
            state.setStatus("Succeeded"); state.setControlOperationUuid(failed.getUuid());
            state.setLastSampleTime(now);
            state.setState("{\"bootId\":\"boot-a\",\"lastConfirmedOperationUuid\":\"prior-drain\","
                    + "\"lifecycle\":{\"paused\":true,\"activeState\":\"paused\"}}");
            return null;
        });
        assertTrue(repository.canReconcileRejectedResume("host1", "rejected-resume"));
        MemoryActionRequest reconcile = request();
        reconcile.setScope("Host"); reconcile.setResourceUuid("host1");
        reconcile.setAction("reconcile"); reconcile.setPolicy("{}");
        reconcile.setExpectedControlOperationUuid("prior-drain");
        String parent = repository.submit(reconcile);
        MemoryTaskVO child = claimAll().stream()
                .filter(task -> parent.equals(task.getUuid()) || parent.equals(task.getParentUuid())).findFirst().get();
        assertEquals("rejected-resume", child.getReconcileOperationUuid());
        assertEquals("prior-drain", child.getExpectedControlOperationUuid());
        assertEquals("rejected-resume", repository.states(Collections.singletonList("host1"), 0, 1)
                .get(0).getControlOperationUuid());

        MemoryAgentResponse proof = rejectedResumeProof("host1", "boot-a", "rejected-resume", "prior-drain", "drain");
        repository.result(child.getUuid(), "Succeeded", null, proof);

        MemoryTaskVO reconciled = repository.findTask(child.getUuid());
        assertEquals("Succeeded", reconciled.getStatus());
        assertEquals("Failed", repository.findTask("rejected-resume").getStatus());
        assertEquals("prior-drain", repository.states(Collections.singletonList("host1"), 0, 1)
                .get(0).getControlOperationUuid());
        // A delayed duplicate callback is not allowed to replay proof-side effects
        // after the reconcile child has already reached a terminal state.
        repository.result(child.getUuid(), "Failed", "late duplicate result", null);
        assertEquals("Succeeded", repository.findTask(child.getUuid()).getStatus());
        assertEquals("prior-drain", repository.states(Collections.singletonList("host1"), 0, 1)
                .get(0).getControlOperationUuid());
    }

    @Test public void rejectedResumeIsReconciledOnceWithoutAnOperatorRequest() {
        long now = System.currentTimeMillis();
        repository.transaction(em -> {
            em.persist(testTask("prior-drain", null, "drain", "Succeeded", null));
            MemoryTaskVO failed = testTask("rejected-resume", null, "resume", "Failed", null);
            failed.setExpectedControlOperationUuid("prior-drain");
            failed.setReason("CONTROL_OPERATION_FENCED");
            em.persist(failed);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Succeeded"); state.setControlOperationUuid(failed.getUuid());
            state.setLastSampleTime(now);
            state.setState("{\"bootId\":\"boot-a\",\"lastConfirmedOperationUuid\":\"prior-drain\","
                    + "\"lifecycle\":{\"paused\":true,\"activeState\":\"paused\"}}");
            return null;
        });
        assertTrue(repository.reconcileRejectedResume("host1"));
        long tasks = taskCount();
        assertFalse("a repeated observation does not queue another reconcile", repository.reconcileRejectedResume("host1"));
        assertEquals(tasks, taskCount());
        MemoryTaskVO child = claimAll().stream()
                .filter(task -> "reconcile".equals(task.getAction())).findFirst().get();
        assertEquals("rejected-resume", child.getReconcileOperationUuid());
        assertEquals("prior-drain", child.getExpectedControlOperationUuid());

        repository.result(child.getUuid(), "Succeeded", null,
                rejectedResumeProof("host1", "boot-a", "rejected-resume", "prior-drain", "drain"));
        assertEquals("prior-drain", host1State().getControlOperationUuid());
        assertFalse(repository.reconcileRejectedResume("host1"));
    }

    @Test public void rejectedResumeRecoveryFailsClosedOnWrongBootOrControlProof() {
        long now = System.currentTimeMillis();
        repository.transaction(em -> {
            em.persist(testTask("prior-pause", null, "pause", "Succeeded", null));
            MemoryTaskVO failed = testTask("rejected-resume", null, "resume", "Failed", null);
            failed.setExpectedControlOperationUuid("prior-pause"); failed.setReason("CONTROL_OPERATION_FENCED");
            em.persist(failed);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Succeeded"); state.setControlOperationUuid(failed.getUuid());
            state.setLastSampleTime(now);
            state.setState("{\"bootId\":\"boot-a\",\"lastConfirmedOperationUuid\":\"prior-pause\","
                    + "\"lifecycle\":{\"paused\":true,\"activeState\":\"paused\"}}");
            return null;
        });
        MemoryActionRequest reconcile = request();
        reconcile.setScope("Host"); reconcile.setResourceUuid("host1");
        reconcile.setAction("reconcile"); reconcile.setPolicy("{}");
        reconcile.setExpectedControlOperationUuid("prior-pause");
        String parent = repository.submit(reconcile);
        MemoryTaskVO child = claimAll().stream()
                .filter(task -> parent.equals(task.getUuid()) || parent.equals(task.getParentUuid())).findFirst().get();
        MemoryAgentResponse wrongBoot = rejectedResumeProof("host1", "boot-b", "rejected-resume", "prior-pause", "pause");
        repository.result(child.getUuid(), "Succeeded", null, wrongBoot);
        assertEquals("Unknown", repository.findTask(child.getUuid()).getStatus());
        assertEquals("Failed", repository.findTask("rejected-resume").getStatus());
        assertEquals("rejected-resume", repository.states(Collections.singletonList("host1"), 0, 1)
                .get(0).getControlOperationUuid());
    }

    private MemoryAgentResponse rejectedResumeProof(String host, String boot, String rejected, String control,
                                                    String action) {
        MemoryAgentResponse response = new MemoryAgentResponse();
        response.setSuccess(true); response.status = "Succeeded"; response.operationUuid = rejected;
        response.bootId = boot; response.sampleTime = System.currentTimeMillis();
        Map<String, Object> proof = new LinkedHashMap<>();
        proof.put("schemaVersion", 1); proof.put("hostUuid", host); proof.put("bootId", boot);
        proof.put("rejectedOperationUuid", rejected); proof.put("controlOperationUuid", control);
        proof.put("controlAction", action); proof.put("controlStage", "APPLIED");
        proof.put("rejectedStage", "REJECTED"); proof.put("reasonCode", "CONTROL_OPERATION_FENCED");
        response.state = new LinkedHashMap<>(); response.state.put("phase", "CONTROL_NOT_ISSUED");
        response.state.put("controlFenceProof", proof);
        return response;
    }

    @Test public void reconcilingUnknownBackendPreparationPreservesOriginalDrainFence() {
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setControlOperationUuid("completed-drain");
            state.setStatus("Unknown");
            em.persist(testTask("completed-drain", null, "drain", "Succeeded", null));
            MemoryTaskVO original = testTask("pool-preparation-task", "pool-preparation-parent",
                    "prepareWritebackBackend", "Unknown", null);
            original.setExpectedControlOperationUuid("completed-drain");
            MemoryTaskVO parent = testTask("pool-preparation-parent", null,
                    "prepareWritebackBackend", "Unknown", null);
            em.persist(parent); em.persist(original);
            state.setActiveTaskUuid(original.getUuid());
            return null;
        });

        MemoryActionRequest reconcile = request();
        reconcile.setScope("Host"); reconcile.setResourceUuid("host1");
        reconcile.setAction("reconcile"); reconcile.setPolicy("{}");
        String accepted = repository.submit(reconcile);
        MemoryTaskVO child = claimAll().stream()
                .filter(task -> accepted.equals(task.getUuid()) || accepted.equals(task.getParentUuid())).findFirst().get();
        assertEquals("pool-preparation-task", child.getReconcileOperationUuid());
        MemoryStateVO state = repository.states(Collections.singletonList("host1"), 0, 1).get(0);
        assertEquals("completed-drain", state.getControlOperationUuid());
        assertEquals(child.getUuid(), state.getActiveTaskUuid());
    }

    @Test public void reconcilingAliasOfUnknownPreparationRequiresOriginalDrainFence() {
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            // The active task can be a reconcile alias, but its Host control
            // fence must remain the original successful drain UUID.
            state.setControlOperationUuid("completed-drain");
            state.setStatus("Unknown");
            em.persist(testTask("completed-drain", null, "drain", "Succeeded", null));
            MemoryTaskVO originalParent = testTask("pool-preparation-parent", null,
                    "prepareWritebackBackend", "Unknown", null);
            MemoryTaskVO original = testTask("pool-preparation-task", "pool-preparation-parent",
                    "prepareWritebackBackend", "Unknown", null);
            original.setExpectedControlOperationUuid("completed-drain");
            MemoryTaskVO priorReconcile = testTask("prior-reconcile", "prior-reconcile-parent",
                    "reconcile", "Unknown", "pool-preparation-task");
            MemoryTaskVO priorParent = testTask("prior-reconcile-parent", null,
                    "reconcile", "Unknown", null);
            em.persist(originalParent); em.persist(original); em.persist(priorParent); em.persist(priorReconcile);
            state.setActiveTaskUuid(priorReconcile.getUuid());
            return null;
        });

        MemoryActionRequest reconcile = request();
        reconcile.setScope("Host"); reconcile.setResourceUuid("host1");
        reconcile.setAction("reconcile"); reconcile.setPolicy("{}");
        String accepted = repository.submit(reconcile);
        MemoryTaskVO child = claimAll().stream()
                .filter(task -> accepted.equals(task.getUuid()) || accepted.equals(task.getParentUuid())).findFirst().get();
        assertEquals("pool-preparation-task", child.getReconcileOperationUuid());
        assertEquals("completed-drain", repository.states(Collections.singletonList("host1"), 0, 1)
                .get(0).getControlOperationUuid());
    }

    @Test public void unknownDifferentControlOwnerIsNotReplacedByMaintenanceReconcile() {
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setControlOperationUuid("completed-drain"); state.setStatus("Unknown");
            em.persist(testTask("completed-drain", null, "drain", "Succeeded", null));
            em.persist(testTask("different-control", null, "pause", "Succeeded", null));
            MemoryTaskVO parent = testTask("pool-preparation-parent", null,
                    "prepareWritebackBackend", "Unknown", null);
            MemoryTaskVO original = testTask("pool-preparation-task", "pool-preparation-parent",
                    "prepareWritebackBackend", "Unknown", null);
            original.setExpectedControlOperationUuid("completed-drain");
            em.persist(parent); em.persist(original); state.setActiveTaskUuid(original.getUuid());
            state.setControlOperationUuid("different-control");
            return null;
        });
        MemoryActionRequest reconcile = request();
        reconcile.setScope("Host"); reconcile.setResourceUuid("host1");
        reconcile.setAction("reconcile"); reconcile.setPolicy("{}");
        try { repository.submit(reconcile); fail("must not overwrite an unrelated Host control owner"); }
        catch (MemoryOperationException expected) {
            assertEquals("MEMORY_CONTROL_OPERATION_FENCED", expected.getCode());
        }
        assertEquals("different-control", repository.states(Collections.singletonList("host1"), 0, 1)
                .get(0).getControlOperationUuid());
    }

    @Test public void sparseTaskAckDoesNotEraseOrRefreshMonitoringSample() {
        long sample = System.currentTimeMillis();
        MemoryAgentResponse observation = new MemoryAgentResponse();
        observation.status = "Succeeded";
        observation.sampleTime = sample;
        observation.capabilities = new HashMap<>();
        observation.capabilities.put("supported", true);
        observation.capabilities.put("ksm", true);
        observation.capabilities.put("ksmZeroPages", true);
        observation.capabilities.put("zram", true);
        observation.capabilities.put("writeback", true);
        observation.state = new HashMap<>();
        observation.state.put("managed", true);
        observation.state.put("actual", Collections.<String, Object>singletonMap("zram", "old"));
        observation.state.put("savings", Collections.<String, Object>singletonMap("total", 42L));
        repository.observe("host1", observation);

        MemoryActionRequest update = request();
        repository.submit(update);
        MemoryTaskVO task = claimAll().get(0);
        MemoryAgentResponse ack = new MemoryAgentResponse();
        ack.status = "Succeeded";
        ack.appliedRevision = 7L;
        ack.sampleTime = sample + 1000;
        ack.state = new HashMap<>();
        ack.state.put("phase", "PAUSED");
        ack.state.put("sampleQuality", "error");
        // A one-sided metric in an operation ACK is still not a monitoring
        // envelope; it must not replace the last complete observation.
        ack.state.put("savings", Collections.<String, Object>singletonMap("total", 999L));
        repository.result(task.getUuid(), "Succeeded", null, ack);

        MemoryStateVO state = repository.states(Collections.singletonList("host1"), 0, 1).get(0);
        assertEquals("ACK must update control revision independently", Long.valueOf(7L), state.getAppliedRevision());
        assertEquals("sparse ACK must not refresh the monitoring timestamp", Long.valueOf(sample), state.getLastSampleTime());
        assertTrue("sparse ACK must preserve prior metrics", state.getState().contains("savings"));
        assertTrue("sparse ACK must preserve the prior actual envelope", state.getState().contains("\"old\""));
        assertFalse("one-sided ACK metrics must not be applied", state.getState().contains("999"));
        assertTrue("sparse ACK may update control state", state.getState().contains("PAUSED"));
        assertTrue("sparse ACK must preserve capabilities", state.getCapabilities().contains("zram"));
    }

    @Test public void laterObservationReplacesOmittedMetricsInsteadOfMergingOldSample() {
        long first = System.currentTimeMillis();
        MemoryAgentResponse firstSample = new MemoryAgentResponse();
        firstSample.status = "Succeeded"; firstSample.sampleTime = first;
        firstSample.state = new HashMap<>();
        firstSample.state.put("actual", Collections.<String, Object>singletonMap("zram", "old"));
        firstSample.state.put("savings", Collections.<String, Object>singletonMap("total", 42L));
        firstSample.state.put("managed", true);
        repository.observe("host1", firstSample);

        MemoryAgentResponse secondSample = new MemoryAgentResponse();
        secondSample.status = "Succeeded"; secondSample.sampleTime = first + 1;
        secondSample.state = new HashMap<>();
        secondSample.state.put("actual", Collections.<String, Object>singletonMap("zram", "new"));
        // This is a read-path sample. Missing savings means unavailable now,
        // not permission to carry the previous savings forward.
        repository.observe("host1", secondSample);

        MemoryStateVO state = repository.states(Collections.singletonList("host1"), 0, 1).get(0);
        assertTrue(state.getState().contains("\"new\""));
        assertFalse("old savings must not survive a newer observation", state.getState().contains("42"));
        assertFalse("old actual payload must not survive a newer observation", state.getState().contains("\"old\""));
        assertEquals(Long.valueOf(first + 1), state.getLastSampleTime());
        assertTrue("managed ownership is the only explicitly retained control fact", state.getState().contains("managed"));
    }

    @Test public void drainInterruptedByRebootReturnsThePriorControl() {
        assertReconciledDrainWithoutEffectKeepsControl("DRAIN_INTERRUPTED_BY_REBOOT");
    }

    @Test public void drainNeverIssuedReturnsThePriorControl() {
        assertReconciledDrainWithoutEffectKeepsControl("OPERATION_NEVER_ISSUED");
    }

    private MemoryTaskVO unknownDrainOnAppliedControl() {
        repository.transaction(em -> {
            MemoryTaskVO applied = testTask("applied-control", null, "apply", "Succeeded", null);
            em.persist(applied);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setControlOperationUuid(applied.getUuid()); state.setActiveTaskUuid(null); state.setStatus("Succeeded");
            return null;
        });
        MemoryActionRequest msg = request(); msg.setAction("drain");
        msg.setScope("Host"); msg.setResourceUuid("host1");
        repository.submit(msg);
        MemoryTaskVO drain = claimAll().get(0);
        repository.result(drain.getUuid(), "Unknown", "cannot do the operation on the KVM host", null);
        return drain;
    }

    private void assertReconciledDrainWithoutEffectKeepsControl(String reason) {
        MemoryTaskVO drain = unknownDrainOnAppliedControl();
        MemoryActionRequest reconcile = request();
        reconcile.setScope("Host"); reconcile.setResourceUuid("host1");
        reconcile.setAction("reconcile"); reconcile.setPolicy("{}");
        repository.submit(reconcile);
        MemoryTaskVO query = claimAll().stream()
                .filter(task -> drain.getUuid().equals(task.getReconcileOperationUuid())).findFirst().get();
        MemoryAgentResponse ended = new MemoryAgentResponse(); ended.setSuccess(false);
        ended.status = "Failed"; ended.reason = reason; ended.reasonCode = reason;
        repository.result(query.getUuid(), "Failed", reason, ended);
        assertEquals("Failed", repository.findTask(drain.getUuid()).getStatus());
        assertEquals("a drain that never took effect must return the Host to its prior control: " + reason,
                "applied-control", host1State().getControlOperationUuid());
        assertEquals("Succeeded", host1State().getStatus());
    }

    @Test public void freshObservationQueuesOneReconcileForAnUnknownDrain() {
        MemoryTaskVO drain = unknownDrainOnAppliedControl();

        assertTrue(repository.reconcileUnknownControl("host1"));
        assertFalse("the queued reconcile now owns the Host", repository.reconcileUnknownControl("host1"));

        MemoryTaskVO reconcile = claimAll().stream()
                .filter(task -> "reconcile".equals(task.getAction())).findFirst().get();
        assertEquals(drain.getUuid(), reconcile.getReconcileOperationUuid());
    }

    @Test public void newerConfigurationSupersedesAnApplyOnlyWaitingForItsPool() {
        prepareCapacityObservation("Succeeded", false, false, false);
        final String waiting = "first-zram-waiting-for-pool";
        repository.transaction(em -> {
            MemoryTaskVO task = testTask(waiting, null, "apply", "Blocked", null);
            task.setReason("NATIVE_POOL_UNQUALIFIED"); em.persist(task);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Blocked"); state.setReason("NATIVE_POOL_UNQUALIFIED");
            state.setActiveTaskUuid(waiting); state.setControlOperationUuid(waiting);
            return null;
        });

        standardCommit(Collections.singletonList(standardChange(
                MemoryStandardField.ZRAM_ENABLED, null, null, "false")), null, false);

        MemoryTaskVO superseded = repository.findTask(waiting);
        assertEquals("Failed", superseded.getStatus());
        assertEquals("SUPERSEDED_BY_NEWER_CONFIGURATION", superseded.getReason());
        MemoryTaskVO next = repository.findTask(host1State().getActiveTaskUuid());
        assertNotEquals(waiting, next.getUuid());
        assertEquals("the newer configuration is dispatched, not deferred", "Queued", next.getStatus());
    }

    @Test public void supersededPoolWaitingApplyIsReflectedWhenTheNewerConfigurationDefers() {
        prepareCapacityObservation("Succeeded", false, false, false);
        final String waiting = "first-zram-waiting-for-pool";
        repository.transaction(em -> {
            MemoryTaskVO task = testTask(waiting, null, "apply", "Blocked", null);
            task.setReason("NATIVE_POOL_UNQUALIFIED"); em.persist(task);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Blocked"); state.setReason("NATIVE_POOL_UNQUALIFIED");
            state.setActiveTaskUuid(waiting); state.setControlOperationUuid(waiting);
            em.find(TestHost.class, "host1").status = HostStatus.Disconnected;
            return null;
        });

        standardCommit(Collections.singletonList(standardChange(
                MemoryStandardField.ZRAM_ENABLED, null, null, "false")), null, false);

        assertEquals("Failed", repository.findTask(waiting).getStatus());
        assertNotEquals("the Host no longer shows the superseded wait", "NATIVE_POOL_UNQUALIFIED", host1State().getReason());
    }

    @Test public void verifiedPoolCapacityQueuesOneReconcileForBlockedFirstZramApply() {
        final String control = "control-pool-ready-host1";
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Blocked"); state.setReason("NATIVE_POOL_UNQUALIFIED");
            state.setControlOperationUuid(control); state.setActiveTaskUuid(control); state.setDesiredRevision(1);
            MemoryTaskVO task = taskFixture(control, null, "host1", "apply", "Blocked");
            task.setScope("Host"); task.setResourceUuid("host1"); task.setReason("NATIVE_POOL_UNQUALIFIED");
            em.persist(task);
            return null;
        });

        assertFalse("pool not verified yet", repository.reconcileVerifiedPoolBlock("host1", poolObservation("BLOCKED")));
        assertTrue(repository.reconcileVerifiedPoolBlock("host1", poolObservation("APPLIED")));
        assertFalse("the queued reconcile now owns the Host", repository.reconcileVerifiedPoolBlock("host1", poolObservation("APPLIED")));

        MemoryTaskVO reconcile = claimAll().stream()
                .filter(task -> "reconcile".equals(task.getAction())).findFirst().get();
        assertEquals(control, reconcile.getReconcileOperationUuid());
        assertEquals(1L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t where t.action = 'reconcile'", Long.class)
                .getSingleResult()));
    }

    private static MemoryAgentResponse poolObservation(String applyState) {
        MemoryAgentResponse response = new MemoryAgentResponse();
        response.setSuccess(true); response.status = "Succeeded"; response.hostUuid = "host1";
        response.state = new LinkedHashMap<>();
        response.state.put("actual", Collections.singletonMap("zram",
                Collections.singletonMap("capacity", Collections.singletonMap("apply_state", applyState))));
        return response;
    }

    @Test public void currentSwapBlockReasonFlowsThroughManagerAndStatesWithoutRewritingTaskOutcome() throws Exception {
        final String control = "control-blocked-host1";
        final String planHash = String.join("", Collections.nCopies(64, "a"));
        long priorSample = System.currentTimeMillis() - 1000;
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Blocked"); state.setReason("NATIVE_POOL_UNQUALIFIED");
            state.setControlOperationUuid(control); state.setActiveTaskUuid(control);
            state.setDesiredRevision(9); state.setAppliedRevision(8L);
            state.setPolicyPlanHash(planHash); state.setPolicyPlanStatus("Blocked");
            state.setLastSampleTime(priorSample);
            state.setState("{\"bootId\":\"boot-old\",\"host_zram\":{\"reason\":\"STARTUP_OBSERVATION_IN_PROGRESS\","
                    + "\"observed_at\":\"2026-10-18T12:33:51.2697+08:00\"}}");
            MemoryTaskVO task = taskFixture(control, null, "host1", "apply", "Blocked");
            task.setScope("Host"); task.setResourceUuid("host1"); task.setReason("NATIVE_POOL_UNQUALIFIED");
            task.setDesiredRevision(9); em.persist(task);
            return null;
        });

        final List<MemoryAgentResponse> responses = new ArrayList<>();
        long sampleTime = System.currentTimeMillis();
        for (String status : new String[]{"Blocked", "Failed", "Unknown"}) {
            MemoryAgentResponse observation = new MemoryAgentResponse();
            observation.setSuccess(true); observation.status = "Succeeded";
            observation.hostUuid = "host1"; observation.bootId = "boot-new";
            observation.sampleTime = sampleTime++;
            observation.state = new LinkedHashMap<>(); observation.state.put("bootId", "boot-new");
            Map<String, Object> hostZram = new LinkedHashMap<>();
            hostZram.put("reason", "NO_QUALIFIED_MANAGED_POOL: EXCLUSIVE_ZRAM_ROUTE_REQUIRED");
            hostZram.put("observed_at", "2026-10-18T12:44:57.566+08:00");
            observation.state.put("host_zram", hostZram);
            responses.add(observation);
        }
        MemoryAgentResponse foreign = new MemoryAgentResponse(); foreign.setSuccess(true); foreign.status = "Succeeded";
        foreign.hostUuid = "other-host"; foreign.bootId = "boot-new"; foreign.sampleTime = sampleTime++;
        foreign.state = new LinkedHashMap<>(); foreign.state.put("bootId", "boot-new");
        foreign.state.put("host_zram", Collections.singletonMap("reason", "foreign")); responses.add(foreign);
        MemoryAgentResponse bootConflict = new MemoryAgentResponse(); bootConflict.setSuccess(true); bootConflict.status = "Succeeded";
        bootConflict.hostUuid = "host1"; bootConflict.bootId = "boot-new"; bootConflict.sampleTime = sampleTime++;
        bootConflict.state = new LinkedHashMap<>(); bootConflict.state.put("bootId", "boot-other");
        bootConflict.state.put("host_zram", Collections.singletonMap("reason", "conflicting")); responses.add(bootConflict);
        MemoryAgentResponse stale = new MemoryAgentResponse(); stale.setSuccess(true); stale.status = "Succeeded";
        stale.hostUuid = "host1"; stale.bootId = "boot-new";
        stale.sampleTime = System.currentTimeMillis() - MemoryOptimizationGlobalConfig.displayTtlMillis() - 1;
        stale.state = new LinkedHashMap<>(); stale.state.put("bootId", "boot-new");
        stale.state.put("host_zram", Collections.singletonMap("reason", "stale")); responses.add(stale);

        final int[] responseIndex = {0};
        MemoryOptimizationManager manager = new MemoryOptimizationManager() {
            @Override void call(String host, String action, Map<String, Object> command,
                                java.util.function.Consumer<MemoryAgentResponse> completion) {
                assertEquals("host1", host); assertEquals("state", action);
                completion.accept(responses.get(responseIndex[0]++));
            }
        };
        setField(MemoryOptimizationManager.class, manager, "repository", repository);
        PluginRegistry noLicense = (PluginRegistry) Proxy.newProxyInstance(PluginRegistry.class.getClassLoader(),
                new Class<?>[]{PluginRegistry.class}, (proxy, method, args) -> Collections.emptyList());
        setField(MemoryOptimizationManager.class, manager, "pluginRgty", noLicense);
        MemoryLifecycleHooks hooks = new MemoryLifecycleHooks();
        setField(MemoryLifecycleHooks.class, hooks, "repository", repository);
        setField(MemoryLifecycleHooks.class, hooks, "manager", manager);
        setField(MemoryOptimizationManager.class, manager, "lifecycleHooks", hooks);

        // Valid current observations update the nested Go diagnostic only. A
        // subsequent new boot is accepted; task/control/plan receipts survive.
        // Establish each task state immediately before the observation that
        // exercises it, so DTO setup cannot mutate the repository ahead of
        // the manager callback.
        String[] taskStatuses = {"Blocked", "Failed", "Unknown"};
        for (int i = 0; i < taskStatuses.length; i++) {
            final String taskStatus = taskStatuses[i];
            repository.transaction(em -> {
                MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
                state.setStatus(taskStatus); state.setReason("NATIVE_POOL_UNQUALIFIED");
                state.setPolicyPlanStatus(taskStatus);
                state.setActiveTaskUuid("Blocked".equals(taskStatus) ? control : null);
                MemoryTaskVO task = em.find(MemoryTaskVO.class, control); task.setStatus(taskStatus);
                return null;
            });
            manager.observeHostAfterConnect("host1");
            MemoryStateVO row = repository.states(Collections.singletonList("host1"), 0, 1).get(0);
            MemoryStateInventory projected = MemoryMetricsPresentation.inventory(row, null, System.currentTimeMillis());
            assertEquals(new String[]{"Blocked", "Failed", "Unknown"}[i], projected.getStatus());
            assertEquals("NATIVE_POOL_UNQUALIFIED", projected.getReason());
            assertEquals(control, projected.getControlOperationUuid());
            assertEquals(9L, projected.getDesiredRevision()); assertEquals(Long.valueOf(8L), projected.getAppliedRevision());
            assertEquals(planHash, projected.getPolicyPlanHash());
            assertEquals(new String[]{"Blocked", "Failed", "Unknown"}[i], projected.getPolicyPlanStatus());
            Map<?, ?> state = org.zstack.utils.gson.JSONObjectUtil.toObject(projected.getState(), Map.class);
            Map<?, ?> hostZram = (Map<?, ?>) state.get("host_zram");
            assertEquals("NO_QUALIFIED_MANAGED_POOL: EXCLUSIVE_ZRAM_ROUTE_REQUIRED", hostZram.get("reason"));
            assertEquals("2026-10-18T12:44:57.566+08:00", hostZram.get("observed_at"));
            MemoryTaskVO task = repository.findTask(control);
            assertEquals(new String[]{"Blocked", "Failed", "Unknown"}[i], task.getStatus());
            assertEquals("NATIVE_POOL_UNQUALIFIED", task.getReason());
        }

        MemoryStateVO beforeRejected = repository.states(Collections.singletonList("host1"), 0, 1).get(0);
        Long acceptedSampleTime = beforeRejected.getLastSampleTime();
        String acceptedState = beforeRejected.getState();
        for (int i = 0; i < 3; i++) { manager.observeHostAfterConnect("host1"); }
        MemoryStateVO afterRejected = repository.states(Collections.singletonList("host1"), 0, 1).get(0);
        assertEquals("foreign/stale/conflicting-boot samples must not replace the accepted observation",
                acceptedSampleTime, afterRejected.getLastSampleTime());
        assertEquals(acceptedState, afterRejected.getState());
        assertEquals("Unknown", afterRejected.getStatus());
        assertEquals(control, afterRejected.getControlOperationUuid());
        assertEquals(planHash, afterRejected.getPolicyPlanHash());
    }

    private Map<String, Object> completeState(long saved, String actualValue) {
        Map<String, Object> state = new HashMap<>();
        Map<String, Object> actual = new HashMap<>();
        actual.put("ksm", Collections.<String, Object>singletonMap("enabled", true));
        actual.put("zram", Collections.<String, Object>singletonMap("value", actualValue));
        actual.put("writeback", Collections.emptyMap()); state.put("actual", actual);
        Map<String, Object> savings = new HashMap<>();
        savings.put("formulaVersion", "mechanism-estimate-v1"); savings.put("quality", "Fresh");
        savings.put("sampleTime", System.currentTimeMillis());
        savings.put("ksmOrdinaryBytes", 1L); savings.put("ksmZeroBytes", 2L);
        savings.put("ksmTotalBytes", 3L); savings.put("zramBytes", 4L);
        savings.put("totalSavedEstimateBytes", saved); state.put("savings", savings);
        return state;
    }

    @Test public void deletedHostsAreNotCurrentStateEvenWhenEvidenceIsRetained() {
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Unknown"); state.setActiveTaskUuid("unresolved-operation");
            em.createQuery("delete from HostVO h where h.uuid = 'host1'").executeUpdate();
            return null;
        });
        assertEquals(1L, repository.stateCount(null));
        assertTrue(repository.states(Collections.singletonList("host1"), 0, 10).isEmpty());
        assertEquals("host2", repository.states(null, 0, 10).get(0).getHostUuid());
        assertNotNull("Unknown evidence is not discarded by a read",
                repository.transaction(em -> em.find(MemoryStateVO.class, "host1")));
    }

    @Test public void lateObservationCannotRecreateDeletedHostState() {
        repository.transaction(em -> {
            em.remove(em.find(MemoryStateVO.class, "host1"));
            em.createQuery("delete from HostVO h where h.uuid = 'host1'").executeUpdate();
            return null;
        });
        MemoryAgentResponse response = new MemoryAgentResponse();
        response.status = "Succeeded"; response.sampleTime = System.currentTimeMillis();
        response.state = Collections.singletonMap("managed", false);
        repository.observe("host1", response);
        assertNull("A delayed response must not recreate a deleted resource",
                repository.transaction(em -> em.find(MemoryStateVO.class, "host1")));
    }

    @Test public void deletionCleanupRemovesCurrentDataButKeepsTaskHistory() {
        repository.transaction(em -> {
            persistPolicy(em, "Host", "host1"); persistPolicy(em, "Cluster", "cluster1");
            persistPolicy(em, "VM", "vm1"); persistPolicy(em, "Host", "host2");
            persistExclusion(em, "vm1"); persistMigration(em, "vm1", "Released");
            em.persist(testTask("completed-history", null, "drain", "Succeeded", null));
            em.createQuery("delete from HostVO h where h.uuid = 'host1'").executeUpdate();
            em.createQuery("delete from ClusterVO c where c.uuid = 'cluster1'").executeUpdate();
            em.createQuery("delete from VmInstanceVO v where v.uuid = 'vm1'").executeUpdate();
            return null;
        });
        repository.cleanupDeletedResources(); repository.cleanupDeletedResources();
        repository.transaction(em -> {
            assertNull(em.find(MemoryPolicyVO.class, "Host:host1"));
            assertNull(em.find(MemoryPolicyVO.class, "Cluster:cluster1"));
            assertNull(em.find(MemoryPolicyVO.class, "VM:vm1"));
            assertNull(em.find(MemoryVmExclusionVO.class, "vm1"));
            assertNull(em.find(MemoryMigrationVO.class, "vm1"));
            assertNull(em.find(MemoryStateVO.class, "host1"));
            assertNotNull(em.find(MemoryTaskVO.class, "completed-history"));
            assertNotNull(em.find(MemoryPolicyVO.class, "Global:global"));
            assertNotNull(em.find(MemoryPolicyVO.class, "Host:host2"));
            assertNotNull(em.find(MemoryStateVO.class, "host2"));
            return null;
        });
    }

    @Test public void forcedDeletionKeepsUnresolvedEvidenceAndStickyPolicyForExistingVm() {
        repository.transaction(em -> {
            persistPolicy(em, "Host", "host1"); persistPolicy(em, "VM", "vm1");
            persistExclusion(em, "vm1");
            em.persist(testTask("unknown-history", null, "apply", "Unknown", null));
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Unknown"); state.setActiveTaskUuid("unknown-history");
            em.createQuery("delete from HostVO h where h.uuid = 'host1'").executeUpdate();
            return null;
        });
        repository.cleanupDeletedResources();
        repository.transaction(em -> {
            assertNotNull(em.find(MemoryStateVO.class, "host1"));
            assertNotNull(em.find(MemoryPolicyVO.class, "Host:host1"));
            assertEquals("Unknown", em.find(MemoryTaskVO.class, "unknown-history").getStatus());
            assertNotNull("Host deletion cannot re-enable a VM on another Host", em.find(MemoryVmExclusionVO.class, "vm1"));
            assertNotNull(em.find(MemoryPolicyVO.class, "VM:vm1"));
            return null;
        });
        assertEquals("deny", repository.vmParticipation("vm1"));
        assertEquals(1L, repository.stateCount(null));
    }

    @Test public void finalVmDeletionRetainsUnresolvedMigrationEvidence() {
        repository.transaction(em -> {
            persistPolicy(em, "VM", "vm1"); persistExclusion(em, "vm1");
            persistMigration(em, "vm1", "Unknown");
            em.createQuery("delete from VmInstanceVO v where v.uuid = 'vm1'").executeUpdate();
            return null;
        });
        repository.cleanupDeletedResources();
        repository.transaction(em -> {
            assertNotNull(em.find(MemoryPolicyVO.class, "VM:vm1"));
            assertNotNull(em.find(MemoryVmExclusionVO.class, "vm1"));
            assertEquals("Unknown", em.find(MemoryMigrationVO.class, "vm1").status);
            return null;
        });
    }

    @Test public void deletedHostCannotReceiveNewDispatchAndPendingHistoryIsNotRewritten() {
        repository.submit(request());
        MemoryActionRequest other = request(); other.setResourceUuid("host2");
        repository.submit(other);
        repository.transaction(em -> {
            em.createQuery("delete from HostVO h where h.uuid = 'host1'").executeUpdate(); return null;
        });
        List<MemoryTaskVO> claimed = claimAll();
        assertEquals(1, claimed.size()); assertEquals("host2", claimed.get(0).getHostUuid());
        repository.transaction(em -> {
            assertEquals("Queued", em.createQuery("from MemoryTaskVO t where t.hostUuid = 'host1'", MemoryTaskVO.class)
                    .getSingleResult().getStatus());
            return null;
        });
    }

    private void persistPolicy(EntityManager em, String scope, String resource) {
        MemoryPolicyVO row = new MemoryPolicyVO(); row.setUuid(scope + ":" + resource);
        row.setScope(scope); row.setResourceUuid(resource); row.setPolicy("{\"schemaVersion\":1}");
        em.persist(row);
    }

    private void persistExclusion(EntityManager em, String vm) {
        MemoryVmExclusionVO row = new MemoryVmExclusionVO(); row.vmUuid = vm;
        row.sourceHostUuid = "host1"; row.targetHostUuid = "host2";
        row.retained = true; em.persist(row);
    }

    private void persistMigration(EntityManager em, String vm, String status) {
        MemoryMigrationVO row = new MemoryMigrationVO(); row.vmUuid = vm; row.operationUuid = "migration-op";
        row.sourceHostUuid = "host1"; row.targetHostUuid = "host2"; row.status = status; em.persist(row);
    }

    private static Object readStatic(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); return field.get(null);
    }

    private static void writeStatic(Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(null, value);
    }

    private static void setField(Class<?> type, Object target, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }

    private void populateQueryScaleFixture() {
        repository.transaction(em -> {
            for (int i = 0; i < 1200; i++) {
                String id = String.format("scale%05d", i);
                TestHost host = new TestHost(); host.uuid = id; host.hypervisorType = "KVM";
                host.status = HostStatus.Connected; em.persist(host);
                MemoryStateVO state = new MemoryStateVO(); state.setHostUuid(id); state.setStatus("Succeeded"); em.persist(state);
                em.persist(testTask(id, null, "apply", "Succeeded", null));
            }
            return null;
        });
    }

    private long taskCount() {
        return repository.transaction(em -> em.createQuery("select count(t) from MemoryTaskVO t", Long.class).getSingleResult());
    }

    private String standardDefault(MemoryStandardField field) {
        if ("ksm.enabled".equals(field.path())) { return "none"; }
        // Isolated KSM tests leave service switches unmanaged; real deployment defaults
        // are exercised through the standard initializer in the framework suite.
        if ("zram.enabled".equals(field.path()) || "writeback.enabled".equals(field.path())) { return "none"; }
        return "";
    }

    private MemoryActionRequest request() {
        MemoryActionRequest msg = new MemoryActionRequest();
        SessionInventory session = new SessionInventory(); session.setAccountUuid("admin"); session.setUserUuid("actor");
        msg.setSession(session); msg.setScope("Host"); msg.setResourceUuid("host1");
        msg.setAction("pause"); msg.setPolicy("{}");
        return msg;
    }

    /** Claims every Host's queued operation, as each owning management node's scan does. */
    private List<MemoryTaskVO> claimAll() {
        List<MemoryTaskVO> claimed = new ArrayList<>();
        for (String host : repository.hostsWithPendingWork()) {
            MemoryTaskVO task = repository.claim(host);
            if (task != null && "Applying".equals(task.getStatus())) { claimed.add(task); }
        }
        return claimed;
    }

    private String effectivePolicy(String scope, String resource) {
        return repository.transaction(em -> repository.effective(em, scope, resource));
    }

    private String ownPolicy(String scope, String resource) {
        return repository.transaction(em -> repository.ownPolicy(em, scope, resource));
    }

    private MemoryTaskVO taskFixture(String uuid, String parentUuid, String hostUuid, String action, String status) {
        MemoryTaskVO task = new MemoryTaskVO();
        task.setUuid(uuid); task.setParentUuid(parentUuid); task.setHostUuid(hostUuid);
        task.setScope("Global"); task.setResourceUuid("global"); task.setActorUuid("admin:actor");
        task.setAction(action); task.setStatus(status); task.setPolicy("{}"); task.setDesiredRevision(1);
        return task;
    }

    @Test public void configurationRejectsDisconnectedHostBeforePersistingDesiredOrTask() {
        repository.transaction(em -> { em.find(TestHost.class, "host1").status = HostStatus.Connecting; return null; });
        try {
            repository.submit(request());
            fail("configuration must not be admitted while a target host is not Connected");
        } catch (MemoryOperationException expected) {
            assertEquals("MEMORY_HOST_NOT_CONNECTED", expected.getCode());
        }
        assertEquals(0L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t", Long.class).getSingleResult()));
    }

    private Object exclusionCall(String name, Class<?>[] types, Object... values) throws Exception {
        java.lang.reflect.Method method;
        try { method = MemoryRepository.class.getDeclaredMethod(name, types); }
        catch (NoSuchMethodException e) { fail("Migration participation must be durably captured"); return null; }
        method.setAccessible(true); return method.invoke(repository, values);
    }

    @Test public void failedMigrationDoesNotCreatePermanentExclusion() throws Exception {
        exclusionCall("captureMigrationParticipation", new Class[]{String.class, String.class, String.class}, "vm1", "host1", "host2");
        exclusionCall("abortMigrationParticipation", new Class[]{String.class}, "vm1");
        assertEquals("inherit", exclusionCall("vmParticipation", new Class[]{String.class}, "vm1"));
    }

    @Test public void claimProjectsExactActiveTaskIntoState() {
        registerStandardResources();
        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.KSM_ENABLED,
                null, null, "true")), null, false);
        List<MemoryTaskVO> claimed = claimAll();
        assertEquals(2, claimed.size());
        for (MemoryTaskVO task : claimed) {
            MemoryStateVO state = repository.states(Collections.singletonList(task.getHostUuid()), 0, 1).get(0);
            assertEquals("claim must advance the matching State row", "Applying", state.getStatus());
            assertEquals(task.getUuid(), state.getActiveTaskUuid());
            assertEquals("Applying", state.getPolicyPlanStatus());
        }
    }

    @Test public void claimDoesNotOverwriteNewBlockedPlanWithOldQueuedTask() {
        registerStandardResources();
        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.KSM_ENABLED,
                "host1", "HostVO", "true")), null, false);
        String oldTask = host1State().getActiveTaskUuid();
        assertNotNull(oldTask);
        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.WRITEBACK_ENABLED,
                "host1", "HostVO", "true")), null, false);
        String newPlan = host1State().getPolicyPlanHash();
        String blockers = host1State().getPolicyBlockedFields();
        assertEquals("Blocked", host1State().getPolicyPlanStatus());
        assertTrue(claimAll().isEmpty());
        assertEquals("Failed", repository.findTask(oldTask).getStatus());
        assertNull(host1State().getActiveTaskUuid());
        assertEquals("Blocked", host1State().getPolicyPlanStatus());
        assertEquals(newPlan, host1State().getPolicyPlanHash());
        assertEquals(blockers, host1State().getPolicyBlockedFields());
    }

    @Test public void claimingDrainPreservesNeedsReviewUntilAgentReportsDraining() {
        repository.transaction(em -> {
            TestHost drainHost = new TestHost(); drainHost.uuid = "host4";
            drainHost.hypervisorType = "KVM"; drainHost.clusterUuid = "cluster1";
            drainHost.zoneUuid = "zone4"; drainHost.status = HostStatus.Connected; em.persist(drainHost);
            MemoryStateVO draining = new MemoryStateVO(); draining.setHostUuid("host4");
            draining.setStatus("Queued"); draining.setPolicyPlanStatus("NeedsReview");
            MemoryTaskVO drainParent = testTask("drain-parent", null, "drain", "Succeeded", null);
            em.persist(drainParent);
            MemoryTaskVO drain = testTask("drain-visible", drainParent.getUuid(), "drain", "Queued", null);
            drain.setHostUuid("host4"); em.persist(drain); draining.setActiveTaskUuid(drain.getUuid());
            em.persist(draining);
            return null;
        });
        MemoryTaskVO drain = claimAll().stream()
                .filter(task -> "drain-visible".equals(task.getUuid())).findFirst().orElse(null);
        assertNotNull(drain);
        assertEquals("drain", drain.getAction());
        assertEquals("drain-visible", repository.states(Collections.singletonList("host4"), 0, 1)
                .get(0).getActiveTaskUuid());
        MemoryStateVO state = repository.states(Collections.singletonList("host4"), 0, 1).get(0);
        assertEquals("Applying", state.getStatus());
        assertEquals("NeedsReview", state.getPolicyPlanStatus());
        repository.result(drain.getUuid(), "Draining", "executor running", null);
        assertEquals("Draining", repository.states(Collections.singletonList("host4"), 0, 1)
                .get(0).getStatus());
    }

    @Test public void configuredPaginationReachesTheRepository() throws Exception {
        Field field = org.zstack.core.config.GlobalConfig.class.getDeclaredField("value"); field.setAccessible(true);
        org.zstack.core.config.GlobalConfig maximum = MemoryOptimizationGlobalConfig.QUERY_MAX_PAGE_SIZE;
        String oldMaximum = maximum.value();
        try {
            field.set(maximum, "800");
            repository.transaction(em -> {
                for (int i = 0; i < 600; i++) {
                    MemoryStateVO state = new MemoryStateVO(); state.setHostUuid(String.format("extra%04d", i));
                    TestHost host = new TestHost(); host.uuid = state.getHostUuid();
                    host.hypervisorType = "KVM"; host.clusterUuid = "cluster1";
                    host.status = HostStatus.Connected; em.persist(host);
                    state.setStatus("Unknown"); em.persist(state);
                }
                return null;
            });
            assertEquals(600, repository.states(null, 0, 600).size());
        } finally { field.set(maximum, oldMaximum); }
    }

    @Test public void concurrentClaimersNeverDispatchSameTask() throws Exception {
        repository.submit(request());
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CountDownLatch go = new java.util.concurrent.CountDownLatch(1);
        try {
            List<java.util.concurrent.Future<MemoryTaskVO>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                futures.add(executor.submit(() -> { go.await(); return repository.claim("host1"); }));
            }
            go.countDown(); int claimed = 0;
            for (java.util.concurrent.Future<MemoryTaskVO> future : futures) {
                if (future.get(15, java.util.concurrent.TimeUnit.SECONDS) != null) { claimed++; }
            }
            assertEquals(1, claimed);
        } finally { executor.shutdownNow(); }
    }

    @Test public void onlyAFailedResultFiresTheHostFailureEvent() {
        repository.submit(request());
        MemoryTaskVO task = claimAll().get(0);
        repository.result(task.getUuid(), "Unknown", "request timed out", null);
        assertTrue("an Unknown result is not a failure", firedEvents.isEmpty());
        repository.result(task.getUuid(), "Failed", "agent rejected the pause", null);
        assertEquals(1, firedEvents.size());
        assertEquals(MemoryTaskFailureEvent.PATH, firedEvents.get(0)[0]);
        MemoryTaskFailureEvent event = (MemoryTaskFailureEvent) firedEvents.get(0)[1];
        assertEquals(task.getUuid(), event.getTaskUuid()); assertEquals("host1", event.getHostUuid());
        assertEquals("pause", event.getAction()); assertEquals("agent rejected the pause", event.getReason());
    }

    @Test public void historyPurgeKeepsEveryOperationAHostStillReaches() {
        repository.transaction(em -> {
            em.createQuery("delete from MemoryTaskVO").executeUpdate();
            em.persist(testTask("root", null, "prepareWritebackBackend", "Succeeded", null));
            em.persist(testTask("drain", "root", "drain", "Succeeded", null));
            em.persist(testTask("sibling", "root", "prepareWritebackBackend", "Succeeded", null));
            MemoryTaskVO control = testTask("control", null, "apply", "Succeeded", null);
            control.setExpectedControlOperationUuid("drain"); em.persist(control);
            em.persist(testTask("observed", null, "apply", "Succeeded", null));
            em.persist(testTask("unknown", null, "apply", "Unknown", null));
            em.persist(testTask("expired", null, "pause", "Failed", null));
            em.persist(testTask("recent", null, "pause", "Failed", null));
            for (String host : Arrays.asList("host1", "host2")) {
                MemoryStateVO state = em.find(MemoryStateVO.class, host);
                state.setActiveTaskUuid(null); state.setControlOperationUuid(null); state.setState(null);
            }
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setControlOperationUuid("control");
            state.setState("{\"maintenanceProof\":{\"drainOperationUuid\":\"observed\"}}");
            return null;
        });
        repository.transaction(em -> em.createQuery("update MemoryTaskVO t set t.lastOpDate = :old where t.uuid <> 'recent'")
                .setParameter("old", new java.sql.Timestamp(System.currentTimeMillis() - java.util.concurrent.TimeUnit.DAYS.toMillis(8)))
                .executeUpdate());
        assertEquals(1, repository.purgeHistory(System.currentTimeMillis()));
        assertNull(repository.findTask("expired"));
        for (String kept : Arrays.asList("root", "drain", "sibling", "control", "observed", "unknown", "recent")) {
            assertNotNull(kept, repository.findTask(kept));
        }
        repository.transaction(em -> { em.find(MemoryStateVO.class, "host2").setState("{not json"); return null; });
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setControlOperationUuid(null); state.setState(null);
            return null;
        });
        assertEquals("unreadable Host evidence keeps all history", 0, repository.purgeHistory(System.currentTimeMillis()));
        assertNotNull(repository.findTask("control"));
    }

    @Test public void lostManagementNodeBecomesUnknownWithoutReplay() {
        repository.submit(request()); claimAll();
        assertEquals(Collections.singletonList("host1"), repository.hostsWithUnconfirmedWork());
        repository.expireUnconfirmed(repository.hostsWithUnconfirmedWork());
        assertTrue(claimAll().isEmpty());
        assertEquals("Unknown", host1State().getStatus());
    }

    @Test public void completedDrainWithExactMaintenanceProofAllowsNormalPolicyApplyWithoutResume() {
        repository.transaction(em -> {
            em.persist(testTask("completed-drain", null, "drain", "Succeeded", null));
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Succeeded");
            state.setControlOperationUuid("completed-drain");
            state.setLastSampleTime(System.currentTimeMillis());
            state.setState("{\"bootId\":\"boot-a\",\"lastConfirmedOperationUuid\":\"completed-drain\","
                    + "\"lifecycle\":{\"paused\":true,\"activeState\":\"paused\"},"
                    + "\"maintenanceProof\":{\"maintenanceReady\":true,\"maintenanceArchived\":true,"
                    + "\"drainOperationUuid\":\"completed-drain\",\"oldPoolGeneration\":\"old-pool\","
                    + "\"activeOperations\":0,\"poolOwnedByService\":false,"
                    + "\"readyForInitialization\":true,\"originalDeviceInactive\":true,"
                    + "\"oldPoolOwnershipAbsent\":true,\"executorExited\":true}}");
            return null;
        });

        MemoryActionRequest apply = request();
        apply.setScope("Host"); apply.setResourceUuid("host1");
        String submitted = repository.submit(apply);
        assertEquals("Queued", repository.findTask(submitted).getStatus());
        MemoryTaskVO child = claimAll().stream()
                .filter(task -> "host1".equals(task.getHostUuid())).findFirst().get();
        MemoryStateVO state = repository.states(Collections.singletonList("host1"), 0, 1).get(0);
        assertEquals(child.getUuid(), state.getActiveTaskUuid());
        assertEquals(child.getUuid(), state.getControlOperationUuid());
        assertNotEquals("completed-drain", state.getControlOperationUuid());
    }

    private void rejectedMaintenanceActivationFixture() {
        repository.transaction(em -> {
            MemoryTaskVO drain = testTask("completed-drain", null, "drain", "Succeeded", null);
            em.persist(drain);
            MemoryTaskVO rejected = testTask("rejected-activation", null, "apply", "Failed", null);
            rejected.setReason("MAINTENANCE_ACTIVATION_WRITEBACK_MUST_REMAIN_DISABLED");
            em.persist(rejected); em.flush();
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Succeeded"); state.setControlOperationUuid(rejected.getUuid());
            state.setLastSampleTime(System.currentTimeMillis());
            state.setState("{\"managed\":true,\"bootId\":\"boot-a\","
                    + "\"lastConfirmedOperationUuid\":\"completed-drain\","
                    + "\"lifecycle\":{\"paused\":true,\"activeState\":\"paused\"},"
                    + "\"maintenanceProof\":{\"maintenanceReady\":true,\"maintenanceArchived\":true,"
                    + "\"drainOperationUuid\":\"completed-drain\",\"oldPoolGeneration\":\"old-pool\","
                    + "\"activeOperations\":0,\"poolOwnedByService\":false,"
                    + "\"readyForInitialization\":true,\"originalDeviceInactive\":true,"
                    + "\"oldPoolOwnershipAbsent\":true,\"executorExited\":true}}");
            return null;
        });
    }

    @Test public void notReadyMaintenanceActivationRetriesOnlyAfterSafeSampleAndDelay() {
        registerStandardResources();
        String policy = "{\"schemaVersion\":1,\"zram\":{\"enabled\":true}}";
        repository.transaction(em -> {
            writeOverride(em, "host1", "HostVO", "zram.enabled", true);
            em.persist(testTask("completed-drain", null, "drain", "Succeeded", null));
            MemoryTaskVO rejected = testTask("not-ready-activation", null, "apply", "Failed", null);
            rejected.setReason("MAINTENANCE_ACTIVATION_NATIVE_NOT_READY");
            em.persist(rejected); em.flush();
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Failed"); state.setActiveTaskUuid(null);
            state.setControlOperationUuid(rejected.getUuid());
            state.setAppliedPolicy(policy); state.setAppliedPolicyHash(sha256(policy));
            state.setDesiredRevision(3); state.setAppliedRevision(3L);
            state.setCapabilities("{\"supported\":true,\"ksm\":true,\"zram\":true,\"writeback\":true}");
            state.setLastSampleTime(System.currentTimeMillis());
            return null;
        });
        java.util.function.Consumer<String> observe = host -> repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setLastSampleTime(System.currentTimeMillis());
            state.setState("{\"managed\":true,\"bootId\":\"boot-a\",\"lastConfirmedOperationUuid\":\"completed-drain\","
                    + "\"host\":" + host + ",\"lifecycle\":{\"paused\":true,\"activeState\":\"paused\"},"
                    + "\"maintenanceProof\":{\"maintenanceReady\":true,\"maintenanceArchived\":true,"
                    + "\"drainOperationUuid\":\"completed-drain\",\"oldPoolGeneration\":\"old-pool\","
                    + "\"activeOperations\":0,\"poolOwnedByService\":false,"
                    + "\"readyForInitialization\":true,\"originalDeviceInactive\":true,"
                    + "\"oldPoolOwnershipAbsent\":true,\"executorExited\":true}}");
            return null;
        });
        String safe = "{\"qualified\":true,\"safe\":true}";

        observe.accept(safe);
        assertFalse("a just rejected activation waits before retrying", repository.applyEffectivePolicyAfterConnect("host1"));
        repository.transaction(em -> em.createQuery("update MemoryTaskVO t set t.lastOpDate = :old where t.uuid = 'not-ready-activation'")
                .setParameter("old", new java.sql.Timestamp(System.currentTimeMillis() - 120_000L)).executeUpdate());
        observe.accept("{\"qualified\":false,\"safe\":false}");
        assertFalse("an unsafe Host sample is not ready for activation", repository.applyEffectivePolicyAfterConnect("host1"));

        observe.accept(safe);
        assertTrue(repository.applyEffectivePolicyAfterConnect("host1"));
        MemoryTaskVO retry = claimAll().stream()
                .filter(t -> "host1".equals(t.getHostUuid())).findFirst().get();
        assertEquals("apply", retry.getAction());
        assertNotEquals("not-ready-activation", retry.getUuid());
    }

    @Test public void rejectedMaintenanceActivationAllowsCorrectedStandardConfiguration() {
        registerStandardResources(); rejectedMaintenanceActivationFixture();
        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.ZRAM_ENABLED,
                "host1", "HostVO", "true")), null, false);
        MemoryStateVO state = host1State();
        assertNotNull(state.getActiveTaskUuid());
        assertNotEquals("rejected-activation", state.getControlOperationUuid());
        assertEquals("Failed", repository.findTask("rejected-activation").getStatus());
        assertEquals("Succeeded", repository.findTask("completed-drain").getStatus());
        assertEquals(1, claimAll().size());
        assertTrue("second MN cannot dispatch the same operation", claimAll().isEmpty());
    }

    @Test public void drainedHostWithZramDisabledSavesWritebackDisableWithoutInvalidActivation() {
        registerStandardResources(); rejectedMaintenanceActivationFixture();
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setControlOperationUuid("completed-drain");
            state.setAppliedPolicy("{\"schemaVersion\":1,\"zram\":{\"enabled\":false},\"writeback\":{\"enabled\":true}}");
            state.setAppliedPolicyHash("previous-policy-before-maintenance");
            return null;
        });
        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.WRITEBACK_ENABLED,
                "host1", "HostVO", "false")), null, false);
        assertEquals(Boolean.FALSE, repository.transaction(em -> standardConfigAdapter.readOverrideOwn(
                em, "writeback.enabled", "host1")));
        assertNull(host1State().getActiveTaskUuid());
        assertEquals("completed-drain", host1State().getControlOperationUuid());
        assertEquals("Blocked", host1State().getPolicyPlanStatus());
        assertTrue(host1State().getPolicyBlockedFields().contains("HOST_DRAIN_MAINTENANCE_REQUIRED"));
        assertFalse(repository.applyEffectivePolicyAfterConnect("host1"));
        assertTrue(claimAll().isEmpty());
    }

    @Test public void pauseAndDrainWithoutExplicitFenceUseTheCurrentHostControl() {
        for (String action : Arrays.asList("pause", "drain")) {
            repository.transaction(em -> {
                em.createQuery("delete from MemoryTaskVO").executeUpdate();
                MemoryTaskVO applied = testTask("applied-control", null, "apply", "Succeeded", null);
                em.persist(applied);
                MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
                state.setControlOperationUuid(applied.getUuid()); state.setActiveTaskUuid(null); state.setStatus("Succeeded");
                MemoryStateVO other = em.find(MemoryStateVO.class, "host2");
                other.setControlOperationUuid(null); other.setActiveTaskUuid(null); other.setStatus("Succeeded");
                return null;
            });
            for (String host : Arrays.asList("host1", "host2")) {
                MemoryActionRequest msg = request(); msg.setAction(action); msg.setResourceUuid(host);
                repository.submit(msg);
            }
            Map<String, String> fences = new HashMap<>();
            for (MemoryTaskVO task : claimAll()) {
                fences.put(task.getHostUuid(), task.getExpectedControlOperationUuid());
            }
            assertEquals(action, "applied-control", fences.get("host1"));
            assertTrue(action, fences.containsKey("host2"));
            assertNull(action, fences.get("host2"));
        }
    }

    @Test public void agentFencedDrainKeepsTheHostControlItWasFencedOn() {
        for (String reason : Arrays.asList("KSM_CONTROL_OPERATION_FENCED", "KSM_CONTROL_OPERATION_UNKNOWN",
                "CONTROL_OPERATION_FENCED")) {
            assertDrainRejectedBeforeEffectsKeepsControl(reason);
        }
    }

    @Test public void secondPauseRejectedBeforeEffectsStillAllowsResume() {
        repository.transaction(em -> {
            MemoryTaskVO paused = testTask("successful-pause", null, "pause", "Succeeded", null);
            em.persist(paused);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setControlOperationUuid(paused.getUuid()); state.setActiveTaskUuid(null); state.setStatus("Succeeded");
            return null;
        });
        MemoryActionRequest msg = request(); msg.setAction("pause");
        msg.setScope("Host"); msg.setResourceUuid("host1");
        repository.submit(msg);
        MemoryTaskVO second = claimAll().get(0);
        MemoryAgentResponse rejected = new MemoryAgentResponse(); rejected.setSuccess(false);
        rejected.status = "Failed"; rejected.reason = "KSM_CONTROL_OPERATION_UNKNOWN"; rejected.reasonCode = "INVALID_CONTROL_ACTION";
        repository.result(second.getUuid(), "Failed", rejected.reason, rejected);
        assertEquals("successful-pause", host1State().getControlOperationUuid());
        // canResumeControl and submit require a Succeeded Host besides a fresh confirmation.
        assertEquals("the Host is still under the successful pause", "Succeeded", host1State().getStatus());
    }

    private void assertDrainRejectedBeforeEffectsKeepsControl(String reason) {
        repository.transaction(em -> {
            MemoryTaskVO applied = testTask("applied-control", null, "apply", "Succeeded", null);
            em.persist(applied);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setControlOperationUuid(applied.getUuid()); state.setActiveTaskUuid(null); state.setStatus("Succeeded");
            return null;
        });
        MemoryActionRequest msg = request(); msg.setAction("drain");
        msg.setScope("Host"); msg.setResourceUuid("host1");
        repository.submit(msg);
        MemoryTaskVO drain = claimAll().get(0);
        MemoryAgentResponse fenced = new MemoryAgentResponse(); fenced.setSuccess(false);
        fenced.status = "Failed"; fenced.reason = reason; fenced.reasonCode = "INVALID_CONTROL_ACTION";
        repository.result(drain.getUuid(), "Failed", reason, fenced);
        assertEquals("Failed", repository.findTask(drain.getUuid()).getStatus());
        assertEquals("a drain the Agent rejected before any effect must not replace its control: " + reason,
                "applied-control", host1State().getControlOperationUuid());
        repository.transaction(em -> {
            em.remove(em.find(MemoryTaskVO.class, "applied-control"));
            return null;
        });
    }

    @Test public void successfulResumeFenceWinsOverOneStalePausedSample() {
        MemoryActionRequest pause = request(); pause.setAction("pause"); pause.setPolicy("{}");
        repository.submit(pause);
        MemoryTaskVO pauseTask = claimAll().get(0);
        repository.result(pauseTask.getUuid(), "Succeeded", null,
                new MemoryAgentResponse() {{ status = "Succeeded"; sampleTime = System.currentTimeMillis(); state = Collections.singletonMap("phase", "PAUSED"); }});
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            MemoryTaskVO resume = em.find(MemoryTaskVO.class, state.getControlOperationUuid());
            resume.setAction("resume"); resume.setStatus("Succeeded");
            state.setActiveTaskUuid(null); state.setStatus("Succeeded");
            // This fixture is a real observation predating the resume, not a
            // sparse pause ACK (which intentionally has no monitoring time).
            state.setLastSampleTime(System.currentTimeMillis() - 1000);
            state.setState("{\"lifecycle\":{\"paused\":true,\"activeState\":\"paused\"}}");
            return null;
        });
        MemoryActionRequest apply = request();
        apply.setScope("Host"); apply.setResourceUuid("host1");
        assertEquals("Queued", repository.findTask(repository.submit(apply)).getStatus());
    }

    @Test public void reconcileResultClosesOriginalAndHistoricalAliasesOnlyForSameRoot() {
        repository.transaction(em -> {
            for (MemoryTaskVO task : Arrays.asList(
                    testTask("root-parent", null, "apply", "Unknown", null),
                    testTask("original-child", "root-parent", "apply", "Unknown", null),
                    testTask("old-parent", null, "reconcile", "Unknown", null),
                    testTask("old-reconcile", "old-parent", "reconcile", "Unknown", "original-child"),
                    testTask("current-parent", null, "reconcile", "Applying", null),
                    testTask("current-reconcile", "current-parent", "reconcile", "Applying", "original-child"),
                    testTask("other-parent", null, "reconcile", "Unknown", null),
                    testTask("other-reconcile", "other-parent", "reconcile", "Unknown", "different-root"))) {
                em.persist(task);
            }
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            if (state == null) { state = new MemoryStateVO(); state.setHostUuid("host1"); state.setStatus("Applying"); em.persist(state); }
            state.setStatus("Applying"); state.setActiveTaskUuid("current-reconcile");
            return null;
        });
        MemoryAgentResponse failed = new MemoryAgentResponse(); failed.setSuccess(false);
        failed.reasonCode = "HOST_ADMISSION_NOT_SENT";
        repository.result("current-reconcile", "Failed", "HOST_ADMISSION_NOT_SENT", failed);
        repository.transaction(em -> {
            assertEquals("Failed", em.find(MemoryTaskVO.class, "original-child").getStatus());
            assertEquals("Failed", em.find(MemoryTaskVO.class, "old-reconcile").getStatus());
            assertEquals("Failed", em.find(MemoryTaskVO.class, "current-reconcile").getStatus());
            assertEquals("Failed", em.find(MemoryTaskVO.class, "root-parent").getStatus());
            assertEquals("Failed", em.find(MemoryTaskVO.class, "old-parent").getStatus());
            assertEquals("Failed", em.find(MemoryTaskVO.class, "current-parent").getStatus());
            assertEquals("Unknown", em.find(MemoryTaskVO.class, "other-reconcile").getStatus());
            assertNull(em.find(MemoryStateVO.class, "host1").getActiveTaskUuid());
            return null;
        });
    }

    @Test public void ordinaryReconcileCommitsOriginalPolicyAndControlInsteadOfQueryIdentity() {
        for (String action : Arrays.asList("apply", "clearOverride")) {
            MemoryAgentResponse ack = ordinaryReconcileFixture(action, false);
            repository.result("query", "Succeeded", null, ack);
            MemoryStateVO state = host1State();
            assertEquals(action, "original", state.getControlOperationUuid());
            assertNull(state.getActiveTaskUuid());
            assertEquals(Long.valueOf(8), state.getAppliedRevision());
            assertEquals("Succeeded", repository.findTask("original").getStatus());
            MemoryPolicyConfig applied = MemoryPolicyRules.decode(state.getAppliedPolicy());
            assertEquals(Boolean.TRUE, applied.zram.enabled);
            if ("apply".equals(action)) { assertEquals(Boolean.TRUE, applied.ksm.enabled); }
            else { assertNull("clear must not resurrect removed KSM intent", applied.ksm); }
            assertEquals("Applied", state.getPolicyPlanStatus());
            assertEquals("{}", state.getPolicyBlockedFields());
            assertEquals(state.getState(), "Active", state.toInventory().getFeatureState());
            assertTrue(state.toInventory().getAllowedActions().contains("drain"));
            assertFalse("already applied policy must not be replayed", repository.applyEffectivePolicyAfterConnect("host1"));
            String policy = state.getAppliedPolicy();
            repository.result("query", "Failed", "late duplicate", null);
            assertEquals(policy, host1State().getAppliedPolicy());
            assertEquals("original", host1State().getControlOperationUuid());
        }
    }

    @Test public void configurationReconcileNotIssuedSettlesOnlyTheCurrentPlanOutcome() {
        for (String receipt : Arrays.asList("FIRST_ZRAM_ADMISSION_NOT_ISSUED", "KSM_CLEAR_NOT_ISSUED")) {
            for (String action : Arrays.asList("apply", "clearOverride")) {
                for (String queryIdentity : Arrays.asList("explicitOmitted", "explicitExpected", "configuration")) {
                  for (String agentControl : Arrays.asList("omitted", "", "0123456789abcdef0123456789abcdef")) {
                    ordinaryReconcileFixture(action, false);
                    repository.transaction(em -> {
                        em.persist(testTask("old-query", null, "reconcile", "Unknown", "original"));
                        return null;
                    });
                    if ("explicitExpected".equals(queryIdentity)) {
                        repository.transaction(em -> {
                            em.find(MemoryTaskVO.class, "query").setExpectedControlOperationUuid("original");
                            return null;
                        });
                    } else if ("configuration".equals(queryIdentity)) {
                        repository.transaction(em -> {
                            MemoryTaskVO query = em.find(MemoryTaskVO.class, "query");
                            query.setActorUuid("system:memory-config-reconcile");
                            query.setExpectedControlOperationUuid("original");
                            em.find(MemoryStateVO.class, "host1").setControlOperationUuid("original");
                            return null;
                        });
                    }
                    if ("KSM_CLEAR_NOT_ISSUED".equals(receipt)) {
                        repository.transaction(em -> {
                            em.find(MemoryTaskVO.class, "original").setPolicy("{\"schemaVersion\":1,\"ksm\":{}}");
                            return null;
                        });
                    }
                    MemoryAgentResponse rejected = new MemoryAgentResponse();
                    rejected.setSuccess(false); rejected.status = "Failed";
                    rejected.operationUuid = "original";
                    rejected.reasonCode = receipt;
                    rejected.state = new LinkedHashMap<>();
                    rejected.state.put("phase", "NOT_ISSUED"); rejected.state.put("bootId", "boot1");
                    rejected.state.put("requestHash", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
                    rejected.state.put("policyWritesIssued", false);
                    if ("KSM_CLEAR_NOT_ISSUED".equals(receipt)) {
                        rejected = org.zstack.utils.gson.JSONObjectUtil.toObject("{\"success\":false,\"status\":\"Failed\","
                                + "\"operationUuid\":\"original\",\"reasonCode\":\"KSM_CLEAR_NOT_ISSUED\","
                                + "\"state\":{\"phase\":\"NOT_ISSUED\",\"bootId\":\"boot1\","
                                + "\"requestHash\":\"988f535ac21c705d00553452e03e90ee28c5a80679e91d5d9f5f7052e9c7a1ac\","
                                + "\"policyWritesIssued\":false}}", MemoryAgentResponse.class);
                    }
                    if (!"omitted".equals(agentControl)) rejected.state.put("controlOperationUuid", agentControl);
                    String control = host1State().getControlOperationUuid();
                    String appliedPolicy = host1State().getAppliedPolicy();
                    String appliedPolicyHash = host1State().getAppliedPolicyHash();
                    String nativeState = host1State().getState();
                    Long appliedRevision = host1State().getAppliedRevision();
                    String planHash = host1State().getPolicyPlanHash();

                    repository.result("query", "Failed", rejected.reasonCode, rejected);

                    MemoryStateVO state = host1State();
                    assertEquals(queryIdentity, "Failed", repository.findTask("original").getStatus());
                    assertEquals(queryIdentity, "Failed", repository.findTask("query").getStatus());
                    assertEquals(queryIdentity, "Failed", repository.findTask("old-query").getStatus());
                    assertEquals(queryIdentity, "Failed", state.getPolicyPlanStatus());
                    assertEquals(queryIdentity, "Failed", MemoryMetricsPresentation.inventory(
                            state, null, System.currentTimeMillis()).getStatus());
                    assertEquals(queryIdentity, appliedPolicy, state.getAppliedPolicy());
                    assertEquals(queryIdentity, appliedPolicyHash, state.getAppliedPolicyHash());
                    assertEquals(queryIdentity, nativeState, state.getState());
                    assertEquals(queryIdentity, appliedRevision, state.getAppliedRevision());
                    assertEquals(queryIdentity, planHash, state.getPolicyPlanHash());
                    assertEquals("nothing was issued; the fence follows the control the Agent reports",
                            "omitted".equals(agentControl) ? control : agentControl.isEmpty() ? null : agentControl,
                            state.getControlOperationUuid());
                    assertNull(queryIdentity, state.getActiveTaskUuid());
                  }
                }
            }
        }
    }


    @Test public void configurationReconcileNotIssuedCannotSettleChangedOrUnprovenPlan() {
        for (String receipt : Arrays.asList("FIRST_ZRAM_ADMISSION_NOT_ISSUED", "KSM_CLEAR_NOT_ISSUED")) {
            for (String invalid : Arrays.asList("newPlan", "newRevision", "unknownProof", "wrongReason",
                    "wrongExpectedControl", "wrongHost", "wrongTopBoot", "nonConfig", "badHash", "partialWrite",
                    "mixedTarget", "nonemptyTarget", "badSchema", "malformedPolicy",
                    "newOwner", "missingState", "wrongProofBoot", "missingHash")) {
                if (!"KSM_CLEAR_NOT_ISSUED".equals(receipt)
                        && Arrays.asList("mixedTarget", "nonemptyTarget", "badSchema", "malformedPolicy",
                            "newOwner", "missingState", "wrongProofBoot", "missingHash").contains(invalid)) continue;
                ordinaryReconcileFixture("apply", false);
                repository.transaction(em -> {
                    em.persist(testTask("old-query", null, "reconcile", "Unknown", "original"));
                    if ("KSM_CLEAR_NOT_ISSUED".equals(receipt)) {
                        em.find(MemoryTaskVO.class, "original").setPolicy(
                                "mixedTarget".equals(invalid) ? "{\"schemaVersion\":1,\"ksm\":{},\"zram\":{\"enabled\":true}}"
                                : "nonemptyTarget".equals(invalid) ? "{\"schemaVersion\":1,\"ksm\":{\"enabled\":true}}"
                                : "malformedPolicy".equals(invalid) ? "invalid-json"
                                : "badSchema".equals(invalid) ? "{\"schemaVersion\":2,\"ksm\":{}}"
                                : "{\"schemaVersion\":1,\"ksm\":{}}");
                    }
                    if ("wrongExpectedControl".equals(invalid)) {
                        em.find(MemoryTaskVO.class, "query").setExpectedControlOperationUuid("other");
                    }
                    if ("nonConfig".equals(invalid)) {
                        em.find(MemoryTaskVO.class, "original").setAction("pause");
                    }
                    MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
                    if ("newPlan".equals(invalid)) state.setPolicyPlanHash("newer-plan");
                    if ("newRevision".equals(invalid)) state.setDesiredRevision(9);
                    if ("newOwner".equals(invalid)) {
                        state.setActiveTaskUuid("new-owner");
                        state.setControlOperationUuid("new-owner");
                        state.setDesiredRevision(9);
                        state.setPolicyPlanHash("newer-plan");
                    }
                    return null;
                });
                MemoryAgentResponse rejected = new MemoryAgentResponse();
                rejected.setSuccess(false); rejected.status = "Failed";
                rejected.operationUuid = "original";
                if ("wrongHost".equals(invalid)) rejected.hostUuid = "host2";
                if ("wrongTopBoot".equals(invalid)) rejected.bootId = "boot2";
                rejected.reasonCode = "wrongReason".equals(invalid)
                        ? "FIRST_ZRAM_OBSERVATION_NOT_FRESH_OR_SAFE" : receipt;
                rejected.state = new LinkedHashMap<>();
                rejected.state.put("phase", "unknownProof".equals(invalid) ? "UNKNOWN" : "NOT_ISSUED");
                rejected.state.put("bootId", "boot1");
                rejected.state.put("requestHash", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
                rejected.state.put("policyWritesIssued", false);
                if ("badHash".equals(invalid)) rejected.state.put("requestHash", "bad");
                if ("partialWrite".equals(invalid)) rejected.state.put("policyWritesIssued", true);
                if ("missingState".equals(invalid)) rejected.state = null;
                if ("wrongProofBoot".equals(invalid)) rejected.state.put("bootId", "different-boot");
                if ("missingHash".equals(invalid)) rejected.state.remove("requestHash");
                String activeTask = host1State().getActiveTaskUuid();
                String control = host1State().getControlOperationUuid();
                long desiredRevision = host1State().getDesiredRevision();
                String planHash = host1State().getPolicyPlanHash();
                Long appliedRevision = host1State().getAppliedRevision();
                String appliedPolicy = host1State().getAppliedPolicy();
                String nativeState = host1State().getState();

                String resultStatus = "unknownProof".equals(invalid) ? "Unknown" : "Failed";
                repository.result("query", resultStatus, rejected.reasonCode, rejected);

                MemoryStateVO state = host1State();
                assertEquals(invalid, "Unknown", state.getPolicyPlanStatus());
                assertEquals(invalid, "Unknown", MemoryMetricsPresentation.inventory(
                        state, null, System.currentTimeMillis()).getStatus());
                assertEquals(invalid, appliedPolicy, state.getAppliedPolicy());
                assertEquals(invalid, nativeState, state.getState());
                assertEquals(invalid, appliedRevision, state.getAppliedRevision());
                if ("KSM_CLEAR_NOT_ISSUED".equals(rejected.reasonCode)) {
                    assertEquals(invalid, "Unknown", repository.findTask("original").getStatus());
                    assertEquals(invalid, "Unknown", repository.findTask("query").getStatus());
                    assertEquals(invalid, "Unknown", repository.findTask("old-query").getStatus());
                    assertEquals(invalid, activeTask, state.getActiveTaskUuid());
                    assertEquals(invalid, control, state.getControlOperationUuid());
                    assertEquals(invalid, desiredRevision, state.getDesiredRevision());
                    assertEquals(invalid, planHash, state.getPolicyPlanHash());
                }
                if ("unknownProof".equals(invalid)) {
                    assertEquals(invalid, "Unknown", repository.findTask("original").getStatus());
                    assertEquals(invalid, "query", state.getActiveTaskUuid());
                }
            }
        }
    }


    @Test public void ordinaryReconcileRejectsContradictoryProofAndNewerControl() {
        for (String invalid : Arrays.asList("operation", "host", "boot", "stateBoot", "confirmedControl",
                "confirmedRevision", "revision", "control", "active", "appliedRevision", "stale", "future", "missing")) {
            MemoryAgentResponse ack = ordinaryReconcileFixture("apply", false);
            if ("operation".equals(invalid)) ack.operationUuid = "different";
            if ("host".equals(invalid)) ack.hostUuid = "host2";
            if ("boot".equals(invalid)) ack.bootId = "new-boot";
            if ("stateBoot".equals(invalid)) ack.state.put("bootId", "new-boot");
            if ("confirmedControl".equals(invalid)) ack.state.put("lastConfirmedOperationUuid", "different");
            if ("confirmedRevision".equals(invalid)) ack.state.put("lastConfirmedAppliedRevision", 9L);
            if ("revision".equals(invalid)) ack.appliedRevision = 7L;
            if ("stale".equals(invalid)) ack.sampleTime -= MemoryOptimizationGlobalConfig.displayTtlMillis() + 1;
            if ("future".equals(invalid)) ack.sampleTime += 60000;
            repository.transaction(em -> {
                MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
                if ("control".equals(invalid)) state.setControlOperationUuid("newer");
                if ("active".equals(invalid)) state.setActiveTaskUuid("newer");
                if ("appliedRevision".equals(invalid)) state.setAppliedRevision(9L);
                return null;
            });
            MemoryStateVO before = host1State();
            repository.result("query", "Succeeded", null, "missing".equals(invalid) ? null : ack);
            MemoryStateVO after = host1State();
            assertEquals(invalid, "Unknown", repository.findTask("original").getStatus());
            assertEquals(invalid, before.getAppliedPolicy(), after.getAppliedPolicy());
            assertEquals(invalid, before.getAppliedRevision(), after.getAppliedRevision());
            assertEquals(invalid, before.getControlOperationUuid(), after.getControlOperationUuid());
            assertEquals(invalid, before.getActiveTaskUuid(), after.getActiveTaskUuid());
        }
    }

    @Test public void completedReconcileAliasUsesExistingQueryWithoutPolicyReplay() {
        MemoryAgentResponse ack = ordinaryReconcileFixture("apply", true);
        assertFalse("observation must not apply across an unfinished query alias", repository.applyEffectivePolicyAfterConnect("host1"));
        assertEquals("query", host1State().getControlOperationUuid());
        MemoryActionRequest query = request();
        query.setScope("Host"); query.setResourceUuid("host1");
        query.setAction("reconcile"); query.setPolicy("{}");
        String parent = repository.submit(query);
        MemoryTaskVO child = claimChild(parent);
        assertEquals("original", child.getReconcileOperationUuid());
        assertEquals(8L, child.getDesiredRevision());
        assertEquals("reconcile", child.getAction());
        repository.result(child.getUuid(), "Succeeded", null, ack);
        assertEquals("original", host1State().getControlOperationUuid());
        assertEquals(8L, host1State().getDesiredRevision());
        assertEquals("Applied", host1State().getPolicyPlanStatus());
        assertEquals(Boolean.TRUE, MemoryPolicyRules.decode(host1State().getAppliedPolicy()).zram.enabled);
        assertEquals("Succeeded", repository.findTask("query").getStatus());
        assertFalse(repository.applyEffectivePolicyAfterConnect("host1"));
    }

    @Test public void completedReconcileCannotBorrowAnotherHostOrRevision() {
        for (String invalid : Arrays.asList("originalHost", "queryHost", "originalStatus", "originalAction", "queryRevision", "hostRevision")) {
            ordinaryReconcileFixture("apply", true);
            repository.transaction(em -> {
                MemoryTaskVO original = em.find(MemoryTaskVO.class, "original");
                MemoryTaskVO query = em.find(MemoryTaskVO.class, "query");
                if ("originalHost".equals(invalid)) original.setHostUuid("host2");
                if ("queryHost".equals(invalid)) query.setHostUuid("host2");
                if ("originalStatus".equals(invalid)) original.setStatus("Unknown");
                if ("originalAction".equals(invalid)) original.setAction("recoverUncertain");
                if ("queryRevision".equals(invalid)) query.setDesiredRevision(9);
                if ("hostRevision".equals(invalid)) em.find(MemoryStateVO.class, "host1").setDesiredRevision(9);
                return null;
            });
            MemoryActionRequest query = request();
            query.setScope("Host"); query.setResourceUuid("host1");
            query.setAction("reconcile"); query.setPolicy("{}");
            try { repository.submit(query); fail(invalid); }
            catch (MemoryOperationException expected) { assertEquals(invalid, "MEMORY_CONTROL_OPERATION_FENCED", expected.getCode()); }
            assertEquals("query", host1State().getControlOperationUuid());
            assertNull(host1State().getActiveTaskUuid());
            assertEquals(2L, (long) repository.transaction(em -> em.createQuery("select count(t) from MemoryTaskVO t", Long.class).getSingleResult()));
        }
    }

    @Test public void historicalReconcileReceiptAndFreshObservationRemainSeparateProofs() throws Exception {
        for (String action : Arrays.asList("apply", "clearOverride", "pause", "resume", "drain")) {
            MemoryAgentResponse observation = ordinaryReconcileFixture(action, false);
            // The actual APPLIED/POOL_READY Agent callback contains a historical
            // result, not the state endpoint's current Host/boot/sample envelope.
            String wire = "{\"appliedRevision\":8,\"operationUuid\":\"original\",\"state\":{"
                    + "\"phase\":\"POOL_READY\",\"maintenanceActivation\":{\"service\":{"
                    + "\"observed_at\":\"2026-10-14T05:08:03+08:00\"}}},\"status\":\"Succeeded\",\"success\":true}";
            MemoryAgentResponse receipt = org.zstack.utils.gson.JSONObjectUtil.toObject(wire, MemoryAgentResponse.class);
            String unchanged = org.zstack.utils.gson.JSONObjectUtil.toJsonString(receipt);
            observation.operationUuid = "separate-state-read";
            repository.result("query", "Succeeded", null, receipt, observation);
            assertEquals(action, "Succeeded", repository.findTask("query").getStatus());
            assertEquals("original", host1State().getControlOperationUuid());
            assertNull(host1State().getActiveTaskUuid());
            assertEquals(unchanged, org.zstack.utils.gson.JSONObjectUtil.toJsonString(receipt));
            assertNull("historical receipt must not acquire a synthetic current time", receipt.sampleTime);
        }
    }

    @Test public void freshObservationDoesNotReplaceContradictoryHistoricalReceipt() throws Exception {
        for (boolean automatic : new boolean[]{false, true}) {
        for (String invalid : Arrays.asList("operation", "revision", "host", "boot", "status", "success",
                "currentControl", "currentRevision", "currentFailure", "currentMissing", "currentStale")) {
            MemoryAgentResponse observation = ordinaryReconcileFixture("apply", false);
            if (automatic) {
                repository.transaction(em -> {
                    MemoryTaskVO query = em.find(MemoryTaskVO.class, "query");
                    query.setActorUuid("system:memory-config-reconcile");
                    query.setExpectedControlOperationUuid("original");
                    em.find(MemoryStateVO.class, "host1").setControlOperationUuid("original");
                    return null;
                });
            }
            MemoryAgentResponse receipt = org.zstack.utils.gson.JSONObjectUtil.toObject(
                    "{\"operationUuid\":\"original\",\"appliedRevision\":8,\"status\":\"Succeeded\",\"success\":true}",
                    MemoryAgentResponse.class);
            if ("operation".equals(invalid)) receipt.operationUuid = "different";
            if ("revision".equals(invalid)) receipt.appliedRevision = 9L;
            if ("host".equals(invalid)) receipt.hostUuid = "host2";
            if ("boot".equals(invalid)) receipt.bootId = "boot2";
            if ("status".equals(invalid)) receipt.status = "Failed";
            if ("success".equals(invalid)) receipt.setSuccess(false);
            if ("currentControl".equals(invalid)) observation.state.put("lastConfirmedOperationUuid", "other");
            if ("currentRevision".equals(invalid)) observation.appliedRevision = 9L;
            if ("currentFailure".equals(invalid)) observation.setSuccess(false);
            if ("currentStale".equals(invalid)) observation.sampleTime -= MemoryOptimizationGlobalConfig.displayTtlMillis()+1;
            if ("currentMissing".equals(invalid)) observation = null;
            MemoryStateVO before = host1State();
            repository.result("query", "Succeeded", null, receipt, observation);
            assertEquals(invalid, "Unknown", repository.findTask("query").getStatus());
            assertEquals(invalid, before.getAppliedPolicy(), host1State().getAppliedPolicy());
            assertEquals(invalid, before.getControlOperationUuid(), host1State().getControlOperationUuid());
            assertEquals(invalid, before.getActiveTaskUuid(), host1State().getActiveTaskUuid());
        }
        }
    }

    @Test public void managerReconcileUsesTwoReadOnlySerializedCallbacksAndRealRepository() throws Exception {
        for (String mode : Arrays.asList("success", "throw", "stale", "reboot", "lateControl")) {
            MemoryAgentResponse current = ordinaryReconcileFixture("apply", false);
            MemoryStateVO before = host1State();
            MemoryOptimizationManager manager = new MemoryOptimizationManager();
            Field repoField = MemoryOptimizationManager.class.getDeclaredField("repository");
            repoField.setAccessible(true); repoField.set(manager, repository);
            List<org.zstack.kvm.KVMHostAsyncHttpCallMsg> requests = new ArrayList<>();
            List<org.zstack.core.cloudbus.CloudBusCallBack> callbacks = new ArrayList<>();
            org.zstack.core.cloudbus.CloudBus bus = (org.zstack.core.cloudbus.CloudBus) Proxy.newProxyInstance(
                    org.zstack.core.cloudbus.CloudBus.class.getClassLoader(),
                    new Class<?>[]{org.zstack.core.cloudbus.CloudBus.class}, (proxy, method, args) -> {
                        if ("send".equals(method.getName()) && args != null && args.length == 2) {
                            org.zstack.kvm.KVMHostAsyncHttpCallMsg request = (org.zstack.kvm.KVMHostAsyncHttpCallMsg) args[0];
                            requests.add(request);
                            if ("throw".equals(mode) && requests.size() == 2) throw new IllegalStateException("state transport unavailable");
                            callbacks.add((org.zstack.core.cloudbus.CloudBusCallBack) args[1]);
                        }
                        if (method.getReturnType() == boolean.class) return false;
                        if (method.getReturnType() == int.class) return 0;
                        if (method.getReturnType() == long.class) return 0L;
                        return null;
                    });
            Field busField = MemoryOptimizationManager.class.getDeclaredField("bus");
            busField.setAccessible(true); busField.set(manager, bus);
            java.lang.reflect.Method dispatch = MemoryOptimizationManager.class.getDeclaredMethod("dispatch",
                    MemoryTaskVO.class, boolean.class, Runnable.class);
            dispatch.setAccessible(true); dispatch.invoke(manager, repository.findTask("query"), false, (Runnable) () -> { });
            assertEquals(1, requests.size());
            assertEquals("/memory/optimization/reconcile", requests.get(0).getPath());
            String receiptWire = "{\"success\":true,\"status\":\"Succeeded\",\"operationUuid\":\"original\","
                    + "\"appliedRevision\":8,\"state\":{\"phase\":\"POOL_READY\"}}";
            org.zstack.kvm.KVMHostAsyncHttpCallReply receipt = new org.zstack.kvm.KVMHostAsyncHttpCallReply();
            receipt.setResponse(org.zstack.utils.gson.JSONObjectUtil.toObject(receiptWire, LinkedHashMap.class));
            callbacks.get(0).run(receipt);
            assertEquals(2, requests.size());
            assertEquals("/memory/optimization/state", requests.get(1).getPath());
            assertEquals("host1", requests.get(1).getHostUuid());
            Map<?, ?> stateCommand = org.zstack.utils.gson.JSONObjectUtil.toObject(requests.get(1).getCommand(), Map.class);
            assertFalse(stateCommand.containsKey("policy"));
            assertFalse(stateCommand.containsKey("action"));
            assertNotEquals("original", stateCommand.get("operationUuid"));
            if (!"throw".equals(mode)) {
                current.operationUuid = (String) stateCommand.get("operationUuid");
                if ("stale".equals(mode)) current.sampleTime -= MemoryOptimizationGlobalConfig.displayTtlMillis()+1;
                if ("reboot".equals(mode)) {
                    current.bootId = "boot2"; current.state.put("bootId", "boot2");
                    // Even a concurrent periodic observer seeing the new boot
                    // cannot turn the old query receipt into current proof.
                    repository.transaction(em -> { em.find(MemoryStateVO.class, "host1")
                            .setState("{\"bootId\":\"boot2\"}"); return null; });
                }
                if ("lateControl".equals(mode)) repository.transaction(em -> {
                    em.find(MemoryStateVO.class, "host1").setControlOperationUuid("new-control"); return null; });
                org.zstack.kvm.KVMHostAsyncHttpCallReply observation = new org.zstack.kvm.KVMHostAsyncHttpCallReply();
                observation.setResponse(org.zstack.utils.gson.JSONObjectUtil.toObject(
                        org.zstack.utils.gson.JSONObjectUtil.toJsonString(current), LinkedHashMap.class));
                callbacks.get(1).run(observation);
            }
            assertEquals(mode, "success".equals(mode) ? "Succeeded" : "Unknown", repository.findTask("query").getStatus());
            if ("success".equals(mode)) {
                assertEquals("original", host1State().getControlOperationUuid());
                assertNull(host1State().getActiveTaskUuid());
                assertEquals("Applied", host1State().getPolicyPlanStatus());
            } else {
                assertEquals(before.getAppliedPolicy(), host1State().getAppliedPolicy());
                assertEquals("query", host1State().getActiveTaskUuid());
                assertEquals("lateControl".equals(mode) ? "new-control" : "query", host1State().getControlOperationUuid());
            }
            assertEquals("neither callback may dispatch configuration", 2, requests.size());
        }
    }

    private MemoryAgentResponse ordinaryReconcileFixture(String action, boolean completed) {
        MemoryAgentResponse ack = new MemoryAgentResponse();
        ack.setSuccess(true); ack.status = "Succeeded"; ack.operationUuid = "original";
        ack.hostUuid = "host1"; ack.bootId = "boot1"; ack.appliedRevision = 8L;
        ack.sampleTime = System.currentTimeMillis(); ack.state = completeState(42L, "current");
        ack.capabilities = new HashMap<>();
        for (String field : Arrays.asList("supported", "ksm", "ksmZeroPages", "zram", "writeback")) ack.capabilities.put(field, true);
        ack.state.put("managed", true); ack.state.put("bootId", "boot1");
        ack.state.put("lastConfirmedOperationUuid", "original"); ack.state.put("lastConfirmedAppliedRevision", 8L);
        Map<String, Object> lifecycle = new HashMap<>();
        lifecycle.put("paused", false); lifecycle.put("activeState", "compatibility");
        ack.state.put("lifecycle", lifecycle);
        ack.state.put("poolGeneration", "pool1");
        Map<String, Object> pool = new HashMap<>();
        pool.put("pool_generation", "pool1"); pool.put("device", "/dev/zram0");
        pool.put("quality", "native_host_device"); pool.put("observed_at", Instant.ofEpochMilli(ack.sampleTime).toString());
        ack.state.put("host_zram", pool);
        ((Map<String, Object>) ack.state.get("actual")).put("zram", Collections.singletonMap("efficiencyEnabled", true));
        repository.transaction(em -> {
            em.createQuery("delete from MemoryTaskVO").executeUpdate();
            em.createQuery("delete from ResourceConfigVO").executeUpdate();
            writeOverride(em, "host1", "HostVO", "zram.enabled", true);
            if ("apply".equals(action)) writeOverride(em, "host1", "HostVO", "ksm.enabled", true);
            MemoryTaskVO original = testTask("original", null, action, completed ? "Succeeded" : "Unknown", null);
            original.setDesiredRevision(8); original.setPolicy("{\"zram\":{\"enabled\":true}}");
            original.setPolicyPlanHash("original-plan"); em.persist(original);
            MemoryTaskVO query = testTask("query", null, "reconcile", completed ? "Succeeded" : "Applying", "original");
            query.setDesiredRevision(8); em.persist(query);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setDesiredRevision(8); state.setAppliedRevision(completed ? 8L : 6L);
            state.setStatus(completed ? "Succeeded" : "Unknown");
            state.setControlOperationUuid("query"); state.setActiveTaskUuid(completed ? null : "query");
            state.setAppliedPolicy("{\"ksm\":{\"enabled\":true}}"); state.setAppliedPolicyHash("previous");
            state.setPolicyPlanHash("original-plan"); state.setPolicyPlanStatus("Unknown");
            state.setPolicyBlockedFields("{\"zram.enabled\":{\"reason\":\"BACKEND_NOT_READY\"}}");
            state.setLastSampleTime(ack.sampleTime - 1); state.setState("{\"bootId\":\"boot1\",\"managed\":true}");
            return null;
        });
        return ack;
    }

    @Test public void uncertainRecoveryRetainsUnknownHistoryAndRestoresOnlyDrainFence() {
        MemoryActionRequest msg = uncertainRecoveryRequest();
        String parent = repository.submit(msg);
        MemoryTaskVO task = claimChild(parent);
        assertEquals(MemoryUncertainRecoveryRulesTest.OLD, host1State().getControlOperationUuid());
        assertEquals(task.getUuid(), host1State().getActiveTaskUuid());
        // Once recovery is admitted, a late old callback cannot change the
        // evidence being recovered even before the retirement ACK arrives.
        MemoryAgentResponse racing = new MemoryAgentResponse(); racing.operationUuid = MemoryUncertainRecoveryRulesTest.OLD;
        racing.setSuccess(true); racing.status = "Succeeded";
        repository.result(racing.operationUuid, "Succeeded", "late old success", racing);
        repository.result(racing.operationUuid, "Failed", "late old failure", racing);
        assertEquals("Unknown", repository.findTask(MemoryUncertainRecoveryRulesTest.OLD).getStatus());
        repository.result(task.getUuid(), "Succeeded", null, MemoryUncertainRecoveryRulesTest.proof(task.getUuid()));
        assertEquals("Succeeded", repository.findTask(task.getUuid()).getStatus());
        assertEquals("Unknown", repository.findTask(MemoryUncertainRecoveryRulesTest.OLD).getStatus());
        assertEquals("missing receipt", repository.findTask(MemoryUncertainRecoveryRulesTest.OLD).getReason());
        assertEquals("Unknown", repository.findTask("old-uncertain-parent").getStatus());
        assertNull(host1State().getActiveTaskUuid());
        assertEquals(MemoryUncertainRecoveryRulesTest.DRAIN, host1State().getControlOperationUuid());
        assertNull("recovery is not policy application", host1State().getAppliedRevision());
        assertTrue(host1State().getState().contains("UNCERTAIN_RECOVERED"));
        // A late historical callback cannot relabel a retired operation.
        MemoryAgentResponse late = new MemoryAgentResponse(); late.setSuccess(true); late.status = "Succeeded";
        late.operationUuid = MemoryUncertainRecoveryRulesTest.OLD; late.appliedRevision = 77L;
        repository.result(late.operationUuid, "Succeeded", "late", late);
        assertEquals("Unknown", repository.findTask(late.operationUuid).getStatus());
        assertNull(host1State().getAppliedRevision());
        assertEquals(MemoryUncertainRecoveryRulesTest.DRAIN, host1State().getControlOperationUuid());
        // A NEW request goes through the ordinary policy path; no old apply is replayed.
        MemoryActionRequest apply = request(); apply.setScope("Host"); apply.setResourceUuid("host1");
        assertNotNull(repository.submit(apply));
        assertEquals("Unknown", repository.findTask(MemoryUncertainRecoveryRulesTest.OLD).getStatus());
    }

    @Test public void partialPolicyRecoveryRetainsOldUnknownAndAppliesOnlyItsConfirmedPolicy() {
        MemoryActionRequest msg = uncertainRecoveryRequest();
        msg.getRecovery().setDrainControlOperationUuid(null);
        repository.transaction(em -> {
            MemoryTaskVO old = em.find(MemoryTaskVO.class, MemoryUncertainRecoveryRulesTest.OLD);
            old.setDesiredRevision(9); old.setPolicy(MemoryUncertainRecoveryRulesTest.TUNING_POLICY);
            MemoryStateVO host = em.find(MemoryStateVO.class, "host1");
            host.setDesiredRevision(9); host.setAppliedRevision(8L);
            host.setAppliedPolicy("{\"ksm\":{\"enabled\":true}}");
            return null;
        });
        MemoryTaskVO task = claimChild(repository.submit(msg));
        repository.result(task.getUuid(), "Unknown", "response lost", null);
        MemoryActionRequest query = request(); query.setScope("Host"); query.setResourceUuid("host1");
        query.setPolicy("{}"); query.setAction("reconcile");
        MemoryTaskVO reconciliation = claimChild(repository.submit(query));
        MemoryAgentResponse proof = MemoryUncertainRecoveryRulesTest.policyProof(task.getUuid());
        repository.result(reconciliation.getUuid(), "Succeeded", null, proof);
        assertEquals("Succeeded", repository.findTask(task.getUuid()).getStatus());
        assertEquals("Succeeded", repository.findTask(reconciliation.getUuid()).getStatus());
        assertEquals("Unknown", repository.findTask(MemoryUncertainRecoveryRulesTest.OLD).getStatus());
        assertEquals("missing receipt", repository.findTask(MemoryUncertainRecoveryRulesTest.OLD).getReason());
        assertEquals("Unknown", repository.findTask("old-uncertain-parent").getStatus());
        assertEquals(task.getUuid(), host1State().getControlOperationUuid());
        assertNull(host1State().getActiveTaskUuid()); assertEquals(Long.valueOf(9), host1State().getAppliedRevision());
        assertEquals("{\"ksm\":{\"enabled\":true},\"schemaVersion\":1,\"writeback\":{\"batchBytes\":67108864}}",
                host1State().getAppliedPolicy());
        MemoryAgentResponse late = new MemoryAgentResponse(); late.operationUuid = MemoryUncertainRecoveryRulesTest.OLD;
        late.setSuccess(true); late.status = "Succeeded"; late.appliedRevision = 10L;
        repository.result(late.operationUuid, "Succeeded", "late old result", late);
        assertEquals("Unknown", repository.findTask(late.operationUuid).getStatus());
        assertEquals(Long.valueOf(9), host1State().getAppliedRevision());
    }

    @Test public void partialServiceRecoveryLoadsBoundAnchorTaskBeforeAcceptingProof() {
        MemoryActionRequest msg = uncertainRecoveryRequest();
        msg.getRecovery().setDrainControlOperationUuid(null);
        String originalPolicy = "{\"schemaVersion\":1,\"ksm\":{\"enabled\":true,\"zeroPagesEnabled\":true},"
                + "\"writeback\":{\"enabled\":false}}";
        repository.transaction(em -> {
            MemoryTaskVO old = em.find(MemoryTaskVO.class, MemoryUncertainRecoveryRulesTest.OLD);
            old.setDesiredRevision(9); old.setPolicy(originalPolicy);
            MemoryTaskVO anchor = testTask(MemoryUncertainRecoveryRulesTest.ANCHOR, null, "apply", "Succeeded", null);
            anchor.setDesiredRevision(8);
            anchor.setPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":false,\"zeroPagesEnabled\":false},"
                    + "\"writeback\":{\"enabled\":true,\"backendResourceUuid\":"
                    + "\"33333333333333333333333333333333\",\"backendCapacityBytes\":21474836480}}");
            em.persist(anchor);
            MemoryStateVO host = em.find(MemoryStateVO.class, "host1");
            host.setDesiredRevision(9); host.setAppliedRevision(8L); host.setAppliedPolicy("{}");
            return null;
        });
        MemoryTaskVO recovery = claimChild(repository.submit(msg));
        repository.result(recovery.getUuid(), "Unknown", "response lost", null);

        MemoryActionRequest query = request(); query.setScope("Host"); query.setResourceUuid("host1");
        query.setPolicy("{}"); query.setAction("reconcile");
        MemoryTaskVO reconciliation = claimChild(repository.submit(query));
        MemoryAgentResponse proof = MemoryUncertainRecoveryRulesTest.partialServicePolicyProof(
                recovery.getUuid(), originalPolicy);
        repository.result(reconciliation.getUuid(), "Succeeded", null, proof);

        assertEquals("Succeeded", repository.findTask(recovery.getUuid()).getStatus());
        assertEquals("Succeeded", repository.findTask(reconciliation.getUuid()).getStatus());
        assertEquals("Unknown", repository.findTask(MemoryUncertainRecoveryRulesTest.OLD).getStatus());
        assertEquals(recovery.getUuid(), host1State().getControlOperationUuid());
        assertNull(host1State().getActiveTaskUuid());
        assertEquals(Long.valueOf(9), host1State().getAppliedRevision());
    }

    @Test public void partialServiceRecoveryAcceptsOnlyExactReadOnlyConfigurationAlias() {
        MemoryActionRequest msg = uncertainRecoveryRequest();
        msg.getRecovery().setDrainControlOperationUuid(null);
        String originalPolicy = "{\"schemaVersion\":1,\"ksm\":{\"enabled\":true,\"zeroPagesEnabled\":true},"
                + "\"writeback\":{\"enabled\":false}}";
        repository.transaction(em -> {
            MemoryTaskVO old = em.find(MemoryTaskVO.class, MemoryUncertainRecoveryRulesTest.OLD);
            old.setDesiredRevision(9); old.setPolicy(originalPolicy);
            MemoryTaskVO anchor = testTask(MemoryUncertainRecoveryRulesTest.ANCHOR, null, "apply", "Succeeded", null);
            anchor.setDesiredRevision(8);
            anchor.setPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":false,\"zeroPagesEnabled\":false},"
                    + "\"writeback\":{\"enabled\":true,\"backendResourceUuid\":"
                    + "\"33333333333333333333333333333333\",\"backendCapacityBytes\":21474836480}}");
            em.persist(anchor);
            MemoryTaskVO alias = testTask("read-only-query-alias", null, "reconcile", "Unknown",
                    MemoryUncertainRecoveryRulesTest.OLD);
            alias.setActorUuid("system:memory-config-reconcile");
            alias.setDesiredRevision(9); alias.setExpectedControlOperationUuid(MemoryUncertainRecoveryRulesTest.OLD);
            em.persist(alias);
            MemoryStateVO host = em.find(MemoryStateVO.class, "host1");
            host.setDesiredRevision(9); host.setAppliedRevision(8L); host.setAppliedPolicy("{}");
            host.setActiveTaskUuid(alias.getUuid()); host.setControlOperationUuid(MemoryUncertainRecoveryRulesTest.OLD);
            return null;
        });

        // A similarly shaped task is not sufficient: it must be the exact
        // system read-only alias for this still-current original operation.
        repository.transaction(em -> {
            em.find(MemoryTaskVO.class, "read-only-query-alias")
                    .setReconcileOperationUuid(MemoryUncertainRecoveryRulesTest.ANCHOR);
            return null;
        });
        try { repository.submit(msg); fail("alias for a different operation must not authorize recovery"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_UNCERTAIN_RECOVERY_FENCED", expected.getCode()); }
        assertEquals("read-only-query-alias", host1State().getActiveTaskUuid());
        assertEquals(MemoryUncertainRecoveryRulesTest.OLD, host1State().getControlOperationUuid());

        repository.transaction(em -> {
            MemoryTaskVO alias = em.find(MemoryTaskVO.class, "read-only-query-alias");
            alias.setReconcileOperationUuid(MemoryUncertainRecoveryRulesTest.OLD);
            alias.setActorUuid("ordinary-user");
            return null;
        });
        try { repository.submit(msg); fail("user reconciliation must not authorize recovery"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_UNCERTAIN_RECOVERY_FENCED", expected.getCode()); }
        assertEquals("read-only-query-alias", host1State().getActiveTaskUuid());
        assertEquals(MemoryUncertainRecoveryRulesTest.OLD, host1State().getControlOperationUuid());

        repository.transaction(em -> {
            em.find(MemoryTaskVO.class, "read-only-query-alias")
                    .setActorUuid("system:memory-config-reconcile");
            return null;
        });
        for (String invalid : Arrays.asList("scope", "host", "resource", "expectedControl", "status",
                "revision", "originalRevisionWithDrain", "currentControl")) {
            if ("originalRevisionWithDrain".equals(invalid)) {
                msg.getRecovery().setDrainControlOperationUuid(MemoryUncertainRecoveryRulesTest.DRAIN);
            }
            repository.transaction(em -> {
                MemoryTaskVO alias = em.find(MemoryTaskVO.class, "read-only-query-alias");
                MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
                MemoryTaskVO original = em.find(MemoryTaskVO.class, MemoryUncertainRecoveryRulesTest.OLD);
                if ("scope".equals(invalid)) alias.setScope("Cluster");
                if ("host".equals(invalid)) alias.setHostUuid("host2");
                if ("resource".equals(invalid)) alias.setResourceUuid("host2");
                if ("expectedControl".equals(invalid)) alias.setExpectedControlOperationUuid(MemoryUncertainRecoveryRulesTest.ANCHOR);
                if ("status".equals(invalid)) alias.setStatus("Succeeded");
                if ("revision".equals(invalid)) alias.setDesiredRevision(8);
                if ("originalRevisionWithDrain".equals(invalid)) original.setDesiredRevision(8);
                if ("currentControl".equals(invalid)) state.setControlOperationUuid(MemoryUncertainRecoveryRulesTest.ANCHOR);
                return null;
            });
            try { repository.submit(msg); fail(invalid); }
            catch (MemoryOperationException expected) {
                String expectedCode = "status".equals(invalid)
                        ? "MEMORY_HOST_BUSY" : "MEMORY_UNCERTAIN_RECOVERY_FENCED";
                assertEquals(invalid, expectedCode, expected.getCode());
            }
            assertEquals(invalid, "read-only-query-alias", host1State().getActiveTaskUuid());
            repository.transaction(em -> {
                MemoryTaskVO alias = em.find(MemoryTaskVO.class, "read-only-query-alias");
                MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
                MemoryTaskVO original = em.find(MemoryTaskVO.class, MemoryUncertainRecoveryRulesTest.OLD);
                alias.setScope("Host"); alias.setHostUuid("host1"); alias.setResourceUuid("host1");
                alias.setExpectedControlOperationUuid(MemoryUncertainRecoveryRulesTest.OLD);
                alias.setStatus("Unknown"); alias.setDesiredRevision(9);
                original.setDesiredRevision(9);
                state.setControlOperationUuid(MemoryUncertainRecoveryRulesTest.OLD);
                return null;
            });
            msg.getRecovery().setDrainControlOperationUuid(null);
        }

        MemoryTaskVO recovery = claimChild(repository.submit(msg));
        assertEquals(recovery.getUuid(), host1State().getActiveTaskUuid());
        assertEquals(MemoryUncertainRecoveryRulesTest.OLD, host1State().getControlOperationUuid());
        assertEquals("Unknown", repository.findTask("read-only-query-alias").getStatus());

        // A delayed terminal callback from the old read-only alias must not
        // settle its original apply after recovery has taken the active slot.
        repository.result("read-only-query-alias", "Failed", "late verify callback", null);
        assertEquals(recovery.getUuid(), host1State().getActiveTaskUuid());
        assertEquals(MemoryUncertainRecoveryRulesTest.OLD, host1State().getControlOperationUuid());
        assertEquals("Unknown", repository.findTask(MemoryUncertainRecoveryRulesTest.OLD).getStatus());
        assertEquals("Unknown", repository.findTask("read-only-query-alias").getStatus());

        repository.result(recovery.getUuid(), "Succeeded", null,
                MemoryUncertainRecoveryRulesTest.partialServicePolicyProof(recovery.getUuid(), originalPolicy));
        repository.result("read-only-query-alias", "Failed", "late verify after recovery", null);
        assertEquals("Succeeded", repository.findTask(recovery.getUuid()).getStatus());
        assertEquals("Unknown", repository.findTask("read-only-query-alias").getStatus());
        assertEquals("Unknown", repository.findTask(MemoryUncertainRecoveryRulesTest.OLD).getStatus());
        assertNull(host1State().getActiveTaskUuid());
        assertEquals(recovery.getUuid(), host1State().getControlOperationUuid());
        assertEquals(Long.valueOf(9), host1State().getAppliedRevision());
    }

    @Test public void partialPolicyRecoveryPreflightFailureKeepsOriginalOwner() {
        MemoryActionRequest msg = uncertainRecoveryRequest(); msg.getRecovery().setDrainControlOperationUuid(null);
        MemoryTaskVO task = claimChild(repository.submit(msg));
        MemoryAgentResponse rejected = MemoryUncertainRecoveryRulesTest.notIssued(task.getUuid());
        ((Map<?, ?>) rejected.state.get("recoveryRejectionProof")).remove("drainControlOperationUuid");
        repository.result(task.getUuid(), "Failed", "native policy changed", rejected);
        assertEquals("Failed", repository.findTask(task.getUuid()).getStatus());
        assertEquals(MemoryUncertainRecoveryRulesTest.OLD, host1State().getActiveTaskUuid());
        assertEquals(MemoryUncertainRecoveryRulesTest.OLD, host1State().getControlOperationUuid());
        assertEquals("Unknown", repository.findTask(MemoryUncertainRecoveryRulesTest.OLD).getStatus());
        assertNull(host1State().getAppliedRevision());
    }

    @Test public void uncertainRecoveryResponseLossCanBeReadOnlyReconciledWithoutRelabelingOldApply() {
        MemoryTaskVO task = claimChild(repository.submit(uncertainRecoveryRequest()));
        repository.result(task.getUuid(), "Unknown", "response lost", null);
        assertEquals(task.getUuid(), host1State().getActiveTaskUuid());
        MemoryActionRequest query = request(); query.setScope("Host"); query.setResourceUuid("host1");
        query.setPolicy("{}"); query.setAction("reconcile");
        MemoryTaskVO reconciliation = claimChild(repository.submit(query));
        assertEquals(task.getUuid(), reconciliation.getReconcileOperationUuid());
        assertEquals(MemoryUncertainRecoveryRulesTest.OLD, host1State().getControlOperationUuid());
        repository.result(reconciliation.getUuid(), "Succeeded", null, MemoryUncertainRecoveryRulesTest.proof(task.getUuid()));
        assertEquals("Succeeded", repository.findTask(task.getUuid()).getStatus());
        assertEquals("Succeeded", repository.findTask(reconciliation.getUuid()).getStatus());
        assertEquals("Unknown", repository.findTask(MemoryUncertainRecoveryRulesTest.OLD).getStatus());
        assertNull(host1State().getActiveTaskUuid());
        assertEquals(MemoryUncertainRecoveryRulesTest.DRAIN, host1State().getControlOperationUuid());
    }

    @Test public void uncertainRecoveryRejectsContradictoryAckAndDoesNotRefreshTrustedState() {
        MemoryTaskVO task = claimChild(repository.submit(uncertainRecoveryRequest()));
        String originalState = host1State().getState();
        MemoryAgentResponse bad = MemoryUncertainRecoveryRulesTest.proof(task.getUuid());
        bad.bootId = "other-boot"; bad.appliedRevision = 77L;
        repository.result(task.getUuid(), "Succeeded", null, bad);
        assertEquals("Unknown", repository.findTask(task.getUuid()).getStatus());
        assertEquals(task.getUuid(), host1State().getActiveTaskUuid());
        assertEquals(MemoryUncertainRecoveryRulesTest.OLD, host1State().getControlOperationUuid());
        assertEquals(originalState, host1State().getState()); assertNull(host1State().getAppliedRevision());
        // Transport failure cannot release this slot either.
        repository.result(task.getUuid(), "Failed", "transport exception", null);
        assertEquals(task.getUuid(), host1State().getActiveTaskUuid());
    }

    @Test public void uncertainRecoveryRequiresFreshExactOwner() {
        MemoryActionRequest msg = uncertainRecoveryRequest();
        repository.transaction(em -> { em.find(MemoryStateVO.class, "host1").setLastSampleTime(1L); return null; });
        try { repository.submit(msg); fail("stale boot observation must fail"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_UNCERTAIN_RECOVERY_FENCED", expected.getCode()); }
        repository.transaction(em -> { em.find(MemoryStateVO.class, "host1").setLastSampleTime(System.currentTimeMillis()); return null; });
        repository.submit(msg);
        assertEquals("Unknown", repository.findTask(MemoryUncertainRecoveryRulesTest.OLD).getStatus());
    }

    @Test public void lateRecoveryResultCannotReleaseNewerHostOperation() {
        MemoryTaskVO task = claimChild(repository.submit(uncertainRecoveryRequest()));
        repository.transaction(em -> { em.find(MemoryStateVO.class, "host1").setActiveTaskUuid("newer-operation"); return null; });
        repository.result(task.getUuid(), "Succeeded", null, MemoryUncertainRecoveryRulesTest.proof(task.getUuid()));
        assertEquals("Unknown", repository.findTask(task.getUuid()).getStatus());
        assertEquals("newer-operation", host1State().getActiveTaskUuid());
        assertEquals(MemoryUncertainRecoveryRulesTest.OLD, host1State().getControlOperationUuid());
    }

    @Test public void recoveryLicenseExpiryBeforeDispatchRestoresOldSlotAndAllowsANewRequest() {
        MemoryActionRequest msg = uncertainRecoveryRequest();
        MemoryTaskVO task = claimChild(repository.submit(msg));
        repository.recoveryLicenseExpiredBeforeDispatch(task.getUuid());
        assertEquals("Failed", repository.findTask(task.getUuid()).getStatus());
        assertEquals("CLOUD_LICENSE_EXPIRED_BEFORE_RECOVERY_DISPATCH", repository.findTask(task.getUuid()).getReason());
        assertEquals(MemoryUncertainRecoveryRulesTest.OLD, host1State().getActiveTaskUuid());
        assertEquals(MemoryUncertainRecoveryRulesTest.OLD, host1State().getControlOperationUuid());
        assertEquals("Unknown", repository.findTask(MemoryUncertainRecoveryRulesTest.OLD).getStatus());
        MemoryTaskVO next = claimChild(repository.submit(msg));
        // A duplicate local callback for the previous request cannot touch the new owner.
        repository.recoveryLicenseExpiredBeforeDispatch(task.getUuid());
        assertEquals(next.getUuid(), host1State().getActiveTaskUuid());
    }

    @Test public void recoveryNotIssuedProofRestoresOldUnknownOwnerAndAllowsNewRecoveryRequest() {
        MemoryActionRequest msg = uncertainRecoveryRequest();
        MemoryTaskVO task = claimChild(repository.submit(msg));
        String before = host1State().getState();
        repository.result(task.getUuid(), "Failed", "precheck rejected", MemoryUncertainRecoveryRulesTest.notIssued(task.getUuid()));
        assertEquals("Failed", repository.findTask(task.getUuid()).getStatus());
        assertEquals("Unknown", repository.findTask(MemoryUncertainRecoveryRulesTest.OLD).getStatus());
        assertEquals("Unknown", repository.findTask("old-uncertain-parent").getStatus());
        assertEquals(MemoryUncertainRecoveryRulesTest.OLD, host1State().getActiveTaskUuid());
        assertEquals(MemoryUncertainRecoveryRulesTest.OLD, host1State().getControlOperationUuid());
        assertEquals("Unknown", host1State().getStatus());
        assertEquals(before, host1State().getState()); assertNull(host1State().getAppliedRevision());
        MemoryTaskVO next = claimChild(repository.submit(msg));
        repository.result(task.getUuid(), "Failed", "late duplicate", MemoryUncertainRecoveryRulesTest.notIssued(task.getUuid()));
        assertEquals(next.getUuid(), host1State().getActiveTaskUuid());
    }

    @Test public void recoveryNotIssuedResponseLossReconcilesOnlyNewRecoveryAndKeepsOldUnknown() {
        MemoryTaskVO task = claimChild(repository.submit(uncertainRecoveryRequest()));
        repository.result(task.getUuid(), "Unknown", "response lost", null);
        MemoryActionRequest query = request(); query.setScope("Host"); query.setResourceUuid("host1");
        query.setPolicy("{}"); query.setAction("reconcile");
        MemoryTaskVO reconciliation = claimChild(repository.submit(query));
        repository.result(reconciliation.getUuid(), "Failed", "precheck rejected", MemoryUncertainRecoveryRulesTest.notIssued(task.getUuid()));
        assertEquals("Failed", repository.findTask(task.getUuid()).getStatus());
        assertEquals("Failed", repository.findTask(reconciliation.getUuid()).getStatus());
        assertEquals("Unknown", repository.findTask(MemoryUncertainRecoveryRulesTest.OLD).getStatus());
        assertEquals(MemoryUncertainRecoveryRulesTest.OLD, host1State().getActiveTaskUuid());
        assertEquals(MemoryUncertainRecoveryRulesTest.OLD, host1State().getControlOperationUuid());
    }

    @Test public void recoveryNotIssuedRequiresEveryProofFieldAndNoContradictoryRetirementEvidence() {
        MemoryTaskVO task = claimChild(repository.submit(uncertainRecoveryRequest()));
        String before = host1State().getState();
        for (Object field : ((Map<?, ?>) MemoryUncertainRecoveryRulesTest.notIssued(task.getUuid()).state.get("recoveryRejectionProof")).keySet()) {
            MemoryAgentResponse bad = MemoryUncertainRecoveryRulesTest.notIssued(task.getUuid());
            ((Map<?, ?>) bad.state.get("recoveryRejectionProof")).remove(field);
            repository.result(task.getUuid(), "Failed", "missing proof field", bad);
            assertEquals("Unknown", repository.findTask(task.getUuid()).getStatus());
            assertEquals(task.getUuid(), host1State().getActiveTaskUuid());
        }
        for (String contradiction : Arrays.asList("retirementReceipt", "recoveryProof", "appliedRevision", "bootId", "success")) {
            MemoryAgentResponse bad = MemoryUncertainRecoveryRulesTest.notIssued(task.getUuid());
            if ("appliedRevision".equals(contradiction)) { bad.appliedRevision = 1L; }
            else if ("bootId".equals(contradiction)) { bad.bootId = "other-boot"; }
            else if ("success".equals(contradiction)) { bad.setSuccess(true); }
            else { bad.state.put(contradiction, Collections.singletonMap("retired", true)); }
            repository.result(task.getUuid(), "Failed", "contradictory proof", bad);
            assertEquals(task.getUuid(), host1State().getActiveTaskUuid());
        }
        assertEquals(before, host1State().getState()); assertNull(host1State().getAppliedRevision());
        assertEquals("Unknown", repository.findTask(MemoryUncertainRecoveryRulesTest.OLD).getStatus());
    }

    @Test public void unconfirmedTaskResponsesCannotOverwriteControlProofOrAppliedRevision() {
        MemoryActionRequest msg = request(); msg.setScope("Host"); msg.setResourceUuid("host1");
        MemoryTaskVO task = claimChild(repository.submit(msg));
        String before = host1State().getState();
        MemoryAgentResponse untrusted = MemoryUncertainRecoveryRulesTest.proof(task.getUuid());
        untrusted.setSuccess(false); untrusted.status = "Unknown"; untrusted.appliedRevision = 999L;
        repository.result(task.getUuid(), "Unknown", "unconfirmed", untrusted);
        assertEquals(before, host1State().getState()); assertNull(host1State().getAppliedRevision());
        untrusted.status = "Failed";
        repository.result(task.getUuid(), "Failed", "unconfirmed", untrusted);
        assertEquals(before, host1State().getState()); assertNull(host1State().getAppliedRevision());
    }

    @Test public void unresolvedDrainedActivationOffersTheExactUncertainRecoveryRequest() {
        uncertainRecoveryRequest();
        repository.transaction(em -> {
            em.find(MemoryTaskVO.class, MemoryUncertainRecoveryRulesTest.OLD)
                    .setExpectedControlOperationUuid(MemoryUncertainRecoveryRulesTest.DRAIN);
            em.find(MemoryStateVO.class, "host1").setState("{\"bootId\":\"" + MemoryUncertainRecoveryRulesTest.BOOT
                    + "\",\"maintenanceProof\":{\"oldPoolGeneration\":\"" + MemoryUncertainRecoveryRulesTest.POOL + "\"}}");
            return null;
        });

        Map<String, String> offered = repository.uncertainRecoveryRequest("host1");

        assertEquals(MemoryUncertainRecoveryRulesTest.OLD, offered.get("expectedControlOperationUuid"));
        assertEquals(MemoryUncertainRecoveryRulesTest.BOOT, offered.get("expectedHostBootId"));
        assertEquals(MemoryUncertainRecoveryRulesTest.POOL, offered.get("expectedPoolGeneration"));
        assertEquals(MemoryUncertainRecoveryRulesTest.DRAIN, offered.get("drainControlOperationUuid"));
        repository.transaction(em -> {
            em.find(MemoryStateVO.class, "host1").setLastSampleTime(1L);
            return null;
        });
        assertNull("a stale sample cannot be confirmed", repository.uncertainRecoveryRequest("host1"));
    }

    @Test public void uncertainRecoveryRequestReadsTheLegacyMaintenanceProofLayout() {
        uncertainRecoveryRequest();
        repository.transaction(em -> {
            em.find(MemoryTaskVO.class, MemoryUncertainRecoveryRulesTest.OLD)
                    .setExpectedControlOperationUuid(MemoryUncertainRecoveryRulesTest.DRAIN);
            em.find(MemoryStateVO.class, "host1").setState("{\"bootId\":\"" + MemoryUncertainRecoveryRulesTest.BOOT
                    + "\",\"old_pool_generation\":\"" + MemoryUncertainRecoveryRulesTest.POOL + "\"}");
            return null;
        });
        assertEquals(MemoryUncertainRecoveryRulesTest.POOL,
                repository.uncertainRecoveryRequest("host1").get("expectedPoolGeneration"));
    }

    @Test public void uncertainApplyWithoutDrainOffersTheCurrentPoolRequest() {
        uncertainRecoveryRequest();
        repository.transaction(em -> {
            em.find(MemoryStateVO.class, "host1").setState("{\"bootId\":\"" + MemoryUncertainRecoveryRulesTest.BOOT
                    + "\",\"poolGeneration\":\"" + MemoryUncertainRecoveryRulesTest.POOL + "\"}");
            return null;
        });

        Map<String, String> offered = repository.uncertainRecoveryRequest("host1");

        assertEquals(MemoryUncertainRecoveryRulesTest.POOL, offered.get("expectedPoolGeneration"));
        assertFalse("an apply that followed no drain has none to name", offered.containsKey("drainControlOperationUuid"));
    }

    private MemoryActionRequest uncertainRecoveryRequest() {
        repository.transaction(em -> {
            em.persist(testTask("old-uncertain-parent", null, "apply", "Unknown", null));
            MemoryTaskVO old = testTask(MemoryUncertainRecoveryRulesTest.OLD, "old-uncertain-parent", "apply", "Unknown", null);
            old.setReason("missing receipt"); em.persist(old);
            em.persist(testTask(MemoryUncertainRecoveryRulesTest.DRAIN, null, "drain", "Succeeded", null));
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Unknown"); state.setActiveTaskUuid(old.getUuid()); state.setControlOperationUuid(old.getUuid());
            state.setState("{\"bootId\":\"" + MemoryUncertainRecoveryRulesTest.BOOT + "\"}");
            state.setLastSampleTime(System.currentTimeMillis()); return null;
        });
        MemoryActionRequest msg = request(); msg.setScope("Host"); msg.setResourceUuid("host1");
        msg.setAction("recoverUncertain"); msg.setPolicy("{}");
        msg.setExpectedControlOperationUuid(MemoryUncertainRecoveryRulesTest.OLD);
        msg.setRecovery(MemoryUncertainRecoveryRulesTest.requestDto()); return msg;
    }

    /** The submitted task, or the first step under a submitted chain root. */
    private MemoryTaskVO claimChild(String submitted) {
        return claimAll().stream().filter(t -> submitted.equals(t.getUuid())
                || submitted.equals(t.getParentUuid())).findFirst().get();
    }

    @Test public void hostStateProjectionStoresThePublicStateForQueries() {
        long now = System.currentTimeMillis();
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Succeeded"); state.setActiveTaskUuid(null); state.setLastSampleTime(now - 1000);
            state.setPolicyPlanStatus("Blocked"); state.setReason("NATIVE_POOL_UNQUALIFIED");
            return null;
        });

        repository.refreshHostStates(Collections.singletonList("host1"), now);

        HostMemoryStateVO stored = repository.transaction(em -> em.find(HostMemoryStateVO.class, "host1"));
        MemoryStateInventory expected = MemoryMetricsPresentation.inventory(host1State(), null, now,
                repository.canResumeControl("host1", host1State().getControlOperationUuid()),
                repository.uncertainRecoveryRequest("host1"));
        assertEquals("Blocked", stored.getStatus());
        assertEquals(expected.getStatus(), stored.getStatus());
        assertEquals(expected.getFeatureState(), stored.getFeatureState());
        assertEquals(expected.getReason(), stored.getReason());
        assertEquals(org.zstack.utils.gson.JSONObjectUtil.toJsonString(expected.getAllowedActions()), stored.getAllowedActions());
        assertEquals(Long.valueOf(now - 1000), stored.getLastObservedAt());

        repository.transaction(em -> {
            em.createNativeQuery("insert into ResourceVO (uuid, resourceType) values ('job1', 'LongJobVO')").executeUpdate();
            em.createNativeQuery("insert into LongJobVO (uuid, state, userSuspended) values ('job1', 'Running', false)")
                    .executeUpdate();
            return null;
        });
        repository.setActiveJob("host1", "job1", null);
        repository.refreshHostStates(Collections.singletonList("host1"), now);
        assertEquals("a refresh keeps the running LongJob", "job1",
                repository.transaction(em -> em.find(HostMemoryStateVO.class, "host1")).getActiveJobUuid());
        repository.setActiveJob("host1", null, "job1");
        assertNull(repository.transaction(em -> em.find(HostMemoryStateVO.class, "host1")).getActiveJobUuid());
        repository.setActiveJob("host1", "job1", null);
        repository.transaction(em -> { em.find(LongJobVO.class, "job1").setState(LongJobState.Failed); return null; });
        repository.refreshHostStates(Collections.singletonList("host1"), now);
        assertNull("a LongJob failed by a takeover no longer shows as active",
                repository.transaction(em -> em.find(HostMemoryStateVO.class, "host1")).getActiveJobUuid());

        repository.transaction(em -> { em.remove(em.find(MemoryStateVO.class, "host1")); return null; });
        repository.refreshHostStates(Collections.singletonList("host1"), now);
        assertNull("a Host without state has no public state",
                repository.transaction(em -> em.find(HostMemoryStateVO.class, "host1")));
    }

    private MemoryStateVO host1State() { return repository.states(Collections.singletonList("host1"), 0, 1).get(0); }

    private MemoryTaskVO testTask(String uuid, String parent, String action, String status, String reconcile) {
        MemoryTaskVO task = new MemoryTaskVO(); task.setUuid(uuid); task.setParentUuid(parent);
        task.setHostUuid("host1"); task.setScope("Host"); task.setResourceUuid("host1");
        task.setActorUuid("actor"); task.setAction(action); task.setStatus(status);
        task.setPolicy("{}"); task.setDesiredRevision(0); task.setReconcileOperationUuid(reconcile);
        return task;
    }

    @Test public void clusterHasIndependentPolicyAndHostInheritsFieldwise() {
        repository.transaction(em -> {
            MemoryPolicyVO global = em.find(MemoryPolicyVO.class, "Global:global");
            global.setPolicy("{\"ksm\":{\"enabled\":true,\"zeroPagesEnabled\":false,\"pagesToScan\":2000}}");
            MemoryPolicyVO cluster = new MemoryPolicyVO();
            cluster.setUuid("Cluster:cluster1"); cluster.setScope("Cluster"); cluster.setResourceUuid("cluster1");
            cluster.setPolicy("{\"ksm\":{\"zeroPagesEnabled\":true}}");
            em.persist(cluster);
            MemoryPolicyVO host = new MemoryPolicyVO();
            host.setUuid("Host:host1"); host.setScope("Host"); host.setResourceUuid("host1");
            host.setPolicy("{\"ksm\":{\"enabled\":false}}"); em.persist(host);
            return null;
        });
        MemoryPolicyConfig config = MemoryPolicyRules.decode(effectivePolicy("Host", "host1"));
        assertEquals(Boolean.FALSE, config.ksm.enabled);
        assertEquals(Boolean.TRUE, config.ksm.zeroPagesEnabled);
        assertEquals(Long.valueOf(2000), config.ksm.pagesToScan);
        assertEquals(Arrays.asList("host1", "host2"), repository.targets("Cluster", "cluster1"));
    }

    @Test public void coldKsmAdmissionIsDurableForEveryConfigurationLevel() {
        registerStandardResources();
        for (String scope : Arrays.asList("Host", "Cluster", "Global")) {
            repository.transaction(em -> {
                for (String host : Arrays.asList("host1", "host2")) {
                    MemoryStateVO state = em.find(MemoryStateVO.class, host);
                    state.setStatus("Unknown"); state.setLastSampleTime(null);
                    state.setActiveTaskUuid(null); state.setControlOperationUuid(null);
                    state.setAppliedPolicy(null); state.setAppliedPolicyHash(null);
                }
                return null;
            });
            long sample = System.currentTimeMillis();
            for (String host : Arrays.asList("host1", "host2")) {
                MemoryAgentResponse observation = coldKsmPartial(sample, true);
                observation.bootId = "boot-" + host; observation.state.put("bootId", observation.bootId);
                repository.observePartialKsm(host, observation);
            }
            String resource = "Host".equals(scope) ? "host1" : "Cluster".equals(scope) ? "cluster1" : null;
            standardCommit(Collections.singletonList(standardChange(MemoryStandardField.KSM_ENABLED,
                    resource, resource == null ? null : scope + "VO", "true")), null, false);
            List<MemoryTaskVO> tasks = claimAll();
            assertEquals(scope, "Host".equals(scope) ? 1 : 2, tasks.size());
            for (MemoryTaskVO task : tasks) {
                // claim opens a separate EntityManager: these are persisted columns, not transient callbacks.
                assertEquals(scope, task.getScope());
                assertEquals(Boolean.TRUE, task.getRequiresKsmNativePartialFence());
                assertEquals("boot-" + task.getHostUuid(), task.getKsmNativeObservationBootId());
                assertEquals(Long.valueOf(sample), task.getKsmNativeObservationSampleTime());
            }
            repository.transaction(em -> {
                for (String host : Arrays.asList("host1", "host2")) {
                    MemoryStateVO state = em.find(MemoryStateVO.class, host);
                    state.setCapabilities("{}"); state.setState("{}");
                }
                return null;
            });
            for (MemoryTaskVO task : tasks) {
                MemoryTaskVO reloaded = repository.findTask(task.getUuid());
                assertEquals(Boolean.TRUE, reloaded.getRequiresKsmNativePartialFence());
                assertEquals(Long.valueOf(sample), reloaded.getKsmNativeObservationSampleTime());
            }
            // Leave no earlier in-flight task for the next scope's independent admission.
            repository.transaction(em -> { em.createQuery("delete from MemoryTaskVO").executeUpdate();
                em.createQuery("delete from ResourceConfigVO").executeUpdate(); return null; });
        }
    }
}
