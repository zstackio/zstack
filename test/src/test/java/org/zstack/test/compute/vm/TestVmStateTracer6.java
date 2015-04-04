package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.host.HostGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.host.HostInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.simulator.SimulatorController;
import org.zstack.simulator.SimulatorVmSyncPingTask;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.concurrent.TimeUnit;

public class TestVmStateTracer6 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    GlobalConfigFacade gcf;
    SimulatorController sctrl;
    SimulatorVmSyncPingTask tracer;

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
        
        deployer.build();
        api = deployer.getApi();
    }
    
    @Test
    public void test() throws InterruptedException, ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm1");
        api.stopVmInstance(vm.getUuid());
        sctrl.removeVmOnSimulatorHost(vm.getHostUuid(), vm.getUuid());
        TimeUnit.SECONDS.sleep(3);
        VmInstanceVO vo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
        Assert.assertEquals(VmInstanceState.Stopped, vo.getState());
        Assert.assertNull(vo.getHostUuid());
    }
}
