package org.zstack.compute.allocator;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
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
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;
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

        String hypervisorType = spec.getHypervisorType();

        // normalize empty strings to null — treat empty string as "not specified"
        zoneUuid = StringUtils.isEmpty(zoneUuid) ? null : zoneUuid;
        hostUuid = StringUtils.isEmpty(hostUuid) ? null : hostUuid;
        hypervisorType = StringUtils.isEmpty(hypervisorType) ? null : hypervisorType;
        if (!CollectionUtils.isEmpty(clusterUuids)) {
            clusterUuids = new ArrayList<>(clusterUuids);
            clusterUuids.removeIf(s -> s == null || s.isEmpty());
            if (clusterUuids.isEmpty()) {
                clusterUuids = null;
            }
        }

        if (zoneUuid == null && CollectionUtils.isEmpty(clusterUuids) && hostUuid == null && hypervisorType == null) {
            next(candidates);
            return;
        }

        if (amITheFirstFlow()) {
            candidates = allocate(zoneUuid, clusterUuids, hostUuid, hypervisorType);
        } else {
            candidates = allocate(candidates, zoneUuid, clusterUuids, hostUuid, hypervisorType);
        }

        if (candidates.isEmpty()) {
            StringBuilder args = new StringBuilder();
            if (zoneUuid != null) {
                args.append(String.format("zoneUuid=%s", zoneUuid)).append(" ");
            }
            if (!CollectionUtils.isEmpty(clusterUuids)) {
                args.append(String.format("clusterUuid in %s", clusterUuids)).append(" ");
            }
            if (hostUuid != null) {
                args.append(String.format("hostUuid=%s", hostUuid)).append(" ");
            }
            if (hypervisorType != null) {
                args.append(String.format("hypervisorType=%s", hypervisorType)).append(" ");
            }
            fail(Platform.operr(ORG_ZSTACK_COMPUTE_ALLOCATOR_10036, "No host with %s found", args));
        } else {
            next(candidates);
        }
    }
}
