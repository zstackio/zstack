package org.zstack.identity;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.identity.Quota;
import org.zstack.header.identity.rbac.RBACEntity;

import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class QuotaAPIRequestChecker implements APIRequestChecker {
    @Autowired
    private AccountManager acntMgr;

    @Override
    public void check(RBACEntity entity) {
        if (acntMgr.isAdmin(entity.getApiMessage().getSession())) {
            return;
        }

        List<Quota> quotas = acntMgr.getMessageQuotaMap().get(entity.getApiMessage().getClass());
        if (quotas == null || quotas.isEmpty()) {
            return;
        }

        quotas.forEach(quota -> {
            Map<String, Quota.QuotaPair> pairs = new QuotaUtil().makeQuotaPairs(entity.getApiMessage().getSession().getAccountUuid());
            quota.getOperator().checkQuota(entity.getApiMessage(), pairs);
            if (quota.getQuotaValidators() != null) {
                for (Quota.QuotaValidator q : quota.getQuotaValidators()) {
                    q.checkQuota(entity.getApiMessage(), pairs);
                }
            }
        });
    }
}
