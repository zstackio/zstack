package org.zstack.test.storage.primary.local;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageState;
import org.zstack.header.storage.primary.PrimaryStorageStateEvent;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;

import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.list;

/**
 * 1. two clusters, use local storage as ps for one cluster(A) and nfs storage as ps for the other cluster(B)
 * 2. disable local storage
 * 3. create a vm in cluster B with nfs ps specified
 * <p>
 * confirm a successful creation of vm
 */
public class TestLocalStoragePSNfsPSTwoClusters {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/localStorage/TestLocalStoragePSNfsPSTwoClusters.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("localStorageSimulator.xml");
        deployer.addSpringConfig("localStorage.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(LocalStorageSimulatorConfig.class);

        Capacity c = new Capacity();
        c.total = totalSize;
        c.avail = totalSize;

        config.capacityMap.put("host1", c);

        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        PrimaryStorageInventory localPS = deployer.primaryStorages.get("local");
        HostInventory localHost = deployer.hosts.get("host1");
        ClusterInventory localCluster = deployer.clusters.get("Cluster1");

        PrimaryStorageInventory nfsPS = deployer.primaryStorages.get("nfs");
        HostInventory nfsHost = deployer.hosts.get("host2");
        ClusterInventory nfsCluster = deployer.clusters.get("Cluster2");

        ImageInventory image = deployer.images.get("TestImage");
        InstanceOfferingInventory instanceOffering = deployer.instanceOfferings.get("TestInstanceOffering");
        L3NetworkInventory l3network1 = deployer.l3Networks.get("TestL3Network1");


        // disable local ps
        PrimaryStorageInventory inv = api.changePrimaryStorageState(localPS.getUuid(), PrimaryStorageStateEvent.disable);
        Assert.assertEquals(PrimaryStorageState.Disabled.toString(), inv.getState());
        TimeUnit.SECONDS.sleep(1);

        // create a new vm with nfs ps specified
        VmInstanceInventory testVm = new VmInstanceInventory();
        testVm.setName("testVm");
        testVm.setImageUuid(image.getUuid());
        testVm.setUuid(null);
        testVm.setZoneUuid(null);
        testVm.setClusterUuid(nfsCluster.getUuid());
        testVm.setDefaultL3NetworkUuid(l3network1.getUuid());
        testVm.setInstanceOfferingUuid(instanceOffering.getUuid());
        testVm = api.createVmByFullConfigWithSpecifiedPS(
                testVm,
                null,
                list(l3network1.getUuid()),
                null,
                nfsPS.getUuid(),
                session
        );
        Assert.assertEquals(VmInstanceState.Running.toString(), testVm.getState());
        Assert.assertEquals(nfsHost.getUuid(), testVm.getHostUuid());
    }
}
