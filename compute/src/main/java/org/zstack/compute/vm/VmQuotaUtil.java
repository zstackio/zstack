package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by miao on 16-10-9.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmQuotaUtil {
    @Autowired
    private DatabaseFacade dbf;

    public class VmQuota {
        public long totalVmNum;
        public long runningVmNum;
        public long runningVmCpuNum;
        public long runningVmMemorySize;
    }

    @Transactional(readOnly = true)
    public long getUsedDataVolumeCount(String accountUuid) {
        String sql = "select count(vol)" +
                " from VolumeVO vol, AccountResourceRefVO ref " +
                " where vol.type = :vtype" +
                " and ref.resourceUuid = vol.uuid " +
                " and ref.accountUuid = :auuid" +
                " and ref.resourceType = :rtype" +
                " and vol.status != :status ";
        TypedQuery<Tuple> volq = dbf.getEntityManager().createQuery(sql, Tuple.class);
        volq.setParameter("auuid", accountUuid);
        volq.setParameter("rtype", VolumeVO.class.getSimpleName());
        volq.setParameter("vtype", VolumeType.Data);
        volq.setParameter("status", VolumeStatus.Deleted);
        Long n = volq.getSingleResult().get(0, Long.class);
        n = n == null ? 0 : n;
        return n;
    }

    @Transactional(readOnly = true)
    public long getUsedAllVolumeSize(String accountUuid) {
        String sql = "select sum(vol.size)" +
                " from VolumeVO vol, AccountResourceRefVO ref" +
                " where ref.resourceUuid = vol.uuid" +
                " and ref.accountUuid = :auuid" +
                " and ref.resourceType = :rtype";
        TypedQuery<Long> vq = dbf.getEntityManager().createQuery(sql, Long.class);
        vq.setParameter("auuid", accountUuid);
        vq.setParameter("rtype", VolumeVO.class.getSimpleName());
        Long vsize = vq.getSingleResult();
        vsize = vsize == null ? 0 : vsize;
        return vsize;
    }


    @Transactional(readOnly = true)
    public VmQuota getUsedVmCpuMemory(String accountUUid) {
        VmQuota quota = new VmQuota();
        // get running info
        String sql = "select count(vm), sum(vm.cpuNum), sum(vm.memorySize)" +
                " from VmInstanceVO vm, AccountResourceRefVO ref" +
                " where vm.uuid = ref.resourceUuid" +
                " and ref.accountUuid = :auuid" +
                " and ref.resourceType = :rtype" +
                " and vm.state not in (:states)";
        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setParameter("auuid", accountUUid);
        q.setParameter("rtype", VmInstanceVO.class.getSimpleName());
        q.setParameter("states", list(VmInstanceState.Stopped, VmInstanceState.Destroying,
                VmInstanceState.Destroyed, VmInstanceState.Created));
        Tuple t = q.getSingleResult();
        Long vnum = t.get(0, Long.class);
        quota.runningVmNum = vnum == null ? 0 : vnum;
        Long cnum = t.get(1, Long.class);
        quota.runningVmCpuNum = cnum == null ? 0 : cnum;
        Long msize = t.get(2, Long.class);
        quota.runningVmMemorySize = msize == null ? 0 : msize;
        // get total vm
        String sql2 = "select count(vm)" +
                " from VmInstanceVO vm, AccountResourceRefVO ref" +
                " where vm.uuid = ref.resourceUuid" +
                " and ref.accountUuid = :auuid" +
                " and ref.resourceType = :rtype" +
                " and vm.state not in (:states)";
        TypedQuery<Long> q2 = dbf.getEntityManager().createQuery(sql2, Long.class);
        q2.setParameter("auuid", accountUUid);
        q2.setParameter("rtype", VmInstanceVO.class.getSimpleName());
        q2.setParameter("states", list(VmInstanceState.Destroyed));
        Long totalVmNum = q2.getSingleResult();
        quota.totalVmNum = totalVmNum == null ? 0 : totalVmNum;

        return quota;
    }

    @Transactional(readOnly = true)
    public long getVmInstanceRootVolumeSize(String vmInstanceUuid) {
        SimpleQuery<VolumeVO> sq = dbf.createQuery(VolumeVO.class);
        sq.select(VolumeVO_.size);
        sq.add(VolumeVO_.type, SimpleQuery.Op.EQ, VolumeType.Root);
        sq.add(VolumeVO_.vmInstanceUuid, SimpleQuery.Op.EQ, vmInstanceUuid);
        Long rootVolumeSize = sq.findValue();
        rootVolumeSize = rootVolumeSize == null ? 0 : rootVolumeSize;
        return rootVolumeSize;
    }
}
