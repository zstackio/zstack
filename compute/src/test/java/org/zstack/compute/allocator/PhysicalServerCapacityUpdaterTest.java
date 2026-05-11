package org.zstack.compute.allocator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.zstack.core.aspect.EncryptColumnAspect;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.EntityMetadata;
import org.zstack.core.db.Q;
import org.zstack.header.allocator.ReservedHostCapacity;
import org.zstack.header.allocator.ServerReservedCapacityExtensionPoint;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.server.CapacityUsage;
import org.zstack.header.server.CreateRoleEntityContext;
import org.zstack.header.server.PhysicalServerCapacityState;
import org.zstack.header.server.PhysicalServerCapacityVO;
import org.zstack.header.server.PhysicalServerRoleProvider;
import org.zstack.header.server.PhysicalServerRoleVO;
import org.zstack.header.server.PhysicalServerVO;
import org.zstack.header.server.RoleWorkloadStatus;
import org.zstack.header.server.SchedulingMode;
import org.zstack.header.server.ServerRoleType;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PhysicalServerCapacityUpdater} (Phase 3 Wave 1 U4).
 *
 * <p>Per Wave 1 plan §Q3 the test uses mock {@link PhysicalServerRoleProvider} instances rather
 * than depending on real KVM / BM2 / Container providers. Container's
 * {@code getCapacityConsumption} still returns 0 today (Wave 2 U8 fix), so depending on it would
 * couple this test to a downstream change.
 *
 * <p>Mocking strategy:
 * <ul>
 *   <li>{@link DatabaseFacade} → mock; its {@code getEntityManager()} returns a mock
 *       {@link EntityManager} on which {@code find(PhysicalServerVO.class, …)} and
 *       {@code find(PhysicalServerCapacityVO.class, …, PESSIMISTIC_WRITE)} are stubbed.</li>
 *   <li>{@link Q} static → {@link MockedStatic} so {@code Q.New(PhysicalServerRoleVO.class)}
 *       returns a list of fake roles per scenario.</li>
 *   <li>{@link PluginRegistry#getExtensionList(Class)} → returns the scenario's mock providers.</li>
 * </ul>
 */
public class PhysicalServerCapacityUpdaterTest {

    private static final String SERVER_UUID  = "server-uuid-1";
    private static final long   TOTAL_CPU    = 32L;
    private static final long   TOTAL_MEMORY = 64L * 1024L * 1024L * 1024L; // 64 GB

    private PhysicalServerCapacityUpdater updater;
    private DatabaseFacade dbf;
    private EntityManager em;
    private PluginRegistry pluginRgty;
    private MockedStatic<EntityMetadata> metadataMock;

    @Before
    public void setUp() throws Exception {
        updater    = new PhysicalServerCapacityUpdater();
        dbf        = mock(DatabaseFacade.class);
        em         = mock(EntityManager.class);
        pluginRgty = mock(PluginRegistry.class);

        when(dbf.getEntityManager()).thenReturn(em);
        // Default: ANY getExtensionList query returns emptyList. Required because the
        // AspectJ-woven em.merge() (EncryptColumnAspect after-advice) queries pluginRegistry
        // for IntegrityVerificationResourceFactory + EncryptAfterSaveDbRecordExtensionPoint.
        // Specific stubs in individual tests override this default.
        when(pluginRgty.<Object>getExtensionList(Mockito.<Class<Object>>any()))
                .thenReturn(Collections.emptyList());

        injectField(updater, "dbf", dbf);
        injectField(updater, "pluginRgty", pluginRgty);

        // Prime the GlobalConfig static fields so value(Integer.class) returns the
        // default values (5% / 10%) rather than null (which would NPE on auto-unbox).
        // setValue() is package-private; use the same injectField reflective helper
        // to set the backing `value` field directly on the static GlobalConfig instances.
        injectField(HostAllocatorGlobalConfig.PHYSICAL_SERVER_CPU_SAFETY_BUFFER_PERCENT,    "value", "5");
        injectField(HostAllocatorGlobalConfig.PHYSICAL_SERVER_MEMORY_SAFETY_BUFFER_PERCENT, "value", "10");

        // EncryptColumnAspect is AspectJ-woven into every EntityManager.merge() / persist() call
        // — including those issued from production code under test. The aspect's @Autowired
        // pluginRegistry is null in unit-test context (no Spring container), so we set it
        // reflectively on the aspect singleton.
        EncryptColumnAspect aspect = EncryptColumnAspect.aspectOf();
        injectField(aspect, "pluginRegistry", pluginRgty);

        // EntityMetadata is consulted by EncryptColumnAspect to decide whether to invoke the
        // EncryptAfterSaveDbRecordExtensionPoint hook; in unit-test context the metadata cache
        // is empty so calls would throw "cannot find metadata for entity". Stub the static
        // to always return false (PSC has no @EncryptColumn fields anyway).
        metadataMock = Mockito.mockStatic(EntityMetadata.class);
        metadataMock.when(() -> EntityMetadata.hasEncryptField(any(Class.class))).thenReturn(false);
    }

    @After
    public void tearDown() {
        if (metadataMock != null) {
            metadataMock.close();
        }
    }

    // -------------------------------------------------------------------------
    // Scenario 1: happy KVM single role
    // -------------------------------------------------------------------------

    @Test
    public void happy_kvm_single_role_subtracts_consumed_and_buffer() {
        PhysicalServerCapacityVO psc = stubPsAndPsc();
        FakeRoleProvider kvm = FakeRoleProvider.kvm(8L, 16L * 1024L * 1024L * 1024L);
        when(pluginRgty.getExtensionList(PhysicalServerRoleProvider.class))
                .thenReturn(Collections.<PhysicalServerRoleProvider>singletonList(kvm));

        try (MockedStatic<Q> qStatic = Mockito.mockStatic(Q.class)) {
            stubRoleList(qStatic, Collections.singletonList(
                    role(ServerRoleType.KVM_HOST.toString(), "kvm-role-uuid")));

            updater.recalculate(SERVER_UUID);
        }

        // available = 32 - 8 - max(4, 32*5/100=1)=4 = 20
        assertEquals(20L, psc.getAvailableCpu());
        // available = 64GiB - 16GiB - 0(reservedMemory) - max(4GiB, 64GiB*10/100=6.4GiB) = 41.6GiB
        assertEquals(44667659879L, psc.getAvailableMemory());
        assertEquals(PhysicalServerCapacityState.Ready, psc.getCapacityState());
        verify(em, atLeastOnce()).merge(psc);
    }

    // -------------------------------------------------------------------------
    // Scenario 2: happy mixed (2 roles: KVM 4 + Container 2)
    // -------------------------------------------------------------------------

    @Test
    public void happy_mixed_roles_aggregate_consumed() {
        PhysicalServerCapacityVO psc = stubPsAndPsc();
        FakeRoleProvider kvm       = FakeRoleProvider.kvm(4L, 8L * 1024L * 1024L * 1024L);
        FakeRoleProvider container = FakeRoleProvider.container(2L, 4L * 1024L * 1024L * 1024L);

        when(pluginRgty.getExtensionList(PhysicalServerRoleProvider.class))
                .thenReturn(Arrays.<PhysicalServerRoleProvider>asList(kvm, container));

        try (MockedStatic<Q> qStatic = Mockito.mockStatic(Q.class)) {
            stubRoleList(qStatic, Arrays.asList(
                    role(ServerRoleType.KVM_HOST.toString(),       "kvm-role"),
                    role(ServerRoleType.CONTAINER_HOST.toString(), "container-role")));

            updater.recalculate(SERVER_UUID);
        }

        // mixed deployment (kvm+container, 2 roles) → buffer applies.
        // cpuBuffer = max(CPU_BUFFER_FLOOR=4, 32*5/100=1) = 4
        // available = 32 - (4+2) - 4 = 22
        assertEquals(22L, psc.getAvailableCpu());
        // memBuffer = max(MEMORY_BUFFER_FLOOR=4GiB, 64GiB*10/100=6.4GiB) = 6.4GiB
        // available = 64GiB - (8GiB+4GiB) - 0 - 6.4GiB = 45.6GiB
        assertEquals(48962627175L, psc.getAvailableMemory());
        assertEquals(PhysicalServerCapacityState.Ready, psc.getCapacityState());
    }

    // -------------------------------------------------------------------------
    // Scenario 3: edge — no roles → consumed = 0, available = total - buffer
    // -------------------------------------------------------------------------

    @Test
    public void edge_no_role_consumed_is_zero() {
        PhysicalServerCapacityVO psc = stubPsAndPsc();
        when(pluginRgty.getExtensionList(PhysicalServerRoleProvider.class))
                .thenReturn(Collections.<PhysicalServerRoleProvider>emptyList());

        try (MockedStatic<Q> qStatic = Mockito.mockStatic(Q.class)) {
            stubRoleList(qStatic, Collections.<PhysicalServerRoleVO>emptyList());

            updater.recalculate(SERVER_UUID);
        }

        // available = 32 - 0 = 32 (no implicit buffer)
        assertEquals(32L, psc.getAvailableCpu());
        // available = 64GiB - 0 - 0(reservedMemory) = 64GiB
        assertEquals(64L * 1024L * 1024L * 1024L, psc.getAvailableMemory());
        assertEquals(PhysicalServerCapacityState.Ready, psc.getCapacityState());
    }

    // -------------------------------------------------------------------------
    // Scenario 4: edge — PhysicalServer missing → fail-loud, no PSC mutation
    // -------------------------------------------------------------------------

    @Test
    public void edge_ps_missing_throws_OperationFailureException_no_psc_write() {
        when(em.find(eq(PhysicalServerVO.class), eq(SERVER_UUID))).thenReturn(null);

        try {
            updater.recalculate(SERVER_UUID);
            fail("expected OperationFailureException");
        } catch (OperationFailureException e) {
            assertNotNull(e.getErrorCode());
            String desc = e.getErrorCode().getDescription();
            assertTrue("error description should mention PhysicalServer not found, got: " + desc,
                    desc != null && desc.contains("PhysicalServer[uuid:" + SERVER_UUID + "] not found"));
        }
        verify(em, never()).merge(any());
    }

    // -------------------------------------------------------------------------
    // Scenario 5: concurrent — 2 threads recalculating same server
    // PESSIMISTIC_WRITE serialization is the DB's job; here we verify there is no
    // double-deduction in updater code: each call observes its own snapshot of
    // consumption + reservedMemory and writes deterministic values.
    // -------------------------------------------------------------------------

    @Test
    public void concurrent_two_threads_same_server_no_double_deduction() throws Exception {
        // Shared PSC instance — both threads observe the same totals (PESSIMISTIC_WRITE
        // serialization in production guarantees one writer at a time).
        final PhysicalServerCapacityVO psc = freshPsc();
        when(em.find(eq(PhysicalServerVO.class), eq(SERVER_UUID))).thenReturn(mock(PhysicalServerVO.class));
        when(em.find(eq(PhysicalServerCapacityVO.class), eq(SERVER_UUID), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(psc);

        final FakeRoleProvider kvm = FakeRoleProvider.kvm(8L, 16L * 1024L * 1024L * 1024L);
        when(pluginRgty.getExtensionList(PhysicalServerRoleProvider.class))
                .thenReturn(Collections.<PhysicalServerRoleProvider>singletonList(kvm));

        final CountDownLatch start = new CountDownLatch(1);
        final AtomicInteger errors = new AtomicInteger();

        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    start.await();
                    // MockedStatic is thread-local; each worker thread re-opens its own scope.
                    try (MockedStatic<Q> qStatic = Mockito.mockStatic(Q.class);
                         MockedStatic<EntityMetadata> metaStatic =
                                 Mockito.mockStatic(EntityMetadata.class)) {
                        stubRoleList(qStatic, Collections.singletonList(
                                role(ServerRoleType.KVM_HOST.toString(), "kvm-role")));
                        metaStatic.when(() -> EntityMetadata.hasEncryptField(any(Class.class)))
                                .thenReturn(false);
                        updater.recalculate(SERVER_UUID);
                    }
                } catch (Throwable t) {
                    errors.incrementAndGet();
                }
            }
        };

        Thread t1 = new Thread(task, "psc-recalc-concurrent-1");
        Thread t2 = new Thread(task, "psc-recalc-concurrent-2");
        t1.start();
        t2.start();
        start.countDown();
        t1.join(5_000L);
        t2.join(5_000L);
        assertFalse("worker 1 must finish before assertion", t1.isAlive());
        assertFalse("worker 2 must finish before assertion", t2.isAlive());

        assertEquals("no thread should have errored", 0, errors.get());
        // After both runs the value is the same idempotent result: no double-deduction since
        // recalculate() is a pure function of (totals, consumed, reserved, buffer); running it
        // twice produces the same available* on the shared row (total 32 - 8 - 4 = 20).
        assertEquals(20L, psc.getAvailableCpu());
        assertEquals(44667659879L, psc.getAvailableMemory());
        // Each thread's internal call invokes merge once.
        verify(em, times(2)).merge(psc);
    }

    // -------------------------------------------------------------------------
    // Scenario 6: provider throws → updater throws, PSC unchanged
    // -------------------------------------------------------------------------

    @Test
    public void provider_throws_psc_remains_unmodified() {
        // Pre-set distinctive PSC values so we can detect any partial write.
        PhysicalServerCapacityVO psc = freshPsc();
        psc.setAvailableCpu(999L);
        psc.setAvailableMemory(7777L);
        psc.setCapacityState(PhysicalServerCapacityState.Initialized);
        long originalAvailableCpu    = psc.getAvailableCpu();
        long originalAvailableMemory = psc.getAvailableMemory();
        PhysicalServerCapacityState originalState = psc.getCapacityState();

        when(em.find(eq(PhysicalServerVO.class), eq(SERVER_UUID))).thenReturn(mock(PhysicalServerVO.class));
        when(em.find(eq(PhysicalServerCapacityVO.class), eq(SERVER_UUID), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(psc);

        FakeRoleProvider exploding = new FakeRoleProvider(
                ServerRoleType.KVM_HOST, /*usedCpu*/ 0, /*usedMem*/ 0, /*exclusive*/ false,
                /*throwOnConsumption*/ true);
        when(pluginRgty.getExtensionList(PhysicalServerRoleProvider.class))
                .thenReturn(Collections.<PhysicalServerRoleProvider>singletonList(exploding));

        try (MockedStatic<Q> qStatic = Mockito.mockStatic(Q.class)) {
            stubRoleList(qStatic, Collections.singletonList(
                    role(ServerRoleType.KVM_HOST.toString(), "kvm-role")));

            try {
                updater.recalculate(SERVER_UUID);
                fail("expected OperationFailureException");
            } catch (OperationFailureException e) {
                String desc = e.getErrorCode().getDescription();
                assertTrue("expected provider failure description, got: " + desc,
                        desc != null && desc.contains("getCapacityConsumption failed"));
            }
        }

        // PSC must not have been merged.
        verify(em, never()).merge(any());
        assertEquals(originalAvailableCpu,    psc.getAvailableCpu());
        assertEquals(originalAvailableMemory, psc.getAvailableMemory());
        assertEquals(originalState,           psc.getCapacityState());
    }

    // -------------------------------------------------------------------------
    // Scenario 7: SPI — no extension registered → same as base buffer only
    // -------------------------------------------------------------------------

    @Test
    public void spi_no_extension_registered_uses_buffer_only() {
        PhysicalServerCapacityVO psc = stubPsAndPsc();
        FakeRoleProvider kvm = FakeRoleProvider.kvm(8L, 16L * 1024L * 1024L * 1024L);
        when(pluginRgty.getExtensionList(PhysicalServerRoleProvider.class))
                .thenReturn(Collections.<PhysicalServerRoleProvider>singletonList(kvm));
        // ServerReservedCapacityExtensionPoint: default stub already returns emptyList from setUp()

        try (MockedStatic<Q> qStatic = Mockito.mockStatic(Q.class)) {
            stubRoleList(qStatic, Collections.singletonList(
                    role(ServerRoleType.KVM_HOST.toString(), "kvm-role-uuid")));
            updater.recalculate(SERVER_UUID);
        }

        // extReservedCpu=0, extReservedMemory=0 → identical to scenario 1
        assertEquals(20L, psc.getAvailableCpu());
        assertEquals(44667659879L, psc.getAvailableMemory());
    }

    // -------------------------------------------------------------------------
    // Scenario 8: SPI — one extension returns positive cpu+memory reserved
    // -------------------------------------------------------------------------

    @Test
    public void spi_one_extension_with_positive_reserved_reduces_available() {
        PhysicalServerCapacityVO psc = stubPsAndPsc();
        FakeRoleProvider kvm = FakeRoleProvider.kvm(8L, 16L * 1024L * 1024L * 1024L);
        when(pluginRgty.getExtensionList(PhysicalServerRoleProvider.class))
                .thenReturn(Collections.<PhysicalServerRoleProvider>singletonList(kvm));

        long extCpu = 2L;
        long extMem = 2L * 1024L * 1024L * 1024L; // 2 GiB
        when(pluginRgty.getExtensionList(ServerReservedCapacityExtensionPoint.class))
                .thenReturn(Collections.<ServerReservedCapacityExtensionPoint>singletonList(
                        new FakeReservedCapacityExt(extCpu, extMem)));

        try (MockedStatic<Q> qStatic = Mockito.mockStatic(Q.class)) {
            stubRoleList(qStatic, Collections.singletonList(
                    role(ServerRoleType.KVM_HOST.toString(), "kvm-role-uuid")));
            updater.recalculate(SERVER_UUID);
        }

        // availableCpu = 32 - 8 - 4(buffer) - 2(ext) = 18
        assertEquals(18L, psc.getAvailableCpu());
        // availableMemory = 64GiB - 16GiB - 0(reserved) - 6.4GiB(buffer) - 2GiB(ext) = 39.6GiB
        long expectedMem = 44667659879L - extMem;
        assertEquals(expectedMem, psc.getAvailableMemory());
    }

    // -------------------------------------------------------------------------
    // Scenario 9: SPI — extension returns null → skipped, no NPE
    // -------------------------------------------------------------------------

    @Test
    public void spi_extension_returns_null_is_skipped_no_npe() {
        PhysicalServerCapacityVO psc = stubPsAndPsc();
        FakeRoleProvider kvm = FakeRoleProvider.kvm(8L, 16L * 1024L * 1024L * 1024L);
        when(pluginRgty.getExtensionList(PhysicalServerRoleProvider.class))
                .thenReturn(Collections.<PhysicalServerRoleProvider>singletonList(kvm));
        when(pluginRgty.getExtensionList(ServerReservedCapacityExtensionPoint.class))
                .thenReturn(Collections.<ServerReservedCapacityExtensionPoint>singletonList(
                        new FakeReservedCapacityExt(null)));

        try (MockedStatic<Q> qStatic = Mockito.mockStatic(Q.class)) {
            stubRoleList(qStatic, Collections.singletonList(
                    role(ServerRoleType.KVM_HOST.toString(), "kvm-role-uuid")));
            updater.recalculate(SERVER_UUID);
        }

        // null return → ext contribution = 0, same as no-ext scenario
        assertEquals(20L, psc.getAvailableCpu());
        assertEquals(44667659879L, psc.getAvailableMemory());
    }

    // -------------------------------------------------------------------------
    // Scenario 10: SPI — extension returns fully-negative values → entire tuple
    // rejected per P1-1 (was: per-field >0 clamp; now: whole-or-nothing reject).
    // Net effect on this happy-baseline server is identical to no-ext: 20 / 44.6 GiB.
    // -------------------------------------------------------------------------

    @Test
    public void spi_extension_returns_negative_values_whole_tuple_rejected() {
        PhysicalServerCapacityVO psc = stubPsAndPsc();
        FakeRoleProvider kvm = FakeRoleProvider.kvm(8L, 16L * 1024L * 1024L * 1024L);
        when(pluginRgty.getExtensionList(PhysicalServerRoleProvider.class))
                .thenReturn(Collections.<PhysicalServerRoleProvider>singletonList(kvm));
        when(pluginRgty.getExtensionList(ServerReservedCapacityExtensionPoint.class))
                .thenReturn(Collections.<ServerReservedCapacityExtensionPoint>singletonList(
                        new FakeReservedCapacityExt(-100L, -1024L * 1024L * 1024L)));

        try (MockedStatic<Q> qStatic = Mockito.mockStatic(Q.class)) {
            stubRoleList(qStatic, Collections.singletonList(
                    role(ServerRoleType.KVM_HOST.toString(), "kvm-role-uuid")));
            updater.recalculate(SERVER_UUID);
        }

        assertEquals(20L, psc.getAvailableCpu());
        assertEquals(44667659879L, psc.getAvailableMemory());
    }

    // -------------------------------------------------------------------------
    // Scenario 10b (P1-1): SPI — extension returns partial-negative (cpu=+10,
    // mem=-1) → ENTIRE tuple rejected. Old per-field guard would have honoured
    // cpu=10 (availableCpu=18); new whole-or-nothing behavior leaves cpu=20.
    // -------------------------------------------------------------------------

    @Test
    public void spi_partial_negative_rejects_whole_tuple_p1_1() {
        PhysicalServerCapacityVO psc = stubPsAndPsc();
        FakeRoleProvider kvm = FakeRoleProvider.kvm(8L, 16L * 1024L * 1024L * 1024L);
        when(pluginRgty.getExtensionList(PhysicalServerRoleProvider.class))
                .thenReturn(Collections.<PhysicalServerRoleProvider>singletonList(kvm));
        // Partial-negative: positive cpu + negative memory.
        when(pluginRgty.getExtensionList(ServerReservedCapacityExtensionPoint.class))
                .thenReturn(Collections.<ServerReservedCapacityExtensionPoint>singletonList(
                        new FakeReservedCapacityExt(10L, -1L)));

        try (MockedStatic<Q> qStatic = Mockito.mockStatic(Q.class)) {
            stubRoleList(qStatic, Collections.singletonList(
                    role(ServerRoleType.KVM_HOST.toString(), "kvm-role-uuid")));
            updater.recalculate(SERVER_UUID);
        }

        // Whole tuple discarded → identical to baseline; cpu=10 NOT honoured.
        assertEquals(20L, psc.getAvailableCpu());
        assertEquals(44667659879L, psc.getAvailableMemory());
    }

    // -------------------------------------------------------------------------
    // Scenario 10c (P1-1): SPI — extension returns (0, 0) → valid no-op
    // contribution (e.g. Container with no cordoned pods). Distinct from null-
    // return (scenario 9): null skips the impl entirely; (0, 0) records zero.
    // Both produce identical numeric output here, but the path through the loop
    // differs — this test exists so a later refactor that conflates zero with
    // negative again fails loudly.
    // -------------------------------------------------------------------------

    @Test
    public void spi_zero_zero_is_valid_no_op_p1_1() {
        PhysicalServerCapacityVO psc = stubPsAndPsc();
        FakeRoleProvider kvm = FakeRoleProvider.kvm(8L, 16L * 1024L * 1024L * 1024L);
        when(pluginRgty.getExtensionList(PhysicalServerRoleProvider.class))
                .thenReturn(Collections.<PhysicalServerRoleProvider>singletonList(kvm));
        when(pluginRgty.getExtensionList(ServerReservedCapacityExtensionPoint.class))
                .thenReturn(Collections.<ServerReservedCapacityExtensionPoint>singletonList(
                        new FakeReservedCapacityExt(0L, 0L)));

        try (MockedStatic<Q> qStatic = Mockito.mockStatic(Q.class)) {
            stubRoleList(qStatic, Collections.singletonList(
                    role(ServerRoleType.KVM_HOST.toString(), "kvm-role-uuid")));
            updater.recalculate(SERVER_UUID);
        }

        assertEquals(20L, psc.getAvailableCpu());
        assertEquals(44667659879L, psc.getAvailableMemory());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Build PSC, wire em.find stubs for both PSV existence and PSC PESSIMISTIC_WRITE lookup. */
    private PhysicalServerCapacityVO stubPsAndPsc() {
        PhysicalServerCapacityVO psc = freshPsc();
        when(em.find(eq(PhysicalServerVO.class), eq(SERVER_UUID))).thenReturn(mock(PhysicalServerVO.class));
        when(em.find(eq(PhysicalServerCapacityVO.class), eq(SERVER_UUID), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenReturn(psc);
        return psc;
    }

    private static PhysicalServerCapacityVO freshPsc() {
        PhysicalServerCapacityVO psc = new PhysicalServerCapacityVO();
        psc.setUuid(SERVER_UUID);
        psc.setTotalCpu(TOTAL_CPU);
        psc.setTotalMemory(TOTAL_MEMORY);
        psc.setReservedMemory(0L);
        psc.setCapacityState(PhysicalServerCapacityState.Stale);
        return psc;
    }

    private static PhysicalServerRoleVO role(String roleType, String roleUuid) {
        PhysicalServerRoleVO v = new PhysicalServerRoleVO();
        v.setServerUuid(SERVER_UUID);
        v.setRoleType(roleType);
        v.setRoleUuid(roleUuid);
        return v;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void stubRoleList(MockedStatic<Q> qStatic, List<PhysicalServerRoleVO> rolesToReturn) {
        Q mockQ = mock(Q.class);
        qStatic.when(() -> Q.New(PhysicalServerRoleVO.class)).thenReturn(mockQ);
        when(mockQ.eq(any(), any())).thenReturn(mockQ);
        when(mockQ.list()).thenReturn((List) new ArrayList<>(rolesToReturn));
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

    // -------------------------------------------------------------------------
    // Hand-written PhysicalServerRoleProvider stub.
    //
    // Mockito-inline cannot mock interfaces with Java 8 servlet quirks reliably (see
    // KvmRoleProviderTest comment); the simpler path is a hand-written stub that records
    // arguments and returns a deterministic CapacityUsage.
    // -------------------------------------------------------------------------
    private static class FakeRoleProvider implements PhysicalServerRoleProvider {
        private final ServerRoleType type;
        private final long usedCpu;
        private final long usedMemory;
        private final boolean exclusive;
        private final boolean throwOnConsumption;

        FakeRoleProvider(ServerRoleType type, long usedCpu, long usedMemory,
                         boolean exclusive, boolean throwOnConsumption) {
            this.type = type;
            this.usedCpu = usedCpu;
            this.usedMemory = usedMemory;
            this.exclusive = exclusive;
            this.throwOnConsumption = throwOnConsumption;
        }

        static FakeRoleProvider kvm(long usedCpu, long usedMemory) {
            return new FakeRoleProvider(ServerRoleType.KVM_HOST, usedCpu, usedMemory, false, false);
        }

        static FakeRoleProvider container(long usedCpu, long usedMemory) {
            return new FakeRoleProvider(ServerRoleType.CONTAINER_HOST, usedCpu, usedMemory, false, false);
        }

        @Override public ServerRoleType getRoleType()       { return type; }
        @Override public SchedulingMode getSchedulingMode() { return SchedulingMode.INTERNAL_SHARED; }

        @Override
        public CapacityUsage getCapacityConsumption(String serverUuid, String roleUuid) {
            if (throwOnConsumption) {
                throw new RuntimeException("simulated provider failure");
            }
            CapacityUsage u = new CapacityUsage();
            u.setUsedCpu(usedCpu);
            u.setUsedMemory(usedMemory);
            u.setExclusive(exclusive);
            return u;
        }

        @Override public void createRoleEntity(CreateRoleEntityContext context, org.zstack.header.core.ReturnValueCompletion<String> completion) { throw new UnsupportedOperationException(); }
        @Override public void deleteRoleEntity(String roleUuid, org.zstack.header.core.Completion completion) { throw new UnsupportedOperationException(); }
        @Override public RoleWorkloadStatus getWorkloadStatus(String serverUuid, String roleUuid) { throw new UnsupportedOperationException(); }
    }

    // -------------------------------------------------------------------------
    // Hand-written ServerReservedCapacityExtensionPoint stub.
    // Supports both null-return and fixed positive/negative capacity scenarios.
    // -------------------------------------------------------------------------
    private static class FakeReservedCapacityExt implements ServerReservedCapacityExtensionPoint {
        private final ReservedHostCapacity result;

        /** Construct with a pre-built result (may be null). */
        FakeReservedCapacityExt(ReservedHostCapacity result) {
            this.result = result;
        }

        /** Convenience: build a non-null result with the given cpu/memory values. */
        FakeReservedCapacityExt(long reservedCpu, long reservedMemory) {
            ReservedHostCapacity rc = new ReservedHostCapacity();
            rc.setReservedCpuCapacity(reservedCpu);
            rc.setReservedMemoryCapacity(reservedMemory);
            this.result = rc;
        }

        @Override
        public ReservedHostCapacity getReservedCapacityForPhysicalServer(String physicalServerUuid) {
            return result;
        }
    }
}
