package org.zstack.test;

import junit.framework.Assert;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AccountReferenceValidator {
    @Autowired
    private DatabaseFacade dbf;

    public void hasReference(String resourceUuid, Class resourceType) {
        SimpleQuery<AccountResourceRefVO> q = dbf.createQuery(AccountResourceRefVO.class);
        q.add(AccountResourceRefVO_.resourceUuid, Op.EQ, resourceUuid);
        q.add(AccountResourceRefVO_.resourceType, Op.EQ, resourceType.getSimpleName());
        boolean has = q.isExists();
        Assert.assertTrue(String.format("no AccountResourceRefVO found for %s[uuid:%s]", resourceType.getName(), resourceUuid), has);
    }

    public void noReference(String resourceUuid, Class resourceType) {
        SimpleQuery<AccountResourceRefVO> q = dbf.createQuery(AccountResourceRefVO.class);
        q.add(AccountResourceRefVO_.resourceUuid, Op.EQ, resourceUuid);
        q.add(AccountResourceRefVO_.resourceType, Op.EQ, resourceType.getSimpleName());
        boolean has = q.isExists();
        Assert.assertFalse(String.format("AccountResourceRefVO found for %s[uuid:%s], expect none", resourceType.getName(), resourceUuid), has);
    }

    public void noReference(Class resourceType) {
        SimpleQuery<AccountResourceRefVO> q = dbf.createQuery(AccountResourceRefVO.class);
        q.add(AccountResourceRefVO_.resourceType, Op.EQ, resourceType.getSimpleName());
        boolean has = q.isExists();
        Assert.assertFalse(String.format("AccountResourceRefVO found for %s, expect none", resourceType.getName()), has);
    }
}
