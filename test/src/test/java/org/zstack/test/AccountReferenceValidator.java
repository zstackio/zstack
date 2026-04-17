package org.zstack.test;

import junit.framework.Assert;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AccountReferenceValidator {
    @Autowired
    private DatabaseFacade dbf;

    public void hasReference(String resourceUuid, Class resourceType) {
        boolean has = Q.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.resourceUuid, resourceUuid)
                .eq(AccountResourceRefVO_.resourceType, resourceType.getSimpleName())
                .isExists();
        Assert.assertTrue(String.format("no AccountResourceRefVO found for %s[uuid:%s]", resourceType.getName(), resourceUuid), has);
    }

    public void noReference(String resourceUuid, Class resourceType) {
        boolean has = Q.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.resourceUuid, resourceUuid)
                .eq(AccountResourceRefVO_.resourceType, resourceType.getSimpleName())
                .isExists();
        Assert.assertFalse(String.format("AccountResourceRefVO found for %s[uuid:%s], expect none", resourceType.getName(), resourceUuid), has);
    }

    public void noReference(Class resourceType) {
        boolean has = Q.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.resourceType, resourceType.getSimpleName())
                .isExists();
        Assert.assertFalse(String.format("AccountResourceRefVO found for %s, expect none", resourceType.getName()), has);
    }
}
