package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DesignatedHostAllocatorFlow extends AbstractHostAllocatorFlow {
    private static final CLogger logger = Utils.getLogger(DesignatedHostAllocatorFlow.class);

    @Autowired
    private DatabaseFacade dbf;

    @Transactional(readOnly = true)
    private List<HostVO> allocate(String zoneUuid, String clusterUuid, String hostUuid, String hypervisorType) {
        StringBuilder sql = new StringBuilder();
        sql.append("select h from HostVO h where ");
        if (zoneUuid != null) {
            sql.append(String.format("h.zoneUuid = '%s' and ", zoneUuid));
        }
        if (clusterUuid != null) {
            sql.append(String.format("h.clusterUuid = '%s' and ", clusterUuid));
        }
        if (hostUuid != null) {
            sql.append(String.format("h.uuid = '%s' and ", hostUuid));
        }
        if (hypervisorType != null) {
            sql.append(String.format("h.hypervisorType = '%s' and ", hypervisorType));
        }
        sql.append(String.format("h.status = '%s' and h.state = '%s'", HostStatus.Connected, HostState.Enabled));
        TypedQuery<HostVO> query = dbf.getEntityManager().createQuery(sql.toString(), HostVO.class);

        if (usePagination()) {
            query.setFirstResult(paginationInfo.getOffset());
            query.setMaxResults(paginationInfo.getLimit());
        }

        return query.getResultList();
    }
    
    
    private List<HostVO> allocate(List<HostVO> candidates, String zoneUuid, String clusterUuid, String hostUuid, String hypervisorType) {
        List<HostVO> ret = new ArrayList<HostVO>(candidates.size());
        for (HostVO h : candidates) {
            if (zoneUuid != null && !h.getZoneUuid().equals(zoneUuid)) {
                continue;
            }
            if (clusterUuid != null && !h.getClusterUuid().equals(clusterUuid)) {
                continue;
            }
            if (hostUuid != null && !h.getUuid().equals(hostUuid)) {
                continue;
            }
            if (hypervisorType != null && !h.getHypervisorType().equals(hypervisorType)) {
                continue;
            }
            ret.add(h);
        }
        return ret;
    }

    @Override
    public void allocate() {
        String zoneUuid = (String) spec.getExtraData().get(HostAllocatorConstant.LocationSelector.zone);
        String clusterUuid = (String) spec.getExtraData().get(HostAllocatorConstant.LocationSelector.cluster);
        String hostUuid = (String) spec.getExtraData().get(HostAllocatorConstant.LocationSelector.host);

        if (zoneUuid == null && clusterUuid == null && hostUuid == null && spec.getHypervisorType() == null) {
            next(candidates);
            return;
        }

        if (amITheFirstFlow()) {
            candidates = allocate(zoneUuid, clusterUuid, hostUuid, spec.getHypervisorType());
        } else {
            candidates = allocate(candidates, zoneUuid, clusterUuid, hostUuid, spec.getHypervisorType());
        }

        if (candidates.isEmpty()) {
            StringBuilder err = new StringBuilder("No host with ");
            if (zoneUuid != null) {
                err.append(String.format("zoneUuid=%s ", zoneUuid));
            }
            if (clusterUuid != null) {
                err.append(String.format("clusterUuid=%s ", clusterUuid));
            }
            if (hostUuid != null) {
                err.append(String.format("uuid=%s ", hostUuid));
            }
            if (spec.getHypervisorType() != null) {
                err.append(String.format("hypervisorType=%s ", spec.getHypervisorType()));
            }
            err.append("found");
            fail(err.toString());
        } else {
            next(candidates);
        }
    }
}
