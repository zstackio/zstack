package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.host.HostGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.allocator.HostCpuOverProvisioningManager;
import org.zstack.header.host.RecalculateHostCapacityMsg;
import org.zstack.header.server.PhysicalServerCapacityVO;
import org.zstack.header.server.PhysicalServerCapacityVO_;
import org.zstack.header.server.PhysicalServerRoleVO;
import org.zstack.header.server.PhysicalServerRoleVO_;
import org.zstack.header.server.ServerRoleType;
import org.zstack.header.zone.ZoneVO;
import org.zstack.header.zone.ZoneVO_;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import javax.persistence.Query;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by xing5 on 2016/5/12.
 */
public class HostCpuOverProvisioningManagerImpl implements HostCpuOverProvisioningManager {
    private Integer globalRatio;
    private ConcurrentHashMap<String, Integer> ratios = new ConcurrentHashMap<String, Integer>();

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ResourceConfigFacade rcf;

    @Override
    public void setGlobalRatio(int ratio) {
        globalRatio = ratio;

        updateHostsCpuCapacity(ratio);
        recalculateAllHostCapacity();
    }

    private void recalculateAllHostCapacity() {
        SimpleQuery<ZoneVO> q = dbf.createQuery(ZoneVO.class);
        q.select(ZoneVO_.uuid);
        List<String> zuuids = q.listValue();
        if (zuuids.isEmpty()) {
            return;
        }

        List<RecalculateHostCapacityMsg> rmsgs = CollectionUtils.transformToList(zuuids, new Function<RecalculateHostCapacityMsg, String>() {
            @Override
            public RecalculateHostCapacityMsg call(String arg) {
                RecalculateHostCapacityMsg msg = new RecalculateHostCapacityMsg();
                msg.setZoneUuid(arg);
                bus.makeLocalServiceId(msg, HostAllocatorConstant.SERVICE_ID);
                return msg;
            }
        });

        bus.send(rmsgs);
    }

    @Transactional
    private void updateHostsCpuCapacity(int ratio) {
        // W4/W5 (capacity PRD §2.1, 2026-04-22 U5): write path redirected to
        // PhysicalServerCapacityVO truth table. hostUuid keys in `ratios` map translated to
        // serverUuid via PhysicalServerRoleVO subquery. roleType filter scopes the bulk update
        // to KVM_HOST rows only — BM2 (INTERNAL_EXCLUSIVE) and Container (EXTERNAL_READONLY)
        // have different capacity semantics and MUST NOT be touched by KVM CPU overprovisioning.
        if (ratios.isEmpty()) {
            String sql = String.format(
                    "update PhysicalServerCapacityVO cap set cap.totalCpu = cap.cpuNum * %s"
                            + " where cap.uuid in (select r.serverUuid from PhysicalServerRoleVO r"
                            + " where r.roleType = 'KVM_HOST')",
                    ratio);
            Query q = dbf.getEntityManager().createQuery(sql);
            q.executeUpdate();
        } else {
            String sql = String.format(
                    "update PhysicalServerCapacityVO cap set cap.totalCpu = cap.cpuNum * %s"
                            + " where cap.uuid in (select r.serverUuid from PhysicalServerRoleVO r"
                            + " where r.roleType = 'KVM_HOST' and r.roleUuid not in (:uuids))",
                    ratio);
            Query q = dbf.getEntityManager().createQuery(sql);
            q.setParameter("uuids", ratios.keySet());
            q.executeUpdate();
        }
    }

    @Override
    public int getGlobalRatio() {
        return globalRatio == null ? HostGlobalConfig.HOST_CPU_OVER_PROVISIONING_RATIO.value(Integer.class) : globalRatio;
    }

    @Override
    public void setRatio(String hostUuid, int ratio) {
        ratios.put(hostUuid, ratio);
        updateHostCpuCapacityByUuid(hostUuid, ratio);
        recalculateHostCapacityByUuid(hostUuid);
    }

