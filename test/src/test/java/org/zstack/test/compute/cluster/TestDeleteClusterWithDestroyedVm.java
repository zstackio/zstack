package org.zstack.test.compute.cluster;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.network.l3.APIGetIpAddressCapacityReply;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.header.vm.*;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.storage.volume.VolumeGlobalConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.concurrent.TimeUnit;

public class TestDeleteClusterWithDestroyedVm {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestDestroyVm.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmInstanceInventory vm1 = deployer.vms.get("TestVm");
        api.destroyVmInstance(vm1.getUuid());
        VmInstanceVO vmvo1 = dbf.findByUuid(vm1.getUuid(), VmInstanceVO.class);
        Assert.assertNotNull(vmvo1);
        Assert.assertEquals(VmInstanceState.Destroyed, vmvo1.getState());

        ClusterInventory clusterInv1 = deployer.clusters.get("TestCluster");
        api.deleteCluster(clusterInv1.getUuid());
        ClusterVO clusterVO1 = dbf.findByUuid(clusterInv1.getUuid(), ClusterVO.class);
        Assert.assertNull(clusterVO1);
    }
}
