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
        public long totalNum;
        public long runningNum;
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
        // get running info
        String sql = "select count(vr), sum(vr.cpuNum), sum(vr.memorySize)" +
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
        Long vnum = t.get(0, Long.class);
        quota.runningNum = vnum == null ? 0 : vnum;
        Long cnum = t.get(1, Long.class);
        quota.runningCpuNum = cnum == null ? 0 : cnum;
        Long msize = t.get(2, Long.class);
        quota.runningMemorySize = msize == null ? 0 : msize;
        // get total vrouter
        String sql2 = "select count(vr)" +
                " from VirtualRouterVmVO vr, AccountResourceRefVO ref" +
                " where vr.uuid = ref.resourceUuid" +
                " and ref.accountUuid = :auuid" +
                " and not (vr.hostUuid is null and vr.lastHostUuid is null)" +
                " and vr.state not in (:states)";
        TypedQuery<Long> q2 = dbf.getEntityManager().createQuery(sql2, Long.class);
        q2.setParameter("auuid", accountUuid);
        q2.setParameter("states", list(VmInstanceState.Destroyed));
        Long totalVmNum = q2.getSingleResult();
        quota.totalNum = totalVmNum == null ? 0 : totalVmNum;

        return quota;
    }
}
