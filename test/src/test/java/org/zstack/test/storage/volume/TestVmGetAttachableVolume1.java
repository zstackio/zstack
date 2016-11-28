package org.zstack.test.storage.volume;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

public class TestVmGetAttachableVolume1 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/volume/TestVmGetAttachableVolume.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        DiskOfferingInventory dinv = deployer.diskOfferings.get("TestDataDiskOffering");
        VmInstanceInventory vm = deployer.vms.get("TestVm");

        VmInstanceInventory adminVm = deployer.vms.get("TestVm1");
        VolumeInventory adminVol1 = api.createDataVolume("data1", dinv.getUuid());
        VolumeInventory adminVol2 = api.createDataVolume("data2", dinv.getUuid());
        api.attachVolumeToVm(adminVm.getUuid(), adminVol1.getUuid());
        api.detachVolumeFromVm(adminVol1.getUuid());

        IdentityCreator identityCreator = new IdentityCreator(api);
        AccountInventory test = identityCreator.useAccount("test");
        SessionInventory session = identityCreator.getAccountSession();

        // for account test
        List<VolumeInventory> vols = api.getVmAttachableVolume(vm.getUuid(), session);
        Assert.assertTrue(vols.isEmpty());

        api.shareResource(list(adminVol1.getUuid(), adminVol2.getUuid()), list(test.getUuid()), false);

        vols = api.getVmAttachableVolume(vm.getUuid(), session);
        Assert.assertEquals(2, vols.size());

        VolumeInventory userVol1 = api.createDataVolume("user-data1", dinv.getUuid(), session);
        vols = api.getVmAttachableVolume(vm.getUuid(), session);
        Assert.assertEquals(3, vols.size());

        VolumeInventory userVol2 = api.createDataVolume("user-data2", dinv.getUuid(), session);
        api.attachVolumeToVm(vm.getUuid(), userVol2.getUuid(), session);
        api.detachVolumeFromVm(userVol2.getUuid(), session);
        vols = api.getVmAttachableVolume(vm.getUuid(), session);
        Assert.assertEquals(4, vols.size());

        api.revokeAllResourceSharing(list(adminVol1.getUuid(), adminVol2.getUuid()), null);
        vols = api.getVmAttachableVolume(vm.getUuid(), session);
        Assert.assertEquals(2, vols.size());

        // for admin
        vols = api.getVmAttachableVolume(vm.getUuid());
        Assert.assertEquals(4, vols.size());


        // create a data volume on the primary storage not attached
        // confirm it's not in the attachable candidate list
        PrimaryStorageInventory ps3 = deployer.primaryStorages.get("TestPrimaryStorage2");
        VolumeInventory data3 = api.createDataVolume("user-data3", dinv.getUuid(), ps3.getUuid(), null);
        vols = api.getVmAttachableVolume(vm.getUuid());
        for (VolumeInventory vol : vols) {
            if (vol.getUuid().equals(data3.getUuid())) {
                Assert.fail(String.format("volume %s should not be attachable", data3.getUuid()));
            }
        }
    }
}
