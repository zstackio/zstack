package org.zstack.test.identity;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.identity.UserVO;
import org.zstack.test.*;

public class TestResetAccountPassword {
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
        SessionInventory session = api.loginAsAdmin();
        AccountInventory inv = api.resetAccountPassword(session.getAccountUuid(), "Test", session);
        AccountVO vo = dbf.findByUuid(inv.getUuid(), AccountVO.class);
        Assert.assertNotNull(vo);
        Assert.assertEquals("Test", vo.getPassword());
        UserVO uvo = dbf.findByUuid(vo.getUuid(), UserVO.class);
        Assert.assertNotNull(uvo);
        Assert.assertEquals("Test", uvo.getPassword());
    }
}
