package org.zstack.test.cascade;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.appliancevm.ApplianceVmVO;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

/**
 * 1. have two clusters
 * 2. create a vm with virtual router
 * 3. detach l2 which vr has l3 on
 * <p>
 * confirm vr is migrated to another cluster
 */
public class TestCascadeDeletion30 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/cascade/TestCascade30.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        ClusterInventory cluster1 = deployer.clusters.get("Cluster1");
        ClusterInventory cluster2 = deployer.clusters.get("Cluster2");
        ApplianceVmVO vr = dbf.listAll(ApplianceVmVO.class).get(0);
        String lastClusterUuid = vr.getClusterUuid();
        String targetClusterUuid = cluster1.getUuid().equals(lastClusterUuid) ? cluster2.getUuid() : cluster1.getUuid();
        L2NetworkInventory l2 = deployer.l2Networks.get("TestL2Network");
        api.detachL2NetworkFromCluster(l2.getUuid(), lastClusterUuid);

        vr = dbf.listAll(ApplianceVmVO.class).get(0);
        Assert.assertEquals(VmInstanceState.Running, vr.getState());
        Assert.assertEquals(targetClusterUuid, vr.getClusterUuid());

        long count = dbf.count(VmInstanceVO.class);
        Assert.assertEquals(2, count);
    }
}
