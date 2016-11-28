package org.zstack.test.storage.backup.sftp;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageDeletionPolicyManager.ImageDeletionPolicy;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.image.ImageGlobalConfig;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

/**
 * 1. add 2 images to sftp backup storage
 * 2. directly delete image1
 * <p>
 * confirm the capacity returned to the backup storage
 * <p>
 * 3. delete image2
 * 4. expunge image2
 * <p>
 * confirm the capacity returned to the backup storage
 */
public class TestSftpBackupStorageExpungeImage {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageExpungeImage.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    GlobalConfigFacade gcf;
    SessionInventory session;
    SftpBackupStorageSimulatorConfig config;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/sftpBackupStorage/TestSftpBackupStorageExpungeImage.xml", con);
        deployer.addSpringConfig("SftpBackupStorage.xml");
        deployer.addSpringConfig("SftpBackupStorageSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(SftpBackupStorageSimulatorConfig.class);
        gcf = loader.getComponent(GlobalConfigFacade.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        BackupStorageInventory sftp = deployer.backupStorages.get("sftp1");
        long size = SizeUnit.GIGABYTE.toByte(1);
        ImageInventory img = new ImageInventory();
        img.setUuid(Platform.getUuid());
        img.setName("image1");
        img.setFormat(VolumeConstant.VOLUME_FORMAT_QCOW2);
        img.setUrl("http://download/image.qcow2");
        img.setSize(size);

        config.imageSizes.put(img.getUuid(), size);
        img = api.addImage(img, sftp.getUuid());

        ImageInventory img1 = new ImageInventory();
        img1.setUuid(Platform.getUuid());
        img1.setName("image1");
        img1.setFormat(VolumeConstant.VOLUME_FORMAT_QCOW2);
        img1.setUrl("http://download/image.qcow2");
        img1.setSize(size);
        img1.setUuid(Platform.getUuid());
        config.imageSizes.put(img1.getUuid(), size);
        img1 = api.addImage(img1, sftp.getUuid());

        BackupStorageVO bs = dbf.findByUuid(sftp.getUuid(), BackupStorageVO.class);
        ImageGlobalConfig.DELETION_POLICY.updateValue(ImageDeletionPolicy.Direct.toString());
        api.deleteImage(img.getUuid());
        TimeUnit.SECONDS.sleep(3);
        BackupStorageVO bs1 = dbf.findByUuid(sftp.getUuid(), BackupStorageVO.class);
        Assert.assertEquals(bs1.getAvailableCapacity(), bs.getAvailableCapacity() + img.getActualSize());

        ImageGlobalConfig.DELETION_POLICY.updateValue(ImageDeletionPolicy.Delay.toString());
        ImageGlobalConfig.EXPUNGE_PERIOD.updateValue(1);
        ImageGlobalConfig.EXPUNGE_INTERVAL.updateValue(1);
        api.deleteImage(img1.getUuid());
        TimeUnit.SECONDS.sleep(3);
        BackupStorageVO bs2 = dbf.findByUuid(sftp.getUuid(), BackupStorageVO.class);
        Assert.assertEquals(bs2.getAvailableCapacity(), bs1.getAvailableCapacity() + img1.getActualSize());
    }
}
