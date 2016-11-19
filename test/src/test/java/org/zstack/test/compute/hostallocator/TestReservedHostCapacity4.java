package org.zstack.test.compute.hostallocator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.zone.ZoneSystemTags;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.HostAllocatorError;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.tag.TagInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.tag.SystemTagCreator;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * 1. set ZoneTag.HOST_RESERVED_CPU_CAPACITY to a big value that makes allocation failure
 * 2. set ZoneTag.HOST_RESERVED_MEMORY_CAPACITY to a big value that makes allocation failure
 * 3. remove all tags
 * <p>
 * confirm in case 1,2 vm creation fails, in case 3 vm creation succeeds
 */
@Deprecated
public class TestReservedHostCapacity4 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/hostAllocator/TestReservedHostCapacity.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException {
        boolean success = false;
        ZoneInventory zone = deployer.zones.values().iterator().next();
        SystemTagCreator sc = ZoneSystemTags.HOST_RESERVED_CPU_CAPACITY.newSystemTagCreator(zone.getUuid());
        sc.setTagByTokens(map(e("capacity", 10*2600L)));
        TagInventory tag = sc.create();

        L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");
        InstanceOfferingInventory instanceOffering = deployer.instanceOfferings.get("TestInstanceOffering");
        ImageInventory imageInventory = deployer.images.get("TestImage");

        try {
            VmCreator creator = new VmCreator(api);
            creator.timeout = 600;
            creator.addL3Network(l3.getUuid());
            creator.imageUuid = imageInventory.getUuid();
            creator.instanceOfferingUuid = instanceOffering.getUuid();
            creator.create();
        } catch (ApiSenderException e) {
            if (e.getError().getCause() != null && HostAllocatorError.NO_AVAILABLE_HOST.toString().equals(e.getError().getCause().getCode())) {
                success = true;
            }
        }

        Assert.assertTrue(success);
        api.deleteTag(tag.getUuid());

        success = false;
        sc = ZoneSystemTags.HOST_RESERVED_MEMORY_CAPACITY.newSystemTagCreator(zone.getUuid());
        sc.setTagByTokens(map(e("capacity", "10G")));
        tag = sc.create();
        try {
            VmCreator creator = new VmCreator(api);
            creator.timeout = 600;
            creator.addL3Network(l3.getUuid());
            creator.imageUuid = imageInventory.getUuid();
            creator.instanceOfferingUuid = instanceOffering.getUuid();
            creator.create();
        } catch (ApiSenderException e) {
            if (e.getError().getCause() != null && HostAllocatorError.NO_AVAILABLE_HOST.toString().equals(e.getError().getCause().getCode())) {
                success = true;
            }
        }

        Assert.assertTrue(success);
        api.deleteTag(tag.getUuid());

        VmCreator creator = new VmCreator(api);
        creator.timeout = 600;
        creator.addL3Network(l3.getUuid());
        creator.imageUuid = imageInventory.getUuid();
        creator.instanceOfferingUuid = instanceOffering.getUuid();
        creator.create();
    }
}
