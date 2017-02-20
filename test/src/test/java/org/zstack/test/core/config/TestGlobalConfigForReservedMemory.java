package org.zstack.test.core.config;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.config.GlobalConfigInventory;
import org.zstack.test.*;

public class TestGlobalConfigForReservedMemory {
    GlobalConfigFacade gcf;
    ComponentLoader loader;
    Api api;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

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
        String category = "kvm";
        String name = "reservedMemory";
        String _2T = "2T";
        String _1025G = "2T";


        GlobalConfigInventory inv = new GlobalConfigInventory();
        inv.setName(name);
        inv.setCategory(category);
        inv.setValue(_2T);
        thrown.expect(ApiSenderException.class);
        api.updateGlobalConfig(inv);
        thrown.expect(GlobalConfigException.class);
        gcf.updateConfig(category, name, _2T);

        inv.setValue(_1025G);
        thrown.expect(ApiSenderException.class);
        api.updateGlobalConfig(inv);
        thrown.expect(GlobalConfigException.class);
        gcf.updateConfig(category, name, _1025G);
    }


}
