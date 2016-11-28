package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.image.ImageDeletionPolicyManager.ImageDeletionPolicy;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.VolumeDeletionPolicyManager.VolumeDeletionPolicy;
import org.zstack.header.volume.VolumeVO;
import org.zstack.image.ImageGlobalConfig;
import org.zstack.storage.volume.VolumeGlobalConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.VmCreator;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;

/**
 * 1. delete the account
 * <p>
 * confirm resources created by the account are deleted
 */
public class TestPolicyForVm4 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestPolicyForVm3.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicy.Direct.toString());
        VolumeGlobalConfig.VOLUME_DELETION_POLICY.updateValue(VolumeDeletionPolicy.Direct.toString());
        ImageGlobalConfig.DELETION_POLICY.updateValue(ImageDeletionPolicy.Direct.toString());

        InstanceOfferingInventory ioinv = deployer.instanceOfferings.get("TestInstanceOffering");
        ImageInventory img = deployer.images.get("TestImage");
        L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");
        DiskOfferingInventory dov = deployer.diskOfferings.get("disk50G");

        IdentityCreator identityCreator = new IdentityCreator(api);
        AccountInventory test = identityCreator.useAccount("test");

        VmCreator vmCreator = new VmCreator(api);
        vmCreator.imageUuid = img.getUuid();
        vmCreator.addL3Network(l3.getUuid());
        vmCreator.addDisk(dov.getUuid());
        vmCreator.instanceOfferingUuid = ioinv.getUuid();
        vmCreator.name = "vm";
        vmCreator.session = identityCreator.getAccountSession();
        vmCreator.create();

        api.deleteAccount(test.getUuid(), identityCreator.getAccountSession());

        long count = dbf.count(VmInstanceVO.class);
        Assert.assertEquals(0, count);
        count = dbf.count(InstanceOfferingVO.class);
        Assert.assertEquals(0, count);
        count = dbf.count(DiskOfferingVO.class);
        Assert.assertEquals(0, count);
        count = dbf.count(ImageVO.class);
        Assert.assertEquals(0, count);
        count = dbf.count(L3NetworkVO.class);
        Assert.assertEquals(0, count);
        count = dbf.count(VolumeVO.class);
        Assert.assertEquals(0, count);
    }
}

