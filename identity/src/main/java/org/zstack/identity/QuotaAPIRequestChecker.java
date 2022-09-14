package org.zstack.identity;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.identity.rbac.RBACEntity;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class QuotaAPIRequestChecker implements APIRequestChecker {

    private final QuotaUtil util = new QuotaUtil();

    @Autowired
    private AccountManager acntMgr;

    @Override
    public void check(RBACEntity entity) {
        if (acntMgr.isAdmin(entity.getApiMessage().getSession())) {
            return;
        }

        util.checkQuota(entity.getApiMessage());
    }
}
