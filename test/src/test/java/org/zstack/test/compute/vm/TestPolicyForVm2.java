package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.*;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.VmCreator;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;

import java.util.ArrayList;

import static org.zstack.utils.CollectionDSL.list;

/**
 * 1. create a deployment owned by the admin account
 * 2. create a normal account
 * 3. share the instance offering/disk offering/image/l3Network to the account
 * <p>
 * confirm the account can create vm using the shared resource
 */
public class TestPolicyForVm2 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestPolicyForVm2.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        InstanceOfferingInventory ioinv = deployer.instanceOfferings.get("TestInstanceOffering");
        ImageInventory img = deployer.images.get("TestImage");
        L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");
        HostInventory host1 = deployer.hosts.get("TestHost1");
        HostInventory host2 = deployer.hosts.get("TestHost2");
        DiskOfferingInventory dov = deployer.diskOfferings.get("TestRootDiskOffering");

        IdentityCreator identityCreator = new IdentityCreator(api);
        AccountInventory test = identityCreator.createAccount("test", "password");

        api.shareResource(
                list(ioinv.getUuid(), l3.getUuid(), img.getUuid(), dov.getUuid()),
                list(test.getUuid()), false
        );

        APIQuerySharedResourceMsg qmsg = new APIQuerySharedResourceMsg();
        qmsg.setConditions(new ArrayList<QueryCondition>());
        APIQuerySharedResourceReply qr = api.query(qmsg, APIQuerySharedResourceReply.class);
        Assert.assertFalse(qr.getInventories().isEmpty());

        SessionInventory session = identityCreator.getAccountSession();

        VmCreator vmCreator = new VmCreator(api);
        vmCreator.imageUuid = img.getUuid();
        vmCreator.addL3Network(l3.getUuid());
        vmCreator.instanceOfferingUuid = ioinv.getUuid();
        vmCreator.session = session;
        vmCreator.hostUuid = host1.getUuid();
        VmInstanceInventory vm = vmCreator.create();

        api.revokeResourceSharing(list(ioinv.getUuid()), list(test.getUuid()), false);

        boolean success = false;
        try {
            vmCreator.create();
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        api.shareResource(list(ioinv.getUuid()), null, true);

        vmCreator.create();

        VolumeInventory data = api.createDataVolume("Data", dov.getUuid(), session);

        api.revokeResourceSharing(list(dov.getUuid()), list(test.getUuid()), true);

        success = false;
        try {
            data = api.createDataVolume("Data", dov.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        api.shareResource(list(dov.getUuid()), null, true);

        data = api.createDataVolume("Data", dov.getUuid(), session);

        success = false;
        try {
            api.deleteDiskOffering(dov.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        api.revokeAllResourceSharing(list(ioinv.getUuid(), dov.getUuid(), l3.getUuid(), img.getUuid()), null);
        long count = dbf.count(SharedResourceVO.class);
        Assert.assertEquals(0, count);
    }
}
