package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.VmCreator;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

public class TestVmGetAttachableL3Networks {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestVmGetAttachableL3Networks.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmInstanceInventory cvm = deployer.vms.get("TestVm");
        List<L3NetworkInventory> cl3s = api.getVmAttachableL3Networks(cvm.getUuid());
        Assert.assertTrue(cl3s.isEmpty());

        IdentityCreator identityCreator = new IdentityCreator(api);
        AccountInventory account1 = identityCreator.useAccount("test");
        SessionInventory session1 = identityCreator.getAccountSession();

        IdentityCreator identityCreator1 = new IdentityCreator(api);
        AccountInventory account2 = identityCreator1.createAccount("account2", "password");
        SessionInventory session2 = identityCreator1.getAccountSession();

        InstanceOfferingInventory ioinv = deployer.instanceOfferings.get("TestInstanceOffering");
        ImageInventory image = deployer.images.get("TestImage");
        L3NetworkInventory l31 = deployer.l3Networks.get("TestL3Network1");
        L3NetworkInventory l32 = deployer.l3Networks.get("TestL3Network2");

        L2NetworkInventory l2 = deployer.l2Networks.get("TestL2Network");

        api.shareResource(list(ioinv.getUuid(), image.getUuid(), l31.getUuid()), list(account2.getUuid()), false, session1);

        VmCreator vmCreator = new VmCreator(api);
        vmCreator.instanceOfferingUuid = ioinv.getUuid();
        vmCreator.imageUuid = image.getUuid();
        vmCreator.addL3Network(l31.getUuid());
        vmCreator.session = session2;
        VmInstanceInventory vm = vmCreator.create();

        List<L3NetworkInventory> l3s = api.getVmAttachableL3Networks(vm.getUuid(), session2);
        Assert.assertEquals(0, l3s.size());

        api.shareResource(list(l32.getUuid()), list(account2.getUuid()), false, session1);

        l3s = api.getVmAttachableL3Networks(vm.getUuid(), session2);
        Assert.assertEquals(1, l3s.size());
        for (L3NetworkInventory l3 : l3s) {
            Assert.assertFalse(l3.getUuid().equals(l31.getUuid()));
        }

        api.createL3BasicNetwork(l2.getUuid(), session2);

        l3s = api.getVmAttachableL3Networks(vm.getUuid(), session2);
        Assert.assertEquals(2, l3s.size());
        for (L3NetworkInventory l3 : l3s) {
            Assert.assertFalse(l3.getUuid().equals(l31.getUuid()));
        }

        api.revokeAllResourceSharing(list(l32.getUuid()), session1);

        l3s = api.getVmAttachableL3Networks(vm.getUuid(), session2);
        Assert.assertEquals(1, l3s.size());
        for (L3NetworkInventory l3 : l3s) {
            Assert.assertFalse(l3.getUuid().equals(l31.getUuid()));
        }

        // for admin
        l3s = api.getVmAttachableL3Networks(vm.getUuid());
        Assert.assertEquals(3, l3s.size());
        for (L3NetworkInventory l3 : l3s) {
            Assert.assertFalse(l3.getUuid().equals(l31.getUuid()));
        }

        for (VmNicInventory nic : vm.getVmNics()) {
            api.detachNic(nic.getUuid());
        }

        l3s = api.getVmAttachableL3Networks(vm.getUuid());
        Assert.assertEquals(4, l3s.size());

        api.attachNic(vm.getUuid(), l31.getUuid());
        l3s = api.getVmAttachableL3Networks(vm.getUuid());
        for (L3NetworkInventory l3 : l3s) {
            Assert.assertFalse(l3.getUuid().equals(l31.getUuid()));
        }

        l3s = api.getInterdependentL3NetworksByImageUuid(vm.getImageUuid(), vm.getZoneUuid(), session1);
        Assert.assertEquals(3, l3s.size());

        l3s = api.getInterdependentL3NetworksByImageUuid(vm.getImageUuid(), vm.getZoneUuid(), session2);
        Assert.assertEquals(2, l3s.size());
    }
}
