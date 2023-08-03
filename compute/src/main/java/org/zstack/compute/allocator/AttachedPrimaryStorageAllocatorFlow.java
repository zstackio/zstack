package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Set;

import static org.zstack.utils.CollectionUtils.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AttachedPrimaryStorageAllocatorFlow extends AbstractHostAllocatorFlow {
    private static final CLogger logger = Utils.getLogger(AttachedPrimaryStorageAllocatorFlow.class);

    @Autowired
    private DatabaseFacade dbf;

    @Transactional(readOnly = true)
    private List<HostVO> allocate(Set<String> psUuids, VmInstanceInventory vm) {
        if (getCandidates() != null) {
            String sql = "select h from HostVO h where h.clusterUuid in (select pcr.clusterUuid from PrimaryStorageClusterRefVO pcr where pcr.primaryStorageUuid in :psUuids group by pcr.clusterUuid having count(pcr.clusterUuid) = :psUuidSize) and h.state = :hostState and h.status = :hostConnectionState and h.hypervisorType = :hypervisorType and h.uuid in :hostUuids";
            TypedQuery<HostVO> query = dbf.getEntityManager().createQuery(sql, HostVO.class);
            query.setParameter("psUuids", psUuids);
            query.setParameter("psUuidSize", psUuids.size());
            query.setParameter("hostState", HostState.Enabled);
            query.setParameter("hostConnectionState", HostStatus.Connected);
            query.setParameter("hypervisorType", vm.getHypervisorType());
            query.setParameter("hostUuids", getHostUuidsFromCandidates());

            if (usePagination()) {
                query.setFirstResult(paginationInfo.getOffset());
                query.setMaxResults(paginationInfo.getLimit());
            }

            return query.getResultList();
        } else {
            String sql = "select h from HostVO h where h.clusterUuid in (select pcr.clusterUuid from PrimaryStorageClusterRefVO pcr where pcr.primaryStorageUuid in :psUuids group by pcr.clusterUuid having count(pcr.clusterUuid) = :psUuidSize) and h.state = :hostState and h.status = :hostConnectionState and h.hypervisorType = :hypervisorType";
            TypedQuery<HostVO> query = dbf.getEntityManager().createQuery(sql, HostVO.class);
            query.setParameter("psUuids", psUuids);
            query.setParameter("psUuidSize", Long.valueOf(psUuids.size()));
            query.setParameter("hostState", HostState.Enabled);
            query.setParameter("hostConnectionState", HostStatus.Connected);
            query.setParameter("hypervisorType", vm.getHypervisorType());

            if (usePagination()) {
                query.setFirstResult(paginationInfo.getOffset());
                query.setMaxResults(paginationInfo.getLimit());
            }

            return query.getResultList();
        }
    }

    @Override
    public void allocate() {
        VmInstanceInventory vm = spec.getVmInstance();
        Set<String> psuuids = transformToSet(vm.getAllVolumes(), VolumeInventory::getPrimaryStorageUuid);
        psuuids.remove(null);
        candidates = allocate(psuuids, vm);

        if (candidates.isEmpty()) {
            fail(Platform.operr("no host found in clusters that have attached to primary storage %s", psuuids));
        }  else {
            next(candidates);
        }
    }
}
