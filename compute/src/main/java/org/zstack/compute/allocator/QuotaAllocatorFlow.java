package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.vm.VmQuotaOperator;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.identity.AccountConstant;
import org.zstack.identity.Account;
import org.zstack.identity.QuotaUtil;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class QuotaAllocatorFlow extends AbstractHostAllocatorFlow {
    @Override
    public void allocate() {
        throwExceptionIfIAmTheFirstFlow();

        final String vmInstanceUuid = spec.getVmInstance().getUuid();
        final String accountUuid = Account.getAccountUuidOfResource(vmInstanceUuid);
        if (accountUuid == null || AccountConstant.isAdminPermission(accountUuid)) {
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
