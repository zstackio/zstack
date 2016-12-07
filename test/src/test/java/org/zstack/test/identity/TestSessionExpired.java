package org.zstack.test.identity;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.config.GlobalConfigInventory;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.identity.SessionInventory;
import org.zstack.identity.IdentityGlobalConfig;
import org.zstack.test.*;

import java.util.concurrent.TimeUnit;

public class TestSessionExpired {
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;
    GlobalConfigFacade gcf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        gcf = loader.getComponent(GlobalConfigFacade.class);
        api = new Api();
        api.startServer();
    }


    @Test
    public void test() throws ApiSenderException, InterruptedException {
        IdentityGlobalConfig.SESSION_TIMEOUT.updateValue(1);

        GlobalConfigInventory g = new GlobalConfigInventory();
        g.setCategory(IdentityGlobalConfig.CATEGORY);
        g.setName(IdentityGlobalConfig.SESSION_TIMEOUT.getName());
        //g.setValue("72000000000000000000000000000");
        g.setValue("720000000000000000000000000000000000000000");

        boolean s = false;
        try {
            api.updateGlobalConfig(g);
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);

        int val = IdentityGlobalConfig.SESSION_TIMEOUT.value(Integer.class);
        Assert.assertEquals(1, val);

        AccountInventory inv = api.createAccount("Test", "Test");
        AccountVO vo = dbf.findByUuid(inv.getUuid(), AccountVO.class);
        Assert.assertNotNull(vo);
        SessionInventory session = api.loginByAccount(inv.getName(), vo.getPassword());
        api.setAdminSession(session);
        TimeUnit.SECONDS.sleep(3);
        s = false;
        try {
            api.listAccount(null);
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);
    }

}
