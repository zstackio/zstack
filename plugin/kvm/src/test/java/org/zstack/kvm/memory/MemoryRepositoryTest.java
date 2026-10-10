package org.zstack.kvm.memory;

import org.zstack.core.Platform;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.host.HostStatus;
import javax.persistence.*;
import java.util.*;
import java.util.function.Function;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfigVO;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.config.ConfigMutation;
import org.zstack.core.config.ConfigMutationContext;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigApiInterceptor;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.resourceconfig.ResourceConfigTransactionalMutationExtensionPoint;
import org.zstack.resourceconfig.APIUpdateResourceConfigsMsg;
import org.zstack.resourceconfig.APIUpdateResourceConfigMsg;
import org.zstack.resourceconfig.ResourceConfigFacadeImpl;
import org.zstack.kvm.KVMHostFactory;
import org.zstack.core.cloudbus.CloudBus;
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
        try { configuration.addAnnotatedClass(Class.forName("org.zstack.kvm.memory.MemoryVmExclusionVO")); }
        catch (ClassNotFoundException ignored) { }
        factory = configuration.addAnnotatedClass(MemoryPolicyVO.class)
                .addAnnotatedClass(MemoryQueryVersionVO.class)
                .addAnnotatedClass(MemoryTargetShardVO.class)
                .addAnnotatedClass(MemoryTaskVO.class).addAnnotatedClass(MemoryTaskIdempotencyReceiptVO.class)
                .addAnnotatedClass(MemoryTaskFailureOutboxVO.class)
                .addAnnotatedClass(MemoryCloudBootstrapVO.class)
                .addAnnotatedClass(MemoryMigrationVO.class)
                .addAnnotatedClass(MemoryStandardConfigMigrationVO.class)
                .addAnnotatedClass(MemoryStateVO.class)
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
                (proxy, method, args) -> null));
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

    @Test public void policyReadInfersScopeFromRealResourceRegistryWithoutWritingPolicy() {
        registerPolicyReadResource("host1", "HostVO");
        registerPolicyReadResource("cluster1", "ClusterVO");
        registerPolicyReadResource("vm1", "VmInstanceVO");
        long policiesBefore = repository.transaction(em -> em.createQuery(
                "select count(p) from MemoryPolicyVO p", Long.class).getSingleResult());
        for (String[] resource : new String[][]{{"global", "Global"}, {"host1", "Host"},
                {"cluster1", "Cluster"}, {"vm1", "VM"}}) {
            MemoryPolicyInventory inferred = repository.getPolicy(null, resource[0]);
            MemoryPolicyInventory compatible = repository.getPolicy(resource[1], resource[0]);
            assertEquals(resource[1], inferred.getScope());
            assertEquals(resource[0], inferred.getResourceUuid());
            assertEquals(compatible.getEffectivePolicy(), inferred.getEffectivePolicy());
            assertEquals(compatible.getSourceRevisions(), inferred.getSourceRevisions());
        }
        assertEquals(policiesBefore, (long) repository.transaction(em -> em.createQuery(
                "select count(p) from MemoryPolicyVO p", Long.class).getSingleResult()));
        assertEquals(0L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t", Long.class).getSingleResult()));
    }

    @Test public void policyReadRejectsConflictingCompatibilityScopeAndResource() {
        for (String[] request : new String[][]{{"Cluster", "host1"}, {"Host", "cluster1"},
                {"VM", "host1"}, {"Global", "host1"}, {"Host", "global"}, {"", "host1"}}) {
            try {
                repository.getPolicy(request[0], request[1]);
                fail("scope and resource mismatch must be rejected");
            } catch (MemoryOperationException expected) {
                assertEquals("MEMORY_INVALID_SCOPE", expected.getCode());
            }
        }
    }

    @Test public void inferredPolicyReadRejectsUnknownUnsupportedAndNonKvmResources() {
        registerPolicyReadResource("volume1", "VolumeVO");
        registerPolicyReadResource("stale-host", "HostVO");
        registerPolicyReadResource("host2", "HostVO");
        repository.transaction(em -> {
            em.find(TestHost.class, "host2").hypervisorType = "ESX"; return null;
        });
        for (String uuid : Arrays.asList("unknown", "volume1", "stale-host", "host2")) {
            try {
                repository.getPolicy(null, uuid);
                fail("unsupported or missing resource must not become a Global policy");
            } catch (MemoryOperationException expected) {
                assertEquals("MEMORY_INVALID_SCOPE", expected.getCode());
            }
        }
    }

    @Test public void previewInfersScopeAndRejectsConflictsWithoutPersistingConfiguration() {
        registerPolicyReadResource("host1", "HostVO");
        registerPolicyReadResource("cluster1", "ClusterVO");
        registerPolicyReadResource("vm1", "VmInstanceVO");
        for (String[] resource : new String[][]{{"global", "Global"}, {"host1", "Host"},
                {"cluster1", "Cluster"}, {"vm1", "VM"}}) {
            String patch = "VM".equals(resource[1]) ? "{\"participation\":\"deny\"}" : "{\"ksm\":{\"enabled\":true}}";
            MemoryPolicyInventory inferred = repository.preview(null, resource[0], patch);
            MemoryPolicyInventory compatible = repository.preview(resource[1], resource[0], patch);
            assertEquals(resource[1], inferred.getScope());
            assertEquals(compatible.getEffectivePolicy(), inferred.getEffectivePolicy());
        }
        for (String[] request : new String[][]{{"Cluster", "host1"}, {"Global", "host1"},
                {null, "unknown"}, {"", "host1"}}) {
            try {
                repository.preview(request[0], request[1], "{}"); fail("invalid preview resource");
            } catch (MemoryOperationException expected) { assertEquals("MEMORY_INVALID_SCOPE", expected.getCode()); }
        }
        assertEquals(0L, repository.getPolicy(null, "global").getRevision());
        assertEquals(0L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t", Long.class).getSingleResult()));
    }

    @Test public void updateInfersScopeBeforeActionRulesAndNormalizesIdempotencyIdentity() {
        registerPolicyReadResource("host1", "HostVO");
        APIUpdateMemoryPolicyMsg global = request(0); global.setScope(null);
        MemoryTaskInventory first = repository.submit(global);
        assertEquals("Global", global.getScope());
        assertEquals("Global", first.getScope());
        global.setScope(null);
        assertEquals(first.getUuid(), repository.submit(global).getUuid());
        global.setScope("Global");
        assertEquals(first.getUuid(), repository.submit(global).getUuid());

        // An omitted scope must be resolved before the Host-only maintenance
        // rules; use a rejected shape to prove that rule, not scope, rejects it.
        APIUpdateMemoryPolicyMsg preparation = request(0); preparation.setScope(null);
        preparation.setResourceUuid("host1"); preparation.setTargetHostUuids(null);
        preparation.setAction("prepareWritebackBackend"); preparation.setPolicy("{}");
        try { repository.submit(preparation); fail("missing preparation must be rejected"); }
        catch (MemoryOperationException expected) {
            assertEquals("MEMORY_BACKEND_PREPARATION_INVALID", expected.getCode());
            assertEquals("Host", preparation.getScope());
        }
    }

    @Test public void updateRejectsConflictingOrUnresolvableScopeBeforeAnyNewTask() {
        for (String[] invalid : new String[][]{{"Host", "global"}, {"Cluster", "host1"},
                {"Global", "host1"}, {null, "unknown"}}) {
            APIUpdateMemoryPolicyMsg msg = request(0); msg.setScope(invalid[0]);
            msg.setResourceUuid(invalid[1]); msg.setTargetHostUuids(null);
            try { repository.submit(msg); fail("invalid update resource"); }
            catch (MemoryOperationException expected) { assertEquals("MEMORY_INVALID_SCOPE", expected.getCode()); }
        }
        assertEquals(0L, repository.getPolicy(null, "global").getRevision());
        assertEquals(0L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t", Long.class).getSingleResult()));
    }

    private ConfigMutation standardChange(MemoryStandardField field, String resource, String type, String value) {
        return new ConfigMutation(field.category(), field.configName(), resource, type, value, false);
    }

    /** Domain transaction fixture. Actual standard API/framework wiring is verified separately. */
    private void standardCommit(List<ConfigMutation> changes, ConfigMutationContext context, boolean rollback) {
        repository.transaction(em -> {
            repository.prepareStandardMutations(em, changes, context);
            for (ConfigMutation change : changes) {
                MemoryStandardField field = MemoryStandardField.forConfig(change.getCategory(), change.getName());
                if ("ksm.enabled".equals(field.path()) && !change.isDelete() && "none".equalsIgnoreCase(change.getNewValue())) {
                    persistLegacyKsmValue(em, change.getResourceUuid(), change.getResourceType(), "none");
                } else {
                    Object value = change.isDelete() ? null : MemoryStandardConfigCodec.decode(field.path(), change.getNewValue());
                    if (change.getResourceUuid() == null) { standardConfigAdapter.writeGlobal(em, field.path(), value); }
                    else { standardConfigAdapter.writeOverride(em, change.getResourceUuid(), change.getResourceType(), field.path(), value); }
                }
            }
            repository.completeStandardMutations(em, changes, context);
            if (rollback) { throw new IllegalStateException("forced standard transaction rollback"); }
            return null;
        });
    }

    @Test public void standardManagedHostBatchValidatesFinalCandidateAndCommitsOneRevision() {
        registerStandardResources();
        repository.transaction(em -> {
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "zram.logicalCapacityBytes", 16384L);
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "zram.ramLimitBytes", 8192L);
            em.find(MemoryStateVO.class, "host1").setState("{\"managed\":true}"); return null;
        });
        List<ConfigMutation> changes = Arrays.asList(
                standardChange(MemoryStandardField.ZRAM_LOGICAL_CAPACITY, "host1", "HostVO", "4096"),
                standardChange(MemoryStandardField.ZRAM_RAM_LIMIT, "host1", "HostVO", "4096"));
        standardCommit(changes, ConfigMutationContext.internal(), false);
        assertEquals(1L, repository.getPolicy("Host", "host1").getRevision());
        assertEquals(Long.valueOf(4096), MemoryPolicyRules.decode(repository.getPolicy("Host", "host1")
                .getEffectivePolicy()).zram.ramLimitBytes);
        assertEquals(2L, repository.taskCount(new APIQueryMemoryTaskMsg()));
        assertNotNull(host1State().getActiveTaskUuid());
    }

    @Test public void standardSingleInvalidCandidateAndPostWriteFailureRollbackEverything() {
        registerStandardResources();
        repository.transaction(em -> {
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "zram.logicalCapacityBytes", 16384L);
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "zram.ramLimitBytes", 8192L); return null;
        });
        try {
            standardCommit(Collections.singletonList(standardChange(MemoryStandardField.ZRAM_LOGICAL_CAPACITY,
                    "host1", "HostVO", "4096")), ConfigMutationContext.internal(), false);
            fail("single field must validate the complete candidate");
        } catch (MemoryOperationException expected) { assertEquals("MEMORY_INVALID_POLICY", expected.getCode()); }
        try {
            standardCommit(Collections.singletonList(standardChange(MemoryStandardField.KSM_ENABLED,
                    "host1", "HostVO", "true")), ConfigMutationContext.internal(), true);
            fail("forced rollback");
        } catch (IllegalStateException expected) { }
        assertEquals(0L, repository.getPolicy("Host", "host1").getRevision());
        assertEquals(0L, repository.taskCount(new APIQueryMemoryTaskMsg()));
        assertNull(host1State().getActiveTaskUuid());
        assertNull(repository.transaction(em -> standardConfigAdapter.readOverrideOwn(em, "ksm.enabled", "host1")));
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
                null, null, "true")), ConfigMutationContext.internal(), false);
        assertEquals(1L, repository.getPolicy("Global", "global").getRevision());
        assertEquals(Boolean.TRUE, MemoryPolicyRules.decode(repository.getPolicy("Global", "global").getEffectivePolicy()).ksm.enabled);
        assertEquals("old-unknown", host1State().getActiveTaskUuid());
        assertEquals("old-unknown", host1State().getControlOperationUuid());
        assertEquals("Unknown", host1State().getStatus());
        assertEquals("Blocked", host1State().getPolicyPlanStatus());
        assertTrue(repository.claim("test-worker").isEmpty());
        assertEquals(3L, repository.taskCount(new APIQueryMemoryTaskMsg()));
    }

    @Test public void standardGlobalWithNoHostCanSaveAndHostOnlyFieldCannot() {
        repository.transaction(em -> { em.createQuery("delete from HostVO").executeUpdate(); return null; });
        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.KSM_ENABLED,
                null, null, "true")), ConfigMutationContext.internal(), false);
        assertEquals(1L, repository.getPolicy("Global", "global").getRevision());
        assertEquals(1L, repository.taskCount(new APIQueryMemoryTaskMsg()));
        try {
            standardCommit(Collections.singletonList(standardChange(MemoryStandardField.WRITEBACK_BACKEND_RESOURCE,
                    null, null, "0123456789abcdef0123456789abcdef")), ConfigMutationContext.internal(), false);
            fail("backend identity is Host-only");
        } catch (MemoryOperationException expected) { assertEquals("MEMORY_INVALID_SCOPE", expected.getCode()); }
        assertEquals(1L, repository.getPolicy("Global", "global").getRevision());
    }

    @Test public void standardReplayCannotOverwriteLaterConfiguration() {
        registerStandardResources();
        List<ConfigMutation> changes = Collections.singletonList(standardChange(MemoryStandardField.KSM_ENABLED,
                "host1", "HostVO", "true"));
        ConfigMutationContext context = ConfigMutationContext.internal();
        standardCommit(changes, context, false);
        try { standardCommit(changes, context, false); fail("replay must not apply again"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_IDEMPOTENCY_CONFLICT", expected.getCode()); }
        assertEquals(1L, repository.getPolicy("Host", "host1").getRevision());
        assertEquals(2L, repository.taskCount(new APIQueryMemoryTaskMsg()));
    }

    @Test public void compatibilityAndStandardWritesShareRevisionAndTaskCommit() {
        registerStandardResources();
        APIUpdateMemoryPolicyMsg compatibility = request(0);
        compatibility.setPolicy("{\"ksm\":{\"enabled\":true}}");
        MemoryTaskInventory first = repository.submit(compatibility);
        assertEquals(1L, repository.getPolicy("Global", "global").getRevision());
        assertEquals(first.getUuid(), repository.submit(compatibility).getUuid());
        String active = host1State().getActiveTaskUuid();
        long oldDesired = host1State().getDesiredRevision();

        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.KSM_ENABLED,
                null, null, "false")), ConfigMutationContext.internal(), false);
        assertEquals(2L, repository.getPolicy("Global", "global").getRevision());
        assertEquals(Boolean.FALSE, MemoryPolicyRules.decode(repository.getPolicy("Global", "global")
                .getEffectivePolicy()).ksm.enabled);
        assertEquals("saving later intent must not replace a running executor", active, host1State().getActiveTaskUuid());
        assertEquals(oldDesired, host1State().getDesiredRevision());
        assertEquals("Blocked", host1State().getPolicyPlanStatus());
        APIUpdateMemoryPolicyMsg stale = request(1);
        try { repository.submit(stale); fail("compatibility CAS must observe standard-entry revision"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_REVISION_CONFLICT", expected.getCode()); }
        assertEquals(2L, repository.getPolicy("Global", "global").getRevision());
    }

    @Test public void compatibilityParentWriteValidatesUnselectedInheritedChild() {
        repository.transaction(em -> {
            standardConfigAdapter.writeGlobal(em, "zram.logicalCapacityBytes", 16384L);
            standardConfigAdapter.writeOverride(em, "host2", "HostVO", "zram.ramLimitBytes", 8192L);
            return null;
        });
        APIUpdateMemoryPolicyMsg msg = request(0);
        msg.setTargetHostUuids(Collections.singletonList("host1"));
        msg.setPolicy("{\"zram\":{\"logicalCapacityBytes\":4096}}");
        try { repository.submit(msg); fail("unselected Host still inherits the new Global value"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_INVALID_POLICY", expected.getCode()); }
        assertEquals(0L, repository.getPolicy("Global", "global").getRevision());
        assertEquals(Long.valueOf(16384), MemoryPolicyRules.decode(repository.getPolicy("Global", "global")
                .getEffectivePolicy()).zram.logicalCapacityBytes);
        assertEquals(0L, repository.taskCount(new APIQueryMemoryTaskMsg()));
    }

    @Test public void publishedUpdateExamplePassesActualSourceAdmissionWithoutAddingFields() {
        APIUpdateMemoryPolicyMsg example = APIUpdateMemoryPolicyMsg.__example__();
        example.setSession(request(0).getSession()); // Transport authentication, not a business-field repair.
        String clusterUuid = example.getExpectedSourceRevisions().keySet().stream()
                .filter(key -> key.startsWith("Cluster:")).findFirst().get().substring("Cluster:".length());
        repository.transaction(em -> {
            TestCluster cluster = new TestCluster(); cluster.uuid = clusterUuid; cluster.hypervisorType = "KVM";
            em.persist(cluster);
            TestHost host = new TestHost(); host.uuid = example.getResourceUuid(); host.clusterUuid = clusterUuid;
            host.hypervisorType = "KVM"; host.status = HostStatus.Connected; em.persist(host);
            MemoryStateVO state = new MemoryStateVO(); state.setHostUuid(host.uuid); state.setStatus("Succeeded");
            state.setState("{}"); state.setLastSampleTime(System.currentTimeMillis());
            state.setCapabilities("{\"supported\":true,\"ksm\":true,\"ksmZeroPages\":true,\"zram\":true,\"writeback\":true}");
            em.persist(state); return null;
        });
        registerPolicyReadResource(example.getResourceUuid(), "HostVO");
        Map<String, Long> original = example.getExpectedSourceRevisions();
        example.setExpectedSourceRevisions(null);
        try { repository.submit(example); fail("missing documented sources must be rejected"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_REVISION_CONFLICT", expected.getCode()); }
        example.setExpectedSourceRevisions(original);
        assertEquals("Queued", repository.submit(example).getStatus());
        assertEquals(1L, repository.getPolicy("Host", example.getResourceUuid()).getRevision());
    }

    @Test public void publishedReadReplySourceChainCanBeCopiedIntoUpdate() {
        verifyPublishedReplyAdmission(APIGetMemoryPolicyReply.__example__().getInventory());
    }

    @Test public void publishedPreviewReplySourceChainCanBeCopiedIntoUpdate() {
        verifyPublishedReplyAdmission(APIPreviewMemoryPolicyReply.__example__().getInventory());
    }

    private void verifyPublishedReplyAdmission(MemoryPolicyInventory read) {
        String clusterUuid = read.getSourceRevisions().keySet().stream()
                .filter(key -> key.startsWith("Cluster:")).findFirst().get().substring("Cluster:".length());
        repository.transaction(em -> {
            TestCluster cluster = new TestCluster(); cluster.uuid = clusterUuid; cluster.hypervisorType = "KVM";
            em.persist(cluster);
            TestHost host = new TestHost(); host.uuid = read.getResourceUuid(); host.clusterUuid = clusterUuid;
            host.hypervisorType = "KVM"; host.status = HostStatus.Connected; em.persist(host);
            for (Map.Entry<String, Long> source : read.getSourceRevisions().entrySet()) {
                MemoryPolicyVO policy = em.find(MemoryPolicyVO.class, source.getKey());
                if (policy == null) {
                    policy = new MemoryPolicyVO(); policy.setUuid(source.getKey());
                    String[] identity = source.getKey().split(":", 2);
                    policy.setScope(identity[0]); policy.setResourceUuid(identity[1]);
                    policy.setPolicy("{\"schemaVersion\":1}"); em.persist(policy);
                }
                policy.setRevision(source.getValue());
            }
            MemoryStateVO state = new MemoryStateVO(); state.setHostUuid(host.uuid); state.setStatus("Succeeded");
            state.setState("{}"); state.setLastSampleTime(System.currentTimeMillis());
            state.setCapabilities("{\"supported\":true,\"ksm\":true,\"ksmZeroPages\":true,\"zram\":true,\"writeback\":true}");
            em.persist(state); return null;
        });
        registerPolicyReadResource(read.getResourceUuid(), "HostVO");
        MemoryPolicyInventory actual = repository.getPolicy(null, read.getResourceUuid());
        assertEquals(actual.getSourceRevisions(), read.getSourceRevisions());
        assertEquals(actual.getRevision(), read.getRevision());
        assertEquals(actual.getSourceRevision(), read.getSourceRevision());
        assertEquals("Host:" + read.getResourceUuid(), read.getFieldSources().get("ksm.enabled"));
        APIUpdateMemoryPolicyMsg update = APIUpdateMemoryPolicyMsg.__example__();
        update.setSession(request(0).getSession());
        update.setResourceUuid(read.getResourceUuid());
        update.setExpectedRevision(read.getRevision());
        update.setExpectedSourceRevisions(read.getSourceRevisions());
        assertNull("new public example must omit compatibility scope", update.getScope());
        assertEquals("Queued", repository.submit(update).getStatus());
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
                "host1", "HostVO", "true")), ConfigMutationContext.internal(), false);
        assertEquals(1L, repository.getPolicy("Host", "host1").getRevision());
        assertNull(host1State().getActiveTaskUuid());
        assertEquals("standard-pause", host1State().getControlOperationUuid());
        assertEquals("Blocked", host1State().getPolicyPlanStatus());
        assertTrue(repository.claim("mn").isEmpty());
        assertFalse("reconnection cannot bypass the same persistent pause fence",
                repository.applyEffectivePolicyAfterConnect("host1"));
        assertEquals("standard-pause", host1State().getControlOperationUuid());
    }

    @Test public void reconnectCannotBypassUnresolvedControlWithSucceededObservationStatus() {
        registerStandardResources();
        repository.transaction(em -> {
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.enabled", true);
            em.persist(testTask("unresolved-control", null, "apply", "Unknown", null));
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setControlOperationUuid("unresolved-control");
            state.setStatus("Succeeded"); state.setAppliedPolicy("{\"schemaVersion\":1}");
            state.setAppliedPolicyHash("prior-policy");
            state.setLastSampleTime(System.currentTimeMillis());
            state.setState("{\"lifecycle\":{\"paused\":false,\"activeState\":\"running\"}}");
            return null;
        });
        assertFalse(repository.applyEffectivePolicyAfterConnect("host1"));
        assertNull(host1State().getActiveTaskUuid());
        assertEquals("unresolved-control", host1State().getControlOperationUuid());
        assertEquals(1L, repository.taskCount(new APIQueryMemoryTaskMsg()));
    }

    @Test public void standardWriteAfterCompletedDrainUsesSameMaintenanceProofAsCompatibility() {
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
                "host1", "HostVO", "true")), ConfigMutationContext.internal(), false);
        assertEquals(1L, repository.getPolicy("Host", "host1").getRevision());
        assertNotNull(host1State().getActiveTaskUuid());
        assertNotEquals("completed-drain", host1State().getControlOperationUuid());
        assertEquals(1, repository.claim("mn").size());
    }

    @Test public void legacyHostKsmMutationAdvancesRevisionInsideCallerTransaction() {
        repository.transaction(em -> {
            em.persist(new ResourceVO(new Object[]{"host1", "host1", "HostVO"})); return null;
        });
        assertEquals(0L, repository.getPolicy("Host", "host1").getRevision());
        repository.recordLegacyKsmResourceMutation("host1");
        assertEquals(1L, repository.getPolicy("Host", "host1").getRevision());

        try {
            repository.transaction(em -> {
                repository.recordLegacyKsmResourceMutation("host1");
                throw new IllegalStateException("force caller rollback after validator");
            });
            fail("outer transaction must abort");
        } catch (IllegalStateException expected) { }
        assertEquals("revision bump must roll back with the config write", 1L,
                repository.getPolicy("Host", "host1").getRevision());
    }

    @Test public void realInterceptorAndRepositoryMutationFenceOnlyCommitOnceAndRollbackTogether() throws Exception {
        repository.transaction(em -> {
            em.persist(new ResourceVO(new Object[]{"host1", "host1", "HostVO"}));
            MemoryPolicyVO row = new MemoryPolicyVO(); row.setUuid("Host:host1"); row.setScope("Host");
            row.setResourceUuid("host1"); row.setRevision(0); row.setPolicy("{\"schemaVersion\":1}"); em.persist(row);
            ResourceConfigVO config = new ResourceConfigVO(); config.setUuid("rc-host1-ksm"); config.setResourceUuid("host1");
            config.setResourceType("HostVO"); config.setCategory("kvm"); config.setName("host.ksm"); config.setValue("true");
            em.persist(config); return null;
        });

        org.springframework.orm.jpa.JpaTransactionManager manager = new org.springframework.orm.jpa.JpaTransactionManager(factory);
        org.springframework.transaction.aspectj.AnnotationTransactionAspect aspect =
                org.springframework.transaction.aspectj.AnnotationTransactionAspect.aspectOf();
        Field managerField = org.springframework.transaction.interceptor.TransactionAspectSupport.class.getDeclaredField("transactionManager");
        managerField.setAccessible(true); Object previousManager = managerField.get(aspect);
        Object previousLoader = readStatic(Platform.class, "loader");
        EntityManager shared = org.springframework.orm.jpa.SharedEntityManagerCreator.createSharedEntityManager(factory);
        org.zstack.core.db.DatabaseFacade dbf = (org.zstack.core.db.DatabaseFacade) Proxy.newProxyInstance(
                org.zstack.core.db.DatabaseFacade.class.getClassLoader(), new Class<?>[]{org.zstack.core.db.DatabaseFacade.class},
                (p, m, a) -> {
                    if (m.getName().equals("getEntityManager")) return shared;
                    if (m.getName().equals("getCriteriaBuilder")) return factory.getCriteriaBuilder();
                    if (m.getReturnType().equals(boolean.class)) return false;
                    if (m.getReturnType().equals(long.class)) return 0L;
                    return null;
                });
        ComponentLoader loader = (ComponentLoader) Proxy.newProxyInstance(ComponentLoader.class.getClassLoader(),
                new Class<?>[]{ComponentLoader.class}, (p, m, a) -> {
                    if (m.getName().equals("getComponent") && a != null && a.length == 1
                            && a[0] == org.zstack.core.db.DatabaseFacade.class) return dbf;
                    if (m.getName().equals("getComponent") && a != null && a.length == 1
                            && a[0] == org.zstack.core.errorcode.ErrorFacade.class) {
                        return Proxy.newProxyInstance(org.zstack.core.errorcode.ErrorFacade.class.getClassLoader(),
                                new Class<?>[]{org.zstack.core.errorcode.ErrorFacade.class}, (ep, em, ea) -> {
                                    if (em.getReturnType() == org.zstack.header.errorcode.ErrorCode.class) {
                                        return new org.zstack.header.errorcode.ErrorCode("TEST", "test");
                                    }
                                    return null;
                                });
                    }
                    if (m.getName().equals("getComponentNoExceptionWhenNotExisting")) return null;
                    if (m.getName().equals("hasComponent")) return false;
                    return null;
                });
        writeStatic(Platform.class, "loader", loader);
        aspect.setTransactionManager(manager);
        try {
            GlobalConfig global = new GlobalConfig("kvm", "host.ksm");
            setField(GlobalConfig.class, global, "value", "false"); setField(GlobalConfig.class, global, "defaultValue", "false");
            ResourceConfig config = new ResourceConfig();
            setField(ResourceConfig.class, config, "globalConfig", global); setField(ResourceConfig.class, config, "dbf", dbf);
            setField(ResourceConfig.class, config, "evtf", Proxy.newProxyInstance(EventFacade.class.getClassLoader(),
                    new Class<?>[]{EventFacade.class}, (p, m, a) -> null));
            Map<String, Object> getters = new HashMap<>(); getters.put("HostVO", new Object());
            setField(ResourceConfig.class, config, "configGetter", getters);
            KVMHostFactory kvmFactory = new KVMHostFactory();
            ResourceConfigFacade configFacade = (ResourceConfigFacade) Proxy.newProxyInstance(
                    ResourceConfigFacade.class.getClassLoader(), new Class<?>[]{ResourceConfigFacade.class},
                    (p, m, a) -> m.getName().equals("getResourceConfig") ? config : null);
            setField(KVMHostFactory.class, kvmFactory, "rcf", configFacade);
            setField(KVMHostFactory.class, kvmFactory, "memoryRepository", repository);
            java.lang.reflect.Method installFence = KVMHostFactory.class.getDeclaredMethod("installLegacyHostKsmCoordination");
            installFence.setAccessible(true); installFence.invoke(kvmFactory);
            assertTrue("actual KVM bootstrap registration opts the KSM config into atomic bulk", config.requiresAtomicBulkTransaction());

            GlobalConfigFacade gcf = (GlobalConfigFacade) Proxy.newProxyInstance(GlobalConfigFacade.class.getClassLoader(),
                    new Class<?>[]{GlobalConfigFacade.class}, (p, m, a) -> m.getName().equals("getAllConfig")
                            ? Collections.singletonMap(global.getIdentity(), global) : null);
            ResourceConfigFacade rcf = (ResourceConfigFacade) Proxy.newProxyInstance(ResourceConfigFacade.class.getClassLoader(),
                    new Class<?>[]{ResourceConfigFacade.class}, (p, m, a) -> m.getName().equals("getResourceConfig") ? config : null);
            ResourceConfigApiInterceptor interceptor = new ResourceConfigApiInterceptor();
            setField(ResourceConfigApiInterceptor.class, interceptor, "gcf", gcf);
            setField(ResourceConfigApiInterceptor.class, interceptor, "rcf", rcf);
            APIUpdateResourceConfigsMsg rejected = new APIUpdateResourceConfigsMsg(); rejected.setResourceUuid("host1");
            APIUpdateResourceConfigsMsg.ResourceConfigAO good = new APIUpdateResourceConfigsMsg.ResourceConfigAO();
            good.setCategory("kvm"); good.setName("host.ksm"); good.setValue("false");
            APIUpdateResourceConfigsMsg.ResourceConfigAO bad = new APIUpdateResourceConfigsMsg.ResourceConfigAO();
            bad.setCategory("missing"); bad.setName("unknown"); bad.setValue("x");
            rejected.setResourceConfigs(Arrays.asList(good, bad));
            try {
                interceptor.intercept(rejected);
                fail("a later invalid config must reject the whole interceptor pass");
            } catch (org.zstack.header.apimediator.ApiMessageInterceptionException expected) { }
            assertEquals("interceptor-only request cannot bump real repository revision", 0L,
                    repository.getPolicy("Host", "host1").getRevision());
            assertEquals(0L, repository.taskCount(new APIQueryMemoryTaskMsg()));

            APIUpdateResourceConfigsMsg valid = new APIUpdateResourceConfigsMsg(); valid.setResourceUuid("host1");
            valid.setResourceConfigs(Collections.singletonList(good)); interceptor.intercept(valid);
            ResourceConfigFacadeImpl facade = new ResourceConfigFacadeImpl();
            CloudBus bus = (CloudBus) Proxy.newProxyInstance(CloudBus.class.getClassLoader(), new Class<?>[]{CloudBus.class},
                    (p, m, a) -> null);
            setField(ResourceConfigFacadeImpl.class, facade, "bus", bus);
            Map<String, ResourceConfig> configs = new HashMap<>(); configs.put(global.getIdentity(), config);
            setField(ResourceConfigFacadeImpl.class, facade, "resourceConfigs", configs);
            APIUpdateResourceConfigMsg update = new APIUpdateResourceConfigMsg(); update.setCategory("kvm");
            update.setName("host.ksm"); update.setResourceUuid("host1"); update.setValue("false");
            java.lang.reflect.Method updateHandler = ResourceConfigFacadeImpl.class
                    .getDeclaredMethod("handle", APIUpdateResourceConfigMsg.class);
            updateHandler.setAccessible(true);
            updateHandler.invoke(facade, update);
            assertEquals("actual ResourceConfig handler invokes the real repository fence once", 1L,
                    repository.getPolicy("Host", "host1").getRevision());
            assertEquals(0L, repository.taskCount(new APIQueryMemoryTaskMsg()));

            config.installTransactionalMutationExtension(new ResourceConfigTransactionalMutationExtensionPoint() {
                @Override public boolean requiresAtomicBulkTransaction() { return true; }
                @Override public void beforeUpdate(EntityManager em, ResourceConfig ignored, String uuid,
                        String type, String oldValue, String newValue) {
                    repository.recordLegacyKsmResourceMutation(em, uuid);
                    throw new IllegalStateException("rollback after real revision fence");
                }
                @Override public void beforeDelete(EntityManager em, ResourceConfig ignored, String uuid,
                        String type, String oldValue) { }
            });
            try { config.updateValue("host1", "true"); fail("must rollback coordinator mutation with config"); }
            catch (IllegalStateException expected) { assertEquals("rollback after real revision fence", expected.getMessage()); }
            assertEquals("real revision fence rolls back with the config transaction", 1L,
                    repository.getPolicy("Host", "host1").getRevision());
            assertEquals(0L, repository.taskCount(new APIQueryMemoryTaskMsg()));
        } finally {
            managerField.set(aspect, previousManager);
            writeStatic(Platform.class, "loader", previousLoader);
        }
    }

    @Test public void startupMigrationMovesLegacyScalarsAndPreservesSpecializedPolicy() {
        repository.transaction(em -> {
            MemoryPolicyVO row = new MemoryPolicyVO(); row.setUuid("Host:host1"); row.setScope("Host");
            row.setResourceUuid("host1"); row.setRevision(4);
            row.setPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":true,\"zeroPagesEnabled\":false},"
                    + "\"zram\":{\"selectionMode\":\"all_running\",\"enabled\":false}} ");
            em.persist(row);
            em.remove(em.find(MemoryStandardConfigMigrationVO.class, MemoryStandardConfigMigrationVO.ORDINARY_FIELDS_V1));
            return null;
        });
        repository.initialize();
        repository.transaction(em -> {
            MemoryPolicyVO row = em.find(MemoryPolicyVO.class, "Host:host1");
            Map<?, ?> specialized = org.zstack.utils.gson.JSONObjectUtil.toObject(row.getPolicy(), Map.class);
            assertFalse("legacy KSM scalars should move to standard config; policy=" + row.getPolicy(), specialized.containsKey("ksm"));
            assertEquals(4L, row.getRevision());
            assertNotNull(row.getLegacyPolicy());
            assertEquals(Boolean.TRUE, standardConfigAdapter.readOverrideOwn(em, "ksm.enabled", "host1"));
            assertEquals(Boolean.FALSE, standardConfigAdapter.readOverrideOwn(em, "ksm.zeroPagesEnabled", "host1"));
            assertEquals("all_running", MemoryPolicyRules.decode(repository.getPolicy("Host", "host1").getPolicy()).zram.selectionMode);
            assertEquals(Boolean.TRUE, MemoryPolicyRules.decode(repository.getPolicy("Host", "host1").getEffectivePolicy()).ksm.enabled);
            assertEquals("DONE", em.find(MemoryStandardConfigMigrationVO.class,
                    MemoryStandardConfigMigrationVO.ORDINARY_FIELDS_V1).getStatus());
            return null;
        });
    }

    @Test public void startupMigrationBlocksConflictingLegacyAndStandardValuesWithoutPartialMove() {
        repository.transaction(em -> {
            MemoryPolicyVO row = new MemoryPolicyVO(); row.setUuid("Host:host1"); row.setScope("Host");
            row.setResourceUuid("host1"); row.setRevision(2);
            row.setPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":true}}"); em.persist(row);
            MemoryStandardField field = MemoryStandardField.forPath("ksm.enabled");
            ResourceConfigVO standard = new ResourceConfigVO(); standard.setUuid(UUID.randomUUID().toString().replace("-", ""));
            standard.setCategory(field.category()); standard.setName(field.configName()); standard.setResourceUuid("host1");
            standard.setResourceType("HostVO"); standard.setValue("false"); em.persist(standard);
            em.remove(em.find(MemoryStandardConfigMigrationVO.class, MemoryStandardConfigMigrationVO.ORDINARY_FIELDS_V1));
            return null;
        });
        try { repository.initialize(); fail("conflicting source values must block migration"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_NOT_READY", expected.getCode()); }
        repository.transaction(em -> {
            MemoryPolicyVO row = em.find(MemoryPolicyVO.class, "Host:host1");
            assertTrue(row.getPolicy().contains("\"enabled\":true"));
            assertNull(row.getLegacyPolicy());
            assertEquals("BLOCKED", em.find(MemoryStandardConfigMigrationVO.class,
                    MemoryStandardConfigMigrationVO.ORDINARY_FIELDS_V1).getStatus());
            return null;
        });
    }

    @Test public void startupMigrationBlocksStoredNoneOrBlankResourceRowsWithoutOverwritingThem() {
        MemoryStandardField field = MemoryStandardField.KSM_ENABLED;
        for (String stored : Arrays.asList("none", "  ")) {
            repository.transaction(em -> {
                MemoryPolicyVO row = em.find(MemoryPolicyVO.class, "Host:host1");
                if (row == null) {
                    row = new MemoryPolicyVO(); row.setUuid("Host:host1"); row.setScope("Host");
                    row.setResourceUuid("host1"); row.setRevision(2); row.setPolicy("{\"schemaVersion\":1}"); em.persist(row);
                }
                row.setPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":true}}");
                ResourceConfigVO standard = em.createQuery("select r from ResourceConfigVO r where r.resourceUuid=:uuid and r.category=:category and r.name=:name", ResourceConfigVO.class)
                        .setParameter("uuid", "host1").setParameter("category", field.category())
                        .setParameter("name", field.configName()).getResultList().stream().findFirst().orElse(null);
                if (standard == null) {
                    standard = new ResourceConfigVO(); standard.setUuid(UUID.randomUUID().toString().replace("-", ""));
                    standard.setCategory(field.category()); standard.setName(field.configName());
                    standard.setResourceUuid("host1"); standard.setResourceType("HostVO");
                }
                standard.setValue(stored); em.merge(standard);
                MemoryStandardConfigMigrationVO marker = em.find(MemoryStandardConfigMigrationVO.class,
                        MemoryStandardConfigMigrationVO.ORDINARY_FIELDS_V1);
                if (marker != null) { em.remove(marker); }
                return null;
            });

            try { repository.initialize(); fail("stored none/blank cannot be treated as an absent override"); }
            catch (MemoryOperationException expected) { assertEquals("MEMORY_NOT_READY", expected.getCode()); }
            repository.transaction(em -> {
                MemoryPolicyVO row = em.find(MemoryPolicyVO.class, "Host:host1");
                assertTrue(row.getPolicy().contains("\"enabled\":true"));
                ResourceConfigVO standard = em.createQuery("select r from ResourceConfigVO r where r.resourceUuid=:uuid and r.category=:category and r.name=:name", ResourceConfigVO.class)
                        .setParameter("uuid", "host1").setParameter("category", field.category())
                        .setParameter("name", field.configName()).getSingleResult();
                assertEquals(stored, standard.getValue());
                assertEquals("BLOCKED", em.find(MemoryStandardConfigMigrationVO.class,
                        MemoryStandardConfigMigrationVO.ORDINARY_FIELDS_V1).getStatus());
                return null;
            });
        }
    }

    @Test public void startupMigrationBlocksExplicitGlobalNoneButAllowsSchemaDefaultNone() {
        repository.transaction(em -> {
            MemoryPolicyVO globalPolicy = em.find(MemoryPolicyVO.class, "Global:global");
            globalPolicy.setPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":true}}");
            GlobalConfigVO global = em.createQuery("select g from GlobalConfigVO g where g.category=:category and g.name=:name", GlobalConfigVO.class)
                    .setParameter("category", MemoryStandardField.KSM_ENABLED.category())
                    .setParameter("name", MemoryStandardField.KSM_ENABLED.configName()).getSingleResult();
            global.setDefaultValue("false"); global.setValue("none");
            MemoryStandardConfigMigrationVO marker = em.find(MemoryStandardConfigMigrationVO.class,
                    MemoryStandardConfigMigrationVO.ORDINARY_FIELDS_V1);
            if (marker != null) { em.remove(marker); }
            return null;
        });
        try { repository.initialize(); fail("explicit Global none conflicts with legacy true"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_NOT_READY", expected.getCode()); }
        repository.transaction(em -> {
            assertTrue(em.find(MemoryPolicyVO.class, "Global:global").getPolicy().contains("\"enabled\":true"));
            GlobalConfigVO global = em.createQuery("select g from GlobalConfigVO g where g.category=:category and g.name=:name", GlobalConfigVO.class)
                    .setParameter("category", MemoryStandardField.KSM_ENABLED.category())
                    .setParameter("name", MemoryStandardField.KSM_ENABLED.configName()).getSingleResult();
            assertEquals("none", global.getValue());
            assertEquals("BLOCKED", em.find(MemoryStandardConfigMigrationVO.class,
                    MemoryStandardConfigMigrationVO.ORDINARY_FIELDS_V1).getStatus());
            return null;
        });
        repository.transaction(em -> {
            GlobalConfigVO global = em.createQuery("select g from GlobalConfigVO g where g.category=:category and g.name=:name", GlobalConfigVO.class)
                    .setParameter("category", MemoryStandardField.KSM_ENABLED.category())
                    .setParameter("name", MemoryStandardField.KSM_ENABLED.configName()).getSingleResult();
            global.setDefaultValue("none"); global.setValue("none");
            return null;
        });
        repository.initialize();
        repository.transaction(em -> {
            assertEquals(Boolean.TRUE, standardConfigAdapter.readGlobalOwn(em, "ksm.enabled"));
            assertFalse(em.find(MemoryPolicyVO.class, "Global:global").getPolicy().contains("\"enabled\":true"));
            assertEquals("DONE", em.find(MemoryStandardConfigMigrationVO.class,
                    MemoryStandardConfigMigrationVO.ORDINARY_FIELDS_V1).getStatus());
            return null;
        });
    }

    @Test public void policyApiPersistsOrdinaryCapacityOnlyToStandardRowsAndChecksCombinedPolicy() {
        APIUpdateMemoryPolicyMsg msg = request(0);
        MemoryPolicyInventory current = repository.getPolicy("Host", "host1");
        msg.setScope("Host"); msg.setResourceUuid("host1"); msg.setTargetHostUuids(null);
        msg.setExpectedRevision(current.getRevision()); msg.setExpectedSourceRevisions(current.getSourceRevisions());
        msg.setPolicy("{\"zram\":{\"logicalCapacityBytes\":16777216,\"ramLimitBytes\":8388608}}");
        repository.submit(msg);
        repository.transaction(em -> {
            MemoryPolicyVO row = em.find(MemoryPolicyVO.class, "Host:host1");
            assertFalse("ordinary fields must not be duplicated in policy JSON", row.getPolicy().contains("logicalCapacityBytes"));
            assertEquals(Long.valueOf(16777216), standardConfigAdapter.readOverrideOwn(em, "zram.logicalCapacityBytes", "host1"));
            assertEquals(Long.valueOf(8388608), standardConfigAdapter.readOverrideOwn(em, "zram.ramLimitBytes", "host1"));
            return null;
        });

        MemoryPolicyInventory updated = repository.getPolicy("Host", "host1");
        msg.setExpectedRevision(updated.getRevision()); msg.setExpectedSourceRevisions(updated.getSourceRevisions());
        msg.setClientRequestUuid(UUID.randomUUID().toString());
        msg.setPolicy("{\"zram\":{\"logicalCapacityBytes\":8388608,\"ramLimitBytes\":16777216}}");
        try { repository.submit(msg); fail("combined effective capacity limits must validate before commit"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_INVALID_POLICY", expected.getCode()); }
        repository.transaction(em -> {
            assertEquals(Long.valueOf(16777216), standardConfigAdapter.readEffective(em,
                    "zram.logicalCapacityBytes", Collections.singletonList("host1")));
            assertEquals(Long.valueOf(8388608), standardConfigAdapter.readEffective(em,
                    "zram.ramLimitBytes", Collections.singletonList("host1")));
            assertEquals(1L, em.find(MemoryPolicyVO.class, "Host:host1").getRevision());
            return null;
        });
    }

    @Test public void standardNoneWinsAtEachScopeAndDoesNotEraseOtherKsmFields() {
        repository.transaction(em -> {
            GlobalConfigVO global = em.createQuery("select g from GlobalConfigVO g where g.category=:category and g.name=:name", GlobalConfigVO.class)
                    .setParameter("category", MemoryStandardField.KSM_ENABLED.category())
                    .setParameter("name", MemoryStandardField.KSM_ENABLED.configName()).getSingleResult();
            global.setDefaultValue("none"); global.setValue("none");
            MemoryPolicyVO clusterPolicy = new MemoryPolicyVO(); clusterPolicy.setUuid("Cluster:cluster1");
            clusterPolicy.setScope("Cluster"); clusterPolicy.setResourceUuid("cluster1");
            clusterPolicy.setPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":true}}"); em.persist(clusterPolicy);
            MemoryPolicyVO hostPolicy = new MemoryPolicyVO(); hostPolicy.setUuid("Host:host1");
            hostPolicy.setScope("Host"); hostPolicy.setResourceUuid("host1");
            hostPolicy.setPolicy("{\"schemaVersion\":1,\"ksm\":{\"zeroPagesEnabled\":true,\"pagesToScan\":777,\"sleepMillis\":23}}");
            em.persist(hostPolicy);
            persistLegacyKsmValue(em, "cluster1", "ClusterVO", "none");
            persistLegacyKsmValue(em, "host1", "HostVO", "none");
            return null;
        });

        MemoryPolicyInventory global = repository.getPolicy("Global", "global");
        assertNull(MemoryStandardConfigCodec.get(global.getEffectivePolicy(), "ksm.enabled"));
        assertTrue("schema-default none must not be reported as a user override",
                global.getFieldModes() == null || !global.getFieldModes().containsKey("ksm.enabled"));
        assertFalse("Global KSM schema default is not an explicit field source",
                "Global:global".equals(global.getFieldSources().get("ksm.enabled")));
        assertFalse("all Global schema-default scalar values remain non-explicit sources",
                "Global:global".equals(global.getFieldSources().get("zram.enabled")));

        MemoryPolicyInventory host = repository.getPolicy("Host", "host1");
        assertNull(MemoryStandardConfigCodec.get(host.getEffectivePolicy(), "ksm.enabled"));
        assertEquals("Host:host1", host.getFieldSources().get("ksm.enabled"));
        assertEquals("Unmanaged", host.getFieldModes().get("ksm.enabled"));
        assertEquals(Boolean.TRUE, MemoryStandardConfigCodec.get(host.getEffectivePolicy(), "ksm.zeroPagesEnabled"));
        assertEquals(Long.valueOf(777), MemoryStandardConfigCodec.get(host.getEffectivePolicy(), "ksm.pagesToScan"));
        assertEquals(Long.valueOf(23), MemoryStandardConfigCodec.get(host.getEffectivePolicy(), "ksm.sleepMillis"));

        repository.transaction(em -> {
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.enabled", true);
            return null;
        });
        host = repository.getPolicy("Host", "host1");
        assertEquals(Boolean.TRUE, MemoryStandardConfigCodec.get(host.getEffectivePolicy(), "ksm.enabled"));
        assertFalse(host.getFieldModes().containsKey("ksm.enabled"));

        repository.transaction(em -> {
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.enabled", null);
            return null;
        });
        MemoryPolicyInventory clusterNone = repository.getPolicy("Host", "host1");
        assertNull(MemoryStandardConfigCodec.get(clusterNone.getEffectivePolicy(), "ksm.enabled"));
        assertEquals("Cluster:cluster1", clusterNone.getFieldSources().get("ksm.enabled"));
        assertEquals("Unmanaged", clusterNone.getFieldModes().get("ksm.enabled"));

        repository.transaction(em -> {
            standardConfigAdapter.writeOverride(em, "cluster1", "ClusterVO", "ksm.enabled", true);
            GlobalConfigVO globalConfig = em.createQuery("select g from GlobalConfigVO g where g.category=:category and g.name=:name", GlobalConfigVO.class)
                    .setParameter("category", MemoryStandardField.KSM_ENABLED.category())
                    .setParameter("name", MemoryStandardField.KSM_ENABLED.configName()).getSingleResult();
            globalConfig.setDefaultValue("false");
            globalConfig.setValue("none");
            MemoryPolicyVO globalPolicy = em.find(MemoryPolicyVO.class, "Global:global");
            globalPolicy.setPolicy(MemoryStandardConfigCodec.put(globalPolicy.getPolicy(), "ksm.enabled", true));
            return null;
        });
        MemoryPolicyInventory clusterTrue = repository.getPolicy("Host", "host1");
        assertEquals(Boolean.TRUE, MemoryStandardConfigCodec.get(clusterTrue.getEffectivePolicy(), "ksm.enabled"));
        MemoryPolicyInventory explicitGlobalNone = repository.getPolicy("Global", "global");
        assertNull("explicit Global none must mask the default false and compat JSON",
                MemoryStandardConfigCodec.get(explicitGlobalNone.getEffectivePolicy(), "ksm.enabled"));
        assertEquals("explicit none over a non-none schema default is a real unmanaged override",
                "Unmanaged", explicitGlobalNone.getFieldModes().get("ksm.enabled"));
        assertEquals("Global explicit none remains the field source", "Global:global",
                explicitGlobalNone.getFieldSources().get("ksm.enabled"));
    }

    @Test public void standardMutationNoneAndDeleteUseCandidateStateAndDoNotResurrectCompatValue() {
        registerStandardResources();
        repository.transaction(em -> {
            MemoryPolicyVO hostPolicy = new MemoryPolicyVO(); hostPolicy.setUuid("Host:host1");
            hostPolicy.setScope("Host"); hostPolicy.setResourceUuid("host1");
            hostPolicy.setPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":false}}"); em.persist(hostPolicy);
            return null;
        });
        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.KSM_ENABLED,
                "host1", "HostVO", "none")), ConfigMutationContext.internal(), false);
        MemoryPolicyInventory unmanaged = repository.getPolicy("Host", "host1");
        assertNull(MemoryStandardConfigCodec.get(unmanaged.getEffectivePolicy(), "ksm.enabled"));
        assertEquals("Unmanaged", unmanaged.getFieldModes().get("ksm.enabled"));
        assertNull("compatibility value must be scrubbed when standard config takes ownership",
                repository.transaction(em -> MemoryStandardConfigCodec.get(
                        em.find(MemoryPolicyVO.class, "Host:host1").getPolicy(), "ksm.enabled")));

        standardCommit(Collections.singletonList(new ConfigMutation(MemoryStandardField.KSM_ENABLED.category(),
                MemoryStandardField.KSM_ENABLED.configName(), "host1", "HostVO", null, true)),
                ConfigMutationContext.internal(), false);
        MemoryPolicyInventory inherited = repository.getPolicy("Host", "host1");
        assertNull("Global default none is unmanaged, not an implicit false",
                MemoryStandardConfigCodec.get(inherited.getEffectivePolicy(), "ksm.enabled"));
        assertFalse(inherited.getFieldModes().containsKey("ksm.enabled"));
    }

    @Test public void clearingStandardNonePreviewAndCommitUsesParentAndConsumesCompatibilityValue() {
        repository.transaction(em -> {
            GlobalConfigVO global = em.createQuery("select g from GlobalConfigVO g where g.category=:category and g.name=:name", GlobalConfigVO.class)
                    .setParameter("category", MemoryStandardField.KSM_ENABLED.category())
                    .setParameter("name", MemoryStandardField.KSM_ENABLED.configName()).getSingleResult();
            global.setValue("true");
            MemoryPolicyVO hostPolicy = new MemoryPolicyVO(); hostPolicy.setUuid("Host:host1");
            hostPolicy.setScope("Host"); hostPolicy.setResourceUuid("host1");
            hostPolicy.setPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":false,\"zeroPagesEnabled\":true}}");
            em.persist(hostPolicy);
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "zram.enabled", true);
            persistLegacyKsmValue(em, "host1", "HostVO", "none");
            return null;
        });
        MemoryPolicyInventory initial = repository.getPolicy("Host", "host1");
        MemoryPolicyInventory preview = repository.preview("Host", "host1", "{}", "clearOverride",
                Collections.singletonList("ksm.enabled"));
        assertEquals("clear preview must reveal parent", Boolean.TRUE,
                MemoryStandardConfigCodec.get(preview.getEffectivePolicy(), "ksm.enabled"));
        assertEquals("clearing one field must retain other Host source metadata", "Host:host1",
                preview.getFieldSources().get("zram.enabled"));
        assertFalse("clear preview must not report the removed Host none mode",
                preview.getFieldModes() != null && preview.getFieldModes().containsKey("ksm.enabled"));

        MemoryPolicyInventory enablePreview = repository.preview("Host", "host1",
                "{\"ksm\":{\"enabled\":true}}", "apply", null);
        assertEquals(Boolean.TRUE, MemoryStandardConfigCodec.get(enablePreview.getEffectivePolicy(), "ksm.enabled"));
        assertFalse("preview enabled must override the stored unmanaged marker",
                enablePreview.getFieldModes() != null && enablePreview.getFieldModes().containsKey("ksm.enabled"));

        APIUpdateMemoryPolicyMsg clear = request(initial.getRevision()); clear.setScope("Host"); clear.setResourceUuid("host1");
        clear.setTargetHostUuids(null); clear.setExpectedGlobalRevision(initial.getSourceRevision());
        clear.setExpectedSourceRevisions(initial.getSourceRevisions()); clear.setAction("clearOverride"); clear.setPolicy("{}");
        clear.setClearOverrideFields(Collections.singletonList("ksm.enabled"));
        repository.submit(clear);
        MemoryPolicyInventory after = repository.getPolicy("Host", "host1");
        assertEquals("clear commit must reveal parent", Boolean.TRUE,
                MemoryStandardConfigCodec.get(after.getEffectivePolicy(), "ksm.enabled"));
        assertFalse(after.getFieldModes().containsKey("ksm.enabled"));
        assertNull("clearing canonical standard none must not resurrect old compatibility false",
                repository.transaction(em -> MemoryPolicyRules.decode(em.find(MemoryPolicyVO.class, "Host:host1").getPolicy()).ksm.enabled));
    }

    @Test public void vmParticipationNeverReadsUnrelatedKsmResourceRows() {
        repository.transaction(em -> {
            persistLegacyKsmValue(em, "vm1", "VmInstanceVO", "  ");
            return null;
        });
        MemoryPolicyInventory vm = repository.getPolicy("VM", "vm1");
        assertTrue(vm.getFieldModes().isEmpty());
        assertFalse(vm.getFieldSources().containsKey("ksm.enabled"));
    }

    private void persistLegacyKsmValue(EntityManager em, String resource, String type, String value) {
        MemoryStandardField field = MemoryStandardField.forPath("ksm.enabled");
        ResourceConfigVO row = new ResourceConfigVO(); row.setUuid(UUID.randomUUID().toString().replace("-", ""));
        row.setCategory(field.category()); row.setName(field.configName()); row.setValue(value);
        row.setResourceUuid(resource); row.setResourceType(type); em.persist(row);
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

    @Test public void legacyHostKsmMutationRejectsManagedOwnershipWithoutBumpingRevision() {
        MemoryAgentResponse managed = new MemoryAgentResponse();
        managed.status = "Succeeded"; managed.sampleTime = System.currentTimeMillis();
        managed.state = Collections.<String, Object>singletonMap("managed", true);
        repository.observe("host1", managed);
        repository.transaction(em -> {
            em.persist(new ResourceVO(new Object[]{"host1", "host1", "HostVO"})); return null;
        });
        try { repository.recordLegacyKsmResourceMutation("host1"); fail("managed host must reject legacy KSM writes"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_CONTROLLER_CONFLICT", expected.getCode()); }
        assertEquals(0L, repository.getPolicy("Host", "host1").getRevision());
    }

    @Test public void legacyHostKsmMutationRejectsUnknownStateWithoutBumpingRevision() {
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Unknown"); state.setState("{}");
            em.persist(new ResourceVO(new Object[]{"host1", "host1", "HostVO"})); return null;
        });
        try { repository.recordLegacyKsmResourceMutation("host1"); fail("unknown ownership must reject legacy KSM writes"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_CONTROLLER_CONFLICT", expected.getCode()); }
        assertEquals(0L, repository.getPolicy("Host", "host1").getRevision());
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
        assertFalse(state.isPermitAuthorized());
        response.status = null;
        repository.observe("host2", response);
        assertEquals("Unknown", repository.states(Collections.singletonList("host2"), 0, 10).get(0).getStatus());
    }

    @Test public void freshCloudBootstrapPersistsFullTargetAndPlansHostsIndependently() {
        insertFreshBootstrap("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host2");
            state.setCapabilities("{\"supported\":true,\"ksm\":true,\"ksmZeroPages\":false,\"zram\":false,\"writeback\":false}");
            return null;
        });
        assertNull(repository.freshCloudBootstrapBlockReason());
        MemoryTaskInventory accepted = repository.submitFreshCloudBootstrap("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        assertNotNull(accepted);
        assertEquals(1L, repository.getPolicy("Global", "global").getRevision());
        MemoryPolicyConfig effective = MemoryPolicyRules.decode(repository.getPolicy("Global", "global").getEffectivePolicy());
        assertTrue(Boolean.TRUE.equals(effective.ksm.enabled));
        assertTrue(Boolean.TRUE.equals(effective.ksm.zeroPagesEnabled));
        assertTrue(Boolean.TRUE.equals(effective.zram.enabled));
        assertFalse(Boolean.TRUE.equals(effective.writeback.enabled));

        List<MemoryTaskVO> children = repository.claim("mn").stream()
                .filter(task -> task.getHostUuid() != null).collect(java.util.stream.Collectors.toList());
        assertEquals(2, children.size());
        MemoryTaskVO host2 = children.stream().filter(task -> "host2".equals(task.getHostUuid())).findFirst().get();
        MemoryPolicyConfig partial = MemoryPolicyRules.decode(host2.getPolicy());
        assertTrue(Boolean.TRUE.equals(partial.ksm.enabled));
        assertNull(partial.ksm.zeroPagesEnabled);
        assertNull(partial.zram);
        MemoryStateVO blocked = repository.states(Collections.singletonList("host2"), 0, 1).get(0);
        assertTrue(blocked.getPolicyBlockedFields().contains("ksm.zeroPagesEnabled"));
        assertTrue(blocked.getPolicyBlockedFields().contains("zram.enabled"));
        assertEquals("Queued", blocked.getPolicyPlanStatus());
        MemoryAgentResponse ack = new MemoryAgentResponse();
        ack.appliedRevision = host2.getDesiredRevision();
        repository.result(host2.getUuid(), "Succeeded", "applied supported fields", ack);
        MemoryStateVO after = repository.states(Collections.singletonList("host2"), 0, 1).get(0);
        assertNotNull(after.getAppliedPolicyHash());
        assertEquals(org.zstack.utils.gson.JSONObjectUtil.toObject(host2.getPolicy(), Map.class),
                org.zstack.utils.gson.JSONObjectUtil.toObject(after.getAppliedPolicy(), Map.class));
        assertEquals("Blocked", after.getPolicyPlanStatus());
        assertFalse(repository.applyEffectivePolicyAfterConnect("host2"));

        MemoryTaskInventory replay = repository.submitFreshCloudBootstrap("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        assertEquals(accepted.getUuid(), replay.getUuid());
        assertEquals(MemoryCloudBootstrapVO.APPLIED, repository.freshCloudBootstrap().getStatus());
        assertEquals(1L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t where t.parentUuid is null", Long.class).getSingleResult()));
    }

    @Test public void ordinaryUpgradeHasNoBootstrapRowAndMissingHostsDoNotInventOne() {
        assertNull(repository.freshCloudBootstrap());
        insertFreshBootstrap("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
        repository.transaction(em -> { em.createQuery("delete from HostVO").executeUpdate(); return null; });
        assertNull(repository.freshCloudBootstrapBlockReason());
        assertNotNull(repository.submitFreshCloudBootstrap("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"));
        assertEquals(MemoryCloudBootstrapVO.APPLIED, repository.freshCloudBootstrap().getStatus());
        assertEquals(1L, repository.getPolicy("Global", "global").getRevision());
    }

    @Test public void bootstrapRetriesOnlyNewlySupportedFieldsAfterCapabilityChange() {
        insertFreshBootstrap("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee");
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host2");
            state.setCapabilities("{\"supported\":true,\"ksm\":true,\"ksmZeroPages\":false,\"zram\":false}");
            return null;
        });
        repository.submitFreshCloudBootstrap("eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee");
        MemoryTaskVO first = repository.claim("mn").stream()
                .filter(task -> "host2".equals(task.getHostUuid())).findFirst().get();
        MemoryAgentResponse ack = new MemoryAgentResponse(); ack.appliedRevision = first.getDesiredRevision();
        repository.result(first.getUuid(), "Succeeded", "supported subset applied", ack);

        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host2");
            state.setCapabilities("{\"supported\":true,\"ksm\":true,\"ksmZeroPages\":false,\"zram\":true}");
            return null;
        });
        assertTrue(repository.applyEffectivePolicyAfterConnect("host2"));
        MemoryTaskVO next = repository.claim("mn").stream()
                .filter(task -> "host2".equals(task.getHostUuid()) && !task.getUuid().equals(first.getUuid()))
                .findFirst().get();
        MemoryPolicyConfig nextFields = MemoryPolicyRules.decode(next.getPolicy());
        assertNull("already applied KSM is not cleared/replayed", nextFields.ksm);
        assertNull("still unsupported zero-pages remains deferred", nextFields.ksm);
        assertTrue(Boolean.TRUE.equals(nextFields.zram.enabled));
        MemoryStateVO state = repository.states(Collections.singletonList("host2"), 0, 1).get(0);
        assertTrue(state.getPolicyBlockedFields().contains("ksm.zeroPagesEnabled"));
        assertFalse(state.getPolicyBlockedFields().contains("zram.enabled"));
        ack.appliedRevision = next.getDesiredRevision();
        repository.result(next.getUuid(), "Succeeded", "newly supported ZRAM applied", ack);

        repository.transaction(em -> {
            MemoryStateVO current = em.find(MemoryStateVO.class, "host2");
            current.setCapabilities("{\"supported\":true,\"ksm\":true,\"ksmZeroPages\":true,\"zram\":true}");
            return null;
        });
        assertTrue(repository.applyEffectivePolicyAfterConnect("host2"));
        MemoryTaskVO third = repository.claim("mn").stream()
                .filter(task -> "host2".equals(task.getHostUuid()) && !task.getUuid().equals(first.getUuid())
                        && !task.getUuid().equals(next.getUuid())).findFirst().get();
        MemoryPolicyConfig thirdFields = MemoryPolicyRules.decode(third.getPolicy());
        assertNull("the previously applied KSM enable field must remain in the applied snapshot",
                thirdFields.ksm == null ? null : thirdFields.ksm.enabled);
        assertNull("the previously applied ZRAM field must remain in the applied snapshot", thirdFields.zram);
        assertTrue(Boolean.TRUE.equals(thirdFields.ksm.zeroPagesEnabled));
        ack.appliedRevision = third.getDesiredRevision();
        repository.result(third.getUuid(), "Succeeded", "newly supported zero-page policy applied", ack);
        for (int i = 0; i < 3; i++) { assertFalse(repository.applyEffectivePolicyAfterConnect("host2")); }
        assertEquals(3L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t where t.hostUuid = 'host2'", Long.class).getSingleResult()));
    }

    @Test public void clearedFinalHostKsmOverrideDoesNotBlockInheritedPlanOnDefaultFalseDisappearance() {
        repository.transaction(em -> {
            MemoryPolicyVO global = em.find(MemoryPolicyVO.class, "Global:global");
            global.setRevision(1);
            global.setPolicy("{\"schemaVersion\":1}");
            standardConfigAdapter.writeGlobal(em, "ksm.enabled", false);
            standardConfigAdapter.writeGlobal(em, "ksm.pagesToScan", 300L);
            standardConfigAdapter.writeGlobal(em, "ksm.sleepMillis", 20L);
            standardConfigAdapter.writeGlobal(em, "ksm.zeroPagesEnabled", false);
            MemoryPolicyVO host = new MemoryPolicyVO(); host.setUuid("Host:host1");
            host.setScope("Host"); host.setResourceUuid("host1"); host.setRevision(2);
            // The last local KSM override has already been cleared; the only
            // effective KSM fields are now inherited from Global. ZRAM and
            // writeback were previously applied as explicit false defaults.
            host.setPolicy("{\"schemaVersion\":1}"); em.persist(host);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Succeeded");
            state.setAppliedPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":false,\"pagesToScan\":300,\"sleepMillis\":20,\"zeroPagesEnabled\":false},\"zram\":{\"enabled\":false},\"writeback\":{\"enabled\":false}}");
            state.setAppliedPolicyHash("prior-full-policy");
            state.setCapabilities("{\"supported\":true,\"ksm\":true,\"zram\":false,\"writeback\":false}");
            return null;
        });

        assertFalse("no configuration payload should be dispatched for inert false defaults",
                repository.applyEffectivePolicyAfterConnect("host1"));
        MemoryStateVO planned = repository.states(Collections.singletonList("host1"), 0, 1).get(0);
        assertEquals("false ZRAM/writeback defaults disappearing from the sparse target are inert",
                "Applied", planned.getPolicyPlanStatus());
        assertEquals("{}", planned.getPolicyBlockedFields());
        MemoryPolicyConfig inherited = MemoryPolicyRules.decode(repository.getPolicy("Host", "host1").getEffectivePolicy());
        assertEquals(Boolean.FALSE, inherited.ksm.enabled);
        assertEquals(Long.valueOf(300), inherited.ksm.pagesToScan);
        assertEquals(Boolean.FALSE, inherited.ksm.zeroPagesEnabled);
    }

    @Test public void unsupportedExplicitZramEnableStillBlocksInheritedPlan() {
        repository.transaction(em -> {
            standardConfigAdapter.writeGlobal(em, "zram.enabled", true);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Succeeded");
            state.setAppliedPolicy("{\"schemaVersion\":1,\"zram\":{\"enabled\":false}}");
            state.setAppliedPolicyHash("prior-zram-disabled");
            state.setCapabilities("{\"supported\":true,\"zram\":false,\"zramReasonCode\":\"ABI_INCOMPLETE\"}");
            return null;
        });

        assertFalse("unsupported enable is blocked rather than automatically dispatched",
                repository.applyEffectivePolicyAfterConnect("host1"));
        MemoryStateVO planned = repository.states(Collections.singletonList("host1"), 0, 1).get(0);
        assertEquals("Blocked", planned.getPolicyPlanStatus());
        assertTrue(planned.getPolicyBlockedFields().contains("ABI_INCOMPLETE"));
        assertTrue(planned.getPolicyBlockedFields().contains("zram.enabled"));
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

    @Test public void freshObservationMakesNewHostEligibleWithoutInventingSucceededState() {
        repository.transaction(em -> {
            TestHost host = new TestHost(); host.uuid = "new-host"; host.hypervisorType = "KVM";
            host.clusterUuid = "cluster1"; host.status = HostStatus.Connected; em.persist(host);
            MemoryPolicyVO global = em.find(MemoryPolicyVO.class, "Global:global");
            global.setRevision(1); global.setPolicy("{\"schemaVersion\":1}");
            standardConfigAdapter.writeGlobal(em, "ksm.enabled", false);
            return null;
        });
        MemoryAgentResponse refresh = new MemoryAgentResponse();
        refresh.sampleTime = System.currentTimeMillis();
        refresh.capabilities = new HashMap<>();
        refresh.capabilities.put("supported", true); refresh.capabilities.put("ksm", true);
        refresh.capabilities.put("ksmZeroPages", true); refresh.capabilities.put("zram", false);
        refresh.state = Collections.<String, Object>emptyMap();
        repository.observe("new-host", refresh);
        MemoryStateVO refreshed = repository.states(Collections.singletonList("new-host"), 0, 1).get(0);
        assertEquals("the real first observation has no fabricated operation success", "Unknown", refreshed.getStatus());
        assertNotNull(refreshed.getLastSampleTime());
        assertTrue(repository.applyEffectivePolicyAfterConnect("new-host"));
        MemoryTaskVO task = repository.claim("mn").stream()
                .filter(item -> "new-host".equals(item.getHostUuid())).findFirst().get();
        MemoryPolicyConfig sent = MemoryPolicyRules.decode(task.getPolicy());
        assertFalse(Boolean.TRUE.equals(sent.ksm.enabled));
    }

    @Test public void historicalDrainWithoutAppliedPolicyHashIsNotAdoptedByReconnect() {
        repository.transaction(em -> {
            MemoryPolicyVO global = em.find(MemoryPolicyVO.class, "Global:global");
            global.setRevision(1); global.setPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":true}}");
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

    @Test public void explicitSuccessfulApplyResolvesMatchingLegacyPolicyReview() {
        seedLegacyPolicyNeedsReview("host1");
        APIUpdateMemoryPolicyMsg apply = hostApply("host1");
        apply.setExpectedRevision(1L);
        MemoryTaskInventory parent = repository.submit(apply);
        MemoryTaskVO task = repository.claim("mn").stream()
                .filter(row -> "host1".equals(row.getHostUuid())).findFirst().get();
        assertNull("ordinary API tasks do not carry a policy plan hash", task.getPolicyPlanHash());
        assertEquals("NeedsReview", repository.states(Collections.singletonList("host1"), 0, 1)
                .get(0).getPolicyPlanStatus());

        repository.result(task.getUuid(), "Succeeded", null, null);
        MemoryStateVO state = repository.states(Collections.singletonList("host1"), 0, 1).get(0);
        assertEquals("Applied", state.getPolicyPlanStatus());
        assertEquals("{}", state.getPolicyBlockedFields());
        assertNotNull(state.getAppliedPolicyHash());
        assertEquals("Queued", parent.getStatus());
    }

    @Test public void staleSuccessfulApplyDoesNotResolveLegacyPolicyReview() {
        seedLegacyPolicyNeedsReview("host1");
        APIUpdateMemoryPolicyMsg apply = hostApply("host1");
        apply.setExpectedRevision(1L);
        repository.submit(apply);
        MemoryTaskVO task = repository.claim("mn").stream()
                .filter(row -> "host1".equals(row.getHostUuid())).findFirst().get();
        repository.transaction(em -> {
            MemoryPolicyVO policy = em.find(MemoryPolicyVO.class, "Host:host1");
            policy.setRevision(policy.getRevision() + 1);
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.enabled", false);
            return null;
        });

        repository.result(task.getUuid(), "Succeeded", null, null);
        MemoryStateVO state = repository.states(Collections.singletonList("host1"), 0, 1).get(0);
        assertEquals("NeedsReview", state.getPolicyPlanStatus());
        assertTrue(state.getPolicyBlockedFields().contains("LEGACY_APPLIED_POLICY_UNKNOWN"));
    }

    @Test public void unknownApplyAndSuccessfulPauseOrDrainDoNotResolveLegacyPolicyReview() {
        seedLegacyPolicyNeedsReview("host1");
        APIUpdateMemoryPolicyMsg apply = hostApply("host1");
        apply.setExpectedRevision(1L);
        repository.submit(apply);
        MemoryTaskVO unknown = repository.claim("mn").stream()
                .filter(row -> "host1".equals(row.getHostUuid())).findFirst().get();
        repository.result(unknown.getUuid(), "Unknown", "timeout", null);
        assertEquals("NeedsReview", repository.states(Collections.singletonList("host1"), 0, 1)
                .get(0).getPolicyPlanStatus());

        for (String action : Arrays.asList("pause", "drain")) {
            String taskUuid = "legacy-" + action;
            repository.transaction(em -> {
                MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
                state.setActiveTaskUuid(taskUuid);
                MemoryTaskVO task = testTask(taskUuid, null, action, "Applying", null);
                task.setPolicyPlanHash(null);
                em.persist(task);
                return null;
            });
            repository.result(taskUuid, "Succeeded", null, null);
            assertEquals("NeedsReview", repository.states(Collections.singletonList("host1"), 0, 1)
                    .get(0).getPolicyPlanStatus());
        }
    }

    private void seedLegacyPolicyNeedsReview(String hostUuid) {
        repository.transaction(em -> {
            MemoryPolicyVO policy = new MemoryPolicyVO();
            policy.setUuid("Host:" + hostUuid); policy.setScope("Host"); policy.setResourceUuid(hostUuid);
            policy.setRevision(1); policy.setPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":true}}");
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

    private APIUpdateMemoryPolicyMsg hostApply(String hostUuid) {
        APIUpdateMemoryPolicyMsg apply = request(0);
        apply.setScope("Host"); apply.setResourceUuid(hostUuid); apply.setTargetHostUuids(null);
        apply.setExpectedGlobalRevision(0L); apply.setPolicy("{\"ksm\":{\"enabled\":true}}");
        return apply;
    }

    @Test public void unavailableOrUnknownPeerDoesNotBlockEligibleFreshHost() {
        insertFreshBootstrap("ffffffffffffffffffffffffffffffff");
        repository.transaction(em -> {
            TestHost host = em.find(TestHost.class, "host2"); host.status = HostStatus.Disconnected;
            MemoryStateVO state = em.find(MemoryStateVO.class, "host2"); state.setStatus("Unknown");
            state.setActiveTaskUuid(null);
            return null;
        });
        assertNull(repository.freshCloudBootstrapBlockReason());
        repository.submitFreshCloudBootstrap("ffffffffffffffffffffffffffffffff");
        List<MemoryTaskVO> children = repository.claim("mn").stream()
                .filter(task -> task.getHostUuid() != null).collect(java.util.stream.Collectors.toList());
        assertEquals(Collections.singletonList("host1"), children.stream().map(MemoryTaskVO::getHostUuid)
                .collect(java.util.stream.Collectors.toList()));
        MemoryStateVO deferred = repository.states(Collections.singletonList("host2"), 0, 1).get(0);
        assertEquals("Blocked", deferred.getPolicyPlanStatus());
        assertTrue(deferred.getPolicyBlockedFields().contains("HOST_NOT_CONNECTED"));
    }

    @Test public void unknownApplyPlanIsNotAutomaticallyResubmitted() {
        repository.transaction(em -> {
            MemoryPolicyVO explicit = new MemoryPolicyVO(); explicit.setUuid("Host:host1");
            explicit.setScope("Host"); explicit.setResourceUuid("host1"); explicit.setRevision(1);
            explicit.setPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":false}}"); em.persist(explicit);
            return null;
        });
        assertTrue(repository.applyEffectivePolicyAfterConnect("host1"));
        MemoryTaskVO task = repository.claim("mn").stream().filter(t -> "host1".equals(t.getHostUuid())).findFirst().get();
        repository.result(task.getUuid(), "Unknown", "timeout", null);
        assertFalse(repository.applyEffectivePolicyAfterConnect("host1"));
        assertEquals(1L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t where t.hostUuid = 'host1'", Long.class).getSingleResult()));
    }

    @Test public void explicitUserKsmPolicyCancelsPendingFreshBootstrap() {
        insertFreshBootstrap("cccccccccccccccccccccccccccccccc");
        repository.transaction(em -> {
            MemoryPolicyVO global = em.find(MemoryPolicyVO.class, "Global:global");
            global.setRevision(1); global.setPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":false}}");
            return null;
        });
        assertEquals("EXPLICIT_MEMORY_POLICY_PRESENT", repository.freshCloudBootstrapBlockReason());
        assertEquals(MemoryCloudBootstrapVO.CANCELLED, repository.freshCloudBootstrap().getStatus());
        assertEquals(0L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t", Long.class).getSingleResult()));
    }

    @Test public void newHostUsesCurrentInheritedPolicyWithoutCreatingOverrideOrRepeating() {
        insertFreshBootstrap("dddddddddddddddddddddddddddddddd");
        assertNotNull(repository.submitFreshCloudBootstrap("dddddddddddddddddddddddddddddddd"));
        repository.transaction(em -> {
            TestHost host = new TestHost(); host.uuid = "host3"; host.hypervisorType = "KVM";
            host.clusterUuid = "cluster1"; host.status = HostStatus.Connected; em.persist(host);
            MemoryStateVO state = new MemoryStateVO(); state.setHostUuid("host3"); state.setStatus("Succeeded");
            state.setLastSampleTime(System.currentTimeMillis());
            state.setCapabilities("{\"supported\":true,\"ksm\":true,\"ksmZeroPages\":true}");
            state.setState("{}"); em.persist(state);
            MemoryPolicyVO override = new MemoryPolicyVO(); override.setUuid("Host:host3"); override.setScope("Host");
            override.setResourceUuid("host3"); override.setRevision(1);
            override.setPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":false}}"); em.persist(override);
            return null;
        });
        assertTrue(repository.applyEffectivePolicyAfterConnect("host3"));
        assertFalse(repository.applyEffectivePolicyAfterConnect("host3"));
        MemoryTaskVO child = repository.claim("mn").stream().filter(t -> "host3".equals(t.getHostUuid())).findFirst().get();
        MemoryPolicyConfig sent = MemoryPolicyRules.decode(child.getPolicy());
        assertFalse(Boolean.TRUE.equals(sent.ksm.enabled));
        assertTrue(Boolean.TRUE.equals(sent.ksm.zeroPagesEnabled));
        MemoryAgentResponse response = new MemoryAgentResponse(); response.appliedRevision = child.getDesiredRevision();
        repository.result(child.getUuid(), "Succeeded", "applied", response);
        assertNotNull(repository.transaction(em -> em.find(MemoryStateVO.class, "host3").getAppliedPolicyHash()));
        assertFalse(repository.applyEffectivePolicyAfterConnect("host3"));
        assertEquals(1L, repository.getPolicy("Host", "host3").getRevision());
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
            explicit.setScope("Host"); explicit.setResourceUuid("host1"); explicit.setRevision(1);
            explicit.setPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":false}}");
            em.persist(explicit);
            return null;
        });
        assertTrue(repository.applyEffectivePolicyAfterConnect("host1"));
        MemoryTaskVO child = repository.claim("mn").stream()
                .filter(task -> "host1".equals(task.getHostUuid())).findFirst().get();
        assertFalse(Boolean.TRUE.equals(MemoryPolicyRules.decode(child.getPolicy()).ksm.enabled));
        assertEquals("Applying", repository.findTask(child.getUuid()).getStatus());
        assertEquals(1L, repository.getPolicy("Host", "host1").getRevision());
    }

    private void insertFreshBootstrap(String requestUuid) {
        repository.transaction(em -> {
            MemoryCloudBootstrapVO row = new MemoryCloudBootstrapVO();
            row.setUuid(MemoryCloudBootstrapVO.GLOBAL); row.setRequestUuid(requestUuid);
            row.setStatus(MemoryCloudBootstrapVO.PENDING); row.setReason("pending"); em.persist(row);
            return null;
        });
    }

    @Test public void failedHostTaskCreatesOneDurableFailureIntentAndDeliveryIsIdempotent() {
        APIUpdateMemoryPolicyMsg msg = request(0);
        msg.setScope("Host"); msg.setResourceUuid("host1"); msg.setTargetHostUuids(null);
        msg.setExpectedGlobalRevision(0L);
        MemoryTaskInventory submitted = repository.submit(msg);
        assertEquals(submitted.getUuid(), repository.submit(msg).getUuid());

        MemoryTaskVO child = repository.claim("mn-a").get(0);
        repository.result(child.getUuid(), "Failed", "agent rejected policy", null);
        repository.result(child.getUuid(), "Failed", "duplicate callback", null);
        assertEquals("replayed request UUID must resolve to the same failed task",
                submitted.getUuid(), repository.submit(msg).getUuid());

        MemoryTaskFailureOutboxVO outbox = repository.transaction(em -> em.find(
                MemoryTaskFailureOutboxVO.class, child.getUuid()));
        assertNotNull("Failed transition and durable notification intent share one transaction", outbox);
        assertEquals("host1", outbox.getHostUuid());
        assertEquals(child.getUuid(), outbox.getTaskUuid());
        assertEquals("apply", outbox.getAction());
        assertEquals("agent rejected policy", outbox.getReason());
        assertEquals("Failed", outbox.toEvent().getStatus());

        List<MemoryTaskFailureOutboxVO> claimed = repository.claimFailureNotifications("mn-a");
        assertEquals(1, claimed.size());
        assertTrue(repository.claimFailureNotifications("mn-b").isEmpty());
        repository.completeFailureNotification(child.getUuid(), "mn-a", true, null);
        assertTrue(repository.claimFailureNotifications("mn-b").isEmpty());
        assertTrue(repository.transaction(em -> em.find(MemoryTaskFailureOutboxVO.class, child.getUuid()).isDelivered()));
    }

    @Test public void unknownTaskResultDoesNotCreateFailureIntent() {
        APIUpdateMemoryPolicyMsg msg = request(0);
        msg.setScope("Host"); msg.setResourceUuid("host1"); msg.setTargetHostUuids(null);
        msg.setExpectedGlobalRevision(0L);
        repository.submit(msg);
        MemoryTaskVO child = repository.claim("mn-a").get(0);

        repository.result(child.getUuid(), "Unknown", "request timed out", null);

        assertNull("Unknown/timeout is not a Failed task", repository.transaction(em ->
                em.find(MemoryTaskFailureOutboxVO.class, child.getUuid())));
    }

    @Test public void globalAndClusterChildFailuresNotifyTheActualHostNotTheAggregateParent() {
        APIUpdateMemoryPolicyMsg global = request(0);
        MemoryTaskInventory parent = repository.submit(global);
        List<MemoryTaskVO> globalChildren = repository.claim("mn-a");
        for (MemoryTaskVO child : globalChildren) {
            repository.result(child.getUuid(), "Failed", "global task failure", null);
        }
        assertEquals(2, repository.claimFailureNotifications("mn-a").size());
        assertNull(repository.transaction(em -> em.find(MemoryTaskFailureOutboxVO.class, parent.getUuid())));
        APIUpdateMemoryPolicyMsg cluster = request(0);
        cluster.setScope("Cluster"); cluster.setResourceUuid("cluster1"); cluster.setExpectedGlobalRevision(1L);
        MemoryTaskInventory clusterParent = repository.submit(cluster);
        for (MemoryTaskVO child : repository.claim("mn-b")) {
            repository.result(child.getUuid(), "Failed", "cluster task failure", null);
        }
        assertEquals(2, repository.claimFailureNotifications("mn-b").size());
        assertNull(repository.transaction(em -> em.find(MemoryTaskFailureOutboxVO.class, clusterParent.getUuid())));
    }

    @Test public void backendPreparationKeepsPolicyAndDrainFenceAndUsesExistingTaskIdempotency() {
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Succeeded"); state.setControlOperationUuid("completed-drain"); return null;
        });
        APIUpdateMemoryPolicyMsg msg = request(0);
        msg.setScope("Host"); msg.setResourceUuid("host1"); msg.setTargetHostUuids(null);
        msg.setAction(MemoryBackendPreparationRules.ACTION); msg.setPolicy("{}");
        msg.setExpectedGlobalRevision(0L); msg.setExpectedControlOperationUuid("completed-drain");
        msg.setBackendPreparation(MemoryBackendPreparationRulesTest.preparation());
        MemoryTaskInventory parent = repository.submit(msg);
        assertEquals(parent.getUuid(), repository.submit(msg).getUuid());
        assertEquals(0L, repository.getPolicy("Host", "host1").getRevision());
        assertEquals("{\"schemaVersion\":1}", repository.getPolicy("Host", "host1").getPolicy());
        MemoryStateVO state = repository.states(Collections.singletonList("host1"), 0, 10).get(0);
        assertEquals("completed-drain", state.getControlOperationUuid());
        assertNotNull(state.getActiveTaskUuid());
        MemoryTaskVO child = repository.claim("mn-a").get(0);
        assertEquals(MemoryBackendPreparationRules.ACTION, child.getAction());
        assertTrue(child.getPolicy().contains("candidateId"));
        msg.getBackendPreparation().setBackendCapacityBytes(24L << 30);
        try { repository.submit(msg); fail("reusing request with a different device contract must fail"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_IDEMPOTENCY_CONFLICT", expected.getCode()); }
        msg.setClientRequestUuid(UUID.randomUUID().toString());
        try { repository.submit(msg); fail("unresolved preparation must serialize with other Host tasks"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_HOST_BUSY", expected.getCode()); }
        repository.result(child.getUuid(), "Succeeded", "prepared; optimization still disabled", null);
        msg.setExpectedControlOperationUuid("stale-drain");
        try { repository.submit(msg); fail("stale control anchor must fail"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_CONTROL_OPERATION_FENCED", expected.getCode()); }
    }

    @Test public void zramPoolPreparationKeepsDrainFenceAndDoesNotChangePolicy() {
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Succeeded"); state.setControlOperationUuid("completed-drain"); return null;
        });
        APIUpdateMemoryPolicyMsg msg = request(0);
        msg.setScope("Host"); msg.setResourceUuid("host1"); msg.setTargetHostUuids(null);
        msg.setAction(MemoryZramPoolPreparationRules.ACTION); msg.setPolicy("{}");
        msg.setExpectedGlobalRevision(0L); msg.setExpectedControlOperationUuid("completed-drain");
        msg.setPoolPreparation(MemoryZramPoolPreparationRulesTest.preparation());
        MemoryTaskInventory parent = repository.submit(msg);
        assertEquals(parent.getUuid(), repository.submit(msg).getUuid());
        assertEquals(0L, repository.getPolicy("Host", "host1").getRevision());
        MemoryStateVO state = repository.states(Collections.singletonList("host1"), 0, 10).get(0);
        assertEquals("completed-drain", state.getControlOperationUuid());
        assertNotNull(state.getActiveTaskUuid());
        MemoryTaskVO child = repository.claim("mn-a").get(0);
        assertEquals(MemoryZramPoolPreparationRules.ACTION, child.getAction());
        assertTrue(child.getPolicy().contains("expectedPoolGeneration"));
        repository.result(child.getUuid(), "Succeeded", "pool prepared; optimization remains disabled", null);
        msg.setExpectedControlOperationUuid("stale-drain");
        msg.setClientRequestUuid(UUID.randomUUID().toString());
        try { repository.submit(msg); fail("stale drain anchor must fail"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_CONTROL_OPERATION_FENCED", expected.getCode()); }
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
        APIUpdateMemoryPolicyMsg resume = request(0);
        resume.setScope("Host"); resume.setResourceUuid("host1"); resume.setTargetHostUuids(null);
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
        APIUpdateMemoryPolicyMsg reconcile = request(0);
        reconcile.setScope("Host"); reconcile.setResourceUuid("host1"); reconcile.setTargetHostUuids(null);
        reconcile.setAction("reconcile"); reconcile.setPolicy("{}");
        reconcile.setExpectedControlOperationUuid("prior-drain");
        MemoryTaskInventory parent = repository.submit(reconcile);
        MemoryTaskVO child = repository.claim("mn-a").stream()
                .filter(task -> parent.getUuid().equals(task.getParentUuid())).findFirst().get();
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
        APIUpdateMemoryPolicyMsg reconcile = request(0);
        reconcile.setScope("Host"); reconcile.setResourceUuid("host1"); reconcile.setTargetHostUuids(null);
        reconcile.setAction("reconcile"); reconcile.setPolicy("{}");
        reconcile.setExpectedControlOperationUuid("prior-pause");
        MemoryTaskInventory parent = repository.submit(reconcile);
        MemoryTaskVO child = repository.claim("mn-a").stream()
                .filter(task -> parent.getUuid().equals(task.getParentUuid())).findFirst().get();
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

        APIUpdateMemoryPolicyMsg reconcile = request(0);
        reconcile.setScope("Host"); reconcile.setResourceUuid("host1");
        reconcile.setTargetHostUuids(null); reconcile.setAction("reconcile"); reconcile.setPolicy("{}");
        MemoryTaskInventory accepted = repository.submit(reconcile);
        MemoryTaskVO child = repository.claim("mn-a").stream()
                .filter(task -> accepted.getUuid().equals(task.getParentUuid())).findFirst().get();
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

        APIUpdateMemoryPolicyMsg reconcile = request(0);
        reconcile.setScope("Host"); reconcile.setResourceUuid("host1");
        reconcile.setTargetHostUuids(null); reconcile.setAction("reconcile"); reconcile.setPolicy("{}");
        MemoryTaskInventory accepted = repository.submit(reconcile);
        MemoryTaskVO child = repository.claim("mn-a").stream()
                .filter(task -> accepted.getUuid().equals(task.getParentUuid())).findFirst().get();
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
        APIUpdateMemoryPolicyMsg reconcile = request(0);
        reconcile.setScope("Host"); reconcile.setResourceUuid("host1");
        reconcile.setTargetHostUuids(null); reconcile.setAction("reconcile"); reconcile.setPolicy("{}");
        try { repository.submit(reconcile); fail("must not overwrite an unrelated Host control owner"); }
        catch (MemoryOperationException expected) {
            assertEquals("MEMORY_CONTROL_OPERATION_FENCED", expected.getCode());
        }
        assertEquals("different-control", repository.states(Collections.singletonList("host1"), 0, 1)
                .get(0).getControlOperationUuid());
    }

    @Test public void partialLifecycleSampleDoesNotDropKnownManagedOwnership() {
        long first = System.currentTimeMillis();
        MemoryAgentResponse managed = new MemoryAgentResponse();
        managed.status = "Succeeded";
        managed.sampleTime = first;
        managed.state = Collections.<String, Object>singletonMap("managed", true);
        repository.observe("host1", managed);
        assertTrue(repository.hasManagedOwnership("host1"));

        MemoryAgentResponse lifecycleAck = new MemoryAgentResponse();
        lifecycleAck.status = "Succeeded";
        lifecycleAck.sampleTime = first + 1;
        lifecycleAck.state = Collections.<String, Object>singletonMap("phase", "PAUSED");
        repository.observe("host1", lifecycleAck);
        assertTrue("partial ACK must not make a managed host look unowned",
                repository.hasManagedOwnership("host1"));

        MemoryAgentResponse explicitUnmanaged = new MemoryAgentResponse();
        explicitUnmanaged.status = "Succeeded";
        explicitUnmanaged.sampleTime = first + 2;
        explicitUnmanaged.state = Collections.<String, Object>singletonMap("managed", false);
        repository.observe("host1", explicitUnmanaged);
        assertFalse("an explicit ownership transition is authoritative",
                repository.hasManagedOwnership("host1"));
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

        APIUpdateMemoryPolicyMsg update = request(0);
        update.setTargetHostUuids(Collections.singletonList("host1"));
        repository.submit(update);
        MemoryTaskVO task = repository.claim("mn").get(0);
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

    @Test public void reconcileAckWithoutSampleTimeStillUpdatesAppliedRevision() {
        APIUpdateMemoryPolicyMsg update = request(0);
        update.setTargetHostUuids(Collections.singletonList("host1"));
        update.setPolicy("{}"); // This case exercises readback merge behavior, not policy capability admission.
        repository.submit(update);
        repository.transaction(em -> { em.find(MemoryStateVO.class, "host1").setLastSampleTime(null); return null; });
        MemoryTaskVO task = repository.claim("mn").get(0);
        MemoryAgentResponse ack = new MemoryAgentResponse();
        ack.status = "Succeeded";
        ack.appliedRevision = 9L;
        repository.result(task.getUuid(), "Succeeded", null, ack);
        MemoryStateVO state = repository.states(Collections.singletonList("host1"), 0, 1).get(0);
        assertEquals(Long.valueOf(9L), state.getAppliedRevision());
        assertNull("missing sample time must not invent a monitoring sample", state.getLastSampleTime());
    }

    @Test public void completeSampleWithoutCapabilitiesClearsPreviousCapabilityEnvelope() {
        long sample = System.currentTimeMillis();
        MemoryAgentResponse initial = new MemoryAgentResponse();
        initial.status = "Succeeded"; initial.sampleTime = sample;
        initial.capabilities = Collections.<String, Object>singletonMap("zram", true);
        initial.state = completeState(11L, "old");
        repository.observe("host1", initial);

        APIUpdateMemoryPolicyMsg update = request(0);
        update.setTargetHostUuids(Collections.singletonList("host1"));
        update.setPolicy("{}"); // Keep the test focused on monitoring-envelope preservation.
        repository.submit(update);
        MemoryTaskVO task = repository.claim("mn").get(0);
        MemoryAgentResponse next = new MemoryAgentResponse();
        next.status = "Succeeded"; next.sampleTime = sample + 1;
        next.state = completeState(12L, "new");
        // A complete observation with no capability readback must not expose
        // the previous host capability result as if it were current.
        repository.result(task.getUuid(), "Succeeded", null, next);

        MemoryStateVO state = repository.states(Collections.singletonList("host1"), 0, 1).get(0);
        assertNull(state.getCapabilities());
        assertEquals(Long.valueOf(sample + 1), state.getLastSampleTime());
        assertTrue(state.getState().contains("12"));
    }

    @Test public void unknownMonitoringEnvelopeIsNotAcceptedAsFreshTaskSample() {
        long sample = System.currentTimeMillis();
        MemoryAgentResponse initial = new MemoryAgentResponse();
        initial.status = "Succeeded"; initial.sampleTime = sample;
        initial.state = completeState(11L, "old");
        repository.observe("host1", initial);

        APIUpdateMemoryPolicyMsg update = request(0);
        update.setTargetHostUuids(Collections.singletonList("host1"));
        update.setPolicy("{}"); // Monitoring-envelope behavior is independent from feature admission.
        repository.submit(update);
        MemoryTaskVO task = repository.claim("mn").get(0);
        MemoryAgentResponse unknown = new MemoryAgentResponse();
        unknown.status = "Unknown"; unknown.sampleTime = sample + 1;
        unknown.state = new HashMap<>();
        unknown.state.put("actual", new HashMap<>());
        unknown.state.put("savings", new HashMap<String, Object>() {{
            put("formulaVersion", "mechanism-estimate-v1"); put("quality", "Unknown");
        }});
        repository.result(task.getUuid(), "Unknown", "collector unavailable", unknown);

        MemoryStateVO state = repository.states(Collections.singletonList("host1"), 0, 1).get(0);
        assertEquals(Long.valueOf(sample), state.getLastSampleTime());
        Map observed = org.zstack.utils.gson.JSONObjectUtil.toObject(state.getState(), Map.class);
        assertEquals(11L, ((Number) ((Map) observed.get("savings")).get("totalSavedEstimateBytes")).longValue());
        assertFalse(state.getState().contains("Unknown"));
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

    @Test public void partialCompleteSampleFromOldKernelReplacesObservationWithoutFabricatingZero() {
        long sample = System.currentTimeMillis();
        MemoryAgentResponse initial = new MemoryAgentResponse();
        initial.status = "Succeeded"; initial.sampleTime = sample;
        initial.state = completeState(11L, "old"); repository.observe("host1", initial);
        APIUpdateMemoryPolicyMsg update = request(0); update.setTargetHostUuids(Collections.singletonList("host1"));
        update.setPolicy("{}"); // Keep this old-kernel monitoring test independent from feature admission.
        repository.submit(update); MemoryTaskVO task = repository.claim("mn").get(0);
        MemoryAgentResponse partial = new MemoryAgentResponse(); partial.status = "Succeeded"; partial.sampleTime = sample + 1;
        partial.state = new HashMap<>();
        Map<String, Object> actual = new HashMap<>(); actual.put("ksm", Collections.emptyMap());
        actual.put("zram", Collections.emptyMap()); actual.put("writeback", Collections.emptyMap());
        partial.state.put("actual", actual);
        Map<String, Object> savings = new HashMap<>(); savings.put("formulaVersion", "mechanism-estimate-v1");
        savings.put("quality", "Partial"); savings.put("sampleTime", sample + 1);
        for (String field : Arrays.asList("ksmOrdinaryBytes", "ksmZeroBytes", "ksmTotalBytes",
                "zramBytes", "totalSavedEstimateBytes")) { savings.put(field, null); }
        partial.state.put("savings", savings);
        repository.result(task.getUuid(), "Succeeded", null, partial);
        MemoryStateVO state = repository.states(Collections.singletonList("host1"), 0, 1).get(0);
        assertEquals(Long.valueOf(sample + 1), state.getLastSampleTime());
        // Inspect the statistic, not the JSON text: a timestamp may contain "11".
        Map observed = org.zstack.utils.gson.JSONObjectUtil.toObject(state.getState(), Map.class);
        Map observedSavings = (Map) observed.get("savings");
        assertEquals("Partial", observedSavings.get("quality"));
        for (String field : Arrays.asList("ksmOrdinaryBytes", "ksmZeroBytes", "ksmTotalBytes",
                "zramBytes", "totalSavedEstimateBytes")) {
            assertNull("Unsupported statistic must not retain an older value: " + field, observedSavings.get(field));
        }
        assertFalse(((Map) ((Map) observed.get("actual")).get("zram")).containsKey("value"));
    }

    @Test public void olderSparseAckCannotOverwriteNewerObservedControl() {
        long sample = System.currentTimeMillis();
        APIUpdateMemoryPolicyMsg update = request(0);
        update.setTargetHostUuids(Collections.singletonList("host1"));
        update.setPolicy("{}"); // The test validates unknown samples, not enabling KSM.
        repository.submit(update);
        MemoryTaskVO task = repository.claim("mn").get(0);
        MemoryAgentResponse observation = new MemoryAgentResponse();
        observation.sampleTime = sample; observation.appliedRevision = 9L;
        observation.state = Collections.<String, Object>singletonMap("phase", "RUNNING");
        repository.observe("host1", observation);
        MemoryAgentResponse ack = new MemoryAgentResponse();
        ack.sampleTime = sample - 1; ack.appliedRevision = 8L;
        ack.state = Collections.<String, Object>singletonMap("phase", "PAUSED");
        repository.result(task.getUuid(), "Succeeded", null, ack);
        MemoryStateVO state = repository.states(Collections.singletonList("host1"), 0, 1).get(0);
        assertEquals(Long.valueOf(9), state.getAppliedRevision());
        assertEquals(Long.valueOf(sample), state.getLastSampleTime());
        assertTrue(state.getState().contains("RUNNING"));
        assertFalse(state.getState().contains("PAUSED"));
    }

    @Test public void firstTakeoverFenceBlocksLegacyWriterBeforeManagedFlag() {
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            if (state == null) { state = new MemoryStateVO(); state.setHostUuid("host1"); state.setStatus("Unknown"); em.persist(state); }
            state.setActiveTaskUuid("takeover-in-flight");
            return null;
        });
        assertTrue("an in-flight first takeover is ownership-protected",
                repository.hasManagedOwnership("host1"));
        repository.transaction(em -> {
            em.find(MemoryStateVO.class, "host1").setActiveTaskUuid(null);
            return null;
        });
        assertFalse(repository.hasManagedOwnership("host1"));
    }

    @Test public void legacyOwnershipCheckSharesGlobalTakeoverLock() throws Exception {
        java.util.concurrent.CountDownLatch locked = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        Thread takeover = new Thread(() -> repository.transaction(em -> {
            em.find(MemoryPolicyVO.class, "Global:global", LockModeType.PESSIMISTIC_WRITE);
            locked.countDown();
            try { release.await(5, java.util.concurrent.TimeUnit.SECONDS); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return null;
        }));
        takeover.start();
        assertTrue(locked.await(2, java.util.concurrent.TimeUnit.SECONDS));
        java.util.concurrent.atomic.AtomicBoolean finished = new java.util.concurrent.atomic.AtomicBoolean();
        Thread legacy = new Thread(() -> { repository.hasManagedOwnership("host1"); finished.set(true); });
        legacy.start();
        Thread.sleep(150);
        assertFalse("legacy check must wait behind takeover GLOBAL fence", finished.get());
        release.countDown();
        takeover.join(3000); legacy.join(3000);
        assertTrue(finished.get());
    }

    @Test public void productionSqlBatchJoinsOuterLegacyTransactionAndSerializesFirstTakeover() throws Exception {
        // Use the production transaction() (not the fixture override), actual
        // woven SQLBatchWithReturn and Spring's JPA transaction manager. This
        // proves that returning from hasManagedOwnership does not release the
        // GLOBAL lock while the legacy caller still owns its outer transaction.
        org.springframework.orm.jpa.JpaTransactionManager manager =
                new org.springframework.orm.jpa.JpaTransactionManager(factory);
        org.springframework.transaction.aspectj.AnnotationTransactionAspect aspect =
                org.springframework.transaction.aspectj.AnnotationTransactionAspect.aspectOf();
        Field managerField = org.springframework.transaction.interceptor.TransactionAspectSupport.class
                .getDeclaredField("transactionManager");
        managerField.setAccessible(true);
        Object previousManager = managerField.get(aspect);
        EntityManager shared = org.springframework.orm.jpa.SharedEntityManagerCreator
                .createSharedEntityManager(factory);
        MemoryRepository production = new MemoryRepository();
        Field dbf = MemoryRepository.class.getDeclaredField("dbf");
        dbf.setAccessible(true);
        dbf.set(production, Proxy.newProxyInstance(org.zstack.core.db.DatabaseFacade.class.getClassLoader(),
                new Class<?>[]{org.zstack.core.db.DatabaseFacade.class}, (proxy, method, args) -> {
                    if ("getEntityManager".equals(method.getName())) { return shared; }
                    throw new UnsupportedOperationException(method.getName());
                }));
        java.util.concurrent.ExecutorService threads = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CountDownLatch checked = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        try {
            aspect.setTransactionManager(manager);
            java.util.concurrent.Future<?> legacy = threads.submit(() ->
                    new org.springframework.transaction.support.TransactionTemplate(manager).execute(status -> {
                        assertFalse(production.hasManagedOwnership("host1"));
                        checked.countDown();
                        try { assertTrue(release.await(5, java.util.concurrent.TimeUnit.SECONDS)); }
                        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
                        return null;
                    }));
            assertTrue(checked.await(3, java.util.concurrent.TimeUnit.SECONDS));
            java.util.concurrent.Future<?> takeover = threads.submit(() -> production.submit(request(0)));
            try {
                takeover.get(150, java.util.concurrent.TimeUnit.MILLISECONDS);
                fail("first submit must wait until legacy outer transaction commits");
            } catch (java.util.concurrent.TimeoutException expected) { }
            release.countDown();
            legacy.get(5, java.util.concurrent.TimeUnit.SECONDS);
            takeover.get(5, java.util.concurrent.TimeUnit.SECONDS);
            // The opposite admission order now sees the committed active task,
            // despite no Agent managed=true ACK having been received.
            assertTrue(production.hasManagedOwnership("host1"));
        } finally {
            release.countDown();
            threads.shutdownNow();
            try { assertTrue(threads.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)); }
            finally { managerField.set(aspect, previousManager); }
        }
    }

    @Test public void currentStateSelectionRejectsAnyNonCurrentKvmHostWithoutChangingHistory() {
        repository.transaction(em -> {
            TestHost host = em.find(TestHost.class, "host2");
            host.hypervisorType = "ESX"; em.merge(host); return null;
        });
        for (List<String> selected : Arrays.asList(Collections.singletonList("missing"),
                Arrays.asList("host1", "missing"), Collections.singletonList("host2"),
                Arrays.asList("host1", "host2"), Collections.singletonList("cluster1"))) {
            try { repository.statePage(selected, 0, 10, null); fail("invalid current Host selection accepted: " + selected); }
            catch (MemoryOperationException expected) { assertEquals("MEMORY_INVALID_TARGETS", expected.getCode()); }
        }
        assertEquals(repository.statePage(Collections.singletonList("host1"), 0, 10, null).total,
                repository.statePage(Arrays.asList("host1", "host1"), 0, 10, null).total);
        assertEquals(repository.statePage(null, 0, 10, null).total,
                repository.statePage(Collections.emptyList(), 0, 10, null).total);
        APIQueryMemoryTaskMsg history = new APIQueryMemoryTaskMsg();
        history.setHostUuid("missing"); history.setStart(0); history.setLimit(10);
        assertTrue("historical task filters may name a deleted Host", repository.tasks(history).isEmpty());
    }

    @Test public void stateSnapshotRejectsMutationBetweenPages() {
        MemoryAgentResponse response = new MemoryAgentResponse(); response.status = "Succeeded";
        response.sampleTime = System.currentTimeMillis(); response.state = Collections.singletonMap("managed", false);
        repository.observe("host1", response); repository.observe("host2", response);
        MemoryQueryPage<MemoryStateVO> first = repository.statePage(null, 0, 1, null);
        repository.transaction(em -> { MemoryStateVO state = em.find(MemoryStateVO.class, "host2"); em.remove(state); return null; });
        try {
            repository.statePage(null, 1, 1, first.snapshotId);
            fail("a changed state set must not be paged with an old snapshot");
        } catch (MemoryOperationException expected) {
            assertEquals("MEMORY_QUERY_SNAPSHOT_CHANGED", expected.getCode());
        }
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
        MemoryQueryPage<MemoryStateVO> page = repository.statePage(null, 0, 10, null);
        assertEquals(1L, page.total);
        assertEquals("host2", page.items.get(0).getHostUuid());
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
        assertEquals(1L, repository.statePage(null, 0, 10, null).total);
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
        repository.submit(request(0));
        repository.transaction(em -> {
            em.createQuery("delete from HostVO h where h.uuid = 'host1'").executeUpdate(); return null;
        });
        List<MemoryTaskVO> claimed = repository.claim("mn");
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
        row.sourceRevisions = "{}"; row.retained = true; em.persist(row);
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

    @Test public void taskSnapshotRejectsStatusChangeBetweenPages() {
        repository.submit(request(0));
        APIQueryMemoryTaskMsg query = new APIQueryMemoryTaskMsg(); query.setLimit(1); query.setStatus("Queued");
        MemoryQueryPage<MemoryTaskInventory> first = repository.taskPage(query);
        assertNotNull(first.snapshotId);
        repository.transaction(em -> {
            MemoryTaskVO task = em.createQuery("from MemoryTaskVO t order by t.createDate desc", MemoryTaskVO.class)
                    .setMaxResults(1).getSingleResult();
            task.setStatus("Unknown"); return null;
        });
        query.setSnapshotId(first.snapshotId);
        try { repository.taskPage(query); fail("status mutation must invalidate the task snapshot"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_QUERY_SNAPSHOT_CHANGED", expected.getCode()); }
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

    @Test public void statePagesMaterializeOnlyTheRequestedRows() {
        populateQueryScaleFixture();
        factory.getStatistics().clear();
        MemoryQueryPage<MemoryStateVO> states = repository.statePage(null, 500, 7, null);
        assertEquals(1202L, states.total); assertEquals(7, states.items.size());
        assertTrue("State page must not materialize all matching UUIDs", queryRows() <= 8);
    }

    @Test public void taskPagesMaterializeOnlyTheRequestedRows() {
        populateQueryScaleFixture();
        APIQueryMemoryTaskMsg msg = new APIQueryMemoryTaskMsg(); msg.setStart(500); msg.setLimit(7);
        factory.getStatistics().clear();
        MemoryQueryPage<MemoryTaskInventory> tasks = repository.taskPage(msg);
        assertEquals(1200L, tasks.total); assertEquals(7, tasks.items.size());
        assertTrue("Task page must not materialize all matching UUIDs or dates", queryRows() <= 8);
        msg.setSnapshotId(tasks.snapshotId); msg.setStart(507);
        MemoryQueryPage<MemoryTaskInventory> next = repository.taskPage(msg);
        assertEquals(tasks.snapshotId, next.snapshotId);
        Set<String> ids = new HashSet<>(); tasks.items.forEach(t -> ids.add(t.getUuid()));
        assertTrue(next.items.stream().noneMatch(t -> ids.contains(t.getUuid())));
    }

    @Test public void rejectedSubmissionDoesNotCommitQueryVersion() {
        repository.submit(request(0));
        APIQueryMemoryTaskMsg query = new APIQueryMemoryTaskMsg();
        String snapshot = repository.taskPage(query).snapshotId;
        long revision = repository.getPolicy("Global", "global").getRevision();
        try { repository.submit(request(revision)); fail("Active task must block a second submission"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_HOST_BUSY", expected.getCode()); }
        query.setSnapshotId(snapshot);
        assertEquals(snapshot, repository.taskPage(query).snapshotId);
    }

    private long queryRows() {
        long rows = 0;
        for (String query : factory.getStatistics().getQueries()) {
            rows += factory.getStatistics().getQueryStatistics(query).getExecutionRowCount();
        }
        return rows;
    }

    @Test public void queryVersionDetectsMutationEvenWhenCountAndOrderingAreUnchanged() {
        repository.submit(request(0));
        APIQueryMemoryTaskMsg query = new APIQueryMemoryTaskMsg(); query.setLimit(1);
        MemoryQueryPage<MemoryTaskInventory> first = repository.taskPage(query);
        repository.claim("mn"); // Same rows/count/order, but task state changed.
        query.setSnapshotId(first.snapshotId); query.setStart(1);
        try { repository.taskPage(query); fail("Old task snapshot must not silently accept changed task data"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_QUERY_SNAPSHOT_CHANGED", expected.getCode()); }
    }

    @Test public void samplingDoesNotInvalidateStateMembershipSnapshot() {
        MemoryQueryPage<MemoryStateVO> first = repository.statePage(null, 0, 1, null);
        MemoryAgentResponse response = new MemoryAgentResponse(); response.status = "Succeeded";
        response.sampleTime = System.currentTimeMillis(); response.state = Collections.singletonMap("managed", false);
        repository.observe("host1", response);
        assertEquals(first.snapshotId, repository.statePage(null, 1, 1, first.snapshotId).snapshotId);
        repository.cleanupDeletedResources();
        try { repository.statePage(null, 1, 1, first.snapshotId); fail("Cleanup boundary invalidates old membership snapshots"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_QUERY_SNAPSHOT_CHANGED", expected.getCode()); }
    }

    private String standardDefault(MemoryStandardField field) {
        if ("ksm.enabled".equals(field.path())) { return "none"; }
        if ("zram.enabled".equals(field.path()) || "writeback.enabled".equals(field.path())) { return "false"; }
        return "";
    }

    private APIUpdateMemoryPolicyMsg request(long revision) {
        APIUpdateMemoryPolicyMsg msg = new APIUpdateMemoryPolicyMsg();
        SessionInventory session = new SessionInventory(); session.setAccountUuid("admin"); session.setUserUuid("actor");
        msg.setSession(session); msg.setScope("Global"); msg.setResourceUuid("global");
        msg.setTargetHostUuids(Arrays.asList("host1", "host2"));
        msg.setAction("apply"); msg.setExpectedRevision(revision);
        msg.setPolicy("{\"ksm\":{\"enabled\":true}}");
        msg.setClientRequestUuid(UUID.randomUUID().toString()); return msg;
    }

    private MemoryTaskVO taskFixture(String uuid, String parentUuid, String hostUuid, String action, String status) {
        MemoryTaskVO task = new MemoryTaskVO();
        task.setUuid(uuid); task.setParentUuid(parentUuid); task.setHostUuid(hostUuid);
        task.setScope("Global"); task.setResourceUuid("global"); task.setActorUuid("admin:actor");
        task.setAction(action); task.setStatus(status); task.setPolicy("{}"); task.setDesiredRevision(1);
        return task;
    }

    @Test public void explicitDeletePurgesTerminalRootTreeButKeepsPermanentIdempotencyReceipt() {
        APIUpdateMemoryPolicyMsg original = request(0);
        MemoryTaskInventory submitted = repository.submit(original);
        repository.transaction(em -> {
            MemoryTaskVO root = em.find(MemoryTaskVO.class, submitted.getUuid());
            root.setStatus("Succeeded");
            List<MemoryTaskVO> children = em.createQuery("from MemoryTaskVO t where t.parentUuid = :parent", MemoryTaskVO.class)
                    .setParameter("parent", submitted.getUuid()).getResultList();
            assertEquals(2, children.size());
            for (MemoryTaskVO child : children) {
                child.setStatus("Succeeded");
                MemoryStateVO state = em.find(MemoryStateVO.class, child.getHostUuid());
                state.setActiveTaskUuid(null); state.setControlOperationUuid(null); state.setStatus("Succeeded");
            }
            return null;
        });
        long revision = repository.getPolicy("Global", "global").getRevision();
        assertTrue(repository.deleteTaskTree(submitted.getUuid()).isDeleted());
        assertFalse(repository.deleteTaskTree(submitted.getUuid()).isDeleted());
        assertEquals(0L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t where t.uuid = :uuid or t.parentUuid = :uuid", Long.class)
                .setParameter("uuid", submitted.getUuid()).getSingleResult()));
        MemoryTaskIdempotencyReceiptVO receipt = repository.transaction(em ->
                em.find(MemoryTaskIdempotencyReceiptVO.class, submitted.getUuid()));
        assertNotNull(receipt); assertEquals("Succeeded", receipt.getOriginalStatus());
        assertEquals(revision, repository.getPolicy("Global", "global").getRevision());
        try {
            repository.submit(original);
            fail("a deleted request must never be replayed as a new task");
        } catch (MemoryOperationException expected) {
            assertEquals("MEMORY_REQUEST_RESULT_PURGED", expected.getCode());
            assertTrue(expected.getMessage().contains(submitted.getUuid()));
            assertTrue(expected.getMessage().contains("Succeeded"));
        }
        APIUpdateMemoryPolicyMsg changed = request(revision);
        changed.setClientRequestUuid(original.getClientRequestUuid());
        try {
            repository.submit(changed);
            fail("reusing the client UUID with a different request hash must remain a conflict");
        } catch (MemoryOperationException expected) {
            assertEquals("MEMORY_IDEMPOTENCY_CONFLICT", expected.getCode());
        }
    }

    @Test public void explicitDeleteIsNoOpForMissingRootAndRejectsChildOrBlockedTree() {
        assertFalse(repository.deleteTaskTree("abcdefabcdefabcdefabcdefabcdefab").isDeleted());
        repository.transaction(em -> {
            MemoryTaskVO root = taskFixture("11111111111111111111111111111111", null, null, "apply", "Succeeded");
            em.persist(root);
            em.persist(taskFixture("22222222222222222222222222222222", root.getUuid(), "host1", "apply", "Succeeded"));
            MemoryTaskVO blockedRoot = taskFixture("33333333333333333333333333333333", null, null, "apply", "Partial");
            em.persist(blockedRoot);
            em.persist(taskFixture("44444444444444444444444444444444", blockedRoot.getUuid(), "host2", "apply", "Blocked"));
            MemoryTaskVO snapshotRoot = taskFixture("55555555555555555555555555555555", null, null, "apply", "Succeeded");
            snapshotRoot.setTargetSnapshotHash("target-snapshot-hash"); em.persist(snapshotRoot);
            em.persist(taskFixture("66666666666666666666666666666666", null, null, "apply", "Applying"));
            em.persist(taskFixture("77777777777777777777777777777777", null, null, "apply", "Unknown"));
            return null;
        });
        try {
            repository.deleteTaskTree("22222222222222222222222222222222");
            fail("child tasks must not be deleted independently");
        } catch (MemoryOperationException expected) { assertEquals("MEMORY_TASK_NOT_ROOT", expected.getCode()); }
        try {
            repository.deleteTaskTree("33333333333333333333333333333333");
            fail("Blocked is an active Host fence, not a deletable terminal state");
        } catch (MemoryOperationException expected) { assertEquals("MEMORY_TASK_REFERENCED", expected.getCode()); }
        try {
            repository.deleteTaskTree("55555555555555555555555555555555");
            fail("target snapshot commit evidence must remain until its replay contract is tombstoned");
        } catch (MemoryOperationException expected) { assertEquals("MEMORY_TASK_REFERENCED", expected.getCode()); }
        for (String unresolved : Arrays.asList("66666666666666666666666666666666", "77777777777777777777777777777777")) {
            try { repository.deleteTaskTree(unresolved); fail("active and Unknown root rows cannot be purged"); }
            catch (MemoryOperationException expected) { assertEquals("MEMORY_TASK_REFERENCED", expected.getCode()); }
        }
        assertEquals(7L, (long) repository.transaction(em -> em.createQuery("select count(t) from MemoryTaskVO t", Long.class).getSingleResult()));
    }

    @Test public void explicitDeleteAcceptsOnlyConsistentPartialAggregateAndRollsBackWithCaller() {
        String rootUuid = "88888888888888888888888888888888";
        repository.transaction(em -> {
            MemoryTaskVO root = taskFixture(rootUuid, null, null, "apply", "Partial"); em.persist(root);
            em.persist(taskFixture("99999999999999999999999999999999", rootUuid, "host1", "apply", "Succeeded"));
            em.persist(taskFixture("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaab", rootUuid, "host2", "apply", "Failed"));
            return null;
        });
        try {
            repository.transaction(em -> {
                assertTrue(repository.deleteTaskTree(rootUuid).isDeleted());
                throw new IllegalStateException("force caller rollback after purge");
            });
            fail("outer transaction should roll back the deletion and tombstone together");
        } catch (IllegalStateException expected) { assertEquals("force caller rollback after purge", expected.getMessage()); }
        assertEquals(3L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t where t.uuid = :uuid or t.parentUuid = :uuid", Long.class)
                .setParameter("uuid", rootUuid).getSingleResult()));
        assertNull(repository.transaction(em -> em.find(MemoryTaskIdempotencyReceiptVO.class, rootUuid)));
    }

    @Test public void explicitDeleteRejectsHostStateTaskAndMigrationReferencesAndPendingOutbox() {
        repository.transaction(em -> {
            MemoryTaskVO root = taskFixture("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", null, null, "apply", "Succeeded");
            root.setRequestKey("admin:actor:11111111-1111-1111-1111-111111111111");
            root.setRequestHash("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
            em.persist(root);
            MemoryTaskVO child = taskFixture("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", root.getUuid(), "host1", "apply", "Succeeded");
            em.persist(child);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setActiveTaskUuid(child.getUuid()); state.setControlOperationUuid(child.getUuid());
            return null;
        });
        assertReferenced("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setActiveTaskUuid(null); state.setControlOperationUuid(null);
            state.setState("{\"lastConfirmedOperationUuid\":\"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb\"}");
            return null;
        });
        assertReferenced("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1"); state.setState("{}");
            MemoryTaskVO outside = taskFixture("cccccccccccccccccccccccccccccccc", null, null, "reconcile", "Succeeded");
            outside.setExpectedControlOperationUuid("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"); em.persist(outside);
            return null;
        });
        assertReferenced("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        repository.transaction(em -> {
            em.remove(em.find(MemoryTaskVO.class, "cccccccccccccccccccccccccccccccc"));
            MemoryMigrationVO migration = new MemoryMigrationVO(); migration.vmUuid = "vm1";
            migration.operationUuid = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"; migration.sourceHostUuid = "host1";
            migration.targetHostUuid = "host2"; migration.status = "Holding"; em.persist(migration);
            return null;
        });
        assertReferenced("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        repository.transaction(em -> {
            em.remove(em.find(MemoryMigrationVO.class, "vm1"));
            MemoryTaskFailureOutboxVO outbox = new MemoryTaskFailureOutboxVO();
            outbox.setTaskUuid("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"); outbox.setHostUuid("host1");
            outbox.setAction("apply"); outbox.setDelivered(false); outbox.setAttempts(0); em.persist(outbox);
            return null;
        });
        assertReferenced("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    }

    private void assertReferenced(String taskUuid) {
        try {
            repository.deleteTaskTree(taskUuid);
            fail("referenced history must be retained");
        } catch (MemoryOperationException expected) {
            assertEquals("MEMORY_TASK_REFERENCED", expected.getCode());
        }
        assertEquals(2L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t where t.uuid = 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa' "
                        + "or t.uuid = 'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb'", Long.class).getSingleResult()));
        assertNull(repository.transaction(em -> em.find(MemoryTaskIdempotencyReceiptVO.class, taskUuid)));
    }

    @Test public void configurationRejectsDisconnectedHostBeforePersistingDesiredOrTask() {
        repository.transaction(em -> { em.find(TestHost.class, "host1").status = HostStatus.Connecting; return null; });
        try {
            repository.submit(request(0));
            fail("configuration must not be admitted while a target host is not Connected");
        } catch (MemoryOperationException expected) {
            assertEquals("MEMORY_HOST_NOT_CONNECTED", expected.getCode());
        }
        assertEquals(0L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t", Long.class).getSingleResult()));
        assertEquals(0L, repository.getPolicy("Global", "global").getRevision());
    }

    private Object exclusionCall(String name, Class<?>[] types, Object... values) throws Exception {
        java.lang.reflect.Method method;
        try { method = MemoryRepository.class.getDeclaredMethod(name, types); }
        catch (NoSuchMethodException e) { fail("Migration participation must be durably captured"); return null; }
        method.setAccessible(true); return method.invoke(repository, values);
    }

    private APIUpdateMemoryPolicyMsg shardRequest(String requestId, String action, long revision) {
        APIUpdateMemoryPolicyMsg msg = request(revision);
        msg.setClientRequestUuid(requestId); msg.setAction(action);
        msg.setTargetVmUuids(Collections.singletonList("aabbccddeeff00112233445566778899"));
        msg.setTargetShardIndex(0); msg.setTargetShardCount(1); msg.setTargetTotalCount(1L);
        msg.setTargetSnapshotGeneration("snapshot1");
        msg.setTargetDigest(MemoryTargetShardAssembler.digest(msg.getTargetVmUuids()));
        if ("stageTargetShard".equals(action)) {
            msg.setPolicy("{}"); msg.setTargetHostUuids(null);
        } else if ("commitTargetShards".equals(action)) {
            msg.setTargetShardIndex(null); msg.setTargetVmUuids(null);
        }
        return msg;
    }

    @Test public void shardCommitCannotSubstituteAnotherRevisionOrDigest() {
        String id = UUID.randomUUID().toString();
        repository.submit(shardRequest(id, "stageTargetShard", 0));
        repository.submit(request(0));
        APIUpdateMemoryPolicyMsg changed = shardRequest(id, "commitTargetShards", 1);
        try { repository.submit(changed); fail("staged revision must bind commit"); }
        catch (MemoryOperationException e) { assertEquals("MEMORY_TARGET_SNAPSHOT_CONFLICT", e.getCode()); }
        changed = shardRequest(id, "commitTargetShards", 0); changed.setTargetDigest("different");
        try { repository.submit(changed); fail("staged digest must bind commit"); }
        catch (MemoryOperationException e) { assertEquals("MEMORY_TARGET_SNAPSHOT_CONFLICT", e.getCode()); }
    }

    @Test public void committedShardReplaySurvivesStageCleanupAndRejectsChangedPayload() {
        String id = UUID.randomUUID().toString();
        repository.submit(shardRequest(id, "stageTargetShard", 0));
        MemoryTaskInventory first = repository.submit(shardRequest(id, "commitTargetShards", 0));
        repository.transaction(em -> { em.createQuery("delete from MemoryTargetShardVO").executeUpdate(); return null; });
        assertEquals(first.getUuid(), repository.submit(shardRequest(id, "commitTargetShards", 0)).getUuid());
        APIUpdateMemoryPolicyMsg changed = shardRequest(id, "commitTargetShards", 0);
        changed.setPolicy("{\"ksm\":{\"enabled\":false}}");
        try { repository.submit(changed); fail("different payload is not replay"); }
        catch (MemoryOperationException e) { assertEquals("MEMORY_IDEMPOTENCY_CONFLICT", e.getCode()); }
    }

    @Test public void sourceHostExclusionSurvivesMigrationUntilExplicitRestore() throws Exception {
        exclusionCall("captureMigrationParticipation", new Class[]{String.class, String.class, String.class},
                "vm1", "host1", "host2"); // source defaults disabled, no active ZRAM pool needed
        repository.transaction(em -> { em.find(TestVm.class, "vm1").hostUuid = "host2"; return null; });
        assertEquals("deny", exclusionCall("vmParticipation", new Class[]{String.class}, "vm1"));
        MemoryPolicyInventory inventory = repository.getPolicy("VM", "vm1");
        assertTrue(inventory.getEffectivePolicy().contains("deny"));
        repository.initialize(); // MN restart does not discard the retained exclusion
        assertEquals("deny", exclusionCall("vmParticipation", new Class[]{String.class}, "vm1"));
        APIUpdateMemoryPolicyMsg restore = request(inventory.getRevision());
        restore.setScope("VM"); restore.setResourceUuid("vm1"); restore.setTargetHostUuids(null);
        restore.setExpectedInstanceGeneration("new-instance"); restore.setPolicy("{\"participation\":\"inherit\"}");
        repository.submit(restore);
        assertEquals("inherit", exclusionCall("vmParticipation", new Class[]{String.class}, "vm1"));
    }

    @Test public void failedMigrationDoesNotCreatePermanentExclusion() throws Exception {
        exclusionCall("captureMigrationParticipation", new Class[]{String.class, String.class, String.class}, "vm1", "host1", "host2");
        exclusionCall("abortMigrationParticipation", new Class[]{String.class}, "vm1");
        assertEquals("inherit", exclusionCall("vmParticipation", new Class[]{String.class}, "vm1"));
    }

    @Test public void emptyVmUpdateDoesNotRestoreMigrationExclusion() throws Exception {
        exclusionCall("captureMigrationParticipation", new Class[]{String.class, String.class, String.class},
                "vm1", "host1", "host2");
        repository.transaction(em -> { em.find(TestVm.class, "vm1").hostUuid = "host2"; return null; });
        assertEquals("deny", repository.vmParticipation("vm1"));
        APIUpdateMemoryPolicyMsg empty = request(0);
        empty.setScope("VM"); empty.setResourceUuid("vm1"); empty.setTargetHostUuids(null);
        empty.setExpectedInstanceGeneration("new-instance"); empty.setPolicy("{}");
        repository.submit(empty);
        assertEquals("deny", repository.vmParticipation("vm1"));
    }

    @Test public void sourceParticipationIgnoresTransientMonitoringFailures() throws Exception {
        MemoryTaskInventory parent = repository.submit(request(0));
        for (MemoryTaskVO task : repository.claim("mn")) { repository.result(task.getUuid(), "Succeeded", null, null); }
        APIUpdateMemoryPolicyMsg enable = request(1); enable.setPolicy("{\"zram\":{\"enabled\":true,\"selectionMode\":\"all_running\"}}");
        repository.submit(enable);
        exclusionCall("captureMigrationParticipation", new Class[]{String.class, String.class, String.class}, "vm1", "host1", "host2");
        repository.transaction(em -> { em.find(TestVm.class, "vm1").hostUuid = "host2"; return null; });
        assertEquals("inherit", exclusionCall("vmParticipation", new Class[]{String.class}, "vm1"));
    }

    @Test public void replayReturnsOriginalTaskAndDifferentPayloadConflicts() {
        APIUpdateMemoryPolicyMsg msg = request(0);
        MemoryTaskInventory first = repository.submit(msg);
        assertEquals(first.getUuid(), repository.submit(msg).getUuid());
        assertEquals(1, repository.getPolicy("Global", "global").getRevision());
        msg.setPolicy("{\"ksm\":{\"enabled\":false}}");
        try { repository.submit(msg); fail("must conflict"); }
        catch (MemoryOperationException expected) { assertTrue(expected.getMessage().contains("different parameters")); }
    }

    @Test public void claimIsAtMostOnceAndUnknownKeepsHostLocked() {
        MemoryTaskInventory parent = repository.submit(request(0));
        List<MemoryTaskVO> claimed = repository.claim("mn1"); assertEquals(2, claimed.size());
        assertTrue(repository.claim("mn2").isEmpty());
        repository.result(claimed.get(0).getUuid(), "Unknown", "timeout", null);
        repository.result(claimed.get(1).getUuid(), "Succeeded", null, null);
        try { repository.submit(request(1)); fail("unknown must block"); }
        catch (MemoryOperationException expected) { assertTrue(expected.getMessage().contains("unresolved")); }
        assertEquals(1, repository.getPolicy("Global", "global").getRevision());
        APIQueryMemoryTaskMsg query = new APIQueryMemoryTaskMsg(); query.setUuid(parent.getUuid());
        assertTrue(repository.tasks(query).stream().anyMatch(task -> "Unknown".equals(task.getStatus())));
    }

    @Test public void queuedCancelReleasesClaimsButNeverCancelsStartedOperation() {
        MemoryTaskInventory task = repository.submit(request(0));
        assertEquals("Cancelled", repository.cancel(task.getUuid()).getStatus());
        assertTrue(repository.claim("mn").isEmpty());
        MemoryTaskInventory next = repository.submit(request(1));
        assertEquals(2, repository.claim("mn").size());
        try { repository.cancel(next.getUuid()); fail("must not cancel started work"); }
        catch (MemoryOperationException expected) { assertTrue(expected.getMessage().contains("Queued")); }
    }

    @Test public void clearOverrideCannotSilentlyDiscardSuppliedPolicy() {
        APIUpdateMemoryPolicyMsg msg = request(0); msg.setAction("clearOverride");
        try { repository.submit(msg); fail("non-empty clearOverride must reject"); }
        catch (MemoryOperationException expected) { assertTrue(expected.getMessage().contains("configuration")); }
        assertEquals(0, repository.getPolicy("Global", "global").getRevision());
    }

    @Test public void paginationReportsTotalAndReturnsStableDistinctPages() {
        repository.submit(request(0));
        APIQueryMemoryTaskMsg query = new APIQueryMemoryTaskMsg(); query.setLimit(1);
        assertEquals(3, repository.taskCount(query));
        String first = repository.tasks(query).get(0).getUuid(); query.setStart(1);
        assertNotEquals(first, repository.tasks(query).get(0).getUuid());
        assertEquals(2, repository.stateCount(null));
    }

    @Test public void configuredConcurrencyAndPaginationReachTheRepository() throws Exception {
        Field field = org.zstack.core.config.GlobalConfig.class.getDeclaredField("value"); field.setAccessible(true);
        org.zstack.core.config.GlobalConfig concurrency = MemoryOptimizationGlobalConfig.DISPATCH_CONCURRENCY;
        org.zstack.core.config.GlobalConfig maximum = MemoryOptimizationGlobalConfig.QUERY_MAX_PAGE_SIZE;
        String oldConcurrency = concurrency.value(), oldMaximum = maximum.value();
        try {
            field.set(concurrency, "1"); repository.submit(request(0));
            assertEquals(1, repository.claim("mn").size());
            assertTrue(repository.claim("mn2").isEmpty());
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
        } finally { field.set(concurrency, oldConcurrency); field.set(maximum, oldMaximum); }
    }

    @Test public void concurrentClaimersNeverDispatchSameChild() throws Exception {
        repository.submit(request(0));
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CountDownLatch go = new java.util.concurrent.CountDownLatch(1);
        try {
            List<java.util.concurrent.Future<List<MemoryTaskVO>>> futures = new ArrayList<>();
            for (String owner : Arrays.asList("mn1", "mn2")) {
                futures.add(executor.submit(() -> { go.await(); return repository.claim(owner); }));
            }
            go.countDown(); Set<String> uuids = new HashSet<>(); int claimed = 0;
            for (java.util.concurrent.Future<List<MemoryTaskVO>> future : futures) {
                for (MemoryTaskVO task : future.get(15, java.util.concurrent.TimeUnit.SECONDS)) {
                    claimed++; assertTrue(uuids.add(task.getUuid()));
                }
            }
            assertEquals(2, claimed);
        } finally { executor.shutdownNow(); }
    }

    @Test public void lostManagementNodeBecomesUnknownWithoutReplay() {
        repository.submit(request(0)); repository.claim("dead-mn");
        repository.expireUnconfirmed("dead-mn");
        assertTrue(repository.claim("new-mn").isEmpty());
        assertTrue(repository.states(null, 0, 100).stream().allMatch(s -> "Unknown".equals(s.getStatus())));
    }

    @Test public void delayedPermitResultCannotOverrideNewPauseTask() {
        repository.submit(request(0)); MemoryTaskVO old = repository.claim("mn").get(0);
        long initialDeadline = System.currentTimeMillis() + 300000;
        assertTrue(repository.recordTaskPermit(old.getUuid(), "managed-" + old.getHostUuid(), initialDeadline));
        assertFalse(repository.recordTaskPermit(old.getUuid(), "managed-" + old.getHostUuid(), initialDeadline + 60000));
        repository.result(old.getUuid(), "Succeeded", null, null);
        repository.authorizeTaskPermit(old.getUuid());
        MemoryStateVO state = repository.states(Arrays.asList(old.getHostUuid()), 0, 1).get(0);
        assertTrue(state.isPermitAuthorized());
        assertEquals(initialDeadline, state.getPermitDeadline().longValue());
        APIUpdateMemoryPolicyMsg pause = request(1); pause.setAction("pause"); pause.setPolicy("{}");
        pause.setTargetHostUuids(Arrays.asList(old.getHostUuid())); repository.submit(pause);
        repository.authorizeTaskPermit(old.getUuid());
        repository.renewPermit(old.getHostUuid(), old.getUuid(), System.currentTimeMillis() + 300000);
        assertFalse(repository.states(Arrays.asList(old.getHostUuid()), 0, 1).get(0).isPermitAuthorized());
    }

    @Test public void pausedHostFencesPolicyChangesButKeepsVmAndResumeFence() {
        APIUpdateMemoryPolicyMsg pause = request(0);
        pause.setScope("Host"); pause.setResourceUuid("host1"); pause.setTargetHostUuids(null);
        pause.setAction("pause"); pause.setPolicy("{}");
        MemoryTaskInventory pausedParent = repository.submit(pause);
        MemoryTaskVO pausedTask = repository.claim("mn").get(0);
        MemoryAgentResponse pausedReply = new MemoryAgentResponse();
        pausedReply.status = "Succeeded"; pausedReply.sampleTime = System.currentTimeMillis();
        pausedReply.state = new HashMap<>(); pausedReply.state.put("phase", "PAUSED");
        repository.result(pausedTask.getUuid(), "Succeeded", null, pausedReply);
        MemoryStateVO pausedState = repository.states(Collections.singletonList("host1"), 0, 1).get(0);
        String pauseFence = pausedState.getControlOperationUuid();
        assertEquals(pausedTask.getUuid(), pauseFence);

        APIUpdateMemoryPolicyMsg apply = request(0);
        apply.setScope("Host"); apply.setResourceUuid("host1"); apply.setTargetHostUuids(null);
        try { repository.submit(apply); fail("paused Host must require resume before apply"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_CONTROL_PAUSED_RESUME_REQUIRED", expected.getCode()); }
        assertEquals(0, repository.getPolicy("Host", "host1").getRevision());
        assertEquals(pauseFence, repository.states(Collections.singletonList("host1"), 0, 1).get(0).getControlOperationUuid());

        APIUpdateMemoryPolicyMsg clear = request(0);
        clear.setScope("Host"); clear.setResourceUuid("host1"); clear.setTargetHostUuids(null);
        clear.setAction("clearOverride"); clear.setPolicy("{}"); clear.setClearOverrideFields(Arrays.asList("ksm.enabled"));
        try { repository.submit(clear); fail("paused Host must require resume before clearOverride"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_CONTROL_PAUSED_RESUME_REQUIRED", expected.getCode()); }

        APIUpdateMemoryPolicyMsg vm = request(0);
        vm.setScope("VM"); vm.setResourceUuid("vm1"); vm.setTargetHostUuids(null);
        vm.setExpectedInstanceGeneration("instance-1"); vm.setPolicy("{\"participation\":\"allow\"}");
        MemoryTaskInventory vmParent = repository.submit(vm);
        MemoryTaskVO vmTask = repository.claim("mn").stream().filter(t -> "VM".equals(t.getScope())).findFirst().get();
        repository.result(vmTask.getUuid(), "Succeeded", null, null);
        assertEquals(pauseFence, repository.states(Collections.singletonList("host1"), 0, 1).get(0).getControlOperationUuid());

        APIUpdateMemoryPolicyMsg resume = request(0);
        resume.setScope("Host"); resume.setResourceUuid("host1"); resume.setTargetHostUuids(null);
        resume.setAction("resume"); resume.setPolicy("{}"); resume.setExpectedControlOperationUuid(pauseFence);
        assertEquals("Queued", repository.submit(resume).getStatus());
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

        APIUpdateMemoryPolicyMsg apply = request(0);
        apply.setScope("Host"); apply.setResourceUuid("host1"); apply.setTargetHostUuids(null);
        apply.setExpectedGlobalRevision(0L);
        MemoryTaskInventory submitted = repository.submit(apply);
        assertEquals("Queued", submitted.getStatus());
        MemoryTaskVO child = repository.claim("mn-a").stream()
                .filter(task -> "host1".equals(task.getHostUuid())).findFirst().get();
        MemoryStateVO state = repository.states(Collections.singletonList("host1"), 0, 1).get(0);
        assertEquals(child.getUuid(), state.getActiveTaskUuid());
        assertEquals(child.getUuid(), state.getControlOperationUuid());
        assertNotEquals("completed-drain", state.getControlOperationUuid());
    }

    @Test public void completedDrainWithoutExactMaintenanceProofStillFencesPolicyApply() {
        repository.transaction(em -> {
            em.persist(testTask("completed-drain", null, "drain", "Succeeded", null));
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Succeeded"); state.setControlOperationUuid("completed-drain");
            state.setState("{\"lifecycle\":{\"paused\":true,\"activeState\":\"paused\"}}");
            return null;
        });
        APIUpdateMemoryPolicyMsg apply = request(0);
        apply.setScope("Host"); apply.setResourceUuid("host1"); apply.setTargetHostUuids(null);
        apply.setExpectedGlobalRevision(0L);
        try { repository.submit(apply); fail("drain without fresh maintenance proof must remain fenced"); }
        catch (MemoryOperationException expected) {
            assertEquals("MEMORY_CONTROL_PAUSED_RESUME_REQUIRED", expected.getCode());
            assertTrue(expected.getMessage().contains("explicit drained-pool maintenance"));
        }
    }

    @Test public void successfulResumeFenceWinsOverOneStalePausedSample() {
        APIUpdateMemoryPolicyMsg pause = request(0); pause.setAction("pause"); pause.setPolicy("{}");
        repository.submit(pause);
        MemoryTaskVO pauseTask = repository.claim("mn").get(0);
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
        APIUpdateMemoryPolicyMsg apply = request(0);
        apply.setScope("Host"); apply.setResourceUuid("host1"); apply.setTargetHostUuids(null);
        apply.setExpectedGlobalRevision(0L);
        assertEquals("Queued", repository.submit(apply).getStatus());
    }

    @Test public void newerPausedSampleAfterResumeStillFencesApply() {
        APIUpdateMemoryPolicyMsg pause = request(0); pause.setAction("pause"); pause.setPolicy("{}");
        repository.submit(pause);
        MemoryTaskVO pauseTask = repository.claim("mn").get(0);
        repository.result(pauseTask.getUuid(), "Succeeded", null,
                new MemoryAgentResponse() {{ status = "Succeeded"; sampleTime = System.currentTimeMillis(); state = Collections.singletonMap("phase", "PAUSED"); }});
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            MemoryTaskVO resume = em.find(MemoryTaskVO.class, state.getControlOperationUuid());
            resume.setAction("resume"); resume.setStatus("Succeeded");
            state.setActiveTaskUuid(null); state.setStatus("Succeeded");
            state.setLastSampleTime(System.currentTimeMillis() + 60000);
            state.setState("{\"lifecycle\":{\"paused\":true,\"activeState\":\"paused\"}}");
            return null;
        });
        APIUpdateMemoryPolicyMsg apply = request(0);
        apply.setScope("Host"); apply.setResourceUuid("host1"); apply.setTargetHostUuids(null);
        apply.setExpectedGlobalRevision(0L);
        try {
            repository.submit(apply);
            fail("a newer paused sample must not be hidden by an older successful resume");
        } catch (MemoryOperationException expected) {
            assertEquals("MEMORY_CONTROL_PAUSED_RESUME_REQUIRED", expected.getCode());
        }
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

    @Test public void uncertainRecoveryRetainsUnknownHistoryAndRestoresOnlyDrainFence() {
        APIUpdateMemoryPolicyMsg msg = uncertainRecoveryRequest();
        MemoryTaskInventory parent = repository.submit(msg);
        assertEquals(parent.getUuid(), repository.submit(msg).getUuid());
        MemoryTaskVO task = claimChild(parent.getUuid());
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
        APIUpdateMemoryPolicyMsg apply = request(0); apply.setScope("Host"); apply.setResourceUuid("host1");
        apply.setTargetHostUuids(null); apply.setExpectedGlobalRevision(0L);
        assertNotNull(repository.submit(apply));
        assertEquals("Unknown", repository.findTask(MemoryUncertainRecoveryRulesTest.OLD).getStatus());
    }

    @Test public void uncertainRecoveryResponseLossCanBeReadOnlyReconciledWithoutRelabelingOldApply() {
        MemoryTaskVO task = claimChild(repository.submit(uncertainRecoveryRequest()).getUuid());
        repository.result(task.getUuid(), "Unknown", "response lost", null);
        assertEquals(task.getUuid(), host1State().getActiveTaskUuid());
        APIUpdateMemoryPolicyMsg query = request(0); query.setScope("Host"); query.setResourceUuid("host1");
        query.setTargetHostUuids(null); query.setPolicy("{}"); query.setAction("reconcile");
        MemoryTaskVO reconciliation = claimChild(repository.submit(query).getUuid());
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
        MemoryTaskVO task = claimChild(repository.submit(uncertainRecoveryRequest()).getUuid());
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

    @Test public void uncertainRecoveryRequiresFreshExactOwnerAndIdempotencyIncludesProofInputs() {
        APIUpdateMemoryPolicyMsg msg = uncertainRecoveryRequest();
        repository.transaction(em -> { em.find(MemoryStateVO.class, "host1").setLastSampleTime(1L); return null; });
        try { repository.submit(msg); fail("stale boot observation must fail"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_UNCERTAIN_RECOVERY_FENCED", expected.getCode()); }
        repository.transaction(em -> { em.find(MemoryStateVO.class, "host1").setLastSampleTime(System.currentTimeMillis()); return null; });
        repository.submit(msg);
        msg.getRecovery().setExpectedPoolGeneration(String.join("", Collections.nCopies(64, "b")));
        try { repository.submit(msg); fail("same request cannot change pool proof"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_IDEMPOTENCY_CONFLICT", expected.getCode()); }
        assertEquals("Unknown", repository.findTask(MemoryUncertainRecoveryRulesTest.OLD).getStatus());
    }

    @Test public void lateRecoveryResultCannotReleaseNewerHostOperation() {
        MemoryTaskVO task = claimChild(repository.submit(uncertainRecoveryRequest()).getUuid());
        repository.transaction(em -> { em.find(MemoryStateVO.class, "host1").setActiveTaskUuid("newer-operation"); return null; });
        repository.result(task.getUuid(), "Succeeded", null, MemoryUncertainRecoveryRulesTest.proof(task.getUuid()));
        assertEquals("Unknown", repository.findTask(task.getUuid()).getStatus());
        assertEquals("newer-operation", host1State().getActiveTaskUuid());
        assertEquals(MemoryUncertainRecoveryRulesTest.OLD, host1State().getControlOperationUuid());
    }

    @Test public void recoveryLicenseExpiryBeforeDispatchRestoresOldSlotAndAllowsANewRequest() {
        APIUpdateMemoryPolicyMsg msg = uncertainRecoveryRequest();
        MemoryTaskVO task = claimChild(repository.submit(msg).getUuid());
        repository.recoveryLicenseExpiredBeforeDispatch(task.getUuid());
        assertEquals("Failed", repository.findTask(task.getUuid()).getStatus());
        assertEquals("CLOUD_LICENSE_EXPIRED_BEFORE_RECOVERY_DISPATCH", repository.findTask(task.getUuid()).getReason());
        assertEquals(MemoryUncertainRecoveryRulesTest.OLD, host1State().getActiveTaskUuid());
        assertEquals(MemoryUncertainRecoveryRulesTest.OLD, host1State().getControlOperationUuid());
        assertEquals("Unknown", repository.findTask(MemoryUncertainRecoveryRulesTest.OLD).getStatus());
        msg.setClientRequestUuid(UUID.randomUUID().toString());
        MemoryTaskVO next = claimChild(repository.submit(msg).getUuid());
        // A duplicate local callback for the previous request cannot touch the new owner.
        repository.recoveryLicenseExpiredBeforeDispatch(task.getUuid());
        assertEquals(next.getUuid(), host1State().getActiveTaskUuid());
    }

    @Test public void recoveryNotIssuedProofRestoresOldUnknownOwnerAndAllowsNewRecoveryRequest() {
        APIUpdateMemoryPolicyMsg msg = uncertainRecoveryRequest();
        MemoryTaskVO task = claimChild(repository.submit(msg).getUuid());
        String before = host1State().getState();
        repository.result(task.getUuid(), "Failed", "precheck rejected", MemoryUncertainRecoveryRulesTest.notIssued(task.getUuid()));
        assertEquals("Failed", repository.findTask(task.getUuid()).getStatus());
        assertEquals("Failed", repository.findTask(task.getParentUuid()).getStatus());
        assertEquals("Unknown", repository.findTask(MemoryUncertainRecoveryRulesTest.OLD).getStatus());
        assertEquals("Unknown", repository.findTask("old-uncertain-parent").getStatus());
        assertEquals(MemoryUncertainRecoveryRulesTest.OLD, host1State().getActiveTaskUuid());
        assertEquals(MemoryUncertainRecoveryRulesTest.OLD, host1State().getControlOperationUuid());
        assertEquals("Unknown", host1State().getStatus());
        assertEquals(before, host1State().getState()); assertNull(host1State().getAppliedRevision());
        assertTrue(repository.claimFailureNotifications("recovery-rejected-outbox").stream()
                .anyMatch(row -> task.getUuid().equals(row.getTaskUuid())));
        msg.setClientRequestUuid(UUID.randomUUID().toString());
        MemoryTaskVO next = claimChild(repository.submit(msg).getUuid());
        repository.result(task.getUuid(), "Failed", "late duplicate", MemoryUncertainRecoveryRulesTest.notIssued(task.getUuid()));
        assertEquals(next.getUuid(), host1State().getActiveTaskUuid());
    }

    @Test public void recoveryNotIssuedResponseLossReconcilesOnlyNewRecoveryAndKeepsOldUnknown() {
        MemoryTaskVO task = claimChild(repository.submit(uncertainRecoveryRequest()).getUuid());
        repository.result(task.getUuid(), "Unknown", "response lost", null);
        APIUpdateMemoryPolicyMsg query = request(0); query.setScope("Host"); query.setResourceUuid("host1");
        query.setTargetHostUuids(null); query.setPolicy("{}"); query.setAction("reconcile");
        MemoryTaskVO reconciliation = claimChild(repository.submit(query).getUuid());
        repository.result(reconciliation.getUuid(), "Failed", "precheck rejected", MemoryUncertainRecoveryRulesTest.notIssued(task.getUuid()));
        assertEquals("Failed", repository.findTask(task.getUuid()).getStatus());
        assertEquals("Failed", repository.findTask(reconciliation.getUuid()).getStatus());
        assertEquals("Unknown", repository.findTask(MemoryUncertainRecoveryRulesTest.OLD).getStatus());
        assertEquals(MemoryUncertainRecoveryRulesTest.OLD, host1State().getActiveTaskUuid());
        assertEquals(MemoryUncertainRecoveryRulesTest.OLD, host1State().getControlOperationUuid());
    }

    @Test public void recoveryNotIssuedRequiresEveryProofFieldAndNoContradictoryRetirementEvidence() {
        MemoryTaskVO task = claimChild(repository.submit(uncertainRecoveryRequest()).getUuid());
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

    @Test public void cancellingQueuedRecoveryRestoresUnknownOwnerRatherThanClearingIt() {
        MemoryTaskInventory parent = repository.submit(uncertainRecoveryRequest());
        repository.cancel(parent.getUuid());
        assertEquals("Cancelled", repository.findTask(parent.getUuid()).getStatus());
        assertEquals(MemoryUncertainRecoveryRulesTest.OLD, host1State().getActiveTaskUuid());
        assertEquals(MemoryUncertainRecoveryRulesTest.OLD, host1State().getControlOperationUuid());
        assertEquals("Unknown", repository.findTask(MemoryUncertainRecoveryRulesTest.OLD).getStatus());
    }

    @Test public void cancellingQueuedRecoveryQueryRestoresUnresolvedRecoveryRatherThanOldApply() {
        MemoryTaskVO task = claimChild(repository.submit(uncertainRecoveryRequest()).getUuid());
        repository.result(task.getUuid(), "Unknown", "response lost", null);
        APIUpdateMemoryPolicyMsg query = request(0); query.setScope("Host"); query.setResourceUuid("host1");
        query.setTargetHostUuids(null); query.setPolicy("{}"); query.setAction("reconcile");
        MemoryTaskInventory parent = repository.submit(query);
        repository.cancel(parent.getUuid());
        assertEquals(task.getUuid(), host1State().getActiveTaskUuid());
        assertEquals("Unknown", repository.findTask(task.getUuid()).getStatus());
        assertEquals(MemoryUncertainRecoveryRulesTest.OLD, host1State().getControlOperationUuid());
    }

    @Test public void unconfirmedTaskResponsesCannotOverwriteControlProofOrAppliedRevision() {
        APIUpdateMemoryPolicyMsg msg = request(0); msg.setScope("Host"); msg.setResourceUuid("host1");
        msg.setTargetHostUuids(null); msg.setExpectedGlobalRevision(0L);
        MemoryTaskVO task = claimChild(repository.submit(msg).getUuid());
        String before = host1State().getState();
        MemoryAgentResponse untrusted = MemoryUncertainRecoveryRulesTest.proof(task.getUuid());
        untrusted.setSuccess(false); untrusted.status = "Unknown"; untrusted.appliedRevision = 999L;
        repository.result(task.getUuid(), "Unknown", "unconfirmed", untrusted);
        assertEquals(before, host1State().getState()); assertNull(host1State().getAppliedRevision());
        untrusted.status = "Failed";
        repository.result(task.getUuid(), "Failed", "unconfirmed", untrusted);
        assertEquals(before, host1State().getState()); assertNull(host1State().getAppliedRevision());
    }

    @Test public void executedBlockedTaskHoldsHostWithoutConsumingRunningSlotAndAllowsReadOnlyReconcile() {
        APIUpdateMemoryPolicyMsg msg = request(0); msg.setScope("Host"); msg.setResourceUuid("host1");
        msg.setTargetHostUuids(null); msg.setExpectedGlobalRevision(0L);
        MemoryTaskVO task = claimChild(repository.submit(msg).getUuid());
        repository.result(task.getUuid(), "Blocked", "NATIVE_POOL_UNQUALIFIED", null);
        assertEquals(task.getUuid(), host1State().getActiveTaskUuid());
        assertEquals("Blocked", repository.findTask(task.getParentUuid()).getStatus());
        assertTrue(repository.claim("another-mn").isEmpty());
        APIUpdateMemoryPolicyMsg query = request(1); query.setScope("Host"); query.setResourceUuid("host1");
        query.setTargetHostUuids(null); query.setAction("reconcile"); query.setPolicy("{}");
        MemoryTaskVO reconciliation = claimChild(repository.submit(query).getUuid());
        assertEquals(task.getUuid(), reconciliation.getReconcileOperationUuid());
        repository.result(reconciliation.getUuid(), "Succeeded", null, null);
        assertEquals("Succeeded", repository.findTask(task.getUuid()).getStatus());
        assertNull(host1State().getActiveTaskUuid());
    }

    private APIUpdateMemoryPolicyMsg uncertainRecoveryRequest() {
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
        APIUpdateMemoryPolicyMsg msg = request(0); msg.setScope("Host"); msg.setResourceUuid("host1");
        msg.setTargetHostUuids(null); msg.setAction("recoverUncertain"); msg.setPolicy("{}");
        msg.setExpectedControlOperationUuid(MemoryUncertainRecoveryRulesTest.OLD);
        msg.setRecovery(MemoryUncertainRecoveryRulesTest.requestDto()); return msg;
    }

    private MemoryTaskVO claimChild(String parent) {
        return repository.claim("mn-recovery-test").stream().filter(t -> parent.equals(t.getParentUuid())).findFirst().get();
    }

    private MemoryStateVO host1State() { return repository.states(Collections.singletonList("host1"), 0, 1).get(0); }

    private MemoryTaskVO testTask(String uuid, String parent, String action, String status, String reconcile) {
        MemoryTaskVO task = new MemoryTaskVO(); task.setUuid(uuid); task.setParentUuid(parent);
        task.setHostUuid("host1"); task.setScope("Host"); task.setResourceUuid("host1");
        task.setActorUuid("actor"); task.setAction(action); task.setStatus(status);
        task.setPolicy("{}"); task.setDesiredRevision(0); task.setReconcileOperationUuid(reconcile);
        return task;
    }

    @Test public void expiredInitialPermitCannotBeAuthorizedByLatePoll() {
        repository.submit(request(0)); MemoryTaskVO task = repository.claim("mn").get(0);
        long expired = System.currentTimeMillis() - 1;
        assertFalse(repository.recordTaskPermit(task.getUuid(), "managed-" + task.getHostUuid(), expired));
        // Simulate a persisted permit that expired while the executor/poll was
        // in flight; late success must not reopen host control.
        repository.transaction(em -> {
            MemoryTaskVO row = em.find(MemoryTaskVO.class, task.getUuid());
            row.setIssuedPermitOperationUuid("managed-" + task.getHostUuid());
            row.setIssuedPermitDeadline(expired); row.setStatus("Succeeded");
            return null;
        });
        repository.authorizeTaskPermit(task.getUuid());
        assertFalse(repository.states(Arrays.asList(task.getHostUuid()), 0, 1).get(0).isPermitAuthorized());
    }

    @Test public void hostApplyRejectsInheritedGlobalRevisionChange() {
        MemoryPolicyInventory preview = repository.preview("Host", "host1", "{\"zram\":{\"enabled\":false}}");
        MemoryTaskInventory changed = repository.submit(request(0)); repository.cancel(changed.getUuid());
        APIUpdateMemoryPolicyMsg host = request(preview.getRevision()); host.setScope("Host"); host.setResourceUuid("host1");
        host.setTargetHostUuids(null);
        host.setExpectedGlobalRevision(preview.getSourceRevision());
        try { repository.submit(host); fail("stale inherited policy must not apply"); }
        catch (MemoryOperationException expected) { assertTrue(expected.getMessage().contains("policy changed")); }
        host.setExpectedGlobalRevision(repository.getPolicy("Host", "host1").getSourceRevision());
        assertEquals("Queued", repository.submit(host).getStatus());
    }

    @Test public void vmPolicyDoesNotOverwriteHostLeaseOwnerOrAppliedRevision() {
        repository.submit(request(0));
        for (MemoryTaskVO task : repository.claim("mn")) {
            repository.result(task.getUuid(), "Succeeded", null, null);
        }
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setPermitAuthorized(true); state.setPermitDeadline(System.currentTimeMillis() + 300000);
            state.setAppliedRevision(1L); return null;
        });
        MemoryStateVO before = repository.states(Collections.singletonList("host1"), 0, 1).get(0);
        APIUpdateMemoryPolicyMsg vm = request(0); vm.setScope("VM"); vm.setResourceUuid("vm1");
        vm.setTargetHostUuids(null); vm.setPolicy("{\"participation\":\"deny\"}");
        vm.setExpectedInstanceGeneration("generation-1"); repository.submit(vm);
        MemoryTaskVO task = repository.claim("mn").get(0);
        MemoryAgentResponse response = new MemoryAgentResponse(); response.appliedRevision = 99L;
        response.sampleTime = System.currentTimeMillis();
        repository.result(task.getUuid(), "Succeeded", null, response);
        MemoryStateVO after = repository.states(Collections.singletonList("host1"), 0, 1).get(0);
        assertEquals(before.getControlOperationUuid(), after.getControlOperationUuid());
        assertEquals(before.getDesiredRevision(), after.getDesiredRevision());
        assertEquals(before.getAppliedRevision(), after.getAppliedRevision());
        assertTrue(after.isPermitAuthorized());
    }

    @Test public void clusterHasIndependentPolicyAndHostInheritsFieldwise() {
        repository.transaction(em -> {
            MemoryPolicyVO global = em.find(MemoryPolicyVO.class, "Global:global");
            global.setPolicy("{\"ksm\":{\"enabled\":true,\"zeroPagesEnabled\":false,\"pagesToScan\":2000}}");
            MemoryPolicyVO cluster = new MemoryPolicyVO();
            cluster.setUuid("Cluster:cluster1"); cluster.setScope("Cluster"); cluster.setResourceUuid("cluster1");
            cluster.setRevision(7); cluster.setPolicy("{\"ksm\":{\"zeroPagesEnabled\":true}}");
            em.persist(cluster);
            MemoryPolicyVO host = new MemoryPolicyVO();
            host.setUuid("Host:host1"); host.setScope("Host"); host.setResourceUuid("host1");
            host.setPolicy("{\"ksm\":{\"enabled\":false}}"); em.persist(host);
            return null;
        });
        assertEquals(7, repository.getPolicy("Cluster", "cluster1").getRevision());
        MemoryPolicyConfig config = MemoryPolicyRules.decode(repository.getPolicy("Host", "host1").getEffectivePolicy());
        assertEquals(Boolean.FALSE, config.ksm.enabled);
        assertEquals(Boolean.TRUE, config.ksm.zeroPagesEnabled);
        assertEquals(Long.valueOf(2000), config.ksm.pagesToScan);
        assertEquals(Arrays.asList("host1", "host2"), repository.targets("Cluster", "cluster1"));
        MemoryPolicyInventory inventory = repository.getPolicy("Host", "host1");
        assertNotNull(inventory.getSourceRevisions());
        assertEquals(Long.valueOf(7), inventory.getSourceRevisions().get("Cluster:cluster1"));
        assertEquals("Cluster:cluster1", inventory.getFieldSources().get("ksm.zeroPagesEnabled"));
        assertEquals("Host:host1", inventory.getFieldSources().get("ksm.enabled"));
        assertEquals("Global:global", inventory.getFieldSources().get("ksm.pagesToScan"));
    }

    @Test public void hostApplyMustRejectChangedClusterSnapshot() {
        MemoryPolicyInventory preview = repository.preview("Host", "host1", "{\"ksm\":{\"enabled\":true}}");
        repository.transaction(em -> {
            MemoryPolicyVO cluster = new MemoryPolicyVO(); cluster.setUuid("Cluster:cluster1");
            cluster.setScope("Cluster"); cluster.setResourceUuid("cluster1"); cluster.setRevision(1);
            cluster.setPolicy("{\"ksm\":{\"zeroPagesEnabled\":false}}"); em.persist(cluster);
            return null;
        });
        APIUpdateMemoryPolicyMsg host = request(0); host.setScope("Host"); host.setResourceUuid("host1");
        host.setTargetHostUuids(null); host.setExpectedGlobalRevision(preview.getSourceRevision());
        host.setExpectedSourceRevisions(preview.getSourceRevisions());
        try { repository.submit(host); fail("changed Cluster must invalidate preview"); }
        catch (MemoryOperationException expected) { assertTrue(expected.getMessage().contains("policy changed")); }
    }

    @Test public void globalZramAdmissionRejectsUnsupportedTargetBeforePolicyOrTaskPersistence() {
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host2");
            state.setCapabilities("{\"supported\":true,\"ksm\":true,\"zram\":false,\"zramReasonCode\":\"UNSUPPORTED_RECLAIM_KERNEL\"}");
            return null;
        });
        APIUpdateMemoryPolicyMsg msg = request(0); msg.setPolicy("{\"zram\":{\"enabled\":true}}");
        try { repository.submit(msg); fail("unsupported Host must be rejected before intent/task creation"); }
        catch (MemoryOperationException expected) {
            assertEquals("MEMORY_CAPABILITY_PRECHECK", expected.getCode());
            assertTrue(expected.getMessage().contains("UNSUPPORTED_RECLAIM_KERNEL"));
        }
        assertEquals(0L, (long) repository.transaction(em -> em.createQuery("select count(t) from MemoryTaskVO t", Long.class).getSingleResult()));
        assertEquals(0L, repository.getPolicy("Global", "global").getRevision());
    }

    @Test public void globalPreviewAndAdmissionIgnoreLowerOverrideAndUnchangedUnsupportedFeatures() {
        repository.transaction(em -> {
            MemoryPolicyVO host = new MemoryPolicyVO(); host.setUuid("Host:host1");
            host.setScope("Host"); host.setResourceUuid("host1");
            host.setPolicy("{\"zram\":{\"enabled\":false}}"); em.persist(host);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setCapabilities("{\"supported\":true,\"ksm\":true,\"zram\":false}");
            return null;
        });
        Map<String, MemoryHostPolicyPreviewSnapshot> preview = repository.previewHostPolicies("Global", "global",
                "{\"zram\":{\"enabled\":true},\"ksm\":{\"enabled\":true}}", "apply", null,
                Arrays.asList("host1", "host2"));
        assertTrue(MemoryPolicyAdmissionRules.changedFields(preview.get("host1").before, preview.get("host1").after)
                .keySet().stream().noneMatch(field -> field.startsWith("zram.")));
        assertFalse(MemoryPolicyAdmissionRules.changedFields(preview.get("host2").before, preview.get("host2").after)
                .keySet().stream().noneMatch(field -> field.startsWith("zram.")));

        // A lower-level explicit disable means the parent ZRAM change has no effect on host1.
        APIUpdateMemoryPolicyMsg ksm = request(0); ksm.setTargetHostUuids(Collections.singletonList("host1"));
        ksm.setPolicy("{\"ksm\":{\"enabled\":true}}");
        assertEquals("Queued", repository.submit(ksm).getStatus());
    }

    @Test public void hostKsmOnlyChangeWorksWhenUnsupportedZramIsNotEnabled() {
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host2");
            state.setCapabilities("{\"supported\":true,\"ksm\":true,\"zram\":false,\"zramReasonCode\":\"UNSUPPORTED_RECLAIM_KERNEL\"}");
            return null;
        });
        MemoryPolicyInventory current = repository.getPolicy("Host", "host2");
        APIUpdateMemoryPolicyMsg ksm = request(current.getRevision());
        ksm.setScope("Host"); ksm.setResourceUuid("host2"); ksm.setTargetHostUuids(null);
        ksm.setExpectedGlobalRevision(current.getSourceRevision()); ksm.setExpectedSourceRevisions(current.getSourceRevisions());
        ksm.setPolicy("{\"ksm\":{\"enabled\":true}}");
        assertEquals("Queued", repository.submit(ksm).getStatus());
    }

    @Test public void historicalUnsupportedEnabledZramBlocksUnrelatedKsmChangeUntilExplicitlyDisabled() {
        repository.transaction(em -> {
            standardConfigAdapter.writeGlobal(em, "zram.enabled", true);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host2");
            state.setCapabilities("{\"supported\":true,\"ksm\":true,\"zram\":false,\"zramReasonCode\":\"UNSUPPORTED_RECLAIM_KERNEL\"}");
            return null;
        });
        MemoryPolicyInventory current = repository.getPolicy("Host", "host2");
        Map<String, MemoryHostPolicyPreviewSnapshot> preview = repository.previewHostPolicies("Host", "host2",
                "{\"ksm\":{\"enabled\":true}}", "apply", null, Collections.singletonList("host2"));
        Map<String, String> changed = MemoryPolicyAdmissionRules.changedFields(
                preview.get("host2").before, preview.get("host2").after);
        assertEquals("host2", preview.get("host2").state.getHostUuid());
        assertNotNull("capability and policy snapshot travel together", preview.get("host2").state.getLastSampleTime());
        Map<String, String> blocked = MemoryPolicyAdmissionRules.blockedFields(changed,
                preview.get("host2").state, System.currentTimeMillis(), preview.get("host2").after);
        assertEquals("UNSUPPORTED_RECLAIM_KERNEL", blocked.get("zram.enabled"));

        APIUpdateMemoryPolicyMsg ksm = request(current.getRevision()); ksm.setScope("Host"); ksm.setResourceUuid("host2");
        ksm.setTargetHostUuids(null); ksm.setExpectedGlobalRevision(current.getSourceRevision());
        ksm.setExpectedSourceRevisions(current.getSourceRevisions()); ksm.setPolicy("{\"ksm\":{\"enabled\":true}}");
        try { repository.submit(ksm); fail("full effective payload cannot safely apply unsupported active ZRAM"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_CAPABILITY_PRECHECK", expected.getCode()); }
        assertEquals(current.getRevision(), repository.getPolicy("Host", "host2").getRevision());
    }

    @Test public void replayReturnsAcceptedTaskBeforeRecheckingExpiredCapabilitySnapshot() {
        APIUpdateMemoryPolicyMsg msg = request(0); msg.setPolicy("{\"ksm\":{\"enabled\":true}}");
        MemoryTaskInventory accepted = repository.submit(msg);
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setLastSampleTime(System.currentTimeMillis() - MemoryOptimizationGlobalConfig.displayTtlMillis() - 1);
            return null;
        });
        assertEquals(accepted.getUuid(), repository.submit(msg).getUuid());
    }

    @Test public void clearOneOverridePreservesOtherHostFieldsAndInheritsFalse() {
        repository.transaction(em -> {
            standardConfigAdapter.writeGlobal(em, "ksm.enabled", false);
            standardConfigAdapter.writeGlobal(em, "ksm.zeroPagesEnabled", false);
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.enabled", true);
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.zeroPagesEnabled", true);
            MemoryPolicyVO host = new MemoryPolicyVO(); host.setUuid("Host:host1");
            host.setScope("Host"); host.setResourceUuid("host1");
            host.setPolicy("{\"schemaVersion\":1}"); em.persist(host);
            return null;
        });
        MemoryPolicyInventory preview = repository.getPolicy("Host", "host1");
        APIUpdateMemoryPolicyMsg host = request(0); host.setScope("Host"); host.setResourceUuid("host1");
        host.setTargetHostUuids(null); host.setExpectedGlobalRevision(preview.getSourceRevision());
        host.setExpectedSourceRevisions(preview.getSourceRevisions());
        host.setAction("clearOverride"); host.setPolicy("{}");
        host.setClearOverrideFields(Collections.singletonList("ksm.enabled"));
        repository.submit(host);
        MemoryPolicyConfig config = MemoryPolicyRules.decode(repository.getPolicy("Host", "host1").getEffectivePolicy());
        assertEquals(Boolean.FALSE, config.ksm.enabled);
        assertEquals(Boolean.TRUE, config.ksm.zeroPagesEnabled);
    }

    @Test public void previewKeepsSnapshotWithoutObservationAndRejectsDeletedTargetsWithoutPartialResult() {
        List<String> targets = Arrays.asList("host1", "host2");
        String patch = "{\"ksm\":{\"enabled\":true}}";
        Map<String, MemoryHostPolicyPreviewSnapshot> snapshots = repository.previewHostPolicies(
                "Global", "global", patch, "apply", null, targets);
        assertEquals(new java.util.LinkedHashSet<>(targets), snapshots.keySet());
        assertNotNull(snapshots.get("host1"));
        assertNotNull(snapshots.get("host2"));
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host2");
            assertNotNull(state); em.remove(state); return null;
        });
        snapshots = repository.previewHostPolicies("Global", "global", patch, "apply", null, targets);
        assertEquals(new java.util.LinkedHashSet<>(targets), snapshots.keySet());
        assertNotNull("missing observation must not remove the policy snapshot", snapshots.get("host2"));
        assertNull(snapshots.get("host2").state);
        repository.transaction(em -> {
            TestHost host = em.find(TestHost.class, "host2");
            assertNotNull(host); em.remove(host); return null;
        });
        try {
            repository.previewHostPolicies("Global", "global", patch, "apply", null, targets);
            fail("a deleted Host must abort preview, not return a partial map");
        } catch (RuntimeException expected) { }
        try {
            repository.previewHostPolicies("Host", "host2", patch, "apply", null, targets);
            fail("a deleted Host scope must fail resource validation");
        } catch (RuntimeException expected) { }
    }

    @Test public void startupMigratesKnownCandidateFieldsOnceWithoutEnablingOrChangingRevision() {
        String old = "{\"ksm\":{\"mode\":\"Legacy\",\"profileVersion\":1,\"cpuBudgetPercent\":5,"
                + "\"enabled\":false,\"zeroPagesEnabled\":false,\"pagesToScan\":1200,\"sleepMillis\":30}}";
        repository.transaction(em -> {
            MemoryPolicyVO global = em.find(MemoryPolicyVO.class, "Global:global");
            global.setPolicy(old); global.setRevision(9);
            MemoryStandardConfigMigrationVO marker = em.find(MemoryStandardConfigMigrationVO.class,
                    MemoryStandardConfigMigrationVO.ORDINARY_FIELDS_V1);
            if (marker != null) { em.remove(marker); }
            return null;
        });
        repository.initialize();
        repository.initialize();
        MemoryPolicyInventory result = repository.getPolicy("Global", "global");
        MemoryPolicyConfig policy = MemoryPolicyRules.decode(result.getPolicy());
        assertEquals(9, result.getRevision());
        assertEquals(Boolean.FALSE, policy.ksm.enabled);
        assertEquals(Boolean.FALSE, policy.ksm.zeroPagesEnabled);
        assertEquals(Long.valueOf(1200), policy.ksm.pagesToScan);
        assertEquals(Long.valueOf(30), policy.ksm.sleepMillis);
        assertFalse(result.getPolicy().contains("mode"));
        assertEquals(old, repository.transaction(em -> em.find(MemoryPolicyVO.class, "Global:global").getLegacyPolicy()));
        assertEquals(0L, (long) repository.transaction(em -> em.createQuery("select count(t) from MemoryTaskVO t", Long.class).getSingleResult()));
    }
}
