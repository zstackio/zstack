package org.zstack.test.mevoco.ha;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.ha.HaKvmSimulatorConfig;
import org.zstack.ha.VmHaLevel;
import org.zstack.header.host.HostInventory;
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

/**
 * 1. make the host where the VM runs down
 * <p>
 * confirm the VM is HA started on another host
 */

public class TestHaOnKvm10 {
    CLogger logger = Utils.getLogger(TestHaOnKvm10.class);
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
        deployer = new Deployer("deployerXml/ha/TestHaOnKvm9.xml", con);
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
        VmInstanceInventory vm1 = deployer.vms.get("TestVm1");
        VmInstanceInventory vm2 = deployer.vms.get("TestVm2");
        api.setVmHaLevel(vm1.getUuid(), VmHaLevel.NeverStop, null);
        api.setVmHaLevel(vm2.getUuid(), VmHaLevel.OnHostFailure, null);

        api.maintainHost(vm1.getHostUuid());

        HostInventory host2 = deployer.hosts.get("host2");
        VmInstanceVO vmvo1 = dbf.findByUuid(vm1.getUuid(), VmInstanceVO.class);
        VmInstanceVO vmvo2 = dbf.findByUuid(vm2.getUuid(), VmInstanceVO.class);
        Assert.assertEquals(VmInstanceState.Running, vmvo1.getState());
        Assert.assertEquals(host2.getUuid(), vmvo1.getHostUuid());
        Assert.assertEquals(VmInstanceState.Stopped, vmvo2.getState());
    }
}
