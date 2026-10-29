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
import org.zstack.header.host.HostCanonicalEvents;
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
import org.zstack.utils.gson.JSONObjectUtil;
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
    private static final String BACKEND = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    private SessionFactory factory;
    private MemoryRepository repository;
    private final MemoryStandardConfigAdapter standardConfigAdapter = new MemoryStandardConfigAdapter();
    private Field registryField;
    private Object encryptAspect;
    private Object previousRegistry;
    private Class<?> previousResourceBaseType;
    private final List<Object[]> firedEvents = new ArrayList<>();

    @Before public void setup() throws Exception {
        previousResourceBaseType = org.zstack.header.vo.ResourceTypeMetadata.concreteBaseTypeMapping.get(ResourceVO.class);
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
        factory = configuration.addAnnotatedClass(MemoryTaskVO.class)
                .addAnnotatedClass(MemoryMigrationVO.class)
                .addAnnotatedClass(MemoryStateVO.class)
                .addAnnotatedClass(HostMemoryStateVO.class).addAnnotatedClass(LongJobVO.class)
                .addAnnotatedClass(TestHost.class).addAnnotatedClass(TestVm.class).addAnnotatedClass(TestCluster.class)
                .addAnnotatedClass(ResourceVO.class)
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
            @Override protected <T> T transaction(Function<EntityManager, T> function) {
                EntityManager existing = activeEm.get();
                if (existing != null) { return function.apply(existing); }
                EntityManager em = factory.createEntityManager();
                boolean synchronize = !org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive();
                try {
                    em.getTransaction().begin(); activeEm.set(em);
                    if (synchronize) { org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization(); }
                    T result = function.apply(em);
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
        repository.transaction(em -> {
            TestCluster cluster = new TestCluster(); cluster.uuid = "cluster1";
            cluster.hypervisorType = "KVM"; em.persist(cluster);
            for (String id : Arrays.asList("host1", "host2")) {
                TestHost host = new TestHost(); host.uuid = id; host.hypervisorType = "KVM";
                host.clusterUuid = "cluster1"; host.zoneUuid = id.equals("host1") ? "zone1" : "zone2";
                host.status = HostStatus.Connected; em.persist(host);
            }
            TestVm vm = new TestVm(); vm.uuid = "vm1"; vm.hostUuid = "host1"; em.persist(vm);
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

    private void setGlobal(MemoryStandardField field, String value) {
        repository.transaction(em -> {
            GlobalConfigVO row = em.createQuery("select g from GlobalConfigVO g where g.category = :c and g.name = :n",
                    GlobalConfigVO.class).setParameter("c", field.category()).setParameter("n", field.configName())
                    .getSingleResult();
            row.setValue(value); return null;
        });
    }

    private void setOverride(String resource, String type, MemoryStandardField field, Object value) {
        repository.transaction(em -> {
            standardConfigAdapter.writeOverrides(em, resource, type, Collections.singletonMap(field.path(), value));
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> desired(String host) {
        return JSONObjectUtil.toObject(repository.state(host).getDesired(), LinkedHashMap.class);
    }

    private static Object field(Map<String, Object> desired, String section, String key) {
        return ((Map<?, ?>) desired.get(section)).get(key);
    }

    private static MemoryAgentResponse reply(String status, String reason, Long retryAfter) {
        MemoryAgentResponse response = new MemoryAgentResponse();
        response.setSuccess(!"Failed".equals(status));
        response.status = status; response.reasonCode = reason; response.retryAfterSeconds = retryAfter;
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("sampleTime", System.currentTimeMillis()); state.put("control", "active");
        response.state = state;
        return response;
    }

    private MemoryTaskVO task(String uuid) { return repository.tasks(Collections.singleton(uuid)).get(uuid); }

    @Test public void desiredUsesTheMostSpecificStandardValue() {
        setGlobal(MemoryStandardField.ZRAM_ENABLED, "true");
        setGlobal(MemoryStandardField.KSM_ENABLED, "true");
        setOverride("cluster1", "ClusterVO", MemoryStandardField.ZRAM_LOGICAL_CAPACITY, 1L << 30);
        setOverride("host1", "HostVO", MemoryStandardField.KSM_ENABLED, false);
        setOverride("host1", "HostVO", MemoryStandardField.ZRAM_LOGICAL_CAPACITY, 2L << 30);
        repository.plan("host1", null, "connect", "test", true, false);
        repository.plan("host2", null, "connect", "test", true, false);
        Map<String, Object> host1 = desired("host1"), host2 = desired("host2");
        assertEquals("active", host1.get("control"));
        assertEquals(false, field(host1, "ksm", "enabled"));
        assertEquals(true, field(host2, "ksm", "enabled"));
        assertEquals(2.0 * (1L << 30), ((Number) field(host1, "zram", "logicalCapacityBytes")).doubleValue(), 0);
        assertEquals(1.0 * (1L << 30), ((Number) field(host2, "zram", "logicalCapacityBytes")).doubleValue(), 0);
        assertEquals(true, field(host1, "zram", "enabled"));
        // none leaves the setting to the Host.
        setOverride("host1", "HostVO", MemoryStandardField.ZRAM_ENABLED, true);
        repository.transaction(em -> em.createQuery("update ResourceConfigVO r set r.value = 'none' where r.resourceUuid = 'host1'"
                + " and r.name = :name").setParameter("name", MemoryStandardField.ZRAM_ENABLED.configName()).executeUpdate());
        repository.plan("host1", null, "configure", "test", true, false);
        assertNull(field(desired("host1"), "zram", "enabled"));
    }

    @Test public void writebackWaitsForAPreparedBackendAndLicenseOnlyGatesOptimization() {
        setGlobal(MemoryStandardField.WRITEBACK_ENABLED, "true");
        setGlobal(MemoryStandardField.ZRAM_ENABLED, "true");
        setGlobal(MemoryStandardField.KSM_ENABLED, "true");
        repository.plan("host1", null, "connect", "test", true, false);
        assertEquals(false, field(desired("host1"), "writeback", "enabled"));
        repository.recordPreparedBackend("host1", BACKEND, 1L << 30);
        repository.plan("host1", null, "prepareWritebackBackend", "test", true, false);
        assertEquals(true, field(desired("host1"), "writeback", "enabled"));
        // Writeback runs inside ZRAM: turning ZRAM off turns it off too.
        setOverride("host1", "HostVO", MemoryStandardField.ZRAM_ENABLED, false);
        repository.plan("host1", null, "configure", "test", true, false);
        assertEquals(false, field(desired("host1"), "zram", "enabled"));
        assertEquals(false, field(desired("host1"), "writeback", "enabled"));
        repository.plan("host2", null, "connect", "test", false, false);
        assertEquals(true, field(desired("host2"), "ksm", "enabled"));
        assertTrue(((Map<?, ?>) desired("host2").get("zram")).isEmpty());
        assertTrue(((Map<?, ?>) desired("host2").get("writeback")).isEmpty());
    }

    @Test public void onlyAChangedConfigurationStartsAGenerationAndSupersedesTheOldOne() {
        MemoryRepository.Planned first = repository.plan("host1", "paused", "pause", "test", true, true);
        assertEquals(1, first.generation);
        assertEquals(1, repository.plan("host1", null, "connect", "test", true, false).generation);
        assertNull(repository.plan("host1", null, "connect", "test", true, false).task);
        MemoryRepository.Planned second = repository.plan("host1", "active", "resume", "test", true, true);
        assertEquals(2, second.generation);
        assertEquals("Superseded", task(first.task).getStatus());
        assertEquals("Converging", task(second.task).getStatus());
        MemoryRepository.Delivery delivery = repository.pending("host1");
        assertEquals(2, delivery.generation);
        assertEquals("active", delivery.desired.get("control"));
    }

    @Test public void appliedGenerationFinishesItsOperationAndIgnoresAStaleResult() {
        MemoryRepository.Planned pause = repository.plan("host1", "paused", "pause", "test", true, true);
        assertNull(repository.record("host1", 7, reply("Succeeded", null, null)));
        assertEquals("Converging", repository.state("host1").getStatus());
        MemoryRepository.Outcome outcome = repository.record("host1", pause.generation, reply("Succeeded", null, null));
        assertEquals("Succeeded", outcome.status);
        assertEquals(Long.valueOf(pause.generation), repository.state("host1").getAppliedRevision());
        assertEquals("Succeeded", task(pause.task).getStatus());
        assertNull(repository.pending("host1"));
        HostMemoryStateVO row = repository.transaction(em -> em.find(HostMemoryStateVO.class, "host1"));
        assertEquals("Applied", row.getStatus());
        assertTrue(row.getAllowedActions().contains("resume"));
        // Asking again resends the same generation, and finishes when the Host confirms it.
        MemoryRepository.Planned again = repository.plan("host1", "paused", "pause", "test", true, true);
        assertEquals(pause.generation, again.generation);
        assertEquals("Converging", task(again.task).getStatus());
        assertEquals(pause.generation, repository.pending("host1").generation);
        repository.record("host1", pause.generation, reply("Succeeded", null, null));
        assertEquals("Succeeded", task(again.task).getStatus());
    }

    @Test public void convergingIsResentAfterTheAgentDelay() {
        MemoryRepository.Planned drain = repository.plan("host1", "drained", "drain", "test", true, true);
        long before = System.currentTimeMillis();
        MemoryRepository.Outcome outcome = repository.record("host1", drain.generation, reply("Converging", "DRAINING", 30L));
        assertEquals("Converging", outcome.status);
        MemoryStateVO state = repository.state("host1");
        assertEquals("DRAINING", state.getReason());
        assertEquals(0, state.getFailures());
        assertTrue(state.getNextAttemptAt() >= before + 30000);
        assertTrue(repository.dueHosts(before).isEmpty());
        assertEquals(Collections.singletonList("host1"), repository.dueHosts(before + 31000));
        assertEquals("Converging", task(drain.task).getStatus());
        assertEquals("Converging",
                repository.transaction(em -> em.find(HostMemoryStateVO.class, "host1")).getStatus());
    }

    @Test public void failuresBackOffAndBecomeFailedAfterFive() {
        MemoryRepository.Planned pause = repository.plan("host1", "paused", "pause", "test", true, true);
        long[] delays = {30000, 60000, 120000, 240000};
        for (int i = 0; i < 4; i++) {
            long before = System.currentTimeMillis();
            MemoryRepository.Outcome outcome = i % 2 == 0 ? repository.record("host1", pause.generation, null)
                    : repository.record("host1", pause.generation, reply("Failed", "KSM_NATIVE_PROFILE_UNSUPPORTED", null));
            assertEquals("Converging", outcome.status);
            long next = repository.state("host1").getNextAttemptAt();
            assertTrue(next >= before + delays[i] && next <= System.currentTimeMillis() + delays[i]);
        }
        assertTrue(firedEvents.isEmpty());
        MemoryRepository.Outcome failed = repository.record("host1", pause.generation, null);
        assertEquals("Failed", failed.status);
        assertEquals("AGENT_UNREACHABLE", failed.reason);
        assertEquals("Failed", task(pause.task).getStatus());
        assertEquals(1, firedEvents.size());
        assertEquals(MemoryTaskFailureEvent.PATH, firedEvents.get(0)[0]);
        assertNull(repository.pending("host1"));
        // An operator asking again, or a reconnect, retries the generation.
        repository.resend("host1");
        assertEquals("Converging", repository.state("host1").getStatus());
        assertEquals(0, repository.state("host1").getFailures());
        assertNotNull(repository.pending("host1"));
    }

    @Test public void aHostAheadOfTheRecordedGenerationIsSentAHigherOne() {
        MemoryRepository.Planned pause = repository.plan("host1", "paused", "pause", "test", true, true);
        MemoryAgentResponse stale = reply("Failed", "STALE_GENERATION", null);
        stale.appliedGeneration = 7L;
        MemoryRepository.Outcome outcome = repository.record("host1", pause.generation, stale);
        assertEquals("Converging", outcome.status);
        assertEquals(0, repository.state("host1").getFailures());
        MemoryRepository.Delivery delivery = repository.pending("host1");
        assertEquals(8, delivery.generation);
        assertEquals("paused", delivery.desired.get("control"));
        repository.record("host1", 8, reply("Succeeded", null, null));
        assertEquals("the waiting operation finishes with the later generation", "Succeeded", task(pause.task).getStatus());
    }

    @Test public void retryDelayIsCappedAtTenMinutes() {
        assertEquals(30000, MemoryRepository.retryDelay(1));
        assertEquals(480000, MemoryRepository.retryDelay(5));
        assertEquals(600000, MemoryRepository.retryDelay(6));
        assertEquals(600000, MemoryRepository.retryDelay(40));
    }

    @Test public void observationResendsWhenTheHostNoLongerReportsTheGeneration() {
        MemoryRepository.Planned pause = repository.plan("host1", "paused", "pause", "test", true, true);
        MemoryAgentResponse applied = reply("Succeeded", null, null);
        applied.state.put("appliedGeneration", pause.generation);
        applied.state.put("control", "paused");
        repository.record("host1", pause.generation, applied);
        repository.observe("host1", applied);
        assertEquals("Succeeded", repository.state("host1").getStatus());
        MemoryAgentResponse reinstalled = reply("Succeeded", null, null);
        reinstalled.state.put("control", "paused");
        repository.observe("host1", reinstalled);
        assertEquals("Converging", repository.state("host1").getStatus());
        assertNotNull(repository.pending("host1"));
    }

    @Test public void aHostWhoseWantedKsmtunedFailedIsSentItsGenerationAgain() {
        setGlobal(MemoryStandardField.KSM_ENABLED, "true");
        MemoryRepository.Planned plan = repository.plan("host1", null, "connect", "test", true, true);
        MemoryAgentResponse applied = reply("Succeeded", null, null);
        applied.state.put("appliedGeneration", plan.generation);
        Map<String, Object> ksm = new LinkedHashMap<>();
        ksm.put("enabled", true); ksm.put("serviceState", "active"); ksm.put("tunedServiceState", "active");
        applied.state.put("ksm", ksm);
        repository.record("host1", plan.generation, applied);
        repository.observe("host1", applied);
        assertEquals("Succeeded", repository.state("host1").getStatus());
        // ksm still merges, but its tuning daemon died: restart it like a stopped ZRAM service.
        ksm.put("enabled", null); ksm.put("tunedServiceState", "failed");
        repository.observe("host1", applied);
        assertEquals("Converging", repository.state("host1").getStatus());
        assertEquals("HOST_DRIFTED", repository.state("host1").getReason());
    }

    @Test public void aServiceStartedAgainByAnApplyIsReportedRecovered() {
        setGlobal(MemoryStandardField.KSM_ENABLED, "true");
        MemoryRepository.Planned plan = repository.plan("host1", null, "connect", "test", true, true);
        MemoryAgentResponse running = ksmSample(plan.generation, "active");
        repository.record("host1", plan.generation, running);
        repository.observe("host1", running);
        firedEvents.clear();
        // An observation of the failure and the reply of the resend that started ksmtuned again,
        // each compared with the sample it replaces.
        repository.observe("host1", ksmSample(plan.generation, "failed"));
        repository.record("host1", plan.generation, ksmSample(plan.generation, "active"));
        assertEquals(Arrays.asList("Failed", "Recovered"), serviceEventStatuses());
        // A reply that is not stored, such as one for an older generation, reports nothing.
        repository.record("host1", plan.generation - 1, ksmSample(plan.generation, "failed"));
        assertEquals(Arrays.asList("Failed", "Recovered"), serviceEventStatuses());
    }

    private static MemoryAgentResponse ksmSample(long generation, String tunedState) {
        MemoryAgentResponse response = reply("Succeeded", null, null);
        response.state.put("appliedGeneration", generation);
        Map<String, Object> ksm = new LinkedHashMap<>();
        ksm.put("enabled", "active".equals(tunedState) ? Boolean.TRUE : null);
        ksm.put("serviceState", "active"); ksm.put("tunedServiceState", tunedState);
        response.state.put("ksm", ksm);
        return response;
    }

    private List<String> serviceEventStatuses() {
        List<String> statuses = new ArrayList<>();
        for (Object[] fired : firedEvents) {
            if (fired[1] instanceof HostCanonicalEvents.HostMemoryOptimizationServiceStateChangedData) {
                statuses.add(((HostCanonicalEvents.HostMemoryOptimizationServiceStateChangedData) fired[1]).getStatus());
            }
        }
        return statuses;
    }

    @Test public void aHostWithoutKsmIsNotSentKsmAgainAndAgain() {
        setGlobal(MemoryStandardField.KSM_ENABLED, "true");
        MemoryRepository.Planned plan = repository.plan("host1", null, "connect", "test", true, true);
        MemoryAgentResponse applied = reply("Succeeded", null, null);
        applied.state.put("appliedGeneration", plan.generation);
        applied.state.put("capabilities", new LinkedHashMap<>(Collections.singletonMap("ksm", false)));
        applied.state.put("ksm", new LinkedHashMap<>(Collections.singletonMap("serviceState", "unknown")));
        repository.record("host1", plan.generation, applied);
        repository.observe("host1", applied);
        assertEquals("Succeeded", repository.state("host1").getStatus());
    }

    @Test public void aHostThatStoppedItsWantedZramIsSentItsGenerationAgain() {
        setGlobal(MemoryStandardField.ZRAM_ENABLED, "true");
        MemoryRepository.Planned plan = repository.plan("host1", null, "connect", "test", true, true);
        MemoryAgentResponse applied = reply("Succeeded", null, null);
        applied.state.put("appliedGeneration", plan.generation);
        applied.state.put("zram", new LinkedHashMap<>(Collections.singletonMap("serviceState", "active")));
        repository.record("host1", plan.generation, applied);
        repository.observe("host1", applied);
        assertEquals("Succeeded", repository.state("host1").getStatus());
        MemoryAgentResponse stopped = reply("Succeeded", null, null);
        stopped.state.put("appliedGeneration", plan.generation);
        stopped.state.put("control", "stopped");
        stopped.state.put("zram", new LinkedHashMap<>(Collections.singletonMap("serviceState", "inactive")));
        stopped.state.put("ksm", new LinkedHashMap<>(Collections.singletonMap("enabled", false)));
        repository.observe("host1", stopped);
        assertEquals("Converging", repository.state("host1").getStatus());
        assertEquals("HOST_DRIFTED", repository.state("host1").getReason());
        assertNotNull(repository.pending("host1"));
        HostMemoryStateVO row = repository.transaction(em -> em.find(HostMemoryStateVO.class, "host1"));
        assertEquals("a stopped wanted ZRAM is not reported as Disabled", "Unknown", row.getFeatureState());
        assertEquals("Converging", row.getStatus());
    }

    @Test public void aHostThatCannotRunTheWantedZramIsAppliedWithTheReason() {
        setGlobal(MemoryStandardField.ZRAM_ENABLED, "true");
        MemoryRepository.Planned plan = repository.plan("host1", null, "connect", "test", true, true);
        MemoryAgentResponse applied = reply("Succeeded", null, null);
        applied.state.put("appliedGeneration", plan.generation);
        Map<String, Object> capabilities = new LinkedHashMap<>();
        capabilities.put("supported", true); capabilities.put("ksm", true); capabilities.put("zram", false);
        capabilities.put("zramReasonCode", "UNSUPPORTED_CGROUP_MEMORY");
        applied.state.put("capabilities", capabilities);
        applied.state.put("ksm", new LinkedHashMap<>(Collections.singletonMap("enabled", true)));
        repository.record("host1", plan.generation, applied);
        repository.observe("host1", applied);
        // No service runs on such a Host; that is not drift to resend every observation.
        assertEquals("Succeeded", repository.state("host1").getStatus());
        HostMemoryStateVO row = repository.transaction(em -> em.find(HostMemoryStateVO.class, "host1"));
        assertEquals("ZRAM_UNSUPPORTED: UNSUPPORTED_CGROUP_MEMORY", row.getReason());
        assertEquals("Active", row.getFeatureState());
        capabilities.remove("zramReasonCode");
        repository.observe("host1", applied);
        row = repository.transaction(em -> em.find(HostMemoryStateVO.class, "host1"));
        assertEquals("ZRAM_UNSUPPORTED", row.getReason());
    }

    @Test public void aHostStillReclaimingAfterZramIsTurnedOffIsSentItsGenerationAgain() {
        setGlobal(MemoryStandardField.ZRAM_ENABLED, "false");
        MemoryRepository.Planned plan = repository.plan("host1", null, "connect", "test", true, true);
        MemoryAgentResponse applied = reply("Succeeded", null, null);
        applied.state.put("appliedGeneration", plan.generation);
        Map<String, Object> zram = new LinkedHashMap<>();
        zram.put("serviceState", "active"); zram.put("efficiencyEnabled", false);
        applied.state.put("zram", zram);
        repository.record("host1", plan.generation, applied);
        repository.observe("host1", applied);
        assertEquals("Succeeded", repository.state("host1").getStatus());
        zram.put("efficiencyEnabled", true);
        repository.observe("host1", applied);
        assertEquals("HOST_DRIFTED", repository.state("host1").getReason());
    }

    @Test public void drainedHostIsReported() {
        repository.plan("host1", "drained", "drain", "test", true, true);
        assertTrue(repository.isDrained("host1"));
        assertFalse(repository.isDrained("host2"));
    }

    @Test public void summaryTargetsIntersectZoneAndHostSelection() {
        assertEquals(Arrays.asList("host1", "host2"), repository.summaryTargets(null, null));
        assertEquals(Collections.singletonList("host1"), repository.summaryTargets(null, "zone1"));
        assertEquals(Collections.singletonList("host2"), repository.summaryTargets(Arrays.asList("host2", "host2"), null));
        assertTrue(repository.summaryTargets(Collections.singletonList("host2"), "zone1").isEmpty());
        assertTrue(repository.summaryTargets(Collections.emptyList(), null).isEmpty());
        try {
            repository.summaryTargets(Arrays.asList("host1", "absent"), "zone1");
            fail("an invalid Host selection must not become zero coverage");
        } catch (MemoryOperationException expected) {
            assertEquals("MEMORY_INVALID_SCOPE", expected.getCode());
        }
    }

    @Test public void theLegacyKsmSwitchStaysSettableOnANonKvmHost() {
        repository.transaction(em -> {
            TestHost host = new TestHost(); host.uuid = "esx1"; host.hypervisorType = "ESX"; host.status = HostStatus.Connected;
            em.persist(host);
            em.persist(new ResourceVO(new Object[]{"esx1", "esx1", "HostVO"}));
            em.flush();
            return null;
        });
        // 5.5.38 accepted host.ksm on any Host; only the memory optimization settings are KVM-only.
        repository.validateStandardChange(MemoryStandardField.KSM_ENABLED, "esx1", "true");
        assertTrue(repository.standardConfigurationTargets("esx1").isEmpty());
        try {
            repository.validateStandardChange(MemoryStandardField.ZRAM_ENABLED, "esx1", "true");
            fail("ZRAM is a KVM Host setting");
        } catch (MemoryOperationException expected) {
            assertEquals("MEMORY_INVALID_SCOPE", expected.getCode());
        }
    }

    @Test public void standardChangesAreScopeCheckedAndTargetTheirHosts() {
        repository.transaction(em -> {
            em.persist(new ResourceVO(new Object[]{"host1", "host1", "HostVO"}));
            em.persist(new ResourceVO(new Object[]{"cluster1", "cluster1", "ClusterVO"}));
            em.flush();
            // The fixture's resource type mapping persists every ResourceVO as a Host.
            em.createQuery("update ResourceVO r set r.resourceType = 'ClusterVO' where r.uuid = 'cluster1'").executeUpdate();
            return null;
        });
        assertEquals(Arrays.asList("host1", "host2"), repository.standardConfigurationTargets(null));
        assertEquals(Arrays.asList("host1", "host2"), repository.standardConfigurationTargets("cluster1"));
        assertEquals(Collections.singletonList("host1"), repository.standardConfigurationTargets("host1"));
        repository.validateStandardChange(MemoryStandardField.WRITEBACK_BACKEND_RESOURCE, "host1", BACKEND);
        try {
            repository.validateStandardChange(MemoryStandardField.WRITEBACK_BACKEND_RESOURCE, "cluster1", BACKEND);
            fail("a Host-only field cannot be set on a Cluster");
        } catch (MemoryOperationException expected) {
            assertEquals("MEMORY_INVALID_SCOPE", expected.getCode());
        }
    }

    @Test public void deletedHostRecordsAreRemoved() {
        repository.plan("host2", "paused", "pause", "test", true, true);
        repository.transaction(em -> { em.remove(em.find(TestHost.class, "host2")); return null; });
        repository.cleanupDeletedResources();
        assertNull(repository.state("host2"));
        assertTrue(repository.transaction(em -> em.createQuery("select t from MemoryTaskVO t", MemoryTaskVO.class)
                .getResultList()).isEmpty());
        assertNull(repository.transaction(em -> em.find(HostMemoryStateVO.class, "host2")));
    }

    @Test public void finishedOperationsArePurgedAfterAWeek() {
        MemoryRepository.Planned first = repository.plan("host1", "paused", "pause", "test", true, true);
        MemoryRepository.Planned second = repository.plan("host1", "active", "resume", "test", true, true);
        repository.purgeHistory(System.currentTimeMillis() + MemoryRepository.HISTORY_MILLIS + 1000);
        assertNull(task(first.task));
        assertNotNull(task(second.task));
    }

    private String standardDefault(MemoryStandardField field) {
        if (field.isFeatureSwitch()) { return "none"; }
        return "";
    }
}
