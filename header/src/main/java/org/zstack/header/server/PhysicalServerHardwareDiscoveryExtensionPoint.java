package org.zstack.header.server;

/**
 * U16: per-source hardware-info contributor SPI.
 *
 * <p>Each role module (KVM / BM2 / Container) provides one implementation that adapts
 * its own persisted state or remote agent into a {@code UnifiedHardwareInfo}-shaped
 * carrier. {@code PhysicalServerHardwareService} calls each impl in turn and merges
 * non-null fields into the aggregate.</p>
 *
 * <p>The DTO type is intentionally module-local ({@code org.zstack.server.hardware.UnifiedHardwareInfo}),
 * so this SPI uses a generic carrier interface to keep header free of plugin types.
 * Implementations populate the carrier via the simple setters on
 * {@link HardwareInfoCarrier}; the service downcasts for actual storage.</p>
 *
 * <h3>Single-PSR-query contract</h3>
 *
 * <p>The whole discover path runs inside a {@code PhysicalServerCapacityVO}
 * PESSIMISTIC_WRITE lock during fleet recalculate. Every extra DB round-trip
 * inside that lock multiplies fleet sweep latency by N-hosts. To keep the lock
 * window tight, this SPI collapses the historical {@code isApplicable + discover}
 * pair into a single {@link #discover} method whose contract is:</p>
 *
 * <ul>
 *   <li>The implementation MUST resolve its role-entity uuid (KVM hostUuid / BM2
 *       chassisUuid / NativeHost uuid) <strong>at most once</strong> per call.
 *       No second {@code Q.New(PhysicalServerRoleVO.class)} on the same server
 *       within the same {@code discover} invocation.</li>
 *   <li>If the server is not applicable (e.g. no role row of the matching type),
 *       {@code discover} MUST return {@code false} <strong>without populating
 *       the carrier</strong>. The orchestrator uses the return value to decide
 *       whether this contribution counts toward {@code discoverSource}.</li>
 *   <li>If applicable, the impl MUST set null (i.e. skip setter call) for fields
 *       it cannot supply — the service's {@code mergeNonNull} contract relies on
 *       this. Returns {@code true} regardless of how many setters fired (even 0
 *       is "applicable but contributed nothing for this row"; this is rare and
 *       still preferable to throwing).</li>
 *   <li>MUST NOT throw on transient backend failures; log and return {@code false}
 *       (treat the failure as a non-applicable result for this pass).</li>
 * </ul>
 *
 * <p>The orchestrator also pre-screens by {@link #getDiscoverSource()} so an
 * impl is only called for its own source tag; this method is therefore the
 * single per-server entry point per applicable source.</p>
 */
public interface PhysicalServerHardwareDiscoveryExtensionPoint {

    /**
     * Identifies the source for {@code PhysicalServerHardwareInfoVO.discoverSource}.
     * Suggested values: "IPMI_FRU", "KVM_AGENT", "K8S_NODEINFO".
     */
    String getDiscoverSource();

    /**
     * Resolve role-entity uuid once, populate the carrier with whatever fields
     * this source can supply, and report whether this server was applicable.
     *
     * <p>See class-level "Single-PSR-query contract" for invariants.</p>
     *
     * @param server  physical server under discovery; never null.
     * @param carrier setter-only view onto the aggregate {@code UnifiedHardwareInfo};
     *                impls call setters only for fields they know.
     * @return {@code true} if this contributor applies to {@code server} (caller
     *         counts it toward {@code discoverSource}); {@code false} if not
     *         applicable (no role row of the matching type, or transient lookup
     *         failure). When {@code false}, the carrier MUST be left untouched.
     */
    boolean discover(PhysicalServerVO server, HardwareInfoCarrier carrier);

    /**
     * Setter-only view onto {@code UnifiedHardwareInfo}. Header types only.
     */
    interface HardwareInfoCarrier {
        void setManufacturer(String v);
        void setModel(String v);
        void setSerialNumber(String v);
        void setBiosVersion(String v);
        void setCpuModel(String v);
        void setCpuSockets(Integer v);
        void setCpuCores(Integer v);
        void setCpuArchitecture(String v);
        void setTotalMemoryBytes(Long v);
        void setMemoryModuleCount(Integer v);
        void setTotalDiskBytes(Long v);
        void setDiskCount(Integer v);
        void setNicCount(Integer v);
        void setGpuCount(Integer v);
        void setHealthStatus(String v);
    }
}
