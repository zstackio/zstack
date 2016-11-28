package org.zstack.test.compute.cluster;

import junit.framework.Assert;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.APIGetClusterMsg;
import org.zstack.header.cluster.APISearchClusterMsg;
import org.zstack.header.cluster.ClusterInventory;
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

public class TestSearchCluster {
    CLogger logger = Utils.getLogger(TestSearchCluster.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/cluster/TestSearchCluster.xml");
        deployer.addSpringConfig("SearchManager.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws InterruptedException, ApiSenderException, JSONException {
        TimeUnit.SECONDS.sleep(2);
        APISearchClusterMsg msg = new APISearchClusterMsg();
        NOVTriple t = new NOVTriple();
        t.setName("description");
        t.setOp(SearchOp.AND_NOT_EQ.toString());
        t.setVal("Cluster1");
        msg.getNameOpValueTriples().add(t);
        String content = api.search(msg);

        List<ClusterInventory> invs = JSONObjectUtil.toCollection(content, ArrayList.class, ClusterInventory.class);
        Assert.assertEquals(4, invs.size());

        APISearchClusterMsg msg1 = new APISearchClusterMsg();
        msg1.getFields().add("name");
        msg1.getFields().add("state");
        msg1.getFields().add("uuid");
        NOVTriple t1 = new NOVTriple();
        t1.setName("name");
        t1.setOp(SearchOp.OR_EQ.toString());
        t1.setVal("Cluster2");
        msg1.getNameOpValueTriples().add(t1);
        content = api.search(msg1);
        JSONArray jarr = new JSONArray(content);
        JSONObject jobj = jarr.getJSONObject(0);
        Assert.assertEquals("Cluster2", jobj.getString("name"));
        Assert.assertEquals("Enabled", jobj.getString("state"));

        APIGetClusterMsg gmsg = new APIGetClusterMsg();
        gmsg.setUuid(jobj.getString("uuid"));
        String cres = api.getInventory(gmsg);
        ClusterInventory cinv = JSONObjectUtil.toObject(cres, ClusterInventory.class);
        Assert.assertEquals(jobj.getString("name"), cinv.getName());
    }

}
