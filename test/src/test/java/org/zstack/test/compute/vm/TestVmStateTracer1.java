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
import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.host.HostVO;
import org.zstack.header.vm.VmInstance;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.simulator.SimulatorController;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.concurrent.TimeUnit;

/**
 * 1. create a vm
 * 2. disable host auto-reconnect
 * 3. make the host disconnected
 *
 * confirm the vm becomes Unknown
 * confirm the host capacity not changed
 *
 * 4. reconnect the host
 *
 * confirm the vm becomes running
 *
 */
public class TestVmStateTracer1 {
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
        HostGlobalConfig.AUTO_RECONNECT_ON_ERROR.updateValue(false);
        SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
        q.add(VmInstanceVO_.name, Op.EQ, "TestVm1");
        VmInstanceVO vm1 = q.find();

        String hostUuid = vm1.getHostUuid();
        HostCapacityVO cap1 = dbf.findByUuid(hostUuid, HostCapacityVO.class);
        sctrl.setSimulatorHostConnectionState(hostUuid, true);
        TimeUnit.SECONDS.sleep(3);
        vm1 = q.find();

        Assert.assertEquals(VmInstanceState.Unknown, vm1.getState());
        Assert.assertEquals(hostUuid, vm1.getHostUuid());
        HostCapacityVO cap2 = dbf.findByUuid(hostUuid, HostCapacityVO.class);
        Assert.assertEquals(cap1.getAvailableMemory(), cap2.getAvailableMemory());
        Assert.assertEquals(cap1.getAvailableCpu(), cap2.getAvailableCpu());

        sctrl.setSimulatorHostConnectionState(hostUuid, false);
        api.reconnectHost(hostUuid);
        TimeUnit.SECONDS.sleep(3);
        vm1 = q.find();
        Assert.assertEquals(VmInstanceState.Running, vm1.getState());
    }
}
