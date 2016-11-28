package org.zstack.test.storage.primary;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.query.QueryOp;
import org.zstack.header.storage.primary.APIQueryPrimaryStorageMsg;
import org.zstack.header.storage.primary.APIQueryPrimaryStorageReply;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.search.QueryTestValidator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestQueryPrimaryStorage {
    CLogger logger = Utils.getLogger(TestQueryPrimaryStorage.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/primaryStorage/TestQueryPrimaryStorage.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        PrimaryStorageInventory inv = deployer.primaryStorages.get("TestPrimaryStorage1");
        QueryTestValidator.validateEQ(new APIQueryPrimaryStorageMsg(), api, APIQueryPrimaryStorageReply.class, inv);
        QueryTestValidator.validateRandomEQConjunction(new APIQueryPrimaryStorageMsg(), api, APIQueryPrimaryStorageReply.class, inv, 3);

        ClusterInventory cluster2 = deployer.clusters.get("cluster2");
        APIQueryPrimaryStorageMsg msg = new APIQueryPrimaryStorageMsg();
        msg.addQueryCondition("attachedClusterUuids", QueryOp.NOT_IN, cluster2.getUuid());
        APIQueryPrimaryStorageReply reply = api.query(msg, APIQueryPrimaryStorageReply.class);
        Assert.assertEquals(2, reply.getInventories().size());
    }
}
