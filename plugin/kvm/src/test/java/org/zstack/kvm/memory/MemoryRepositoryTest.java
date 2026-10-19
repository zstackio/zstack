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
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.function.Function;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfigVO;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.ConfigMutation;
import org.zstack.core.config.ConfigMutationContext;
import org.zstack.core.cloudbus.EventFacade;
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
                .addAnnotatedClass(MemoryQueryVersionVO.class)
                .addAnnotatedClass(MemoryTargetShardVO.class)
                .addAnnotatedClass(MemoryTaskVO.class).addAnnotatedClass(MemoryTaskIdempotencyReceiptVO.class)
                .addAnnotatedClass(MemoryTaskFailureOutboxVO.class)
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

    @Test public void memoryStateZqlSubqueryFiltersBeforeHostPagingAndTotal() throws Exception {
        // Use the product's once-only registration; invoking @StaticInit methods
        // separately would poison a later Platform initialization in this JVM.
        Class.forName("org.zstack.core.Platform");
        repository.transaction(em -> {
            for (String id : Arrays.asList("zql-a", "zql-b", "zql-c", "zql-d")) {
                TestHost host = new TestHost(); host.uuid = id; host.hypervisorType = "KVM";
                host.status = HostStatus.Connected; em.persist(host);
                if ("zql-d".equals(id)) continue;
                MemoryStateVO state = new MemoryStateVO(); state.setHostUuid(id);
                state.setStatus("Succeeded"); state.setPolicyPlanStatus("Applied");
                if ("zql-b".equals(id)) state.setPolicyPlanStatus("Blocked");
                if ("zql-c".equals(id)) state.setActiveTaskUuid("running-operation");
                em.persist(state);
            }
            return null;
        });
        // Public Unknown contains an unresolved active operation and a Host with no state row.
        String known = "status in ('Queued','Applying','Draining') or "
                + "(status in ('Succeeded','Failed','Blocked') and activeTaskUuid is null "
                + "and (policyPlanStatus is null or policyPlanStatus not in ('Unknown','Queued','Applying','Draining'))) "
                + "or (status='Blocked' and activeTaskUuid is not null)";
        String text = "query Host.uuid where uuid like 'zql-%' and uuid not in "
                + "(query MemoryState.hostUuid where " + known + ") "
                + "return with (total) order by uuid asc limit 1 offset 1";
        org.zstack.zql.antlr4.ZQLParser parser = new org.zstack.zql.antlr4.ZQLParser(
                new org.antlr.v4.runtime.CommonTokenStream(new org.zstack.zql.antlr4.ZQLLexer(
                        new org.antlr.v4.runtime.ANTLRInputStream(text))));
        org.zstack.header.zql.ASTNode.Query query = parser.query().accept(
                new org.zstack.zql.ast.parser.visitors.QueryVisitor());
        assertEquals(0, parser.getNumberOfSyntaxErrors());
        org.zstack.zql.ast.visitors.result.QueryResult compiled =
                new org.zstack.zql.ast.visitors.QueryVisitor(false).visit(query);
        repository.transaction(em -> {
            assertEquals(Collections.singletonList("zql-d"), compiled.createJPAQuery.apply(em).getResultList());
            assertEquals(2L, ((Number) compiled.createSimpleCountQuery.apply(em).getSingleResult()).longValue());
            MemoryStateVO original = em.find(MemoryStateVO.class, "zql-b");
            assertEquals("Succeeded", original.getStatus());
            MemoryStateInventory converted = (MemoryStateInventory) org.zstack.header.vo.ToInventory.toInventory(original);
            assertEquals("zql-b", converted.getHostUuid());
            assertEquals("Succeeded", converted.getStatus());
            assertEquals("Blocked", converted.getPolicyPlanStatus());
            assertEquals("zql-b", MemoryStateInventory.valueOf(Collections.singletonList(original)).get(0).getHostUuid());
            assertEquals("Blocked", MemoryMetricsPresentation.inventory(original, null, 100000L).getStatus());
            return null;
        });
    }

    @Test public void hostEffectivePolicyBatchesStandardConfigReadsWithoutChangingInheritance() {
        repository.transaction(em -> {
            standardConfigAdapter.writeGlobal(em, "zram.enabled", true);
            standardConfigAdapter.writeOverride(em, "cluster1", "ClusterVO", "ksm.pagesToScan", 2500L);
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.pagesToScan", 3100L);
            return null;
        });
        factory.getStatistics().clear();
        MemoryPolicyConfig policy = repository.serviceObservationPolicy("host1");
        assertNotNull(policy);
        assertEquals(Long.valueOf(3100), policy.ksm.pagesToScan);
        assertEquals(Boolean.TRUE, policy.zram.enabled);
        long configQueries = 0;
        for (String query : factory.getStatistics().getQueries()) {
            if (query.contains("GlobalConfigVO") || query.contains("ResourceConfigVO")) {
                configQueries += factory.getStatistics().getQueryStatistics(query).getExecutionCount();
            }
        }
        assertTrue("one scoped batch and KSM mode read per inheritance layer, got " + configQueries,
                configQueries <= 6);
        assertEquals(0, factory.getStatistics().getEntityInsertCount());
        assertEquals(0, factory.getStatistics().getEntityUpdateCount());
        assertEquals(0, factory.getStatistics().getEntityDeleteCount());
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

    @Test public void previewAllowsGlobalWritebackIntentWithoutHostBackend() {
        MemoryPolicyInventory preview = repository.preview("Global", "global",
                "{\"writeback\":{\"enabled\":true}}");
        assertEquals(Boolean.TRUE, MemoryPolicyRules.decode(preview.getEffectivePolicy()).writeback.enabled);
        assertEquals(0L, repository.getPolicy("Global", "global").getRevision());
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
                    List<ResourceConfigVO> rows = em.createQuery("select r from ResourceConfigVO r where "
                                    + "r.resourceUuid=:uuid and r.category=:category and r.name=:name", ResourceConfigVO.class)
                            .setParameter("uuid", change.getResourceUuid()).setParameter("category", field.category())
                            .setParameter("name", field.configName()).getResultList();
                    if (rows.isEmpty()) {
                        persistLegacyKsmValue(em, change.getResourceUuid(), change.getResourceType(), "none");
                    } else {
                        assertEquals(1, rows.size()); rows.get(0).setValue("none");
                    }
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
        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.KSM_PAGES_TO_SCAN,
                "host1", "HostVO", "2048")), ConfigMutationContext.internal(), false);

        MemoryStateVO queued = host1State();
        assertNotNull("only a pure KSM first dispatch may consume independently observed KSM capability; status="
                + queued.getStatus() + ", plan=" + queued.getPolicyPlanStatus() + ", blockedFields="
                + queued.getPolicyBlockedFields() + ", reason=" + queued.getReason(),
                queued.getActiveTaskUuid());
        assertEquals("Queued", queued.getStatus());
        assertNotNull(queued.getControlOperationUuid());
        assertEquals(2L, repository.taskCount(new APIQueryMemoryTaskMsg()));

        final long sample2 = System.currentTimeMillis();
        repository.observePartialKsm("host2", coldKsmPartial(sample2, false));
        assertEquals("Agent-local Unknown with empty DB fences must be preserved in the persisted partial state",
                "LOCAL_CONTROL_FENCE_UNKNOWN", MemoryCapabilityRules.coldKsmAdmissionReason(
                        repository.states(Collections.singletonList("host2"), 0, 1).get(0),
                        System.currentTimeMillis()));
        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.KSM_PAGES_TO_SCAN,
                "host2", "HostVO", "2048")), ConfigMutationContext.internal(), false);
        MemoryStateVO blocked = repository.states(Collections.singletonList("host2"), 0, 1).get(0);
        assertNull("Agent-local Unknown must not be bypassed because DB activeTask is empty",
                blocked.getActiveTaskUuid());
        assertEquals("Unknown", blocked.getStatus());
        assertEquals("Blocked", blocked.getPolicyPlanStatus());
        assertEquals(4L, repository.taskCount(new APIQueryMemoryTaskMsg()));
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

    @Test public void standardWritebackIntentSavesBeforeBackendAndReconcilesAfterReadyObservation() {
        registerStandardResources();
        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.WRITEBACK_ENABLED,
                null, null, "true")), ConfigMutationContext.internal(), false);
        assertEquals(Boolean.TRUE, repository.transaction(em -> standardConfigAdapter.readEffective(
                em, "writeback.enabled", Collections.singletonList("host1"))));
        assertEquals("Blocked", host1State().getPolicyPlanStatus());
        assertTrue(host1State().getPolicyBlockedFields().contains("ZRAM_NOT_ENABLED"));
        assertNull(host1State().getActiveTaskUuid());

        standardCommit(Arrays.asList(
                standardChange(MemoryStandardField.ZRAM_ENABLED, "host1", "HostVO", "true"),
                standardChange(MemoryStandardField.WRITEBACK_BACKEND_RESOURCE, "host1", "HostVO",
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"),
                standardChange(MemoryStandardField.WRITEBACK_BACKEND_CAPACITY, "host1", "HostVO", "4096")),
                ConfigMutationContext.internal(), false);
        assertEquals("Blocked", host1State().getPolicyPlanStatus());
        assertTrue(host1State().getPolicyBlockedFields().contains("BACKEND_NOT_READY"));
        assertNull(host1State().getActiveTaskUuid());

        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setState("{\"actual\":{\"writeback\":{\"backend\":{"
                    + "\"resource_uuid\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\","
                    + "\"state\":\"PREPARED\",\"fingerprint\":\"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb\","
                    + "\"capacity_bytes\":4096,\"observed_at\":\"" + Instant.now() + "\"}}}}");
            state.setLastSampleTime(System.currentTimeMillis());
            return null;
        });
        assertTrue("ready observation must reuse the existing connect/reconcile path",
                repository.applyEffectivePolicyAfterConnect("host1"));
        MemoryStateVO queued = host1State();
        assertNotNull(queued.getActiveTaskUuid());
        assertEquals("Queued", queued.getPolicyPlanStatus());
        MemoryTaskVO child = repository.findTask(queued.getActiveTaskUuid());
        assertTrue(child.getPolicy().contains("writeback"));
        assertTrue(child.getPolicy().contains("backendResourceUuid"));
    }

    @Test public void standardZramCapacityChangeQueuesDrainBeforeCapacityApply() {
        prepareCapacityObservation("Succeeded", false, false, false);

        standardCommit(Collections.singletonList(standardChange(
                MemoryStandardField.ZRAM_LOGICAL_CAPACITY, null, null, "16384")),
                ConfigMutationContext.internal(), false);

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
                ConfigMutationContext.internal(), false);
        List<MemoryTaskVO> tasks = repository.transaction(em -> em.createQuery(
                "from MemoryTaskVO t where t.hostUuid = :host order by t.createDate", MemoryTaskVO.class)
                .setParameter("host", "host1").getResultList());
        assertEquals("physical capacity change must queue drain", "drain",
                tasks.get(tasks.size() - 1).getAction());
    }

    @Test public void desiredCapacityMatchingEffectiveReadbackDoesNotDrainDespiteStaleAppliedPolicy() {
        prepareCapacityObservation("Succeeded", false, false, false, 16384L, 4096L);
        standardCommit(Collections.singletonList(standardChange(
                MemoryStandardField.ZRAM_LOGICAL_CAPACITY, null, null, "16384")),
                ConfigMutationContext.internal(), false);
        assertEquals(0L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t where t.hostUuid = :host and t.action = :action", Long.class)
                .setParameter("host", "host1").setParameter("action", "drain").getSingleResult()));
    }

    @Test public void staleQueuedDrainIsNotDispatchedAfterCapacityTargetChanges() {
        prepareCapacityObservation("Succeeded", false, false, false);
        standardCommit(Collections.singletonList(standardChange(
                MemoryStandardField.ZRAM_LOGICAL_CAPACITY, null, null, "16384")),
                ConfigMutationContext.internal(), false);
        assertEquals(1L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t where t.hostUuid = :host and t.action = :action and t.status = :status",
                Long.class).setParameter("host", "host1").setParameter("action", "drain")
                .setParameter("status", "Queued").getSingleResult()));
        try {
            standardCommit(Collections.singletonList(standardChange(
                    MemoryStandardField.ZRAM_LOGICAL_CAPACITY, null, null, "32768")),
                    ConfigMutationContext.internal(), false);
        } catch (RuntimeException ignored) {
            // The RED must still prove the old queued drain is not dispatched.
        }
        assertTrue("old drain must not be sent for a newer capacity target",
                repository.claim("capacity-stale-target").isEmpty());
        assertTrue("new target must remain blocked or unknown",
                Arrays.asList("Blocked", "Unknown").contains(host1State().getPolicyPlanStatus())
                        || Arrays.asList("Blocked", "Unknown").contains(host1State().getStatus()));
    }

    @Test public void successfulCapacityDrainQueuesPoolPreparationFromFreshAgentInventory() {
        prepareCapacityObservation("Succeeded", false, false, false);
        standardCommit(Collections.singletonList(standardChange(
                MemoryStandardField.ZRAM_LOGICAL_CAPACITY, null, null, "16384")),
                ConfigMutationContext.internal(), false);
        assertEquals(1L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t where t.hostUuid = :host and t.action = :action and t.status = :status",
                Long.class).setParameter("host", "host1").setParameter("action", "drain")
                .setParameter("status", "Queued").getSingleResult()));
        MemoryTaskVO drain = repository.claim("capacity-drain-agent").stream()
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
                ConfigMutationContext.internal(), false);
        MemoryTaskVO drain = repository.claim("capacity-invalid-inventory").stream()
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
        prepareCapacityObservation("Succeeded", false, false, false);
        String resource = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        String fingerprint = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
        String localFingerprint = "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc";
        String boot = "01234567-89ab-cdef-0123-456789abcdef";
        String pool = "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";
        String candidateId = "candidate:" + localFingerprint;
        repository.transaction(em -> {
            standardConfigAdapter.writeGlobal(em, "writeback.enabled", true);
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "writeback.backendResourceUuid", resource);
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "writeback.backendCapacityBytes", 8192L);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setCapabilities("{\"supported\":true,\"ksm\":true,\"zram\":true,\"writeback\":true}");
            com.google.gson.JsonObject current = new com.google.gson.JsonParser().parse(state.getState()).getAsJsonObject();
            com.google.gson.JsonObject backend = new com.google.gson.JsonObject();
            backend.addProperty("resource_uuid", resource); backend.addProperty("state", "ACTIVE");
            backend.addProperty("active", true); backend.addProperty("fingerprint", fingerprint);
            backend.addProperty("capacity_bytes", 8192L); backend.addProperty("observed_at", Instant.now().toString());
            com.google.gson.JsonObject writeback = new com.google.gson.JsonObject(); writeback.add("backend", backend);
            current.getAsJsonObject("actual").add("writeback", writeback); state.setState(current.toString());
            state.setAppliedPolicy(MemoryPolicyRules.merge(state.getAppliedPolicy(),
                    "{\"schemaVersion\":1,\"writeback\":{\"enabled\":true,\"backendResourceUuid\":\""
                    + resource + "\",\"backendCapacityBytes\":8192}}", "Host"));
            state.setLastSampleTime(System.currentTimeMillis());
            return null;
        });
        standardCommit(Collections.singletonList(
                standardChange(MemoryStandardField.ZRAM_LOGICAL_CAPACITY, "host1", "HostVO", "16384")),
                ConfigMutationContext.internal(), false);
        MemoryTaskVO drain = repository.claim("writeback-backend-capacity-red").stream()
                .filter(task -> "drain".equals(task.getAction())).findFirst().orElse(null);
        assertNotNull("capacity change with an existing backend must drain first: "
                + host1State().getPolicyBlockedFields(), drain);
        assertStateCurrentStep("drain");
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
        MemoryTaskVO preparation = repository.claim("backend-prepare").stream()
                .filter(task -> "prepareWritebackBackend".equals(task.getAction())).findFirst().orElse(null);
        assertNotNull(preparation);
        assertStateCurrentStep("prepareWritebackBackend");
        MemoryTaskVO restore = assertCapacityPreparationQueuesRestore(drain, preparation, queryStage);
        if ("preparation-owner-mismatch".equals(queryStage)) { return; }
        assertStateCurrentStep("activateZramPool");
        assertTrue(repository.claim("backend-activate").stream().anyMatch(task -> task.getUuid().equals(restore.getUuid())));
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
        actual.put("zram", zram); actual.put("writeback", Collections.singletonMap("backend", backend));
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
        assertStateCurrentStep("restoreWriteback");
        assertNotEquals("Succeeded", repository.findTask(drain.getParentUuid()).getStatus());
        repository.result(activationResult.getUuid(), "Succeeded", "duplicate activation", activated, activated);
        assertEquals(writeback.getUuid(), host1State().getActiveTaskUuid());
        assertTrue(repository.claim("backend-restore-writeback").stream()
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
        assertStateCurrentStep(null);
    }

    @Test public void successfulPoolPreparationQueuesRestoreTargetAsSiblingUnderSameParent() {
        prepareCapacityObservation("Succeeded", false, false, false);
        repository.transaction(em -> {
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.enabled", false);
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.pagesToScan", 2000L);
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.sleepMillis", 20L);
            return null;
        });
        standardCommit(Collections.singletonList(standardChange(
                MemoryStandardField.ZRAM_LOGICAL_CAPACITY, "host1", "HostVO", "16384")),
                ConfigMutationContext.internal(), false);
        MemoryTaskVO drain = repository.claim("capacity-activation-red").stream()
                .filter(task -> "drain".equals(task.getAction())).findFirst().orElse(null);
        assertNotNull(drain);
        assertStateCurrentStep("drain");
        String boot = "01234567-89ab-cdef-0123-456789abcdef";
        String pool = "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";
        Map<String, Object> nativeReadback = new LinkedHashMap<>();
        nativeReadback.put("pool_generation", pool);
        nativeReadback.put("device", "/dev/zram0");
        nativeReadback.put("state", "WITHDRAWN_SWAP_INACTIVE_POOL_PRESERVED");
        nativeReadback.put("active", false); nativeReadback.put("reset_performed", false);
        Map<String, Object> receipt = new LinkedHashMap<>(nativeReadback);
        Map<String, Object> drained = new LinkedHashMap<>();
        drained.put("hostUuid", "host1"); drained.put("bootId", boot);
        drained.put("observedAt", Instant.now().toString()); drained.put("status", "AVAILABLE");
        drained.put("poolGeneration", pool); drained.put("controlOperationUuid", drain.getUuid());
        drained.put("canPrepareZramPool", true); drained.put("zramPoolPreparationQuality", "Fresh");
        drained.put("zramPoolPreparationObservedAt", System.currentTimeMillis());
        drained.put("reasons", Collections.emptyList()); drained.put("candidates", Collections.emptyList());
        drained.put("phase", "WITHDRAWN_SWAP_INACTIVE_POOL_PRESERVED"); drained.put("receipt", receipt);
        drained.put("nativeReadback", nativeReadback);
        MemoryAgentResponse drainResponse = new MemoryAgentResponse();
        drainResponse.operationUuid = drain.getUuid(); drainResponse.status = "Succeeded";
        drainResponse.setSuccess(true); drainResponse.hostUuid = "host1";
        drainResponse.appliedRevision = drain.getDesiredRevision(); drainResponse.state = drained;
        repository.result(drain.getUuid(), "Succeeded", "drain completed", drainResponse, drainResponse);
        MemoryTaskVO preparation = repository.claim("capacity-activation-prep").stream()
                .filter(task -> "prepareZramPool".equals(task.getAction())).findFirst().orElse(null);
        assertNotNull(preparation);
        assertStateCurrentStep("prepareZramPool");
        MemoryTaskVO restore = assertCapacityPreparationQueuesRestore(drain, preparation, null);
        assertTrue(repository.claim("capacity-activation").stream()
                .anyMatch(task -> task.getUuid().equals(restore.getUuid())));
        assertStateCurrentStep("activateZramPool");

        MemoryAgentResponse activated = new MemoryAgentResponse();
        activated.operationUuid = restore.getUuid(); activated.status = "Succeeded"; activated.setSuccess(true);
        activated.hostUuid = "host1"; activated.bootId = boot;
        activated.appliedRevision = restore.getDesiredRevision(); activated.sampleTime = System.currentTimeMillis();
        activated.capabilities = new LinkedHashMap<>();
        activated.capabilities.put("supported", true); activated.capabilities.put("ksm", true);
        activated.capabilities.put("ksmZeroPages", true); activated.capabilities.put("zram", true);
        activated.capabilities.put("writeback", false);
        activated.state = completeState(0L, "activated");
        activated.state.put("bootId", boot);
        activated.state.put("lastConfirmedOperationUuid", restore.getUuid());
        activated.state.put("lastConfirmedAppliedRevision", restore.getDesiredRevision());
        activated.state.put("poolGeneration", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
        Map<String, Object> lifecycle = new LinkedHashMap<>();
        lifecycle.put("paused", false); lifecycle.put("activeState", "running");
        activated.state.put("lifecycle", lifecycle);
        Map<String, Object> capacity = new LinkedHashMap<>();
        capacity.put("effective_logical_bytes", 16384L);
        capacity.put("effective_physical_limit_bytes", 4096L);
        Map<String, Object> zram = new LinkedHashMap<>();
        zram.put("enabled", true); zram.put("efficiencyEnabled", true); zram.put("capacity", capacity);
        Map<String, Object> actual = (Map<String, Object>) activated.state.get("actual");
        actual.put("zram", zram);
        Map<String, Object> ksm = new LinkedHashMap<>();
        ksm.put("enabled", false); ksm.put("pagesToScan", 2000L); ksm.put("sleepMillis", 20L);
        actual.put("ksm", ksm); actual.put("writeback", Collections.emptyMap());
        repository.result(restore.getUuid(), "Succeeded", "pool activated", activated, activated);

        assertEquals("RAM activation must close the root; restore=" + restore.getStatus() + "/"
                + restore.getReason() + ", host=" + host1State().getActiveTaskUuid() + "/"
                + host1State().getStatus() + "/" + host1State().getReason() + ", children="
                + repository.transaction(em -> em.createQuery(
                        "select concat(t.action, '/', t.status, '/', t.desiredRevision, '/', t.policy) "
                                + "from MemoryTaskVO t where t.parentUuid=:p order by t.uuid", String.class)
                        .setParameter("p", drain.getParentUuid()).getResultList()),
                "Succeeded", repository.findTask(drain.getParentUuid()).getStatus());
        assertNull(host1State().getActiveTaskUuid());
        assertStateCurrentStep(null);
        assertEquals(0L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t where t.parentUuid=:p and t.action='prepareWritebackBackend'",
                Long.class).setParameter("p", drain.getParentUuid()).getSingleResult()));
        MemoryPolicyConfig applied = MemoryPolicyRules.decode(host1State().getAppliedPolicy());
        assertEquals(Long.valueOf(16384L), applied.zram.logicalCapacityBytes);
        assertEquals(Long.valueOf(4096L), applied.zram.ramLimitBytes);
        assertEquals(Boolean.FALSE, applied.ksm.enabled);
        assertEquals(Long.valueOf(2000L), applied.ksm.pagesToScan);
        assertEquals(Long.valueOf(20L), applied.ksm.sleepMillis);
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
        APIUpdateMemoryPolicyMsg reconcile = request(repository.getPolicy("Host", "host1").getRevision());
        reconcile.setScope("Host"); reconcile.setResourceUuid("host1"); reconcile.setTargetHostUuids(null);
        reconcile.setAction("reconcile"); reconcile.setPolicy("{}");
        MemoryTaskInventory accepted = repository.submit(reconcile);
        MemoryTaskVO alias = repository.claim("capacity-query").stream()
                .filter(task -> accepted.getUuid().equals(task.getParentUuid())).findFirst().get();
        assertEquals(original.getUuid(), alias.getReconcileOperationUuid());
        return alias;
    }

    private MemoryTaskVO assertCapacityPreparationQueuesRestore(MemoryTaskVO drain, MemoryTaskVO preparation,
            String queryStage) {
        com.google.gson.JsonObject context = new com.google.gson.JsonParser().parse(drain.getPolicy()).getAsJsonObject();
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
                    MemoryTaskVO.class).setParameter("parent", drain.getParentUuid())
                .setParameter("expected", drain.getUuid())
                .setMaxResults(1).getResultList();
            return rows.isEmpty() ? null : rows.get(0);
        });
        assertNotNull("successful preparation must queue the retained restore target; prep="
                + repository.findTask(preparation.getUuid()).getStatus() + "/"
                + repository.findTask(preparation.getUuid()).getReason() + ", host="
                + host1State().getStatus() + "/" + host1State().getReason()
                + ", drainPolicy=" + drain.getPolicy() + ", effective="
                + repository.getPolicy("Host", "host1").getEffectivePolicy(), restore);
        assertTrue("restore action must remain a standard activation action: " + restore.getAction(),
                "apply".equals(restore.getAction()) || "clearOverride".equals(restore.getAction()));
        assertEquals("restore=" + restore.getUuid() + ", action=" + restore.getAction()
                + ", parent=" + restore.getParentUuid() + ", expected=" + restore.getExpectedControlOperationUuid(),
                "Queued", restore.getStatus());
        assertEquals(drain.getParentUuid(), restore.getParentUuid());
        assertEquals(drain.getUuid(), restore.getExpectedControlOperationUuid());
        return restore;
    }

    private void assertStateCurrentStep(String expected) {
        MemoryQueryPage<MemoryStateVO> page = repository.statePage(Collections.singletonList("host1"), 0, 10, null);
        assertEquals(1, page.items.size());
        Map<String, Object> inventory = org.zstack.utils.gson.JSONObjectUtil.toObject(
                new com.google.gson.Gson().toJson(page.items.get(0).toInventory()), Map.class);
        assertEquals(expected, inventory.get("currentStep"));
    }

    @Test public void statePageCurrentStepHasNoValueForEmptyPage() {
        MemoryQueryPage<MemoryStateVO> page = repository.statePage(null, 500, 10, null);
        assertTrue(page.items.isEmpty());
    }

    @Test public void statePageCurrentStepIgnoresTerminalOldActivePointer() {
        repository.transaction(em -> {
            em.persist(testTask("terminal-old-step", null, "drain", "Succeeded", null));
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setActiveTaskUuid("terminal-old-step");
            return null;
        });
        assertStateCurrentStep(null);
    }

    @Test public void statePageCurrentStepPreservesKnownUnknownOperation() {
        repository.transaction(em -> {
            em.persist(testTask("unknown-step", null, "prepareZramPool", "Unknown", null));
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Unknown"); state.setActiveTaskUuid("unknown-step"); state.setDesiredRevision(0L);
            return null;
        });
        assertStateCurrentStep("prepareZramPool");
        MemoryStateVO state = host1State();
        assertEquals("Unknown", state.getStatus());
        assertEquals("unknown-step", state.getActiveTaskUuid());
        assertEquals(0L, state.getDesiredRevision());
    }

    @Test public void statePageCurrentStepIgnoresMissingCrossHostAndWrongRevisionBindings() {
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Unknown"); state.setActiveTaskUuid("missing-active"); state.setDesiredRevision(7L);
            return null;
        });
        MemoryStateVO missing = repository.statePage(Collections.singletonList("host1"), 0, 10, null).items.get(0);
        assertNull(missing.getCurrentStep());
        assertEquals("Unknown", missing.getStatus());
        assertEquals("missing-active", missing.getActiveTaskUuid());
        assertEquals(7L, missing.getDesiredRevision());

        repository.transaction(em -> {
            MemoryTaskVO crossHost = testTask("cross-host-step", null, "drain", "Applying", null);
            crossHost.setHostUuid("host2"); em.persist(crossHost);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Unknown"); state.setActiveTaskUuid("cross-host-step"); state.setDesiredRevision(0L);
            return null;
        });
        MemoryStateVO crossHost = repository.statePage(Collections.singletonList("host1"), 0, 10, null).items.get(0);
        assertNull(crossHost.getCurrentStep());
        assertEquals("Unknown", crossHost.getStatus());
        assertEquals("cross-host-step", crossHost.getActiveTaskUuid());
        assertEquals(0L, crossHost.getDesiredRevision());

        repository.transaction(em -> {
            MemoryTaskVO wrongRevision = testTask("wrong-revision-step", null, "drain", "Applying", null);
            wrongRevision.setDesiredRevision(9L); em.persist(wrongRevision);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Unknown"); state.setActiveTaskUuid("wrong-revision-step"); state.setDesiredRevision(8L);
            return null;
        });
        MemoryStateVO wrongRevision = repository.statePage(Collections.singletonList("host1"), 0, 10, null).items.get(0);
        assertNull(wrongRevision.getCurrentStep());
        assertEquals("Unknown", wrongRevision.getStatus());
        assertEquals("wrong-revision-step", wrongRevision.getActiveTaskUuid());
        assertEquals(8L, wrongRevision.getDesiredRevision());
    }

    @Test public void metricsInventoryRetainsTransientCurrentStep() {
        MemoryStateVO state = host1State();
        state.setCurrentStep("drain");
        MemoryStateInventory inventory = MemoryMetricsPresentation.inventory(state, null,
                System.currentTimeMillis());
        assertEquals("drain", inventory.getCurrentStep());
        Map<String, Object> json = org.zstack.utils.gson.JSONObjectUtil.toObject(
                new com.google.gson.Gson().toJson(inventory), Map.class);
        assertEquals("drain", json.get("currentStep"));
    }

    @Test public void standardZramCapacityChangeWithMatchingEffectiveCapacityDoesNotDrain() {
        prepareCapacityObservation("Succeeded", false, false, false);

        standardCommit(Collections.singletonList(standardChange(
                MemoryStandardField.ZRAM_LOGICAL_CAPACITY, null, null, "8192")),
                ConfigMutationContext.internal(), false);

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
            standardConfigAdapter.writeGlobal(em, "zram.enabled", true);
            standardConfigAdapter.writeGlobal(em, "zram.logicalCapacityBytes", 8192L);
            standardConfigAdapter.writeGlobal(em, "zram.ramLimitBytes", 4096L);
            standardConfigAdapter.writeGlobal(em, "writeback.enabled", false);
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
                    + "\"logicalCapacityBytes\":8192,\"ramLimitBytes\":4096}}");
            state.setAppliedPolicyHash("807688c75514e917154c2e66c1b89b31fe9b0817784cafdb5d38c506b84dad6a");
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
                ConfigMutationContext.internal(), false);
        repository.transaction(em -> {
            em.find(TestHost.class, "host1").status = HostStatus.Connected;
            return null;
        });
        assertTrue("reconnect must queue drain for a changed effective capacity",
                repository.applyEffectivePolicyAfterConnect("host1"));
        MemoryTaskVO child = repository.claim("capacity-reconnect-agent").stream()
                .filter(task -> "host1".equals(task.getHostUuid())).findFirst().orElse(null);
        assertNotNull(child);
        assertEquals("drain", child.getAction());
    }

    @Test public void revertingBlockedCapacityTargetToAlreadyAppliedPolicyClearsOldPlanWithoutChild() {
        prepareCapacityObservation("Succeeded", false, false, false);
        repository.transaction(em -> {
            standardConfigAdapter.writeGlobal(em, "ksm.enabled", false);
            standardConfigAdapter.writeGlobal(em, "ksm.pagesToScan", 2000L);
            standardConfigAdapter.writeGlobal(em, "ksm.sleepMillis", 20L);
            em.find(TestHost.class, "host1").status = HostStatus.Disconnected;
            return null;
        });

        standardCommit(Collections.singletonList(standardChange(
                MemoryStandardField.ZRAM_LOGICAL_CAPACITY, null, null, "16384")),
                ConfigMutationContext.internal(), false);
        MemoryStateVO blocked = host1State();
        assertEquals("Blocked", blocked.getPolicyPlanStatus());
        assertNull(blocked.getActiveTaskUuid());
        String control = blocked.getControlOperationUuid();
        Long desiredRevision = blocked.getDesiredRevision();
        Long appliedRevision = blocked.getAppliedRevision();
        long hostTasks = repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t where t.hostUuid = :host", Long.class)
                .setParameter("host", "host1").getSingleResult());

        repository.transaction(em -> {
            em.find(TestHost.class, "host1").status = HostStatus.Connected;
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Succeeded"); state.setActiveTaskUuid(null);
            state.setLastSampleTime(System.currentTimeMillis());
            return null;
        });
        standardCommit(Collections.singletonList(standardChange(
                MemoryStandardField.ZRAM_LOGICAL_CAPACITY, null, null, "8192")),
                ConfigMutationContext.internal(), false);

        MemoryStateVO settled = host1State();
        assertEquals("Applied", settled.getPolicyPlanStatus());
        assertNull(settled.getActiveTaskUuid());
        assertEquals(control, settled.getControlOperationUuid());
        assertEquals(desiredRevision.longValue(), settled.getDesiredRevision());
        assertEquals(appliedRevision.longValue(), settled.getAppliedRevision().longValue());
        assertEquals(hostTasks, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t where t.hostUuid = :host", Long.class)
                .setParameter("host", "host1").getSingleResult()));
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
            standardConfigAdapter.writeGlobal(em, "ksm.enabled", false);
            standardConfigAdapter.writeGlobal(em, "ksm.pagesToScan", 2000L);
            standardConfigAdapter.writeGlobal(em, "ksm.sleepMillis", 20L);
            em.find(MemoryStateVO.class, "host1").setPolicyPlanStatus("Blocked");
            return null;
        });
    }

    private void assertNoopKeepsBlockedPlan() { assertNoopKeepsPlan("Blocked"); }

    private void assertNoopKeepsPlan(String expected) {
        standardCommit(Collections.singletonList(standardChange(
                MemoryStandardField.ZRAM_LOGICAL_CAPACITY, null, null, "8192")),
                ConfigMutationContext.internal(), false);
        assertEquals(expected, host1State().getPolicyPlanStatus());
    }

    @Test public void capacityChangeDoesNotDrainUnknownHost() {
        prepareCapacityObservation("Unknown", false, false, false);
        standardCommit(Collections.singletonList(standardChange(
                MemoryStandardField.ZRAM_LOGICAL_CAPACITY, null, null, "16384")),
                ConfigMutationContext.internal(), false);
        assertEquals("Blocked", host1State().getPolicyPlanStatus());
        assertEquals(0L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t where t.action = :action", Long.class)
                .setParameter("action", "drain").getSingleResult()));
    }

    @Test public void capacityChangeDoesNotDrainPausedHost() {
        prepareCapacityObservation("Succeeded", true, false, false);
        standardCommit(Collections.singletonList(standardChange(
                MemoryStandardField.ZRAM_LOGICAL_CAPACITY, null, null, "16384")),
                ConfigMutationContext.internal(), false);
        assertEquals("Blocked", host1State().getPolicyPlanStatus());
        assertEquals(0L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t where t.action = :action", Long.class)
                .setParameter("action", "drain").getSingleResult()));
    }

    @Test public void capacityChangeDoesNotDrainStaleObservation() {
        prepareCapacityObservation("Succeeded", false, true, false);
        standardCommit(Collections.singletonList(standardChange(
                MemoryStandardField.ZRAM_LOGICAL_CAPACITY, null, null, "16384")),
                ConfigMutationContext.internal(), false);
        assertEquals("Blocked", host1State().getPolicyPlanStatus());
        assertEquals(0L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t where t.action = :action", Long.class)
                .setParameter("action", "drain").getSingleResult()));
    }

    @Test public void ksmOnlyChangeDoesNotDrainZramPool() {
        prepareCapacityObservation("Succeeded", false, false, true);
        standardCommit(Collections.singletonList(standardChange(
                MemoryStandardField.KSM_PAGES_TO_SCAN, null, null, "3000")),
                ConfigMutationContext.internal(), false);
        assertEquals(0L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t where t.action = :action", Long.class)
                .setParameter("action", "drain").getSingleResult()));
    }

    @Test public void disablingWritebackDoesNotDeleteHostBackendAndDeleteRestoresInheritance() {
        registerStandardResources();
        standardCommit(Arrays.asList(
                standardChange(MemoryStandardField.WRITEBACK_BACKEND_RESOURCE, "host1", "HostVO",
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"),
                standardChange(MemoryStandardField.WRITEBACK_BACKEND_CAPACITY, "host1", "HostVO", "4096")),
                ConfigMutationContext.internal(), false);
        assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", repository.transaction(em ->
                String.valueOf(standardConfigAdapter.readOverrideOwn(em, "writeback.backendResourceUuid", "host1"))));

        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.WRITEBACK_ENABLED,
                "host1", "HostVO", "false")), ConfigMutationContext.internal(), false);
        assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", repository.transaction(em ->
                String.valueOf(standardConfigAdapter.readOverrideOwn(em, "writeback.backendResourceUuid", "host1"))));

        standardCommit(Collections.singletonList(new ConfigMutation(
                MemoryStandardField.WRITEBACK_ENABLED.category(),
                MemoryStandardField.WRITEBACK_ENABLED.configName(), "host1", "HostVO", null, true)),
                ConfigMutationContext.internal(), false);
        assertNull(repository.transaction(em -> standardConfigAdapter.readOverrideOwn(
                em, "writeback.enabled", "host1")));
        assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", repository.transaction(em ->
                String.valueOf(standardConfigAdapter.readOverrideOwn(em, "writeback.backendResourceUuid", "host1"))));
    }

    @Test public void inheritedWritebackEnableCarriesHostBackendInDeltaWithoutHostEnableOverride() {
        registerStandardResources();
        repository.transaction(em -> {
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "zram.enabled", true);
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "writeback.backendResourceUuid",
                    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "writeback.backendCapacityBytes", 4096L);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setState("{\"actual\":{\"writeback\":{\"backend\":{"
                    + "\"resource_uuid\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\",\"state\":\"PREPARED\","
                    + "\"fingerprint\":\"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb\","
                    + "\"capacity_bytes\":4096,\"observed_at\":\"" + Instant.now() + "\"}}}}");
            state.setAppliedPolicy("{\"schemaVersion\":1,\"zram\":{\"enabled\":true},\"writeback\":{"
                    + "\"enabled\":false,\"backendResourceUuid\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\","
                    + "\"backendCapacityBytes\":4096}}");
            state.setAppliedPolicyHash("baseline-policy");
            return null;
        });
        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.WRITEBACK_ENABLED,
                null, null, "true")), ConfigMutationContext.internal(), false);
        assertNull(repository.transaction(em -> standardConfigAdapter.readOverrideOwn(
                em, "writeback.enabled", "host1")));
        MemoryTaskVO child = repository.claim("mn").stream()
                .filter(task -> "host1".equals(task.getHostUuid())).findFirst().get();
        assertTrue(child.getPolicy().contains("backendResourceUuid"));
        assertTrue(child.getPolicy().contains("backendCapacityBytes"));
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

    @Test public void standardWriteAfterCompletedDrainWaitsForEnabledZramActivation() {
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
        assertNull("Agent maintenance apply requires an enabled ZRAM initialization payload",
                host1State().getActiveTaskUuid());
        assertEquals("completed-drain", host1State().getControlOperationUuid());
        assertEquals("Blocked", host1State().getPolicyPlanStatus());
        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.ZRAM_ENABLED,
                "host1", "HostVO", "true")), ConfigMutationContext.internal(), false);
        assertNotNull(host1State().getActiveTaskUuid());
        assertNotEquals("completed-drain", host1State().getControlOperationUuid());
        assertEquals(1, repository.claim("mn").size());
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

    @Test public void standardMutationUsesScalarRegistryWithoutLoadingPolymorphicResource() throws Exception {
        registerStandardResources();
        java.lang.reflect.Method mutation = MemoryRepository.class.getDeclaredMethod(
                "standardMutation", EntityManager.class, List.class);
        mutation.setAccessible(true);
        for (String[] target : Arrays.asList(new String[]{"host1", "HostVO", "Host"},
                new String[]{"cluster1", "ClusterVO", "Cluster"})) {
            EntityManager em = factory.createEntityManager();
            try {
                factory.getStatistics().clear();
                Object result = mutation.invoke(repository, em, Collections.singletonList(
                        standardChange(MemoryStandardField.KSM_ENABLED, target[0], target[1], "false")));
                Field scope = result.getClass().getDeclaredField("scope"); scope.setAccessible(true);
                assertEquals(target[2], scope.get(result));
                assertEquals("registry lookup must not load the JOINED resource entity graph", 0L,
                        factory.getStatistics().getEntityStatistics(ResourceVO.class.getName()).getLoadCount());
            } finally { em.close(); }
        }
    }

    @Test public void standardMutationRejectsMissingOrMismatchedResourceWithoutRevisionChanges() {
        registerStandardResources();
        repository.transaction(em -> {
            em.createQuery("update ResourceVO r set r.resourceType = null where r.uuid = :uuid")
                    .setParameter("uuid", "host2").executeUpdate();
            return null;
        });
        for (ConfigMutation invalid : Arrays.asList(
                standardChange(MemoryStandardField.KSM_ENABLED, "cluster1", "HostVO", "false"),
                standardChange(MemoryStandardField.KSM_ENABLED, "host2", "HostVO", "false"),
                standardChange(MemoryStandardField.KSM_ENABLED, "missing-resource", "HostVO", "true"))) {
            try {
                standardCommit(Collections.singletonList(invalid), ConfigMutationContext.internal(), false);
                fail("resource registry identity must exist and match the requested type");
            } catch (MemoryOperationException expected) {
                assertEquals("MEMORY_INVALID_SCOPE", expected.getCode());
            }
        }
        assertEquals(0L, repository.getPolicy("Host", "host1").getRevision());
        assertEquals(0L, repository.getPolicy("Cluster", "cluster1").getRevision());
        for (String[] target : Arrays.asList(new String[]{"host1", "HostVO", "Host"},
                new String[]{"cluster1", "ClusterVO", "Cluster"})) {
            standardCommit(Collections.singletonList(standardChange(MemoryStandardField.KSM_ENABLED,
                    target[0], target[1], "true")), ConfigMutationContext.internal(), false);
            assertEquals(1L, repository.getPolicy(target[2], target[0]).getRevision());
        }
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

    private void prepareAppliedKsmForStandardClear() {
        registerStandardResources();
        repository.transaction(em -> {
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.enabled", false);
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
        standardCommit(Collections.singletonList(new ConfigMutation(MemoryStandardField.KSM_ENABLED.category(),
                MemoryStandardField.KSM_ENABLED.configName(), "host1", "HostVO", null, true)),
                ConfigMutationContext.internal(), false);
    }

    @Test public void standardKsmSingleFieldChangesRetainEffectiveScanParameters() {
        prepareAppliedKsmForStandardClear();
        repository.transaction(em -> {
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.pagesToScan", 2500L);
            standardConfigAdapter.writeOverride(em, "cluster1", "ClusterVO", "ksm.sleepMillis", 40L);
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.zeroPagesEnabled", false);
            em.find(MemoryStateVO.class, "host1").setAppliedPolicy(
                    "{\"schemaVersion\":1,\"ksm\":{\"enabled\":false,\"pagesToScan\":2500,"
                            + "\"sleepMillis\":40,\"zeroPagesEnabled\":false}}");
            return null;
        });
        MemoryStandardField[] fields = {MemoryStandardField.KSM_ENABLED, MemoryStandardField.KSM_ENABLED,
                MemoryStandardField.KSM_ZERO_PAGES, MemoryStandardField.KSM_ZERO_PAGES,
                MemoryStandardField.KSM_PAGES_TO_SCAN, MemoryStandardField.KSM_SLEEP_MILLIS};
        String[] values = {"true", "false", "true", "false", "3000", "30"};
        for (int i = 0; i < fields.length; i++) {
            standardCommit(Collections.singletonList(standardChange(fields[i], "host1", "HostVO", values[i])),
                    ConfigMutationContext.internal(), false);
            MemoryTaskVO task = repository.claim("mn-ksm-delta").stream()
                    .filter(t -> "host1".equals(t.getHostUuid())).findFirst().get();
            MemoryPolicyConfig payload = MemoryPolicyRules.decode(task.getPolicy());
            assertEquals("apply", task.getAction());
            assertEquals("a single-field update must not reset the other scan setting",
                    Long.valueOf(i < 4 ? 2500 : 3000), payload.ksm.pagesToScan);
            assertEquals(Long.valueOf(i < 5 ? 40 : 30), payload.ksm.sleepMillis);
            if (i < 2) {
                assertEquals(Boolean.valueOf(values[i]), payload.ksm.enabled);
                assertNull("an unchanged zero switch must not be replayed", payload.ksm.zeroPagesEnabled);
            } else {
                assertNull("independent tuning must not start or stop KSM", payload.ksm.enabled);
            }
            MemoryAgentResponse ack = new MemoryAgentResponse(); ack.appliedRevision = task.getDesiredRevision();
            repository.result(task.getUuid(), "Succeeded", "native parameter receipt", ack);
            if (i < 5) {
                assertNull("execution payload must not materialize inherited sleep as a Host row",
                        repository.transaction(em ->
                                standardConfigAdapter.readOverrideOwn(em, "ksm.sleepMillis", "host1")));
            }
            assertFalse("the same effective target must not be redispatched",
                    repository.applyEffectivePolicyAfterConnect("host1"));
        }
        assertEquals("carrying inherited sleep in a command must not create a Host override",
                Long.valueOf(40), repository.transaction(em ->
                        standardConfigAdapter.readOverrideOwn(em, "ksm.sleepMillis", "cluster1")));
        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.KSM_ENABLED,
                "host1", "HostVO", "none")), ConfigMutationContext.internal(), false);
        MemoryTaskVO clear = repository.claim("mn-ksm-none").stream()
                .filter(t -> "host1".equals(t.getHostUuid())).findFirst().get();
        MemoryPolicyConfig unmanaged = MemoryPolicyRules.decode(clear.getPolicy());
        assertEquals("clearOverride", clear.getAction());
        assertNull("none is not an implicit false", unmanaged.ksm.enabled);
        assertEquals(Long.valueOf(3000), unmanaged.ksm.pagesToScan);
        assertEquals(Long.valueOf(30), unmanaged.ksm.sleepMillis);
    }

    @Test public void reconnectRepairsOnlyTrustedKsmActualDriftWhileRunRemainsDisabled() {
        registerStandardResources();
        repository.transaction(em -> {
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.enabled", false);
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.zeroPagesEnabled", true);
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.pagesToScan", 2000L);
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.sleepMillis", 20L);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Succeeded"); state.setDesiredRevision(8); state.setAppliedRevision(8L);
            state.setAppliedPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":false,\"zeroPagesEnabled\":true,\"pagesToScan\":2000,\"sleepMillis\":20}}");
            state.setAppliedPolicyHash("saved-policy-r8"); state.setControlOperationUuid("ksm-r8");
            state.setCapabilities("{\"supported\":true,\"ksm\":true,\"ksmZeroPages\":true}");
            state.setLastSampleTime(System.currentTimeMillis());
            state.setState("{\"bootId\":\"boot-a\",\"lastConfirmedOperationUuid\":\"ksm-r8\","
                    + "\"lastConfirmedAppliedRevision\":8,\"actual\":{\"ksm\":{\"enabled\":false,"
                    + "\"zeroPagesEnabled\":true,\"pagesToScan\":2000,\"sleepMillis\":20}}}");
            MemoryTaskVO control = testTask("ksm-r8", null, "apply", "Succeeded", null);
            control.setDesiredRevision(8); em.persist(control);
            return null;
        });
        long tasksBeforeRepair = repository.taskCount(new APIQueryMemoryTaskMsg());

        assertFalse("matching same-boot KSM values are not redispatched",
                repository.applyEffectivePolicyAfterConnect("host1"));
        assertTrue(repository.claim("mn-ksm-matching-values").isEmpty());
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setState("{\"bootId\":\"boot-a\",\"lastConfirmedOperationUuid\":\"ksm-r8\","
                    + "\"lastConfirmedAppliedRevision\":8,\"actual\":{\"ksm\":{\"enabled\":false,"
                    + "\"zeroPagesEnabled\":false,\"pagesToScan\":2000,\"sleepMillis\":20}}}");
            state.setLastSampleTime(System.currentTimeMillis());
            return null;
        });
        assertTrue("same-boot readback shows only zero-page drift", repository.applyEffectivePolicyAfterConnect("host1"));
        MemoryTaskVO repair = repository.claim("mn-ksm-actual-drift").stream()
                .filter(t -> "host1".equals(t.getHostUuid())).findFirst().get();
        MemoryPolicyConfig payload = MemoryPolicyRules.decode(repair.getPolicy());
        assertEquals(Boolean.TRUE, payload.ksm.zeroPagesEnabled);
        assertNull("actual-value repair must never start KSM", payload.ksm.enabled);
        assertEquals(Long.valueOf(2000), payload.ksm.pagesToScan);
        assertEquals(Long.valueOf(20), payload.ksm.sleepMillis);
        assertFalse("the same actual drift must not create parallel repair tasks",
                repository.applyEffectivePolicyAfterConnect("host1"));
        assertEquals("repair adds exactly the normal parent and Host task, not duplicate tasks",
                tasksBeforeRepair + 2, repository.taskCount(new APIQueryMemoryTaskMsg()));
    }

    @Test public void reconnectRestoresOnlyPreviouslyConfirmedKsmFieldsAcrossBoot() {
        registerStandardResources();
        String policy = "{\"schemaVersion\":1,\"ksm\":{\"enabled\":true,"
                + "\"zeroPagesEnabled\":true,\"pagesToScan\":3000,\"sleepMillis\":30}}";
        seedConfirmedKsmReconnect(policy);
        long tasksBefore = repository.taskCount(new APIQueryMemoryTaskMsg());

        assertTrue(repository.applyEffectivePolicyAfterConnect("host1"));
        MemoryTaskVO restore = repository.claim("mn-ksm-reconnect-restore").stream()
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
        assertEquals(tasksBefore + 2, repository.taskCount(new APIQueryMemoryTaskMsg()));
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
            long before = repository.taskCount(new APIQueryMemoryTaskMsg());
            assertTrue(drift, repository.applyEffectivePolicyAfterConnect("host1"));
            MemoryTaskVO restore = repository.claim("mn-ksm-delta-reconnect").stream()
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
            assertEquals(before + 2, repository.taskCount(new APIQueryMemoryTaskMsg()));
            assertEquals(delta, repository.transaction(em -> em.find(MemoryTaskVO.class, "confirmed-ksm-r8").getPolicy()));
        }
    }

    @Test public void reconnectKsmRestoreRequiresExactPriorControlAndFreshActualReadback() {
        registerStandardResources();
        String policy = "{\"schemaVersion\":1,\"ksm\":{\"enabled\":false,"
                + "\"zeroPagesEnabled\":true,\"pagesToScan\":2000,\"sleepMillis\":20}}";
        repository.transaction(em -> {
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.enabled", false);
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.zeroPagesEnabled", true);
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.pagesToScan", 2000L);
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.sleepMillis", 20L);
            return null;
        });
        for (String defect : Arrays.asList("matched", "unknown", "stale", "partial", "paused",
                "pendingRevision", "wrongControl", "wrongAppliedHash", "missingActual", "unsupportedZero",
                "emptyDelta", "conflictingDelta", "unknownDelta", "wrongDeltaType")) {
            repository.transaction(em -> {
                em.createQuery("delete from MemoryTaskVO").executeUpdate();
                String controlPolicy = "emptyDelta".equals(defect) ? "{\"schemaVersion\":1,\"ksm\":{}}"
                        : "conflictingDelta".equals(defect) ? policy.replace("2000", "2500")
                        : "unknownDelta".equals(defect) ? policy.replace("\"enabled\":false", "\"unrecognized\":false")
                        : "wrongDeltaType".equals(defect) ? policy.replace("\"enabled\":false", "\"enabled\":0") : policy;
                MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
                state.setStatus("unknown".equals(defect) ? "Unknown" : "Succeeded");
                state.setActiveTaskUuid(null);
                state.setDesiredRevision("pendingRevision".equals(defect) ? 9 : 8);
                state.setAppliedRevision(8L);
                state.setAppliedPolicy(policy);
                state.setAppliedPolicyHash("wrongAppliedHash".equals(defect) ? "wrong" : sha256(controlPolicy));
                state.setControlOperationUuid("wrongControl".equals(defect) ? "missing-control" : "confirmed-ksm-r8");
                state.setCapabilities("partial".equals(defect)
                        ? "{\"supported\":true,\"ksm\":true,\"ksmZeroPages\":true,"
                            + "\"nativeKsmPartial\":{\"observationKind\":\"KSM_NATIVE_PARTIAL\"}}"
                        : "unsupportedZero".equals(defect)
                            ? "{\"supported\":true,\"ksm\":true,\"ksmZeroPages\":false}"
                            : "{\"supported\":true,\"ksm\":true,\"ksmZeroPages\":true}");
                state.setLastSampleTime("stale".equals(defect)
                        ? System.currentTimeMillis() - MemoryOptimizationGlobalConfig.displayTtlMillis() - 1
                        : System.currentTimeMillis());
                String lifecycle = "paused".equals(defect)
                        ? ",\"lifecycle\":{\"paused\":true,\"activeState\":\"paused\"}" : "";
                String actualKsm = "matched".equals(defect)
                        ? "\"enabled\":false,\"zeroPagesEnabled\":true,\"pagesToScan\":2000,\"sleepMillis\":20"
                        : "\"enabled\":true,\"zeroPagesEnabled\":false,\"pagesToScan\":100,\"sleepMillis\":10";
                if ("missingActual".equals(defect)) { actualKsm = "\"enabled\":true,\"pagesToScan\":100,\"sleepMillis\":10"; }
                state.setState("{\"bootId\":\"boot-b\"" + lifecycle + ",\"actual\":{\"ksm\":{" + actualKsm + "}}}");
                MemoryTaskVO control = testTask("confirmed-ksm-r8", null, "apply", "Succeeded", null);
                control.setDesiredRevision(8); control.setPolicy(controlPolicy); em.persist(control);
                return null;
            });
            long tasksBefore = repository.taskCount(new APIQueryMemoryTaskMsg());
            assertFalse(defect, repository.applyEffectivePolicyAfterConnect("host1"));
            assertTrue(defect, repository.claim("mn-ksm-untrusted-reconnect").isEmpty());
            assertEquals(defect, tasksBefore, repository.taskCount(new APIQueryMemoryTaskMsg()));
        }
    }

    private void seedConfirmedKsmReconnect(String policy) {
        repository.transaction(em -> {
            Map<String, Object> ksm = (Map<String, Object>) org.zstack.utils.gson.JSONObjectUtil.toObject(policy, Map.class).get("ksm");
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.enabled", ksm.get("enabled"));
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.zeroPagesEnabled", ksm.get("zeroPagesEnabled"));
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.pagesToScan", ((Number) ksm.get("pagesToScan")).longValue());
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.sleepMillis", ((Number) ksm.get("sleepMillis")).longValue());
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

    @Test public void reconnectDoesNotRepairKsmFromUnknownStaleOrIncompleteObservation() {
        registerStandardResources();
        repository.transaction(em -> {
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.enabled", false);
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.zeroPagesEnabled", true);
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.pagesToScan", 2000L);
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.sleepMillis", 20L);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Unknown"); state.setDesiredRevision(8); state.setAppliedRevision(8L);
            state.setAppliedPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":false,\"zeroPagesEnabled\":true,\"pagesToScan\":2000,\"sleepMillis\":20}}");
            state.setAppliedPolicyHash("saved-policy-r8"); state.setControlOperationUuid("ksm-r8");
            state.setCapabilities("{\"supported\":true,\"ksm\":true,\"ksmZeroPages\":true}");
            state.setLastSampleTime(System.currentTimeMillis());
            state.setState("{\"bootId\":\"boot-a\",\"lastConfirmedOperationUuid\":\"ksm-r8\","
                    + "\"lastConfirmedAppliedRevision\":8,\"actual\":{\"ksm\":{\"enabled\":false,"
                    + "\"zeroPagesEnabled\":false,\"pagesToScan\":2000,\"sleepMillis\":20}}}");
            MemoryTaskVO control = testTask("ksm-r8", null, "apply", "Succeeded", null);
            control.setDesiredRevision(8); em.persist(control);
            return null;
        });
        assertFalse(repository.applyEffectivePolicyAfterConnect("host1"));

        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Succeeded");
            state.setState("{\"bootId\":\"boot-a\",\"lastConfirmedOperationUuid\":\"ksm-r8\","
                    + "\"lastConfirmedAppliedRevision\":8,\"actual\":{\"ksm\":{\"enabled\":false,"
                    + "\"pagesToScan\":2000,\"sleepMillis\":20}}}");
            return null;
        });
        assertFalse("a missing actual zero-page value is not treated as zero", repository.applyEffectivePolicyAfterConnect("host1"));
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setState("{\"bootId\":\"boot-a\",\"lastConfirmedOperationUuid\":\"ksm-r8\","
                    + "\"lastConfirmedAppliedRevision\":8,\"actual\":{\"ksm\":{\"enabled\":false,"
                    + "\"zeroPagesEnabled\":false,\"pagesToScan\":2000,\"sleepMillis\":20}}}");
            state.setLastSampleTime(System.currentTimeMillis() - MemoryOptimizationGlobalConfig.displayTtlMillis() - 1);
            return null;
        });
        assertFalse("stale same-boot values cannot authorize a repair", repository.applyEffectivePolicyAfterConnect("host1"));
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Succeeded"); state.setLastSampleTime(System.currentTimeMillis());
            state.setCapabilities("{\"supported\":true,\"ksm\":true,\"ksmZeroPages\":false}");
            state.setState("{\"bootId\":\"boot-a\",\"lastConfirmedOperationUuid\":\"ksm-r8\","
                    + "\"lastConfirmedAppliedRevision\":8,\"actual\":{\"ksm\":{\"enabled\":false,"
                    + "\"zeroPagesEnabled\":false,\"pagesToScan\":2000,\"sleepMillis\":20}}}");
            return null;
        });
        assertFalse("unsupported zero-page capability cannot dispatch an actual-value repair",
                repository.applyEffectivePolicyAfterConnect("host1"));
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setCapabilities("{\"supported\":true,\"ksm\":true,\"ksmZeroPages\":true}");
            state.setState("{\"bootId\":\"boot-a\",\"lastConfirmedOperationUuid\":\"ksm-r8\","
                    + "\"lastConfirmedAppliedRevision\":8,\"actual\":{\"ksm\":{\"enabled\":true,"
                    + "\"zeroPagesEnabled\":false,\"pagesToScan\":2000,\"sleepMillis\":20}}}");
            return null;
        });
        assertFalse("actual run=1 is never changed by this disabled-target repair path",
                repository.applyEffectivePolicyAfterConnect("host1"));
        for (String defect : Arrays.asList("wrong-control", "wrong-revision", "missing-boot", "active-task", "paused", "partial")) {
            repository.transaction(em -> {
                MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
                state.setStatus("Succeeded"); state.setAppliedRevision(8L); state.setDesiredRevision(8);
                state.setControlOperationUuid("ksm-r8"); state.setLastSampleTime(System.currentTimeMillis());
                state.setActiveTaskUuid("active-task".equals(defect) ? "in-flight" : null);
                state.setCapabilities("partial".equals(defect)
                        ? "{\"supported\":true,\"ksm\":true,\"ksmZeroPages\":true,"
                                + "\"nativeKsmPartial\":{\"observationKind\":\"KSM_NATIVE_PARTIAL\"}}"
                        : "{\"supported\":true,\"ksm\":true,\"ksmZeroPages\":true}");
                String boot = "missing-boot".equals(defect) ? "" : "boot-a";
                String controlUuid = "wrong-control".equals(defect) ? "other-operation" : "ksm-r8";
                long confirmedRevision = "wrong-revision".equals(defect) ? 7 : 8;
                String lifecycle = "paused".equals(defect)
                        ? ",\"lifecycle\":{\"paused\":true,\"activeState\":\"paused\"}" : "";
                state.setState("{\"bootId\":\"" + boot + "\",\"lastConfirmedOperationUuid\":\""
                        + controlUuid + "\",\"lastConfirmedAppliedRevision\":" + confirmedRevision
                        + lifecycle + ",\"actual\":{\"ksm\":{\"enabled\":false,\"zeroPagesEnabled\":false,"
                        + "\"pagesToScan\":2000,\"sleepMillis\":20}}}");
                return null;
            });
            assertFalse(defect + " must not authorize a KSM actual-value repair",
                    repository.applyEffectivePolicyAfterConnect("host1"));
        }
        assertTrue(repository.claim("mn-ksm-untrusted").isEmpty());
        assertEquals("only the pre-existing successful control record remains", 1L,
                repository.taskCount(new APIQueryMemoryTaskMsg()));
    }

    @Test public void standardFinalKsmDeleteQueuesEffectiveClearAndOnlyAckRemovesAppliedFields() {
        prepareAppliedKsmForStandardClear();
        deleteStandardKsmEnabled();
        MemoryStateVO queued = host1State();
        assertEquals("Queued", queued.getPolicyPlanStatus());
        assertEquals("{}", queued.getPolicyBlockedFields());
        assertEquals(2L, queued.getDesiredRevision());
        assertEquals(Long.valueOf(1), queued.getAppliedRevision());
        assertEquals(Boolean.FALSE, MemoryPolicyRules.decode(queued.getAppliedPolicy()).ksm.enabled);
        MemoryTaskVO clear = repository.claim("mn-clear").stream()
                .filter(t -> "host1".equals(t.getHostUuid())).findFirst().get();
        assertEquals("clearOverride", clear.getAction());
        assertNotNull(clear.getPolicyPlanHash());
        assertEquals("{\"schemaVersion\":1,\"ksm\":{}}", clear.getPolicy());
        MemoryAgentResponse ack = new MemoryAgentResponse(); ack.appliedRevision = clear.getDesiredRevision();
        repository.result(clear.getUuid(), "Succeeded", "fresh Agent clear proof", ack);
        MemoryStateVO applied = host1State();
        assertEquals(Long.valueOf(2), applied.getAppliedRevision());
        assertEquals("Applied", applied.getPolicyPlanStatus());
        assertNull(MemoryPolicyRules.decode(applied.getAppliedPolicy()).ksm.enabled);
        assertNull(applied.getActiveTaskUuid());
        assertEquals(clear.getUuid(), applied.getControlOperationUuid());
        long tasks = repository.taskCount(new APIQueryMemoryTaskMsg());
        for (int i = 0; i < 3; i++) { assertFalse(repository.applyEffectivePolicyAfterConnect("host1")); }
        repository.result(clear.getUuid(), "Succeeded", "duplicate receipt", ack);
        assertEquals(tasks, repository.taskCount(new APIQueryMemoryTaskMsg()));
        assertEquals(Long.valueOf(2), host1State().getAppliedRevision());
    }

    @Test public void standardKsmClearUnknownReceiptKeepsPriorAppliedSnapshotAndFence() {
        prepareAppliedKsmForStandardClear(); deleteStandardKsmEnabled();
        MemoryTaskVO clear = repository.claim("mn-clear").stream()
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
        assertTrue(repository.claim("mn-clear").isEmpty());
        repository.transaction(em -> { em.find(TestHost.class, "host1").status = HostStatus.Connected; return null; });
        assertTrue("clearing the final field must not disable reconnect reconciliation",
                repository.applyEffectivePolicyAfterConnect("host1"));
        assertFalse(repository.applyEffectivePolicyAfterConnect("host1"));
        MemoryTaskVO clear = repository.claim("mn-clear").stream()
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
        MemoryTaskVO query = repository.claim("automatic-completion").stream().findFirst().get();
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
        assertTrue(repository.claim("automatic-completion").isEmpty());
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
        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.KSM_PAGES_TO_SCAN,
                "host1", "HostVO", "2500")), ConfigMutationContext.internal(), false);
        MemoryTaskVO query = repository.claim("automatic-completion").stream().findFirst().get();
        assertTrue(MemoryRepository.isConfigurationReconcile(query));
        assertEquals("previous-ksm-apply", query.getExpectedControlOperationUuid());
        assertEquals(1L, query.getDesiredRevision());
        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.KSM_PAGES_TO_SCAN,
                "host1", "HostVO", "3000")), ConfigMutationContext.internal(), false);
        assertEquals(query.getUuid(), host1State().getActiveTaskUuid());
        assertEquals(1L, host1State().getDesiredRevision());
        assertTrue(repository.claim("other-mn").isEmpty());
        List<String> deferred = repository.transaction(em -> em.createQuery(
                "select t.uuid from MemoryTaskVO t where t.hostUuid = :host and t.status = :status", String.class)
                .setParameter("host", "host1").setParameter("status", "Blocked").getResultList());
        assertEquals(1, deferred.size());
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
        assertEquals(Long.valueOf(3000), MemoryPolicyRules.decode(repository.getPolicy("Host", "host1")
                .getEffectivePolicy()).ksm.pagesToScan);
        assertTrue(repository.applyEffectivePolicyAfterConnect("host1"));
        assertFalse(repository.applyEffectivePolicyAfterConnect("host1"));
        MemoryTaskVO next = repository.claim("automatic-completion").stream().findFirst().get();
        assertEquals(Long.valueOf(3000), MemoryPolicyRules.decode(next.getPolicy()).ksm.pagesToScan);
        assertEquals(2L, next.getDesiredRevision());
        repository.result(query.getUuid(), "Succeeded", "late duplicate", receipt, current);
        assertEquals(next.getUuid(), host1State().getActiveTaskUuid());
        assertEquals(next.getUuid(), host1State().getControlOperationUuid());
        MemoryAgentResponse applied = new MemoryAgentResponse(); applied.appliedRevision = next.getDesiredRevision();
        repository.result(next.getUuid(), "Succeeded", "latest desired applied", applied);
        assertEquals("Succeeded", host1State().getStatus());
        assertEquals("Applied", host1State().getPolicyPlanStatus());
        assertEquals(Long.valueOf(3000), MemoryPolicyRules.decode(host1State().getAppliedPolicy()).ksm.pagesToScan);
        assertNull(host1State().getActiveTaskUuid());
        // A formerly blocked attempt remains truthful history. It must not
        // override the current States projection after the latest apply.
        assertEquals("Blocked", repository.findTask(deferred.get(0)).getStatus());
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
                "host1", "HostVO", "false")), ConfigMutationContext.internal(), false);
        List<MemoryTaskVO> tasks = repository.claim("automatic-completion");
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
        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.KSM_PAGES_TO_SCAN,
                "host1", "HostVO", "2500")), ConfigMutationContext.internal(), false);
        MemoryTaskVO query = repository.claim("mn-before-agent-upgrade").get(0);
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
        MemoryTaskVO retry = repository.claim("mn-after-agent-upgrade").get(0);
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
        assertTrue(repository.claim("mn-after-agent-upgrade").isEmpty());
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
        assertTrue(repository.claim("mn-clear").isEmpty());
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
        assertTrue(repository.claim("mn-clear").isEmpty());
    }

    @Test public void standardPartialKsmClearRetainsInheritedAndOtherMechanismFields() {
        prepareAppliedKsmForStandardClear();
        repository.transaction(em -> {
            standardConfigAdapter.writeGlobal(em, "ksm.pagesToScan", 300L);
            standardConfigAdapter.writeGlobal(em, "zram.enabled", true);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setAppliedPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":false,\"pagesToScan\":300},\"zram\":{\"enabled\":true}}");
            return null;
        });
        deleteStandardKsmEnabled();
        MemoryTaskVO clear = repository.claim("mn-clear").stream()
                .filter(t -> "host1".equals(t.getHostUuid())).findFirst().get();
        MemoryPolicyConfig target = MemoryPolicyRules.decode(clear.getPolicy());
        assertEquals("clearOverride", clear.getAction());
        assertNull(target.ksm.enabled);
        assertEquals(Long.valueOf(300), target.ksm.pagesToScan);
        assertEquals(Boolean.TRUE, target.zram.enabled);
        MemoryAgentResponse ack = new MemoryAgentResponse(); ack.appliedRevision = clear.getDesiredRevision();
        repository.result(clear.getUuid(), "Succeeded", "clear with inherited target", ack);
        MemoryPolicyConfig applied = MemoryPolicyRules.decode(host1State().getAppliedPolicy());
        assertNull(applied.ksm.enabled);
        assertEquals(Long.valueOf(300), applied.ksm.pagesToScan);
        assertEquals(Boolean.TRUE, applied.zram.enabled);
        assertFalse(repository.applyEffectivePolicyAfterConnect("host1"));
    }

    @Test public void standardKsmClearRechecksUnchangedActiveMechanismCapability() {
        prepareAppliedKsmForStandardClear();
        repository.transaction(em -> {
            standardConfigAdapter.writeGlobal(em, "zram.enabled", true);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setAppliedPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":false},\"zram\":{\"enabled\":true}}");
            state.setCapabilities("{\"supported\":true,\"ksm\":true,\"zram\":false,\"zramReasonCode\":\"ABI_INCOMPLETE\"}");
            return null;
        });
        deleteStandardKsmEnabled();
        assertEquals("Blocked", host1State().getPolicyPlanStatus());
        assertTrue(host1State().getPolicyBlockedFields().contains("ABI_INCOMPLETE"));
        assertNull(host1State().getActiveTaskUuid());
        assertTrue(repository.claim("mn-clear").isEmpty());
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

    @Test public void globalKsmTrueIntentSurvivesLowerFalseAndReconnectIsIdempotent() {
        repository.transaction(em -> {
            GlobalConfigVO globalKsm = em.createQuery(
                    "select g from GlobalConfigVO g where g.category=:category and g.name=:name", GlobalConfigVO.class)
                    .setParameter("category", MemoryStandardField.KSM_ENABLED.category())
                    .setParameter("name", MemoryStandardField.KSM_ENABLED.configName()).getSingleResult();
            globalKsm.setDefaultValue("true"); globalKsm.setValue("true");
            TestCluster cluster = new TestCluster(); cluster.uuid = "cluster2";
            cluster.hypervisorType = "KVM"; em.persist(cluster);
            TestHost host = new TestHost(); host.uuid = "host3"; host.hypervisorType = "KVM";
            host.clusterUuid = "cluster2"; host.zoneUuid = "zone1"; host.status = HostStatus.Connected; em.persist(host);
            MemoryStateVO state = new MemoryStateVO(); state.setHostUuid("host3"); state.setStatus("Succeeded");
            state.setLastSampleTime(System.currentTimeMillis());
            state.setCapabilities("{\"supported\":true,\"ksm\":true,\"ksmZeroPages\":true,\"zram\":true,\"writeback\":true}");
            state.setState("{}"); em.persist(state);
            standardConfigAdapter.writeOverride(em, "cluster1", "ClusterVO", "ksm.enabled", false);
            standardConfigAdapter.writeOverride(em, "host2", "HostVO", "ksm.enabled", true);
            return null;
        });

        assertTrue("the Global KSM schema default applies when no more-specific override exists",
                repository.applyEffectivePolicyAfterConnect("host3"));
        assertTrue("Global true remains effective below an explicit Cluster false",
                repository.applyEffectivePolicyAfterConnect("host1"));
        assertTrue("an explicit Host value must override its Cluster value",
                repository.applyEffectivePolicyAfterConnect("host2"));
        for (String hostUuid : Arrays.asList("host1", "host2", "host3")) {
            assertFalse("a repeated connect must not enqueue a duplicate policy for " + hostUuid,
                    repository.applyEffectivePolicyAfterConnect(hostUuid));
        }

        Map<String, Boolean> applied = new HashMap<>();
        for (MemoryTaskVO task : repository.claim("mn")) {
            if (task.getHostUuid() != null) {
                applied.put(task.getHostUuid(), MemoryPolicyRules.decode(task.getPolicy()).ksm.enabled);
                repository.result(task.getUuid(), "Succeeded", null, null);
            }
        }
        assertEquals(Boolean.TRUE, applied.get("host3"));
        assertEquals(Boolean.TRUE, applied.get("host1"));
        assertEquals(Boolean.TRUE, applied.get("host2"));
        for (String hostUuid : Arrays.asList("host1", "host2", "host3")) {
            assertFalse("the applied default and overrides must be idempotent for " + hostUuid,
                    repository.applyEffectivePolicyAfterConnect(hostUuid));
        }
    }

    @Test public void hostLegacyNoneCanBeReplacedAndClusterNoneStillIsolatesFalseHost() {
        registerStandardResources();
        registerPolicyReadResource("host3", "HostVO");
        registerPolicyReadResource("host4", "HostVO");
        registerPolicyReadResource("cluster2", "ClusterVO");
        repository.transaction(em -> {
            GlobalConfigVO globalKsm = em.createQuery(
                    "select g from GlobalConfigVO g where g.category=:category and g.name=:name", GlobalConfigVO.class)
                    .setParameter("category", MemoryStandardField.KSM_ENABLED.category())
                    .setParameter("name", MemoryStandardField.KSM_ENABLED.configName()).getSingleResult();
            globalKsm.setDefaultValue("true"); globalKsm.setValue("true");
            TestCluster cluster2 = new TestCluster(); cluster2.uuid = "cluster2";
            cluster2.hypervisorType = "KVM"; em.persist(cluster2);
            TestHost host = new TestHost(); host.uuid = "host3"; host.hypervisorType = "KVM";
            host.clusterUuid = "cluster2"; host.zoneUuid = "zone1"; host.status = HostStatus.Connected; em.persist(host);
            MemoryStateVO state = new MemoryStateVO(); state.setHostUuid("host3"); state.setStatus("Succeeded");
            state.setLastSampleTime(System.currentTimeMillis());
            state.setCapabilities("{\"supported\":true,\"ksm\":true,\"ksmZeroPages\":true,\"zram\":true,\"writeback\":true}");
            state.setState("{}"); em.persist(state);

            TestHost isolated = new TestHost(); isolated.uuid = "host4"; isolated.hypervisorType = "KVM";
            isolated.clusterUuid = "cluster1"; isolated.zoneUuid = "zone1"; isolated.status = HostStatus.Connected; em.persist(isolated);
            MemoryStateVO isolatedState = new MemoryStateVO(); isolatedState.setHostUuid("host4");
            isolatedState.setStatus("Succeeded"); isolatedState.setLastSampleTime(System.currentTimeMillis());
            isolatedState.setCapabilities("{\"supported\":true,\"ksm\":true,\"ksmZeroPages\":true,\"zram\":true,\"writeback\":true}");
            isolatedState.setState("{}"); em.persist(isolatedState);

            persistLegacyKsmValue(em, "cluster1", "ClusterVO", "none");
            persistLegacyKsmValue(em, "host3", "HostVO", "none");
            standardConfigAdapter.writeOverride(em, "host4", "HostVO", "ksm.enabled", false);
            standardConfigAdapter.writeOverride(em, "host2", "HostVO", "ksm.enabled", true);
            return null;
        });

        MemoryPolicyInventory hostNone = repository.getPolicy("Host", "host3");
        assertNull(MemoryStandardConfigCodec.get(hostNone.getEffectivePolicy(), "ksm.enabled"));
        assertEquals(Boolean.TRUE, MemoryStandardConfigCodec.get(repository.preview("Host", "host3",
                "{\"ksm\":{\"enabled\":false}}").getEffectivePolicy(), "ksm.enabled"));
        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.KSM_ENABLED,
                "host3", "HostVO", "false")), ConfigMutationContext.internal(), false);
        MemoryPolicyInventory afterExplicitFalse = repository.getPolicy("Host", "host3");
        assertEquals(Boolean.FALSE, repository.transaction(em -> standardConfigAdapter.readOverrideOwn(
                em, "ksm.enabled", "host3")));
        assertEquals(Boolean.TRUE, MemoryStandardConfigCodec.get(afterExplicitFalse.getEffectivePolicy(), "ksm.enabled"));

        assertEquals(Boolean.FALSE, MemoryStandardConfigCodec.get(repository.getPolicy("Host", "host4")
                .getEffectivePolicy(), "ksm.enabled"));
        assertTrue("an explicit Host boolean still overrides Cluster none",
                repository.applyEffectivePolicyAfterConnect("host2"));
        List<MemoryTaskVO> tasks = repository.claim("mn");
        Map<String, Boolean> payloads = new HashMap<>();
        for (MemoryTaskVO task : tasks) {
            payloads.put(task.getHostUuid(), MemoryPolicyRules.decode(task.getPolicy()).ksm.enabled);
        }
        assertEquals(Boolean.TRUE, payloads.get("host2"));
        assertEquals(Boolean.TRUE, payloads.get("host3"));
        assertFalse("Cluster none plus explicit Host false must remain outside the inherited true intent",
                payloads.containsKey("host4"));
    }

    @Test public void falseGlobalKsmSchemaDefaultIsAppliedToDisableKsm() {
        repository.transaction(em -> {
            GlobalConfigVO globalKsm = em.createQuery(
                    "select g from GlobalConfigVO g where g.category=:category and g.name=:name", GlobalConfigVO.class)
                    .setParameter("category", MemoryStandardField.KSM_ENABLED.category())
                    .setParameter("name", MemoryStandardField.KSM_ENABLED.configName()).getSingleResult();
            globalKsm.setDefaultValue("false"); globalKsm.setValue("false");
            return null;
        });

        assertTrue(repository.applyEffectivePolicyAfterConnect("host1"));
        MemoryTaskVO task = repository.claim("mn").stream()
                .filter(item -> "host1".equals(item.getHostUuid())).findFirst().get();
        assertEquals(Boolean.FALSE, MemoryPolicyRules.decode(task.getPolicy()).ksm.enabled);
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
                state.setCurrentStep("drain");
                standardConfigAdapter.writeGlobal(em, "writeback.enabled", !zramDisabled);
                if (zramDisabled) {
                    standardConfigAdapter.writeGlobal(em, "ksm.enabled", false);
                    standardConfigAdapter.writeGlobal(em, "ksm.pagesToScan", 2000L);
                    standardConfigAdapter.writeGlobal(em, "ksm.sleepMillis", 20L);
                    standardConfigAdapter.writeGlobal(em, "zram.enabled", false);
                    state.setAppliedPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":false,\"pagesToScan\":2000,\"sleepMillis\":20},"
                            + "\"zram\":{\"enabled\":false,\"logicalCapacityBytes\":8192,\"ramLimitBytes\":4096}}");
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

        APIUpdateMemoryPolicyMsg request = request(0);
        request.setScope("Host"); request.setResourceUuid("host1"); request.setTargetHostUuids(null);
        request.setExpectedRevision(0L);
        request.setExpectedGlobalRevision(repository.getPolicy("Global", "global").getRevision());
        request.setAction("prepareWritebackBackend"); request.setPolicy("{}");
        request.setExpectedControlOperationUuid(control);
        MemoryBackendPreparation selected = new MemoryBackendPreparation();
        selected.setCandidateId(candidateId); selected.setExpectedHostBootId(boot);
        selected.setExpectedPoolGeneration(pool); selected.setBackendCapacityBytes(8192L);
        selected.setResetConfirmed(true); request.setBackendPreparation(selected);

        long beforeInvalid = repository.taskCount(new APIQueryMemoryTaskMsg());
        if (!manualDrain) {
        String originalHostState = host1State().getState();
        request.setClientRequestUuid(UUID.randomUUID().toString());
        observation.state.put("observedAt", Instant.now().minusMillis(
                MemoryOptimizationGlobalConfig.displayTtlMillis() + 1000).toString());
        expectSelectedPreparationRejected(request, observation);
        observation.state.put("observedAt", Instant.now().toString());
        observation.state.put("candidates", Arrays.asList(candidate, candidate));
        request.setClientRequestUuid(UUID.randomUUID().toString());
        expectSelectedPreparationRejected(request, observation);
        observation.state.put("candidates", Collections.singletonList(candidate));
        selected.setBackendCapacityBytes(4096L);
        request.setClientRequestUuid(UUID.randomUUID().toString());
        expectSelectedPreparationRejected(request, observation);
        selected.setBackendCapacityBytes(8192L); selected.setResetConfirmed(false);
        request.setClientRequestUuid(UUID.randomUUID().toString());
        expectSelectedPreparationRejected(request, observation);
        selected.setResetConfirmed(true); candidate.put("active", true);
        observation.state.put("status", "UNKNOWN");
        request.setClientRequestUuid(UUID.randomUUID().toString());
        expectSelectedPreparationRejected(request, observation);
        candidate.put("active", false); observation.state.put("status", "AVAILABLE");
        observation.state.put("preparationReasons", Collections.emptyList());
        request.setClientRequestUuid(UUID.randomUUID().toString());
        if (!manualDrain) { expectSelectedPreparationRejected(request, observation); }
        observation.state.put("preparationReasons", Arrays.asList("MEMORY_SERVICE_NOT_STOPPED", "DRAIN_PROOF_UNAVAILABLE"));
        observation.state.remove("canPrepare");
        request.setClientRequestUuid(UUID.randomUUID().toString());
        expectSelectedPreparationRejected(request, observation);
        observation.state.put("canPrepare", false);
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            com.google.gson.JsonObject root = new com.google.gson.JsonParser().parse(state.getState()).getAsJsonObject();
            root.getAsJsonObject("host_zram").remove("device"); state.setState(root.toString()); return null;
        });
        request.setClientRequestUuid(UUID.randomUUID().toString());
        expectSelectedPreparationRejected(request, observation);
        repository.transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            com.google.gson.JsonObject root = new com.google.gson.JsonParser().parse(state.getState()).getAsJsonObject();
            root.getAsJsonObject("host_zram").addProperty("device", "/dev/sda"); state.setState(root.toString()); return null;
        });
        request.setClientRequestUuid(UUID.randomUUID().toString());
        expectSelectedPreparationRejected(request, observation);
        repository.transaction(em -> { em.find(MemoryStateVO.class, "host1").setState(originalHostState); return null; });
        stableIdentity.remove("rdev");
        request.setClientRequestUuid(UUID.randomUUID().toString());
        expectSelectedPreparationRejected(request, observation);
        stableIdentity.put("rdev", 64528L);
        assertEquals(beforeInvalid, repository.taskCount(new APIQueryMemoryTaskMsg()));
        }

        MemoryTaskInventory parent = repository.submit(request, observation);
        assertEquals("prepareWritebackBackend", parent.getAction());
        MemoryTaskVO drain = manualDrain ? manualDrainTask : repository.claim("selected-candidate-drain").stream()
                .filter(task -> "drain".equals(task.getAction())).findFirst().orElse(null);
        assertNotNull(drain);
        if (manualDrain) {
            assertNotEquals("selected preparation must have its own root, not reuse the manual drain root",
                    drain.getParentUuid(), parent.getUuid());
        } else {
        assertEquals(0L, repository.getPolicy("Host", "host1").getRevision());
        assertEquals(1L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t where t.parentUuid=:p and t.action='drain'", Long.class)
                .setParameter("p", parent.getUuid()).getSingleResult()));
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
        MemoryTaskVO preparation = repository.claim("selected-candidate-prep").stream()
                .filter(task -> "prepareWritebackBackend".equals(task.getAction())).findFirst().orElse(null);
        assertNotNull("successful drain must queue selected backend preparation; host="
                + host1State().getStatus() + "/" + host1State().getReason() + "/"
                + host1State().getActiveTaskUuid() + "/" + host1State().getPolicyBlockedFields(), preparation);
        assertStateCurrentStep("prepareWritebackBackend");

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
            String parentStatusBeforeResult = repository.findTask(parent.getUuid()).getStatus();
            String preparationStatusBeforeResult = repository.findTask(preparation.getUuid()).getStatus();
            long taskCountBeforeResult = repository.taskCount(new APIQueryMemoryTaskMsg());
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
            assertEquals(parentStatusBeforeResult, repository.findTask(parent.getUuid()).getStatus());
            assertEquals(preparationStatusBeforeResult, repository.findTask(preparation.getUuid()).getStatus());
            assertEquals(taskCountBeforeResult, repository.taskCount(new APIQueryMemoryTaskMsg()));
            assertEquals(0L, repository.getPolicy("Host", "host1").getRevision());
            assertNull(repository.transaction(em -> standardConfigAdapter.readOverrideOwn(
                    em, "writeback.backendResourceUuid", "host1")));
            assertNull(repository.transaction(em -> standardConfigAdapter.readOverrideOwn(
                    em, "writeback.backendCapacityBytes", "host1")));
            assertTrue(repository.transaction(em -> em.createQuery(
                    "select count(t) from MemoryTaskVO t where t.parentUuid=:p and t.action in ('apply','clearOverride')",
                    Long.class).setParameter("p", parent.getUuid()).getSingleResult() == 0L));
            return;
        }
        if (targetChangesBeforePrepareResult) {
            repository.transaction(em -> {
                standardConfigAdapter.writeGlobal(em, "writeback.enabled", false);
                return null;
            });
        }
        String appliedPolicyBeforePreparationResult = host1State().getAppliedPolicy();
        String controlBeforePreparationResult = host1State().getControlOperationUuid();
        long taskCountBeforePrepareResult = repository.taskCount(new APIQueryMemoryTaskMsg());
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
            assertEquals(taskCountBeforePrepareResult, repository.taskCount(new APIQueryMemoryTaskMsg()));
            assertEquals(0L, repository.getPolicy("Host", "host1").getRevision());
            assertEquals(new com.google.gson.JsonParser().parse(appliedPolicyBeforePreparationResult),
                    new com.google.gson.JsonParser().parse(host1State().getAppliedPolicy()));
            assertEquals(controlBeforePreparationResult, host1State().getControlOperationUuid());
            assertTrue(repository.transaction(em -> em.createQuery(
                    "select count(t) from MemoryTaskVO t where t.parentUuid=:p and t.action in ('apply','clearOverride')",
                    Long.class).setParameter("p", parent.getUuid()).getSingleResult() == 0L));
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
                    MemoryTaskVO.class).setParameter("p", parent.getUuid()).getResultList().isEmpty()));
            return;
        }
        if (zramDisabled) {
            Object preparedOwner = repository.transaction(em ->
                    standardConfigAdapter.readOverrideOwn(em, "writeback.backendResourceUuid", "host1"));
            Object preparedCapacity = repository.transaction(em ->
                    standardConfigAdapter.readOverrideOwn(em, "writeback.backendCapacityBytes", "host1"));
            List<MemoryTaskVO> rootTasks = repository.transaction(em -> em.createQuery(
                    "from MemoryTaskVO t where t.parentUuid=:p order by t.createDate", MemoryTaskVO.class)
                    .setParameter("p", parent.getUuid()).getResultList());
            StringBuilder taskDetails = new StringBuilder();
            for (MemoryTaskVO task : rootTasks) {
                taskDetails.append(task.getAction()).append('/').append(task.getStatus())
                        .append(':').append(task.getPolicy()).append(';');
            }
            MemoryStateVO postPrepareState = host1State();
            assertEquals("selected prepare result=" + repository.findTask(preparation.getUuid()).getStatus() + "/"
                    + repository.findTask(preparation.getUuid()).getReason() + ", root="
                    + repository.findTask(parent.getUuid()).getStatus() + "/" + repository.findTask(parent.getUuid()).getReason()
                    + ", host=" + host1State().getStatus() + "/" + host1State().getReason() + "/"
                    + host1State().getCurrentStep() + ", children=" + taskDetails + ", effective="
                    + repository.getPolicy("Host", "host1").getEffectivePolicy() + ", applied="
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
            assertEquals("Succeeded", repository.findTask(parent.getUuid()).getStatus());
            MemoryPolicyConfig effective = MemoryPolicyRules.decode(repository.getPolicy("Host", "host1").getEffectivePolicy());
            assertEquals(Boolean.FALSE, effective.zram.enabled);
            assertEquals(Boolean.FALSE, effective.writeback.enabled);
            assertNull(host1State().getActiveTaskUuid());
            List<MemoryTaskVO> followups = repository.transaction(em -> em.createQuery(
                    "from MemoryTaskVO t where t.parentUuid=:p and t.action in ('apply','clearOverride')",
                    MemoryTaskVO.class).setParameter("p", parent.getUuid()).getResultList());
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
            long taskCountBeforeDuplicate = repository.taskCount(new APIQueryMemoryTaskMsg());
            long revisionBeforeDuplicate = repository.getPolicy("Host", "host1").getRevision();
            repository.result(preparation.getUuid(), "Succeeded", "duplicate backend prepared", prepared, fresh);
            assertEquals(taskCountBeforeDuplicate, repository.taskCount(new APIQueryMemoryTaskMsg()));
            assertEquals(revisionBeforeDuplicate, repository.getPolicy("Host", "host1").getRevision());
            assertEquals(appliedPolicyBeforeDuplicate, host1State().getAppliedPolicy());
            assertEquals(activeTaskBeforeDuplicate, host1State().getActiveTaskUuid());
            assertEquals(ownerBeforeDuplicate, repository.transaction(em ->
                    standardConfigAdapter.readOverrideOwn(em, "writeback.backendResourceUuid", "host1")));
            assertEquals(capacityBeforeDuplicate, repository.transaction(em ->
                    standardConfigAdapter.readOverrideOwn(em, "writeback.backendCapacityBytes", "host1")));
            assertEquals(1L, (long) repository.transaction(em -> em.createQuery(
                    "select count(t) from MemoryTaskVO t where t.parentUuid=:p and t.action in ('apply','clearOverride')",
                    Long.class).setParameter("p", parent.getUuid()).getSingleResult()));
        }
        MemoryTaskVO activation = repository.transaction(em -> {
            List<MemoryTaskVO> rows = em.createQuery(
                    "from MemoryTaskVO t where t.parentUuid=:p and t.action in ('apply','clearOverride') order by t.createDate",
                    MemoryTaskVO.class).setParameter("p", parent.getUuid()).getResultList();
            return rows.isEmpty() ? null : rows.get(rows.size() - 1);
        });
        assertNotNull("trusted first-use preparation must queue activation and register its owner UUID; prep="
                + repository.findTask(preparation.getUuid()).getStatus() + "/"
                + repository.findTask(preparation.getUuid()).getReason() + ", host="
                + host1State().getStatus() + "/" + host1State().getReason()
                + ", drainPolicy=" + drain.getPolicy() + ", effective="
                + repository.getPolicy("Host", "host1").getEffectivePolicy(), activation);
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
                repository.claim("selected-candidate-activation").stream()
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
        assertTrue(repository.claim("selected-candidate-restore").stream()
                .anyMatch(task -> task.getUuid().equals(restore.getUuid())));
        activated.operationUuid = restore.getUuid(); activated.appliedRevision = restore.getDesiredRevision();
        activated.state.put("lastConfirmedOperationUuid", restore.getUuid());
        activated.state.put("lastConfirmedAppliedRevision", restore.getDesiredRevision());
        repository.result(restore.getUuid(), "Succeeded", "retained backend restored", activated, activated);
        assertEquals(parent.getUuid(), restore.getParentUuid());
        assertEquals("Succeeded", repository.findTask(parent.getUuid()).getStatus());
        assertNull(host1State().getActiveTaskUuid());
        assertEquals(manualDrain ? Boolean.TRUE : Boolean.FALSE,
                MemoryPolicyRules.decode(repository.getPolicy("Host", "host1").getEffectivePolicy()).writeback.enabled);
        if (manualDrain) {
            assertEquals("Succeeded", repository.findTask(drain.getUuid()).getStatus());
            assertEquals("Succeeded", repository.findTask(drain.getParentUuid()).getStatus());
        }
        Object retainedOwner = repository.transaction(em ->
                standardConfigAdapter.readOverrideOwn(em, "writeback.backendResourceUuid", "host1"));
        assertEquals(newResource, retainedOwner == null ? null : retainedOwner.toString());
        repository.result(restore.getUuid(), "Succeeded", "late duplicate restore", activated, activated);
        assertEquals("Succeeded", repository.findTask(parent.getUuid()).getStatus());
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
            standardConfigAdapter.writeGlobal(em, "zram.enabled", false);
            standardConfigAdapter.writeGlobal(em, "writeback.enabled", false);
            standardConfigAdapter.writeGlobal(em, "ksm.enabled", false);
            standardConfigAdapter.writeGlobal(em, "ksm.pagesToScan", 2000L);
            standardConfigAdapter.writeGlobal(em, "ksm.sleepMillis", 20L);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Succeeded"); state.setControlOperationUuid(control); state.setActiveTaskUuid(null);
            state.setAppliedPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":false,\"pagesToScan\":2000,\"sleepMillis\":20},"
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
                controlTask.setPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":false,"
                        + "\"pagesToScan\":2000,\"sleepMillis\":20}"
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
        APIUpdateMemoryPolicyMsg request = request(0); request.setScope("Host"); request.setResourceUuid("host1");
        request.setTargetHostUuids(null); request.setAction("prepareWritebackBackend"); request.setPolicy("{}");
        request.setExpectedRevision(0L); request.setExpectedGlobalRevision(repository.getPolicy("Global", "global").getRevision());
        request.setExpectedControlOperationUuid(control); request.setClientRequestUuid(UUID.randomUUID().toString());
        MemoryBackendPreparation selected = new MemoryBackendPreparation(); selected.setCandidateId(candidateId);
        selected.setExpectedHostBootId(boot); selected.setExpectedPoolGeneration("none"); selected.setBackendCapacityBytes(8192L);
        selected.setResetConfirmed(true); request.setBackendPreparation(selected);
        MemoryBackendPreparationRules.validate(request);
        if (priorControlIncludesZram) {
            expectSelectedPreparationRejected(request, observation);
            assertTrue(repository.claim("preparation-must-not-start").isEmpty());
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
                request.setClientRequestUuid(UUID.randomUUID().toString());
                expectSelectedPreparationRejected(request, observation);
                assertTrue("unproven service control must not dispatch: " + defect,
                        repository.claim("unproven-service-control").isEmpty());
            }
            repository.transaction(em -> {
                MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
                state.setState(savedState); state.setLastSampleTime(System.currentTimeMillis());
                em.find(MemoryTaskVO.class, control).setPolicy("{\"schemaVersion\":1,\"writeback\":{\"enabled\":false}}");
                return null;
            });
            request.setClientRequestUuid(UUID.randomUUID().toString());
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
                request.setClientRequestUuid(UUID.randomUUID().toString());
                expectSelectedPreparationRejected(request, observation);
                assertTrue("unproven bound writeback control must not dispatch: " + defect,
                        repository.claim("unproven-bound-writeback-control").isEmpty());
            }
            repository.transaction(em -> {
                em.find(MemoryStateVO.class, "host1").setState(savedState);
                em.find(MemoryTaskVO.class, control).setPolicy(savedPolicy);
                return null;
            });
            request.setClientRequestUuid(UUID.randomUUID().toString());
        }
        if (activeServiceNotStopped) {
            observation.state.put("preparationReasons", Collections.singletonList("UNKNOWN_PREPARATION_BLOCKER"));
            request.setClientRequestUuid(UUID.randomUUID().toString());
            expectSelectedPreparationRejected(request, observation);
            observation.state.put("preparationReasons", Arrays.asList(
                    "MEMORY_SERVICE_NOT_STOPPED", "DRAIN_PROOF_UNAVAILABLE"));
            request.setClientRequestUuid(UUID.randomUUID().toString());
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
            request.setClientRequestUuid(UUID.randomUUID().toString());
            expectSelectedPreparationRejected(request, observation);
            repository.transaction(em -> {
                MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
                com.google.gson.JsonObject freshState = new com.google.gson.JsonParser()
                        .parse(state.getState()).getAsJsonObject();
                freshState.getAsJsonObject("actual").getAsJsonObject("zram").remove("efficiencyEnabled");
                state.setState(freshState.toString());
                return null;
            });
            request.setClientRequestUuid(UUID.randomUUID().toString());
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
            request.setClientRequestUuid(UUID.randomUUID().toString());
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
            request.setClientRequestUuid(UUID.randomUUID().toString());
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
            request.setClientRequestUuid(UUID.randomUUID().toString());
            expectSelectedPreparationRejected(request, observation);
            repository.transaction(em -> {
                em.find(MemoryStateVO.class, "host1").setActiveTaskUuid(null);
                return null;
            });
        }
        MemoryTaskInventory root;
        try { root = repository.submit(request, observation); }
        catch (MemoryOperationException rejected) {
            fail("cold selected backend should be accepted for preparation: " + rejected.getCode()
                    + "/" + rejected.getMessage()); return;
        }
        assertEquals("prepareWritebackBackend", root.getAction());
        if (activeServiceNotStopped) {
            long drainChildren = repository.transaction(em -> em.createQuery(
                    "select count(t) from MemoryTaskVO t where t.parentUuid=:p and t.action='drain'",
                    Long.class).setParameter("p", root.getUuid()).getSingleResult());
            assertEquals("the cold active-service exception must not create a drain child", 0L, drainChildren);
        }
        assertEquals(Boolean.FALSE, MemoryPolicyRules.decode(host1State().getAppliedPolicy()).zram.enabled);
        MemoryTaskVO preparation = repository.claim("cold-selected-preparation").stream()
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
            standardCommit(Collections.singletonList(standardChange(MemoryStandardField.KSM_PAGES_TO_SCAN,
                    "host1", "HostVO", "2500")), ConfigMutationContext.internal(), false);
            resultTask = repository.claim("saved-configuration-query").stream().findFirst().get();
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
            assertTrue(repository.claim("other-mn").isEmpty());
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
            assertEquals(Long.valueOf(2500), MemoryPolicyRules.decode(repository.getPolicy("Host", "host1")
                    .getEffectivePolicy()).ksm.pagesToScan);
            MemoryTaskVO next = repository.claim("continue-latest-target").stream().findFirst().get();
            assertTrue(Arrays.asList("apply", "clearOverride").contains(next.getAction()));
            assertEquals(Long.valueOf(2500), MemoryPolicyRules.decode(next.getPolicy()).ksm.pagesToScan);
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
        MemoryTaskVO rootNow = repository.findTask(root.getUuid());
        assertEquals("Succeeded", rootNow.getStatus());
        assertNull(host1State().getActiveTaskUuid());
        assertEquals(Boolean.FALSE, MemoryPolicyRules.decode(
                repository.getPolicy("Host", "host1").getEffectivePolicy()).zram.enabled);
        assertEquals(Boolean.FALSE, MemoryPolicyRules.decode(
                repository.getPolicy("Host", "host1").getEffectivePolicy()).writeback.enabled);
        assertTrue("trusted PREPARED proof must bind owner/capacity without a normal apply child",
                repository.transaction(em -> em.createQuery(
                        "select count(t) from MemoryTaskVO t where t.parentUuid=:p and t.action in ('apply','clearOverride')",
                        Long.class).setParameter("p", root.getUuid()).getSingleResult() == 0L));
    }

    private void expectSelectedPreparationRejected(APIUpdateMemoryPolicyMsg request,
                                                    MemoryAgentResponse observation) {
        try {
            repository.submit(request, observation);
            fail("invalid selected backend evidence must be rejected");
        } catch (MemoryOperationException expected) { }
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
            MemoryStateVO row = repository.statePage(Collections.singletonList("host1"), 0, 1, null).items.get(0);
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
        java.util.concurrent.atomic.AtomicBoolean failAfterStaleResult = new java.util.concurrent.atomic.AtomicBoolean();
        MemoryRepository production = new MemoryRepository() {
            @Override public void result(String uuid, String status, String reason, MemoryAgentResponse response) {
                super.result(uuid, status, reason, response);
                if (failAfterStaleResult.get() && "Failed".equals(status)) {
                    throw new IllegalStateException("injected failure after woven stale result");
                }
            }
        };
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
            List<MemoryTaskVO> existingClaim = production.claim("woven-existing-worker");
            assertTrue(existingClaim.stream().anyMatch(task -> "host1".equals(task.getHostUuid())));

            // Exercise claim() and its nested result() through the real woven
            // SQLBatchWithReturn/JPA transaction path. Throw only after result()
            // has performed its normal writes: they must still roll back with
            // the claim, then commit together on retry.
            registerStandardResources();
            repository.transaction(em -> {
                standardConfigAdapter.writeGlobal(em, "ksm.enabled", true);
                standardConfigAdapter.writeOverride(em, "host2", "HostVO", "ksm.enabled", false);
                MemoryTaskVO parent = taskFixture("woven-stale-parent", null, null, "apply", "Queued");
                parent.setScope("Host"); parent.setResourceUuid("host2"); parent.setPolicy("{}");
                em.persist(parent);
                MemoryTaskVO stale = taskFixture("woven-stale-child", parent.getUuid(), "host2", "apply", "Queued");
                stale.setScope("Host"); stale.setResourceUuid("host2"); stale.setDesiredRevision(2);
                stale.setPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":false}}");
                stale.setPolicyPlanHash("woven-old-plan"); stale.setTargetSnapshotHash("woven-old-target");
                em.persist(stale);
                MemoryStateVO state = em.find(MemoryStateVO.class, "host2");
                state.setStatus("Queued"); state.setActiveTaskUuid(stale.getUuid());
                state.setControlOperationUuid(stale.getUuid()); state.setDesiredRevision(2);
                state.setPolicyPlanHash("woven-old-plan"); state.setPolicyPlanStatus("Queued");
                state.setPolicyTargetHash("woven-old-target");
                return null;
            });
            long taskVersionBefore = repository.transaction(em -> em.find(MemoryQueryVersionVO.class, "task").getRevision());
            failAfterStaleResult.set(true);
            try { production.claim("woven-stale-worker"); fail("injected post-result failure must escape claim"); }
            catch (IllegalStateException expected) {
                assertEquals("injected failure after woven stale result", expected.getMessage());
            } finally { failAfterStaleResult.set(false); }
            assertEquals("Queued", repository.findTask("woven-stale-child").getStatus());
            assertEquals("Queued", repository.findTask("woven-stale-parent").getStatus());
            assertNull(repository.transaction(em -> em.find(MemoryTaskFailureOutboxVO.class, "woven-stale-child")));
            assertEquals(Long.valueOf(taskVersionBefore), repository.transaction(em ->
                    em.find(MemoryQueryVersionVO.class, "task").getRevision()));
            MemoryStateVO rolledBack = repository.transaction(em -> em.find(MemoryStateVO.class, "host2"));
            assertEquals("Queued", rolledBack.getStatus());
            assertEquals("woven-stale-child", rolledBack.getActiveTaskUuid());

            assertTrue(production.claim("woven-stale-worker-retry").isEmpty());
            assertEquals("Failed", repository.findTask("woven-stale-child").getStatus());
            assertEquals("Failed", repository.findTask("woven-stale-parent").getStatus());
            assertNotNull(repository.transaction(em -> em.find(MemoryTaskFailureOutboxVO.class, "woven-stale-child")));
            assertNull(repository.transaction(em -> em.find(MemoryStateVO.class, "host2").getActiveTaskUuid()));
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

    @Test public void claimProjectsExactActiveTaskIntoState() {
        registerStandardResources();
        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.KSM_ENABLED,
                null, null, "true")), ConfigMutationContext.internal(), false);
        List<MemoryTaskVO> claimed = repository.claim("mn-states");
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
                "host1", "HostVO", "true")), ConfigMutationContext.internal(), false);
        String oldTask = host1State().getActiveTaskUuid();
        assertNotNull(oldTask);
        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.WRITEBACK_ENABLED,
                "host1", "HostVO", "true")), ConfigMutationContext.internal(), false);
        String newPlan = host1State().getPolicyPlanHash();
        String blockers = host1State().getPolicyBlockedFields();
        assertEquals("Blocked", host1State().getPolicyPlanStatus());
        assertTrue(repository.claim("mn-states-retry").isEmpty());
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
        MemoryTaskVO drain = repository.claim("mn-states-drain").stream()
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

    @Test public void rejectedMaintenanceActivationAllowsCorrectedStandardConfiguration() {
        registerStandardResources(); rejectedMaintenanceActivationFixture();
        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.ZRAM_ENABLED,
                "host1", "HostVO", "true")), ConfigMutationContext.internal(), false);
        MemoryStateVO state = host1State();
        assertNotNull(state.getActiveTaskUuid());
        assertNotEquals("rejected-activation", state.getControlOperationUuid());
        assertEquals("Failed", repository.findTask("rejected-activation").getStatus());
        assertEquals("Succeeded", repository.findTask("completed-drain").getStatus());
        assertEquals(1, repository.claim("mn-a").size());
        assertTrue("second MN cannot dispatch the same operation", repository.claim("mn-b").isEmpty());
    }

    @Test public void maintenanceActivationKeepsWritebackDisabledUntilFreshReadyApply() {
        registerStandardResources(); rejectedMaintenanceActivationFixture();
        repository.transaction(em -> {
            standardConfigAdapter.writeGlobal(em, "writeback.enabled", true);
            standardConfigAdapter.writeGlobal(em, "ksm.enabled", true);
            standardConfigAdapter.writeGlobal(em, "ksm.pagesToScan", 2500L);
            standardConfigAdapter.writeGlobal(em, "ksm.sleepMillis", 20L);
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "zram.enabled", true);
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "writeback.backendResourceUuid",
                    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "writeback.backendCapacityBytes", 4096L);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setAppliedPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":true,\"pagesToScan\":2500,\"sleepMillis\":20},\"zram\":{\"enabled\":true},\"writeback\":{\"enabled\":false}}");
            state.setAppliedPolicyHash("previous-policy-before-maintenance");
            state.setCapabilities("{\"supported\":true,\"ksm\":true,\"ksmZeroPages\":true,\"zram\":true,\"writeback\":true}");
            state.setLastSampleTime(System.currentTimeMillis());
            state.setState("{\"managed\":true,\"bootId\":\"boot-a\","
                    + "\"lastConfirmedOperationUuid\":\"completed-drain\","
                    + "\"lifecycle\":{\"paused\":true,\"activeState\":\"paused\"},"
                    + "\"maintenanceProof\":{\"maintenanceReady\":true,\"maintenanceArchived\":true,"
                    + "\"drainOperationUuid\":\"completed-drain\",\"activeOperations\":0,"
                    + "\"poolOwnedByService\":false,\"readyForInitialization\":true,"
                    + "\"originalDeviceInactive\":true,\"oldPoolOwnershipAbsent\":true,"
                    + "\"executorExited\":true},\"actual\":{\"writeback\":{\"backend\":{"
                    + "\"resource_uuid\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\",\"state\":\"PREPARED\","
                    + "\"fingerprint\":\"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb\","
                    + "\"capacity_bytes\":4096,\"observed_at\":\"" + Instant.now() + "\"}}}}");
            return null;
        });
        assertEquals(Boolean.TRUE, repository.transaction(em -> standardConfigAdapter.readEffective(
                em, "writeback.enabled", Collections.singletonList("host1"))));
        assertNull(repository.transaction(em -> standardConfigAdapter.readOverrideOwn(
                em, "writeback.enabled", "host1")));
        assertTrue(repository.applyEffectivePolicyAfterConnect("host1"));
        MemoryTaskVO activation = repository.claim("mn-a").stream()
                .filter(task -> "host1".equals(task.getHostUuid())).findFirst().get();
        MemoryPolicyConfig payload = MemoryPolicyRules.decode(activation.getPolicy());
        assertTrue(payload.zram != null && Boolean.TRUE.equals(payload.zram.enabled));
        assertNotNull("maintenance activation must retain the complete KSM target", payload.ksm);
        assertEquals(Boolean.TRUE, payload.ksm.enabled);
        assertEquals(Long.valueOf(2500L), payload.ksm.pagesToScan);
        assertEquals(Long.valueOf(20L), payload.ksm.sleepMillis);
        assertNotNull(payload.writeback);
        assertEquals("activation explicitly disables prior tuning before initialization",
                Boolean.FALSE, payload.writeback.enabled);
        assertEquals(Boolean.TRUE, MemoryPolicyRules.decode(repository.getPolicy("Global", "global")
                .getEffectivePolicy()).writeback.enabled);

        MemoryAgentResponse activated = new MemoryAgentResponse();
        activated.status = "Succeeded"; activated.sampleTime = System.currentTimeMillis();
        activated.capabilities = new HashMap<>(); activated.capabilities.put("supported", true);
        activated.capabilities.put("ksm", true); activated.capabilities.put("ksmZeroPages", true);
        activated.capabilities.put("zram", true); activated.capabilities.put("writeback", true);
        activated.state = new HashMap<>(); activated.state.put("bootId", "boot-a");
        Map<String, Object> lifecycle = new LinkedHashMap<>();
        lifecycle.put("paused", false); lifecycle.put("activeState", "running");
        activated.state.put("lifecycle", lifecycle);
        Map<String, Object> backend = new LinkedHashMap<>();
        backend.put("resource_uuid", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"); backend.put("state", "ACTIVE");
        backend.put("fingerprint", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
        backend.put("capacity_bytes", 4096L); backend.put("observed_at", Instant.now().toString());
        activated.state.put("actual", Collections.singletonMap("writeback", Collections.singletonMap("backend", backend)));
        repository.result(activation.getUuid(), "Succeeded", null, activated);
        assertEquals(Boolean.FALSE, MemoryPolicyRules.decode(host1State().getAppliedPolicy()).writeback.enabled);
        activated.sampleTime = System.currentTimeMillis();
        repository.observe("host1", activated);
        assertTrue("fresh active backend observation must continue the retained desired WB intent: "
                        + org.zstack.utils.gson.JSONObjectUtil.toJsonString(host1State()),
                repository.applyEffectivePolicyAfterConnect("host1"));
        MemoryTaskVO continuation = repository.claim("mn-a").stream()
                .filter(task -> "host1".equals(task.getHostUuid())).findFirst().get();
        assertTrue(MemoryPolicyRules.decode(continuation.getPolicy()).writeback.enabled);
    }

    @Test public void maintenanceActivationBlocksWhenFreshKsmCapabilityIsMissing() {
        registerStandardResources(); rejectedMaintenanceActivationFixture();
        repository.transaction(em -> {
            standardConfigAdapter.writeGlobal(em, "writeback.enabled", true);
            standardConfigAdapter.writeGlobal(em, "ksm.enabled", true);
            standardConfigAdapter.writeGlobal(em, "ksm.pagesToScan", 2500L);
            standardConfigAdapter.writeGlobal(em, "ksm.sleepMillis", 20L);
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "zram.enabled", true);
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "writeback.backendResourceUuid",
                    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "writeback.backendCapacityBytes", 4096L);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setAppliedPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":true,\"pagesToScan\":2500,\"sleepMillis\":20},\"zram\":{\"enabled\":true},\"writeback\":{\"enabled\":false}}");
            state.setAppliedPolicyHash("previous-policy-before-maintenance");
            state.setCapabilities("{\"supported\":true,\"ksm\":false,\"ksmZeroPages\":false,\"zram\":true,\"writeback\":true}");
            state.setLastSampleTime(System.currentTimeMillis());
            state.setState("{\"managed\":true,\"bootId\":\"boot-a\","
                    + "\"lastConfirmedOperationUuid\":\"completed-drain\","
                    + "\"lifecycle\":{\"paused\":true,\"activeState\":\"paused\"},"
                    + "\"maintenanceProof\":{\"maintenanceReady\":true,\"maintenanceArchived\":true,"
                    + "\"drainOperationUuid\":\"completed-drain\",\"activeOperations\":0,"
                    + "\"poolOwnedByService\":false,\"readyForInitialization\":true,"
                    + "\"originalDeviceInactive\":true,\"oldPoolOwnershipAbsent\":true,"
                    + "\"executorExited\":true},\"actual\":{\"writeback\":{\"backend\":{"
                    + "\"resource_uuid\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\",\"state\":\"PREPARED\","
                    + "\"fingerprint\":\"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb\","
                    + "\"capacity_bytes\":4096,\"observed_at\":\"" + Instant.now() + "\"}}}}");
            return null;
        });
        boolean queued = repository.applyEffectivePolicyAfterConnect("host1");
        assertFalse("missing fresh KSM capability must block retained KSM recovery; state="
                + host1State().getPolicyPlanStatus() + ", active=" + host1State().getActiveTaskUuid(), queued);
        assertEquals("Blocked", host1State().getPolicyPlanStatus());
        assertEquals(0L, (long) repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t where t.hostUuid = :host and t.status in ('Queued','Applying')",
                Long.class).setParameter("host", "host1").getSingleResult()));
    }

    @Test public void maintenanceActivationCanClearKsmOverrideWithoutEnablingWriteback() {
        registerStandardResources(); rejectedMaintenanceActivationFixture();
        repository.transaction(em -> {
            standardConfigAdapter.writeGlobal(em, "writeback.enabled", true);
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "zram.enabled", true);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setAppliedPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":true},"
                    + "\"zram\":{\"enabled\":true},\"writeback\":{\"enabled\":false}}");
            state.setAppliedPolicyHash("previous-policy-before-maintenance");
            return null;
        });
        assertTrue(repository.applyEffectivePolicyAfterConnect("host1"));
        MemoryTaskVO activation = repository.claim("mn-a").get(0);
        assertEquals("clearOverride", activation.getAction());
        MemoryPolicyConfig payload = MemoryPolicyRules.decode(activation.getPolicy());
        assertNotNull(payload.ksm); assertNull(payload.ksm.enabled);
        assertEquals(Boolean.TRUE, payload.zram.enabled);
        assertEquals(Boolean.FALSE, payload.writeback.enabled);
        assertEquals(Boolean.TRUE, MemoryPolicyRules.decode(repository.getPolicy("Host", "host1")
                .getEffectivePolicy()).writeback.enabled);
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
                "host1", "HostVO", "false")), ConfigMutationContext.internal(), false);
        assertEquals(Boolean.FALSE, repository.transaction(em -> standardConfigAdapter.readOverrideOwn(
                em, "writeback.enabled", "host1")));
        assertNull(host1State().getActiveTaskUuid());
        assertEquals("completed-drain", host1State().getControlOperationUuid());
        assertEquals("Blocked", host1State().getPolicyPlanStatus());
        assertTrue(host1State().getPolicyBlockedFields().contains("HOST_DRAIN_MAINTENANCE_REQUIRED"));
        assertFalse(repository.applyEffectivePolicyAfterConnect("host1"));
        assertTrue(repository.claim("mn-a").isEmpty());
    }

    @Test public void rejectedMaintenanceActivationAllowsCompatibilityApplyWithoutRewritingOldTasks() {
        rejectedMaintenanceActivationFixture();
        APIUpdateMemoryPolicyMsg apply = request(0);
        apply.setScope("Host"); apply.setResourceUuid("host1"); apply.setTargetHostUuids(null);
        apply.setExpectedGlobalRevision(0L);
        assertEquals("Queued", repository.submit(apply).getStatus());
        assertEquals("Failed", repository.findTask("rejected-activation").getStatus());
        assertEquals("Succeeded", repository.findTask("completed-drain").getStatus());
    }

    @Test public void rejectedMaintenanceActivationDoesNotAdmitUnprovenRecovery() {
        rejectedMaintenanceActivationFixture();
        final String valid = host1State().getState();
        for (String defect : Arrays.asList("unknown", "other-reason", "other-action", "wrong-host", "wrong-drain-host",
                "drain-failed", "no-drain", "stale", "future", "before-failure", "no-boot",
                "wrong-confirmed", "missing-proof", "executor", "inflight", "owned", "active-device")) {
            repository.transaction(em -> {
                MemoryTaskVO rejected = em.find(MemoryTaskVO.class, "rejected-activation");
                rejected.setStatus("unknown".equals(defect) ? "Unknown" : "Failed");
                rejected.setReason("other-reason".equals(defect) ? "arbitrary failure"
                        : "MAINTENANCE_ACTIVATION_WRITEBACK_MUST_REMAIN_DISABLED");
                rejected.setAction("other-action".equals(defect) ? "pause" : "apply");
                rejected.setHostUuid("wrong-host".equals(defect) ? "host2" : "host1");
                MemoryTaskVO drain = em.find(MemoryTaskVO.class, "completed-drain");
                drain.setStatus("drain-failed".equals(defect) ? "Failed" : "Succeeded");
                drain.setHostUuid("wrong-drain-host".equals(defect) ? "host2" : "host1");
                em.flush();
                MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
                long now = System.currentTimeMillis();
                state.setLastSampleTime("stale".equals(defect) ? 1L : "future".equals(defect) ? now + 60000
                        : "before-failure".equals(defect) ? rejected.getLastOpDate().getTime() - 1 : now);
                String raw = valid;
                if ("no-boot".equals(defect)) raw = raw.replace("\"boot-a\"", "\"\"");
                if ("wrong-confirmed".equals(defect)) raw = raw.replace("\"lastConfirmedOperationUuid\":\"completed-drain\"", "\"lastConfirmedOperationUuid\":\"other\"");
                if ("missing-proof".equals(defect)) raw = raw.replace("\"maintenanceReady\":true", "\"maintenanceReady\":false");
                if ("no-drain".equals(defect)) raw = raw.replace("completed-drain", "missing-drain");
                if ("executor".equals(defect)) raw = raw.replace("\"executorExited\":true", "\"executorExited\":false");
                if ("inflight".equals(defect)) raw = raw.replace("\"activeOperations\":0", "\"activeOperations\":1");
                if ("owned".equals(defect)) raw = raw.replace("\"poolOwnedByService\":false", "\"poolOwnedByService\":true");
                if ("active-device".equals(defect)) raw = raw.replace("\"originalDeviceInactive\":true", "\"originalDeviceInactive\":false");
                state.setState(raw); return null;
            });
            APIUpdateMemoryPolicyMsg apply = request(0);
            apply.setScope("Host"); apply.setResourceUuid("host1"); apply.setTargetHostUuids(null);
            apply.setExpectedGlobalRevision(0L);
            try { repository.submit(apply); fail("must remain fenced: " + defect); }
            catch (MemoryOperationException expected) { /* the exact unsafe state retains its existing fence */ }
            assertNull(defect, host1State().getActiveTaskUuid());
            assertEquals(defect, "rejected-activation", host1State().getControlOperationUuid());
            assertEquals(defect, 0L, repository.getPolicy("Host", "host1").getRevision());
        }
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
                    ordinaryReconcileFixture(action, false);
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
                    String appliedPolicy = host1State().getAppliedPolicy();
                    String appliedPolicyHash = host1State().getAppliedPolicyHash();
                    String nativeState = host1State().getState();
                    Long appliedRevision = host1State().getAppliedRevision();
                    String planHash = host1State().getPolicyPlanHash();

                    repository.result("query", "Failed", rejected.reasonCode, rejected);

                    MemoryStateVO state = host1State();
                    assertEquals(queryIdentity, "Failed", repository.findTask("original").getStatus());
                    assertEquals(queryIdentity, "Failed", state.getPolicyPlanStatus());
                    assertEquals(queryIdentity, "Failed", MemoryMetricsPresentation.inventory(
                            state, null, System.currentTimeMillis()).getStatus());
                    assertEquals(queryIdentity, appliedPolicy, state.getAppliedPolicy());
                    assertEquals(queryIdentity, appliedPolicyHash, state.getAppliedPolicyHash());
                    assertEquals(queryIdentity, nativeState, state.getState());
                    assertEquals(queryIdentity, appliedRevision, state.getAppliedRevision());
                    assertEquals(queryIdentity, planHash, state.getPolicyPlanHash());
                    assertEquals(queryIdentity, "configuration".equals(queryIdentity) ? "original" : "query",
                            state.getControlOperationUuid());
                    assertNull(queryIdentity, state.getActiveTaskUuid());
                }
            }
        }
    }


    @Test public void configurationReconcileNotIssuedCannotSettleChangedOrUnprovenPlan() {
        for (String receipt : Arrays.asList("FIRST_ZRAM_ADMISSION_NOT_ISSUED", "KSM_CLEAR_NOT_ISSUED")) {
            for (String invalid : Arrays.asList("newPlan", "newRevision", "unknownProof", "wrongReason",
                    "wrongExpectedControl", "wrongHost", "wrongTopBoot", "nonConfig", "badHash", "partialWrite",
                    "mixedTarget", "nonemptyTarget", "badSchema")) {
                if (!"KSM_CLEAR_NOT_ISSUED".equals(receipt)
                        && Arrays.asList("mixedTarget", "nonemptyTarget", "badSchema").contains(invalid)) continue;
                ordinaryReconcileFixture("apply", false);
                repository.transaction(em -> {
                    if ("KSM_CLEAR_NOT_ISSUED".equals(receipt)) {
                        em.find(MemoryTaskVO.class, "original").setPolicy(
                                "mixedTarget".equals(invalid) ? "{\"schemaVersion\":1,\"ksm\":{},\"zram\":{\"enabled\":true}}"
                                : "nonemptyTarget".equals(invalid) ? "{\"schemaVersion\":1,\"ksm\":{\"enabled\":true}}"
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
        APIUpdateMemoryPolicyMsg query = request(0);
        query.setScope("Host"); query.setResourceUuid("host1"); query.setTargetHostUuids(null);
        query.setAction("reconcile"); query.setPolicy("{}");
        MemoryTaskInventory parent = repository.submit(query);
        MemoryTaskVO child = claimChild(parent.getUuid());
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
            APIUpdateMemoryPolicyMsg query = request(0);
            query.setScope("Host"); query.setResourceUuid("host1"); query.setTargetHostUuids(null);
            query.setAction("reconcile"); query.setPolicy("{}");
            try { repository.submit(query); fail(invalid); }
            catch (MemoryOperationException expected) { assertEquals(invalid, "MEMORY_CONTROL_OPERATION_FENCED", expected.getCode()); }
            assertEquals("query", host1State().getControlOperationUuid());
            assertNull(host1State().getActiveTaskUuid());
            assertEquals(2L, (long) repository.transaction(em -> em.createQuery("select count(t) from MemoryTaskVO t", Long.class).getSingleResult()));
        }
    }

    @Test public void completedReconcileCancellationAndContradictionRetainOriginalProof() {
        MemoryAgentResponse ack = ordinaryReconcileFixture("apply", true);
        APIUpdateMemoryPolicyMsg query = request(0);
        query.setScope("Host"); query.setResourceUuid("host1"); query.setTargetHostUuids(null);
        query.setAction("reconcile"); query.setPolicy("{}");
        MemoryTaskInventory parent = repository.submit(query);
        repository.cancel(parent.getUuid());
        assertEquals("query", host1State().getControlOperationUuid());
        assertNull(host1State().getActiveTaskUuid());
        query.setClientRequestUuid(UUID.randomUUID().toString());
        MemoryTaskVO retry = claimChild(repository.submit(query).getUuid());
        ack.setSuccess(false); ack.status = "Failed";
        repository.result(retry.getUuid(), "Failed", "contradictory response", ack);
        assertEquals("Succeeded", repository.findTask("original").getStatus());
        assertEquals("Unknown", repository.findTask(retry.getUuid()).getStatus());
        assertEquals(retry.getUuid(), host1State().getActiveTaskUuid());
        assertEquals("query", host1State().getControlOperationUuid());
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
            java.lang.reflect.Method dispatch = MemoryOptimizationManager.class.getDeclaredMethod("dispatch", MemoryTaskVO.class, boolean.class);
            dispatch.setAccessible(true); dispatch.invoke(manager, repository.findTask("query"), false);
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
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "zram.enabled", true);
            if ("apply".equals(action)) standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.enabled", true);
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

    @Test public void partialPolicyRecoveryRetainsOldUnknownAndAppliesOnlyItsConfirmedPolicy() {
        APIUpdateMemoryPolicyMsg msg = uncertainRecoveryRequest();
        msg.getRecovery().setDrainControlOperationUuid(null);
        repository.transaction(em -> {
            MemoryTaskVO old = em.find(MemoryTaskVO.class, MemoryUncertainRecoveryRulesTest.OLD);
            old.setDesiredRevision(9); old.setPolicy(MemoryUncertainRecoveryRulesTest.TUNING_POLICY);
            MemoryStateVO host = em.find(MemoryStateVO.class, "host1");
            host.setDesiredRevision(9); host.setAppliedRevision(8L);
            host.setAppliedPolicy("{\"ksm\":{\"enabled\":true}}");
            return null;
        });
        MemoryTaskVO task = claimChild(repository.submit(msg).getUuid());
        repository.result(task.getUuid(), "Unknown", "response lost", null);
        APIUpdateMemoryPolicyMsg query = request(0); query.setScope("Host"); query.setResourceUuid("host1");
        query.setTargetHostUuids(null); query.setPolicy("{}"); query.setAction("reconcile");
        MemoryTaskVO reconciliation = claimChild(repository.submit(query).getUuid());
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
        APIUpdateMemoryPolicyMsg msg = uncertainRecoveryRequest();
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
        MemoryTaskVO recovery = claimChild(repository.submit(msg).getUuid());
        repository.result(recovery.getUuid(), "Unknown", "response lost", null);

        APIUpdateMemoryPolicyMsg query = request(0); query.setScope("Host"); query.setResourceUuid("host1");
        query.setTargetHostUuids(null); query.setPolicy("{}"); query.setAction("reconcile");
        MemoryTaskVO reconciliation = claimChild(repository.submit(query).getUuid());
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
        APIUpdateMemoryPolicyMsg msg = uncertainRecoveryRequest();
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

        MemoryTaskVO recovery = claimChild(repository.submit(msg).getUuid());
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
        APIUpdateMemoryPolicyMsg msg = uncertainRecoveryRequest(); msg.getRecovery().setDrainControlOperationUuid(null);
        MemoryTaskVO task = claimChild(repository.submit(msg).getUuid());
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
        repository.transaction(em -> { em.find(MemoryStateVO.class, "host1").setState("{\"bootId\":\"boot1\"}"); return null; });
        MemoryAgentResponse proof = new MemoryAgentResponse(); proof.setSuccess(true); proof.status = "Succeeded";
        proof.operationUuid = task.getUuid(); proof.hostUuid = "host1"; proof.bootId = "boot1";
        proof.appliedRevision = task.getDesiredRevision(); proof.sampleTime = System.currentTimeMillis();
        proof.state = new HashMap<>(); proof.state.put("bootId", proof.bootId);
        proof.state.put("lastConfirmedOperationUuid", task.getUuid());
        proof.state.put("lastConfirmedAppliedRevision", task.getDesiredRevision());
        repository.result(reconciliation.getUuid(), "Succeeded", null, proof);
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

    @Test public void featureIntentSwitchesAreEnabledWhenAnyGlobalClusterOrHostLevelIsTrue() {
        registerStandardResources();
        String[] paths = {"ksm.enabled", "ksm.zeroPagesEnabled", "zram.enabled", "writeback.enabled"};
        for (String path : paths) {
            for (int levels = 0; levels < 8; levels++) {
                final boolean global = (levels & 1) != 0;
                final boolean cluster = (levels & 2) != 0;
                final boolean host = (levels & 4) != 0;
                repository.transaction(em -> {
                    standardConfigAdapter.writeGlobal(em, path, global);
                    standardConfigAdapter.writeOverride(em, "cluster1", "ClusterVO", path, cluster);
                    standardConfigAdapter.writeOverride(em, "host1", "HostVO", path, host);
                    return null;
                });

                Object effective = MemoryStandardConfigCodec.get(
                        repository.getPolicy("Host", "host1").getEffectivePolicy(), path);
                assertEquals(path + " at G/C/H=" + global + "/" + cluster + "/" + host,
                        global || cluster || host, effective);
            }
        }
    }

    @Test public void featureIntentPreviewReportsNearestTrueSourceInsteadOfMoreSpecificFalse() {
        registerStandardResources();
        repository.transaction(em -> {
            standardConfigAdapter.writeGlobal(em, "zram.enabled", true);
            standardConfigAdapter.writeOverride(em, "cluster1", "ClusterVO", "zram.enabled", true);
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "zram.enabled", false);
            return null;
        });

        MemoryPolicyInventory current = repository.getPolicy("Host", "host1");
        assertEquals(Boolean.TRUE, MemoryStandardConfigCodec.get(current.getEffectivePolicy(), "zram.enabled"));
        assertEquals("Cluster:cluster1", current.getFieldSources().get("zram.enabled"));

        MemoryPolicyInventory preview = repository.preview("Host", "host1", "{\"zram\":{\"enabled\":false}}");
        assertEquals(Boolean.TRUE, MemoryStandardConfigCodec.get(preview.getEffectivePolicy(), "zram.enabled"));
        assertEquals("Cluster:cluster1", preview.getFieldSources().get("zram.enabled"));
    }

    @Test public void standardGlobalIntentCommitFansOutAndReconnectUsesTheSameInheritedSwitch() {
        registerStandardResources();
        repository.transaction(em -> {
            standardConfigAdapter.writeGlobal(em, "ksm.enabled", false);
            standardConfigAdapter.writeOverride(em, "cluster1", "ClusterVO", "ksm.enabled", false);
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.enabled", false);
            standardConfigAdapter.writeOverride(em, "host2", "HostVO", "ksm.enabled", false);
            em.find(TestHost.class, "host1").status = HostStatus.Disconnected;
            return null;
        });

        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.KSM_ENABLED,
                null, null, "true")), ConfigMutationContext.internal(), false);
        assertEquals(Boolean.TRUE, MemoryStandardConfigCodec.get(
                repository.getPolicy("Host", "host1").getEffectivePolicy(), "ksm.enabled"));
        assertEquals("Blocked", host1State().getPolicyPlanStatus());
        assertTrue(host1State().getPolicyBlockedFields().contains("HOST_NOT_CONNECTED"));
        assertNotNull("connected Host should receive a standard-config child despite lower-level false values",
                repository.states(Collections.singletonList("host2"), 0, 1).get(0).getActiveTaskUuid());

        repository.transaction(em -> { em.find(TestHost.class, "host1").status = HostStatus.Connected; return null; });
        assertTrue("the existing reconnect reconciliation should use the same effective inherited intent",
                repository.applyEffectivePolicyAfterConnect("host1"));
        List<MemoryTaskVO> claimed = repository.claim("hostcfg-intent-worker");
        Set<String> hosts = new HashSet<>();
        for (MemoryTaskVO task : claimed) {
            if (task.getHostUuid() == null) { continue; }
            hosts.add(task.getHostUuid());
            assertEquals(Boolean.TRUE, MemoryStandardConfigCodec.get(task.getPolicy(), "ksm.enabled"));
        }
        assertEquals(new HashSet<>(Arrays.asList("host1", "host2")), hosts);
    }

    @Test public void sameScopeFalseReplacesThatValueButDoesNotMaskAnAncestorTrue() {
        registerStandardResources();
        repository.transaction(em -> {
            standardConfigAdapter.writeGlobal(em, "ksm.enabled", false);
            standardConfigAdapter.writeOverride(em, "cluster1", "ClusterVO", "ksm.enabled", true);
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.enabled", true);
            return null;
        });
        MemoryPolicyInventory before = repository.getPolicy("Host", "host1");
        assertEquals(Boolean.TRUE, MemoryStandardConfigCodec.get(before.getEffectivePolicy(), "ksm.enabled"));
        assertEquals("Host:host1", before.getFieldSources().get("ksm.enabled"));

        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.KSM_ENABLED,
                "host1", "HostVO", "false")), ConfigMutationContext.internal(), false);
        MemoryPolicyInventory afterHostWrite = repository.getPolicy("Host", "host1");
        assertEquals(Boolean.TRUE, MemoryStandardConfigCodec.get(afterHostWrite.getEffectivePolicy(), "ksm.enabled"));
        assertEquals("Cluster:cluster1", afterHostWrite.getFieldSources().get("ksm.enabled"));
        assertEquals(Boolean.FALSE, repository.transaction(em -> standardConfigAdapter.readOverrideOwn(
                em, "ksm.enabled", "host1")));

        standardCommit(Collections.singletonList(standardChange(MemoryStandardField.KSM_ENABLED,
                "cluster1", "ClusterVO", "false")), ConfigMutationContext.internal(), false);
        MemoryPolicyInventory afterClusterWrite = repository.getPolicy("Host", "host1");
        assertEquals(Boolean.FALSE, MemoryStandardConfigCodec.get(afterClusterWrite.getEffectivePolicy(), "ksm.enabled"));
        assertEquals("Host:host1", afterClusterWrite.getFieldSources().get("ksm.enabled"));
    }

    @Test public void staleQueuedStandardPayloadIsNotDispatchedAfterEffectiveIntentChanges() {
        registerStandardResources();
        repository.transaction(em -> {
            // Freeze the persisted rows from before the feature-intent
            // precedence change, then seed the old-version queued child as it
            // was written. Do not ask the changed planner to recreate history.
            standardConfigAdapter.writeGlobal(em, "ksm.enabled", true);
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.enabled", false);

            MemoryTaskVO parent = taskFixture("stale-queued-parent", null, null, "apply", "Queued");
            parent.setScope("Host"); parent.setResourceUuid("host1"); parent.setPolicy("{}");
            em.persist(parent);
            MemoryTaskVO stale = taskFixture("stale-queued-child", parent.getUuid(), "host1", "apply", "Queued");
            stale.setScope("Host"); stale.setResourceUuid("host1"); stale.setDesiredRevision(2);
            stale.setPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":false}}");
            stale.setPolicyPlanHash("old-plan-hash"); stale.setTargetSnapshotHash("old-target-hash");
            em.persist(stale);

            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Queued"); state.setReason(null);
            state.setActiveTaskUuid(stale.getUuid());
            state.setControlOperationUuid(stale.getUuid()); state.setDesiredRevision(2);
            state.setAppliedPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":true}}");
            state.setAppliedRevision(1L);
            state.setPolicyPlanHash("old-plan-hash"); state.setPolicyPlanStatus("Queued");
            state.setPolicyTargetHash("old-target-hash");
            return null;
        });

        MemoryPolicyInventory beforeClaim = repository.getPolicy("Host", "host1");

        // No config row or revision changes here: only the upgraded effective
        // switch semantics make this old false payload stale.
        List<MemoryTaskVO> claimed = repository.claim("stale-payload-worker");
        assertFalse("an obsolete queued false must not be handed to the Agent after the effective intent became true",
                claimed.stream().anyMatch(task -> "host1".equals(task.getHostUuid())
                        && Boolean.FALSE.equals(MemoryStandardConfigCodec.get(task.getPolicy(), "ksm.enabled"))));
        MemoryTaskVO rejected = repository.findTask("stale-queued-child");
        assertEquals("Failed", rejected.getStatus());
        assertEquals("{\"schemaVersion\":1,\"ksm\":{\"enabled\":false}}", rejected.getPolicy());
        assertEquals("Failed", repository.findTask("stale-queued-parent").getStatus());
        MemoryTaskFailureOutboxVO outbox = repository.transaction(em ->
                em.find(MemoryTaskFailureOutboxVO.class, "stale-queued-child"));
        assertNotNull("pre-dispatch rejection must use the existing durable failure outbox", outbox);
        assertTrue(outbox.getReason().contains("effective policy changed before dispatch"));
        MemoryStateVO afterClaim = host1State();
        assertEquals("Failed", afterClaim.getStatus());
        assertNull(afterClaim.getActiveTaskUuid());
        assertEquals("stale-queued-child", afterClaim.getControlOperationUuid());
        assertEquals(Boolean.TRUE, repository.transaction(em -> standardConfigAdapter.readGlobalOwn(em, "ksm.enabled")));
        assertEquals(Boolean.FALSE, repository.transaction(em -> standardConfigAdapter.readOverrideOwn(em,
                "ksm.enabled", "host1")));
        assertEquals(beforeClaim.getSourceRevisions(), repository.getPolicy("Host", "host1").getSourceRevisions());
    }

    @Test public void staleQueuedChildFailureDoesNotClearIndependentUnknownControl() {
        registerStandardResources();
        repository.transaction(em -> {
            standardConfigAdapter.writeGlobal(em, "ksm.enabled", true);
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.enabled", false);
            MemoryTaskVO unknown = taskFixture("pre-existing-unknown-control", null, "host1", "apply", "Unknown");
            unknown.setScope("Host"); unknown.setResourceUuid("host1");
            unknown.setReason("original operation has no receipt");
            em.persist(unknown);
            MemoryTaskVO parent = taskFixture("queued-stale-parent", null, null, "apply", "Queued");
            parent.setScope("Host"); parent.setResourceUuid("host1"); parent.setPolicy("{}");
            em.persist(parent);
            MemoryTaskVO stale = taskFixture("queued-stale-child", parent.getUuid(), "host1", "apply", "Queued");
            stale.setScope("Host"); stale.setResourceUuid("host1"); stale.setDesiredRevision(2);
            stale.setPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":false}}");
            em.persist(stale);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Unknown"); state.setReason("original operation has no receipt");
            state.setActiveTaskUuid(unknown.getUuid()); state.setControlOperationUuid(unknown.getUuid());
            state.setDesiredRevision(2); state.setAppliedRevision(1L);
            state.setAppliedPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":true}}");
            return null;
        });

        assertTrue(repository.claim("unknown-fence-worker").isEmpty());
        assertEquals("Unknown", repository.findTask("pre-existing-unknown-control").getStatus());
        MemoryStateVO state = host1State();
        assertEquals("Unknown", state.getStatus());
        assertEquals("original operation has no receipt", state.getReason());
        assertEquals("pre-existing-unknown-control", state.getActiveTaskUuid());
        assertEquals("pre-existing-unknown-control", state.getControlOperationUuid());
        assertEquals("Failed", repository.findTask("queued-stale-child").getStatus());
        assertEquals("Failed", repository.findTask("queued-stale-parent").getStatus());
    }

    @Test public void staleClaimFailureRollsBackClaimAndRetryUsesDurableFailurePath() {
        registerStandardResources();
        repository.transaction(em -> {
            standardConfigAdapter.writeGlobal(em, "ksm.enabled", true);
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "ksm.enabled", false);
            MemoryTaskVO parent = taskFixture("stale-rollback-parent", null, null, "apply", "Queued");
            parent.setScope("Host"); parent.setResourceUuid("host1"); parent.setPolicy("{}");
            em.persist(parent);
            MemoryTaskVO stale = taskFixture("stale-rollback-child", parent.getUuid(), "host1", "apply", "Queued");
            stale.setScope("Host"); stale.setResourceUuid("host1"); stale.setDesiredRevision(2);
            stale.setPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":false}}");
            stale.setPolicyPlanHash("old-plan-hash"); stale.setTargetSnapshotHash("old-target-hash");
            em.persist(stale);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setStatus("Queued"); state.setActiveTaskUuid(stale.getUuid());
            state.setControlOperationUuid(stale.getUuid()); state.setDesiredRevision(2);
            state.setPolicyPlanHash("old-plan-hash"); state.setPolicyPlanStatus("Queued");
            state.setPolicyTargetHash("old-target-hash");
            return null;
        });

        failResultUuid = "stale-rollback-child";
        try { repository.claim("stale-failure-worker"); fail("injected result failure must escape the claim attempt"); }
        catch (IllegalStateException expected) { assertEquals("injected stale-result failure", expected.getMessage()); }
        finally { failResultUuid = null; }

        MemoryTaskVO childAfterRollback = repository.findTask("stale-rollback-child");
        assertEquals("Queued", childAfterRollback.getStatus());
        assertNull(childAfterRollback.getOwnerManagementNodeUuid());
        assertEquals("Queued", repository.findTask("stale-rollback-parent").getStatus());
        assertNull(repository.transaction(em -> em.find(MemoryTaskFailureOutboxVO.class, "stale-rollback-child")));
        MemoryStateVO hostAfterRollback = host1State();
        assertEquals("Queued", hostAfterRollback.getStatus());
        assertEquals("stale-rollback-child", hostAfterRollback.getActiveTaskUuid());
        assertEquals("stale-rollback-child", hostAfterRollback.getControlOperationUuid());

        assertTrue("the retried stale task is failed before it can be returned for dispatch",
                repository.claim("stale-failure-worker-retry").isEmpty());
        assertEquals("Failed", repository.findTask("stale-rollback-child").getStatus());
        assertEquals("Failed", repository.findTask("stale-rollback-parent").getStatus());
        assertNotNull(repository.transaction(em -> em.find(MemoryTaskFailureOutboxVO.class, "stale-rollback-child")));
        assertNull(host1State().getActiveTaskUuid());
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

    @Test public void globalPreviewAndAdmissionIncludeLowerFalseForFeatureIntentAndStillAllowKsmOnly() {
        repository.transaction(em -> {
            standardConfigAdapter.writeGlobal(em, "zram.enabled", false);
            standardConfigAdapter.writeOverride(em, "host1", "HostVO", "zram.enabled", false);
            MemoryStateVO state = em.find(MemoryStateVO.class, "host1");
            state.setCapabilities("{\"supported\":true,\"ksm\":true,\"zram\":false,\"zramReasonCode\":\"UNSUPPORTED_RECLAIM_KERNEL\"}");
            return null;
        });
        Map<String, MemoryHostPolicyPreviewSnapshot> preview = repository.previewHostPolicies("Global", "global",
                "{\"zram\":{\"enabled\":true},\"ksm\":{\"enabled\":true}}", "apply", null,
                Arrays.asList("host1", "host2"));
        Map<String, String> host1Changed = MemoryPolicyAdmissionRules.changedFields(
                preview.get("host1").before, preview.get("host1").after);
        assertFalse(host1Changed
                .keySet().stream().noneMatch(field -> field.startsWith("zram.")));
        assertFalse(MemoryPolicyAdmissionRules.changedFields(preview.get("host2").before, preview.get("host2").after)
                .keySet().stream().noneMatch(field -> field.startsWith("zram.")));
        Map<String, String> blocked = MemoryPolicyAdmissionRules.blockedFields(host1Changed,
                preview.get("host1").state, System.currentTimeMillis(), preview.get("host1").after);
        assertEquals("UNSUPPORTED_RECLAIM_KERNEL", blocked.get("zram.enabled"));

        long taskCountBefore = repository.taskCount(new APIQueryMemoryTaskMsg());
        long globalRevisionBefore = repository.getPolicy("Global", "global").getRevision();
        APIUpdateMemoryPolicyMsg enableUnsupportedZram = request(globalRevisionBefore);
        enableUnsupportedZram.setTargetHostUuids(Collections.singletonList("host1"));
        enableUnsupportedZram.setPolicy("{\"zram\":{\"enabled\":true},\"ksm\":{\"enabled\":true}}");
        try { repository.submit(enableUnsupportedZram); fail("cross-layer feature intent must expose unsupported ZRAM admission"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_CAPABILITY_PRECHECK", expected.getCode()); }
        assertEquals(taskCountBefore, repository.taskCount(new APIQueryMemoryTaskMsg()));
        assertEquals(globalRevisionBefore, repository.getPolicy("Global", "global").getRevision());

        repository.transaction(em -> {
            standardConfigAdapter.writeOverride(em, "cluster1", "ClusterVO", "zram.enabled", false);
            return null;
        });
        MemoryPolicyInventory host1 = repository.getPolicy("Host", "host1");
        assertEquals(Boolean.FALSE, MemoryStandardConfigCodec.get(host1.getEffectivePolicy(), "zram.enabled"));
        APIUpdateMemoryPolicyMsg ksmOnly = request(host1.getRevision());
        ksmOnly.setScope("Host"); ksmOnly.setResourceUuid("host1"); ksmOnly.setTargetHostUuids(null);
        ksmOnly.setExpectedGlobalRevision(host1.getSourceRevision());
        ksmOnly.setExpectedSourceRevisions(host1.getSourceRevisions());
        ksmOnly.setPolicy("{\"ksm\":{\"enabled\":true}}");
        assertEquals("Queued", repository.submit(ksmOnly).getStatus());
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
            standardCommit(Collections.singletonList(standardChange(MemoryStandardField.KSM_PAGES_TO_SCAN,
                    resource, resource == null ? null : scope + "VO", "2048")), ConfigMutationContext.internal(), false);
            List<MemoryTaskVO> tasks = repository.claim("new-worker");
            assertEquals(scope, "Host".equals(scope) ? 1 : 2, tasks.size());
            for (MemoryTaskVO task : tasks) {
                // claim opens a separate EntityManager: these are persisted columns, not transient callbacks.
                assertEquals(scope, task.getScope());
                assertEquals(Boolean.TRUE, task.getRequiresKsmNativePartialFence());
                assertEquals("boot-" + task.getHostUuid(), task.getKsmNativeObservationBootId());
                assertEquals(Long.valueOf(sample), task.getKsmNativeObservationSampleTime());
                assertFalse(new com.google.gson.Gson().toJson(task.toInventory()).contains("requiresKsmNativePartialFence"));
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
