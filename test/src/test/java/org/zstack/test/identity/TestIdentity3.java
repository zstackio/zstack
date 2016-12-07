package org.zstack.test.identity;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.identity.SessionInventory;
import org.zstack.test.*;

/**
 * 1. create an account
 * 2. reset password of the account
 * <p>
 * confirm the password is reset
 */
public class TestIdentity3 {
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
        creator.resetAccountPassword("password");
        AccountVO vo = dbf.findByUuid(a.getUuid(), AccountVO.class);
        Assert.assertEquals("password", vo.getPassword());
        SessionInventory session = api.loginByAccount("test", "password");
        api.resetAccountPassword(null, "new", session);
        vo = dbf.findByUuid(a.getUuid(), AccountVO.class);
        Assert.assertEquals("new", vo.getPassword());

        boolean s = false;
        try {
            api.resetAccountPassword(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, "new", session);
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);

        AccountInventory a2 = api.createAccount("account2", "password");
        s = false;
        try {
            api.resetAccountPassword(a2.getUuid(), "new", session);
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);
    }
}
