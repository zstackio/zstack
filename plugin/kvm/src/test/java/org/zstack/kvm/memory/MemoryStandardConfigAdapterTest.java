package org.zstack.kvm.memory;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfigVO;
import org.zstack.resourceconfig.ResourceConfigVO;

import javax.persistence.EntityManager;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.Assert.*;

public class MemoryStandardConfigAdapterTest {
    private SessionFactory factory;
    private MemoryStandardConfigAdapter adapter;
    private Object aspect;
    private Field registryField;
    private Object oldRegistry;

    @Before public void setup() throws Exception {
        Class<?> aspectClass = Class.forName("org.zstack.core.aspect.EncryptColumnAspect");
        aspect = aspectClass.getMethod("aspectOf").invoke(null);
        registryField = aspectClass.getDeclaredField("pluginRegistry"); registryField.setAccessible(true);
        oldRegistry = registryField.get(aspect);
        registryField.set(aspect, Proxy.newProxyInstance(PluginRegistry.class.getClassLoader(),
                new Class<?>[]{PluginRegistry.class}, (proxy, method, args) -> Collections.emptyList()));
        java.lang.reflect.Method metadataInit = org.zstack.core.db.EntityMetadata.class.getDeclaredMethod("staticInit");
        metadataInit.setAccessible(true); metadataInit.invoke(null);
        factory = new Configuration().addAnnotatedClass(GlobalConfigVO.class).addAnnotatedClass(ResourceConfigVO.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:standard" + UUID.randomUUID() + ";MODE=MySQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
                .setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.show_sql", "false").buildSessionFactory();
        adapter = new MemoryStandardConfigAdapter();
        seedGlobal("ksm.enabled", "none", "none");
        seedGlobal("zram.enabled", "true", "false");
        seedGlobal("zram.hostFloorBytes", "123", "");
        seedGlobal("writeback.enabled", "false", "false");
        seedGlobal("writeback.backendResourceUuid", "", "");
        seedGlobal("zram.algorithm", "", "");
    }

    @After public void cleanup() throws Exception {
        if (factory != null) { factory.close(); }
        if (registryField != null) { registryField.set(aspect, oldRegistry); }
    }

    @Test public void scopedBatchMatchesScalarReadsAndSeesSameTransactionChanges() {
        inTransaction(em -> {
            adapter.writeOverride(em, "cluster1", "ClusterVO", "ksm.pagesToScan", 2500L);
            adapter.writeOverride(em, "host1", "HostVO", "zram.enabled", false);
            for (MemoryStandardField.Scope scope : MemoryStandardField.Scope.values()) {
                String resource = scope == MemoryStandardField.Scope.CLUSTER ? "cluster1" : "host1";
                Map<MemoryStandardField, Object> values = adapter.readOwn(em, scope, resource, Collections.emptySet());
                for (MemoryStandardField field : MemoryStandardField.values()) {
                    if (!field.allows(scope)) { assertFalse(values.containsKey(field)); continue; }
                    Object scalar = scope == MemoryStandardField.Scope.GLOBAL
                            ? adapter.readGlobalOwn(em, field.path()) : adapter.readOverrideOwn(em, field.path(), resource);
                    assertEquals(scope + ":" + field.path(), scalar, values.get(field));
                    assertEquals(scope == MemoryStandardField.Scope.GLOBAL
                                    || adapter.hasOverrideRow(em, field.path(), resource), values.containsKey(field));
                }
            }
            adapter.writeOverride(em, "host1", "HostVO", "zram.enabled", true);
            assertEquals(Boolean.TRUE, adapter.readOwn(em, MemoryStandardField.Scope.HOST, "host1",
                    Collections.emptySet()).get(MemoryStandardField.ZRAM_ENABLED));
            adapter.writeOverride(em, "host1", "HostVO", "zram.enabled", null);
            assertFalse(adapter.readOwn(em, MemoryStandardField.Scope.HOST, "host1",
                    Collections.emptySet()).containsKey(MemoryStandardField.ZRAM_ENABLED));
            return null;
        });
    }

    @Test public void scopedBatchRejectsInvalidStoredOverridesButAllowsExplicitReplacement() {
        inTransaction(em -> {
            adapter.writeOverride(em, "host1", "HostVO", "ksm.enabled", false);
            ResourceConfigVO row = resourceRow(em, "host1", MemoryStandardField.KSM_ENABLED.configName());
            row.setValue("none");
            assertEquals(MemoryStandardConfigAdapter.OverrideMode.UNMANAGED,
                    adapter.readOwn(em, MemoryStandardField.Scope.HOST, "host1", Collections.emptySet())
                            .get(MemoryStandardField.KSM_ENABLED));
            row.setValue(" ");
            assertThrows(IllegalArgumentException.class,
                    () -> adapter.readOwn(em, MemoryStandardField.Scope.HOST, "host1", Collections.emptySet()));
            assertFalse(adapter.readOwn(em, MemoryStandardField.Scope.HOST, "host1",
                    Collections.singleton("ksm.enabled")).containsKey(MemoryStandardField.KSM_ENABLED));
            adapter.writeOverride(em, "host1", "HostVO", "ksm.enabled", null);
            assertFalse(adapter.readOwn(em, MemoryStandardField.Scope.HOST, "host1", Collections.emptySet())
                    .containsKey(MemoryStandardField.KSM_ENABLED));
            return null;
        });
    }

    @Test public void scopedBatchRejectsDuplicateOverridesLikeScalarReads() {
        inTransaction(em -> {
            MemoryStandardField field = MemoryStandardField.ZRAM_ENABLED;
            for (String value : Arrays.asList("true", "false")) {
                ResourceConfigVO row = new ResourceConfigVO();
                row.setUuid(UUID.randomUUID().toString().replace("-", ""));
                row.setCategory(field.category()); row.setName(field.configName()); row.setValue(value);
                row.setResourceUuid("host1"); row.setResourceType("HostVO"); em.persist(row);
            }
            em.flush();
            assertThrows(javax.persistence.NonUniqueResultException.class,
                    () -> adapter.readOverrideOwn(em, field.path(), "host1"));
            assertThrows(javax.persistence.NonUniqueResultException.class,
                    () -> adapter.readOwn(em, MemoryStandardField.Scope.HOST, "host1", Collections.emptySet()));
            return null;
        });
    }

    @Test public void globalClusterHostFeatureIntentAndClearFallsBack() {
        inTransaction(em -> {
            adapter.writeOverride(em, "cluster1", "ClusterVO", "ksm.enabled", true);
            adapter.writeOverride(em, "host1", "HostVO", "ksm.enabled", false);
            assertEquals(Boolean.FALSE, adapter.readEffective(em, "ksm.enabled", Arrays.asList("host1", "cluster1")));
            adapter.writeOverride(em, "host1", "HostVO", "ksm.enabled", null);
            assertEquals(Boolean.TRUE, adapter.readEffective(em, "ksm.enabled", Arrays.asList("host1", "cluster1")));
            adapter.writeOverride(em, "cluster1", "ClusterVO", "ksm.enabled", null);
            assertEquals(MemoryStandardConfigAdapter.OverrideMode.UNMANAGED,
                    adapter.readEffective(em, "ksm.enabled", Arrays.asList("host1", "cluster1")));
            assertEquals(1, adapter.writeGlobal(em, "ksm.enabled", false).refreshAfterCommit().size());
            assertEquals(Boolean.FALSE, adapter.readEffective(em, "ksm.enabled", Arrays.asList("host1", "cluster1")));
            adapter.writeGlobal(em, "ksm.enabled", null);
            assertEquals(MemoryStandardConfigAdapter.OverrideMode.UNMANAGED,
                    adapter.readEffective(em, "ksm.enabled", Arrays.asList("host1", "cluster1")));
            return null;
        });
    }

    @Test public void featureSwitchesUseNearestBooleanAndNoneInheritsAcrossAllLevels() {
        seedGlobal("ksm.zeroPagesEnabled", "false", "false");
        String[] paths = {"ksm.enabled", "ksm.zeroPagesEnabled", "zram.enabled", "writeback.enabled"};
        String[] values = {"true", "false", "none"};
        inTransaction(em -> {
            for (String path : paths) {
                MemoryStandardField field = MemoryStandardField.forPath(path);
                adapter.writeOverride(em, "cluster1", "ClusterVO", path, false);
                adapter.writeOverride(em, "host1", "HostVO", path, false);
                GlobalConfigVO global = em.createQuery("select g from GlobalConfigVO g where g.category=:category and g.name=:name", GlobalConfigVO.class)
                        .setParameter("category", field.category()).setParameter("name", field.configName()).getSingleResult();
                for (String globalValue : values) {
                    for (String clusterValue : values) {
                        for (String hostValue : values) {
                            global.setValue(globalValue);
                            resourceRow(em, "cluster1", field.configName()).setValue(clusterValue);
                            resourceRow(em, "host1", field.configName()).setValue(hostValue);
                            Object expected = !"none".equals(hostValue) ? Boolean.valueOf(hostValue)
                                    : !"none".equals(clusterValue) ? Boolean.valueOf(clusterValue)
                                    : !"none".equals(globalValue) ? Boolean.valueOf(globalValue)
                                    : MemoryStandardConfigAdapter.OverrideMode.UNMANAGED;
                            assertEquals(path + " at G/C/H=" + globalValue + "/" + clusterValue + "/" + hostValue,
                                    expected, adapter.readEffective(em, path, Arrays.asList("host1", "cluster1")));
                            assertEquals(hostValue, adapter.overrideStoredValue(em, path, "host1"));
                        }
                    }
                }
            }
            return null;
        });
    }

    @Test public void noneInheritsAndDeleteRestoresParentWhileCodecPreservesJsonTypes() {
        inTransaction(em -> {
            adapter.writeOverride(em, "cluster1", "ClusterVO", "ksm.enabled", true);
            MemoryStandardField field = MemoryStandardField.forPath("ksm.enabled");
            ResourceConfigVO unset = new ResourceConfigVO(); unset.setUuid(UUID.randomUUID().toString().replace("-", ""));
            unset.setCategory(field.category()); unset.setName(field.configName()); unset.setValue("none");
            unset.setResourceUuid("host1"); unset.setResourceType("HostVO"); em.persist(unset);
            assertEquals(Boolean.TRUE,
                    adapter.readEffective(em, "ksm.enabled", Arrays.asList("host1", "cluster1")));
            adapter.writeOverride(em, "host1", "HostVO", "ksm.enabled", null);
            assertEquals(Boolean.TRUE, adapter.readEffective(em, "ksm.enabled", Arrays.asList("host1", "cluster1")));
            ResourceConfigVO cluster = resourceRow(em, "cluster1", field.configName());
            assertNotNull(cluster); cluster.setValue("none");
            assertEquals(MemoryStandardConfigAdapter.OverrideMode.UNMANAGED,
                    adapter.readEffective(em, "ksm.enabled", Arrays.asList("host1", "cluster1")));
            adapter.writeOverride(em, "host1", "HostVO", "ksm.enabled", true);
            assertEquals(Boolean.TRUE, adapter.readEffective(em, "ksm.enabled", Arrays.asList("host1", "cluster1")));
            String policy = MemoryStandardConfigCodec.put("{\"ksm\":{\"enabled\":true}}", "ksm.enabled", false);
            assertEquals(Boolean.FALSE, MemoryStandardConfigCodec.get(policy, "ksm.enabled"));
            policy = MemoryStandardConfigCodec.put(policy, "zram.hostFloorBytes", 0L);
            assertEquals(Long.valueOf(0), MemoryStandardConfigCodec.get(policy, "zram.hostFloorBytes"));
            return null;
        });
    }

    @Test public void typedBlankWritesRejectButFeatureNoneIsAccepted() {
        MemoryStandardField field = MemoryStandardField.forPath("ksm.enabled");
        assertThrows(org.zstack.core.config.GlobalConfigException.class,
                () -> MemoryStandardConfigurationCoordinator.validateScalar(field, " \t "));
        assertNull(MemoryStandardConfigCodec.decode("ksm.enabled", "   "));
        for (String path : Arrays.asList("ksm.enabled", "ksm.zeroPagesEnabled", "zram.enabled", "writeback.enabled")) {
            try { MemoryStandardConfigurationCoordinator.validateScalar(MemoryStandardField.forPath(path), "none"); }
            catch (RuntimeException invalid) { fail(path + " must accept the existing none encoding: " + invalid.getMessage()); }
            assertNull(MemoryStandardConfigCodec.decode(path, "none"));
        }
        assertThrows(IllegalArgumentException.class,
                () -> MemoryStandardConfigCodec.decode("zram.hostCpuGuardEnabled", "none"));
        assertEquals(MemoryStandardConfigAdapter.OverrideMode.UNMANAGED,
                readUnmanagedStoredOverride());
    }

    @Test public void emptyStoredResourceOverrideFailsClosedAndDeleteRepairsIt() {
        inTransaction(em -> {
            adapter.writeGlobal(em, "ksm.enabled", true);
            MemoryStandardField field = MemoryStandardField.forPath("ksm.enabled");
            ResourceConfigVO invalid = new ResourceConfigVO();
            invalid.setUuid(UUID.randomUUID().toString().replace("-", "")); invalid.setCategory(field.category());
            invalid.setName(field.configName()); invalid.setValue("  "); invalid.setResourceUuid("host1");
            invalid.setResourceType("HostVO"); em.persist(invalid); em.flush();
            assertThrows(IllegalArgumentException.class,
                    () -> adapter.readEffective(em, "ksm.enabled", Collections.singletonList("host1")));
            adapter.writeOverride(em, "host1", "HostVO", "ksm.enabled", null);
            assertEquals(Boolean.TRUE, adapter.readEffective(em, "ksm.enabled", Collections.singletonList("host1")));
            return null;
        });
    }

    private Object readUnmanagedStoredOverride() {
        return inTransaction(em -> {
            ResourceConfigVO row = new ResourceConfigVO();
            MemoryStandardField field = MemoryStandardField.forPath("ksm.enabled");
            row.setUuid(UUID.randomUUID().toString().replace("-", "")); row.setCategory(field.category());
            row.setName(field.configName()); row.setValue("none"); row.setResourceUuid("host1");
            row.setResourceType("HostVO"); em.persist(row); em.flush();
            return adapter.readOverrideOwn(em, "ksm.enabled", "host1");
        });
    }

    @Test public void falseAndZeroRemainExplicitAndTyped() {
        inTransaction(em -> {
            MemoryStandardConfigAdapter.ChangeSet changes = adapter.writeOverrides(em, "host1", "HostVO", new LinkedHashMap<String, Object>() {{
                put("zram.enabled", false); put("zram.hostFloorBytes", 0L);
            }});
            em.flush();
            assertTrue(changes.refreshAfterCommit().isEmpty());
            assertEquals(2, changes.resourceChanges().size());
            assertEquals(Boolean.FALSE, adapter.readOverrideOwn(em, "zram.enabled", "host1"));
            assertEquals(Boolean.FALSE, adapter.readEffective(em, "zram.enabled", Collections.singletonList("host1")));
            assertEquals(Long.valueOf(0), adapter.readEffective(em, "zram.hostFloorBytes", Collections.singletonList("host1")));
            ResourceConfigVO zero = resourceRow(em, "host1", "memory.zram.hostFloorBytes");
            assertNotNull(zero); assertEquals("0", zero.getValue());
            adapter.writeGlobal(em, "zram.enabled", false);
            assertEquals(Boolean.FALSE, adapter.readEffective(em, "zram.enabled", Collections.singletonList("host1")));
            return null;
        });
    }

    @Test public void hostNoneInheritsAndExplicitFalseSurvivesClusterNone() {
        inTransaction(em -> {
            adapter.writeGlobal(em, "ksm.enabled", true);
            adapter.writeOverride(em, "cluster1", "ClusterVO", "ksm.enabled", true);
            MemoryStandardField field = MemoryStandardField.forPath("ksm.enabled");
            ResourceConfigVO hostNone = new ResourceConfigVO();
            hostNone.setUuid(UUID.randomUUID().toString().replace("-", "")); hostNone.setCategory(field.category());
            hostNone.setName(field.configName()); hostNone.setValue("none");
            hostNone.setResourceUuid("host1"); hostNone.setResourceType("HostVO"); em.persist(hostNone);
            assertEquals(Boolean.TRUE,
                    adapter.readEffective(em, "ksm.enabled", Arrays.asList("host1", "cluster1")));

            adapter.writeOverride(em, "host1", "HostVO", "ksm.enabled", false);
            assertEquals(Boolean.FALSE, adapter.readOverrideOwn(em, "ksm.enabled", "host1"));
            assertEquals(Boolean.FALSE, adapter.readEffective(em, "ksm.enabled", Arrays.asList("host1", "cluster1")));

            ResourceConfigVO cluster = resourceRow(em, "cluster1", field.configName());
            assertNotNull(cluster); cluster.setValue("none");
            assertEquals(Boolean.FALSE, adapter.readEffective(em, "ksm.enabled", Arrays.asList("host1", "cluster1")));
            return null;
        });
    }

    @Test public void ordinaryTuningAndSafetyGuardsKeepMostSpecificExplicitValue() {
        seedGlobal("ksm.pagesToScan", "4000", "2000");
        seedGlobal("zram.hostCpuGuardEnabled", "true", "true");
        seedGlobal("zram.hostMemoryPsiGuardEnabled", "true", "true");
        seedGlobal("zram.hostIoPsiGuardEnabled", "true", "true");
        inTransaction(em -> {
            adapter.writeGlobal(em, "ksm.pagesToScan", 4000L);
            adapter.writeOverride(em, "cluster1", "ClusterVO", "ksm.pagesToScan", 3000L);
            adapter.writeOverride(em, "host1", "HostVO", "ksm.pagesToScan", 2048L);
            adapter.writeGlobal(em, "zram.hostFloorBytes", 1024L);
            adapter.writeOverride(em, "cluster1", "ClusterVO", "zram.hostFloorBytes", 512L);
            adapter.writeOverride(em, "host1", "HostVO", "zram.hostFloorBytes", 0L);
            adapter.writeGlobal(em, "zram.hostCpuGuardEnabled", true);
            adapter.writeOverride(em, "cluster1", "ClusterVO", "zram.hostCpuGuardEnabled", true);
            adapter.writeOverride(em, "host1", "HostVO", "zram.hostCpuGuardEnabled", false);
            adapter.writeGlobal(em, "zram.hostMemoryPsiGuardEnabled", false);
            adapter.writeOverride(em, "cluster1", "ClusterVO", "zram.hostMemoryPsiGuardEnabled", true);
            adapter.writeOverride(em, "host1", "HostVO", "zram.hostMemoryPsiGuardEnabled", false);
            adapter.writeGlobal(em, "zram.hostIoPsiGuardEnabled", true);
            adapter.writeOverride(em, "cluster1", "ClusterVO", "zram.hostIoPsiGuardEnabled", false);
            return null;
        });
        inTransaction(em -> {
            List<String> levels = Arrays.asList("host1", "cluster1");
            assertEquals(Long.valueOf(2048), adapter.readEffective(em, "ksm.pagesToScan", levels));
            assertEquals(Long.valueOf(0), adapter.readEffective(em, "zram.hostFloorBytes", levels));
            assertEquals(Boolean.FALSE, adapter.readEffective(em, "zram.hostCpuGuardEnabled", levels));
            assertEquals(Boolean.FALSE, adapter.readEffective(em, "zram.hostMemoryPsiGuardEnabled", levels));
            assertEquals(Boolean.FALSE, adapter.readEffective(em, "zram.hostIoPsiGuardEnabled", levels));
            return null;
        });
    }

    @Test public void writesParticipateInCallerTransactionAndRollback() {
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            adapter.writeOverrides(em, "host1", "HostVO", new LinkedHashMap<String, Object>() {{
                put("zram.enabled", true); put("zram.hostFloorBytes", 17L);
            }});
            assertEquals(Boolean.TRUE, adapter.readEffective(em, "zram.enabled", Collections.singletonList("host1")));
            em.getTransaction().rollback();
        } finally { em.close(); }
        inTransaction(check -> {
            assertEquals(Boolean.TRUE, adapter.readEffective(check, "zram.enabled", Collections.singletonList("host1")));
            assertNull(resourceRow(check, "host1", "memory.zram.hostFloorBytes"));
            return null;
        });
    }

