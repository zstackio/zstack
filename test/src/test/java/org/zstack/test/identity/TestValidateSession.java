package org.zstack.test.identity;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.identity.IdentityGlobalConfig;
import org.zstack.test.*;

import java.util.concurrent.TimeUnit;

public class TestValidateSession {
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
    public void test() throws ApiSenderException, InterruptedException {
        SessionInventory session = api.loginAsAdmin();
        boolean ret = api.validateSession(session.getUuid());
        Assert.assertEquals(true, ret);
        api.logout(session.getUuid());
        ret = api.validateSession(session.getUuid());
        Assert.assertEquals(false, ret);

        IdentityGlobalConfig.SESSION_TIMEOUT.updateValue(1);
        session = api.loginAsAdmin();
        TimeUnit.SECONDS.sleep(3);
        ret = api.validateSession(session.getUuid());
        Assert.assertEquals(false, ret);
    }
}
