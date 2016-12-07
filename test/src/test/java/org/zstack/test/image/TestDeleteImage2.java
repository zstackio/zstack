package org.zstack.test.image;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.image.*;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageDeletionPolicyManager.ImageDeletionPolicy;
import org.zstack.header.simulator.SimulatorConstant;
import org.zstack.header.simulator.storage.backup.SimulatorBackupStorageDetails;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.image.ImageGlobalConfig;
import org.zstack.test.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.list;

public class TestDeleteImage2 {
    CLogger logger = Utils.getLogger(TestDeleteImage2.class);
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("Simulator.xml").addXml("BackupStorageManager.xml")
                .addXml("ImageManager.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        api = new Api();
        api.startServer();
    }

    @After
    public void tearDown() throws Exception {
        api.stopServer();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        SimulatorBackupStorageDetails ss = new SimulatorBackupStorageDetails();
        ss.setTotalCapacity(SizeUnit.GIGABYTE.toByte(100));
        ss.setUsedCapacity(0);
        ss.setUrl("nfs://simulator/backupstorage/");
        final BackupStorageInventory bs1 = api.createSimulatorBackupStorage(1, ss).get(0);
        BackupStorageInventory bs2 = api.createSimulatorBackupStorage(1, ss).get(0);
        List<String> bsUuids = list(bs1.getUuid(), bs2.getUuid());

        ImageInventory iinv = new ImageInventory();
        iinv.setName("Test Image");
        iinv.setDescription("Test Image");
        iinv.setMediaType(ImageMediaType.RootVolumeTemplate.toString());
        iinv.setGuestOsType("Window7");
        iinv.setFormat(SimulatorConstant.SIMULATOR_VOLUME_FORMAT_STRING);
        iinv.setUrl("http://zstack.org/download/win7.qcow2");
        iinv = api.addImage(iinv, bsUuids.toArray(new String[bsUuids.size()]));

        api.deleteImage(iinv.getUuid(), list(bs1.getUuid()));
        ImageVO img = dbf.findByUuid(iinv.getUuid(), ImageVO.class);
        Assert.assertNotNull(img);
        Assert.assertEquals(ImageStatus.Ready, img.getStatus());
        Assert.assertEquals(2, img.getBackupStorageRefs().size());

        ImageBackupStorageRefVO ref1 = CollectionUtils.find(img.getBackupStorageRefs(), new Function<ImageBackupStorageRefVO, ImageBackupStorageRefVO>() {
            @Override
            public ImageBackupStorageRefVO call(ImageBackupStorageRefVO arg) {
                return arg.getBackupStorageUuid().equals(bs1.getUuid()) ? arg : null;
            }
        });
        Assert.assertNotNull(ref1);
        Assert.assertEquals(ImageStatus.Deleted, ref1.getStatus());

        ImageGlobalConfig.EXPUNGE_PERIOD.updateValue(1);
        ImageGlobalConfig.EXPUNGE_INTERVAL.updateValue(1);
        TimeUnit.SECONDS.sleep(3);
        img = dbf.findByUuid(iinv.getUuid(), ImageVO.class);
        Assert.assertNotNull(img);
        Assert.assertEquals(ImageStatus.Ready, img.getStatus());
        Assert.assertEquals(1, img.getBackupStorageRefs().size());
        ref1 = CollectionUtils.find(img.getBackupStorageRefs(), new Function<ImageBackupStorageRefVO, ImageBackupStorageRefVO>() {
            @Override
            public ImageBackupStorageRefVO call(ImageBackupStorageRefVO arg) {
                return arg.getBackupStorageUuid().equals(bs1.getUuid()) ? arg : null;
            }
        });
        Assert.assertNull(ref1);

        ImageGlobalConfig.DELETION_POLICY.updateValue(ImageDeletionPolicy.Direct.toString());
        api.deleteImage(iinv.getUuid(), list(bs2.getUuid()));
        img = dbf.findByUuid(iinv.getUuid(), ImageVO.class);
        Assert.assertNull(img);
        long count = dbf.count(ImageBackupStorageRefVO.class);
        Assert.assertEquals(0, count);

        ImageGlobalConfig.DELETION_POLICY.updateValue(ImageDeletionPolicy.Delay.toString());
        ImageGlobalConfig.EXPUNGE_PERIOD.updateValue(1000);
        ImageGlobalConfig.EXPUNGE_INTERVAL.updateValue(1000);
        TimeUnit.SECONDS.sleep(2);
        iinv = api.addImage(iinv, bsUuids.toArray(new String[bsUuids.size()]));
        api.deleteImage(iinv.getUuid());
        img = dbf.findByUuid(iinv.getUuid(), ImageVO.class);
        Assert.assertNotNull(img);
        Assert.assertEquals(ImageStatus.Deleted, img.getStatus());
        Assert.assertEquals(2, img.getBackupStorageRefs().size());
        for (ImageBackupStorageRefVO ref : img.getBackupStorageRefs()) {
            if (ref.getStatus() != ImageStatus.Deleted) {
                Assert.fail(String.format("ref[status:%s], ref.backupStorageUuid= %s", ref.getStatus(), ref.getBackupStorageUuid()));
            }
        }

        api.expungeImage(img.getUuid(), list(bs1.getUuid()), null);
        img = dbf.findByUuid(iinv.getUuid(), ImageVO.class);
        Assert.assertNotNull(img);
        Assert.assertEquals(ImageStatus.Deleted, img.getStatus());
        Assert.assertEquals(1, img.getBackupStorageRefs().size());
        ref1 = CollectionUtils.find(img.getBackupStorageRefs(), new Function<ImageBackupStorageRefVO, ImageBackupStorageRefVO>() {
            @Override
            public ImageBackupStorageRefVO call(ImageBackupStorageRefVO arg) {
                return arg.getBackupStorageUuid().equals(bs1.getUuid()) ? arg : null;
            }
        });
        Assert.assertNull(ref1);
        ImageGlobalConfig.EXPUNGE_PERIOD.updateValue(1);
        ImageGlobalConfig.EXPUNGE_INTERVAL.updateValue(1);
        TimeUnit.SECONDS.sleep(3);

        img = dbf.findByUuid(iinv.getUuid(), ImageVO.class);
        Assert.assertNull(img);
        count = dbf.count(ImageBackupStorageRefVO.class);
        Assert.assertEquals(0, count);

        iinv.setUuid(Platform.getUuid());
        ImageGlobalConfig.DELETION_POLICY.updateValue(ImageDeletionPolicy.Never.toString());
        iinv = api.addImage(iinv, bsUuids.toArray(new String[bsUuids.size()]));
        api.deleteImage(iinv.getUuid());
        TimeUnit.SECONDS.sleep(3);
        img = dbf.findByUuid(iinv.getUuid(), ImageVO.class);
        Assert.assertNotNull(img);
        Assert.assertEquals(ImageStatus.Deleted, img.getStatus());
        Assert.assertEquals(2, img.getBackupStorageRefs().size());
        for (ImageBackupStorageRefVO ref : img.getBackupStorageRefs()) {
            if (ref.getStatus() != ImageStatus.Deleted) {
                Assert.fail(String.format("ref[status:%s], ref.backupStorageUuid= %s", ref.getStatus(), ref.getBackupStorageUuid()));
            }
        }

        ImageGlobalConfig.DELETION_POLICY.updateValue(ImageDeletionPolicy.Delay.toString());
        TimeUnit.SECONDS.sleep(3);
        img = dbf.findByUuid(iinv.getUuid(), ImageVO.class);
        Assert.assertNull(img);
        count = dbf.count(ImageBackupStorageRefVO.class);
        Assert.assertEquals(0, count);

        ImageGlobalConfig.EXPUNGE_PERIOD.updateValue(1000);
        ImageGlobalConfig.EXPUNGE_INTERVAL.updateValue(1000);
        TimeUnit.SECONDS.sleep(2);
        iinv.setUuid(Platform.getUuid());
        iinv = api.addImage(iinv, bsUuids.toArray(new String[bsUuids.size()]));
        api.deleteImage(iinv.getUuid(), list(bs1.getUuid()));
        iinv = api.recoverImage(iinv.getUuid(), list(bs1.getUuid()), null);
        Assert.assertEquals(2, iinv.getBackupStorageRefs().size());
        for (ImageBackupStorageRefInventory ref : iinv.getBackupStorageRefs()) {
            Assert.assertEquals(ImageStatus.Ready.toString(), ref.getStatus());
        }
        Assert.assertEquals(ImageStatus.Ready.toString(), iinv.getStatus());

        api.deleteImage(iinv.getUuid());
        iinv = api.recoverImage(iinv.getUuid(), null, null);
        Assert.assertEquals(2, iinv.getBackupStorageRefs().size());
        for (ImageBackupStorageRefInventory ref : iinv.getBackupStorageRefs()) {
            Assert.assertEquals(ImageStatus.Ready.toString(), ref.getStatus());
        }
        Assert.assertEquals(ImageStatus.Ready.toString(), iinv.getStatus());
    }
}
