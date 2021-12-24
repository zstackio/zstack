package org.zstack.identity;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.identity.Quota;
import org.zstack.header.identity.QuotaVO;

import java.util.*;

import static org.zstack.core.Platform.argerr;

/**
 * Created by Wenhao.Zhang on 21/12/22
 */
public class AccountQuotaUpdateChecker implements QuotaUpdateChecker {
    @Autowired
    private AccountManager accountManager;

    @Override
    public Set<String> type() {
        Set<String> set = new HashSet<>();
        set.add(AccountVO.class.getSimpleName());
        return set;
    }

    @Override
    public ErrorCode check(QuotaVO quota, long updatedValue) {
        if (updatedValue < 0) {
            return argerr("the quota[name:%s] of account[uuid:%s] can not be %d",
                    quota.getName(), quota.getIdentityUuid(), updatedValue);
        }
        return checkQuotaChangeForAccount(quota.getIdentityUuid(), quota.getName(), updatedValue);
    }

    /**
     * check:
     *   usage <= total.updatedValue
     * TODO duplicate code with APIGetAccountQuotaUsageMsg's implement refactor requested
     */
    private ErrorCode checkQuotaChangeForAccount(String accountUuid, String quotaName, long updatedValue) {
        List<Quota> quotas = accountManager.getQuotas();
        Quota.QuotaUsage usage = quotas.stream()
                .map(q -> q.getOperator().getQuotaUsageByAccount(accountUuid))
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(u -> quotaName.equals(u.getName()))
                .findAny().orElse(null);
        if (usage == null) {
            throw new CloudRuntimeException(String.format("cannot find usage[name:%s]", quotaName));
        }

        if (usage.getUsed() > updatedValue) {
            return argerr("the account[uuid:%s] used [name:%s, usedValue:%s] exceeds request quota: %d",
                    accountUuid, quotaName, usage.getUsed(), updatedValue);
        }

        return null;
    }
}
