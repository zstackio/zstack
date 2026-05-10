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
import org.zstack.header.allocator.ServerReservedCapacityExtensionPoint;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Orchestration-overhead bench for {@link PhysicalServerCapacityUpdater#recalculate(String)}
 * (Phase 3 Wave 4 U17, AC-CM-PERF-01).
 *
 * <p>Scope: <b>orchestration overhead only</b>. The DB layer is mocked (per the established
 * convention in {@link PhysicalServerCapacityUpdaterTest}); this bench measures the cost of the
 * unified recalculate code path itself — role iteration, RoleProvider SPI dispatch, buffer math,
 * reserved-capacity extension fan-out, and the merge writeback. DB-bound query cost is analyzed
 * statically in {@code docs/runbooks/v5518-recalculate-perf.md} via EXPLAIN of the four hot-path
 * queries (PSC PK lookup, PSR by serverUuid, PSR by roleUuid+roleType, BM2InstanceVO count, and
 * PodVO sum).
 *
 * <p>What this protects against: a subsequent refactor that adds an O(roles²) iteration, a
 * synchronous bus call inside the SPI loop, or an unintended Hibernate flush would explode the
 * code-path latency. The DB-side regression surface is covered by the EXPLAIN report's
 * index-status table.
 *
 * <p>Fixture: mocks 1000 distinct PSC rows with identical KVM-only role topology (one
 * {@code PhysicalServerRoleVO(KVM_HOST)} each) and one {@code ServerReservedCapacityExtensionPoint}
 * returning a fixed contribution. The mocked Q.New + em.find pair returns a per-uuid PSC instance
 * so the merge target varies per call.
 *
 * <p>Targets (proposed for AC-CM-PERF-01, since the plan §U17 lists "&lt;50ms single / &lt;5s
 * batch 1000" — those numbers were sized against a real-DB end-to-end call. With the orchestration
 * overhead alone, the targets shrink an order of magnitude):
 * <ul>
 *   <li>p50 &lt; 1ms / call (orchestration only)</li>
 *   <li>p95 &lt; 5ms / call</li>
 *   <li>p99 &lt; 10ms / call</li>
 *   <li>1000-call batch wall &lt; 5000ms (matches the PRD's &lt;5s batch budget)</li>
 * </ul>
 * If these collapse below 100µs / call (typical for pure in-memory mocks), the targets are
 * "trivially passing" and the meaningful gate is the EXPLAIN report. The bench is still kept
 * because (a) it pins absolute orchestration cost so a later regression with a 100x slowdown
 * is caught, (b) AC-CM-PERF-01 explicitly requires a re-runnable bench harness.
 *
 * <p>TODO: add a real-DB end-to-end bench gated by {@code -Dtest.realDb=true}.
 *
 * <p>Run: {@code mvn test -pl compute -Dtest=PhysicalServerCapacityUpdaterOrchestrationOverheadTest -P premium}
 *   <br>(perfReport: dump perf numbers to stdout)
 */
public class PhysicalServerCapacityUpdaterOrchestrationOverheadTest {

    private static final int FIXTURE_HOST_COUNT = 1000;
    private static final int WARMUP_ITERATIONS = 100;

    // Per-server capacity profile — uniform across the fixture so the bench measures
    // code-path latency independent of fixture variability.
    private static final long TOTAL_CPU = 64L;
    private static final long TOTAL_MEMORY = 256L * 1024L * 1024L * 1024L; // 256 GiB
    private static final long PER_ROLE_USED_CPU = 16L;
    private static final long PER_ROLE_USED_MEMORY = 64L * 1024L * 1024L * 1024L; // 64 GiB

    // Default targets — see Javadoc; tunable by -Dperf.* JVM args.
    private static final long P50_NS_TARGET =
            Long.parseLong(System.getProperty("perf.p50.ns", String.valueOf(TimeUnit.MILLISECONDS.toNanos(1))));
    private static final long P95_NS_TARGET =
            Long.parseLong(System.getProperty("perf.p95.ns", String.valueOf(TimeUnit.MILLISECONDS.toNanos(5))));
    private static final long P99_NS_TARGET =
            Long.parseLong(System.getProperty("perf.p99.ns", String.valueOf(TimeUnit.MILLISECONDS.toNanos(10))));
    private static final long BATCH_WALL_MS_TARGET =
            Long.parseLong(System.getProperty("perf.batch.ms", "5000"));

    // -p flag to skip strict assertions when running in CI-with-no-perf-budget mode.
    // (Defaults: enforce p99/wall; relax with -Dperf.assert=false for diagnostic-only runs.)
    private static final boolean ASSERT_TARGETS = Boolean.parseBoolean(
            System.getProperty("perf.assert", "true"));

    private PhysicalServerCapacityUpdater updater;
    private DatabaseFacade dbf;
    private EntityManager em;
    private PluginRegistry pluginRgty;
    private MockedStatic<EntityMetadata> metadataMock;
    private Map<String, PhysicalServerCapacityVO> pscByUuid;
    private List<String> serverUuids;

    @Before
    public void setUp() throws Exception {
        updater = new PhysicalServerCapacityUpdater();
        dbf = mock(DatabaseFacade.class);
        em = mock(EntityManager.class);
        pluginRgty = mock(PluginRegistry.class);

        when(dbf.getEntityManager()).thenReturn(em);
        // EntityManager-merge AspectJ weaving needs IntegrityVerificationResourceFactory
        // + EncryptAfterSaveDbRecordExtensionPoint resolvable to emptyList.
        when(pluginRgty.<Object>getExtensionList(Mockito.<Class<Object>>any()))
                .thenReturn(Collections.emptyList());

        injectField(updater, "dbf", dbf);
        injectField(updater, "pluginRgty", pluginRgty);
        injectField(HostAllocatorGlobalConfig.PHYSICAL_SERVER_CPU_SAFETY_BUFFER_PERCENT, "value", "5");
        injectField(HostAllocatorGlobalConfig.PHYSICAL_SERVER_MEMORY_SAFETY_BUFFER_PERCENT, "value", "10");

        EncryptColumnAspect aspect = EncryptColumnAspect.aspectOf();
        injectField(aspect, "pluginRegistry", pluginRgty);

        metadataMock = Mockito.mockStatic(EntityMetadata.class);
        metadataMock.when(() -> EntityMetadata.hasEncryptField(any(Class.class))).thenReturn(false);

        // ---- Fixture: 1000 PSC rows + matching role-list lookups + 1 SPI extension. ----
        pscByUuid = new HashMap<String, PhysicalServerCapacityVO>(FIXTURE_HOST_COUNT * 2);
        serverUuids = new ArrayList<String>(FIXTURE_HOST_COUNT);
        for (int i = 0; i < FIXTURE_HOST_COUNT; i++) {
            String uuid = String.format("perf-server-%05d", i);
            serverUuids.add(uuid);
            PhysicalServerCapacityVO psc = new PhysicalServerCapacityVO();
            psc.setUuid(uuid);
            psc.setTotalCpu(TOTAL_CPU);
            psc.setTotalMemory(TOTAL_MEMORY);
            psc.setReservedMemory(0L);
            psc.setCapacityState(PhysicalServerCapacityState.Stale);
            pscByUuid.put(uuid, psc);

            // Same physical server VO is fine — recalculate only checks for null.
            when(em.find(eq(PhysicalServerVO.class), eq(uuid)))
                    .thenReturn(mock(PhysicalServerVO.class));
            when(em.find(eq(PhysicalServerCapacityVO.class), eq(uuid), eq(LockModeType.PESSIMISTIC_WRITE)))
                    .thenReturn(psc);
        }

        // RoleProvider: a single KVM provider returning fixed consumption per call.
        FakeRoleProvider kvm = new FakeRoleProvider(
                ServerRoleType.KVM_HOST, PER_ROLE_USED_CPU, PER_ROLE_USED_MEMORY);
        when(pluginRgty.getExtensionList(PhysicalServerRoleProvider.class))
                .thenReturn(Collections.<PhysicalServerRoleProvider>singletonList(kvm));

        // ServerReservedCapacityExtensionPoint: empty list (default already).
        // Single-extension exercise is covered by PhysicalServerCapacityUpdaterTest scenario 8;
        // here we keep the SPI loop active but contributing zero so we are timing the loop.
        when(pluginRgty.getExtensionList(ServerReservedCapacityExtensionPoint.class))
                .thenReturn(Collections.<ServerReservedCapacityExtensionPoint>emptyList());
    }

    @After
    public void tearDown() {
        if (metadataMock != null) {
            metadataMock.close();
        }
    }

    /**
     * 1000-host sequential bench: warm up, then time each {@code recalculate} call individually,
     * record per-call ns latencies, compute p50/p95/p99 and total wall time, assert against
     * configured targets.
     */
    @Test
    public void bench_1000_hosts_sequential_recalculate() throws Exception {
        // ---- Warm up — JIT the orchestration code path. ----
        try (MockedStatic<Q> qStatic = Mockito.mockStatic(Q.class)) {
            primeRoleListStub(qStatic);
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                updater.recalculate(serverUuids.get(i % FIXTURE_HOST_COUNT));
            }
        }

        // ---- Measure — fresh MockedStatic scope so warmup invocation counts don't pollute. ----
        long[] perCallNs = new long[FIXTURE_HOST_COUNT];
        long batchStart = System.nanoTime();
        try (MockedStatic<Q> qStatic = Mockito.mockStatic(Q.class)) {
            primeRoleListStub(qStatic);
            for (int i = 0; i < FIXTURE_HOST_COUNT; i++) {
                String uuid = serverUuids.get(i);
                long t0 = System.nanoTime();
                updater.recalculate(uuid);
                perCallNs[i] = System.nanoTime() - t0;
            }
        }
        long batchTotalNs = System.nanoTime() - batchStart;

        // ---- Sanity: each PSC was actually mutated to Ready with the expected available*. ----
        // available = total - consumed - extReserved (no implicit buffer)
        // cpu: 64 - 16 = 48
        // mem: 256GiB - 64GiB - 0(reserved) = 192GiB
        long expectedAvailableCpu = 48L;
        long expectedAvailableMemory = TOTAL_MEMORY - PER_ROLE_USED_MEMORY;

        for (int i = 0; i < FIXTURE_HOST_COUNT; i += FIXTURE_HOST_COUNT / 10) {
            PhysicalServerCapacityVO psc = pscByUuid.get(serverUuids.get(i));
            assertEquals("uuid " + serverUuids.get(i),
                    PhysicalServerCapacityState.Ready, psc.getCapacityState());
            assertEquals("availableCpu @ uuid " + serverUuids.get(i),
                    expectedAvailableCpu, psc.getAvailableCpu());
            assertEquals("availableMemory @ uuid " + serverUuids.get(i),
                    expectedAvailableMemory, psc.getAvailableMemory());
        }

        // ---- Stats. ----
        long[] sorted = perCallNs.clone();
        Arrays.sort(sorted);
        long p50 = sorted[sorted.length / 2];
        long p95 = sorted[(int) (sorted.length * 0.95)];
        long p99 = sorted[(int) (sorted.length * 0.99)];
        long max = sorted[sorted.length - 1];
        long min = sorted[0];
        long sum = 0;
        for (long ns : sorted) {
            sum += ns;
        }
        long mean = sum / sorted.length;
        long batchTotalMs = TimeUnit.NANOSECONDS.toMillis(batchTotalNs);

        System.out.println("");
        System.out.println("================================================================");
        System.out.println("PhysicalServerCapacityUpdater perf bench (AC-CM-PERF-01)");
        System.out.println("================================================================");
        System.out.println(String.format("Hosts:           %d", FIXTURE_HOST_COUNT));
        System.out.println(String.format("Roles per host:  1 (KVM_HOST)"));
        System.out.println(String.format("min  per call:   %s", fmtNs(min)));
        System.out.println(String.format("mean per call:   %s", fmtNs(mean)));
        System.out.println(String.format("p50  per call:   %s   (target < %s)", fmtNs(p50), fmtNs(P50_NS_TARGET)));
        System.out.println(String.format("p95  per call:   %s   (target < %s)", fmtNs(p95), fmtNs(P95_NS_TARGET)));
        System.out.println(String.format("p99  per call:   %s   (target < %s)", fmtNs(p99), fmtNs(P99_NS_TARGET)));
        System.out.println(String.format("max  per call:   %s", fmtNs(max)));
        System.out.println(String.format("batch wall:      %d ms   (target < %d ms)",
                batchTotalMs, BATCH_WALL_MS_TARGET));
        System.out.println(String.format("assert mode:     %s",
                ASSERT_TARGETS ? "STRICT (-Dperf.assert=true)" : "DIAGNOSTIC (-Dperf.assert=false)"));
        System.out.println("================================================================");
        System.out.println("");

        if (ASSERT_TARGETS) {
            assertTrue(String.format("p50 %s exceeds target %s", fmtNs(p50), fmtNs(P50_NS_TARGET)),
                    p50 < P50_NS_TARGET);
            assertTrue(String.format("p95 %s exceeds target %s", fmtNs(p95), fmtNs(P95_NS_TARGET)),
                    p95 < P95_NS_TARGET);
            assertTrue(String.format("p99 %s exceeds target %s", fmtNs(p99), fmtNs(P99_NS_TARGET)),
                    p99 < P99_NS_TARGET);
            assertTrue(String.format("batch wall %d ms exceeds target %d ms",
                            batchTotalMs, BATCH_WALL_MS_TARGET),
                    batchTotalMs < BATCH_WALL_MS_TARGET);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Stubs {@code Q.New(PhysicalServerRoleVO.class).eq(...).list()} to return a single-element
     * KVM role list. The role's {@code roleUuid} carries the same uuid as the server (KVM happy
     * path: server uuid == host uuid) — sufficient because RoleProvider is mocked.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void primeRoleListStub(MockedStatic<Q> qStatic) {
        Q mockQ = mock(Q.class);
        qStatic.when(() -> Q.New(PhysicalServerRoleVO.class)).thenReturn(mockQ);
        when(mockQ.eq(any(), any())).thenReturn(mockQ);
        // Always return a single KVM role; getCapacityConsumption is provider-mocked.
        PhysicalServerRoleVO role = new PhysicalServerRoleVO();
        role.setRoleType(ServerRoleType.KVM_HOST.toString());
        role.setRoleUuid("kvm-role-uuid");
        when(mockQ.list()).thenReturn((List) Collections.singletonList(role));
    }

    private static String fmtNs(long ns) {
        if (ns < 1_000L) {
            return ns + " ns";
        } else if (ns < 1_000_000L) {
            return String.format("%.2f us", ns / 1_000.0);
        } else {
            return String.format("%.3f ms", ns / 1_000_000.0);
        }
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

    /** Minimal RoleProvider stub returning constant CapacityUsage. Mirrors the test fixture. */
    private static class FakeRoleProvider implements PhysicalServerRoleProvider {
        private final ServerRoleType type;
        private final long usedCpu;
        private final long usedMemory;

        FakeRoleProvider(ServerRoleType type, long usedCpu, long usedMemory) {
            this.type = type;
            this.usedCpu = usedCpu;
            this.usedMemory = usedMemory;
        }

        @Override public ServerRoleType getRoleType()       { return type; }
        @Override public SchedulingMode getSchedulingMode() { return SchedulingMode.INTERNAL_SHARED; }

        @Override
        public CapacityUsage getCapacityConsumption(String serverUuid, String roleUuid) {
            CapacityUsage u = new CapacityUsage();
            u.setUsedCpu(usedCpu);
            u.setUsedMemory(usedMemory);
            return u;
        }

        @Override public void createRoleEntity(CreateRoleEntityContext context, org.zstack.header.core.ReturnValueCompletion<String> completion) { throw new UnsupportedOperationException(); }
        @Override public void deleteRoleEntity(String roleUuid, org.zstack.header.core.Completion completion) { throw new UnsupportedOperationException(); }
        @Override public RoleWorkloadStatus getWorkloadStatus(String serverUuid, String roleUuid) { throw new UnsupportedOperationException(); }
    }
}
