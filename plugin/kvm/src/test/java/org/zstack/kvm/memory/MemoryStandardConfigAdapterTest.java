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
