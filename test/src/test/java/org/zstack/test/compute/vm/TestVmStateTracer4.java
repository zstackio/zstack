package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.host.HostGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.host.HostInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmTracerCanonicalEvents;
import org.zstack.header.vm.VmTracerCanonicalEvents.VmStateChangedOnHostData;
import org.zstack.simulator.SimulatorController;
import org.zstack.simulator.SimulatorVmSyncPingTask;
import org.zstack.test.Api;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 1. create a vm
 * 2. remove the vm from its current host
 * 3. put the vm to a new host
 * <p>
 * confirm the vm changed to the new host
 * confirm the capacity of the old host returned and of hte new host allocated
 * confirm the VmStateChangedOnHostData issued
 */
public class TestVmStateTracer4 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    GlobalConfigFacade gcf;
    SimulatorController sctrl;
    SimulatorVmSyncPingTask tracer;
    EventFacade evtf;
    boolean success1 = false;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestVmStateTracer.xml");
        loader = deployer.getComponentLoader();
        gcf = loader.getComponent(GlobalConfigFacade.class);
        sctrl = loader.getComponent(SimulatorController.class);
        tracer = loader.getComponent(SimulatorVmSyncPingTask.class);
        HostGlobalConfig.PING_HOST_INTERVAL.updateValue(1);
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        evtf = loader.getComponent(EventFacade.class);

        deployer.build();
        api = deployer.getApi();
    }

    @Test
    public void test() throws InterruptedException {
        final HostInventory host2 = deployer.hosts.get("TestHost2");
        final HostInventory host1 = deployer.hosts.get("TestHost1");
        VmInstanceInventory vm = deployer.vms.get("TestVm1");
        final String vmUuid = vm.getUuid();

        evtf.on(VmTracerCanonicalEvents.VM_STATE_CHANGED_PATH, new EventCallback() {
            @Override
            public void run(Map tokens, Object data) {
                VmStateChangedOnHostData d = (VmTracerCanonicalEvents.VmStateChangedOnHostData) data;
                if (d.getVmUuid().equals(vmUuid) && d.getTo() == VmInstanceState.Running
                        && d.getCurrentHostUuid().equals(host2.getUuid())) {
                    success1 = true;
                }
            }
        });

        HostCapacityVO cap1 = dbf.findByUuid(host1.getUuid(), HostCapacityVO.class);
        HostCapacityVO cap2 = dbf.findByUuid(host2.getUuid(), HostCapacityVO.class);
        sctrl.setVmStateOnSimulatorHost(host2.getUuid(), vm.getUuid(), VmInstanceState.Running);
        sctrl.removeVmOnSimulatorHost(vm.getHostUuid(), vm.getUuid());
        TimeUnit.SECONDS.sleep(3);
        VmInstanceVO vo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
        Assert.assertEquals(VmInstanceState.Running, vo.getState());
        Assert.assertEquals(host2.getUuid(), vo.getHostUuid());

        HostCapacityVO cap11 = dbf.findByUuid(host1.getUuid(), HostCapacityVO.class);
        HostCapacityVO cap22 = dbf.findByUuid(host2.getUuid(), HostCapacityVO.class);
        Assert.assertEquals(cap11.getAvailableCpu(), cap1.getAvailableCpu() + vm.getCpuNum());
        Assert.assertEquals(cap11.getAvailableMemory(), cap1.getAvailableMemory() + vm.getMemorySize());
        Assert.assertEquals(cap22.getAvailableCpu(), cap2.getAvailableCpu() - vm.getCpuNum());
        Assert.assertEquals(cap22.getAvailableMemory(), cap2.getAvailableMemory() - vm.getMemorySize());
        Assert.assertTrue(success1);
    }
}
