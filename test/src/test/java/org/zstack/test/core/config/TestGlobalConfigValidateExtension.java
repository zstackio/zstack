package org.zstack.test.core.config;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.config.GlobalConfigInventory;
import org.zstack.test.*;

public class TestGlobalConfigValidateExtension {
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
    public void test() throws InterruptedException, ApiSenderException {
        GlobalConfigInventory target = null;
        for (GlobalConfigInventory inv : api.listGlobalConfig(null)) {
            if ("Test3".equals(inv.getName())) {
                target = inv;
                break;
            }
        }
        target.setValue("not a integer");

        boolean s = false;
        try {
            api.updateGlobalConfig(target);
        } catch (ApiSenderException e) {
            s = true;
        }

        Assert.assertTrue(s);
    }
}
