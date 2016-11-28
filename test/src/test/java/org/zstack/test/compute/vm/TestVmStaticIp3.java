package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.network.l3.UsedIpVO_;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.VmCreator;
import org.zstack.test.deployer.Deployer;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * 1 create vm with 1 static IP
 * 2. delete the static IP, create a new one
 * 3. stop/start the vm
 * <p>
 * confirm the new IP is allocated
 * <p>
 * 4. update the static IP to a new one
 * <p>
 * confirm the new IP is allocated
 * <p>
 * 5. delete the static IP
 * 6. stop/start the vm
 * <p>
 * confirm a new IP is allocated
 * <p>
 * 7. change the static IP to a wrong one
 * <p>
 * confirm the operation failed
 */
public class TestVmStaticIp3 {
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

        VmCreator creator = new VmCreator(api);
        creator.name = "vm1";
        creator.imageUuid = iminv.getUuid();
        creator.instanceOfferingUuid = ioinv.getUuid();
        creator.addL3Network(l31.getUuid());

        String l3Ip1 = "10.10.1.101";
        creator.systemTags.add(VmSystemTags.STATIC_IP.instantiateTag(map(
                e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l31.getUuid()),
                e(VmSystemTags.STATIC_IP_TOKEN, l3Ip1))));

        VmInstanceInventory vm = creator.create();

        SystemTagInventory tag = VmSystemTags.STATIC_IP.getTagInventory(vm.getUuid());
        api.deleteTag(tag.getUuid());

        api.stopVmInstance(vm.getUuid());
        String l3Ip2 = "10.10.1.102";
        api.setStaticIp(vm.getUuid(), l31.getUuid(), l3Ip2);

        vm = api.startVmInstance(vm.getUuid());

        VmNicInventory nic = vm.findNic(l31.getUuid());
        Assert.assertNull(nic.getMetaData());
        Assert.assertEquals(l3Ip2, nic.getIp());
        SimpleQuery<UsedIpVO> q = dbf.createQuery(UsedIpVO.class);
        q.add(UsedIpVO_.ip, Op.EQ, l3Ip1);
        q.add(UsedIpVO_.l3NetworkUuid, Op.EQ, l31.getUuid());
        Assert.assertFalse(q.isExists());

        api.stopVmInstance(vm.getUuid());
        String l3Ip3 = "10.10.1.103";

        api.setStaticIp(vm.getUuid(), l31.getUuid(), l3Ip3);

        vm = api.startVmInstance(vm.getUuid());

        nic = vm.findNic(l31.getUuid());
        Assert.assertNull(nic.getMetaData());
        Assert.assertEquals(l3Ip3, nic.getIp());
        q = dbf.createQuery(UsedIpVO.class);
        q.add(UsedIpVO_.ip, Op.EQ, l3Ip2);
        q.add(UsedIpVO_.l3NetworkUuid, Op.EQ, l31.getUuid());
        Assert.assertFalse(q.isExists());

        tag = VmSystemTags.STATIC_IP.getTagInventory(vm.getUuid());
        api.stopVmInstance(vm.getUuid());
        api.deleteTag(tag.getUuid());
        vm = api.startVmInstance(vm.getUuid());
        nic = vm.findNic(l31.getUuid());
        Assert.assertTrue(nic.getIp() != null);

        boolean s = false;
        api.stopVmInstance(vm.getUuid());
        String wrongIp = "129.12.19.1";
        try {
            api.setStaticIp(vm.getUuid(), l31.getUuid(), wrongIp);
            /*
            api.createSystemTag(vm.getUuid(), VmSystemTags.STATIC_IP.instantiateTag(map(
                    e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l31.getUuid()),
                    e(VmSystemTags.STATIC_IP_TOKEN, l3Ip3)
            )), VmInstanceVO.class);
            */
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);

        // set a correct static IP
        // update a wrong static IP
        // confirm the old static IP is not replaced
        vm = api.stopVmInstance(vm.getUuid());
        nic = vm.findNic(l31.getUuid());
        l3Ip3 = "10.10.1.102";
        api.setStaticIp(vm.getUuid(), l31.getUuid(), l3Ip3);
        // check the old IP is returned
        Assert.assertTrue(api.checkIpAvailability(nic.getL3NetworkUuid(), nic.getIp()));

        try {
            api.setStaticIp(vm.getUuid(), l31.getUuid(), wrongIp);
        } catch (ApiSenderException e) {
            // pass
        }

        tag = VmSystemTags.STATIC_IP.getTagInventory(vm.getUuid());
        Assert.assertTrue(tag.getTag().contains(l3Ip3));
    }
}
