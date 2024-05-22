package org.zstack.network.service.virtualrouter;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;

import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.vm.VmInstanceState;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;

import static org.zstack.utils.CollectionDSL.list;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouterQuotaUtil {
    @Autowired
    private DatabaseFacade dbf;

    public class VirtualRouterQuota {
        // public long totalNum;
        // public long runningNum;
        public long runningCpuNum;
        public long runningMemorySize;
    }

    @Transactional(readOnly = true)
    public VirtualRouterQuota getUsedVirtualRouterCpuMemory(String accountUuid) {
        return getUsedVirtualRouterCpuMemory(accountUuid, null);
    }

    @Transactional(readOnly = true)
    public VirtualRouterQuota getUsedVirtualRouterCpuMemory(String accountUuid, String excludeUuid) {
        VirtualRouterQuota quota = new VirtualRouterQuota();
        // get running vrouter cpu num and memory size
        String sql = "select sum(vr.cpuNum), sum(vr.memorySize)" +
                " from VirtualRouterVmVO vr, AccountResourceRefVO ref" +
                " where vr.uuid = ref.resourceUuid" +
                " and ref.accountUuid = :auuid" +
                " and not (vr.state = :starting and vr.hostUuid is null)" +
                " and vr.state not in (:states)";
        if (excludeUuid != null) {
            sql += " and vr.uuid != (:excludeUuid)";
        }

        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setParameter("auuid", accountUuid);
        q.setParameter("starting", VmInstanceState.Starting);
        q.setParameter("states", list(VmInstanceState.Stopped, VmInstanceState.Destroying,
                VmInstanceState.Destroyed, VmInstanceState.Created));

        if (excludeUuid != null) {
            q.setParameter("excludeUuid", excludeUuid);
        }

        Tuple t = q.getSingleResult();
        Long cnum = t.get(0, Long.class);
        quota.runningCpuNum = cnum == null ? 0 : cnum;
        Long msize = t.get(1, Long.class);
        quota.runningMemorySize = msize == null ? 0 : msize;

        return quota;
    }
}
