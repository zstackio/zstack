package org.zstack.core.aspect;

import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.vo.ResourceVO;
import org.zstack.core.db.Q;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.header.identity.OwnedByAccount;
import javax.persistence.EntityManager;

public aspect OwnedByAccountAspect {
    private static final CLogger logger = Utils.getLogger(OwnedByAccountAspect.class);

    Object around(OwnedByAccount oa) : this(oa) && execution(String OwnedByAccount+.getAccountUuid()) {
        Object accountUuid = proceed(oa);

        if (accountUuid == null) {
            accountUuid = Q.New(AccountResourceRefVO.class).select(AccountResourceRefVO_.accountUuid)
                .eq(AccountResourceRefVO_.resourceUuid, ((ResourceVO)oa).getUuid()).findValue();
            oa.setAccountUuid((String)accountUuid);
        }

        return accountUuid;
    }

    after(EntityManager mgr, Object entity) : call(void EntityManager+.persist(Object))
            && target(mgr)
            && args(entity) {
        if (!(entity instanceof OwnedByAccount)) {
            return;
        }

        OwnedByAccount oa = (OwnedByAccount) entity;
        OwnedByAccountAspectHelper.createAccountResourceRefVO(oa, mgr, entity);
    }
}