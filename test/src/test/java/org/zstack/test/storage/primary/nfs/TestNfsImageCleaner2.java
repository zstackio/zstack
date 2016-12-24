package org.zstack.test.storage.primary.nfs;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageDeletionPolicyManager.ImageDeletionPolicy;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.ImageCacheVO_;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.image.ImageGlobalConfig;
import org.zstack.simulator.storage.primary.nfs.NfsPrimaryStorageSimulatorConfig;
import org.zstack.storage.primary.PrimaryStorageGlobalConfig;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackendCommands.DeleteCmd;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class TestNfsImageCleaner2 {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    NfsPrimaryStorageSimulatorConfig config;

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
        config = loader.getComponent(NfsPrimaryStorageSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmInstanceInventory vm1 = deployer.vms.get("TestVm");

        DiskOfferingInventory doinv = new DiskOfferingInventory();
        doinv.setName("disk");
        doinv.setDiskSize(SizeUnit.GIGABYTE.toByte(5));
        doinv = api.addDiskOffering(doinv);

        VolumeInventory data = api.createDataVolume("disk", doinv.getUuid());
        api.attachVolumeToVm(vm1.getUuid(), data.getUuid());

        ImageGlobalConfig.DELETION_POLICY.updateValue(ImageDeletionPolicy.Direct.toString());
        ImageInventory image1 = deployer.images.get("TestImage");
        api.deleteImage(image1.getUuid());

        SimpleQuery<ImageCacheVO> q = dbf.createQuery(ImageCacheVO.class);
        q.add(ImageCacheVO_.imageUuid, Op.EQ, image1.getUuid());
        ImageCacheVO c = q.find();

        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicy.Direct.toString());
        api.destroyVmInstance(vm1.getUuid());

        config.deleteCmds.clear();
        PrimaryStorageGlobalConfig.IMAGE_CACHE_GARBAGE_COLLECTOR_INTERVAL.updateValue(1);
        TimeUnit.SECONDS.sleep(3);

        Assert.assertEquals(1, config.deleteCmds.size());
        DeleteCmd cmd = config.deleteCmds.get(0);
        Assert.assertTrue(cmd.isFolder());
        Assert.assertEquals(new File(c.getInstallUrl()).getParent(), cmd.getInstallPath());
        c = q.find();
        Assert.assertNull(c);
    }
}
