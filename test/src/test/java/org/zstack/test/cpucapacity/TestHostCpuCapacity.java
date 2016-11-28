package org.zstack.test.cpucapacity;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.host.HostGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.allocator.HostCpuOverProvisioningManager;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

public class TestHostCpuCapacity {
    CLogger logger = Utils.getLogger(TestHostCpuCapacity.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    HostCpuOverProvisioningManager cpuMgr;
    KVMSimulatorConfig kconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/cpuCapacity/TestHostCpuCapacity.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        cpuMgr = loader.getComponent(HostCpuOverProvisioningManager.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        InstanceOfferingInventory ioinv = deployer.instanceOfferings.get("TestInstanceOffering");
        Assert.assertEquals(ioinv.getCpuNum(), vm.getCpuNum().intValue());
        HostCapacityVO cap = dbf.findByUuid(vm.getHostUuid(), HostCapacityVO.class);
        Assert.assertEquals(4, cap.getCpuNum());
        Assert.assertEquals(cpuMgr.calculateHostCpuByRatio(vm.getHostUuid(), 4), cap.getTotalCpu());
        Assert.assertEquals(cap.getTotalCpu() - vm.getCpuNum(), cap.getAvailableCpu());

        kconfig.usedCpu = 4;
        cpuMgr.setGlobalRatio(20);
        api.reconnectHost(vm.getHostUuid());
        TimeUnit.SECONDS.sleep(2);

        cap = dbf.findByUuid(vm.getHostUuid(), HostCapacityVO.class);
        Assert.assertEquals(4, cap.getCpuNum());
        Assert.assertEquals(cpuMgr.calculateHostCpuByRatio(vm.getHostUuid(), 4), cap.getTotalCpu());
        Assert.assertEquals(cap.getTotalCpu() - vm.getCpuNum(), cap.getAvailableCpu());

        HostGlobalConfig.HOST_CPU_OVER_PROVISIONING_RATIO.updateValue(10);
        TimeUnit.SECONDS.sleep(2);
        cap = dbf.findByUuid(vm.getHostUuid(), HostCapacityVO.class);
        Assert.assertEquals(cap.getTotalCpu(), cap.getCpuNum() * HostGlobalConfig.HOST_CPU_OVER_PROVISIONING_RATIO.value(Integer.class));
        Assert.assertEquals(cap.getTotalCpu() - vm.getCpuNum(), cap.getAvailableCpu());
    }
}
