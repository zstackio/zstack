package org.zstack.test.core.config;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.config.GlobalConfigInventory;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.test.*;

public class TestGlobalConfigForSessionTimeOut {
    GlobalConfigFacade gcf;
    ComponentLoader loader;
    Api api;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        loader = con.addXml("PortalForUnitTest.xml").addXml("AccountManager.xml").build();
        gcf = loader.getComponent(GlobalConfigFacade.class);
        api = new Api();
        api.startServer();
    }

    @Test
    public void test() throws InterruptedException, ApiSenderException, GlobalConfigException {
        String category = "identity";
        String name = "session.timeout";
        boolean flag = false;

        GlobalConfigInventory inv = new GlobalConfigInventory();
        inv.setName(name);
        inv.setCategory(category);
        inv.setValue(String.valueOf(31536001));
        try {
            api.updateGlobalConfig(inv);
        } catch (ApiSenderException e) {
            Assert.assertEquals(e instanceof ApiSenderException, true);
            flag = true;
        } catch (Exception e) {
            Assert.assertEquals(e instanceof Exception, "Unknown error");
        }
        Assert.assertEquals(flag, true);
        flag = false;
        try {
            gcf.updateConfig(category, name, String.valueOf(31536002));
        } catch (GlobalConfigException e) {
            Assert.assertEquals(e instanceof GlobalConfigException, true);
            flag = true;
        } catch (Exception e) {
            Assert.assertEquals(e instanceof Exception, "Unknown error");
        }
        Assert.assertEquals(flag, true);
        GlobalConfigInventory inv2 = new GlobalConfigInventory();
        inv2.setName(name);
        inv2.setCategory(category);
        inv2.setValue(String.valueOf(7200));
        api.updateGlobalConfig(inv2);
    }

}
