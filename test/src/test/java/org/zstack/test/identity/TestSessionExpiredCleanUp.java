package org.zstack.test.identity;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.identity.SessionVO;
import org.zstack.identity.IdentityGlobalConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.BeanConstructor;
import org.zstack.test.DBUtil;

import java.util.concurrent.TimeUnit;

public class TestSessionExpiredCleanUp {
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;
    GlobalConfigFacade gcf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new BeanConstructor();
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
        IdentityGlobalConfig.SESSION_CELANUP_INTERVAL.updateValue(1);
        SessionInventory session = api.loginAsAdmin();
        TimeUnit.SECONDS.sleep(5);
        SessionVO vo = dbf.findByUuid(session.getUserUuid(), SessionVO.class);
        Assert.assertNull(vo);
    }

}
