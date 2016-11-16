package org.zstack.test.mevoco.ha;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.ha.HaGlobalConfig;
import org.zstack.ha.HaKvmSimulatorConfig;
import org.zstack.ha.SelfFencerKvmBackend.CancelSelfFencerCmd;
import org.zstack.ha.SelfFencerKvmBackend.SetupSelfFencerCmd;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

/**
 * test setup/cancel self-fencer for filesystem primary storage
 */

public class TestHaOnKvm11 {
    CLogger logger = Utils.getLogger(TestHaOnKvm11.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    KVMSimulatorConfig config;
    HaKvmSimulatorConfig hconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/ha/TestHaOnKvm1.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("ha.xml");
        deployer.addSpringConfig("haSimulator.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(KVMSimulatorConfig.class);
        hconfig = loader.getComponent(HaKvmSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        HostInventory host1 = deployer.hosts.get("host1");
        HostInventory host2 = deployer.hosts.get("host2");

        hconfig.setupSelfFencerCmds.clear();
        hconfig.cancelSelfFencerCmds.clear();
        HaGlobalConfig.ALL.updateValue(false);
        TimeUnit.SECONDS.sleep(2);

        Assert.assertEquals(2, hconfig.cancelSelfFencerCmds.size());

        CancelSelfFencerCmd cmd = hconfig.cancelSelfFencerCmds.stream().filter(c -> c.hostUuid.equals(host1.getUuid())).findAny().get();
        Assert.assertNotNull(cmd);
        cmd = hconfig.cancelSelfFencerCmds.stream().filter(c -> c.hostUuid.equals(host2.getUuid())).findAny().get();
        Assert.assertNotNull(cmd);

        HaGlobalConfig.ALL.updateValue(true);
        TimeUnit.SECONDS.sleep(2);

        Assert.assertEquals(2, hconfig.setupSelfFencerCmds.size());
        SetupSelfFencerCmd scmd = hconfig.setupSelfFencerCmds.stream().filter(c -> c.hostUuid.equals(host1.getUuid())).findAny().get();
        Assert.assertNotNull(scmd);
        scmd = hconfig.setupSelfFencerCmds.stream().filter(c -> c.hostUuid.equals(host2.getUuid())).findAny().get();
        Assert.assertNotNull(scmd);
    }
}
