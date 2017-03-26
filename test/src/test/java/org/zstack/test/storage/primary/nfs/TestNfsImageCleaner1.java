package org.zstack.test.storage.primary.nfs;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageDeletionPolicyManager.ImageDeletionPolicy;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.ImageCacheVO_;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.image.ImageGlobalConfig;
import org.zstack.simulator.storage.primary.nfs.NfsPrimaryStorageSimulatorConfig;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackendCommands.DeleteCmd;
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
 * 1. two NFS storage running two VMs with the same image
 * 2. delete the image and two VMs
 * 3. clean up image cache on the nfs
 * <p>
 * confirm the image cache of the nfs get cleaned up
 * confirm the image cache of the nfs1 doesn't get cleaned up
 */
@Ignore
@Deprecated
// New Groovy Case : CleanImageCacheOnPrimaryStorageTest
public class TestNfsImageCleaner1 {
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
        deployer = new Deployer("deployerXml/nfsPrimaryStorage/TestNfsImageCleaner1.xml", con);
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
        ImageGlobalConfig.DELETION_POLICY.updateValue(ImageDeletionPolicy.Direct.toString());
        ImageInventory image1 = deployer.images.get("TestImage");
        api.deleteImage(image1.getUuid());

        PrimaryStorageInventory nfs = deployer.primaryStorages.get("nfs");
        PrimaryStorageInventory nfs1 = deployer.primaryStorages.get("nfs1");

        SimpleQuery<ImageCacheVO> q = dbf.createQuery(ImageCacheVO.class);
        q.add(ImageCacheVO_.imageUuid, Op.EQ, image1.getUuid());
        q.add(ImageCacheVO_.primaryStorageUuid, Op.EQ, nfs.getUuid());
        ImageCacheVO c = q.find();

        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicy.Direct.toString());
        VmInstanceInventory vm1 = deployer.vms.get("TestVm");
        api.destroyVmInstance(vm1.getUuid());
        VmInstanceInventory vm2 = deployer.vms.get("TestVm1");
        api.destroyVmInstance(vm2.getUuid());

        config.deleteCmds.clear();
        api.cleanupImageCache(nfs.getUuid());
        TimeUnit.SECONDS.sleep(3);

        Assert.assertEquals(1, config.deleteCmds.size());
        DeleteCmd cmd = config.deleteCmds.get(0);
        Assert.assertEquals(c.getInstallUrl(), cmd.getInstallPath());
        c = q.find();
        Assert.assertNull(c);

        q = dbf.createQuery(ImageCacheVO.class);
        q.add(ImageCacheVO_.imageUuid, Op.EQ, image1.getUuid());
        q.add(ImageCacheVO_.primaryStorageUuid, Op.EQ, nfs1.getUuid());
        c = q.find();
        Assert.assertNotNull(c);
    }
}
