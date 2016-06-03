package org.zstack.test.mevoco.logging;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.logging.Event;
import org.zstack.core.logging.Log;
import org.zstack.core.logging.Log.Content;
import org.zstack.core.logging.LogType;
import org.zstack.header.allocator.HostCapacityOverProvisioningManager;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.logging.APIQueryLogMsg;
import org.zstack.logging.APIQueryLogReply;
import org.zstack.logging.LogConstants;
import org.zstack.logging.LogInventory;
import org.zstack.network.service.flat.FlatNetworkServiceSimulatorConfig;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 */
public class TestLogging1 {
    CLogger logger = Utils.getLogger(TestLogging1.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    FlatNetworkServiceSimulatorConfig fconfig;
    KVMSimulatorConfig kconfig;
    PrimaryStorageOverProvisioningManager psRatioMgr;
    HostCapacityOverProvisioningManager hostRatioMgr;
    EventFacade evtf;
    Log.Content logContent;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        DBUtil.reDeployCassandra(LogConstants.KEY_SPACE);
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/OnlyOneZone.xml", con);
        deployer.addSpringConfig("cassandra.xml");
        deployer.addSpringConfig("mevocoRelated.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(LocalStorageSimulatorConfig.class);
        fconfig = loader.getComponent(FlatNetworkServiceSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        psRatioMgr = loader.getComponent(PrimaryStorageOverProvisioningManager.class);
        hostRatioMgr = loader.getComponent(HostCapacityOverProvisioningManager.class);
        evtf = loader.getComponent(EventFacade.class);

        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }
    
	@Test
	public void test() throws ApiSenderException, IOException, InterruptedException {
        Log log = new Log(Platform.getUuid()).log(LogLabelTest.TEST1);

        APIQueryLogMsg msg = new APIQueryLogMsg();
        msg.setType(LogType.RESOURCE.toString());
        msg.setResourceUuid(log.getResourceUuid());
        APIQueryLogReply reply = api.queryCassandra(msg, APIQueryLogReply.class);
        Assert.assertEquals(1, reply.getInventories().size());
        LogInventory loginv = reply.getInventories().get(0);
        Assert.assertEquals("测试1", loginv.getText());
        Assert.assertEquals(LogType.RESOURCE.toString(), loginv.getType());
        Assert.assertEquals(log.getResourceUuid(), loginv.getResourceUuid());

        api.deleteLog(loginv.getUuid(), null);
        reply = api.queryCassandra(msg, APIQueryLogReply.class);
        Assert.assertEquals(0, reply.getInventories().size());

        log = new Log().log(LogLabelTest.TEST1);

        msg = new APIQueryLogMsg();
        msg.setType(LogType.SYSTEM.toString());
        msg.setResourceUuid(log.getResourceUuid());
        reply = api.queryCassandra(msg, APIQueryLogReply.class);
        Assert.assertEquals(1, reply.getInventories().size());
        loginv = reply.getInventories().get(0);
        Assert.assertEquals("测试1", loginv.getText());
        Assert.assertEquals(LogType.SYSTEM.toString(), loginv.getType());
        Assert.assertEquals(LogType.SYSTEM.toString(), loginv.getResourceUuid());

        evtf.on(Event.EVENT_PATH, new EventCallback() {
            @Override
            public void run(Map tokens, Object data) {
                logContent = (Content) data;
            }
        });

        new Event(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID).log(LogLabelTest.TEST1);

        TimeUnit.SECONDS.sleep(2);

        Assert.assertNotNull(logContent);

        msg = new APIQueryLogMsg();
        msg.setType(LogType.EVENT.toString());
        msg.setResourceUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
        reply = api.queryCassandra(msg, APIQueryLogReply.class);
        Assert.assertEquals(1, reply.getInventories().size());
        loginv = reply.getInventories().get(0);
        Assert.assertEquals("测试1", loginv.getText());
        Assert.assertEquals(LogType.EVENT.toString(), loginv.getType());
        Assert.assertEquals(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, loginv.getResourceUuid());
    }
}
