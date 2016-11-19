package org.zstack.test.compute.hostallocator;

import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.cluster.ClusterSystemTags;
import org.zstack.compute.host.HostSystemTags;
import org.zstack.compute.zone.ZoneSystemTags;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.tag.TagInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.kvm.KVMGlobalConfig;
import org.zstack.tag.SystemTagCreator;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * 1.reserve ZoneTag.HOST_RESERVED_CPU_CAPACITY, ClusterTag.HOST_RESERVED_CPU_CAPACITY, HostTag.RESERVED_CPU_CAPACITY, KvmGlobalConfig.RESERVED_CPU_CAPACITY
 * to a small value
 * <p>
 * confirm vm creation succeeds
 */
public class TestReservedHostCapacity7 {
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
        ZoneInventory zone = deployer.zones.values().iterator().next();
        ClusterInventory cluster = deployer.clusters.values().iterator().next();
        HostInventory host = deployer.hosts.values().iterator().next();

        KVMGlobalConfig.RESERVED_CPU_CAPACITY.updateValue(1);
        SystemTagCreator sc = ZoneSystemTags.HOST_RESERVED_CPU_CAPACITY.newSystemTagCreator(zone.getUuid());
        sc.setTagByTokens(map(e("capacity", 1)));
        TagInventory ztag = sc.create();

        sc = ClusterSystemTags.HOST_RESERVED_CPU_CAPACITY.newSystemTagCreator(cluster.getUuid());
        sc.setTagByTokens(map(e("capacity", 1)));
        TagInventory ctag = sc.create();

        sc = HostSystemTags.RESERVED_CPU_CAPACITY.newSystemTagCreator(host.getUuid());
        sc.setTagByTokens(map(e("capacity", 1)));
        TagInventory htag = sc.create();

        L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");
        InstanceOfferingInventory instanceOffering = deployer.instanceOfferings.get("TestInstanceOffering");
        ImageInventory imageInventory = deployer.images.get("TestImage");

        // host tag takes effect
        VmCreator creator = new VmCreator(api);
        creator.timeout = 600;
        creator.addL3Network(l3.getUuid());
        creator.imageUuid = imageInventory.getUuid();
        creator.instanceOfferingUuid = instanceOffering.getUuid();
        VmInstanceInventory vm = creator.create();
        api.destroyVmInstance(vm.getUuid());

        api.deleteTag(htag.getUuid());

        // cluster tag takes effect
        creator = new VmCreator(api);
        creator.timeout = 600;
        creator.addL3Network(l3.getUuid());
        creator.imageUuid = imageInventory.getUuid();
        creator.instanceOfferingUuid = instanceOffering.getUuid();
        vm = creator.create();
        api.deleteTag(ctag.getUuid());
        api.destroyVmInstance(vm.getUuid());

        // zone tag takes effect
        creator = new VmCreator(api);
        creator.timeout = 600;
        creator.addL3Network(l3.getUuid());
        creator.imageUuid = imageInventory.getUuid();
        creator.instanceOfferingUuid = instanceOffering.getUuid();
        vm = creator.create();
        api.destroyVmInstance(vm.getUuid());

        api.deleteTag(ztag.getUuid());

        // kvm global config takes effect
        creator = new VmCreator(api);
        creator.timeout = 600;
        creator.addL3Network(l3.getUuid());
        creator.imageUuid = imageInventory.getUuid();
        creator.instanceOfferingUuid = instanceOffering.getUuid();
        vm = creator.create();
        api.destroyVmInstance(vm.getUuid());
    }
}
