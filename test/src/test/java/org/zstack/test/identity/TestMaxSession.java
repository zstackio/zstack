package org.zstack.test.identity;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.identity.IdentityGlobalConfig;
import org.zstack.test.*;

public class TestMaxSession {
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;
    GlobalConfigFacade gcf;
    int num = 5;

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

    @Test(expected = ApiSenderException.class)
    public void test() throws ApiSenderException {
        IdentityGlobalConfig.MAX_CONCURRENT_SESSION.updateValue(num);
        for (int i = 0; i < num + 1; i++) {
            api.loginAsAdmin();
        }
    }

}
