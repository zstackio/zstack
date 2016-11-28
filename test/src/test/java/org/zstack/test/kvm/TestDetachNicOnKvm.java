package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.APIGetIpAddressCapacityReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

/**
 * 1. attach a nic to vm
 * 2. detach the nic
 * <p>
 * confirm the nic detached successfully
 */
public class TestDetachNicOnKvm {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    KVMSimulatorConfig config;

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
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        final L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network4");
        VmInstanceInventory vm = deployer.vms.get("TestVm");

        APIGetIpAddressCapacityReply ipcap = api.getIpAddressCapacityByAll();
        long avail1 = ipcap.getAvailableCapacity();

        vm = api.attachNic(vm.getUuid(), l3.getUuid());
        Assert.assertEquals(4, vm.getVmNics().size());

        VmNicInventory nic = vm.getVmNics().get(0);
        vm = api.detachNic(nic.getUuid());
        Assert.assertEquals(3, vm.getVmNics().size());
        Assert.assertFalse(config.detachNicCommands.isEmpty());

        TimeUnit.SECONDS.sleep(3);
        ipcap = api.getIpAddressCapacityByAll();
        long avail2 = ipcap.getAvailableCapacity();

        Assert.assertEquals(avail1, avail2);

        String l3Uuid = nic.getL3NetworkUuid();
        nic = vm.findNic(l3Uuid);
        Assert.assertNull(nic);

        api.stopVmInstance(vm.getUuid());
        vm = api.startVmInstance(vm.getUuid());
        Assert.assertEquals(3, vm.getVmNics().size());
        nic = vm.findNic(l3Uuid);
        Assert.assertNull(nic);
    }
}
