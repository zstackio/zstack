package org.zstack.compute.allocator;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
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
    private List<HostVO> allocate(String zoneUuid, List<String> clusterUuids, String hostUuid, String hypervisorType) {
        StringBuilder sql = new StringBuilder();
        sql.append("select h from HostVO h where ");
        if (zoneUuid != null) {
            sql.append(String.format("h.zoneUuid = '%s' and ", zoneUuid));
        }
        if (!CollectionUtils.isEmpty(clusterUuids)) {
            sql.append(String.format("h.clusterUuid in ('%s') and ", String.join("','", clusterUuids)));
        }
        if (hostUuid != null) {
            sql.append(String.format("h.uuid = '%s' and ", hostUuid));
        }
        if (hypervisorType != null) {
            sql.append(String.format("h.hypervisorType = '%s' and ", hypervisorType));
        }
        sql.append(String.format("h.status = '%s' and h.state = '%s'", HostStatus.Connected, HostState.Enabled));
        logger.debug("DesignatedHostAllocatorFlow sql: " + sql);
        TypedQuery<HostVO> query = dbf.getEntityManager().createQuery(sql.toString(), HostVO.class);

        if (usePagination()) {
            query.setFirstResult(paginationInfo.getOffset());
            query.setMaxResults(paginationInfo.getLimit());
        }

        return query.getResultList();
    }
    
    
    private List<HostVO> allocate(List<HostVO> candidates, String zoneUuid,  List<String> clusterUuids, String hostUuid, String hypervisorType) {
        List<HostVO> ret = new ArrayList<HostVO>(candidates.size());
        for (HostVO h : candidates) {
            if (zoneUuid != null && !h.getZoneUuid().equals(zoneUuid)) {
                continue;
            }
            if (!CollectionUtils.isEmpty(clusterUuids) && !clusterUuids.contains(h.getClusterUuid())) {
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
        List<String> clusterUuids = (List<String>) spec.getExtraData().get(HostAllocatorConstant.LocationSelector.cluster);
        String hostUuid = (String) spec.getExtraData().get(HostAllocatorConstant.LocationSelector.host);

        if (zoneUuid == null && CollectionUtils.isEmpty(clusterUuids) && hostUuid == null && spec.getHypervisorType() == null) {
            next(candidates);
            return;
        }

        if (amITheFirstFlow()) {
            candidates = allocate(zoneUuid, clusterUuids, hostUuid, spec.getHypervisorType());
        } else {
            candidates = allocate(candidates, zoneUuid, clusterUuids, hostUuid, spec.getHypervisorType());
        }

        if (candidates.isEmpty()) {
            StringBuilder args = new StringBuilder();
            if (zoneUuid != null) {
                args.append(String.format("zoneUuid=%s", zoneUuid)).append(" ");
            }
            if (!clusterUuids.isEmpty()) {
                args.append(String.format("clusterUuid in %s", clusterUuids)).append(" ");
            }
            if (hostUuid != null) {
                args.append(String.format("hostUuid=%s", hostUuid)).append(" ");
            }
            if (spec.getHypervisorType() != null) {
                args.append(String.format("hypervisorType=%s", spec.getHypervisorType())).append(" ");
            }
            fail(Platform.operr("No host with %s found", args));
        } else {
            next(candidates);
        }
    }
}
