package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.storage.primary.PrimaryStorageAllocationSpec;
import org.zstack.header.storage.primary.PrimaryStorageConstant.AllocatorParams;
import org.zstack.header.storage.primary.PrimaryStorageState;
import org.zstack.header.storage.primary.PrimaryStorageStatus;
import org.zstack.header.storage.primary.PrimaryStorageVO;

import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Map;

/**
 * Created by frank on 7/1/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LocalStorageMainAllocatorFlow extends NoRollbackFlow {
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
        String errorInfo;

        if (spec.getRequiredPrimaryStorageUuid() != null) {
            String sql = "select pri from PrimaryStorageVO pri, LocalStorageHostRefVO ref, HostVO host where ref.primaryStorageUuid = pri.uuid and pri.state = :state and pri.status = :status and ref.availableCapacity > :size and pri.uuid = :uuid and host.uuid = ref.hostUuid and host.state = :hstate and host.status = :hstatus and pri.type = :ptype";
            query = dbf.getEntityManager().createQuery(sql, PrimaryStorageVO.class);
            query.setParameter("state", PrimaryStorageState.Enabled);
            query.setParameter("status", PrimaryStorageStatus.Connected);
            query.setParameter("size", spec.getSize());
            query.setParameter("uuid", spec.getRequiredPrimaryStorageUuid());
            query.setParameter("hstate", HostState.Enabled);
            query.setParameter("hstatus", HostStatus.Connected);
            query.setParameter("ptype", LocalStorageConstants.LOCAL_STORAGE_TYPE);
            errorInfo = String.format("required local primary storage[uuid:%s] cannot satisfy conditions[state: %s, status: %s], or hosts providing the primary storage don't satisfy conditions[state: %s, status: %s, size > %s bytes]",
                    spec.getRequiredPrimaryStorageUuid(), PrimaryStorageState.Enabled, PrimaryStorageStatus.Connected, HostState.Enabled, HostStatus.Connected, spec.getSize());
        } else if (spec.getRequiredHostUuid() != null) {
            String sql = "select pri from PrimaryStorageVO pri, PrimaryStorageClusterRefVO pref, HostVO host, LocalStorageHostRefVO lref where pri.uuid = pref.primaryStorageUuid and lref.primaryStorageUuid = pri.uuid and host.uuid = :huuid and host.uuid = lref.hostUuid and host.clusterUuid = pref.clusterUuid and pri.state = :pstate and pri.status = :pstatus and host.state = :hstate and host.status = :hstatus and lref.availableCapacity > :size and pri.type = :ptype";
            query = dbf.getEntityManager().createQuery(sql, PrimaryStorageVO.class);
            query.setParameter("huuid", spec.getRequiredHostUuid());
            query.setParameter("pstate", PrimaryStorageState.Enabled);
            query.setParameter("pstatus", PrimaryStorageStatus.Connected);
            query.setParameter("hstate", HostState.Enabled);
            query.setParameter("hstatus", HostStatus.Connected);
            query.setParameter("size", spec.getSize());
            query.setParameter("ptype", LocalStorageConstants.LOCAL_STORAGE_TYPE);
            errorInfo = String.format("the required host[uuid:%s] cannot satisfy conditions[state: %s, status: %s, size > %s bytes], or doesn't belong to a local primary storage satisfying conditions[state: %s, status: %s], or its cluster doesn't attach to any local primary storage",
                    spec.getRequiredHostUuid(), HostState.Enabled, HostStatus.Connected, spec.getSize(), PrimaryStorageState.Enabled, PrimaryStorageStatus.Connected);
        } else if (spec.getRequiredZoneUuid() != null) {
            String sql = "select pri from PrimaryStorageVO pri, LocalStorageHostRefVO ref, HostVO host where pri.uuid = ref.primaryStorageUuid and pri.state = :pstate and pri.status = :pstatus and ref.availableCapacity > :size and pri.zoneUuid = :zoneUuid and pri.type = :ptype and host.uuid = ref.hostUuid and host.state = :hstate and host.status = :hstatus";
            query = dbf.getEntityManager().createQuery(sql, PrimaryStorageVO.class);
            query.setParameter("pstate", PrimaryStorageState.Enabled);
            query.setParameter("pstatus", PrimaryStorageStatus.Connected);
            query.setParameter("size", spec.getSize());
            query.setParameter("zoneUuid", spec.getRequiredZoneUuid());
            query.setParameter("hstate", HostState.Enabled);
            query.setParameter("hstatus", HostStatus.Connected);
            query.setParameter("ptype", LocalStorageConstants.LOCAL_STORAGE_TYPE);
            errorInfo = String.format("no local primary storage in zone[uuid:%s] can satisfy conditions[state: %s, status: %s] or contain hosts satisfying conditions[state: %s, status: %s, size > %s bytes]",
                    spec.getRequiredZoneUuid(), PrimaryStorageState.Enabled, PrimaryStorageStatus.Connected, HostState.Enabled, HostStatus.Connected, spec.getSize());
        } else {
            String sql = "select pri from PrimaryStorageVO pri, LocalStorageHostRefVO ref, HostVO host where pri.uuid = ref.primaryStorageUuid and host.uuid = ref.hostUuid and ref.size > :size and pri.state = :pstate and pri.status = :pstatus and host.state = :hstate and host.status = :hstatus and pri.type = :ptype";
            query = dbf.getEntityManager().createQuery(sql, PrimaryStorageVO.class);
            query.setParameter("size", spec.getSize());
            query.setParameter("pstate", PrimaryStorageState.Enabled);
            query.setParameter("pstatus", PrimaryStorageStatus.Connected);
            query.setParameter("hstate", HostState.Enabled);
            query.setParameter("hstatus", HostStatus.Connected);
            query.setParameter("ptype", LocalStorageConstants.LOCAL_STORAGE_TYPE);

            errorInfo = String.format("no local primary storage can satisfy conditions[state: %s, status: %s] or contain hosts satisfying conditions[state: %s, status: %s, size > %s bytes]",
                    PrimaryStorageState.Enabled, PrimaryStorageStatus.Connected, HostState.Enabled, HostStatus.Connected, spec.getSize());
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
