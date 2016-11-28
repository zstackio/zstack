package org.zstack.test.storage.primary.local;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;

/**
 * 1. have two hosts with local storage; each host has the capacity only enough to create one vm
 * 2. create a vm1
 * 3. stop the vm1
 * 4. create the vm2 on the host where vm1 created
 * 5. start the vm1
 * <p>
 * confirm the vm1 fails to start
 * <p>
 * 6. stop vm2
 * 7. start vm1
 * <p>
 * confirm the vm1 starts successfully
 */
public class TestLocalStorage14 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/localStorage/TestLocalStorage14.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("localStorageSimulator.xml");
        deployer.addSpringConfig("localStorage.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(LocalStorageSimulatorConfig.class);

        Capacity c = new Capacity();
        c.total = totalSize;
        c.avail = totalSize;

        config.capacityMap.put("host1", c);
        config.capacityMap.put("host2", c);

        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        VmInstanceInventory vm1 = deployer.vms.get("TestVm");
        ImageInventory image = deployer.images.get("TestImage");
        InstanceOfferingInventory ioinv = deployer.instanceOfferings.get("TestInstanceOffering");
        L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");

        VmCreator creator = new VmCreator(api);
        creator.name = "vm2";
        creator.imageUuid = image.getUuid();
        creator.instanceOfferingUuid = ioinv.getUuid();
        creator.addL3Network(l3.getUuid());
        creator.hostUuid = vm1.getHostUuid();

        vm1 = api.stopVmInstance(vm1.getUuid());

        VmInstanceInventory vm2 = creator.create();

        boolean success = false;
        try {
            api.startVmInstance(vm1.getUuid());
        } catch (ApiSenderException e) {
            success = true;
        }

        Assert.assertTrue(success);

        api.stopVmInstance(vm2.getUuid());
        api.startVmInstance(vm1.getUuid());
    }
}
