package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.host.HostGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.host.HostInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.simulator.SimulatorController;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.concurrent.TimeUnit;

/**
 * 1. create a vm
 * 2. migrate the vm
 * <p>
 * confirm the vm migrated successfully
 * confirm the src/dst host capacity correct
 */
public class TestVmStateTracer5 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    GlobalConfigFacade gcf;
    SimulatorController sctrl;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestVmStateTracer.xml");
        loader = deployer.getComponentLoader();
        gcf = loader.getComponent(GlobalConfigFacade.class);
        sctrl = loader.getComponent(SimulatorController.class);
        HostGlobalConfig.PING_HOST_INTERVAL.updateValue(1);
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);

        deployer.build();
        api = deployer.getApi();
    }

    @Test
    public void test() throws InterruptedException, ApiSenderException {
        HostInventory host1 = deployer.hosts.get("TestHost1");
        HostInventory host2 = deployer.hosts.get("TestHost2");
        VmInstanceInventory vm1 = deployer.vms.get("TestVm1");

        HostCapacityVO cap1 = dbf.findByUuid(host1.getUuid(), HostCapacityVO.class);
        HostCapacityVO cap2 = dbf.findByUuid(host2.getUuid(), HostCapacityVO.class);
        api.migrateVmInstance(vm1.getUuid(), host2.getUuid());
        TimeUnit.SECONDS.sleep(3);
        VmInstanceVO vmvo = dbf.findByUuid(vm1.getUuid(), VmInstanceVO.class);
        Assert.assertEquals(VmInstanceState.Running, vmvo.getState());
        Assert.assertEquals(host2.getUuid(), vmvo.getHostUuid());

        HostCapacityVO cap11 = dbf.findByUuid(host1.getUuid(), HostCapacityVO.class);
        HostCapacityVO cap22 = dbf.findByUuid(host2.getUuid(), HostCapacityVO.class);
        long cpu = vm1.getCpuNum();
        Assert.assertEquals(cap11.getAvailableCpu(), cap1.getAvailableCpu() + cpu);
        Assert.assertEquals(cap11.getAvailableMemory(), cap1.getAvailableMemory() + vm1.getMemorySize());
        Assert.assertEquals(cap22.getAvailableCpu(), cap2.getAvailableCpu() - cpu);
        Assert.assertEquals(cap22.getAvailableMemory(), cap2.getAvailableMemory() - vm1.getMemorySize());
    }
}
