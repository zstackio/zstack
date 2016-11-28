package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.VmCreator;
import org.zstack.test.deployer.Deployer;

import java.util.List;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * 1 create vm with 3 static IP
 * <p>
 * confirm three static IP are correctly allocated
 * <p>
 * 2. delete l3 network1
 * <p>
 * confirm the static ip tag of l3 network1 deleted
 * <p>
 * 3. stop/start the vm
 * <p>
 * confirm the vm starts/stops successfully
 */
public class TestVmStaticIp {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestVmStaticIp.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        InstanceOfferingInventory ioinv = deployer.instanceOfferings.get("TestInstanceOffering");
        ImageInventory iminv = deployer.images.get("TestImage");
        L3NetworkInventory l31 = deployer.l3Networks.get("TestL3Network1");
        L3NetworkInventory l32 = deployer.l3Networks.get("TestL3Network2");
        L3NetworkInventory l33 = deployer.l3Networks.get("TestL3Network3");

        VmCreator creator = new VmCreator(api);
        creator.name = "vm1";
        creator.imageUuid = iminv.getUuid();
        creator.instanceOfferingUuid = ioinv.getUuid();
        creator.addL3Network(l31.getUuid());
        creator.addL3Network(l32.getUuid());
        creator.addL3Network(l33.getUuid());

        String l3Ip1 = "10.10.1.101";
        creator.systemTags.add(VmSystemTags.STATIC_IP.instantiateTag(map(
                e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l31.getUuid()),
                e(VmSystemTags.STATIC_IP_TOKEN, l3Ip1))));

        String l3Ip2 = "10.10.2.101";
        creator.systemTags.add(VmSystemTags.STATIC_IP.instantiateTag(map(
                e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l32.getUuid()),
                e(VmSystemTags.STATIC_IP_TOKEN, l3Ip2))));

        String l3Ip3 = "10.20.3.101";
        creator.systemTags.add(VmSystemTags.STATIC_IP.instantiateTag(map(
                e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l33.getUuid()),
                e(VmSystemTags.STATIC_IP_TOKEN, l3Ip3))));

        VmInstanceInventory vm = creator.create();

        for (VmNicInventory nic : vm.getVmNics()) {
            if (nic.getL3NetworkUuid().equals(l31.getUuid())) {
                Assert.assertEquals(l3Ip1, nic.getIp());
            } else if (nic.getL3NetworkUuid().equals(l32.getUuid())) {
                Assert.assertEquals(l3Ip2, nic.getIp());
            } else if (nic.getL3NetworkUuid().equals(l33.getUuid())) {
                Assert.assertEquals(l3Ip3, nic.getIp());
            }
        }

        api.deleteL3Network(l31.getUuid());
        List<String> tags = VmSystemTags.STATIC_IP.getTags(vm.getUuid());
        for (String t : tags) {
            if (t.contains(l3Ip1)) {
                Assert.fail("static ip tag is still on l3 network1");
            }
        }

        api.stopVmInstance(vm.getUuid());
        api.startVmInstance(vm.getUuid());
    }
}