    @Transactional
    private void updateHostCpuCapacityByUuid(String hostUuid, int ratio) {
        // W6 (capacity PRD §2.1, 2026-04-22 U5): single-host update. Fail-loud via
        // HostCapacityUpdater.resolveServerUuidOrThrow per NB-24 — orphan hostUuid (no KVM_HOST
        // PhysicalServerRoleVO) surfaces FlowChain timing bugs instead of silently no-op'ing.
        String serverUuid = HostCapacityUpdater.resolveServerUuidOrThrow(hostUuid);
        // P0-1: write PSC column inline so the U12 read tier sees the same value as the in-memory cache
        String sql = String.format(
                "update PhysicalServerCapacityVO cap"
                        + " set cap.totalCpu = cap.cpuNum * %s,"
                        + " cap.cpuOverprovisioningRatio = %s"
                        + " where cap.uuid = :suuid",
                ratio, ratio);
        Query q = dbf.getEntityManager().createQuery(sql);
        q.setParameter("suuid", serverUuid);
        q.executeUpdate();
    }

    @Override
    public void deleteRatio(String hostUuid) {
        ratios.remove(hostUuid);
        updateHostCpuCapacityByUuid(hostUuid, getGlobalRatio());
        recalculateHostCapacityByUuid(hostUuid);
    }

    @Override
    public void refreshHostCpuCapacity(String hostUuid, int ratio) {
        // ResourceConfig hierarchy listeners call this to push an effective ratio onto PSC.totalCpu
        // without populating the in-memory ratios cache (which is reserved for explicit per-host
        // setRatio API calls). getRatio() therefore continues to walk the ResourceConfig stack
        // for hierarchy resolution.
        updateHostCpuCapacityByUuid(hostUuid, ratio);
        recalculateHostCapacityByUuid(hostUuid);
    }

    private void recalculateHostCapacityByUuid(String hostUuid) {
        RecalculateHostCapacityMsg msg = new RecalculateHostCapacityMsg();
        msg.setHostUuid(hostUuid);
        bus.makeLocalServiceId(msg, HostAllocatorConstant.SERVICE_ID);
        bus.send(msg);
    }

    @Override
    public int getRatio(String hostUuid) {
        Integer r = ratios.get(hostUuid);
        if (r != null) {
            return r;
        }
        // AC-CM-11: per-server PSC override before falling back to ResourceConfig default.
        // The unwritten default (1.0f) is treated as "no override" — fall through. <=0 is also
        // unsafe and falls through (zero ratio would break VM placement, see U12 spec).
        Float pscRatio = readPscCpuRatio(hostUuid);
        if (pscRatio != null && pscRatio > 1.0f) {
            return Math.round(pscRatio);
        }
        // TODO: init from db, not get from db every time.
        return rcf.getResourceConfigValue(HostGlobalConfig.HOST_CPU_OVER_PROVISIONING_RATIO, hostUuid, Integer.class);
    }

    private Float readPscCpuRatio(String hostUuid) {
        String serverUuid = Q.New(PhysicalServerRoleVO.class)
                .eq(PhysicalServerRoleVO_.roleUuid, hostUuid)
                .eq(PhysicalServerRoleVO_.roleType, ServerRoleType.KVM_HOST.toString())
                .select(PhysicalServerRoleVO_.serverUuid)
                .findValue();
        if (serverUuid == null) {
            return null;
        }
        return Q.New(PhysicalServerCapacityVO.class)
                .eq(PhysicalServerCapacityVO_.uuid, serverUuid)
                .select(PhysicalServerCapacityVO_.cpuOverprovisioningRatio)
                .findValue();
    }

    @Override
    public Map<String, Integer> getAllRatio() {
        return ratios;
    }

    @Override
    public int calculateByRatio(String hostUuid, int cpuNum) {
        int r = getRatio(hostUuid);
        int ret = Math.round((float)cpuNum / r);
        return ret == 0 ? 1 : ret;
    }

    @Override
    public int calculateHostCpuByRatio(String hostUuid, int cpuNum) {
        int r = getRatio(hostUuid);
        return cpuNum * r;
    }
}
