package org.zstack.test.cascade;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.storage.primary.PrimaryStorageSystemTags;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.VmCreator;
import org.zstack.test.deployer.Deployer;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * 1. two primary storage
 * 2. create vm whose root volume and data volume are on different primary storage
 * 3. detach primary storage ps1 where vm's data volume is on
 * 4. attach ps1 to another cluster(TestCluster1)
 * 5. start vm
 * <p>
 * confirm vm started in TestCluster1
 */
public class TestCascadeDeletion26 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/cascade/TestCascade24.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        ImageInventory imageInventory = deployer.images.get("TestImage");
        L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");

        PrimaryStorageInventory ps = deployer.primaryStorages.get("TestPrimaryStorage");
        PrimaryStorageInventory ps1 = deployer.primaryStorages.get("TestPrimaryStorage1");

        String itag = PrimaryStorageSystemTags.PRIMARY_STORAGE_ALLOCATOR_UUID_TAG.instantiateTag(map(e("uuid", ps1.getUuid())));
        DiskOfferingInventory dinv = deployer.diskOfferings.get("TestDataDiskOffering");
        api.createSystemTag(dinv.getUuid(), itag, DiskOfferingVO.class);

        InstanceOfferingInventory instanceOffering = deployer.instanceOfferings.get("TestInstanceOffering");
        itag = PrimaryStorageSystemTags.PRIMARY_STORAGE_ALLOCATOR_UUID_TAG.instantiateTag(map(e("uuid", ps.getUuid())));
        api.createSystemTag(instanceOffering.getUuid(), itag, InstanceOfferingVO.class);

        VmCreator creator = new VmCreator(api);
        creator.addL3Network(l3.getUuid());
        creator.imageUuid = imageInventory.getUuid();
        creator.instanceOfferingUuid = instanceOffering.getUuid();
        creator.addDisk(dinv.getUuid());
        VmInstanceInventory vm = creator.create();

        L2NetworkInventory l21 = deployer.l2Networks.get("TestL2Network1");

        ClusterInventory cluster = deployer.clusters.get("TestCluster");


        api.detachPrimaryStorage(ps1.getUuid(), cluster.getUuid());
        VmInstanceVO vmvo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
        Assert.assertEquals(VmInstanceState.Stopped, vmvo.getState());

        ClusterInventory cluster1 = deployer.clusters.get("TestCluster1");

        api.attachPrimaryStorage(cluster1.getUuid(), ps1.getUuid());

        vm = api.startVmInstance(vmvo.getUuid());

        Assert.assertEquals(VmInstanceState.Running.toString(), vm.getState());
        Assert.assertEquals(vm.getClusterUuid(), cluster1.getUuid());
    }
}
