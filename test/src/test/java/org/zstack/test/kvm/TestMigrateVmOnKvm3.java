package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
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
 * migrate vm to another cluster which doesn't have primary storage vm on attached
 * should fail
 */
public class TestMigrateVmOnKvm3 {
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
        deployer = new Deployer("deployerXml/kvm/TestMigrateVmOnKvm3.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(KVMSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test(expected = ApiSenderException.class)
    public void test() throws ApiSenderException {
        final VmInstanceInventory vm = deployer.vms.get("TestVm");
        HostInventory target = deployer.hosts.get("host2");

        try {
            api.migrateVmInstance(vm.getUuid(), target.getUuid());
        } catch (ApiSenderException e) {
            HostCapacityVO cvo = dbf.findByUuid(vm.getHostUuid(), HostCapacityVO.class);
            Assert.assertTrue(0 != cvo.getUsedCpu());
            Assert.assertTrue(0 != cvo.getUsedMemory());
            VmInstanceVO vo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
            Assert.assertEquals(VmInstanceState.Running, vo.getState());

            HostCapacityVO tvo = dbf.findByUuid(target.getUuid(), HostCapacityVO.class);
            Assert.assertEquals(tvo.getTotalCpu(), tvo.getAvailableCpu());
            Assert.assertEquals(tvo.getTotalMemory(), tvo.getAvailableMemory());
            throw e;
        }
    }
}
