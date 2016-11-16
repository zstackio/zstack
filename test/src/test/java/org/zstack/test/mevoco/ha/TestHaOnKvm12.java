package org.zstack.test.mevoco.ha;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.ha.HaGlobalConfig;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase.KvmCancelSelfFencerCmd;
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase.KvmSetupSelfFencerCmd;
import org.zstack.storage.ceph.primary.CephPrimaryStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

/**
 * test setup/cancel self-fencer for ceph primary storage
 */

public class TestHaOnKvm12 {
    CLogger logger = Utils.getLogger(TestHaOnKvm12.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    KVMSimulatorConfig config;
    CephPrimaryStorageSimulatorConfig cconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/ha/TestHaOnKvm12.xml", con);
        deployer.addSpringConfig("ceph.xml");
        deployer.addSpringConfig("cephSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("ha.xml");
        deployer.addSpringConfig("haSimulator.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(KVMSimulatorConfig.class);
        cconfig = loader.getComponent(CephPrimaryStorageSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        HostInventory host1 = deployer.hosts.get("host1");

        cconfig.kvmSetupSelfFencerCmds.clear();
        cconfig.kvmCancelSelfFencerCmds.clear();
        HaGlobalConfig.ALL.updateValue(false);
        TimeUnit.SECONDS.sleep(2);

        Assert.assertEquals(1, cconfig.kvmCancelSelfFencerCmds.size());

        KvmCancelSelfFencerCmd cmd = cconfig.kvmCancelSelfFencerCmds.stream().filter(c -> c.hostUuid.equals(host1.getUuid())).findAny().get();
        Assert.assertNotNull(cmd);

        HaGlobalConfig.ALL.updateValue(true);
        TimeUnit.SECONDS.sleep(2);

        Assert.assertEquals(1, cconfig.kvmSetupSelfFencerCmds.size());
        KvmSetupSelfFencerCmd scmd = cconfig.kvmSetupSelfFencerCmds.stream().filter(c -> c.hostUuid.equals(host1.getUuid())).findAny().get();
        Assert.assertNotNull(scmd);
    }
}
