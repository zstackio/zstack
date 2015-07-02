package org.zstack.storage.primary;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowTrigger;
import org.zstack.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.primary.PrimaryStorageConstant.AllocatorParams;

import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PrimaryStorageMainAllocatorFlow extends NoRollbackFlow {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected ErrorFacade errf;

    private class Result {
        List<PrimaryStorageVO> result;
        String error;
    }

    @Transactional(readOnly = true)
    private Result allocate(Map data) {
        PrimaryStorageAllocationSpec spec = (PrimaryStorageAllocationSpec) data.get(AllocatorParams.SPEC);
        TypedQuery<PrimaryStorageVO> query;
        String errorInfo = null;
        if (spec.getRequiredPrimaryStorageUuid() != null) {
            String sql = "select pri from PrimaryStorageVO pri, PrimaryStorageCapacityVO cap where pri.uuid = cap.uuid and pri.state = :priState and pri.status = :status and pri.uuid = :priUuid and cap.availableCapacity > :size";
            query = dbf.getEntityManager().createQuery(sql, PrimaryStorageVO.class);
            query.setParameter("priState", PrimaryStorageState.Enabled);
            query.setParameter("status", PrimaryStorageStatus.Connected);
            query.setParameter("size", spec.getSize());
            query.setParameter("priUuid", spec.getRequiredPrimaryStorageUuid());
            errorInfo = String.format("required primary storage[uuid:%s] cannot satisfy conditions[state:%s, status:%s, size:%s]",
                    spec.getRequiredPrimaryStorageUuid(), PrimaryStorageState.Enabled, PrimaryStorageStatus.Connected, spec.getSize());
        } else if (spec.getRequiredHostUuid() != null) {
            String sql = "select pri from PrimaryStorageVO pri, PrimaryStorageClusterRefVO ref, HostVO host, PrimaryStorageCapacityVO cap where pri.uuid = cap.uuid and host.uuid = :huuid" +
                    " and host.clusterUuid = ref.clusterUuid and ref.primaryStorageUuid = pri.uuid and pri.status = :status and pri.state = :priState" +
                    " and cap.availableCapacity > :size";
            query = dbf.getEntityManager().createQuery(sql, PrimaryStorageVO.class);
            query.setParameter("huuid", spec.getRequiredHostUuid());
            query.setParameter("priState", PrimaryStorageState.Enabled);
            query.setParameter("status", PrimaryStorageStatus.Connected);
            query.setParameter("size", spec.getSize());
            errorInfo = String.format("cannot find primary storage satisfying conditions[attached to cluster having host:%s, state:%s, status: %s, available capacity > %s",
                    spec.getRequiredHostUuid(), PrimaryStorageState.Enabled, PrimaryStorageStatus.Connected, spec.getSize());
        } else if (spec.getRequiredClusterUuids() != null && !spec.getRequiredClusterUuids().isEmpty()) {
            String sql = "select pri from PrimaryStorageVO pri, PrimaryStorageClusterRefVO ref, ClusterVO cluster, PrimaryStorageCapacityVO cap where" +
                    " pri.uuid = cap.uuid and cluster.uuid = ref.clusterUuid and ref.primaryStorageUuid = pri.uuid and pri.status = :status and pri.state = :priState" +
                    " and cap.availableCapacity > :size and cluster.uuid in (:clusterUuids)";
            query = dbf.getEntityManager().createQuery(sql, PrimaryStorageVO.class);
            query.setParameter("clusterUuids", spec.getRequiredClusterUuids());
            query.setParameter("status", PrimaryStorageStatus.Connected);
            query.setParameter("priState", PrimaryStorageState.Enabled);
            query.setParameter("size", spec.getSize());
            errorInfo = String.format("cannot find primary storage satisfying conditions[attached to clusters:%s, state:%s, status:%s, available capacity > %s",
                    spec.getRequiredClusterUuids(), PrimaryStorageState.Enabled, PrimaryStorageStatus.Connected, spec.getSize());
        } else if (spec.getRequiredZoneUuid() != null) {
            String sql = "select pri from PrimaryStorageVO pri, PrimaryStorageCapacityVO cap where pri.uuid = cap.uuid and pri.zoneUuid = :zoneUuid and cap.availableCapacity > :size and pri.status = :status and pri.state = :priState";
            query = dbf.getEntityManager().createQuery(sql, PrimaryStorageVO.class);
            query.setParameter("priState", PrimaryStorageState.Enabled);
            query.setParameter("status", PrimaryStorageStatus.Connected);
            query.setParameter("size", spec.getSize());
            query.setParameter("zoneUuid", spec.getRequiredZoneUuid());
            errorInfo = String.format("cannot find primary storage satisfying conditions[in zone:%s, state:%s, status:%s, available capacity > %s",
                    spec.getRequiredZoneUuid(), PrimaryStorageState.Enabled, PrimaryStorageStatus.Connected, spec.getSize());
        } else {
            String sql = "select pri from PrimaryStorageVO pri, PrimaryStorageCapacityVO cap where pri.uuid = cap.uuid and cap.availableCapacity > :size and pri.status = :status and pri.state = :priState";
            query = dbf.getEntityManager().createQuery(sql, PrimaryStorageVO.class);
            query.setParameter("priState", PrimaryStorageState.Enabled);
            query.setParameter("status", PrimaryStorageStatus.Connected);
            query.setParameter("size", spec.getSize());
            errorInfo = String.format("cannot find primary storage satisfying conditions[state:%s, status:%s, available capacity > %s",
                    PrimaryStorageState.Enabled, PrimaryStorageStatus.Connected, spec.getSize());
        }

        List<PrimaryStorageVO> vos = query.getResultList();
        Result ret = new Result();
        ret.error = errorInfo;
        ret.result = vos;
        return ret;
    }

    @Override
    public void run(FlowTrigger trigger, Map data) {
        Result ret = allocate(data);
        if (ret.result.isEmpty()) {
            throw new OperationFailureException(errf.stringToOperationError(ret.error));
        }

        data.put(AllocatorParams.CANDIDATES, ret.result);
        trigger.next();
    }
}
