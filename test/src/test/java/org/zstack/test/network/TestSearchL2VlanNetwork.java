package org.zstack.test.network;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.network.l2.*;
import org.zstack.header.search.APISearchMessage.NOLTriple;
import org.zstack.header.search.APISearchMessage.NOVTriple;
import org.zstack.header.search.SearchOp;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestSearchL2VlanNetwork {
    CLogger logger = Utils.getLogger(TestSearchL2VlanNetwork.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/network/TestSearchL2VlanNetwork.xml");
        deployer.addSpringConfig("SearchManager.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws InterruptedException, ApiSenderException {
        SimpleQuery<ClusterVO> cq = dbf.createQuery(ClusterVO.class);
        cq.add(ClusterVO_.name, Op.EQ, "TestCluster");
        ClusterVO cluster = cq.find();

        TimeUnit.SECONDS.sleep(1);
        APISearchL2VlanNetworkMsg msg = new APISearchL2VlanNetworkMsg();

        List<String> clusters = new ArrayList<String>();
        clusters.add(cluster.getUuid());

        NOLTriple tl = new NOLTriple();
        tl.setName("attachedClusterUuids");
        tl.setOp(SearchOp.AND_IN.toString());
        tl.setVals(clusters);
        msg.getNameOpListTriples().add(tl);

        NOVTriple t = new NOVTriple();
        t.setName("physicalInterface");
        t.setOp(SearchOp.AND_EQ.toString());
        t.setVal("eth2");
        msg.getNameOpValueTriples().add(t);

        t = new NOVTriple();
        t.setName("vlan");
        t.setOp(SearchOp.AND_EQ.toString());
        t.setVal("3");
        msg.getNameOpValueTriples().add(t);

        String content = api.search(msg);
        List<L2VlanNetworkInventory> invs = JSONObjectUtil.toCollection(content, ArrayList.class, L2VlanNetworkInventory.class);
        Assert.assertEquals(1, invs.size());

        SimpleQuery<L2VlanNetworkVO> q = dbf.createQuery(L2VlanNetworkVO.class);
        q.add(L2VlanNetworkVO_.vlan, Op.EQ, 1);
        L2VlanNetworkVO vlan1 = q.find();

        APIGetL2VlanNetworkMsg gmsg = new APIGetL2VlanNetworkMsg();
        gmsg.setUuid(vlan1.getUuid());
        String vlan1str = api.getInventory(gmsg);
        L2VlanNetworkInventory vlan1Inv = JSONObjectUtil.toObject(vlan1str, L2VlanNetworkInventory.class);
        Assert.assertEquals(vlan1.getName(), vlan1Inv.getName());
    }

}
