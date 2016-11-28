package org.zstack.test.network;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.network.l2.*;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.query.QueryOp;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;

public class TestQueryL2Network {
    CLogger logger = Utils.getLogger(TestQueryL2Network.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/network/TestQueryL2Network.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws InterruptedException, ApiSenderException {
        APIQueryL2NetworkMsg msg = new APIQueryL2NetworkMsg();
        QueryCondition qc = new QueryCondition();
        qc.setName("description");
        qc.setOp(QueryOp.IN.toString());
        qc.setValue("Test");
        msg.getConditions().add(qc);
        APIQueryL2NetworkReply reply = api.query(msg, APIQueryL2NetworkReply.class);
        Assert.assertEquals(7, reply.getInventories().size());

        boolean hasVlanL2 = false;
        for (L2NetworkInventory inv : reply.getInventories()) {
            if (inv instanceof L2VlanNetworkInventory) {
                hasVlanL2 = true;
                break;
            }
        }

        Assert.assertTrue(hasVlanL2);

        msg = new APIQueryL2NetworkMsg();
        qc = new QueryCondition();
        qc.setName("description");
        qc.setOp(QueryOp.NOT_IN.toString());
        qc.setValue("Test");
        msg.getConditions().add(qc);
        reply = api.query(msg, APIQueryL2NetworkReply.class);
        Assert.assertEquals(0, reply.getInventories().size());

        ClusterInventory cinv = deployer.clusters.get("TestCluster");
        msg = new APIQueryL2NetworkMsg();
        msg.addQueryCondition("attachedClusterUuids", QueryOp.IN, cinv.getUuid());
        reply = api.query(msg, APIQueryL2NetworkReply.class);
        Assert.assertEquals(2, reply.getInventories().size());

        msg = new APIQueryL2NetworkMsg();
        msg.addQueryCondition("type", QueryOp.EQ, L2NetworkConstant.L2_VLAN_NETWORK_TYPE);
        msg.addQueryCondition("vlan", QueryOp.EQ, "10");
        reply = api.query(msg, APIQueryL2NetworkReply.class);
        Assert.assertEquals(1, reply.getInventories().size());
        Assert.assertTrue(reply.getInventories().get(0) instanceof L2VlanNetworkInventory);

        msg = new APIQueryL2NetworkMsg();
        msg.setSortBy("physicalInterface");
        msg.setSortDirection("desc");
        msg.setConditions(new ArrayList<QueryCondition>());
        reply = api.query(msg, APIQueryL2NetworkReply.class);
        Assert.assertTrue(reply.getInventories().get(0) instanceof L2VlanNetworkInventory);
    }

}
