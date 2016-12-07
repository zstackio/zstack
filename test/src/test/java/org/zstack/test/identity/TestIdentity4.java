package org.zstack.test.identity;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.identity.UserInventory;
import org.zstack.header.identity.UserVO;
import org.zstack.test.*;

/**
 * 1. create an account
 * 2. create a user
 * 3. reset the password of user
 * <p>
 * confirm the password reset successfully
 */
public class TestIdentity4 {
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
        creator.resetUserPassword("test", "password");
        UserVO user = dbf.findByUuid(u.getUuid(), UserVO.class);
        Assert.assertEquals("password", user.getPassword());
        SessionInventory session = api.loginByUser("test", "password", a.getUuid());
        api.resetUserPassword(null, "new", session);
        user = dbf.findByUuid(u.getUuid(), UserVO.class);
        Assert.assertEquals("new", user.getPassword());

        UserInventory user2 = creator.createUser("user2", "password");

        boolean s = false;
        try {
            api.resetUserPassword(user2.getUuid(), "new", session);
        } catch (ApiSenderException e) {
            if (SysErrors.INVALID_ARGUMENT_ERROR.toString().equals(e.getError().getCode())) {
                s = true;
            }
        }
        Assert.assertTrue(s);
    }
}
