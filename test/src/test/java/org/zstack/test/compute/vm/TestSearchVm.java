package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.DbEntityLister;
import org.zstack.header.search.APISearchMessage.NOVTriple;
import org.zstack.header.search.SearchOp;
import org.zstack.header.vm.*;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestSearchVm {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    DbEntityLister dl;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestSearchVm.xml");
        deployer.addSpringConfig("SearchManager.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        dl = loader.getComponent(DbEntityLister.class);
    }

    @Test
    public void test() throws InterruptedException, ApiSenderException, JSONException {
        TimeUnit.SECONDS.sleep(2);
        List<VmNicVO> nics = dl.listAll(VmNicVO.class);
        VmNicVO n1 = nics.get(0);

        APISearchVmInstanceMsg msg = new APISearchVmInstanceMsg();
        NOVTriple t = new NOVTriple();
        t.setName("vmNics.uuid");
        t.setOp(SearchOp.AND_EQ.toString());
        t.setVal(n1.getUuid());
        msg.getNameOpValueTriples().add(t);
        String content = api.search(msg);

        List<VmInstanceInventory> vms = JSONObjectUtil.toCollection(content, ArrayList.class, VmInstanceInventory.class);
        Assert.assertEquals(1, vms.size());
        VmInstanceInventory vm1 = vms.get(0);
        Assert.assertEquals(n1.getVmInstanceUuid(), vm1.getUuid());

        APISearchVmInstanceMsg msg1 = new APISearchVmInstanceMsg();
        msg.getFields().add("vmNics");
        content = api.search(msg1);
        JSONArray jarr = new JSONArray(content);
        List<VmNicInventory> ninvs = new ArrayList<VmNicInventory>(nics.size());
        for (int i = 0; i < jarr.length(); i++) {
            JSONObject jo = jarr.getJSONObject(i);
            ninvs.addAll(JSONObjectUtil.toCollection(jo.getString("vmNics"), ArrayList.class, VmNicInventory.class));
        }
        Assert.assertEquals(nics.size(), ninvs.size());

        APISearchVmInstanceMsg msg2 = new APISearchVmInstanceMsg();
        content = api.search(msg2);
        jarr = new JSONArray(content);
        vms = JSONObjectUtil.toCollection(content, ArrayList.class, VmInstanceInventory.class);
        Assert.assertEquals(2, vms.size());
        for (VmInstanceInventory vm : vms) {
            Assert.assertEquals(VmInstanceState.Running.toString(), vm.getState());
        }

        VmInstanceInventory inv0 = vms.get(0);
        APIGetVmInstanceMsg gmsg = new APIGetVmInstanceMsg();
        gmsg.setUuid(inv0.getUuid());
        String res = api.getInventory(gmsg);
        VmInstanceInventory vminv = JSONObjectUtil.toObject(res, VmInstanceInventory.class);
        Assert.assertEquals(inv0.getName(), vminv.getName());
    }

}
