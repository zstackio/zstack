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
import org.zstack.header.host.HostVO;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vm.VmTracerCanonicalEvents;
import org.zstack.header.vm.VmTracerCanonicalEvents.VmStateChangedOnHostData;
import org.zstack.simulator.SimulatorController;
import org.zstack.test.Api;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 1. create a vm
 * 2. make the vm abnormally stopped
 * <p>
 * confirm the host capacity returned
 * confirm VmStateChangedOnHostData issued
 * <p>
 * 3. make the vm abnormally started
 * <p>
 * confirm the host capacity allocated
 * confirm VmStateChangedOnHostData issued
 */
public class TestVmStateTracer {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    GlobalConfigFacade gcf;
    SimulatorController sctrl;
    EventFacade evtf;
    boolean success1 = false;
    boolean success2 = false;
    private static final CLogger logger = Utils.getLogger(TestVmStateTracer.class);

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
        final String vmUuid = vm1.getUuid();
        final String hostUuid = vm1.getHostUuid();
        Assert.assertNotNull(vm1);
        Assert.assertEquals(VmInstanceState.Running, vm1.getState());

        evtf.on(VmTracerCanonicalEvents.VM_STATE_CHANGED_PATH, new EventCallback() {
            @Override
            public void run(Map tokens, Object data) {
                VmStateChangedOnHostData d = (VmTracerCanonicalEvents.VmStateChangedOnHostData) data;
                if (d.getVmUuid().equals(vmUuid) && d.getTo() == VmInstanceState.Stopped
                        && d.getOriginalHostUuid().equals(hostUuid) && d.getCurrentHostUuid() == null) {
                    // abnormally stopped
                    success1 = true;
                } else if (d.getVmUuid().equals(vmUuid) && d.getTo() == VmInstanceState.Running &&
                        d.getOriginalHostUuid() == null && d.getCurrentHostUuid().equals(hostUuid)) {
                    // abnormally started
                    success2 = true;
                }
            }
        });

        HostVO hvo1 = dbf.findByUuid(vm1.getHostUuid(), HostVO.class);
        sctrl.setVmStateOnSimulatorHost(hostUuid, vm1.getUuid(), VmInstanceState.Stopped);
        TimeUnit.SECONDS.sleep(3);
        vm1 = dbf.findByUuid(vm1.getUuid(), VmInstanceVO.class);
        Assert.assertNotNull(vm1);
        Assert.assertEquals(VmInstanceState.Stopped, vm1.getState());
        Assert.assertNull(vm1.getHostUuid());
        HostVO hvo2 = dbf.findByUuid(hostUuid, HostVO.class);
        Assert.assertEquals(hvo2.getCapacity().getAvailableMemory(), hvo1.getCapacity().getAvailableMemory() + vm1.getMemorySize());
        Assert.assertEquals(hvo2.getCapacity().getAvailableCpu(), hvo1.getCapacity().getAvailableCpu() + vm1.getCpuNum());
        Assert.assertTrue(success1);
        Assert.assertFalse(success2);

        sctrl.setVmStateOnSimulatorHost(hostUuid, vm1.getUuid(), VmInstanceState.Running);
        TimeUnit.SECONDS.sleep(3);
        vm1 = dbf.findByUuid(vm1.getUuid(), VmInstanceVO.class);
        Assert.assertNotNull(vm1);
        Assert.assertEquals(VmInstanceState.Running, vm1.getState());
        Assert.assertNotNull(vm1.getHostUuid());
        HostVO hvo3 = dbf.findByUuid(vm1.getHostUuid(), HostVO.class);
        Assert.assertEquals(hvo3.getCapacity().getAvailableMemory(), hvo2.getCapacity().getAvailableMemory() - vm1.getMemorySize());
        Assert.assertEquals(hvo3.getCapacity().getAvailableCpu(), hvo2.getCapacity().getAvailableCpu() - vm1.getCpuNum());
        Assert.assertTrue(success2);
    }
}
