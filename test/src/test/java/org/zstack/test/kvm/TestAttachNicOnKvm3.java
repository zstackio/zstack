package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
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
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

public class TestAttachNicOnKvm3 {
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
        deployer = new Deployer("deployerXml/kvm/TestAttachNicOnKvm3.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(KVMSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        final L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network2");
        VmInstanceInventory vm1 = deployer.vms.get("TestVm");
        VmInstanceInventory vm2 = deployer.vms.get("TestVm1");

        // attach nic with an occupied IP, failure
        VmNicInventory vm2Nic = vm2.getVmNics().get(0);
        boolean s = false;
        try {
            api.attachNic(vm1.getUuid(), l3.getUuid(), vm2Nic.getIp());
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);

        // destroy the vm2 to make the IP available
        api.destroyVmInstance(vm2.getUuid());

        // now attaching should succeed
        vm1 = api.attachNic(vm1.getUuid(), l3.getUuid(), vm2Nic.getIp());
        VmNicInventory nic = CollectionUtils.find(vm1.getVmNics(), new Function<VmNicInventory, VmNicInventory>() {
            @Override
            public VmNicInventory call(VmNicInventory arg) {
                return arg.getL3NetworkUuid().equals(l3.getUuid()) ? arg : null;
            }
        });
        Assert.assertNotNull(nic);
        Assert.assertEquals(vm2Nic.getIp(), nic.getIp());
    }
}
