package org.zstack.test.mevoco.ha;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.ha.HaKvmSimulatorConfig;
import org.zstack.ha.VmHaLevel;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
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
 * 1. stop a vm
 * 2. set the vm to neverstop
 * <p>
 * confirm the vm starts after being set to neverstop
 */

public class TestHaOnKvm14 {
    CLogger logger = Utils.getLogger(TestHaOnKvm14.class);
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
        final VmInstanceInventory vm = deployer.vms.get("TestVm");
        api.stopVmInstance(vm.getUuid());
        api.setVmHaLevel(vm.getUuid(), VmHaLevel.NeverStop, null);
        TimeUnit.SECONDS.sleep(3);
        VmInstanceVO vo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
        Assert.assertEquals(VmInstanceState.Running, vo.getState());
    }
}
