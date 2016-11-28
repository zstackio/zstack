package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.rest.RESTConstant;
import org.zstack.header.rest.RESTFacade;
import org.zstack.kvm.KVMAgentCommands.ReconnectMeCmd;
import org.zstack.kvm.KVMConstant;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * 1. send ReconnectMe command
 * <p>
 * confirm the reconnect happens
 */
public class TestKvmReconnectMe {
    CLogger logger = Utils.getLogger(TestKvmReconnectMe.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    KVMSimulatorConfig config;
    RESTFacade restf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestKvmAgentRestart.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(KVMSimulatorConfig.class);
        restf = loader.getComponent(RESTFacade.class);
        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws InterruptedException {
        config.connectCmds.clear();

        UriComponentsBuilder ub = UriComponentsBuilder.fromHttpUrl(restf.getBaseUrl());
        ub.path(RESTConstant.COMMAND_CHANNEL_PATH);
        String url = ub.build().toUriString();
        Map<String, String> header = map(e(RESTConstant.COMMAND_PATH, KVMConstant.KVM_RECONNECT_ME));

        HostInventory host = deployer.hosts.get("host1");
        ReconnectMeCmd cmd = new ReconnectMeCmd();
        cmd.hostUuid = host.getUuid();
        cmd.reason = "on purpose";
        restf.syncJsonPost(url, JSONObjectUtil.toJsonString(cmd), header, String.class);
        TimeUnit.SECONDS.sleep(3);
        Assert.assertEquals(1, config.connectCmds.size());
    }
}
