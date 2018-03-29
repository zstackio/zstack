package org.zstack.core.aspect;

import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.vo.ResourceVO;

public aspect OwnedByAccountAspect {
    Object around(org.zstack.header.identity.OwnedByAccount oa) : this(oa) && execution(String org.zstack.header.identity.OwnedByAccount+.getAccountUuid()) {
        Object accountUuid = oa.getAccountUuid();

        if (accountUuid == null) {
            accountUuid = Q.New(AccountResourceRefVO.class).select(AccountResourceRefVO_.accountUuid)
                .eq(AccountResourceRefVO_.resourceUuid, ((ResourceVO)oa).getUuid()).findValue();
            oa.setAccountUuid(accountUuid);
        }

        return accountUuid;
    }
}