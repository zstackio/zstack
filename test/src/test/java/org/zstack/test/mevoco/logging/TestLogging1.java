package org.zstack.test.mevoco.logging;

import junit.framework.Assert;
import org.apache.commons.lang.LocaleUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.core.Platform;
import org.zstack.core.ansible.AnsibleLogCmd;
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
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.kvm.KVMConstant;
import org.zstack.logging.*;
import org.zstack.network.service.flat.FlatNetworkServiceSimulatorConfig;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.StringBind;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.list;

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
    RESTFacade restf;
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
        deployer.addSpringConfig("logging.xml");
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
        restf = loader.getComponent(RESTFacade.class);

        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }
    
	@Test
	public void test() throws ApiSenderException, IOException, InterruptedException {
        Log log = new Log(Platform.getUuid());
        log.log(LogLabelTest.TEST1);

        APIQueryLogMsg msg = new APIQueryLogMsg();
        msg.setType(LogType.RESOURCE.toString());
        msg.setResourceUuid(log.getResourceUuid());
        APIQueryLogReply reply = api.queryCassandra(msg, APIQueryLogReply.class);
        Assert.assertEquals(1, reply.getInventories().size());
        LogInventory loginv = reply.getInventories().get(0);
        Assert.assertEquals("测试1", loginv.getMessage());
        Assert.assertEquals(LogType.RESOURCE.toString(), loginv.getType());
        Assert.assertEquals(log.getResourceUuid(), loginv.getResourceUuid());

        api.deleteLog(loginv.getUuid(), null);
        reply = api.queryCassandra(msg, APIQueryLogReply.class);
        Assert.assertEquals(0, reply.getInventories().size());

        log = new Log();
        log.log(LogLabelTest.TEST1);

        msg = new APIQueryLogMsg();
        msg.setType(LogType.SYSTEM.toString());
        msg.setResourceUuid(log.getResourceUuid());
        reply = api.queryCassandra(msg, APIQueryLogReply.class);
        Assert.assertEquals(1, reply.getInventories().size());
        loginv = reply.getInventories().get(0);
        Assert.assertEquals("测试1", loginv.getMessage());
        Assert.assertEquals(LogType.SYSTEM.toString(), loginv.getType());
        Assert.assertEquals(LogType.SYSTEM.toString(), loginv.getResourceUuid());

        api.deleteLog(loginv.getUuid(), null);

        evtf.on(Event.EVENT_PATH, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
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
        Assert.assertEquals("测试1", loginv.getMessage());
        Assert.assertEquals(LogType.EVENT.toString(), loginv.getType());
        Assert.assertEquals(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, loginv.getResourceUuid());

        LogGlobalConfig.LOCALE.updateValue("en_US");
        log = new Log(Platform.getUuid());
        log.log(LogLabelTest.TEST2, "你好");
        msg = new APIQueryLogMsg();
        msg.setType(LogType.RESOURCE.toString());
        msg.setResourceUuid(log.getResourceUuid());
        reply = api.queryCassandra(msg, APIQueryLogReply.class);
        Assert.assertEquals(1, reply.getInventories().size());
        loginv = reply.getInventories().get(0);
        Assert.assertEquals(Platform.i18n(LogLabelTest.TEST2, LocaleUtils.toLocale("en_US"), "你好"), loginv.getMessage());

        api.deleteLog(loginv.getUuid(), null);

        AnsibleLogCmd cmd = new AnsibleLogCmd();
        cmd.setLabel(LogLabelTest.TEST2);
        cmd.setParameters(list("test"));
        String uuid = Platform.getUuid();

        UriComponentsBuilder ub = UriComponentsBuilder.fromHttpUrl(restf.getBaseUrl());
        //ub.path(String.format("/kvm/ansiblelog/%s", uuid));
        ub.path(new StringBind(KVMConstant.KVM_ANSIBLE_LOG_PATH_FROMAT).bind("uuid", uuid).toString());
        String url = ub.build().toUriString();
        restf.syncJsonPost(url, cmd, Void.class);
        msg = new APIQueryLogMsg();
        msg.setType(LogType.RESOURCE.toString());
        msg.setResourceUuid(uuid);
        reply = api.queryCassandra(msg, APIQueryLogReply.class);
        loginv = reply.getInventories().get(0);
        Assert.assertEquals(uuid, loginv.getResourceUuid());
        Assert.assertEquals(Platform.i18n(LogLabelTest.TEST2, LocaleUtils.toLocale("en_US"), "test"), loginv.getMessage());
    }
}
