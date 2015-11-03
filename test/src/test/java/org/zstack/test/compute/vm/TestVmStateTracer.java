package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.host.HostGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.host.HostVO;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.simulator.SimulatorController;
import org.zstack.test.Api;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.concurrent.TimeUnit;

/**
 * 1. create a vm
 * 2. make the vm abnormally stopped
 *
 * confirm the host capacity returned
 *
 * 3. make the vm abnormally started
 *
 * confirm the host capacity allocated
 */
public class TestVmStateTracer {
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
    public void test() throws InterruptedException {
        SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
        q.add(VmInstanceVO_.name, Op.EQ, "TestVm1");
        VmInstanceVO vm1 = q.find();
        Assert.assertNotNull(vm1);
        Assert.assertEquals(VmInstanceState.Running, vm1.getState());

        HostVO hvo1 = dbf.findByUuid(vm1.getHostUuid(), HostVO.class);
        String hostUuid = vm1.getHostUuid();
        sctrl.setVmStateOnSimulatorHost(hostUuid, vm1.getUuid(), VmInstanceState.Stopped);
        TimeUnit.SECONDS.sleep(3);
        vm1 = dbf.findByUuid(vm1.getUuid(), VmInstanceVO.class);
        Assert.assertNotNull(vm1);
        Assert.assertEquals(VmInstanceState.Stopped, vm1.getState());
        Assert.assertNull(vm1.getHostUuid());
        HostVO hvo2 = dbf.findByUuid(hostUuid, HostVO.class);
        Assert.assertEquals(hvo2.getCapacity().getAvailableMemory(), hvo1.getCapacity().getAvailableMemory() + vm1.getMemorySize());
        Assert.assertEquals(hvo2.getCapacity().getAvailableCpu(), hvo1.getCapacity().getAvailableCpu() + vm1.getCpuSpeed() * vm1.getCpuNum());

        sctrl.setVmStateOnSimulatorHost(hostUuid, vm1.getUuid(), VmInstanceState.Running);
        TimeUnit.SECONDS.sleep(3);
        vm1 = dbf.findByUuid(vm1.getUuid(), VmInstanceVO.class);
        Assert.assertNotNull(vm1);
        Assert.assertEquals(VmInstanceState.Running, vm1.getState());
        Assert.assertNotNull(vm1.getHostUuid());
        HostVO hvo3 = dbf.findByUuid(vm1.getHostUuid(), HostVO.class);
        Assert.assertEquals(hvo3.getCapacity().getAvailableMemory(), hvo2.getCapacity().getAvailableMemory() - vm1.getMemorySize());
        Assert.assertEquals(hvo3.getCapacity().getAvailableCpu(), hvo2.getCapacity().getAvailableCpu() - vm1.getCpuNum() * vm1.getCpuSpeed());
    }
}
