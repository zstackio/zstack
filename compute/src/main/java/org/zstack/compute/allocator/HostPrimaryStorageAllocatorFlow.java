package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.allocator.*;
import org.zstack.header.host.HostVO;
import org.zstack.header.storage.primary.PrimaryStorageState;
import org.zstack.header.storage.primary.PrimaryStorageStatus;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HostPrimaryStorageAllocatorFlow extends AbstractHostAllocatorFlow {
	private static final CLogger logger = Utils.getLogger(HostPrimaryStorageAllocatorFlow.class);

    @Autowired
    private DatabaseFacade dbf;

	@Transactional(readOnly = true)
	private List<HostVO> allocate(long diskSize) {
		String sql = String
		        .format("select h from HostVO h where h.clusterUuid in (select pr.clusterUuid from PrimaryStorageClusterRefVO pr where pr.primaryStorageUuid in (select p.uuid from PrimaryStorageVO p where p.state = :state and p.status = :status and p.availableCapacity > :disk))");
		TypedQuery<HostVO> query = dbf.getEntityManager().createQuery(sql, HostVO.class);
		query.setParameter("state", PrimaryStorageState.Enabled);
        query.setParameter("status", PrimaryStorageStatus.Connected);
		query.setParameter("disk", diskSize);

        if (usePagination()) {
            query.setFirstResult(paginationInfo.getOffset());
            query.setMaxResults(paginationInfo.getLimit());
        }

		return query.getResultList();
	}

	@Transactional(readOnly = true)
	private List<HostVO> allocateFromCandidates(long diskSize) {
		List<String> huuids = getHostUuidsFromCandidates();

		if (!huuids.isEmpty()) {
            String sql = String
                    .format("select h from HostVO h where h.uuid in :uuids and h.clusterUuid in (select pr.clusterUuid from PrimaryStorageClusterRefVO pr, PrimaryStorageVO pri, PrimaryStorageCapacityVO cap where pr.primaryStorageUuid = pri.uuid and pri.uuid = cap.uuid and pri.state = :state and pri.status = :status and cap.availableCapacity > :disk)");
            TypedQuery<HostVO> query = dbf.getEntityManager().createQuery(sql, HostVO.class);
            query.setParameter("uuids", huuids);
            query.setParameter("state", PrimaryStorageState.Enabled);
            query.setParameter("status", PrimaryStorageStatus.Connected);
            query.setParameter("disk", diskSize);
            return query.getResultList();
		} else {
            String sql = String
                    .format("select h from HostVO h where h.clusterUuid in (select pr.clusterUuid from PrimaryStorageClusterRefVO pr, PrimaryStorageVO pri, PrimaryStorageCapacityVO cap where pr.primaryStorageUuid = pri.uuid and pri.uuid = cap.uuid and pri.state = :state and pri.status = :status and cap.availableCapacity > :disk)");
            TypedQuery<HostVO> query = dbf.getEntityManager().createQuery(sql, HostVO.class);
            query.setParameter("state", PrimaryStorageState.Enabled);
            query.setParameter("status", PrimaryStorageStatus.Connected);
            query.setParameter("disk", diskSize);
            return query.getResultList();
        }


	}

    @Override
    public void allocate() {
        if (amITheFirstFlow()) {
            candidates = allocate(spec.getDiskSize());
        } else {
            candidates = allocateFromCandidates(spec.getDiskSize());
        }

        if (candidates.isEmpty()) {
            fail(String.format("no host in clusters that have attached to primary storage which can provide disk capacity[%s bytes] found", spec.getDiskSize()));
        } else {
            next(candidates);
        }
    }
}
