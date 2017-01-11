package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.storage.primary.nfs.NfsPrimaryStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

public class TestDeleteZoneForDeletedVm {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    KVMSimulatorConfig config;
    NfsPrimaryStorageSimulatorConfig nconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestAttachVolumeToVmOnKvm.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(KVMSimulatorConfig.class);
        nconfig = loader.getComponent(NfsPrimaryStorageSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        ZoneInventory zone = deployer.zones.get("Zone1");

        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy.Never);
        api.destroyVmInstance(vm.getUuid());
        TimeUnit.SECONDS.sleep(3);

        api.deleteZone(zone.getUuid());

        Assert.assertTrue(0 == Q.New(VmInstanceVO.class).count());
    }
}
