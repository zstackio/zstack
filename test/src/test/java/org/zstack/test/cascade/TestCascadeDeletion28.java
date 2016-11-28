package org.zstack.test.cascade;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.VmCreator;
import org.zstack.test.deployer.Deployer;

/**
 * 1. two l2Networks
 * 2. create vm which has two l3 on two l2
 * 3. detach one l2
 * 4. start vm
 * <p>
 * confirm vm failed to start
 */
public class TestCascadeDeletion28 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/cascade/TestCascade27.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test(expected = ApiSenderException.class)
    public void test() throws ApiSenderException, InterruptedException {
        ImageInventory imageInventory = deployer.images.get("TestImage");
        L3NetworkInventory l31 = deployer.l3Networks.get("TestL3Network1");
        L3NetworkInventory l32 = deployer.l3Networks.get("TestL3Network2");

        InstanceOfferingInventory instanceOffering = deployer.instanceOfferings.get("TestInstanceOffering");

        VmCreator creator = new VmCreator(api);
        creator.addL3Network(l31.getUuid());
        creator.addL3Network(l32.getUuid());
        creator.imageUuid = imageInventory.getUuid();
        creator.instanceOfferingUuid = instanceOffering.getUuid();
        VmInstanceInventory vm = creator.create();

        L2NetworkInventory l21 = deployer.l2Networks.get("TestL2Network1");
        ClusterInventory cluster = deployer.clusters.get("TestCluster1");
        api.detachL2NetworkFromCluster(l21.getUuid(), cluster.getUuid());
        api.startVmInstance(vm.getUuid());
    }
}