    @Test public void strictMappingAndBackendUuidHostOnlyBoundary() {
        assertThrows(IllegalArgumentException.class, () -> MemoryStandardConfigCodec.encode("zram.selectedVmUuids", Collections.singletonList("vm1")));
        EntityManager unsupportedScope = factory.createEntityManager();
        try {
            assertThrows(IllegalArgumentException.class, () -> adapter.writeOverride(unsupportedScope, "cluster1", "ClusterVO", "writeback.backendResourceUuid", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));
            assertThrows(IllegalArgumentException.class, () -> adapter.writeOverride(unsupportedScope, "host1", "HostVO", "zram.enabled", 1));
        } finally { unsupportedScope.close(); }
        inTransaction(em -> {
            adapter.writeOverride(em, "host1", "HostVO", "writeback.backendResourceUuid", "0123456789abcdef0123456789abcdef");
            assertEquals("0123456789abcdef0123456789abcdef",
                    adapter.readEffective(em, "writeback.backendResourceUuid", Collections.singletonList("host1")));
            assertThrows(IllegalArgumentException.class, () -> adapter.writeGlobal(em, "writeback.backendResourceUuid", "0123456789abcdef0123456789abcdef"));
            return null;
        });
    }

    @Test public void policySplitKeepsSpecializedSelectionAndRejectsInvalidScope() {
        MemoryStandardPolicy.Parts parts = MemoryStandardPolicy.split(
                "{\"zram\":{\"logicalCapacityBytes\":16777216,\"ramLimitBytes\":8388608,"
                        + "\"selectionMode\":\"list\",\"selectedVmUuids\":[\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"]}}", "Host");
        assertEquals(2, parts.standardValues.size());
        assertTrue(parts.specialized.contains("selectionMode"));
        assertFalse(parts.specialized.contains("logicalCapacityBytes"));
        assertThrows(IllegalArgumentException.class, () -> MemoryStandardPolicy.split(
                "{\"writeback\":{\"backendResourceUuid\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}}", "Cluster"));
    }

    private void seedGlobal(String path, String value, String defaultValue) {
        MemoryStandardField field = MemoryStandardField.forPath(path);
        inTransaction(em -> {
            GlobalConfigVO row = new GlobalConfigVO(); row.setCategory(field.category()); row.setName(field.configName());
            row.setDescription(field.config().getDescription()); row.setDefaultValue(defaultValue); row.setValue(value);
            em.persist(row); return null;
        });
    }

    private ResourceConfigVO resourceRow(EntityManager em, String uuid, String name) {
        List<ResourceConfigVO> rows = em.createQuery("select r from ResourceConfigVO r where r.resourceUuid=:uuid and r.name=:name", ResourceConfigVO.class)
                .setParameter("uuid", uuid).setParameter("name", name).getResultList();
        return rows.isEmpty() ? null : rows.get(0);
    }

    private <T> T inTransaction(java.util.function.Function<EntityManager, T> action) {
        EntityManager em = factory.createEntityManager();
        try { em.getTransaction().begin(); T value = action.apply(em); em.getTransaction().commit(); return value; }
        catch (RuntimeException e) { if (em.getTransaction().isActive()) { em.getTransaction().rollback(); } throw e; }
        finally { em.close(); }
    }
}
