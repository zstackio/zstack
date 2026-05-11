package org.zstack.compute.allocator;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.zstack.compute.host.HostGlobalConfig;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.db.Q;
import org.zstack.header.server.PhysicalServerCapacityVO;
import org.zstack.header.server.PhysicalServerCapacityVO_;
import org.zstack.header.server.PhysicalServerRoleVO;
import org.zstack.header.server.PhysicalServerRoleVO_;
import org.zstack.resourceconfig.ResourceConfigFacade;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link HostCpuOverProvisioningManagerImpl#getRatio(String)} (Phase 3 Wave 3 U12,
 * AC-CM-11).
 *
 * <p>Verifies the read-path priority order:
 * <ol>
 *   <li>in-memory {@code ratios} cache (existing, untouched);</li>
 *   <li>per-server {@link PhysicalServerCapacityVO#cpuOverprovisioningRatio} override;</li>
 *   <li>{@link ResourceConfigFacade}/{@link HostGlobalConfig} default (existing fallback).</li>
 * </ol>
 *
 * <p>The PSC column has primitive default {@code 1.0f}. Until a later U-unit writes per-server
 * ratios, every PSC row carries 1.0f and the read path falls through to ResourceConfig — that
 * "fall-through on unwritten default" path is verified by {@link #psc_ratio_unwritten_default_falls_back_to_resource_config()}.
 */
public class HostCpuOverProvisioningManagerImplTest {

    private static final String HOST_UUID    = "host-uuid-1";
    private static final String SERVER_UUID  = "server-uuid-1";
    private static final int    DEFAULT_RATIO = 10;

    private HostCpuOverProvisioningManagerImpl manager;
    private ResourceConfigFacade rcf;

    @Before
    public void setUp() throws Exception {
        manager = new HostCpuOverProvisioningManagerImpl();
        rcf = mock(ResourceConfigFacade.class);
        injectField(manager, "rcf", rcf);
        when(rcf.getResourceConfigValue(any(GlobalConfig.class), eq(HOST_UUID), eq(Integer.class)))
                .thenReturn(DEFAULT_RATIO);
    }

    /** AC-CM-11: PSC row carries a non-default per-server ratio → that value is returned. */
    @Test
    public void psc_per_server_ratio_overrides_resource_config_default() {
        try (MockedStatic<Q> qStatic = Mockito.mockStatic(Q.class)) {
            stubServerUuidLookup(qStatic, SERVER_UUID);
            stubPscRatioLookup(qStatic, 16.0f);

            int ratio = manager.getRatio(HOST_UUID);

            assertEquals(16, ratio);
        }
    }

    /** AC-CM-11 fall-through: no PhysicalServerRoleVO mapping → ResourceConfig default. */
    @Test
    public void psc_role_mapping_absent_falls_back_to_resource_config() {
        try (MockedStatic<Q> qStatic = Mockito.mockStatic(Q.class)) {
            stubServerUuidLookup(qStatic, null);

            int ratio = manager.getRatio(HOST_UUID);

            assertEquals(DEFAULT_RATIO, ratio);
        }
    }

    /** AC-CM-11 fall-through: PSC carries the unwritten default 1.0f → ResourceConfig default. */
    @Test
    public void psc_ratio_unwritten_default_falls_back_to_resource_config() {
        try (MockedStatic<Q> qStatic = Mockito.mockStatic(Q.class)) {
            stubServerUuidLookup(qStatic, SERVER_UUID);
            stubPscRatioLookup(qStatic, 1.0f);

            int ratio = manager.getRatio(HOST_UUID);

            assertEquals(DEFAULT_RATIO, ratio);
        }
    }

    /** In-memory cache wins over PSC (existing behaviour preserved). */
    @Test
    public void inmemory_ratio_takes_priority_over_psc() {
        manager.getAllRatio().put(HOST_UUID, 7);
        try (MockedStatic<Q> qStatic = Mockito.mockStatic(Q.class)) {
            // Q.New must NOT be consulted — the cache short-circuits the read.
            int ratio = manager.getRatio(HOST_UUID);
            assertEquals(7, ratio);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Stubs {@code Q.New(PhysicalServerRoleVO.class).eq(...).eq(...).select(...).findValue()} to
     * return {@code serverUuidToReturn} (may be null).
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void stubServerUuidLookup(MockedStatic<Q> qStatic, String serverUuidToReturn) {
        Q roleQ = mock(Q.class);
        qStatic.when(() -> Q.New(PhysicalServerRoleVO.class)).thenReturn(roleQ);
        when(roleQ.eq(any(), any())).thenReturn(roleQ);
        when(roleQ.select(eq(PhysicalServerRoleVO_.serverUuid))).thenReturn(roleQ);
        when(roleQ.findValue()).thenReturn(serverUuidToReturn);
    }

    /**
     * Stubs {@code Q.New(PhysicalServerCapacityVO.class).eq(...).select(...).findValue()} to
     * return {@code ratioToReturn} (may be null).
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void stubPscRatioLookup(MockedStatic<Q> qStatic, Float ratioToReturn) {
        Q pscQ = mock(Q.class);
        qStatic.when(() -> Q.New(PhysicalServerCapacityVO.class)).thenReturn(pscQ);
        when(pscQ.eq(any(), any())).thenReturn(pscQ);
        when(pscQ.select(eq(PhysicalServerCapacityVO_.cpuOverprovisioningRatio))).thenReturn(pscQ);
        when(pscQ.findValue()).thenReturn(ratioToReturn);
    }

    private static void injectField(Object target, String name, Object value) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field f = clazz.getDeclaredField(name);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException ignore) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
