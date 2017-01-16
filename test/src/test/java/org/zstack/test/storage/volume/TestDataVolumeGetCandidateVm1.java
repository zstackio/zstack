package org.zstack.test.storage.volume;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

public class TestDataVolumeGetCandidateVm1 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/volume/TestDataVolumeGetCandidateVm1.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        DiskOfferingInventory dinv = deployer.diskOfferings.get("TestDataDiskOffering");

        IdentityCreator identityCreator = new IdentityCreator(api);
        AccountInventory account2 = identityCreator.createAccount("account2", "password");
        SessionInventory session2 = identityCreator.getAccountSession();

        IdentityCreator identityCreator1 = new IdentityCreator(api);
        AccountInventory account1 = identityCreator.useAccount("test");
        SessionInventory session1 = identityCreator1.getAccountSession();

        api.shareResource(list(dinv.getUuid()), list(account2.getUuid()), false, session1);

        // for account2
        VolumeInventory account2Data1 = api.createDataVolume("account2-data1", dinv.getUuid(), session2);
        List<VmInstanceInventory> vms = api.getDataVolumeCandidateVmForAttaching(account2Data1.getUuid(), session2);
        Assert.assertEquals(0, vms.size());

        // for account1
        VolumeInventory account1Data1 = api.createDataVolume("account1-data1", dinv.getUuid(), session1);
        vms = api.getDataVolumeCandidateVmForAttaching(account1Data1.getUuid(), session1);
        Assert.assertEquals(1, vms.size());

        api.attachVolumeToVm(vms.get(0).getUuid(), account1Data1.getUuid());
        vms = api.getDataVolumeCandidateVmForAttaching(account1Data1.getUuid(), session1);
        Assert.assertEquals(0, vms.size());

        // for admin
        VolumeInventory adminData1 = api.createDataVolume("admin-data1", dinv.getUuid());
        vms = api.getDataVolumeCandidateVmForAttaching(adminData1.getUuid(), session1);
        Assert.assertEquals(1, vms.size());
    }
}
