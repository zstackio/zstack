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
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import java.util.List;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * 1 create vm with 3 static IP
 * <p>
 * test static IP apis
 */
public class TestVmStaticIp4 {
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
        final L3NetworkInventory l32 = deployer.l3Networks.get("TestL3Network2");
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

        api.stopVmInstance(vm.getUuid());

        api.deleteStaticIp(vm.getUuid(), l31.getUuid());
        List<String> tags = VmSystemTags.STATIC_IP.getTags(vm.getUuid());
        for (String tag : tags) {
            if (tag.contains(l3Ip1)) {
                Assert.fail(String.format("%s is still there", l3Ip1));
            }
        }

        boolean s = false;
        api.setStaticIp(vm.getUuid(), l31.getUuid(), "10.10.1.102");
        tags = VmSystemTags.STATIC_IP.getTags(vm.getUuid());
        for (String tag : tags) {
            if (tag.contains("10.10.1.102")) {
                s = true;
                break;
            }
        }
        Assert.assertTrue(s);

        api.setStaticIp(vm.getUuid(), l31.getUuid(), "10.10.1.103");
        tags = VmSystemTags.STATIC_IP.getTags(vm.getUuid());
        Assert.assertEquals(3, tags.size());

        // invalid ip
        s = false;
        try {
            api.setStaticIp(vm.getUuid(), l31.getUuid(), "adfadf");
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);

        // delete twice, no error
        api.deleteStaticIp(vm.getUuid(), l32.getUuid());
        api.deleteStaticIp(vm.getUuid(), l32.getUuid());

        // no nic on the l3, deleting static on that l3 fails
        VmNicInventory nic = CollectionUtils.find(vm.getVmNics(), new Function<VmNicInventory, VmNicInventory>() {
            @Override
            public VmNicInventory call(VmNicInventory arg) {
                return arg.getL3NetworkUuid().equals(l32.getUuid()) ? arg : null;
            }
        });
        api.detachNic(nic.getUuid());

        s = false;
        try {
            api.deleteStaticIp(vm.getUuid(), l32.getUuid());
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);
    }
}
