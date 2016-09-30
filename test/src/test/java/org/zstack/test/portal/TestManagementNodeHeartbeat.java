package org.zstack.test.portal;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.managementnode.ManagementNodeState;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.portal.managementnode.ManagementNodeGlobalConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.BeanConstructor;
import org.zstack.test.DBUtil;

import java.util.concurrent.TimeUnit;

/**
 */
public class TestManagementNodeHeartbeat {
    ComponentLoader loader;
    Api api;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new BeanConstructor();
        con.addXml("PortalForUnitTest.xml");
        con.addXml("AccountManager.xml");
        loader = con.build();
        dbf = loader.getComponent(DatabaseFacade.class);
        api = new Api();
        api.startServer();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        ManagementNodeGlobalConfig.NODE_HEARTBEAT_INTERVAL.updateValue(1);
        ManagementNodeVO fake = new ManagementNodeVO();
        fake.setUuid(Platform.getUuid());
        fake.setHostName("192.168.0.11");
        fake.setPort(8080);
        fake.setState(ManagementNodeState.RUNNING);
        dbf.persist(fake);
        TimeUnit.SECONDS.sleep(5);
        long count = dbf.count(ManagementNodeVO.class);
        Assert.assertEquals(1, count);
    }
}
