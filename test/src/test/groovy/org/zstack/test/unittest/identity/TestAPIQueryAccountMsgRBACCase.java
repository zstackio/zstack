package org.zstack.test.unittest.identity;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.header.identity.APIQueryAccountMsg;
import org.zstack.header.identity.RBACInfo;
import org.zstack.header.identity.rbac.RBAC;

public class TestAPIQueryAccountMsgRBACCase {

    @Before
    public void setUp() {
        RBAC.permissions.clear();
    }

    @Test
    public void testAPIQueryAccountMsgIsAdminOnly() {
        RBACInfo rbacInfo = new RBACInfo();
        rbacInfo.permissions();

        boolean isAdminOnly = RBAC.isAdminOnlyAPI(APIQueryAccountMsg.class.getName());
        Assert.assertTrue(
                "APIQueryAccountMsg should be admin-only to prevent privilege escalation, " +
                "but it is currently accessible to normal IAM users via the wildcard normalAPIs pattern",
                isAdminOnly
        );
    }

    @Test
    public void testAPIQueryAccountMsgNotInNormalAPIs() {
        RBACInfo rbacInfo = new RBACInfo();
        rbacInfo.permissions();

        String apiName = APIQueryAccountMsg.class.getName();
        boolean isInNormalOnly = RBAC.permissions.stream()
                .anyMatch(p -> p.getNormalAPIs().contains(apiName) && !p.getAdminOnlyAPIs().contains(apiName));
        Assert.assertFalse(
                "APIQueryAccountMsg should NOT be accessible as a normal API",
                isInNormalOnly
        );
    }
}
