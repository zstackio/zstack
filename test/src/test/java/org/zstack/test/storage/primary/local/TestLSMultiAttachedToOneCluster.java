package org.zstack.test.storage.primary.local;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

/**
 * Created by miao on 16-10-21.
 */
public class TestLSMultiAttachedToOneCluster {
    CLogger logger = Utils.getLogger(TestLSMultiAttachedToOneCluster.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/localStorage/TestLSMultiAttachedToOneCluster.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("localStorageSimulator.xml");
        deployer.addSpringConfig("localStorage.xml");
        deployer.load();
        loader = deployer.getComponentLoader();
        config = loader.getComponent(LocalStorageSimulatorConfig.class);

        long totalSize = SizeUnit.TERABYTE.toByte(2);
        LocalStorageSimulatorConfig.Capacity c = new LocalStorageSimulatorConfig.Capacity();
        c.total = totalSize;
        c.avail = totalSize;

        config.capacityMap.put("host1", c);
        config.capacityMap.put("host2", c);

        deployer.build();
        api = deployer.getApi();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        session = api.loginAsAdmin();
    }


    @Test
    public void test() throws ApiSenderException {
        ClusterInventory cluster = deployer.clusters.get("Cluster1");
        PrimaryStorageInventory local = deployer.primaryStorages.get("local");
        PrimaryStorageInventory local2 = deployer.primaryStorages.get("local2");
        HostInventory host1 = deployer.hosts.get("host1");
        HostInventory host2 = deployer.hosts.get("host2");
        api.attachPrimaryStorage(cluster.getUuid(), local.getUuid());
        api.attachPrimaryStorage(cluster.getUuid(), local2.getUuid());
    }
}
