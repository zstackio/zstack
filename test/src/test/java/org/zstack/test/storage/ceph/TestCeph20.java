package org.zstack.test.storage.ceph;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.ceph.backup.CephBackupStorageSimulatorConfig;
import org.zstack.storage.ceph.primary.CephPrimaryStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import java.util.List;

/**
 * 1. test APIGetCandidateBackupStorageForCreatingImageMsg
 */
public class TestCeph20 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    CephPrimaryStorageSimulatorConfig config;
    KVMSimulatorConfig kconfig;
    CephBackupStorageSimulatorConfig bconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/ceph/TestCeph20.xml", con);
        deployer.addSpringConfig("ceph.xml");
        deployer.addSpringConfig("cephSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(CephPrimaryStorageSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        bconfig = loader.getComponent(CephBackupStorageSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        VmInstanceInventory vm1 = deployer.vms.get("TestVm1");
        VmInstanceInventory vm2 = deployer.vms.get("TestVm2");
        BackupStorageInventory sftp = deployer.backupStorages.get("sftp");
        BackupStorageInventory ceph = deployer.backupStorages.get("ceph-bk");

        VolumeInventory root1 = vm1.getRootVolume();
        VolumeInventory root2 = vm2.getRootVolume();

        List<BackupStorageInventory> bs1 = api.getCandidateBackupStorageForCreatingImage(root1.getUuid(), null, null);
        Assert.assertEquals(1, bs1.size());
        Assert.assertEquals(ceph.getUuid(), bs1.get(0).getUuid());

        bs1 = api.getCandidateBackupStorageForCreatingImage(root2.getUuid(), null, null);
        Assert.assertEquals(1, bs1.size());
        Assert.assertEquals(sftp.getUuid(), bs1.get(0).getUuid());

        VolumeSnapshotInventory sp = api.createSnapshot(root1.getUuid());
        bs1 = api.getCandidateBackupStorageForCreatingImage(null, sp.getUuid(), null);
        Assert.assertEquals(1, bs1.size());
        Assert.assertEquals(ceph.getUuid(), bs1.get(0).getUuid());

        sp = api.createSnapshot(root2.getUuid());
        bs1 = api.getCandidateBackupStorageForCreatingImage(null, sp.getUuid(), null);
        Assert.assertEquals(1, bs1.size());
        Assert.assertEquals(sftp.getUuid(), bs1.get(0).getUuid());
    }
}
