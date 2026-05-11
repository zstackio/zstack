package org.zstack.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.cluster.ClusterAO_;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.server.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Three-tier fallback matching for FR-027 auto-association.
 * 1. serialNumber (primary key match)
 * 2. oobAddress + zoneUuid (BM scenario fallback)
 * 3. managementIp + zoneUuid (final fallback)
 *
 * If no match found, auto-creates a new PhysicalServerVO (requires serverPoolUuid from ClusterVO).
 */
public class PhysicalServerAutoAssociator {

    @Autowired
    private DatabaseFacade dbf;

    @Autowired(required = false)
    private PhysicalServerPowerTracker powerTracker;

    private static final Set<String> SERIAL_NUMBER_BLACKLIST = new HashSet<>(Arrays.asList(
            "", "Not Specified", "To Be Filled", "Default string", "None", "N/A"
    ));

    /**
     * Find an existing PhysicalServer by three-tier fallback, or create a new one.
     *
     * @param ctx         role match context carrying serialNumber, oobAddress, managementIp, zoneUuid
     * @param clusterUuid used to look up the associated ServerPool
     * @return the matched or newly created PhysicalServer UUID, or null if no pool is bound and creation is needed
     */
    public String findOrCreate(RoleMatchContext ctx, String clusterUuid) {
        // ZSTAC-84191: serialize find→persist for the same physical machine.
        // Without this lock, 2 concurrent path-2 flows for the same NativeHost
        // both see no existing PSVO and both create a new one with distinct uuid.
        // Downstream the same NativeHost.uuid is then bound to 2 different
        // PhysicalServerRoleVO rows (one per PSVO), and ContainerEndpointBase's
        // `eq(roleUuid, h.getUuid()).findValue()` throws NonUniqueResultException
        // at capacity-recalc-and-evaluate-cordon. Key on (managementIp, zoneUuid)
        // — the most discriminating tier of the 3-tier fallback below — so flows
        // for the same physical machine block on the same JVM monitor.
        String lockKey = ("ZSTAC-84191-auto-associate-"
                + (ctx.getManagementIp() == null ? "" : ctx.getManagementIp())
                + "-"
                + (ctx.getZoneUuid() == null ? "" : ctx.getZoneUuid())).intern();
        synchronized (lockKey) {
            return doFindOrCreate(ctx, clusterUuid);
        }
    }

    private String doFindOrCreate(RoleMatchContext ctx, String clusterUuid) {
        // Tier 1: match by serialNumber
        String sn = ctx.getSerialNumber();
        if (sn != null && !SERIAL_NUMBER_BLACKLIST.contains(sn.trim())) {
            PhysicalServerVO vo = Q.New(PhysicalServerVO.class)
                    .eq(PhysicalServerAO_.serialNumber, sn.trim())
                    .eq(PhysicalServerAO_.zoneUuid, ctx.getZoneUuid())
                    .find();
            if (vo != null) {
                return vo.getUuid();
            }
        }

        // Tier 2: match by oobAddress + zoneUuid
        String oobAddr = ctx.getOobAddress();
        if (oobAddr != null && !oobAddr.isEmpty() && ctx.getZoneUuid() != null) {
            PhysicalServerVO vo = Q.New(PhysicalServerVO.class)
                    .eq(PhysicalServerAO_.oobAddress, oobAddr)
                    .eq(PhysicalServerAO_.zoneUuid, ctx.getZoneUuid())
                    .find();
            if (vo != null) {
                return vo.getUuid();
            }
        }

        // Tier 3: match by managementIp + zoneUuid
        String mgmtIp = ctx.getManagementIp();
        if (mgmtIp != null && !mgmtIp.isEmpty() && ctx.getZoneUuid() != null) {
            PhysicalServerVO vo = Q.New(PhysicalServerVO.class)
                    .eq(PhysicalServerAO_.managementIp, mgmtIp)
                    .eq(PhysicalServerAO_.zoneUuid, ctx.getZoneUuid())
                    .find();
            if (vo != null) {
                return vo.getUuid();
            }
        }

        // No match — auto-create if pool is available
        String poolUuid = Q.New(ClusterVO.class)
                .eq(ClusterAO_.uuid, clusterUuid)
                .select(ClusterAO_.serverPoolUuid)
                .findValue();

        if (poolUuid == null) {
            return null;
        }

        PhysicalServerVO vo = new PhysicalServerVO();
        vo.setUuid(Platform.getUuid());
        vo.setName("auto-" + ctx.getManagementIp());
        vo.setZoneUuid(ctx.getZoneUuid());
        vo.setPoolUuid(poolUuid);
        vo.setManagementIp(ctx.getManagementIp());
        vo.setSerialNumber(ctx.getSerialNumber());
        vo.setOobAddress(ctx.getOobAddress());
        vo.setState(PhysicalServerState.Enabled);
        vo.setPowerStatus(PhysicalServerPowerStatus.POWER_UNKNOWN);
        dbf.persistAndRefresh(vo);
        if (powerTracker != null) {
            powerTracker.track(vo.getUuid());
        }
        return vo.getUuid();
    }
}
