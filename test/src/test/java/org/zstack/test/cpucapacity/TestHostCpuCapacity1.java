package org.zstack.test.cpucapacity;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.allocator.HostCpuOverProvisioningManager;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestHostCpuCapacity1 {
    CLogger logger = Utils.getLogger(TestHostCpuCapacity1.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    HostCpuOverProvisioningManager cpuMgr;

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

        api.stopVmInstance(vm.getUuid());
        cap = dbf.findByUuid(vm.getHostUuid(), HostCapacityVO.class);
        Assert.assertEquals(cap.getTotalCpu(), cap.getAvailableCpu());

        api.startVmInstance(vm.getUuid());
        cap = dbf.findByUuid(vm.getHostUuid(), HostCapacityVO.class);
        Assert.assertEquals(cap.getTotalCpu() - vm.getCpuNum(), cap.getAvailableCpu());

        api.rebootVmInstance(vm.getUuid());
        cap = dbf.findByUuid(vm.getHostUuid(), HostCapacityVO.class);
        Assert.assertEquals(cap.getTotalCpu() - vm.getCpuNum(), cap.getAvailableCpu());

        HostInventory host1 = deployer.hosts.get("host1");
        HostInventory host2 = deployer.hosts.get("host2");
        api.migrateVmInstance(vm.getUuid(), host2.getUuid());
        cap = dbf.findByUuid(host1.getUuid(), HostCapacityVO.class);
        Assert.assertEquals(cap.getTotalCpu(), cap.getAvailableCpu());
        HostCapacityVO cap2 = dbf.findByUuid(host2.getUuid(), HostCapacityVO.class);
        Assert.assertEquals(cap2.getTotalCpu() - vm.getCpuNum(), cap2.getAvailableCpu());

        api.destroyVmInstance(vm.getUuid());
        cap = dbf.findByUuid(host1.getUuid(), HostCapacityVO.class);
        Assert.assertEquals(cap.getTotalCpu(), cap.getAvailableCpu());
        cap2 = dbf.findByUuid(host2.getUuid(), HostCapacityVO.class);
        Assert.assertEquals(cap2.getTotalCpu(), cap2.getAvailableCpu());
    }
}
