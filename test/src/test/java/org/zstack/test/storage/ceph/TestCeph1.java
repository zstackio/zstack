package org.zstack.test.storage.ceph;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.ceph.backup.CephBackupStorageMonVO;
import org.zstack.storage.ceph.backup.CephBackupStorageMonVO_;
import org.zstack.storage.ceph.backup.CephBackupStorageSimulatorConfig;
import org.zstack.storage.ceph.primary.CephPrimaryStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;

/**
 * 1. use ceph for backup storage and primary storage
 * 2. create a vm
 *
 * confirm the vm created successfully
 *
 * 3. delete the ps
 *
 * confirm pools are deleted.
 */
public class TestCeph1 {
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
        bconfig = loader.getComponent(CephBackupStorageSimulatorConfig.class);
        session = api.loginAsAdmin();
    }
    
	@Test
	public void test() throws ApiSenderException {
        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicy.Direct.toString());
        BackupStorageInventory bs = deployer.backupStorages.get("ceph-bk");
        SimpleQuery<CephBackupStorageMonVO> q = dbf.createQuery(CephBackupStorageMonVO.class);
        q.add(CephBackupStorageMonVO_.hostname, SimpleQuery.Op.EQ, "127.0.0.1");
        CephBackupStorageMonVO bsmon = q.find();

        Assert.assertEquals("root", bsmon.getSshUsername());
        Assert.assertEquals("pass@#$word", bsmon.getSshPassword());
        Assert.assertEquals(23, bsmon.getSshPort());

        Assert.assertFalse(config.createSnapshotCmds.isEmpty());
        Assert.assertFalse(config.protectSnapshotCmds.isEmpty());
        Assert.assertFalse(config.cloneCmds.isEmpty());

        VmInstanceInventory vm = deployer.vms.get("TestVm");
        api.destroyVmInstance(vm.getUuid());

        Assert.assertFalse(config.deleteCmds.isEmpty());

        PrimaryStorageInventory ps = deployer.primaryStorages.get("ceph-pri");
        api.deletePrimaryStorage(ps.getUuid());

        Assert.assertTrue(config.deletePoolCmds.isEmpty());

        ImageInventory img = deployer.images.get("TestImage");
        long size = SizeUnit.GIGABYTE.toByte(2);
        bconfig.getImageSizeCmdSize.put(img.getUuid(), size);
        long asize = SizeUnit.GIGABYTE.toByte(1);
        bconfig.getImageSizeCmdActualSize.put(img.getUuid(), asize);
        img = api.syncImageSize(img.getUuid(), null);
        Assert.assertEquals(size, img.getSize());
        Assert.assertEquals(asize, img.getActualSize().longValue());
    }
}
