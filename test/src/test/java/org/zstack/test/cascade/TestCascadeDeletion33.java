package org.zstack.test.cascade;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.appliancevm.ApplianceVmVO;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
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
 * 2. detach primary storage to another cluster that vr is not running
 * 3. detach primary storage from cluster vr is running
 * <p>
 * confirm vr is stopped
 */
public class TestCascadeDeletion33 {
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
        PrimaryStorageInventory ps = deployer.primaryStorages.get("nfs");
        ApplianceVmVO vr = dbf.listAll(ApplianceVmVO.class).get(0);
        String lastClusterUuid = vr.getClusterUuid();
        String targetClusterUuid = cluster1.getUuid().equals(lastClusterUuid) ? cluster2.getUuid() : cluster1.getUuid();
        api.detachPrimaryStorage(ps.getUuid(), targetClusterUuid);
        api.detachPrimaryStorage(ps.getUuid(), lastClusterUuid);

        vr = dbf.listAll(ApplianceVmVO.class).get(0);
        Assert.assertEquals(VmInstanceState.Stopped, vr.getState());

        long count = dbf.count(VmInstanceVO.class);
        Assert.assertEquals(2, count);
    }
}
