package org.zstack.test.core.config;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.*;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.test.*;
import org.zstack.test.search.QueryTestValidator;

public class TestQueryGlobalConfig {
    GlobalConfigFacade gcf;
    ComponentLoader loader;
    DatabaseFacade dbf;
    Api api;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        loader = con.addAllConfigInZstackXml().build();
        gcf = loader.getComponent(GlobalConfigFacade.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        api = new Api();
        api.startServer();
    }

    @Test
    public void test() throws InterruptedException, ApiSenderException, GlobalConfigException {
        String category = "Test";
        String name = "Test";
        GlobalConfigInventory inv = null;
        for (GlobalConfigInventory i : api.listGlobalConfig(null)) {
            if (i.getName().equals(name) && i.getCategory().equals(category)) {
                inv = i;
                break;
            }
        }
        QueryTestValidator.validateEQ(new APIQueryGlobalConfigMsg(), api, APIQueryGlobalConfigReply.class, inv, api.getAdminSession());
        QueryTestValidator.validateRandomEQConjunction(new APIQueryGlobalConfigMsg(), api, APIQueryGlobalConfigReply.class, inv, api.getAdminSession(), 2);
    }
}
