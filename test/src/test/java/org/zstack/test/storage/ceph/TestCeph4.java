package org.zstack.test.storage.ceph;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.ceph.primary.CephPrimaryStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

/**
 * 1. use ceph for primary storage and backup storage
 * 2. create a vm
 * 3. create an snapshot from the vm's root volume
 * <p>
 * confirm the snapshot created successfully
 * <p>
 * 4. rollback the snapshot
 * <p>
 * confirm the snapshot is rolled back
 * <p>
 * 5. create an image from the snapshot
 * <p>
 * confirm the image created successfully
 * <p>
 * 6. create a data volume from the snapshot
 * <p>
 * confirm the data volume created successfully
 * <p>
 * 7. delete the snapshot
 * <p>
 * confirm the snapshot deleted successfully
 */
public class TestCeph4 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    CephPrimaryStorageSimulatorConfig config;
    KVMSimulatorConfig kconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/ceph/TestCeph1.xml", con);
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
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        BackupStorageInventory bs = deployer.backupStorages.get("ceph-bk");

        config.createSnapshotCmds.clear();
        VolumeInventory root = vm.getRootVolume();
        VolumeSnapshotInventory sp = api.createSnapshot(root.getUuid());
        Assert.assertFalse(config.createSnapshotCmds.isEmpty());
        Assert.assertTrue(sp.getPrimaryStorageInstallPath().contains("@"));

        api.stopVmInstance(vm.getUuid());
        api.revertVolumeToSnapshot(sp.getUuid());
        Assert.assertFalse(config.rollbackSnapshotCmds.isEmpty());
        VolumeVO rootvo = dbf.findByUuid(root.getUuid(), VolumeVO.class);
        Assert.assertEquals(root.getInstallPath(), rootvo.getInstallPath());

        config.cpCmds.clear();
        ImageInventory image = api.createTemplateFromSnapshot(sp.getUuid(), bs.getUuid());
        Assert.assertFalse(config.cpCmds.isEmpty());

        config.cpCmds.clear();
        api.createDataVolumeFromSnapshot(sp.getUuid());
        Assert.assertFalse(config.cpCmds.isEmpty());

        api.deleteSnapshot(sp.getUuid());
        Assert.assertFalse(config.deleteSnapshotCmds.isEmpty());
        Assert.assertFalse(dbf.isExist(sp.getUuid(), VolumeSnapshotVO.class));
    }
}
