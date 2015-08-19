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
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HostPrimaryStorageAllocatorFlow extends AbstractHostAllocatorFlow {
    @Autowired
    private DatabaseFacade dbf;

	@Transactional(readOnly = true)
	private List<HostVO> allocateFreshly() {
        String sql;
        TypedQuery<HostVO> query;
        if (spec.getVmOperation().equals(VmOperation.NewCreate.toString())) {
            sql = "select h from HostVO h where h.clusterUuid in (select pr.clusterUuid from PrimaryStorageClusterRefVO pr where pr.primaryStorageUuid in (select p.uuid from PrimaryStorageVO p where p.state = :state and p.status = :status and p.availableCapacity > :disk))";
            query = dbf.getEntityManager().createQuery(sql, HostVO.class);
            query.setParameter("disk", spec.getDiskSize());
        } else {
            sql = "select h from HostVO h where h.clusterUuid in (select pr.clusterUuid from PrimaryStorageClusterRefVO pr where pr.primaryStorageUuid in (select p.uuid from PrimaryStorageVO p where p.state = :state and p.status = :status))";
            query = dbf.getEntityManager().createQuery(sql, HostVO.class);
        }

		query.setParameter("state", PrimaryStorageState.Enabled);
        query.setParameter("status", PrimaryStorageStatus.Connected);

        if (usePagination()) {
            query.setFirstResult(paginationInfo.getOffset());
            query.setMaxResults(paginationInfo.getLimit());
        }

		return query.getResultList();
	}

    @Transactional(readOnly = true)
    private List<HostVO> allocateFromCandidates() {
        List<String> huuids = getHostUuidsFromCandidates();

        String sql;
        TypedQuery<HostVO> query;
        if (VmOperation.NewCreate.toString().equals(spec.getVmOperation())) {
            sql = "select h from HostVO h where h.uuid in :uuids and h.clusterUuid in (select pr.clusterUuid from PrimaryStorageClusterRefVO pr, PrimaryStorageVO pri, PrimaryStorageCapacityVO cap where pr.primaryStorageUuid = pri.uuid and pri.uuid = cap.uuid and pri.state = :state and pri.status = :status and cap.availableCapacity > :disk)";
            query = dbf.getEntityManager().createQuery(sql, HostVO.class);
            query.setParameter("disk", spec.getDiskSize());
        } else {
            sql = "select h from HostVO h where h.uuid in :uuids and h.clusterUuid in (select pr.clusterUuid from PrimaryStorageClusterRefVO pr, PrimaryStorageVO pri, PrimaryStorageCapacityVO cap where pr.primaryStorageUuid = pri.uuid and pri.uuid = cap.uuid and pri.state = :state and pri.status = :status)";
            query = dbf.getEntityManager().createQuery(sql, HostVO.class);
        }

        query.setParameter("uuids", huuids);
        query.setParameter("state", PrimaryStorageState.Enabled);
        query.setParameter("status", PrimaryStorageStatus.Connected);
        return query.getResultList();
    }

    @Override
    public void allocate() {
        if (amITheFirstFlow()) {
            candidates = allocateFreshly();
        } else {
            candidates = allocateFromCandidates();
        }

        if (candidates.isEmpty()) {
            fail(String.format("no host in clusters that have attached to primary storage which can provide disk capacity[%s bytes] found", spec.getDiskSize()));
        } else {
            next(candidates);
        }
    }
}
