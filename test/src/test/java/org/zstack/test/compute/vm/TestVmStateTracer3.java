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
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vm.VmTracerCanonicalEvents;
import org.zstack.simulator.SimulatorController;
import org.zstack.test.Api;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 1. create a vm
 * 2. remove the vm abnormally from the host
 * <p>
 * confirm the vm stopped and the host capacity returned
 * confirm VmStateChangedOnHostData issued
 */
public class TestVmStateTracer3 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    GlobalConfigFacade gcf;
    SimulatorController sctrl;
    EventFacade evtf;
    boolean success1 = false;

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
        evtf = loader.getComponent(EventFacade.class);

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

        final String vmUuid = vm1.getUuid();
        final String hostUuid = vm1.getHostUuid();

        evtf.on(VmTracerCanonicalEvents.VM_STATE_CHANGED_PATH, new EventCallback() {
            @Override
            public void run(Map tokens, Object data) {
                VmTracerCanonicalEvents.VmStateChangedOnHostData d = (VmTracerCanonicalEvents.VmStateChangedOnHostData) data;
                if (d.getVmUuid().equals(vmUuid) && d.getTo() == VmInstanceState.Stopped
                        && d.getOriginalHostUuid().equals(hostUuid) && d.getCurrentHostUuid() == null) {
                    success1 = true;
                }
            }
        });

        HostCapacityVO cap1 = dbf.findByUuid(hostUuid, HostCapacityVO.class);
        sctrl.removeVmOnSimulatorHost(hostUuid, vm1.getUuid());
        TimeUnit.SECONDS.sleep(3);
        vm1 = dbf.findByUuid(vm1.getUuid(), VmInstanceVO.class);
        Assert.assertEquals(VmInstanceState.Stopped, vm1.getState());
        Assert.assertNull(vm1.getHostUuid());
        HostCapacityVO cap2 = dbf.findByUuid(hostUuid, HostCapacityVO.class);
        Assert.assertEquals(cap2.getAvailableCpu(), cap1.getAvailableCpu() + vm1.getCpuNum());
        Assert.assertEquals(cap2.getAvailableMemory(), cap1.getAvailableMemory() + vm1.getMemorySize());
        Assert.assertTrue(success1);
    }
}
