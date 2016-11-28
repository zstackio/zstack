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
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * 1. delete a L3
 * <p>
 * confirm the vm's default L3 is set to NULL, and no nic
 * <p>
 * 2. attach a L3
 * <p>
 * confirm the vm's default L3 is set to the new one, and has nic
 * <p>
 * 3. stop the vm
 * 4. delete a L3
 * <p>
 * confirm the vm's default L3 is set to NULL, and no nic
 * <p>
 * 5. attach a L3
 * <p>
 * confirm the vm's default L3 is set to the new one, and has nic
 */
public class TestDetachNicOnKvm9 {
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
        deployer = new Deployer("deployerXml/kvm/TestDetachNicOnKvm9.xml", con);
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
        final L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        api.deleteL3Network(l3.getUuid());
        VmInstanceVO vmvo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
        Assert.assertNull(vmvo.getDefaultL3NetworkUuid());
        Assert.assertTrue(vmvo.getVmNics().isEmpty());

        L3NetworkInventory l32 = deployer.l3Networks.get("TestL3Network2");
        api.attachNic(vm.getUuid(), l32.getUuid());
        vmvo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
        Assert.assertEquals(l32.getUuid(), vmvo.getDefaultL3NetworkUuid());
        Assert.assertEquals(1, vmvo.getVmNics().size());

        api.stopVmInstance(vmvo.getUuid());
        api.deleteL3Network(l32.getUuid());
        vmvo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
        Assert.assertNull(vmvo.getDefaultL3NetworkUuid());
        Assert.assertTrue(vmvo.getVmNics().isEmpty());

        L3NetworkInventory l33 = deployer.l3Networks.get("TestL3Network3");
        api.attachNic(vm.getUuid(), l33.getUuid());
        vmvo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
        Assert.assertEquals(l33.getUuid(), vmvo.getDefaultL3NetworkUuid());
        Assert.assertEquals(1, vmvo.getVmNics().size());
    }
}
