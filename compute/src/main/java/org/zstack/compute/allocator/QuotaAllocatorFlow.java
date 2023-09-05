package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.vm.VmQuotaOperator;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.AccountConstant;
import org.zstack.identity.Account;
import org.zstack.identity.QuotaUtil;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.HashSet;
import java.util.Set;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class QuotaAllocatorFlow extends AbstractHostAllocatorFlow {
    private static final CLogger logger = Utils.getLogger(QuotaAllocatorFlow.class);

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void allocate() {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            if (candidates == null || candidates.isEmpty()) {
                String sql = "select h from HostVO h where h.clusterUuid in (:cuuids)";
                Set<String> clusterUuids = new HashSet<>();
                clusterUuids.add(spec.getVmInstance().getClusterUuid());

                TypedQuery<HostVO> hq = dbf.getEntityManager().createQuery(sql, HostVO.class);
                hq.setParameter("cuuids", clusterUuids);
                if (usePagination()) {
                    hq.setFirstResult(paginationInfo.getOffset());
                    hq.setMaxResults(paginationInfo.getLimit());
                }
                candidates = hq.getResultList();
                logger.debug(String.format("vm clusteruuid:%s, candidates size:%s", spec.getVmInstance().getClusterUuid(), candidates.size()));
                next(candidates);
            }
        }

        throwExceptionIfIAmTheFirstFlow();

        final String vmInstanceUuid = spec.getVmInstance().getUuid();
        final String accountUuid = Account.getAccountUuidOfResource(vmInstanceUuid);
        if (accountUuid == null || AccountConstant.isAdminPermission(accountUuid)) {
            next(candidates);
            return;
        }

        if (!spec.isFullAllocate()) {
            new VmQuotaOperator().checkVmCupAndMemoryCapacity(accountUuid,
                    accountUuid,
                    spec.getCpuCapacity(),
                    spec.getMemoryCapacity(),
                    new QuotaUtil().makeQuotaPairs(accountUuid));

            next(candidates);
            return;
        }

        new VmQuotaOperator().checkVmInstanceQuota(
                accountUuid,
                accountUuid,
                vmInstanceUuid,
                new QuotaUtil().makeQuotaPairs(accountUuid));
        next(candidates);
    }
}
