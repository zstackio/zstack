package org.zstack.test.storage.primary.smp;


import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

/**
 * delete primary storage
 */
public class TestDeletePrimaryStorageWhenStillAttached {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestCreateVm.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        ClusterInventory cinv = deployer.clusters.get("TestCluster");
        PrimaryStorageInventory prinv = deployer.primaryStorages.get("TestPrimaryStorage");
        VmGlobalConfig.VM_DELETION_POLICY.updateValue(
                VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy.Direct.toString());

        try {
            api.deletePrimaryStorage(prinv.getUuid());
        } catch (Exception e) {

        }

        api.detachPrimaryStorage(prinv.getUuid(), cinv.getUuid());
        api.deletePrimaryStorage(prinv.getUuid());

    }
}
