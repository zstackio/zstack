package org.zstack.test.identity;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.UserInventory;
import org.zstack.header.identity.UserVO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.BeanConstructor;
import org.zstack.test.DBUtil;

/**
 * 1. create an account
 * 2. create a user
 * 3. reset the password of user
 *
 * confirm the password reset successfully
 */
public class TestIdentity4 {
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new BeanConstructor();
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
        api.loginByUser("test", "password", a.getUuid());
    }
}
