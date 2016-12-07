package org.zstack.test.core.config;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.config.GlobalConfigInventory;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.test.*;

import java.util.concurrent.TimeUnit;

public class TestGlobalConfigUpdatedExtension {
    GlobalConfigFacade gcf;
    ComponentLoader loader;
    Api api;
    int result;

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
        GlobalConfigForTest.TEST.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                result = newConfig.value(Integer.class);
            }
        });

        GlobalConfigInventory target = null;
        for (GlobalConfigInventory inv : api.listGlobalConfig(null)) {
            if ("Test".equals(inv.getName())) {
                target = inv;
                break;
            }
        }
        target.setValue("1200");
        api.updateGlobalConfig(target);
        TimeUnit.SECONDS.sleep(1);
        Assert.assertEquals(1200, result);
    }

}
