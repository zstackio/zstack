package org.zstack.test.image;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.Quota;
import org.zstack.header.identity.QuotaInventory;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import java.util.List;

/**
 * test vm quota usage
 */
public class TestQuotaUsageForImage {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/image/TestImageQuota.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        InstanceOfferingInventory ioinv = deployer.instanceOfferings.get("TestInstanceOffering");
        ImageInventory img = deployer.images.get("TestImage");
        L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");
        DiskOfferingInventory disk = deployer.diskOfferings.get("disk50G");

        IdentityCreator identityCreator = new IdentityCreator(api);
        AccountInventory test = identityCreator.useAccount("test");

        api.updateQuota(test.getUuid(), ImageConstant.QUOTA_IMAGE_NUM, 2);
        //
        BackupStorageInventory inv = deployer.backupStorages.get("TestBackupStorage");
        //add image
        ImageInventory iinv = new ImageInventory();
        iinv.setName("Test Image1");
        iinv.setDescription("Test Image1");
        iinv.setMediaType(ImageConstant.ImageMediaType.RootVolumeTemplate.toString());
        iinv.setGuestOsType("Window7");
        iinv.setFormat("simulator");
        iinv.setUrl("http://192.168.200.1/mirror/diskimages/zstack-vr.qcow2");
        iinv = api.addImage(iinv, identityCreator.getAccountSession(), inv.getUuid());
        //add image
        iinv = new ImageInventory();
        iinv.setName("Test Image2");
        iinv.setDescription("Test Image2");
        iinv.setMediaType(ImageConstant.ImageMediaType.RootVolumeTemplate.toString());
        iinv.setGuestOsType("Window7");
        iinv.setFormat("simulator");
        iinv.setUrl("http://192.168.200.1/mirror/diskimages/blank-test.img");
        iinv = api.addImage(iinv, identityCreator.getAccountSession(), inv.getUuid());
        //add image
        iinv = new ImageInventory();
        iinv.setName("Test Image21");
        iinv.setDescription("Test Image21");
        iinv.setMediaType(ImageConstant.ImageMediaType.RootVolumeTemplate.toString());
        iinv.setGuestOsType("Window7");
        iinv.setFormat("simulator");
        iinv.setUrl("http://192.168.200.1/mirror/diskimages/zstack-image-0.0.7.qcow2");

        thrown.expect(ApiSenderException.class);
        thrown.expectMessage("The user exceeds a quota of a resource");
        thrown.expectMessage(ImageConstant.QUOTA_IMAGE_NUM);

        iinv = api.addImage(iinv, identityCreator.getAccountSession(), inv.getUuid());
        //
        List<Quota.QuotaUsage> usages = api.getQuotaUsage(test.getUuid(), null);
        //
        Quota.QuotaUsage imageNum = CollectionUtils.find(usages, new Function<Quota.QuotaUsage, Quota.QuotaUsage>() {
            @Override
            public Quota.QuotaUsage call(Quota.QuotaUsage arg) {
                return ImageConstant.QUOTA_IMAGE_NUM.equals(arg.getName()) ? arg : null;
            }
        });
        Assert.assertNotNull(imageNum);
        QuotaInventory qvm = api.getQuota(ImageConstant.QUOTA_IMAGE_NUM, test.getUuid(), identityCreator.getAccountSession());
        Assert.assertEquals(qvm.getValue(), imageNum.getTotal().longValue());
        Assert.assertEquals(2, imageNum.getUsed().longValue());
        //
        Quota.QuotaUsage imageSize = CollectionUtils.find(usages, new Function<Quota.QuotaUsage, Quota.QuotaUsage>() {
            @Override
            public Quota.QuotaUsage call(Quota.QuotaUsage arg) {
                return ImageConstant.QUOTA_IMAGE_SIZE.equals(arg.getName()) ? arg : null;
            }
        });
        Assert.assertNotNull(imageSize);
        qvm = api.getQuota(ImageConstant.QUOTA_IMAGE_SIZE, test.getUuid(), identityCreator.getAccountSession());
        Assert.assertEquals(qvm.getValue(), imageSize.getTotal().longValue());
        Assert.assertTrue(imageSize.getUsed().longValue() <= imageSize.getTotal().longValue());

    }
}

