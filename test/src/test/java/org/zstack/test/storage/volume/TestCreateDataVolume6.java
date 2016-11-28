package org.zstack.test.storage.volume;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.storage.primary.PrimaryStorageSystemTags;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * 1. create system tag of primary storage required user tag on disk offering
 * 2. not create the user tag
 * 3. use that disk offering to create data disk
 * <p>
 * confirm data disk creation failure
 */
public class TestCreateDataVolume6 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/volume/TestCreateDataVolume.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test(expected = ApiSenderException.class)
    public void test() throws ApiSenderException, InterruptedException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        DiskOfferingInventory dinv = deployer.diskOfferings.get("TestDataDiskOffering");
        String itag = PrimaryStorageSystemTags.PRIMARY_STORAGE_ALLOCATOR_USERTAG_TAG_MANDATORY.instantiateTag(map(e("tag", "ps1")));
        api.createSystemTag(dinv.getUuid(), itag, DiskOfferingVO.class);

        VolumeInventory vol = api.createDataVolume("data", dinv.getUuid());
        api.attachVolumeToVm(vm.getUuid(), vol.getUuid());
    }
}
