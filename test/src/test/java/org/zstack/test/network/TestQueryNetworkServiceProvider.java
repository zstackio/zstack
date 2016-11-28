package org.zstack.test.network;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.service.APIQueryNetworkServiceProviderMsg;
import org.zstack.header.network.service.APIQueryNetworkServiceProviderReply;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.query.QueryOp;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestQueryNetworkServiceProvider {
    CLogger logger = Utils.getLogger(TestQueryNetworkServiceProvider.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/network/TestQueryNetworkServiceProvider.xml");
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        APIQueryNetworkServiceProviderMsg msg = new APIQueryNetworkServiceProviderMsg();
        QueryCondition qc = new QueryCondition();
        qc.setName("networkServiceTypes");
        qc.setOp(QueryOp.IN.toString());
        qc.setValues("DHCP", "DNS");
        msg.getConditions().add(qc);
        APIQueryNetworkServiceProviderReply reply = api.query(msg, APIQueryNetworkServiceProviderReply.class);
        Assert.assertEquals(1, reply.getInventories().size());

        L2NetworkInventory l2inv = deployer.l2Networks.get("TestL2Network");
        msg = new APIQueryNetworkServiceProviderMsg();
        qc = new QueryCondition();
        qc.setName("attachedL2NetworkUuids");
        qc.setOp(QueryOp.EQ.toString());
        qc.setValue(l2inv.getUuid());

        qc = new QueryCondition();
        qc.setName("name");
        qc.setOp(QueryOp.EQ.toString());
        qc.setValue(VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE);
        msg.getConditions().add(qc);
        reply = api.query(msg, APIQueryNetworkServiceProviderReply.class);
        Assert.assertEquals(1, reply.getInventories().size());

        msg = new APIQueryNetworkServiceProviderMsg();
        qc = new QueryCondition();
        qc.setName("attachedL2NetworkUuids");
        qc.setOp(QueryOp.EQ.toString());
        qc.setValue(l2inv.getUuid());
        msg.getConditions().add(qc);

        qc = new QueryCondition();
        qc.setName("networkServiceTypes");
        qc.setOp(QueryOp.IN.toString());
        qc.setValues("DHCP", "DNS");
        msg.getConditions().add(qc);
        reply = api.query(msg, APIQueryNetworkServiceProviderReply.class);
        Assert.assertEquals(1, reply.getInventories().size());

        msg = new APIQueryNetworkServiceProviderMsg();
        qc = new QueryCondition();
        qc.setName("attachedL2NetworkUuids");
        qc.setOp(QueryOp.EQ.toString());
        qc.setValue(l2inv.getUuid());
        msg.getConditions().add(qc);

        qc = new QueryCondition();
        qc.setName("name");
        qc.setOp(QueryOp.EQ.toString());
        qc.setValue(VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE);
        msg.getConditions().add(qc);

        qc = new QueryCondition();
        qc.setName("networkServiceTypes");
        qc.setOp(QueryOp.NOT_IN.toString());
        qc.setValues("DHCP", "DNS");
        msg.getConditions().add(qc);
        reply = api.query(msg, APIQueryNetworkServiceProviderReply.class);
        Assert.assertEquals(0, reply.getInventories().size());
    }

}
