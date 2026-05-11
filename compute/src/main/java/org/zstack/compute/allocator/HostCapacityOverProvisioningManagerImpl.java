package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.header.allocator.HostCapacityOverProvisioningManager;
import org.zstack.header.server.PhysicalServerCapacityVO;
import org.zstack.header.server.PhysicalServerCapacityVO_;
import org.zstack.header.server.PhysicalServerRoleVO;
import org.zstack.header.server.PhysicalServerRoleVO_;
import org.zstack.header.server.ServerRoleType;

import javax.persistence.Query;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by frank on 10/19/2015.
 */
public class HostCapacityOverProvisioningManagerImpl implements HostCapacityOverProvisioningManager {
    private double globalMemoryRatio = 1;
    private ConcurrentHashMap<String, Double> hostMemoryRatio = new ConcurrentHashMap<String, Double>();
    private GlobalConfig globalConfig;

    @Autowired
    GlobalConfigFacade gcf;

    @Autowired
    ResourceConfigFacade rcf;

    @Autowired
    DatabaseFacade dbf;

    @Override
    public void setGlobalConfig(String category, String name) {
        globalConfig = gcf.getAllConfig().get(GlobalConfig.produceIdentity(category, name));
        globalMemoryRatio = globalConfig.value(Double.class);
    }

    @Override
    public void setMemoryGlobalRatio(double ratio) {
        globalMemoryRatio = ratio;
    }

    @Override
    public double getMemoryGlobalRatio() {
        return globalMemoryRatio;
    }

    @Override
    public void setMemoryRatio(String hostUuid, double ratio) {
        hostMemoryRatio.put(hostUuid, ratio);
        updateHostMemoryRatioByUuid(hostUuid, ratio);
    }

    @Override
    public void deleteMemoryRatio(String hostUuid) {
        hostMemoryRatio.remove(hostUuid);
        updateHostMemoryRatioByUuid(hostUuid, getMemoryGlobalRatio());
    }

    @Transactional
    private void updateHostMemoryRatioByUuid(String hostUuid, double ratio) {
        // P0-1: write PSC column inline so the U12 read tier sees the same value as the in-memory cache
        String serverUuid = Q.New(PhysicalServerRoleVO.class)
                .eq(PhysicalServerRoleVO_.roleUuid, hostUuid)
                .eq(PhysicalServerRoleVO_.roleType, ServerRoleType.KVM_HOST.toString())
                .select(PhysicalServerRoleVO_.serverUuid)
                .findValue();
        if (serverUuid == null) {
            return;  // not a KVM host (BM2/Container) — no PSC override to write
        }
        String sql = String.format(
                "update PhysicalServerCapacityVO cap"
                        + " set cap.memoryOverprovisioningRatio = %s"
                        + " where cap.uuid = :suuid",
                ratio);
        Query q = dbf.getEntityManager().createQuery(sql);
        q.setParameter("suuid", serverUuid);
        q.executeUpdate();
    }

    @Override
    public double getMemoryRatio(String hostUuid) {
        Double ratio =  hostMemoryRatio.get(hostUuid);
        if (ratio != null) {
            return ratio;
        }
        // AC-CM-11: per-server PSC override before falling back to ResourceConfig default.
        // The unwritten default (1.0f) is treated as "no override" — fall through. <=0 also
        // unsafe (zero ratio would break VM placement).
        Float pscRatio = readPscMemoryRatio(hostUuid);
        if (pscRatio != null && pscRatio > 1.0f) {
            return pscRatio.doubleValue();
        }
        if (globalConfig != null) {
            return rcf.getResourceConfigValue(globalConfig, hostUuid, Double.class);
        } else {
            return globalMemoryRatio;
        }
    }

    private Float readPscMemoryRatio(String hostUuid) {
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
                .select(PhysicalServerCapacityVO_.memoryOverprovisioningRatio)
                .findValue();
    }

    @Override
    public Map<String, Double> getAllMemoryRatio() {
        return hostMemoryRatio;
    }

    @Override
    public long calculateMemoryByRatio(String hostUuid, long capacity) {
        double ratio = getMemoryRatio(hostUuid);
        return Math.round(capacity / ratio);
    }

    @Override
    public long calculateHostAvailableMemoryByRatio(String hostUuid, long capacity) {
        double ratio = getMemoryRatio(hostUuid);
        return Math.round(capacity * ratio);
    }
}
