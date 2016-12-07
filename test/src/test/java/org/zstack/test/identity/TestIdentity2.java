package org.zstack.test.identity;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.*;
import org.zstack.test.*;
import org.zstack.test.search.QueryTestValidator;

/**
 * 1. create an account
 * 2. create a user
 * <p>
 * confirm the user created successfully
 */
public class TestIdentity2 {
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        api = new Api();
        api.startServer();
    }

    @Test
    public void test() throws ApiSenderException {
        IdentityCreator creator = new IdentityCreator(api);
        AccountInventory a = creator.createAccount("test", "test");
        UserInventory u = creator.createUser("test", "test");
        Assert.assertTrue(dbf.isExist(u.getUuid(), UserVO.class));
        Assert.assertEquals(a.getUuid(), u.getAccountUuid());

        QueryTestValidator.validateEQ(new APIQueryUserMsg(), api, APIQueryUserReply.class, u);
        QueryTestValidator.validateRandomEQConjunction(new APIQueryUserMsg(), api, APIQueryUserReply.class, u, 3);

        api.loginByUserAccountName("test", "test", "test");
    }
}
