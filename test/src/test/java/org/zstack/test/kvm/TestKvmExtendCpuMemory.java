package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.allocator.HostCpuOverProvisioningManager;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

/**
 * 1. extend memory/cpu of the host
 * 2. reconnect the host
 * <p>
 * confirm the host capacity is extended
 */
public class TestKvmExtendCpuMemory {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    KVMSimulatorConfig config;
    HostCpuOverProvisioningManager cpuMgr;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestCreateVmOnKvm.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(KVMSimulatorConfig.class);
        cpuMgr = loader.getComponent(HostCpuOverProvisioningManager.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        long expandedMem = SizeUnit.GIGABYTE.toGigaByte(10);
        config.totalMemory += expandedMem;
        int expandedCpuNum = 10;
        config.cpuNum += expandedCpuNum;
        config.usedCpu = vm.getCpuNum();

        HostInventory host1 = deployer.hosts.get("host1");
        HostCapacityVO cap1 = dbf.findByUuid(host1.getUuid(), HostCapacityVO.class);

        api.reconnectHost(host1.getUuid());
        TimeUnit.SECONDS.sleep(3);
        HostCapacityVO cap = dbf.findByUuid(host1.getUuid(), HostCapacityVO.class);
        Assert.assertEquals(cap.getTotalCpu(), cpuMgr.calculateHostCpuByRatio(host1.getUuid(), (int) config.cpuNum));
        Assert.assertEquals(cap.getAvailableCpu(), cap1.getAvailableCpu() + cpuMgr.calculateHostCpuByRatio(host1.getUuid(), expandedCpuNum));
        Assert.assertEquals(cap.getTotalMemory(), config.totalMemory);
        Assert.assertEquals(cap.getAvailableMemory(), cap1.getAvailableMemory() + expandedMem);
        Assert.assertEquals(cap.getTotalPhysicalMemory(), config.totalMemory);
    }

}
