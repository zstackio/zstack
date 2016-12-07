package org.zstack.test.core.config;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.config.GlobalConfigInventory;
import org.zstack.test.*;

public class TestGlobalConfig {
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
        String category = "Test";
        String name = "Test";
        int value = gcf.getConfigValue(category, name, Integer.class);
        Assert.assertEquals(1000, value);
        gcf.updateConfig(category, name, String.valueOf(10000));
        value = gcf.getConfigValue(category, name, Integer.class);
        Assert.assertEquals(10000, value);
        Assert.assertEquals(10000, (int) GlobalConfigForTest.TEST.value(Integer.class));

        GlobalConfigForTest.TEST.updateValue(10);
        Assert.assertEquals(10, (int) GlobalConfigForTest.TEST.value(Integer.class));
        value = gcf.getConfigValue(category, name, Integer.class);
        Assert.assertEquals(10, value);

        GlobalConfigInventory inv = new GlobalConfigInventory();
        inv.setName(name);
        inv.setCategory(category);
        inv.setValue(String.valueOf(100));
        api.updateGlobalConfig(inv);
        Assert.assertEquals(100, (int) GlobalConfigForTest.TEST.value(Integer.class));
        value = gcf.getConfigValue(category, name, Integer.class);
        Assert.assertEquals(100, value);
    }

}
