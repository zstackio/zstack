package org.zstack.kvm.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zstack.compute.host.HostSystemTags;
import org.zstack.core.db.Q;
import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.allocator.HostCapacityVO_;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.server.PhysicalServerHardwareDiscoveryExtensionPoint;
import org.zstack.header.server.PhysicalServerRoleVO;
import org.zstack.header.server.PhysicalServerRoleVO_;
import org.zstack.header.server.PhysicalServerVO;
import org.zstack.header.server.ServerRoleType;
import org.zstack.tag.PatternedSystemTag;

/**
 * U16a: KVM-side contributor for {@link PhysicalServerHardwareDiscoveryExtensionPoint}.
 *
 * <p>DB-only read path. The KVM agent populates host facts at connect time
 * ({@code KVMHost.saveGeneralHostHardwareFacts}), which materialises in {@code HostVO} columns,
 * {@code HostCapacityVO} columns and {@code HostSystemTags}. This adapter projects those
 * persisted fields into the carrier — no SSH / agent / IPMI calls — so it is safe to run on
 * any thread without I/O budgeting.</p>
 *
 * <p>Carrier fields left null (no canonical KVM source today): {@code memoryModuleCount},
 * {@code totalDiskBytes}, {@code diskCount}, {@code nicCount}, {@code gpuCount},
 * {@code healthStatus}. The mergeNonNull contract in {@code PhysicalServerHardwareService}
 * relies on null = "this source did not contribute", so other adapters (IPMI FRU) can fill them.</p>
 */
public class KvmHardwareDiscoveryAdapter implements PhysicalServerHardwareDiscoveryExtensionPoint {
    private static final Logger logger = LogManager.getLogger(KvmHardwareDiscoveryAdapter.class);

    @Override
    public String getDiscoverSource() {
        return "KVM_AGENT";
    }

    @Override
    public boolean discover(PhysicalServerVO server, HardwareInfoCarrier carrier) {
        // P1-2 (ZSTAC-84191): single PSR query per pass. Resolve hostUuid once
        // and short-circuit when the KVM_HOST role is absent — the orchestrator's
        // hasActiveRole pre-check is gone, so this method is the sole gate.
        if (server == null || server.getUuid() == null || carrier == null) {
            return false;
        }
        String hostUuid = resolveHostUuid(server.getUuid());
        if (hostUuid == null) {
            // Not applicable: no KVM_HOST role for this server.
            return false;
        }

        HostVO host = Q.New(HostVO.class).eq(HostVO_.uuid, hostUuid).find();
        if (host == null) {
            // Transient: PhysicalServerRoleVO row exists but HostVO is gone (mid-cascade-delete).
            logger.warn(String.format("[KvmHardwareDiscoveryAdapter] HostVO[uuid:%s] not found for " +
                    "PhysicalServer[uuid:%s]; skipping KVM discovery.", hostUuid, server.getUuid()));
            // Still applicable (role exists); we just have no fields to contribute right now.
            return true;
        }

        carrier.setCpuArchitecture(host.getArchitecture());

        HostCapacityVO hcv = Q.New(HostCapacityVO.class).eq(HostCapacityVO_.uuid, hostUuid).find();
        if (hcv != null) {
            if (hcv.getCpuSockets() > 0) {
                carrier.setCpuSockets(hcv.getCpuSockets());
            }
            if (hcv.getCpuNum() > 0) {
                carrier.setCpuCores(hcv.getCpuNum());
            }
            if (hcv.getTotalPhysicalMemory() > 0) {
                carrier.setTotalMemoryBytes(hcv.getTotalPhysicalMemory());
            }
        }

        carrier.setManufacturer(readTag(hostUuid, HostSystemTags.SYSTEM_MANUFACTURER, HostSystemTags.SYSTEM_MANUFACTURER_TOKEN));
        carrier.setModel(readTag(hostUuid, HostSystemTags.SYSTEM_PRODUCT_NAME, HostSystemTags.SYSTEM_PRODUCT_NAME_TOKEN));
        carrier.setSerialNumber(readTag(hostUuid, HostSystemTags.SYSTEM_SERIAL_NUMBER, HostSystemTags.SYSTEM_SERIAL_NUMBER_TOKEN));
        carrier.setBiosVersion(readTag(hostUuid, HostSystemTags.BIOS_VERSION, HostSystemTags.BIOS_VERSION_TOKEN));
        carrier.setCpuModel(readTag(hostUuid, HostSystemTags.HOST_CPU_MODEL_NAME, HostSystemTags.HOST_CPU_MODEL_NAME_TOKEN));
        return true;
    }

    private String resolveHostUuid(String serverUuid) {
        return Q.New(PhysicalServerRoleVO.class)
                .eq(PhysicalServerRoleVO_.serverUuid, serverUuid)
                .eq(PhysicalServerRoleVO_.roleType, ServerRoleType.KVM_HOST.toString())
                .select(PhysicalServerRoleVO_.roleUuid)
                .findValue();
    }

    private String readTag(String hostUuid, PatternedSystemTag tag, String token) {
        String v = tag.getTokenByResourceUuid(hostUuid, token);
        return (v == null || v.isEmpty()) ? null : v;
    }
}
