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
import org.zstack.header.network.l2.APIGetL2NetworkMsg;
import org.zstack.header.network.l2.APISearchL2NetworkMsg;
import org.zstack.header.network.l2.L2NetworkInventory;
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

public class TestSearchL2Network {
    CLogger logger = Utils.getLogger(TestSearchL2Network.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/network/TestSearchL2Network.xml");
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
        APISearchL2NetworkMsg msg = new APISearchL2NetworkMsg();

        List<String> clusters = new ArrayList<String>();
        clusters.add(cluster.getUuid());

        NOLTriple tl = new NOLTriple();
        tl.setName("attachedClusterUuids");
        tl.setOp(SearchOp.AND_IN.toString());
        tl.setVals(clusters);
        msg.getNameOpListTriples().add(tl);

        NOVTriple t = new NOVTriple();
        t.setName("physicalInterface");
        t.setOp(SearchOp.OR_EQ.toString());
        t.setVal("eth3");
        msg.getNameOpValueTriples().add(t);

        String content = api.search(msg);
        List<L2NetworkInventory> invs = JSONObjectUtil.toCollection(content, ArrayList.class, L2NetworkInventory.class);
        Assert.assertEquals(3, invs.size());

        for (L2NetworkInventory l2 : invs) {
            if (!l2.getAttachedClusterUuids().isEmpty()) {
                api.detachL2NetworkFromCluster(l2.getUuid(), l2.getAttachedClusterUuids().iterator().next());
            }
        }

        TimeUnit.SECONDS.sleep(1);
        content = api.search(msg);
        invs = JSONObjectUtil.toCollection(content, ArrayList.class, L2NetworkInventory.class);
        Assert.assertEquals(1, invs.size());

        msg = new APISearchL2NetworkMsg();
        NOVTriple t1 = new NOVTriple();
        t1.setName("name");
        t1.setOp(SearchOp.AND_EQ.toString());
        t1.setVal("noVlan");
        msg.getNameOpValueTriples().add(t1);
        content = api.search(msg);
        invs = JSONObjectUtil.toCollection(content, ArrayList.class, L2NetworkInventory.class);
        Assert.assertEquals(1, invs.size());

        L2NetworkInventory inv0 = invs.get(0);
        APIGetL2NetworkMsg gmsg = new APIGetL2NetworkMsg();
        gmsg.setUuid(inv0.getUuid());
        String res = api.getInventory(gmsg);
        L2NetworkInventory linv = JSONObjectUtil.toObject(res, L2NetworkInventory.class);
        Assert.assertEquals(inv0.getName(), linv.getName());
    }

}
